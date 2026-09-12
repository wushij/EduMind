<template>
  <div class="document-parse-container">
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.back()">
        返回知识库详情
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库中心</el-breadcrumb-item>
        <el-breadcrumb-item>文档解析与向量切片工作台</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter-panel-card mb-4">
      <el-select
        v-model="selectedDocumentId"
        placeholder="选择要解析的文档"
        style="width: 320px"
        @change="handleDocumentChange"
      >
        <el-option
          v-for="doc in documents"
          :key="doc.id"
          :label="doc.name"
          :value="doc.id"
        />
      </el-select>
      <el-button type="primary" :loading="pipelineRunning" @click="handleRunPipeline">
        执行解析 → 切片 → 向量化
      </el-button>
    </div>

    <div v-loading="loading" class="doc-hero-card">
      <div class="doc-info-left">
        <div class="doc-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div>
          <div class="doc-title-row">
            <h2 class="doc-title">{{ docInfo.fileName || '请选择文档' }}</h2>
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
          </div>
          <div class="doc-meta-row">
            <span>文件大小：{{ docInfo.fileSize }}</span>
            <span>文档格式：{{ docInfo.fileType }}</span>
            <span>切分块数：<strong class="text-blue-600">{{ chunks.length }} 块</strong></span>
            <span>已向量化：<strong>{{ indexStatus.indexedChunks ?? 0 }}</strong></span>
          </div>
        </div>
      </div>
      <div class="doc-actions-right">
        <el-button type="primary" plain :loading="pipelineRunning" @click="handleRunPipeline">
          重新执行流水线
        </el-button>
      </div>
    </div>

    <div class="pipeline-card">
      <div class="pipeline-steps">
        <div class="step-node" :class="{ active: stepDone(1) }">
          <div class="step-circle">1</div>
          <div class="step-text">
            <span class="step-name">文本解析</span>
            <span class="step-status">{{ stepDone(1) ? '已完成' : '待执行' }}</span>
          </div>
        </div>
        <el-icon class="step-arrow"><ArrowRight /></el-icon>
        <div class="step-node" :class="{ active: stepDone(2) }">
          <div class="step-circle">2</div>
          <div class="step-text">
            <span class="step-name">语义分块</span>
            <span class="step-status">{{ chunks.length }} 个分块</span>
          </div>
        </div>
        <el-icon class="step-arrow"><ArrowRight /></el-icon>
        <div class="step-node" :class="{ active: stepDone(3) }">
          <div class="step-circle">3</div>
          <div class="step-text">
            <span class="step-name">向量索引</span>
            <span class="step-status">{{ indexStatus.status || '待索引' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="workspace-grid">
      <div class="outline-col">
        <el-card shadow="never" class="panel-card">
          <h3 class="panel-title">文档目录大纲</h3>
          <el-empty v-if="outlineData.length === 0" description="暂无大纲，请先完成切片" />
          <div v-else class="outline-tree-wrapper">
            <el-tree :data="outlineData" default-expand-all node-key="id" />
          </div>
        </el-card>
      </div>

      <div class="chunks-col">
        <el-card shadow="never" class="panel-card">
          <h3 class="panel-title">已生成的切片分块</h3>
          <el-empty v-if="chunks.length === 0" description="暂无切片数据" />
          <div v-else class="chunks-flow">
            <div v-for="(chunk, cIdx) in chunks" :key="chunk.id" class="chunk-card">
              <div class="chunk-top">
                <div class="chunk-index-badge">
                  <span>#Chunk {{ cIdx + 1 }}</span>
                  <span class="tokens-tag">{{ chunk.tokenCount }} Tokens</span>
                </div>
                <el-tag size="small">{{ chunk.status }}</el-tag>
              </div>
              <div class="chunk-content">{{ chunk.content }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, ArrowRight, Document } from '@element-plus/icons-vue';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import { getDocuments, parseDocument } from '@/api/knowledge/document';
import { getChunks, triggerChunk } from '@/api/knowledge/chunk';
import { getVectorStats, triggerReindex } from '@/api/knowledge/embedding';
import type { KBDocument } from '@/types/knowledge/document';
import type { DocumentChunk } from '@/types/knowledge/chunk';

const router = useRouter();
const route = useRoute();
const { kbId } = useKnowledgeRoute();

const documents = ref<KBDocument[]>([]);
const selectedDocumentId = ref<number | undefined>();
const chunks = ref<DocumentChunk[]>([]);
const loading = ref(false);
const pipelineRunning = ref(false);
const indexStatus = ref<{ status?: string; indexedChunks?: number; totalChunks?: number }>({});

const docInfo = computed(() => {
  const doc = documents.value.find((d) => d.id === selectedDocumentId.value);
  if (!doc) {
    return { fileName: '', fileSize: '-', fileType: '-', status: 'PENDING' };
  }
  const sizeMb = doc.size ? `${(doc.size / 1024 / 1024).toFixed(2)} MB` : '-';
  return {
    fileName: doc.name,
    fileSize: sizeMb,
    fileType: doc.type || 'FILE',
    status: doc.status
  };
});

const statusLabel = computed(() => {
  const s = docInfo.value.status;
  if (s === 'COMPLETED') return '解析完成';
  if (s === 'PARSING') return '解析中';
  if (s === 'FAILED') return '解析失败';
  return '待处理';
});

const statusTagType = computed(() => {
  const s = docInfo.value.status;
  if (s === 'COMPLETED') return 'success';
  if (s === 'FAILED') return 'danger';
  if (s === 'PARSING') return 'warning';
  return 'info';
});

const outlineData = computed(() => {
  const headings = chunks.value
    .filter((c) => c.heading)
    .map((c, idx) => ({ id: idx + 1, label: c.heading as string }));
  return headings.length > 0 ? headings : [];
});

function stepDone(step: number) {
  if (step === 1) return docInfo.value.status === 'COMPLETED';
  if (step === 2) return chunks.value.length > 0;
  if (step === 3) return (indexStatus.value.indexedChunks ?? 0) > 0;
  return false;
}

async function loadDocuments() {
  if (!kbId.value) return;
  try {
    const res = await getDocuments(kbId.value);
    documents.value = Array.isArray(res?.data) ? res.data : [];
    const queryDocId = Number(route.query.documentId);
    if (queryDocId && documents.value.some((d) => d.id === queryDocId)) {
      selectedDocumentId.value = queryDocId;
    } else if (documents.value.length > 0) {
      selectedDocumentId.value = documents.value[0].id;
    }
  } catch {
    ElMessage.error('加载文档列表失败');
  }
}

async function loadChunks() {
  if (!selectedDocumentId.value) {
    chunks.value = [];
    return;
  }
  loading.value = true;
  try {
    chunks.value = await getChunks(selectedDocumentId.value, { page: 1, pageSize: 100 });
  } catch {
    chunks.value = [];
    ElMessage.error('加载切片失败');
  } finally {
    loading.value = false;
  }
}

async function loadIndexStatus() {
  if (!kbId.value) return;
  try {
    const stats = await getVectorStats(kbId.value);
    indexStatus.value = {
      status: stats.connectionStatus === 'ONLINE' ? 'INDEXED' : 'INDEXING',
      indexedChunks: stats.totalVectors,
      totalChunks: stats.expectedVectors
    };
  } catch {
    indexStatus.value = {};
  }
}

async function handleDocumentChange() {
  await loadChunks();
  await loadIndexStatus();
}

async function handleRunPipeline() {
  if (!kbId.value || !selectedDocumentId.value) {
    ElMessage.warning('请先选择文档');
    return;
  }
  pipelineRunning.value = true;
  try {
    await parseDocument(kbId.value, selectedDocumentId.value);
    await triggerChunk(selectedDocumentId.value);
    await triggerReindex(kbId.value, 'INCREMENTAL');
    ElMessage.success('文档流水线已启动：解析 → 切片 → 向量化');
    await loadDocuments();
    await loadChunks();
    await loadIndexStatus();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '流水线执行失败');
  } finally {
    pipelineRunning.value = false;
  }
}

watch(kbId, () => {
  loadDocuments();
  loadIndexStatus();
});

onMounted(async () => {
  await loadDocuments();
  await loadChunks();
  await loadIndexStatus();
});
</script>

<style scoped lang="scss">
.document-parse-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);

  .filter-panel-card {
    display: flex;
    gap: 12px;
    align-items: center;
    background: #fff;
    padding: 16px;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
  }

  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .doc-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .doc-info-left {
      display: flex;
      align-items: center;
      gap: 18px;

      .doc-icon {
        width: 56px;
        height: 56px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        color: #1677FF;
      }

      .doc-title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .doc-title {
          font-size: 20px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }
      }

      .doc-meta-row {
        display: flex;
        gap: 20px;
        font-size: 13px;
        color: #64748b;
      }
    }
  }

  .pipeline-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    padding: 16px 28px;
    margin-bottom: 24px;

    .pipeline-steps {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .step-node {
      display: flex;
      align-items: center;
      gap: 10px;
      opacity: 0.5;

      &.active {
        opacity: 1;
      }

      .step-circle {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: #e2e8f0;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 12px;
      }

      &.active .step-circle {
        background: #3b82f6;
        color: #fff;
      }

      .step-text {
        display: flex;
        flex-direction: column;

        .step-name {
          font-size: 13px;
          font-weight: 600;
        }

        .step-status {
          font-size: 12px;
          color: #64748b;
        }
      }
    }

    .step-arrow {
      color: #94a3b8;
    }
  }

  .workspace-grid {
    display: grid;
    grid-template-columns: 320px 1fr;
    gap: 20px;

    .panel-card {
      border-radius: 14px;
      border: 1px solid #e2e8f0;
    }

    .panel-title {
      font-size: 15px;
      font-weight: 700;
      margin: 0 0 16px;
    }

    .chunks-flow {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 600px;
      overflow-y: auto;
    }

    .chunk-card {
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      padding: 12px;
      background: #f8fafc;

      .chunk-top {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
      }

      .chunk-index-badge {
        display: flex;
        gap: 8px;
        font-size: 12px;
        font-weight: 600;
      }

      .tokens-tag {
        color: #64748b;
      }

      .chunk-content {
        font-size: 13px;
        line-height: 1.6;
        color: #334155;
      }
    }
  }
}
</style>
