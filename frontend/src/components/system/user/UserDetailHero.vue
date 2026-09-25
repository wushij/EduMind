<template>
  <div v-loading="loading" class="user-detail-header-card">
    <!-- 顶部导航与面包屑（对标图 3 题库详情设计） -->
    <div class="header-nav-bar">
      <button type="button" class="back-btn" @click="onBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回用户列表</span>
      </button>
      <el-divider direction="vertical" class="nav-divider" />
      <el-breadcrumb separator="/" class="header-breadcrumb">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/system/users' }">用户权限管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ (userInfo?.realName as string) || '用户画像详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 用户形象、标题与元数据展示区（全宽通栏，舒展透气，不扎堆） -->
    <div class="header-profile-section">
      <div class="user-avatar-orb">
        <el-avatar
          :size="64"
          shape="square"
          :src="displayAvatar"
          class="user-orb-avatar"
          @error="onAvatarError"
        >
          {{ userInfo?.realName ? String(userInfo.realName).slice(0, 1) : '用' }}
        </el-avatar>
        <span class="orb-glow-ring" />
      </div>

      <div class="user-meta-content">
        <!-- 标题行：姓名 + 账号 + 主角色标识 + 状态胶囊 -->
        <div class="user-title-line">
          <h1 class="user-title">{{ (userInfo?.realName as string) || '超级管理员' }}</h1>
          <span class="user-account-badge">@{{ (userInfo?.username as string) || 'admin' }}</span>
          <span class="role-badge-chip">
            <el-icon><Stamp /></el-icon>
            {{ primaryRoleLabel }}
          </span>
          <span class="status-pill" :class="{ 'is-disabled': !isUserEnabled(userInfo) }">
            <span class="status-indicator-dot" />
            {{ isUserEnabled(userInfo) ? '正常活跃' : '账号冻结' }}
          </span>
        </div>

        <!-- 角色职责与业务描述 -->
        <p class="user-description">
          {{ userRoleDescription }}
        </p>

        <!-- 元数据时间与院系机构行（整洁单行排布） -->
        <div class="user-time-meta">
          <template v-if="userInfo?.department">
            <span class="meta-item">
              <el-icon><School /></el-icon>
              {{ userInfo.department }}
            </span>
            <span class="meta-dot">·</span>
          </template>
          <span class="meta-item">
            <el-icon><Clock /></el-icon>
            最近活跃于 {{ recentActiveTime }}
          </span>
          <template v-if="userInfo?.createTime">
            <span class="meta-dot">·</span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              注册于 {{ formatDateTime(userInfo.createTime) }}
            </span>
          </template>
          <span class="meta-dot">·</span>
          <span class="status-item">
            <span class="status-indicator-dot" />
            账号会话运行就绪
          </span>
        </div>
      </div>
    </div>

    <!-- 底部操作按钮一排（图 2 独立移至底部最下一排，主客分明） -->
    <div class="header-actions-bar">
      <!-- 左侧：核心业务主要操作 -->
      <div class="actions-group-left">
        <button
          type="button"
          class="action-pill action-pill--brand"
          @click="onResetPassword"
        >
          <el-icon><Key /></el-icon>
          <span>重置登录密码</span>
          <span class="ai-spark-chip">安全凭据</span>
        </button>

        <button
          type="button"
          class="action-pill action-pill--compose"
          @click="onOpenEditProfile"
        >
          <el-icon><EditPen /></el-icon>
          <span>编辑基础档案</span>
        </button>

        <button
          type="button"
          class="action-pill action-pill--indigo"
          @click="onOpenEditRoles"
        >
          <el-icon><Stamp /></el-icon>
          <span>调整分配角色</span>
          <span class="ai-spark-chip">RBAC</span>
        </button>
      </div>

      <!-- 右侧：辅助与安全管控操作 -->
      <div class="actions-group-right">
        <button
          type="button"
          class="action-pill action-pill--ghost"
          title="强制切断在线 Token，要求重新登录"
          @click="onKickoutSession"
        >
          <el-icon><SwitchButton /></el-icon>
          <span>强制下线会话</span>
        </button>

        <button
          type="button"
          class="action-pill action-pill--ghost"
          title="导出结构化档案凭证"
          @click="onExportCard"
        >
          <el-icon><Download /></el-icon>
          <span>导出档案凭证</span>
        </button>

        <el-popconfirm
          :title="isUserEnabled(userInfo) ? '确定要冻结此用户账号吗？冻结后该用户将无法登录。' : '确定要解冻并恢复该用户的使用权限吗？'"
          :confirm-button-text="isUserEnabled(userInfo) ? '确认冻结' : '确认解冻'"
          cancel-button-text="取消"
          :confirm-button-type="isUserEnabled(userInfo) ? 'danger' : 'primary'"
          @confirm="onToggleStatus"
        >
          <template #reference>
            <button
              type="button"
              class="action-pill"
              :class="isUserEnabled(userInfo) ? 'action-pill--danger' : 'action-pill--success'"
            >
              <el-icon><component :is="isUserEnabled(userInfo) ? Lock : Unlock" /></el-icon>
              <span>{{ isUserEnabled(userInfo) ? '冻结账号' : '解冻恢复' }}</span>
            </button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <!-- 底部：4 维教学资产与管理微看板 (1:1 模仿图 3 题库详情) -->
    <UserDetailStatsBar
      :audit-count="statAuditCount"
      :role-count="statRoleCount"
      :roles="(userInfo?.roles as string[])"
      :is-enabled="isUserEnabled(userInfo)"
      :security-score="statSecurityScore"
    />
  </div>

  <!-- 重置登录密码对话框 (保留并升华高阶安全风格) -->
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

    <div class="target-user-summary-card">
      <div class="target-user-left">
        <el-avatar
          :size="42"
          shape="square"
          :src="displayAvatar"
          class="target-avatar"
          @error="onAvatarError"
        >
          {{ userInfo?.realName ? String(userInfo.realName).slice(0, 1) : '用' }}
        </el-avatar>
        <div class="target-user-meta">
          <div class="target-name-row">
            <span class="target-real-name">{{ userInfo?.realName || '—' }}</span>
            <span class="target-username">@{{ userInfo?.username }}</span>
          </div>
          <div class="target-dept-text">{{ userInfo?.department || '未配置院系' }}</div>
        </div>
      </div>
      <div class="target-user-status">
        <span class="status-pill" :class="{ enabled: isUserEnabled(userInfo) }">
          <span class="status-dot"></span>
          {{ isUserEnabled(userInfo) ? '账号正常' : '已冻结' }}
        </span>
      </div>
    </div>

    <div class="presets-quick-bar">
      <span class="presets-label">快捷预设：</span>
      <button
        type="button"
        class="preset-chip-btn default-chip"
        :class="{ active: resetPasswordForm.newPassword === '123456' }"
        @click="onApplyPreset('123456')"
      >
        <el-icon class="chip-icon"><Lightning /></el-icon>
        <span>默认初始密码 (123456)</span>
      </button>
      <button type="button" class="preset-chip-btn random-chip" @click="onGenerateRandom">
        <el-icon class="chip-icon"><AiSparkleIcon /></el-icon>
        <span>生成高强度随机密码</span>
      </button>
    </div>

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

        <div v-if="resetPasswordForm.newPassword" class="password-strength-container">
          <div class="strength-bars">
            <div
              class="strength-bar-segment"
              :class="{
                active: passwordStrengthScore >= 1,
                weak: passwordStrengthScore === 1,
                medium: passwordStrengthScore === 2,
                strong: passwordStrengthScore >= 3
              }"
            />
            <div
              class="strength-bar-segment"
              :class="{
                active: passwordStrengthScore >= 2,
                medium: passwordStrengthScore === 2,
                strong: passwordStrengthScore >= 3
              }"
            />
            <div
              class="strength-bar-segment"
              :class="{ active: passwordStrengthScore >= 3, strong: passwordStrengthScore >= 3 }"
            />
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
          @click="onSubmitReset"
        >
          <el-icon v-if="resetPasswordLoading" class="is-loading mr-1"><Loading /></el-icon>
          <el-icon v-else class="mr-1"><Check /></el-icon>
          <span>确认重置密码</span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  ArrowLeft,
  Key,
  Lock,
  Unlock,
  EditPen,
  Stamp,
  SwitchButton,
  Download,
  Clock,
  Calendar,
  School,
  Lightning,
  Check,
  Loading,
  WarningFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue';
