import type { SSEClient } from '@/core/sse/client';
import { API_BASE_URL } from '@/config';
import { splitCopilotStream } from '@/utils/ai/copilot-stream-split';
import type { CitationItem } from '@/types/ai/assistant';

export type CopilotStreamProgress = {
  receivedContent: boolean;
  lastStreamedIndex: number;
};

export type CopilotSseHandlers = {
  isStopped?: () => boolean;
  onIntent?: (data: unknown) => void;
  onStatus?: (message: string, phase?: string) => void;
  onReasoningChunk?: (chunk: string) => void;
  onDeltaChunk?: (chunk: string, answerDelta: string) => void;
  onCitations?: (citations: CitationItem[]) => void;
  onMemory?: (memories: Array<{ id: number; summary: string; memoryType?: string }>) => void;
  onDone?: (data: unknown) => void;
  onFollowOutput?: () => void;
  defaultErrorMessage?: string;
};

function extractDeltaChunk(data: unknown): string {
  return String(
    (data as { content?: string; text?: string })?.content ||
      (data as { text?: string })?.text ||
      data ||
      ''
  );
}

function extractCitations(data: unknown): CitationItem[] {
  const cits =
    (Array.isArray(data) ? data : (data as { citations?: CitationItem[] })?.citations) || [];
  return Array.isArray(cits) ? (cits as CitationItem[]) : [];
}

/** Shared SSE event dispatcher for course chat and global assistant copilot streams. */
export function handleCopilotSseEvent(
  event: string,
  data: unknown,
  streamingContentRef: { value: string },
  progress: CopilotStreamProgress,
  handlers: CopilotSseHandlers
): void {
  if (handlers.isStopped?.()) return;

  if (event === 'intent') {
    handlers.onIntent?.(data);
  } else if (event === 'status') {
    const s = data as { phase?: string; message?: string };
    if (s?.message) handlers.onStatus?.(s.message, s?.phase);
  } else if (event === 'reasoning') {
    const chunk = String((data as { content?: string })?.content || data || '');
    if (chunk) handlers.onReasoningChunk?.(chunk);
  } else if (event === 'delta') {
    const chunk = extractDeltaChunk(data);
    if (chunk) {
      progress.receivedContent = true;
      streamingContentRef.value += chunk;
      const split = splitCopilotStream(streamingContentRef.value);
      if (split.answer) {
        const answerDelta = split.answer.slice(progress.lastStreamedIndex);
        progress.lastStreamedIndex = split.answer.length;
        handlers.onDeltaChunk?.(chunk, answerDelta);
      }
    }
  } else if (event === 'citation' || event === 'citations') {
    const cits = extractCitations(data);
    if (cits.length > 0) handlers.onCitations?.(cits);
  } else if (event === 'memory') {
    const mems = (data as { memories?: Array<{ id: number; summary: string; memoryType?: string }> })?.memories || [];
    if (Array.isArray(mems) && mems.length > 0) {
      handlers.onMemory?.(mems);
    }
  } else if (event === 'done') {
    handlers.onDone?.(data);
  } else if (event === 'error') {
    throw new Error(
      String((data as { message?: string })?.message || handlers.defaultErrorMessage || '流式输出服务异常')
    );
  }

  handlers.onFollowOutput?.();
}

export async function runCopilotSseStream(
  sseClient: SSEClient,
  url: string,
  body: Record<string, unknown>,
  streamingContentRef: { value: string },
  handlers: CopilotSseHandlers
): Promise<boolean> {
  const progress: CopilotStreamProgress = { receivedContent: false, lastStreamedIndex: 0 };

  await sseClient.streamEvents(url, body, (event, data) => {
    handleCopilotSseEvent(event, data, streamingContentRef, progress, handlers);
  });

  return progress.receivedContent;
}

export async function streamGlobalAssistantChat(
  sseClient: SSEClient,
  query: string,
  courseId: number | undefined,
  conversationId: string | undefined,
  streamingContentRef: { value: string },
  handlers: CopilotSseHandlers
): Promise<boolean> {
  return runCopilotSseStream(
    sseClient,
    `${API_BASE_URL}/ai/assistant/chat`,
    {
      message: query,
      courseId,
      conversationId
    },
    streamingContentRef,
    {
      defaultErrorMessage: '流式响应异常',
      ...handlers
    }
  );
}
