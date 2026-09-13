<template>
  <el-form :model="draft" label-width="140px" class="config-form" autocomplete="off" @submit.prevent>
    <el-form-item label="登录人机校检">
      <el-switch v-model="draft.captchaEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.captchaEnabled" label="验证码类型">
      <el-radio-group v-model="draft.captchaType" :disabled="!canEdit">
        <el-radio value="image">图片</el-radio>
        <el-radio value="slider">滑块</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="短信验证码登录">
      <el-switch v-model="draft.smsLoginEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.smsLoginEnabled" label="发送前滑块验证">
      <el-switch v-model="draft.smsLoginSliderCaptchaEnabled" :disabled="!canEdit" />
      <span class="unit">获取短信验证码前需完成滑块验证</span>
    </el-form-item>
    <el-form-item label="邮箱验证码登录">
      <el-switch v-model="draft.emailLoginEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.emailLoginEnabled" label="邮箱发送前滑块">
      <el-switch v-model="draft.emailLoginSliderCaptchaEnabled" :disabled="!canEdit" />
      <span class="unit">获取邮箱登录验证码前需完成滑块验证</span>
    </el-form-item>
    <el-form-item label="记住我">
      <el-switch v-model="draft.rememberMe" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="账号最大重试">
      <el-input-number v-model="draft.maxRetryCount" :min="1" :max="20" :disabled="!canEdit" />
      <span class="unit">次后锁定该账号</span>
    </el-form-item>
    <el-form-item label="IP 最大重试">
      <el-input-number v-model="draft.maxRetryCountIp" :min="1" :max="50" :disabled="!canEdit" />
      <span class="unit">次后锁定该 IP（同一出口共享计数）</span>
    </el-form-item>
    <el-form-item label="锁定时长">
      <el-input-number v-model="draft.lockTime" :min="1" :max="120" :disabled="!canEdit" />
      <span class="unit">分钟</span>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { LoginAdminConfig } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: LoginAdminConfig;
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
</style>
