<template>
  <div v-loading="loading" class="models-container">
    <el-card v-if="displayedModels.length === 0 && !loading" shadow="never" class="empty-card">
      <el-empty :description="`暂未配置${activeTab === 'chat' ? '对话推理' : '向量计算'}模型`">
        <el-button type="primary" class="add-btn" @click="onOpenCreate">
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
            @click="onTest(m)"
          >
            测试
          </el-button>
          <el-button size="small" class="model-action-btn model-action-btn--edit" @click="onEdit(m)">
            编辑
          </el-button>
          <el-button
            v-if="!m.isDefault"
            size="small"
            class="model-action-btn model-action-btn--primary"
            @click="onSetDefault(m)"
          >
            设默认
          </el-button>
          <el-button
            size="small"
            class="model-action-btn model-action-btn--danger"
            @click="onDelete(m)"
          >
            删除
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, Connection } from '@element-plus/icons-vue';
import type { AIModelConfigItem, ReasoningEffort } from '@/types/system/model';

defineProps<{
  loading: boolean;
  activeTab: 'chat' | 'embedding';
  displayedModels: AIModelConfigItem[];
  testingModels: Record<string, boolean>;
  testResults: Record<string, { success: boolean; latency?: number; error?: string }>;
  reasoningLabel: (effort?: ReasoningEffort) => string;
  getProviderDisplayName: (p?: string) => string;
  onOpenCreate: () => void;
  onTest: (m: AIModelConfigItem) => void;
  onEdit: (m: AIModelConfigItem) => void;
  onSetDefault: (m: AIModelConfigItem) => void;
  onDelete: (m: AIModelConfigItem) => void;
}>();
</script>

<style scoped lang="scss">
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
</style>
