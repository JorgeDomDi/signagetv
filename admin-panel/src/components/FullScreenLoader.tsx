import { Loader2 } from 'lucide-react';

export function FullScreenLoader({ label = 'Cargando...' }: { label?: string }) {
  return (
    <div className="grid min-h-screen place-items-center bg-gray-50 text-gray-600">
      <div className="flex flex-col items-center gap-3">
        <Loader2 className="h-6 w-6 animate-spin text-brand-600" />
        <span className="text-sm">{label}</span>
      </div>
    </div>
  );
}
