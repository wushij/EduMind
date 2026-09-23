<template>
  <div class="course-ai-page-wrapper">
    <CourseAIWorkbench />
    <CourseAIHistoryDrawer />
  </div>
</template>

<script setup lang="ts">
import { toRef, provide } from 'vue';
import type { CourseVO } from '@/types/course/course';
import { useCourseAIWorkspace } from '@/composables/course/useCourseAIWorkspace';
import CourseAIWorkbench from '@/components/course/course-ai/CourseAIWorkbench.vue';
import CourseAIHistoryDrawer from '@/components/course/course-ai/CourseAIHistoryDrawer.vue';
import { courseAiUiKey } from '@/components/course/course-ai/course-ai-ui-key';

const props = defineProps<{
  course?: CourseVO | null;
}>();

const emit = defineEmits<{
  (e: 'switch-course', courseId: number): void;
}>();

const workspace = useCourseAIWorkspace({
  course: toRef(props, 'course'),
  onSwitchCourse: (courseId) => emit('switch-course', courseId)
});

provide(courseAiUiKey, workspace);
</script>

<style lang="scss">
@import '@/components/course/course-ai/course-ai-panel-styles.scss';
</style>
