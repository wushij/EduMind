export interface LearningTrendPoint {
  date: string;
  activeUsers: number;
}

export interface ScoreTrendPoint {
  date: string;
  avgScore: number;
}

export interface LearningTrendData {
  learning: LearningTrendPoint[];
  score: ScoreTrendPoint[];
}

export interface StudentLearningItemVO {
  studentId: number;
  username: string;
  realName: string;
  studentNo: string;
  avatar?: string;
  studyMinutes: number;
  avgScore: number;
  submissionRate: number;
  masteryScore: number;
  aiUsageCount: number;
  wrongCount: number;
  status: 'EXCELLENT' | 'GOOD' | 'WARNING' | 'RISK';
}

export interface LearningAnalyticsVO {
  courseId: number;
  studentCount: number;
  completionRate: number;
  avgScore: number;
  avgStudyMinutes: number;
  knowledgeMasteryAvg: number;
  aiUsageCount: number;
  aggregated?: boolean;
  trends: LearningTrendData;
  students?: StudentLearningItemVO[];
}

export interface StudentPortraitStudentInfo {
  studentId: number;
  username: string;
  realName: string;
  avatar?: string;
  studentNo: string;
  className: string;
  role: string;
  lastActiveTime: string;
}

export interface StudentPortraitSummary {
  totalStudyMinutes: number;
  classAvgStudyMinutes: number;
  avgScore: number;
  classAvgScore: number;
  submissionRate: number;
  overallMastery: number;
  aiUsageCount: number;
  wrongQuestionCount: number;
  learningPace: 'FAST' | 'NORMAL' | 'STEADY' | 'LAGGING';
}

export interface StudentPortraitRadar {
  dimensions: string[];
  personalScores: number[];
  classAvgScores: number[];
}

export interface StudentPortraitKnowledgePoint {
  knowledgePointId: number;
  title: string;
  masteryScore: number;
  sampleCount: number;
  status: 'MASTERED' | 'LEARNING' | 'WEAK';
  lastAssessedAt: string;
  suggestion: string;
}

export interface StudentPortraitWeakPoint {
  knowledgePointId: number;
  title: string;
  mastery: number;
  suggestion: string;
}

export interface StudentPortraitMasteredPoint {
  knowledgePointId: number;
  title: string;
  mastery: number;
}

export interface StudentPortraitWrongQuestion {
  recordId: number;
  questionId: number;
  questionStem: string;
  knowledgePointId: number;
  knowledgePointTitle: string;
  errorTypes: string;
  diagnosis: string;
  wrongCount: number;
  createTime: string;
}

export interface StudentPortraitAdaptiveTask {
  title: string;
  type: string;
  status: string;
}

export interface StudentPortraitAdaptiveWeek {
  weekNo: number;
  theme: string;
  tasks: StudentPortraitAdaptiveTask[];
}

export interface StudentPortraitVO {
  studentInfo: StudentPortraitStudentInfo;
  summary: StudentPortraitSummary;
  radar: StudentPortraitRadar;
  knowledgePoints: StudentPortraitKnowledgePoint[];
  weakPoints: StudentPortraitWeakPoint[];
  masteredPoints: StudentPortraitMasteredPoint[];
  wrongQuestions: StudentPortraitWrongQuestion[];
  adaptiveWeeks: StudentPortraitAdaptiveWeek[];
  aiDiagnosis: string;
}

export interface AiUsageDailyVO {
  date: string;
  calls: number;
  tokens: number;
}

export interface AiUsageByProviderVO {
  provider: string;
  calls: number;
  tokens: number;
}

export interface AiUsageAnalyticsVO {
  totalCalls: number;
  totalTokens: number;
  daily: AiUsageDailyVO[];
  byProvider: AiUsageByProviderVO[];
}

export interface LearningAnalyticsQuery {
  courseId: number;
  range?: string;
  classId?: number;
}

export interface StudentPortraitQuery {
  courseId: number;
  studentId?: number;
}

export interface AiUsageAnalyticsQuery {
  courseId?: number;
  range?: string;
}
