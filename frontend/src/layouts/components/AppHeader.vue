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
        <!-- max-height 由 el-dropdown 透传给内置 el-scrollbar：
             租户/校区数量多时由官方滚动容器承载，避免自建 overflow 造成内容裁切与滚动条压字 -->
        <el-dropdown
          trigger="click"
          placement="bottom-end"
          :max-height="360"
          popper-class="tenant-dropdown-popper"
          @command="handleTenantSwitch"
          @visible-change="handleDropdownVisible"
        >
          <el-button size="small" class="tenant-switch-btn" plain :loading="tenantStore.switching">
            <el-icon class="tenant-icon"><School /></el-icon>
            <span class="tenant-text">{{ tenantStore.activeTenantName }}</span>
            <el-tag size="small" type="primary" effect="plain" class="campus-tag">{{ tenantStore.activeCampusName }}</el-tag>
            <el-icon v-if="!tenantStore.switching" class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu class="tenant-dropdown-menu">
              <template v-if="displayTenants.length">
                <el-dropdown-item disabled class="tenant-menu-header">
                  <span class="menu-group-title">切换学校 / 租户</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-for="tenant in displayTenants"
                  :key="tenant.id"
                  :command="tenant.id"
                  :disabled="tenantStore.currentTenant?.id === tenant.id || tenantStore.switching"
                  :class="{ 'is-current-tenant': tenantStore.currentTenant?.id === tenant.id }"
                >
                  <div class="tenant-menu-item">
                    <div class="tenant-item-title">
                      <el-icon v-if="tenantStore.currentTenant?.id === tenant.id" class="current-check"><Select /></el-icon>
                      <span class="name">{{ tenant.name }}</span>
                      <el-tag v-if="tenantStore.currentTenant?.id === tenant.id" size="small" type="success" effect="light">当前</el-tag>
                    </div>
                    <div class="tenant-item-meta">
                      <span>编码: {{ tenant.tenantCode || tenant.code }}</span>
                      <span v-if="tenant.campusCount">校区: {{ tenant.campusCount }}</span>
                    </div>
                  </div>
                </el-dropdown-item>
              </template>
              <el-dropdown-item v-else disabled>
                <div class="tenant-menu-item">
                  <div class="tenant-item-meta">暂无可切换的租户</div>
                </div>
              </el-dropdown-item>

              <template v-if="currentTenantCampuses.length > 1">
                <el-dropdown-item divided disabled class="tenant-menu-header">
                  <span class="menu-group-title">切换校区</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-for="campus in currentTenantCampuses"
                  :key="`campus-${campus.id}`"
                  :command="`campus:${campus.id}`"
                  :disabled="tenantStore.currentCampusId === campus.id"
                >
                  <div class="tenant-menu-item">
                    <div class="tenant-item-title">
                      <el-icon v-if="tenantStore.currentCampusId === campus.id" class="current-check"><Select /></el-icon>
                      <span class="name">{{ campus.name }}</span>
                      <el-tag v-if="tenantStore.currentCampusId === campus.id" size="small" type="success" effect="light">当前</el-tag>
                    </div>
                  </div>
                </el-dropdown-item>
              </template>
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
import { ref, computed, onMounted, nextTick } from 'vue';
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
  School,
  Select
} from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { useTenantStore } from '@/stores/system/tenant';
import { DEFAULT_AVATAR } from '@/constants/auth';
import { ElMessage, ElMessageBox } from 'element-plus';
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

/** el-dropdown command 复用：`campus:<id>` 表示仅切换校区 */
const CAMPUS_COMMAND_PREFIX = 'campus:';

/**
 * 租户/校区下拉统一入口。
 * el-dropdown 的 command 类型为 string | number | object，这里做一次显式判定，
 * 避免把 `campus:3` 之类的字符串误当作租户 ID 传入切换接口。
 */
