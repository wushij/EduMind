<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    class="question-form-container"
  >
    <div class="form-grid-layout">
      <!-- 左侧主栏：试题题干、选项配置与深度解析 (Main Content Pane) -->
      <div class="form-main-pane">
        <!-- 01 试题题干与内容 -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header-dock">
              <div class="header-left">
                <span class="header-badge">01</span>
                <span class="header-title">试题题干与内容</span>
              </div>
              <div class="header-ai-tools">
                <button
                  type="button"
                  class="ai-mini-btn"
                  @click="handlePolishStem"
                >
                  <el-icon><AiSparkleIcon /></el-icon>
                  <span>AI 润色题干</span>
                </button>
              </div>
            </div>
          </template>

          <!-- 题干输入 -->
          <el-form-item label="题干描述 (Stem)" prop="stem" class="stem-form-item">
            <QuestionLatexEditor
              ref="stemInputRef"
              v-model="form.stem"
              :rows="5"
              placeholder="请输入试题题干，支持 LaTeX：行内 $...$，独立 $$...$$"
              input-class="stem-textarea"
            />
          </el-form-item>

          <!-- 选项与标准答案编辑 -->
          <div class="field-section-header">
            <div class="field-title-box">
              <span class="required-asterisk">*</span>
              <span class="field-title-text">选项配置与标准答案</span>
            </div>
            <div
              v-if="form.type === 'SINGLE_CHOICE' || form.type === 'MULTIPLE_CHOICE'"
              class="field-ai-action"
            >
              <button
                type="button"
                class="ai-link-btn"
                @click="handleGenerateOptions"
              >
                <el-icon><Opportunity /></el-icon>
                <span>AI 自动生成选项与典型干扰项</span>
              </button>
            </div>
          </div>
          <el-form-item prop="correctAnswer" class="no-label-item">
            <QuestionOptionEditor
              :type="form.type || 'SINGLE_CHOICE'"
              :model-value="form.options"
              :correct-answer="form.correctAnswer"
              @update:model-value="(opts) => form.options = opts"
              @update:correct-answer="(ans) => form.correctAnswer = ans"
            />
          </el-form-item>

          <!-- 深度解析 -->
          <div class="field-section-header">
            <div class="field-title-box">
              <span class="field-title-text">试题深度解析与解题思路 (Analysis)</span>
            </div>
            <div class="field-ai-action">
              <button
                type="button"
                class="ai-link-btn"
                @click="handleGenerateAnalysis"
              >
                <el-icon><Reading /></el-icon>
                <span>AI 智能生成考点解析</span>
              </button>
            </div>
          </div>
          <el-form-item prop="analysis" class="no-label-item">
            <QuestionLatexEditor
              v-model="form.analysis"
              :rows="4"
              placeholder="请输入解题思路、易错点等，支持 LaTeX 公式..."
              input-class="analysis-textarea"
            />
          </el-form-item>
        </el-card>
      </div>

      <!-- 右侧侧栏：试题属性归属与分值参数 (Sidebar Pane) -->
      <div class="form-side-pane">
        <el-card shadow="never" class="section-card side-prop-card">
          <template #header>
            <div class="card-header-dock">
              <div class="header-left">
                <span class="header-badge">02</span>
                <span class="header-title">试题属性与归属</span>
              </div>
              <div class="header-ai-tools"></div>
            </div>
          </template>

          <!-- 试题题型 -->
          <el-form-item label="试题题型" prop="type">
            <el-select v-model="form.type" placeholder="请选择题型" class="w-full prop-select" @change="handleTypeChange">
              <el-option label="单项选择题 (Single Choice)" value="SINGLE_CHOICE" />
              <el-option label="多项选择题 (Multiple Choice)" value="MULTIPLE_CHOICE" />
              <el-option label="判断正误题 (True/False)" value="TRUE_FALSE" />
              <el-option label="填空题 (Fill Blank)" value="FILL_BLANK" />
              <el-option label="简答/主观题 (Essay)" value="SHORT_ANSWER" />
            </el-select>
          </el-form-item>

          <!-- 所属课程 (必定回显真实课程名称，绝不出现纯数字ID) -->
          <el-form-item label="所属课程" prop="courseId">
            <el-select
              v-model="form.courseId"
              placeholder="请选择对应课程"
              class="w-full prop-select"
              filterable
              @change="handleCourseChange"
            >
              <el-option
                v-for="c in displayCourses"
                :key="c.id"
                :label="c.name || c.title || `课程 #${c.id}`"
                :value="c.id"
              />
            </el-select>
          </el-form-item>

          <!-- 难度级别 (长圆胶囊边框，1:1:1 三等分单行不折行) -->
          <el-form-item label="难度级别" prop="difficulty">
            <div class="segmented-difficulty-dock">
              <button
                type="button"
                class="seg-btn"
                :class="{ 'is-active': form.difficulty === 'EASY' }"
                @click="form.difficulty = 'EASY'"
              >
                <span class="seg-dot seg-dot--green" />
                <span class="seg-title">基础易</span>
                <span class="seg-subtitle">Easy</span>
              </button>
              <button
                type="button"
                class="seg-btn"
                :class="{ 'is-active': form.difficulty === 'MEDIUM' }"
                @click="form.difficulty = 'MEDIUM'"
              >
                <span class="seg-dot seg-dot--amber" />
                <span class="seg-title">中等</span>
                <span class="seg-subtitle">Med</span>
              </button>
              <button
                type="button"
                class="seg-btn"
                :class="{ 'is-active': form.difficulty === 'HARD' }"
                @click="form.difficulty = 'HARD'"
              >
                <span class="seg-dot seg-dot--red" />
                <span class="seg-title">高阶难</span>
                <span class="seg-subtitle">Hard</span>
              </button>
            </div>
          </el-form-item>

          <!-- 默认分值 -->
          <el-form-item label="默认分值 (Score)" prop="score">
            <div class="score-dock">
              <el-input-number
                v-model="form.score"
                :min="1"
                :max="100"
                :step="1"
                controls-position="right"
                class="score-number-input"
              />
              <span class="score-unit-tag">分</span>
            </div>
          </el-form-item>
        </el-card>

        <!-- 提交控制区 -->
        <div class="action-footer-card">
          <button type="button" class="btn-cancel" @click="emit('cancel')">
            取消返回
          </button>
          <button
            type="button"
            class="btn-submit"
            :disabled="submitting"
            @click="handleSubmit"
          >
            <el-icon v-if="!submitting" class="mr-1"><Check /></el-icon>
            <span>{{ submitting ? '保存中...' : (isEdit ? '保存修改' : '确认录入入库') }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 全局统一 AI 认知推演与辅导弹窗面板 (带雷达波纹、实时计时与中止控制) -->
    <QuestionAiThinkingModal
      v-model:visible="aiModalVisible"
      :mode="aiModalMode"
      :form="form"
      @finish="handleAiFinish"
    />
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, nextTick, computed } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  Opportunity,
  Reading,
  Plus,
  Close,
  Check
} from '@element-plus/icons-vue';
import QuestionOptionEditor from './QuestionOptionEditor.vue';
import QuestionLatexEditor from './common/QuestionLatexEditor.vue';
import QuestionAiThinkingModal, { type ThinkingMode } from './QuestionAiThinkingModal.vue';
import { fetchCoursesForForm } from '@/composables/question/useQuestion';
import { useQuestionFormAiCopilot } from '@/composables/question/useQuestionFormAiCopilot';
import { useAuthStore } from '@/stores/auth/auth';
import type { Question, QuestionType, Difficulty } from '@/types/question/question';
import type { Course } from '@/types/course/course';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

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

