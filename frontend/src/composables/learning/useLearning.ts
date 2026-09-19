import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getLearningPath } from '@/api/learning/learning-path';
import { getLearningHomeOverview } from '@/api/learning/home';
import { useAuthStore } from '@/stores/auth/auth';
import type { LearningPathVO } from '@/types/learning/learning-path';

export function useLearning(defaultCourseId?: number) {
  const courseOptions = ref<Array<{ id: number; name: string }>>([]);
  const courseId = ref(defaultCourseId ?? 0);
  const authStore = useAuthStore();
  const loading = ref(false);
  const path = ref<LearningPathVO | null>(null);

  async function loadCourses() {
    try {
      const res = await getLearningHomeOverview();
      const list = res.data?.courses ?? [];
      courseOptions.value = list.map((c) => ({
        id: c.courseId,
        name: c.courseName || `课程 #${c.courseId}`
      }));
      if (!courseId.value && res.data?.primaryCourseId) {
        courseId.value = res.data.primaryCourseId;
      } else if (!courseId.value && courseOptions.value.length) {
        courseId.value = courseOptions.value[0].id;
      }
    } catch {
      courseOptions.value = [];
    }
  }

  async function loadPath() {
    if (!courseId.value) return;
    loading.value = true;
    try {
      const studentId = authStore.currentUser?.id;
      const res = await getLearningPath(courseId.value, studentId);
      path.value = res?.data || null;
    } catch {
      path.value = null;
      ElMessage.error('加载学习路径失败');
    } finally {
      loading.value = false;
    }
  }

  onMounted(async () => {
    await loadCourses();
    await loadPath();
  });

  return {
    courseOptions,
    courseId,
    loading,
    path,
    loadPath,
    loadCourses
  };
}

export const useLearningPath = useLearning;