import UserDetailStatsBar from '@/components/system/user/UserDetailStatsBar.vue';
import { isUserEnabled, getRoleDescription } from '@/composables/system/useUserDetail';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const resetPasswordDialogVisible = defineModel<boolean>('resetPasswordDialogVisible', { required: true });
const resetPasswordForm = defineModel<{ newPassword: string; confirmPassword: string }>('resetPasswordForm', {
  required: true
});

const props = defineProps<{
  loading: boolean;
  userInfo: Record<string, any> | null;
  displayAvatar?: string;
  statAuditCount: number;
  statRoleCount: number;
  statSecurityScore: { title: string; desc: string };
  resetPasswordLoading: boolean;
  passwordStrengthScore: number;
  passwordStrengthLevel: { text: string; type: string };
  isPasswordMismatch: boolean;
  isSubmitValid: boolean;
  onBack: () => void;
  onAvatarError: () => void;
  onResetPassword: () => void;
  onOpenEditProfile: () => void;
  onOpenEditRoles: () => void;
  onKickoutSession: () => void;
  onExportCard: () => void;
  onToggleStatus: () => void;
  onApplyPreset: (pwd: string) => void;
  onGenerateRandom: () => void;
  onSubmitReset: () => void;
}>();

const primaryRoleLabel = computed(() => {
  const roles = (props.userInfo?.roles as string[]) || [];
  if (roles.length === 0) return '系统常规用户';
  if (roles.some((r) => r.includes('ADMIN'))) return '系统超级管理员';
  if (roles.some((r) => r.includes('TEACHER'))) return '课程主讲教师';
  if (roles.some((r) => r.includes('STUDENT'))) return '修读学生';
  return '系统授权用户';
});

