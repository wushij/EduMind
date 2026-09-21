<template>
  <article class="wrong-card">
    <header class="card-top-bar">
      <div class="badge-group">
        <span class="index-badge">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="pill pill--danger">累计错误 {{ item.wrongCount }} 次</span>
        <span v-if="item.knowledgePointName" class="pill pill--kp">{{ item.knowledgePointName }}</span>
        <span v-if="item.type" class="pill pill--type">{{ formatQuestionType(item.type) }}</span>
        <span v-if="item.difficulty" class="pill" :class="difficultyPillClass">{{ difficultyLabel }}</span>
      </div>

      <div class="right-actions">
        <span class="item-time">{{ item.createTime || '近期学习' }}</span>
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
          :title="showAnalysis ? '收起认知归因解析' : '展开认知归因解析'"
          @click="showAnalysis = !showAnalysis"
        >
          <span>{{ showAnalysis ? '收起解析' : '展开解析' }}</span>
        </button>
      </div>
    </header>

    <div class="stem-container">
      <MathText :text="item.stem || '暂无题目内容'" />
    </div>

    <!-- 展开题目：选择题渲染选项，其他题型直接对照作答与参考答案 -->
    <div v-if="showBody" class="body-section">
      <div v-if="options.length > 0" class="options-container">
        <div
          v-for="opt in options"
          :key="opt.key"
          class="option-row"
          :class="{
            'option-row--answer': opt.key === item.answer,
            'option-row--wrong': opt.key === item.studentAnswer && opt.key !== item.answer
          }"
        >
          <span class="option-key">{{ opt.key }}</span>
          <MathText class="option-val" :text="opt.content" />
          <span v-if="opt.key === item.answer" class="option-label option-label--answer">正确答案</span>
          <span
            v-else-if="opt.key === item.studentAnswer"
            class="option-label option-label--wrong"
          >
            历史作答
          </span>
        </div>
      </div>

      <div v-else class="answers-compare-row">
        <div class="ans-box ans-box--wrong">
          <span class="ans-title">历史错误提交</span>
          <span class="ans-text">{{ item.studentAnswer || '作答不完整或步骤中断' }}</span>
        </div>
        <div class="ans-box ans-box--right">
          <span class="ans-title">标准参考答案</span>
          <span class="ans-text">{{ item.answer || '详见完整解析' }}</span>
        </div>
      </div>
    </div>

    <!-- 展开解析：AI 认知归因结论与失分主因 -->
    <div v-if="showAnalysis" class="ai-diagnosis-banner">
      <div class="ai-header">
        <span class="ai-tag">
          <el-icon><Cpu /></el-icon>
          <span>AI 认知归因诊断</span>
        </span>
        <div v-if="errorTags.length || sourceTag" class="error-types-tags">
          <span v-if="sourceTag" class="pill" :class="sourceTagClass">{{ sourceTag }}</span>
          <span v-for="err in errorTags" :key="err" class="pill pill--warn">{{ err }}</span>
        </div>
      </div>
      <MathText
        class="diagnosis-text"
        :class="{ 'diagnosis-text--muted': !hasDiagnosis }"
        :text="cleanedDiagnosis || '尚未生成归因结论，点击下方「AI 深度归因与前驱知识」，在面板内触发大模型归因诊断（可随时中止）。'"
      />
    </div>

    <footer class="card-footer-bar">
      <el-button size="small" type="primary" text @click="emit('open-diagnosis', item)">
        <el-icon><Cpu /></el-icon>
        <span>AI 深度归因与前驱知识</span>
      </el-button>
      <div class="footer-right">
        <el-button size="small" round plain type="success" @click="emit('mark-mastered', item)">
          <el-icon><Check /></el-icon>
          <span>标为已攻克</span>
        </el-button>
        <el-button size="small" round type="primary" @click="emit('start-variant', item)">
          <el-icon><Aim /></el-icon>
          <span>练习同类变式题</span>
        </el-button>
      </div>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Cpu, Check, Aim } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import type { WrongQuestionRecordItem } from '@/types/learning/wrong-question';

