<template>
  <div class="practice-modes-row">
    <div
      class="mode-badge"
      :class="{ active: practiceMode === 'WEAK_POINT' }"
      @click="onSwitchMode('WEAK_POINT')"
    >
      <div class="mode-icon-box mode-icon-box--aim">
        <el-icon><Aim /></el-icon>
      </div>
      <div class="mode-info">
        <span class="mode-title">错题变式攻坚</span>
        <span class="mode-desc">聚焦历史高频失分题型定向突破</span>
      </div>
    </div>
    <div
      class="mode-badge"
      :class="{ active: practiceMode === 'KNOWLEDGE_TIER' }"
      @click="onSwitchMode('KNOWLEDGE_TIER')"
    >
      <div class="mode-icon-box mode-icon-box--trend">
        <el-icon><TrendCharts /></el-icon>
      </div>
      <div class="mode-info">
        <span class="mode-title">考点认知进阶</span>
        <span class="mode-desc">按识记/理解/应用梯次挑战</span>
      </div>
    </div>
    <div
      class="mode-badge"
      :class="{ active: practiceMode === 'ADAPTIVE_SPRINT' }"
      @click="onSwitchMode('ADAPTIVE_SPRINT')"
    >
      <div class="mode-icon-box mode-icon-box--lightning">
        <el-icon><Lightning /></el-icon>
      </div>
      <div class="mode-info">
        <span class="mode-title">全真自适应冲刺</span>
        <span class="mode-desc">AI 随答题动态升降难度</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Aim, TrendCharts, Lightning } from '@element-plus/icons-vue';

defineProps<{
  practiceMode: string;
  onSwitchMode: (mode: string) => void;
}>();
</script>

<style scoped lang="scss">
.practice-modes-row {
  display: flex;
  gap: 14px;
  margin-top: 4px;
  flex-wrap: wrap;

  .mode-badge {
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
    border: 1.5px solid rgba(22, 119, 255, 0.14);
    border-radius: 9999px;
    padding: 6px 20px 6px 8px;
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.04);

    .mode-icon-box {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;
      transition: all 0.25s ease;

      &--aim {
        background: rgba(245, 34, 45, 0.08);
        color: #F5222D;
      }

      &--trend {
        background: rgba(22, 119, 255, 0.08);
        color: #1677FF;
      }

      &--lightning {
        background: rgba(250, 140, 22, 0.1);
        color: #FA8C16;
      }
    }

    .mode-info {
      display: flex;
      flex-direction: column;

      .mode-title {
        font-size: 13.5px;
        font-weight: 700;
        color: #1E293B;
        line-height: 1.3;
      }

      .mode-desc {
        font-size: 11px;
        color: #64748B;
        margin-top: 1px;
      }
    }

    &:hover {
      background: #FFFFFF;
      border-color: #1677FF;
      transform: translateY(-2px);
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.12);
    }

    &.active {
      background: #FFFFFF;
      border-color: #1677FF;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.18), 0 4px 14px rgba(22, 119, 255, 0.12);

      .mode-title {
        color: #1677FF;
      }

      .mode-icon-box {
        &--aim {
          background: #F5222D;
          color: #FFFFFF;
        }
        &--trend {
          background: #1677FF;
          color: #FFFFFF;
        }
        &--lightning {
          background: #FA8C16;
          color: #FFFFFF;
        }
      }
    }
  }
}
</style>
