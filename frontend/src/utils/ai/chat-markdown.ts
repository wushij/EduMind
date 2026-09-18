import {
  renderMarkdownForChat,
  renderMarkdownForReasoning,
  normalizeCodeFences,
  looksLikeMermaid
} from '@/utils/markdown';
import { cleanReasoningText } from './copilot-stream-split';
import { expandAsciiTreeToMultiline, looksLikeAsciiKnowledgeTree } from './ascii-tree-graph';

const FENCED_CODE_BLOCK_RE = /(```[\s\S]*?```)/g;

/**
 * 大模型常把「1.知识地图…2.概念辨析…3.定理…」挤在同一行；
 * Markdown 无法识别，需拆行并在序号后补空格。
 * 必须严格保护带加粗的序号（如「**1. 异常体系**」），防止换行符插入 ** 与数字之间导致加粗断裂。
 */
function isMarkdownTableRowLine(line: string): boolean {
  const trimmed = line.trim();
  return trimmed.startsWith('|') && (trimmed.endsWith('|') || trimmed.includes('|', 1));
}

/**
 * 大模型常把「1.知识地图…2.概念辨析…3.定理…」挤在同一行；
 * Markdown 无法识别，需拆行并在序号后补空格。
 * 必须严格保护表格行及带加粗的序号，防止在表格单元格内插入换行切碎表格。
 */
function normalizeInlineNumberedLists(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        // 表格行必须完整保护，绝不可在单元格内插入换行切断表格
        if (isMarkdownTableRowLine(line)) return line;

        let s = line;

        // 1. 若同一行前文粘连了带加粗的序号小标题（仅在标点符号、中文、括号之后粘连，绝不在 | 等非文本符号后拆行）
        s = s.replace(
          /([\p{Extended_Pictographic}\u4e00-\u9fa5a-zA-Z0-9。；;!?！？:：）)”"』」】])[ \t]*\*\*(\d{1,2})\.[ \t]*/gu,
          '$1\n\n**$2. '
        );

        // 2. 确保已在行首的加粗序号内部有标准空格（如「**1.知识体系**」->「**1. 知识体系**」），绝不拆开 ** 与数字
        s = s.replace(
          /(^|\n)([ \t]*)\*\*(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z])/gu,
          '$1$2**$3. '
        );

        // 3. 普通无加粗序号拆行：仅在标点符号、中文后粘连时拆行，避免误伤
        s = s.replace(
          /([\p{Extended_Pictographic}\u4e00-\u9fa5。；;!?！？:：）)”"』」】])[ \t]*(\d{1,2})\.[ \t]*(?=[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z])/gu,
          '$1\n$2. '
        );

        // 4. 行首普通序号后补齐标准空格
        s = s.replace(
          /^([ \t]*)(\d{1,2})\.(?=[ \t]*[\p{Extended_Pictographic}\u4e00-\u9fa5（(「『【A-Za-z])/gmu,
          '$1$2. '
        );

        return s;
      });

      return processed.join('\n');
    })
    .join('');
}

/**
 * 同一行粘连的「-两个重要极限 … -连续判定 …」拆成 Markdown 无序列表。
 * 全面支持 Emoji 图标及句末交流引导句自动分段。表格行内不拆行。
 */
