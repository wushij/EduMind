<template>
  <div class="forgot-container">
    <div class="forgot-bg-mask"></div>

    <div class="forgot-card-box">
      <div class="forgot-card">
        <!-- 顶部返回与标题 -->
        <div class="card-header">
          <div class="back-link" @click="goBackToLogin">
            <el-icon><Back /></el-icon>
            <span>返回登录</span>
          </div>
          <h2 class="card-title">找回账号密码</h2>
          <p class="card-subtitle">通过绑定的手机号或机构邮箱安全重置您的登录密码</p>
        </div>

        <!-- 步骤进度指示条 -->
        <div class="steps-wrapper">
          <el-steps :active="currentStep" finish-status="success" align-center>
            <el-step title="验证身份" />
            <el-step title="重置密码" />
            <el-step title="完成" />
          </el-steps>
        </div>

        <!-- 步骤 1：验证身份 -->
        <div v-if="currentStep === 0" class="step-content">
          <el-form
            ref="step1FormRef"
            :model="step1Form"
            :rules="step1Rules"
            class="reset-form"
          >
            <el-form-item prop="account">
              <el-input
                v-model="step1Form.account"
                placeholder="请输入用户名 / 手机号 / 邮箱"
                :prefix-icon="User"
                size="large"
                clearable
              />
            </el-form-item>

            <el-form-item prop="code">
              <div class="sms-code-box">
                <el-input
                  v-model="step1Form.code"
                  placeholder="请输入6位验证码"
                  :prefix-icon="Key"
                  maxlength="6"
                  size="large"
                />
                <el-button
                  type="primary"
                  plain
                  size="large"
                  class="get-code-btn"
                  :disabled="countdown > 0"
                  @click="sendSmsCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重新获取` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="action-btn"
              :loading="submitting"
              @click="handleVerifyStep1"
            >
              下一步，重置密码
            </el-button>
          </el-form>
        </div>

        <!-- 步骤 2：设置新密码 -->
        <div v-else-if="currentStep === 1" class="step-content">
          <el-form
            ref="step2FormRef"
            :model="step2Form"
            :rules="step2Rules"
            class="reset-form"
          >
            <el-form-item prop="newPassword">
              <el-input
                v-model="step2Form.newPassword"
                type="password"
                placeholder="请输入新密码 (不少于6位)"
                :prefix-icon="Lock"
                show-password
                size="large"
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="step2Form.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                :prefix-icon="Lock"
                show-password
                size="large"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="action-btn"
              :loading="submitting"
              @click="handleResetPassword"
            >
              确认修改并提交
            </el-button>
          </el-form>
        </div>

        <!-- 步骤 3：完成提示 -->
        <div v-else class="step-content success-content">
          <div class="success-icon-box">
            <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
          </div>
          <h3 class="success-title">密码重置成功</h3>
          <p class="success-desc">您的新密码已生效，请使用新密码重新登录 EduMind 智教云平台。</p>

          <el-button
            type="primary"
            size="large"
            class="action-btn"
            @click="goBackToLogin"
          >
            立即返回登录
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { User, Lock, Key, Back, CircleCheckFilled } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';

const router = useRouter();

const currentStep = ref(0);
const submitting = ref(false);
const countdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const step1FormRef = ref<FormInstance>();
const step2FormRef = ref<FormInstance>();

// 步骤 1 数据
const step1Form = reactive({
  account: '',
  code: ''
});

const step1Rules = reactive<FormRules>({
  account: [
    { required: true, message: '请输入账号 / 手机号 / 邮箱', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
});

// 步骤 2 数据
const step2Form = reactive({
  newPassword: '',
  confirmPassword: ''
});

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请再次确认密码'));
  } else if (value !== step2Form.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const step2Rules = reactive<FormRules>({
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
});

// 发送短信验证码
const sendSmsCode = () => {
  if (!step1Form.account) {
    ElMessage.warning('请先输入要找回的账号、手机号或机构邮箱');
    return;
  }
  countdown.value = 60;
  step1Form.code = '123456'; // 自动填入演示码
  ElMessage.success('验证码已发送至预留手机/邮箱（演示环境已自动填入: 123456）');

  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer!);
      timer = null;
    }
  }, 1000);
};

// 步骤 1 校验并推进
const handleVerifyStep1 = async () => {
  if (!step1FormRef.value) return;
  const valid = await step1FormRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  setTimeout(() => {
    submitting.value = false;
    currentStep.value = 1;
    ElMessage.success('身份验证通过，请设置新密码');
  }, 500);
};

// 步骤 2 提交重置
const handleResetPassword = async () => {
  if (!step2FormRef.value) return;
  const valid = await step2FormRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  setTimeout(() => {
    submitting.value = false;
    currentStep.value = 2;
    ElMessage.success('密码重置成功！');
  }, 600);
};

const goBackToLogin = () => {
  router.push('/auth/login');
};

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped lang="scss">
.forgot-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  min-height: 640px;
  background: url('@/assets/images/登录2.png') no-repeat left center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 9vw;
  overflow: hidden;

  .forgot-bg-mask {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 80% 50%, rgba(255, 255, 255, 0.05) 0%, rgba(10, 27, 57, 0.12) 100%);
    pointer-events: none;
  }

  .forgot-card-box {
    position: relative;
    z-index: 10;
    animation: fadeIn 0.4s ease-out;
  }

  .forgot-card {
    width: 440px;
    background: #FFFFFF;
    border-radius: 20px;
    box-shadow: 0 20px 50px rgba(10, 27, 57, 0.12), 0 4px 16px rgba(22, 119, 255, 0.08);
    padding: 34px 38px 32px;
    box-sizing: border-box;

    .card-header {
      margin-bottom: 22px;

      .back-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #64748B;
        cursor: pointer;
        margin-bottom: 14px;
        transition: color 0.2s;

        &:hover {
          color: #1677FF;
        }
      }

      .card-title {
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 6px 0;
      }

      .card-subtitle {
        font-size: 13px;
        color: #64748B;
        margin: 0;
      }
    }

    .steps-wrapper {
      margin-bottom: 24px;

      :deep(.el-step__title) {
        font-size: 12px;
      }
    }

    .reset-form {
      .el-form-item {
        margin-bottom: 20px;
      }

      .sms-code-box {
        display: flex;
        gap: 12px;
        width: 100%;

        .get-code-btn {
          white-space: nowrap;
          border-radius: 8px;
          min-width: 110px;
        }
      }

      .action-btn {
        width: 100%;
        height: 44px;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;
        background: linear-gradient(135deg, #1677FF 0%, #0958D9 100%);
        border: none;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);
        margin-top: 6px;
      }
    }

    .success-content {
      text-align: center;
      padding: 10px 0;

      .success-icon-box {
        .check-icon {
          font-size: 56px;
          color: #52C41A;
        }
      }

      .success-title {
        font-size: 18px;
        color: #0F172A;
        font-weight: 700;
        margin: 12px 0 6px;
      }

      .success-desc {
        font-size: 13px;
        color: #64748B;
        margin-bottom: 24px;
        line-height: 1.5;
      }

      .action-btn {
        width: 100%;
        height: 44px;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;
      }
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media screen and (max-width: 1024px) {
  .forgot-container {
    justify-content: center;
    padding-right: 0;

    .forgot-card-box {
      width: 90%;
      max-width: 440px;
    }

    .forgot-card {
      width: 100%;
      padding: 28px 24px;
    }
  }
}
</style>
