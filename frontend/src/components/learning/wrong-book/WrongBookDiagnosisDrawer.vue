<template>
  <el-drawer
    :model-value="visible"
    title="错题深度认知归因与前驱依赖分析"
    size="580px"
    class="wrong-diagnosis-drawer"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="drawer-content">
      <template v-if="item">
        <!-- AI 调用来源与当前状态提示（明确本面板结论来自大模型） -->
        <div class="ai-source-bar">
          <span class="ai-source-pill" :class="{ 'ai-source-pill--legacy': isLegacySource }">
            <el-icon><Cpu /></el-icon>
            <span>{{ sourceChipLabel }}</span>
          </span>
          <span class="ai-source-text">{{ aiStatusText }}</span>
        </div>

        <!-- ① 错因根源诊断 -->
        <section class="drawer-section">
          <header class="section-head">
            <h4 class="section-title">
              <el-icon><Compass /></el-icon>
              <span>错因根源诊断</span>
            </h4>
            <el-button
              size="small"
              type="primary"
              plain
              round
              :loading="diagnosing"
              :disabled="isUnanswered && !hasDiagnosisText"
              @click="emit('regenerate-diagnosis')"
            >
              <el-icon v-if="!diagnosing"><Refresh /></el-icon>
              <span>{{ diagnoseBtnText }}</span>
            </el-button>
          </header>

          <!-- AI 认知归因推演中：统一罗盘雷达 + 秒级计时 + 随时中止 -->
          <AiCognitiveThinkingPanel
            v-if="diagnosing"
            :active="diagnosing"
            v-bind="AI_COGNITIVE_THINKING_PRESETS.wrongBookDiagnosis"
            show-footer-actions
            abort-label="中止 AI 诊断"
            @abort="emit('abort-diagnosis')"
          />

          <div v-else class="diagnosis-callout">
            <div v-if="sourceMeta" class="callout-head">
              <span class="source-pill" :class="sourceMeta.className">{{ sourceMeta.label }}</span>
            </div>
            <MathText class="summary-p" :class="{ 'summary-p--muted': !hasDiagnosisText }" :text="diagnosisText" />
            <p v-if="isLegacySource" class="legacy-hint">
              该结论为演示（历史预置）数据，并非大模型产出；点击右上角「重新诊断」可获取 AI 实时归因。
            </p>
            <p v-if="isUnanswered" class="unanswered-hint">
              该题本次作答为空白，AI 不进行认知归因，避免把「作答缺失」误判成审题或计算问题。
            </p>
            <div v-if="errorTags.length" class="tags-row">
              <span v-for="err in errorTags" :key="err" class="soft-tag soft-tag--danger">
                失分主因：{{ err }}
              </span>
            </div>
          </div>
        </section>

        <!-- ② 知识图谱前驱依赖溯源 -->
        <section class="drawer-section">
          <header class="section-head">
            <h4 class="section-title">
              <el-icon><Connection /></el-icon>
              <span>知识图谱前驱依赖溯源</span>
            </h4>
            <span class="section-hint">
              {{ hasPrerequisite ? `${prerequisiteCount} 个前驱考点` : '暂无前驱依赖' }}
            </span>
          </header>

          <div v-if="!prerequisiteNodes.length" class="soft-empty">
            <el-icon class="soft-empty__icon"><Connection /></el-icon>
            <p class="soft-empty__text">当前考点暂无配置前驱依赖，可在课程知识图谱中维护 prerequisite 关系</p>
          </div>
          <div v-else class="prereq-tree">
            <!-- 无前驱时后端仍会回传「当前考点」自身节点，需明确说明，避免被读成"1 个前驱" -->
            <p v-if="!hasPrerequisite" class="prereq-note">
              当前考点暂无配置的前驱依赖（可在课程知识图谱中维护 prerequisite 关系），下方仅展示该考点自身掌握度。
            </p>
            <template v-for="(node, idx) in prerequisiteNodes" :key="node.knowledgePointId">
              <div class="tree-node" :class="node.current ? 'tree-node--current' : 'tree-node--parent'">
                <span class="node-badge" :class="{ 'node-badge--current': node.current }">
                  {{ node.current ? '当前错题考点' : '前驱基础考点' }}
                </span>
                <span class="node-title">{{ node.name }}</span>
                <span class="mastery-chip" :class="masteryClass(node.masteryPercent)">
                  掌握度 {{ node.masteryPercent }}%
                </span>
              </div>
              <div v-if="idx < prerequisiteNodes.length - 1" class="tree-link-line" />
            </template>
          </div>
        </section>

        <!-- ③ 推荐变式攻坚题集 -->
        <section class="drawer-section">
          <header class="section-head">
            <h4 class="section-title">
              <el-icon><Tickets /></el-icon>
              <span>推荐变式攻坚题集</span>
            </h4>
            <el-button
              size="small"
              type="success"
              plain
              round
              :loading="variantsLoading"
              @click="emit('generate-variants')"
            >
              <el-icon v-if="!variantsLoading"><MagicStick /></el-icon>
              <span>{{ variantsLoading ? 'AI 生成中…' : (variantQuestions.length ? '重新生成' : '生成同构变式题') }}</span>
            </el-button>
          </header>

          <!-- 变式题 AI 生成中：同一推演面板，可随时中止 -->
          <AiCognitiveThinkingPanel
            v-if="variantsLoading"
            :active="variantsLoading"
            v-bind="AI_COGNITIVE_THINKING_PRESETS.wrongBookVariants"
            show-footer-actions
            abort-label="中止 AI 生成"
            @abort="emit('abort-variants')"
          />

          <div v-else-if="!variantQuestions.length" class="soft-empty">
            <el-icon class="soft-empty__icon"><Tickets /></el-icon>
            <p class="soft-empty__text">
              暂无变式题，点击右上角「生成同构变式题」，将由大模型按当前考点同构改造 2 道新题（可随时中止）
            </p>
          </div>
          <div v-else class="variant-list">
            <div
              v-for="(variant, idx) in variantQuestions"
              :key="variant.questionId"
              class="variant-item-box"
            >
              <div class="variant-left">
                <span class="variant-idx">变式 {{ idx + 1 }}</span>
                <MathText class="variant-desc" :text="variant.stem || variant.stemPreview" />
              </div>
              <el-button size="small" type="primary" round plain @click="emit('practice-variant', variant.questionId)">
                立即自测
              </el-button>
            </div>
          </div>
        </section>
      </template>

      <div v-else class="soft-empty">
        <el-icon class="soft-empty__icon"><Warning /></el-icon>
        <p class="soft-empty__text">未获取到错题信息，请关闭后重新进入</p>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button round @click="emit('update:visible', false)">关闭</el-button>
        <el-button type="primary" round :disabled="!item" @click="emit('start-practice')">
          开始这组变式题练习
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Compass, Connection, Tickets, Refresh, MagicStick, Warning, Cpu } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import type {
  WrongBookKnowledgeGraphNode,
  WrongBookVariantSummary,
  WrongQuestionRecordItem
} from '@/types/learning/wrong-question';

