<template>
  <div class="course-create-page">
    <CourseCreateHeader @back="goToCourseList" />

    <div class="create-workspace-layout">
      <div class="form-sections-col">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="capsule-form">
          <CourseCreateForm
            v-model:kb-mode="kbMode"
            v-model:enable-rag-auto-index="enableRagAutoIndex"
            :form="form"
            :category-presets="categoryPresets"
            :available-knowledge-bases="availableKnowledgeBases"
            :ai-personas="aiPersonas"
            :preset-covers="presetCovers"
            :selected-cover-id="selectedCoverId"
            @generate-code="generateRandomCourseCode"
            @select-persona="selectPersona"
            @select-cover="selectPresetCover"
            @clear-cover-selection="selectedCoverId = 0"
          >
            <template #chapter-editor>
              <CourseChapterInitEditor
                :initial-chapters="form.initialChapters"
                :is-ai-generating-outline="isAiGeneratingOutline"
                :thinking-time-text="aiThinkingElapsedTime"
                @ai-generate="handleAiGenerateOutline"
                @cancel-ai-generate="cancelAiGenerateOutline"
                @add-chapter="addChapter"
                @remove-chapter="removeChapter"
                @update-chapter="(idx, value) => (form.initialChapters[idx] = value)"
              />
            </template>
          </CourseCreateForm>
        </el-form>
      </div>

      <div class="preview-sticky-col">
        <CoursePreviewCard
          :form="form"
          :current-cover-class="currentCoverClass"
          :kb-label="kbLabel"
          :current-persona-name="currentPersonaName"
        />
      </div>
    </div>

    <div class="create-page-actions">
      <button type="button" class="action-btn action-btn--cancel" @click="goToCourseList">
        取消并返回
      </button>
      <button
        type="button"
        class="action-btn action-btn--submit"
        :disabled="submitting"
        @click="handleSubmit"
      >
        <el-icon v-if="!submitting"><Select /></el-icon>
        <span v-if="!submitting">立即创建并初始化全链路空间</span>
        <span v-else>正在全链路初始化空间与大纲...</span>
      </button>
    </div>

    <CourseCreateSuccessModal
      v-model:visible="successModalVisible"
      :created-course-name="createdCourse?.name || form.name"
      :course-code="form.code"
      :created-course-chapters-count="createdCourseChaptersCount"
      @copy-code="handleCopyCode"
      @go-to-course="goToCreatedCourse"
      @go-to-list="goToCourseList"
    />
  </div>
</template>

<script setup lang="ts">
import { Select } from '@element-plus/icons-vue';
import { useCourseCreate } from '@/composables/course/useCourseCreate';
import CourseCreateHeader from '@/components/course/CourseCreateHeader.vue';
import CourseCreateForm from '@/components/course/CourseCreateForm.vue';
import CourseChapterInitEditor from '@/components/course/CourseChapterInitEditor.vue';
import CoursePreviewCard from '@/components/course/CoursePreviewCard.vue';
import CourseCreateSuccessModal from '@/components/course/CourseCreateSuccessModal.vue';

const {
  formRef, form, rules, submitting, isAiGeneratingOutline, selectedCoverId, kbMode, enableRagAutoIndex,
  successModalVisible, createdCourse, createdCourseChaptersCount, categoryPresets, presetCovers, aiPersonas,
  availableKnowledgeBases, currentCoverClass, kbLabel, currentPersonaName, selectPresetCover,
  generateRandomCourseCode, selectPersona, addChapter, removeChapter,
  handleAiGenerateOutline, cancelAiGenerateOutline, aiThinkingElapsedTime,
  handleSubmit, handleCopyCode, goToCreatedCourse, goToCourseList
} = useCourseCreate();
</script>

<style scoped lang="scss">
.course-create-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  padding-bottom: 24px;
}

.create-page-actions {
  margin-top: 4px;
  padding: 20px 28px;
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #ebf1f7;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;

  .action-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 44px;
    padding: 0 28px;
    border-radius: 9999px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.22s ease;

    &--cancel {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      color: #64748b;

      &:hover {
        background: #f8fafc;
        color: #1e293b;
        border-color: #cbd5e1;
      }
    }

    &--submit {
      background: #1677ff;
      border: none;
      color: #ffffff;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);

      &:hover:not(:disabled) {
        background: #4096ff;
        transform: translateY(-1px);
        box-shadow: 0 8px 22px rgba(22, 119, 255, 0.4);
      }

      &:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }
    }
  }
}

.create-workspace-layout {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  width: 100%;

  @media (max-width: 1100px) {
    flex-direction: column;
  }
}

.form-sections-col {
  flex: 1 1 0;
  min-width: 0;
  width: 100%;

  :deep(.capsule-form) {
    width: 100%;
    display: block;
  }
}

.preview-sticky-col {
  flex: 0 0 360px;
  width: 360px;
  max-width: 360px;
  min-width: 0;
  position: sticky;
  top: 20px;

  @media (max-width: 1100px) {
    flex: none;
    width: 100%;
    max-width: 100%;
    position: static;
  }
}
</style>
