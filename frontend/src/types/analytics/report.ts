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

export interface TeachingReportVO {
  courseId: number;
  range: string;
  totalChapters: number;
  recommendedQuestions: number;
  recommendedResources: number;
  aiCallCount: number;
  avgSubmissionRate: number;
  knowledgeMasteryAvg?: number;
  weeklyActivity?: TeachingReportWeeklyActivity[];
  errorCategories?: TeachingReportErrorCategory[];
  weakPoints: Array<{
    title: string;
    wrongCount: number;
    suggestion: string;
  }>;
}
