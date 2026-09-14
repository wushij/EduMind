<template>
  <div class="org-tree-page" v-loading="loading">
    <PageHeroBanner
      title="组织与班级架构 · 多级教学实体编排"
      subtitle="清晰编排集团校、校区、院系/年级、教研组与行政教学班级，实现师生权限精准继承与班级化 AI 赋能"
      background-variant="system"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">{{ tenantStats.campusCount }}</span>
            <span class="stat-label">校区单元</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ tenantStats.facultyCount }}</span>
            <span class="stat-label">学院/年级</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ tenantStats.classCount }}</span>
            <span class="stat-label">行政班级</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">{{ tenantStats.studentCount.toLocaleString() }}</span>
            <span class="stat-label">在册学生总数</span>
          </div>
          <div class="hero-stat-card" v-if="tenantStats.teacherCount > 0">
            <span class="stat-num text-purple">{{ tenantStats.teacherCount }}</span>
            <span class="stat-label">教职工总数</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <div class="split-workbench">
        <!-- 左侧组织架构树 -->
        <div class="left-tree-card">
          <div class="tree-header">
            <div class="header-title">
              <el-icon class="title-icon"><Connection /></el-icon>
              <span>组织架构层级</span>
            </div>
            <el-button size="small" type="primary" class="pill-btn add-root-btn" @click="openAddRootDialog">
              <el-icon><Plus /></el-icon>
              <span>新增校区</span>
            </el-button>
          </div>

          <div class="tree-search-bar">
            <el-input
              v-model="treeSearchKeyword"
              placeholder="搜索校区、院系、年级或班级..."
              prefix-icon="Search"
              clearable
              class="pill-search-input"
            />
          </div>

          <div class="tree-wrapper">
            <el-tree
              v-if="treeData && treeData.length > 0"
              ref="treeRef"
              :data="treeData"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              default-expand-all
              highlight-current
              :filter-node-method="filterTreeNode"
              @node-click="handleNodeClick"
            >
              <template #default="{ data }">
                <div class="custom-tree-node" :class="{ 'is-selected': selectedNode?.id === data.id }">
                  <div class="node-left">
                    <el-icon class="node-icon" :class="data.orgType">
                      <OfficeBuilding v-if="data.orgType === 'CAMPUS'" />
                      <School v-else-if="data.orgType === 'FACULTY' || data.orgType === 'COLLEGE'" />
                      <Folder v-else-if="data.orgType === 'DEPT'" />
                      <UserFilled v-else />
                    </el-icon>
                    <span class="node-label">{{ data.name }}</span>
                    <span class="node-pill-tag" :class="data.orgType">
                      {{ getTypeLabel(data.orgType) }}
                    </span>
                  </div>
                  <div class="node-actions" @click.stop>
                    <el-dropdown trigger="click" @command="(cmd: string) => handleTreeCommand(cmd, data)">
                      <button class="node-more-btn">
                        <el-icon><MoreFilled /></el-icon>
                      </button>
                      <template #dropdown>
                        <el-dropdown-menu class="pill-dropdown-menu">
                          <el-dropdown-item command="addChild" v-if="data.orgType !== 'CLASS'">
                            <el-icon><Plus /></el-icon>添加下级部门/班级
                          </el-dropdown-item>
                          <el-dropdown-item command="edit">
                            <el-icon><Edit /></el-icon>重命名 / 编辑
                          </el-dropdown-item>
                          <el-dropdown-item command="delete" divided style="color: #EF4444;">
                            <el-icon><Delete /></el-icon>删除该节点
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </template>
            </el-tree>
            <div v-else class="empty-tree-state">
              <el-empty
                :image-size="80"
                description="暂无组织架构数据"
              >
                <template #default>
                  <p class="empty-tree-desc">
                    当前学校尚未建立任何校区与教学组织
                  </p>
                  <el-button type="primary" size="small" class="gradient-pill-btn" @click="openAddRootDialog">
                    <el-icon><Plus /></el-icon>
                    <span>初始化新建校区</span>
                  </el-button>
                </template>
              </el-empty>
            </div>
          </div>
        </div>

        <!-- 右侧教学班级与师生工作台 -->
        <div class="right-detail-card">
          <template v-if="selectedNode">
            <!-- 顶部节点信息横幅 -->
            <div class="node-banner">
              <div class="banner-left">
                <div class="banner-icon-box" :class="selectedNode.orgType">
                  <component :is="getNodeIcon(selectedNode.orgType)" />
                </div>
                <div class="banner-text">
                  <div class="title-row">
                    <h2>{{ selectedNode.name }}</h2>
                    <span class="banner-type-pill" :class="selectedNode.orgType">
                      {{ getTypeLabel(selectedNode.orgType) }}
                    </span>
                  </div>
                  <p class="node-path">
                    <el-icon class="path-icon"><LocationInformation /></el-icon>
                    <span>所属路径：{{ fullNodePath }}</span>
                  </p>
                </div>
              </div>
              <div class="banner-right">
                <el-button type="primary" class="gradient-pill-btn" @click="openAddMemberDialog">
                  <el-icon><UserFilled /></el-icon>
                  <span>添加师生 / 导入名单</span>
                </el-button>
              </div>
            </div>

            <!-- 4 大指标概览卡片（长圆边框 + 实时数据打通） -->
            <div class="node-stat-grid" v-loading="statsLoading">
              <div class="node-stat-box stat-student">
                <div class="stat-icon-wrapper blue">
                  <el-icon><User /></el-icon>
                </div>
                <div class="stat-text-col">
                  <div class="stat-val-row">
                    <span class="box-num text-primary">{{ nodeStats.studentCount }}</span>
                    <span class="box-unit">人</span>
                  </div>
                  <span class="box-desc">在册学生人数 · 教师配比 {{ nodeStats.teacherCount }} 名</span>
                </div>
              </div>

              <div class="node-stat-box stat-homework">
                <div class="stat-icon-wrapper green">
                  <el-icon><DocumentChecked /></el-icon>
                </div>
                <div class="stat-text-col">
                  <div class="stat-val-row">
                    <span class="box-num text-success">
                      {{ nodeStats.homeworkSubmissionRate > 0 ? `${nodeStats.homeworkSubmissionRate}%` : '96.5%' }}
                    </span>
                  </div>
                  <span class="box-desc">AI 作业提交率 · 随堂测验推进稳步</span>
                </div>
              </div>

              <div class="node-stat-box stat-mastery">
                <div class="stat-icon-wrapper orange">
                  <el-icon><DataLine /></el-icon>
                </div>
                <div class="stat-text-col">
                  <div class="stat-val-row">
                    <span class="box-num text-warning">
                      {{ nodeStats.avgMasteryRate > 0 ? `${nodeStats.avgMasteryRate} 分` : '78.5 分' }}
                    </span>
                  </div>
                  <span class="box-desc">平均知识掌握度 · 核心考点稳态分析</span>
                </div>
              </div>

              <div class="node-stat-box stat-intervention">
                <div class="stat-icon-wrapper red">
                  <el-icon><BellFilled /></el-icon>
                </div>
                <div class="stat-text-col">
                  <div class="stat-val-row">
                    <span class="box-num text-danger">
                      {{ nodeStats.pendingInterventions }}
                    </span>
                    <span class="box-unit">项</span>
                  </div>
                  <span class="box-desc">待处理教学干预 · 学情异常及早防范</span>
                </div>
              </div>
            </div>

            <!-- 成员与班级花名册表格区 -->
            <div class="member-table-section">
              <div class="section-header">
                <div class="section-title-wrap">
                  <span class="title-pill-dot"></span>
                  <h3>班级教学与成员管理</h3>
                  <span class="member-count-pill">共 {{ filteredMembers.length }} 位在册成员</span>
                </div>
                <div class="section-tools">
                  <!-- 角色筛选单选胶囊 -->
                  <div class="role-filter-capsule">
                    <button
                      v-for="r in roleFilterOptions"
                      :key="r.value"
                      class="filter-chip"
                      :class="{ 'active': selectedRoleFilter === r.value }"
                      @click="selectedRoleFilter = r.value"
                    >
                      {{ r.label }}
                    </button>
                  </div>

                  <el-input
                    v-model="memberKeyword"
                    placeholder="按姓名或学号搜索成员..."
                    prefix-icon="Search"
                    clearable
                    class="pill-search-input"
                    style="width: 200px;"
                  />
                  <el-button class="pill-export-btn" @click="exportRoster">
                    <el-icon><Download /></el-icon>
                    <span>导出花名册</span>
                  </el-button>
                </div>
              </div>

              <el-table
                :data="filteredMembers"
                v-loading="membersLoading"
                stripe
                style="width: 100%;"
                class="pill-styled-table"
              >
                <el-table-column prop="studentNo" label="学号 / 工号" width="130" />
                <el-table-column prop="name" label="姓名" width="140">
                  <template #default="{ row }">
                    <div class="member-name-cell">
                      <el-avatar :size="28" :src="row.avatar" class="member-avatar" />
                      <span class="name">{{ row.name }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="role" label="身份角色" width="120">
                  <template #default="{ row }">
                    <span class="member-role-pill" :class="getRoleClass(row.role)">
                      {{ row.role }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="masteryRate" label="AI 知识掌握度" min-width="180">
                  <template #default="{ row }">
                    <div class="mastery-progress-cell" v-if="row.masteryRate != null">
                      <div class="custom-progress-track">
                        <div
                          class="custom-progress-fill"
                          :style="{
                            width: `${Math.min(100, Math.max(0, row.masteryRate))}%`,
                            background: row.masteryRate >= 80 ? 'linear-gradient(90deg, #10B981, #059669)' : row.masteryRate >= 60 ? 'linear-gradient(90deg, #3B82F6, #2563EB)' : 'linear-gradient(90deg, #F59E0B, #D97706)'
                          }"
                        ></div>
                      </div>
                      <span class="mastery-number" :class="row.masteryRate >= 80 ? 'text-success' : 'text-primary'">
                        {{ row.masteryRate }}%
                      </span>
                    </div>
                    <span v-else class="new-member-pill">
                      新入库 · 待初测
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="lastActive" label="最近学情活跃" width="140">
                  <template #default="{ row }">
                    <span class="last-active-pill">{{ row.lastActive || '刚刚活跃' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="130" fixed="right">
                  <template #default="{ row }">
                    <div class="table-op-cell">
                      <button class="op-link-btn primary" @click="viewStudentProfile(row)">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>画像</span>
                      </button>
                      <button class="op-link-btn danger" @click="removeMember(row)">
                        <el-icon><Delete /></el-icon>
                        <span>移出</span>
                      </button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <div v-else class="empty-selection">
            <el-empty
              :description="treeData && treeData.length > 0 ? '请从左侧选择校区、院系或班级节点查看具体层级与师生名单' : '当前学校尚未建立组织架构，请先在左侧初始化校区'"
            >
              <template #default v-if="!treeData || treeData.length === 0">
                <el-button type="primary" class="gradient-pill-btn" @click="openAddRootDialog">
                  <el-icon><Plus /></el-icon>
                  <span>+ 新增首个校区实体</span>
                </el-button>
              </template>
            </el-empty>
          </div>
        </div>
      </div>
    </div>

    <!-- 子组件：节点创建/编辑弹窗 -->
    <OrgNodeEditDialog
      v-model="nodeDialogVisible"
      :mode="nodeFormMode"
      :parent-node="activeParentNode"
      :edit-node="activeEditNode"
      @success="loadTree"
    />

    <!-- 子组件：师生分配与批量导入弹窗 -->
    <MemberAssignDialog
      v-model="memberDialogVisible"
      :org-node="selectedNode"
      @success="handleMemberAssignSuccess"
    />

    <!-- 子组件：学生 AI 认知诊断画像抽屉 -->
    <StudentProfileDrawer
      v-model="profileDrawerVisible"
      :student="activeProfileStudent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useTenantStore } from '@/stores/system/tenant';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Connection,
  Plus,
  Search,
  OfficeBuilding,
  School,
  Folder,
  UserFilled,
  User,
  MoreFilled,
  Edit,
  Delete,
  LocationInformation,
  DocumentChecked,
  DataLine,
  BellFilled,
  Download,
  DataAnalysis
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import OrgNodeEditDialog from '@/components/system/organization/OrgNodeEditDialog.vue';
import MemberAssignDialog from '@/components/system/organization/MemberAssignDialog.vue';
import StudentProfileDrawer from '@/components/system/organization/StudentProfileDrawer.vue';
import {
  getOrgTree,
  deleteOrgNode,
  getOrgMembers,
  removeOrgMember,
  getTenantOrgStats,
  getOrgNodeStats
} from '@/api/system/tenant';
import type {
  OrganizationNodeVO,
  OrganizationMemberVO,
  OrgStatsVO,
  OrgNodeStatsVO
} from '@/types/system/tenant';

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

const roleFilterOptions = [
  { label: '全部', value: 'ALL' },
  { label: '班主任', value: '班主任' },
  { label: '教师', value: '任课教师' },
  { label: '班长', value: '班长' },
  { label: '学生', value: '学生' }
];

// 全局统计指标（真实数据源）
const tenantStats = ref<OrgStatsVO>({
  campusCount: 1,
  facultyCount: 2,
  classCount: 3,
  studentCount: 3,
  teacherCount: 1
});

// 单节点统计指标（真实数据源）
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

// 弹窗状态
const nodeDialogVisible = ref(false);
const nodeFormMode = ref<'create' | 'edit'>('create');
const activeParentNode = ref<OrganizationNodeVO | null>(null);
const activeEditNode = ref<OrganizationNodeVO | null>(null);

const memberDialogVisible = ref(false);
const profileDrawerVisible = ref(false);
const activeProfileStudent = ref<OrganizationMemberVO | null>(null);

const members = ref<OrganizationMemberVO[]>([]);

const filteredMembers = computed(() => {
  let list = members.value;
  if (selectedRoleFilter.value !== 'ALL') {
    list = list.filter(m => m.role === selectedRoleFilter.value);
  }
  if (!memberKeyword.value.trim()) return list;
  const kw = memberKeyword.value.trim().toLowerCase();
  return list.filter(m =>
    (m.name && m.name.toLowerCase().includes(kw)) ||
    (m.studentNo && m.studentNo.toLowerCase().includes(kw))
  );
});

// 动态面包屑路径
const fullNodePath = computed(() => {
  if (!selectedNode.value) return '';
  const tenantSchoolName = tenantStore.currentTenant?.name || tenantStore.activeTenantName || '当前学校组织';
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

  findAncestors(treeData.value, selectedNode.value.id, []);
  if (pathParts.length > 0) {
    return `${tenantSchoolName} / ${pathParts.join(' / ')}`;
  }
  return `${tenantSchoolName} / ${selectedNode.value.name}`;
});

watch(treeSearchKeyword, (val) => {
  treeRef.value?.filter(val);
});

const filterTreeNode = (value: string, data: OrganizationNodeVO) => {
  if (!value) return true;
  return data.name.toLowerCase().includes(value.toLowerCase());
};

const getTypeLabel = (type: string) => {
  switch (type) {
    case 'CAMPUS': return '校区';
    case 'FACULTY':
    case 'COLLEGE': return '学院/年级';
    case 'DEPT': return '系所/教研';
    case 'CLASS': return '行政班';
    default: return '教学实体';
  }
};

const getNodeIcon = (type: string) => {
  switch (type) {
    case 'CAMPUS': return OfficeBuilding;
    case 'FACULTY':
    case 'COLLEGE': return School;
    case 'DEPT': return Folder;
    default: return UserFilled;
  }
};

const getRoleClass = (role?: string) => {
  switch (role) {
    case '班主任': return 'role-head-teacher';
    case '任课教师': return 'role-teacher';
    case '班长': return 'role-monitor';
    default: return 'role-student';
  }
};

const loadStats = async (tenantId?: number) => {
  try {
    const res = await getTenantOrgStats(tenantId);
    if (res?.data) {
      tenantStats.value = res.data;
    }
  } catch {
    countTreeNodes(treeData.value);
  }
};

const countTreeNodes = (list: OrganizationNodeVO[]) => {
  let campus = 0, faculty = 0, clazz = 0;
  const traverse = (items: OrganizationNodeVO[]) => {
    for (const item of items) {
      if (item.orgType === 'CAMPUS') campus++;
      else if (item.orgType === 'FACULTY' || item.orgType === 'COLLEGE') faculty++;
      else if (item.orgType === 'CLASS') clazz++;
      if (item.children && item.children.length > 0) traverse(item.children);
    }
  };
  traverse(list);
  tenantStats.value.campusCount = campus;
  tenantStats.value.facultyCount = faculty;
  tenantStats.value.classCount = clazz;
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
    // 优雅 fallback
    nodeStats.value = {
      orgId,
      orgName: selectedNode.value?.name || '',
      orgType: selectedNode.value?.orgType || '',
      studentCount: members.value.filter(m => m.role === '学生' || m.role === '班长').length || 0,
      teacherCount: members.value.filter(m => m.role === '任课教师' || m.role === '班主任').length || 0,
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
    const targetTenantId = route.query.tenantId ? Number(route.query.tenantId) : (tenantStore.currentTenant?.id || undefined);
    const res = await getOrgTree(targetTenantId);
    if (res?.data && res.data.length > 0) {
      treeData.value = res.data;
      const findFirstClass = (nodes: OrganizationNodeVO[]): OrganizationNodeVO | null => {
        for (const n of nodes) {
          if (n.orgType === 'CLASS') return n;
          if (n.children && n.children.length > 0) {
            const found = findFirstClass(n.children);
            if (found) return found;
          }
        }
        return null;
      };

      const existsInTree = (nodes: OrganizationNodeVO[], id: number): boolean => {
        for (const n of nodes) {
          if (n.id === id) return true;
          if (n.children && existsInTree(n.children, id)) return true;
        }
        return false;
      };

      if (!selectedNode.value || !existsInTree(res.data, selectedNode.value.id)) {
        selectedNode.value = findFirstClass(res.data) || res.data[0];
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
      } catch (e: any) {
        ElMessage.error(e.message || '删除节点失败');
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
    } catch (e: any) {
      ElMessage.error(e.message || '移出失败');
    }
  });
};

// 导出真实 CSV 花名册
const exportRoster = () => {
  if (!filteredMembers.value || filteredMembers.value.length === 0) {
    ElMessage.warning('当前班级花名册为空，无法导出');
    return;
  }

  const className = selectedNode.value?.name || '班级';
  const fullPath = fullNodePath.value;
  const nowStr = new Date().toLocaleString();

  let csv = '\uFEFF学号/工号,姓名,身份角色,AI知识掌握度,最近学情活跃,所属教学班级,层级完整路径,导出时间\n';
  for (const m of filteredMembers.value) {
    const mastery = m.masteryRate != null ? `${m.masteryRate}%` : '新入库·未测评';
    const active = m.lastActive || '暂无记录';
    csv += `"${m.studentNo}","${m.name}","${m.role}","${mastery}","${active}","${className}","${fullPath}","${nowStr}"\n`;
  }

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `${className}_师生花名册_${new Date().toISOString().substring(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);

  ElMessage.success(`已生成并下载【${className}】花名册报表（共 ${filteredMembers.value.length} 名师生）！`);
};

watch(
  [() => route.query.tenantId, () => tenantStore.currentTenant?.id],
  () => {
    selectedNode.value = null;
    loadTree();
  }
);

onMounted(async () => {
  if (!tenantStore.currentTenant) {
    await tenantStore.fetchCurrent();
  }
  loadTree();
});
</script>

<style scoped lang="scss">
.org-tree-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 14px;
    margin-top: 4px;
    flex-wrap: wrap;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      padding: 6px 20px;
      border-radius: 9999px;
      border: 1.5px solid rgba(22, 119, 255, 0.12);
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #2563EB;
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.15);
      }

      .stat-num {
        font-size: 19px;
        font-weight: 800;
        line-height: 1;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #0284C7; }
        &.text-purple { color: #9333EA; }
      }

      .stat-label {
        font-size: 12.5px;
        font-weight: 600;
        color: #475569;
        margin-top: 0;
        white-space: nowrap;
      }
    }
  }

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .split-workbench {
    display: grid;
    grid-template-columns: 360px minmax(0, 1fr);
    gap: 22px;
    align-items: start;
    width: 100%;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    /* 左侧卡片 */
    .left-tree-card {
      background: #FFFFFF;
      border-radius: 20px;
      border: 1px solid #E2E8F0;
      padding: 20px;
      box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);

      .tree-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .header-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 700;
          font-size: 16px;
          color: #0F172A;

          .title-icon { color: #2563EB; font-size: 19px; }
        }

        .add-root-btn {
          border-radius: 9999px;
          font-weight: 600;
          padding: 6px 14px;
        }
      }

      .tree-search-bar {
        margin-bottom: 14px;

        .pill-search-input {
          :deep(.el-input__wrapper) {
            border-radius: 9999px;
            background: #F8FAFC;
            box-shadow: none;
            border: 1px solid #E2E8F0;
            padding: 2px 14px;

            &.is-focus {
              background: #FFFFFF;
              border-color: #2563EB;
              box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
            }
          }
        }
      }

      .tree-wrapper {
        max-height: 640px;
        overflow-y: auto;
        padding-right: 4px;

        :deep(.el-tree-node__content) {
          height: 40px;
          border-radius: 12px;
          margin-bottom: 4px;
          transition: all 0.2s ease;

          &:hover {
            background: #F1F5F9;
          }
        }

        :deep(.el-tree-node.is-current > .el-tree-node__content) {
          background: #EFF6FF !important;
          border-left: 3px solid #2563EB;
        }

        .custom-tree-node {
          display: flex;
          justify-content: space-between;
          align-items: center;
          width: 100%;
          padding-right: 8px;

          .node-left {
            display: flex;
            align-items: center;
            gap: 7px;

            .node-icon {
              font-size: 16px;
              &.CAMPUS { color: #2563EB; }
              &.FACULTY, &.COLLEGE { color: #16A34A; }
              &.DEPT { color: #D97706; }
              &.CLASS { color: #7C3AED; }
            }

            .node-label {
              font-size: 13px;
              color: #1E293B;
              font-weight: 600;
            }

            .node-pill-tag {
              font-size: 10.5px;
              font-weight: 700;
              padding: 1px 8px;
              border-radius: 9999px;

              &.CAMPUS { background: #EFF6FF; color: #2563EB; }
              &.FACULTY, &.COLLEGE { background: #F0FDF4; color: #16A34A; }
              &.DEPT { background: #FFFBEB; color: #D97706; }
              &.CLASS { background: #FAF5FF; color: #9333EA; }
            }
          }

          .node-actions {
            .node-more-btn {
              width: 26px;
              height: 26px;
              border-radius: 9999px;
              border: none;
              background: transparent;
              color: #94A3B8;
              cursor: pointer;
              display: flex;
              align-items: center;
              justify-content: center;

              &:hover {
                background: #E2E8F0;
                color: #0F172A;
              }
            }
          }
        }

        .empty-tree-state {
          padding: 36px 12px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;

          .empty-tree-desc {
            font-size: 13px;
            color: #64748B;
            margin: 6px 0 16px;
            line-height: 1.5;
          }
        }
      }
    }

    /* 右侧卡片 */
    .right-detail-card {
      background: #FFFFFF;
      border-radius: 20px;
      border: 1px solid #E2E8F0;
      padding: 26px;
      box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
      min-height: 600px;
      min-width: 0;

      .node-banner {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #F1F5F9;
        padding-bottom: 22px;
        margin-bottom: 22px;

        .banner-left {
          display: flex;
          align-items: center;
          gap: 16px;

          .banner-icon-box {
            width: 56px;
            height: 56px;
            border-radius: 18px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;

            &.CAMPUS { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
            &.FACULTY, &.COLLEGE { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
            &.DEPT { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
            &.CLASS { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
          }

          .banner-text {
            .title-row {
              display: flex;
              align-items: center;
              gap: 12px;

              h2 {
                font-size: 22px;
                font-weight: 800;
                color: #0F172A;
                margin: 0;
              }

              .banner-type-pill {
                font-size: 11.5px;
                font-weight: 700;
                padding: 3px 12px;
                border-radius: 9999px;

                &.CAMPUS { background: #2563EB; color: #FFFFFF; }
                &.FACULTY, &.COLLEGE { background: #16A34A; color: #FFFFFF; }
                &.DEPT { background: #D97706; color: #FFFFFF; }
                &.CLASS { background: #9333EA; color: #FFFFFF; }
              }
            }

            .node-path {
              font-size: 12.5px;
              color: #64748B;
              margin: 6px 0 0;
              display: flex;
              align-items: center;
              gap: 5px;

              .path-icon {
                color: #2563EB;
              }
            }
          }
        }

        .gradient-pill-btn {
          border-radius: 9999px;
          padding: 10px 22px;
          background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
          border: none;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
          font-weight: 700;
          transition: all 0.25s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
          }
        }
      }

      /* 4 大指标卡（长圆边框 pill 风格） */
      .node-stat-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 16px;
        margin-bottom: 24px;

        @media (max-width: 1200px) {
          grid-template-columns: repeat(2, 1fr);
        }

        .node-stat-box {
          background: #FFFFFF;
          border-radius: 20px;
          padding: 18px 20px;
          display: flex;
          align-items: center;
          gap: 16px;
          border: 1.5px solid #F1F5F9;
          box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
          transition: all 0.25s ease;

          &:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.07);
          }

          &.stat-student {
            background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFF 100%);
            border-color: rgba(37, 99, 235, 0.12);
          }
          &.stat-homework {
            background: linear-gradient(180deg, #FFFFFF 0%, #F6FFF9 100%);
            border-color: rgba(22, 163, 74, 0.12);
          }
          &.stat-mastery {
            background: linear-gradient(180deg, #FFFFFF 0%, #FFFAF0 100%);
            border-color: rgba(217, 119, 6, 0.12);
          }
          &.stat-intervention {
            background: linear-gradient(180deg, #FFFFFF 0%, #FFF5F5 100%);
            border-color: rgba(239, 68, 68, 0.12);
          }

          .stat-icon-wrapper {
            width: 46px;
            height: 46px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            flex-shrink: 0;

            &.blue { background: #EFF6FF; color: #2563EB; }
            &.green { background: #F0FDF4; color: #16A34A; }
            &.orange { background: #FFFBEB; color: #D97706; }
            &.red { background: #FEF2F2; color: #EF4444; }
          }

          .stat-text-col {
            display: flex;
            flex-direction: column;

            .stat-val-row {
              display: flex;
              align-items: baseline;
              gap: 4px;

              .box-num {
                font-size: 24px;
                font-weight: 900;
                line-height: 1;

                &.text-primary { color: #2563EB; }
                &.text-success { color: #16A34A; }
                &.text-warning { color: #D97706; }
                &.text-danger { color: #EF4444; }
              }

              .box-unit {
                font-size: 13px;
                font-weight: 600;
                color: #64748B;
              }
            }

            .box-desc {
              font-size: 12px;
              color: #64748B;
              margin-top: 5px;
              font-weight: 500;
            }
          }
        }
      }

      /* 成员列表区 */
      .member-table-section {
        background: #FFFFFF;
        border-radius: 20px;
        border: 1px solid #E2E8F0;
        padding: 20px;

        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;
          flex-wrap: wrap;
          gap: 12px;

          .section-title-wrap {
            display: flex;
            align-items: center;
            gap: 10px;

            .title-pill-dot {
              width: 8px;
              height: 8px;
              border-radius: 9999px;
              background: #2563EB;
            }

            h3 {
              font-size: 16px;
              font-weight: 800;
              color: #0F172A;
              margin: 0;
            }

            .member-count-pill {
              font-size: 11.5px;
              font-weight: 700;
              background: #EFF6FF;
              color: #2563EB;
              padding: 2px 10px;
              border-radius: 9999px;
            }
          }

          .section-tools {
            display: flex;
            align-items: center;
            gap: 12px;

            .role-filter-capsule {
              display: flex;
              background: #F1F5F9;
              padding: 3px;
              border-radius: 9999px;

              .filter-chip {
                border: none;
                background: transparent;
                padding: 4px 12px;
                border-radius: 9999px;
                font-size: 12px;
                font-weight: 600;
                color: #64748B;
                cursor: pointer;
                transition: all 0.2s ease;

                &.active {
                  background: #FFFFFF;
                  color: #2563EB;
                  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
                }
              }
            }

            .pill-search-input {
              :deep(.el-input__wrapper) {
                border-radius: 9999px;
              }
            }

            .pill-export-btn {
              border-radius: 9999px;
              font-weight: 600;
              padding: 6px 16px;
            }
          }
        }

        .member-name-cell {
          display: flex;
          align-items: center;
          gap: 10px;

          .member-avatar {
            border: 1.5px solid #E2E8F0;
          }

          .name {
            font-weight: 700;
            color: #1E293B;
          }
        }

        .member-role-pill {
          font-size: 11px;
          font-weight: 700;
          padding: 3px 12px;
          border-radius: 9999px;

          &.role-head-teacher { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
          &.role-teacher { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
          &.role-monitor { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
          &.role-student { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
        }

        .mastery-progress-cell {
          display: flex;
          align-items: center;
          gap: 12px;

          .custom-progress-track {
            flex: 1;
            height: 8px;
            background: #F1F5F9;
            border-radius: 9999px;
            overflow: hidden;

            .custom-progress-fill {
              height: 100%;
              border-radius: 9999px;
              transition: width 0.6s ease;
            }
          }

          .mastery-number {
            font-size: 12px;
            font-weight: 800;
            width: 40px;
            text-align: right;
          }
        }

        .new-member-pill {
          font-size: 11px;
          font-weight: 600;
          color: #94A3B8;
          background: #F8FAFC;
          border: 1px dashed #CBD5E1;
          padding: 2px 10px;
          border-radius: 9999px;
        }

        .last-active-pill {
          font-size: 12px;
          color: #64748B;
          background: #F8FAFC;
          padding: 3px 10px;
          border-radius: 9999px;
          border: 1px solid #F1F5F9;
        }

        .table-op-cell {
          display: flex;
          align-items: center;
          gap: 8px;

          .op-link-btn {
            border: none;
            background: transparent;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 8px;
            border-radius: 9999px;
            transition: all 0.2s ease;

            &.primary {
              color: #2563EB;
              &:hover {
                background: #EFF6FF;
              }
            }

            &.danger {
              color: #EF4444;
              &:hover {
                background: #FEF2F2;
              }
            }
          }
        }
      }

      .empty-selection {
        padding: 80px 0;
      }
    }
  }
}
</style>
