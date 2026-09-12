export interface KnowledgeGraphNode {
  id: string;
  label: string;
  type: 'KNOWLEDGE_POINT' | 'CHAPTER' | 'CHUNK' | string;
  refId?: number;
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
