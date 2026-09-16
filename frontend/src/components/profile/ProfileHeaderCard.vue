<template>
  <div class="profile-header-card">
    <div class="user-avatar-block">
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        accept="image/jpeg,image/png,image/gif,image/webp"
        :before-upload="beforeAvatarUpload"
        :http-request="handleAvatarUpload"
      >
        <img :src="displayAvatar" alt="Avatar" class="avatar-img" @error="handleAvatarError" />
        <div class="avatar-upload-mask">
          <el-icon><Camera /></el-icon>
          <span>更换头像</span>
        </div>
      </el-upload>
    </div>

    <div class="user-main-info">
      <div class="name-title-row">
        <h1 class="user-name">{{ currentUser?.realName || '平台学者' }}</h1>
        <span class="username-tag">@{{ currentUser?.username }}</span>
        <span class="role-badge-pill">{{ roleLabel }}</span>
      </div>
      <p class="user-dept">{{ profileForm.department || '未填写所属院系' }}</p>
    </div>

    <!-- 快捷角色一键切换演示器 (用于评审演示快速切视角) -->
    <div v-if="isDev" class="demo-role-switcher">
      <span class="switcher-hint">演示角色切换：</span>
      <div class="switcher-pills">
        <button
          type="button"
          class="role-switch-pill"
          :class="{ active: currentRole === 'ADMIN' }"
          @click="handleSwitchRole('ADMIN')"
        >
          <el-icon class="role-icon"><Management /></el-icon>
          <span>超级管理员</span>
        </button>
        <button
          type="button"
          class="role-switch-pill"
          :class="{ active: currentRole === 'TEACHER' }"
          @click="handleSwitchRole('TEACHER')"
        >
          <el-icon class="role-icon"><School /></el-icon>
          <span>任课教师</span>
        </button>
        <button
          type="button"
          class="role-switch-pill"
          :class="{ active: currentRole === 'STUDENT' }"
          @click="handleSwitchRole('STUDENT')"
        >
          <el-icon class="role-icon"><Reading /></el-icon>
          <span>在校学生</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Management, School, Reading, Camera } from '@element-plus/icons-vue';
import type { UserInfo } from '@/types/auth/auth';
import type { UploadRequestOptions } from 'element-plus';

defineProps<{
  isDev: boolean;
  currentUser: UserInfo | null;
  currentRole: string;
  displayAvatar: string;
  roleLabel: string;
  profileForm: { department: string };
  handleAvatarError: () => void;
  handleSwitchRole: (role: 'ADMIN' | 'TEACHER' | 'STUDENT') => void;
  beforeAvatarUpload: (file: File) => boolean;
  handleAvatarUpload: (options: UploadRequestOptions) => void;
}>();
</script>

<style scoped lang="scss">
.profile-header-card {
  background: #FFFFFF;
  border-radius: 24px;
  padding: 28px 32px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;

  .user-avatar-block {
    flex-shrink: 0;

    .avatar-uploader {
      position: relative;
      display: inline-block;
      cursor: pointer;

      :deep(.el-upload) {
        position: relative;
        display: inline-block;
      }
    }

    .avatar-img {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      border: 2px solid #1677FF;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.2);
      object-fit: cover;
      display: block;
    }

    .avatar-upload-mask {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      border-radius: 50%;
      background: rgba(15, 23, 42, 0.55);
      color: #fff;
      font-size: 11px;
      font-weight: 600;
      opacity: 0;
      transition: opacity 0.2s;

      .el-icon {
        font-size: 18px;
      }
    }

    .avatar-uploader:hover .avatar-upload-mask {
      opacity: 1;
    }
  }

  .user-main-info {
    flex: 1;
    min-width: 200px;

    .name-title-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 4px;

      .user-name {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
      }

      .username-tag {
        font-size: 13px;
        color: #94A3B8;
      }

      .role-badge-pill {
        white-space: nowrap;
        padding: 2px 10px;
        border-radius: 9999px;
        background: #1677FF;
        color: #FFFFFF;
        font-size: 10.5px;
        font-weight: 600;
        line-height: 1.4;
        box-shadow: 0 2px 6px rgba(22, 119, 255, 0.3);
      }
    }

    .user-dept {
      margin: 0;
      font-size: 13px;
      color: #64748B;
    }
  }

  .demo-role-switcher {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 18px;
    border-radius: 14px;
    background: #F8FAFC;
    border: 1px dashed #CBD5E1;

    .switcher-hint {
      font-size: 11px;
      color: #94A3B8;
      font-weight: 600;
    }

    .switcher-pills {
      display: flex;
      align-items: center;
      gap: 6px;

      .role-switch-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 5px 14px;
        border-radius: 9999px; // 长圆按钮
        border: 1px solid #E2E8F0;
        background: #FFFFFF;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        .role-icon {
          font-size: 14px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
        }

        &:hover {
          border-color: #93C5FD;
          color: #1677FF;

          .role-icon {
            color: #1677FF;
          }
        }

        &.active {
          background: #1677FF;
          border-color: #1677FF;
          color: #FFFFFF;
          font-weight: 600;

          .role-icon {
            color: #FFFFFF;
          }
        }
      }
    }
  }
}
</style>
