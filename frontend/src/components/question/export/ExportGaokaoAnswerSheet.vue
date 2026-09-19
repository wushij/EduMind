<template>
  <div class="gaokao-answer-sheet">
    <header class="sheet-title-block">
      <p class="sheet-org-line">普通高等学校招生全国统一考试 · 答题卡样式参考（A4）</p>
      <h1 class="sheet-title">{{ title }}</h1>
      <div class="sheet-meta-row">
        <span>科目：{{ courseName || '—' }}</span>
        <span>满分：{{ totalScore }} 分</span>
      </div>
    </header>

    <!-- 卡首：手写信息 + 条形码 + 注意事项（参照各省考试院说明） -->
    <table class="header-table" cellspacing="0" cellpadding="0">
      <tbody>
        <tr>
          <td class="ht-info">
            <div class="ht-row">
              <span class="ht-label">姓名</span>
              <span class="ht-line long"></span>
            </div>
            <div class="ht-row">
              <span class="ht-label">考场号</span>
              <span class="ht-line mid"></span>
              <span class="ht-label indent">座位号</span>
              <span class="ht-line short"></span>
            </div>
          </td>
          <td class="ht-barcode" rowspan="2">
            <div class="barcode-box">
              <span>贴条形码区</span>
              <span class="barcode-sub">（由监考员发放，勿贴出虚线框）</span>
            </div>
            <div class="absent-line">
              <span>缺考标记</span>
              <span class="omr-slot absent"></span>
            </div>
          </td>
        </tr>
        <tr>
          <td class="ht-admission">
            <div class="adm-wrap">
              <div class="adm-block" :style="admBlockGridStyle">
                <span class="adm-label ht-label strong">准考证号</span>
                <span class="adm-corner" aria-hidden="true"></span>
                <span
                  v-for="col in admissionColCount"
                  :key="'hw-' + col"
                  class="handwrite-cell"
                  :style="admHandwriteCellStyle(col)"
                />
                <span
                  v-for="d in 10"
                  :key="'rl-' + d"
                  class="adm-digit-label"
                  :style="admDigitLabelStyle(d)"
                >{{ d - 1 }}</span>
                <template v-for="col in admissionColCount" :key="'omr-col-' + col">
                  <span
                    v-for="d in 10"
                    :key="'omr-' + col + '-' + d"
                    class="omr-slot adm"
                    :style="admOmrCellStyle(col, d)"
                  />
                </template>
              </div>
              <div class="fill-demo">
              <span>填涂示例</span>
              <span class="omr-slot filled"></span>
              <span>正确</span>
              <span class="omr-slot wrong"></span>
              <span>错误</span>
              <span class="demo-muted">每列仅涂一个数字</span>
              </div>
            </div>
          </td>
        </tr>
        <tr>
          <td class="ht-notice" colspan="2">
            <strong>注意事项</strong>
            <ol>
              <li>答题前，先填好姓名、准考证号、考场号、座位号，并核对条形码信息无误后再粘贴。</li>
              <li>选择题须用 2B 铅笔将选项涂满、涂黑、涂匀；非选择题须用 0.5 毫米黑色签字笔作答。</li>
              <li>不得在条形码、图像定位点（黑方块）周围作涂写和标记；保持卡面清洁，不要折叠、弄破。</li>
            </ol>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="objectiveItems.length" class="objective-section">
      <div class="scan-frame">
        <span class="scan-mark tl" aria-hidden="true"></span>
        <span class="scan-mark tr" aria-hidden="true"></span>
        <span class="scan-mark bl" aria-hidden="true"></span>
        <span class="scan-mark br" aria-hidden="true"></span>
        <h2 class="block-title">{{ sectionLabel(0) }}、选择题（用 2B 铅笔填涂）</h2>
        <table class="choice-table" cellspacing="0" cellpadding="0">
          <thead>
            <tr>
              <th class="col-no">题号</th>
              <th v-for="letter in maxOptionLetters" :key="'h-' + letter">{{ letter }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in objectiveItems" :key="'obj-' + item.index">
              <td class="col-no">{{ item.index }}</td>
              <td v-for="letter in maxOptionLetters" :key="letter">
                <span
                  v-if="optionLetters(item).includes(letter)"
                  class="omr-slot choice"
                  aria-hidden="true"
                ></span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="fillBlankItems.length" class="subjective-section">
      <div class="scan-frame subjective">
        <span class="scan-mark tl" aria-hidden="true"></span>
        <span class="scan-mark tr" aria-hidden="true"></span>
        <h2 class="block-title">{{ sectionLabel(hasObjective ? 1 : 0) }}、填空题（黑色签字笔）</h2>
        <div v-for="item in fillBlankItems" :key="'fill-' + item.index" class="fill-item">
          <span class="q-index">{{ item.index }}.</span>
          <span class="fill-line"></span>
        </div>
      </div>
    </section>

    <section v-if="shortAnswerItems.length" class="subjective-section">
      <h2 class="block-title plain">{{ sectionLabel(shortAnswerSectionIndex) }}、解答题（在矩形框内作答）</h2>
      <div
        v-for="item in shortAnswerItems"
        :key="'short-' + item.index"
        class="answer-frame-block scan-frame subjective"
      >
        <span class="scan-mark tl" aria-hidden="true"></span>
        <span class="scan-mark tr" aria-hidden="true"></span>
        <span class="scan-mark bl" aria-hidden="true"></span>
        <span class="scan-mark br" aria-hidden="true"></span>
        <div class="frame-head">
          <span>{{ item.index }} 题</span>
          <span>评卷人</span>
          <span class="score-box"></span>
          <span>得分</span>
          <span class="score-box wide"></span>
        </div>
        <div class="answer-frame">
          <p class="frame-tip">超出本题黑色边框区域的答案无效</p>
        </div>
      </div>
    </section>

    <footer class="sheet-footer-row">
      <span>版式参考：新高考 A4 答题卡（湖北/天津等省考试院公开说明）</span>
      <span>正面</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export interface AnswerSheetQuestionItem {
  index: number;
  type: string;
  optionCount?: number;
}

const props = withDefaults(
  defineProps<{
    title: string;
    courseName?: string;
    totalScore?: number;
    objectiveItems?: AnswerSheetQuestionItem[];
    fillBlankItems?: AnswerSheetQuestionItem[];
    shortAnswerItems?: AnswerSheetQuestionItem[];
    admissionDigits?: number;
  }>(),
  {
    courseName: '',
    totalScore: 100,
    objectiveItems: () => [],
    fillBlankItems: () => [],
    shortAnswerItems: () => [],
    admissionDigits: 10
  }
);

const admissionColCount = computed(() => Math.max(8, Math.min(props.admissionDigits, 12)));

/** 浏览器对 repeat(var(--n)) 支持不稳定，列数用内联 grid-template-columns */
const ADM_FIRST_DATA_COL = 3;

const admBlockGridStyle = computed(() => {
  const n = admissionColCount.value;
  return {
    gridTemplateColumns: `auto 11px repeat(${n}, 14px)`,
    gridTemplateRows: `18px repeat(10, 8px)`
  };
});

function admDataGridColumn(colIndex: number): number {
  return ADM_FIRST_DATA_COL + colIndex - 1;
}

function admHandwriteCellStyle(col: number) {
  return {
    gridColumn: admDataGridColumn(col),
    gridRow: 1
  };
}

/** 行号 0–9 各占网格第 2–11 行，与填涂格同一行 */
function admDigitLabelStyle(digitRow: number) {
  return {
    gridColumn: 2,
    gridRow: digitRow + 1
  };
}

function admOmrCellStyle(col: number, digitRow: number) {
  return {
    gridColumn: admDataGridColumn(col),
    gridRow: digitRow + 1
  };
}

const hasObjective = computed(() => props.objectiveItems.length > 0);
const hasFill = computed(() => props.fillBlankItems.length > 0);

const shortAnswerSectionIndex = computed(() => {
  let idx = 0;
  if (hasObjective.value) idx += 1;
  if (hasFill.value) idx += 1;
  return idx;
});

const maxOptionCount = computed(() => {
  if (!props.objectiveItems.length) return 4;
  return Math.min(
    Math.max(...props.objectiveItems.map((i) => Math.min(Math.max(i.optionCount ?? 4, 2), 7))),
    7
  );
});

const maxOptionLetters = computed(() =>
  Array.from({ length: maxOptionCount.value }, (_, i) => String.fromCharCode(65 + i))
);

const CHINESE_NUM = ['一', '二', '三', '四', '五', '六', '七', '八'];

function sectionLabel(index: number): string {
  return CHINESE_NUM[index] ?? String(index + 1);
}

function optionLetters(item: AnswerSheetQuestionItem): string[] {
  const n = Math.min(Math.max(item.optionCount ?? 4, 2), 7);
  return Array.from({ length: n }, (_, i) => String.fromCharCode(65 + i));
}
</script>

<style scoped lang="scss">
.gaokao-answer-sheet {
  font-family: SimSun, 'Songti SC', serif;
  font-size: 12px;
  color: #000;
  line-height: 1.45;
  width: 100%;
  box-sizing: border-box;
}

.sheet-title-block {
  text-align: center;
  margin-bottom: 8px;

  .sheet-org-line {
    margin: 0 0 4px;
    font-size: 10px;
    color: #333;
  }

  .sheet-title {
    font-size: 16px;
    font-weight: 700;
    margin: 0 0 6px;
  }

  .sheet-meta-row {
    display: flex;
    justify-content: center;
    gap: 20px;
    font-size: 11px;
    border-top: 1px solid #000;
    border-bottom: 1px solid #000;
    padding: 4px 0;
  }
}

.header-table {
  width: 100%;
  border-collapse: collapse;
  border: 1.5px solid #000;
  margin-bottom: 10px;
  table-layout: fixed;

  td {
    border: 1px solid #000;
    vertical-align: top;
    padding: 5px 6px;
  }

  .ht-barcode {
    width: 128px;
    text-align: center;
  }

  .ht-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }

  .ht-label {
    flex-shrink: 0;
    font-size: 11px;

    &.strong {
      font-weight: 700;
    }

    &.indent {
      margin-left: 10px;
    }
  }

  .ht-line {
    display: inline-block;
    border-bottom: 1px solid #000;
    height: 14px;

    &.long {
      flex: 1;
    }

    &.mid {
      width: 48px;
    }

    &.short {
      width: 36px;
    }
  }

  .barcode-box {
    min-height: 52px;
    border: 1px dashed #666;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-size: 9px;
    padding: 4px;

    .barcode-sub {
      font-size: 8px;
      color: #555;
      margin-top: 2px;
    }
  }

  .absent-line {
    margin-top: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 10px;
  }

  .ht-admission {
    padding-left: 4px;
    padding-right: 6px;
  }

  /* 准考证整体靠右，贴近条形码列（各省 A 卡常规版式） */
  .adm-wrap {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    width: fit-content;
    max-width: 100%;
    margin-left: auto;
  }

  /* 准考证：标签 | 行号 | N 列（第 1 行写手格，第 2–11 行与左侧 0–9 对齐填涂） */
  .adm-block {
    --adm-col-w: 14px;
    --adm-col-gap: 4px;
    --adm-digit-col-w: 11px;
    --adm-omr-row-h: 8px;
    display: grid;
    column-gap: var(--adm-col-gap);
    row-gap: 0;
    align-items: start;
    width: fit-content;
  }

  .adm-label {
    grid-column: 1;
    grid-row: 1;
    align-self: end;
    padding-bottom: 1px;
    white-space: nowrap;
  }

  .adm-corner {
    grid-column: 2;
    grid-row: 1;
  }

  .adm-digit-label {
    width: var(--adm-digit-col-w);
    height: var(--adm-omr-row-h);
    line-height: var(--adm-omr-row-h);
    font-size: 8px;
    text-align: right;
    justify-self: end;
    align-self: center;
    box-sizing: border-box;
  }

  .handwrite-cell {
    width: var(--adm-col-w);
    height: 17px;
    border: 1px solid #000;
    box-sizing: border-box;
    justify-self: center;
    align-self: end;
  }

  .fill-demo {
    margin-top: 4px;
    width: 100%;
    justify-content: flex-end;
    font-size: 8.5px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px;

    .demo-muted {
      color: #444;
    }
  }

  .ht-notice {
    font-size: 9px;
    padding: 8px 12px 8px 14px !important;

    ol {
      margin: 4px 0 0;
      padding-left: 1.35em;

      li {
        padding-left: 0.2em;
      }
    }
  }
}

