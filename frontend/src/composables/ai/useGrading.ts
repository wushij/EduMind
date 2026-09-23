import { ref } from 'vue';
import { getGradingResult, triggerGrading, reviewGrading } from '@/api/ai/grading';
import { getAssignments } from '@/api/question/assignment';
import {
  getSubmissionsByAssignment,
  getSubmissionStats,
  batchGradeSubmissions
} from '@/api/question/submission';
import { getChatModelsRaw, type ChatModelVO } from '@/api/ai/chat';
import type { SubmissionItem, GradingItem } from '@/types/question/submission';
import { ElMessage } from 'element-plus';

export interface GradingRequestOptions {
  signal?: AbortSignal;
  silent?: boolean;
}


export interface GradingTaskRow {
  id: number;
  title: string;
  courseName: string;
  submissionCount: number;
  progress: number;
  status: 'DONE' | 'RUNNING' | 'PENDING';
  running: boolean;
  submissions?: SubmissionItem[];
}

export interface ScoreTier {
  name: string;
  rangeText: string;
  count: number;
  percentage: number;
  colorClass: string;
}

export interface WeakQuestionStat {
  questionId: number;
  stem: string;
  errorRate: number;
  averageScore: number;
  maxScore: number;
  wrongCount: number;
  aiComment?: string;
}

export interface AssignmentAnalyticsResult {
  totalSubmissions: number;
  gradedCount: number;
  pendingReviewCount: number;
  averageScore: number;
  maxScore: number;
  scoreTiers: ScoreTier[];
  weakQuestions: WeakQuestionStat[];
  teachingAdvices: Array<{ tag: string; content: string }>;
}

