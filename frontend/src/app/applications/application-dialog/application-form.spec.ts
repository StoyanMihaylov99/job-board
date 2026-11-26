import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';

import {
  ApplicationFormModel,
  applicationSchema,
  emptyApplicationForm,
  toApplicationRequest,
} from './application-form';

describe('application form', () => {
  function createForm(overrides: Partial<ApplicationFormModel> = {}) {
    return TestBed.runInInjectionContext(() =>
      form(signal({ ...emptyApplicationForm(), ...overrides }), applicationSchema),
    );
  }

  it('requires title and company', () => {
    const f = createForm();
    expect(f.title().errors()[0]?.message).toBe('Enter the job title.');
    expect(f.company().errors()[0]?.message).toBe('Enter the company name.');
  });

  it('is valid with only title and company', () => {
    expect(createForm({ title: 'Dev', company: 'Acme' })().valid()).toBe(true);
  });

  it('rejects a maximum salary below the minimum', () => {
    const f = createForm({ title: 'Dev', company: 'Acme', salaryMin: 5000, salaryMax: 4000 });
    expect(
      f
        .salaryMax()
        .errors()
        .map((e) => e.kind),
    ).toContain('range');
  });

  it('rejects future dates, bad currency codes and non-http links', () => {
    const f = createForm({
      title: 'Dev',
      company: 'Acme',
      appliedDate: '2999-01-01',
      salaryCurrency: 'EURO',
      jobUrl: 'ftp://example.com',
    });
    expect(f.appliedDate().invalid()).toBe(true);
    expect(f.salaryCurrency().invalid()).toBe(true);
    expect(f.jobUrl().invalid()).toBe(true);
  });

  it('maps the form model to a trimmed API request with nulls for empty fields', () => {
    const request = toApplicationRequest({
      ...emptyApplicationForm(),
      title: '  Dev  ',
      company: ' Acme ',
      appliedDate: '',
      salaryCurrency: 'eur',
      notes: '   ',
    });
    expect(request).toMatchObject({
      title: 'Dev',
      company: 'Acme',
      appliedDate: null,
      salaryCurrency: 'EUR',
      notes: null,
      workMode: null,
      position: null,
    });
  });
});
