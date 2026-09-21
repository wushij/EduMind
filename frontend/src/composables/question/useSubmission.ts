import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getSubmissionDetail,
  getSubmissionGrading,
  gradeSubmission,
  reviewGrading,
  getSubmissionsPage,
  getSubmissionStats,
  batchGradeSubmissions,
  deleteSubmission
} from '@/api/question/submission';
import { getCourseList } from '@/api/course/course';
import { normalizeCourseListFromApi } from '@/utils/course/course-display';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import type { SubmissionItem, SubmissionOverviewStats } from '@/types/question/submission';
import type { Course } from '@/types/course/course';
import type { QuestionType } from '@/types/question/question';

export interface GradingItem {
  questionId: number;
  type: QuestionType;
  stem: string;
  maxScore: number;
  studentAnswer: string;
  standardAnswer: string;
  analysis: string;
  isObjective: boolean;
  isCorrect: boolean;
  aiScore: number;
  aiComment: string;
  teacherScore: number;
  teacherComment: string;
}

/** 仅接受合法正整数 ID；非法（含 NaN / 0 / 负数 / undefined 字面量）一律返回 null */
export function parsePositiveId(raw: unknown): number | null {
  const value = Number(raw);
  return Number.isInteger(value) && value > 0 ? value : null;
}

export function buildGradingItems(sub: any, gList: any[]): GradingItem[] {
  if (!Array.isArray(gList) || gList.length === 0) {
    return [];
  }
  const answersMap = new Map<number, string>();
  if (sub?.answers && Array.isArray(sub.answers)) {
    sub.answers.forEach((ans: any) => {
      const qid = parsePositiveId(ans.questionId);
      if (qid !== null) {
        answersMap.set(qid, ans.answer);
      }
    });
  }

  return gList.map((g: any, idx: number) => {
    // questionId 必须来自后端真实值，缺失时不能退化成行号，否则保存时会出现「题目批改记录不存在」
    const qid = parsePositiveId(g.questionId) ?? 0;
    const stuAns = answersMap.get(qid) || '';
    const isObj = g.isCorrect !== undefined && g.isCorrect !== null;
    const type = g.type || (isObj ? 'SINGLE_CHOICE' : 'SHORT_ANSWER');

    return {
      questionId: qid,
      type,
      stem: g.stem || `试题 #${qid || idx + 1}`,
      maxScore: g.maxScore ?? 10,
      studentAnswer: stuAns || '（未作答）',
      standardAnswer: g.standardAnswer || '—',
      analysis: g.analysis || '—',
      isObjective: isObj,
      isCorrect: g.isCorrect ?? (g.score === g.maxScore),
      aiScore: g.score !== undefined && g.score !== null ? g.score : 0,
      aiComment: g.aiComment || '',
      teacherScore: g.score !== undefined && g.score !== null ? g.score : 0,
      teacherComment: g.teacherComment || ''
    };
  });
}

export function calculateGradingTotalScore(items: GradingItem[]): number {
  return items.reduce((acc, item) => acc + (item.teacherScore || 0), 0);
}

export function getSubmissionTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '综合简答题'
  };
  return map[type] || type || '题目';
}

export function getSubmissionTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as string) || '';
}

