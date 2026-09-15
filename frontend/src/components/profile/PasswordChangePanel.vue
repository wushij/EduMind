<template>
  <div class="security-panel-card left-panel">
    <div class="panel-header-line">
      <div class="title-with-icon">
        <el-icon class="panel-icon"><Lock /></el-icon>
        <h3 class="panel-title">密码管理与身份安全重置</h3>
      </div>

      <div class="mode-pill-switcher">
        <button
          type="button"
          class="mode-switch-btn"
          :class="{ active: activeMode === 'password' }"
          @click="activeMode = 'password'"
        >
          <el-icon><Key /></el-icon>
          <span>原密码验证修改</span>
        </button>
        <button
          type="button"
          class="mode-switch-btn"
          :class="{ active: activeMode === 'email' }"
          @click="activeMode = 'email'"
        >
          <el-icon><Message /></el-icon>
          <span>安全邮箱验证重置</span>
          <span v-if="userEmail" class="mini-badge">推荐</span>
        </button>
      </div>
    </div>

    <div v-if="activeMode === 'password'" class="mode-content password-mode">
      <div class="mode-desc-alert">
        <el-icon class="alert-icon"><InfoFilled /></el-icon>
        <span>如果您记得当前登录密码，请输入原密码验证身份并设置新密码。</span>
      </div>

      <el-form label-position="top" class="security-capsule-form" autocomplete="off" @submit.prevent="handleChangePassword">
        <el-form-item label="当前登录密码">
          <div class="capsule-input-box">
            <el-icon class="input-icon"><Lock /></el-icon>
            <input
              v-model="pwdForm.oldPassword"
              :type="showOldPwd ? 'text' : 'password'"
              class="capsule-input"
              placeholder="请输入您当前使用的登录密码"
              autocomplete="current-password"
              name="current-password"
            />
            <button type="button" class="pwd-eye-btn" @click="showOldPwd = !showOldPwd">
              <el-icon><View v-if="showOldPwd" /><Hide v-else /></el-icon>
            </button>
          </div>
        </el-form-item>

        <el-form-item label="设定新密码">
          <div class="capsule-input-box">
            <el-icon class="input-icon"><Lock /></el-icon>
            <input
              v-model="pwdForm.newPassword"
              :type="showNewPwd ? 'text' : 'password'"
              class="capsule-input"
              placeholder="请输入 8~20 位新密码 (包含字母与数字)"
              autocomplete="new-password"
              name="change-new-password"
            />
            <button type="button" class="pwd-eye-btn" @click="showNewPwd = !showNewPwd">
              <el-icon><View v-if="showNewPwd" /><Hide v-else /></el-icon>
            </button>
          </div>

          <div v-if="pwdForm.newPassword" class="strength-meter-box">
            <div class="meter-top">
              <span class="meter-label">密码强度：</span>
              <span class="meter-text" :class="pwdStrength.class">{{ pwdStrength.label }}</span>
            </div>
            <div class="meter-bars">
              <div class="bar" :class="{ active: pwdStrength.score >= 1, weak: pwdStrength.score === 1, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
              <div class="bar" :class="{ active: pwdStrength.score >= 2, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
              <div class="bar" :class="{ active: pwdStrength.score >= 3, strong: pwdStrength.score === 3 }"></div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="确认新密码">
          <div class="capsule-input-box">
            <el-icon class="input-icon"><Lock /></el-icon>
            <input
              v-model="pwdForm.confirmPassword"
              :type="showConfirmPwd ? 'text' : 'password'"
              class="capsule-input"
              placeholder="请再次输入新密码以确认"
              autocomplete="new-password"
              name="confirm-new-password"
            />
            <button type="button" class="pwd-eye-btn" @click="showConfirmPwd = !showConfirmPwd">
              <el-icon><View v-if="showConfirmPwd" /><Hide v-else /></el-icon>
            </button>
          </div>
        </el-form-item>

        <div class="submit-action-row">
          <button
            type="button"
            class="capsule-action-btn primary"
            :disabled="submittingPwd"
            @click="handleChangePassword"
          >
            <span v-if="submittingPwd">正在安全提交...</span>
            <span v-else>确认修改登录密码</span>
          </button>
        </div>

        <div class="forgot-hint-row">
          <span>不记得当前原密码？</span>
          <button type="button" class="link-btn" @click="activeMode = 'email'">
            切换至安全邮箱验证码免密重置
          </button>
          <span class="divider">或</span>
          <button type="button" class="link-btn" @click="handleGoToForgotPassword">
            系统外部找回通道
          </button>
        </div>
      </el-form>
    </div>

    <div v-else class="mode-content email-mode">
      <div v-if="userEmail">
        <div class="mode-desc-alert email-alert">
          <el-icon class="alert-icon"><InfoFilled /></el-icon>
          <span>无需原密码！系统将向您绑定的安全邮箱发送 6 位临时验证码，核验通过即可直接重设密码。</span>
        </div>

        <el-form label-position="top" class="security-capsule-form" autocomplete="off" @submit.prevent="handleEmailResetPassword">
          <el-form-item label="密保邮箱">
            <div class="email-status-card">
              <el-icon class="email-prefix-icon"><Message /></el-icon>
              <span class="email-address">{{ maskEmail(userEmail) }}</span>
              <button type="button" class="email-action-btn" @click="emit('open-bind')">
                更换绑定
              </button>
            </div>
          </el-form-item>

          <el-form-item label="邮箱验证码">
            <div class="capsule-code-row">
              <div class="capsule-input-box code-input">
                <el-icon class="input-icon"><Message /></el-icon>
                <input
                  v-model="emailResetForm.code"
                  type="text"
                  maxlength="6"
                  inputmode="numeric"
                  class="capsule-input"
                  placeholder="请输入 6 位数字验证码"
                  autocomplete="one-time-code"
                  name="email-verification-code"
                  autocapitalize="off"
                  autocorrect="off"
                  spellcheck="false"
                />
              </div>
              <button
                type="button"
                class="capsule-code-btn"
                :disabled="sendingCode || countdown > 0"
                @click="handleSendEmailResetCode"
              >
                <span v-if="sendingCode">发送中...</span>
                <span v-else-if="countdown > 0">{{ countdown }}s 后重新获取</span>
                <span v-else>获取验证码</span>
              </button>
            </div>
          </el-form-item>

          <el-form-item label="设置新密码">
            <div class="capsule-input-box">
              <el-icon class="input-icon"><Lock /></el-icon>
              <input
                v-model="emailResetForm.newPassword"
                :type="showEmailNewPwd ? 'text' : 'password'"
                class="capsule-input"
                placeholder="请输入 8~20 位新密码 (包含字母与数字)"
                autocomplete="new-password"
                name="reset-new-password"
              />
              <button type="button" class="pwd-eye-btn" @click="showEmailNewPwd = !showEmailNewPwd">
                <el-icon><View v-if="showEmailNewPwd" /><Hide v-else /></el-icon>
              </button>
            </div>

            <div v-if="emailResetForm.newPassword" class="strength-meter-box">
              <div class="meter-top">
                <span class="meter-label">密码强度：</span>
                <span class="meter-text" :class="pwdStrength.class">{{ pwdStrength.label }}</span>
              </div>
              <div class="meter-bars">
                <div class="bar" :class="{ active: pwdStrength.score >= 1, weak: pwdStrength.score === 1, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
                <div class="bar" :class="{ active: pwdStrength.score >= 2, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
                <div class="bar" :class="{ active: pwdStrength.score >= 3, strong: pwdStrength.score === 3 }"></div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="确认新密码">
            <div class="capsule-input-box">
              <el-icon class="input-icon"><Lock /></el-icon>
              <input
                v-model="emailResetForm.confirmPassword"
                :type="showEmailConfirmPwd ? 'text' : 'password'"
                class="capsule-input"
                placeholder="请再次输入新密码以确认"
                autocomplete="new-password"
                name="reset-confirm-password"
              />
              <button type="button" class="pwd-eye-btn" @click="showEmailConfirmPwd = !showEmailConfirmPwd">
                <el-icon><View v-if="showEmailConfirmPwd" /><Hide v-else /></el-icon>
              </button>
            </div>
          </el-form-item>

          <div class="submit-action-row">
            <button
              type="button"
              class="capsule-action-btn primary"
              :disabled="submittingEmailReset"
              @click="handleEmailResetPassword"
            >
              <span v-if="submittingEmailReset">正在核验并重置...</span>
              <span v-else>安全核验并重置密码</span>
            </button>
          </div>
        </el-form>
      </div>

      <div v-else class="unbound-email-empty">
        <div class="empty-icon-box">
          <el-icon><Message /></el-icon>
        </div>
        <h4 class="empty-title">当前账号尚未绑定安全密保邮箱</h4>
        <p class="empty-desc">
          绑定邮箱后，您不仅可以在遗忘密码时随时秒级免密重置，还能接收敏感操作预警与教学通知推送。
        </p>
        <div class="empty-actions">
          <button type="button" class="capsule-action-btn primary" @click="emit('open-bind')">
            <el-icon><Plus /></el-icon>
            <span>立即绑定安全邮箱</span>
          </button>
          <button type="button" class="capsule-action-btn secondary" @click="activeMode = 'password'">
            <span>使用原密码修改</span>
          </button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="bindDialogVisible"
      width="480px"
      destroy-on-close
      class="email-bind-dialog"
    >
      <template #header>
        <div class="bind-dialog-header">
          <div class="header-icon-badge">
            <el-icon><Message /></el-icon>
          </div>
          <div class="header-titles">
            <h3 class="title">{{ userEmail ? '更换安全绑定邮箱' : '绑定账户安全邮箱' }}</h3>
            <span class="subtitle">绑定后可用于接收安全验证码及敏感操作身份核验</span>
          </div>
        </div>
      </template>

      <div class="dialog-body-box">
        <div class="dialog-input-group">
          <label class="group-label">电子邮箱地址</label>
          <div class="capsule-input-box">
            <el-icon class="input-icon"><Message /></el-icon>
            <input
              v-model="bindForm.email"
              type="email"
              placeholder="请输入新的安全电子邮箱"
              class="capsule-input"
              autocomplete="email"
              name="bind-email"
            />
          </div>
        </div>

        <div class="dialog-input-group">
          <label class="group-label">邮箱验证码</label>
          <div class="capsule-code-row">
            <div class="capsule-input-box code-input">
              <el-icon class="input-icon"><Message /></el-icon>
              <input
                v-model="bindForm.code"
                type="text"
                maxlength="6"
                inputmode="numeric"
                placeholder="6 位数字验证码"
                class="capsule-input"
                autocomplete="one-time-code"
                name="bind-email-verification-code"
                autocapitalize="off"
                autocorrect="off"
                spellcheck="false"
              />
            </div>
            <button
              type="button"
              class="capsule-code-btn"
              :disabled="sendingBindCode || bindCountdown > 0"
              @click="handleSendBindCode"
            >
              <span v-if="sendingBindCode">发送中...</span>
              <span v-else-if="bindCountdown > 0">{{ bindCountdown }}s</span>
              <span v-else>获取验证码</span>
            </button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer-row">
          <button type="button" class="dialog-btn cancel" @click="bindDialogVisible = false">
            取消
          </button>
          <button
            type="button"
            class="dialog-btn confirm"
            :disabled="bindingLoading"
            @click="handleConfirmBind"
          >
            <span v-if="bindingLoading">正在绑定...</span>
            <span v-else>确认保存绑定</span>
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  Lock,
  Key,
  Message,
  View,
  Hide,
  Plus,
  InfoFilled
} from '@element-plus/icons-vue';
import type { PasswordStrength } from '@/composables/profile/useSecurity';

