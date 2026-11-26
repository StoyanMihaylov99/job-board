import { formatPlace, formatSalary } from './format';

describe('formatSalary', () => {
  it('formats a range with currency', () => {
    expect(formatSalary({ salaryMin: 3000, salaryMax: 4500, salaryCurrency: 'EUR' })).toMatch(
      /3,000.*4,500/,
    );
  });

  it('formats open-ended ranges', () => {
    expect(formatSalary({ salaryMin: 3000, salaryMax: null, salaryCurrency: null })).toBe(
      'from 3,000',
    );
    expect(formatSalary({ salaryMin: null, salaryMax: 4500, salaryCurrency: null })).toBe(
      'up to 4,500',
    );
  });

  it('returns an empty string when there is no salary', () => {
    expect(formatSalary({ salaryMin: null, salaryMax: null, salaryCurrency: 'EUR' })).toBe('');
  });
});

describe('formatPlace', () => {
  it('combines location and work mode', () => {
    expect(formatPlace({ location: 'Sofia', workMode: 'HYBRID' })).toBe('Sofia, hybrid');
    expect(formatPlace({ location: null, workMode: 'REMOTE' })).toBe('Remote');
    expect(formatPlace({ location: null, workMode: null })).toBe('');
  });
});
