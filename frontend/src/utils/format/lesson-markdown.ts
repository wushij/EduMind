import { bindMarkdownCodeCopy } from '@/utils/markdown';
import { normalizeDisplayListMarkdown, renderChatMarkdown } from '@/utils/ai/chat-markdown';

/** 围栏代码块：内部文本不得参与标题规范化 */
const FENCED_CODE_BLOCK_RE = /(```[\s\S]*?```)/g;
/** 粘连标题行：`##标题正文…`（`#` 后缺空格，标题与后文挤在同一行） */
const GLUED_HEADING_LINE_RE = /^(#{1,6})([^\s#].*)$/;
/** 行内粘连的下一级标题：`…适用边界####3.1判定标准…` */
const GLUED_INLINE_HEADING_RE = /([^\s#\n])(#{2,6})(?=\S)/g;
/**
 * 粘连标题的「标题 → 正文」分界信号，按出现位置取最早者。
 * consume 表示分界符是否属于标题（冒号与句号收在标题侧，列表项/加粗/编号起始留给正文）。
 */
const HEADING_BOUNDARY_PATTERNS: Array<{ re: RegExp; consume: number; colon: boolean }> = [
  { re: /[：:]/, consume: 1, colon: true },
  { re: /。/, consume: 1, colon: false },
  { re: /\s-\s/, consume: 0, colon: false },
  { re: /\*\*/, consume: 0, colon: false },
  { re: /\d+\.\s/, consume: 0, colon: false }
];
/** 冒号后仍可能属于标题的最长片段；更长说明冒号后已经是正文句子 */
const MAX_HEADING_TAIL_CHARS = 12;
/** 粘连行可被当作标题的最大字数；超过即不再猜测边界 */
const MAX_GLUED_HEADING_CHARS = 30;
/** 短行判定：不超过该字数的粘连行，整行就是一个完整标题 */
const SHORT_HEADING_CHARS = 12;
/** 收口符号：以这些符号结尾的粘连行视为完整标题 */
const HEADING_CLOSER_RE = /[）)】」》]$/;
/** 标题被字数上限截断时留下的省略号 */
const TITLE_ELLIPSIS_RE = /[…⋯]/;

/** 清理 AI 学习目标/列表文本，并规范为可渲染的 Markdown 无序列表 */
export function normalizeLessonListMarkdown(source: string): string {
  let text = (source || '')
    .replace(/^(\s*)[\uFF0D\u2013\u2014\u2212•·]\s*/gm, '$1- ')
    .replace(/\r\n/g, '\n');
  text = normalizeDisplayListMarkdown(text);
  return text
    .replace(/^---+$/gm, '')
    .replace(/^\s*[-–—]{2,}\s*$/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

/** 编号或结构化小节前缀 */
const HEADING_LEADER_RE =
  /^(?:[一二三四五六七八九十百千]+[、．.]|第[一二三四五六七八九十\d]+[章节部分]|\d+\.|\d+\.\d+|例\d+|变式)\s*/;

/** 紧随小标题之后的正文叙述引导词（在这些词之前切断标题） */
const PROSE_INTRO_BOUNDARY_RE =
  /(?=本[课节章节讲篇部分模块]|设|若|当|如果|关注|定义为|对任意|记作|其[中依据核心]|第一[，,个步条章节]|第1[个步条章节]|除定义|求[$\\ \t\d]|主要[包围绕]|围绕|核心[任务是要]|旨在|极限是微积分)/;

/** 在 from 之后查找最早出现的分界信号 */
function findHeadingBoundary(
  text: string,
  from: number
): { index: number; consume: number; colon: boolean } | null {
  // 1. 优先针对序号小标题紧贴的正文词根重复（如「2.函数极限函数极限关注…」-> 组1:「2.」，组2:「函数极限」）
  if (from === 0) {
    const repeatMatch = text.match(
      /^((?:[一二三四五六七八九十]+[、．.]|\d+\.|\d+\.\d+|例\d+|变式)?\s*)([\u4e00-\u9fa5]{2,6})\2/
    );
    if (repeatMatch && repeatMatch[2].length >= 2) {
      const boundaryLen = repeatMatch[1].length + repeatMatch[2].length;
      return { index: boundaryLen, consume: 0, colon: false };
    }

    // 2. 序号/小标题后紧随正文假设引导词（若含有靠前的冒号，则优先走冒号分界）
    const leaderMatch = text.match(HEADING_LEADER_RE);
    if (leaderMatch) {
      const afterLeader = text.slice(leaderMatch[0].length);
      const colonIdx = afterLeader.search(/[：:]/);
      if (colonIdx < 0 || colonIdx > 12) {
        const introIdx = afterLeader.search(PROSE_INTRO_BOUNDARY_RE);
        if (introIdx >= 2 && introIdx <= 24) {
          return { index: leaderMatch[0].length + introIdx, consume: 0, colon: false };
        }
      }
      // 3. 序号小标题后紧贴破折号列表（如「3.其他未定式转化- 0·∞」）
      const dashIdx = afterLeader.search(/\s*-\s*[^\s-]/);
      if (dashIdx >= 2 && dashIdx <= 15) {
        return { index: leaderMatch[0].length + dashIdx, consume: 0, colon: false };
      }
      // 4. 序号小标题后紧随英文实体/缩写 + 谓词引导的正文（如「二、JVM指令与栈帧执行模型JVM是“字节码的 CPU”」）
      const entityProseMatch = afterLeader.match(
        /^([\u4e00-\u9fa5A-Za-z0-9_（）()]{2,24}?)(?=[A-Za-z]{2,}(?:是|为|指|由|在|采用|负责|主要|可以|将|通过|用于))/
      );
      if (entityProseMatch && entityProseMatch[1].length >= 2 && entityProseMatch[1].length <= 24) {
        return { index: leaderMatch[0].length + entityProseMatch[1].length, consume: 0, colon: false };
      }
      // 5. 序号小标题后紧随代词/指示词/常见谓词引导的正文（如「二、栈帧模型它是指令运算…」或「二、栈帧模型是指…」）
      const proseBoundaryMatch = afterLeader.match(
        /^([\u4e00-\u9fa5A-Za-z0-9_（）()]{2,24}?)(?=(?:它[们的是]|这[项个是门种]|此[类项处时]|该[系统模型算法]|在[实际此本]|是指|所谓|主要包括|核心在于|通常由|通常是|一般是))/
      );
      if (proseBoundaryMatch && proseBoundaryMatch[1].length >= 2 && proseBoundaryMatch[1].length <= 24) {
        return { index: leaderMatch[0].length + proseBoundaryMatch[1].length, consume: 0, colon: false };
      }
    }
  }

  let best: { index: number; consume: number; colon: boolean } | null = null;
  for (const pattern of HEADING_BOUNDARY_PATTERNS) {
    const index = text.slice(from).search(pattern.re);
    if (index < 0) continue;
    const absolute = from + index;
    if (!best || absolute < best.index) {
      best = { index: absolute, consume: pattern.consume, colon: pattern.colon };
    }
  }
  return best;
}

/**
 * 拆分单个粘连标题行：`###二、洛必达法则：条件重于计算**定理**…` → 标题 + 空行 + 正文。
 *
 * 模型经常把 `#` 之后的标题和紧随其后的正文写在同一行，渲染器补上空格后会把这「一整段」
 * 变成标题（页面上就是一大片粗体），因此必须在渲染前把标题与正文分开。
 * 边界只认标点与结构符号，猜不到边界时宁可整行降级为正文，也不硬切出半截标题。
 */
function splitGluedHeadingLine(line: string): string {
  const match = line.match(GLUED_HEADING_LINE_RE);
  if (!match) return line;
  const marker = match[1];
  const text = match[2];
  const boundary = findHeadingBoundary(text, 0);

  if (!boundary || boundary.index <= 0 || boundary.index > MAX_GLUED_HEADING_CHARS) {
    const looksComplete = text.length <= SHORT_HEADING_CHARS || HEADING_CLOSER_RE.test(text);
    // 无分界可依：完整标题只补空格，长句整行按正文渲染（避免出现一整段粗体标题）
    return looksComplete ? `${marker} ${text}` : text;
  }

  let headEnd = boundary.index + boundary.consume;
  let restStart = headEnd;
  if (boundary.colon) {
    const next = findHeadingBoundary(text, headEnd);
    const tailEnd = next ? next.index : Math.min(text.length, headEnd + MAX_HEADING_TAIL_CHARS + 1);
    const tail = text.slice(headEnd, tailEnd).trim();
    if (tail && tail.length <= MAX_HEADING_TAIL_CHARS) {
      // 冒号后接的是短定语（「二、洛必达法则：条件重于计算」）→ 并入标题
      headEnd = tailEnd;
      restStart = tailEnd;
    } else {
      // 冒号即标题结尾：冒号归正文侧丢弃，标题不带尾冒号
      headEnd = boundary.index;
      restStart = boundary.index + 1;
    }
  }

  const rawHead = text.slice(0, headEnd).trim();
  const trailingListBullet = /[-–—•·]\s*$/.test(rawHead);
  const head = rawHead.replace(/[-–—•·\s]+$/, '');
  let rest = text.slice(restStart).trim();
  // 若分界处原本包含无序列表符号（如「###小节标题- **加粗列表项**」），剥离标题尾部符号后需归还给列表正文
  // 注意：排除 ** 加粗标记，列表标记必须包含空格（如 "- " 或 "* "）
  const isAlreadyList = /^(?:[-+•·]\s|\*(?!\*)\s|\d+\.\s)/.test(rest);
  if (trailingListBullet && !isAlreadyList) {
    rest = `- ${rest}`;
  }
  return rest ? `${marker} ${head}\n\n${rest}` : `${marker} ${head}`;
}

/** 逐行拆分粘连标题（跳过围栏代码块，避免把代码里的 # 当标题） */
export function splitGluedHeadings(source: string): string {
  if (!source || !source.includes('#')) return source;
  return source
    .split(FENCED_CODE_BLOCK_RE)
    .map((segment, index) => {
      if (index % 2 === 1) return segment;
      const withInlineSplit = segment.replace(GLUED_INLINE_HEADING_RE, '$1\n\n$2');
      return withInlineSplit
        .split('\n')
        .map((line) => splitGluedHeadingLine(line))
        .join('\n');
    })
    .join('');
}

/** 折叠空白与省略号：用于比较「正文里的标题回声」与课节标题 */
function foldTitle(text: string): string {
  return (text || '').replace(/\s+/g, '').replace(/[…⋯]|\.{3}/g, '');
}

/** 在原文中按「折叠后的字符数」定位切割点，跳过空白与省略号 */
function consumeFolded(raw: string, foldedLength: number): number | null {
  let count = 0;
  for (let i = 0; i < raw.length; i++) {
    if (/\s/.test(raw[i]) || TITLE_ELLIPSIS_RE.test(raw[i])) continue;
    count += 1;
    if (count === foldedLength) return i + 1;
  }
  return null;
}

/** 按省略号切分标题：返回「省略号前锚点」与「省略号后后缀」 */
function splitAtEllipsis(title: string): [string, string] {
  const index = title.search(TITLE_ELLIPSIS_RE);
  if (index < 0) return [title, ''];
  return [title.slice(0, index), title.slice(index + 1).replace(/^[\s.]+/, '')];
}

/**
 * 定位正文开头「课节标题回声」的结束位置。
 *
 * 优先严格匹配（折叠空白/省略号后，正文行以课节标题开头）；
 * 标题曾被字数上限截断（带 `…`）的历史数据，退化为「省略号前锚点 + 省略号后后缀」定位。
 */
function resolveTitleEchoCut(line: string, lessonTitle: string): number | null {
  const folded = foldTitle(line);
  const foldedTitle = foldTitle(lessonTitle);
  if (!foldedTitle || foldedTitle.length < 4 || !folded.startsWith(foldedTitle.slice(0, 4))) {
    return null;
  }
  if (folded.startsWith(foldedTitle)) {
    return consumeFolded(line, foldedTitle.length);
  }

  const [anchor, tail] = splitAtEllipsis(lessonTitle);
  if (!tail) return null;
  const consumed = consumeFolded(line, foldTitle(anchor).length);
  if (consumed === null) return null;
  const tailIndex = line.slice(consumed).indexOf(tail);
  // 后缀必须紧跟在锚点之后，否则说明匹配到的不是同一标题
  if (tailIndex < 0 || tailIndex > 6) return null;
  return consumed + tailIndex + tail.length;
}

/**
 * 剥离正文开头的课节标题回声。
 *
 * 模型拿到 `{{lesson_title}}` 后常把标题当作正文的一级标题再写一遍
 * （`##1.2 洛必达法则求未定式极限专项突破…`），而工作台与学习页已经用课节标题
 * 作为页面标题展示，于是同一屏出现两个「1.2 xxx专项突破」。
 */
export function stripLessonTitleEcho(source: string, lessonTitle?: string): string {
  if (!source || !lessonTitle || !source.trimStart().startsWith('#')) return source;
  const leading = source.match(/^\s*#{1,6}\s*([^\n]*)/);
  if (!leading) return source;
  const cut = resolveTitleEchoCut(leading[1] || '', lessonTitle);
  if (cut === null) return source;
  const rest = (leading[1] || '').slice(cut).trim();
  const remainder = `${rest}${source.slice(leading[0].length)}`;
  return remainder.replace(/^[\s\n]+/, '');
}

/** 课节正文渲染前的统一 Markdown 规范化（目录解析与正文渲染共用同一套规则） */
export function normalizeLessonMarkdown(source: string, lessonTitle?: string): string {
  return splitGluedHeadings(stripLessonTitleEcho(source || '', lessonTitle));
}

export function renderLessonMarkdown(source: string, lessonTitle?: string): string {
  return renderChatMarkdown(normalizeLessonListMarkdown(normalizeLessonMarkdown(source, lessonTitle)));
}

export function bindLessonMarkdownEnhancements(root: HTMLElement | null) {
  if (!root) return;
  bindMarkdownCodeCopy(root, { renderMermaid: true });
}
