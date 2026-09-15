<template>
  <div class="detail-body-grid">
    <el-card shadow="never" class="info-card">
      <h3 class="card-title">
        <el-icon class="mr-1"><User /></el-icon>
        <span>用户基础档案</span>
      </h3>
      <div class="fields-list">
        <div class="field-item">
          <span class="label">用户名/账号：</span>
          <span class="val font-mono">{{ userInfo?.username }}</span>
        </div>
        <div class="field-item">
          <span class="label">真实姓名：</span>
          <span class="val font-semibold">{{ userInfo?.realName }}</span>
        </div>
        <div class="field-item">
          <span class="label">电子邮箱：</span>
          <span class="val">{{ userInfo?.email || 'user@edumind.edu.cn' }}</span>
        </div>
        <div class="field-item">
          <span class="label">联系手机：</span>
          <span class="val font-mono">{{ userInfo?.phone || '13800001234' }}</span>
        </div>
        <div class="field-item">
          <span class="label">归属院系/班级：</span>
          <span class="val">{{ userInfo?.department || '计算机科学与技术学院 · 软件工程系' }}</span>
        </div>
        <div class="field-item">
          <span class="label">注册加入时间：</span>
          <span class="val text-slate-500">{{ userInfo?.createTime || '2026-09-01 09:00:00' }}</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="info-card">
      <div class="card-title-line">
        <h3 class="card-title">
          <el-icon class="mr-1"><Lock /></el-icon>
          <span>角色身份与权限分配</span>
        </h3>
        <el-button type="primary" link size="small" @click="editRolesModal = true">
          调整分配角色
        </el-button>
      </div>

      <div class="roles-allocated-box">
        <div v-for="r in (userInfo?.roles as string[])" :key="r" class="role-desc-card">
          <div class="role-top">
            <span class="role-badge">{{ r }}</span>
            <span class="role-title">{{ getRoleLabel(r) }}</span>
          </div>
          <p class="role-note">{{ getRoleDescription(r) }}</p>
        </div>
      </div>
    </el-card>
  </div>

  <el-dialog v-model="editRolesModal" title="调整用户系统角色" width="480px" destroy-on-close>
    <el-checkbox-group v-model="selectedRoles" class="role-checkboxes">
      <el-checkbox label="ROLE_ADMIN">系统超级管理员 (ROLE_ADMIN)</el-checkbox>
      <el-checkbox label="ROLE_TEACHER">课程主讲教师 (ROLE_TEACHER)</el-checkbox>
      <el-checkbox label="ROLE_STUDENT">修读学生 (ROLE_STUDENT)</el-checkbox>
    </el-checkbox-group>

    <template #footer>
      <el-button @click="editRolesModal = false">取消</el-button>
      <el-button type="primary" @click="onSaveRoles">确认保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue';
import { getRoleLabel, getRoleDescription } from '@/composables/system/useUserDetail';

const editRolesModal = defineModel<boolean>('editRolesModal', { required: true });
const selectedRoles = defineModel<string[]>('selectedRoles', { required: true });

defineProps<{
  userInfo: Record<string, unknown> | null;
  onSaveRoles: () => void;
}>();
</script>

<style scoped lang="scss">
.detail-body-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  .info-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    padding: 16px 20px;

    .card-title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 16px;
    }

    .card-title-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      .card-title {
        margin: 0;
      }
    }

    .fields-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .field-item {
        display: flex;
        font-size: 13px;

        .label {
          width: 120px;
          color: #64748b;
        }

        .val {
          color: #1e293b;
        }
      }
    }

    .roles-allocated-box {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .role-desc-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 12px 14px;

        .role-top {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 6px;

          .role-badge {
            font-family: monospace;
            font-size: 11px;
            background: #eff6ff;
            color: #2563eb;
            padding: 2px 6px;
            border-radius: 4px;
          }

          .role-title {
            font-size: 13px;
            font-weight: 700;
            color: #1e293b;
          }
        }

        .role-note {
          font-size: 12px;
          color: #64748b;
          margin: 0;
          line-height: 1.5;
        }
      }
    }
  }
}

.role-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 10px 0;
}
</style>
