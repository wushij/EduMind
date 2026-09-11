export interface SubmissionAnswerItem {
  questionId: number;
  answer: string;
}

export interface GradingItem {
  questionId: number;
  score?: number;
  maxScore?: number;
  isCorrect?: boolean;
  aiComment?: string;
  teacherComment?: string;
  status?: string;
}

export interface SubmissionItem {
  id: number;
  assignmentId: number;
  assignmentTitle?: string;
  studentId: number;
  studentName?: string;
  studentNo?: string;
  courseId?: number;
  courseName?: string;
  status: 'PENDING' | 'AI_GRADED' | 'GRADED' | string;
  totalScore?: number;
  maxScore?: number;
  submitTime?: string;
  isLate?: boolean;
  aiScore?: number;
  finalScore?: number | null;
  answers?: SubmissionAnswerItem[];
  gradingItems?: GradingItem[];
}

export interface GradingReviewPayload {
  items: Array<{
    questionId: number;
    score: number;
    teacherComment?: string;
  }>;
}
