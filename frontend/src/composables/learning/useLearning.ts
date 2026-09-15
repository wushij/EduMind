import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getLearningPath } from '@/api/learning/learning-path';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { useAuthStore } from '@/stores/auth/auth';
import type { LearningPathVO } from '@/types/learning/learning-path';

export function useLearning(defaultCourseId = 102) {
  const { courseOptions, courseId } = useTeacherCourses(defaultCourseId);
  const authStore = useAuthStore();
  const loading = ref(false);
  const path = ref<LearningPathVO | null>(null);

  async function loadPath() {
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

  onMounted(loadPath);

  return {
    courseOptions,
    courseId,
    loading,
    path,
    loadPath
  };
}

export const useLearningPath = useLearning;
