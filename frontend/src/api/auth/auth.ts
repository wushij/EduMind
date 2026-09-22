import { post, get, put } from '@/core/http/request';
import {
  LoginParams,
  LoginResult,
  UserInfo,
  CaptchaVO,
  RegisterParams,
  PasswordResetVerifyRequest,
  PasswordResetVerifyResult,
  PasswordResetRequest,
  CaptchaPolicyResult,
  SliderChallengeResult,
  SliderVerifyPayload,
  SliderVerifyResult
} from '@/types/auth/auth';

export const getCaptcha = () => get<CaptchaVO>('/auth/captcha');

export const getCaptchaPolicy = () => get<CaptchaPolicyResult>('/auth/captcha/policy');

export const createSliderChallenge = (operation = 'LOGIN', username = '') =>
  post<SliderChallengeResult>('/auth/captcha/slider/challenge', {
    operation,
    username: username.trim()
  });

export const verifySliderCaptcha = (payload: SliderVerifyPayload) =>
  post<SliderVerifyResult>('/auth/captcha/slider/verify', payload, { silent: true });

export const login = (params: LoginParams) => post<LoginResult>('/auth/login', params);

/**
 * 演示扫码登录（登录页「模拟扫码授权通过(Demo)」）。
 * 免密，账号由后端 sys.login.config.mockScanAccount 指定；不再像旧实现那样在前端硬编码账号密码。
 */
export const demoScanLogin = () => post<LoginResult>('/auth/demo-scan-login', undefined, { silent: true });

export const emailLogin = (data: { email: string; code: string }) =>
  post<LoginResult>('/auth/email-login', data);

export const sendEmailCode = (data: { email: string; scene?: string; captchaId?: string; captcha?: string }) =>
  post<void>('/auth/send-email-code', data);

export const verifyResetCode = (data: PasswordResetVerifyRequest) =>
  post<PasswordResetVerifyResult>('/auth/verify-reset-code', data);

export const resetPassword = (data: PasswordResetRequest) =>
  post<void>('/auth/reset-password', data);

export const register = (params: RegisterParams) => post<{ userId: number }>('/auth/register', params);

export const getUserInfo = () => get<UserInfo>('/auth/user-info');

export const logout = () => post<void>('/auth/logout');

export const changePassword = (oldPassword: string, newPassword: string) =>
  put<void>('/auth/password', { oldPassword, newPassword });

