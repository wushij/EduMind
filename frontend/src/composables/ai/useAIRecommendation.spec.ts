import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import { useAIRecommendation } from './useAIRecommendation';
import { getRecommendations } from '@/api/ai/recommendation';
import { getCourseList } from '@/api/course/course';

vi.mock('@/api/ai/recommendation', () => ({
  getRecommendations: vi.fn()
}));

vi.mock('@/api/course/course', () => ({
  getCourseList: vi.fn()
}));

/** 造一批后端课程列表数据：id 以字符串返回是真实行为，需校验 Number 归一化 */
function givenAvailableCourses(list: Array<{ id: string; title: string }>) {
  (getCourseList as unknown as Mock).mockResolvedValue({ data: { list } });
}

describe('useAIRecommendation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    givenAvailableCourses([]);
  });

  it('解析不到可用课程时不发请求，直接保持空态', async () => {
    const { courseId, loadData, questions, resources, loading } = useAIRecommendation();

    await loadData();

    expect(courseId.value).toBe(0);
    expect(getRecommendations).not.toHaveBeenCalled();
    expect(questions.value).toEqual([]);
    expect(resources.value).toEqual([]);
    expect(loading.value).toBe(false);
  });

  it('按解析出的可用课程拉取推荐（而不是写死的课程 1）', async () => {
    givenAvailableCourses([{ id: '102', title: 'Java面向对象程序设计' }]);
    (getRecommendations as unknown as Mock).mockResolvedValue({
      data: {
        questions: [{ id: 1, stem: '以下哪个是 Java 的基类？' }],
        resources: [{ id: 2, title: '第一章课件' }]
      }
    });

    const { courseId, loadCourses, questions, resources } = useAIRecommendation();

    await loadCourses();
    await flushPromises();

    expect(courseId.value).toBe(102);
    expect(getRecommendations).toHaveBeenCalledWith(102);
    expect(questions.value).toHaveLength(1);
    expect(resources.value).toHaveLength(1);
  });

  it('请求失败时清空列表，错误提示交由 HTTP 拦截器统一给出', async () => {
    givenAvailableCourses([{ id: '101', title: '数据结构与算法' }]);
    (getRecommendations as unknown as Mock).mockRejectedValue(new Error('课程不存在'));

    const { loadData, loadCourses, questions, resources, loading } = useAIRecommendation();

    await loadCourses();
    await loadData();
    await flushPromises();

    expect(questions.value).toEqual([]);
    expect(resources.value).toEqual([]);
    expect(loading.value).toBe(false);
  });

  it('支持显式指定课程作为初始上下文', async () => {
    const { courseId } = useAIRecommendation(258);
    expect(courseId.value).toBe(258);
  });
});
