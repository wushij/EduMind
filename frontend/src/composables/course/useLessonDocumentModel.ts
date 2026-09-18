import type {
  CalloutBlock,
  LessonBlock,
  LessonContentDocument
} from '@/types/course/lesson-content';

export interface ObjectiveCalloutState {
  title: string;
  body: string;
}

export interface LessonStudioDocument {
  mainMarkdown: string;
  objectiveCallout: ObjectiveCalloutState;
  extraBlocks: LessonBlock[];
  knowledgePointIds: number[];
}

export function createDefaultObjectiveCallout(): ObjectiveCalloutState {
  return {
    title: '学习目标',
    body: '学完本节后，你将能够掌握本课核心概念并完成相关练习。'
  };
}

export function createDefaultStudioDocument(): LessonStudioDocument {
  return {
    mainMarkdown: '',
    objectiveCallout: createDefaultObjectiveCallout(),
    extraBlocks: [],
    knowledgePointIds: []
  };
}

function isObjectiveCallout(block: LessonBlock): block is CalloutBlock {
  return block.type === 'callout' && block.variant === 'objective';
}

function isMainMarkdownCandidate(block: LessonBlock, index: number, blocks: LessonBlock[]): boolean {
  if (block.type !== 'markdown') return false;
  const firstMd = blocks.findIndex(b => b.type === 'markdown');
  return index === firstMd;
}

export function blocksToStudio(
  doc: LessonContentDocument,
  knowledgePointIds: number[] = []
): LessonStudioDocument {
  const blocks = doc.blocks || [];
  if (blocks.length === 0) {
    return {
      mainMarkdown: '',
      objectiveCallout: { title: '学习目标', body: '' },
      extraBlocks: [],
      knowledgePointIds
    };
  }

  let mainMarkdown = '';
  let objectiveCallout = createDefaultObjectiveCallout();
  const extraBlocks: LessonBlock[] = [];
  let objectiveTaken = false;
  let mainMdTaken = false;

  blocks.forEach((block, index) => {
    if (!objectiveTaken && isObjectiveCallout(block)) {
      objectiveCallout = {
        title: block.title || '学习目标',
        body: block.body || ''
      };
      objectiveTaken = true;
      return;
    }
    if (block.type === 'markdown') {
      if (!mainMdTaken && isMainMarkdownCandidate(block, index, blocks)) {
        mainMarkdown = block.body || '';
        mainMdTaken = true;
      } else {
        extraBlocks.push(block);
      }
      return;
    }
    if (block.type === 'knowledgePoints') {
      const ids = block.knowledgePointIds || [];
      if (ids.length) {
        knowledgePointIds = ids;
      }
      return;
    }
    extraBlocks.push(block);
  });

  if (!mainMdTaken) {
    const extraMd = blocks.filter(b => b.type === 'markdown') as Array<{ type: 'markdown'; body: string }>;
    if (extraMd.length) {
      mainMarkdown = extraMd.map(m => m.body).join('\n\n');
    }
  }

  return {
    mainMarkdown,
    objectiveCallout,
    extraBlocks,
    knowledgePointIds
  };
}

export function studioToBlocks(studio: LessonStudioDocument): LessonContentDocument {
  const blocks: LessonBlock[] = [];

  const objectiveBody = studio.objectiveCallout.body?.trim();
  const objectiveTitle = studio.objectiveCallout.title?.trim() || '学习目标';
  if (objectiveTitle || objectiveBody) {
    blocks.push({
      type: 'callout',
      variant: 'objective',
      title: objectiveTitle,
      body: objectiveBody || ''
    });
  }

  blocks.push({
    type: 'markdown',
    body: studio.mainMarkdown || ''
  });

  if (studio.knowledgePointIds.length > 0) {
    blocks.push({
      type: 'knowledgePoints',
      knowledgePointIds: [...studio.knowledgePointIds]
    });
  }

  for (const block of studio.extraBlocks) {
    if (block.type === 'knowledgePoints') {
      continue;
    }
    if (block.type !== 'markdown' && !isObjectiveCallout(block)) {
      blocks.push(block);
    }
  }

  return { version: 1, blocks };
}
