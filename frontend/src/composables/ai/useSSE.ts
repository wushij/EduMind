import { SSEClient } from '@/core/sse/client';

export function useSSE() {
  const client = new SSEClient();
  return { client };
}
