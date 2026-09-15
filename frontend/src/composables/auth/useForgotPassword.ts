import { ref, reactive, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { sendEmailCode, verifyResetCode, resetPassword } from '@/api/auth/auth';

export const FORGOT_PASSWORD_EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

export function useForgotPassword() {
  const router = useRouter();

  const currentStep = ref(0);
  const submitting = ref(false);
  const sendingCode = ref(false);
  const countdown = ref(0);
  const resetToken = ref('');
  let timer: ReturnType<typeof setInterval> | null = null;

  const step1Form = reactive({
    email: '',
    code: ''
  });

  const step2Form = reactive({
    newPassword: '',
    confirmPassword: ''
  });

  const handleSendEmailCode = async () => {
    if (sendingCode.value || countdown.value > 0) return;

    const email = step1Form.email.trim();
    if (!email) {
      ElMessage.warning('请输入要找回密码的电子邮箱');
      return;
    }
    if (!FORGOT_PASSWORD_EMAIL_REGEX.test(email)) {
      ElMessage.warning('请输入有效的电子邮箱格式');
      return;
    }

    sendingCode.value = true;
    try {
      await sendEmailCode({
        email,
        scene: 'resetpwd'
      });
      ElMessage.success('验证码已发送至您的邮箱，请前往查收');
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
      // 全局拦截器已统一弹出错误提示
    } finally {
      sendingCode.value = false;
    }
  };

  const handleVerifyStep1 = async () => {
    if (submitting.value) return;

    const email = step1Form.email.trim();
    const code = step1Form.code.trim();

    if (!email) {
      ElMessage.warning('请输入注册或绑定的电子邮箱');
      return;
    }
    if (!FORGOT_PASSWORD_EMAIL_REGEX.test(email)) {
      ElMessage.warning('请输入有效的电子邮箱格式');
      return;
    }
    if (!code) {
      ElMessage.warning('请输入 6 位邮箱验证码');
      return;
    }
    if (code.length !== 6) {
      ElMessage.warning('验证码应为 6 位数字');
      return;
    }

    submitting.value = true;
    try {
      const res = await verifyResetCode({
        email,
        code
      });
      resetToken.value = res.data?.resetToken || '';
      currentStep.value = 1;
      ElMessage.success('身份验证通过，请设置新密码');
    } catch {
      // 全局拦截器已提示
    } finally {
      submitting.value = false;
    }
  };

  const handleResetPassword = async () => {
    if (submitting.value) return;

    const newPassword = step2Form.newPassword;
    const confirmPassword = step2Form.confirmPassword;

    if (!newPassword) {
      ElMessage.warning('请输入新密码');
      return;
    }
    if (newPassword.length < 6) {
      ElMessage.warning('新密码长度不能少于 6 个字符');
      return;
    }
    if (!confirmPassword) {
      ElMessage.warning('请再次输入新密码确认');
      return;
    }
    if (newPassword !== confirmPassword) {
      ElMessage.warning('两次输入的密码不一致，请重新核对');
      return;
    }
    if (!resetToken.value) {
      ElMessage.warning('身份核验凭据已失效，请返回第一步重新获取验证码');
      currentStep.value = 0;
      return;
    }

    submitting.value = true;
    try {
      await resetPassword({
        resetToken: resetToken.value,
        newPassword
      });
      currentStep.value = 2;
      ElMessage.success('密码重置成功！');
    } catch {
      // 全局拦截器已提示
    } finally {
      submitting.value = false;
    }
  };

  const goBackToLogin = () => {
    router.push('/auth/login');
  };

  onBeforeUnmount(() => {
    if (timer) clearInterval(timer);
  });

  return {
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
  };
}
