import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  ListVideo,
  MonitorPlay,
  Image as ImageIcon,
  CalendarClock,
  ArrowRight,
  CircleDot,
} from 'lucide-react';
import { useFetch } from '@/hooks/useFetch';
import { usePlaylistChannel } from '@/hooks/useWebSocket';
import {
  media as mediaApi,
  playlists as playlistsApi,
  schedules as schedulesApi,
  tvs as tvsApi,
} from '@/api/endpoints';
import { Skeleton } from '@/components/Skeleton';
import { DAYS_OF_WEEK, formatHm, isOnline, parseDays, relativeTime } from '@/lib/format';
import { useAuth } from '@/auth/AuthContext';
import type { Schedule } from '@/types';

interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  icon: React.ComponentType<{ className?: string }>;
  to: string;
  tone?: 'brand' | 'emerald' | 'amber' | 'sky';
}

const tones: Record<NonNullable<StatCardProps['tone']>, string> = {
  brand: 'bg-brand-50 text-brand-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  sky: 'bg-sky-50 text-sky-600',
};

function StatCard({ label, value, hint, icon: Icon, to, tone = 'brand' }: StatCardProps) {
  return (
    <Link
      to={to}
      className="card group flex items-center justify-between p-5 transition-transform hover:-translate-y-0.5"
    >
      <div>
        <div className="text-xs font-medium uppercase tracking-wide text-gray-500">
          {label}
        </div>
        <div className="mt-1 text-2xl font-semibold tracking-tight text-gray-900">
          {value}
        </div>
        {hint && <div className="mt-1 text-xs text-gray-500">{hint}</div>}
      </div>
      <div className={`grid h-11 w-11 place-items-center rounded-xl ${tones[tone]}`}>
        <Icon className="h-5 w-5" />
      </div>
    </Link>
  );
}

const DAY_INDEX: Record<string, number> = {
  MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6, SUN: 0,
};

function isScheduleActiveNow(s: Schedule, now: Date): boolean {
  if (!s.activo) return false;
  const days = parseDays(s.dias_semana).map((d) => DAY_INDEX[d]);
  if (!days.includes(now.getDay())) return false;
  const cur = now.getHours() * 60 + now.getMinutes();
  const [hs, ms] = s.hora_inicio.split(':');
  const [he, me] = s.hora_fin.split(':');
  const start = parseInt(hs, 10) * 60 + parseInt(ms ?? '0', 10);
  const end = parseInt(he, 10) * 60 + parseInt(me ?? '0', 10);
  return cur >= start && cur < end;
}

function nextSchedule(list: Schedule[]): Schedule | null {
  const now = new Date();
  let best: { s: Schedule; mins: number } | null = null;
  for (const s of list) {
    if (!s.activo) continue;
    const days = parseDays(s.dias_semana).map((d) => DAY_INDEX[d]);
    if (!days.length) continue;
    const [hs, ms] = s.hora_inicio.split(':');
    const startMins = parseInt(hs, 10) * 60 + parseInt(ms ?? '0', 10);
    // calcular la próxima ocurrencia
    for (let offset = 0; offset < 7; offset++) {
      const day = (now.getDay() + offset) % 7;
      if (!days.includes(day)) continue;
      const total = offset * 24 * 60 + startMins - (now.getHours() * 60 + now.getMinutes());
      if (total > 0) {
        if (!best || total < best.mins) best = { s, mins: total };
        break;
      }
    }
  }
  return best?.s ?? null;
}

