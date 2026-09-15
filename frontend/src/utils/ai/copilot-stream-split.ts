export interface CopilotStreamSplit {
  thinking: string;
  answer: string;
  inThinkingPhase: boolean;
}

const THINK_HEADERS = [
  '## 简要思考',
  '### 简要思考',
  '# 简要思考',
  '【简要思考】',
  '## 思考过程',
  '### 思考过程',
  '## 思考',
  '### 思考'
];

const ANSWER_HEADERS = [
  '## 回答',
  '### 回答',
  '# 回答',
  '【回答】',
  '## 答案',
  '### 答案',
  '## 解决方案',
  '## 代码实现'
];

export function splitCopilotStream(raw: string): CopilotStreamSplit {
  const text = raw || '';
  if (!text.trim()) {
    return { thinking: '', answer: '', inThinkingPhase: false };
  }

  let thinkStart = -1;
  let thinkHeaderLen = 0;
  for (const h of THINK_HEADERS) {
    const idx = text.indexOf(h);
    if (idx >= 0 && (thinkStart === -1 || idx < thinkStart)) {
      thinkStart = idx;
      thinkHeaderLen = h.length;
    }
  }

  if (thinkStart === -1) {
    let cleanAnswer = text;
    cleanAnswer = cleanAnswer.replace(/^#{1,3}\s*(?:回答|答案|解决方案)[:：]?\s*/i, '');
    return { thinking: '', answer: cleanAnswer, inThinkingPhase: false };
  }

  let answerStart = -1;
  let answerHeaderLen = 0;
  for (const h of ANSWER_HEADERS) {
    const idx = text.indexOf(h);
    if (idx >= 0 && (answerStart === -1 || idx < answerStart)) {
      answerStart = idx;
      answerHeaderLen = h.length;
    }
  }

  if (answerStart >= 0 && answerStart >= thinkStart) {
    const thinkingBlock = text.slice(thinkStart + thinkHeaderLen, answerStart).trim();
    const answerBlock = text.slice(answerStart + answerHeaderLen).trim();
    return { thinking: thinkingBlock, answer: answerBlock, inThinkingPhase: false };
  }

  const afterThink = text.slice(thinkStart + thinkHeaderLen);
  const codeIdx = afterThink.indexOf('```');
  if (codeIdx >= 0) {
    return {
      thinking: afterThink.slice(0, codeIdx).trim(),
      answer: afterThink.slice(codeIdx).trim(),
      inThinkingPhase: false
    };
  }

  return { thinking: afterThink.trim(), answer: '', inThinkingPhase: true };
}

export function cleanReasoningText(raw: string): string {
  if (!raw) return '';
  return raw.replace(/\n{3,}/g, '\n\n').trim();
}

/** 手动停止生成时展示在回答区的提示文案 */
export const STOPPED_GENERATION_MARKER = '*(已停止生成)*';

export function buildStoppedGenerationContent(answer: string): string {
  const trimmed = answer.trim();
  return trimmed ? `${trimmed}\n\n${STOPPED_GENERATION_MARKER}` : STOPPED_GENERATION_MARKER;
}

export function formatInlineMarkdown(text: string): string {
  if (!text) return '';
  let s = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  s = s.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
  s = s.replace(/\n/g, '<br/>');
  return s;
}
