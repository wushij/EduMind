<template>
  <div class="login-container">
    <LoginCard
      v-model:is-qr-code-mode="isQrCodeMode"
      v-model:active-tab="activeTab"
      v-model:remember-me="rememberMe"
      v-model:show-password="showPassword"
      :login-form="loginForm"
      :email-form="emailForm"
      :loading="loading"
      :captcha-type="captchaType"
      :captcha-img="captchaImg"
      :toggle-login-mode="toggleLoginMode"
      :refresh-captcha="refreshCaptcha"
      :handle-login="handleLogin"
      :handle-email-login="handleEmailLogin"
      :handle-mock-qr-login="handleMockQrLogin"
      :handle-third-login="handleThirdLogin"
    />
    <SliderCaptchaModal
      v-model="sliderModalVisible"
      :username="loginForm.username.trim()"
      @success="handleSliderSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import SliderCaptchaModal from '@/components/common/SliderCaptchaModal.vue';
import LoginCard from '@/components/auth/LoginCard.vue';
import { useLogin } from '@/composables/auth/useLogin';

const {
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
} = useLogin();
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
}

@media screen and (max-width: 1024px) {
  .login-container {
    justify-content: center;
    padding-right: 0;
  }
}
</style>
