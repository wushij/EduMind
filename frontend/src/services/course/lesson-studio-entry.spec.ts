import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Router } from 'vue-router';
import { createChapterApi, getChapters } from '@/api/course/chapter';
import { updateLesson } from '@/api/course/lesson';
import { takeLessonPrepDiagnosis } from '@/utils/course/lesson-prep-diagnosis';
import { openLessonStudio } from './lesson-studio-entry';

vi.mock('@/api/course/chapter', () => ({
  getChapters: vi.fn(),
  createChapterApi: vi.fn()
}));

vi.mock('@/api/course/lesson', () => ({
  updateLesson: vi.fn()
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    info: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}));

const mockGetChapters = vi.mocked(getChapters);
const mockCreateChapter = vi.mocked(createChapterApi);
const mockUpdateLesson = vi.mocked(updateLesson);

/** 后端 ChapterTreeVO：课节挂在 children 下，且响应里没有 sections 字段 */
const chapterTree = [
  {
    id: 11,
    title: '第一章 函数与极限论',
    children: [{ id: 101, title: '1.1 数列与函数极限计算', sort: 1 }]
  },
  {
    id: 12,
    title: '第二章 导数与微分',
    children: [{ id: 102, title: '2.1 导数的定义与几何意义', sort: 1 }]
  }
];

function createRouterMock() {
  return { push: vi.fn() } as unknown as Router;
}

beforeEach(() => {
  vi.clearAllMocks();
  window.sessionStorage.clear();
  mockGetChapters.mockResolvedValue({ data: chapterTree } as never);
  mockUpdateLesson.mockResolvedValue({} as never);
});

