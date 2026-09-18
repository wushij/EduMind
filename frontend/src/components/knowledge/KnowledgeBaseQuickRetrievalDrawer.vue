<template>
  <el-drawer
    :model-value="visible"
    title="RAG 语义检索即时测试"
    size="560px"
    class="kb-retrieval-drawer"
    direction="rtl"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="drawer-container">
      <div class="drawer-intro-card">
        <el-icon class="intro-icon"><Aim /></el-icon>
        <div class="intro-text">
          <p class="intro-title">即时验证知识切片与向量索引质量</p>
          <p class="intro-sub">
            输入任意自然语言问题或知识点名称，直接通过底层 Milvus / PgVector 向量数据库计算余弦距离并召回最相关的切片。
          </p>
        </div>
      </div>

      <!-- 参数配置表单 -->
      <div class="retrieval-form-card">
        <div class="form-item-block">
          <label class="item-label">目标知识库</label>
          <el-select
            v-model="selectedKbId"
            placeholder="请选择要检索的目标知识库"
            class="capsule-select"
            style="width: 100%"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            >
              <div class="kb-option-item">
                <span class="opt-name">{{ kb.name }}</span>
                <span class="opt-meta">{{ kb.chunkCount }} 切片</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="form-item-block">
          <label class="item-label">检索问题 / 关键词</label>
          <el-input
            v-model="searchQuery"
            type="textarea"
            :rows="3"
            placeholder="例如：什么是快速排序的时间复杂度？或者输入课件中的关键概念..."
            class="capsule-textarea"
            clearable
            @keydown.enter.prevent="handleExecuteSearch"
          />
        </div>

        <div class="slider-row">
          <div class="slider-col">
            <div class="slider-header">
              <span class="sub-label">召回数量 Top-K</span>
              <strong class="slider-val">{{ topK }} 条</strong>
            </div>
            <el-slider v-model="topK" :min="1" :max="10" :step="1" />
          </div>

          <div class="slider-col">
            <div class="slider-header">
              <span class="sub-label">相似度阈值 Score</span>
              <strong class="slider-val">{{ scoreThreshold.toFixed(2) }}</strong>
            </div>
            <el-slider v-model="scoreThreshold" :min="0.1" :max="0.95" :step="0.05" />
          </div>
        </div>

        <button
          type="button"
          class="execute-search-btn"
          :disabled="searchLoading || !selectedKbId || !searchQuery.trim()"
          @click="handleExecuteSearch"
        >
          <el-icon :class="{ 'is-spinning': searchLoading }"><Search /></el-icon>
          <span>{{ searchLoading ? '语义检索计算中...' : '开始执行向量召回' }}</span>
        </button>
      </div>

      <!-- 检索结果呈现区 -->
      <div class="results-stage">
        <div class="results-header">
          <div class="title-with-pill">
            <span class="sec-title">召回切片结果</span>
            <span v-if="hasSearched" class="count-badge">
              匹配 {{ results.length }} 条
            </span>
          </div>
          <span v-if="durationMs > 0" class="time-cost-pill">
            耗时 {{ durationMs }} ms
          </span>
        </div>

        <div v-loading="searchLoading" class="results-list">
          <div v-if="!hasSearched && !searchLoading" class="empty-state-box">
            <el-icon class="empty-icon"><Compass /></el-icon>
            <p class="empty-tip">在上方选择知识库并输入问题后点击检索</p>
          </div>

          <div v-else-if="hasSearched && results.length === 0 && !searchLoading" class="empty-state-box">
            <el-icon class="empty-icon"><Warning /></el-icon>
            <p class="empty-tip">未检索到满足相似度阈值（>= {{ scoreThreshold }}）的相关切片，可尝试调低相似度阈值或增加 Top-K</p>
          </div>

          <div
            v-for="(item, idx) in results"
            :key="idx"
            class="chunk-result-card"
          >
            <div class="chunk-top-line">
              <div class="chunk-index-tag">#{{ idx + 1 }}</div>
              <div class="score-pill" :class="getScoreClass(item.score)">
                <span class="score-label">匹配度</span>
                <strong class="score-num">{{ (item.score * 100).toFixed(1) }}%</strong>
              </div>
              <span v-if="item.documentName" class="doc-source-pill" :title="item.documentName">
                <el-icon><Document /></el-icon>
                <span>{{ item.documentName }}</span>
              </span>
            </div>

            <div class="chunk-content-body">
              <p class="chunk-text">{{ item.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Aim, Search, Compass, Warning, Document } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { retrieveChunks } from '@/api/knowledge/retrieval';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import type { RetrievalResultItem } from '@/types/knowledge/rag';

const props = defineProps<{
  visible: boolean;
  knowledgeBases: KnowledgeBase[];
  initialKbId?: number;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
}>();

const selectedKbId = ref<number | undefined>(undefined);
const searchQuery = ref('');
const topK = ref(5);
const scoreThreshold = ref(0.60);
const searchLoading = ref(false);
const hasSearched = ref(false);
const durationMs = ref(0);
const results = ref<RetrievalResultItem[]>([]);

watch(
  () => props.visible,
  (open) => {
    if (open) {
      if (props.initialKbId) {
        selectedKbId.value = props.initialKbId;
      } else if (!selectedKbId.value && props.knowledgeBases.length > 0) {
        selectedKbId.value = props.knowledgeBases[0].id;
      }
    }
  }
);

function getScoreClass(score: number) {
  if (score >= 0.85) return 'is-high';
  if (score >= 0.70) return 'is-medium';
  return 'is-low';
}

async function handleExecuteSearch() {
  if (!selectedKbId.value) {
    ElMessage.warning('请先选择目标知识库');
    return;
  }
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入检索问题或关键词');
    return;
  }

  searchLoading.value = true;
  hasSearched.value = true;
  const start = performance.now();

  try {
    const list = await retrieveChunks({
      knowledgeBaseId: selectedKbId.value,
      query: searchQuery.value.trim(),
      topK: topK.value,
      scoreThreshold: scoreThreshold.value
    });
    results.value = list || [];
    durationMs.value = Math.round(performance.now() - start);
  } catch (err: any) {
    console.error('检索失败', err);
    ElMessage.error(err?.message || '向量检索失败，请确认该知识库已有已索引切片');
    results.value = [];
  } finally {
    searchLoading.value = false;
  }
}
</script>

