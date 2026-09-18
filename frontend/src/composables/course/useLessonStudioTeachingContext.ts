import { onMounted, onUnmounted, watch, type Ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import type { TeachingCopilotContext } from '@/types/ai/teaching-copilot-context';
import type { LessonMarkdownEditorExpose } from '@/composables/course/useLessonStudioCopilotBridge';

const DRAFT_EXCERPT_MAX = 1500;

type SyncOptions = {
  courseId: number;
  lessonChapterId: number;
  meta: { title: string; description: string; lessonType: string };
  studio: { mainMarkdown: string; objectiveCallout: { body: string } };
  wordCount: Ref<number>;
  contentStatus?: Ref<string | undefined>;
  editorRef: Ref<LessonMarkdownEditorExpose | null>;
};

export function useLessonStudioTeachingContext(options: SyncOptions) {
  const store = useTeachingCopilotStore();

  function buildContext(): TeachingCopilotContext {
    const markdown = options.studio.mainMarkdown || '';
    return {
      contextModule: 'lesson_studio',
      courseId: options.courseId,
      lessonChapterId: options.lessonChapterId,
      title: options.meta.title,
      description: options.meta.description,
      lessonType: options.meta.lessonType,
      wordCount: options.wordCount.value,
      contentStatus: options.contentStatus?.value,
      selectedText: options.editorRef.value?.getSelectedText() || '',
      draftExcerpt: markdown.slice(0, DRAFT_EXCERPT_MAX),
      objectiveExcerpt: (options.studio.objectiveCallout.body || '').slice(0, 600)
    };
  }

  function syncLessonStudioContext() {
    store.setContext(buildContext());
  }

  onMounted(() => {
    syncLessonStudioContext();
  });

  watch(
    () => [
      options.meta.title,
      options.meta.description,
      options.meta.lessonType,
      options.studio.mainMarkdown,
      options.studio.objectiveCallout.body,
      options.wordCount.value,
      options.contentStatus?.value
    ],
    () => syncLessonStudioContext()
  );

  onBeforeRouteLeave((to) => {
    if (!String(to.path).includes('/lessons/') || !String(to.path).endsWith('/edit')) {
      store.clearContext();
    }
  });

  onUnmounted(() => {
    if (store.activeContext?.contextModule === 'lesson_studio') {
      store.clearContext();
    }
  });

  return { syncLessonStudioContext, buildContext };
}
