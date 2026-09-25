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

/**
 * 事件名 → 当前活跃处理器。
 *
 * <p>本桥接用 onMounted 注册 window 事件；一旦页面出现重复挂载（HMR、路由 key 变化重建、
 * 多实例并存），监听器就会叠加：一次点击被 N 个处理器响应，表现为重复写入表单、
 * 重复弹出「已关联 N 个考点建议」。这里在注册前先摘掉上一个处理器，
 * 保证同一事件同一时刻只有一个处理者。</p>
 */
const activeLessonStudioListeners = new Map<string, EventListener>();

function bindLessonStudioEvent(name: string, handler: EventListener) {
  const previous = activeLessonStudioListeners.get(name);
  if (previous && previous !== handler) {
    window.removeEventListener(name, previous);
  }
  activeLessonStudioListeners.set(name, handler);
  window.addEventListener(name, handler);
}

function unbindLessonStudioEvent(name: string, handler: EventListener) {
  // 仅当自己仍是当前登记的处理者时才注销登记，避免误摘后来实例的监听
  if (activeLessonStudioListeners.get(name) === handler) {
    activeLessonStudioListeners.delete(name);
  }
  window.removeEventListener(name, handler);
}

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

    // 幂等：一次备课对话里往往有多条助手回答都推荐同一批考点，
    // 已关联过的考点不再重复写入、也不再反复弹出「已关联」成功提示
    const appended = matched.filter(id => !options.studio.knowledgePointIds.includes(id));
    if (appended.length === 0) {
      ElMessage({
        type: 'info',
        message: `这 ${matched.length} 个考点已在关联列表中，无需重复应用`,
        grouping: true
      });
      return;
    }

    pushHistory(previousFormHistory.knowledgePointIds, [...options.studio.knowledgePointIds]);
    options.studio.knowledgePointIds = [...options.studio.knowledgePointIds, ...appended];
    options.onDirty();

    const skipped = matched.length - appended.length;
    ElMessage({
      type: 'success',
      message:
        skipped > 0
          ? `已关联 ${appended.length} 个考点建议（另有 ${skipped} 个已存在）`
          : `已关联 ${appended.length} 个考点建议`,
      // 同一文案多次触发时合并为一条，避免右上角堆叠成一串重复提示
      grouping: true
    });
  }

  function onRollbackKnowledge() {
    const prev = previousFormHistory.knowledgePointIds.pop();
    if (prev) options.studio.knowledgePointIds = prev;
    options.onDirty();
  }

  /** 本实例负责的 window 事件清单（工作台侧栏的「应用到表单」全部走全局事件） */
  const windowHandlers: Array<[string, EventListener]> = [
    ['insert-lesson-markdown-snippet', onInsertMarkdown],
    ['rollback-lesson-markdown-snippet', onRollbackMarkdown],
    ['apply-lesson-title', onApplyTitle],
    ['rollback-lesson-title', onRollbackTitle],
    ['apply-lesson-description', onApplyDescription],
    ['rollback-lesson-description', onRollbackDescription],
    ['apply-lesson-objective', onApplyObjective],
    ['rollback-lesson-objective', onRollbackObjective],
    ['apply-lesson-knowledge-suggestions', onApplyKnowledge],
    ['rollback-lesson-knowledge-suggestions', onRollbackKnowledge]
  ];

  onMounted(() => {
    for (const [name, handler] of windowHandlers) bindLessonStudioEvent(name, handler);
  });

  onUnmounted(() => {
    for (const [name, handler] of windowHandlers) unbindLessonStudioEvent(name, handler);
  });
}
