import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ref } from 'vue';
import { useTeachingReport } from './useTeachingReport';
import * as learningAnalyticsModule from '@/composables/analytics/useLearningAnalytics';
import * as teacherCoursesModule from '@/composables/course/useTeacherCourses';
import * as routeAccessModule from '@/utils/router/route-access';

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}));

describe('useTeachingReport composable', () => {
  const mockFetchReport = vi.fn();
  const mockFetchAdvice = vi.fn();
  const mockStopAdvice = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(teacherCoursesModule, 'useTeacherCourses').mockReturnValue({
      courseOptions: ref([
        { id: 101, name: '高等数学(上)' },
        { id: 102, name: '线性代数' }
      ]),
      courseId: ref(101),
      loading: ref(false),
      courseIdCorrected: ref(false),
      loadCourses: vi.fn().mockResolvedValue([])
    } as any);

    vi.spyOn(learningAnalyticsModule, 'useLearningAnalytics').mockReturnValue({
      fetchTeachingReport: mockFetchReport,
      fetchTeachingAdvice: mockFetchAdvice,
      stopTeachingAdvice: mockStopAdvice,
      adviceLoading: ref(false),
      teachingAdvice: ref(null)
    } as any);
  });

  it('initializes and loads teaching report with rich weak points and metadata', async () => {
    mockFetchReport.mockResolvedValueOnce({
      courseId: 101,
      courseName: '高等数学(上)',
      courseCode: 'MATH101',
      teacherName: '李教授',
      studentCount: 65,
      syllabusProgress: 75,
      avgSubmissionRate: 88.5,
      knowledgeMasteryAvg: 0.82,
      aiCallCount: 120,
      weeklyActivity: [
        { date: '09-20', count: 18 },
        { date: '09-21', count: 24 }
      ],
      errorCategories: [
        { type: 'CONCEPT', name: '概念理解错误', percent: 60 }
      ],
      weakPoints: [
        {
          questionId: 1001,
          questionStem: '若 \\(\\lim_{x \\to 0} \\frac{\\sin 2x}{x} = 2\\)，求等价无穷小。',
          knowledgePointId: 201,
          knowledgePointName: '等价无穷小替换',
          masteryRate: 45,
          status: 'danger',
          statusLabel: '急需攻坚',
          errorType: 'CONCEPT',
          errorTypeName: '概念理解错误',
          errorReason: '学生没有准确掌握等价无穷小的判定标准：两个无穷小量比值极限应为1，误把 \\(\\sin 2x \\sim 2x\\) 当作一般同阶无穷小。',
          suggestion: '建议强化概念辨析并安排 5 分钟微测验。'
        },
        {
          questionId: 1002,
          questionStem: '求导数 \\(f\'(x)\\)',
          knowledgePointId: 202,
          knowledgePointName: '复合函数求导法则',
          masteryRate: 78,
          status: 'normal',
          statusLabel: '稳步提升中',
          errorType: 'CALC',
          errorTypeName: '计算失误',
          errorReason: '未按链式法则逐层求导，漏算最内层因子的导数。',
          suggestion: '指导学生按由外到内的层次分步演算。'
        }
      ]
    });

    const report = useTeachingReport(101);
    await report.loadReport();

    expect(report.courseName.value).toBe('高等数学(上)');
    expect(report.courseCode.value).toBe('MATH101');
    expect(report.studentCount.value).toBe(65);
    expect(report.passRate.value).toBe(88.5);
    expect(report.masteryRate.value).toBe(82);
    expect(report.aiCallCount.value).toBe(120);

    // 验证考点掌握度榜单不再全为 76%
    expect(report.knowledgeMasteryList.value.length).toBe(2);
    expect(report.knowledgeMasteryList.value[0].rate).toBe(45);
    expect(report.knowledgeMasteryList.value[0].status).toBe('danger');
    expect(report.knowledgeMasteryList.value[0].name).toBe('等价无穷小替换');
    expect(report.knowledgeMasteryList.value[1].rate).toBe(78);
    expect(report.knowledgeMasteryList.value[1].status).toBe('normal');
  });

  it('triggers AI advice thinking modal correctly', async () => {
    const report = useTeachingReport(101);
    report.knowledgeMasteryList.value = [
      {
        id: 1001,
        index: 1,
        name: '等价无穷小替换',
        course: '高等数学(上)',
        rate: 45,
        status: 'danger',
        statusLabel: '急需攻坚',
        questionId: 1001
      }
    ];

    report.handleOpenAiAdvice();
    expect(report.aiThinkingModalVisible.value).toBe(true);
    expect(mockFetchAdvice).toHaveBeenCalledWith({
      courseId: 101,
      focusKnowledgePointIds: [1001]
    });
  });

  it('supports course and range changes with reload', async () => {
    mockFetchReport.mockResolvedValue({
      courseId: 102,
      courseName: '线性代数',
      avgSubmissionRate: 90,
      knowledgeMasteryAvg: 0.85,
      weakPoints: []
    });

    const report = useTeachingReport(101);
    report.handleCourseChange(102);

    expect(report.courseId.value).toBe(102);
    expect(mockFetchReport).toHaveBeenCalledWith(102, '7d');

    report.handleRangeChange('30d');
    expect(report.currentRange.value).toBe('30d');
    expect(mockFetchReport).toHaveBeenCalledWith(102, '30d');
  });
});
