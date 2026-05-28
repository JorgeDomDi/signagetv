import { useCallback, useRef, useState, type DragEvent } from 'react';
import { toast } from 'sonner';
import { UploadCloud, ImagePlus, Loader2 } from 'lucide-react';
import { media as mediaApi } from '@/api/endpoints';
import { useFetch } from '@/hooks/useFetch';
import { usePlaylistChannel } from '@/hooks/useWebSocket';
import { MediaCard } from '@/components/MediaCard';
import { EmptyState } from '@/components/EmptyState';
import { CardGridSkeleton } from '@/components/Skeleton';
import { Modal } from '@/components/Modal';
import { formatBytes } from '@/lib/format';
import { apiErrorMessage } from '@/api/client';
import { cn } from '@/lib/cn';
import type { MediaItem } from '@/types';

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'video/mp4', 'video/webm', 'video/quicktime'];
const MAX_BYTES = 500 * 1024 * 1024; // 500 MB

interface PendingUpload {
  id: string;
  file: File;
  progress: number;
  status: 'queued' | 'uploading' | 'done' | 'error';
  error?: string;
}

export default function MediaLibrary() {
  const { data, loading, error, refetch, setData } = useFetch(() => mediaApi.list(), []);
  const [uploads, setUploads] = useState<PendingUpload[]>([]);
  const [dragOver, setDragOver] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<MediaItem | null>(null);
  const [deleting, setDeleting] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  usePlaylistChannel(() => {
    void refetch();
  });

  const handleFiles = useCallback(
    async (files: FileList | File[]) => {
      const list = Array.from(files);
      const valid: File[] = [];
      for (const f of list) {
        if (f.size > MAX_BYTES) {
          toast.error(`"${f.name}" supera el tamaño máximo (${formatBytes(MAX_BYTES)})`);
          continue;
        }
        if (!ACCEPTED.includes(f.type) && !f.type.startsWith('image/') && !f.type.startsWith('video/')) {
          toast.error(`"${f.name}" tiene un tipo no soportado`);
          continue;
        }
        valid.push(f);
      }

      const initial: PendingUpload[] = valid.map((f) => ({
        id: `${f.name}-${f.size}-${Date.now()}-${Math.random()}`,
        file: f,
        progress: 0,
        status: 'queued',
      }));
      if (initial.length === 0) return;
      setUploads((prev) => [...initial, ...prev]);

      for (const up of initial) {
        setUploads((prev) =>
          prev.map((p) => (p.id === up.id ? { ...p, status: 'uploading' } : p)),
        );
        try {
          const created = await mediaApi.upload(up.file, (pct) => {
            setUploads((prev) =>
              prev.map((p) => (p.id === up.id ? { ...p, progress: pct } : p)),
            );
          });
          setUploads((prev) =>
            prev.map((p) =>
              p.id === up.id ? { ...p, status: 'done', progress: 100 } : p,
            ),
          );
          setData((prev) => (prev ? [created, ...prev] : [created]));
        } catch (err) {
          const msg = apiErrorMessage(err, 'Error subiendo archivo');
          setUploads((prev) =>
            prev.map((p) =>
              p.id === up.id ? { ...p, status: 'error', error: msg } : p,
            ),
          );
          toast.error(`${up.file.name}: ${msg}`);
        }
      }

      // Auto-limpieza de uploads completados a los 4s
      setTimeout(() => {
        setUploads((prev) => prev.filter((p) => p.status !== 'done'));
      }, 4000);
    },
    [setData],
  );

  function onDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files?.length) void handleFiles(e.dataTransfer.files);
  }

  async function onDelete(item: MediaItem) {
    setConfirmDelete(item);
  }

  async function confirmDeleteNow() {
    if (!confirmDelete) return;
    setDeleting(true);
    try {
      await mediaApi.remove(confirmDelete.id);
      setData((prev) => prev?.filter((m) => m.id !== confirmDelete.id) ?? []);
      toast.success('Archivo eliminado');
      setConfirmDelete(null);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo eliminar'));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Header + upload */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-gray-900">
            Biblioteca de medios
          </h2>
          <p className="text-sm text-gray-500">
            Sube imágenes y videos para usar en tus playlists.
          </p>
        </div>
        <button
          type="button"
          className="btn-primary"
          onClick={() => inputRef.current?.click()}
        >
          <UploadCloud className="h-4 w-4" /> Subir archivos
        </button>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept="image/*,video/*"
          className="hidden"
          onChange={(e) => {
            if (e.target.files) void handleFiles(e.target.files);
            e.target.value = '';
          }}
        />
      </div>

      {/* Dropzone */}
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
        className={cn(
          'flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors',
          dragOver
            ? 'border-brand-500 bg-brand-50/40 text-brand-700'
            : 'border-gray-300 bg-white text-gray-500 hover:border-gray-400',
        )}
        onClick={() => inputRef.current?.click()}
      >
        <UploadCloud className="h-8 w-8" />
        <p className="mt-2 text-sm font-medium">
          Arrastra y suelta archivos aquí, o haz clic para seleccionarlos
        </p>
        <p className="text-xs text-gray-400">
          Imágenes (JPG, PNG, WEBP, GIF) o videos (MP4, WEBM, MOV) · máx{' '}
          {formatBytes(MAX_BYTES)}
        </p>
      </div>

      {/* Uploads en curso */}
      {uploads.length > 0 && (
        <section className="card p-4">
          <h3 className="mb-3 text-sm font-semibold text-gray-900">Subidas</h3>
          <ul className="space-y-2.5">
            {uploads.map((u) => (
              <li key={u.id} className="rounded-lg border border-gray-200 px-3 py-2.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="truncate font-medium text-gray-700">{u.file.name}</span>
                  <span className="ml-2 shrink-0 text-gray-500">
                    {u.status === 'uploading'
                      ? `${u.progress}%`
                      : u.status === 'done'
                      ? 'Completado'
                      : u.status === 'error'
                      ? `Error`
                      : 'En cola'}
                  </span>
                </div>
                <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                  <div
                    className={cn(
                      'h-full transition-all',
                      u.status === 'error' ? 'bg-red-500' : 'bg-brand-500',
                    )}
                    style={{ width: `${u.status === 'error' ? 100 : u.progress}%` }}
                  />
                </div>
                {u.error && <div className="mt-1 text-xs text-red-600">{u.error}</div>}
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Grid */}
      {loading ? (
        <CardGridSkeleton count={8} />
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={ImagePlus}
          title="Tu biblioteca está vacía"
          description="Sube tu primer archivo para empezar a crear playlists para tus pantallas."
          action={
            <button className="btn-primary" onClick={() => inputRef.current?.click()}>
              <UploadCloud className="h-4 w-4" /> Subir ahora
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
          {data.map((m) => (
            <MediaCard key={m.id} item={m} onDelete={onDelete} />
          ))}
        </div>
      )}

      <Modal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        title="Eliminar archivo"
        description="Esta acción no se puede deshacer."
        footer={
          <>
            <button
              className="btn-secondary"
              onClick={() => setConfirmDelete(null)}
              disabled={deleting}
            >
              Cancelar
            </button>
            <button
              className="btn-danger"
              onClick={confirmDeleteNow}
              disabled={deleting}
            >
              {deleting && <Loader2 className="h-4 w-4 animate-spin" />}
              Eliminar
            </button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          ¿Seguro que quieres eliminar <strong>{confirmDelete?.filename}</strong>? Si está
          en alguna playlist, podría dejar de reproducirse.
        </p>
      </Modal>
    </div>
  );
}
