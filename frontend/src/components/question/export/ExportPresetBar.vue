<template>
  <div class="template-presets-bar no-print">
    <div class="presets-header">
      <div class="presets-title">
        <el-icon class="icon-preset"><Files /></el-icon>
        <span>考务排版方案预设库</span>
      </div>
      <span class="presets-tip">点击一键套用官方考场版式与卷面规范配置</span>
    </div>
    <div class="preset-cards-grid">
      <div
        v-for="preset in presetTemplates"
        :key="preset.id"
        class="preset-card"
        :class="{ active: currentPresetId === preset.id }"
        @click="applyPreset(preset)"
      >
        <div class="preset-icon-badge" :style="{ background: preset.bgColor, color: preset.color }">
          <component :is="preset.icon" />
        </div>
        <div class="preset-info">
          <div class="preset-name-row">
            <span class="preset-name">{{ preset.name }}</span>
            <el-tag size="small" :type="preset.tagType" effect="plain">{{ preset.tag }}</el-tag>
          </div>
          <p class="preset-desc">{{ preset.desc }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Files } from '@element-plus/icons-vue';
import type { PresetTemplate } from '@/composables/question/useExport';

defineProps<{
  presetTemplates: PresetTemplate[];
  currentPresetId: string;
  applyPreset: (preset: PresetTemplate) => void;
}>();
</script>

<style scoped lang="scss">
.template-presets-bar {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  padding: 18px 22px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

  .presets-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    .presets-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #0F172A;

      .icon-preset {
        color: #2563EB;
        font-size: 18px;
      }
    }

    .presets-tip {
      font-size: 12px;
      color: #64748B;
    }
  }

  .preset-cards-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }

    .preset-card {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid #E2E8F0;
      background: #F8FAFC;
      cursor: pointer;
      box-sizing: border-box;
      min-width: 0;
      transition: border-color 0.15s ease, background-color 0.15s ease;
      outline: none;

      &:hover {
        border-color: #CBD5E1;
        background: #FFFFFF;
      }

      &.active {
        border-color: #2563EB;
        background: #F8FAFC;
      }

      &:focus-visible {
        outline: 1px solid #2563EB;
        outline-offset: 0;
      }

      .preset-icon-badge {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        flex-shrink: 0;
      }

      .preset-info {
        flex: 1;
        min-width: 0;

        .preset-name-row {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 4px 6px;
          margin-bottom: 4px;

          .preset-name {
            font-size: 13px;
            font-weight: 600;
            color: #1E293B;
            line-height: 1.3;
            min-width: 0;
            flex: 1 1 auto;
          }

          :deep(.el-tag) {
            flex-shrink: 0;
            max-width: 100%;
            height: 20px;
            padding: 0 6px;
            font-size: 10px;
            line-height: 18px;
          }
        }

        .preset-desc {
          font-size: 11px;
          color: #64748B;
          line-height: 1.4;
          margin: 0;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }
    }
  }
}
</style>
