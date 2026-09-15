<template>
  <div class="section-card vars-manager-box">
    <div class="section-card-header">
      <div class="sch-left">
        <span class="sec-icon cyan"><el-icon><DataAnalysis /></el-icon></span>
        <div class="sec-title-wrap">
          <span class="sec-title">动态注入参数规格清单 ({{ variables.length }} 个插槽)</span>
          <span class="sec-sub">声明与维护大模型提示词中引用的上下文变量定义</span>
        </div>
      </div>
      <el-button size="small" :icon="Plus" @click="onAdd">
        新增参数
      </el-button>
    </div>

    <div class="vars-table-wrap">
      <div class="vt-head">
        <span class="c-name">变量插槽标识</span>
        <span class="c-label">中文语义</span>
        <span class="c-val">默认测试示例</span>
        <span class="c-op">操作</span>
      </div>
      <div v-for="(v, idx) in variables" :key="idx" class="vt-row">
        <div class="c-name">
          <el-input v-model="v.name" size="small" placeholder="如 course_name" class="mono-inp" />
        </div>
        <div class="c-label">
          <el-input v-model="v.label" size="small" placeholder="如 课程名称" />
        </div>
        <div class="c-val">
          <el-input v-model="v.defaultValue" size="small" placeholder="测试默认值" />
        </div>
        <div class="c-op">
          <el-button
            size="small"
            link
            type="danger"
            :icon="Delete"
            title="删除参数插槽"
            @click="onRemove(idx, v)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DataAnalysis, Delete, Plus } from '@element-plus/icons-vue';
import type { PromptTemplate } from '@/types/system/prompt';

defineProps<{
  variables: PromptTemplate['variables'];
  onAdd: () => void;
  onRemove: (index: number, v: { name: string; label?: string }) => void;
}>();
</script>

<style scoped lang="scss">
.section-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  padding: 18px 22px;
  margin-bottom: 18px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);

  .section-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    margin-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;

    .sch-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .sec-icon {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;

        &.cyan { background: #ECFEFF; color: #0891B2; }
      }

      .sec-title-wrap {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .sec-title {
          font-size: 14.5px;
          font-weight: 700;
          color: #0F172A;
        }

        .sec-sub {
          font-size: 11.5px;
          color: #64748B;
        }
      }
    }
  }
}

.vars-manager-box {
  .vars-table-wrap {
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    overflow: hidden;

    .vt-head {
      display: grid;
      grid-template-columns: 180px 140px 1fr 50px;
      gap: 10px;
      padding: 8px 14px;
      background: #F8FAFC;
      font-size: 12px;
      font-weight: 600;
      color: #64748B;
    }

    .vt-row {
      display: grid;
      grid-template-columns: 180px 140px 1fr 50px;
      gap: 10px;
      padding: 8px 14px;
      border-top: 1px solid #F1F5F9;
      align-items: center;

      .mono-inp {
        :deep(input) {
          font-family: ui-monospace, monospace;
          color: #2563EB;
          font-weight: 600;
        }
      }
    }
  }
}
</style>
