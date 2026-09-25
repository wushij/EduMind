<template>
  <div class="lesson-studio-page" v-loading="loading">
    <LessonStudioNavbar
      :title="meta.title"
      :word-count="wordCount"
      :read-minutes="readMinutes"
      :save-status-text="saveStatusText"
      :saving="saving"
      :view-mode="viewMode"
      :sidebar-open="sidebarOpen"
      :content-status="lesson?.contentStatus"
      @back="handleBack"
      @save-draft="() => void runSave(true)"
      @publish="() => void handlePublish()"
      @unpublish="() => void unpublish()"
      @update:view-mode="viewMode = $event"
      @toggle-sidebar="sidebarOpen = !sidebarOpen"
    />

    <div class="studio-main">
      <div class="editor-area">
        <LessonMarkdownEditor
          ref="markdownEditorRef"
          v-model="studio.mainMarkdown"
          :view-mode="viewMode"
          :title="meta.title"
          :description="meta.description"
          :duration-minutes="meta.durationMinutes"
          :lesson-type="meta.lessonType"
          :objective-title="studio.objectiveCallout.title"
          :objective-body="studio.objectiveCallout.body"
          @save-shortcut="() => void runSave(true)"
          @ai-action="handleAiAction"
          @insert-image="showImageDialog = true"
        />
      </div>

      <LessonStudioSidebar
        v-if="sidebarOpen"
        :course-id="courseId"
        :lesson-chapter-id="lessonId"
        :main-markdown="studio.mainMarkdown"
        :content-status="contentStatus"
        :word-count="wordCount"
        :meta="meta"
        :objective="studio.objectiveCallout"
        :knowledge-point-ids="studio.knowledgePointIds"
        :knowledge-points="knowledgePoints"
        :resources="resources"
        :extra-blocks="studio.extraBlocks"
        @close="sidebarOpen = false"
        @update:meta="patchMeta"
        @update:objective="patchObjective"
        @update:knowledge-point-ids="onKpChange"
        @add-resource-block="addResourceBlock"
        @remove-extra="removeExtraBlock"
        @move-extra="moveExtraBlock"
      />
    </div>

    <el-dialog v-model="showImageDialog" title="插入图片" width="420px">
      <el-input v-model="imageUrl" placeholder="图片 URL" />
      <template #footer>
        <el-button @click="showImageDialog = false">取消</el-button>
        <el-button type="primary" @click="insertImageUrl">插入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import LessonStudioNavbar from '@/components/course/lesson/studio/LessonStudioNavbar.vue';
import type { ViewMode } from '@/components/course/lesson/studio/LessonStudioNavbar.vue';
import LessonMarkdownEditor from '@/components/course/lesson/studio/LessonMarkdownEditor.vue';
import LessonStudioSidebar from '@/components/course/lesson/studio/LessonStudioSidebar.vue';
import { useLessonEditor } from '@/composables/course/useLessonEditor';
import { useLessonAutosave } from '@/composables/course/useLessonAutosave';
import { getCourseKnowledgePoints } from '@/api/course/knowledge-point';
import { getCourseResources } from '@/api/course/resource';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import type { CourseResourceItem } from '@/types/course/resource';
import type { ObjectiveCalloutState } from '@/composables/course/useLessonDocumentModel';
import type { LessonMetaForm } from '@/components/course/lesson/studio/LessonStudioSidebar.vue';
import {
  useLessonStudioCopilotBridge,
  type LessonMarkdownEditorExpose
} from '@/composables/course/useLessonStudioCopilotBridge';
import { useLessonStudioTeachingContext } from '@/composables/course/useLessonStudioTeachingContext';
import { useLessonSidebarCopilotActions } from '@/composables/course/useLessonSidebarCopilotActions';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import {
  shouldOfferLessonEditBackupRestore,
  type LessonEditBackupPayload
} from '@/utils/course/lesson-edit-local-backup';
import { LESSON_AI_PREP_QUERY } from '@/services/course/lesson-studio-entry';
import { takeLessonPrepDiagnosis } from '@/utils/course/lesson-prep-diagnosis';

const route = useRoute();
const router = useRouter();
const courseId = Number(route.params.id);
const lessonId = Number(route.params.lessonId);

const {
  loading,
  saving,
  lesson,
  studio,
  load,
  saveDraft,
  publish,
  unpublish,
  removeExtraBlock,
  moveExtraBlock,
  addResourceBlock
} = useLessonEditor(courseId, lessonId);

