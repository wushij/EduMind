<template>
  <div class="preview-canvas-card">
    <div class="canvas-top-bar no-print">
      <div class="preview-toolbar">
        <div class="preview-toolbar-row preview-toolbar-row--tabs">
          <div class="preview-segmented" role="tablist" aria-label="卷面预览分页">
            <button
              v-for="tab in previewTabs"
              :key="tab.id"
              type="button"
              role="tab"
              class="seg-btn"
              :class="{ 'is-active': previewPageMode === tab.id }"
              @click="previewPageMode = tab.id"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>

        <div class="preview-toolbar-row preview-toolbar-row--actions">
          <div class="preview-tools-grid">
            <div class="tool-slot">
              <div class="zoom-chip">
                <button type="button" class="chip-btn" aria-label="缩小" @click="zoomScale = Math.max(0.5, Number((zoomScale - 0.05).toFixed(2)))">−</button>
                <span class="zoom-text">{{ Math.round(zoomScale * 100) }}%</span>
                <button type="button" class="chip-btn" aria-label="放大" @click="zoomScale = Math.min(1.2, Number((zoomScale + 0.05).toFixed(2)))">+</button>
                <button type="button" class="chip-link" @click="zoomScale = 0.85">自适应</button>
              </div>
            </div>

            <div class="tool-slot">
              <el-select v-model="printScope" size="small" class="print-scope-select">
                <el-option label="打印：整套试卷" value="all" />
                <el-option label="打印：当前视图" value="current" />
              </el-select>
            </div>

            <div class="tool-slot">
              <button type="button" class="print-capsule-btn" @click="handlePrintDirect">
                <el-icon><Printer /></el-icon>
                <span>纯净打印</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-loading="previewLoading" class="paper-scroll-wrapper">
      <div class="paper-scaler" :style="{ zoom: zoomScale }">
        <div
          id="printable-exam-paper"
          class="export-paper-root"
          :class="[`font-${configForm.fontFamily}`, `spacing-${configForm.lineSpacing}`, { 'is-b4-mode': configForm.paperSize === 'B4' }]"
        >
          <div
            v-show="showBodyPage"
            class="simulated-sheet exam-page-body"
            :class="sheetPrintClass('body')"
          >
            <div v-if="configForm.showWatermark" class="watermark-layer" aria-hidden="true">
              <span class="watermark-text">{{ configForm.watermarkText || '智教云 · EduMind 官方试卷' }}</span>
            </div>

            <ExportExamSealingStrip v-if="configForm.showSealingLine" />

            <div class="sheet-main-content">
              <div class="sheet-header">
                <div class="confidential-bar">
                  <span v-if="configForm.confidentialLevel" class="confidential-badge">{{ configForm.confidentialLevel }}</span>
                  <span v-else class="confidential-badge conf-placeholder"></span>
                  <span class="exam-type-badge">试题卷</span>
                </div>
                <h1 class="paper-main-title">{{ configForm.paperTitle || examPaper?.title || '试卷标题' }}</h1>
                <h3 class="paper-sub-title">{{ configForm.paperSubtitle || defaultSubtitle }}</h3>
                <div class="paper-rules-bar">
                  <span>科目：{{ examPaper?.courseName || '—' }}</span>
                  <span>试卷满分：{{ examPaper?.totalScore ?? '—' }} 分</span>
                  <span>考试时间：{{ examPaper?.durationMinutes ?? '—' }} 分钟</span>
                  <span>题量：{{ totalQuestionsCount }} 题</span>
                </div>
              </div>

              <div v-if="configForm.showNoticeBar" class="exam-notice-box">
                <div class="notice-title">考生须知：</div>
                <ol class="notice-list">
                  <li>答卷前，考生务必将自己的姓名、准考证号填写在试卷和答题卡相应位置上。</li>
                  <li>回答选择题时，选出每小题答案后，用 2B 铅笔把答题卡上对应题目的答案标号涂黑。</li>
                  <li>回答非选择题时，将答案书写在答题卡规定区域内，写在本试卷及草稿纸上无效。</li>
                </ol>
              </div>

              <div v-if="configForm.showStudentInfo" class="candidate-info-box">
                <template v-if="configForm.showAnswerSheet">
                  <p class="paper-lite-info">
                    本试卷与配套答题卡同时使用。姓名、准考证号、考场与座位号等请在答题卡规定位置填写或填涂；试题卷密封线内请勿作答。
                  </p>
                </template>
                <template v-else>
                  <div class="info-fill-row">
                    <span>姓名：</span><span class="fill-blank long"></span>
                    <span class="gap">班级：</span><span class="fill-blank mid"></span>
                  </div>
                  <div class="info-fill-row">
                    <span>准考证号：</span>
                    <div class="barcode-cells">
                      <span v-for="i in 10" :key="i" class="cell"></span>
                    </div>
                  </div>
                  <div class="barcode-sticker-box">
                    <span>贴条形码区（请勿贴出虚线框）</span>
                  </div>
                </template>
              </div>

              <div v-if="configForm.showScoreGrid && groupedSections.length" class="score-summary-table">
                <table class="score-grid-table">
                  <thead>
                    <tr>
                      <th class="col-label">题号</th>
                      <th v-for="(sec, si) in groupedSections" :key="'h-' + sec.type">
                        {{ getChineseNumber(si + 1) }}
                      </th>
                      <th class="col-total">总分</th>
                      <th class="col-grader">评卷人</th>
                      <th class="col-grader">复核人</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>得分</td>
                      <td v-for="sec in groupedSections" :key="'s-' + sec.type"></td>
                      <td></td>
                      <td rowspan="2"></td>
                      <td rowspan="2"></td>
                    </tr>
                    <tr>
                      <td>满分</td>
                      <td v-for="sec in groupedSections" :key="'m-' + sec.type">{{ sec.totalScore }}</td>
                      <td>{{ examPaper?.totalScore ?? '' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div v-if="previewError" class="preview-error-hint">{{ previewError }}</div>

              <ExamPaperBody
                :grouped-sections="groupedSections"
                :option-layout="configForm.optionLayout"
                :show-point-badge="configForm.showPointBadge"
                skin="export"
              />

              <div class="sheet-footer">
                <span>智教云 · EduMind 智能命题与考务排版系统</span>
                <span>{{ configForm.paperTitle || examPaper?.title }} · 试卷正文</span>
              </div>
            </div>
          </div>

          <div
            v-if="configForm.showAnswerSheet"
            v-show="showAnswerSheetPage"
            class="simulated-sheet exam-page-answer answer-sheet-page"
            :class="sheetPrintClass('answerSheet')"
          >
            <div class="sheet-main-content answer-sheet-container">
              <ExportGaokaoAnswerSheet
                v-if="totalQuestionsCount > 0"
                :title="configForm.paperTitle || examPaper?.title || '答题卡'"
                :course-name="examPaper?.courseName"
                :total-score="examPaper?.totalScore ?? 100"
                :objective-items="answerSheetObjective"
                :fill-blank-items="answerSheetFillBlank"
                :short-answer-items="answerSheetShortAnswer"
              />
              <el-empty v-else description="本卷暂无题目，无需打印答题卡" />
            </div>
          </div>

          <div
            v-if="configForm.showAnalysis"
            v-show="showAnalysisPage"
            class="simulated-sheet exam-page-analysis analysis-page"
            :class="sheetPrintClass('analysis')"
          >
            <div class="sheet-main-content">
              <div class="sheet-header">
                <h2 class="analysis-title">{{ configForm.paperTitle || examPaper?.title }} · 参考答案与解析</h2>
                <div class="paper-rules-bar">
                  <span>教研备课专用</span>
                  <span>含标准答案与文字解析</span>
                </div>
              </div>

              <div class="analysis-content">
                <div v-for="(q, idx) in flatQuestions" :key="q.id ?? idx" class="analysis-block">
                  <h4>第 {{ idx + 1 }} 题</h4>
                  <div class="analysis-line">
                    <strong>【答案】</strong>
                    <MathText tag="span" custom-class="analysis-math" :text="q.correctAnswer || '略'" />
                  </div>
                  <p v-if="q.analysis" class="analysis-line analysis-line--flow">
                    <strong>【解析】</strong>
                    <MathText tag="span" custom-class="analysis-math" :text="q.analysis" />
                  </p>
                  <p v-else class="muted">暂无解析</p>
                </div>
                <el-empty v-if="!flatQuestions.length" description="暂无题目解析" />
              </div>

              <div class="sheet-footer">
                <span>智教云 · EduMind 考务排版系统</span>
                <span>教师参考答案</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Printer } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import ExamPaperBody from '@/components/question/paper/ExamPaperBody.vue';
import ExportGaokaoAnswerSheet from '@/components/question/export/ExportGaokaoAnswerSheet.vue';
import ExportExamSealingStrip from '@/components/question/export/ExportExamSealingStrip.vue';
import type { AnswerSheetQuestionItem } from '@/components/question/export/ExportGaokaoAnswerSheet.vue';
import type { ExportConfigForm } from '@/composables/question/useExport';
import type { ExamPaper } from '@/types/question/exam';
import type { GroupedSection } from '@/composables/question/useExam';
import { getChineseNumber } from '@/composables/question/useExam';

const props = defineProps<{
  configForm: ExportConfigForm;
  examPaper: ExamPaper | null;
  groupedSections: GroupedSection[];
  totalQuestionsCount: number;
  objectiveQuestionCount: number;
  previewLoading?: boolean;
  previewError?: string;
  handlePrintDirect: () => void;
}>();

const previewPageMode = defineModel<string>('previewPageMode', { required: true });
const zoomScale = defineModel<number>('zoomScale', { required: true });
const printScope = defineModel<string>('printScope', { required: true });

const defaultSubtitle = computed(() => {
  if (!props.examPaper) return '';
  return `${props.examPaper.courseName} · 满分 ${props.examPaper.totalScore} 分 · ${props.examPaper.durationMinutes} 分钟`;
});

const flatQuestions = computed(() => props.examPaper?.questions ?? []);

const OBJECTIVE_TYPES = new Set(['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE']);

function buildAnswerSheetBuckets() {
  const objective: AnswerSheetQuestionItem[] = [];
  const fillBlank: AnswerSheetQuestionItem[] = [];
  const shortAnswer: AnswerSheetQuestionItem[] = [];
  flatQuestions.value.forEach((q, i) => {
    const item: AnswerSheetQuestionItem = {
      index: i + 1,
      type: q.type,
      optionCount: q.options?.length ?? 4
    };
    if (OBJECTIVE_TYPES.has(q.type)) {
      objective.push(item);
    } else if (q.type === 'FILL_BLANK') {
      fillBlank.push(item);
    } else {
      shortAnswer.push(item);
    }
  });
  return { objective, fillBlank, shortAnswer };
}

const answerSheetObjective = computed(() => buildAnswerSheetBuckets().objective);
const answerSheetFillBlank = computed(() => buildAnswerSheetBuckets().fillBlank);
const answerSheetShortAnswer = computed(() => buildAnswerSheetBuckets().shortAnswer);

const previewTabs = computed(() => {
  const tabs = [{ id: 'body', label: '试卷正文' }];
  if (props.configForm.showAnswerSheet) {
    tabs.push({ id: 'answerSheet', label: '答题卡' });
  }
  if (props.configForm.showAnalysis) {
    tabs.push({ id: 'analysis', label: '参考答案' });
  }
  tabs.push({ id: 'continuous', label: '连续预览' });
  return tabs;
});

function sheetPrintClass(kind: 'body' | 'answerSheet' | 'analysis') {
  const mode = previewPageMode.value;
  const active = mode === kind || mode === 'continuous';
  return {
    'with-watermark': props.configForm.showWatermark,
    'is-print-active': active
  };
}

const showBodyPage = computed(
  () => previewPageMode.value === 'body' || previewPageMode.value === 'continuous'
);
const showAnswerSheetPage = computed(
  () => previewPageMode.value === 'answerSheet' || previewPageMode.value === 'continuous'
);
const showAnalysisPage = computed(
  () => previewPageMode.value === 'analysis' || previewPageMode.value === 'continuous'
);

</script>

<style lang="scss">
@media print {
  .export-center-page .module-page-hero,
  .template-presets-bar,
  .control-panel-card,
  .export-history-card,
  .canvas-top-bar,
  .hero-stats-row,
  .no-print {
    display: none !important;
  }

  .export-center-page,
  .main-content-layout,
  .export-split-workspace,
  .preview-canvas-card,
  .paper-scroll-wrapper,
  .paper-scaler {
    display: block !important;
    position: static !important;
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    background: transparent !important;
    box-shadow: none !important;
    border: none !important;
    transform: none !important;
    overflow: visible !important;
  }

  #printable-exam-paper {
    display: block !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    background: #ffffff !important;
  }

  #printable-exam-paper.export-paper-root {
    max-width: 178mm !important;
    margin-left: auto !important;
    margin-right: auto !important;
  }

  .simulated-sheet {
    display: flex !important;
    box-shadow: none !important;
    border: none !important;
    margin: 0 auto !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    min-height: auto !important;
    height: auto !important;
    page-break-after: always !important;
    break-after: page !important;
    box-sizing: border-box !important;
  }

  .simulated-sheet:last-child {
    page-break-after: auto !important;
    break-after: auto !important;
  }

  @page {
    size: A4 portrait;
    margin: 18mm 16mm 16mm 16mm;
  }
}
</style>

