export interface SubmissionAnswerItem {
  questionId: number;
  answer: string;
}

export interface GradingItem {
  questionId: number;
  type?: string;
  stem?: string;
  standardAnswer?: string;
  analysis?: string;
  score?: number;
  maxScore?: number;
  isCorrect?: boolean;
  aiComment?: string;
  teacherComment?: string;
  status?: string;
}

export interface SubmissionOverviewStats {
  total?: number;
  submittedCount?: number;
  gradedCount?: number;
  reviewedCount?: number;
}

export interface SubmissionItem {
  id: number;
  assignmentId: number;
  assignmentTitle?: string;
  studentId: number;
  studentName?: string;
  /** 学生头像 URL；为空时前端回退显示姓名首字 */
  studentAvatar?: string;
  studentNo?: string;
  courseId?: number;
  courseName?: string;
  status: 'SUBMITTED' | 'GRADED' | 'REVIEWED' | 'IN_PROGRESS' | string;
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
