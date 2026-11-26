import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthResponse } from './auth.model';
import { AuthService } from './auth.service';

const session = (token: string): AuthResponse => ({
  accessToken: token,
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: '1', email: 'a@example.com', displayName: 'Ana', createdAt: '2026-11-26T10:00:00Z' },
});

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    auth.applySession(session('token-1'));
  });

  afterEach(() => backend.verify());

  const flushMicrotasks = () => new Promise((resolve) => setTimeout(resolve));

  it('adds the bearer token to API calls but not to auth endpoints or other hosts', () => {
    http.get('/api/v1/applications').subscribe();
    http.post('/api/v1/auth/refresh', null).subscribe();
    http.get('https://example.com/data').subscribe();

    expect(backend.expectOne('/api/v1/applications').request.headers.get('Authorization')).toBe(
      'Bearer token-1',
    );
    expect(backend.expectOne('/api/v1/auth/refresh').request.headers.has('Authorization')).toBe(
      false,
    );
    expect(backend.expectOne('https://example.com/data').request.headers.has('Authorization')).toBe(
      false,
    );
  });

  it('refreshes once on 401 and retries with the new token', async () => {
    const result = firstValueFrom(http.get<{ ok: boolean }>('/api/v1/applications'));

    backend
      .expectOne('/api/v1/applications')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    await flushMicrotasks();
    backend.expectOne('/api/v1/auth/refresh').flush(session('token-2'));
    await flushMicrotasks();

    const retry = backend.expectOne('/api/v1/applications');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer token-2');
    retry.flush({ ok: true });
    await expect(result).resolves.toEqual({ ok: true });
  });

  it('shares a single refresh between parallel requests', async () => {
    const a = firstValueFrom(http.get('/api/v1/applications'));
    const b = firstValueFrom(http.get('/api/v1/applications/stats'));

    backend
      .expectOne('/api/v1/applications')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    backend
      .expectOne('/api/v1/applications/stats')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    await flushMicrotasks();
    backend.expectOne('/api/v1/auth/refresh').flush(session('token-2'));
    await flushMicrotasks();

    backend.expectOne('/api/v1/applications').flush([]);
    backend.expectOne('/api/v1/applications/stats').flush({});
    await Promise.all([a, b]);
  });

  it('ends the session when the refresh token is rejected', async () => {
    const ended = vi.spyOn(auth, 'sessionEnded').mockResolvedValue();
    const result = firstValueFrom(http.get('/api/v1/applications'));

    backend
      .expectOne('/api/v1/applications')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    await flushMicrotasks();
    backend
      .expectOne('/api/v1/auth/refresh')
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(ended).toHaveBeenCalled();
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('passes other errors through untouched', async () => {
    const result = firstValueFrom(http.get('/api/v1/applications'));
    backend
      .expectOne('/api/v1/applications')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await expect(result).rejects.toMatchObject({ status: 500 });
  });
});
