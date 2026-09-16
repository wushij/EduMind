<template>
  <ProfilePageHero
    title="个人 AI 使用量与配额"
    subtitle="实时监控大模型调用频次、Token 消耗明细及周期可用额度"
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
      <div class="hero-quota-badge-row">
        <div class="quota-pill" :class="activePeriodInfo.quotaClass">
          <span class="quota-dot" />
          {{ activePeriodInfo.status }}
        </div>
        <div class="quota-percent-pill">
          <template v-if="selectedPeriod === 'today'">
            剩余 <strong>{{ usage.remainingPercent }}%</strong>
          </template>
          <template v-else>
            已用 <strong>{{ activePeriodInfo.percent }}%</strong>
          </template>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="period-switcher-row">
        <div class="period-pills">
          <button
            v-for="p in periodOptions"
            :key="p.key"
            type="button"
            class="period-pill-btn"
            :class="{ 'is-active': selectedPeriod === p.key }"
            @click="emit('update:selectedPeriod', p.key)"
          >
            {{ p.label }}
          </button>
        </div>
        <span class="period-tip-text">{{ activePeriodInfo.desc }}</span>
      </div>

      <div class="hero-progress-block">
        <div class="progress-horizontal-row">
          <span class="progress-label">{{ activePeriodInfo.label }}</span>
          <div class="capsule-progress-track">
            <div
              class="capsule-progress-fill"
              :class="activePeriodInfo.quotaClass"
              :style="{ width: `${activePeriodInfo.percent}%` }"
            />
          </div>
          <span class="progress-val">
            {{ formatNumber(activePeriodInfo.usedTokens) }}
            <template v-if="activePeriodInfo.limitTokens">
              <span class="sep">/</span>
              {{ formatNumber(activePeriodInfo.limitTokens) }}
            </template>
            <span class="unit-text">toks</span>
          </span>
        </div>
      </div>
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';
import type { AiUsageTimePeriod } from '@/composables/profile/useAIUsage';

export interface ActivePeriodInfo {
  key: string;
  label: string;
  usedTokens: number;
  limitTokens?: number;
  percent: number;
  quotaClass: string;
  status: string;
  desc: string;
}

const props = withDefaults(
  defineProps<{
    loading: boolean;
    usage: PersonalAiUsageVO;
    selectedPeriod?: AiUsageTimePeriod;
    activePeriodInfo?: ActivePeriodInfo;
    quotaPillClass: string;
    todayUsagePercent: number;
    lastUpdatedText: string;
    formatNumber: (value: number) => string;
  }>(),
  {
    selectedPeriod: 'today',
    activePeriodInfo: () => ({
      key: 'today',
      label: '今日 Token 额度',
      usedTokens: 0,
      limitTokens: 100000,
      percent: 0,
      quotaClass: 'is-success',
      status: '今日额度充足',
      desc: '日限额 100,000 toks / 日'
    })
  }
);

const emit = defineEmits<{
  refresh: [];
  'update:selectedPeriod': [period: AiUsageTimePeriod];
}>();

const periodOptions: Array<{ key: AiUsageTimePeriod; label: string }> = [
  { key: 'today', label: '今日 (1日)' },
  { key: '7d', label: '近 7 天 (一周)' },
  { key: '30d', label: '近 30 天 (一个月)' },
  { key: 'all', label: '全部累计 (总的)' }
];
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

.hero-quota-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
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

.period-switcher-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;

  .period-pills {
    display: inline-flex;
    align-items: center;
    background: rgba(241, 245, 249, 0.85);
    padding: 3px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;
    gap: 4px;

    .period-pill-btn {
      border: none;
      background: transparent;
      padding: 5px 14px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s ease;
      outline: none;

      &:hover:not(.is-active) {
        color: #1e293b;
      }

      &.is-active {
        background: #ffffff;
        color: #2563eb;
        box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
      }
    }
  }

  .period-tip-text {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
}

.hero-progress-block {
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
      height: 10px;
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

        &.is-warning {
          background: linear-gradient(90deg, #fbbf24 0%, #f59e0b 100%);
        }

        &.is-danger {
          background: linear-gradient(90deg, #f87171 0%, #ef4444 100%);
        }
      }
    }

    .progress-val {
      color: #0f172a;
      font-weight: 700;
      font-size: 13px;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
      flex-shrink: 0;

      .sep {
        color: #94a3b8;
        margin: 0 4px;
        font-weight: 400;
      }

      .unit-text {
        color: #94a3b8;
        font-size: 11.5px;
        font-weight: 500;
        margin-left: 2px;
      }
    }
  }

  @media (max-width: 640px) {
    .progress-horizontal-row {
      flex-wrap: wrap;
      gap: 8px;

      .capsule-progress-track {
        order: 3;
        width: 100%;
        flex: 100%;
      }
    }
  }
}
</style>
