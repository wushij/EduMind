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
    { id: 10, name: '张三', studentNo: 'S001', measuredKpCount: 2, estimatedKpCount: 0 },
    { id: 11, name: '李四', studentNo: 'S002', measuredKpCount: 0, estimatedKpCount: 1 }
  ],
  cells: [
    { studentId: 10, knowledgePointId: 1, mastery: 0.85, source: 'MEASURED', sampleCount: 4 },
    { studentId: 10, knowledgePointId: 2, mastery: 72, source: 'ESTIMATED', sampleCount: 0 },
    { studentId: 11, knowledgePointId: 1, mastery: 0.55, source: 'MEASURED', sampleCount: 2 }
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
    // 缺失方格不再伪装成 70 分，避免「无数据」被误读为「掌握良好」
    expect(parsed.studentRows[1].scores[2]).toBe(0);
  });

  it('keeps per-cell data source so 实测/推算 can be told apart', () => {
    const parsed = parseHeatmapVO(sampleVo);

    expect(parsed.studentRows[0].sources[1]).toBe('MEASURED');
    expect(parsed.studentRows[0].sources[2]).toBe('ESTIMATED');
    expect(parsed.studentRows[0].sampleCounts[1]).toBe(4);
    expect(parsed.measuredCellCount).toBe(2);
    expect(parsed.estimatedCellCount).toBe(1);
  });

  it('falls back to local average only when backend omits classAvgScores', () => {
    const parsed = parseHeatmapVO(sampleVo);

    expect(parsed.classAvgScores[1]).toBe(70);
    expect(parsed.classAvgScores[2]).toBe(36);
  });

  it('prefers backend classAvgScores over local recomputation', () => {
    const parsed = parseHeatmapVO({
      ...sampleVo,
      classAvgScores: { '1': 61.5, '2': 48 }
    });

    expect(parsed.classAvgScores[1]).toBe(61.5);
    expect(parsed.classAvgScores[2]).toBe(48);
  });
});

describe('useKnowledgeHeatmap', () => {
  it('exports composable factory', async () => {
    const { useKnowledgeHeatmap } = await import('./useKnowledgeHeatmap');
    expect(typeof useKnowledgeHeatmap).toBe('function');
  });
});
