<template>
  <div class="filter-header-bar">
    <div class="title-wrap">
      <h3 class="sec-title">AI 调用全链路审计日志</h3>
      <span class="sec-count">共 {{ total }} 条调用记录</span>
    </div>

    <div class="filter-controls">
      <el-select
        v-model="sceneFilter"
        placeholder="业务场景"
        clearable
        class="filter-pill-select"
        @change="$emit('search')"
      >
        <el-option value="" label="全部场景" />
        <el-option value="CHAT" label="智能对话 (CHAT)" />
        <el-option value="CHAT_RAG" label="知识增强对话 (CHAT_RAG)" />
        <el-option value="PREP" label="备课助手 (PREP)" />
        <el-option value="RAG" label="知识问答 (RAG)" />
        <el-option value="GRADING" label="作业批改 (GRADING)" />
        <el-option value="AGENT" label="智能体协作 (AGENT)" />
      </el-select>

      <!-- 动态加载已配置的系统模型，不硬编码 mock 或 chat -->
      <el-select
        v-model="modelFilter"
        :placeholder="modelOptions.length ? '生成模型' : '暂无可用模型'"
        :disabled="!modelOptions.length"
        clearable
        class="filter-pill-select"
        @change="$emit('search')"
      >
        <el-option value="" label="全部模型" />
        <el-option
          v-for="m in modelOptions"
          :key="m"
          :value="m"
          :label="m"
        />
      </el-select>

      <el-select
        v-model="statusFilter"
        placeholder="调用状态"
        clearable
        class="filter-pill-select"
        @change="$emit('search')"
      >
        <el-option value="" label="全部状态" />
        <el-option value="SUCCESS" label="成功 (SUCCESS)" />
        <el-option value="TIMEOUT" label="超时 (TIMEOUT)" />
        <el-option value="FAILED" label="异常 (FAILED)" />
      </el-select>

      <el-input
        v-model="searchKeyword"
        placeholder="搜索 Trace ID、用户名..."
        clearable
        class="filter-pill-input"
        :prefix-icon="Search"
        @keyup.enter="$emit('search')"
        @clear="$emit('search')"
      />

      <el-button
        type="primary"
        class="pill-action-btn primary"
        :disabled="tableLoading"
        @click="$emit('search')"
      >
        <el-icon><Search /></el-icon>
        <span>查询</span>
      </el-button>
      <el-button
        round
        class="btn-refresh"
        :icon="Refresh"
        :loading="tableLoading"
        @click="$emit('reset')"
      >
        重置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue';

const sceneFilter = defineModel<string>('sceneFilter', { required: true });
const modelFilter = defineModel<string>('modelFilter', { required: true });
const statusFilter = defineModel<string>('statusFilter', { required: true });
const searchKeyword = defineModel<string>('searchKeyword', { required: true });

defineProps<{
  total: number;
  modelOptions: string[];
  tableLoading: boolean;
}>();

defineEmits<{
  search: [];
  reset: [];
}>();
</script>

<style scoped lang="scss">
.filter-header-bar {
  padding: 18px 24px;
  border-bottom: 1px solid #F1F5F9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;

  .title-wrap {
    display: flex;
    align-items: center;
    gap: 10px;

    .sec-title {
      font-size: 15px;
      font-weight: 700;
      color: #0F172A;
      margin: 0;
    }

    .sec-count {
      font-size: 12px;
      color: #64748B;
      background: #F8FAFC;
      padding: 2px 8px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
    }
  }

  .filter-controls {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    .filter-pill-select {
      width: 150px;

      :deep(.el-select__wrapper) {
        border-radius: 9999px;
        font-size: 12.5px;
      }
    }

    .filter-pill-input {
      width: 240px;

      :deep(.el-input__wrapper) {
        border-radius: 9999px;
        font-size: 12.5px;
      }
    }

    .pill-action-btn {
      border-radius: 9999px;
      height: 32px;
      padding: 0 16px;
      font-size: 12.5px;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 5px;

      &.primary {
        background: #2563EB;
        border-color: #2563EB;

        &:hover {
          background: #1D4ED8;
        }
      }
    }
  }
}
</style>
