import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ref, nextTick } from 'vue';
import { getLearningReport } from '@/api/learning/report';

vi.mock('@/api/learning/report', () => ({
  getLearningReport: vi.fn()
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  })
}));

describe('useLearningReport', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('passes range to getLearningReport when loading', async () => {
    vi.mocked(getLearningReport).mockResolvedValue({
      data: {
        courseId: 102,
        courseName: 'Java面向对象程序设计',
        portrait: null,
        trends: { studyMinutesByDate: [], scoreByDate: [] },
        enrolledCourses: [{ courseId: 102, courseName: 'Java面向对象程序设计' }]
      }
    } as never);

    const { useLearningReport } = await import('./useLearningReport');
    const { timeRange, loadReport, courseId } = useLearningReport();
    courseId.value = 102;
    timeRange.value = '7d';
    await loadReport();

    expect(getLearningReport).toHaveBeenCalledWith({ courseId: 102, range: '7d' });
  });

  it('marks notEnrolled when API returns empty courses', async () => {
    vi.mocked(getLearningReport).mockResolvedValue({
      data: {
        courseId: null,
        courseName: null,
        portrait: null,
        trends: { studyMinutesByDate: [], scoreByDate: [] },
        enrolledCourses: []
      }
    } as never);

    const { useLearningReport } = await import('./useLearningReport');
    const { init, notEnrolled } = useLearningReport();
    await init();
    await nextTick();
    expect(notEnrolled.value).toBe(true);
  });
});
