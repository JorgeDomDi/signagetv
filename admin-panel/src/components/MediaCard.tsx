import { Trash2, Film, Image as ImageIcon } from 'lucide-react';
import type { MediaItem } from '@/types';
import { media } from '@/api/endpoints';
import { formatBytes, formatDuration } from '@/lib/format';
import { cn } from '@/lib/cn';

interface Props {
  item: MediaItem;
  onDelete?: (item: MediaItem) => void;
  onClick?: (item: MediaItem) => void;
  selectable?: boolean;
  selected?: boolean;
  compact?: boolean;
}

export function MediaCard({
  item,
  onDelete,
  onClick,
  selectable,
  selected,
  compact,
}: Props) {
  const isVideo = item.type === 'VIDEO';
  const src = media.fileUrl(item.id);

  return (
    <div
      className={cn(
        'group card relative overflow-hidden',
        selectable && 'cursor-pointer',
        selected && 'ring-2 ring-brand-500',
      )}
      onClick={() => onClick?.(item)}
    >
      <div
        className={cn(
          'relative w-full overflow-hidden bg-gray-100',
          compact ? 'aspect-[16/10]' : 'aspect-video',
        )}
      >
        {isVideo ? (
          <video
            src={src}
            className="h-full w-full object-cover"
            preload="metadata"
            muted
          />
        ) : (
          <img
            src={src}
            alt={item.filename}
            loading="lazy"
            className="h-full w-full object-cover"
          />
        )}
        <span className="absolute left-2 top-2 inline-flex items-center gap-1 rounded-md bg-black/60 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-white backdrop-blur">
          {isVideo ? (
            <>
              <Film className="h-3 w-3" /> Video
            </>
          ) : (
            <>
              <ImageIcon className="h-3 w-3" /> Imagen
            </>
          )}
        </span>
        {onDelete && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onDelete(item);
            }}
            className="absolute right-2 top-2 grid h-7 w-7 place-items-center rounded-md bg-white/90 text-red-600 opacity-0 shadow-sm transition-opacity hover:bg-white group-hover:opacity-100"
            title="Eliminar"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
      <div className="px-3 py-2">
        <div className="truncate text-sm font-medium text-gray-900">
          {item.filename}
        </div>
        <div className="mt-0.5 flex items-center gap-2 text-xs text-gray-500">
          <span>{formatBytes(item.size_bytes)}</span>
          {isVideo && (
            <>
              <span>·</span>
              <span>{formatDuration(item.duration_seconds ?? undefined)}</span>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
