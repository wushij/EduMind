<template>
  <aside class="sidebar" :class="{ 'is-collapse': isCollapsed }">
    <!-- Logo 品牌区域 (docs/logo.png + PRD 品牌命名) -->
    <div class="logo" @click="router.push('/dashboard')" title="EduMind｜AI 智能教学赋能平台">
      <div class="logo-icon">
        <img class="logo-image" src="@/assets/images/logo.png" alt="EduMind" />
      </div>
      <div v-show="!isCollapsed" class="logo-text">
        <span class="logo-title">EduMind</span>
        <span class="logo-subtitle">智教云 · EduMind</span>
      </div>
    </div>

    <!-- 菜单滚动容器 (原生轻量滚动 + 零抖动 + 定制细长滚动条) -->
    <div class="menu-wrapper">
      <el-menu
        ref="menuRef"
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        popper-class="sidebar-menu-popper"
        router
        @open="handleSubMenuOpen"
        @close="handleSubMenuClose"
      >
        <!-- 一级导航：首页 (原型图 §五，不展开待办/概览/最近使用) -->
        <el-menu-item index="/dashboard" class="menu-item">
          <el-icon><HomeFilled /></el-icon>
          <template #title><span>首页</span></template>
        </el-menu-item>

        <!-- 核心业务模块 (对齐 PRD §4 / 原型图 §五、§二十六) -->
        <template v-for="mod in displayModules" :key="mod.key">
          <!-- 包含子菜单的分类项 -->
          <el-sub-menu
            v-if="mod.children && mod.children.length > 0"
            :index="mod.key"
            class="menu-group"
          >
            <template #title>
              <el-icon :class="{ 'is-spin-once': mod.key === 'system' && menuIconSpinKey === 'system' }">
                <component :is="mod.icon" />
              </el-icon>
              <span>{{ mod.name }}</span>
            </template>

            <el-menu-item
              v-for="child in mod.children"
              :key="child.path"
              :index="child.path"
              :class="'menu-item'"
            >
              <el-icon v-if="child.icon">
                <component :is="child.icon" />
              </el-icon>
              <template #title>
                <div class="menu-item-inner">
                  <span>{{ child.name }}</span>
                  <span v-if="child.badge" class="v05-badge">{{ child.badge }}</span>
                </div>
              </template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 无子菜单的一级项 -->
          <el-menu-item v-else :index="mod.path || ''" class="menu-item">
            <el-icon>
              <component :is="mod.icon" />
            </el-icon>
            <template #title><span>{{ mod.name }}</span></template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  HomeFilled,
  Compass,
  Reading,
  MagicStick,
  FolderOpened,
  Document,
  TrendCharts,
  DataAnalysis,
  Setting,
  User,
  Collection,
  EditPen,
  ChatDotRound,
  Tickets,
  Notebook,
  Files,
  Cpu,
  Histogram,
  PieChart,
  Warning,
  Coin,
  Lock,
  Clock,
  Operation,
  Grid,
  School,
  Star,
  CollectionTag,
  Promotion,
  DocumentAdd,
  Service,
  CircleCheck,
  DocumentCopy,
  Search,
  Connection,
  Folder,
  FolderAdd,
  SetUp,
  Memo,
  DocumentChecked,
  Finished,
  DataBoard,
  List,
  MapLocation,
  DataLine,
  Key,
  ChatLineSquare,
  QuestionFilled,
  Money,
  Monitor,
  Avatar,
  CreditCard,
  Tools
} from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { useAppStore } from '@/stores/app/app';

const props = withDefaults(
  defineProps<{
    isCollapsed?: boolean;
  }>(),
  {
    isCollapsed: false
  }
);

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const appStore = useAppStore();
const menuRef = ref();

const LAST_KNOWLEDGE_ID_KEY = 'edumind_last_knowledge_id';

interface SubMenuItem {
  path: string;
  name: string;
  icon?: any;
  roles?: string[];
  permissions?: string[];
  badge?: string;
}

interface NavModule {
  key: string;
  name: string;
  icon: any;
  roles?: string[];
  permissions?: string[];
  path?: string;
  children?: SubMenuItem[];
}

/** 解析当前上下文知识库 ID，避免侧边栏硬编码 /knowledge/1/... */
function getKnowledgeBaseId(): string {
  const routeMatch = route.path.match(/^\/knowledge\/([^/]+)/);
  const routeId = routeMatch?.[1];
  if (routeId && routeId !== 'create') {
    return routeId;
  }
  return localStorage.getItem(LAST_KNOWLEDGE_ID_KEY) || '1';
}

