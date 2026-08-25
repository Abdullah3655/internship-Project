import { apiRequest } from './client';
import { cacheInvalidate } from '../lib/cache';
import type { AccountStatus, AuthTokens, Role, User } from '../types';

export const authApi = {
  login(email: string, password: string) {
    return apiRequest<AuthTokens>('/api/auth/login', {
      method: 'POST',
      body: { email, password },
      auth: false,
    });
  },
  logout(refreshToken: string) {
    return apiRequest<void>('/api/auth/logout', {
      method: 'POST',
      body: { refreshToken },
      auth: false,
    });
  },
  me() {
    return apiRequest<User>('/api/auth/me');
  },
  listUsers(role?: Role) {
    const q = role ? `?role=${role}` : '';
    return apiRequest<{ items: User[] }>(`/api/auth/users${q}`);
  },
  getUser(id: string) {
    return apiRequest<User>(`/api/auth/users/${id}`);
  },
  async updateManagedUser(
    id: string,
    body: { role: 'HR' | 'INTERVIEWER'; accountStatus: AccountStatus },
  ) {
    const updated = await apiRequest<User>(`/api/auth/users/${id}`, {
      method: 'PATCH',
      body,
    });
    cacheInvalidate('auth:users');
    return updated;
  },
  async register(
    kind: 'hr' | 'interviewer' | 'ldap/hr' | 'ldap/interviewer',
    body: {
      email: string;
      password: string;
      firstName: string;
      lastName: string;
    },
  ) {
    const created = await apiRequest<User>(`/api/auth/register/${kind}`, {
      method: 'POST',
      body,
    });
    cacheInvalidate('auth:users');
    return created;
  },
};
