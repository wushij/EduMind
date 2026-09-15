import { describe, it, expect } from 'vitest';
import type { Course } from '@/types/course/course';
import {
  DEFAULT_PUBLIC_COURSES,
  buildStatusTabs,
  countActiveCourses,
  countArchivedCourses,
  resolvePublicCoursesList
} from './useCourseList';

const sampleCourses: Course[] = [
  {
    id: 1,
    title: '数据结构',
    teacherName: '张老师',
    studentCount: 30,
    chapterCount: 8,
    status: 'ACTIVE'
  },
  {
    id: 2,
    title: '高等数学',
    teacherName: '李老师',
    studentCount: 50,
    chapterCount: 12,
    status: 1
  },
  {
    id: 3,
    title: '软件工程',
    teacherName: '王老师',
    studentCount: 20,
    chapterCount: 10,
    status: 'ARCHIVED'
  },
  {
    id: 4,
    title: '人工智能',
    teacherName: '赵老师',
    studentCount: 40,
    chapterCount: 9,
    status: 2
  },
  {
    id: 5,
    title: '计算机网络',
    teacherName: '陈老师',
    studentCount: 35,
    chapterCount: 11,
    status: 'ACTIVE'
  }
];

describe('countActiveCourses', () => {
  it('counts ACTIVE string and numeric status 1 courses', () => {
    expect(countActiveCourses(sampleCourses)).toBe(3);
  });

  it('returns zero for empty list', () => {
    expect(countActiveCourses([])).toBe(0);
  });
});

describe('countArchivedCourses', () => {
  it('counts ARCHIVED string and numeric status 2 courses', () => {
    expect(countArchivedCourses(sampleCourses)).toBe(2);
  });
});

describe('buildStatusTabs', () => {
  it('builds three tabs with provided counts', () => {
    const tabs = buildStatusTabs(12, 8, 4);
    expect(tabs).toHaveLength(3);
    expect(tabs[0]).toEqual({ label: '全部课程', value: 'ALL', count: 12 });
    expect(tabs[1]).toEqual({ label: '进行中', value: 'ACTIVE', count: 8 });
    expect(tabs[2]).toEqual({ label: '已结课', value: 'ARCHIVED', count: 4 });
  });
});

describe('resolvePublicCoursesList', () => {
  it('returns first four courses when list is non-empty', () => {
    const result = resolvePublicCoursesList(sampleCourses);
    expect(result).toHaveLength(4);
    expect(result[0].title).toBe('数据结构');
    expect(result[3].title).toBe('人工智能');
  });

  it('returns default public courses when list is empty', () => {
    expect(resolvePublicCoursesList([])).toEqual([...DEFAULT_PUBLIC_COURSES]);
  });
});

describe('DEFAULT_PUBLIC_COURSES', () => {
  it('defines four fallback public courses', () => {
    expect(DEFAULT_PUBLIC_COURSES).toHaveLength(4);
    expect(DEFAULT_PUBLIC_COURSES[0].code).toBe('CS201');
  });
});