const authStore = useAuthStore();
const canUseAi = computed(() => {
  return authStore.hasAnyRole(['ADMIN', 'TEACHER']) || authStore.hasAnyPermission(['ai:chat', 'ai:question', 'ai:generation']);
});

const stemInputRef = ref();

const {
  openInteractiveAiTutor
} = useQuestionFormAiCopilot();

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

// 课程名一律来自课程接口返回的真实数据；接口不可用时只做「课程 #id」占位，
// 不再用写死的 id → 名称映射（会把 102 这类 id 错标成固定课程名，与真实课程不符）。
// 解决数字ID回显问题：确保 courses 中必定有可读名称，绝不出现纯数字
const displayCourses = computed<any[]>(() => {
  const list: any[] = courses.value.map(c => {
    const rawName = (c as any).name || c.title || (c as any).courseName;
    const resolvedName = rawName || `课程 #${c.id}`;
    return {
      ...c,
      id: Number(c.id),
      name: resolvedName,
      title: resolvedName
    };
  });

  if (form.courseId) {
    const exists = list.some(c => String(c.id) === String(form.courseId));
    if (!exists) {
      const fallbackTitle = form.courseName || `课程 #${form.courseId}`;
      list.unshift({
        id: Number(form.courseId) as any,
        name: fallbackTitle,
        title: fallbackTitle
      } as Course);
    }
  }
  return list;
});

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
    const list = await fetchCoursesForForm(50);
    courses.value = list;
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
  const match = displayCourses.value.find(c => String(c.id) === String(courseId));
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

