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
      <!-- 多租户与校区一键切换器 (V2.0) -->
      <div class="tenant-switcher">
        <el-dropdown trigger="click" placement="bottom-end" @command="handleTenantSwitch" @visible-change="handleDropdownVisible">
          <el-button size="small" class="tenant-switch-btn" plain>
            <el-icon class="tenant-icon"><School /></el-icon>
            <span class="tenant-text">{{ tenantStore.activeTenantName }}</span>
            <el-tag size="small" type="primary" effect="plain" class="campus-tag">{{ tenantStore.activeCampusName }}</el-tag>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu class="tenant-dropdown-menu">
              <el-dropdown-item
                v-for="tenant in displayTenants"
                :key="tenant.id"
                :command="tenant.id"
                :disabled="tenantStore.currentTenant?.id === tenant.id"
              >
                <div class="tenant-menu-item">
                  <div class="tenant-item-title">
                    <span class="name">{{ tenant.name }}</span>
                    <el-tag v-if="tenantStore.currentTenant?.id === tenant.id" size="small" type="success" effect="light">当前</el-tag>
                  </div>
                  <div class="tenant-item-meta">
                    <span>编码: {{ tenant.tenantCode || tenant.code }}</span>
                    <span v-if="tenant.campusCount">校区: {{ tenant.campusCount }}</span>
                  </div>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

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

      <NotificationBell />

      <el-dropdown trigger="click" placement="bottom-end" @command="handleCommand">
        <div class="user-profile">
          <el-avatar :size="34" :src="authStore.currentUser?.avatar || DEFAULT_AVATAR" />
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
  ArrowDown,
  User,
  Setting,
  EditPen,
  Reading,
  Lock,
  SwitchButton,
  School
} from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { useTenantStore } from '@/stores/system/tenant';
import { DEFAULT_AVATAR } from '@/constants/auth';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted } from 'vue';
import NotificationBell from '@/components/notification/NotificationBell.vue';

defineProps<{ isCollapsed: boolean }>();
defineEmits(['toggle-sidebar']);

const router = useRouter();
const authStore = useAuthStore();
const tenantStore = useTenantStore();
const searchKeyword = ref('');

onMounted(() => {
  tenantStore.fetchCurrent();
  tenantStore.fetchAvailable();
});

const handleTenantSwitch = async (tenantId: number) => {
  if (tenantStore.currentTenant?.id === tenantId) return;
  const target = displayTenants.value.find(t => t.id === tenantId);
  const targetName = target?.name || '目标学校';
  try {
    await ElMessageBox.confirm(
      `确定切入【${targetName}】学校租户上下文吗？切入后将以管理员视角刷新页面并加载该学校专属教学与治理空间。`,
      '切入学校租户确认',
      {
        confirmButtonText: '确认切入',
        cancelButtonText: '取消',
        type: 'info'
      }
    );
    await tenantStore.switchTenant(tenantId);
  } catch (err) {
    // 用户取消切入
  }
};

const handleDropdownVisible = (visible: boolean) => {
  if (visible) {
    tenantStore.fetchAvailable();
  }
};

const displayTenants = computed(() => {
  const list = [...tenantStore.availableTenants];
  // 如果当前已激活租户不在列表中，置入当前租户作为第一项确保状态自洽
  if (tenantStore.currentTenant && !list.some(t => t.id === tenantStore.currentTenant?.id)) {
    list.unshift({
      id: tenantStore.currentTenant.id,
      code: tenantStore.currentTenant.code,
      tenantCode: tenantStore.currentTenant.code,
      name: tenantStore.currentTenant.name,
      campusCount: tenantStore.currentTenant.campusCount || 1,
      status: tenantStore.currentTenant.status || 1,
      planCode: tenantStore.currentTenant.planCode || 'PRO',
      planName: tenantStore.currentTenant.planName || '专业版'
    } as any);
  }
  return list;
});

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

    .tenant-switcher {
      .tenant-switch-btn {
        border-radius: 16px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        font-weight: 500;
        color: #334155;
        padding: 4px 12px;
        display: flex;
        align-items: center;
        gap: 6px;

        .tenant-icon {
          color: #2563EB;
        }

        .tenant-text {
          max-width: 140px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .campus-tag {
          font-size: 11px;
          height: 18px;
          padding: 0 5px;
          border-radius: 10px;
        }

        &:hover {
          background: #EFF6FF;
          border-color: #BFDBFE;
          color: #1D4ED8;
        }
      }
    }

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

.tenant-dropdown-menu {
  display: flex !important;
  flex-direction: column !important;
  min-width: 260px;
  padding: 6px 0 !important;

  .el-dropdown-menu__item {
    display: flex !important;
    padding: 8px 14px !important;
    width: 100%;
    box-sizing: border-box;

    .tenant-menu-item {
      display: flex;
      flex-direction: column;
      gap: 2px;
      width: 100%;

      .tenant-item-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 500;
        color: #1E293B;
      }

      .tenant-item-meta {
        display: flex;
        gap: 12px;
        font-size: 11px;
        color: #64748B;
      }
    }

    &:hover:not(.is-disabled) {
      background-color: #EFF6FF;
    }
  }
}
</style>
