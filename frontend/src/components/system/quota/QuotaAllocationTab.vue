<template>
  <div v-show="active" class="tab-pane-content">
    <!-- 部门配额统计概览胶囊 -->
    <div class="summary-ribbon">
      <div class="ribbon-item">
        <span class="ribbon-label">总已切分配额</span>
        <span class="ribbon-val text-primary">{{ (totalAllocatedTokens / 10000).toFixed(0) }} <span class="unit">万 Tokens</span></span>
      </div>
      <div class="ribbon-divider"></div>
      <div class="ribbon-item">
        <span class="ribbon-label">部门累计已消耗</span>
        <span class="ribbon-val text-amber">{{ (totalAllocatedUsed / 10000).toFixed(1) }} <span class="unit">万 Tokens</span></span>
      </div>
      <div class="ribbon-divider"></div>
      <div class="ribbon-item">
        <span class="ribbon-label">租户未切分机动池</span>
        <span class="ribbon-val text-emerald">{{ Math.max(0, (quotas.tokenLimit - totalAllocatedTokens) / 10000).toFixed(0) }} <span class="unit">万 Tokens</span></span>
      </div>
      <div class="ribbon-divider"></div>
      <div class="ribbon-item">
        <span class="ribbon-label">预警/超额部门数</span>
        <span class="ribbon-val" :class="warningDeptCount > 0 ? 'text-danger' : 'text-success'">
          {{ warningDeptCount }} <span class="unit">个</span>
        </span>
      </div>
    </div>

    <!-- 部门配额筛选工具栏 -->
    <div class="dept-filter-bar">
      <el-input
        v-model="deptSearchKeyword"
        placeholder="搜索学院/教研组/部门名称"
        clearable
        size="small"
        class="filter-search-input"
        :prefix-icon="Search"
      />
      <el-select
        v-model="deptStatusFilter"
        placeholder="配额健康度"
        clearable
        size="small"
        class="filter-select"
      >
        <el-option label="全部状态" value="" />
        <el-option label="配额正常 (<80%)" value="NORMAL" />
        <el-option label="水位预警 (≥80%)" value="WARNING" />
        <el-option label="超额阻断 (≥100%)" value="EXCEEDED" />
      </el-select>
    </div>

    <!-- 部门配额分配表格 -->
    <div class="table-wrap">
      <el-table
        :data="paginatedDeptQuotaList"
        row-key="orgId"
        stripe
        style="width: 100%;"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '46px' }"
      >
        <el-table-column label="院系/组织机构" min-width="190">
          <template #default="{ row }">
            <div class="dept-name-cell">
              <span class="dept-title">{{ row.name }}</span>
              <div class="sub-dept-meta">
                <span class="org-type-badge">{{ row.orgTypeLabel }}</span>
                <span class="campus-sub">{{ row.campusName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- Token 算力配额与消耗水位 -->
        <el-table-column label="AI Token 算力配额及消耗水位" min-width="220">
          <template #default="{ row }">
            <div class="quota-progress-cell">
              <div class="metric-line">
                <span class="used-metric font-mono">{{ (row.tokenUsed / 10000).toFixed(1) }}万</span>
                <span class="limit-metric font-mono">/ {{ (row.tokenLimit / 10000).toFixed(0) }}万</span>
                <span class="pct-badge" :class="row.usagePercent >= 85 ? (row.usagePercent >= 100 ? 'danger' : 'warning') : 'normal'">
                  {{ row.usagePercent }}%
                </span>
              </div>
              <el-progress
                :percentage="Math.min(100, row.usagePercent)"
                :color="row.usagePercent >= 85 ? (row.usagePercent >= 100 ? '#EF4444' : '#F59E0B') : '#2563EB'"
                :stroke-width="7"
                :show-text="false"
                style="margin: 6px 0 3px 0;"
              />
              <div class="remain-line">
                <span>剩余：{{ Math.max(0, ((row.tokenLimit - row.tokenUsed) / 10000)).toFixed(1) }}万 Tokens</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 向量知识库配额 -->
        <el-table-column label="向量库存储配额" min-width="140">
          <template #default="{ row }">
            <div class="sub-quota-item">
              <span class="sub-val font-mono">{{ row.storageUsed }} MB / {{ row.storageLimit }} MB</span>
              <el-progress
                :percentage="Math.min(100, Math.round((row.storageUsed / row.storageLimit) * 100))"
                color="#10B981"
                :stroke-width="5"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>

        <!-- 并发会话席位 -->
        <el-table-column label="Agent 并发席位" min-width="130">
          <template #default="{ row }">
            <div class="seats-item font-mono">
              <span class="active-seats">{{ row.seatsUsed }}</span>
              <span class="total-seats">/ {{ row.seatsLimit }} 席</span>
            </div>
          </template>
        </el-table-column>

        <!-- 配额健康状态 -->
        <el-table-column label="配额状态" width="110">
          <template #default="{ row }">
            <span class="status-pill" :class="row.usagePercent >= 85 ? (row.usagePercent >= 100 ? 'danger' : 'warning') : 'success'">
              <span class="status-dot"></span>
              {{ row.usagePercent >= 100 ? '超额阻断' : (row.usagePercent >= 85 ? '水位预警' : '配额正常') }}
            </span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="算力管控" width="120" fixed="right">
          <template #default="{ row }">
            <button type="button" class="table-action-pill" @click="emit('adjust-dept', row)">
              <el-icon><EditPen /></el-icon>
              <span>调整配额</span>
            </button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页器（对标用户管理规范） -->
    <div class="pagination-footer">
      <AppPagination
        v-model:page-num="deptPageNum"
        v-model:page-size="deptPageSize"
        :total="filteredDeptQuotaList.length"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, EditPen } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { OrgQuotaVO } from '@/types/system/tenant';
import type { TenantQuotasState } from '@/composables/system/useTenantQuota';

defineProps<{
  active: boolean;
  quotas: TenantQuotasState;
  totalAllocatedTokens: number;
  totalAllocatedUsed: number;
  warningDeptCount: number;
  paginatedDeptQuotaList: OrgQuotaVO[];
  filteredDeptQuotaList: OrgQuotaVO[];
}>();

const deptSearchKeyword = defineModel<string>('deptSearchKeyword', { required: true });
const deptStatusFilter = defineModel<string>('deptStatusFilter', { required: true });
const deptPageNum = defineModel<number>('deptPageNum', { required: true });
const deptPageSize = defineModel<number>('deptPageSize', { required: true });

const emit = defineEmits<{
  'adjust-dept': [row: OrgQuotaVO];
}>();
</script>
