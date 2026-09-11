import { aiChatApi } from '@/api/ai/chat';
import { useAIChatStore } from '@/stores/ai';

export class AIChatService {
  /**
   * 创建或激活新会话，编排前后端状态同步
   */
  public static async initConversation(courseId?: number, initialTitle?: string) {
    const aiStore = useAIChatStore();
    try {
      const res = await aiChatApi.createConversation({
        courseId,
        title: initialTitle || '新的课程教学会话'
      });
      if (res && res.data) {
        aiStore.setCurrentSession(res.data.id);
        return res.data;
      }
    } catch (e) {
      console.warn('后端服务不可用，进入前端模拟会话模式');
      const mockSessionId = 'session_' + Date.now();
      aiStore.setCurrentSession(mockSessionId);
      return { id: mockSessionId, title: initialTitle || '新的课程教学会话' };
    }
  }

  /**
   * 编排流式消息接收与打字机状态
   */
  public static parseSSEResponseChunk(chunk: string): { content: string; citations?: string[]; isDone: boolean } {
    if (!chunk || chunk.trim() === '') {
      return { content: '', isDone: false };
    }
    if (chunk.includes('[DONE]')) {
      return { content: '', isDone: true };
    }
    try {
      const data = JSON.parse(chunk.replace(/^data:\s*/, ''));
      return {
        content: data.delta || data.content || '',
        citations: data.citations || [],
        isDone: Boolean(data.done)
      };
    } catch (e) {
      return { content: chunk, isDone: false };
    }
  }
}

export const aiChatService = AIChatService;
