<template>
  <div class="route-rules-page" v-loading="loading">
    <div class="page-top-nav">
      <el-button link class="back-btn" @click="router.push('/system/gateway')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回 AI 网关监控大盘</span>
      </el-button>
    </div>

    <div class="page-header">
      <div class="header-info">
        <div class="header-badges">
          <span class="badge primary">AI 网关核心调度</span>
          <span class="badge success">高可用容灾保障</span>
        </div>
        <h2>网关路由与模型调度规则</h2>
        <p>为平台核心教学业务场景配置主选执行模型与故障自动降级模型，当主模型发生限流、异常或超时时无缝熔断切换</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" class="gradient-btn" :loading="saving" @click="saveRoutes">
          <el-icon><Check /></el-icon>
          <span>保存路由策略</span>
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 示例数据，真实环境将直接同步服务端配置"
      class="mock-alert"
    />

    <el-card shadow="never" class="table-card">
      <div class="table-tip-row">
        <el-icon><InfoFilled /></el-icon>
        <span>建议：主模型选择能力强、推理质量高的大模型（如 deepseek-v4-flash），降级模型选择本地备用或高吞吐低延迟模型（如 mock 或轻量模型）。</span>
      </div>

      <el-table :data="routes" stripe style="width: 100%;">
        <el-table-column label="教学业务场景" width="240">
          <template #default="{ row }">
            <div class="scene-cell">
              <span class="scene-title">{{ getSceneName(row.scene) }}</span>
              <span class="scene-code font-mono">{{ row.scene }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="首选主模型 (Primary Model)" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.primaryModelKey"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入模型标识"
              style="width: 100%;"
            >
              <el-option
                v-for="opt in modelOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="自动降级模型 (Fallback Model)" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.fallbackModelKey"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入降级模型"
              style="width: 100%;"
            >
              <el-option
                v-for="opt in modelOptions"
                :key="`${opt.value}-fallback`"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="调度说明" width="220">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">
              {{ row.primaryModelKey === row.fallbackModelKey ? '未启用降级隔离' : '故障自动熔断降级' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { listGatewayRoutes, updateGatewayRoutes } from '@/api/system/gateway';
import { fetchModels } from '@/api/system/model';
import { USE_MOCK } from '@/config/mock';
import { MOCK_GATEWAY_ROUTES } from '@/mock/gateway';
import type { GatewayRouteVO } from '@/types/system/gateway';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Refresh, Check, InfoFilled } from '@element-plus/icons-vue';

const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const usedMockFallback = ref(false);
const routes = ref<GatewayRouteVO[]>([]);

interface ModelOption {
  label: string;
  value: string;
}

const modelOptions = ref<ModelOption[]>([]);

function buildModelOption(model: {
  name?: string;
  modelKey?: string;
  modelName?: string;
}): ModelOption {
  const configName = model.name || model.modelKey || model.modelName || '未命名模型';
  const modelName = model.modelName || configName;
  const label = modelName !== configName ? `${configName} · ${modelName}` : configName;
  return {
    label,
    value: model.modelKey || model.name || modelName
  };
}

function getSceneName(scene: string) {
  if (!scene) return '通用场景';
  switch (scene.toLowerCase()) {
    case 'chat': return '课程智能助教答疑';
    case 'rag':
    case 'chat_rag': return '课程知识库问答';
    case 'question':
    case 'question_generate': return 'AI 题库出题与变式';
    case 'grading': return '作业/主观题智能批改';
    case 'agent': return 'Agent 多步任务规划';
    case 'embedding': return '向量知识库切片嵌入';
    case 'stream': return '流式长文本启发对话';
    default: return scene;
  }
}

async function loadData() {
  loading.value = true;
  usedMockFallback.value = false;
  try {
    // 1. 加载所有系统中配置的模型，供下拉选择
    const models = await fetchModels();
    const opts: ModelOption[] = (models || [])
      .filter((m) => (m.configType === 'chat' || !m.configType) && m.status !== 'disabled')
      .map((m) => buildModelOption(m));

    // 保障 mock 降级项存在
    const knownValues = new Set(opts.map((o) => o.value));
    if (!knownValues.has('mock')) {
      opts.push({ label: 'mock', value: 'mock' });
    }
    modelOptions.value = opts;

    // 2. 加载路由规则
    const res = await listGatewayRoutes();
    routes.value = res.data ?? [];
  } catch {
    if (USE_MOCK) {
      usedMockFallback.value = true;
      routes.value = MOCK_GATEWAY_ROUTES.map((item) => ({ ...item }));
      if (modelOptions.value.length === 0) {
        modelOptions.value = [
          buildModelOption({ name: 'Flash', modelKey: 'Flash', modelName: 'deepseek-v4-flash' }),
          buildModelOption({ name: 'deepseek-chat', modelKey: 'deepseek-chat', modelName: 'deepseek-chat' }),
          { label: 'mock', value: 'mock' }
        ];
      }
    } else {
      routes.value = [];
      ElMessage.error('加载网关路由规则失败');
    }
  } finally {
    loading.value = false;
  }
}

async function saveRoutes() {
  saving.value = true;
  try {
    await updateGatewayRoutes(routes.value);
    ElMessage.success('网关路由调度规则已成功保存并立即生效');
    usedMockFallback.value = false;
  } catch {
    if (USE_MOCK) {
      ElMessage.success('Mock 模式：配置已本地保存');
    } else {
      ElMessage.error('保存路由规则失败');
    }
  } finally {
    saving.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped lang="scss">
.route-rules-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 40px;

  .page-top-nav {
    .back-btn {
      font-size: 13.5px;
      font-weight: 600;
      color: #2563EB;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0;

      &:hover {
        color: #1D4ED8;
      }
    }
  }

  .page-header {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 20px 24px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .header-info {
      .header-badges {
        display: flex;
        gap: 8px;
        margin-bottom: 8px;

        .badge {
          font-size: 11px;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 6px;

          &.primary {
            background: #EFF6FF;
            color: #2563EB;
          }

          &.success {
            background: #F0FDF4;
            color: #16A34A;
          }
        }
      }

      h2 {
        margin: 0 0 6px;
        font-size: 20px;
        font-weight: 800;
        color: #0F172A;
      }

      p {
        margin: 0;
        color: #64748B;
        font-size: 13px;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;

      .gradient-btn {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        border: none;
        border-radius: 8px;
      }
    }
  }

  .table-card {
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 10px 16px 20px;

    .table-tip-row {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;
      border-radius: 8px;
      padding: 10px 14px;
      margin-bottom: 16px;
      font-size: 12.5px;
      color: #475569;

      .el-icon {
        color: #2563EB;
        font-size: 16px;
      }
    }

    .scene-cell {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .scene-title {
        font-weight: 700;
        color: #1E293B;
        font-size: 13.5px;
      }

      .scene-code {
        font-size: 11px;
        color: #94A3B8;
      }
    }

  }
}
</style>
