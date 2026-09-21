/** 与后端 QuestionVO 对齐 */
export interface PracticeQuestionVO {
  id: number;
  courseId?: number;
  knowledgePointId?: number;
  stem: string;
  type: string;
  options?: string;
  answer?: string;
  analysis?: string;
  difficulty?: number;
  score?: number;
  knowledgePointName?: string;
  cognitiveLevel?: string;
}

export interface AiPracticeStartRequest {
  courseId: number;
  count?: number;
  mode?: string;
  knowledgePointId?: number;
  cognitiveLevel?: string;
  instantFeedback?: boolean;
  /**
   * 指定练习题目 ID（错题变式 / 单题自测）。
   * 雪花 ID 超出 JS 安全整数范围，必须用字符串传递，否则精度丢失会导致后端查不到题目。
   */
  seedQuestionIds?: Array<number | string>;
}

export interface AiPracticeSessionVO {
  sessionId: string;
  courseId: number;
  questionCount: number;
  questions: PracticeQuestionVO[];
  weakPointHint?: string;
  estimatedMinutes?: number;
  weakKnowledgePointCount?: number;
  pendingWrongQuestionCount?: number;
}

export interface AiPracticeGradeRequest {
  sessionId: string;
  questionId: number;
  studentAnswer?: string;
}

export interface AiPracticeGradeVO {
  questionId: number;
  correct?: boolean;
  score?: number;
  maxScore?: number;
  referenceAnswer?: string;
  analysis?: string;
  knowledgePointName?: string;
}

export interface AiPracticeSubmitRequest {
  courseId: number;
  sessionId: string;
  durationSeconds?: number;
  answers: Array<{
    questionId: number;
    knowledgePointId?: number;
    answer?: string;
  }>;
}

export interface AiPracticeQuestionResult {
  questionId: number;
  correct?: boolean;
  studentAnswer?: string;
  referenceAnswer?: string;
  analysis?: string;
  knowledgePointName?: string;
}

export interface AiPracticeSubmitVO {
  sessionId: string;
  totalCount: number;
  correctCount: number;
  accuracyRate: number;
  durationSeconds: number;
  wrongQuestionIds?: number[];
  weakKnowledgePointIds?: number[];
  aiSummary?: string;
  questionResults?: AiPracticeQuestionResult[];
}
