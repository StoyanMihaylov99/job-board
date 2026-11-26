import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

/** "Show" button that reveals the text of the password input it controls. */
@Component({
  selector: 'app-password-toggle',
  template: `
    <button
      type="button"
      class="toggle"
      [attr.aria-controls]="control().id"
      [attr.aria-pressed]="visible()"
      (click)="toggle()"
    >
      Show<span class="visually-hidden"> password</span>
    </button>
  `,
  styles: `
    .toggle {
      min-block-size: 2rem;
      padding-inline: var(--space-3);
      border: 0;
      border-radius: var(--radius-s);
      background: transparent;
      color: var(--text-muted);
      font: inherit;
      font-size: var(--step--1);
      font-weight: 550;
      cursor: pointer;

      &:hover {
        color: var(--text);
      }

      &[aria-pressed='true'] {
        background: var(--ink-800);
        color: var(--text);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasswordToggle {
  readonly control = input.required<HTMLInputElement>();
  protected readonly visible = signal(false);

  protected toggle(): void {
    this.visible.update((v) => !v);
    this.control().type = this.visible() ? 'text' : 'password';
  }
}
