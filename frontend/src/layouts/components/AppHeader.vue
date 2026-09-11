<template>
  <header class="app-header">
    <div class="header-left">
      <el-button link @click="$emit('toggle-sidebar')">
        <el-icon :size="20"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
      </el-button>
      <div class="global-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索课程、AI 工具、资料或知识点..."
          prefix-icon="Search"
          clearable
        />
      </div>
    </div>

    <div class="header-right">
      <!-- 评审/演示专用：三端角色一键切换器 -->
      <div class="role-switcher">
        <el-dropdown trigger="click" placement="bottom-end" @command="handleRoleSwitch">
          <el-button size="small" :type="currentRoleTagType" plain class="role-switch-btn">
            <el-icon><component :is="currentRoleIcon" /></el-icon>
            <span>身份：{{ currentRoleName }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu class="role-dropdown-menu">
              <el-dropdown-item command="ADMIN" :disabled="authStore.currentRole === 'ADMIN'">
                <span class="role-item-icon role-item-icon--admin">
                  <el-icon><Setting /></el-icon>
                </span>
                <span>切换为：系统管理员 (全量管理权限)</span>
              </el-dropdown-item>
              <el-dropdown-item command="TEACHER" :disabled="authStore.currentRole === 'TEACHER'">
                <span class="role-item-icon role-item-icon--teacher">
                  <el-icon><EditPen /></el-icon>
                </span>
                <span>切换为：骨干教师 (教学出题组卷)</span>
              </el-dropdown-item>
              <el-dropdown-item command="STUDENT" :disabled="authStore.currentRole === 'STUDENT'">
                <span class="role-item-icon role-item-icon--student">
                  <el-icon><Reading /></el-icon>
                </span>
                <span>切换为：统招学生 (课程问答与学习)</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-tooltip content="通知消息" placement="bottom">
        <div class="action-item">
          <el-badge :value="3" class="item">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
        </div>
      </el-tooltip>

      <el-dropdown trigger="click" placement="bottom-end" @command="handleCommand">
        <div class="user-profile">
          <el-avatar :size="34" :src="authStore.currentUser?.avatar" />
          <div class="user-meta">
            <span class="user-name">{{ authStore.currentUser?.realName }}</span>
            <el-tag size="small" :type="currentRoleTagType" effect="plain" class="role-badge">
              {{ currentRoleName }}
            </el-tag>
          </div>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="profile-dropdown-menu">
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-dropdown-item>
            <el-dropdown-item command="security">
              <el-icon><Lock /></el-icon>
              <span>安全设置</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  Fold,
  Expand,
  Search,
  Bell,
  ArrowDown,
  User,
  Setting,
  EditPen,
  Reading,
  Lock,
  SwitchButton
} from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { ElMessage } from 'element-plus';

defineProps<{ isCollapsed: boolean }>();
defineEmits(['toggle-sidebar']);

const router = useRouter();
const authStore = useAuthStore();
const searchKeyword = ref('');

const currentRoleName = computed(() => {
  const role = authStore.currentRole;
  if (role === 'ADMIN') return '系统管理员';
  if (role === 'TEACHER') return '骨干教师';
  return '统招学生';
});

const currentRoleIcon = computed(() => {
  const role = authStore.currentRole;
  if (role === 'ADMIN') return Setting;
  if (role === 'TEACHER') return EditPen;
  return Reading;
});

const currentRoleTagType = computed(() => {
  const role = authStore.currentRole;
  if (role === 'ADMIN') return 'danger';
  if (role === 'TEACHER') return 'primary';
  return 'success';
});

const handleRoleSwitch = async (role: 'ADMIN' | 'TEACHER' | 'STUDENT') => {
  try {
    await authStore.switchRole(role);
    ElMessage.success(`已切换至【${currentRoleName.value}】视角，导航菜单与权限已实时更新`);
    if (router.currentRoute.value.path !== '/dashboard') {
      await router.push('/dashboard');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '角色切换失败');
  }
};

const handleCommand = (cmd: string) => {
  if (cmd === 'logout') {
    authStore.logout();
    router.push('/auth/login');
  } else if (cmd === 'profile') {
    router.push('/profile');
  } else if (cmd === 'security') {
    router.push('/profile/security');
  }
};
</script>

<style scoped lang="scss">
.app-header {
  height: 64px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);
  z-index: 9;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .global-search {
      width: 320px;
      :deep(.el-input__wrapper) {
        background-color: #F1F5F9;
        border-radius: 20px;
        box-shadow: none;
        &:hover, &.is-focus {
          background-color: #FFFFFF;
          box-shadow: 0 0 0 1px #1677FF inset;
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 18px;

    .role-switcher {
      .role-switch-btn {
        border-radius: 16px;
        font-weight: 500;
      }
    }

    .action-item {
      cursor: pointer;
      display: flex;
      align-items: center;
      color: #64748B;
      &:hover { color: #1677FF; }
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 20px;
      transition: background 0.2s;

      &:hover { background: #F8FAFC; }

      .user-meta {
        display: flex;
        flex-direction: column;
        .user-name {
          font-size: 13px;
          font-weight: 600;
          color: #1E293B;
          line-height: 1.2;
        }
        .role-badge {
          margin-top: 2px;
          align-self: flex-start;
          height: 18px;
          padding: 0 6px;
          font-size: 11px;
        }
      }
    }
  }
}
</style>

<style lang="scss">
/* 修复下拉菜单排布：保证垂直纵向（向下）展示 */
.role-dropdown-menu {
  display: flex !important;
  flex-direction: column !important;
  min-width: 290px;
  padding: 6px 0 !important;

  .el-dropdown-menu__item {
    display: flex !important;
    align-items: center;
    gap: 12px;
    padding: 10px 16px !important;
    font-size: 13px;
    width: 100%;
    box-sizing: border-box;

    .role-item-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border-radius: 8px;
      flex-shrink: 0;

      .el-icon {
        font-size: 15px;
      }
    }

    .role-item-icon--admin {
      background: rgba(245, 108, 108, 0.14);
      color: #E03131;
    }

    .role-item-icon--teacher {
      background: rgba(22, 119, 255, 0.12);
      color: #1677FF;
    }

    .role-item-icon--student {
      background: rgba(82, 196, 26, 0.12);
      color: #52C41A;
    }

    &:hover:not(.is-disabled) {
      background-color: #F1F5F9;
    }

    &.is-disabled {
      opacity: 0.55;
      cursor: not-allowed;
      background-color: transparent !important;
    }
  }
}

.profile-dropdown-menu {
  display: flex !important;
  flex-direction: column !important;
  min-width: 140px;
  padding: 6px 0 !important;

  .el-dropdown-menu__item {
    display: flex !important;
    align-items: center;
    gap: 8px;
    padding: 8px 16px !important;
    font-size: 13px;
    width: 100%;
    box-sizing: border-box;

    .el-icon {
      font-size: 15px;
      margin-right: 2px;
    }
  }
}
</style>
