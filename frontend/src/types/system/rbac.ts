export interface RoleVO {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  createTime?: string;
  permissions: string[];
}

export interface PermissionVO {
  id: number;
  permissionCode: string;
  permissionName: string;
  permissionType?: string;
  parentId?: number;
  children?: PermissionVO[];
}

export interface RoleCreateRequest {
  roleCode: string;
  roleName: string;
  description?: string;
  permissionIds?: number[];
}

export interface RoleUpdateRequest {
  roleName?: string;
  description?: string;
  permissionIds?: number[];
}
