export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK' | 'SHORT_ANSWER';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type DifficultyLevel = Difficulty;

export interface QuestionOption {
  key: string;
  content: string;
  isCorrect?: boolean;
}

export interface Question {
  id: number;
  courseId: number;
  courseName?: string;
  chapterId?: number;
  chapterName?: string;
  type: QuestionType;
  typeLabel?: string;
  difficulty: Difficulty;
  difficultyLabel?: string;
  score: number;
  stem: string;
  options?: QuestionOption[];
  correctAnswer?: string;
  analysis: string;
  knowledgePointNames: string[];
  createdAt?: string;
}

export type QuestionItem = Question;
export type QuestionVO = Question;

export interface QuestionGenerateParams {
  subject: string;
  chapter?: string;
  knowledgePoints: string[];
  questionTypes: QuestionType[];
  difficulty: Difficulty;
  count: number;
  promptNotes?: string;
}
