<template>
  <div class="login-container">

    <!-- 右侧高保真登录卡片 (严格 1:1 对齐 docs/登录1.png) -->
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
        <div v-show="!isQrCodeMode && activeTab === 'account'" class="tab-body">
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

        <!-- 模式 C：邮箱验证码快捷登录 -->
        <div v-show="!isQrCodeMode && activeTab === 'email'" class="tab-body">
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
              <span class="forgot-pwd-link pointer" @click="activeTab = 'account'">
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

        <!-- 模式 B：扫码登录 (右上角切角触发) -->
        <div v-show="isQrCodeMode" class="tab-body qrcode-body">
          <div class="card-title-section qrcode-header">
            <p class="card-welcome-title">扫码安全登录</p>
            <p class="card-sub-title qrcode-sub-title">请使用微信或企业微信扫一扫快捷登录</p>
          </div>

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

          <div class="back-account-btn" @click="isQrCodeMode = false">
            返回账号密码登录
          </div>
        </div>
      </div>
    </div>
    <SliderCaptchaModal
      v-model="sliderModalVisible"
      :username="loginForm.username.trim()"
      @success="handleSliderSuccess"
    />
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
import SliderCaptchaModal from '@/components/common/SliderCaptchaModal.vue';
import EmailCodeBtn from '@/components/common/EmailCodeBtn.vue';
import { getCaptcha, getCaptchaPolicy, login as loginApi, emailLogin as emailLoginApi } from '@/api/auth/auth';
import { toCaptchaDataUrl } from '@/utils/captcha';
import { storage } from '@/core/storage/local';
import { TENANT_ID_KEY } from '@/stores/system/tenant';
import type { LoginResult } from '@/types/auth/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 状态管理
const isQrCodeMode = ref(false);
const activeTab = ref<'account' | 'email'>('account');
const rememberMe = ref(false);
const showPassword = ref(false);
const loading = ref(false);

const toggleLoginMode = () => {
  isQrCodeMode.value = !isQrCodeMode.value;
};

// 验证码策略与状态 (支持字符图形验证码与 Code Compass 滑块验证码)
const captchaType = ref<'image' | 'slider'>('image');
const sliderModalVisible = ref(false);
const captchaToken = ref('');
const captchaId = ref('');
const captchaImg = ref('');

// 账号密码登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
});

// 邮箱验证码登录表单数据
const emailForm = reactive({
  email: '',
  code: ''
});

const applyLoginSession = (data: LoginResult) => {
  authStore.setToken(data.token);
  if (data.userInfo) {
    authStore.setUser({
      id: data.userInfo.id,
      username: data.userInfo.username,
      realName: data.userInfo.realName || data.userInfo.username,
      avatar: data.userInfo.avatar || '',
      roles: (data.userInfo.roles || []) as any,
      permissions: data.userInfo.permissions || [],
      department: data.userInfo.department,
      email: data.userInfo.email
    });
  }
  if (data.tenantId) {
    storage.set(TENANT_ID_KEY, data.tenantId);
  }
};

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

// 滑块验证码校验通过回调 (对齐 Code Compass)
const handleSliderSuccess = (payload: { captchaToken: string }) => {
  captchaToken.value = payload.captchaToken;
  handleLogin();
};

// 登录提交 (支持图形验证码与 Code Compass 滑块安全验证)
const handleLogin = async () => {
  if (loading.value) return;

  const username = loginForm.username.trim();
  const password = loginForm.password;
  const captcha = loginForm.captcha.trim();

  if (!username || !password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }

  // 若系统采用滑块验证且尚未完成人机验证，弹出滑块弹窗
  if (captchaType.value === 'slider' && !captchaToken.value) {
    sliderModalVisible.value = true;
    return;
  }

  // 若系统采用字符图形验证码且未填写
  if (captchaType.value === 'image' && !captcha) {
    ElMessage.warning('请完成图形验证码');
    return;
  }

  loading.value = true;
  try {
    const res = await loginApi({
      username,
      password,
      captcha: captchaType.value === 'image' ? captcha : undefined,
      captchaId: captchaType.value === 'image' ? captchaId.value : undefined,
      captchaToken: captchaType.value === 'slider' ? captchaToken.value : undefined
    });
    captchaToken.value = '';
    if (res?.data?.token) {
      applyLoginSession(res.data);
      if (!res.data.userInfo) {
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
    // 登录失败时销毁当前票据，避免重试漏验
    captchaToken.value = '';
    if (captchaType.value === 'image') {
      refreshCaptcha();
    }
  } finally {
    loading.value = false;
  }
};

// 邮箱验证码快捷登录提交
const handleEmailLogin = async () => {
  if (loading.value) return;

  const email = emailForm.email.trim().toLowerCase();
  const code = emailForm.code.trim();

  if (!email) {
    ElMessage.warning('请输入绑定的电子邮箱地址');
    return;
  }
  if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
    ElMessage.warning('请输入有效的电子邮箱格式');
    return;
  }
  if (!code) {
    ElMessage.warning('请输入 6 位邮箱验证码');
    return;
  }

  loading.value = true;
  try {
    const res = await emailLoginApi({ email, code });
    if (res?.data?.token) {
      applyLoginSession(res.data);
      if (!res.data.userInfo) {
        await authStore.fetchUserInfo();
      }

      if (rememberMe.value) {
        localStorage.setItem('edumind_saved_email', email);
      } else {
        localStorage.removeItem('edumind_saved_email');
      }

      ElMessage.success({
        message: `邮箱验证授权成功！欢迎使用 EduMind 平台，${authStore.currentUser?.realName || ''}`,
        type: 'success'
      });

      const redirect = (route.query.redirect as string) || '/dashboard';
      router.push(redirect);
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '邮箱登录失败，请重试');
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

onMounted(async () => {
  const savedUsername = localStorage.getItem('edumind_saved_username');
  if (savedUsername) {
    loginForm.username = savedUsername;
    rememberMe.value = true;
  }
  const savedEmail = localStorage.getItem('edumind_saved_email');
  if (savedEmail) {
    emailForm.email = savedEmail;
  }
  try {
    const policyRes = await getCaptchaPolicy();
    if (policyRes?.data?.captchaType) {
      captchaType.value = policyRes.data.captchaType;
    }
  } catch {
    // 获取失败默认采用图片验证码
  }
  if (captchaType.value === 'image') {
    refreshCaptcha();
  }
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

    /* 扫码模式 */
    .qrcode-body {
      display: flex;
      flex-direction: column;
      align-items: center;

      .qrcode-header {
        margin-bottom: 8px;
        text-align: center;

        .card-welcome-title {
          font-size: 16px;
          font-weight: 600;
          color: #0A1B39;
          margin: 0 0 6px 0;
        }

        .qrcode-sub-title {
          font-size: 13px;
          color: #6E7A8A;
          margin: 0;
        }
      }

      .scan-box-panel {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 6px 0;

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
