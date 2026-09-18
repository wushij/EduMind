<template>
  <div class="system-page-shell permission-tree-page gb-fade-in">
    <ProfilePageHero
      title="系统菜单与路由权限树"
      subtitle="已同步最新 9 大核心业务模块架构（含学习中心 / AI 运维）"
    >
      <template #footer>
        <p class="hero-meta-line">
          共 <strong>{{ moduleCount }}</strong> 个顶层模块 · <strong>{{ totalNodeCount }}</strong> 个节点
        </p>
      </template>
    </ProfilePageHero>

    <div class="system-filter-card filter-toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索菜单名称 / 路径 / 权限标识"
        class="search-input"
        :prefix-icon="Search"
      />
      <el-button round class="action-btn" @click="toggleExpandAll">
        <el-icon><Operation /></el-icon>
        {{ expandAll ? '折叠全部' : '展开全部' }}
      </el-button>
      <el-button round :icon="Refresh" class="btn-refresh" :loading="loading" @click="fetchData">
        刷新
      </el-button>
    </div>

    <div class="system-table-card table-card">
      <el-table
        :key="tableKey"
        ref="tableRef"
        :data="treeTableData"
        row-key="rowKey"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
        v-loading="loading"
        style="width: 100%"
        class="gb-modern-table menu-tree-table"
        @row-click="onRowClick"
      >
        <!-- 菜单名称列 (严格对齐：占位符与展开箭头等宽，图标与文本同列垂直对齐，彻底消除纵向文字换行) -->
        <el-table-column prop="name" label="菜单名称" min-width="250" class-name="menu-name-col">
          <template #default="{ row }">
            <div
              class="menu-name-cell"
              :class="{ 'is-parent': row.children && row.children.length > 0 }"
              @click.stop="toggleRow(row)"
            >
              <el-icon
                v-if="row.icon"
                size="17"
                class="name-icon"
                :color="row.type === 1 ? '#1677ff' : row.type === 2 ? '#10b981' : '#f59e0b'"
              >
                <component :is="getIconComponent(row.icon)" />
              </el-icon>
              <el-icon v-else size="17" class="name-icon" color="#94a3b8">
                <component :is="row.type === 3 ? Pointer : Document" />
              </el-icon>
              <span
                class="menu-name-text"
                :class="{
                  'font-bold': row.type === 1,
                  'font-medium': row.type === 2,
                  'text-btn': row.type === 3
                }"
              >
                {{ row.name }}
              </span>
              <span v-if="row.children && row.children.length > 0" class="child-count-pill">
                {{ row.children.length }} 项
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 图标展示 -->
        <el-table-column prop="icon" label="图标" width="130" align="center">
          <template #default="{ row }">
            <div v-if="row.icon" class="icon-preview-box">
              <el-icon size="17" color="#6366f1"><component :is="getIconComponent(row.icon)" /></el-icon>
              <span class="icon-code-text">{{ row.icon }}</span>
            </div>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 类型标识 -->
        <el-table-column prop="type" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.type === 1 ? 'primary' : row.type === 2 ? 'success' : 'warning'"
              :effect="row.type === 1 ? 'dark' : 'light'"
              size="small"
              round
            >
              {{ row.type === 1 ? '目录' : row.type === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 路由路径 -->
        <el-table-column prop="path" label="路由路径" min-width="210">
          <template #default="{ row }">
            <span v-if="row.path" class="code-pill">{{ row.path }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 权限标识 (Code Compass 风格 Danger Tag) -->
        <el-table-column prop="permission" label="权限标识" min-width="170" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.permission" type="danger" size="small" round font-mono>
              {{ row.permission }}
            </el-tag>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 排序 -->
        <el-table-column prop="sort" label="排序" width="80" align="center">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 36px 0">
            <el-empty description="暂无匹配的菜单权限数据" />
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Operation,
  Refresh,
  Search,
  Reading,
  MagicStick,
  FolderOpened,
  Document,
  DataAnalysis,
  Files,
  Setting,
  Bell,
  Collection,
  DocumentAdd,
  EditPen,
  Service,
  Tickets,
  CircleCheck,
  Cpu,
  Folder,
  FolderAdd,
  DocumentChecked,
  Notebook,
  TrendCharts,
  Upload,
  School,
  Connection,
  User,
  Lock,
  Key,
  ChatLineSquare,
  Money,
  Pointer,
  Clock,
  Monitor,
  Promotion,
  Plus,
  Delete,
  ChatDotRound,
  Memo
} from '@element-plus/icons-vue';
import type { SysMenuNode } from '@/constants/permission';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import { getIconComponent as getRoleIconComponent, usePermissionTree } from '@/composables/system/useRole';

const {
  loading,
  keyword,
  expandAll,
  tableKey,
  tableRef,
  treeTableData,
  moduleCount,
  totalNodeCount,
  toggleExpandAll,
  toggleRow,
  onRowClick,
  fetchData
} = usePermissionTree();

const permissionIconMap: Record<string, unknown> = {
  Pointer,
  Operation,
  Clock,
  Monitor,
  Promotion,
  Plus,
  Delete,
  ChatDotRound,
  Memo
};

function getIconComponent(iconName?: string) {
  return (iconName && permissionIconMap[iconName]) || getRoleIconComponent(iconName);
}
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.permission-tree-page {
  .hero-meta-line {
    margin: 0;
    font-size: 13px;
    color: #64748b;

    strong {
      color: #0f172a;
      font-weight: 700;
    }
  }

  .filter-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;

    .search-input {
      width: 320px;

      :deep(.el-input__wrapper) {
        border-radius: 999px;
      }
    }

    .action-btn {
      border-radius: 999px;
    }
  }

  .table-card {
    overflow: hidden;
    padding: 0;
  }
}

/* 核心：精确消除第一列占位符与箭头宽度差异，文本永不竖向折行 */
.menu-tree-table :deep(.el-table__body td.menu-name-col .cell),
.menu-tree-table :deep(.el-table__header th.menu-name-col .cell) {
  display: flex !important;
  align-items: center !important;
  text-align: left !important;
  justify-content: flex-start !important;
  white-space: nowrap !important;
}

.menu-tree-table :deep(.el-table__expand-icon) {
  width: 20px !important;
  height: 20px !important;
  margin-right: 6px !important;
  cursor: pointer;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-tree-table :deep(.el-table__placeholder) {
  width: 20px !important;
  height: 20px !important;
  margin-right: 6px !important;
  display: inline-block !important;
  flex-shrink: 0;
}

.menu-name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  user-select: none;
  white-space: nowrap;
}

.menu-name-cell.is-parent {
  cursor: pointer;
}

.menu-name-cell.is-parent:hover .menu-name-text {
  color: #6366f1;
}

.name-icon {
  flex-shrink: 0;
}

.menu-name-text {
  font-size: 13.5px;
  white-space: nowrap;
}

.menu-name-text.font-bold {
  font-weight: 700;
  color: #0f172a;
}

.menu-name-text.font-medium {
  font-weight: 600;
  color: #1e293b;
}

.menu-name-text.text-btn {
  font-weight: 400;
  color: #475569;
  font-size: 13px;
}

.child-count-pill {
  font-size: 11px;
  color: #94a3b8;
  background: rgba(15, 23, 42, 0.05);
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 500;
}

.icon-preview-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.icon-code-text {
  font-size: 11.5px;
  color: #64748b;
  font-family: monospace;
}

.code-pill {
  font-family: monospace;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
  color: #334155;
}

.sort-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.05);
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.text-placeholder {
  color: #94a3b8;
}
</style>
