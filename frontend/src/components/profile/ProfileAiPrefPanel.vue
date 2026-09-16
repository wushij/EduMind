<template>
  <div class="profile-side-column">
    <div class="profile-panel-card ai-pref-card">
      <div class="panel-header-line">
        <div class="header-icon-wrap">
          <el-icon class="panel-icon"><Cpu /></el-icon>
        </div>
        <div class="header-text">
          <h3 class="panel-title">AI 大模型推理与答疑偏好</h3>
          <p class="panel-subtitle">仅展示当前租户已启用的推理模型，与智能网关配置一致</p>
        </div>
        <button
          type="button"
          class="icon-refresh-btn"
          :disabled="modelsLoading"
          title="刷新模型列表"
          @click="emit('refresh-models')"
        >
          <el-icon :class="{ 'is-loading': modelsLoading }"><Refresh /></el-icon>
        </button>
      </div>

      <div v-loading="prefLoading" class="preference-section">
        <div class="pref-item">
          <span class="pref-label">默认教学大模型引擎</span>
          <div v-if="modelsLoading && modelOptions.length === 0" class="pref-hint">正在同步可用模型…</div>
          <div v-else-if="modelOptions.length === 0" class="empty-models-box">
            <p>当前没有可用的对话模型。请在「系统管理 → 模型配置」中启用至少一个模型，或检查 API Key 是否已配置。</p>
            <router-link to="/system/models" class="text-link">前往模型配置</router-link>
          </div>
          <div v-else class="pill-radio-group">
            <span
              v-for="model in modelOptions"
              :key="model.modelKey"
              class="pill-radio-opt"
              :class="{ active: selectedModelKey === model.modelKey }"
              @click="selectedModelKey = model.modelKey"
            >
              <span class="pill-label">{{ model.name }}</span>
              <span v-if="model.isDefault" class="pill-badge">推荐</span>
            </span>
          </div>
        </div>

        <div class="pref-item">
          <span class="pref-label">生成创造性（Temperature）</span>
          <div class="pill-radio-group">
            <span
              v-for="opt in tempOptions"
              :key="opt.value"
              class="pill-radio-opt"
              :class="{ active: inferenceTemperature === opt.value }"
              @click="inferenceTemperature = opt.value"
            >
              {{ opt.label }}
            </span>
          </div>
        </div>

        <div class="pref-item">
          <span class="pref-label">RAG 知识库召回数量（TopK）</span>
          <div class="pill-radio-group">
            <span
              v-for="k in topKOptions"
              :key="k"
              class="pill-radio-opt"
              :class="{ active: ragTopK === k }"
              @click="ragTopK = k"
            >
              Top {{ k }} 片段
            </span>
          </div>
        </div>

        <div class="action-row">
          <button
            type="button"
            class="capsule-ai-pref-btn"
            :disabled="saving || modelOptions.length === 0"
            @click="emit('save')"
          >
            <span>{{ saving ? '保存中…' : '保存 AI 推理偏好' }}</span>
          </button>
          <router-link to="/profile/preferences" class="secondary-link">完整偏好设置</router-link>
        </div>
      </div>
    </div>

    <div class="profile-panel-card quota-card">
      <div class="panel-header-line">
        <div class="header-icon-wrap quota-icon">
          <el-icon class="panel-icon"><Lightning /></el-icon>
        </div>
        <div class="header-text">
          <h3 class="panel-title">今日个人 AI 算力额度</h3>
          <p class="panel-subtitle">按自然日统计 Token 消耗，与「AI 消耗明细」数据同源</p>
        </div>
        <button
          type="button"
          class="icon-refresh-btn"
          :disabled="usageLoading"
          title="刷新额度"
          @click="emit('refresh-usage')"
        >
          <el-icon :class="{ 'is-loading': usageLoading }"><Refresh /></el-icon>
        </button>
      </div>

      <div v-loading="usageLoading" class="quota-meter-box">
        <div class="quota-top">
          <span class="quota-label">今日 Token 消耗</span>
          <span class="quota-val">
            <strong>{{ formatNumber(usage.todayTokensUsed) }}</strong>
            <span class="quota-sep">/</span>
            {{ formatNumber(usage.dailyTokenLimit) }}
          </span>
        </div>
        <div class="capsule-progress-track">
          <div
            class="capsule-progress-fill"
            :class="quotaBarClass"
            :style="{ width: `${todayUsagePercent}%` }"
          />
        </div>
        <div class="quota-sub">
          <span>{{ usage.quotaStatus }} · 剩余约 {{ usage.remainingPercent }}%</span>
          <router-link to="/profile/ai-usage" class="quota-link">查看消耗明细</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Cpu, Lightning, Refresh } from '@element-plus/icons-vue';
import type { ModelProviderConfig } from '@/types/system/model';
import { calcTodayUsagePercent, formatUsageNumber } from '@/composables/profile/useAIUsage';

const props = defineProps<{
  modelOptions: ModelProviderConfig[];
  modelsLoading: boolean;
  prefLoading: boolean;
  saving: boolean;
  selectedModelKey: string;
  inferenceTemperature: number;
  ragTopK: number;
  usageLoading: boolean;
  usage: {
    todayTokensUsed: number;
    dailyTokenLimit: number;
    remainingPercent: number;
    quotaStatus: string;
  };
}>();

