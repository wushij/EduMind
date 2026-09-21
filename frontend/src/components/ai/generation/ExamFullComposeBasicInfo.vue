<template>
  <div class="config-section-card">
    <div class="section-header-box">
      <h3 class="section-title">1. 试卷基本属性设定</h3>
      <p class="section-desc">指定考察的课程空间、设定试卷标题及卷面总分时长：</p>
    </div>

    <div class="form-grid-row">
      <div class="form-item-col">
        <label class="form-label">
          <span>适用课程空间</span>
          <span class="required-star">*</span>
        </label>
        <el-select
          v-model="examForm.courseId"
          size="large"
          class="w-100 round-select"
        >
          <el-option
            v-for="c in displayCourses"
            :key="c.id"
            :label="`${c.code || ('CS' + c.id)} · ${c.name || c.title}`"
            :value="c.id"
          />
        </el-select>
      </div>

      <div class="form-item-col form-item-col--wide">
        <label class="form-label">
          <span>试卷标题</span>
          <span class="required-star">*</span>
        </label>
        <el-input
          v-model="examForm.title"
          placeholder="例如：2025-2026学年第二学期期末专业综合能力测试卷"
          size="large"
          class="round-input"
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
        <span class="num-title">合格及格线 (60%)</span>
        <div class="stepper-wrap">
          <span class="fixed-score-pill">{{ Math.round(examForm.totalScore * 0.6) }}</span>
          <span class="unit">分及格</span>
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
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13.5px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 8px;

        .required-star {
          color: #ef4444;
        }
      }

      .w-100 {
        width: 100%;
      }
    }
  }

  .round-input {
    :deep(.el-input__wrapper) {
      border-radius: 9999px !important; // 长圆边框
      padding-left: 18px;
    }
  }

  .round-select {
    :deep(.el-select__wrapper) {
      border-radius: 9999px !important; // 长圆边框
      padding-left: 18px;
    }
  }

  .meta-numbers-row {
    background: #f8fafc;
    border-radius: 9999px; // 长圆边框
    padding: 12px 28px;
    margin-bottom: 0;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    border: 1px solid #e2e8f0;

    .number-stepper-box {
      display: flex;
      flex-direction: column;
      align-items: center;

      .num-title {
        font-size: 12px;
        color: #64748b;
        margin-bottom: 4px;
        font-weight: 500;
      }

      .stepper-wrap {
        display: flex;
        align-items: center;
        gap: 6px;

        .capsule-num-input {
          width: 72px;
          height: 32px;
          text-align: center;
          border-radius: 9999px; // 长圆数字框
          border: 1px solid #cbd5e1;
          font-size: 15px;
          font-weight: 700;
          color: #2563eb;
          background: #ffffff;
          outline: none;
          transition: all 0.2s;

          &:focus {
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
          }
        }

        .fixed-score-pill {
          padding: 2px 14px;
          border-radius: 9999px; // 长圆
          background: #ecfdf5;
          color: #059669;
          font-size: 15px;
          font-weight: 700;
          border: 1px solid #a7f3d0;
        }

        .unit {
          font-size: 12px;
          color: #64748b;
        }
      }
    }
  }
}
</style>
