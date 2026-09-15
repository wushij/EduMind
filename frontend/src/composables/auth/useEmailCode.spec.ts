import { describe, it, expect, vi } from 'vitest';

vi.mock('@/api/auth/auth', () => ({
  sendEmailCode: vi.fn()
}));

vi.mock('@/api/system/user', () => ({
  sendBindEmailCode: vi.fn()
}));

describe('useEmailCode', () => {
  it('exports email code helpers and composable factory', async () => {
    const { useEmailCode, sendAuthEmailCode, sendProfileBindEmailCode } = await import('./useEmailCode');

    expect(typeof useEmailCode).toBe('function');
    expect(typeof sendAuthEmailCode).toBe('function');
    expect(typeof sendProfileBindEmailCode).toBe('function');

    const api = useEmailCode();
    expect(api.sendAuthEmailCode).toBe(sendAuthEmailCode);
    expect(api.sendProfileBindEmailCode).toBe(sendProfileBindEmailCode);
  });

  it('delegates auth email send to auth api', async () => {
    const { sendEmailCode } = await import('@/api/auth/auth');
    const { sendAuthEmailCode } = await import('./useEmailCode');

    vi.mocked(sendEmailCode).mockResolvedValue({ code: 200, message: 'ok', data: undefined, timestamp: Date.now() });

    await sendAuthEmailCode('user@example.com', 'login');

    expect(sendEmailCode).toHaveBeenCalledWith({
      email: 'user@example.com',
      scene: 'login'
    });
  });
});
