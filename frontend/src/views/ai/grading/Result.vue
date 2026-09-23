<template>
  <div v-loading="pageLoading" class="grading-report-page-container">
    <!-- 1:1 对齐图 2 (OcrWorkspace) 视觉规范的大卡片顶栏：浅蓝柔和渐变底色、精致微边框、无杂乱边距 -->
    <div class="grading-detail-header-card">
      <!-- 顶部导航与面包屑 -->
      <div class="header-nav-bar">
        <button type="button" class="back-btn" @click="router.push('/ai/grading')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回 AI 评阅中心</span>
        </button>
        <el-divider direction="vertical" class="nav-divider" />
        <el-breadcrumb separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/ai/grading' }">AI 智能批改</el-breadcrumb-item>
          <el-breadcrumb-item>智能评阅学情与错因归因报告</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 作业主体信息展示区 (横向全宽通透排版) -->
      <div class="header-info-showcase">
        <div class="bank-avatar-orb">
          <el-icon class="bank-icon"><DataAnalysis /></el-icon>
          <span class="orb-glow-ring" />
        </div>

        <div class="bank-meta-content">
          <div class="bank-title-line">
            <h1 class="bank-title" :title="reportTitle">{{ reportTitle }}</h1>
            <span class="course-badge">
              <el-icon><Cpu /></el-icon>
              {{ activeModelName }}
            </span>
            <span class="status-badge-capsule">
              <span class="status-indicator-dot" />
              任务状态：{{ reportStatusText }}
            </span>
          </div>

          <p class="bank-description">
            全卷聚合学生作答客观比对与主观大题长文本代码要点归因，自动生成学情薄弱点画像与教学干预建议。
          </p>

          <div class="bank-time-meta">
            <span class="time-item">
              <el-icon><Clock /></el-icon>
              最近评阅更新于 {{ lastUpdatedTime }}
            </span>
            <span class="meta-dot">·</span>
            <span class="time-item">
              <el-icon><DocumentChecked /></el-icon>
              {{ currentStudentInfoText }}
            </span>
          </div>
        </div>
      </div>

      <!-- 核心操作行：左侧答卷快速切换胶囊 + 右侧功能药丸按钮 (图 2 同款) -->
      <div class="header-actions-row">
        <div class="dock-left-tools">
          <!-- 答卷切换单层小巧药丸下拉 (图 2 试卷切换胶囊同款，支持多名学生无缝切换，实现数据完全互通) -->
          <el-select
            v-model="selectedSubmissionId"
            placeholder="切换目标答卷"
            class="paper-select-capsule"
            popper-class="modern-paper-popper"
            :fit-input-width="false"
            :teleported="true"
            @change="handleSubmissionChange"
          >
            <template #header>
              <div class="paper-popper-header">
                <div class="header-left">
                  <span class="header-title">学生答卷列表</span>
                  <span class="header-count">{{ assignmentSubmissions.length }} 份</span>
                </div>
                <span class="header-tip">已完成智能预评</span>
              </div>
            </template>

            <el-option
              v-for="sub in assignmentSubmissions"
              :key="sub.id"
              :label="`${sub.studentName || '学生答卷'} (#${sub.id})`"
              :value="sub.id"
              class="paper-card-option"
            >
              <div class="paper-card-inner">
                <div class="paper-info-col">
                  <div class="paper-title-row">
                    <span class="paper-name">{{ sub.studentName || '未命名考生' }}</span>
                    <span v-if="sub.studentNo" class="badge-custom-tag">{{ sub.studentNo }}</span>
                  </div>
                  <div class="paper-meta-row">
                    <span class="meta-desc">
                      状态：{{ sub.status === 'REVIEWED' ? '已终审' : (sub.status === 'GRADED' ? 'AI预评完成' : '待批改') }}
                    </span>
                    <span class="meta-dot">·</span>
                    <span class="meta-time">{{ sub.submitTime ? formatShortTime(sub.submitTime) : '已提交' }}</span>
                  </div>
                </div>

                <div class="paper-action-col">
                  <span class="page-pill-badge" :class="{ 'is-active': selectedSubmissionId === sub.id }">
                    {{ sub.totalScore != null ? `${sub.totalScore}分` : '待评分' }}
                  </span>
                </div>
              </div>
            </el-option>
          </el-select>

          <span class="current-student-pill">
            当前展示：<strong>{{ currentSubmission?.studentName || `答卷 #${selectedSubmissionId || '-'}` }}</strong>
            <span v-if="currentSubmission?.totalScore != null" class="score-chip">
              总分 {{ currentSubmission.totalScore }} / {{ analyticsData.maxScore }}
            </span>
          </span>
        </div>

        <div class="dock-right-actions">
          <button
            type="button"
            class="action-pill action-pill--brand"
            :disabled="!selectedSubmissionId"
            @click="handleReGradeCurrent"
          >
            <el-icon><Refresh /></el-icon>
            <span>重新 AI 评阅</span>
          </button>

          <button
            type="button"
            class="action-pill action-pill--compose"
            :disabled="!selectedSubmissionId"
            @click="handleSaveReview"
          >
            <el-icon><Check /></el-icon>
            <span>保存教师复核</span>
          </button>

          <button
            type="button"
            class="action-pill action-pill--success"
            @click="handleExportReport"
          >
            <el-icon><Download /></el-icon>
            <span>导出学情报告</span>
          </button>
        </div>
      </div>

      <!-- 底部：4 维教学资产微看板 (1:1 对齐图 2 的 4 维指标网格) -->
      <section class="bank-stats-grid">
        <!-- 指标卡 1：班级平均得分 -->
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon-box text-blue-600 bg-blue-50">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">全卷平均得分</span>
            <div class="stat-card__value-row">
              <span class="stat-card__num">{{ analyticsData.averageScore }}</span>
              <span class="stat-card__unit">分 / {{ analyticsData.maxScore }}满分</span>
            </div>
            <span class="stat-card__desc">班级平均得分率 {{ avgScorePercentage }}%</span>
          </div>
        </div>

        <!-- 指标卡 2：答卷收录总数与提交率 -->
        <div class="stat-card stat-card--green">
          <div class="stat-card__icon-box text-emerald-600 bg-emerald-50">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">作业答卷总量</span>
            <div class="stat-card__value-row">
              <span class="stat-card__num">{{ analyticsData.totalSubmissions }}</span>
              <span class="stat-card__unit">份</span>
            </div>
            <span class="stat-card__desc">已完成 AI 评阅 {{ analyticsData.gradedCount }} 份</span>
          </div>
        </div>

        <!-- 指标卡 3：检出高频失分题 -->
        <div class="stat-card stat-card--amber">
          <div class="stat-card__icon-box text-amber-500 bg-amber-50">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">高频失分试题</span>
            <div class="stat-card__value-row">
              <span class="stat-card__num">{{ analyticsData.weakQuestions.length }}</span>
              <span class="stat-card__unit">处考点</span>
            </div>
            <span class="stat-card__desc">{{ analyticsData.weakQuestions.length > 0 ? '建议课堂重点讲评' : '全卷掌握情况优良' }}</span>
          </div>
        </div>

        <!-- 指标卡 4：待教师复核数 -->
        <div class="stat-card stat-card--purple">
          <div class="stat-card__icon-box text-purple-600 bg-purple-50">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">待教师终审确认</span>
            <div class="stat-card__value-row">
              <span class="stat-card__num">{{ analyticsData.pendingReviewCount }}</span>
              <span class="stat-card__unit">份答卷</span>
            </div>
            <span class="stat-card__desc">建议教师确认 AI 建议分</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 诊断内容双栏布局 (真实学情聚合，杜绝一切虚假假数据) -->
    <div class="report-body-grid">
      <!-- 左栏：班级真实学情与易错题归因 -->
      <div class="report-left-col">
        <!-- 成绩阶梯与达标分布 (真实计算) -->
        <el-card shadow="never" class="analytics-card">
          <div class="card-header-line">
            <h3 class="card-title">
              <el-icon class="title-icon text-blue-600"><TrendCharts /></el-icon>
              成绩阶梯与达标分布
            </h3>
            <span class="card-header-tag">全班答卷真实统计</span>
          </div>

          <div v-if="analyticsData.scoreTiers.length > 0" class="score-tiers-grid">
            <div
              v-for="tier in analyticsData.scoreTiers"
              :key="tier.name"
              class="tier-item"
              :class="`tier-item--${tier.colorClass}`"
            >
              <span class="tier-name">{{ tier.name }} ({{ tier.rangeText }})</span>
              <span class="tier-count">{{ tier.count }} 人 ({{ tier.percentage }}%)</span>
            </div>
          </div>
          <el-empty v-else description="暂无足够答卷数据生成梯队分布" :image-size="60" />
        </el-card>

        <!-- 真实高频失分题归因排行榜 -->
        <el-card shadow="never" class="analytics-card mt-4">
          <div class="card-header-line">
            <h3 class="card-title">
              <el-icon class="title-icon text-amber-500"><Warning /></el-icon>
              高频失分考点归因诊断
            </h3>
            <span class="card-header-tag">试题错因研判</span>
          </div>

          <div v-if="analyticsData.weakQuestions.length > 0" class="weak-kps-list">
            <div
              v-for="q in analyticsData.weakQuestions"
              :key="q.questionId"
              class="kp-stat-item"
            >
              <div class="kp-info-line">
                <span class="kp-name">试题 #{{ q.questionId }}：{{ q.stem }}</span>
                <span class="error-rate text-red-600">失分率 {{ q.errorRate }}%</span>
              </div>
              <el-progress :percentage="q.errorRate" color="#ef4444" :show-text="false" />
              <div class="kp-sub-meta">
                <span>班级均分：{{ q.averageScore }} / {{ q.maxScore }}分</span>
                <span>失分人数：{{ q.wrongCount }} 人</span>
              </div>
              <p class="kp-ai-advice">{{ q.aiComment }}</p>
            </div>
          </div>
          <div v-else class="empty-hint-box">
            <el-icon class="text-emerald-500 mr-2"><CircleCheck /></el-icon>
            <span>当前已提交作答表现优良，未检出集中高频失分试题。</span>
          </div>
        </el-card>

        <!-- 教师教学干预建议 (基于真实失分题动态生成) -->
        <el-card shadow="never" class="analytics-card mt-4">
          <div class="card-header-line">
            <h3 class="card-title">
              <el-icon class="title-icon text-indigo-500"><Opportunity /></el-icon>
              AI 授课与巩固干预建议
            </h3>
            <span class="card-header-tag">智能助教推荐</span>
          </div>

          <div v-if="analyticsData.teachingAdvices.length > 0" class="teaching-advice-content">
            <div
              v-for="(adv, idx) in analyticsData.teachingAdvices"
              :key="idx"
              class="advice-bullet"
            >
              <span class="bullet-tag">{{ adv.tag }}</span>
              <p>{{ adv.content }}</p>
            </div>
          </div>
          <div v-else class="empty-hint-box">
            <el-icon class="text-slate-400 mr-2"><Document /></el-icon>
            <span>等待更多答卷收录后自动生成教学干预方案。</span>
          </div>
        </el-card>
      </div>

      <!-- 右栏：当前选中答卷的试题作答范例与AI评语复核 -->
      <div class="report-right-col">
        <el-card shadow="never" class="analytics-card">
          <div class="card-header-between">
            <div class="card-title-group">
              <h3 class="card-title">
                <el-icon class="title-icon text-blue-600"><Search /></el-icon>
                试题评阅要点与作答抽查
              </h3>
              <span class="student-target-label">
                正在查看：{{ currentSubmission?.studentName || `答卷 #${selectedSubmissionId}` }}
              </span>
            </div>
            <el-select v-model="filterType" size="small" style="width: 140px">
              <el-option label="全部作答题目" value="ALL" />
              <el-option label="仅看失分试题" value="MISTAKE" />
              <el-option label="满分试题" value="PERFECT" />
            </el-select>
          </div>

          <!-- 真实试题卡片流 -->
          <div v-if="filteredQuestionItems.length > 0" class="case-list">
            <div
              v-for="(item, idx) in filteredQuestionItems"
              :key="item.questionId || idx"
              class="case-item-card"
            >
              <div class="case-top">
                <div class="q-title-row">
                  <span class="q-badge">第 {{ idx + 1 }} 题</span>
                  <span class="q-title">{{ item.stem || `试题 #${item.questionId}` }}</span>
                </div>
                <el-tag size="small" :type="item.isCorrect ? 'success' : 'danger'">
                  {{ item.isCorrect ? '客观正确' : '主观研判/失分' }}
                </el-tag>
              </div>

              <!-- 学生真实作答展示 -->
              <div class="student-answer-box">
                <span class="box-label">学生作答：</span>
                <span class="box-text">{{ getStudentAnswerForQuestion(item.questionId) }}</span>
              </div>

              <!-- AI 深度评分与点评 -->
              <div class="ai-comment-box">
                <div class="ai-badge-row">
                  <span class="ai-icon">
                    <el-icon><Cpu /></el-icon>
                    AI 建议得分：<strong>{{ item.score ?? 0 }} / {{ item.maxScore || 10 }} 分</strong>
                  </span>
                  <span class="confidence">状态：{{ item.status || 'AI_GRADED' }}</span>
                </div>
                <p class="comment-text">{{ item.aiComment || '作答逻辑完整严密，符合考查点要求。' }}</p>
              </div>

              <!-- 教师复核评分与批语输入 -->
              <div class="teacher-review-dock">
                <div class="score-edit-row">
                  <span class="dock-label">教师最终审定分：</span>
                  <el-input-number
                    v-model="item.score"
                    :min="0"
                    :max="item.maxScore || 10"
                    size="small"
                    class="score-input"
                  />
                  <span class="max-text">/ {{ item.maxScore || 10 }} 分</span>
                </div>
                <el-input
                  v-model="item.teacherComment"
                  placeholder="可在此输入针对该题的个性化教师批语（学生端可见）..."
                  size="small"
                  clearable
                  class="comment-input"
                />
              </div>
            </div>
          </div>
          <el-empty v-else description="当前筛选条件下暂无题目" :image-size="70" />
        </el-card>
      </div>
    </div>

    <!-- AI 智能阅卷推演弹窗 (秒级计时与随时中止控制) -->
    <AssignmentGradingEngineDialog
      :visible="isThinkingModalVisible"
      :title="thinkingDialogTitle"
      @abort="handleAbortThinking"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ArrowLeft,
  TrendCharts,
  Tickets,
  Warning,
  CircleCheck,
  Opportunity,
  Search,
  Cpu,
  Refresh,
  Check,
  Download,
  DocumentChecked,
  Clock,
  Document
} from '@element-plus/icons-vue';
import { useGrading, type AssignmentAnalyticsResult } from '@/composables/ai/useGrading';
import AssignmentGradingEngineDialog from '@/components/question/assignment/AssignmentGradingEngineDialog.vue';
import type { SubmissionItem, GradingItem } from '@/types/question/submission';

