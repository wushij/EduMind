import { QuestionItem } from '../question/question';

export interface WrongQuestion {
  id: number;
  question: QuestionItem;
  studentAnswer: string;
  errorReason: string;
  aiAdvice: string;
  status: 'NOT_MASTERED' | 'LEARNING' | 'MASTERED';
}
