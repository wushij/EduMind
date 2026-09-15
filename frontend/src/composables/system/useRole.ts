import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import type { SysMenuNode } from '@/constants/permission';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type ElTree } from 'element-plus';
import {
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
  Money
} from '@element-plus/icons-vue';
import {
  createRole,
  deleteRole,
  getPermissions,
  getRoles,
  updateRole,
  updateRolePermissions
} from '@/api/system/role';
import type { PermissionVO, RoleVO } from '@/types/system/rbac';
import {
  buildSidebarMenuTree,
  flattenPermissions,
  mergeSystemPermissions
} from '@/constants/permission';

const ICON_COMPONENT_MAP: Record<string, unknown> = {
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
  Money
};

export function getRoleTagType(code: string) {
  const c = code.toUpperCase();
  if (c.includes('ADMIN')) return 'danger';
  if (c.includes('TEACHER')) return 'primary';
  if (c.includes('STUDENT')) return 'warning';
  return 'info';
}

export function getIconComponent(iconName?: string) {
  return (iconName && ICON_COMPONENT_MAP[iconName]) || Document;
}

export function filterTreeNode(val: string, data: { name?: string; permission?: string }) {
  if (!val) return true;
  const kw = val.toLowerCase();
  return data.name?.toLowerCase().includes(kw) || data.permission?.toLowerCase().includes(kw);
}

export function filterRoles(roles: RoleVO[], keyword: string) {
  const kw = keyword.trim().toLowerCase();
  if (!kw) return roles;
  return roles.filter(
    (r) =>
      r.roleName.toLowerCase().includes(kw) ||
      r.roleCode.toLowerCase().includes(kw) ||
      (r.description || '').toLowerCase().includes(kw)
  );
}

