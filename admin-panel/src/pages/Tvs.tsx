import { useState } from 'react';
import { toast } from 'sonner';
import { MonitorPlay, MonitorOff, Settings, Loader2 } from 'lucide-react';
import { playlists as playlistsApi, tvs as tvsApi } from '@/api/endpoints';
import { useFetch } from '@/hooks/useFetch';
import { usePlaylistChannel } from '@/hooks/useWebSocket';
import { Skeleton } from '@/components/Skeleton';
import { EmptyState } from '@/components/EmptyState';
import { isOnline, relativeTime } from '@/lib/format';
import { apiErrorMessage } from '@/api/client';
import { cn } from '@/lib/cn';

export default function Tvs() {
  const tvsQ = useFetch(() => tvsApi.list(), []);
  const playlistsQ = useFetch(() => playlistsApi.list(), []);
  const [savingId, setSavingId] = useState<number | null>(null);

  usePlaylistChannel(() => {
    void tvsQ.refetch();
    void playlistsQ.refetch();
  });

  async function changePlaylist(tvId: number, value: string) {
    const playlistId = value === '' ? null : Number(value);
    setSavingId(tvId);
    try {
      const updated = await tvsApi.setPlaylist(tvId, playlistId);
      tvsQ.setData(
        (prev) => prev?.map((t) => (t.id === tvId ? { ...t, ...updated } : t)) ?? null,
      );
      toast.success('Playlist asignada');
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo asignar'));
    } finally {
      setSavingId(null);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold tracking-tight text-gray-900">TVs</h2>
        <p className="text-sm text-gray-500">
          Pantallas Android TV vinculadas a tu local. Las nuevas TVs se registran
          desde la propia app.
        </p>
      </div>

      {tvsQ.loading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-40 w-full rounded-xl" />
          ))}
        </div>
      ) : tvsQ.error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {tvsQ.error}
        </div>
      ) : (tvsQ.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={MonitorOff}
          title="No hay TVs registradas"
          description="Cuando una TV inicie sesión por primera vez con tus credenciales, aparecerá aquí."
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {tvsQ.data!.map((tv) => {
            const online = isOnline(tv.last_seen, tv.online);
            return (
              <article key={tv.id} className="card flex flex-col p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="grid h-10 w-10 place-items-center rounded-lg bg-slate-900 text-white">
                    <MonitorPlay className="h-5 w-5" />
                  </div>
                  <span
                    className={cn(
                      'inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium',
                      online
                        ? 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200'
                        : 'bg-gray-100 text-gray-500 ring-1 ring-inset ring-gray-200',
                    )}
                  >
                    <span
                      className={cn(
                        'h-1.5 w-1.5 rounded-full',
                        online ? 'bg-emerald-500 animate-pulse' : 'bg-gray-400',
                      )}
                    />
                    {online ? 'En línea' : 'Desconectado'}
                  </span>
                </div>

                <div className="mt-4 min-w-0">
                  <h3 className="truncate text-base font-semibold text-gray-900">
                    {tv.nombre || 'TV sin nombre'}
                  </h3>
                  <p className="mt-0.5 truncate text-xs text-gray-500">
                    ID dispositivo: <code className="text-[11px]">{tv.device_id}</code>
                  </p>
                  <p className="mt-0.5 text-xs text-gray-500">
                    Último contacto: {relativeTime(tv.last_seen)}
                  </p>
                </div>

                <div className="mt-5">
                  <label className="label flex items-center gap-1.5">
                    <Settings className="h-3.5 w-3.5 text-gray-400" /> Playlist asignada
                  </label>
                  <div className="flex items-center gap-2">
                    <select
                      className="input"
                      value={tv.current_playlist_id ?? ''}
                      onChange={(e) => changePlaylist(tv.id, e.target.value)}
                      disabled={savingId === tv.id || playlistsQ.loading}
                    >
                      <option value="">— Sin asignar —</option>
                      {(playlistsQ.data ?? []).map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.nombre}
                        </option>
                      ))}
                    </select>
                    {savingId === tv.id && (
                      <Loader2 className="h-4 w-4 shrink-0 animate-spin text-brand-600" />
                    )}
                  </div>
                  <p className="mt-1.5 text-xs text-gray-400">
                    Los horarios activos tienen prioridad sobre la asignación manual.
                  </p>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
