import type { PipelineStage } from '../types';

const FORWARD: PipelineStage[] = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER'];
const INTERVIEW_LOOP: PipelineStage[] = ['SCREENING', 'INTERVIEW'];

export function allowedStagesFrom(from: PipelineStage): PipelineStage[] {
  if (from === 'HIRED') return [];
  if (from === 'DISQUALIFIED') return ['APPLIED'];

  const next: PipelineStage[] = ['DISQUALIFIED'];
  const fromIndex = FORWARD.indexOf(from);
  if (fromIndex >= 0) {
    for (let i = fromIndex + 1; i < FORWARD.length; i++) {
      next.push(FORWARD[i]);
    }
  }
  if (from === 'OFFER') next.push('HIRED');
  return next;
}

export function isTerminalStage(stage: PipelineStage): boolean {
  return stage === 'HIRED' || stage === 'DISQUALIFIED';
}

export function allowsInterviewerAssignment(stage: PipelineStage): boolean {
  return INTERVIEW_LOOP.includes(stage);
}

export function allowsEvaluation(stage: PipelineStage): boolean {
  return INTERVIEW_LOOP.includes(stage);
}

export type InterviewerWorkStatus = 'closed' | 'needs_feedback' | 'submitted' | 'in_progress';

export function interviewerWorkStatus(
  stage: PipelineStage,
  hasMyEvaluation: boolean,
): InterviewerWorkStatus {
  if (isTerminalStage(stage)) return 'closed';
  if (allowsEvaluation(stage)) {
    return hasMyEvaluation ? 'submitted' : 'needs_feedback';
  }
  return 'in_progress';
}

export function interviewerWorkStatusLabel(status: InterviewerWorkStatus): string {
  switch (status) {
    case 'closed':
      return 'Closed';
    case 'needs_feedback':
      return 'Needs feedback';
    case 'submitted':
      return 'Feedback submitted';
    case 'in_progress':
      return 'In progress';
  }
}

export function awaitingHrReview(stage: PipelineStage, evaluationCount: number): boolean {
  return allowsEvaluation(stage) && evaluationCount > 0;
}

export function stageLabel(stage: PipelineStage): string {
  return stage.charAt(0) + stage.slice(1).toLowerCase().replace('_', ' ');
}

export function roleLabel(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase().replace(/_/g, ' ');
}

export function parseTags(raw: string): string[] {
  return raw
    .split(/[,;\s]+/)
    .map((t) => t.trim().toLowerCase())
    .filter(Boolean);
}

export function fullName(user: { firstName: string; lastName: string }): string {
  return `${user.firstName} ${user.lastName}`.trim();
}

export function initials(user: { firstName: string; lastName: string }): string {
  return `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();
}

export function formatDate(value: string): string {
  return new Date(value).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}
