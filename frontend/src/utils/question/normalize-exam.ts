import type { ExamPaper, ExamRule } from '@/types/question/exam';
import type { QuestionItem } from '@/types/question/question';
import { normalizeQuestion } from '@/utils/question/normalize-question';

const QUESTION_TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单项选择题',
  MULTIPLE_CHOICE: '多项选择题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '解答题'
};

function buildRulesFromQuestions(raw: Record<string, any>): ExamRule[] {
  const questions = Array.isArray(raw.questions) ? raw.questions : [];
  if (questions.length === 0) {
    return [];
  }

  const grouped = new Map<string, { count: number; totalScore: number }>();

  questions.forEach((item: Record<string, any>) => {
    const question = item.question || item;
    const type = String(question.type || question.questionType || 'SHORT_ANSWER');
    const score = Number(item.score ?? question.score ?? 0);
    const current = grouped.get(type) || { count: 0, totalScore: 0 };
    current.count += 1;
    current.totalScore += Number.isFinite(score) ? score : 0;
    grouped.set(type, current);
  });

  return Array.from(grouped.entries()).map(([type, stat]) => ({
    type,
    label: QUESTION_TYPE_LABELS[type] || '综合题',
    count: stat.count,
    scoreEach: stat.count > 0 ? Number((stat.totalScore / stat.count).toFixed(1)) : 0
  }));
}

/** 将 ExamQuestionVO（含嵌套 question）或扁平题目转为 QuestionItem[] */
export function flattenExamQuestionItems(rawQuestions: unknown[]): QuestionItem[] {
  if (!Array.isArray(rawQuestions)) {
    return [];
  }
  const items = rawQuestions.map((row, index) => {
    const record = row as Record<string, unknown>;
    const nested =
      record.question && typeof record.question === 'object'
        ? (record.question as Record<string, unknown>)
        : record;
    const score = record.score ?? nested.score;
    const sortOrder = Number(record.sortOrder ?? index);
    const normalized = normalizeQuestion({ ...nested, score });
    return { ...normalized, sortOrder } as QuestionItem & { sortOrder?: number };
  });
  return items
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map(({ sortOrder: _sort, ...q }) => q);
}

export function normalizeExamPaper(raw: Record<string, any>): ExamPaper {
  const rules = Array.isArray(raw.rules) && raw.rules.length > 0
    ? raw.rules
    : buildRulesFromQuestions(raw);

  const rawQuestions = Array.isArray(raw.questions) ? raw.questions : [];

  return {
    id: Number(raw.id),
    courseId: Number(raw.courseId || 0),
    courseName: raw.courseName || raw.courseTitle || (raw.courseId ? `课程 #${raw.courseId}` : '未关联课程'),
    title: raw.title || '未命名试卷',
    semester: raw.semester || '当前学期',
    totalScore: Number(raw.totalScore ?? 100),
    durationMinutes: Number(raw.durationMinutes ?? 90),
    passScore: Number(raw.passScore ?? 60),
    rules,
    questions: flattenExamQuestionItems(rawQuestions),
    createdAt: raw.createdAt || raw.createTime || '',
    status: raw.status
  };
}

export function normalizeExamList(list: Record<string, any>[] = []): ExamPaper[] {
  return list.map(normalizeExamPaper);
}
