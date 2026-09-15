import { describe, it, expect } from 'vitest';
import { useLogin } from './useLogin';

describe('useLogin', () => {
  it('exports composable factory', () => {
    expect(typeof useLogin).toBe('function');
  });
});
