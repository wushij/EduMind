<template>
  <aside class="lesson-studio-sidebar">
    <div class="sidebar-head">
      <h3>课节属性</h3>
      <button type="button" class="close-btn" @click="emit('close')">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <el-form label-position="top" class="sidebar-form">
      <el-form-item>
        <template #label>
          <div class="field-label-row">
            <span>课节标题</span>
            <button type="button" class="sidebar-ai-btn" title="根据正文优化课节标题" @click="aiPolishTitle">
              <el-icon :size="12"><EditPen /></el-icon>
              <span>AI 优化</span>
            </button>
          </div>
        </template>
        <el-input :model-value="meta.title" @update:model-value="patchMeta('title', $event)" />
      </el-form-item>
      <el-form-item>
        <template #label>
          <div class="field-label-row">
            <span>导读</span>
            <div class="field-label-actions">
              <button
                type="button"
                class="sidebar-ai-btn sidebar-ai-btn--ghost"
                title="还原上一版导读"
                @click="rollbackDescription"
              >
                <el-icon :size="12"><RefreshLeft /></el-icon>
                <span>还原</span>
              </button>
              <button
                type="button"
                class="sidebar-ai-btn"
                title="根据课节标题联动全局助手提炼导读（正文可选参考）"
                @click="aiExtractDescription"
              >
                <el-icon :size="12"><EditPen /></el-icon>
                <span>AI 提炼</span>
              </button>
            </div>
          </div>
        </template>
        <el-input
          :model-value="meta.description"
          type="textarea"
          :rows="3"
          placeholder="用于大纲卡片与学习页展示的简要概述"
          maxlength="300"
          show-word-limit
          @update:model-value="patchMeta('description', $event)"
        />
      </el-form-item>
      <el-form-item label="时长（分钟）">
        <el-input-number
          :model-value="meta.durationMinutes"
          :min="5"
          :max="180"
          @update:model-value="patchMeta('durationMinutes', $event)"
        />
      </el-form-item>
      <el-form-item label="课节类型">
        <el-select :model-value="meta.lessonType" @update:model-value="patchMeta('lessonType', $event)">
          <el-option label="讲义精讲" value="LECTURE" />
          <el-option label="实战演练" value="PRACTICE" />
          <el-option label="智能自测" value="QUIZ" />
        </el-select>
      </el-form-item>
    </el-form>

    <section class="sidebar-section">
      <div class="field-label-row section-heading">
        <h4>学习目标</h4>
        <div class="field-label-actions">
          <button
            type="button"
            class="sidebar-ai-btn sidebar-ai-btn--ghost"
            title="还原上一版学习目标"
            @click="rollbackObjective"
          >
            <el-icon :size="12"><RefreshLeft /></el-icon>
            <span>还原</span>
          </button>
          <button
            type="button"
            class="sidebar-ai-btn"
            title="根据课节标题生成可测量学习目标（正文可选参考）"
            @click="aiExtractObjective"
          >
            <el-icon :size="12"><EditPen /></el-icon>
            <span>AI 提炼</span>
          </button>
        </div>
      </div>
      <el-input
        :model-value="objective.title"
        placeholder="标题"
        class="mb-8"
        @update:model-value="patchObjective('title', $event)"
      />
      <el-input
        :model-value="objective.body"
        type="textarea"
        :rows="4"
        placeholder="每行一条目标，建议使用「- 能够…」列表；左侧分屏可查看排版"
        @update:model-value="patchObjective('body', $event)"
      />
    </section>

    <section class="sidebar-section">
      <div class="field-label-row section-heading">
        <h4>考查知识点</h4>
        <div class="field-label-actions">
          <button
            type="button"
            class="sidebar-ai-btn sidebar-ai-btn--ghost"
            title="还原上一版考点关联"
            @click="rollbackKnowledge"
          >
            <el-icon :size="12"><RefreshLeft /></el-icon>
            <span>还原</span>
          </button>
          <button
            type="button"
            class="sidebar-ai-btn"
            title="基于标题与正文推荐课程考点"
            @click="aiRecommendKnowledgePoints"
          >
            <el-icon :size="12"><EditPen /></el-icon>
            <span>AI 推荐</span>
          </button>
        </div>
      </div>
      <el-select
        :model-value="knowledgePointIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        :max-collapse-tags="1"
        placeholder="选择知识点"
        class="w-full kp-multi-select"
        @update:model-value="emit('update:knowledgePointIds', $event)"
      >
        <el-option
          v-for="kp in knowledgePoints ?? []"
          :key="kp.id"
          :label="kp.title || kp.name"
          :value="kp.id"
        />
      </el-select>
    </section>

    <section class="sidebar-section">
      <h4>教学资料块</h4>
      <el-select
        placeholder="从课程资料插入"
        filterable
        class="w-full"
        @change="onResourcePick"
      >
        <el-option
          v-for="r in resources ?? []"
          :key="r.id"
          :label="r.title"
          :value="r.resourceId || r.id"
        />
      </el-select>
      <p class="hint">插入后可在下方列表调整顺序</p>
    </section>

    <section v-if="manageableExtraBlocks.length > 0" class="sidebar-section">
      <h4>附属内容块</h4>
      <p class="hint">正文后的资料、自测等块，可在此调整顺序（考查知识点请在上方选择）</p>
      <div
        v-for="block in manageableExtraBlocks"
        :key="extraBlockKey(block)"
        class="extra-block-row"
      >
        <span class="extra-label">{{ blockLabel(block) }}</span>
        <div class="extra-actions">
          <button type="button" @click="emit('move-extra', extraBlockIndex(block), -1)">上</button>
          <button type="button" @click="emit('move-extra', extraBlockIndex(block), 1)">下</button>
          <button type="button" class="danger" @click="emit('remove-extra', extraBlockIndex(block))">删</button>
        </div>
      </div>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue';
