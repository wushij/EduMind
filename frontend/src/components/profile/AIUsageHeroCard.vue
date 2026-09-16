<template>
  <ProfilePageHero
    title="个人 AI 使用量与配额"
    subtitle="实时监控大模型调用频次、Token 消耗明细及今日可用额度"
  >
    <template #actions>
      <div class="hero-action-row">
        <el-button
          round
          class="btn-refresh"
          :icon="Refresh"
          :loading="loading"
          @click="emit('refresh')"
        >
          刷新
        </el-button>
        <span v-if="lastUpdatedText" class="updated-at-pill">{{ lastUpdatedText }}</span>
      </div>
      <div class="quota-pill" :class="quotaPillClass">
        <span class="quota-dot" />
        {{ usage.quotaStatus }}
      </div>
      <div class="quota-percent-pill">
        剩余 <strong>{{ usage.remainingPercent }}%</strong>
      </div>
    </template>

    <template #footer>
      <div class="hero-progress-block">
        <div class="progress-meta">
          <span class="progress-label">今日 Token 额度</span>
          <span class="progress-val">
            {{ formatNumber(usage.todayTokensUsed) }}
            <span class="sep">/</span>
            {{ formatNumber(usage.dailyTokenLimit) }}
          </span>
        </div>
        <div class="capsule-progress-track">
          <div
            class="capsule-progress-fill"
            :class="quotaPillClass"
            :style="{ width: `${todayUsagePercent}%` }"
          />
        </div>
      </div>
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

defineProps<{
  loading: boolean;
  usage: PersonalAiUsageVO;
  quotaPillClass: string;
  todayUsagePercent: number;
  lastUpdatedText: string;
  formatNumber: (value: number) => string;
}>();

const emit = defineEmits<{
  refresh: [];
}>();
</script>

<style scoped lang="scss">
.updated-at-pill {
  padding: 6px 12px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e2e8f0;
  font-size: 11.5px;
  color: #94a3b8;
}

.quota-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 9999px;
  font-size: 13px;
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
    }
  }

  &.is-warning {
    background: #fffbeb;
    color: #d97706;
    border-color: #fde68a;

    .quota-dot {
      background: #f59e0b;
      box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.25);
    }
  }

  &.is-danger {
    background: #fef2f2;
    color: #dc2626;
    border-color: #fecaca;

    .quota-dot {
      background: #ef4444;
      box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.25);
    }
  }
}

.quota-percent-pill {
  padding: 8px 16px;
  border-radius: 9999px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #64748b;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

  strong {
    color: #0f172a;
    font-weight: 700;
  }
}

.hero-progress-block {
  .progress-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    font-size: 12.5px;

    .progress-label {
      color: #64748b;
      font-weight: 500;
    }

    .progress-val {
      color: #1e293b;
      font-weight: 600;
      font-variant-numeric: tabular-nums;

      .sep {
        color: #cbd5e1;
        margin: 0 4px;
        font-weight: 400;
      }
    }
  }

  .capsule-progress-track {
    width: 100%;
    height: 10px;
    background: rgba(226, 232, 240, 0.8);
    border-radius: 9999px;
    overflow: hidden;

    .capsule-progress-fill {
      height: 100%;
      border-radius: 9999px;
      transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);

      &.is-success {
        background: linear-gradient(90deg, #34d399 0%, #10b981 100%);
      }

      &.is-warning {
        background: linear-gradient(90deg, #fbbf24 0%, #f59e0b 100%);
      }

      &.is-danger {
        background: linear-gradient(90deg, #f87171 0%, #ef4444 100%);
      }
    }
  }
}
</style>
