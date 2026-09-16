import { describe, it, expect, vi } from 'vitest';
import {
  handleCopilotSseEvent,
  runCopilotSseStream,
  streamGlobalAssistantChat
} from '@/services/ai/copilot-sse-stream';
import type { SSEClient } from '@/core/sse/client';

describe('handleCopilotSseEvent', () => {
  it('accumulates delta into answer slices', () => {
    const content = { value: '' };
    const progress = { receivedContent: false, lastStreamedIndex: 0 };
    const deltas: string[] = [];

    handleCopilotSseEvent('delta', { content: 'hello' }, content, progress, {
      onDeltaChunk: (_chunk, answerDelta) => {
        if (answerDelta) deltas.push(answerDelta);
      }
    });

    expect(progress.receivedContent).toBe(true);
    expect(content.value).toBe('hello');
    expect(deltas).toEqual(['hello']);
  });

  it('dispatches intent and citations', () => {
    const onIntent = vi.fn();
    const onCitations = vi.fn();
    const content = { value: '' };
    const progress = { receivedContent: false, lastStreamedIndex: 0 };

    handleCopilotSseEvent('intent', { route: 'rag' }, content, progress, { onIntent });
    handleCopilotSseEvent('citations', [{ id: '1' }], content, progress, { onCitations });

    expect(onIntent).toHaveBeenCalledWith({ route: 'rag' });
    expect(onCitations).toHaveBeenCalledWith([{ id: '1' }]);
  });

  it('short-circuits when stopped', () => {
    const onStatus = vi.fn();
    const content = { value: '' };
    const progress = { receivedContent: false, lastStreamedIndex: 0 };

    handleCopilotSseEvent(
      'status',
      { message: 'thinking' },
      content,
      progress,
      { isStopped: () => true, onStatus }
    );

    expect(onStatus).not.toHaveBeenCalled();
  });
});

describe('runCopilotSseStream', () => {
  it('forwards events through SSE client', async () => {
    const content = { value: '' };
    const onDone = vi.fn();
    const sseClient = {
      streamEvents: vi.fn(async (_url, _body, onEvent) => {
        onEvent('delta', { content: 'ok' });
        onEvent('done', { conversationId: 'c1' });
      }),
      stop: vi.fn()
    } as unknown as SSEClient;

    const received = await runCopilotSseStream(
      sseClient,
      'http://test/stream',
      { message: 'hi' },
      content,
      { onDone }
    );

    expect(received).toBe(true);
    expect(onDone).toHaveBeenCalledWith({ conversationId: 'c1' });
    expect(sseClient.streamEvents).toHaveBeenCalled();
  });
});

describe('streamGlobalAssistantChat', () => {
  it('uses assistant chat endpoint', async () => {
    const sseClient = {
      streamEvents: vi.fn(async () => {}),
      stop: vi.fn()
    } as unknown as SSEClient;

    await streamGlobalAssistantChat(sseClient, 'q', 101, 'conv-1', { value: '' }, {});

    expect(sseClient.streamEvents).toHaveBeenCalledWith(
      expect.stringContaining('/ai/assistant/chat'),
      { message: 'q', courseId: 101, conversationId: 'conv-1' },
      expect.any(Function)
    );
  });
});
