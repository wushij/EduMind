<template>
  <div class="table-wrap">
    <el-table
      v-loading="tableLoading"
      element-loading-text="正在检索全链路 AI 审计日志..."
      :data="logs"
      stripe
      style="width: 100%"
      :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '48px' }"
    >
      <!-- 链路 Trace ID -->
      <el-table-column prop="traceId" label="链路 Trace ID" min-width="150">
        <template #default="{ row }">
          <div class="trace-cell">
            <span class="trace-badge">{{ row.traceId }}</span>
            <el-tooltip content="复制 Trace ID" placement="top">
              <button class="copy-btn" @click="$emit('copy-text', row.traceId)">
                <el-icon><CopyDocument /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>

      <!-- 调用用户（显示图形头像，对标 UserList） -->
      <el-table-column label="调用用户" min-width="170">
        <template #default="{ row }">
          <div class="user-info-cell">
            <el-avatar
              :size="36"
              :src="getUserAvatarUrl(row)"
              class="em-user-avatar-sm"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="user-text">
              <span class="user-name">{{ row.realName || row.username }}</span>
              <div class="user-sub">
                <span class="user-account">@{{ row.username }}</span>
                <span class="role-pill" :class="row.userRole.toLowerCase()">{{ getRoleLabel(row.userRole) }}</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- 业务场景 / 工具 -->
      <el-table-column prop="toolName" label="业务场景 / 工具" min-width="160">
        <template #default="{ row }">
          <div class="scene-tag-wrap">
            <span class="scene-badge" :class="getSceneStyleClass(row.scene || row.toolName)">
              {{ getSceneLabel(row.scene || row.toolName) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <!-- 生成模型 -->
      <el-table-column prop="model" label="生成模型" min-width="150">
        <template #default="{ row }">
          <span class="model-badge">
            <el-icon class="model-icon"><Cpu /></el-icon>
            {{ row.model }}
          </span>
        </template>
      </el-table-column>

      <!-- Token 规模与成本 -->
      <el-table-column label="Token 规模 & 成本" min-width="180">
        <template #default="{ row }">
          <div class="tokens-cell">
            <div class="tokens-main">
              <span class="token-sum">{{ row.totalTokens.toLocaleString() }}</span>
              <span class="cost-val">¥{{ row.estimatedCost.toFixed(4) }}</span>
            </div>
            <div class="tokens-split">
              入: {{ row.promptTokens }} · 出: {{ row.completionTokens }}
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- 耗时 Latency -->
      <el-table-column label="耗时" min-width="110">
        <template #default="{ row }">
          <span class="latency-badge" :class="getLatencyClass(row.durationMs)">
            {{ row.durationMs }}ms
          </span>
        </template>
      </el-table-column>

      <!-- 调用状态 -->
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span class="status-pill" :class="row.status.toLowerCase()">
            <span class="status-dot"></span>
            {{ getStatusLabel(row.status) }}
          </span>
        </template>
      </el-table-column>

      <!-- 请求时间 -->
      <el-table-column prop="createdAt" label="请求时间" min-width="165" />

      <!-- 操作 -->
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <button class="table-detail-btn" @click="$emit('view-detail', row)">
            <el-icon><View /></el-icon>
            <span>详情</span>
          </button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <!-- 分页栏：严格左对齐，支持每页 10 条分页 -->
  <div class="pagination-footer">
    <AppPagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @change="$emit('load-logs')"
    />
  </div>

  <!-- 4. 链路调用审计详情抽屉（使用专业 Icon 代替 Emoji） -->
  <el-drawer
    v-model="detailDrawerVisible"
    title="AI 调用全链路审计详情"
    size="580px"
    destroy-on-close
    class="audit-detail-drawer"
  >
    <div v-if="selectedLog" class="drawer-content">
      <!-- 头部 Trace 概览卡片 -->
      <div class="drawer-hero-card">
        <div class="hero-top">
          <div class="trace-title">
            <span class="label">Trace ID</span>
            <span class="val">{{ selectedLog.traceId }}</span>
          </div>
          <span class="status-pill" :class="selectedLog.status.toLowerCase()">
            <span class="status-dot"></span>
            {{ getStatusLabel(selectedLog.status) }}
          </span>
        </div>
        <div class="hero-grid">
          <div class="hero-item">
            <span class="hi-label">总消耗 Token</span>
            <span class="hi-val text-purple">{{ selectedLog.totalTokens.toLocaleString() }}</span>
          </div>
          <div class="hero-item">
            <span class="hi-label">响应耗时</span>
            <span class="hi-val text-emerald">{{ selectedLog.durationMs }}ms</span>
          </div>
          <div class="hero-item">
            <span class="hi-label">预估成本</span>
            <span class="hi-val text-amber">¥{{ selectedLog.estimatedCost.toFixed(4) }}</span>
          </div>
        </div>
      </div>

      <!-- 结构化项：全部使用 el-icon 规范呈现，无 emoji -->
      <div class="detail-section">
        <h4 class="sec-heading">
          <el-icon class="sec-icon icon-blue"><User /></el-icon>
          <span>调用方身份上下文</span>
        </h4>
        <div class="info-list">
          <div class="info-row">
            <span class="k">调用用户</span>
            <div class="v user-flex-item">
              <el-avatar :size="24" :src="getUserAvatarUrl(selectedLog)" class="mr-1" />
              <span class="font-bold">{{ selectedLog.realName || selectedLog.username }} (@{{ selectedLog.username }})</span>
            </div>
          </div>
          <div class="info-row">
            <span class="k">用户 ID / 角色</span>
            <span class="v">ID: {{ selectedLog.userId }} · <span class="role-pill" :class="selectedLog.userRole.toLowerCase()">{{ getRoleLabel(selectedLog.userRole) }}</span></span>
          </div>
          <div class="info-row">
            <span class="k">请求 IP 地址</span>
            <span class="v monospace">{{ selectedLog.ipAddress || '127.0.0.1' }}</span>
          </div>
          <div class="info-row">
            <span class="k">发起时间</span>
            <span class="v monospace">{{ selectedLog.createdAt }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h4 class="sec-heading">
          <el-icon class="sec-icon icon-purple"><Cpu /></el-icon>
          <span>AI 模型与调度场景</span>
        </h4>
        <div class="info-list">
          <div class="info-row">
            <span class="k">业务场景 / 工具</span>
            <span class="v">
              <span class="scene-badge" :class="getSceneStyleClass(selectedLog.scene || selectedLog.toolName)">
                {{ getSceneLabel(selectedLog.scene || selectedLog.toolName) }}
              </span>
            </span>
          </div>
          <div class="info-row">
            <span class="k">生成模型</span>
            <span class="v monospace font-bold">{{ selectedLog.model }}</span>
          </div>
          <div class="info-row">
            <span class="k">模型提供方</span>
            <span class="v">{{ selectedLog.provider }}</span>
          </div>
          <div v-if="selectedLog.courseId" class="info-row">
            <span class="k">关联课程 ID</span>
            <span class="v monospace">Course #{{ selectedLog.courseId }}</span>
          </div>
          <div v-if="selectedLog.conversationId" class="info-row">
            <span class="k">关联会话 ID</span>
            <span class="v monospace">{{ selectedLog.conversationId }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h4 class="sec-heading">
          <el-icon class="sec-icon icon-emerald"><DataLine /></el-icon>
          <span>Token 细化度量</span>
        </h4>
        <div class="info-list">
          <div class="info-row">
            <span class="k">输入 Prompt Tokens</span>
            <span class="v monospace">{{ selectedLog.promptTokens }} toks</span>
          </div>
          <div class="info-row">
            <span class="k">输出 Completion Tokens</span>
            <span class="v monospace">{{ selectedLog.completionTokens }} toks</span>
          </div>
          <div class="info-row">
            <span class="k">总计 Tokens</span>
            <span class="v monospace font-bold">{{ selectedLog.totalTokens }} toks</span>
          </div>
          <div class="info-row">
            <span class="k">预估算力成本</span>
            <span class="v text-amber font-bold">¥ {{ selectedLog.estimatedCost.toFixed(4) }} RMB</span>
          </div>
        </div>
      </div>

      <div v-if="selectedLog.knowledgeBaseId || selectedLog.retrievalHitCount" class="detail-section">
        <h4 class="sec-heading">
          <el-icon class="sec-icon icon-cyan"><Reading /></el-icon>
          <span>RAG 知识检索与切片溯源</span>
        </h4>
        <div class="info-list">
          <div class="info-row">
            <span class="k">知识库 ID</span>
            <span class="v monospace">KB #{{ selectedLog.knowledgeBaseId }}</span>
          </div>
          <div class="info-row">
            <span class="k">向量检索命中切片数</span>
            <span class="v font-bold">{{ selectedLog.retrievalHitCount }} 片</span>
          </div>
          <div v-if="selectedLog.citationDocIds" class="info-row">
            <span class="k">引用文档溯源 ID</span>
            <span class="v monospace">{{ selectedLog.citationDocIds }}</span>
          </div>
        </div>
      </div>

      <!-- 原始元数据预览 -->
      <div class="detail-section">
        <div class="json-header">
          <h4 class="sec-heading mb-0">
            <el-icon class="sec-icon icon-amber"><Document /></el-icon>
            <span>链路完整元数据 (JSON)</span>
          </h4>
          <button class="copy-json-btn" @click="$emit('copy-text', JSON.stringify(selectedLog, null, 2))">
            <el-icon><CopyDocument /></el-icon>
            <span>复制 JSON</span>
          </button>
        </div>
        <pre class="json-code-block"><code>{{ JSON.stringify(selectedLog, null, 2) }}</code></pre>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import {
  Cpu,
  CopyDocument,
  View,
  User,
  Reading,
  Document,
  DataLine
} from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { AIAuditLog } from '@/types/system/audit';
import {
  getRoleLabel,
  getSceneLabel,
  getStatusLabel,
  getSceneStyleClass,
  getLatencyClass,
  getUserAvatarUrl
} from '@/composables/system/useAudit';

const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });
const detailDrawerVisible = defineModel<boolean>('detailDrawerVisible', { required: true });

defineProps<{
  logs: AIAuditLog[];
  tableLoading: boolean;
  total: number;
  selectedLog: AIAuditLog | null;
}>();

defineEmits<{
  'load-logs': [];
  'view-detail': [row: AIAuditLog];
  'copy-text': [text: string];
}>();
</script>

<style scoped lang="scss">
.table-wrap {
  padding: 0 8px;

  .trace-cell {
    display: flex;
    align-items: center;
    gap: 6px;

    .trace-badge {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 12px;
      font-weight: 600;
      color: #334155;
      background: #F1F5F9;
      padding: 3px 10px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
    }

    .copy-btn {
      background: transparent;
      border: none;
      color: #94A3B8;
      cursor: pointer;
      padding: 2px;
      display: flex;
      align-items: center;
      border-radius: 4px;
      transition: all 0.2s;

      &:hover {
        color: #2563EB;
        background: #EFF6FF;
      }
    }
  }

  .user-info-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .em-user-avatar-sm {
      flex-shrink: 0;
      background: #EFF6FF;
      border: 1px solid #DBEAFE;
    }

    .user-text {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .user-name {
        font-size: 13px;
        font-weight: 600;
        color: #0F172A;
        line-height: 1.2;
      }

      .user-sub {
        display: flex;
        align-items: center;
        gap: 6px;

        .user-account {
          font-size: 11px;
          color: #94A3B8;
          font-family: monospace;
        }
      }
    }
  }

  .role-pill {
    font-size: 10px;
    font-weight: 600;
    padding: 1px 6px;
    border-radius: 9999px;
    line-height: 1.2;

    &.admin { background: #FEE2E2; color: #DC2626; }
    &.teacher { background: #EFF6FF; color: #2563EB; }
    &.student { background: #ECFDF5; color: #059669; }
    &.user { background: #F1F5F9; color: #475569; }
  }

  .scene-tag-wrap {
    .scene-badge {
      display: inline-flex;
      align-items: center;
      white-space: nowrap;
      font-size: 11.5px;
      font-weight: 600;
      padding: 3px 10px;
      border-radius: 9999px;
      line-height: 1.35;
      background: #F8FAFC;
      color: #475569;
      border: 1px solid #E2E8F0;

      &.badge-chat { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
      &.badge-prep { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
      &.badge-rag { background: #F0FDFA; color: #0D9488; border: 1px solid #99F6E4; }
      &.badge-grading { background: #EEF2FF; color: #4F46E5; border: 1px solid #C7D2FE; }
      &.badge-agent { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
      &.badge-question { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
      &.badge-memory { background: #F5F3FF; color: #7C3AED; border: 1px solid #DDD6FE; }
      &.badge-advice { background: #ECFEFF; color: #0891B2; border: 1px solid #A5F3FC; }
      &.badge-default { background: #F8FAFC; color: #475569; border: 1px solid #E2E8F0; }
    }
  }

  .model-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-family: monospace;
    font-size: 12px;
    color: #334155;
    background: #F8FAFC;
    padding: 4px 12px;
    border-radius: 9999px;
    border: 1px solid #E2E8F0;
    transition: all 0.2s;

    &:hover {
      background: #F1F5F9;
      border-color: #CBD5E1;
    }

    .model-icon {
      font-size: 13px;
      color: #64748B;
    }
  }

  .tokens-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .tokens-main {
      display: flex;
      align-items: baseline;
      gap: 8px;

      .token-sum {
        font-size: 13px;
        font-weight: 700;
        color: #0F172A;
        font-family: monospace;
      }

      .cost-val {
        font-size: 11px;
        font-weight: 600;
        color: #D97706;
        font-family: monospace;
      }
    }

    .tokens-split {
      font-size: 11px;
      color: #94A3B8;
    }
  }

  .latency-badge {
    font-family: monospace;
    font-size: 11.5px;
    font-weight: 700;
    padding: 2px 10px;
    border-radius: 9999px;

    &.lat-fast { background: #ECFDF5; color: #059669; }
    &.lat-normal { background: #EFF6FF; color: #2563EB; }
    &.lat-warning { background: #FFFBEB; color: #D97706; }
    &.lat-danger { background: #FEF2F2; color: #DC2626; }
  }

  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 9999px;
    white-space: nowrap;
    flex-shrink: 0;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &.success {
      background: #ECFDF5;
      color: #059669;
      .status-dot { background: #10B981; }
    }

    &.timeout {
      background: #FFFBEB;
      color: #D97706;
      .status-dot { background: #F59E0B; }
    }

    &.failed {
      background: #FEF2F2;
      color: #DC2626;
      .status-dot { background: #EF4444; }
    }
  }

  .table-detail-btn {
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    color: #2563EB;
    padding: 4px 10px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: all 0.2s;

    &:hover {
      background: #2563EB;
      border-color: #2563EB;
      color: #FFFFFF;
    }
  }
}

.pagination-footer {
  padding: 14px 20px 6px;
  border-top: 1px solid #F1F5F9;
  display: flex;
  justify-content: flex-start !important;
  align-items: center;

  :deep(.el-pagination) {
    justify-content: flex-start !important;
    display: flex;
    align-items: center;
  }
}

:deep(.audit-detail-drawer) {
  .el-drawer__header {
    margin-bottom: 16px;
    padding: 20px 24px 16px;
    border-bottom: 1px solid #F1F5F9;
    font-size: 16px;
    font-weight: 700;
    color: #0F172A;
  }

  .el-drawer__body {
    padding: 0 24px 24px;
  }
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .role-pill {
    font-size: 10px;
    font-weight: 600;
    padding: 1px 6px;
    border-radius: 9999px;
    line-height: 1.2;

    &.admin { background: #FEE2E2; color: #DC2626; }
    &.teacher { background: #EFF6FF; color: #2563EB; }
    &.student { background: #ECFDF5; color: #059669; }
    &.user { background: #F1F5F9; color: #475569; }
  }

  .scene-badge {
    display: inline-flex;
    align-items: center;
    white-space: nowrap;
    font-size: 11.5px;
    font-weight: 600;
    padding: 3px 10px;
    border-radius: 9999px;
    line-height: 1.35;
    background: #F8FAFC;
    color: #475569;
    border: 1px solid #E2E8F0;

    &.badge-chat { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
    &.badge-prep { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
    &.badge-rag { background: #F0FDFA; color: #0D9488; border: 1px solid #99F6E4; }
    &.badge-grading { background: #EEF2FF; color: #4F46E5; border: 1px solid #C7D2FE; }
    &.badge-agent { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
    &.badge-question { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
    &.badge-memory { background: #F5F3FF; color: #7C3AED; border: 1px solid #DDD6FE; }
    &.badge-advice { background: #ECFEFF; color: #0891B2; border: 1px solid #A5F3FC; }
    &.badge-default { background: #F8FAFC; color: #475569; border: 1px solid #E2E8F0; }
  }

  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 9999px;
    white-space: nowrap;
    flex-shrink: 0;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &.success {
      background: #ECFDF5;
      color: #059669;
      .status-dot { background: #10B981; }
    }

    &.timeout {
      background: #FFFBEB;
      color: #D97706;
      .status-dot { background: #F59E0B; }
    }

    &.failed {
      background: #FEF2F2;
      color: #DC2626;
      .status-dot { background: #EF4444; }
    }
  }

  .drawer-hero-card {
    background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%);
    border: 1px solid #DBEAFE;
    border-radius: 14px;
    padding: 16px 20px;

    .hero-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;

      .trace-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .label { font-size: 12px; color: #64748B; }
        .val { font-family: monospace; font-size: 14px; font-weight: 700; color: #1E293B; }
      }
    }

    .hero-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;

      .hero-item {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .hi-label { font-size: 11.5px; color: #64748B; }
        .hi-val {
          font-size: 17px;
          font-weight: 700;
          font-family: monospace;

          &.text-purple { color: #9333EA; }
          &.text-emerald { color: #059669; }
          &.text-amber { color: #D97706; }
        }
      }
    }
  }

  .detail-section {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 16px 18px;

    .sec-heading {
      font-size: 13.5px;
      font-weight: 700;
      color: #0F172A;
      margin: 0 0 12px 0;
      display: flex;
      align-items: center;
      gap: 7px;

      &.mb-0 { margin-bottom: 0; }

      .sec-icon {
        font-size: 16px;

        &.icon-blue { color: #2563EB; }
        &.icon-purple { color: #9333EA; }
        &.icon-emerald { color: #059669; }
        &.icon-cyan { color: #0891B2; }
        &.icon-amber { color: #D97706; }
      }
    }

    .info-list {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .info-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 12.5px;

        .k { color: #64748B; }
        .v {
          color: #1E293B;
          text-align: right;

          &.font-bold { font-weight: 600; }
          &.monospace { font-family: monospace; }
          &.text-amber { color: #D97706; }
        }

        .user-flex-item {
          display: inline-flex;
          align-items: center;
          gap: 6px;
        }
      }
    }

    .json-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .copy-json-btn {
        background: #F1F5F9;
        border: 1px solid #E2E8F0;
        color: #475569;
        padding: 3px 8px;
        border-radius: 6px;
        font-size: 11.5px;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 4px;

        &:hover {
          background: #E2E8F0;
          color: #0F172A;
        }
      }
    }

    .json-code-block {
      margin: 0;
      background: #0F172A;
      color: #E2E8F0;
      border-radius: 8px;
      padding: 12px 14px;
      font-family: monospace;
      font-size: 11.5px;
      line-height: 1.5;
      max-height: 220px;
      overflow-y: auto;
    }
  }
}
</style>
