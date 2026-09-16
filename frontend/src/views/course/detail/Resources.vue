<template>
  <ResourcePanel :course="course" :editable="courseEditable" />
</template>

<script setup lang="ts">
import type { Course } from '@/types/course/course';
import ResourcePanel from '@/components/course/ResourcePanel.vue';
import { useCourseEditable } from '@/composables/course/useCourseEditable';

const props = defineProps<{
  course?: Course | null;
}>();

const courseEditable = useCourseEditable(() => props.course);
</script>
