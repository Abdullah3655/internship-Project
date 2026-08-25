import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { candidatesApi } from '../api/candidates';
import { useAuth } from '../auth/AuthContext';
import { TalentBadge } from '../components/Badges';
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
} from '../components/ui';
import { cacheInvalidate } from '../lib/cache';
import { formatDate, parseTags } from '../lib/helpers';
import type { Candidate, TalentStatus } from '../types';

export function CandidateDetailPage() {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [candidate, setCandidate] = useState<Candidate | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [openingId, setOpeningId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [pendingDocDelete, setPendingDocDelete] = useState<{
    id: string;
    name: string;
  } | null>(null);
  const [deletingDoc, setDeletingDoc] = useState(false);
  const canEdit = user?.role === 'HR' || user?.role === 'ADMIN';

  async function load() {
    setLoading(true);
    setError('');
    try {
      setCandidate(await candidatesApi.get(id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Candidate not found');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [id]);

  if (loading) return <LoadingBlock />;
  if (!candidate) return <ErrorBanner message={error || 'Candidate not found'} />;

  return (
    <div className="fade-up stack">
      <header className="page-head">
        <div>
          <p className="ui-hint">
            <Link to="/candidates">Candidates</Link>
          </p>
          <h1>
            {candidate.firstName} {candidate.lastName}
          </h1>
          <p>{candidate.email}</p>
        </div>
        <TalentBadge status={candidate.talentStatus} />
      </header>

      {error ? <ErrorBanner message={error} /> : null}

      <div className="split">
        <Panel className="stack">
          <h2 style={{ fontSize: '1.15rem' }}>Profile</h2>
          {canEdit ? (
            <EditForm
              candidate={candidate}
              onSaved={(c) => {
                setCandidate(c);
                setError('');
                toast.success('Candidate saved');
                cacheInvalidate('candidates');
              }}
              onError={(m) => {
                setError(m);
                toast.error(m);
              }}
            />
          ) : (
            <ReadOnly candidate={candidate} />
          )}
        </Panel>

        <div className="stack">
          <Panel className="stack">
            <h2 style={{ fontSize: '1.15rem' }}>Documents</h2>
            {candidate.documents.length === 0 ? (
              <p className="ui-hint">No CV uploaded yet.</p>
            ) : (
              candidate.documents.map((d) => (
                <div
                  key={d.id}
                  style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}
                >
                  <div>
                    <strong>{d.originalFilename}</strong>
                    <div className="ui-hint">
                      {(d.sizeBytes / 1024).toFixed(1)} KB · {formatDate(d.uploadedAt)}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Button
                      size="sm"
                      variant="ghost"
                      loading={openingId === d.id}
                      disabled={deletingDoc}
                      onClick={async () => {
                        setOpeningId(d.id);
                        try {
                          const { blob, filename } = await candidatesApi.downloadDocument(
                            candidate.id,
                            d.id,
                          );
                          const url = URL.createObjectURL(blob);
                          const canPreview =
                            blob.type.includes('pdf') ||
                            blob.type.includes('text') ||
                            d.originalFilename.toLowerCase().endsWith('.pdf') ||
                            d.originalFilename.toLowerCase().endsWith('.txt');
                          if (canPreview) {
                            window.open(url, '_blank', 'noopener,noreferrer');
                          } else {
                            const a = document.createElement('a');
                            a.href = url;
                            a.download = filename || d.originalFilename;
                            a.click();
                            toast.info('Download started');
                          }
                          setTimeout(() => URL.revokeObjectURL(url), 60_000);
                        } catch (err) {
                          const msg = err instanceof Error ? err.message : 'Could not open document';
                          setError(msg);
                          toast.error(msg);
                        } finally {
                          setOpeningId(null);
                        }
                      }}
                    >
                      Open
                    </Button>
                    {canEdit && (
                      <Button
                        size="sm"
                        variant="ghost"
                        disabled={deletingDoc || openingId !== null}
                        onClick={() =>
                          setPendingDocDelete({ id: d.id, name: d.originalFilename })
                        }
                      >
                        Delete
                      </Button>
                    )}
                  </div>
                </div>
              ))
            )}
            {canEdit && (
              <Field label="Upload CV" hint={uploading ? 'Uploading…' : 'PDF, DOC, DOCX, or TXT'}>
                <Input
                  type="file"
                  accept=".pdf,.doc,.docx,.txt"
                  disabled={uploading}
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    setUploading(true);
                    setError('');
                    try {
                      const res = await candidatesApi.uploadCv(candidate.id, file);
                      setCandidate(res.candidate);
                      toast.success(`Uploaded ${file.name}`);
                      cacheInvalidate('candidates');
                      e.target.value = '';
                    } catch (err) {
                      const msg = err instanceof Error ? err.message : 'Could not upload CV';
                      setError(msg);
                      toast.error(msg);
                    } finally {
                      setUploading(false);
                    }
                  }}
                />
              </Field>
            )}
          </Panel>

          {canEdit && (
            <Panel>
              <Button variant="danger" onClick={() => setConfirmDelete(true)}>
                Delete candidate
              </Button>
            </Panel>
          )}
        </div>
      </div>

      {pendingDocDelete && (
        <ConfirmModal
          title="Delete document?"
          message={`Delete “${pendingDocDelete.name}”?`}
          confirmLabel="Delete"
          danger
          loading={deletingDoc}
          onClose={() => {
            if (!deletingDoc) setPendingDocDelete(null);
          }}
          onConfirm={async () => {
            setDeletingDoc(true);
            setError('');
            try {
              await candidatesApi.deleteDocument(candidate.id, pendingDocDelete.id);
              setCandidate(await candidatesApi.get(candidate.id));
              setPendingDocDelete(null);
              toast.success('Document deleted');
              cacheInvalidate('candidates');
            } catch (err) {
              const msg = err instanceof Error ? err.message : 'Could not delete document';
              setError(msg);
              toast.error(msg);
            } finally {
              setDeletingDoc(false);
            }
          }}
        />
      )}

      {confirmDelete && (
        <ConfirmModal
          title="Delete candidate?"
          message={`Delete ${candidate.firstName} ${candidate.lastName}?`}
          confirmLabel="Delete"
          danger
          loading={deleting}
          onClose={() => {
            if (!deleting) setConfirmDelete(false);
          }}
          onConfirm={async () => {
            setDeleting(true);
            try {
              await candidatesApi.remove(candidate.id);
              toast.success('Candidate deleted');
              cacheInvalidate('candidates');
              navigate('/candidates');
            } catch (err) {
              const msg = err instanceof Error ? err.message : 'Could not delete candidate';
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

function ReadOnly({ candidate }: { candidate: Candidate }) {
  return (
    <div className="stack">
      <div>
        <div className="ui-label">Phone</div>
        <div>{candidate.phone || '—'}</div>
      </div>
      <div>
        <div className="ui-label">Tags</div>
        <div>
          {candidate.tags.map((t) => (
            <span key={t} className="tag-chip">
              {t}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

function EditForm({
  candidate,
  onSaved,
  onError,
}: {
  candidate: Candidate;
  onSaved: (c: Candidate) => void;
  onError: (m: string) => void;
}) {
  const [saving, setSaving] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSaving(true);
    try {
      const updated = await candidatesApi.update(candidate.id, {
        firstName: String(fd.get('firstName')),
        lastName: String(fd.get('lastName')),
        email: String(fd.get('email')),
        phone: String(fd.get('phone') || ''),
        talentStatus: String(fd.get('talentStatus')) as TalentStatus,
        tags: parseTags(String(fd.get('tags') || '')),
      });
      onSaved(updated);
    } catch (err) {
      onError(err instanceof Error ? err.message : 'Could not save candidate');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form-grid" onSubmit={onSubmit}>
      <div className="form-row">
        <Field label="First name">
          <Input name="firstName" defaultValue={candidate.firstName} required />
        </Field>
        <Field label="Last name">
          <Input name="lastName" defaultValue={candidate.lastName} required />
        </Field>
      </div>
      <Field label="Email">
        <Input name="email" type="email" defaultValue={candidate.email} required />
      </Field>
      <Field label="Phone">
        <Input name="phone" defaultValue={candidate.phone ?? ''} />
      </Field>
      <Field label="Talent status">
        <Select name="talentStatus" defaultValue={candidate.talentStatus}>
          <option value="IN_POOL">In pool</option>
          <option value="HIRED">Hired</option>
          <option value="ARCHIVED">Archived</option>
        </Select>
      </Field>
      <Field label="Tags" hint="Comma-separated, e.g. java, spring">
        <Input name="tags" defaultValue={candidate.tags.join(', ')} />
      </Field>
      <Button type="submit" loading={saving}>
        {saving ? 'Saving…' : 'Save changes'}
      </Button>
    </form>
  );
}