export default function Dashboard() {
  const { local } = useAuth();
  const playlistsQ = useFetch(() => playlistsApi.list(), []);
  const tvsQ = useFetch(() => tvsApi.list(), []);
  const mediaQ = useFetch(() => mediaApi.list(), []);
  const schedulesQ = useFetch(() => schedulesApi.list(), []);

  // Refetch en cambios push
  usePlaylistChannel(() => {
    void playlistsQ.refetch();
    void schedulesQ.refetch();
    void tvsQ.refetch();
  });

  const onlineCount = useMemo(
    () => (tvsQ.data ?? []).filter((t) => isOnline(t.last_seen, t.online)).length,
    [tvsQ.data],
  );

  const now = new Date();
  const activeSchedules = (schedulesQ.data ?? []).filter((s) =>
    isScheduleActiveNow(s, now),
  );
  const upcoming = nextSchedule(schedulesQ.data ?? []);
  const playlistName = (id?: number | null) =>
    (playlistsQ.data ?? []).find((p) => p.id === id)?.nombre ?? '—';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <p className="text-sm text-gray-500">Bienvenido de nuevo</p>
        <h2 className="text-2xl font-semibold tracking-tight text-gray-900">
          {local?.nombre ?? 'Local'}
        </h2>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Playlists"
          value={playlistsQ.loading ? '—' : playlistsQ.data?.length ?? 0}
          icon={ListVideo}
          to="/playlists"
          tone="brand"
        />
        <StatCard
          label="TVs online"
          value={
            tvsQ.loading
              ? '—'
              : `${onlineCount}/${tvsQ.data?.length ?? 0}`
          }
          hint={tvsQ.loading ? undefined : 'Activas en los últimos 2 min'}
          icon={MonitorPlay}
          to="/tvs"
          tone="emerald"
        />
        <StatCard
          label="Items en biblioteca"
          value={mediaQ.loading ? '—' : mediaQ.data?.length ?? 0}
          icon={ImageIcon}
          to="/media"
          tone="sky"
        />
        <StatCard
          label="Horarios"
          value={schedulesQ.loading ? '—' : schedulesQ.data?.length ?? 0}
          icon={CalendarClock}
          to="/schedules"
          tone="amber"
        />
      </div>

      {/* En vivo / Próximo */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <section className="card p-5">
          <header className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-gray-900">Reproduciéndose ahora</h3>
              <p className="text-xs text-gray-500">
                Playlists activas según horario en este momento
              </p>
            </div>
            <Link
              to="/schedules"
              className="text-xs font-medium text-brand-600 hover:text-brand-700"
            >
              Ver horarios
            </Link>
          </header>
          {schedulesQ.loading ? (
            <div className="space-y-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : activeSchedules.length === 0 ? (
            <p className="text-sm text-gray-500">Ningún horario activo ahora mismo.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {activeSchedules.map((s) => (
                <li key={s.id} className="flex items-center gap-3 py-3">
                  <CircleDot className="h-4 w-4 text-emerald-500" />
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-gray-900">
                      {s.nombre}
                    </div>
                    <div className="text-xs text-gray-500">
                      Playlist: {playlistName(s.playlist_id)} ·{' '}
                      {formatHm(s.hora_inicio)}–{formatHm(s.hora_fin)}
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card p-5">
          <header className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-gray-900">Próximo horario</h3>
              <p className="text-xs text-gray-500">Siguiente programación que arrancará</p>
            </div>
          </header>
          {schedulesQ.loading ? (
            <Skeleton className="h-16 w-full" />
          ) : !upcoming ? (
            <p className="text-sm text-gray-500">No hay próximos horarios programados.</p>
          ) : (
            <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
              <div className="text-sm font-semibold text-gray-900">{upcoming.nombre}</div>
              <div className="mt-1 text-xs text-gray-600">
                Playlist: {playlistName(upcoming.playlist_id)}
              </div>
              <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500">
                <span>
                  {parseDays(upcoming.dias_semana)
                    .map((d) => DAYS_OF_WEEK.find((x) => x.key === d)?.short)
                    .join(' ')}
                </span>
                <span>·</span>
                <span>
                  {formatHm(upcoming.hora_inicio)}–{formatHm(upcoming.hora_fin)}
                </span>
              </div>
            </div>
          )}
        </section>
      </div>

      {/* TVs preview */}
      <section className="card p-5">
        <header className="mb-4 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-gray-900">Pantallas registradas</h3>
            <p className="text-xs text-gray-500">Estado de las TVs vinculadas a tu local</p>
          </div>
          <Link
            to="/tvs"
            className="inline-flex items-center gap-1 text-xs font-medium text-brand-600 hover:text-brand-700"
          >
            Ver todas <ArrowRight className="h-3 w-3" />
          </Link>
        </header>
        {tvsQ.loading ? (
          <div className="space-y-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : (tvsQ.data?.length ?? 0) === 0 ? (
          <p className="text-sm text-gray-500">
            Aún no hay TVs registradas. Las TVs se vinculan desde la app Android.
          </p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {tvsQ.data!.slice(0, 5).map((tv) => {
              const online = isOnline(tv.last_seen, tv.online);
              return (
                <li key={tv.id} className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <span
                      className={`h-2 w-2 rounded-full ${
                        online ? 'bg-emerald-500' : 'bg-gray-300'
                      }`}
                    />
                    <div>
                      <div className="text-sm font-medium text-gray-900">{tv.nombre}</div>
                      <div className="text-xs text-gray-500">
                        {online ? 'En línea' : `Última conexión ${relativeTime(tv.last_seen)}`}
                      </div>
                    </div>
                  </div>
                  <div className="text-xs text-gray-500">
                    {playlistName(tv.current_playlist_id)}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}
