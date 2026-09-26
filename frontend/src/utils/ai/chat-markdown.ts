import {
  renderMarkdownForChat,
  renderMarkdownForReasoning,
  normalizeCodeFences,
  looksLikeMermaid
} from '@/utils/markdown';
import { cleanReasoningText } from './copilot-stream-split';
import { expandAsciiTreeToMultiline, looksLikeAsciiKnowledgeTree } from './ascii-tree-graph';

const FENCED_CODE_BLOCK_RE = /(```[\s\S]*?```)/g;

function looksLikeProseNotCode(inner: string): boolean {
  const t = inner.trim();
  if (!t) return false;

  // 1. ASCII 字符画、流程图、框线图、表格结构保护：含有 +---+、框线字符或 |...| 管道结构者绝非散文
  if (/\+[-=]{3,}\+/.test(t) || /^[ \t]*\+[-=]+[ \t]*$/m.test(t)) return false;
  if (/^[ \t]*[┌├└│─┼]/m.test(t)) return false;
  const linesWithPipes = t.split('\n').filter((l) => /^[ \t]*\|.*\|[ \t]*$/.test(l));
  if (linesWithPipes.length >= 2) return false;

  const cn = (t.match(/[\u4e00-\u9fa5]/g) || []).length;
  // 2. 仅当内容明确具有大段 Markdown 章节标题、带加粗列表等散文排版结构时，才判定为被误套围栏的讲义
  if (/^#{1,6}\s/m.test(t) && cn > 15) return true;
  if (/^\s*[-*]\s+\*\*/m.test(t) && cn > 30) return true;
  if (/^#{1,3}\s+[\d一二三四五六七八九十]/m.test(t)) return true;
  return false;
}

/** 模型常把整段 Markdown 讲义包在 ``` / ```markdown 里，渲染会变成 CODE 黑框 */
function unwrapProseCodeFences(text: string): string {
  return text.replace(/```([a-zA-Z0-9+#-]*)\s*\n([\s\S]*?)```/g, (full, lang: string, inner: string) => {
    const langLower = (lang || '').trim().toLowerCase();
    if (langLower && !['markdown', 'md', 'text', 'txt'].includes(langLower)) {
      return full;
    }
    // 未标注语言的通用围栏默认是代码/ASCII图，仅当内部以 # 标题开头时才考虑解包
    if (!langLower && !/^#{1,6}\s/m.test(inner.trim())) {
      return full;
    }
    if (looksLikeProseNotCode(inner)) {
      return inner.trim();
    }
    return full;
  });
}

/**
 * 大模型常把「1.知识地图…2.概念辨析…3.定理…」挤在同一行；
 * Markdown 无法识别，需拆行并在序号后补空格。
 * 必须严格保护带加粗的序号（如「**1. 异常体系**」），防止换行符插入 ** 与数字之间导致加粗断裂。
 */
function isMarkdownTableRowLine(line: string): boolean {
  const trimmed = line.trim();
  return trimmed.startsWith('|') && (trimmed.endsWith('|') || trimmed.includes('|', 1));
}

function hasUnclosedBrackets(text: string): boolean {
  let round = 0;
  let square = 0;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === '(' || ch === '（') round++;
    else if ((ch === ')' || ch === '）') && round > 0) round--;
    else if (ch === '[' || ch === '【') square++;
    else if ((ch === ']' || ch === '】') && square > 0) square--;
  }
  return round > 0 || square > 0;
}

/**
 * 保护数学公式（$$...$$、\[...\]、\(...\)、$...$），防止公式内部的减号（如 (1+x)-x）或小数点被列表拆行器误切
 */
function protectMathBlocks(text: string): { masked: string; unmask: (s: string) => string } {
  const placeholders: string[] = [];
  const masked = text.replace(
    /(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\(.+?\\\)|\$(?!\$)[^$\n]+?\$(?!\$))/g,
    (m) => {
      const key = `%%%CHAT_MD_MATH_${placeholders.length}%%%`;
      placeholders.push(m);
      return key;
    }
  );
  return {
    masked,
    unmask: (s: string) =>
      s.replace(/%%%CHAT_MD_MATH_(\d+)%%%/g, (_m, idx) => placeholders[Number(idx)] ?? _m)
  };
}

/**
 * 修复模型在行内公式中漏写闭合 $ 符号便直接开始写中文标点或说明文字的问题
 * 例如：「求 $\displaystyle \lim_{x\to0}\frac{...}{...}。提示：...」
 * 在中文标点（。；！？）之前自动补齐闭合 $，并修复紧跟中文说明后的孤立结尾 $
 */
function repairUnclosedMathBeforeChinesePunct(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      // 先保护合法的 $$...$$ 独立数学块，绝不在 $$ 块内部乱插 $
      const blockMathList: string[] = [];
      let s = segment.replace(/\$\$[\s\S]+?\$\$/g, (m) => {
        const key = `%%%BLOCK_MATH_UNCLOSED_${blockMathList.length}%%%`;
        blockMathList.push(m);
        return key;
      });

      // 1. 匹配 $ 开头且含有 LaTeX 指令（如 \lim, \frac 等），但在遇到中文句末标点时未闭合 $ 的情况
      s = s.replace(
        /(?<![\$\\a-zA-Z0-9])(\$(?!\$)(?:[^\$\n]*?\\[a-zA-Z]+[^\$\n]*?))([。；;!?！？])/g,
        (match, mathPart: string, punc: string) => {
          const dollars = (mathPart.match(/(?<!\\)\$/g) || []).length;
          if (dollars % 2 === 0) return match;
          return `${mathPart}$${punc}`;
        }
      );

      // 2. 补齐紧随中文说明（如「结果为」）后的孤立闭合 $ 对应的开启 $（排除 $$）
      s = s.replace(
        /([\u4e00-\u9fa5][：:]?)[ \t]*([+\\-]?\\(?:frac|sqrt|[a-zA-Z]+)[^$\n]*?)(?<!\$)\$(?!\$)/g,
        (_match, prefix, expr) => `${prefix} $${expr.trim()}$`
      );

      // 还原 $$...$$ 块
      blockMathList.forEach((m, idx) => {
        s = s.replaceAll(`%%%BLOCK_MATH_UNCLOSED_${idx}%%%`, () => m);
      });

      return s;
    })
    .join('');
}

