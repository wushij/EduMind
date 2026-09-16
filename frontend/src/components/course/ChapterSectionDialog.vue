<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑微课节内容' : '新增大纲微课节'"
    width="520px"
    destroy-on-close
    append-to-body
    class="section-dialog"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
    >
      <div class="parent-chapter-tip">
        <span class="tip-label">所属大纲章节：</span>
        <strong class="tip-value">{{ chapterTitle || '当前章节' }}</strong>
      </div>

      <el-form-item label="微课节标题" prop="title" required>
        <el-input
          v-model="formData.title"
          placeholder="例如：第1节 核心数据结构内存布局与指针实现"
          maxlength="80"
          show-word-limit
        />
      </el-form-item>

      <div class="form-grid-2">
        <el-form-item label="课节修读时长" prop="duration">
          <el-select v-model="formData.duration" class="w-full">
            <el-option label="15 分钟（精炼微课）" value="15分钟" />
            <el-option label="30 分钟（标准课时）" value="30分钟" />
            <el-option label="45 分钟（深度讲析）" value="45分钟" />
            <el-option label="60 分钟（综合实训）" value="60分钟" />
          </el-select>
        </el-form-item>

        <el-form-item label="课节教学形式" prop="type">
          <el-select v-model="formData.type" class="w-full">
            <el-option label="讲义 / 理论精讲" value="lecture" />
            <el-option label="工程代码演练" value="practice" />
            <el-option label="智能自测诊断" value="quiz" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="核心考查知识点数量" prop="knowledgePointCount">
        <el-input-number
          v-model="formData.knowledgePointCount"
          :min="1"
          :max="10"
          class="w-full"
        />
      </el-form-item>

      <el-form-item label="课节学习目标与导读">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="简述该微课节的核心考核要点、学习目标与难点..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer-actions">
        <button type="button" class="capsule-btn-cancel" @click="visible = false">取消</button>
        <button
          type="button"
          class="capsule-btn-confirm"
          :disabled="submitting || !formData.title.trim()"
          @click="handleSubmit"
        >
          <span>{{ submitting ? '保存中...' : (isEdit ? '保存修改' : '确认录入课节') }}</span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { FormInstance, FormRules } from 'element-plus';

const props = defineProps<{
  modelValue: boolean;
  chapterId: number;
  chapterTitle?: string;
  initialData?: any | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'submit', data: { title: string; duration: string; type: string; knowledgePointCount: number; description?: string }): void;
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
});

const isEdit = computed(() => Boolean(props.initialData?.id));

const formData = reactive({
  title: '',
  duration: '30分钟',
  type: 'lecture',
  knowledgePointCount: 2,
  description: ''
});

const formRules: FormRules = {
  title: [{ required: true, message: '微课节标题不能为空', trigger: 'blur' }]
};

watch(
  () => props.initialData,
  init => {
    if (init) {
      formData.title = init.title || '';
      formData.duration = init.duration || '30分钟';
      formData.type = init.type || 'lecture';
      formData.knowledgePointCount = init.knowledgePointCount || 2;
      formData.description = init.description || '';
    } else {
      formData.title = '';
      formData.duration = '30分钟';
      formData.type = 'lecture';
      formData.knowledgePointCount = 2;
      formData.description = '';
    }
  },
  { immediate: true }
);

function handleSubmit() {
  if (!formData.title.trim()) return;
  submitting.value = true;
  try {
    emit('submit', {
      title: formData.title.trim(),
      duration: formData.duration,
      type: formData.type,
      knowledgePointCount: formData.knowledgePointCount,
      description: formData.description.trim()
    });
    visible.value = false;
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped lang="scss">
.parent-chapter-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  margin-bottom: 16px;

  .tip-label {
    color: #3B82F6;
  }

  .tip-value {
    color: #1D4ED8;
  }
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;

  .capsule-btn-cancel {
    height: 36px;
    padding: 0 18px;
    border-radius: 9999px;
    background: #F1F5F9;
    border: 1px solid #CBD5E1;
    color: #475569;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #E2E8F0;
      color: #0F172A;
    }
  }

  .capsule-btn-confirm {
    height: 36px;
    padding: 0 20px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
    border: none;
    color: #FFFFFF;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
    transition: all 0.2s;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}
</style>
