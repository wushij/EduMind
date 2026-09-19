<template>
  <div class="question-card" :class="{ 'is-editing': isEditing }">
    <!-- 顶部卡片元信息条 -->
    <div class="card-top-bar">
      <div class="left-meta">
        <span class="index-badge">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="pill-badge pill-badge--type">{{ typeLabel }}</span>
        <span class="pill-badge pill-badge--diff" :class="difficultyClass">
          {{ difficultyLabel }}
        </span>
        <span class="pill-badge pill-badge--score">{{ localQuestion.score }} 分</span>
        <span v-if="localQuestion.cognitiveLevel" class="pill-badge pill-badge--cog">
          {{ formatCognitive(localQuestion.cognitiveLevel) }}
        </span>
      </div>

      <div class="right-actions">
        <!-- 单题 AI 辅导（题库列表） -->
        <button
          v-if="allowAiTutor"
          type="button"
          class="capsule-icon-btn capsule-icon-btn--ai"
          title="打开侧边栏 AI，锚定本题进行讲解与答疑"
          @click="handleAiTutor"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 辅导</span>
        </button>

        <!-- 单题 AI 换一题（命题预览） -->
        <button
          v-if="allowRegenerate"
          type="button"
          class="capsule-icon-btn capsule-icon-btn--ai"
          :disabled="isRegenerating"
          title="使用 AI 为此知识点重新命制一道平行变式题"
          @click="$emit('regenerate', index)"
        >
          <el-icon v-if="!isRegenerating"><Refresh /></el-icon>
          <span v-if="!isRegenerating">AI 换一题</span>
          <span v-else class="text-xs">生成中...</span>
        </button>

        <button
          type="button"
          class="capsule-icon-btn"
          :title="showBody ? '收起选项与参考答案' : '展开选项与参考答案'"
          @click="showBody = !showBody"
        >
          <span>{{ showBody ? '收起题目' : '展开题目' }}</span>
        </button>

        <button
          type="button"
          class="capsule-icon-btn capsule-icon-btn--analysis"
          :title="showAnalysis ? '收起考点解析' : '展开考点解析'"
          @click="showAnalysis = !showAnalysis"
        >
          <span>{{ showAnalysis ? '收起解析' : '展开解析' }}</span>
        </button>

        <button
          v-if="allowEdit"
          type="button"
          class="capsule-icon-btn capsule-icon-btn--primary"
          @click="toggleEdit"
        >
          <span>{{ isEditing ? '完成保存' : '原位编辑' }}</span>
        </button>

        <button
          v-if="allowDelete"
          type="button"
          class="capsule-icon-btn capsule-icon-btn--danger"
          title="从题库中移除此题"
          @click="confirmDelete"
        >
          <span>删除</span>
        </button>
      </div>
    </div>

    <!-- 题干编辑或展示 -->
    <div class="stem-section">
      <MathText
        v-if="!isEditing"
        :text="localQuestion.stem"
        tag="div"
        custom-class="stem-content"
      />
      <div v-else class="edit-box">
        <label class="edit-label">题干内容：</label>
        <el-input
          v-model="localQuestion.stem"
          type="textarea"
          :rows="3"
          class="edit-textarea"
        />
      </div>
    </div>

    <!-- 选项列表 (选择题) -->
    <div v-if="displayOptions.length > 0 && (showBody || isEditing)" class="options-container">
      <div
        v-for="(opt, optIdx) in displayOptions"
        :key="opt.key"
        class="option-pill-row"
        :class="{ 'is-correct': opt.isCorrect }"
      >
        <span class="opt-key-circle">{{ opt.key }}</span>
        <MathText v-if="!isEditing" :text="opt.content" tag="span" custom-class="opt-content" />
        <el-input
          v-else
          v-model="opt.content"
          size="small"
          class="opt-edit-input"
        />
        <span v-if="opt.isCorrect" class="correct-tag">正确答案</span>
        <button
          v-if="isEditing"
          type="button"
          class="mark-correct-btn"
          @click="toggleOptionCorrect(optIdx)"
        >
          {{ opt.isCorrect ? '设为错项' : '设为对项' }}
        </button>
      </div>
    </div>

    <!-- 简答题/填空题答案 -->
    <div v-else-if="localQuestion.correctAnswer && (showBody || isEditing)" class="answer-display-box">
      <span class="ans-label">参考答案：</span>
      <span v-if="!isEditing" class="ans-text">{{ localQuestion.correctAnswer }}</span>
      <el-input
        v-else
        v-model="localQuestion.correctAnswer"
        size="small"
        class="ans-edit-input"
      />
    </div>

    <!-- 展开的解析与考点卡片 -->
    <div v-show="showAnalysis || isEditing" class="analysis-expanded-card">
      <div class="analysis-row">
        <strong class="analysis-title">
          <el-icon class="analysis-icon text-amber-500"><Opportunity /></el-icon>
          <span>权威考点解析与解题步骤：</span>
        </strong>
        <MathText
          v-if="!isEditing"
          :text="localQuestion.analysis"
          tag="p"
          custom-class="analysis-body"
        />
        <el-input
          v-else
          v-model="localQuestion.analysis"
          type="textarea"
          :rows="2"
          class="analysis-edit-textarea"
        />
      </div>

      <!-- 专属干扰项诊断剖析 (若有) -->
      <div v-if="localQuestion.distractorAnalysis || isEditing" class="distractor-row">
        <strong class="distractor-title">
          <el-icon class="distractor-icon text-indigo-500"><Warning /></el-icon>
          <span>干扰项诱惑力与学生易错点剖析：</span>
        </strong>
        <p v-if="!isEditing" class="distractor-body">{{ localQuestion.distractorAnalysis || '已根据易错概念对齐干扰逻辑' }}</p>
        <el-input
          v-else
          v-model="localQuestion.distractorAnalysis"
          type="textarea"
          :rows="2"
          class="distractor-edit-textarea"
        />
      </div>

      <div class="kp-row">
        <span class="kp-label">命中大纲知识点：</span>
        <div class="kp-tags">
          <span
            v-for="kp in localQuestion.knowledgePointNames || []"
            :key="kp"
            class="pill-kp-badge"
          >
            <el-icon class="kp-icon"><Reading /></el-icon>
            <span>{{ kp }}</span>
          </span>
          <span v-if="!localQuestion.knowledgePointNames || localQuestion.knowledgePointNames.length === 0" class="pill-kp-badge">
            <el-icon class="kp-icon"><Reading /></el-icon>
            <span>课程核心考点</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Opportunity, Reading, Warning, Refresh, ChatDotRound } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import type { Question } from '@/types/question/question';

