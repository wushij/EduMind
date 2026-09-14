<template>
  <div class="page-container gb-fade-in">
    <!-- 顶部工具栏：标题、架构状态标签与操作按钮 (1:1 像素级复刻 Code Compass Filter Card) -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <span class="filter-title">系统菜单与路由权限树</span>
          <span class="filter-tag">已同步最新 9 大核心业务模块架构（含 AI 智算中心）</span>
          <span class="filter-count">共 {{ moduleCount }} 个顶层模块 · {{ totalNodeCount }} 个节点</span>
        </div>
        <div class="filter-actions">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索菜单名称 / 路径 / 权限标识"
            style="width: 250px"
            :prefix-icon="Search"
            class="search-input"
          />
          <el-button @click="toggleExpandAll" class="expand-btn" round>
            <el-icon><Operation /></el-icon>
            {{ expandAll ? '折叠全部' : '展开全部' }}
          </el-button>
          <el-button type="primary" round :loading="loading" @click="fetchData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 菜单树形数据表格 (1:1 复刻 Code Compass menu-tree-table 规范) -->
    <el-card shadow="never" class="table-card">
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
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
import { getPermissions } from '@/api/system/permission';
import { USE_MOCK } from '@/config/mock';
import type { PermissionVO } from '@/types/system/rbac';
import { buildSidebarMenuTree, type SysMenuNode } from '@/constants/permission';

const loading = ref(false);
const rawPermissions = ref<PermissionVO[]>([]);
const keyword = ref('');
const expandAll = ref(false);
const tableKey = ref(0);
const tableRef = ref();

function getIconComponent(iconName?: string) {
  const map: Record<string, any> = {
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
    Operation,
    Clock,
    Monitor,
    Promotion,
    Plus,
    Delete,
    ChatDotRound,
    Memo
  };
  return (iconName && map[iconName]) || Document;
}

const treeTableData = computed(() => {
  return buildSidebarMenuTree(rawPermissions.value, keyword.value);
});

const moduleCount = computed(() => {
  return treeTableData.value.filter((m) => m.type === 1).length;
});

function countAllNodes(list: SysMenuNode[]): number {
  let count = 0;
  for (const item of list) {
    count++;
    if (item.children && item.children.length > 0) {
      count += countAllNodes(item.children);
    }
  }
  return count;
}
const totalNodeCount = computed(() => countAllNodes(treeTableData.value));

function toggleExpandAll() {
  expandAll.value = !expandAll.value;
  tableKey.value++;
}

function toggleRow(row: SysMenuNode) {
  if (row.children && row.children.length > 0) {
    tableRef.value?.toggleRowExpansion(row);
  }
}

function onRowClick(row: SysMenuNode, column: { property?: string }) {
  if (column?.property === 'name') {
    toggleRow(row);
  }
}

async function fetchData() {
  loading.value = true;
  try {
    const res = await getPermissions();
    let list = res.data || [];
    if (list.length === 0) {
      list = getDefaultPermissionMock();
    } else {
      // 补全系统中定义的标准权限，确保 AI 智算中心与系统管理所有模块完整展示
      const existingCodes = new Set(list.map((p) => p.permissionCode));
      const defaultList = getDefaultPermissionMock();
      for (const item of defaultList) {
        if (!existingCodes.has(item.permissionCode)) {
          list.push(item);
        }
      }
    }
    rawPermissions.value = list;
  } catch (err: any) {
    rawPermissions.value = getDefaultPermissionMock();
  } finally {
    loading.value = false;
  }
}