const meta = reactive<LessonMetaForm>({
  title: '',
  description: '',
  durationMinutes: 30,
  lessonType: 'LECTURE'
});

const viewMode = ref<ViewMode>('split');
const sidebarOpen = ref(true);
const isDirty = ref(false);
const knowledgePoints = ref<KnowledgePoint[]>([]);
const resources = ref<CourseResourceItem[]>([]);
const showImageDialog = ref(false);
const imageUrl = ref('');
const markdownEditorRef = ref<LessonMarkdownEditorExpose | null>(null);
const teachingCopilotStore = useTeachingCopilotStore();
/** 服务端数据灌入完成前，不把初始化变更当作「用户编辑」 */
const hydrationComplete = ref(false);
/** 教情报告带来的学情诊断（薄弱考点 + 错因），只用于强化 AI 备课的针对性 */
const prepDiagnosis = ref('');

const wordCount = computed(() => (studio.mainMarkdown || '').replace(/\s+/g, '').length);
const readMinutes = computed(() => Math.max(1, Math.ceil(wordCount.value / 350)));
const contentStatus = computed(() => lesson.value?.contentStatus);

useLessonStudioCopilotBridge({
  meta,
  studio,
  knowledgePoints,
  editorRef: markdownEditorRef,
  // 助教「应用到表单」写入后同样按内容快照判定，写入与基线相同的值不算未保存
  onDirty: () => {
    syncDirtyBySnapshot();
  }
});

useLessonStudioTeachingContext({
  courseId,
  lessonChapterId: lessonId,
  meta,
  studio,
  wordCount,
  contentStatus,
  editorRef: markdownEditorRef
});

const lessonSidebarCopilot = useLessonSidebarCopilotActions({
  courseId,
  lessonChapterId: lessonId,
  meta,
  mainMarkdown: computed(() => studio.mainMarkdown),
  objectiveBody: computed(() => studio.objectiveCallout.body),
  knowledgePoints,
  contentStatus,
  wordCount,
  diagnosis: prepDiagnosis,
  onPatchDescription: value => {
    meta.description = value;
  }
});

const { saveStatusText, runSave, readLocalBackup, clearLocalBackup, writeLocalBackup } = useLessonAutosave({
  courseId,
  lessonId,
  isDirty,
  save: persist
});

/** 最近一次与服务端一致的编辑态快照，作为「是否有未保存改动」的判定基线 */
let savedSnapshot = '';

/**
 * 采集当前编辑态快照。
 *
 * <p>把脏判定从「是否触发过响应式变更」升级为「内容是否真的偏离基线」，
 * 服务端回填、AI 回填同值、用户改回原样都不应再被判为未保存。
 */
function currentSnapshot() {
  return JSON.stringify({
    meta: { ...meta },
    mainMarkdown: studio.mainMarkdown,
    objectiveCallout: { ...studio.objectiveCallout },
    extraBlocks: [...studio.extraBlocks],
    knowledgePointIds: [...studio.knowledgePointIds]
  });
}

/** 按内容与基线比对刷新脏标记，返回是否真的存在未保存改动 */
function syncDirtyBySnapshot() {
  const dirty = currentSnapshot() !== savedSnapshot;
  isDirty.value = dirty;
  return dirty;
}

/** 保存成功后重置基线，避免「已保存却仍提示未保存」 */
function markSaved() {
  savedSnapshot = currentSnapshot();
  isDirty.value = false;
}

watch(
  () => [meta, studio.mainMarkdown, studio.objectiveCallout, studio.extraBlocks, studio.knowledgePointIds],
  () => {
    if (!hydrationComplete.value) return;
    if (!syncDirtyBySnapshot()) {
      // 内容已改回与基线一致：撤销脏标记并丢弃本地备份，避免留下过期草稿
      clearLocalBackup();
      return;
    }
    writeLocalBackup({
      meta: { ...meta },
      studio: {
        mainMarkdown: studio.mainMarkdown,
        objectiveCallout: { ...studio.objectiveCallout },
        extraBlocks: [...studio.extraBlocks],
        knowledgePointIds: [...studio.knowledgePointIds]
      }
    });
  },
  { deep: true }
);

