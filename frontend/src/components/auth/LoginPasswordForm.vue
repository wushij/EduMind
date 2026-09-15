<template>
  <div class="tab-body">
    <div class="card-title-section">
      <p class="card-welcome-title">欢迎使用 EduMind AI 智能教学赋能平台</p>
    </div>

    <!-- 登录表单 -->
    <el-form
      class="login-main-form"
      @keyup.enter="handleLogin"
    >
      <!-- 用户名输入框 -->
      <el-form-item>
        <div class="input-container">
          <span class="field-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </span>
          <input
            v-model="loginForm.username"
            type="text"
            class="native-input"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>
      </el-form-item>

      <!-- 密码输入框 -->
      <el-form-item>
        <div class="input-container">
          <span class="field-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
          </span>
          <input
            v-model="loginForm.password"
            :type="showPassword ? 'text' : 'password'"
            class="native-input"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
          <button
            type="button"
            class="auth-eye-btn"
            tabindex="-1"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            @click="showPassword = !showPassword"
          >
            <svg
              v-if="showPassword"
              viewBox="0 0 24 24"
              width="15"
              height="15"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              width="15"
              height="15"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
              <line x1="3" y1="4" x2="21" y2="20" />
            </svg>
          </button>
        </div>
      </el-form-item>

      <!-- 图形验证码 (当系统策略为字符图片验证码时展示) -->
      <el-form-item v-if="captchaType === 'image'" class="auth-captcha-item">
        <AuthCaptchaField
          v-model="loginForm.captcha"
          :captcha-img="captchaImg"
          @refresh="refreshCaptcha"
          @enter="handleLogin"
        />
      </el-form-item>

      <!-- 记住账号与忘记密码 -->
      <div class="form-util-row">
        <label class="remember-label">
          <input v-model="rememberMe" type="checkbox" class="remember-check" />
          <span class="remember-text">记住账号</span>
        </label>
        <router-link to="/auth/forgot-password" class="forgot-pwd-link">
          忘记密码？
        </router-link>
      </div>

      <!-- 登录主按钮 (与登录1.png完全一致) -->
      <button
        type="button"
        class="primary-login-btn"
        :disabled="loading"
        @click="handleLogin"
      >
        <span v-if="!loading">登录</span>
        <span v-else class="btn-loading-text">登录中...</span>
      </button>
    </el-form>

    <!-- 底部其他登录方式 (对齐设计图：微信、企业微信圆图标) -->
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
import AuthCaptchaField from '@/components/common/AuthCaptchaField.vue';

defineProps<{
  loginForm: { username: string; password: string; captcha: string };
  loading: boolean;
  captchaType: 'image' | 'slider';
  captchaImg: string;
  refreshCaptcha: () => void;
  handleLogin: () => void;
  handleThirdLogin: (platform: string) => void;
}>();

const rememberMe = defineModel<boolean>('rememberMe', { required: true });
const showPassword = defineModel<boolean>('showPassword', { required: true });
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

/* AuthCaptchaField 内 el-input 长圆样式 (对齐云盘 Login.vue) */
:deep(.auth-captcha-item .el-input__wrapper) {
  border-radius: 999px !important;
  background-color: #f8fafc !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  padding: 4px 20px !important;
  transition: all 0.2s ease !important;
}

:deep(.auth-captcha-item .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset !important;
  background-color: #ffffff !important;
}

:deep(.auth-captcha-item .el-input__wrapper.is-focus) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 2px #0f172a inset, 0 0 12px rgba(15, 23, 42, 0.1) !important;
}

:deep(.auth-captcha-item .el-input__inner) {
  font-size: 14px !important;
  color: #0f172a !important;
}
</style>