export function useGrading() {
  const gradingResult = ref<any>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 真实大模型列表与选择（杜绝硬编码假模型）
  const availableModels = ref<ChatModelVO[]>([]);
  const selectedModel = ref<string>('');
  const loadingModels = ref(false);

  // 顶栏概览统计
  const overviewStats = ref({
    submittedCount: 0,
    gradedCount: 0,
    reviewedCount: 0
  });

  // 中止控制器与推演弹窗状态
  const isThinkingModalVisible = ref(false);
  let activeAbortController: AbortController | null = null;

  async function loadAvailableModels(): Promise<ChatModelVO[]> {
    loadingModels.value = true;
    try {
      const res = await getChatModelsRaw();
      const list = Array.isArray(res.data) ? res.data : [];
      // 过滤出已启用的模型
      const enabled = list.filter((m) => m.enabled !== false);
      availableModels.value = enabled.length > 0 ? enabled : list;

      if (availableModels.value.length > 0) {
        // 优先选中标记为默认的模型，否则取第一个
        const defaultM = availableModels.value.find((m) => m.isDefault);
        selectedModel.value = defaultM ? defaultM.modelKey : availableModels.value[0].modelKey;
      } else {
        selectedModel.value = '';
      }
      return availableModels.value;
    } catch (err) {
      console.warn('获取真实模型列表异常:', err);
      availableModels.value = [];
      selectedModel.value = '';
      return [];
    } finally {
      loadingModels.value = false;
    }
  }

  async function loadOverviewStats() {
    try {
      const res = await getSubmissionStats();
      overviewStats.value = {
        submittedCount: res.data?.submittedCount ?? 0,
        gradedCount: res.data?.gradedCount ?? 0,
        reviewedCount: res.data?.reviewedCount ?? 0
      };
      return overviewStats.value;
    } catch {
      overviewStats.value = { submittedCount: 0, gradedCount: 0, reviewedCount: 0 };
      return overviewStats.value;
    }
  }

  function createAbortSignal(): AbortSignal {
    if (activeAbortController) {
      activeAbortController.abort();
    }
    activeAbortController = new AbortController();
    return activeAbortController.signal;
  }

  function abortGrading() {
    if (activeAbortController) {
      activeAbortController.abort();
      activeAbortController = null;
    }
    isThinkingModalVisible.value = false;
    loading.value = false;
    ElMessage.info('已中止本次 AI 批改推演');
  }

  async function fetchGrading(submissionId: number, options?: GradingRequestOptions) {
    loading.value = true;
    error.value = null;
    try {
      const res = await getGradingResult(submissionId, options);
      gradingResult.value = res.data;
      return res.data;
    } catch (err: any) {
      error.value = err?.message || '获取评阅结果失败';
      console.warn('获取评阅结果异常:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function startGrading(submissionId: number, options?: GradingRequestOptions) {
    loading.value = true;
    error.value = null;
    try {
      const res = await triggerGrading(submissionId, options);
      gradingResult.value = res.data;
      return res.data;
    } catch (err: any) {
      if (err?.name === 'CanceledError' || err?.name === 'AbortError') {
        return null;
      }
      error.value = err?.message || '触发评阅失败';
      ElMessage.error(err?.message || '触发 AI 智能评阅失败，请检查后端 AI 服务状态');
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function submitReview(
    submissionId: number,
    items: Array<{ questionId: number; score: number; teacherComment?: string }>,
    options?: GradingRequestOptions
  ) {
    loading.value = true;
    error.value = null;
    try {
      await reviewGrading(submissionId, items, options);
      ElMessage.success('成绩复核已成功提交！');
      await fetchGrading(submissionId, options);
    } catch (err: any) {
      error.value = err?.message || '提交复核失败';
      ElMessage.error(err?.message || '成绩复核提交失败，请重试');
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function loadGradingTasks(): Promise<GradingTaskRow[]> {
    loading.value = true;
    try {
      const res = await getAssignments({ page: 1, pageSize: 50 });
      const list = res.data?.list || [];
      const taskRows: GradingTaskRow[] = [];
      for (const a of list) {
        let subs: SubmissionItem[] = [];
        try {
          const sRes = await getSubmissionsByAssignment(a.id);
          subs = sRes.data || [];
        } catch {
          subs = [];
        }
        const total = subs.length || a.submissionCount || 0;
        const graded = subs.filter(
          (s) => s.status === 'GRADED' || s.status === 'REVIEWED' || s.status === 'AI_GRADED'
        ).length;
        const prog = total > 0 ? Math.round((graded / total) * 100) : a.status === 'GRADED' ? 100 : 0;

        taskRows.push({
          id: a.id,
          title: a.title,
          courseName: a.courseName || '通识核心课程',
          submissionCount: total,
          progress: prog,
          status: prog === 100 ? 'DONE' : 'PENDING',
          running: false,
          submissions: subs
        });
      }
      return taskRows;
    } finally {
      loading.value = false;
    }
  }

  async function batchGrade(submissionIds: number[], options?: { signal?: AbortSignal }) {
    loading.value = true;
    try {
      const res = await batchGradeSubmissions(
        { submissionIds, forceRegrade: true },
        { signal: options?.signal }
      );
      return res.data?.successCount ?? submissionIds.length;
    } catch (err: any) {
      if (err?.name === 'CanceledError' || err?.name === 'AbortError') {
        return 0;
      }
      // 备用兜底循环调用
      let successCount = 0;
      for (const sid of submissionIds) {
        if (options?.signal?.aborted) break;
        try {
          await triggerGrading(sid, { signal: options?.signal, silent: true });
          successCount++;
        } catch (e: any) {
          if (e?.name === 'CanceledError' || e?.name === 'AbortError') break;
        }
      }
      return successCount;
    } finally {
      loading.value = false;
    }
  }

  async function loadAssignmentSubmissions(assignmentId: number): Promise<SubmissionItem[]> {
    try {
      const res = await getSubmissionsByAssignment(assignmentId);
      return res.data || [];
    } catch (err) {
      console.warn('获取作业全部答卷异常:', err);
      return [];
    }
  }

  /**
   * 基于真实学生答卷动态聚合计算学情画像与题型失分，杜绝写死假数据
   */
  function computeAssignmentAnalytics(submissions: SubmissionItem[]): AssignmentAnalyticsResult {
    const total = submissions.length;
    if (total === 0) {
      return {
        totalSubmissions: 0,
        gradedCount: 0,
        pendingReviewCount: 0,
        averageScore: 0,
        maxScore: 100,
        scoreTiers: [],
        weakQuestions: [],
        teachingAdvices: []
      };
    }

    const gradedSubs = submissions.filter(
      (s) => s.status === 'GRADED' || s.status === 'REVIEWED' || s.status === 'AI_GRADED' || (s.finalScore != null || s.totalScore != null || s.aiScore != null)
    );
    const pendingReviewCount = submissions.filter((s) => s.status === 'SUBMITTED' || s.status === 'PENDING_REVIEW').length;

    let scoreSum = 0;
    let maxAssignmentScore = 100;

    // 统计分数阶梯
    let excellentCount = 0; // >= 90%
    let goodCount = 0;      // 80% - 89%
    let passCount = 0;      // 60% - 79%
    let failCount = 0;      // < 60%

    // 试题级聚合字典
    const questionStatsMap = new Map<number, {
      questionId: number;
      stem: string;
      totalScoreEarned: number;
      maxScore: number;
      wrongCount: number;
      sampleAiComment?: string;
    }>();

    for (const sub of gradedSubs) {
      const sScore = Number(sub.totalScore ?? sub.finalScore ?? sub.aiScore ?? 0);
      const mScore = Number(sub.maxScore ?? 100);
      if (mScore > 0) {
        maxAssignmentScore = mScore;
      }
      scoreSum += sScore;

      const ratio = mScore > 0 ? (sScore / mScore) * 100 : 0;
      if (ratio >= 90) excellentCount++;
      else if (ratio >= 80) goodCount++;
      else if (ratio >= 60) passCount++;
      else failCount++;

      // 聚合答卷内的每道试题
      const items = sub.gradingItems || [];
      for (const item of items) {
        const qId = item.questionId;
        if (!qId) continue;
        const qScore = Number(item.score ?? 0);
        const qMax = Number(item.maxScore ?? 10);
        const isWrong = !item.isCorrect || qScore < qMax;

        const existing = questionStatsMap.get(qId) || {
          questionId: qId,
          stem: item.stem || `试题 #${qId}`,
          totalScoreEarned: 0,
          maxScore: qMax,
          wrongCount: 0,
          sampleAiComment: item.aiComment
        };

        existing.totalScoreEarned += qScore;
        if (isWrong) {
          existing.wrongCount++;
          if (item.aiComment) {
            existing.sampleAiComment = item.aiComment;
          }
        }
        questionStatsMap.set(qId, existing);
      }
    }

    const gradedCount = gradedSubs.length;
    const avgScore = gradedCount > 0 ? Math.round((scoreSum / gradedCount) * 10) / 10 : 0;

    const calcPct = (cnt: number) => (gradedCount > 0 ? Math.round((cnt / gradedCount) * 100) : 0);

    const scoreTiers: ScoreTier[] = [
      {
        name: '优秀',
        rangeText: `90-${maxAssignmentScore}分`,
        count: excellentCount,
        percentage: calcPct(excellentCount),
        colorClass: 'emerald'
      },
      {
        name: '良好',
        rangeText: '80-89分',
        count: goodCount,
        percentage: calcPct(goodCount),
        colorClass: 'blue'
      },
      {
        name: '及格',
        rangeText: '60-79分',
        count: passCount,
        percentage: calcPct(passCount),
        colorClass: 'amber'
      },
      {
        name: '不及格',
        rangeText: '<60分',
        count: failCount,
        percentage: calcPct(failCount),
        colorClass: 'red'
      }
    ];

    // 计算失分率前 5 题
    const weakQuestions: WeakQuestionStat[] = Array.from(questionStatsMap.values())
      .map((q) => {
        const errorRate = gradedCount > 0 ? Math.round((q.wrongCount / gradedCount) * 100) : 0;
        const avg = gradedCount > 0 ? Math.round((q.totalScoreEarned / gradedCount) * 10) / 10 : 0;
        return {
          questionId: q.questionId,
          stem: q.stem,
          errorRate,
          averageScore: avg,
          maxScore: q.maxScore,
          wrongCount: q.wrongCount,
          aiComment: q.sampleAiComment || '作答存在薄弱环节，建议巩固考查要点。'
        };
      })
      .filter((q) => q.errorRate > 0)
      .sort((a, b) => b.errorRate - a.errorRate)
      .slice(0, 5);

    // 动态生成干预建议（基于真实失分题）
    const teachingAdvices: Array<{ tag: string; content: string }> = [];
    if (weakQuestions.length > 0) {
      const topWeak = weakQuestions[0];
      teachingAdvices.push({
        tag: '讲义重点复盘',
        content: `全班在【试题 #${topWeak.questionId}】失分率达 ${topWeak.errorRate}%，平均得分仅 ${topWeak.averageScore}/${topWeak.maxScore}分。建议习题课重点围绕该题考点（${topWeak.stem.slice(0, 32)}…）进行靶向解析。`
      });
      if (weakQuestions.length > 1) {
        const secondWeak = weakQuestions[1];
        teachingAdvices.push({
          tag: '同构强化训练',
          content: `针对【试题 #${secondWeak.questionId}】的高频失分，系统已建立针对性巩固机制，可结合 AI 命题助手一键生成同构变式题进行靶向随堂测验。`
        });
      }
    } else if (gradedCount > 0) {
      teachingAdvices.push({
        tag: '学情整体良好',
        content: `当前已评阅的 ${gradedCount} 份答卷整体掌握度较好，平均得分 ${avgScore} 分，未出现集中显著高频失分试题，可按原定教学计划有序推进。`
      });
    }

    return {
      totalSubmissions: total,
      gradedCount,
      pendingReviewCount,
      averageScore: avgScore,
      maxScore: maxAssignmentScore,
      scoreTiers,
      weakQuestions,
      teachingAdvices
    };
  }

  return {
    gradingResult,
    loading,
    error,
    availableModels,
    selectedModel,
    loadingModels,
    overviewStats,
    isThinkingModalVisible,
    loadAvailableModels,
    loadOverviewStats,
    createAbortSignal,
    abortGrading,
    fetchGrading,
    startGrading,
    submitReview,
    loadGradingTasks,
    batchGrade,
    loadAssignmentSubmissions,
    computeAssignmentAnalytics
  };
}
