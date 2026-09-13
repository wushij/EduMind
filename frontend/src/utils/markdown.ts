import MarkdownIt from 'markdown-it';
import multimdTable from 'markdown-it-multimd-table';
import hljs from 'highlight.js';
import katex from 'katex';
import mermaid from 'mermaid';
import 'highlight.js/styles/atom-one-dark.css';
import 'katex/dist/katex.min.css';
import { normalizeChatTables } from './ai/chat-table-normalize';

const FENCED_CODE_BLOCK_RE = /(```[\s\S]*?```)/g;

const PLAIN_MATH_HINT =
  /(?:~|[\u2192\u221e\u00b2]|lim|sin|cos|tan|ln|arcsin|arctan|\^|\/\s*x|□)/i;

/** 模型输出的纯文本公式 → 可给 KaTeX 的 LaTeX */
function plainMathToLatex(raw: string): string {
  let s = raw.trim();
  s = s.replace(/ˣ/g, '^x').replace(/ᵃ/g, '^a');
  s = s.replace(/x₀/g, 'x_0');
  s = s.replace(/x²/g, 'x^2');
  s = s.replace(/∞/g, '\\infty');
  s = s.replace(/□/g, '\\square');
  s = s.replace(/\s*~\s*/g, ' \\sim ');
  // lim x→0 / limₓ→0 须在全局 → 替换之前处理
  s = s.replace(/\blimₓ→(0|∞|x₀|x_0)/gi, (_m, target: string) => {
    const t = target.replace(/∞/g, '\\infty').replace(/x₀|x_0/g, 'x_0');
    return `\\lim_{x \\to ${t}}`;
  });
  s = s.replace(/\blim\s+x→(0|∞|x₀|x_0)/gi, (_m, target: string) => {
    const t = target.replace(/∞/g, '\\infty').replace(/x₀|x_0/g, 'x_0');
    return `\\lim_{x \\to ${t}}`;
  });
  s = s.replace(/\blim\s+x\s*→\s*(\S+)/gi, (_m, target: string) => {
    const t = target.replace(/∞/g, '\\infty').replace(/x₀|x_0/g, 'x_0');
    return `\\lim_{x \\to ${t}}`;
  });
  s = s.replace(/\s*→\s*/g, ' \\to ');
  s = s.replace(/\blim\s*\(/gi, '\\lim(');
  s = s.replace(/\barcsin\b/g, '\\arcsin');
  s = s.replace(/\barctan\b/g, '\\arctan');
  s = s.replace(/\bsin\b/g, '\\sin');
  s = s.replace(/\bcos\b/g, '\\cos');
  s = s.replace(/\btan\b/g, '\\tan');
  s = s.replace(/\bln\b/g, '\\ln');
  s = s.replace(/\\sin\s+x\s*\/\s*x/g, '\\frac{\\sin x}{x}');
  s = s.replace(/\\sin\s*x\s*\/\s*x/g, '\\frac{\\sin x}{x}');
  // 规范化多余空格：(1 + x) → (1+x)
  s = s.replace(/\(\s+/g, '(').replace(/\s+\)/g, ')').replace(/\s*\+\s*/g, '+');
  return s;
}

function wrapPlainMath(expr: string): string {
  const trimmed = expr.trim();
  if (!trimmed || trimmed.includes('$') || !PLAIN_MATH_HINT.test(trimmed)) {
    return expr;
  }
  return `$${plainMathToLatex(trimmed)}$`;
}

/**
 * 模型常输出无 $ 包裹的纯文本公式（如 sin x ~ x、lim x→0 sin x/x =1），KaTeX 无法识别，需补 $ 并转 LaTeX。
 */
function normalizePlainMathExpressions(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      let s = segment;

      // limₓ→0 / lim x→0 / lim x→x₀ f(x)=f(x₀)
      s = s.replace(
        /(?<![\\$])\blimₓ→(?:0|∞|x₀|x_0)\s+[^；;，。\n]+?(?=[；;，。]|$)/gi,
        (match) => wrapPlainMath(match)
      );
      s = s.replace(
        /(?<![\\$])\blim\s+x→(?:0|∞|x₀|x_0)\s+[^；;，。\n]+?(?=[；;，。]|$)/gi,
        (match) => wrapPlainMath(match)
      );
      s = s.replace(
        /(?<![\\$])\blim\s+x\s*→\s*\S+\s+[^；;，。\n]+?(?=[；;，。]|$)/gi,
        (match) => wrapPlainMath(match)
      );

      // lim(sin x/x)=1; lim(1+1/x)^x=e（括号后可有 ^x 等）
      s = s.replace(
        /(?<![\\$])\blim\s*\([^)]+\)[^；;，。\s=]*=\s*[^；;，。\s]+/gi,
        (match) => wrapPlainMath(match)
      );

      // 等价无穷小：sin x ~ x, 1-cos x ~ x²/2
      s = s.replace(
        /(?<![\\$~])([0-9a-zA-Z()+\-^/\s²]+?)\s*~\s*([0-9a-zA-Z()+\-^/\s²]+?)(?=[,，；;。\n]|$)/g,
        (_m, left: string, right: string) => {
          if (!/[a-zA-Z]/.test(left + right)) return _m;
          return wrapPlainMath(`${left} ~ ${right}`);
        }
      );

      // 结构模板：sin/□→1,□→0；(1+□)^(1/□)→e
      s = s.replace(/(?<![\\$])sin\/□→\d+(?:,□→\d+)*/g, (match) => wrapPlainMath(match));
      s = s.replace(/(?<![\\$])\(\d+\+□\)\^\(\d+\/□\)→e/g, (match) => wrapPlainMath(match));

      return s;
    })
    .join('');
}

