<template>
  <section class="kpi-cards-grid">
    <div class="kpi-card">
      <div class="kpi-top">
        <span class="kpi-title">班级测验及格率</span>
        <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #1677FF 0%, #38BDF8 100%)">
          <el-icon><Aim /></el-icon>
        </span>
      </div>
      <div class="kpi-val-row">
        <span class="kpi-number">{{ passRateText }}</span>
        <span
          v-if="passRateSampleAvailable"
          :class="['trend-pill', passRate >= 80 ? 'trend-pill--up' : 'trend-pill--down']"
        >
          {{ passRate >= 80 ? '达成良好' : '需重点辅导' }}
        </span>
        <span v-else class="trend-pill trend-pill--muted">暂无样本</span>
      </div>
      <span class="kpi-sub">
        {{ passRateSampleAvailable ? `已批改 ${gradedCount} 份成绩，均分 ≥ 60 计为及格` : '尚无已批改成绩，暂无法计算' }}
      </span>
    </div>

    <div class="kpi-card">
      <div class="kpi-top">
        <span class="kpi-title">知识点全班平均掌握度</span>
        <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #722ED1 0%, #C084FC 100%)">
          <el-icon><Reading /></el-icon>
        </span>
      </div>
      <div class="kpi-val-row">
        <span class="kpi-number">{{ masteryRateText }}</span>
        <span
          v-if="masteryRate !== null"
          :class="['trend-pill', masteryRate >= 75 ? 'trend-pill--up' : 'trend-pill--down']"
        >
          {{ masteryRate >= 75 ? '整体达标' : '待巩固强化' }}
        </span>
        <span v-else class="trend-pill trend-pill--muted">暂无数据</span>
      </div>
      <span class="kpi-sub">
        {{ masteryRate === null ? '课程未关联考点或尚无学情记录' : masteryEstimated ? '暂无实测掌握度记录，由作业均分推算' : '基于课程考点实测掌握度记录聚合' }}
      </span>
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
      <span class="kpi-sub">来自 AI 调用日志，统计周期内真实问答次数</span>
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
        <span v-if="savedHoursEstimated" class="trend-pill trend-pill--muted">估算值</span>
        <span v-else class="trend-pill trend-pill--up">智能量规评阅</span>
      </div>
      <span class="kpi-sub">按已批改 {{ gradedCount }} 份 × 单份人工均时 3 分钟折算</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Aim, Reading, Service, Timer } from '@element-plus/icons-vue';

const props = defineProps<{
  /** 班级测验及格率 (0~100) */
  passRate: number;
  /** 知识点全班平均掌握度 (0~100)；null 表示无数据 */
  masteryRate: number | null;
  /** 掌握度是否含推算成分 */
  masteryEstimated: boolean;
  aiCallCount: number;
  /** AI 辅助批改估算节约工时（小时） */
  savedHours: number;
  /** 节约工时是否为估算值 */
  savedHoursEstimated: boolean;
  /** 已批改答卷份数，用于判断及格率样本是否充足 */
  gradedCount: number;
}>();

/** 无已批改成绩时及格率无意义，展示「--」而不是 0% */
const passRateSampleAvailable = computed(() => props.gradedCount > 0);
const passRateText = computed(() =>
  passRateSampleAvailable.value ? `${props.passRate}%` : '--'
);
const masteryRateText = computed(() =>
  props.masteryRate === null ? '--' : `${props.masteryRate}%`
);
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

        &--muted {
          background: #F1F5F9;
          color: #64748B;
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