.omr-slot {
  display: inline-block;
  width: 12px;
  height: 7px;
  border: 1px solid #000;
  border-radius: 1px;
  background: #fff;
  box-sizing: border-box;

  &.adm {
    width: var(--adm-col-w, 14px);
    height: calc(var(--adm-omr-row-h, 8px) - 1px);
    flex-shrink: 0;
    justify-self: center;
    align-self: center;
  }

  &.choice {
    width: 14px;
    height: 8px;
  }

  &.filled {
    background: #000;
  }

  &.wrong {
    position: relative;

    &::after {
      content: '';
      position: absolute;
      left: 1px;
      right: 1px;
      top: 50%;
      height: 1px;
      background: #000;
    }
  }

  &.absent {
    width: 14px;
    height: 9px;
    border-radius: 50%;
  }
}

.scan-frame {
  position: relative;
  border: 1px solid #000;
  padding: 8px 10px 10px;
  margin-bottom: 8px;

  &.subjective {
    padding-top: 10px;
  }
}

.scan-mark {
  position: absolute;
  width: 10px;
  height: 10px;
  background: #000;

  &.tl {
    top: -1px;
    left: -1px;
  }

  &.tr {
    top: -1px;
    right: -1px;
  }

  &.bl {
    bottom: -1px;
    left: -1px;
  }

  &.br {
    bottom: -1px;
    right: -1px;
  }
}

