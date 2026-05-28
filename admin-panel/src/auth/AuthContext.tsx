import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { toast } from 'sonner';
import { auth as authApi } from '@/api/endpoints';
import { TOKEN_KEY, apiErrorMessage } from '@/api/client';
import type { Local } from '@/types';

interface AuthState {
  local: Local | null;
  token: string | null;
  loading: boolean;
  /** Devuelve el Local autenticado para que el caller pueda redirigir según rol. */
  login: (username: string, password: string) => Promise<Local>;
  logout: () => void;
}

const AuthCtx = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [local, setLocal] = useState<Local | null>(null);
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_KEY),
  );
  const [loading, setLoading] = useState<boolean>(!!localStorage.getItem(TOKEN_KEY));

  const doLogout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setLocal(null);
  }, []);

  // Bootstrap: si hay token, recuperar /auth/me
  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    authApi
      .me()
      .then((me) => {
        if (!cancelled) setLocal(me);
      })
      .catch(() => {
        if (!cancelled) doLogout();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [token, doLogout]);

  // Escucha el 401 del interceptor
  useEffect(() => {
    const handler = () => {
      doLogout();
      toast.error('Tu sesión ha expirado. Inicia sesión de nuevo.');
    };
    window.addEventListener('signagetv:unauthorized', handler);
    return () => window.removeEventListener('signagetv:unauthorized', handler);
  }, [doLogout]);

  const login = useCallback(async (username: string, password: string) => {
    try {
      const { token: newToken, local: newLocal } = await authApi.login(username, password);
      localStorage.setItem(TOKEN_KEY, newToken);
      setToken(newToken);
      setLocal(newLocal);
      return newLocal;
    } catch (err) {
      throw new Error(apiErrorMessage(err, 'Credenciales incorrectas'));
    }
  }, []);

  const value = useMemo<AuthState>(
    () => ({ local, token, loading, login, logout: doLogout }),
    [local, token, loading, login, doLogout],
  );

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return ctx;
}
