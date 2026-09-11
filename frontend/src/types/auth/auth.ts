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
