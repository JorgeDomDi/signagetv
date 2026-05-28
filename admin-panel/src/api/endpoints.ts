import client, { API_BASE, TOKEN_KEY } from './client';
import type {
  AdminLocale,
  AdminLocaleCreateInput,
  AdminLocaleUpdateInput,
  AdminUser,
  AdminUserCreateInput,
  Local,
  LoginResponse,
  MediaItem,
  Playlist,
  PlaylistCreateInput,
  PlaylistItem,
  Schedule,
  ScheduleInput,
  Tv,
} from '@/types';

// ---- Auth ----
export const auth = {
  login: (username: string, password: string) =>
    client
      .post<LoginResponse>('/auth/login', { username, password })
      .then((r) => r.data),
  me: () => client.get<Local>('/auth/me').then((r) => r.data),
};

// ---- Media ----
export const media = {
  list: () => client.get<MediaItem[]>('/media').then((r) => r.data),
  upload: (file: File, onProgress?: (pct: number) => void) => {
    const fd = new FormData();
    fd.append('file', file);
    return client
      .post<MediaItem>('/media/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30 * 60 * 1000, // 30 minutos para videos pesados
        maxBodyLength: 1024 * 1024 * 1024,
        maxContentLength: 1024 * 1024 * 1024,
        onUploadProgress: (evt) => {
          if (!evt.total || !onProgress) return;
          onProgress(Math.round((evt.loaded / evt.total) * 100));
        },
      })
      .then((r) => r.data);
  },
  remove: (id: number) => client.delete<void>(`/media/${id}`).then((r) => r.data),
  /**
   * URL para usar en <img src> / <video src>. Los tags HTML no pueden enviar
   * headers, así que añadimos el JWT como query param (?token=...). El backend
   * (JwtFilter) acepta este fallback sólo para GET /api/v1/media/{id}/file.
   */
  fileUrl: (id: number) => {
    const token = localStorage.getItem(TOKEN_KEY);
    const qs = token ? `?token=${encodeURIComponent(token)}` : '';
    return `${API_BASE}/api/v1/media/${id}/file${qs}`;
  },
};

// ---- Playlists ----
export const playlists = {
  list: () => client.get<Playlist[]>('/playlists').then((r) => r.data),
  get: (id: number) => client.get<Playlist>(`/playlists/${id}`).then((r) => r.data),
  create: (input: PlaylistCreateInput) =>
    client.post<Playlist>('/playlists', input).then((r) => r.data),
  update: (id: number, input: Partial<PlaylistCreateInput>) =>
    client.put<Playlist>(`/playlists/${id}`, input).then((r) => r.data),
  remove: (id: number) => client.delete<void>(`/playlists/${id}`).then((r) => r.data),
  setItems: (id: number, items: PlaylistItem[]) =>
    client
      .put<Playlist>(`/playlists/${id}/items`, {
        items: items.map((it) => ({
          media_item_id: it.media_item_id,
          position: it.position,
          duration_seconds: it.duration_seconds ?? null,
        })),
      })
      .then((r) => r.data),
};

// ---- Schedules ----
export const schedules = {
  list: () => client.get<Schedule[]>('/schedules').then((r) => r.data),
  create: (input: ScheduleInput) =>
    client.post<Schedule>('/schedules', input).then((r) => r.data),
  update: (id: number, input: Partial<ScheduleInput>) =>
    client.put<Schedule>(`/schedules/${id}`, input).then((r) => r.data),
  remove: (id: number) => client.delete<void>(`/schedules/${id}`).then((r) => r.data),
};

// ---- TVs ----
export const tvs = {
  list: () => client.get<Tv[]>('/tvs').then((r) => r.data),
  setPlaylist: (id: number, playlistId: number | null) =>
    client
      .put<Tv>(`/tvs/${id}/playlist`, { playlist_id: playlistId })
      .then((r) => r.data),
};

// ---- Super-admin ----
// Todos los endpoints aquí requieren rol SUPER_ADMIN en el backend.
export const admin = {
  // Tiendas (locales con role=LOCAL)
  listLocales: () => client.get<AdminLocale[]>('/admin/locales').then((r) => r.data),
  createLocale: (input: AdminLocaleCreateInput) =>
    client.post<AdminLocale>('/admin/locales', input).then((r) => r.data),
  updateLocale: (id: number, input: AdminLocaleUpdateInput) =>
    client.put<AdminLocale>(`/admin/locales/${id}`, input).then((r) => r.data),
  resetLocalePassword: (id: number, newPassword: string) =>
    client
      .put<void>(`/admin/locales/${id}/password`, { new_password: newPassword })
      .then((r) => r.data),
  deleteLocale: (id: number) =>
    client.delete<void>(`/admin/locales/${id}`).then((r) => r.data),

  // Otros super-admins
  listAdmins: () => client.get<AdminUser[]>('/admin/admins').then((r) => r.data),
  createAdmin: (input: AdminUserCreateInput) =>
    client.post<AdminUser>('/admin/admins', input).then((r) => r.data),
  deleteAdmin: (id: number) =>
    client.delete<void>(`/admin/admins/${id}`).then((r) => r.data),
};
