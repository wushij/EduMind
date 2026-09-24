import { nextTick, ref, type ComputedRef, type Ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SSEClient } from '@/core/sse/client';
import {
  collectPairedMessageIds,
  isPersistedMessageId,
  removeMessagesByIds
} from '@/utils/ai/chat-message-pair';
import {
  cleanReasoningText,
  buildStoppedGenerationContent
} from '@/utils/ai/copilot-stream-split';
import { bindMarkdownCodeCopy, renderMermaidInElement } from '@/utils/ai/chat-markdown';
import type { CitationItem } from '@/types/ai/assistant';
import { getDefaultReasoningFolded } from '@/utils/ai/thinking-display';
import { pickTurnMessage } from '@/utils/ai/follow-up-message';
import {
  type ChatMessage,
  type ChatSession,
  type StreamOptions,
  isDefaultSessionTitle,
  persistMessageCache,
  storeSessionId,
  buildSessionTitleFromPrompt,
  requestFollowUps,
  deleteRemoteMessage,
  renameRemoteSession,
  streamAssistantChat,
  fallbackAskAssistant
} from '@/services/ai/stream-service';
import { cancelChatStream } from '@/api/ai/chat';

export type AIStreamOperationsDeps = {
  sseClient: SSEClient;
  streaming: Ref<boolean>;
  sessions: Ref<ChatSession[]>;
  currentSessionId: Ref<string>;
  messages: Ref<ChatMessage[]>;
  messagesScrollRef: Ref<HTMLDivElement | null>;
  streamingReasoning: Ref<string>;
  streamingContent: Ref<string>;
  streamingCitations: Ref<CitationItem[]>;
  isReasoningFolded: Ref<boolean>;
  isReasoningActive: Ref<boolean>;
  streamPhaseMessage: Ref<string>;
  answerStreamStarted: Ref<boolean>;
  followUpPrompts: Ref<string[]>;
  userStoppedGeneration: Ref<boolean>;
  activeStreamCourseId: Ref<number | undefined>;
  streamingThinkingBody: ComputedRef<string>;
  streamingAnswerBody: ComputedRef<string>;
  appendStreamingMarkdown: (chunk: string) => void;
  finishStreamingMarkdown: () => void;
  resetStreamingState: () => void;
  scrollToBottomInstant: () => void;
  scheduleFollowStreamOutput: () => void;
  syncSessionTitleIfDefaultLocal: (
    conversationId: string,
    promptHint?: string,
    options?: { tryLlmTitle?: boolean }
  ) => Promise<void>;
  refreshSessionsMeta: (courseId?: number) => Promise<void>;
  loadSessions: (courseId?: number, options?: import('@/services/ai/stream-service').LoadSessionsOptions) => Promise<void>;
  createNewSession: (
    courseId?: number,
    force?: boolean
  ) => Promise<import('@/composables/ai/useAIStreamSessions').NewSessionResult | null>;
};

