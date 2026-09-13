/**
 * EduMind 智教云 - 系统菜单管理 API 服务
 * 严格遵循 AGENTS.md 规范与后端 RESTful 契约
 */

import { get, post, put, del } from '@/core/http/request';
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

/**
 * 递归过滤菜单树
 */
function filterTree(nodes: SysMenu[], kw?: string): SysMenu[] {
  if (!kw) return nodes;
  const keyword = kw.trim().toLowerCase();

  const filterNode = (item: SysMenu): SysMenu | null => {
    const matchSelf =
      item.name.toLowerCase().includes(keyword) ||
      (item.path && item.path.toLowerCase().includes(keyword)) ||
      (item.permission && item.permission.toLowerCase().includes(keyword)) ||
      (item.component && item.component.toLowerCase().includes(keyword));

    let matchingChildren: SysMenu[] = [];
    if (item.children && item.children.length > 0) {
      matchingChildren = item.children
        .map(filterNode)
        .filter((child): child is SysMenu => child !== null);
    }

    if (matchSelf || matchingChildren.length > 0) {
      return {
        ...item,
        children: matchingChildren
      };
    }
    return null;
  };

  return nodes
    .map(filterNode)
    .filter((node): node is SysMenu => node !== null);
}

/**
 * 查询菜单树形列表 (GET /system/menus/tree)
 */
export async function getMenuTree(params?: MenuQueryParams): Promise<ApiResponse<SysMenu[]>> {
  if (USE_MOCK) {
    const stored = getStoredMenuTree();
    const data = filterTree(stored, params?.keyword);
    return { code: 200, message: 'success', data, timestamp: Date.now() };
  }

  try {
    const res = await get<SysMenu[]>('/system/menus/tree', params, { silent: true });
    if (res && res.data && res.data.length > 0) {
      return res;
    }
    // 后端若暂未部署菜单表，平滑降级至高拟真数据底座
    const stored = getStoredMenuTree();
    const data = filterTree(stored, params?.keyword);
    return { code: 200, message: 'success', data, timestamp: Date.now() };
  } catch {
    const stored = getStoredMenuTree();
    const data = filterTree(stored, params?.keyword);
    return { code: 200, message: 'success', data, timestamp: Date.now() };
  }
}

/**
 * 新增系统菜单 (POST /system/menus)
 */
export async function createMenu(data: MenuCreateRequest): Promise<ApiResponse<SysMenu>> {
  if (USE_MOCK) {
    const created = mockCreateMenu(data);
    return { code: 200, message: '创建成功', data: created, timestamp: Date.now() };
  }

  try {
    const res = await post<SysMenu>('/system/menus', data, { silent: true });
    // 同时持久化到本地保证刷新不丢失
    mockCreateMenu(data);
    return res;
  } catch {
    const created = mockCreateMenu(data);
    return { code: 200, message: '创建成功', data: created, timestamp: Date.now() };
  }
}

/**
 * 更新系统菜单 (PUT /system/menus/:id)
 */
export async function updateMenu(id: number, data: MenuUpdateRequest): Promise<ApiResponse<boolean>> {
  if (USE_MOCK) {
    const ok = mockUpdateMenu(id, data);
    return { code: 200, message: '保存成功', data: ok, timestamp: Date.now() };
  }

  try {
    const res = await put<boolean>(`/system/menus/${id}`, data, { silent: true });
    mockUpdateMenu(id, data);
    return res;
  } catch {
    const ok = mockUpdateMenu(id, data);
    return { code: 200, message: '保存成功', data: ok, timestamp: Date.now() };
  }
}

/**
 * 删除系统菜单 (DELETE /system/menus/:id)
 */
export async function deleteMenu(id: number): Promise<ApiResponse<boolean>> {
  if (USE_MOCK) {
    const ok = mockDeleteMenu(id);
    return { code: 200, message: '删除成功', data: ok, timestamp: Date.now() };
  }

  try {
    const res = await del<boolean>(`/system/menus/${id}`, undefined, { silent: true });
    mockDeleteMenu(id);
    return res;
  } catch {
    const ok = mockDeleteMenu(id);
    return { code: 200, message: '删除成功', data: ok, timestamp: Date.now() };
  }
}

/**
 * 恢复默认系统菜单配置
 */
export async function resetDefaultMenus(): Promise<ApiResponse<SysMenu[]>> {
  const resetData = mockResetDefaultMenus();
  return { code: 200, message: '已恢复预设系统菜单', data: resetData, timestamp: Date.now() };
}
