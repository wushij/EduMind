import { get, post, put } from '@/core/http/request';
import { UserInfo } from '@/types/auth/auth';
import { PageResult } from '@/types/common/api';

export const getUsers = (params?: Record<string, any>) =>
  get<PageResult<any>>('/system/users', params);

export const getUserDetail = (id: number) => get<any>(`/system/users/${id}`);

export const createUser = (data: Record<string, any>) => post<number>('/system/users', data);

export const updateUser = (id: number, data: Record<string, any>) =>
  put<void>(`/system/users/${id}`, data);

export const updateUserStatus = (id: number, status: string) =>
  put<void>(`/system/users/${id}/status`, { status });

export const getProfile = () => get<UserInfo>('/users/profile');

export const updateProfile = (data: Partial<UserInfo>) => put<UserInfo>('/users/profile', data);

export const sendBindEmailCode = (email: string) =>
  post<void>('/users/send-bind-code', { email, scene: 'bind' });

export const bindEmail = (data: { email: string; code: string }) =>
  post<UserInfo>('/users/bind-email', data);
