<template>
  <el-dialog
    v-model="visible"
    title="管理教学目标"
    width="680px"
    destroy-on-close
    class="course-objective-editor-dialog"
  >
    <AiCognitiveThinkingPanel
      v-if="aiLoading"
      :active="aiLoading"
      v-bind="AI_COGNITIVE_THINKING_PRESETS.courseObjectives"
      show-footer-actions
      abort-label="中止生成"
      @abort="abortAiSuggest"
    />

    <div v-else class="dialog-body-wrap">
      <div class="dialog-toolbar">
        <p class="hint">最多 6 条，按顺序展示在课程概览页。</p>
        <div class="toolbar-actions">
          <span v-if="aiSourceLabel" class="ai-source-badge">{{ aiSourceLabel }}</span>
          <button
            v-if="courseId"
            type="button"
            class="ai-pill-btn"
            @click="handleAiSuggest"
          >
            <el-icon><MagicStick /></el-icon>
            AI 根据课程生成
          </button>
        </div>
      </div>

      <div v-if="rows.length === 0" class="empty-editor">
        <p>暂无目标，可手动添加或由 AI 根据章节与知识点生成。</p>
      </div>

      <div v-else class="objective-editor-list">
        <div v-for="(row, index) in rows" :key="row._key" class="objective-card">
          <div class="objective-card__head">
            <div class="head-left">
              <span class="row-index">{{ index + 1 }}</span>
              <span class="head-title">教学目标 {{ index + 1 }}</span>
            </div>
            <button type="button" class="remove-btn" @click="removeRow(index)">删除</button>
          </div>
          <div class="objective-card__fields">
            <label class="field-label">标题</label>
            <el-input
              v-model="row.title"
              placeholder="简短概括本条能力目标"
              maxlength="80"
              show-word-limit
              class="field-input"
            />
            <label class="field-label">说明（可选）</label>
            <el-input
              v-model="row.description"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 8 }"
              placeholder="可评价的学习成果描述，如「能够…」「掌握…」"
              maxlength="500"
              show-word-limit
              class="field-textarea"
            />
          </div>
        </div>
      </div>

      <button
        v-if="rows.length < 6"
        type="button"
        class="add-row-btn"
        @click="addRow"
      >
        添加目标
      </button>
    </div>
    <template #footer>
      <template v-if="!aiLoading">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { MagicStick } from '@element-plus/icons-vue';
import type { CourseObjectiveVO } from '@/types/course/overview';
import { suggestCourseObjectives } from '@/api/ai/course-objectives';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { isAxiosError } from 'axios';

type EditorRow = { _key: string; title: string; description?: string };

const props = defineProps<{
  modelValue: boolean;
  courseId?: number;
  objectives: CourseObjectiveVO[];
  saving?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [boolean];
  save: [objectives: Array<{ title: string; description?: string }>];
}>();

const visible = ref(props.modelValue);
const rows = ref<EditorRow[]>([]);
const aiLoading = ref(false);
const aiSourceLabel = ref('');
let aiAbortController: AbortController | null = null;

let keySeq = 0;
function nextKey() {
  keySeq += 1;
  return `obj-${keySeq}`;
}

function mapToRows(items: Array<{ title: string; description?: string }>): EditorRow[] {
  return items.map(o => ({
    _key: nextKey(),
    title: o.title,
    description: o.description
  }));
}

watch(() => props.modelValue, (v) => {
  visible.value = v;
  if (v) {
    keySeq = 0;
    rows.value = props.objectives.length
      ? mapToRows(props.objectives)
      : [];
    aiSourceLabel.value = '';
  } else {
    abortAiSuggest();
  }
});

watch(visible, (v) => emit('update:modelValue', v));

function addRow() {
  if (rows.value.length >= 6) return;
  rows.value.push({ _key: nextKey(), title: '', description: '' });
}

