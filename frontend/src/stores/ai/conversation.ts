import { defineStore } from 'pinia';
import { ref } from 'vue';
import { AIConversation } from '@/types/ai/conversation';

export const useConversationStore = defineStore('ai-conversation', () => {
  const currentConversationId = ref<string | null>(null);
  const conversations = ref<AIConversation[]>([]);

  const setCurrentSession = (id: string) => {
    currentConversationId.value = id;
  };

  const setConversations = (list: AIConversation[]) => {
    conversations.value = list;
  };

  return {
    currentConversationId,
    conversations,
    setCurrentSession,
    setConversations
  };
});
