<template>
  <div class="lesson-markdown-editor" :class="`view-${viewMode}`">
    <div v-show="viewMode !== 'preview'" class="editor-pane">
      <div class="editor-toolbar">
        <div class="toolbar-group">
          <button type="button" class="tool-btn" :disabled="!canUndo" @click="history.undo()">
            <el-icon><RefreshLeft /></el-icon>
          </button>
          <button type="button" class="tool-btn" :disabled="!canRedo" @click="history.redo()">
            <el-icon><RefreshRight /></el-icon>
          </button>
        </div>
        <div class="toolbar-divider" />
        <div class="toolbar-group">
          <button type="button" class="tool-btn" title="加粗" @click="insertWrap('**', '**', '加粗')">
            <span class="tool-label bold">B</span>
          </button>
          <button type="button" class="tool-btn" title="斜体" @click="insertWrap('*', '*', '斜体')">
            <span class="tool-label italic">I</span>
          </button>
          <button type="button" class="tool-btn" title="二级标题" @click="insertPrefix('## ', '小节标题')">
            <span class="tool-label">H2</span>
          </button>
          <button type="button" class="tool-btn" title="代码块" @click="insertSnippet('\n```java\n// 示例代码\n```\n')">
            <el-icon><DocumentCopy /></el-icon>
          </button>
          <button type="button" class="tool-btn" title="表格" @click="insertTable">
            <span class="tool-label">表格</span>
          </button>
          <button type="button" class="tool-btn" title="链接" @click="insertSnippet('[链接文字](https://)')">
            <el-icon><Link /></el-icon>
          </button>
          <button type="button" class="tool-btn" title="Mermaid" @click="insertMermaid">
            <span class="tool-label">Mermaid</span>
          </button>
          <button type="button" class="tool-btn" title="图片" @click="emit('insert-image')">
            <el-icon><Picture /></el-icon>
          </button>
        </div>
        <div class="toolbar-divider" />
        <div class="toolbar-group toolbar-group--ai">
          <LessonEditorAiMenu @command="cmd => emit('ai-action', cmd)" />
        </div>
      </div>

      <div class="textarea-wrapper">
        <textarea
          ref="textareaRef"
          :value="modelValue"
          class="markdown-textarea"
          placeholder="在此编写课节 Markdown 正文（支持 Ctrl+S 保存）"
          @input="onTextareaInput"
          @keydown="handleKeydown"
          @click="recordCursor"
          @keyup="recordCursor"
          @select="recordCursor"
          @blur="recordCursor"
        />
      </div>
    </div>

    <div v-show="viewMode !== 'edit'" class="preview-pane-wrapper">
      <LessonPreviewPane
        :markdown="modelValue"
        :title="title"
        :description="description"
        :duration-minutes="durationMinutes"
        :lesson-type="lessonType"
        :objective-title="objectiveTitle"
        :objective-body="objectiveBody"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { RefreshLeft, RefreshRight, DocumentCopy, Link, Picture } from '@element-plus/icons-vue';
import LessonPreviewPane from './LessonPreviewPane.vue';
import LessonEditorAiMenu from './LessonEditorAiMenu.vue';
import { useMarkdownEditorHistory } from '@/composables/course/useMarkdownEditorHistory';
import type { ViewMode } from './LessonStudioNavbar.vue';

const props = defineProps<{
  modelValue: string;
  viewMode: ViewMode;
  title?: string;
  description?: string;
  durationMinutes?: number;
  lessonType?: string;
  objectiveTitle?: string;
  objectiveBody?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [string];
  'save-shortcut': [];
  'ai-action': [string];
  'insert-image': [];
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);
const lastStart = ref(0);
const lastEnd = ref(0);

const history = useMarkdownEditorHistory(
  ref(props.modelValue),
  val => emit('update:modelValue', val)
);

const canUndo = history.canUndo;
const canRedo = history.canRedo;

onMounted(() => {
  history.initHistory(props.modelValue || '');
});

function recordCursor() {
  const el = textareaRef.value;
  if (!el) return;
  lastStart.value = el.selectionStart;
  lastEnd.value = el.selectionEnd;
}

