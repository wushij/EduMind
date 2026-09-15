<template>
  <div class="tool-list-page">
    <div class="tool-hero-card">
      <div class="hero-bg-glow hero-bg-glow--blue" />
      <div class="hero-bg-glow hero-bg-glow--purple" />

      <div class="hero-header-row">
        <div class="hero-title-area">
          <div class="hero-eyebrow">
            <span class="eyebrow-chip">EduMind AI Compute</span>
            <span class="eyebrow-divider">/</span>
            <span class="eyebrow-sub">教学工具资产中枢</span>
          </div>
          <h1 class="hero-title">教学工具配置</h1>
          <p class="hero-desc">
            统一纳管 AI 工具广场的展示元数据、路由跳转、推荐标记与上下架状态。配置变更将实时同步至用户端工具广场。
          </p>
        </div>
        <div class="hero-action-area">
          <el-button type="primary" round class="add-btn" :icon="Plus" @click="goCreate">
            新建工具
          </el-button>
          <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="handleManualRefresh">
            刷新
          </el-button>
          <el-button round class="preview-btn" :icon="Shop" @click="router.push('/ai/marketplace')">
            前往广场预览
          </el-button>
        </div>
      </div>

      <div class="metrics-grid" v-loading="loading">
        <div class="metric-card">
          <div class="metric-icon-box blue"><el-icon><Operation /></el-icon></div>
          <div class="metric-content">
            <div class="metric-label">纳管工具总数</div>
            <div class="metric-value">{{ stats?.total ?? toolList.length }} <span class="unit">个</span></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon-box green"><el-icon><CircleCheck /></el-icon></div>
          <div class="metric-content">
            <div class="metric-label">在线运行</div>
            <div class="metric-value text-green">{{ stats?.online ?? onlineCount }} <span class="unit">项</span></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon-box cyan"><el-icon><User /></el-icon></div>
          <div class="metric-content">
            <div class="metric-label">教师工具</div>
            <div class="metric-value text-cyan">{{ stats?.teacherCount ?? teacherCount }} <span class="unit">个</span></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon-box purple"><el-icon><DataAnalysis /></el-icon></div>
          <div class="metric-content">
            <div class="metric-label">累计调用</div>
            <div class="metric-value text-purple">{{ formatCount(stats?.totalUseCount ?? totalUseCount) }} <span class="unit">次</span></div>
          </div>
        </div>
      </div>
    </div>

    <div class="filter-bar-card">
      <div class="category-tabs">
        <button
          v-for="cat in categoryOptions"
          :key="cat.key"
          class="cat-tab-btn"
          :class="{ active: selectedCategory === cat.key }"
          @click="changeCategory(cat.key)"
        >
          <span class="tab-label">{{ cat.label }}</span>
          <span class="tab-badge">{{ getCategoryCount(cat.key) }}</span>
        </button>
      </div>

      <div class="filter-right-tools">
        <el-select v-model="selectedStatus" placeholder="全部状态" style="width: 110px" clearable @change="applyFilter">
          <el-option label="全部状态" :value="''" />
          <el-option label="在线" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索名称、标签、路由..."
          style="width: 220px"
          clearable
          :prefix-icon="Search"
          @keyup.enter="applyFilter"
          @clear="applyFilter"
        />
      </div>
    </div>

    <div v-loading="loading" class="tools-grid-wrap">
      <el-empty v-if="!loading && filteredTools.length === 0" description="暂无匹配的工具">
        <el-button type="primary" @click="goCreate">新建第一个工具</el-button>
      </el-empty>

      <div v-else class="tools-grid">
        <ToolAdminCard
          v-for="tool in filteredTools"
          :key="tool.id"
          :tool="tool"
          @edit="handleEdit"
          @publish="handlePublish"
          @offline="handleOffline"
          @delete="handleDelete"
          @preview="handlePreview"
          @toggle-rec="handleToggleRec"
          @toggle-hot="handleToggleHot"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Plus, Refresh, Search, Operation, CircleCheck, User, DataAnalysis, Shop
} from '@element-plus/icons-vue';
import ToolAdminCard from '@/components/system/ai-tool/ToolAdminCard.vue';
import { useAIToolManage } from '@/composables/system/useAIToolManage';
import type { AIToolAdminVO } from '@/types/system/tool';

