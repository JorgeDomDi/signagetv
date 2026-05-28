import { useState, type FormEvent } from 'react';
import { toast } from 'sonner';
import { Plus, Loader2, ShieldCheck, Trash2 } from 'lucide-react';
import { admin as adminApi } from '@/api/endpoints';
import { useFetch } from '@/hooks/useFetch';
import { Skeleton } from '@/components/Skeleton';
import { EmptyState } from '@/components/EmptyState';
import { Modal } from '@/components/Modal';
import { apiErrorMessage } from '@/api/client';
import { useAuth } from '@/auth/AuthContext';
import type { AdminUser } from '@/types';

function formatDate(iso?: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default function AdminAdmins() {
  const { local: me } = useAuth();
  const { data, loading, error, setData, refetch } = useFetch(
    () => adminApi.listAdmins(),
    [],
  );

  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<AdminUser | null>(null);

  // Para deshabilitar el botón delete si es el último activo
  const activeAdmins = (data ?? []).filter((a) => a.active).length;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-gray-900">
            Administradores
          </h2>
          <p className="text-sm text-gray-500">
            Usuarios con acceso al panel super-admin.
          </p>
        </div>
        <button className="btn-primary" onClick={() => setCreating(true)}>
          <Plus className="h-4 w-4" /> Nuevo admin
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
          icon={ShieldCheck}
          title="No hay administradores"
          description="Crea otro super-admin para tener respaldo en el sistema."
          action={
            <button className="btn-primary" onClick={() => setCreating(true)}>
              <Plus className="h-4 w-4" /> Crear admin
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
                  <th className="px-4 py-3">Creado</th>
                  <th className="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {data.map((a) => {
                  const isMe = me?.id === a.id;
                  const isLastActive = a.active && activeAdmins <= 1;
                  const disabled = isMe || isLastActive;
                  return (
                    <tr key={a.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium text-gray-900">
                        <div className="flex items-center gap-2">
                          {a.nombre}
                          {isMe && (
                            <span className="badge-indigo">Tú</span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-600">@{a.username}</td>
                      <td className="px-4 py-3 text-gray-600">
                        {formatDate(a.created_at)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          className="btn-ghost px-2 py-1 text-red-600 hover:bg-red-50 hover:text-red-700 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-red-600"
                          title={
                            isMe
                              ? 'No puedes eliminar tu propia cuenta'
                              : isLastActive
                                ? 'No se puede eliminar el último admin activo'
                                : 'Eliminar'
                          }
                          disabled={disabled}
                          onClick={() => setDeleting(a)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <CreateAdminModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={(created) => {
          setData((prev) => (prev ? [created, ...prev] : [created]));
        }}
      />

      <DeleteAdminModal
        admin={deleting}
        onClose={() => setDeleting(null)}
        onDeleted={(id) => {
          setData((prev) => prev?.filter((x) => x.id !== id) ?? null);
          void refetch();
        }}
      />
    </div>
  );
}

function CreateAdminModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (a: AdminUser) => void;
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
      const created = await adminApi.createAdmin({
        nombre: nombre.trim(),
        username: username.trim(),
        password,
      });
      toast.success('Administrador creado');
      onCreated(created);
      reset();
      onClose();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo crear'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => !submitting && (reset(), onClose())}
      title="Nuevo administrador"
      description="Crea otra cuenta con acceso completo al panel super-admin."
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
            form="create-admin-form"
            className="btn-primary"
            disabled={submitting}
          >
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />} Crear
          </button>
        </>
      }
    >
      <form id="create-admin-form" onSubmit={onSubmit} className="space-y-4">
        <div>
          <label htmlFor="na-nombre" className="label">
            Nombre
          </label>
          <input
            id="na-nombre"
            className="input"
            placeholder="Ej. Ana Pérez"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
            maxLength={120}
            autoFocus
          />
        </div>
        <div>
          <label htmlFor="na-username" className="label">
            Usuario
          </label>
          <input
            id="na-username"
            className="input"
            placeholder="ana.perez"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            minLength={3}
            maxLength={60}
            autoComplete="off"
          />
        </div>
        <div>
          <label htmlFor="na-password" className="label">
            Contraseña
          </label>
          <input
            id="na-password"
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

function DeleteAdminModal({
  admin,
  onClose,
  onDeleted,
}: {
  admin: AdminUser | null;
  onClose: () => void;
  onDeleted: (id: number) => void;
}) {
  const [deleting, setDeleting] = useState(false);

  async function confirm() {
    if (!admin) return;
    setDeleting(true);
    try {
      await adminApi.deleteAdmin(admin.id);
      toast.success('Administrador eliminado');
      onDeleted(admin.id);
      onClose();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo eliminar'));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <Modal
      open={!!admin}
      onClose={() => !deleting && onClose()}
      title="Eliminar administrador"
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
        ¿Eliminar al administrador <strong>{admin?.nombre}</strong> (
        <code>@{admin?.username}</code>)? Perderá inmediatamente el acceso al
        panel.
      </p>
    </Modal>
  );
}