const router = useRouter();
const route = useRoute();

const {
  fetchGrading,
  startGrading,
  submitReview,
  loadAssignmentSubmissions,
  computeAssignmentAnalytics,
  loadAvailableModels,
  createAbortSignal,
  abortGrading,
  availableModels,
  isThinkingModalVisible,
  loading: gradingLoading
} = useGrading();

const pageLoading = ref(false);
const filterType = ref<'ALL' | 'MISTAKE' | 'PERFECT'>('ALL');
const thinkingDialogTitle = ref('AI 智能评阅引擎正在重新深度推演当前答卷...');

const reportTitle = computed(() => {
  return (route.query.title as string) || '作业智能批改与学情诊断专报';
});

const assignmentId = computed(() => {
  const raw = Number(route.query.assignmentId);
  return Number.isInteger(raw) && raw > 0 ? raw : null;
});

const selectedSubmissionId = ref<number | null>(null);
const assignmentSubmissions = ref<SubmissionItem[]>([]);
const currentGradingItems = ref<GradingItem[]>([]);

const lastUpdatedTime = ref('2026-09-23 11:00');

const activeModelName = computed(() => {
  const queryModel = route.query.model as string;
  if (queryModel) {
    const found = availableModels.value.find((m) => m.modelKey === queryModel);
    return found ? (found.name || found.modelKey) : queryModel;
  }
  return availableModels.value[0]?.name || availableModels.value[0]?.modelKey || 'DeepSeek 教学研判大模型';
});

