<template>
  <el-dialog
    :model-value="visible"
    title="创建新课程教学知识库"
    width="560px"
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

      <el-form-item label="关联课程大纲" prop="courseName">
        <select v-model="createForm.courseName" class="capsule-form-select">
          <option value="高等数学（上）">高等数学（上）</option>
          <option value="数据结构与算法">数据结构与算法</option>
          <option value="大学物理">大学物理</option>
          <option value="人工智能导论">人工智能导论</option>
          <option value="线性代数">线性代数</option>
          <option value="不关联特定课程（通用）">不关联特定课程（通用）</option>
        </select>
      </el-form-item>

      <el-form-item label="向量嵌入模型 (Embedding Model)" prop="embeddingModel">
        <select v-model="createForm.embeddingModel" class="capsule-form-select">
          <option value="bge-large-zh-v1.5 (1024维)">bge-large-zh-v1.5 (1024维 - 推荐中文教学)</option>
          <option value="text-embedding-3-small (1536维)">text-embedding-3-small (1536维 - 多语言均衡)</option>
          <option value="bge-m3 (多模态高密度嵌入)">bge-m3 (多模态高密度嵌入)</option>
        </select>
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
          <span>立即创建知识库</span>
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
  courseName: string;
  embeddingModel: string;
  description: string;
};

defineProps<{
  visible: boolean;
  createForm: CreateForm;
  createRules: Record<string, unknown>;
  categoryOptions: CategoryTab[];
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  confirm: [];
}>();
</script>

<style scoped lang="scss">
.capsule-dialog-form {
  .capsule-form-input,
  .capsule-form-select {
    width: 100%;
    height: 42px;
    border-radius: 9999px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    padding: 0 18px;
    font-size: 13.5px;
    color: #1E293B;
    outline: none;
    box-sizing: border-box;
    transition: all 0.2s;

    &:focus {
      border-color: #1677FF;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
    }
  }

  .capsule-form-textarea {
    width: 100%;
    border-radius: 14px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    padding: 12px 16px;
    font-size: 13.5px;
    color: #1E293B;
    outline: none;
    box-sizing: border-box;
    resize: vertical;
    font-family: inherit;

    &:focus {
      border-color: #1677FF;
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
      border: 1px solid #E2E8F0;
      background: #F8FAFC;
      color: #64748B;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      .cat-opt-icon {
        font-size: 13px;
      }

      &:hover {
        border-color: #93C5FD;
        color: #1677FF;
      }

      &.active {
        background: #EFF6FF;
        border-color: #1677FF;
        color: #1677FF;
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
      background: #F1F5F9;
      color: #64748B;

      &:hover {
        background: #E2E8F0;
      }
    }

    &--confirm {
      background: #1677FF;
      color: #FFFFFF;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.28);

      &:hover {
        background: #4096FF;
      }
    }
  }
}
</style>
