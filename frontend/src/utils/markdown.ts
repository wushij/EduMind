import MarkdownIt from 'markdown-it';
import multimdTable from 'markdown-it-multimd-table';
import hljs from 'highlight.js';
import katex from 'katex';
import mermaid from 'mermaid';
import 'highlight.js/styles/atom-one-dark.css';
import 'katex/dist/katex.min.css';
import { normalizeChatTables } from './ai/chat-table-normalize';
import {
  asciiTreeToMermaid,
  expandAsciiTreeToMultiline,
  looksLikeAsciiKnowledgeTree
} from './ai/ascii-tree-graph';
import { repairLatexDoubleEscapes, repairCommonLatexSpacing } from './format/render-math';

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
    /(?<!\$)(\\(?:in|notin|subset|cup|cap)(?![a-zA-Z])[^$\n]*\\left[^$\n]+?\\right(?:[\]\).]|\\right)(?:\\cup[^$\n]+?)?)/g,
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

      // 1. 保护已有合法 $$...$$ 独立块与 $...$ 行内公式，防止多行块内换行被后续单行正则拆碎打烂
      const mathPlaceholders: string[] = [];
      const maskMath = (raw: string): string => {
        const key = `%%%PRE_MATH_TOKEN_${mathPlaceholders.length}%%%`;
        mathPlaceholders.push(raw);
        return key;
      };

      // 修复已有的 $$...$$ 与 $...$ 内部语法，并将其占位保护起来
      s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_m, formula: string) =>
        maskMath(`$$${repairMalformedLeftRight(formula)}$$`)
      );
      s = s.replace(/(?<!\$)\$(?!\$)([^$\n]+?)(?<!\$)\$(?!\$)/g, (_m, formula: string) =>
        maskMath(`$${repairMalformedLeftRight(formula)}$`)
      );

      // 2. 裸多行环境识别（如未包裹 $$ 的 \begin{aligned}...\end{aligned}、\boxed{...}）自动包裹为 $$...$$
      s = s.replace(
        /(^|\n)[ \t]*(\\begin\{(?:aligned|cases|matrix|bmatrix|pmatrix|gathered)\}[\s\S]*?\\end\{(?:aligned|cases|matrix|bmatrix|pmatrix|gathered)\})(?=[ \t]*(?:\n|$))/g,
        (_m, prefix: string, formula: string) => `${prefix}\n$$${formula.trim()}$$\n`
      );
      s = s.replace(
        /(^|\n)[ \t]*(\\boxed\s*\{[\s\S]*?\n?[ \t]*\})(?=[ \t]*(?:\n|$))/g,
        (_m, prefix: string, formula: string) => `${prefix}\n$$${formula.trim()}$$\n`
      );

      // 3. 独占一行的裸 LaTeX 独立数学公式（如模型直接输出未包裹的 \lim_{x\to\infty}...\right)^x=e）自动包裹为 $$...$$
      s = s.replace(
        /(^|\n)[ \t]*(\\(?:lim|frac|sqrt|sum|int|iint|iiint|oint|prod|left|displaystyle|aligned)(?![a-zA-Z])[^\$\n]+?)(?=[ \t]*(?:\n|$))/g,
        (_m, prefix: string, formula: string) => `${prefix}\n$$${formula.trim()}$$\n`
      );

      // 4. 模型常输出 [ f'(x)=\frac{...}{...} ] 而非 $...$
      // 注意：必须排除 LaTeX 语法本身的 \left[ ... \right]、\Big[ ... \Big] 等括号！
      s = s.replace(
        /(\\?(?:left|right|big|Big|bigg|Bigg)\s*)?\[\s*([^[\n]*\\(?:frac|left|right|boxed|lim|ln|le|ge|Rightarrow|infty|varepsilon|delta)[^[\n]*?)\s*(\\?(?:left|right|big|Big|bigg|Bigg)\s*)?\]/g,
        (match, prefixCmd, formula, suffixCmd) => {
          if (prefixCmd || suffixCmd || match.startsWith('\\[') || match.startsWith('\\[')) {
            return match;
          }
          return `$${repairMalformedLeftRight(formula.trim())}$`;
        }
      );

      s = normalizeUnicodeIntervalNotation(s);
      s = wrapRawLatexFragments(s);
      s = normalizePlainMathExpressions(s);

      // 5. 集合描述 {x | ...}、区间 [1,3] 等纯文本数学片段补 $ 包裹
      s = s.replace(
        /(?<!\$)([A-Z]\s*=\s*\{[^{}\n|]+\|\s*[^}\n]+\})(?!\$)/g,
        (_m, expr: string) => `$${normalizeUnicodeMathSymbols(expr.trim())}$`
      );
      s = s.replace(
        /(?<!\$)(\d+\s*≤\s*[a-zA-Z]\s*≤\s*\d+)(?!\$)/g,
        (_m, expr: string) => `$${normalizeUnicodeMathSymbols(expr.trim())}$`
      );

      // 6. 还原占位符（使用函数回调防止 $$ 被 replace 机制吞并为单个 $）
      mathPlaceholders.forEach((mathStr, idx) => {
        s = s.replaceAll(`%%%PRE_MATH_TOKEN_${idx}%%%`, () => mathStr);
      });

      return s;
    })
    .join('');
}

