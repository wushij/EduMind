<template>
  <div class="register-container">
    <!-- 右侧高保真注册卡片 (严格 1:1 对齐 Login.vue 与 AUTH_BG 规范) -->
    <div class="register-card-wrapper">
      <div class="register-card">
        <!-- 顶部标题与副标 -->
        <div class="card-header-section">
          <h2 class="card-main-title">欢迎注册 EduMind 账号</h2>
          <p class="card-sub-title">高校 AI 智能教学赋能平台 · 赋能每一位师生</p>
        </div>

        <!-- 注册主表单 -->
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-main-form"
          @keyup.enter="handleRegister"
        >
          <!-- 用户名 -->
          <el-form-item prop="username">
            <div class="input-container">
              <span class="field-icon">
                <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </span>
              <input
                v-model="registerForm.username"
                type="text"
                class="native-input"
                placeholder="请输入用户名 (4-16位字符)"
                autocomplete="username"
              />
            </div>
          </el-form-item>

          <!-- 真实姓名 -->
          <el-form-item prop="realName">
            <div class="input-container">
              <span class="field-icon">
                <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2z"></path>
                  <circle cx="12" cy="10" r="3"></circle>
                  <path d="M7 21v-2a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v2"></path>
                </svg>
              </span>
              <input
                v-model="registerForm.realName"
                type="text"
                class="native-input"
                placeholder="请输入真实姓名 (用于班级名册)"
              />
            </div>
          </el-form-item>

          <!-- 角色身份选择 (纯正药丸长圆选择器) -->
          <el-form-item prop="role">
            <div class="role-selector-row">
              <div
                class="role-pill-item"
                :class="{ active: registerForm.role === 'STUDENT' }"
                @click="registerForm.role = 'STUDENT'"
              >
                <span class="role-icon">🎓</span>
                <span>我是学生</span>
              </div>
              <div
                class="role-pill-item"
                :class="{ active: registerForm.role === 'TEACHER' }"
                @click="registerForm.role = 'TEACHER'"
              >
                <span class="role-icon">👨‍🏫</span>
                <span>我是教师</span>
              </div>
            </div>
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password">
            <div class="input-container">
              <span class="field-icon">
                <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
              </span>
              <input
                v-model="registerForm.password"
                :type="showPassword ? 'text' : 'password'"
                class="native-input"
                placeholder="设置登录密码 (至少6位)"
                autocomplete="new-password"
              />
              <span class="eye-toggle" @click="showPassword = !showPassword">
                <svg v-if="!showPassword" viewBox="0 0 24 24" class="svg-icon eye-icon" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
                <svg v-else viewBox="0 0 24 24" class="svg-icon eye-icon" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="7" r="3"></circle>
                </svg>
              </span>
            </div>
          </el-form-item>

          <!-- 确认密码 -->
          <el-form-item prop="confirmPassword">
            <div class="input-container">
              <span class="field-icon">
                <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="1.8">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
              </span>
              <input
                v-model="registerForm.confirmPassword"
                type="password"
                class="native-input"
                placeholder="请再次输入确认密码"
                autocomplete="new-password"
              />
            </div>
          </el-form-item>

          <!-- 同意条款 -->
          <div class="policy-agreement-row">
            <label class="agreement-label">
              <input v-model="agreePolicy" type="checkbox" class="agreement-check" />
              <span class="agreement-text">
                我已阅读并同意 <a href="javascript:void(0)" class="link-blue">《服务协议》</a> 与 <a href="javascript:void(0)" class="link-blue">《隐私政策》</a>
              </span>
            </label>
          </div>

          <!-- 纯正长圆主注册按钮 -->
          <button
            type="button"
            class="primary-register-btn"
            :disabled="loading"
            @click="handleRegister"
          >
            <span v-if="!loading">立即注册并开启教学</span>
            <span v-else>正在注册...</span>
          </button>
        </el-form>

        <!-- 底部返回登录链接 -->
        <div class="back-to-login-row">
          <span>已有平台账号？</span>
          <router-link to="/auth/login" class="login-link">
            直接登录
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import { register } from '@/api/auth/auth';

