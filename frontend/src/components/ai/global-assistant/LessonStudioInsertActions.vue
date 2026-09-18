<template>
  <div class="lesson-insert-actions">
    <button
      v-if="applied?.isApplied"
      type="button"
      class="action-chip action-chip--applied"
      :title="'撤销' + targetLabel(applied.target)"
      @click="rollback"
    >
      <el-icon :size="12"><RefreshLeft /></el-icon>
      <span class="action-chip-text">{{ appliedButtonText(applied.target) }}</span>
    </button>

    <template v-else>
      <button
        v-if="primaryIntent === 'knowledge'"
        type="button"
        class="action-chip action-chip--primary"
        @click="applyKnowledge"
      >
        <el-icon :size="12"><Collection /></el-icon>
        <span class="action-chip-text">应用考点建议</span>
      </button>
      <button
        v-else-if="primaryIntent === 'description'"
        type="button"
        class="action-chip action-chip--primary"
        @click="applyDescription"
      >
        <el-icon :size="12"><EditPen /></el-icon>
        <span class="action-chip-text">填入导读</span>
      </button>
      <button
        v-else-if="primaryIntent === 'objective'"
        type="button"
        class="action-chip action-chip--primary"
        @click="applyObjective"
      >
        <el-icon :size="12"><Aim /></el-icon>
        <span class="action-chip-text">填入学习目标</span>
      </button>
      <button
        v-else-if="primaryIntent === 'title'"
        type="button"
        class="action-chip action-chip--primary"
        @click="applyTitle"
      >
        <el-icon :size="12"><Tickets /></el-icon>
        <span class="action-chip-text">设为课节标题</span>
      </button>
      <button
        v-else
        type="button"
        class="action-chip action-chip--primary"
        @click="insertEditor"
      >
        <el-icon :size="12"><DocumentAdd /></el-icon>
        <span class="action-chip-text">插入正文</span>
      </button>

      <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd)">
        <button type="button" class="action-chip">
          <el-icon :size="12"><ArrowDown /></el-icon>
          <span class="action-chip-text">定向插入</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="editor">插入正文</el-dropdown-item>
            <el-dropdown-item command="description">填入导读</el-dropdown-item>
            <el-dropdown-item command="objective">填入学习目标</el-dropdown-item>
            <el-dropdown-item command="title">设为课节标题</el-dropdown-item>
            <el-dropdown-item command="knowledge">应用考点建议</el-dropdown-item>
            <el-dropdown-item v-if="applied?.isApplied" command="rollback" divided>
              撤销本次应用
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  RefreshLeft,
  Collection,
  EditPen,
  Aim,
  Tickets,
  DocumentAdd,
  ArrowDown
} from '@element-plus/icons-vue';
import type { GlobalAssistantMessage } from '@/types/ai/assistant';
import { splitCopilotStream } from '@/utils/ai/copilot-stream-split';
import { normalizeLessonListMarkdown } from '@/utils/format/lesson-markdown';
import {
  resolveLessonInsertIntent,
  extractCleanLessonDescription,
  extractCleanLessonObjective,
  extractCleanLessonTitle,
  extractKnowledgeSuggestions,
  type LessonInsertIntent
} from '@/utils/ai/lesson-copilot-intent';

const props = defineProps<{
  msg: GlobalAssistantMessage;
  msgIndex: number;
  messages: GlobalAssistantMessage[];
}>();

type AppliedState = { target: LessonInsertIntent; isApplied: boolean };

const messageAppliedMap = ref<Record<string, AppliedState>>({});

const msgKey = computed(() => String(props.msg.id ?? props.msgIndex));

const applied = computed(() => messageAppliedMap.value[msgKey.value]);

const answerBody = computed(() => {
  const split = splitCopilotStream(props.msg.content || '');
  return (split.answer || props.msg.content || '').trim();
});

const primaryIntent = computed(() =>
  resolveLessonInsertIntent(props.msg, props.messages, props.msgIndex)
);

watch(
  () => props.msg.id,
  () => {
    delete messageAppliedMap.value[msgKey.value];
  }
);

function targetLabel(target?: LessonInsertIntent): string {
  switch (target) {
    case 'editor':
      return '正文插入';
    case 'description':
      return '课节导读';
    case 'objective':
      return '学习目标';
    case 'title':
      return '课节标题';
    case 'knowledge':
      return '考点关联';
    default:
      return '应用';
  }
}

