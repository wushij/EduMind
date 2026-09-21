import type { ExamRule } from '@/types/question/exam';

export interface ExamFormState {
  courseId: number;
  title: string;
  totalScore: number;
  durationMinutes: number;
  chapterIds: number[];
  knowledgePointIds?: number[];
  promptDirective?: string;
  aiGenerateFillShortfall?: boolean;
  rules: ExamRule[];
}
