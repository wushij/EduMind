<template>
  <div class="page-container role-management-page gb-fade-in">
    <!-- 顶部过滤栏 (对标 Code Compass Filter Card) -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <span class="filter-title">系统角色与权限授权</span>
          <span class="filter-tag">RBAC 核心控制</span>
          <span class="filter-count">共 {{ roles.length }} 个系统角色 · 支持细粒度菜单与功能授权</span>
        </div>
        <div class="filter-actions">
          <el-input
            v-model="searchKeyword"
            clearable
            placeholder="搜索角色名称或编码..."
            style="width: 220px"
            :prefix-icon="Search"
            class="search-input"
          />
          <el-button type="primary" round class="add-btn" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 新增角色
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 角色数据表格 (对标 Code Compass gb-modern-table) -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="filteredRoles"
        style="width: 100%"
        class="gb-modern-table role-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column prop="roleName" label="角色名称" min-width="160">
          <template #default="{ row }">
            <div class="role-name-cell">
              <strong class="role-name-text">{{ row.roleName }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="roleCode" label="角色标识" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.roleCode)" size="small" round font-mono>
              {{ row.roleCode }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="已赋权限数" width="130" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" round class="perm-count-badge">
              {{ row.permissions?.length || 0 }} 项权限
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="role-desc-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button link type="primary" size="small" @click="openPermissionDrawer(row)">
                分配权限
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                :disabled="row.roleCode === 'ADMIN'"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 36px 0">
            <el-empty description="暂无角色数据" />
          </div>
        </template>
      </el-table>
    </el-card>

    <!-- 角色新增 / 编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增角色' : '编辑角色'"
      width="480px"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            :disabled="formMode === 'edit'"
            placeholder="例如：TEACHER / ASSISTANT"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="例如：任课骨干教师" />
        </el-form-item>
        <el-form-item label="说明备注">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="例如：负责课程教学、作业批改与题库管理"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="formVisible = false">取消</el-button>
        <el-button type="primary" round :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限抽屉 (1:1 模仿 Code Compass permDrawer) -->
    <el-drawer
      v-model="permDrawer"
      title="分配菜单权限"
      size="480px"
      append-to-body
      class="perm-drawer"
    >
      <div class="perm-drawer-body">
        <p class="drawer-subtitle">
          当前角色：<strong>{{ currentRole?.roleName }}</strong> ({{ currentRole?.roleCode }})
        </p>

        <!-- 快捷操作栏 -->
        <div class="drawer-toolbar">
          <el-input
            v-model="treeFilterText"
            placeholder="过滤菜单或权限..."
            clearable
            size="small"
            :prefix-icon="Search"
            style="width: 180px"
          />
          <div class="toolbar-btns">
            <el-button size="small" link type="primary" @click="checkAllNodes(true)">全选</el-button>
            <el-button size="small" link @click="checkAllNodes(false)">清空</el-button>
            <el-button size="small" link @click="toggleTreeExpand">
              {{ isTreeExpanded ? '折叠全部' : '展开全部' }}
            </el-button>
          </div>
        </div>

        <!-- 侧边栏对齐的菜单权限树 -->
        <div v-loading="permissionLoading" class="tree-box">
          <el-tree
            ref="treeRef"
            :data="menuTreeData"
            show-checkbox
            node-key="id"
            :default-expand-all="isTreeExpanded"
            :filter-node-method="filterTreeNode"
            :props="{ label: 'name', children: 'children' }"
          >
            <template #default="{ data }">
              <div class="tree-node-item">
                <el-icon v-if="data.icon" class="node-icon" color="#6366f1">
                  <component :is="getIconComponent(data.icon)" />
                </el-icon>
                <span class="node-title">{{ data.name }}</span>
                <span v-if="data.children?.length" class="node-count">({{ data.children.length }})</span>
                <el-tag
                  v-if="data.permission"
                  size="small"
                  type="danger"
                  round
                  class="node-perm-tag font-mono"
                >
                  {{ data.permission }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button round @click="permDrawer = false">取消</el-button>
          <el-button type="primary" round :loading="submitting" @click="savePermissions">保存授权</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type ElTree } from 'element-plus';
import {
  Plus,
  Search,
  Reading,
  MagicStick,
  FolderOpened,
  Document,
  DataAnalysis,
  Files,
  Setting,
  Bell,
  Collection,
  DocumentAdd,
  EditPen,
  Service,
  Tickets,
  CircleCheck,
  Cpu,
  Folder,
  FolderAdd,
  DocumentChecked,
  Notebook,
  TrendCharts,
  Upload,
  School,
  Connection,
  User,
  Lock,
  Key,
  ChatLineSquare,
  Money
} from '@element-plus/icons-vue';
import {
  createRole,
  deleteRole,
  getPermissions,
  getRoles,
  updateRole,
  updateRolePermissions
} from '@/api/system/role';
import type { PermissionVO, RoleVO } from '@/types/system/rbac';
import { buildSidebarMenuTree, flattenPermissions, type SysMenuNode } from '@/constants/permission';

const roles = ref<RoleVO[]>([]);
const rawPermissions = ref<PermissionVO[]>([]);
const loading = ref(false);
const permissionLoading = ref(false);
const submitting = ref(false);
const searchKeyword = ref('');

const formVisible = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const editingRoleId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const permDrawer = ref(false);
const currentRole = ref<RoleVO | null>(null);
const treeRef = ref<InstanceType<typeof ElTree>>();
const treeFilterText = ref('');
const isTreeExpanded = ref(true);

const form = reactive({
  roleCode: '',
  roleName: '',
  description: ''
});

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
};

