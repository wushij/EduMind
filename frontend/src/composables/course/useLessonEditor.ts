import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getLessonDetail,
  updateLesson,
  publishLesson,
  unpublishLesson,
  type LessonDetail
} from '@/api/course/lesson';
import {
  parseLessonContent,
  serializeLessonContent,
  type LessonContentDocument,
  type LessonBlock
} from '@/types/course/lesson-content';
import {
  blocksToStudio,
  studioToBlocks,
  type LessonStudioDocument
} from '@/composables/course/useLessonDocumentModel';

export function useLessonEditor(courseId: number, lessonId: number) {
  const loading = ref(true);
  const saving = ref(false);
  const lesson = ref<LessonDetail | null>(null);
  const content = ref<LessonContentDocument>({ version: 1, blocks: [] });

  const studio = reactive<LessonStudioDocument>({
    mainMarkdown: '',
    objectiveCallout: { title: '学习目标', body: '' },
    extraBlocks: [],
    knowledgePointIds: []
  });

  function syncFromContent() {
    const kpIds =
      lesson.value?.knowledgePoints?.map(k => Number(k.id)).filter(Boolean) || [];
    const mapped = blocksToStudio(content.value, kpIds);
    studio.mainMarkdown = mapped.mainMarkdown;
    studio.objectiveCallout = { ...mapped.objectiveCallout };
    studio.extraBlocks = [...mapped.extraBlocks];
    studio.knowledgePointIds =
      mapped.knowledgePointIds.length ? mapped.knowledgePointIds : kpIds;
  }

  function buildContentJson(): string {
    content.value = studioToBlocks(studio);
    return serializeLessonContent(content.value);
  }

  async function load() {
    loading.value = true;
    try {
      const res = await getLessonDetail(courseId, lessonId, true);
      lesson.value = res.data || null;
      content.value = parseLessonContent(lesson.value?.contentJson);
      syncFromContent();
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '加载课节失败');
    } finally {
      loading.value = false;
    }
  }

  async function saveDraft(
    meta?: {
      title?: string;
      description?: string;
      durationMinutes?: number;
      lessonType?: string;
      knowledgePointIds?: number[];
    },
    options?: { showToast?: boolean }
  ): Promise<boolean> {
    const showToast = options?.showToast !== false;
    saving.value = true;
    try {
      await updateLesson(courseId, lessonId, {
        title: meta?.title ?? lesson.value?.title,
        description: meta?.description ?? lesson.value?.description,
        durationMinutes: meta?.durationMinutes ?? lesson.value?.durationMinutes,
        lessonType: meta?.lessonType ?? lesson.value?.lessonType,
        contentJson: buildContentJson(),
        knowledgePointIds: meta?.knowledgePointIds ?? studio.knowledgePointIds
      });
      if (showToast) {
        ElMessage.success('课节草稿已保存');
      }
      await load();
      return true;
    } catch (err: unknown) {
      if (showToast) {
        ElMessage.error(err instanceof Error ? err.message : '保存失败');
      }
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function publish(meta?: {
    title?: string;
    description?: string;
    durationMinutes?: number;
    lessonType?: string;
  }) {
    const ok = await saveDraft(meta, { showToast: false });
    if (!ok) return false;
    await publishLesson(courseId, lessonId);
    ElMessage.success('课节已发布');
    await load();
    return true;
  }

  async function unpublish() {
    await unpublishLesson(courseId, lessonId);
    ElMessage.success('已撤回为草稿');
    await load();
  }

  function removeExtraBlock(index: number) {
    studio.extraBlocks.splice(index, 1);
  }

  function moveExtraBlock(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= studio.extraBlocks.length) return;
    const [item] = studio.extraBlocks.splice(index, 1);
    studio.extraBlocks.splice(target, 0, item);
  }

  function addResourceBlock(resourceId: number) {
    studio.extraBlocks.push({
      type: 'resource',
      resourceId,
      display: 'embed'
    });
  }

  return {
    loading,
    saving,
    lesson,
    content,
    studio,
    load,
    syncFromContent,
    buildContentJson,
    saveDraft,
    publish,
    unpublish,
    removeExtraBlock,
    moveExtraBlock,
    addResourceBlock
  };
}
