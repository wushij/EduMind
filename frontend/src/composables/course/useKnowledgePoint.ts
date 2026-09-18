import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCourseKnowledgePoints, createKnowledgePoint, deleteKnowledgePoint } from '@/api/course/knowledge-point';
import { getChapters } from '@/api/course/chapter';
import { askGlobalAssistant } from '@/api/ai/assistant';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';

export function useKnowledgePoint() {
  const route = useRoute();
  const router = useRouter();
  const teachingCopilotStore = useTeachingCopilotStore();
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
    chapterId: 0,
    code: '',
    cognitiveDimension: 'APPLY',
    importance: 4,
    description: '',
    prerequisites: [] as string[],
    examFocus: ''
  });

  const showAiSuggestModal = ref(false);
  const aiExtracting = ref(false);
  const aiSuggestedPoints = ref<any[]>([]);

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

  function openAiSuggestModal() {
    showAiSuggestModal.value = true;
    generateAiSuggestedPoints();
  }

  function parseJsonArray<T = any>(rawText: string): T[] {
    if (!rawText) return [];
    let text = rawText.trim();
    const codeBlockMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
    if (codeBlockMatch) {
      text = codeBlockMatch[1].trim();
    }
    const startIdx = text.indexOf('[');
    const endIdx = text.lastIndexOf(']');
    if (startIdx !== -1 && endIdx !== -1 && endIdx > startIdx) {
      text = text.slice(startIdx, endIdx + 1);
    }
    try {
      const parsed = JSON.parse(text);
      return Array.isArray(parsed) ? parsed : [];
    } catch (e) {
      console.error('Failed to parse AI knowledge points JSON:', e, rawText);
      return [];
    }
  }

  async function generateAiSuggestedPoints() {
    aiExtracting.value = true;
    aiSuggestedPoints.value = [];
    const targetChap = chapters.value.find(c => c.id === (newKp.chapterId || selectedChapterId.value)) || chapters.value[0];
    const chapTitle = targetChap?.title || '核心课程大纲';

    const prompt = `你是一名高校计算机专业课资深教研室主任及命题专家。请针对章节【${chapTitle}】，结合布鲁姆认知模型（识记/理解/应用/分析），提炼 3~4 个真实、具体、高频的核心考点/知识点。
【严格要求】：
1. 知识点名称必须针对【${chapTitle}】的具体专业学科内涵，严禁空泛通用的套话（例如切勿输出“状态迁移模型与拓扑演进”这种与本章无关的抽象名词）。
如果章节是“Java学习概述与核心认知模型”，考点应如：“JVM内存结构与垃圾回收机制基础”、“JDK核心工具链（javac/java/javap）编译执行流程”、“Java跨平台特性与字节码WORA机制剖析”；
如果章节涉及“数据结构与算法”，考点应如：“时间与空间渐进复杂度大O推导”、“双指针在有序数组中的移动策略与边界条件”等。
2. 包含认知维度（严格限制只能是：REMEMBER, UNDERSTAND, APPLY, ANALYZE 之一）、重要星级（3-5分）、简要说明（40字左右）、前置依赖知识点数组、常见考试易错陷阱。
3. 请严格以标准 JSON 数组格式直接返回，严禁任何代码块标记（不要输出 \`\`\`json ），格式如下：
[
  {
    "title": "具体考点名称",
    "cognitiveDimension": "APPLY",
    "importance": 5,
    "description": "考点内涵与掌握要求解析",
    "prerequisites": ["前置知识A", "前置知识B"],
    "examFocus": "常见考试题型与避坑重点"
  }
]`;

    try {
      const res = await askGlobalAssistant({
        message: prompt,
        courseId: courseId.value
      });
      const rawContent = res.data?.content || '';
      const parsed = parseJsonArray(rawContent);
      if (parsed.length > 0) {
        aiSuggestedPoints.value = parsed.map((item: any, idx: number) => ({
          title: item.title || `${chapTitle} 核心考点 ${idx + 1}`,
          chapterId: targetChap?.id || chapters.value[0]?.id || 1,
          code: `KP-${Math.floor(1000 + Math.random() * 9000)}`,
          cognitiveDimension: ['REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE'].includes(item.cognitiveDimension) ? item.cognitiveDimension : 'APPLY',
          importance: Math.min(5, Math.max(1, Number(item.importance) || 4)),
          description: item.description || '本章节高频核心考点，涉及原理推导与综合实践。',
          prerequisites: Array.isArray(item.prerequisites) && item.prerequisites.length ? item.prerequisites : ['前置核心概念'],
          examFocus: item.examFocus || '重点概念理解与综合实战推演'
        }));
        ElMessage.success(`AI 已针对【${chapTitle}】提炼出 ${aiSuggestedPoints.value.length} 个核心考点！`);
      } else {
        throw new Error('未解析到结构化知识点数据');
      }
    } catch (err: any) {
      console.warn('AI 提炼考点失败:', err);
      aiSuggestedPoints.value = [];
      ElMessage.error(err?.message || 'AI 考点提炼失败，请稍后重试');
    } finally {
      aiExtracting.value = false;
    }
  }

  function applyAiSuggestedPoint(point: any) {
    newKp.title = point.title;
    newKp.chapterId = point.chapterId;
    newKp.code = point.code;
    newKp.cognitiveDimension = point.cognitiveDimension;
    newKp.importance = point.importance;
    newKp.description = point.description;
    newKp.examFocus = point.examFocus;
    newKp.prerequisites = [...(point.prerequisites || [])];
    showAiSuggestModal.value = false;
    showCreateDrawer.value = true;
    ElMessage.success('已采纳推荐考点，可直接在表单中继续编辑细化！');
  }

  async function batchImportAiPoints(points: any[]) {
    if (!points || points.length === 0) return;
    creating.value = true;
    try {
      for (const p of points) {
        await createKnowledgePoint(courseId.value, {
          chapterId: p.chapterId,
          title: p.title,
          sortOrder: p.importance || 1
        });
      }
      ElMessage.success(`AI 已一键持久化录入 ${points.length} 个核心考点！`);
      showAiSuggestModal.value = false;
      await loadKnowledgePoints();
    } catch (err: any) {
      ElMessage.error(err?.message || '批量录入知识点失败');
    } finally {
      creating.value = false;
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
    const title = kp.title || kp.name || '核心考点';
    const prompt = `请结合本课程知识图谱，详细讲解核心考点【${title}】的定义、推导与常见考查题型。`;
    const excerpt = [
      `知识点：${title}`,
      kp.code ? `编码：${kp.code}` : '',
      `所属章节：${getChapterTitle(kp.chapterId)}`,
      kp.cognitiveDimension ? `认知维度：${getLevelLabel(kp.cognitiveDimension)}` : '',
      kp.description ? `说明：${kp.description}` : ''
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

  async function confirmDeleteKp(kp: any) {
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
      // 用户取消
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
    showAiSuggestModal,
    aiExtracting,
    aiSuggestedPoints,
    openAiSuggestModal,
    generateAiSuggestedPoints,
    applyAiSuggestedPoint,
    batchImportAiPoints,
    loadKnowledgePoints,
    getChapterTitle,
    getPointsForChapter,
    getLevelLabel,
    getLevelTagType,
    handleSaveNewKp,
    openGraphDrawer,
    handleAskAi,
    handleGenerateQuizForKp,
    handleDeleteKp,
    confirmDeleteKp
  };
}