function normalizeInlineBulletLists(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;

      const lines = segment.split('\n');
      const processed = lines.map((line) => {
        if (isMarkdownTableRowLine(line)) return line;

        let s = line;

        // 1. 将行首（包含缩进）或行内实心圆点 • 或 · 转换为标准 Markdown 无序列表项 - 
        s = s.replace(/([^\n])\s*[•·]\s*(?=[\p{Extended_Pictographic}\*\*\u4e00-\u9fa5【「『（])/gu, '$1\n- ');
        s = s.replace(/^([ \t]*)[•·]\s*/gmu, '$1- ');

        // 2. 标点符号、冒号或中文后紧贴的 -项（包含 Emoji、粗体**、中文小标题等，排除 --- 水平分割线）
        s = s.replace(
          /(?<!-)([\u4e00-\u9fa5。；;!?！？:：）)”"』」】])\s*-(?:\s*|\s+)(?=[\p{Extended_Pictographic}\*\*\u4e00-\u9fa5【「『（])/gu,
          '$1\n- '
        );

        // 3. 段末/行中同一行粘连 -列表（仅中文/Emoji/** 开头），必须排除减号与换行，且空格仅限行内空白
        s = s.replace(
          /(?<!-)([^\n\r\t -])[ \t]*-[ \t]+(?=\*\*|[\p{Extended_Pictographic}\u4e00-\u9fff（(「『【])/gu,
          '$1\n- '
        );

        // 4. CommonMark 规范化：确保行首（允许带缩进空格）- 后面有空格，支持 Emoji、中英文、粗体
        s = s.replace(/^([ \t]*)-(?=[\p{Extended_Pictographic}\u4e00-\u9fa5*（(「『【A-Za-z0-9])/gmu, '$1- ');

        // 5. 列表项末尾粘连的交流互动引导句自动脱离列表形成独立自然段
        s = s.replace(
          /([\u4e00-\u9fa5。；;!?！？）)”"』」】])\s*(不妨(?:先聊|告诉|直接|说说)|那么[，, ]?我们|请问(?:你|您)|你现在(?:是|想)|你是(?:刚|想))/gu,
          '$1\n\n$2'
        );

        return s;
      });

      return processed.join('\n');
    })
    .join('');
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

function escapeHtmlText(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/**
 * 将正文中的 **粗体** 预转为 <strong>，规避 CommonMark 在中文/引号/空格混排时无法闭合 emphasis 的问题。
 */
export function repairChatBoldMarkers(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      return segment.replace(/\*\*([^*\n]+?)\*\*/g, (_match, inner: string) => {
        const trimmed = inner.trim();
        if (!trimmed) return _match;
        return `<strong>${escapeHtmlText(trimmed)}</strong>`;
      });
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
  text = text.replace(/(^|\n)(#{1,6})([^\s#\n])/g, '$1$2 $3');

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

/** 正文中间粘连的小节标题，如「运行链路##5.1编写」 */
function normalizeGluedInlineHeadings(text: string): string {
  return text
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment) => {
      if (segment.startsWith('```')) return segment;
      let s = segment.replace(/([^\s#\n])(#{2,6})(\d+(?:\.\d+)?)/g, '$1\n\n$2 $3');
      s = s.replace(
        /([^\s#\n])(#{2,6})(?=[\u4e00-\u9fa5（(「『【A-Za-z])/g,
        '$1\n\n$2 '
      );
      return s;
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
      if (t === '`' || t === '``' || t === '```') return false;
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

  text = text.replace(/\*\*\s+([^*\n]+?)\s+\*\*/g, '**$1**');
  text = text.replace(/\*\*\s+([^*\n]+?)\*\*/g, '**$1**');
  text = text.replace(/\*\*([^*\n]+?)\s+\*\*/g, '**$1**');
  // 仅消除横向空格与制表符，避免误吞换行符 \n
  text = text.replace(/\*\*([^*\n]+?)\*\*[ \t]+(?=[\u4e00-\u9fa5（(「『【])/g, '**$1**');

  // 4. 同一行内粘连的「1.xxx 2.xxx 3.xxx」拆成 Markdown 有序列表（CommonMark 要求每条独占一行）
  text = normalizeInlineNumberedLists(text);
  // 5. 同一行内粘连的「-项1 … -项2 …」拆成无序列表
  text = normalizeInlineBulletLists(text);
  // 6. 关键词引号 "" 粘连拆行
  text = normalizeQuotedKeywordLines(text);
  // 7. 教学与问答高频小标题（如「**参考答案：**」「**解析：**」「【题目】」等）粘连自动拆行
  text = normalizeTeachingHeaders(text);

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
