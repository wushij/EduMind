import { ref, computed, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  getQuestionBanks,
  createQuestionBank,
  updateQuestionBank,
  getQuestionBankDetail,
  addQuestionsToBank,
  removeQuestionFromBank,
  deleteQuestionBank
} from '@/api/question/question-bank';
import { getCourseList } from '@/api/course/course';
import { getQuestions } from '@/api/question/question';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';
import type { Course } from '@/types/course/course';
import { normalizeQuestionList } from '@/utils/question/normalize-question';
import { canAccessRoute } from '@/utils/router/route-access';

export interface BankFilterState {
  searchKeyword: string;
  filterType: string;
  filterDifficulty: string;
}

export interface CandidateFilterState {
  drawerSearch: string;
  drawerType: string;
}

export function calculateBankTotalScore(questions: QuestionItem[]): number {
  return questions.reduce((acc, q) => acc + (q.score || 5), 0);
}

export function filterBankQuestions(
  questions: QuestionItem[],
  filters: BankFilterState
): QuestionItem[] {
  return questions.filter(item => {
    if (filters.filterType && item.type !== filters.filterType) return false;
    if (filters.filterDifficulty && item.difficulty !== filters.filterDifficulty) return false;
    if (filters.searchKeyword.trim()) {
      const kw = filters.searchKeyword.trim().toLowerCase();
      const inStem = item.stem?.toLowerCase().includes(kw);
      const inKp = item.knowledgePointNames?.some(k => k.toLowerCase().includes(kw));
      if (!inStem && !inKp) return false;
    }
    return true;
  });
}

export function filterCandidateQuestions(
  pool: QuestionItem[],
  bankQuestionIds: (number | string)[],
  filters: CandidateFilterState
): QuestionItem[] {
  const currentIds = new Set(bankQuestionIds.map(String));
  return pool.filter(q => {
    if (currentIds.has(String(q.id))) return false;
    if (filters.drawerType && q.type !== filters.drawerType) return false;
    if (filters.drawerSearch.trim()) {
      const kw = filters.drawerSearch.trim().toLowerCase();
      const inStem = q.stem?.toLowerCase().includes(kw);
      const inKp = q.knowledgePointNames?.some(k => k.toLowerCase().includes(kw));
      if (!inStem && !inKp) return false;
    }
    return true;
  });
}

export function toggleArraySelection<T>(list: T[], value: T): T[] {
  const idx = list.indexOf(value);
  if (idx > -1) {
    return list.filter((_, i) => i !== idx);
  }
  return [...list, value];
}

export function getTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[type] || type || '单选题';
}

export function getTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as string) || '';
}

export function getDifficultyLabel(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  };
  return map[diff] || '中等';
}

export function getDifficultyTagType(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  };
  return (map[diff] as string) || '';
}

const FALLBACK_BANKS = [
  { id: 1, name: '数据结构核心真题库', courseId: 101, questionCount: 5, description: '涵盖408与期末高频真题', updateTime: '2026-09-10' },
  { id: 2, name: 'Java面向对象精选题集', courseId: 102, questionCount: 3, description: 'Java核心典型题型', updateTime: '2026-09-09' },
  { id: 3, name: '高等数学期末测试真题库', courseId: 103, questionCount: 3, description: '微积分计算经典测试题', updateTime: '2026-09-08' }
];

