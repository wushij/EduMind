import { onMounted, ref } from 'vue';
import { getCourseList } from '@/api/course/course';
import type { Course } from '@/types/course/course';
import { courseLabel } from '@/utils/learning/course-label';

export function useLearningCourseOptions(defaultCourseId = 102) {
  const courseOptions = ref<Array<{ id: number; name: string }>>([]);
  const courseId = ref(defaultCourseId);
  const loading = ref(false);

  async function loadCourses() {
    loading.value = true;
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = (res.data?.list ?? []) as Course[];
      courseOptions.value = list.map((c) => ({ id: c.id, name: courseLabel(c) }));
      if (courseOptions.value.length > 0 && !courseOptions.value.some((c) => c.id === courseId.value)) {
        courseId.value = courseOptions.value[0].id;
      }
    } catch {
      courseOptions.value = [];
    } finally {
      loading.value = false;
    }
  }

  onMounted(loadCourses);

  return { courseOptions, courseId, loading, loadCourses, courseLabel };
}
