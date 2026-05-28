// Tipos compartidos del dominio SignageTV.
// Espejo del modelo descrito en docs/ARCHITECTURE.md.
// Los IDs son Long en Java -> number en TS.

export type UserRole = 'LOCAL' | 'SUPER_ADMIN';

export interface Local {
  id: number;
  nombre: string;
  username: string;
  role: UserRole;
  active: boolean;
  created_at?: string;
}

export interface LoginResponse {
  token: string;
  local: Local;
}

// ---- Super-admin panel ----

export interface AdminLocale {
  id: number;
  nombre: string;
  username: string;
  active: boolean;
  created_at?: string;
  tv_count: number;
  media_count: number;
  playlist_count: number;
}

export interface AdminLocaleCreateInput {
  nombre: string;
  username: string;
  password: string;
}

export interface AdminLocaleUpdateInput {
  nombre?: string;
  active?: boolean;
}

export interface AdminUser {
  id: number;
  nombre: string;
  username: string;
  active: boolean;
  created_at?: string;
}

export interface AdminUserCreateInput {
  nombre: string;
  username: string;
  password: string;
}

export type MediaType = 'IMAGE' | 'VIDEO';

export interface MediaItem {
  id: number;
  local_id: number;
  filename: string;
  storage_path?: string;
  type: MediaType;
  mime_type: string;
  size_bytes: number;
  duration_seconds?: number | null;
  created_at?: string;
}

export type TransitionType = 'FADE' | 'SLIDE' | 'ZOOM' | 'NONE';

export interface PlaylistItem {
  id?: number;
  media_item_id: number;
  position: number;
  duration_seconds?: number | null;
  // Hidratado por el cliente cuando el backend incluye el media expandido
  media?: MediaItem;
}

export interface Playlist {
  id: number;
  local_id: number;
  nombre: string;
  transicion: TransitionType;
  default_image_seconds: number;
  updated_at?: string;
  items?: PlaylistItem[];
}

export interface PlaylistCreateInput {
  nombre: string;
  transicion: TransitionType;
  default_image_seconds: number;
}

export type DayOfWeek = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN';

export interface Schedule {
  id: number;
  local_id: number;
  playlist_id: number;
  nombre: string;
  // CSV bitmask: "MON,TUE,WED,THU,FRI"
  dias_semana: string;
  // HH:mm o HH:mm:ss
  hora_inicio: string;
  hora_fin: string;
  activo: boolean;
  prioridad: number;
}

export interface ScheduleInput {
  playlist_id: number;
  nombre: string;
  dias_semana: string;
  hora_inicio: string;
  hora_fin: string;
  activo: boolean;
  prioridad: number;
}

export interface Tv {
  id: number;
  local_id: number;
  nombre: string;
  device_id: string;
  current_playlist_id?: number | null;
  last_seen?: string | null;
  online?: boolean;
}

export interface ApiError {
  message: string;
  status?: number;
}