const userRoleDescription = computed(() => {
  const roles = (props.userInfo?.roles as string[]) || [];
  if (roles.length === 0) {
    return '智教云平台注册用户，具备基础教学与学习资源访问权限。';
  }
  const descriptions = roles.map(getRoleDescription);
  return descriptions[0] || '拥有平台全局系统操作与资源调度配置权限。';
});

const recentActiveTime = computed(() => {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day} ${hh}:${mm}`;
});

function formatDateTime(val: unknown): string {
  if (!val) return '';
  return String(val).replace('T', ' ').slice(0, 10);
}
</script>

<style scoped lang="scss">
.user-detail-header-card {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 24px rgba(30, 80, 160, 0.05);
  background: #ffffff;
  background-image:
    linear-gradient(90deg, #2563eb 0%, #6366f1 50%, #7c3aed 100%),
    linear-gradient(145deg, #ffffff 0%, #f8fafc 55%, #f1f7ff 100%);
  background-size: 100% 3px, 100% calc(100% - 3px);
  background-position: 0 0, 0 3px;
  background-repeat: no-repeat;
  padding: 16px 28px 20px;
  margin-bottom: 20px;

  /* 顶部导航与面包屑 */
  .header-nav-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f1f5f9;
    margin-bottom: 18px;

    .back-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0;
      border: none;
      background: transparent;
      font-size: 13px;
      font-weight: 600;
      color: #2563eb;
      cursor: pointer;
      white-space: nowrap;
      transition: color 0.15s ease;

      .el-icon {
        font-size: 14px;
      }

      &:hover {
        color: #1d4ed8;
      }
    }

    .nav-divider {
      margin: 0 4px;
      height: 14px;
      border-color: #e2e8f0;
    }

    .header-breadcrumb {
      flex: 1;

      :deep(.el-breadcrumb__inner) {
        font-size: 12.5px;
        color: #94a3b8;
      }

      :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
        color: #475569;
        font-weight: 600;
      }
    }
  }

  /* 2. 用户全宽形象与档案概览（舒展通畅，绝不扎堆） */
  .header-profile-section {
    display: flex;
    align-items: flex-start;
    gap: 20px;
    width: 100%;

    .user-avatar-orb {
      position: relative;
      flex-shrink: 0;
      width: 64px;
      height: 64px;
      border-radius: 18px;
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      border: 1.5px solid #bfdbfe;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.12);
      overflow: hidden;

      .user-orb-avatar {
        width: 100%;
        height: 100%;
        border-radius: 16px;
        font-size: 26px;
        font-weight: 800;
        background: linear-gradient(135deg, #2563eb, #6366f1);
        color: #ffffff;

        :deep(img) {
          object-fit: cover;
        }
      }

      .orb-glow-ring {
        position: absolute;
        inset: -2px;
        border-radius: 20px;
        background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), transparent 70%);
        pointer-events: none;
      }
    }

    .user-meta-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;
      min-width: 0;

      .user-title-line {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .user-title {
          margin: 0;
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          letter-spacing: -0.02em;
          line-height: 1.25;
        }

        .user-account-badge {
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 13px;
          color: #64748b;
          font-weight: 600;
          background: #f1f5f9;
          padding: 2px 8px;
          border-radius: 6px;
          border: 1px solid #e2e8f0;
        }

        .role-badge-chip {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 3px 10px;
          border-radius: 9999px;
          background: #eff6ff;
          border: 1px solid #bfdbfe;
          color: #2563eb;
          font-size: 12px;
          font-weight: 600;
          white-space: nowrap;

          .el-icon {
            font-size: 13px;
          }
        }

        .status-pill {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 3px 10px;
          border-radius: 9999px;
          background: #ecfdf5;
          border: 1px solid #a7f3d0;
          color: #059669;
          font-size: 12px;
          font-weight: 600;
          white-space: nowrap;

          .status-indicator-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
          }

          &.is-disabled {
            background: #fef2f2;
            border-color: #fecaca;
            color: #dc2626;

            .status-indicator-dot {
              background: #ef4444;
              box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
            }
          }
        }
      }

      .user-description {
        margin: 0;
        font-size: 13px;
        color: #64748b;
        line-height: 1.6;
      }

      .user-time-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12.5px;
        color: #94a3b8;
        flex-wrap: wrap;

        .meta-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;

          .el-icon {
            font-size: 13px;
          }
        }

        .meta-dot {
          color: #cbd5e1;
        }

        .status-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          color: #059669;
          font-weight: 500;

          .status-indicator-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
          }
        }
      }
    }
  }

  /* 3. 底部操作按钮一排（图 2 独立在底部一排展示，左右分布） */
  .header-actions-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid #f1f5f9;
    flex-wrap: wrap;

    .actions-group-left,
    .actions-group-right {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    .action-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px;
      font-size: 12.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      outline: none;
      user-select: none;
      white-space: nowrap;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .el-icon {
        font-size: 14px;
      }

      /* 品牌主蓝：重置密码 */
      &--brand {
        background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
          background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
        }

        .ai-spark-chip {
          padding: 1px 7px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.24);
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.02em;
        }
      }

      /* 档案编辑：浅蓝边框 */
      &--compose {
        background: #ffffff;
        color: #1e40af;
        border: 1.5px solid #93c5fd;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);

        &:hover {
          background: #eff6ff;
          border-color: #60a5fa;
          transform: translateY(-2px);
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.16);
        }
      }

      /* 紫色渐变：角色权限 */
      &--indigo {
        background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(124, 58, 237, 0.22);

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 18px rgba(124, 58, 237, 0.32);
          background: linear-gradient(135deg, #4338ca 0%, #6d28d9 100%);
        }

        .ai-spark-chip {
          padding: 1px 7px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.25);
          font-size: 11px;
          font-weight: 700;
        }
      }

      /* 次要 Ghost 胶囊 */
      &--ghost {
        background: #f8fafc;
        color: #475569;
        border: 1px solid #cbd5e1;

        &:hover {
          background: #f1f5f9;
          color: #0f172a;
          border-color: #94a3b8;
          transform: translateY(-1px);
        }
      }

      /* 危险红胶囊 */
      &--danger {
        background: #fff1f2;
        color: #e11d48;
        border: 1px solid #fecdd3;

        &:hover {
          background: #ffe4e6;
          border-color: #fda4af;
          transform: translateY(-1px);
        }
      }

      /* 恢复绿胶囊 */
      &--success {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;

        &:hover {
          background: #d1fae5;
          transform: translateY(-1px);
        }
      }
    }
  }
}

/* 密码重置对话框样式 */
.custom-reset-password-dialog {
  :deep(.el-dialog__header) {
    padding: 0;
    margin: 0;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
  }

  :deep(.el-dialog__footer) {
    padding: 0 24px 20px;
    border-top: none;
  }
}

.dialog-header-premium {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;

  .header-icon-badge {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    color: #2563eb;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  .header-text-group {
    flex: 1;

    .dialog-title-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .dialog-title {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
      }

      .dialog-badge {
        font-size: 11px;
        padding: 2px 7px;
        border-radius: 9999px;
        background: #eff6ff;
        color: #2563eb;
        font-weight: 600;
        border: 1px solid #bfdbfe;
      }
    }

    .dialog-subtitle {
      margin: 4px 0 0;
      font-size: 12.5px;
      color: #64748b;
    }
  }
}

.target-user-summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  margin-bottom: 16px;

  .target-user-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .target-avatar {
      font-weight: 700;
      background: linear-gradient(135deg, #3b82f6, #1d4ed8);
      color: #fff;
    }

    .target-user-meta {
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
        font-size: 12px;
        color: #94a3b8;
        margin-top: 2px;
      }
    }
  }

  .status-pill {
    font-size: 12px;
    font-weight: 600;
    color: #dc2626;
    display: inline-flex;
    align-items: center;
    gap: 4px;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #ef4444;
    }

    &.enabled {
      color: #059669;

      .status-dot {
        background: #10b981;
      }
    }
  }
}

.presets-quick-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;

  .presets-label {
    font-size: 12.5px;
    font-weight: 600;
    color: #475569;
  }

  .preset-chip-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 12px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    border: 1px solid transparent;
    transition: all 0.15s ease;

    .chip-icon {
      font-size: 13px;
    }

    &.default-chip {
      background: #eff6ff;
      border-color: #bfdbfe;
      color: #2563eb;

      &:hover,
      &.active {
        background: #2563eb;
        color: #ffffff;
      }
    }

    &.random-chip {
      background: #f5f3ff;
      border-color: #ddd6fe;
      color: #7c3aed;

      &:hover {
        background: #7c3aed;
        color: #ffffff;
      }
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
      font-size: 13px;
      font-weight: 600;
      color: #1e293b;
      display: flex;
      align-items: center;
      gap: 4px;

      .label-req {
        color: #ef4444;
      }
    }

    .password-strength-container {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-top: 4px;

      .strength-bars {
        display: flex;
        gap: 4px;
        flex: 1;

        .strength-bar-segment {
          height: 5px;
          flex: 1;
          border-radius: 9999px;
          background: #e2e8f0;
          transition: background 0.2s ease;

          &.active.weak {
            background: #ef4444;
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
          color: #ef4444;
        }

        &.medium {
          color: #f59e0b;
        }

        &.strong {
          color: #10b981;
        }
      }
    }

    .error-tip-line {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #ef4444;
      margin-top: 4px;
    }
  }

  .security-compliance-box {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 10px 14px;
    border-radius: 10px;
    background: #fffbeb;
    border: 1px solid #fde68a;

    .compliance-icon {
      color: #d97706;
      font-size: 16px;
      margin-top: 2px;
    }

    .compliance-text {
      font-size: 12px;
      color: #92400e;
      line-height: 1.5;

      strong {
        font-weight: 700;
      }
    }
  }
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  .action-btn-cancel {
    padding: 8px 18px;
    border-radius: 9999px;
    border: 1px solid #cbd5e1;
    background: #ffffff;
    color: #475569;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background: #f1f5f9;
      color: #0f172a;
    }
  }

  .action-btn-confirm {
    display: inline-flex;
    align-items: center;
    padding: 8px 22px;
    border-radius: 9999px;
    border: none;
    background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
    color: #ffffff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
    transition: all 0.2s ease;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      box-shadow: none;
    }

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
    }
  }
}
</style>
