import type { CitationItem } from '@/types/ai/assistant';

export interface SSEMessageEvent {
  type: 'delta' | 'citation' | 'done' | 'error';
  text?: string;
  citations?: CitationItem[];
  messageId?: number | string;
  finishReason?: string;
  error?: string;
}

export class RAGPipelineFeature {
  /**
   * 解析 SSE 数据块字符串
   */
  public static parseSSEChunk(chunkText: string, onEvent: (ev: SSEMessageEvent) => void): void {
    const lines = chunkText.split('\n');
    let currentEvent = 'delta';

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;

      if (trimmed.startsWith('event:')) {
        currentEvent = trimmed.replace('event:', '').trim();
        continue;
      }

      if (trimmed.startsWith('data:')) {
        const dataStr = trimmed.replace('data:', '').trim();
        try {
          const parsed = JSON.parse(dataStr);
          if (currentEvent === 'citation' || parsed.citations) {
            onEvent({ type: 'citation', citations: parsed.citations || [parsed] });
          } else if (currentEvent === 'done' || parsed.finish_reason || parsed.finishReason) {
            onEvent({ type: 'done', messageId: parsed.messageId, finishReason: parsed.finish_reason || parsed.finishReason });
          } else if (currentEvent === 'error' || parsed.error) {
            onEvent({ type: 'error', error: parsed.error || parsed.message });
          } else if (currentEvent === 'delta' || currentEvent === 'message') {
            onEvent({ type: 'delta', text: parsed.text || parsed.content || '' });
          } else {
            onEvent({ type: 'delta', text: parsed.text || parsed.content || '' });
          }
        } catch {
          // 纯文本 data 兜底
          if (currentEvent === 'delta') {
            onEvent({ type: 'delta', text: dataStr });
          }
        }
      }
    }
  }
}
