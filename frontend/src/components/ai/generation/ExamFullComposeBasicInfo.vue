<template>
  <div class="config-section-card">
    <h3 class="section-title">1. 试卷基本信息</h3>
    <div class="form-grid-row">
      <div class="form-item-col">
        <label class="form-label">适用课程空间</label>
        <el-select
          v-model="examForm.courseId"
          size="large"
          class="w-100"
        >
          <el-option
            v-for="c in displayCourses"
            :key="c.id"
            :label="`${c.code || ('CS' + c.id)} · ${c.title}`"
            :value="c.id"
          />
        </el-select>
      </div>

      <div class="form-item-col form-item-col--wide">
        <label class="form-label">试卷名称</label>
        <el-input
          v-model="examForm.title"
          placeholder="例如：2026秋季学期高等数学期中统一水平测试卷"
          size="large"
        />
      </div>
    </div>

    <div class="form-grid-row meta-numbers-row">
      <div class="number-stepper-box">
        <span class="num-title">目标卷面总分</span>
        <div class="stepper-wrap">
          <input v-model.number="examForm.totalScore" type="number" class="capsule-num-input" />
          <span class="unit">分</span>
        </div>
      </div>

      <div class="number-stepper-box">
        <span class="num-title">考试建议时长</span>
        <div class="stepper-wrap">
          <input v-model.number="examForm.durationMinutes" type="number" class="capsule-num-input" />
          <span class="unit">分钟</span>
        </div>
      </div>

      <div class="number-stepper-box">
        <span class="num-title">合格通过线 (60%)</span>
        <div class="stepper-wrap">
          <span class="fixed-score">{{ Math.round(examForm.totalScore * 0.6) }}</span>
          <span class="unit">分</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ExamFormState } from './exam-generate-types';

defineProps<{
  examForm: ExamFormState;
  displayCourses: any[];
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

  .form-grid-row {
    display: flex;
    gap: 20px;
    margin-bottom: 18px;

    .form-item-col {
      flex: 1;

      &--wide {
        flex: 2;
      }

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

  .meta-numbers-row {
    background: #F8FAFC;
    border-radius: 16px;
    padding: 16px 20px;
    margin-bottom: 0;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;

    .number-stepper-box {
      display: flex;
      flex-direction: column;
      align-items: center;

      .num-title {
        font-size: 12.5px;
        color: #64748B;
        margin-bottom: 6px;
      }

      .stepper-wrap {
        display: flex;
        align-items: baseline;
        gap: 6px;

        .capsule-num-input {
          width: 70px;
          height: 36px;
          text-align: center;
          border-radius: 9999px;
          border: 1px solid #CBD5E1;
          font-size: 16px;
          font-weight: 700;
          color: #1677FF;
          background: #FFFFFF;
          outline: none;

          &:focus {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
          }
        }

        .fixed-score {
          font-size: 18px;
          font-weight: 700;
          color: #10B981;
        }

        .unit {
          font-size: 13px;
          color: #64748B;
        }
      }
    }
  }
}
</style>
