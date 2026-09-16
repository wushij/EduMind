<template>
  <div class="overall-analytics-view">
    <!-- 聚合表来源状态提示 -->
    <div v-if="isAggregated" class="status-alert-pill status-alert-pill--success">
      <svg viewBox="0 0 24 24" class="alert-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
        <polyline points="22 4 12 14.01 9 11.01"></polyline>
      </svg>
      <span>数据状态：当前 KPI 与趋势数据优先由 <strong>course_statistics</strong> 高性能日聚合引擎毫秒级呈现</span>
    </div>

    <!-- 6 核心 KPI 长圆卡片阵列 -->
    <div class="kpi-capsule-grid">
      <div class="kpi-capsule-card">
        <div class="card-icon-circle card-icon--blue">
          <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="card-text-wrap">
          <span class="card-label">在读选课学生</span>
          <div class="card-num-row">
            <strong class="card-num">{{ learningData?.studentCount ?? 0 }}</strong>
            <span class="card-unit">人</span>
          </div>
        </div>
      </div>

      <div class="kpi-capsule-card">
        <div class="card-icon-circle card-icon--green">
          <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 11 12 14 22 4"></polyline>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
          </svg>
        </div>
        <div class="card-text-wrap">
          <span class="card-label">任务章节完成率</span>
          <div class="card-num-row">
            <strong class="card-num">{{ formatPercent(learningData?.completionRate) }}</strong>
          </div>
        </div>
      </div>

      <div class="kpi-capsule-card">
        <div class="card-icon-circle card-icon--amber">
          <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="8" r="7"></circle>
            <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"></polyline>
          </svg>
        </div>
        <div class="card-text-wrap">
          <span class="card-label">班级测验平均分</span>
          <div class="card-num-row">
            <strong class="card-num">{{ learningData?.avgScore != null ? learningData.avgScore.toFixed(1) : '-' }}</strong>
            <span class="card-unit">分</span>
          </div>
        </div>
      </div>

      <div class="kpi-capsule-card">
        <div class="card-icon-circle card-icon--purple">
          <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="card-text-wrap">
          <span class="card-label">生均在线学习时长</span>
          <div class="card-num-row">
            <strong class="card-num">{{ learningData?.avgStudyMinutes ?? 0 }}</strong>
            <span class="card-unit">分钟</span>
          </div>
        </div>
      </div>

      <div class="kpi-capsule-card">
        <div class="card-icon-circle card-icon--cyan">
          <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 20h9"></path>
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
          </svg>
        </div>
        <div class="card-text-wrap">
          <span class="card-label">知识点掌握达成均值</span>
          <div class="card-num-row">
            <strong class="card-num">{{ formatPercent(learningData?.knowledgeMasteryAvg) }}</strong>
          </div>
        </div>
      </div>

      <div class="kpi-capsule-card">
        <div class="card-icon-circle card-icon--indigo">
          <svg viewBox="0 0 24 24" class="kpi-svg" fill="currentColor">
            <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
          </svg>
        </div>
        <div class="card-text-wrap">
          <span class="card-label">AI 助教答疑调用</span>
          <div class="card-num-row">
            <strong class="card-num">{{ learningData?.aiUsageCount?.toLocaleString() ?? 0 }}</strong>
            <span class="card-unit">次</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 中部图表与掌握度概览 -->
    <div class="chart-and-distribution-row">
      <div class="content-panel chart-panel">
        <div class="panel-header">
          <div class="header-title-group">
            <span class="title-decor-pill"></span>
            <h3 class="panel-title">班级学习活跃与成绩波动趋势</h3>
          </div>
          <span class="panel-tag">双轴融合态势</span>
        </div>
        <div class="chart-container">
          <LearningChart :trends="learningData?.trends ?? null" />
        </div>
      </div>

      <div class="content-panel distribution-panel">
        <div class="panel-header">
          <div class="header-title-group">
            <span class="title-decor-pill"></span>
            <h3 class="panel-title">班级知识图谱达成度概览</h3>
          </div>
          <span class="panel-tag">考点掌握分布</span>
        </div>

        <div class="distribution-body">
          <div class="mastery-summary-circle">
            <div class="circular-progress-wrap">
              <span class="circle-val">{{ formatPercent(learningData?.knowledgeMasteryAvg) }}</span>
              <span class="circle-sub">班级平均达成</span>
            </div>
          </div>

          <div class="mastery-tier-list">
            <div class="tier-item">
              <div class="tier-label">
                <span class="tier-dot tier-dot--mastered"></span>
                <span>牢固掌握 (≥85%)</span>
              </div>
              <span class="tier-count">6 个考点</span>
            </div>
            <div class="tier-item">
              <div class="tier-label">
                <span class="tier-dot tier-dot--learning"></span>
                <span>进阶理解中 (70%~84%)</span>
              </div>
              <span class="tier-count">12 个考点</span>
            </div>
            <div class="tier-item">
              <div class="tier-label">
                <span class="tier-dot tier-dot--weak"></span>
                <span>薄弱预警 (<70%)</span>
              </div>
              <span class="tier-count text-danger">3 个考点</span>
            </div>
          </div>

          <div class="distribution-tip-card">
            <svg viewBox="0 0 24 24" class="tip-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            <p>薄弱考点已自动同步至智能出题与自适应练习模块，点击学生画像可下发个性化强化任务。</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 班级选课学生学情诊断明细花名册 -->
    <div class="content-panel roster-panel">
      <div class="panel-header">
        <div class="header-title-group">
          <span class="title-decor-pill"></span>
          <h3 class="panel-title">班级选课学生学情诊断明细花名册</h3>
          <span class="roster-count-badge">{{ studentList.length }} 位学员</span>
        </div>
        <div class="header-actions">
          <div class="search-input-capsule">
            <svg viewBox="0 0 24 24" class="search-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索姓名、学号或用户名..."
              class="search-input"
            />
          </div>
        </div>
      </div>

      <div class="roster-table-wrap">
        <table class="roster-table">
          <thead>
            <tr>
              <th>学生姓名 / 编号</th>
              <th>在线学习时长</th>
              <th>平时测验均分</th>
              <th>知识点掌握度</th>
              <th>AI 助教互动</th>
              <th>错题记录</th>
              <th>学情态势</th>
              <th style="text-align: right;">个体画像</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="stu in filteredStudents" :key="stu.studentId" class="roster-row">
              <td>
                <div class="student-profile-cell">
                  <el-avatar :size="36" :src="stu.avatar" class="student-avatar">
                    {{ stu.realName?.slice(0, 1) || '学' }}
                  </el-avatar>
                  <div class="student-name-meta">
                    <span class="name-text">{{ stu.realName }}</span>
                    <span class="no-text">{{ stu.studentNo || `STU-000${stu.studentId}` }}</span>
                  </div>
                </div>
              </td>

              <td>
                <span class="val-bold">{{ stu.studyMinutes }}</span>
                <span class="unit-text"> 分钟</span>
              </td>

              <td>
                <span class="score-badge" :class="getScoreClass(stu.avgScore)">
                  {{ stu.avgScore.toFixed(1) }} 分
                </span>
              </td>

              <td>
                <div class="mastery-cell-wrap">
                  <div class="mastery-bar-bg">
                    <div
                      class="mastery-bar-fill"
                      :style="{ width: `${Math.min(100, stu.masteryScore)}%`, background: getMasteryColor(stu.masteryScore) }"
                    ></div>
                  </div>
                  <span class="mastery-text">{{ stu.masteryScore.toFixed(1) }}%</span>
                </div>
              </td>

              <td>
                <span class="val-bold text-primary">{{ stu.aiUsageCount }}</span>
                <span class="unit-text"> 次提问</span>
              </td>

              <td>
                <span class="wrong-badge" :class="{ 'wrong-badge--warn': stu.wrongCount > 3 }">
                  {{ stu.wrongCount }} 题
                </span>
              </td>

              <td>
                <span class="health-pill" :class="`health-pill--${stu.status.toLowerCase()}`">
                  <span class="dot"></span>
                  <span>{{ getHealthLabel(stu.status) }}</span>
                </span>
              </td>

              <td style="text-align: right;">
                <button
                  type="button"
                  class="action-pill-btn action-pill-btn--portrait"
                  title="深入查看该学生的学情诊断画像"
                  @click="emit('view-portrait', stu.studentId)"
                >
                  <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                  <span>查看画像</span>
                </button>
              </td>
            </tr>

            <tr v-if="filteredStudents.length === 0">
              <td colspan="8" class="empty-roster-cell">
                <p>未找到符合条件的选课学生</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- AI 教学诊断与干预建议卡片 -->
    <AiTeachingAdvice
      :advice="teachingAdvice"
      :loading="adviceLoading"
      @generate="emit('generate-advice')"
      @clear="emit('clear-advice')"
      @stop="emit('stop-advice')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import LearningChart from '@/components/analytics/LearningChart.vue';
