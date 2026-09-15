import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { generateLessonPlan } from '@/api/ai/lesson';

export function renderLessonPlanMarkdown(markdown: string): string {
  return markdown
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\n/g, '<br/>');
}

export function useLessonPlan() {
  const form = ref({
    courseId: 1,
    topic: '',
    hours: 2,
    objectives: ''
  });
  const loading = ref(false);
  const result = ref('');

  const renderedHtml = computed(() => renderLessonPlanMarkdown(result.value));

  async function handleGenerate() {
    if (!form.value.topic.trim()) {
      ElMessage.warning('请输入授课主题');
      return;
    }
    loading.value = true;
    try {
      const res = await generateLessonPlan(form.value);
      result.value = res?.data?.content || '';
      ElMessage.success('教案生成完成');
    } catch {
      ElMessage.error('教案生成失败');
    } finally {
      loading.value = false;
    }
  }

  function copyResult() {
    navigator.clipboard.writeText(result.value);
    ElMessage.success('已复制到剪贴板');
  }

  return {
    form,
    loading,
    result,
    renderedHtml,
    handleGenerate,
    copyResult
  };
}
