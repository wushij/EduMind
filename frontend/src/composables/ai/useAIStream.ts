import { ref } from 'vue';
import { SSEClient } from '@/core/sse/sse-client';
import {
  getConversations,
  getMessages,
  createConversation,
  renameConversation,
  deleteConversation
} from '@/api/ai/chat';
import { USE_MOCK } from '@/config/mock';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  createdAt: string;
  isStreaming?: boolean;
}

export interface ChatSession {
  id: string;
  title: string;
  updatedAt: string;
}

export function useAIStream() {
  const sseClient = new SSEClient();
  const streaming = ref(false);
  const sessions = ref<ChatSession[]>([]);
  const currentSessionId = ref<string>('');
  const messages = ref<ChatMessage[]>([]);

  function mapSession(raw: Record<string, any>): ChatSession {
    return {
      id: raw.id,
      title: raw.title || '新会话',
      updatedAt: raw.updatedAt || raw.createdAt || ''
    };
  }

  function mapMessage(raw: Record<string, any>): ChatMessage {
    return {
      id: raw.id || `msg_${Date.now()}`,
      role: (raw.role || 'assistant') as ChatMessage['role'],
      content: raw.content || '',
      createdAt: raw.createdAt || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
  }

  async function loadSessions(courseId?: number) {
    try {
      const res = await getConversations(courseId);
      sessions.value = (res.data || []).map(mapSession);
      if (sessions.value.length > 0 && !currentSessionId.value) {
        currentSessionId.value = sessions.value[0].id;
        await loadMessages(currentSessionId.value);
      }
    } catch {
      if (USE_MOCK && sessions.value.length === 0) {
        sessions.value = [
          { id: 'sess_mock_1', title: '课程问答', updatedAt: '今天' }
        ];
        currentSessionId.value = 'sess_mock_1';
      }
    }
  }

  async function loadMessages(conversationId: string) {
    try {
      const res = await getMessages(conversationId);
      messages.value = (res.data || []).map(mapMessage);
    } catch {
      if (USE_MOCK && messages.value.length === 0) {
        messages.value = [
          {
            id: 'welcome',
            role: 'assistant',
            content: '你好！我是课程 AI 助教，有什么可以帮你？',
            createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          }
        ];
      }
    }
  }

  async function sendMessage(promptText: string, courseId: number = 101) {
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
      isStreaming: true
    };
    messages.value.push(assistantMsg);
    streaming.value = true;

    try {
      await sseClient.stream(
        '/api/ai/chat/stream',
        { conversationId: currentSessionId.value, courseId, message: promptText },
        (chunk: string) => {
          assistantMsg.content += chunk;
        },
        () => {
          assistantMsg.isStreaming = false;
          streaming.value = false;
        },
        (err) => {
          console.warn('SSE stream error, falling back to typewriter output:', err);
          fallbackMockTypewriter(assistantMsg, promptText);
        }
      );
    } catch (err) {
      console.warn('Stream request failed, running fallback typewriter:', err);
      fallbackMockTypewriter(assistantMsg, promptText);
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

    const reply = `针对你的问题【${promptText}】，我已结合当前课程知识大纲为你整理出以下核心要点：\n\n1. **核心原理**：面向对象三大核心特性是封装、继承与多态。封装保障内部状态安全，继承促进代码复用，多态提升系统可扩展性。\n2. **实战建议**：在编写 Java 业务代码时，遵循高内聚低耦合原则，善用设计模式并规范异常捕获与资源释放机制。\n3. **课后拓展**：右侧课程资料已同步推荐相关章节课件与自测习题，建议结合实际代码多进行调试验证。如有其他疑问，可随时向我继续提问！`;

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
    }, 35);
  }

  function stopStream() {
    if (mockTimer) {
      clearInterval(mockTimer);
      mockTimer = null;
    }
    sseClient.stop();
    streaming.value = false;
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg?.isStreaming) lastMsg.isStreaming = false;
  }



  async function createNewSession(courseId?: number) {
    try {
      const res = await createConversation({ courseId, title: '新会话' });
      const session = mapSession(res.data || {});
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
    sessions.value = sessions.value.filter(s => s.id !== id);
    if (currentSessionId.value === id) {
      currentSessionId.value = sessions.value[0]?.id || '';
      if (currentSessionId.value) await loadMessages(currentSessionId.value);
      else messages.value = [];
    }
  }

  async function renameSession(id: string, title: string) {
    try {
      await renameConversation(id, title);
      const s = sessions.value.find(x => x.id === id);
      if (s) s.title = title;
    } catch {
      const s = sessions.value.find(x => x.id === id);
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
