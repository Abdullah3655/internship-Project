import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { jobsApi } from '../api/jobs';
import { JobStatusBadge } from '../components/Badges';
import { useToast } from '../components/Toast';
import {
  Button,
  EmptyState,
  ErrorBanner,
  Field,
  Input,
  LoadingBlock,
  Modal,
  Panel,
  RefreshHint,
  Select,
} from '../components/ui';
import { useCachedResource } from '../hooks/useCachedResource';
import { cacheInvalidate, cacheKeys } from '../lib/cache';
import { parseTags } from '../lib/helpers';
import type { EmploymentType, Job, JobStatus } from '../types';

const STATUS_LABEL: Record<JobStatus, string> = {
  DRAFT: 'draft',
  PUBLISHED: 'published',
  CLOSED: 'closed',
};

export function JobsPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [status, setStatus] = useState<JobStatus | ''>('');
  const [showCreate, setShowCreate] = useState(false);

  const { data, loading, refreshing, error } = useCachedResource(
    cacheKeys.jobs(status || undefined),
    () => jobsApi.list(status ? { status } : undefined),
    [status],
  );

  const items = data?.items ?? [];
  const emptyTitle = status ? `No ${STATUS_LABEL[status]} jobs` : 'No jobs yet';
  const emptyBody = status
    ? 'Try another filter or clear it to see all jobs.'
    : 'Create a job to get started.';

  return (
    <div className="fade-up">
      <header className="page-head">
        <div>
          <h1>Jobs</h1>
          <p>Open roles.</p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <RefreshHint show={refreshing} />
          <Button onClick={() => setShowCreate(true)}>Create job</Button>
        </div>
      </header>

      <div className="toolbar">
        <Select
          value={status}
          onChange={(e) => setStatus(e.target.value as JobStatus | '')}
          style={{ maxWidth: 220 }}
        >
          <option value="">All statuses</option>
          <option value="DRAFT">Draft</option>
          <option value="PUBLISHED">Published</option>
          <option value="CLOSED">Closed</option>
        </Select>
      </div>

      {error ? <ErrorBanner message={error} /> : null}
      {loading ? (
        <LoadingBlock />
      ) : items.length === 0 ? (
        <Panel>
          <EmptyState
            title={emptyTitle}
            body={emptyBody}
            action={
              status ? (
                <Button variant="secondary" onClick={() => setStatus('')}>
                  Clear filter
                </Button>
              ) : (
                <Button onClick={() => setShowCreate(true)}>Create job</Button>
              )
            }
          />
        </Panel>
      ) : (
        <Panel>
          <table className="data-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Department</th>
                <th>Type</th>
                <th>Status</th>
                <th>Tags</th>
              </tr>
            </thead>
            <tbody>
              {items.map((job) => (
                <tr key={job.id} onClick={() => navigate(`/jobs/${job.id}`)}>
                  <td>
                    <strong>{job.title}</strong>
                  </td>
                  <td>{job.department || '—'}</td>
                  <td>{job.employmentType.replace('_', ' ')}</td>
                  <td>
                    <JobStatusBadge status={job.jobStatus} />
                  </td>
                  <td>
                    {job.tags.map((t) => (
                      <span key={t} className="tag-chip">
                        {t}
                      </span>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      )}

      {showCreate && (
        <CreateJobModal
          onClose={() => setShowCreate(false)}
          onCreated={(job) => {
            setShowCreate(false);
            cacheInvalidate('jobs');
            toast.success(`Created “${job.title}”`);
            navigate(`/jobs/${job.id}`);
          }}
        />
      )}
    </div>
  );
}

function CreateJobModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (job: Job) => void;
}) {
  const toast = useToast();
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSaving(true);
    setError('');
    try {
      const created = await jobsApi.create({
        title: String(fd.get('title')),
        department: String(fd.get('department') || '') || undefined,
        location: String(fd.get('location') || '') || undefined,
        description: String(fd.get('description') || '') || undefined,
        employmentType: String(fd.get('employmentType')) as EmploymentType,
        tags: parseTags(String(fd.get('tags') || '')),
      });
      onCreated(created);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not create job';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Create job" onClose={onClose} wide>
      {error ? <ErrorBanner message={error} /> : null}
      <form className="form-grid" onSubmit={onSubmit}>
        <Field label="Title">
          <Input name="title" required />
        </Field>
        <div className="form-row">
          <Field label="Department">
            <Input name="department" />
          </Field>
          <Field label="Location">
            <Input name="location" />
          </Field>
        </div>
        <Field label="Employment type">
          <Select name="employmentType" defaultValue="FULL_TIME">
            <option value="FULL_TIME">Full time</option>
            <option value="PART_TIME">Part time</option>
            <option value="CONTRACT">Contract</option>
          </Select>
        </Field>
        <Field label="Description">
          <Input name="description" />
        </Field>
        <Field label="Tags" hint="e.g. java, kafka">
          <Input name="tags" />
        </Field>
        <Button type="submit" loading={saving}>
          {saving ? 'Creating…' : 'Create draft'}
        </Button>
      </form>
    </Modal>
  );
}