const router = useRouter();
const {
  loading,
  toolList,
  stats,
  fetchTools,
  fetchStats,
  handlePublish: publish,
  handleOffline: offline,
  handleDelete: remove,
  handleToggleFlag
} = useAIToolManage();

const selectedCategory = ref('ALL');
const selectedStatus = ref<number | ''>('');
const searchKeyword = ref('');

const categoryOptions = [
  { key: 'ALL', label: '全部' },
  { key: 'TEACHER', label: '教师' },
  { key: 'STUDENT', label: '学生' },
  { key: 'GENERAL', label: '通用' },
  { key: 'RECOMMENDED', label: '推荐' }
];

const filteredTools = computed(() => {
  let list = [...toolList.value];
  if (selectedCategory.value !== 'ALL') {
    if (selectedCategory.value === 'RECOMMENDED') {
      list = list.filter(t => t.isRecommended);
    } else {
      list = list.filter(t => t.category === selectedCategory.value);
    }
  }
  if (selectedStatus.value !== '') {
    list = list.filter(t => t.status === selectedStatus.value);
  }
  const kw = searchKeyword.value.trim().toLowerCase();
  if (kw) {
    list = list.filter(t =>
      t.name.toLowerCase().includes(kw) ||
      (t.tags || '').toLowerCase().includes(kw) ||
      (t.route || '').toLowerCase().includes(kw)
    );
  }
  return list;
});

const onlineCount = computed(() => toolList.value.filter(t => t.status === 1).length);
const teacherCount = computed(() => toolList.value.filter(t => t.category === 'TEACHER').length);
const totalUseCount = computed(() => toolList.value.reduce((sum, t) => sum + (t.useCount || 0), 0));

function getCategoryCount(key: string) {
  if (key === 'ALL') return toolList.value.length;
  if (key === 'RECOMMENDED') return toolList.value.filter(t => t.isRecommended).length;
  return toolList.value.filter(t => t.category === key).length;
}

