import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Draggable, Droppable } from '@hello-pangea/dnd';
import {
  GripVertical,
  Music2,
  Pause,
  Play,
  Plus,
  Search,
  Trash2,
  Volume2,
  VolumeX,
  X,
} from 'lucide-react';
import { media as mediaApi } from '@/api/endpoints';
import type { MediaItem } from '@/types';
import { formatBytes, formatClock, formatDuration, prettyMediaName } from '@/lib/format';
import { cn } from '@/lib/cn';

export interface MusicTrack {
  uid: string;
  media: MediaItem;
}

interface Props {
  /** Pistas ya agregadas a la playlist, en orden. */
  tracks: MusicTrack[];
  /** Todos los archivos de audio de la biblioteca del local. */
  audioLibrary: MediaItem[];
  onAdd: (m: MediaItem) => void;
  onRemove: (index: number) => void;
}

/**
 * Lee la duración de un archivo sin descargarlo entero: el navegador pide sólo
 * la cabecera gracias al soporte de Range del backend. Devuelve null si el
 * archivo no se puede leer o tarda demasiado, para no trabar la cola.
 */
function probeDuration(url: string): Promise<number | null> {
  return new Promise((resolve) => {
    const el = new Audio();
    let settled = false;
    const finish = (value: number | null) => {
      if (settled) return;
      settled = true;
      el.removeEventListener('loadedmetadata', onMeta);
      el.removeEventListener('error', onError);
      el.src = '';
      resolve(value);
    };
    const onMeta = () =>
      finish(isFinite(el.duration) && el.duration > 0 ? el.duration : null);
    const onError = () => finish(null);

    el.preload = 'metadata';
    el.addEventListener('loadedmetadata', onMeta);
    el.addEventListener('error', onError);
    el.src = url;
    window.setTimeout(() => finish(null), 15000);
  });
}

