<template>
  <div class="memory-capsule-card" :class="cardClass">
    <div class="card-accent-bar" :style="{ backgroundColor: accentColor }"></div>

    <div class="card-header-row">
      <div class="header-tags-group">
        <span class="type-pill" :style="{ backgroundColor: typeBgColor, color: typeTextColor }">
          {{ typeLabel }}
        </span>

        <span v-if="item.encrypted || item.sensitivityLevel === 'HIGH_RISK'" class="security-sm4-pill">
          <el-icon><Lock /></el-icon>
          <span>国密 SM4 加密</span>
        </span>

        <span class="source-channel-pill">
          {{ sourceLabel }}
        </span>
      </div>

      <div class="header-confidence-badge">
        <span class="conf-label">置信度:</span>
        <span class="conf-val">{{ confidencePercent }}%</span>
        <span class="conf-level" :class="confidenceLevelClass">{{ confidenceLevelText }}</span>
      </div>
    </div>

    <div class="card-content-body">
      <p class="memory-summary-text">{{ item.summary || item.memoryValue }}</p>

      <!-- 国密解密展示抽屉/区域 -->
      <div v-if="item.fullContent" class="decrypted-callout">
        <div class="callout-header">
          <el-icon><Unlock /></el-icon>
          <span class="title">国密 SM4 明文详情 (Key V{{ item.keyVersion || 1 }})：</span>
        </div>
        <p class="content">{{ item.fullContent }}</p>
      </div>

      <div v-else-if="item.encrypted || item.sensitivityLevel === 'HIGH_RISK'" class="decrypt-prompt-row">
        <el-button link size="small" type="primary" :loading="decrypting" @click="handleDecrypt">
          <el-icon><View /></el-icon>
          <span>授权解密查验敏感明文</span>
        </el-button>
      </div>

      <!-- AI 认知推导依据 -->
      <div v-if="item.reasoning" class="card-reasoning-callout">
        <div class="reason-header">
          <el-icon class="reason-icon"><Opportunity /></el-icon>
          <span class="reason-title">AI 认知推导依据：</span>
        </div>
        <p class="reason-text">{{ item.reasoning }}</p>
      </div>
    </div>

    <div class="card-footer-row">
      <div class="footer-meta-group">
        <span class="access-pill">
          <el-icon><Connection /></el-icon>
          助教已调用 {{ item.accessCount || 0 }} 次
        </span>
        <span class="time-meta">记录于 {{ formatTime(item.createTime) }}</span>
      </div>

      <div class="footer-actions-group">
        <el-button
          link
          size="small"
          type="primary"
          class="action-btn"
          @click="$emit('feedback', item, 5)"
        >
          <el-icon><Check /></el-icon>
          <span>准确</span>
        </el-button>

        <el-button
          link
          size="small"
          type="info"
          class="action-btn"
          @click="$emit('edit', item)"
        >
          <el-icon><EditPen /></el-icon>
          <span>校准</span>
        </el-button>

        <el-button
          link
          size="small"
          type="danger"
          class="action-btn danger"
          @click="$emit('forget', item)"
        >
          <el-icon><Delete /></el-icon>
          <span>遗忘</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Lock,
  Unlock,
  View,
  Connection,
  Check,
  EditPen,
  Delete,
  Opportunity
} from '@element-plus/icons-vue';
import type { MemoryItemVO } from '@/types/ai/memory';
import { getMemoryTypeLabel } from '@/composables/ai/useAgentMemory';

const props = defineProps<{
  item: MemoryItemVO;
}>();

const emit = defineEmits<{
  (e: 'feedback', item: MemoryItemVO, score: number): void;
  (e: 'edit', item: MemoryItemVO): void;
  (e: 'forget', item: MemoryItemVO): void;
  (e: 'decrypt', item: MemoryItemVO): Promise<void> | void;
}>();

const decrypting = ref(false);

const cardClass = computed(() => {
  const type = props.item.memoryType || 'PREFERENCE';
  return `type-${type.toLowerCase()}`;
});

const typeLabel = computed(() => getMemoryTypeLabel(props.item.memoryType));

const accentColor = computed(() => {
  switch (props.item.memoryType) {
    case 'PREFERENCE': return '#10B981';
    case 'PROFILE': return '#7C3AED';
    case 'EPISODIC': return '#F59E0B';
    case 'FEEDBACK': return '#06B6D4';
    default: return '#2563EB';
  }
});

const typeBgColor = computed(() => {
  switch (props.item.memoryType) {
    case 'PREFERENCE': return '#ECFDF5';
    case 'PROFILE': return '#F5F3FF';
    case 'EPISODIC': return '#FFFBEB';
    case 'FEEDBACK': return '#ECFEFF';
    default: return '#EFF6FF';
  }
});

