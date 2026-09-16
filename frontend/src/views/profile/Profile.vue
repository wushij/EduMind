<template>
  <div class="profile-page-container profile-page-shell">
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
        v-model:selected-model-key="preferences.defaultModel"
        v-model:inference-temperature="preferences.inferenceTemperature"
        v-model:rag-top-k="preferences.ragTopK"
        :model-options="availableModels"
        :models-loading="modelsLoading"
        :pref-loading="prefLoading"
        :saving="prefSaving"
        :usage-loading="usageLoading"
        :usage="usageSnapshot"
        @save="handleSaveAiPref"
        @refresh-models="fetchDynamicModels({ notify: true })"
        @refresh-usage="loadUsageSnapshot"
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
  availableModels,
  modelsLoading,
  prefLoading,
  prefSaving,
  preferences,
  usageSnapshot,
  usageLoading,
  fetchDynamicModels,
  loadUsageSnapshot,
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
@use '@/styles/profile-page-shell.scss';

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
