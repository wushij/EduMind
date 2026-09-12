import { axiosInstance } from './axios';
import type { HttpRequestConfig } from './types';
import { ApiResponse } from '@/types/common/api';

export function get<T>(url: string, params?: any, config?: HttpRequestConfig): Promise<ApiResponse<T>> {
  return axiosInstance.get(url, { params, ...config });
}

export function post<T>(url: string, data?: any, config?: HttpRequestConfig): Promise<ApiResponse<T>> {
  return axiosInstance.post(url, data, config);
}

export function put<T>(url: string, data?: any, config?: HttpRequestConfig): Promise<ApiResponse<T>> {
  return axiosInstance.put(url, data, config);
}

export function del<T>(url: string, params?: any, config?: HttpRequestConfig): Promise<ApiResponse<T>> {
  return axiosInstance.delete(url, { params, ...config });
}
