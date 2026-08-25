import { apiRequest, downloadBlob } from './client';
import { cacheInvalidate } from '../lib/cache';
import type { Candidate, TalentStatus } from '../types';

function bustCandidateCaches() {
  cacheInvalidate('candidates');
}

export const candidatesApi = {
  list(tags?: string[]) {
    const q = new URLSearchParams();
    (tags ?? []).forEach((t) => q.append('tags', t));
    const qs = q.toString();
    return apiRequest<{ items: Candidate[] }>(`/api/candidates${qs ? `?${qs}` : ''}`, {
      base: 'candidate',
    });
  },
  get(id: string) {
    return apiRequest<Candidate>(`/api/candidates/${id}`, { base: 'candidate' });
  },
  async create(body: {
    firstName: string;
    lastName: string;
    email: string;
    phone?: string;
    tags?: string[];
  }) {
    const created = await apiRequest<Candidate>('/api/candidates', {
      base: 'candidate',
      method: 'POST',
      body,
    });
    bustCandidateCaches();
    return created;
  },
  async update(
    id: string,
    body: Partial<{
      firstName: string;
      lastName: string;
      email: string;
      phone: string;
      talentStatus: TalentStatus;
      tags: string[];
    }>,
  ) {
    const updated = await apiRequest<Candidate>(`/api/candidates/${id}`, {
      base: 'candidate',
      method: 'PATCH',
      body,
    });
    bustCandidateCaches();
    return updated;
  },
  async remove(id: string) {
    await apiRequest<void>(`/api/candidates/${id}`, {
      base: 'candidate',
      method: 'DELETE',
    });
    bustCandidateCaches();
  },
  async uploadCv(id: string, file: File) {
    const form = new FormData();
    form.append('file', file);
    const res = await apiRequest<{ candidate: Candidate }>(`/api/candidates/${id}/cv`, {
      base: 'candidate',
      method: 'POST',
      formData: form,
    });
    bustCandidateCaches();
    return res;
  },
  async uploadCvBulk(files: FileList | File[]) {
    const form = new FormData();
    Array.from(files).forEach((f) => form.append('files', f));
    const res = await apiRequest<{
      items: Array<{
        filename: string;
        success: boolean;
        candidate?: Candidate;
        error?: string;
      }>;
    }>('/api/candidates/cv/bulk', {
      base: 'candidate',
      method: 'POST',
      formData: form,
    });
    bustCandidateCaches();
    return res;
  },
  async downloadDocument(candidateId: string, documentId: string) {
    return downloadBlob(
      `/api/candidates/${candidateId}/documents/${documentId}`,
      'candidate',
    );
  },
  async deleteDocument(candidateId: string, documentId: string) {
    await apiRequest<void>(`/api/candidates/${candidateId}/documents/${documentId}`, {
      base: 'candidate',
      method: 'DELETE',
    });
    bustCandidateCaches();
  },
};
