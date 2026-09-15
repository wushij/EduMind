import { ref, computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useTenantStore } from '@/stores/system/tenant';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  OfficeBuilding,
  School,
  Folder,
  UserFilled
} from '@element-plus/icons-vue';
import {
  getOrgTree,
  deleteOrgNode,
  getOrgMembers,
  removeOrgMember,
  getTenantOrgStats,
  getOrgNodeStats
} from '@/api/system/tenant';

export {
  createOrgNode,
  updateOrgNode,
  getOrgCandidates,
  batchAssignOrgMembers,
  assignOrgMember,
  getStudentCognitiveProfile
} from '@/api/system/tenant';
import type {
  OrganizationNodeVO,
  OrganizationMemberVO,
  OrgStatsVO,
  OrgNodeStatsVO
} from '@/types/system/tenant';

export const ORG_ROLE_FILTER_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '班主任', value: '班主任' },
  { label: '教师', value: '任课教师' },
  { label: '班长', value: '班长' },
  { label: '学生', value: '学生' }
];

export function filterOrgTreeNode(value: string, data: OrganizationNodeVO) {
  if (!value) return true;
  return data.name.toLowerCase().includes(value.toLowerCase());
}

export function filterOrgMembers(
  members: OrganizationMemberVO[],
  roleFilter: string,
  keyword: string
): OrganizationMemberVO[] {
  let list = members;
  if (roleFilter !== 'ALL') {
    list = list.filter((m) => m.role === roleFilter);
  }
  if (!keyword.trim()) return list;
  const kw = keyword.trim().toLowerCase();
  return list.filter(
    (m) =>
      (m.name && m.name.toLowerCase().includes(kw)) ||
      (m.studentNo && m.studentNo.toLowerCase().includes(kw))
  );
}

export function getOrgTypeLabel(type: string) {
  switch (type) {
    case 'CAMPUS':
      return '校区';
    case 'FACULTY':
    case 'COLLEGE':
      return '学院/年级';
    case 'DEPT':
      return '系所/教研';
    case 'CLASS':
      return '行政班';
    default:
      return '教学实体';
  }
}

export function getMemberRoleClass(role?: string) {
  switch (role) {
    case '班主任':
      return 'role-head-teacher';
    case '任课教师':
      return 'role-teacher';
    case '班长':
      return 'role-monitor';
    default:
      return 'role-student';
  }
}

export function buildOrgNodePath(
  treeData: OrganizationNodeVO[],
  selectedNode: OrganizationNodeVO,
  tenantSchoolName: string
): string {
  const pathParts: string[] = [];

  const findAncestors = (nodes: OrganizationNodeVO[], targetId: number, currentPath: string[]): boolean => {
    for (const node of nodes) {
      const nextPath = [...currentPath, node.name];
      if (node.id === targetId) {
        pathParts.push(...nextPath);
        return true;
      }
      if (node.children && node.children.length > 0) {
        if (findAncestors(node.children, targetId, nextPath)) return true;
      }
    }
    return false;
  };

  findAncestors(treeData, selectedNode.id, []);
  if (pathParts.length > 0) {
    return `${tenantSchoolName} / ${pathParts.join(' / ')}`;
  }
  return `${tenantSchoolName} / ${selectedNode.name}`;
}

export function countTreeNodes(list: OrganizationNodeVO[]) {
  let campus = 0;
  let faculty = 0;
  let clazz = 0;
  const traverse = (items: OrganizationNodeVO[]) => {
    for (const item of items) {
      if (item.orgType === 'CAMPUS') campus++;
      else if (item.orgType === 'FACULTY' || item.orgType === 'COLLEGE') faculty++;
      else if (item.orgType === 'CLASS') clazz++;
      if (item.children && item.children.length > 0) traverse(item.children);
    }
  };
  traverse(list);
  return { campusCount: campus, facultyCount: faculty, classCount: clazz };
}

export function findFirstClassNode(nodes: OrganizationNodeVO[]): OrganizationNodeVO | null {
  for (const n of nodes) {
    if (n.orgType === 'CLASS') return n;
    if (n.children && n.children.length > 0) {
      const found = findFirstClassNode(n.children);
      if (found) return found;
    }
  }
  return null;
}

export function existsInTree(nodes: OrganizationNodeVO[], id: number): boolean {
  for (const n of nodes) {
    if (n.id === id) return true;
    if (n.children && existsInTree(n.children, id)) return true;
  }
  return false;
}

export function getNodeIcon(type: string) {
  switch (type) {
    case 'CAMPUS':
      return OfficeBuilding;
    case 'FACULTY':
    case 'COLLEGE':
      return School;
    case 'DEPT':
      return Folder;
    default:
      return UserFilled;
  }
}