async function removeRow(index: number) {
  const row = rows.value[index];
  const label = row?.title?.trim() || `第 ${index + 1} 条目标`;
  try {
    await ElMessageBox.confirm(`确定删除「${label}」吗？删除后需点击保存才会生效。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
  } catch {
    return;
  }
  rows.value.splice(index, 1);
}

function abortAiSuggest() {
  if (aiAbortController) {
    aiAbortController.abort();
    aiAbortController = null;
  }
  aiLoading.value = false;
}

async function handleAiSuggest() {
  if (!props.courseId) {
    ElMessage.warning('课程信息未加载');
    return;
  }
  abortAiSuggest();
  aiAbortController = new AbortController();
  aiLoading.value = true;
  try {
    const res = await suggestCourseObjectives(props.courseId, 4, {
      signal: aiAbortController.signal
    });
    const payload = res?.data;
    const suggested = payload?.objectives ?? [];
    if (!suggested.length) {
      ElMessage.warning('未生成有效目标，请稍后重试');
      return;
    }
    rows.value = mapToRows(suggested);
    aiSourceLabel.value = payload?.sourceLabel || (payload?.aiGenerated ? '模型推演已生成' : '上下文智能兜底');
    ElMessage.success(
      payload?.aiGenerated
        ? 'AI 模型已生成教学目标，请确认后保存'
        : '模型暂不可用，已使用课程上下文兜底生成，请确认后保存'
    );
  } catch (err: unknown) {
    if (isAxiosError(err) && err.code === 'ERR_CANCELED') {
      return;
    }
    const msg =
      (err as { message?: string })?.message ||
      (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
    if (msg) {
      ElMessage.error(msg);
    }
  } finally {
    aiAbortController = null;
    aiLoading.value = false;
  }
}

async function submit() {
  const payload = rows.value
    .map(r => ({ title: r.title.trim(), description: r.description?.trim() }))
    .filter(r => r.title);
  if (payload.length === 0) {
    try {
      await ElMessageBox.confirm('未保留任何目标，保存后将清空概览中的教学目标。', '确认清空', {
        type: 'warning',
        confirmButtonText: '清空并保存',
        cancelButtonText: '继续编辑'
      });
    } catch {
      return;
    }
  }
  emit('save', payload);
}
</script>

<style scoped lang="scss">
.dialog-body-wrap {
  min-height: 120px;
}

.dialog-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 16px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.hint {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.ai-source-badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 9999px;
  background: #f3e8ff;
  color: #7e22ce;
  font-weight: 600;
  white-space: nowrap;
}

.ai-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border-radius: 9999px;
  border: 1px solid #d8b4fe;
  background: #faf5ff;
  color: #7c3aed;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.empty-editor {
  padding: 16px 18px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  font-size: 13px;
  color: #64748b;
  p { margin: 0; }
}

.objective-editor-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: min(58vh, 520px);
  overflow-y: auto;
  padding-right: 4px;
}

.objective-card {
  border: 1px solid #e8eef5;
  border-radius: 14px;
  background: #fafbfc;
  padding: 14px 16px 16px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;

    .head-left {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }

    .head-title {
      font-size: 14px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  &__fields {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
}

.row-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-top: 4px;
}

.remove-btn {
  flex-shrink: 0;
  height: 30px;
  padding: 0 14px;
  border-radius: 9999px;
  border: 1px solid #fecaca;
  background: #fff;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    background: #fef2f2;
    border-color: #f87171;
  }
}

.objective-card__fields {
  :deep(.field-input),
  :deep(.field-textarea) {
    width: 100%;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 10px;
    box-shadow: 0 0 0 1px #e2e8f0 inset;
    background: #fff;
  }

  :deep(.el-textarea__inner) {
    line-height: 1.65;
    padding: 10px 12px 32px;
    font-size: 13px;
    resize: vertical;
    min-height: 96px !important;
  }

  :deep(.el-input .el-input__count) {
    background: rgba(255, 255, 255, 0.95);
    padding-left: 6px;
    bottom: 6px;
    right: 10px;
    line-height: 1;
  }

  :deep(.el-textarea .el-input__count) {
    background: rgba(255, 255, 255, 0.95);
    padding: 2px 6px;
    bottom: 8px;
    right: 10px;
    line-height: 1.2;
    pointer-events: none;
  }
}

.add-row-btn {
  margin-top: 14px;
  width: 100%;
  height: 38px;
  border-radius: 9999px;
  border: 1px dashed #93c5fd;
  background: #f8fbff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    background: #eff6ff;
  }
}
</style>
