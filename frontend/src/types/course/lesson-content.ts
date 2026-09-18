export type LessonBlockType =
  | 'heading'
  | 'markdown'
  | 'callout'
  | 'resource'
  | 'knowledgePoints'
  | 'quizEntry';

export interface LessonContentDocument {
  version: number;
  blocks: LessonBlock[];
}

export type LessonBlock =
  | HeadingBlock
  | MarkdownBlock
  | CalloutBlock
  | ResourceBlock
  | KnowledgePointsBlock
  | QuizEntryBlock;

export interface HeadingBlock {
  type: 'heading';
  level: number;
  text: string;
}

export interface MarkdownBlock {
  type: 'markdown';
  body: string;
}

export interface CalloutBlock {
  type: 'callout';
  variant: 'objective' | 'tip' | 'warning';
  title: string;
  body: string;
}

export interface ResourceBlock {
  type: 'resource';
  resourceId: number;
  display: 'embed' | 'link';
}

export interface KnowledgePointsBlock {
  type: 'knowledgePoints';
  knowledgePointIds: number[];
}

export interface QuizEntryBlock {
  type: 'quizEntry';
  mode: 'ai_generate';
  knowledgePointIds: number[];
}

export function parseLessonContent(json?: string | null): LessonContentDocument {
  if (!json) {
    return { version: 1, blocks: [] };
  }
  try {
    const parsed = JSON.parse(json) as LessonContentDocument;
    if (!parsed.blocks) {
      return { version: parsed.version || 1, blocks: [] };
    }
    return parsed;
  } catch {
    return { version: 1, blocks: [] };
  }
}

export function serializeLessonContent(doc: LessonContentDocument): string {
  return JSON.stringify(doc);
}
