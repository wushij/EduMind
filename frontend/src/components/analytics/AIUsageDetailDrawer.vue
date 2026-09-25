<template>
  <el-drawer
    v-model="visible"
    title="AI 调用与算力消耗审计详情"
    size="560px"
    class="ai-usage-detail-drawer"
    destroy-on-close
  >
    <div v-if="logItem" class="detail-container">
      <!-- 头部状态与核心概览 -->
      <div class="summary-hero-card">
        <div class="summary-header">
          <span class="scene-badge">{{ logItem.sceneLabel || logItem.scene || 'AI 教学交互' }}</span>
          <span class="status-pill status-pill--success">
            <span class="status-dot" /> 调优执行完成
          </span>
        </div>
        <div class="summary-meta-id">
          <span class="id-label">审计日志流水号:</span>
          <code>#{{ logItem.id }}</code>
        </div>
      </div>

      <!-- 4 维核心数值卡片 -->
      <div class="metrics-grid">
        <div class="metric-card">
          <span class="metric-label">总消耗 Token</span>
          <span class="metric-val text-primary">{{ logItem.totalTokens?.toLocaleString() ?? 0 }}</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">响应耗时</span>
          <span class="metric-val text-info">{{ logItem.latencyMs ? `${logItem.latencyMs} ms` : '—' }}</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">Prompt Token</span>
          <span class="metric-val">{{ logItem.promptTokens?.toLocaleString() ?? 0 }}</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">Completion Token</span>
          <span class="metric-val">{{ logItem.completionTokens?.toLocaleString() ?? 0 }}</span>
        </div>
      </div>

      <!-- 详细审计属性表格 -->
      <div class="detail-section">
        <h4 class="section-title">算力与模型配置</h4>
        <div class="info-list">
          <div class="info-item">
            <span class="label">模型名称</span>
            <span class="val font-mono font-medium">{{ logItem.model || '标准智教大模型' }}</span>
          </div>
          <div v-if="logItem.modelKey" class="info-item">
            <span class="label">路由规则键 (ModelKey)</span>
            <span class="val font-mono">{{ logItem.modelKey }}</span>
          </div>
          <div class="info-item">
            <span class="label">调用场景码</span>
            <span class="val font-mono">{{ logItem.scene || 'UNKNOWN' }}</span>
          </div>
          <div class="info-item">
            <span class="label">记录时间</span>
            <span class="val">{{ logItem.createTime || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 调用人与身份归属 -->
      <div class="detail-section">
        <h4 class="section-title">调用人与身份归属</h4>
        <div class="info-list">
          <div class="info-item">
            <span class="label">调用人姓名</span>
            <span class="val font-medium">
              {{ logItem.realName || logItem.username || (logItem.userId ? `用户 #${logItem.userId}` : '系统 / 匿名') }}
            </span>
          </div>
          <div v-if="logItem.username" class="info-item">
            <span class="label">系统账号</span>
            <span class="val font-mono">@{{ logItem.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">身份角色</span>
            <span class="val">
              <span class="drawer-role-badge" :class="getRoleClass(logItem.userRole)">
                {{ getRoleName(logItem.userRole) }}
              </span>
            </span>
          </div>
          <div v-if="logItem.userId" class="info-item">
            <span class="label">用户系统 ID</span>
            <span class="val font-mono">#{{ logItem.userId }}</span>
          </div>
        </div>
      </div>

      <!-- 教学业务关联 -->
      <div class="detail-section">
        <h4 class="section-title">教学业务与上下文归属</h4>
        <div class="info-list">
          <div class="info-item">
            <span class="label">关联课程 ID</span>
            <span class="val">{{ logItem.courseId ? `课程 #${logItem.courseId}` : '全局系统能力' }}</span>
          </div>
          <div v-if="logItem.courseName" class="info-item">
            <span class="label">课程名称</span>
            <span class="val font-medium">{{ logItem.courseName }}</span>
          </div>
          <div v-if="logItem.conversationId" class="info-item">
            <span class="label">会话标识 (ConversationId)</span>
            <span class="val font-mono text-xs">{{ logItem.conversationId }}</span>
          </div>
          <div class="info-item">
            <span class="label">知识库增强 (RAG)</span>
            <span class="val">
              {{ logItem.knowledgeBaseId ? `知识库 #${logItem.knowledgeBaseId}` : '未挂载知识库' }}
            </span>
          </div>
          <div v-if="logItem.retrievalHitCount != null && logItem.retrievalHitCount > 0" class="info-item">
            <span class="label">知识检索召回片段数</span>
            <span class="val text-success font-medium">{{ logItem.retrievalHitCount }} 个知识切片</span>
          </div>
          <div v-if="logItem.citationDocIds" class="info-item">
            <span class="label">命中溯源文档</span>
            <span class="val font-mono text-xs">{{ logItem.citationDocIds }}</span>
          </div>
        </div>
      </div>

      <!-- 操作与关闭 -->
      <div class="drawer-footer-actions">
        <el-button type="primary" plain class="btn-close" @click="visible = false">
          关闭详情
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { AiCallLogItem } from '@/types/analytics/learning';

const props = defineProps<{
  modelValue: boolean;
  logItem: AiCallLogItem | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
});

function getRoleName(role?: string): string {
  if (!role) return '平台成员';
  const r = role.toUpperCase();
  if (r.includes('ADMIN')) return '管理员';
  if (r.includes('TEACHER')) return '任课教师';
  if (r.includes('STUDENT')) return '学员';
  return role;
}

function getRoleClass(role?: string): string {
  if (!role) return 'role--default';
  const r = role.toUpperCase();
  if (r.includes('ADMIN')) return 'role--admin';
  if (r.includes('TEACHER')) return 'role--teacher';
  if (r.includes('STUDENT')) return 'role--student';
  return 'role--default';
}
</script>

<style scoped lang="scss">
.ai-usage-detail-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 12px;
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
    border-bottom: 1px solid #f1f5f9;
    padding-bottom: 16px;
  }
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 0 4px 20px;
}

.summary-hero-card {
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%);
  border-radius: 14px;
  border: 1px solid #dbeafe;
  padding: 16px 20px;

  .summary-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .scene-badge {
    display: inline-block;
    font-size: 14px;
    font-weight: 700;
    color: #1d4ed8;
  }

  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 600;
    color: #059669;
    background: #ecfdf5;
    padding: 3px 10px;
    border-radius: 9999px;
    border: 1px solid #a7f3d0;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #10b981;
    }
  }

  .summary-meta-id {
    font-size: 12px;
    color: #64748b;

    code {
      margin-left: 6px;
      font-family: monospace;
      color: #334155;
      font-weight: 600;
    }
  }
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  .metric-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .metric-label {
      font-size: 12px;
      color: #64748b;
    }

    .metric-val {
      font-size: 20px;
      font-weight: 700;
      color: #0f172a;

      &.text-primary {
        color: #2563eb;
      }

      &.text-info {
        color: #0284c7;
      }
    }
  }
}

