import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Plus, Loader2, ListPlus } from 'lucide-react';
import { playlists as playlistsApi } from '@/api/endpoints';
import { useFetch } from '@/hooks/useFetch';
import { usePlaylistChannel } from '@/hooks/useWebSocket';
import { PlaylistCard } from '@/components/PlaylistCard';
import { EmptyState } from '@/components/EmptyState';
import { CardGridSkeleton } from '@/components/Skeleton';
import { Modal } from '@/components/Modal';
import { apiErrorMessage } from '@/api/client';
import type { Playlist, TransitionType } from '@/types';

const TRANSITIONS: { value: TransitionType; label: string }[] = [
  { value: 'FADE', label: 'Fade (recomendada)' },
  { value: 'SLIDE', label: 'Slide' },
  { value: 'ZOOM', label: 'Zoom' },
  { value: 'NONE', label: 'Sin transición' },
];

export default function Playlists() {
  const { data, loading, error, refetch, setData } = useFetch(
    () => playlistsApi.list(),
    [],
  );
  const nav = useNavigate();

  const [creating, setCreating] = useState(false);
  const [confirm, setConfirm] = useState<Playlist | null>(null);
  const [deleting, setDeleting] = useState(false);

  // form state
  const [name, setName] = useState('');
  const [transition, setTransition] = useState<TransitionType>('FADE');
  const [defaultSec, setDefaultSec] = useState(8);
  const [submitting, setSubmitting] = useState(false);

  usePlaylistChannel(() => {
    void refetch();
  });

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    try {
      const created = await playlistsApi.create({
        nombre: name.trim(),
        transicion: transition,
        default_image_seconds: defaultSec,
      });
      toast.success('Playlist creada');
      setData((prev) => (prev ? [created, ...prev] : [created]));
      setCreating(false);
      setName('');
      setTransition('FADE');
      setDefaultSec(8);
      nav(`/playlists/${created.id}`);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo crear'));
    } finally {
      setSubmitting(false);
    }
  }

  async function onDelete(p: Playlist) {
    setConfirm(p);
  }

  async function confirmDeleteNow() {
    if (!confirm) return;
    setDeleting(true);
    try {
      await playlistsApi.remove(confirm.id);
      setData((prev) => prev?.filter((x) => x.id !== confirm.id) ?? []);
      toast.success('Playlist eliminada');
      setConfirm(null);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo eliminar'));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-gray-900">Playlists</h2>
          <p className="text-sm text-gray-500">
            Crea listas ordenadas de contenido para programar en tus pantallas.
          </p>
        </div>
        <button className="btn-primary" onClick={() => setCreating(true)}>
          <Plus className="h-4 w-4" /> Nueva playlist
        </button>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <CardGridSkeleton count={6} />
        </div>
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={ListPlus}
          title="Aún no tienes playlists"
          description="Crea tu primera playlist para arrastrar contenido y programarla en tus TVs."
          action={
            <button className="btn-primary" onClick={() => setCreating(true)}>
              <Plus className="h-4 w-4" /> Crear playlist
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((p) => (
            <PlaylistCard key={p.id} playlist={p} onDelete={onDelete} />
          ))}
        </div>
      )}

      {/* Modal Crear */}
      <Modal
        open={creating}
        onClose={() => !submitting && setCreating(false)}
        title="Nueva playlist"
        description="Configura los valores generales. Luego añades el contenido en el editor."
        footer={
          <>
            <button
              className="btn-secondary"
              onClick={() => setCreating(false)}
              disabled={submitting}
            >
              Cancelar
            </button>
            <button
              type="submit"
              form="playlist-form"
              className="btn-primary"
              disabled={submitting}
            >
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />} Crear
            </button>
          </>
        }
      >
        <form id="playlist-form" onSubmit={onCreate} className="space-y-4">
          <div>
            <label htmlFor="pl-name" className="label">
              Nombre
            </label>
            <input
              id="pl-name"
              className="input"
              placeholder="Ej. Menú desayuno"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              maxLength={120}
              autoFocus
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="pl-trans" className="label">
                Transición
              </label>
              <select
                id="pl-trans"
                className="input"
                value={transition}
                onChange={(e) => setTransition(e.target.value as TransitionType)}
              >
                {TRANSITIONS.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="pl-dur" className="label">
                Duración por imagen (s)
              </label>
              <input
                id="pl-dur"
                type="number"
                min={1}
                max={600}
                className="input"
                value={defaultSec}
                onChange={(e) => setDefaultSec(Math.max(1, parseInt(e.target.value || '1', 10)))}
              />
            </div>
          </div>
        </form>
      </Modal>

      {/* Confirmar eliminar */}
      <Modal
        open={!!confirm}
        onClose={() => setConfirm(null)}
        title="Eliminar playlist"
        description="Esta acción no se puede deshacer."
        footer={
          <>
            <button
              className="btn-secondary"
              onClick={() => setConfirm(null)}
              disabled={deleting}
            >
              Cancelar
            </button>
            <button className="btn-danger" onClick={confirmDeleteNow} disabled={deleting}>
              {deleting && <Loader2 className="h-4 w-4 animate-spin" />} Eliminar
            </button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          ¿Eliminar la playlist <strong>{confirm?.nombre}</strong>? Los horarios que la
          usen quedarán huérfanos.
        </p>
      </Modal>
    </div>
  );
}