const activeMode = defineModel<'password' | 'email'>('activeMode', { required: true });
const showOldPwd = defineModel<boolean>('showOldPwd', { required: true });
const showNewPwd = defineModel<boolean>('showNewPwd', { required: true });
const showConfirmPwd = defineModel<boolean>('showConfirmPwd', { required: true });
const showEmailNewPwd = defineModel<boolean>('showEmailNewPwd', { required: true });
const showEmailConfirmPwd = defineModel<boolean>('showEmailConfirmPwd', { required: true });
const bindDialogVisible = defineModel<boolean>('bindDialogVisible', { required: true });

defineProps<{
  submittingPwd: boolean;
  submittingEmailReset: boolean;
  sendingCode: boolean;
  countdown: number;
  pwdForm: { oldPassword: string; newPassword: string; confirmPassword: string };
  emailResetForm: { code: string; newPassword: string; confirmPassword: string };
  bindingLoading: boolean;
  sendingBindCode: boolean;
  bindCountdown: number;
  bindForm: { email: string; code: string };
  userEmail: string;
  pwdStrength: PasswordStrength;
  maskEmail: (email: string) => string;
  handleChangePassword: () => Promise<void>;
  handleSendEmailResetCode: () => Promise<void>;
  handleEmailResetPassword: () => Promise<void>;
  handleSendBindCode: () => Promise<void>;
  handleConfirmBind: () => Promise<void>;
  handleGoToForgotPassword: () => void;
}>();

