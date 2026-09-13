<template>
  <div class="icon-picker-wrapper">
    <!-- 图标选择触发区 (输入框前缀预览 + 弹出选择器) -->
    <el-popover
      v-model:visible="visible"
      placement="bottom-start"
      :width="460"
      trigger="click"
      popper-class="em-icon-picker-popper"
      :teleported="true"
    >
      <template #reference>
        <el-input
          :model-value="modelValue"
          placeholder="点击选择图标或输入图标名称..."
          clearable
          class="icon-input-field"
          @clear="handleClear"
          @input="handleInput"
        >
          <template #prefix>
            <div class="current-icon-badge">
              <el-icon v-if="modelValue && getIconComponent(modelValue)" :size="18" color="#1677ff">
                <component :is="getIconComponent(modelValue)" />
              </el-icon>
              <el-icon v-else :size="18" color="#94a3b8">
                <Document />
              </el-icon>
            </div>
          </template>
          <template #suffix>
            <el-button link type="primary" size="small" class="select-btn">
              选择图标
            </el-button>
          </template>
        </el-input>
      </template>

      <!-- 弹出面板内容 -->
      <div class="picker-panel">
        <!-- 头部搜索与分类切换 -->
        <div class="panel-header">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索图标名称 (如 Setting, Cpu, Reading)..."
            size="small"
            clearable
            :prefix-icon="Search"
            class="search-bar"
          />
          <el-radio-group v-model="activeCategory" size="small" class="category-tabs">
            <el-radio-button label="all">全部 ({{ filteredIcons.length }})</el-radio-button>
            <el-radio-button label="teaching">教学与课程</el-radio-button>
            <el-radio-button label="ai">AI 智能</el-radio-button>
            <el-radio-button label="analytics">数据图表</el-radio-button>
            <el-radio-button label="system">系统设置</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 图标矩阵网格 -->
        <div class="icons-grid-scroll">
          <div v-if="filteredIcons.length > 0" class="icons-grid">
            <div
              v-for="item in filteredIcons"
              :key="item.name"
              class="icon-grid-item"
              :class="{ 'is-selected': modelValue === item.name }"
              :title="item.label || item.name"
              @click="selectIcon(item.name)"
            >
              <el-icon :size="22" class="grid-icon">
                <component :is="item.component" />
              </el-icon>
              <span class="grid-name">{{ item.name }}</span>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty description="未找到匹配的图标" :image-size="48" />
          </div>
        </div>

        <!-- 底部快捷工具 -->
        <div class="panel-footer">
          <span class="footer-tip">已选图标：<strong>{{ modelValue || '未选择' }}</strong></span>
          <div class="footer-actions">
            <el-button size="small" round @click="handleClear">清空</el-button>
            <el-button size="small" type="primary" round @click="visible = false">确定</el-button>
          </div>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  Search,
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
  Refresh,
  Plus,
  Edit,
  Delete,
  Warning,
  QuestionFilled,
  List,
  Finished,
  DocumentCopy,
  Avatar,
  Upload,
  Download,
  CollectionTag,
  Pointer,
  Share,
  Document
} from '@element-plus/icons-vue';

interface IconMeta {
  name: string;
  label?: string;
  category: 'teaching' | 'ai' | 'analytics' | 'system';
  component: any;
}

