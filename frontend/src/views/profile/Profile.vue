<template>
  <div class="profile-page-container">
    <ProfileHeaderCard
      :is-dev="isDev"
      :current-user="currentUser"
      :current-role="currentRole"
      :display-avatar="displayAvatar"
      :role-label="roleLabel"
      :profile-form="profileForm"
      :handle-avatar-error="handleAvatarError"
      :handle-switch-role="handleSwitchRole"
      :before-avatar-upload="beforeAvatarUpload"
      :handle-avatar-upload="handleAvatarUpload"
    />

    <div class="profile-main-grid">
      <ProfileEditForm
        v-model:bind-dialog-visible="bindDialogVisible"
        :current-user="currentUser"
        :profile-form="profileForm"
        :binding-loading="bindingLoading"
        :bind-form="bindForm"
        :mask-email="maskEmail"
        :handle-save-profile="handleSaveProfile"
        :open-bind-dialog="openBindDialog"
        :handle-confirm-bind="handleConfirmBind"
      />

      <ProfileAiPrefPanel
        :model-options="modelOptions"
        :ai-pref="aiPref"
        :handle-save-ai-pref="handleSaveAiPref"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useProfile } from '@/composables/profile/useProfile';
import ProfileHeaderCard from '@/components/profile/ProfileHeaderCard.vue';
import ProfileEditForm from '@/components/profile/ProfileEditForm.vue';
import ProfileAiPrefPanel from '@/components/profile/ProfileAiPrefPanel.vue';

const {
  isDev,
  currentUser,
  currentRole,
  displayAvatar,
  roleLabel,
  profileForm,
  modelOptions,
  aiPref,
  bindDialogVisible,
  bindingLoading,
  bindForm,
  maskEmail,
  handleAvatarError,
  handleSwitchRole,
  beforeAvatarUpload,
  handleAvatarUpload,
  handleSaveProfile,
  handleSaveAiPref,
  openBindDialog,
  handleConfirmBind
} = useProfile();
</script>

<style scoped lang="scss">
.profile-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

.profile-main-grid {
  display: grid;
  grid-template-columns: 55% 45%;
  gap: 20px;
}

@media (max-width: 1024px) {
  .profile-main-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
