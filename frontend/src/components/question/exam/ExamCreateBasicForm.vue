<template>
  <div class="step-content-box">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header-title">
          <div class="icon-badge">
            <el-icon><EditPen /></el-icon>
          </div>
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
                placeholder="例如：2025-2026学年第二学期《Java面向对象程序设计》期末测试卷"
                size="large"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试学期" prop="semester">
              <el-select
                v-model="examForm.semester"
                placeholder="请选择或输入考试学期"
                size="large"
                class="w-full"
                filterable
                allow-create
                default-first-option
              >
                <el-option
                  v-for="option in academicTermOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
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
                  :label="c.title || (c as any).name"
                  :value="Number(c.id)"
                />
                <el-option
                  v-if="examForm.courseId && !courses.some(c => Number(c.id) === Number(examForm.courseId))"
                  :key="examForm.courseId"
                  :label="examForm.courseName || getStaticCourseName(examForm.courseId)"
                  :value="Number(examForm.courseId)"
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
            <el-form-item label="及格分线（及格线）" prop="passScore">
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
            placeholder="例如：本试卷满分100分，答题时间120分钟。请在答题卡规定区域内规范书写，独立完成作答。"
          />
        </el-form-item>
      </el-form>

      <div class="step-footer-bar">
        <div></div>
        <el-button type="primary" size="large" class="next-step-capsule-btn" @click="$emit('next')">
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
import { buildAcademicTermOptions } from '@/constants/semester';

/** 考试学期选项：按当前学年动态推导（学年制，最近的学期在前），不再写死具体学年 */
const academicTermOptions = buildAcademicTermOptions();

const props = defineProps<{
  setStep1FormRef: (el: FormInstance | undefined) => void;
  examForm: {
    title: string;
    semester: string;
    courseId?: number;
    courseName?: string;
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

function getStaticCourseName(id?: number | string): string {
  const map: Record<string, string> = {
    '101': '数据结构与算法',
    '102': 'Java面向对象程序设计',
    '103': '高等数学（上）'
  };
  return id ? (map[String(id)] || 'Java面向对象程序设计') : 'Java面向对象程序设计';
}
</script>

<style scoped lang="scss">
.step-content-box {
  width: 100%;
  max-width: none;
  margin: 0;

  .form-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1.5px solid #e2e8f0;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.03);
    padding: 32px 40px;

    :deep(.el-card__header) {
      padding: 0 0 24px 0;
      border-bottom: 1px solid #f1f5f9;
    }

    .card-header-title {
      display: flex;
      align-items: center;
      gap: 18px;

      .icon-badge {
        width: 52px;
        height: 52px;
        border-radius: 16px;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border: 1.5px solid #bfdbfe;
        color: #2563eb;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
      }

      h3 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #0f172a;
        letter-spacing: -0.01em;
      }

      p {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .step-form-grid {
      margin-top: 28px;

      :deep(.el-form-item__label) {
        font-size: 14px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 8px;
      }

      :deep(.el-input__wrapper),
      :deep(.el-select__wrapper) {
        border-radius: 12px;
        box-shadow: 0 0 0 1px #e2e8f0 inset;
        padding: 4px 14px;
        transition: all 0.2s ease;

        &:hover {
          box-shadow: 0 0 0 1px #93c5fd inset;
        }

        &.is-focus {
          box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2), 0 0 0 1px #2563eb inset !important;
        }
      }

      :deep(.el-textarea__inner) {
        border-radius: 12px;
        border: 1px solid #e2e8f0;
        padding: 12px 16px;
        font-size: 14px;
        line-height: 1.6;
        transition: all 0.2s ease;

        &:focus {
          border-color: #2563eb;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }
      }
    }

    .step-footer-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 36px;
      padding-top: 24px;
      border-top: 1px solid #f1f5f9;

      .next-step-capsule-btn {
        border-radius: 9999px;
        padding: 12px 32px;
        font-size: 15px;
        font-weight: 600;
        background: #2563eb;
        border-color: #2563eb;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
        transition: all 0.2s ease;

        &:hover {
          background: #1d4ed8;
          border-color: #1d4ed8;
          box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
        }
      }
    }
  }
}
</style>
