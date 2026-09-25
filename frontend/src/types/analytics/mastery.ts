/** 统计作用域：全班口径 / 聚焦某位学员 */
export type MasteryScope = 'CLASS' | 'STUDENT';

/** 单个分值的来源：真实测评 / 规则推算 */
export type MasterySource = 'MEASURED' | 'ESTIMATED';

/** 考点级别数据可信度 */
export type DataConfidence = 'MEASURED' | 'MIXED' | 'ESTIMATED';

export interface WeakPointVO {
  knowledgePointId: number;
  title: string;
  chapterName?: string;
  mastery: number;
  suggestion: string;
  wrongCount?: number;
  affectedStudentCount?: number;
  /** 该考点上真正有实测成绩的学员数 */
  measuredStudentCount?: number;
  /** 参与统计的班级学员数（受影响人数的分母） */
  classStudentCount?: number;
  /** 数据可信度，ESTIMATED 表示结论尚未被真实测评验证 */
  dataConfidence?: DataConfidence;
}

export interface KnowledgeMasteryStudentItem {
  id: number;
  name: string;
  username: string;
  studentNo?: string;
  avatar?: string;
  className?: string;
  masteryAvg?: number;
  /** 该学员已有真实测评记录的考点数 */
  measuredKpCount?: number;
  /** 该学员依靠规则推算得出分值的考点数 */
  estimatedKpCount?: number;
}

export interface KnowledgeMasteryVO {
  dimensions: string[];
  personal: number[];
  classAvg: number[];
  weakPoints: WeakPointVO[];
  totalKnowledgePoints?: number;
  classAvgMastery?: number;
  masteredCount?: number;
  goodCount?: number;
  warningCount?: number;
  studentCount?: number;
  students?: KnowledgeMasteryStudentItem[];
  /** 当前统计作用域 */
  scope?: MasteryScope;
  /** 聚焦学员 ID，未聚焦为 null */
  focusStudentId?: number | null;
  /** 聚焦学员的掌握度均分 */
  focusAvgMastery?: number;
  /** 课程真实选课成员总数（未过滤测试账号） */
  classStudentCount?: number;
  /** 本次统计中来自真实测评的方格数 */
  measuredCellCount?: number;
  /** 本次统计的方格总数 */
  totalCellCount?: number;
}

export interface KnowledgeMasteryQuery {
  courseId: number;
  studentId?: number;
  includeTesting?: boolean;
}

export interface StudentWrongDetailVO {
  studentId: number;
  studentName: string;
  studentNo?: string;
  lastStudentAnswer?: string;
  wrongCount: number;
  updateTime?: string;
}

export interface WeakKpSummaryVO {
  knowledgePointId: number;
  knowledgePointName: string;
  wrongCount: number;
  errorRate: number;
}

export interface WrongQuestionItemVO {
  id?: number;
  questionId: number;
  stem?: string;
  type?: string;
  typeName?: string;
  difficulty?: number;
  options?: string;
  answer?: string;
  analysis?: string;
  knowledgePointId?: number;
  knowledgePointName?: string;
  wrongStudentCount?: number;
  classStudentCount?: number;
  errorRate?: number;
  wrongCount: number;
  errorTypes: string[];
  errorTypeLabels?: string[];
  diagnosis: string;
  diagnosisSource?: 'AI' | 'LEGACY';
  variantQuestionIds: number[];
  variantCount?: number;
  studentWrongList?: StudentWrongDetailVO[];
}

export interface WrongQuestionAnalyticsVO {
  list: WrongQuestionItemVO[];
  total: number;
  totalWrongQuestions?: number;
  totalWrongRecords?: number;
  avgErrorRate?: number;
  weakKnowledgePointCount?: number;
  totalVariantQuestions?: number;
  errorTypeDistribution?: Record<string, number>;
  topWeakKnowledgePoints?: WeakKpSummaryVO[];
}

export interface WrongQuestionQuery {
  courseId: number;
  knowledgePointId?: number;
  page?: number;
  pageSize?: number;
}

export interface TeachingAdviceRequest {
  courseId: number;
  studentId?: number;
  focusKnowledgePointIds?: number[];
}

export interface TeachingAdviceVO {
  summary: string;
  actions: string[];
}

export interface KnowledgeHeatmapPoint {
  id: number;
  title: string;
  chapterId?: number;
  chapterName?: string;
}

export interface KnowledgeHeatmapStudent {
  id: number;
  name: string;
  username?: string;
  studentNo?: string;
  avatar?: string;
  className?: string;
  avgScore?: number;
  /** 该学员已有真实测评记录的考点数 */
  measuredKpCount?: number;
  /** 该学员依靠规则推算得出分值的考点数 */
  estimatedKpCount?: number;
}

export interface KnowledgeHeatmapCell {
  studentId: number;
  knowledgePointId: number;
  mastery: number;
  /** 数据来源：MEASURED 实测 / ESTIMATED 规则推算 */
  source?: MasterySource;
  /** 实测样本数（推算结果为 0） */
  sampleCount?: number;
}

export interface KnowledgeHeatmapVO {
  courseId: number;
  knowledgePoints: KnowledgeHeatmapPoint[];
  students: KnowledgeHeatmapStudent[];
  cells: KnowledgeHeatmapCell[];
  /** 后端权威班级均分（考点 ID → 百分制分值），前端不再自行重算 */
  classAvgScores?: Record<string, number>;
  /** 参与统计的学员数（已按后端过滤规则处理测试账号） */
  studentCount?: number;
  /** 课程真实选课成员总数 */
  classStudentCount?: number;
  /** 实测方格数 */
  measuredCellCount?: number;
  /** 推算方格数 */
  estimatedCellCount?: number;
}


