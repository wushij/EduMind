import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth/auth';
import { changePassword, sendEmailCode, verifyResetCode, resetPassword } from '@/api/auth/auth';
import { sendBindEmailCode, bindEmail as bindEmailApi } from '@/api/system/user';

export interface PasswordStrength {
  score: number;
  label: string;
  class: string;
  color: string;
}

export interface SecurityFactors {
  hasSession: boolean;
  hasRole: boolean;
  hasEmail: boolean;
  hasPhone: boolean;
  hasRealName: boolean;
  hasAvatar: boolean;
}

export interface SecurityLevel {
  label: string;
  class: string;
  color: string;
  tip: string;
}

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

export function calcPasswordStrength(pwd: string): PasswordStrength {
  if (!pwd) return { score: 0, label: '', class: '', color: '#e2e8f0' };

  let score = 0;
  if (pwd.length >= 8) score++;
  if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++;
  else if (/[A-Za-z]/.test(pwd) && /[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd) || (/[0-9]/.test(pwd) && /[A-Z]/.test(pwd) && /[a-z]/.test(pwd))) score++;

  if (score <= 1) return { score: 1, label: '弱 (建议加强)', class: 'weak', color: '#ef4444' };
  if (score === 2) return { score: 2, label: '中等 (符合要求)', class: 'medium', color: '#f59e0b' };
  return { score: 3, label: '极强 (安全稳固)', class: 'strong', color: '#10b981' };
}

export function calcSecurityScore(factors: SecurityFactors): number {
  let score = 0;
  if (factors.hasSession) score += 20;
  if (factors.hasRole) score += 10;
  if (factors.hasEmail) score += 30;
  if (factors.hasPhone) score += 15;
  if (factors.hasRealName) score += 10;
  if (factors.hasAvatar) score += 5;
  if (factors.hasEmail && factors.hasPhone) score += 10;
  return Math.min(score, 100);
}

export function calcSecurityLevel(score: number, factors: SecurityFactors): SecurityLevel {
  const missing: string[] = [];
  if (!factors.hasEmail) missing.push('密保邮箱');
  if (!factors.hasPhone) missing.push('手机号码');
  if (!factors.hasRealName) missing.push('真实姓名');

  if (score >= 85) {
    return {
      label: '安全极佳',
      class: 'excellent',
      color: '#10b981',
      tip: '密保邮箱与联系方式已完善，账号防护要素齐全'
    };
  }
  if (score >= 60) {
    return {
      label: '防护良好',
      class: 'good',
      color: '#3b82f6',
      tip: missing.length
        ? `建议继续完善：${missing.join('、')}`
        : '基础防护已就绪，建议每 90 天定期更新登录密码'
    };
  }
  return {
    label: '等级偏低',
    class: 'warn',
    color: '#f59e0b',
    tip: missing.length
      ? `账号资料不完整，请优先绑定：${missing.join('、')}`
      : '请完善账号安全资料以降低密码找回与异地登录风险'
  };
}

export function maskEmail(email: string) {
  if (!email || !email.includes('@')) return email;
  const [name, domain] = email.split('@');
  if (name.length <= 2) return `${name}***@${domain}`;
  return `${name.slice(0, 2)}***${name.slice(-1)}@${domain}`;
}

export function maskPhone(phone: string) {
  const digits = phone.replace(/\D/g, '');
  if (digits.length < 7) return phone;
  if (digits.length === 11) {
    return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
  }
  return `${digits.slice(0, 2)}****${digits.slice(-2)}`;
}

export function getRoleLabel(role: string | null | undefined) {
  if (role === 'ADMIN') return '超级管理员';
  if (role === 'TEACHER') return '任课教师';
  if (role === 'STUDENT') return '在校学生';
  return '平台学者';
}

