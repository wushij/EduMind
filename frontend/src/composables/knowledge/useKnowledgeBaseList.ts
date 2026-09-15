import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Monitor,
  Reading,
  Files,
  Grid
} from '@element-plus/icons-vue';
import { useKnowledgeBase } from '@/composables/knowledge/useKnowledgeBase';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import kbBannerImg from '@/assets/images/知识库banner.png';

export function useKnowledgeBaseList() {
  const router = useRouter();
  const { knowledgeBases, fetchKnowledgeBases, create, remove } = useKnowledgeBase();

  onMounted(async () => {
    try {
      await fetchKnowledgeBases();
    } catch {
      // axios 拦截器已弹出错误提示
    }
  });

  const selectedCategory = ref<string>('ALL');
  const searchKeyword = ref<string>('');
  const showCreateDialog = ref(false);

  const categoryTabs = [
    { label: '全部知识库', value: 'ALL', icon: Grid },
    { label: '专业核心', value: 'MAJOR', icon: Monitor },
    { label: '公卡通识', value: 'COMMON', icon: Reading },
    { label: '历年真题', value: 'EXAM', icon: Files }
  ];

  const totalDocs = computed(() => {
    return knowledgeBases.value.reduce((sum, item) => sum + item.documentCount, 0);
  });

  const totalChunks = computed(() => {
    return knowledgeBases.value.reduce((sum, item) => sum + item.chunkCount, 0);
  });

  function getCategoryCount(cat: string) {
    if (cat === 'ALL') return knowledgeBases.value.length;
    return knowledgeBases.value.filter((item) => item.category === cat).length;
  }

  const filteredList = computed(() => {
    let list = [...knowledgeBases.value];

    if (selectedCategory.value !== 'ALL') {
      list = list.filter((item) => item.category === selectedCategory.value);
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

  function resetFilters() {
    selectedCategory.value = 'ALL';
    searchKeyword.value = '';
  }

  function handleOpenDetail(item: KnowledgeBase) {
    router.push(`/knowledge/${item.id}/documents`);
  }

  function handleUploadDoc(item: KnowledgeBase) {
    router.push(`/knowledge/${item.id}/documents`);
  }

  async function handleDeleteKb(item: KnowledgeBase) {
    try {
      await remove(item.id);
      ElMessage.success(`知识库《${item.name}》已移除`);
    } catch {
      ElMessage.error('删除知识库失败，请稍后重试');
    }
  }

  const createForm = reactive({
    name: '',
    category: 'MAJOR',
    courseName: '高等数学（上）',
    embeddingModel: 'bge-large-zh-v1.5 (1024维)',
    description: ''
  });

  const createRules = {
    name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
  };

  async function handleConfirmCreate() {
    if (!createForm.name.trim()) {
      ElMessage.warning('请输入知识库名称');
      return;
    }

    const courseIdMap: Record<string, number> = {
      '高等数学（上）': 101,
      '数据结构与算法': 102
    };
    try {
      await create({
        name: createForm.name.trim(),
        description: createForm.description.trim(),
        courseId: courseIdMap[createForm.courseName]
      });
      await fetchKnowledgeBases();
      showCreateDialog.value = false;
      ElMessage.success('知识库创建成功');
      createForm.name = '';
      createForm.description = '';
    } catch {
      ElMessage.error('创建知识库失败，请稍后重试');
    }
  }

  return {
    kbBannerImg,
    knowledgeBases,
    selectedCategory,
    searchKeyword,
    showCreateDialog,
    categoryTabs,
    totalDocs,
    totalChunks,
    filteredList,
    createForm,
    createRules,
    getCategoryCount,
    resetFilters,
    handleOpenDetail,
    handleUploadDoc,
    handleDeleteKb,
    handleConfirmCreate
  };
}
