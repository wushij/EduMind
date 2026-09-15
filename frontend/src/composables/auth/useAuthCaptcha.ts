import { createSliderChallenge, verifySliderCaptcha } from '@/api/auth/auth';
import type { SliderVerifyPayload } from '@/types/auth/auth';

export async function loadSliderChallenge(operation = 'LOGIN', username = '') {
  return createSliderChallenge(operation, username);
}

export async function verifySliderChallenge(payload: SliderVerifyPayload) {
  return verifySliderCaptcha(payload);
}

export function useAuthCaptcha() {
  return {
    loadSliderChallenge,
    verifySliderChallenge
  };
}
