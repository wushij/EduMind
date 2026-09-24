import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { USE_MOCK } from '@/config/mock';
import { mockPromptTemplates } from '@/mock/prompt';
import {
  listPromptTemplates,
  getPromptTemplate,
  createPromptTemplate,
  updatePromptTemplate,
  publishPromptTemplate,
  listPromptVersions,
  rollbackPromptTemplate,
  testPromptTemplate as testPromptTemplateRaw
} from '@/api/system/prompt';
import {
  buildPromptSavePayload,
  filterPromptTemplatesByCategory,
  mapPromptTemplate
} from '@/utils/system/map-prompt';
import {
  PromptTemplate,
  PromptTestRequest,
  PromptTestResponse,
  PromptVersionItem
} from '@/types/system/prompt';

export type { PromptVersionItem };

export async function getPromptTemplates(category?: string): Promise<PromptTemplate[]> {
  try {
    const res = await listPromptTemplates(category);
    if (res?.data && Array.isArray(res.data) && res.data.length > 0) {
      return res.data.map((item) => mapPromptTemplate(item));
    }
    if (USE_MOCK) {
      return filterPromptTemplatesByCategory(mockPromptTemplates, category);
    }
    return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback mockPromptTemplates', err);
    return filterPromptTemplatesByCategory(mockPromptTemplates, category);
  }
}

export async function getPromptById(id: number): Promise<PromptTemplate | null> {
  try {
    const res = await getPromptTemplate(id);
    if (res?.data) {
      return mapPromptTemplate(res.data);
    }
    if (!USE_MOCK) return null;
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback getPromptById mock', err);
  }
  if (!USE_MOCK) return null;
  return mockPromptTemplates.find((p) => p.id === Number(id)) || null;
}

export async function savePromptTemplate(
  template: Partial<PromptTemplate>
): Promise<{ success: boolean; id: number }> {
  const payload = buildPromptSavePayload(template);
  try {
    if (template.id) {
      await updatePromptTemplate(template.id, payload);
      return { success: true, id: template.id };
    }
    const res = await createPromptTemplate(payload);
    return { success: true, id: res?.data?.id || Date.now() };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback save mock', err);
  }
  return { success: true, id: template.id || Date.now() };
}

export async function publishPromptTemplateResolved(
  id: number
): Promise<{ success: boolean; message: string }> {
  try {
    await publishPromptTemplate(id);
    return { success: true, message: '提示词模板已发布' };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback publish mock', err);
  }
  return { success: true, message: '提示词模板已成功发布' };
}

export async function getPromptVersions(id: number): Promise<PromptVersionItem[]> {
  try {
    const res = await listPromptVersions(id);
    if (res?.data && Array.isArray(res.data)) {
      return res.data;
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback versions mock', err);
  }
  return [];
}

export async function rollbackPromptTemplateResolved(
  id: number,
  targetVersion: number
): Promise<{ success: boolean; message: string }> {
  try {
    await rollbackPromptTemplate(id, targetVersion);
    return { success: true, message: `已回滚至 v${targetVersion}` };
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback rollback mock', err);
  }
  return { success: true, message: `已回滚至 v${targetVersion}` };
}

function buildMockTestResponse(req: PromptTestRequest): PromptTestResponse {
  let rendered = req.userPromptTemplate;
  Object.keys(req.variables || {}).forEach((k) => {
    rendered = rendered.replaceAll(`{{${k}}}`, req.variables[k]);
  });
  return {
    renderedUserPrompt: rendered,
    output: '（Mock）基于模板渲染的测试输出。',
    promptTokens: Math.round(rendered.length / 4),
    completionTokens: 64,
    totalTokens: Math.round(rendered.length / 4) + 64,
    durationMs: 200
  };
}

export async function testPromptTemplate(
  templateId: number,
  req: PromptTestRequest
): Promise<PromptTestResponse> {
  const startTime = Date.now();
  try {
    const res = await testPromptTemplateRaw(templateId, req);
    const durationMs = Date.now() - startTime;
    const raw: any = res;
    const outputText: string = (raw?.output || raw?.data?.output || (typeof raw?.data === 'string' ? raw.data : '') || '').trim();
    if (outputText) {
      let rendered = req.userPromptTemplate;
      Object.entries(req.variables || {}).forEach(([k, v]) => {
        rendered = rendered.replaceAll(`{{${k}}}`, v);
      });
      return {
        renderedUserPrompt: rendered,
        output: outputText,
        promptTokens: Math.round(((req.systemPrompt || '').length + rendered.length) / 4),
        completionTokens: Math.round(outputText.length / 4),
        totalTokens: Math.round(((req.systemPrompt || '').length + rendered.length + outputText.length) / 4),
        durationMs
      };
    }
    throw new Error('LLM 返回空内容，请检查模型配置、maxTokens 或关闭 thinking 后重试');
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Prompt] Fallback test mock', err);
  }
  return buildMockTestResponse(req);
}