export function downloadMarkdownFile(title: string, questions: QuestionItem[], courseName: string) {
  const totalScore = calculateBankTotalScore(questions);
  const lines: string[] = [];
  lines.push(`# ${title}`);
  lines.push(`> 关联课程：${courseName} | 试题题量：${questions.length} 题 | 卷面参考总分：${totalScore} 分 | 导出时间：${new Date().toLocaleString()}`);
  lines.push('');

  questions.forEach((q, idx) => {
    lines.push(`### 第 ${idx + 1} 题【${getTypeLabel(q.type)}】(${q.score || 5} 分)`);
    lines.push(q.stem || '');
    lines.push('');
    if (Array.isArray(q.options) && q.options.length > 0) {
      q.options.forEach(opt => {
        lines.push(`- **${opt.key}.** ${opt.content}`);
      });
      lines.push('');
    }
    lines.push(`**【参考答案】** ${(q as any).answer || q.correctAnswer || '略'}`);
    if (q.analysis) {
      lines.push(`**【考点解析】** ${q.analysis}`);
    }
    lines.push('');
  });

  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${title.replace(/\s+/g, '_')}_${Date.now()}.md`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function resolveCourseName(courses: Course[], courseId: number | string | undefined | null): string {
  if (!courseId) return '专业核心课';
  const c = courses.find((item) => String(item.id) === String(courseId));
  if (c) return c.title || (c as any).name || '专业核心课';
  const staticNames: Record<string, string> = {
    '101': '数据结构与算法',
    '102': 'Java面向对象程序设计',
    '103': '高等数学（上）'
  };
  return staticNames[String(courseId)] || '专业核心课';
}

export function useBankList() {
  const router = useRouter();
  const loading = ref(false);
  const creating = ref(false);
  const showCreateDialog = ref(false);
  const dialogFormRef = ref<FormInstance>();

  const banks = ref<any[]>([]);
  const courses = ref<Course[]>([]);
  const selectedCourseId = ref<number | null>(null);
  const searchKeyword = ref('');
  const pageNum = ref(1);
  const pageSize = ref(12);
  const total = ref(0);

  const newBankForm = reactive({
    name: '',
    courseId: undefined as number | undefined,
    description: ''
  });

  const editBankForm = reactive({
    name: '',
    courseId: undefined as number | undefined,
    description: ''
  });
  const showEditDialog = ref(false);
  const editingBankId = ref<number | string | null>(null);
  const editDialogFormRef = ref<FormInstance>();
  const updating = ref(false);
  const sortBy = ref<'updateTime' | 'questionCount' | 'name'>('updateTime');

  const dialogRules: FormRules = {
    name: [{ required: true, message: '请输入题库名称', trigger: 'blur' }],
    courseId: [{ required: true, message: '请选择关联课程', trigger: 'change' }]
  };

  const coursesWithCounts = computed(() => {
    return courses.value.map(c => ({
      ...c,
      bankCount: banks.value.filter(b => String(b.courseId) === String(c.id)).length
    }));
  });

  const filteredBanks = computed(() => {
    const list = banks.value.filter(b => {
      if (selectedCourseId.value !== null && String(b.courseId) !== String(selectedCourseId.value)) {
        return false;
      }
      if (searchKeyword.value.trim()) {
        const kw = searchKeyword.value.trim().toLowerCase();
        const inName = b.name?.toLowerCase().includes(kw);
        const inDesc = b.description?.toLowerCase().includes(kw);
        const cName = getCourseName(b.courseId)?.toLowerCase();
        const inCourse = cName?.includes(kw);
        if (!inName && !inDesc && !inCourse) return false;
      }
      return true;
    });

    return [...list].sort((a, b) => {
      if (sortBy.value === 'questionCount') {
        return (Number(b.questionCount) || 0) - (Number(a.questionCount) || 0);
      }
      if (sortBy.value === 'name') {
        return (a.name || '').localeCompare(b.name || '');
      }
      // default: updateTime desc
      const timeA = a.updateTime ? new Date(a.updateTime).getTime() : 0;
      const timeB = b.updateTime ? new Date(b.updateTime).getTime() : 0;
      return timeB - timeA;
    });
  });

  const totalQuestionsAcrossBanks = computed(() => {
    return banks.value.reduce((acc, b) => acc + (Number(b.questionCount) || 0), 0);
  });

  function getCourseBankCount(courseId: number | null): number {
    if (courseId === null) return banks.value.length;
    return banks.value.filter(b => String(b.courseId) === String(courseId)).length;
  }

  function handleCourseFilter(courseId: number | null) {
    selectedCourseId.value = courseId;
    pageNum.value = 1;
    loadBanks();
  }

  function getCourseName(courseId: number | string | undefined | null) {
    return resolveCourseName(courses.value, courseId);
  }

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 100 });
      courses.value = (res.data?.list || []).map(c => ({
        ...c,
        title: c.title || c.name || '未命名课程'
      }));
    } catch (err) {
      console.error('加载课程失败', err);
    }
  }

  async function loadBanks() {
    loading.value = true;
    try {
      const res = await getQuestionBanks({
        page: pageNum.value,
        pageSize: pageSize.value,
        courseId: selectedCourseId.value || undefined
      });
      banks.value = res.data?.list || [];
      total.value = res.data?.total ?? banks.value.length;
    } catch {
      banks.value = FALLBACK_BANKS;
      total.value = banks.value.length;
    } finally {
      loading.value = false;
    }
  }

  async function handleCreateBank() {
    if (!dialogFormRef.value) return;
    await dialogFormRef.value.validate(async (valid) => {
      if (valid) {
        creating.value = true;
        try {
          await createQuestionBank(newBankForm);
          ElMessage.success('题库创建成功！');
          showCreateDialog.value = false;
          newBankForm.name = '';
          newBankForm.description = '';
          pageNum.value = 1;
          await loadBanks();
        } catch (err: any) {
          ElMessage.error(err?.message || '创建题库失败');
        } finally {
          creating.value = false;
        }
      }
    });
  }

  async function handleDeleteBank(bank: { id: number; name?: string }) {
    try {
      await ElMessageBox.confirm(
        `确定删除题库「${bank.name || '当前题库'}」吗？删除后题库内关联将解除，且不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deleteQuestionBank(bank.id);
      ElMessage.success('题库已删除');
      await loadBanks();
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      const message = err instanceof Error ? err.message : '删除题库失败';
      ElMessage.error(message);
    }
  }

  async function exportSingleBankMarkdown(bank: any) {
    try {
      ElMessage.info(`正在准备导出「${bank.name}」试题集...`);
      const res = await getQuestionBankDetail(bank.id);
      const detail = res.data;
      const rawQuestions = detail?.questions && Array.isArray(detail.questions) ? detail.questions : [];
      const questions = normalizeQuestionList(rawQuestions as Record<string, unknown>[]);
      if (questions.length === 0) {
        ElMessage.warning(`题库「${bank.name}」内暂无收录试题`);
        return;
      }
      downloadMarkdownFile(
        bank.name || '题库精选试卷',
        questions,
        bank.courseName || getCourseName(bank.courseId)
      );
      ElMessage.success(`题库「${bank.name}」(${questions.length} 题) 已成功导出为 Markdown 试卷`);
    } catch (err: any) {
      ElMessage.error(err?.message || '导出题库试卷失败');
    }
  }

  function handleOpenEdit(bank: any) {
    editingBankId.value = bank.id;
    editBankForm.name = bank.name || '';
    editBankForm.courseId = bank.courseId;
    editBankForm.description = bank.description || '';
    showEditDialog.value = true;
  }

  async function handleSaveEdit() {
    if (!editDialogFormRef.value || !editingBankId.value) return;
    await editDialogFormRef.value.validate(async (valid) => {
      if (valid) {
        updating.value = true;
        try {
          await updateQuestionBank(editingBankId.value!, editBankForm);
          ElMessage.success('题库信息更新成功！');
          showEditDialog.value = false;
          await loadBanks();
        } catch (err: any) {
          ElMessage.error(err?.message || '更新题库失败');
        } finally {
          updating.value = false;
        }
      }
    });
  }

  async function handleCloneBank(bank: any) {
    try {
      ElMessage.info(`正在准备复制题库「${bank.name}」...`);
      const resDetail = await getQuestionBankDetail(bank.id);
      const rawQuestions = resDetail.data?.questions || [];
      const questionIds = rawQuestions.map((q: any) => q.id);
      const resCreate = await createQuestionBank({
        name: `${bank.name} - 副本`,
        courseId: bank.courseId,
        description: bank.description ? `${bank.description} (克隆副本)` : '题库克隆副本'
      });
      const newBankId = resCreate.data;
      if (questionIds.length > 0 && newBankId) {
        await addQuestionsToBank(newBankId, questionIds);
      }
      ElMessage.success(`题库「${bank.name}」克隆成功！共继承 ${questionIds.length} 道试题`);
      await loadBanks();
    } catch (err: any) {
      ElMessage.error(err?.message || '克隆题库失败');
    }
  }

  function handleFastCompose(bank: any) {
    router.push({
      path: '/question/exams/create',
      query: { bankId: bank.id, courseId: bank.courseId }
    });
  }

  function handleAiExpand(bank: any) {
    // AI 智能出题为教师模块，前置拦截避免被路由守卫弹「权限不足」并踢回工作台
    if (!canAccessRoute(router, '/ai/question/generate')) {
      ElMessage.warning('AI 智能扩题为教师专属功能');
      return;
    }
    router.push({
      path: '/ai/question/generate',
      query: {
        courseId: bank.courseId,
        targetBankId: bank.id,
        bankName: bank.name
      }
    });
  }

  onMounted(async () => {
    await Promise.all([loadCourses(), loadBanks()]);
  });

  return {
    router,
    loading,
    creating,
    updating,
    showCreateDialog,
    showEditDialog,
    editingBankId,
    dialogFormRef,
    editDialogFormRef,
    banks,
    filteredBanks,
    courses,
    coursesWithCounts,
    selectedCourseId,
    searchKeyword,
    sortBy,
    pageNum,
    pageSize,
    total,
    totalQuestionsAcrossBanks,
    newBankForm,
    editBankForm,
    dialogRules,
    getCourseBankCount,
    handleCourseFilter,
    getCourseName,
    loadCourses,
    loadBanks,
    handleCreateBank,
    handleOpenEdit,
    handleSaveEdit,
    handleCloneBank,
    handleFastCompose,
    handleAiExpand,
    handleDeleteBank,
    exportSingleBankMarkdown
  };
}

