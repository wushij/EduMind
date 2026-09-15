<template>
  <section class="kpi-cards-grid">
    <div class="kpi-card">
      <div class="kpi-top">
        <span class="kpi-title">班级期末及格预测率</span>
        <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)">
          <el-icon><Aim /></el-icon>
        </span>
      </div>
      <div class="kpi-val-row">
        <span class="kpi-number">{{ passRate }}%</span>
        <span :class="['trend-pill', passRate >= 80 ? 'trend-pill--up' : 'trend-pill--down']">
          {{ passRate >= 80 ? '达成良好' : '需重点辅导' }}
        </span>
      </div>
      <span class="kpi-sub">基于已完成作业与随堂测验推演</span>
    </div>

    <div class="kpi-card">
      <div class="kpi-top">
        <span class="kpi-title">知识点全班平均掌握度</span>
        <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #722ED1 0%, #C084FC 100%)">
          <el-icon><Reading /></el-icon>
        </span>
      </div>
      <div class="kpi-val-row">
        <span class="kpi-number">{{ masteryRate }}%</span>
        <span :class="['trend-pill', masteryRate >= 75 ? 'trend-pill--up' : 'trend-pill--down']">
          {{ masteryRate >= 75 ? '整体达标' : '待巩固强化' }}
        </span>
      </div>
      <span class="kpi-sub">基于当前课程考点学情统计汇总</span>
    </div>

    <div class="kpi-card">
      <div class="kpi-top">
        <span class="kpi-title">AI 助教分担答疑频次</span>
        <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #059669 0%, #10B981 100%)">
          <el-icon><Service /></el-icon>
        </span>
      </div>
      <div class="kpi-val-row">
        <span class="kpi-number">{{ aiCallCount.toLocaleString() }} 次</span>
        <span class="trend-pill trend-pill--up">7×24h 智能助学</span>
      </div>
      <span class="kpi-sub">全天候智能助学，分担常规教学咨询</span>
    </div>

    <div class="kpi-card">
      <div class="kpi-top">
        <span class="kpi-title">AI 辅助批改节约工时</span>
        <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #D97706 0%, #F59E0B 100%)">
          <el-icon><Timer /></el-icon>
        </span>
      </div>
      <div class="kpi-val-row">
        <span class="kpi-number">{{ savedHours }} h</span>
        <span class="trend-pill trend-pill--up">智能量规评阅</span>
      </div>
      <span class="kpi-sub">客观题自动判分，主观题智能评分量规</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Aim, Reading, Service, Timer } from '@element-plus/icons-vue';

defineProps<{
  passRate: number;
  masteryRate: number;
  aiCallCount: number;
  savedHours: number;
}>();
</script>

<style scoped lang="scss">
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;

  .kpi-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 20px 22px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    flex-direction: column;

    .kpi-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .kpi-title {
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
      }

      .kpi-icon-bubble {
        width: 34px;
        height: 34px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        color: #FFFFFF;
        box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);

        .el-icon {
          font-size: 18px;
        }
      }
    }

    .kpi-val-row {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 6px;
      margin-bottom: 8px;

      .kpi-number {
        font-size: 26px;
        font-weight: 700;
        color: #0F172A;
        letter-spacing: -0.5px;
      }

      .trend-pill {
        padding: 2px 10px;
        border-radius: 9999px;
        font-size: 11px;
        font-weight: 600;

        &--up {
          background: #ECFDF5;
          color: #059669;
        }

        &--down {
          background: #FEF2F2;
          color: #DC2626;
        }
      }
    }

    .kpi-sub {
      font-size: 11.5px;
      color: #94A3B8;
    }
  }
}

@media (max-width: 1280px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 640px) {
  .kpi-cards-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
