<template>
  <div class="teaching-report-container">
    <!-- 1. 顶部 Hero Banner (教学分析主视觉背景 + 胶囊动作坞) -->
    <PageHeroBanner
      title="教学分析"
      subtitle="汇总课程学情、作业测验与 AI 助教使用数据，辅助教学决策"
      :background-image="analyticsBannerImg"
      background-variant="default"
      :show-illustration="false"
    >
      <template #actions>
        <div class="report-actions-dock">
          <div class="semester-pill-chip">
            <span>📅 当前评估周期：2026秋季学期 · 第3教学周</span>
          </div>

          <button
            type="button"
            class="capsule-btn capsule-btn--primary"
            @click="handleExportReport"
          >
            <span>📊 导出教学质量评估周报</span>
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
            🎯
          </span>
        </div>
        <div class="kpi-val-row">
          <span class="kpi-number">{{ passRate }}%</span>
          <span class="trend-pill trend-pill--up">+4.1% 环比上升</span>
        </div>
        <span class="kpi-sub">根据前 3 次随堂测验与作业推演</span>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-title">知识点全班平均掌握度</span>
          <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #722ED1 0%, #C084FC 100%)">
            🧠
          </span>
        </div>
        <div class="kpi-val-row">
          <span class="kpi-number">{{ masteryRate }}%</span>
          <span class="trend-pill trend-pill--up">+6.5% 较期初</span>
        </div>
        <span class="kpi-sub">核心考点 24 个已达达标线</span>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-title">AI 助教分担答疑频次</span>
          <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #059669 0%, #10B981 100%)">
            🤖
          </span>
        </div>
        <div class="kpi-val-row">
          <span class="kpi-number">{{ aiCallCount.toLocaleString() }} 次</span>
          <span class="trend-pill trend-pill--up">分担 76% 咨询</span>
        </div>
        <span class="kpi-sub">平均首字延迟 1.1s (满意度 98.6%)</span>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-title">AI 辅助批改节约工时</span>
          <span class="kpi-icon-bubble" style="background: linear-gradient(135deg, #D97706 0%, #F59E0B 100%)">
            ⏱️
          </span>
        </div>
        <div class="kpi-val-row">
          <span class="kpi-number">{{ savedHours }} h</span>
          <span class="trend-pill trend-pill--up">提效 68%</span>
        </div>
        <span class="kpi-sub">客观题秒级判分，主观题智能评分量规</span>
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
              <span class="panel-icon">📊</span>
              <h3 class="panel-title">本学期核心考点掌握度热力排行榜</h3>
            </div>
            <span class="badge-pill">基于大纲 24 个知识点</span>
          </div>

          <div class="kp-rank-list">
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
                <span class="tip-text">⚠️ 该考点错误率较高，建议使用 AI 出题进行随堂 5 分钟微测验</span>
                <button
                  type="button"
                  class="capsule-mini-btn"
                  @click="handleQuickQuiz(kp)"
                >
                  <span>✨ 一键生成巩固测验</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- B. 周度 AI 助教交互趋势 -->
        <div class="report-panel-card">
          <div class="panel-header-line">
            <div class="header-left">
              <span class="panel-icon">📈</span>
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
              <span class="panel-icon">🔍</span>
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
              <span class="panel-icon">💡</span>
              <h3 class="panel-title">AI 教学策略改进建议</h3>
            </div>
          </div>

          <div class="suggestion-content-box">
            <div class="suggestion-bubble">
              <div class="bot-avatar">🤖</div>
              <div class="bubble-text">
                <strong>下周课堂教学建议：</strong>
                <p>
                  根据本周做题轨迹分析，学生普遍在<strong>《未定式极限代换前提》</strong>与<strong>《泰勒公式高阶截断》</strong>存在理解偏差。建议在周二第 3 节课安排 <strong>10 分钟典型数形结合例题辨析</strong>。
                </p>
              </div>
            </div>

            <div class="action-buttons-stack">
              <button
                type="button"
                class="capsule-card-action-btn"
                @click="router.push('/ai/lesson-plan')"
              >
                <span>📋 一键由 AI 自动生成针对性教案</span>
              </button>

              <button
                type="button"
                class="capsule-card-action-btn capsule-card-action-btn--secondary"
                @click="router.push('/course/101/ai')"
              >
                <span>💬 与课程 AI 助教研讨教学方案</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import analyticsBannerImg from '@/assets/images/教学分析banner.png';
