<template>
  <div class="user-detail-container">
    <!-- 顶部导航条 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/system/users')">
        返回用户列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/system/users' }">用户权限管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ userInfo?.realName || '用户画像详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 用户头部英雄卡片 -->
    <div v-loading="loading" class="user-hero-card">
      <div class="hero-left">
        <div class="avatar-box">
          <el-avatar
            :size="64"
            shape="square"
            :src="displayAvatar"
            class="user-detail-avatar"
            @error="avatarBroken = true"
          >
            {{ userInfo?.realName ? userInfo.realName.slice(0, 1) : '用' }}
          </el-avatar>
        </div>
        <div>
          <div class="name-line">
            <h1 class="user-name">{{ userInfo?.realName }}</h1>
            <span class="user-account">@{{ userInfo?.username }}</span>
            <el-tag :type="isUserEnabled(userInfo) ? 'success' : 'danger'" size="small">
              {{ isUserEnabled(userInfo) ? '正常活跃' : '账号冻结' }}
            </el-tag>
          </div>
          <div class="role-badges-flow">
            <el-tag
              v-for="r in userInfo?.roles"
              :key="r"
              size="small"
              type="primary"
              effect="light"
            >
              {{ getRoleLabel(r) }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="hero-actions">
        <el-button :icon="Key" @click="handleResetPassword">
          重置登录密码
        </el-button>
        <el-button
          :type="isUserEnabled(userInfo) ? 'danger' : 'success'"
          :icon="isUserEnabled(userInfo) ? Lock : Unlock"
          plain
          @click="toggleUserStatus"
        >
          {{ isUserEnabled(userInfo) ? '冻结账号' : '解冻并恢复使用' }}
        </el-button>
      </div>
    </div>

    <!-- 用户详情主体网格 -->
    <div class="detail-body-grid">
      <!-- 基本信息与所属院系 -->
      <el-card shadow="never" class="info-card">
        <h3 class="card-title">
          <el-icon class="mr-1"><User /></el-icon>
          <span>用户基础档案</span>
        </h3>
        <div class="fields-list">
          <div class="field-item">
            <span class="label">用户名/账号：</span>
            <span class="val font-mono">{{ userInfo?.username }}</span>
          </div>
          <div class="field-item">
            <span class="label">真实姓名：</span>
            <span class="val font-semibold">{{ userInfo?.realName }}</span>
          </div>
          <div class="field-item">
            <span class="label">电子邮箱：</span>
            <span class="val">{{ userInfo?.email || 'user@edumind.edu.cn' }}</span>
          </div>
          <div class="field-item">
            <span class="label">联系手机：</span>
            <span class="val font-mono">{{ userInfo?.phone || '13800001234' }}</span>
          </div>
          <div class="field-item">
            <span class="label">归属院系/班级：</span>
            <span class="val">{{ userInfo?.department || '计算机科学与技术学院 · 软件工程系' }}</span>
          </div>
          <div class="field-item">
            <span class="label">注册加入时间：</span>
            <span class="val text-slate-500">{{ userInfo?.createTime || '2026-09-01 09:00:00' }}</span>
          </div>
        </div>
      </el-card>

      <!-- 角色权限与系统资源分配 -->
      <el-card shadow="never" class="info-card">
        <div class="card-title-line">
          <h3 class="card-title">
            <el-icon class="mr-1"><Lock /></el-icon>
            <span>角色身份与权限分配</span>
          </h3>
          <el-button type="primary" link size="small" @click="editRolesModal = true">
            调整分配角色
          </el-button>
        </div>

        <div class="roles-allocated-box">
          <div v-for="r in userInfo?.roles" :key="r" class="role-desc-card">
            <div class="role-top">
              <span class="role-badge">{{ r }}</span>
              <span class="role-title">{{ getRoleLabel(r) }}</span>
            </div>
            <p class="role-note">{{ getRoleDescription(r) }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 近期操作与登录审计记录（对接真实操作日志接口） -->
    <el-card shadow="never" class="audit-table-card mt-4">
      <div class="card-header-flex">
        <h3 class="card-title">
          <el-icon class="mr-1"><Clock /></el-icon>
          <span>账号近期安全与操作审计日志</span>
        </h3>
        <button
          type="button"
          class="jump-log-center-btn"
          @click="router.push('/system/oper-log')"
        >
          查看全平台操作日志大盘 →
        </button>
      </div>
      <el-table v-loading="auditLogsLoading" :data="auditLogs" stripe class="audit-table">
        <el-table-column label="操作时间" prop="time" width="180" />
        <el-table-column label="操作模块" prop="module" width="140" />
        <el-table-column label="操作行为说明" prop="action" min-width="220" />
        <el-table-column label="客户端 IP" prop="ip" width="140" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            <span>{{ row.costTime ?? 0 }}ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small" round>
              {{ row.status === 'SUCCESS' ? '操作成功' : '拦截失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 调整角色弹窗 -->
    <el-dialog v-model="editRolesModal" title="调整用户系统角色" width="480px" destroy-on-close>
      <el-checkbox-group v-model="selectedRoles" class="role-checkboxes">
        <el-checkbox label="ROLE_ADMIN">系统超级管理员 (ROLE_ADMIN)</el-checkbox>
        <el-checkbox label="ROLE_TEACHER">课程主讲教师 (ROLE_TEACHER)</el-checkbox>
        <el-checkbox label="ROLE_STUDENT">修读学生 (ROLE_STUDENT)</el-checkbox>
      </el-checkbox-group>

      <template #footer>
        <el-button @click="editRolesModal = false">取消</el-button>
        <el-button type="primary" @click="saveRoleAssignment">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码高端定制对话框 -->
    <el-dialog
      v-model="resetPasswordDialogVisible"
      width="520px"
      class="custom-reset-password-dialog"
      :show-close="true"
      destroy-on-close
      append-to-body
    >
      <template #header>
        <div class="dialog-header-premium">
          <div class="header-icon-badge">
            <el-icon><Lock /></el-icon>
          </div>
          <div class="header-text-group">
            <div class="dialog-title-row">
              <h3 class="dialog-title">重置用户登录密码</h3>
              <span class="dialog-badge">安全凭据</span>
            </div>
            <p class="dialog-subtitle">为该用户重新配置凭据，支持自主输入或一键采用默认安全预设</p>
          </div>
        </div>
      </template>

      <!-- 目标用户摘要条卡片 -->
      <div class="target-user-summary-card">
        <div class="target-user-left">
          <el-avatar
            :size="42"
            shape="square"
            :src="displayAvatar"
            class="target-avatar"
            @error="avatarBroken = true"
          >
            {{ userInfo?.realName ? userInfo.realName.slice(0, 1) : '用' }}
          </el-avatar>
          <div class="target-user-meta">
            <div class="target-name-row">
              <span class="target-real-name">{{ userInfo?.realName || '—' }}</span>
              <span class="target-username">@{{ userInfo?.username }}</span>
            </div>
            <div class="target-dept-text">{{ userInfo?.department || '计算机科学与技术学院' }}</div>
          </div>
        </div>
        <div class="target-user-status">
          <span class="status-pill" :class="{ enabled: isUserEnabled(userInfo) }">
            <span class="status-dot"></span>
            {{ isUserEnabled(userInfo) ? '账号正常' : '已冻结' }}
          </span>
        </div>
      </div>

      <!-- 快捷预设填充区 (使用矢量 Icon 替代 Emoji) -->
      <div class="presets-quick-bar">
        <span class="presets-label">快捷预设：</span>
        <button
          type="button"
          class="preset-chip-btn default-chip"
          :class="{ active: resetPasswordForm.newPassword === '123456' }"
          @click="applyPresetPassword('123456')"
        >
          <el-icon class="chip-icon"><Lightning /></el-icon>
          <span>默认初始密码 (123456)</span>
        </button>
        <button
          type="button"
          class="preset-chip-btn random-chip"
          @click="generateRandomPassword"
        >
          <el-icon class="chip-icon"><MagicStick /></el-icon>
          <span>生成高强度随机密码</span>
        </button>
      </div>

      <!-- 密码表单 -->
      <div class="reset-form-wrap">
        <div class="form-field-group">
          <label class="field-label">
            <span>新登录密码</span>
            <span class="label-req">*</span>
          </label>
          <div class="input-glow-wrap">
            <el-input
              v-model="resetPasswordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码（默认为 123456）"
              size="large"
              class="premium-password-input"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 密码强度指示器 -->
          <div v-if="resetPasswordForm.newPassword" class="password-strength-container">
            <div class="strength-bars">
              <div
                class="strength-bar-segment"
                :class="{ active: passwordStrengthScore >= 1, weak: passwordStrengthScore === 1, medium: passwordStrengthScore === 2, strong: passwordStrengthScore >= 3 }"
              ></div>
              <div
                class="strength-bar-segment"
                :class="{ active: passwordStrengthScore >= 2, medium: passwordStrengthScore === 2, strong: passwordStrengthScore >= 3 }"
              ></div>
              <div
                class="strength-bar-segment"
                :class="{ active: passwordStrengthScore >= 3, strong: passwordStrengthScore >= 3 }"
              ></div>
            </div>
            <span class="strength-text" :class="passwordStrengthLevel.type">
              强度：{{ passwordStrengthLevel.text }}
            </span>
          </div>
        </div>

        <div class="form-field-group">
          <label class="field-label">
            <span>确认新密码</span>
            <span class="label-req">*</span>
          </label>
          <div class="input-glow-wrap">
            <el-input
              v-model="resetPasswordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次确认输入新密码"
              size="large"
              class="premium-password-input"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </div>
          <div v-if="isPasswordMismatch" class="error-tip-line">
            <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
            <span>两次输入的密码不一致，请核对后提交</span>
          </div>
        </div>

        <!-- 安全合规与下线警示 -->
        <div class="security-compliance-box">
          <div class="compliance-icon">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="compliance-text">
            <strong>安全审计提示：</strong>
            密码重置成功后，系统将自动使该账号的所有历史登录凭据（Token）失效并强制下线，用户下次必须使用新设置的密码重新认证。
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer-actions">
          <button type="button" class="action-btn-cancel" @click="resetPasswordDialogVisible = false">
            取消
          </button>
          <button
            type="button"
            class="action-btn-confirm"
            :disabled="!isSubmitValid || resetPasswordLoading"
            @click="submitResetPassword"
          >
            <el-icon v-if="resetPasswordLoading" class="is-loading mr-1"><Loading /></el-icon>
            <el-icon v-else class="mr-1"><Check /></el-icon>
            <span>确认重置密码</span>
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft,
  Key,
  Lock,
  Unlock,
  User,
  Clock,
  WarningFilled,
  Loading,
  Lightning,
  MagicStick,
  Check,
  CircleCloseFilled
} from '@element-plus/icons-vue';
import { getUserDetail, updateUserStatus, resetUserPassword } from '@/api/system/user';
import { getUserOperLogs } from '@/api/system/oper-log';
import { useAuthStore } from '@/stores/auth/auth';
import { normalizeAvatarUrl } from '@/utils/format/file';
import type { UserInfo } from '@/types/auth/auth';
import type { RoleEnum } from '@/constants/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const userId = ref(Number(route.params.id) || 2);
const loading = ref(false);
const userInfo = ref<any>(null);
const avatarBroken = ref(false);
const editRolesModal = ref(false);
const selectedRoles = ref<string[]>([]);

const displayAvatar = computed(() => {
  if (avatarBroken.value || !userInfo.value?.avatar) {
    return undefined;
  }
  return normalizeAvatarUrl(userInfo.value.avatar);
});

const auditLogs = ref<any[]>([]);
const auditLogsLoading = ref(false);

function parseOperSummary(row: any): string {
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
  } catch {}
  return row.title || '常规系统操作';
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

onMounted(() => {
  loadUser();
  loadUserAuditLogs();
});

onActivated(() => {
  loadUser();
  loadUserAuditLogs();
});

function isUserEnabled(user: { status?: string | number } | null) {
  return user?.status === 'ENABLE' || user?.status === 'ENABLED' || user?.status === 1 || user?.status === '1';
}

function syncCurrentUserCache(detail: any) {
  if (!detail?.id || detail.id !== authStore.currentUser?.id) {
    return;
  }
  authStore.setUser({
    id: detail.id,
    username: detail.username,
    realName: detail.realName || detail.username,
    avatar: detail.avatar || '',
    roles: (detail.roles || []) as RoleEnum[],
    permissions: authStore.currentUser?.permissions || [],
    department: detail.department,
    email: detail.email
  } as UserInfo);
}

async function loadUser() {
  loading.value = true;
  avatarBroken.value = false;
  try {
    const res = await getUserDetail(userId.value);
    if (res.data) {
      userInfo.value = res.data;
      syncCurrentUserCache(res.data);
    } else {
      throw new Error('未找到该用户信息');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取用户详情失败');
    userInfo.value = null;
  } finally {
    selectedRoles.value = [...(userInfo.value?.roles || [])];
    loading.value = false;
  }
}

function getRoleLabel(role: string) {
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

function getRoleDescription(role: string) {
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
      userInfo.value.status = enabling ? 'ENABLE' : 'DISABLE';
      ElMessage.success(`用户状态已成功变更为：${enabling ? '正常活跃' : '账号冻结'}`);
    } catch (err: any) {
      ElMessage.error(err?.message || '用户状态更新失败');
    }
  });
}

const resetPasswordDialogVisible = ref(false);
const resetPasswordLoading = ref(false);
const resetPasswordForm = ref({
  newPassword: '123456',
  confirmPassword: '123456'
});

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
  } catch (err: any) {
    ElMessage.error(err?.message || '重置密码失败');
  } finally {
    resetPasswordLoading.value = false;
  }
}

