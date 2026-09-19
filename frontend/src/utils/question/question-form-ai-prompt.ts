import type { Question } from '@/types/question/question';

export type QuestionFormAiThinkingMode = 'fullAuto' | 'tutor' | 'options' | 'analysis' | 'polish';

const MODE_TASK: Record<QuestionFormAiThinkingMode, string> = {
  polish:
    '【任务】仅对已有题干做学术规范化润色：消除语病与歧义，规范设问问法与全角括号「（ ）」；保持原考查意图、知识点与难度不变；禁止更换学科或重写为另一道题。',
  options:
    '【任务】基于下方题干生成选择题选项：1 个正确选项 + 3 个高迷惑性干扰项；干扰项须针对该题常见概念混淆，禁止与题干考点无关的模板题（例如题干为数学时不得输出 Java/数据结构选项）。',
  analysis:
    '【任务】针对下方题干与给定正确答案，撰写深度教学解析，结构须包含【核心考点】【解题推导步骤】【易错思维误区】；不得编造与题干无关的考点。',
  fullAuto:
    '【任务】在保留教师题干核心意图的前提下全套装配：润色题干、匹配选项（含正确答案与干扰项）、输出深度解析、给出 knowledgePointName 与合理 difficulty；全程不得偷换学科或套用无关模板题。',
  tutor:
    '【任务】教研质检：评估命题科学性、设问严密性、选项区分度；在 analysis 中输出结构化报告（综合评分 0–100、诊断摘要、至少 3 条可执行优化建议、推荐润色题干与解析要点）；stem 字段可给出优化后的题干草案。'
};

function buildContextBlock(form: Partial<Question>, courseLabel: string): string {
  const type = form.type || 'SINGLE_CHOICE';
  const lines = [
    `【课程】${courseLabel}`,
    `【题型】${type}`,
    `【难度】${form.difficulty || 'MEDIUM'}`,
    `【默认分值】${form.score ?? 5} 分`
  ];
  if (form.knowledgePointNames?.length) {
    lines.push(`【已关联考点】${form.knowledgePointNames.join('、')}`);
  }
  lines.push(`【题干】${(form.stem || '').trim()}`);
  if (form.correctAnswer) {
    lines.push(`【当前正确答案】${form.correctAnswer}`);
  }
  if (form.options?.length && (type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE')) {
    lines.push(
      `【当前选项】${form.options.map((o) => `${o.key}.${o.content}`).join('；')}`
    );
  }
  return lines.join('\n');
}

const HARD_CONSTRAINTS = `
【硬性约束（必须遵守）】
1. 输出须符合系统提示中的 JSON Schema（questions 数组，单题时长度为 1）。
2. 必须严格围绕教师给出的题干与课程命题，禁止生成与题干学科/考点无关的内容。
3. 禁止 Mock、占位符、示例套话或与任务无关的寒暄。
4. 数学/物理公式使用 LaTeX：行内 $...$，独立块 $$...$$。
5. 选择题 options 须为 A/B/C/D 四项（或多选按规定），每项 content 必须为非空选项正文（可含 LaTeX），answer 字段为正确选项字母。
6. 禁止只输出 analysis / 解题思路而不输出 options；干扰项说明可写在 distractorAnalysis，但选项正文必须出现在 options 数组内。
`.trim();

/** 录入新题页 AI 辅助：统一用户侧命题指令（与后端 question_generate 系统 Prompt 叠加） */
export function buildQuestionFormAiPromptDirective(
  mode: QuestionFormAiThinkingMode,
  form: Partial<Question>,
  courseLabel: string
): string {
  return [MODE_TASK[mode], buildContextBlock(form, courseLabel), HARD_CONSTRAINTS].join('\n\n');
}
