import type { ApiResponse } from '@/types/common/api';
import type {
  SysMenu,
  MenuCreateRequest,
  MenuUpdateRequest,
  MenuQueryParams
} from '@/types/system/menu';
import { USE_MOCK } from '@/config/mock';
import {
  getStoredMenuTree,
  mockCreateMenu,
  mockUpdateMenu,
  mockDeleteMenu,
  mockResetDefaultMenus
} from '@/mock/menu';
import { filterMenuTree } from '@/utils/system/menu-tree-filter';
import * as menuApi from '@/api/system/menu';

export async function getMenuTree(params?: MenuQueryParams): Promise<ApiResponse<SysMenu[]>> {
  if (USE_MOCK) {
    const stored = getStoredMenuTree();
    const data = filterMenuTree(stored, params?.keyword);
    return { code: 200, message: 'success', data, timestamp: Date.now() };
  }

  return menuApi.fetchMenuTree(params);
}

export async function createMenu(data: MenuCreateRequest): Promise<ApiResponse<SysMenu>> {
  if (USE_MOCK) {
    const created = mockCreateMenu(data);
    return { code: 200, message: '创建成功', data: created, timestamp: Date.now() };
  }

  return menuApi.createMenuHttp(data);
}

export async function updateMenu(
  id: number,
  data: MenuUpdateRequest
): Promise<ApiResponse<boolean>> {
  if (USE_MOCK) {
    const ok = mockUpdateMenu(id, data);
    return { code: 200, message: '保存成功', data: ok, timestamp: Date.now() };
  }

  return menuApi.updateMenuHttp(id, data);
}

export async function deleteMenu(id: number): Promise<ApiResponse<boolean>> {
  if (USE_MOCK) {
    const ok = mockDeleteMenu(id);
    return { code: 200, message: '删除成功', data: ok, timestamp: Date.now() };
  }

  return menuApi.deleteMenuHttp(id);
}

export async function resetDefaultMenus(): Promise<ApiResponse<SysMenu[]>> {
  const resetData = mockResetDefaultMenus();
  return { code: 200, message: '已恢复预设系统菜单', data: resetData, timestamp: Date.now() };
}
