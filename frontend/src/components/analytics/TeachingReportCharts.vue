<template>
  <div class="split-left-col">
    <!-- 1. 核心考点掌握度热力排行榜面板 -->
    <div class="report-panel-card">
      <div class="panel-header-line">
        <div class="header-left">
          <el-icon class="panel-icon panel-icon--blue"><Histogram /></el-icon>
          <h3 class="panel-title">本学期核心考点掌握度热力排行榜</h3>
        </div>
        <div class="header-right-group">
          <span class="rank-order-hint">可归因薄弱点优先 · 掌握度升序 · 同分按答错人次</span>
          <span class="badge-pill">{{ knowledgeMasteryList.length }} 个薄弱考点</span>
        </div>
      </div>

      <div v-if="knowledgeMasteryList.length === 0" class="empty-kp-box">
        <el-empty description="暂无考点薄弱项数据，班级整体掌握良好" :image-size="80" />
      </div>

      <div v-else class="kp-rank-list">
        <div
          v-for="kp in knowledgeMasteryList"
          :key="kp.id"
          class="kp-rank-card"
          :class="`border-level-${kp.status}`"
        >
          <!-- 考点头部：排名 + 名称 + 课程/章节 + 掌握度进度 -->
          <div class="kp-card-header">
            <div class="header-main-group">
              <span class="kp-badge" :class="getRankBadgeClass(kp.index)">
                {{ kp.index }}
              </span>
              <div class="title-meta-box">
                <div class="name-row">
                  <span class="kp-title" v-html="renderMath(kp.name)" />
                  <span class="course-name-tag">{{ kp.course }}</span>
                  <!-- 纯空白作答没有可归因内容：如实标注，并说明它不按掌握度抢榜位 -->
                  <span
                    v-if="kp.unansweredOnly"
                    class="merged-wrong-tag merged-wrong-tag--unanswered"
                  >
                    {{ kp.wrongStudentCount ?? 1 }} 人未作答 · 暂无归因
                  </span>
                  <!-- 题目数与人次必须分开标注：写着「合并 4 道错题」点开只有 1 道，就是这两个口径被混用 -->
                  <span v-else-if="(kp.wrongQuestionCount ?? 1) > 1" class="merged-wrong-tag">
                    合并 {{ kp.wrongQuestionCount }} 道错题
                  </span>
                  <span
                    v-else-if="(kp.wrongCount ?? 0) > 1"
                    class="merged-wrong-tag merged-wrong-tag--times"
                  >
                    {{ kp.wrongCount }} 人次答错
                  </span>
                </div>
              </div>
            </div>

            <!-- 右侧掌握度数值与等级胶囊 -->
            <div class="kp-rate-container">
              <span class="rate-number" :class="`text-${kp.status}`">
                {{ kp.rate === null ? '暂无数据' : `${kp.rate}%` }}
              </span>
              <span class="status-chip" :class="`chip-${kp.status}`">{{ kp.statusLabel }}</span>
              <!-- 样本量：0% 常常只是 1~2 名学生的实测结果，必须如实标注口径，避免被读成全班结论 -->
              <span v-if="kp.masterySampleCount" class="rate-sample-hint">
                实测 {{ kp.masterySampleCount }} 名学生<template
                  v-if="kp.masteryAssessmentCount"
                >（{{ kp.masteryAssessmentCount }} 次测评）</template>{{ kp.masterySampleCount < 3 ? ' · 样本较小' : '' }}
              </span>
            </div>
          </div>

          <!-- 进度条轨 -->
          <div class="capsule-progress-track">
            <div
              class="capsule-progress-fill"
              :class="`fill-${kp.status}`"
              :style="{ width: `${kp.rate ?? 0}%` }"
            />
          </div>

          <!-- AI 深度错因诊断与教学干预策略卡片 -->
          <div class="diagnosis-detail-card" :class="`diagnosis--${kp.status}`">
            <div class="diagnosis-header-line">
              <div class="diagnosis-tag-group">
                <span class="category-tag" :class="`cat-${kp.errorType || 'UNKNOWN'}`">
                  {{ kp.errorTypeName || '待归因' }}{{ kp.errorTypeInferred ? '（推断）' : '' }}
                </span>
                <span class="diagnosis-lead-title">AI 学情深度归因诊断：</span>
              </div>
            </div>

            <!-- 渲染数学公式的根本原因剖析；无诊断记录时不编造错因 -->
            <div
              v-if="kp.errorReason"
              class="diagnosis-content-text math-rendered-body"
              v-html="renderMath(kp.errorReason)"
            />
            <div v-else class="diagnosis-content-text diagnosis-content-text--empty">
              暂无该考点的 AI 错因诊断记录（需先产生错题并由 AI 完成归因）。
            </div>

            <!-- 下方建议与操作按钮行 -->
            <div class="diagnosis-footer-bar">
              <div v-if="kp.suggestion" class="suggestion-snippet">
                <el-icon class="action-light-icon"><AiSparkleIcon /></el-icon>
                <span class="sugg-text"><strong>教学建议：</strong>{{ kp.suggestion }}</span>
              </div>
              <div v-else class="suggestion-snippet suggestion-snippet--empty">
                <span class="sugg-text">暂无针对性教学建议</span>
              </div>

              <div class="kp-card-actions">
                <button
                  v-if="kp.questionId"
                  type="button"
                  class="action-text-btn"
                  @click="onViewQuestion(kp)"
                >
                  <el-icon><View /></el-icon>
                  <span>查看原题</span>
                </button>

                <button
                  type="button"
                  class="capsule-mini-btn"
                  :class="`btn-${kp.status}`"
                  @click="onQuickQuiz(kp)"
                >
                  <el-icon class="btn-inner-icon"><AiSparkleIcon /></el-icon>
                  <span>一键生成巩固测验</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 统计周期内学生学习活跃趋势（真实日活用户数） -->
    <div class="report-panel-card">
      <div class="panel-header-line">
        <div class="header-left">
          <el-icon class="panel-icon panel-icon--emerald"><TrendCharts /></el-icon>
          <h3 class="panel-title">统计周期内学生学习活跃趋势</h3>
        </div>
        <span v-if="weeklyActivity.length > 0" class="total-call-hint">
          累计活跃 <strong>{{ totalActiveUsers }}</strong> 人次
        </span>
      </div>

      <div v-if="weeklyActivity.length === 0" class="empty-kp-box">
        <el-empty description="当前周期内暂无学生学习行为记录" :image-size="80" />
      </div>

      <div v-else class="weekly-bars-chart">
        <div
          v-for="day in weeklyActivity"
          :key="day.date"
          class="chart-bar-column"
        >
          <span class="bar-val-hint">{{ day.count }} 人</span>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{ height: `${calculateBarHeight(day.count)}%` }"
              :title="`${day.date}: ${day.count} 名活跃学生`"
            />
          </div>
          <span class="bar-date-label">{{ day.date }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Histogram,
  TrendCharts,
  View
} from '@element-plus/icons-vue';
import type { KnowledgeMasteryItem } from '@/composables/analytics/useTeachingReport';
import { renderMathText } from '@/utils/format/render-math';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const props = defineProps<{
  knowledgeMasteryList: KnowledgeMasteryItem[];
  weeklyActivity: Array<{ date: string; count: number }>;
  onQuickQuiz: (kp: KnowledgeMasteryItem) => void;
  onViewQuestion: (kp: KnowledgeMasteryItem) => void;
}>();

