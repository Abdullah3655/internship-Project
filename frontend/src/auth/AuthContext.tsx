import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authApi } from '../api/auth';
import { configureTokenStore } from '../api/client';
import { cacheInvalidate, cacheSet } from '../lib/cache';
import type { AuthTokens, User } from '../types';

const USER_SNAPSHOT_KEY = 'rp_user_snapshot';

type AuthState = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

function readTokens(): AuthTokens | null {
  const accessToken = localStorage.getItem('access_token');
  const refreshToken = localStorage.getItem('refresh_token');
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

function readUserSnapshot(): User | null {
  try {
    const raw = sessionStorage.getItem(USER_SNAPSHOT_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

function writeUserSnapshot(user: User | null) {
  if (!user) {
    sessionStorage.removeItem(USER_SNAPSHOT_KEY);
    return;
  }
  sessionStorage.setItem(USER_SNAPSHOT_KEY, JSON.stringify(user));
  cacheSet('auth:me', user);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const snapshot = readTokens() ? readUserSnapshot() : null;
  const [user, setUser] = useState<User | null>(snapshot);
  const [loading, setLoading] = useState(!snapshot);

  useEffect(() => {
    configureTokenStore({
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
    });
  }, []);

  const refreshUser = useCallback(async () => {
    const tokens = readTokens();
    if (!tokens) {
      setUser(null);
      writeUserSnapshot(null);
      return;
    }
    const me = await authApi.me();
    setUser(me);
    writeUserSnapshot(me);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const tokens = readTokens();
      if (!tokens) {
        if (!cancelled) {
          setUser(null);
          writeUserSnapshot(null);
          setLoading(false);
        }
        return;
      }

      if (!snapshot) setLoading(true);

      try {
        const me = await authApi.me();
        if (!cancelled) {
          setUser(me);
          writeUserSnapshot(me);
        }
      } catch {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        writeUserSnapshot(null);
        cacheInvalidate();
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password);
    localStorage.setItem('access_token', tokens.accessToken);
    localStorage.setItem('refresh_token', tokens.refreshToken);
    cacheInvalidate();
    const me = await authApi.me();
    setUser(me);
    writeUserSnapshot(me);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refresh_token');
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
    }
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    writeUserSnapshot(null);
    cacheInvalidate();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, logout, refreshUser }),
    [user, loading, login, logout, refreshUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
