import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { usePreferenceStore } from '@/stores/user/preference';
import { resolveChatModels } from '@/services/ai/chat-service';
import type { ModelProviderConfig } from '@/types/system/model';

const FALLBACK_MODEL_DEFAULTS: Omit<ModelProviderConfig, 'id' | 'modelKey' | 'name' | 'provider' | 'contextLength' | 'isDefault'> = {
  endpoint: '',
  apiKeyMasked: '****',
  maxOutputTokens: 4096,
  temperature: 0.7,
  supportsStreaming: true,
  supportsEmbedding: false,
  supportsVision: false,
  enabled: true,
  costPer1kPrompt: 0,
  costPer1kCompletion: 0,
  healthStatus: 'HEALTHY'
};

export const FALLBACK_MODELS: ModelProviderConfig[] = [
  {
    id: 1,
    modelKey: 'deepseek-v3',
    name: 'DeepSeek-V3',
    provider: 'DeepSeek',
    contextLength: 65536,
    isDefault: true,
    ...FALLBACK_MODEL_DEFAULTS
  },
  {
    id: 2,
    modelKey: 'deepseek-r1',
    name: 'DeepSeek-R1 (深度思维链)',
    provider: 'DeepSeek',
    contextLength: 65536,
    isDefault: false,
    ...FALLBACK_MODEL_DEFAULTS
  },
  {
    id: 3,
    modelKey: 'qwen-2.5',
    name: '通义千问 Qwen-2.5-72B',
    provider: 'Qwen',
    contextLength: 131072,
    isDefault: false,
    ...FALLBACK_MODEL_DEFAULTS
  },
  {
    id: 4,
    modelKey: 'glm-4-plus',
    name: '智谱 GLM-4-Plus',
    provider: 'Zhipu',
    contextLength: 131072,
    isDefault: false,
    ...FALLBACK_MODEL_DEFAULTS
  }
];

export function resolveProviderTagType(provider: string) {
  if (!provider) return 'info';
  const p = provider.toLowerCase();
  if (p.includes('deepseek')) return 'primary';
  if (p.includes('qwen') || p.includes('aliyun')) return 'success';
  if (p.includes('zhipu')) return 'warning';
  return 'info';
}

export function usePreferences() {
  const preferenceStore = usePreferenceStore();
  const modelsLoading = ref(false);
  const dynamicModels = ref<ModelProviderConfig[]>([]);

  /** 仅展示网关返回的已启用模型，不使用本地假数据兜底 */
  const availableModels = computed(() => dynamicModels.value);

  const fetchDynamicModels = async (options?: { notify?: boolean }) => {
    const notify = options?.notify ?? false;
    try {
      modelsLoading.value = true;
      const list = await resolveChatModels();
      if (list && list.length > 0) {
        dynamicModels.value = list.filter((m) => m.enabled !== false);
        if (notify) {
          ElMessage.success(`已从智能网关同步 ${dynamicModels.value.length} 个可用模型`);
        }
      } else if (notify) {
        ElMessage.warning('当前暂无可用模型，请稍后重试或联系管理员');
      }
    } catch (e: unknown) {
      console.warn('获取动态模型列表失败:', e);
      if (notify) {
        ElMessage.error('模型列表同步失败，请检查网络或稍后重试');
      }
    } finally {
      modelsLoading.value = false;
    }
  };

  const handleThemeChange = (val: unknown) => {
    preferenceStore.applyTheme(val as 'LIGHT' | 'DARK' | 'AUTO');
  };

  const handleReset = async () => {
    try {
      await ElMessageBox.confirm(
        '确定要恢复系统默认的个人偏好设置吗？所有显示模式、默认模型与渲染开关将还原为出厂状态。',
        '恢复默认偏好确认',
        {
          confirmButtonText: '恢复默认',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await preferenceStore.resetToDefaults();
    } catch {
      // 用户取消
    }
  };

  const handleSave = async () => {
    await preferenceStore.savePreferences();
  };

  onMounted(async () => {
    await preferenceStore.loadPreferences();
    await fetchDynamicModels();
  });

  return {
    preferenceStore,
    modelsLoading,
    availableModels,
    resolveProviderTagType,
    fetchDynamicModels,
    handleThemeChange,
    handleReset,
    handleSave
  };
}