const menuTreeData = computed(() => {
  return buildSidebarMenuTree(rawPermissions.value);
});

watch(treeFilterText, (val) => {
  treeRef.value?.filter(val);
});

function filterTreeNode(val: string, data: any) {
  if (!val) return true;
  const kw = val.toLowerCase();
  return data.name?.toLowerCase().includes(kw) || data.permission?.toLowerCase().includes(kw);
}

function getIconComponent(iconName?: string) {
  const map: Record<string, any> = {
    Reading,
    MagicStick,
    FolderOpened,
    Document,
    DataAnalysis,
    Files,
    Setting,
    Bell,
    Collection,
    DocumentAdd,
    EditPen,
    Service,
    Tickets,
    CircleCheck,
    Cpu,
    Folder,
    FolderAdd,
    DocumentChecked,
    Notebook,
    TrendCharts,
    Upload,
    School,
    Connection,
    User,
    Lock,
    Key,
    ChatLineSquare,
    Money
  };
  return (iconName && map[iconName]) || Document;
}

const filteredRoles = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return roles.value;
  return roles.value.filter(
    (r) =>
      r.roleName.toLowerCase().includes(kw) ||
      r.roleCode.toLowerCase().includes(kw) ||
      (r.description || '').toLowerCase().includes(kw)
  );
});

async function loadRoles() {
  loading.value = true;
  try {
    const res = await getRoles();
    roles.value = res.data || [];
  } finally {
    loading.value = false;
  }
}

async function loadPermissions() {
  permissionLoading.value = true;
  try {
    const res = await getPermissions();
    rawPermissions.value = res.data || [];
  } finally {
    permissionLoading.value = false;
  }
}

function resetForm() {
  form.roleCode = '';
  form.roleName = '';
  form.description = '';
}

function openCreateDialog() {
  formMode.value = 'create';
  editingRoleId.value = null;
  resetForm();
  formVisible.value = true;
}

function openEditDialog(row: RoleVO) {
  formMode.value = 'edit';
  editingRoleId.value = row.id;
  form.roleCode = row.roleCode;
  form.roleName = row.roleName;
  form.description = row.description || '';
  formVisible.value = true;
}

