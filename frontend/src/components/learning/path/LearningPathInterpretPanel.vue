<template>
  <div v-if="hint" class="path-interpret-card">
    <el-collapse class="path-interpret-collapse">
      <el-collapse-item name="interpret">
        <template #title>
          <div class="interpret-header-row">
            <div class="interpret-icon-box">
              <el-icon><Compass /></el-icon>
            </div>
            <div class="interpret-header-text">
              <span class="interpret-title">AI 学习路径规划与自适应导学说明</span>
              <span class="interpret-tag">智能导学解读</span>
            </div>
          </div>
        </template>
        <div class="interpret-content-box">
          <p class="interpret-text">{{ hint }}</p>
          <div v-if="generatedAt" class="interpret-meta-row">
            <el-icon><Timer /></el-icon>
            <span>算法生成时间：{{ generatedAt }}</span>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import { Compass, Timer } from '@element-plus/icons-vue';

defineProps<{
  hint?: string;
  generatedAt?: string;
}>();
</script>

<style scoped lang="scss">
.path-interpret-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #ebf1f7;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  overflow: hidden;

  :deep(.el-collapse) {
    border: none;
    --el-collapse-header-bg-color: transparent;
  }

  :deep(.el-collapse-item__header) {
    padding: 16px 20px;
    height: auto;
    line-height: 1.5;
    border-bottom: none;
    background: #ffffff;
    transition: all 0.2s;

    &:hover {
      background: #fbfcfe;
    }
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
    background: #fafbfc;
  }

  :deep(.el-collapse-item__content) {
    padding: 0 20px 18px;
  }

  .interpret-header-row {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;

    .interpret-icon-box {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: #eff6ff;
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 17px;
      border: 1px solid rgba(37, 99, 235, 0.12);
    }

    .interpret-header-text {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .interpret-title {
        font-size: 14.5px;
        font-weight: 700;
        color: #0f172a;
      }

      .interpret-tag {
        font-size: 11px;
        font-weight: 600;
        padding: 1px 8px;
        border-radius: 9999px;
        background: #f5f3ff;
        color: #7c3aed;
        border: 1px solid rgba(124, 58, 237, 0.15);
      }
    }
  }

  .interpret-content-box {
    background: #ffffff;
    border-radius: 12px;
    padding: 14px 16px;
    border: 1px solid #f1f5f9;

    .interpret-text {
      margin: 0 0 10px;
      font-size: 13px;
      color: #334155;
      line-height: 1.65;
    }

    .interpret-meta-row {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 11.5px;
      color: #94a3b8;
    }
  }
}
</style>
