import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { getExams, getExamDetail, createExam, updateExam, deleteExam, exportExam } from '@/api/question/exam';
import { getCourseList } from '@/api/course/course';
import { getQuestions } from '@/api/question/question';
import { getQuestionBankDetail } from '@/api/question/question-bank';
import type { ExamPaper } from '@/types/question/exam';
import type { Course } from '@/types/course/course';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';
import { normalizeExamList, normalizeExamPaper } from '@/utils/question/normalize-exam';
import { normalizeQuestionList } from '@/utils/question/normalize-question';

export interface ExamSection {
  id: string;
  type: QuestionType;
  title: string;
  defaultScore: number;
  questions: QuestionItem[];
}

export interface GroupedSection {
  type: QuestionType;
  title: string;
  totalScore: number;
  questions: QuestionItem[];
}

export interface DifficultyCounts {
  EASY: number;
  MEDIUM: number;
  HARD: number;
}

const SECTION_TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单项选择题',
  MULTIPLE_CHOICE: '多项选择题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '综合应用与简答题'
};

const SECTION_DEFAULT_SCORES: Record<string, number> = {
  SINGLE_CHOICE: 5,
  MULTIPLE_CHOICE: 5,
  TRUE_FALSE: 3,
  FILL_BLANK: 4,
  SHORT_ANSWER: 10
};

export function getChineseNumber(num: number) {
  const cn = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
  return cn[num] || String(num);
}

export function getExamTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[type] || type || '单选题';
}

export function getExamTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as string) || '';
}

export function getExamDifficultyLabel(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  };
  return map[diff] || '中等';
}

export function getExamDifficultyTagType(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  };
  return (map[diff] as string) || '';
}

export function getExamStatusLabel(status?: string) {
  const map: Record<string, string> = {
    PUBLISHED: '正式发布',
    DRAFT: '草稿暂存',
    ARCHIVED: '已归档'
  };
  return map[status || 'PUBLISHED'] || '正式发布';
}

export function getExamStatusTagType(status?: string) {
  const map: Record<string, string> = {
    PUBLISHED: 'success',
    DRAFT: 'info',
    ARCHIVED: 'warning'
  };
  return (map[status || 'PUBLISHED'] as string) || 'success';
}

export function calculateSectionsTotalScore(sections: ExamSection[]): number {
  let sum = 0;
  for (const sec of sections) {
    for (const q of sec.questions) {
      sum += q.score || 0;
    }
  }
  return sum;
}

export function calculateSectionsTotalQuestions(sections: ExamSection[]): number {
  return sections.reduce((acc, s) => acc + s.questions.length, 0);
}

export function getSectionScore(sec: ExamSection): number {
  return sec.questions.reduce((acc, q) => acc + (q.score || 0), 0);
}

export function createDefaultExamSections(): ExamSection[] {
  return [
    {
      id: 'sec-1',
      type: 'SINGLE_CHOICE',
      title: '单项选择题',
      defaultScore: 5,
      questions: []
    },
    {
      id: 'sec-2',
      type: 'MULTIPLE_CHOICE',
      title: '多项选择题',
      defaultScore: 5,
      questions: []
    },
    {
      id: 'sec-3',
      type: 'SHORT_ANSWER',
      title: '综合应用与简答题',
      defaultScore: 10,
      questions: []
    }
  ];
}

export function createSectionByType(type: QuestionType): ExamSection {
  const addLabels: Record<string, string> = {
    SINGLE_CHOICE: '单项选择题',
    MULTIPLE_CHOICE: '多项选择题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答与分析题'
  };
  return {
    id: `sec-${Date.now()}`,
    type,
    title: addLabels[type] || '试题大题',
    defaultScore: SECTION_DEFAULT_SCORES[type] || 5,
    questions: []
  };
}

