export interface TeachingReport {
  avgScore: number;
  masteryRate: number;
  aiAdvice: string;
}

export interface TeachingReportVO {
  courseId: number;
  range: string;
  totalChapters: number;
  recommendedQuestions: number;
  recommendedResources: number;
  aiCallCount: number;
  avgSubmissionRate: number;
  weakPoints: Array<{
    title: string;
    wrongCount: number;
    suggestion: string;
  }>;
}
