export interface ParsedQuestionOption {
  key: string;
  content: string;
}

/** 形如 "A." / "A、" / "A）" / "A:" 的选项键前缀 */
const OPTION_KEY_PREFIX = /^\s*([A-Ha-h])\s*[.．、,:：)）]\s*/;

const fallbackKey = (index: number) => String.fromCharCode(65 + index);

/**
 * 解析题库 options 字段，兼容三种实际存储形态：
 * 1) [{ key: 'A', content: '...' }] / [{ key: 'A', val: '...' }] —— 标准结构
 * 2) ["A.xxx", "B.yyy"] —— AI 出题常见：选项键写在文本里（此前会解析失败导致选项不渲染、退化成手输框）
 * 3) { "A": "xxx", "B": "yyy" } —— 键值映射
 */
export function parseQuestionOptions(raw?: string | null): ParsedQuestionOption[] {
  if (!raw) return [];
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return [];
  }
  return normalizeOptions(parsed);
}

function normalizeOptions(parsed: unknown): ParsedQuestionOption[] {
  if (!parsed) return [];

  if (!Array.isArray(parsed)) {
    if (typeof parsed === 'object') {
      return Object.entries(parsed as Record<string, unknown>)
        .map(([key, value]) => ({
          key: String(key).trim().toUpperCase().replace(/[^A-H]/g, ''),
          content: String(value ?? '').trim()
        }))
        .filter((o) => o.key && o.content);
    }
    return [];
  }

  const list: ParsedQuestionOption[] = [];
  parsed.forEach((item, index) => {
    if (typeof item === 'string') {
      const match = item.match(OPTION_KEY_PREFIX);
      const key = match ? match[1].toUpperCase() : fallbackKey(index);
      const content = (match ? item.slice(match[0].length) : item).trim();
      if (content) list.push({ key, content });
      return;
    }
    if (item && typeof item === 'object') {
      const obj = item as { key?: unknown; content?: unknown; val?: unknown };
      const key = String(obj.key ?? '').trim() || fallbackKey(index);
      const content = String(obj.content ?? obj.val ?? '').trim();
      if (content) list.push({ key, content });
    }
  });
  return list;
}

/** 供练习页选项组件使用的 { key, val } 形态 */
export function parseQuestionOptionsAsVal(raw?: string | null): Array<{ key: string; val: string }> {
  return parseQuestionOptions(raw).map((o) => ({ key: o.key, val: o.content }));
}
