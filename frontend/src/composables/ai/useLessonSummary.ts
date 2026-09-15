import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { generateSummary } from '@/api/ai/summary';

export function useLessonSummary() {
  const documentId = ref<number | undefined>();
  const content = ref('');
  const loading = ref(false);
  const result = ref('');

  async function handleSummarize() {
    loading.value = true;
    try {
      const res = await generateSummary({
        documentId: documentId.value,
        content: content.value || undefined
      });
      result.value = res?.data?.content || '';
      ElMessage.success('总结生成完成');
    } catch {
      ElMessage.error('总结生成失败');
    } finally {
      loading.value = false;
    }
  }

  return {
    documentId,
    content,
    loading,
    result,
    handleSummarize
  };
}