/**
 * 大模型常把「1.知识地图…2.概念辨析…3.定理…」挤在同一行；
 * Markdown 无法识别，需拆行并在序号后补空格。
 * 必须严格保护表格行、标题行、未闭合括号及带加粗的序号，防止截断正文。
 */
function normalizeInlineNumberedLists(text: string): string {
  const { masked, unmask } = protectMathBlocks(text);
  const result = masked
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        // 表格行与标题行必须完整保护
        if (isMarkdownTableRowLine(line)) return line;
        if (/^#{1,6}\s/.test(line.trim())) return line;

        let s = line;

        // 1. 若同一行前文粘连了带加粗的序号小标题（不在未闭合括号内）
        s = s.replace(
          /([\p{Extended_Pictographic}\u4e00-\u9fa5a-zA-Z0-9。；;!?！？:：）)”"』」】])[ \t]*\*\*(\d{1,2})\.[ \t]*/gu,
          (match, char, num, offset, fullStr) => {
            const before = fullStr.slice(0, offset + char.length);
            if (hasUnclosedBrackets(before)) return match;
            return `${char}\n\n**${num}. `;
          }
        );

        // 2. 确保已在行首的加粗序号内部有标准空格（如「**1.知识体系**」->「**1. 知识体系**」），绝不拆开 ** 与数字
        s = s.replace(
          /(^|\n)([ \t]*)\*\*(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z$\\])/gu,
          '$1$2**$3. '
        );

        // 3. 普通无加粗序号拆行：仅在标点符号、中文后粘连且不在未闭合括号内时拆行（支持公式 $ 与 \ 前缀）
        s = s.replace(
          /([\p{Extended_Pictographic}\u4e00-\u9fa5。；;!?！？:：）)”"』」】])[ \t]*(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z$\\]|\*\*)/gu,
          (match, char, num, offset, fullStr) => {
            const before = fullStr.slice(0, offset + char.length);
            if (hasUnclosedBrackets(before)) return match;
            return `${char}\n\n${num}. `;
          }
        );

        // 4. 行首普通序号后补齐标准空格（支持公式 $ 与 \ 前缀）
        s = s.replace(
          /^([ \t]*)(\d{1,2})\.(?=[ \t]*[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z$\\])/gmu,
          '$1$2. '
        );

        return s;
      });

      return processed.join('\n');
    })
    .join('');

  return unmask(result);
}

/**
 * 同一行粘连的「-两个重要极限 … -连续判定 …」拆成 Markdown 无序列表。
 * 注意：必须严格排除副标题连接符（如「题目一（数列极限 - 四则运算）」）、人名间隔号（如「约翰·冯·诺依曼」）、
 * 标题行（# 开头）以及未闭合括号内部的内容，防止错误拆行断裂语义。
 */
