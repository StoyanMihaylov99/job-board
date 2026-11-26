import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ApplicationStatus, STATUS_LABELS, STATUSES, StatusStats } from '../application.model';

interface Segment {
  status: ApplicationStatus;
  label: string;
  count: number;
  share: number;
}

/**
 * Proportional bar of where every application currently sits, with a legend that doubles as the status filter.
 */
@Component({
  selector: 'app-pipeline',
  templateUrl: './pipeline.html',
  styleUrl: './pipeline.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pipeline {
  readonly stats = input.required<StatusStats>();
  readonly selected = input<ApplicationStatus | null>(null);
  readonly selectStatus = output<ApplicationStatus | null>();

  protected readonly segments = computed<Segment[]>(() => {
    const { total, byStatus } = this.stats();
    return STATUSES.map((status) => ({
      status,
      label: STATUS_LABELS[status],
      count: byStatus[status] ?? 0,
      share: total ? ((byStatus[status] ?? 0) / total) * 100 : 0,
    }));
  });

  protected readonly visibleSegments = computed(() => this.segments().filter((s) => s.count > 0));

  protected toggle(status: ApplicationStatus): void {
    this.selectStatus.emit(this.selected() === status ? null : status);
  }
}