const props = withDefaults(
  defineProps<{
    modelValue?: string;
  }>(),
  {
    modelValue: ''
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void;
  (e: 'change', val: string): void;
}>();

const visible = ref(false);
const searchKeyword = ref('');
const activeCategory = ref<'all' | 'teaching' | 'ai' | 'analytics' | 'system'>('all');

/**
 * 结构化精选图标库 (覆盖 EduMind 8 大核心模块高频图标)
 */
const ICON_LIST: IconMeta[] = [
  // 教学与课程
  { name: 'Reading', label: '阅读课程', category: 'teaching', component: Reading },
  { name: 'Collection', label: '我的收藏/课程', category: 'teaching', component: Collection },
  { name: 'DocumentAdd', label: '新建课程/作业', category: 'teaching', component: DocumentAdd },
  { name: 'DocumentChecked', label: '试卷审查', category: 'teaching', component: DocumentChecked },
  { name: 'Notebook', label: '教案/笔记本', category: 'teaching', component: Notebook },
  { name: 'Tickets', label: '试卷编排', category: 'teaching', component: Tickets },
  { name: 'EditPen', label: '修改/编辑', category: 'teaching', component: EditPen },
  { name: 'Memo', label: '题目便签', category: 'teaching', component: Memo },
  { name: 'Files', label: '文档管理', category: 'teaching', component: Files },
  { name: 'Folder', label: '知识库', category: 'teaching', component: Folder },
  { name: 'FolderAdd', label: '新增知识库', category: 'teaching', component: FolderAdd },
  { name: 'FolderOpened', label: '展开知识库', category: 'teaching', component: FolderOpened },
  { name: 'School', label: '校区租户', category: 'teaching', component: School },
  { name: 'User', label: '用户学员', category: 'teaching', component: User },
  { name: 'List', label: '任务清单', category: 'teaching', component: List },
  { name: 'Finished', label: '批改完成', category: 'teaching', component: Finished },
  { name: 'DocumentCopy', label: '导出复制', category: 'teaching', component: DocumentCopy },

  // AI 智能
  { name: 'MagicStick', label: 'AI 教学/魔法', category: 'ai', component: MagicStick },
  { name: 'Cpu', label: 'AI 模型/算力', category: 'ai', component: Cpu },
  { name: 'ChatDotRound', label: 'AI 助手对话', category: 'ai', component: ChatDotRound },
  { name: 'ChatLineSquare', label: 'Prompt 提示词', category: 'ai', component: ChatLineSquare },
  { name: 'Promotion', label: 'AI 推荐/推送', category: 'ai', component: Promotion },
  { name: 'Compass', label: 'AI 广场/罗盘', category: 'ai', component: Compass },
  { name: 'Connection', label: '知识图谱/网关', category: 'ai', component: Connection },
  { name: 'Grid', label: '切片网格', category: 'ai', component: Grid },
  { name: 'CircleCheck', label: 'AI 批改确认', category: 'ai', component: CircleCheck },
  { name: 'Service', label: 'AI 智能客服', category: 'ai', component: Service },
  { name: 'Star', label: '推荐星选', category: 'ai', component: Star },
  { name: 'Key', label: '记忆/密钥', category: 'ai', component: Key },

  // 数据分析
  { name: 'DataAnalysis', label: '教学分析看板', category: 'analytics', component: DataAnalysis },
  { name: 'TrendCharts', label: '学情走势趋势', category: 'analytics', component: TrendCharts },
  { name: 'PieChart', label: '掌握度分布饼图', category: 'analytics', component: PieChart },
  { name: 'Histogram', label: '柱状统计图', category: 'analytics', component: Histogram },
  { name: 'DataBoard', label: '学习总览大盘', category: 'analytics', component: DataBoard },
  { name: 'DataLine', label: '折线诊断图', category: 'analytics', component: DataLine },
  { name: 'MapLocation', label: '学习路径图', category: 'analytics', component: MapLocation },
  { name: 'Coin', label: 'AI Token 消耗', category: 'analytics', component: Coin },
  { name: 'CreditCard', label: '配额额度', category: 'analytics', component: CreditCard },

  // 系统设置
  { name: 'Setting', label: '系统管理', category: 'system', component: Setting },
  { name: 'Operation', label: '菜单与操作', category: 'system', component: Operation },
  { name: 'Tools', label: '偏好工具', category: 'system', component: Tools },
  { name: 'Monitor', label: '全局配置监控', category: 'system', component: Monitor },
  { name: 'Clock', label: '审计日志', category: 'system', component: Clock },
  { name: 'Lock', label: '角色权限安全', category: 'system', component: Lock },
  { name: 'Bell', label: '系统公告通知', category: 'system', component: Bell },
  { name: 'SetUp', label: '文档解析设置', category: 'system', component: SetUp },
  { name: 'Search', label: '检索诊断/查询', category: 'system', component: Search },
  { name: 'Refresh', label: '刷新重载', category: 'system', component: Refresh },
  { name: 'Warning', label: '干预预警', category: 'system', component: Warning },
  { name: 'QuestionFilled', label: '错题成因问答', category: 'system', component: QuestionFilled },
  { name: 'Avatar', label: '个人中心头像', category: 'system', component: Avatar },
  { name: 'Pointer', label: '按钮操作指针', category: 'system', component: Pointer },
  { name: 'CollectionTag', label: '分类标签', category: 'system', component: CollectionTag }
];

const iconMap: Record<string, any> = ICON_LIST.reduce((acc, cur) => {
  acc[cur.name] = cur.component;
  return acc;
}, {} as Record<string, any>);

function getIconComponent(name: string) {
  return iconMap[name] || Document;
}

const filteredIcons = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  return ICON_LIST.filter((item) => {
    const matchCategory =
      activeCategory.value === 'all' || item.category === activeCategory.value;
    if (!matchCategory) return false;
    if (!kw) return true;
    return (
      item.name.toLowerCase().includes(kw) ||
      (item.label && item.label.toLowerCase().includes(kw))
    );
  });
});

