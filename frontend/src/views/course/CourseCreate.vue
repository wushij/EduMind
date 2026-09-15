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
            :submitting="submitting"
            @generate-code="generateRandomCourseCode"
            @select-persona="selectPersona"
            @select-cover="selectPresetCover"
            @clear-cover-selection="selectedCoverId = 0"
            @cancel="goToCourseList"
            @submit="handleSubmit"
          >
            <template #chapter-editor>
              <CourseChapterInitEditor
                :initial-chapters="form.initialChapters"
                :is-ai-generating-outline="isAiGeneratingOutline"
                @ai-generate="handleAiGenerateOutline"
                @apply-template="applySyllabusTemplate"
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
  generateRandomCourseCode, selectPersona, addChapter, removeChapter, applySyllabusTemplate,
  handleAiGenerateOutline, handleSubmit, handleCopyCode, goToCreatedCourse, goToCourseList
} = useCourseCreate();
</script>

<style scoped lang="scss">
.course-create-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  padding-bottom: 50px;
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
