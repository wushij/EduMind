import { del, get, post } from '@/core/http/request';
import type { PageResult } from '@/types/common/api';
import type { OperLogVO, OperLogPageQuery, OperLogStatsVO } from '@/types/system/oper-log';

/**
 * 分页查询操作日志
 */
export const pageOperLog = (params?: OperLogPageQuery) =>
  get<PageResult<OperLogVO>>('/system/oper-log/page', params);

/**
 * 获取操作日志全局看板统计
 */
export const getOperLogStats = () =>
  get<OperLogStatsVO>('/system/oper-log/stats');

/**
 * 获取操作日志详情
 */
export const getOperLogDetail = (id: number) =>
  get<OperLogVO>(`/system/oper-log/${id}`);

/**
 * 单条删除操作日志
 */
export const deleteOperLog = (id: number) =>
  del<boolean>(`/system/oper-log/${id}`);

/**
 * 批量删除操作日志
 */
export const batchDeleteOperLog = (ids: number[]) =>
  post<boolean>('/system/oper-log/batch-delete', ids);

/**
 * 清空操作日志
 */
export const cleanOperLog = () =>
  del<boolean>('/system/oper-log/clean');

/**
 * 获取指定用户最近的操作日志（用于用户详情页）
 */
export const getUserOperLogs = (userId: number, limit = 10) =>
  get<OperLogVO[]>(`/system/oper-log/user/${userId}`, { limit });
