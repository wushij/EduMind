export interface WrongQuestionRecordItem {
  id: number;
  questionId: number;
  knowledgePointId?: number;
  knowledgePointName?: string;
  stem?: string;
  type?: string;
  difficulty?: string;
  options?: string;
  answer?: string;
  analysis?: string;
  studentAnswer?: string;
  wrongCount: number;
  status?: number;
  errorTypes?: string[];
  errorTypeLabels?: string[];
  diagnosis?: string;
  variantQuestionIds?: number[];
  createTime?: string;
}

export interface WrongQuestionListVO {
  list: WrongQuestionRecordItem[];
  total: number;
}

export type WrongBookListVO = WrongQuestionListVO;

export interface WrongBookOverviewVO {
  pendingCount: number;
  weakKnowledgePointCount: number;
  masteredCount: number;
  variantConquerRatePercent: number;
}

export interface WrongBookKnowledgeGraphNode {
  knowledgePointId: number;
  name: string;
  masteryPercent: number;
  current: boolean;
}

export interface WrongBookVariantSummary {
  questionId: number;
  stemPreview: string;
}

export interface WrongBookDetailVO extends WrongQuestionRecordItem {
  prerequisiteNodes?: WrongBookKnowledgeGraphNode[];
  variantQuestions?: WrongBookVariantSummary[];
}

/** 错因筛选与后端 WrongErrorType code 一致 */
export const WRONG_ERROR_TYPE_OPTIONS = [
  { code: '', label: '全部错误类型' },
  { code: 'CONCEPT', label: '概念模糊' },
  { code: 'CALC', label: '计算失误' },
  { code: 'LOGIC', label: '逻辑漏洞' },
  { code: 'READING', label: '审题不清' }
] as const;
