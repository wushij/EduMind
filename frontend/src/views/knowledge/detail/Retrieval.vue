<template>
  <div class="retrieval-sandbox-page">
    <!-- 1. 检索控制与配置面板 -->
    <div class="sandbox-config-card">
      <div class="config-header">
        <div class="left-title">
          <el-icon class="icon"><Search /></el-icon>
          <span class="title">向量检索测试沙箱 (Vector Retrieval Sandbox)</span>
        </div>
      </div>

      <div class="query-input-row">
        <el-input
          v-model="query"
          type="textarea"
          :rows="2"
          placeholder="请输入测试问题或语义检索词句..."
          class="custom-textarea"
          @keydown.enter.prevent="runRetrieval"
        />
      </div>

      <!-- 参数调节与检索操作栏 -->
      <div class="parameters-row">
        <div class="params-left">
          <div class="param-item">
            <span class="param-label">召回数量 (Top-K): <strong>{{ topK }}</strong></span>
            <el-slider v-model="topK" :min="1" :max="15" :step="1" style="width: 130px" />
          </div>

          <div class="param-item">
            <span class="param-label">相似度阈值 (Threshold): <strong>{{ scoreThreshold.toFixed(2) }}</strong></span>
            <el-slider v-model="scoreThreshold" :min="0.30" :max="0.95" :step="0.05" style="width: 130px" />
          </div>
        </div>

        <div class="params-right">
          <div class="param-item switch-item">
            <span class="param-label">混合检索 (Hybrid)</span>
            <el-tooltip content="沙箱专注于 Milvus 稠密向量语义检索；全链路混合检索（BM25 + 向量召回 + RRF 融合重排）已在完整 RAG 链路中默认生效">
              <el-switch
                v-model="hybridSearch"
                disabled
                active-text="混合检索"
                inactive-text="仅向量模式"
              />
            </el-tooltip>
          </div>

          <div class="action-item">
            <el-button
              type="primary"
              size="default"
              :icon="Search"
              :loading="retrievalLoading"
              class="search-btn"
              @click="runRetrieval"
            >
              执行语义检索
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 检索结果展示区域 -->
    <div v-loading="retrievalLoading" class="results-container">
      <div v-if="retrievalResults.length > 0" class="results-header-info">
        <span class="res-count">召回结果：共 <strong>{{ retrievalResults.length }}</strong> 个相关切片</span>
        <span class="res-tip">结果已按 Milvus 余弦相似度由高至低重排</span>
      </div>

      <RetrievalResult
        v-if="retrievalResults.length > 0"
        :items="retrievalResults"
      />

      <div v-else-if="!retrievalLoading" class="empty-state-box">
        <el-empty description="在上方输入测试问题并点击【执行语义检索】，验证知识库语义召回精度" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRAG } from '@/composables/knowledge/useRAG';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import RetrievalResult from '@/components/knowledge/RetrievalResult.vue';
import { Search } from '@element-plus/icons-vue';

const { kbId } = useKnowledgeRoute();

const {
  query,
  topK,
  scoreThreshold,
  hybridSearch,
  retrievalLoading,
  retrievalResults,
  runRetrieval
} = useRAG(kbId);

</script>

<style scoped lang="scss">
.retrieval-sandbox-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .sandbox-config-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 18px 20px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

    .config-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;

      .left-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 700;
        color: #1E293B;

        .icon {
          color: #2563EB;
          font-size: 17px;
        }
      }
    }

    .parameters-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 10px;
      padding: 10px 16px;

      .params-left {
        display: flex;
        align-items: center;
        gap: 20px;
        flex-wrap: wrap;
      }

      .params-right {
        display: flex;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
        margin-left: auto;
      }

      .param-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .param-label {
          font-size: 12px;
          color: #475569;
          white-space: nowrap;

          strong {
            color: #2563EB;
          }
        }
      }

      .action-item {
        .search-btn {
          height: 34px;
          padding: 0 18px;
          border-radius: 999px !important;
          background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%) !important;
          border: 1px solid transparent !important;
          color: #FFFFFF !important;
          font-size: 13px;
          font-weight: 600;
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.28);
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover:not(:disabled) {
            background: linear-gradient(135deg, #0958D9 0%, #2563EB 100%) !important;
            transform: translateY(-1px);
            box-shadow: 0 5px 14px rgba(22, 119, 255, 0.36);
          }

          &:active:not(:disabled) {
            transform: translateY(0);
          }
        }
      }
    }
  }

  .results-container {
    min-height: 380px;

    .results-header-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      font-size: 13px;

      .res-count {
        color: #334155;
        font-weight: 500;

        strong {
          color: #2563EB;
          font-weight: 700;
        }
      }

      .res-tip {
        font-size: 12px;
        color: #94A3B8;
      }
    }

    .empty-state-box {
      background: #FFFFFF;
      border: 1px dashed #CBD5E1;
      border-radius: 12px;
      padding: 60px 20px;
    }
  }
}
</style>
