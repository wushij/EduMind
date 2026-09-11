export interface Assignment {
  id: number;
  title: string;
  courseId: number;
  courseName?: string;
  examId?: number;
  description?: string;
  totalScore?: number;
  passScore?: number;
  deadline: string;
  submissionCount?: number;
  status: 'PENDING' | 'GRADED' | string;
  createTime?: string;
}
