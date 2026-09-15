<template>
  <div class="key-management-table-card">
    <!-- 筛选控制条：搜索框紧凑，重置紧贴查询右侧 -->
    <div class="table-header-box">
      <div class="filter-controls-row">
        <el-select
          v-model="aliasFilter"
          placeholder="密钥资产别名"
          clearable
          size="small"
          class="filter-select"
          @change="handleFilterChange"
        >
          <el-option label="全部密钥资产别名" value="" />
          <el-option label="edumind-data-key (教学沙箱数据主密钥)" value="edumind-data-key" />
          <el-option label="edumind-model-key (AI模型中枢凭证密钥)" value="edumind-model-key" />
        </el-select>

        <el-select
          v-model="statusFilter"
          placeholder="版本生命周期状态"
          clearable
          size="small"
          class="filter-select"
          @change="handleFilterChange"
        >
          <el-option label="全部状态" value="" />
          <el-option label="生效中 (ACTIVE)" value="ACTIVE" />
          <el-option label="已弃用归档 (DEPRECATED)" value="DEPRECATED" />
        </el-select>

        <el-input
          v-model="searchKeyword"
          placeholder="搜索密钥别名 / 指纹 / 算法"
          clearable
          size="small"
          class="filter-search-input"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />

        <!-- 查询与重置按钮并排锁定在一组 -->
        <div class="filter-actions-group">
          <el-button
            type="primary"
            size="small"
            class="action-pill-btn primary"
            :disabled="loading"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button
            round
            size="small"
            class="btn-refresh"
            :disabled="loading"
            @click="handleReset"
          >
            <el-icon class="mr-1" :class="{ 'is-loading': loading }"><Refresh /></el-icon>
            <span>重置</span>
          </el-button>
        </div>
      </div>

      <!-- 右侧轻量统计提示 -->
      <div class="toolbar-meta">
        <span class="meta-item">共托管 <strong>{{ keyList.length }}</strong> 个密钥版本流水记录</span>
      </div>
    </div>

    <!-- 密钥版本台账数据表格 -->
    <div
      class="table-wrap"
      v-loading="loading"
      element-loading-text="正在检索国密密钥资产流水..."
    >
      <el-table
        :data="paginatedKeyList"
        row-key="id"
        stripe
        style="width: 100%;"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '46px' }"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" header-align="center" />

        <!-- 密钥别名与应用场景 -->
        <el-table-column label="密钥资产别名 / 保护业务场景" min-width="260">
          <template #default="{ row }">
            <div class="key-alias-cell">
              <div class="alias-top">
                <span class="font-mono font-bold alias-title">{{ row.keyAlias }}</span>
                <el-tag
                  size="small"
                  :type="row.keyAlias?.includes('model') ? 'purple' : 'primary'"
                  effect="plain"
                  class="alias-pill"
                >
                  {{ row.keyAlias?.includes('model') ? 'AI模型中枢' : '数据沙箱' }}
                </el-tag>
              </div>
              <span class="usage-scope-desc">{{ row.usageScope || '学生错题 / 认知画像 / 对话切片 / 敏感学情' }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 版本号 -->
        <el-table-column label="版本号" width="90" align="center" header-align="center">
          <template #default="{ row }">
            <div class="version-tag-box">
              <span class="ver-pill font-mono" :class="row.status === 'ACTIVE' ? 'active' : 'deprecated'">
                v{{ row.keyVersion }}
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 加密算法 -->
        <el-table-column label="国密算法标准" width="140" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small" effect="light" class="algorithm-tag">
              <el-icon class="mr-1"><Lock /></el-icon>
              <span>{{ row.algorithm }}</span>
            </el-tag>
          </template>
        </el-table-column>

        <!-- 安全指纹 (Fingerprint) -->
        <el-table-column label="派生安全指纹 (Fingerprint)" min-width="210" align="center" header-align="center">
          <template #default="{ row }">
            <div class="table-fingerprint font-mono" @click="copyText(row.keyFingerprint)">
              <span>{{ row.keyFingerprint }}</span>
              <el-icon class="copy-icon"><CopyDocument /></el-icon>
            </div>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="生命周期状态" width="180" align="center" header-align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 'ACTIVE' ? 'success' : 'muted'">
              <span class="status-dot"></span>
              {{ row.status === 'ACTIVE' ? '生效中 (ACTIVE)' : '已弃用 (DEPRECATED)' }}
            </span>
          </template>
        </el-table-column>

        <!-- 激活生效时间 -->
        <el-table-column label="激活生效时间" width="170" align="center" header-align="center">
          <template #default="{ row }">
            <span class="font-mono text-secondary text-xs">{{ row.activatedTime || row.createTime || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="安全管控" width="230" align="center" header-align="center" fixed="right">
          <template #default="{ row }">
            <div class="table-actions-flex">
              <button type="button" class="table-action-pill" @click="openCryptoTestModal(row)">
                <el-icon><Connection /></el-icon>
                <span>自检</span>
              </button>
              <button type="button" class="table-action-pill secondary" @click="openDetailDrawer(row)">
                <el-icon><Document /></el-icon>
                <span>详情</span>
              </button>
              <button
                v-if="row.status === 'ACTIVE'"
                type="button"
                class="table-action-pill warning"
                @click="openRotateModal(row.keyAlias)"
              >
                <el-icon><RefreshRight /></el-icon>
                <span>轮换</span>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页器（对标用户管理标准分页交互） -->
    <div class="pagination-footer">
      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="filteredKeyList.length"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Lock,
  Refresh,
  RefreshRight,
  Search,
  CopyDocument,
  Connection,
  Document
} from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { SecurityKeyVersionVO } from '@/types/system/security-key';

const aliasFilter = defineModel<string>('aliasFilter', { default: '' });
const statusFilter = defineModel<string>('statusFilter', { default: '' });
const searchKeyword = defineModel<string>('searchKeyword', { default: '' });
const pageNum = defineModel<number>('pageNum', { default: 1 });
const pageSize = defineModel<number>('pageSize', { default: 10 });

defineProps<{
  loading: boolean;
  keyList: SecurityKeyVersionVO[];
  paginatedKeyList: SecurityKeyVersionVO[];
  filteredKeyList: SecurityKeyVersionVO[];
  handleFilterChange: () => void;
  handleSearch: () => void;
  handleReset: () => void;
  openCryptoTestModal: (targetKey?: SecurityKeyVersionVO | null) => void;
  openDetailDrawer: (row: SecurityKeyVersionVO) => void;
  openRotateModal: (alias?: string) => void;
  copyText: (text?: string) => void | Promise<void>;
}>();
</script>