import { getTeachingReport } from '@/api/analytics/report';
import type { TeachingReportVO } from '@/types/analytics/report';

const router = useRouter();
const courseId = 1;
const reportData = ref<TeachingReportVO | null>(null);
const passRate = ref(92.4);
const masteryRate = ref(86.8);
const aiCallCount = ref(0);
const savedHours = ref(24.5);

const knowledgeMasteryList = ref([
  {
    id: 1,
    index: 1,
    name: '极限的四则运算法则与夹逼准则',
    course: '高等数学（上）',
    rate: 96,
    status: 'good',
    statusLabel: '掌握优秀'
  },
  {
    id: 2,
    index: 2,
    name: '导数的四则运算与复合函数求导',
    course: '高等数学（上）',
    rate: 92,
    status: 'good',
    statusLabel: '掌握良好'
  },
  {
    id: 3,
    index: 3,
    name: '二叉树非递归遍历显式栈模拟',
    course: '数据结构与算法',
    rate: 74,
    status: 'normal',
    statusLabel: '掌握中等'
  },
  {
    id: 4,
    index: 4,
    name: '泰勒公式的高阶皮亚诺余项展开',
    course: '高等数学（上）',
    rate: 56,
    status: 'warning',
    statusLabel: '需加紧巩固'
  },
  {
    id: 5,
    index: 5,
    name: '洛必达法则未定式前提与充要条件',
    course: '高等数学（上）',
    rate: 42,
    status: 'danger',
    statusLabel: '急需薄弱强化'
  }
]);

const weeklyActivity = ref([
  { date: '周一', count: 140 },
  { date: '周二', count: 210 },
  { date: '周三', count: 185 },
  { date: '周四', count: 260 },
  { date: '周五', count: 295 },
  { date: '周六', count: 190 },
  { date: '周日', count: 140 }
]);

const errorCategories = ref([
  {
    type: 'concept',
    name: '定理先验条件与概念混淆',
    percent: 42,
    color: '#EF4444',
    desc: '在未验证 $0/0$ 或 $\\infty/\\infty$ 时直接套用导数除法导致计算谬误。'
  },
  {
    type: 'calc',
    name: '代数计算与高阶符号失误',
    percent: 35,
    color: '#F59E0B',
    desc: '负号展开、高阶展开项保留阶数不足或冗余。'
  },
  {
    type: 'logic',
    name: '解题步骤不全与逆命题推演',
    percent: 23,
    color: '#3B82F6',
    desc: '主观问答题未陈述函数连续可导前提，扣除步骤分。'
  }
]);

async function loadReport() {
  try {
    const res = await getTeachingReport(courseId);
    reportData.value = res?.data || null;
    if (reportData.value) {
      passRate.value = reportData.value.avgSubmissionRate;
      masteryRate.value = Math.min(99, reportData.value.totalChapters * 10 + 50);
      aiCallCount.value = reportData.value.aiCallCount;
      knowledgeMasteryList.value = reportData.value.weakPoints.map((item, index) => ({
        id: index + 1,
        index: index + 1,
        name: item.title,
        course: `课程 #${courseId}`,
        rate: Math.max(30, 100 - item.wrongCount * 12),
        status: item.wrongCount >= 4 ? 'danger' : item.wrongCount >= 2 ? 'warning' : 'normal',
        statusLabel: item.suggestion
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
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.88);
      border: 1px solid rgba(226, 232, 240, 0.8);
      color: #334155;
      font-size: 13px;
      font-weight: 600;
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
          font-size: 16px;
          color: #FFFFFF;
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

            .capsule-mini-btn {
              padding: 3px 12px;
              border-radius: 9999px;
              background: #DC2626;
              color: #FFFFFF;
              border: none;
              font-size: 11px;
              font-weight: 600;
              cursor: pointer;
              transition: all 0.2s;

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
              background: #722ED1;
              color: #FFFFFF;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 18px;
              flex-shrink: 0;
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
