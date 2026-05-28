import { useEffect, useState, type FormEvent } from 'react';
import { Loader2 } from 'lucide-react';
import type { DayOfWeek, Playlist, Schedule, ScheduleInput } from '@/types';
import { DayPicker } from './DayPicker';
import { TimePicker } from './TimePicker';
import { parseDays, serializeDays } from '@/lib/format';

interface Props {
  initial?: Schedule;
  playlists: Playlist[];
  submitting: boolean;
  onSubmit: (input: ScheduleInput) => Promise<void> | void;
  onCancel: () => void;
}

export function ScheduleForm({
  initial,
  playlists,
  submitting,
  onSubmit,
  onCancel,
}: Props) {
  const [nombre, setNombre] = useState(initial?.nombre ?? '');
  const [playlistId, setPlaylistId] = useState<number | ''>(
    initial?.playlist_id ?? playlists[0]?.id ?? '',
  );
  const [dias, setDias] = useState<DayOfWeek[]>(parseDays(initial?.dias_semana));
  const [horaInicio, setHoraInicio] = useState(initial?.hora_inicio?.slice(0, 5) ?? '08:00');
  const [horaFin, setHoraFin] = useState(initial?.hora_fin?.slice(0, 5) ?? '12:00');
  const [activo, setActivo] = useState(initial?.activo ?? true);
  const [prioridad, setPrioridad] = useState(initial?.prioridad ?? 0);

  useEffect(() => {
    if (!initial && playlists.length > 0 && playlistId === '') {
      setPlaylistId(playlists[0].id);
    }
  }, [playlists, initial, playlistId]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!nombre.trim() || !playlistId || dias.length === 0) return;
    if (horaFin <= horaInicio) return;
    await onSubmit({
      nombre: nombre.trim(),
      playlist_id: Number(playlistId),
      dias_semana: serializeDays(dias),
      hora_inicio: horaInicio,
      hora_fin: horaFin,
      activo,
      prioridad,
    });
  }

  const invalid =
    !nombre.trim() || !playlistId || dias.length === 0 || horaFin <= horaInicio;

  return (
    <form onSubmit={submit} className="space-y-4" id="schedule-form">
      <div>
        <label className="label">Nombre</label>
        <input
          className="input"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          placeholder="Ej. Desayuno"
          maxLength={120}
          required
          autoFocus
        />
      </div>

      <div>
        <label className="label">Playlist</label>
        <select
          className="input"
          value={playlistId}
          onChange={(e) => setPlaylistId(e.target.value ? Number(e.target.value) : '')}
          required
        >
          {playlists.length === 0 && <option value="">No hay playlists</option>}
          {playlists.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nombre}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="label">Días de la semana</label>
        <DayPicker value={dias} onChange={setDias} />
        {dias.length === 0 && (
          <p className="mt-1.5 text-xs text-amber-600">
            Selecciona al menos un día.
          </p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Hora inicio</label>
          <TimePicker value={horaInicio} onChange={setHoraInicio} />
        </div>
        <div>
          <label className="label">Hora fin</label>
          <TimePicker value={horaFin} onChange={setHoraFin} />
        </div>
      </div>
      {horaFin <= horaInicio && (
        <p className="text-xs text-amber-600">
          La hora de fin debe ser posterior a la de inicio.
        </p>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Prioridad</label>
          <input
            type="number"
            min={0}
            max={100}
            className="input"
            value={prioridad}
            onChange={(e) => setPrioridad(parseInt(e.target.value || '0', 10))}
          />
          <p className="mt-1 text-xs text-gray-500">Mayor número = prioridad más alta.</p>
        </div>
        <div className="flex flex-col">
          <label className="label">Estado</label>
          <label className="mt-1 inline-flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2">
            <input
              type="checkbox"
              checked={activo}
              onChange={(e) => setActivo(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
            />
            <span className="text-sm text-gray-700">Activo</span>
          </label>
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-2">
        <button
          type="button"
          className="btn-secondary"
          onClick={onCancel}
          disabled={submitting}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="btn-primary"
          disabled={invalid || submitting}
        >
          {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
          {initial ? 'Guardar' : 'Crear'}
        </button>
      </div>
    </form>
  );
}