const props = withDefaults(
  defineProps<{
    question: Question;
    index?: number;
    allowEdit?: boolean;
    allowDelete?: boolean;
    allowRegenerate?: boolean;
    allowAiTutor?: boolean;
    isRegenerating?: boolean;
    /** 初始是否展开选项/参考答案；题库列表建议 false */
    defaultBodyExpanded?: boolean;
    /** 初始是否展开解析；题库列表建议 false */
    defaultAnalysisExpanded?: boolean;
    /** 与 expandSyncKey 配合，列表页一键展开/收起题目正文 */
    bulkExpandBody?: boolean;
    /** 与 expandSyncKey 配合，列表页一键展开/收起解析 */
    bulkExpandAnalysis?: boolean;
    expandSyncKey?: number;
  }>(),
  {
    index: 0,
    allowEdit: true,
    allowDelete: true,
    allowRegenerate: false,
    allowAiTutor: false,
    isRegenerating: false,
    defaultBodyExpanded: true,
    defaultAnalysisExpanded: true,
    bulkExpandBody: false,
    bulkExpandAnalysis: false,
    expandSyncKey: undefined
  }
);

const emit = defineEmits<{
  (e: 'delete', id: number | string): void;
  (e: 'update', q: Question): void;
  (e: 'regenerate', index: number): void;
  (e: 'tutor', q: Question): void;
}>();

const isListControlled = props.expandSyncKey !== undefined;
const showBody = ref(isListControlled ? props.bulkExpandBody : props.defaultBodyExpanded);
const showAnalysis = ref(isListControlled ? props.bulkExpandAnalysis : props.defaultAnalysisExpanded);
const isEditing = ref(false);

watch(
  () => props.expandSyncKey,
  () => {
    if (props.expandSyncKey === undefined) return;
    showBody.value = props.bulkExpandBody;
    showAnalysis.value = props.bulkExpandAnalysis;
  }
);