const props = withDefaults(
  defineProps<{
    item: WrongQuestionRecordItem;
    index?: number;
    formatQuestionType: (type?: string) => string;
    getDifficultyType: (diff?: string) => 'success' | 'warning' | 'danger';
    parsedOptions: (optionsJson?: string) => Array<{ key: string; content: string }>;
    /** 清洗掉英文类型标记后的诊断正文 */
    displayDiagnosis: (item: WrongQuestionRecordItem) => string;
    displayErrorTags: (item: WrongQuestionRecordItem) => string[];
    /** 初始是否展开选项与答案对照；列表页建议 false，保持紧凑 */
    defaultBodyExpanded?: boolean;
    /** 初始是否展开 AI 认知归因解析；列表页建议 false */
    defaultAnalysisExpanded?: boolean;
    /** 与 expandSyncKey 配合，列表页一键展开/收起题目 */
    bulkExpandBody?: boolean;
    /** 与 expandSyncKey 配合，列表页一键展开/收起解析 */
    bulkExpandAnalysis?: boolean;
    expandSyncKey?: number;
  }>(),
  {
    index: 0,
    defaultBodyExpanded: false,
    defaultAnalysisExpanded: false,
    bulkExpandBody: false,
    bulkExpandAnalysis: false,
    expandSyncKey: undefined
  }
);

const emit = defineEmits<{
  'open-diagnosis': [item: WrongQuestionRecordItem];
  'mark-mastered': [item: WrongQuestionRecordItem];
  'start-variant': [item: WrongQuestionRecordItem];
}>();

const isListControlled = props.expandSyncKey !== undefined;
const showBody = ref(isListControlled ? props.bulkExpandBody : props.defaultBodyExpanded);
const showAnalysis = ref(isListControlled ? props.bulkExpandAnalysis : props.defaultAnalysisExpanded);

watch(
  () => props.expandSyncKey,
  () => {
    if (props.expandSyncKey === undefined) {
      return;
    }
    showBody.value = props.bulkExpandBody;
    showAnalysis.value = props.bulkExpandAnalysis;
  }
);

const errorTags = computed(() => props.displayErrorTags(props.item));

const options = computed(() => props.parsedOptions(props.item.options));

/** 展示用诊断正文：剥离 “类型：CONCEPT” 这类仅用于提取 error_types 的机器标记 */
const cleanedDiagnosis = computed(() => props.displayDiagnosis(props.item));

const hasDiagnosis = computed(() => cleanedDiagnosis.value.length >= 10);

const difficultyLabel = computed(() => {
  const level = props.item.difficulty;
  if (level === 'EASY') return '容易';
  if (level === 'HARD') return '较难';
  return '中等';
});

const difficultyPillClass = computed(() => {
  const type = props.getDifficultyType(props.item.difficulty);
  if (type === 'danger') return 'pill--danger';
  if (type === 'success') return 'pill--ok';
  return 'pill--warn';
});

/**
 * 归因来源标记：仅「演示数据 / 未作答」需要显式提示。
 * 演示种子里的结论形如 "CALC: 等价无穷小代换条件应用错误"，并非大模型产出，
 * 不加标记会被误当成 AI 诊断结论。
 */
const sourceTag = computed(() => {
  if (props.item.diagnosisSource === 'LEGACY') return '演示数据 · 非 AI';
  if (props.item.diagnosisSource === 'UNANSWERED') return '未作答 · 未归因';
  return '';
});

const sourceTagClass = computed(() =>
  props.item.diagnosisSource === 'LEGACY' ? 'pill--source-legacy' : 'pill--source-muted'
);
</script>

<style scoped lang="scss">
.wrong-card {
  background: #fff;
  border: 1px solid #e8eef7;
  border-radius: 22px;
  padding: 20px 22px;
  box-shadow: 0 6px 22px rgba(30, 80, 150, 0.05);
  /* 仅过渡颜色与阴影：不做位移，避免鼠标悬浮时卡片边框产生抖动 */
  transition: border-color 0.22s ease, box-shadow 0.22s ease;

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 12px 30px rgba(22, 119, 255, 0.09);
  }
}

