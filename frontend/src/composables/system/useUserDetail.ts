import { ref, computed, onMounted, onActivated } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserDetail, updateUserStatus, resetUserPassword } from '@/api/system/user';
import { getUserOperLogs } from '@/api/system/oper-log';
import { useAuthStore } from '@/stores/auth/auth';
import { normalizeAvatarUrl } from '@/utils/format/file';
import type { UserInfo } from '@/types/auth/auth';
import type { RoleEnum } from '@/constants/auth';

export function isUserEnabled(user: { status?: string | number } | null) {
  return user?.status === 'ENABLE' || user?.status === 'ENABLED' || user?.status === 1 || user?.status === '1';
}

export function getRoleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '系统管理员',
    TEACHER: '任课教师',
    STUDENT: '在读学生',
    ROLE_ADMIN: '系统管理员',
    ROLE_TEACHER: '任课教师',
    ROLE_STUDENT: '在读学生'
  };
  return map[role] || role;
}

export function getRoleDescription(role: string) {
  const map: Record<string, string> = {
    ADMIN: '拥有平台系统全局管理权限、角色权限分配、日志审计与模型配额管控。',
    TEACHER: '拥有所授课程的教学大纲制定、作业与试卷发布、AI智能批改及知识库维护权限。',
    STUDENT: '拥有所选课程的学习、作业在线作答、AI学伴答疑与个人错题本查看权限。',
    ROLE_ADMIN: '拥有平台系统全局管理权限、角色权限分配、日志审计与模型配额管控。',
    ROLE_TEACHER: '拥有所授课程的教学大纲制定、作业与试卷发布、AI智能批改及知识库维护权限。',
    ROLE_STUDENT: '拥有所选课程的学习、作业在线作答、AI学伴答疑与个人错题本查看权限。'
  };
  return map[role] || '系统常规授权';
}

export function parseOperSummary(row: { operParam?: string; title?: string }) {
  if (!row.operParam) {
    return row.title || '常规系统操作';
  }
  try {
    const obj = JSON.parse(row.operParam);
    if (obj.action) return String(obj.action);
    const p = obj.params || obj;
    if (p && typeof p === 'object') {
      const name = p.username || p.realName || p.title || p.name || '';
      if (name) return `${row.title || '操作'}「${name}」`;
    }
  } catch {
    /* ignore parse errors */
  }
  return row.title || '常规系统操作';
}