export function filterPickerQuestions(
  pool: QuestionItem[],
  section: ExamSection | null,
  keyword: string,
  difficulty: string
): QuestionItem[] {
  if (!section) return [];
  const currentSectionIds = new Set(section.questions.map(q => q.id));

  return pool.filter(q => {
    if (q.type !== section.type) return false;
    if (currentSectionIds.has(q.id)) return false;
    if (difficulty && q.difficulty !== difficulty) return false;
    if (keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      const inStem = q.stem.toLowerCase().includes(kw);
      const inKp = q.knowledgePointNames?.some(k => k.toLowerCase().includes(kw));
      if (!inStem && !inKp) return false;
    }
    return true;
  });
}

export function groupQuestionsByType(questions: QuestionItem[]): GroupedSection[] {
  const map = new Map<string, GroupedSection>();

  questions.forEach(q => {
    const type = q.type || 'SINGLE_CHOICE';
    if (!map.has(type)) {
      map.set(type, {
        type,
        title: SECTION_TYPE_LABELS[type] || '综合试题',
        totalScore: 0,
        questions: []
      });
    }
    const sec = map.get(type)!;
    sec.questions.push(q);
    sec.totalScore += q.score || 5;
  });

  return Array.from(map.values());
}

export function calculateDifficultyCounts(questions: QuestionItem[]): DifficultyCounts {
  const counts: DifficultyCounts = { EASY: 0, MEDIUM: 0, HARD: 0 };
  questions.forEach(q => {
    const diff = (q.difficulty || 'MEDIUM') as keyof DifficultyCounts;
    if (counts[diff] !== undefined) {
      counts[diff]++;
    }
  });
  return counts;
}

export function calculateDifficultyPercentages(
  counts: DifficultyCounts,
  total: number
): DifficultyCounts {
  const safeTotal = total || 1;
  return {
    EASY: Math.round((counts.EASY / safeTotal) * 100),
    MEDIUM: Math.round((counts.MEDIUM / safeTotal) * 100),
    HARD: Math.round((counts.HARD / safeTotal) * 100)
  };
}

export function collectKnowledgePoints(questions: QuestionItem[]): string[] {
  const set = new Set<string>();
  questions.forEach(q => {
    q.knowledgePointNames?.forEach(kp => set.add(kp));
  });
  return Array.from(set);
}

export function useExam() {
  const exams = ref<ExamPaper[]>([]);
  const currentExam = ref<ExamPaper | null>(null);
  const loading = ref(false);
  const total = ref(0);

  async function fetchExams(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getExams(params);
      exams.value = normalizeExamList((res.data?.list || []) as Record<string, any>[]);
      total.value = res.data?.total ?? exams.value.length;
    } catch (err) {
      exams.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchExamDetail(id: number) {
    loading.value = true;
    try {
      const res = await getExamDetail(id);
      currentExam.value = normalizeExamPaper((res.data || {}) as Record<string, any>);
    } catch (err) {
      currentExam.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentExam.value;
  }

  async function saveExam(data: Partial<ExamPaper>, id?: number) {
    if (id) await updateExam(id, data);
    else await createExam(data);
  }

  async function removeExam(id: number) {
    await deleteExam(id);
    exams.value = exams.value.filter(e => e.id !== id);
    total.value = exams.value.length;
  }

  async function fetchExport(id: number) {
    const res = await exportExam(id);
    return res.data;
  }

  async function loadCourseOptions() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 100 });
      const list = res.data?.list || [];
      return [
        { label: '全部课程', value: null as number | null },
        ...list.map((item: any) => ({
          label: item.title || item.name,
          value: item.id as number
        }))
      ];
    } catch {
      return [{ label: '全部课程', value: null as number | null }];
    }
  }

  return {
    exams,
    currentExam,
    loading,
    total,
    fetchExams,
    fetchExamDetail,
    saveExam,
    removeExam,
    fetchExport,
    loadCourseOptions
  };
}

