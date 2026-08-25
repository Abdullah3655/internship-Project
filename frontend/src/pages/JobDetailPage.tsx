import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { jobsApi } from '../api/jobs';
import { useAuth } from '../auth/AuthContext';
import { JobStatusBadge } from '../components/Badges';
import { useToast } from '../components/Toast';
import {
  Button,
  ConfirmModal,
  ErrorBanner,
  Field,
  Input,
  LoadingBlock,
  Panel,
  Select,
  TextArea,
} from '../components/ui';
import { cacheInvalidate } from '../lib/cache';
import { parseTags } from '../lib/helpers';
import type { EmploymentType, Job, JobStatus } from '../types';

export function JobDetailPage() {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const canEdit = user?.role === 'HR' || user?.role === 'ADMIN';

  async function load() {
    setLoading(true);
    try {
      setJob(await jobsApi.get(id));
      setError('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Job not found');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [id]);

  if (loading) return <LoadingBlock />;
  if (!job) return <ErrorBanner message={error || 'Job not found'} />;

  return (
    <div className="fade-up stack">
      <header className="page-head">
        <div>
          <p className="ui-hint">
            <Link to="/jobs">Jobs</Link>
          </p>
          <h1>{job.title}</h1>
          <p>
            {[job.department, job.location].filter(Boolean).join(' · ') || 'No location set'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <JobStatusBadge status={job.jobStatus} />
          {canEdit && job.jobStatus === 'DRAFT' && (
            <Button
              loading={publishing}
              onClick={async () => {
                setPublishing(true);
                try {
                  setJob(await jobsApi.publish(job.id));
                  toast.success('Job published');
                  cacheInvalidate('jobs');
                  cacheInvalidate('applications');
                } catch (e) {
                  const msg = e instanceof Error ? e.message : 'Could not publish job';
                  setError(msg);
                  toast.error(msg);
                } finally {
                  setPublishing(false);
                }
              }}
            >
              {publishing ? 'Publishing…' : 'Publish'}
            </Button>
          )}
        </div>
      </header>

      {error ? <ErrorBanner message={error} /> : null}

      <Panel>
        {canEdit ? (
          <form
            key={`${job.id}-${job.jobStatus}-${job.updatedAt}`}
            className="form-grid"
            onSubmit={async (e: FormEvent<HTMLFormElement>) => {
              e.preventDefault();
              const fd = new FormData(e.currentTarget);
              setSaving(true);
              setError('');
              try {
                const updated = await jobsApi.update(job.id, {
                  title: String(fd.get('title')),
                  department: String(fd.get('department') || ''),
                  location: String(fd.get('location') || ''),
                  description: String(fd.get('description') || ''),
                  employmentType: String(fd.get('employmentType')) as EmploymentType,
                  jobStatus: String(fd.get('jobStatus')) as JobStatus,
                  tags: parseTags(String(fd.get('tags') || '')),
                });
                setJob(updated);
                toast.success('Job saved');
                cacheInvalidate('jobs');
              } catch (err) {
                const msg = err instanceof Error ? err.message : 'Could not save job';
                setError(msg);
                toast.error(msg);
              } finally {
                setSaving(false);
              }
            }}
          >
            <Field label="Title">
              <Input name="title" defaultValue={job.title} required />
            </Field>
            <div className="form-row">
              <Field label="Department">
                <Input name="department" defaultValue={job.department ?? ''} />
              </Field>
              <Field label="Location">
                <Input name="location" defaultValue={job.location ?? ''} />
              </Field>
            </div>
            <div className="form-row">
              <Field label="Employment type">
                <Select name="employmentType" defaultValue={job.employmentType}>
                  <option value="FULL_TIME">Full time</option>
                  <option value="PART_TIME">Part time</option>
                  <option value="CONTRACT">Contract</option>
                </Select>
              </Field>
              <Field label="Status">
                <Select name="jobStatus" defaultValue={job.jobStatus}>
                  <option value="DRAFT">Draft</option>
                  <option value="PUBLISHED">Published</option>
                  <option value="CLOSED">Closed</option>
                </Select>
              </Field>
            </div>
            <Field label="Description">
              <TextArea name="description" defaultValue={job.description ?? ''} />
            </Field>
            <Field label="Tags">
              <Input name="tags" defaultValue={job.tags.join(', ')} />
            </Field>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <Button type="submit" loading={saving}>
                {saving ? 'Saving…' : 'Save changes'}
              </Button>
              <Button
                type="button"
                variant="danger"
                disabled={deleting || saving}
                onClick={() => setConfirmDelete(true)}
              >
                Delete
              </Button>
            </div>
          </form>
        ) : (
          <div className="stack">
            <p>{job.description || 'No description.'}</p>
            <div>
              {job.tags.map((t) => (
                <span key={t} className="tag-chip">
                  {t}
                </span>
              ))}
            </div>
          </div>
        )}
      </Panel>

      {confirmDelete && (
        <ConfirmModal
          title="Delete job?"
          message={`Delete “${job.title}”?`}
          confirmLabel="Delete"
          danger
          loading={deleting}
          onClose={() => {
            if (!deleting) setConfirmDelete(false);
          }}
          onConfirm={async () => {
            setDeleting(true);
            try {
              await jobsApi.remove(job.id);
              toast.success('Job deleted');
              cacheInvalidate('jobs');
              navigate('/jobs');
            } catch (err) {
              const msg = err instanceof Error ? err.message : 'Could not delete job';
              setError(msg);
              toast.error(msg);
              setDeleting(false);
            }
          }}
        />
      )}
    </div>
  );
}
