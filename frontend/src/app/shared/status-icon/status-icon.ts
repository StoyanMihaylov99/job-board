import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ApplicationStatus } from '../../applications/application.model';

/**
 * Status glyph: the circle fills up as an application progresses, and closed outcomes get a mark.
 * Shape carries the meaning as well as colour, so statuses stay distinguishable without colour vision.
 * Decorative: always shown next to the status name.
 */
@Component({
  selector: 'app-status-icon',
  host: { 'aria-hidden': 'true', '[attr.data-status]': 'status()' },
  template: `
    <svg viewBox="0 0 14 14" fill="none">
      @switch (status()) {
        @case ('WISHLIST') {
          <circle
            cx="7"
            cy="7"
            r="5.75"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-dasharray="2.2 2"
          />
        }
        @case ('APPLIED') {
          <circle cx="7" cy="7" r="5.75" stroke="currentColor" stroke-width="1.5" />
        }
        @case ('INTERVIEWING') {
          <circle cx="7" cy="7" r="5.75" stroke="currentColor" stroke-width="1.5" />
          <path d="M7 7V3.25a3.75 3.75 0 0 1 0 7.5Z" fill="currentColor" />
        }
        @case ('OFFER') {
          <circle cx="7" cy="7" r="5.75" stroke="currentColor" stroke-width="1.5" />
          <path d="M7 7V3.25a3.75 3.75 0 1 1-3.75 3.75Z" fill="currentColor" />
        }
        @case ('ACCEPTED') {
          <circle cx="7" cy="7" r="6.5" fill="currentColor" />
          <path
            d="m4.4 7.1 1.8 1.8 3.5-3.6"
            stroke="var(--ink-950)"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        }
        @case ('REJECTED') {
          <circle cx="7" cy="7" r="6.5" fill="currentColor" />
          <path
            d="m4.9 4.9 4.2 4.2m0-4.2-4.2 4.2"
            stroke="var(--ink-950)"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        }
        @case ('WITHDRAWN') {
          <circle cx="7" cy="7" r="6.5" fill="currentColor" />
          <path d="M4.5 7h5" stroke="var(--ink-950)" stroke-width="1.5" stroke-linecap="round" />
        }
        @case ('GHOSTED') {
          <circle
            cx="7"
            cy="7"
            r="5.75"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-dasharray="0.1 2.6"
            stroke-linecap="round"
          />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
      inline-size: 0.875rem;
      block-size: 0.875rem;
      color: var(--status);
    }

    svg {
      inline-size: 100%;
      block-size: 100%;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusIcon {
  readonly status = input.required<ApplicationStatus>();
}