const emit = defineEmits<{
  'open-bind': [];
}>();
</script>

<style scoped lang="scss">
.security-panel-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 26px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.03);

  .panel-header-line {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 22px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f1f5f9;

    .title-with-icon {
      display: flex;
      align-items: center;
      gap: 10px;

      .panel-icon {
        font-size: 20px;
        color: #2563eb;
      }

      .panel-title {
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
      }
    }

    .mode-pill-switcher {
      display: inline-flex;
      background: #f1f5f9;
      padding: 4px;
      border-radius: 9999px;
      gap: 4px;

      .mode-switch-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 14px;
        border-radius: 9999px;
        border: none;
        background: transparent;
        color: #64748b;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        &:hover {
          color: #1e293b;
        }

        &.active {
          background: #ffffff;
          color: #2563eb;
          box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
        }

        .mini-badge {
          background: #dbeafe;
          color: #1d4ed8;
          font-size: 10px;
          padding: 1px 6px;
          border-radius: 9999px;
          font-weight: 700;
        }
      }
    }
  }
}

.mode-desc-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 22px;
  font-size: 13px;
  color: #475569;

  .alert-icon {
    font-size: 18px;
    color: #3b82f6;
    flex-shrink: 0;
  }

  &.email-alert {
    background: #eff6ff;
    border-color: #dbeafe;
    color: #1e40af;

    .alert-icon {
      color: #2563eb;
    }
  }
}

