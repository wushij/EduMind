<template>
  <el-button
    type="primary"
    :plain="!isCounting"
    :size="size"
    :loading="loading"
    :disabled="disabled || isCounting"
    :class="['email-code-btn', { 'is-counting': isCounting }]"
    @click="handleSendCode"
  >
    <span class="email-code-btn__text">
      {{ isCounting ? `${countdown}s 后重新获取` : (buttonText || '获取验证码') }}
    </span>
  </el-button>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { sendAuthEmailCode, sendProfileBindEmailCode } from '@/composables/auth/useEmailCode';
import type { EmailScene } from '@/types/auth/auth';

const props = withDefaults(
  defineProps<{
    email: string;
    scene?: EmailScene;
    size?: 'small' | 'default' | 'large';
    buttonText?: string;
    disabled?: boolean;
    customSender?: () => Promise<any>;
  }>(),
  {
    scene: 'login',
    size: 'default',
    buttonText: '',
    disabled: false,
    customSender: undefined
  }
);

const emit = defineEmits<{
  (e: 'sent'): void;
  (e: 'error', error: any): void;
}>();

const loading = ref(false);
const countdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const isCounting = computed(() => countdown.value > 0);

const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

async function handleSendCode() {
  const targetEmail = props.email.trim();
  if (!targetEmail) {
    ElMessage.warning('请输入电子邮箱地址');
    return;
  }
  if (!emailRegex.test(targetEmail)) {
    ElMessage.warning('请输入有效的电子邮箱格式');
    return;
  }

  loading.value = true;
  try {
    if (props.customSender) {
      await props.customSender();
    } else if (props.scene === 'bind') {
      await sendProfileBindEmailCode(targetEmail);
    } else {
      await sendAuthEmailCode(targetEmail, props.scene);
    }

    ElMessage.success('验证码已发送，请前往邮箱查收');
    emit('sent');
    startCountdown(60);
  } catch (err: any) {
    emit('error', err);
    // 默认走 axios 的全局错误提示，避免与拦截器重复弹窗
    if (props.customSender) {
      ElMessage.error(err?.response?.data?.message || err?.message || '验证码发送失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
}

function startCountdown(seconds: number) {
  countdown.value = seconds;
  if (timer) clearInterval(timer);
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      if (timer) clearInterval(timer);
      timer = null;
    }
  }, 1000);
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});

defineExpose({
  startCountdown
});
</script>

<style scoped lang="scss">
.email-code-btn {
  white-space: nowrap;
  font-weight: 600;
  font-size: 12.5px;
  border-radius: 9999px; // 长圆胶囊外观
  transition: all 0.25s ease;
  padding: 0 16px;
  height: 38px;
  letter-spacing: 0.3px;

  &.el-button--primary.is-plain:not(.is-disabled) {
    background: #EFF6FF;
    border-color: #BFDBFE;
    color: #1677FF;

    &:hover {
      background: #1677FF;
      border-color: #1677FF;
      color: #FFFFFF;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);
    }
  }

  &.is-counting.is-disabled {
    cursor: not-allowed;
    background: #F8FAFC !important;
    border-color: #E2E8F0 !important;
    color: #94A3B8 !important;
    opacity: 0.9 !important;
  }

  .email-code-btn__text {
    display: inline-block;
    line-height: 1.2;
  }
}
</style>
