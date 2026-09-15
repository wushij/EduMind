import { describe, it, expect } from 'vitest';
import type { QuestionItem } from '@/types/question/question';
import {
  calculateBankTotalScore,
  filterBankQuestions,
  filterCandidateQuestions,
  toggleArraySelection,
  getTypeLabel,
  getTypeTagType,
  getDifficultyLabel,
  getDifficultyTagType,
  resolveCourseName
} from './useBank';
import type { Course } from '@/types/course/course';

const sampleQuestions: QuestionItem[] = [
  {
    id: 1,
    courseId: 101,
    type: 'SINGLE_CHOICE',
    difficulty: 'EASY',
    score: 5,
    stem: '二叉树遍历基础',
    analysis: '',
    knowledgePointNames: ['数据结构', '二叉树']
  },
  {
    id: 2,
    courseId: 101,
    type: 'SHORT_ANSWER',
    difficulty: 'HARD',
    score: 10,
    stem: '图的最短路径算法',
    analysis: '',
    knowledgePointNames: ['图论']
  },
  {
    id: 3,
    courseId: 101,
    type: 'SINGLE_CHOICE',
    difficulty: 'MEDIUM',
    score: 5,
    stem: '栈与队列应用',
    analysis: '',
    knowledgePointNames: ['栈']
  }
];

describe('calculateBankTotalScore', () => {
  it('sums question scores with default fallback', () => {
    expect(calculateBankTotalScore(sampleQuestions)).toBe(20);
  });

  it('uses default score of 5 when score is missing', () => {
    const items = [{ ...sampleQuestions[0], score: undefined as unknown as number }];
    expect(calculateBankTotalScore(items)).toBe(5);
  });
});

describe('filterBankQuestions', () => {
  it('returns all questions when filters are empty', () => {
    expect(
      filterBankQuestions(sampleQuestions, {
        searchKeyword: '',
        filterType: '',
        filterDifficulty: ''
      })
    ).toHaveLength(3);
  });

  it('filters by type and difficulty', () => {
    const result = filterBankQuestions(sampleQuestions, {
      searchKeyword: '',
      filterType: 'SINGLE_CHOICE',
      filterDifficulty: 'EASY'
    });
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(1);
  });

  it('filters by keyword in stem or knowledge points', () => {
    const byStem = filterBankQuestions(sampleQuestions, {
      searchKeyword: '最短路径',
      filterType: '',
      filterDifficulty: ''
    });
    expect(byStem).toHaveLength(1);
    expect(byStem[0].id).toBe(2);

    const byKp = filterBankQuestions(sampleQuestions, {
      searchKeyword: '栈',
      filterType: '',
      filterDifficulty: ''
    });
    expect(byKp).toHaveLength(1);
    expect(byKp[0].id).toBe(3);
  });
});

describe('filterCandidateQuestions', () => {
  it('excludes questions already in bank', () => {
    const result = filterCandidateQuestions(sampleQuestions, [1, 2], {
      drawerSearch: '',
      drawerType: ''
    });
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(3);
  });

  it('applies drawer type and search filters', () => {
    const result = filterCandidateQuestions(sampleQuestions, [], {
      drawerSearch: '图',
      drawerType: 'SHORT_ANSWER'
    });
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(2);
  });
});

describe('toggleArraySelection', () => {
  it('adds value when not present', () => {
    expect(toggleArraySelection([1, 2], 3)).toEqual([1, 2, 3]);
  });

  it('removes value when present', () => {
    expect(toggleArraySelection([1, 2, 3], 2)).toEqual([1, 3]);
  });
});

describe('resolveCourseName', () => {
  const courses: Course[] = [
    { id: 101, title: '数据结构' } as Course,
    { id: 102, title: 'Java' } as Course
  ];

  it('returns course title when found', () => {
    expect(resolveCourseName(courses, 101)).toBe('数据结构');
  });

  it('returns fallback label when course is missing', () => {
    expect(resolveCourseName(courses, 999)).toBe('专业核心课');
  });
});

describe('bank label helpers', () => {
  it('maps question type labels and tag types', () => {
    expect(getTypeLabel('SINGLE_CHOICE')).toBe('单选题');
    expect(getTypeTagType('MULTIPLE_CHOICE')).toBe('success');
    expect(getTypeLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('maps difficulty labels and tag types', () => {
    expect(getDifficultyLabel('EASY')).toBe('简单');
    expect(getDifficultyTagType('HARD')).toBe('danger');
    expect(getDifficultyLabel('UNKNOWN')).toBe('中等');
  });
});
