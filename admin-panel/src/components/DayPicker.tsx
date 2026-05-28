import { DAYS_OF_WEEK } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { DayOfWeek } from '@/types';

interface Props {
  value: DayOfWeek[];
  onChange: (v: DayOfWeek[]) => void;
}

export function DayPicker({ value, onChange }: Props) {
  function toggle(d: DayOfWeek) {
    if (value.includes(d)) onChange(value.filter((x) => x !== d));
    else onChange([...value, d]);
  }
  return (
    <div className="flex flex-wrap gap-1.5">
      {DAYS_OF_WEEK.map((d) => {
        const active = value.includes(d.key);
        return (
          <button
            key={d.key}
            type="button"
            onClick={() => toggle(d.key)}
            title={d.long}
            className={cn(
              'grid h-9 w-9 place-items-center rounded-lg text-sm font-semibold transition-colors',
              active
                ? 'bg-brand-600 text-white shadow-sm'
                : 'border border-gray-200 bg-white text-gray-600 hover:bg-gray-50',
            )}
          >
            {d.short}
          </button>
        );
      })}
    </div>
  );
}
