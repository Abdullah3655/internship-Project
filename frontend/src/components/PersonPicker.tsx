import { useMemo, useState } from 'react';
import { authApi } from '../api/auth';
import { useCachedResource } from '../hooks/useCachedResource';
import { cacheKeys } from '../lib/cache';
import { fullName, initials } from '../lib/helpers';
import type { Role, User } from '../types';
import { Avatar, ErrorBanner, Input, LoadingBlock, RefreshHint } from './ui';

export function PersonPicker({
  role,
  value,
  onChange,
  excludeIds = [],
}: {
  role?: Role;
  value: string | null;
  onChange: (user: User) => void;
  excludeIds?: string[];
}) {
  const [query, setQuery] = useState('');
  const { data, loading, refreshing, error } = useCachedResource(
    cacheKeys.users(role),
    () => authApi.listUsers(role),
    [role],
  );

  const users = useMemo(
    () => (data?.items ?? []).filter((u) => u.accountStatus === 'ACTIVE'),
    [data],
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return users
      .filter((u) => !excludeIds.includes(u.id))
      .filter((u) => {
        if (!q) return true;
        return (
          fullName(u).toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q) ||
          u.role.toLowerCase().includes(q)
        );
      });
  }, [users, query, excludeIds]);

  if (loading) return <LoadingBlock label="Loading team…" />;
  if (error) return <ErrorBanner message={error} />;

  return (
    <div className="person-picker">
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
        <Input
          placeholder="Search by name or email"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search people"
        />
        <RefreshHint show={refreshing} />
      </div>
      {filtered.length === 0 ? (
        <p className="ui-hint">No matching people found.</p>
      ) : (
        filtered.map((user) => (
          <button
            key={user.id}
            type="button"
            className={`person-option ${value === user.id ? 'is-selected' : ''}`}
            onClick={() => onChange(user)}
          >
            <Avatar label={initials(user)} />
            <div>
              <strong>{fullName(user)}</strong>
              <span>
                {user.role} · {user.email}
              </span>
            </div>
          </button>
        ))
      )}
    </div>
  );
}
