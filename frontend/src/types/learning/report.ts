import type { StudentPortraitVO } from '@/types/analytics/learning';

export interface LearningReportEnrolledCourseVO {
  courseId: number;
  courseName: string;
}

export interface LearningReportDailyStudyVO {
  date: string;
  minutes: number;
}

export interface LearningReportDailyScoreVO {
  date: string;
  avgScore: number;
}

export interface LearningReportTrendsVO {
  studyMinutesByDate: LearningReportDailyStudyVO[];
  scoreByDate: LearningReportDailyScoreVO[];
}

export interface LearningReportVO {
  courseId: number | null;
  courseName: string | null;
  portrait: StudentPortraitVO | null;
  trends: LearningReportTrendsVO;
  enrolledCourses: LearningReportEnrolledCourseVO[];
}

export interface LearningReportQuery {
  courseId?: number;
  range?: string;
}
