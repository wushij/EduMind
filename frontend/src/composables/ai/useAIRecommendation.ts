import { ref, onMounted, watch } from 'vue';
import { getRecommendations } from '@/api/ai/recommendation';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

/**
 * AI 学习推荐数据装配。
 *
 * 课程来源统一收敛到 useTeacherCourses：显式指定 → 上次访问的课程 → 可用课程列表首项。
 * 历史实现把默认课程写死为 1，但库中课程 ID 形如 101/102/258，
 * 于是每次打开页面都会打到一门不存在的课程上：后端抛「课程不存在」，
 * 前端再叠一条「加载推荐失败」，页面永远停在空态。
 * 解析不到课程时 courseId 为 0，调用方应据此跳过请求并展示空态。
 */
export function useAIRecommendation(initialCourseId?: number) {
  const {
    courseOptions,
    courseId,
    loading: courseLoading,
    loadCourses
  } = useTeacherCourses(initialCourseId);

  const loading = ref(false);
  const questions = ref<Array<{ id: number; stem: string }>>([]);
  const resources = ref<Array<{ id: number; title: string }>>([]);

  async function loadData() {
    // 课程尚未解析出来（或被纠正中）时不去打扰后端，直接展示空态
    if (!courseId.value || courseId.value <= 0) {
      questions.value = [];
      resources.value = [];
      return;
    }
    loading.value = true;
    try {
      const res = await getRecommendations(courseId.value);
      questions.value = res?.data?.questions || [];
      resources.value = res?.data?.resources || [];
    } catch {
      // 具体失败原因由 HTTP 拦截器统一提示，这里只清空列表，避免展示上一次的脏数据
      questions.value = [];
      resources.value = [];
    } finally {
      loading.value = false;
    }
  }

  // courseId 可能被 useTeacherCourses 纠正为首门可用课程（或由用户主动切换），纠正后必须重载
  watch(courseId, () => {
    loadData();
  });

  onMounted(loadData);

  return {
    courseId,
    courseOptions,
    courseLoading,
    loading,
    questions,
    resources,
    loadCourses,
    loadData
  };
}
