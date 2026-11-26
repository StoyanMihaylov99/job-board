import { maxLength, min, pattern, required, schema, validate } from '@angular/forms/signals';

import {
  ApplicationStatus,
  JobApplication,
  JobApplicationRequest,
  WorkMode,
} from '../application.model';

/** Form model: every field is a plain value so it can bind straight to a native control. */
export interface ApplicationFormModel {
  title: string;
  company: string;
  position: string;
  status: ApplicationStatus;
  appliedDate: string;
  location: string;
  workMode: WorkMode | '';
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  jobUrl: string;
  notes: string;
}

export function emptyApplicationForm(): ApplicationFormModel {
  return {
    title: '',
    company: '',
    position: '',
    status: 'APPLIED',
    appliedDate: todayIso(),
    location: '',
    workMode: '',
    salaryMin: null,
    salaryMax: null,
    salaryCurrency: '',
    jobUrl: '',
    notes: '',
  };
}

export function toApplicationForm(app: JobApplication): ApplicationFormModel {
  return {
    title: app.title,
    company: app.company,
    position: app.position ?? '',
    status: app.status,
    appliedDate: app.appliedDate ?? '',
    location: app.location ?? '',
    workMode: app.workMode ?? '',
    salaryMin: app.salaryMin,
    salaryMax: app.salaryMax,
    salaryCurrency: app.salaryCurrency ?? '',
    jobUrl: app.jobUrl ?? '',
    notes: app.notes ?? '',
  };
}

export function toApplicationRequest(model: ApplicationFormModel): JobApplicationRequest {
  const text = (value: string) => value.trim() || null;
  return {
    title: model.title.trim(),
    company: model.company.trim(),
    position: text(model.position),
    status: model.status,
    appliedDate: model.appliedDate || null,
    location: text(model.location),
    workMode: model.workMode || null,
    salaryMin: model.salaryMin,
    salaryMax: model.salaryMax,
    salaryCurrency: text(model.salaryCurrency)?.toUpperCase() ?? null,
    jobUrl: text(model.jobUrl),
    notes: text(model.notes),
  };
}

export function todayIso(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

/** Client-side rules mirror the API's Bean Validation so most mistakes are caught before a round trip. */
export const applicationSchema = schema<ApplicationFormModel>((p) => {
  required(p.title, { message: 'Enter the job title.' });
  maxLength(p.title, 200, { message: 'Keep the title under 200 characters.' });

  required(p.company, { message: 'Enter the company name.' });
  maxLength(p.company, 200, { message: 'Keep the company name under 200 characters.' });

  maxLength(p.position, 100, { message: 'Keep the level under 100 characters.' });
  maxLength(p.location, 200, { message: 'Keep the location under 200 characters.' });
  maxLength(p.notes, 10_000, { message: 'Notes can be up to 10,000 characters.' });

  validate(p.appliedDate, ({ value }) =>
    value() && value() > todayIso()
      ? { kind: 'future', message: 'The applied date can’t be in the future.' }
      : null,
  );

  min(p.salaryMin, 0, { message: 'Salary can’t be negative.' });
  min(p.salaryMax, 0, { message: 'Salary can’t be negative.' });
  validate(p.salaryMax, ({ value, valueOf }) => {
    const minSalary = valueOf(p.salaryMin);
    const maxSalary = value();
    return minSalary != null && maxSalary != null && maxSalary < minSalary
      ? { kind: 'range', message: 'The maximum must be at least the minimum.' }
      : null;
  });

  maxLength(p.salaryCurrency, 3, { message: 'Use a 3-letter code such as EUR or USD.' });
  pattern(p.salaryCurrency, /^[A-Za-z]{3}$/, {
    message: 'Use a 3-letter code such as EUR or USD.',
  });
  pattern(p.jobUrl, /^https?:\/\/\S+$/i, { message: 'Paste a full link starting with https://' });
});
