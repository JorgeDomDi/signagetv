import { useEffect, useState, type FormEvent } from 'react';
import { toast } from 'sonner';
import {
  Plus,
  Loader2,
  Store,
  Pencil,
  KeyRound,
  Trash2,
  Power,
} from 'lucide-react';
import { admin as adminApi } from '@/api/endpoints';
import { useFetch } from '@/hooks/useFetch';
import { Skeleton } from '@/components/Skeleton';
import { EmptyState } from '@/components/EmptyState';
import { Modal } from '@/components/Modal';
import { apiErrorMessage } from '@/api/client';
import { cn } from '@/lib/cn';
import type { AdminLocale } from '@/types';

export default function AdminLocales() {
  const { data, loading, error, refetch, setData } = useFetch(
    () => adminApi.listLocales(),
    [],
  );

  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<AdminLocale | null>(null);
  const [resetting, setResetting] = useState<AdminLocale | null>(null);
  const [deleting, setDeleting] = useState<AdminLocale | null>(null);
  const [togglingId, setTogglingId] = useState<number | null>(null);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-gray-900">
            Tiendas
          </h2>
          <p className="text-sm text-gray-500">
            Gestiona los locales del sistema: alta, edición, suspensión y
            eliminación.
          </p>
        </div>
        <button className="btn-primary" onClick={() => setCreating(true)}>
          <Plus className="h-4 w-4" /> Nueva tienda
        </button>
      </div>

      {loading ? (
        <div className="space-y-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-lg" />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={Store}
          title="Aún no hay tiendas registradas"
          description="Crea la primera tienda para que pueda iniciar sesión y subir contenido."
          action={
            <button className="btn-primary" onClick={() => setCreating(true)}>
              <Plus className="h-4 w-4" /> Crear tienda
            </button>
          }
        />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">Nombre</th>
                  <th className="px-4 py-3">Usuario</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3 text-right">TVs</th>
                  <th className="px-4 py-3 text-right">Media</th>
                  <th className="px-4 py-3 text-right">Playlists</th>
                  <th className="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {data.map((l) => (
                  <tr key={l.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {l.nombre}
                    </td>
                    <td className="px-4 py-3 text-gray-600">@{l.username}</td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={async () => {
                          setTogglingId(l.id);
                          try {
                            const updated = await adminApi.updateLocale(l.id, {
                              active: !l.active,
                            });
                            setData(
                              (prev) =>
                                prev?.map((x) =>
                                  x.id === l.id ? { ...x, ...updated } : x,
                                ) ?? null,
                            );
                            toast.success(
                              updated.active
                                ? 'Tienda activada'
                                : 'Tienda suspendida',
                            );
                          } catch (err) {
                            toast.error(
                              apiErrorMessage(err, 'No se pudo actualizar'),
                            );
                          } finally {
                            setTogglingId(null);
                          }
                        }}
                        disabled={togglingId === l.id}
                        className={cn(
                          'inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset transition',
                          l.active
                            ? 'bg-emerald-50 text-emerald-700 ring-emerald-200 hover:bg-emerald-100'
                            : 'bg-red-50 text-red-700 ring-red-200 hover:bg-red-100',
                          togglingId === l.id && 'opacity-60',
                        )}
                        title={
                          l.active
                            ? 'Click para suspender'
                            : 'Click para activar'
                        }
                      >
                        {togglingId === l.id ? (
                          <Loader2 className="h-3 w-3 animate-spin" />
                        ) : (
                          <Power className="h-3 w-3" />
                        )}
                        {l.active ? 'Activa' : 'Suspendida'}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right text-gray-700">
                      {l.tv_count}
                    </td>
                    <td className="px-4 py-3 text-right text-gray-700">
                      {l.media_count}
                    </td>
                    <td className="px-4 py-3 text-right text-gray-700">
                      {l.playlist_count}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          className="btn-ghost px-2 py-1"
                          title="Editar"
                          onClick={() => setEditing(l)}
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          className="btn-ghost px-2 py-1"
                          title="Resetear contraseña"
                          onClick={() => setResetting(l)}
                        >
                          <KeyRound className="h-4 w-4" />
                        </button>
                        <button
                          className="btn-ghost px-2 py-1 text-red-600 hover:bg-red-50 hover:text-red-700"
                          title="Eliminar"
                          onClick={() => setDeleting(l)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <CreateLocaleModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={(created) => {
          setData((prev) => (prev ? [created, ...prev] : [created]));
        }}
      />

      <EditLocaleModal
        locale={editing}
        onClose={() => setEditing(null)}
        onUpdated={(updated) => {
          setData(
            (prev) =>
              prev?.map((x) => (x.id === updated.id ? { ...x, ...updated } : x)) ??
              null,
          );
        }}
      />

      <ResetPasswordModal
        locale={resetting}
        onClose={() => setResetting(null)}
      />

      <DeleteLocaleModal
        locale={deleting}
        onClose={() => setDeleting(null)}
        onDeleted={(id) => {
          setData((prev) => prev?.filter((x) => x.id !== id) ?? null);
          void refetch();
        }}
      />
    </div>
  );
}

// ============================================================
//  Modales
// ============================================================

function CreateLocaleModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (l: AdminLocale) => void;
}) {
  const [nombre, setNombre] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function reset() {
    setNombre('');
    setUsername('');
    setPassword('');
    setSubmitting(false);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!nombre.trim() || !username.trim() || password.length < 6) return;
    setSubmitting(true);
    try {
      const created = await adminApi.createLocale({
        nombre: nombre.trim(),
        username: username.trim(),
        password,
      });
      toast.success('Tienda creada');
      onCreated(created);
      reset();
      onClose();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo crear la tienda'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => !submitting && (reset(), onClose())}
      title="Nueva tienda"
      description="Crea una cuenta de tienda. Podrá iniciar sesión inmediatamente con estas credenciales."
      footer={
        <>
          <button
            className="btn-secondary"
            onClick={() => {
              reset();
              onClose();
            }}
            disabled={submitting}
          >
            Cancelar
          </button>
          <button
            type="submit"
            form="create-locale-form"
            className="btn-primary"
            disabled={submitting}
          >
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />} Crear
          </button>
        </>
      }
    >
      <form id="create-locale-form" onSubmit={onSubmit} className="space-y-4">
        <div>
          <label htmlFor="nl-nombre" className="label">
            Nombre de la tienda
          </label>
          <input
            id="nl-nombre"
            className="input"
            placeholder="Ej. Sucursal Centro"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
            maxLength={120}
            autoFocus
          />
        </div>
        <div>
          <label htmlFor="nl-username" className="label">
            Usuario
          </label>
          <input
            id="nl-username"
            className="input"
            placeholder="sucursal-centro"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            minLength={3}
            maxLength={60}
            autoComplete="off"
          />
        </div>
        <div>
          <label htmlFor="nl-password" className="label">
            Contraseña inicial
          </label>
          <input
            id="nl-password"
            type="password"
            className="input"
            placeholder="Mínimo 6 caracteres"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
            maxLength={100}
            autoComplete="new-password"
          />
        </div>
      </form>
    </Modal>
  );
}

