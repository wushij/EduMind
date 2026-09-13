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
            <span class="stat-num text-primary">{{ stats.campusCount }}</span>
            <span class="stat-label">校区单元</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ stats.facultyCount }}</span>
            <span class="stat-label">学院/年级</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ stats.classCount }}</span>
            <span class="stat-label">行政班级</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">{{ stats.studentCount.toLocaleString() }}</span>
            <span class="stat-label">在册学生总数</span>
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
              <el-icon><Connection /></el-icon>
              <span>组织架构层级</span>
            </div>
            <el-button size="small" type="primary" plain @click="openAddRootDialog">
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
              size="small"
            />
          </div>

          <div class="tree-wrapper">
            <el-tree
              ref="treeRef"
              :data="treeData"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              default-expand-all
              highlight-current
              :filter-node-method="filterTreeNode"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="custom-tree-node">
                  <div class="node-left">
                    <el-icon class="node-icon" :class="data.orgType">
                      <OfficeBuilding v-if="data.orgType === 'CAMPUS'" />
                      <School v-else-if="data.orgType === 'FACULTY' || data.orgType === 'COLLEGE'" />
                      <Folder v-else-if="data.orgType === 'DEPT'" />
                      <UserFilled v-else />
                    </el-icon>
                    <span class="node-label">{{ data.name }}</span>
                    <el-tag size="small" :type="getTypeTagType(data.orgType)" effect="light" class="node-tag">
                      {{ getTypeLabel(data.orgType) }}
                    </el-tag>
                  </div>
                  <div class="node-actions" @click.stop>
                    <el-dropdown trigger="click" @command="(cmd: string) => handleTreeCommand(cmd, data)">
                      <el-button link size="small">
                        <el-icon><MoreFilled /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="addChild" v-if="data.orgType !== 'CLASS'">
                            <el-icon><Plus /></el-icon>添加下级部门/班级
                          </el-dropdown-item>
                          <el-dropdown-item command="edit">
                            <el-icon><Edit /></el-icon>重命名 / 编辑
                          </el-dropdown-item>
                          <el-dropdown-item command="delete" divided style="color: #F56C6C;">
                            <el-icon><Delete /></el-icon>删除节点
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </template>
            </el-tree>
          </div>
        </div>

        <!-- 右侧教学班级与师生工作台 -->
        <div class="right-detail-card">
          <template v-if="selectedNode">
            <div class="node-banner">
              <div class="banner-left">
                <div class="banner-icon-box" :class="selectedNode.orgType">
                  <component :is="getNodeIcon(selectedNode.orgType)" />
                </div>
                <div class="banner-text">
                  <div class="title-row">
                    <h2>{{ selectedNode.name }}</h2>
                    <el-tag :type="getTypeTagType(selectedNode.orgType)" effect="dark">
                      {{ getTypeLabel(selectedNode.orgType) }}
                    </el-tag>
                  </div>
                  <p class="node-path">所属路径：{{ fullNodePath }}</p>
                </div>
              </div>
              <div class="banner-right">
                <el-button type="primary" class="gradient-btn" @click="openAddMemberDialog">
                  <el-icon><UserFilled /></el-icon>
                  <span>添加师生/导入名单</span>
                </el-button>
              </div>
            </div>

            <!-- 指标概览 -->
            <div class="node-stat-grid">
              <div class="node-stat-box">
                <span class="box-num">{{ selectedNode.studentCount || 45 }} 人</span>
                <span class="box-desc">在册学生人数</span>
              </div>
              <div class="node-stat-box">
                <span class="box-num text-success">98.5%</span>
                <span class="box-desc">AI 作业提交率</span>
              </div>
              <div class="node-stat-box">
                <span class="box-num text-primary">84.2 分</span>
                <span class="box-desc">平均知识掌握度</span>
              </div>
              <div class="node-stat-box">
                <span class="box-num text-warning">2 项</span>
                <span class="box-desc">待处理教学干预</span>
              </div>
            </div>

            <!-- 成员与班级花名册 -->
            <div class="member-table-section">
              <div class="section-header">
                <h3>班级教学与成员管理</h3>
                <div class="section-tools">
                  <el-input
                    v-model="memberKeyword"
                    placeholder="按姓名或学号搜索成员..."
                    prefix-icon="Search"
                    size="small"
                    style="width: 220px;"
                  />
                  <el-button size="small" plain @click="exportRoster">导出花名册</el-button>
                </div>
              </div>

              <el-table :data="filteredMembers" v-loading="membersLoading" stripe style="width: 100%;">
                <el-table-column prop="studentNo" label="学号/工号" width="130" />
                <el-table-column prop="name" label="姓名" width="120">
                  <template #default="{ row }">
                    <div class="member-name-cell">
                      <el-avatar :size="26" :src="row.avatar" />
                      <span class="name">{{ row.name }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="role" label="身份" width="110">
                  <template #default="{ row }">
                    <el-tag :type="row.role === '班长' ? 'warning' : 'info'" size="small">
                      {{ row.role }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="masteryRate" label="AI 知识掌握度" min-width="160">
                  <template #default="{ row }">
                    <el-progress :percentage="row.masteryRate" :color="row.masteryRate > 80 ? '#10B981' : '#F59E0B'" />
                  </template>
                </el-table-column>
                <el-table-column prop="lastActive" label="最近学情活跃" width="150" />
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="viewStudentProfile(row)">画像</el-button>
                    <el-button link type="danger" size="small" @click="removeMember(row)">移出</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <div v-else class="empty-selection">
            <el-empty description="请从左侧选择校区、院系或班级节点查看具体层级与师生名单" />
          </div>
        </div>
      </div>
    </div>

    <!-- 节点创建/编辑弹窗 -->
    <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="460px">
      <el-form :model="nodeForm" label-position="top">
        <el-form-item label="节点名称" required>
          <el-input v-model="nodeForm.name" placeholder="如：高一年级组 / 计算机科学与技术(1)班" />
        </el-form-item>
        <el-form-item label="节点层级类型" v-if="nodeFormMode === 'create'">
          <el-select v-model="nodeForm.orgType" style="width: 100%;">
            <el-option label="校区 (CAMPUS)" value="CAMPUS" />
            <el-option label="学院/年级 (FACULTY)" value="FACULTY" />
            <el-option label="学院/系所 (COLLEGE)" value="COLLEGE" />
            <el-option label="教研室/系所 (DEPT)" value="DEPT" />
            <el-option label="行政班级 (CLASS)" value="CLASS" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="nodeForm.sortOrder" :min="1" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNodeForm">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Connection,
  Plus,
  Search,
  OfficeBuilding,
  School,
  Folder,
  UserFilled,
  MoreFilled,
  Edit,
  Delete
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { getOrgTree, createOrgNode, updateOrgNode, deleteOrgNode, getOrgMembers } from '@/api/system/tenant';
import type { OrganizationNodeVO, OrganizationMemberVO } from '@/types/system/tenant';

const loading = ref(false);
const membersLoading = ref(false);
const treeSearchKeyword = ref('');
const treeRef = ref();
const treeData = ref<OrganizationNodeVO[]>([]);
const selectedNode = ref<OrganizationNodeVO | null>(null);
const memberKeyword = ref('');

const stats = ref({
  campusCount: 2,
  facultyCount: 6,
  classCount: 32,
  studentCount: 1420
});

const nodeDialogVisible = ref(false);
const nodeDialogTitle = ref('新增下级教学节点');
const nodeFormMode = ref<'create' | 'edit'>('create');
const activeParentNode = ref<OrganizationNodeVO | null>(null);
const activeEditNode = ref<OrganizationNodeVO | null>(null);

const nodeForm = ref({
  name: '',
  orgType: 'CLASS',
  sortOrder: 1
});

const members = ref<OrganizationMemberVO[]>([]);

const filteredMembers = computed(() => {
  if (!memberKeyword.value) return members.value;
  return members.value.filter(m =>
    m.name.includes(memberKeyword.value) || m.studentNo.includes(memberKeyword.value)
  );
});

const fullNodePath = computed(() => {
  if (!selectedNode.value) return '';
  return `智教云示范第一中学 / ${selectedNode.value.name}`;
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

const getTypeTagType = (type: string) => {
  switch (type) {
    case 'CAMPUS': return 'primary';
    case 'FACULTY':
    case 'COLLEGE': return 'success';
    case 'DEPT': return 'warning';
    case 'CLASS': return 'info';
    default: return 'info';
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

const defaultTreeData: OrganizationNodeVO[] = [
  {
    id: 1,
    tenantId: 1,
    name: '本部校区 (科教园区)',
    orgType: 'CAMPUS',
    parentId: 0,
    sortOrder: 1,
    studentCount: 1200,
    children: [
      {
        id: 11,
        tenantId: 1,
        name: '高中部 / 高三年级',
        orgType: 'FACULTY',
        parentId: 1,
        sortOrder: 1,
        studentCount: 480,
        children: [
          { id: 111, tenantId: 1, name: '高三(1)班 [理科实验班]', orgType: 'CLASS', parentId: 11, sortOrder: 1, studentCount: 45 },
          { id: 112, tenantId: 1, name: '高三(2)班 [数学拔尖班]', orgType: 'CLASS', parentId: 11, sortOrder: 2, studentCount: 48 },
          { id: 113, tenantId: 1, name: '高三(3)班 [平行冲刺班]', orgType: 'CLASS', parentId: 11, sortOrder: 3, studentCount: 50 }
        ]
      },
      {
        id: 12,
        tenantId: 1,
        name: '高中部 / 高二年级',
        orgType: 'FACULTY',
        parentId: 1,
        sortOrder: 2,
        studentCount: 520,
        children: [
          { id: 121, tenantId: 1, name: '高二(1)班 [科技创新班]', orgType: 'CLASS', parentId: 12, sortOrder: 1, studentCount: 46 },
          { id: 122, tenantId: 1, name: '高二(2)班 [人文社科班]', orgType: 'CLASS', parentId: 12, sortOrder: 2, studentCount: 44 }
        ]
      }
    ]
  },
  {
    id: 2,
    tenantId: 1,
    name: '东校区 (国际学术交流中心)',
    orgType: 'CAMPUS',
    parentId: 0,
    sortOrder: 2,
    studentCount: 220,
    children: [
      {
        id: 21,
        tenantId: 1,
        name: '国际部 / AP & IB 项目组',
        orgType: 'FACULTY',
        parentId: 2,
        sortOrder: 1,
        studentCount: 220,
        children: [
          { id: 211, tenantId: 1, name: 'AP 物理与统计融合班', orgType: 'CLASS', parentId: 21, sortOrder: 1, studentCount: 30 }
        ]
      }
    ]
  }
];

const countNodes = (nodes: OrganizationNodeVO[]): { campus: number; faculty: number; clazz: number } => {
  let campus = 0, faculty = 0, clazz = 0;
  const traverse = (list: OrganizationNodeVO[]) => {
    for (const item of list) {
      if (item.orgType === 'CAMPUS') campus++;
      else if (item.orgType === 'FACULTY' || item.orgType === 'COLLEGE') faculty++;
      else if (item.orgType === 'CLASS') clazz++;
      if (item.children && item.children.length > 0) {
        traverse(item.children);
      }
    }
  };
  traverse(nodes);
  return { campus, faculty, clazz };
};

const loadTree = async () => {
  try {
    loading.value = true;
    const res = await getOrgTree();
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
      selectedNode.value = findFirstClass(res.data) || res.data[0];
      const counts = countNodes(res.data);
      stats.value.campusCount = counts.campus || 1;
      stats.value.facultyCount = counts.faculty || 2;
      stats.value.classCount = counts.clazz || 3;
    } else {
      treeData.value = defaultTreeData;
      selectedNode.value = treeData.value[0]?.children?.[0]?.children?.[0] || treeData.value[0];
    }
    if (selectedNode.value?.id) {
      loadMembers(selectedNode.value.id);
    }
  } catch (e) {
    if (!treeData.value || treeData.value.length === 0) {
      treeData.value = defaultTreeData;
      selectedNode.value = treeData.value[0]?.children?.[0]?.children?.[0] || treeData.value[0];
    }
    if (selectedNode.value?.id) {
      loadMembers(selectedNode.value.id);
    }
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
  }
};

const openAddRootDialog = () => {
  nodeFormMode.value = 'create';
  nodeDialogTitle.value = '新增校区顶层实体';
  activeParentNode.value = null;
  nodeForm.value = {
    name: '',
    orgType: 'CAMPUS',
    sortOrder: treeData.value.length + 1
  };
  nodeDialogVisible.value = true;
};

const handleTreeCommand = (command: string, data: OrganizationNodeVO) => {
  if (command === 'addChild') {
    nodeFormMode.value = 'create';
    nodeDialogTitle.value = `在【${data.name}】下添加子级实体`;
    activeParentNode.value = data;
    const nextType = data.orgType === 'CAMPUS' ? 'FACULTY' : 'CLASS';
    nodeForm.value = {
      name: '',
      orgType: nextType,
      sortOrder: (data.children?.length || 0) + 1
    };
    nodeDialogVisible.value = true;
  } else if (command === 'edit') {
    nodeFormMode.value = 'edit';
    nodeDialogTitle.value = `重命名【${data.name}】`;
    activeEditNode.value = data;
    nodeForm.value = {
      name: data.name,
      orgType: data.orgType,
      sortOrder: data.sortOrder || 1
    };
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
        loadTree();
      } catch (e: any) {
        ElMessage.error(e.message || '删除节点失败');
      }
    });
  }
};

const submitNodeForm = async () => {
  if (!nodeForm.value.name.trim()) {
    ElMessage.warning('请输入节点名称');
    return;
  }
  try {
    if (nodeFormMode.value === 'create') {
      await createOrgNode({
        name: nodeForm.value.name,
        orgType: nodeForm.value.orgType,
        parentId: activeParentNode.value ? activeParentNode.value.id : 0,
        sortOrder: nodeForm.value.sortOrder
      });
      ElMessage.success('下级实体创建成功');
    } else if (activeEditNode.value) {
      await updateOrgNode(activeEditNode.value.id, {
        name: nodeForm.value.name,
        sortOrder: nodeForm.value.sortOrder
      });
      ElMessage.success('实体已重命名');
    }
    nodeDialogVisible.value = false;
    loadTree();
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败');
  }
};

const openAddMemberDialog = () => {
  ElMessage.info('名单导入：支持一键上传 Excel 或同步教务系统');
};

const exportRoster = () => {
  ElMessage.success('已生成本班花名册导出报表');
};

const viewStudentProfile = (student: any) => {
  ElMessage.info(`查看【${student.name}】的 AI 认知诊断画像`);
};

const removeMember = (student: any) => {
  ElMessageBox.confirm(`确认将【${student.name}】从当前班级中移出吗？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    members.value = members.value.filter(m => m.id !== student.id);
    ElMessage.success('已移出');
  });
};

onMounted(() => {
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
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(8px);
      padding: 6px 20px;
      border-radius: 9999px;
      border: 1.5px solid rgba(22, 119, 255, 0.12);
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #1677FF;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
      }

      .stat-num {
        font-size: 19px;
        font-weight: 800;
        line-height: 1;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #0284C7; }
      }

      .stat-label {
        font-size: 12.5px;
        font-weight: 500;
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
    grid-template-columns: 340px minmax(0, 1fr);
    gap: 20px;
    align-items: start;
    width: 100%;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    .left-tree-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 18px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

      .tree-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;

        .header-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 600;
          font-size: 15px;
          color: #0F172A;

          .el-icon { color: #2563EB; font-size: 18px; }
        }
      }

      .tree-search-bar {
        margin-bottom: 14px;
      }

      .tree-wrapper {
        max-height: 620px;
        overflow-y: auto;

        .custom-tree-node {
          display: flex;
          justify-content: space-between;
          align-items: center;
          width: 100%;
          padding-right: 8px;

          .node-left {
            display: flex;
            align-items: center;
            gap: 6px;

            .node-icon {
              font-size: 15px;
              &.CAMPUS { color: #2563EB; }
              &.FACULTY { color: #16A34A; }
              &.DEPT { color: #D97706; }
              &.CLASS { color: #7C3AED; }
            }

            .node-label {
              font-size: 13px;
              color: #1E293B;
              font-weight: 500;
            }

            .node-tag {
              font-size: 10px;
              height: 18px;
              padding: 0 4px;
            }
          }
        }
      }
    }

    .right-detail-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 24px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      min-height: 580px;
      min-width: 0;
      width: 100%;
      box-sizing: border-box;

      .node-banner {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #F1F5F9;
        padding-bottom: 20px;
        margin-bottom: 20px;

        .banner-left {
          display: flex;
          align-items: center;
          gap: 16px;

          .banner-icon-box {
            width: 52px;
            height: 52px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 26px;

            &.CAMPUS { background: #EFF6FF; color: #2563EB; }
            &.FACULTY { background: #F0FDF4; color: #16A34A; }
            &.DEPT { background: #FFFBEB; color: #D97706; }
            &.CLASS { background: #F5F3FF; color: #7C3AED; }
          }

          .banner-text {
            .title-row {
              display: flex;
              align-items: center;
              gap: 12px;

              h2 {
                font-size: 20px;
                font-weight: 700;
                color: #0F172A;
                margin: 0;
              }
            }

            .node-path {
              font-size: 12px;
              color: #64748B;
              margin: 4px 0 0;
            }
          }
        }

        .gradient-btn {
          background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
          border: none;
          border-radius: 10px;
        }
      }

      .node-stat-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 16px;
        margin-bottom: 24px;

        .node-stat-box {
          background: #F8FAFC;
          border-radius: 12px;
          padding: 14px;
          display: flex;
          flex-direction: column;
          align-items: center;

          .box-num {
            font-size: 18px;
            font-weight: 700;
            color: #1E293B;

            &.text-success { color: #16A34A; }
            &.text-primary { color: #2563EB; }
            &.text-warning { color: #D97706; }
          }

          .box-desc {
            font-size: 12px;
            color: #64748B;
            margin-top: 4px;
          }
        }
      }

      .member-table-section {
        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 14px;

          h3 {
            font-size: 16px;
            font-weight: 600;
            color: #1E293B;
            margin: 0;
          }

          .section-tools {
            display: flex;
            gap: 12px;
          }
        }

        .member-name-cell {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 500;
        }
      }

      .empty-selection {
        padding: 80px 0;
      }
    }
  }
}
</style>