const KNOWN_CODE_LANGS = [
  'c\\+\\+', 'cpp', 'csharp', 'c#', 'python', 'py', 'javascript', 'js',
  'typescript', 'ts', 'java', 'golang', 'go', 'rust', 'rs', 'kotlin', 'kt',
  'swift', 'scala', 'sql', 'html', 'css', 'scss', 'less', 'bash', 'shell', 'sh', 'zsh',
  'json', 'xml', 'yaml', 'yml', 'markdown', 'md', 'mermaid', 'r', 'matlab',
  'php', 'ruby', 'rb', 'perl', 'lua', 'dart', 'c',
  'text', 'plain', 'txt', 'console', 'output', 'code'
];

/**
 * 修复大模型输出代码块时缺失换行导致 Markdown 语法颠倒的问题：
 * 1. ``` 前缺失换行（如「### 例题3：混合嵌套循环```c」或「嵌套混合```」）
 *    -> 若前面紧贴非换行内容，CommonMark 无法识别开头的代码围栏，会导致后方的闭合 ``` 被误判为开头，
 *       把正文讲解/公式全部吞进代码框，代码反而裸露在外。
 * 2. ```lang 与首行代码粘连（如 ```cfor 或 ```pythondef）
 *    -> 拆开语言声明与代码文本，确保语法高亮与代码独立换行。
 * 3. 未声明语言但紧贴代码（如 ```for 或 ```i=1）
 *    -> 拆开围栏与首行代码。
 * 4. 闭合 ``` 后面粘连中文小标题/正文（如 ```思路：或 ```解析：）
 *    -> 确保代码块闭合后换行。
 */