import AiTeachingAdvice from '@/components/analytics/AiTeachingAdvice.vue';
import type { LearningAnalyticsVO, StudentLearningItemVO } from '@/types/analytics/learning';
import type { TeachingAdviceVO } from '@/types/analytics/mastery';

const props = defineProps<{
  learningData: LearningAnalyticsVO | null;
  isAggregated?: boolean;
  teachingAdvice: TeachingAdviceVO | null;
  adviceLoading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'view-portrait', studentId: number): void;
  (e: 'generate-advice'): void;
  (e: 'clear-advice'): void;
  (e: 'stop-advice'): void;
}>();

const searchKeyword = ref('');

const studentList = computed<StudentLearningItemVO[]>(() => {
  return props.learningData?.students ?? [];
});

const filteredStudents = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return studentList.value;
  return studentList.value.filter(
    (s) =>
      s.realName?.toLowerCase().includes(kw) ||
      s.username?.toLowerCase().includes(kw) ||
      s.studentNo?.toLowerCase().includes(kw)
  );
});

function formatPercent(value?: number) {
  if (value == null) return '-';
  const val = value <= 1.0 ? value * 100 : value;
  return `${val.toFixed(1)}%`;
}

function getScoreClass(score: number) {
  if (score >= 85) return 'score--excellent';
  if (score >= 70) return 'score--good';
  return 'score--risk';
}

