<template>
  <div class="broadcast-page">
    <!-- 顶部标题与操作卡片 -->
    <div class="page-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <el-icon :size="24"><Promotion /></el-icon>
        </div>
        <div class="header-text">
          <div class="title-row">
            <h2>消息广播推送中心</h2>
            <el-tag size="small" effect="plain" round class="status-live-tag">
              <span class="live-dot" /> 实时全校触达
            </el-tag>
          </div>
          <p>支持面向全校师生或指定角色（教师/学生/管理员）定向下发系统广播、业务提醒与紧急公告</p>
        </div>
      </div>
      <div class="header-right">
        <el-button
          v-if="authStore.hasPermission('notice:broadcast:send')"
          type="danger"
          plain
          round
          :icon="Delete"
          class="header-btn-clear"
          @click="handleClear"
        >
          清空历史
        </el-button>
        <el-button
          v-if="authStore.hasPermission('notice:broadcast:send')"
          type="primary"
          round
          :icon="Promotion"
          class="header-btn-create"
          @click="drawerVisible = true"
        >
          发起广播
        </el-button>
      </div>
    </div>

    <!-- 统计指标卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon-wrapper stat-blue">
          <el-icon :size="22"><ChatDotSquare /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">历史广播任务</span>
          <div class="stat-number-wrap">
            <span class="stat-value">{{ stats.totalBroadcasts }}</span>
            <span class="stat-unit">批次</span>
          </div>
          <span class="stat-desc">全校累计下发记录</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper stat-purple">
          <el-icon :size="22"><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">累计受众触达</span>
          <div class="stat-number-wrap">
            <span class="stat-value">{{ stats.totalReach }}</span>
            <span class="stat-unit">人次</span>
          </div>
          <span class="stat-desc">系统覆盖接收人次</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper stat-emerald">
          <el-icon :size="22"><CircleCheckFilled /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">累计已读确认</span>
          <div class="stat-number-wrap">
            <span class="stat-value">{{ stats.totalRead }}</span>
            <span class="stat-unit">人次</span>
          </div>
          <span class="stat-desc">师生已阅回执统计</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper stat-amber">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">平均触达已读率</span>
          <div class="stat-number-wrap">
            <span class="stat-value">{{ Math.round(stats.avgReadRate || 0) }}%</span>
          </div>
          <div class="stat-progress-box">
            <el-progress
              :percentage="Math.min(Math.round(stats.avgReadRate || 0), 100)"
              :stroke-width="5"
              :show-text="false"
              color="#f59e0b"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="filter-card">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索广播标题或正文内容..."
          clearable
          :prefix-icon="Search"
          class="filter-search-input"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />

        <el-select
          v-model="targetTypeFilter"
          placeholder="全部受众"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option label="全部受众" value="" />
          <el-option label="全体用户" value="all" />
          <el-option label="按角色" value="role" />
        </el-select>

        <el-select
          v-model="priorityFilter"
          placeholder="全部级别"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option label="全部级别" value="" />
          <el-option label="普通广播" :value="0" />
          <el-option label="重要弹窗" :value="1" />
          <el-option label="紧急公告" :value="2" />
        </el-select>

        <el-button type="primary" round :icon="Search" class="btn-round-search" @click="handleSearch">
          查询
        </el-button>
        <el-button round :icon="Refresh" class="btn-round-reset" @click="handleReset">
          重置
        </el-button>
      </div>

      <div class="filter-right">
        <span class="total-badge">共找到 {{ filteredTableData.length }} 条广播</span>
      </div>
    </div>

    <!-- 数据表格卡片 -->
    <div v-loading="loading" class="table-card">
      <el-table :data="paginatedData" stripe class="broadcast-table">
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #default="{ row }">
            <span class="id-pill">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="消息内容" min-width="320">
          <template #default="{ row }">
            <div class="msg-column-wrap">
              <div class="msg-title-line">
                <el-tag
                  size="small"
                  round
                  :type="priorityTagType(row.priority)"
                  class="priority-tag"
                >
                  <span class="status-dot" :class="'dot-' + priorityTagType(row.priority)" />
                  {{ priorityLabel(row.priority) }}
                </el-tag>
                <span class="msg-title-text" :title="row.title">{{ row.title }}</span>
              </div>
              <p class="msg-snippet-text" :title="row.content">{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="受众对象" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small" round :type="audienceTagType(row.targetType, row.targetPayload)" class="audience-tag">
              <el-icon class="audience-icon"><component :is="audienceIcon(row.targetType, row.targetPayload)" /></el-icon>
              {{ audienceLabel(row.targetType, row.targetPayload) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="触达 / 已读" width="160" align="center">
          <template #default="{ row }">
            <div class="reach-cell-wrap">
              <div class="reach-meta-line">
                <span class="read-count">{{ row.readCount || 0 }}</span>
                <span class="slash">/</span>
                <span class="total-count">{{ row.totalCount || 0 }}</span>
                <span class="rate-badge">({{ calcPercent(row.readCount, row.totalCount) }}%)</span>
              </div>
              <el-progress
                :percentage="calcPercent(row.readCount, row.totalCount)"
                :stroke-width="5"
                :show-text="false"
                :color="getReachProgressColor(row.readCount, row.totalCount)"
                class="reach-progress-bar"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发送人" width="140" align="center">
          <template #default="{ row }">
            <div class="sender-cell">
              <el-avatar
                :size="26"
                :src="resolveSenderAvatar(row)"
                class="sender-avatar-img"
              >
                <el-icon :size="14"><UserFilled /></el-icon>
              </el-avatar>
              <span class="sender-name">{{ row.senderName || 'admin' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布时间" width="170" align="center">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon class="time-icon"><Clock /></el-icon>
              <span>{{ row.createTime ? formatDateTime(row.createTime) : '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-pill-group">
              <el-button
                size="small"
                round
                class="action-pill-btn action-pill-btn-detail"
                :icon="View"
                @click="openDetail(row)"
              >
                详情
              </el-button>
              <el-button
                v-if="authStore.hasPermission('notice:broadcast:send')"
                size="small"
                round
                class="action-pill-btn action-pill-btn-delete"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="filteredTableData.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 发起广播抽屉 -->
    <BroadcastCreateDrawer v-model:visible="drawerVisible" @sent="onSent" />

    <!-- 广播详情弹窗 -->
    <BroadcastDetailDialog v-model:visible="detailVisible" :broadcast="currentBroadcast" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Promotion,
  Delete,
  Search,
  Refresh,
  View,
  Clock,
  ChatDotSquare,
  UserFilled,
  CircleCheckFilled,
  DataAnalysis,
  User,
  School,
  Reading,
  Key
} from '@element-plus/icons-vue';
import {
  listBroadcasts,
  getBroadcastStats,
  deleteBroadcast,
  clearAllBroadcasts
} from '@/api/notification/broadcast';
import type { BroadcastStatsVO, NotificationBroadcastVO } from '@/types/notification/broadcast';
import { formatDateTime } from '@/utils/format/date';
import { useAuthStore } from '@/stores/auth/auth';
import BroadcastCreateDrawer from '@/components/notification/BroadcastCreateDrawer.vue';
import BroadcastDetailDialog from '@/components/notification/BroadcastDetailDialog.vue';

const authStore = useAuthStore();

const loading = ref(false);
const rawTableData = ref<NotificationBroadcastVO[]>([]);
const searchKeyword = ref('');
const targetTypeFilter = ref('');
const priorityFilter = ref<number | ''>('');
const drawerVisible = ref(false);
const detailVisible = ref(false);
const currentBroadcast = ref<NotificationBroadcastVO | null>(null);

const query = reactive({
  pageNum: 1,
  pageSize: 10
});

const stats = ref<BroadcastStatsVO>({
  totalBroadcasts: 0,
  totalReach: 0,
  totalRead: 0,
  avgReadRate: 0
});

async function loadStats() {
  try {
    const res = await getBroadcastStats();
    if (res.data) stats.value = res.data;
  } catch {
    /* ignore */
  }
}

async function fetchData() {
  loading.value = true;
  try {
    const res = await listBroadcasts({
      page: 1,
      pageSize: 100, // 获取全量数据以支持前端实时关键词与多条件极速筛选
      targetType: targetTypeFilter.value || undefined
    });
    rawTableData.value = res.data?.list || [];
  } finally {
    loading.value = false;
  }
}

// 关键词与优先级前端组合筛选
const filteredTableData = computed(() => {
  let list = rawTableData.value;
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase();
    list = list.filter(
      (item) =>
        (item.title && item.title.toLowerCase().includes(kw)) ||
        (item.content && item.content.toLowerCase().includes(kw)) ||
        (item.senderName && item.senderName.toLowerCase().includes(kw))
    );
  }
  if (priorityFilter.value !== '') {
    list = list.filter((item) => (item.priority ?? 0) === Number(priorityFilter.value));
  }
  return list;
});

// 分页切片数据
const paginatedData = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize;
  return filteredTableData.value.slice(start, start + query.pageSize);
});

function handleSearch() {
  query.pageNum = 1;
  fetchData();
}

function handleReset() {
  searchKeyword.value = '';
  targetTypeFilter.value = '';
  priorityFilter.value = '';
  query.pageNum = 1;
  query.pageSize = 10;
  fetchData();
}

function handleSizeChange(size: number) {
  query.pageSize = size;
  query.pageNum = 1;
}

function handlePageChange(page: number) {
  query.pageNum = page;
}

function onSent() {
  loadStats();
  handleSearch();
}

function openDetail(row: NotificationBroadcastVO) {
  currentBroadcast.value = row;
  detailVisible.value = true;
}

function priorityLabel(p: number) {
  if (p === 2) return '紧急公告';
  if (p === 1) return '重要弹窗';
  return '普通广播';
}

function priorityTagType(p: number): 'danger' | 'warning' | 'info' {
  if (p === 2) return 'danger';
  if (p === 1) return 'warning';
  return 'info';
}

function audienceLabel(type: string, payload?: string) {
  if (type === 'all') return '全体用户';
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[payload || ''] || '指定角色';
}

function audienceTagType(type: string, payload?: string): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'all') return 'primary';
  if (payload === 'TEACHER') return 'success';
  if (payload === 'STUDENT') return 'warning';
  return 'info';
}

function audienceIcon(type: string, payload?: string) {
  if (type === 'all') return User;
  if (payload === 'TEACHER') return School;
  if (payload === 'STUDENT') return Reading;
  return Key;
}

function calcPercent(read?: number, total?: number) {
  if (!total || total <= 0) return 0;
  return Math.min(Math.round(((read || 0) / total) * 100), 100);
}

function getReachProgressColor(read?: number, total?: number) {
  const pct = calcPercent(read, total);
  if (pct >= 80) return '#10b981';
  if (pct >= 40) return '#3b82f6';
  return '#f59e0b';
}

function resolveSenderAvatar(row: NotificationBroadcastVO): string {
  if (row.senderAvatar) {
    return row.senderAvatar;
  }
  if (
    authStore.currentUser &&
    (row.senderId === authStore.currentUser.id ||
      (row.senderName && row.senderName === authStore.currentUser.username))
  ) {
    return authStore.currentUser.avatar || '';
  }
  return '';
}


async function handleDelete(row: NotificationBroadcastVO) {
  try {
    await ElMessageBox.confirm(`确定删除广播「${row.title}」？关联用户通知将同步移除。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    });
    await deleteBroadcast(row.id);
    ElMessage.success('已成功删除广播');
    fetchData();
    loadStats();
  } catch {
    /* cancel */
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定清空全部历史广播记录？此操作不可恢复。', '高危操作确认', {
      type: 'warning',
      confirmButtonText: '确认清空',
      cancelButtonText: '取消'
    });
    await clearAllBroadcasts();
    ElMessage.success('已清空全部广播记录');
    fetchData();
    loadStats();
  } catch {
    /* cancel */
  }
}

onMounted(() => {
  loadStats();
  fetchData();
});
</script>

<style scoped lang="scss">
.broadcast-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  min-height: calc(100vh - 64px);
  background: #f8fafc;
}

/* 顶部卡片 */
.page-header-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  padding: 22px 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px -2px rgba(15, 23, 42, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px -4px rgba(37, 99, 235, 0.35);
  flex-shrink: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .title-row {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: -0.01em;
    }
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
  }
}

.status-live-tag {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
  font-weight: 500;
  font-size: 12px;
}

.live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
  margin-right: 4px;
  box-shadow: 0 0 6px #2563eb;
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.2); }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-btn-clear {
  border-radius: 9999px !important;
  padding: 9px 18px;
  font-size: 13px;
  transition: all 0.2s ease;
}

.header-btn-create {
  border-radius: 9999px !important;
  padding: 9px 20px;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
  }
}

/* 统计卡片网格 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px -3px rgba(0, 0, 0, 0.06);
    border-color: #cbd5e1;
  }
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.stat-blue {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #2563eb;
  }
  &.stat-purple {
    background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
    color: #7c3aed;
  }
  &.stat-emerald {
    background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
    color: #059669;
  }
  &.stat-amber {
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
    color: #d97706;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.stat-number-wrap {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
  font-feature-settings: 'tnum';
}

.stat-unit {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.stat-desc {
  font-size: 11px;
  color: #94a3b8;
}

.stat-progress-box {
  margin-top: 4px;
}

/* 筛选工具栏 */
.filter-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-search-input {
  width: 250px;
}

.btn-round-search {
  border-radius: 9999px !important;
  padding: 8px 18px;
}

.btn-round-reset {
  border-radius: 9999px !important;
  padding: 8px 18px;
}

.total-badge {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

/* 表格卡片 */
.table-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 16px 20px 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.broadcast-table {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-table__header) th {
    background: #f8fafc;
    color: #475569;
    font-weight: 600;
    font-size: 13px;
  }
}

.id-pill {
  display: inline-block;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
}

.msg-column-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
}

.msg-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.priority-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  padding: 1px 9px;
  border-radius: 9999px;
  flex-shrink: 0;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;

  &.dot-danger {
    background: #ef4444;
    box-shadow: 0 0 5px rgba(239, 68, 68, 0.8);
  }
  &.dot-warning {
    background: #f59e0b;
    box-shadow: 0 0 5px rgba(245, 158, 11, 0.8);
  }
  &.dot-info {
    background: #94a3b8;
  }
}

.msg-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-snippet-text {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 420px;
}

.audience-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-weight: 500;
}

.reach-cell-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.reach-meta-line {
  font-size: 12px;
  color: #475569;
  font-feature-settings: 'tnum';

  .read-count {
    font-weight: 700;
    color: #0f172a;
  }
  .slash {
    margin: 0 3px;
    color: #cbd5e1;
  }
  .total-count {
    color: #64748b;
  }
  .rate-badge {
    margin-left: 4px;
    color: #059669;
    font-weight: 600;
    font-size: 11px;
  }
}

.reach-progress-bar {
  width: 100%;
}

.sender-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.sender-avatar-img {
  background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
  color: #ffffff;
  border: 1.5px solid #e0e7ff;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
  flex-shrink: 0;

  :deep(img) {
    object-fit: cover;
    width: 100%;
    height: 100%;
  }
}

.sender-name {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.time-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;

  .time-icon {
    font-size: 13px;
    color: #94a3b8;
  }
}

/* 操作长圆按钮组 */
.action-pill-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-pill-btn {
  border-radius: 9999px !important;
  padding: 4px 13px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  height: 28px !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.action-pill-btn-detail {
  background: #f0f7ff !important;
  border: 1px solid #bfdbfe !important;
  color: #1d4ed8 !important;

  &:hover {
    background: #2563eb !important;
    border-color: #2563eb !important;
    color: #ffffff !important;
    box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3) !important;
    transform: translateY(-1px);
  }
}

.action-pill-btn-delete {
  background: #fef2f2 !important;
  border: 1px solid #fecaca !important;
  color: #dc2626 !important;

  &:hover {
    background: #ef4444 !important;
    border-color: #ef4444 !important;
    color: #ffffff !important;
    box-shadow: 0 4px 10px rgba(239, 68, 68, 0.3) !important;
    transform: translateY(-1px);
  }
}

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1080px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
