import { post, get, put, del } from '@/core/http/request';
import { DEFAULT_CHAT_MODELS } from '@/constants/ai';
import { AIConversation } from '@/types/ai/conversation';
import { AIMessage } from '@/types/ai/message';
import { ModelProviderConfig } from '@/types/system/model';
import { getModelConfigs } from '@/api/system/model';

interface ChatModelVO {
  id: number;
  modelKey: string;
  name?: string;
  provider: string;
  enabled?: boolean;
  isDefault?: boolean;
}

export const getConversations = (courseId?: number) =>
  get<AIConversation[]>('/ai/conversations', { courseId }, { silent: true });

export const getMessages = (conversationId: string) =>
  get<AIMessage[]>(`/ai/conversations/${conversationId}/messages`);

export const createConversation = (params: { courseId?: number; title?: string }) =>
  post<AIConversation>('/ai/conversations', params);

export const renameConversation = (id: string, title: string) =>
  put<void>(`/ai/conversations/${id}`, { title });

export const deleteConversation = (id: string) => del<void>(`/ai/conversations/${id}`);

export const generateConversationTitle = (id: string) =>
  post<string>(`/ai/conversations/${id}/generate-title`);

export const cancelChatStream = (streamId: string) => del<void>(`/ai/chat/stream/${streamId}`);

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
export const getChatModels = async (): Promise<ModelProviderConfig[]> => {
  try {
    const res = await get<ChatModelVO[]>('/ai/chat/models', undefined, { silent: true });
    const list = Array.isArray(res?.data) ? res.data : [];
    if (list.length > 0) {
      return mapChatModelVo(list);
    }
  } catch {
    // 后端未重启或接口暂不可用时继续降级
  }

  try {
    const configs = await getModelConfigs();
    const chatModels = configs.filter((m) => m.enabled && !m.supportsEmbedding);
    if (chatModels.length > 0) {
      return chatModels;
    }
  } catch {
    // ignore
  }

  return DEFAULT_CHAT_MODELS;
};

export const aiChatApi = {
  getConversations,
  getMessages,
  createConversation,
  renameConversation,
  deleteConversation
};