function normalizeInlineBulletLists(text: string): string {
  const { masked, unmask } = protectMathBlocks(text);
  const result = masked
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        // 表格行与 Markdown 标题行绝不可在行内拆出列表
        if (isMarkdownTableRowLine(line)) return line;
        if (/^#{1,6}\s/.test(line.trim())) return line;

        let s = line;

        // 1. 将行首（包含缩进）的实心圆点 • 或 · 转换为标准 Markdown 无序列表项 -
        s = s.replace(/^([ \t]*)[•·]\s*/gmu, '$1- ');

        // 2. 行内实心圆点 • 转换为无序列表项（仅限明确的 bullet •，不包含中文人名/书名间隔号 ·，且不在未闭合括号内）
        s = s.replace(/([^\n])\s*•\s*(?=[\p{Extended_Pictographic}\*\*\u4e00-\u9fa5【「『（])/gu, (match, prefix, offset, fullStr) => {
          const before = fullStr.slice(0, offset + prefix.length);
          if (hasUnclosedBrackets(before)) return match;
          return `${prefix}\n- `;
        });

        // 3. 明确的列表引导标点（冒号、分号、句号、感叹号、问号、已闭合引号、中文括号）之后紧贴的 -项
        // 严格排除英文半角括号 )（在数学公式与英文表达式中 ) - x 属于减法或复合词，绝非列表）
        // 且仅对中文、加粗及 Emoji 放宽空格限制；英文单词/字母后必须有空格（防止误伤 -x 等变量）
        s = s.replace(
          /(?<!-)([。；;!?！？:：）”"』」】])[ \t]*-(?:[ \t]*(?=\*\*|[\p{Extended_Pictographic}\u4e00-\u9fa5【「『（])|[ \t]+(?=[A-Za-z]))/gu,
          (match, punc, offset, fullStr) => {
            const before = fullStr.slice(0, offset + punc.length);
            if (hasUnclosedBrackets(before)) return match;
            return `${punc}\n- `;
          }
        );

        // 4. 若当前行（或拆行后的列表行）以无序列表项开头（- 开头），则该行内后续粘连的 -列表项也允许拆行（不在未闭合括号内）
        s = s
          .split('\n')
          .map((subLine) => {
            if (/^[ \t]*-[ \t]*/.test(subLine)) {
              return subLine.replace(
                /(?<!-)([^\n\r\t -])[ \t]*-(?:[ \t]*(?=\*\*|[\p{Extended_Pictographic}\u4e00-\u9fff（(「『【])|[ \t]+(?=[A-Za-z]))/gu,
                (match, prevChar, offset, fullStr) => {
                  const before = fullStr.slice(0, offset + prevChar.length);
                  if (hasUnclosedBrackets(before)) return match;
                  // 保护常见的复合词或代码标识符，如 key-value、foo-bar
                  const afterHyphen = fullStr.slice(offset + match.length);
                  if (/[A-Za-z0-9]/.test(prevChar) && /^[A-Za-z0-9]/.test(afterHyphen)) return match;
                  return `${prevChar}\n- `;
                }
              );
            }
            return subLine;
          })
          .join('\n');

        // 4b. 若该行原本未以 - 开头，但在该行内拆出了连续的 - 列表项，且首段也是形如「xxx：yyy。」的并列结构（非冒号结尾的引导句），补全首项列表标记
        const splitSubLines = s.split('\n');
        if (
          splitSubLines.length > 1 &&
          !/^[ \t]*[-*•·]/.test(splitSubLines[0]) &&
          !/[:：]\s*$/.test(splitSubLines[0]) &&
          splitSubLines.slice(1).every((sub) => /^[ \t]*- /.test(sub)) &&
          /[:：]/.test(splitSubLines[0])
        ) {
          splitSubLines[0] = `- ${splitSubLines[0].trimStart()}`;
          s = splitSubLines.join('\n');
        }

        // 5. CommonMark 规范化：确保行首（允许带缩进空格）- 后面有空格，支持 Emoji、中英文、粗体
        s = s.replace(/^([ \t]*)-(?=[\p{Extended_Pictographic}\u4e00-\u9fa5*（(「『【A-Za-z0-9])/gmu, '$1- ');

        // 6. 列表项末尾粘连的交流互动引导句自动脱离列表形成独立自然段
        s = s.replace(
          /([\u4e00-\u9fa5。；;!?！？）)”"』」】])\s*(不妨(?:先聊|告诉|直接|说说)|那么[，, ]?我们|请问(?:你|您)|你现在(?:是|想)|你是(?:刚|想))/gu,
          '$1\n\n$2'
        );

        return s;
      });

      return processed.join('\n');
    })
    .join('');

  return unmask(result);
}

/** 聊天 / 课节预览共用：拆粘连列表并规范行首 - 与 •（与深度思考区同源） */
export function normalizeDisplayListMarkdown(text: string): string {
  if (!text?.trim()) return text || '';
  let out = normalizeInlineNumberedLists(text);
  out = normalizeInlineBulletLists(out);
  return out;
}

/** 代码块内的字符画知识树：拆行便于阅读与 Mermaid 转换 */
function normalizeAsciiTreeInCodeFences(text: string): string {
  return text.replace(/```[^\n]*\n([\s\S]*?)```/g, (full, body: string) => {
    if (!looksLikeAsciiKnowledgeTree(body)) return full;
    const expanded = expandAsciiTreeToMultiline(body);
    return full.replace(body, expanded);
  });
}

/** 围栏未声明 mermaid 但内容是流程图时，强制 ```mermaid（与侧边栏 AI 同源渲染） */
function normalizeMermaidInCodeFences(text: string): string {
  return text.replace(/```([^\n]*)\n([\s\S]*?)```/g, (full, langLine: string, body: string) => {
    const trimmedBody = body.trim();
    if (!looksLikeMermaid(trimmedBody)) return full;
    const lang = (langLine.trim().split(/\s+/)[0] || '').toLowerCase();
    if (lang === 'mermaid' || lang === 'graph' || lang === 'flowchart') return full;
    return `\`\`\`mermaid\n${trimmedBody}\n\`\`\``;
  });
}

/** 正文里裸露的字符树包进代码围栏，避免被 Markdown 渲染成 <p> 后逐字竖排 */
function wrapBareAsciiTreeBlocks(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const out: string[] = [];
      let i = 0;

      while (i < lines.length) {
        const line = lines[i];
        if (!/[├└]/.test(line)) {
          out.push(line);
          i += 1;
          continue;
        }

        const blockLines: string[] = [];
        while (i < lines.length) {
          const ln = lines[i];
          const trimmed = ln.trim();
          if (blockLines.length > 0 && trimmed === '') {
            i += 1;
            break;
          }
          if (
            blockLines.length === 0
            || /[├└│]/.test(ln)
            || /^[ \t│]/.test(ln)
            || (trimmed.startsWith('│') && blockLines.length > 0)
          ) {
            blockLines.push(ln);
            i += 1;
          } else {
            break;
          }
        }

        const joined = blockLines.join('\n').trim();
        if (joined && looksLikeAsciiKnowledgeTree(joined)) {
          out.push('```\n' + expandAsciiTreeToMultiline(joined) + '\n```');
        } else if (blockLines.length) {
          out.push(blockLines.join('\n'));
        }
      }

      return out.join('\n');
    })
    .join('');
}

/** 正文里粘连的字符树（非代码块）拆行 */
function normalizeInlineAsciiTree(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      if (!/[├└]/.test(segment)) return segment;
      return expandAsciiTreeToMultiline(segment);
    })
    .join('');
}

