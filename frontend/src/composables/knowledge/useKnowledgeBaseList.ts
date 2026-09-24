import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Monitor,
  Reading,
  Files,
  Grid,
  Document
} from '@element-plus/icons-vue';
import { useKnowledgeBase } from '@/composables/knowledge/useKnowledgeBase';
import { getCourseList } from '@/api/course/course';
import { triggerReindex } from '@/api/knowledge/embedding';
import { getKnowledgeRagDashboardStats } from '@/api/knowledge/rag-dashboard';
import {
  getStoredKnowledgeBaseId,
  setStoredKnowledgeBaseId,
  clearStoredKnowledgeBaseId
} from '@/composables/knowledge/useKnowledgeRoute';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import type { Course } from '@/types/course/course';
import type { KnowledgeRagDashboardVO } from '@/types/knowledge/rag-dashboard';

export function useKnowledgeBaseList() {
  const router = useRouter();
  const { knowledgeBases, loading, fetchKnowledgeBases, create, update, remove } = useKnowledgeBase();

  const courses = ref<Array<{ id: number; title: string }>>([]);
  const selectedCategory = ref<string>('ALL');
  const selectedStatus = ref<string>('ALL');
  const searchKeyword = ref<string>('');
  const viewMode = ref<'card' | 'table'>('card');
  const lastUpdatedText = ref<string>('刚刚更新');
  const ragStats = ref<KnowledgeRagDashboardVO | null>(null);
  const ragStatsLoading = ref(false);

  // 弹窗与抽屉状态
  const showCreateDialog = ref(false);
  const isEditMode = ref(false);
  const editingKbId = ref<number | null>(null);

  const showUploadDialog = ref(false);
  const uploadTargetKbId = ref<number | undefined>(undefined);

  const showRetrievalDrawer = ref(false);
  const retrievalInitialKbId = ref<number | undefined>(undefined);

  const categoryTabs = [
    { label: '全部知识库', value: 'ALL', icon: Grid },
    { label: '专业核心', value: 'MAJOR', icon: Monitor },
    { label: '公卡通识', value: 'COMMON', icon: Reading },
    { label: '历年真题', value: 'EXAM', icon: Files },
    { label: '教学课件', value: 'COURSEWARE', icon: Document }
  ];

  const statusOptions = [
    { label: '全部状态', value: 'ALL' },
    { label: '正常就绪 (SYNCED)', value: 'SYNCED' },
    { label: '解析构建中 (PARSING)', value: 'PARSING' },
    { label: '待处理 (PENDING)', value: 'PENDING' }
  ];

  // 统计指标
  const totalDocs = computed(() => {
    return knowledgeBases.value.reduce((sum, item) => sum + (item.documentCount || 0), 0);
  });

  const totalChunks = computed(() => {
    return knowledgeBases.value.reduce((sum, item) => sum + (item.chunkCount || 0), 0);
  });

  const activeCoursesCount = computed(() => {
    const courseIds = new Set(
      knowledgeBases.value.map((item) => item.courseId).filter(Boolean)
    );
    return Math.max(1, courseIds.size);
  });

  function getCategoryCount(cat: string) {
    if (cat === 'ALL') return knowledgeBases.value.length;
    return knowledgeBases.value.filter((item) => item.category === cat).length;
  }

  // 过滤列表
  const filteredList = computed(() => {
    let list = [...knowledgeBases.value];

    if (selectedCategory.value !== 'ALL') {
      list = list.filter((item) => item.category === selectedCategory.value);
    }

    if (selectedStatus.value !== 'ALL') {
      list = list.filter((item) => item.vectorStatus === selectedStatus.value);
    }

    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      list = list.filter(
        (item) =>
          item.name.toLowerCase().includes(kw) ||
          item.description.toLowerCase().includes(kw) ||
          (item.courseName && item.courseName.toLowerCase().includes(kw))
      );
    }

    return list;
  });

  function formatNumber(val: number): string {
    return (val || 0).toLocaleString();
  }

  function resetFilters() {
    selectedCategory.value = 'ALL';
    selectedStatus.value = 'ALL';
    searchKeyword.value = '';
  }

  // 加载系统真实课程
  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 100 });
      const list = res.data?.list || [];
      courses.value = list.map((c: Course) => ({
        id: c.id,
        title: c.title || c.name || `课程 #${c.id}`
      }));
    } catch {
      // 课程接口不可用时不再伪造课程列表（原先写死 101/102/103），
      // 避免用户误以为这些课程真实可选，把知识库绑到并不存在的课程上。
      courses.value = [];
    }
  }

  async function fetchRagDashboardStats() {
    ragStatsLoading.value = true;
    try {
      const res = await getKnowledgeRagDashboardStats();
      ragStats.value = res?.data ?? null;
    } catch {
      ragStats.value = null;
    } finally {
      ragStatsLoading.value = false;
    }
  }

  // 刷新全量数据
  async function handleRefresh(notify = true) {
    try {
      await Promise.all([fetchKnowledgeBases(), fetchRagDashboardStats()]);
      const now = new Date();
      lastUpdatedText.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;

      // 校验当前存储的 knowledgeBaseId 是否仍有效，若失效则智能纠偏到第 1 个可用知识库
      const currentStoredId = getStoredKnowledgeBaseId();
      if (knowledgeBases.value.length > 0) {
        const stillExists = currentStoredId && knowledgeBases.value.some((k) => k.id === currentStoredId);
        if (!stillExists) {
          setStoredKnowledgeBaseId(knowledgeBases.value[0].id);
        }
      } else {
        clearStoredKnowledgeBaseId();
      }

      if (notify) {
        ElMessage.success('知识库与向量索引数据已同步更新');
      }
    } catch {
      // 拦截器已处理错误
    }
  }

  // 页面导航与快速抽屉
  function handleOpenDetail(item: KnowledgeBase) {
    router.push(`/knowledge/${item.id}/documents`);
  }

  function handleUploadDoc(item: KnowledgeBase) {
    uploadTargetKbId.value = item.id;
    showUploadDialog.value = true;
  }

  function handleOpenRetrieval(item?: KnowledgeBase) {
    retrievalInitialKbId.value = item?.id;
    showRetrievalDrawer.value = true;
  }

  // 新建知识库弹窗表单
  const createForm = reactive({
    name: '',
    category: 'MAJOR',
    courseId: undefined as number | undefined,
    courseName: '',
    embeddingModel: 'bge-large-zh-v1.5 (1024维)',
    description: ''
  });

  const createRules = {
    name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
  };

  function handleOpenCreate() {
    isEditMode.value = false;
    editingKbId.value = null;
    createForm.name = '';
    createForm.category = 'MAJOR';
    createForm.courseId = courses.value.length > 0 ? courses.value[0].id : undefined;
    createForm.embeddingModel = 'bge-large-zh-v1.5 (1024维)';
    createForm.description = '';
    showCreateDialog.value = true;
  }

  function handleEditKb(item: KnowledgeBase) {
    isEditMode.value = true;
    editingKbId.value = item.id;
    createForm.name = item.name;
    createForm.category = item.category || 'MAJOR';
    createForm.courseId = item.courseId;
    createForm.embeddingModel = item.embeddingModel || 'bge-large-zh-v1.5 (1024维)';
    createForm.description = item.description || '';
    showCreateDialog.value = true;
  }

  async function handleConfirmSave() {
    if (!createForm.name.trim()) {
      ElMessage.warning('请输入知识库名称');
      return;
    }

    try {
      if (isEditMode.value && editingKbId.value) {
        await update(editingKbId.value, {
          name: createForm.name.trim(),
          description: createForm.description.trim(),
          courseId: createForm.courseId
        });
        ElMessage.success('知识库基本信息已更新');
      } else {
        await create({
          name: createForm.name.trim(),
          description: createForm.description.trim(),
          courseId: createForm.courseId
        });
        ElMessage.success('新知识库创建成功');
      }

      await fetchKnowledgeBases();
      showCreateDialog.value = false;
    } catch {
      ElMessage.error(isEditMode.value ? '更新知识库失败' : '创建知识库失败，请稍后重试');
    }
  }

  // 重新构建向量索引
  async function handleTriggerIndex(item: KnowledgeBase) {
    try {
      await ElMessageBox.confirm(
        `确定要为知识库《${item.name}》重新触发全量向量化索引构建吗？已有切片将重新计算 Embedding 向量。`,
        '全量重建索引确认',
        {
          confirmButtonText: '立即构建',
          cancelButtonText: '取消',
          type: 'info'
        }
      );

      item.vectorStatus = 'PARSING';
      item.vectorStatusLabel = '解析中';
      await triggerReindex(item.id, 'FULL');
      ElMessage.success(`知识库《${item.name}》全量向量索引构建任务已提交`);
      setTimeout(() => {
        fetchKnowledgeBases();
      }, 1500);
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('触发重新索引失败');
      }
    }
  }

  // 删除知识库
  async function handleDeleteKb(item: KnowledgeBase) {
    try {
      await ElMessageBox.confirm(
        `确定要彻底移除知识库《${item.name}》吗？移除后已入库的文档及向量切片将无法通过 AI 助教检索。`,
        '移除知识库确认',
        {
          confirmButtonText: '确认移除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      );

      await remove(item.id);

      // 同步校准全局选中的知识库 ID
      const currentStoredId = getStoredKnowledgeBaseId();
      if (currentStoredId === item.id) {
        const remaining = knowledgeBases.value.filter((k) => k.id !== item.id);
        if (remaining.length > 0) {
          setStoredKnowledgeBaseId(remaining[0].id);
        } else {
          clearStoredKnowledgeBaseId();
        }
      }

      ElMessage.success(`知识库《${item.name}》已成功移除`);
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('删除知识库失败，请稍后重试');
      }
    }
  }

  onMounted(async () => {
    loadCourses();
    handleRefresh(false);
  });

  return {
    loading,
    knowledgeBases,
    courses,
    selectedCategory,
    selectedStatus,
    statusOptions,
    searchKeyword,
    viewMode,
    lastUpdatedText,
    showCreateDialog,
    isEditMode,
    showUploadDialog,
    uploadTargetKbId,
    showRetrievalDrawer,
    retrievalInitialKbId,
    categoryTabs,
    totalDocs,
    totalChunks,
    ragStats,
    ragStatsLoading,
    activeCoursesCount,
    filteredList,
    createForm,
    createRules,
    formatNumber,
    getCategoryCount,
    resetFilters,
    handleRefresh,
    handleOpenCreate,
    handleEditKb,
    handleConfirmSave,
    handleOpenDetail,
    handleUploadDoc,
    handleOpenRetrieval,
    handleTriggerIndex,
    handleDeleteKb
  };
}
