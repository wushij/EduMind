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
 * 以及大模型常误输出的非法数字转义宏（如 \0 \cdot \infty、\1^\infty、\0^0 等）
 * 在 LaTeX 中，单个斜杠加字母是命令；双斜杠 \\ 紧跟字母会被 KaTeX 误当作“强制换行 + 普通英文字母”；
 * 而反斜杠后直接跟数字（如 \0, \1）在 LaTeX 中是非法未定义指令，会导致 KaTeX 抛出 Undefined control sequence 并渲染为红色报错字符。
 */
export function repairLatexDoubleEscapes(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\{2,}([a-zA-Z]+)/g, '\\$1')
    .replace(/\\+([0-9])/g, '$1')
    .replace(/\\{2,}(\(|\)|\[|\])/g, '\\$1');
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
  'ln|sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|frac|dfrac|tfrac|sqrt|lim|sum|int|iint|iiint|oint|prod|partial|nabla|sim|approx|ne|neq|le|leq|ge|geq|in|notin|subset|subseteq|cup|cap|to|leftarrow|rightarrow|Rightarrow|Leftarrow|Leftrightarrow|forall|exists|infty|pm|times|div|cdot|alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega';

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

/**
 * 将混在中文散文/评语中未加 $ 的常用数学表达式（等价无穷小、等式、乘方多项式、初等函数）
 * 自动识别并包裹为 $...$，以便 KaTeX 精准渲染
 */
export function wrapProsePlainMath(text: string): string {
  if (!text) return '';
  // 纯英文自然语言句子（如 "The value of x^2 is 4"）避免过度识别，交给后续严格公式判断
  if (!/[\u4e00-\u9fa5]/.test(text) && PROSE_WORD_PAIR_RE.test(text)) {
    return text;
  }

  // 1. 保护已有标准公式与占位符
  const tokens: string[] = [];
  let s = text.replace(
    /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+?\$)/g,
    (m) => {
      const idx = tokens.length;
      tokens.push(m);
      return `%%%PROSE_MATH_TOKEN_${idx}%%%`;
    }
  );

  const CJK_OR_PUNCT_BEFORE = '(?<=[:：，,；;、。\\s\\u4e00-\\u9fa5\\uff00-\\uffef]|^)';
  const CJK_OR_PUNCT_AFTER = '(?=[:：，,；;、。\\s\\u4e00-\\u9fa5\\uff00-\\uffef]|$)';

  // 2. 极限箭头推导：例如 (x^3/6 + o(x^3))/x^3 ->1/6 或 原式 = ... -> 1/6
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}((?:[a-zA-Z0-9()+\\-*/^~]|\\s*=\\s*)+\\s*(?:->|→)\\s*[0-9a-zA-Z/+\\-]+)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      let expr = match.trim();
      if (!/[a-zA-Z]/.test(expr)) return match;
      expr = expr.replace(/->|→/g, ' \\to ').replace(/~/g, ' \\sim ');
      return `$${expr}$`;
    }
  );

  // 3. 数学等式：例如 sin x = x - x^3/6 + o(x^3) 或 x - sin x = x^3/6 + o(x^3)
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z0-9()+\\-*/^~\\s]{2,}=\\s*[a-zA-Z0-9()+\\-*/^~\\s]{2,})${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const expr = match.trim();
      if (!/[a-zA-Z]/.test(expr) || /[a-zA-Z]{4,}\s+[a-zA-Z]{4,}/.test(expr)) return match;
      if (!/(?:\^|\/|\+|-|\b(?:sin|cos|tan|cot|ln|exp|o)\b|\d)/.test(expr)) return match;
      let clean = expr.replace(/\b(sin|cos|tan|cot|sec|csc|ln|exp)\b/g, '\\$1 ');
      clean = clean.replace(/~/g, ' \\sim ');
      return `$${clean}$`;
    }
  );

  // 4. 等价无穷小：例如 e^x-1 ~ x、sin x ~ x
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z0-9()+\\-*/^]+)\\s*~\\s*([a-zA-Z0-9()+\\-*/^]+)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match, left: string, right: string) => {
      if (!/[a-zA-Z]/.test(left + right)) return match;
      let l = left.trim().replace(/\b(sin|cos|tan|ln|exp)\b/g, '\\$1 ');
      let r = right.trim().replace(/\b(sin|cos|tan|ln|exp)\b/g, '\\$1 ');
      return `$${l} \\sim ${r}$`;
    }
  );

  // 5. 代数项与含括号乘积：例如 x^2(e^x-1)、o(x^3)
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([a-zA-Z0-9]+\\^[0-9a-zA-Z]+(?:\\([a-zA-Z0-9^/+\\-\\s~]+\\))?|o\\([a-zA-Z0-9^/+\\-\\s]+\\))${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const expr = match.trim();
      if (!/[a-zA-Z]/.test(expr)) return match;
      return `$${expr}$`;
    }
  );

  // 6. 多项式减去初等函数：例如 x - sin x、1 - cos x
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}([0-9a-zA-Z^]+\\s*[-+]\\s*(?:sin|cos|tan|ln)\\s+[a-zA-Z0-9]+)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const expr = match.trim().replace(/\b(sin|cos|tan|ln)\b/g, '\\$1 ');
      return `$${expr}$`;
    }
  );

  // 7. 单独出现的初等函数与简单幂次项：例如 sin x、e^x、ln(1+x)、x^3
  s = s.replace(
    new RegExp(
      `${CJK_OR_PUNCT_BEFORE}((?:sin|cos|tan|cot|ln|exp)\\s*(?:\\([^)]+\\)|[a-zA-Z0-9]+)|[a-zA-Z]\\^[0-9a-zA-Z]+)${CJK_OR_PUNCT_AFTER}`,
      'g'
    ),
    (match) => {
      const expr = match.trim();
      let clean = expr.replace(/\b(sin|cos|tan|cot|ln|exp)\b/g, '\\$1 ');
      return `$${clean}$`;
    }
  );

  // 8. 还原占位符
  return s.replace(/%%%PROSE_MATH_TOKEN_(\d+)%%%/g, (_m, idxStr) => {
    const idx = Number(idxStr);
    return tokens[idx] ?? _m;
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
  const normalized = wrapBareLatexExpressions(wrapProsePlainMath(normalizeMathTextNewlines(text)));
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
          let escaped = escapeHtml(part.replace(/\n/g, ' '));
          // 处理 ***加粗斜体*** 与 **加粗** 语法，消除大模型裸露的星号标记
          escaped = escaped.replace(/\*\*\*([^*]+?)\*\*\*/g, '<strong><em>$1</em></strong>');
          escaped = escaped.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>');
          // 将采分点分项符（如「；- <strong>」或「。- <strong>」）优化为带圆点的换行分项
          escaped = escaped.replace(/([；;。])\s*-\s*<strong>/g, '$1<br />&bull; <strong>');
          escaped = escaped.replace(/(?:^|\s)-\s*<strong>/g, '<br />&bull; <strong>');
          // 兜底清理孤立残留的 ** 星号
          escaped = escaped.replace(/\*\*/g, '');
          return escaped;
        })
        .join('<br /><br />');
    })
    .join('');
}
