import { storage } from '@/core/storage/local';
import { getTimestamp, generateNonce } from '@/utils/crypto';
import { TOKEN_KEY } from '@/constants/auth';

export class SSEClient {
  private controller: AbortController | null = null;

  async stream(
    url: string,
    payload: any,
    onChunk: (text: string) => void,
    onDone?: () => void,
    onError?: (err: any) => void
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

      if (!response.body) throw new Error('ReadableStream not supported');
      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const text = decoder.decode(value, { stream: true });
        onChunk(text);
      }
      onDone?.();
    } catch (err: any) {
      if (err.name === 'AbortError') {
        console.log('Stream stopped by user');
        onDone?.();
      } else {
        onError?.(err);
      }
    } finally {
      this.controller = null;
    }
  }

  stop() {
    if (this.controller) {
      this.controller.abort();
      this.controller = null;
    }
  }
}

