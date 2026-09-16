import { ElMessage } from 'element-plus';
import type { Ref } from 'vue';
import {
  type ChatMessage,
  type ChatSession,
  type LoadSessionsOptions,
  isDefaultSessionTitle,
  persistMessageCache,
  readMessageCache,
  clearMessageCache,
  mergeServerWithLocalDrafts,
  getStoredSessionId,
  storeSessionId,
  clearStoredSessionId,
  buildSessionTitleFromPrompt,
  generateSmartFollowUps,
  fetchConversationList,
  fetchMessageList,
  createRemoteSession,
  renameRemoteSession,
  deleteRemoteSession,
  syncSessionTitleIfDefault
} from '@/services/ai/stream-service';

export type AIStreamSessionsDeps = {
  sessions: Ref<ChatSession[]>;
  currentSessionId: Ref<string>;
  messages: Ref<ChatMessage[]>;
  followUpPrompts: Ref<string[]>;
  streaming: Ref<boolean>;
  resetStreamingState: () => void;
  scrollToBottomInstant: () => void;
};

export function createAIStreamSessionActions(deps: AIStreamSessionsDeps) {
  const {
    sessions,
    currentSessionId,
    messages,
    followUpPrompts,
    streaming,
    resetStreamingState,
    scrollToBottomInstant
  } = deps;

  function findFirstUserPrompt(source: ChatMessage[] = messages.value): string {
    return source.find((m) => m.role === 'user' && m.content?.trim())?.content?.trim() || '';
  }

  async function syncSessionTitleIfDefaultLocal(
    conversationId: string,
    promptHint?: string,
    options?: { tryLlmTitle?: boolean }
  ): Promise<void> {
    await syncSessionTitleIfDefault(sessions.value, conversationId, messages.value, promptHint, options);
  }

  async function refreshSessionsMeta(courseId?: number) {
    const preservedTitles = new Map(
      sessions.value
        .filter((s) => !isDefaultSessionTitle(s.title))
        .map((s) => [s.id, s.title] as const)
    );
    try {
      sessions.value = (await fetchConversationList(courseId)).map((mapped) => {
        const preserved = preservedTitles.get(mapped.id);
        if (preserved && isDefaultSessionTitle(mapped.title)) {
          mapped.title = preserved;
        }
        return mapped;
      });
      if (
        currentSessionId.value &&
        !sessions.value.some((s) => s.id === currentSessionId.value)
      ) {
        const firstUser = messages.value.find((m) => m.role === 'user');
        sessions.value.unshift({
          id: currentSessionId.value,
          title: firstUser?.content
            ? buildSessionTitleFromPrompt(firstUser.content)
            : '新问答会话',
          updatedAt: '刚刚'
        });
      }
    } catch {
      // 保留本地会话列表，避免手动停止后左侧列表闪空
    }
  }

  function restoreFollowUpsForLastTurn() {
    if (messages.value.length === 0) {
      followUpPrompts.value = [];
      return;
    }
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg && lastMsg.role === 'assistant') {
      if (lastMsg.followUpPrompts && lastMsg.followUpPrompts.length > 0) {
        followUpPrompts.value = [...lastMsg.followUpPrompts];
        return;
      }
      const lastUserMsg = [...messages.value].reverse().find((m) => m.role === 'user');
      if (lastUserMsg?.content) {
        const prompts = generateSmartFollowUps(lastUserMsg.content);
        followUpPrompts.value = prompts;
        lastMsg.followUpPrompts = prompts;
      }
    } else {
      followUpPrompts.value = [];
    }
  }

  async function loadMessages(conversationId: string, courseId?: number) {
    // 仅以 sessionStorage 草稿合并（含手动停止生成）；勿用内存 messages，避免切会话/刷新时串会话导致重复气泡
    const localDraft = readMessageCache(courseId, conversationId);
    try {
      const serverMsgs = await fetchMessageList(conversationId);
      messages.value = mergeServerWithLocalDrafts(serverMsgs, localDraft);
      persistMessageCache(courseId, conversationId, messages.value);
      await syncSessionTitleIfDefaultLocal(conversationId);
      restoreFollowUpsForLastTurn();
    } catch {
      if (localDraft.length > 0) {
        messages.value = localDraft;
      } else {
        messages.value = [];
        followUpPrompts.value = [];
      }
    }
  }

  async function loadSessions(courseId?: number, options?: LoadSessionsOptions) {
    const restoreLastSession = options?.restoreLastSession !== false;
    const preservedTitles = new Map(
      sessions.value
        .filter((s) => !isDefaultSessionTitle(s.title))
        .map((s) => [s.id, s.title] as const)
    );
    try {
      sessions.value = (await fetchConversationList(courseId)).map((mapped) => {
        const preserved = preservedTitles.get(mapped.id);
        if (preserved && isDefaultSessionTitle(mapped.title)) {
          mapped.title = preserved;
        }
        return mapped;
      });

      if (!restoreLastSession) {
        currentSessionId.value = '';
        messages.value = [];
        followUpPrompts.value = [];
        return;
      }

      const storedId = getStoredSessionId(courseId);
      const preferredId =
        (storedId && sessions.value.some((s) => s.id === storedId) ? storedId : '') ||
        (currentSessionId.value && sessions.value.some((s) => s.id === currentSessionId.value)
          ? currentSessionId.value
          : '') ||
        sessions.value[0]?.id ||
        '';

      if (preferredId) {
        currentSessionId.value = preferredId;
        storeSessionId(courseId, preferredId);
        if (!options?.skipMessageReload) {
          await loadMessages(preferredId, courseId);
        }
      } else if (!options?.skipMessageReload) {
        currentSessionId.value = '';
        messages.value = [];
      }
    } catch {
      if (sessions.value.length === 0) {
        currentSessionId.value = '';
        messages.value = [];
      }
    }
  }

  async function createNewSession(courseId?: number, force = false) {
    if (!force && messages.value.length === 0 && !streaming.value) {
      return;
    }
    try {
      const session = await createRemoteSession(courseId);
      sessions.value.unshift(session);
      currentSessionId.value = session.id;
      storeSessionId(courseId, session.id);
      messages.value = [];
      followUpPrompts.value = [];
    } catch {
      ElMessage.error('创建会话失败，请确认已登录且网络正常');
      throw new Error('create conversation failed');
    }
  }

  async function startNewChat(courseId?: number) {
    await createNewSession(courseId, true);
  }

  async function switchSession(id: string, courseId?: number) {
    if (streaming.value) {
      ElMessage.warning('当前正在生成中，请先停止生成');
      return;
    }
    currentSessionId.value = id;
    storeSessionId(courseId, id);
    followUpPrompts.value = [];
    resetStreamingState();
    await loadMessages(id, courseId);
    scrollToBottomInstant();
  }

  async function removeSession(id: string, courseId?: number) {
    try {
      await deleteRemoteSession(id);
    } catch {
      // ignore
    }
    sessions.value = sessions.value.filter((s) => s.id !== id);
    clearStoredSessionId(courseId, id);
    clearMessageCache(courseId, id);
    if (currentSessionId.value === id) {
      currentSessionId.value = sessions.value[0]?.id || '';
      if (currentSessionId.value) {
        storeSessionId(courseId, currentSessionId.value);
        await loadMessages(currentSessionId.value, courseId);
      } else {
        messages.value = [];
      }
    }
  }

  async function renameSession(id: string, title: string) {
    try {
      await renameRemoteSession(id, title);
      const s = sessions.value.find((x) => x.id === id);
      if (s) s.title = title;
    } catch {
      const s = sessions.value.find((x) => x.id === id);
      if (s) s.title = title;
    }
  }

  return {
    findFirstUserPrompt,
    syncSessionTitleIfDefaultLocal,
    refreshSessionsMeta,
    restoreFollowUpsForLastTurn,
    loadMessages,
    loadSessions,
    createNewSession,
    startNewChat,
    switchSession,
    removeSession,
    renameSession
  };
}
