import type { LessonBlock } from '@/types/course/lesson-content';
import { normalizeChatMarkdown } from '@/utils/ai/chat-markdown';
import { normalizeLessonListMarkdown } from '@/utils/format/lesson-markdown';

export interface LessonTocItem {
  id: string;
  title: string;
  /** 用于侧栏缩进，通常 2–4 */
  level: number;
}

const MD_HEADING_RE = /^(#{1,6})\s+(.+)$/gm;

/** 与正文渲染一致的预处理后再解析 # 标题 */
function normalizeForHeadingParse(body: string): string {
  return normalizeChatMarkdown(normalizeLessonListMarkdown(body || ''));
}

function stripHeadingTitle(raw: string): string {
  return raw
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[*_`~]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function parseMarkdownHeadings(body: string): { level: number; title: string }[] {
  const items: { level: number; title: string }[] = [];
  const text = normalizeForHeadingParse(body);
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

function isPseudoHeadingParagraph(p: HTMLParagraphElement): boolean {
  const text = (p.textContent || '').replace(/\s+/g, ' ').trim();
  if (!text || text.length > 120) return false;
  if (p.closest('blockquote, li, td, th')) return false;

  const strong = p.querySelector(':scope > strong');
  const probe =
    strong && p.children.length === 1 ? (strong.textContent || '').trim() : text;

  if (!probe || probe.length > 100) return false;

  return (
    /^[一二三四五六七八九十百千]+[、．.]/.test(probe) ||
    /^第[一二三四五六七八九十\d]+[章节部分]/.test(probe) ||
    /^\d+\.\d+(\s|：|:|、|．|.)?/.test(probe) ||
    (/^\*\*/.test(text) === false &&
      !!strong &&
      /^(?:\d+\.|[一二三四五六七八九十]+[、．.])/.test(probe))
  );
}

function headingLevelFromElement(el: HTMLElement): number {
  const tag = el.tagName;
  if (/^H[1-6]$/.test(tag)) {
    return Math.min(4, Math.max(2, parseInt(tag[1], 10)));
  }
  const t = (el.textContent || '').trim();
  if (/^\d+\.\d+/.test(t)) return 3;
  return 2;
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
  const blockEls = container.querySelectorAll(':scope > .lesson-block');

  blockEls.forEach(block => {
    const blockEl = block as HTMLElement;

    const objective = blockEl.querySelector(':scope > .lesson-callout--objective');
    if (objective) {
      const title =
        blockEl.querySelector('.callout-title')?.textContent?.trim() || '学习目标';
      const id = `lesson-toc-${seq++}`;
      blockEl.id = id;
      items.push({ id, title, level: 2 });
      const innerMd = objective.querySelector('.markdown-body');
      if (innerMd) {
        for (const el of collectHeadingsInMarkdownRoot(innerMd)) {
          const t = el.textContent?.trim();
          if (!t) continue;
          const hid = `lesson-toc-${seq++}`;
          el.id = hid;
          el.classList.add('lesson-toc-anchor');
          items.push({ id: hid, title: t, level: headingLevelFromElement(el) });
        }
      }
      return;
    }

    const blockHeading = blockEl.querySelector(':scope > .block-heading');
    if (blockHeading) {
      const title = blockHeading.textContent?.trim();
      if (title) {
        const id = `lesson-toc-${seq++}`;
        blockHeading.id = id;
        items.push({ id, title, level: headingLevelFromElement(blockHeading as HTMLElement) });
      }
    }

    const mdRoots = blockEl.querySelectorAll(':scope > .markdown-body');
    mdRoots.forEach(mdRoot => {
      for (const el of collectHeadingsInMarkdownRoot(mdRoot)) {
        const title = el.textContent?.trim();
        if (!title) continue;
        const id = `lesson-toc-${seq++}`;
        el.id = id;
        el.classList.add('lesson-toc-anchor');
        items.push({ id, title, level: headingLevelFromElement(el) });
      }
    });

    const callout = blockEl.querySelector(':scope > .lesson-callout:not(.lesson-callout--objective)');
    if (callout) {
      const titleEl = callout.querySelector('.callout-title');
      const title = titleEl?.textContent?.trim();
      if (title) {
        const id = `lesson-toc-${seq++}`;
        (titleEl as HTMLElement).id = id;
        items.push({ id, title, level: 2 });
      }
    }
  });

  return items;
}

/** 块 JSON 预解析目录（渲染前占位，渲染后以 DOM 为准） */
export function buildLessonToc(blocks: LessonBlock[]): LessonTocItem[] {
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
      for (const h of parseMarkdownHeadings(block.body)) {
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
