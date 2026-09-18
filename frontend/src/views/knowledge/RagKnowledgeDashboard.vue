<template>
  <div v-loading="loading" class="rag-dashboard-page">
    <div class="page-toolbar">
      <CapsuleBackButton label="返回知识库中心" @click="router.push('/knowledge')" />
      <span class="page-context-badge">
        <el-icon><DataAnalysis /></el-icon>
        <span>RAG 切片与健康度大盘</span>
      </span>
    </div>

    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div class="header-info">
          <div class="header-tag-row">
            <span class="module-badge">
              <el-icon><DataAnalysis /></el-icon>
              EduMind · RAG 切片与健康度
            </span>
            <span class="env-badge">全景监控</span>
          </div>
          <h2 class="header-title">课程知识库切片与健康度大盘</h2>
          <p class="header-subtitle">
            监控课节讲义与上传文档切片、混合检索召回与向量索引状态
          </p>
        </div>
        <div class="header-actions">
          <el-button
            type="primary"
            class="sync-all-btn"
            :loading="syncingAll"
            @click="handleSyncAll"
          >
            <el-icon><Refresh /></el-icon>
            全量切片同步与重建
          </el-button>
          <el-button
            :loading="purging"
            @click="handlePurgeOrphans"
          >
            <el-icon><Delete /></el-icon>
            清理无效切片
          </el-button>
          <el-button :loading="loading" @click="handleRefresh">
            <el-icon><RefreshRight /></el-icon>
            刷新大盘
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="tab-pane">
      <el-alert
        v-if="syncBanner"
        :title="syncBanner"
        type="success"
        show-icon
        closable
        class="banner-alert"
        @close="syncBanner = null"
      />
      <el-alert
        v-if="stats.latestIndexStatus === 'INDEXING'"
        title="向量索引任务进行中"
        type="info"
        show-icon
        :closable="false"
        class="banner-alert"
        :description="`进度 ${stats.latestIndexIndexedChunks ?? 0} / ${stats.latestIndexTotalChunks ?? 0}`"
      />
      <el-alert
        v-if="stats.orphanChunkEstimate > 0"
        type="warning"
        show-icon
        :closable="false"
        class="banner-alert"
        :title="`约 ${stats.orphanChunkEstimate} 块无效切片可清理`"
      />

      <el-row :gutter="14" class="stats-row">
        <el-col :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
          <div class="stat-card stat-articles">
            <div class="stat-card-head">
              <span class="stat-label">知识文档</span>
              <div class="stat-icon-badge"><el-icon><Document /></el-icon></div>
            </div>
            <div class="stat-value">{{ stats.totalDocuments }}</div>
            <div class="stat-sub">已切片 {{ stats.indexedDocuments }} 篇</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
          <div class="stat-card stat-chunks">
            <div class="stat-card-head">
              <span class="stat-label">切片总量</span>
              <div class="stat-icon-badge"><el-icon><Files /></el-icon></div>
            </div>
            <div class="stat-value">{{ stats.totalChunks }}</div>
            <div class="stat-sub">课节 {{ stats.lessonChunks }} · 上传 {{ stats.uploadChunks }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
          <div class="stat-card stat-normal">
            <div class="stat-card-head">
              <span class="stat-label">可用向量切片</span>
              <div class="stat-icon-badge"><el-icon><CircleCheck /></el-icon></div>
            </div>
            <div class="stat-value">{{ stats.normalChunks }}</div>
            <div class="stat-sub">INDEXED 状态</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
          <div class="stat-card stat-degraded">
            <div class="stat-card-head">
              <span class="stat-label">索引失败</span>
              <div class="stat-icon-badge"><el-icon><Warning /></el-icon></div>
            </div>
            <div class="stat-value">{{ stats.failedChunks }}</div>
            <div class="stat-sub">需重建或检查 Key</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
          <div class="stat-card stat-sessions">
            <div class="stat-card-head">
              <span class="stat-label">AI 会话</span>
              <div class="stat-icon-badge"><el-icon><ChatDotRound /></el-icon></div>
            </div>
            <div class="stat-value">{{ stats.totalSessions }}</div>
            <div class="stat-sub">累计对话会话</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4" class="stat-col">
          <div class="stat-card stat-embedding">
            <div class="stat-card-head">
              <span class="stat-label">Embedding 维度</span>
              <div class="stat-icon-badge"><el-icon><Cpu /></el-icon></div>
            </div>
            <div class="stat-value">{{ stats.embeddingDimension ?? '—' }}</div>
            <div class="stat-sub stat-sub--ellipsis" :title="stats.embeddingModelName">
              {{ stats.embeddingModelName || '后台模型配置' }}
            </div>
          </div>
        </el-col>
      </el-row>

      <el-card shadow="never" class="health-card">
        <template #header>
          <div class="card-header-flex">
            <span class="card-title"><el-icon><DataAnalysis /></el-icon> 切片索引健康度</span>
            <el-tag :type="healthRate >= 95 ? 'success' : healthRate >= 80 ? 'warning' : 'danger'" round effect="dark">
              健康指数 {{ healthRate }}%
            </el-tag>
          </div>
        </template>
        <el-progress :percentage="healthRate" :stroke-width="16" striped striped-flow :color="progressColors" />
        <div class="health-meta">
          <span><i class="dot dot-ok" />可用 {{ stats.normalChunks }}</span>
          <span><i class="dot dot-warn" />失败 {{ stats.failedChunks }}</span>
        </div>
      </el-card>

      <el-card shadow="never" class="control-card">
        <template #header>
          <span class="card-title"><el-icon><EditPen /></el-icon> 单文档切片重建</span>
        </template>
        <p class="control-desc">
          某篇文档或课节讲义更新后，输入知识库文档 ID 可单独触发向量重建。向量化模型见上方「Embedding 维度」卡片。
        </p>
        <div class="input-row">
          <el-input v-model="targetDocumentId" placeholder="知识库文档 ID" clearable />
          <el-button type="primary" :loading="syncingDoc" @click="handleSyncDocument">立即重建</el-button>
          <el-button @click="router.push('/system/models')">模型配置</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CapsuleBackButton from '@/components/common/CapsuleBackButton.vue'
import {
  ChatDotRound,
  CircleCheck,
  Cpu,
  DataAnalysis,
  Delete,
  Document,
  EditPen,
  Files,
  Refresh,
  RefreshRight,
  Warning
} from '@element-plus/icons-vue'
import { useRagKnowledgeDashboard } from '@/composables/knowledge/useRagKnowledgeDashboard'

const router = useRouter()

const {
  loading,
  syncingAll,
  purging,
  syncingDoc,
  stats,
  targetDocumentId,
  syncBanner,
  healthRate,
  fetchStats,
  handleSyncAll,
  handlePurgeOrphans,
  handleSyncDocument
} = useRagKnowledgeDashboard()

const progressColors = [
  { color: '#f59e0b', percentage: 60 },
  { color: '#10b981', percentage: 100 }
]

function handleRefresh() {
  fetchStats()
}

onMounted(() => fetchStats())
</script>

<style scoped lang="scss">
@import '@/components/knowledge/rag-dashboard/rag-dashboard-stats.scss';

.rag-dashboard-page {
  padding: 0 4px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.page-context-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #a7f3d0;
  border-radius: 9999px;
  font-size: 12.5px;
  font-weight: 600;
  color: #059669;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

.header-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: none;
}

.header-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
}

.module-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #059669;
  background: rgba(16, 185, 129, 0.1);
  padding: 4px 10px;
  border-radius: 999px;
}

.env-badge {
  margin-left: 8px;
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.header-title {
  margin: 10px 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.header-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  max-width: 720px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
}

.stats-row :deep(.stat-col) {
  display: flex;
}

.stats-row :deep(.stat-col) .stat-card {
  flex: 1;
  width: 100%;
}

.tab-pane {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.banner-alert {
  border-radius: 10px;
}

.health-card,
.control-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: none;
}

.health-card :deep(.el-card__header),
.control-card :deep(.el-card__header) {
  border-bottom: 1px solid #e2e8f0;
}

.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.health-meta {
  display: flex;
  gap: 20px;
  margin-top: 12px;
  font-size: 13px;
  color: #64748b;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.dot-ok { background: #10b981; }
.dot-warn { background: #f59e0b; }

.control-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px;
}

.input-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  .el-input {
    flex: 1;
    min-width: 200px;
  }
}
</style>
