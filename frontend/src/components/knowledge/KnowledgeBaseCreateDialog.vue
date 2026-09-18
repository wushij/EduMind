<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑知识库基本信息' : '创建新课程专有知识库'"
    width="580px"
    class="capsule-custom-dialog"
    :show-close="true"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form
      ref="createFormRef"
      :model="createForm"
      :rules="createRules"
      label-position="top"
      class="capsule-dialog-form"
    >
      <el-form-item label="知识库名称" prop="name">
        <input
          v-model="createForm.name"
          type="text"
          class="capsule-form-input"
          placeholder="例如：大学物理（下）微课与习题知识库"
        />
      </el-form-item>

      <el-form-item label="所属学科/类别" prop="category">
        <div class="category-pill-selector">
          <span
            v-for="cat in categoryOptions"
            :key="cat.value"
            class="cat-opt-pill"
            :class="{ active: createForm.category === cat.value }"
            @click="createForm.category = cat.value"
          >
            <el-icon v-if="cat.icon" class="cat-opt-icon"><component :is="cat.icon" /></el-icon>
            <span>{{ cat.label }}</span>
          </span>
        </div>
      </el-form-item>

      <el-form-item label="关联课程大纲" prop="courseId">
        <el-select
          v-model="createForm.courseId"
          placeholder="不关联特定课程（通用公共库）"
          clearable
          filterable
          size="large"
          class="kb-dialog-select w-full"
        >
          <el-option
            v-for="course in courses"
            :key="course.id"
            :label="course.title"
            :value="course.id"
          />
        </el-select>
        <span v-if="courses.length === 0" class="field-hint-text">
          当前暂无可选课程，将创建为通用公共库；可在课程中心创建课程后编辑绑定。
        </span>
        <span v-else class="field-hint-text">清空选择即不绑定课程，知识库仍可独立使用。</span>
      </el-form-item>

      <el-form-item label="向量嵌入模型 (Embedding Model)" prop="embeddingModel">
        <el-select v-model="createForm.embeddingModel" size="large" class="kb-dialog-select w-full">
          <el-option
            label="bge-large-zh-v1.5 (1024维 - 推荐中文教学)"
            value="bge-large-zh-v1.5 (1024维)"
          />
          <el-option
            label="text-embedding-3-small (1536维 - 多语言高精度)"
            value="text-embedding-3-small (1536维)"
          />
          <el-option
            label="bge-m3 (多模态高密度嵌入 - 跨语言大文档)"
            value="bge-m3 (多模态高密度嵌入)"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="知识库简介与入库文档说明" prop="description">
        <textarea
          v-model="createForm.description"
          rows="3"
          class="capsule-form-textarea"
          placeholder="简述该知识库收录的课件、习题或文献范围，以便 AI 助教优先检索..."
        ></textarea>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-actions-dock">
        <button
          type="button"
          class="capsule-modal-btn capsule-modal-btn--cancel"
          @click="emit('update:visible', false)"
        >
          <span>取消</span>
        </button>
        <button
          type="button"
          class="capsule-modal-btn capsule-modal-btn--confirm"
          @click="emit('confirm')"
        >
          <span>{{ isEdit ? '保存修改' : '立即创建知识库' }}</span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { Component } from 'vue';

type CategoryTab = {
  label: string;
  value: string;
  icon?: Component;
};

type CreateForm = {
  name: string;
  category: string;
  courseId?: number;
  courseName?: string;
  embeddingModel: string;
  description: string;
};

withDefaults(
  defineProps<{
    visible: boolean;
    isEdit?: boolean;
    createForm: CreateForm;
    createRules: Record<string, unknown>;
    categoryOptions: CategoryTab[];
    courses?: Array<{ id: number; title: string }>;
  }>(),
  {
    isEdit: false,
    courses: () => []
  }
);

const emit = defineEmits<{
  'update:visible': [value: boolean];
  confirm: [];
}>();
</script>

<style scoped lang="scss">
.capsule-dialog-form {
  .w-full {
    width: 100%;
  }

  .field-hint-text {
    display: block;
    margin-top: 6px;
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.45;
  }

  .kb-dialog-select {
    :deep(.el-select__wrapper) {
      border-radius: 9999px !important;
      min-height: 42px;
      box-shadow: 0 0 0 1px #e2e8f0 inset !important;
      padding: 0 16px;
    }

    :deep(.el-select__wrapper.is-focused) {
      box-shadow: 0 0 0 2px #1677ff inset !important;
    }
  }

  .capsule-form-input {
    width: 100%;
    height: 42px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;
    background: #ffffff;
    padding: 0 18px;
    font-size: 13.5px;
    color: #1e293b;
    outline: none;
    box-sizing: border-box;
    transition: all 0.2s;

    &:focus {
      border-color: #1677ff;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
    }
  }

  .capsule-form-textarea {
    width: 100%;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    background: #ffffff;
    padding: 12px 16px;
    font-size: 13.5px;
    color: #1e293b;
    outline: none;
    box-sizing: border-box;
    resize: vertical;
    font-family: inherit;

    &:focus {
      border-color: #1677ff;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
    }
  }

  .category-pill-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .cat-opt-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 16px;
      border-radius: 9999px;
      border: 1px solid #e2e8f0;
      background: #f8fafc;
      color: #64748b;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      .cat-opt-icon {
        font-size: 13px;
      }

      &:hover {
        border-color: #93c5fd;
        color: #1677ff;
      }

      &.active {
        background: #eff6ff;
        border-color: #1677ff;
        color: #1677ff;
        font-weight: 600;
      }
    }
  }
}

.dialog-actions-dock {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  .capsule-modal-btn {
    height: 40px;
    padding: 0 24px;
    border-radius: 9999px;
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: all 0.2s;

    &--cancel {
      background: #f1f5f9;
      color: #64748b;

      &:hover {
        background: #e2e8f0;
      }
    }

    &--confirm {
      background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
      color: #ffffff;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.28);

      &:hover {
        background: linear-gradient(135deg, #4096ff 0%, #1d4ed8 100%);
      }
    }
  }
}
</style>
