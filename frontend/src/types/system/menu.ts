/**
 * EduMind 智教云 - 系统菜单管理领域类型定义
 * 严格遵循 AGENTS.md 规范与 goblog-web 交互结构
 */

/**
 * 菜单类型枚举
 * 1: 目录 (Directory, 顶级/二级业务模块)
 * 2: 菜单 (Menu, 页面真实路由与组件)
 * 3: 按钮 (Button, 细粒度操作权限)
 */
export type MenuType = 1 | 2 | 3;

/**
 * 菜单状态枚举
 * 1: 正常启用
 * 0: 停用
 */
export type MenuStatus = 1 | 0;

/**
 * 菜单完整实体定义
 */
export interface SysMenu {
  id: number;
  parentId: number;
  name: string;
  type: MenuType;
  path?: string;
  component?: string;
  icon?: string;
  permission?: string;
  sort: number;
  status: MenuStatus;
  visible?: boolean;
  keepAlive?: boolean;
  createTime?: string;
  updateTime?: string;
  children?: SysMenu[];
}

/**
 * 菜单树形下拉项
 */
export interface MenuTreeOption {
  id: number;
  name: string;
  children?: MenuTreeOption[];
  disabled?: boolean;
}

/**
 * 新增菜单请求入参
 */
export interface MenuCreateRequest {
  parentId: number;
  name: string;
  type: MenuType;
  path?: string;
  component?: string;
  icon?: string;
  permission?: string;
  sort: number;
  status: MenuStatus;
  visible?: boolean;
  keepAlive?: boolean;
}

/**
 * 更新菜单请求入参
 */
export interface MenuUpdateRequest extends Partial<MenuCreateRequest> {
  id?: number;
}

/**
 * 菜单检索过滤入参
 */
export interface MenuQueryParams {
  keyword?: string;
  type?: MenuType;
  status?: MenuStatus;
}
