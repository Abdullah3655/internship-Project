import { useCallback, useEffect, useState } from 'react';
import { cacheGet, cachePeek, cacheSet } from '../lib/cache';

export function useCachedResource<T>(
  key: string | null,
  fetcher: () => Promise<T>,
  deps: unknown[] = [],
) {
  const initial = key ? cachePeek<T>(key) : undefined;
  const [data, setData] = useState<T | undefined>(initial);
  const [loading, setLoading] = useState(!initial && key !== null);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const reload = useCallback(
    async (opts?: { force?: boolean }) => {
      if (!key) return;
      const cached = cacheGet<T>(key);
      const hasCache = cached !== undefined && !opts?.force;

      if (hasCache) {
        setData(cached);
        setLoading(false);
        setRefreshing(true);
      } else if (data === undefined) {
        setLoading(true);
      } else {
        setRefreshing(true);
      }

      setError('');
      try {
        const fresh = await fetcher();
        cacheSet(key, fresh);
        setData(fresh);
      } catch (e) {
        if (!hasCache && data === undefined) {
          setError(e instanceof Error ? e.message : 'Failed to load');
        }
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [key, ...deps],
  );

  useEffect(() => {
    void reload();
  }, [reload]);

  return { data, loading, refreshing, error, reload, setData };
}
