<template>
  <div class="chunks-management-page">
    <!-- 1. 顶部切片工程与指标概览看板 -->
    <div class="stats-overview-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper icon-blue">
          <el-icon><Grid /></el-icon>
        </div>
        <div class="stat-details">
          <span class="stat-label">总切片数 (Chunks)</span>
          <span class="stat-number">{{ stats.totalChunks }}</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper icon-green">
          <el-icon><Check /></el-icon>
        </div>
        <div class="stat-details">
          <span class="stat-label">已完成向量化入库</span>
          <span class="stat-number text-green">{{ stats.indexedChunks }}</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper icon-purple">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-details">
          <span class="stat-label">单切片平均 Token</span>
          <span class="stat-number">{{ stats.avgTokens }} <span class="unit">toks</span></span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper icon-amber">
          <el-icon><Refresh /></el-icon>
        </div>
        <div class="stat-details">
          <span class="stat-label">待索引 / 异常切片</span>
          <span class="stat-number" :class="{ 'text-amber': stats.failedChunks > 0 }">
            {{ stats.pendingChunks + stats.failedChunks }}
          </span>
        </div>
      </div>
    </div>

    <!-- 2. 控制台与检索过滤栏 -->
    <div class="filter-panel-card">
      <div class="filter-left">
        <!-- 所属课件筛选 -->
        <el-select
          v-model="selectedDocumentId"
          placeholder="选择课件文档"
          clearable
          style="width: 260px"
          @change="fetchChunks"
        >
          <el-option
            v-for="doc in documents"
            :key="doc.id"
            :value="doc.id"
            :label="(doc as any).fileName || doc.name"
          />
        </el-select>

        <!-- 状态单选胶囊 -->
        <el-radio-group v-model="statusFilter" size="default" @change="fetchChunks">
          <el-radio-button label="ALL">全部切片</el-radio-button>
          <el-radio-button label="INDEXED">已向量化</el-radio-button>
          <el-radio-button label="PENDING">处理中</el-radio-button>
          <el-radio-button label="INDEX_FAILED">索引异常</el-radio-button>
        </el-radio-group>

        <!-- 关键词搜索 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索分块内容或大纲标题..."
          style="width: 240px"
          clearable
          :prefix-icon="Search"
          @keyup.enter="fetchChunks"
          @clear="fetchChunks"
        />
      </div>

      <div class="filter-right">
        <el-button
          type="primary"
          :icon="RefreshRight"
          :loading="rechunking"
          @click="handleRechunk()"
        >
          重新切片此文档
        </el-button>
      </div>
    </div>

    <!-- 3. 切片内容网格 -->
    <div v-loading="loading" class="chunks-content-area">
      <div v-if="paginatedChunks.length > 0" class="chunks-grid">
        <div
          v-for="chunk in paginatedChunks"
          :key="chunk.id"
          class="chunk-card"
          @click="openViewer(chunk)"
        >
          <div class="chunk-card-header">
            <div class="chunk-index-tag">#{{ chunk.chunkIndex }}</div>
            <div class="chunk-badges">
              <span class="badge page-badge" v-if="chunk.pageNo">P.{{ chunk.pageNo }}</span>
              <span class="badge token-badge">{{ chunk.tokenCount }} tok</span>
              <span
                class="status-dot"
                :class="chunk.status.toLowerCase()"
                :title="chunk.status === 'INDEXED' ? '已向量化' : '未就绪'"
              ></span>
            </div>
          </div>

          <div v-if="chunk.heading" class="chunk-heading" :title="chunk.heading">
            <el-icon><CollectionTag /></el-icon>
            <span>{{ chunk.heading }}</span>
          </div>

          <p class="chunk-text-snippet">
            {{ chunk.content }}
          </p>

          <div class="chunk-card-footer">
            <span class="doc-label" :title="chunk.documentName">
              {{ chunk.documentName || `文档 #${chunk.documentId}` }}
            </span>
            <span class="inspect-hint">查看详情 →</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-chunks-wrapper">
        <el-empty description="当前筛选条件下没有检索到分块数据">
          <template #extra>
            <el-button type="primary" plain @click="resetFilters">重置筛选条件</el-button>
          </template>
        </el-empty>
      </div>

      <!-- 分页栏 -->
      <AppPagination
        v-model:page-num="currentPage"
        v-model:page-size="pageSize"
        :total="totalCount"
        :page-sizes="[8, 16, 24]"
        @change="() => {}"
      />
    </div>

    <!-- 4. 切片详情抽屉 -->
    <ChunkViewer
      :visible="viewerVisible"
      :chunk="currentChunk"
      @update:visible="viewerVisible = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useDocumentChunk } from '@/composables/knowledge/useDocumentChunk';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import ChunkViewer from '@/components/knowledge/ChunkViewer.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import {
  Grid,
  Check,
  DataAnalysis,
  Refresh,
  Search,
  RefreshRight,
  CollectionTag
} from '@element-plus/icons-vue';

