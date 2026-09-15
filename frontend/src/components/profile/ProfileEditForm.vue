<template>
  <div class="profile-panel-card">
    <div class="panel-header-line">
      <el-icon class="panel-icon"><User /></el-icon>
      <h3 class="panel-title">基本信息与个人资料</h3>
    </div>

    <el-form label-position="top" class="profile-capsule-form">
      <el-form-item label="登录账号 (唯一识别码)">
        <input
          :value="currentUser?.username"
          type="text"
          class="capsule-input"
          disabled
        />
      </el-form-item>

      <el-form-item label="真实姓名">
        <input
          v-model="profileForm.realName"
          type="text"
          class="capsule-input"
          placeholder="请输入您的真实姓名"
        />
      </el-form-item>

      <el-form-item label="所属院系 / 教学机构">
        <input
          v-model="profileForm.department"
          type="text"
          class="capsule-input"
          placeholder="例如：计算机科学与工程学院"
        />
      </el-form-item>

      <el-form-item label="安全联系邮箱">
        <div class="email-status-card">
          <span class="email-address">
            {{ currentUser?.email ? maskEmail(currentUser.email) : '未绑定电子邮箱' }}
          </span>
          <button
            type="button"
            class="email-action-btn"
            @click="openBindDialog"
          >
            {{ currentUser?.email ? '更换绑定' : '立即绑定' }}
          </button>
        </div>
      </el-form-item>

      <el-form-item label="联系电话">
        <input
          v-model="profileForm.phone"
          type="tel"
          class="capsule-input"
          placeholder="请输入 11 位手机号码"
        />
      </el-form-item>

      <el-form-item label="教学与学术研究方向简介">
        <textarea
          v-model="profileForm.bio"
          rows="3"
          class="capsule-textarea"
          placeholder="简述您的授课领域或主要研究兴趣..."
        ></textarea>
      </el-form-item>

      <div class="form-submit-row">
        <button
          type="button"
          class="capsule-save-btn"
          @click="handleSaveProfile"
        >
          <span>保存个人资料修改</span>
        </button>
      </div>
    </el-form>

    <!-- 邮箱安全绑定 / 更换弹窗 (深度美化升级) -->
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
            <h3 class="title">{{ currentUser?.email ? '更换安全绑定邮箱' : '绑定账户安全邮箱' }}</h3>
            <span class="subtitle">绑定后可用于接收安全验证码及敏感操作身份核验</span>
          </div>
        </div>
      </template>

      <div class="bind-dialog-body">
        <div class="dialog-notice-box">
          <el-icon class="notice-icon"><InfoFilled /></el-icon>
          <span>电子邮箱为账号首要密保渠道，绑定成功后可用于密码找回与极速安全登录。</span>
        </div>

        <el-form label-position="top" class="bind-dialog-form">
          <el-form-item label="电子邮箱地址">
            <el-input
              v-model="bindForm.email"
              type="email"
              size="large"
              placeholder="请输入有效的电子邮箱 (例如: name@univ.edu.cn)"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>

          <el-form-item label="邮箱专属验证码">
            <div class="dialog-code-row">
              <el-input
                v-model="bindForm.code"
                type="text"
                maxlength="6"
                size="large"
                placeholder="请输入 6 位验证码"
                :prefix-icon="Message"
                class="code-input"
              />
              <EmailCodeBtn
                :email="bindForm.email"
                scene="bind"
                size="large"
                class="dialog-code-btn"
              />
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer-actions">
          <el-button size="large" @click="bindDialogVisible = false">
            取消
          </el-button>
          <el-button
            type="primary"
            size="large"
            :loading="bindingLoading"
            @click="handleConfirmBind"
          >
            {{ bindingLoading ? '正在核验绑定...' : '确认绑定' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { User, Message, InfoFilled } from '@element-plus/icons-vue';
import EmailCodeBtn from '@/components/common/EmailCodeBtn.vue';
import type { UserInfo } from '@/types/auth/auth';

const bindDialogVisible = defineModel<boolean>('bindDialogVisible', { required: true });

defineProps<{
  currentUser: UserInfo | null;
  profileForm: {
    realName: string;
    department: string;
    phone: string;
    bio: string;
  };
  bindingLoading: boolean;
  bindForm: {
    email: string;
    code: string;
  };
  maskEmail: (email: string) => string;
  handleSaveProfile: () => void;
  openBindDialog: () => void;
  handleConfirmBind: () => void;
}>();
</script>

<style scoped lang="scss">
.profile-panel-card {
  background: #FFFFFF;
  border-radius: 18px;
  border: 1px solid #E2E8F0;
  padding: 24px 28px;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  margin-bottom: 20px;

  .panel-header-line {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 1px solid #F1F5F9;

    .panel-icon {
      font-size: 18px;
      color: #1677FF;
      display: inline-flex;
      align-items: center;
    }

    .panel-title {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
    }
  }

  // 表单输入
  .profile-capsule-form {
    :deep(.el-form-item) {
      margin-bottom: 16px;
    }

    .capsule-input {
      width: 100%;
      height: 42px;
      border-radius: 9999px; // 长圆输入框
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      padding: 0 18px;
      font-size: 13.5px;
      color: #1E293B;
      outline: none;
      box-sizing: border-box;
      transition: all 0.2s;

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }

      &:disabled {
        background: #F8FAFC;
        color: #94A3B8;
        cursor: not-allowed;
      }
    }

    .email-status-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      min-height: 46px;
      padding: 0 8px 0 18px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 9999px;
      box-sizing: border-box;

      .email-address {
        flex: 1;
        min-width: 0;
        font-size: 14px;
        color: #1E293B;
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
        background: #FFFFFF;
        border: 1px solid #CBD5E1;
        color: #1677FF;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #1677FF;
          border-color: #1677FF;
          color: #FFFFFF;
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);
        }
      }
    }

    .capsule-textarea {
      width: 100%;
      border-radius: 14px;
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      padding: 12px 16px;
      font-size: 13px;
      color: #1E293B;
      outline: none;
      box-sizing: border-box;
      font-family: inherit;

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }
    }

    .form-submit-row {
      margin-top: 10px;

      .capsule-save-btn {
        height: 42px;
        padding: 0 26px;
        border-radius: 9999px; // 胶囊保存
        background: #1677FF;
        color: #FFFFFF;
        border: none;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.25);
        transition: all 0.2s;

        &:hover {
          background: #4096FF;
        }
      }
    }
  }
}

