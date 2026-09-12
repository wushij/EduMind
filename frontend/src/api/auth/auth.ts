import { post, get, put } from '@/core/http/request';
import { LoginParams, LoginResult, UserInfo, CaptchaVO, RegisterParams } from '@/types/auth/auth';

export const getCaptcha = () => get<CaptchaVO>('/auth/captcha');

export const login = (params: LoginParams) => post<LoginResult>('/auth/login', params);

export const emailLogin = (data: { email: string; code: string }) =>
  post<LoginResult>('/auth/email-login', data);

export const sendEmailCode = (data: { email: string; scene?: string; captchaId?: string; captcha?: string }) =>
  post<void>('/auth/send-email-code', data);

export const register = (params: RegisterParams) => post<{ userId: number }>('/auth/register', params);

export const getUserInfo = () => get<UserInfo>('/auth/user-info');

export const logout = () => post<void>('/auth/logout');

export const changePassword = (oldPassword: string, newPassword: string) =>
  put<void>('/auth/password', { oldPassword, newPassword });
