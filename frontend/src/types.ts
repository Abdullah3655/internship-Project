export type Role = 'ADMIN' | 'HR' | 'INTERVIEWER';
export type AccountStatus = 'ACTIVE' | 'DISABLED';
export type TalentStatus = 'IN_POOL' | 'HIRED' | 'ARCHIVED';
export type JobStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';
export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT';
export type PipelineStage =
  | 'APPLIED'
  | 'SCREENING'
  | 'INTERVIEW'
  | 'OFFER'
  | 'HIRED'
  | 'DISQUALIFIED';
export type AssignmentRole = 'RECRUITER' | 'INTERVIEWER';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  accountStatus: AccountStatus;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface DocumentMeta {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface Candidate {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  source: string;
  talentStatus: TalentStatus;
  tags: string[];
  documents: DocumentMeta[];
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface Job {
  id: string;
  title: string;
  department: string | null;
  location: string | null;
  description: string | null;
  employmentType: EmploymentType;
  jobStatus: JobStatus;
  tags: string[];
  createdByUserId: string;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Application {
  id: string;
  jobId: string;
  jobTitle: string;
  candidateId: string;
  currentStage: PipelineStage;
  evaluationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface StageEvent {
  id: string;
  fromStage: PipelineStage | null;
  toStage: PipelineStage;
  changedByUserId: string;
  note: string | null;
  createdAt: string;
}

export interface Assignment {
  id: string;
  applicationId?: string;
  userId: string;
  assignmentRole: AssignmentRole;
  createdAt: string;
}

export interface Evaluation {
  id: string;
  interviewerUserId: string;
  score: number;
  feedback: string | null;
  createdAt: string;
}

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}