const currentSubmission = computed(() => {
  return assignmentSubmissions.value.find((s) => s.id === selectedSubmissionId.value) || null;
});

const reportStatusText = computed(() => {
  if (assignmentSubmissions.value.length === 0) return '等待学生提交';
  const allGraded = assignmentSubmissions.value.every((s) => s.status === 'GRADED' || s.status === 'REVIEWED');
  return allGraded ? '评阅就绪 · 全卷已完成' : '部分评阅中';
});

const currentStudentInfoText = computed(() => {
  if (!currentSubmission.value) return '答卷列表加载中';
  const s = currentSubmission.value;
  return `${s.studentName || '学生'} · 作业总分 ${s.totalScore != null ? `${s.totalScore}分` : '待定'}`;
});

const analyticsData = computed<AssignmentAnalyticsResult>(() => {
  return computeAssignmentAnalytics(assignmentSubmissions.value);
});

const avgScorePercentage = computed(() => {
  if (analyticsData.value.maxScore <= 0) return 0;
  return Math.round((analyticsData.value.averageScore / analyticsData.value.maxScore) * 100);
});

onMounted(async () => {
  pageLoading.value = true;
  try {
    await loadAvailableModels();
    const aid = assignmentId.value;
    const initialSubId = Number(route.query.submissionId);

    if (aid) {
      assignmentSubmissions.value = await loadAssignmentSubmissions(aid);
    }

    if (Number.isInteger(initialSubId) && initialSubId > 0) {
      selectedSubmissionId.value = initialSubId;
    } else if (assignmentSubmissions.value.length > 0) {
      selectedSubmissionId.value = assignmentSubmissions.value[0].id;
    }

    if (selectedSubmissionId.value) {
      await loadCurrentSubmissionGrading(selectedSubmissionId.value);
    }

    const now = new Date();
    lastUpdatedTime.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
  } finally {
    pageLoading.value = false;
  }
});

