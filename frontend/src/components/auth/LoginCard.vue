<template>
  <div class="login-card-wrapper">
    <div class="login-card">
      <!-- 右上角经典切角扫码/电脑登录切换器 (Corner Switcher) -->
      <div
        class="corner-switch-box"
        :class="{ 'is-qrcode': isQrCodeMode }"
        :title="isQrCodeMode ? '点击切换为账号密码登录' : '点击切换为扫码登录'"
        @click="toggleLoginMode"
      >
        <!-- 折角徽标 (带斜切几何质感背景与 SVG 矢量图标) -->
        <div class="corner-badge">
          <div class="corner-triangle"></div>
          <div class="corner-icon-wrap">
            <!-- 处于扫码模式时展示：电脑显示器图标 (切回密码) -->
            <svg
              v-if="isQrCodeMode"
              viewBox="0 0 24 24"
              class="corner-svg"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
              <line x1="8" y1="21" x2="16" y2="21" />
              <line x1="12" y1="17" x2="12" y2="21" />
            </svg>
            <!-- 处于表单模式时展示：二维码图标 (切换扫码) -->
            <svg
              v-else
              viewBox="0 0 24 24"
              class="corner-svg"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="3" y="3" width="7" height="7" rx="1" />
              <rect x="14" y="3" width="7" height="7" rx="1" />
              <rect x="14" y="14" width="7" height="7" rx="1" />
              <rect x="3" y="14" width="7" height="7" rx="1" />
              <line x1="6.5" y1="6.5" x2="6.5" y2="6.5" stroke-width="2.5" stroke-linecap="round" />
              <line x1="17.5" y1="6.5" x2="17.5" y2="6.5" stroke-width="2.5" stroke-linecap="round" />
              <line x1="6.5" y1="17.5" x2="6.5" y2="17.5" stroke-width="2.5" stroke-linecap="round" />
              <line x1="17.5" y1="17.5" x2="17.5" y2="17.5" stroke-width="2.5" stroke-linecap="round" />
            </svg>
          </div>
        </div>
      </div>

      <!-- 顶部 Tabs 导航：保留账号登录 / 邮箱登录 双模式 (带图标) -->
      <div v-show="!isQrCodeMode" class="card-tabs-header">
        <div
          class="tab-btn"
          :class="{ active: activeTab === 'account' }"
          @click="activeTab = 'account'"
        >
          <svg viewBox="0 0 24 24" class="tab-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
          <span>账号登录</span>
          <div v-if="activeTab === 'account'" class="tab-indicator"></div>
        </div>
        <div
          class="tab-btn"
          :class="{ active: activeTab === 'email' }"
          @click="activeTab = 'email'"
        >
          <svg viewBox="0 0 24 24" class="tab-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
            <polyline points="22,6 12,13 2,6" />
          </svg>
          <span>邮箱登录</span>
          <div v-if="activeTab === 'email'" class="tab-indicator"></div>
        </div>
        <div class="tabs-base-line"></div>
      </div>

      <!-- 模式 A：账号密码登录 -->
      <LoginPasswordForm
        v-show="!isQrCodeMode && activeTab === 'account'"
        v-model:remember-me="rememberMe"
        v-model:show-password="showPassword"
        :login-form="loginForm"
        :loading="loading"
        :captcha-type="captchaType"
        :captcha-img="captchaImg"
        :refresh-captcha="refreshCaptcha"
        :handle-login="handleLogin"
        :handle-third-login="handleThirdLogin"
      />

      <!-- 模式 C：邮箱验证码快捷登录 -->
      <LoginEmailForm
        v-show="!isQrCodeMode && activeTab === 'email'"
        v-model:remember-me="rememberMe"
        :email-form="emailForm"
        :loading="loading"
        :handle-email-login="handleEmailLogin"
        :handle-third-login="handleThirdLogin"
        @switch-to-account="activeTab = 'account'"
      />

      <!-- 模式 B：扫码登录 (右上角切角触发) -->
      <LoginQrPanel
        v-show="isQrCodeMode"
        :handle-mock-qr-login="handleMockQrLogin"
        @back-to-account="isQrCodeMode = false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import LoginPasswordForm from '@/components/auth/LoginPasswordForm.vue';
