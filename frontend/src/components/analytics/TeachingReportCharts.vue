<template>
  <div class="split-left-col">
      <div class="report-panel-card">
        <div class="panel-header-line">
          <div class="header-left">
            <el-icon class="panel-icon panel-icon--blue"><Histogram /></el-icon>
            <h3 class="panel-title">本学期核心考点掌握度热力排行榜</h3>
          </div>
          <span class="badge-pill">覆盖 {{ knowledgeMasteryList.length }} 个薄弱知识点</span>
        </div>

        <div v-if="knowledgeMasteryList.length === 0" class="empty-kp-box">
          <el-empty description="暂无考点薄弱项数据，班级整体掌握良好" :image-size="80" />
        </div>
        <div v-else class="kp-rank-list">
          <div
            v-for="kp in knowledgeMasteryList"
            :key="kp.id"
            class="kp-rank-item"
          >
            <div class="kp-info-line">
              <div class="kp-title-group">
                <span class="kp-index" :class="`kp-index--${kp.status}`">{{ kp.index }}</span>
                <span class="kp-name">{{ kp.name }}</span>
                <span class="kp-course-tag">{{ kp.course }}</span>
              </div>
              <div class="kp-rate-group">
                <span class="kp-rate" :class="`kp-rate--${kp.status}`">{{ kp.rate }}%</span>
                <span class="kp-status-text">{{ kp.statusLabel }}</span>
              </div>
            </div>

            <div class="capsule-progress-track">
              <div
                class="capsule-progress-fill"
                :class="`fill-${kp.status}`"
                :style="{ width: `${kp.rate}%` }"
              ></div>
            </div>

            <div v-if="kp.status === 'danger'" class="kp-action-tip">
              <div class="tip-text">
                <el-icon class="tip-icon"><WarningFilled /></el-icon>
                <span>该考点错误率较高，建议使用 AI 出题进行随堂 5 分钟微测验</span>
              </div>
              <button
                type="button"
                class="capsule-mini-btn"
                @click="onQuickQuiz(kp)"
              >
                <el-icon class="btn-inner-icon"><MagicStick /></el-icon>
                <span>一键生成巩固测验</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="report-panel-card">
        <div class="panel-header-line">
          <div class="header-left">
            <el-icon class="panel-icon panel-icon--emerald"><TrendCharts /></el-icon>
            <h3 class="panel-title">近 7 日 AI 助教答疑负荷与学生提问时段分布</h3>
          </div>
        </div>

        <div class="weekly-bars-chart">
          <div
            v-for="day in weeklyActivity"
            :key="day.date"
            class="chart-bar-column"
          >
            <span class="bar-val-hint">{{ day.count }} 次</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ height: `${(day.count / 300) * 100}%` }"></div>
            </div>
            <span class="bar-date-label">{{ day.date }}</span>
          </div>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import {
  Histogram,
  WarningFilled,
  MagicStick,
  TrendCharts
} from '@element-plus/icons-vue';
import type { KnowledgeMasteryItem } from '@/composables/analytics/useTeachingReport';

defineProps<{
  knowledgeMasteryList: KnowledgeMasteryItem[];
  weeklyActivity: Array<{ date: string; count: number }>;
  onQuickQuiz: (kp: KnowledgeMasteryItem) => void;
}>();
</script>

<style scoped lang="scss">
.split-left-col {
  .report-panel-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 22px 24px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    margin-bottom: 20px;

    .panel-header-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .panel-icon {
          font-size: 18px;
          display: inline-flex;
          align-items: center;
          justify-content: center;

          &--blue { color: #1677FF; }
          &--emerald { color: #059669; }
        }

        .panel-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      .badge-pill {
        padding: 2px 10px;
        border-radius: 9999px;
        background: #F1F5F9;
        color: #64748B;
        font-size: 11.5px;
      }
    }

    .empty-kp-box {
      padding: 24px 0;
      display: flex;
      justify-content: center;
    }

    .kp-rank-list {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .kp-rank-item {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .kp-info-line {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 13px;

          .kp-title-group {
            display: flex;
            align-items: center;
            gap: 8px;

            .kp-index {
              width: 20px;
              height: 20px;
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 11px;
              font-weight: 700;
              background: #F1F5F9;
              color: #64748B;

              &--danger {
                background: #FEE2E2;
                color: #DC2626;
              }
            }

            .kp-name {
              font-weight: 600;
              color: #1E293B;
            }

            .kp-course-tag {
              font-size: 11px;
              color: #94A3B8;
            }
          }

          .kp-rate-group {
            display: flex;
            align-items: center;
            gap: 8px;

            .kp-rate {
              font-weight: 700;

              &--good { color: #059669; }
              &--normal { color: #1677FF; }
              &--warning { color: #D97706; }
              &--danger { color: #DC2626; }
            }

            .kp-status-text {
              font-size: 11px;
              color: #94A3B8;
            }
          }
        }

        .capsule-progress-track {
          width: 100%;
          height: 6px;
          background: #E2E8F0;
          border-radius: 9999px;
          overflow: hidden;

          .capsule-progress-fill {
            height: 100%;
            border-radius: 9999px;

            &.fill-good { background: linear-gradient(90deg, #10B981 0%, #34D399 100%); }
            &.fill-normal { background: linear-gradient(90deg, #1677FF 0%, #38BDF8 100%); }
            &.fill-warning { background: linear-gradient(90deg, #F59E0B 0%, #FBBF24 100%); }
            &.fill-danger { background: linear-gradient(90deg, #EF4444 0%, #F87171 100%); }
          }
        }

        .kp-action-tip {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 12px;
          background: #FEF2F2;
          border: 1px solid #FECACA;
          border-radius: 10px;
          font-size: 11.5px;
          color: #DC2626;

          .tip-text {
            display: flex;
            align-items: center;
            gap: 5px;

            .tip-icon {
              font-size: 14px;
              color: #DC2626;
              flex-shrink: 0;
            }
          }

          .capsule-mini-btn {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 12px;
            border-radius: 9999px;
            background: #DC2626;
            color: #FFFFFF;
            border: none;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;

            .btn-inner-icon {
              font-size: 12px;
            }

            &:hover {
              background: #B91C1C;
            }
          }
        }
      }
    }

    .weekly-bars-chart {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 16px;
      height: 160px;
      padding-top: 20px;

      .chart-bar-column {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        height: 100%;

        .bar-val-hint {
          font-size: 10.5px;
          color: #94A3B8;
          margin-bottom: 6px;
        }

        .bar-track {
          flex: 1;
          width: 24px;
          background: #F1F5F9;
          border-radius: 9999px 9999px 4px 4px;
          display: flex;
          align-items: flex-end;
          overflow: hidden;

          .bar-fill {
            width: 100%;
            background: linear-gradient(180deg, #38BDF8 0%, #1677FF 100%);
            border-radius: 9999px 9999px 0 0;
            transition: height 0.4s ease;
          }
        }

        .bar-date-label {
          font-size: 11.5px;
          color: #64748B;
          margin-top: 8px;
        }
      }
    }
  }
}

</style>
