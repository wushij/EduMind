export interface WeakPointVO {
  knowledgePointId: number;
  title: string;
  mastery: number;
  suggestion: string;
}

export interface KnowledgeMasteryVO {
  dimensions: string[];
  personal: number[];
  classAvg: number[];
  weakPoints: WeakPointVO[];
}

export interface KnowledgeMasteryQuery {
  courseId: number;
  studentId?: number;
}

export interface WrongQuestionItemVO {
  id?: number;
  questionId: number;
  wrongCount: number;
  errorTypes: string[];
  diagnosis: string;
  variantQuestionIds: number[];
}

export interface WrongQuestionAnalyticsVO {
  list: WrongQuestionItemVO[];
  total: number;
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
}

export interface KnowledgeHeatmapStudent {
  id: number;
  name: string;
  studentNo?: string;
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

