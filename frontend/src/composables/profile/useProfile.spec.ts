import { describe, it, expect } from 'vitest';
import { parseProfileExtras, maskEmail, getProfileRoleLabel } from './useProfile';

describe('parseProfileExtras', () => {
  it('returns empty object for blank input', () => {
    expect(parseProfileExtras()).toEqual({});
    expect(parseProfileExtras('')).toEqual({});
  });

  it('parses department and bio from preferences json', () => {
    expect(
      parseProfileExtras(JSON.stringify({ department: '计算机学院', bio: 'AI 教学' }))
    ).toEqual({
      department: '计算机学院',
      bio: 'AI 教学'
    });
  });

  it('returns empty strings for invalid field types', () => {
    expect(parseProfileExtras(JSON.stringify({ department: 1, bio: null }))).toEqual({
      department: '',
      bio: ''
    });
  });

  it('returns empty object for malformed json', () => {
    expect(parseProfileExtras('{bad json')).toEqual({});
  });
});

describe('maskEmail', () => {
  it('masks standard email addresses', () => {
    expect(maskEmail('teacher@univ.edu.cn')).toBe('te***r@univ.edu.cn');
  });

  it('returns short local part unchanged pattern', () => {
    expect(maskEmail('ab@test.com')).toBe('ab***@test.com');
  });

  it('returns original value when @ is missing', () => {
    expect(maskEmail('invalid-email')).toBe('invalid-email');
  });
});

describe('getProfileRoleLabel', () => {
  it('maps known roles to display labels', () => {
    expect(getProfileRoleLabel('ADMIN')).toBe('平台超级管理员');
    expect(getProfileRoleLabel('TEACHER')).toBe('任课主讲教师');
    expect(getProfileRoleLabel('STUDENT')).toBe('在校本科生');
  });

  it('falls back for unknown roles', () => {
    expect(getProfileRoleLabel('CUSTOM')).toBe('认证用户');
  });
});

