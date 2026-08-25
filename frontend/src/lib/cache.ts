type Entry<T> = {
  data: T;
  at: number;
};

const store = new Map<string, Entry<unknown>>();

export const CACHE_TTL_MS = 45_000;

export function cacheGet<T>(key: string): T | undefined {
  const entry = store.get(key) as Entry<T> | undefined;
  if (!entry) return undefined;
  if (Date.now() - entry.at > CACHE_TTL_MS) {
    store.delete(key);
    return undefined;
  }
  return entry.data;
}

export function cachePeek<T>(key: string): T | undefined {
  return (store.get(key) as Entry<T> | undefined)?.data;
}

export function cacheSet<T>(key: string, data: T): void {
  store.set(key, { data, at: Date.now() });
}

export function cacheInvalidate(prefix?: string): void {
  if (!prefix) {
    store.clear();
    return;
  }
  for (const key of store.keys()) {
    if (key === prefix || key.startsWith(`${prefix}:`) || key.startsWith(prefix)) {
      store.delete(key);
    }
  }
}

export const cacheKeys = {
  me: 'auth:me',
  users: (role?: string) => (role ? `auth:users:${role}` : 'auth:users'),
  candidates: (tags?: string[]) =>
    tags?.length ? `candidates:list:${tags.join(',')}` : 'candidates:list',
  jobs: (status?: string) => (status ? `jobs:list:${status}` : 'jobs:list'),
  applications: (stage?: string) =>
    stage ? `applications:list:${stage}` : 'applications:list',
};
