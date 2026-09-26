import { ref, computed, watch, nextTick, onMounted, onActivated, markRaw, type Ref } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ChatDotRound,
  Monitor,
  Reading,
  EditPen,
  Opportunity
} from '@element-plus/icons-vue';
import type { CourseVO, Course } from '@/types/course/course';
import type { Chapter } from '@/types/course/chapter';
import { getCourseList } from '@/api/course/course';
import { getChapters } from '@/api/course/chapter';
import { getCourseResources } from '@/api/course/resource';
import { useAIStream } from '@/composables/ai/useAIStream';
import { requestFollowUps } from '@/services/ai/stream-service';
import {
  getCourseAiPersonaPromptPrefix,
  normalizeCourseAiPersona
} from '@/constants/course/ai-persona';

export interface ChapterNode {
  id: number;
  title: string;
  expanded: boolean;
  sections: { id: number; title: string }[];
}

export interface ResourceItem {
  title: string;
  type: string;
  ext: string;
  cat: string;
  downloadUrl?: string;
}

export const LAST_COURSE_ID_KEY = 'edumind_last_course_id';

export function resolveActiveCourse(list: Course[], storedId?: number | null): Course | null {
  if (list.length === 0) return null;
  const id = storedId ?? NaN;
  const matched = !Number.isNaN(id) ? list.find((c) => Number(c.id) === id) : undefined;
  return matched || list[0];
}

export function useCourseAIPage() {
  const authStore = useAuthStore();
  const loading = ref(true);
  const courses = ref<Course[]>([]);
  const activeCourse = ref<Course | null>(null);

  const canCreateCourse = computed(() => {
    return authStore.hasAnyRole(['ADMIN', 'TEACHER']) || authStore.hasPermission('course:create');
  });

  function handleSwitchActiveCourse(courseId: number) {
    const found = courses.value.find((c) => Number(c.id) === Number(courseId));
    if (found) {
      activeCourse.value = found;
      localStorage.setItem(LAST_COURSE_ID_KEY, String(found.id));
    }
  }

  async function loadCourses() {
    loading.value = true;
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = res?.data?.list || [];
      courses.value = list;

      if (list.length > 0) {
        const storedIdStr = localStorage.getItem(LAST_COURSE_ID_KEY);
        const storedId = storedIdStr ? Number(storedIdStr) : NaN;
        activeCourse.value = resolveActiveCourse(list, storedId);
        if (activeCourse.value) {
          localStorage.setItem(LAST_COURSE_ID_KEY, String(activeCourse.value.id));
        }
      } else {
        activeCourse.value = null;
      }
    } catch {
      courses.value = [];
      activeCourse.value = null;
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    loadCourses();
  });

  return {
    loading,
    courses,
    activeCourse,
    canCreateCourse,
    loadCourses,
    handleSwitchActiveCourse
  };
}

export function mapChapterTree(nodes: Chapter[], expandedFirst = true): ChapterNode[] {
  return nodes.map((node, index) => {
    const childSections = (node.children || []).map((child) => ({
      id: child.id,
      title: child.title
    }));
    const sections =
      childSections.length > 0 ? childSections : [{ id: node.id, title: node.title }];
    return {
      id: node.id,
      title: node.title,
      expanded: expandedFirst && index === 0,
      sections
    };
  });
}

/**
 * 剥离小节 / 章节前导编号与代码（如「1.1 极限的概念」「1.1. 极限的概念」「第1节 极限的概念」→「极限的概念」）。
 * 推荐问题、提问材料中应展示纯知识点，避免模型在推荐问题中输出硬编码编号。
 */
export function stripSectionNumber(title?: string): string {
  if (!title) return '';
  return (
    title
      .replace(
        /^\s*(?:第\s*[\d一二三四五六七八九十]+(?:\.[\d]+)*\s*[章节讲课节]|[\d]+(?:\.[\d]+)+\.?)\s*[-_、.：:\s]*/,
        ''
      )
      .trim() || title.trim()
  );
}

/**
 * 为「推荐问题」构造模型材料：课程名 + 章节结构 + 当前小节。
 *
 * <p>清洗掉机械的小节编号，使模型能够专注于核心知识点提问，不把「1.1」硬编码进问题。</p>
 */
