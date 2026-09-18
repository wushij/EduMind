import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SSEClient } from '@/core/sse/client';
import { streamGlobalAssistantChat } from '@/services/ai/copilot-sse-stream';
import { useAIStreamScrollFollow } from '@/composables/ai/useAIStreamScrollFollow';
import { askGlobalAssistant } from '@/api/ai/assistant';
import {
  getMessages,
  deleteConversation,
  deleteMessage
} from '@/api/ai/chat';
import {
  collectPairedMessageIds,
  isPersistedMessageId,
  removeMessagesByIds
} from '@/utils/ai/chat-message-pair';
import { storage } from '@/core/storage/local';
import {
  splitCopilotStream,
  cleanReasoningText,
  buildStoppedGenerationContent
} from '@/utils/ai/copilot-stream-split';
import { useStreamingMarkdown } from '@/composables/ai/useStreamingMarkdown';
import { bindMarkdownCodeCopy } from '@/utils/ai/chat-markdown';
import type {
  CitationItem,
  GlobalAssistantChatRequest,
  GlobalAssistantIntentEvent,
  GlobalAssistantMessage,
  GlobalAssistantSession
} from '@/types/ai/assistant';
import { useTeachingCopilotStore, OPEN_GLOBAL_ASSISTANT_EVENT } from '@/stores/ai/teaching-copilot-context';
import { buildTeachingContextRequestPayload } from '@/types/ai/teaching-copilot-context';
import { useAuthStore } from '@/stores/auth/auth';
import {
  getDefaultReasoningFolded,
  isThinkingPanelHidden
} from '@/utils/ai/thinking-display';


const INTENT_DESC_MAP: Record<string, string> = {
  agent: '智能体任务编排',
  rag: '知识库检索',
  navigate: '功能页面直达',
  chat: '课程助教答疑'
};

function resolveTargetPath(intent?: string, agentCode?: string): string | undefined {
  if (!agentCode) return undefined;
  if (agentCode.startsWith('/')) return agentCode;
  if (agentCode === 'exam' || intent === 'EXAM_COMPOSE') return '/ai/exam/generate';
  if (agentCode === 'learning') return '/analytics/knowledge-mastery';
  if (agentCode === 'kb_retrieval') return '/knowledge/retrieval';
  return undefined;
}

function resolveIntentDesc(intent?: string, agentCode?: string): string {
  if (intent === 'agent' && agentCode === 'exam') return '识别意图：AI 智能组卷与出题';
  if (intent === 'rag') return '识别意图：知识库考点检索';
  if (intent === 'navigate') return '识别意图：页面功能直达';
  if (intent && INTENT_DESC_MAP[intent]) return `识别意图：${INTENT_DESC_MAP[intent]}`;
  return agentCode || intent || '智能助手';
}

function generateSmartFollowUps(query: string): string[] {
  const cleanQuery = query.replace(/^\[[^\]]+\]\s*/g, '').trim().toLowerCase();
  const q = cleanQuery || query.toLowerCase();
  if (/卷|题|考|试|做题|作业|数列|数学|考点/.test(q)) {
    return [
      '根据此题型衍生 3 道同等难度的变式训练题',
      '导出试题解析与评分细则标准',
      '分析这道题目考察的底层核心知识点与易错陷阱'
    ];
  }
  if (/切片|检索|文档|资料|知识库/.test(q)) {
    return [
      '将这些切片资料汇总为教学大纲概要',
      '查看切片关联的知识图谱上下游节点',
      '提炼切片中的关键定义与公式要点'
    ];
  }
  if (/学情|分析|学生|成绩|分布/.test(q)) {
    return [
      '生成班级薄弱知识点的针对性强化策略',
      '查看掌握度低于 60% 的预警学员名单',
      '导出近 30 天学生学习趋势对比'
    ];
  }
  if (/图谱|关系|拓扑/.test(q)) {
    return [
      '展开当前知识点的前驱依赖与后继拓展',
      '为零基础学员规划最佳学习路径',
      '推荐与本图谱相关的配套教案课件'
    ];
  }
  if (/算法|数据结构|复杂度|渐进表示/.test(q)) {
    return [
      '分析该算法在最好、最坏与平均情况下的时空复杂度',
      '用简明步骤图解该算法的执行推演过程',
      '对比该算法与其他替代方案的核心优缺点'
    ];
  }
  return [
    '用更通俗生动的教学案例进一步解释',
    '为该考点出 2 道课堂即兴互动提问',
    '结合实际场景列举典型的应用实例'
  ];
}

