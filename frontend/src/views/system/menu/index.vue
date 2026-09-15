<template>
  <div class="page-container gb-fade-in">
    <!-- 顶部工具栏：标题、架构状态标签与操作按钮 (1:1 深度对标 Code Compass filter-card 视觉规范) -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <div class="title-wrap">
            <span class="filter-title">系统菜单与动态路由配置</span>
            <span class="filter-tag">已同步最新 9 大核心业务模块架构（含 AI 智算中心）</span>
          </div>
          <span class="filter-count">
            共 <strong>{{ moduleCount }}</strong> 个顶层模块 · <strong>{{ totalMenuCount }}</strong> 个节点
            <span class="type-stats">({{ typeStats.dirs }} 目录 / {{ typeStats.menus }} 菜单 / {{ typeStats.btns }} 权限)</span>
          </span>
        </div>

        <div class="filter-actions">
          <el-input
            v-model="searchKeyword"
            clearable
            placeholder="搜索菜单名称 / 路由路径 / 权限标识..."
            style="width: 250px"
            :prefix-icon="Search"
            class="search-input"
            @input="handleSearch"
            @clear="handleSearch"
          />

          <el-button round class="action-btn" @click="toggleExpandAll">
            <el-icon><Operation /></el-icon>
            {{ expandAll ? '折叠全部' : '展开全部' }}
          </el-button>

          <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="fetchData">
            刷新
          </el-button>

          <el-popconfirm
            title="确定将菜单配置重置为官方 8 大业务模块预设吗？"
            confirm-button-text="确定重置"
            cancel-button-text="取消"
            icon="Warning"
            icon-color="#f59e0b"
            @confirm="handleResetDefault"
          >
            <template #reference>
              <el-button round class="action-btn text-amber">
                恢复预设
              </el-button>
            </template>
          </el-popconfirm>

          <el-button type="primary" round class="add-btn" @click="handleCreate()">
            <el-icon><Plus /></el-icon> 新增顶级菜单
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 菜单树形数据表格 (1:1 复刻 Code Compass menu-tree-table 像素级对齐规范) -->
    <el-card shadow="never" class="table-card">
      <el-table
        :key="tableKey"
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        row-key="id"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
        class="gb-modern-table menu-tree-table"
        @row-click="onRowClick"
      >
        <!-- 菜单名称列 (严格对齐：占位符与展开箭头等宽，图标与文本同列垂直对齐，彻底消除纵向文字折行) -->
        <el-table-column prop="name" label="菜单名称" min-width="230" class-name="menu-name-col">
          <template #default="{ row }">
            <div
              class="menu-name-cell"
              :class="{ 'is-parent': row.children && row.children.length > 0 }"
              @click.stop="toggleRow(row)"
            >
              <el-icon v-if="row.icon" size="17" class="name-icon" color="#1677ff">
                <component :is="getIconComponent(row.icon)" />
              </el-icon>
              <el-icon v-else size="17" class="name-icon" color="#94a3b8">
                <component :is="row.type === 3 ? Pointer : Document" />
              </el-icon>
              <strong class="menu-name-text">{{ row.name }}</strong>
              <span v-if="row.children && row.children.length > 0" class="child-count-pill">
                {{ row.children.length }} 项
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 图标展示 -->
        <el-table-column prop="icon" label="图标" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.icon" class="icon-preview-box">
              <el-icon size="16" color="#1677ff">
                <component :is="getIconComponent(row.icon)" />
              </el-icon>
              <span class="icon-code-text">{{ row.icon }}</span>
            </div>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 类型标识 -->
        <el-table-column prop="type" label="类型" width="95" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.type === 1 ? 'primary' : row.type === 2 ? 'success' : 'warning'"
              :effect="row.type === 1 ? 'dark' : 'light'"
              size="small"
              round
              class="type-tag"
            >
              {{ row.type === 1 ? '目录' : row.type === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 路由路径 -->
        <el-table-column prop="path" label="路由路径" min-width="180">
          <template #default="{ row }">
            <span v-if="row.path" class="code-pill">{{ row.path }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 前端组件路径 -->
        <el-table-column prop="component" label="前端组件" min-width="210">
          <template #default="{ row }">
            <span v-if="row.component" class="component-text" :title="row.component">{{ row.component }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 权限标识 -->
        <el-table-column prop="permission" label="权限标识" min-width="160" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.permission" type="danger" size="small" round font-mono class="perm-tag">
              {{ row.permission }}
            </el-tag>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 排序 -->
        <el-table-column prop="sort" label="排序" width="75" align="center">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'info'"
              size="small"
              round
            >
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns" @click.stop>
              <el-button type="primary" size="small" link @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button
                v-if="row.type !== 3"
                type="primary"
                size="small"
                link
                @click="handleCreate(row.id, row.type)"
              >
                添加子项
              </el-button>
              <el-button type="danger" size="small" link @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 36px 0">
            <el-empty description="未找到相关菜单配置" />
          </div>
        </template>
      </el-table>
    </el-card>

    <!-- 菜单编辑 / 新增对话框 (深度适配 EduMind dialog-shell 与长圆胶囊体系) -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜单与权限' : '新增菜单项'"
      width="600px"
      append-to-body
      destroy-on-close
      class="menu-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="96px"
        class="menu-form"
      >
        <!-- 1. 上级菜单 -->
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTreeOptions"
            node-key="id"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单（留空则为根目录顶级模块）"
            check-strictly
            default-expand-all
            clearable
            style="width: 100%"
          />
        </el-form-item>

        <!-- 2. 菜单类型 -->
        <el-form-item label="菜单类型" required>
          <el-radio-group v-model="form.type" @change="handleTypeChange">
            <el-radio :value="1">
              <span class="radio-label"><strong>目录</strong> (顶级/二级业务模块)</span>
            </el-radio>
            <el-radio :value="2">
              <span class="radio-label"><strong>菜单</strong> (路由页面与组件)</span>
            </el-radio>
            <el-radio :value="3">
              <span class="radio-label"><strong>按钮</strong> (细粒度操作权限)</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 3. 菜单名称 -->
        <el-form-item label="菜单名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="例如：课程中心、AI出题、新增用户"
            clearable
          />
        </el-form-item>

        <!-- 4. 路由路径 (目录与菜单需要) -->
        <el-form-item v-if="form.type !== 3" label="路由路径" prop="path">
          <el-input
            v-model="form.path"
            placeholder="例如：/course 或 /ai/question/generate"
            clearable
          >
            <template #prepend>path</template>
          </el-input>
        </el-form-item>

        <!-- 5. 组件路径 (仅菜单页面需要) -->
        <el-form-item v-if="form.type === 2" label="前端组件">
          <el-autocomplete
            v-model="form.component"
            :fetch-suggestions="queryComponentSuggestions"
            placeholder="例如：views/course/CourseList.vue"
            clearable
            style="width: 100%"
          >
            <template #prepend>src/</template>
          </el-autocomplete>
        </el-form-item>

        <!-- 6. 图标选择 (目录与菜单需要) -->
        <el-form-item v-if="form.type !== 3" label="图标选择">
          <IconPicker v-model="form.icon" />
        </el-form-item>

        <!-- 7. 权限标识 -->
        <el-form-item label="权限标识">
          <el-input
            v-model="form.permission"
            placeholder="例如：course:create 或 ai:chat"
            clearable
          />
        </el-form-item>

        <!-- 8. 排序号与显示状态 -->
        <div class="form-row-grid">
          <el-form-item label="排序权重">
            <el-input-number
              v-model="form.sort"
              :min="1"
              :max="999"
              controls-position="right"
              style="width: 140px"
            />
          </el-form-item>

          <el-form-item label="状态" label-width="60px">
            <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button round @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" round :loading="saving" @click="saveMenu">
            保存配置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  Operation,
  Plus,
  Search,
  Refresh,
  Reading,
  Collection,
  DocumentAdd,
  DocumentChecked,
  Notebook,
  Tickets,
  EditPen,
  Memo,
  Files,
  Folder,
  FolderAdd,
  FolderOpened,
  School,
  User,
  MagicStick,
  Cpu,
  ChatDotRound,
  ChatLineSquare,
  Promotion,
  Compass,
  Connection,
  Grid,
  CircleCheck,
  Service,
  Star,
  Key,
  DataAnalysis,
  TrendCharts,
  PieChart,
  Histogram,
  DataBoard,
  DataLine,
  MapLocation,
  Coin,
  CreditCard,
  Setting,
  Tools,
  Monitor,
  Clock,
  Lock,
  Bell,
  SetUp,
  Pointer,
  Document
} from '@element-plus/icons-vue';
import IconPicker from '@/components/system/menu/IconPicker.vue';
import {
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
  resetDefaultMenus
} from '@/api/system/menu';
import type { SysMenu, MenuType } from '@/types/system/menu';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<SysMenu[]>([]);
const tableRef = ref();
const searchKeyword = ref('');
const expandAll = ref(false);
const tableKey = ref(0);

const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInstance>();

const form = reactive({
  id: 0,
  parentId: 0,
  name: '',
  type: 2 as MenuType,
  path: '',
  component: '',
  icon: '',
  permission: '',
  sort: 10,
  status: 1 as 1 | 0,
  visible: true,
  keepAlive: false
});

const formRules: FormRules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [
    {
      validator: (_rule, value, callback) => {
        if (form.type !== 3 && !value) {
          callback(new Error('目录或菜单必须填写路由路径'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 图标快速映射表
const iconComponentMap: Record<string, any> = {
  Reading,
  Collection,
  DocumentAdd,
  DocumentChecked,
  Notebook,
  Tickets,
  EditPen,
  Memo,
  Files,
  Folder,
  FolderAdd,
  FolderOpened,
  School,
  User,
  MagicStick,
  Cpu,
  ChatDotRound,
  ChatLineSquare,
  Promotion,
  Compass,
  Connection,
  Grid,
  CircleCheck,
  Service,
  Star,
  Key,
  DataAnalysis,
  TrendCharts,
  PieChart,
  Histogram,
  DataBoard,
  DataLine,
  MapLocation,
  Coin,
  CreditCard,
  Setting,
  Operation,
  Tools,
  Monitor,
  Clock,
  Lock,
  Bell,
  SetUp,
  Pointer,
  Document
};

function getIconComponent(iconName?: string) {
  if (!iconName) return Document;
  return iconComponentMap[iconName] || Document;
}

// 常见前端组件建议列表
const COMPONENT_SUGGESTIONS = [
  { value: 'views/course/CourseList.vue' },
  { value: 'views/course/CourseCreate.vue' },
  { value: 'views/course/detail/CourseAI.vue' },
  { value: 'views/ai/AIChat.vue' },
  { value: 'views/ai/AIQuestion.vue' },
  { value: 'views/ai/AIExam.vue' },
  { value: 'views/ai/AIGrading.vue' },
  { value: 'views/ai/AgentCenter.vue' },
  { value: 'views/knowledge/KnowledgeList.vue' },
  { value: 'views/knowledge/KnowledgeCreate.vue' },
  { value: 'views/knowledge/KnowledgeOCR.vue' },
  { value: 'views/knowledge/detail/Chunks.vue' },
  { value: 'views/knowledge/detail/Embeddings.vue' },
  { value: 'views/knowledge/detail/RAGDebug.vue' },
  { value: 'views/knowledge/detail/KnowledgeGraph.vue' },
  { value: 'views/question/QuestionList.vue' },
  { value: 'views/question/QuestionBankList.vue' },
  { value: 'views/question/ExamList.vue' },
  { value: 'views/question/AssignmentList.vue' },
  { value: 'views/learning/LearningDashboard.vue' },
  { value: 'views/learning/PracticeAI.vue' },
  { value: 'views/learning/WrongQuestions.vue' },
  { value: 'views/analytics/LearningAnalytics.vue' },
  { value: 'views/analytics/CourseOverview.vue' },
  { value: 'views/system/users/UserList.vue' },
  { value: 'views/system/roles/RoleList.vue' },
  { value: 'views/system/menu/index.vue' },
  { value: 'views/system/tenants/TenantList.vue' },
  { value: 'views/system/organizations/OrgTree.vue' },
  { value: 'views/system/ai-model/ModelList.vue' },
  { value: 'views/system/prompt/PromptList.vue' },
  { value: 'views/system/quota/TenantQuota.vue' },
  { value: 'views/system/audit/AuditLog.vue' },
  { value: 'views/system/config/SystemConfig.vue' },
  { value: 'views/system/gateway/GatewayDashboard.vue' }
];

function queryComponentSuggestions(queryString: string, cb: (results: any[]) => void) {
  const results = queryString
    ? COMPONENT_SUGGESTIONS.filter((item) =>
        item.value.toLowerCase().includes(queryString.toLowerCase())
      )
    : COMPONENT_SUGGESTIONS;
  cb(results);
}

// 统计顶层模块数
const moduleCount = computed(() => {
  return tableData.value.filter((m) => m.type === 1 || m.parentId === 0).length;
});

// 计算全部节点总数
function countNodes(list: SysMenu[]): number {
  let count = 0;
  for (const item of list) {
    count++;
    if (item.children && item.children.length > 0) {
      count += countNodes(item.children);
    }
  }
  return count;
}
const totalMenuCount = computed(() => countNodes(tableData.value));

// 统计各类目节点数量
const typeStats = computed(() => {
  let dirs = 0;
  let menus = 0;
  let btns = 0;

  const traverse = (items: SysMenu[]) => {
    for (const item of items) {
      if (item.type === 1) dirs++;
      else if (item.type === 2) menus++;
      else if (item.type === 3) btns++;

      if (item.children && item.children.length > 0) {
        traverse(item.children);
      }
    }
  };

  traverse(tableData.value);
  return { dirs, menus, btns };
});

// 上级菜单下拉树形选项（包含顶级根目录虚拟项）
const parentTreeOptions = computed(() => {
  const sanitize = (nodes: SysMenu[]): any[] => {
    return nodes
      .filter((n) => n.type !== 3) // 按钮不允许作为父级
      .map((item) => ({
        id: item.id,
        name: `${item.type === 1 ? '📁 ' : '📄 '}${item.name}`,
        disabled: isEdit.value && item.id === form.id, // 不能选自己为父级
        children: item.children ? sanitize(item.children) : []
      }));
  };

  return [
    {
      id: 0,
      name: '🌱 根目录（创建顶级模块）',
      children: []
    },
    ...sanitize(tableData.value)
  ];
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await getMenuTree({ keyword: searchKeyword.value });
    tableData.value = res.data || [];
    tableKey.value++;
  } catch (err: any) {
    ElMessage.error(err?.message || '获取系统菜单失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  fetchData();
}

// 一键展开全部 / 折叠全部
function toggleExpandAll() {
  expandAll.value = !expandAll.value;
  tableKey.value++;
}

// 单行展开/折叠
function toggleRow(row: SysMenu) {
  if (row.children && row.children.length > 0) {
    tableRef.value?.toggleRowExpansion(row);
  }
}

function onRowClick(row: SysMenu, column: { property?: string }) {
  if (column?.property === 'name') {
    toggleRow(row);
  }
}

function handleTypeChange(newType: MenuType) {
  if (newType === 3) {
    // 按钮类型自动清空路径与组件
    form.path = '';
    form.component = '';
    form.icon = '';
  }
}

function handleCreate(parentId = 0, parentType?: MenuType) {
  isEdit.value = false;
  let inferredType: MenuType = 2;
  if (parentId === 0) {
    inferredType = 1; // 顶级目录
  } else if (parentType === 2) {
    inferredType = 3; // 菜单下默认创建按钮权限
  } else {
    inferredType = 2; // 目录下默认创建页面菜单
  }

  Object.assign(form, {
    id: 0,
    parentId,
    name: '',
    type: inferredType,
    path: '',
    component: '',
    icon: inferredType === 1 ? 'Folder' : inferredType === 2 ? 'Document' : '',
    permission: '',
    sort: 10,
    status: 1,
    visible: true,
    keepAlive: false
  });

  dialogVisible.value = true;
  nextTick(() => {
    formRef.value?.clearValidate();
  });
}

function handleEdit(row: SysMenu) {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || 0,
    name: row.name,
    type: row.type,
    path: row.path || '',
    component: row.component || '',
    icon: row.icon || '',
    permission: row.permission || '',
    sort: row.sort || 10,
    status: row.status ?? 1,
    visible: row.visible !== false,
    keepAlive: Boolean(row.keepAlive)
  });

  dialogVisible.value = true;
  nextTick(() => {
    formRef.value?.clearValidate();
  });
}

async function saveMenu() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    saving.value = true;
    try {
      if (isEdit.value) {
        await updateMenu(form.id, {
          parentId: form.parentId,
          name: form.name,
          type: form.type,
          path: form.path,
          component: form.component,
          icon: form.icon,
          permission: form.permission,
          sort: form.sort,
          status: form.status,
          visible: form.visible,
          keepAlive: form.keepAlive
        });
        ElMessage.success(`菜单「${form.name}」已保存更新`);
      } else {
        await createMenu({
          parentId: form.parentId,
          name: form.name,
          type: form.type,
          path: form.path,
          component: form.component,
          icon: form.icon,
          permission: form.permission,
          sort: form.sort,
          status: form.status,
          visible: form.visible,
          keepAlive: form.keepAlive
        });
        ElMessage.success(`新增菜单「${form.name}」创建成功`);
      }
      dialogVisible.value = false;
      await fetchData();
    } catch (err: any) {
      ElMessage.error(err?.message || '保存菜单失败');
    } finally {
      saving.value = false;
    }
  });
}

async function handleDelete(row: SysMenu) {
  try {
    await ElMessageBox.confirm(
      `确定删除菜单「${row.name}」${row.children?.length ? '及其所有子项' : ''}？操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    );

    await deleteMenu(row.id);
    ElMessage.success(`菜单「${row.name}」已删除`);
    await fetchData();
  } catch {
    // cancelled
  }
}

async function handleResetDefault() {
  loading.value = true;
  try {
    await resetDefaultMenus();
    ElMessage.success('已恢复为 EduMind 8 大核心模块预设菜单');
    await fetchData();
  } catch (err: any) {
    ElMessage.error(err?.message || '重置菜单失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchData();
});
</script>

<style scoped lang="scss">
/* 页面大容器与沉浸背景 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
}

/* 顶部搜索筛选卡片：对标 Code Compass filter-card */
.filter-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
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
  gap: 14px;
  flex-wrap: wrap;

  .title-wrap {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.filter-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.2px;
}

.filter-tag {
  font-size: 11.5px;
  font-weight: 600;
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(22, 119, 255, 0.16);
}

.filter-count {
  font-size: 12.5px;
  color: #64748b;

  strong {
    color: #0f172a;
    font-weight: 700;
  }

  .type-stats {
    margin-left: 4px;
    color: #94a3b8;
    font-size: 11.5px;
  }
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  .search-input :deep(.el-input__wrapper) {
    border-radius: 999px;
    box-shadow: 0 0 0 1px #e2e8f0 inset;
    background: #ffffff;

    &:hover {
      box-shadow: 0 0 0 1px #93c5fd inset;
    }
  }

  .action-btn {
    border-radius: 999px;
    font-weight: 500;
    font-size: 13px;
  }

  .text-amber {
    color: #d97706;
    border-color: #fde68a;
    background: #fffbeb;

    &:hover {
      background: #fef3c7;
      border-color: #fcd34d;
    }
  }

  .add-btn {
    border-radius: 999px;
    font-weight: 600;
    padding: 8px 18px;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.28);
    border: none;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);
    }
  }
}

/* 主数据表格卡片 */
.table-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 0;
  }
}

/* ==========================================================
   核心：精确消除第一列占位符与箭头宽度差异，文本永不竖向折行
   1:1 复刻 Code Compass menu-tree-table 机制
   ========================================================== */
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

  &.is-parent {
    cursor: pointer;

    &:hover .menu-name-text {
      color: #1677ff;
    }
  }
}

.name-icon {
  flex-shrink: 0;
}

.menu-name-text {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
  transition: color 0.15s ease;
  white-space: nowrap;
}

.child-count-pill {
  font-size: 11px;
  color: #64748b;
  background: rgba(15, 23, 42, 0.05);
  padding: 1px 7px;
  border-radius: 999px;
  font-weight: 600;
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

.type-tag {
  font-weight: 600;
  padding: 2px 8px;
}

.code-pill {
  font-family: monospace;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.04);
  padding: 3px 8px;
  border-radius: 6px;
  color: #334155;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.component-text {
  font-family: monospace;
  font-size: 11.5px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 200px;
}

.perm-tag {
  font-size: 11px;
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
  font-weight: 700;
  color: #64748b;
}

.text-placeholder {
  color: #94a3b8;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  .el-button {
    font-weight: 600;
  }
}

/* 对话框内表单美化 */
.menu-dialog {
  :deep(.el-dialog__body) {
    padding: 16px 24px;
  }
}

.menu-form {
  .radio-label {
    font-size: 13px;

    strong {
      color: #0f172a;
    }
  }

  .form-row-grid {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 999px;
  }
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;

  .el-button {
    min-width: 84px;
    font-weight: 600;
  }
}
</style>
