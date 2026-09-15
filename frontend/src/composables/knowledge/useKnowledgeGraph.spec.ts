import { describe, it, expect } from 'vitest';
import {
  buildRelationTypesParam,
  resolveKnowledgePointId,
  fetchKnowledgePointRelations,
  saveKnowledgePointRelation,
  removeKnowledgePointRelation,
  fetchRelationSuggestions
} from './useKnowledgeGraph';
import type { KnowledgeGraphNode } from '@/types/knowledge/graph';

describe('buildRelationTypesParam', () => {
  it('joins relation types with comma', () => {
    expect(buildRelationTypesParam(['prerequisite', 'related'])).toBe('prerequisite,related');
  });

  it('returns empty string for empty list', () => {
    expect(buildRelationTypesParam([])).toBe('');
  });
});

describe('resolveKnowledgePointId', () => {
  it('returns refId for knowledge point nodes', () => {
    const node: KnowledgeGraphNode = {
      id: 'kp-1',
      label: '导数',
      type: 'KNOWLEDGE_POINT',
      refId: 42
    };
    expect(resolveKnowledgePointId(node)).toBe(42);
  });

  it('returns undefined for non knowledge point nodes', () => {
    const node: KnowledgeGraphNode = {
      id: 'doc-1',
      label: '文档',
      type: 'DOCUMENT'
    };
    expect(resolveKnowledgePointId(node)).toBeUndefined();
  });
});

describe('knowledge graph relation wrappers', () => {
  it('exports relation api wrappers', () => {
    expect(typeof fetchKnowledgePointRelations).toBe('function');
    expect(typeof saveKnowledgePointRelation).toBe('function');
    expect(typeof removeKnowledgePointRelation).toBe('function');
    expect(typeof fetchRelationSuggestions).toBe('function');
  });
});
