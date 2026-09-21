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

const COMMON_MATH_COMMANDS =
  'ln|sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|frac|sqrt|lim|sum|int|iint|iiint|oint|prod|partial|nabla|sim|approx|ne|neq|le|leq|ge|geq|in|notin|subset|subseteq|cup|cap|to|leftarrow|rightarrow|Rightarrow|Leftarrow|Leftrightarrow|forall|exists|infty|pm|times|div|cdot|alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega';

const BARE_LATEX_RE = new RegExp(
  `(?<![\\\\$a-zA-Z0-9])\\\\(?:${COMMON_MATH_COMMANDS})(?:\\\\[a-zA-Z]+|\\{[^{}]*\\}|\\[[^\\]]*\\]|\\([^()]*\\)|[0-9a-zA-Z+\\-*/=^_(),.:~\\s])*(?=[，；。！？、\\n]|$)`,
  'g'
);

/**
 * 将文本中未被 $ 包裹的裸 LaTeX 命令表达式（如 \\ln(1+x) \\sim x、\\sin 2x \\sim 2x）
 * 自动识别并包裹为 $...$，便于 KaTeX 精准解析渲染
 */
export function wrapBareLatexExpressions(text: string): string {
  if (!text || !text.includes('\\')) return text;

  // 1. 保护已有标准数学公式占位符
  const placeholders: string[] = [];
  let masked = text.replace(
    /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+?\$)/g,
    (m) => {
      const idx = placeholders.length;
      placeholders.push(m);
      return `%%%MATH_TOKEN_${idx}%%%`;
    }
  );

  // 2. 匹配裸 LaTeX 表达式
  masked = masked.replace(BARE_LATEX_RE, (match) => {
    const trimmed = match.trim();
    if (!trimmed || trimmed.startsWith('%%%MATH_TOKEN_')) return match;
    // 去除末尾可能被多吃的英文逗号句号或冒号
    const clean = trimmed.replace(/[，；。！？]+$/, '').trim();
    if (!clean) return match;
    return `$${clean}$`;
  });

  // 3. 还原占位符
  return masked.replace(/%%%MATH_TOKEN_(\d+)%%%/g, (_m, idxStr) => {
    const idx = Number(idxStr);
    return placeholders[idx] ?? _m;
  });
}

const CJK_OR_FULLWIDTH_RE = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/;
const BARE_FORMULA_CHARS_RE = /^[0-9A-Za-z\s+\-*/=().,;:^_]+$/;
/** 连续两个 3 字母以上单词（The value / value of）基本可判定为自然语言句子，而不是公式 */
const PROSE_WORD_PAIR_RE = /[A-Za-z]{3,}\s+[A-Za-z]{3,}/;

/**
 * 纯文本公式判定：兜底渲染未加 $ 定界符的公式（如 e^6、2^h - 1、dy/dx = 3(t^2+1)/(2t)）。
 * 这类数据来自历史导入/人工录入，KaTeX 无法识别，此前只能原样显示。
 * 保守约束（避免把中文说明、英文句子、代码或正则误当公式）：
 * 1. 整段不含中文/全角字符；2. 同时含数字与上标(^)或下标(_)；3. 只允许数学与标点字符；
 * 4. 不含反斜杠/美元符/花括号/方括号等 LaTeX 或正则特征；5. 不含自然语言单词对。
 */
export function isBareFormulaSegment(text: string): boolean {
  const trimmed = text.trim();
  if (!trimmed || trimmed.length > 160) {
    return false;
  }
  if (CJK_OR_FULLWIDTH_RE.test(trimmed) || PROSE_WORD_PAIR_RE.test(trimmed)) {
    return false;
  }
  if (!/[\^_]/.test(trimmed) || !/\d/.test(trimmed)) {
    return false;
  }
  return BARE_FORMULA_CHARS_RE.test(trimmed);
}

type MathSegment = { kind: 'text'; value: string } | { kind: 'math'; value: string; display: boolean };

function splitMathSegments(text: string): MathSegment[] {
  const normalized = wrapBareLatexExpressions(normalizeMathTextNewlines(text));
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
      return parts
        .map((part) => {
          if (isBareFormulaSegment(part)) {
            return renderKatex(part.trim(), false);
          }
          return escapeHtml(part.replace(/\n/g, ' '));
        })
        .join('<br /><br />');
    })
    .join('');
}
