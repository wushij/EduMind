<template>
  <div class="security-page-container">
    <SecurityMetricsGrid
      :security-score="securityScore"
      :security-level="securityLevel"
      :user-email="userEmail"
      :user-phone="userPhone"
      :display-name="displayName"
      :session-active="sessionActive"
      :current-user="currentUser"
      :mask-email="maskEmail"
      :mask-phone="maskPhone"
      @open-bind="openBindDialog"
      @logout="handleLogoutConfirm"
      @forgot-password="handleGoToForgotPassword"
    />

    <div class="security-main-grid">
      <PasswordChangePanel
        v-model:active-mode="activeMode"
        v-model:show-old-pwd="showOldPwd"
        v-model:show-new-pwd="showNewPwd"
        v-model:show-confirm-pwd="showConfirmPwd"
        v-model:show-email-new-pwd="showEmailNewPwd"
        v-model:show-email-confirm-pwd="showEmailConfirmPwd"
        v-model:bind-dialog-visible="bindDialogVisible"
        :submitting-pwd="submittingPwd"
        :submitting-email-reset="submittingEmailReset"
        :sending-code="sendingCode"
        :countdown="countdown"
        :pwd-form="pwdForm"
        :email-reset-form="emailResetForm"
        :binding-loading="bindingLoading"
        :sending-bind-code="sendingBindCode"
        :bind-countdown="bindCountdown"
        :bind-form="bindForm"
        :user-email="userEmail"
        :pwd-strength="pwdStrength"
        :mask-email="maskEmail"
        :handle-change-password="handleChangePassword"
        :handle-send-email-reset-code="handleSendEmailResetCode"
        :handle-email-reset-password="handleEmailResetPassword"
        :handle-send-bind-code="handleSendBindCode"
        :handle-confirm-bind="handleConfirmBind"
        :handle-go-to-forgot-password="handleGoToForgotPassword"
        @open-bind="openBindDialog"
      />

      <SecurityGuidelinesPanel
        :current-checking-pwd="currentCheckingPwd"
        :current-user="currentUser"
        :role-label="roleLabel"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useSecurity } from '@/composables/profile/useSecurity';
import SecurityMetricsGrid from '@/components/profile/SecurityMetricsGrid.vue';
import PasswordChangePanel from '@/components/profile/PasswordChangePanel.vue';
import SecurityGuidelinesPanel from '@/components/profile/SecurityGuidelinesPanel.vue';

const {
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
} = useSecurity();
</script>

<style scoped lang="scss">
@use '@/components/profile/security-layout';
</style>
