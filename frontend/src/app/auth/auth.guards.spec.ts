import { safeReturnUrl } from './auth.guards';

describe('safeReturnUrl', () => {
  it('keeps in-app paths', () => {
    expect(safeReturnUrl('/profile')).toBe('/profile');
    expect(safeReturnUrl('/?status=OFFER')).toBe('/?status=OFFER');
  });

  it('falls back to home for anything that could leave the app', () => {
    expect(safeReturnUrl(undefined)).toBe('/');
    expect(safeReturnUrl('https://evil.example')).toBe('/');
    expect(safeReturnUrl('//evil.example')).toBe('/');
    expect(safeReturnUrl('/\\evil.example')).toBe('/');
  });
});