describe('openLessonStudio 的备课课节新建模式', () => {
  it('按薄弱考点匹配章节，续接序号新建课节并携带自动备课标记', async () => {
    mockCreateChapter.mockResolvedValue({ data: 999 } as never);
    const router = createRouterMock();

    // 「微分」命中第二章标题，但未被任何已有课节覆盖，因此应当新建而不是复用
    await openLessonStudio(router, 5, { weakPoints: [{ name: '微分' }] });

    expect(mockCreateChapter).toHaveBeenCalledTimes(1);
    expect(mockCreateChapter).toHaveBeenCalledWith(
      5,
      expect.objectContaining({
        title: '2.2 微分专项突破',
        parentId: 12,
        sortOrder: 2,
        lessonType: 'LECTURE'
      })
    );
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/999/edit?aiPrep=1');
  });

  it('续接排序值时取已有课节的最大 sortOrder，避免与跳号数据撞号', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            { id: 101, title: '1.1 数列与函数极限计算', sort: 1 },
            { id: 103, title: '1.2 无穷小与等价代换', sort: 5 }
          ]
        }
      ]
    } as never);
    mockCreateChapter.mockResolvedValue({ data: 555 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '微分' }] });

    expect(mockCreateChapter).toHaveBeenCalledWith(
      5,
      expect.objectContaining({ title: '1.3 微分专项突破', sortOrder: 6 })
    );
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/555/edit?aiPrep=1');
  });

  it('续编号取已有标题里的最大节号，删过课节后也不会与残留编号撞号', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            { id: 101, title: '1.1 数列与函数极限计算', sort: 1 },
            { id: 104, title: '1.4 两个重要极限专项突破', sort: 4 }
          ]
        }
      ]
    } as never);
    mockCreateChapter.mockResolvedValue({ data: 556 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '微分' }] });

    // 数量只有 2 个，但 1.3 会与「已讲过 1.4」的课程顺序冲突，因此必须顺延到 1.5
    expect(mockCreateChapter).toHaveBeenCalledWith(
      5,
      expect.objectContaining({ title: '1.5 微分专项突破', sortOrder: 5 })
    );
  });

  it('导读写成面向学生的自然文案：不搬运错因原文，也不出现后台术语', async () => {
    mockCreateChapter.mockResolvedValue({ data: 888 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, {
      weakPoints: [
        {
          name: '等价无穷小代换及其应用条件与边界判定',
          errorTypeName: '概念理解错误',
          errorReason: '最根本原因是：学生没有准确掌握$$\\frac{f(2x)-f(0)}{x}$$的等价判定标准，'.repeat(3)
        }
      ]
    });

    const payload = mockCreateChapter.mock.calls[0][1] as unknown as {
      title: string;
      description: string;
    };
    // 标题只取主考点主干（在「及其」处断开），导读里则列出完整考点名
    expect(payload.title).toBe('1.2 等价无穷小代换专项突破');
    expect(payload.title.length).toBeLessThanOrEqual(20);

    expect(payload.description).toContain('等价无穷小代换及其应用条件与边界判定');
    expect(payload.description).not.toMatch(/[\\$^_{}~]/);
    expect(payload.description).not.toContain('依据教情报告');
    expect(payload.description).not.toContain('学情提示：');
    expect(payload.description).not.toContain('概念理解错误');
    expect(payload.description.length).toBeLessThanOrEqual(281);
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/888/edit?aiPrep=1');
  });

  it('错因分析写入 AI 学情诊断通道（供备课 prompt 使用），不写进课节导读', async () => {
    mockCreateChapter.mockResolvedValue({ data: 999 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, {
      weakPoints: [
        {
          name: '等价无穷小代换',
          errorTypeName: '概念理解错误',
          errorReason:
            '最根本原因是：学生没有准确掌握$$\\frac{f(2x)-f(0)}{x}$$的等价判定标准，' +
            '误把仅同阶的无穷小当成与 \\(x\\) 等价。'
        }
      ]
    });

    // 导读保持自然文案，不含错因分析
    const payload = mockCreateChapter.mock.calls[0][1] as unknown as { description: string };
    expect(payload.description).not.toContain('最根本原因');
    expect(payload.description).not.toContain('概念理解错误');

    // 错因分析通过一次性诊断通道交给 AI
    const diagnosis = takeLessonPrepDiagnosis(5, 999);
    expect(diagnosis).toContain('等价无穷小代换');
    expect(diagnosis).toContain('概念理解错误');
    expect(diagnosis).toContain('最根本原因是');
    expect(diagnosis).not.toMatch(/[\\$^_{}~]/);
    // 读取即销毁，避免刷新或二次进入重复携带
    expect(takeLessonPrepDiagnosis(5, 999)).toBe('');
  });

  it('诊断会清理空括号、按句边界截断，纯公式错因不产生空条目', async () => {
    mockCreateChapter.mockResolvedValue({ data: 999 } as never);
    const router = createRouterMock();

    const longReason =
      '最根本原因是：学生没有准确掌握等价无穷小的判定标准，误把仅同阶（如 \\(\\sin2x\\sim2x\\)）' +
      '或高阶（如 \\(1-\\cos x\\)）的无穷小当成与 \\(x\\) 等价。' +
      '教学建议按导数定义拆解增量结构并配套变式训练。'.repeat(6);

    await openLessonStudio(router, 5, {
      weakPoints: [
        { name: '等价无穷小代换', errorTypeName: '概念理解错误', errorReason: longReason },
        {
          name: '复合函数链式求导法则',
          errorTypeName: '概念理解错误',
          errorReason: '$$\\frac{dy}{dx}=\\frac{dy}{du}\\cdot\\frac{du}{dx}$$'
        }
      ]
    });

    const diagnosis = takeLessonPrepDiagnosis(5, 999);
    expect(diagnosis).not.toMatch(/[\\$^_{}~]/);
    expect(diagnosis).not.toContain('（如 ）');

    // 长错因截断到完整句子，而不是半句加省略号
    const firstLine = diagnosis.split('\n')[0];
    expect(firstLine.endsWith('。')).toBe(true);

    // 错因全是公式时只保留考点与错因类型，不出现空冒号
    expect(diagnosis).toContain('- 复合函数链式求导法则【概念理解错误】');
    expect(diagnosis).not.toContain('复合函数链式求导法则【概念理解错误】：');
  });

  it('超长错因原文不会进入导读，导读长度始终可控', async () => {
    mockCreateChapter.mockResolvedValue({ data: 777 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, {
      weakPoints: [
        {
          name: '微分',
          errorTypeName: '概念理解错误',
          errorReason:
            '最根本原因是：学生没有准确掌握“等价无穷小”的判定标准——两个无穷小量比值极限应为 1，' +
            '误把仅同阶（如 \\(\\sin2x\\sim2x\\)）或高阶（如 \\(1-\\cos x\\)、\\(e^x-1-x\\)）的无穷小当成与 \\(x\\) 等价，' +
            '属于概念理解错误；' +
            '后续教学建议围绕导数定义拆解增量结构并配套变式训练。'.repeat(8)
        }
      ]
    });

    const payload = mockCreateChapter.mock.calls[0][1] as unknown as { description: string };
    // 导读是纯文本字段：不能残留公式定界符，也不能把整段学情分析照搬进去
    expect(payload.description).not.toMatch(/[\\$^_{}~]/);
    expect(payload.description).not.toContain('学情提示：');
    expect(payload.description).not.toContain('最根本原因');
    expect(payload.description).toContain('微分');
    expect(payload.description.length).toBeLessThanOrEqual(281);
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/777/edit?aiPrep=1');
  });

  it('已存在聚焦同一考点的课节时直接复用，不再重复新建', async () => {
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '函数极限' }] });

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/101/edit?aiPrep=1');
  });

  it('课程还没有章层时先补建章，保证新课节满足后端的课节节点判定', async () => {
    mockGetChapters.mockResolvedValue({ data: [] } as never);
    mockCreateChapter
      .mockResolvedValueOnce({ data: 77 } as never)
      .mockResolvedValueOnce({ data: 88 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '等价无穷小代换' }] });

    expect(mockCreateChapter).toHaveBeenNthCalledWith(
      1,
      5,
      expect.objectContaining({ title: '等价无穷小代换专项突破', sortOrder: 1 })
    );
    expect(mockCreateChapter).toHaveBeenNthCalledWith(
      2,
      5,
      expect.objectContaining({ title: '1.1 等价无穷小代换专项突破', parentId: 77 })
    );
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/88/edit?aiPrep=1');
  });

  it('autoPrepare 为 false 时只新建课节、不要求工作台自动备课', async () => {
    mockCreateChapter.mockResolvedValue({ data: 999 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '微分' }], autoPrepare: false });

    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/999/edit');
  });

  it('未传薄弱考点时沿用旧行为：进入课程第一个已有课节', async () => {
    const router = createRouterMock();

    await openLessonStudio(router, 5);

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/101/edit');
  });

  it('指定 lessonId 时直接进入该课节', async () => {
    const router = createRouterMock();

    await openLessonStudio(router, 5, { lessonId: 102 });

    expect(mockGetChapters).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/102/edit');
  });

  it('大纲接口失败时退回大纲页，不误报「没有课节」', async () => {
    mockGetChapters.mockRejectedValue(new Error('network down'));
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '导数' }] });

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/chapters');
  });

  it('复用旧备课课节时，超限或含公式的旧导读会被刷新为可读版本', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            {
              id: 101,
              title: '1.1 函数极限专项突破',
              description:
                '本课节依据教情报告自动创建，聚焦薄弱考点：函数极限（概念理解错误）。学情提示：' +
                '\\(\\sin2x\\sim2x\\)'.repeat(40)
            }
          ]
        }
      ]
    } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '函数极限' }] });

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(mockUpdateLesson).toHaveBeenCalledTimes(1);
    const [, lessonId, payload] = mockUpdateLesson.mock.calls[0];
    expect(lessonId).toBe(101);
    const refreshed = (payload as unknown as { description: string }).description;
    expect(refreshed).not.toMatch(/[\\$^_{}~]/);
    expect(refreshed.length).toBeLessThanOrEqual(281);
  });

  it('复用早期模板生成的导读（不超限也无公式）同样会被刷新为最新文案', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            {
              id: 101,
              title: '1.1 函数极限专项突破',
              description:
                '本课节依据教情报告自动创建，聚焦薄弱考点：函数极限（概念理解错误）。' +
                '建议围绕上述考点安排典型例题讲评、变式巩固与错因复盘，并配套定向练习。' +
                '学情提示：最根本原因是：学生没有准确掌握判定标准，误把仅同阶或高阶的无穷小当成与 x 等价。'
            }
          ]
        }
      ]
    } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '函数极限' }] });

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(mockUpdateLesson).toHaveBeenCalledTimes(1);
    const refreshed = (mockUpdateLesson.mock.calls[0][2] as unknown as { description: string })
      .description;
    expect(refreshed).not.toContain('依据教情报告');
    expect(refreshed).not.toContain('学情提示：');
    expect(refreshed).toContain('函数极限');
  });

  it('复用旧课节时，过长的自动标题会被缩短并保留原有序号', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            {
              id: 101,
              title: '1.2 等价无穷小代换及其应用条件与洛必达法则求未定式极限专项突破',
              description: '本节先复习数列极限的计算方法，再自然引出函数极限的定义与判定。'
            }
          ]
        }
      ]
    } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, {
      weakPoints: [{ name: '等价无穷小代换及其应用条件与洛必达法则求未定式极限' }]
    });

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(mockUpdateLesson).toHaveBeenCalledTimes(1);
    const [courseId, lessonId, patch] = mockUpdateLesson.mock.calls[0];
    expect(courseId).toBe(5);
    expect(lessonId).toBe(101);
    expect((patch as unknown as { title: string }).title).toBe('1.2 等价无穷小代换专项突破');
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/101/edit?aiPrep=1');
  });

  it('考点名没有连接词可依时保留完整名称，标题里不出现半截词', async () => {
    mockCreateChapter.mockResolvedValue({ data: 999 } as never);
    const router = createRouterMock();

    // 「洛必达法则求未定式极限」11 字且不含「及其/与/和」：旧规则会硬截成「…求未定式极…」
    await openLessonStudio(router, 5, { weakPoints: [{ name: '洛必达法则求未定式极限' }] });

    const payload = mockCreateChapter.mock.calls[0][1] as unknown as { title: string };
    expect(payload.title).toBe('1.2 洛必达法则求未定式极限专项突破');
    expect(payload.title).not.toContain('…');
  });

  it('标题超出总长上限时优先丢掉「专项突破」后缀，保住完整考点名', async () => {
    mockCreateChapter.mockResolvedValue({ data: 999 } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '等价无穷小代换及其应用条件' }] });

    const payload = mockCreateChapter.mock.calls[0][1] as unknown as { title: string };
    expect(payload.title).toBe('1.2 等价无穷小代换及其应用条件');
    expect(payload.title).not.toContain('…');
  });

  it('被旧规则截断过标题的课节仍按考点指纹复用，并刷新为完整标题', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            {
              id: 101,
              title: '1.2 洛必达法则求未定式极…专项突破',
              description: '本节先梳理极限判型，再讲洛必达法则的适用条件与使用边界。'
            }
          ]
        }
      ]
    } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '洛必达法则求未定式极限' }] });

    expect(mockCreateChapter).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/101/edit?aiPrep=1');
    const patch = mockUpdateLesson.mock.calls[0][2] as unknown as { title: string };
    expect(patch.title).toBe('1.2 洛必达法则求未定式极限专项突破');
  });

  it('标题已符合当前规则时不再重复写库与提示', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            {
              id: 101,
              title: '1.2 洛必达法则求未定式极限专项突破',
              description: '本节先梳理极限判型，再讲洛必达法则的适用条件与使用边界。'
            }
          ]
        }
      ]
    } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '洛必达法则求未定式极限' }] });

    expect(mockUpdateLesson).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/101/edit?aiPrep=1');
  });

  it('复用旧备课课节时，教师手写的正常导读不会被覆盖', async () => {
    mockGetChapters.mockResolvedValue({
      data: [
        {
          id: 11,
          title: '第一章 函数与极限论',
          children: [
            {
              id: 101,
              title: '1.1 函数极限专项突破',
              description: '本节先复习数列极限的计算方法，再自然引出函数极限的定义与判定。'
            }
          ]
        }
      ]
    } as never);
    const router = createRouterMock();

    await openLessonStudio(router, 5, { weakPoints: [{ name: '函数极限' }] });

    expect(mockUpdateLesson).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/course/5/lessons/101/edit?aiPrep=1');
  });
});
