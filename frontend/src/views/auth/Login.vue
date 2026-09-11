<template>
  <div class="login-container">

    <!-- 右侧高保真登录卡片 (严格 1:1 对齐 docs/登录1.png) -->
    <div class="login-card-wrapper">
      <div class="login-card">
        <!-- 顶部 Tabs 导航：账号登录 / 扫码登录 -->
        <div class="card-tabs-header">
          <div
            class="tab-btn"
            :class="{ active: activeTab === 'account' }"
            @click="activeTab = 'account'"
          >
            <span>账号登录</span>
            <div v-if="activeTab === 'account'" class="tab-indicator"></div>
          </div>
          <div
            class="tab-btn"
            :class="{ active: activeTab === 'qrcode' }"
            @click="activeTab = 'qrcode'"
          >
            <span>扫码登录</span>
            <div v-if="activeTab === 'qrcode'" class="tab-indicator"></div>
          </div>
          <div class="tabs-base-line"></div>
        </div>

        <!-- 模式 A：账号密码登录 -->
        <div v-show="activeTab === 'account'" class="tab-body">
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
                <span class="eye-toggle" @click="showPassword = !showPassword">
                  <svg v-if="!showPassword" viewBox="0 0 24 24" class="svg-icon eye-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="svg-icon eye-icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="7" r="3"></circle>
                  </svg>
                </span>
              </div>
            </el-form-item>

            <!-- 图形验证码 (与云盘 AuthCaptchaField 模式一致) -->
            <el-form-item class="auth-captcha-item">
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

        <!-- 模式 B：扫码登录 -->
        <div v-show="activeTab === 'qrcode'" class="tab-body qrcode-body">
          <p class="card-sub-title qrcode-sub-title">使用手机微信或企业微信扫一扫快速登录</p>

          <div class="scan-box-panel">
            <div class="qr-frame">
              <div class="scan-laser-line"></div>
              <!-- 真实标准高清矢量二维码 -->
              <qrcode-vue
                value="https://edumind.ai/auth/scan-login"
                :size="150"
                level="H"
                render-as="svg"
                class="real-qrcode-svg"
              />
            </div>
            <p class="qr-guidance">请打开 <span class="blue-text">微信 / 企业微信</span> 扫码</p>

            <button type="button" class="mock-pass-btn" @click="handleMockQrLogin">
              模拟扫码授权通过 (Demo)
            </button>
          </div>

          <div class="back-account-btn" @click="activeTab = 'account'">
            返回账号密码登录
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import QrcodeVue from 'qrcode.vue';
import wechatIcon from '@/assets/icons/wechat.svg';
import wecomIcon from '@/assets/icons/wecom.svg';
import { useAuthStore } from '@/stores/auth/auth';
import { USE_MOCK } from '@/config/mock';
import { MOCK_USERS } from '@/mock/users';
import AuthCaptchaField from '@/components/common/AuthCaptchaField.vue';
import { getCaptcha, login as loginApi } from '@/api/auth/auth';
import { toCaptchaDataUrl } from '@/utils/captcha';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 状态管理
const activeTab = ref<'account' | 'qrcode'>('account');
const rememberMe = ref(false);
const showPassword = ref(false);
const loading = ref(false);

// 验证码状态 (云盘同款：后端 Hutool LineCaptcha 130×48)
const captchaId = ref('');
const captchaImg = ref('');

// 表单数据 (默认全部置空，由用户手动输入)
const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
});

// 刷新验证码 (云盘同款：仅走后端 Hutool LineCaptcha)
const refreshCaptcha = async () => {
  loginForm.captcha = '';
  try {
    const res = await getCaptcha();
    captchaId.value = res.data.id;
    captchaImg.value = toCaptchaDataUrl(res.data.img);
  } catch {
    captchaImg.value = '';
  }
};

