import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ref } from 'vue';
import { useAIRecommendation } from './useAIRecommendation';

vi.mock('@/api/ai/recommendation', () => ({
  getRecommendations: vi.fn()
}));

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() }
}));

describe('useAIRecommendation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initializes with default course id', () => {
    const { courseId } = useAIRecommendation();
    expect(courseId.value).toBe(1);
  });

  it('accepts custom initial course id', () => {
    const { courseId } = useAIRecommendation(42);
    expect(courseId.value).toBe(42);
  });

  it('exposes reactive recommendation collections', () => {
    const { questions, resources, loading } = useAIRecommendation();
    expect(questions.value).toEqual([]);
    expect(resources.value).toEqual([]);
    expect(loading.value).toBe(false);
  });
});