.security-capsule-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;

    .el-form-item__label {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      padding-bottom: 6px;
    }

    :deep(.el-form-item__content) {
      width: 100%;
    }
  }

  .email-status-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    min-height: 46px;
    padding: 0 8px 0 16px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 9999px;
    box-sizing: border-box;

    .email-prefix-icon {
      font-size: 16px;
      color: #94a3b8;
      margin-right: 10px;
      flex-shrink: 0;
    }

    .email-address {
      flex: 1;
      min-width: 0;
      font-size: 14px;
      color: #1e293b;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .email-action-btn {
      flex-shrink: 0;
      margin-left: 12px;
      padding: 6px 16px;
      border-radius: 9999px;
      background: #ffffff;
      border: 1px solid #cbd5e1;
      color: #1677ff;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #1677ff;
        border-color: #1677ff;
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);
      }
    }
  }

  .capsule-input-box {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    height: 46px;
    background: #f8fafc;
    border: 1px solid #cbd5e1;
    border-radius: 9999px;
    padding: 0 16px;
    transition: all 0.2s ease;

    &:focus-within {
      background: #ffffff;
      border-color: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
    }

    .input-icon {
      font-size: 16px;
      color: #94a3b8;
      margin-right: 10px;
      flex-shrink: 0;
    }

    .capsule-input {
      flex: 1;
      border: none;
      background: transparent;
      outline: none;
      font-size: 14px;
      color: #0f172a;
      height: 100%;
      width: 100%;

      &::placeholder {
        color: #94a3b8;
      }
    }

    .pwd-eye-btn {
      border: none;
      background: transparent;
      color: #94a3b8;
      font-size: 16px;
      cursor: pointer;
      display: flex;
      align-items: center;
      padding: 4px;
      transition: color 0.15s;

      &:hover {
        color: #475569;
      }
    }
  }

  .capsule-code-row {
    display: flex;
    gap: 12px;
    width: 100%;

    .code-input {
      flex: 1;
    }

    .capsule-code-btn {
      height: 46px;
      padding: 0 20px;
      border-radius: 9999px;
      border: none;
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #ffffff !important;
      font-size: 13px;
      font-weight: 600;
      white-space: nowrap;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
      transition: all 0.2s;

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
      }

      &:disabled {
        background: #94a3b8 !important;
        color: #f1f5f9 !important;
        cursor: not-allowed;
        box-shadow: none;
      }
    }
  }

  .strength-meter-box {
    margin-top: 8px;
    padding: 0 4px;

    .meter-top {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      margin-bottom: 4px;

      .meter-label {
        color: #64748b;
      }

      .meter-text {
        font-weight: 600;

        &.weak { color: #ef4444; }
        &.medium { color: #f59e0b; }
        &.strong { color: #10b981; }
      }
    }

    .meter-bars {
      display: flex;
      gap: 4px;
      height: 4px;

      .bar {
        flex: 1;
        background: #e2e8f0;
        border-radius: 9999px;
        transition: background 0.3s;

        &.active.weak { background: #ef4444; }
        &.active.medium { background: #f59e0b; }
        &.active.strong { background: #10b981; }
      }
    }
  }

  .submit-action-row {
    margin-top: 28px;

    .capsule-action-btn {
      width: 100%;
      height: 46px;
      border-radius: 9999px;
      border: none;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;

      &.primary {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 4px 16px rgba(37, 99, 235, 0.25);

        &:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
        }

        &:disabled {
          background: #94a3b8;
          cursor: not-allowed;
          box-shadow: none;
        }
      }
    }
  }

  .forgot-hint-row {
    margin-top: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 13px;
    color: #64748b;

    .divider {
      color: #cbd5e1;
    }

    .link-btn {
      background: none;
      border: none;
      color: #2563eb;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      padding: 0;

      &:hover {
        color: #1d4ed8;
        text-decoration: underline;
      }
    }
  }
}

.unbound-email-empty {
  text-align: center;
  padding: 36px 20px;

  .empty-icon-box {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #eff6ff;
    color: #2563eb;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #64748b;
    max-width: 440px;
    margin: 0 auto 24px;
    line-height: 1.6;
  }

  .empty-actions {
    display: flex;
    justify-content: center;
    gap: 12px;

    .capsule-action-btn {
      height: 44px;
      padding: 0 24px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      border: none;
      transition: all 0.2s;

      &.primary {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.2);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(37, 99, 235, 0.3);
        }
      }

      &.secondary {
        background: #f1f5f9;
        color: #334155;

        &:hover {
          background: #e2e8f0;
        }
      }
    }
  }
}

:deep(.email-bind-dialog) {
  border-radius: 20px;
  overflow: hidden;

  .el-dialog__header {
    margin: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #f1f5f9;
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px 24px;
    border-top: 1px solid #f1f5f9;
  }

  .bind-dialog-header {
    display: flex;
    align-items: center;
    gap: 14px;

    .header-icon-badge {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      background: #eff6ff;
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .header-titles {
      .title {
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 2px;
      }
      .subtitle {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .dialog-body-box {
    display: flex;
    flex-direction: column;
    gap: 18px;

    .dialog-input-group {
      .group-label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 6px;
      }

      .capsule-input-box {
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
        height: 46px;
        background: #f8fafc;
        border: 1px solid #cbd5e1;
        border-radius: 9999px;
        padding: 0 16px;

        &:focus-within {
          border-color: #2563eb;
          background: #ffffff;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
        }

        .input-icon {
          font-size: 16px;
          color: #94a3b8;
          margin-right: 10px;
        }

        .capsule-input {
          flex: 1;
          border: none;
          background: transparent;
          outline: none;
          font-size: 14px;
          color: #0f172a;
          height: 100%;
        }
      }

      .capsule-code-row {
        display: flex;
        gap: 10px;

        .code-input {
          flex: 1;
        }

        .capsule-code-btn {
          height: 46px;
          padding: 0 18px;
          border-radius: 9999px;
          border: none;
          background: linear-gradient(135deg, #2563eb, #1d4ed8);
          color: #ffffff;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          white-space: nowrap;

          &:disabled {
            background: #94a3b8;
            cursor: not-allowed;
          }
        }
      }
    }
  }

  .dialog-footer-row {
    display: flex;
    justify-content: flex-end;
    gap: 12px;

    .dialog-btn {
      height: 42px;
      padding: 0 22px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s;

      &.cancel {
        background: #f1f5f9;
        color: #475569;

        &:hover {
          background: #e2e8f0;
        }
      }

      &.confirm {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);

        &:hover:not(:disabled) {
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
        }

        &:disabled {
          background: #94a3b8;
          cursor: not-allowed;
        }
      }
    }
  }
}
</style>
