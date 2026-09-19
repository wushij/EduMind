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
  promptDirective: string;
  questionScene: string;
  customInstruction: string;
}

export interface KnowledgePointItem {
  id: number;
  title: string;
  chapterId?: number;
  cognitiveDimension?: string;
  importance?: number;
  examFocus?: string;
}

export interface QuestionTypeOption {
  type: QuestionType;
  label: string;
  icon: any;
  color: string;
  desc?: string;
}

export interface DifficultyOption {
  val: Difficulty;
  label: string;
  desc: string;
  cognitiveGoal?: string;
  colorClass: string;
}
