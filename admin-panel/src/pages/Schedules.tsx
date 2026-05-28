import { useMemo, useState } from 'react';
import { toast } from 'sonner';
import { CalendarPlus, Loader2, Pencil, Plus, Trash2 } from 'lucide-react';
import { playlists as playlistsApi, schedules as schedulesApi } from '@/api/endpoints';
import { useFetch } from '@/hooks/useFetch';
import { usePlaylistChannel } from '@/hooks/useWebSocket';
import { Skeleton } from '@/components/Skeleton';
import { EmptyState } from '@/components/EmptyState';
import { Modal } from '@/components/Modal';
import { ScheduleForm } from '@/components/ScheduleForm';
import { DAYS_OF_WEEK, formatHm, parseDays } from '@/lib/format';
import { apiErrorMessage } from '@/api/client';
import { cn } from '@/lib/cn';
import type { DayOfWeek, Schedule, ScheduleInput } from '@/types';

// Paleta para distinguir playlists en el calendario
const PALETTE = [
  'bg-brand-500/85 hover:bg-brand-600',
  'bg-emerald-500/85 hover:bg-emerald-600',
  'bg-amber-500/85 hover:bg-amber-600',
  'bg-sky-500/85 hover:bg-sky-600',
  'bg-pink-500/85 hover:bg-pink-600',
  'bg-violet-500/85 hover:bg-violet-600',
  'bg-rose-500/85 hover:bg-rose-600',
  'bg-teal-500/85 hover:bg-teal-600',
];

function timeToMinutes(t: string): number {
  const [h, m] = t.split(':');
  return parseInt(h, 10) * 60 + parseInt(m ?? '0', 10);
}