<style scoped lang="scss">
.drawer-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 4px 0 20px;

  .drawer-intro-card {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    border-radius: 16px;
    padding: 14px 16px;

    .intro-icon {
      font-size: 20px;
      color: #2563eb;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .intro-text {
      .intro-title {
        margin: 0 0 4px;
        font-size: 13.5px;
        font-weight: 700;
        color: #1e3a8a;
      }
      .intro-sub {
        margin: 0;
        font-size: 12px;
        color: #3b82f6;
        line-height: 1.5;
      }
    }
  }

  .retrieval-form-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 18px;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
    display: flex;
    flex-direction: column;
    gap: 14px;

    .form-item-block {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .item-label {
        font-size: 12.5px;
        font-weight: 600;
        color: #334155;
      }

      .kb-option-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;

        .opt-name {
          font-weight: 500;
          color: #0f172a;
        }

        .opt-meta {
          font-size: 11.5px;
          color: #94a3b8;
        }
      }
    }

    .slider-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;

      .slider-col {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .slider-header {
          display: flex;
          align-items: center;
          justify-content: space-between;

          .sub-label {
            font-size: 12px;
            color: #64748b;
          }

          .slider-val {
            font-size: 12px;
            color: #2563eb;
            font-variant-numeric: tabular-nums;
          }
        }
      }
    }

    .execute-search-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      height: 40px;
      border-radius: 9999px;
      background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
      color: #ffffff;
      font-size: 13.5px;
      font-weight: 600;
      border: none;
      cursor: pointer;
      box-shadow: 0 3px 12px rgba(22, 119, 255, 0.28);
      transition: all 0.2s ease;

      &:hover:not(:disabled) {
        background: linear-gradient(135deg, #4096ff 0%, #1d4ed8 100%);
        transform: translateY(-1px);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .is-spinning {
        animation: spin 1s infinite linear;
      }
    }
  }

  .results-stage {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .results-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .title-with-pill {
        display: flex;
        align-items: center;
        gap: 8px;

        .sec-title {
          font-size: 14px;
          font-weight: 700;
          color: #0f172a;
        }

        .count-badge {
          padding: 2px 8px;
          border-radius: 9999px;
          background: #f1f5f9;
          font-size: 11px;
          font-weight: 600;
          color: #475569;
        }
      }

      .time-cost-pill {
        font-size: 11.5px;
        color: #94a3b8;
        font-variant-numeric: tabular-nums;
      }
    }

    .results-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-height: 120px;

      .empty-state-box {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 40px 20px;
        text-align: center;
        border: 1px dashed #cbd5e1;
        border-radius: 16px;
        background: #f8fafc;

        .empty-icon {
          font-size: 32px;
          color: #94a3b8;
          margin-bottom: 8px;
        }

        .empty-tip {
          margin: 0;
          font-size: 12.5px;
          color: #94a3b8;
          line-height: 1.5;
        }
      }

      .chunk-result-card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 16px;
        padding: 14px 16px;
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
        display: flex;
        flex-direction: column;
        gap: 10px;
        transition: border-color 0.2s;

        &:hover {
          border-color: #93c5fd;
        }

        .chunk-top-line {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .chunk-index-tag {
            width: 22px;
            height: 22px;
            border-radius: 6px;
            background: #f1f5f9;
            color: #475569;
            font-size: 11px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
          }

          .score-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 10px;
            border-radius: 9999px;
            font-size: 11.5px;
            font-weight: 600;

            &.is-high {
              background: #ecfdf5;
              color: #059669;
              border: 1px solid #a7f3d0;
            }

            &.is-medium {
              background: #eff6ff;
              color: #2563eb;
              border: 1px solid #bfdbfe;
            }

            &.is-low {
              background: #fffbeb;
              color: #d97706;
              border: 1px solid #fde68a;
            }
          }

          .doc-source-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 10px;
            border-radius: 9999px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            font-size: 11.5px;
            color: #64748b;
            max-width: 220px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .chunk-content-body {
          .chunk-text {
            margin: 0;
            font-size: 13px;
            color: #334155;
            line-height: 1.6;
            background: #f8fafc;
            padding: 10px 12px;
            border-radius: 10px;
            white-space: pre-wrap;
            word-break: break-word;
          }
        }
      }
    }
  }
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}
</style>