function closingDelimiterForLeft(open: string): string {
  switch (open) {
    case '(':
      return ')';
    case '[':
      return ']';
    case '{':
      return '}';
    case '|':
      return '|';
    case '.':
      return '.';
    default:
      return ']';
  }
}

function inferLeftDelimiter(beforeRight: string): string {
  let depth = 0;
  for (let i = beforeRight.length - 1; i >= 0; i--) {
    if (beforeRight.startsWith('\\left', i)) {
      const next = beforeRight[i + 5];
      if (next === '(' || next === '[' || next === '{' || next === '|' || next === '.') {
        return next;
      }
      if (next === '-') return '[';
    }
  }
  return '[';
}

/** 修复模型常输出的非法 \\left / \\right */
function repairMalformedLeftRight(latex: string): string {
  let s = latex;

  // \left-\frac{a}{b},1\right\cup → \left[-\frac{a}{b},1\right]\cup
  s = s.replace(
    /\\left-(\\frac\{[^}]+\}\{[^}]+\}(?:,[^\\]+?)?)\\right(?![\]\).|])/g,
    '\\left[-$1\\right]'
  );

  // \left-\frac{a}{b}\right（无逗号区间）
  s = s.replace(
    /\\left-(\\frac\{[^}]+\}\{[^}]+\})\\right(?![\]\).|])/g,
    '\\left[-$1\\right]'
  );

  // \left- 缺定界符
  s = s.replace(/\\left-(?=\\frac|\d)/g, '\\left[-');

  // \left( … \right\cup → \right)\cup
  s = s.replace(/\\left\(((?:[^\\]|\\.)*?)\\right(?=\\cup)/g, '\\left($1\\right)\\cup');
  // \left[ … \right\cup → \right]\cup
  s = s.replace(/\\left\[((?:[^\\]|\\.)*?)\\right(?=\\cup)/g, '\\left[$1\\right]\\cup');

  // 兜底：\right 后接 \cup 时按最近的 \left 补全闭合符
  s = s.replace(/\\right(?=\\cup)/g, (_match, offset: number, whole: string) => {
    const before = whole.slice(0, offset);
    const open = inferLeftDelimiter(before);
    return `\\right${closingDelimiterForLeft(open)}`;
  });

  // \left( … \right 行末缺 )
  s = s.replace(/\\left\(((?:[^\\]|\\.)*?)\\right(?![\]\).|])/g, '\\left($1\\right)');

  return repairCommonLatexSpacing(s);
}

