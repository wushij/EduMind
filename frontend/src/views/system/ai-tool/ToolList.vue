<template>
  <div class="tool-list-page">
    <div class="tool-hero-card">
      <div class="hero-bg-glow hero-bg-glow--blue" />
      <div class="hero-bg-glow hero-bg-glow--purple" />

      <!-- 1. 顶部主栏：左侧精致徽标与标题描述，右侧操作坞 -->
      <div class="hero-header-row">
        <div class="hero-left-section">
          <div class="hero-icon-box">
            <el-icon><Tools /></el-icon>
          </div>
          <div class="hero-text-wrap">
            <div class="hero-title-line">
              <h1 class="hero-title">教学工具配置</h1>
              <span class="status-pill">
                <span class="status-dot" />
                <span>{{ stats?.online ?? onlineCount }} 项在线</span>
              </span>
            </div>
            <p class="hero-desc">
              统一纳管 AI 工具广场的展示元数据、模型路由、场景分类与上下架状态，变更实时同步至用户端。
            </p>
          </div>
        </div>

        <div class="hero-action-dock">
          <button
            type="button"
            class="action-btn action-btn--ghost"
            @click="router.push('/ai/marketplace')"
          >
            <el-icon><Shop /></el-icon>
            <span>前往广场预览</span>
          </button>
          <button
            type="button"
            class="action-btn action-btn--secondary"
            :disabled="loading"
            @click="handleManualRefresh"
          >
            <el-icon :class="{ 'is-loading': loading }"><Refresh /></el-icon>
            <span>刷新</span>
          </button>
          <button type="button" class="action-btn action-btn--primary" @click="goCreate">
            <el-icon><Plus /></el-icon>
            <span>新建工具</span>
          </button>
        </div>
      </div>

      <!-- 2. 精致指标概览网格 -->
      <div class="metrics-grid" v-loading="loading">
        <div class="metric-card metric-card--total">
          <div class="metric-icon-box blue">
            <el-icon><Operation /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-label">纳管工具总数</span>
            <div class="metric-num-row">
              <span class="metric-value">{{ stats?.total ?? toolList.length }}</span>
              <span class="metric-unit">个</span>
            </div>
          </div>
        </div>

        <div class="metric-card metric-card--online">
          <div class="metric-icon-box green">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-label">在线运行状态</span>
            <div class="metric-num-row">
              <span class="metric-value text-green">{{ stats?.online ?? onlineCount }}</span>
              <span class="metric-unit">项</span>
            </div>
          </div>
        </div>

        <div class="metric-card metric-card--teacher">
          <div class="metric-icon-box cyan">
            <el-icon><User /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-label">教师专属工具</span>
            <div class="metric-num-row">
              <span class="metric-value text-cyan">{{ stats?.teacherCount ?? teacherCount }}</span>
              <span class="metric-unit">个</span>
            </div>
          </div>
        </div>

        <div class="metric-card metric-card--usage">
          <div class="metric-icon-box purple">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-label">全平台累计调用</span>
            <div class="metric-num-row">
              <span class="metric-value text-purple">{{ formatCount(stats?.totalUseCount ?? totalUseCount) }}</span>
              <span class="metric-unit">次</span>
            </div>
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
  Plus, Refresh, Search, Operation, CircleCheck, User, DataAnalysis, Shop, Tools
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
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fbff 0%, #ffffff 55%, #f5f3ff 100%);
  border: 1px solid #e8edf5;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03), 0 6px 16px rgba(15, 23, 42, 0.02);
  padding: 22px 24px 20px;
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
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.hero-left-section {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 720px;

  .hero-icon-box {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #2563eb;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    flex-shrink: 0;
    border: 1px solid rgba(37, 99, 235, 0.15);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  }

  .hero-text-wrap {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .hero-title-line {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .hero-title {
        margin: 0;
        font-size: 22px;
        font-weight: 800;
        color: #0f172a;
        letter-spacing: -0.02em;
        line-height: 1.25;
      }

      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 2px 10px;
        border-radius: 9999px;
        background: #ecfdf5;
        border: 1px solid #a7f3d0;
        color: #065f46;
        font-size: 12px;
        font-weight: 600;

        .status-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #10b981;
          box-shadow: 0 0 6px rgba(16, 185, 129, 0.6);
        }
      }
    }

    .hero-desc {
      margin: 0;
      font-size: 13px;
      line-height: 1.5;
      color: #64748b;
    }
  }
}

.hero-action-dock {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;

  .action-btn {
    height: 36px;
    padding: 0 16px;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 1px solid transparent;

    &--primary {
      background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
      color: #ffffff;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.22);

      &:hover {
        background: linear-gradient(135deg, #0958d9 0%, #003eb3 100%);
        box-shadow: 0 6px 16px rgba(22, 119, 255, 0.32);
      }
    }

    &--secondary {
      background: #ffffff;
      border-color: #e2e8f0;
      color: #475569;

      &:hover:not(:disabled) {
        background: #f8fafc;
        border-color: #cbd5e1;
        color: #1e293b;
      }

      &:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }
    }

    &--ghost {
      background: #eff6ff;
      border-color: #dbeafe;
      color: #1d4ed8;

      &:hover {
        background: #dbeafe;
        border-color: #bfdbfe;
        color: #1e40af;
      }
    }
  }
}

.metrics-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 540px) {
    grid-template-columns: 1fr;
  }
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
  backdrop-filter: blur(8px);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  }
}

.metric-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;

  &.blue { background: #eff6ff; color: #2563eb; }
  &.green { background: #ecfdf5; color: #059669; }
  &.cyan { background: #ecfeff; color: #0891b2; }
  &.purple { background: #f5f3ff; color: #7c3aed; }
}

.metric-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.metric-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  white-space: nowrap;
}

.metric-num-row {
  display: flex;
  align-items: baseline;
  gap: 3px;

  .metric-value {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.2;
    letter-spacing: -0.02em;

    &.text-green { color: #059669; }
    &.text-cyan { color: #0891b2; }
    &.text-purple { color: #7c3aed; }
  }

  .metric-unit {
    font-size: 12px;
    font-weight: 500;
    color: #94a3b8;
  }
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
