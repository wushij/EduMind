import { sendEmailCode } from '@/api/auth/auth';
import { sendBindEmailCode } from '@/api/system/user';
import type { EmailScene } from '@/types/auth/auth';

export async function sendAuthEmailCode(email: string, scene: EmailScene = 'login') {
  return sendEmailCode({ email, scene });
}

export async function sendProfileBindEmailCode(email: string) {
  return sendBindEmailCode(email);
}

export function useEmailCode() {
  return {
    sendAuthEmailCode,
    sendProfileBindEmailCode
  };
}
