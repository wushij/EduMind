import { describe, expect, it } from 'vitest';
import { citationMatchPercent, formatCitationMatchLabel } from './citation-score';

describe('citation-score', () => {
  it('normalizes RRF scores within batch to 0-100', () => {
    const peers = [0.052, 0.048, 0.05, 0.044, 0.041];
    expect(citationMatchPercent(0.052, peers)).toBe(100);
    expect(citationMatchPercent(0.048, peers)).toBe(92);
    expect(formatCitationMatchLabel(0.05, peers)).toBe('96%');
  });

  it('keeps cosine-like scores as direct percent', () => {
    expect(citationMatchPercent(0.86, [0.86, 0.72])).toBe(86);
  });
});
