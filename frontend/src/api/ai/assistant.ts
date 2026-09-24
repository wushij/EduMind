import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import type {
  FollowUpSuggestRequest,
  FollowUpSuggestResponse,
  GlobalAssistantAskResponse,
  GlobalAssistantChatRequest
} from '@/types/ai/assistant';

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

/**
 * 生成「下一步追问」。
 *
 * 该接口独立于聊天主流程：回答先落地展示，追问随后异步补齐；
 * 调用方需以 silent 方式调用，失败时回落到本地规则生成，不要让用户看到错误提示。
 */
export function suggestFollowUps(data: FollowUpSuggestRequest, config?: HttpRequestConfig) {
  return post<FollowUpSuggestResponse>('/ai/assistant/follow-ups', data, config);
}