export function useBank() {
  const route = useRoute();
  const router = useRouter();

  const bankId = computed(() => Number(route.params.id) || 1);
  const loading = ref(false);
  const bankInfo = ref<any>(null);

  const bankQuestions = ref<QuestionItem[]>([]);
  const searchKeyword = ref('');
  const filterType = ref('');
  const filterDifficulty = ref('');
  const selectedRowKeys = ref<(number | string)[]>([]);
  const expandedAnalyses = ref<(number | string)[]>([]);

  const drawerVisible = ref(false);
  const drawerSearch = ref('');
  const drawerType = ref('');
  const candidatePool = ref<QuestionItem[]>([]);
  const selectedCandidateIds = ref<(number | string)[]>([]);
  const addingLoading = ref(false);

  const detailModalVisible = ref(false);
  const activeQuestion = ref<QuestionItem | null>(null);

  const totalScore = computed(() => calculateBankTotalScore(bankQuestions.value));

  const filteredQuestions = computed(() =>
    filterBankQuestions(bankQuestions.value, {
      searchKeyword: searchKeyword.value,
      filterType: filterType.value,
      filterDifficulty: filterDifficulty.value
    })
  );

  const candidateQuestions = computed(() =>
    filterCandidateQuestions(
      candidatePool.value,
      bankQuestions.value.map(q => q.id),
      {
        drawerSearch: drawerSearch.value,
        drawerType: drawerType.value
      }
    )
  );

  async function loadBankDetail() {
    loading.value = true;
    try {
      const res = await getQuestionBankDetail(bankId.value);
      bankInfo.value = res.data;
      if (bankInfo.value) {
        bankInfo.value.courseName = bankInfo.value.courseName || resolveCourseName(courses.value, bankInfo.value.courseId);
      }
      if (bankInfo.value?.questions && Array.isArray(bankInfo.value.questions)) {
        bankQuestions.value = normalizeQuestionList(bankInfo.value.questions as Record<string, unknown>[]);
      } else {
        bankQuestions.value = [];
      }
    } catch (err: any) {
      bankInfo.value = null;
      bankQuestions.value = [];
      ElMessage.error(err?.message || '加载题库详情失败，请检查网络或后端状态');
    } finally {
      loading.value = false;
    }
  }

  async function loadCandidatePool() {
    try {
      const res = await getQuestions({ pageSize: 50 });
      candidatePool.value = normalizeQuestionList(res.data?.list || []);
    } catch (err: any) {
      candidatePool.value = [];
      ElMessage.error(err?.message || '获取试题池失败');
    }
  }

  function toggleSelectRow(id: number | string) {
    selectedRowKeys.value = toggleArraySelection(selectedRowKeys.value, id);
  }

  function toggleExpandAnalysis(id: number | string) {
    expandedAnalyses.value = toggleArraySelection(expandedAnalyses.value, id);
  }

  function viewDetailDialog(q: QuestionItem) {
    activeQuestion.value = q;
    detailModalVisible.value = true;
  }

  async function handleRemoveQuestion(id: number | string) {
    try {
      await removeQuestionFromBank(bankId.value, id);
      await loadBankDetail();
      selectedRowKeys.value = selectedRowKeys.value.filter(k => String(k) !== String(id));
      ElMessage.success('已成功从题库中移出该试题');
    } catch (err: any) {
      ElMessage.error(err?.message || '移出试题失败');
    }
  }

  async function handleBatchRemove() {
    const count = selectedRowKeys.value.length;
    if (count === 0) return;
    try {
      for (const qId of selectedRowKeys.value) {
        await removeQuestionFromBank(bankId.value, qId);
      }
      await loadBankDetail();
      selectedRowKeys.value = [];
      ElMessage.success(`已批量移出 ${count} 道试题`);
    } catch (err: any) {
      ElMessage.error(err?.message || '批量移出试题失败');
    }
  }

  function openAddDrawer() {
    selectedCandidateIds.value = [];
    drawerSearch.value = '';
    drawerType.value = '';
    drawerVisible.value = true;
  }

  function toggleCandidateSelect(id: number | string) {
    selectedCandidateIds.value = toggleArraySelection(selectedCandidateIds.value, id);
  }

  async function confirmAddQuestions() {
    if (selectedCandidateIds.value.length === 0) return;
    addingLoading.value = true;
    try {
      await addQuestionsToBank(bankId.value, selectedCandidateIds.value);
      await loadBankDetail();
      ElMessage.success(`成功添加 ${selectedCandidateIds.value.length} 道题目到当前题库！`);
      drawerVisible.value = false;
    } catch (err: any) {
      ElMessage.error(err?.message || '批量添加题目失败');
    } finally {
      addingLoading.value = false;
    }
  }

  function handleFastComposeExam() {
    router.push({
      path: '/question/exams/create',
      query: {
        bankId: bankId.value,
        courseId: bankInfo.value?.courseId,
        bankName: bankInfo.value?.name
      }
    });
  }

  async function handleDeleteBank() {
    const name = bankInfo.value?.name || '当前题库';
    try {
      await ElMessageBox.confirm(
        `确定删除题库「${name}」吗？删除后题库内关联将解除，且不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deleteQuestionBank(bankId.value);
      ElMessage.success('题库已删除');
      router.push('/question/banks');
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      const message = err instanceof Error ? err.message : '删除题库失败';
      ElMessage.error(message);
    }
  }

  const courses = ref<Course[]>([]);

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 100 });
      courses.value = (res.data?.list || []).map(c => ({
        ...c,
        id: Number(c.id),
        title: c.title || c.name || '未命名课程'
      }));
      if (bankInfo.value && (!bankInfo.value.courseName || bankInfo.value.courseName === '未指定课程')) {
        bankInfo.value.courseName = resolveCourseName(courses.value, bankInfo.value.courseId);
      }
    } catch {}
  }

  function getCourseName(courseId: number | string | undefined | null) {
    return resolveCourseName(courses.value, courseId);
  }

  function exportBankMarkdown() {
    if (bankQuestions.value.length === 0) {
      ElMessage.warning('题库中暂无试题可导出');
      return;
    }
    const cName = bankInfo.value?.courseName || getCourseName(bankInfo.value?.courseId);
    downloadMarkdownFile(
      bankInfo.value?.name || '题库精选试卷',
      bankQuestions.value,
      cName
    );
    ElMessage.success(`题库「${bankInfo.value?.name || '试题集'}」已成功导出为 Markdown 文件`);
  }

  function handleCreateNewQuestion() {
    router.push({
      path: '/question/create',
      query: {
        bankId: bankId.value,
        courseId: bankInfo.value?.courseId,
        bankName: bankInfo.value?.name
      }
    });
  }

  function handleAiExpand() {
    // 同上：AI 出题属教师模块，学生点击前先给出明确说明
    if (!canAccessRoute(router, '/ai/question/generate')) {
      ElMessage.warning('AI 智能扩题为教师专属功能');
      return;
    }
    const cId = bankInfo.value?.courseId || '';
    const bId = bankId.value;
    router.push(`/ai/question/generate?courseId=${cId}&targetBankId=${bId}`);
  }

  onMounted(async () => {
    await loadCourses();
    await Promise.all([loadBankDetail(), loadCandidatePool()]);
    if (bankInfo.value && (!bankInfo.value.courseName || bankInfo.value.courseName === '未指定课程')) {
      bankInfo.value.courseName = resolveCourseName(courses.value, bankInfo.value.courseId);
    }
  });

  return {
    router,
    bankId,
    loading,
    bankInfo,
    courses,
    bankQuestions,
    searchKeyword,
    filterType,
    filterDifficulty,
    selectedRowKeys,
    expandedAnalyses,
    drawerVisible,
    drawerSearch,
    drawerType,
    candidatePool,
    selectedCandidateIds,
    addingLoading,
    detailModalVisible,
    activeQuestion,
    totalScore,
    filteredQuestions,
    candidateQuestions,
    getCourseName,
    loadBankDetail,
    loadCandidatePool,
    toggleSelectRow,
    toggleExpandAnalysis,
    viewDetailDialog,
    handleRemoveQuestion,
    handleBatchRemove,
    openAddDrawer,
    toggleCandidateSelect,
    confirmAddQuestions,
    handleFastComposeExam,
    handleCreateNewQuestion,
    handleDeleteBank,
    exportBankMarkdown,
    handleAiExpand,
    getTypeLabel,
    getTypeTagType,
    getDifficultyLabel,
    getDifficultyTagType
  };
}
