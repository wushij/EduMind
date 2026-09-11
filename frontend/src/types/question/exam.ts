import type { Question } from './question';

export interface ExamRule {
  type: string;
  label: string;
  count: number;
  scoreEach: number;
}

export interface ExamRuleConfig {
  courseId: number;
  courseName: string;
  title: string;
  semester: string;
  durationMinutes: number;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  rules: ExamRule[];
  knowledgePoints: string[];
}

export interface Exam {
  id: number;
  courseId: number;
  courseName: string;
  title: string;
  semester: string;
  totalScore: number;
  durationMinutes: number;
  passScore: number;
  rules: ExamRule[];
  questions: Question[];
  createdAt: string;
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
}

export type ExamPaper = Exam;
