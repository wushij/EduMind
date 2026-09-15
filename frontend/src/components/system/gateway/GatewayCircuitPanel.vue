<template>
  <el-drawer
    :model-value="visible"
    title="网关实时调用链路 Trace 审计"
    size="760px"
    direction="rtl"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="drawer-filters">
      <el-input
        :model-value="traceModelFilter"
        placeholder="筛选模型..."
        clearable
        style="width: 180px;"
        @update:model-value="$emit('update:traceModelFilter', $event)"
        @change="$emit('load-trace-logs')"
      />
      <el-select
        :model-value="traceSceneFilter"
        placeholder="全部业务场景"
        clearable
        style="width: 180px;"
        @update:model-value="$emit('update:traceSceneFilter', $event)"
        @change="$emit('load-trace-logs')"
      >
        <el-option label="全部场景" value="" />
        <el-option label="课程智能助教答疑 (chat)" value="chat" />
        <el-option label="AI 题库出题与变式 (question_generate)" value="question_generate" />
        <el-option label="作业/主观题智能批改 (grading)" value="grading" />
        <el-option label="Agent 任务规划 (agent)" value="agent" />
      </el-select>
      <el-button type="primary" plain @click="$emit('load-trace-logs')">查询</el-button>
    </div>

    <el-table :data="traceLogs" stripe style="width: 100%; margin-top: 14px;" v-loading="traceLoading">
      <el-table-column prop="createTime" label="调用时间" width="160" />
      <el-table-column prop="scene" label="业务场景" width="130">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ translateSceneName(row.scene) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="model" label="转发模型" min-width="140">
        <template #default="{ row }">
          <span class="font-mono">{{ row.model }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Token 进/出" width="130">
        <template #default="{ row }">
          <span class="font-mono" style="font-size: 12px;">{{ row.promptTokens }} / {{ row.completionTokens }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="latencyMs" label="耗时" width="90">
        <template #default="{ row }">
          <span class="font-mono">{{ row.latencyMs }} ms</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default>
          <el-tag type="success" size="small">成功</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="drawer-pagination">
      <el-pagination
        :current-page="tracePage"
        :page-size="tracePageSize"
        :total="traceTotal"
        layout="prev, pager, next, total"
        @update:current-page="$emit('update:tracePage', $event)"
        @update:page-size="$emit('update:tracePageSize', $event)"
        @current-change="$emit('load-trace-logs')"
      />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import type { GatewayTraceLogVO } from '@/types/system/gateway';

defineProps<{
  visible: boolean;
  traceLoading: boolean;
  traceLogs: GatewayTraceLogVO[];
  traceModelFilter: string;
  traceSceneFilter: string;
  tracePage: number;
  tracePageSize: number;
  traceTotal: number;
  translateSceneName: (scene: string) => string;
}>();

defineEmits<{
  'update:visible': [value: boolean];
  'update:traceModelFilter': [value: string];
  'update:traceSceneFilter': [value: string];
  'update:tracePage': [value: number];
  'update:tracePageSize': [value: number];
  'load-trace-logs': [];
}>();
</script>

<style scoped lang="scss">
.drawer-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.drawer-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
