import { del, get, post, put } from '@/core/http/request';
import { axiosInstance } from '@/core/http/axios';
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

export const deleteUser = (id: number) => del<void>(`/system/users/${id}`);

export const getProfile = () => get<UserInfo>('/users/profile');

export const updateProfile = (data: Partial<UserInfo>) => put<UserInfo>('/users/profile', data);

export const sendBindEmailCode = (email: string) =>
  post<void>('/users/send-bind-code', { email, scene: 'bind' });

export const bindEmail = (data: { email: string; code: string }) =>
  post<UserInfo>('/users/bind-email', data);

export const uploadAvatar = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await axiosInstance.post('/users/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};
