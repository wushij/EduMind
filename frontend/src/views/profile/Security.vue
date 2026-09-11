<template>
  <div class="page-container">
    <div class="page-header">
      <h2>账号安全</h2>
    </div>
    <div class="page-content">
      <el-card shadow="never">
        <el-form label-position="top" @submit.prevent="handleChangePassword">
          <el-form-item label="当前密码">
            <el-input v-model="form.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="form.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="handleChangePassword">
            修改密码
          </el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { changePassword } from '@/api/auth/auth';
import { useAuthStore } from '@/stores/auth/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const loading = ref(false);
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

async function handleChangePassword() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写完整密码信息');
    return;
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }
  loading.value = true;
  try {
    await changePassword(form.oldPassword, form.newPassword);
    ElMessage.success('密码修改成功，请重新登录');
    authStore.logout();
    router.push('/auth/login');
  } catch {
    ElMessage.error('密码修改失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="scss">
.page-container {
  .page-header {
    margin-bottom: 16px;
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #1f2937;
    }
  }
}
</style>
