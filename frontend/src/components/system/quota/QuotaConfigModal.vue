<template>
  <el-dialog :model-value="visible" title="配置租户资源全局预警阈值与额度" width="520px" destroy-on-close @update:model-value="emit('update:visible', $event)">
    <el-form label-position="top">
      <el-form-item label="Token 预警阈值水位线 (%)">
        <el-slider v-model="editForm.warningThreshold" :min="50" :max="95" show-input />
        <span class="form-tip">当租户总用量达到该百分比时，系统自动发送平台通知/邮件预警</span>
      </el-form-item>
      <el-form-item label="月度 Token 额度上限 (Tokens)">
        <el-input-number v-model="editForm.tokenLimit" :step="5000000" :min="1000000" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="单校区最高 QPS 限流速率">
        <el-input-number v-model="editForm.qpsLimit" :step="10" :min="10" :max="200" style="width: 100%;" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="emit('save')">保存配额策略</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
  saving: boolean;
  editForm: {
    warningThreshold: number;
    tokenLimit: number;
    qpsLimit: number;
  };
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  save: [];
}>();
</script>
