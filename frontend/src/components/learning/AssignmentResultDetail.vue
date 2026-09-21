<template>
  <div class="result-detail">
    <div v-if="items.length === 0" class="detail-empty">
      本次提交暂无逐题评阅明细，评阅完成后可在此查看每题得分与评语。
    </div>

    <template v-else>
      <!-- 工具条：题量概览 + 批量展开（与错题本保持一致的交互） -->
      <div class="detail-toolbar">
        <span class="toolbar-count">
          共 <strong>{{ items.length }}</strong> 道题 · 第 {{ page }} / {{ totalPages }} 页
        </span>
        <div class="toolbar-actions">
          <button type="button" class="capsule-icon-btn" @click="toggleAllBody">
            <span>{{ listExpandAllBody ? '收起题目' : '展开题目' }}</span>
          </button>
          <button
            type="button"
            class="capsule-icon-btn capsule-icon-btn--analysis"
            @click="toggleAllDetails"
          >
            <span>{{ listExpandAllFull ? '收起题目和解析' : '展开题目和解析' }}</span>
          </button>
        </div>
      </div>

      <div v-for="(item, idx) in pagedItems" :key="item.questionId" class="detail-card">
        <div class="card-top-bar">
          <span class="index-badge">{{ String(pageStart + idx + 1).padStart(2, '0') }}</span>
          <span class="pill pill--type">{{ typeLabel(item.type) }}</span>
          <span class="pill pill--score">得分 {{ formatScore(item) }}</span>
          <span
            v-if="resultState(item)"
            class="pill"
            :class="resultState(item)!.className"
          >
            {{ resultState(item)!.label }}
          </span>

          <div class="card-top-actions">
            <button
              type="button"
              class="capsule-icon-btn"
              :title="isBodyOpen(item.questionId) ? '收起我的作答与参考答案' : '展开我的作答与参考答案'"
              @click="toggleItemBody(item.questionId)"
            >
              <span>{{ isBodyOpen(item.questionId) ? '收起题目' : '展开题目' }}</span>
            </button>
            <button
              type="button"
              class="capsule-icon-btn capsule-icon-btn--analysis"
              :title="isAnalysisOpen(item.questionId) ? '收起解析与评语' : '展开解析与评语'"
              @click="toggleItemAnalysis(item.questionId)"
            >
              <span>{{ isAnalysisOpen(item.questionId) ? '收起解析' : '展开解析' }}</span>
            </button>
          </div>
        </div>

        <!-- 题干始终展示 -->
        <div class="answer-block">
          <div class="block-title">题干</div>
          <div class="block-body"><MathText :text="item.stem" /></div>
        </div>

        <!-- 展开题目：我的作答 + 参考答案 -->
        <template v-if="isBodyOpen(item.questionId)">
          <div class="answer-block answer-block--mine">
            <div class="block-title">我的作答</div>
            <!-- 未作答（无作答记录或作答为空白）时明确写出"未作答"，并用弱化样式区别于真实作答内容 -->
            <div class="block-body">
              <MathText v-if="hasTextAnswer(item.questionId)" :text="myAnswer(item.questionId)" />
              <span v-else class="muted">未作答</span>
            </div>
          </div>

          <div v-if="item.standardAnswer" class="answer-block answer-block--standard">
            <div class="block-title">参考答案</div>
            <div class="block-body"><MathText :text="item.standardAnswer" /></div>
          </div>
        </template>

        <!-- 展开解析：解析 + AI 评语 + 教师评语 -->
        <template v-if="isAnalysisOpen(item.questionId)">
          <div v-if="item.analysis" class="answer-block">
            <div class="block-title">解析</div>
            <div class="block-body"><MathText :text="item.analysis" /></div>
          </div>

          <div v-if="item.aiComment" class="answer-block answer-block--ai">
            <div class="block-title">
              <el-icon><MagicStick /></el-icon>
              <span>AI 评阅说明</span>
            </div>
            <!-- AI 评语为大模型输出的 Markdown（含 ** 强调、公式、表格），需走 Markdown 渲染而非纯文本 -->
            <div class="block-body markdown-body"><MarkdownLlmOutput :content="item.aiComment" /></div>
          </div>

          <div v-if="item.teacherComment" class="answer-block answer-block--teacher">
            <div class="block-title">
              <el-icon><EditPen /></el-icon>
              <span>教师评语</span>
            </div>
            <div class="block-body"><MathText :text="item.teacherComment" /></div>
          </div>
        </template>
      </div>

      <!-- 统一分页：与错题本 / 答卷列表共用 AppPagination（默认 10 题/页） -->
      <div class="pagination-footer-bar">
        <AppPagination
          v-model:page-num="page"
          v-model:page-size="pageSize"
          :total="items.length"
          :page-sizes="[10, 20, 50]"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { MagicStick, EditPen } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import MarkdownLlmOutput from '@/components/system/PromptLlmOutput.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { getSubmissionTypeLabel } from '@/composables/question/useSubmission';
