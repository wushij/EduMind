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
  focusKnowledgePointIds?: number[];
}

export interface TeachingAdviceVO {
  summary: string;
  actions: string[];
}
