<template>
  <div class="question-option-editor">
    <!-- 单选 / 多选题选项编辑 -->
    <div v-if="type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE'" class="options-container">
      <div
        v-for="(opt, index) in options"
        :key="opt.key"
        class="option-item-row"
        :class="{ 'is-correct': isOptionCorrect(opt.key) }"
      >
        <div class="opt-prefix">
          <!-- 单选题单选框 -->
          <el-radio
            v-if="type === 'SINGLE_CHOICE'"
            :model-value="singleAnswer"
            :label="opt.key"
            class="answer-radio"
            @change="handleSingleSelect(opt.key)"
          >
            <span class="opt-key-badge">{{ opt.key }}</span>
          </el-radio>

          <!-- 多选题复选框 -->
          <el-checkbox
            v-else
            :model-value="isMultipleChecked(opt.key)"
            class="answer-checkbox"
            @change="(val: boolean) => handleMultipleToggle(opt.key, val)"
          >
            <span class="opt-key-badge">{{ opt.key }}</span>
          </el-checkbox>
        </div>

        <div class="opt-input-wrapper">
          <el-input
            v-model="opt.content"
            type="textarea"
            :rows="2"
            :placeholder="`请输入选项 ${opt.key} 的具体内容，支持公式或代码`"
            class="option-content-input"
            @input="emitOptions"
          />
        </div>

        <div class="opt-actions">
          <el-button
            type="danger"
            link
            :disabled="options.length <= 2"
            @click="removeOption(index)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <div class="add-option-dock">
        <el-button
          type="primary"
          plain
          size="small"
          :disabled="options.length >= 8"
          @click="addOption"
        >
          <el-icon><Plus /></el-icon>
          添加新选项（{{ options.length }}/8）
        </el-button>
        <span class="helper-text">请在左侧勾选设定正确选项</span>
      </div>
    </div>

    <!-- 判断题编辑 -->
    <div v-else-if="type === 'TRUE_FALSE'" class="judgment-container">
      <el-radio-group :model-value="singleAnswer" class="judgment-group" @change="handleSingleSelect">
        <el-radio-button label="T">
          <span class="judgment-opt">
            <el-icon class="mr-1 text-success"><Check /></el-icon> 正确 (True)
          </span>
        </el-radio-button>
        <el-radio-button label="F">
          <span class="judgment-opt">
            <el-icon class="mr-1 text-danger"><Close /></el-icon> 错误 (False)
          </span>
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 填空题答案编辑 -->
    <div v-else-if="type === 'FILL_BLANK'" class="blank-container">
      <el-input
        :model-value="singleAnswer"
        placeholder="请输入填空题标准参考答案，多个空可用逗号或分号隔开"
        clearable
        @update:model-value="handleTextAnswerChange"
      />
    </div>

    <!-- 简答题/主观题参考答案 -->
    <div v-else-if="type === 'SHORT_ANSWER' || type === 'ESSAY'" class="essay-container">
      <el-input
        :model-value="singleAnswer"
        type="textarea"
        :rows="4"
        placeholder="请输入参考答案、核心解题推导步骤与采分点关键词"
        @update:model-value="handleTextAnswerChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { Plus, Delete, Check, Close } from '@element-plus/icons-vue';
import type { QuestionType, QuestionOption } from '@/types/question/question';

const props = defineProps<{
  type: QuestionType;
  modelValue?: QuestionOption[];
  correctAnswer?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: QuestionOption[]): void;
  (e: 'update:correctAnswer', val: string): void;
}>();

const options = ref<QuestionOption[]>([]);
const singleAnswer = computed(() => props.correctAnswer || '');

const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal && newVal.length > 0) {
      options.value = JSON.parse(JSON.stringify(newVal));
    } else if (props.type === 'SINGLE_CHOICE' || props.type === 'MULTIPLE_CHOICE') {
      initDefaultOptions();
    }
  },
  { immediate: true, deep: true }
);

function initDefaultOptions() {
  options.value = [
    { key: 'A', content: '', isCorrect: false },
    { key: 'B', content: '', isCorrect: false },
    { key: 'C', content: '', isCorrect: false },
    { key: 'D', content: '', isCorrect: false }
  ];
  emitOptions();
}

function emitOptions() {
  emit('update:modelValue', options.value);
}

function handleSingleSelect(key: string) {
  emit('update:correctAnswer', key);
  options.value.forEach(o => o.isCorrect = (o.key === key));
  emitOptions();
}

function isMultipleChecked(key: string): boolean {
  return (props.correctAnswer || '').includes(key);
}

function handleMultipleToggle(key: string, checked: boolean) {
  const current = (props.correctAnswer || '').split('').filter(Boolean);
  let updated: string[];
  if (checked) {
    updated = Array.from(new Set([...current, key])).sort();
  } else {
    updated = current.filter(k => k !== key);
  }
  const answerStr = updated.join('');
  emit('update:correctAnswer', answerStr);
  options.value.forEach(o => o.isCorrect = answerStr.includes(o.key));
  emitOptions();
}

function isOptionCorrect(key: string): boolean {
  if (props.type === 'SINGLE_CHOICE') {
    return singleAnswer.value === key;
  }
  if (props.type === 'MULTIPLE_CHOICE') {
    return (props.correctAnswer || '').includes(key);
  }
  return false;
}

function addOption() {
  if (options.value.length >= OPTION_KEYS.length) return;
  const nextKey = OPTION_KEYS[options.value.length];
  options.value.push({ key: nextKey, content: '', isCorrect: false });
  emitOptions();
}

function removeOption(idx: number) {
  if (options.value.length <= 2) return;
  options.value.splice(idx, 1);
  // 重置 key
  options.value.forEach((opt, i) => {
    opt.key = OPTION_KEYS[i];
  });
  emitOptions();
}

function handleTextAnswerChange(val: string) {
  emit('update:correctAnswer', val);
}
</script>

<style scoped lang="scss">
.question-option-editor {
  width: 100%;

  .options-container {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .option-item-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px 14px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    transition: all 0.2s ease;

    &.is-correct {
      background: #eff6ff;
      border-color: #3b82f6;
    }

    .opt-prefix {
      display: flex;
      align-items: center;
      padding-top: 4px;
    }

    .opt-key-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 26px;
      height: 26px;
      font-weight: 600;
      color: #1e293b;
      background: #ffffff;
      border: 1px solid #cbd5e1;
      border-radius: 50%;
      margin-left: 4px;
    }

    .opt-input-wrapper {
      flex: 1;
    }

    .opt-actions {
      padding-top: 4px;
    }
  }

  .add-option-dock {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 6px;

    .helper-text {
      font-size: 13px;
      color: #64748b;
    }
  }

  .judgment-container {
    .judgment-group {
      display: flex;
      gap: 16px;
    }
    .judgment-opt {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 16px;
      font-weight: 600;
    }
  }

  .blank-container,
  .essay-container {
    padding: 6px 0;
  }
}
</style>
