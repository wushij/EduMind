export interface KnowledgeGraphNode {
  id: string;
  label: string;
  type: 'KNOWLEDGE_POINT' | 'CHAPTER' | 'CHUNK' | string;
  refId?: number;
  /** 掌握度百分比（学习路径图谱会下发；纯知识图谱场景为空） */
  masteryPercent?: number;
  /** 掌握状态：WEAK / LEARNING / MASTERED（决定节点配色，与图例一一对应） */
  status?: string;
  /** 是否为当前学习路径的焦点考点（焦点节点放大高亮） */
  highlight?: boolean;
}

export interface KnowledgeGraphEdge {
  source: string;
  target: string;
  relation: string;
}

export interface KnowledgeGraphVO {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
}

export interface GraphGapVO {
  knowledgePointId: number;
  title: string;
  missingPrerequisites: Array<{ id: number; title: string }>;
}

export interface GraphRelationSuggestion {
  sourceKnowledgePointId: number;
  targetKnowledgePointId: number;
  sourceTitle: string;
  targetTitle: string;
  relationType: string;
  confidence: number;
  reason: string;
}