const props = defineProps<{
  visible: boolean;
  loading: boolean;
  /** AI 认知归因诊断进行中 */
  diagnosing: boolean;
  /** 变式题生成进行中 */
  variantsLoading: boolean;
  item: WrongQuestionRecordItem | null;
  prerequisiteNodes: WrongBookKnowledgeGraphNode[];
  variantQuestions: WrongBookVariantSummary[];
  /** 清洗掉英文类型标记后的诊断正文 */
  displayDiagnosis: (item: WrongQuestionRecordItem) => string;
  displayErrorTags: (item: WrongQuestionRecordItem) => string[];
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'regenerate-diagnosis': [];
  'abort-diagnosis': [];
  'generate-variants': [];
  'abort-variants': [];
  'practice-variant': [questionId: number];
  'start-practice': [];
}>();

const errorTags = computed(() => (props.item ? props.displayErrorTags(props.item) : []));

/** 展示用诊断正文：剥离 “类型：CONCEPT”“（READING）” 等仅用于提取 error_types 的机器标记 */
const cleanDiagnosis = computed(() => (props.item ? props.displayDiagnosis(props.item) : ''));

const hasDiagnosisText = computed(() => cleanDiagnosis.value.length >= 10);

/** 本题是否为空作答：作答缺失不构成可归因的失分模式，前端直接给出说明并禁用 AI 诊断 */
const isUnanswered = computed(() => !props.item?.studentAnswer?.trim());