export function BackgroundMusicPanel({ tracks, audioLibrary, onAdd, onRemove }: Props) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const probedRef = useRef<Set<number>>(new Set());

  const [learnedDurations, setLearnedDurations] = useState<Record<number, number>>({});
  const [playingId, setPlayingId] = useState<number | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [position, setPosition] = useState(0);
  const [playerDuration, setPlayerDuration] = useState<number | null>(null);
  const [search, setSearch] = useState('');

  const durationOf = (m: MediaItem): number | null =>
    m.duration_seconds ?? learnedDurations[m.id] ?? null;

  // --- Averiguar duraciones que todavía no conocemos, de a una por vez ---
  useEffect(() => {
    const pending = audioLibrary.filter(
      (m) => m.duration_seconds == null && !probedRef.current.has(m.id),
    );
    if (pending.length === 0) return;

    let cancelled = false;
    (async () => {
      for (const m of pending) {
        if (cancelled) return;
        probedRef.current.add(m.id);
        const secs = await probeDuration(mediaApi.fileUrl(m.id));
        if (cancelled) return;
        if (secs != null) {
          setLearnedDurations((prev) => ({ ...prev, [m.id]: secs }));
          // Se guarda una sola vez: la próxima ya viene del servidor.
          void mediaApi.setDuration(m.id, secs).catch(() => undefined);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [audioLibrary]);

  const usedIds = useMemo(() => new Set(tracks.map((t) => t.media.id)), [tracks]);

  const available = useMemo(() => {
    const q = search.trim().toLowerCase();
    return audioLibrary.filter((m) => {
      if (usedIds.has(m.id)) return false;
      if (!q) return true;
      return (
        m.filename.toLowerCase().includes(q) ||
        prettyMediaName(m.filename).toLowerCase().includes(q)
      );
    });
  }, [audioLibrary, usedIds, search]);

  const totalLabel = useMemo(() => {
    if (tracks.length === 0) return null;
    let known = 0;
    let missing = 0;
    tracks.forEach((t) => {
      const d = durationOf(t.media);
      if (d == null) missing += 1;
      else known += d;
    });
    if (known === 0) return null;
    return missing > 0 ? `más de ${formatDuration(known)}` : formatDuration(known);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tracks, learnedDurations]);

  const playingMedia = useMemo(() => {
    if (playingId == null) return null;
    return (
      tracks.find((t) => t.media.id === playingId)?.media ??
      audioLibrary.find((m) => m.id === playingId) ??
      null
    );
  }, [playingId, tracks, audioLibrary]);

  function togglePlay(m: MediaItem) {
    const el = audioRef.current;
    if (!el) return;
    if (playingId === m.id) {
      if (el.paused) void el.play().catch(() => undefined);
      else el.pause();
      return;
    }
    setPlayingId(m.id);
    setPosition(0);
    setPlayerDuration(durationOf(m));
    el.src = mediaApi.fileUrl(m.id);
    void el.play().catch(() => {
      setPlayingId(null);
      setIsPlaying(false);
    });
  }

  function stopPlayback() {
    const el = audioRef.current;
    if (el) {
      el.pause();
      el.src = '';
    }
    setPlayingId(null);
    setIsPlaying(false);
    setPosition(0);
  }

  function seek(value: number) {
    const el = audioRef.current;
    if (!el || !playerDuration) return;
    el.currentTime = value;
    setPosition(value);
  }

  const hasAudio = audioLibrary.length > 0;

  return (
    <section className="card overflow-hidden">
      <header className="flex flex-col gap-2 border-b border-gray-200 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-2.5">
          <span className="mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-indigo-50 text-indigo-600">
            <Music2 className="h-4 w-4" />
          </span>
          <div>
            <h3 className="text-sm font-semibold text-gray-900">Música de fondo</h3>
            <p className="text-xs text-gray-500">
              Suena en loop mientras rotan las imágenes y videos. Al terminar la última
              pista vuelve a empezar por la primera.
            </p>
          </div>
        </div>
        <Link
          to="/media"
          className="shrink-0 text-xs font-medium text-brand-600 hover:text-brand-700"
        >
          Subir música
        </Link>
      </header>

      <div className="space-y-6 p-4">
        {/* ===== Pistas de la playlist ===== */}
        <div>
          <div className="mb-2 flex items-baseline justify-between gap-3">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-gray-500">
              En esta playlist
            </h4>
            {tracks.length > 0 && (
              <span className="text-xs text-gray-500">
                {tracks.length} {tracks.length === 1 ? 'pista' : 'pistas'}
                {totalLabel ? ` · ${totalLabel}` : ''}
              </span>
            )}
          </div>

          <Droppable droppableId="audio">
            {(provided, snapshot) => (
              <ul
                ref={provided.innerRef}
                {...provided.droppableProps}
                className={cn(
                  'space-y-2 rounded-lg transition-colors',
                  snapshot.isDraggingOver && 'bg-indigo-50/40',
                )}
              >
                {tracks.length === 0 && (
                  <li className="rounded-lg border-2 border-dashed border-gray-200 px-6 py-8 text-center text-sm text-gray-500">
                    Sin música. La playlist se reproduce con el audio propio de los videos.
                  </li>
                )}
                {tracks.map((t, idx) => {
                  const active = playingId === t.media.id;
                  return (
                    <Draggable key={t.uid} draggableId={t.uid} index={idx}>
                      {(prov, snap) => (
                        <li
                          ref={prov.innerRef}
                          {...prov.draggableProps}
                          className={cn(
                            'flex items-center gap-3 rounded-lg border bg-white p-2.5 transition-shadow',
                            active
                              ? 'border-indigo-300 bg-indigo-50/40 shadow-cardHover'
                              : 'border-gray-200 shadow-card',
                            snap.isDragging && 'shadow-cardHover ring-2 ring-indigo-500',
                          )}
                        >
                          <span
                            {...prov.dragHandleProps}
                            className="cursor-grab text-gray-400 hover:text-gray-600 active:cursor-grabbing"
                            title="Arrastrar para cambiar el orden"
                          >
                            <GripVertical className="h-4 w-4" />
                          </span>
                          <span className="w-5 shrink-0 text-center text-xs font-semibold text-gray-500">
                            {idx + 1}
                          </span>
                          <PlayButton
                            active={active}
                            playing={active && isPlaying}
                            onClick={() => togglePlay(t.media)}
                          />
                          <TrackLabel media={t.media} />
                          <span className="shrink-0 text-xs tabular-nums text-gray-500">
                            {formatClock(durationOf(t.media))}
                          </span>
                          <button
                            type="button"
                            onClick={() => {
                              if (playingId === t.media.id) stopPlayback();
                              onRemove(idx);
                            }}
                            className="grid h-8 w-8 shrink-0 place-items-center rounded-md text-gray-400 hover:bg-red-50 hover:text-red-600"
                            title="Quitar de la playlist"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </li>
                      )}
                    </Draggable>
                  );
                })}
                {provided.placeholder}
              </ul>
            )}
          </Droppable>

          {tracks.length > 0 && (
            <p className="mt-2 flex items-start gap-1.5 text-xs text-gray-500">
              <VolumeX className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gray-400" />
              La música tiene prioridad: mientras haya pistas, los videos de esta
              playlist se reproducen sin sonido.
            </p>
          )}
        </div>

        {/* ===== Biblioteca de audio ===== */}
        <div>
          <div className="mb-2 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <h4 className="text-xs font-semibold uppercase tracking-wide text-gray-500">
              Disponibles en la biblioteca
            </h4>
            {audioLibrary.length > 5 && (
              <div className="relative sm:w-64">
                <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="Buscar pista..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="input pl-9"
                />
              </div>
            )}
          </div>

          {!hasAudio ? (
            <div className="rounded-lg border-2 border-dashed border-gray-200 px-6 py-8 text-center">
              <Music2 className="mx-auto mb-2 h-5 w-5 text-gray-400" />
              <p className="text-sm font-medium text-gray-700">
                No hay archivos de audio en la biblioteca
              </p>
              <p className="mt-1 text-xs text-gray-500">
                Subí tus compilados (MP3, M4A, WAV...) en la{' '}
                <Link to="/media" className="font-medium text-brand-600 hover:underline">
                  Biblioteca
                </Link>{' '}
                y después agregalos acá.
              </p>
            </div>
          ) : available.length === 0 ? (
            <p className="rounded-lg border border-gray-200 px-4 py-6 text-center text-sm text-gray-500">
              {search
                ? 'Ninguna pista coincide con la búsqueda.'
                : 'Ya agregaste todas las pistas de la biblioteca.'}
            </p>
          ) : (
            <ul className="max-h-72 space-y-2 overflow-y-auto pr-1">
              {available.map((m) => {
                const active = playingId === m.id;
                return (
                  <li
                    key={m.id}
                    className={cn(
                      'flex items-center gap-3 rounded-lg border p-2.5 transition-colors',
                      active
                        ? 'border-indigo-300 bg-indigo-50/40'
                        : 'border-gray-200 bg-white hover:border-gray-300',
                    )}
                  >
                    <PlayButton
                      active={active}
                      playing={active && isPlaying}
                      onClick={() => togglePlay(m)}
                    />
                    <TrackLabel media={m} />
                    <span className="shrink-0 text-xs tabular-nums text-gray-500">
                      {formatClock(durationOf(m))}
                    </span>
                    <button
                      type="button"
                      onClick={() => onAdd(m)}
                      className="inline-flex shrink-0 items-center gap-1 rounded-md bg-brand-50 px-2.5 py-1.5 text-xs font-medium text-brand-700 transition-colors hover:bg-brand-100"
                      title="Agregar a la música de fondo"
                    >
                      <Plus className="h-3.5 w-3.5" /> Agregar
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>

      {/* ===== Reproductor de escucha previa ===== */}
      {playingMedia && (
        <div className="sticky bottom-0 flex items-center gap-3 border-t border-gray-200 bg-white/95 px-4 py-3 backdrop-blur">
          <PlayButton
            active
            playing={isPlaying}
            onClick={() => togglePlay(playingMedia)}
          />
          <div className="min-w-0 flex-1">
            <div className="flex items-baseline gap-2">
              <Volume2 className="h-3.5 w-3.5 shrink-0 text-indigo-500" />
              <span className="truncate text-sm font-medium text-gray-900">
                {prettyMediaName(playingMedia.filename)}
              </span>
            </div>
            <div className="mt-1.5 flex items-center gap-2">
              <span className="w-10 shrink-0 text-right text-[11px] tabular-nums text-gray-500">
                {formatClock(position)}
              </span>
              <input
                type="range"
                min={0}
                max={playerDuration ?? 0}
                step={1}
                value={Math.min(position, playerDuration ?? 0)}
                disabled={!playerDuration}
                onChange={(e) => seek(Number(e.target.value))}
                className="h-1.5 flex-1 cursor-pointer appearance-none rounded-full bg-gray-200 accent-indigo-600"
                aria-label="Posición de la pista"
              />
              <span className="w-10 shrink-0 text-[11px] tabular-nums text-gray-500">
                {formatClock(playerDuration)}
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={stopPlayback}
            className="grid h-8 w-8 shrink-0 place-items-center rounded-md text-gray-400 hover:bg-gray-100 hover:text-gray-700"
            title="Cerrar el reproductor"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      <audio
        ref={audioRef}
        preload="metadata"
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={() => {
          setIsPlaying(false);
          setPosition(0);
        }}
        onTimeUpdate={(e) => setPosition(e.currentTarget.currentTime)}
        onLoadedMetadata={(e) => {
          const d = e.currentTarget.duration;
          if (isFinite(d) && d > 0) {
            setPlayerDuration(d);
            if (playingId != null && !learnedDurations[playingId]) {
              setLearnedDurations((prev) => ({ ...prev, [playingId]: d }));
            }
          }
        }}
      />
    </section>
  );
}

function PlayButton({
  active,
  playing,
  onClick,
}: {
  active: boolean;
  playing: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'grid h-9 w-9 shrink-0 place-items-center rounded-full transition-colors',
        active
          ? 'bg-indigo-600 text-white hover:bg-indigo-700'
          : 'bg-indigo-50 text-indigo-600 hover:bg-indigo-100',
      )}
      title={playing ? 'Pausar' : 'Escuchar'}
    >
      {playing ? <Pause className="h-4 w-4" /> : <Play className="ml-0.5 h-4 w-4" />}
    </button>
  );
}

function TrackLabel({ media }: { media: MediaItem }) {
  return (
    <div className="min-w-0 flex-1">
      <div
        className="truncate text-sm font-medium text-gray-900"
        title={media.filename}
      >
        {prettyMediaName(media.filename)}
      </div>
      <div className="truncate text-[11px] text-gray-400" title={media.filename}>
        {media.filename} · {formatBytes(media.size_bytes)}
      </div>
    </div>
  );
}
