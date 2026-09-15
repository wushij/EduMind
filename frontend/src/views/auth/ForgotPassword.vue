<template>
  <div class="forgot-container">
    <div class="forgot-bg-mask"></div>

    <div class="forgot-card-box">
      <div class="forgot-card">
        <!-- 顶部返回与标题 -->
        <div class="card-header">
          <div class="back-link" @click="goBackToLogin">
            <el-icon><Back /></el-icon>
            <span>返回登录</span>
          </div>
          <h2 class="card-title">找回账号密码</h2>
          <p class="card-subtitle">通过绑定的安全邮箱获取验证码，安全重置您的登录密码</p>
        </div>

        <!-- 步骤进度指示条 -->
        <div class="steps-wrapper">
          <el-steps :active="currentStep" finish-status="success" align-center>
            <el-step title="验证身份" />
            <el-step title="重置密码" />
            <el-step title="完成" />
          </el-steps>
        </div>

        <!-- 步骤 1：验证身份 -->
        <div v-if="currentStep === 0" class="step-content">
          <el-form class="reset-form" @submit.prevent="handleVerifyStep1">
            <el-form-item>
              <el-input
                v-model="step1Form.email"
                placeholder="请输入注册或绑定的电子邮箱"
                :prefix-icon="Message"
                size="large"
                clearable
                @keyup.enter="handleVerifyStep1"
              />
            </el-form-item>

            <el-form-item>
              <div class="sms-code-box">
                <el-input
                  v-model="step1Form.code"
                  placeholder="请输入 6 位邮箱验证码"
                  :prefix-icon="Lock"
                  maxlength="6"
                  size="large"
                  @keyup.enter="handleVerifyStep1"
                />
                <button
                  type="button"
                  class="get-code-btn"
                  :disabled="countdown > 0 || sendingCode"
                  @click="handleSendEmailCode"
                >
                  <span v-if="sendingCode">发送中...</span>
                  <span v-else-if="countdown > 0">{{ countdown }}s 后重新获取</span>
                  <span v-else>获取验证码</span>
                </button>
              </div>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="action-btn"
              :loading="submitting"
              @click="handleVerifyStep1"
            >
              下一步，重置密码
            </el-button>
          </el-form>
        </div>

        <!-- 步骤 2：设置新密码 -->
        <div v-else-if="currentStep === 1" class="step-content">
          <el-form class="reset-form" @submit.prevent="handleResetPassword">
            <el-form-item>
              <el-input
                v-model="step2Form.newPassword"
                type="password"
                placeholder="请输入新密码 (不少于6位)"
                :prefix-icon="Lock"
                show-password
                size="large"
              />
            </el-form-item>

            <el-form-item>
              <el-input
                v-model="step2Form.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                :prefix-icon="Lock"
                show-password
                size="large"
                @keyup.enter="handleResetPassword"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="action-btn"
              :loading="submitting"
              @click="handleResetPassword"
            >
              确认修改并提交
            </el-button>
          </el-form>
        </div>

        <!-- 步骤 3：完成提示 -->
        <div v-else class="step-content success-content">
          <div class="success-icon-box">
            <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
          </div>
          <h3 class="success-title">密码重置成功</h3>
          <p class="success-desc">您的新密码已生效，请使用新密码重新登录 EduMind 智教云平台。</p>

          <el-button
            type="primary"
            size="large"
            class="action-btn"
            @click="goBackToLogin"
          >
            立即返回登录
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Message, Lock, Back, CircleCheckFilled } from '@element-plus/icons-vue';
import { useForgotPassword } from '@/composables/auth/useForgotPassword';

const {
  currentStep,
  submitting,
  sendingCode,
  countdown,
  step1Form,
  step2Form,
  handleSendEmailCode,
  handleVerifyStep1,
  handleResetPassword,
  goBackToLogin
} = useForgotPassword();
</script>

