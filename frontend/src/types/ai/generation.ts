export interface GenerateQuestionRequest {
  courseId: number;
  chapterIds?: number[];
  knowledgePointIds?: number[];
  knowledgePointNames?: string[];
  questionTypes?: string[];
  difficulty?: string;
  count?: number;
  scorePerQuestion?: number;
  promptDirective?: string;
  questionScene?: string;
  documentIds?: number[];
}
