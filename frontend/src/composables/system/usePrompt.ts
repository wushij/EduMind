import { ref } from 'vue';
import { PromptTemplate, PromptTestRequest, PromptTestResponse } from '@/types/system/prompt';
import {
  getPromptTemplates,
  getPromptById,
  getPromptVersions,
  savePromptTemplate,
  publishPromptTemplate,
  rollbackPromptTemplate,
  testPromptTemplate,
  type PromptVersionItem
} from '@/api/system/prompt';
import { ElMessage } from 'element-plus';

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
      const res = await publishPromptTemplate(id);
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
      const res = await rollbackPromptTemplate(id, targetVersion);
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

  const runTest = async (templateId: number, req: PromptTestRequest) => {
    testing.value = true;
    try {
      testResult.value = await testPromptTemplate(templateId, req);
      ElMessage.success('Prompt 在线测试执行完毕');
    } catch (err: any) {
      ElMessage.error(err.message || '测试执行异常');
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