.detail-section {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 16px;

  .section-title {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 700;
    color: #334155;
    display: flex;
    align-items: center;
    gap: 6px;

    &::before {
      content: '';
      display: inline-block;
      width: 3px;
      height: 14px;
      background: #3b82f6;
      border-radius: 2px;
    }
  }

  .info-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    padding-bottom: 8px;
    border-bottom: 1px dashed #f1f5f9;

    &:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }

    .label {
      color: #64748b;
    }

    .val {
      color: #0f172a;
      text-align: right;
      max-width: 60%;
      word-break: break-all;

      &.font-mono {
        font-family: monospace;
      }

      &.font-medium {
        font-weight: 600;
      }

      &.text-xs {
        font-size: 12px;
      }

      &.text-success {
        color: #059669;
      }

      .drawer-role-badge {
        display: inline-flex;
        align-items: center;
        padding: 2px 8px;
        border-radius: 6px;
        font-size: 11.5px;
        font-weight: 500;

        &.role--teacher {
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #dbeafe;
        }

        &.role--student {
          background: #f0fdf4;
          color: #16a34a;
          border: 1px solid #dcfce7;
        }

        &.role--admin {
          background: #faf5ff;
          color: #9333ea;
          border: 1px solid #f3e8ff;
        }

        &.role--default {
          background: #f8fafc;
          color: #64748b;
          border: 1px solid #e2e8f0;
        }
      }
    }
  }
}

.drawer-footer-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;

  .btn-close {
    border-radius: 10px;
    padding: 8px 24px;
  }
}
</style>
