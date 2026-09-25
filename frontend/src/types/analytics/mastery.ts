export interface WeakPointVO {
  knowledgePointId: number;
  title: string;
  chapterName?: string;
  mastery: number;
  suggestion: string;
  wrongCount?: number;
  affectedStudentCount?: number;
}

export interface KnowledgeMasteryStudentItem {
  id: number;
  name: string;
  username: string;
  studentNo?: string;
  avatar?: string;
  className?: string;
  masteryAvg?: number;
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
}

export interface KnowledgeMasteryQuery {
  courseId: number;
  studentId?: number;
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
  studentNo?: string;
  avatar?: string;
  className?: string;
  avgScore?: number;
}

export interface KnowledgeHeatmapCell {
  studentId: number;
  knowledgePointId: number;
  mastery: number;
}

export interface KnowledgeHeatmapVO {
  courseId: number;
  knowledgePoints: KnowledgeHeatmapPoint[];
  students: KnowledgeHeatmapStudent[];
  cells: KnowledgeHeatmapCell[];
}


