export interface LearningHomeSummaryVO {
  avgCourseProgressPercent: number;
  totalStudyMinutes: number;
  completedTasks: number;
  totalTasks: number;
  overallMasteryPercent: number;
}

export interface LearningHomeCourseItemVO {
  courseId: number;
  courseName: string;
  lessonProgressPercent: number;
  overallMastery: number;
  pendingAssignmentCount: number;
}

export interface LearningHomeWeakPointVO {
  knowledgePointId: number;
  title: string;
  courseId: number;
  courseName: string;
  mastery: number;
  suggestion: string;
  level: 'danger' | 'warning';
}

export interface LearningHomeTodayTaskVO {
  id: string;
  title: string;
  courseId: number;
  courseName: string;
  type: string;
  estimatedMinutes: number;
  status: 'PENDING' | 'COMPLETED';
  targetUrl?: string;
}

export interface LearningHomeOverviewVO {
  primaryCourseId: number | null;
  summary: LearningHomeSummaryVO;
  courses: LearningHomeCourseItemVO[];
  weakPoints: LearningHomeWeakPointVO[];
  todayTasks: LearningHomeTodayTaskVO[];
}

export interface LearningHomeWeakPointUI {
  id: string;
  name: string;
  course: string;
  courseId: number;
  knowledgePointId: number;
  mastery: number;
  level: string;
  reason: string;
}

export interface LearningHomeTaskUI {
  id: string;
  title: string;
  course: string;
  courseId: number;
  type: string;
  estimatedMinutes: number;
  completed: boolean;
  targetUrl?: string;
}
