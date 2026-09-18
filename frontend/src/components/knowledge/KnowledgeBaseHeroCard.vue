<template>
  <div class="kb-hero-card">
    <!-- 背景环境发光微晕 -->
    <div class="hero-bg-glow hero-bg-glow--blue" />
    <div class="hero-bg-glow hero-bg-glow--purple" />

    <div class="hero-content">
      <!-- 左侧：标题与定位说明 -->
      <div class="hero-left">
        <span class="hero-eyebrow">RAG 语义检索 & 知识资产中枢</span>
        <div class="hero-title-container">
          <div class="hero-icon-box">
            <el-icon :size="22"><Collection /></el-icon>
          </div>
          <h2 class="hero-heading">知识库管理</h2>
        </div>
        <p class="hero-subtitle">
          统一构建课程与学科级专有知识库，打通多模态文档解析、语义向量切片、精准检索与大模型教学答疑协同。
        </p>
      </div>

      <!-- 右侧：快捷操作区与状态药丸 -->
      <div class="hero-right">
        <div class="hero-action-row">
          <button
            type="button"
            class="hero-pill-btn is-primary"
            @click="emit('create')"
          >
            <el-icon><Plus /></el-icon>
            <span>新建知识库</span>
          </button>

          <button
            type="button"
            class="hero-pill-btn is-outline"
            @click="emit('open-retrieval')"
          >
            <el-icon><Search /></el-icon>
            <span>快速语义检索</span>
          </button>

          <button
            type="button"
            class="hero-pill-btn is-outline"
            @click="emit('open-rag-dashboard')"
          >
            <el-icon><DataAnalysis /></el-icon>
            <span>RAG 切片大盘</span>
          </button>

          <button
            type="button"
            class="hero-pill-btn is-refresh"
            :disabled="loading"
            @click="emit('refresh')"
          >
            <el-icon :class="{ 'is-spinning': loading }"><Refresh /></el-icon>
            <span>刷新</span>
          </button>

          <span v-if="lastUpdatedText" class="updated-at-pill">
            <el-icon class="clock-icon"><Clock /></el-icon>
            {{ lastUpdatedText }}
          </span>
        </div>

        <div class="hero-quota-badge-row">
          <div class="quota-pill" :class="vectorIndexPillClass">
            <span class="quota-dot" />
            {{ vectorIndexSummary }}
          </div>
          <div class="quota-percent-pill">
            嵌入模型
            <strong>{{ embeddingModelDisplay }}</strong>
            <template v-if="embeddingDimensionDisplay"> ({{ embeddingDimensionDisplay }}维)</template>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部状态指示坞 (类 AI 消耗明细进度胶囊) -->
    <div class="hero-footer">
      <div class="progress-horizontal-row">
        <span class="progress-label">向量切片索引状态</span>
        <div class="capsule-progress-track">
          <div
            class="capsule-progress-fill is-success"
            :style="{ width: `${chunkIndexPercent}%` }"
          />
        </div>
        <span class="progress-val">
          {{ formatNumber(indexedChunkCount) }}
          <span class="sep">/</span>
          {{ formatNumber(totalChunkCount) }}
          <span class="unit-text">Chunks 已向量化 ({{ chunkIndexPercent }}%)</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Collection, Plus, Search, Refresh, Clock, DataAnalysis } from '@element-plus/icons-vue';
import type { KnowledgeRagDashboardVO } from '@/types/knowledge/rag-dashboard';
import {
  resolveRagChunkIndexPercent,
  resolveRagIndexHealthPercent,
  resolveRagIndexStatusText
} from '@/utils/knowledge/rag-hero-display';

const props = withDefaults(
  defineProps<{
    loading?: boolean;
    ragStats?: KnowledgeRagDashboardVO | null;
    ragStatsLoading?: boolean;
    knowledgeBaseCount: number;
    totalDocs: number;
    totalChunks: number;
    lastUpdatedText?: string;
    formatNumber: (n: number) => string;
  }>(),
  {
    loading: false,
    ragStats: null,
    ragStatsLoading: false,
    lastUpdatedText: ''
  }
);

const emit = defineEmits<{
  create: [];
  'open-retrieval': [];
  'open-rag-dashboard': [];
  refresh: [];
}>();

const indexHealthPercent = computed(() => {
  if (!props.ragStats) return null;
  return resolveRagIndexHealthPercent(props.ragStats);
});

const vectorIndexSummary = computed(() => {
  if (props.ragStatsLoading && !props.ragStats) {
    return '向量索引状态：加载中…';
  }
  if (!props.ragStats) {
    return '向量索引状态：暂无数据（请检查后端或权限）';
  }
  const store = props.ragStats.vectorStoreLabel || '向量库';
  const status = resolveRagIndexStatusText(props.ragStats);
  const health = indexHealthPercent.value ?? 0;
  return `${store} 索引：${status}（健康度 ${health}%）`;
});

const vectorIndexPillClass = computed(() => {
  if (!props.ragStats) {
    return props.ragStatsLoading ? 'is-neutral' : 'is-warning';
  }
  const health = indexHealthPercent.value ?? 0;
  if (props.ragStats.latestIndexStatus === 'INDEXING') return 'is-neutral';
  if ((props.ragStats.failedChunks ?? 0) > 0 || health < 80) return 'is-warning';
  if (health < 95) return 'is-neutral';
  return 'is-success';
});

const embeddingModelDisplay = computed(() => {
  if (props.ragStatsLoading && !props.ragStats) return '加载中…';
  const name = props.ragStats?.embeddingModelName?.trim();
  return name || '未配置（将使用 Mock / 默认）';
});

const embeddingDimensionDisplay = computed(() => {
  const dim = props.ragStats?.embeddingDimension;
  return dim != null && dim > 0 ? String(dim) : '';
});

