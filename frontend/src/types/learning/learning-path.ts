export type LearningPathTaskType =
  | 'READ'
  | 'PRACTICE'
  | 'AI_CHAT'
  | 'WRONG_BOOK'
  | 'ASSIGNMENT'
  | 'RESOURCE';

export type LearningPathTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export interface LearningPathTask {
  id?: string;
  title: string;
  type: string;
  typeLabel?: string;
  refId?: number;
  knowledgePointId?: number;
  status: string;
  estimatedMinutes?: number;
  targetUrl?: string;
  actionLabel?: string;
}

export interface LearningPathWeek {
  weekNo: number;
  theme: string;
  knowledgePointId?: number;
  masteryPercent?: number;
  focusReason?: string;
  tasks: LearningPathTask[];
}

export interface LearningPathVO {
  courseId: number;
  title: string;
  weeks: LearningPathWeek[];
}

export interface LearningPathWeakPointBrief {
  knowledgePointId: number;
  title: string;
  masteryPercent: number;
  suggestion?: string;
}

export interface LearningPathGraphNode {
  id: string;
  label: string;
  type: string;
  refId?: number;
  masteryPercent?: number;
  status?: string;
}

export interface LearningPathGraphEdge {
  id?: string;
  source: string;
  target: string;
  relation: string;
}

export interface LearningPathGraphSlice {
  nodes: LearningPathGraphNode[];
  edges: LearningPathGraphEdge[];
  highlightNodeIds: string[];
  pathEdgeIds: string[];
}

export interface LearningPathDetailVO {
  courseId: number;
  courseName: string;
  studentId: number;
  title: string;
  overallProgressPercent: number;
  weakPointCount: number;
  graphGapCount: number;
  estimatedTotalMinutes: number;
  generatedAt: string;
  interpretHint?: string;
  weeks: LearningPathWeek[];
  graphSlice: LearningPathGraphSlice;
  weakPointsBrief: LearningPathWeakPointBrief[];
}

export interface LearningPathStudentItem {
  studentId: number;
  username?: string;
  realName?: string;
}

export interface LearningStep {
  stage: string;
  title: string;
  status: 'DONE' | 'DOING' | 'TODO';
}
