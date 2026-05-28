import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { Store, ShieldCheck, LogOut, Tv2 } from 'lucide-react';
import { useAuth } from '@/auth/AuthContext';
import { cn } from '@/lib/cn';

const NAV = [
  { to: '/admin/locales', label: 'Tiendas', icon: Store },
  { to: '/admin/admins', label: 'Administradores', icon: ShieldCheck },
];

const TITLES: Record<string, string> = {
  '/admin/locales': 'Tiendas',
  '/admin/admins': 'Administradores',
};

export function AdminLayout() {
  const { local, logout } = useAuth();
  const loc = useLocation();

  const title =
    Object.entries(TITLES).find(([k]) => loc.pathname.startsWith(k))?.[1] ??
    'Panel super-admin';

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      {/* Sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col bg-slate-900 text-slate-300 md:flex">
        <div className="flex items-center gap-2.5 border-b border-white/5 px-5 py-5">
          <div className="grid h-9 w-9 place-items-center rounded-lg bg-brand-600 text-white shadow-md">
            <Tv2 className="h-5 w-5" />
          </div>
          <div className="leading-tight">
            <div className="text-sm font-semibold text-white">SignageTV</div>
            <div className="text-xs text-slate-400">Super-admin</div>
          </div>
        </div>

        <nav className="flex-1 space-y-0.5 px-3 py-4">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-white/10 text-white'
                    : 'text-slate-400 hover:bg-white/5 hover:text-white',
                )
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* Usuario */}
        <div className="border-t border-white/5 p-3">
          <div className="rounded-lg bg-white/5 p-3">
            <div className="flex items-center gap-3">
              <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-600 text-sm font-semibold text-white">
                {(local?.nombre ?? local?.username ?? '?').charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-medium text-white">
                  {local?.nombre ?? '—'}
                </div>
                <div className="truncate text-xs text-slate-400">
                  @{local?.username}
                </div>
              </div>
              <button
                type="button"
                title="Cerrar sesión"
                onClick={logout}
                className="grid h-8 w-8 place-items-center rounded-md text-slate-400 hover:bg-white/10 hover:text-white"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </aside>

      {/* Contenido */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-6">
          <h1 className="text-base font-semibold tracking-tight text-gray-900">
            {title}
          </h1>
          <div className="flex items-center gap-3">
            <span className="badge-indigo">
              <ShieldCheck className="h-3 w-3" />
              Super-admin
            </span>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto px-6 py-6">
          <div className="mx-auto w-full max-w-7xl animate-fadeIn">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
