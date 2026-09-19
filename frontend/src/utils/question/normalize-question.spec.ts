import { describe, expect, it } from 'vitest';
import { difficultyToApiLevel, serializeQuestionForApi } from './normalize-question';

describe('serializeQuestionForApi', () => {
  it('maps frontend fields to backend DTO shape', () => {
    const payload = serializeQuestionForApi({
      courseId: 258,
      type: 'SINGLE_CHOICE',
      stem: '题干',
      difficulty: 'MEDIUM',
      correctAnswer: 'B',
      options: [
        { key: 'A', content: '1' },
        { key: 'B', content: '2', isCorrect: true }
      ],
      analysis: '解析',
      score: 5
    });
    expect(payload.answer).toBe('B');
    expect(payload.difficulty).toBe(3);
    expect(payload.courseId).toBe(258);
    expect(typeof payload.options).toBe('string');
    expect(JSON.parse(payload.options as string)).toHaveLength(2);
  });

  it('converts difficulty labels to numeric levels', () => {
    expect(difficultyToApiLevel('EASY')).toBe(2);
    expect(difficultyToApiLevel('HARD')).toBe(4);
  });
});