const diagnosisText = computed(() => {
  if (hasDiagnosisText.value) {
    return cleanDiagnosis.value;
  }
  if (isUnanswered.value) {
    return '该题本次作答为空白（未提交答案），暂无可归因的失分环节，系统不作认知归因。';
  }
  return '尚未生成 AI 归因结论，点击右上角「AI 深度诊断」，模型将结合题干、题型难度、你的历史作答与标准答案解析定位失分根因。';
});

const diagnoseBtnText = computed(() => {
  if (props.diagnosing) return 'AI 诊断中…';
  // 未作答题目：已有历史结论时允许一键校正（后端不再调用模型，仅清掉误导性标签）
  if (isUnanswered.value) {
    return hasDiagnosisText.value ? '校正归因结论' : '未作答不作归因';
  }
  return hasDiagnosisText.value ? '重新诊断' : 'AI 深度诊断';
});

/** AI 调用进行中：此时展示的是「正在生成」，不能再挂历史来源标记 */
const aiRunning = computed(() => props.diagnosing || props.variantsLoading);

/** 当前展示的结论是否为演示（历史预置）数据；调用进行中一律按「正在生成」呈现 */
const isLegacySource = computed(() => !aiRunning.value && props.item?.diagnosisSource === 'LEGACY');

const aiStatusText = computed(() => {
  if (props.diagnosing) return '正在调用大模型进行错因认知推演，可随时点击「中止 AI 诊断」';
  if (props.variantsLoading) return '正在调用大模型同构改造变式题，可随时点击「中止 AI 生成」';
  if (isLegacySource.value) {
    return '当前展示的是演示（历史预置）结论，并非大模型产出；点击「重新诊断」可获取 AI 实时归因';
  }
  return '归因结论与变式题均由大模型实时推演生成，调用过程中可随时中止';
});

/** 顶部来源徽标文案：与结论真实来源保持一致 */
const sourceChipLabel = computed(() => (isLegacySource.value ? '演示数据' : '大模型实时生成'));

/** 归因结论来源标记：让「演示假结论」与「AI 真结论」在面板内一眼可辨 */
const sourceMeta = computed(() => {
  switch (props.item?.diagnosisSource) {
    case 'LEGACY':
      return { label: '演示数据 · 非 AI 结论', className: 'source-pill--legacy' };
    case 'UNANSWERED':
      return { label: '未作答 · 不作归因', className: 'source-pill--muted' };
    case 'AI':
      return { label: '大模型生成', className: 'source-pill--ai' };
    default:
      return null;
  }
});

/** 真实前驱考点数（排除后端固定回传的「当前考点」自身节点） */
const prerequisiteCount = computed(() => props.prerequisiteNodes.filter((node) => !node.current).length);

const hasPrerequisite = computed(() => prerequisiteCount.value > 0);

function masteryClass(percent: number) {
  if (percent >= 80) return 'mastery-chip--good';
  if (percent >= 60) return 'mastery-chip--medium';
  return 'mastery-chip--weak';
}
</script>

<style scoped lang="scss">
.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* AI 来源与调用状态提示条：让用户明确本面板结论来自大模型实时推演 */
.ai-source-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eef4ff 0%, #f5f0ff 100%);
  border: 1px solid #dbe6fb;

  .ai-source-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 22px;
    padding: 0 10px;
    border-radius: 9999px;
    background: #1677ff;
    color: #fff;
    font-size: 11.5px;
    font-weight: 700;
    flex-shrink: 0;

    .el-icon {
      font-size: 13px;
    }

    /* 演示数据态：与「大模型实时生成」明确区分 */
    &--legacy {
      background: #f97316;
    }
  }

  .ai-source-text {
    flex: 1;
    min-width: 160px;
    font-size: 12.5px;
    line-height: 1.6;
    color: #475569;
  }
}

.drawer-section {
  background: #fff;
  border: 1px solid #e8eef7;
  border-radius: 20px;
  padding: 18px 20px;
  box-shadow: 0 6px 20px rgba(30, 80, 150, 0.05);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;

  .el-icon {
    color: #1677ff;
    font-size: 17px;
  }
}

