import type { LessonBlock } from '@/types/course/lesson-content';
import { normalizeChatMarkdown } from '@/utils/ai/chat-markdown';
import { normalizeLessonListMarkdown, normalizeLessonMarkdown } from '@/utils/format/lesson-markdown';

export interface LessonTocItem {
  id: string;
  title: string;
  /** 用于侧栏缩进，通常 2–4 */
  level: number;
}

const MD_HEADING_RE = /^(#{1,6})\s+(.+)$/gm;

/** 与正文渲染完全一致的预处理后再解析 # 标题（粘连标题拆分、课节标题回声剥离都要生效） */
function normalizeForHeadingParse(body: string, lessonTitle?: string): string {
  return normalizeChatMarkdown(
    normalizeLessonListMarkdown(normalizeLessonMarkdown(body || '', lessonTitle))
  );
}

function stripHeadingTitle(raw: string): string {
  return raw
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[*_`~]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function parseMarkdownHeadings(body: string, lessonTitle?: string): { level: number; title: string }[] {
  const items: { level: number; title: string }[] = [];
  const text = normalizeForHeadingParse(body, lessonTitle);
  let match: RegExpExecArray | null;
  MD_HEADING_RE.lastIndex = 0;
  while ((match = MD_HEADING_RE.exec(text))) {
    const hashes = match[1].length;
    const title = stripHeadingTitle(match[2]);
    if (!title) continue;
    const level = Math.min(4, Math.max(2, hashes));
    items.push({ level, title });
  }
  return items;
}

/** 常见理科/通识教学概念小节前缀 */
const TEACHING_CONCEPT_PREFIX_RE =
  /^(?:定理|公理|定义|准则|法则|公式|推论|引理|性质|判据|判别法|考点|方法|技巧)/;

/** 常见正文陈述句引导词（在这些词之前截断为纯标题） */
const TITLE_BODY_BOUNDARY_KEYWORDS = [
  '设', '若', '当', '如果', '对于', '除定义', '关注', '其依据', '第一个', '第1个'
];

function isPseudoHeadingParagraph(p: HTMLParagraphElement): boolean {
  const text = (p.textContent || '').replace(/\s+/g, ' ').trim();
  if (!text || text.length > 140) return false;
  if (p.closest('blockquote, li, td, th')) return false;

  const firstEl = p.firstElementChild;
  const strong =
    firstEl && (firstEl.tagName === 'STRONG' || firstEl.tagName === 'B')
      ? firstEl
      : p.querySelector('strong, b');
  const probe =
    strong && (p.children.length === 1 || !p.textContent?.trim().startsWith(' '))
      ? (strong.textContent || '').trim()
      : text;

  if (!probe || probe.length > 100) return false;

  // 1. 中文序号大节（一、二、）
  if (/^[一二三四五六七八九十百千]+[、．.]/.test(probe)) return true;
  // 2. 章节（第一章、第二节）
  if (/^第[一二三四五六七八九十\d]+[章节部分]/.test(probe)) return true;
  // 3. 多级数字编号（1.1、3.2）
  if (/^\d+\.\d+(\s|：|:|、|．|.)?/.test(probe)) return true;
  // 4. 理科教学核心概念（定理、定义、性质、法则等，如「定理（0/0型）」、「夹逼准则」）
  if (TEACHING_CONCEPT_PREFIX_RE.test(probe)) return true;
  // 5. 例题 / 变式 / 重点标记
  if (/^(?:例\d+|变式)/.test(probe)) return true;
  // 6. 单数字序号条目（如 1. 数列极限、2. 函数极限、3. 极限存在的常用判别）
  if (/^\d+\.[ \t]*[\u4e00-\u9fa5A-Za-z\$\\]/.test(probe)) {
    if (strong || text.length <= 90 || /[:：。]/.test(text.slice(0, 30))) {
      return true;
    }
  }

  return false;
}

function headingLevelFromElement(el: HTMLElement, titleText?: string): number {
  const tag = el.tagName;
  if (/^H[1-6]$/.test(tag)) {
    return Math.min(4, Math.max(2, parseInt(tag[1], 10)));
  }
  const t = (titleText || el.textContent || '').trim();
  if (/^(\d+\.\d+|\d+\.|例\d+|变式|定理|公理|定义|准则|法则|公式|推论|引理|性质|判据)/.test(t)) return 3;
  return 2;
}

/**
 * 从 DOM 节点提取干净的标题文本：
 * 1. 若为段落 <p> 且含有开头的 <strong>/<b>，优先提取加粗部分的标题，避免将后文大段正文全部吸入目录；
 * 2. 处理已渲染的 KaTeX 节点（.katex）：
 *    从 annotation[encoding="application/x-tex"] 提取最准确的原生 LaTeX 源码并包装为 $...$ 文本；
 * 3. 对定理带括号名称（如「定理 ($0/0$型)」）或后文说明性长句进行分界截断，保证目录干净整洁。
 */
export function extractCleanTitleFromElement(el: HTMLElement): string {
  let targetNode: Node = el;

  if (el.tagName === 'P') {
    const firstEl = el.firstElementChild;
    const strong =
      firstEl && (firstEl.tagName === 'STRONG' || firstEl.tagName === 'B')
        ? firstEl
        : el.querySelector('strong, b');
    if (strong) {
      targetNode = strong;
    }
  }

  // 克隆节点进行 KaTeX 清洗，避免污染页面真实 DOM
  const clone = targetNode.cloneNode(true) as HTMLElement;

  const katexList = clone.querySelectorAll('.katex');
  katexList.forEach((kEl) => {
    const annotation = kEl.querySelector('annotation[encoding="application/x-tex"]');
    let tex = annotation?.textContent?.trim();
    if (!tex) {
      const mathml = kEl.querySelector('.katex-mathml');
      if (mathml) mathml.remove();
      tex = kEl.textContent?.trim() || '';
    }
    if (tex) {
      const formula = tex.startsWith('$') ? tex : `$${tex}$`;
      kEl.replaceWith(document.createTextNode(formula));
    } else {
      kEl.remove();
    }
  });

  let raw = (clone.textContent || '').replace(/\s+/g, ' ').trim();

  // 仅对普通段落 P 伪标题进行正文截断，原生 H1~H6 保留完整标题（含冒号副标题）
  if (el.tagName === 'P') {
    // 1. 定理/定义后带括号的完整名词提取（如「定理 ($0/0$型) 设 f,g...」-> 提取「定理 ($0/0$型)」）
    const theoremMatch = raw.match(/^(?:定理|公理|定义|准则|法则|公式|推论|引理|性质|判据|判别法)[\s]*[(（\[][^)）\]]+[)）\]]/);
    if (theoremMatch) {
      raw = theoremMatch[0].trim();
    } else {
      // 2. 序号小标题后紧跟重复主语正文（如「2. 函数极限函数极限关注...」-> 提取「2. 函数极限」）
      const repeatMatch = raw.match(/^(\d+\.[ \t]*([\u4e00-\u9fa5]{2,8}))\2/);
      if (repeatMatch) {
        raw = repeatMatch[1].trim();
      } else {
        // 3. 冒号、句号等明确标点截断（如「3. 极限存在的常用判别：除定义外...」->「3. 极限存在的常用判别」）
        const punctBoundary = raw.search(/[:：。]/);
        if (punctBoundary > 0 && punctBoundary <= 35) {
          raw = raw.slice(0, punctBoundary).trim();
        } else {
          // 4. 针对无标点粘连长句（如「3.极限存在的常用判别除定义外，本节常用」或「1.夹逼准则若在某一变化过程中」）
          for (const kw of TITLE_BODY_BOUNDARY_KEYWORDS) {
            const kwIdx = raw.indexOf(kw);
            if (kwIdx >= 4 && kwIdx <= 22) {
              raw = raw.slice(0, kwIdx).trim();
              break;
            }
          }
          // 5. 若依然过长（超过 24 字且带有逗号），截断到第一个逗号
          if (raw.length > 24) {
            const commaIdx = raw.search(/[,，]/);
            if (commaIdx >= 5 && commaIdx <= 22) {
              raw = raw.slice(0, commaIdx).trim();
            }
          }
        }
      }
    }
  }

  // 清理尾部多余的孤立标点（如冒号或破折号）
  raw = raw.replace(/[:：\-\s]+$/, '').trim();

  return raw;
}

function collectHeadingsInMarkdownRoot(mdRoot: Element): HTMLElement[] {
  const nodes: HTMLElement[] = [];
  mdRoot.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(h => {
    nodes.push(h as HTMLElement);
  });
  mdRoot.querySelectorAll('p').forEach(p => {
    if (isPseudoHeadingParagraph(p as HTMLParagraphElement)) {
      nodes.push(p as HTMLParagraphElement);
    }
  });
  nodes.sort((a, b) => {
    const pos = a.compareDocumentPosition(b);
    if (pos & Node.DOCUMENT_POSITION_FOLLOWING) return -1;
    if (pos & Node.DOCUMENT_POSITION_PRECEDING) return 1;
    return 0;
  });
  return nodes;
}

/**
 * 根据页面已渲染的正文生成目录并写入锚点 id（与左侧正文所见一致）
 */
export function applyLessonTocFromDom(container: HTMLElement | null): LessonTocItem[] {
  if (!container) return [];

  const items: LessonTocItem[] = [];
  let seq = 0;
  const blockEls = Array.from(container.children).filter(el =>
    el.classList.contains('lesson-block')
  );

  blockEls.forEach(block => {
    const blockEl = block as HTMLElement;

    const objective = Array.from(blockEl.children).find(c =>
      c.classList.contains('lesson-callout--objective')
    );
    if (objective) {
      const title =
        blockEl.querySelector('.callout-title')?.textContent?.trim() || '学习目标';
      const id = `lesson-toc-${seq++}`;
      blockEl.id = id;
      items.push({ id, title, level: 2 });
      const innerMd = objective.querySelector('.markdown-body');
      if (innerMd) {
        for (const el of collectHeadingsInMarkdownRoot(innerMd)) {
          const t = extractCleanTitleFromElement(el);
          if (!t) continue;
          const hid = `lesson-toc-${seq++}`;
          el.id = hid;
          el.classList.add('lesson-toc-anchor');
          items.push({ id: hid, title: t, level: headingLevelFromElement(el, t) });
        }
      }
      return;
    }

    const blockHeading = Array.from(blockEl.children).find(c =>
      c.classList.contains('block-heading')
    );
    if (blockHeading) {
      const title = extractCleanTitleFromElement(blockHeading as HTMLElement);
      if (title) {
        const id = `lesson-toc-${seq++}`;
        blockHeading.id = id;
        items.push({ id, title, level: headingLevelFromElement(blockHeading as HTMLElement, title) });
      }
    }

    const mdRoots = Array.from(blockEl.children).filter(c =>
      c.classList.contains('markdown-body')
    );
    mdRoots.forEach(mdRoot => {
      for (const el of collectHeadingsInMarkdownRoot(mdRoot)) {
        const title = extractCleanTitleFromElement(el);
        if (!title) continue;
        const id = `lesson-toc-${seq++}`;
        el.id = id;
        el.classList.add('lesson-toc-anchor');
        items.push({ id, title, level: headingLevelFromElement(el, title) });
      }
    });

    const callout = Array.from(blockEl.children).find(
      c => c.classList.contains('lesson-callout') && !c.classList.contains('lesson-callout--objective')
    );
    if (callout) {
      const titleEl = callout.querySelector('.callout-title') as HTMLElement | null;
      const title = titleEl ? extractCleanTitleFromElement(titleEl) : '';
      if (title) {
        const id = `lesson-toc-${seq++}`;
        if (titleEl) titleEl.id = id;
        items.push({ id, title, level: 2 });
      }
    }
  });

  return items;
}

/** 块 JSON 预解析目录（渲染前占位，渲染后以 DOM 为准） */
export function buildLessonToc(blocks: LessonBlock[], lessonTitle?: string): LessonTocItem[] {
  const items: LessonTocItem[] = [];
  let seq = 0;

  for (const block of blocks) {
    if (block.type === 'callout' && block.variant === 'objective') {
      const title = (block.title || '学习目标').trim();
      if (title) {
        items.push({
          id: `lesson-toc-${seq++}`,
          title,
          level: 2
        });
      }
      continue;
    }
    if (block.type === 'heading') {
      const title = (block.text || '').trim();
      if (title) {
        const level = Math.min(4, Math.max(2, block.level || 2));
        items.push({ id: `lesson-toc-${seq++}`, title, level });
      }
      continue;
    }
    if (block.type === 'markdown') {
      for (const h of parseMarkdownHeadings(block.body, lessonTitle)) {
        items.push({
          id: `lesson-toc-${seq++}`,
          title: h.title,
          level: h.level
        });
      }
    }
  }

  return items;
}

/** @deprecated 使用 applyLessonTocFromDom */
export function bindLessonTocAnchors(container: HTMLElement | null, _blocks: LessonBlock[]) {
  applyLessonTocFromDom(container);
}
