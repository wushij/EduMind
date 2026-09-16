import { ref, computed, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  getQuestionBanks,
  createQuestionBank,
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
  bankQuestionIds: number[],
  filters: CandidateFilterState
): QuestionItem[] {
  const currentIds = new Set(bankQuestionIds);
  return pool.filter(q => {
    if (currentIds.has(q.id)) return false;
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

export function resolveCourseName(courses: Course[], courseId: number): string {
  const c = courses.find((item) => item.id === courseId);
  return c ? c.title : '专业核心课';
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
  const pageNum = ref(1);
  const pageSize = ref(10);
  const total = ref(0);

  const newBankForm = reactive({
    name: '',
    courseId: undefined as number | undefined,
    description: ''
  });

  const dialogRules: FormRules = {
    name: [{ required: true, message: '请输入题库名称', trigger: 'blur' }],
    courseId: [{ required: true, message: '请选择关联课程', trigger: 'change' }]
  };

  function handleCourseFilter(courseId: number | null) {
    selectedCourseId.value = courseId;
    pageNum.value = 1;
    loadBanks();
  }

  function getCourseName(courseId: number) {
    return resolveCourseName(courses.value, courseId);
  }

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      courses.value = res.data?.list || [];
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

  onMounted(async () => {
    await Promise.all([loadCourses(), loadBanks()]);
  });

  return {
    router,
    loading,
    creating,
    showCreateDialog,
    dialogFormRef,
    banks,
    courses,
    selectedCourseId,
    pageNum,
    pageSize,
    total,
    newBankForm,
    dialogRules,
    handleCourseFilter,
    getCourseName,
    loadCourses,
    loadBanks,
    handleCreateBank,
    handleDeleteBank
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
  const selectedRowKeys = ref<number[]>([]);
  const expandedAnalyses = ref<number[]>([]);

  const drawerVisible = ref(false);
  const drawerSearch = ref('');
  const drawerType = ref('');
  const candidatePool = ref<QuestionItem[]>([]);
  const selectedCandidateIds = ref<number[]>([]);
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

  function toggleSelectRow(id: number) {
    selectedRowKeys.value = toggleArraySelection(selectedRowKeys.value, id);
  }

  function toggleExpandAnalysis(id: number) {
    expandedAnalyses.value = toggleArraySelection(expandedAnalyses.value, id);
  }

  function viewDetailDialog(q: QuestionItem) {
    activeQuestion.value = q;
    detailModalVisible.value = true;
  }

  async function handleRemoveQuestion(id: number) {
    try {
      await removeQuestionFromBank(bankId.value, id);
      await loadBankDetail();
      selectedRowKeys.value = selectedRowKeys.value.filter(k => k !== id);
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

  function toggleCandidateSelect(id: number) {
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
      query: { bankId: bankId.value, courseId: bankInfo.value?.courseId }
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

  onMounted(async () => {
    await loadBankDetail();
    await loadCandidatePool();
  });

  return {
    router,
    bankId,
    loading,
    bankInfo,
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
    handleDeleteBank,
    getTypeLabel,
    getTypeTagType,
    getDifficultyLabel,
    getDifficultyTagType
  };
}
