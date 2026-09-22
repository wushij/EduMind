import { ref, computed, watch, nextTick, onMounted, markRaw, type Ref } from 'vue';
import { useAuthStore } from '@/stores/auth/auth';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
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

  function handleCreateNewSession() {
    const courseId = resolveCourseId();
    if (!courseId) {
      ElMessage.warning('课程信息加载中，请稍后再试');
      return;
    }
    startNewChat(courseId);
    historyDrawerVisible.value = false;
    ElMessage.success('已开启新问答会话');
  }

  function getPersonaPromptPrefix(persona?: string) {
    return getCourseAiPersonaPromptPrefix(persona);
  }

  function handleSend(promptText: string) {
    const courseId = resolveCourseId();
    if (!courseId) {
      ElMessage.warning('课程信息加载中，请稍后再试');
      return;
    }
    let finalPrompt = promptText;
    if (activeSectionTitle.value) {
      finalPrompt = `[针对课时: ${activeSectionTitle.value}] ${finalPrompt}`;
    }
    // 注入人设风格指导
    if (course.value?.aiPersona && messages.value.length === 0) {
      finalPrompt = `${getPersonaPromptPrefix(course.value.aiPersona)}${finalPrompt}`;
    }
    const lessonChapterId = route.query.lessonId ? Number(route.query.lessonId) : undefined;
    sendMessage(finalPrompt, courseId, {
      chapterId: activeChapterId.value,
      lessonChapterId: Number.isFinite(lessonChapterId) ? lessonChapterId : undefined,
      // 不传 modelKey：由后端按「场景路由 → 平台默认对话模型(is_default)」自动选择
      useRag: Boolean(course.value?.knowledgeBaseId)
    });
  }

  function handleSendPrompt(promptText: string) {
    handleSend(promptText);
  }

  function handleRegenerate(idx: number) {
    const courseId = resolveCourseId();
    if (!courseId) return;
    regenerateStreamMessage(idx, courseId, {
      chapterId: activeChapterId.value
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

  const recommendedQuestions = computed(() => {
    const name = displayCourseTitle.value;
    return [
      `请结合大纲为我梳理【${name}】的核心知识图谱架构`,
      `当前阶段如何高效复习【${name}】？请给出科学备考指引`,
      `在【${name}】中，有哪些最易混淆的重点概念？请对比解析`,
      `请针对当前章节出一道典型综合解析题并附解题思路`,
      `【${name}】在实际工程研发与学科前沿中有哪些典型应用？`
    ];
  });

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

  watch(
    () => resolveCourseId(),
    (courseId) => {
      if (!courseId) return;
      void loadSessions(courseId, { restoreLastSession: true });
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
    modeTabs,
    currentModeTab,
    switchModeTab,
    historyDrawerVisible,
    chatInputRef,
    sessions,
    currentSessionId,
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
    handleSendRecommended
  };
}
