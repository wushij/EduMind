import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getLessonDetail,
  updateLessonProgress,
  type LessonDetail
} from '@/api/course/lesson';
import { parseLessonContent, type LessonContentDocument } from '@/types/course/lesson-content';

export function useLessonLearn(courseId: number, lessonId: number, preview = false) {
  const loading = ref(true);
  const lesson = ref<LessonDetail | null>(null);
  const content = ref<LessonContentDocument>({ version: 1, blocks: [] });
  const errorMessage = ref('');

  let heartbeatTimer: ReturnType<typeof setInterval> | null = null;

  async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
      const res = await getLessonDetail(courseId, lessonId, preview);
      lesson.value = res.data || null;
      content.value = parseLessonContent(lesson.value?.contentJson);
      await updateLessonProgress(courseId, lessonId, {
        progressPercent: Math.max(lesson.value?.progress?.progressPercent ?? 0, 1),
        durationMinutes: 0
      }).catch(() => {});
    } catch (err: unknown) {
      lesson.value = null;
      errorMessage.value = err instanceof Error ? err.message : '课节加载失败';
    } finally {
      loading.value = false;
    }
  }

  async function markCompleted() {
    try {
      const res = await updateLessonProgress(courseId, lessonId, {
        status: 'COMPLETED',
        progressPercent: 100,
        durationMinutes: 5
      });
      if (lesson.value && res.data) {
        lesson.value.progress = res.data;
      }
      ElMessage.success('已标记本课节学习完成');
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '更新进度失败');
    }
  }

  function startHeartbeat() {
    heartbeatTimer = setInterval(() => {
      void updateLessonProgress(courseId, lessonId, {
        progressPercent: lesson.value?.progress?.progressPercent ?? 10,
        durationMinutes: 1
      }).catch(() => {});
    }, 30000);
  }

  onMounted(() => {
    void load();
    startHeartbeat();
  });

  onUnmounted(() => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
    }
  });

  return {
    loading,
    lesson,
    content,
    errorMessage,
    load,
    markCompleted
  };
}
