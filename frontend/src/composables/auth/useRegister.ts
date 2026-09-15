import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { register } from '@/api/auth/auth';

export interface RegisterForm {
  username: string;
  realName: string;
  role: string;
  password: string;
  confirmPassword: string;
}

export function createConfirmPasswordValidator(getPassword: () => string) {
  return (_rule: unknown, value: string, callback: (error?: Error) => void) => {
    if (value !== getPassword()) {
      callback(new Error('两次输入的密码不一致'));
    } else {
      callback();
    }
  };
}

export function createRegisterRules(getPassword: () => string): FormRules {
  return {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码至少 6 位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      { validator: createConfirmPasswordValidator(getPassword), trigger: 'blur' }
    ]
  };
}

export function useRegister() {
  const router = useRouter();
  const registerFormRef = ref<FormInstance>();
  const loading = ref(false);
  const showPassword = ref(false);
  const agreePolicy = ref(true);

  const registerForm = reactive<RegisterForm>({
    username: '',
    realName: '',
    role: 'STUDENT',
    password: '',
    confirmPassword: ''
  });

  const registerRules = reactive<FormRules>(createRegisterRules(() => registerForm.password));

  async function handleRegister() {
    if (!agreePolicy.value) {
      ElMessage.warning('请勾选同意服务协议与隐私政策');
      return;
    }

    if (!registerFormRef.value) return;
    await registerFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true;
        try {
          await register({
            username: registerForm.username,
            realName: registerForm.realName,
            password: registerForm.password,
            role: registerForm.role as 'TEACHER' | 'STUDENT'
          });
          ElMessage.success('注册成功！正在跳转至登录页面...');
          setTimeout(() => {
            router.push('/auth/login');
          }, 800);
        } catch (err: any) {
          ElMessage.error(err?.message || '注册失败，请检查用户名是否已存在或稍后重试');
        } finally {
          loading.value = false;
        }
      }
    });
  }

  return {
    registerFormRef,
    loading,
    showPassword,
    agreePolicy,
    registerForm,
    registerRules,
    handleRegister
  };
}
