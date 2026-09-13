<template>
  <el-form :model="draft" label-width="120px" class="config-form" autocomplete="off" @submit.prevent>
    <el-form-item label="开放注册">
      <el-switch v-model="draft.enabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.enabled" label="人机校检">
      <el-switch v-model="draft.captchaEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.enabled && draft.captchaEnabled" label="验证码类型">
      <el-radio-group v-model="draft.captchaType" :disabled="!canEdit">
        <el-radio value="image">图片</el-radio>
        <el-radio value="slider">滑块</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item v-if="draft.enabled" label="默认角色">
      <el-select v-model="draft.defaultRoleCode" placeholder="请选择默认角色" :disabled="!canEdit" style="width: 220px">
        <el-option v-for="r in roleOptions" :key="r.code" :label="r.name" :value="r.code" />
      </el-select>
    </el-form-item>
    <el-form-item v-if="draft.enabled" label="密码最小长度">
      <el-input-number v-model="draft.minPasswordLength" :min="6" :max="32" :disabled="!canEdit" />
      <span class="unit">位</span>
    </el-form-item>
    <el-form-item v-if="draft.enabled" label="人工审核">
      <el-switch v-model="draft.needAudit" :disabled="!canEdit" />
      <span class="unit">开启后，新注册用户需管理员审核通过后方可登录</span>
    </el-form-item>
    <el-form-item v-if="draft.enabled && draft.needAudit" label="审核人" class="config-auditor-field">
      <el-select v-model="draft.auditorUserIds" multiple filterable placeholder="留空则超级管理员均可审核" :disabled="!canEdit" class="config-auditor-select">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.label" :value="u.id" />
      </el-select>
      <p class="config-auditor-field__hint">指定用户后仅其可在用户列表审核；未指定则超级管理员均可审核</p>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { RegisterAdminConfig, RoleOption, UserOption } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: RegisterAdminConfig;
    roleOptions: RoleOption[];
    userOptions: UserOption[];
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

.config-auditor-field {
  width: 100%;
}

.config-auditor-select {
  width: 100%;
}

.config-auditor-field__hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}
</style>
