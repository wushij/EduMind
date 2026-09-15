import type { PromptTemplate } from '@/types/system/prompt';

export function parseVariables(raw: unknown): PromptTemplate['variables'] {
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

export function mapPromptTemplate(raw: Record<string, unknown>): PromptTemplate {
  return {
    id: raw.id as number,
    code: (raw.code as string) || '',
    name: (raw.name as string) || '',
    category: ((raw.category as string) || 'rag') as PromptTemplate['category'],
    description: (raw.description as string) || '',
    systemPrompt: (raw.systemPrompt as string) || '',
    userPromptTemplate: (raw.content as string) || (raw.userPromptTemplate as string) || '',
    variables: parseVariables(raw.variables),
    version: raw.version ? (String(raw.version).startsWith('v') ? String(raw.version) : `v${raw.version}.0`) : 'v1.0',
    status: ((raw.status as string) || 'DRAFT') as PromptTemplate['status'],
    boundModel: (raw.boundModel as string) || 'deepseek-chat',
    temperature: Number(raw.temperature ?? 0.3),
    maxTokens: Number(raw.maxTokens ?? 2000),
    callCount: Number(raw.callCount ?? 0),
    createdAt: (raw.createTime as string) || (raw.createdAt as string) || '',
    updatedAt: (raw.updateTime as string) || (raw.updatedAt as string) || ''
  };
}

export function filterPromptTemplatesByCategory(
  templates: PromptTemplate[],
  category?: string
): PromptTemplate[] {
  if (!category || category === 'ALL') {
    return [...templates];
  }
  return templates.filter((p) => p.category === category);
}

export function buildPromptSavePayload(template: Partial<PromptTemplate>): Record<string, unknown> {
  return {
    code: template.code,
    name: template.name,
    category: template.category,
    description: template.description,
    systemPrompt: template.systemPrompt,
    content: template.userPromptTemplate,
    variables: template.variables?.map((v) => v.name).join(','),
    boundModel: template.boundModel,
    temperature: template.temperature,
    maxTokens: template.maxTokens
  };
}