const localQuestion = ref<Question>({ ...props.question });

watch(
  () => props.question,
  (newVal) => {
    localQuestion.value = { ...newVal };
  },
  { deep: true }
);

const typeLabel = computed(() => {
  if (localQuestion.value.typeLabel) return localQuestion.value.typeLabel;
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[localQuestion.value.type] || '题目';
});

const difficultyLabel = computed(() => {
  if (localQuestion.value.difficultyLabel) return localQuestion.value.difficultyLabel;
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  };
  return map[localQuestion.value.difficulty] || '中等';
});

const difficultyClass = computed(() => {
  if (localQuestion.value.difficulty === 'EASY') return 'diff-easy';
  if (localQuestion.value.difficulty === 'HARD') return 'diff-hard';
  return 'diff-medium';
});

const displayOptions = computed(() => {
  const options = localQuestion.value.options;
  return Array.isArray(options) ? options : [];
});

function formatCognitive(level: string): string {
  const map: Record<string, string> = {
    REMEMBER: '识记',
    UNDERSTAND: '理解',
    APPLY: '应用',
    ANALYZE: '分析',
    EVALUATE: '综合',
    CREATE: '设计'
  };
  return map[level] || level;
}

function toggleOptionCorrect(optIdx: number) {
  const opts = displayOptions.value;
  if (opts[optIdx]) {
    opts[optIdx].isCorrect = !opts[optIdx].isCorrect;
    const correctKeys = opts.filter(o => o.isCorrect).map(o => o.key);
    localQuestion.value.correctAnswer = correctKeys.join(',');
  }
}

function toggleEdit() {
  if (!isEditing.value) {
    showBody.value = true;
    showAnalysis.value = true;
  }
  if (isEditing.value) {
    emit('update', { ...localQuestion.value });
  }
  isEditing.value = !isEditing.value;
}

function handleAiTutor() {
  emit('tutor', { ...localQuestion.value });
}

