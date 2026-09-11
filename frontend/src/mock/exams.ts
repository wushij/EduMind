import { Question, MOCK_QUESTIONS } from './questions';

import type { ExamRule, Exam, ExamPaper, ExamRuleConfig } from '@/types/question/exam';

export type { ExamRule, Exam, ExamPaper, ExamRuleConfig };

export const MOCK_EXAMS: Exam[] = [
  {
    id: 501,
    courseId: 101,
    courseName: '大学数学：高等数学（上）',
    title: '2026秋季学期高等数学期中统一水平测试卷',
    semester: '2026秋季学期',
    totalScore: 100,
    durationMinutes: 90,
    passScore: 60,
    rules: [
      { type: 'SINGLE_CHOICE', label: '单项选择题', count: 10, scoreEach: 3 },
      { type: 'FILL_BLANK', label: '填空题', count: 5, scoreEach: 4 },
      { type: 'SHORT_ANSWER', label: '解答与推导题', count: 4, scoreEach: 12.5 }
    ],
    questions: [...MOCK_QUESTIONS],
    createdAt: '2026-09-10'
  },
  {
    id: 502,
    courseId: 102,
    courseName: '计算机核心：数据结构与算法',
    title: '数据结构与算法分析阶段性上机诊断试卷',
    semester: '2026秋季学期',
    totalScore: 100,
    durationMinutes: 100,
    passScore: 60,
    rules: [
      { type: 'SINGLE_CHOICE', label: '单项选择题', count: 10, scoreEach: 3 },
      { type: 'MULTIPLE_CHOICE', label: '多项选择题', count: 5, scoreEach: 4 },
      { type: 'SHORT_ANSWER', label: '算法设计与分析题', count: 3, scoreEach: 16.6 }
    ],
    questions: [...MOCK_QUESTIONS],
    createdAt: '2026-09-11'
  }
];

export const MOCK_EXAM_PAPERS: Exam[] = MOCK_EXAMS;