const handleTenantSwitch = async (command: string | number) => {
  const raw = String(command);
  if (raw.startsWith(CAMPUS_COMMAND_PREFIX)) {
    const campusId = Number(raw.slice(CAMPUS_COMMAND_PREFIX.length));
    if (Number.isFinite(campusId) && campusId > 0) {
      tenantStore.selectCampus(campusId);
      ElMessage.success(`已切换至【${tenantStore.activeCampusName}】`);
    }
    return;
  }

  const tenantId = Number(raw);
  if (!Number.isFinite(tenantId) || tenantId <= 0) return;
  if (tenantStore.currentTenant?.id === tenantId || tenantStore.switching) return;

  const target = displayTenants.value.find(t => t.id === tenantId);
  const targetName = target?.name || '目标学校';
  try {
    await ElMessageBox.confirm(
      `确定切入【${targetName}】学校租户上下文吗？切入后将以该学校租户的数据与权限重新加载页面。`,
      '切入学校租户确认',
      {
        confirmButtonText: '确认切入',
        cancelButtonText: '取消',
        type: 'info'
      }
    );
    await tenantStore.switchTenant(tenantId);
  } catch {
    // 用户取消切入，无需提示
  }
};

/**
 * el-dropdown 的弹层是 persistent（关闭后不销毁）的，内部 el-scrollbar 会保留上次滚动位置，
 * 重新打开时看起来就像「分组标题被弹层上沿裁掉半截」，这里在每次展开时复位到顶部。
 * 选择器通过 popper-class + 内置滚动容器的 BEM 类名定位（弹层被 teleport 到 body，无法用组件 ref 拿到）。
 */
const resetTenantMenuScroll = () => {
  const wrap = document.querySelector<HTMLElement>('.tenant-dropdown-popper .el-scrollbar__wrap');
  if (wrap) {
    wrap.scrollTop = 0;
  }
};

const handleDropdownVisible = (visible: boolean) => {
  if (!visible) {
    return;
  }
  tenantStore.fetchAvailable();
  nextTick(resetTenantMenuScroll);
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

/** 当前租户下的可选校区（仅多于一个时才在下拉中展示分组） */
const currentTenantCampuses = computed(() => tenantStore.currentTenant?.campuses || []);

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
    // 身份切换会更换会话，租户上下文由 switchRole 内部统一同步（避免带着旧租户的 X-Tenant-Id）
    await authStore.switchRole(role);
    ElMessage.success(`已切换至【${currentRoleName.value}】视角，已按新会话的租户重新加载权限`);
    if (router.currentRoute.value.path !== '/dashboard') {
      await router.push('/dashboard');
    }
    // 不同身份的可见数据与权限差异较大，整体重载可确保不残留上一身份的视图缓存
    window.location.reload();
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

/* 弹层外框：圆角与描边只在外框上定义一次（内外背景同为白色，不会出现灰色夹层边框）。
   注意：此处不能加 overflow: hidden，否则会把 el-popper 自带的箭头裁掉。 */
.tenant-dropdown-popper.el-popper {
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}

.tenant-dropdown-menu {
  display: flex !important;
  flex-direction: column !important;
  min-width: 260px;
  // 滚动统一由 el-dropdown 的 maxHeight（内置 el-scrollbar）承载，
  // 这里不再自建 overflow，否则滚动条会挤占条目宽度、把左右文字裁掉
  padding: 6px 0 !important;
  overflow: visible;

  // flex 纵向布局下禁止条目被压缩，保证滚动而非挤压
  .tenant-menu-header,
  .el-dropdown-menu__item {
    flex-shrink: 0;
  }

  // 分组标题：仅作视觉分段，不可点击
  .tenant-menu-header {
    padding: 6px 14px !important;
    min-height: auto;
    cursor: default;

    .menu-group-title {
      font-size: 11px;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #94a3b8;
      font-weight: 600;
    }
  }

  .el-dropdown-menu__item {
    display: flex !important;
    padding: 8px 14px !important;
    width: 100%;
    box-sizing: border-box;

    // 当前生效租户：左侧金色强调条，便于一眼识别当前上下文
    &.is-current-tenant {
      background: rgba(212, 168, 83, 0.08);
      border-left: 3px solid #d4a853;
    }

    .tenant-menu-item {
      display: flex;
      flex-direction: column;
      gap: 2px;
      width: 100%;

      .tenant-item-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 6px;
        font-weight: 500;
        color: #1E293B;

        .current-check {
          color: #d4a853;
          font-size: 14px;
        }

        .name {
          flex: 1;
        }
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
