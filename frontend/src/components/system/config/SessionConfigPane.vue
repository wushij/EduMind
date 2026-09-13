<template>
  <div class="session-config-tab">
    <el-form :model="draft" label-width="130px" class="config-form session-form">
      <el-form-item label="Token 有效期">
        <el-input-number v-model="draft.tokenExpireHours" :min="1" :max="720" :disabled="!canEdit" />
        <span class="unit">小时（保存全部后生效）</span>
      </el-form-item>

      <el-form-item label="签名密钥有效期">
        <el-input-number v-model="draft.sessionSignExpireHours" :min="1" :max="120" :disabled="!canEdit" />
        <span class="unit">小时（会话签名/加密临时密钥，保存后下次协商生效）</span>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="建议：将「签名密钥有效期」与「Token 有效期」设为相同的时长（例如均为 24 小时），确保会话生命周期与签名加密凭证同步存活与续约。"
      class="config-tip-alert"
    />
  </div>
</template>

<script setup lang="ts">
import type { SessionConfig } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: SessionConfig;
    canEdit?: boolean;
  }>(),
  {
    canEdit: true
  }
);
</script>

<style scoped>
.config-form {
  max-width: 680px;
  padding-top: 12px;
}

.unit {
  margin-left: 10px;
  color: #94a3b8;
  font-size: 13px;
}

.config-tip-alert {
  max-width: 760px;
  margin-top: 16px;
}
</style>
