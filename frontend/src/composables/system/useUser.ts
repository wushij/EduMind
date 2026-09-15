import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { getUsers, createUser, updateUserStatus, deleteUser } from '@/api/system/user';
import { getRoles } from '@/api/system/role';
import { USE_MOCK } from '@/config/mock';
import { useAuthStore } from '@/stores/auth/auth';

export interface SystemUserRow {
  id: number;
  username: string;
  realName?: string;
  avatar?: string;
  department?: string;
  roles?: string[];
  status?: string;
}

export interface RoleOption {
  id: number;
  roleCode: string;
}

export function getDefaultUsers(): SystemUserRow[] {
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

export function getRoleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[role] || role;
}

export function getRoleTagType(role: string) {
  const map: Record<string, string> = {
    ADMIN: 'danger',
    TEACHER: 'primary',
    STUDENT: 'success'
  };
  return map[role] || 'info';
}

export function isUserEnabled(row: { status?: string }) {
  return row.status === 'ENABLE' || row.status === 'ENABLED';
}

export function canDeleteUser(
  row: { id: number; username: string },
  currentUserId?: number
) {
  if (row.username === 'admin') return false;
  return row.id !== currentUserId;
}

export function useUser() {
  const authStore = useAuthStore();
  const loading = ref(false);
  const creating = ref(false);
  const showCreateDialog = ref(false);
  const dialogFormRef = ref<FormInstance>();

  const searchKeyword = ref('');
  const selectedRole = ref('');
  const selectedStatus = ref('');

  const users = ref<SystemUserRow[]>([]);
  const roles = ref<RoleOption[]>([]);
  const pageNum = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const avatarBroken = ref<Record<number, boolean>>({});

  const newUserForm = reactive({
    username: '',
    realName: '',
    role: 'TEACHER',
    password: '123456'
  });

  const dialogRules = reactive<FormRules>({
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }]
  });

  function onAvatarError(userId: number) {
    avatarBroken.value[userId] = true;
  }

  async function loadRoles() {
    try {
      const res = await getRoles();
      roles.value = (res.data || []).map((item) => ({
        id: item.id,
        roleCode: item.roleCode
      }));
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
      const [res] = await Promise.all([
        getUsers({
          page: pageNum.value,
          pageSize: pageSize.value,
          keyword: searchKeyword.value.trim() || undefined,
          role: selectedRole.value || undefined,
          status: selectedStatus.value || undefined
        }),
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);
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

  function handleReset() {
    searchKeyword.value = '';
    selectedRole.value = '';
    selectedStatus.value = '';
    pageNum.value = 1;
    loadUsers();
  }

  function canDeleteCurrentUser(row: { id: number; username: string }) {
    return canDeleteUser(row, authStore.currentUser?.id);
  }

  async function toggleUserStatus(row: SystemUserRow) {
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
    } catch (err: unknown) {
      if (err !== 'cancel' && err !== 'close') {
        ElMessage.error('更新用户状态失败，请稍后重试');
      }
    }
  }

  async function handleDeleteUser(row: SystemUserRow) {
    if (!canDeleteCurrentUser(row)) {
      ElMessage.warning(
        row.username === 'admin' ? '系统内置管理员账号不允许删除' : '不能删除当前登录账号'
      );
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
    } catch (err: unknown) {
      if (err !== 'cancel' && err !== 'close') {
        const message = err instanceof Error ? err.message : '删除用户失败，请稍后重试';
        ElMessage.error(message);
      }
    }
  }

  async function handleCreateUser() {
    if (!dialogFormRef.value) return;
    await dialogFormRef.value.validate(async (valid) => {
      if (!valid) return;
      creating.value = true;
      try {
        const role = roles.value.find((item) => item.roleCode === newUserForm.role);
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

  onMounted(async () => {
    await Promise.all([loadRoles(), loadUsers()]);
  });

  return {
    loading,
    creating,
    showCreateDialog,
    dialogFormRef,
    searchKeyword,
    selectedRole,
    selectedStatus,
    users,
    roles,
    pageNum,
    pageSize,
    total,
    avatarBroken,
    newUserForm,
    dialogRules,
    onAvatarError,
    loadUsers,
    handleSearch,
    handleReset,
    getRoleLabel,
    getRoleTagType,
    isUserEnabled,
    canDeleteUser: canDeleteCurrentUser,
    toggleUserStatus,
    handleDeleteUser,
    handleCreateUser
  };
}
