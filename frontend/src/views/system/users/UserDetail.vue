<template>
  <div class="user-detail-container">
    <!-- 图 3 风格集成头部、导航、两层操作胶囊与 4 维微看板 -->
    <UserDetailHero
      :loading="loading"
      :user-info="userInfo"
      :display-avatar="displayAvatar"
      :stat-audit-count="statAuditCount"
      :stat-role-count="statRoleCount"
      :stat-security-score="statSecurityScore"
      v-model:reset-password-dialog-visible="resetPasswordDialogVisible"
      v-model:reset-password-form="resetPasswordForm"
      :reset-password-loading="resetPasswordLoading"
      :password-strength-score="passwordStrengthScore"
      :password-strength-level="passwordStrengthLevel"
      :is-password-mismatch="isPasswordMismatch"
      :is-submit-valid="isSubmitValid"
      :on-back="() => router.push('/system/users')"
      :on-avatar-error="() => { avatarBroken = true }"
      :on-reset-password="handleResetPassword"
      :on-open-edit-profile="handleOpenEditProfile"
      :on-open-edit-roles="() => { editRolesModal = true }"
      :on-kickout-session="handleKickoutSession"
      :on-export-card="exportUserCard"
      :on-toggle-status="toggleUserStatus"
      :on-apply-preset="applyPresetPassword"
      :on-generate-random="generateRandomPassword"
      :onSubmitReset="submitResetPassword"
    />

    <!-- 用户基础档案与角色权限分配卡片 -->
    <UserDetailBody
      :user-info="userInfo"
      :system-roles-list="systemRolesList"
      :edit-profile-form="editProfileForm"
      :edit-profile-loading="editProfileLoading"
      v-model:edit-roles-modal="editRolesModal"
      v-model:selected-roles="selectedRoles"
      v-model:edit-profile-modal="editProfileModal"
      :on-open-edit-profile="handleOpenEditProfile"
      :on-submit-edit-profile="submitEditProfile"
      :on-save-roles="saveRoleAssignment"
    />

    <!-- 账号近期安全与操作审计日志（多维胶囊筛选与检索） -->
    <UserOperLogPanel
      v-model:log-keyword="logKeyword"
      v-model:log-status="logStatus"
      v-model:log-module="logModule"
      :filtered-audit-logs="filteredAuditLogs"
      :available-modules="availableModules"
      :audit-logs-loading="auditLogsLoading"
      :on-refresh="loadUserAuditLogs"
      :on-jump-oper-log="() => router.push('/system/oper-log')"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
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
  systemRolesList,
  editProfileModal,
  editProfileLoading,
  editProfileForm,
  displayAvatar,
  auditLogsLoading,
  logKeyword,
  logStatus,
  logModule,
  availableModules,
  filteredAuditLogs,
  statAuditCount,
  statRoleCount,
  statSecurityScore,
  resetPasswordDialogVisible,
  resetPasswordLoading,
  resetPasswordForm,
  passwordStrengthScore,
  passwordStrengthLevel,
  isPasswordMismatch,
  isSubmitValid,
  loadUserAuditLogs,
  toggleUserStatus,
  handleResetPassword,
  applyPresetPassword,
  generateRandomPassword,
  submitResetPassword,
  handleOpenEditProfile,
  submitEditProfile,
  handleKickoutSession,
  exportUserCard,
  saveRoleAssignment
} = useUserDetail();
</script>

<style scoped lang="scss">
.user-detail-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
}
</style>
