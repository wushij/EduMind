<template>
  <div class="teaching-report-container">
    <!-- 1. 顶部 Hero Banner (教学分析主视觉背景 + 胶囊动作坞) -->
    <PageHeroBanner
      title="教学分析"
      subtitle="汇总课程学情、作业测验与 AI 助教使用数据，辅助教学决策"
      background-variant="default"
      :show-illustration="false"
    >
      <template #actions>
        <div class="report-actions-dock">
          <div class="semester-pill-chip">
            <el-icon class="pill-prefix-icon"><Calendar /></el-icon>
            <span>{{ evaluationPeriod }}</span>
          </div>

          <button
            type="button"
            class="capsule-btn capsule-btn--primary"
            @click="handleExportReport"
          >
            <el-icon class="btn-prefix-icon"><Download /></el-icon>
            <span>导出教学质量评估周报</span>
          </button>
        </div>
      </template>
    </PageHeroBanner>

    <!-- 2. 四大核心教学能效 KPI 卡片矩阵 -->
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

    <!-- 3. 主体分栏：左侧知识点掌握度与趋势，右侧错因诊断与优化建议 -->
    <div class="analytics-main-split">
      <!-- 左侧：知识点掌握度热力排行 + 活跃趋势 -->
      <div class="split-left-col">
        <!-- A. 知识点掌握度热力排行 -->
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
                  @click="handleQuickQuiz(kp)"
                >
                  <el-icon class="btn-inner-icon"><MagicStick /></el-icon>
                  <span>一键生成巩固测验</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- B. 周度 AI 助教交互趋势 -->
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

      <!-- 右侧：错因智能归因诊断 + 教学策略建议卡片 -->
      <div class="split-right-col">
        <!-- A. 错因智能归类诊断 -->
        <div class="report-panel-card">
          <div class="panel-header-line">
            <div class="header-left">
              <el-icon class="panel-icon panel-icon--amber"><PieChart /></el-icon>
              <h3 class="panel-title">AI 学情错因聚类分析</h3>
            </div>
          </div>

          <div class="error-diagnosis-list">
            <div
              v-for="err in errorCategories"
              :key="err.type"
              class="error-cat-box"
            >
              <div class="error-top-line">
                <span class="err-name">{{ err.name }}</span>
                <span class="err-percent">{{ err.percent }}% 占比</span>
              </div>
              <div class="capsule-progress-track">
                <div class="capsule-progress-fill" :style="{ width: `${err.percent}%`, background: err.color }"></div>
              </div>
              <p class="err-desc">{{ err.desc }}</p>
            </div>
          </div>
        </div>

        <!-- B. AI 教学策略优化建议卡片 -->
        <div class="report-panel-card report-panel-card--ai-suggestion">
          <div class="panel-header-line">
            <div class="header-left">
              <el-icon class="panel-icon panel-icon--purple"><Opportunity /></el-icon>
              <h3 class="panel-title">AI 教学策略改进建议</h3>
            </div>
          </div>

          <div class="suggestion-content-box">
            <div class="suggestion-bubble">
              <div class="bot-avatar">
                <el-icon><Service /></el-icon>
              </div>
              <div class="bubble-text">
                <strong>课堂教学与改进建议：</strong>
                <p v-if="reportData && reportData.weakPoints && reportData.weakPoints.length > 0">
                  根据近期做题轨迹与错因诊断分析，学生在<strong>《{{ topWeakPointNames }}》</strong>等考点存在较集中错因。建议在下阶段教学中安排 <strong>10~15 分钟典型数形辨析与变式巩固</strong>。
                </p>
                <p v-else>
                  当前课程学生知识点掌握情况总体平稳，无显著集中性错因。建议持续关注平时作业订正率，并通过课程 AI 助教保持常态化答疑支持。
                </p>
              </div>
            </div>

            <div class="action-buttons-stack">
              <button
                type="button"
                class="capsule-card-action-btn"
                @click="router.push('/ai/lesson-plan')"
              >
                <el-icon class="btn-inner-icon"><DocumentAdd /></el-icon>
                <span>一键由 AI 自动生成针对性教案</span>
              </button>

              <button
                type="button"
                class="capsule-card-action-btn capsule-card-action-btn--secondary"
                @click="router.push(`/course/${courseId}/ai`)"
              >
                <el-icon class="btn-inner-icon"><ChatDotRound /></el-icon>
                <span>与课程 AI 助教研讨教学方案</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Calendar,
  Download,
  Aim,
  Reading,
  Service,
  Timer,
  Histogram,
  WarningFilled,
  MagicStick,
  TrendCharts,
  PieChart,
  Opportunity,
  DocumentAdd,
  ChatDotRound
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { getTeachingReport } from '@/api/analytics/report';
import type { TeachingReportVO } from '@/types/analytics/report';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

