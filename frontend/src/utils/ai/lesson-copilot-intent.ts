import type { GlobalAssistantMessage } from '@/types/ai/assistant';

export type LessonInsertIntent =
  | 'editor'
  | 'description'
  | 'objective'
  | 'title'
  | 'knowledge';

export function findPrecedingUserMessage(
  messages: GlobalAssistantMessage[],
  msgIndex: number
): GlobalAssistantMessage | null {
  for (let i = msgIndex - 1; i >= 0; i--) {
    const candidate = messages[i];
    if (candidate?.role === 'user') {
      return candidate;
    }
  }
  return null;
}

function findPrecedingUserPrompt(
  messages: GlobalAssistantMessage[],
  msgIndex: number
): string {
  const user = findPrecedingUserMessage(messages, msgIndex);
  return user?.content?.trim() || '';
}

/** 优先使用提问时锁定的意图，再回退到指令/内容推断（参考 Code Compass detectMessageIntent） */
export function resolveLessonInsertIntent(
  msg: GlobalAssistantMessage,
  messages: GlobalAssistantMessage[],
  msgIndex: number
): LessonInsertIntent {
  const userMsg = findPrecedingUserMessage(messages, msgIndex);
  if (userMsg?.lessonInsertIntent) {
    return userMsg.lessonInsertIntent;
  }
  return detectLessonMessageIntent(msg, messages, msgIndex);
}

export function detectLessonMessageIntent(
  msg: GlobalAssistantMessage,
  messages: GlobalAssistantMessage[],
  msgIndex: number
): LessonInsertIntent {
  const content = (msg.content || '').trim();
  const prevPrompt = findPrecedingUserPrompt(messages, msgIndex);
  const instructionPart = (prevPrompt.split(/\n{2,}/)[0] || prevPrompt).toLowerCase();
  const fullPromptLower = prevPrompt.toLowerCase();

  // 用户指令优先：避免「课节导读参考：」等附注误触导读意图
  if (/(?:学习目标|教学目标|学习目标卡)/.test(instructionPart)) {
    return 'objective';
  }
  if (/(?:推荐考点|考查知识点|关联考点|知识标签)/.test(instructionPart)) {
    return 'knowledge';
  }
  if (/(?:课节标题|设为标题|优化.*标题|拟定.*标题)/.test(instructionPart)) {
    return 'title';
  }
  if (
    /(?:提炼|生成|写一段|精炼|润色).*(?:导读|摘要|简介)/.test(instructionPart) ||
    /(?:撰写|生成).*(?:课节导读|课程导读)/.test(instructionPart) ||
    /seo.*导读/.test(instructionPart)
  ) {
    return 'description';
  }
  if (/(?:推荐|挑选|提供|生成|匹配).*(?:考点|知识点)/.test(instructionPart)) {
    return 'knowledge';
  }
  if (/(?:起|推荐|拟定|构思|设为).*(?:标题|课节名)/.test(instructionPart)) {
    return 'title';
  }
  if (
    /(?:撰写|生成|编写|扩写|续写|润色|重构).*(?:教学正文|课节正文|markdown)/.test(fullPromptLower) ||
    /完整教学正文|插入课节编辑器|可直接插入课节编辑器|从零生成初稿/.test(fullPromptLower) ||
    /承接.*续写|润色.*markdown|mermaid|流程图/.test(fullPromptLower)
  ) {
    return 'editor';
  }

  if (/^(?:【?(?:课节)?导读】?|导读[：:])/m.test(content)) {
    return 'description';
  }
  if (/^(?:【?学习目标】?|教学目标[：:])/m.test(content)) {
    return 'objective';
  }
  if (/(?:【推荐考点】|推荐关联考点|核心考点)/m.test(content)) {
    if (/(?:学习目标|教学目标)/.test(fullPromptLower)) {
      return 'objective';
    }
    if (/(?:课节导读|课程导读)/.test(fullPromptLower) && /(?:导读|摘要)/.test(fullPromptLower)) {
      return 'description';
    }
    return 'knowledge';
  }
  if (
    /^[-*•\d.]+\s*(?:理解|掌握|辨析|能够|学会|说明|描述|运用)/m.test(content) &&
    /(?:理解|掌握|能够|辨析)/.test(content) &&
    !/(?:教学正文|课节正文|插入课节编辑器|撰写完整)/.test(fullPromptLower)
  ) {
    return 'objective';
  }

  return 'editor';
}

