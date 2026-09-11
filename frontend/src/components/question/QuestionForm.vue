<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    class="question-form-container"
  >
    <div class="form-grid-layout">
      <!-- 左侧：核心题干与选项作答 -->
      <div class="form-main-pane">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header-dock">
              <span class="header-badge">01</span>
              <span class="header-title">试题题干与内容</span>
            </div>
          </template>

          <el-form-item label="题干描述 (Stem)" prop="stem">
            <el-input
              v-model="form.stem"
              type="textarea"
              :rows="5"
              placeholder="请输入清晰的题目描述，支持 LaTeX 数学公式（如 $x \to 0$）与代码块..."
              class="stem-textarea"
            />
          </el-form-item>

          <!-- 选项与标准答案编辑 -->
          <el-form-item label="选项配置与标准答案" prop="correctAnswer">
            <QuestionOptionEditor
              :type="form.type || 'SINGLE_CHOICE'"
              :model-value="form.options"
              :correct-answer="form.correctAnswer"
              @update:model-value="(opts) => form.options = opts"
              @update:correct-answer="(ans) => form.correctAnswer = ans"
            />
          </el-form-item>

          <!-- 解析 -->
          <el-form-item label="试题深度解析与解题思路 (Analysis)" prop="analysis">
            <el-input
              v-model="form.analysis"
              type="textarea"
              :rows="3"
              placeholder="请提供解题依据、公式推导或关键考察点，方便学生作答后复盘..."
            />
          </el-form-item>
        </el-card>
      </div>

      <!-- 右侧：属性归属与分值参数 -->
      <div class="form-side-pane">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header-dock">
              <span class="header-badge">02</span>
              <span class="header-title">试题属性与归属</span>
            </div>
          </template>

          <!-- 试题题型 -->
          <el-form-item label="试题题型" prop="type">
            <el-select v-model="form.type" placeholder="请选择题型" class="w-full" @change="handleTypeChange">
              <el-option label="单项选择题 (Single Choice)" value="SINGLE_CHOICE" />
              <el-option label="多项选择题 (Multiple Choice)" value="MULTIPLE_CHOICE" />
              <el-option label="判断正误题 (True/False)" value="JUDGMENT" />
              <el-option label="填空题 (Fill Blank)" value="FILL_BLANK" />
              <el-option label="简答/主观题 (Essay)" value="SHORT_ANSWER" />
            </el-select>
          </el-form-item>

          <!-- 所属课程 -->
          <el-form-item label="所属课程" prop="courseId">
            <el-select
              v-model="form.courseId"
              placeholder="请选择对应课程"
              class="w-full"
              filterable
              @change="handleCourseChange"
            >
              <el-option
                v-for="c in courses"
                :key="c.id"
                :label="c.title"
                :value="c.id"
              />
            </el-select>
          </el-form-item>

          <!-- 试题难度 -->
          <el-form-item label="难度级别" prop="difficulty">
            <el-radio-group v-model="form.difficulty" class="difficulty-radio-group">
              <el-radio-button label="EASY">基础易 (Easy)</el-radio-button>
              <el-radio-button label="MEDIUM">中等适中 (Medium)</el-radio-button>
              <el-radio-button label="HARD">高阶难 (Hard)</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 分值 -->
          <el-form-item label="默认分值 (Score)" prop="score">
            <el-input-number v-model="form.score" :min="1" :max="100" :step="1" class="w-full" />
          </el-form-item>

          <!-- 考察知识点标签 -->
          <el-form-item label="关联知识点">
            <div class="tags-input-box">
              <el-tag
                v-for="(tag, idx) in form.knowledgePointNames"
                :key="idx"
                closable
                class="kp-tag"
                @close="removeTag(idx)"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="inputTagVisible"
                ref="tagInputRef"
                v-model="inputTagValue"
                size="small"
                class="tag-input"
                placeholder="输入知识点按回车"
                @keyup.enter="handleTagInputConfirm"
                @blur="handleTagInputConfirm"
              />
              <el-button v-else size="small" plain @click="showTagInput">
                + 添加知识点
              </el-button>
            </div>
          </el-form-item>
        </el-card>

        <!-- 提交控制区 -->
        <div class="action-footer-card">
          <el-button size="large" @click="emit('cancel')">取消返回</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ isEdit ? '保存修改' : '确认录入入库' }}
          </el-button>
        </div>
      </div>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, nextTick } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import QuestionOptionEditor from './QuestionOptionEditor.vue';