/** 累计活跃人次：对真实日活学生数求和 */
const totalActiveUsers = computed(() => {
  return props.weeklyActivity.reduce((acc, curr) => acc + (curr.count || 0), 0);
});

const maxWeeklyCount = computed(() => {
  const max = Math.max(...props.weeklyActivity.map((d) => d.count), 10);
  return Math.max(max, 30);
});

function calculateBarHeight(count: number): number {
  if (!count) return 8;
  return Math.min(100, Math.max(12, Math.round((count / maxWeeklyCount.value) * 100)));
}

function getRankBadgeClass(index: number): string {
  if (index === 1) return 'badge-rank-1';
  if (index === 2) return 'badge-rank-2';
  if (index === 3) return 'badge-rank-3';
  return 'badge-rank-normal';
}

/** KaTeX 统一公式渲染工具，安全转义与美化理科公式 */
function renderMath(text?: string): string {
  if (!text) return '';
  return renderMathText(text);
}
</script>

<style scoped lang="scss">
.split-left-col {
  .report-panel-card {
    background: #ffffff;
    border-radius: 18px;
    border: 1px solid #e2e8f0;
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

          &--blue { color: #1677ff; }
          &--emerald { color: #059669; }
        }

        .panel-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
        }
      }

      .header-right-group {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        flex-wrap: wrap;
        gap: 10px;

        /* 排序口径：标题承诺"掌握度榜"，排序依据必须与之一致，避免被误读为按错题次数排 */
        .rank-order-hint {
          font-size: 11.5px;
          color: #94a3b8;
        }
      }

      .badge-pill {
        padding: 3px 12px;
        border-radius: 9999px;
        background: #f1f5f9;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
      }

      .total-call-hint {
        font-size: 12.5px;
        color: #64748b;

        strong {
          color: #059669;
          font-weight: 700;
        }
      }
    }

    .empty-kp-box {
      padding: 32px 0;
      display: flex;
      justify-content: center;
    }

    .kp-rank-list {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .kp-rank-card {
        background: #fafcff;
        border: 1px solid #e2e8f0;
        border-radius: 14px;
        padding: 16px 18px;
        transition: all 0.2s ease-in-out;

        &:hover {
          background: #ffffff;
          box-shadow: 0 6px 20px rgba(30, 80, 150, 0.06);
          border-color: #cbd5e1;
        }

        &.border-level-danger {
          border-left: 4px solid #ef4444;
        }

        &.border-level-warning {
          border-left: 4px solid #f59e0b;
        }

        &.border-level-normal {
          border-left: 4px solid #1677ff;
        }

        &.border-level-good {
          border-left: 4px solid #10b981;
        }

        &.border-level-unknown {
          border-left: 4px solid #cbd5e1;
        }

        .kp-card-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 12px;
          gap: 12px;

          .header-main-group {
            display: flex;
            align-items: center;
            gap: 12px;
            min-width: 0;
            flex: 1;

            .kp-badge {
              width: 24px;
              height: 24px;
              border-radius: 8px;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 12px;
              font-weight: 800;
              flex-shrink: 0;

              &.badge-rank-1 {
                background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
                color: #ffffff;
                box-shadow: 0 2px 6px rgba(217, 119, 6, 0.35);
              }

              &.badge-rank-2 {
                background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
                color: #ffffff;
                box-shadow: 0 2px 6px rgba(100, 116, 139, 0.3);
              }

              &.badge-rank-3 {
                background: linear-gradient(135deg, #b45309 0%, #78350f 100%);
                color: #ffffff;
                box-shadow: 0 2px 6px rgba(180, 83, 9, 0.3);
              }

              &.badge-rank-normal {
                background: #f1f5f9;
                color: #64748b;
              }
            }

            .title-meta-box {
              .name-row {
                display: flex;
                align-items: center;
                gap: 8px;
                flex-wrap: wrap;

                .kp-title {
                  font-size: 14.5px;
                  font-weight: 700;
                  color: #0f172a;
                  line-height: 1.4;
                }

                .course-name-tag {
                  display: inline-flex;
                  align-items: center;
                  padding: 1px 7px;
                  border-radius: 4px;
                  background: #f1f5f9;
                  color: #64748b;
                  font-size: 11px;
                }

                /* 同考点多道错题已合并，如实标注来源条数 */
                .merged-wrong-tag {
                  display: inline-flex;
                  align-items: center;
                  padding: 1px 7px;
                  border-radius: 4px;
                  background: #fef3c7;
                  color: #b45309;
                  font-size: 11px;
                }

                /* 只有一道题、多个学生答错：说「人次」，不能说「道数」 */
                .merged-wrong-tag--times {
                  background: #eef2ff;
                  color: #4338ca;
                }

                /* 纯空白作答：中性色标注，避免与真实错因混淆 */
                .merged-wrong-tag--unanswered {
                  background: #f1f5f9;
                  color: #64748b;
                }
              }
            }
          }

          .kp-rate-container {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-shrink: 0;

            .rate-number {
              font-size: 15px;
              font-weight: 800;
              font-family: ui-monospace, SFMono-Regular, monospace;
              flex-shrink: 0;
              white-space: nowrap;

              &.text-danger { color: #dc2626; }
              &.text-warning { color: #d97706; }
              &.text-normal { color: #1677ff; }
              &.text-good { color: #059669; }
              &.text-unknown { color: #94a3b8; font-size: 13px; }
            }

            .status-chip {
              padding: 2px 7px;
              border-radius: 9999px;
              font-size: 10px;
              font-weight: 600;
              line-height: 1.4;
              white-space: nowrap;
              flex-shrink: 0;
              display: inline-flex;
              align-items: center;
              justify-content: center;

              &.chip-danger {
                background: #fee2e2;
                color: #b91c1c;
              }

              &.chip-warning {
                background: #fef3c7;
                color: #b45309;
              }

              &.chip-normal {
                background: #e0f2fe;
                color: #0369a1;
              }

              &.chip-good {
                background: #d1fae5;
                color: #047857;
              }

              &.chip-unknown {
                background: #f1f5f9;
                color: #64748b;
              }
            }

            /* 实测覆盖人数与测评次数：极小样本下的 0% 需标注口径，避免被读成"全班都不掌握" */
            .rate-sample-hint {
              padding: 1px 7px;
              border-radius: 9999px;
              background: #ffffff;
              border: 1px solid #e2e8f0;
              color: #94a3b8;
              font-size: 10px;
              white-space: nowrap;
              flex-shrink: 0;
            }
          }
        }

        .capsule-progress-track {
          width: 100%;
          height: 7px;
          background: #e2e8f0;
          border-radius: 9999px;
          overflow: hidden;
          margin-bottom: 12px;

          .capsule-progress-fill {
            height: 100%;
            border-radius: 9999px;
            transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);

            &.fill-good { background: linear-gradient(90deg, #10b981 0%, #34d399 100%); }
            &.fill-normal { background: linear-gradient(90deg, #1677ff 0%, #38bdf8 100%); }
            &.fill-warning { background: linear-gradient(90deg, #f59e0b 0%, #fbbf24 100%); }
            &.fill-danger { background: linear-gradient(90deg, #ef4444 0%, #f87171 100%); }
            &.fill-unknown { background: linear-gradient(90deg, #cbd5e1 0%, #e2e8f0 100%); }
          }
        }

        .diagnosis-detail-card {
          border-radius: 10px;
          padding: 12px 14px;
          font-size: 12.5px;
          line-height: 1.6;

          &.diagnosis--danger {
            background: #fff5f5;
            border: 1px solid #fed7d7;
            color: #991b1b;
          }

          &.diagnosis--warning {
            background: #fffbeb;
            border: 1px solid #fde68a;
            color: #92400e;
          }

          &.diagnosis--normal {
            background: #f0f7ff;
            border: 1px solid #bae6fd;
            color: #075985;
          }

          &.diagnosis--good {
            background: #f0fdf4;
            border: 1px solid #bbf7d0;
            color: #166534;
          }

          &.diagnosis--unknown {
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            color: #475569;
          }

          .diagnosis-header-line {
            display: flex;
            align-items: center;
            margin-bottom: 6px;

            .diagnosis-tag-group {
              display: flex;
              align-items: center;
              gap: 8px;

              .category-tag {
                padding: 1px 7px;
                border-radius: 4px;
                font-size: 11px;
                font-weight: 700;

                &.cat-CONCEPT { background: #fee2e2; color: #dc2626; }
                &.cat-CALC { background: #fef3c7; color: #d97706; }
                &.cat-LOGIC { background: #e0f2fe; color: #0284c7; }
                &.cat-READING { background: #ede9fe; color: #6d28d9; }
                &.cat-TRANSFER { background: #e0f2fe; color: #0369a1; }
                &.cat-MEMORY { background: #f1f5f9; color: #475569; }
                &.cat-UNKNOWN { background: #f1f5f9; color: #64748b; }
              }

              .diagnosis-lead-title {
                font-weight: 700;
                font-size: 12px;
              }
            }
          }

          .diagnosis-content-text {
            color: #334155;
            margin-bottom: 10px;
            font-size: 12.5px;

            &--empty {
              color: #94a3b8;
              font-style: italic;
            }

            :deep(.katex) {
              font-size: 1.05em;
            }

            :deep(.katex-html) {
              color: #0f172a;
            }
          }

          .diagnosis-footer-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
            padding-top: 8px;
            border-top: 1px dashed rgba(203, 213, 225, 0.6);

            .suggestion-snippet {
              display: flex;
              align-items: center;
              gap: 6px;
              font-size: 12px;
              color: #475569;
              flex: 1;

              &--empty {
                color: #94a3b8;
              }

              .action-light-icon {
                font-size: 13px;
                color: #1677ff;
                flex-shrink: 0;
              }

              .sugg-text strong {
                color: #0f172a;
              }
            }

            .kp-card-actions {
              display: flex;
              align-items: center;
              gap: 10px;

              .action-text-btn {
                background: transparent;
                border: none;
                color: #1677ff;
                font-size: 12px;
                font-weight: 600;
                cursor: pointer;
                display: inline-flex;
                align-items: center;
                gap: 4px;
                padding: 4px 6px;

                &:hover {
                  text-decoration: underline;
                  color: #2563eb;
                }
              }

              .capsule-mini-btn {
                display: inline-flex;
                align-items: center;
                gap: 5px;
                padding: 4px 14px;
                border-radius: 9999px;
                color: #ffffff;
                border: none;
                font-size: 11.5px;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s ease;

                .btn-inner-icon {
                  font-size: 12px;
                }

                &.btn-danger {
                  background: #dc2626;
                  box-shadow: 0 2px 6px rgba(220, 38, 38, 0.25);
                  &:hover { background: #b91c1c; transform: translateY(-1px); }
                }

                &.btn-warning {
                  background: #d97706;
                  box-shadow: 0 2px 6px rgba(217, 119, 6, 0.25);
                  &:hover { background: #b45309; transform: translateY(-1px); }
                }

                &.btn-normal,
                &.btn-good {
                  background: #1677ff;
                  box-shadow: 0 2px 6px rgba(22, 119, 255, 0.25);
                  &:hover { background: #2563eb; transform: translateY(-1px); }
                }

                &.btn-unknown {
                  background: #64748b;
                  box-shadow: 0 2px 6px rgba(100, 116, 139, 0.25);
                  &:hover { background: #475569; transform: translateY(-1px); }
                }
              }
            }
          }
        }
      }
    }

    .weekly-bars-chart {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 10px;
      height: 160px;
      padding-top: 20px;

      .chart-bar-column {
        flex: 1;
        min-width: 56px;
        display: flex;
        flex-direction: column;
        align-items: center;
        height: 100%;

        .bar-val-hint {
          font-size: 11px;
          font-weight: 600;
          color: #64748b;
          margin-bottom: 6px;
          white-space: nowrap;
        }

        .bar-track {
          flex: 1;
          width: 26px;
          background: #f1f5f9;
          border-radius: 9999px 9999px 4px 4px;
          display: flex;
          align-items: flex-end;
          overflow: hidden;
          box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);

          .bar-fill {
            width: 100%;
            background: linear-gradient(180deg, #38bdf8 0%, #1677ff 100%);
            border-radius: 9999px 9999px 0 0;
            transition: height 0.5s cubic-bezier(0.4, 0, 0.2, 1);
            cursor: pointer;

            &:hover {
              background: linear-gradient(180deg, #0284c7 0%, #2563eb 100%);
            }
          }
        }

        .bar-date-label {
          font-size: 10px;
          font-weight: 500;
          color: #64748b;
          margin-top: 8px;
          white-space: nowrap;
          letter-spacing: -0.2px;
          line-height: 1.3;
        }
      }
    }
  }
}
</style>
