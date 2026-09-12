import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { SSEClient } from '@/core/sse/client';
import {
  getConversations,
  getMessages,
  createConversation,
  renameConversation,
  deleteConversation,
  generateConversationTitle,
  cancelChatStream
} from '@/api/ai/chat';
import { USE_MOCK } from '@/config/mock';
import type { CitationItem } from '@/components/knowledge/CitationList.vue';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  createdAt: string;
  isStreaming?: boolean;
  citations?: CitationItem[];
}

export interface ChatSession {
  id: string;
  title: string;
  updatedAt: string;
}

function mapCitation(raw: Record<string, unknown>): CitationItem {
  return {
    id: raw.chunkId as number | string | undefined,
    docTitle: (raw.documentName as string) || (raw.docTitle as string),
    documentName: raw.documentName as string,
    page: raw.pageNo as number | undefined,
    pageNo: raw.pageNo as number | undefined,
    score: raw.score as number | undefined,
    snippet: (raw.excerpt as string) || (raw.snippet as string),
    excerpt: raw.excerpt as string
  };
}

export function useAIStream() {
  const sseClient = new SSEClient();
  const streaming = ref(false);
  const sessions = ref<ChatSession[]>([]);
  const currentSessionId = ref<string>('');
  const messages = ref<ChatMessage[]>([]);
  let currentStreamId = '';

  function mapSession(raw: any): ChatSession {
    return {
      id: String(raw?.id || `sess_${Date.now()}`),
      title: (raw?.title as string) || '新会话',
      updatedAt: (raw?.updatedAt as string) || (raw?.createTime as string) || '刚刚'
    };
  }

  function mapMessage(raw: any): ChatMessage {
    const citations = Array.isArray(raw?.citations)
      ? (raw.citations as any[]).map(mapCitation)
      : undefined;
    return {
      id: (raw.id as string) || `msg_${Date.now()}`,
      role: (raw.role || 'assistant') as ChatMessage['role'],
      content: (raw.content as string) || '',
      createdAt: raw.createTime
        ? new Date(raw.createTime as string).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      citations
    };
  }

  async function loadSessions(courseId?: number) {
    try {
      const res = await getConversations(courseId);
      sessions.value = (res.data || []).map((item) => mapSession(item as unknown as Record<string, unknown>));
      if (sessions.value.length > 0 && !currentSessionId.value) {
        currentSessionId.value = sessions.value[0].id;
        await loadMessages(currentSessionId.value);
      }
    } catch {
      if (USE_MOCK && sessions.value.length === 0) {
        sessions.value = [
          { id: 'sess_mock_1', title: 'Java面向对象与多态问答', updatedAt: '今天 10:25' },
          { id: 'sess_mock_2', title: '异常体系与分部积分答疑', updatedAt: '昨天 16:40' }
        ];
        currentSessionId.value = 'sess_mock_1';
      }
    }
  }

  async function loadMessages(conversationId: string) {
    try {
      const res = await getMessages(conversationId);
      messages.value = (res.data || []).map((item) => mapMessage(item));
    } catch {
      if (USE_MOCK && messages.value.length === 0) {
        messages.value = [
          {
            id: 'welcome',
            role: 'assistant',
            content:
              '你好！我是本课程的专属 AI 助教。已为你加载当前课程知识库与教学大纲，关于章节知识、典型例题或代码实现，请随时向我提问！',
            createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          }
        ];
      }
    }
  }

  interface StreamOptions {
    chapterId?: number;
    modelKey?: string;
    useRag?: boolean;
  }

  async function sendMessage(promptText: string, courseId: number = 101, options?: StreamOptions) {
    if (!promptText.trim() || streaming.value) return;

    if (!currentSessionId.value) {
      await createNewSession(courseId);
    }

    const userMsg: ChatMessage = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: promptText.trim(),
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    messages.value.push(userMsg);

    const assistantMsg: ChatMessage = {
      id: `ai_${Date.now()}`,
      role: 'assistant',
      content: '',
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isStreaming: true,
      citations: []
    };
    messages.value.push(assistantMsg);
    streaming.value = true;

    const curSess = sessions.value.find((s) => s.id === currentSessionId.value);
    if (curSess && (curSess.title === '新会话' || !curSess.title)) {
      generateConversationTitle(curSess.id)
        .then((res) => {
          const title = res.data || promptText.trim().slice(0, 15);
          renameSession(curSess.id, title);
        })
        .catch(() => {
          const fallback = promptText.trim().slice(0, 15) + (promptText.length > 15 ? '...' : '');
          renameSession(curSess.id, fallback);
        });
    }

    try {
      await sseClient.streamEvents(
        '/api/ai/chat/stream',
        {
          conversationId: currentSessionId.value,
          courseId,
          chapterId: options?.chapterId,
          modelKey: options?.modelKey,
          message: promptText,
          useRag: options?.useRag ?? true
        },
        (event, data) => {
          if (event === 'stream' && data.streamId) {
            currentStreamId = String(data.streamId);
            return;
          }
          if (event === 'delta' || event === 'message') {
            const chunk = String(data.content || data.text || '');
            if (chunk) assistantMsg.content += chunk;
            return;
          }
          if (event === 'citation' && Array.isArray(data.citations)) {
            assistantMsg.citations = (data.citations as Record<string, unknown>[]).map(mapCitation);
            return;
          }
          if (event === 'done') {
            if (data.messageId) {
              assistantMsg.id = String(data.messageId);
            }
            return;
          }
          if (event === 'error') {
            throw new Error(String(data.message || data.error || 'AI 流式响应失败'));
          }
        },
        () => {
          assistantMsg.isStreaming = false;
          streaming.value = false;
          currentStreamId = '';
        },
        (err) => {
          assistantMsg.isStreaming = false;
          streaming.value = false;
          currentStreamId = '';
          const message = err instanceof Error ? err.message : 'AI 对话失败';
          if (!USE_MOCK) {
            assistantMsg.content = assistantMsg.content || `*${message}*`;
            ElMessage.error(message);
          } else {
            fallbackMockTypewriter(assistantMsg, promptText);
          }
        }
      );
    } catch (err) {
      assistantMsg.isStreaming = false;
      streaming.value = false;
      if (USE_MOCK) {
        fallbackMockTypewriter(assistantMsg, promptText);
      } else {
        ElMessage.error(err instanceof Error ? err.message : 'AI 对话失败');
      }
    }
  }

  let mockTimer: ReturnType<typeof setInterval> | null = null;

  function fallbackMockTypewriter(assistantMsg: ChatMessage, promptText: string) {
    if (mockTimer) {
      clearInterval(mockTimer);
      mockTimer = null;
    }
    assistantMsg.isStreaming = true;
    streaming.value = true;

    const reply = `针对你的问题【${promptText}】，我已结合当前课程知识大纲与课件资料库为你提炼权威解析。`;

    let index = 0;
    mockTimer = setInterval(() => {
      if (index < reply.length) {
        assistantMsg.content += reply[index++];
      } else {
        if (mockTimer) {
          clearInterval(mockTimer);
          mockTimer = null;
        }
        assistantMsg.isStreaming = false;
        streaming.value = false;
      }
    }, 28);
  }

  function stopStream() {
    if (mockTimer) {
      clearInterval(mockTimer);
      mockTimer = null;
    }
    sseClient.stop();
    streaming.value = false;
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg?.isStreaming) {
      lastMsg.isStreaming = false;
      lastMsg.content += '\n\n*(已手动停止生成)*';
    }
    if (currentStreamId) {
      cancelChatStream(currentStreamId).catch(() => undefined);
      currentStreamId = '';
    }
  }

  async function createNewSession(courseId?: number) {
    try {
      const res = await createConversation({ courseId, title: '新会话' });
      const session = mapSession((res.data || {}) as unknown as Record<string, unknown>);
      sessions.value.unshift(session);
      currentSessionId.value = session.id;
      messages.value = [];
    } catch {
      const newId = `sess_${Date.now()}`;
      sessions.value.unshift({ id: newId, title: '新会话', updatedAt: '刚刚' });
      currentSessionId.value = newId;
      messages.value = [];
    }
  }

  async function switchSession(id: string) {
    currentSessionId.value = id;
    await loadMessages(id);
  }

  async function removeSession(id: string) {
    try {
      await deleteConversation(id);
    } catch {
      // ignore
    }
    sessions.value = sessions.value.filter((s) => s.id !== id);
    if (currentSessionId.value === id) {
      currentSessionId.value = sessions.value[0]?.id || '';
      if (currentSessionId.value) await loadMessages(currentSessionId.value);
      else messages.value = [];
    }
  }

  async function renameSession(id: string, title: string) {
    try {
      await renameConversation(id, title);
      const s = sessions.value.find((x) => x.id === id);
      if (s) s.title = title;
    } catch {
      const s = sessions.value.find((x) => x.id === id);
      if (s) s.title = title;
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    streaming,
    loadSessions,
    loadMessages,
    sendMessage,
    stopStream,
    createNewSession,
    switchSession,
    deleteSession: removeSession,
    renameSession
  };
}
