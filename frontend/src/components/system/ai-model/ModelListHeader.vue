<template>
  <div>
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div class="header-info">
          <div class="header-tag-row">
            <span class="module-badge">
              <el-icon><Cpu /></el-icon> EduMind AI · 模型配置
            </span>
            <span class="env-badge">Zero-Restart 热重载</span>
          </div>
          <h2 class="header-title">LLM 与 Embedding 模型管理</h2>
          <p class="header-subtitle">
            统一维护全平台对话推理与向量计算模型接入，支持行内一键连通性测试与默认调度策略
          </p>
        </div>
        <div class="header-actions">
          <el-button type="primary" class="add-btn" @click="onOpenCreate">
            <el-icon><Plus /></el-icon> 新增模型
          </el-button>
          <el-button round :icon="Refresh" class="btn-refresh" :loading="refreshing" @click="onRefresh">
            刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="filter-segment-wrap">
      <div class="filter-segment">
        <button
          type="button"
          class="segment-btn"
          :class="{ active: activeTab === 'chat' }"
          @click="onSwitchTab('chat')"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>对话推理模型 (Chat)</span>
          <span class="count-pill">{{ chatModels.length }}</span>
        </button>
        <button
          type="button"
          class="segment-btn"
          :class="{ active: activeTab === 'embedding' }"
          @click="onSwitchTab('embedding')"
        >
          <el-icon><Connection /></el-icon>
          <span>向量计算模型 (Embedding)</span>
          <span class="count-pill">{{ embeddingModels.length }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, Refresh, Connection, ChatDotRound, Cpu } from '@element-plus/icons-vue';
import type { AIModelConfigItem } from '@/types/system/model';

defineProps<{
  activeTab: 'chat' | 'embedding';
  refreshing: boolean;
  chatModels: AIModelConfigItem[];
  embeddingModels: AIModelConfigItem[];
  onOpenCreate: () => void;
  onRefresh: () => void;
  onSwitchTab: (tab: 'chat' | 'embedding') => void;
}>();
</script>

<style scoped lang="scss">
.header-card {
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  background: #ffffff;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.module-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #eaf3ff;
  color: #1677ff;
  border: 1px solid #bae0ff;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.env-badge {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
}

.header-title {
  margin: 4px 0 2px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.header-subtitle {
  color: #64748b;
  font-size: 13px;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;

  .add-btn {
    min-width: 112px;
  }
}

.filter-segment-wrap {
  display: flex;
  align-items: center;
}

.filter-segment {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 999px;
  gap: 4px;
  border: 1px solid #e2e8f0;
}

.segment-btn {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;

  &:hover {
    color: #1677ff;
  }

  &.active {
    background: #ffffff;
    color: #1677ff;
    font-weight: 600;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  }
}

.count-pill {
  background: #f1f5f9;
  color: #64748b;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 11px;
  min-width: 22px;
  text-align: center;
  box-sizing: border-box;
}

.segment-btn.active .count-pill {
  background: #eaf3ff;
  color: #1677ff;
}
</style>
