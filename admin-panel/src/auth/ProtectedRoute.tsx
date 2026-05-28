import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { FullScreenLoader } from '@/components/FullScreenLoader';

export function ProtectedRoute() {
  const { token, local, loading } = useAuth();
  const loc = useLocation();

  if (loading) return <FullScreenLoader label="Cargando sesión..." />;
  if (!token || !local) {
    return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  }
  return <Outlet />;
}
