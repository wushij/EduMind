<template>
  <CourseOverviewPanel :course="course" />
</template>

<script setup lang="ts">
import type { Course } from '@/types/course/course';
import CourseOverviewPanel from '@/components/course/CourseOverviewPanel.vue';

defineProps<{
  course?: Course | null;
}>();
</script>
