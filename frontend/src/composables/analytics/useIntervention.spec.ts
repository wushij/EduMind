import { describe, it, expect } from 'vitest';
import type { TeachingInterventionVO } from '@/types/analytics/intervention';
import {
  filterInterventions,
  getInterventionTriggerLabel,
  getInterventionStatusLabel,
  resolveCourseNameFromOptions
} from './useIntervention';

const sample: TeachingInterventionVO[] = [
  {
    id: 1,
    tenantId: 1,
    courseId: 101,
    courseName: '数据结构',
    triggerType: 'EXAM_WEAK',
    status: 'PENDING',
    title: 'A',
    proposalText: '方案A',
    affectedStudentCount: 3,
    createTime: '2026-01-01T00:00:00Z'
  },
  {
    id: 2,
    tenantId: 1,
    courseId: 102,
    courseName: '高数',
    triggerType: 'ACTIVITY_DROP',
    status: 'DISPATCHED',
    title: 'B',
    proposalText: '方案B',
    affectedStudentCount: 5,
    createTime: '2026-01-02T00:00:00Z'
  }
];

describe('filterInterventions', () => {
  it('returns all items when filters are empty', () => {
    expect(filterInterventions(sample)).toHaveLength(2);
  });

  it('filters by course and status', () => {
    expect(filterInterventions(sample, 101, undefined, 'PENDING')).toHaveLength(1);
  });

  it('filters correctly when courseId is serialized as string from backend', () => {
    const stringCourseSample = [
      { ...sample[0], courseId: '103' as unknown as number },
      { ...sample[1], courseId: '101' as unknown as number }
    ];
    expect(filterInterventions(stringCourseSample, 103)).toHaveLength(1);
    expect(filterInterventions(stringCourseSample, 103)[0].courseId).toBe('103');
  });

  it('filters by trigger type', () => {
    expect(filterInterventions(sample, undefined, 'ACTIVITY_DROP')).toHaveLength(1);
  });
});

describe('intervention label helpers', () => {
  it('maps trigger labels', () => {
    expect(getInterventionTriggerLabel('EXAM_WEAK')).toBe('考试薄弱断层');
  });

  it('maps status labels', () => {
    expect(getInterventionStatusLabel('DISPATCHED')).toBe('执行追踪中');
  });

  it('resolves course names from the real course options', () => {
    const options = [
      { id: 101, name: '数据结构与算法' },
      { id: 258, name: 'Java面向对象程序设计' }
    ];
    expect(resolveCourseNameFromOptions(options, 101)).toBe('数据结构与算法');
    expect(resolveCourseNameFromOptions(options, 258)).toBe('Java面向对象程序设计');
    expect(resolveCourseNameFromOptions(options, 999)).toBeUndefined();
    expect(resolveCourseNameFromOptions(options, undefined)).toBeUndefined();
  });
});
