<template>
  <div v-loading="loading" class="user-hero-card">
    <div class="hero-left">
      <div class="avatar-box">
        <el-avatar
          :size="64"
          shape="square"
          :src="displayAvatar"
          class="user-detail-avatar"
          @error="onAvatarError"
        >
          {{ userInfo?.realName ? String(userInfo.realName).slice(0, 1) : '用' }}
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
            v-for="r in (userInfo?.roles as string[])"
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
      <el-button :icon="Key" @click="onResetPassword">
        重置登录密码
      </el-button>
      <el-button
        :type="isUserEnabled(userInfo) ? 'danger' : 'success'"
        :icon="isUserEnabled(userInfo) ? Lock : Unlock"
        plain
        @click="onToggleStatus"
      >
        {{ isUserEnabled(userInfo) ? '冻结账号' : '解冻并恢复使用' }}
      </el-button>
    </div>
  </div>

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
        <el-icon class="chip-icon"><MagicStick /></el-icon>
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
import {
  Key,
  Lock,
  Unlock,
  WarningFilled,
  Loading,
  Lightning,
  MagicStick,
  Check,
  CircleCloseFilled
} from '@element-plus/icons-vue';
import { isUserEnabled, getRoleLabel } from '@/composables/system/useUserDetail';

const resetPasswordDialogVisible = defineModel<boolean>('resetPasswordDialogVisible', { required: true });
const resetPasswordForm = defineModel<{ newPassword: string; confirmPassword: string }>('resetPasswordForm', {
  required: true
});

defineProps<{
  loading: boolean;
  userInfo: Record<string, unknown> | null;
  displayAvatar?: string;
  resetPasswordLoading: boolean;
  passwordStrengthScore: number;
  passwordStrengthLevel: { text: string; type: string };
  isPasswordMismatch: boolean;
  isSubmitValid: boolean;
  onAvatarError: () => void;
  onResetPassword: () => void;
  onToggleStatus: () => void;
  onApplyPreset: (pwd: string) => void;
  onGenerateRandom: () => void;
  onSubmitReset: () => void;
}>();
</script>

<style scoped lang="scss">
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
</style>

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
</style>
