export interface TeachingReport {
  avgScore: number;
  masteryRate: number;
  aiAdvice: string;
}

export interface TeachingReportWeeklyActivity {
  date: string;
  count: number;
}

export interface TeachingReportErrorCategory {
  type: string;
  name: string;
  percent: number;
}

export interface TeachingReportWeakPoint {
  questionId?: number | string;
  questionStem?: string;
  knowledgePointId?: number | string;
  knowledgePointName?: string;
  chapterName?: string;
  title: string;
  wrongCount: number;
  masteryRate?: number;
  errorType?: string;
  errorTypeName?: string;
  errorReason?: string;
  suggestion: string;
  status?: 'good' | 'normal' | 'warning' | 'danger';
  statusLabel?: string;
}

export interface TeachingReportVO {
  courseId: number;
  courseName?: string;
  courseCode?: string;
  teacherName?: string;
  studentCount?: number;
  syllabusProgress?: number;
  range: string;
  totalChapters: number;
  recommendedQuestions: number;
  recommendedResources: number;
  aiCallCount: number;
  avgSubmissionRate: number;
  knowledgeMasteryAvg?: number;
  weeklyActivity?: TeachingReportWeeklyActivity[];
  errorCategories?: TeachingReportErrorCategory[];
  weakPoints: TeachingReportWeakPoint[];
}

