import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import {
  email,
  FieldTree,
  form,
  FormField,
  maxLength,
  minLength,
  required,
  schema,
  submit,
  validate,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';

import { Announcer } from '../../shared/announcer/announcer';
import { PasswordToggle } from '../../shared/password-toggle/password-toggle';
import { toServerErrors } from '../../shared/problem';
import { AuthLayout } from '../auth-layout/auth-layout';
import { safeReturnUrl } from '../auth.guards';
import { AuthService } from '../auth.service';

interface RegisterModel {
  displayName: string;
  email: string;
  password: string;
}

/** Mirrors the API: at least 8 characters, at most 72 bytes (BCrypt's limit). */
export const MAX_PASSWORD_BYTES = 72;

export const registerSchema = schema<RegisterModel>((p) => {
  required(p.displayName, { message: 'Enter your name.' });
  maxLength(p.displayName, 100, { message: 'Keep your name under 100 characters.' });
  required(p.email, { message: 'Enter your email address.' });
  email(p.email, { message: 'Enter an email address like name@example.com.' });
  maxLength(p.email, 254, { message: 'That email address is too long.' });
  required(p.password, { message: 'Choose a password.' });
  minLength(p.password, 8, { message: 'Use at least 8 characters.' });
  validate(p.password, ({ value }) =>
    new TextEncoder().encode(value()).length > MAX_PASSWORD_BYTES
      ? { kind: 'tooLong', message: 'That password is too long. Use up to 72 characters.' }
      : null,
  );
});

@Component({
  selector: 'app-register-page',
  imports: [AuthLayout, FormField, PasswordToggle, RouterLink],
  templateUrl: './register-page.html',
  styleUrl: '../auth-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage {
  readonly returnUrl = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly announcer = inject(Announcer);

  protected readonly model = signal<RegisterModel>({ displayName: '', email: '', password: '' });
  protected readonly form = form(this.model, registerSchema);
  protected readonly formError = signal('');

  protected showError(field: FieldTree<unknown>): boolean {
    return field().touched() && field().invalid();
  }

  protected errorText(field: FieldTree<unknown>): string {
    return field().errors()[0]?.message ?? 'Check this field.';
  }

  protected async createAccount(event: Event): Promise<void> {
    event.preventDefault();
    this.formError.set('');
    await submit(this.form, {
      action: async () => {
        try {
          const user = await this.auth.register(this.model());
          this.announcer.announce(`Welcome, ${user.displayName}`);
          await this.router.navigateByUrl(safeReturnUrl(this.returnUrl()));
          return null;
        } catch (error) {
          const fields = this.form as unknown as Record<string, FieldTree<unknown>>;
          const { fieldErrors, formMessage } = toServerErrors(
            error,
            fields,
            'Your account couldn’t be created. Try again.',
          );
          this.formError.set(formMessage ?? '');
          if (fieldErrors.length) queueMicrotask(() => this.focusFirstInvalid());
          return fieldErrors;
        }
      },
      onInvalid: () => this.focusFirstInvalid(),
    });
  }

  private focusFirstInvalid(): void {
    this.form().errorSummary()[0]?.fieldTree().focusBoundControl();
  }
}
