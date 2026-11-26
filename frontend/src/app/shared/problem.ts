import { HttpErrorResponse } from '@angular/common/http';
import { FieldTree, ValidationError } from '@angular/forms/signals';

import { ProblemDetail } from '../applications/application.model';

export interface ServerErrors {
  /** Errors to attach to individual fields (returned from a Signal Forms submit action). */
  fieldErrors: ValidationError.WithOptionalFieldTree[];
  /** A message for the whole form when nothing maps onto a field. */
  formMessage: string | null;
}

/** Maps an RFC 9457 problem response onto form fields by name. */
export function toServerErrors(
  error: unknown,
  fields: Record<string, FieldTree<unknown> | undefined>,
  fallback: string,
): ServerErrors {
  if (!(error instanceof HttpErrorResponse)) {
    return { fieldErrors: [], formMessage: fallback };
  }
  if (error.status === 0) {
    return {
      fieldErrors: [],
      formMessage: 'Can’t reach the server. Check your connection and try again.',
    };
  }
  const problem = error.error as ProblemDetail | null;
  const fieldErrors: ValidationError.WithOptionalFieldTree[] = [];
  for (const [name, message] of Object.entries(problem?.errors ?? {})) {
    const fieldTree = fields[name];
    if (fieldTree) fieldErrors.push({ kind: 'server', message, fieldTree });
  }
  return {
    fieldErrors,
    formMessage: fieldErrors.length ? null : (problem?.detail ?? fallback),
  };
}
