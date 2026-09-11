<template>
  <div class="user-list-container">
    <!-- 顶部操作头区 -->
    <div class="user-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <span class="header-icon">👥</span>
          <h1 class="main-title">系统用户与角色权限中心</h1>
          <span class="capsule-count-tag">共 {{ filteredUsers.length }} 位注册用户</span>
        </div>
        <p class="sub-desc">
          统一维护全校教职工、教学督导与修读学生的基础身份信息、角色绑定与账号活跃状态。
        </p>
      </div>

      <div class="header-right-actions">
        <el-button
          type="primary"
          class="capsule-btn-primary"
          @click="showCreateDialog = true"
        >
          <el-icon class="mr-1"><Plus /></el-icon> 新增系统用户
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
        />
        <el-select v-model="selectedRole" placeholder="系统角色" clearable class="filter-select">
          <el-option label="全部角色" value="" />
          <el-option label="系统管理员" value="ADMIN" />
          <el-option label="任课教师" value="TEACHER" />
          <el-option label="在读学生" value="STUDENT" />
        </el-select>
        <el-select v-model="selectedStatus" placeholder="账号状态" clearable class="filter-select">
          <el-option label="全部状态" :value="null" />
          <el-option label="正常活跃" value="ENABLE" />
          <el-option label="冻结停用" value="DISABLED" />
        </el-select>
      </div>
    </div>

    <!-- 用户列表表格卡片 -->
    <div v-loading="loading" class="user-table-card">
      <el-table :data="filteredUsers" stripe class="main-table">
        <el-table-column label="用户名/账号" prop="username" width="160">
          <template #default="{ row }">
            <span class="font-mono font-semibold text-slate-800">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column label="真实姓名" prop="realName" width="140">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="avatar-dot"></span>
              <span class="font-medium text-slate-800">{{ row.realName }}</span>
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

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="router.push(`/system/users/${row.id}`)"
            >
              档案与权限
            </el-button>
            <el-button
              :type="isUserEnabled(row) ? 'danger' : 'success'"
              link
              size="small"
              @click="toggleUserStatus(row)"
            >
              {{ isUserEnabled(row) ? '冻结' : '解冻' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
import { getUsers, createUser, updateUserStatus } from '@/api/system/user';
import { getRoles } from '@/api/system/role';
import { USE_MOCK } from '@/config/mock';

const router = useRouter();
const loading = ref(false);
const creating = ref(false);
const showCreateDialog = ref(false);
const dialogFormRef = ref<FormInstance>();

const searchKeyword = ref('');
const selectedRole = ref('');
const selectedStatus = ref('');

const users = ref<any[]>([]);
const roles = ref<Array<{ id: number; roleCode: string }>>([]);

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
    const res = await getUsers({ page: 1, pageSize: 50 });
    users.value = res.data?.list || [];
  } catch {
    users.value = USE_MOCK ? getDefaultUsers() : [];
  } finally {
    loading.value = false;
  }
}

// 严守默认用户名规范：admin, teacher, student, student2 (无 01 后缀)
function getDefaultUsers() {
  return [
    {
      id: 1,
      username: 'admin',
      realName: '系统超级管理员',
      department: '数字化与信息化中心',
      roles: ['ADMIN'],
      status: 'ENABLE'
    },
    {
      id: 2,
      username: 'teacher',
      realName: '李华教授',
      department: '计算机学院 · 软件工程系',
      roles: ['TEACHER'],
      status: 'ENABLE'
    },
    {
      id: 3,
      username: 'student',
      realName: '张子轩',
      department: '计算机学院 · 2024级软工1班',
      roles: ['STUDENT'],
      status: 'ENABLE'
    },
    {
      id: 4,
      username: 'student2',
      realName: '李梦琪',
      department: '计算机学院 · 2024级软工1班',
      roles: ['STUDENT'],
      status: 'ENABLE'
    }
  ];
}

const filteredUsers = computed(() => {
  return users.value.filter(u => {
    if (selectedRole.value && !u.roles?.includes(selectedRole.value)) return false;
    if (selectedStatus.value && u.status !== selectedStatus.value) return false;
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const inUsername = u.username?.toLowerCase().includes(kw);
      const inRealName = u.realName?.toLowerCase().includes(kw);
      if (!inUsername && !inRealName) return false;
    }
    return true;
  });
});

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

async function toggleUserStatus(row: any) {
  const newStatus = isUserEnabled(row) ? 'DISABLED' : 'ENABLE';
  try {
    await updateUserStatus(row.id, newStatus);
    row.status = newStatus;
    ElMessage.success(`已${newStatus === 'ENABLE' ? '启用' : '冻结'}用户：${row.username}`);
  } catch {
    ElMessage.error('更新用户状态失败，请稍后重试');
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
  min-height: calc(100vh - 64px);

  .user-header-dock {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 24px;

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 28px;
        }

        .main-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .capsule-count-tag {
          font-size: 12px;
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
          border-radius: 9999px;
          padding: 2px 10px;
          font-weight: 500;
        }
      }

      .sub-desc {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .header-right-actions {
      .capsule-btn-primary {
        background: #2563eb;
        border-color: #2563eb;
        border-radius: 8px;
        font-weight: 500;
        padding: 9px 20px;
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

    .name-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .avatar-dot {
        width: 8px;
        height: 8px;
        background: #3b82f6;
        border-radius: 50%;
      }
    }

    .role-tags-cell {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
  }
}
</style>
