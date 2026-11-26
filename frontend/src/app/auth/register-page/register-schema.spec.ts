import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';

import { registerSchema } from './register-page';

describe('registration rules', () => {
  const build = (password: string, email = 'ana@example.com', displayName = 'Ana') =>
    TestBed.runInInjectionContext(() =>
      form(signal({ displayName, email, password }), registerSchema),
    );

  it('accepts a valid registration', () => {
    expect(build('long enough')().valid()).toBe(true);
  });

  it('requires at least 8 characters', () => {
    expect(build('short').password().errors()[0]?.message).toBe('Use at least 8 characters.');
  });

  it('rejects passwords over 72 bytes even when under 72 characters', () => {
    expect(
      build('ж'.repeat(40))
        .password()
        .errors()
        .map((e) => e.kind),
    ).toContain('tooLong');
  });

  it('checks the email format', () => {
    expect(build('long enough', 'not-an-email').email().invalid()).toBe(true);
  });
});
