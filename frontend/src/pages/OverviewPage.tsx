import { Link, useNavigate } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { candidatesApi } from '../api/candidates';
import { jobsApi } from '../api/jobs';
import { useAuth } from '../auth/AuthContext';
import { StageBadge } from '../components/Badges';
import { EmptyState, LoadingBlock, Panel, RefreshHint } from '../components/ui';
import { useCachedResource } from '../hooks/useCachedResource';
import { useMyEvaluationAppIds } from '../hooks/useMyEvaluationAppIds';
import { cacheKeys } from '../lib/cache';
import { awaitingHrReview, interviewerWorkStatus, isTerminalStage } from '../lib/helpers';
import type { Candidate } from '../types';

function InterviewerOverview() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const apps = useCachedResource(cacheKeys.applications(), () => applicationsApi.list());
  const applications = apps.data?.items ?? [];
  const { evaluatedAppIds, loading: evalsLoading } = useMyEvaluationAppIds(applications, user?.id);

  if (apps.loading) return <LoadingBlock />;

  const active = applications.filter((a) => !isTerminalStage(a.currentStage));
  const closed = applications.filter((a) => isTerminalStage(a.currentStage));
  const needsFeedback = applications.filter(
    (a) => interviewerWorkStatus(a.currentStage, evaluatedAppIds.has(a.id)) === 'needs_feedback',
  );

  return (
    <div className="fade-up stack">
      <header className="page-head">
        <div>
          <h1>Overview</h1>
          <p>
            Welcome back{user ? `, ${user.firstName}` : ''}.
          </p>
        </div>
        <RefreshHint show={apps.refreshing || evalsLoading} />
      </header>

      <div className="grid-cards">
        <Panel className="stat-card">
          <h3>Active</h3>
          <strong>{active.length}</strong>
        </Panel>
        <Panel className="stat-card">
          <h3>Needs feedback</h3>
          <strong>{needsFeedback.length}</strong>
        </Panel>
        <Panel className="stat-card">
          <h3>Closed</h3>
          <strong>{closed.length}</strong>
        </Panel>
      </div>

      <Panel>
        <div className="page-head" style={{ marginBottom: '0.75rem' }}>
          <h2 style={{ fontSize: '1.2rem' }}>Your active assignments</h2>
          <Link to="/my-work">View all</Link>
        </div>
        {active.length === 0 ? (
          <EmptyState
            title="No active assignments"
            body="Assignments from HR appear here during Screening and Interview."
          />
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Job</th>
                <th>Stage</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {active.slice(0, 8).map((app) => (
                <tr key={app.id} onClick={() => navigate(`/applications/${app.id}`)}>
                  <td>
                    <strong>{app.jobTitle}</strong>
                  </td>
                  <td>
                    <StageBadge stage={app.currentStage} />
                  </td>
                  <td>{new Date(app.updatedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Panel>
    </div>
  );
}

export function OverviewPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isInterviewer = user?.role === 'INTERVIEWER';

  const apps = useCachedResource(cacheKeys.applications(), () => applicationsApi.list());
  const cands = useCachedResource(
    isInterviewer ? null : cacheKeys.candidates(),
    () => candidatesApi.list(),
    [isInterviewer],
  );
  const jobList = useCachedResource(
    isInterviewer ? null : cacheKeys.jobs(),
    () => jobsApi.list(),
    [isInterviewer],
  );

  const loading = apps.loading || (!isInterviewer && (jobList.loading || cands.loading));
  const refreshing = apps.refreshing || jobList.refreshing || cands.refreshing;

  if (isInterviewer) return <InterviewerOverview />;

  if (loading) return <LoadingBlock />;

  const applications = apps.data?.items ?? [];
  const candidates = cands.data?.items ?? ([] as Candidate[]);
  const jobs = jobList.data?.items ?? [];
  const openApps = applications.filter(
    (a) => a.currentStage !== 'HIRED' && a.currentStage !== 'DISQUALIFIED',
  );
  const publishedJobs = jobs.filter((j) => j.jobStatus === 'PUBLISHED');
  const inPool = candidates.filter((c) => c.talentStatus === 'IN_POOL');
  const readyToReview = applications.filter((a) =>
    awaitingHrReview(a.currentStage, a.evaluationCount ?? 0),
  );

  return (
    <div className="fade-up stack">
      <header className="page-head">
        <div>
          <h1>Overview</h1>
          <p>
            Welcome back{user ? `, ${user.firstName}` : ''}.
          </p>
        </div>
        <RefreshHint show={refreshing} />
      </header>

      <div className="grid-cards">
        <Panel className="stat-card">
          <h3>Talent pool</h3>
          <strong>{inPool.length}</strong>
        </Panel>
        <Panel className="stat-card">
          <h3>Published jobs</h3>
          <strong>{publishedJobs.length}</strong>
        </Panel>
        <Panel className="stat-card">
          <h3>Active applications</h3>
          <strong>{openApps.length}</strong>
        </Panel>
        <Panel className="stat-card">
          <h3>Ready to review</h3>
          <strong>{readyToReview.length}</strong>
        </Panel>
      </div>

      {readyToReview.length > 0 && (
        <Panel>
          <div className="page-head" style={{ marginBottom: '0.75rem' }}>
            <h2 style={{ fontSize: '1.2rem' }}>Interviewer feedback waiting</h2>
            <Link to="/applications">View applications</Link>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>Job</th>
                <th>Stage</th>
                <th>Evaluations</th>
              </tr>
            </thead>
            <tbody>
              {readyToReview.slice(0, 5).map((app) => (
                <tr key={app.id} onClick={() => navigate(`/applications/${app.id}`)}>
                  <td>{app.jobTitle}</td>
                  <td>
                    <StageBadge stage={app.currentStage} />
                  </td>
                  <td>{app.evaluationCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      )}

      <Panel>
        <div className="page-head" style={{ marginBottom: '0.75rem' }}>
          <h2 style={{ fontSize: '1.2rem' }}>Recent applications</h2>
          <Link to="/applications">View all</Link>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Job</th>
              <th>Stage</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {applications.slice(0, 6).map((app) => (
              <tr key={app.id} onClick={() => navigate(`/applications/${app.id}`)}>
                <td>{app.jobTitle}</td>
                <td>
                  <StageBadge stage={app.currentStage} />
                </td>
                <td>{new Date(app.updatedAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  );
}
