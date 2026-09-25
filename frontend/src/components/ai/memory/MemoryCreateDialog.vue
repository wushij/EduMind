<template>
  <el-dialog
    :model-value="visible"
    title="注入教学先验认知记忆"
    width="540px"
    class="memory-form-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-position="top" class="capsule-form">
      <el-form-item label="记忆维度类别" required>
        <el-select v-model="form.memoryType" style="width: 100%;">
          <el-option label="学习风格与偏好 (PREFERENCE)" value="PREFERENCE" />
          <el-option label="学术画像与盲区 (PROFILE)" value="PROFILE" />
          <el-option label="典型攻坚情境 (EPISODIC)" value="EPISODIC" />
          <el-option label="人机调优反馈 (FEEDBACK)" value="FEEDBACK" />
        </el-select>
      </el-form-item>

      <el-form-item label="敏感安全级别 (数据合规)">
        <el-select v-model="form.sensitivityLevel" style="width: 100%;">
          <el-option label="普通公开 (NORMAL)" value="NORMAL" />
          <el-option label="学术档案特征 (ACADEMIC)" value="ACADEMIC" />
          <el-option label="高度敏感 (HIGH_RISK - 启用国密 SM4 加密)" value="HIGH_RISK" />
        </el-select>
      </el-form-item>

      <el-form-item label="记忆核心摘要 (明文索引)" required>
        <!-- 上限与后端 MemorySummaryNormalizer.MAX_SUMMARY_CHARS 对齐，避免超长提交后才在服务端被拒 -->
        <el-input
          v-model="form.memoryValue"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="如：该学生对复合函数求导链式法则极度敏感，但极易遗忘定义域前置检查，建议提问时多设陷阱校验。"
        />
      </el-form-item>

      <el-form-item
        v-if="form.sensitivityLevel === 'HIGH_RISK'"
        label="敏感事实详情 (将被国密 SM4 强加密存储落盘)"
      >
        <el-input
          v-model="form.fullContent"
          type="textarea"
          :rows="3"
          placeholder="输入未经脱敏的详细事实描述，系统将自动使用租户专属 KMS 密钥进行硬件加密..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer-actions">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" class="gradient-submit-btn" @click="$emit('submit')">
          沉淀入库
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
  form: {
    memoryType: string;
    sensitivityLevel: string;
    memoryValue: string;
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

  .gradient-submit-btn {
    border-radius: 9999px;
    padding: 8px 24px;
    background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
    border: none;
  }
}
</style>
