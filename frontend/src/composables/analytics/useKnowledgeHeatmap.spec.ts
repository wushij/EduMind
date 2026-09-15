import { describe, it, expect } from 'vitest';
import { parseHeatmapVO } from './useKnowledgeHeatmap';
import type { KnowledgeHeatmapVO } from '@/types/analytics/mastery';

const sampleVo: KnowledgeHeatmapVO = {
  courseId: 102,
  knowledgePoints: [
    { id: 1, title: '导数' },
    { id: 2, title: '积分' }
  ],
  students: [
    { id: 10, name: '张三', studentNo: 'S001' },
    { id: 11, name: '李四', studentNo: 'S002' }
  ],
  cells: [
    { studentId: 10, knowledgePointId: 1, mastery: 0.85 },
    { studentId: 10, knowledgePointId: 2, mastery: 72 },
    { studentId: 11, knowledgePointId: 1, mastery: 0.55 }
  ]
};

describe('parseHeatmapVO', () => {
  it('maps knowledge points and student rows', () => {
    const parsed = parseHeatmapVO(sampleVo);

    expect(parsed.kpList).toHaveLength(2);
    expect(parsed.studentRows).toHaveLength(2);
    expect(parsed.studentRows[0].scores[1]).toBe(85);
    expect(parsed.studentRows[0].scores[2]).toBe(72);
    expect(parsed.studentRows[1].scores[1]).toBe(55);
    expect(parsed.studentRows[1].scores[2]).toBe(70);
  });

  it('computes class average scores per knowledge point', () => {
    const parsed = parseHeatmapVO(sampleVo);

    expect(parsed.classAvgScores[1]).toBe(70);
    expect(parsed.classAvgScores[2]).toBe(71);
  });
});

describe('useKnowledgeHeatmap', () => {
  it('exports composable factory', async () => {
    const { useKnowledgeHeatmap } = await import('./useKnowledgeHeatmap');
    expect(typeof useKnowledgeHeatmap).toBe('function');
  });
});