export function buildRecommendedQuestionMaterial(
  courseTitle: string,
  chapters: ChapterNode[],
  sectionTitle?: string
): string {
  const lines = [`课程：${courseTitle?.trim() || '未命名课程'}`];
  const structure = chapters
    .slice(0, 12)
    .map((chapter) => {
      const cleanChapter = stripSectionNumber(chapter.title);
      const sections = chapter.sections
        .slice(0, 12)
        .map((sec) => `    - ${stripSectionNumber(sec.title)}`)
        .join('\n');
      return `  - ${cleanChapter}${sections ? `\n${sections}` : ''}`;
    })
    .join('\n');
  if (structure) {
    lines.push('章节知识点体系：', structure);
  }
  if (sectionTitle?.trim()) {
    lines.push(`当前正在学习的知识点：${stripSectionNumber(sectionTitle)}`);
  }
  lines.push('【提问规范】：生成的推荐问题必须面向具体知识点，严禁出现章节小节编号（如“1.1”、“1.2”、“第1节”等数字编号）。');
  return lines.join('\n');
}

export interface UseCourseAIWorkspaceOptions {
  course: Ref<CourseVO | null | undefined>;
  onSwitchCourse?: (courseId: number) => void;
}

export function useCourseAIWorkspace(options: UseCourseAIWorkspaceOptions) {
  const { course, onSwitchCourse } = options;
  const router = useRouter();
  const route = useRoute();

  function resolveCourseId(): number | undefined {
    const propId = course.value?.id ? Number(course.value.id) : NaN;
    if (!Number.isNaN(propId) && propId > 0) return propId;
    const routeId = Number(route.params.id);
    if (!Number.isNaN(routeId) && routeId > 0) return routeId;
    return undefined;
  }

  const currentCourseIdNum = computed(() => resolveCourseId() ?? 0);
  const displayCourseTitle = computed(() => {
    return course.value?.title || course.value?.name || '学科专属';
  });

  const tenantCourseOptions = ref<Course[]>([]);
  async function loadTenantCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      tenantCourseOptions.value = res?.data?.list || [];
    } catch {
      tenantCourseOptions.value = [];
    }
  }

  function handleSwitchCourse(targetId: number | string) {
    const numId = Number(targetId);
    localStorage.setItem('edumind_last_course_id', String(numId));
    onSwitchCourse?.(numId);
    if (route.params.id) {
      router.push(`/course/${numId}/ai`);
    }
  }

  const quickActionPills = [
    {
      icon: markRaw(ChatDotRound),
      title: '智能答疑',
      prompt: '请针对当前课程知识体系进行智能答疑与难点梳理',
      color: '#1677FF'
    },
    {
      icon: markRaw(Monitor),
      title: '代码解析',
      prompt: '请解析一段典型的核心代码示例并分析关键逻辑',
      color: '#0284C7'
    },
    {
      icon: markRaw(Reading),
      title: '学习指导',
      prompt: '请为我提供当前学习阶段的高效复习与规划指导',
      color: '#10B981'
    },
    {
      icon: markRaw(EditPen),
      title: '习题讲解',
      prompt: '请结合典型例题为我详细讲解解题思路与避坑指南',
      color: '#F59E0B'
    },
    {
      icon: markRaw(Opportunity),
      title: '知识拓展',
      prompt: '请提供本门课程的进阶知识拓展与工程实践场景',
      color: '#EAB308'
    }
  ];

  const chapterKeyword = ref('');
  const activeSectionId = ref<number | null>(null);
  const activeSectionTitle = ref('');
  const activeChapterId = ref<number | undefined>(undefined);
  const chaptersLoading = ref(false);
  const chaptersData = ref<ChapterNode[]>([]);

  async function loadChapters() {
    const courseId = resolveCourseId() ?? 0;
    if (!courseId) return;
    chaptersLoading.value = true;
    try {
      const res = await getChapters(courseId);
      const list = Array.isArray(res?.data) ? res.data : [];
      chaptersData.value = mapChapterTree(list as Chapter[]);
      if (chaptersData.value.length > 0) {
        const first = chaptersData.value[0];
        const firstSec = first.sections[0];
        if (firstSec) {
          activeSectionId.value = firstSec.id;
          activeChapterId.value = firstSec.id;
          activeSectionTitle.value = firstSec.title;
        }
      }
    } catch {
      chaptersData.value = [];
    } finally {
      chaptersLoading.value = false;
    }
  }

  const filteredChapters = computed(() => {
    const kw = chapterKeyword.value.trim().toLowerCase();
    if (!kw) return chaptersData.value;
    return chaptersData.value
      .map((chap) => {
        const matchChap = chap.title.toLowerCase().includes(kw);
        const filteredSecs = chap.sections.filter((s) => s.title.toLowerCase().includes(kw));
        if (matchChap || filteredSecs.length > 0) {
          return {
            ...chap,
            expanded: true,
            sections: filteredSecs.length > 0 ? filteredSecs : chap.sections
          };
        }
        return null;
      })
      .filter(Boolean) as ChapterNode[];
  });

  function selectSection(sec: { id: number; title: string }) {
    activeSectionId.value = sec.id;
    activeChapterId.value = sec.id;
    activeSectionTitle.value = sec.title;
  }

  async function clearSectionAnchor() {
    try {
      await ElMessageBox.confirm(
        `确定解除对章节「${activeSectionTitle.value || '当前章节'}」的知识锚定吗？解除后 AI 答疑将恢复为全门课程知识库检索模式。`,
        '解除章节锚定确认',
        {
          confirmButtonText: '确定解除',
          cancelButtonText: '取消',
          type: 'warning',
          lockScroll: false
        }
      );
      activeSectionId.value = null;
      activeChapterId.value = undefined;
      activeSectionTitle.value = '';
      ElMessage.success('已解除章节知识锚定，恢复全门课程通用检索模式');
    } catch {
      // 用户取消操作，无需任何操作
    }
  }

  function handlePillClick(pill: (typeof quickActionPills)[0]) {
    handleSend(pill.prompt);
  }

  const modeTabs = [
    { key: 'ai', label: 'AI 助手' },
    { key: 'task', label: '学习任务' },
    { key: 'graph', label: '知识图谱' },
    { key: 'resource', label: '推荐资源' }
  ];
  const currentModeTab = ref('ai');

  function switchModeTab(key: string) {
    currentModeTab.value = key;
    const cid = resolveCourseId();
    if (!cid) return;
    if (key === 'graph') {
      router.push(`/course/${cid}/knowledge-points`);
    } else if (key === 'resource') {
      router.push(`/course/${cid}/resources`);
    } else if (key === 'task') {
      router.push(`/course/${cid}/overview`);
    }
  }

  // 模型选择已移除：统一由后端按「场景路由 → 平台默认模型(is_default)」决定，
  // 与后台「AI 模型配置」里标了默认的对话模型保持一致，前端不再提供自选入口。

  const historyDrawerVisible = ref(false);
  const chatInputRef = ref<{ focus?: () => void } | null>(null);

  const {
    sessions,
    currentSessionId,
    messages,
    streaming,
    messagesScrollRef,
    streamAnchorRef,
    streamingRenderedHtml,
    streamingAnswerBody,
    streamingThinkingDisplay,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    followUpPrompts,
    showThinkingPanel,
    loadSessions,
    refreshSessionsMeta,
    switchSession,
    sendMessage,
    stopStream,
    startNewChat,
    deleteSession,
    clearAllSessions,
    handleRegenerate: regenerateStreamMessage,
    confirmDeleteMessage,
    scrollToBottomSmooth,
    scrollToBottomInstant,
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll
  } = useAIStream();

  const welcomeMessage = computed(() => {
    const custom = course.value?.welcomeMessage;
    const persona = course.value?.aiPersona;
    const personaId = normalizeCourseAiPersona(persona);
    const personaPrefix =
      personaId === 'academic'
        ? '【严谨学术推导型】'
        : personaId === 'engineer'
          ? '【工程实战导师型】'
          : personaId === 'gentle'
            ? '【温和鼓励引路人】'
            : '【苏格拉底启发型】';
    return {
      id: 'welcome',
      role: 'assistant' as const,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      content: custom || `${personaPrefix}同学你好！我是本课程的专属 AI 助教。已为你挂载本课程专有知识库与教学大纲，关于微课时核心考点、概念辨析或代码实战，请随时向我提问！`
    };
  });

  const allDisplayMessages = computed(() => {
    if (messages.value.length === 0) {
      return [welcomeMessage.value];
    }
    return messages.value;
  });

  function handleDeleteSession(id: string) {
    void deleteSession(id, resolveCourseId());
  }

  async function handleClearAllSessions() {
    if (streaming.value) {
      stopStream();
    }
    await clearAllSessions(resolveCourseId());
  }

  function handleSelectSession(id: string) {
    switchSession(id, resolveCourseId());
    historyDrawerVisible.value = false;
  }

  async function handleCreateNewSession() {
    const courseId = resolveCourseId();
    if (!courseId) {
      ElMessage.warning('课程信息加载中，请稍后再试');
      return;
    }
    if (streaming.value) {
      ElMessage.warning('当前正在生成中，请先停止生成');
      return;
    }

    const previousSessionId = currentSessionId.value;
    try {
      const result = await startNewChat(courseId);
      historyDrawerVisible.value = false;
      if (!result) {
        ElMessage.info('当前已是空白会话，直接提问即可');
      } else if (result.reused) {
        ElMessage.info(
          result.sessionId === previousSessionId
            ? '当前已是空白会话，直接提问即可'
            : '已切换到空白会话，直接提问即可'
        );
      } else {
        ElMessage.success('已开启新问答会话');
      }
    } catch {
      // createNewSession 内部已给出失败提示，此处仅避免未处理的 Promise 拒绝
    }
  }

  function getPersonaPromptPrefix(persona?: string) {
    return getCourseAiPersonaPromptPrefix(persona);
  }

  function handleSend(input: string | { text: string; webSearch?: boolean; attachmentIds?: string[] }) {
    const courseId = resolveCourseId();
    if (!courseId) {
      ElMessage.warning('课程信息加载中，请稍后再试');
      return;
    }
    const text = typeof input === 'string' ? input : input.text;
    const cleanPrompt = (text || '').trim();
    if (!cleanPrompt) return;

    const webSearch = typeof input === 'object' ? input.webSearch : false;
    const attachmentIds = typeof input === 'object' ? input.attachmentIds : undefined;

    const queryLessonId = route.query.lessonId ? Number(route.query.lessonId) : undefined;
    const resolvedLessonId = Number.isFinite(queryLessonId)
      ? queryLessonId
      : (activeSectionId.value ?? activeChapterId.value ?? undefined);

    sendMessage(cleanPrompt, courseId, {
      chapterId: activeChapterId.value,
      lessonChapterId: resolvedLessonId,
      sectionTitle: activeSectionTitle.value || undefined,
      // 不传 modelKey：由后端按「场景路由 → 平台默认对话模型(is_default)」自动选择
      // 当 course 包含明确 knowledgeBaseId 时传 true；否则传 undefined 由后端按 courseId 自动关联知识库
      useRag: course.value?.knowledgeBaseId ? true : undefined,
      webSearch,
      attachmentIds
    });
  }

  function handleSendPrompt(promptText: string) {
    handleSend(promptText);
  }

  function handleRegenerate(idx: number) {
    const courseId = resolveCourseId();
    if (!courseId) return;
    regenerateStreamMessage(idx, courseId, {
      chapterId: activeChapterId.value,
      sectionTitle: activeSectionTitle.value || undefined
    });
  }

  const resourceCategories = ['课件', '视频', '文档', '习题', '项目'];
  const activeResourceCat = ref('课件');
  const realResources = ref<ResourceItem[]>([]);
  const resourcesLoading = ref(false);

  async function loadCourseResources() {
    const cid = resolveCourseId();
    if (!cid) {
      realResources.value = [];
      return;
    }
    resourcesLoading.value = true;
    try {
      const res = await getCourseResources(cid);
      const list = Array.isArray(res?.data) ? res.data : [];
      realResources.value = list.map((item) => {
        // 无扩展名时 lastIndexOf('.') 为 -1，不能用 split('.').pop()——那会把整个标题当成扩展名，
        // 于是 30px 的徽标里渲染出完整文件名并溢出，与右侧标题文字重叠（微课节 MD 资源即为此例）
        const rawTitle = (item.title || '').trim();
        const dotIndex = rawTitle.lastIndexOf('.');
        const extFromTitle = dotIndex > 0 ? rawTitle.slice(dotIndex + 1).toUpperCase() : '';
        const ext = (extFromTitle || item.resourceType || 'DOC').toUpperCase();
        let cat = '课件';
        if (['MP4', 'AVI', 'MKV', 'WEBM', 'VIDEO'].includes(ext)) cat = '视频';
        else if (['ZIP', 'RAR', '7Z', 'CODE', 'TAR'].includes(ext)) cat = '项目';
        else if (['EXAM', 'QUIZ'].includes(item.resourceType || '') || ext === 'DOCX') cat = '习题';
        // 注意：MD 是微课节课件正文，保持归入「课件」，不要挪到「文档」
        else if (['PDF', 'DOC', 'TXT'].includes(ext)) cat = '文档';

        return {
          title: item.title,
          type: ext.toLowerCase(),
          ext,
          cat,
          downloadUrl: item.downloadUrl
        };
      });
    } catch {
      realResources.value = [];
    } finally {
      resourcesLoading.value = false;
    }
  }

  const filteredResources = computed(() => {
    return realResources.value.filter((r) => r.cat === activeResourceCat.value);
  });

  function downloadResource(res: ResourceItem) {
    if (res.downloadUrl) {
      window.open(res.downloadUrl, '_blank');
    } else {
      ElMessage.info(`资料「${res.title}」已进入下载队列`);
    }
  }

  function navigateToResources() {
    const cid = resolveCourseId();
    if (cid) {
      router.push(`/course/${cid}/resources`);
    }
  }

  const recommendedQuestions = ref<string[]>([]);
  const recommendedQuestionsLoading = ref(false);
  let recommendedTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * 按「课程 + 当前小节」动态生成推荐问题（模型优先，失败回落到规则生成）。
   * 命中本地缓存时同步出结果，不重复调用模型；章节结构未加载完时做一次防抖，
   * 避免先按标题生成一版、章节加载后又重来一版。
   */
  function refreshRecommendedQuestions() {
    const courseTitle = displayCourseTitle.value;
    const rawSecTitle = activeSectionTitle.value?.trim();
    const cleanSecTitle = rawSecTitle ? stripSectionNumber(rawSecTitle) : '';
    const material = buildRecommendedQuestionMaterial(courseTitle, chaptersData.value, rawSecTitle);
    const asking = cleanSecTitle
      ? `我正准备学习「${cleanSecTitle}」，请给出我最该先弄清楚的几个问题`
      : `我想系统学习【${courseTitle}】，请给出我最该先弄清楚的几个问题`;

    recommendedQuestionsLoading.value = true;
    const prompts = requestFollowUps(asking, material, {
      courseTitle,
      sectionTitle: cleanSecTitle || undefined,
      courseId: resolveCourseId(),
      minMaterialChars: 0,
      count: 5,
      onUpdate: (list) => {
        recommendedQuestions.value = list.slice(0, 5);
        recommendedQuestionsLoading.value = false;
      }
    });
    if (prompts.length > 0) {
      recommendedQuestions.value = prompts.slice(0, 5);
      recommendedQuestionsLoading.value = false;
    }
  }

  function scheduleRecommendedQuestions(delay = 600) {
    if (recommendedTimer) clearTimeout(recommendedTimer);
    recommendedTimer = setTimeout(() => {
      recommendedTimer = undefined;
      refreshRecommendedQuestions();
    }, delay);
  }

  // 课程 / 小节 / 章节结构变化时重新生成推荐问题（防抖 + 本地缓存，切回同一小节不再调模型）
  watch(
    [currentCourseIdNum, activeSectionTitle, () => chaptersData.value],
    () => {
      scheduleRecommendedQuestions();
    },
    { immediate: true }
  );

  function handleSendRecommended(question: string) {
    handleSend(question);
  }

  onMounted(() => {
    loadTenantCourses();
    nextTick(() => {
      const appContent = document.querySelector('.app-content');
      if (appContent) {
        appContent.scrollLeft = 0;
      }
    });
  });

  onActivated(() => {
    nextTick(() => {
      scrollToBottomInstant();
      const appContent = document.querySelector('.app-content');
      if (appContent) {
        appContent.scrollLeft = 0;
      }
    });
  });

  watch(
    () => resolveCourseId(),
    (courseId) => {
      if (!courseId) return;
      void loadSessions(courseId, { restoreLastSession: false });
      void loadChapters();
      void loadCourseResources();
    },
    { immediate: true }
  );

  return {
    currentCourseIdNum,
    displayCourseTitle,
    tenantCourseOptions,
    handleSwitchCourse,
    quickActionPills,
    handlePillClick,
    chapterKeyword,
    activeSectionId,
    activeSectionTitle,
    activeChapterId,
    chaptersData,
    filteredChapters,
    selectSection,
    clearSectionAnchor,
    modeTabs,
    currentModeTab,
    switchModeTab,
    historyDrawerVisible,
    chatInputRef,
    sessions,
    currentSessionId,
    loadSessions,
    refreshSessionsMeta,
    streaming,
    messagesScrollRef,
    streamAnchorRef,
    streamingRenderedHtml,
    streamingAnswerBody,
    streamingThinkingDisplay,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    followUpPrompts,
    showThinkingPanel,
    allDisplayMessages,
    handleDeleteSession,
    handleClearAllSessions,
    handleSelectSession,
    handleCreateNewSession,
    handleSend,
    handleSendPrompt,
    handleRegenerate,
    confirmDeleteMessage,
    stopStream,
    scrollToBottomSmooth,
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll,
    resourceCategories,
    activeResourceCat,
    resourcesLoading,
    filteredResources,
    downloadResource,
    navigateToResources,
    recommendedQuestions,
    recommendedQuestionsLoading,
    refreshRecommendedQuestions,
    handleSendRecommended
  };
}