function EditLocaleModal({
  locale,
  onClose,
  onUpdated,
}: {
  locale: AdminLocale | null;
  onClose: () => void;
  onUpdated: (l: AdminLocale) => void;
}) {
  const [nombre, setNombre] = useState('');
  const [active, setActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // sincronizar cuando cambia el locale objetivo
  useStateEffect(locale, (l) => {
    if (l) {
      setNombre(l.nombre);
      setActive(l.active);
    }
  });

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!locale || !nombre.trim()) return;
    setSubmitting(true);
    try {
      const updated = await adminApi.updateLocale(locale.id, {
        nombre: nombre.trim(),
        active,
      });
      toast.success('Tienda actualizada');
      onUpdated(updated);
      onClose();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo actualizar'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open={!!locale}
      onClose={() => !submitting && onClose()}
      title="Editar tienda"
      description="Modifica el nombre o suspende la cuenta. Para cambiar la contraseña usa la acción 'resetear'."
      footer={
        <>
          <button
            className="btn-secondary"
            onClick={onClose}
            disabled={submitting}
          >
            Cancelar
          </button>
          <button
            type="submit"
            form="edit-locale-form"
            className="btn-primary"
            disabled={submitting}
          >
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />} Guardar
          </button>
        </>
      }
    >
      <form id="edit-locale-form" onSubmit={onSubmit} className="space-y-4">
        <div>
          <label className="label">Usuario</label>
          <input className="input" value={locale?.username ?? ''} disabled />
        </div>
        <div>
          <label htmlFor="el-nombre" className="label">
            Nombre
          </label>
          <input
            id="el-nombre"
            className="input"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
            maxLength={120}
            autoFocus
          />
        </div>
        <label className="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2">
          <div>
            <div className="text-sm font-medium text-gray-900">
              Cuenta activa
            </div>
            <div className="text-xs text-gray-500">
              Si la desactivas, la tienda no podrá iniciar sesión.
            </div>
          </div>
          <input
            type="checkbox"
            className="h-4 w-4 rounded text-brand-600 focus:ring-brand-500"
            checked={active}
            onChange={(e) => setActive(e.target.checked)}
          />
        </label>
      </form>
    </Modal>
  );
}

