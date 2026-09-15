<template>
  <div class="course-create-page">
    <!-- 顶部导航与面包屑 -->
    <div class="create-header">
      <button type="button" class="back-link-btn" @click="router.push('/course')">
        <svg viewBox="0 0 24 24" class="back-arrow-svg" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
        <span>返回课程中心</span>
      </button>
      <h1 class="page-title">创建新课程空间</h1>
      <p class="page-subtitle">配置课程基本信息，系统将自动挂载 AI 助教助手并初始化知识体系架构</p>
    </div>

    <!-- 创建表单主卡片 -->
    <div class="form-container-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="capsule-form"
      >
        <!-- 课程名称 -->
        <el-form-item label="课程全称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="例如：计算机组成原理与体系结构"
            size="large"
          />
        </el-form-item>

        <!-- 课程代号与学期 (双列排布) -->
        <div class="form-row-two-cols">
          <el-form-item label="课程代码 / 编号" prop="code">
            <el-input
              v-model="form.code"
              placeholder="例如：CS3001"
              size="large"
            />
          </el-form-item>

          <el-form-item label="开课学期" prop="semester">
            <el-select
              v-model="form.semester"
              placeholder="请选择开课学期"
              size="large"
              class="w-100"
            >
              <el-option label="2026年秋季学期" value="2026秋季学期" />
              <el-option label="2027年春季学期" value="2027春季学期" />
              <el-option label="2026年暑期实训课" value="2026暑期实训" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 课程简介 -->
        <el-form-item label="课程简介与修读要求" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="简要介绍课程教学目标、重点涵盖章节，AI 助教将以此为依据向选课学生提供背景引导..."
            class="refined-textarea"
          />
        </el-form-item>

        <!-- 课程封面选择 (预设学科渐变 / 自定义封面) -->
        <el-form-item label="课程封面与视觉主题">
          <div class="cover-selection-grid">
            <div
              v-for="preset in presetCovers"
              :key="preset.id"
              class="preset-cover-card"
              :class="[preset.gradientClass, { active: selectedCoverId === preset.id }]"
              @click="selectPresetCover(preset)"
            >
              <span class="preset-name">{{ preset.name }}</span>
              <div v-if="selectedCoverId === preset.id" class="check-badge">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>

          <div class="custom-url-row">
            <span class="custom-url-label">或输入自定义封面图片 URL：</span>
            <el-input
              v-model="form.coverUrl"
              placeholder="https://..."
              size="default"
              clearable
            />
          </div>
        </el-form-item>

        <!-- 底部长圆操作按钮 -->
        <div class="form-actions-row">
          <button
            type="button"
            class="action-btn action-btn--cancel"
            @click="router.push('/course')"
          >
            取消
          </button>
          <button
            type="button"
            class="action-btn action-btn--submit"
            :disabled="submitting"
            @click="handleSubmit"
          >
            <span v-if="!submitting">立即创建并进入空间</span>
            <span v-else>正在初始化空间...</span>
          </button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import { Check } from '@element-plus/icons-vue';
import { useCourse } from '@/composables/course/useCourse';

const router = useRouter();
const { createCourse } = useCourse();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  name: '',
  code: '',
  semester: '2026秋季学期',
  description: '',
  coverUrl: ''
});

const rules = reactive<FormRules>({
  name: [{ required: true, message: '请输入课程全称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  semester: [{ required: true, message: '请选择学期', trigger: 'change' }]
});

const selectedCoverId = ref(1);
const presetCovers = [
  { id: 1, name: '计算科技蓝', gradientClass: 'grad-blue' },
  { id: 2, name: '数学几何紫', gradientClass: 'grad-purple' },
  { id: 3, name: '智能仿生青', gradientClass: 'grad-cyan' },
  { id: 4, name: '工程系统靛', gradientClass: 'grad-indigo' }
];

function selectPresetCover(preset: typeof presetCovers[0]) {
  selectedCoverId.value = preset.id;
  form.coverUrl = '';
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        const newCourse = await createCourse(form);
        if (!newCourse) {
          ElMessage.error('创建失败，请重试');
          return;
        }
        ElMessage.success(`课程【${newCourse.title}】创建成功！`);
        router.push(`/course/${newCourse.id}/overview`);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : '创建失败，请检查网络';
        ElMessage.error(msg);
      } finally {
        submitting.value = false;
      }
    }
  });
}
</script>

