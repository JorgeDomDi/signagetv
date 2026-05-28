import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  DragDropContext,
  Droppable,
  Draggable,
  type DropResult,
} from '@hello-pangea/dnd';
import { toast } from 'sonner';
import {
  ArrowLeft,
  Film,
  GripVertical,
  Image as ImageIcon,
  Loader2,
  Plus,
  Save,
  Search,
  Settings2,
  Trash2,
} from 'lucide-react';
import {
  media as mediaApi,
  playlists as playlistsApi,
} from '@/api/endpoints';
import type { MediaItem, Playlist, PlaylistItem, TransitionType } from '@/types';
import { useFetch } from '@/hooks/useFetch';
import { usePlaylistChannel } from '@/hooks/useWebSocket';
import { Skeleton } from '@/components/Skeleton';
import { formatDuration } from '@/lib/format';
import { apiErrorMessage } from '@/api/client';
import { cn } from '@/lib/cn';

interface EditorItem {
  // ID local estable para dnd, independiente del ID del backend
  uid: string;
  media: MediaItem;
  duration_seconds: number | null;
}

const TRANSITIONS: TransitionType[] = ['FADE', 'SLIDE', 'ZOOM', 'NONE'];

function buildUid(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
}

export default function PlaylistEditor() {
  const { id } = useParams<{ id: string }>();
  const playlistId = Number(id);
  const nav = useNavigate();

  const playlistQ = useFetch(() => playlistsApi.get(playlistId), [playlistId]);
  const mediaQ = useFetch(() => mediaApi.list(), []);

  const [items, setItems] = useState<EditorItem[]>([]);
  const [search, setSearch] = useState('');
  const [name, setName] = useState('');
  const [transition, setTransition] = useState<TransitionType>('FADE');
  const [defaultSec, setDefaultSec] = useState(8);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);

  usePlaylistChannel(() => {
    // Sólo refrescamos si NO hay cambios sin guardar para no atropellar al usuario
    if (!dirty) {
      void playlistQ.refetch();
      void mediaQ.refetch();
    }
  });

  // Hidratar estado local cuando llega la playlist
  useEffect(() => {
    if (!playlistQ.data) return;
    setName(playlistQ.data.nombre);
    setTransition(playlistQ.data.transicion);
    setDefaultSec(playlistQ.data.default_image_seconds);

    const mediaById = new Map<number, MediaItem>(
      (mediaQ.data ?? []).map((m) => [m.id, m]),
    );

    const remoteItems: EditorItem[] = (playlistQ.data.items ?? [])
      .slice()
      .sort((a, b) => a.position - b.position)
      .map((it) => {
        const m = it.media ?? mediaById.get(it.media_item_id);
        if (!m) return null;
        return {
          uid: buildUid('it'),
          media: m,
          duration_seconds: it.duration_seconds ?? null,
        } satisfies EditorItem;
      })
      .filter((x): x is EditorItem => x !== null);

    setItems(remoteItems);
    setDirty(false);
  }, [playlistQ.data, mediaQ.data]);

  // Aviso al salir si hay cambios sin guardar
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (dirty) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  const filteredLibrary = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (mediaQ.data ?? []).filter(
      (m) => !q || m.filename.toLowerCase().includes(q),
    );
  }, [mediaQ.data, search]);

  function addFromLibrary(m: MediaItem) {
    setItems((prev) => [
      ...prev,
      { uid: buildUid('it'), media: m, duration_seconds: null },
    ]);
    setDirty(true);
  }

  function removeAt(index: number) {
    setItems((prev) => prev.filter((_, i) => i !== index));
    setDirty(true);
  }

  function updateDuration(index: number, value: number | null) {
    setItems((prev) =>
      prev.map((it, i) => (i === index ? { ...it, duration_seconds: value } : it)),
    );
    setDirty(true);
  }

  function onDragEnd(result: DropResult) {
    const { source, destination } = result;
    if (!destination) return;

    // Reordenamiento dentro de la playlist
    if (source.droppableId === 'playlist' && destination.droppableId === 'playlist') {
      if (source.index === destination.index) return;
      setItems((prev) => {
        const next = prev.slice();
        const [moved] = next.splice(source.index, 1);
        next.splice(destination.index, 0, moved);
        return next;
      });
      setDirty(true);
      return;
    }

    // Arrastrar de biblioteca a playlist
    if (source.droppableId === 'library' && destination.droppableId === 'playlist') {
      const m = filteredLibrary[source.index];
      if (!m) return;
      const newItem: EditorItem = {
        uid: buildUid('it'),
        media: m,
        duration_seconds: null,
      };
      setItems((prev) => {
        const next = prev.slice();
        next.splice(destination.index, 0, newItem);
        return next;
      });
      setDirty(true);
    }
  }

  async function saveAll() {
    if (!playlistQ.data) return;
    setSaving(true);
    try {
      // 1) actualizar config general si cambió
      const hadConfigChange =
        name !== playlistQ.data.nombre ||
        transition !== playlistQ.data.transicion ||
        defaultSec !== playlistQ.data.default_image_seconds;

      if (hadConfigChange) {
        await playlistsApi.update(playlistId, {
          nombre: name.trim(),
          transicion: transition,
          default_image_seconds: defaultSec,
        });
      }

      // 2) reemplazar items
      const payload: PlaylistItem[] = items.map((it, idx) => ({
        media_item_id: it.media.id,
        position: idx,
        duration_seconds:
          it.media.type === 'IMAGE' ? it.duration_seconds ?? null : null,
      }));
      await playlistsApi.setItems(playlistId, payload);

      toast.success('Cambios guardados');
      setDirty(false);
      void playlistQ.refetch();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'No se pudo guardar'));
    } finally {
      setSaving(false);
    }
  }

  if (playlistQ.loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <div className="grid grid-cols-12 gap-4">
          <Skeleton className="col-span-5 h-[60vh]" />
          <Skeleton className="col-span-7 h-[60vh]" />
        </div>
      </div>
    );
  }

  if (playlistQ.error || !playlistQ.data) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        {playlistQ.error ?? 'Playlist no encontrada'}
        <div className="mt-3">
          <Link to="/playlists" className="btn-secondary">
            <ArrowLeft className="h-4 w-4" /> Volver
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Cabecera */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <button
            type="button"
            onClick={() => nav('/playlists')}
            className="mb-2 inline-flex items-center gap-1 text-xs font-medium text-gray-500 hover:text-gray-700"
          >
            <ArrowLeft className="h-3.5 w-3.5" /> Playlists
          </button>
          <input
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setDirty(true);
            }}
            className="w-full max-w-md rounded-md border-transparent bg-transparent px-1 py-0.5 text-xl font-semibold tracking-tight text-gray-900 outline-none ring-0 hover:bg-gray-100 focus:bg-white focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div className="flex items-center gap-2">
          {dirty && (
            <span className="badge-amber">Cambios sin guardar</span>
          )}
          <button
            type="button"
            className="btn-primary"
            disabled={saving}
            onClick={saveAll}
          >
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            Guardar
          </button>
        </div>
      </div>

      {/* Config */}
      <section className="card flex flex-wrap items-end gap-4 p-4">
        <div className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-brand-50 text-brand-600">
          <Settings2 className="h-4 w-4" />
        </div>
        <div className="min-w-[180px]">
          <label className="label">Transición</label>
          <select
            className="input"
            value={transition}
            onChange={(e) => {
              setTransition(e.target.value as TransitionType);
              setDirty(true);
            }}
          >
            {TRANSITIONS.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
        <div className="min-w-[200px]">
          <label className="label">Duración por imagen por defecto (s)</label>
          <input
            type="number"
            min={1}
            max={600}
            className="input"
            value={defaultSec}
            onChange={(e) => {
              setDefaultSec(Math.max(1, parseInt(e.target.value || '1', 10)));
              setDirty(true);
            }}
          />
        </div>
        <div className="ml-auto text-right text-xs text-gray-500">
          {items.length} {items.length === 1 ? 'item' : 'items'}
        </div>
      </section>

      <DragDropContext onDragEnd={onDragEnd}>
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
          {/* Biblioteca */}
          <section className="card flex max-h-[70vh] flex-col overflow-hidden lg:col-span-5">
            <header className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
              <h3 className="text-sm font-semibold text-gray-900">Biblioteca</h3>
              <Link
                to="/media"
                className="text-xs font-medium text-brand-600 hover:text-brand-700"
              >
                Gestionar
              </Link>
            </header>
            <div className="border-b border-gray-200 px-4 py-2">
              <div className="relative">
                <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="Buscar archivo..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="input pl-9"
                />
              </div>
            </div>

            {mediaQ.loading ? (
              <div className="space-y-2 p-4">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-14 w-full" />
                ))}
              </div>
            ) : filteredLibrary.length === 0 ? (
              <div className="flex flex-1 flex-col items-center justify-center px-4 py-10 text-center text-sm text-gray-500">
                <ImageIcon className="mb-2 h-5 w-5 text-gray-400" />
                {search ? 'Sin resultados' : 'No hay medios. Sube algunos en la biblioteca.'}
              </div>
            ) : (
              <Droppable droppableId="library" isDropDisabled={true}>
                {(provided) => (
                  <ul
                    ref={provided.innerRef}
                    {...provided.droppableProps}
                    className="flex-1 space-y-1.5 overflow-y-auto p-3"
                  >
                    {filteredLibrary.map((m, idx) => (
                      <Draggable key={m.id} draggableId={`lib-${m.id}`} index={idx}>
                        {(prov, snap) => (
                          <li
                            ref={prov.innerRef}
                            {...prov.draggableProps}
                            {...prov.dragHandleProps}
                            className={cn(
                              'group flex items-center gap-3 rounded-lg border border-gray-200 bg-white p-2 transition-shadow',
                              snap.isDragging && 'shadow-cardHover ring-2 ring-brand-500',
                            )}
                          >
                            <LibraryThumb item={m} />
                            <div className="min-w-0 flex-1">
                              <div className="truncate text-sm font-medium text-gray-900">
                                {m.filename}
                              </div>
                              <div className="text-xs text-gray-500">
                                {m.type === 'VIDEO' ? 'Video' : 'Imagen'} ·{' '}
                                {m.type === 'VIDEO'
                                  ? formatDuration(m.duration_seconds ?? undefined)
                                  : 'duración configurable'}
                              </div>
                            </div>
                            <button
                              type="button"
                              className="opacity-0 transition group-hover:opacity-100"
                              onClick={() => addFromLibrary(m)}
                              title="Añadir a la playlist"
                            >
                              <span className="grid h-7 w-7 place-items-center rounded-md bg-brand-50 text-brand-600 hover:bg-brand-100">
                                <Plus className="h-4 w-4" />
                              </span>
                            </button>
                          </li>
                        )}
                      </Draggable>
                    ))}
                    {provided.placeholder}
                  </ul>
                )}
              </Droppable>
            )}
          </section>

          {/* Playlist */}
          <section className="card flex max-h-[70vh] flex-col overflow-hidden lg:col-span-7">
            <header className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
              <h3 className="text-sm font-semibold text-gray-900">
                Contenido de la playlist
              </h3>
              <span className="text-xs text-gray-500">
                Arrastra para reordenar · Click en duración para editar
              </span>
            </header>

            <Droppable droppableId="playlist">
              {(provided, snapshot) => (
                <ul
                  ref={provided.innerRef}
                  {...provided.droppableProps}
                  className={cn(
                    'flex-1 space-y-2 overflow-y-auto p-3 transition-colors',
                    snapshot.isDraggingOver && 'bg-brand-50/30',
                  )}
                >
                  {items.length === 0 && (
                    <li className="flex h-full flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-200 px-6 py-12 text-center">
                      <ImageIcon className="mb-2 h-6 w-6 text-gray-400" />
                      <p className="text-sm font-medium text-gray-700">
                        Arrastra contenido desde la biblioteca
                      </p>
                      <p className="mt-1 text-xs text-gray-500">
                        O usa el botón + en cada archivo.
                      </p>
                    </li>
                  )}
                  {items.map((it, idx) => (
                    <Draggable key={it.uid} draggableId={it.uid} index={idx}>
                      {(prov, snap) => (
                        <li
                          ref={prov.innerRef}
                          {...prov.draggableProps}
                          className={cn(
                            'flex items-center gap-3 rounded-lg border border-gray-200 bg-white p-2.5 shadow-card transition-shadow',
                            snap.isDragging && 'shadow-cardHover ring-2 ring-brand-500',
                          )}
                        >
                          <span
                            {...prov.dragHandleProps}
                            className="cursor-grab text-gray-400 hover:text-gray-600 active:cursor-grabbing"
                          >
                            <GripVertical className="h-4 w-4" />
                          </span>
                          <span className="grid h-6 w-6 shrink-0 place-items-center rounded-md bg-gray-100 text-xs font-semibold text-gray-600">
                            {idx + 1}
                          </span>
                          <LibraryThumb item={it.media} small />
                          <div className="min-w-0 flex-1">
                            <div className="truncate text-sm font-medium text-gray-900">
                              {it.media.filename}
                            </div>
                            <div className="text-xs text-gray-500">
                              {it.media.type === 'VIDEO'
                                ? `Video · ${formatDuration(
                                    it.media.duration_seconds ?? undefined,
                                  )}`
                                : 'Imagen'}
                            </div>
                          </div>

                          {it.media.type === 'IMAGE' && (
                            <div className="flex items-center gap-1.5">
                              <input
                                type="number"
                                min={1}
                                max={600}
                                value={it.duration_seconds ?? ''}
                                placeholder={`${defaultSec}`}
                                className="input w-20 text-right"
                                onChange={(e) => {
                                  const v = e.target.value;
                                  updateDuration(
                                    idx,
                                    v === '' ? null : Math.max(1, parseInt(v, 10) || 1),
                                  );
                                }}
                              />
                              <span className="text-xs text-gray-500">s</span>
                            </div>
                          )}

                          <button
                            type="button"
                            onClick={() => removeAt(idx)}
                            className="grid h-8 w-8 place-items-center rounded-md text-gray-400 hover:bg-red-50 hover:text-red-600"
                            title="Quitar"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </li>
                      )}
                    </Draggable>
                  ))}
                  {provided.placeholder}
                </ul>
              )}
            </Droppable>
          </section>
        </div>
      </DragDropContext>
    </div>
  );
}

function LibraryThumb({ item, small }: { item: MediaItem; small?: boolean }) {
  const size = small ? 'h-10 w-14' : 'h-12 w-16';
  if (item.type === 'VIDEO') {
    return (
      <div className={`relative ${size} shrink-0 overflow-hidden rounded-md bg-gray-100`}>
        <video src={mediaApi.fileUrl(item.id)} preload="metadata" muted className="h-full w-full object-cover" />
        <div className="pointer-events-none absolute inset-0 grid place-items-center bg-black/30">
          <Film className="h-3 w-3 text-white" />
        </div>
      </div>
    );
  }
  return (
    <img
      src={mediaApi.fileUrl(item.id)}
      alt={item.filename}
      className={`${size} shrink-0 rounded-md object-cover`}
      loading="lazy"
    />
  );
}

