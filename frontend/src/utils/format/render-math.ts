import katex from 'katex';

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * 修复因 JSON/SQL/大模型多重转义导致的 LaTeX 命令双斜杠问题（如 \\frac, \\cos, \\sin, \\ln, \\sim, \\to 等）
 * 在 LaTeX 中，单个斜杠加字母是命令；双斜杠 \\ 紧跟字母会被 KaTeX 误当作“强制换行 + 普通英文字母”
 */
export function repairLatexDoubleEscapes(text: string): string {
  if (!text) return '';
  return text.replace(/\\{2,}([a-zA-Z]+)/g, '\\$1');
}

function renderKatex(formula: string, displayMode: boolean): string {
  try {
    let cleaned = repairLatexDoubleEscapes(formula.trim());
    if (
      !displayMode &&
      !/\\begin\{(?:matrix|cases|array|aligned|gathered|bmatrix|pmatrix|vmatrix|Vmatrix)\}/.test(cleaned)
    ) {
      // 行内数学公式中不应含有孤立的强制换行 \\，将其转为空格避免破坏 CSS 基线导致碎行上浮
      cleaned = cleaned.replace(/\\\\(?!\s*[\n\r]|\[)/g, ' ');
    }
    return katex.renderToString(cleaned, {
      throwOnError: false,
      displayMode,
      strict: 'ignore'
    });
  } catch {
    return displayMode ? `$$${formula}$$` : `$${formula}$`;
  }
}

/** JSON 未转义时 \\frac 会变成 Form Feed + rac，导致公式碎成多行纯文本 */
export function repairControlCharsInMathText(text: string): string {
  return text.replace(/\u000C(?=rac\{)/g, '\\f');
}

/** 模型/JSON 常输出字面量 \\n；勿误伤 LaTeX 命令如 \\neq、\\not */
export function normalizeMathTextNewlines(text: string): string {
  return repairLatexDoubleEscapes(repairControlCharsInMathText(text))
    .replace(/\r\n/g, '\n')
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n(?![a-zA-Z])/g, '\n');
}

type MathSegment = { kind: 'text'; value: string } | { kind: 'math'; value: string; display: boolean };

function splitMathSegments(text: string): MathSegment[] {
  const normalized = normalizeMathTextNewlines(text);
  const segments: MathSegment[] = [];
  let i = 0;

  while (i < normalized.length) {
    if (normalized.startsWith('$$', i)) {
      const end = normalized.indexOf('$$', i + 2);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 2, end), display: true });
        i = end + 2;
        continue;
      }
    }
    if (normalized.startsWith('\\[', i)) {
      const end = normalized.indexOf('\\]', i + 2);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 2, end), display: true });
        i = end + 2;
        continue;
      }
    }
    if (normalized.startsWith('\\(', i)) {
      const end = normalized.indexOf('\\)', i + 2);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 2, end), display: false });
        i = end + 2;
        continue;
      }
    }
    if (normalized[i] === '$' && normalized[i + 1] !== '$') {
      const end = normalized.indexOf('$', i + 1);
      if (end !== -1) {
        segments.push({ kind: 'math', value: normalized.slice(i + 1, end), display: false });
        i = end + 1;
        continue;
      }
    }

    let next = normalized.length;
    for (const marker of ['$$', '\\[', '\\(', '$']) {
      const pos = normalized.indexOf(marker, i + 1);
      if (pos !== -1 && pos < next) {
        next = pos;
      }
    }
    const chunk = normalized.slice(i, next);
    if (chunk) {
      segments.push({ kind: 'text', value: chunk });
    }
    i = next;
  }

  return segments;
}

export function renderMathText(text: string): string {
  if (!text) return '';

  return splitMathSegments(text)
    .map((seg) => {
      if (seg.kind === 'math') {
        const html = renderKatex(seg.value, seg.display);
        return seg.display ? `<span class="math-block">${html}</span>` : html;
      }
      const parts = seg.value.replace(/\r/g, '').split(/\n{2,}/);
      return parts.map((part) => escapeHtml(part.replace(/\n/g, ' '))).join('<br /><br />');
    })
    .join('');
}
