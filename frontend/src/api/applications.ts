import { apiRequest } from './client';
import { cacheInvalidate } from '../lib/cache';
import type {
  Application,
  Assignment,
  AssignmentRole,
  Evaluation,
  PipelineStage,
  StageEvent,
} from '../types';

function bustApplicationCaches() {
  cacheInvalidate('applications');
  cacheInvalidate('candidates');
}

export const applicationsApi = {
  list(params?: { jobId?: string; candidateId?: string; stage?: PipelineStage }) {
    const q = new URLSearchParams();
    if (params?.jobId) q.set('jobId', params.jobId);
    if (params?.candidateId) q.set('candidateId', params.candidateId);
    if (params?.stage) q.set('stage', params.stage);
    const qs = q.toString();
    return apiRequest<{ items: Application[] }>(
      `/api/applications${qs ? `?${qs}` : ''}`,
      { base: 'application' },
    );
  },
  get(id: string) {
    return apiRequest<Application>(`/api/applications/${id}`, { base: 'application' });
  },
  async create(body: { jobId: string; candidateId: string }) {
    const created = await apiRequest<Application>('/api/applications', {
      base: 'application',
      method: 'POST',
      body,
    });
    bustApplicationCaches();
    return created;
  },
  async remove(id: string) {
    await apiRequest<void>(`/api/applications/${id}`, {
      base: 'application',
      method: 'DELETE',
    });
    bustApplicationCaches();
  },
  async changeStage(id: string, body: { toStage: PipelineStage; note?: string }) {
    const event = await apiRequest<StageEvent>(`/api/applications/${id}/stage-changes`, {
      base: 'application',
      method: 'POST',
      body,
    });
    bustApplicationCaches();
    return event;
  },
  stageHistory(id: string) {
    return apiRequest<{ items: StageEvent[] }>(`/api/applications/${id}/stage-changes`, {
      base: 'application',
    });
  },
  listAssignments(id: string) {
    return apiRequest<{ items: Assignment[] }>(`/api/applications/${id}/assignments`, {
      base: 'application',
    });
  },
  async assign(id: string, body: { userId: string; assignmentRole: AssignmentRole }) {
    const created = await apiRequest<Assignment>(`/api/applications/${id}/assignments`, {
      base: 'application',
      method: 'POST',
      body,
    });
    bustApplicationCaches();
    return created;
  },
  async removeAssignment(applicationId: string, assignmentId: string) {
    await apiRequest<void>(`/api/applications/${applicationId}/assignments/${assignmentId}`, {
      base: 'application',
      method: 'DELETE',
    });
    bustApplicationCaches();
  },
  listEvaluations(id: string) {
    return apiRequest<{ items: Evaluation[] }>(`/api/applications/${id}/evaluations`, {
      base: 'application',
    });
  },
  async evaluate(id: string, body: { score: number; feedback?: string }) {
    const created = await apiRequest<Evaluation>(`/api/applications/${id}/evaluations`, {
      base: 'application',
      method: 'POST',
      body,
    });
    bustApplicationCaches();
    return created;
  },
  myAssignments() {
    return apiRequest<{ items: Assignment[] }>('/api/assignments', {
      base: 'application',
    });
  },
};
