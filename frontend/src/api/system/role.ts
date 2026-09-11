import { get, post, put, del } from '@/core/http/request';
import type {
  PermissionVO,
  RoleCreateRequest,
  RoleUpdateRequest,
  RoleVO
} from '@/types/system/rbac';

export const getRoles = () => get<RoleVO[]>('/system/roles');

export const createRole = (data: RoleCreateRequest) => post<number>('/system/roles', data);

export const updateRole = (id: number, data: RoleUpdateRequest) =>
  put<RoleVO>(`/system/roles/${id}`, data);

export const updateRolePermissions = (id: number, permissionIds: number[]) =>
  put<RoleVO>(`/system/roles/${id}/permissions`, { permissionIds });

export const deleteRole = (id: number) => del<void>(`/system/roles/${id}`);

export const getPermissions = () => get<PermissionVO[]>('/system/permissions');
