import { computed, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createSystemTool,
  deleteSystemTool,
  getSystemTool,
  getSystemTools,
  getToolStats,
  offlineTool,
  publishTool,
  updateSystemTool,
  updateToolFlags
} from '@/api/system/tool';
import type {
  AIToolAdminVO,
  AIToolQuery,
  AIToolSaveRequest,
  AIToolStatsVO,
  AIToolUpdateRequest
} from '@/types/system/tool';
import { mapTool } from '@/utils/ai/map-tool';
import type { AITool } from '@/types/ai/tool';

export function useAIToolManage() {
  const loading = ref(false);
  const saving = ref(false);
  const toolList = ref<AIToolAdminVO[]>([]);
  const stats = ref<AIToolStatsVO | null>(null);
  const currentTool = ref<AIToolAdminVO | null>(null);

  const previewTool = computed<AITool | null>(() => {
    if (!currentTool.value) return null;
    return mapTool(currentTool.value);
  });

  async function fetchTools(query?: AIToolQuery) {
    loading.value = true;
    try {
      toolList.value = await getSystemTools(query);
    } catch (err: any) {
      toolList.value = [];
      ElMessage.error(err.message || '获取工具列表失败');
    } finally {
      loading.value = false;
    }
  }

  async function fetchStats() {
    try {
      stats.value = await getToolStats();
    } catch {
      stats.value = null;
    }
  }

  async function loadTool(id: string) {
    loading.value = true;
    try {
      currentTool.value = await getSystemTool(id);
    } catch (err: any) {
      currentTool.value = null;
      ElMessage.error(err.message || '获取工具详情失败');
    } finally {
      loading.value = false;
    }
  }

  async function handleCreate(dto: AIToolSaveRequest) {
    saving.value = true;
    try {
      const id = await createSystemTool(dto);
      ElMessage.success('工具已创建');
      return id;
    } catch (err: any) {
      ElMessage.error(err.message || '创建失败');
      return null;
    } finally {
      saving.value = false;
    }
  }

  async function handleUpdate(id: string, dto: AIToolUpdateRequest) {
    saving.value = true;
    try {
      await updateSystemTool(id, dto);
      ElMessage.success('工具已保存');
      return true;
    } catch (err: any) {
      ElMessage.error(err.message || '保存失败');
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function handlePublish(id: string) {
    try {
      await publishTool(id);
      ElMessage.success('工具已上架');
      return true;
    } catch (err: any) {
      ElMessage.error(err.message || '上架失败');
      return false;
    }
  }

  async function handleOffline(id: string) {
    try {
      await ElMessageBox.confirm('下架后用户将无法在工具广场看到该工具，确认继续？', '下架确认', {
        type: 'warning'
      });
      await offlineTool(id);
      ElMessage.success('工具已下架');
      return true;
    } catch (err: any) {
      if (err !== 'cancel') {
        ElMessage.error(err.message || '下架失败');
      }
      return false;
    }
  }

  async function handleToggleFlag(id: string, field: 'isRecommended' | 'isHot', value: boolean) {
    try {
      await updateToolFlags(id, { [field]: value });
      ElMessage.success('标记已更新');
      return true;
    } catch (err: any) {
      ElMessage.error(err.message || '更新标记失败');
      return false;
    }
  }

  async function handleDelete(id: string) {
    try {
      await ElMessageBox.confirm('删除后不可恢复，确认删除该工具？', '删除确认', {
        type: 'warning',
        confirmButtonText: '确认删除',
        confirmButtonClass: 'el-button--danger'
      });
      await deleteSystemTool(id);
      ElMessage.success('工具已删除');
      return true;
    } catch (err: any) {
      if (err !== 'cancel') {
        ElMessage.error(err.message || '删除失败');
      }
      return false;
    }
  }

  function setPreviewFromForm(form: Partial<AIToolAdminVO>) {
    currentTool.value = {
      id: form.id || 'preview_tool',
      name: form.name || '工具名称',
      description: form.description || '',
      detailedIntro: form.detailedIntro || form.description || '',
      category: form.category || 'GENERAL',
      icon: form.icon || 'MagicStick',
      modelId: form.modelId,
      route: form.route || '/',
      executionMode: form.executionMode || 'ROUTE',
      tags: form.tags || '',
      isRecommended: !!form.isRecommended,
      isHot: !!form.isHot,
      useCount: form.useCount ?? 0,
      sortOrder: form.sortOrder ?? 0,
      status: form.status ?? 0
    };
  }

  return {
    loading,
    saving,
    toolList,
    stats,
    currentTool,
    previewTool,
    fetchTools,
    fetchStats,
    loadTool,
    handleCreate,
    handleUpdate,
    handlePublish,
    handleOffline,
    handleToggleFlag,
    handleDelete,
    setPreviewFromForm
  };
}