function formatCount(n: number) {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`;
  return String(n);
}

function changeCategory(key: string) {
  selectedCategory.value = key;
}

function applyFilter() {
  reload(false);
}

async function handleManualRefresh() {
  await reload(true);
}

async function reload(isManual = false) {
  try {
    await Promise.all([
      fetchTools({
        category: selectedCategory.value === 'ALL' ? undefined : selectedCategory.value,
        status: selectedStatus.value === '' ? undefined : selectedStatus.value,
        keyword: searchKeyword.value.trim() || undefined
      }),
      fetchStats(),
      // 保底微延时 220ms，与菜单管理保持 1:1 统一的顺滑旋转加载动效与清晰反馈
      new Promise((resolve) => setTimeout(resolve, 220))
    ]);
    if (isManual) {
      ElMessage.success('已刷新最新教学工具与统计指标');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '刷新工具数据失败');
  }
}

function goCreate() {
  router.push('/system/tools/edit');
}

function handleEdit(tool: AIToolAdminVO) {
  router.push(`/system/tools/edit/${tool.id}`);
}

async function handlePublish(tool: AIToolAdminVO) {
  if (await publish(tool.id)) {
    tool.status = 1;
    await reload();
  }
}

async function handleOffline(tool: AIToolAdminVO) {
  if (await offline(tool.id)) {
    tool.status = 0;
    await reload();
  }
}

async function handleDelete(tool: AIToolAdminVO) {
  if (await remove(tool.id)) {
    await reload();
  }
}

function handlePreview(tool: AIToolAdminVO) {
  if (tool.status === 1) {
    router.push('/ai/marketplace');
  } else {
    router.push('/system/tools/edit/' + tool.id);
  }
}

async function handleToggleRec(tool: AIToolAdminVO) {
  if (await handleToggleFlag(tool.id, 'isRecommended', !tool.isRecommended)) {
    tool.isRecommended = !tool.isRecommended;
  }
}

async function handleToggleHot(tool: AIToolAdminVO) {
  if (await handleToggleFlag(tool.id, 'isHot', !tool.isHot)) {
    tool.isHot = !tool.isHot;
  }
}

onMounted(reload);
</script>

<style scoped lang="scss">
.tool-list-page {
  padding: 0 4px 24px;
}

.tool-hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(135deg, #f8fbff 0%, #ffffff 55%, #f5f3ff 100%);
  border: 1px solid #e8edf5;
  padding: 28px 28px 20px;
  margin-bottom: 16px;
}

.hero-bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;

  &--blue {
    width: 280px;
    height: 280px;
    background: rgba(22, 119, 255, 0.12);
    top: -80px;
    right: 10%;
  }

  &--purple {
    width: 220px;
    height: 220px;
    background: rgba(139, 92, 246, 0.1);
    bottom: -60px;
    left: 5%;
  }
}

.hero-header-row {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.eyebrow-chip {
  background: #1677ff14;
  color: #1677ff;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 600;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.hero-desc {
  margin: 0;
  max-width: 640px;
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

.hero-action-area {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;

  .add-btn {
    border-radius: 9999px !important;
    font-weight: 600;
    font-size: 13px;
    height: 34px !important;
    padding: 0 18px !important;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%) !important;
    box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3) !important;
    border: none !important;
    color: #ffffff !important;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(22, 119, 255, 0.4) !important;
    }
  }

  .preview-btn {
    border-radius: 9999px !important;
    font-weight: 500;
    font-size: 13px;
    height: 34px !important;
    padding: 0 16px !important;
    background: #ffffff !important;
    border: 1px solid #e2e8f0 !important;
    color: #475569 !important;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03) !important;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s ease;

    &:hover {
      color: #1677ff !important;
      border-color: #93c5fd !important;
      background: #eff6ff !important;
      transform: translateY(-1px);
      box-shadow: 0 3px 8px rgba(37, 99, 235, 0.1) !important;
    }
  }

  // 统一 1:1 对齐菜单管理高定胶囊刷新按钮
  :deep(.btn-refresh) {
    height: 34px !important;
    padding: 0 16px !important;
    border-radius: 9999px !important;
    font-size: 13px !important;
  }
}

.metrics-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 16px;
  padding: 16px 18px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.02);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(22, 119, 255, 0.08);
    border-color: rgba(147, 197, 253, 0.7);
  }
}

.metric-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);

  &.blue { background: #eff6ff; color: #2563eb; }
  &.green { background: #ecfdf5; color: #059669; }
  &.cyan { background: #ecfeff; color: #0891b2; }
  &.purple { background: #f5f3ff; color: #7c3aed; }
}

.metric-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 2px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;

  .unit { font-size: 13px; font-weight: 500; color: #94a3b8; margin-left: 2px; }
  &.text-green { color: #059669; }
  &.text-cyan { color: #0891b2; }
  &.text-purple { color: #7c3aed; }
}

.filter-bar-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 16px;
  padding: 12px 18px;
  margin-bottom: 18px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.02);
}

.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cat-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  background: #ffffff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    color: #1677ff;
    border-color: #93c5fd;
    background: #f8fbff;
  }

  &.active {
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    border-color: transparent;
    color: #ffffff;
    box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

    .tab-badge { background: rgba(255,255,255,0.22); color: #ffffff; }
  }
}

.tab-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 9999px;
  background: #f1f5f9;
  color: #64748b;
  transition: all 0.2s;
}

.filter-right-tools {
  display: flex;
  gap: 10px;
  align-items: center;

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    border-radius: 9999px !important;
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e2e8f0 inset;

    &:hover {
      box-shadow: 0 0 0 1px #93c5fd inset;
    }

    &.is-focus {
      background: #ffffff;
      box-shadow: 0 0 0 1px #1677ff inset, 0 0 0 3px rgba(22, 119, 255, 0.12) !important;
    }
  }
}

.tools-grid-wrap {
  min-height: 240px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;
}

@media (max-width: 1100px) {
  .metrics-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .metrics-grid { grid-template-columns: 1fr; }
  .filter-bar-card { flex-direction: column; align-items: stretch; }
}
</style>
