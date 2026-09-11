import { ref } from 'vue';
import { AIMessage } from '@/types/ai/message';

export function useAIChat() {
  const messages = ref<AIMessage[]>([]);
  return { messages };
}