export function useSubmission() {
  const route = useRoute();
  const router = useRouter();

  /**
   * 答卷 ID 来自路由参数，必须是合法正整数。
   * 这里不再使用 `|| 201` 之类的兜底值：一旦路由参数丢失，兜底值会把「页面参数异常」
   * 伪装成后端「提交记录不存在」，导致 500 报错难以定位。
   */
  const submissionId = computed(() => parsePositiveId(route.params.id));
  const loading = ref(false);
  const saving = ref(false);
  const gradingInProgress = ref(false);

  const submissionData = ref<any>(null);
  const gradingItems = ref<GradingItem[]>([]);

  const calculatedTotalScore = computed(() => calculateGradingTotalScore(gradingItems.value));

  /** 统一获取可用答卷 ID：非法时提示并中止，不发注定失败的请求 */
  function requireSubmissionId(): number | null {
    const id = submissionId.value;
    if (id === null) {
      ElMessage.error('答卷参数缺失或非法，请从答卷列表重新进入该答卷');
      return null;
    }
    return id;
  }

  async function loadSubmissionData() {
    const id = requireSubmissionId();
    if (id === null) {
      submissionData.value = null;
      gradingItems.value = [];
      return;
    }

    loading.value = true;
    try {
      const res = await getSubmissionDetail(id, { silent: true });
      submissionData.value = res.data;
      if (!submissionData.value) {
        ElMessage.error('未找到该答卷详情');
        return;
      }

      let gradingResults: any[] = [];
      try {
        const gRes = await getSubmissionGrading(id, { silent: true });
        gradingResults = gRes.data || [];
      } catch {
        gradingResults = submissionData.value?.gradingItems || [];
      }

      gradingItems.value = buildGradingItems(submissionData.value, gradingResults);
    } catch (err: unknown) {
      submissionData.value = null;
      gradingItems.value = [];
      ElMessage.error(resolveApiErrorMessage(err, '加载答卷详情失败'));
    } finally {
      loading.value = false;
    }
  }

  const aiThinkingVisible = ref(false);
  const aiThinkingTitle = ref('AI 智能阅卷推演引擎');
  let submissionAbortController: AbortController | null = null;

  async function handleTriggerGradeNow() {
    const id = requireSubmissionId();
    if (id === null) {
      return;
    }

    aiThinkingTitle.value = `AI 智能阅卷 · ${submissionData.value?.studentName || '单份答卷'}`;
    submissionAbortController = new AbortController();
    aiThinkingVisible.value = true;
    gradingInProgress.value = true;
    try {
      if (submissionAbortController.signal.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }
      await gradeSubmission(id, { signal: submissionAbortController.signal, silent: true });
      aiThinkingVisible.value = false;
      ElMessage.success('已成功触发该答卷的 AI 智能分析与评分！');
      await loadSubmissionData();
    } catch (err: unknown) {
      aiThinkingVisible.value = false;
      if (err instanceof DOMException && err.name === 'AbortError') {
        ElMessage.info('已中止本次 AI 阅卷推演');
        return;
      }
      ElMessage.error(resolveApiErrorMessage(err, '触发 AI 评阅失败，请稍后重试'));
    } finally {
      aiThinkingVisible.value = false;
      gradingInProgress.value = false;
      submissionAbortController = null;
    }
  }

  function handleAbortSubmissionGrade() {
    if (submissionAbortController) {
      submissionAbortController.abort();
    }
    aiThinkingVisible.value = false;
  }

  function adoptSingleAIScore(item: GradingItem) {
    item.teacherScore = item.aiScore;
    if (!item.teacherComment && item.aiComment) {
      item.teacherComment = item.aiComment;
    }
    ElMessage.success(`已采纳本题 AI 建议得分：${item.aiScore} 分`);
  }

  function adoptAllAIScores() {
    gradingItems.value.forEach(item => {
      item.teacherScore = item.aiScore;
      if (!item.teacherComment) {
        item.teacherComment = item.aiComment;
      }
    });
    ElMessage.success('已将所有试题的得分一键同步为 AI 建议评分！');
  }

  async function handleSaveGrading() {
    const id = requireSubmissionId();
    if (id === null) {
      return;
    }
    // 明细为空时提交会把后端 totalScore 直接清零，必须拦截
    if (gradingItems.value.length === 0) {
      ElMessage.warning('当前没有逐题评阅明细，请先执行 AI 智能批改或确认答卷明细已加载完成');
      return;
    }
    const invalidItem = gradingItems.value.find(item => parsePositiveId(item.questionId) === null);
    if (invalidItem) {
      ElMessage.error('评阅明细缺少有效题目标识，请重新执行 AI 智能批改后再保存');
      return;
    }

    saving.value = true;
    try {
      const payload = gradingItems.value.map(item => ({
        questionId: item.questionId,
        score: item.teacherScore,
        teacherComment: item.teacherComment || ''
      }));

      await reviewGrading(id, payload, { silent: true });
      if (submissionData.value) {
        submissionData.value.status = 'REVIEWED';
      }
      ElMessage.success('评阅成绩已正式确认并发布！总成绩：' + calculatedTotalScore.value + ' 分');
      setTimeout(() => {
        router.push('/question/submissions');
      }, 800);
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, '评阅结果保存失败，请稍后重试'));
    } finally {
      saving.value = false;
    }
  }

  onMounted(async () => {
    await loadSubmissionData();
  });

  return {
    router,
    submissionId,
    loading,
    saving,
    gradingInProgress,
    submissionData,
    gradingItems,
    calculatedTotalScore,
    aiThinkingVisible,
    aiThinkingTitle,
    loadSubmissionData,
    handleTriggerGradeNow,
    handleAbortSubmissionGrade,
    adoptSingleAIScore,
    adoptAllAIScores,
    handleSaveGrading,
    getTypeLabel: getSubmissionTypeLabel,
    getTypeTagType: getSubmissionTypeTagType
  };
}

