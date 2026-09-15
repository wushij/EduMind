<template>
  <div class="quota-management-page">
    <div class="quota-header-card">
      <div class="header-left">
        <h2>AI Token 用户配额管理</h2>
        <p>按用户维度设定每日 Token 与调用次数上限，支持实时查看当日消耗进度</p>
      </div>
    </div>

    <el-table v-loading="loading" :data="userQuotas" stripe>
      <el-table-column prop="userId" label="用户 ID" width="100" />
      <el-table-column prop="roleName" label="用户" min-width="160" />
      <el-table-column label="今日消耗 / 日上限" min-width="220">
        <template #default="{ row }">
          <div class="progress-cell">
            <span>{{ row.usedTokensToday.toLocaleString() }} / {{ row.isUnlimited ? '不限' : row.dailyTokenLimit.toLocaleString() }}</span>
            <el-progress
              v-if="!row.isUnlimited"
              :percentage="computeQuotaUsagePercent(row.usedTokensToday, row.dailyTokenLimit, row.isUnlimited)"
              :stroke-width="6"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="qpsLimit" label="日调用上限" width="120">
        <template #default="{ row }">
          {{ row.qpsLimit || '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="editQuota(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editDialogVisible" title="编辑用户配额" width="460px">
      <el-form v-if="editingQuota" label-position="top">
        <el-form-item label="用户">
          <el-input :model-value="editingQuota.roleName" disabled />
        </el-form-item>
        <el-form-item label="日 Token 上限（0 表示不限）">
          <el-input-number v-model="editingQuota.dailyTokenLimit" :step="10000" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日调用次数上限（0 表示不限）">
          <el-input-number v-model="editingQuota.qpsLimit" :step="10" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUserQuota">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useQuotaOverview, computeQuotaUsagePercent } from '@/composables/system/useQuotaOverview';

const {
  loading,
  userQuotas,
  editDialogVisible,
  editingQuota,
  editQuota,
  saveUserQuota
} = useQuotaOverview();
</script>

<style scoped lang="scss">
.quota-management-page {
  padding: 24px;

  .quota-header-card {
    margin-bottom: 20px;

    h2 {
      margin: 0 0 8px;
      font-size: 22px;
    }

    p {
      margin: 0;
      color: #64748b;
      font-size: 14px;
    }
  }

  .progress-cell {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 13px;
  }
}
</style>