/** 粘连的中文关键词引号 "" 拆行（表格行内跳过） */
function normalizeQuotedKeywordLines(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        if (isMarkdownTableRowLine(line)) return line;
        return line.replace(/""/g, '"\n"');
      });
      return processed.join('\n');
    })
    .join('');
}

/**
 * 教学与问答高频小标题（如「**参考答案：**」「参考解析：」「【题目】」等）粘连自动拆行（表格行跳过）
 */
function normalizeTeachingHeaders(text: string): string {
  const HEADER_RE =
    /([^\n])\s*(\*\*【?(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】?\*\*|【(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】)/g;
  const HEADER_BEFORE_CIRCLED_RE =
    /(\*\*【?(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】?\*\*|【(?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|分析|解答|解题思路|解题步骤|思路点拨|考点精解|核心考点|考点|题目描述|题目|知识点|评分细则|评分标准|总结|小结|归纳)[:：]?】)[ \t]*([①②③④⑤⑥⑦⑧⑨⑩])/g;

  // 句末标点（。！？）后粘连的无粗体小标题（如「...前n项和。参考解析：」或「...解答。答案：」）
  const PLAIN_HEADER_AFTER_PUNCT_RE =
    /([。；;!?！？])[ \t]*((?:参考答案|标准答案|正确答案|答案|试题解析|题目解析|试题分析|参考解析|解析|解答|解题思路|解题步骤|思路点拨|考点精解|考点|题目)[:：])/g;

  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        if (isMarkdownTableRowLine(line)) return line;
        let s = line.replace(HEADER_RE, '$1\n\n$2');
        s = s.replace(HEADER_BEFORE_CIRCLED_RE, '$1\n$2');
        s = s.replace(PLAIN_HEADER_AFTER_PUNCT_RE, '$1\n\n$2');
        s = s.replace(/([。；;!?！？])[ \t]*([①②③④⑤⑥⑦⑧⑨⑩])/g, '$1\n$2');
        return s;
      });
      return processed.join('\n');
    })
    .join('');
}

/**
 * 8. 模型常输出用 ▼ 或 ↓ 串联的字符执行链路（例如「Hello.java | javac ▼Hello.class | 类加载...」）
 * 将粘连在同一行的符号与步骤自动拆分为结构清晰的垂直分步执行流。
 */
function normalizeInlineExecutionChains(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      if (!/[▼↓]/.test(segment)) return segment;
      return segment.replace(/\s*([▼↓])\s*/g, '\n\n$1\n\n');
    })
    .join('');
}

function escapeHtmlText(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/**
 * 将正文中的 **粗体** 预转为 <strong>，规避 CommonMark 在中文/引号/空格混排时无法闭合 emphasis 的问题。
 * 精细化区分：
 * 1. 结构化引导词/字段标签（如 **为什么错：**、**正确做法：**、**易错点：**、- **表现**：等）：赋予 class="chat-md-label"，渲染为主色调蓝；
 * 2. 句中随文普通强调（如 **可能仍然存在**、**不确定**）：赋予 class="chat-md-bold"，保持自然深黑字重加粗，不染蓝色。
 */
export function repairChatBoldMarkers(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment.replace(
        /(^|[\n\r]|[-*+•]\s*|[0-9]+\.\s*)?\*\*([^*\n]+?)\*\*([：:]?)/g,
        (match, prefix = '', inner: string, colon = '') => {
          const trimmed = inner.trim();
          if (!trimmed) return match;

          // 判断是否为结构化字段标签 / 引导小标头：
          // a. 粗体内或紧随其后带中英文冒号（如 **为什么错：**、**正确做法**：、**注意：**、**Step 1:**）
          // b. 格式为标签中括号（如 **【核心考点】**、**[重点]**）
          // c. 紧跟在行首或列表项开头且短小（长度 <= 8 且无句逗符号，如 - **易错点**）
          const hasColon = /[:：]$/.test(trimmed) || Boolean(colon);
          const isBracketTag = /^[【\[].+[】\]]$/.test(trimmed);
          const isLeadingShortTag = Boolean(prefix) && trimmed.length <= 8 && !/[，。！？；]/.test(trimmed);

          const isLabel = hasColon || isBracketTag || isLeadingShortTag;
          const displayInner = escapeHtmlText(trimmed);

          if (isLabel) {
            if (colon) {
              return `${prefix}<strong class="chat-md-label">${displayInner}${colon}</strong>`;
            }
            return `${prefix}<strong class="chat-md-label">${displayInner}</strong>`;
          }

          if (colon) {
            return `${prefix}<strong>${displayInner}${colon}</strong>`;
          }
          return `${prefix}<strong>${displayInner}</strong>`;
        }
      );
    })
    .join('');
}

/**
 * 深度思考区：不展示 Mermaid/大段 graph 源码，避免黑色 CODE 块；保留字符树并拆行
 */
function stripHeavyDiagramsFromReasoning(text: string): string {
  return text.replace(/```([^\n]*)\n([\s\S]*?)```/g, (full, _langLine: string, body: string) => {
    const trimmed = body.trim();
    if (!trimmed) return full;
    if (looksLikeMermaid(trimmed)) {
      return '\n\n> 流程图与拓扑图谱已在下方正文中展示，思考过程不再重复铺陈源码。\n\n';
    }
    if (looksLikeAsciiKnowledgeTree(trimmed)) {
      return full;
    }
    if (trimmed.length > 280 && /(?:-->|flowchart|graph\s+(?:TD|LR|TB))/i.test(trimmed)) {
      return '\n\n> 流程图与拓扑图谱已在下方正文中展示，思考过程不再重复铺陈源码。\n\n';
    }
    return full;
  });
}

