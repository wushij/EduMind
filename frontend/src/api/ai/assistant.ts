import { post } from '@/core/http/request';
import type { GlobalAssistantAskResponse, GlobalAssistantChatRequest } from '@/types/ai/assistant';

export function askGlobalAssistant(data: GlobalAssistantChatRequest) {
  return post<GlobalAssistantAskResponse>('/ai/assistant/ask', {
    message: data.message,
    input: data.message,
    courseId: data.courseId,
    conversationId: data.conversationId
  });
}
