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
  /**
   * 归因结论来源（后端计算）：
   * NONE=暂无结论 / UNANSWERED=未作答不作归因 / LEGACY=演示或历史预置数据（非大模型产出） / AI=大模型实时生成。
   */
  diagnosisSource?: 'NONE' | 'UNANSWERED' | 'LEGACY' | 'AI';
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
  /** 截断预览（公式可能被截断，展示请优先用 stem） */
  stemPreview: string;
  /** 完整题干（后端已补全裸 LaTeX 的 $ 定界符），由 CSS 按行裁切展示，保证公式完整渲染 */
  stem?: string;
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