function knowledgePath(suffix: string): string {
  return `/knowledge/${getKnowledgeBaseId()}/${suffix}`;
}

/**
 * 导航模块定义 (对齐 PRD §4 信息架构 + 原型图左侧导航)
 */
function buildModuleDefinitions(): NavModule[] {
  const kbId = getKnowledgeBaseId();

  return [
    // AI 广场
    {
      key: 'ai-market',
      name: 'AI 广场',
      icon: Compass,
      children: [
        { path: '/ai/marketplace', name: '全部工具', icon: Grid },
        { path: '/ai/marketplace/teacher', name: '教师工具', icon: School, roles: ['ADMIN', 'TEACHER'] },
        { path: '/ai/marketplace/student', name: '学生工具', icon: Reading, roles: ['ADMIN', 'STUDENT'] },
        { path: '/ai/marketplace/recommended', name: '推荐工具', icon: Star },
        { path: '/ai/marketplace/my-tools', name: '我的工具', icon: CollectionTag }
      ]
    },

    // 课程中心
    {
      key: 'course',
      name: '课程中心',
      icon: Reading,
      children: [
        { path: '/course', name: '我的课程', icon: Collection },
        { path: '/course/create', name: '创建课程', icon: DocumentAdd, roles: ['ADMIN', 'TEACHER'], permissions: ['course:create'] },
        { path: '/course/ai-assistant', name: '课程 AI', icon: ChatDotRound, roles: ['ADMIN', 'STUDENT'] }
      ]
    },

    // AI 教学
    {
      key: 'ai-teaching',
      name: 'AI 教学',
      icon: MagicStick,
      roles: ['ADMIN', 'TEACHER'],
      permissions: ['ai:question', 'ai:exam', 'ai:grading', 'ai:chat'],
      children: [
        { path: '/ai/assistant/chat', name: 'AI 助手', icon: Service, permissions: ['ai:chat'] },
        { path: '/ai/question/generate', name: 'AI 出题', icon: EditPen, permissions: ['ai:question'] },
        { path: '/ai/exam/generate', name: 'AI 组卷', icon: Tickets, permissions: ['ai:exam'] },
        { path: '/ai/grading', name: 'AI 批改', icon: CircleCheck, permissions: ['ai:grading'] },
        { path: '/ai/lesson', name: 'AI 教案', icon: Notebook, badge: 'V0.5' },
        { path: '/ai/summary', name: 'AI 总结', icon: DocumentCopy, badge: 'V0.5' },
        { path: '/ai/recommendation', name: 'AI 推荐', icon: Promotion, badge: 'V0.5' }
      ]
    },

    // 知识库
    {
      key: 'knowledge',
      name: '知识库',
      icon: FolderOpened,
      roles: ['ADMIN', 'TEACHER'],
      permissions: ['knowledge:view'],
      children: [
        { path: '/knowledge', name: '知识库', icon: Folder, permissions: ['knowledge:view'] },
        { path: '/knowledge/create', name: '创建知识库', icon: FolderAdd },
        { path: `/knowledge/${kbId}/documents`, name: '文档管理', icon: Files },
        { path: `/knowledge/${kbId}/parse`, name: '文档解析', icon: SetUp, badge: 'V0.5' },
        { path: `/knowledge/${kbId}/rag-debug`, name: 'RAG 检索', icon: Search, badge: 'V0.5' },
        { path: `/knowledge/${kbId}/graph`, name: '知识图谱', icon: Connection, badge: 'V0.5' }
      ]
    },

    // 题库与作业
    {
      key: 'question',
      name: '题库与作业',
      icon: Document,
      roles: ['ADMIN', 'TEACHER'],
      permissions: ['question:view', 'exam:view', 'assignment:view'],
      children: [
        { path: '/question/list', name: '题目', icon: Memo, permissions: ['question:view'] },
        { path: '/question/banks', name: '题库', icon: Collection },
        { path: '/question/exams', name: '试卷', icon: DocumentChecked },
        { path: '/question/assignments', name: '作业', icon: Notebook },
        { path: '/question/submissions', name: '提交记录', icon: Finished }
      ]
    },

    // 学习中心
    {
      key: 'learning',
      name: '学习中心',
      icon: TrendCharts,
      roles: ['ADMIN', 'STUDENT'],
      children: [
        { path: '/learning', name: '学习总览', icon: DataBoard },
        { path: '/learning/tasks', name: '学习任务', icon: List },
        { path: '/learning/practice', name: 'AI 练习', icon: MagicStick },
        { path: '/learning/wrong-questions', name: '错题本', icon: Warning },
        { path: '/learning/report', name: '学习报告', icon: DataLine },
        { path: '/learning/path', name: '学习路径', icon: MapLocation, badge: 'V0.5' }
      ]
    },

    // 教学分析
    {
      key: 'analytics',
      name: '教学分析',
      icon: DataAnalysis,
      roles: ['ADMIN', 'TEACHER'],
      children: [
        { path: '/analytics', name: '课程概览', icon: Histogram },
        { path: '/analytics/learning', name: '学情分析', icon: TrendCharts },
        { path: '/analytics/mastery', name: '知识点掌握', icon: PieChart },
        { path: '/analytics/wrong-questions', name: '错题分析', icon: QuestionFilled },
        { path: '/analytics/ai-usage', name: 'AI 使用分析', icon: Coin },
        { path: '/analytics/teaching-report', name: '教学报告', icon: DocumentCopy, badge: 'V0.5' }
      ]
    },

    // 系统管理
    {
      key: 'system',
      name: '系统管理',
      icon: Setting,
      roles: ['ADMIN'],
      permissions: ['system:user:view', 'system:role:view'],
      children: [
        { path: '/system/users', name: '用户管理', icon: User, permissions: ['system:user:view'] },
        { path: '/system/roles', name: '角色权限', icon: Lock, permissions: ['system:role:view'] },
        { path: '/system/permissions', name: '权限分配', icon: Key },
        { path: '/system/tools', name: 'AI 工具', icon: Operation },
        { path: '/system/models', name: 'AI 模型', icon: Cpu },
        { path: '/system/prompts', name: 'Prompt', icon: ChatLineSquare },
        { path: '/system/quotas', name: 'AI 配额', icon: Money, badge: 'V0.5' },
        { path: '/system/audit', name: '审计日志', icon: Clock },
        { path: '/system/config', name: '系统配置', icon: Monitor }
      ]
    },

    // 个人中心
    {
      key: 'profile',
      name: '个人中心',
      icon: User,
      children: [
        { path: '/profile', name: '个人资料', icon: Avatar },
        { path: '/profile/security', name: '账号安全', icon: Lock },
        { path: '/profile/ai-usage', name: 'AI 消耗明细', icon: CreditCard },
        { path: '/profile/preferences', name: '偏好设置', icon: Tools }
      ]
    }
  ];
}

