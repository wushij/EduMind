import { describe, it, expect, vi } from 'vitest';
import { stripDiagnosisTypeMarker, useWrongQuestions } from './useWrongQuestions';
import type { WrongQuestionRecordItem } from '@/types/learning/wrong-question';

vi.mock('@/api/learning/wrong-book', () => ({
  getWrongBook: vi.fn(),
  getWrongBookOverview: vi.fn(),
  diagnoseWrongBookItem: vi.fn(),
  generateWrongBookVariants: vi.fn(),
  masterWrongBookItem: vi.fn()
}));

import { diagnoseWrongBookItem } from '@/api/learning/wrong-book';

describe('stripDiagnosisTypeMarker', () => {
  it('剥离句末裸类型标记，且保留结论原有的句读', () => {
    const raw =
      '根本原因：概念理解错误，未能准确区分 ArrayList 基于 Object[] 数组、支持下标随机访问，'
      + '而 LinkedList 基于双向链表、查找需遍历且存在前后节点引用开销等核心特性。 CONCEPT';
    const cleaned = stripDiagnosisTypeMarker(raw);
    expect(cleaned).not.toContain('CONCEPT');
    expect(cleaned.endsWith('等核心特性。')).toBe(true);
  });

  it('剥离演示数据的句首 code: 前缀', () => {
    expect(stripDiagnosisTypeMarker('CALC: 等价无穷小代换条件应用错误'))
      .toBe('等价无穷小代换条件应用错误');
  });

  it('剥离「类型：CODE」与括号包裹形式', () => {
    expect(stripDiagnosisTypeMarker('审题偏差（READING）导致漏看条件'))
      .not.toContain('READING');
    expect(stripDiagnosisTypeMarker('……逻辑推理跳步。类型：LOGIC'))
      .not.toContain('LOGIC');
  });
});

describe('useWrongQuestions.loadDiagnosis', () => {
  it('重新诊断后同步刷新归因来源，不再残留「演示数据」标记', async () => {
    vi.mocked(diagnoseWrongBookItem).mockResolvedValue({
      data: {
        diagnosis: '根本原因：概念理解错误，未能区分 ArrayList 与 LinkedList 的随机访问特性。',
        diagnosisSource: 'AI',
        errorTypes: ['CONCEPT'],
        errorTypeLabels: ['概念模糊'],
        variantQuestionIds: [9001]
      }
    } as never);

    const { loadDiagnosis } = useWrongQuestions();
    const item = {
      id: 1,
      questionId: 1007,
      diagnosis: 'CONCEPT: 混淆 ArrayList 与 LinkedList 的随机访问时间复杂度',
      diagnosisSource: 'LEGACY'
    } as unknown as WrongQuestionRecordItem;

    await loadDiagnosis(item, true);

    // 正文与来源必须一起更新：只更新正文会让面板同时出现「AI 正文 + 演示数据标签」
    expect(item.diagnosisSource).toBe('AI');
    expect(item.diagnosis).toContain('根本原因');
    expect(item.errorTypeLabels).toEqual(['概念模糊']);
    expect(item.variantQuestionIds).toEqual([9001]);
  });
});