export function usePrompt() {
  const loading = ref(false);
  const testing = ref(false);
  const publishing = ref(false);
  const rollingBack = ref(false);
  const promptList = ref<PromptTemplate[]>([]);
  const versionHistory = ref<PromptVersionItem[]>([]);
  const currentPrompt = ref<PromptTemplate | null>(null);
  const testResult = ref<PromptTestResponse | null>(null);

  const fetchPrompts = async (category?: string) => {
    loading.value = true;
    try {
      promptList.value = await getPromptTemplates(category);
    } catch (err: any) {
      promptList.value = [];
      ElMessage.error(err.message || '获取提示词模板失败');
    } finally {
      loading.value = false;
    }
  };

  const loadPrompt = async (id: number) => {
    loading.value = true;
    try {
      const res = await getPromptById(id);
      if (!res) {
        ElMessage.warning('提示词不存在');
        currentPrompt.value = null;
      } else {
        currentPrompt.value = res;
      }
    } catch (err: any) {
      ElMessage.error(err.message || '获取提示词详情失败');
      currentPrompt.value = null;
    } finally {
      loading.value = false;
    }
  };

  const handleSave = async (data: Partial<PromptTemplate>) => {
    try {
      const res = await savePromptTemplate(data);
      ElMessage.success('提示词模板已保存');
      return res.id;
    } catch (err: any) {
      ElMessage.error(err.message || '保存失败');
      return null;
    }
  };

  const handlePublish = async (id: number) => {
    publishing.value = true;
    try {
      const res = await publishPromptTemplateResolved(id);
      ElMessage.success(res.message);
      if (currentPrompt.value) currentPrompt.value.status = 'PUBLISHED';
    } catch (err: any) {
      ElMessage.error(err.message || '发布失败');
    } finally {
      publishing.value = false;
    }
  };

  const loadVersions = async (id: number) => {
    try {
      versionHistory.value = await getPromptVersions(id);
    } catch (err: any) {
      ElMessage.error(err.message || '获取版本历史失败');
    }
  };

  const handleRollback = async (id: number, targetVersion: number) => {
    rollingBack.value = true;
    try {
      const res = await rollbackPromptTemplateResolved(id, targetVersion);
      ElMessage.success(res.message);
      await loadPrompt(id);
      await loadVersions(id);
      return true;
    } catch (err: any) {
      ElMessage.error(err.message || '回滚失败');
      return false;
    } finally {
      rollingBack.value = false;
    }
  };

  /** @return 是否执行成功，调用方据此决定是否展示输出（失败时不得伪造结果） */
  const runTest = async (templateId: number, req: PromptTestRequest): Promise<boolean> => {
    testing.value = true;
    try {
      testResult.value = await testPromptTemplate(templateId, req);
      ElMessage.success('Prompt 在线测试执行完毕');
      return true;
    } catch {
      // 失败提示已由 axios 拦截器统一弹出，此处再弹一次会出现重复 toast；
      // 同时清空上一次结果，避免调用方把旧输出当成本次结果。
      testResult.value = null;
      return false;
    } finally {
      testing.value = false;
    }
  };

  return {
    loading,
    testing,
    publishing,
    rollingBack,
    promptList,
    versionHistory,
    currentPrompt,
    testResult,
    fetchPrompts,
    loadPrompt,
    loadVersions,
    handleSave,
    handlePublish,
    handleRollback,
    runTest
  };
}
