<template>
  <CourseAIPanel
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
