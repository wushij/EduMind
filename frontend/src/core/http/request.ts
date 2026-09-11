import { axiosInstance } from './axios';
import { ApiResponse } from '@/types/common/api';

export function get<T>(url: string, params?: any): Promise<ApiResponse<T>> {
  return axiosInstance.get(url, { params });
}

export function post<T>(url: string, data?: any): Promise<ApiResponse<T>> {
  return axiosInstance.post(url, data);
}

export function put<T>(url: string, data?: any): Promise<ApiResponse<T>> {
  return axiosInstance.put(url, data);
}

export function del<T>(url: string, params?: any): Promise<ApiResponse<T>> {
  return axiosInstance.delete(url, { params });
}
