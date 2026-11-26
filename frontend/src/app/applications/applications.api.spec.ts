import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApplicationsApi } from './applications.api';
import { JobApplicationRequest } from './application.model';

describe('ApplicationsApi', () => {
  let api: ApplicationsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApplicationsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates an application with POST', async () => {
    const request = { title: 'Dev', company: 'Acme' } as JobApplicationRequest;
    const result = api.create(request);
    const req = http.expectOne('/api/v1/applications');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: '1', ...request });
    await expect(result).resolves.toMatchObject({ id: '1' });
  });

  it('changes status with PATCH', async () => {
    const result = api.updateStatus('42', 'OFFER');
    const req = http.expectOne('/api/v1/applications/42/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'OFFER' });
    req.flush({ id: '42', status: 'OFFER' });
    await result;
  });

  it('deletes with DELETE', async () => {
    const result = api.delete('42');
    const req = http.expectOne('/api/v1/applications/42');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await result;
  });

  it('builds list query params from filters', () => {
    TestBed.runInInjectionContext(() =>
      api.listResource(() => ({
        status: 'APPLIED',
        q: ' acme ',
        sort: 'createdAt,desc',
        page: 1,
        size: 20,
      })),
    );
    TestBed.tick();
    const req = http.expectOne((r) => r.url === '/api/v1/applications');
    expect(req.request.params.get('status')).toBe('APPLIED');
    expect(req.request.params.get('q')).toBe('acme');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('sort')).toBe('createdAt,desc');
    req.flush({ content: [], page: { size: 20, number: 1, totalElements: 0, totalPages: 0 } });
  });
});
