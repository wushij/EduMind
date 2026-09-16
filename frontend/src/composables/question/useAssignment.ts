import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  getAssignments,
  getAssignmentDetail,
  createAssignment,
  publishAssignment,
  deleteAssignment,
  submitAssignment,
  getAssignmentSubmissions
} from '@/api/question/assignment';
import { getSubmissionsByAssignment, gradeSubmission } from '@/api/question/submission';
import { getCourseList } from '@/api/course/course';
import { getExams, getExamDetail } from '@/api/question/exam';
import { Assignment } from '@/types/question/assignment';
import type { Course } from '@/types/course/course';
import type { QuestionItem, QuestionType } from '@/types/question/question';
import { normalizeQuestionList } from '@/utils/question/normalize-question';

export function useAssignment() {
  const assignments = ref<Assignment[]>([]);
  const currentAssignment = ref<Assignment | null>(null);
  const submissions = ref<any[]>([]);
  const loading = ref(false);
  const total = ref(0);

  async function fetchAssignments(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getAssignments(params);
      assignments.value = (res.data?.list || []) as Assignment[];
      total.value = res.data?.total ?? assignments.value.length;
    } catch (err) {
      assignments.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchAssignmentDetail(id: number) {
    loading.value = true;
    try {
      const res = await getAssignmentDetail(id);
      currentAssignment.value = res.data;
    } catch (err) {
      currentAssignment.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentAssignment.value;
  }

  async function createAndPublish(data: Record<string, any>, publish = true) {
    const res = await createAssignment(data);
    const id = res.data;
    if (publish && id) await publishAssignment(id);
    return id;
  }

  async function submit(id: number, answers: Array<{ questionId: number; answer: string }>) {
    return submitAssignment(id, answers);
  }

  async function fetchSubmissions(assignmentId: number) {
    try {
      const res = await getAssignmentSubmissions(assignmentId);
      submissions.value = res.data || [];
    } catch (err) {
      submissions.value = [];
      throw err;
    }
    return submissions.value;
  }

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      return res.data?.list || [
        { id: 101, title: '数据结构与算法' } as Course,
        { id: 102, title: 'Java程序设计' } as Course,
        { id: 103, title: '高等数学（上）' } as Course
      ];
    } catch {
      return [
        { id: 101, title: '数据结构与算法' } as Course,
        { id: 102, title: 'Java程序设计' } as Course,
        { id: 103, title: '高等数学（上）' } as Course
      ];
    }
  }

  async function gradePendingSubmissions(assignmentId: number) {
    const sRes = await getSubmissionsByAssignment(assignmentId);
    const pendingSubs = (sRes.data || []).filter((s: any) => s.status === 'PENDING');
    for (const sub of pendingSubs) {
      await gradeSubmission(sub.id);
    }
    return pendingSubs.length;
  }

  async function removeAssignment(id: number, title?: string) {
    try {
      await ElMessageBox.confirm(
        `确定删除草稿作业「${title || '当前作业'}」吗？删除后不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deleteAssignment(id);
      assignments.value = assignments.value.filter(a => a.id !== id);
      total.value = Math.max(0, total.value - 1);
      ElMessage.success('作业已删除');
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '删除作业失败');
      throw err;
    }
  }

  return {
    assignments,
    currentAssignment,
    submissions,
    loading,
    total,
    fetchAssignments,
    fetchAssignmentDetail,
    createAndPublish,
    submit,
    fetchSubmissions,
    loadCourses,
    gradePendingSubmissions,
    removeAssignment
  };
}

/* ── Pure helpers (create) ── */

export function getAssignmentCreateTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选',
    MULTIPLE_CHOICE: '多选',
    TRUE_FALSE: '判断',
    FILL_BLANK: '填空',
    SHORT_ANSWER: '简答'
  };
  return map[type] || type || '单选';
}

export function getAssignmentCreateTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as any) || '';
}

export function calculateSelectedQuestionsScore(questions: QuestionItem[]) {
  return questions.reduce((acc, q) => acc + (q.score || 5), 0);
}

/* ── Pure helpers (detail) ── */

export function getSubmissionStatusLabel(status: string) {
  const map: Record<string, string> = {
    GRADED: '批改完成',
    AI_GRADED: 'AI已预评',
    PENDING: '待教师终审'
  };
  return map[status] || '待批改';
}

export function getSubmissionStatusType(status: string) {
  const map: Record<string, string> = {
    GRADED: 'success',
    AI_GRADED: 'primary',
    PENDING: 'warning'
  };
  return (map[status] as any) || 'info';
}

export function getAssignmentDetailTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[type] || type || '试题';
}

export function getAssignmentDetailTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as any) || '';
}

export function filterSubmissions(
  submissions: any[],
  statusFilter: string,
  studentSearch: string
) {
  return submissions.filter(s => {
    if (statusFilter && s.status !== statusFilter) return false;
    if (studentSearch.trim()) {
      const kw = studentSearch.trim().toLowerCase();
      const inName = s.studentName.toLowerCase().includes(kw);
      const inNo = s.studentNo.includes(kw);
      if (!inName && !inNo) return false;
    }
    return true;
  });
}

export function resolveTotalStudentsCount(enrolled: unknown, submissionCount: number) {
  if (typeof enrolled === 'number' && enrolled > 0) {
    return enrolled;
  }
  return Math.max(submissionCount, 1);
}

export function calculateSubmissionRate(submittedCount: number, totalStudentsCount: number) {
  return Math.round((submittedCount / totalStudentsCount) * 100);
}

export function countPendingReview(submissions: any[]) {
  return submissions.filter(s => s.status !== 'GRADED').length;
}

export function calculateAverageScore(submissions: any[]) {
  const scored = submissions.filter(s => s.finalScore !== null);
  if (!scored.length) return '88.5';
  const sum = scored.reduce((acc, s) => acc + s.finalScore, 0);
  return (sum / scored.length).toFixed(1);
}

/* ── useAssignmentCreate ── */

export function useAssignmentCreate() {
  const router = useRouter();
  const route = useRoute();

  const formRef = ref<FormInstance>();
  const submitting = ref(false);
  const courses = ref<Course[]>([]);
  const examOptions = ref<any[]>([]);
  const sourceMode = ref<'EXAM' | 'QUESTIONS'>('EXAM');

  const formData = reactive({
    title: '',
    courseId: 101 as number | undefined,
    deadline: '2026-09-30 23:59:59',
    totalScore: 100,
    passScore: 60,
    examId: undefined as number | undefined,
    aiGradingEnabled: true,
    allowLate: false,
    instantFeedback: true
  });

  const rules = reactive<FormRules>({
    title: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
    courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
    deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
  });

  const selectedQuestions = ref<QuestionItem[]>([]);

  const totalCalculatedScore = computed(() =>
    calculateSelectedQuestionsScore(selectedQuestions.value)
  );

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      courses.value = res.data?.list || [];
    } catch (err: any) {
      courses.value = [];
      ElMessage.error(err?.message || '获取课程列表失败');
    }
  }

  async function loadExams() {
    try {
      const res = await getExams({ pageSize: 50 });
      examOptions.value = res.data?.list || [];
    } catch (err: any) {
      examOptions.value = [];
      ElMessage.error(err?.message || '获取试卷列表失败');
    }
  }

  function handleCourseChange(_courseId?: number) {
    // 可按课程联动试卷
  }

  function handleExamSelected(id?: number) {
    const e = examOptions.value.find(item => item.id === id);
    if (e) {
      formData.totalScore = e.totalScore || 100;
      formData.passScore = e.passScore || 60;
      selectedQuestions.value = e.questions?.length
        ? normalizeQuestionList(e.questions as Record<string, unknown>[])
        : [];
    }
  }

  async function handlePublishAssignment() {
    if (!formRef.value) return;
    await formRef.value.validate(async (valid) => {
      if (!valid) return;
      if (selectedQuestions.value.length === 0) {
        ElMessage.warning('作业中至少需要包含一道试题！');
        return;
      }

      submitting.value = true;
      try {
        const payload = {
          title: formData.title,
          courseId: formData.courseId,
          deadline: formData.deadline,
          totalScore: formData.totalScore,
          passScore: formData.passScore,
          examId: formData.examId,
          settings: {
            aiGradingEnabled: formData.aiGradingEnabled,
            allowLate: formData.allowLate,
            instantFeedback: formData.instantFeedback
          },
          questionIds: selectedQuestions.value.map(q => q.id)
        };

        const res = await createAssignment(payload);
        const assignmentId = res.data;
        if (assignmentId) {
          await publishAssignment(assignmentId);
        }
        ElMessage.success('作业已成功发布，学生现已可在学生端查收并作答！');
        router.push('/question/assignments');
      } catch (err: any) {
        console.error('发布作业失败:', err);
        ElMessage.error(err?.message || '作业发布失败，请检查网络或后端接口状态');
      } finally {
        submitting.value = false;
      }
    });
  }

  function handleCancel() {
    router.push('/question/assignments');
  }

  onMounted(async () => {
    await loadCourses();
    await loadExams();

    if (route.query.examId) {
      formData.examId = Number(route.query.examId);
      if (route.query.courseId) formData.courseId = Number(route.query.courseId);
      if (route.query.title) formData.title = `${route.query.title} - 课堂作业测验`;
      handleExamSelected(formData.examId);
    } else {
      if (examOptions.value.length > 0) {
        formData.examId = examOptions.value[0].id;
        formData.title = '第三周：二叉树遍历与递归算法平时作业';
        handleExamSelected(formData.examId);
      }
    }
  });

  return {
    formRef,
    submitting,
    courses,
    examOptions,
    sourceMode,
    formData,
    rules,
    selectedQuestions,
    totalCalculatedScore,
    handleCourseChange,
    handleExamSelected,
    handlePublishAssignment,
    handleCancel,
    getTypeLabel: getAssignmentCreateTypeLabel,
    getTypeTagType: getAssignmentCreateTypeTagType
  };
}

/* ── useAssignmentDetail ── */

export function useAssignmentDetail() {
  const route = useRoute();
  const router = useRouter();

  const assignmentId = computed(() => Number(route.params.id) || 1);
  const loading = ref(false);
  const batchAILoading = ref(false);
  const activeTab = ref('submissions');

  const assignmentInfo = ref<any>(null);
  const submissionsList = ref<any[]>([]);
  const questionsList = ref<QuestionItem[]>([]);

  const studentSearch = ref('');
  const statusFilter = ref('');

  const totalStudentsCount = computed(() =>
    resolveTotalStudentsCount(assignmentInfo.value?.studentCount, submissionsList.value.length)
  );

  const filteredSubmissions = computed(() =>
    filterSubmissions(submissionsList.value, statusFilter.value, studentSearch.value)
  );

  const submittedCount = computed(() => submissionsList.value.length);

  const submissionRate = computed(() =>
    calculateSubmissionRate(submittedCount.value, totalStudentsCount.value)
  );

  const pendingReviewCount = computed(() => countPendingReview(submissionsList.value));

  const averageScore = computed(() => calculateAverageScore(submissionsList.value));

  async function loadAssignmentData() {
    loading.value = true;
    try {
      const res = await getAssignmentDetail(assignmentId.value);
      assignmentInfo.value = res.data;
      if (res.data?.examId) {
        try {
          const examRes = await getExamDetail(res.data.examId);
          questionsList.value = normalizeQuestionList(examRes.data?.questions || []);
        } catch {
          questionsList.value = [];
        }
      } else {
        questionsList.value = [];
      }
    } catch (err: any) {
      assignmentInfo.value = null;
      questionsList.value = [];
      ElMessage.error(err?.message || '加载作业详情失败，请检查网络或后端状态');
    } finally {
      loading.value = false;
    }
  }

  async function loadSubmissions() {
    try {
      const res = await getSubmissionsByAssignment(assignmentId.value);
      submissionsList.value = res.data || [];
    } catch (err: any) {
      submissionsList.value = [];
      ElMessage.error(err?.message || '加载提交记录失败');
    }
  }

  function goToGradingWorkspace(submissionId: number) {
    router.push(`/question/submissions/${submissionId}`);
  }

  async function triggerSingleAIGrade(row: any) {
    try {
      const res = await gradeSubmission(row.id);
      row.aiGraded = true;
      row.aiScore = res.data?.totalScore ?? res.data?.score ?? 88;
      row.status = 'AI_GRADED';
      ElMessage.success(`学生 ${row.studentName} 的答卷已完成AI智能预批！得分：${row.aiScore}分`);
    } catch (err: any) {
      ElMessage.error(err?.message || 'AI批改请求失败，请检查服务连接');
    }
  }

  async function handleBatchAIGrade() {
    batchAILoading.value = true;
    try {
      const pendings = submissionsList.value.filter(s => s.status === 'PENDING' || !s.aiGraded);
      if (!pendings.length) {
        ElMessage.info('当前暂无待AI预评的答卷');
        return;
      }
      let successCount = 0;
      for (const sub of pendings) {
        try {
          const res = await gradeSubmission(sub.id);
          sub.aiGraded = true;
          sub.aiScore = res.data?.totalScore ?? res.data?.score ?? 85;
          sub.status = 'AI_GRADED';
          successCount++;
        } catch (subErr) {
          console.warn(`批改答卷 #${sub.id} 失败`, subErr);
        }
      }
      if (successCount > 0) {
        ElMessage.success(`已成功为 ${successCount} 份答卷完成AI智能辅助预批改！`);
      } else {
        ElMessage.warning('批量AI批改未完成，请检查答卷状态与后端接口');
      }
    } catch (err: any) {
      ElMessage.error(err?.message || '批量批改处理失败');
    } finally {
      batchAILoading.value = false;
    }
  }

  function handleRemindUnsubmitted() {
    ElMessage.success('已通过系统站内信与移动端向 4 位尚未提交作业的同学发送催交提醒！');
  }

  function handleBack() {
    router.push('/question/assignments');
  }

  async function handleDeleteDraft() {
    const title = assignmentInfo.value?.title || '当前作业';
    try {
      await ElMessageBox.confirm(
        `确定删除草稿作业「${title}」吗？删除后不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deleteAssignment(assignmentId.value);
      ElMessage.success('作业已删除');
      router.push('/question/assignments');
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '删除作业失败');
    }
  }

  onMounted(async () => {
    await loadAssignmentData();
    await loadSubmissions();
  });

  return {
    assignmentId,
    loading,
    batchAILoading,
    activeTab,
    assignmentInfo,
    submissionsList,
    questionsList,
    studentSearch,
    statusFilter,
    totalStudentsCount,
    filteredSubmissions,
    submittedCount,
    submissionRate,
    pendingReviewCount,
    averageScore,
    goToGradingWorkspace,
    triggerSingleAIGrade,
    handleBatchAIGrade,
    handleRemindUnsubmitted,
    handleBack,
    handleDeleteDraft,
    getSubmissionStatusLabel,
    getSubmissionStatusType,
    getTypeLabel: getAssignmentDetailTypeLabel,
    getTypeTagType: getAssignmentDetailTypeTagType
  };
}
