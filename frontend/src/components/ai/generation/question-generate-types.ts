import type { QuestionType, Difficulty } from '@/mock/questions';

export interface QuestionGenerateFormState {
  courseId: number;
  chapterIds: number[];
  knowledgePointIds: number[];
  knowledgePointNames: string[];
  questionTypes: QuestionType[];
  difficulty: Difficulty;
  count: number;
  scorePerQuestion: number;
}

export interface QuestionTypeOption {
  type: QuestionType;
  label: string;
  icon: any;
  color: string;
}

export interface DifficultyOption {
  val: Difficulty;
  label: string;
  desc: string;
  colorClass: string;
}