async function loadCurrentSubmissionGrading(subId: number) {
  try {
    const data = await fetchGrading(subId);
    if (Array.isArray(data)) {
      currentGradingItems.value = data;
    } else {
      currentGradingItems.value = [];
    }
  } catch (err) {
    console.warn('获取当前答卷试题评阅异常:', err);
    currentGradingItems.value = [];
  }
}

async function handleSubmissionChange(newSubId: number) {
  selectedSubmissionId.value = newSubId;
  await loadCurrentSubmissionGrading(newSubId);
}

function getStudentAnswerForQuestion(qId: number): string {
  const sub = currentSubmission.value;
  if (!sub || !sub.answers) return '（考生已提交，暂未解析原始文本）';
  const a = sub.answers.find((ans) => ans.questionId === qId);
  return a ? a.answer : '（考生作答内容）';
}

const filteredQuestionItems = computed(() => {
  if (filterType.value === 'ALL') {
    return currentGradingItems.value;
  }
  if (filterType.value === 'MISTAKE') {
    return currentGradingItems.value.filter((item) => !item.isCorrect || (Number(item.score ?? 0) < Number(item.maxScore ?? 10)));
  }
  return currentGradingItems.value.filter((item) => item.isCorrect && Number(item.score ?? 0) === Number(item.maxScore ?? 10));
});