function appliedButtonText(target?: LessonInsertIntent): string {
  switch (target) {
    case 'editor':
      return '已插入 · 撤销';
    case 'description':
      return '已填入导读 · 撤销';
    case 'objective':
      return '已填入目标 · 撤销';
    case 'title':
      return '已设标题 · 撤销';
    case 'knowledge':
      return '已应用考点 · 撤销';
    default:
      return '已应用 · 撤销';
  }
}

function markApplied(target: LessonInsertIntent) {
  messageAppliedMap.value[msgKey.value] = { target, isApplied: true };
}

function rollback() {
  const state = applied.value;
  if (!state) return;
  switch (state.target) {
    case 'editor':
      window.dispatchEvent(new CustomEvent('rollback-lesson-markdown-snippet'));
      break;
    case 'description':
      window.dispatchEvent(new CustomEvent('rollback-lesson-description'));
      break;
    case 'objective':
      window.dispatchEvent(new CustomEvent('rollback-lesson-objective'));
      break;
    case 'title':
      window.dispatchEvent(new CustomEvent('rollback-lesson-title'));
      break;
    case 'knowledge':
      window.dispatchEvent(new CustomEvent('rollback-lesson-knowledge-suggestions'));
      break;
  }
  delete messageAppliedMap.value[msgKey.value];
  ElMessage.success(`已撤销本次${targetLabel(state.target)}`);
}

function insertEditor() {
  if (!answerBody.value) return;
  window.dispatchEvent(
    new CustomEvent('insert-lesson-markdown-snippet', { detail: { text: answerBody.value } })
  );
  markApplied('editor');
  ElMessage.success('已插入课节正文，可随时撤销');
}

function applyDescription() {
  const cleaned = extractCleanLessonDescription(answerBody.value);
  if (!cleaned) {
    ElMessage.warning('未能识别有效导读内容');
    return;
  }
  window.dispatchEvent(
    new CustomEvent('apply-lesson-description', { detail: { description: cleaned } })
  );
  markApplied('description');
}

function applyObjective() {
  const cleaned = normalizeLessonListMarkdown(extractCleanLessonObjective(answerBody.value));
  if (!cleaned) {
    ElMessage.warning('未能识别有效学习目标');
    return;
  }
  window.dispatchEvent(new CustomEvent('apply-lesson-objective', { detail: { body: cleaned } }));
  markApplied('objective');
}

function applyTitle() {
  const cleaned = extractCleanLessonTitle(answerBody.value);
  if (!cleaned) {
    ElMessage.warning('未能识别有效标题');
    return;
  }
  window.dispatchEvent(new CustomEvent('apply-lesson-title', { detail: { title: cleaned } }));
  markApplied('title');
}

async function applyKnowledge() {
  const extracted = extractKnowledgeSuggestions(answerBody.value);
  if (extracted.length > 0) {
    window.dispatchEvent(
      new CustomEvent('apply-lesson-knowledge-suggestions', { detail: { tags: extracted } })
    );
    markApplied('knowledge');
    return;
  }
  try {
    const { value } = await ElMessageBox.prompt(
      '未能自动解析考点，请输入标签（多个用逗号分隔）：',
      '应用考点建议',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputValue: '' }
    );
    if (value) {
      const manual = value.split(/[,，、\s]+/).filter(Boolean);
      window.dispatchEvent(
        new CustomEvent('apply-lesson-knowledge-suggestions', { detail: { tags: manual } })
      );
      markApplied('knowledge');
    }
  } catch {
    /* cancelled */
  }
}

function handleCommand(cmd: string) {
  if (cmd === 'rollback') {
    rollback();
    return;
  }
  switch (cmd) {
    case 'editor':
      insertEditor();
      break;
    case 'description':
      applyDescription();
      break;
    case 'objective':
      applyObjective();
      break;
    case 'title':
      applyTitle();
      break;
    case 'knowledge':
      void applyKnowledge();
      break;
  }
}
</script>

<style scoped lang="scss">
.lesson-insert-actions {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-left: 4px;
}

.action-chip--primary {
  border-color: rgba(59, 130, 246, 0.35);
  color: #2563eb;
}

.action-chip--applied {
  border-color: rgba(245, 158, 11, 0.45);
  color: #d97706;
}
</style>
