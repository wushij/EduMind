<template>
  <div class="user-detail-container">
    <UserDetailNavBar
      :user-name="(userInfo?.realName as string) || undefined"
      :on-back="() => router.push('/system/users')"
    />

    <UserDetailHero
      :loading="loading"
      :user-info="userInfo"
      :display-avatar="displayAvatar"
      v-model:reset-password-dialog-visible="resetPasswordDialogVisible"
      v-model:reset-password-form="resetPasswordForm"
      :reset-password-loading="resetPasswordLoading"
      :password-strength-score="passwordStrengthScore"
      :password-strength-level="passwordStrengthLevel"
      :is-password-mismatch="isPasswordMismatch"
      :is-submit-valid="isSubmitValid"
      :on-avatar-error="() => { avatarBroken = true }"
      :on-reset-password="handleResetPassword"
      :on-toggle-status="toggleUserStatus"
      :on-apply-preset="applyPresetPassword"
      :on-generate-random="generateRandomPassword"
      :on-submit-reset="submitResetPassword"
    />

    <UserDetailBody
      :user-info="userInfo"
      v-model:edit-roles-modal="editRolesModal"
      v-model:selected-roles="selectedRoles"
      :on-save-roles="saveRoleAssignment"
    />

    <UserOperLogPanel
      :audit-logs="auditLogs"
      :audit-logs-loading="auditLogsLoading"
      :on-jump-oper-log="() => router.push('/system/oper-log')"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import UserDetailNavBar from '@/components/system/user/UserDetailNavBar.vue';
import UserDetailHero from '@/components/system/user/UserDetailHero.vue';
import UserDetailBody from '@/components/system/user/UserDetailBody.vue';
import UserOperLogPanel from '@/components/system/user/UserOperLogPanel.vue';
import { useUserDetail } from '@/composables/system/useUserDetail';

const router = useRouter();

const {
  loading,
  userInfo,
  avatarBroken,
  editRolesModal,
  selectedRoles,
  displayAvatar,
  auditLogs,
  auditLogsLoading,
  resetPasswordDialogVisible,
  resetPasswordLoading,
  resetPasswordForm,
  passwordStrengthScore,
  passwordStrengthLevel,
  isPasswordMismatch,
  isSubmitValid,
  toggleUserStatus,
  handleResetPassword,
  applyPresetPassword,
  generateRandomPassword,
  submitResetPassword,
  saveRoleAssignment
} = useUserDetail();
</script>

<style scoped lang="scss">
.user-detail-container {
  padding: 24px;
  background: #f8fafc;
}
</style>
