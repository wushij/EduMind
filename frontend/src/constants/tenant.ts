import { storage } from '@/core/storage/local';

/**
 * 多租户前端状态键与统一读写入口
 *
 * 背景：租户 ID 会通过请求头 X-Tenant-Id 参与后端隔离判定，
 * 一旦在「切换身份 / 登出 / 401 失效」时未同步清理，就会把上一个会话的租户
 * 带到新会话中，造成跨账号、跨租户的串号问题。
 * 因此所有读写必须统一走本文件，禁止在业务代码里散落字符串键。
 */
export const TENANT_ID_KEY = 'edumind_tenant_id';
export const CAMPUS_ID_KEY = 'edumind_campus_id';

/** 读取当前租户 ID（无效返回 null） */
export function getStoredTenantId(): number | null {
  const raw = storage.get(TENANT_ID_KEY);
  if (raw === undefined || raw === null || raw === '') return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

/** 写入当前租户 ID */
export function setStoredTenantId(tenantId?: number | null): void {
  if (tenantId === undefined || tenantId === null || !Number.isFinite(Number(tenantId)) || Number(tenantId) <= 0) {
    return;
  }
  storage.set(TENANT_ID_KEY, Number(tenantId));
}

/** 读取当前校区 ID */
export function getStoredCampusId(): number | null {
  const raw = storage.get(CAMPUS_ID_KEY);
  if (raw === undefined || raw === null || raw === '') return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

/** 写入当前校区 ID */
export function setStoredCampusId(campusId?: number | null): void {
  if (campusId === undefined || campusId === null || !Number.isFinite(Number(campusId)) || Number(campusId) <= 0) {
    return;
  }
  storage.set(CAMPUS_ID_KEY, Number(campusId));
}

/**
 * 清理全部租户/校区上下文。
 * 必须在「登出」「登录态失效(401)」「更换会话(身份切换/扫码登录)」时调用，
 * 否则残留的租户头会让新会话落入错误租户。
 */
export function clearTenantContext(): void {
  storage.remove(TENANT_ID_KEY);
  storage.remove(CAMPUS_ID_KEY);
}