export function useExamCreate() {
  const router = useRouter();
  const route = useRoute();

  const currentStep = ref(0);
  const saving = ref(false);
  const courses = ref<Course[]>([]);
  const publishStatus = ref<'DRAFT' | 'PUBLISHED'>('PUBLISHED');

  const step1FormRef = ref<FormInstance>();
  const examForm = reactive({
    title: '2025-2026学年第二学期期末综合测试试卷',
    semester: '2025-2026-2',
    courseId: 101 as number | undefined,
    courseName: '数据结构与算法',
    durationMinutes: 120,
    totalScore: 100,
    passScore: 60,
    description: '本试卷满分100分，答题时间120分钟。请在规定区域内规范书写，独立完成作答。'
  });

  const step1Rules = reactive<FormRules>({
    title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
    semester: [{ required: true, message: '请选择学期', trigger: 'change' }],
    courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }]
  });

  const sections = ref<ExamSection[]>(createDefaultExamSections());

  const pickerVisible = ref(false);
  const activeSection = ref<ExamSection | null>(null);
  const pickerKeyword = ref('');
  const pickerDifficulty = ref('');
  const selectedPickerIds = ref<(number | string)[]>([]);
  const poolQuestions = ref<QuestionItem[]>([]);

  const currentTotalScore = computed(() => calculateSectionsTotalScore(sections.value));
  const currentTotalQuestions = computed(() => calculateSectionsTotalQuestions(sections.value));

  const filteredCandidateQuestions = computed(() =>
    filterPickerQuestions(
      poolQuestions.value,
      activeSection.value,
      pickerKeyword.value,
      pickerDifficulty.value
    )
  );

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 100 });
      courses.value = (res.data?.list || []).map((c: any) => ({
        ...c,
        id: Number(c.id),
        title: c.title || c.name || '未命名课程'
      }));
    } catch {
      // 课程接口不可用时不再注入写死的种子课程（101/102/103），
      // 否则教师可能把试卷挂到并不存在、或自己无权访问的课程上。
      courses.value = [];
      ElMessage.error('课程列表加载失败，请稍后重试');
    }
  }

  async function loadPoolQuestions() {
    try {
      const res = await getQuestions({ pageSize: 50 });
      poolQuestions.value = normalizeQuestionList(res.data?.list || []);
    } catch (err: any) {
      poolQuestions.value = [];
      ElMessage.error(err?.message || '获取试题池失败');
    }
  }

  function handleCourseChange(val?: number) {
    const numId = Number(val);
    const c = courses.value.find(item => Number(item.id) === numId);
    // 课程名只取自课程列表的真实数据，不再用写死的 id → 名称映射兜底
    examForm.courseName = c ? c.title || (c as any).name : '';
  }

  function getCourseName(courseId?: number) {
    const numId = Number(courseId);
    const c = courses.value.find(item => Number(item.id) === numId);
    if (c) return c.title || (c as any).name || '未指定课程';
    return examForm.courseName || '未指定课程';
  }

  function updateSectionDefaultScore(sec: ExamSection) {
    for (const q of sec.questions) {
      q.score = sec.defaultScore;
    }
  }

  function calculateScores() {
    // trigger reactivity
  }

  function handleAddSection(type: QuestionType) {
    const labels: Record<string, string> = {
      SINGLE_CHOICE: '单项选择题',
      MULTIPLE_CHOICE: '多项选择题',
      TRUE_FALSE: '判断题',
      FILL_BLANK: '填空题',
      SHORT_ANSWER: '简答与分析题'
    };
    const newSection = createSectionByType(type);
    sections.value.push(newSection);
    ElMessage.success(`已添加新的大题模块：${labels[type]}`);
  }

  function removeSection(idx: number) {
    sections.value.splice(idx, 1);
    ElMessage.info('已移除该大题');
  }

  function removeQuestionFromSection(sec: ExamSection, qIdx: number) {
    sec.questions.splice(qIdx, 1);
  }

  function openQuestionPicker(sec: ExamSection) {
    activeSection.value = sec;
    pickerKeyword.value = '';
    pickerDifficulty.value = '';
    selectedPickerIds.value = [];
    pickerVisible.value = true;
  }

  function togglePickerItem(id: number | string) {
    const idx = selectedPickerIds.value.findIndex(item => String(item) === String(id));
    if (idx > -1) {
      selectedPickerIds.value.splice(idx, 1);
    } else {
      selectedPickerIds.value.push(id);
    }
  }

  function confirmAddPickedQuestions() {
    if (!activeSection.value) return;
    const pickedList = poolQuestions.value.filter(q =>
      selectedPickerIds.value.some(id => String(id) === String(q.id))
    );
    const newQuestions = pickedList.map(q => ({
      ...q,
      score: activeSection.value?.defaultScore || q.score || 5
    }));
    activeSection.value.questions.push(...newQuestions);
    pickerVisible.value = false;
    ElMessage.success(`成功添加 ${newQuestions.length} 道题目到【${activeSection.value.title}】`);
  }

  async function goToStep2() {
    if (!step1FormRef.value) return;
    await step1FormRef.value.validate((valid) => {
      if (valid) {
        currentStep.value = 1;
      }
    });
  }

  function goToStep3() {
    if (sections.value.length === 0) {
      ElMessage.warning('试卷至少需要包含一个大题！');
      return;
    }
    if (currentTotalQuestions.value === 0) {
      ElMessage.warning('试卷尚未挑选任何试题，请至少添加试题后再审阅！');
      return;
    }
    currentStep.value = 2;
  }

  async function handleSaveExam() {
    saving.value = true;
    try {
      const allQuestions: QuestionItem[] = [];
      sections.value.forEach(sec => {
        allQuestions.push(...sec.questions);
      });

      const payload = {
        title: examForm.title,
        courseId: examForm.courseId || 101,
        courseName: examForm.courseName,
        semester: examForm.semester,
        totalScore: currentTotalScore.value,
        passScore: examForm.passScore,
        durationMinutes: examForm.durationMinutes,
        status: publishStatus.value,
        rules: sections.value.map(s => ({
          type: s.type,
          label: s.title,
          count: s.questions.length,
          scoreEach: s.defaultScore
        })),
        questions: allQuestions
      };

      await createExam(payload as any);
      ElMessage.success(publishStatus.value === 'PUBLISHED' ? '试卷正式发布成功！' : '试卷草稿已成功保存！');
      router.push('/question/exams');
    } catch (err: any) {
      console.error('保存试卷失败', err);
      ElMessage.error(err?.message || '试卷保存失败，请检查网络或后端接口状态');
    } finally {
      saving.value = false;
    }
  }

  function handleCancel() {
    router.push('/question/exams');
  }

  onMounted(async () => {
    await loadCourses();
    await loadPoolQuestions();

    if (route.query.courseId) {
      examForm.courseId = Number(route.query.courseId);
      handleCourseChange(examForm.courseId);
    }

    if (route.query.bankName) {
      examForm.title = `${decodeURIComponent(String(route.query.bankName))} - 综合测试卷`;
    }

    if (route.query.bankId) {
      try {
        const bankRes = await getQuestionBankDetail(String(route.query.bankId));
        const bankData = bankRes.data;
        if (bankData?.name) {
          examForm.title = `${bankData.name} - 综合测试卷`;
        }
        if (bankData?.courseId) {
          examForm.courseId = Number(bankData.courseId);
          handleCourseChange(examForm.courseId);
        }
        const rawList = (bankData?.questions && Array.isArray(bankData.questions)) ? bankData.questions : [];
        const bQuestions = normalizeQuestionList(rawList as Record<string, unknown>[]);
        if (bQuestions.length > 0) {
          sections.value.forEach(s => { s.questions = []; });
          bQuestions.forEach(q => {
            let targetSec = sections.value.find(s => s.type === q.type);
            if (!targetSec) {
              targetSec = createSectionByType(q.type);
              sections.value.push(targetSec);
            }
            targetSec.questions.push({ ...q, score: q.score || targetSec.defaultScore || 5 });
          });
          examForm.totalScore = currentTotalScore.value || 100;
          examForm.passScore = Math.round(examForm.totalScore * 0.6);
          ElMessage.success(`已自动从题库「${bankData?.name || '题库'}」载入 ${bQuestions.length} 道试题至组卷清单！`);
        }
      } catch (err) {
        console.error('从题库载入试题失败', err);
      }
    } else if (sections.value[0].questions.length === 0 && poolQuestions.value.length > 0) {
      const singleChoices = poolQuestions.value.filter(q => q.type === 'SINGLE_CHOICE').slice(0, 3);
      sections.value[0].questions = singleChoices.map(q => ({ ...q, score: 5 }));
    }
  });

  return {
    router,
    currentStep,
    saving,
    courses,
    publishStatus,
    step1FormRef,
    examForm,
    step1Rules,
    sections,
    pickerVisible,
    activeSection,
    pickerKeyword,
    pickerDifficulty,
    selectedPickerIds,
    poolQuestions,
    currentTotalScore,
    currentTotalQuestions,
    filteredCandidateQuestions,
    loadCourses,
    loadPoolQuestions,
    handleCourseChange,
    getCourseName,
    updateSectionDefaultScore,
    calculateScores,
    handleAddSection,
    removeSection,
    removeQuestionFromSection,
    openQuestionPicker,
    togglePickerItem,
    confirmAddPickedQuestions,
    goToStep2,
    goToStep3,
    handleSaveExam,
    handleCancel,
    getSectionScore,
    getChineseNumber,
    getTypeLabel: getExamTypeLabel,
    getTypeTagType: getExamTypeTagType,
    getDifficultyLabel: getExamDifficultyLabel,
    getDifficultyTagType: getExamDifficultyTagType
  };
}