export function useSecurity() {
  const router = useRouter();
  const authStore = useAuthStore();
  const { currentUser } = storeToRefs(authStore);

  const activeMode = ref<'password' | 'email'>('password');

  const showOldPwd = ref(false);
  const showNewPwd = ref(false);
  const showConfirmPwd = ref(false);
  const showEmailNewPwd = ref(false);
  const showEmailConfirmPwd = ref(false);

  const submittingPwd = ref(false);
  const submittingEmailReset = ref(false);
  const sendingCode = ref(false);
  const countdown = ref(0);
  let timer: ReturnType<typeof setInterval> | null = null;

  const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const emailResetForm = reactive({
    code: '',
    newPassword: '',
    confirmPassword: ''
  });

  const bindDialogVisible = ref(false);
  const bindingLoading = ref(false);
  const sendingBindCode = ref(false);
  const bindCountdown = ref(0);
  let bindTimer: ReturnType<typeof setInterval> | null = null;
  const bindForm = reactive({
    email: '',
    code: ''
  });

  const userEmail = computed(() => currentUser.value?.email?.trim() || '');
  const userPhone = computed(() => currentUser.value?.phone?.trim() || '');
  const displayName = computed(() => {
    const user = currentUser.value;
    if (!user) return '—';
    return user.realName?.trim() || user.username || '—';
  });
  const sessionActive = computed(() => !!authStore.token);

  const securityFactors = computed<SecurityFactors>(() => ({
    hasSession: sessionActive.value,
    hasRole: !!(currentUser.value?.roles?.length),
    hasEmail: !!userEmail.value,
    hasPhone: !!userPhone.value,
    hasRealName: !!(
      currentUser.value?.realName?.trim() &&
      currentUser.value.realName.trim() !== currentUser.value.username
    ),
    hasAvatar: !!currentUser.value?.avatar?.trim()
  }));

  const roleLabel = computed(() => getRoleLabel(authStore.currentRole));

  const currentCheckingPwd = computed(() =>
    activeMode.value === 'password' ? pwdForm.newPassword : emailResetForm.newPassword
  );

  const pwdStrength = computed(() => calcPasswordStrength(currentCheckingPwd.value));
  const securityScore = computed(() => calcSecurityScore(securityFactors.value));
  const securityLevel = computed(() => calcSecurityLevel(securityScore.value, securityFactors.value));

  async function handleChangePassword() {
    const oldPwd = pwdForm.oldPassword.trim();
    const newPwd = pwdForm.newPassword.trim();
    const confirmPwd = pwdForm.confirmPassword.trim();

    if (!oldPwd) {
      ElMessage.warning('请输入当前使用的登录密码');
      return;
    }
    if (!newPwd) {
      ElMessage.warning('请输入新密码');
      return;
    }
    if (newPwd.length < 8) {
      ElMessage.warning('新密码长度不能少于 8 个字符');
      return;
    }
    if (newPwd === oldPwd) {
      ElMessage.warning('新密码不能与原密码相同');
      return;
    }
    if (newPwd !== confirmPwd) {
      ElMessage.warning('两次输入的新密码不一致，请重新核对');
      return;
    }

    submittingPwd.value = true;
    try {
      await changePassword(oldPwd, newPwd);
      ElMessage.success('密码修改成功，请使用新密码重新登录');
      authStore.logout();
      router.push('/auth/login');
    } catch {
      // global interceptor handles errors
    } finally {
      submittingPwd.value = false;
    }
  }

  async function handleSendEmailResetCode() {
    if (sendingCode.value || countdown.value > 0) return;
    if (!userEmail.value) {
      ElMessage.warning('当前账号尚未绑定密保邮箱，请先绑定邮箱');
      return;
    }

    sendingCode.value = true;
    try {
      await sendEmailCode({
        email: userEmail.value,
        scene: 'resetpwd'
      });
      ElMessage.success(`验证码已成功发送至 ${maskEmail(userEmail.value)}，请前往查收`);
      countdown.value = 60;
      if (timer) clearInterval(timer);
      timer = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
          clearInterval(timer!);
          timer = null;
        }
      }, 1000);
    } catch {
      // global interceptor handles errors
    } finally {
      sendingCode.value = false;
    }
  }

  async function handleEmailResetPassword() {
    const code = emailResetForm.code.trim();
    const newPwd = emailResetForm.newPassword.trim();
    const confirmPwd = emailResetForm.confirmPassword.trim();

    if (!userEmail.value) {
      ElMessage.warning('当前账号尚未绑定安全邮箱，请先完成绑定');
      return;
    }
    if (!code) {
      ElMessage.warning('请输入 6 位邮箱验证码');
      return;
    }
    if (code.length !== 6) {
      ElMessage.warning('验证码格式不正确，应为 6 位数字');
      return;
    }
    if (!newPwd) {
      ElMessage.warning('请输入新密码');
      return;
    }
    if (newPwd.length < 8) {
      ElMessage.warning('新密码长度不能少于 8 个字符');
      return;
    }
    if (newPwd !== confirmPwd) {
      ElMessage.warning('两次输入的新密码不一致，请重新核对');
      return;
    }

    submittingEmailReset.value = true;
    try {
      const res = await verifyResetCode({
        email: userEmail.value,
        code
      });
      const resetToken = res.data?.resetToken || '';

      await resetPassword({
        resetToken,
        newPassword: newPwd
      });

      ElMessage.success('密码重置成功，请使用新密码重新登录');
      authStore.logout();
      router.push('/auth/login');
    } catch {
      // global interceptor handles errors
    } finally {
      submittingEmailReset.value = false;
    }
  }

  function openBindDialog() {
    bindForm.email = '';
    bindForm.code = '';
    bindDialogVisible.value = true;
  }

  async function handleSendBindCode() {
    const targetEmail = bindForm.email.trim().toLowerCase();
    if (!targetEmail) {
      ElMessage.warning('请输入电子邮箱地址');
      return;
    }
    if (!EMAIL_REGEX.test(targetEmail)) {
      ElMessage.warning('请输入合法的电子邮箱格式');
      return;
    }

    sendingBindCode.value = true;
    try {
      await sendBindEmailCode(targetEmail);
      ElMessage.success('绑定验证码已发送至该邮箱，请注意查收');
      bindCountdown.value = 60;
      if (bindTimer) clearInterval(bindTimer);
      bindTimer = setInterval(() => {
        bindCountdown.value--;
        if (bindCountdown.value <= 0) {
          clearInterval(bindTimer!);
          bindTimer = null;
        }
      }, 1000);
    } catch {
      // global interceptor handles errors
    } finally {
      sendingBindCode.value = false;
    }
  }

  async function handleConfirmBind() {
    const targetEmail = bindForm.email.trim().toLowerCase();
    const code = bindForm.code.trim();

    if (!targetEmail) {
      ElMessage.warning('请输入电子邮箱');
      return;
    }
    if (!EMAIL_REGEX.test(targetEmail)) {
      ElMessage.warning('请输入合法的电子邮箱格式');
      return;
    }
    if (!code) {
      ElMessage.warning('请输入 6 位验证码');
      return;
    }

    bindingLoading.value = true;
    try {
      const res = await bindEmailApi({ email: targetEmail, code });
      if (res?.data) {
        authStore.setUser(res.data);
      } else {
        await authStore.fetchUserInfo();
      }
      ElMessage.success('安全密保邮箱绑定成功！');
      bindDialogVisible.value = false;
    } catch {
      // global interceptor handles errors
    } finally {
      bindingLoading.value = false;
    }
  }

  function handleGoToForgotPassword() {
    router.push('/auth/forgot-password');
  }

  async function handleLogoutConfirm() {
    try {
      await authStore.logout();
      ElMessage.success('已安全退出当前会话');
    } catch {
      // fallback logout
    } finally {
      router.push('/auth/login');
    }
  }

  onMounted(() => {
    authStore.fetchUserInfo();
  });

  onBeforeUnmount(() => {
    if (timer) clearInterval(timer);
    if (bindTimer) clearInterval(bindTimer);
  });

  return {
    activeMode,
    showOldPwd,
    showNewPwd,
    showConfirmPwd,
    showEmailNewPwd,
    showEmailConfirmPwd,
    submittingPwd,
    submittingEmailReset,
    sendingCode,
    countdown,
    pwdForm,
    emailResetForm,
    bindDialogVisible,
    bindingLoading,
    sendingBindCode,
    bindCountdown,
    bindForm,
    currentUser,
    userEmail,
    userPhone,
    displayName,
    sessionActive,
    roleLabel,
    currentCheckingPwd,
    pwdStrength,
    securityScore,
    securityLevel,
    maskEmail,
    maskPhone,
    handleChangePassword,
    handleSendEmailResetCode,
    handleEmailResetPassword,
    openBindDialog,
    handleSendBindCode,
    handleConfirmBind,
    handleGoToForgotPassword,
    handleLogoutConfirm
  };
}