/**
 * 依据当前登录角色进行多级菜单过滤
 */
const displayModules = computed(() => {
  return buildModuleDefinitions()
    .filter((mod) => {
      if (mod.roles && mod.roles.length > 0 && !authStore.hasAnyRole(mod.roles)) {
        return false;
      }
      if (mod.permissions && mod.permissions.length > 0 && !authStore.hasAnyPermission(mod.permissions)) {
        return false;
      }
      return true;
    })
    .map((mod) => {
      if (!mod.children) return mod;
      const visibleChildren = mod.children.filter((child) => {
        if (child.roles && child.roles.length > 0 && !authStore.hasAnyRole(child.roles)) {
          return false;
        }
        if (child.permissions && child.permissions.length > 0 && !authStore.hasAnyPermission(child.permissions)) {
          return false;
        }
        return true;
      });
      return {
        ...mod,
        children: visibleChildren
      };
    })
    .filter((mod) => !mod.children || mod.children.length > 0);
});

/**
 * 计算激活菜单项（支持多级动态路径映射）
 */
const activeMenu = computed(() => {
  const { meta, path } = route;
  if (meta?.activeMenu) {
    return meta.activeMenu as string;
  }
  if (path.startsWith('/dashboard')) {
    return '/dashboard';
  }
  if (path.match(/^\/course\/[^/]+\/ai/)) {
    return '/course/ai-assistant';
  }
  if (path.startsWith('/knowledge/') && path.includes('/documents')) {
    return knowledgePath('documents');
  }
  if (path.startsWith('/knowledge/') && path.includes('/parse')) {
    return knowledgePath('parse');
  }
  if (path.startsWith('/knowledge/') && path.includes('/rag-debug')) {
    return knowledgePath('rag-debug');
  }
  if (path.startsWith('/knowledge/') && path.includes('/graph')) {
    return knowledgePath('graph');
  }
  if (path.startsWith('/ai/assistant')) {
    return '/ai/assistant/chat';
  }
  return path;
});

