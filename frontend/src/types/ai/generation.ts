import { QuestionType, DifficultyLevel } from '@/constants/question';

export interface GenerateQuestionParams {
  courseId: number;
  chapterIds: number[];
  types: QuestionType[];
  difficulty: DifficultyLevel;
  count: number;
  ragEnabled: boolean;
}
