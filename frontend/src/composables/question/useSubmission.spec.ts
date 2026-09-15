import { describe, it, expect } from 'vitest';
import type { GradingItem } from './useSubmission';
import {
  buildGradingItems,
  calculateGradingTotalScore,
  getSubmissionTypeLabel,
  getSubmissionTypeTagType
} from './useSubmission';

describe('buildGradingItems', () => {
  it('returns empty array when grading list is empty', () => {
    expect(buildGradingItems({ answers: [] }, [])).toEqual([]);
    expect(buildGradingItems(null, undefined as unknown as any[])).toEqual([]);
  });

  it('maps grading results with student answers', () => {
    const sub = {
      answers: [
        { questionId: 10, answer: 'A' },
        { questionId: 11, answer: '递归实现' }
      ]
    };
    const grading = [
      {
        questionId: 10,
        isCorrect: true,
        maxScore: 5,
        score: 5,
        stem: '选择题',
        standardAnswer: 'A',
        analysis: '基础题',
        aiComment: '客观题正确'
      },
      {
        questionId: 11,
        maxScore: 10,
        score: 8,
        stem: '简答题',
        standardAnswer: '标准答案',
        analysis: '考查递归',
        aiComment: '步骤完整'
      }
    ];

    const items = buildGradingItems(sub, grading);
    expect(items).toHaveLength(2);
    expect(items[0]).toMatchObject({
      questionId: 10,
      type: 'SINGLE_CHOICE',
      studentAnswer: 'A',
      isObjective: true,
      isCorrect: true,
      teacherScore: 5
    });
    expect(items[1]).toMatchObject({
      questionId: 11,
      type: 'SHORT_ANSWER',
      studentAnswer: '递归实现',
      isObjective: false,
      teacherScore: 8
    });
  });

  it('falls back to generated stem and default answers', () => {
    const items = buildGradingItems({}, [{ maxScore: 6, score: 4 }]);
    expect(items[0].stem).toBe('答卷试题 #1 评分考查点');
    expect(items[0].studentAnswer).toBe('（考生作答内容）');
    expect(items[0].teacherScore).toBe(4);
  });
});

describe('calculateGradingTotalScore', () => {
  it('sums teacher scores', () => {
    const items: GradingItem[] = [
      {
        questionId: 1,
        type: 'SINGLE_CHOICE',
        stem: 'q1',
        maxScore: 5,
        studentAnswer: 'A',
        standardAnswer: 'A',
        analysis: '',
        isObjective: true,
        isCorrect: true,
        aiScore: 5,
        aiComment: '',
        teacherScore: 5,
        teacherComment: ''
      },
      {
        questionId: 2,
        type: 'SHORT_ANSWER',
        stem: 'q2',
        maxScore: 10,
        studentAnswer: 'ans',
        standardAnswer: 'std',
        analysis: '',
        isObjective: false,
        isCorrect: false,
        aiScore: 6,
        aiComment: '',
        teacherScore: 7,
        teacherComment: ''
      }
    ];
    expect(calculateGradingTotalScore(items)).toBe(12);
  });
});

describe('submission label helpers', () => {
  it('maps question type labels and tag types', () => {
    expect(getSubmissionTypeLabel('SHORT_ANSWER')).toBe('综合简答题');
    expect(getSubmissionTypeTagType('TRUE_FALSE')).toBe('warning');
    expect(getSubmissionTypeLabel('CUSTOM')).toBe('CUSTOM');
  });
});
