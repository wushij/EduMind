import { onMounted, ref } from 'vue';
import { getCourseList } from '@/api/course/course';
import { getCourseDisplayName } from '@/utils/course/course-display';
import type { Course } from '@/types/course/course';

export function useTeacherCourses(defaultCourseId?: number) {
  const courseOptions = ref<Array<{ id: number; name: string }>>([]);
  const lastStoredId = Number(localStorage.getItem('edumind_last_course_id'));
  const initialId = defaultCourseId ?? (!Number.isNaN(lastStoredId) && lastStoredId > 0 ? lastStoredId : 0);
  const courseId = ref(initialId);
  const loading = ref(false);
  /**
   * 目标课程不在可访问列表时会被自动纠正为第一门可用课程。
   * 调用方必须监听该标记并重新加载数据：纠正只改了 courseId，
   * 若不重载，页面就会出现「显示的课程是 A、内部认的课程还是 B」的错位
   * （典型表现：学情诊断页标题是数学课，点「返回课程空间」却跳回原来那门无权课程）。
   */
  const courseIdCorrected = ref(false);

  async function loadCourses(): Promise<Course[]> {
    loading.value = true;
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = (res.data?.list ?? []) as Course[];
      courseOptions.value = list.map((c) => ({ id: Number(c.id), name: getCourseDisplayName(c) }));
      if (courseOptions.value.length > 0) {
        const hasCurrent = courseOptions.value.some((c) => Number(c.id) === Number(courseId.value));
        if (!hasCurrent) {
          const oldId = courseId.value;
          courseId.value = Number(courseOptions.value[0].id);
          if (oldId > 0 && oldId !== courseId.value) {
            courseIdCorrected.value = true;
          }
          localStorage.setItem('edumind_last_course_id', String(courseId.value));
        }
      } else {
        courseId.value = 0;
      }
      return list;
    } catch {
      courseOptions.value = [];
      courseId.value = 0;
      return [];
    } finally {
      loading.value = false;
    }
  }

  onMounted(loadCourses);

  return { courseOptions, courseId, loading, loadCourses, courseIdCorrected };
}
