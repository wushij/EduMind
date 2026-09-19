import { ref, reactive, computed, onMounted, type Ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getCourseKnowledgePoints,
  createKnowledgePoint,
  updateKnowledgePoint,
  deleteKnowledgePoint
} from '@/api/course/knowledge-point';
import { suggestCourseKnowledgePoints } from '@/api/ai/course-knowledge-points';
import { getChapters } from '@/api/course/chapter';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import type { Course } from '@/types/course/course';
import type { CourseKnowledgePointSuggestItem, KnowledgePoint } from '@/types/course/knowledge-point';
import { mapSuggestItemToSave, resolvePrerequisiteIds } from '@/services/course/knowledge-point-import';
import { isAxiosError } from 'axios';

export function useKnowledgePoint(courseRef?: Ref<Course | null | undefined>) {
  const route = useRoute();
  const router = useRouter();
  const teachingCopilotStore = useTeachingCopilotStore();
  const courseId = computed(() => Number(route.params.id) || courseRef?.value?.id || 101);

  const loading = ref(false);
  const creating = ref(false);
  const showFormDrawer = ref(false);
  const showDetailDrawer = ref(false);
  const showGraphDrawer = ref(false);
  const selectedGraphKp = ref<KnowledgePoint | null>(null);
  const detailKp = ref<KnowledgePoint | null>(null);
  const editingKpId = ref<number | null>(null);

  const searchKeyword = ref('');
  const selectedChapterId = ref<number | undefined>(undefined);
  const selectedLevel = ref('');

  const chapters = ref<any[]>([]);
  const knowledgePoints = ref<KnowledgePoint[]>([]);

  const newKp = reactive({
    title: '',
    chapterId: 0,
    code: '',
    cognitiveDimension: 'APPLY',
    importance: 4,
    description: '',
    prerequisiteIds: [] as number[],
    examFocus: ''
  });

  const showAiSuggestModal = ref(false);
  const aiExtracting = ref(false);
  const aiSuggestedPoints = ref<CourseKnowledgePointSuggestItem[]>([]);
  let aiSuggestAbortController: AbortController | null = null;

  const formDrawerTitle = computed(() => (editingKpId.value ? '编辑课程核心知识点' : '录入课程核心知识点'));

  onMounted(async () => {
    await Promise.all([loadChapters(), loadKnowledgePoints()]);
  });

  async function loadChapters() {
    try {
      const res = await getChapters(courseId.value);
      chapters.value = res.data || [];
      if (chapters.value.length > 0 && (!newKp.chapterId || newKp.chapterId === 0)) {
        newKp.chapterId = chapters.value[0].id;
      }
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

  function resetForm() {
    editingKpId.value = null;
    newKp.title = '';
    newKp.code = '';
    newKp.cognitiveDimension = 'APPLY';
    newKp.importance = 4;
    newKp.description = '';
    newKp.examFocus = '';
    newKp.prerequisiteIds = [];
    newKp.chapterId = chapters.value[0]?.id || selectedChapterId.value || 0;
  }

  function openCreateDrawer() {
    resetForm();
    showFormDrawer.value = true;
  }

  function openEditDrawer(kp: KnowledgePoint) {
    editingKpId.value = kp.id;
    newKp.title = kp.title || kp.name || '';
    newKp.chapterId = kp.chapterId || chapters.value[0]?.id || 0;
    newKp.code = kp.code || '';
    newKp.cognitiveDimension = kp.cognitiveDimension || 'APPLY';
    newKp.importance = kp.importance ?? 4;
    newKp.description = kp.description || '';
    newKp.examFocus = kp.examFocus || '';
    newKp.prerequisiteIds = [...(kp.prerequisiteIds || kp.prerequisites?.map((p) => p.id) || [])];
    showDetailDrawer.value = false;
    showFormDrawer.value = true;
  }

  function openDetailDrawer(kp: KnowledgePoint) {
    detailKp.value = kp;
    showDetailDrawer.value = true;
  }

  function openAiSuggestModal() {
    showAiSuggestModal.value = true;
    generateAiSuggestedPoints();
  }

  function abortAiSuggestedPoints() {
    if (aiSuggestAbortController) {
      aiSuggestAbortController.abort();
      aiSuggestAbortController = null;
    }
    aiExtracting.value = false;
  }

  async function generateAiSuggestedPoints() {
    abortAiSuggestedPoints();
    aiSuggestAbortController = new AbortController();
    aiExtracting.value = true;
    aiSuggestedPoints.value = [];
    const targetChapterId = newKp.chapterId || selectedChapterId.value || chapters.value[0]?.id;
    try {
      const res = await suggestCourseKnowledgePoints(
        courseId.value,
        targetChapterId,
        4,
        { signal: aiSuggestAbortController.signal }
      );
      const data = res.data;
      if (data?.points?.length) {
        aiSuggestedPoints.value = data.points;
        const label = data.sourceLabel || (data.aiGenerated ? '模型推演已生成' : '智能兜底');
        ElMessage.success(`${label}：共 ${data.points.length} 个考点`);
      } else {
        throw new Error('未返回有效考点');
      }
    } catch (err: unknown) {
      if (isAxiosError(err) && err.code === 'ERR_CANCELED') {
        return;
      }
      aiSuggestedPoints.value = [];
      const msg = err instanceof Error ? err.message : 'AI 考点提炼失败，请稍后重试';
      ElMessage.error(msg);
    } finally {
      aiSuggestAbortController = null;
      aiExtracting.value = false;
    }
  }

  function applyAiSuggestedPoint(point: CourseKnowledgePointSuggestItem) {
    const chapterId = newKp.chapterId || selectedChapterId.value || chapters.value[0]?.id || 0;
    const prereqIds = resolvePrerequisiteIds(point.prerequisiteTitles, knowledgePoints.value);
    newKp.title = point.title;
    newKp.chapterId = chapterId;
    newKp.code = '';
    newKp.cognitiveDimension = point.cognitiveDimension || 'APPLY';
    newKp.importance = point.importance ?? 4;
    newKp.description = point.description || '';
    newKp.examFocus = point.examFocus || '';
    newKp.prerequisiteIds = prereqIds;
    editingKpId.value = null;
    showAiSuggestModal.value = false;
    showFormDrawer.value = true;
    ElMessage.success('已采纳推荐考点，可继续编辑后保存');
  }

  async function batchImportAiPoints(points: CourseKnowledgePointSuggestItem[]) {
    if (!points?.length) return;
    creating.value = true;
    const chapterId = newKp.chapterId || selectedChapterId.value || chapters.value[0]?.id || 0;
    try {
      let imported = 0;
      for (const p of points) {
        await loadKnowledgePoints();
        const prereqIds = resolvePrerequisiteIds(p.prerequisiteTitles, knowledgePoints.value);
        await createKnowledgePoint(
          courseId.value,
          mapSuggestItemToSave(p, chapterId, prereqIds)
        );
        imported++;
      }
      ElMessage.success(`已入库 ${imported} 个核心考点`);
      showAiSuggestModal.value = false;
      await loadKnowledgePoints();
    } catch (err: any) {
      ElMessage.error(err?.message || '批量录入知识点失败');
    } finally {
      creating.value = false;
    }
  }

  const prerequisiteOptions = computed(() =>
    knowledgePoints.value
      .filter((kp) => kp.id !== editingKpId.value)
      .map((kp) => ({
        value: kp.id,
        label: kp.title || kp.name || `知识点 ${kp.id}`
      }))
  );

  const filteredPoints = computed(() => {
    return knowledgePoints.value.filter((kp) => {
      if (selectedChapterId.value && kp.chapterId !== selectedChapterId.value) return false;
      if (selectedLevel.value && kp.cognitiveDimension !== selectedLevel.value) return false;
      if (searchKeyword.value.trim()) {
        const kw = searchKeyword.value.trim().toLowerCase();
        const inTitle = (kp.title || kp.name || '').toLowerCase().includes(kw);
        const inCode = (kp.code || '').toLowerCase().includes(kw);
        const inDesc = (kp.description || '').toLowerCase().includes(kw);
        const inFocus = (kp.examFocus || '').toLowerCase().includes(kw);
        if (!inTitle && !inCode && !inDesc && !inFocus) return false;
      }
      return true;
    });
  });

  function getChapterTitle(chapterId?: number) {
    const c = chapters.value.find((item) => item.id === chapterId);
    return c?.title || '未指定章节';
  }

  function getPointsForChapter(chapterId: number) {
    return knowledgePoints.value.filter((k) => k.chapterId === chapterId);
  }

  function getLevelLabel(level?: string) {
    if (!level) return '未设置';
    const map: Record<string, string> = {
      REMEMBER: '识记概念',
      UNDERSTAND: '理解领会',
      APPLY: '实践应用',
      ANALYZE: '综合探究'
    };
    return map[level] || '核心要点';
  }

  function getLevelTagType(level?: string) {
    if (!level) return 'info';
    const map: Record<string, string> = {
      REMEMBER: 'info',
      UNDERSTAND: 'primary',
      APPLY: 'success',
      ANALYZE: 'warning'
    };
    return (map[level] as any) || 'info';
  }

  function buildSavePayload() {
    return {
      chapterId: newKp.chapterId,
      title: newKp.title.trim(),
      code: newKp.code.trim() || undefined,
      description: newKp.description.trim() || undefined,
      cognitiveDimension: newKp.cognitiveDimension,
      importance: newKp.importance,
      examFocus: newKp.examFocus.trim() || undefined,
      sortOrder: newKp.importance || 0,
      prerequisiteIds: newKp.prerequisiteIds
    };
  }

  async function handleSaveKp() {
    if (!newKp.title.trim()) {
      ElMessage.warning('知识点名称不能为空');
      return;
    }
    creating.value = true;
    try {
      const payload = buildSavePayload();
      if (editingKpId.value) {
        await updateKnowledgePoint(courseId.value, editingKpId.value, payload);
        ElMessage.success('知识点已更新');
      } else {
        await createKnowledgePoint(courseId.value, payload);
        ElMessage.success('知识点录入成功');
      }
      showFormDrawer.value = false;
      resetForm();
      await loadKnowledgePoints();
    } catch (err: any) {
      ElMessage.error(err?.message || '保存知识点失败');
    } finally {
      creating.value = false;
    }
  }

  function openGraphDrawer(kp: KnowledgePoint | null) {
    selectedGraphKp.value = kp || knowledgePoints.value[0] || null;
    showGraphDrawer.value = true;
  }

  function handleAskAi(kp: KnowledgePoint) {
    showGraphDrawer.value = false;
    const title = kp.title || kp.name || '核心考点';
    const courseName = courseRef?.value?.title || courseRef?.value?.name || '';
    const prompt = `请结合本课程${courseName ? `「${courseName}」` : ''}知识图谱，详细讲解核心考点【${title}】的定义、推导与常见考查题型。`;
    const excerpt = [
      `知识点：${title}`,
      kp.code ? `编码：${kp.code}` : '',
      `所属章节：${getChapterTitle(kp.chapterId)}`,
      kp.cognitiveDimension ? `认知维度：${getLevelLabel(kp.cognitiveDimension)}` : '',
      kp.description ? `说明：${kp.description}` : '',
      kp.examFocus ? `考查重点：${kp.examFocus}` : ''
    ]
      .filter(Boolean)
      .join('\n');

    teachingCopilotStore.openAssistantWithContext(
      {
        contextModule: 'course_space',
        courseId: courseId.value,
        title: `考点：${title}`,
        description: kp.description || '',
        draftExcerpt: excerpt
      },
      prompt,
      { autoSend: true }
    );
  }

  function handleGenerateQuizForKp(kp: KnowledgePoint) {
    router.push(`/ai/question/generate?courseId=${courseId.value}&kp=${encodeURIComponent(kp.title || kp.name || '')}`);
  }

  async function handleDeleteKp(kp: KnowledgePoint) {
    try {
      await deleteKnowledgePoint(courseId.value, kp.id);
      ElMessage.success(`知识点「${kp.title || kp.name}」已删除`);
      if (detailKp.value?.id === kp.id) {
        showDetailDrawer.value = false;
        detailKp.value = null;
      }
      await loadKnowledgePoints();
    } catch (err: any) {
      ElMessage.error(err?.message || '删除知识点失败');
    }
  }

  async function confirmDeleteKp(kp: KnowledgePoint) {
    try {
      await ElMessageBox.confirm(
        `确定要删除核心考点「${kp.title || kp.name}」吗？删除后将从知识图谱与课程考点中移除。`,
        '删除确认',
        {
          type: 'warning',
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          confirmButtonClass: 'el-button--danger',
          lockScroll: false
        }
      );
      await handleDeleteKp(kp);
    } catch {
      // cancel
    }
  }

  return {
    courseId,
    loading,
    creating,
    showFormDrawer,
    showDetailDrawer,
    showGraphDrawer,
    showCreateDrawer: showFormDrawer,
    selectedGraphKp,
    detailKp,
    formDrawerTitle,
    searchKeyword,
    selectedChapterId,
    selectedLevel,
    chapters,
    knowledgePoints,
    newKp,
    prerequisiteOptions,
    filteredPoints,
    showAiSuggestModal,
    aiExtracting,
    aiSuggestedPoints,
    openAiSuggestModal,
    openCreateDrawer,
    openEditDrawer,
    openDetailDrawer,
    generateAiSuggestedPoints,
    abortAiSuggestedPoints,
    applyAiSuggestedPoint,
    batchImportAiPoints,
    loadKnowledgePoints,
    getChapterTitle,
    getPointsForChapter,
    getLevelLabel,
    getLevelTagType,
    handleSaveKp,
    handleSaveNewKp: handleSaveKp,
    openGraphDrawer,
    handleAskAi,
    handleGenerateQuizForKp,
    handleDeleteKp,
    confirmDeleteKp
  };
}