/** 修复模型输出里 LaTeX 命令与后续字母粘连（如 \\quadB → \\quad B） */
function repairCommonLatexSpacing(latex: string): string {
  let s = latex;
  const cmds = [
    'quad',
    'qquad',
    'Rightarrow',
    'Leftrightarrow',
    'rightarrow',
    'leftarrow',
    'subseteq',
    'supseteq',
    'subset',
    'supset',
    'leq',
    'geq',
    'neq',
    'mid',
    'cdot',
    'times'
  ];
  for (const cmd of cmds) {
    s = s.replace(new RegExp(`\\\\${cmd}([A-Za-z])`, 'g'), `\\${cmd} $1`);
  }
  return s;
}

/** 将常见 Unicode 数学符号转为 LaTeX（仅在即将进入公式的片段内使用） */
function normalizeUnicodeMathSymbols(text: string): string {
  return text
    .replace(/⊆/g, '\\subseteq ')
    .replace(/⊂/g, '\\subset ')
    .replace(/≥/g, '\\geq ')
    .replace(/≤/g, '\\leq ')
    .replace(/≠/g, '\\neq ')
    .replace(/∈/g, '\\in ')
    .replace(/∪/g, '\\cup ')
    .replace(/∩/g, '\\cap ')
    .replace(/²/g, '^2')
    .replace(/³/g, '^3')
    .replace(/₀/g, '_0')
    .replace(/₁/g, '_1');
}

/** 将 Unicode 区间写法 a∈(-½,1)∪(3,+∞) 转为 $...$ LaTeX */
function normalizeUnicodeIntervalNotation(text: string): string {
  const fracMap: Record<string, string> = { '½': '\\frac{1}{2}', '¼': '\\frac{1}{4}', '¾': '\\frac{3}{4}' };

  const toLatexNum = (raw: string): string => {
    let t = raw.trim();
    for (const [u, lx] of Object.entries(fracMap)) {
      t = t.replace(new RegExp(u, 'g'), lx);
    }
    return t.replace(/∞/g, '\\infty').replace(/−/g, '-');
  };

  return text.replace(
    /([a-zA-Z]?)\s*∈\s*\(([^)]+)\)\s*∪\s*\(([^)]+)\)/g,
    (_m, varName: string, interval1: string, interval2: string) => {
      const v = varName || 'x';
      return `$${v} \\in (${toLatexNum(interval1)}) \\cup (${toLatexNum(interval2)})$`;
    }
  );
}

/** 未加 $ 的 \\in\\left... 片段自动包裹 */
function wrapRawLatexFragments(text: string): string {
  return text.replace(
    /(?<!\$)(\\(?:in|notin|subset|cup|cap)[^$\n]*\\left[^$\n]+?\\right(?:[\]\).]|\\right)(?:\\cup[^$\n]+?)?)/g,
    (match) => `$${repairMalformedLeftRight(match.trim())}$`
  );
}

/** 将 \(...\)/\[...\] 及模型常用的 [latex] 转为 $...$ / $$...$$ */
function normalizeLatexDelimiters(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      let s = segment
        .replace(/\\\\([\(\[\]\)])/g, '\\$1')
        .replace(/\\\[([\s\S]+?)\\\]/g, (_m, formula: string) => `\n$$${repairMalformedLeftRight(formula.trim())}$$\n`)
        .replace(/\\\(([\s\S]+?)\\\)/g, (_m, formula: string) =>
          `$${repairMalformedLeftRight(formula.replace(/\$/g, '').trim())}$`
        );

      // 模型常输出 [ f'(x)=\frac{...}{...} ] 而非 $...$
      s = s.replace(
        /\[\s*([^[\n]*\\(?:frac|left|right|boxed|lim|ln|le|ge|Rightarrow|infty|varepsilon|delta)[^[\n]*)\s*\]/g,
        (_m, formula: string) => `$${repairMalformedLeftRight(formula.trim())}$`
      );


      // 修复 $...$ 与 $$...$$ 内部的 \left/\right
      s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_m, formula: string) => `$$${repairMalformedLeftRight(formula)}$$`);
      s = s.replace(/(?<!\$)\$(?!\$)([^$\n]+?)(?<!\$)\$(?!\$)/g, (_m, formula: string) =>
        `$${repairMalformedLeftRight(formula)}$`
      );

      s = normalizeUnicodeIntervalNotation(s);
      s = wrapRawLatexFragments(s);
      s = normalizePlainMathExpressions(s);

      // 集合描述 {x | ...}、区间 [1,3] 等纯文本数学片段补 $ 包裹
      s = s.replace(
        /(?<!\$)([A-Z]\s*=\s*\{[^{}\n|]+\|\s*[^}\n]+\})(?!\$)/g,
        (_m, expr: string) => `$${normalizeUnicodeMathSymbols(expr.trim())}$`
      );
      s = s.replace(
        /(?<!\$)(\d+\s*≤\s*[a-zA-Z]\s*≤\s*\d+)(?!\$)/g,
        (_m, expr: string) => `$${normalizeUnicodeMathSymbols(expr.trim())}$`
      );

      return s;
    })
    .join('');
}

mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'loose',
  theme: 'default',
  themeVariables: {
    darkMode: false,
    background: 'transparent',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    fontSize: '13px',
    primaryColor: '#e6f4ff',
    primaryTextColor: '#0958d9',
    primaryBorderColor: '#91caff',
    lineColor: '#1677ff',
    nodeTextColor: '#0f172a',
    edgeLabelBackground: '#ffffff'
  },
  flowchart: {
    htmlLabels: true,
    useMaxWidth: true,
    curve: 'basis'
  }
});

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
});

if (!(md.utils as { assign?: typeof Object.assign }).assign) {
  (md.utils as { assign: typeof Object.assign }).assign = Object.assign;
}

md.use(multimdTable, {
  multiline: false,
  rowspan: false,
  headerless: false
});

const GENERIC_LANGS = new Set(['', 'text', 'plain', 'txt', 'code', 'output', 'console']);

function normalizeDeclaredLang(declared: string): string {
  const decl = declared.toLowerCase().trim();
  if (GENERIC_LANGS.has(decl)) return '';
  if (decl === 'py' || decl === 'python3') return 'python';
  if (decl === 'js') return 'javascript';
  if (decl === 'ts') return 'typescript';
  if (decl === 'sh' || decl === 'shell' || decl === 'zsh') return 'bash';
  return decl;
}

function formatLangLabel(lang: string): string {
  const map: Record<string, string> = {
    py: 'python',
    python3: 'python',
    js: 'javascript',
    ts: 'typescript',
    sh: 'bash',
    shell: 'bash',
    zsh: 'bash',
    htm: 'html',
    md: 'markdown',
    mermaid: 'mermaid'
  };
  const key = lang.toLowerCase();
  if (GENERIC_LANGS.has(key)) return 'code';
  return map[key] || key || 'code';
}

const MERMAID_HINT =
  /^(?:graph |flowchart |sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie |gitGraph|journey|mindmap|timeline)/m;