export function useRole() {
  const roles = ref<RoleVO[]>([]);
  const apiPermissions = ref<PermissionVO[]>([]);
  const rawPermissions = ref<PermissionVO[]>([]);
  const loading = ref(false);
  const permissionLoading = ref(false);
  const submitting = ref(false);
  const searchKeyword = ref('');

  const formVisible = ref(false);
  const formMode = ref<'create' | 'edit'>('create');
  const editingRoleId = ref<number | null>(null);
  const formRef = ref<FormInstance>();

  const permDrawer = ref(false);
  const currentRole = ref<RoleVO | null>(null);
  const treeRef = ref<InstanceType<typeof ElTree>>();
  const treeFilterText = ref('');
  const isTreeExpanded = ref(true);

  const form = reactive({
    roleCode: '',
    roleName: '',
    description: ''
  });

  const rules: FormRules = {
    roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
    roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
  };

  const menuTreeData = computed(() => buildSidebarMenuTree(rawPermissions.value));

  const filteredRoles = computed(() => filterRoles(roles.value, searchKeyword.value));

  watch(treeFilterText, (val) => {
    treeRef.value?.filter(val);
  });

  async function loadRoles() {
    loading.value = true;
    try {
      const res = await getRoles();
      roles.value = res.data || [];
    } finally {
      loading.value = false;
    }
  }

  async function loadPermissions() {
    permissionLoading.value = true;
    try {
      const res = await getPermissions();
      apiPermissions.value = res.data || [];
      rawPermissions.value = mergeSystemPermissions(apiPermissions.value);
    } finally {
      permissionLoading.value = false;
    }
  }

  function resetForm() {
    form.roleCode = '';
    form.roleName = '';
    form.description = '';
  }

  function openCreateDialog() {
    formMode.value = 'create';
    editingRoleId.value = null;
    resetForm();
    formVisible.value = true;
  }

  function openEditDialog(row: RoleVO) {
    formMode.value = 'edit';
    editingRoleId.value = row.id;
    form.roleCode = row.roleCode;
    form.roleName = row.roleName;
    form.description = row.description || '';
    formVisible.value = true;
  }

  async function openPermissionDrawer(row: RoleVO) {
    currentRole.value = row;
    permDrawer.value = true;
    treeFilterText.value = '';

    if (!rawPermissions.value.length) {
      await loadPermissions();
    }

    await nextTick();

    const flat = flattenPermissions(rawPermissions.value);
    const targetCodes = row.permissions || [];
    const matchedIds = flat
      .filter((p) => targetCodes.includes(p.permissionCode))
      .map((p) => p.id);

    treeRef.value?.setCheckedKeys(matchedIds, false);
  }

  function checkAllNodes(check: boolean) {
    if (check) {
      const allLeafIds = flattenPermissions(rawPermissions.value).map((p) => p.id);
      treeRef.value?.setCheckedKeys(allLeafIds, false);
    } else {
      treeRef.value?.setCheckedKeys([], false);
    }
  }

  function toggleTreeExpand() {
    isTreeExpanded.value = !isTreeExpanded.value;
    menuTreeData.value.forEach((node) => {
      const elNode = treeRef.value?.getNode(node.id);
      if (elNode) {
        elNode.expanded = isTreeExpanded.value;
      }
    });
  }

  async function submitForm() {
    await formRef.value?.validate();
    submitting.value = true;
    try {
      if (formMode.value === 'create') {
        await createRole({
          roleCode: form.roleCode,
          roleName: form.roleName,
          description: form.description
        });
        ElMessage.success('角色创建成功');
      } else if (editingRoleId.value) {
        await updateRole(editingRoleId.value, {
          roleName: form.roleName,
          description: form.description
        });
        ElMessage.success('角色已保存');
      }
      formVisible.value = false;
      await loadRoles();
    } finally {
      submitting.value = false;
    }
  }

  async function savePermissions() {
    if (!currentRole.value) return;
    submitting.value = true;
    try {
      const checkedKeys = (treeRef.value?.getCheckedKeys(false) as unknown[]) || [];
      const persistedIds = new Set(flattenPermissions(apiPermissions.value).map((item) => item.id));
      const validNumericIds = checkedKeys.filter(
        (key): key is number => typeof key === 'number' && persistedIds.has(key)
      );

      await updateRolePermissions(currentRole.value.id, validNumericIds);
      ElMessage.success('已保存权限');
      permDrawer.value = false;
      await loadRoles();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '保存权限失败';
      ElMessage.error(message);
    } finally {
      submitting.value = false;
    }
  }

  async function handleDelete(row: RoleVO) {
    if (row.roleCode === 'ADMIN') {
      ElMessage.warning('超级管理员角色无法删除');
      return;
    }
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '确认删除', {
      type: 'warning'
    });
    await deleteRole(row.id);
    ElMessage.success('已删除');
    await loadRoles();
  }

  onMounted(loadRoles);

  return {
    roles,
    loading,
    permissionLoading,
    submitting,
    searchKeyword,
    formVisible,
    formMode,
    formRef,
    permDrawer,
    currentRole,
    treeRef,
    treeFilterText,
    isTreeExpanded,
    form,
    rules,
    menuTreeData,
    filteredRoles,
    loadRoles,
    loadPermissions,
    openCreateDialog,
    openEditDialog,
    openPermissionDrawer,
    checkAllNodes,
    toggleTreeExpand,
    submitForm,
    savePermissions,
    handleDelete,
    getRoleTagType,
    getIconComponent,
    filterTreeNode
  };
}

export function countPermissionNodes(list: SysMenuNode[]): number {
  let count = 0;
  for (const item of list) {
    count++;
    if (item.children && item.children.length > 0) {
      count += countPermissionNodes(item.children);
    }
  }
  return count;
}

export function usePermissionTree() {
  const loading = ref(false);
  const rawPermissions = ref<PermissionVO[]>([]);
  const keyword = ref('');
  const expandAll = ref(false);
  const tableKey = ref(0);
  const tableRef = ref();

  const treeTableData = computed(() => buildSidebarMenuTree(rawPermissions.value, keyword.value));

  const moduleCount = computed(() => treeTableData.value.filter((m) => m.type === 1).length);

  const totalNodeCount = computed(() => countPermissionNodes(treeTableData.value));

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
      rawPermissions.value = mergeSystemPermissions(res.data || []);
    } catch {
      rawPermissions.value = mergeSystemPermissions([]);
    } finally {
      loading.value = false;
    }
  }

  onMounted(fetchData);

  return {
    loading,
    rawPermissions,
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
  };
}
