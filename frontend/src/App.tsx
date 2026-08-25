import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { AppLayout } from './components/AppLayout';
import { ToastProvider } from './components/Toast';
import { LoadingBlock } from './components/ui';
import { AdminPage } from './pages/AdminPage';
import { ApplicationDetailPage } from './pages/ApplicationDetailPage';
import { ApplicationsPage } from './pages/ApplicationsPage';
import { CandidateDetailPage } from './pages/CandidateDetailPage';
import { CandidatesPage } from './pages/CandidatesPage';
import { JobDetailPage } from './pages/JobDetailPage';
import { JobsPage } from './pages/JobsPage';
import { LoginPage } from './pages/LoginPage';
import { MyWorkPage } from './pages/MyWorkPage';
import { OverviewPage } from './pages/OverviewPage';
import type { Role } from './types';

function Protected({ roles }: { roles?: Role[] }) {
  const { user, loading } = useAuth();
  if (loading) return <LoadingBlock label="Restoring session…" />;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <Outlet />;
}

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<Protected />}>
            <Route element={<AppLayout />}>
              <Route index element={<OverviewPage />} />
              <Route element={<Protected roles={['HR', 'ADMIN']} />}>
                <Route path="candidates" element={<CandidatesPage />} />
                <Route path="candidates/:id" element={<CandidateDetailPage />} />
                <Route path="jobs" element={<JobsPage />} />
                <Route path="jobs/:id" element={<JobDetailPage />} />
                <Route path="applications" element={<ApplicationsPage />} />
              </Route>
              <Route path="applications/:id" element={<ApplicationDetailPage />} />
              <Route element={<Protected roles={['INTERVIEWER']} />}>
                <Route path="my-work" element={<MyWorkPage />} />
              </Route>
              <Route element={<Protected roles={['ADMIN']} />}>
                <Route path="admin" element={<AdminPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </ToastProvider>
  );
}
