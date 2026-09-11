<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <el-button type="primary" @click="openCreateDialog">新建角色</el-button>
    </div>

    <div class="page-content">
      <el-card shadow="never">
        <el-table v-loading="loading" :data="roles" stripe>
          <el-table-column prop="roleCode" label="角色编码" width="140" />
          <el-table-column prop="roleName" label="角色名称" width="160" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="权限数" width="100">
            <template #default="{ row }">
              {{ row.permissions?.length || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="primary" @click="openPermissionDialog(row)">分配权限</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建角色' : '编辑角色'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="formMode === 'edit'" placeholder="如 TEACHER" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 任课教师" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="分配权限" width="560px">
      <el-tree
        ref="treeRef"
        v-loading="permissionLoading"
        :data="permissionTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'permissionName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  createRole,
  deleteRole,
  getPermissions,
  getRoles,
  updateRole,
  updateRolePermissions
} from '@/api/system/role';
import type { PermissionVO, RoleVO } from '@/types/system/rbac';

const roles = ref<RoleVO[]>([]);
const permissionTree = ref<PermissionVO[]>([]);
const loading = ref(false);
const permissionLoading = ref(false);
const submitting = ref(false);
const formVisible = ref(false);
const permissionVisible = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const editingRoleId = ref<number | null>(null);
const currentRole = ref<RoleVO | null>(null);
const formRef = ref<FormInstance>();
const treeRef = ref<any>();

const form = reactive({
  roleCode: '',
  roleName: '',
  description: ''
});

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
};

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
    permissionTree.value = res.data || [];
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

async function openPermissionDialog(row: RoleVO) {
  currentRole.value = row;
  permissionVisible.value = true;
  if (!permissionTree.value.length) {
    await loadPermissions();
  }
  const checkedIds = findPermissionIdsByCodes(permissionTree.value, row.permissions || []);
  treeRef.value?.setCheckedKeys(checkedIds);
}

function findPermissionIdsByCodes(nodes: PermissionVO[], codes: string[]): number[] {
  const ids: number[] = [];
  const walk = (list: PermissionVO[]) => {
    list.forEach((node) => {
      if (codes.includes(node.permissionCode)) {
        ids.push(node.id);
      }
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };
  walk(nodes);
  return ids;
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
      ElMessage.success('角色更新成功');
    }
    formVisible.value = false;
    await loadRoles();
  } finally {
    submitting.value = false;
  }
}

async function submitPermissions() {
  if (!currentRole.value) return;
  const checked = treeRef.value?.getCheckedKeys(false) as number[];
  const halfChecked = treeRef.value?.getHalfCheckedKeys() as number[];
  const permissionIds = [...new Set([...(checked || []), ...(halfChecked || [])])];
  submitting.value = true;
  try {
    await updateRolePermissions(currentRole.value.id, permissionIds);
    ElMessage.success('权限分配成功');
    permissionVisible.value = false;
    await loadRoles();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row: RoleVO) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' });
  await deleteRole(row.id);
  ElMessage.success('删除成功');
  await loadRoles();
}

onMounted(loadRoles);
</script>

<style scoped lang="scss">
.page-container {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #1f2937;
    }
  }
}
</style>
