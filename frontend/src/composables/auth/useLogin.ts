import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth/auth';
import { USE_MOCK } from '@/config/mock';
import { MOCK_USERS } from '@/mock/users';
import { getCaptcha, getCaptchaPolicy, login as loginApi, emailLogin as emailLoginApi } from '@/api/auth/auth';
import { toCaptchaDataUrl } from '@/utils/captcha';
import { storage } from '@/core/storage/local';
import { TENANT_ID_KEY } from '@/stores/system/tenant';
import type { LoginResult } from '@/types/auth/auth';

export function useLogin() {
  const router = useRouter();
  const route = useRoute();
  const authStore = useAuthStore();

  const isQrCodeMode = ref(false);
  const activeTab = ref<'account' | 'email'>('account');
  const rememberMe = ref(false);
  const showPassword = ref(false);
  const loading = ref(false);

  const captchaType = ref<'image' | 'slider'>('image');
  const sliderModalVisible = ref(false);
  const captchaToken = ref('');
  const captchaId = ref('');
  const captchaImg = ref('');

  const loginForm = reactive({
    username: '',
    password: '',
    captcha: ''
  });

  const emailForm = reactive({
    email: '',
    code: ''
  });

  const toggleLoginMode = () => {
    isQrCodeMode.value = !isQrCodeMode.value;
  };

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

  const handleSliderSuccess = (payload: { captchaToken: string }) => {
    captchaToken.value = payload.captchaToken;
    handleLogin();
  };

  const handleLogin = async () => {
    if (loading.value) return;

    const username = loginForm.username.trim();
    const password = loginForm.password;
    const captcha = loginForm.captcha.trim();

    if (!username || !password) {
      ElMessage.warning('请输入用户名和密码');
      return;
    }

    if (captchaType.value === 'slider' && !captchaToken.value) {
      sliderModalVisible.value = true;
      return;
    }

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
          (r) => MOCK_USERS[r as 'ADMIN' | 'TEACHER' | 'STUDENT'].username === username
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
      captchaToken.value = '';
      if (captchaType.value === 'image') {
        refreshCaptcha();
      }
    } finally {
      loading.value = false;
    }
  };

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

  return {
    isQrCodeMode,
    activeTab,
    rememberMe,
    showPassword,
    loading,
    captchaType,
    sliderModalVisible,
    captchaImg,
    loginForm,
    emailForm,
    toggleLoginMode,
    refreshCaptcha,
    handleSliderSuccess,
    handleLogin,
    handleEmailLogin,
    handleMockQrLogin,
    handleThirdLogin
  };
}
