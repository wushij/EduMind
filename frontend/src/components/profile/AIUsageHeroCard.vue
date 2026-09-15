<template>
  <div class="usage-hero-card">
    <div class="hero-bg-glow hero-bg-glow--blue" />
    <div class="hero-bg-glow hero-bg-glow--purple" />
    <div class="hero-content">
      <div class="hero-left">
        <span class="hero-eyebrow">Personal AI Dashboard</span>
        <h2>个人 AI 使用量与配额</h2>
        <p>实时监控大模型调用频次、Token 消耗明细及今日可用额度</p>
      </div>
      <div class="hero-right">
        <div class="hero-actions">
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
      </div>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
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
.usage-hero-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFF 55%, #F5F3FF 100%);
  border: 1px solid #E2E8F0;
  border-radius: 24px;
  padding: 28px 32px 24px;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);

  .hero-bg-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(60px);
    opacity: 0.35;

    &--blue {
      width: 220px;
      height: 220px;
      background: #BFDBFE;
      top: -80px;
      right: 120px;
    }

    &--purple {
      width: 160px;
      height: 160px;
      background: #DDD6FE;
      bottom: -60px;
      left: 40px;
    }
  }

  .hero-content {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 22px;

    @media (max-width: 768px) {
      flex-direction: column;
    }
  }

  .hero-left {
    .hero-eyebrow {
      display: inline-block;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #1677FF;
      background: rgba(22, 119, 255, 0.08);
      border: 1px solid rgba(22, 119, 255, 0.15);
      padding: 4px 14px;
      border-radius: 9999px;
      margin-bottom: 10px;
    }

    h2 {
      margin: 0;
      font-size: 22px;
      font-weight: 800;
      color: #0F172A;
      letter-spacing: -0.02em;
    }

    p {
      margin: 6px 0 0;
      font-size: 13.5px;
      color: #64748B;
      line-height: 1.5;
    }
  }

  .hero-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
    flex-shrink: 0;
    flex-wrap: wrap;
  }

  .hero-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .refresh-pill-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 7px 16px;
    border-radius: 9999px;
    border: 1px solid #BFDBFE;
    background: #FFFFFF;
    color: #2563EB;
    font-size: 12.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);

    &:hover:not(:disabled) {
      background: #EFF6FF;
      border-color: #93C5FD;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .updated-at-pill {
    padding: 6px 12px;
    border-radius: 9999px;
    background: rgba(255, 255, 255, 0.85);
    border: 1px solid #E2E8F0;
    font-size: 11.5px;
    color: #94A3B8;
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
      background: #ECFDF5;
      color: #059669;
      border-color: #A7F3D0;

      .quota-dot { background: #10B981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25); }
    }

    &.is-warning {
      background: #FFFBEB;
      color: #D97706;
      border-color: #FDE68A;

      .quota-dot { background: #F59E0B; box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.25); }
    }

    &.is-danger {
      background: #FEF2F2;
      color: #DC2626;
      border-color: #FECACA;

      .quota-dot { background: #EF4444; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.25); }
    }
  }

  .quota-percent-pill {
    padding: 8px 16px;
    border-radius: 9999px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    font-size: 13px;
    color: #64748B;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

    strong {
      color: #0F172A;
      font-weight: 700;
    }
  }

  .hero-progress-block {
    position: relative;

    .progress-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      font-size: 12.5px;

      .progress-label {
        color: #64748B;
        font-weight: 500;
      }

      .progress-val {
        color: #1E293B;
        font-weight: 600;
        font-variant-numeric: tabular-nums;

        .sep {
          color: #CBD5E1;
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
          background: linear-gradient(90deg, #34D399 0%, #10B981 100%);
        }

        &.is-warning {
          background: linear-gradient(90deg, #FBBF24 0%, #F59E0B 100%);
        }

        &.is-danger {
          background: linear-gradient(90deg, #F87171 0%, #EF4444 100%);
        }
      }
    }
  }
}

// 鈹€鈹€ 鎸囨爣鍗＄墖 鈹€鈹€
</style>
