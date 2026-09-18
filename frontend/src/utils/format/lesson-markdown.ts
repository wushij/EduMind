import { bindMarkdownCodeCopy } from '@/utils/markdown';
import { normalizeDisplayListMarkdown, renderChatMarkdown } from '@/utils/ai/chat-markdown';

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

export function renderLessonMarkdown(source: string): string {
  return renderChatMarkdown(normalizeLessonListMarkdown(source || ''));
}

export function bindLessonMarkdownEnhancements(root: HTMLElement | null) {
  if (!root) return;
  bindMarkdownCodeCopy(root, { renderMermaid: true });
}
