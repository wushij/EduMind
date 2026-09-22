<template>
  <aside class="ocr-thumbnail-bar">
    <div class="bar-header">
      <div class="header-title">
        <el-icon><Tickets /></el-icon>
        <span>试卷页码大纲 ({{ pages.length }})</span>
      </div>
      <button type="button" class="close-btn" title="收起大纲" @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <div class="thumbnails-list-scroll">
      <div
        v-for="(page, idx) in pages"
        :key="idx"
        class="thumbnail-card"
        :class="{ active: idx === activePageIdx }"
        @click="$emit('select-page', idx)"
      >
        <div class="card-meta-row">
          <span class="page-no-badge">P{{ idx + 1 }}</span>
          <span class="confidence-tag" :class="getConfidenceClass(page.confidenceScore)">
            {{ page.confidenceScore || 98.5 }}%
          </span>
        </div>

        <div class="card-preview-surface">
          <!-- 模拟缩略微页面 -->
          <div class="mini-paper-mock">
            <div class="mock-line mock-line--title" />
            <div class="mock-line" />
            <div class="mock-line mock-line--short" />
            <div class="mock-formula-box">$$ f(x) $$</div>
            <div class="mock-line" />
          </div>
        </div>

        <div class="card-footer-info">
          <span class="info-item">共 {{ page.blocks?.length || 0 }} 处切片</span>
          <span v-if="page.proofreadText && page.proofreadText !== page.rawText" class="edited-flag">已校对</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { Tickets, Close } from '@element-plus/icons-vue';
import type { WorkspacePage } from '@/composables/knowledge/useOcrWorkspace';

defineProps<{
  pages: WorkspacePage[];
  activePageIdx: number;
}>();

defineEmits<{
  'select-page': [idx: number];
  'close': [];
}>();

function getConfidenceClass(score?: number): string {
  if (!score || score >= 98.5) return 'is-high';
  if (score >= 95.0) return 'is-medium';
  return 'is-low';
}
</script>

<style scoped lang="scss">
.ocr-thumbnail-bar {
  width: 170px;
  background: #FFFFFF;
  border-right: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100%;
  box-sizing: border-box;

  .bar-header {
    padding: 10px 12px;
    border-bottom: 1px solid #F1F5F9;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-title {
      font-size: 12px;
      font-weight: 600;
      color: #334155;
      display: flex;
      align-items: center;
      gap: 5px;
    }

    .close-btn {
      border: none;
      background: transparent;
      color: #94A3B8;
      cursor: pointer;
      font-size: 14px;
      padding: 2px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        color: #0F172A;
        background: #F1F5F9;
      }
    }
  }

  .thumbnails-list-scroll {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .thumbnail-card {
      background: #F8FAFC;
      border: 1.5px solid #E2E8F0;
      border-radius: 8px;
      padding: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      flex-direction: column;
      gap: 6px;

      &:hover {
        border-color: #93C5FD;
        background: #F0F7FF;
        transform: translateY(-1px);
        box-shadow: 0 4px 10px rgba(37, 99, 235, 0.06);
      }

      &.active {
        border-color: #2563EB;
        background: #EFF6FF;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);

        .page-no-badge {
          background: #2563EB;
          color: #FFFFFF;
        }
      }

      .card-meta-row {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .page-no-badge {
          font-size: 11px;
          font-weight: 700;
          color: #334155;
          background: #E2E8F0;
          padding: 1px 6px;
          border-radius: 4px;
          transition: all 0.15s ease;
        }

        .confidence-tag {
          font-size: 10px;
          font-weight: 600;
          padding: 1px 4px;
          border-radius: 3px;

          &.is-high {
            color: #15803D;
            background: #DCFCE7;
          }

          &.is-medium {
            color: #B45309;
            background: #FEF3C7;
          }

          &.is-low {
            color: #B91C1C;
            background: #FEE2E2;
          }
        }
      }

      .card-preview-surface {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 4px;
        padding: 6px;
        height: 70px;
        box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.03);
        display: flex;
        align-items: center;
        justify-content: center;

        .mini-paper-mock {
          width: 100%;
          display: flex;
          flex-direction: column;
          gap: 4px;

          .mock-line {
            height: 4px;
            background: #E2E8F0;
            border-radius: 2px;
            width: 100%;

            &--title {
              background: #CBD5E1;
              width: 70%;
              margin-bottom: 2px;
            }

            &--short {
              width: 50%;
            }
          }

          .mock-formula-box {
            font-size: 8px;
            font-family: serif;
            color: #2563EB;
            background: #F1F5F9;
            padding: 1px 3px;
            border-radius: 2px;
            text-align: center;
            line-height: 1.2;
          }
        }
      }

      .card-footer-info {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 10px;
        color: #64748B;

        .edited-flag {
          color: #7C3AED;
          font-weight: 600;
          background: #F3E8FF;
          padding: 0 4px;
          border-radius: 2px;
        }
      }
    }
  }
}
</style>
