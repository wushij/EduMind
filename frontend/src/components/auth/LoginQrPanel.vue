<template>
  <div class="tab-body qrcode-body">
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

    <div class="back-account-btn" @click="emit('back-to-account')">
      返回账号密码登录
    </div>
  </div>
</template>

<script setup lang="ts">
import QrcodeVue from 'qrcode.vue';

defineProps<{
  handleMockQrLogin: () => void;
}>();

const emit = defineEmits<{
  (e: 'back-to-account'): void;
}>();
</script>

<style scoped lang="scss">
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

@keyframes scanMove {
  0% { top: 4px; opacity: 0.7; }
  50% { top: calc(100% - 6px); opacity: 1; }
  100% { top: 4px; opacity: 0.7; }
}
</style>
