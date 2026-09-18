import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  Operation,
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
import {
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
  resetDefaultMenus
} from '@/services/system/menu-service';
import { getStoredMenuTree } from '@/mock/menu';
import { filterMenuTree } from '@/utils/system/menu-tree-filter';
import type { SysMenu, MenuType } from '@/types/system/menu';

const ICON_COMPONENT_MAP: Record<string, unknown> = {
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

export const COMPONENT_SUGGESTIONS = [
  { value: 'views/course/CourseList.vue' },
  { value: 'views/course/CourseCreate.vue' },
  { value: 'views/course/detail/CourseAI.vue' },
  { value: 'views/ai/AIQuestion.vue' },
  { value: 'views/ai/AIExam.vue' },
  { value: 'views/ai/AIGrading.vue' },
  { value: 'views/ai/AgentCenter.vue' },
  { value: 'views/ai/memory/MemoryList.vue' },
  { value: 'views/profile/Profile.vue' },
  { value: 'views/profile/Security.vue' },
  { value: 'views/profile/AIUsage.vue' },
  { value: 'views/profile/Preferences.vue' },
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

export function getIconComponent(iconName?: string) {
  if (!iconName) return Document;
  return ICON_COMPONENT_MAP[iconName] || Document;
}

export function countNodes(list: SysMenu[]): number {
  let count = 0;
  for (const item of list) {
    count++;
    if (item.children && item.children.length > 0) {
      count += countNodes(item.children);
    }
  }
  return count;
}

export function countTypeStats(items: SysMenu[]) {
  let dirs = 0;
  let menus = 0;
  let btns = 0;

  const traverse = (nodes: SysMenu[]) => {
    for (const item of nodes) {
      if (item.type === 1) dirs++;
      else if (item.type === 2) menus++;
      else if (item.type === 3) btns++;

      if (item.children && item.children.length > 0) {
        traverse(item.children);
      }
    }
  };

  traverse(items);
  return { dirs, menus, btns };
}

export function buildParentTreeOptions(
  tableData: SysMenu[],
  isEdit: boolean,
  editingId: number
) {
  const sanitize = (nodes: SysMenu[]): Array<{
    id: number;
    name: string;
    disabled?: boolean;
    children: unknown[];
  }> => {
    return nodes
      .filter((n) => n.type !== 3)
      .map((item) => ({
        id: item.id,
        name: `${item.type === 1 ? '[目录] ' : '[菜单] '}${item.name}`,
        disabled: isEdit && item.id === editingId,
        children: item.children ? sanitize(item.children) : []
      }));
  };

  return [
    {
      id: 0,
      name: '根目录（创建顶级模块）',
      children: []
    },
    ...sanitize(tableData)
  ];
}

export function inferMenuType(parentId: number, parentType?: MenuType): MenuType {
  if (parentId === 0) return 1;
  if (parentType === 2) return 3;
  return 2;
}

export function queryComponentSuggestions(queryString: string, cb: (results: typeof COMPONENT_SUGGESTIONS) => void) {
  const results = queryString
    ? COMPONENT_SUGGESTIONS.filter((item) =>
        item.value.toLowerCase().includes(queryString.toLowerCase())
      )
    : COMPONENT_SUGGESTIONS;
  cb(results);
}

export function useMenu() {
  const loading = ref(false);
  const saving = ref(false);
  const tableData = ref<SysMenu[]>([]);
  const tableRef = ref<{ toggleRowExpansion: (row: SysMenu) => void }>();
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

  const moduleCount = computed(() => {
    return tableData.value.filter((m) => m.type === 1 || m.parentId === 0).length;
  });

  const totalMenuCount = computed(() => countNodes(tableData.value));

  const typeStats = computed(() => countTypeStats(tableData.value));

  const parentTreeOptions = computed(() =>
    buildParentTreeOptions(tableData.value, isEdit.value, form.id)
  );

  async function fetchData() {
    loading.value = true;
    try {
      const res = await getMenuTree({ keyword: searchKeyword.value });
      let data = res.data || [];
      // 后端库表暂无菜单时，回退本地预设树（与「恢复预设」一致，避免管理页空白）
      if (data.length === 0 && !searchKeyword.value.trim()) {
        const localTree = filterMenuTree(getStoredMenuTree(), searchKeyword.value);
        if (localTree.length > 0) {
          data = localTree;
        }
      }
      tableData.value = data;
      tableKey.value++;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '获取系统菜单失败';
      ElMessage.error(message);
      if (!searchKeyword.value.trim()) {
        const localTree = filterMenuTree(getStoredMenuTree(), searchKeyword.value);
        if (localTree.length > 0) {
          tableData.value = localTree;
          tableKey.value++;
        }
      }
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    fetchData();
  }

  function toggleExpandAll() {
    expandAll.value = !expandAll.value;
    tableKey.value++;
  }

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
      form.path = '';
      form.component = '';
      form.icon = '';
    }
  }

  function handleCreate(parentId = 0, parentType?: MenuType) {
    isEdit.value = false;
    const inferredType = inferMenuType(parentId, parentType);

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
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : '保存菜单失败';
        ElMessage.error(message);
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
      const res = await resetDefaultMenus();
      tableData.value = res.data || [];
      tableKey.value++;
      ElMessage.success('已恢复为 EduMind 8 大核心模块预设菜单');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '重置菜单失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    fetchData();
  });

  return {
    loading,
    saving,
    tableData,
    tableRef,
    searchKeyword,
    expandAll,
    tableKey,
    dialogVisible,
    isEdit,
    formRef,
    form,
    formRules,
    moduleCount,
    totalMenuCount,
    typeStats,
    parentTreeOptions,
    fetchData,
    handleSearch,
    toggleExpandAll,
    toggleRow,
    onRowClick,
    handleTypeChange,
    handleCreate,
    handleEdit,
    saveMenu,
    handleDelete,
    handleResetDefault,
    getIconComponent,
    queryComponentSuggestions
  };
}
