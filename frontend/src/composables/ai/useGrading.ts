import { ref } from 'vue';
import { getGradingResult, triggerGrading, reviewGrading } from '@/api/ai/grading';
import { ElMessage } from 'element-plus';

export function useGrading() {
  const gradingResult = ref<any>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchGrading(submissionId: number) {
    loading.value = true;
    error.value = null;
    try {
      const res = await getGradingResult(submissionId);
      gradingResult.value = res.data;
      return res.data;
    } catch (err: any) {
      error.value = err?.message || '获取评阅结果失败';
      console.warn('获取评阅结果异常:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function startGrading(submissionId: number) {
    loading.value = true;
    error.value = null;
    try {
      const res = await triggerGrading(submissionId);
      gradingResult.value = res.data;
      ElMessage.success('AI 智能评阅任务已成功触发！');
      return res.data;
    } catch (err: any) {
      error.value = err?.message || '触发评阅失败';
      ElMessage.error(err?.message || '触发 AI 智能评阅失败，请检查后端 AI 服务状态');
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function submitReview(
    submissionId: number,
    items: Array<{ questionId: number; score: number; teacherComment?: string }>
  ) {
    loading.value = true;
    error.value = null;
    try {
      await reviewGrading(submissionId, items);
      ElMessage.success('成绩复核已成功提交！');
      await fetchGrading(submissionId);
    } catch (err: any) {
      error.value = err?.message || '提交复核失败';
      ElMessage.error(err?.message || '成绩复核提交失败，请重试');
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function batchGrade(submissionIds: number[]) {
    loading.value = true;
    const results: Record<number, boolean> = {};
    for (const sid of submissionIds) {
      try {
        await triggerGrading(sid);
        results[sid] = true;
      } catch (err) {
        results[sid] = false;
      }
    }
    loading.value = false;
    const successCount = Object.values(results).filter(Boolean).length;
    ElMessage.success(`批量评阅完成：成功 ${successCount} / ${submissionIds.length} 份`);
    return results;
  }

  return {
    gradingResult,
    loading,
    error,
    fetchGrading,
    startGrading,
    submitReview,
    batchGrade
  };
}
