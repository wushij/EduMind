<template>
  <div class="ocr-diff-viewer">
    <div class="diff-header-bar">
      <div class="diff-summary-info">
        <span class="diff-tag diff-tag--raw">原始 OCR 识别</span>
        <span class="diff-arrow">→</span>
        <span class="diff-tag diff-tag--proof">当前校对文本</span>
        <span class="diff-stats">已对比 {{ rawLines.length }} 行文本，存在 {{ diffCount }} 处差异</span>
      </div>

      <div class="diff-actions">
        <el-button size="small" @click="$emit('reset')">
          还原为原始识别
        </el-button>
      </div>
    </div>

    <div class="diff-columns-grid">
      <!-- 左列：原始 OCR 识别内容 -->
      <div class="diff-column diff-column--left">
        <div class="col-head">
          <span class="title">OCR 原始识别内容 (只读参照)</span>
        </div>
        <div class="lines-scroll-box">
          <div
            v-for="(line, idx) in rawLines"
            :key="idx"
            class="diff-line-row"
            :class="{ 'is-different': line !== proofreadLines[idx] }"
          >
            <span class="line-num">{{ idx + 1 }}</span>
            <span class="line-content">{{ line || ' ' }}</span>
          </div>
        </div>
      </div>

      <!-- 右列：当前校对后文本 -->
      <div class="diff-column diff-column--right">
        <div class="col-head">
          <span class="title">当前校对后内容 (实时结果)</span>
        </div>
        <div class="lines-scroll-box">
          <div
            v-for="(line, idx) in proofreadLines"
            :key="idx"
            class="diff-line-row"
            :class="{ 'is-different': line !== rawLines[idx] }"
          >
            <span class="line-num">{{ idx + 1 }}</span>
            <span class="line-content">{{ line || ' ' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  rawText: string;
  proofreadText: string;
}>();

defineEmits<{
  'reset': [];
}>();

const rawLines = computed(() => (props.rawText || '').split('\n'));
const proofreadLines = computed(() => (props.proofreadText || '').split('\n'));

const diffCount = computed(() => {
  let count = 0;
  const maxLen = Math.max(rawLines.value.length, proofreadLines.value.length);
  for (let i = 0; i < maxLen; i++) {
    if (rawLines.value[i] !== proofreadLines.value[i]) {
      count++;
    }
  }
  return count;
});
</script>

<style scoped lang="scss">
.ocr-diff-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #FFFFFF;
  border-radius: 8px;
  overflow: hidden;

  .diff-header-bar {
    padding: 10px 16px;
    background: #F8FAFC;
    border-bottom: 1px solid #E2E8F0;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .diff-summary-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;

      .diff-tag {
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: 600;

        &--raw {
          background: #FEE2E2;
          color: #DC2626;
        }

        &--proof {
          background: #DCFCE7;
          color: #16A34A;
        }
      }

      .diff-arrow {
        color: #94A3B8;
        font-weight: 700;
      }

      .diff-stats {
        color: #64748B;
        margin-left: 6px;
      }
    }
  }

  .diff-columns-grid {
    flex: 1;
    display: grid;
    grid-template-columns: 1fr 1fr;
    min-height: 0;
    overflow: hidden;

    .diff-column {
      display: flex;
      flex-direction: column;
      min-height: 0;

      &--left {
        border-right: 1px solid #E2E8F0;
        background: #FAFAFA;
      }

      &--right {
        background: #FFFFFF;
      }

      .col-head {
        padding: 8px 14px;
        background: #F1F5F9;
        border-bottom: 1px solid #E2E8F0;
        font-size: 12px;
        font-weight: 600;
        color: #475569;
      }

      .lines-scroll-box {
        flex: 1;
        overflow-y: auto;
        font-family: 'Consolas', 'Courier New', monospace;
        font-size: 12px;
        line-height: 1.6;

        .diff-line-row {
          display: flex;
          align-items: flex-start;
          padding: 2px 8px;
          min-height: 22px;
          border-left: 3px solid transparent;

          .line-num {
            width: 32px;
            color: #94A3B8;
            font-size: 11px;
            text-align: right;
            padding-right: 10px;
            user-select: none;
            flex-shrink: 0;
          }

          .line-content {
            flex: 1;
            white-space: pre-wrap;
            word-break: break-all;
            color: #1E293B;
          }

          &.is-different {
            background: #FEF3C7;
            border-left-color: #F59E0B;

            .line-content {
              color: #92400E;
              font-weight: 500;
            }
          }
        }
      }
    }
  }
}
</style>