function getMasteryColor(score: number) {
  if (score >= 80) return 'linear-gradient(90deg, #10B981, #059669)';
  if (score >= 65) return 'linear-gradient(90deg, #3B82F6, #2563EB)';
  return 'linear-gradient(90deg, #F59E0B, #EF4444)';
}

function getHealthLabel(status: string) {
  switch (status) {
    case 'EXCELLENT': return '学情优异';
    case 'GOOD': return '稳步推进';
    case 'WARNING': return '轻微滞后';
    case 'RISK': return '薄弱预警';
    default: return '正常修读';
  }
}
</script>

<style scoped lang="scss">
.overall-analytics-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// 聚合数据状态胶囊提示
.status-alert-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  border-radius: 9999px;
  font-size: 13px;
  box-sizing: border-box;

  &--success {
    background: #F0FDF4;
    border: 1px solid #BBF7D0;
    color: #166534;

    .alert-icon-svg {
      width: 16px;
      height: 16px;
      color: #16A34A;
      flex-shrink: 0;
    }
  }
}

// 6 核心 KPI 长圆卡片阵列
.kpi-capsule-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  .kpi-capsule-card {
    background: #FFFFFF;
    border-radius: 20px;
    padding: 18px 22px;
    border: 1px solid rgba(226, 232, 240, 0.9);
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
    display: flex;
    align-items: center;
    gap: 16px;
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
      border-color: #CBD5E1;
    }

    .card-icon-circle {
      width: 48px;
      height: 48px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .kpi-svg {
        width: 22px;
        height: 22px;
      }

      &--blue {
        background: #EFF6FF;
        color: #2563EB;
      }
      &--green {
        background: #F0FDF4;
        color: #16A34A;
      }
      &--amber {
        background: #FFFBEB;
        color: #D97706;
      }
      &--purple {
        background: #FAF5FF;
        color: #9333EA;
      }
      &--cyan {
        background: #ECFEFF;
        color: #0891B2;
      }
      &--indigo {
        background: #EEF2FF;
        color: #4F46E5;
      }
    }

    .card-text-wrap {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .card-label {
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
      }

      .card-num-row {
        display: flex;
        align-items: baseline;
        gap: 4px;

        .card-num {
          font-size: 24px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.5px;
        }

        .card-unit {
          font-size: 12px;
          color: #94A3B8;
          font-weight: 600;
        }
      }
    }
  }
}