function selectIcon(name: string) {
  emit('update:modelValue', name);
  emit('change', name);
  visible.value = false;
}

function handleInput(val: string) {
  emit('update:modelValue', val);
  emit('change', val);
}

function handleClear() {
  emit('update:modelValue', '');
  emit('change', '');
}
</script>

<style scoped lang="scss">
.icon-picker-wrapper {
  width: 100%;
}

.icon-input-field {
  width: 100%;

  :deep(.el-input__wrapper) {
    padding-left: 8px;
    border-radius: 999px;
  }
}

.current-icon-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: rgba(22, 119, 255, 0.08);
  margin-right: 4px;
}

.select-btn {
  font-weight: 600;
  font-size: 12.5px;
}

.picker-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px;
}

.panel-header {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .search-bar :deep(.el-input__wrapper) {
    border-radius: 999px;
    background: #f8fafc;
  }

  .category-tabs {
    width: 100%;
    display: flex;
    overflow-x: auto;

    :deep(.el-radio-button) {
      flex: 1;

      .el-radio-button__inner {
        width: 100%;
        padding: 5px 8px;
        font-size: 11.5px;
      }
    }
  }
}

.icons-grid-scroll {
  max-height: 250px;
  overflow-y: auto;
  padding: 4px 2px;
  scrollbar-width: thin;
}

.icons-grid-scroll::-webkit-scrollbar {
  width: 5px;
}

.icons-grid-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.icons-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.icon-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.18s ease;

  .grid-icon {
    color: #475569;
    transition: transform 0.2s ease, color 0.2s ease;
  }

  .grid-name {
    margin-top: 5px;
    font-size: 10.5px;
    color: #64748b;
    text-align: center;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 0 2px;
  }

  &:hover {
    border-color: #93c5fd;
    background: #eff6ff;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.08);

    .grid-icon {
      color: #1677ff;
      transform: scale(1.15);
    }

    .grid-name {
      color: #1677ff;
      font-weight: 600;
    }
  }

  &.is-selected {
    border-color: #1677ff;
    background: #eaf3ff;

    .grid-icon {
      color: #1677ff;
    }

    .grid-name {
      color: #1677ff;
      font-weight: 700;
    }
  }
}

.empty-state {
  padding: 20px 0;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;

  .footer-tip {
    font-size: 12px;
    color: #64748b;

    strong {
      color: #1677ff;
    }
  }

  .footer-actions {
    display: flex;
    gap: 6px;
  }
}
</style>
