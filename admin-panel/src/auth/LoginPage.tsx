import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Tv2, Loader2, Lock, User } from 'lucide-react';
import { useAuth } from './AuthContext';

export default function LoginPage() {
  const { login, token, local } = useAuth();
  const nav = useNavigate();
  const loc = useLocation() as { state?: { from?: string } };

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Si ya hay sesión, mandamos al panel adecuado según rol
  if (token && local) {
    const fallback = local.role === 'SUPER_ADMIN' ? '/admin/locales' : '/';
    return <Navigate to={loc.state?.from ?? fallback} replace />;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!username || !password) return;
    setSubmitting(true);
    try {
      const loggedIn = await login(username.trim(), password);
      toast.success('Bienvenido');
      const target =
        loc.state?.from ??
        (loggedIn.role === 'SUPER_ADMIN' ? '/admin/locales' : '/');
      nav(target, { replace: true });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'No se pudo iniciar sesión');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Hero lateral */}
      <aside className="relative hidden overflow-hidden bg-slate-900 lg:block">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(99,102,241,0.35),transparent_55%),radial-gradient(circle_at_80%_80%,rgba(79,70,229,0.25),transparent_55%)]" />
        <div className="relative z-10 flex h-full flex-col justify-between p-12 text-white">
          <div className="flex items-center gap-2.5">
            <div className="grid h-9 w-9 place-items-center rounded-lg bg-brand-600 shadow-lg">
              <Tv2 className="h-5 w-5" />
            </div>
            <span className="text-lg font-semibold tracking-tight">SignageTV</span>
          </div>

          <div>
            <h1 className="text-4xl font-semibold leading-tight tracking-tight">
              Tu cartelería digital,
              <br />
              al alcance de un clic.
            </h1>
            <p className="mt-4 max-w-md text-base text-slate-300">
              Sube contenido, crea playlists, programa horarios y controla todas tus
              pantallas Android TV en tiempo real desde un único panel.
            </p>
            <ul className="mt-8 space-y-2 text-sm text-slate-300">
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-brand-400" />
                Sincronización instantánea por WebSocket
              </li>
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-brand-400" />
                Programación por días y franjas horarias
              </li>
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-brand-400" />
                Drag & drop para organizar tu contenido
              </li>
            </ul>
          </div>

          <p className="text-xs text-slate-500">© SignageTV · Panel de administración</p>
        </div>
      </aside>

      {/* Formulario */}
      <main className="flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-2.5">
              <div className="grid h-9 w-9 place-items-center rounded-lg bg-brand-600 text-white">
                <Tv2 className="h-5 w-5" />
              </div>
              <span className="text-lg font-semibold tracking-tight text-slate-900">
                SignageTV
              </span>
            </div>
          </div>

          <h2 className="text-2xl font-semibold tracking-tight text-gray-900">
            Iniciar sesión
          </h2>
          <p className="mt-1 text-sm text-gray-500">
            Accede al panel de tu local para gestionar tu contenido.
          </p>

          <form className="mt-8 space-y-4" onSubmit={onSubmit}>
            <div>
              <label htmlFor="username" className="label">
                Usuario
              </label>
              <div className="relative">
                <User className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  id="username"
                  type="text"
                  autoComplete="username"
                  className="input pl-9"
                  placeholder="usuario-del-local"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  autoFocus
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="label">
                Contraseña
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  className="input pl-9"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" /> Entrando...
                </>
              ) : (
                'Entrar'
              )}
            </button>
          </form>

          <p className="mt-8 text-center text-xs text-gray-400">
            ¿Problemas para acceder? Contacta con el administrador del sistema.
          </p>
        </div>
      </main>
    </div>
  );
}
