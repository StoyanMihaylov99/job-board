import { ChangeDetectionStrategy, Component, ElementRef, input, viewChild } from '@angular/core';

let nextId = 0;

/** Native modal confirmation. `open()` resolves to true only when the confirm button was used. */
@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly heading = input.required<string>();
  readonly message = input('');
  readonly confirmLabel = input('Confirm');

  protected readonly titleId = `confirm-title-${nextId++}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private resolve?: (confirmed: boolean) => void;

  open(): Promise<boolean> {
    this.resolve?.(false);
    this.dialog().nativeElement.showModal();
    return new Promise((resolve) => (this.resolve = resolve));
  }

  protected onClose(): void {
    const dialog = this.dialog().nativeElement;
    // Esc / light dismiss set no value; treat anything but "confirm" as a cancel, then reset to avoid stale values.
    const confirmed = dialog.returnValue === 'confirm';
    dialog.returnValue = '';
    this.resolve?.(confirmed);
    this.resolve = undefined;
  }
}
