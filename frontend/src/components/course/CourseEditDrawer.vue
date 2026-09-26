<template>
  <el-drawer
    v-model="visible"
    title="编辑课程信息与 AI 配置"
    size="560px"
    destroy-on-close
    class="course-edit-drawer"
  >
    <template #header>
      <div class="drawer-custom-header">
        <div class="header-icon-circle">
          <el-icon><EditPen /></el-icon>
        </div>
        <div class="header-title-meta">
          <h3>编辑课程档案</h3>
          <p>更新课程基本教学元数据及专属 AI 助教人设配置</p>
        </div>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
      class="edit-course-form"
    >
      <!-- 基础信息卡片 -->
      <div class="form-section-card">
        <div class="section-badge">
          <el-icon><Reading /></el-icon>
          <span>基础教学信息</span>
        </div>

        <el-form-item label="课程名称" prop="name" required>
          <el-input
            v-model="formData.name"
            placeholder="例如：高级Java分布式系统架构与实战"
            maxlength="60"
            show-word-limit
          />
        </el-form-item>

        <div class="form-grid-2">
          <el-form-item label="课程代号（班级选课码）" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="例如：AI2026-CS01"
              maxlength="30"
            />
          </el-form-item>

          <el-form-item label="开课学期" prop="semester">
            <el-select
              v-model="formData.semester"
              placeholder="选择或输入学期"
              class="w-full"
              filterable
              allow-create
              default-first-option
            >
              <el-option
                v-for="option in semesterOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid-3">
          <el-form-item label="学科门类" prop="category">
            <el-select
              v-model="formData.category"
              placeholder="选择或输入学科专业"
              class="w-full"
              filterable
              allow-create
              default-first-option
            >
              <el-option
                v-for="cat in courseCategoryPresets"
                :key="cat"
                :label="cat"
                :value="cat"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="课程学分" prop="credits">
            <el-input-number
              v-model="formData.credits"
              :min="0.5"
              :max="10"
              :step="0.5"
              :precision="1"
              class="w-full"
            />
          </el-form-item>

          <el-form-item label="计划总学时" prop="plannedHours">
            <el-input-number
              v-model="formData.plannedHours"
              :min="8"
              :max="160"
              :step="8"
              class="w-full"
            />
          </el-form-item>
        </div>

        <el-form-item label="课程教学状态" prop="status">
          <el-radio-group v-model="formData.status" class="status-radio-group">
            <el-radio-button value="ACTIVE">教学进行中</el-radio-button>
            <el-radio-button value="ARCHIVED">已归档结课</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item prop="description">
          <template #label>
            <div class="label-with-ai">
              <span>课程简介与修读要求</span>
              <button
                type="button"
                class="ai-inline-pill"
                :disabled="!course?.id || descAiLoading"
                @click="handleAiDescription"
              >
                <el-icon v-if="descAiLoading" class="is-loading"><Loading /></el-icon>
                <el-icon v-else><AiSparkleIcon /></el-icon>
                AI 帮写
              </button>
            </div>
          </template>
          <AiCognitiveThinkingSlot
            :active="descAiLoading"
            preset-key="courseDescription"
            show-footer-actions
            abort-label="中止帮写"
            @abort="abortDescAi"
          >
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="说明课程重点培养目标、前置学科要求与学习建议..."
              maxlength="500"
              show-word-limit
            />
          </AiCognitiveThinkingSlot>
        </el-form-item>
      </div>

      <!-- AI 助教专属配置卡片 -->
      <div class="form-section-card form-section-card--ai">
        <div class="section-badge section-badge--ai">
          <el-icon><Service /></el-icon>
          <span>课程专属 AI 助教设定</span>
        </div>

        <el-form-item label="AI 助教人设交互风格" prop="aiPersona">
          <div class="persona-cards-grid">
            <div
              v-for="p in personaList"
              :key="p.key"
              class="persona-card-item"
              :class="{ 'is-selected': formData.aiPersona === p.key }"
              @click="formData.aiPersona = p.key"
            >
              <div class="persona-icon-avatar">
                <el-icon :size="20"><component :is="p.icon" /></el-icon>
              </div>
              <div class="persona-info">
                <strong>{{ p.name }}</strong>
                <p>{{ p.desc }}</p>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="AI 助教专属问候开场白" prop="welcomeMessage">
          <el-input
            v-model="formData.welcomeMessage"
            type="textarea"
            :rows="2"
            placeholder="例如：同学你好！我是本课程专属 AI 助教，7x24 小时为你提供大纲解析与代码答疑。"
            maxlength="150"
            show-word-limit
          />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <div class="drawer-footer-actions">
        <button type="button" class="capsule-drawer-btn" @click="visible = false">取消</button>
        <button
          type="button"
          class="capsule-drawer-btn capsule-drawer-btn--primary"
          :disabled="saving"
          @click="handleSaveCourse"
        >
          <el-icon v-if="saving" class="is-loading"><Loading /></el-icon>
          <span>{{ saving ? '保存中...' : '确认保存课程信息' }}</span>
        </button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus';
