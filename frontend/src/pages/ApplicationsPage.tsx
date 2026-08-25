import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { candidatesApi } from '../api/candidates';
import { jobsApi } from '../api/jobs';
import { useAuth } from '../auth/AuthContext';
import { StageBadge } from '../components/Badges';
import { useToast } from '../components/Toast';
import {
  Button,
  EmptyState,
  ErrorBanner,
  Field,
  LoadingBlock,
  Modal,
  Panel,
  RefreshHint,
  Select,
} from '../components/ui';
import { useCachedResource } from '../hooks/useCachedResource';
import { cacheGet, cacheInvalidate, cacheKeys, cacheSet } from '../lib/cache';
import { awaitingHrReview, stageLabel } from '../lib/helpers';
import type { Application, Candidate, Job, PipelineStage } from '../types';

const STAGES: PipelineStage[] = [
  'APPLIED',
  'SCREENING',
  'INTERVIEW',
  'OFFER',
  'HIRED',
  'DISQUALIFIED',
];

export function ApplicationsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [stage, setStage] = useState<PipelineStage | ''>('');
  const [readyOnly, setReadyOnly] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const canCreate = user?.role === 'HR' || user?.role === 'ADMIN';

  const { data, loading, refreshing, error } = useCachedResource(
    cacheKeys.applications(stage || undefined),
    () => applicationsApi.list(stage ? { stage } : undefined),
    [stage],
  );

  const items = (data?.items ?? []).filter((app) =>
    readyOnly ? awaitingHrReview(app.currentStage, app.evaluationCount ?? 0) : true,
  );
  const readyCount = (data?.items ?? []).filter((app) =>
    awaitingHrReview(app.currentStage, app.evaluationCount ?? 0),
  ).length;

  return (
    <div className="fade-up">
      <header className="page-head">
        <div>
          <h1>Applications</h1>
          <p>Hiring pipeline.</p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <RefreshHint show={refreshing} />
          {canCreate && <Button onClick={() => setShowCreate(true)}>New application</Button>}
        </div>
      </header>

      <div className="toolbar">
        <Select
          value={stage}
          onChange={(e) => setStage(e.target.value as PipelineStage | '')}
          style={{ maxWidth: 240 }}
        >
          <option value="">All stages</option>
          {STAGES.map((s) => (
            <option key={s} value={s}>
              {stageLabel(s)}
            </option>
          ))}
        </Select>
        {canCreate && (
          <Button
            variant={readyOnly ? 'primary' : 'secondary'}
            size="sm"
            onClick={() => setReadyOnly((v) => !v)}
          >
            Ready to review{readyCount > 0 ? ` (${readyCount})` : ''}
          </Button>
        )}
      </div>

      {error ? <ErrorBanner message={error} /> : null}
      {loading ? (
        <LoadingBlock />
      ) : items.length === 0 ? (
        <Panel>
          <EmptyState
            title={
              readyOnly
                ? 'Nothing ready to review'
                : stage
                  ? `No applications in ${stageLabel(stage)}`
                  : 'No applications'
            }
            body={
              readyOnly
                ? 'No applications with new interviewer feedback right now.'
                : stage
                  ? 'Clear the filter to see all applications.'
                  : canCreate
                    ? 'Create an application to get started.'
                    : 'No applications yet.'
            }
            action={
              readyOnly ? (
                <Button variant="secondary" onClick={() => setReadyOnly(false)}>
                  Show all
                </Button>
              ) : stage ? (
                <Button variant="secondary" onClick={() => setStage('')}>
                  Clear filter
                </Button>
              ) : canCreate ? (
                <Button onClick={() => setShowCreate(true)}>New application</Button>
              ) : undefined
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
                <th>Feedback</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {items.map((app) => (
                <tr key={app.id} onClick={() => navigate(`/applications/${app.id}`)}>
                  <td>
                    <strong>{app.jobTitle}</strong>
                  </td>
                  <td>
                    <StageBadge stage={app.currentStage} />
                  </td>
                  <td>
                    {awaitingHrReview(app.currentStage, app.evaluationCount ?? 0) ? (
                      <span className="tag-chip" style={{ background: 'var(--accent-soft)', color: 'var(--accent-deep)' }}>
                        Ready · {app.evaluationCount}
                      </span>
                    ) : (app.evaluationCount ?? 0) > 0 ? (
                      <span className="ui-hint">{app.evaluationCount} submitted</span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{new Date(app.updatedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      )}

      {showCreate && (
        <CreateApplicationModal
          onClose={() => setShowCreate(false)}
          onCreated={(app) => {
            setShowCreate(false);
            cacheInvalidate('applications');
            toast.success(`Application created for ${app.jobTitle}`);
            navigate(`/applications/${app.id}`);
          }}
        />
      )}
    </div>
  );
}

function CreateApplicationModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (app: Application) => void;
}) {
  const toast = useToast();
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [candidateId, setCandidateId] = useState('');
  const [jobId, setJobId] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const candKey = cacheKeys.candidates();
        const jobKey = cacheKeys.jobs('PUBLISHED');
        const cachedC = cacheGet<{ items: Candidate[] }>(candKey);
        const cachedJ = cacheGet<{ items: Job[] }>(jobKey);

        const [c, j] = await Promise.all([
          cachedC
            ? Promise.resolve(cachedC)
            : candidatesApi.list().then((res) => {
                cacheSet(candKey, res);
                return res;
              }),
          cachedJ
            ? Promise.resolve(cachedJ)
            : jobsApi.list({ status: 'PUBLISHED' }).then((res) => {
                cacheSet(jobKey, res);
                return res;
              }),
        ]);
        if (!cancelled) {
          setCandidates(c.items.filter((x) => x.talentStatus === 'IN_POOL'));
          setJobs(j.items);
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load options');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const candidateOptions = useMemo(
    () =>
      candidates.map((c) => ({
        id: c.id,
        label: `${c.firstName} ${c.lastName} · ${c.email}`,
      })),
    [candidates],
  );

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!candidateId || !jobId) {
      const msg = 'Select both a candidate and a published job';
      setError(msg);
      toast.error(msg);
      return;
    }
    setSaving(true);
    setError('');
    try {
      const created = await applicationsApi.create({ candidateId, jobId });
      onCreated(created);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not create application';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="New application" onClose={onClose}>
      {error ? <ErrorBanner message={error} /> : null}
      {loading ? (
        <LoadingBlock />
      ) : (
        <form className="form-grid" onSubmit={onSubmit}>
          <Field label="Candidate">
            <Select value={candidateId} onChange={(e) => setCandidateId(e.target.value)} required>
              <option value="">Select candidate</option>
              {candidateOptions.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Published job">
            <Select value={jobId} onChange={(e) => setJobId(e.target.value)} required>
              <option value="">Select job</option>
              {jobs.map((j) => (
                <option key={j.id} value={j.id}>
                  {j.title}
                </option>
              ))}
            </Select>
          </Field>
          <Button type="submit" loading={saving}>
            {saving ? 'Creating…' : 'Create application'}
          </Button>
        </form>
      )}
    </Modal>
  );
}
