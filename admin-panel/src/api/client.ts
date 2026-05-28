import axios, { AxiosError, type AxiosInstance } from 'axios';

export const API_BASE = import.meta.env.VITE_API_URL || '';
export const WS_BASE = import.meta.env.VITE_WS_URL || API_BASE;

export const TOKEN_KEY = 'signagetv.token';

const client: AxiosInstance = axios.create({
  baseURL: `${API_BASE}/api/v1`,
  timeout: 30_000,
});

// --- Interceptor request: inyecta Bearer ---
client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// --- Interceptor response: 401 -> logout ---
client.interceptors.response.use(
  (resp) => resp,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Solo hacemos logout si había un token (evita kick durante el propio login)
      const hadToken = !!localStorage.getItem(TOKEN_KEY);
      if (hadToken) {
        localStorage.removeItem(TOKEN_KEY);
        // Notificar a la app — escuchado en AuthContext
        window.dispatchEvent(new CustomEvent('signagetv:unauthorized'));
      }
    }
    return Promise.reject(error);
  },
);

export function apiErrorMessage(err: unknown, fallback = 'Algo salió mal'): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string; error?: string } | undefined;
    return data?.message || data?.error || err.message || fallback;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}

export default client;
