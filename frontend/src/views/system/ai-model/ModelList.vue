<template>
  <div class="system-page-shell">
    <ModelListHeader
      :active-tab="activeTab"
      :refreshing="refreshing"
      :chat-models="chatModels"
      :embedding-models="embeddingModels"
      :on-open-create="openCreate"
      :on-refresh="handleRefresh"
      :on-switch-tab="switchTab"
    />

    <ModelListTable
      :loading="loading"
      :active-tab="activeTab"
      :displayed-models="displayedModels"
      :testing-models="testingModels"
      :test-results="testResults"
      :reasoning-label="reasoningLabel"
      :get-provider-display-name="getProviderDisplayName"
      :on-open-create="openCreate"
      :on-test="handleTest"
      :on-edit="openEdit"
      :on-set-default="handleSetDefault"
      :on-delete="handleDelete"
    />

    <ModelFormDialog
      :visible="dialogVisible"
      :editing="editing"
      :saving="saving"
      :dialog-testing="dialogTesting"
      :dialog-test-result="dialogTestResult"
      :form="form"
      :current-presets-group="currentPresetsGroup"
      :current-preset="currentPreset"
      :current-model-options="currentModelOptions"
      :on-visible-change="(v) => { dialogVisible = v }"
      :on-config-type-change="handleConfigTypeChange"
      :on-provider-change="handleProviderChange"
      :on-dialog-test="handleDialogTest"
      :on-save="handleSave"
    />

    <ModelDimensionWarnDialog
      :visible="dimensionWarnVisible"
      :on-visible-change="(v) => { dimensionWarnVisible = v }"
      :on-confirm="confirmDimensionWarn"
    />
  </div>
</template>

<script setup lang="ts">
import ModelListHeader from '@/components/system/ai-model/ModelListHeader.vue';
import ModelListTable from '@/components/system/ai-model/ModelListTable.vue';
import ModelFormDialog from '@/components/system/ai-model/ModelFormDialog.vue';
import ModelDimensionWarnDialog from '@/components/system/ai-model/ModelDimensionWarnDialog.vue';
import { useAIModel } from '@/composables/system/useAIModel';

const {
  loading,
  refreshing,
  saving,
  activeTab,
  dialogVisible,
  editing,
  testingModels,
  testResults,
  dialogTesting,
  dialogTestResult,
  form,
  dimensionWarnVisible,
  chatModels,
  embeddingModels,
  displayedModels,
  currentPresetsGroup,
  currentPreset,
  currentModelOptions,
  reasoningLabel,
  getProviderDisplayName,
  handleConfigTypeChange,
  handleProviderChange,
  confirmDimensionWarn,
  openCreate,
  openEdit,
  handleRefresh,
  switchTab,
  handleSave,
  handleSetDefault,
  handleDelete,
  handleDialogTest,
  handleTest
} = useAIModel();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';
</style>