const totalChunkCount = computed(() => props.ragStats?.totalChunks ?? props.totalChunks);

const indexedChunkCount = computed(() => props.ragStats?.indexedChunks ?? 0);

const chunkIndexPercent = computed(() => {
  if (props.ragStats) {
    return resolveRagChunkIndexPercent(props.ragStats);
  }
  if (props.totalChunks === 0) return 100;
  return 0;
});
</script>

<style scoped lang="scss">
.kb-hero-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 55%, #f5f3ff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 28px 32px 22px;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);

  .hero-bg-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(60px);
    opacity: 0.35;

    &--blue {
      width: 240px;
      height: 240px;
      background: #bfdbfe;
      top: -80px;
      right: 140px;
    }

    &--purple {
      width: 180px;
      height: 180px;
      background: #ddd6fe;
      bottom: -60px;
      left: 60px;
    }
  }

  .hero-content {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 20px;

    @media (max-width: 960px) {
      flex-direction: column;
    }
  }

  .hero-left {
    min-width: 0;

    .hero-eyebrow {
      display: inline-block;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.04em;
      color: #1677ff;
      background: rgba(22, 119, 255, 0.08);
      border: 1px solid rgba(22, 119, 255, 0.15);
      padding: 4px 14px;
      border-radius: 9999px;
      margin-bottom: 10px;
    }

    .hero-title-container {
      display: flex;
      align-items: center;
      gap: 12px;

      .hero-icon-box {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 42px;
        height: 42px;
        border-radius: 14px;
        background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
        flex-shrink: 0;
      }

      .hero-heading {
        margin: 0;
        font-size: 24px;
        font-weight: 800;
        color: #0f172a;
        letter-spacing: -0.02em;
      }
    }

    .hero-subtitle {
      margin: 8px 0 0;
      font-size: 13.5px;
      color: #64748b;
      line-height: 1.6;
      max-width: 680px;
    }
  }

  .hero-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 12px;
    flex-shrink: 0;

    @media (max-width: 960px) {
      align-items: flex-start;
      width: 100%;
    }
  }

  .hero-action-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: flex-end;

    @media (max-width: 960px) {
      justify-content: flex-start;
    }

    .hero-pill-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 18px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      outline: none;
      white-space: nowrap;

      &.is-primary {
        background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
        color: #ffffff;
        border: none;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.3);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(22, 119, 255, 0.4);
          background: linear-gradient(135deg, #4096ff 0%, #1d4ed8 100%);
        }
      }

      &.is-outline {
        background: #ffffff;
        color: #2563eb;
        border: 1px solid #bfdbfe;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);

        &:hover {
          background: #eff6ff;
          border-color: #93c5fd;
          transform: translateY(-1px);
        }
      }

      &.is-refresh {
        background: #ffffff;
        color: #475569;
        border: 1px solid #e2e8f0;
        box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);

        &:hover:not(:disabled) {
          color: #1677ff;
          border-color: #bfdbfe;
          background: #f8faff;
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }

      .is-spinning {
        animation: spin 1s infinite linear;
      }
    }

    .updated-at-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 6px 13px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.85);
      border: 1px solid #e2e8f0;
      font-size: 11.5px;
      color: #64748b;

      .clock-icon {
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }

  .hero-quota-badge-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .quota-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 7px 16px;
    border-radius: 9999px;
    font-size: 12.5px;
    font-weight: 600;
    border: 1px solid transparent;

    .quota-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &.is-success {
      background: #ecfdf5;
      color: #059669;
      border-color: #a7f3d0;

      .quota-dot {
        background: #10b981;
        box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
        animation: pulse 2s infinite ease-in-out;
      }
    }

    &.is-neutral {
      background: #f1f5f9;
      color: #475569;
      border-color: #e2e8f0;

      .quota-dot {
        background: #94a3b8;
      }
    }

    &.is-warning {
      background: #fffbeb;
      color: #b45309;
      border-color: #fde68a;

      .quota-dot {
        background: #f59e0b;
      }
    }
  }

  .quota-percent-pill {
    padding: 7px 15px;
    border-radius: 9999px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    font-size: 12.5px;
    color: #64748b;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);

    strong {
      color: #0f172a;
      font-weight: 700;
      margin: 0 2px;
    }
  }

  .hero-footer {
    position: relative;
    margin-top: 22px;
    padding-top: 16px;
    border-top: 1px solid rgba(226, 232, 240, 0.7);

    .progress-horizontal-row {
      display: flex;
      align-items: center;
      gap: 16px;
      width: 100%;

      .progress-label {
        color: #334155;
        font-weight: 600;
        font-size: 13px;
        white-space: nowrap;
        flex-shrink: 0;
      }

      .capsule-progress-track {
        flex: 1;
        height: 8px;
        background: rgba(226, 232, 240, 0.85);
        border-radius: 9999px;
        overflow: hidden;
        min-width: 100px;

        .capsule-progress-fill {
          height: 100%;
          border-radius: 9999px;
          transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);

          &.is-success {
            background: linear-gradient(90deg, #34d399 0%, #10b981 100%);
          }
        }
      }

      .progress-val {
        color: #0f172a;
        font-weight: 700;
        font-size: 12.5px;
        font-variant-numeric: tabular-nums;
        white-space: nowrap;
        flex-shrink: 0;

        .sep {
          color: #94a3b8;
          margin: 0 4px;
          font-weight: 400;
        }

        .unit-text {
          color: #64748b;
          font-size: 11.5px;
          font-weight: 500;
          margin-left: 2px;
        }
      }
    }
  }
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.25);
    opacity: 0.7;
  }
}
</style>
