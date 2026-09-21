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
      <section
        v-for="m in displayedModels"
        :key="m.name"
        class="model-card"
        :class="{ 'is-default': m.isDefault, 'is-disabled': m.status !== 'enabled' }"
      >
        <!-- 默认模型顶部高光标 -->
        <div v-if="m.isDefault" class="default-ribbon">
          <el-icon><StarFilled /></el-icon>
          <span>默认调度</span>
        </div>

        <!-- 头部：图标 + 标题与模型 + 状态指示 -->
        <div class="model-card__header">
          <div class="model-card__main-info">
            <div class="model-icon-box" :class="`model-icon-box--${m.configType || 'chat'}`">
              <el-icon :size="20">
                <ChatDotRound v-if="m.configType === 'chat' || !m.configType" />
                <Connection v-else />
              </el-icon>
            </div>
            <div class="model-card__title-meta">
              <div class="title-row">
                <h3 class="model-name" :title="m.name">{{ m.name }}</h3>
              </div>
              <div class="provider-row">
                <span class="provider-name">{{ getProviderDisplayName(m.provider) }}</span>
                <span class="meta-dot">·</span>
                <code class="model-code" :title="m.modelName">{{ m.modelName }}</code>
              </div>
            </div>
          </div>

          <div class="model-card__status-box">
            <span class="status-dot" :class="m.status === 'enabled' ? 'is-active' : 'is-disabled'" />
            <span class="status-label">{{ m.status === 'enabled' ? '已启用' : '已禁用' }}</span>
          </div>
        </div>

        <!-- 参数指标区 -->
        <div class="model-card__params-grid">
          <template v-if="m.configType === 'chat' || !m.configType">
            <div class="param-badge">
              <span class="param-key">思考强度</span>
              <span class="param-val highlight-blue">{{ reasoningLabel(m.reasoningEffort) }}</span>
            </div>
            <div class="param-badge">
              <span class="param-key">采样温度</span>
              <span class="param-val">{{ m.temperature !== undefined ? m.temperature : 0.7 }}</span>
            </div>
          </template>
          <template v-else>
            <div class="param-badge" v-if="m.dimension">
              <span class="param-key">向量维度</span>
              <span class="param-val highlight-purple">{{ m.dimension }} 维</span>
            </div>
            <div class="param-badge">
              <span class="param-key">计算类型</span>
              <span class="param-val">Dense Embedding</span>
            </div>
          </template>
        </div>

        <!-- 连通性探测状态条 -->
        <div class="model-card__probe-slot">
          <div v-if="testingModels[m.name]" class="probe-item is-testing">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在探测模型连通性与握手延迟...</span>
          </div>
          <div v-else-if="testResults[m.name]?.success" class="probe-item is-success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>连接正常 · 响应延迟 {{ testResults[m.name].latency }}ms</span>
          </div>
          <div
            v-else-if="testResults[m.name]"
            class="probe-item is-fail"
            :title="testResults[m.name].error"
          >
            <el-icon><CircleCloseFilled /></el-icon>
            <span class="fail-text">连接异常：{{ testResults[m.name].error || '测试未通过' }}</span>
          </div>
          <div v-else class="probe-item is-idle">
            <el-icon><Odometer /></el-icon>
            <span>就绪 · 可一键连通性探测</span>
          </div>
        </div>

        <!-- 底部操作坞 -->
        <div class="model-card__actions-dock">
          <div class="actions-left">
            <el-button
              size="small"
              class="action-btn action-btn--test"
              :icon="Connection"
              :loading="testingModels[m.name]"
              @click="onTest(m)"
            >
              连通测试
            </el-button>
          </div>

          <div class="actions-right">
            <el-button
              v-if="!m.isDefault"
              size="small"
              class="action-btn action-btn--default"
              @click="onSetDefault(m)"
            >
              设默认
            </el-button>
            <el-button
              size="small"
              class="action-btn action-btn--edit"
              :icon="Edit"
              @click="onEdit(m)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              class="action-btn action-btn--delete"
              :icon="Delete"
              @click="onDelete(m)"
            >
              删除
            </el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Connection,
  ChatDotRound,
  StarFilled,
  Edit,
  Delete,
  Loading,
  CircleCheckFilled,
  CircleCloseFilled,
  Odometer
} from '@element-plus/icons-vue';
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
  border-radius: 16px;
  border: 1px solid #e8edf3;
  padding: 48px 0;
  min-height: 280px;
  box-sizing: border-box;
}

