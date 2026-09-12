import { RoleEnum } from '@/constants/auth';

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  avatar: string;
  roles: RoleEnum[];
  permissions?: string[];
  department?: string;
  email?: string;
}

export interface CaptchaVO {
  id: string;
  img: string;
}

export interface LoginParams {
  username: string;
  password: string;
  captcha?: string;
  captchaId?: string;
  role?: RoleEnum;
}

export type EmailScene = 'login' | 'bind' | 'resetpwd' | 'test';

export interface EmailSendCodeRequest {
  email: string;
  scene?: EmailScene;
  captchaId?: string;
  captcha?: string;
}

export interface EmailLoginRequest {
  email: string;
  code: string;
}

export interface LoginResult {
  token: string;
  userInfo: UserInfo;
}

export interface RegisterParams {
  username: string;
  realName: string;
  password: string;
  role: 'TEACHER' | 'STUDENT';
  email?: string;
  phone?: string;
  department?: string;
}
