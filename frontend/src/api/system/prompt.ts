import { get, post, put } from '@/core/http/request';
import { USE_MOCK } from '@/config/mock';
import { PromptTemplate, PromptTestRequest, PromptTestResponse } from '@/types/system/prompt';

export const mockPromptTemplates: PromptTemplate[] = [
  {
    id: 1,
    code: 'chat_rag',
    name: '课程 AI-RAG 对话',
    category: 'rag',
    description: '课程知识库 RAG 对话默认模板',
    systemPrompt: '',
    userPromptTemplate: '你是课程 AI 助教。请基于以下资料回答。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}',
    variables: [
      { name: 'context', label: '检索上下文', defaultValue: '' },
      { name: 'question', label: '用户问题', defaultValue: '' }
    ],
    version: 'v1',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.3,
    maxTokens: 2048,
    callCount: 0,
    createdAt: '',
    updatedAt: ''
  }
];

function parseVariables(raw: unknown): PromptTemplate['variables'] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw as PromptTemplate['variables'];
  if (typeof raw === 'string') {
    return raw
      .split(',')
      .map((name) => name.trim())
      .filter(Boolean)
      .map((name) => ({ name, label: name }));
  }
  return [];
}

function mapPromptTemplate(raw: Record<string, unknown>): PromptTemplate {
  return {
    id: raw.id as number,
    code: (raw.code as string) || '',
    name: (raw.name as string) || '',
    category: ((raw.category as string) || 'rag') as PromptTemplate['category'],
    description: (raw.description as string) || '',
    systemPrompt: (raw.systemPrompt as string) || '',
    userPromptTemplate: (raw.userPromptTemplate as string) || (raw.content as string) || '',
    variables: parseVariables(raw.variables),
    version: raw.version ? `v${raw.version}` : 'v1',
    status: ((raw.status as string) || 'DRAFT') as PromptTemplate['status'],
    boundModel: (raw.boundModel as string) || 'deepseek-chat',
    temperature: (raw.temperature as number) ?? 0.3,
    maxTokens: (raw.maxTokens as number) ?? 2048,
    callCount: (raw.callCount as number) ?? 0,
    createdAt: (raw.createdAt as string) || '',
    updatedAt: (raw.updateTime as string) || (raw.updatedAt as string) || ''
  };
}

export const getPromptTemplates = async (category?: string): Promise<PromptTemplate[]> => {
  try {
    const res = await get<Record<string, unknown>[]>('/system/prompts', { category });
    if (res?.data && Array.isArray(res.data)) {
      return res.data.map((item) => mapPromptTemplate(item));
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback mockPromptTemplates', err);
  }
  if (!USE_MOCK) return [];
  let result = [...mockPromptTemplates];
  if (category && category !== 'ALL') {
    result = result.filter((p) => p.category === category);
  }
  return result;
};

export const getPromptById = async (id: number): Promise<PromptTemplate | null> => {
  try {
    const res = await get<Record<string, unknown>>(`/system/prompts/${id}`);
    if (res?.data) {
      return mapPromptTemplate(res.data);
    }
    if (!USE_MOCK) return null;
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback getPromptById mock', err);
  }
  return USE_MOCK ? mockPromptTemplates.find((p) => p.id === Number(id)) || null : null;
};

export const savePromptTemplate = async (template: Partial<PromptTemplate>): Promise<{ success: boolean; id: number }> => {
  const payload = {
    code: template.code,
    name: template.name,
    category: template.category,
    content: template.userPromptTemplate,
    variables: template.variables?.map((v) => v.name).join(',')
  };
  try {
    if (template.id) {
      await put(`/system/prompts/${template.id}`, payload);
      return { success: true, id: template.id };
    }
    const res = await post<{ id: number }>('/system/prompts', payload);
    return { success: true, id: res?.data?.id || Date.now() };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback save mock', err);
  }
  return { success: true, id: template.id || Date.now() };
};

export const publishPromptTemplate = async (id: number): Promise<{ success: boolean; message: string }> => {
  try {
    await post(`/system/prompts/${id}/publish`);
    return { success: true, message: '提示词模板已发布' };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback publish mock', err);
  }
  return { success: true, message: '提示词模板已成功发布' };
};

export interface PromptVersionItem {
  id: number;
  templateId: number;
  version: number;
  content: string;
  variables?: string;
  publishedBy?: number;
  createTime?: string;
}

export const getPromptVersions = async (id: number): Promise<PromptVersionItem[]> => {
  try {
    const res = await get<PromptVersionItem[]>(`/system/prompts/${id}/versions`);
    if (res?.data && Array.isArray(res.data)) {
      return res.data;
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback versions mock', err);
  }
  return [];
};

export const rollbackPromptTemplate = async (
  id: number,
  targetVersion: number
): Promise<{ success: boolean; message: string }> => {
  try {
    await post(`/system/prompts/${id}/rollback`, { targetVersion });
    return { success: true, message: `已回滚至 v${targetVersion}` };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback rollback mock', err);
  }
  return { success: true, message: `已回滚至 v${targetVersion}` };
};

export const testPromptTemplate = async (
  templateId: number,
  req: PromptTestRequest
): Promise<PromptTestResponse> => {
  try {
    const res = await post<{ output: string }>(`/system/prompts/${templateId}/test`, {
      variables: req.variables
    });
    if (res?.data?.output) {
      let rendered = req.userPromptTemplate;
      Object.entries(req.variables || {}).forEach(([k, v]) => {
        rendered = rendered.replaceAll(`{{${k}}}`, v);
      });
      return {
        renderedUserPrompt: rendered,
        output: res.data.output,
        promptTokens: Math.round(rendered.length / 4),
        completionTokens: Math.round(res.data.output.length / 4),
        totalTokens: Math.round((rendered.length + res.data.output.length) / 4),
        durationMs: 0
      };
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt API] Fallback test mock', err);
  }

  let rendered = req.userPromptTemplate;
  Object.keys(req.variables || {}).forEach((k) => {
    rendered = rendered.replaceAll(`{{${k}}}`, req.variables[k]);
  });
  return {
    renderedUserPrompt: rendered,
    output: `（Mock）基于模板渲染的测试输出。`,
    promptTokens: Math.round(rendered.length / 4),
    completionTokens: 64,
    totalTokens: Math.round(rendered.length / 4) + 64,
    durationMs: 200
  };
};