// 图表与分布布局
.chart-and-distribution-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;

  .chart-panel {
    display: flex;
    flex-direction: column;
    min-height: 380px;

    .chart-container {
      flex: 1;
      width: 100%;
      min-height: 320px;
    }
  }

  .distribution-panel {
    display: flex;
    flex-direction: column;

    .distribution-body {
      padding: 16px 20px 20px;
      display: flex;
      flex-direction: column;
      gap: 18px;

      .mastery-summary-circle {
        display: flex;
        justify-content: center;
        padding: 10px 0;

        .circular-progress-wrap {
          width: 120px;
          height: 120px;
          border-radius: 50%;
          background: radial-gradient(circle, #FFFFFF 58%, #EFF6FF 100%);
          border: 4px solid #3B82F6;
          box-shadow: 0 4px 16px rgba(59, 130, 246, 0.15);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 2px;

          .circle-val {
            font-size: 24px;
            font-weight: 800;
            color: #1E40AF;
          }

          .circle-sub {
            font-size: 11px;
            color: #64748B;
            font-weight: 600;
          }
        }
      }

      .mastery-tier-list {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .tier-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 12px;
          background: #F8FAFC;
          border-radius: 12px;
          font-size: 13px;

          .tier-label {
            display: flex;
            align-items: center;
            gap: 8px;
            color: #334155;
            font-weight: 500;

            .tier-dot {
              width: 8px;
              height: 8px;
              border-radius: 50%;

              &--mastered { background: #10B981; }
              &--learning { background: #3B82F6; }
              &--weak { background: #EF4444; }
            }
          }

          .tier-count {
            font-weight: 700;
            color: #0F172A;

            &.text-danger {
              color: #DC2626;
            }
          }
        }
      }

      .distribution-tip-card {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        padding: 12px 14px;
        background: #FFFBEB;
        border-radius: 14px;
        border: 1px solid #FEF3C7;

        .tip-icon-svg {
          width: 16px;
          height: 16px;
          color: #D97706;
          flex-shrink: 0;
          margin-top: 2px;
        }

        p {
          margin: 0;
          font-size: 12px;
          color: #92400E;
          line-height: 1.5;
        }
      }
    }
  }
}

// 统一内容面板容器
.content-panel {
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  overflow: hidden;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 22px;
    border-bottom: 1px solid #F1F5F9;

    .header-title-group {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-decor-pill {
        width: 4px;
        height: 16px;
        border-radius: 9999px;
        background: #1677FF;
      }

      .panel-title {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
      }

      .roster-count-badge {
        font-size: 12px;
        padding: 2px 10px;
        border-radius: 9999px;
        background: #EAF3FF;
        color: #1677FF;
        font-weight: 700;
      }
    }

    .panel-tag {
      font-size: 12px;
      padding: 3px 10px;
      border-radius: 9999px;
      background: #F1F5F9;
      color: #64748B;
      font-weight: 600;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .search-input-capsule {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 6px 14px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        width: 240px;
        transition: all 0.2s ease;

        &:focus-within {
          background: #FFFFFF;
          border-color: #1677FF;
          box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15);
        }

        .search-svg {
          width: 14px;
          height: 14px;
          color: #94A3B8;
        }

        .search-input {
          border: none;
          background: transparent;
          outline: none;
          width: 100%;
          font-size: 12px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
          }
        }
      }
    }
  }
}

