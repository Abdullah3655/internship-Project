import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fullName, initials, roleLabel } from '../lib/helpers';
import { useToast } from './Toast';
import { Avatar, Button } from './ui';

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [loggingOut, setLoggingOut] = useState(false);

  if (!user) return null;

  const isStaff = user.role === 'HR' || user.role === 'ADMIN';
  const isAdmin = user.role === 'ADMIN';
  const isInterviewer = user.role === 'INTERVIEWER';

  return (
    <div className="shell">
      <aside className="shell-aside fade-in">
        <div className="brand-block">
          <div className="brand-mark" aria-hidden>
            RP
          </div>
          <div>
            <p className="brand-name">Recruitment Platform</p>
            <p className="brand-sub">Internal hiring</p>
          </div>
        </div>

        <nav className="shell-nav">
          <NavLink to="/" end>
            Overview
          </NavLink>
          {isStaff && <NavLink to="/candidates">Candidates</NavLink>}
          {isStaff && <NavLink to="/jobs">Jobs</NavLink>}
          {isStaff && <NavLink to="/applications">Applications</NavLink>}
          {isInterviewer && <NavLink to="/my-work">My assignments</NavLink>}
          {isAdmin && <NavLink to="/admin">Team</NavLink>}
        </nav>

        <div className="shell-user">
          <Avatar label={initials(user)} size={40} />
          <div className="shell-user-meta">
            <strong>{fullName(user)}</strong>
            <span>{roleLabel(user.role)}</span>
          </div>
          <Button
            variant="ghost"
            size="sm"
            loading={loggingOut}
            onClick={async () => {
              setLoggingOut(true);
              try {
                await logout();
                toast.info('Signed out');
                navigate('/login');
              } catch {
                toast.error('Could not sign out');
                navigate('/login');
              } finally {
                setLoggingOut(false);
              }
            }}
          >
            Log out
          </Button>
        </div>
      </aside>

      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  );
}
