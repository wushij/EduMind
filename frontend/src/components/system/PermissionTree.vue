<template>
  <div class="permission-tree-container">
    <!-- 顶部过滤与快捷操作栏 -->
    <div class="tree-toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索菜单名称或权限标识 (如 course / view)..."
        clearable
        size="small"
        :prefix-icon="Search"
        class="search-input"
        style="width: 240px"
      />

      <div class="toolbar-actions">
        <template v-if="checkable">
          <el-button size="small" link type="primary" @click="selectAll">全选</el-button>
          <el-button size="small" link @click="clearAll">清空</el-button>
        </template>
        <el-button size="small" link @click="toggleExpand">
          {{ isAllExpanded ? '折叠全部' : '展开全部' }}
        </el-button>

        <div v-if="checkable" class="selected-counter-badge">
          已勾选 <span class="num">{{ selectedLeafIds.length }}</span> 项权限
        </div>
      </div>
    </div>

    <!-- 树主体展示区 -->
    <div v-loading="loading" class="tree-content-wrapper" :style="{ maxHeight: computedHeight }">
      <el-empty
        v-if="!loading && treeData.length === 0"
        description="未匹配到任何相关权限节点"
        :image-size="80"
      />

      <el-tree
        v-else
        ref="treeRef"
        :data="treeData"
        :show-checkbox="checkable"
        node-key="id"
        :default-expand-all="isAllExpanded"
        :filter-node-method="filterNode"
        :props="{ label: 'name', children: 'children' }"
        @check="handleTreeCheck"
      >
        <template #default="{ data }">
          <div class="tree-node-item">
            <el-icon v-if="data.icon" class="node-icon" color="#6366f1">
              <component :is="getIconComponent(data.icon)" />
            </el-icon>
            <span class="node-title">{{ data.name }}</span>
            <span v-if="data.children?.length" class="node-count">({{ data.children.length }})</span>
            <el-tag
              v-if="data.permission"
              size="small"
              type="danger"
              round
              class="node-perm-tag font-mono"
            >
              {{ data.permission }}
            </el-tag>
          </div>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue';
import {
  Search,
  Setting,
  Reading,
  Document,
  Tickets,
  EditPen,
  Collection,
  Cpu,
  TrendCharts,
  FolderOpened,
  Bell,
  MagicStick,
  DataAnalysis,
  Files,
  Service,
  CircleCheck,
  Folder,
  FolderAdd,
  DocumentChecked,
  Notebook,
  Upload,
  School,
  Connection,
  User,
  Lock,
  Key,
  ChatLineSquare,
  Money,
  Pointer,
  Operation
} from '@element-plus/icons-vue';
import type { ElTree } from 'element-plus';
import type { PermissionVO } from '@/types/system/rbac';
import {
  buildSidebarMenuTree,
  flattenPermissions,
  type SysMenuNode
} from '@/constants/permission';

const props = withDefaults(
  defineProps<{
    permissions: PermissionVO[];
    modelValue?: number[];
    checkable?: boolean;
    loading?: boolean;
    defaultExpandAll?: boolean;
    maxHeight?: string | number;
  }>(),
  {
    modelValue: () => [],
    checkable: false,
    loading: false,
    defaultExpandAll: true,
    maxHeight: '520px'
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: number[]): void;
  (e: 'change', value: number[]): void;
}>();

const treeRef = ref<InstanceType<typeof ElTree>>();
const searchKeyword = ref('');
const isAllExpanded = ref(props.defaultExpandAll);
const selectedLeafIds = ref<number[]>([]);

watch(
  () => props.modelValue,
  (val) => {
    selectedLeafIds.value = [...(val || [])];
    nextTick(() => {
      treeRef.value?.setCheckedKeys(selectedLeafIds.value, false);
    });
  },
  { immediate: true, deep: true }
);

watch(searchKeyword, (val) => {
  treeRef.value?.filter(val);
});

function filterNode(val: string, data: any) {
  if (!val) return true;
  const kw = val.toLowerCase();
  return data.name?.toLowerCase().includes(kw) || data.permission?.toLowerCase().includes(kw);
}

const treeData = computed(() => {
  return buildSidebarMenuTree(props.permissions);
});

const computedHeight = computed(() => {
  if (typeof props.maxHeight === 'number') {
    return `${props.maxHeight}px`;
  }
  return props.maxHeight;
});

function getIconComponent(iconName?: string) {
  const map: Record<string, any> = {
    Setting,
    Reading,
    Document,
    Tickets,
    EditPen,
    Collection,
    Cpu,
    TrendCharts,
    FolderOpened,
    Bell,
    MagicStick,
    DataAnalysis,
    Files,
    Service,
    CircleCheck,
    Folder,
    FolderAdd,
    DocumentChecked,
    Notebook,
    Upload,
    School,
    Connection,
    User,
    Lock,
    Key,
    ChatLineSquare,
    Money,
    Pointer,
    Operation
  };
  return (iconName && map[iconName]) || Document;
}

function handleTreeCheck() {
  const checkedKeys = (treeRef.value?.getCheckedKeys(false) as any[]) || [];
  const numericIds = checkedKeys.filter((k) => typeof k === 'number');
  selectedLeafIds.value = numericIds;
  emit('update:modelValue', numericIds);
  emit('change', numericIds);
}

function selectAll() {
  const allIds = flattenPermissions(props.permissions).map((p) => p.id);
  selectedLeafIds.value = allIds;
  treeRef.value?.setCheckedKeys(allIds, false);
  emit('update:modelValue', allIds);
  emit('change', allIds);
}

function clearAll() {
  selectedLeafIds.value = [];
  treeRef.value?.setCheckedKeys([], false);
  emit('update:modelValue', []);
  emit('change', []);
}

function toggleExpand() {
  isAllExpanded.value = !isAllExpanded.value;
  const nodes = treeData.value;
  nodes.forEach((node) => {
    const elNode = treeRef.value?.getNode(node.id);
    if (elNode) {
      elNode.expanded = isAllExpanded.value;
    }
  });
}

defineExpose({
  selectAll,
  clearAll,
  toggleExpand
});
</script>

<style scoped>
.permission-tree-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.tree-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-counter-badge {
  font-size: 12px;
  color: #64748b;
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
}

.selected-counter-badge .num {
  color: #6366f1;
  font-weight: 700;
}

.tree-content-wrapper {
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
}

.tree-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  width: 100%;
}

.node-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.node-title {
  font-weight: 500;
  color: #1e293b;
}

.node-count {
  font-size: 11px;
  color: #94a3b8;
}

.node-perm-tag {
  margin-left: auto;
  font-size: 11px;
}
</style>
