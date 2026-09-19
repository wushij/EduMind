import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import type { GlobalAssistantAskResponse, GlobalAssistantChatRequest } from '@/types/ai/assistant';

export function askGlobalAssistant(data: GlobalAssistantChatRequest, config?: HttpRequestConfig) {
  return post<GlobalAssistantAskResponse>(
    '/ai/assistant/ask',
    {
      message: data.message,
      input: data.message,
      courseId: data.courseId,
      conversationId: data.conversationId
    },
    config
  );
}
