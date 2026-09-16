<template>
  <div class="config-section-card">
    <h3 class="section-title">2. 布鲁姆认知分层权重</h3>
    <p class="section-desc">调节各认知层级题量占比，组卷引擎将按权重从题库中智能抽题：</p>
    <div class="cognitive-level-grid">
      <div v-for="item in cognitiveLevelOptions" :key="item.key" class="cognitive-level-item">
        <span class="level-label">{{ item.label }}</span>
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
    <el-alert
      v-if="cognitiveLevelTotal !== 100"
      type="warning"
      :closable="false"
      show-icon
      :title="`认知分层权重合计 ${cognitiveLevelTotal}%，建议调整为 100%`"
      style="margin-top: 12px"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  cognitiveLevelOptions: { key: string; label: string }[];
  cognitiveLevels: Record<string, number>;
  cognitiveLevelTotal: number;
}>();
</script>

<style scoped lang="scss">
.config-section-card {
  background: #FFFFFF;
  border-radius: 24px;
  padding: 28px 32px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  margin-bottom: 22px;

  .section-title {
    margin: 0 0 16px 0;
    font-size: 16.5px;
    font-weight: 700;
    color: #0F172A;
  }
}

.cognitive-level-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .cognitive-level-item {
    display: grid;
    grid-template-columns: 160px 1fr;
    align-items: center;
    gap: 12px;

    .level-label {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }
  }
}

.section-desc {
  margin: 0 0 14px;
  font-size: 13px;
  color: #64748B;
}
</style>