<style scoped lang="scss">
.preview-canvas-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 18px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  min-width: 0;
  width: 100%;
  box-sizing: border-box;

  .canvas-top-bar {
    margin-bottom: 14px;
  }

  .preview-toolbar {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
    padding: 8px 10px 10px;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    box-sizing: border-box;
  }

  .preview-toolbar-row {
    width: 100%;
    min-width: 0;
    box-sizing: border-box;

    &--actions {
      padding-top: 10px;
      border-top: 1px solid #e8edf2;
    }
  }

  .preview-segmented {
    display: flex;
    align-items: stretch;
    gap: 4px;
    width: 100%;
    padding: 3px;
    background: #eef2f7;
    border-radius: 9999px;
    box-sizing: border-box;

    .seg-btn {
      border: none;
      background: transparent;
      padding: 8px 10px;
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      border-radius: 9999px;
      cursor: pointer;
      white-space: nowrap;
      text-align: center;
      flex: 1 1 0;
      min-width: 0;
      transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;

      &:hover {
        color: #334155;
      }

      &.is-active {
        background: #ffffff;
        color: #1d4ed8;
        box-shadow: 0 1px 4px rgba(15, 23, 42, 0.1);
      }
    }
  }

  .preview-tools-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    align-items: center;
    width: 100%;
  }

  .tool-slot {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 0;
  }

  .zoom-chip {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    width: 100%;
    max-width: 240px;
    height: 36px;
    padding: 0 8px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;
    background: #fff;
    box-sizing: border-box;

    .chip-btn {
      width: 28px;
      height: 28px;
      border: none;
      border-radius: 50%;
      background: #f1f5f9;
      cursor: pointer;
      font-size: 14px;
      line-height: 1;
    }

    .zoom-text {
      min-width: 42px;
      text-align: center;
      font-size: 12px;
      font-weight: 600;
      color: #0f172a;
    }

    .chip-link {
      border: none;
      background: transparent;
      font-size: 12px;
      color: #2563eb;
      cursor: pointer;
      padding: 0 6px;
    }
  }

  .print-capsule-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    width: 100%;
    max-width: 200px;
    height: 36px;
    padding: 0 16px;
    border: none;
    border-radius: 9999px;
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 2px 6px rgba(5, 150, 105, 0.2);
    box-sizing: border-box;
  }

  .print-scope-select {
    width: 100%;
    max-width: 200px;

    :deep(.el-select__wrapper) {
      border-radius: 9999px;
      min-height: 36px;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
    }
  }

  @media (max-width: 720px) {
    .preview-segmented {
      flex-wrap: wrap;

      .seg-btn {
        flex: 1 1 calc(50% - 4px);
      }
    }

    .preview-tools-grid {
      grid-template-columns: 1fr;
    }
  }

  .paper-scroll-wrapper {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 12px 0;
    max-height: 820px;
    overflow-x: hidden;
    overflow-y: auto;
    display: flex;
    justify-content: center;
    min-height: 680px;
    min-width: 0;

    .paper-scaler {
      flex-shrink: 0;
      max-width: 100%;
      transition: zoom 0.15s ease;
    }

    #printable-exam-paper {
      display: flex;
      flex-direction: column;
      gap: 24px;
      max-width: 100%;

      &.font-SimSun {
        font-family: 'Times New Roman', SimSun, 'Songti SC', serif;
      }

      &.spacing-compact :deep(.mock-q-item) {
        margin-bottom: 10px;
      }

      &.spacing-relaxed :deep(.draft-answer-box) {
        min-height: 140px;
      }
    }

    .simulated-sheet {
      width: min(740px, 100%);
      max-width: 100%;
      min-height: 900px;
      box-sizing: border-box;
      background: #ffffff;
      box-shadow: none;
      border: none;
      border-bottom: 1px dashed #e2e8f0;
      padding: 36px 32px 36px 28px;
      display: flex;
      position: relative;
      color: #000;
      flex-shrink: 0;
      isolation: isolate;

      .watermark-layer {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        pointer-events: none;
        z-index: 0;
        overflow: hidden;

        .watermark-text {
          font-size: 28px;
          color: rgba(148, 163, 184, 0.18);
          transform: rotate(-28deg);
          font-weight: 700;
          white-space: nowrap;
          user-select: none;
        }
      }

      .sheet-main-content {
        flex: 1;
        min-width: 0;
        z-index: 2;
        background: rgba(255, 255, 255, 0.98);

        .sheet-header {
          text-align: center;
          margin-bottom: 14px;

          .confidential-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
            font-weight: 700;
            margin-bottom: 8px;
            min-height: 18px;

            .conf-placeholder {
              visibility: hidden;
            }

            .exam-type-badge {
              font-weight: 600;
              color: #334155;
            }
          }

          .paper-main-title {
            font-size: 21px;
            font-weight: bold;
            margin: 0 0 6px;
          }

          .paper-sub-title {
            font-size: 13.5px;
            margin: 0 0 8px;
          }

          .paper-rules-bar {
            display: flex;
            justify-content: center;
            flex-wrap: wrap;
            gap: 12px;
            font-size: 12px;
            border-top: 1.5px solid #000;
            border-bottom: 1.5px solid #000;
            padding: 4px 0;
          }
        }

        .exam-notice-box {
          border: 1px solid #000;
          padding: 10px 16px 10px 18px;
          font-size: 11px;
          margin-bottom: 12px;
          line-height: 1.55;

          .notice-title {
            font-weight: 700;
            margin-bottom: 6px;
          }

          .notice-list {
            margin: 0;
            padding-left: 1.35em;

            li {
              margin: 0 0 4px;
              padding-left: 0.25em;

              &:last-child {
                margin-bottom: 0;
              }
            }
          }
        }

        .candidate-info-box {
          font-size: 12px;
          margin-bottom: 12px;

          .paper-lite-info {
            margin: 0;
            padding: 8px 10px;
            border: 1px solid #000;
            line-height: 1.55;
            font-size: 11px;
            background: #fafafa;
          }

          .info-fill-row {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 8px;

            .gap {
              margin-left: 16px;
            }

            .fill-blank {
              display: inline-block;
              border-bottom: 1px solid #000;
              height: 16px;

              &.long {
                width: 120px;
              }

              &.mid {
                width: 80px;
              }
            }
          }

          .barcode-cells {
            display: flex;
            gap: 3px;

            .cell {
              width: 14px;
              height: 16px;
              border: 1px solid #000;
            }
          }

          .barcode-sticker-box {
            border: 1px dashed #666;
            padding: 4px 10px;
            font-size: 11px;
          }
        }

        .score-summary-table .score-grid-table {
          width: 100%;
          border-collapse: collapse;
          text-align: center;
          font-size: 12px;
          margin-bottom: 14px;
          table-layout: fixed;

          th,
          td {
            border: 1px solid #000;
            height: 26px;
            padding: 2px;
          }

          .col-label {
            width: 52px;
          }

          .col-total,
          .col-grader {
            width: 56px;
          }
        }

        .preview-error-hint {
          color: #dc2626;
          font-size: 13px;
          margin-bottom: 12px;
        }

        :deep(.draft-answer-box) {
          border: 1px dashed #94a3b8;
          min-height: 90px;
          margin-top: 8px;
          display: flex;
          align-items: flex-end;
          justify-content: flex-end;
          padding: 8px;

          .draft-tip {
            font-size: 10px;
            color: #94a3b8;
          }
        }

        .sheet-footer {
          margin-top: 24px;
          display: flex;
          justify-content: space-between;
          font-size: 11px;
          color: #64748b;
          border-top: 1px solid #e2e8f0;
          padding-top: 8px;
        }
      }
    }

    .simulated-sheet:last-child {
      border-bottom: none;
    }

    .simulated-sheet.answer-sheet-page {
      min-height: auto;
    }

    .answer-sheet-container {
      padding: 12px 8px;
    }

    .analysis-content {
      .analysis-block {
        margin-bottom: 16px;
        font-size: 13px;
        line-height: 1.75;

        h4 {
          margin: 0 0 6px;
          font-size: 14px;
        }

        .analysis-line {
          margin: 0 0 8px;
          text-align: left;
          word-break: normal;
          overflow-wrap: anywhere;

          strong {
            margin-right: 4px;
          }

          &--flow {
            line-height: 1.75;
          }

          :deep(.analysis-math),
          :deep(.math-text) {
            display: inline;
            white-space: normal;
            word-break: normal;
          }

          :deep(.katex) {
            display: inline-block;
            vertical-align: baseline;
            max-width: 100%;
          }

          :deep(.mspace.newline) {
            display: inline !important;
            padding: 0 4px !important;
          }

          :deep(.math-block) {
            display: inline-block;
            margin: 0 2px;
            vertical-align: middle;
          }
        }

        .muted {
          color: #94a3b8;
        }
      }
    }
  }
}
</style>
