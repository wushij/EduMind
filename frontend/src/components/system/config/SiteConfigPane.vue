<template>
  <el-form :model="draft" label-width="120px" class="config-form" autocomplete="off" @submit.prevent>
    <!-- 隐藏诱饵输入框拦截浏览器自动填充 -->
    <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
      <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
      <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
    </div>

    <el-form-item label="平台名称">
      <el-input v-model="draft.platformName" name="edumind_site_platform_name" maxlength="50" placeholder="例如：智教云 · EduMind" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="平台副标题">
      <el-input v-model="draft.platformSubtitle" name="edumind_site_platform_sub" maxlength="80" placeholder="例如：AI 智能教学赋能平台" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="登录页标题">
      <el-input v-model="draft.loginWelcome" name="edumind_site_login_welcome" maxlength="30" placeholder="例如：欢迎登录智教云平台" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="注册页标题">
      <el-input v-model="draft.registerTitle" name="edumind_site_register_title" maxlength="30" placeholder="例如：开启智教未来之旅" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="页脚版权">
      <el-input v-model="draft.copyright" name="edumind_site_copyright" maxlength="120" placeholder="例如：Copyright © 2026 EduMind. All rights reserved." :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="ICP备案展示">
      <el-switch v-model="draft.icpEnabled" :disabled="!canEdit" />
      <span class="form-item-tip">开启后将在网站登录页、注册页及系统底部显示ICP备案号</span>
    </el-form-item>
    <el-form-item v-if="draft.icpEnabled" label="ICP备案号">
      <el-input v-model="draft.icpNumber" name="edumind_site_icp_number" maxlength="60" placeholder="例如：京ICP备20260001号-1" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.icpEnabled" label="工信部链接">
      <el-input v-model="draft.icpUrl" name="edumind_site_icp_url" maxlength="120" placeholder="默认：https://beian.miit.gov.cn" :disabled="!canEdit" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { SiteConfig } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: SiteConfig;
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

.form-item-tip {
  margin-left: 12px;
  font-size: 13px;
  color: #94a3b8;
}
</style>
