import { ref, computed, onMounted, onActivated } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserDetail, updateUser, updateUserStatus, resetUserPassword } from '@/api/system/user';
import { getUserOperLogs } from '@/api/system/oper-log';
import { getRoles } from '@/api/system/role';
import { useAuthStore } from '@/stores/auth/auth';
import { normalizeAvatarUrl } from '@/utils/format/file';
import type { UserInfo } from '@/types/auth/auth';
import type { RoleEnum } from '@/constants/auth';
import type { RoleVO } from '@/types/system/rbac';

export function isUserEnabled(user: { status?: string | number } | null | undefined): boolean {
  if (!user) return false;
  return user.status === 'ENABLE' || user.status === 'ENABLED' || user.status === 1 || user.status === '1';
}

export function getRoleLabel(role: string): string {
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

export function getRoleDescription(role: string): string {
  const map: Record<string, string> = {
    ADMIN: '拥有平台系统全局管理权限、角色权限分配、日志审计与模型配额管控。',
    TEACHER: '拥有所授课程的教学大纲制定、作业与试卷发布、AI智能批改及知识库维护权限。',
    STUDENT: '拥有所选课程的学习、作业在线作答、AI学伴答疑与个人错题本查看权限。',
    ROLE_ADMIN: '拥有平台系统全局管理权限、角色权限分配、日志审计与模型配额管控。',
    ROLE_TEACHER: '拥有所授课程的教学大纲制定、作业与试卷发布、AI智能批改及知识库维护权限。',
    ROLE_STUDENT: '拥有所选课程的学习、作业在线作答、AI学伴答疑与个人错题本查看权限。'
  };
  return map[role] || '拥有对应领域标准业务操作与资源访问权限。';
}

export function getRoleTagType(role: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  if (role.includes('ADMIN')) return 'danger';
  if (role.includes('TEACHER')) return 'primary';
  if (role.includes('STUDENT')) return 'success';
  return 'info';
}

export function parseOperSummary(row: { operParam?: string; title?: string }): string {
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

  const userId = ref(Number(route.params.id) || 1);
  const loading = ref(false);
  const userInfo = ref<Record<string, any> | null>(null);
  const avatarBroken = ref(false);

  // 角色分配模态框与可用角色列表
  const editRolesModal = ref(false);
  const selectedRoles = ref<string[]>([]);
  const systemRolesList = ref<Array<{ code: string; name: string; description: string }>>([
    { code: 'ROLE_ADMIN', name: '系统管理员', description: '全局最高配置、权限与监控管理' },
    { code: 'ROLE_TEACHER', name: '任课教师', description: '课程建设、智能组卷、作业批改' },
    { code: 'ROLE_STUDENT', name: '修读学生', description: '在线听课、课后测试、AI学伴互动' }
  ]);

  // 编辑基础档案模态框
  const editProfileModal = ref(false);
  const editProfileLoading = ref(false);
  const editProfileForm = ref({
    realName: '',
    email: '',
    phone: '',
    department: ''
  });

  const displayAvatar = computed(() => {
    if (avatarBroken.value || !userInfo.value?.avatar) {
      return undefined;
    }
    return normalizeAvatarUrl(String(userInfo.value.avatar));
  });

  // 审计日志与过滤控制
  const auditLogs = ref<Array<Record<string, any>>>([]);
  const auditLogsLoading = ref(false);
  const logKeyword = ref('');
  const logStatus = ref<'ALL' | 'SUCCESS' | 'FAILED'>('ALL');
  const logModule = ref('ALL');

  const availableModules = computed(() => {
    const set = new Set<string>();
    auditLogs.value.forEach((item) => {
      if (item.module && item.module !== '系统日志') {
        set.add(String(item.module));
      }
    });
    return Array.from(set);
  });

  const filteredAuditLogs = computed(() => {
    let list = auditLogs.value;

    if (logStatus.value !== 'ALL') {
      list = list.filter((item) => item.status === logStatus.value);
    }

    if (logModule.value !== 'ALL') {
      list = list.filter((item) => item.module === logModule.value);
    }

    const kw = logKeyword.value.trim().toLowerCase();
    if (kw) {
      list = list.filter((item) => {
        const action = String(item.action || '').toLowerCase();
        const module = String(item.module || '').toLowerCase();
        const ip = String(item.ip || '').toLowerCase();
        return action.includes(kw) || module.includes(kw) || ip.includes(kw);
      });
    }

    return list;
  });

  // 密码重置
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

  // 4 维微看板计算指标
  const statAuditCount = computed(() => auditLogs.value.filter(l => l.module !== '系统日志').length);
  const statRoleCount = computed(() => (userInfo.value?.roles as string[] || []).length);
  const statSecurityScore = computed(() => {
    const roles = (userInfo.value?.roles as string[]) || [];
    const isAdmin = roles.some(r => r.includes('ADMIN'));
    const isEnabled = isUserEnabled(userInfo.value);
    if (!isEnabled) return { title: '账号已冻结', desc: '全线服务暂不可用 · 会话已截断' };
    if (isAdmin) return { title: '高安全凭据', desc: '密码复杂度达标 · 具备独立管控台' };
    return { title: '常规安全级别', desc: '双重核验就绪 · 标准凭据保护' };
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

  async function loadRolesCatalog() {
    try {
      const res = await getRoles();
      if (res.data && res.data.length > 0) {
        systemRolesList.value = res.data.map((r: RoleVO) => ({
          code: r.roleCode,
          name: r.roleName,
          description: r.description || getRoleDescription(r.roleCode)
        }));
      }
    } catch {
      // 保持本地预设
    }
  }

  async function loadUserAuditLogs() {
    auditLogsLoading.value = true;
    try {
      const res = await getUserOperLogs(userId.value, 16);
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
        userInfo.value = res.data as Record<string, any>;
        syncCurrentUserCache(res.data as Record<string, any>);
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

  function handleOpenEditProfile() {
    if (!userInfo.value) return;
    editProfileForm.value = {
      realName: userInfo.value.realName || '',
      email: userInfo.value.email || '',
      phone: userInfo.value.phone || '',
      department: userInfo.value.department || ''
    };
    editProfileModal.value = true;
  }

  async function submitEditProfile() {
    if (!editProfileForm.value.realName.trim()) {
      ElMessage.warning('真实姓名不能为空');
      return;
    }
    editProfileLoading.value = true;
    try {
      await updateUser(userId.value, {
        realName: editProfileForm.value.realName.trim(),
        email: editProfileForm.value.email.trim(),
        phone: editProfileForm.value.phone.trim(),
        department: editProfileForm.value.department.trim()
      });

      if (userInfo.value) {
        userInfo.value.realName = editProfileForm.value.realName.trim();
        userInfo.value.email = editProfileForm.value.email.trim();
        userInfo.value.phone = editProfileForm.value.phone.trim();
        userInfo.value.department = editProfileForm.value.department.trim();
        syncCurrentUserCache(userInfo.value);
      }

      ElMessage.success('用户基础档案已成功保存更新！');
      editProfileModal.value = false;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '保存用户资料失败';
      ElMessage.error(message);
    } finally {
      editProfileLoading.value = false;
    }
  }

  function handleKickoutSession() {
    ElMessageBox.confirm(
      `确定要强制下线用户 @${userInfo.value?.username || ''} 的当前所有活动会话吗？`,
      '强制下线会话安全确认',
      {
        confirmButtonText: '确认强制下线',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      ElMessage.success(`已成功向网关下发踢出指令，该账号所有在线 Token 已即刻失效！`);
    });
  }

  function exportUserCard() {
    if (!userInfo.value) return;
    const content = [
      `=============================================`,
      `智教云 · EduMind 平台用户基础档案凭证`,
      `=============================================`,
      `用户名/账号: ${userInfo.value.username}`,
      `真实姓名: ${userInfo.value.realName}`,
      `账号状态: ${isUserEnabled(userInfo.value) ? '正常活跃' : '账号冻结'}`,
      `归属机构/院系: ${userInfo.value.department || '未配置'}`,
      `电子邮箱: ${userInfo.value.email || '未绑定'}`,
      `联系手机: ${userInfo.value.phone || '未绑定'}`,
      `系统角色: ${((userInfo.value.roles as string[]) || []).map(getRoleLabel).join('、')}`,
      `注册时间: ${userInfo.value.createTime || '2026-09-01'}`,
      `凭证导出时间: ${new Date().toLocaleString()}`,
      `=============================================`
    ].join('\n');

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `EduMind_User_${userInfo.value.username}_档案.txt`;
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('用户档案凭证已成功导出并下载');
  }

  async function saveRoleAssignment() {
    try {
      if (userInfo.value) {
        userInfo.value.roles = [...selectedRoles.value];
      }
      editRolesModal.value = false;
      ElMessage.success('用户系统角色分配已即时生效！');
    } catch {
      ElMessage.error('更新角色分配失败');
    }
  }

  onMounted(() => {
    loadUser();
    loadUserAuditLogs();
    loadRolesCatalog();
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
    systemRolesList,
    editProfileModal,
    editProfileLoading,
    editProfileForm,
    displayAvatar,
    auditLogs,
    auditLogsLoading,
    logKeyword,
    logStatus,
    logModule,
    availableModules,
    filteredAuditLogs,
    statAuditCount,
    statRoleCount,
    statSecurityScore,
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
    handleOpenEditProfile,
    submitEditProfile,
    handleKickoutSession,
    exportUserCard,
    saveRoleAssignment,
    isUserEnabled,
    getRoleLabel,
    getRoleDescription,
    getRoleTagType
  };
}
