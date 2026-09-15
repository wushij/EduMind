import { describe, it, expect } from 'vitest';
import {
  calcPasswordStrength,
  calcSecurityScore,
  calcSecurityLevel,
  maskEmail,
  maskPhone,
  type SecurityFactors
} from './useSecurity';

describe('calcPasswordStrength', () => {
  it('returns empty result for blank password', () => {
    expect(calcPasswordStrength('')).toEqual({
      score: 0,
      label: '',
      class: '',
      color: '#e2e8f0'
    });
  });

  it('rates short simple password as weak', () => {
    const result = calcPasswordStrength('abc');
    expect(result.score).toBe(1);
    expect(result.class).toBe('weak');
    expect(result.label).toBe('弱 (建议加强)');
  });

  it('rates 8+ chars with letters and numbers as medium', () => {
    const result = calcPasswordStrength('password1');
    expect(result.score).toBe(2);
    expect(result.class).toBe('medium');
    expect(result.label).toBe('中等 (符合要求)');
  });

  it('rates mixed case with numbers as strong', () => {
    const result = calcPasswordStrength('Password1');
    expect(result.score).toBe(3);
    expect(result.class).toBe('strong');
  });

  it('rates complex password with symbol as strong', () => {
    const result = calcPasswordStrength('Password1!');
    expect(result.score).toBe(3);
    expect(result.class).toBe('strong');
    expect(result.label).toBe('极强 (安全稳固)');
  });
});

describe('calcSecurityScore', () => {
  const emptyFactors: SecurityFactors = {
    hasSession: false,
    hasRole: false,
    hasEmail: false,
    hasPhone: false,
    hasRealName: false,
    hasAvatar: false
  };

  it('returns 0 when no security factors are present', () => {
    expect(calcSecurityScore(emptyFactors)).toBe(0);
  });

  it('adds session and role points', () => {
    expect(calcSecurityScore({ ...emptyFactors, hasSession: true, hasRole: true })).toBe(30);
  });

  it('adds email binding bonus', () => {
    expect(calcSecurityScore({ ...emptyFactors, hasEmail: true })).toBe(30);
  });

  it('awards combo bonus when email and phone are both bound', () => {
    expect(calcSecurityScore({ ...emptyFactors, hasEmail: true, hasPhone: true })).toBe(55);
  });

  it('caps score at 100 for fully complete profile', () => {
    expect(
      calcSecurityScore({
        hasSession: true,
        hasRole: true,
        hasEmail: true,
        hasPhone: true,
        hasRealName: true,
        hasAvatar: true
      })
    ).toBe(100);
  });
});

describe('calcSecurityLevel', () => {
  const minimalFactors: SecurityFactors = {
    hasSession: true,
    hasRole: true,
    hasEmail: false,
    hasPhone: false,
    hasRealName: false,
    hasAvatar: false
  };

  it('returns warn level for low scores', () => {
    const level = calcSecurityLevel(30, minimalFactors);
    expect(level.class).toBe('warn');
    expect(level.label).toBe('等级偏低');
    expect(level.tip).toContain('密保邮箱');
  });

  it('returns good level for mid-range scores', () => {
    const level = calcSecurityLevel(65, {
      ...minimalFactors,
      hasEmail: true,
      hasPhone: true
    });
    expect(level.class).toBe('good');
    expect(level.label).toBe('防护良好');
  });

  it('returns excellent level for high scores', () => {
    const level = calcSecurityLevel(90, {
      hasSession: true,
      hasRole: true,
      hasEmail: true,
      hasPhone: true,
      hasRealName: true,
      hasAvatar: true
    });
    expect(level.class).toBe('excellent');
    expect(level.label).toBe('安全极佳');
  });
});

describe('maskEmail', () => {
  it('masks standard email addresses', () => {
    expect(maskEmail('teacher@example.com')).toBe('te***r@example.com');
  });

  it('handles short local parts', () => {
    expect(maskEmail('ab@test.com')).toBe('ab***@test.com');
  });
});

describe('maskPhone', () => {
  it('masks 11-digit mobile numbers', () => {
    expect(maskPhone('13812345678')).toBe('138****5678');
  });
});