// 系统菜单齿轮微旋转动效 (对标 goblog-web 交互动效)
const menuIconSpinKey = ref('');
let menuIconSpinTimer: number | null = null;

function triggerSystemMenuIconSpin() {
  if (menuIconSpinTimer) {
    window.clearTimeout(menuIconSpinTimer);
    menuIconSpinTimer = null;
  }
  menuIconSpinKey.value = '';
  requestAnimationFrame(() => {
    menuIconSpinKey.value = 'system';
    menuIconSpinTimer = window.setTimeout(() => {
      if (menuIconSpinKey.value === 'system') {
        menuIconSpinKey.value = '';
      }
      menuIconSpinTimer = null;
    }, 520);
  });
}

function handleSubMenuOpen(index: string) {
  if (index === 'system') triggerSystemMenuIconSpin();
}

function handleSubMenuClose(index: string) {
  if (index === 'system') triggerSystemMenuIconSpin();
}

/**
 * 自动展开当前激活页面所在的模块（对标 goblog-web 顺滑联动体验）
 */
function openActiveSubMenu() {
  if (props.isCollapsed) return;
  for (const mod of displayModules.value) {
    if (!mod.children) continue;
    const match = mod.children.some(
      (c) => c.path === activeMenu.value || (c.path !== '/' && activeMenu.value.startsWith(c.path + '/'))
    );
    if (match && menuRef.value) {
      menuRef.value.open(mod.key);
      break;
    }
  }
}

watch(
  () => route.path,
  (path) => {
    const match = path.match(/^\/knowledge\/([^/]+)/);
    if (match && match[1] !== 'create') {
      localStorage.setItem(LAST_KNOWLEDGE_ID_KEY, match[1]);
    }
  },
  { immediate: true }
);

watch(
  () => activeMenu.value,
  () => {
    nextTick(() => {
      openActiveSubMenu();
    });
  },
  { immediate: true }
);

watch(
  () => props.isCollapsed,
  (collapsed) => {
    appStore.sidebarCollapsed = collapsed;
    if (!collapsed) {
      nextTick(() => {
        openActiveSubMenu();
      });
    }
  }
);

onUnmounted(() => {
  if (menuIconSpinTimer) {
    window.clearTimeout(menuIconSpinTimer);
    menuIconSpinTimer = null;
  }
});

onMounted(() => {
  nextTick(() => {
    openActiveSubMenu();
  });
});

</script>

<style scoped lang="scss">
/* 侧边栏主容器 (对标 goblog-web 架构体系，保留 EduMind 原有科技蓝质感与背景图) */
.sidebar {
  width: 256px;
  height: 100vh;
  background-color: #0A1B39;
  background-image: url('@/assets/images/侧边栏背景图.png');
  background-size: cover;
  background-position: bottom left;
  background-repeat: no-repeat;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 2px 0 14px rgba(10, 27, 57, 0.25);
  display: flex;
  flex-direction: column;
  z-index: 100;
  flex-shrink: 0;
  position: relative;
  user-select: none;

  &.is-collapse {
    width: 64px;
  }
}

/* 顶部品牌 Logo */
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
  transition: all 0.3s ease;
  background: rgba(10, 27, 57, 0.7);
  backdrop-filter: blur(8px);
}

.sidebar.is-collapse .logo {
  justify-content: center;
  padding: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.4s ease;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.logo:hover .logo-icon {
  transform: scale(1.05);
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  white-space: nowrap;
}

.logo-title {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.3px;
  color: #FFFFFF;
}

.logo-subtitle {
  color: #94A3B8;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.1px;
  line-height: 1.3;
}

/* 菜单滚动容器 (stable 滚动槽避免展开子菜单时滚动条挤占宽度导致抖动) */
.menu-wrapper {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
  scrollbar-gutter: stable;
}

.menu-wrapper::-webkit-scrollbar {
  width: 4px;
}

.menu-wrapper::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.25);
  border-radius: 4px;
}

.menu-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.45);
}

.menu-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

@supports not (scrollbar-gutter: stable) {
  .menu-wrapper {
    overflow-y: scroll;
  }
}

