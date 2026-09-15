import { describe, it, expect, vi } from 'vitest';

vi.mock('@/api/auth/auth', () => ({
  createSliderChallenge: vi.fn(),
  verifySliderCaptcha: vi.fn()
}));

describe('useAuthCaptcha', () => {
  it('exports challenge helpers and composable factory', async () => {
    const { useAuthCaptcha, loadSliderChallenge, verifySliderChallenge } = await import('./useAuthCaptcha');

    expect(typeof useAuthCaptcha).toBe('function');
    expect(typeof loadSliderChallenge).toBe('function');
    expect(typeof verifySliderChallenge).toBe('function');

    const { loadSliderChallenge: load, verifySliderChallenge: verify } = useAuthCaptcha();
    expect(load).toBe(loadSliderChallenge);
    expect(verify).toBe(verifySliderChallenge);
  });
});
