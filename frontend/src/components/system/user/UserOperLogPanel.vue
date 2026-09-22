<template>
  <el-card shadow="never" class="audit-table-card">
    <!-- 顶部标题与快速入口 -->
    <div class="card-header-flex">
      <div class="header-title-group">
        <h3 class="card-title">
          <el-icon class="mr-1 text-blue-600"><Clock /></el-icon>
          <span>账号近期安全与操作审计日志</span>
        </h3>
        <span class="header-count-chip">共 {{ filteredAuditLogs.length }} 条记录</span>
      </div>

      <div class="header-right-tools">
        <button type="button" class="refresh-log-btn" @click="onRefresh">
          <el-icon><RefreshRight /></el-icon>
          <span>刷新</span>
        </button>
        <button type="button" class="jump-log-center-btn" @click="onJumpOperLog">
          <span>查看全平台操作日志大盘</span>
          <el-icon><Right /></el-icon>
        </button>
      </div>
    </div>

    <!-- 过滤器与检索栏（1:1 模仿图 3 题库多维筛选胶囊） -->
    <div class="audit-filter-section">
      <!-- 结果状态筛选胶囊 -->
      <div class="filter-row">
        <span class="filter-label">结果状态：</span>
        <div class="filter-pills">
          <button
            type="button"
            class="pill-btn"
            :class="{ active: logStatus === 'ALL' }"
            @click="logStatus = 'ALL'"
          >
            全部状态
          </button>
          <button
            type="button"
            class="pill-btn pill-btn--success"
            :class="{ active: logStatus === 'SUCCESS' }"
            @click="logStatus = 'SUCCESS'"
          >
            操作成功
          </button>
          <button
            type="button"
            class="pill-btn pill-btn--danger"
            :class="{ active: logStatus === 'FAILED' }"
            @click="logStatus = 'FAILED'"
          >
            拦截失败
          </button>
        </div>
      </div>

      <!-- 操作模块筛选胶囊 -->
      <div v-if="availableModules.length > 0" class="filter-row">
        <span class="filter-label">涉及模块：</span>
        <div class="filter-pills">
          <button
            type="button"
            class="pill-btn"
            :class="{ active: logModule === 'ALL' }"
            @click="logModule = 'ALL'"
          >
            全部模块
          </button>
          <button
            v-for="mod in availableModules"
            :key="mod"
            type="button"
            class="pill-btn"
            :class="{ active: logModule === mod }"
            @click="logModule = mod"
          >
            {{ mod }}
          </button>
        </div>
      </div>

      <!-- 搜索栏与辅助控制 -->
      <div class="filter-search-row">
        <el-input
          v-model="logKeyword"
          placeholder="搜索操作行为说明、所属模块或客户端 IP..."
          clearable
          size="default"
          class="audit-search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <span v-if="logKeyword" class="search-hint">已根据「{{ logKeyword }}」过滤</span>
      </div>
    </div>

    <!-- 审计日志列表表格 -->
    <el-table
      v-loading="auditLogsLoading"
      :data="filteredAuditLogs"
      stripe
      class="audit-table"
      empty-text="暂无匹配的安全操作或审计记录"
    >
      <el-table-column label="操作时间" prop="time" width="190">
        <template #default="{ row }">
          <div class="time-cell">
            <el-icon class="mr-1 text-slate-400"><Clock /></el-icon>
            <span class="font-mono">{{ row.time }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作模块" prop="module" width="160">
        <template #default="{ row }">
          <span class="module-chip" :class="getModuleChipClass(row.module)">
            {{ row.module || '系统运维' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="操作行为说明" prop="action" min-width="240">
        <template #default="{ row }">
          <span class="action-desc-text">{{ row.action }}</span>
        </template>
      </el-table-column>

      <el-table-column label="客户端 IP" prop="ip" width="150">
        <template #default="{ row }">
          <span class="ip-chip">{{ row.ip || '127.0.0.1' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="耗时" width="110">
        <template #default="{ row }">
          <span class="cost-time-badge" :class="getCostTimeClass(row.costTime)">
            {{ row.costTime ?? 0 }}ms
          </span>
        </template>
      </el-table-column>

      <el-table-column label="结果状态" width="130" align="center">
        <template #default="{ row }">
          <span
            class="status-result-badge"
            :class="row.status === 'SUCCESS' ? 'status-result-badge--success' : 'status-result-badge--failed'"
          >
            <span class="dot" />
            <span>{{ row.status === 'SUCCESS' ? '操作成功' : '拦截失败' }}</span>
          </span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { Clock, RefreshRight, Right, Search } from '@element-plus/icons-vue';

const logKeyword = defineModel<string>('logKeyword', { required: true });
const logStatus = defineModel<'ALL' | 'SUCCESS' | 'FAILED'>('logStatus', { required: true });
const logModule = defineModel<string>('logModule', { required: true });

defineProps<{
  filteredAuditLogs: Array<Record<string, any>>;
  availableModules: string[];
  auditLogsLoading: boolean;
  onRefresh: () => void;
  onJumpOperLog: () => void;
}>();

function getModuleChipClass(module: string): string {
  if (!module) return 'module-chip--default';
  if (module.includes('用户') || module.includes('权限')) return 'module-chip--blue';
  if (module.includes('OCR') || module.includes('AI') || module.includes('模型')) return 'module-chip--purple';
  if (module.includes('课程') || module.includes('题库')) return 'module-chip--green';
  return 'module-chip--default';
}

function getCostTimeClass(costTime: number): string {
  const t = Number(costTime) || 0;
  if (t < 50) return 'cost-fast';
  if (t < 200) return 'cost-normal';
  return 'cost-slow';
}
</script>

<style scoped lang="scss">
.audit-table-card {
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
  padding: 20px 24px;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 18px rgba(15, 23, 42, 0.05);
  }

  .card-header-flex {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f1f5f9;

    .header-title-group {
      display: flex;
      align-items: center;
      gap: 12px;

      .card-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
        display: flex;
        align-items: center;
      }

      .header-count-chip {
        font-size: 12px;
        background: #f1f5f9;
        color: #64748b;
        padding: 2px 8px;
        border-radius: 9999px;
        font-weight: 600;
      }
    }

    .header-right-tools {
      display: flex;
      align-items: center;
      gap: 10px;

      .refresh-log-btn {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 5px 12px;
        border-radius: 9999px;
        background: #ffffff;
        border: 1px solid #cbd5e1;
        color: #475569;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: #f8fafc;
          color: #0f172a;
          border-color: #94a3b8;
        }
      }

      .jump-log-center-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        color: #2563eb;
        font-size: 12.5px;
        font-weight: 600;
        padding: 5px 14px;
        border-radius: 9999px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #2563eb;
          color: #ffffff;
        }
      }
    }
  }

  /* 过滤器区域（对标图 3 风格） */
  .audit-filter-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 14px 18px;
    margin-bottom: 18px;

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .filter-label {
        font-size: 12.5px;
        color: #64748b;
        font-weight: 600;
        width: 72px;
        flex-shrink: 0;
      }

      .filter-pills {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .pill-btn {
          border: 1px solid #e2e8f0;
          background: #ffffff;
          color: #475569;
          font-size: 12px;
          font-weight: 500;
          padding: 4px 14px;
          border-radius: 9999px;
          cursor: pointer;
          transition: all 0.15s ease;

          &:hover {
            border-color: #cbd5e1;
            color: #0f172a;
          }

          &.active {
            background: #2563eb;
            color: #ffffff;
            border-color: #2563eb;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
          }

          &--success.active {
            background: #059669;
            border-color: #059669;
            box-shadow: 0 2px 8px rgba(5, 150, 105, 0.25);
          }

          &--danger.active {
            background: #dc2626;
            border-color: #dc2626;
            box-shadow: 0 2px 8px rgba(220, 38, 38, 0.25);
          }
        }
      }
    }

    .filter-search-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-top: 6px;
      border-top: 1px dashed #e2e8f0;

      .audit-search-input {
        max-width: 420px;

        :deep(.el-input__wrapper) {
          border-radius: 9999px;
        }
      }

      .search-hint {
        font-size: 12px;
        color: #2563eb;
      }
    }
  }

  /* 表格内元素美化 */
  .audit-table {
    border-radius: 12px;
    overflow: hidden;

    .time-cell {
      display: flex;
      align-items: center;
      font-size: 12.5px;
      color: #475569;
    }

    .module-chip {
      font-size: 11.5px;
      font-weight: 600;
      padding: 2px 9px;
      border-radius: 9999px;

      &--blue {
        background: #eff6ff;
        color: #2563eb;
        border: 1px solid #bfdbfe;
      }

      &--purple {
        background: #f5f3ff;
        color: #7c3aed;
        border: 1px solid #ddd6fe;
      }

      &--green {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
      }

      &--default {
        background: #f1f5f9;
        color: #475569;
        border: 1px solid #e2e8f0;
      }
    }

    .action-desc-text {
      font-size: 13px;
      color: #1e293b;
      font-weight: 500;
    }

    .ip-chip {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 12px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      padding: 2px 8px;
      border-radius: 6px;
      color: #475569;
    }

    .cost-time-badge {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;

      &.cost-fast {
        background: #ecfdf5;
        color: #059669;
      }

      &.cost-normal {
        background: #eff6ff;
        color: #2563eb;
      }

      &.cost-slow {
        background: #fffbeb;
        color: #d97706;
      }
    }

    .status-result-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      font-weight: 600;
      padding: 2px 10px;
      border-radius: 9999px;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      &--success {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;

        .dot {
          background: #10b981;
        }
      }

      &--failed {
        background: #fef2f2;
        color: #dc2626;
        border: 1px solid #fecaca;

        .dot {
          background: #ef4444;
        }
      }
    }
  }
}
</style>
