import { useEffect, useState, type FormEvent } from 'react';
import { authApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import { AccountStatusBadge, RoleBadge } from '../components/Badges';
import { useToast } from '../components/Toast';
import {
  Avatar,
  Button,
  ErrorBanner,
  Field,
  Input,
  LoadingBlock,
  Modal,
  Panel,
  Select,
} from '../components/ui';
import { fullName, initials } from '../lib/helpers';
import type { AccountStatus, User } from '../types';

export function AdminPage() {
  const { user: me } = useAuth();
  const toast = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showRegister, setShowRegister] = useState(false);
  const [managing, setManaging] = useState<User | null>(null);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const res = await authApi.listUsers();
      setUsers(res.items);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load team');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  function canManage(u: User): boolean {
    if (!me) return false;
    if (u.id === me.id) return false;
    if (u.role === 'ADMIN') return false;
    return true;
  }

  return (
    <div className="fade-up">
      <header className="page-head">
        <div>
          <h1>Team</h1>
          <p>User accounts and access.</p>
        </div>
        <Button onClick={() => setShowRegister(true)}>Register user</Button>
      </header>

      {error ? <ErrorBanner message={error} /> : null}
      {loading ? (
        <LoadingBlock />
      ) : users.length === 0 ? (
        <Panel>
          <p className="ui-hint">No users found.</p>
        </Panel>
      ) : (
        <Panel>
          <table className="data-table">
            <thead>
              <tr>
                <th>Person</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} style={{ cursor: 'default' }}>
                  <td>
                    <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                      <Avatar label={initials(u)} />
                      <strong>
                        {fullName(u)}
                        {me?.id === u.id ? ' (you)' : ''}
                      </strong>
                    </div>
                  </td>
                  <td>{u.email}</td>
                  <td>
                    <RoleBadge role={u.role} />
                  </td>
                  <td>
                    <AccountStatusBadge status={u.accountStatus} />
                  </td>
                  <td>
                    {canManage(u) ? (
                      <Button variant="ghost" size="sm" onClick={() => setManaging(u)}>
                        Manage
                      </Button>
                    ) : (
                      <span className="ui-hint">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      )}

      {showRegister && (
        <RegisterModal
          onClose={() => setShowRegister(false)}
          onCreated={(name) => {
            setShowRegister(false);
            toast.success(`Registered ${name}`);
            void load();
          }}
        />
      )}

      {managing && (
        <ManageUserModal
          user={managing}
          onClose={() => setManaging(null)}
          onSaved={() => {
            setManaging(null);
            void load();
          }}
        />
      )}
    </div>
  );
}

function ManageUserModal({
  user,
  onClose,
  onSaved,
}: {
  user: User;
  onClose: () => void;
  onSaved: () => void;
}) {
  const toast = useToast();
  const [role, setRole] = useState<'HR' | 'INTERVIEWER'>(
    user.role === 'INTERVIEWER' ? 'INTERVIEWER' : 'HR',
  );
  const [status, setStatus] = useState<AccountStatus>(user.accountStatus);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await authApi.updateManagedUser(user.id, { role, accountStatus: status });
      toast.success(`Updated ${fullName(user)}`);
      onSaved();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not update user';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Manage ${fullName(user)}`} onClose={onClose}>
      {error ? <ErrorBanner message={error} /> : null}
      <form className="form-grid" onSubmit={onSubmit}>
        <Field label="Email">
          <Input value={user.email} disabled readOnly />
        </Field>
        <Field label="Role">
          <Select
            value={role}
            onChange={(e) => setRole(e.target.value as 'HR' | 'INTERVIEWER')}
          >
            <option value="HR">HR</option>
            <option value="INTERVIEWER">Interviewer</option>
          </Select>
        </Field>
        <Field label="Status">
          <Select
            value={status}
            onChange={(e) => setStatus(e.target.value as AccountStatus)}
          >
            <option value="ACTIVE">Active</option>
            <option value="DISABLED">Disabled</option>
          </Select>
        </Field>
        <Button type="submit" loading={saving}>
          {saving ? 'Saving…' : 'Save changes'}
        </Button>
      </form>
    </Modal>
  );
}

function RegisterModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (name: string) => void;
}) {
  const toast = useToast();
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const kind = String(fd.get('kind')) as 'hr' | 'interviewer' | 'ldap/hr' | 'ldap/interviewer';
    const firstName = String(fd.get('firstName'));
    const lastName = String(fd.get('lastName'));
    setSaving(true);
    setError('');
    try {
      await authApi.register(kind, {
        email: String(fd.get('email')),
        password: String(fd.get('password')),
        firstName,
        lastName,
      });
      onCreated(`${firstName} ${lastName}`.trim());
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not register user';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Register user" onClose={onClose}>
      {error ? <ErrorBanner message={error} /> : null}
      <form className="form-grid" onSubmit={onSubmit}>
        <Field label="Account type">
          <Select name="kind" defaultValue="interviewer">
            <option value="hr">Local HR</option>
            <option value="interviewer">Local interviewer</option>
            <option value="ldap/hr">LDAP HR</option>
            <option value="ldap/interviewer">LDAP interviewer</option>
          </Select>
        </Field>
        <div className="form-row">
          <Field label="First name">
            <Input name="firstName" required />
          </Field>
          <Field label="Last name">
            <Input name="lastName" required />
          </Field>
        </div>
        <Field label="Email">
          <Input name="email" type="email" required />
        </Field>
        <Field label="Password" hint="Minimum 8 characters">
          <Input name="password" type="password" minLength={8} required />
        </Field>
        <Button type="submit" loading={saving}>
          {saving ? 'Creating…' : 'Create account'}
        </Button>
      </form>
    </Modal>
  );
}
