import { get, post, put, del } from '@/core/http/request';
import type {
  AIToolAdminVO,
  AIToolFlagsRequest,
  AIToolQuery,
  AIToolSaveRequest,
  AIToolStatsVO,
  AIToolUpdateRequest
} from '@/types/system/tool';

interface ToolListResponse {
  records?: AIToolAdminVO[];
  list?: AIToolAdminVO[];
  total?: number;
}

function unwrapToolList(res: unknown): AIToolAdminVO[] {
  const data = (res as { data?: unknown })?.data ?? res;
  if (Array.isArray(data)) return data as AIToolAdminVO[];
  const page = data as ToolListResponse;
  if (Array.isArray(page?.records)) return page.records;
  if (Array.isArray(page?.list)) return page.list;
  return [];
}

function unwrapData<T>(res: unknown): T {
  return ((res as { data?: T })?.data ?? res) as T;
}

export async function getSystemTools(query?: AIToolQuery): Promise<AIToolAdminVO[]> {
  const res = await get<ToolListResponse>('/system/tools', query);
  return unwrapToolList(res);
}

export async function getToolStats(): Promise<AIToolStatsVO> {
  const res = await get<AIToolStatsVO>('/system/tools/stats');
  return unwrapData<AIToolStatsVO>(res);
}

export async function getSystemTool(id: string): Promise<AIToolAdminVO> {
  const res = await get<AIToolAdminVO>(`/system/tools/${id}`);
  return unwrapData<AIToolAdminVO>(res);
}

export async function createSystemTool(dto: AIToolSaveRequest): Promise<string> {
  const res = await post<{ id: string }>('/system/tools', dto);
  const data = unwrapData<{ id: string }>(res);
  return data.id;
}

export async function updateSystemTool(id: string, dto: AIToolUpdateRequest): Promise<void> {
  await put(`/system/tools/${id}`, dto);
}

export async function publishTool(id: string): Promise<void> {
  await post(`/system/tools/${id}/publish`);
}

export async function offlineTool(id: string): Promise<void> {
  await post(`/system/tools/${id}/offline`);
}

export async function updateToolFlags(id: string, flags: AIToolFlagsRequest): Promise<void> {
  await post(`/system/tools/${id}/flags`, flags);
}

export async function deleteSystemTool(id: string): Promise<void> {
  await del(`/system/tools/${id}`);
}
