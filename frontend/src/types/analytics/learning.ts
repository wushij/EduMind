export interface LearningTrendPoint {
  date: string;
  activeUsers: number;
}

export interface ScoreTrendPoint {
  date: string;
  /** 当日班级真实均分（按提交日聚合）；当日无已批改答卷时为 null，图表应断线而非平滑伪造 */
  avgScore?: number | null;
  /** 当日全校对照均分（全平台提交真实聚合）；无数据时为 null */
  schoolAvgScore?: number | null;
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

export interface ChapterProgressVO {
  chapterId: number;
  chapterTitle: string;
  sort: number;
  /** 章节学习覆盖率 0~100：本章（含其微课节）有学习行为的学生数占选课学生比例 */
  completionRate: number;
  /** 章节掌握度 0~100：本章关联知识点的平均掌握度；课程未挂知识点时为 null */
  avgScore: number | null;
  studentCount: number;
  avgStudyMinutes: number;
}

export interface CourseHealthVO {
  overallScore: number;
  syllabusCoverage: number;
  assignmentCompletion: number;
  studentInteraction: number;
  passRate: number;
  aiAssistanceRate: number;
  healthLevel: 'EXCELLENT' | 'GOOD' | 'WARNING';
}

export interface CourseWeakPointVO {
  knowledgePointId: number;
  title: string;
  mastery: number;
  wrongCount: number;
  affectedStudents: number;
  urgency: 'HIGH' | 'MEDIUM' | 'LOW';
}

/** 课程全量考点的真实掌握度；mastery 为 null 表示该考点尚无实测记录（未测评） */
export interface KnowledgePointMasteryVO {
  knowledgePointId: number;
  title: string;
  mastery: number | null;
  assessedStudentCount: number;
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
  /** 数据更新时间：统计范围内最近一次真实学习行为时间（yyyy-MM-dd HH:mm），无数据时为 null */
  dataUpdatedAt?: string | null;
  trends: LearningTrendData;
  students?: StudentLearningItemVO[];
  chapterProgressList?: ChapterProgressVO[];
  courseHealth?: CourseHealthVO;
  courseWeakPoints?: CourseWeakPointVO[];
  /** 全量考点真实掌握度（含未测评考点，mastery 为 null）；旧后端无此字段 */
  courseKnowledgePoints?: KnowledgePointMasteryVO[];
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
  totalStudyMinutesAllTime?: number;
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

export interface AiUsageSceneVO {
  /** 教育业务场景名（由 ai_call_log.scene 归并得出，如"智能答疑解惑"） */
  scene: string;
  /** 该场景真实调用次数 */
  calls: number;
  /** 占全部调用的百分比 0~100 */
  ratio: number;
  /** 归入该场景的原始场景码，便于核对口径 */
  sourceScenes?: string[];
}

export interface AiUsageAnalyticsVO {
  totalCalls: number;
  totalTokens: number;
  avgLatencyMs?: number;
  todayCalls?: number;
  todayTokens?: number;
  successRate?: number;
  totalSavedHours?: number;
  daily: AiUsageDailyVO[];
  byProvider: AiUsageByProviderVO[];
  /** 按真实调用场景聚合的分布（课程维度优先） */
  byScene?: AiUsageSceneVO[];
}

export interface AiCallLogItem {
  id: number;
  userId?: number;
  username?: string;
  realName?: string;
  avatar?: string;
  userRole?: string;
  courseId?: number;
  courseName?: string;
  conversationId?: string;
  model?: string;
  modelKey?: string;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  latencyMs?: number;
  scene?: string;
  sceneLabel?: string;
  knowledgeBaseId?: number;
  knowledgeBaseName?: string;
  retrievalHitCount?: number;
  citationDocIds?: string;
  createTime?: string;
}

export interface AiCallLogPageResult {
  list: AiCallLogItem[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface AiUsageLogQuery {
  courseId?: number;
  range?: string;
  scene?: string;
  model?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface LearningAnalyticsQuery {
  courseId: number;
  range?: string;
  classId?: number;
  startDate?: string;
  endDate?: string;
}

export interface StudentPortraitQuery {
  courseId: number;
  studentId?: number;
  range?: string;
}

export interface AiUsageAnalyticsQuery {
  courseId?: number;
  range?: string;
}
