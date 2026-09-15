<template>
  <div class="step-content-box">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header-title">
          <el-icon class="icon"><EditPen /></el-icon>
          <div>
            <h3>第一步：设置试卷基本规范</h3>
            <p>请填写本次测验或期末考试的名称、所属课程与考场基准时间规则。</p>
          </div>
        </div>
      </template>

      <el-form
        :ref="bindFormRef"
        :model="examForm"
        :rules="step1Rules"
        label-position="top"
        class="step-form-grid"
      >
        <el-row :gutter="24">
          <el-col :span="16">
            <el-form-item label="试卷名称 / 标题" prop="title">
              <el-input
                v-model="examForm.title"
                placeholder="例如：2025-2026学年第二学期《数据结构》期末统一考核试卷 (A卷)"
                size="large"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试学期" prop="semester">
              <el-select v-model="examForm.semester" size="large" class="w-full">
                <el-option label="2025-2026 第二学期" value="2025-2026-2" />
                <el-option label="2025-2026 第一学期" value="2025-2026-1" />
                <el-option label="2024-2025 第二学期" value="2024-2025-2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="所属课程" prop="courseId">
              <el-select
                v-model="examForm.courseId"
                placeholder="选择课程"
                size="large"
                class="w-full"
                @change="onCourseChange"
              >
                <el-option
                  v-for="c in courses"
                  :key="c.id"
                  :label="c.title"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试限时（分钟）" prop="durationMinutes">
              <el-input-number
                v-model="examForm.durationMinutes"
                :min="10"
                :max="300"
                :step="10"
                size="large"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分线 (及格线)" prop="passScore">
              <el-input-number
                v-model="examForm.passScore"
                :min="1"
                :max="examForm.totalScore || 100"
                size="large"
                class="w-full"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="试卷卷首考生须知 / 考试说明">
          <el-input
            v-model="examForm.description"
            type="textarea"
            :rows="3"
            placeholder="例如：本试卷满分100分，答题时间120分钟。请在答题卡规定区域内作答，严禁使用通讯工具及外附存储设备。"
          />
        </el-form-item>
      </el-form>

      <div class="step-footer-bar">
        <div></div>
        <el-button type="primary" size="large" @click="$emit('next')">
          <span>下一步：编排大题与选题</span>
          <el-icon class="ml-1"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';
import { ArrowRight, EditPen } from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';

const props = defineProps<{
  setStep1FormRef: (el: FormInstance | undefined) => void;
  examForm: {
    title: string;
    semester: string;
    courseId?: number;
    durationMinutes: number;
    totalScore: number;
    passScore: number;
    description: string;
  };
  step1Rules: FormRules;
  courses: Course[];
  onCourseChange: (val?: number) => void;
}>();

defineEmits<{
  next: [];
}>();

function bindFormRef(el: FormInstance | null) {
  props.setStep1FormRef(el ?? undefined);
}
</script>

<style scoped lang="scss">
.step-content-box {
  .form-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    padding: 16px 24px;

    .card-header-title {
      display: flex;
      align-items: center;
      gap: 16px;

      .icon {
        font-size: 32px;
      }

      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 700;
        color: #0f172a;
      }

      p {
        margin: 4px 0 0;
        font-size: 13px;
        color: #64748b;
      }
    }

    .step-form-grid {
      margin-top: 24px;
    }

    .step-footer-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 32px;
      padding-top: 20px;
      border-top: 1px solid #f1f5f9;
    }
  }
}
</style>