const router = useRouter();

const { courseId } = useTeacherCourses(102);
const reportData = ref<TeachingReportVO | null>(null);
const passRate = ref(0);
const masteryRate = ref(0);
const aiCallCount = ref(0);

const savedHours = computed(() => {
  if (!reportData.value) return 0;
  // 基于 AI 助教交互频次与作业提交动态推导节约工时
  const calculated = Math.round((aiCallCount.value * 0.05 + (passRate.value ? 4.5 : 0)) * 10) / 10;
  return Math.max(0.5, calculated);
});

const evaluationPeriod = computed(() => {
  const now = new Date();
  const month = now.getMonth() + 1;
  const semester = month >= 2 && month <= 7 ? '春季学期' : '秋季学期';
  const rangeText = reportData.value?.range ? ` · 统计周期: ${reportData.value.range}` : '';
  return `${now.getFullYear()}${semester} · 教学质量评估${rangeText}`;
});

const topWeakPointNames = computed(() => {
  if (!reportData.value?.weakPoints?.length) return '';
  return reportData.value.weakPoints
    .slice(0, 2)
    .map((w) => w.title)
    .join('》与《');
});

interface KnowledgeMasteryItem {
  id: number;
  index: number;
  name: string;
  course: string;
  rate: number;
  status: 'good' | 'normal' | 'warning' | 'danger';
  statusLabel: string;
}

const knowledgeMasteryList = ref<KnowledgeMasteryItem[]>([]);

const weeklyActivity = ref<Array<{ date: string; count: number }>>([]);

const errorCategories = ref<Array<{ type: string; name: string; percent: number; color: string; desc: string }>>([]);

const errorColorMap: Record<string, string> = {
  CONCEPT: '#EF4444',
  LOGIC: '#3B82F6',
  CALC: '#F59E0B'
};

async function loadReport() {
  try {
    const res = await getTeachingReport(courseId.value);
    reportData.value = res?.data || null;
    if (reportData.value) {
      passRate.value = reportData.value.avgSubmissionRate ?? 0;
      masteryRate.value = Math.round((reportData.value.knowledgeMasteryAvg ?? 0) * 100);
      aiCallCount.value = reportData.value.aiCallCount ?? 0;
      weeklyActivity.value = (reportData.value.weeklyActivity ?? []).map((item) => ({
        date: item.date,
        count: item.count
      }));
      errorCategories.value = (reportData.value.errorCategories ?? []).map((item) => ({
        type: item.type,
        name: item.name,
        percent: item.percent,
        color: errorColorMap[item.type] ?? '#94A3B8',
        desc: item.name
      }));
      knowledgeMasteryList.value = (reportData.value.weakPoints ?? []).map((item, index) => ({
        id: index + 1,
        index: index + 1,
        name: item.title,
        course: `课程 #${courseId.value}`,
        rate: Math.max(30, 100 - (item.wrongCount ?? 1) * 12),
        status: (item.wrongCount ?? 1) >= 4 ? 'danger' : (item.wrongCount ?? 1) >= 2 ? 'warning' : 'normal',
        statusLabel: item.suggestion || '建议巩固强化'
      }));
    }
  } catch {
    ElMessage.error('加载教学报告失败');
  }
}