export function extractCleanLessonDescription(text: string): string {
  if (!text) return '';
  let cleaned = text.trim();
  cleaned = cleaned.replace(
    /^(?:好的|为您|根据|基于|已为您|我为您)?[^：:\n]{0,30}(?:如下|导读如下|摘要如下)[：:]\s*/i,
    ''
  );
  cleaned = cleaned.replace(/^[>#\s]*【?(?:课节)?导读】?[：:\s]*/i, '');
  cleaned = cleaned.replace(/\n+(?:希望对您有所帮助|如有需要|以上导读).*$/s, '');
  cleaned = cleaned.replace(/^>\s*/gm, '');
  cleaned = cleaned.trim();
  if (cleaned.length > 400) {
    cleaned = cleaned.slice(0, 400);
  }
  return cleaned;
}

export function extractCleanLessonTitle(text: string): string {
  if (!text) return '';
  let cleaned = text.trim();
  const bookMatch = cleaned.match(/《([^》]+)》/);
  if (bookMatch?.[1]) {
    return bookMatch[1].trim();
  }
  const lineMatch = cleaned.match(/^(?:1[.\s、]|推荐[：:])\s*([^\n]+)/);
  if (lineMatch?.[1]) {
    return lineMatch[1].replace(/^[#*\s"“”'‘’《]+|[#*\s"“”'‘’》]+$/g, '').trim();
  }
  return cleaned.replace(/^[#*\s"“”'‘’《]+|[#*\s"“”'‘’》]+$/g, '').slice(0, 80).trim();
}

export function extractCleanLessonObjective(text: string): string {
  if (!text) return '';
  let cleaned = text.trim();
  cleaned = cleaned.replace(/^[>#\s]*【?学习目标】?[：:\s]*/i, '');
  cleaned = cleaned.replace(
    /^(?:好的|为您|根据|基于)[^\n]{0,40}(?:如下|目标如下)[：:\s]*\n?/i,
    ''
  );
  cleaned = cleaned.replace(/^---+$/gm, '').replace(/\n{3,}/g, '\n\n').trim();
  if (cleaned.length > 1200) {
    cleaned = cleaned.slice(0, 1200);
  }
  return cleaned;
}

export function extractKnowledgeSuggestions(text: string): string[] {
  if (!text) return [];
  const candidates: string[] = [];

  const summaryMatch = text.match(
    /(?:【?推荐[核心]*考点】?|精选考点|核心考点|推荐关联考点)[：:]\s*([^\n]+)/i
  );
  if (summaryMatch?.[1]) {
    const rawList = summaryMatch[1]
      .replace(/[\[\]【】()（）]/g, ' ')
      .split(/[,，、|/;\s]+/)
      .map((t) => t.trim().replace(/^[-*•#\d.]+|\.+$/g, ''))
      .filter((t) => t.length > 0 && t.length <= 30);
    for (const t of rawList) {
      if (!candidates.includes(t)) candidates.push(t);
    }
  }

  if (candidates.length === 0) {
    const lines = text.split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (/(?:排除|不作为|未涉及|不推荐)/.test(trimmed)) continue;
      const match = trimmed.match(
        /^[-*•\d.]+\s*(?:\*\*)?([A-Za-z0-9\u4e00-\u9fa5.\-_+]{1,24})(?:\*\*)?\s*[:：\-—]/
      );
      if (match?.[1]) {
        const tag = match[1].trim();
        if (tag && !candidates.includes(tag)) candidates.push(tag);
      }
    }
  }

  return candidates.slice(0, 8);
}

export function matchKnowledgePointIds(
  suggestions: string[],
  knowledgePoints: Array<{ id: number; title?: string; name?: string }>
): number[] {
  const ids: number[] = [];
  for (const s of suggestions) {
    const norm = s.trim().toLowerCase();
    if (!norm) continue;
    const hit = knowledgePoints.find((kp) => {
      const title = (kp.title || kp.name || '').trim().toLowerCase();
      return title === norm || title.includes(norm) || norm.includes(title);
    });
    if (hit && !ids.includes(hit.id)) ids.push(hit.id);
  }
  return ids;
}