export function normalizeCodeFences(raw: string): string {
  if (!raw) return '';
  let s = raw;

  // 1. 如果 ``` 或 ~~~ 紧随表格行（以 | 结尾），必须用空行分隔，避免被表格误吞为最后一列单元格
  s = s.replace(/(\|[ \t]*)\n*(```+|~~~+)/g, '$1\n\n$2');

  // 2. ``` 或 ~~~ 前面紧贴非换行字符，强制换行（仅行内空白，不吞多行换行）
  s = s.replace(/([^\n\r])[ \t]*(```+|~~~+)/g, '$1\n$2');

  // 3. ```lang 后面在同一行直接粘连代码文本（如 ```cfor 或 ```pythondef）
  const knownLangsPattern = KNOWN_CODE_LANGS.join('|');
  const langGluedRegex = new RegExp(`(^|\\n)([\`~]{3,})(${knownLangsPattern})([^\\s\\n])`, 'gi');
  s = s.replace(langGluedRegex, '$1$2$3\n$4');

  // 4. ``` 后面没有任何已知语言声明直接紧贴代码（如 ```for 或 ```i=1）
  s = s.replace(/(^|\n)([\`~]{3,})(?![a-zA-Z0-9_#+-]+(\s|\n|$))([^\s\n]+)/g, '$1$2\n$3');

  // 5. 闭合代码块 ``` 后面直接紧贴中文或小标题（如 ```思路：外层 或 ```解析：）
  s = s.replace(/(^|\n)([\`~]{3,})([ \t]*)([\u4e00-\u9fa5【「『（\(（])/g, '$1$2\n$4');

  return s;
}

mermaid.initialize({
  startOnLoad: false,
  // Mermaid 12 默认会把错误 SVG 插入 document.body；开启后失败时清理并抛错，避免污染全站页面
  suppressErrorRendering: true,
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
    useMaxWidth: false,
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

export function looksLikeMermaid(code: string): boolean {
  const trimmed = code.trim();
  if (MERMAID_HINT.test(trimmed)) return true;
  if (/^\s*(?:graph|flowchart)\s+(?:TD|TB|BT|RL|LR)\b/im.test(trimmed)) return true;
  if (/^\s*subgraph\b/im.test(trimmed)) return true;
  const hasArrow = /(?:-->|==>|-\.->)/.test(trimmed);
  const nodeDefs = (trimmed.match(/\b[A-Za-z][\w-]*\s*\[[^\]]+\]/g) || []).length;
  return hasArrow && nodeDefs >= 2;
}

function isMermaidErrorSvg(svg: string): boolean {
  if (!svg?.trim()) return true;
  return /Syntax error in text|aria-roledescription=['"]error['"]|class=['"]error-icon/i.test(svg);
}

function cleanupMermaidTempDom(id: string) {
  if (typeof document === 'undefined') return;
  document.getElementById(`d${id}`)?.remove();
  document.getElementById(`i${id}`)?.remove();
}

/** 清理 mermaid.render 遗留在 body 上的临时节点（会导致任意页面出现炸弹图标） */
export function cleanupOrphanMermaidDom() {
  if (typeof document === 'undefined') return;
  document.querySelectorAll<HTMLElement>('body > div[id^="dmermaid-"]').forEach((el) => el.remove());
  document.querySelectorAll<HTMLElement>('body > iframe[id^="imermaid-"]').forEach((el) => el.remove());
}

async function renderMermaidSvg(id: string, code: string) {
  try {
    const result = await mermaid.render(id, code);
    if (isMermaidErrorSvg(result.svg)) {
      throw new Error('Mermaid syntax error');
    }
    return result;
  } finally {
    cleanupMermaidTempDom(id);
  }
}

function quoteMermaidLabel(label: string): string {
  let trimmed = label.trim();
  if (trimmed.length > 44) {
    trimmed = `${trimmed.slice(0, 43)}…`;
  }
  if (!trimmed) return '""';
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) return trimmed;
  return `"${trimmed.replace(/"/g, '\\"')}"`;
}

function normalizeMermaidNodeLine(line: string, anonymousSubgraphSeq: { value: number }): string {
  if (/^\s*(?:%%|classDef|class |style |end\b)/i.test(line)) return line;

  if (/^\s*subgraph\b/i.test(line)) {
    const withId = line.replace(
      /subgraph\s+([A-Za-z][\w-]*)\s*\[([^\]]+)\]/i,
      (_m, id: string, label: string) => `subgraph ${id}[${quoteMermaidLabel(label)}]`
    );
    if (withId !== line) return withId;
    return line.replace(/subgraph\s+\[([^\]]+)\]/i, (_m, label: string) => {
      anonymousSubgraphSeq.value += 1;
      return `subgraph sg${anonymousSubgraphSeq.value}[${quoteMermaidLabel(label)}]`;
    });
  }

  return line
    .replace(/\b([A-Za-z][\w-]*)\s+\[([^\]]+)\]/g, (_m, nodeId: string, label: string) => {
      return `${nodeId}[${quoteMermaidLabel(label)}]`;
    })
    .replace(/\b([A-Za-z][\w-]*)\[([^\]]+)\]/g, (_m, nodeId: string, label: string) => {
      const trimmed = label.trim();
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        return `${nodeId}[${trimmed}]`;
      }
      if (/[\u4e00-\u9fa5\s()（）]/.test(trimmed)) {
        return `${nodeId}[${quoteMermaidLabel(trimmed)}]`;
      }
      return `${nodeId}[${trimmed}]`;
    });
}

/** 模型常只列出节点不写连线，Mermaid 12 无法解析；在 subgraph 块内自动串联 */
function autoLinkMermaidNodes(text: string): string {
  const lines = text.split('\n');
  const out: string[] = [];
  let pending: string[] = [];
  const nodeLineRe = /^([A-Za-z][\w-]*)(\[.*\])?$/;

  const flush = () => {
    if (pending.length === 0) return;
    if (pending.length === 1) {
      out.push(pending[0]);
    } else if (pending.length <= 6) {
      for (let i = 0; i < pending.length - 1; i++) {
        out.push(`${pending[i]} --> ${pending[i + 1]}`);
      }
    } else {
      // 大量孤立节点：以首个为锚点放射连接，避免 LR 下排成超长横条
      const hub = pending[0];
      out.push(hub);
      for (let i = 1; i < pending.length; i++) {
        out.push(`${hub} --> ${pending[i]}`);
      }
    }
    pending = [];
  };

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) {
      flush();
      out.push(line);
      continue;
    }
    if (
      /^(flowchart|graph)\s/i.test(trimmed)
      || /^subgraph\b/i.test(trimmed)
      || /^end\s*$/i.test(trimmed)
      || /^%%/.test(trimmed)
      || /^classDef\b/i.test(trimmed)
      || /^class\b/i.test(trimmed)
      || /^style\b/i.test(trimmed)
    ) {
      flush();
      out.push(line);
      continue;
    }
    if (/-->|==>|-\.-?>/.test(trimmed)) {
      flush();
      out.push(line);
      continue;
    }
    if (nodeLineRe.test(trimmed)) {
      pending.push(trimmed);
      continue;
    }
    flush();
    out.push(line);
  }
  flush();
  return out.join('\n');
}

/** 课程知识图谱节点较多时，LR 易被压成「一字长蛇」；统一改为纵向 TD */
function preferVerticalCourseFlowchart(text: string): string {
  const nodeCount = (text.match(/\[[^\]]+\]/g) || []).length;
  const edgeCount = (text.match(/-->/g) || []).length;
  const isWideLayout = /^flowchart\s+(?:LR|RL)\b/im.test(text) || /^graph\s+(?:LR|RL)\b/im.test(text);

  if (isWideLayout && (nodeCount > 6 || edgeCount > 8)) {
    return text
      .replace(/^flowchart\s+(?:LR|RL)\b/im, 'flowchart TD')
      .replace(/^graph\s+(?:LR|RL)\b/im, 'flowchart TD');
  }

  return text;
}