onMounted(async () => {
  await load();
  if (lesson.value) {
    meta.title = lesson.value.title;
    meta.description = lesson.value.description || '';
    meta.durationMinutes = lesson.value.durationMinutes || 30;
    meta.lessonType = lesson.value.lessonType || 'LECTURE';
  }
  // 服务端内容基线：即使随后恢复了本地草稿，脏判定仍以「是否偏离服务端」为准
  const serverSnapshot = currentSnapshot();

  const backup = readLocalBackup();
  const backupPayload = backup?.payload as LessonEditBackupPayload | undefined;
  if (
    backupPayload &&
    shouldOfferLessonEditBackupRestore(backupPayload, meta, studio)
  ) {
    try {
      await ElMessageBox.confirm('检测到本地未同步的草稿备份，是否恢复？', '恢复草稿', {
        confirmButtonText: '恢复',
        cancelButtonText: '忽略'
      });
      const p = backupPayload;
      if (p.meta) Object.assign(meta, p.meta);
      if (p.studio) {
        studio.mainMarkdown = p.studio.mainMarkdown ?? studio.mainMarkdown;
        if (p.studio.objectiveCallout) {
          studio.objectiveCallout = { ...p.studio.objectiveCallout };
        }
        studio.extraBlocks = p.studio.extraBlocks ? [...p.studio.extraBlocks] : studio.extraBlocks;
        studio.knowledgePointIds = p.studio.knowledgePointIds
          ? [...p.studio.knowledgePointIds]
          : studio.knowledgePointIds;
      }
      // 恢复的草稿与服务器内容存在差异，脏标记由下方基线比对统一得出
    } catch {
      clearLocalBackup();
    }
  } else if (backupPayload) {
    clearLocalBackup();
  }

  // watch 的 pre flush 回调排在微任务里：必须等初始化回填触发的回调跑完再建立基线，
  // 否则「刚打开课节、什么都没改」也会被判为有未保存修改
  await nextTick();
  savedSnapshot = serverSnapshot;
  syncDirtyBySnapshot();
  hydrationComplete.value = true;

  await maybeAutoPrepare();

  try {
    const kpRes = await getCourseKnowledgePoints(courseId);
    knowledgePoints.value = kpRes.data || [];
  } catch {
    knowledgePoints.value = [];
  }
  try {
    const resRes = await getCourseResources(courseId);
    resources.value = resRes.data || [];
  } catch {
    resources.value = [];
  }
});

/**
 * 由教情报告「新建备课课节」入口跳转而来时（当前路由带 aiPrep=1），
 * 落地即按课节标题与导读（已写入薄弱考点）发起 AI 备课，
 * 省去教师再点一次「AI 辅助 → 一键生成课节正文」。
 */
async function maybeAutoPrepare() {
  if (String(route.query[LESSON_AI_PREP_QUERY] ?? '') !== '1') return;

  // 先摘掉标记，避免刷新、返回或二次跳转时重复触发
  const restQuery = { ...route.query };
  delete restQuery[LESSON_AI_PREP_QUERY];
  void router.replace({ path: route.path, query: restQuery });

  if (!meta.title?.trim()) {
    ElMessage.warning('课节标题为空，无法自动发起备课，请在「AI 辅助」中手动生成');
    return;
  }

  // 取出教情报告带来的学情诊断（读取即销毁），供随后发起的 AI 备课使用
  prepDiagnosis.value = takeLessonPrepDiagnosis(courseId, lessonId);

  // 全局助手的 open 事件监听在 AppLayout 中晚于本页 mounted 注册，
  // 直接刷新该路由时需等一帧，否则事件会被丢弃、备课无法自动发起
  await nextTick();
  await new Promise(resolve => setTimeout(resolve, 0));

  // 新建备课课节属于全新备课场景：显式开启新会话，
  // 否则侧栏会沿用上一节课（或上一个入口）遗留的旧对话
  void handleAiAction('generate', { startNewSession: true });
  ElMessage.info('已按薄弱考点发起 AI 备课：正文请在右侧助教确认后插入，导读可用「AI 提炼」生成更具体的版本');
}

function patchMeta(patch: Partial<LessonMetaForm>) {
  Object.assign(meta, patch);
}

function patchObjective(patch: Partial<ObjectiveCalloutState>) {
  Object.assign(studio.objectiveCallout, patch);
}

function onKpChange(ids: number[]) {
  studio.knowledgePointIds = ids;
}

async function persist(showToast = false) {
  const ok = await saveDraft(
    {
      title: meta.title,
      description: meta.description,
      durationMinutes: meta.durationMinutes,
      lessonType: meta.lessonType,
      knowledgePointIds: studio.knowledgePointIds
    },
    { showToast }
  );
  if (ok) markSaved();
  return ok;
}

async function handlePublish() {
  await publish({
    title: meta.title,
    description: meta.description,
    durationMinutes: meta.durationMinutes,
    lessonType: meta.lessonType
  });
  markSaved();
}