<style scoped lang="scss">
.course-create-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  padding: 6px 0 40px;

  .create-header {
    padding: 6px 0;

    .back-link-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0;
      background: transparent;
      border: none;
      color: #64748B;
      font-size: 13.5px;
      font-weight: 500;
      cursor: pointer;
      margin-bottom: 12px;
      transition: color 0.2s;

      .back-arrow-svg {
        width: 16px;
        height: 16px;
      }

      &:hover {
        color: #1677FF;
      }
    }

    .page-title {
      margin: 0 0 6px 0;
      font-size: 24px;
      font-weight: 700;
      color: #0F172A;
    }

    .page-subtitle {
      margin: 0;
      font-size: 13.5px;
      color: #64748B;
    }
  }

  // 表单主体卡片（与课程中心筛选面板/列表区保持同款全宽容器）
  .form-container-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 28px 32px 32px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);

    .capsule-form {
      :deep(.el-form-item__label) {
        font-weight: 600;
        color: #1E293B;
        font-size: 14px;
        margin-bottom: 8px;
      }

      .form-row-two-cols {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
      }

      .w-100 {
        width: 100%;
      }

      // 文本域专属精致圆角
      .refined-textarea {
        :deep(.el-textarea__inner) {
          border-radius: 16px !important;
          border: 1px solid #E2E8F0;
          padding: 14px 18px;
          font-size: 13.5px;
          color: #1E293B;
          transition: all 0.2s;

          &:hover {
            border-color: #CBD5E1;
          }

          &:focus {
            border-color: #1677FF;
            box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.16);
          }
        }
      }

      // 预设封面选择器
      .cover-selection-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 14px;
        width: 100%;
        margin-bottom: 12px;

        .preset-cover-card {
          position: relative;
          height: 72px;
          border-radius: 14px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #FFFFFF;
          font-size: 13px;
          font-weight: 600;
          border: 2px solid transparent;
          transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

          &.grad-blue { background: linear-gradient(135deg, #2563EB, #1D4ED8); }
          &.grad-purple { background: linear-gradient(135deg, #7C3AED, #4F46E5); }
          &.grad-cyan { background: linear-gradient(135deg, #0284C7, #0D9488); }
          &.grad-indigo { background: linear-gradient(135deg, #4338CA, #312E81); }

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
          }

          &.active {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px #FFFFFF, 0 0 0 4px #1677FF;
          }

          .check-badge {
            position: absolute;
            top: 6px;
            right: 8px;
            width: 18px;
            height: 18px;
            background: #FFFFFF;
            color: #1677FF;
            border-radius: 50%;
            font-size: 11px;
            font-weight: 800;
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }
      }

      .custom-url-row {
        width: 100%;
        display: flex;
        align-items: center;
        gap: 12px;

        .custom-url-label {
          font-size: 12.5px;
          color: #64748B;
          white-space: nowrap;
        }
      }

      // 底部操作长圆按钮
      .form-actions-row {
        margin-top: 32px;
        padding-top: 24px;
        border-top: 1px solid #F1F5F9;
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 14px;

        .action-btn {
          height: 44px;
          padding: 0 26px;
          border-radius: 9999px; // 长圆跑道胶囊
          font-size: 14.5px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.22s ease;

          &--cancel {
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            color: #64748B;

            &:hover {
              background: #F8FAFC;
              color: #1E293B;
              border-color: #CBD5E1;
            }
          }

          &--submit {
            background: #1677FF;
            border: none;
            color: #FFFFFF;
            box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);

            &:hover {
              background: #4096FF;
              transform: translateY(-1px);
              box-shadow: 0 8px 20px rgba(22, 119, 255, 0.4);
            }

            &:active {
              background: #0958D9;
            }

            &:disabled {
              opacity: 0.6;
              cursor: not-allowed;
            }
          }
        }
      }
    }
  }
}
</style>
