<template>
  <div class="config-section-card quick-compose-card">
    <h3 class="section-title">快速智能组卷 (V1.1 多目标正态分布算法)</h3>
    <div class="form-grid-row">
      <div class="form-item-col">
        <label class="form-label">适用课程</label>
        <el-select v-model="examForm.courseId" size="large" class="w-100">
          <el-option
            v-for="c in displayCourses"
            :key="c.id"
            :label="`${c.code || ('CS' + c.id)} · ${c.title}`"
            :value="c.id"
          />
        </el-select>
      </div>
      <div class="form-item-col">
        <label class="form-label">抽题数量</label>
        <el-input-number v-model="quickCountModel" :min="5" :max="50" size="large" />
      </div>
      <div class="form-item-col">
        <label class="form-label">卷面目标总分</label>
        <el-input-number v-model="quickTotalScoreModel" :min="50" :max="150" :step="10" size="large" />
      </div>
    </div>

    <!-- 难度梯度模型 -->
    <div class="difficulty-model-section" style="margin-top: 16px;">
      <label class="form-label" style="display:block; margin-bottom: 8px;">难度分布模型</label>
      <el-radio-group v-model="difficultyModelBinding" size="default">
        <el-radio-button label="FOUNDATION">基础巩固型 (5:4:1)</el-radio-button>
        <el-radio-button label="NORMAL">标准正态型 (3:5:2)</el-radio-button>
        <el-radio-button label="ADVANCED">综合拔高型 (1:4:5)</el-radio-button>
      </el-radio-group>
    </div>

    <div style="margin-top: 20px;">
      <el-button type="primary" size="large" :loading="composing" @click="$emit('quick-compose')">
        <el-icon><Lightning /></el-icon>
        <span>启动智能组卷 v2 计算</span>
      </el-button>
    </div>

    <div v-if="composePreview" class="compose-preview" style="margin-top: 20px;">
      <PaperDistributionChart
        :coverage-rate="composePreview.coverageRate || 0"
        :distinct-knowledge-count="composePreview.distinctKnowledgePointCount || 0"
        :total-count="composePreview.selectedCount || quickCountModel"
        :difficulty-histogram="composePreview.difficultyHistogram"
        :type-distribution="composePreview.typeDistribution"
      />
      <div style="margin-top: 14px; text-align: right;">
        <el-button type="success" size="large" @click="$emit('proceed-preview')">
          确认并进入试卷预览与发布 →
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lightning } from '@element-plus/icons-vue';
import PaperDistributionChart from '@/components/question/PaperDistributionChart.vue';
import type { SmartPaperComposeVO } from '@/types/ai/paper-compose';
import type { ExamFormState } from './exam-generate-types';

defineProps<{
  examForm: ExamFormState;
  displayCourses: any[];
  composing: boolean;
  composePreview: SmartPaperComposeVO | null;
}>();

const quickCountModel = defineModel<number>('quickCount', { required: true });
const quickTotalScoreModel = defineModel<number>('quickTotalScore', { required: true });
const difficultyModelBinding = defineModel<'FOUNDATION' | 'NORMAL' | 'ADVANCED'>('difficultyModel', { required: true });

defineEmits<{
  'quick-compose': [];
  'proceed-preview': [];
}>();
</script>

<style scoped lang="scss">
.quick-compose-card {
  margin-bottom: 20px;

  .compose-preview {
    margin: 16px 0;
  }
}

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

  .form-grid-row {
    display: flex;
    gap: 20px;
    margin-bottom: 18px;

    .form-item-col {
      flex: 1;

      .form-label {
        display: block;
        font-size: 13.5px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 8px;
      }

      .w-100 {
        width: 100%;
      }
    }
  }
}
</style>
