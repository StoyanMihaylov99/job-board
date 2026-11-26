import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Announcer } from '../../shared/announcer/announcer';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { formatPlace, formatSalary } from '../../shared/format';
import { ApplicationDialog } from '../application-dialog/application-dialog';
import {
  ACTIVE_STATUSES,
  ApplicationStatus,
  JobApplication,
  ListQuery,
  SortOption,
  STATUS_LABELS,
  STATUSES,
} from '../application.model';
import { ApplicationsApi } from '../applications.api';
import { StatusIcon } from '../../shared/status-icon/status-icon';
import { Pipeline } from '../pipeline/pipeline';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, Pipeline, ApplicationDialog, ConfirmDialog, StatusIcon],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  private readonly api = inject(ApplicationsApi);
  private readonly announcer = inject(Announcer);
  private readonly editor = viewChild.required(ApplicationDialog);
  private readonly confirmDelete = viewChild.required<ConfirmDialog>('confirmDelete');

  protected readonly statuses = STATUSES;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly sortOptions: { value: SortOption; label: string }[] = [
    { value: 'createdAt,desc', label: 'Recently added' },
    { value: 'updatedAt,desc', label: 'Recently updated' },
    { value: 'appliedDate,desc', label: 'Applied date' },
    { value: 'company,asc', label: 'Company A–Z' },
  ];
  protected readonly formatSalary = formatSalary;
  protected readonly formatPlace = formatPlace;

  // Filters
  protected readonly status = signal<ApplicationStatus | null>(null);
  protected readonly search = signal('');
  protected readonly sort = signal<SortOption>('createdAt,desc');
  protected readonly page = signal(0);
  private readonly debouncedSearch = toSignal(
    toObservable(this.search).pipe(debounceTime(250), distinctUntilChanged()),
    { initialValue: '' },
  );

  private readonly query = computed<ListQuery>(() => ({
    status: this.status(),
    q: this.debouncedSearch(),
    sort: this.sort(),
    page: this.page(),
    size: PAGE_SIZE,
  }));

  protected readonly list = this.api.listResource(this.query);
  protected readonly stats = this.api.statsResource();

  /** Keep showing the last good page while a new one loads, so the table doesn't flash empty. */
  protected readonly rows = computed(() => (this.list.hasValue() ? this.list.value().content : []));
  protected readonly pageInfo = computed(() =>
    this.list.hasValue() ? this.list.value().page : null,
  );
  protected readonly hasFilters = computed(() => !!this.status() || !!this.search().trim());
  protected readonly pendingId = signal<string | null>(null);
  protected readonly deleting = signal<JobApplication | null>(null);

  protected readonly summary = computed(() => {
    if (!this.stats.hasValue()) return null;
    const { total, byStatus } = this.stats.value();
    const active = ACTIVE_STATUSES.reduce((sum, s) => sum + (byStatus[s] ?? 0), 0);
    return { total, active };
  });

  protected readonly rangeLabel = computed(() => {
    const info = this.pageInfo();
    if (!info || info.totalElements === 0) return '';
    const from = info.number * info.size + 1;
    const to = Math.min(from + info.size - 1, info.totalElements);
    return `${from}–${to} of ${info.totalElements}`;
  });

  protected setStatusFilter(status: ApplicationStatus | null): void {
    this.status.set(status);
    this.page.set(0);
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected onSort(value: string): void {
    this.sort.set(value as SortOption);
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.status.set(null);
    this.search.set('');
    this.page.set(0);
  }

  protected goToPage(page: number): void {
    this.page.set(page);
  }

  protected add(): void {
    this.editor().open();
  }

  protected edit(app: JobApplication): void {
    this.editor().open(app);
  }

  protected async changeStatus(app: JobApplication, status: string): Promise<void> {
    const next = status as ApplicationStatus;
    if (next === app.status) return;
    this.pendingId.set(app.id);
    try {
      await this.api.updateStatus(app.id, next);
      this.announcer.announce(`${app.company} moved to ${STATUS_LABELS[next]}`);
      this.refresh();
    } catch {
      this.announcer.announce(`Couldn’t update ${app.company}. Try again.`);
      this.list.reload(); // restore the select to the stored value
    } finally {
      this.pendingId.set(null);
    }
  }

  protected async remove(app: JobApplication): Promise<void> {
    this.deleting.set(app);
    const confirmed = await this.confirmDelete().open();
    if (!confirmed) return;
    try {
      await this.api.delete(app.id);
      this.announcer.announce(`Deleted ${app.title} at ${app.company}`);
      // Step back a page if this removed the last row on it.
      if (this.rows().length === 1 && this.page() > 0) this.page.update((p) => p - 1);
      this.refresh();
    } catch {
      this.announcer.announce(`Couldn’t delete ${app.company}. Try again.`);
    }
  }

  protected refresh(): void {
    this.list.reload();
    this.stats.reload();
  }
}