async function confirmDelete() {
  const stemRaw = localQuestion.value.stem?.replace(/\s+/g, ' ').trim() || '';
  const stemPreview = stemRaw.slice(0, 60);
  const hint = stemRaw ? `「${stemPreview}${stemRaw.length > 60 ? '…' : ''}」` : '该试题';
  try {
    await ElMessageBox.confirm(
      `${hint} 将从题库中永久移除，此操作不可撤销。确定删除吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true
      }
    );
    const rawId = localQuestion.value.id;
    if (rawId === undefined || rawId === null || rawId === '' || rawId === 0) {
      ElMessage.warning('题目标识无效，无法删除');
      return;
    }
    emit('delete', rawId);
  } catch {
    // 用户取消
  }
}
</script>

<style scoped lang="scss">
.question-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 22px 26px;
  border: 1.5px solid #EBF1F7;
  box-shadow: 0 4px 20px rgba(30, 80, 160, 0.04);
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 20px;

  &:hover {
    box-shadow: 0 8px 28px rgba(30, 80, 160, 0.08);
    border-color: #DBEAFE;
  }

  &.is-editing {
    border-color: #3B82F6;
    background: #FAFCFF;
  }

  .card-top-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    flex-wrap: wrap;
    gap: 10px;

    .left-meta {
      display: flex;
      align-items: center;
      gap: 8px;

      .index-badge {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        background: #0F172A;
        color: #FFFFFF;
        font-size: 13px;
        font-weight: 800;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .pill-badge {
        padding: 3px 11px;
        border-radius: 9999px;
        font-size: 11.5px;
        font-weight: 600;

        &--type {
          background: #EFF6FF;
          color: #1677FF;
        }

        &--score {
          background: #FEF3C7;
          color: #D97706;
        }

        &--cog {
          background: #F3E8FF;
          color: #7E22CE;
        }

        &--diff {
          &.diff-easy { background: #ECFDF5; color: #059669; }
          &.diff-medium { background: #EEF2FF; color: #4F46E5; }
          &.diff-hard { background: #FEE2E2; color: #DC2626; }
        }
      }
    }

    .right-actions {
      display: flex;
      align-items: center;
      gap: 8px;

      .capsule-icon-btn {
        height: 32px;
        padding: 0 14px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #CBD5E1;
        color: #475569;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        display: inline-flex;
        align-items: center;
        gap: 4px;

        &:hover:not(:disabled) {
          border-color: #1677FF;
          color: #1677FF;
          background: #EFF6FF;
        }

        &:disabled {
          opacity: 0.6;
          cursor: wait;
        }

        &--ai {
          background: linear-gradient(135deg, #EEF2FF 0%, #F5F3FF 100%);
          border-color: #C7D2FE;
          color: #4F46E5;

          &:hover:not(:disabled) {
            border-color: #4F46E5;
            color: #4F46E5;
            background: #E0E7FF;
          }
        }

        &--primary {
          background: #EFF6FF;
          border-color: #BFDBFE;
          color: #1677FF;
        }

        &--analysis {
          background: #FFFBEB;
          border-color: #FDE68A;
          color: #B45309;

          &:hover {
            border-color: #F59E0B;
            color: #D97706;
            background: #FEF3C7;
          }
        }

        &--danger {
          &:hover {
            background: #FEF2F2;
            border-color: #FECACA;
            color: #EF4444;
          }
        }
      }
    }
  }

  .stem-section {
    margin-bottom: 16px;

    .stem-content {
      font-size: 15.5px;
      font-weight: 600;
      color: #0F172A;
      line-height: 1.65;
    }

    .edit-box {
      .edit-label {
        font-size: 12px;
        color: #64748B;
        font-weight: 600;
        margin-bottom: 4px;
        display: block;
      }
    }
  }

  .options-container {
    display: flex;
    flex-direction: column;
    gap: 9px;
    margin-bottom: 16px;

    .option-pill-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 18px;
      border-radius: 9999px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      font-size: 13.5px;
      color: #334155;
      transition: all 0.2s;

      .opt-key-circle {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #E2E8F0;
        color: #475569;
        font-weight: 700;
        font-size: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .opt-content {
        flex: 1;
        line-height: 1.5;
      }

      .opt-edit-input {
        flex: 1;
      }

      .correct-tag {
        font-size: 11px;
        font-weight: 700;
        color: #15803D;
        background: #DCFCE7;
        padding: 2px 9px;
        border-radius: 9999px;
        flex-shrink: 0;
      }

      .mark-correct-btn {
        font-size: 11px;
        color: #4F46E5;
        background: #FFFFFF;
        border: 1px solid #CBD5E1;
        padding: 2px 8px;
        border-radius: 9999px;
        cursor: pointer;
      }

      &.is-correct {
        background: #F0FDF4;
        border-color: #86EFAC;
        color: #166534;

        .opt-key-circle {
          background: #22C55E;
          color: #FFFFFF;
        }
      }
    }
  }

  .answer-display-box {
    padding: 12px 18px;
    border-radius: 12px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    margin-bottom: 16px;
    font-size: 13.5px;
    display: flex;
    align-items: center;
    gap: 8px;

    .ans-label {
      font-weight: 700;
      color: #0F172A;
    }

    .ans-text {
      color: #1677FF;
      font-weight: 700;
    }

    .ans-edit-input {
      max-width: 300px;
    }
  }

  .analysis-expanded-card {
    background: #F8FAFC;
    border-radius: 16px;
    padding: 18px 20px;
    border: 1px solid #E2E8F0;
    margin-top: 14px;

    .analysis-row {
      margin-bottom: 12px;

      .analysis-title {
        font-size: 13px;
        color: #0F172A;
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 6px;
      }

      .analysis-body {
        margin: 0;
        font-size: 13px;
        color: #475569;
        line-height: 1.6;
      }
    }

    .distractor-row {
      margin-bottom: 12px;
      padding: 10px 14px;
      background: #FFFFFF;
      border-radius: 10px;
      border: 1px solid #EEF2F6;

      .distractor-title {
        font-size: 12.5px;
        color: #4F46E5;
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 4px;
      }

      .distractor-body {
        margin: 0;
        font-size: 12.5px;
        color: #64748B;
        line-height: 1.5;
      }
    }

    .kp-row {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .kp-label {
        font-size: 12px;
        color: #64748B;
      }

      .kp-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .pill-kp-badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          color: #1D4ED8;
          font-size: 11.5px;
          font-weight: 500;

          .kp-icon {
            font-size: 12px;
          }
        }
      }
    }
  }
}
</style>