const hasStem = computed(() => {
  return Boolean(form.stem && form.stem.trim().length >= 2);
});

function focusStem() {
  nextTick(() => {
    try {
      stemInputRef.value?.focus?.();
    } catch {
      // 容错
    }
  });
}

const aiModalVisible = ref(false);
const aiModalMode = ref<ThinkingMode>('fullAuto');

function openAiThinking(mode: ThinkingMode) {
  if (!canUseAi.value) {
    ElMessage.warning('抱歉，当前账号暂无 AI 智能命题权限');
    return;
  }
  if (!hasStem.value) {
    const actionName =
      mode === 'fullAuto' ? '进行全套出题装配'
      : mode === 'tutor' ? '咨询 AI 命题助教'
      : mode === 'options' ? '生成选项与干扰项'
      : mode === 'analysis' ? '生成考点解析'
      : '进行题干规范润色';
    ElMessage.warning(`题目尚未输入任何信息，请先在下方输入试题题干内容后再${actionName}！`);
    focusStem();
    return;
  }
  aiModalMode.value = mode;
  aiModalVisible.value = true;
}

function handleAiFinish(patch: Partial<Question>) {
  if (patch.stem !== undefined) {
    form.stem = patch.stem;
  }
  if (patch.analysis !== undefined) {
    form.analysis = patch.analysis;
  }
  if (patch.options?.length) {
    form.options = patch.options.map((o) => ({
      key: o.key,
      content: o.content,
      isCorrect: !!o.isCorrect
    }));
  }
  if (patch.correctAnswer !== undefined && patch.correctAnswer !== '') {
    form.correctAnswer = patch.correctAnswer;
  }
  if (patch.difficulty) {
    form.difficulty = patch.difficulty;
  }
  if (patch.score) {
    form.score = patch.score;
  }
  if (patch.knowledgePointNames?.length) {
    form.knowledgePointNames = [...patch.knowledgePointNames];
  }
  if (aiModalMode.value === 'fullAuto') {
    ElMessage.success('AI 已完成整道试题的全套智能出题装配！');
  } else if (aiModalMode.value === 'options') {
    ElMessage.success('AI 已基于题干生成 4 个选项并标记正确项！');
  } else if (aiModalMode.value === 'analysis') {
    ElMessage.success('AI 考点深度解析已生成并填入！');
  } else if (aiModalMode.value === 'polish') {
    ElMessage.success('题干已完成 AI 学术化规范润色！');
  } else if (aiModalMode.value === 'tutor') {
    ElMessage.success('已成功采纳 AI 命题助教的教研优化建议并更新试题！');
  }
}

function handleGenerateOptions() {
  openAiThinking('options');
}

function handleGenerateAnalysis() {
  openAiThinking('analysis');
}

function handlePolishStem() {
  openAiThinking('polish');
}

watch(
  () => form,
  () => {
    emit('update:modelValue', { ...form });
  },
  { deep: true }
);

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate((valid) => {
    if (valid) {
      emit('submit', { ...form });
    }
  });
}

// 供父级 Hero 头部按钮远程调用与校验
defineExpose({
  triggerFullAuto: () => openAiThinking('fullAuto'),
  triggerAiTutor: () => openInteractiveAiTutor(form),
  getStem: () => form.stem,
  focusStem
});
</script>

