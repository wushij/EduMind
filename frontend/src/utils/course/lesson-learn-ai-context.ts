import type { LessonContentDocument } from '@/types/course/lesson-content';

/** 与 Code Compass 写作伴侣一致：正文节选仅作 Prompt 补充，完整知识靠课程知识库 RAG */
const DRAFT_EXCERPT_MAX = 1200;
const OBJECTIVE_EXCERPT_MAX = 600;

/** 供全局助教隐式锚定：用户提问不写课节名，正文摘录走 draftExcerpt */
export function buildLessonLearnAiExcerpt(doc: LessonContentDocument): {
  draftExcerpt: string;
  objectiveExcerpt: string;
} {
  const blocks = doc.blocks || [];
  let objectiveExcerpt = '';
  const markdownParts: string[] = [];

  for (const block of blocks) {
    if (block.type === 'callout' && block.variant === 'objective') {
      const parts = [block.title, block.body].filter(Boolean);
      objectiveExcerpt = parts.join('\n').trim().slice(0, OBJECTIVE_EXCERPT_MAX);
      continue;
    }
    if (block.type === 'markdown' && block.body?.trim()) {
      markdownParts.push(block.body.trim());
    }
    if (block.type === 'heading' && block.text?.trim()) {
      markdownParts.push(`## ${block.text.trim()}`);
    }
  }

  return {
    draftExcerpt: markdownParts.join('\n\n').slice(0, DRAFT_EXCERPT_MAX),
    objectiveExcerpt
  };
}
