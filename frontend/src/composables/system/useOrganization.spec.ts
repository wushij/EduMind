import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { OrganizationNodeVO, OrganizationMemberVO } from '@/types/system/tenant';
import {
  filterOrgTreeNode,
  filterOrgMembers,
  getOrgTypeLabel,
  getMemberRoleClass,
  buildOrgNodePath,
  countTreeNodes,
  findFirstClassNode,
  existsInTree
} from './useOrganization';

vi.mock('@/api/system/tenant', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/system/tenant')>();
  return {
    ...actual,
    getOrgTree: vi.fn(),
    deleteOrgNode: vi.fn(),
    getOrgMembers: vi.fn(),
    removeOrgMember: vi.fn(),
    getTenantOrgStats: vi.fn(),
    getOrgNodeStats: vi.fn()
  };
});

vi.mock('@/stores/system/tenant', () => ({
  useTenantStore: () => ({
    currentTenant: { id: 1, name: 'Demo School' },
    activeTenantName: 'Demo School',
    fetchCurrent: vi.fn()
  })
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  })
}));

const tree: OrganizationNodeVO[] = [
  {
    id: 1,
    tenantId: 1,
    parentId: 0,
    name: '主校区',
    orgType: 'CAMPUS',
    sortOrder: 1,
    children: [
      {
        id: 2,
        tenantId: 1,
        parentId: 1,
        name: '计算机学院',
        orgType: 'FACULTY',
        sortOrder: 1,
        children: [{ id: 3, tenantId: 1, parentId: 2, name: '软工1班', orgType: 'CLASS', sortOrder: 1, children: [] }]
      }
    ]
  }
];

describe('useOrganization helpers', () => {
  it('filters tree nodes and members', () => {
    expect(filterOrgTreeNode('软工', { id: 3, tenantId: 1, parentId: 2, name: '软工1班', orgType: 'CLASS', sortOrder: 1, children: [] })).toBe(true);
    const members: OrganizationMemberVO[] = [
      { id: 1, userId: 1, name: '张三', role: '学生', studentNo: 'S001' },
      { id: 2, userId: 2, name: '李老师', role: '任课教师', studentNo: 'T001' }
    ];
    expect(filterOrgMembers(members, '学生', '')).toHaveLength(1);
    expect(filterOrgMembers(members, 'ALL', '张')).toHaveLength(1);
  });

  it('maps org type and role classes', () => {
    expect(getOrgTypeLabel('CAMPUS')).toBe('校区');
    expect(getMemberRoleClass('班主任')).toBe('role-head-teacher');
  });

  it('builds node path and counts tree nodes', () => {
    const path = buildOrgNodePath(tree, tree[0].children![0].children![0], 'Demo School');
    expect(path).toContain('软工1班');
    const counts = countTreeNodes(tree);
    expect(counts.campusCount).toBe(1);
    expect(counts.classCount).toBe(1);
  });

  it('finds class node and checks existence', () => {
    expect(findFirstClassNode(tree)?.name).toBe('软工1班');
    expect(existsInTree(tree, 3)).toBe(true);
    expect(existsInTree(tree, 99)).toBe(false);
  });
});

describe('useOrganization API wrappers', () => {
  it('re-exports organization API functions', async () => {
    const api = await import('@/api/system/tenant');
    const composable = await import('./useOrganization');

    expect(composable.createOrgNode).toBe(api.createOrgNode);
    expect(composable.updateOrgNode).toBe(api.updateOrgNode);
    expect(composable.getOrgCandidates).toBe(api.getOrgCandidates);
    expect(composable.batchAssignOrgMembers).toBe(api.batchAssignOrgMembers);
    expect(composable.assignOrgMember).toBe(api.assignOrgMember);
    expect(composable.getStudentCognitiveProfile).toBe(api.getStudentCognitiveProfile);
  });
});

describe('useOrganization composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads organization tree', async () => {
    const { getOrgTree } = await import('@/api/system/tenant');
    vi.mocked(getOrgTree).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: tree,
      timestamp: Date.now()
    });

    const { useOrganization } = await import('./useOrganization');
    const { treeData, loadTree } = useOrganization();

    await loadTree();

    expect(treeData.value).toHaveLength(1);
  });
});
