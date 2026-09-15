<template>
  <el-dialog :model-value="visible" :title="`调整算力配额 - ${currentEditingDept?.name || ''}`" width="480px" destroy-on-close @update:model-value="emit('update:visible', $event)">
    <el-form v-if="currentEditingDept" label-position="top">
      <el-form-item label="院系/组织名称">
        <el-input :model-value="currentEditingDept.name" disabled />
      </el-form-item>
      <el-form-item label="AI Token 算力分配额度 (Tokens)">
        <el-input-number v-model="currentEditingDept.tokenLimit" :step="1000000" :min="500000" :max="50000000" style="width: 100%;" />
        <span class="form-tip">当前已消耗：{{ (currentEditingDept.tokenUsed / 10000).toFixed(1) }} 万 Tokens</span>
      </el-form-item>
      <el-form-item label="向量知识库存储配额 (MB)">
        <el-input-number v-model="currentEditingDept.storageLimit" :step="20" :min="10" :max="500" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="Agent 最大并发席位">
        <el-input-number v-model="currentEditingDept.seatsLimit" :step="50" :min="10" :max="1000" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="预警水位线阈值 (%)">
        <el-input-number v-model="currentEditingDept.warningThreshold" :step="5" :min="50" :max="95" style="width: 100%;" />
        <span class="form-tip">当该部门已消耗配额达到此水位时触发系统预警</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="emit('save')">保存院系配额</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { OrgQuotaVO } from '@/types/system/tenant';

defineProps<{
  visible: boolean;
  saving: boolean;
  currentEditingDept: OrgQuotaVO | null;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  save: [];
}>();
</script>
