<template>
  <div class="embeddings-dashboard-page">
    <!-- 1. 向量数据库连接与核心状态横幅 -->
    <div class="engine-status-banner">
      <div class="banner-left">
        <div class="engine-logo-box">
          <el-icon class="engine-icon"><Cpu /></el-icon>
        </div>
        <div class="engine-meta">
          <div class="title-row">
            <h3>{{ stats.engine }} 向量引擎</h3>
            <span class="version-tag">{{ stats.engineVersion }}</span>
            <span class="status-indicator-badge" :class="stats.connectionStatus === 'ONLINE' ? 'online' : 'offline'">
              <span class="ping-circle"></span>
              {{ stats.connectionStatus === 'ONLINE' ? '运行中 (ONLINE)' : stats.connectionStatus === 'DEGRADED' ? '降级运行 (DEGRADED)' : '离线 (OFFLINE)' }}
            </span>
          </div>
          <p class="collection-desc">
            底层物理 Collection：<code class="collection-code">{{ stats.collectionName || 'edu_kb_math_vectors' }}</code>
            · 索引类型：<span class="tech-tag">{{ stats.indexType }}</span>
            · 距离度量：<span class="tech-tag">{{ stats.metricType }}</span>
          </p>
        </div>
      </div>

      <div class="banner-right">
        <el-button
          type="primary"
          :icon="Refresh"
          :loading="reindexing"
          @click="startReindex('INCREMENTAL')"
        >
          增量索引同步
        </el-button>
        <el-button
          type="danger"
          plain
          :icon="Warning"
          @click="confirmFullReindex"
        >
          全量重建索引
        </el-button>
      </div>
    </div>

    <!-- 2. 核心指标与健康度大屏 -->
    <div class="metrics-grid">
      <!-- 向量入库进度环 -->
      <div class="metric-card progress-card">
        <div class="card-header">
          <span class="card-title">向量化完成率</span>
          <span class="health-tag">健康指数: {{ stats.indexHealthScore }}/100</span>
        </div>
        <div class="progress-circle-wrapper">
          <el-progress
            type="dashboard"
            :percentage="completionPercentage"
            :color="customColors"
            :stroke-width="12"
            :width="150"
          >
            <template #default="{ percentage }">
              <span class="percentage-value">{{ percentage }}%</span>
              <span class="percentage-label">已入库</span>
            </template>
          </el-progress>
          <div class="progress-meta-counts">
            <div class="p-item">
              <span class="k">已向量化：</span>
              <span class="v text-blue">{{ stats.totalVectors }}</span>
            </div>
            <div class="p-item">
              <span class="k">目标总数：</span>
              <span class="v">{{ stats.expectedVectors }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 向量维度 -->
      <div class="metric-card info-card">
        <div class="card-icon-squircle icon-blue">
          <el-icon><Compass /></el-icon>
        </div>
        <div class="info-content">
          <span class="info-label">向量空间维度 (Dimension)</span>
          <span class="info-val">{{ stats.dimensions }} <span class="sub">维稠密向量</span></span>
          <span class="info-hint">底层 {{ stats.engine }} · {{ stats.metricType }} 距离度量</span>
        </div>
      </div>

      <!-- 平均召回延迟 -->
      <div class="metric-card info-card">
        <div class="card-icon-squircle icon-emerald">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="info-content">
          <span class="info-label">平均检索耗时 (Recall Latency)</span>
          <span class="info-val text-emerald">
            <template v-if="stats.avgQueryLatencyMs > 0">
              {{ stats.avgQueryLatencyMs }} <span class="sub">ms</span>
            </template>
            <template v-else>
              <span style="font-size: 18px; font-weight: 500; color: #64748B;">暂无记录</span>
            </template>
          </span>
          <span class="info-hint">
            <router-link v-if="stats.avgQueryLatencyMs === 0" :to="`/knowledge/${kbId}/retrieval`" style="color: #2563EB;">
              前往检索测试 &gt;
            </router-link>
            <template v-else>
              Top-10 相似度计算耗时 P95 &lt; 25ms
            </template>
          </span>
        </div>
      </div>

      <!-- 异常切片预警 -->
      <div class="metric-card info-card">
        <div class="card-icon-squircle" :class="failedItems.length > 0 ? 'icon-rose' : 'icon-slate'">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="info-content">
          <span class="info-label">向量化异常切片</span>
          <span class="info-val" :class="{ 'text-rose': failedItems.length > 0 }">
            {{ failedItems.length }} <span class="sub">个异常块</span>
          </span>
          <span class="info-hint">
            {{ failedItems.length > 0 ? '需要重试或重新生成' : '全量分块索引正常' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 3. 异常向量排查与处理表格 -->
    <div class="failed-vectors-section">
      <div class="section-header-card">
        <div class="left">
          <span class="sec-title">索引异常与重试列表</span>
          <span class="sec-badge">{{ failedItems.length }} 待修复</span>
        </div>
        <div class="right">
          <el-button
            v-if="failedItems.length > 0"
            type="primary"
            size="small"
            :loading="retrying"
            :icon="RefreshRight"
            @click="handleRetryFailed"
          >
            一键重试全部失败切片
          </el-button>
        </div>
      </div>

      <el-card shadow="never" class="table-card">
        <el-table
          v-if="failedItems.length > 0"
          :data="failedItems"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600' }"
        >
          <el-table-column label="切片信息" min-width="200">
            <template #default="{ row }">
              <div class="failed-chunk-info">
                <span class="chunk-idx-tag">#{{ row.chunkIndex }}</span>
                <span class="doc-title" :title="row.documentName">{{ row.documentName }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="切片摘录" min-width="280">
            <template #default="{ row }">
              <span class="snippet-text" :title="row.snippet">{{ row.snippet }}</span>
            </template>
          </el-table-column>

          <el-table-column label="异常归因与错误码" min-width="260">
            <template #default="{ row }">
              <div class="error-cause-cell">
                <el-tag type="danger" size="small">{{ row.errorCode }}</el-tag>
                <span class="error-reason-text" :title="row.errorReason">{{ row.errorReason }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="retryCount" label="重试次数" width="100" align="center">
            <template #default="{ row }">
              <span class="retry-count-pill">{{ row.retryCount }} 次</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" :loading="retryingChunkId === row.id" @click="handleRetrySingleChunk(row.id)">
                单独重试
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="all-healthy-empty">
          <el-icon class="healthy-icon"><CircleCheckFilled /></el-icon>
          <p class="healthy-title">所有切片均已完成向量化入库</p>
          <p class="healthy-subtitle">向量库状态健康，支持秒级语义相似度召回与 RAG 上下文增强。</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useEmbeddingIndex } from '@/composables/knowledge/useEmbeddingIndex';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';

const { kbId } = useKnowledgeRoute();
import { ElMessageBox } from 'element-plus';
import {
  Cpu,
  Refresh,
  Warning,
  Compass,
  Timer,
  CircleClose,
  RefreshRight,
  CircleCheckFilled
} from '@element-plus/icons-vue';

const {
  loading,
  reindexing,
  retrying,
  retryingChunkId,
  stats,
  failedItems,
  fetchStatus,
  startReindex,
  handleRetryFailed,
  handleRetrySingleChunk
} = useEmbeddingIndex(kbId);

const completionPercentage = computed(() => {
  if (!stats.value.expectedVectors || stats.value.expectedVectors <= 0) return 0;
  return Math.min(100, Math.round((stats.value.totalVectors / stats.value.expectedVectors) * 100));
});

const customColors = [
  { color: '#EF4444', percentage: 20 },
  { color: '#F59E0B', percentage: 60 },
  { color: '#3B82F6', percentage: 90 },
  { color: '#10B981', percentage: 100 }
];

const confirmFullReindex = () => {
  ElMessageBox.confirm(
    '全量重建索引将清空现有向量空间并对全部文档重新执行 Embedding 计算，可能消耗较多 Token 额度。确定继续？',
    '全量重建索引确认',
    {
      confirmButtonText: '确定重建',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    startReindex('FULL');
  }).catch(() => {});
};

onMounted(() => {
  fetchStatus();
});
</script>

<style scoped lang="scss">
.embeddings-dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  /* 顶部引擎横幅 */
  .engine-status-banner {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(30, 80, 150, 0.04);

    .banner-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .engine-logo-box {
        width: 52px;
        height: 52px;
        border-radius: 12px;
        background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
        border: 1px solid #BFDBFE;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        color: #1D4ED8;
      }

      .engine-meta {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .title-row {
          display: flex;
          align-items: center;
          gap: 10px;

          h3 {
            margin: 0;
            font-size: 18px;
            font-weight: 700;
            color: #0F172A;
          }

          .version-tag {
            font-family: ui-monospace, monospace;
            font-size: 11px;
            background: #F1F5F9;
            color: #475569;
            padding: 2px 8px;
            border-radius: 4px;
          }

          .status-indicator-badge {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 11.5px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 12px;

            &.online {
              background: #ECFDF5;
              color: #059669;
              border: 1px solid #A7F3D0;

              .ping-circle {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #10B981;
                box-shadow: 0 0 8px #10B981;
              }
            }
          }
        }

        .collection-desc {
          margin: 0;
          font-size: 13px;
          color: #64748B;

          .collection-code {
            font-family: ui-monospace, monospace;
            color: #2563EB;
            background: #EFF6FF;
            padding: 2px 6px;
            border-radius: 4px;
          }

          .tech-tag {
            color: #334155;
            font-weight: 600;
          }
        }
      }
    }

    .banner-right {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }

  /* 指标网格 */
  .metrics-grid {
    display: grid;
    grid-template-columns: 1.4fr 1fr 1fr 1fr;
    gap: 16px;

    @media (max-width: 1280px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .metric-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 18px 20px;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

      &.progress-card {
        display: flex;
        flex-direction: column;

        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .card-title {
            font-size: 13px;
            font-weight: 600;
            color: #475569;
          }

          .health-tag {
            font-size: 11px;
            color: #059669;
            background: #ECFDF5;
            padding: 2px 8px;
            border-radius: 6px;
            font-weight: 600;
          }
        }

        .progress-circle-wrapper {
          display: flex;
          align-items: center;
          justify-content: space-around;
          flex: 1;

          .percentage-value {
            font-size: 24px;
            font-weight: 700;
            color: #0F172A;
            display: block;
          }

          .percentage-label {
            font-size: 11px;
            color: #94A3B8;
            margin-top: 2px;
          }

          .progress-meta-counts {
            display: flex;
            flex-direction: column;
            gap: 8px;

            .p-item {
              display: flex;
              flex-direction: column;

              .k {
                font-size: 11px;
                color: #94A3B8;
              }

              .v {
                font-size: 18px;
                font-weight: 700;
                color: #1E293B;

                &.text-blue { color: #2563EB; }
              }
            }
          }
        }
      }

      &.info-card {
        display: flex;
        flex-direction: column;
        gap: 12px;
        justify-content: center;

        .card-icon-squircle {
          width: 44px;
          height: 44px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 22px;

          &.icon-blue {
            background: #EFF6FF;
            color: #2563EB;
          }
          &.icon-emerald {
            background: #ECFDF5;
            color: #059669;
          }
          &.icon-rose {
            background: #FFF1F2;
            color: #E11D48;
          }
          &.icon-slate {
            background: #F1F5F9;
            color: #64748B;
          }
        }

        .info-content {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .info-label {
            font-size: 12px;
            color: #64748B;
            font-weight: 500;
          }

          .info-val {
            font-size: 22px;
            font-weight: 700;
            color: #0F172A;

            &.text-emerald { color: #059669; }
            &.text-rose { color: #E11D48; }

            .sub {
              font-size: 12px;
              font-weight: normal;
              color: #94A3B8;
            }
          }

          .info-hint {
            font-size: 11.5px;
            color: #94A3B8;
          }
        }
      }
    }
  }

  /* 异常表格卡片 */
  .failed-vectors-section {
    .section-header-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-bottom: none;
      border-radius: 12px 12px 0 0;
      padding: 14px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .left {
        display: flex;
        align-items: center;
        gap: 8px;

        .sec-title {
          font-size: 14px;
          font-weight: 700;
          color: #1E293B;
        }

        .sec-badge {
          font-size: 11px;
          background: #FEE2E2;
          color: #DC2626;
          font-weight: 600;
          padding: 2px 6px;
          border-radius: 4px;
        }
      }
    }

    .table-card {
      border-radius: 0 0 12px 12px;
      border-color: #E2E8F0;

      .failed-chunk-info {
        display: flex;
        align-items: center;
        gap: 8px;

        .chunk-idx-tag {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          font-weight: 700;
          color: #DC2626;
          background: #FEF2F2;
          padding: 1px 6px;
          border-radius: 4px;
        }

        .doc-title {
          font-size: 13px;
          font-weight: 600;
          color: #334155;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .snippet-text {
        font-size: 12.5px;
        color: #64748B;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
        line-height: 1.5;
      }

      .error-cause-cell {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .error-reason-text {
          font-size: 11.5px;
          color: #EF4444;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .retry-count-pill {
        font-family: ui-monospace, monospace;
        font-size: 12px;
        background: #F1F5F9;
        color: #475569;
        padding: 2px 6px;
        border-radius: 4px;
      }

      .all-healthy-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 40px 20px;
        color: #059669;

        .healthy-icon {
          font-size: 48px;
          margin-bottom: 12px;
        }

        .healthy-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .healthy-subtitle {
          margin: 6px 0 0 0;
          font-size: 13px;
          color: #64748B;
        }
      }
    }
  }
}
</style>
