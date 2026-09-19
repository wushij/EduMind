<template>
  <div class="content-card">
    <div class="card-header-row">
      <div class="card-header-left">
        <el-icon class="card-icon card-icon--blue"><Reading /></el-icon>
        <h3 class="card-title">课程简介与修读要求</h3>
      </div>
      <button
        v-if="editable"
        type="button"
        class="table-action-pill table-action-pill--primary"
        @click="emit('edit-profile')"
      >
        编辑档案
      </button>
    </div>

    <template v-if="course?.description">
      <p class="course-full-desc">{{ course.description }}</p>
    </template>
    <div v-else class="empty-block">
      <el-icon class="empty-icon"><Document /></el-icon>
      <p class="empty-text">{{ editable ? '尚未填写课程简介，点击「编辑档案」补充修读要求。' : '教师尚未填写课程简介。' }}</p>
      <button
        v-if="editable"
        type="button"
        class="capsule-mini-btn"
        @click="emit('edit-profile')"
      >
        去填写
      </button>
    </div>

    <div class="key-tags-row">
      <span v-if="course?.category" class="pill-badge pill-badge--tag is-highlight">{{ course.category }}</span>
      <span v-if="course?.credits != null" class="pill-badge pill-badge--tag">{{ course.credits }} 学分</span>
      <span v-if="course?.plannedHours != null" class="pill-badge pill-badge--tag">{{ course.plannedHours }} 计划学时</span>
      <span v-if="personaLabel" class="pill-badge pill-badge--tag is-ai">助教人设：{{ personaLabel }}</span>
      <span
        v-for="tag in capabilityTags"
        :key="tag.code"
        class="pill-badge pill-badge--tag"
        :class="toneClass(tag.tone)"
      >
        {{ tag.label }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Document, Reading } from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';
import type { CourseCapabilityTagVO } from '@/types/course/overview';
import { getCourseAiPersonaLabel } from '@/constants/course/ai-persona';

const props = defineProps<{
  course?: Course | null;
  capabilityTags: CourseCapabilityTagVO[];
  editable?: boolean;
}>();

const emit = defineEmits<{ 'edit-profile': [] }>();

const personaLabel = computed(() => {
  const p = props.course?.aiPersona;
  if (!p) return '';
  return getCourseAiPersonaLabel(p);
});

function toneClass(tone?: string) {
  if (tone === 'ai') return 'is-ai';
  if (tone === 'kb') return 'is-kb';
  if (tone === 'primary') return 'is-highlight';
  return '';
}
</script>

<style scoped lang="scss">
@use './course-overview-shared.scss' as *;
</style>
