// Tiny re-export wrapper for clsx so we have a stable `cn` import everywhere.
import clsx, { type ClassValue } from 'clsx';

export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs);
}
