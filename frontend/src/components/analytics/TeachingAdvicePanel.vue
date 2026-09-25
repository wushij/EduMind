<template>
  <div class="split-right-col">
    <div class="report-panel-card">
      <div class="panel-header-line">
        <div class="header-left">
          <el-icon class="panel-icon panel-icon--amber"><PieChart /></el-icon>
          <h3 class="panel-title">AI 学情错因聚类分析</h3>
        </div>
      </div>

      <div v-if="errorCategories.length === 0" class="error-empty-box">
        <el-empty description="当前课程暂无可归因的错题记录" :image-size="80" />
      </div>

      <div v-else class="error-diagnosis-list">
        <div
          v-for="err in errorCategories"
          :key="err.type"
          class="error-cat-box"
        >
          <div class="error-top-line">
            <span class="err-name">{{ getErrorName(err) }}</span>
            <span class="err-percent">{{ err.percent }}% 占比</span>
          </div>
          <div class="capsule-progress-track">
            <div class="capsule-progress-fill" :style="{ width: `${err.percent}%`, background: err.color }"></div>
          </div>
          <p class="err-desc">{{ getErrorDesc(err) }}</p>
        </div>
      </div>
    </div>

    <div
      ref="aiCardRef"
      class="report-panel-card report-panel-card--ai-suggestion"
      :class="{ 'is-highlight': isHighlighted }"
    >
      <div class="panel-header-line">
        <div class="header-left">
          <el-icon class="panel-icon panel-icon--purple"><Opportunity /></el-icon>
          <h3 class="panel-title">AI 教学策略改进建议</h3>
        </div>
        <span v-if="hasAiAdvice" class="ai-source-tag">模型生成</span>
      </div>

      <div class="suggestion-content-box">
        <!-- 有真实模型结论时优先展示：诊断结论 + 逐条干预清单 -->
        <template v-if="hasAiAdvice">
          <div class="suggestion-bubble">
            <div class="bot-avatar">
              <el-icon><Service /></el-icon>
            </div>
            <div class="bubble-text">
              <strong>AI 诊断结论：</strong>
              <!-- 模型结论含数学公式（如 $\\lim_{x \\to \\infty}(1+\\frac{a}{x})^{bx}=e^{ab}$），必须走 KaTeX 渲染 -->
              <p class="math-rendered-body" v-html="adviceSummaryHtml"></p>
            </div>
          </div>

          <ol v-if="(teachingAdvice?.actions ?? []).length > 0" class="ai-action-list">
            <li v-for="(action, idx) in teachingAdvice?.actions ?? []" :key="idx" class="ai-action-item">
              <span class="action-index">{{ idx + 1 }}</span>
              <span class="action-text math-rendered-body" v-html="renderMathText(action)" />
            </li>
          </ol>
        </template>

        <div v-else class="suggestion-bubble">
          <div class="bot-avatar">
            <el-icon><Service /></el-icon>
          </div>
          <div class="bubble-text">
            <strong>课堂教学与改进建议：</strong>
            <p v-if="reportData && reportData.weakPoints && reportData.weakPoints.length > 0">
              根据错题记录与错因诊断，学生在<strong>《{{ topWeakPointNames }}》</strong>等考点存在较集中的错因分布。建议在下一阶段教学中针对上述考点安排变式巩固与错因讲评，并布置对应知识点的定向练习。
            </p>
            <p v-else>
              当前课程暂无已归因的错题记录，无法生成集中性错因分析。可先通过作业批改与课程 AI 助教积累作答数据后再行评估。
            </p>
            <p class="bubble-tip">点击右上角「AI 智能诊断」，由大模型结合真实学情数据生成诊断结论与干预清单。</p>
          </div>
        </div>

        <div class="action-buttons-stack">
          <button
            type="button"
            class="capsule-card-action-btn"
            @click="goLessonStudio"
          >
            <el-icon class="btn-inner-icon"><DocumentAdd /></el-icon>
            <span>{{ lessonStudioActionText }}</span>
          </button>

          <button
            type="button"
            class="capsule-card-action-btn capsule-card-action-btn--secondary"
            @click="router.push(`/course/${courseId}/ai`)"
          >
            <el-icon class="btn-inner-icon"><ChatDotRound /></el-icon>
            <span>与课程 AI 助教研讨教学方案</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from 'vue';
import { PieChart, Opportunity, Service, DocumentAdd, ChatDotRound } from '@element-plus/icons-vue';
import type { Router } from 'vue-router';
import type { TeachingReportVO } from '@/types/analytics/report';
import type { TeachingAdviceVO } from '@/types/analytics/mastery';
import { renderMathText } from '@/utils/format/render-math';
import {
  openLessonStudio,
  type LessonStudioWeakPoint
} from '@/services/course/lesson-studio-entry';