export function createAIStreamOperations(deps: AIStreamOperationsDeps) {
  const getInitialReasoningFolded = getDefaultReasoningFolded;
  const currentStreamingMemories = ref<Array<{ id: number; summary: string; memoryType?: string }>>([]);
  let activeChatStreamId = '';
  let currentTurnSettled = false;

  function clearActiveChatStreamId() {
    activeChatStreamId = '';
  }

  async function notifyBackendStreamCancel() {
    const id = activeChatStreamId;
    clearActiveChatStreamId();
    if (!id) return;
    try {
      await cancelChatStream(id);
    } catch {
      // 静默：SSE 断开 + 后端 TTL 仍可兜底
    }
  }

  function finalizeAssistantMessage(
    promptText: string,
    serverIds?: { userMessageId?: string; messageId?: string }
  ) {
    deps.finishStreamingMarkdown();
    const finalReasoning = cleanReasoningText(
      deps.streamingReasoning.value || deps.streamingThinkingBody.value
    );
    const finalAnswer = deps.streamingAnswerBody.value || deps.streamingContent.value;

    if (serverIds?.userMessageId) {
      for (let i = deps.messages.value.length - 1; i >= 0; i--) {
        if (deps.messages.value[i]?.role === 'user') {
          deps.messages.value[i].id = serverIds.userMessageId;
          break;
        }
      }
    }

    const assistantMessage: ChatMessage = {
      id: serverIds?.messageId || `ai_${Date.now()}`,
      role: 'assistant',
      content: finalAnswer.trim() || '已处理你的课程学习咨询。',
      reasoningContent: finalReasoning,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      citations: [...deps.streamingCitations.value],
      recalledMemories: [...currentStreamingMemories.value],
      followUpPrompts: []
    };

    // 追问：模型结果异步到达后才回写（拿不到就静默回落到规则生成）
    const followUps = requestFollowUps(promptText, finalAnswer, {
      onUpdate: (prompts) => {
        // 用 pickTurnMessage 按 id 拿「数组里的代理对象」回写：
        // 直接与原始对象做引用比较会恒为 false（响应式代理），表现就是「追问要刷新才出现」
        const current = pickTurnMessage(deps.messages.value, assistantMessage.id);
        if (!current) return;
        current.followUpPrompts = prompts;
        deps.followUpPrompts.value = prompts;
        persistMessageCache(
          deps.activeStreamCourseId.value,
          deps.currentSessionId.value,
          deps.messages.value
        );
      }
    });
    assistantMessage.followUpPrompts = followUps;
    deps.messages.value.push(assistantMessage);
    currentStreamingMemories.value = [];

    currentTurnSettled = true;
    deps.followUpPrompts.value = followUps;
    deps.resetStreamingState();
    // 关键修复：一旦回答结算入库，流式状态必须立即置为 false，消除正在进行的思考气泡
    deps.streaming.value = false;
    persistMessageCache(
      deps.activeStreamCourseId.value,
      deps.currentSessionId.value,
      deps.messages.value
    );
    window.dispatchEvent(new CustomEvent('edumind:ai-usage-changed'));
    deps.scrollToBottomInstant();
    nextTick(() => {
      if (deps.messagesScrollRef.value) {
        bindMarkdownCodeCopy(deps.messagesScrollRef.value, { renderMermaid: false });
        void renderMermaidInElement(deps.messagesScrollRef.value);
      }
    });
  }

  async function streamAssistantChatLocal(
    query: string,
    courseId: number,
    options?: StreamOptions
  ): Promise<boolean> {
    return streamAssistantChat(
      deps.sseClient,
      query,
      courseId,
      deps.currentSessionId.value,
      options,
      {
        isStopped: () => deps.userStoppedGeneration.value,
        onStreamId: (streamId) => {
          activeChatStreamId = streamId;
        },
        onStatus: (message, phase) => {
          deps.streamPhaseMessage.value = message;
          if (phase === 'reasoning') deps.isReasoningActive.value = true;
          if (phase === 'composing') deps.isReasoningActive.value = false;
        },
        onReasoningChunk: (chunk) => {
          deps.streamingReasoning.value += chunk;
          deps.isReasoningActive.value = true;
          deps.streamPhaseMessage.value = '';
        },
        onDeltaChunk: (_chunk, answerDelta) => {
          deps.isReasoningActive.value = false;
          deps.streamPhaseMessage.value = '';
          if (!deps.answerStreamStarted.value) {
            deps.answerStreamStarted.value = true;
            deps.isReasoningFolded.value = getInitialReasoningFolded();
          }
          if (answerDelta) {
            deps.appendStreamingMarkdown(answerDelta);
          }
        },
        onCitations: (citations) => {
          deps.streamingCitations.value = citations;
        },
        onMemory: (memories) => {
          currentStreamingMemories.value = memories;
        },
        onDone: (d) => {
          if (d.conversationId) {
            const convId = String(d.conversationId);
            if (convId !== deps.currentSessionId.value) {
              deps.currentSessionId.value = convId;
            }
            storeSessionId(courseId, convId);
            if (!deps.sessions.value.some((s) => s.id === convId)) {
              deps.sessions.value.unshift({
                id: convId,
                title: query.slice(0, 20).replace(/\n/g, ' ').trim() || '新问答会话',
                updatedAt: '刚刚'
              });
            }
          }
          if (d.citations && d.citations.length > 0) {
            deps.streamingCitations.value = d.citations;
          }
          if (d.reasoningContent && !deps.streamingReasoning.value) {
            deps.streamingReasoning.value = String(d.reasoningContent);
          }
          finalizeAssistantMessage(query, {
            userMessageId: d.userMessageId,
            messageId: d.messageId
          });
        },
        onFollowOutput: deps.scheduleFollowStreamOutput
      },
      deps.streamingContent
    );
  }

  async function fallbackAsk(query: string, courseId: number) {
    const data = await fallbackAskAssistant(query, courseId, deps.currentSessionId.value);
    if (data.reasoningContent) {
      deps.streamingReasoning.value = data.reasoningContent;
    }
    if (data.citations && Array.isArray(data.citations)) {
      deps.streamingCitations.value = data.citations;
    }
    deps.streamingContent.value = data.content;
    finalizeAssistantMessage(query);
  }

  async function sendMessage(promptText: string, courseId: number = 101, options?: StreamOptions) {
    const text = promptText.trim();
    if (!text || deps.streaming.value) return;

    deps.activeStreamCourseId.value = courseId;
    currentTurnSettled = false;

    if (!deps.currentSessionId.value) {
      await deps.createNewSession(courseId, true);
    }

    // 1. 若非重新生成，将用户提问上屏；若为重新生成，列表里已保留该轮用户的原有提问，避免重复追加相同提问气泡
    if (!options?.isRegenerate) {
      const userMsg: ChatMessage = {
        id: `user_${Date.now()}`,
        role: 'user',
        content: text,
        createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      deps.messages.value.push(userMsg);
    }

    // 2. 激活流式状态与阶段提示
    deps.userStoppedGeneration.value = false;
    deps.followUpPrompts.value = [];
    deps.resetStreamingState();
    deps.streamPhaseMessage.value = '正在检索课程知识切片并深度思考...';
    deps.streaming.value = true;
    deps.scrollToBottomInstant();

    // 乐观更新列表标题，避免等待接口期间仍显示「新问答会话」
    const curSess = deps.sessions.value.find((s) => s.id === deps.currentSessionId.value);
    if (curSess && isDefaultSessionTitle(curSess.title)) {
      const previewTitle = buildSessionTitleFromPrompt(text);
      if (!isDefaultSessionTitle(previewTitle)) {
        curSess.title = previewTitle;
        if (deps.currentSessionId.value) {
          void renameRemoteSession(deps.currentSessionId.value, previewTitle).catch(() => {});
        }
      }
    }

    try {
      const streamed = await streamAssistantChatLocal(text, courseId, options);
      if (deps.userStoppedGeneration.value || currentTurnSettled) return;
      if (!streamed && !deps.streamingContent.value) {
        try {
          await fallbackAsk(text, courseId);
        } catch {
          if (options?.isRegenerate) {
            throw new Error('重新生成未返回有效内容，请稍后重试');
          }
        }
      }
    } catch (err: unknown) {
      if (deps.userStoppedGeneration.value || currentTurnSettled) return;
      if (options?.isRegenerate) {
        const errorMsg = err instanceof Error ? err.message : '服务繁忙，请稍后重试';
        deps.messages.value.push({
          id: `ai_err_${Date.now()}`,
          role: 'assistant',
          content: `抱歉，重新生成失败：${errorMsg}`,
          createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        });
        deps.resetStreamingState();
      } else {
        try {
          await fallbackAsk(text, courseId);
        } catch (fallbackErr: unknown) {
          const errorMsg =
            fallbackErr instanceof Error
              ? fallbackErr.message
              : err instanceof Error
                ? err.message
                : '服务繁忙，请稍后重试';
          deps.messages.value.push({
            id: `ai_err_${Date.now()}`,
            role: 'assistant',
            content: `抱歉，知识库研读解析遇到异常：${errorMsg}`,
            createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          });
          deps.resetStreamingState();
        }
      }
    } finally {
      const wasStopped = deps.userStoppedGeneration.value;
      deps.userStoppedGeneration.value = false;
      deps.streaming.value = false;
      deps.scrollToBottomInstant();
      if (deps.currentSessionId.value) {
        storeSessionId(courseId, deps.currentSessionId.value);
        if (wasStopped) {
          persistMessageCache(courseId, deps.currentSessionId.value, deps.messages.value);
          try {
            await deps.refreshSessionsMeta(courseId);
          } catch {}
        } else {
          try {
            await deps.syncSessionTitleIfDefaultLocal(deps.currentSessionId.value, text, {
              tryLlmTitle: true
            });
          } catch {}
          try {
            await deps.refreshSessionsMeta(courseId);
          } catch {}
          persistMessageCache(courseId, deps.currentSessionId.value, deps.messages.value);
        }
      }
      nextTick(() => {
        if (deps.messagesScrollRef.value) {
          bindMarkdownCodeCopy(deps.messagesScrollRef.value);
        }
      });
    }
  }

  function stopStream() {
    if (!deps.streaming.value) return;
    deps.userStoppedGeneration.value = true;
    void notifyBackendStreamCancel();
    deps.sseClient.stop();
    deps.streaming.value = false;

    const answer = deps.streamingAnswerBody.value || deps.streamingContent.value;
    deps.finishStreamingMarkdown();
    deps.messages.value.push({
      id: `ai_${Date.now()}`,
      role: 'assistant',
      content: buildStoppedGenerationContent(answer),
      reasoningContent: cleanReasoningText(
        deps.streamingReasoning.value || deps.streamingThinkingBody.value
      ),
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      citations: [...deps.streamingCitations.value]
    });
    deps.resetStreamingState();
    persistMessageCache(
      deps.activeStreamCourseId.value,
      deps.currentSessionId.value,
      deps.messages.value
    );
    ElMessage.info('已停止生成');
    deps.scrollToBottomInstant();
  }

  function handleRegenerate(targetIdx: number, courseId: number = 101, options?: StreamOptions) {
    if (deps.streaming.value) return;
    let userPrompt = '';
    for (let i = targetIdx; i >= 0; i--) {
      if (deps.messages.value[i]?.role === 'user') {
        userPrompt = deps.messages.value[i].content;
        break;
      }
    }
    if (!userPrompt) return;
    // 移除 targetIdx 及其后续的消息，使得末尾保留该轮用户的原有提问
    deps.messages.value.splice(targetIdx);
    sendMessage(userPrompt, courseId, { ...options, isRegenerate: true });
  }

  async function confirmDeleteMessage(targetIdx: number) {
    try {
      await ElMessageBox.confirm(
        '将删除本条及其对应的一问一答，删除后无法恢复。确定继续吗？',
        '删除对话记录',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          lockScroll: false
        }
      );
    } catch {
      return;
    }

    const idsToDelete = collectPairedMessageIds(deps.messages.value, targetIdx);
    if (!idsToDelete.length) return;

    const persistedId = idsToDelete.find((id) => isPersistedMessageId(id));
    try {
      let deletedIds = idsToDelete;
      if (persistedId) {
        const res = await deleteRemoteMessage(persistedId);
        if (res?.data?.deletedIds?.length) {
          deletedIds = res.data.deletedIds;
        }
      }
      deps.messages.value = removeMessagesByIds(deps.messages.value, deletedIds);
      persistMessageCache(
        deps.activeStreamCourseId.value,
        deps.currentSessionId.value,
        deps.messages.value
      );
      deps.followUpPrompts.value = [];
      ElMessage.success('已删除本轮对话记录');
    } catch {
      if (!persistedId) {
        deps.messages.value = removeMessagesByIds(deps.messages.value, idsToDelete);
        deps.followUpPrompts.value = [];
        ElMessage.success('已删除本轮对话记录');
        return;
      }
      ElMessage.error('删除失败，请稍后重试');
    }
  }

  return {
    sendMessage,
    stopStream,
    handleRegenerate,
    confirmDeleteMessage
  };
}