/* ---------------- 顶部标签与操作 ---------------- */
.card-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;

  .badge-group {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .right-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  .item-time {
    font-size: 12px;
    color: #94a3b8;
    white-space: nowrap;
  }
}

.index-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 24px;
  padding: 0 9px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 800;
  color: #1d4ed8;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
}

.pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
  white-space: nowrap;

  &--danger {
    background: #fff1f0;
    color: #cf1322;
    border-color: #ffccc7;
  }

  &--ok {
    background: #f0fdf4;
    color: #15803d;
    border-color: #bbf7d0;
  }

  &--warn {
    background: #fffbeb;
    color: #b45309;
    border-color: #fde68a;
  }

  &--kp {
    background: #eff6ff;
    color: #1d4ed8;
    border-color: #bfdbfe;
  }

  &--type {
    background: #f8fafc;
    color: #64748b;
    border-color: #e2e8f0;
  }

  &--source-legacy {
    background: #fff7ed;
    color: #c2410c;
    border-color: #fed7aa;
  }

  &--source-muted {
    background: #f1f5f9;
    color: #64748b;
    border-color: #cbd5e1;
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

/* ---------------- 题干 ---------------- */
.stem-container {
  font-size: 14.5px;
  line-height: 1.75;
  font-weight: 500;
  color: #1e293b;
  padding: 14px 18px;
  background: #f8fafc;
  border-radius: 16px;
  border-left: 4px solid #1677ff;
  overflow-x: auto;
}

.body-section {
  margin-top: 14px;
}

/* ---------------- 选项 ---------------- */
.options-container {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 16px;
  border-radius: 14px;
  border: 1px solid #e8eef7;
  background: #fff;
  font-size: 13.5px;
  color: #334155;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cfe0f5;
    background: #fafcff;
  }

  .option-key {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    flex-shrink: 0;
    border-radius: 9999px;
    background: #eff6ff;
    border: 1px solid #dbeafe;
    color: #1d4ed8;
    font-size: 12.5px;
    font-weight: 700;
  }

  .option-val {
    flex: 1;
    min-width: 0;
    line-height: 1.6;
    word-break: break-word;
  }

  &--answer {
    background: #f6ffed;
    border-color: #b7eb8f;

    .option-key {
      background: #eaf8ee;
      border-color: #b7eb8f;
      color: #389e0d;
    }
  }

  &--wrong {
    background: #fff1f0;
    border-color: #ffa39e;

    .option-key {
      background: #fff1f0;
      border-color: #ffccc7;
      color: #cf1322;
    }
  }
}

.option-label {
  flex-shrink: 0;
  height: 22px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  font-size: 11.5px;
  font-weight: 700;

  &--answer {
    background: #eaf8ee;
    color: #52c41a;
    border: 1px solid #b7eb8f;
  }

  &--wrong {
    background: #fff1f0;
    color: #f5222d;
    border: 1px solid #ffa39e;
  }
}

/* ---------------- 无选项题型的答案对照 ---------------- */
.answers-compare-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  .ans-box {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 16px;
    border-radius: 16px;
    font-size: 13px;

    .ans-title {
      font-size: 12px;
      font-weight: 700;
      opacity: 0.85;
    }

    .ans-text {
      line-height: 1.65;
      word-break: break-word;
    }

    &--wrong {
      background: #fff5f5;
      border: 1px solid #ffccc7;
      color: #cf1322;
    }

    &--right {
      background: #f6ffed;
      border: 1px solid #d9f7be;
      color: #389e0d;
    }
  }
}

/* ---------------- AI 归因横幅 ---------------- */
.ai-diagnosis-banner {
  padding: 14px 18px;
  margin-top: 14px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f2f7ff 0%, #f6f2ff 100%);
  border: 1px solid #dfe9fb;

  .ai-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 8px;

    .ai-tag {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 700;
      color: #1677ff;
    }

    .error-types-tags {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }
  }

  .diagnosis-text {
    display: block;
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
    color: #475569;
    word-break: break-word;

    &--muted {
      color: #94a3b8;
    }
  }
}

/* ---------------- 底部操作 ---------------- */
.card-footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;

  .footer-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}
</style>
