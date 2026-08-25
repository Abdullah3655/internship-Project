import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { Button, ErrorBanner, Field, Input, Panel } from '../components/ui';

export function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [email, setEmail] = useState('hr@company.com');
  const [password, setPassword] = useState('password123');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!loading && user) return <Navigate to="/" replace />;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await login(email.trim(), password);
      toast.success('Signed in');
      navigate('/');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not sign in';
      setError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <section className="login-hero">
        <div className="login-hero-content">
          <p className="login-brand">Recruitment Platform</p>
          <h1>Sign in to continue</h1>
          <p>Manage candidates, jobs, and interviews.</p>
        </div>
      </section>

      <section className="login-panel">
        <Panel className="login-card">
          <h2>Sign in</h2>
          <p>Enter your email and password.</p>
          {error ? <ErrorBanner message={error} /> : null}
          <form className="form-grid" onSubmit={onSubmit}>
            <Field label="Email">
              <Input
                type="email"
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </Field>
            <Field label="Password">
              <Input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </Field>
            <Button type="submit" loading={submitting}>
              {submitting ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
        </Panel>
      </section>
    </div>
  );
}
