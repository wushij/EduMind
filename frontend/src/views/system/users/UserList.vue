<template>
  <div class="user-list-container">
    <div class="page-header-card">
      <div class="header-left">
        <h2>用户管理</h2>
        <p>管理系统账号、角色授权与账号状态</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
          新增系统用户
        </el-button>
      </div>
    </div>

    <!-- 筛选过滤行 -->
    <div class="filter-capsule-card">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名、姓名、邮箱..."
          clearable
          class="search-input"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="selectedRole" placeholder="系统角色" clearable class="filter-select" @change="handleSearch">
          <el-option label="全部角色" value="" />
          <el-option label="系统管理员" value="ADMIN" />
          <el-option label="任课教师" value="TEACHER" />
          <el-option label="在读学生" value="STUDENT" />
        </el-select>
        <el-select v-model="selectedStatus" placeholder="账号状态" clearable class="filter-select" @change="handleSearch">
          <el-option label="全部状态" value="" />
          <el-option label="正常活跃" value="ENABLE" />
          <el-option label="冻结停用" value="DISABLED" />
        </el-select>
      </div>
    </div>

    <!-- 用户列表表格卡片 -->
    <div v-loading="loading" class="user-table-card">
      <el-table :data="users" stripe class="main-table">
        <el-table-column label="用户信息" min-width="220">
          <template #default="{ row }">
            <div class="em-user-cell">
              <el-avatar
                :size="40"
                :src="(!avatarBroken[row.id] && normalizeAvatarUrl(row.avatar)) ? normalizeAvatarUrl(row.avatar) : undefined"
                class="em-user-avatar-sm"
                @error="onAvatarError(row.id)"
              >
                {{ (row.realName || row.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-meta-info">
                <div class="user-real-name">{{ row.realName || row.username }}</div>
                <div class="user-account-sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="所属院系 / 班级" prop="department" min-width="180">
          <template #default="{ row }">
            <span class="text-slate-600 text-xs">{{ row.department || '计算机学院' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="授权角色" min-width="200">
          <template #default="{ row }">
            <div class="role-tags-cell">
              <el-tag
                v-for="r in row.roles"
                :key="r"
                size="small"
                :type="getRoleTagType(r)"
              >
                {{ getRoleLabel(r) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="120">
          <template #default="{ row }">
            <el-tag :type="isUserEnabled(row) ? 'success' : 'danger'" size="small">
              {{ isUserEnabled(row) ? '正常活跃' : '已冻结' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="table-action-group">
              <button
                type="button"
                class="table-action-pill table-action-pill--primary"
                @click="router.push(`/system/users/${row.id}`)"
              >
                档案与权限
              </button>
              <button
                type="button"
                class="table-action-pill"
                :class="isUserEnabled(row) ? 'table-action-pill--warning' : 'table-action-pill--success'"
                @click="toggleUserStatus(row)"
              >
                {{ isUserEnabled(row) ? '冻结' : '解冻' }}
              </button>
              <button
                type="button"
                class="table-action-pill table-action-pill--danger"
                :disabled="!canDeleteUser(row)"
                @click="handleDeleteUser(row)"
              >
                删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadUsers"
      />
    </div>

    <!-- 新增用户对话框 -->
    <el-dialog v-model="showCreateDialog" title="新增系统用户" width="520px" destroy-on-close>
      <el-form ref="dialogFormRef" :model="newUserForm" :rules="dialogRules" label-position="top">
        <el-form-item label="用户登录名" prop="username">
          <el-input v-model="newUserForm.username" placeholder="英文字母或数字，例如 teacher_wang" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="newUserForm.realName" placeholder="例如：王建国" />
        </el-form-item>
        <el-form-item label="初始角色身份" prop="role">
          <el-select v-model="newUserForm.role" placeholder="请选择角色" class="w-full">
            <el-option label="任课教师" value="TEACHER" />
            <el-option label="在读学生" value="STUDENT" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="newUserForm.password" placeholder="默认 admin123" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateUser">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
import { getUsers, createUser, updateUserStatus, deleteUser } from '@/api/system/user';
import { getRoles } from '@/api/system/role';
import AppPagination from '@/components/common/AppPagination.vue';
import { normalizeAvatarUrl } from '@/utils/format/file';
import { USE_MOCK } from '@/config/mock';
import { useAuthStore } from '@/stores/auth/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const creating = ref(false);
const showCreateDialog = ref(false);
const dialogFormRef = ref<FormInstance>();

const searchKeyword = ref('');
const selectedRole = ref('');
const selectedStatus = ref('');

const users = ref<any[]>([]);
const roles = ref<Array<{ id: number; roleCode: string }>>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const avatarBroken = ref<Record<number, boolean>>({});

function onAvatarError(userId: number) {
  avatarBroken.value[userId] = true;
}

const newUserForm = reactive({
  username: '',
  realName: '',
  role: 'TEACHER',
  password: 'admin123'
});

const dialogRules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
});

onMounted(async () => {
  await Promise.all([loadRoles(), loadUsers()]);
});

async function loadRoles() {
  try {
    const res = await getRoles();
    roles.value = res.data || [];
  } catch {
    roles.value = USE_MOCK
      ? [
          { id: 1, roleCode: 'ADMIN' },
          { id: 2, roleCode: 'TEACHER' },
          { id: 3, roleCode: 'STUDENT' }
        ]
      : [];
  }
}

async function loadUsers() {
  loading.value = true;
  try {
    const res = await getUsers({
      page: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value.trim() || undefined,
      role: selectedRole.value || undefined,
      status: selectedStatus.value || undefined
    });
    users.value = res.data?.list || [];
    total.value = res.data?.total ?? users.value.length;
  } catch {
    users.value = USE_MOCK ? getDefaultUsers() : [];
    total.value = users.value.length;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadUsers();
}

// 严守默认用户名规范：admin, teacher, student, student2 (无 01 后缀)
function getDefaultUsers() {
  return [
    {
      id: 1,
      username: 'admin',
      realName: '系统超级管理员',
      avatar: 'https://api.dicebear.com/7.x/bottts/svg?seed=EduMindAdmin&backgroundColor=e0e7ff',
      department: '数字化与信息化中心',
      roles: ['ADMIN'],
      status: 'ENABLE'
    },
    {
      id: 2,
      username: 'teacher',
      realName: '李华教授',
      avatar: 'https://api.dicebear.com/7.x/adventurer/svg?seed=TeacherZhang&backgroundColor=dbeafe',
      department: '计算机学院 · 软件工程系',
      roles: ['TEACHER'],
      status: 'ENABLE'
    },
    {
      id: 3,
      username: 'student',
      realName: '张子轩',
      avatar: 'https://api.dicebear.com/7.x/adventurer/svg?seed=StudentLi&backgroundColor=fef3c7',
      department: '计算机学院 · 2024级软工1班',
      roles: ['STUDENT'],
      status: 'ENABLE'
    },
    {
      id: 4,
      username: 'student2',
      realName: '李梦琪',
      avatar: 'https://api.dicebear.com/7.x/micah/svg?seed=ScholarChen&backgroundColor=fce7f3',
      department: '计算机学院 · 2024级软工1班',
      roles: ['STUDENT'],
      status: 'ENABLE'
    }
  ];
}

function getRoleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[role] || role;
}

function getRoleTagType(role: string) {
  const map: Record<string, string> = {
    ADMIN: 'danger',
    TEACHER: 'primary',
    STUDENT: 'success'
  };
  return (map[role] as any) || 'info';
}

function isUserEnabled(row: { status?: string }) {
  return row.status === 'ENABLE' || row.status === 'ENABLED';
}

function canDeleteUser(row: { id: number; username: string }) {
  if (row.username === 'admin') return false;
  return row.id !== authStore.currentUser?.id;
}

async function toggleUserStatus(row: any) {
  const enabling = !isUserEnabled(row);
  const actionText = enabling ? '解冻恢复该用户账号' : '冻结该用户账号';
  const confirmType = enabling ? 'info' : 'warning';

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}「${row.realName || row.username}」吗？${enabling ? '' : '冻结后该用户将无法登录系统。'}`,
      '账号状态变更确认',
      {
        confirmButtonText: enabling ? '确认解冻' : '确认冻结',
        cancelButtonText: '取消',
        type: confirmType
      }
    );
    const newStatus = enabling ? 'ENABLE' : 'DISABLED';
    await updateUserStatus(row.id, newStatus);
    row.status = newStatus;
    ElMessage.success(`已${enabling ? '解冻' : '冻结'}用户：${row.username}`);
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error('更新用户状态失败，请稍后重试');
    }
  }
}

async function handleDeleteUser(row: any) {
  if (!canDeleteUser(row)) {
    ElMessage.warning(row.username === 'admin' ? '系统内置管理员账号不允许删除' : '不能删除当前登录账号');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `删除后账号「${row.realName || row.username}」将无法恢复，其角色授权也将一并清除。确定继续吗？`,
      '删除用户确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    );
    await deleteUser(row.id);
    ElMessage.success(`已删除用户：${row.username}`);
    await loadUsers();
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.message || '删除用户失败，请稍后重试');
    }
  }
}

async function handleCreateUser() {
  if (!dialogFormRef.value) return;
  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return;
    creating.value = true;
    try {
      const role = roles.value.find(item => item.roleCode === newUserForm.role);
      if (!role) {
        ElMessage.error('角色数据未加载，无法创建用户');
        return;
      }
      await createUser({
        username: newUserForm.username,
        password: newUserForm.password,
        realName: newUserForm.realName,
        roleIds: [role.id],
        status: 'ENABLE'
      });
      await loadUsers();
      showCreateDialog.value = false;
      ElMessage.success('新用户创建成功');
  } catch {
      ElMessage.error('创建用户失败，请检查输入或稍后重试');
    } finally {
      creating.value = false;
    }
  });
}
</script>

<style scoped lang="scss">
.user-list-container {
  padding: 24px;
  background: #f8fafc;

  .page-header-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    padding: 20px 24px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);

    .header-left {
      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #0f172a;
      }

      p {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }
  }

  .filter-capsule-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 20px;
    margin-bottom: 20px;

    .filter-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .search-input {
        width: 320px;
      }

      .filter-select {
        width: 150px;
      }
    }
  }

  .user-table-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 16px 20px;

    .em-user-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .em-user-avatar-sm {
        flex-shrink: 0;
        background: linear-gradient(135deg, #dbeafe, #eff6ff);
        color: #2563eb;
        font-weight: 700;
      }

      .user-meta-info {
        min-width: 0;

        .user-real-name {
          font-size: 14px;
          font-weight: 600;
          color: #0f172a;
          line-height: 1.3;
        }

        .user-account-sub {
          font-size: 12px;
          color: #64748B;
          font-family: monospace;
          line-height: 1.2;
        }
      }
    }

    .role-tags-cell {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .table-action-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .table-action-pill {
      height: 30px;
      padding: 0 14px;
      border-radius: 9999px;
      border: 1px solid transparent;
      font-size: 12px;
      font-weight: 600;
      line-height: 1;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;

      &:disabled {
        opacity: 0.45;
        cursor: not-allowed;
      }

      &--primary {
        background: #eff6ff;
        border-color: #bfdbfe;
        color: #2563eb;

        &:hover:not(:disabled) {
          background: #2563eb;
          border-color: #2563eb;
          color: #ffffff;
        }
      }

      &--warning {
        background: #fff7ed;
        border-color: #fed7aa;
        color: #ea580c;

        &:hover:not(:disabled) {
          background: #ea580c;
          border-color: #ea580c;
          color: #ffffff;
        }
      }

      &--success {
        background: #ecfdf5;
        border-color: #a7f3d0;
        color: #059669;

        &:hover:not(:disabled) {
          background: #059669;
          border-color: #059669;
          color: #ffffff;
        }
      }

      &--danger {
        background: #fef2f2;
        border-color: #fecaca;
        color: #dc2626;

        &:hover:not(:disabled) {
          background: #dc2626;
          border-color: #dc2626;
          color: #ffffff;
        }
      }
    }
  }
}
</style>
