import type { Question } from '@/types/question/question';

const TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
};

const DIFF_LABELS: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
};

/** 供全局 AI 侧边栏锚定题库试题的完整上下文文本 */
export function buildQuestionCopilotExcerpt(question: Question): string {
  const lines: string[] = [];
  if (question.id != null) {
    lines.push(`题目ID: ${question.id}`);
  }
  if (question.courseName) {
    lines.push(`所属课程: ${question.courseName}`);
  } else if (question.courseId != null) {
    lines.push(`课程ID: ${question.courseId}`);
  }
  if (question.chapterName) {
    lines.push(`章节: ${question.chapterName}`);
  }
  const typeLabel = question.typeLabel || TYPE_LABELS[question.type] || question.type;
  const diffLabel = question.difficultyLabel || DIFF_LABELS[question.difficulty] || question.difficulty;
  lines.push(`题型: ${typeLabel}`);
  lines.push(`难度: ${diffLabel}`);
  lines.push(`分值: ${question.score ?? '-'} 分`);
  if (question.cognitiveLevel) {
    lines.push(`认知层次: ${question.cognitiveLevel}`);
  }
  if (question.knowledgePointNames?.length) {
    lines.push(`关联知识点: ${question.knowledgePointNames.join('、')}`);
  }
  lines.push('');
  lines.push('【题干】');
  lines.push(question.stem?.trim() || '（无题干）');

  const options = Array.isArray(question.options) ? question.options : [];
  if (options.length > 0) {
    lines.push('');
    lines.push('【选项】');
    for (const opt of options) {
      const mark = opt.isCorrect ? '（正确）' : '';
      lines.push(`${opt.key}. ${opt.content}${mark}`);
    }
  }

  if (question.correctAnswer) {
    lines.push('');
    lines.push(`【参考答案】${question.correctAnswer}`);
  }

  if (question.analysis?.trim()) {
    lines.push('');
    lines.push('【官方解析】');
    lines.push(question.analysis.trim());
  }

  if (question.distractorAnalysis?.trim()) {
    lines.push('');
    lines.push('【干扰项剖析】');
    lines.push(question.distractorAnalysis.trim());
  }

  return lines.join('\n');
}

export function buildQuestionCopilotTitle(question: Question): string {
  const stem = question.stem?.replace(/\s+/g, ' ').trim() || '题库试题';
  return stem.length > 48 ? `${stem.slice(0, 48)}…` : stem;
}
