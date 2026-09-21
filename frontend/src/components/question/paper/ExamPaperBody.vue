<template>
  <div class="exam-paper-body" :class="[`skin-${skin}`]">
    <template v-if="groupedSections.length > 0">
      <div
        v-for="(sec, sIdx) in groupedSections"
        :key="sec.type + sIdx"
        class="sheet-section-block"
      >
        <div class="section-title-bar" :class="{ 'mt-2': sIdx > 0 }">
          <div class="section-heading">
            <template v-if="skin === 'export'">
              {{ getChineseNumber(sIdx + 1) }}、{{ sec.title }}
              <span v-if="showSectionScore" class="sec-score-inline">
                {{ sectionScoreText(sec) }}
              </span>
            </template>
            <template v-else>
              <span class="sec-number">{{ getChineseNumber(sIdx + 1) }}、{{ sec.title }}</span>
              <span v-if="showSectionScore" class="sec-score-info">
                {{ sectionScoreText(sec) }}
              </span>
            </template>
          </div>
        </div>

        <div v-if="skin === 'export'" class="section-notice">
          {{ sectionNotice(sec) }}
        </div>

        <div class="questions-list" :class="{ 'solving-list': hasSolving(sec) }">
          <div
            v-for="(q, qIdx) in sec.questions"
            :key="q.id ?? `${sec.type}-${qIdx}`"
            class="mock-q-item"
            :class="{ 'solving-item': isSolvingType(q.type) }"
          >
            <div class="q-title-row">
              <div class="q-text">
                <span class="q-index-inline">{{ globalIndex(sIdx, qIdx) }}.</span>
                <MathText tag="span" class="q-stem-math" :text="stemForDisplay(q.stem)" />
                <span v-if="shouldShowChoiceBlank(q.type, q.stem)" class="q-blank-hint">（&nbsp;&nbsp;）</span>
              </div>
              <span v-if="showPointBadge" class="point-tag">（{{ q.score || 5 }}分）</span>
            </div>

            <div
              v-if="q.options && q.options.length"
              class="q-options"
              :class="[`layout-${optionLayout}`, `opt-count-${Math.min(q.options.length, 4)}`]"
            >
              <div v-for="opt in q.options" :key="opt.key" class="opt-cell">
                <span class="opt-key">{{ opt.key }}.</span>
                <MathText tag="span" class="opt-content-math" :text="opt.content" />
              </div>
            </div>

            <div
              v-if="viewMode === 'ANSWER_KEY'"
              class="answer-key-box"
            >
              <div class="ans-row">
                <span class="ans-title">【标准答案】</span>
                <MathText tag="span" class="ans-text" :text="q.correctAnswer || '略'" />
              </div>
              <div v-if="q.analysis" class="ans-row">
                <span class="ans-title">【试题解析】</span>
                <MathText tag="div" class="ans-text" :text="q.analysis" />
              </div>
              <div v-if="q.knowledgePointNames?.length" class="ans-row">
                <span class="ans-title">【考查考点】</span>
                <span
                  v-for="kp in q.knowledgePointNames"
                  :key="kp"
                  class="kp-pill"
                >{{ kp }}</span>
              </div>
            </div>

            <div
              v-else-if="isSolvingType(q.type) && showAnswerArea"
              class="draft-answer-box"
            >
              <span class="draft-tip">【考生答题区域 · 书写需工整清晰】</span>
            </div>

            <div
              v-else-if="(q.type === 'FILL_BLANK' || q.type === 'SHORT_ANSWER') && showAnswerArea && skin === 'detail'"
              class="student-blank-area"
            >
              <div class="answer-guide">考生答题区：</div>
              <div class="ruled-lines">
                <div class="line"></div>
                <div class="line"></div>
                <div class="line"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <el-empty v-else description="当前试卷暂无试题，请先组卷或从题库选题" />
  </div>
</template>

<script setup lang="ts">
import MathText from '@/components/common/MathText.vue';
import type { GroupedSection } from '@/composables/question/useExam';
import { getChineseNumber } from '@/composables/question/useExam';
import type { QuestionType } from '@/types/question/question';
import { stripQuestionStemNumber } from '@/utils/question/display-stem';

const props = withDefaults(
  defineProps<{
    groupedSections: GroupedSection[];
    optionLayout?: string;
    showPointBadge?: boolean;
    showSectionScore?: boolean;
    showAnswerArea?: boolean;
    viewMode?: 'PAPER' | 'ANSWER_KEY';
    skin?: 'export' | 'detail';
  }>(),
  {
    optionLayout: 'horizontal',
    showPointBadge: false,
    showSectionScore: true,
    showAnswerArea: true,
    viewMode: 'PAPER',
    skin: 'export'
  }
);

function sectionScoreText(sec: GroupedSection): string {
  const count = sec.questions?.length || 0;
  if (!count) return '';
  const firstScore = sec.questions[0]?.score;
  const allSame = firstScore != null && sec.questions.every((q) => q.score === firstScore);
  if (allSame && count > 1) {
    return `（共 ${count} 小题，每小题 ${firstScore} 分，合计 ${sec.totalScore} 分）`;
  }
  return `（共 ${count} 小题，合计 ${sec.totalScore} 分）`;
}

function stemForDisplay(stem: string): string {
  return stripQuestionStemNumber(stem);
}

