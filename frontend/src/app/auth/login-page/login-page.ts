import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import {
  email,
  form,
  FormField,
  required,
  schema,
  submit,
  FieldTree,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';

import { PasswordToggle } from '../../shared/password-toggle/password-toggle';
import { toServerErrors } from '../../shared/problem';
import { AuthLayout } from '../auth-layout/auth-layout';
import { safeReturnUrl } from '../auth.guards';
import { AuthService } from '../auth.service';

interface LoginModel {
  email: string;
  password: string;
}

const loginSchema = schema<LoginModel>((p) => {
  required(p.email, { message: 'Enter your email address.' });
  email(p.email, { message: 'Enter an email address like name@example.com.' });
  required(p.password, { message: 'Enter your password.' });
});

@Component({
  selector: 'app-login-page',
  imports: [AuthLayout, FormField, PasswordToggle, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: '../auth-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  /** Bound from the query string by the router. */
  readonly returnUrl = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly model = signal<LoginModel>({ email: '', password: '' });
  protected readonly form = form(this.model, loginSchema);
  protected readonly formError = signal('');

  protected showError(field: FieldTree<unknown>): boolean {
    return field().touched() && field().invalid();
  }

  protected errorText(field: FieldTree<unknown>): string {
    return field().errors()[0]?.message ?? 'Check this field.';
  }

  protected async signIn(event: Event): Promise<void> {
    event.preventDefault();
    this.formError.set('');
    await submit(this.form, {
      action: async () => {
        try {
          await this.auth.login(this.model());
          await this.router.navigateByUrl(safeReturnUrl(this.returnUrl()));
          return null;
        } catch (error) {
          const { fieldErrors, formMessage } = toServerErrors(
            error,
            {},
            'Sign-in failed. Try again.',
          );
          this.formError.set(formMessage ?? '');
          return fieldErrors;
        }
      },
      onInvalid: () => this.form().errorSummary()[0]?.fieldTree().focusBoundControl(),
    });
  }
}
