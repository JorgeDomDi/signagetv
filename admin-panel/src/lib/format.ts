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
  if (seconds < 60) return `${seconds}s`;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return s === 0 ? `${m}m` : `${m}m ${s}s`;
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