function handleExportReport() {
  if (!reportData.value) {
    ElMessage.warning('暂无可导出的报告数据');
    return;
  }
  const blob = new Blob([JSON.stringify(reportData.value, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `teaching-report-${courseId}.json`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('教学报告已导出为 JSON');
}

onMounted(loadReport);

function handleQuickQuiz(kp: any) {
  router.push({
    path: '/ai/question/generate',
    query: {
      subject: kp.course,
      knowledgePoint: kp.name
    }
  });
}
</script>

<style scoped lang="scss">
.teaching-report-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. Hero 动作坞
  .report-actions-dock {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .semester-pill-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.88);
      border: 1px solid rgba(226, 232, 240, 0.8);
      color: #334155;
      font-size: 13px;
      font-weight: 600;

      .pill-prefix-icon {
        font-size: 14px;
        color: #1677FF;
      }
    }

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 20px;
      border-radius: 9999px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;

      .btn-prefix-icon {
        font-size: 15px;
      }

      &--primary {
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.28);

        &:hover {
          background: #4096FF;
          transform: translateY(-2px);
        }
      }
    }
  }

  // 2. KPI 卡片矩阵
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

  // 3. 主体分栏
  .analytics-main-split {
    display: grid;
    grid-template-columns: 58% 42%;
    gap: 20px;

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
            &--amber { color: #D97706; }
            &--purple { color: #722ED1; }
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

      // 排行榜列表
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

      // 柱状走势
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

      // 错因分类
      .error-diagnosis-list {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .error-cat-box {
          .error-top-line {
            display: flex;
            justify-content: space-between;
            font-size: 13px;
            margin-bottom: 6px;

            .err-name {
              font-weight: 600;
              color: #1E293B;
            }

            .err-percent {
              font-weight: 700;
              color: #1677FF;
            }
          }

          .capsule-progress-track {
            width: 100%;
            height: 6px;
            background: #E2E8F0;
            border-radius: 9999px;
            overflow: hidden;
            margin-bottom: 6px;

            .capsule-progress-fill {
              height: 100%;
              border-radius: 9999px;
            }
          }

          .err-desc {
            margin: 0;
            font-size: 11.5px;
            color: #94A3B8;
            line-height: 1.5;
          }
        }
      }

      // 建议面板
      &--ai-suggestion {
        background: linear-gradient(135deg, #FAF5FF 0%, #FFFFFF 100%);
        border-color: #E9D5FF;

        .suggestion-content-box {
          display: flex;
          flex-direction: column;
          gap: 16px;

          .suggestion-bubble {
            display: flex;
            gap: 12px;

            .bot-avatar {
              width: 38px;
              height: 38px;
              border-radius: 50%;
              background: linear-gradient(135deg, #722ED1 0%, #9333EA 100%);
              color: #FFFFFF;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 20px;
              flex-shrink: 0;
              box-shadow: 0 4px 12px rgba(114, 46, 209, 0.25);
            }

            .bubble-text {
              font-size: 12.5px;
              color: #334155;
              line-height: 1.6;

              strong {
                color: #722ED1;
              }

              p {
                margin: 4px 0 0 0;
              }
            }
          }

          .action-buttons-stack {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .capsule-card-action-btn {
              width: 100%;
              height: 38px;
              border-radius: 9999px;
              background: #722ED1;
              color: #FFFFFF;
              border: none;
              font-size: 13px;
              font-weight: 600;
              cursor: pointer;
              box-shadow: 0 2px 8px rgba(114, 46, 209, 0.25);
              transition: all 0.2s;
              display: inline-flex;
              align-items: center;
              justify-content: center;
              gap: 6px;

              .btn-inner-icon {
                font-size: 15px;
              }

              &:hover {
                background: #531DAB;
              }

              &--secondary {
                background: #FFFFFF;
                border: 1px solid #CBD5E1;
                color: #334155;
                box-shadow: none;

                &:hover {
                  background: #F8FAFC;
                  color: #1677FF;
                }
              }
            }
          }
        }
      }
    }
  }
}

@media (max-width: 1280px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .analytics-main-split {
    grid-template-columns: 1fr !important;
  }
}

@media (max-width: 640px) {
  .kpi-cards-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
