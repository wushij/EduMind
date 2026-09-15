import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getCourseKnowledgePoints, createKnowledgePoint, deleteKnowledgePoint } from '@/api/course/knowledge-point';
import { getChapters } from '@/api/course/chapter';

export function useKnowledgePoint() {
  const route = useRoute();
  const router = useRouter();
  const courseId = computed(() => Number(route.params.id) || 101);

  const loading = ref(false);
  const creating = ref(false);
  const showCreateDrawer = ref(false);
  const showGraphDrawer = ref(false);
  const selectedGraphKp = ref<any>(null);

  const searchKeyword = ref('');
  const selectedChapterId = ref<number | undefined>(undefined);
  const selectedLevel = ref('');

  const chapters = ref<any[]>([]);
  const knowledgePoints = ref<any[]>([]);

  const newKp = reactive({
    title: '',
    chapterId: 1,
    cognitiveDimension: 'APPLY',
    importance: 4,
    description: ''
  });

  onMounted(async () => {
    await Promise.all([loadChapters(), loadKnowledgePoints()]);
  });

  async function loadChapters() {
    try {
      const res = await getChapters(courseId.value);
      chapters.value = res.data || [];
    } catch (err) {
      console.warn('加载课程章节失败:', err);
      chapters.value = [];
    }
  }

  async function loadKnowledgePoints() {
    loading.value = true;
    try {
      const res = await getCourseKnowledgePoints(courseId.value, selectedChapterId.value);
      knowledgePoints.value = res.data || [];
    } catch (err: any) {
      ElMessage.error(err?.message || '获取课程知识点失败');
      knowledgePoints.value = [];
    } finally {
      loading.value = false;
    }
  }

  const filteredPoints = computed(() => {
    return knowledgePoints.value.filter(kp => {
      if (selectedChapterId.value && kp.chapterId !== selectedChapterId.value) return false;
      if (selectedLevel.value && kp.cognitiveDimension !== selectedLevel.value) return false;
      if (searchKeyword.value.trim()) {
        const kw = searchKeyword.value.trim().toLowerCase();
        const inTitle = (kp.title || kp.name || '').toLowerCase().includes(kw);
        const inCode = (kp.code || '').toLowerCase().includes(kw);
        if (!inTitle && !inCode) return false;
      }
      return true;
    });
  });

  function getChapterTitle(chapterId?: number) {
    const c = chapters.value.find(item => item.id === chapterId);
    return c?.title || '通用教学大纲';
  }

  function getPointsForChapter(chapterId: number) {
    return knowledgePoints.value.filter(k => k.chapterId === chapterId || (!k.chapterId && chapterId === 1));
  }

  function getLevelLabel(level?: string) {
    const map: Record<string, string> = {
      REMEMBER: '识记概念',
      UNDERSTAND: '理解领会',
      APPLY: '实践应用',
      ANALYZE: '综合探究'
    };
    return map[level || 'APPLY'] || '核心要点';
  }

  function getLevelTagType(level?: string) {
    const map: Record<string, string> = {
      REMEMBER: 'info',
      UNDERSTAND: 'primary',
      APPLY: 'success',
      ANALYZE: 'warning'
    };
    return (map[level || 'APPLY'] as any) || '';
  }

  async function handleSaveNewKp() {
    if (!newKp.title.trim()) {
      ElMessage.warning('知识点名称不能为空');
      return;
    }
    creating.value = true;
    try {
      await createKnowledgePoint(courseId.value, {
        chapterId: newKp.chapterId,
        title: newKp.title.trim(),
        sortOrder: newKp.importance || 0
      });
      ElMessage.success('知识点录入成功并已持久化入库！');
      showCreateDrawer.value = false;
      newKp.title = '';
      newKp.description = '';
      await loadKnowledgePoints();
    } catch (err: any) {
      ElMessage.error(err?.message || '新增知识点失败，请稍后重试');
    } finally {
      creating.value = false;
    }
  }

  function openGraphDrawer(kp: any) {
    selectedGraphKp.value = kp || knowledgePoints.value[0] || null;
    showGraphDrawer.value = true;
  }

  function handleAskAi(kp: any) {
    router.push({
      path: `/course/${courseId.value}/ai`,
      query: { prompt: `请结合本课程知识图谱，详细讲解核心考点【${kp.title || kp.name}】的定义、推导与常见考查题型。` }
    });
  }

  function handleGenerateQuizForKp(kp: any) {
    router.push(`/ai/question/generate?courseId=${courseId.value}&kp=${encodeURIComponent(kp.title || kp.name)}`);
  }

  async function handleDeleteKp(kp: any) {
    try {
      await deleteKnowledgePoint(courseId.value, kp.id);
      ElMessage.success(`知识点【${kp.title || kp.name}】已成功删除`);
      await loadKnowledgePoints();
    } catch (err: any) {
      ElMessage.error(err?.message || '删除知识点失败');
    }
  }

  return {
    loading,
    creating,
    showCreateDrawer,
    showGraphDrawer,
    selectedGraphKp,
    searchKeyword,
    selectedChapterId,
    selectedLevel,
    chapters,
    knowledgePoints,
    newKp,
    filteredPoints,
    loadKnowledgePoints,
    getChapterTitle,
    getPointsForChapter,
    getLevelLabel,
    getLevelTagType,
    handleSaveNewKp,
    openGraphDrawer,
    handleAskAi,
    handleGenerateQuizForKp,
    handleDeleteKp
  };
}
