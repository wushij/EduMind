export interface ParsedQuestionOption {
  key: string;
  content: string;
}

/** 解析题库 options JSON（兼容 content / val 字段） */
export function parseQuestionOptions(raw?: string | null): ParsedQuestionOption[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((o: { key?: string; content?: string; val?: string }) => ({
      key: String(o.key ?? '').trim(),
      content: String(o.content ?? o.val ?? '').trim()
    })).filter((o) => o.key);
  } catch {
    return [];
  }
}

/** 供练习页选项组件使用的 { key, val } 形态 */
export function parseQuestionOptionsAsVal(raw?: string | null): Array<{ key: string; val: string }> {
  return parseQuestionOptions(raw).map((o) => ({ key: o.key, val: o.content }));
}
