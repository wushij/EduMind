<template>
  <el-card shadow="never" class="audit-table-card mt-4">
    <div class="card-header-flex">
      <h3 class="card-title">
        <el-icon class="mr-1"><Clock /></el-icon>
        <span>账号近期安全与操作审计日志</span>
      </h3>
      <button type="button" class="jump-log-center-btn" @click="onJumpOperLog">
        查看全平台操作日志大盘 →
      </button>
    </div>
    <el-table v-loading="auditLogsLoading" :data="auditLogs" stripe class="audit-table">
      <el-table-column label="操作时间" prop="time" width="180" />
      <el-table-column label="操作模块" prop="module" width="140" />
      <el-table-column label="操作行为说明" prop="action" min-width="220" />
      <el-table-column label="客户端 IP" prop="ip" width="140" />
      <el-table-column label="耗时" width="100">
        <template #default="{ row }">
          <span>{{ row.costTime ?? 0 }}ms</span>
        </template>
      </el-table-column>
      <el-table-column label="结果状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small" round>
            {{ row.status === 'SUCCESS' ? '操作成功' : '拦截失败' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { Clock } from '@element-plus/icons-vue';

defineProps<{
  auditLogs: Array<Record<string, unknown>>;
  auditLogsLoading: boolean;
  onJumpOperLog: () => void;
}>();
</script>

<style scoped lang="scss">
.audit-table-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 16px 20px;

  .card-title {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 16px;
  }
}
</style>

<style lang="scss">
.audit-table-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;

  .card-header-flex {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    .card-title {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      display: flex;
      align-items: center;
      margin: 0;
    }

    .jump-log-center-btn {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #2563eb;
      font-size: 12px;
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
</style>
