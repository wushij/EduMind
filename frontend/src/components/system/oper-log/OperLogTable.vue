<template>
  <div
    v-loading="loading"
    element-loading-text="正在检索审计日志..."
    class="log-table-card"
  >
    <div class="table-toolbar-bar">
      <div class="toolbar-left">
        <span class="table-title">操作日志列表</span>
        <span class="table-badge-count">共 {{ total }} 条记录</span>
      </div>
      <div class="toolbar-right">
        <button
          v-if="selectedRowIds.length > 0"
          type="button"
          class="table-action-pill table-action-pill--danger"
          @click="$emit('batch-delete')"
        >
          <el-icon class="mr-1"><Delete /></el-icon>
          <span>批量删除 ({{ selectedRowIds.length }})</span>
        </button>
        <button
          type="button"
          class="table-action-pill table-action-pill--primary"
          @click="$emit('export-csv')"
        >
          <el-icon class="mr-1"><Download /></el-icon>
          <span>导出日志</span>
        </button>
        <button
          type="button"
          class="table-action-pill table-action-pill--danger"
          @click="$emit('clean')"
        >
          <el-icon class="mr-1"><Delete /></el-icon>
          <span>清空所有日志</span>
        </button>
      </div>
    </div>

    <el-table
      :data="tableData"
      stripe
      class="custom-oper-table"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column prop="id" label="ID" width="70" align="center" />

      <el-table-column label="模块名称" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="module-title-tag">{{ row.title || '系统操作' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作行为摘要" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="summary-cell">
            <span class="summary-text">{{ getActionSummary(row) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="业务类型" width="110" align="center">
        <template #default="{ row }">
          <span :class="['pill-tag', `pill-tag--biz-${row.businessType ?? 0}`]">
            {{ businessTypeLabel(row.businessType) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="requestMethod" label="方式" width="80" align="center">
        <template #default="{ row }">
          <span class="method-badge" :class="getMethodBadgeClass(row.requestMethod)">
            {{ row.requestMethod || 'POST' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="操作人员" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="oper-user-cell">
            <span class="user-name">{{ row.operName || ('用户#' + (row.operUserId || '0')) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="operIp" label="客户端 IP" width="130" align="center">
        <template #default="{ row }">
          <span class="ip-code">{{ row.operIp || '127.0.0.1' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="执行状态" width="115" align="center">
        <template #default="{ row }">
          <span :class="['pill-tag', row.status === 0 ? 'pill-tag--success' : 'pill-tag--danger']">
            <span class="dot"></span>
            {{ row.status === 0 ? '正常执行' : '异常拦截' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="耗时" width="95" align="center">
        <template #default="{ row }">
          <span :class="['cost-time-badge', row.costTime > 1000 ? 'text-amber' : 'text-slate-600']">
            {{ row.costTime }}ms
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="operTime" label="操作时间" width="170" align="center" />

      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-pill-group">
            <button
              type="button"
              class="table-action-pill table-action-pill--primary"
              @click="$emit('open-detail', row)"
            >
              详情
            </button>
            <button
              type="button"
              class="table-action-pill table-action-pill--danger"
              @click="$emit('delete', row)"
            >
              删除
            </button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部分页栏（严格左对齐项目规范） -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="custom-pagination"
        @size-change="$emit('load-data')"
        @current-change="$emit('load-data')"
      />
    </div>
  </div>

  <!-- 4. 日志详细信息长圆对话框（高度对齐 E:\wu-admin，升级视觉体验） -->
  <el-dialog
    v-model="detailVisible"
    title="操作日志详细审计档案"
    width="780px"
    destroy-on-close
    class="custom-oper-detail-dialog"
  >
    <div v-if="detail" class="detail-dialog-body">
      <!-- 顶部操作状态 Alert 警报条 -->
      <div :class="['detail-status-banner', detail.status === 0 ? 'banner--success' : 'banner--danger']">
        <el-icon class="banner-icon">
          <CircleCheck v-if="detail.status === 0" />
          <CircleClose v-else />
        </el-icon>
        <div class="banner-content">
          <div class="banner-title">
            {{ parsedAction || `${businessTypeLabel(detail.businessType)}${detail.title || ''}` }}
          </div>
          <div class="banner-sub">
            操作人：{{ detail.operName }} | 客户端 IP：{{ detail.operIp }} | 耗时：{{ detail.costTime }}ms | 时间：{{ detail.operTime }}
          </div>
        </div>
      </div>

      <!-- 字段变更明细（若有） -->
      <div v-if="parsedDiffItems.length > 0" class="diff-section">
        <div class="section-label">
          <el-icon class="mr-1"><InfoFilled /></el-icon>
          <span>属性值变更明细（字段变动跟踪）</span>
        </div>
        <div class="diff-tags-cloud">
          <span v-for="(item, idx) in parsedDiffItems" :key="idx" class="diff-pill-tag">
            {{ item }}
          </span>
        </div>
      </div>

      <!-- 异常堆栈信息（若有） -->
      <div v-if="detail.errorMsg" class="error-section">
        <div class="section-label text-danger">
          <el-icon class="mr-1"><Warning /></el-icon>
          <span>异常拦截错误信息</span>
        </div>
        <div class="error-box">
          {{ detail.errorMsg }}
        </div>
      </div>

      <!-- 基础元数据网格 -->
      <div class="meta-grid">
        <div class="meta-item">
          <span class="meta-k">模块名称</span>
          <span class="meta-v">{{ detail.title }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-k">业务类型</span>
          <span class="meta-v">{{ businessTypeLabel(detail.businessType) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-k">请求方式</span>
          <span class="meta-v">{{ detail.requestMethod }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-k">响应耗时</span>
          <span class="meta-v text-amber font-semibold">{{ detail.costTime }} ms</span>
        </div>
        <div class="meta-item full-span">
          <span class="meta-k">请求地址</span>
          <span class="meta-v code-text">{{ detail.operUrl }}</span>
        </div>
        <div class="meta-item full-span">
          <span class="meta-k">执行方法</span>
          <span class="meta-v code-text">{{ detail.method }}</span>
        </div>
      </div>

      <!-- 请求入参与出参代码预览卡片 -->
      <div class="payload-cards-grid">
        <!-- 请求入参 -->
        <div class="payload-box">
          <div class="payload-header">
            <span class="box-title">请求参数 (Request Payload)</span>
            <button
              v-if="detail.operParam"
              type="button"
              class="copy-pill-btn"
              @click="$emit('copy-text', formatJson(detail.operParam))"
            >
              <el-icon class="mr-1"><DocumentCopy /></el-icon>
              <span>复制入参</span>
            </button>
          </div>
          <pre class="json-code-block">{{ formatJson(detail.operParam) || '无请求参数' }}</pre>
        </div>

        <!-- 返回结果 -->
        <div class="payload-box">
          <div class="payload-header">
            <span class="box-title">返回响应 (Response Result)</span>
            <button
              v-if="detail.jsonResult"
              type="button"
              class="copy-pill-btn"
              @click="$emit('copy-text', formatJson(detail.jsonResult))"
            >
              <el-icon class="mr-1"><DocumentCopy /></el-icon>
              <span>复制出参</span>
            </button>
          </div>
          <pre class="json-code-block">{{ formatJson(detail.jsonResult) || '无返回出参' }}</pre>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <button type="button" class="pill-btn pill-btn--primary" @click="detailVisible = false">
          关闭详情
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  Delete,
  Download,
  DocumentCopy,
  CircleCheck,
  CircleClose,
  InfoFilled,
  Warning
} from '@element-plus/icons-vue';
import type { OperLogVO, OperLogPageQuery } from '@/types/system/oper-log';
import {
  businessTypeLabel,
  getMethodBadgeClass,
  getActionSummary,
  formatJson
} from '@/composables/system/useAudit';

const detailVisible = defineModel<boolean>('detailVisible', { required: true });
const queryParams = defineModel<OperLogPageQuery>('queryParams', { required: true });

defineProps<{
  loading: boolean;
  total: number;
  tableData: OperLogVO[];
  selectedRowIds: number[];
  detail: Partial<OperLogVO>;
  parsedAction: string;
  parsedDiffItems: string[];
}>();

const emit = defineEmits<{
  'selection-change': [rows: OperLogVO[]];
  'open-detail': [row: OperLogVO];
  delete: [row: OperLogVO];
  'batch-delete': [];
  'export-csv': [];
  clean: [];
  'load-data': [];
  'copy-text': [text: string];
}>();

function onSelectionChange(rows: OperLogVO[]) {
  emit('selection-change', rows);
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.log-table-card {
  background: #ffffff;
  border-radius: $border-radius-xl;
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: $shadow-sm;

  .table-toolbar-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .table-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }

      .table-badge-count {
        font-size: 12px;
        color: #64748b;
        background: #f1f5f9;
        padding: 2px 10px;
        border-radius: 9999px;
        font-weight: 500;
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .module-title-tag {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
  }

  .summary-cell {
    .summary-text {
      font-size: 13px;
      color: #334155;
      font-weight: 500;
    }
  }

  .oper-user-cell {
    .user-name {
      font-size: 13px;
      font-weight: 600;
      color: #2563eb;
    }
  }

  .ip-code {
    font-family: 'JetBrains Mono', monospace, Consolas;
    font-size: 12px;
    color: #64748b;
    background: #f8fafc;
    padding: 2px 6px;
    border-radius: 6px;
  }

  .cost-time-badge {
    font-family: 'JetBrains Mono', monospace;
    font-size: 12px;
    font-weight: 600;
  }

  /* 跑道标签 pill-tag */
  .pill-tag {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 24px;
    padding: 0 10px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    flex-shrink: 0;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &--success {
      background: #ecfdf5;
      color: #059669;
      .dot {
        background: #10b981;
      }
    }

    &--danger {
      background: #fef2f2;
      color: #dc2626;
      .dot {
        background: #ef4444;
      }
    }

    &--biz-1 {
      background: #ecfdf5;
      color: #059669;
    } // 新增
    &--biz-2 {
      background: #eff6ff;
      color: #2563eb;
    } // 修改
    &--biz-3 {
      background: #fef2f2;
      color: #dc2626;
    } // 删除
    &--biz-7 {
      background: #faf5ff;
      color: #7c3aed;
    } // 授权
    &--biz-5,
    &--biz-6 {
      background: #fff7ed;
      color: #ea580c;
    } // 导入导出
    &--biz-8 {
      background: #fdf2f8;
      color: #db2777;
    } // 清空
    &--biz-0 {
      background: #f1f5f9;
      color: #64748b;
    }
  }

  .method-badge {
    display: inline-block;
    font-size: 12px;
    font-weight: 700;
    font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
    padding: 0 !important;

    &.method--get {
      color: #16a34a;
    }
    &.method--post {
      color: #2563eb;
    }
    &.method--put {
      color: #d97706;
    }
    &.method--delete {
      color: #dc2626;
    }
    &.method--other {
      color: #64748b;
    }
  }

  .action-pill-group {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  .pagination-bar {
    display: flex;
    justify-content: flex-start !important;
    align-items: center;
    margin-top: 18px;
    padding-top: 12px;
    border-top: 1px solid #ebf1f7;

    :deep(.el-pagination) {
      justify-content: flex-start !important;
      align-items: center;
      display: flex;
      width: 100%;
    }
  }
}

/* 药丸操作按钮（对齐 UserList.vue 的风格） */
.table-action-pill {
  height: 28px;
  padding: 0 12px;
  border-radius: 9999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;

  &--primary {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #2563eb;
    &:hover {
      background: #2563eb;
      border-color: #2563eb;
      color: #ffffff;
      transform: translateY(-1px);
    }
  }

  &--danger {
    background: #fef2f2;
    border-color: #fecaca;
    color: #dc2626;
    &:hover {
      background: #dc2626;
      border-color: #dc2626;
      color: #ffffff;
      transform: translateY(-1px);
    }
  }
}

/* 跑道药丸胶囊按钮通用样式 */
.pill-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &--primary {
    background: #1677ff;
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
    &:hover {
      background: #4096ff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
    }
  }

  &--default {
    background: #f1f5f9;
    color: #475569;
    border-color: #e2e8f0;
    &:hover {
      background: #e2e8f0;
      color: #1e293b;
    }
  }

  &:disabled {
    opacity: 0.65;
    cursor: not-allowed;
    transform: none !important;
  }
}

/* 4. 详情弹窗自定义样式 */
:deep(.custom-oper-detail-dialog) {
  border-radius: 16px;
  overflow: hidden;

  .el-dialog__header {
    border-bottom: 1px solid #f1f5f9;
    padding: 16px 20px;
    margin-right: 0;
    .el-dialog__title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  .el-dialog__body {
    padding: 20px;
  }

  .el-dialog__footer {
    border-top: 1px solid #f1f5f9;
    padding: 12px 20px;
  }
}

.detail-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .detail-status-banner {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    border-radius: 12px;

    &.banner--success {
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      color: #065f46;
      .banner-icon {
        color: #059669;
        font-size: 26px;
      }
    }

    &.banner--danger {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
      .banner-icon {
        color: #dc2626;
        font-size: 26px;
      }
    }

    .banner-title {
      font-size: 15px;
      font-weight: 700;
      line-height: 1.3;
    }

    .banner-sub {
      font-size: 12px;
      opacity: 0.85;
      margin-top: 2px;
    }
  }

  .diff-section {
    background: #fffbeb;
    border: 1px solid #fde68a;
    border-radius: 12px;
    padding: 12px 16px;

    .section-label {
      font-size: 13px;
      font-weight: 700;
      color: #92400e;
      display: flex;
      align-items: center;
      margin-bottom: 8px;
    }

    .diff-tags-cloud {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .diff-pill-tag {
        background: #ffffff;
        border: 1px solid #fcd34d;
        color: #b45309;
        font-size: 12px;
        font-weight: 600;
        padding: 3px 10px;
        border-radius: 9999px;
      }
    }
  }

  .error-section {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 12px;
    padding: 12px 16px;

    .section-label {
      font-size: 13px;
      font-weight: 700;
      display: flex;
      align-items: center;
      margin-bottom: 6px;
    }

    .error-box {
      font-size: 12px;
      color: #dc2626;
      font-family: monospace;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }

  .meta-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    background: #f8fafc;
    border-radius: 12px;
    padding: 14px 18px;
    border: 1px solid #e2e8f0;

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 2px;

      &.full-span {
        grid-column: span 2;
      }

      .meta-k {
        font-size: 12px;
        color: #64748b;
        font-weight: 500;
      }

      .meta-v {
        font-size: 13px;
        color: #0f172a;
        font-weight: 600;

        &.code-text {
          font-family: monospace;
          color: #2563eb;
          font-weight: 500;
        }
      }
    }
  }

  .payload-cards-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .payload-box {
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      overflow: hidden;

      .payload-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: #f8fafc;
        padding: 8px 14px;
        border-bottom: 1px solid #e2e8f0;

        .box-title {
          font-size: 12px;
          font-weight: 700;
          color: #475569;
        }

        .copy-pill-btn {
          height: 24px;
          padding: 0 10px;
          border-radius: 9999px;
          font-size: 11px;
          font-weight: 600;
          background: #ffffff;
          border: 1px solid #cbd5e1;
          color: #475569;
          display: inline-flex;
          align-items: center;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            border-color: #2563eb;
            color: #2563eb;
          }
        }
      }

      .json-code-block {
        margin: 0;
        padding: 12px 14px;
        background: #ffffff;
        font-family: 'JetBrains Mono', Consolas, monospace;
        font-size: 12px;
        line-height: 1.5;
        max-height: 200px;
        overflow-y: auto;
        white-space: pre-wrap;
        word-break: break-all;
        color: #334155;
      }
    }
  }
}
</style>