export function useUserDetail() {
  const route = useRoute();
  const authStore = useAuthStore();

  const userId = ref(Number(route.params.id) || 2);
  const loading = ref(false);
  const userInfo = ref<Record<string, unknown> | null>(null);
  const avatarBroken = ref(false);
  const editRolesModal = ref(false);
  const selectedRoles = ref<string[]>([]);

  const displayAvatar = computed(() => {
    if (avatarBroken.value || !userInfo.value?.avatar) {
      return undefined;
    }
    return normalizeAvatarUrl(String(userInfo.value.avatar));
  });

  const auditLogs = ref<Array<Record<string, unknown>>>([]);
  const auditLogsLoading = ref(false);

  const resetPasswordDialogVisible = ref(false);
  const resetPasswordLoading = ref(false);
  const resetPasswordForm = ref({
    newPassword: '123456',
    confirmPassword: '123456'
  });

  const passwordStrengthScore = computed(() => {
    const pwd = resetPasswordForm.value.newPassword || '';
    if (!pwd) return 0;
    let score = 0;
    if (pwd.length >= 6) score++;
    if (/[a-zA-Z]/.test(pwd) && /\d/.test(pwd)) score++;
    if (pwd.length >= 9 || /[^a-zA-Z0-9]/.test(pwd)) score++;
    return score;
  });

  const passwordStrengthLevel = computed(() => {
    const score = passwordStrengthScore.value;
    if (score <= 1) return { text: '弱 (基础凭据)', type: 'weak' };
    if (score === 2) return { text: '中 (常规安全)', type: 'medium' };
    return { text: '强 (高安全)', type: 'strong' };
  });

  const isPasswordMismatch = computed(() => {
    const { newPassword, confirmPassword } = resetPasswordForm.value;
    return Boolean(confirmPassword && newPassword !== confirmPassword);
  });

  const isSubmitValid = computed(() => {
    const { newPassword, confirmPassword } = resetPasswordForm.value;
    return newPassword.length >= 6 && newPassword === confirmPassword;
  });

  function syncCurrentUserCache(detail: Record<string, unknown>) {
    if (!detail?.id || detail.id !== authStore.currentUser?.id) {
      return;
    }
    authStore.setUser({
      id: detail.id as number,
      username: detail.username as string,
      realName: (detail.realName as string) || (detail.username as string),
      avatar: (detail.avatar as string) || '',
      roles: (detail.roles || []) as RoleEnum[],
      permissions: authStore.currentUser?.permissions || [],
      department: detail.department as string,
      email: detail.email as string
    } as UserInfo);
  }

  async function loadUserAuditLogs() {
    auditLogsLoading.value = true;
    try {
      const res = await getUserOperLogs(userId.value, 8);
      if (res.data && res.data.length > 0) {
        auditLogs.value = res.data.map((item) => ({
          time: item.operTime,
          module: item.title || '系统管理',
          action: parseOperSummary(item),
          ip: item.operIp || '127.0.0.1',
          costTime: item.costTime || 0,
          status: item.status === 0 ? 'SUCCESS' : 'FAILED'
        }));
      } else {
        auditLogs.value = [
          {
            time: '近期暂无',
            module: '系统日志',
            action: '该账号近期尚无敏感写操作或管理记录',
            ip: '127.0.0.1',
            costTime: 0,
            status: 'SUCCESS'
          }
        ];
      }
    } catch {
      auditLogs.value = [];
    } finally {
      auditLogsLoading.value = false;
    }
  }

  async function loadUser() {
    loading.value = true;
    avatarBroken.value = false;
    try {
      const res = await getUserDetail(userId.value);
      if (res.data) {
        userInfo.value = res.data as Record<string, unknown>;
        syncCurrentUserCache(res.data as Record<string, unknown>);
      } else {
        throw new Error('未找到该用户信息');
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '获取用户详情失败';
      ElMessage.error(message);
      userInfo.value = null;
    } finally {
      selectedRoles.value = [...((userInfo.value?.roles as string[]) || [])];
      loading.value = false;
    }
  }

  function toggleUserStatus() {
    const enabling = !isUserEnabled(userInfo.value);
    const actionText = enabling ? '解冻恢复该用户账号' : '冻结该用户账号';

    ElMessageBox.confirm(`确定要${actionText}吗？`, '账号状态变更确认', {
      confirmButtonText: '确定变更',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await updateUserStatus(userId.value, enabling ? 'ENABLE' : 'DISABLE');
        if (userInfo.value) {
          userInfo.value.status = enabling ? 'ENABLE' : 'DISABLE';
        }
        ElMessage.success(`用户状态已成功变更为：${enabling ? '正常活跃' : '账号冻结'}`);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : '用户状态更新失败';
        ElMessage.error(message);
      }
    });
  }

  function handleResetPassword() {
    resetPasswordForm.value = {
      newPassword: '123456',
      confirmPassword: '123456'
    };
    resetPasswordDialogVisible.value = true;
  }

  function applyPresetPassword(pwd: string) {
    resetPasswordForm.value.newPassword = pwd;
    resetPasswordForm.value.confirmPassword = pwd;
  }

  function generateRandomPassword() {
    const chars = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%&*';
    let res = '';
    for (let i = 0; i < 10; i++) {
      res += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    resetPasswordForm.value.newPassword = res;
    resetPasswordForm.value.confirmPassword = res;
    ElMessage.info('已生成高强度随机密码并自动填入');
  }

  async function submitResetPassword() {
    if (!isSubmitValid.value) {
      ElMessage.warning('请检查输入的密码，长度需至少为 6 位且两次输入一致');
      return;
    }
    resetPasswordLoading.value = true;
    try {
      await resetUserPassword(userId.value, resetPasswordForm.value.newPassword);
      ElMessage.success(`用户 @${userInfo.value?.username || ''} 的登录密码已成功重置！`);
      resetPasswordDialogVisible.value = false;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '重置密码失败';
      ElMessage.error(message);
    } finally {
      resetPasswordLoading.value = false;
    }
  }

  function saveRoleAssignment() {
    if (userInfo.value) {
      userInfo.value.roles = [...selectedRoles.value];
    }
    editRolesModal.value = false;
    ElMessage.success('用户所属角色已成功更新！');
  }

  onMounted(() => {
    loadUser();
    loadUserAuditLogs();
  });

  onActivated(() => {
    loadUser();
    loadUserAuditLogs();
  });

  return {
    userId,
    loading,
    userInfo,
    avatarBroken,
    editRolesModal,
    selectedRoles,
    displayAvatar,
    auditLogs,
    auditLogsLoading,
    resetPasswordDialogVisible,
    resetPasswordLoading,
    resetPasswordForm,
    passwordStrengthScore,
    passwordStrengthLevel,
    isPasswordMismatch,
    isSubmitValid,
    loadUser,
    loadUserAuditLogs,
    toggleUserStatus,
    handleResetPassword,
    applyPresetPassword,
    generateRandomPassword,
    submitResetPassword,
    saveRoleAssignment,
    isUserEnabled,
    getRoleLabel,
    getRoleDescription
  };
}
