export interface AssignmentSettings {
  aiGradingEnabled?: boolean;
  allowLate?: boolean;
  instantFeedback?: boolean;
}

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
  submittedCount?: number;
  pendingGradingCount?: number;
  studentCount?: number;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | string;
  createTime?: string;
  settings?: AssignmentSettings;
}

export interface AssignmentStats {
  activeAssignmentCount?: number;
  pendingGradingCount?: number;
  aiGradedCount?: number;
  avgSubmissionRate?: number;
}

export interface StudentAssignment extends Assignment {
  mySubmissionStatus?: string;
  mySubmissionId?: number;
}

export interface AssignmentPaper {
  assignmentId: number;
  title: string;
  courseId: number;
  deadline?: string;
  totalScore?: number;
  passScore?: number;
  settings?: AssignmentSettings;
  questions?: Array<{
    questionId: number;
    score?: number;
    sortOrder?: number;
    question?: {
      id: number;
      stem?: string;
      type?: string;
      options?: string;
      score?: number;
    };
  }>;
  mySubmissionStatus?: string;
  mySubmissionId?: number;
}
