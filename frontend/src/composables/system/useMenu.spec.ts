import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { SysMenu } from '@/types/system/menu';
import {
  buildParentTreeOptions,
  countNodes,
  countTypeStats,
  inferMenuType,
  queryComponentSuggestions
} from './useMenu';

vi.mock('@/api/system/menu', () => ({
  getMenuTree: vi.fn(),
  createMenu: vi.fn(),
  updateMenu: vi.fn(),
  deleteMenu: vi.fn(),
  resetDefaultMenus: vi.fn()
}));

const sampleTree: SysMenu[] = [
  {
    id: 1,
    parentId: 0,
    name: '课程中心',
    type: 1,
    sort: 1,
    status: 1,
    children: [
      {
        id: 11,
        parentId: 1,
        name: '课程列表',
        type: 2,
        path: '/course',
        sort: 1,
        status: 1,
        children: [
          {
            id: 111,
            parentId: 11,
            name: '新增课程',
            type: 3,
            permission: 'course:create',
            sort: 1,
            status: 1
          }
        ]
      }
    ]
  }
];

describe('useMenu helpers', () => {
  it('counts nodes recursively', () => {
    expect(countNodes(sampleTree)).toBe(3);
    expect(countNodes([])).toBe(0);
  });

  it('counts menu type stats', () => {
    expect(countTypeStats(sampleTree)).toEqual({ dirs: 1, menus: 1, btns: 1 });
  });

  it('infers menu type from parent context', () => {
    expect(inferMenuType(0)).toBe(1);
    expect(inferMenuType(1, 2)).toBe(3);
    expect(inferMenuType(1, 1)).toBe(2);
  });

  it('builds parent tree options excluding buttons', () => {
    const options = buildParentTreeOptions(sampleTree, false, 0);
    expect(options[0].name).toContain('根目录');
    expect(options).toHaveLength(2);
    expect(options[1].children).toHaveLength(1);
  });

  it('filters component suggestions', () => {
    const cb = vi.fn();
    queryComponentSuggestions('CourseList', cb);
    expect(cb).toHaveBeenCalled();
    expect(cb.mock.calls[0][0].every((item: { value: string }) => item.value.includes('CourseList'))).toBe(true);
  });
});

describe('useMenu composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads menu tree on mount', async () => {
    const { getMenuTree } = await import('@/api/system/menu');
    vi.mocked(getMenuTree).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: sampleTree,
      timestamp: Date.now()
    });

    const { useMenu } = await import('./useMenu');
    const { tableData, totalMenuCount, moduleCount, fetchData } = useMenu();

    await fetchData();

    expect(tableData.value).toHaveLength(1);
    expect(totalMenuCount.value).toBe(3);
    expect(moduleCount.value).toBe(1);
  });
});
