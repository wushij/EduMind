export interface GenerateQuestionRequest {
  courseId: number;
  chapterIds?: number[];
  knowledgePointIds?: number[];
  questionTypes?: string[];
  difficulty?: string;
  count?: number;
  scorePerQuestion?: number;
}