export function useExamDetail() {
  const route = useRoute();
  const router = useRouter();
  const { removeExam } = useExam();

  const examId = computed(() => Number(route.params.id) || 1);
  const loading = ref(false);
  const examData = ref<ExamPaper | null>(null);
  const viewMode = ref<'PAPER' | 'ANSWER_KEY'>('PAPER');

  const groupedSections = computed<GroupedSection[]>(() => {
    if (!examData.value?.questions) return [];
    return groupQuestionsByType(examData.value.questions);
  });

  const totalQuestionsCount = computed(() => examData.value?.questions?.length || 0);

  const difficultyCounts = computed(() =>
    calculateDifficultyCounts(examData.value?.questions || [])
  );

  const difficultyPercentages = computed(() =>
    calculateDifficultyPercentages(difficultyCounts.value, totalQuestionsCount.value)
  );

  const coveredKnowledgePoints = computed(() =>
    collectKnowledgePoints(examData.value?.questions || [])
  );

  async function loadExam() {
    loading.value = true;
    try {
      const res = await getExamDetail(examId.value);
      examData.value = normalizeExamPaper((res.data || {}) as Record<string, any>);
      if (examData.value && (!examData.value.questions || examData.value.questions.length === 0)) {
        examData.value.questions = [];
      }
    } catch (err: any) {
      examData.value = null;
      ElMessage.error(err?.message || '加载试卷详情失败，请检查网络或后端状态');
    } finally {
      loading.value = false;
    }
  }

  function handlePublishAsAssignment() {
    router.push({
      path: '/question/assignments/create',
      query: {
        examId: examId.value,
        courseId: examData.value?.courseId,
        title: examData.value?.title
      }
    });
  }

  function handleExportPaper() {
    router.push({
      path: '/question/exports',
      query: { examId: String(examId.value) }
    });
  }

  function printPaper() {
    window.print();
  }

  async function handleDeleteExam() {
    const title = examData.value?.title || '当前试卷';
    try {
      await ElMessageBox.confirm(
        `确定删除试卷「${title}」吗？删除后不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await removeExam(examId.value);
      ElMessage.success('试卷已删除');
      router.push('/question/exams');
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '删除试卷失败');
    }
  }

  onMounted(async () => {
    await loadExam();
  });

  return {
    router,
    examId,
    loading,
    examData,
    viewMode,
    groupedSections,
    totalQuestionsCount,
    difficultyCounts,
    difficultyPercentages,
    coveredKnowledgePoints,
    loadExam,
    handlePublishAsAssignment,
    handleExportPaper,
    printPaper,
    handleDeleteExam,
    getChineseNumber,
    getStatusLabel: getExamStatusLabel,
    getStatusTagType: getExamStatusTagType
  };
}
