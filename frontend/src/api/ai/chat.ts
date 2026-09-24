import { post, get, put, del } from '@/core/http/request';
import { AIConversation } from '@/types/ai/conversation';
import { AIMessage } from '@/types/ai/message';

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

export const deleteMessage = (messageId: string) =>
  del<{ deletedIds: string[] }>(`/ai/conversations/messages/${messageId}`);

export const generateConversationTitle = (id: string) =>
  post<string>(`/ai/conversations/${id}/generate-title`, undefined, { silent: true, timeout: 6000 });

export const cancelChatStream = (streamId: string) => del<void>(`/ai/chat/stream/${streamId}`);

export const getChatModelsRaw = () =>
  get<ChatModelVO[]>('/ai/chat/models', undefined, { silent: true });

import axiosInstance from '@/core/http/axios';

export interface ChatAttachmentVO {
  attachmentId: string;
  fileName: string;
  fileSizeText: string;
  fileSizeBytes: number;
  fileType: string;
  previewText: string;
  charCount: number;
  parseStatus: string;
  errorMessage?: string;
}

export async function uploadChatAttachment(file: File): Promise<ChatAttachmentVO> {
  const formData = new FormData();
  formData.append('file', file);
  const res = await axiosInstance.post<{ code: number; data: ChatAttachmentVO; message?: string }>(
    '/ai/chat/attachment',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  );
  return res.data.data;
}

export const aiChatApi = {
  getConversations,
  getMessages,
  createConversation,
  renameConversation,
  deleteConversation,
  uploadChatAttachment
};

export type { ChatModelVO };
