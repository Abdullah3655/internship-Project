import { ApiError, type AuthTokens } from '../types';

const AUTH_URL = import.meta.env.VITE_AUTH_URL ?? 'http://localhost:8081';
const CANDIDATE_URL = import.meta.env.VITE_CANDIDATE_URL ?? 'http://localhost:8082';
const APPLICATION_URL = import.meta.env.VITE_APPLICATION_URL ?? 'http://localhost:8083';

const ACCEPT = 'application/json;version=1.0';

type TokenStore = {
  getAccess: () => string | null;
  getRefresh: () => string | null;
  setTokens: (tokens: AuthTokens) => void;
  clear: () => void;
};

let tokenStore: TokenStore = {
  getAccess: () => localStorage.getItem('access_token'),
  getRefresh: () => localStorage.getItem('refresh_token'),
  setTokens: (tokens) => {
    localStorage.setItem('access_token', tokens.accessToken);
    localStorage.setItem('refresh_token', tokens.refreshToken);
  },
  clear: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  },
};

export function configureTokenStore(store: TokenStore) {
  tokenStore = store;
}

export const urls = {
  auth: AUTH_URL,
  candidate: CANDIDATE_URL,
  application: APPLICATION_URL,
};

let refreshPromise: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return false;

  const res = await fetch(`${AUTH_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: {
      Accept: ACCEPT,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    tokenStore.clear();
    return false;
  }

  const data = (await res.json()) as AuthTokens;
  tokenStore.setTokens(data);
  return true;
}

async function ensureRefreshed(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function parseError(res: Response): Promise<ApiError> {
  try {
    const body = await res.json();
    return new ApiError(res.status, body.message ?? res.statusText);
  } catch {
    return new ApiError(res.status, res.statusText || 'Request failed');
  }
}

type RequestOptions = {
  method?: string;
  body?: unknown;
  auth?: boolean;
  formData?: FormData;
  base?: 'auth' | 'candidate' | 'application';
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const base =
    options.base === 'candidate'
      ? CANDIDATE_URL
      : options.base === 'application'
        ? APPLICATION_URL
        : AUTH_URL;

  const headers: Record<string, string> = { Accept: ACCEPT };
  if (!options.formData) {
    headers['Content-Type'] = 'application/json';
  }

  const useAuth = options.auth !== false;
  if (useAuth) {
    const access = tokenStore.getAccess();
    if (access) headers.Authorization = `Bearer ${access}`;
  }

  const init: RequestInit = {
    method: options.method ?? (options.body || options.formData ? 'POST' : 'GET'),
    headers,
    body: options.formData
      ? options.formData
      : options.body !== undefined
        ? JSON.stringify(options.body)
        : undefined,
  };

  let res = await fetch(`${base}${path}`, init);

  if (res.status === 401 && useAuth) {
    const refreshed = await ensureRefreshed();
    if (refreshed) {
      const access = tokenStore.getAccess();
      if (access) headers.Authorization = `Bearer ${access}`;
      res = await fetch(`${base}${path}`, { ...init, headers });
    }
  }

  if (res.status === 204) {
    return undefined as T;
  }

  if (!res.ok) {
    throw await parseError(res);
  }

  if (res.headers.get('content-length') === '0') {
    return undefined as T;
  }

  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export async function downloadBlob(
  path: string,
  base: 'auth' | 'candidate' | 'application' = 'candidate',
): Promise<{ blob: Blob; filename: string | null }> {
  const root =
    base === 'candidate' ? CANDIDATE_URL : base === 'application' ? APPLICATION_URL : AUTH_URL;

  const headers: Record<string, string> = {
    Accept: '*/*',
  };
  const access = tokenStore.getAccess();
  if (access) headers.Authorization = `Bearer ${access}`;

  let res = await fetch(`${root}${path}`, { headers });
  if (res.status === 401) {
    const refreshed = await ensureRefreshed();
    if (refreshed) {
      const next = tokenStore.getAccess();
      if (next) headers.Authorization = `Bearer ${next}`;
      res = await fetch(`${root}${path}`, { headers });
    }
  }

  if (!res.ok) {
    throw await parseError(res);
  }

  const disposition = res.headers.get('Content-Disposition');
  let filename: string | null = null;
  if (disposition) {
    const match = /filename="?([^";]+)"?/i.exec(disposition);
    if (match) filename = match[1];
  }

  return { blob: await res.blob(), filename };
}
