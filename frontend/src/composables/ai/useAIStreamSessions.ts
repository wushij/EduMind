import { ElMessage } from 'element-plus';
import type { Ref } from 'vue';
import { pickTurnMessage } from '@/utils/ai/follow-up-message';
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
  requestFollowUps,
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

/** 新建会话的结果：reused 为 true 表示复用了已存在的空白会话，并未真正新建 */
export interface NewSessionResult {
  sessionId: string;
  reused: boolean;
}

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
    // 传 getter：会话列表刷新时会被整体替换，用闭包读当前数组，标题才写得回正在渲染的那一份
    await syncSessionTitleIfDefault(
      () => sessions.value,
      conversationId,
      messages.value,
      promptHint,
      options
    );
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

  function restoreFollowUpsForLastTurn(courseId?: number) {
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
        const targetId = lastMsg.id;
        const prompts = requestFollowUps(lastUserMsg.content, lastMsg.content, {
          courseId,
          onUpdate: (next) => {
            // 按 id 判定「仍然是当轮消息」，并通过数组里的代理对象回写（引用比较在响应式代理下不可靠）
            const current = pickTurnMessage(messages.value, targetId);
            if (!current) return;
            current.followUpPrompts = next;
            followUpPrompts.value = next;
          }
        });
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
      restoreFollowUpsForLastTurn(courseId);
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

      // 静默收敛历史里重复的空白会话（不阻塞首屏渲染）
      void pruneDuplicateEmptySessions(courseId);

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
          scrollToBottomInstant();
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

  /** 把目标空白会话置为当前会话（仅本地切换，不重复拉取消息） */
  function activateSession(session: ChatSession, courseId?: number) {
    sessions.value = [session, ...sessions.value.filter((s) => s.id !== session.id)];
    currentSessionId.value = session.id;
    storeSessionId(courseId, session.id);
    messages.value = [];
    followUpPrompts.value = [];
    resetStreamingState();
  }

  /**
   * 查找可复用的空白会话：
   * 1. 当前会话本身就没有任何问答记录时，直接复用当前会话；
   * 2. 否则回溯历史列表，找出「默认标题 + 服务端确认零消息」的空白会话。
   * 目的：用户反复点击「新建问答会话」时不再无限生成空会话。
   */
  async function findReusableEmptySession(courseId?: number): Promise<ChatSession | null> {
    const current = sessions.value.find((s) => s.id === currentSessionId.value);
    if (current && messages.value.length === 0) {
      return current;
    }

    const candidates = sessions.value.filter(
      (s) => s.id !== currentSessionId.value && isDefaultSessionTitle(s.title)
    );

    for (const candidate of candidates) {
      // 本地草稿非空说明该会话已有未落库内容（如手动停止生成），不可复用
      if (readMessageCache(courseId, candidate.id).length > 0) continue;
      try {
        const serverMsgs = await fetchMessageList(candidate.id);
        if (serverMsgs.length === 0) return candidate;
      } catch {
        // 单个会话校验失败（网络抖动 / 权限）时跳过，不影响其余候选
      }
    }
    return null;
  }

  async function createNewSession(
    courseId?: number,
    force = false
  ): Promise<NewSessionResult | null> {
    if (!force && messages.value.length === 0 && !streaming.value) {
      return currentSessionId.value
        ? { sessionId: currentSessionId.value, reused: true }
        : null;
    }

    // 关键防线：已存在空白会话时不再新建，直接复用，避免历史列表被空会话刷屏
    const reusable = await findReusableEmptySession(courseId);
    if (reusable) {
      activateSession(reusable, courseId);
      return { sessionId: reusable.id, reused: true };
    }

    try {
      const session = await createRemoteSession(courseId);
      sessions.value.unshift(session);
      currentSessionId.value = session.id;
      storeSessionId(courseId, session.id);
      messages.value = [];
      followUpPrompts.value = [];
      return { sessionId: session.id, reused: false };
    } catch {
      ElMessage.error('创建会话失败，请确认已登录且网络正常');
      throw new Error('create conversation failed');
    }
  }

  async function startNewChat(courseId?: number): Promise<NewSessionResult | null> {
    return createNewSession(courseId, true);
  }

  /**
   * 收敛历史里重复的空白会话：仅保留一条，其余「服务端确认零消息」的空白会话静默删除。
   * 早期版本反复点「新建」会残留多条「新问答会话」，此处在列表加载后异步清理。
   */
  async function pruneDuplicateEmptySessions(courseId?: number) {
    const candidates = sessions.value.filter((s) => isDefaultSessionTitle(s.title));
    if (candidates.length <= 1) return;

    const confirmedEmptyIds: string[] = [];
    for (const candidate of candidates) {
      if (readMessageCache(courseId, candidate.id).length > 0) continue;
      try {
        const serverMsgs = await fetchMessageList(candidate.id);
        if (serverMsgs.length === 0) confirmedEmptyIds.push(candidate.id);
      } catch {
        // 校验失败时保留该会话，宁可留一条也不误删
      }
    }
    if (confirmedEmptyIds.length <= 1) return;

    const keepId = confirmedEmptyIds.includes(currentSessionId.value)
      ? currentSessionId.value
      : confirmedEmptyIds[0];
    const removeIds = confirmedEmptyIds.filter((id) => id !== keepId);

    await Promise.all(removeIds.map((id) => deleteRemoteSession(id).catch(() => undefined)));
    removeIds.forEach((id) => {
      clearMessageCache(courseId, id);
      clearStoredSessionId(courseId, id);
    });
    sessions.value = sessions.value.filter((s) => !removeIds.includes(s.id));

    // 极端情况下当前会话恰好是被清理的重复空白会话，回落到保留下来的那一条
    if (removeIds.includes(currentSessionId.value)) {
      currentSessionId.value = keepId;
      storeSessionId(courseId, keepId);
    }
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
        scrollToBottomInstant();
      } else {
        messages.value = [];
      }
    }
  }

  async function clearAllSessions(courseId?: number) {
    if (sessions.value.length === 0) return;

    const ids = sessions.value.map((s) => s.id);
    await Promise.all(ids.map((id) => deleteRemoteSession(id).catch(() => undefined)));
    ids.forEach((id) => clearMessageCache(courseId, id));
    if (courseId) {
      clearStoredSessionId(courseId, getStoredSessionId(courseId));
    }
    sessions.value = [];
    currentSessionId.value = '';
    messages.value = [];
    followUpPrompts.value = [];
    resetStreamingState();
    ElMessage.success(ids.length > 0 ? `已清空 ${ids.length} 条历史会话` : '暂无历史会话可清空');
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
    clearAllSessions,
    renameSession
  };
}