function onTextareaInput(e: Event) {
  const val = (e.target as HTMLTextAreaElement).value;
  history.onInput(val);
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault();
    emit('save-shortcut');
    return;
  }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault();
    if (e.shiftKey) history.redo();
    else history.undo();
  }
}

function insertWrap(prefix: string, suffix: string, placeholder: string) {
  mutateSelection((start, end, text) => {
    const selected = text.substring(start, end) || placeholder;
    return {
      value: text.substring(0, start) + prefix + selected + suffix + text.substring(end),
      cursor: start + prefix.length + selected.length
    };
  });
}

function insertPrefix(prefix: string, placeholder: string) {
  mutateSelection((start, end, text) => {
    const selected = text.substring(start, end) || placeholder;
    const chunk = `\n${prefix}${selected}\n`;
    return {
      value: text.substring(0, start) + chunk + text.substring(end),
      cursor: start + chunk.length
    };
  });
}

function insertSnippet(snippet: string) {
  mutateSelection((start, end, text) => ({
    value: text.substring(0, start) + snippet + text.substring(end),
    cursor: start + snippet.length
  }));
}

function insertTable() {
  insertSnippet('\n| 列1 | 列2 |\n| --- | --- |\n| 内容 | 内容 |\n');
}

function insertMermaid() {
  insertSnippet('\n```mermaid\ngraph TD;\n  A[开始] --> B[结束];\n```\n');
}

function mutateSelection(
  fn: (start: number, end: number, text: string) => { value: string; cursor: number }
) {
  const el = textareaRef.value;
  const text = props.modelValue || '';
  const start = lastStart.value;
  const end = lastEnd.value;
  history.beforeMutation(text);
  const { value, cursor } = fn(start, end, text);
  emit('update:modelValue', value);
  history.pushHistory(value);
  nextTick(() => {
    if (!el) return;
    el.focus();
    el.selectionStart = el.selectionEnd = cursor;
    lastStart.value = cursor;
    lastEnd.value = cursor;
  });
}

function getSelectedText(): string {
  const el = textareaRef.value;
  const text = props.modelValue || '';
  if (!el) return '';
  const start = el.selectionStart ?? lastStart.value;
  const end = el.selectionEnd ?? lastEnd.value;
  if (start === end) return '';
  return text.substring(start, end);
}

function focusEditor() {
  textareaRef.value?.focus();
}

function insertSnippetAtCursor(snippet: string) {
  recordCursor();
  insertSnippet(snippet);
}

function rollbackLastSnippet() {
  history.undo();
}

defineExpose({
  getSelectedText,
  focusEditor,
  insertSnippetAtCursor,
  rollbackLastSnippet
});
</script>

<style scoped lang="scss">
.lesson-markdown-editor {
  display: flex;
  height: 100%;
  min-height: 420px;
  background: #f8fafc;

  &.view-split {
    .editor-pane,
    .preview-pane-wrapper {
      width: 50%;
    }
  }

  &.view-edit {
    .editor-pane {
      width: 100%;
    }
  }

  &.view-preview {
    .preview-pane-wrapper {
      width: 100%;
    }
  }
}

.editor-pane {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0;
  min-width: 0;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
  margin: 0 6px;
}

.tool-btn {
  border: none;
  background: transparent;
  border-radius: 8px;
  padding: 4px 8px;
  cursor: pointer;
  color: #475569;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;

  &:hover:not(:disabled) {
    background: #f1f5f9;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

}

.toolbar-group--ai {
  margin-left: 2px;
}

.tool-label.bold {
  font-weight: 800;
}

.tool-label.italic {
  font-style: italic;
}

.textarea-wrapper {
  flex: 1;
  min-height: 0;
}

.markdown-textarea {
  width: 100%;
  height: 100%;
  min-height: 360px;
  border: none;
  resize: none;
  padding: 16px 18px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 14px;
  line-height: 1.65;
  box-sizing: border-box;
  outline: none;
  background: #fff;
}

.preview-pane-wrapper {
  overflow: auto;
  min-width: 0;
}
</style>