const router = useRouter();
const registerFormRef = ref<FormInstance>();
const loading = ref(false);
const showPassword = ref(false);
const agreePolicy = ref(true);

const registerForm = reactive({
  username: '',
  realName: '',
  role: 'STUDENT',
  password: '',
  confirmPassword: ''
});

const validateConfirmPwd = (_rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const registerRules = reactive<FormRules>({
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
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
});

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
</script>

<style scoped lang="scss">
.register-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  min-height: 640px;
  background: #EAF3FD url('@/assets/images/登录2.png') no-repeat left center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: clamp(30px, 4.8vw, 88px);
  box-sizing: border-box;
  overflow: hidden;

  .register-card-wrapper {
    position: relative;
    z-index: 10;
  }

  .register-card {
    width: 416px;
    background: #FFFFFF;
    border-radius: 16px;
    box-shadow: 0 12px 36px rgba(16, 68, 148, 0.08), 0 2px 8px rgba(0, 0, 0, 0.02);
    padding: 32px 34px 26px;
    box-sizing: border-box;

    .card-header-section {
      text-align: center;
      margin-bottom: 20px;

      .card-main-title {
        font-size: 18px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 6px 0;
      }

      .card-sub-title {
        font-size: 13px;
        color: #64748B;
        margin: 0;
      }
    }

    // 表单样式
    .register-main-form {
      :deep(.el-form-item) {
        margin-bottom: 14px;
      }

      // 纯正长圆跑道输入框 (1:1 对齐登录页)
      .input-container {
        display: flex;
        align-items: center;
        width: 100%;
        height: 44px;
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 9999px; // 长圆胶囊
        padding: 0 18px;
        box-sizing: border-box;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        &:hover {
          border-color: #CBD5E1;
        }

        &:focus-within {
          border-color: #1677FF;
          box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
        }

        .field-icon {
          display: flex;
          align-items: center;
          margin-right: 10px;
          color: #8C9BAE;

          .svg-icon {
            width: 17px;
            height: 17px;
          }
        }

        .native-input {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 13.5px;
          color: #1E293B;

          &::placeholder {
            color: #8C9BAE;
            font-size: 13px;
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
            width: 17px;
            height: 17px;
          }
        }
      }

      // 药丸身份切换单选
      .role-selector-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        width: 100%;

        .role-pill-item {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          height: 38px;
          border-radius: 9999px; // 长圆跑道
          border: 1px solid #E2E8F0;
          background: #F8FAFC;
          color: #64748B;
          font-size: 13px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;

          .role-icon {
            font-size: 14px;
          }

          &:hover {
            border-color: #CBD5E1;
            color: #1E293B;
          }

          &.active {
            border-color: #1677FF;
            background: #EAF3FF;
            color: #1677FF;
            font-weight: 600;
          }
        }
      }

      .policy-agreement-row {
        margin: 10px 0 16px;

        .agreement-label {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          cursor: pointer;

          .agreement-check {
            width: 14px;
            height: 14px;
            accent-color: #1677FF;
          }

          .agreement-text {
            font-size: 12px;
            color: #64748B;

            .link-blue {
              color: #1677FF;
              text-decoration: none;
              &:hover { text-decoration: underline; }
            }
          }
        }
      }

      // 长圆主注册按钮
      .primary-register-btn {
        width: 100%;
        height: 46px;
        border-radius: 9999px; // 长圆胶囊
        background: #1677FF;
        border: none;
        color: #FFFFFF;
        font-size: 15px;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
        transition: all 0.22s ease;

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
        }

        &:active {
          background: #0958D9;
          transform: translateY(0);
        }

        &:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }
      }
    }

    .back-to-login-row {
      margin-top: 18px;
      text-align: center;
      font-size: 13px;
      color: #64748B;

      .login-link {
        color: #1677FF;
        font-weight: 600;
        text-decoration: none;
        margin-left: 4px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}
</style>
