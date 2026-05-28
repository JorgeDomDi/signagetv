import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { FullScreenLoader } from '@/components/FullScreenLoader';

/**
 * Guard para rutas {@code /admin/**}. Exige sesión activa y rol SUPER_ADMIN.
 * - Sin sesión → /login
 * - Sesión LOCAL → / (panel de tienda)
 */
export function RequireSuperAdmin() {
  const { token, local, loading } = useAuth();
  const loc = useLocation();

  if (loading) return <FullScreenLoader label="Cargando sesión..." />;
  if (!token || !local) {
    return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  }
  if (local.role !== 'SUPER_ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}

/**
 * Guard para el panel de tienda. Si un super-admin intenta entrar al panel
 * normal lo mandamos al panel de gestión.
 */
export function RequireLocal() {
  const { token, local, loading } = useAuth();
  const loc = useLocation();

  if (loading) return <FullScreenLoader label="Cargando sesión..." />;
  if (!token || !local) {
    return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  }
  if (local.role === 'SUPER_ADMIN') {
    return <Navigate to="/admin/locales" replace />;
  }
  return <Outlet />;
}
