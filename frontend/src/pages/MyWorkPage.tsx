import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { useAuth } from '../auth/AuthContext';
import { StageBadge } from '../components/Badges';
import { EmptyState, ErrorBanner, LoadingBlock, Panel, RefreshHint, Select } from '../components/ui';
import { useCachedResource } from '../hooks/useCachedResource';
import { useMyEvaluationAppIds } from '../hooks/useMyEvaluationAppIds';
import { cacheKeys } from '../lib/cache';
import {
  formatDate,
  interviewerWorkStatus,
  interviewerWorkStatusLabel,
} from '../lib/helpers';
import type { Application } from '../types';

type Filter = 'all' | 'active' | 'closed' | 'feedback';

export function MyWorkPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [filter, setFilter] = useState<Filter>('active');

  const { data, loading, refreshing, error } = useCachedResource(
    cacheKeys.applications(),
    () => applicationsApi.list(),
  );

  const applications = data?.items ?? [];
  const { evaluatedAppIds, loading: evalsLoading } = useMyEvaluationAppIds(applications, user?.id);

  const rows = useMemo(() => {
    return applications.filter((app) => {
      const status = interviewerWorkStatus(app.currentStage, evaluatedAppIds.has(app.id));
      if (filter === 'active') return status !== 'closed';
      if (filter === 'closed') return status === 'closed';
      if (filter === 'feedback') return status === 'needs_feedback';
      return true;
    });
  }, [applications, evaluatedAppIds, filter]);

  if (loading) return <LoadingBlock />;

  return (
    <div className="fade-up">
      <header className="page-head">
        <div>
          <h1>My assignments</h1>
          <p>Assigned interviews.</p>
        </div>
        <RefreshHint show={refreshing || evalsLoading} />
      </header>

      <div className="toolbar">
        <Select
          value={filter}
          onChange={(e) => setFilter(e.target.value as Filter)}
          style={{ maxWidth: 220 }}
        >
          <option value="active">Active</option>
          <option value="feedback">Needs feedback</option>
          <option value="closed">Closed</option>
          <option value="all">All</option>
        </Select>
      </div>

      {error ? <ErrorBanner message={error} /> : null}

      {rows.length === 0 ? (
        <Panel>
          <EmptyState
            title={
              filter === 'feedback'
                ? 'Nothing needs feedback'
                : filter === 'closed'
                  ? 'No closed assignments'
                  : 'Nothing assigned yet'
            }
            body={
              filter === 'active' || filter === 'all'
                ? 'No assignments yet.'
                : 'Try another filter.'
            }
          />
        </Panel>
      ) : (
        <Panel>
          <table className="data-table">
            <thead>
              <tr>
                <th>Job</th>
                <th>Stage</th>
                <th>Updated</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((app) => (
                <AssignmentRow
                  key={app.id}
                  app={app}
                  hasMyEvaluation={evaluatedAppIds.has(app.id)}
                  onOpen={() => navigate(`/applications/${app.id}`)}
                />
              ))}
            </tbody>
          </table>
        </Panel>
      )}
    </div>
  );
}

function AssignmentRow({
  app,
  hasMyEvaluation,
  onOpen,
}: {
  app: Application;
  hasMyEvaluation: boolean;
  onOpen: () => void;
}) {
  const status = interviewerWorkStatus(app.currentStage, hasMyEvaluation);

  return (
    <tr onClick={onOpen} style={{ cursor: 'pointer' }}>
      <td>
        <strong>{app.jobTitle}</strong>
      </td>
      <td>
        <StageBadge stage={app.currentStage} />
      </td>
      <td>{formatDate(app.updatedAt)}</td>
      <td>
        <span className="ui-hint">{interviewerWorkStatusLabel(status)}</span>
      </td>
    </tr>
  );
}