import { EditPen, Reading, Service, Loading, Opportunity, Tools, Sunny } from '@element-plus/icons-vue';
import { suggestCourseDescription } from '@/api/ai/course-profile';
import type { Course } from '@/types/course/course';
import { useCourse } from '@/composables/course/useCourse';
import { COURSE_CATEGORY_PRESETS } from '@/constants/course';
import { buildSemesterOptions, getCurrentSemester, type SemesterOption } from '@/constants/semester';
import {
  COURSE_AI_PERSONA_OPTIONS,
  normalizeCourseAiPersona,
  type CourseAiPersonaId
} from '@/constants/course/ai-persona';
import AiCognitiveThinkingSlot from '@/components/ai/common/AiCognitiveThinkingSlot.vue';
import { isAxiosError } from 'axios';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const courseCategoryPresets = COURSE_CATEGORY_PRESETS;

const props = defineProps<{
  modelValue: boolean;
  course: Course | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'saved', updated: Course): void;
}>();

const { saveCourse } = useCourse();
const formRef = ref<FormInstance>();
const saving = ref(false);
const descAiLoading = ref(false);
let descAiAbortController: AbortController | null = null;

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
});

const formData = reactive({
  name: '',
  code: '',
  semester: getCurrentSemester(),
  category: '计算机与软件',
  credits: 3.0,
  plannedHours: 48,
  status: 'ACTIVE',
  description: '',
  aiPersona: 'socrates' as CourseAiPersonaId,
  welcomeMessage: ''
});

/**
 * 开课学期选项：只列当前及未来学期。
 * 编辑历史课程时其原学期已不在候选中，这里兜底把它补回首位，
 * 否则 el-select 找不到匹配项会显示为空，看起来像学期数据丢了。
 */
const semesterOptions = computed<SemesterOption[]>(() => {
  const options = buildSemesterOptions();
  const original = formData.semester;
  if (!original || options.some(option => option.value === original)) return options;
  return [{ label: original, value: original }, ...options];
});

const formRules: FormRules = {
  name: [{ required: true, message: '课程名称不能为空', trigger: 'blur' }]
};

const personaIconMap: Record<CourseAiPersonaId, typeof Opportunity> = {
  socrates: Opportunity,
  academic: Reading,
  engineer: Tools,
  gentle: Sunny
};

const personaList = COURSE_AI_PERSONA_OPTIONS.map(p => ({
  key: p.id,
  name: p.name,
  desc: p.desc,
  icon: personaIconMap[p.id]
}));

function applyCourseToForm(newCourse: Course) {
  formData.name = newCourse.title || newCourse.name || '';
  formData.code = newCourse.code || '';
  formData.semester = newCourse.semester || getCurrentSemester();
  formData.category = newCourse.category || '计算机与软件';
  formData.credits = Number(newCourse.credits) || 3.0;
  formData.plannedHours = Number(newCourse.plannedHours) || 48;
  formData.status = (newCourse.status === 'ARCHIVED' || newCourse.status === 2) ? 'ARCHIVED' : 'ACTIVE';
  formData.description = newCourse.description || '';
  formData.aiPersona = normalizeCourseAiPersona(newCourse.aiPersona);
  formData.welcomeMessage = newCourse.welcomeMessage || '';
}

watch(
  () => props.course,
  newCourse => {
    if (!newCourse) return;
    applyCourseToForm(newCourse);
  },
  { immediate: true }
);

watch(visible, open => {
  if (open && props.course) {
    applyCourseToForm(props.course);
  } else if (!open) {
    abortDescAi();
  }
});

function abortDescAi() {
  if (descAiAbortController) {
    descAiAbortController.abort();
    descAiAbortController = null;
  }
  descAiLoading.value = false;
}