.section-hint {
  font-size: 12.5px;
  color: #94a3b8;
}

.diagnosis-callout {
  background: linear-gradient(135deg, #f6f9ff 0%, #f3f6fe 100%);
  border-radius: 16px;
  padding: 14px 16px;
  border-left: 4px solid #f5222d;

  .callout-head {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 8px;
  }

  .summary-p {
    display: block;
    margin: 0;
    font-size: 13.5px;
    line-height: 1.75;
    color: #334155;
    word-break: break-word;

    &--muted {
      color: #94a3b8;
    }
  }

  .unanswered-hint {
    margin: 10px 0 0;
    font-size: 12.5px;
    line-height: 1.65;
    color: #b45309;
  }

  .legacy-hint {
    margin: 10px 0 0;
    font-size: 12.5px;
    line-height: 1.65;
    color: #c2410c;
  }

  .tags-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-top: 10px;
  }
}

/* 归因结论来源标记（演示数据 / 未作答 / AI 实时生成） */
.source-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 9999px;
  font-size: 11.5px;
  font-weight: 700;
  border: 1px solid transparent;
  white-space: nowrap;

  &--legacy {
    background: #fff7ed;
    color: #c2410c;
    border-color: #fed7aa;
  }

  &--muted {
    background: #f1f5f9;
    color: #64748b;
    border-color: #cbd5e1;
  }

  &--ai {
    background: #eff6ff;
    color: #1d4ed8;
    border-color: #bfdbfe;
  }
}

.soft-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;

  &--danger {
    background: #fff1f0;
    color: #cf1322;
    border: 1px solid #ffccc7;
  }
}

.soft-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 22px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px dashed #dbe4f0;

  &__icon {
    font-size: 22px;
    color: #c0cbd9;
  }

  &__text {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
    color: #94a3b8;
    text-align: center;
  }
}

.prereq-tree {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;

  .prereq-note {
    margin: 0 0 10px;
    font-size: 12.5px;
    line-height: 1.65;
    color: #94a3b8;
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    width: 100%;
    padding: 12px 16px;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    transition: all 0.2s ease;

    &--parent {
      background: #f4fdf7;
      border-color: #c8efd4;
    }

    &--current {
      background: #fff5f5;
      border-color: #ffccc7;
    }
  }

  .node-badge {
    font-size: 11px;
    font-weight: 700;
    padding: 3px 10px;
    border-radius: 9999px;
    background: #e8eef7;
    color: #475569;

    &--current {
      background: #fff1f0;
      color: #cf1322;
    }
  }

  .node-title {
    flex: 1;
    min-width: 110px;
    font-size: 13.5px;
    font-weight: 600;
    color: #1e293b;
  }

  .tree-link-line {
    width: 2px;
    height: 16px;
    margin-left: 28px;
    background: linear-gradient(180deg, #d7e2f0 0%, #eef3fa 100%);
    border-radius: 9999px;
  }
}

.mastery-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 700;

  &--good {
    background: #f0fdf4;
    color: #15803d;
    border: 1px solid #bbf7d0;
  }

  &--medium {
    background: #fffbeb;
    color: #b45309;
    border: 1px solid #fde68a;
  }

  &--weak {
    background: #fef2f2;
    color: #b91c1c;
    border: 1px solid #fecaca;
  }
}

.variant-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .variant-item-box {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 16px;
    border-radius: 16px;
    border: 1px solid #e8eef7;
    background: #fff;
    transition: border-color 0.2s ease, background-color 0.2s ease;

    &:hover {
      border-color: #bfdbfe;
      background: #f5faff;
    }

    .variant-left {
      display: flex;
      flex-direction: column;
      gap: 3px;
      flex: 1;
      min-width: 0;

      .variant-idx {
        font-size: 12px;
        font-weight: 700;
        color: #1677ff;
      }

      .variant-desc {
        /* 后端回传完整题干（保证公式不被截断），这里按 3 行裁切展示 */
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
        font-size: 13px;
        line-height: 1.55;
        color: #334155;
        word-break: break-word;
      }
    }
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
