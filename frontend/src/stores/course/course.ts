import { defineStore } from 'pinia';
import { ref } from 'vue';
import { Course } from '@/types/course/course';

export const useCourseStore = defineStore('course', () => {
  const currentCourse = ref<Course | null>(null);
  return { currentCourse };
});
