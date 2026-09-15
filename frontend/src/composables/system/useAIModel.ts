import { ref, reactive, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { USE_MOCK } from '@/config/mock';
import { mockModels, mockProviderPresets } from '@/mock/model';
import {
  listModels,
  createModel as createModelRaw,
  updateModel,
  deleteModel,
  setDefaultModel,
  testModel as testModelRaw,
  testModelDraft as testModelDraftRaw,
  getProviderPresets as getProviderPresetsRaw
} from '@/api/system/model';
import {
  isValidProviderPresets,
  mapTestModelResponse,
  mapVoToItem,
  unwrapModelList
} from '@/utils/system/map-model';
import type {
  AIModelConfigItem,
  AIProviderPresetsResponse,
  ModelProviderConfig,
  ReasoningEffort
} from '@/types/system/model';
import { toLegacyModelConfig } from '@/types/system/model';

export async function resolveModels(): Promise<AIModelConfigItem[]> {
  try {
    const res = await listModels();
    const raw = unwrapModelList<Record<string, unknown>>(res);
    if (raw.length > 0) {
      return raw.map(mapVoToItem);
    }
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Model] fallback mock', err);
  }
  return USE_MOCK ? mockModels : [];
}

export async function resolveProviderPresets(): Promise<AIProviderPresetsResponse> {
  try {
    const res = await getProviderPresetsRaw();
    const data = (res as { data?: AIProviderPresetsResponse })?.data ?? res;
    if (isValidProviderPresets(data)) {
      return data;
    }
    if (USE_MOCK) return mockProviderPresets;
    throw new Error('模型 Provider 预设响应格式无效');
  } catch (err) {
    if (!USE_MOCK) throw err;
    return mockProviderPresets;
  }
}

export async function resolveModelConfigs(): Promise<ModelProviderConfig[]> {
  const items = await resolveModels();
  return items.map(toLegacyModelConfig);
}

/** @deprecated Use resolveModels */
export const fetchModels = resolveModels;

export function filterModelsByType(
  models: AIModelConfigItem[],
  configType: 'chat' | 'embedding'
): AIModelConfigItem[] {
  if (configType === 'chat') {
    return models.filter((m) => m.configType === 'chat' || !m.configType);
  }
  return models.filter((m) => m.configType === 'embedding');
}

export function reasoningLabel(effort?: ReasoningEffort): string {
  const map: Record<ReasoningEffort, string> = {
    low: 'LOW · 快速',
    medium: 'MEDIUM · 均衡',
    high: 'HIGH · 深度',
    max: 'MAX · 最强'
  };
  return map[effort || 'low'] || 'LOW · 快速';
}

export function getProviderDisplayName(p?: string): string {
  if (!p) return '通用模型';
  const map: Record<string, string> = {
    deepseek: 'DeepSeek 深度求索',
    openai: 'OpenAI',
    anthropic: 'Anthropic Claude',
    claude: 'Anthropic Claude',
    qwen: '通义千问 (DashScope)',
    kimi: '月之暗面 (Moonshot)',
    minimax: 'MiniMax 名之梦',
    zhipu: '智谱 AI (GLM)',
    mock: '本地测试 Mock'
  };
  return map[p.toLowerCase()] || p;
}

export function suggestDimension(modelName: string): number {
  const lower = modelName.toLowerCase();
  if (lower.includes('bge') || lower.includes('1024')) return 1024;
  if (lower.includes('3072')) return 3072;
  if (lower.includes('768')) return 768;
  return 1536;
}

export async function fetchEnabledChatModels(): Promise<AIModelConfigItem[]> {
  const models = await fetchModels();
  return models.filter((m) => m.configType === 'chat' && m.status === 'enabled');
}