const props = defineProps<{
  router: Router;
  courseId: number;
  reportData: TeachingReportVO | null;
  /** 大模型生成的教学诊断结论；为空表示尚未生成，回退为模板化建议文案 */
  teachingAdvice?: TeachingAdviceVO | null;
  topWeakPointNames: string;
  errorCategories: Array<{ type: string; name: string; percent: number; color: string; desc: string }>;
}>();

/** 是否已拿到真实模型结论（有摘要即视为有效） */
const hasAiAdvice = computed(() => Boolean(props.teachingAdvice?.summary));

/**
 * 诊断结论的公式渲染结果。
 *
 * <p>模型结论里必然出现极限、导数、等价无穷小等数学表达，若直接用插值输出，
 * 教师看到的是一串「lim(1+a/x)^(bx)=e^(ab)」这样的纯文本，与左侧榜单的诊断卡片排版割裂。</p>
 */
const adviceSummaryHtml = computed(() => renderMathText(props.teachingAdvice?.summary ?? ''));

/** 建议卡片 DOM 与高亮态：诊断生成后需要把视线引到这张卡上 */
const aiCardRef = ref<HTMLElement | null>(null);
const isHighlighted = ref(false);
let highlightTimer: number | null = null;

/**
 * 由父页面在诊断生成完成后调用。
 *
 * 建议卡片位于左右分栏的右栏第二张卡，在常见屏幕高度下处于首屏之外：
 * 生成成功若不主动定位，用户会以为「点了没反应、结果不见了」。
 */
async function focusAdvice() {
  await nextTick();
  aiCardRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  isHighlighted.value = true;
  if (highlightTimer !== null) window.clearTimeout(highlightTimer);
  highlightTimer = window.setTimeout(() => {
    isHighlighted.value = false;
  }, 2600);
}

onUnmounted(() => {
  if (highlightTimer !== null) window.clearTimeout(highlightTimer);
});

defineExpose({ focusAdvice });

/** 错因类型 → 典型成因描述（与后端 ERROR_TYPE_LABELS 取值域一致） */
const errorDescMap: Record<string, string> = {
  CONCEPT: '对基础概念定义判定标准或充分必要条件认知模糊',
  CALC: '步骤繁琐导致的运算失误、符号漏算或恒等变形错误',
  LOGIC: '解题步骤推理跳跃、前后因果倒置或推导链断层',
  READING: '未准确提炼题设核心限定条件或忽略了隐含边界',
  TRANSFER: '知识点在跨情境迁移时适用边界判断失误',
  MEMORY: '基础公式或结论记忆不牢，长时间未复习导致遗忘'
};

/** 错因类型 → 中文名，仅在后端未返回中文名时兜底 */
const errorNameMap: Record<string, string> = {
  CONCEPT: '概念理解错误',
  LOGIC: '逻辑推理错误',
  CALC: '计算失误',
  READING: '审题理解偏差',
  TRANSFER: '迁移应用错误',
  MEMORY: '记忆遗忘型错误'
};

function getErrorDesc(err: { type: string; name: string; desc: string }): string {
  if (errorDescMap[err.type]) return errorDescMap[err.type];
  if (err.desc && err.desc !== err.name && err.desc !== err.type) return err.desc;
  return '典型薄弱项成因，需结合变式训练加深理解';
}

function getErrorName(err: { type: string; name: string }): string {
  // 后端已完成类型 → 中文名映射时直接使用；未知类型走本地兜底
  if (err.name && err.name !== err.type) return err.name;
  return errorNameMap[err.type] ?? err.type;
}

/** 教情报告薄弱考点 → 备课课节上下文：名称优先取知识点名，其次退回错题标题 */
const prepWeakPoints = computed<LessonStudioWeakPoint[]>(() =>
  (props.reportData?.weakPoints ?? [])
    .map(item => ({
      name: (item.knowledgePointName || item.title || '').trim(),
      errorTypeName: item.errorTypeName ?? null,
      errorReason: item.errorReason || item.suggestion || null
    }))
    .filter(item => item.name)
);

/** 有薄弱考点时走「新建备课课节」，否则退回进入已有课节 */
const lessonStudioActionText = computed(() =>
  prepWeakPoints.value.length > 0
    ? '新建备课课节并生成针对性教案'
    : '进入课节用 AI 备课生成针对性教案'
);

function goLessonStudio() {
  void openLessonStudio(props.router, props.courseId, { weakPoints: prepWeakPoints.value });
}
</script>