function looksLikeMermaid(code: string): boolean {
  const trimmed = code.trim();
  if (MERMAID_HINT.test(trimmed)) return true;
  const hasArrow = /(?:-->|==>|-\.->)\s*(?:\[|\(|\{|\w+)/.test(trimmed);
  const arrowCount = (trimmed.match(/-->|==>|-\.->/g) || []).length;
  return hasArrow && arrowCount >= 2;
}

function normalizeMermaidCode(raw: string): string {
  let text = raw.trim();
  if (!MERMAID_HINT.test(text)) {
    text = `graph TD\n${text}`;
  }
  return text;
}

function inferOrphanCodeLang(code: string): string {
  if (looksLikeMermaid(code)) return 'mermaid';
  if (/<!DOCTYPE html>|<html\b|<\/?(?:head|body|form|table|div|p|h[1-6])\b/i.test(code)) {
    return 'html';
  }
  if (/(?:\/\*|\*\/|\b(?:font-family|margin|padding|background|color)\s*:|#[0-9a-fA-F]{3,8}\b)/i.test(code)) {
    return 'css';
  }
  if (/(?:^import \{|^export const|interface \w+|type \w+\s*=)/m.test(code)) return 'typescript';
  if (/(?:console\.log|document\.|const |let |var |function\b)/m.test(code)) return 'javascript';
  if (/(?:^def |^import |^from |print\(|asyncio\.)/m.test(code)) return 'python';
  if (/^(\$|curl|npm|pnpm|yarn|mvn|docker|git)\s/m.test(code)) return 'bash';
  if (/(?:public class|System\.out\.println|@SpringBootApplication)/m.test(code)) return 'java';
  if (/(?:SELECT .* FROM|INSERT INTO|CREATE TABLE)/i.test(code)) return 'sql';
  return 'text';
}

function inferCodeFenceLanguage(code: string, declared: string): string {
  const decl = normalizeDeclaredLang(declared);
  const text = code.trim();
  if (decl === 'mermaid' || looksLikeMermaid(text)) return 'mermaid';
  if (decl === 'json' || (!decl && /^\s*[[{]/.test(text) && /"[\w_]+"\s*:/.test(text))) return 'json';
  if (decl && hljs.getLanguage(decl)) return decl;
  return inferOrphanCodeLang(text);
}

let mermaidCounter = 0;

function renderCodeBlock(code: string, declaredLang: string): string {
  if (!code?.trim()) return '';

  const lang = inferCodeFenceLanguage(code, declaredLang);

  if (lang === 'mermaid') {
    const normalized = normalizeMermaidCode(code);
    const encoded = encodeURIComponent(normalized);
    return `<div class="mermaid-diagram-wrapper"><div class="mermaid-header"><span>✦ 课程拓扑知识图谱</span></div><div class="mermaid-diagram" data-code="${encoded}"><span class="mermaid-loading">图谱渲染中…</span></div></div>`;
  }

  let highlighted = '';
  if (lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value;
    } catch {
      highlighted = md.utils.escapeHtml(code);
    }
  } else {
    try {
      highlighted = hljs.highlightAuto(code).value;
    } catch {
      highlighted = md.utils.escapeHtml(code);
    }
  }

  const langLabel = `<span class="code-lang">${formatLangLabel(lang)}</span>`;
  const copyBtn = `<button type="button" class="code-copy-btn" title="复制代码">复制</button>`;

  return `<div class="code-block-wrapper"><div class="code-header">${langLabel}${copyBtn}</div><pre class="hljs"><code class="language-${lang || 'text'}">${highlighted}</code></pre></div>`;
}

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx];
  const info = token.info ? md.utils.unescapeAll(token.info).trim() : '';
  const lang = info.split(/\s+/g)[0] || '';
  return renderCodeBlock(token.content, lang);
};

md.renderer.rules.table_open = () => '<div class="table-wrap"><table>';
md.renderer.rules.table_close = () => '</table></div>';

const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options));

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  const href = String(token.attrGet('href') ?? '').trim();
  const isHash = href.startsWith('#');
  token.attrSet('class', 'chat-link');
  if (!isHash && href) {
    token.attrSet('target', '_blank');
    token.attrSet('rel', 'noopener noreferrer');
  }
  return defaultLinkOpen(tokens, idx, options, env, self);
};

/** 表格内行内 code 去掉标签包裹（Code Compass 同款） */
export function unwrapInlineCodeInTables(html: string): string {
  if (!html || typeof document === 'undefined') return html;
  const doc = new DOMParser().parseFromString(`<div id="md-root">${html}</div>`, 'text/html');
  const root = doc.getElementById('md-root');
  if (!root) return html;

  root.querySelectorAll('td code, th code').forEach((codeEl) => {
    if (codeEl.closest('pre')) return;
    codeEl.replaceWith(doc.createTextNode(codeEl.textContent || ''));
  });

  root.querySelectorAll('td .code-block-wrapper, th .code-block-wrapper').forEach((wrapper) => {
    const codeEl = wrapper.querySelector('code');
    const text = (codeEl?.textContent || wrapper.querySelector('pre')?.textContent || '').trim();
    const pre = doc.createElement('pre');
    pre.className = 'table-cell-code';
    pre.textContent = text;
    wrapper.replaceWith(pre);
  });

  return root.innerHTML;
}

/**
 * Copilot 聊天专用 Markdown 渲染（Code Compass renderMarkdownForChat + 文章同款 KaTeX 占位流程）
 */
export function renderMarkdownForChat(content: string): string {
  return renderMarkdownPipeline(content, true);
}

/** 深度思考区：与正文相同 KaTeX 流程，但不走表格容错（避免 {x | ...} 被误拆） */
export function renderMarkdownForReasoning(content: string): string {
  return renderMarkdownPipeline(content, false);
}

function renderKatexHtml(formula: string, displayMode: boolean): string {
  const fixed = repairMalformedLeftRight(formula.trim());
  const html = katex.renderToString(fixed, { displayMode, throwOnError: false, strict: 'ignore' });
  if (!html.includes('katex-error')) return html;

  const fallback = fixed
    .replace(/\\left[\[\({|.]/g, (m) => (m.endsWith('[') ? '[' : m.endsWith('(') ? '(' : m.endsWith('{') ? '{' : ''))
    .replace(/\\right[\]\)}.|]/g, (m) => (m.endsWith(']') ? ']' : m.endsWith(')') ? ')' : m.endsWith('}') ? '}' : ''));
  const retry = katex.renderToString(fallback, { displayMode, throwOnError: false, strict: 'ignore' });
  return retry.includes('katex-error') ? html : retry;
}

function renderMarkdownPipeline(content: string, normalizeTables: boolean): string {
  if (!content?.trim()) return '';

  let processed = normalizeTables
    ? normalizeLatexDelimiters(normalizeChatTables(content))
    : normalizeLatexDelimiters(content);

  const blockKatexMap = new Map<string, string>();
  let blockIdx = 0;
  processed = processed.replace(/\$\$([\s\S]+?)\$\$/g, (_match, math: string) => {
    const key = `%%%KATEX_BLOCK_${blockIdx++}%%%`;
    blockKatexMap.set(key, `<div class="katex-block-wrapper">${renderKatexHtml(math, true)}</div>`);
    return key;
  });

  const inlineKatexMap = new Map<string, string>();
  let inlineIdx = 0;
  processed = processed.replace(/(?<!\$)\$(?!\$)(.+?)(?<!\$)\$(?!\$)/g, (_match, math: string) => {
    const key = `%%%KATEX_INLINE_${inlineIdx++}%%%`;
    inlineKatexMap.set(key, renderKatexHtml(math, false));
    return key;
  });

  let renderedHtml = md.render(processed);

  blockKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replace(`<p>${key}</p>`, htmlVal).replaceAll(key, htmlVal);
  });
  inlineKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replaceAll(key, htmlVal);
  });

  return unwrapInlineCodeInTables(renderedHtml);
}

