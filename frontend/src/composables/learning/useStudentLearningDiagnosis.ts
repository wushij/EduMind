import { ref } from 'vue';
import { generateTeachingAdvice } from '@/api/analytics/teaching';
import {
  getStoredTeachingAdvice,
  removeStoredTeachingAdvice,
  saveStoredTeachingAdvice
} from '@/composables/analytics/useLearningAnalytics';
import type { TeachingAdviceVO } from '@/types/analytics/mastery';

export function useStudentLearningDiagnosis() {
  const adviceLoading = ref(false);
  const teachingAdvice = ref<TeachingAdviceVO | null>(null);
  let abortController: AbortController | null = null;
  let manualStopped = false;

  function stopAdvice() {
    manualStopped = true;
    if (abortController) {
      try {
        abortController.abort();
      } catch {
        // ignore
      }
      abortController = null;
    }
    adviceLoading.value = false;
  }

  function clearAdvice(courseId: number, studentId?: number) {
    stopAdvice();
    teachingAdvice.value = null;
    removeStoredTeachingAdvice(courseId, studentId);
  }

  function loadStoredAdvice(courseId: number, studentId?: number) {
    const stored = getStoredTeachingAdvice(courseId, studentId);
    teachingAdvice.value = stored;
    return stored;
  }

  async function generateAdvice(courseId: number, studentId?: number) {
    manualStopped = false;
    stopAdvice();
    manualStopped = false;
    abortController = new AbortController();
    adviceLoading.value = true;
    try {
      const res = await generateTeachingAdvice(
        { courseId, studentId },
        { signal: abortController.signal }
      );
      if (manualStopped) {
        return null;
      }
      if (res?.data) {
        teachingAdvice.value = res.data;
        saveStoredTeachingAdvice(courseId, studentId, res.data);
      }
      return res?.data ?? null;
    } catch (err: unknown) {
      const e = err as { name?: string; code?: string; message?: string };
      if (
        manualStopped ||
        e?.name === 'CanceledError' ||
        e?.name === 'AbortError' ||
        e?.code === 'ERR_CANCELED' ||
        e?.message === 'canceled'
      ) {
        return null;
      }
      teachingAdvice.value = null;
      return null;
    } finally {
      adviceLoading.value = false;
      abortController = null;
    }
  }

  return {
    adviceLoading,
    teachingAdvice,
    generateAdvice,
    clearAdvice,
    stopAdvice,
    loadStoredAdvice
  };
}