import type { GradingItem, SubmissionAnswerItem } from '@/types/question/submission';

const props = withDefaults(
  defineProps<{
    gradingItems?: GradingItem[];
    answers?: SubmissionAnswerItem[];
  }>(),
  {
    gradingItems: () => [],
    answers: () => []
  }
);

const items = computed(() => props.gradingItems || []);

/* ---------------- 分页：默认 10 题/页，与错题本、答卷列表保持一致 ---------------- */
const page = ref(1);
const pageSize = ref(10);

const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / pageSize.value)));
const pageStart = computed(() => (page.value - 1) * pageSize.value);
const pagedItems = computed(() =>
  items.value.slice(pageStart.value, pageStart.value + pageSize.value)
);

/* ---------------- 展开折叠：单题开关 + 批量联动，交互对齐错题本 ---------------- */
const bodyOpenIds = ref<Set<number>>(new Set());
const analysisOpenIds = ref<Set<number>>(new Set());

const allQuestionIds = computed(() => items.value.map((item) => item.questionId));

const listExpandAllBody = computed(
  () => allQuestionIds.value.length > 0 && allQuestionIds.value.every((id) => bodyOpenIds.value.has(id))
);
const listExpandAllFull = computed(
  () => listExpandAllBody.value && allQuestionIds.value.every((id) => analysisOpenIds.value.has(id))
);

function isBodyOpen(questionId?: number) {
  return questionId != null && bodyOpenIds.value.has(questionId);
}

function isAnalysisOpen(questionId?: number) {
  return questionId != null && analysisOpenIds.value.has(questionId);
}

function toggleItemBody(questionId?: number) {
  if (questionId == null) {
    return;
  }
  const next = new Set(bodyOpenIds.value);
  if (next.has(questionId)) {
    next.delete(questionId);
  } else {
    next.add(questionId);
  }
  bodyOpenIds.value = next;
}

function toggleItemAnalysis(questionId?: number) {
  if (questionId == null) {
    return;
  }
  const next = new Set(analysisOpenIds.value);
  if (next.has(questionId)) {
    next.delete(questionId);
  } else {
    next.add(questionId);
  }
  analysisOpenIds.value = next;
}

function toggleAllBody() {
  bodyOpenIds.value = listExpandAllBody.value ? new Set() : new Set(allQuestionIds.value);
}

function toggleAllDetails() {
  const ids = listExpandAllFull.value ? [] : allQuestionIds.value;
  bodyOpenIds.value = new Set(ids);
  analysisOpenIds.value = new Set(ids);
}

/** 我的作答按 questionId 建索引，便于与逐题评阅结果对齐展示 */
const answerMap = computed(() => {
  const map = new Map<number, string>();
  (props.answers || []).forEach(answer => {
    if (answer?.questionId != null) {
      map.set(answer.questionId, answer.answer ?? '');
    }
  });
  return map;
});

function myAnswer(questionId?: number): string {
  return questionId != null ? (answerMap.value.get(questionId) ?? '') : '';
}

/**
 * 是否存在真实作答内容。
 * 「无作答记录」与「有记录但内容为空白」都视为未作答——
 * 学生端需要明确显示"未作答"字样，而不是留出一个空白的作答框。
 */
function hasTextAnswer(questionId?: number): boolean {
  return myAnswer(questionId).trim() !== '';
}

function formatScore(item: GradingItem) {
  const score = item.score != null ? item.score : '—';
  const maxScore = item.maxScore != null ? item.maxScore : '—';
  return `${score} / ${maxScore}`;
}

interface ResultStateMeta {
  label: string;
  className: string;
}

