<template>
  <div v-show="active" class="tab-pane-content">
    <!-- 筛选控制条：搜索框变小，重置紧贴查询右侧 -->
    <div class="table-header-box">
      <div class="filter-controls-row">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          size="small"
          class="filter-date-picker"
          @change="emit('filter-change')"
        />

        <el-select
          v-model="sceneFilter"
          placeholder="算力消费场景"
          clearable
          size="small"
          class="filter-select"
          @change="emit('filter-change')"
        >
          <el-option label="全部场景" value="" />
          <el-option label="智能对话 (CHAT)" value="CHAT" />
          <el-option label="知识增强问答 (CHAT_RAG)" value="CHAT_RAG" />
          <el-option label="智能备课 (PREP)" value="PREP" />
          <el-option label="知识库问答 (RAG)" value="RAG" />
          <el-option label="作业批改 (GRADING)" value="GRADING" />
          <el-option label="AI 出题 (QUESTION)" value="question_generate" />
          <el-option label="智能体协作 (AGENT)" value="AGENT" />
          <el-option label="全局智能助手" value="GLOBAL_ASSISTANT" />
        </el-select>

        <el-select
          v-model="modelFilter"
          :placeholder="modelOptions.length ? '模型计费规格' : '暂无可用模型'"
          :disabled="!modelOptions.length"
          clearable
          size="small"
          class="filter-select"
          @change="emit('filter-change')"
        >
          <el-option label="全部模型规格" value="" />
          <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
        </el-select>

        <el-input
          v-model="searchKeyword"
          placeholder="搜索师生/账号/Trace ID"
          clearable
          size="small"
          class="filter-search-input"
          :prefix-icon="Search"
          @keyup.enter="emit('search')"
          @clear="emit('search')"
        />

        <!-- 查询与重置按钮并排锁定在一组 -->
        <div class="filter-actions-group">
          <el-button
            type="primary"
            size="small"
            class="action-pill-btn primary"
            :disabled="tableLoading"
            @click="emit('search')"
          >
            <el-icon><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button
            round
            size="small"
            class="btn-refresh"
            :disabled="tableLoading"
            @click="emit('reset')"
          >
            <el-icon class="mr-1" :class="{ 'is-loading': tableLoading }"><Refresh /></el-icon>
            <span>重置</span>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 算力抵扣统计胶囊条 -->
    <div class="summary-ribbon">
      <div class="ribbon-item">
        <span class="ribbon-label">本页抵扣 Token 合计</span>
        <span class="ribbon-val text-primary">{{ pageTotalTokens.toLocaleString() }} <span class="unit">toks</span></span>
      </div>
      <div class="ribbon-divider"></div>
      <div class="ribbon-item">
        <span class="ribbon-label">预估算力支出折算</span>
        <span class="ribbon-val text-amber">¥ {{ pageEstimatedCost.toFixed(4) }}</span>
      </div>
      <div class="ribbon-divider"></div>
      <div class="ribbon-item">
        <span class="ribbon-label">租户剩余总算力池</span>
        <span class="ribbon-val text-emerald">{{ (Math.max(0, quotas.tokenLimit - quotas.tokenUsed) / 10000).toFixed(1) }} <span class="unit">万 toks (余 {{ (100 - tokenPercentage).toFixed(1) }}%)</span></span>
      </div>
      <div class="ribbon-divider"></div>
      <div class="ribbon-item">
        <span class="ribbon-label">扣减执行状态</span>
        <span class="ribbon-val text-success">100% 成功扣减</span>
      </div>
    </div>

    <!-- 算力扣减账单流水表格 -->
    <div class="table-wrap">
      <el-table
        v-loading="tableLoading"
        element-loading-text="正在检索租户算力扣减账单流水..."
        :data="auditLogs"
        stripe
        style="width: 100%;"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '46px' }"
      >
        <!-- 扣减单号 / 时间 -->
        <el-table-column label="扣减单号 / 时间" width="180">
          <template #default="{ row }">
            <div class="ledger-bill-cell">
              <span class="bill-no font-mono">{{ row.traceId || `QTA-${row.id}` }}</span>
              <span class="time-sub font-mono">{{ row.createdAt || '刚刚' }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 消费主体 (师生/部门) -->
        <el-table-column label="算力消费主体 (师生)" min-width="170">
          <template #default="{ row }">
            <div class="caller-profile-cell">
              <el-avatar :size="30" :src="getUserAvatarUrl(row)" class="user-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-meta">
                <span class="real-name">{{ row.realName || row.username }}</span>
                <div class="sub-meta">
                  <span class="user-tag">@{{ row.username }}</span>
                  <span class="role-pill" :class="(row.userRole || 'USER').toLowerCase()">
                    {{ getRoleLabel(row.userRole) }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 算力资源类型 -->
        <el-table-column label="算力资源类型" width="130">
          <template #default>
            <span class="resource-pill">
              <el-icon><Coin /></el-icon>
              <span>Token 算力</span>
            </span>
          </template>
        </el-table-column>

        <!-- 消费场景与规格 -->
        <el-table-column label="消费场景 / 模型规格" min-width="170">
          <template #default="{ row }">
            <div class="scenario-spec-cell">
              <span class="scene-pill" :class="getSceneStyleClass(row.scene || row.toolName)">
                {{ getSceneLabel(row.scene || row.toolName) }}
              </span>
              <span class="model-spec font-mono">{{ row.model }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 扣减算力额度 -->
        <el-table-column label="本次扣减算力" min-width="150">
          <template #default="{ row }">
            <div class="token-metric-cell">
              <span class="deduct-amount font-mono">-{{ row.totalTokens.toLocaleString() }}</span>
              <div class="token-split font-mono">
                入: {{ row.promptTokens }} · 出: {{ row.completionTokens }}
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 算力折算成本 -->
        <el-table-column label="算力折算" width="110">
          <template #default="{ row }">
            <span class="cost-tag font-mono">¥ {{ row.estimatedCost.toFixed(4) }}</span>
          </template>
        </el-table-column>

        <!-- 扣减状态 -->
        <el-table-column label="扣减状态" width="100">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 'SUCCESS' ? 'success' : 'danger'">
              <span class="status-dot"></span>
              {{ row.status === 'SUCCESS' ? '扣减成功' : '超额阻断' }}
            </span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="配额凭单" width="90" fixed="right">
          <template #default="{ row }">
            <button type="button" class="table-action-pill" @click="emit('view-detail', row)">
              <el-icon><View /></el-icon>
              <span>凭单</span>
            </button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页器（严格对标用户管理规范） -->
    <div class="pagination-footer">
      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @change="emit('load-audit-logs')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh, User, Coin, View } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { AIAuditLog } from '@/types/system/audit';
import type { TenantQuotasState } from '@/composables/system/useTenantQuota';
import { getUserAvatarUrl, getRoleLabel, getSceneLabel, getSceneStyleClass } from '@/composables/system/useTenantQuota';

defineProps<{
  active: boolean;
  quotas: TenantQuotasState;
  tokenPercentage: number;
  auditLogs: AIAuditLog[];
  tableLoading: boolean;
  total: number;
  modelOptions: string[];
  pageTotalTokens: number;
  pageEstimatedCost: number;
}>();

const dateRange = defineModel<[string, string] | null>('dateRange', { required: true });
const sceneFilter = defineModel<string>('sceneFilter', { required: true });
const modelFilter = defineModel<string>('modelFilter', { required: true });
const searchKeyword = defineModel<string>('searchKeyword', { required: true });
const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });

const emit = defineEmits<{
  'filter-change': [];
  search: [];
  reset: [];
  'view-detail': [row: AIAuditLog];
  'load-audit-logs': [];
}>();
</script>
