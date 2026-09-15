<template>
  <div class="tab-body">
    <div class="card-title-section">
      <p class="card-welcome-title">EduMind AI 教学平台 · 邮箱免密安全登录</p>
    </div>

    <!-- 邮箱登录表单 -->
    <el-form class="login-main-form" @keyup.enter="handleEmailLogin">
      <!-- 邮箱输入框 -->
      <el-form-item>
        <div class="input-container">
          <span class="field-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
          </span>
          <input
            v-model="emailForm.email"
            type="email"
            class="native-input"
            placeholder="请输入绑定的电子邮箱"
            autocomplete="email"
          />
        </div>
      </el-form-item>

      <!-- 邮箱验证码输入框 + 获取验证码组件 -->
      <el-form-item>
        <div class="input-container code-input-container">
          <span class="field-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
          </span>
          <input
            v-model="emailForm.code"
            type="text"
            maxlength="6"
            class="native-input"
            placeholder="请输入 6 位邮箱验证码"
            autocomplete="one-time-code"
          />
          <EmailCodeBtn
            :email="emailForm.email"
            scene="login"
            class="login-email-btn"
          />
        </div>
      </el-form-item>

      <!-- 记住邮箱与切换模式 -->
      <div class="form-util-row">
        <label class="remember-label">
          <input v-model="rememberMe" type="checkbox" class="remember-check" />
          <span class="remember-text">记住邮箱</span>
        </label>
        <span class="forgot-pwd-link pointer" @click="emit('switch-to-account')">
          使用账号密码登录
        </span>
      </div>

      <!-- 邮箱登录主按钮 -->
      <button
        type="button"
        class="primary-login-btn"
        :disabled="loading"
        @click="handleEmailLogin"
      >
        <span v-if="!loading">快速登录</span>
        <span v-else class="btn-loading-text">登录核验中...</span>
      </button>
    </el-form>

    <!-- 底部其他登录方式 -->
    <div class="third-login-wrapper">
      <div class="divider-header">
        <span class="divider-dash"></span>
        <span class="divider-title">其他登录方式</span>
        <span class="divider-dash"></span>
      </div>

      <div class="third-platforms">
        <div class="platform-item wechat" @click="handleThirdLogin('微信')">
          <div class="platform-circle wechat-bg">
            <img :src="wechatIcon" alt="" class="platform-svg" aria-hidden="true" />
          </div>
          <span class="platform-label">微信</span>
        </div>

        <div class="platform-item qywx" @click="handleThirdLogin('企业微信')">
          <div class="platform-circle qywx-bg">
            <img :src="wecomIcon" alt="" class="platform-svg" aria-hidden="true" />
          </div>
          <span class="platform-label">企业微信</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import wechatIcon from '@/assets/icons/wechat.svg';
import wecomIcon from '@/assets/icons/wecom.svg';
import EmailCodeBtn from '@/components/common/EmailCodeBtn.vue';

defineProps<{
  emailForm: { email: string; code: string };
  loading: boolean;
  handleEmailLogin: () => void;
  handleThirdLogin: (platform: string) => void;
}>();

const rememberMe = defineModel<boolean>('rememberMe', { required: true });

const emit = defineEmits<{
  (e: 'switch-to-account'): void;
}>();
</script>

<style scoped lang="scss">
/* 账号登录标题 */
.card-title-section {
  margin-bottom: 22px;
  text-align: center;

  .card-welcome-title {
    font-size: 15px;
    font-weight: 600;
    color: #0a1b39;
    margin: 0;
    letter-spacing: 0.3px;
    line-height: 1.5;
  }
}

/* 原生定制高保真输入控件 (高度 46px，圆角 6px，浅蓝灰边框) */
.login-main-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  .input-container {
    display: flex;
    align-items: center;
    width: 100%;
    height: 48px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 9999px; /* 纯正长圆椭圆边框 */
    padding: 0 20px;
    box-sizing: border-box;
    transition: all 0.2s;

    &:hover {
      border-color: #CBD5E1;
    }

    &:focus-within {
      border-color: #1E293B;
      box-shadow: 0 0 0 1.5px #1E293B;
    }

    .field-icon {
      display: flex;
      align-items: center;
      margin-right: 12px;
      color: #8C9BAE;

      .svg-icon {
        width: 18px;
        height: 18px;
      }
    }

    .native-input {
      flex: 1;
      height: 100%;
      border: none;
      outline: none;
      background: transparent;
      font-size: 14px;
      color: #1E293B;

      &::placeholder {
        color: #8C9BAE;
        font-size: 14px;
      }
    }

    .auth-eye-btn,
    .eye-toggle {
      background: none;
      border: none;
      cursor: pointer;
      color: #94A3B8;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color 0.2s ease;

      &:hover {
        color: #475569;
      }

      svg {
        width: 15px;
        height: 15px;
      }
    }

    &.code-input-container {
      padding-right: 6px;

      .login-email-btn {
        margin-left: 8px;
        height: 38px;
        border-radius: 9999px;
      }
    }
  }

  .pointer {
    cursor: pointer;
  }

  /* 记住账号与忘记密码行 */
  .form-util-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 14px 0 20px 0;

    .remember-label {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      user-select: none;

      .remember-check {
        width: 15px;
        height: 15px;
        accent-color: #1677FF;
        cursor: pointer;
      }

      .remember-text {
        font-size: 13px;
        color: #4A5568;
      }
    }

    .forgot-pwd-link {
      font-size: 13px;
      color: #1677FF;
      text-decoration: none;
      transition: color 0.2s;

      &:hover {
        color: #0958D9;
      }
    }
  }

  /* 登录主按钮 (纯正长圆胶囊边框) */
  .primary-login-btn {
    width: 100%;
    height: 48px;
    border-radius: 9999px; /* 纯正长圆边框 */
    background: #1677FF;
    border: none;
    color: #FFFFFF;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s;

    &:hover {
      background: #0958D9;
    }

    &:active {
      background: #003EB3;
    }

    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  }
}

/* 底部其他登录方式 (设计图圆形徽标 + 文本) */
.third-login-wrapper {
  margin-top: 24px;

  .divider-header {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;

    .divider-dash {
      width: 60px;
      height: 1px;
      background: #E2E8F0;
    }

    .divider-title {
      font-size: 12px;
      color: #8C9BAE;
      margin: 0 12px;
    }
  }

  .third-platforms {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 36px;

    .platform-item {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      transition: opacity 0.2s;

      &:hover {
        opacity: 0.85;
      }

      .platform-circle {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        .platform-svg {
          width: 16px;
          height: 16px;
        }

        &.wechat-bg {
          background: #07C160;
        }

        &.qywx-bg {
          background: #0082EF;
        }
      }

      .platform-label {
        font-size: 13px;
        color: #4A5568;
      }
    }
  }
}

/* 视图淡入微动效 */
.tab-body {
  animation: authFadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes authFadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
