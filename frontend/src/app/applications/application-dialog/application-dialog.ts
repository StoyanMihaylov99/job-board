import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FieldTree, form, FormField, submit, ValidationError } from '@angular/forms/signals';

import { Announcer } from '../../shared/announcer/announcer';
import {
  JobApplication,
  ProblemDetail,
  STATUS_LABELS,
  STATUSES,
  WORK_MODE_LABELS,
  WORK_MODES,
} from '../application.model';
import { ApplicationsApi } from '../applications.api';
import {
  ApplicationFormModel,
  applicationSchema,
  emptyApplicationForm,
  toApplicationForm,
  toApplicationRequest,
} from './application-form';

/** Create / edit form in a native modal dialog. */
@Component({
  selector: 'app-application-dialog',
  imports: [FormField],
  templateUrl: './application-dialog.html',
  styleUrl: './application-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationDialog {
  readonly saved = output<JobApplication>();

  private readonly api = inject(ApplicationsApi);
  private readonly announcer = inject(Announcer);
  private readonly injector = inject(Injector);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly discardButton =
    viewChild.required<ElementRef<HTMLButtonElement>>('discardButton');

  protected readonly statuses = STATUSES;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly workModes = WORK_MODES;
  protected readonly workModeLabels = WORK_MODE_LABELS;

  protected readonly editing = signal<JobApplication | null>(null);
  protected readonly model = signal<ApplicationFormModel>(emptyApplicationForm());
  protected readonly form = form(this.model, applicationSchema);
  protected readonly formError = signal('');
  /** Set when the user tried to close a form with unsaved changes; reveals the discard confirmation. */
  protected readonly confirmingDiscard = signal(false);

  protected readonly heading = computed(() =>
    this.editing() ? 'Edit application' : 'Add application',
  );

  open(app?: JobApplication): void {
    this.editing.set(app ?? null);
    const value = app ? toApplicationForm(app) : emptyApplicationForm();
    this.model.set(value);
    this.form().reset(value);
    this.formError.set('');
    this.confirmingDiscard.set(false);
    this.dialog().nativeElement.showModal();
  }

  /** Field has a message worth showing: only after the user has interacted with it or tried to submit. */
  protected showError(field: FieldTree<unknown>): boolean {
    return field().touched() && field().invalid();
  }

  protected errorText(field: FieldTree<unknown>): string {
    return field().errors()[0]?.message ?? 'Check this field.';
  }

  protected async save(event: Event): Promise<void> {
    event.preventDefault();
    this.formError.set('');
    await submit(this.form, {
      action: async () => {
        const current = this.editing();
        const request = toApplicationRequest(this.model());
        try {
          const result = current
            ? await this.api.update(current.id, request)
            : await this.api.create(request);
          this.close();
          this.announcer.announce(
            current ? 'Changes saved' : `Added ${result.title} at ${result.company}`,
          );
          this.saved.emit(result);
          return null;
        } catch (error) {
          return this.toFieldErrors(error);
        }
      },
      onInvalid: () => this.focusFirstInvalid(),
    });
    if (this.form().invalid()) {
      this.focusFirstInvalid();
    }
  }

  /** Cancel button and Esc both route here so unsaved edits are never lost silently. */
  protected requestClose(): void {
    if (this.form().dirty() && !this.confirmingDiscard()) {
      this.confirmingDiscard.set(true);
      // Move focus to Discard so its description (the unsaved-changes message) is read out.
      afterNextRender(() => this.discardButton().nativeElement.focus(), {
        injector: this.injector,
      });
      return;
    }
    this.close();
  }

  protected onCancel(event: Event): void {
    event.preventDefault();
    this.requestClose();
  }

  protected close(): void {
    this.confirmingDiscard.set(false);
    this.dialog().nativeElement.close();
  }

  private focusFirstInvalid(): void {
    this.form().errorSummary()[0]?.fieldTree().focusBoundControl();
  }

  /** Maps API problem details onto fields; anything not tied to a field becomes a form-level message. */
  private toFieldErrors(error: unknown): ValidationError.WithOptionalFieldTree[] {
    const problem =
      error instanceof HttpErrorResponse ? (error.error as ProblemDetail | null) : null;
    const fields = this.form as unknown as Record<string, FieldTree<unknown> | undefined>;
    const errors: ValidationError.WithOptionalFieldTree[] = [];

    for (const [name, message] of Object.entries(problem?.errors ?? {})) {
      const fieldTree = fields[name];
      if (fieldTree) errors.push({ kind: 'server', message, fieldTree });
    }

    if (errors.length === 0) {
      this.formError.set(
        error instanceof HttpErrorResponse && error.status === 0
          ? 'Can’t reach the server. Check that the API is running, then save again.'
          : (problem?.detail ?? 'The application couldn’t be saved. Try again.'),
      );
    } else {
      queueMicrotask(() => this.focusFirstInvalid());
    }
    return errors;
  }
}