export function useOrganization() {
  const route = useRoute();
  const tenantStore = useTenantStore();

  const loading = ref(false);
  const membersLoading = ref(false);
  const statsLoading = ref(false);
  const treeSearchKeyword = ref('');
  const treeRef = ref();
  const treeData = ref<OrganizationNodeVO[]>([]);
  const selectedNode = ref<OrganizationNodeVO | null>(null);
  const memberKeyword = ref('');
  const selectedRoleFilter = ref('ALL');

  const tenantStats = ref<OrgStatsVO>({
    campusCount: 1,
    facultyCount: 2,
    classCount: 3,
    studentCount: 3,
    teacherCount: 1
  });

  const nodeStats = ref<OrgNodeStatsVO>({
    orgId: 0,
    orgName: '',
    orgType: '',
    studentCount: 3,
    teacherCount: 1,
    avgMasteryRate: 78.5,
    homeworkSubmissionRate: 96.5,
    pendingInterventions: 1
  });

  const nodeDialogVisible = ref(false);
  const nodeFormMode = ref<'create' | 'edit'>('create');
  const activeParentNode = ref<OrganizationNodeVO | null>(null);
  const activeEditNode = ref<OrganizationNodeVO | null>(null);

  const memberDialogVisible = ref(false);
  const profileDrawerVisible = ref(false);
  const activeProfileStudent = ref<OrganizationMemberVO | null>(null);

  const members = ref<OrganizationMemberVO[]>([]);

  const filteredMembers = computed(() =>
    filterOrgMembers(members.value, selectedRoleFilter.value, memberKeyword.value)
  );

  const fullNodePath = computed(() => {
    if (!selectedNode.value) return '';
    const tenantSchoolName =
      tenantStore.currentTenant?.name || tenantStore.activeTenantName || '当前学校组织';
    return buildOrgNodePath(treeData.value, selectedNode.value, tenantSchoolName);
  });

  watch(treeSearchKeyword, (val) => {
    treeRef.value?.filter(val);
  });

  const filterTreeNode = filterOrgTreeNode;
  const getTypeLabel = getOrgTypeLabel;
  const getRoleClass = getMemberRoleClass;

  const loadStats = async (tenantId?: number) => {
    try {
      const res = await getTenantOrgStats(tenantId);
      if (res?.data) {
        tenantStats.value = res.data;
      }
    } catch {
      const counts = countTreeNodes(treeData.value);
      tenantStats.value = { ...tenantStats.value, ...counts };
    }
  };

  const loadNodeStats = async (orgId: number) => {
    if (!orgId) return;
    try {
      statsLoading.value = true;
      const res = await getOrgNodeStats(orgId);
      if (res?.data) {
        nodeStats.value = res.data;
      }
    } catch {
      nodeStats.value = {
        orgId,
        orgName: selectedNode.value?.name || '',
        orgType: selectedNode.value?.orgType || '',
        studentCount: members.value.filter((m) => m.role === '学生' || m.role === '班长').length || 0,
        teacherCount:
          members.value.filter((m) => m.role === '任课教师' || m.role === '班主任').length || 0,
        avgMasteryRate: 0,
        homeworkSubmissionRate: 0,
        pendingInterventions: 0
      };
    } finally {
      statsLoading.value = false;
    }
  };

  const loadTree = async () => {
    try {
      loading.value = true;
      const targetTenantId = route.query.tenantId
        ? Number(route.query.tenantId)
        : tenantStore.currentTenant?.id || undefined;
      const res = await getOrgTree(targetTenantId);
      if (res?.data && res.data.length > 0) {
        treeData.value = res.data;

        if (!selectedNode.value || !existsInTree(res.data, selectedNode.value.id)) {
          selectedNode.value = findFirstClassNode(res.data) || res.data[0];
        }
      } else {
        treeData.value = [];
        selectedNode.value = null;
        members.value = [];
      }

      loadStats(targetTenantId);
      if (selectedNode.value?.id) {
        loadMembers(selectedNode.value.id);
        loadNodeStats(selectedNode.value.id);
      } else {
        members.value = [];
      }
    } catch {
      treeData.value = [];
      selectedNode.value = null;
      members.value = [];
      loadStats();
    } finally {
      loading.value = false;
    }
  };

  const loadMembers = async (orgId: number) => {
    try {
      membersLoading.value = true;
      const res = await getOrgMembers(orgId);
      members.value = res?.data ?? [];
    } catch {
      members.value = [];
    } finally {
      membersLoading.value = false;
    }
  };

  const handleNodeClick = (node: OrganizationNodeVO) => {
    selectedNode.value = node;
    if (node?.id) {
      loadMembers(node.id);
      loadNodeStats(node.id);
    }
  };

  const openAddRootDialog = () => {
    nodeFormMode.value = 'create';
    activeParentNode.value = null;
    activeEditNode.value = null;
    nodeDialogVisible.value = true;
  };

  const handleTreeCommand = (command: string, data: OrganizationNodeVO) => {
    if (command === 'addChild') {
      nodeFormMode.value = 'create';
      activeParentNode.value = data;
      activeEditNode.value = null;
      nodeDialogVisible.value = true;
    } else if (command === 'edit') {
      nodeFormMode.value = 'edit';
      activeParentNode.value = null;
      activeEditNode.value = data;
      nodeDialogVisible.value = true;
    } else if (command === 'delete') {
      ElMessageBox.confirm(`确认删除【${data.name}】及所有级联子实体吗？此操作不可逆。`, '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteOrgNode(data.id);
          ElMessage.success('节点已删除');
          if (selectedNode.value?.id === data.id) {
            selectedNode.value = null;
          }
          loadTree();
        } catch (e: unknown) {
          const message = e instanceof Error ? e.message : '删除节点失败';
          ElMessage.error(message);
        }
      });
    }
  };

  const openAddMemberDialog = () => {
    if (!selectedNode.value?.id) {
      ElMessage.warning('请先在左侧选择要分配成员的组织或班级节点');
      return;
    }
    memberDialogVisible.value = true;
  };

  const handleMemberAssignSuccess = () => {
    if (selectedNode.value?.id) {
      loadMembers(selectedNode.value.id);
      loadNodeStats(selectedNode.value.id);
    }
    loadStats();
  };

  const viewStudentProfile = (student: OrganizationMemberVO) => {
    activeProfileStudent.value = student;
    profileDrawerVisible.value = true;
  };

  const removeMember = (student: OrganizationMemberVO) => {
    if (!selectedNode.value?.id) return;
    ElMessageBox.confirm(`确认将【${student.name}】从当前组织中移出吗？`, '提示', {
      confirmButtonText: '确认移出',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await removeOrgMember(selectedNode.value!.id, student.id);
        ElMessage.success('已移出组织');
        loadMembers(selectedNode.value!.id);
        loadNodeStats(selectedNode.value!.id);
        loadStats();
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '移出失败';
        ElMessage.error(message);
      }
    });
  };

  const exportRoster = () => {
    if (!filteredMembers.value || filteredMembers.value.length === 0) {
      ElMessage.warning('当前班级花名册为空，无法导出');
      return;
    }

    const className = selectedNode.value?.name || '班级';
    const fullPath = fullNodePath.value;
    const nowStr = new Date().toLocaleString();

    let csv =
      '\uFEFF学号/工号,姓名,身份角色,AI知识掌握度,最近学情活跃,所属教学班级,层级完整路径,导出时间\n';
    for (const m of filteredMembers.value) {
      const mastery = m.masteryRate != null ? `${m.masteryRate}%` : '新入库·未测评';
      const active = m.lastActive || '暂无记录';
      csv += `"${m.studentNo}","${m.name}","${m.role}","${mastery}","${active}","${className}","${fullPath}","${nowStr}"\n`;
    }

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute(
      'download',
      `${className}_师生花名册_${new Date().toISOString().substring(0, 10)}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    ElMessage.success(
      `已生成并下载【${className}】花名册报表（共 ${filteredMembers.value.length} 名师生）！`
    );
  };

  watch([() => route.query.tenantId, () => tenantStore.currentTenant?.id], () => {
    selectedNode.value = null;
    loadTree();
  });

  onMounted(async () => {
    if (!tenantStore.currentTenant) {
      await tenantStore.fetchCurrent();
    }
    loadTree();
  });

  return {
    tenantStore,
    loading,
    membersLoading,
    statsLoading,
    treeSearchKeyword,
    treeRef,
    treeData,
    selectedNode,
    memberKeyword,
    selectedRoleFilter,
    roleFilterOptions: ORG_ROLE_FILTER_OPTIONS,
    tenantStats,
    nodeStats,
    nodeDialogVisible,
    nodeFormMode,
    activeParentNode,
    activeEditNode,
    memberDialogVisible,
    profileDrawerVisible,
    activeProfileStudent,
    members,
    filteredMembers,
    fullNodePath,
    filterTreeNode,
    getTypeLabel,
    getNodeIcon,
    getRoleClass,
    loadTree,
    handleNodeClick,
    openAddRootDialog,
    handleTreeCommand,
    openAddMemberDialog,
    handleMemberAssignSuccess,
    viewStudentProfile,
    removeMember,
    exportRoster
  };
}