// 选课花名册表格
.roster-panel {
  .roster-table-wrap {
    width: 100%;
    overflow-x: auto;

    .roster-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;

      th {
        padding: 12px 22px;
        font-size: 12px;
        font-weight: 600;
        color: #64748B;
        background: #F8FAFC;
        border-bottom: 1px solid #E2E8F0;
      }

      td {
        padding: 14px 22px;
        border-bottom: 1px solid #F1F5F9;
        vertical-align: middle;
        font-size: 13px;
        color: #334155;
      }

      .roster-row {
        transition: background 0.2s ease;

        &:hover {
          background: #F8FAFC;
        }
      }

      .student-profile-cell {
        display: flex;
        align-items: center;
        gap: 12px;

        .student-avatar {
          background: #1677FF;
          color: #FFFFFF;
          font-weight: 700;
        }

        .student-name-meta {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .name-text {
            font-size: 14px;
            font-weight: 700;
            color: #0F172A;
          }

          .no-text {
            font-size: 11px;
            color: #94A3B8;
            font-family: monospace;
          }
        }
      }

      .val-bold {
        font-weight: 700;
        color: #0F172A;

        &.text-primary {
          color: #1677FF;
        }
      }

      .unit-text {
        font-size: 11px;
        color: #94A3B8;
      }

      .score-badge {
        display: inline-flex;
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 700;

        &.score--excellent {
          background: #DCFCE7;
          color: #15803D;
        }
        &.score--good {
          background: #DBEAFE;
          color: #1D4ED8;
        }
        &.score--risk {
          background: #FEE2E2;
          color: #B91C1C;
        }
      }

      .mastery-cell-wrap {
        display: flex;
        align-items: center;
        gap: 8px;
        width: 140px;

        .mastery-bar-bg {
          flex: 1;
          height: 6px;
          border-radius: 9999px;
          background: #E2E8F0;
          overflow: hidden;

          .mastery-bar-fill {
            height: 100%;
            border-radius: 9999px;
            transition: width 0.3s ease;
          }
        }

        .mastery-text {
          font-size: 12px;
          font-weight: 700;
          color: #1E293B;
          width: 44px;
        }
      }

      .wrong-badge {
        display: inline-flex;
        padding: 2px 8px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        background: #F1F5F9;
        color: #475569;

        &--warn {
          background: #FEF2F2;
          color: #DC2626;
        }
      }

      .health-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;

        .dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
        }

        &--excellent {
          background: #E6F7ED;
          color: #16A34A;
          .dot { background: #16A34A; }
        }
        &--good {
          background: #EAF3FF;
          color: #1677FF;
          .dot { background: #1677FF; }
        }
        &--warning {
          background: #FFFBEB;
          color: #D97706;
          .dot { background: #D97706; }
        }
        &--risk {
          background: #FEF2F2;
          color: #DC2626;
          .dot { background: #DC2626; }
        }
      }

      // 长圆胶囊画像按钮
      .action-pill-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 14px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
        border: none;
        outline: none;

        .btn-svg {
          width: 14px;
          height: 14px;
        }

        &--portrait {
          background: #EAF3FF;
          color: #1677FF;
          border: 1px solid rgba(22, 119, 255, 0.25);

          &:hover {
            background: #1677FF;
            color: #FFFFFF;
            box-shadow: 0 3px 10px rgba(22, 119, 255, 0.3);
            transform: translateY(-1px);
          }
        }
      }

      .empty-roster-cell {
        text-align: center;
        padding: 40px;
        color: #94A3B8;
      }
    }
  }
}

@media (max-width: 960px) {
  .kpi-capsule-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-and-distribution-row {
    grid-template-columns: 1fr;
  }
}
</style>
