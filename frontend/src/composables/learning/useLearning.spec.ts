import { describe, it, expect } from 'vitest';

describe('useLearning module', () => {
  it('exports useLearning as alias for learning path composable', async () => {
    const mod = await import('./useLearning');
    expect(typeof mod.useLearning).toBe('function');
    expect(typeof mod.useLearningPath).toBe('function');
  });

  it('useLearningPath is an alias of useLearning', async () => {
    const mod = await import('./useLearning');
    expect(mod.useLearningPath).toBe(mod.useLearning);
  });
});
