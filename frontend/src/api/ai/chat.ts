import { post, get, put, del } from '@/core/http/request';
import { AIConversation } from '@/types/ai/conversation';
import { AIMessage } from '@/types/ai/message';

export const getConversations = (courseId?: number) =>
  get<AIConversation[]>('/ai/conversations', { courseId });

export const getMessages = (conversationId: string) =>
  get<AIMessage[]>(`/ai/conversations/${conversationId}/messages`);

export const createConversation = (params: { courseId?: number; title?: string }) =>
  post<AIConversation>('/ai/conversations', params);

export const renameConversation = (id: string, title: string) =>
  put<void>(`/ai/conversations/${id}`, { title });

export const deleteConversation = (id: string) => del<void>(`/ai/conversations/${id}`);

export const aiChatApi = {
  getConversations,
  getMessages,
  createConversation,
  renameConversation,
  deleteConversation
};
