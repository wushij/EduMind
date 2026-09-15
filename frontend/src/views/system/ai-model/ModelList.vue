<template>
  <div class="page-container">
    <!-- 顶部标题与操作卡片 -->
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div class="header-info">
          <div class="header-tag-row">
            <span class="module-badge">
              <el-icon><Cpu /></el-icon> EduMind AI · 模型配置
            </span>
            <span class="env-badge">Zero-Restart 热重载</span>
          </div>
          <h2 class="header-title">LLM 与 Embedding 模型管理</h2>
          <p class="header-subtitle">
            统一维护全平台对话推理与向量计算模型接入，支持行内一键连通性测试与默认调度策略
          </p>
        </div>
        <div class="header-actions">
          <el-button type="primary" class="add-btn" @click="openCreate">
            <el-icon><Plus /></el-icon> 新增模型
          </el-button>
          <el-button round :icon="Refresh" class="btn-refresh" :loading="refreshing" @click="handleRefresh">
            刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 类型切换分段胶囊 -->
    <div class="filter-segment-wrap">
      <div class="filter-segment">
        <button
          type="button"
          class="segment-btn"
          :class="{ active: activeTab === 'chat' }"
          @click="switchTab('chat')"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>对话推理模型 (Chat)</span>
          <span class="count-pill">{{ chatModels.length }}</span>
        </button>
        <button
          type="button"
          class="segment-btn"
          :class="{ active: activeTab === 'embedding' }"
          @click="switchTab('embedding')"
        >
          <el-icon><Connection /></el-icon>
          <span>向量计算模型 (Embedding)</span>
          <span class="count-pill">{{ embeddingModels.length }}</span>
        </button>
      </div>
    </div>

    <!-- 卡片网格展示区 -->
    <div v-loading="loading" class="models-container">
      <el-card v-if="displayedModels.length === 0 && !loading" shadow="never" class="empty-card">
        <el-empty :description="`暂未配置${activeTab === 'chat' ? '对话推理' : '向量计算'}模型`">
          <el-button type="primary" class="add-btn" @click="openCreate">
            <el-icon><Plus /></el-icon> 立即添加第一个模型
          </el-button>
        </el-empty>
      </el-card>

      <div v-else class="model-grid">
        <section v-for="m in displayedModels" :key="m.name" class="model-card">
          <div class="model-card__head">
            <h3 class="model-card__title" :title="m.name">{{ m.name }}</h3>
            <el-tag v-if="m.isDefault" type="primary" round size="small">默认</el-tag>
          </div>

          <p class="model-card__provider">{{ getProviderDisplayName(m.provider) }} · {{ m.modelName }}</p>
          <p v-if="m.configType === 'chat' || !m.configType" class="model-card__dim">
            思考强度 {{ reasoningLabel(m.reasoningEffort) }}
          </p>
          <p v-if="m.configType === 'embedding' && m.dimension" class="model-card__dim">
            向量维度 {{ m.dimension }}
          </p>

          <div class="model-card__meta-row">
            <el-tag :type="m.status === 'enabled' ? 'success' : 'info'" size="small" round>
              {{ m.status === 'enabled' ? 'enabled' : 'disabled' }}
            </el-tag>
            <span v-if="testingModels[m.name]" class="model-card__test-text is-testing">探测中...</span>
            <span
              v-else-if="testResults[m.name]?.success"
              class="model-card__test-text is-success"
            >
              连接成功 ({{ testResults[m.name].latency }}ms)
            </span>
            <span
              v-else-if="testResults[m.name]"
              class="model-card__test-text is-fail"
              :title="testResults[m.name].error"
            >
              连接失败
            </span>
          </div>

          <div class="model-card__actions">
            <el-button
              size="small"
              class="model-action-btn model-action-btn--neutral"
              :icon="Connection"
              :loading="testingModels[m.name]"
              @click="handleTest(m)"
            >
              测试
            </el-button>
            <el-button size="small" class="model-action-btn model-action-btn--edit" @click="openEdit(m)">
              编辑
            </el-button>
            <el-button
              v-if="!m.isDefault"
              size="small"
              class="model-action-btn model-action-btn--primary"
              @click="handleSetDefault(m)"
            >
              设默认
            </el-button>
            <el-button
              size="small"
              class="model-action-btn model-action-btn--danger"
              @click="handleDelete(m)"
            >
              删除
            </el-button>
          </div>
        </section>
      </div>
    </div>

    <!-- 创建 / 编辑模型弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑模型配置: ${editing.name}` : '接入新模型'"
      width="560px"
      append-to-body
      class="model-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="110px" class="model-form">
        <el-form-item label="配置唯一标识" required>
          <el-input
            v-model="form.name"
            class="pill-input"
            :disabled="!!editing"
            placeholder="如 deepseek-chat-prod、qwen-plus"
          />
        </el-form-item>

        <el-form-item label="模型类型" required>
          <el-radio-group
            v-model="form.configType"
            class="pill-radio-group"
            :disabled="!!editing"
            @change="handleConfigTypeChange"
          >
            <el-radio-button label="chat">对话推理 (Chat)</el-radio-button>
            <el-radio-button label="embedding">向量计算 (Embedding)</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="模型服务商" required>
          <el-select
            v-model="form.provider"
            class="pill-select"
            placeholder="选择服务商（自动填充端点与型号建议）"
            style="width: 100%"
            @change="handleProviderChange"
          >
            <el-option
              v-for="(preset, pKey) in currentPresetsGroup"
              :key="pKey"
              :label="preset.label"
              :value="pKey"
            />
          </el-select>
          <span v-if="form.configType === 'embedding'" class="form-item-tip">
            说明：DeepSeek 官方不提供向量接口；向量计算请选用通义千问 (DashScope) 或 OpenAI
          </span>
        </el-form-item>

        <el-form-item label="模型名称型号" required>
          <el-select
            v-model="form.modelName"
            class="pill-select"
            filterable
            allow-create
            default-first-option
            placeholder="可选择最新推荐型号或直接键入输入"
            style="width: 100%"
          >
            <el-option v-for="opt in currentModelOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>

        <el-form-item label="接口 Base URL">
          <el-input
            v-model="form.baseUrl"
            class="pill-input"
            placeholder="https://api.openai.com/v1（留空使用服务商默认端点）"
          />
        </el-form-item>

        <el-form-item label="API Key 秘钥">
          <el-input
            v-model="form.apiKey"
            class="pill-input"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="editing?.hasApiKey ? '留空不修改已配置秘钥' : '请输入 API Key'"
          />
          <div v-if="currentPreset?.portalUrl" class="portal-link-wrap">
            <el-link type="primary" :href="currentPreset.portalUrl" target="_blank" class="portal-link">
              前往获取 {{ currentPreset.label }} API Key ↗
            </el-link>
          </div>
          <div class="form-item-tip kms-security-tip">
            <el-icon class="kms-tip-icon"><Lock /></el-icon>
            <span>已使用国密 SM4-GCM 加密存储，受 KMS 版本控制</span>
            <span v-if="editing?.keyVersion" class="kms-version-tag">（当前版本: v{{ editing.keyVersion }}）</span>
          </div>
        </el-form-item>

        <el-form-item v-if="form.configType === 'chat'" label="采样温度">
          <div class="slider-row">
            <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.05" style="flex: 1" />
            <span class="slider-val">{{ form.temperature.toFixed(2) }}</span>
          </div>
        </el-form-item>

        <el-form-item v-if="form.configType === 'chat'" label="思考强度">
          <el-select v-model="form.reasoningEffort" class="pill-select" style="width: 100%">
            <el-option label="Low · 快速响应，留足正文" value="low" />
            <el-option label="Medium · 中度推演，均衡思考" value="medium" />
            <el-option label="High · 深度推演，攻克复杂逻辑" value="high" />
            <el-option label="Max · 最强思考，全算力展开" value="max" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.configType === 'embedding'" label="向量维度" required>
          <el-input-number
            v-model="form.dimension"
            class="pill-input-number"
            :min="128"
            :max="4096"
            :step="128"
            controls-position="right"
            style="width: 100%"
          />
          <span class="form-item-tip">
            OpenAI text-embedding-3-small → 1536；BAAI/bge-large-zh-v1.5 → 1024。切换不同维度后需执行「全量切片同步与重建」
          </span>
        </el-form-item>

        <el-form-item label="设为默认模型">
          <el-switch v-model="form.isDefault" />
          <span class="switch-hint">同类型下仅允许存在一个默认生效模型</span>
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.status" active-value="enabled" inactive-value="disabled" />
        </el-form-item>

        <el-form-item v-if="dialogTesting || dialogTestResult" class="dialog-test-form-item" label=" ">
          <div
            class="dialog-test-pill"
            :class="{
              testing: dialogTesting,
              success: !dialogTesting && dialogTestResult?.success,
              fail: !dialogTesting && dialogTestResult && !dialogTestResult.success
            }"
          >
            <template v-if="dialogTesting">
              <el-icon class="is-loading"><RefreshRight /></el-icon>
              <span>正在探测端点连通性，请稍候...</span>
            </template>
            <template v-else-if="dialogTestResult?.success">
              <el-icon><CircleCheckFilled /></el-icon>
              <span>连接成功，延时 {{ dialogTestResult.latency }}ms</span>
            </template>
            <template v-else-if="dialogTestResult">
              <el-icon><CircleCloseFilled /></el-icon>
              <span>{{ dialogTestResult.error || '连接失败' }}</span>
            </template>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="dialogTesting" :disabled="saving" @click="handleDialogTest">
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存配置并立即生效
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dimensionWarnVisible"
      width="480px"
      append-to-body
      class="gb-dialog-shell"
      :close-on-click-modal="false"
    >
      <template #header>
        <span class="dimension-warn-title">向量维度变更警告</span>
      </template>
      <p class="dimension-warn-body">
        存量知识切片向量不可跨维度比较，切换默认 Embedding 模型维度后需执行「全量切片同步与重建」，否则检索召回可能异常。
      </p>
      <template #footer>
        <el-button @click="dimensionWarnVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDimensionWarn">仍要切换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Plus,
  Refresh,
  RefreshRight,
  Connection,
  ChatDotRound,
  Cpu,
  CircleCheckFilled,
  CircleCloseFilled,
  Lock
} from '@element-plus/icons-vue';
import {
  fetchModels,
  createModel,
  updateModel,
  deleteModel,
  setDefaultModel,
  testModel,
  testModelDraft,
  getProviderPresets
} from '@/api/system/model';
import type { AIModelConfigItem, AIProviderPresetsResponse, ReasoningEffort } from '@/types/system/model';

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

const chatModels = computed(() => models.value.filter((m) => m.configType === 'chat' || !m.configType));
const embeddingModels = computed(() => models.value.filter((m) => m.configType === 'embedding'));
const displayedModels = computed(() => (activeTab.value === 'chat' ? chatModels.value : embeddingModels.value));

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

function reasoningLabel(effort?: ReasoningEffort): string {
  const map: Record<ReasoningEffort, string> = {
    low: 'LOW · 快速',
    medium: 'MEDIUM · 均衡',
    high: 'HIGH · 深度',
    max: 'MAX · 最强'
  };
  return map[effort || 'low'] || 'LOW · 快速';
}

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

function getProviderDisplayName(p?: string): string {
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

function suggestDimension(modelName: string): number {
  const lower = modelName.toLowerCase();
  if (lower.includes('bge') || lower.includes('1024')) return 1024;
  if (lower.includes('3072')) return 3072;
  if (lower.includes('768')) return 768;
  return 1536;
}

async function confirmDimensionChangeIfNeeded(newDimension: number, willBeDefault: boolean): Promise<boolean> {
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
    presets.value = await getProviderPresets();
  } catch (err: unknown) {
    console.warn('获取预设失败', err);
    const msg = err instanceof Error ? err.message : '获取 Provider 预设配置失败';
    ElMessage.error(msg);
  }
}

async function loadModels(options?: { silent?: boolean }) {
  if (!options?.silent) {
    loading.value = true;
  }
  try {
    models.value = await fetchModels();
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
    models.value = await fetchModels();
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '获取模型列表失败';
    ElMessage.error(msg);
  } finally {
    refreshing.value = false;
  }
}

async function syncModelsSilently() {
  try {
    models.value = await fetchModels();
  } catch {
    // 静默同步失败时保留本地列表，避免触发 loading 导致头部抖动
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
      await createModel(payload);
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
    const currentDefault = embeddingModels.value.find((m) => m.isDefault && m.name !== item.name);
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
    const res = await testModelDraft({
      name: form.name.trim() || undefined,
      provider: form.provider,
      configType: form.configType,
      modelName: form.modelName,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey || undefined
    });
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
    const res = await testModel(item.name);
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
</script>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card {
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  background: #ffffff;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.module-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #eaf3ff;
  color: #1677ff;
  border: 1px solid #bae0ff;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.env-badge {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
}

.header-title {
  margin: 4px 0 2px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.header-subtitle {
  color: #64748b;
  font-size: 13px;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;

  .add-btn {
    min-width: 112px;
  }

  .refresh-btn {
    min-width: 88px;
  }
}

.filter-segment-wrap {
  display: flex;
  align-items: center;
}

.filter-segment {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 999px;
  gap: 4px;
  border: 1px solid #e2e8f0;
}

.segment-btn {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;

  &:hover {
    color: #1677ff;
  }

  &.active {
    background: #ffffff;
    color: #1677ff;
    font-weight: 600;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  }
}

.count-pill {
  background: #f1f5f9;
  color: #64748b;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 11px;
  min-width: 22px;
  text-align: center;
  box-sizing: border-box;
}

.segment-btn.active .count-pill {
  background: #eaf3ff;
  color: #1677ff;
}

.empty-card {
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  padding: 40px 0;
  min-height: 280px;
  box-sizing: border-box;
}

.models-container {
  min-height: 280px;
  position: relative;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.model-card {
  padding: 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e8edf3;
  box-shadow:
    0 4px 16px rgba(15, 23, 42, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.model-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.model-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card__provider {
  margin: 0 0 4px;
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card__dim {
  margin: 0 0 12px;
  font-size: 12px;
  color: #1677ff;
  font-weight: 600;
}

.dimension-warn-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.dimension-warn-body {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
}

.model-card__meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 24px;
}

.model-card__test-text {
  font-size: 13px;
  flex-shrink: 0;
  white-space: nowrap;

  &.is-success {
    color: #52c41a;
  }

  &.is-fail {
    color: #f5222d;
    max-width: 50%;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &.is-testing {
    color: #1677ff;
  }
}

.model-card__actions {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  margin-top: 16px;

  .model-action-btn {
    flex-shrink: 0;
    white-space: nowrap;
  }
}

.model-action-btn {
  border-radius: 999px !important;
  font-weight: 500 !important;
  font-size: 12px !important;
  height: 28px !important;
  padding: 0 14px !important;
  margin: 0 !important;
  background: #f8fafc !important;
}

.model-action-btn--neutral {
  border: 1px solid #e2e8f0 !important;
  color: #64748b !important;

  &:hover {
    background: #f1f5f9 !important;
    border-color: #cbd5e1 !important;
    color: #334155 !important;
  }
}

.model-action-btn--edit,
.model-action-btn--primary {
  border: 1px solid rgba(22, 119, 255, 0.35) !important;
  color: #1677ff !important;

  &:hover {
    background: #eaf3ff !important;
    border-color: #1677ff !important;
    color: #0958d9 !important;
  }
}

.model-action-btn--danger {
  border: 1px solid rgba(245, 34, 45, 0.35) !important;
  color: #f5222d !important;

  &:hover {
    background: rgba(245, 34, 45, 0.08) !important;
    border-color: #f5222d !important;
  }
}

.form-item-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  display: block;
}

.portal-link-wrap {
  margin-top: 4px;
}

.portal-link {
  font-size: 12px;
}

.kms-security-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #10b981;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 999px;
  padding: 4px 14px;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
}

.kms-tip-icon {
  font-size: 13px;
  color: #059669;
}

.kms-version-tag {
  color: #047857;
  font-weight: 600;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.slider-val {
  font-size: 13px;
  font-weight: 700;
  color: #1677ff;
  width: 38px;
}

.switch-hint {
  font-size: 12px;
  color: #64748b;
  margin-left: 10px;
}

.dialog-test-form-item {
  margin-bottom: 4px;

  :deep(.el-form-item__label) {
    padding-right: 0;
  }

  :deep(.el-form-item__content) {
    width: 100%;
  }
}

.dialog-test-pill {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 18px;
  border-radius: 999px;
  font-size: 13px;
  line-height: 1.4;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;

  &.testing {
    background: #f8fafc;
    color: #64748b;
    border: 1px solid #e2e8f0;
  }

  &.success {
    background: #f6ffed;
    color: #389e0d;
    border: 1px solid #b7eb8f;
  }

  &.fail {
    background: #fff2f0;
    color: #cf1322;
    border: 1px solid #ffccc7;
  }

  .el-icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  span {
    flex: 1;
    min-width: 0;
    word-break: break-word;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

<!-- append-to-body 弹窗需全局样式才能命中 teleported DOM -->
<style lang="scss">
.model-dialog {
  .model-form {
    .pill-input .el-input__wrapper,
    .pill-select .el-select__wrapper {
      border-radius: 999px !important;
      box-shadow: 0 0 0 1px #e2e8f0 inset !important;
      padding: 4px 14px !important;
      background-color: #ffffff !important;
      transition: all 0.2s ease !important;
    }

    .pill-input .el-input__wrapper:hover,
    .pill-select .el-select__wrapper:hover {
      box-shadow: 0 0 0 1px #93c5fd inset !important;
    }

    .pill-input .el-input__wrapper.is-focus,
    .pill-select .el-select__wrapper.is-focused {
      box-shadow: 0 0 0 1px #1677ff inset, 0 0 0 3px rgba(22, 119, 255, 0.12) !important;
    }

    .pill-radio-group .el-radio-button__inner {
      border-radius: 0 !important;
      font-weight: 600 !important;
      padding: 8px 16px !important;
    }

    .pill-radio-group .el-radio-button:first-child .el-radio-button__inner {
      border-radius: 999px 0 0 999px !important;
    }

    .pill-radio-group .el-radio-button:last-child .el-radio-button__inner {
      border-radius: 0 999px 999px 0 !important;
    }

    .pill-input-number {
      width: 100% !important;
      border-radius: 999px !important;
      overflow: hidden !important;
      border: 1px solid #e2e8f0 !important;
      background-color: #ffffff !important;
      transition: all 0.2s ease !important;
    }

    .pill-input-number:hover {
      border-color: #93c5fd !important;
    }

    .pill-input-number.is-focus,
    .pill-input-number:focus-within {
      border-color: #1677ff !important;
      box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12) !important;
    }

    .pill-input-number .el-input__wrapper {
      border-radius: 0 !important;
      box-shadow: none !important;
      background: transparent !important;
      padding: 0 8px !important;
    }

    .pill-input-number .el-input__wrapper:hover,
    .pill-input-number .el-input__wrapper.is-focus {
      box-shadow: none !important;
    }

    .pill-input-number .el-input-number__decrease,
    .pill-input-number .el-input-number__increase {
      width: 34px !important;
      border: none !important;
      background-color: #f8fafc !important;
      color: #64748b !important;
    }

    .pill-input-number.is-controls-right .el-input-number__decrease,
    .pill-input-number.is-controls-right .el-input-number__increase {
      border-left: 1px solid #e2e8f0 !important;
    }

    .pill-input-number.is-controls-right .el-input-number__increase {
      border-radius: 0 999px 0 0 !important;
      border-bottom: 1px solid #e2e8f0 !important;
    }

    .pill-input-number.is-controls-right .el-input-number__decrease {
      border-radius: 0 0 999px 0 !important;
    }
  }

  .dialog-footer .el-button:not(.is-text):not(.is-link) {
    border-radius: 999px !important;
    font-weight: 600 !important;
    padding: 8px 18px !important;
  }
}
</style>
