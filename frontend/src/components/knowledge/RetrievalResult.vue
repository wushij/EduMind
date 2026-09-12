<template>
  <div class="retrieval-results-list">
    <div
      v-for="(item, index) in items"
      :key="item.id"
      class="retrieval-result-card"
    >
      <div class="card-top-row">
        <div class="rank-and-source">
          <span class="rank-badge">#{{ index + 1 }}</span>
          <span class="chunk-index-tag">切片 #{{ item.chunkIndex }}</span>
          <span class="doc-title" :title="item.documentName">{{ item.documentName }}</span>
          <span v-if="item.pageNo" class="page-badge">P.{{ item.pageNo }}</span>
        </div>

        <!-- 相似度置信度胶囊 -->
        <div class="score-pill" :class="getScoreClass(item.score)">
          <span class="score-label">相似度</span>
          <span class="score-val">{{ (item.score * 100).toFixed(1) }}%</span>
        </div>
      </div>

      <!-- 大纲标题 -->
      <div v-if="item.heading" class="heading-row">
        <el-icon class="heading-icon"><CollectionTag /></el-icon>
        <span class="heading-text">{{ item.heading }}</span>
      </div>

      <!-- 切片正文摘录 -->
      <div class="content-box">
        {{ item.content }}
      </div>

      <!-- 底部匹配关键词与操作 -->
      <div class="card-footer-row">
        <div class="matched-keywords">
          <span v-if="item.matchedKeywords && item.matchedKeywords.length > 0" class="kw-label">命中实体：</span>
          <el-tag
            v-for="kw in item.matchedKeywords"
            :key="kw"
            size="small"
            type="info"
            class="kw-tag"
          >
            {{ kw }}
          </el-tag>
        </div>
        <el-button
          link
          type="primary"
          size="small"
          :icon="CopyDocument"
          @click="copySnippet(item.content)"
        >
          复制摘录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RetrievalResultItem } from '@/types/knowledge/rag';
import { CollectionTag, CopyDocument } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

defineProps<{
  items: RetrievalResultItem[];
}>();

const getScoreClass = (score: number) => {
  if (score >= 0.85) return 'score-emerald';
  if (score >= 0.70) return 'score-blue';
  return 'score-amber';
};

const copySnippet = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success('切片摘录已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败');
  }
};
</script>

<style scoped lang="scss">
.retrieval-results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .retrieval-result-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 14px 16px;
    transition: all 0.2s ease;

    &:hover {
      border-color: #93C5FD;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.06);
    }

    .card-top-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .rank-and-source {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;

        .rank-badge {
          font-weight: 700;
          font-size: 13px;
          color: #2563EB;
          background: #EFF6FF;
          padding: 2px 8px;
          border-radius: 4px;
        }

        .chunk-index-tag {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 6px;
          border-radius: 4px;
        }

        .doc-title {
          font-size: 13px;
          font-weight: 600;
          color: #1E293B;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 320px;
        }

        .page-badge {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          background: #FEF3C7;
          color: #B45309;
          padding: 2px 6px;
          border-radius: 4px;
        }
      }

      .score-pill {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 3px 10px;
        border-radius: 12px;
        font-family: ui-monospace, monospace;

        .score-label {
          font-size: 10.5px;
          opacity: 0.85;
        }

        .score-val {
          font-size: 13px;
          font-weight: 700;
        }

        &.score-emerald {
          background: #ECFDF5;
          color: #059669;
          border: 1px solid #A7F3D0;
        }

        &.score-blue {
          background: #EFF6FF;
          color: #2563EB;
          border: 1px solid #BFDBFE;
        }

        &.score-amber {
          background: #FFFBEB;
          color: #D97706;
          border: 1px solid #FDE68A;
        }
      }
    }

    .heading-row {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 8px;

      .heading-icon {
        color: #3B82F6;
        font-size: 13px;
      }

      .heading-text {
        font-size: 13px;
        font-weight: 600;
        color: #334155;
      }
    }

    .content-box {
      background: #F8FAFC;
      border: 1px solid #F1F5F9;
      border-radius: 8px;
      padding: 12px 14px;
      font-size: 13px;
      line-height: 1.65;
      color: #334155;
      margin-bottom: 10px;
    }

    .card-footer-row {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .matched-keywords {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;

        .kw-label {
          font-size: 11px;
          color: #94A3B8;
        }

        .kw-tag {
          font-size: 11px;
        }
      }
    }
  }
}
</style>
