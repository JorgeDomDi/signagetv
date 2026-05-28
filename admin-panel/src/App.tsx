import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Toaster } from 'sonner';
import { AuthProvider } from '@/auth/AuthContext';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import {
  RequireLocal,
  RequireSuperAdmin,
} from '@/auth/RequireSuperAdmin';
import LoginPage from '@/auth/LoginPage';
import { Layout } from '@/components/Layout';
import { AdminLayout } from '@/components/AdminLayout';
import { WebSocketProvider } from '@/hooks/useWebSocket';
import Dashboard from '@/pages/Dashboard';
import MediaLibrary from '@/pages/MediaLibrary';
import Playlists from '@/pages/Playlists';
import PlaylistEditor from '@/pages/PlaylistEditor';
import Schedules from '@/pages/Schedules';
import Tvs from '@/pages/Tvs';
import AdminLocales from '@/pages/admin/Locales';
import AdminAdmins from '@/pages/admin/Admins';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <WebSocketProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />

            {/* Panel super-admin */}
            <Route element={<ProtectedRoute />}>
              <Route element={<RequireSuperAdmin />}>
                <Route path="/admin" element={<AdminLayout />}>
                  <Route index element={<Navigate to="/admin/locales" replace />} />
                  <Route path="locales" element={<AdminLocales />} />
                  <Route path="admins" element={<AdminAdmins />} />
                </Route>
              </Route>
            </Route>

            {/* Panel de tienda (LOCAL) */}
            <Route element={<ProtectedRoute />}>
              <Route element={<RequireLocal />}>
                <Route element={<Layout />}>
                  <Route index element={<Dashboard />} />
                  <Route path="media" element={<MediaLibrary />} />
                  <Route path="playlists" element={<Playlists />} />
                  <Route path="playlists/:id" element={<PlaylistEditor />} />
                  <Route path="schedules" element={<Schedules />} />
                  <Route path="tvs" element={<Tvs />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>

          <Toaster
            position="top-right"
            richColors
            closeButton
            toastOptions={{ duration: 4000 }}
          />
        </WebSocketProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