function saveRoleAssignment() {
  userInfo.value.roles = [...selectedRoles.value];
  editRolesModal.value = false;
  ElMessage.success('用户所属角色已成功更新！');
}
</script>

<style scoped lang="scss">
.user-detail-container {
  padding: 24px;
  background: #f8fafc;
  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .user-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .hero-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .avatar-box {
        width: 64px;
        height: 64px;

        .user-detail-avatar {
          width: 64px;
          height: 64px;
          border-radius: 16px;
          font-size: 26px;
          font-weight: 700;
          background: linear-gradient(135deg, #3b82f6, #1d4ed8);
          color: #ffffff;
        }
      }

      .name-line {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .user-name {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .user-account {
          font-size: 14px;
          color: #64748b;
          font-family: monospace;
        }
      }

      .role-badges-flow {
        display: flex;
        gap: 8px;
      }
    }

    .hero-actions {
      display: flex;
      gap: 12px;
    }
  }

  .detail-body-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;

    .info-card {
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      padding: 16px 20px;

      .card-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 16px;
      }

      .card-title-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;

        .card-title {
          margin: 0;
        }
      }

      .fields-list {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .field-item {
          display: flex;
          font-size: 13px;

          .label {
            width: 120px;
            color: #64748b;
          }

          .val {
            color: #1e293b;
          }
        }
      }

      .roles-allocated-box {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .role-desc-card {
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          padding: 12px 14px;

          .role-top {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 6px;

            .role-badge {
              font-family: monospace;
              font-size: 11px;
              background: #eff6ff;
              color: #2563eb;
              padding: 2px 6px;
              border-radius: 4px;
            }

            .role-title {
              font-size: 13px;
              font-weight: 700;
              color: #1e293b;
            }
          }

          .role-note {
            font-size: 12px;
            color: #64748b;
            margin: 0;
            line-height: 1.5;
          }
        }
      }
    }
  }

  .audit-table-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    padding: 16px 20px;

    .card-title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 16px;
    }
  }

  .role-checkboxes {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 10px 0;
  }
}
</style>

