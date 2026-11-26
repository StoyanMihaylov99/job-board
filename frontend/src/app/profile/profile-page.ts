import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
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

import { AuthService } from '../auth/auth.service';
import { MAX_PASSWORD_BYTES } from '../auth/register-page/register-page';
import { Announcer } from '../shared/announcer/announcer';
import { PasswordToggle } from '../shared/password-toggle/password-toggle';
import { toServerErrors } from '../shared/problem';
import { ProfileApi } from './profile.api';

interface NameModel {
  displayName: string;
}

interface PasswordModel {
  currentPassword: string;
  newPassword: string;
}

const nameSchema = schema<NameModel>((p) => {
  required(p.displayName, { message: 'Enter your name.' });
  maxLength(p.displayName, 100, { message: 'Keep your name under 100 characters.' });
});

const passwordSchema = schema<PasswordModel>((p) => {
  required(p.currentPassword, { message: 'Enter your current password.' });
  required(p.newPassword, { message: 'Choose a new password.' });
  minLength(p.newPassword, 8, { message: 'Use at least 8 characters.' });
  validate(p.newPassword, ({ value }) =>
    new TextEncoder().encode(value()).length > MAX_PASSWORD_BYTES
      ? { kind: 'tooLong', message: 'That password is too long. Use up to 72 characters.' }
      : null,
  );
  validate(p.newPassword, ({ value, valueOf }) =>
    value() && value() === valueOf(p.currentPassword)
      ? { kind: 'same', message: 'Choose a password different from the current one.' }
      : null,
  );
});

@Component({
  selector: 'app-profile-page',
  imports: [DatePipe, FormField, PasswordToggle],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ProfileApi);
  private readonly announcer = inject(Announcer);

  protected readonly user = this.auth.user;

  protected readonly nameModel = signal<NameModel>({
    displayName: this.auth.user()?.displayName ?? '',
  });
  protected readonly nameForm = form(this.nameModel, nameSchema);
  protected readonly nameError = signal('');

  protected readonly passwordModel = signal<PasswordModel>({
    currentPassword: '',
    newPassword: '',
  });
  protected readonly passwordForm = form(this.passwordModel, passwordSchema);
  protected readonly passwordError = signal('');

  protected showError(field: FieldTree<unknown>): boolean {
    return field().touched() && field().invalid();
  }

  protected errorText(field: FieldTree<unknown>): string {
    return field().errors()[0]?.message ?? 'Check this field.';
  }

  protected async saveName(event: Event): Promise<void> {
    event.preventDefault();
    this.nameError.set('');
    await submit(this.nameForm, {
      action: async () => {
        try {
          const user = await this.api.updateDisplayName(this.nameModel().displayName.trim());
          this.auth.updateUser(user);
          this.nameForm().reset({ displayName: user.displayName });
          this.announcer.announce('Name saved');
          return null;
        } catch (error) {
          const fields = this.nameForm as unknown as Record<string, FieldTree<unknown>>;
          const { fieldErrors, formMessage } = toServerErrors(
            error,
            fields,
            'Your name couldn’t be saved. Try again.',
          );
          this.nameError.set(formMessage ?? '');
          return fieldErrors;
        }
      },
      onInvalid: () => this.nameForm.displayName().focusBoundControl(),
    });
  }

  protected async changePassword(event: Event): Promise<void> {
    event.preventDefault();
    this.passwordError.set('');
    await submit(this.passwordForm, {
      action: async () => {
        const { currentPassword, newPassword } = this.passwordModel();
        try {
          this.auth.applySession(await this.api.changePassword(currentPassword, newPassword));
          const cleared = { currentPassword: '', newPassword: '' };
          this.passwordModel.set(cleared);
          this.passwordForm().reset(cleared);
          this.announcer.announce('Password changed. Other devices have been signed out.');
          return null;
        } catch (error) {
          const fields = this.passwordForm as unknown as Record<string, FieldTree<unknown>>;
          const { fieldErrors, formMessage } = toServerErrors(
            error,
            fields,
            'Your password couldn’t be changed. Try again.',
          );
          this.passwordError.set(formMessage ?? '');
          if (fieldErrors.length) queueMicrotask(() => this.focusFirstInvalid());
          return fieldErrors;
        }
      },
      onInvalid: () => this.focusFirstInvalid(),
    });
  }

  private focusFirstInvalid(): void {
    this.passwordForm().errorSummary()[0]?.fieldTree().focusBoundControl();
  }
}
