import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  ApplicationStatus,
  JobApplication,
  JobApplicationRequest,
  ListQuery,
  Page,
  StatusStats,
} from './application.model';

const BASE_URL = '/api/v1/applications';

@Injectable({ providedIn: 'root' })
export class ApplicationsApi {
  private readonly http = inject(HttpClient);

  /** Reactive, paged list that refetches whenever the query changes. Call from an injection context. */
  listResource(query: () => ListQuery) {
    return httpResource<Page<JobApplication>>(() => {
      const { status, q, sort, page, size } = query();
      const params: Record<string, string | number> = { sort, page, size };
      if (status) params['status'] = status;
      if (q.trim()) params['q'] = q.trim();
      return { url: BASE_URL, params };
    });
  }

  /** Reactive per-status counts. Call from an injection context. */
  statsResource() {
    return httpResource<StatusStats>(() => `${BASE_URL}/stats`);
  }

  create(request: JobApplicationRequest): Promise<JobApplication> {
    return firstValueFrom(this.http.post<JobApplication>(BASE_URL, request));
  }

  update(id: string, request: JobApplicationRequest): Promise<JobApplication> {
    return firstValueFrom(this.http.put<JobApplication>(`${BASE_URL}/${id}`, request));
  }

  updateStatus(id: string, status: ApplicationStatus): Promise<JobApplication> {
    return firstValueFrom(this.http.patch<JobApplication>(`${BASE_URL}/${id}/status`, { status }));
  }

  delete(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }
}
