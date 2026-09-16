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
            <el-select v-model="formData.semester" placeholder="选择学期" class="w-full">
              <el-option label="2026年秋季学期" value="2026年秋季学期" />
              <el-option label="2026年春季学期" value="2026年春季学期" />
              <el-option label="2025年秋季学期" value="2025年秋季学期" />
              <el-option label="2025年春季学期" value="2025年春季学期" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid-3">
          <el-form-item label="学科门类" prop="category">
            <el-select v-model="formData.category" placeholder="分类" class="w-full">
              <el-option label="计算机与软件" value="计算机与软件" />
              <el-option label="人工智能与大数据" value="人工智能与大数据" />
              <el-option label="电子信息与通信" value="电子信息与通信" />
              <el-option label="数学与基础学科" value="数学与基础学科" />
              <el-option label="通识与工程素养" value="通识与工程素养" />
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

        <el-form-item label="课程简介与修读要求" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="说明课程重点培养目标、前置学科要求与学习建议..."
            maxlength="500"
            show-word-limit
          />
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
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import { EditPen, Reading, Service, Loading, Opportunity, Tools, Sunny } from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';
import { useCourse } from '@/composables/course/useCourse';

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

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
});

const formData = reactive({
  name: '',
  code: '',
  semester: '2026年秋季学期',
  category: '计算机与软件',
  credits: 3.0,
  plannedHours: 48,
  status: 'ACTIVE',
  description: '',
  aiPersona: 'SOCRATIC',
  welcomeMessage: ''
});

const formRules: FormRules = {
  name: [{ required: true, message: '课程名称不能为空', trigger: 'blur' }]
};

const personaList = [
  {
    key: 'SOCRATIC',
    icon: Opportunity,
    name: '苏格拉底启发式助教',
    desc: '善于通过多轮递进发问引导学生自主推导答案，培养批判性思维'
  },
  {
    key: 'STRICT',
    icon: Reading,
    name: '严谨治学学术导师',
    desc: '注重数学严密性、定理定义准确性与学术学术范式规范'
  },
  {
    key: 'PRACTICAL',
    icon: Tools,
    name: '一线工程实战导师',
    desc: '从工业界工程踩坑、并发高可用、代码架构等视角深度剖析'
  },
  {
    key: 'GENTLE',
    icon: Sunny,
    name: '温和鼓励引路人',
    desc: '通俗易懂拆解晦涩概念，步步正向激励，适合初学者破冰'
  }
];

watch(
  () => props.course,
  newCourse => {
    if (!newCourse) return;
    formData.name = newCourse.title || newCourse.name || '';
    formData.code = newCourse.code || '';
    formData.semester = newCourse.semester || '2026年秋季学期';
    formData.category = newCourse.category || '计算机与软件';
    formData.credits = Number(newCourse.credits) || 3.0;
    formData.plannedHours = Number(newCourse.plannedHours) || 48;
    formData.status = (newCourse.status === 'ARCHIVED' || newCourse.status === 2) ? 'ARCHIVED' : 'ACTIVE';
    formData.description = newCourse.description || '';
    formData.aiPersona = newCourse.aiPersona || 'SOCRATIC';
    formData.welcomeMessage = newCourse.welcomeMessage || '';
  },
  { immediate: true }
);

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