.models-container {
  min-height: 280px;
  position: relative;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

.model-card {
  position: relative;
  overflow: hidden;
  padding: 22px 24px 18px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e8edf3;
  box-shadow:
    0 4px 16px rgba(15, 23, 42, 0.03),
    0 1px 2px rgba(15, 23, 42, 0.02);
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: #94a3b8;
    box-shadow:
      0 8px 24px -4px rgba(15, 23, 42, 0.08),
      0 2px 6px -1px rgba(15, 23, 42, 0.04);
  }

  &.is-default {
    border-color: #93c5fd;
    background: linear-gradient(180deg, #f9fbff 0%, #ffffff 40%);
    box-shadow:
      0 4px 16px rgba(22, 119, 255, 0.06),
      0 1px 3px rgba(22, 119, 255, 0.04);

    &:hover {
      border-color: #3b82f6;
      box-shadow:
        0 8px 24px -4px rgba(22, 119, 255, 0.14),
        0 2px 6px -1px rgba(22, 119, 255, 0.06);
    }
  }

  &.is-disabled {
    opacity: 0.72;
    background: #f8fafc;
  }
}

.default-ribbon {
  position: absolute;
  top: 0;
  right: 0;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 14px 5px 12px;
  border-bottom-left-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
}

.model-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.model-card__main-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.model-icon-box {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--chat {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    border: 1px solid #bfdbfe;
    color: #1677ff;
  }

  &--embedding {
    background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
    border: 1px solid #ddd6fe;
    color: #7c3aed;
  }
}

.model-card__title-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.provider-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  min-width: 0;
}

.provider-name {
  white-space: nowrap;
  flex-shrink: 0;
}

.meta-dot {
  color: #cbd5e1;
}

.model-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  background: #f1f5f9;
  color: #334155;
  padding: 1px 6px;
  border-radius: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border: 1px solid #e2e8f0;
}

.model-card__status-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;

  &.is-active {
    background: #10b981;
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
  }

  &.is-disabled {
    background: #94a3b8;
  }
}

.status-label {
  font-size: 11px;
  font-weight: 500;
  color: #475569;
}

.model-card__params-grid {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.param-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 4px 12px;
  border-radius: 9999px; // 长圆参数胶囊
  font-size: 12px;

  .param-key {
    color: #64748b;
  }

  .param-val {
    color: #1e293b;
    font-weight: 600;

    &.highlight-blue {
      color: #1677ff;
    }

    &.highlight-purple {
      color: #7c3aed;
    }
  }
}

.model-card__probe-slot {
  min-height: 32px;
  display: flex;
  align-items: center;
}

.probe-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 14px;
  border-radius: 9999px; // 长圆探测槽
  width: 100%;
  box-sizing: border-box;

  &.is-idle {
    background: #f8fafc;
    color: #94a3b8;
    border: 1px dashed #e2e8f0;
  }

  &.is-testing {
    background: #eff6ff;
    color: #1677ff;
    border: 1px solid #bfdbfe;
  }

  &.is-success {
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
  }

  &.is-fail {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;

    .fail-text {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

.model-card__actions-dock {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.actions-left,
.actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  border-radius: 9999px !important; // 纯正长圆胶囊按钮
  font-weight: 500 !important;
  font-size: 12px !important;
  height: 28px !important;
  padding: 0 14px !important;
  margin: 0 !important;
  border: 1px solid #e2e8f0 !important;
  background: #ffffff !important;
  color: #475569 !important;
  transition: all 0.2s ease !important;

  &:hover {
    background: #f8fafc !important;
    border-color: #cbd5e1 !important;
    color: #0f172a !important;
  }

  &--test {
    background: #f0f7ff !important;
    border-color: #bae0ff !important;
    color: #1677ff !important;

    &:hover {
      background: #e0edff !important;
      border-color: #91caff !important;
      color: #0958d9 !important;
    }
  }

  &--default {
    border-color: #e2e8f0 !important;
    color: #475569 !important;

    &:hover {
      background: #eff6ff !important;
      border-color: #bfdbfe !important;
      color: #1677ff !important;
    }
  }

  &--edit {
    &:hover {
      background: #eff6ff !important;
      border-color: #bfdbfe !important;
      color: #1677ff !important;
    }
  }

  &--delete {
    &:hover {
      background: #fef2f2 !important;
      border-color: #fecaca !important;
      color: #dc2626 !important;
    }
  }
}
</style>

