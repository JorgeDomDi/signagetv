import { Link } from 'react-router-dom';
import { ListVideo, Trash2, ChevronRight, Settings2 } from 'lucide-react';
import type { Playlist } from '@/types';
import { formatDuration } from '@/lib/format';

interface Props {
  playlist: Playlist;
  itemCount?: number;
  onDelete?: (p: Playlist) => void;
}

export function PlaylistCard({ playlist, itemCount, onDelete }: Props) {
  const count = itemCount ?? playlist.items?.length ?? 0;
  return (
    <div className="card flex flex-col p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-lg bg-brand-50 text-brand-600">
          <ListVideo className="h-5 w-5" />
        </div>
        {onDelete && (
          <button
            type="button"
            onClick={() => onDelete(playlist)}
            className="rounded-md p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600"
            title="Eliminar"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        )}
      </div>

      <div className="mt-4 min-w-0 flex-1">
        <h3 className="truncate text-base font-semibold text-gray-900">
          {playlist.nombre}
        </h3>
        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500">
          <span>{count} {count === 1 ? 'item' : 'items'}</span>
          <span>·</span>
          <span className="inline-flex items-center gap-1">
            <Settings2 className="h-3 w-3" />
            {playlist.transicion}
          </span>
          <span>·</span>
          <span>img {formatDuration(playlist.default_image_seconds)}</span>
        </div>
      </div>

      <Link
        to={`/playlists/${playlist.id}`}
        className="mt-4 inline-flex items-center justify-between rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
      >
        Editar contenido
        <ChevronRight className="h-4 w-4 text-gray-400" />
      </Link>
    </div>
  );
}