/* Element Plus 侧栏原生菜单样式深度优化 (无边框 + 极速零抖动) */
.sidebar-menu {
  border-right: none !important;
  background: transparent !important;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

:deep(.el-menu) {
  border-right: none !important;
  background: transparent !important;
}

/* 子菜单展开后的嵌入式圆角背景卡片（半透明，不遮挡底部背景图） */
:deep(.el-menu--inline) {
  background: rgba(0, 0, 0, 0.22) !important;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  margin: 4px 0 8px;
  padding: 4px 0;
  overflow: hidden;
  backdrop-filter: blur(2px);
}

/* 核心优化：彻底关闭 Element Plus 子菜单高度展开过渡计算，解决折叠子项「跳动/抖动」问题 */
:deep(.el-sub-menu .el-collapse-transition),
:deep(.el-sub-menu .horizontal-collapse-transition),
:deep(.el-sub-menu .vertical-collapse-transition) {
  transition: none !important;
}

/* 菜单项基础圆角与平滑过渡 */
:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  color: #D4DEEA;
  border-radius: 8px;
  margin: 2px 0;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  font-weight: 500;
  user-select: none;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}

:deep(.el-sub-menu__title .el-icon),
:deep(.el-menu-item .el-icon) {
  color: #A8B8CC;
  transition: color 0.2s ease;
}

:deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
}

:deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  box-sizing: border-box;
}

:deep(.menu-item-inner) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 6px;
}

:deep(.v05-badge) {
  font-size: 10px;
  line-height: 1.2;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.35);
  font-weight: 600;
  letter-spacing: 0.3px;
}

/* 子菜单项内缩边距，形成舒适的卡片内部内嵌层级 */
:deep(.el-menu--inline .el-menu-item) {
  margin-left: 8px !important;
  margin-right: 8px !important;
  width: calc(100% - 16px);
}

/* 悬停微动画 (柔和微光) */
:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #FFFFFF;
}

:deep(.el-sub-menu__title:hover .el-icon),
:deep(.el-menu-item:hover .el-icon) {
  color: #E2E8F0;
}

/* 子项激活态：白字 + 左侧高亮条，避免青蓝字叠蓝底发糊 */
:deep(.el-menu-item.is-active) {
  color: #FFFFFF !important;
  background: rgba(22, 119, 255, 0.38) !important;
  font-weight: 600;
  box-shadow: inset 3px 0 0 #38BDF8;
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #FFFFFF !important;
}

/* 分组打开时的标题态 */
:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #F1F5F9;
  background: rgba(255, 255, 255, 0.08);
  font-weight: 600;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-icon) {
  color: #E2E8F0;
}

/* 折叠模式对齐 */
:deep(.el-menu--collapse) {
  width: 100%;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  margin-left: 0 !important;
  justify-content: center;
}

/* 系统菜单齿轮微旋转 */
:deep(.sidebar-menu .el-icon.is-spin-once) {
  animation: sidebar-menu-icon-spin 0.52s ease;
}

@keyframes sidebar-menu-icon-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

</style>

<style>
/* 折叠弹出层：去掉 Element Plus light popper 默认白边/白底 */
.el-popper.is-light.sidebar-menu-popper,
.el-popper.is-pure.is-light.sidebar-menu-popper {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
}

.el-popper.sidebar-menu-popper .el-menu--popup-container {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
}

/* 折叠模式下 Element Plus 原生悬浮菜单全局微调 (暗色科技质感) */
.el-popper.sidebar-menu-popper .el-menu--popup {
  min-width: 180px !important;
  padding: 6px !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.45), 0 8px 10px -6px rgba(0, 0, 0, 0.3) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: #0B1936 !important;
}

.el-popper.sidebar-menu-popper .el-menu--popup .el-menu-item {
  height: 38px !important;
  line-height: 38px !important;
  border-radius: 8px !important;
  margin: 2px 0 !important;
  padding: 0 14px !important;
  font-size: 13.5px !important;
  color: #D4DEEA !important;
}

.el-popper.sidebar-menu-popper .el-menu--popup .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #FFFFFF !important;
}

.el-popper.sidebar-menu-popper .el-menu--popup .el-menu-item.is-active {
  color: #FFFFFF !important;
  background: rgba(22, 119, 255, 0.38) !important;
  font-weight: 600 !important;
  box-shadow: inset 3px 0 0 #38BDF8;
}
</style>