/** 深度思考专用预处理：不走 Mermaid 围栏转换，避免思考区出现大段 CODE */
export function normalizeReasoningMarkdown(raw: string): string {
  if (!raw) return '';

  let text = normalizeCodeFences(raw);
  text = stripHeavyDiagramsFromReasoning(text);
  text = normalizeAsciiTreeInCodeFences(text);
  text = wrapBareAsciiTreeBlocks(text);
  text = normalizeInlineAsciiTree(text);

  text = text.replace(/^\s*#{1,3}\s*(?:回答|答案|解决方案)[:：]?\s*\n+/gi, '');
  text = normalizeGluedInlineHeadings(text);
  text = text.replace(/(^|\n)(#{1,6})([^\s#\n])/g, '$1$2 $3');
  text = normalizeGluedHeadingLines(text);

  text = normalizeInlineNumberedLists(text);
  text = normalizeInlineBulletLists(text);
  text = normalizeQuotedKeywordLines(text);
  text = normalizeTeachingHeaders(text);
  text = fixUnclosedCodeFences(text);

  return text;
}

/** 单行 compact Java 示例（Hello.java 一类），不含中文 */
const JAVA_ONE_LINER_RE =
  /^public\s+class\s+\w+\s*\{[^{}\n]*(?:\{[^{}\n]*\}[^{}\n]*)*\}$/;

/** 模型常把 Hello.java 写成无围栏的单行代码，仅包这一行，不碰后面中文正文 */
function wrapStandaloneJavaSnippets(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment
        .split('\n')
        .map((line) => {
          const trimmed = line.trim();
          if (!JAVA_ONE_LINER_RE.test(trimmed)) return line;
          if (trimmed.length > 1500 || /[\u4e00-\u9fa5]/.test(trimmed)) return line;
          return `\`\`\`java\n${trimmed}\n\`\`\``;
        })
        .join('\n');
    })
    .join('');
}

/** 从 public class 起按大括号配平，返回结束行下标（含） */
function findJavaClassBlockEndLine(lines: string[], startIdx: number): number {
  let depth = 0;
  let started = false;
  for (let i = startIdx; i < lines.length; i++) {
    for (const ch of lines[i]) {
      if (ch === '{') {
        depth += 1;
        started = true;
      } else if (ch === '}') {
        depth -= 1;
      }
    }
    if (started && depth === 0) {
      return i;
    }
  }
  return -1;
}

function findCodeEndLineBeforeProse(lines: string[]): number {
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i].trim();
    if (/^#{1,6}\s/.test(line)) return i - 1;
    if (/^\d+[.、．]\s*(?:\*\*|[\u4e00-\u9fa5])/.test(line)) return i - 1;
    if (
      i > 1 &&
      lines[i - 1].trim() === '' &&
      /^[\u4e00-\u9fa5`“"‘']/.test(line) &&
      !/^(?:\/\/|\/\*|\*|#|--)/.test(line)
    ) {
      const cnCount = (line.match(/[\u4e00-\u9fa5]/g) || []).length;
      if (cnCount >= 4) return i - 1;
    }
  }
  return -1;
}

/**
 * 保护机制：若代码块内部在代码之后紧跟了 Markdown 标题（## ）、列表（1. **...**）或大段中文正文，
 * 说明模型漏写了闭合的 ```（或闭合围栏缺失），在代码与正文交界处自动补齐 ``` 闭合代码块，防止正文被吞入黑色代码框。
 */
function splitProseFromCodeBlocks(text: string): string {
  return text.replace(
    /(```[a-zA-Z0-9+#-]*\n)([\s\S]*?)(```|$)/g,
    (full, openFence: string, body: string, _closeFence: string) => {
      // 保护 ASCII 字符框线、树状图与流程框：绝不可在字符画内部强行拆分
      if (
        /\+[-=]{3,}\+/.test(body) ||
        /^[ \t]*\+[-=]+[ \t]*$/m.test(body) ||
        /^[ \t]*[┌├└│─┼]/m.test(body)
      ) {
        return full;
      }
      const lines = body.split('\n');
      if (lines.length < 3) return full;

      const classStart = lines.findIndex((l) =>
        /^(?:public\s+)?(?:class|interface|enum)\s+\w+/.test(l.trim())
      );
      const classEnd = classStart >= 0 ? findJavaClassBlockEndLine(lines, classStart) : -1;
      // 边界必须取「第一个类结束行」与「正文起始边界」中更靠后者。
      // 教学示例常在同一代码块里连续写多个类（Animal → Dog → Test），只按第一个类裁剪，
      // 会把后面的类当成正文甩出代码框，表现为「首个类在框内、其余类裸露」；
      // 而后续代码里只要有一处中文注释（如 //输出 Dog），下方的中文判断即成立，误拆必然发生。
      const proseBoundary = findCodeEndLineBeforeProse(lines);
      const endIdx = Math.max(classEnd, proseBoundary);

      if (endIdx >= 0 && endIdx < lines.length - 1) {
        const remainingLines = lines.slice(endIdx + 1);
        const remainingText = remainingLines.join('\n').trim();
        // 裁剪点之后若仍有类型声明，说明边界取得偏早：此时宁可不拆，也不能截断同一段代码
        const stillCode =
          /^(?:\s*(?:public|private|protected|static|final|abstract)\s+)*(?:class|interface|enum|record)\s+\w+/m.test(
            remainingText
          );
        if (
          !stillCode &&
          (/[\u4e00-\u9fa5]/.test(remainingText) || /^#{1,6}\s/m.test(remainingText))
        ) {
          const code = lines.slice(0, endIdx + 1).join('\n').trimEnd();
          const prose = remainingLines.join('\n').trimStart();
          return `${openFence}${code}\n\`\`\`\n\n${prose}\n\n`;
        }
      }

      return full;
    }
  );
}

function closeFenceAfterJavaLine(head: string, openFence: string, body: string): string | null {
  const lines = body.split('\n');
  const classStart = lines.findIndex((l) => /^public\s+class\s+\w+/.test(l.trim()));
  if (classStart >= 0) {
    const endIdx = findJavaClassBlockEndLine(lines, classStart);
    if (endIdx >= 0) {
      const javaBlock = lines.slice(classStart, endIdx + 1).join('\n');
      const before = lines.slice(0, classStart).join('\n');
      const after = lines.slice(endIdx + 1).join('\n');
      const prefix = before ? `${before}\n` : '';
      return `${head}${prefix}${openFence}${javaBlock}\n\`\`\`\n${after}`;
    }
  }

  const oneLinerIdx = lines.findIndex((l) => JAVA_ONE_LINER_RE.test(l.trim()));
  if (oneLinerIdx >= 0) {
    const before = lines.slice(0, oneLinerIdx).join('\n');
    const javaLine = lines[oneLinerIdx].trim();
    const after = lines.slice(oneLinerIdx + 1).join('\n');
    const prefix = before ? `${before}\n` : '';
    const fence = /java/i.test(openFence) ? openFence : '```java\n';
    return `${head}${prefix}${fence}${javaLine}\n\`\`\`\n${after}`;
  }

  return null;
}

/** 行内粘连的标题，如「运行链路##5.1编写」或「洛必达法则###1.法则内容」 */
function normalizeGluedInlineHeadings(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment.replace(/([^\s#\n])(#{2,6})[ \t]*(?=[^\s#\n])/g, '$1\n\n$2 ');
    })
    .join('');
}

/**
 * 拆分标题行粘连的无序列表、有序列表或紧随其后的正文叙述。
 * 例如：
 * 1. 标题粘连无序列表：
 *    「##八、易错点提醒- **不是未定式不能用洛必达**；」 -> 「## 八、易错点提醒\n\n- **不是未定式不能用洛必达**；」
 *    「## 重点归纳：- 极限运算」 -> 「## 重点归纳\n\n- 极限运算」
 * 2. 标题粘连有序列表：
 *    「### 2. 使用步骤1. 先代入…」 -> 「### 2. 使用步骤\n\n1. 先代入…」
 * 3. 标题粘连正文起笔：
 *    「### 1. 法则内容若\[\lim…」 -> 「### 1. 法则内容\n\n若\[\lim…」
 *    「## 四、洛必达法则的注意事项洛必达法则很强，但不是万能的。」 -> 「## 四、洛必达法则的注意事项\n\n洛必达法则很强，但不是万能的。」
 *
 * 严格保护：
 * - 括号内副标题（如「### 题目二（数列极限 - 四则运算）」）
 * - 普通副标题连字符（如「## 模块一 - 基础篇」）
 */
function normalizeGluedHeadingLines(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed: string[] = [];

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const m = line.match(/^([ \t]*#{1,6}[ \t]+)([^\n]+)$/);
        if (!m) {
          processed.push(line);
          continue;
        }

        const marker = m[1].trim();
        const content = m[2];

        const formatHeadingTitle = (t: string) =>
          t.trim().replace(/[：:]$/, '').replace(/^(\d{1,2}\.)(?=[\u4e00-\u9fa5])/u, '$1 ');

        // 1. 标题行末尾粘连无序列表（如「##八、易错点提醒- **不是未定式…」或「## 重点：- 极限…」）
        const bulletMatch = content.match(
          /(?:([：:][ \t]*)|(?<!-)([^\s\-（(【])([ \t]*))(-[ \t]*\*\*|-[ \t]+(?=[\p{Extended_Pictographic}\u4e00-\u9fa5A-Za-z0-9]))/u
        );
        if (bulletMatch && bulletMatch.index !== undefined) {
          const splitPos = bulletMatch.index + (bulletMatch[1] ? 0 : (bulletMatch[2] ? bulletMatch[2].length : 0));
          const beforeStr = content.slice(0, splitPos);
          if (!hasUnclosedBrackets(beforeStr)) {
            const headingText = formatHeadingTitle(beforeStr);
            const listPart = content.slice(
              bulletMatch.index +
                (bulletMatch[1] ? bulletMatch[1].length : (bulletMatch[2]?.length || 0) + (bulletMatch[3]?.length || 0))
            ).trim();

            if (headingText.length >= 2 && headingText.length <= 40) {
              const isBoldList = /^-[ \t]*\*\*/.test(listPart);
              const hasColon = Boolean(bulletMatch[1]);
              const noSpaceBefore = !bulletMatch[3];
              const hasListPunct = /[；;]$/.test(listPart) || /[。！!]$/.test(listPart);

              if (
                isBoldList ||
                hasColon ||
                (noSpaceBefore && /^[一二三四五六七八九十\d]/.test(headingText)) ||
                hasListPunct
              ) {
                const normalizedList = listPart.replace(/^-[ \t]*/, '- ');
                processed.push(`${marker} ${headingText}\n\n${normalizedList}`);
                continue;
              }
            }
          }
        }

        // 2. 标题行末尾粘连有序列表序号（如「### 2. 使用步骤1. 先代入…」或「## 核心步骤1. 先求导…」或「七、变式训练1.求…」）
        const numberMatch = content.match(
          /([\u4e00-\u9fa5A-Za-z）)】])[ \t]*([1-9]\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5\w*（(「『【$\\]))/u
        );
        if (numberMatch && numberMatch.index !== undefined) {
          const splitPos = numberMatch.index + numberMatch[1].length;
          const beforeStr = content.slice(0, splitPos);
          if (!hasUnclosedBrackets(beforeStr)) {
            const headingText = formatHeadingTitle(beforeStr);
            const listPart = content.slice(splitPos).trim();
            if (headingText.length >= 2 && headingText.length <= 40) {
              processed.push(`${marker} ${headingText}\n\n${listPart}`);
              continue;
            }
          }
        }

        // 2b. 标题行整行末尾以「1.」结尾（如「## 七、变式训练1.」），其后另起一行写题目「求...」
        // 注意：不可将 1. 独立放在空行输出，否则 Markdown 会将其渲染为空的孤立列表项；应拼接到下一行题干开头
        const trailingNumberMatch = content.match(/^(.+?[\u4e00-\u9fa5A-Za-z）)】])[ \t]*([1-9]\.)\s*$/);
        if (trailingNumberMatch) {
          const headingText = formatHeadingTitle(trailingNumberMatch[1]);
          if (headingText.length >= 2 && headingText.length <= 40 && !hasUnclosedBrackets(headingText)) {
            const numPrefix = `${trailingNumberMatch[2]} `;
            let attached = false;
            for (let j = i + 1; j < lines.length; j++) {
              if (lines[j].trim().length > 0) {
                lines[j] = `${numPrefix}${lines[j].trimStart()}`;
                attached = true;
                break;
              }
            }
            if (attached) {
              processed.push(`${marker} ${headingText}`);
            } else {
              processed.push(`${marker} ${headingText}\n\n${numPrefix}`);
            }
            continue;
          }
        }

        // 3. 标题行后直接粘连正文引导叙述（如「### 1. 法则内容若\[\lim…」或「## 五、其他未定式的转化方法除了…」）
        const proseMatch = content.match(
          /^((?:[一二三四五六七八九十]+[、．.]|\d+\.|\d+\.\d+|例\d+|变式)?\s*[\u4e00-\u9fa5A-Za-z0-9_\\(\\)]{2,25}?)(?=若|如果|当|设|除了|遇到|可以按|例如|比如|注意[，,：:]|求[$\\ \t\d]|在计算|在一定条件下)/u
        );
        if (proseMatch && proseMatch[1]) {
          const headingText = formatHeadingTitle(proseMatch[1]);
          const prosePart = content.slice(proseMatch[0].length).trim();
          if (headingText.length >= 2 && prosePart.length > 0 && !hasUnclosedBrackets(headingText)) {
            processed.push(`${marker} ${headingText}\n\n${prosePart}`);
            continue;
          }
        }

        // 4. 标题以经典收尾词（注意事项/常见误区/解题步骤等）结尾，后面紧贴未换行的正文
        const closerMatch = content.match(
          /^((?:[一二三四五六七八九十]+[、．.]|\d+\.|\d+\.\d+|例\d+|变式)?\s*[\u4e00-\u9fa5A-Za-z0-9_]{1,25}?(?:注意事项|常见误区|核心考点|基本概念|定义与性质|解题步骤|解题总策略|学习目标|归纳总结))([^\n]+)$/u
        );
        if (closerMatch && closerMatch[1] && closerMatch[2]?.trim()) {
          const headingText = formatHeadingTitle(closerMatch[1]);
          const prosePart = closerMatch[2].trim();
          if (headingText.length >= 2 && !hasUnclosedBrackets(headingText)) {
            processed.push(`${marker} ${headingText}\n\n${prosePart}`);
            continue;
          }
        }

        processed.push(line);
      }

      return processed.join('\n');
    })
    .join('');
}

/** 去掉模型复读、未闭合的围栏行，避免页面上直接露出 ``` */
function fixUnclosedCodeFences(text: string): string {
  const count = (text.match(/```/g) || []).length;
  if (count % 2 === 0) return text;

  const lastIdx = text.lastIndexOf('```');
  const head = text.slice(0, lastIdx);
  const tailFromFence = text.slice(lastIdx);
  const openMatch = tailFromFence.match(/^```([^\n]*)\n?/);
  if (!openMatch) {
    return head + tailFromFence.replace(/^```+/, '');
  }

  const body = tailFromFence.slice(openMatch[0].length);

  const javaClosed = closeFenceAfterJavaLine(head, openMatch[0], body);
  if (javaClosed) return javaClosed;

  const chineseChars = (body.match(/[\u4e00-\u9fa5]/g) || []).length;

  if (chineseChars > 60 || /^#{1,6}\s/m.test(body) || /^\s*\d+[.、．]\s/m.test(body)) {
    return head + body;
  }

  const blankBreak = body.search(/\n\s*\n/);
  if (blankBreak > 0 && blankBreak < 2000) {
    return `${head}${openMatch[0]}${body.slice(0, blankBreak)}\n\`\`\`\n${body.slice(blankBreak)}`;
  }

  if (body.length < 1200 && chineseChars < 30) {
    return `${text}\n\`\`\``;
  }

  return head + body;
}

function tidyStrayFenceMarkers(text: string): string {
  let s = text;
  s = s.replace(/(```[a-zA-Z0-9+#-]*)\s*\n\s*\1\s*\n/g, '$1\n');
  s = s.replace(/^[ \t]*`[ \t]*`[ \t]*$/gm, '');
  return s
    .split('\n')
    .filter((line) => {
      const t = line.trim();
      if (t === '`' || t === '``') return false;
      return true;
    })
    .join('\n');
}

/** 围栏未配对时 markdown-it 会把 ```java 渲染成普通段落，需从 HTML 剔除 */
function stripOrphanFenceParagraphs(html: string): string {
  if (!html) return '';
  return html
    .replace(/<p>\s*```[a-zA-Z0-9+#-]*\s*<\/p>/gi, '')
    .replace(/<p>\s*```\s*<\/p>/gi, '')
    .replace(/<p>\s*`{1,2}\s*<\/p>/gi, '');
}

/**
 * 针对大模型流式输出与中文混排的专用 Markdown 预处理器（Code Compass 同款）
 */
export function normalizeChatMarkdown(raw: string): string {
  if (!raw) return '';

  let text = normalizeCodeFences(raw);
  text = text.replace(/```text([A-Za-z\u4e00-\u9fa5])/gi, '```text\n$1');
  text = unwrapProseCodeFences(text);
  text = splitProseFromCodeBlocks(text);
  text = wrapStandaloneJavaSnippets(text);
  text = tidyStrayFenceMarkers(text);
  text = normalizeMermaidInCodeFences(text);
  text = normalizeAsciiTreeInCodeFences(text);
  text = wrapBareAsciiTreeBlocks(text);
  text = normalizeInlineAsciiTree(text);

  text = text.replace(/^\s*#{1,3}\s*(?:回答|答案|解决方案)[:：]?\s*\n+/gi, '');
  text = text.replace(/^\s*#{1,3}\s*((?:针对)?您关于)/, '$1');
  text = normalizeGluedInlineHeadings(text);
  text = text.replace(/(^|\n)(#{1,6})([^\s#\n])/g, '$1$2 $3');
  text = normalizeGluedHeadingLines(text);

  // 只消除「加粗标记内侧」的水平空白：\s 会跨行匹配，把上一行的 **分组标题** 与下一行的
  // "- **列表项**" 误配成一对加粗，导致换行与列表标记一起被吞进加粗文本
  text = text.replace(/\*\*[ \t]+([^*\n]+?)[ \t]+\*\*/g, '**$1**');
  text = text.replace(/\*\*[ \t]+([^*\n]+?)\*\*/g, '**$1**');
  text = text.replace(/\*\*([^*\n]+?)[ \t]+\*\*/g, '**$1**');
  // 仅消除横向空格与制表符，避免误吞换行符 \n
  text = text.replace(/\*\*([^*\n]+?)\*\*[ \t]+(?=[\u4e00-\u9fa5（(「『【])/g, '**$1**');

  // 3. 修复模型漏写闭合 $ 符号直接接中文标点/说明文字的问题
  text = repairUnclosedMathBeforeChinesePunct(text);

  // 4. 同一行内粘连的「1.xxx 2.xxx 3.xxx」拆成 Markdown 有序列表（CommonMark 要求每条独占一行）
  text = normalizeInlineNumberedLists(text);
  // 合并单独成行的数字序号（防止「1.」后换行导致空列表项与题干正文剥离）
  text = text.replace(/(^|\n)([ \t]*\d{1,2}\.[ \t]*)\n+([^\n\s#])/g, '$1$2$3');
  // 5. 同一行内粘连的「-项1 … -项2 …」拆成无序列表
  text = normalizeInlineBulletLists(text);
  // 6. 关键词引号 "" 粘连拆行
  text = normalizeQuotedKeywordLines(text);
  // 7. 教学与问答高频小标题（如「**参考答案：**」「**解析：**」「【题目】」等）粘连自动拆行
  text = normalizeTeachingHeaders(text);
  // 8. 执行链路（如用 ▼ 或 ↓ 串联的步骤）粘连自动拆分
  text = normalizeInlineExecutionChains(text);

  text = fixUnclosedCodeFences(text);

  // 补齐末尾流式未闭合的单个 $ 符号（仅在整篇消息 $ 数量为奇数时补在末尾）
  const dollarMatches = text.replace(/\$\$/g, '').match(/(?<!\\)\$/g);
  if (dollarMatches && dollarMatches.length % 2 !== 0) {
    text += '$';
  }

  return text;
}

/** 行内 <code> 保留等宽样式（代码块在 pre 内，不受此影响） */
export function unwrapChatInlineCode(html: string): string {
  if (!html) return '';
  return html.replace(/<code>([^<]*)<\/code>/g, (_match, inner: string) => {
    return `<code class="chat-inline-code">${inner}</code>`;
  });
}

/** 渲染聊天消息 HTML */
export function renderChatMarkdown(raw: string): string {
  if (!raw?.trim()) return '';

  const normalized = normalizeChatMarkdown(raw);
  const withBold = repairChatBoldMarkers(normalized);
  let html = renderMarkdownForChat(withBold);

  html = unwrapChatInlineCode(html);
  html = stripOrphanFenceParagraphs(html);

  html = html.replace(/\[([1-9]\d*)\]/g, (_match, id) => {
    return `<sup class="copilot-citation-sup" data-citation-idx="${id}">[${id}]</sup>`;
  });

  return html;
}

/** 深度思考区专用：不走正文表格容错，避免 {x | ...} 被误拆成表格 */
export function renderReasoningMarkdown(raw: string): string {
  if (!raw?.trim()) return '';
  const normalized = normalizeReasoningMarkdown(cleanReasoningText(raw));
  const withBold = repairChatBoldMarkers(normalized);
  let html = renderMarkdownForReasoning(withBold);
  html = unwrapChatInlineCode(html);
  return html;
}

export { bindMarkdownCodeCopy, cleanupOrphanMermaidDom, renderMermaidInElement, normalizeCodeFences } from '@/utils/markdown';