const emit = defineEmits<{
  'update:selectedModelKey': [value: string];
  'update:inferenceTemperature': [value: number];
  'update:ragTopK': [value: number];
  save: [];
  'refresh-models': [];
  'refresh-usage': [];
}>();

const selectedModelKey = computed({
  get: () => props.selectedModelKey,
  set: (v: string) => emit('update:selectedModelKey', v)
});

const inferenceTemperature = computed({
  get: () => props.inferenceTemperature,
  set: (v: number) => emit('update:inferenceTemperature', v)
});

const ragTopK = computed({
  get: () => props.ragTopK,
  set: (v: number) => emit('update:ragTopK', v)
});

const tempOptions = [
  { label: '严谨学术 (0.2)', value: 0.2 },
  { label: '平衡教学 (0.5)', value: 0.5 },
  { label: '启发发散 (0.8)', value: 0.8 }
];

const topKOptions = [3, 5, 8];

const todayUsagePercent = computed(() =>
  calcTodayUsagePercent(props.usage.todayTokensUsed, props.usage.dailyTokenLimit)
);

const quotaBarClass = computed(() => {
  const p = props.usage.remainingPercent;
  if (p >= 50) return 'is-healthy';
  if (p >= 20) return 'is-warning';
  return 'is-danger';
});

const formatNumber = formatUsageNumber;
</script>

<style scoped lang="scss">
.profile-side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-panel-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  padding: 22px 24px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.04);

  .panel-header-line {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 18px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f1f5f9;
  }

  .header-icon-wrap {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.quota-icon {
      background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
    }

    .panel-icon {
      font-size: 20px;
      color: #2563eb;
    }
  }

  .quota-icon .panel-icon {
    color: #7c3aed;
  }

  .header-text {
    flex: 1;
    min-width: 0;
  }

  .panel-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.35;
  }

  .panel-subtitle {
    margin: 4px 0 0;
    font-size: 12px;
    color: #64748b;
    line-height: 1.45;
  }

  .icon-refresh-btn {
    width: 34px;
    height: 34px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    color: #64748b;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    &:hover:not(:disabled) {
      color: #2563eb;
      border-color: #93c5fd;
      background: #eff6ff;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

.preference-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 80px;

  .pref-item .pref-label {
    display: block;
    font-size: 13px;
    color: #475569;
    font-weight: 600;
    margin-bottom: 10px;
  }

  .pref-hint {
    font-size: 13px;
    color: #94a3b8;
  }

  .empty-models-box {
    padding: 14px 16px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px dashed #cbd5e1;
    font-size: 13px;
    color: #64748b;
    line-height: 1.55;

    p {
      margin: 0 0 8px;
    }

    .text-link {
      color: #2563eb;
      font-weight: 600;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .pill-radio-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .pill-radio-opt {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      border-radius: 9999px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      color: #475569;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: #93c5fd;
        color: #2563eb;
      }

      &.active {
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border-color: #2563eb;
        color: #1d4ed8;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
      }

      .pill-badge {
        font-size: 10px;
        padding: 1px 6px;
        border-radius: 9999px;
        background: #fef2f2;
        color: #dc2626;
        font-weight: 700;
      }
    }
  }

  .action-row {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
    margin-top: 4px;
  }

  .capsule-ai-pref-btn {
    height: 40px;
    padding: 0 22px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
    color: #ffffff;
    border: none;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 14px rgba(109, 40, 217, 0.28);
    transition: all 0.2s ease;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(109, 40, 217, 0.35);
    }

    &:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
  }

  .secondary-link {
    font-size: 13px;
    color: #64748b;
    text-decoration: none;
    font-weight: 500;

    &:hover {
      color: #2563eb;
    }
  }
}

.quota-meter-box {
  .quota-top {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    font-size: 13px;
    margin-bottom: 10px;
    gap: 12px;
    flex-wrap: wrap;

    .quota-label {
      color: #64748b;
      font-weight: 500;
    }

    .quota-val {
      color: #1e293b;
      font-variant-numeric: tabular-nums;

      .quota-sep {
        margin: 0 4px;
        color: #94a3b8;
        font-weight: 400;
      }
    }
  }

  .capsule-progress-track {
    width: 100%;
    height: 10px;
    background: #e2e8f0;
    border-radius: 9999px;
    overflow: hidden;
    margin-bottom: 10px;

    .capsule-progress-fill {
      height: 100%;
      border-radius: 9999px;
      transition: width 0.35s ease;

      &.is-healthy {
        background: linear-gradient(90deg, #2563eb 0%, #38bdf8 100%);
      }

      &.is-warning {
        background: linear-gradient(90deg, #f59e0b 0%, #fbbf24 100%);
      }

      &.is-danger {
        background: linear-gradient(90deg, #ef4444 0%, #f87171 100%);
      }
    }
  }

  .quota-sub {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    font-size: 12px;
    color: #64748b;

    .quota-link {
      color: #2563eb;
      font-weight: 600;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>