/**
 * 展示态判定：是否作答 > 对错 > 待人工评阅。
 * 未作答的题目即使后端历史数据中 isCorrect 为 0，也应显示"未作答 · 不得分"，而不是"回答错误"。
 */
function resultState(item: GradingItem): ResultStateMeta | null {
  if (!hasTextAnswer(item.questionId)) {
    return { label: '未作答 · 不得分', className: 'pill--unanswered' };
  }
  if (item.isCorrect === true) {
    return { label: '回答正确', className: 'pill--ok' };
  }
  if (item.isCorrect === false) {
    return { label: '回答错误', className: 'pill--bad' };
  }
  if (item.status === 'PENDING_MANUAL') {
    return { label: '待人工评阅', className: 'pill--pending' };
  }
  return null;
}

function typeLabel(type?: string) {
  return type ? getSubmissionTypeLabel(type) : '试题';
}
</script>

<style scoped lang="scss">
.result-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 20px;
}

.detail-empty {
  padding: 28px;
  text-align: center;
  color: #64748b;
  font-size: 13px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 22px;
}

.detail-card {
  background: #ffffff;
  border: 1px solid #e8eef7;
  border-radius: 22px;
  padding: 18px 20px;
  box-shadow: 0 6px 22px rgba(30, 80, 150, 0.05);
}

.card-top-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
}

.card-top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* ---------------- 工具条：题量概览 + 批量展开 ---------------- */
.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 18px;
  background: #f8fafc;
  border: 1px solid #e8eef7;
  border-radius: 22px;

  .toolbar-count {
    font-size: 13px;
    color: #475569;

    strong {
      color: #1677ff;
      font-weight: 700;
    }
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.capsule-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  padding: 0 16px;
  border-radius: 9999px;
  border: 1px solid #e8eef7;
  background: #fff;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: #bfdbfe;
    color: #1677ff;
    background: #f5faff;
  }

  &:focus-visible {
    box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.18);
  }

  &--analysis {
    border-color: #ddd6fe;
    background: #f8f5ff;
    color: #7c3aed;

    &:hover {
      border-color: #c4b5fd;
      background: #f1ecff;
      color: #6d28d9;
    }
  }
}

/* ---------------- 分页栏：与其他列表模块一致的白底圆角横条 ---------------- */
.pagination-footer-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #e8eef7;
  border-radius: 22px;
  box-shadow: 0 6px 20px rgba(30, 80, 150, 0.04);

  :deep(.pagination-bar) {
    margin-top: 0;
    padding: 0;
    border-top: none;
    justify-content: flex-start !important;
    width: 100%;
  }
}

.index-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 24px;
  padding: 0 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #1e40af;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
}

.pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;

  &--type {
    background: #f1f5f9;
    color: #475569;
    border: 1px solid #e2e8f0;
  }

  &--score {
    background: #eff6ff;
    color: #2563eb;
    border: 1px solid #bfdbfe;
  }

  &--ok {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #a7f3d0;
  }

  &--bad {
    background: #fef2f2;
    color: #b91c1c;
    border: 1px solid #fecaca;
  }

  &--unanswered {
    background: #f8fafc;
    color: #64748b;
    border: 1px solid #cbd5e1;
  }

  &--pending {
    background: #fffbeb;
    color: #b45309;
    border: 1px solid #fde68a;
  }
}

.answer-block {
  margin-top: 14px;

  .block-title {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12.5px;
    font-weight: 700;
    color: #334155;
    margin-bottom: 6px;
  }

  .block-body {
    padding: 10px 12px;
    border-radius: 10px;
    background: #f8fafc;
    border: 1px solid #eef2f7;
    font-size: 13.5px;
    color: #1e293b;
    line-height: 1.7;
  }

  &--mine .block-body {
    background: #eff6ff;
    border-color: #dbeafe;
  }

  &--standard .block-body {
    background: #ecfdf5;
    border-color: #d1fae5;
  }

  &--ai .block-title {
    color: #6d28d9;
  }

  &--ai .block-body {
    background: #f5f3ff;
    border-color: #ede9fe;
  }

  &--teacher .block-title {
    color: #0369a1;
  }

  &--teacher .block-body {
    background: #f0f9ff;
    border-color: #e0f2fe;
  }
}

.muted {
  color: #94a3b8;
}
</style>
