import { WORK_MODE_LABELS, JobApplication } from '../applications/application.model';

const numberFormat = new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 });

function money(value: number, currency: string | null): string {
  if (!currency) return numberFormat.format(value);
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }).format(value);
  } catch {
    return `${numberFormat.format(value)} ${currency}`;
  }
}

export function formatSalary(
  app: Pick<JobApplication, 'salaryMin' | 'salaryMax' | 'salaryCurrency'>,
): string {
  const { salaryMin: min, salaryMax: max, salaryCurrency: currency } = app;
  if (min != null && max != null) {
    return min === max ? money(min, currency) : `${money(min, currency)} – ${money(max, currency)}`;
  }
  if (min != null) return `from ${money(min, currency)}`;
  if (max != null) return `up to ${money(max, currency)}`;
  return '';
}

export function formatPlace(app: Pick<JobApplication, 'location' | 'workMode'>): string {
  const mode = app.workMode ? WORK_MODE_LABELS[app.workMode] : '';
  if (app.location && mode) return `${app.location}, ${mode.toLowerCase()}`;
  return app.location ?? mode;
}
