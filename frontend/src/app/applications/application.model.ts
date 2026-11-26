export const STATUSES = [
  'WISHLIST',
  'APPLIED',
  'INTERVIEWING',
  'OFFER',
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
  'GHOSTED',
] as const;

export type ApplicationStatus = (typeof STATUSES)[number];

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  WISHLIST: 'Wishlist',
  APPLIED: 'Applied',
  INTERVIEWING: 'Interviewing',
  OFFER: 'Offer',
  ACCEPTED: 'Accepted',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
  GHOSTED: 'Ghosted',
};

/** Statuses where the application is still moving and needs attention. */
export const ACTIVE_STATUSES: readonly ApplicationStatus[] = ['APPLIED', 'INTERVIEWING', 'OFFER'];

export const WORK_MODES = ['ONSITE', 'HYBRID', 'REMOTE'] as const;
export type WorkMode = (typeof WORK_MODES)[number];

export const WORK_MODE_LABELS: Record<WorkMode, string> = {
  ONSITE: 'On-site',
  HYBRID: 'Hybrid',
  REMOTE: 'Remote',
};

export interface JobApplication {
  id: string;
  title: string;
  company: string;
  position: string | null;
  status: ApplicationStatus;
  appliedDate: string | null;
  location: string | null;
  workMode: WorkMode | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  jobUrl: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export type JobApplicationRequest = Omit<JobApplication, 'id' | 'createdAt' | 'updatedAt'>;

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface StatusStats {
  total: number;
  byStatus: Record<ApplicationStatus, number>;
}

/** RFC 9457 problem details as returned by the API. */
export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}

export type SortOption = 'createdAt,desc' | 'appliedDate,desc' | 'updatedAt,desc' | 'company,asc';

export interface ListQuery {
  status: ApplicationStatus | null;
  q: string;
  sort: SortOption;
  page: number;
  size: number;
}
