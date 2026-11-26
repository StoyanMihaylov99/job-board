import { Injectable, signal } from '@angular/core';

/**
 * Single polite status message for the whole app. The shell renders it in one
 * visible `role="status"` region so screen readers and sighted users get the same feedback.
 */
@Injectable({ providedIn: 'root' })
export class Announcer {
  private readonly _message = signal('');
  private timer?: ReturnType<typeof setTimeout>;

  readonly message = this._message.asReadonly();

  announce(message: string): void {
    clearTimeout(this.timer);
    // Clear first so repeating the same message is announced again.
    this._message.set('');
    queueMicrotask(() => this._message.set(message));
    this.timer = setTimeout(() => this._message.set(''), 4000);
  }
}