function ResetPasswordModal({
  locale,
  onClose,
}: {
  locale: AdminLocale | null;
  onClose: () => void;
}) {
  const [pwd, setPwd] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useStateEffect(locale, () => setPwd(''));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!locale || pwd.length < 6) return;
    setSubmitting(true);
    try {
      await adminApi.resetLocalePassword(locale.id, pwd);
      toast.success('Contraseña actualizada');
      onClose();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo resetear la contraseña'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open={!!locale}
      onClose={() => !submitting && onClose()}
      title="Resetear contraseña"
      description={
        locale ? `Nueva contraseña para @${locale.username}` : undefined
      }
      footer={
        <>
          <button
            className="btn-secondary"
            onClick={onClose}
            disabled={submitting}
          >
            Cancelar
          </button>
          <button
            type="submit"
            form="reset-pwd-form"
            className="btn-primary"
            disabled={submitting || pwd.length < 6}
          >
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />} Guardar
          </button>
        </>
      }
    >
      <form id="reset-pwd-form" onSubmit={onSubmit} className="space-y-4">
        <div>
          <label htmlFor="rp-pwd" className="label">
            Nueva contraseña
          </label>
          <input
            id="rp-pwd"
            type="password"
            className="input"
            placeholder="Mínimo 6 caracteres"
            value={pwd}
            onChange={(e) => setPwd(e.target.value)}
            required
            minLength={6}
            maxLength={100}
            autoComplete="new-password"
            autoFocus
          />
          <p className="mt-1.5 text-xs text-gray-500">
            La tienda deberá iniciar sesión de nuevo con esta nueva contraseña.
          </p>
        </div>
      </form>
    </Modal>
  );
}

function DeleteLocaleModal({
  locale,
  onClose,
  onDeleted,
}: {
  locale: AdminLocale | null;
  onClose: () => void;
  onDeleted: (id: number) => void;
}) {
  const [deleting, setDeleting] = useState(false);

  async function confirm() {
    if (!locale) return;
    setDeleting(true);
    try {
      await adminApi.deleteLocale(locale.id);
      toast.success('Tienda eliminada');
      onDeleted(locale.id);
      onClose();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo eliminar'));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <Modal
      open={!!locale}
      onClose={() => !deleting && onClose()}
      title="Eliminar tienda"
      description="Esta acción no se puede deshacer."
      footer={
        <>
          <button
            className="btn-secondary"
            onClick={onClose}
            disabled={deleting}
          >
            Cancelar
          </button>
          <button
            className="btn-danger"
            onClick={confirm}
            disabled={deleting}
          >
            {deleting && <Loader2 className="h-4 w-4 animate-spin" />} Eliminar
          </button>
        </>
      }
    >
      <p className="text-sm text-gray-600">
        ¿Eliminar la tienda <strong>{locale?.nombre}</strong> (
        <code>@{locale?.username}</code>)?
      </p>
      {locale && (
        <ul className="mt-3 list-disc space-y-0.5 pl-5 text-sm text-gray-600">
          <li>Se borrarán {locale.media_count} archivos de media (incluso del disco).</li>
          <li>Se eliminarán {locale.playlist_count} playlists y sus horarios.</li>
          <li>Se desvincularán {locale.tv_count} TVs.</li>
        </ul>
      )}
    </Modal>
  );
}

// Pequeño helper: actualiza estado local cada vez que cambia un valor objetivo.
// Evitamos un useEffect explícito en cada componente con esto.
function useStateEffect<T>(value: T, run: (v: T) => void) {
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => run(value), [value]);
}
