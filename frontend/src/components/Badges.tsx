import type { AccountStatus, JobStatus, PipelineStage, Role, TalentStatus } from '../types';
import { roleLabel, stageLabel } from '../lib/helpers';
import { Badge } from './ui';

export function StageBadge({ stage }: { stage: PipelineStage }) {
  const tone =
    stage === 'HIRED'
      ? 'success'
      : stage === 'DISQUALIFIED'
        ? 'danger'
        : stage === 'OFFER'
          ? 'warn'
          : stage === 'INTERVIEW'
            ? 'info'
            : 'neutral';
  return <Badge tone={tone}>{stageLabel(stage)}</Badge>;
}

export function JobStatusBadge({ status }: { status: JobStatus }) {
  const tone =
    status === 'PUBLISHED' ? 'success' : status === 'CLOSED' ? 'danger' : 'warn';
  return <Badge tone={tone}>{status}</Badge>;
}

export function TalentBadge({ status }: { status: TalentStatus }) {
  const tone =
    status === 'HIRED' ? 'success' : status === 'ARCHIVED' ? 'neutral' : 'info';
  return <Badge tone={tone}>{status.replace('_', ' ')}</Badge>;
}

export function RoleBadge({ role }: { role: Role }) {
  const tone = role === 'ADMIN' ? 'warn' : role === 'HR' ? 'info' : 'neutral';
  return <Badge tone={tone}>{roleLabel(role)}</Badge>;
}

export function AccountStatusBadge({ status }: { status: AccountStatus }) {
  const tone = status === 'ACTIVE' ? 'success' : 'danger';
  return <Badge tone={tone}>{status === 'ACTIVE' ? 'Active' : 'Disabled'}</Badge>;
}
