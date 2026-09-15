<template>
  <div class="preferences-container" v-loading="preferenceStore.loading">
    <PreferencesHeroDock
      :saving="preferenceStore.saving"
      :handle-reset="handleReset"
      :handle-save="handleSave"
    />

    <div class="pref-cards-stack">
      <PreferencesVisualPanel
        :preference-store="preferenceStore"
        :handle-theme-change="handleThemeChange"
      />

      <PreferencesAiEnginePanel
        :preference-store="preferenceStore"
        :models-loading="modelsLoading"
        :available-models="availableModels"
        :resolve-provider-tag-type="resolveProviderTagType"
        :fetch-dynamic-models="fetchDynamicModels"
      />

      <PreferencesRenderNotifyPanel :preference-store="preferenceStore" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { usePreferences } from '@/composables/profile/usePreferences';
import PreferencesHeroDock from '@/components/profile/PreferencesHeroDock.vue';
import PreferencesVisualPanel from '@/components/profile/PreferencesVisualPanel.vue';
import PreferencesAiEnginePanel from '@/components/profile/PreferencesAiEnginePanel.vue';
import PreferencesRenderNotifyPanel from '@/components/profile/PreferencesRenderNotifyPanel.vue';

const {
  preferenceStore,
  modelsLoading,
  availableModels,
  resolveProviderTagType,
  fetchDynamicModels,
  handleThemeChange,
  handleReset,
  handleSave
} = usePreferences();
</script>

<style scoped lang="scss">
.preferences-container {
  padding: 24px 28px 48px;
  background: var(--el-bg-color-page, #f8fafc);
  min-height: calc(100vh - 64px);
  transition: background-color 0.3s ease;

  .pref-cards-stack {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
}
</style>