// 登录提交 (云盘同款：ElMessage 提示，不显示表单内红色错误)
const handleLogin = async () => {
  if (loading.value) return;

  const username = loginForm.username.trim();
  const password = loginForm.password;
  const captcha = loginForm.captcha.trim();

  if (!username || !password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  if (!captcha) {
    ElMessage.warning('请完成图形验证码');
    return;
  }

  loading.value = true;
  try {
    const res = await loginApi({
      username,
      password,
      captcha,
      captchaId: captchaId.value
    });
    if (res?.data?.token) {
      authStore.setToken(res.data.token);
      if (res.data.userInfo) {
        authStore.setUser({
          id: res.data.userInfo.id,
          username: res.data.userInfo.username,
          realName: res.data.userInfo.realName || res.data.userInfo.username,
          avatar: res.data.userInfo.avatar || '',
          roles: (res.data.userInfo.roles || []) as any,
          permissions: res.data.userInfo.permissions || [],
          department: res.data.userInfo.department,
          email: res.data.userInfo.email
        });
      } else {
        await authStore.fetchUserInfo();
      }
    } else if (USE_MOCK) {
      const matchedRole = Object.keys(MOCK_USERS).find(
        r => MOCK_USERS[r as 'ADMIN' | 'TEACHER' | 'STUDENT'].username === username
      ) as 'ADMIN' | 'TEACHER' | 'STUDENT' | undefined;
      if (matchedRole) {
        authStore.switchMockRole(matchedRole);
      } else {
        throw new Error('登录失败');
      }
    } else {
      throw new Error('登录失败');
    }

    if (rememberMe.value) {
      localStorage.setItem('edumind_saved_username', username);
    } else {
      localStorage.removeItem('edumind_saved_username');
    }

    ElMessage.success({
      message: `登录成功！欢迎使用 EduMind 平台，${authStore.currentUser?.realName || ''}`,
      type: 'success'
    });

    const redirect = (route.query.redirect as string) || '/dashboard';
    router.push(redirect);
  } catch {
    // 业务错误已由 axios 全局拦截器统一 ElMessage 提示，此处仅刷新验证码
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

const handleMockQrLogin = async () => {
  if (loading.value) return;
  loading.value = true;

  try {
    // 联调模式：模拟扫码成功后，以默认管理员账号完成真实登录（验证码可省略）
    const res = await loginApi({
      username: 'admin',
      password: 'admin123',
      captcha: '',
      captchaId: ''
    });

    if (res?.data?.token) {
      authStore.setToken(res.data.token);
      if (res.data.userInfo) {
        authStore.setUser({
          id: res.data.userInfo.id,
          username: res.data.userInfo.username,
          realName: res.data.userInfo.realName || res.data.userInfo.username,
          avatar: res.data.userInfo.avatar || '',
          roles: (res.data.userInfo.roles || []) as any,
          permissions: res.data.userInfo.permissions || [],
          department: res.data.userInfo.department,
          email: res.data.userInfo.email
        });
      } else {
        await authStore.fetchUserInfo();
      }

      ElMessage.success('扫码授权成功，正在载入管理控制台...');
      const redirect = (route.query.redirect as string) || '/dashboard';
      await router.push(redirect);
      return;
    }

    throw new Error('登录失败');
  } catch {
    // 纯 Mock 开发兜底（VITE_USE_MOCK=true）
    if (USE_MOCK) {
      authStore.switchMockRole('ADMIN');
      ElMessage.success('扫码授权成功，正在载入管理控制台...');
      await router.push('/dashboard');
      return;
    }
    ElMessage.error('扫码登录失败，请使用账号密码登录');
  } finally {
    loading.value = false;
  }
};

const handleThirdLogin = (platform: string) => {
  ElMessage.info(`正在连接【${platform}】高校统一身份认证 (SSO)...`);
};

onMounted(() => {
  refreshCaptcha();
});
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  min-height: 600px;
  /* 使用优化扩展后的 2:1 纯净背景，机器人整体靠左，右侧预留出整洁空白 */
  background: #EAF3FD url('@/assets/images/登录2.png') no-repeat left center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  /* 距离右侧刚好 4.8vw (~70px)，彻底远离左侧悬浮标签与机器人，绝不遮挡 */
  padding-right: clamp(30px, 4.8vw, 88px);
  box-sizing: border-box;
  overflow: hidden;


  /* 登录卡片容器 (精确定位与宽度：416px，严丝合缝对齐登录1.png) */
  .login-card-wrapper {
    position: relative;
    z-index: 10;
  }

  .login-card {
    width: 416px;
    background: #FFFFFF;
    border-radius: 16px;
    box-shadow: 0 12px 36px rgba(16, 68, 148, 0.08), 0 2px 8px rgba(0, 0, 0, 0.02);
    padding: 34px 34px 28px;
    box-sizing: border-box;

    /* 顶部两列 Tabs 切换 */
    .card-tabs-header {
      position: relative;
      display: flex;
      justify-content: space-around;
      margin-bottom: 24px;

      .tab-btn {
        position: relative;
        font-size: 16px;
        color: #4A5568;
        font-weight: 500;
        padding-bottom: 12px;
        cursor: pointer;
        transition: color 0.2s;

        &:hover {
          color: #1677FF;
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
          width: 68px;
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

        .eye-toggle {
          cursor: pointer;
          display: flex;
          align-items: center;
          color: #8C9BAE;
          transition: color 0.2s;

          &:hover {
            color: #1677FF;
          }

          .eye-icon {
            width: 18px;
            height: 18px;
          }
        }
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

    /* 扫码模式 */
    .qrcode-body {
      display: flex;
      flex-direction: column;
      align-items: center;

      .qrcode-sub-title {
        font-size: 13px;
        color: #6e7a8a;
        margin: 0 0 16px;
        text-align: center;
      }

      .scan-box-panel {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 10px 0;

        .qr-frame {
          position: relative;
          width: 170px;
          height: 170px;
          background: #FFFFFF;
          border: 1px solid #D9E2EC;
          border-radius: 10px;
          padding: 9px;
          box-sizing: border-box;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;

          .scan-laser-line {
            position: absolute;
            left: 0;
            right: 0;
            height: 2px;
            background: linear-gradient(90deg, transparent, #1677FF, transparent);
            box-shadow: 0 0 8px #1677FF;
            animation: scanMove 2.2s ease-in-out infinite;
            z-index: 5;
          }

          .real-qrcode-svg {
            display: block;
            width: 150px;
            height: 150px;
            border-radius: 4px;
          }
        }

        .qr-guidance {
          font-size: 13px;
          color: #6E7A8A;
          margin: 14px 0 10px;

          .blue-text {
            color: #1677FF;
            font-weight: 600;
          }
        }

        .mock-pass-btn {
          padding: 8px 20px;
          border: 1px solid #93C5FD;
          background: #EFF6FF;
          color: #1677FF;
          border-radius: 9999px;
          font-size: 12px;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            background: #DBEAFE;
          }
        }
      }

      .back-account-btn {
        margin-top: 14px;
        font-size: 13px;
        color: #6E7A8A;
        cursor: pointer;
        transition: color 0.2s;

        &:hover {
          color: #1677FF;
        }
      }
    }
  }
}

@keyframes scanMove {
  0% { top: 4px; opacity: 0.7; }
  50% { top: calc(100% - 6px); opacity: 1; }
  100% { top: 4px; opacity: 0.7; }
}

@media screen and (max-width: 1024px) {
  .login-container {
    justify-content: center;
    padding-right: 0;

    .login-card {
      width: 90%;
      max-width: 416px;
      padding: 28px 24px;
    }
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
