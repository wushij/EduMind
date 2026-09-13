<template>
  <div class="broadcast-page">
    <div class="page-header-card">
      <div class="header-left">
        <h2>消息广播推送</h2>
        <p>向全体用户或指定角色（教师/学生/管理员）发送系统广播</p>
      </div>
      <div class="header-right">
        <el-button
          v-if="authStore.hasPermission('notice:broadcast:send')"
          type="danger"
          plain
          @click="handleClear"
        >
          清空历史
        </el-button>
        <el-button
          v-if="authStore.hasPermission('notice:broadcast:send')"
          type="primary"
          @click="drawerVisible = true"
        >
          <el-icon><Promotion /></el-icon>
          发起广播
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">历史广播</span>
        <span class="stat-value">{{ stats.totalBroadcasts }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">累计触达</span>
        <span class="stat-value">{{ stats.totalReach }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">累计已读</span>
        <span class="stat-value">{{ stats.totalRead }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">平均已读率</span>
        <span class="stat-value">{{ Math.round(stats.avgReadRate || 0) }}%</span>
      </div>
    </div>

    <div class="filter-card">
      <el-select
        v-model="targetTypeFilter"
        placeholder="全部受众"
        clearable
        style="width: 180px"
        @change="handleSearch"
      >
        <el-option label="全体用户" value="all" />
        <el-option label="按角色" value="role" />
      </el-select>
      <el-button type="primary" plain @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading" class="table-card">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="消息" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag size="small" :type="priorityTagType(row.priority)">
                {{ priorityLabel(row.priority) }}
              </el-tag>
              <strong>{{ row.title }}</strong>
              <p class="snippet">{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="受众" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" round>{{ audienceLabel(row.targetType, row.targetPayload) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触达/已读" width="140" align="center">
          <template #default="{ row }">
            {{ row.readCount }} / {{ row.totalCount }}
          </template>
        </el-table-column>
        <el-table-column prop="senderName" label="发送人" width="110" align="center" />
        <el-table-column label="时间" width="170" align="center">
          <template #default="{ row }">
            {{ row.createTime ? formatDateTime(row.createTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="authStore.hasPermission('notice:broadcast:send')"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </div>

    <BroadcastCreateDrawer v-model:visible="drawerVisible" @sent="onSent" />
    <BroadcastDetailDialog v-model:visible="detailVisible" :broadcast="currentBroadcast" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Promotion } from '@element-plus/icons-vue';
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
const tableData = ref<NotificationBroadcastVO[]>([]);
const total = ref(0);
const targetTypeFilter = ref('');
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
      page: query.pageNum,
      pageSize: query.pageSize,
      targetType: targetTypeFilter.value || undefined
    });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchData();
}

function handleReset() {
  targetTypeFilter.value = '';
  query.pageNum = 1;
  query.pageSize = 10;
  fetchData();
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
  if (p === 1) return '弹窗';
  if (p === 2) return '跑马灯';
  return '普通';
}

function priorityTagType(p: number) {
  if (p === 1) return 'warning';
  if (p === 2) return 'danger';
  return 'info';
}

function audienceLabel(type: string, payload?: string) {
  if (type === 'all') return '全体';
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[payload || ''] || '角色';
}

async function handleDelete(row: NotificationBroadcastVO) {
  try {
    await ElMessageBox.confirm(`确定删除广播「${row.title}」？关联用户通知将同步删除。`, '删除确认', {
      type: 'warning'
    });
    await deleteBroadcast(row.id);
    ElMessage.success('已删除');
    fetchData();
    loadStats();
  } catch {
    /* cancel */
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定清空全部广播记录？此操作不可恢复。', '高危操作', {
      type: 'warning',
      confirmButtonText: '确认清空'
    });
    await clearAllBroadcasts();
    ElMessage.success('已清空');
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

.page-header-card,
.filter-card,
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e2e8f0;
}

.page-header-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;

  h2 {
    margin: 0 0 6px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
  }
}

.header-right {
  display: flex;
  gap: 10px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.filter-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    font-size: 14px;
  }

  .snippet {
    margin: 0;
    font-size: 12px;
    color: #64748b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 360px;
  }
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
