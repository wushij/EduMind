import { describe, it, expect } from 'vitest';
import { FORGOT_PASSWORD_EMAIL_REGEX } from './useForgotPassword';

describe('FORGOT_PASSWORD_EMAIL_REGEX', () => {
  it('accepts valid email addresses', () => {
    expect(FORGOT_PASSWORD_EMAIL_REGEX.test('teacher@univ.edu.cn')).toBe(true);
    expect(FORGOT_PASSWORD_EMAIL_REGEX.test('name.surname+tag@example.com')).toBe(true);
  });

  it('rejects invalid email addresses', () => {
    expect(FORGOT_PASSWORD_EMAIL_REGEX.test('invalid-email')).toBe(false);
    expect(FORGOT_PASSWORD_EMAIL_REGEX.test('missing-domain@')).toBe(false);
  });
});
