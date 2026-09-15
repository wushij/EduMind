import { describe, it, expect } from 'vitest';
import type { QuestionItem } from '@/types/question/question';
import type { ExamSection } from './useExam';
import {
  getChineseNumber,
  getExamTypeLabel,
  getExamStatusLabel,
  calculateSectionsTotalScore,
  calculateSectionsTotalQuestions,
  getSectionScore,
  createDefaultExamSections,
  createSectionByType,
  filterPickerQuestions,
  groupQuestionsByType,
  calculateDifficultyCounts,
  calculateDifficultyPercentages,
  collectKnowledgePoints
} from './useExam';

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
    type: 'SINGLE_CHOICE',
    difficulty: 'MEDIUM',
    score: 5,
    stem: 'q2',
    analysis: '',
    knowledgePointNames: ['B']
  },
  {
    id: 3,
    courseId: 101,
    type: 'SHORT_ANSWER',
    difficulty: 'HARD',
    score: 10,
    stem: 'q3',
    analysis: '',
    knowledgePointNames: ['A', 'C']
  }
];

describe('getChineseNumber', () => {
  it('converts numbers up to ten', () => {
    expect(getChineseNumber(1)).toBe('一');
    expect(getChineseNumber(10)).toBe('十');
    expect(getChineseNumber(11)).toBe('11');
  });
});

describe('exam label helpers', () => {
  it('maps type and status labels', () => {
    expect(getExamTypeLabel('FILL_BLANK')).toBe('填空题');
    expect(getExamStatusLabel('DRAFT')).toBe('草稿暂存');
    expect(getExamStatusLabel()).toBe('正式发布');
  });
});

describe('section score helpers', () => {
  const sections: ExamSection[] = [
    {
      id: 'sec-1',
      type: 'SINGLE_CHOICE',
      title: '单选',
      defaultScore: 5,
      questions: [sampleQuestions[0], sampleQuestions[1]]
    },
    {
      id: 'sec-2',
      type: 'SHORT_ANSWER',
      title: '简答',
      defaultScore: 10,
      questions: [sampleQuestions[2]]
    }
  ];

  it('calculates total score and question count', () => {
    expect(calculateSectionsTotalScore(sections)).toBe(20);
    expect(calculateSectionsTotalQuestions(sections)).toBe(3);
    expect(getSectionScore(sections[0])).toBe(10);
  });
});

describe('createDefaultExamSections', () => {
  it('creates three default sections', () => {
    const sections = createDefaultExamSections();
    expect(sections).toHaveLength(3);
    expect(sections[0].type).toBe('SINGLE_CHOICE');
    expect(sections[2].defaultScore).toBe(10);
  });
});

describe('createSectionByType', () => {
  it('creates section with type-specific defaults', () => {
    const section = createSectionByType('TRUE_FALSE');
    expect(section.type).toBe('TRUE_FALSE');
    expect(section.title).toBe('判断题');
    expect(section.defaultScore).toBe(3);
    expect(section.questions).toEqual([]);
  });
});

describe('filterPickerQuestions', () => {
  const section: ExamSection = {
    id: 'sec-1',
    type: 'SINGLE_CHOICE',
    title: '单选',
    defaultScore: 5,
    questions: [sampleQuestions[0]]
  };

  it('returns empty when section is null', () => {
    expect(filterPickerQuestions(sampleQuestions, null, '', '')).toEqual([]);
  });

  it('filters by section type, existing ids, difficulty and keyword', () => {
    const result = filterPickerQuestions(sampleQuestions, section, 'q2', 'MEDIUM');
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(2);
  });
});

describe('groupQuestionsByType', () => {
  it('groups questions and sums section scores', () => {
    const grouped = groupQuestionsByType(sampleQuestions);
    expect(grouped).toHaveLength(2);
    expect(grouped[0].questions).toHaveLength(2);
    expect(grouped[0].totalScore).toBe(10);
    expect(grouped[1].title).toBe('综合应用与简答题');
  });
});

describe('difficulty and knowledge helpers', () => {
  it('counts difficulties and calculates percentages', () => {
    const counts = calculateDifficultyCounts(sampleQuestions);
    expect(counts).toEqual({ EASY: 1, MEDIUM: 1, HARD: 1 });

    const percentages = calculateDifficultyPercentages(counts, 3);
    expect(percentages.EASY).toBe(33);
    expect(percentages.MEDIUM).toBe(33);
    expect(percentages.HARD).toBe(33);
  });

  it('collects unique knowledge points', () => {
    expect(collectKnowledgePoints(sampleQuestions)).toEqual(['A', 'B', 'C']);
  });
});