function handleBack() {
  // 未保存确认统一交给 useLessonAutosave 的路由离开守卫，
  // 此处不再自行弹一次，避免一次返回要点两次「确定」
  router.push(`/course/${courseId}/chapters`);
}

function openLessonCopilot(prompt: string, selectedText = '', lessonInsertIntent = 'editor' as const) {
  const excerpt = (studio.mainMarkdown || '').slice(0, 1500);
  teachingCopilotStore.openAssistantWithContext(
    {
      contextModule: 'lesson_studio',
      courseId,
      lessonChapterId: lessonId,
      title: meta.title,
      description: meta.description,
      lessonType: meta.lessonType,
      wordCount: wordCount.value,
      contentStatus: contentStatus.value,
      selectedText,
      draftExcerpt: excerpt,
      objectiveExcerpt: studio.objectiveCallout.body,
      lessonInsertIntent
    },
    prompt
  );
}

async function handleAiAction(command: string, options?: { startNewSession?: boolean }) {
  if (command === 'description') {
    lessonSidebarCopilot.aiExtractDescription();
    return;
  }
  if (command === 'objective') {
    lessonSidebarCopilot.aiExtractObjective();
    return;
  }
  if (command === 'knowledge') {
    lessonSidebarCopilot.aiRecommendKnowledgePoints();
    return;
  }

  if (command === 'generate') {
    lessonSidebarCopilot.aiGenerateLessonBody(options);
    return;
  }

  const currentTitle = meta.title?.trim();
  if (!currentTitle) {
    ElMessage.warning('请先填写课节标题，再使用 AI 辅助');
    return;
  }

  const selected = markdownEditorRef.value?.getSelectedText() || '';
  const excerpt = (studio.mainMarkdown || '').slice(0, 1500);
  let prompt = '';

  if (command === 'continue') {
    const tail = (studio.mainMarkdown || '').slice(-800);
    prompt = selected
      ? `请承接以下选中段落继续撰写课节正文（Markdown，可直接插入）：\n\n${selected}`
      : `请根据课节《${currentTitle}》现有正文结尾自然续写下一小节（Markdown）：\n\n${tail || excerpt}`;
  } else if (command === 'polish') {
    prompt = selected
      ? `请润色以下课节 Markdown 片段，保持教学语气与结构，输出可直接替换的正文：\n\n${selected}`
      : `请润色课节《${currentTitle}》的 Markdown 正文，保持结构清晰、适合课堂学习：\n\n${excerpt}`;
  } else if (command === 'outline') {
    prompt = `请为课节《${currentTitle}》生成 Markdown 小节大纲（使用 ## / ### 标题与要点列表），先搭结构、少写长段落：\n\n${excerpt || '（可结合课节标题与导读展开）'}`;
  } else if (command === 'expand') {
    if (!selected) {
      ElMessage.warning('请先在正文中选中要扩写的段落');
      return;
    }
    prompt = `请在不改变教学主旨的前提下扩写以下段落，补充讲解细节与示例（Markdown，可直接插入）：\n\n${selected}`;
  } else if (command === 'review') {
    prompt = selected
      ? `请从教学目标、难度梯度、案例与练习覆盖角度审查以下片段，给出可执行的修改建议：\n\n${selected}`
      : `请审查课节《${currentTitle}》全文的教学设计，指出薄弱处并给出修改建议：\n\n${excerpt}`;
  } else if (command === 'mermaid') {
    prompt = selected
      ? `请将以下教学描述转为一段 Mermaid 流程图或结构图代码块（含 graph TD 或 flowchart）：\n\n${selected}`
      : `请根据课节《${currentTitle}》正文绘制一张 Mermaid 教学流程图：\n\n${excerpt}`;
  } else {
    prompt = `请为课节《${currentTitle}》生成可直接使用的教学正文大纲与 Markdown 正文草稿。`;
  }

  openLessonCopilot(prompt, selected, 'editor');
}

function insertImageUrl() {
  const url = imageUrl.value.trim();
  if (!url) {
    ElMessage.warning('请输入图片地址');
    return;
  }
  studio.mainMarkdown += `\n![图片](${url})\n`;
  showImageDialog.value = false;
  imageUrl.value = '';
  isDirty.value = true;
}
</script>

<style scoped lang="scss">
.lesson-studio-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 64px);
  margin: -8px -12px 0;
  background: #f1f5f9;
}

.studio-main {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.editor-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
</style>