// 邮箱绑定弹窗深度美化
:deep(.email-bind-dialog),
.email-bind-dialog {
  border-radius: 20px !important;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #F1F5F9;

    .bind-dialog-header {
      display: flex;
      align-items: center;
      gap: 12px;

      .header-icon-badge {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        border: 1px solid #BFDBFE;
      }

      .header-titles {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .subtitle {
          font-size: 12px;
          color: #64748B;
        }
      }
    }
  }

  .el-dialog__body {
    padding: 20px 24px 8px;
  }

  .dialog-notice-box {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 16px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 14px;
    margin-bottom: 20px;
    color: #1E40AF;
    font-size: 12.5px;
    line-height: 1.5;

    .notice-icon {
      font-size: 16px;
      color: #1677FF;
      margin-top: 2px;
      flex-shrink: 0;
    }
  }

  .bind-dialog-form {
    .el-form-item {
      margin-bottom: 18px;

      :deep(.el-form-item__label) {
        font-size: 13px;
        font-weight: 600;
        color: #1E293B;
        padding-bottom: 6px;
      }
    }

    .dialog-code-row {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;

      .code-input {
        flex: 1;

        :deep(.el-input__inner) {
          letter-spacing: 2px;
          font-family: monospace;
          font-weight: 600;
        }
      }

      .dialog-code-btn {
        flex-shrink: 0;
        white-space: nowrap;
      }
    }
  }

  .el-dialog__footer {
    padding: 14px 24px 18px;
    border-top: 1px solid #F1F5F9;
    background: #F8FAFC;

    .dialog-footer-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }
}
</style>
