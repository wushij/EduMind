import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ref } from 'vue';
import { useTeachingReport } from './useTeachingReport';
import * as learningAnalyticsModule from '@/composables/analytics/useLearningAnalytics';
import * as teacherCoursesModule from '@/composables/course/useTeacherCourses';

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
      // loadReport 会先还原该课程上一次已生成的诊断建议，mock 必须提供该方法
      loadStoredAdvice: vi.fn(),
      adviceLoading: ref(false),
      teachingAdvice: ref(null)
    } as any);
  });

  it('initializes and loads teaching report with real backend metrics', async () => {
    mockFetchReport.mockResolvedValueOnce({
      courseId: 101,
      courseName: '高等数学(上)',
      courseCode: 'MATH101',
      teacherName: '李教授',
      studentCount: 65,
      syllabusProgress: 40,
      totalChapters: 10,
      range: '7d',
      aiCallCount: 120,
      avgSubmissionRate: 92.3,
      submittedCount: 120,
      expectedSubmissionCount: 130,
      gradedCount: 98,
      passRate: 72,
      avgScore: 68.5,
      knowledgeMasteryAvg: 82,
      masteryEstimated: false,
      masteryStudentCount: 65,
      savedHours: 4.9,
      savedHoursEstimated: true,
      dataUpdatedAt: '2026-09-25 17:17',
      hasRealData: true,
      weeklyActivity: [
        { date: '09-20', count: 18 },
        { date: '09-21', count: 24 }
      ],
      errorCategories: [
        { type: 'CONCEPT', name: '概念理解错误', percent: 60 },
        { type: 'READING', name: '审题理解偏差', percent: 40 }
      ],
      weakPoints: [
        {
          questionId: 1001,
          questionStem: '若 \\(\\lim_{x \\to 0} \\frac{\\sin 2x}{x} = 2\\)，求等价无穷小。',
          questionType: 'SINGLE_CHOICE',
          questionOptions: '[{"key":"A","content":"\\\\(\\\\sin 2x\\\\)"},{"key":"B","content":"\\\\(x\\\\)"}]',
          questionAnswer: 'B',
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
    // 及格率来自后端真实字段，不再复用作业提交率
    expect(report.passRate.value).toBe(72);
    expect(report.masteryRate.value).toBe(82);
    expect(report.masteryEstimated.value).toBe(false);
    expect(report.aiCallCount.value).toBe(120);
    expect(report.syllabusProgress.value).toBe(40);
    expect(report.totalChapters.value).toBe(10);
    expect(report.savedHours.value).toBe(4.9);
    expect(report.savedHoursEstimated.value).toBe(true);
    // 数据更新时间来自后端真实学习行为时间，不使用浏览器本地时间
    expect(report.lastUpdatedTime.value).toBe('2026-09-25 17:17');
    expect(report.hasRealData.value).toBe(true);

    expect(report.knowledgeMasteryList.value.length).toBe(2);
    expect(report.knowledgeMasteryList.value[0].rate).toBe(45);
    expect(report.knowledgeMasteryList.value[0].status).toBe('danger');
    expect(report.knowledgeMasteryList.value[0].name).toBe('等价无穷小替换');
    // 原题选项/答案必须透传到下钻抽屉，否则「考点原题详情」只剩题干、无法讲评
    expect(report.knowledgeMasteryList.value[0].questionType).toBe('SINGLE_CHOICE');
    expect(report.knowledgeMasteryList.value[0].questionOptions).toContain('"key":"B"');
    expect(report.knowledgeMasteryList.value[0].questionAnswer).toBe('B');
    expect(report.knowledgeMasteryList.value[1].rate).toBe(78);
    expect(report.knowledgeMasteryList.value[1].status).toBe('normal');

    // 错因分类使用后端返回的中文名与类型专属配色
    expect(report.errorCategories.value[0].color).toBe('#EF4444');
    expect(report.errorCategories.value[1].color).toBe('#8B5CF6');
  });

  it('keeps missing metrics empty instead of synthesizing placeholder values', async () => {
    mockFetchReport.mockResolvedValueOnce({
      courseId: 101,
      courseName: '高等数学(上)',
      studentCount: 2,
      syllabusProgress: 0,
      totalChapters: 3,
      range: '7d',
      aiCallCount: 0,
      avgSubmissionRate: 0,
      gradedCount: 0,
      passRate: 0,
      knowledgeMasteryAvg: null,
      masteryEstimated: true,
      savedHours: 0,
      savedHoursEstimated: true,
      dataUpdatedAt: null,
      hasRealData: false,
      weeklyActivity: [],
      errorCategories: [],
      weakPoints: [
        {
          questionId: 2001,
          knowledgePointName: '等价无穷小替换',
          title: '等价无穷小替换',
          wrongCount: 3,
          masteryRate: null,
          errorType: null,
          errorTypeName: '待归因',
          status: 'unknown',
          statusLabel: '暂无测评数据'
        }
      ]
    });

    const report = useTeachingReport(101);
    await report.loadReport();

    // 无实测掌握度时保持空值，由界面展示「暂无数据」，不得用公式兜底
    expect(report.masteryRate.value).toBeNull();
    expect(report.knowledgeMasteryList.value[0].rate).toBeNull();
    expect(report.knowledgeMasteryList.value[0].status).toBe('unknown');
    expect(report.knowledgeMasteryList.value[0].statusLabel).toBe('暂无测评数据');
    // 无教学建议时不编造默认建议文案
    expect(report.knowledgeMasteryList.value[0].suggestion).toBeUndefined();
    expect(report.lastUpdatedTime.value).toBe('暂无学习数据');
    expect(report.hasRealData.value).toBe(false);
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
        // 聚焦参数必须传知识点 ID：只传 questionId 时后端查不到考点，会退回占位编号
        knowledgePointId: 1001,
        questionId: 2001
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
      passRate: 90,
      knowledgeMasteryAvg: 85,
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
