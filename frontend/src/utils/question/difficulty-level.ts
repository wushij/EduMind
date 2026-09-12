import type { Difficulty } from '@/types/question/question';

export function difficultyToLevel(difficulty?: string): number | undefined {
  if (!difficulty || difficulty === 'ALL') return undefined;
  const map: Record<Difficulty, number> = {
    EASY: 2,
    MEDIUM: 3,
    HARD: 5
  };
  return map[difficulty as Difficulty];
}
