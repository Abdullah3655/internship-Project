import { apiRequest } from './client';
import { cacheInvalidate } from '../lib/cache';
import type { EmploymentType, Job, JobStatus } from '../types';

function bustJobCaches() {
  cacheInvalidate('jobs');
}

export const jobsApi = {
  list(params?: { status?: JobStatus; tag?: string }) {
    const q = new URLSearchParams();
    if (params?.status) q.set('status', params.status);
    if (params?.tag) q.set('tag', params.tag);
    const qs = q.toString();
    return apiRequest<{ items: Job[] }>(`/api/jobs${qs ? `?${qs}` : ''}`, {
      base: 'application',
    });
  },
  get(id: string) {
    return apiRequest<Job>(`/api/jobs/${id}`, { base: 'application' });
  },
  async create(body: {
    title: string;
    department?: string;
    location?: string;
    description?: string;
    employmentType: EmploymentType;
    tags?: string[];
  }) {
    const created = await apiRequest<Job>('/api/jobs', {
      base: 'application',
      method: 'POST',
      body,
    });
    bustJobCaches();
    return created;
  },
  async update(
    id: string,
    body: Partial<{
      title: string;
      department: string;
      location: string;
      description: string;
      employmentType: EmploymentType;
      jobStatus: JobStatus;
      tags: string[];
    }>,
  ) {
    const updated = await apiRequest<Job>(`/api/jobs/${id}`, {
      base: 'application',
      method: 'PATCH',
      body,
    });
    bustJobCaches();
    return updated;
  },
  async publish(id: string) {
    const updated = await apiRequest<Job>(`/api/jobs/${id}/publish`, {
      base: 'application',
      method: 'POST',
    });
    bustJobCaches();
    return updated;
  },
  async remove(id: string) {
    await apiRequest<void>(`/api/jobs/${id}`, {
      base: 'application',
      method: 'DELETE',
    });
    bustJobCaches();
  },
};
