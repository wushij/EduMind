import { describe, it, expect } from 'vitest';
import { normalizeCourseAiPersona, getCourseAiPersonaLabel } from './ai-persona';

describe('normalizeCourseAiPersona', () => {
  it('maps legacy edit-drawer keys to canonical ids', () => {
    expect(normalizeCourseAiPersona('SOCRATIC')).toBe('socrates');
    expect(normalizeCourseAiPersona('STRICT')).toBe('academic');
    expect(normalizeCourseAiPersona('PRACTICAL')).toBe('engineer');
    expect(normalizeCourseAiPersona('GENTLE')).toBe('gentle');
  });

  it('keeps create-flow ids', () => {
    expect(normalizeCourseAiPersona('socrates')).toBe('socrates');
    expect(normalizeCourseAiPersona('engineer')).toBe('engineer');
  });

  it('labels normalized persona', () => {
    expect(getCourseAiPersonaLabel('SOCRATIC')).toContain('苏格拉底');
  });
});