async function handleAiDescription() {
  if (!props.course?.id) return;
  if (formData.description.trim()) {
    try {
      await ElMessageBox.confirm('将用 AI 生成内容替换当前简介，是否继续？', 'AI 帮写', {
        type: 'info',
        confirmButtonText: '继续生成',
        cancelButtonText: '取消'
      });
    } catch {
      return;
    }
  }
  abortDescAi();
  descAiAbortController = new AbortController();
  descAiLoading.value = true;
  try {
    const res = await suggestCourseDescription(props.course.id, {
      signal: descAiAbortController.signal
    });
    const payload = res?.data;
    const text = payload?.text?.trim();
    if (!text) {
      ElMessage.warning('未生成有效简介，请稍后重试');
      return;
    }
    formData.description = text.slice(0, 500);
    ElMessage.success(
      payload?.aiGenerated
        ? 'AI 模型已生成课程简介，可继续编辑后保存'
        : '模型暂不可用，已使用课程上下文兜底生成简介'
    );
  } catch (err: unknown) {
    if (isAxiosError(err) && err.code === 'ERR_CANCELED') {
      return;
    }
    const msg =
      (err as { message?: string })?.message ||
      (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
    if (msg) {
      ElMessage.error(msg);
    }
  } finally {
    descAiAbortController = null;
    descAiLoading.value = false;
  }
}

async function handleSaveCourse() {
  if (!props.course?.id) return;
  if (!formRef.value) return;

  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    await saveCourse(props.course.id, {
      name: formData.name.trim(),
      code: formData.code.trim(),
      semester: formData.semester,
      category: formData.category,
      credits: formData.credits,
      plannedHours: formData.plannedHours,
      description: formData.description.trim(),
      aiPersona: formData.aiPersona,
      welcomeMessage: formData.welcomeMessage.trim(),
      status: formData.status
    } as any);

    ElMessage.success('课程档案与 AI 配置更新成功！');
    visible.value = false;
    emit('saved', {
      ...props.course,
      title: formData.name,
      name: formData.name,
      code: formData.code,
      semester: formData.semester,
      category: formData.category,
      credits: formData.credits,
      plannedHours: formData.plannedHours,
      description: formData.description,
      aiPersona: formData.aiPersona,
      welcomeMessage: formData.welcomeMessage,
      status: formData.status as any
    });
  } catch (err: any) {
    ElMessage.error(err?.message || '保存课程信息失败，请稍后重试');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="scss">
.label-with-ai {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;
}

.ai-inline-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  border-radius: 9999px;
  border: 1px solid #d8b4fe;
  background: #faf5ff;
  color: #7c3aed;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.drawer-custom-header {
  display: flex;
  align-items: center;
  gap: 12px;

  .header-icon-circle {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
    color: #FFFFFF;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  }

  .header-title-meta {
    h3 {
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
      margin: 0;
    }
    p {
      font-size: 12px;
      color: #64748B;
      margin: 2px 0 0 0;
    }
  }
}

.edit-course-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section-card {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  &--ai {
    background: linear-gradient(180deg, #F0FDF4 0%, #F8FAFC 100%);
    border-color: #BBF7D0;
  }

  .section-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
    color: #2563EB;
    margin-bottom: 4px;

    &--ai {
      color: #059669;
    }
  }
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-grid-3 {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 12px;
}

.persona-cards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 100%;

  .persona-card-item {
    display: flex;
    gap: 10px;
    padding: 10px 12px;
    background: #FFFFFF;
    border: 1.5px solid #E2E8F0;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    .persona-icon-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: #F1F5F9;
      color: #2563EB;
      flex-shrink: 0;
    }

    .persona-info {
      strong {
        display: block;
        font-size: 13px;
        color: #0F172A;
        margin-bottom: 2px;
      }
      p {
        font-size: 11px;
        color: #64748B;
        margin: 0;
        line-height: 1.4;
      }
    }

    &:hover {
      border-color: #93C5FD;
      background: #F8FAFC;
    }

    &.is-selected {
      border-color: #2563EB;
      background: #EFF6FF;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);

      .persona-icon-avatar {
        background: #DBEAFE;
        color: #1D4ED8;
      }

      strong {
        color: #1D4ED8;
      }
    }
  }
}

.drawer-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  .capsule-drawer-btn {
    height: 38px;
    padding: 0 20px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    border: 1px solid #CBD5E1;
    background: #FFFFFF;
    color: #475569;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s;

    &:hover {
      background: #F1F5F9;
      color: #0F172A;
    }

    &--primary {
      background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
      color: #FFFFFF;
      border: none;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

      &:hover:not(:disabled) {
        opacity: 0.92;
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }
}
</style>