import { Close, RefreshLeft, EditPen } from '@element-plus/icons-vue';
import { useLessonSidebarCopilotActions } from '@/composables/course/useLessonSidebarCopilotActions';
import type { LessonBlock } from '@/types/course/lesson-content';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import type { CourseResourceItem } from '@/types/course/resource';
import type { ObjectiveCalloutState } from '@/composables/course/useLessonDocumentModel';

export interface LessonMetaForm {
  title: string;
  description: string;
  durationMinutes: number;
  lessonType: string;
}

const props = defineProps<{
  courseId: number;
  lessonChapterId: number;
  mainMarkdown: string;
  contentStatus?: string;
  wordCount?: number;
  meta: LessonMetaForm;
  objective: ObjectiveCalloutState;
  knowledgePointIds: number[];
  knowledgePoints: KnowledgePoint[];
  resources: CourseResourceItem[];
  extraBlocks: LessonBlock[];
}>();

const emit = defineEmits<{
  close: [];
  'update:meta': [Partial<LessonMetaForm>];
  'update:objective': [Partial<ObjectiveCalloutState>];
  'update:knowledgePointIds': [number[]];
  'add-resource-block': [number];
  'remove-extra': [number];
  'move-extra': [number, number];
}>();

const objectiveBodyRef = computed(() => props.objective.body);

const extraBlocksSafe = computed(() => props.extraBlocks ?? []);

const manageableExtraBlocks = computed(() =>
  extraBlocksSafe.value.filter(block => block.type !== 'knowledgePoints')
);

function extraBlockIndex(block: LessonBlock) {
  return extraBlocksSafe.value.indexOf(block);
}

function extraBlockKey(block: LessonBlock) {
  if (block.type === 'resource') return `resource-${block.resourceId}`;
  if (block.type === 'heading') return `heading-${block.text}`;
  if (block.type === 'quizEntry') return 'quiz-entry';
  return `${block.type}-${extraBlockIndex(block)}`;
}

const {
  aiExtractDescription,
  aiRecommendKnowledgePoints,
  aiExtractObjective,
  aiPolishTitle,
  rollbackDescription,
  rollbackObjective,
  rollbackKnowledge
} = useLessonSidebarCopilotActions({
  courseId: props.courseId,
  lessonChapterId: props.lessonChapterId,
  meta: props.meta,
  mainMarkdown: toRef(props, 'mainMarkdown'),
  objectiveBody: objectiveBodyRef,
  knowledgePoints: toRef(props, 'knowledgePoints'),
  contentStatus: computed(() => props.contentStatus),
  wordCount: computed(() => props.wordCount ?? 0),
  onPatchDescription: value => emit('update:meta', { description: value })
});

function patchMeta(key: keyof LessonMetaForm, value: unknown) {
  emit('update:meta', { [key]: value } as Partial<LessonMetaForm>);
}

function patchObjective(key: keyof ObjectiveCalloutState, value: string) {
  emit('update:objective', { [key]: value });
}

function onResourcePick(resourceId: number) {
  if (resourceId) emit('add-resource-block', resourceId);
}

function blockLabel(block: LessonBlock) {
  if (block.type === 'resource') return `资料 #${block.resourceId}`;
  if (block.type === 'heading') return `标题：${block.text}`;
  if (block.type === 'quizEntry') return '智能自测入口';
  if (block.type === 'knowledgePoints') return '知识点块';
  return block.type;
}
</script>

<style scoped lang="scss">
.lesson-studio-sidebar {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid #e2e8f0;
  background: #fff;
  overflow-y: auto;
  padding: 12px 14px 24px;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  h3 {
    margin: 0;
    font-size: 15px;
  }
}

.close-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #94a3b8;
}

.sidebar-section {
  margin-top: 18px;

  h4 {
    margin: 0 0 8px;
    font-size: 13px;
    color: #334155;
  }
}

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.section-heading {
  margin-bottom: 8px;

  h4 {
    margin: 0;
  }
}

.field-label-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: flex-end;
}

.sidebar-ai-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
  font-size: 11px;
  cursor: pointer;
  line-height: 1.4;

  &:hover {
    background: rgba(59, 130, 246, 0.16);
    border-color: rgba(59, 130, 246, 0.55);
  }
}

.sidebar-ai-btn--ghost {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #64748b;

  &:hover {
    color: #334155;
    background: #f1f5f9;
  }
}

:deep(.el-form-item__label) {
  width: 100%;
  padding-right: 0;
}

.mb-8 {
  margin-bottom: 8px;
}

.w-full {
  width: 100%;
}

.kp-multi-select {
  :deep(.el-select__wrapper) {
    min-height: 32px;
  }

  :deep(.el-select__selection) {
    flex-wrap: nowrap;
  }

  :deep(.el-select__selected-item) {
    max-width: calc(100% - 52px);
  }

  :deep(.el-tag) {
    max-width: 100%;
  }

  :deep(.el-tag__content) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-tag.el-tag--info) {
    border-radius: 6px;
  }
}

.hint {
  margin: 6px 0 0;
  font-size: 11px;
  color: #94a3b8;
}

.extra-block-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 6px;
  font-size: 12px;
}

.extra-actions {
  display: flex;
  gap: 4px;

  button {
    border: none;
    background: #e2e8f0;
    border-radius: 6px;
    padding: 2px 6px;
    cursor: pointer;
    font-size: 11px;

    &.danger {
      color: #dc2626;
    }
  }
}
</style>