export function useSubmissionList() {
  const loading = ref(false);
  const batchLoading = ref(false);
  const courses = ref<Course[]>([]);
  const allSubmissions = ref<SubmissionItem[]>([]);
  const total = ref(0);
  const stats = ref<SubmissionOverviewStats>({});

  const aiThinkingVisible = ref(false);
  const aiThinkingTitle = ref('AI 智能阅卷引擎 · 全队列批改');
  let submissionAbortController: AbortController | null = null;

  function handleAbortSubmissionGrade() {
    if (submissionAbortController) {
      submissionAbortController.abort();
    }
    aiThinkingVisible.value = false;
  }

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = res.data?.list || [];
      courses.value = normalizeCourseListFromApi(list as unknown as Record<string, unknown>[]) as unknown as Course[];
    } catch {
      courses.value = [];
    }
    return courses.value;
  }

  async function fetchAllSubmissions(params: {
    courseId?: number | null;
    status?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
  } = {}) {
    loading.value = true;
    try {
      const [pageRes, statsRes] = await Promise.all([
        getSubmissionsPage({
          courseId: params.courseId ?? undefined,
          status: params.status || undefined,
          keyword: params.keyword?.trim() || undefined,
          page: params.page ?? 1,
          pageSize: params.pageSize ?? 10
        }),
        getSubmissionStats({
          courseId: params.courseId ?? undefined
        })
      ]);
      const list = pageRes.data?.list || [];
      allSubmissions.value = list.map((sub) => ({
        ...sub,
        aiScore: sub.totalScore,
        finalScore: sub.status === 'REVIEWED' ? sub.totalScore : undefined
      }));
      // 后端分页 total 以字符串 Long 返回，需归一化为数字，避免分页组件判定 total 缺失
      const apiTotal = Number(pageRes.data?.total);
      total.value = Number.isFinite(apiTotal) && apiTotal >= 0 ? apiTotal : list.length;
      stats.value = statsRes.data || {};
      return allSubmissions.value;
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, '获取提交列表失败'));
      allSubmissions.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function batchGradePending(courseId?: number | null, forceRegrade?: boolean) {
    aiThinkingTitle.value = forceRegrade ? 'AI 智能阅卷引擎 · 全队列重新批改' : 'AI 智能阅卷引擎 · 全队列批改';
    submissionAbortController = new AbortController();
    aiThinkingVisible.value = true;
    batchLoading.value = true;
    try {
      if (submissionAbortController.signal.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }
      const res = await batchGradeSubmissions(
        {
          courseId: courseId ?? undefined,
          forceRegrade: !!forceRegrade
        },
        { signal: submissionAbortController.signal, silent: true }
      );
      aiThinkingVisible.value = false;
      const successCount = res.data?.successCount ?? 0;
      if (successCount > 0) {
        ElMessage.success(`全队列批改完成，共成功处理 ${successCount} 份答卷`);
      } else {
        ElMessage.info('当前暂无可批改的答卷');
      }
      return successCount;
    } catch (err: unknown) {
      aiThinkingVisible.value = false;
      if (err instanceof DOMException && err.name === 'AbortError') {
        ElMessage.info('已中止本次全队列 AI 批改推演');
        return 0;
      }
      ElMessage.error(resolveApiErrorMessage(err, '批量批改失败'));
      return 0;
    } finally {
      aiThinkingVisible.value = false;
      batchLoading.value = false;
      submissionAbortController = null;
    }
  }

  async function triggerSingleRegrade(row: SubmissionItem) {
    const submissionId = parsePositiveId(row?.id);
    if (submissionId === null) {
      ElMessage.error('该答卷缺少有效标识，无法执行 AI 批改，请刷新列表后重试');
      return;
    }

    aiThinkingTitle.value = `AI 智能阅卷 · ${row.studentName || '学生答卷'}`;
    submissionAbortController = new AbortController();
    aiThinkingVisible.value = true;
    try {
      if (submissionAbortController.signal.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }
      await gradeSubmission(submissionId, { signal: submissionAbortController.signal, silent: true });
      aiThinkingVisible.value = false;
      ElMessage.success(`已为【${row.studentName || '学生'}】重新完成 AI 智能预评打分与评语生成！`);
    } catch (err: unknown) {
      aiThinkingVisible.value = false;
      if (err instanceof DOMException && err.name === 'AbortError') {
        ElMessage.info('已中止本次 AI 阅卷推演');
        return;
      }
      ElMessage.error(resolveApiErrorMessage(err, 'AI 批改失败'));
    } finally {
      aiThinkingVisible.value = false;
      submissionAbortController = null;
    }
  }

  async function removeSubmissionRecord(id: number, studentName?: string) {
    const submissionId = parsePositiveId(id);
    if (submissionId === null) {
      ElMessage.error('该答卷缺少有效标识，无法删除，请刷新列表后重试');
      return false;
    }

    try {
      await ElMessageBox.confirm(
        `确定要删除${studentName ? `学生「${studentName}」的` : ''}该份答卷记录吗？删除后关联的作答与评分数据将一并清理且不可恢复。`,
        '删除答卷确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      loading.value = true;
      await deleteSubmission(submissionId, { silent: true });
      ElMessage.success('答卷记录已成功删除');
      return true;
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return false;
      ElMessage.error(resolveApiErrorMessage(err, '删除答卷失败'));
      return false;
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    batchLoading,
    courses,
    allSubmissions,
    total,
    stats,
    aiThinkingVisible,
    aiThinkingTitle,
    handleAbortSubmissionGrade,
    loadCourses,
    fetchAllSubmissions,
    batchGradePending,
    triggerSingleRegrade,
    removeSubmissionRecord
  };
}