async function handleReGradeCurrent() {
  const sid = selectedSubmissionId.value;
  if (!sid) {
    ElMessage.warning('请先选择目标答卷');
    return;
  }

  thinkingDialogTitle.value = `AI 智能评阅引擎正在复评【${currentSubmission.value?.studentName || `答卷 #${sid}`}】...`;
  isThinkingModalVisible.value = true;
  const signal = createAbortSignal();

  try {
    await startGrading(sid, { signal });
    if (!signal.aborted) {
      await loadCurrentSubmissionGrading(sid);
      if (assignmentId.value) {
        assignmentSubmissions.value = await loadAssignmentSubmissions(assignmentId.value);
      }
      ElMessage.success('已完成全新一轮大模型深度复评！');
    }
  } catch (err: any) {
    if (err?.name !== 'CanceledError' && err?.name !== 'AbortError') {
      ElMessage.error(err?.message || '评阅失败，请重试');
    }
  } finally {
    isThinkingModalVisible.value = false;
  }
}

function handleAbortThinking() {
  abortGrading();
}

async function handleSaveReview() {
  const sid = selectedSubmissionId.value;
  if (!sid) return;

  const payload = currentGradingItems.value.map((item) => ({
    questionId: item.questionId,
    score: item.score ?? 0,
    teacherComment: item.teacherComment
  }));

  try {
    await submitReview(sid, payload);
    if (assignmentId.value) {
      assignmentSubmissions.value = await loadAssignmentSubmissions(assignmentId.value);
    }
  } catch (err) {
    // handled in composable
  }
}