const {
  loading,
  rechunking,
  stats,
  selectedDocumentId,
  searchKeyword,
  statusFilter,
  currentChunk,
  viewerVisible,
  currentPage,
  pageSize,
  paginatedChunks,
  totalCount,
  documents,
  fetchChunks,
  initializeChunksPage,
  resetFilters,
  handleRechunk,
  openViewer
} = useDocumentChunk();

const { kbId } = useKnowledgeRoute();

onMounted(async () => {
  await initializeChunksPage(kbId.value);
});
</script>

<style scoped lang="scss">
.chunks-management-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  /* 指标概览卡片 */
  .stats-overview-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    @media (max-width: 1300px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }

    .stat-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 18px; // 保持优雅圆角卡片，绝不会在屏幕狭窄时被挤压成竖向椭圆
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.04);
      transition: all 0.2s ease;
      min-width: 0;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(30, 80, 150, 0.08);
      }

      .stat-icon-wrapper {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;

        &.icon-blue {
          background: #EFF6FF;
          color: #2563EB;
        }
        &.icon-green {
          background: #F0FDF4;
          color: #16A34A;
        }
        &.icon-purple {
          background: #FAF5FF;
          color: #9333EA;
        }
        &.icon-amber {
          background: #FFFBEB;
          color: #D97706;
        }
      }

      .stat-details {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;

        .stat-label {
          font-size: 13px;
          color: #64748B;
          font-weight: 500;
          white-space: nowrap; // 严禁单个字换行
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .stat-number {
          font-size: 22px;
          font-weight: 700;
          color: #1E293B;
          white-space: nowrap;

          &.text-green { color: #16A34A; }
          &.text-amber { color: #D97706; }

          .unit {
            font-size: 12px;
            font-weight: normal;
            color: #94A3B8;
          }
        }
      }
    }
  }

  /* 过滤控制栏 */
  .filter-panel-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 16px; // 保持卡片轮廓规整
    padding: 12px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

    .filter-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      :deep(.el-radio-group) {
        background: #F1F5F9;
        padding: 3px;
        border-radius: 9999px; // 内部操作项使用长圆胶囊
        border: 1px solid #E2E8F0;

        .el-radio-button {
          .el-radio-button__inner {
            border-radius: 9999px !important; // 长圆单选选项
            border: none !important;
            background: transparent;
            color: #64748B;
            font-size: 13px;
            padding: 6px 16px;
            box-shadow: none !important;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

            &:hover {
              color: #1677FF;
            }
          }

          &.is-active .el-radio-button__inner {
            background: #1677FF !important;
            color: #FFFFFF !important;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28) !important;
          }
        }
      }
    }
  }

  /* 切片展示网格 */
  .chunks-content-area {
    min-height: 420px;

    .chunks-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 14px;

      @media (max-width: 1024px) {
        grid-template-columns: 1fr;
      }

      .chunk-card {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 18px;
        padding: 16px 18px;
        cursor: pointer;
        display: flex;
        flex-direction: column;
        gap: 10px;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;

        &:hover {
          border-color: #93C5FD;
          box-shadow: 0 6px 20px rgba(37, 99, 235, 0.08);
          transform: translateY(-2px);

          .inspect-hint {
            color: #2563EB;
          }
        }

        .chunk-card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .chunk-index-tag {
            font-family: ui-monospace, monospace;
            font-size: 12px;
            font-weight: 700;
            color: #2563EB;
            background: #EFF6FF;
            padding: 2px 8px;
            border-radius: 6px;
          }

          .chunk-badges {
            display: flex;
            align-items: center;
            gap: 6px;

            .badge {
              font-size: 11px;
              padding: 2px 6px;
              border-radius: 4px;
              font-family: ui-monospace, monospace;

              &.page-badge {
                background: #F1F5F9;
                color: #475569;
              }

              &.token-badge {
                background: #F8FAFC;
                color: #64748B;
                border: 1px solid #E2E8F0;
              }
            }

            .status-dot {
              width: 8px;
              height: 8px;
              border-radius: 50%;

              &.indexed { background: #16A34A; }
              &.pending { background: #EAB308; }
              &.index_failed { background: #EF4444; }
            }
          }
        }

        .chunk-heading {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          font-weight: 600;
          color: #0F172A;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .chunk-text-snippet {
          margin: 0;
          font-size: 13px;
          color: #475569;
          line-height: 1.65;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
          text-overflow: ellipsis;
          background: #F8FAFC;
          padding: 10px 12px;
          border-radius: 8px;
        }

        .chunk-card-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 12px;
          color: #94A3B8;
          border-top: 1px solid #F1F5F9;
          padding-top: 8px;

          .doc-label {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 70%;
          }

          .inspect-hint {
            font-weight: 500;
            transition: color 0.2s;
          }
        }
      }
    }

    .empty-chunks-wrapper {
      background: #FFFFFF;
      border-radius: 20px;
      padding: 60px 20px;
      border: 1px dashed #CBD5E1;
    }

    .pagination-footer {
      display: flex;
      justify-content: flex-end;
      margin-top: 20px;
      background: #FFFFFF;
      padding: 10px 20px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
    }
  }
}
</style>
