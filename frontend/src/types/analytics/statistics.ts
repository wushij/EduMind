export interface RecentCourse {
  id: number;
  name: string;
  lastVisitAt?: string;
}

export interface DashboardStatistics {
  courseCount: number;
  questionCount: number;
  examCount: number;
  aiConversationCount: number;
  assignmentCount?: number;
  pendingGradingCount?: number;
  pendingAssignmentCount?: number;
  recentCourses?: RecentCourse[];
}