export function useAIModel() {
  const loading = ref(false);
  const refreshing = ref(false);
  const saving = ref(false);
  const activeTab = ref<'chat' | 'embedding'>('chat');
  const models = ref<AIModelConfigItem[]>([]);
  const presets = ref<AIProviderPresetsResponse | null>(null);

  const dialogVisible = ref(false);
  const editing = ref<AIModelConfigItem | null>(null);

  const testingModels = ref<Record<string, boolean>>({});
  const testResults = ref<Record<string, { success: boolean; latency?: number; error?: string }>>({});

  const dialogTesting = ref(false);
  const dialogTestResult = ref<{ success: boolean; latency?: number; error?: string } | null>(null);

  const form = reactive({
    name: '',
    provider: '',
    configType: 'chat' as 'chat' | 'embedding',
    modelName: '',
    baseUrl: '',
    apiKey: '',
    temperature: 0.7,
    reasoningEffort: 'low' as ReasoningEffort,
    dimension: 1536,
    isDefault: false,
    status: 'enabled' as 'enabled' | 'disabled'
  });

  const dimensionWarnVisible = ref(false);
  let dimensionWarnResolve: ((ok: boolean) => void) | null = null;

  const chatModels = computed(() => filterModelsByType(models.value, 'chat'));
  const embeddingModels = computed(() => filterModelsByType(models.value, 'embedding'));
  const displayedModels = computed(() =>
    activeTab.value === 'chat' ? chatModels.value : embeddingModels.value
  );

  const currentPresetsGroup = computed(() => {
    if (!presets.value) return {};
    const targetType = form.configType || activeTab.value;
    return targetType === 'chat' ? presets.value.chat : presets.value.embedding;
  });

  const currentPreset = computed(() => {
    if (!form.provider || !presets.value) return null;
    const group = form.configType === 'chat' ? presets.value.chat : presets.value.embedding;
    return group[form.provider] || null;
  });

  const currentModelOptions = computed(() => currentPreset.value?.modelOptions || []);

  function handleConfigTypeChange(val: 'chat' | 'embedding') {
    resetDialogTest();
    if (val === 'embedding') {
      form.provider = 'qwen';
      handleProviderChange('qwen');
    } else {
      form.provider = 'deepseek';
      handleProviderChange('deepseek');
    }
  }

  function handleProviderChange(providerKey: string) {
    const p = currentPresetsGroup.value[providerKey];
    if (p) {
      form.modelName = p.modelName || '';
      form.baseUrl = p.baseUrl || '';
      if (form.configType === 'embedding') {
        form.dimension = suggestDimension(p.modelName || '');
      }
    }
  }

  async function confirmDimensionChangeIfNeeded(
    newDimension: number,
    willBeDefault: boolean
  ): Promise<boolean> {
    if (form.configType !== 'embedding' || !willBeDefault || newDimension <= 0) {
      return true;
    }
    const currentDefault = embeddingModels.value.find((m) => m.isDefault);
    const prevDim = currentDefault?.dimension || 0;
    if (prevDim > 0 && prevDim === newDimension) return true;
    if (editing.value?.isDefault && editing.value.dimension === newDimension) return true;
    return showDimensionWarnDialog();
  }

  function showDimensionWarnDialog(): Promise<boolean> {
    return new Promise((resolve) => {
      dimensionWarnResolve = resolve;
      dimensionWarnVisible.value = true;
    });
  }

  function confirmDimensionWarn() {
    const resolve = dimensionWarnResolve;
    dimensionWarnResolve = null;
    dimensionWarnVisible.value = false;
    resolve?.(true);
  }

  watch(dimensionWarnVisible, (visible) => {
    if (!visible && dimensionWarnResolve) {
      dimensionWarnResolve(false);
      dimensionWarnResolve = null;
    }
  });

  function resetDialogTest() {
    dialogTesting.value = false;
    dialogTestResult.value = null;
  }

  function openCreate() {
    editing.value = null;
    resetDialogTest();
    form.name = '';
    form.provider = activeTab.value === 'chat' ? 'deepseek' : 'openai';
    form.configType = activeTab.value;
    form.modelName = '';
    form.baseUrl = '';
    form.apiKey = '';
    form.temperature = 0.7;
    form.reasoningEffort = 'low';
    form.dimension = activeTab.value === 'embedding' ? 1536 : 0;
    form.isDefault = displayedModels.value.length === 0;
    form.status = 'enabled';
    handleProviderChange(form.provider);
    dialogVisible.value = true;
  }

  function openEdit(item: AIModelConfigItem) {
    editing.value = item;
    resetDialogTest();
    form.name = item.name;
    form.provider = item.provider;
    form.configType = item.configType || 'chat';
    form.modelName = item.modelName;
    form.baseUrl = item.baseUrl || '';
    form.apiKey = '';
    form.temperature = item.temperature ?? 0.7;
    form.reasoningEffort = item.reasoningEffort || 'low';
    form.dimension = item.dimension || suggestDimension(item.modelName || '');
    form.isDefault = !!item.isDefault;
    form.status = item.status || 'enabled';
    dialogVisible.value = true;
  }

  async function loadPresets() {
    try {
      presets.value = await resolveProviderPresets();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '获取 Provider 预设配置失败';
      ElMessage.error(msg);
    }
  }

  async function loadModels(options?: { silent?: boolean }) {
    if (!options?.silent) {
      loading.value = true;
    }
    try {
      models.value = await resolveModels();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '获取模型列表失败';
      ElMessage.error(msg);
    } finally {
      loading.value = false;
    }
  }

  async function handleRefresh() {
    refreshing.value = true;
    try {
      models.value = await resolveModels();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '获取模型列表失败';
      ElMessage.error(msg);
    } finally {
      refreshing.value = false;
    }
  }

  async function syncModelsSilently() {
    try {
      models.value = await resolveModels();
    } catch {
      // 静默同步失败时保留本地列表
    }
  }

  function switchTab(tab: 'chat' | 'embedding') {
    activeTab.value = tab;
  }

  async function handleSave() {
    if (!form.name.trim()) {
      ElMessage.warning('请输入模型配置标识');
      return;
    }
    if (!form.provider) {
      ElMessage.warning('请选择服务商');
      return;
    }
    if (!form.modelName) {
      ElMessage.warning('请输入或选择模型型号');
      return;
    }
    if (form.configType === 'embedding' && (!form.dimension || form.dimension < 128)) {
      ElMessage.warning('请设置有效的向量维度 (128–4096)');
      return;
    }

    const proceed = await confirmDimensionChangeIfNeeded(form.dimension, form.isDefault);
    if (!proceed) return;

    saving.value = true;
    try {
      const payload = {
        name: form.name.trim(),
        provider: form.provider,
        configType: form.configType,
        modelName: form.modelName,
        baseUrl: form.baseUrl,
        apiKey: form.apiKey || undefined,
        temperature: form.temperature,
        isDefault: form.isDefault,
        status: form.status,
        ...(form.configType === 'chat' ? { reasoningEffort: form.reasoningEffort } : {}),
        ...(form.configType === 'embedding' ? { dimension: form.dimension } : {})
      };

      if (editing.value) {
        await updateModel(form.name, payload);
        ElMessage.success('模型更新成功并已热重载');
      } else {
        await createModelRaw(payload);
        ElMessage.success('模型接入成功并已生效');
      }

      dialogVisible.value = false;
      await syncModelsSilently();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '保存失败，请检查网络后重试';
      ElMessage.error(msg);
    } finally {
      saving.value = false;
    }
  }

  async function handleSetDefault(item: AIModelConfigItem) {
    if (item.configType === 'embedding' && item.dimension) {
      const currentDefault = embeddingModels.value.find(
        (m) => m.isDefault && m.name !== item.name
      );
      if (currentDefault?.dimension && currentDefault.dimension !== item.dimension) {
        const ok = await showDimensionWarnDialog();
        if (!ok) return;
      }
    }
    try {
      await setDefaultModel(item.name);
      ElMessage.success(`已成功将「${item.name}」设为默认模型`);
      await syncModelsSilently();
    } catch (err: unknown) {
      console.error('设为默认模型失败:', err);
    }
  }

  async function handleDelete(item: AIModelConfigItem) {
    try {
      await ElMessageBox.confirm(
        `确定要删除模型配置「${item.name}」吗？如果正在被业务场景绑定，调用时将回退到其他可用模型。`,
        '删除确认',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
      );
      await deleteModel(item.name);
      models.value = models.value.filter((m) => m.name !== item.name);
      delete testResults.value[item.name];
      delete testingModels.value[item.name];
      ElMessage.success('模型已删除');
      await syncModelsSilently();
    } catch (err) {
      if (err !== 'cancel') {
        console.error('删除模型失败:', err);
      }
    }
  }

  async function handleDialogTest() {
    if (!form.provider) {
      ElMessage.warning('请选择服务商');
      return;
    }
    if (!form.modelName) {
      ElMessage.warning('请输入或选择模型型号');
      return;
    }
    if (!form.apiKey && !editing.value?.hasApiKey) {
      ElMessage.warning('请先填写 API Key');
      return;
    }

    dialogTesting.value = true;
    dialogTestResult.value = null;
    try {
      const res = mapTestModelResponse(await testModelDraftRaw({
        name: form.name.trim() || undefined,
        provider: form.provider,
        configType: form.configType,
        modelName: form.modelName,
        baseUrl: form.baseUrl,
        apiKey: form.apiKey || undefined
      }));
      const latency = res.latencyMs ?? res.latency ?? 0;
      dialogTestResult.value = { success: true, latency };
      ElMessage.success(`连接成功 (${latency}ms)`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '网络超时或端点异常';
      dialogTestResult.value = { success: false, error: msg };
      ElMessage.error(`连接失败: ${msg}`);
    } finally {
      dialogTesting.value = false;
    }
  }

  async function handleTest(item: AIModelConfigItem) {
    testingModels.value[item.name] = true;
    try {
      const res = mapTestModelResponse(await testModelRaw(item.name));
      testResults.value[item.name] = {
        success: !!res.success,
        latency: res.latencyMs ?? res.latency ?? 0
      };
      ElMessage.success(`「${item.name}」探测成功，延时 ${res.latencyMs ?? res.latency ?? 0}ms`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '网络连接异常';
      testResults.value[item.name] = { success: false, error: msg };
      ElMessage.error(`「${item.name}」连通性探测失败: ${msg}`);
    } finally {
      testingModels.value[item.name] = false;
    }
  }

  onMounted(async () => {
    await Promise.all([loadPresets(), loadModels()]);
  });

  return {
    loading,
    refreshing,
    saving,
    activeTab,
    models,
    presets,
    dialogVisible,
    editing,
    testingModels,
    testResults,
    dialogTesting,
    dialogTestResult,
    form,
    dimensionWarnVisible,
    chatModels,
    embeddingModels,
    displayedModels,
    currentPresetsGroup,
    currentPreset,
    currentModelOptions,
    reasoningLabel,
    getProviderDisplayName,
    handleConfigTypeChange,
    handleProviderChange,
    confirmDimensionWarn,
    openCreate,
    openEdit,
    handleRefresh,
    switchTab,
    handleSave,
    handleSetDefault,
    handleDelete,
    handleDialogTest,
    handleTest
  };
}