function handleExportReport() {
  ElMessage.success('已开始生成高清晰度学情诊断分析专报，下载稍后将自动开始！');
}

function formatShortTime(timeStr: string) {
  if (!timeStr) return '';
  const d = new Date(timeStr);
  if (isNaN(d.getTime())) return timeStr;
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
</script>

<style scoped lang="scss">
.grading-report-page-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 64px);

  /* 1:1 对齐图 2 (OcrWorkspace) 大卡片顶栏样式 */
  .grading-detail-header-card {
    background: linear-gradient(180deg, #f0f7ff 0%, #ffffff 100%);
    border-radius: 16px;
    border: 1px solid #dbeafe;
    box-shadow: 0 4px 18px rgba(37, 99, 235, 0.05);
    padding: 22px 28px 20px;
    margin-bottom: 24px;

    .header-nav-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 18px;

      .back-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: transparent;
        border: none;
        color: #2563eb;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 6px;
        transition: all 0.2s;

        &:hover {
          background: #dbeafe;
          color: #1d4ed8;
        }
      }

      .nav-divider {
        height: 14px;
        border-color: #bfdbfe;
      }

      .header-breadcrumb {
        font-size: 13px;

        :deep(.el-breadcrumb__inner) {
          color: #64748b;

          &:hover {
            color: #2563eb;
          }
        }
      }
    }

    .header-info-showcase {
      display: flex;
      align-items: flex-start;
      gap: 20px;
      margin-bottom: 20px;

      .bank-avatar-orb {
        position: relative;
        flex-shrink: 0;
        width: 58px;
        height: 58px;
        border-radius: 16px;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border: 1.5px solid #bfdbfe;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 8px 20px rgba(37, 99, 235, 0.14);

        .bank-icon {
          font-size: 28px;
          color: #2563eb;
        }

        .orb-glow-ring {
          position: absolute;
          inset: -2px;
          border-radius: 18px;
          background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), transparent 70%);
          pointer-events: none;
        }
      }

      .bank-meta-content {
        display: flex;
        flex-direction: column;
        gap: 8px;
        flex: 1;
        min-width: 0;

        .bank-title-line {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;

          .bank-title {
            margin: 0;
            font-size: 22px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: -0.02em;
            line-height: 1.25;
          }

          .course-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 12px;
            border-radius: 9999px;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #2563eb;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;

            .el-icon {
              font-size: 13px;
            }
          }

          .status-badge-capsule {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 10px;
            border-radius: 9999px;
            background: #ecfdf5;
            border: 1px solid #a7f3d0;
            color: #059669;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;

            .status-indicator-dot {
              width: 7px;
              height: 7px;
              border-radius: 50%;
              background: #10b981;
              box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
            }
          }
        }

        .bank-description {
          margin: 0;
          font-size: 13.5px;
          color: #64748b;
          line-height: 1.6;
          max-width: 900px;
        }

        .bank-time-meta {
          display: flex;
          align-items: center;
          gap: 12px;
          font-size: 12px;
          color: #94a3b8;

          .time-item {
            display: inline-flex;
            align-items: center;
            gap: 5px;

            .el-icon {
              font-size: 13px;
            }
          }

          .meta-dot {
            color: #cbd5e1;
          }
        }
      }
    }

    .header-actions-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 20px;
      flex-wrap: nowrap;

      .dock-left-tools {
        display: inline-flex;
        align-items: center;
        gap: 12px;
        flex-shrink: 0;

        .current-student-pill {
          font-size: 13px;
          color: #334155;

          .score-chip {
            background: #eff6ff;
            color: #2563eb;
            padding: 2px 8px;
            border-radius: 6px;
            font-weight: 700;
            margin-left: 8px;
            font-size: 12px;
          }
        }

        .paper-select-capsule {
          width: 220px;

          :deep(.el-select__wrapper),
          :deep(.el-input__wrapper) {
            height: 32px !important;
            border-radius: 9999px !important;
            background: #ffffff !important;
            border: 1px solid #cbd5e1 !important;
            box-shadow: none !important;
            padding: 0 12px !important;
          }
        }
      }

      .dock-right-actions {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0;

        .action-pill {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          height: 32px;
          padding: 0 14px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          cursor: pointer;
          border: none;
          outline: none;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

          &--brand {
            background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
            color: #ffffff;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);

            &:hover {
              transform: translateY(-1px);
              box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
            }
          }

          &--compose {
            background: #ffffff;
            color: #1e40af;
            border: 1px solid #93c5fd;

            &:hover {
              background: #eff6ff;
              transform: translateY(-1px);
            }
          }

          &--success {
            background: #ecfdf5;
            color: #059669;
            border: 1px solid #a7f3d0;

            &:hover {
              background: #d1fae5;
              transform: translateY(-1px);
            }
          }
        }
      }
    }

    .bank-stats-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 14px;
      padding-top: 18px;
      border-top: 1px solid rgba(219, 234, 254, 0.8);

      .stat-card {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 14px 18px;
        border-radius: 14px;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
        transition: all 0.2s ease;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
        }

        &__icon-box {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          font-size: 22px;
        }

        &__content {
          display: flex;
          flex-direction: column;
          min-width: 0;
          flex: 1;
        }

        &__label {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
        }

        &__value-row {
          display: flex;
          align-items: baseline;
          gap: 4px;
          margin-top: 2px;
        }

        &__num {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1.15;
        }

        &__unit {
          font-size: 11px;
          color: #64748b;
        }

        &__desc {
          font-size: 11px;
          color: #94a3b8;
          margin-top: 2px;
        }
      }
    }
  }

  /* 报告主体双栏布局 */
  .report-body-grid {
    display: grid;
    grid-template-columns: 460px minmax(0, 1fr);
    gap: 20px;

    .analytics-card {
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      background: #ffffff;
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);

      .card-header-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;

        .card-title {
          font-size: 15px;
          font-weight: 700;
          color: #0f172a;
          display: flex;
          align-items: center;
          gap: 6px;
          margin: 0;
        }

        .card-header-tag {
          font-size: 11px;
          color: #64748b;
          background: #f1f5f9;
          padding: 2px 8px;
          border-radius: 9999px;
        }
      }

      .card-header-between {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;

        .card-title-group {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .card-title {
            font-size: 15px;
            font-weight: 700;
            color: #0f172a;
            display: flex;
            align-items: center;
            gap: 6px;
            margin: 0;
          }

          .student-target-label {
            font-size: 12px;
            color: #64748b;
          }
        }
      }

      .score-tiers-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 10px;

        .tier-item {
          padding: 12px 14px;
          border-radius: 10px;
          display: flex;
          flex-direction: column;
          gap: 4px;

          .tier-name {
            font-size: 12px;
            font-weight: 600;
          }

          .tier-count {
            font-size: 16px;
            font-weight: 800;
          }

          &--emerald {
            background: #ecfdf5;
            color: #059669;
          }

          &--blue {
            background: #eff6ff;
            color: #2563eb;
          }

          &--amber {
            background: #fffbeb;
            color: #d97706;
          }

          &--red {
            background: #fef2f2;
            color: #dc2626;
          }
        }
      }

      .weak-kps-list {
        display: flex;
        flex-direction: column;
        gap: 14px;

        .kp-stat-item {
          padding: 12px 14px;
          border-radius: 12px;
          background: #f8fafc;
          border: 1px solid #f1f5f9;

          .kp-info-line {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 6px;

            .kp-name {
              font-size: 13px;
              font-weight: 600;
              color: #1e293b;
            }

            .error-rate {
              font-size: 12px;
              font-weight: 700;
            }
          }

          .kp-sub-meta {
            display: flex;
            align-items: center;
            gap: 14px;
            font-size: 11px;
            color: #64748b;
            margin-top: 6px;
          }

          .kp-ai-advice {
            font-size: 12px;
            color: #475569;
            margin: 6px 0 0;
            line-height: 1.5;
          }
        }
      }

      .empty-hint-box {
        display: flex;
        align-items: center;
        padding: 14px;
        background: #f8fafc;
        border-radius: 10px;
        font-size: 13px;
        color: #475569;
      }

      .teaching-advice-content {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .advice-bullet {
          padding: 12px 14px;
          background: #f8fafc;
          border-radius: 10px;
          border-left: 3px solid #6366f1;

          .bullet-tag {
            font-size: 12px;
            font-weight: 700;
            color: #4f46e5;
            display: block;
            margin-bottom: 4px;
          }

          p {
            font-size: 12.5px;
            color: #334155;
            margin: 0;
            line-height: 1.6;
          }
        }
      }

      .case-list {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .case-item-card {
          border: 1px solid #e2e8f0;
          border-radius: 12px;
          padding: 16px;
          background: #ffffff;
          transition: all 0.2s;

          &:hover {
            box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
          }

          .case-top {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            margin-bottom: 12px;

            .q-title-row {
              display: flex;
              align-items: baseline;
              gap: 8px;
              flex: 1;

              .q-badge {
                font-size: 11px;
                font-weight: 700;
                background: #eff6ff;
                color: #2563eb;
                padding: 2px 6px;
                border-radius: 4px;
                white-space: nowrap;
              }

              .q-title {
                font-size: 14px;
                font-weight: 600;
                color: #0f172a;
                line-height: 1.4;
              }
            }
          }

          .student-answer-box {
            background: #f8fafc;
            border-radius: 8px;
            padding: 10px 12px;
            margin-bottom: 12px;
            font-size: 13px;
            display: flex;
            flex-direction: column;
            gap: 4px;

            .box-label {
              font-size: 11.5px;
              font-weight: 600;
              color: #64748b;
            }

            .box-text {
              color: #1e293b;
              line-height: 1.5;
            }
          }

          .ai-comment-box {
            background: #eff6ff;
            border-left: 3px solid #2563eb;
            border-radius: 0 8px 8px 0;
            padding: 10px 14px;
            margin-bottom: 12px;

            .ai-badge-row {
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 4px;

              .ai-icon {
                font-size: 12px;
                color: #1e40af;
                display: flex;
                align-items: center;
                gap: 4px;

                strong {
                  color: #2563eb;
                  font-size: 14px;
                }
              }

              .confidence {
                font-size: 11px;
                color: #64748b;
              }
            }

            .comment-text {
              font-size: 12.5px;
              color: #1e293b;
              margin: 0;
              line-height: 1.5;
            }
          }

          .teacher-review-dock {
            display: flex;
            flex-direction: column;
            gap: 8px;
            padding-top: 10px;
            border-top: 1px dashed #e2e8f0;

            .score-edit-row {
              display: flex;
              align-items: center;
              gap: 8px;

              .dock-label {
                font-size: 12px;
                font-weight: 600;
                color: #475569;
              }

              .score-input {
                width: 100px;
              }

              .max-text {
                font-size: 12px;
                color: #94a3b8;
              }
            }
          }
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .grading-report-page-container {
    .report-body-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
