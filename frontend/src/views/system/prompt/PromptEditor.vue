<template>
  <div class="prompt-editor-page">
    <PromptEditorPageHeader
      :form="form"
      :publishing="publishing"
      :get-category-label="getCategoryLabel"
      :on-back="() => router.push('/system/prompts')"
      :on-copy-code="() => copyText(form.code, '模板编码已复制')"
      :on-save="saveForm"
      :on-publish="publishForm"
    />

    <div class="editor-studio-grid">
      <div class="studio-main-col">
        <PromptEditorMetaForm
          :form="form"
          :model-options="modelOptions"
          :models-loading="modelsLoading"
          :default-model-label="defaultModelLabel"
        />
        <PromptEditorContentPane
          v-model:system-expanded="systemExpanded"
          :form="form"
          :on-copy="copyText"
          :on-insert-var="insertVar"
          :on-sync-variables="syncVariablesFromPrompts"
        />
        <PromptVariableManager
          :variables="form.variables"
          :on-add="addVariable"
          :on-remove="handleRemoveVariable"
        />
      </div>

      <div class="studio-side-col">
        <PromptPlaygroundPanel
          :variables="form.variables"
          :test-variables="testVariables"
          :test-result-output="testResultOutput"
          :test-result="testResult"
          :testing="testing"
          :demo-filling="demoFilling"
          :on-run-test="handleRunTest"
          :on-fill-demo="fillUniversityDemoData"
        />
        <PromptVersionHistory
          v-model:version-detail-visible="versionDetailVisible"
          :prompt-id="promptId"
          :template-name="form.name"
          :template-code="form.code"
          :version-history="versionHistory"
          :rolling-back="rollingBack"
          :selected-version="selectedVersion"
          :selected-version-variables="selectedVersionVariables"
          :selected-version-diff-info="selectedVersionDiffInfo"
          :is-current-version="isCurrentVersion"
          :get-version-diff-info="getVersionDiffInfo"
          :format-version-date="formatVersionDate"
          :on-open-detail="openVersionDetail"
          :on-rollback="rollbackVersion"
          :on-detail-rollback="handleDetailRollback"
          :on-copy="copyText"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { usePromptEditor } from '@/composables/system/usePromptEditor';
import PromptEditorPageHeader from '@/components/system/prompt/PromptEditorPageHeader.vue';
import PromptEditorMetaForm from '@/components/system/prompt/PromptEditorMetaForm.vue';
import PromptEditorContentPane from '@/components/system/prompt/PromptEditorContentPane.vue';
import PromptVariableManager from '@/components/system/prompt/PromptVariableManager.vue';
import PromptPlaygroundPanel from '@/components/system/prompt/PromptPlaygroundPanel.vue';
import PromptVersionHistory from '@/components/system/prompt/PromptVersionHistory.vue';

const router = useRouter();
const editor = usePromptEditor();
const {
  promptId, systemExpanded, testing, publishing, rollingBack, versionHistory, testResult,
  modelOptions, modelsLoading, defaultModelLabel, form, testVariables, testResultOutput,
  demoFilling,
  versionDetailVisible, selectedVersion, selectedVersionVariables, selectedVersionDiffInfo,
  getCategoryLabel, copyText, insertVar, addVariable, handleRemoveVariable,
  syncVariablesFromPrompts, fillUniversityDemoData, handleRunTest, saveForm, publishForm,
  formatVersionDate, openVersionDetail, getVersionDiffInfo, isCurrentVersion,
  handleDetailRollback, rollbackVersion
} = editor;
</script>

<style scoped lang="scss">
.prompt-editor-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.editor-studio-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
  align-items: start;

  @media (max-width: 1280px) {
    grid-template-columns: 1fr;
  }
}
</style>
