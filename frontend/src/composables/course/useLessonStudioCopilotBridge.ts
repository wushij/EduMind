import { onMounted, onUnmounted, type Ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { ObjectiveCalloutState } from '@/composables/course/useLessonDocumentModel';
import type { LessonMetaForm } from '@/components/course/lesson/studio/LessonStudioSidebar.vue';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import { matchKnowledgePointIds } from '@/utils/ai/lesson-copilot-intent';

type FormHistoryStack = {
  title: string[];
  description: string[];
  objective: string[];
  knowledgePointIds: number[][];
};

export type LessonMarkdownEditorExpose = {
  getSelectedText: () => string;
  focusEditor: () => void;
  insertSnippetAtCursor: (snippet: string) => void;
  rollbackLastSnippet: () => void;
};

type BridgeOptions = {
  meta: LessonMetaForm;
  studio: {
    mainMarkdown: string;
    objectiveCallout: ObjectiveCalloutState;
    knowledgePointIds: number[];
  };
  knowledgePoints: Ref<KnowledgePoint[]>;
  editorRef: Ref<LessonMarkdownEditorExpose | null>;
  onDirty: () => void;
};

export function useLessonStudioCopilotBridge(options: BridgeOptions) {
  const previousFormHistory: FormHistoryStack = {
    title: [],
    description: [],
    objective: [],
    knowledgePointIds: []
  };

  function pushHistory<T>(stack: T[], value: T) {
    stack.push(value);
    if (stack.length > 20) stack.shift();
  }

  function onInsertMarkdown(e: Event) {
    const detail = (e as CustomEvent<{ text?: string }>).detail;
    const text = detail?.text?.trim();
    if (!text) return;
    options.editorRef.value?.insertSnippetAtCursor(`\n\n${text}\n\n`);
    options.onDirty();
  }

  function onRollbackMarkdown() {
    options.editorRef.value?.rollbackLastSnippet();
    options.onDirty();
  }

  function onApplyTitle(e: Event) {
    const title = (e as CustomEvent<{ title?: string }>).detail?.title?.trim();
    if (!title) return;
    pushHistory(previousFormHistory.title, options.meta.title);
    options.meta.title = title;
    options.onDirty();
  }

  function onRollbackTitle() {
    const prev = previousFormHistory.title.pop();
    if (prev !== undefined) options.meta.title = prev;
    options.onDirty();
  }

  function onApplyDescription(e: Event) {
    const summary = (e as CustomEvent<{ description?: string }>).detail?.description?.trim();
    if (!summary) return;
    pushHistory(previousFormHistory.description, options.meta.description);
    options.meta.description = summary;
    options.onDirty();
  }

  function onRollbackDescription() {
    const prev = previousFormHistory.description.pop();
    if (prev !== undefined) options.meta.description = prev;
    options.onDirty();
  }

  function onApplyObjective(e: Event) {
    const body = (e as CustomEvent<{ body?: string }>).detail?.body?.trim();
    if (!body) return;
    pushHistory(previousFormHistory.objective, options.studio.objectiveCallout.body);
    options.studio.objectiveCallout.body = body;
    options.onDirty();
  }

  function onRollbackObjective() {
    const prev = previousFormHistory.objective.pop();
    if (prev !== undefined) options.studio.objectiveCallout.body = prev;
    options.onDirty();
  }

  function onApplyKnowledge(e: Event) {
    const tags = (e as CustomEvent<{ tags?: string[] }>).detail?.tags || [];
    const matched = matchKnowledgePointIds(tags, options.knowledgePoints.value);
    if (!matched.length) {
      ElMessage.warning('未匹配到课程内已有考点，请先在侧栏手动关联');
      return;
    }
    pushHistory(previousFormHistory.knowledgePointIds, [...options.studio.knowledgePointIds]);
    const merged = [...options.studio.knowledgePointIds];
    for (const id of matched) {
      if (!merged.includes(id)) merged.push(id);
    }
    options.studio.knowledgePointIds = merged;
    options.onDirty();
    ElMessage.success(`已关联 ${matched.length} 个考点建议`);
  }

  function onRollbackKnowledge() {
    const prev = previousFormHistory.knowledgePointIds.pop();
    if (prev) options.studio.knowledgePointIds = prev;
    options.onDirty();
  }

  onMounted(() => {
    window.addEventListener('insert-lesson-markdown-snippet', onInsertMarkdown);
    window.addEventListener('rollback-lesson-markdown-snippet', onRollbackMarkdown);
    window.addEventListener('apply-lesson-title', onApplyTitle);
    window.addEventListener('rollback-lesson-title', onRollbackTitle);
    window.addEventListener('apply-lesson-description', onApplyDescription);
    window.addEventListener('rollback-lesson-description', onRollbackDescription);
    window.addEventListener('apply-lesson-objective', onApplyObjective);
    window.addEventListener('rollback-lesson-objective', onRollbackObjective);
    window.addEventListener('apply-lesson-knowledge-suggestions', onApplyKnowledge);
    window.addEventListener('rollback-lesson-knowledge-suggestions', onRollbackKnowledge);
  });

  onUnmounted(() => {
    window.removeEventListener('insert-lesson-markdown-snippet', onInsertMarkdown);
    window.removeEventListener('rollback-lesson-markdown-snippet', onRollbackMarkdown);
    window.removeEventListener('apply-lesson-title', onApplyTitle);
    window.removeEventListener('rollback-lesson-title', onRollbackTitle);
    window.removeEventListener('apply-lesson-description', onApplyDescription);
    window.removeEventListener('rollback-lesson-description', onRollbackDescription);
    window.removeEventListener('apply-lesson-objective', onApplyObjective);
    window.removeEventListener('rollback-lesson-objective', onRollbackObjective);
    window.removeEventListener('apply-lesson-knowledge-suggestions', onApplyKnowledge);
    window.removeEventListener('rollback-lesson-knowledge-suggestions', onRollbackKnowledge);
  });
}
