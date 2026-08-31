import type { DayOfWeek } from '@/types';

export const DAYS_OF_WEEK: { key: DayOfWeek; short: string; long: string }[] = [
  { key: 'MON', short: 'L', long: 'Lunes' },
  { key: 'TUE', short: 'M', long: 'Martes' },
  { key: 'WED', short: 'X', long: 'Miércoles' },
  { key: 'THU', short: 'J', long: 'Jueves' },
  { key: 'FRI', short: 'V', long: 'Viernes' },
  { key: 'SAT', short: 'S', long: 'Sábado' },
  { key: 'SUN', short: 'D', long: 'Domingo' },
];

export function parseDays(csv: string | undefined | null): DayOfWeek[] {
  if (!csv) return [];
  return csv
    .split(',')
    .map((d) => d.trim().toUpperCase())
    .filter((d): d is DayOfWeek =>
      ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'].includes(d),
    );
}

export function serializeDays(days: DayOfWeek[]): string {
  // Mantener orden semana
  const order: DayOfWeek[] = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
  return order.filter((d) => days.includes(d)).join(',');
}

export function formatBytes(bytes: number | undefined): string {
  if (bytes == null || isNaN(bytes)) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

export function formatDuration(seconds: number | undefined | null): string {
  if (seconds == null) return '—';
  const total = Math.round(seconds);
  if (total < 60) return `${total}s`;
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  if (h > 0) return m === 0 ? `${h} h` : `${h} h ${m} min`;
  return s === 0 ? `${m} min` : `${m} min ${s}s`;
}

/** Formato de reproductor: 3:42 o 1:04:11. */
export function formatClock(seconds: number | undefined | null): string {
  if (seconds == null || !isFinite(seconds)) return '--:--';
  const total = Math.max(0, Math.round(seconds));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const pad = (n: number) => n.toString().padStart(2, '0');
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/**
 * Nombre legible a partir del archivo. Los descargadores de YouTube dejan cosas
 * como "YTDown.com_YouTube_Best-of-Rock-90s_nsQ188AxD7U_009_128k.mp3", que en una
 * lista no dice nada. Esto recorta la basura y deja el título.
 * El archivo original se sigue mostrando abajo, en chico.
 */
export function prettyMediaName(filename: string): string {
  let n = filename.replace(/\.[a-z0-9]{2,4}$/i, '');       // extensión
  n = n.replace(/^YTDown\.com[_\-\s]*/i, '');             // prefijo del descargador
  n = n.replace(/^YouTube[_\-\s]*/i, '');
  n = n.replace(/[_\-]+\d{2,4}k$/i, '');                   // bitrate final: _128k
  n = n.replace(/[_\-]+\d{1,4}$/, '');                    // índice final: _009
  n = n.replace(/[_\-]+[A-Za-z0-9_-]{11}$/, '');          // id de YouTube
  n = n.replace(/[_\-]+Media$/i, '');
  n = n.replace(/[_\-]+/g, ' ');                          // guiones y guiones bajos
  n = n.replace(/\s+/g, ' ').trim();
  return n || filename;
}

export function formatHm(time: string | undefined | null): string {
  if (!time) return '—';
  // backend puede mandar HH:mm o HH:mm:ss
  const [h, m] = time.split(':');
  return `${(h ?? '00').padStart(2, '0')}:${(m ?? '00').padStart(2, '0')}`;
}

export function isOnline(lastSeen?: string | null, explicit?: boolean): boolean {
  if (typeof explicit === 'boolean') return explicit;
  if (!lastSeen) return false;
  const t = new Date(lastSeen).getTime();
  if (isNaN(t)) return false;
  return Date.now() - t < 2 * 60 * 1000; // 2 min
}

export function relativeTime(iso?: string | null): string {
  if (!iso) return 'nunca';
  const t = new Date(iso).getTime();
  if (isNaN(t)) return 'nunca';
  const diff = Date.now() - t;
  const s = Math.floor(diff / 1000);
  if (s < 60) return `hace ${s}s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `hace ${m} min`;
  const h = Math.floor(m / 60);
  if (h < 24) return `hace ${h} h`;
  const d = Math.floor(h / 24);
  return `hace ${d} d`;
}