.block-title {
  font-size: 11px;
  font-weight: 700;
  text-align: center;
  margin: 0 0 8px;
  padding: 2px 0;

  &.plain {
    border: 1px solid #000;
    background: #f5f5f5;
    padding: 4px;
    margin-bottom: 6px;
  }
}

.choice-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 10px;

  th,
  td {
    border: 1px solid #999;
    text-align: center;
    padding: 3px 2px;
    height: 22px;
  }

  .col-no {
    width: 36px;
    font-weight: 700;
  }

  thead th {
    background: #f3f4f6;
    font-weight: 700;
  }
}

.subjective-section {
  margin-bottom: 8px;
}

.fill-item {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 10px;

  .q-index {
    font-weight: 700;
  }

  .fill-line {
    flex: 1;
    border-bottom: 1px solid #000;
    height: 18px;
  }
}

.answer-frame-block {
  border: none;
  padding: 0;
  margin-bottom: 10px;

  .frame-head {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 8px;
    border-bottom: 1px solid #000;
    font-size: 10px;
    background: #fafafa;
  }

  .score-box {
    display: inline-block;
    width: 28px;
    height: 16px;
    border: 1px solid #000;

    &.wide {
      width: 46px;
    }
  }

  .answer-frame {
    min-height: 80px;
    padding: 8px;
    background: repeating-linear-gradient(
      to bottom,
      transparent,
      transparent 22px,
      #e8e8e8 22px,
      #e8e8e8 23px
    );

    .frame-tip {
      margin: 0;
      font-size: 9px;
      color: #666;
    }
  }
}

.sheet-footer-row {
  display: flex;
  justify-content: space-between;
  font-size: 8.5px;
  color: #555;
  border-top: 1px solid #999;
  padding-top: 4px;
  margin-top: 6px;
}
</style>
