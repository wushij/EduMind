import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  getSubmissionDetail,
  getSubmissionGrading,
  gradeSubmission,
  reviewGrading,
  getSubmissionsPage,
  getSubmissionStats,
  batchGradeSubmissions
} from '@/api/question/submission';
import { getCourseList } from '@/api/course/course';
import { normalizeCourseListFromApi } from '@/utils/course/course-display';
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

export function buildGradingItems(sub: any, gList: any[]): GradingItem[] {
  if (!Array.isArray(gList) || gList.length === 0) {
    return [];
  }
  const answersMap = new Map<number, string>();
  if (sub?.answers && Array.isArray(sub.answers)) {
    sub.answers.forEach((ans: any) => {
      answersMap.set(ans.questionId, ans.answer);
    });
  }

  return gList.map((g: any, idx: number) => {
    const qid = g.questionId || idx + 1;
    const stuAns = answersMap.get(qid) || '';
    const isObj = g.isCorrect !== undefined && g.isCorrect !== null;
    const type = g.type || (isObj ? 'SINGLE_CHOICE' : 'SHORT_ANSWER');

    return {
      questionId: qid,
      type,
      stem: g.stem || `试题 #${qid}`,
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

  const submissionId = computed(() => Number(route.params.id) || 201);
  const loading = ref(false);
  const saving = ref(false);
  const gradingInProgress = ref(false);

  const submissionData = ref<any>(null);
  const gradingItems = ref<GradingItem[]>([]);

  const calculatedTotalScore = computed(() => calculateGradingTotalScore(gradingItems.value));

  async function loadSubmissionData() {
    loading.value = true;
    try {
      const res = await getSubmissionDetail(submissionId.value);
      submissionData.value = res.data;
      if (!submissionData.value) {
        ElMessage.error('未找到该答卷详情');
        return;
      }

      let gradingResults: any[] = [];
      try {
        const gRes = await getSubmissionGrading(submissionId.value);
        gradingResults = gRes.data || [];
      } catch {
        gradingResults = submissionData.value?.gradingItems || [];
      }

      gradingItems.value = buildGradingItems(submissionData.value, gradingResults);
    } catch (err: any) {
      ElMessage.error(err?.message || '加载答卷详情失败');
      submissionData.value = null;
      gradingItems.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function handleTriggerGradeNow() {
    gradingInProgress.value = true;
    try {
      await gradeSubmission(submissionId.value);
      ElMessage.success('已成功触发该答卷的 AI 智能分析与评分！');
      await loadSubmissionData();
    } catch (err: any) {
      ElMessage.error(err?.message || '触发 AI 评阅失败，请稍后重试');
    } finally {
      gradingInProgress.value = false;
    }
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
    saving.value = true;
    try {
      const payload = gradingItems.value.map(item => ({
        questionId: item.questionId,
        score: item.teacherScore,
        teacherComment: item.teacherComment || ''
      }));

      await reviewGrading(submissionId.value, payload);
      if (submissionData.value) {
        submissionData.value.status = 'REVIEWED';
      }
      ElMessage.success('评阅成绩已正式确认并发布！总成绩：' + calculatedTotalScore.value + ' 分');
      setTimeout(() => {
        router.push('/question/submissions');
      }, 800);
    } catch (err: any) {
      ElMessage.error(err?.message || '评阅结果保存失败，请检查网络与登录权限');
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
    loadSubmissionData,
    handleTriggerGradeNow,
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
          pageSize: params.pageSize ?? 20
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
      total.value = pageRes.data?.total ?? list.length;
      stats.value = statsRes.data || {};
      return allSubmissions.value;
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '获取提交列表失败');
      allSubmissions.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function batchGradePending(courseId?: number | null) {
    batchLoading.value = true;
    try {
      const res = await batchGradeSubmissions({ courseId: courseId ?? undefined });
      const successCount = res.data?.successCount ?? 0;
      ElMessage.success(`全队列批改完成，共成功处理 ${successCount} 份待评答卷`);
      return successCount;
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '批量批改失败');
      return 0;
    } finally {
      batchLoading.value = false;
    }
  }

  return {
    loading,
    batchLoading,
    courses,
    allSubmissions,
    total,
    stats,
    loadCourses,
    fetchAllSubmissions,
    batchGradePending
  };
}
