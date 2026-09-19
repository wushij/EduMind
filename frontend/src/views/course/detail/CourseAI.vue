<template>
  <CourseAIPanel
    class="course-ai-subview-panel"
    :course="course"
    @switch-course="(courseId) => emit('switch-course', courseId)"
  />
</template>

<script setup lang="ts">
import type { CourseVO } from '@/types/course/course';
import CourseAIPanel from '@/components/course/CourseAIPanel.vue';

defineProps<{
  course?: CourseVO | null;
}>();

const emit = defineEmits<{
  (e: 'switch-course', courseId: number): void;
}>();
</script>

<style scoped lang="scss">
.course-ai-subview-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}
</style>
