import { ref, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { SSEClient } from '@/core/sse/client';
import { API_BASE_URL } from '@/config';
import { askGlobalAssistant } from '@/api/ai/assistant';
import type {
  GlobalAssistantIntentEvent,
  GlobalAssistantMessage
} from '@/types/ai/assistant';

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

export function useGlobalAssistant() {
  const route = useRoute();
  const sseClient = new SSEClient();

  const drawerVisible = ref(false);
  const inputContent = ref('');
  const isStreaming = ref(false);
  const messages = ref<GlobalAssistantMessage[]>([]);
  const conversationId = ref<string>();
  const messagesScrollRef = ref<HTMLDivElement | null>(null);

  const activeCourseId = computed(() => {
    const queryCourseId = route.query.courseId;
    if (queryCourseId) {
      const parsed = Number(queryCourseId);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    const paramCourseId = route.params.courseId || route.params.id;
    if (paramCourseId) {
      const parsed = Number(paramCourseId);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    return 102;
  });

  const presetChips = [
    { label: '智能组卷', prompt: '帮我出一份包含导数与微分的期中试卷' },
    { label: '检索切片', prompt: '请检索微积分第一章的核心切片和知识点资料' },
    { label: '学情看板', prompt: '我想看看班级的学情分析报表' },
    { label: '知识图谱', prompt: '展示当前课程的知识图谱拓扑结构' }
  ];

  function toggleDrawer() {
    drawerVisible.value = !drawerVisible.value;
    if (drawerVisible.value) {
      nextTick(scrollToBottom);
    }
  }

  function clearMessages() {
    messages.value = [];
    conversationId.value = undefined;
  }

  function scrollToBottom() {
    if (messagesScrollRef.value) {
      messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight;
    }
  }

  function getIntentTagType(intent?: string) {
    if (intent === 'agent' || intent === 'EXAM_COMPOSE') return 'danger';
    if (intent === 'rag' || intent === 'KNOWLEDGE_RETRIEVAL') return 'warning';
    if (intent === 'navigate' || intent === 'REPORT_ANALYTICS') return 'success';
    return 'primary';
  }

  function applyIntentEvent(assistantMsg: GlobalAssistantMessage, data: GlobalAssistantIntentEvent) {
    const routeType = data.route || '';
    const agentCode = data.agentCode || '';
    assistantMsg.intent = routeType;
    assistantMsg.intentDesc = resolveIntentDesc(routeType, agentCode);
    assistantMsg.targetCode = resolveTargetPath(routeType, agentCode);
  }

  async function streamChat(query: string, assistantMsg: GlobalAssistantMessage): Promise<boolean> {
    let receivedContent = false;

    await sseClient.streamEvents(
      `${API_BASE_URL}/ai/assistant/chat`,
      {
        message: query,
        courseId: activeCourseId.value,
        conversationId: conversationId.value
      },
      (event, data) => {
        if (event === 'intent') {
          applyIntentEvent(assistantMsg, data as GlobalAssistantIntentEvent);
        } else if (event === 'delta' || event === 'message') {
          const chunk = String(data.content || data.text || '');
          if (chunk) {
            assistantMsg.content += chunk;
            receivedContent = true;
          }
        } else if (event === 'done') {
          if (data.conversationId) {
            conversationId.value = String(data.conversationId);
          }
        } else if (event === 'error') {
          throw new Error(String(data.message || '流式响应异常'));
        }
        scrollToBottom();
      }
    );

    return receivedContent;
  }

  async function fallbackAsk(query: string, assistantMsg: GlobalAssistantMessage) {
    const res = await askGlobalAssistant({
      message: query,
      courseId: activeCourseId.value,
      conversationId: conversationId.value
    });
    const data = res?.data;
    if (!data) {
      assistantMsg.content = '无法识别请求，请稍后重试。';
      return;
    }
    conversationId.value = data.conversationId;
    assistantMsg.intent = data.intent;
    assistantMsg.intentDesc = data.intentDesc;
    assistantMsg.content = data.content || '已处理您的教学助手请求。';
    assistantMsg.targetCode = data.targetCode;
  }

  async function handleSubmit() {
    const query = inputContent.value.trim();
    if (!query || isStreaming.value) return;

    messages.value.push({ role: 'user', content: query });
    inputContent.value = '';
    scrollToBottom();

    const assistantMsg: GlobalAssistantMessage = {
      role: 'assistant',
      content: '',
      streaming: true
    };
    messages.value.push(assistantMsg);
    isStreaming.value = true;
    scrollToBottom();

    try {
      const streamed = await streamChat(query, assistantMsg);
      if (!streamed && !assistantMsg.content) {
        await fallbackAsk(query, assistantMsg);
      }
    } catch {
      try {
        await fallbackAsk(query, assistantMsg);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : '服务异常，请检查网络或重试';
        assistantMsg.content = `请求失败：${message}`;
      }
    } finally {
      assistantMsg.streaming = false;
      isStreaming.value = false;
      scrollToBottom();
    }
  }

  function handleSendPrompt(promptText: string) {
    inputContent.value = promptText;
    handleSubmit();
  }

  function stopStreaming() {
    sseClient.stop();
    isStreaming.value = false;
  }

  return {
    drawerVisible,
    inputContent,
    isStreaming,
    messages,
    messagesScrollRef,
    activeCourseId,
    presetChips,
    toggleDrawer,
    clearMessages,
    handleSubmit,
    handleSendPrompt,
    getIntentTagType,
    scrollToBottom,
    stopStreaming
  };
}