async function openPermissionDrawer(row: RoleVO) {
  currentRole.value = row;
  permDrawer.value = true;
  treeFilterText.value = '';

  if (!rawPermissions.value.length) {
    await loadPermissions();
  }

  await nextTick();

  // 获取该角色所有的权限叶子节点 ID
  const flat = flattenPermissions(rawPermissions.value);
  const targetCodes = row.permissions || [];
  const matchedIds = flat
    .filter((p) => targetCodes.includes(p.permissionCode))
    .map((p) => p.id);

  // 严格设置勾选，避免父节点半选引发误勾
  treeRef.value?.setCheckedKeys(matchedIds, false);
}

function checkAllNodes(check: boolean) {
  if (check) {
    const allLeafIds = flattenPermissions(rawPermissions.value).map((p) => p.id);
    treeRef.value?.setCheckedKeys(allLeafIds, false);
  } else {
    treeRef.value?.setCheckedKeys([], false);
  }
}

function toggleTreeExpand() {
  isTreeExpanded.value = !isTreeExpanded.value;
  // 切换所有一级节点
  const nodes = menuTreeData.value;
  nodes.forEach((node) => {
    const elNode = treeRef.value?.getNode(node.id);
    if (elNode) {
      elNode.expanded = isTreeExpanded.value;
    }
  });
}

async function submitForm() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (formMode.value === 'create') {
      await createRole({
        roleCode: form.roleCode,
        roleName: form.roleName,
        description: form.description
      });
      ElMessage.success('角色创建成功');
    } else if (editingRoleId.value) {
      await updateRole(editingRoleId.value, {
        roleName: form.roleName,
        description: form.description
      });
      ElMessage.success('角色已保存');
    }
    formVisible.value = false;
    await loadRoles();
  } finally {
    submitting.value = false;
  }
}

async function savePermissions() {
  if (!currentRole.value) return;
  submitting.value = true;
  try {
    const checkedKeys = (treeRef.value?.getCheckedKeys(false) as any[]) || [];
    // 只提交数值类型的叶子权限 ID（排除顶级字符串目录 ID 如 mod-course）
    const validNumericIds = checkedKeys.filter((k) => typeof k === 'number');

    await updateRolePermissions(currentRole.value.id, validNumericIds);
    ElMessage.success('已保存权限');
    permDrawer.value = false;
    await loadRoles();
  } catch (err: any) {
    ElMessage.error(err?.message || '保存权限失败');
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row: RoleVO) {
  if (row.roleCode === 'ADMIN') {
    ElMessage.warning('超级管理员角色无法删除');
    return;
  }
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '确认删除', {
    type: 'warning'
  });
  await deleteRole(row.id);
  ElMessage.success('已删除');
  await loadRoles();
}

function getRoleTagType(code: string) {
  const c = code.toUpperCase();
  if (c.includes('ADMIN')) return 'danger';
  if (c.includes('TEACHER')) return 'primary';
  if (c.includes('STUDENT')) return 'warning';
  return 'info';
}

onMounted(loadRoles);
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
}

.filter-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.filter-tag {
  font-size: 11.5px;
  font-weight: 600;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 8px;
  border-radius: 6px;
}

.filter-count {
  font-size: 12px;
  color: #64748b;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.role-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  height: 48px;
}

.role-name-text {
  font-weight: 600;
  color: #0f172a;
}

.role-desc-text {
  font-size: 13px;
  color: #64748b;
}

.perm-count-badge {
  font-weight: 600;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 分配菜单权限抽屉 (对齐 Code Compass 抽屉规范) */
.perm-drawer-body {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin: 0;
}

.drawer-subtitle strong {
  color: #0f172a;
}

.drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.toolbar-btns {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-box {
  max-height: calc(100vh - 230px);
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
}

.tree-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  width: 100%;
}

.node-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.node-title {
  font-weight: 500;
  color: #1e293b;
}

.node-count {
  font-size: 11px;
  color: #94a3b8;
}

.node-perm-tag {
  margin-left: auto;
  font-size: 11px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
