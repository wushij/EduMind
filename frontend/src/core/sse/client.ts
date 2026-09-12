import { storage } from '@/core/storage/local';
import { getTimestamp, generateNonce } from '@/utils/crypto';
import { TOKEN_KEY } from '@/constants/auth';

export type SseEventHandler = (event: string, data: Record<string, unknown>) => void;

export class SSEClient {
  private controller: AbortController | null = null;

  /**
   * 解析 SSE 事件流（支持 delta / citation / done / error）
   */
  async streamEvents(
    url: string,
    payload: Record<string, unknown>,
    onEvent: SseEventHandler,
    onComplete?: () => void,
    onError?: (err: unknown) => void
  ) {
    this.controller = new AbortController();
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'X-Timestamp': String(getTimestamp()),
        'X-Nonce': generateNonce()
      };

      const token = storage.get(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY);
      if (token) {
        headers['satoken'] = token;
      }

      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: this.controller.signal
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      if (!response.body) {
        throw new Error('ReadableStream not supported');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const segments = buffer.split('\n\n');
        buffer = segments.pop() || '';
        for (const segment of segments) {
          this.parseSegment(segment, onEvent);
        }
      }

      if (buffer.trim()) {
        this.parseSegment(buffer, onEvent);
      }

      onComplete?.();
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'AbortError') {
        onComplete?.();
      } else {
        onError?.(err);
      }
    } finally {
      this.controller = null;
    }
  }

  /** @deprecated 使用 streamEvents 解析结构化 SSE */
  async stream(
    url: string,
    payload: Record<string, unknown>,
    onChunk: (text: string) => void,
    onDone?: () => void,
    onError?: (err: unknown) => void
  ) {
    await this.streamEvents(
      url,
      payload,
      (event, data) => {
        if (event === 'delta' || event === 'message') {
          const content = String(data.content || data.text || '');
          if (content) onChunk(content);
        }
      },
      onDone,
      onError
    );
  }

  private parseSegment(segment: string, onEvent: SseEventHandler) {
    let eventName = 'delta';
    const dataLines: string[] = [];

    for (const line of segment.split('\n')) {
      const trimmed = line.trim();
      if (!trimmed) continue;
      if (trimmed.startsWith('event:')) {
        eventName = trimmed.slice(6).trim();
      } else if (trimmed.startsWith('data:')) {
        dataLines.push(trimmed.slice(5).trim());
      }
    }

    if (dataLines.length === 0) return;

    const raw = dataLines.join('\n');
    try {
      onEvent(eventName, JSON.parse(raw) as Record<string, unknown>);
    } catch {
      onEvent(eventName, { content: raw });
    }
  }

  stop() {
    if (this.controller) {
      this.controller.abort();
      this.controller = null;
    }
  }
}