function getDefaultPermissionMock(): PermissionVO[] {
  return [
    { id: 1, permissionCode: 'system:user:view', permissionName: '用户档案查看' },
    { id: 2, permissionCode: 'system:user:edit', permissionName: '用户档案编辑' },
    { id: 3, permissionCode: 'system:role:view', permissionName: '角色权限查看' },
    { id: 4, permissionCode: 'system:role:edit', permissionName: '角色权限配置' },
    { id: 32, permissionCode: 'system:menu:view', permissionName: '菜单管理与路由' },
    { id: 33, permissionCode: 'system:menu:edit', permissionName: '菜单维护编辑' },
    { id: 34, permissionCode: 'system:permission:view', permissionName: '权限分配矩阵查看' },
    { id: 25, permissionCode: 'system:tenant:view', permissionName: '租户校区管理' },
    { id: 35, permissionCode: 'system:tenant:edit', permissionName: '租户校区编辑' },
    { id: 26, permissionCode: 'system:org:view', permissionName: '组织架构管理' },
    { id: 36, permissionCode: 'system:audit:view', permissionName: '系统调用审计日志' },
    { id: 46, permissionCode: 'system:operlog:query', permissionName: '操作日志查询浏览' },
    { id: 47, permissionCode: 'system:operlog:delete', permissionName: '操作日志删除记录' },
    { id: 48, permissionCode: 'system:operlog:clear', permissionName: '操作日志一键清空' },
    { id: 49, permissionCode: 'system:operlog:export', permissionName: '操作日志导出报表' },
    { id: 37, permissionCode: 'system:config:view', permissionName: '系统全局配置查看' },
    { id: 38, permissionCode: 'system:config:edit', permissionName: '系统全局配置编辑' },

    // 课程中心
    { id: 5, permissionCode: 'course:view', permissionName: '我的课程' },
    { id: 6, permissionCode: 'course:create', permissionName: '创建课程' },
    { id: 7, permissionCode: 'course:edit', permissionName: '课程编辑' },

    // AI 教学
    { id: 17, permissionCode: 'ai:chat', permissionName: 'AI 助手交互' },
    { id: 18, permissionCode: 'ai:grading', permissionName: 'AI 批改评测' },
    { id: 19, permissionCode: 'ai:question', permissionName: 'AI 出题生成' },
    { id: 20, permissionCode: 'ai:exam', permissionName: 'AI 组卷编排' },
    { id: 31, permissionCode: 'ai:tool:use', permissionName: 'Agent 工具调度' },

    // AI 智算中心
    { id: 27, permissionCode: 'system:model:view', permissionName: 'AI 模型接入调度' },
    { id: 41, permissionCode: 'system:model:edit', permissionName: 'AI 模型参数配置' },
    { id: 28, permissionCode: 'system:prompt:view', permissionName: 'Prompt 模板库' },
    { id: 42, permissionCode: 'system:prompt:edit', permissionName: 'Prompt 模板编辑' },
    { id: 43, permissionCode: 'system:tool:view', permissionName: 'AI 教学工具管理' },
    { id: 44, permissionCode: 'system:gateway:view', permissionName: 'AI 网关监控调度' },
    { id: 29, permissionCode: 'system:quota:view', permissionName: '算力与配额管控' },
    { id: 45, permissionCode: 'system:quota:edit', permissionName: '配额限流与充值' },

    // 知识库
    { id: 15, permissionCode: 'knowledge:view', permissionName: '知识库列表' },
    { id: 16, permissionCode: 'knowledge:edit', permissionName: '创建维护知识库' },
    { id: 30, permissionCode: 'knowledge:rag:debug', permissionName: 'RAG 检索调试' },

    // 题库与作业
    { id: 8, permissionCode: 'question:view', permissionName: '题目浏览查看' },
    { id: 9, permissionCode: 'question:edit', permissionName: '题目创建编辑' },
    { id: 10, permissionCode: 'exam:view', permissionName: '试卷查看浏览' },
    { id: 11, permissionCode: 'exam:edit', permissionName: '试卷编排修改' },
    { id: 12, permissionCode: 'assignment:view', permissionName: '作业任务查看' },
    { id: 13, permissionCode: 'assignment:create', permissionName: '作业发布创建' },
    { id: 14, permissionCode: 'assignment:grade', permissionName: '作业批改打分' },

    // 教学分析
    { id: 21, permissionCode: 'analytics:view', permissionName: '学情分析看板' },

    // 教学资源
    { id: 22, permissionCode: 'resource:view', permissionName: '课件资源查看' },
    { id: 23, permissionCode: 'resource:upload', permissionName: '课件资源上传' },

    // 消息通知
    { id: 24, permissionCode: 'notice:view', permissionName: '系统通知公告' },
    { id: 39, permissionCode: 'notice:broadcast:view', permissionName: '广播推送查看' },
    { id: 40, permissionCode: 'notice:broadcast:send', permissionName: '广播推送发送' }
  ];
}

onMounted(fetchData);
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
}

.filter-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.filter-tag {
  font-size: 11.5px;
  font-weight: 600;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 8px;
  border-radius: 6px;
}

.filter-count {
  font-size: 12px;
  color: #64748b;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
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
