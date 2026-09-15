import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getRecommendations } from '@/api/ai/recommendation';

export function useAIRecommendation(initialCourseId = 1) {
  const courseId = ref(initialCourseId);
  const loading = ref(false);
  const questions = ref<Array<{ id: number; stem: string }>>([]);
  const resources = ref<Array<{ id: number; title: string }>>([]);

  async function loadData() {
    loading.value = true;
    try {
      const res = await getRecommendations(courseId.value);
      questions.value = res?.data?.questions || [];
      resources.value = res?.data?.resources || [];
    } catch {
      questions.value = [];
      resources.value = [];
      ElMessage.error('加载推荐失败');
    } finally {
      loading.value = false;
    }
  }

  onMounted(loadData);

  return {
    courseId,
    loading,
    questions,
    resources,
    loadData
  };
}