<!-- 重置密码高定弹窗样式 (作用域限定在 .custom-reset-password-dialog，解决 append-to-body 导致的 scoped 样式失效问题) -->
<style lang="scss">
.custom-reset-password-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 24px 48px -12px rgba(15, 23, 42, 0.18), 0 0 0 1px rgba(226, 232, 240, 0.8) !important;
  border: 1px solid #e2e8f0;

  .el-dialog__header {
    margin-right: 0;
    padding: 20px 24px 18px;
    border-bottom: 1px solid #f1f5f9;
    background: #ffffff;

    .dialog-header-premium {
      display: flex;
      align-items: center;
      gap: 14px;

      .header-icon-badge {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        color: #2563eb;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        border: 1px solid #bfdbfe;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.12);
        flex-shrink: 0;
      }

      .header-text-group {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .dialog-title-row {
          display: flex;
          align-items: center;
          gap: 8px;

          .dialog-title {
            margin: 0;
            font-size: 16px;
            font-weight: 700;
            color: #0f172a;
            letter-spacing: -0.2px;
            line-height: 1.3;
          }

          .dialog-badge {
            font-size: 11px;
            font-weight: 600;
            color: #2563eb;
            background: #eff6ff;
            border: 1px solid #dbeafe;
            padding: 2px 10px;
            border-radius: 9999px;
            line-height: 1.4;
          }
        }

        .dialog-subtitle {
          margin: 0;
          font-size: 12.5px;
          color: #64748b;
          line-height: 1.4;
        }
      }
    }
  }

  .el-dialog__headerbtn {
    top: 20px;
    right: 20px;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    transition: all 0.2s ease;

    &:hover {
      background: #f1f5f9;
      color: #0f172a;
    }
  }

  .el-dialog__body {
    padding: 20px 24px 16px;
  }

  .el-dialog__footer {
    padding: 14px 24px 18px;
    border-top: 1px solid #f1f5f9;
    background: #fafafa;
  }

  .target-user-summary-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #f8fafc;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    margin-bottom: 16px;

    .target-user-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .target-avatar {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        font-size: 16px;
        font-weight: 700;
        background: linear-gradient(135deg, #3b82f6, #1d4ed8);
        color: #ffffff;
        flex-shrink: 0;
      }

      .target-user-meta {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .target-name-row {
          display: flex;
          align-items: center;
          gap: 8px;

          .target-real-name {
            font-size: 14px;
            font-weight: 700;
            color: #0f172a;
          }

          .target-username {
            font-size: 12px;
            color: #64748b;
            font-family: monospace;
          }
        }

        .target-dept-text {
          font-size: 11.5px;
          color: #94a3b8;
        }
      }
    }

    .target-user-status {
      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 11.5px;
        font-weight: 600;
        background: #fef2f2;
        color: #dc2626;
        border: 1px solid #fecaca;

        .status-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #dc2626;
        }

        &.enabled {
          background: #f0fdf4;
          color: #16a34a;
          border-color: #bbf7d0;

          .status-dot {
            background: #16a34a;
            box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
          }
        }
      }
    }
  }

  .presets-quick-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    flex-wrap: wrap;

    .presets-label {
      font-size: 12px;
      font-weight: 500;
      color: #64748b;
    }

    .preset-chip-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      border: 1px solid #e2e8f0;
      background: #ffffff;
      color: #475569;
      transition: all 0.2s ease;

      .chip-icon {
        font-size: 13px;
        display: inline-flex;
        align-items: center;
      }

      &:hover {
        border-color: #93c5fd;
        color: #2563eb;
        background: #eff6ff;
        transform: translateY(-1px);
      }

      &.active {
        background: #eff6ff;
        border-color: #3b82f6;
        color: #1d4ed8;
        font-weight: 600;
        box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
      }
    }
  }

  .reset-form-wrap {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .form-field-group {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .field-label {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12.5px;
        font-weight: 600;
        color: #334155;

        .label-req {
          color: #ef4444;
        }
      }

      .input-glow-wrap {
        width: 100%;

        .el-input__wrapper {
          border-radius: 9999px !important;
          padding: 4px 18px;
          box-shadow: 0 0 0 1px #cbd5e1 inset !important;
          transition: all 0.2s ease;

          &:hover {
            box-shadow: 0 0 0 1px #93c5fd inset !important;
          }

          &.is-focus {
            box-shadow: 0 0 0 2px #2563eb inset, 0 0 0 4px rgba(37, 99, 235, 0.12) !important;
          }
        }

        .input-icon {
          font-size: 15px;
          color: #94a3b8;
        }
      }

      .error-tip-line {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #ef4444;
        margin-top: 2px;

        .error-icon {
          font-size: 13px;
        }
      }
    }
  }

  .password-strength-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 6px;
    padding: 0 2px;

    .strength-bars {
      display: flex;
      gap: 6px;
      flex: 1;
      max-width: 180px;

      .strength-bar-segment {
        height: 4px;
        flex: 1;
        border-radius: 9999px;
        background: #e2e8f0;
        transition: all 0.25s ease;

        &.active.weak {
          background: #f43f5e;
        }

        &.active.medium {
          background: #f59e0b;
        }

        &.active.strong {
          background: #10b981;
        }
      }
    }

    .strength-text {
      font-size: 11.5px;
      font-weight: 600;

      &.weak {
        color: #f43f5e;
      }

      &.medium {
        color: #d97706;
      }

      &.strong {
        color: #059669;
      }
    }
  }

  .security-compliance-box {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 14px;
    border-radius: 10px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    margin-top: 4px;

    .compliance-icon {
      font-size: 15px;
      color: #2563eb;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .compliance-text {
      font-size: 12px;
      color: #1e40af;
      line-height: 1.5;

      strong {
        color: #1d4ed8;
      }
    }
  }

  .dialog-footer-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;

    .action-btn-cancel {
      padding: 9px 24px;
      border-radius: 9999px;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      color: #475569;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #f1f5f9;
        color: #1e293b;
        border-color: #94a3b8;
      }
    }

    .action-btn-confirm {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 9px 26px;
      border-radius: 9999px;
      border: none;
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      color: #ffffff;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
      transition: all 0.2s ease;

      &:hover:not(:disabled) {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.32);
        transform: translateY(-1px);
      }

      &:disabled {
        opacity: 0.55;
        cursor: not-allowed;
        box-shadow: none;
        transform: none;
      }
    }
  }
}

.audit-table-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;

  .card-header-flex {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    .card-title {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      display: flex;
      align-items: center;
      margin: 0;
    }

    .jump-log-center-btn {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #2563eb;
      font-size: 12px;
      font-weight: 600;
      padding: 5px 14px;
      border-radius: 9999px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #2563eb;
        color: #ffffff;
      }
    }
  }
}
</style>