export async function renderMermaidInElement(root: HTMLElement | null) {
  if (!root) return;
  const diagrams = root.querySelectorAll<HTMLElement>('.mermaid-diagram:not([data-processed="true"])');
  for (const el of Array.from(diagrams)) {
    const rawCode = el.dataset.code ? decodeURIComponent(el.dataset.code) : el.textContent || '';
    if (!rawCode.trim()) continue;

    const id = `mermaid-graph-${Date.now()}-${mermaidCounter++}`;
    const codeToRender = normalizeMermaidCode(rawCode);
    try {
      const { svg } = await mermaid.render(id, codeToRender);
      el.innerHTML = svg;
      el.setAttribute('data-processed', 'true');
    } catch {
      el.innerHTML = `<div class="mermaid-fallback-box"><pre><code>${md.utils.escapeHtml(rawCode)}</code></pre></div>`;
      el.setAttribute('data-processed', 'true');
    }
  }
}

export function bindMarkdownCodeCopy(root: HTMLElement | null) {
  if (!root) return;
  root.querySelectorAll<HTMLButtonElement>('.code-copy-btn').forEach((btn) => {
    if (btn.dataset.bound === '1') return;
    btn.dataset.bound = '1';
    btn.type = 'button';
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      e.stopPropagation();
      const code = btn.closest('.code-block-wrapper')?.querySelector('code')?.textContent || '';
      try {
        await navigator.clipboard.writeText(code);
        const prevText = btn.textContent || '复制';
        btn.textContent = '已复制';
        btn.classList.add('is-copied');
        setTimeout(() => {
          btn.textContent = prevText;
          btn.classList.remove('is-copied');
        }, 1500);
      } catch {
        /* clipboard blocked */
      }
    });
  });

  void renderMermaidInElement(root);
}
