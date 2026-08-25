import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { authApi } from '../api/auth';
import { candidatesApi } from '../api/candidates';
import { useAuth } from '../auth/AuthContext';
import { StageBadge } from '../components/Badges';
import { PersonPicker } from '../components/PersonPicker';
import { useToast } from '../components/Toast';
import {
  Avatar,
  Button,
  ConfirmModal,
  ErrorBanner,
  Field,
  InfoBanner,
  LoadingBlock,
  Modal,
  Panel,
  Select,
  TextArea,
} from '../components/ui';
import { cacheInvalidate } from '../lib/cache';
import {
  allowedStagesFrom,
  allowsEvaluation,
  allowsInterviewerAssignment,
  awaitingHrReview,
  formatDate,
  fullName,
  initials,
  isTerminalStage,
  roleLabel,
  stageLabel,
} from '../lib/helpers';
import type {
  Application,
  Assignment,
  Candidate,
  Evaluation,
  PipelineStage,
  StageEvent,
  User,
} from '../types';

export function ApplicationDetailPage() {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const [app, setApp] = useState<Application | null>(null);
  const [candidate, setCandidate] = useState<Candidate | null>(null);
  const [history, setHistory] = useState<StageEvent[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [evaluations, setEvaluations] = useState<Evaluation[]>([]);
  const [people, setPeople] = useState<Record<string, User>>({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState('');
  const [showAssign, setShowAssign] = useState(false);
  const [selectedInterviewer, setSelectedInterviewer] = useState<User | null>(null);
  const [evalScore, setEvalScore] = useState(4);
  const [evalFeedback, setEvalFeedback] = useState('');
  const [movingStage, setMovingStage] = useState<PipelineStage | null>(null);
  const [pendingStage, setPendingStage] = useState<PipelineStage | null>(null);
  const [savingEval, setSavingEval] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [pendingRemove, setPendingRemove] = useState<{
    id: string;
    name: string;
  } | null>(null);

  const canManage = user?.role === 'HR' || user?.role === 'ADMIN';
  const isInterviewer = user?.role === 'INTERVIEWER';
  const nextStages = useMemo(
    () => (app ? allowedStagesFrom(app.currentStage) : []),
    [app],
  );

  async function load(opts?: { silent?: boolean }) {
    if (!opts?.silent) setLoading(true);
    setError('');
    try {
      const [application, stageEvents, assigns, evals] = await Promise.all([
        applicationsApi.get(id),
        applicationsApi.stageHistory(id),
        applicationsApi.listAssignments(id),
        applicationsApi.listEvaluations(id),
      ]);
      setApp(application);
      setHistory(stageEvents.items);
      setAssignments(assigns.items);
      setEvaluations(evals.items);

      try {
        setCandidate(await candidatesApi.get(application.candidateId));
      } catch {
        setCandidate(null);
      }

      if (canManage) {
        const users = await authApi.listUsers();
        const map: Record<string, User> = {};
        users.items.forEach((u) => {
          map[u.id] = u;
        });
        setPeople(map);
      } else if (user) {
        setPeople({ [user.id]: user });
      } else {
        setPeople({});
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load application');
    } finally {
      if (!opts?.silent) setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [id]);

  async function moveStage(toStage: PipelineStage) {
    if (!app) return;
    setMovingStage(toStage);
    setError('');
    try {
      await applicationsApi.changeStage(app.id, {
        toStage,
        note: note.trim() || undefined,
      });
      setNote('');
      setPendingStage(null);
      toast.success(`Moved to ${stageLabel(toStage)}`);
      cacheInvalidate('applications');
      await load({ silent: true });
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Could not update stage';
      setError(msg);
      toast.error(msg);
    } finally {
      setMovingStage(null);
    }
  }

  function stageConfirmCopy(toStage: PipelineStage): string {
    if (toStage === 'DISQUALIFIED') {
      return `${stageLabel(app!.currentStage)} → Disqualified`;
    }
    if (toStage === 'HIRED') {
      return `${stageLabel(app!.currentStage)} → Hired`;
    }
    if (toStage === 'OFFER') {
      return `${stageLabel(app!.currentStage)} → Offer`;
    }
    return `${stageLabel(app!.currentStage)} → ${stageLabel(toStage)}`;
  }

  if (loading) return <LoadingBlock />;
  if (!app) return <ErrorBanner message={error || 'Application not found'} />;

  const canAssignInterviewer = allowsInterviewerAssignment(app.currentStage);
  const canSubmitEvaluation =
    allowsEvaluation(app.currentStage) &&
    evaluations.every((ev) => ev.interviewerUserId !== user?.id);

  return (
    <div className="fade-up stack">
      <header className="page-head">
        <div>
          <p className="ui-hint">
            <Link to={isInterviewer ? '/my-work' : '/applications'}>
              {isInterviewer ? 'My assignments' : 'Applications'}
            </Link>
          </p>
          <h1>{app.jobTitle}</h1>
          <p>
            {candidate
              ? `${candidate.firstName} ${candidate.lastName} · ${candidate.email}`
              : `Candidate ${app.candidateId}`}
          </p>
        </div>
        <StageBadge stage={app.currentStage} />
      </header>

      {error ? <ErrorBanner message={error} /> : null}

      {canManage && app && awaitingHrReview(app.currentStage, app.evaluationCount ?? evaluations.length) && (
        <InfoBanner message="Interviewer feedback is ready for review." />
      )}

      <div className="split">
        <div className="stack">
          {canManage && (
            <Panel className="stack">
              <h2 style={{ fontSize: '1.15rem' }}>Advance stage</h2>
              <Field label="Optional note">
                <TextArea value={note} onChange={(e) => setNote(e.target.value)} />
              </Field>
              {nextStages.length === 0 ? (
                <p className="ui-hint">No further stage changes are available.</p>
              ) : (
                <div className="stage-actions">
                  {nextStages.map((stage) => (
                    <Button
                      key={stage}
                      size="sm"
                      variant={
                        stage === 'DISQUALIFIED'
                          ? 'danger'
                          : stage === 'HIRED'
                            ? 'primary'
                            : 'secondary'
                      }
                      disabled={movingStage !== null}
                      onClick={() => setPendingStage(stage)}
                    >
                      {stageLabel(stage)}
                    </Button>
                  ))}
                </div>
              )}
            </Panel>
          )}

          <Panel className="stack">
            <h2 style={{ fontSize: '1.15rem' }}>Stage history</h2>
            <div className="timeline">
              {history.map((event) => (
                <div key={event.id} className="timeline-item">
                  <span className="timeline-dot" />
                  <div>
                    <strong>
                      {event.fromStage ? stageLabel(event.fromStage) : 'Created'} →{' '}
                      {stageLabel(event.toStage)}
                    </strong>
                    <p>
                      {formatDate(event.createdAt)}
                      {event.note ? ` · ${event.note}` : ''}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </Panel>

          {isInterviewer && (
            <Panel className="stack">
              <h2 style={{ fontSize: '1.15rem' }}>Submit evaluation</h2>
              {!allowsEvaluation(app.currentStage) ? (
                <p className="ui-hint">
                  Feedback is available during Screening and Interview.
                </p>
              ) : !canSubmitEvaluation ? (
                <p className="ui-hint">You already submitted feedback for this application.</p>
              ) : (
                <form
                  className="form-grid"
                  onSubmit={async (e) => {
                    e.preventDefault();
                    setSavingEval(true);
                    setError('');
                    try {
                      await applicationsApi.evaluate(app.id, {
                        score: evalScore,
                        feedback: evalFeedback.trim() || undefined,
                      });
                      setEvalFeedback('');
                      toast.success('Evaluation saved');
                      await load({ silent: true });
                    } catch (err) {
                      const msg = err instanceof Error ? err.message : 'Could not save evaluation';
                      setError(msg);
                      toast.error(msg);
                    } finally {
                      setSavingEval(false);
                    }
                  }}
                >
                  <Field label="Score (1–5)">
                    <Select
                      value={evalScore}
                      onChange={(e) => setEvalScore(Number(e.target.value))}
                    >
                      {[1, 2, 3, 4, 5].map((n) => (
                        <option key={n} value={n}>
                          {n}
                        </option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Feedback">
                    <TextArea
                      value={evalFeedback}
                      onChange={(e) => setEvalFeedback(e.target.value)}
                    />
                  </Field>
                  <Button type="submit" loading={savingEval}>
                    {savingEval ? 'Saving…' : 'Save evaluation'}
                  </Button>
                </form>
              )}
            </Panel>
          )}
        </div>

        <div className="stack">
          <Panel className="stack">
            <div className="page-head" style={{ marginBottom: 0 }}>
              <h2 style={{ fontSize: '1.15rem' }}>Assignments</h2>
              {canManage && canAssignInterviewer && (
                <Button size="sm" onClick={() => setShowAssign(true)}>
                  Assign interviewer
                </Button>
              )}
            </div>
            {canManage && !canAssignInterviewer && !isTerminalStage(app.currentStage) && (
              <p className="ui-hint">
                Assign interviewers during Screening or Interview.
              </p>
            )}
            {canManage && isTerminalStage(app.currentStage) && assignments.length === 0 && (
              <p className="ui-hint">Assignments are closed for this stage.</p>
            )}
            {assignments.length === 0 ? (
              <p className="ui-hint">No one assigned yet.</p>
            ) : (
              assignments.map((a) => {
                const person = people[a.userId];
                const isSelf = user?.id === a.userId;
                const displayName = person ? fullName(person) : isSelf && user ? fullName(user) : 'Assigned user';
                const displayEmail = person?.email ?? (isSelf ? user?.email : undefined);
                return (
                  <div
                    key={a.id}
                    style={{ display: 'flex', gap: 12, alignItems: 'center' }}
                  >
                    <Avatar
                      label={
                        person
                          ? initials(person)
                          : isSelf && user
                            ? initials(user)
                            : a.userId.slice(0, 2).toUpperCase()
                      }
                    />
                    <div style={{ flex: 1 }}>
                      <strong>{displayName}</strong>
                      <div className="ui-hint">
                        {roleLabel(a.assignmentRole)}
                        {displayEmail ? ` · ${displayEmail}` : ''}
                      </div>
                    </div>
                    {canManage && (
                      <Button
                        size="sm"
                        variant="ghost"
                        disabled={removingId !== null}
                        onClick={() =>
                          setPendingRemove({ id: a.id, name: displayName })
                        }
                      >
                        Remove
                      </Button>
                    )}
                  </div>
                );
              })
            )}
          </Panel>

          <Panel className="stack">
            <h2 style={{ fontSize: '1.15rem' }}>Evaluations</h2>
            {evaluations.length === 0 ? (
              <p className="ui-hint">No evaluations yet.</p>
            ) : (
              evaluations.map((ev) => (
                <div key={ev.id}>
                  <strong>Score {ev.score}/5</strong>
                  <p className="ui-hint">
                    {formatDate(ev.createdAt)}
                    {ev.feedback ? ` · ${ev.feedback}` : ''}
                  </p>
                </div>
              ))
            )}
          </Panel>
        </div>
      </div>

      {pendingRemove && (
        <ConfirmModal
          title="Remove assignment?"
          message={`Remove ${pendingRemove.name} from this application?`}
          confirmLabel="Remove"
          danger
          loading={removingId === pendingRemove.id}
          onClose={() => {
            if (!removingId) setPendingRemove(null);
          }}
          onConfirm={async () => {
            setRemovingId(pendingRemove.id);
            setError('');
            try {
              await applicationsApi.removeAssignment(app.id, pendingRemove.id);
              setPendingRemove(null);
              toast.success('Assignment removed');
              await load({ silent: true });
            } catch (err) {
              const msg =
                err instanceof Error ? err.message : 'Could not remove assignment';
              setError(msg);
              toast.error(msg);
            } finally {
              setRemovingId(null);
            }
          }}
        />
      )}

      {pendingStage && (
        <Modal
          title={`Move to ${stageLabel(pendingStage)}?`}
          onClose={() => {
            if (!movingStage) setPendingStage(null);
          }}
        >
          <p style={{ marginBottom: '1rem' }}>{stageConfirmCopy(pendingStage)}</p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <Button
              variant="ghost"
              disabled={movingStage !== null}
              onClick={() => setPendingStage(null)}
            >
              Cancel
            </Button>
            <Button
              variant={pendingStage === 'DISQUALIFIED' ? 'danger' : 'primary'}
              loading={movingStage === pendingStage}
              disabled={movingStage !== null && movingStage !== pendingStage}
              onClick={() => void moveStage(pendingStage)}
            >
              Confirm
            </Button>
          </div>
        </Modal>
      )}

      {showAssign && (
        <Modal
          title="Assign interviewer"
          onClose={() => {
            if (!assigning) setShowAssign(false);
          }}
        >
          <PersonPicker
            role="INTERVIEWER"
            value={selectedInterviewer?.id ?? null}
            onChange={setSelectedInterviewer}
            excludeIds={assignments.map((a) => a.userId)}
          />
          <Button
            loading={assigning}
            disabled={!selectedInterviewer}
            onClick={async () => {
              if (!selectedInterviewer) return;
              setAssigning(true);
              setError('');
              try {
                await applicationsApi.assign(app.id, {
                  userId: selectedInterviewer.id,
                  assignmentRole: 'INTERVIEWER',
                });
                setShowAssign(false);
                setSelectedInterviewer(null);
                toast.success(`Assigned ${fullName(selectedInterviewer)}`);
                await load({ silent: true });
              } catch (e) {
                const msg = e instanceof Error ? e.message : 'Could not assign interviewer';
                setError(msg);
                toast.error(msg);
              } finally {
                setAssigning(false);
              }
            }}
          >
            {assigning ? 'Assigning…' : 'Confirm assignment'}
          </Button>
        </Modal>
      )}
    </div>
  );
}