import LoginEmailForm from '@/components/auth/LoginEmailForm.vue';
import LoginQrPanel from '@/components/auth/LoginQrPanel.vue';

defineProps<{
  loginForm: { username: string; password: string; captcha: string };
  emailForm: { email: string; code: string };
  loading: boolean;
  captchaType: 'image' | 'slider';
  captchaImg: string;
  toggleLoginMode: () => void;
  refreshCaptcha: () => void;
  handleLogin: () => void;
  handleEmailLogin: () => void;
  handleMockQrLogin: () => void;
  handleThirdLogin: (platform: string) => void;
}>();

const isQrCodeMode = defineModel<boolean>('isQrCodeMode', { required: true });
const activeTab = defineModel<'account' | 'email'>('activeTab', { required: true });
const rememberMe = defineModel<boolean>('rememberMe', { required: true });
const showPassword = defineModel<boolean>('showPassword', { required: true });
</script>

<style scoped lang="scss">
/* 登录卡片容器 (精确定位与宽度：416px，严丝合缝对齐登录1.png) */
.login-card-wrapper {
  position: relative;
  z-index: 10;
}

.login-card {
  position: relative;
  overflow: hidden; /* 保证右上角折角完美契合卡片 16px 圆角 */
  width: 416px;
  background: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 12px 36px rgba(16, 68, 148, 0.08), 0 2px 8px rgba(0, 0, 0, 0.02);
  padding: 34px 34px 28px;
  box-sizing: border-box;

  /* 右上角经典切角扫码/密码登录切换器 (Corner Switcher) */
  .corner-switch-box {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 20;
    cursor: pointer;
    display: flex;
    align-items: center;
    user-select: none;

    /* 折角三角形徽标 */
    .corner-badge {
      position: relative;
      width: 60px;
      height: 60px;
      display: flex;
      align-items: flex-start;
      justify-content: flex-end;

      .corner-triangle {
        position: absolute;
        top: 0;
        right: 0;
        width: 100%;
        height: 100%;
        clip-path: polygon(0 0, 100% 0, 100% 100%);
        background: linear-gradient(135deg, #F0F7FF 0%, #E1EFFE 100%);
        box-shadow: -2px 2px 6px rgba(16, 68, 148, 0.06);
        transition: all 0.25s ease;
      }

      .corner-icon-wrap {
        position: absolute;
        top: 8px;
        right: 8px;
        z-index: 2;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #1677FF;
        transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);

        .corner-svg {
          width: 21px;
          height: 21px;
          display: block;
        }
      }
    }

    &:hover {
      .corner-badge {
        .corner-triangle {
          background: linear-gradient(135deg, #E0EEFD 0%, #C7E2FE 100%);
        }

        .corner-icon-wrap {
          transform: scale(1.15);
          color: #0958D9;
        }
      }
    }

    /* 扫码模式激活状态下的细微调优 */
    &.is-qrcode {
      .corner-icon-wrap {
        color: #2563EB;
      }
    }
  }

  /* 顶部两列 Tabs 切换 (账号登录 / 邮箱登录) */
  .card-tabs-header {
    position: relative;
    display: flex;
    justify-content: center;
    gap: 40px;
    margin-bottom: 24px;

    .tab-btn {
      position: relative;
      font-size: 16px;
      color: #4A5568;
      font-weight: 500;
      padding-bottom: 12px;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: color 0.2s;

      .tab-icon {
        width: 17px;
        height: 17px;
        stroke-width: 1.8;
        color: currentColor;
        transition: transform 0.2s ease;
      }

      &:hover {
        color: #1677FF;

        .tab-icon {
          transform: scale(1.08);
        }
      }

      &.active {
        color: #1677FF;
        font-weight: 600;
      }

      .tab-indicator {
        position: absolute;
        bottom: 0px;
        left: 50%;
        transform: translateX(-50%);
        width: 82px;
        height: 3px;
        background: #1677FF;
        border-radius: 2px;
        z-index: 2;
      }
    }

    .tabs-base-line {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: #EBF1F7;
      z-index: 1;
    }
  }
}

@media screen and (max-width: 1024px) {
  .login-card {
    width: 90%;
    max-width: 416px;
    padding: 28px 24px;
  }
}
</style>
