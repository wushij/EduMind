export interface KnowledgePointBrief {
  id: number;
  title: string;
}

export interface KnowledgePoint {
  id: number;
  courseId?: number;
  chapterId?: number;
  name?: string;
  title?: string;
  description?: string;
  code?: string;
  cognitiveDimension?: string;
  importance?: number;
  examFocus?: string;
  sort?: number;
  masteryRate?: number;
  prerequisiteIds?: number[];
  prerequisites?: KnowledgePointBrief[];
}

export interface KnowledgePointSaveRequest {
  chapterId?: number;
  title: string;
  code?: string;
  description?: string;
  cognitiveDimension?: string;
  importance?: number;
  examFocus?: string;
  sortOrder?: number;
  prerequisiteIds?: number[];
}

export interface CourseKnowledgePointSuggestItem {
  title: string;
  cognitiveDimension?: string;
  importance?: number;
  description?: string;
  examFocus?: string;
  prerequisiteTitles?: string[];
}

export interface CourseKnowledgePointSuggestResult {
  points: CourseKnowledgePointSuggestItem[];
  aiGenerated?: boolean;
  sourceLabel?: string;
}
