<template>
  <KnowledgePointPanel :editable="courseEditable" :course="course" />
</template>

<script setup lang="ts">
import type { Course } from '@/types/course/course';
import KnowledgePointPanel from '@/components/course/KnowledgePointPanel.vue';
import { useCourseEditable } from '@/composables/course/useCourseEditable';

const props = defineProps<{
  course?: Course | null;
}>();

const courseEditable = useCourseEditable(() => props.course);
</script>
