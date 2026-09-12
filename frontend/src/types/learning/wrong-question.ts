export interface WrongQuestionRecordItem {
  id: number;
  questionId: number;
  stem?: string;
  type?: string;
  difficulty?: string;
  options?: string;
  answer?: string;
  analysis?: string;
  studentAnswer?: string;
  wrongCount: number;
  errorTypes?: string[];
  diagnosis?: string;
  variantQuestionIds?: number[];
  createTime?: string;
}

export interface WrongQuestionListVO {
  list: WrongQuestionRecordItem[];
  total: number;
}