<style scoped lang="scss">
.split-right-col {
  .report-panel-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 22px 24px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    margin-bottom: 20px;

    .panel-header-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .panel-icon {
          font-size: 18px;
          display: inline-flex;
          align-items: center;
          justify-content: center;

          &--amber { color: #D97706; }
          &--purple { color: #722ED1; }
        }

        .panel-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      /* 真实模型结论标识：与模板化文案明确区分，避免用户误以为建议是模型生成的 */
      .ai-source-tag {
        height: 22px;
        padding: 0 10px;
        border-radius: 9999px;
        background: #F3E8FF;
        color: #722ED1;
        border: 1px solid #E9D5FF;
        font-size: 11px;
        font-weight: 600;
        line-height: 20px;
      }
    }

    .error-empty-box {
      padding: 28px 0;
      display: flex;
      justify-content: center;
    }

    .error-diagnosis-list {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .error-cat-box {
        .error-top-line {
          display: flex;
          justify-content: space-between;
          font-size: 13px;
          margin-bottom: 6px;

          .err-name {
            font-weight: 600;
            color: #1E293B;
          }

          .err-percent {
            font-weight: 700;
            color: #1677FF;
          }
        }

        .capsule-progress-track {
          width: 100%;
          height: 6px;
          background: #E2E8F0;
          border-radius: 9999px;
          overflow: hidden;
          margin-bottom: 6px;

          .capsule-progress-fill {
            height: 100%;
            border-radius: 9999px;
          }
        }

        .err-desc {
          margin: 0;
          font-size: 11.5px;
          color: #94A3B8;
          line-height: 1.5;
        }
      }
    }

    &--ai-suggestion {
      background: linear-gradient(135deg, #FAF5FF 0%, #FFFFFF 100%);
      border-color: #E9D5FF;
      transition: box-shadow 0.35s ease, border-color 0.35s ease;

      /* 生成完成后短暂高亮：帮助用户在长页面中定位到结果 */
      &.is-highlight {
        border-color: #C084FC;
        box-shadow:
          0 0 0 3px rgba(114, 46, 209, 0.16),
          0 8px 24px rgba(114, 46, 209, 0.18);
      }

      .suggestion-content-box {
        display: flex;
        flex-direction: column;
        gap: 16px;

        /* 模型输出的干预行动清单：序号胶囊 + 文本，避免长段落堆叠难以执行 */
        .ai-action-list {
          list-style: none;
          margin: 0;
          padding: 0;
          display: flex;
          flex-direction: column;
          gap: 10px;

          .ai-action-item {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            padding: 10px 12px;
            border-radius: 12px;
            background: #FFFFFF;
            border: 1px solid #E9D5FF;

            .action-index {
              width: 20px;
              height: 20px;
              border-radius: 50%;
              background: #722ED1;
              color: #FFFFFF;
              font-size: 11px;
              font-weight: 700;
              display: inline-flex;
              align-items: center;
              justify-content: center;
              flex-shrink: 0;
              margin-top: 1px;
            }

            .action-text {
              font-size: 12.5px;
              color: #334155;
              line-height: 1.6;
            }
          }
        }

        /* 公式排版：与榜单诊断卡片保持一致的 KaTeX 字号与配色 */
        .math-rendered-body {
          :deep(.katex) {
            font-size: 1.05em;
          }

          :deep(.katex-html) {
            color: #0f172a;
          }
        }

        .bubble-tip {
          margin-top: 6px !important;
          font-size: 11.5px;
          color: #94A3B8;
        }

        .suggestion-bubble {
          display: flex;
          gap: 12px;

          .bot-avatar {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            background: linear-gradient(135deg, #722ED1 0%, #9333EA 100%);
            color: #FFFFFF;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            flex-shrink: 0;
            box-shadow: 0 4px 12px rgba(114, 46, 209, 0.25);
          }

          .bubble-text {
            font-size: 12.5px;
            color: #334155;
            line-height: 1.6;

            strong {
              color: #722ED1;
            }

            p {
              margin: 4px 0 0 0;
            }
          }
        }

        .action-buttons-stack {
          display: flex;
          flex-direction: column;
          gap: 10px;

          .capsule-card-action-btn {
            width: 100%;
            height: 38px;
            border-radius: 9999px;
            background: #722ED1;
            color: #FFFFFF;
            border: none;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 2px 8px rgba(114, 46, 209, 0.25);
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;

            .btn-inner-icon {
              font-size: 15px;
            }

            &:hover {
              background: #531DAB;
            }

            &--secondary {
              background: #FFFFFF;
              border: 1px solid #CBD5E1;
              color: #334155;
              box-shadow: none;

              &:hover {
                background: #F8FAFC;
                color: #1677FF;
              }
            }
          }
        }
      }
    }
  }
}
</style>