export default function Schedules() {
  const schedulesQ = useFetch(() => schedulesApi.list(), []);
  const playlistsQ = useFetch(() => playlistsApi.list(), []);
  const [editing, setEditing] = useState<Schedule | null>(null);
  const [creating, setCreating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [confirm, setConfirm] = useState<Schedule | null>(null);
  const [deleting, setDeleting] = useState(false);

  usePlaylistChannel(() => {
    void schedulesQ.refetch();
    void playlistsQ.refetch();
  });

  const playlistColor = useMemo(() => {
    const map = new Map<number, string>();
    (playlistsQ.data ?? []).forEach((p, idx) => {
      map.set(p.id, PALETTE[idx % PALETTE.length]);
    });
    return map;
  }, [playlistsQ.data]);

  const playlistName = (id: number) =>
    (playlistsQ.data ?? []).find((p) => p.id === id)?.nombre ?? '—';

  async function createSchedule(input: ScheduleInput) {
    setSubmitting(true);
    try {
      await schedulesApi.create(input);
      toast.success('Horario creado');
      setCreating(false);
      void schedulesQ.refetch();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo crear'));
    } finally {
      setSubmitting(false);
    }
  }

  async function updateSchedule(id: number, input: ScheduleInput) {
    setSubmitting(true);
    try {
      await schedulesApi.update(id, input);
      toast.success('Horario actualizado');
      setEditing(null);
      void schedulesQ.refetch();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo actualizar'));
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmDeleteNow() {
    if (!confirm) return;
    setDeleting(true);
    try {
      await schedulesApi.remove(confirm.id);
      toast.success('Horario eliminado');
      setConfirm(null);
      void schedulesQ.refetch();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo eliminar'));
    } finally {
      setDeleting(false);
    }
  }

  const loading = schedulesQ.loading || playlistsQ.loading;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-gray-900">Horarios</h2>
          <p className="text-sm text-gray-500">
            Programa qué playlist se reproduce en cada franja horaria.
          </p>
        </div>
        <button
          className="btn-primary"
          disabled={(playlistsQ.data?.length ?? 0) === 0}
          onClick={() => setCreating(true)}
          title={
            (playlistsQ.data?.length ?? 0) === 0
              ? 'Primero crea al menos una playlist'
              : ''
          }
        >
          <Plus className="h-4 w-4" /> Nuevo horario
        </button>
      </div>

      {/* Calendario semanal */}
      <section className="card overflow-hidden">
        <header className="border-b border-gray-200 px-4 py-3">
          <h3 className="text-sm font-semibold text-gray-900">Vista semanal</h3>
          <p className="text-xs text-gray-500">
            Cada bloque representa una playlist programada.
          </p>
        </header>
        {loading ? (
          <div className="p-4">
            <Skeleton className="h-72 w-full" />
          </div>
        ) : (
          <WeeklyCalendar
            schedules={schedulesQ.data ?? []}
            playlistColor={playlistColor}
            playlistName={playlistName}
            onClickSchedule={(s) => setEditing(s)}
          />
        )}
      </section>

      {/* Lista */}
      <section className="card overflow-hidden">
        <header className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 className="text-sm font-semibold text-gray-900">Reglas configuradas</h3>
          <span className="text-xs text-gray-500">
            {schedulesQ.data?.length ?? 0} horario(s)
          </span>
        </header>
        {loading ? (
          <div className="p-4">
            <Skeleton className="h-16 w-full" />
          </div>
        ) : (schedulesQ.data?.length ?? 0) === 0 ? (
          <div className="p-4">
            <EmptyState
              icon={CalendarPlus}
              title="No hay horarios"
              description="Crea tu primer horario para que tus TVs cambien de playlist automáticamente."
              action={
                <button
                  className="btn-primary"
                  disabled={(playlistsQ.data?.length ?? 0) === 0}
                  onClick={() => setCreating(true)}
                >
                  <Plus className="h-4 w-4" /> Crear horario
                </button>
              }
            />
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {schedulesQ.data!.map((s) => {
              const days = parseDays(s.dias_semana);
              return (
                <li
                  key={s.id}
                  className="flex flex-wrap items-center gap-3 px-4 py-3 sm:flex-nowrap"
                >
                  <span
                    className={cn(
                      'h-2.5 w-2.5 shrink-0 rounded-full',
                      s.activo ? 'bg-emerald-500' : 'bg-gray-300',
                    )}
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="truncate text-sm font-medium text-gray-900">
                        {s.nombre}
                      </span>
                      {s.activo ? (
                        <span className="badge-green">Activo</span>
                      ) : (
                        <span className="badge-gray">Pausado</span>
                      )}
                      {s.prioridad > 0 && (
                        <span className="badge-indigo">P{s.prioridad}</span>
                      )}
                    </div>
                    <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-gray-500">
                      <span>{playlistName(s.playlist_id)}</span>
                      <span>·</span>
                      <span>
                        {days
                          .map((d) => DAYS_OF_WEEK.find((x) => x.key === d)?.short)
                          .join(' ')}
                      </span>
                      <span>·</span>
                      <span>
                        {formatHm(s.hora_inicio)}–{formatHm(s.hora_fin)}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <button
                      className="btn-ghost h-8 px-2"
                      onClick={() => setEditing(s)}
                      title="Editar"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      className="btn-ghost h-8 px-2 text-red-500 hover:bg-red-50 hover:text-red-600"
                      onClick={() => setConfirm(s)}
                      title="Eliminar"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {/* Crear */}
      <Modal
        open={creating}
        onClose={() => !submitting && setCreating(false)}
        title="Nuevo horario"
        description="Define cuándo se reproducirá una playlist."
        size="lg"
      >
        <ScheduleForm
          playlists={playlistsQ.data ?? []}
          submitting={submitting}
          onCancel={() => setCreating(false)}
          onSubmit={createSchedule}
        />
      </Modal>

      {/* Editar */}
      <Modal
        open={!!editing}
        onClose={() => !submitting && setEditing(null)}
        title="Editar horario"
        size="lg"
      >
        {editing && (
          <ScheduleForm
            initial={editing}
            playlists={playlistsQ.data ?? []}
            submitting={submitting}
            onCancel={() => setEditing(null)}
            onSubmit={(input) => updateSchedule(editing.id, input)}
          />
        )}
      </Modal>

      {/* Confirmar borrar */}
      <Modal
        open={!!confirm}
        onClose={() => setConfirm(null)}
        title="Eliminar horario"
        footer={
          <>
            <button
              className="btn-secondary"
              onClick={() => setConfirm(null)}
              disabled={deleting}
            >
              Cancelar
            </button>
            <button
              className="btn-danger"
              onClick={confirmDeleteNow}
              disabled={deleting}
            >
              {deleting && <Loader2 className="h-4 w-4 animate-spin" />} Eliminar
            </button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          ¿Eliminar el horario <strong>{confirm?.nombre}</strong>?
        </p>
      </Modal>
    </div>
  );
}

interface WeeklyProps {
  schedules: Schedule[];
  playlistColor: Map<number, string>;
  playlistName: (id: number) => string;
  onClickSchedule: (s: Schedule) => void;
}

function WeeklyCalendar({
  schedules,
  playlistColor,
  playlistName,
  onClickSchedule,
}: WeeklyProps) {
  // Bloques de 1h, de 06:00 a 24:00 -> 18 filas
  const startHour = 6;
  const endHour = 24;
  const hours = Array.from({ length: endHour - startHour + 1 }, (_, i) => startHour + i);
  const HOUR_PX = 36; // alto por hora

  const days: { key: DayOfWeek; label: string }[] = DAYS_OF_WEEK.map((d) => ({
    key: d.key,
    label: d.long,
  }));

  // Agrupa schedules por día
  const byDay: Record<DayOfWeek, Schedule[]> = {
    MON: [], TUE: [], WED: [], THU: [], FRI: [], SAT: [], SUN: [],
  };
  for (const s of schedules) {
    for (const d of parseDays(s.dias_semana)) {
      byDay[d].push(s);
    }
  }

  // Asigna a cada schedule un "lane" y calcula cuantas columnas tiene
  // su grupo solapado, para que items concurrentes se rendericen lado a lado.
  type Layout = { lane: number; totalLanes: number };
  function computeLayout(daySchedules: Schedule[]): Map<number, Layout> {
    const sorted = [...daySchedules].sort(
      (a, b) => timeToMinutes(a.hora_inicio) - timeToMinutes(b.hora_inicio),
    );
    const laneOf = new Map<number, number>();
    const laneEnd: number[] = []; // hora_fin de cada lane
    for (const s of sorted) {
      const start = timeToMinutes(s.hora_inicio);
      const end = timeToMinutes(s.hora_fin);
      let lane = laneEnd.findIndex((e) => e <= start);
      if (lane === -1) { lane = laneEnd.length; laneEnd.push(end); }
      else laneEnd[lane] = end;
      laneOf.set(s.id, lane);
    }
    // totalLanes: para cada schedule, el max de lanes en su grupo solapado.
    const result = new Map<number, Layout>();
    for (const s of sorted) {
      const start = timeToMinutes(s.hora_inicio);
      const end = timeToMinutes(s.hora_fin);
      let maxLane = laneOf.get(s.id)!;
      for (const other of sorted) {
        if (other.id === s.id) continue;
        const oStart = timeToMinutes(other.hora_inicio);
        const oEnd = timeToMinutes(other.hora_fin);
        if (oStart < end && oEnd > start) {
          maxLane = Math.max(maxLane, laneOf.get(other.id)!);
        }
      }
      result.set(s.id, { lane: laneOf.get(s.id)!, totalLanes: maxLane + 1 });
    }
    return result;
  }
  const layoutByDay = new Map<DayOfWeek, Map<number, Layout>>(
    (Object.keys(byDay) as DayOfWeek[]).map((d) => [d, computeLayout(byDay[d])]),
  );

  return (
    <div className="overflow-x-auto">
      <div className="flex min-w-[720px]">
        {/* columna de horas */}
        <div className="w-14 shrink-0 border-r border-gray-200 bg-gray-50 pt-8">
          {hours.slice(0, -1).map((h) => (
            <div
              key={h}
              className="relative text-right pr-2 text-[10px] font-medium text-gray-400"
              style={{ height: HOUR_PX }}
            >
              <span className="absolute right-2 -top-1.5">{h.toString().padStart(2, '0')}:00</span>
            </div>
          ))}
        </div>
        {/* columnas por día */}
        <div className="grid flex-1 grid-cols-7">
          {days.map((d) => (
            <div key={d.key} className="border-r border-gray-200 last:border-r-0">
              <div className="sticky top-0 z-10 border-b border-gray-200 bg-white py-1.5 text-center text-[11px] font-semibold uppercase tracking-wide text-gray-500">
                {d.label.slice(0, 3)}
              </div>
              <div
                className="relative"
                style={{
                  height: HOUR_PX * (hours.length - 1),
                  backgroundImage:
                    'linear-gradient(to bottom, transparent calc(100% - 1px), #f3f4f6 calc(100% - 1px))',
                  backgroundSize: `100% ${HOUR_PX}px`,
                }}
              >
                {byDay[d.key].map((s) => {
                  const start = timeToMinutes(s.hora_inicio);
                  const end = timeToMinutes(s.hora_fin);
                  const top = ((start - startHour * 60) / 60) * HOUR_PX;
                  const height = Math.max(20, ((end - start) / 60) * HOUR_PX - 2);
                  const color = playlistColor.get(s.playlist_id) ?? PALETTE[0];
                  if (start >= endHour * 60 || end <= startHour * 60) return null;
                  const layout = layoutByDay.get(d.key)?.get(s.id) ?? { lane: 0, totalLanes: 1 };
                  const widthPct = 100 / layout.totalLanes;
                  const leftPct = layout.lane * widthPct;
                  return (
                    <button
                      key={s.id}
                      type="button"
                      onClick={() => onClickSchedule(s)}
                      style={{
                        top,
                        height,
                        left: `calc(${leftPct}% + 2px)`,
                        width: `calc(${widthPct}% - 4px)`,
                      }}
                      className={cn(
                        'absolute overflow-hidden rounded-md px-2 py-1 text-left text-[11px] leading-tight text-white shadow-sm transition-colors',
                        color,
                        !s.activo && 'opacity-50',
                      )}
                      title={`${s.nombre} · ${playlistName(s.playlist_id)} · ${formatHm(s.hora_inicio)}–${formatHm(s.hora_fin)}`}
                    >
                      <div className="truncate font-semibold">{s.nombre}</div>
                      <div className="truncate opacity-90">
                        {formatHm(s.hora_inicio)}–{formatHm(s.hora_fin)}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
