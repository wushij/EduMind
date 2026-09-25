import type { Router } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getChapters } from '@/api/course/chapter';
import type { Chapter } from '@/types/course/chapter';

/**
 * 递归查找课程大纲中第一个课节 ID。
 * 章节树的课节既可能挂在本级 sections，也可能挂在 children 子树里。
 */
function findFirstLessonId(chapters: Chapter[]): number | null {
  for (const chapter of chapters) {
    if (!chapter) continue;
    for (const section of chapter.sections ?? []) {
      if (section?.id) {
        return section.id;
      }
    }
    const nested = findFirstLessonId(chapter.children ?? []);
    if (nested) {
      return nested;
    }
  }
  return null;
}

/**
 * 进入课程内的课节教案工作台（Lesson Studio），AI 备课能力在该处提供。
 * 课程还没有课节时不做无效跳转，改为引导教师先建课节。
 */
export async function openLessonStudio(router: Router, courseId: number): Promise<void> {
  try {
    const res = await getChapters(courseId);
    const lessonId = findFirstLessonId(res?.data ?? []);
    if (lessonId) {
      router.push(`/course/${courseId}/lessons/${lessonId}/edit`);
      return;
    }
  } catch {
    // 大纲拉取失败时退回大纲页，由教师自行选择课节
  }
  ElMessage.info('当前课程还没有课节，请先在教学大纲中创建课节后再使用 AI 备课');
  router.push(`/course/${courseId}/chapters`);
}