let mermaidSubgraphRepairSeq = 0;

/** 修复模型常输出的粘连语法：flowchart TDsubgraph基础层、-.推广.-> 等 */
function repairMermaidGluedSyntax(text: string): string {
  mermaidSubgraphRepairSeq = 0;
  let s = text.replace(/\r\n/g, '\n');

  s = s.replace(/\b((?:flowchart|graph)\s+(?:TD|TB|BT|RL|LR))(\s*)(subgraph\b)/gi, '$1\n$3 ');
  s = s.replace(/\bend(\s*)(subgraph\b)/gi, 'end\n$2 ');

  s = s.replace(/\bsubgraph([\u4e00-\u9fa5][\u4e00-\u9fa5A-Za-z0-9_]*)(?=[ \t]|$)/g, (_m, title: string) => {
    mermaidSubgraphRepairSeq += 1;
    return `subgraph SG${mermaidSubgraphRepairSeq}["${title}"]`;
  });

  s = s
    .split('\n')
    .map((line) => {
      const onlyTitle = line.match(/^(\s*)subgraph\s+([\u4e00-\u9fa5][^\[\n"]+)\s*$/);
      if (onlyTitle) {
        mermaidSubgraphRepairSeq += 1;
        return `${onlyTitle[1]}subgraph SG${mermaidSubgraphRepairSeq}["${onlyTitle[2].trim()}"]`;
      }
      return line;
    })
    .join('\n');

  s = s.replace(/(subgraph\s+SG\d+\[[^\]]+\])\s+([A-Za-z_])/g, '$1\n  $2');
  s = s.replace(/(subgraph\s+"[^"]+")\s+([A-Za-z_])/g, '$1\n  $2');

  s = s.replace(/-\.([^.\n>-]+)\.-?>/g, '-.->|$1|');
  s = s.replace(/([^\s])-->/g, '$1 -->');
  s = s.replace(/-->([^\s|\[])/g, '--> $1');
  s = s.replace(/([^\n])\s+(subgraph\s+)/gi, '$1\n$2');
  s = s.replace(/([^\n])\s+\b(end\b)\s*$/gim, '$1\n$2');

  return s;
}

/** 去掉行首中文说明、注释化纯文案行、修正链式边与边标签写法 */
function sanitizeMermaidContentLines(text: string): string {
  const out: string[] = [];
  for (const line of text.split('\n')) {
    let s = line.trimEnd();
    const trimmed = s.trim();
    if (!trimmed) {
      out.push(s);
      continue;
    }

    if (/^(flowchart|graph)\s/i.test(trimmed) || /^%%/.test(trimmed)) {
      out.push(s);
      continue;
    }
    if (/^(subgraph|end|classDef|class|style)\b/i.test(trimmed)) {
      out.push(s);
      continue;
    }

    s = s.replace(/--\|([^|\n]+)\|>/g, '-->|$1|');
    s = s.replace(/==\|([^|\n]+)\|>/g, '==>|$1|');

    const nodeStart = trimmed.search(/\b[A-Za-z][\w-]*(\s*\[|-->|-\.->|$)/);
    if (nodeStart > 0 && /[\u4e00-\u9fa5（）、：；。]/.test(trimmed.slice(0, nodeStart))) {
      s = trimmed.slice(nodeStart);
    }

    if (!/\b[A-Za-z][\w-]*(\[|-->|-\.->)/.test(s.trim()) && /[\u4e00-\u9fa5]/.test(s)) {
      out.push(`%% ${s.trim()}`);
      continue;
    }

    const chainParts = s.trim().split(/\s*-->\s*/);
    if (chainParts.length > 2) {
      for (let i = 0; i < chainParts.length - 1; i++) {
        out.push(`${chainParts[i].trim()} --> ${chainParts[i + 1].trim()}`);
      }
      continue;
    }

    out.push(s);
  }
  return out.join('\n');
}

function repairMermaidAggressive(code: string): string {
  const normalized = normalizeMermaidCode(code);
  const filtered = normalized
    .split('\n')
    .filter((line) => {
      const tr = line.trim();
      if (!tr) return true;
      if (/^(flowchart|graph|subgraph|end|%%|classDef|class|style)/i.test(tr)) return true;
      return /\b[A-Za-z][\w-]*(\[|-->|==>|-\.->)/.test(tr);
    })
    .join('\n');
  return normalizeMermaidCode(filtered);
}

export function normalizeMermaidCode(raw: string): string {
  let text = repairMermaidGluedSyntax(raw.trim().replace(/\r\n/g, '\n'));
  if (!text) return text;

  text = text.replace(
    /^(graph\s+(?:TD|TB|BT|RL|LR)|flowchart\s+(?:TD|TB|BT|RL|LR))\s+(?=\S)/im,
    (header) => `${header.trim()}\n`
  );
  text = text.replace(/\bend\s+subgraph\b/gi, 'end\nsubgraph');
  text = text.replace(/([^\n])\s+(subgraph\s+)/gi, '$1\n$2');
  text = text.replace(/([^\n])\s+end\s*$/gim, '$1\nend');

  if (!MERMAID_HINT.test(text)) {
    text = `flowchart TD\n${text}`;
  }

  text = text.replace(/^graph\s+(TD|TB|BT|RL|LR)\b/im, 'flowchart $1');

  const anonymousSubgraphSeq = { value: 0 };
  text = text
    .split('\n')
    .map((line) => normalizeMermaidNodeLine(line, anonymousSubgraphSeq))
    .join('\n');

  text = sanitizeMermaidContentLines(text);
  text = autoLinkMermaidNodes(text);
  text = preferVerticalCourseFlowchart(text);

  // 模型常漏写 subgraph 的 end，导致永远无法通过完整性校验
  const subgraphCount = (text.match(/\bsubgraph\b/gi) || []).length;
  const endCount = (text.match(/^\s*end\s*$/gim) || []).length;
  if (subgraphCount > endCount) {
    text += '\n' + 'end\n'.repeat(subgraphCount - endCount);
  }

  return text;
}

function readMermaidSource(wrapper: HTMLElement): string {
  const sourceEl = wrapper.querySelector<HTMLElement>('.mermaid-source');
  if (sourceEl?.textContent?.trim()) {
    return sourceEl.textContent.trim();
  }
  const diagram = wrapper.querySelector<HTMLElement>('.mermaid-diagram');
  const legacy = diagram?.dataset.code;
  if (legacy) {
    try {
      return decodeURIComponent(legacy).trim();
    } catch {
      return legacy.trim();
    }
  }
  return '';
}

function sanitizeMermaidFallbackLabel(label: string): string | null {
  const t = label.replace(/```+/g, '').replace(/^text/i, '').trim();
  if (!t || t.length < 2) return null;
  if (/^`+$/.test(t)) return null;
  return t;
}

function buildPlainHierarchyFallback(rawCode: string): string | null {
  const lines = rawCode
    .split('\n')
    .map((l) => l.replace(/```+/g, '').trim())
    .filter((l) => l && !/^text$/i.test(l));
  if (lines.length < 2 || lines.length > 24) return null;
  if (/(?:^|\n)\s*(?:graph|flowchart)\s/im.test(rawCode)) return null;
  if (/(?:-->|==>)/.test(rawCode)) return null;
  const items = lines
    .map((l) => l.replace(/^[-*•]\s*/, ''))
    .map((l) => sanitizeMermaidFallbackLabel(l))
    .filter((l): l is string => Boolean(l));
  if (items.length < 2) return null;
  const lis = items.map((l) => `<li>${md.utils.escapeHtml(l)}</li>`).join('');
  return `<div class="mermaid-outline-fallback"><p class="mermaid-outline-title">层级结构概要（流程图渲染失败时的可读版）</p><ul>${lis}</ul></div>`;
}

function buildMermaidOutlineFallback(rawCode: string): string {
  const plain = buildPlainHierarchyFallback(rawCode);
  if (plain) return plain;

  const labels: string[] = [];
  const seen = new Set<string>();
  const re = /\["((?:\\.|[^"\\])*)"\]|\[([^\]"]+)\]/g;
  let match: RegExpExecArray | null;
  while ((match = re.exec(rawCode)) !== null) {
    const label = sanitizeMermaidFallbackLabel((match[1] ?? match[2] ?? '').replace(/\\"/g, '"'));
    if (!label || seen.has(label)) continue;
    seen.add(label);
    labels.push(label);
  }
  if (labels.length === 0) {
    return `<div class="mermaid-fallback-box"><pre><code>${md.utils.escapeHtml(rawCode)}</code></pre></div>`;
  }
  const items = labels.map((l) => `<li>${md.utils.escapeHtml(l)}</li>`).join('');
  return `<div class="mermaid-outline-fallback"><p class="mermaid-outline-title">图谱结构概要（流程图渲染失败时的可读版）</p><ul>${items}</ul></div>`;
}

function looksLikePlainHierarchyDiagram(code: string): boolean {
  const t = code.trim();
  if (!t || /(?:^|\n)\s*(?:graph|flowchart)\s/im.test(t)) return false;
  if (/(?:-->|==>)/.test(t)) return false;
  // ASCII 框线图、表格或流程框绝不属于平铺层级结构（应保留等宽代码框）
  if (/\+[-=]{3,}\+/.test(t) || /^[ \t]*\|.*\|[ \t]*$/m.test(t)) return false;
  if (/[┌├└│─┼]/.test(t)) return false;

  const lines = t.split('\n').map((l) => l.trim()).filter(Boolean);
  if (lines.length < 2 || lines.length > 24) return false;
  // 必须具有子级列表缩进或连字符特征
  const hasIndentOrBullet = lines.some((l) => /^[-*•]\s+/.test(l) || /^[ \t]{2,}/.test(l));
  if (!hasIndentOrBullet) return false;

  const joined = lines.join(' ');
  return /(?:JDK|JRE|JVM|字节码)/.test(joined);
}

function renderPlainHierarchyBlock(code: string): string {
  const html = buildPlainHierarchyFallback(code);
  if (html) return html;
  const escaped = md.utils.escapeHtml(code.trim());
  return `<div class="mermaid-outline-fallback"><p class="mermaid-outline-title">层级结构</p><pre class="hierarchy-plain-pre">${escaped}</pre></div>`;
}

const MERMAID_ZOOM_BTN =
  '<button type="button" class="mermaid-btn mermaid-btn--zoom" disabled title="图谱渲染完成后可放大" aria-label="全屏放大查看" onclick="window.__openMermaidViewer && window.__openMermaidViewer(this)">' +
  '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">' +
  '<path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg></button>';

function buildMermaidWrapper(code: string): string {
  const trimmed = code.trim();
  const escaped = md.utils.escapeHtml(trimmed);
  const safeCode = encodeURIComponent(trimmed);
  return [
    '<div class="mermaid-diagram-wrapper">',
    '<div class="mermaid-header">',
    '<span class="mermaid-header-title">课程拓扑知识图谱</span>',
    `<div class="mermaid-toolbar">${MERMAID_ZOOM_BTN}</div>`,
    '</div>',
    `<pre class="mermaid-source" hidden aria-hidden="true">${escaped}</pre>`,
    `<div class="mermaid-diagram" data-mermaid-code="${safeCode}"><span class="mermaid-loading">图谱渲染中…</span></div>`,
    '</div>'
  ].join('');
}

function inferOrphanCodeLang(code: string): string {
  if (looksLikeAsciiKnowledgeTree(code)) return 'ascii-tree';
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
  if (decl === 'mermaid' || decl === 'graph' || decl === 'flowchart') return 'mermaid';
  if (!decl && looksLikeMermaid(text)) return 'mermaid';
  if (decl === 'json' || (!decl && /^\s*[[{]/.test(text) && /"[\w_]+"\s*:/.test(text))) return 'json';
  if (decl && hljs.getLanguage(decl)) return decl;
  return inferOrphanCodeLang(text);
}

let mermaidCounter = 0;
const mermaidFailureLogged = new Set<string>();

function getStoredPreference<T>(key: string, defaultValue: T): T {
  try {
    const raw = typeof localStorage !== 'undefined' ? localStorage.getItem('edumind_user_preferences') : null;
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed[key] !== undefined) return parsed[key];
    }
  } catch {}
  return defaultValue;
}

function renderAsciiTreeBlock(code: string): string {
  const expanded = expandAsciiTreeToMultiline(code);
  const escaped = md.utils.escapeHtml(expanded);
  const copyBtn = `<button type="button" class="code-copy-btn" title="复制结构">复制</button>`;
  const langLabel = `<span class="code-lang">知识图谱</span>`;
  return [
    '<div class="code-block-wrapper ascii-tree-wrapper">',
    `<div class="code-header">${langLabel}${copyBtn}</div>`,
    `<pre class="ascii-tree-pre"><code>${escaped}</code></pre>`,
    '</div>'
  ].join('');
}

function renderCodeBlock(code: string, declaredLang: string, enableMermaid = true): string {
  if (!code?.trim()) return '';

  const mermaidAllowed = enableMermaid && getStoredPreference('mermaidEnabled', true);
  let lang = inferCodeFenceLanguage(code, declaredLang);
  if (!mermaidAllowed && lang === 'mermaid') {
    lang = 'text';
  }

  const decl = normalizeDeclaredLang(declaredLang);
  if (
    (decl === '' || decl === 'text' || decl === 'plain' || decl === 'txt') &&
    looksLikePlainHierarchyDiagram(code)
  ) {
    return renderPlainHierarchyBlock(code);
  }

  if (looksLikeAsciiKnowledgeTree(code)) {
    const mermaidSource = asciiTreeToMermaid(code);
    if (mermaidSource && mermaidAllowed) {
      return buildMermaidWrapper(mermaidSource);
    }
    return renderAsciiTreeBlock(code);
  }

  if (lang === 'mermaid' && mermaidAllowed) {
    if (looksLikePlainHierarchyDiagram(code) && !looksLikeMermaid(code)) {
      return renderPlainHierarchyBlock(code);
    }
    return buildMermaidWrapper(code);
  }

  const highlightAllowed = getStoredPreference('codeHighlightEnabled', true);
  let highlighted = '';
  if (highlightAllowed && lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value;
    } catch {
      highlighted = md.utils.escapeHtml(code);
    }
  } else if (highlightAllowed) {
    try {
      highlighted = hljs.highlightAuto(code).value;
    } catch {
      highlighted = md.utils.escapeHtml(code);
    }
  } else {
    highlighted = md.utils.escapeHtml(code);
  }

  const langLabel = `<span class="code-lang">${formatLangLabel(lang)}</span>`;
  const copyBtn = `<button type="button" class="code-copy-btn" title="复制代码">复制</button>`;

  return `<div class="code-block-wrapper"><div class="code-header">${langLabel}${copyBtn}</div><pre class="hljs"><code class="language-${lang || 'text'}">${highlighted}</code></pre></div>`;
}

md.renderer.rules.fence = (tokens, idx, _options, env) => {
  const token = tokens[idx];
  const info = token.info ? md.utils.unescapeAll(token.info).trim() : '';
  const lang = info.split(/\s+/g)[0] || '';
  const enableMermaid = env?.enableMermaid !== false;
  return renderCodeBlock(token.content, lang, enableMermaid);
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
  return renderMarkdownPipeline(content, true, true);
}

/** 深度思考区：不走表格容错，且不渲染 Mermaid 图谱（仅正文渲染） */
export function renderMarkdownForReasoning(content: string): string {
  return renderMarkdownPipeline(content, false, false);
}

function renderKatexHtml(formula: string, displayMode: boolean): string {
  if (!getStoredPreference('katexEnabled', true)) {
    return displayMode
      ? `<pre class="math-raw">${md.utils.escapeHtml(formula)}</pre>`
      : `<code class="math-raw">${md.utils.escapeHtml(formula)}</code>`;
  }
  const fixed = repairLatexDoubleEscapes(repairMalformedLeftRight(formula.trim()));
  const html = katex.renderToString(fixed, { displayMode, throwOnError: false, strict: 'ignore' });
  if (!html.includes('katex-error')) return html;

  const fallback = fixed
    .replace(/\\left[\[\({|.]/g, (m) => (m.endsWith('[') ? '[' : m.endsWith('(') ? '(' : m.endsWith('{') ? '{' : ''))
    .replace(/\\right[\]\)}.|]/g, (m) => (m.endsWith(']') ? ']' : m.endsWith(')') ? ')' : m.endsWith('}') ? '}' : ''));
  const retry = katex.renderToString(fallback, { displayMode, throwOnError: false, strict: 'ignore' });
  return retry.includes('katex-error') ? html : retry;
}

function renderMarkdownPipeline(content: string, normalizeTables: boolean, enableMermaid = true): string {
  if (!content?.trim()) return '';

  const fenced = normalizeCodeFences(content);
  let processed = normalizeTables
    ? normalizeLatexDelimiters(normalizeChatTables(fenced))
    : normalizeLatexDelimiters(fenced);

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

  let renderedHtml = md.render(processed, { enableMermaid });

  blockKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replace(`<p>${key}</p>`, htmlVal).replaceAll(key, htmlVal);
  });
  inlineKatexMap.forEach((htmlVal, key) => {
    renderedHtml = renderedHtml.replaceAll(key, htmlVal);
  });

  return unwrapInlineCodeInTables(renderedHtml);
}

export async function renderMermaidInElement(root: HTMLElement | null) {
  if (!root || typeof document === 'undefined') return;

  cleanupOrphanMermaidDom();

  const wrappers = root.querySelectorAll<HTMLElement>(
    '.mermaid-diagram-wrapper:not([data-mermaid-done="true"])'
  );

  for (const wrapper of Array.from(wrappers)) {
    if (wrapper.dataset.mermaidRendering === '1') continue;

    // 深度思考区、流式输出中：不渲染图谱（避免半截语法反复失败并污染 body）
    if (
      wrapper.closest('.reasoning-card')
      || wrapper.closest('.is-streaming-bubble')
      || wrapper.closest('.is-streaming')
    ) {
      continue;
    }

    const el = wrapper.querySelector<HTMLElement>('.mermaid-diagram');
    if (!el) {
      wrapper.setAttribute('data-mermaid-done', 'true');
      continue;
    }

    const rawCode = readMermaidSource(wrapper);
    if (!rawCode) {
      el.innerHTML = '<span class="mermaid-error">图谱内容为空</span>';
      wrapper.setAttribute('data-mermaid-done', 'true');
      continue;
    }

    wrapper.dataset.mermaidRendering = '1';
    const codeToRender = normalizeMermaidCode(rawCode);
    const id = `mermaid-graph-${Date.now()}-${mermaidCounter++}`;

    try {
      const { svg, bindFunctions } = await renderMermaidSvg(id, codeToRender);
      el.innerHTML = svg;
      bindFunctions?.(el);
      wrapper.setAttribute('data-mermaid-done', 'true');
      const zoomBtn = wrapper.querySelector<HTMLButtonElement>('.mermaid-btn--zoom');
      if (zoomBtn) {
        zoomBtn.disabled = false;
        zoomBtn.title = '全屏放大查看';
      }
      const svgEl = el.querySelector('svg');
      if (svgEl?.viewBox?.baseVal?.height) {
        const h = svgEl.viewBox.baseVal.height;
        el.style.minHeight = `${Math.min(Math.max(h * 0.35, 120), 360)}px`;
      }
    } catch (firstErr) {
      const simplified = codeToRender
        .split('\n')
        .filter((line) => !/^\s*subgraph\b/i.test(line) && !/^\s*end\s*$/i.test(line))
        .join('\n');
      try {
        const retryId = `${id}-retry`;
        const { svg, bindFunctions } = await renderMermaidSvg(retryId, simplified);
        el.innerHTML = svg;
        bindFunctions?.(el);
        wrapper.setAttribute('data-mermaid-done', 'true');
        const zoomBtn = wrapper.querySelector<HTMLButtonElement>('.mermaid-btn--zoom');
        if (zoomBtn) zoomBtn.disabled = false;
      } catch {
        try {
          const aggressive = repairMermaidAggressive(rawCode);
          const retryId2 = `${id}-retry2`;
          const { svg, bindFunctions } = await renderMermaidSvg(retryId2, aggressive);
          el.innerHTML = svg;
          bindFunctions?.(el);
          wrapper.setAttribute('data-mermaid-done', 'true');
          const zoomBtn = wrapper.querySelector<HTMLButtonElement>('.mermaid-btn--zoom');
          if (zoomBtn) zoomBtn.disabled = false;
        } catch {
          const logKey = codeToRender.slice(0, 160);
          if (!mermaidFailureLogged.has(logKey)) {
            mermaidFailureLogged.add(logKey);
            console.warn('[Mermaid] render failed:', firstErr);
            console.warn('[Mermaid] normalized snippet:', codeToRender.slice(0, 400));
          }
          el.innerHTML = buildMermaidOutlineFallback(rawCode);
          wrapper.setAttribute('data-mermaid-done', 'true');
        }
      }
    } finally {
      delete wrapper.dataset.mermaidRendering;
    }
  }

  cleanupOrphanMermaidDom();
}

export function bindMarkdownCodeCopy(
  root: HTMLElement | null,
  options?: { renderMermaid?: boolean }
) {
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

  if (options?.renderMermaid !== false) {
    void renderMermaidInElement(root);
  }
}

// 全局委托监听：无论组件是否处于流式高频重绘，点击复制按钮均能可靠触发
if (typeof document !== 'undefined') {
  document.addEventListener('click', async (e) => {
    const btn = (e.target as HTMLElement)?.closest<HTMLButtonElement>('.code-copy-btn');
    if (!btn) return;
    e.preventDefault();
    e.stopPropagation();
    const code = btn.closest('.code-block-wrapper')?.querySelector('code')?.textContent || '';
    if (!code) return;
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
}
