import { describe, it, expect } from 'vitest';
import type { QuestionItem } from '@/types/question/question';
import {
  getAssignmentCreateTypeLabel,
  getAssignmentCreateTypeTagType,
  calculateSelectedQuestionsScore,
  getSubmissionStatusLabel,
  getSubmissionStatusType,
  getAssignmentDetailTypeLabel,
  getAssignmentDetailTypeTagType,
  filterSubmissions,
  resolveTotalStudentsCount,
  calculateSubmissionRate,
  countPendingReview,
  calculateAverageScore
} from './useAssignment';

const sampleQuestions: QuestionItem[] = [
  {
    id: 1,
    courseId: 101,
    type: 'SINGLE_CHOICE',
    difficulty: 'EASY',
    score: 5,
    stem: 'q1',
    analysis: '',
    knowledgePointNames: ['A']
  },
  {
    id: 2,
    courseId: 101,
    type: 'SHORT_ANSWER',
    difficulty: 'HARD',
    score: 10,
    stem: 'q2',
    analysis: '',
    knowledgePointNames: ['B']
  }
];

const sampleSubmissions = [
  {
    id: 1,
    studentNo: '2024001',
    studentName: '张三',
    status: 'GRADED',
    finalScore: 90,
    aiGraded: true,
    aiScore: 88
  },
  {
    id: 2,
    studentNo: '2024002',
    studentName: '李四',
    status: 'SUBMITTED',
    finalScore: null,
    aiGraded: false
  },
  {
    id: 3,
    studentNo: '2024003',
    studentName: '王五',
    status: 'AI_GRADED',
    finalScore: 75,
    aiGraded: true,
    aiScore: 75
  }
];

describe('getAssignmentCreateTypeLabel', () => {
  it('maps create view type labels', () => {
    expect(getAssignmentCreateTypeLabel('SINGLE_CHOICE')).toBe('单选');
    expect(getAssignmentCreateTypeLabel('SHORT_ANSWER')).toBe('简答');
    expect(getAssignmentCreateTypeLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(getAssignmentCreateTypeLabel('')).toBe('单选');
  });
});

describe('getAssignmentCreateTypeTagType', () => {
  it('maps create view tag types', () => {
    expect(getAssignmentCreateTypeTagType('MULTIPLE_CHOICE')).toBe('success');
    expect(getAssignmentCreateTypeTagType('TRUE_FALSE')).toBe('warning');
    expect(getAssignmentCreateTypeTagType('UNKNOWN')).toBe('');
  });
});

describe('calculateSelectedQuestionsScore', () => {
  it('sums question scores with default fallback', () => {
    expect(calculateSelectedQuestionsScore(sampleQuestions)).toBe(15);
    expect(calculateSelectedQuestionsScore([{ ...sampleQuestions[0], score: undefined as unknown as number }])).toBe(5);
  });
});

describe('getSubmissionStatusLabel', () => {
  it('maps submission status labels', () => {
    expect(getSubmissionStatusLabel('REVIEWED')).toBe('批改完成');
    expect(getSubmissionStatusLabel('GRADED')).toBe('AI 已评 · 待确认');
    expect(getSubmissionStatusLabel('AI_GRADED')).toBe('AI 已评 · 待确认');
    expect(getSubmissionStatusLabel('SUBMITTED')).toBe('待批改');
    expect(getSubmissionStatusLabel('OTHER')).toBe('待批改');
  });
});

describe('getSubmissionStatusType', () => {
  it('maps submission status tag types', () => {
    expect(getSubmissionStatusType('GRADED')).toBe('success');
    expect(getSubmissionStatusType('AI_GRADED')).toBe('primary');
    expect(getSubmissionStatusType('PENDING')).toBe('warning');
    expect(getSubmissionStatusType('OTHER')).toBe('info');
  });
});

describe('getAssignmentDetailTypeLabel', () => {
  it('maps detail view type labels', () => {
    expect(getAssignmentDetailTypeLabel('SINGLE_CHOICE')).toBe('单选题');
    expect(getAssignmentDetailTypeLabel('FILL_BLANK')).toBe('填空题');
    expect(getAssignmentDetailTypeLabel('')).toBe('试题');
  });
});

describe('getAssignmentDetailTypeTagType', () => {
  it('maps detail view tag types', () => {
    expect(getAssignmentDetailTypeTagType('SHORT_ANSWER')).toBe('danger');
    expect(getAssignmentDetailTypeTagType('FILL_BLANK')).toBe('info');
  });
});

describe('filterSubmissions', () => {
  it('returns all when no filters applied', () => {
    expect(filterSubmissions(sampleSubmissions, '', '')).toHaveLength(3);
  });

  it('filters by status', () => {
    const result = filterSubmissions(sampleSubmissions, 'GRADED', '');
    expect(result).toHaveLength(1);
    expect(result[0].studentName).toBe('张三');
  });

  it('filters by student name or number', () => {
    expect(filterSubmissions(sampleSubmissions, '', '李四')).toHaveLength(1);
    expect(filterSubmissions(sampleSubmissions, '', '2024003')).toHaveLength(1);
    expect(filterSubmissions(sampleSubmissions, '', '赵六')).toHaveLength(0);
  });

  it('combines status and search filters', () => {
    const result = filterSubmissions(sampleSubmissions, 'AI_GRADED', '王'); // legacy display status
    expect(result).toHaveLength(1);
    expect(result[0].studentName).toBe('王五');
  });
});

describe('resolveTotalStudentsCount', () => {
  it('uses enrolled count when positive', () => {
    expect(resolveTotalStudentsCount(30, 5)).toBe(30);
  });

  it('falls back to submission count', () => {
    expect(resolveTotalStudentsCount(0, 5)).toBe(5);
    expect(resolveTotalStudentsCount(undefined, 0)).toBe(1);
  });
});

describe('calculateSubmissionRate', () => {
  it('calculates percentage rounded', () => {
    expect(calculateSubmissionRate(3, 30)).toBe(10);
    expect(calculateSubmissionRate(1, 3)).toBe(33);
  });
});

describe('countPendingReview', () => {
  it('counts submitted awaiting review', () => {
    expect(countPendingReview(sampleSubmissions)).toBe(1);
  });
});

describe('calculateAverageScore', () => {
  it('returns dash when no scored submissions', () => {
    expect(calculateAverageScore([{ finalScore: null }])).toBe('—');
  });

  it('calculates average of scored submissions', () => {
    expect(calculateAverageScore(sampleSubmissions)).toBe('82.5');
  });
});
