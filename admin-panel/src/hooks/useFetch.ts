import { useCallback, useEffect, useRef, useState } from 'react';
import { apiErrorMessage } from '@/api/client';

interface State<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
  setData: React.Dispatch<React.SetStateAction<T | null>>;
}

/**
 * Hook ligero para datos remotos. Devuelve {data, loading, error, refetch, setData}.
 * Cancela actualizaciones cuando el componente se desmonta.
 */
export function useFetch<T>(
  fetcher: () => Promise<T>,
  deps: React.DependencyList = [],
): State<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const run = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetcher();
      if (mounted.current) setData(res);
    } catch (err) {
      if (mounted.current) setError(apiErrorMessage(err, 'Error al cargar datos'));
    } finally {
      if (mounted.current) setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    void run();
  }, [run]);

  return { data, loading, error, refetch: run, setData };
}