<style scoped lang="scss">
.question-form-container {
  width: 100%;

  /* 经典的左右双栏布局 (Left-Main + Right-Sidebar) */
  .form-grid-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(340px, 390px);
    gap: 20px;
    align-items: flex-start;

    @media (max-width: 1080px) {
      grid-template-columns: 1fr;
    }
  }

  .section-card {
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    margin-bottom: 0;
    background: #ffffff;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);

    :deep(.el-card__header) {
      padding: 16px 22px;
      border-bottom: 1px solid #f1f5f9;
    }

    :deep(.el-card__body) {
      padding: 22px 24px 24px;
    }

    .card-header-dock {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 10px;

        .header-badge {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 26px;
          height: 26px;
          border-radius: 8px;
          background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
          color: #ffffff;
          font-weight: 800;
          font-size: 12px;
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
        }

        .header-title {
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
        }
      }

      .header-ai-tools {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
  }

  /* 精巧 AI 小按钮 */
  .ai-mini-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 30px;
    padding: 0 12px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    color: #4f46e5;
    background: #eef2ff;
    border: 1px solid #c7d2fe;
    cursor: pointer;
    transition: all 0.15s ease;

    .el-icon {
      font-size: 13px;
    }

    &:hover {
      background: #e0e7ff;
      border-color: #818cf8;
      color: #3730a3;
      transform: translateY(-1px);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  /* 字段分组头部规范（红星号不换行，AI按钮靠最右侧对齐） */
  .field-section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    margin-top: 20px;
    margin-bottom: 8px;

    .field-title-box {
      display: inline-flex;
      align-items: center;
      gap: 5px;

      .required-asterisk {
        color: #ef4444;
        font-size: 15px;
        line-height: 1;
        font-weight: 700;
        user-select: none;
      }

      .field-title-text {
        font-size: 14px;
        font-weight: 600;
        color: #334155;
      }
    }

    .field-ai-action {
      margin-left: auto; /* 彻底推到卡片最右边缘 */
      display: flex;
      align-items: center;
    }
  }

  .no-label-item {
    margin-bottom: 8px;

    :deep(.el-form-item__label) {
      display: none !important;
    }

    :deep(.el-form-item__content) {
      line-height: normal;
    }
  }

  .ai-link-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 14px;
    border-radius: 9999px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    color: #2563eb;
    font-weight: 600;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.15s ease;

    .el-icon {
      font-size: 13px;
    }

    &:hover {
      background: #dbeafe;
      border-color: #93c5fd;
      color: #1d4ed8;
      transform: translateY(-1px);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .w-full {
    width: 100%;
  }

  .prop-select {
    :deep(.el-select__wrapper) {
      border-radius: 10px;
      min-height: 40px;
    }
  }

  .stem-textarea :deep(.el-textarea__inner) {
    font-size: 15px;
    line-height: 1.65;
    border-radius: 12px;
    padding: 14px 16px;
    background: #fafcff;
    border: 1.5px solid #e2e8f0;

    &:focus {
      background: #ffffff;
      border-color: #3b82f6;
    }
  }

  .analysis-textarea :deep(.el-textarea__inner) {
    font-size: 14px;
    line-height: 1.6;
    border-radius: 12px;
    padding: 12px 16px;
    background: #fafcff;
    border: 1.5px solid #e2e8f0;

    &:focus {
      background: #ffffff;
      border-color: #3b82f6;
    }
  }

  /* 难度级别长圆胶囊边框 (Pill Segmented, 100% 单行不折行) */
  .segmented-difficulty-dock {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 4px;
    width: 100%;
    background: #f1f5f9;
    padding: 4px;
    border-radius: 9999px; // 长圆边框
    border: 1.5px solid #e2e8f0;
    box-sizing: border-box;

    .seg-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 5px;
      height: 36px;
      border: none;
      background: transparent;
      border-radius: 9999px; // 内部也是长圆形
      cursor: pointer;
      transition: all 0.18s ease;
      padding: 0 4px;
      white-space: nowrap;

      .seg-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        flex-shrink: 0;

        &--green { background: #10b981; }
        &--amber { background: #f59e0b; }
        &--red { background: #ef4444; }
      }

      .seg-title {
        font-size: 12.5px;
        font-weight: 600;
        color: #475569;
      }

      .seg-subtitle {
        font-size: 11px;
        color: #94a3b8;
      }

      &:hover {
        background: rgba(255, 255, 255, 0.7);
        color: #0f172a;
      }

      &.is-active {
        background: #ffffff;
        color: #0f172a;
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);

        .seg-title {
          color: #0f172a;
          font-weight: 700;
        }

        .seg-subtitle {
          color: #64748b;
        }
      }
    }
  }

  .score-dock {
    display: flex;
    align-items: center;
    gap: 10px;

    .score-number-input {
      flex: 1;

      :deep(.el-input__wrapper) {
        border-radius: 10px;
        height: 40px;
      }
    }

    .score-unit-tag {
      font-size: 14px;
      font-weight: 700;
      color: #64748b;
      white-space: nowrap;
    }
  }



  .action-footer-card {
    display: flex;
    gap: 12px;
    padding: 16px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    box-shadow: 0 4px 18px rgba(15, 23, 42, 0.03);
    margin-top: 20px;

    .btn-cancel {
      flex: 1;
      height: 44px;
      border-radius: 9999px;
      border: 1.5px solid #cbd5e1;
      background: #ffffff;
      color: #475569;
      font-weight: 600;
      font-size: 13.5px;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover {
        background: #f8fafc;
        border-color: #94a3b8;
        color: #1e293b;
      }
    }

    .btn-submit {
      flex: 1.6;
      height: 44px;
      border-radius: 9999px;
      border: none;
      background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
      color: #ffffff;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      transition: all 0.2s ease;

      &:hover {
        background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(22, 119, 255, 0.4);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }
}
</style>