const typeTextColor = computed(() => {
  switch (props.item.memoryType) {
    case 'PREFERENCE': return '#059669';
    case 'PROFILE': return '#6D28D9';
    case 'EPISODIC': return '#D97706';
    case 'FEEDBACK': return '#0891B2';
    default: return '#2563EB';
  }
});

const sourceLabel = computed(() => {
  if (props.item.sourceRef) return props.item.sourceRef;
  switch (props.item.sourceChannel) {
    case 'AI_CONVERSATION': return '助教对话萃取';
    case 'DIAGNOSTIC_ANALYSIS': return '学情诊断沉淀';
    case 'STUDENT_FEEDBACK': return '用户调优纠错';
    default: return '先验规则注入';
  }
});

const confidencePercent = computed(() => {
  const score = props.item.confidenceScore ?? 0.95;
  return Math.round(score * 100);
});

const confidenceLevelText = computed(() => {
  if (confidencePercent.value >= 95) return '强事实';
  if (confidencePercent.value >= 85) return '动态推断';
  return '演化中';
});

const confidenceLevelClass = computed(() => {
  if (confidencePercent.value >= 95) return 'level-high';
  if (confidencePercent.value >= 85) return 'level-mid';
  return 'level-low';
});

async function handleDecrypt() {
  try {
    decrypting.value = true;
    emit('decrypt', props.item);
  } finally {
    decrypting.value = false;
  }
}

function formatTime(isoStr?: string) {
  if (!isoStr) return '刚刚';
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return isoStr;
    return `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  } catch {
    return isoStr;
  }
}
</script>

<style scoped lang="scss">
.memory-capsule-card {
  position: relative;
  background: #FFFFFF;
  border-radius: 22px;
  border: 1px solid #E2E8F0;
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.03);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
    border-color: #BFDBFE;
  }

  .card-accent-bar {
    position: absolute;
    top: 0;
    left: 0;
    width: 5px;
    height: 100%;
    border-top-left-radius: 22px;
    border-bottom-left-radius: 22px;
  }

  .card-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    .header-tags-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .type-pill {
        font-size: 12px;
        font-weight: 600;
        padding: 3px 10px;
        border-radius: 9999px;
      }

      .security-sm4-pill {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        font-weight: 500;
        padding: 2px 8px;
        border-radius: 9999px;
        background: #FEF3C7;
        color: #B45309;
        border: 1px solid rgba(245, 158, 11, 0.25);
      }

      .source-channel-pill {
        font-size: 11px;
        color: #64748B;
        background: #F1F5F9;
        padding: 2px 8px;
        border-radius: 9999px;
      }
    }

    .header-confidence-badge {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      color: #64748B;

      .conf-val {
        font-weight: 700;
        color: #1E293B;
        font-family: var(--font-mono, monospace);
      }

      .conf-level {
        font-size: 10px;
        padding: 1px 6px;
        border-radius: 9999px;
        font-weight: 600;

        &.level-high {
          background: #ECFDF5;
          color: #059669;
        }
        &.level-mid {
          background: #EFF6FF;
          color: #2563EB;
        }
        &.level-low {
          background: #FFFBEB;
          color: #D97706;
        }
      }
    }
  }

  .card-content-body {
    flex: 1;
    margin-bottom: 14px;

    .memory-summary-text {
      font-size: 14px;
      line-height: 1.65;
      color: #334155;
      margin: 0;
      font-weight: 500;
    }

    .decrypted-callout {
      margin-top: 10px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      padding: 10px 14px;

      .callout-header {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        font-weight: 600;
        color: #0284C7;
        margin-bottom: 4px;
      }

      .content {
        font-size: 13px;
        color: #475569;
        line-height: 1.5;
        margin: 0;
      }
    }

    .decrypt-prompt-row {
      margin-top: 8px;
    }

    .card-reasoning-callout {
      margin-top: 10px;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;
      border-radius: 12px;
      padding: 8px 12px;

      .reason-header {
        display: flex;
        align-items: center;
        gap: 5px;
        font-size: 11px;
        font-weight: 600;
        color: #475569;
        margin-bottom: 2px;

        .reason-icon {
          font-size: 13px;
          color: #EAB308;
        }
      }

      .reason-text {
        font-size: 12px;
        color: #64748B;
        line-height: 1.45;
        margin: 0;
      }
    }
  }

  .card-footer-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #F1F5F9;
    padding-top: 12px;

    .footer-meta-group {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 12px;
      color: #94A3B8;

      .access-pill {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        background: #F8FAFC;
        padding: 2px 8px;
        border-radius: 9999px;
        color: #475569;
        font-weight: 500;
      }

      .time-meta {
        font-size: 11px;
      }
    }

    .footer-actions-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .action-btn {
        padding: 4px 8px;
        font-size: 12px;
        border-radius: 9999px;

        &.danger:hover {
          color: #EF4444;
        }
      }
    }
  }
}
</style>
