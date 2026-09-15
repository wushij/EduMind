<template>
  <div class="top-nav-bar">
    <el-button :icon="ArrowLeft" link class="back-link" @click="onBack">
      返回用户列表
    </el-button>
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/system/users' }">用户权限管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ userName || '用户画像详情' }}</el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';

defineProps<{
  userName?: string;
  onBack: () => void;
}>();
</script>

<style scoped lang="scss">
.top-nav-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  .back-link {
    font-size: 14px;
    font-weight: 500;
    color: #3b82f6;
  }
}
</style>
