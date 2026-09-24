import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { courseLabel } from '@/utils/learning/course-label';

/**
 * 学习中心通用「课程下拉选项」。
 *
 * 课程解析顺序统一收敛到 useTeacherCourses：显式指定 → 上次访问的课程 → 可用课程列表首项。
 * 不再像历史实现那样默认兜底到写死的课程 id（`defaultCourseId = 102`），
 * 否则任何用户一进页面都会落到同一门固定课程，与真实教学上下文无关。
 * 解析不到课程时 courseId 为 0，调用方应据此跳过请求并展示空态。
 */
export function useLearningCourseOptions(defaultCourseId?: number) {
  const { courseOptions, courseId, loading, loadCourses } = useTeacherCourses(defaultCourseId);
  return { courseOptions, courseId, loading, loadCourses, courseLabel };
}
