import { onMounted, ref } from 'vue';
import { getCourseList } from '@/api/course/course';
import { getCourseDisplayName } from '@/utils/course/course-display';
import type { Course } from '@/types/course/course';

export function useTeacherCourses(defaultCourseId = 102) {
  const courseOptions = ref<Array<{ id: number; name: string }>>([]);
  const courseId = ref(defaultCourseId);
  const loading = ref(false);

  async function loadCourses() {
    loading.value = true;
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = (res.data?.list ?? []) as Course[];
      courseOptions.value = list.map((c) => ({ id: c.id, name: getCourseDisplayName(c) }));
      if (courseOptions.value.length > 0 && !courseOptions.value.some((c) => c.id === courseId.value)) {
        courseId.value = courseOptions.value[0].id;
      }
    } catch {
      courseOptions.value = [
        { id: 101, name: '数据结构与算法' },
        { id: 102, name: 'Java面向对象程序设计' },
        { id: 103, name: '高等数学（上）' }
      ];
    } finally {
      loading.value = false;
    }
  }

  onMounted(loadCourses);

  return { courseOptions, courseId, loading, loadCourses };
}