<style scoped lang="scss">
.forgot-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  min-height: 600px;
  background: #EAF3FD url('@/assets/images/登录2.png') no-repeat left center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  /* 严格 1:1 对齐登录页 Login.vue：距离右侧 4.8vw (~70px)，彻底远离左侧机器人 */
  padding-right: clamp(30px, 4.8vw, 88px);
  box-sizing: border-box;
  overflow: hidden;

  .forgot-bg-mask {
    display: none;
  }

  .forgot-card-box {
    position: relative;
    z-index: 10;
    animation: fadeIn 0.4s ease-out;
  }

  .forgot-card {
    width: 416px;
    background: #FFFFFF;
    border-radius: 16px;
    box-shadow: 0 12px 36px rgba(16, 68, 148, 0.08), 0 2px 8px rgba(0, 0, 0, 0.02);
    padding: 32px 34px 28px;
    box-sizing: border-box;

    .card-header {
      margin-bottom: 22px;

      .back-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #64748B;
        cursor: pointer;
        margin-bottom: 14px;
        transition: color 0.2s;

        &:hover {
          color: #1677FF;
        }
      }

      .card-title {
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 6px 0;
      }

      .card-subtitle {
        font-size: 13px;
        color: #64748B;
        margin: 0;
      }
    }

    .steps-wrapper {
      margin-bottom: 24px;

      :deep(.el-step__title) {
        font-size: 12px;
      }
    }

    .reset-form {
      .el-form-item {
        margin-bottom: 20px;
      }

      .sms-code-box {
        display: flex;
        gap: 12px;
        width: 100%;

        .get-code-btn {
          white-space: nowrap;
          border-radius: 9999px !important;
          min-width: 108px;
          height: 46px;
          font-size: 13.5px;
          font-weight: 600;
          color: #FFFFFF !important;
          background: #1677FF !important;
          border: none;
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);
          cursor: pointer;
          transition: all 0.2s ease;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 0 16px;
          user-select: none;
          letter-spacing: 0.3px;
          box-sizing: border-box;

          span {
            color: #FFFFFF !important;
            font-weight: 600;
            font-size: 13.5px;
          }

          &:hover:not(:disabled) {
            background: #0958D9 !important;
            box-shadow: 0 4px 14px rgba(22, 119, 255, 0.38);
          }

          &:active:not(:disabled) {
            background: #003EB3 !important;
          }

          &:disabled {
            background: #F1F5F9 !important;
            color: #94A3B8 !important;
            border: 1px solid #E2E8F0 !important;
            box-shadow: none !important;
            cursor: not-allowed;

            span {
              color: #94A3B8 !important;
            }
          }
        }
      }

      .action-btn {
        width: 100%;
        height: 46px;
        border-radius: 9999px !important;
        font-size: 15px;
        font-weight: 600;
        background: linear-gradient(135deg, #1677FF 0%, #0958D9 100%);
        border: none;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);
        margin-top: 6px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          box-shadow: 0 6px 20px rgba(22, 119, 255, 0.4);
        }
      }
    }

    .success-content {
      text-align: center;
      padding: 10px 0;

      .success-icon-box {
        .check-icon {
          font-size: 56px;
          color: #52C41A;
        }
      }

      .success-title {
        font-size: 18px;
        color: #0F172A;
        font-weight: 700;
        margin: 12px 0 6px;
      }

      .success-desc {
        font-size: 13px;
        color: #64748B;
        margin-bottom: 24px;
        line-height: 1.5;
      }

      .action-btn {
        width: 100%;
        height: 46px;
        border-radius: 9999px !important;
        font-size: 15px;
        font-weight: 600;
      }
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media screen and (max-width: 1024px) {
  .forgot-container {
    justify-content: center;
    padding-right: 0;

    .forgot-card-box {
      width: 90%;
      max-width: 416px;
    }

    .forgot-card {
      width: 100%;
      padding: 28px 24px;
    }
  }
}
</style>
