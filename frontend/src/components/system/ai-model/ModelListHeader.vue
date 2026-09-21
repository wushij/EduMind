<template>
  <div class="model-header-wrap">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div class="header-main">
          <div class="header-icon-box">
            <el-icon :size="24"><Cpu /></el-icon>
          </div>
          <div class="header-info">
            <h2 class="header-title">LLM 与 Embedding 模型管理</h2>
            <p class="header-subtitle">
              统一维护全平台对话推理与向量计算模型接入，支持行内一键连通性测试与默认调度策略
            </p>
          </div>
        </div>

        <div class="header-actions">
          <el-button type="primary" class="btn-create" @click="onOpenCreate">
            <el-icon><Plus /></el-icon> 新增模型
          </el-button>
          <el-button :icon="Refresh" class="btn-refresh" :loading="refreshing" @click="onRefresh">
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
.model-header-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card {
  border-radius: 16px;
  border: 1px solid #e8edf3;
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 100%);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.25s ease;

  :deep(.el-card__body) {
    padding: 22px 28px;
  }
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.header-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #e0edff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.12);
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.header-subtitle {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;

  .btn-create {
    height: 36px;
    padding: 0 20px;
    border-radius: 9999px !important;
    font-weight: 600;
    font-size: 13px;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    border: none;
    box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
    }
  }

  .btn-refresh {
    height: 36px;
    padding: 0 18px;
    border-radius: 9999px !important;
    border: 1px solid #e2e8f0;
    color: #475569;
    font-weight: 500;
    font-size: 13px;
    background: #ffffff;
    transition: all 0.2s ease;

    &:hover {
      border-color: #cbd5e1;
      background: #f8fafc;
      color: #0f172a;
    }
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
  padding: 4px;
  border-radius: 9999px; // 纯正长圆药丸外框
  gap: 4px;
  border: 1px solid #e2e8f0;
}

.segment-btn {
  border: none;
  background: transparent;
  padding: 7px 20px;
  border-radius: 9999px; // 纯正长圆药丸内部项
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;

  &:hover {
    color: #1677ff;
  }

  &.active {
    background: #ffffff;
    color: #1677ff;
    font-weight: 600;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
  }
}

.count-pill {
  background: #e2e8f0;
  color: #475569;
  padding: 1px 8px;
  border-radius: 9999px; // 纯正长圆计数胶囊
  font-size: 11px;
  font-weight: 700;
  min-width: 22px;
  text-align: center;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.segment-btn.active .count-pill {
  background: #e0edff;
  color: #1677ff;
}
</style>
