import { aiChatApi, getChatModelsRaw, type ChatModelVO } from '@/api/ai/chat';
import { DEFAULT_CHAT_MODELS } from '@/constants/ai';
import { resolveModelConfigs } from '@/composables/system/useAIModel';
import type { ModelProviderConfig } from '@/types/system/model';
import { useAIChatStore } from '@/stores/ai';

function mapChatModelVo(list: ChatModelVO[]): ModelProviderConfig[] {
  return list
    .filter((m) => m.enabled !== false)
    .map((m) => ({
      id: m.id,
      modelKey: m.modelKey,
      name: m.name || m.modelKey,
      provider: (m.provider || 'DeepSeek') as ModelProviderConfig['provider'],
      endpoint: '',
      apiKeyMasked: '',
      contextLength: 32000,
      maxOutputTokens: 8192,
      temperature: 0.3,
      supportsStreaming: true,
      supportsEmbedding: false,
      supportsVision: false,
      enabled: true,
      isDefault: !!m.isDefault,
      costPer1kPrompt: 0,
      costPer1kCompletion: 0,
      healthStatus: 'HEALTHY' as const
    }));
}

/** 课程 AI 可用模型：优先对话接口，管理员可降级系统配置，最后使用本地默认 */
export async function resolveChatModels(): Promise<ModelProviderConfig[]> {
  try {
    const res = await getChatModelsRaw();
    const list = Array.isArray(res?.data) ? res.data : [];
    if (list.length > 0) {
      return mapChatModelVo(list);
    }
  } catch {
    // 后端未重启或接口暂不可用时继续降级
  }

  try {
    const configs = await resolveModelConfigs();
    const chatModels = configs.filter((m) => m.enabled && !m.supportsEmbedding);
    if (chatModels.length > 0) {
      return chatModels;
    }
  } catch {
    // ignore
  }

  return DEFAULT_CHAT_MODELS;
}

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
