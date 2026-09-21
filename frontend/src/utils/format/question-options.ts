export interface ParsedQuestionOption {
  key: string;
  content: string;
}

/** 形如 "A." / "A、" / "A）" / "A:" 的选项键前缀 */
const OPTION_KEY_PREFIX = /^\s*([A-Ha-h])\s*[.．、,:：)）]\s*/;

/**
 * 选项文本字段名白名单：题库历史上出现过 content / val / text 三种写法。
 *
 * 注意必须用白名单而不能"取第一个非空字符串字段"：选项对象里还有 isCorrect/correct 等字段，
 * 一旦取错会把 "true" 当成选项文本。也刻意不把 `value` 列入 —— AI 可能输出 `{key,value,isCorrect}`，
 * 那是在描述"正确与否"，语义不确定时选择不展示，好过展示错误答案文本。
 */
const OPTION_TEXT_FIELDS = ['content', 'val', 'text', 'optionContent', 'option', 'label', 'title'] as const;

const fallbackKey = (index: number) => String.fromCharCode(65 + index);

/** 从选项对象里按白名单取出文本 */
function pickOptionText(obj: Record<string, unknown>): string {
  for (const field of OPTION_TEXT_FIELDS) {
    const value = obj[field];
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }
  return '';
}

/**
 * 把选项对象数组归一为标准的 { key, content, isCorrect } 形态。
 * 生成侧用它把大模型可能输出的 text/val 等字段统一成 content 再入库，
 * 保证"写进去的格式"和"读出来的格式"一致，不依赖前端兜底。
 */
export function normalizeOptionObjects(raw: unknown): Array<{ key: string; content: string; isCorrect?: boolean }> {
  if (!Array.isArray(raw)) return [];
  const list: Array<{ key: string; content: string; isCorrect?: boolean }> = [];
  raw.forEach((item, index) => {
    if (!item || typeof item !== 'object') return;
    const obj = item as Record<string, unknown>;
    const content = pickOptionText(obj);
    if (!content) return;
    const key = String(obj.key ?? '').trim() || fallbackKey(index);
    const normalized: { key: string; content: string; isCorrect?: boolean } = { key, content };
    const isCorrect = obj.isCorrect ?? obj.correct;
    if (typeof isCorrect === 'boolean') {
      normalized.isCorrect = isCorrect;
    }
    list.push(normalized);
  });
  return list;
}

/**
 * 解析题库 options 字段，兼容三种实际存储形态：
 * 1) [{ key: 'A', text: '...' }] —— 标准结构（字段名兼容 content/val/text/option/label/value）
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
      const obj = item as Record<string, unknown>;
      const key = String(obj.key ?? '').trim() || fallbackKey(index);
      const content = pickOptionText(obj);
      if (content) list.push({ key, content });
    }
  });
  return list;
}

/** 供练习页选项组件使用的 { key, val } 形态 */
export function parseQuestionOptionsAsVal(raw?: string | null): Array<{ key: string; val: string }> {
  return parseQuestionOptions(raw).map((o) => ({ key: o.key, val: o.content }));
}
