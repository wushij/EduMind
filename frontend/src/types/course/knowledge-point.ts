export interface KnowledgePoint {
  id: number;
  chapterId: number;
  name: string;
  title?: string;
  description?: string;
  code?: string;
  cognitiveDimension?: string;
  importance?: number;
  masteryRate?: number;
}
