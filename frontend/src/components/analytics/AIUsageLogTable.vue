<template>
  <div class="ai-usage-log-card">
    <div class="table-header">
      <div class="header-left">
        <h3 class="title">AI 调用明细审计流水</h3>
        <span class="subtitle">记录每一次师生与智教大模型交互的算力消耗、耗时与业务场景轨迹</span>
      </div>

      <div class="header-filters">
        <!-- 场景筛选（长圆胶囊） -->
        <el-select
          v-model="selectedScene"
          placeholder="全部场景"
          clearable
          class="capsule-select"
          style="width: 170px"
          @change="handleFilterChange"
        >
          <el-option label="全部场景" value="" />
          <el-option label="智能答疑解惑" value="CHAT" />
          <el-option label="试题精准批阅" value="GRADING" />
          <el-option label="靶向变式推演" value="QUESTION_GENERATE" />
          <el-option label="学情诊断评估" value="LEARNING" />
          <el-option label="教学备课辅助" value="LESSON_PLAN" />
        </el-select>

        <!-- 模型名称筛选（长圆胶囊） -->
        <el-input
          v-model="modelKeyword"
          placeholder="按模型名称过滤..."
          clearable
          class="capsule-input"
          style="width: 190px"
          :prefix-icon="Search"
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        />
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="logs"
      stripe
      class="log-table"
      empty-text="当前筛选条件下暂无 AI 调用明细"
    >
      <el-table-column prop="createTime" label="调用时间" width="140">
        <template #default="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="调用人" min-width="140">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar
              :size="30"
              :src="row.avatar"
              class="user-avatar"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="user-info">
              <span class="user-name" :title="row.realName || row.username || (row.userId ? `用户 #${row.userId}` : '系统/匿名')">
                {{ row.realName || row.username || (row.userId ? `用户 #${row.userId}` : '系统/匿名') }}
              </span>
              <span v-if="row.username" class="user-account">@{{ row.username }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="应用场景" min-width="160">
        <template #default="{ row }">
          <span class="scene-tag" :class="getSceneTagClass(row.scene)">
            {{ row.sceneLabel || getSceneFallbackName(row.scene) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="model" label="大模型 / 引擎" min-width="170">
        <template #default="{ row }">
          <span class="model-name font-mono">{{ row.model || '通用教学模型' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="Token 消耗" min-width="170">
        <template #default="{ row }">
          <div class="token-cell">
            <strong class="total-tokens">{{ (row.totalTokens ?? (row.promptTokens || 0) + (row.completionTokens || 0)).toLocaleString() }}</strong>
            <span class="token-split">
              (输入: {{ row.promptTokens ?? 0 }} / 输出: {{ row.completionTokens ?? 0 }})
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="响应延迟" width="120">
        <template #default="{ row }">
          <span class="latency-tag" :class="getLatencyClass(row.latencyMs)">
            {{ row.latencyMs ? `${row.latencyMs} ms` : '< 200 ms' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100">
        <template #default>
          <span class="status-tag status-tag--success">
            <span class="status-dot" /> 成功
          </span>
        </template>
      </el-table-column>

      <el-table-column label="审计操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="emit('view-detail', row)">
            审计详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页控件：采用系统统一封装的 AppPagination，并支持字符串数字安全归一 -->
    <div class="table-pagination">
      <span class="total-count-tip">共计 <strong>{{ total }}</strong> 条真实算力交互记录</span>
      <AppPagination
        v-model:page-num="currentPage"
        v-model:page-size="pageSize"
        :total="Number(total) || 0"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, jumper"
        @change="handlePaginationChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Search, User } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { AiCallLogItem } from '@/types/analytics/learning';

const props = withDefaults(
  defineProps<{
    logs?: AiCallLogItem[];
    total?: number | string;
    loading?: boolean;
    pageNum?: number;
    pageSize?: number;
  }>(),
  {
    logs: () => [],
    total: 0,
    loading: false,
    pageNum: 1,
    pageSize: 10
  }
);

const emit = defineEmits<{
  (e: 'query-change', params: { scene?: string; model?: string; pageNum: number; pageSize: number }): void;
  (e: 'view-detail', item: AiCallLogItem): void;
}>();

const selectedScene = ref('');
const modelKeyword = ref('');
const currentPage = ref(props.pageNum);
const pageSize = ref(props.pageSize);

watch(
  () => props.pageNum,
  (val) => {
    if (val && val !== currentPage.value) {
      currentPage.value = val;
    }
  }
);

watch(
  () => props.pageSize,
  (val) => {
    if (val && val !== pageSize.value) {
      pageSize.value = val;
    }
  }
);

function handleFilterChange() {
  currentPage.value = 1;
  emitParams();
}

function handlePaginationChange() {
  emitParams();
}

function emitParams() {
  emit('query-change', {
    scene: selectedScene.value || undefined,
    model: modelKeyword.value.trim() || undefined,
    pageNum: currentPage.value,
    pageSize: pageSize.value
  });
}

function formatTime(str?: string): string {
  if (!str) return '—';
  const normalized = str.replace('T', ' ');
  return normalized.length >= 19 ? normalized.substring(5, 19) : normalized;
}

function getSceneTagClass(scene?: string): string {
  if (!scene) return 'scene--blue';
  const s = scene.toUpperCase();
  if (s.includes('CHAT') || s.includes('RAG') || s.includes('ASSISTANT')) return 'scene--blue';
  if (s.includes('GRADING')) return 'scene--green';
  if (s.includes('QUESTION') || s.includes('EXAM')) return 'scene--purple';
  if (s.includes('LEARNING') || s.includes('EVALUATION') || s.includes('ADVICE')) return 'scene--amber';
  if (s.includes('LESSON') || s.includes('PREP')) return 'scene--cyan';
  return 'scene--blue';
}

function getSceneFallbackName(scene?: string): string {
  if (!scene) return 'AI 智能交互';
  const s = scene.toUpperCase();
  // 会话标题/追问建议属于对话流程内的辅助调用，不能因为包含 CHAT 就冒充"智能答疑解惑"；
  // 场景分布图把它们归入「其他」，明细表如实展示具体场景，避免同一批调用两处口径打架。
  if (s === 'CHAT_TITLE') return '会话标题生成';
  if (s === 'CHAT_FOLLOW_UP') return '追问建议生成';
  if (s.includes('CHAT')) return '智能答疑解惑';
  if (s.includes('GRADING')) return '试题精准批阅';
  if (s.includes('QUESTION')) return '靶向变式推演';
  if (s.includes('LEARNING')) return '学情诊断评估';
  if (s.includes('LESSON')) return '教学备课辅助';
  return scene;
}

function getLatencyClass(latency?: number): string {
  if (!latency || latency < 600) return 'latency--fast';
  if (latency < 2000) return 'latency--normal';
  return 'latency--slow';
}
</script>

<style scoped lang="scss">
.ai-usage-log-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.03);
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;

  .header-left {
    .title {
      margin: 0 0 4px;
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }

    .subtitle {
      font-size: 12.5px;
      color: #94a3b8;
    }
  }

  .header-filters {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    /* 长圆胶囊筛选控件 */
    .capsule-select,
    .capsule-input {
      :deep(.el-input__wrapper),
      :deep(.el-select__wrapper) {
        border-radius: 9999px !important;
        border: 1px solid #e2e8f0;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
        padding: 4px 14px;
        transition: all 0.2s ease;

        &:hover,
        &.is-focus {
          border-color: #3b82f6;
          box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
        }
      }
    }
  }
}

.log-table {
  width: 100%;

  :deep(.el-table__header th) {
    background-color: #f8fafc;
    color: #475569;
    font-weight: 600;
    font-size: 12.5px;
  }

  .time-text {
    font-size: 12.5px;
    color: #475569;
    font-family: monospace;
  }

  .user-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .user-avatar {
      flex-shrink: 0;
      background: #f1f5f9;
      color: #64748b;
      font-size: 13px;
    }

    .user-info {
      display: flex;
      flex-direction: column;
      gap: 1px;
      min-width: 0;

      .user-name {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .user-account {
        font-size: 11px;
        color: #94a3b8;
        font-family: 'JetBrains Mono', monospace;
      }
    }
  }

  .scene-tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 10px;
    border-radius: 9999px;
    font-size: 11px;
    font-weight: 500;
    line-height: 1.4;
    letter-spacing: 0.01em;

    &.scene--blue {
      background: #eff6ff;
      color: #2563eb;
      border: 1px solid #dbeafe;
    }

    &.scene--green {
      background: #ecfdf5;
      color: #059669;
      border: 1px solid #a7f3d0;
    }

    &.scene--purple {
      background: #f5f3ff;
      color: #7c3aed;
      border: 1px solid #ddd6fe;
    }

    &.scene--amber {
      background: #fffbeb;
      color: #d97706;
      border: 1px solid #fef3c7;
    }

    &.scene--cyan {
      background: #ecfeff;
      color: #0891b2;
      border: 1px solid #cffafe;
    }
  }

  .model-name {
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 12px;
    color: #334155;
    font-weight: 500;
  }

  .token-cell {
    display: flex;
    flex-direction: column;

    .total-tokens {
      color: #0f172a;
      font-size: 13px;
    }

    .token-split {
      font-size: 11px;
      color: #94a3b8;
    }
  }

  .latency-tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 9999px;
    font-size: 11px;
    font-weight: 500;

    &.latency--fast {
      background: #f0fdf4;
      color: #16a34a;
    }

    &.latency--normal {
      background: #f0f9ff;
      color: #0284c7;
    }

    &.latency--slow {
      background: #fff7ed;
      color: #ea580c;
    }
  }

  .status-tag {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    font-weight: 500;

    &--success {
      color: #059669;

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: #10b981;
      }
    }
  }
}

.table-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 10px;

  .total-count-tip {
    font-size: 13px;
    color: #64748b;

    strong {
      color: #0f172a;
    }
  }
}
</style>