function globalIndex(sectionIdx: number, questionIdx: number): number {
  let before = 0;
  for (let i = 0; i < sectionIdx; i++) {
    before += props.groupedSections[i]?.questions.length ?? 0;
  }
  return before + questionIdx + 1;
}

function isChoiceType(type: QuestionType | string): boolean {
  return type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE' || type === 'TRUE_FALSE';
}

function shouldShowChoiceBlank(type: QuestionType | string, stem: string): boolean {
  if (!isChoiceType(type)) return false;
  // 若题干中已有括号作答位，如 （ ）、()、（   ），则不重复追加末尾括号
  return !/[（(]\s*[)）]/.test(stem || '');
}

function isSolvingType(type: QuestionType | string): boolean {
  return type === 'SHORT_ANSWER';
}

function hasSolving(sec: GroupedSection): boolean {
  return sec.questions.some((q) => isSolvingType(q.type));
}

function sectionNotice(sec: GroupedSection): string {
  const map: Record<string, string> = {
    SINGLE_CHOICE: `本大题共 ${sec.questions.length} 小题，每小题按卷面标注分值计分。在每小题给出的选项中，只有一项是符合题目要求的。`,
    MULTIPLE_CHOICE: `本大题共 ${sec.questions.length} 小题，在每小题给出的选项中，有多项符合题目要求。`,
    TRUE_FALSE: `本大题共 ${sec.questions.length} 小题，请判断下列各题正误。`,
    FILL_BLANK: `本大题共 ${sec.questions.length} 小题，请将答案填写在答题卡规定横线上。`,
    SHORT_ANSWER: `本大题共 ${sec.questions.length} 小题，解答应写出文字说明、证明过程或演算步骤。`
  };
  return map[sec.type] || `本大题共 ${sec.questions.length} 小题。`;
}
</script>

<style scoped lang="scss">
.exam-paper-body.skin-export {
  .section-heading {
    font-size: 14px;
    font-weight: 700;
    margin: 8px 0 4px;
    color: #000;
  }

  .sec-score-inline {
    font-weight: 500;
    font-size: 12px;
  }

  .section-notice {
    font-size: 12px;
    line-height: 1.55;
    margin-bottom: 10px;
    color: #222;
  }

  .mock-q-item {
    margin-bottom: 16px;
    page-break-inside: avoid;
  }

  .q-title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  .q-text {
    flex: 1;
    font-size: 13px;
    line-height: 1.75;
    margin: 0;
    color: #000;
    text-align: justify;
  }

  .q-index-inline {
    font-weight: 700;
    margin-right: 4px;
  }

  .point-tag {
    flex-shrink: 0;
    font-size: 11px;
    color: #333;
    white-space: nowrap;
  }

  .q-options {
    margin: 8px 0 0 1.6em;
    font-size: 12.5px;
    line-height: 1.65;

    .opt-cell {
      display: flex;
      align-items: flex-start;
      gap: 4px;
      min-width: 0;
    }

    .opt-key {
      flex-shrink: 0;
      font-weight: 600;
    }

    &.layout-horizontal {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      column-gap: 10px;
      row-gap: 6px;

      &.opt-count-2 {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      &.opt-count-3 {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }
    }

    &.layout-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      column-gap: 16px;
      row-gap: 6px;
    }

    &.layout-vertical {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
  }
}

.exam-paper-body.skin-detail {
  .sheet-section-block {
    margin-bottom: 28px;
  }

  .section-title-line {
    margin-bottom: 12px;
    font-weight: 700;
    font-size: 15px;
    color: #111827;

    .sec-score-info {
      font-weight: 500;
      color: #64748b;
      font-size: 13px;
    }
  }

  .paper-question-card,
  .mock-q-item {
    margin-bottom: 18px;
  }

  .q-title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .q-index-inline {
    font-weight: 600;
    margin-right: 4px;
  }

  .q-stem-text,
  .q-text {
    flex: 1;
    font-size: 14px;
    line-height: 1.65;
    color: #1e293b;
  }

  .point-tag {
    flex-shrink: 0;
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  .options-grid,
  .q-options {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 16px;
    margin: 10px 0 0 24px;
    font-size: 14px;

    &.layout-horizontal {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 24px;
    }

    &.layout-vertical {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
  }

  .option-item,
  .opt {
    display: flex;
    align-items: flex-start;
    gap: 4px;

    &.is-correct-answer {
      color: #059669;
      font-weight: 600;
    }
  }

  .answer-key-box {
    margin: 10px 0 0 24px;
    padding: 10px 12px;
    background: #f8fafc;
    border-radius: 6px;
    font-size: 13px;

    .ans-row {
      margin-bottom: 6px;
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .ans-title {
      color: #64748b;
      flex-shrink: 0;
    }

    .kp-pill {
      background: #e0f2fe;
      color: #0369a1;
      padding: 2px 8px;
      border-radius: 999px;
      font-size: 12px;
    }
  }

  .student-blank-area {
    margin: 12px 0 0 24px;

    .answer-guide {
      font-size: 12px;
      color: #94a3b8;
      margin-bottom: 6px;
    }

    .ruled-lines .line {
      border-bottom: 1px solid #cbd5e1;
      height: 28px;
    }
  }
}
</style>
