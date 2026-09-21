<template>
  <div class="config-section-card">
    <div class="section-header-box">
      <h3 class="section-title">2. 布鲁姆认知分层权重约束</h3>
      <p class="section-desc">调节各认知层级题量占比，AI 组卷引擎将按权重智能求解最佳拟合路径：</p>
    </div>

    <div class="cognitive-level-grid">
      <div v-for="item in cognitiveLevelOptions" :key="item.key" class="cognitive-level-item">
        <span class="level-label-pill">{{ item.label }}</span>
        <div class="slider-wrapper">
          <el-slider
            v-model="cognitiveLevels[item.key]"
            :min="0"
            :max="100"
            :step="5"
            show-input
            size="small"
          />
        </div>
      </div>
    </div>

    <div
      v-if="cognitiveLevelTotal !== 100"
      class="warning-pill-alert"
    >
      <el-icon><Warning /></el-icon>
      <span>认知分层权重合计当前为 {{ cognitiveLevelTotal }}%，建议调节各权重使总和达到 100%</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Warning } from '@element-plus/icons-vue';

defineProps<{
  cognitiveLevelOptions: { key: string; label: string }[];
  cognitiveLevels: Record<string, number>;
  cognitiveLevelTotal: number;
}>();
</script>

<style scoped lang="scss">
.config-section-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 28px 32px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  margin-bottom: 22px;

  .section-title {
    margin: 0 0 6px 0;
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
  }

  .section-desc {
    margin: 0 0 18px 0;
    font-size: 13px;
    color: #64748b;
  }
}

.cognitive-level-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .cognitive-level-item {
    display: grid;
    grid-template-columns: 180px 1fr;
    align-items: center;
    gap: 16px;
    background: #f8fafc;
    border-radius: 9999px; // 长圆边框
    padding: 6px 18px;
    border: 1px solid #e2e8f0;

    .level-label-pill {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }

    .slider-wrapper {
      padding-right: 8px;
    }
  }
}

.warning-pill-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 8px 18px;
  border-radius: 9999px; // 长圆警示条
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #d97706;
  font-size: 12.5px;
  font-weight: 500;
}
</style>
