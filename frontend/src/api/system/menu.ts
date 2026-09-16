/**
 * EduMind 智教云 - 系统菜单管理 HTTP API
 */

import { get, post, put, del } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type {
  SysMenu,
  MenuCreateRequest,
  MenuUpdateRequest,
  MenuQueryParams
} from '@/types/system/menu';

export function fetchMenuTree(params?: MenuQueryParams): Promise<ApiResponse<SysMenu[]>> {
  return get<SysMenu[]>('/system/menus/tree', params, { silent: true });
}

export function createMenuHttp(data: MenuCreateRequest): Promise<ApiResponse<SysMenu>> {
  return post<SysMenu>('/system/menus', data, { silent: true });
}

export function updateMenuHttp(id: number, data: MenuUpdateRequest): Promise<ApiResponse<boolean>> {
  return put<boolean>(`/system/menus/${id}`, data, { silent: true });
}

export function deleteMenuHttp(id: number): Promise<ApiResponse<boolean>> {
  return del<boolean>(`/system/menus/${id}`, undefined, { silent: true });
}
