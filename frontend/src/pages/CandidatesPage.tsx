import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { candidatesApi } from '../api/candidates';
import { TalentBadge } from '../components/Badges';
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
} from '../components/ui';
import { useCachedResource } from '../hooks/useCachedResource';
import { cacheKeys, cacheInvalidate } from '../lib/cache';
import { parseTags } from '../lib/helpers';
import type { Candidate } from '../types';

export function CandidatesPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [showBulk, setShowBulk] = useState(false);

  useEffect(() => {
    const handle = window.setTimeout(() => {
      setTags(parseTags(tagInput));
    }, 250);
    return () => window.clearTimeout(handle);
  }, [tagInput]);

  const { data, loading, refreshing, error, reload } = useCachedResource(
    cacheKeys.candidates(tags.length ? tags : undefined),
    () => candidatesApi.list(tags.length ? tags : undefined),
    [tags.join(',')],
  );

  const items = data?.items ?? [];
  const tagLabel = tags.join(', ');

  function clearFilter() {
    setTagInput('');
    setTags([]);
  }

  return (
    <div className="fade-up">
      <header className="page-head">
        <div>
          <h1>Candidates</h1>
          <p>Candidates and profiles.</p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <RefreshHint show={refreshing} />
          <Button variant="ghost" onClick={() => setShowBulk(true)}>
            Bulk CV upload
          </Button>
          <Button onClick={() => setShowCreate(true)}>Add candidate</Button>
        </div>
      </header>

      <div className="toolbar">
        <Input
          placeholder="Filter by tags (e.g. java, spring)"
          value={tagInput}
          onChange={(e) => setTagInput(e.target.value)}
          style={{ maxWidth: 320 }}
          aria-label="Filter candidates by tags"
        />
        {tagInput && (
          <Button variant="ghost" size="sm" onClick={clearFilter}>
            Clear
          </Button>
        )}
      </div>
      {tags.length > 1 && (
        <p className="ui-hint" style={{ marginTop: '-0.5rem', marginBottom: '0.75rem' }}>
          Showing candidates with all of: {tagLabel}
        </p>
      )}

      {error ? <ErrorBanner message={error} /> : null}
      {loading ? (
        <LoadingBlock />
      ) : items.length === 0 ? (
        <Panel>
          <EmptyState
            title={
              tags.length
                ? tags.length === 1
                  ? `No candidates tagged “${tagLabel}”`
                  : `No candidates with all tags “${tagLabel}”`
                : 'No candidates yet'
            }
            body={
              tags.length
                ? 'No candidates match these tags.'
                : 'Add a candidate or upload CVs to get started.'
            }
            action={
              tags.length ? (
                <Button variant="secondary" onClick={clearFilter}>
                  Clear filter
                </Button>
              ) : (
                <Button onClick={() => setShowCreate(true)}>Add candidate</Button>
              )
            }
          />
        </Panel>
      ) : (
        <Panel>
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Status</th>
                <th>Tags</th>
              </tr>
            </thead>
            <tbody>
              {items.map((c) => (
                <tr key={c.id} onClick={() => navigate(`/candidates/${c.id}`)}>
                  <td>
                    <strong>
                      {c.firstName} {c.lastName}
                    </strong>
                  </td>
                  <td>{c.email}</td>
                  <td>
                    <TalentBadge status={c.talentStatus} />
                  </td>
                  <td>
                    {c.tags.map((t) => (
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
        <CreateCandidateModal
          onClose={() => setShowCreate(false)}
          onCreated={(c) => {
            setShowCreate(false);
            cacheInvalidate('candidates');
            toast.success(`Added ${c.firstName} ${c.lastName}`);
            navigate(`/candidates/${c.id}`);
          }}
        />
      )}
      {showBulk && (
        <BulkCvModal
          onClose={() => setShowBulk(false)}
          onDone={(summary) => {
            setShowBulk(false);
            cacheInvalidate('candidates');
            toast.success(summary);
            void reload({ force: true });
          }}
        />
      )}
    </div>
  );
}

function CreateCandidateModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (c: Candidate) => void;
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
      const created = await candidatesApi.create({
        firstName: String(fd.get('firstName') ?? ''),
        lastName: String(fd.get('lastName') ?? ''),
        email: String(fd.get('email') ?? ''),
        phone: String(fd.get('phone') ?? '') || undefined,
        tags: parseTags(String(fd.get('tags') ?? '')),
      });
      onCreated(created);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not create candidate';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Add candidate" onClose={onClose}>
      {error ? <ErrorBanner message={error} /> : null}
      <form className="form-grid" onSubmit={onSubmit}>
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
        <Field label="Phone">
          <Input name="phone" />
        </Field>
        <Field label="Tags" hint="Comma-separated, e.g. java, spring">
          <Input name="tags" placeholder="java, spring" />
        </Field>
        <Button type="submit" loading={saving}>
          {saving ? 'Saving…' : 'Create candidate'}
        </Button>
      </form>
    </Modal>
  );
}

function BulkCvModal({
  onClose,
  onDone,
}: {
  onClose: () => void;
  onDone: (summary: string) => void;
}) {
  const toast = useToast();
  const [files, setFiles] = useState<File[]>([]);
  const [error, setError] = useState('');
  const [result, setResult] = useState('');
  const [saving, setSaving] = useState(false);

  function addFiles(list: FileList | null) {
    if (!list?.length) return;
    setFiles((prev) => {
      const next = [...prev];
      for (const file of Array.from(list)) {
        const dup = next.some((f) => f.name === file.name && f.size === file.size);
        if (!dup) next.push(file);
      }
      return next;
    });
    setError('');
    setResult('');
  }

  function removeFile(index: number) {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  }

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!files.length) {
      const msg = 'Choose one or more CV files';
      setError(msg);
      toast.error(msg);
      return;
    }
    setSaving(true);
    setError('');
    try {
      const res = await candidatesApi.uploadCvBulk(files);
      const ok = res.items.filter((i) => i.success).length;
      const fail = res.items.length - ok;
      const summary = `${ok} candidate${ok === 1 ? '' : 's'} created${fail ? `, ${fail} failed` : ''}`;
      setResult(summary);
      if (ok > 0) setTimeout(() => onDone(summary), 500);
      else toast.error(summary);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not upload CVs';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Bulk CV upload" onClose={onClose}>
      {error ? <ErrorBanner message={error} /> : null}
      {result ? <p className="ui-hint">{result}</p> : null}
      <form className="form-grid" onSubmit={onSubmit}>
        <Field label="CV files" hint="PDF, DOC, DOCX, or TXT — one candidate per file.">
          <Input
            type="file"
            multiple
            accept=".pdf,.doc,.docx,.txt"
            onChange={(e) => {
              addFiles(e.target.files);
              e.target.value = '';
            }}
          />
        </Field>

        {files.length > 0 && (
          <div className="stack" style={{ gap: 6 }}>
            <strong style={{ fontSize: '0.9rem' }}>
              {files.length} file{files.length === 1 ? '' : 's'} ready
            </strong>
            {files.map((file, index) => (
              <div
                key={`${file.name}-${file.size}-${index}`}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 8,
                }}
              >
                <span className="ui-hint" style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {file.name}
                </span>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => removeFile(index)}
                  disabled={saving}
                >
                  Remove
                </Button>
              </div>
            ))}
          </div>
        )}

        <Button type="submit" loading={saving} disabled={!files.length}>
          {saving
            ? 'Uploading…'
            : files.length
              ? `Upload ${files.length} CV${files.length === 1 ? '' : 's'}`
              : 'Upload'}
        </Button>
      </form>
    </Modal>
  );
}
