<template>
  <el-dialog
    :model-value="visible"
    title="校准与编辑记忆条目"
    width="540px"
    class="memory-form-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="记忆维度类别" required>
        <el-select v-model="form.memoryType" style="width: 100%;">
          <el-option label="学习风格与偏好 (PREFERENCE)" value="PREFERENCE" />
          <el-option label="学术画像与盲区 (PROFILE)" value="PROFILE" />
          <el-option label="典型攻坚情境 (EPISODIC)" value="EPISODIC" />
          <el-option label="人机调优反馈 (FEEDBACK)" value="FEEDBACK" />
        </el-select>
      </el-form-item>

      <el-form-item label="敏感级别">
        <el-select v-model="form.sensitivityLevel" style="width: 100%;">
          <el-option label="普通公开 (NORMAL)" value="NORMAL" />
          <el-option label="学术档案 (ACADEMIC)" value="ACADEMIC" />
          <el-option label="高敏感 (HIGH_RISK - 国密 SM4 加密)" value="HIGH_RISK" />
        </el-select>
      </el-form-item>

      <el-form-item label="记忆摘要描述" required>
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="3"
        />
      </el-form-item>

      <el-form-item
        v-if="form.sensitivityLevel === 'HIGH_RISK'"
        label="敏感事实详情明文"
      >
        <el-input
          v-model="form.fullContent"
          type="textarea"
          :rows="3"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer-actions">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" class="save-btn" @click="$emit('submit')">
          保存校准
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
  form: {
    summary: string;
    memoryType: string;
    sensitivityLevel: string;
    fullContent: string;
  };
}>();

defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'submit'): void;
}>();
</script>

<style scoped lang="scss">
.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .save-btn {
    border-radius: 9999px;
    padding: 8px 24px;
  }
}
</style>
