import { describe, it, expect, vi } from 'vitest';
import { createConfirmPasswordValidator, createRegisterRules } from './useRegister';

describe('createConfirmPasswordValidator', () => {
  it('rejects mismatched passwords', () => {
    const validator = createConfirmPasswordValidator(() => 'secret123');
    const callback = vi.fn();

    validator({}, 'other', callback);

    expect(callback).toHaveBeenCalledWith(new Error('两次输入的密码不一致'));
  });

  it('accepts matching passwords', () => {
    const validator = createConfirmPasswordValidator(() => 'secret123');
    const callback = vi.fn();

    validator({}, 'secret123', callback);

    expect(callback).toHaveBeenCalledWith();
  });
});

describe('createRegisterRules', () => {
  it('includes required fields and confirm password validator', () => {
    const rules = createRegisterRules(() => 'abc123');

    expect(rules.username).toHaveLength(2);
    expect(rules.realName).toHaveLength(1);
    expect(rules.password).toHaveLength(2);
    expect(rules.confirmPassword).toHaveLength(2);
  });
});