import { getCourseList } from '@/api/course/course';
import type { Question, QuestionType, Difficulty } from '@/types/question/question';
import type { Course } from '@/types/course/course';

const props = defineProps<{
  modelValue?: Partial<Question>;
  isEdit?: boolean;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: Partial<Question>): void;
  (e: 'submit', form: Partial<Question>): void;
  (e: 'cancel'): void;
}>();

const formRef = ref<FormInstance>();
const courses = ref<Course[]>([]);

const form = reactive<Partial<Question>>({
  courseId: undefined,
  courseName: '',
  type: 'SINGLE_CHOICE' as QuestionType,
  difficulty: 'MEDIUM' as Difficulty,
  score: 5,
  stem: '',
  options: [
    { key: 'A', content: '', isCorrect: false },
    { key: 'B', content: '', isCorrect: false },
    { key: 'C', content: '', isCorrect: false },
    { key: 'D', content: '', isCorrect: false }
  ],
  correctAnswer: '',
  analysis: '',
  knowledgePointNames: []
});

const rules: FormRules = {
  stem: [{ required: true, message: '题干内容不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '请选择试题类型', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  correctAnswer: [{ required: true, message: '请设定正确答案或参考答案', trigger: 'change' }],
  score: [{ required: true, message: '分值必须大于0', trigger: 'blur' }]
};

const inputTagVisible = ref(false);
const inputTagValue = ref('');
const tagInputRef = ref();

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      Object.assign(form, val);
      if (!form.knowledgePointNames) form.knowledgePointNames = [];
    }
  },
  { immediate: true, deep: true }
);

onMounted(async () => {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    courses.value = res.data?.list || [];
    if (!form.courseId && courses.value.length > 0) {
      form.courseId = courses.value[0].id;
      form.courseName = courses.value[0].title;
    }
  } catch (err) {
    console.error('Failed to load courses', err);
  }
});

function handleTypeChange(newType: QuestionType) {
  form.correctAnswer = '';
  if (newType === 'SINGLE_CHOICE' || newType === 'MULTIPLE_CHOICE') {
    form.options = [
      { key: 'A', content: '', isCorrect: false },
      { key: 'B', content: '', isCorrect: false },
      { key: 'C', content: '', isCorrect: false },
      { key: 'D', content: '', isCorrect: false }
    ];
  } else {
    form.options = [];
  }
}

function handleCourseChange(courseId: number) {
  const match = courses.value.find(c => c.id === courseId);
  if (match) {
    form.courseName = match.title;
  }
}

function showTagInput() {
  inputTagVisible.value = true;
  nextTick(() => {
    tagInputRef.value?.focus();
  });
}

function handleTagInputConfirm() {
  const trimmed = inputTagValue.value.trim();
  if (trimmed && !form.knowledgePointNames?.includes(trimmed)) {
    form.knowledgePointNames?.push(trimmed);
  }
  inputTagVisible.value = false;
  inputTagValue.value = '';
}

function removeTag(idx: number) {
  form.knowledgePointNames?.splice(idx, 1);
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate((valid) => {
    if (valid) {
      emit('submit', { ...form });
    }
  });
}
</script>

<style scoped lang="scss">
.question-form-container {
  width: 100%;

  .form-grid-layout {
    display: grid;
    grid-template-columns: 1fr 380px;
    gap: 24px;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }
  }

  .section-card {
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    margin-bottom: 20px;
    background: #ffffff;

    .card-header-dock {
      display: flex;
      align-items: center;
      gap: 10px;

      .header-badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        border-radius: 6px;
        background: #3b82f6;
        color: #ffffff;
        font-weight: 700;
        font-size: 12px;
      }

      .header-title {
        font-size: 16px;
        font-weight: 600;
        color: #0f172a;
      }
    }
  }

  .w-full {
    width: 100%;
  }

  .stem-textarea :deep(.el-textarea__inner) {
    font-size: 15px;
    line-height: 1.6;
  }

  .difficulty-radio-group {
    width: 100%;
    display: flex;
    :deep(.el-radio-button) {
      flex: 1;
      .el-radio-button__inner {
        width: 100%;
      }
    }
  }

  .tags-input-box {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;

    .kp-tag {
      font-size: 13px;
    }

    .tag-input {
      width: 140px;
    }
  }

  .action-footer-card {
    display: flex;
    gap: 14px;
    padding: 16px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 12px;

    .el-button {
      flex: 1;
      font-weight: 600;
    }
  }
}
</style>