const SESSION_STORAGE_PREFIX = 'edumind_global_assistant_session:';

function resolveSessionStorageKey(
  courseId?: number,
  lessonChapterId?: number,
  userId?: number
): string {
  if (courseId && lessonChapterId && userId) {
    return `edumind_lesson_copilot_session_${userId}_${courseId}_${lessonChapterId}`;
  }
  return `${SESSION_STORAGE_PREFIX}${courseId ?? 'global'}`;
}

function buildSessionTitleFromPrompt(text: string): string {
  const cleaned = text.replace(/\s+/g, ' ').trim();
  return cleaned.slice(0, 20) || '新问答会话';
}

function formatSessionTime(raw: unknown): string {
  if (!raw) return '刚刚';
  if (typeof raw === 'number') {
    const diff = Math.floor((Date.now() - raw) / 1000);
    if (diff < 60) return '刚刚';
    if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`;
    if (diff < 172800) return '昨天';
  }
  const d = new Date(String(raw));
  if (Number.isNaN(d.getTime())) return String(raw);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  return `${d.toLocaleDateString([], { month: '2-digit', day: '2-digit' })} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

const SESSIONS_STORAGE_KEY = 'edumind_global_assistant_sessions_v1';

function readSessionsFromStorage(): GlobalAssistantSession[] {
  try {
    const raw = localStorage.getItem(SESSIONS_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.sort((a, b) => {
      const ta = typeof a.updatedAt === 'number' ? a.updatedAt : new Date(String(a.updatedAt || 0)).getTime();
      const tb = typeof b.updatedAt === 'number' ? b.updatedAt : new Date(String(b.updatedAt || 0)).getTime();
      return tb - ta;
    });
  } catch (e) {
    console.warn('Failed to parse assistant sessions from localStorage', e);
    return [];
  }
}

function writeSessionsToStorage(list: GlobalAssistantSession[]) {
  try {
    localStorage.setItem(SESSIONS_STORAGE_KEY, JSON.stringify(list));
  } catch (e) {
    console.warn('Failed to write assistant sessions to localStorage', e);
  }
}

function isBackendConversationId(id: string): boolean {
  return Boolean(id) && !/^conv[_-]/i.test(id);
}

function mapApiMessage(raw: Record<string, unknown>): GlobalAssistantMessage {
  const citations = Array.isArray(raw.citations)
    ? (raw.citations as CitationItem[])
    : undefined;
  return {
    id: String(raw.id || `msg_${Date.now()}`),
    role: (raw.role || 'assistant') as GlobalAssistantMessage['role'],
    content: String(raw.content || ''),
    reasoningContent: String(raw.reasoningContent || raw.reasoning_content || ''),
    citations,
    createdAt: raw.createTime ? String(raw.createTime) : undefined
  };
}

const DEFAULT_PRESET_CHIPS = [
  { label: '智能组卷', prompt: '帮我出一份包含导数与微分的期中试卷，含选择、填空与大题' },
  { label: '检索切片', prompt: '请深度检索微积分第一章的核心知识点切片与讲义资料' },
  { label: '学情看板', prompt: '我想查看近期班级知识点掌握度与薄弱点学情报表' },
  { label: '知识图谱', prompt: '生成并展示当前微积分课程的拓扑知识图谱' }
];

const LESSON_STUDIO_PRESET_CHIPS = [
  {
    label: '润色导读',
    prompt: '请润色本课节侧栏导读文案，语气专业简洁，输出可直接填入导读的正文。'
  },
  {
    label: '学习目标',
    prompt: '请为本课节撰写 3–5 条可测量的学习目标，输出可直接填入学习目标卡的内容。'
  },
  {
    label: '续写正文',
    prompt: '请根据当前课节正文摘录自然续写下一小节 Markdown 正文，保持教学节奏。'
  }
];

export function useGlobalAssistant() {
  const route = useRoute();
  const authStore = useAuthStore();
  const teachingCopilotStore = useTeachingCopilotStore();
  const router = useRouter();
  const sseClient = new SSEClient();

  const drawerVisible = ref(false);
  const inputContent = ref('');
  const isStreaming = ref(false);
  const messages = ref<GlobalAssistantMessage[]>([]);
  const conversationId = ref<string>();
  const messagesScrollRef = ref<HTMLDivElement | null>(null);
  const streamAnchorRef = ref<HTMLDivElement | null>(null);

  // 历史对话：localStorage 为主（刷新不丢），UI 对标 Code Compass
  const isHistoryPanelOpen = ref(false);
  const isSessionLoading = ref(false);
  const sessions = ref<GlobalAssistantSession[]>(readSessionsFromStorage());

  // 流式过程中的临时状态
  const streamingReasoning = ref('');
  const streamingContent = ref('');
  const streamingCitations = ref<CitationItem[]>([]);
  const streamingIntent = ref<{ intent?: string; intentDesc?: string; targetCode?: string }>({});

  const getInitialReasoningFolded = getDefaultReasoningFolded;

  // 深度思考折叠状态（遵循个人偏好 → 深度思考默认呈现策略）
  const isReasoningFolded = ref(getInitialReasoningFolded());
  const isReasoningActive = ref(false);
  const streamPhaseMessage = ref('');
  const answerStreamStarted = ref(false);
  const followUpPrompts = ref<string[]>([]);
  /** 用户手动点击停止后置位，避免 fallback / 重新生成错误覆盖已结算内容 */
  const userStoppedGeneration = ref(false);

  const {
    renderedHtml: streamingRenderedHtml,
    appendChunk: appendStreamingMarkdown,
    finish: finishStreamingMarkdown,
    reset: resetStreamingMarkdown
  } = useStreamingMarkdown();

  const streamingSplit = computed(() => splitCopilotStream(streamingContent.value));
  const streamingThinkingBody = computed(() => streamingSplit.value.thinking);
  const streamingAnswerBody = computed(() => streamingSplit.value.answer);
  const streamingThinkingDisplay = computed(() => {
    const raw = streamingReasoning.value || streamingThinkingBody.value;
    return cleanReasoningText(raw);
  });

  const {
    showScrollToBottom,
    pauseAutoScrollFollow,
    handleViewportScroll,
    scrollToBottomInstant,
    scrollToBottomSmooth,
    scheduleFollowStreamOutput
  } = useAIStreamScrollFollow({
    streaming: isStreaming,
    streamingRenderedHtml,
    streamingReasoning,
    messagesScrollRef,
    streamAnchorRef
  });

  function refreshMarkdownUi() {
    nextTick(() => {
      nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
    });
  }

  watch(
    () => messages.value.map((m) => `${m.id ?? ''}:${m.content?.length ?? 0}`).join('|'),
    () => refreshMarkdownUi()
  );

  watch(isStreaming, (streaming, wasStreaming) => {
    if (wasStreaming && !streaming) {
      refreshMarkdownUi();
    }
  });

  // 是否手动指定为全域研读模式（彻底杜绝无故写死 #102）
  const manualGlobalScope = ref(false);

  const lessonSessionScope = computed(() => {
    const ctx = teachingCopilotStore.activeContext;
    if (ctx?.contextModule === 'lesson_studio' && ctx.lessonChapterId && ctx.courseId) {
      return {
        courseId: ctx.courseId,
        lessonChapterId: ctx.lessonChapterId
      };
    }
    return null;
  });

  function sessionStorageKey(courseId?: number): string {
    const scope = lessonSessionScope.value;
    const userId = authStore.currentUser?.id;
    if (scope?.courseId && scope.lessonChapterId && userId) {
      return resolveSessionStorageKey(scope.courseId, scope.lessonChapterId, userId);
    }
    return resolveSessionStorageKey(courseId, undefined, undefined);
  }

  function getStoredSessionId(courseId?: number): string {
    const stored = storage.get(sessionStorageKey(courseId));
    return typeof stored === 'string' ? stored : '';
  }

  function storeSessionId(courseId: number | undefined, sessionId: string) {
    if (!sessionId) return;
    storage.set(sessionStorageKey(courseId), sessionId);
  }

  function clearStoredSessionId(courseId: number | undefined, sessionId: string) {
    if (!sessionId) return;
    if (getStoredSessionId(courseId) === sessionId) {
      storage.remove(sessionStorageKey(courseId));
    }
  }

  const activeCourseId = computed<number | undefined>(() => {
    if (manualGlobalScope.value) return undefined;
    const ctx = teachingCopilotStore.activeContext;
    if (
      ctx?.courseId &&
      (ctx.contextModule === 'lesson_studio' || ctx.contextModule === 'lesson_learn')
    ) {
      return ctx.courseId;
    }
    const queryCourseId = route.query.courseId;
    if (queryCourseId) {
      const parsed = Number(queryCourseId);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    const paramCourseId = route.params.courseId || route.params.id;
    // 只有在课程相关路由下，才解析课程参数
    if (paramCourseId && (route.path.includes('/course') || route.name?.toString().includes('course'))) {
      const parsed = Number(paramCourseId);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    return undefined;
  });

  const isLessonStudioContext = computed(
    () =>
      !manualGlobalScope.value &&
      teachingCopilotStore.activeContext?.contextModule === 'lesson_studio'
  );

  const activeCourseLabel = computed(() => {
    if (manualGlobalScope.value) {
      return '全域研读模式 · 通用教学空间';
    }
    const ctx = teachingCopilotStore.activeContext;
    if (ctx?.contextModule === 'lesson_studio') {
      const statusLabel = ctx.contentStatus === 'PUBLISHED' ? '已发布' : '草稿';
      return `课节备课 · ${ctx.title || '未命名课节'}（${statusLabel}）`;
    }
    if (ctx?.contextModule === 'lesson_learn') {
      return `课节学习 · ${ctx.title || '未命名课节'}`;
    }
    if (activeCourseId.value) {
      return `课程空间: #${activeCourseId.value} 教学研读中枢`;
    }
    // 动态感知当前路由页面标题（如“智能题库中心”、“学情分析”等）
    const pageTitle = (route.meta?.title as string) || '';
    if (pageTitle) {
      return `教学研读空间: 智教云 · ${pageTitle}`;
    }
    return '全域研读模式 · 通用教学空间';
  });

  function toggleScopeMode() {
    manualGlobalScope.value = !manualGlobalScope.value;
    if (manualGlobalScope.value) {
      ElMessage.info('已切换为全域通用教学研读模式');
    } else {
      ElMessage.info('已同步当前页面研读空间');
    }
  }

  const presetChips = computed(() =>
    isLessonStudioContext.value ? LESSON_STUDIO_PRESET_CHIPS : DEFAULT_PRESET_CHIPS
  );

  function buildAssistantRequestBase(): Omit<GlobalAssistantChatRequest, 'message'> {
    const ctxPayload = manualGlobalScope.value
      ? {}
      : buildTeachingContextRequestPayload(teachingCopilotStore.activeContext);
    return {
      courseId: activeCourseId.value,
      conversationId: conversationId.value,
      ...ctxPayload
    };
  }

  function toggleDrawer() {
    drawerVisible.value = !drawerVisible.value;
    if (drawerVisible.value) {
      sessions.value = readSessionsFromStorage();
      restoreFollowUpsForLastTurn();
      nextTick(() => {
        scrollToBottomSmooth();
        bindMarkdownCodeCopy(messagesScrollRef.value);
      });
    }
  }

  function saveCurrentSessionToHistory() {
    if (messages.value.length === 0) return;

    if (!conversationId.value) {
      conversationId.value = `conv_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
    }
    const currentId = conversationId.value;
    const firstUserMsg = messages.value.find((m) => m.role === 'user');
    const rawTitle = firstUserMsg?.content?.trim() || '全新教学研读会话';
    const title = rawTitle.slice(0, 36).replace(/\n/g, ' ');

    const sessionItem: GlobalAssistantSession = {
      id: currentId,
      title: title || '智教云研读会话',
      updatedAt: Date.now(),
      messageCount: messages.value.length,
      messages: JSON.parse(JSON.stringify(messages.value))
    };

    const existingIdx = sessions.value.findIndex((s) => s.id === currentId);
    if (existingIdx >= 0) {
      sessions.value.splice(existingIdx, 1, sessionItem);
    } else {
      sessions.value.unshift(sessionItem);
    }

    if (sessions.value.length > 30) {
      sessions.value = sessions.value.slice(0, 30);
    }
    writeSessionsToStorage(sessions.value);
    storeSessionId(activeCourseId.value, currentId);
  }

  function restoreFollowUpsForLastTurn() {
    if (messages.value.length === 0) {
      followUpPrompts.value = [];
      return;
    }
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg && lastMsg.role === 'assistant') {
      if (lastMsg.followUpPrompts && lastMsg.followUpPrompts.length > 0) {
        followUpPrompts.value = [...lastMsg.followUpPrompts];
        return;
      }
      const lastUserMsg = [...messages.value].reverse().find((m) => m.role === 'user');
      if (lastUserMsg?.content) {
        const prompts = generateSmartFollowUps(lastUserMsg.content);
        followUpPrompts.value = prompts;
        lastMsg.followUpPrompts = prompts;
      }
    } else {
      followUpPrompts.value = [];
    }
  }

  async function loadSessionMessagesFromBackend(sessionId: string) {
    isSessionLoading.value = true;
    try {
      const res = await getMessages(sessionId);
      messages.value = (res.data || []).map((item) =>
        mapApiMessage(item as unknown as Record<string, unknown>)
      );
      conversationId.value = sessionId;
      storeSessionId(activeCourseId.value, sessionId);
      restoreFollowUpsForLastTurn();
    } catch {
      messages.value = [];
      followUpPrompts.value = [];
      ElMessage.error('加载会话记录失败');
    } finally {
      isSessionLoading.value = false;
    }
  }

  function toggleHistoryPanel() {
    isHistoryPanelOpen.value = !isHistoryPanelOpen.value;
    if (isHistoryPanelOpen.value) {
      sessions.value = readSessionsFromStorage();
    }
  }

  async function selectSession(session: GlobalAssistantSession) {
    if (isStreaming.value) {
      ElMessage.warning('当前正在生成中，请先停止生成');
      return;
    }
    if (messages.value.length > 0) {
      saveCurrentSessionToHistory();
    }
    conversationId.value = session.id;
    followUpPrompts.value = [];
    resetStreamingState();
    isHistoryPanelOpen.value = false;

    if (session.messages && session.messages.length > 0) {
      messages.value = JSON.parse(JSON.stringify(session.messages));
      restoreFollowUpsForLastTurn();
    } else if (isBackendConversationId(session.id)) {
      await loadSessionMessagesFromBackend(session.id);
    } else {
      messages.value = [];
      followUpPrompts.value = [];
    }

    nextTick(() => {
      scrollToBottomInstant();
      bindMarkdownCodeCopy(messagesScrollRef.value);
    });
  }

  function confirmDeleteSession(sessionId: string) {
    const target = sessions.value.find((s) => s.id === sessionId);
    ElMessageBox.confirm(
      `确定要删除「${target?.title || '该会话'}」吗？删除后将无法找回。`,
      '删除历史会话',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        lockScroll: false
      }
    )
      .then(async () => {
        if (isBackendConversationId(sessionId)) {
          try {
            await deleteConversation(sessionId);
          } catch {
            // 本地会话仍允许删除
          }
        }
        sessions.value = sessions.value.filter((s) => s.id !== sessionId);
        writeSessionsToStorage(sessions.value);
        clearStoredSessionId(activeCourseId.value, sessionId);
        if (conversationId.value === sessionId) {
          conversationId.value = undefined;
          messages.value = [];
          followUpPrompts.value = [];
          resetStreamingState();
        }
        ElMessage.success('会话已删除');
      })
      .catch(() => {});
  }

  function confirmClearAllSessions() {
    if (sessions.value.length === 0) return;
    ElMessageBox.confirm('确定要清空全部历史会话记录吗？清空后将无法恢复。', '清空全部历史记录', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      lockScroll: false
    })
      .then(async () => {
        if (isStreaming.value) {
          stopStreaming();
        }
        const ids = sessions.value.map((s) => s.id);
        await Promise.all(
          ids.filter(isBackendConversationId).map((id) => deleteConversation(id).catch(() => undefined))
        );
        sessions.value = [];
        writeSessionsToStorage([]);
        conversationId.value = undefined;
        messages.value = [];
        followUpPrompts.value = [];
        resetStreamingState();
        storage.remove(sessionStorageKey(activeCourseId.value));
        ElMessage.success(ids.length > 0 ? `已清空 ${ids.length} 条历史会话` : '暂无历史会话可清空');
      })
      .catch(() => {});
  }

  function clearMessages() {
    if (messages.value.length === 0) return;
    ElMessageBox.confirm('确定要清空当前的全部对话记录吗？清空后将无法恢复。', '清空会话确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      lockScroll: false
    })
      .then(async () => {
        const currentId = conversationId.value;
        if (currentId) {
          if (isBackendConversationId(currentId)) {
            try {
              await deleteConversation(currentId);
            } catch {
              // ignore
            }
          }
          sessions.value = sessions.value.filter((s) => s.id !== currentId);
          writeSessionsToStorage(sessions.value);
          clearStoredSessionId(activeCourseId.value, currentId);
        }
        messages.value = [];
        conversationId.value = undefined;
        followUpPrompts.value = [];
        resetStreamingState();
        ElMessage.success('已清空当前对话记录');
      })
      .catch(() => {});
  }

  function startNewSession() {
    if (isStreaming.value) {
      stopStreaming();
    }
    if (messages.value.length > 0) {
      saveCurrentSessionToHistory();
    }
    if (messages.value.length === 0 && !conversationId.value) {
      isHistoryPanelOpen.value = false;
      return;
    }
    conversationId.value = undefined;
    messages.value = [];
    followUpPrompts.value = [];
    resetStreamingState();
    isHistoryPanelOpen.value = false;
  }


  function getIntentTagType(intent?: string) {
    if (intent === 'agent' || intent === 'EXAM_COMPOSE') return 'danger';
    if (intent === 'rag' || intent === 'KNOWLEDGE_RETRIEVAL') return 'warning';
    if (intent === 'navigate' || intent === 'REPORT_ANALYTICS') return 'success';
    return 'primary';
  }

  function applyIntentEvent(data: GlobalAssistantIntentEvent) {
    const routeType = data.route || '';
    const agentCode = data.agentCode || '';
    streamingIntent.value = {
      intent: routeType,
      intentDesc: resolveIntentDesc(routeType, agentCode),
      targetCode: resolveTargetPath(routeType, agentCode)
    };
  }

  function stopStreaming() {
    if (!isStreaming.value) return;
    userStoppedGeneration.value = true;
    sseClient.stop();
    isStreaming.value = false;

    const answer = streamingAnswerBody.value || streamingContent.value;
    finishStreamingMarkdown();
    messages.value.push({
      id: Date.now(),
      role: 'assistant',
      content: buildStoppedGenerationContent(answer),
      reasoningContent: cleanReasoningText(streamingReasoning.value || streamingThinkingBody.value),
      intent: streamingIntent.value.intent,
      intentDesc: streamingIntent.value.intentDesc,
      targetCode: streamingIntent.value.targetCode,
      citations: [...streamingCitations.value],
      createdAt: Date.now()
    });

    resetStreamingState();
    saveCurrentSessionToHistory();
    ElMessage.info('已停止生成');
    nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
  }

  function resetStreamingState() {
    streamingReasoning.value = '';
    streamingContent.value = '';
    streamingCitations.value = [];
    streamingIntent.value = {};
    isReasoningFolded.value = getInitialReasoningFolded();
    isReasoningActive.value = false;
    streamPhaseMessage.value = '';
    answerStreamStarted.value = false;
    resetStreamingMarkdown();
  }

  function finalizeAssistantMessage() {
    finishStreamingMarkdown();
    const finalReasoning = cleanReasoningText(
      streamingReasoning.value || streamingThinkingBody.value
    );
    const finalAnswer = streamingAnswerBody.value || streamingContent.value;

    messages.value.push({
      id: Date.now(),
      role: 'assistant',
      content: finalAnswer.trim(),
      reasoningContent: finalReasoning,
      intent: streamingIntent.value.intent,
      intentDesc: streamingIntent.value.intentDesc,
      targetCode: streamingIntent.value.targetCode,
      citations: [...streamingCitations.value],
      followUpPrompts: [...followUpPrompts.value],
      createdAt: Date.now()
    });

    resetStreamingState();
    saveCurrentSessionToHistory();
    window.dispatchEvent(new CustomEvent('edumind:ai-usage-changed'));
    nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
  }

  async function streamChat(query: string): Promise<boolean> {
    return streamGlobalAssistantChat(
      sseClient,
      query,
      buildAssistantRequestBase(),
      streamingContent,
      {
        onIntent: (data) => applyIntentEvent(data as GlobalAssistantIntentEvent),
        onStatus: (message, phase) => {
          streamPhaseMessage.value = message;
          if (phase === 'reasoning') isReasoningActive.value = true;
          if (phase === 'composing') isReasoningActive.value = false;
        },
        onReasoningChunk: (chunk) => {
          streamingReasoning.value += chunk;
          isReasoningActive.value = true;
          streamPhaseMessage.value = '';
        },
        onDeltaChunk: (_chunk, answerDelta) => {
          if (!answerDelta) return;
          isReasoningActive.value = false;
          streamPhaseMessage.value = '';
          if (!answerStreamStarted.value) {
            answerStreamStarted.value = true;
            isReasoningFolded.value = getInitialReasoningFolded();
          }
          appendStreamingMarkdown(answerDelta);
        },
        onCitations: (cits) => {
          streamingCitations.value = cits;
        },
        onDone: (data) => {
          const d = data as {
            conversationId?: string;
            citations?: CitationItem[];
            reasoningContent?: string;
          };
          if (d.conversationId) conversationId.value = String(d.conversationId);
          if (d.citations && d.citations.length > 0) {
            streamingCitations.value = d.citations;
          }
          if (d.reasoningContent && !streamingReasoning.value) {
            streamingReasoning.value = String(d.reasoningContent);
          }
          followUpPrompts.value = generateSmartFollowUps(query);
          finalizeAssistantMessage();
        },
        onFollowOutput: scheduleFollowStreamOutput
      }
    );
  }

  async function fallbackAsk(query: string) {
    const res = await askGlobalAssistant({
      message: query,
      ...buildAssistantRequestBase()
    });
    const data = res?.data;
    if (!data) {
      throw new Error('服务未返回有效数据');
    }
    conversationId.value = data.conversationId;
    streamingIntent.value = {
      intent: data.intent,
      intentDesc: data.intentDesc,
      targetCode: data.targetCode
    };
    if (data.reasoningContent) {
      streamingReasoning.value = data.reasoningContent;
    }
    if (data.citations && Array.isArray(data.citations)) {
      streamingCitations.value = data.citations as CitationItem[];
    }
    streamingContent.value = data.content || '已处理您的教学助手请求。';
    followUpPrompts.value = generateSmartFollowUps(query);
    finalizeAssistantMessage();
  }

  async function handleSubmit(options?: { isRegenerate?: boolean } | Event) {
    const query = inputContent.value.trim();
    if (!query || isStreaming.value) return;

    const isRegenerate = Boolean(options && typeof options === 'object' && 'isRegenerate' in options && (options as { isRegenerate?: boolean }).isRegenerate);
    if (!isRegenerate) {
      const lessonInsertIntent = teachingCopilotStore.activeContext?.lessonInsertIntent;
      teachingCopilotStore.patchContext({ lessonInsertIntent: undefined });
      messages.value.push({
        id: Date.now(),
        role: 'user',
        content: query,
        createdAt: Date.now(),
        ...(lessonInsertIntent ? { lessonInsertIntent } : {})
      });
    }
    inputContent.value = '';
    followUpPrompts.value = [];
    resetStreamingState();
    isStreaming.value = true;
    scrollToBottomInstant();

    try {
      const streamed = await streamChat(query);
      if (userStoppedGeneration.value) return;
      if (!streamed && !streamingContent.value) {
        if (isRegenerate) {
          throw new Error('重新生成未返回有效内容，请稍后重试');
        }
        await fallbackAsk(query);
      }
    } catch {
      if (userStoppedGeneration.value) return;
      if (isRegenerate) {
        const message = '重新生成失败，请检查模型 API Key 或网络后重试';
        messages.value.push({
          id: Date.now() + 2,
          role: 'assistant',
          content: message,
          error: true,
          createdAt: Date.now()
        });
        resetStreamingState();
        saveCurrentSessionToHistory();
      } else {
        try {
          await fallbackAsk(query);
        } catch (err: unknown) {
          const message = err instanceof Error ? err.message : '服务异常，请检查网络或稍后重试';
          messages.value.push({
            id: Date.now() + 2,
            role: 'assistant',
            content: `请求失败：${message}`,
            error: true,
            createdAt: Date.now()
          });
          resetStreamingState();
          saveCurrentSessionToHistory();
        }
      }
    } finally {
      userStoppedGeneration.value = false;
      isStreaming.value = false;
      scrollToBottomInstant();
      nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
    }
  }

  function handleSendPrompt(promptText: string) {
    if (isStreaming.value) return;
    inputContent.value = promptText;
    handleSubmit();
  }

  function handleRegenerate(targetIdx: number) {
    if (isStreaming.value) return;
    let userPrompt = '';
    for (let i = targetIdx; i >= 0; i--) {
      if (messages.value[i]?.role === 'user') {
        userPrompt = messages.value[i].content;
        break;
      }
    }
    if (!userPrompt) return;

    messages.value.splice(targetIdx);
    inputContent.value = userPrompt;
    handleSubmit({ isRegenerate: true });
  }

  async function confirmDeleteMessage(targetIdx: number) {
    try {
      await ElMessageBox.confirm(
        '将删除本条及其对应的一问一答，删除后无法恢复。确定继续吗？',
        '删除对话确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          lockScroll: false
        }
      );
    } catch {
      return;
    }

    const idsToDelete = collectPairedMessageIds(messages.value, targetIdx);
    if (!idsToDelete.length) return;

    const persistedId = idsToDelete.find((id) => isPersistedMessageId(id));
    try {
      let deletedIds = idsToDelete;
      if (persistedId) {
        const res = await deleteMessage(persistedId);
        if (res?.data?.deletedIds?.length) {
          deletedIds = res.data.deletedIds;
        }
      }
      messages.value = removeMessagesByIds(messages.value, deletedIds);
      saveCurrentSessionToHistory();
      ElMessage.success('已删除本轮对话记录');
    } catch {
      if (!persistedId) {
        messages.value = removeMessagesByIds(messages.value, idsToDelete);
        saveCurrentSessionToHistory();
        ElMessage.success('已删除本轮对话记录');
        return;
      }
      ElMessage.error('删除失败，请稍后重试');
    }
  }

  async function copyMessage(content: string) {
    if (!content) return;
    try {
      await navigator.clipboard.writeText(content);
      ElMessage.success('已复制内容到剪贴板');
    } catch {
      ElMessage.warning('复制失败，请手动选取复制');
    }
  }

  function jumpToCitation(citation: CitationItem) {
    if (citation.chunkId) {
      drawerVisible.value = false;
      router.push({
        path: '/knowledge/retrieval',
        query: { chunkId: String(citation.chunkId), doc: citation.documentName || '' }
      });
    } else {
      ElMessage.info(`参考来源：${citation.documentName || '课程知识切片'}`);
    }
  }

  function handleNavigate(path: string) {
    drawerVisible.value = false;
    router.push(path);
  }

  function formatMatchScore(score?: number): string {
    if (score == null || Number.isNaN(score)) return '92%';
    const pct = score <= 1 ? Math.round(score * 100) : Math.round(score);
    return `${pct}%`;
  }

  watch(
    () => messages.value.length,
    () => {
      nextTick(() => bindMarkdownCodeCopy(messagesScrollRef.value));
    }
  );

  function handleOpenAssistantEvent(e: Event) {
    const detail = (e as CustomEvent<{ prefill?: string; autoSend?: boolean }>).detail;
    const prefill = detail?.prefill?.trim();
    const autoSend = Boolean(detail?.autoSend && prefill);
    drawerVisible.value = true;
    sessions.value = readSessionsFromStorage();
    restoreFollowUpsForLastTurn();
    if (prefill) {
      inputContent.value = prefill;
    }
    nextTick(() => {
      scrollToBottomSmooth();
      bindMarkdownCodeCopy(messagesScrollRef.value);
      if (autoSend) {
        void handleSubmit();
      }
    });
  }

  onMounted(() => {
    window.addEventListener(OPEN_GLOBAL_ASSISTANT_EVENT, handleOpenAssistantEvent);
  });

  onUnmounted(() => {
    window.removeEventListener(OPEN_GLOBAL_ASSISTANT_EVENT, handleOpenAssistantEvent);
  });

  return {
    drawerVisible,
    inputContent,
    isStreaming,
    messages,
    conversationId,
    messagesScrollRef,
    streamAnchorRef,
    showScrollToBottom,
    activeCourseId,
    activeCourseLabel,
    manualGlobalScope,
    toggleScopeMode,
    presetChips,
    isLessonStudioContext,
    followUpPrompts,
    showThinkingPanel: computed(() => !isThinkingPanelHidden()),
    // 历史会话管理 (对标 Code Compass 原型)
    isHistoryPanelOpen,
    isSessionLoading,
    sessions,
    toggleHistoryPanel,
    selectSession,
    confirmDeleteSession,
    confirmClearAllSessions,
    formatSessionTime,
    // 流式状态
    streamingReasoning,
    streamingContent,
    streamingCitations,
    streamingIntent,
    streamingRenderedHtml,
    streamingAnswerBody,
    streamingThinkingDisplay,
    isReasoningFolded,
    isReasoningActive,
    streamPhaseMessage,
    // 方法
    toggleDrawer,
    clearMessages,
    startNewSession,
    handleSubmit,
    handleSendPrompt,
    stopStreaming,
    handleRegenerate,
    confirmDeleteMessage,
    copyMessage,
    jumpToCitation,
    handleNavigate,
    scrollToBottomSmooth,
    scrollToBottomInstant,
    scheduleFollowStreamOutput,
    pauseAutoScrollFollow,
    handleViewportScroll,
    getIntentTagType,
    formatMatchScore
  };
}

