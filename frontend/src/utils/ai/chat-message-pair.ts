export interface ChatRoleMessage {
  id?: string | number;
  role: 'user' | 'assistant' | 'system';
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const TEMP_ID_PATTERN = /^(user_|ai_|msg_)\d+$/i;

/** 仅 MyBatis ASSIGN_UUID 落库消息才走后端删除 */
export function isPersistedMessageId(id: string | number | undefined): boolean {
  if (id == null || id === '') return false;
  const text = String(id).trim();
  if (TEMP_ID_PATTERN.test(text)) return false;
  return UUID_PATTERN.test(text);
}

/** 收集待删消息及其配对的一问一答（对齐 Code Compass copilot.ts） */
export function collectPairedMessageIds<T extends ChatRoleMessage>(
  messages: T[],
  targetIdx: number
): string[] {
  const current = messages[targetIdx];
  if (!current) return [];

  const ids = new Set<string>([String(current.id)]);
  if (current.role === 'user') {
    const next = messages[targetIdx + 1];
    if (next?.role === 'assistant') ids.add(String(next.id));
  } else if (current.role === 'assistant') {
    const prev = messages[targetIdx - 1];
    if (prev?.role === 'user') ids.add(String(prev.id));
  }
  return Array.from(ids);
}

export function removeMessagesByIds<T extends ChatRoleMessage>(
  messages: T[],
  idsToDelete: string[]
): T[] {
  const deletedSet = new Set(idsToDelete.map(String));
  return messages.filter((m) => !deletedSet.has(String(m.id)));
}
