<template>
  <div class="retrieval-sandbox-page">
    <!-- 1. 检索控制与配置面板 -->
    <div class="sandbox-config-card">
      <div class="config-header">
        <div class="left-title">
          <el-icon class="icon"><Search /></el-icon>
          <span class="title">向量检索测试沙箱 (Vector Retrieval Sandbox)</span>
        </div>
        <div class="quick-examples">
          <span class="label">测试用例：</span>
          <el-tag
            v-for="example in presetExamples"
            :key="example"
            class="example-tag"
            size="small"
            @click="applyExample(example)"
          >
            {{ example }}
          </el-tag>
        </div>
      </div>

      <div class="query-input-row">
        <el-input
          v-model="query"
          type="textarea"
          :rows="2"
          placeholder="请输入测试问题或语义检索词句（例如：什么是 Java 向上转型？为什么静态方法不能被重写？）..."
          class="custom-textarea"
          @keydown.enter.prevent="runRetrieval"
        />
      </div>

      <!-- 参数调节器 -->
      <div class="parameters-row">
        <div class="param-item">
          <span class="param-label">召回数量 (Top-K): <strong>{{ topK }}</strong></span>
          <el-slider v-model="topK" :min="1" :max="15" :step="1" style="width: 140px" />
        </div>

        <div class="param-item">
          <span class="param-label">相似度阈值 (Threshold): <strong>{{ scoreThreshold.toFixed(2) }}</strong></span>
          <el-slider v-model="scoreThreshold" :min="0.30" :max="0.95" :step="0.05" style="width: 140px" />
        </div>

        <div class="param-item switch-item">
          <span class="param-label">混合检索 (Hybrid Search)</span>
          <el-tooltip content="V0.5 仅支持向量检索，混合检索将于后续版本开放">
            <el-switch v-model="hybridSearch" disabled active-text="V0.5 仅向量检索" />
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
        <el-empty description="在上方输入测试问题并点击【执行语义检索】，验证知识库语义召回精度">
          <template #extra>
            <el-button type="primary" plain @click="runRetrieval">
              使用默认测试题召回
            </el-button>
          </template>
        </el-empty>
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

const presetExamples = [
  '什么是 Java 向上转型？',
  '接口与抽象类的设计权衡',
  '拉格朗日中值定理几何意义',
  '受检异常与运行时异常区别'
];

const applyExample = (text: string) => {
  query.value = text;
  runRetrieval();
};
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

      .quick-examples {
        display: flex;
        align-items: center;
        gap: 6px;

        .label {
          font-size: 12px;
          color: #94A3B8;
        }

        .example-tag {
          cursor: pointer;
          transition: all 0.15s;

          &:hover {
            color: #2563EB;
            border-color: #BFDBFE;
            background: #EFF6FF;
          }
        }
      }
    }

    .parameters-row {
      display: flex;
      align-items: center;
      gap: 24px;
      flex-wrap: wrap;
      background: #F8FAFC;
      border: 1px solid #F1F5F9;
      border-radius: 8px;
      padding: 10px 16px;

      .param-item {
        display: flex;
        align-items: center;
        gap: 10px;

        .param-label {
          font-size: 12px;
          color: #475569;

          strong {
            color: #2563EB;
          }
        }

        &.switch-item {
          margin-left: auto;
        }
      }

      .action-item {
        .search-btn {
          font-weight: 600;
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
