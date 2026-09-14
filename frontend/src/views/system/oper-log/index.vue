<template>
  <div class="oper-log-container">
    <!-- 1. 顶部指标概览 Hero 看板（长圆边框与拟态质感） -->
    <div class="metrics-hero-grid">
      <div class="metric-capsule-card">
        <div class="capsule-icon icon-blue">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="capsule-content">
          <span class="capsule-label">累计操作总数</span>
          <span class="capsule-val">{{ stats.totalCount.toLocaleString() }} <span class="unit">条</span></span>
          <span class="capsule-hint text-blue">系统操作全生命周期记录</span>
        </div>
      </div>

      <div class="metric-capsule-card">
        <div class="capsule-icon icon-purple">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="capsule-content">
          <span class="capsule-label">今日操作记录</span>
          <span class="capsule-val text-purple">{{ stats.todayCount.toLocaleString() }} <span class="unit">次</span></span>
          <span class="capsule-hint">实时业务事件流审计</span>
        </div>
      </div>

      <div class="metric-capsule-card">
        <div class="capsule-icon icon-emerald">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="capsule-content">
          <span class="capsule-label">操作执行成功率</span>
          <span class="capsule-val text-emerald">{{ stats.successRate }} <span class="unit">%</span></span>
          <span class="capsule-hint" :class="stats.errorCount > 0 ? 'text-danger' : 'text-emerald'">
            {{ stats.errorCount > 0 ? `当前发现 ${stats.errorCount} 次异常拦截` : '全链路调用健康稳定' }}
          </span>
        </div>
      </div>

      <div class="metric-capsule-card">
        <div class="capsule-icon icon-amber">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="capsule-content">
          <span class="capsule-label">平均响应耗时</span>
          <span class="capsule-val text-amber">{{ stats.avgCostTime }} <span class="unit">ms</span></span>
          <span class="capsule-hint">接口端到端执行效率监控</span>
        </div>
      </div>
    </div>

    <!-- 2. 筛选过滤栏（纯正长圆跑道边框控件） -->
    <div class="filter-capsule-card">
      <div class="filter-row">
        <div class="filter-inputs">
          <el-input
            v-model="queryParams.title"
            placeholder="搜索操作模块/标题..."
            clearable
            class="pill-input"
            :prefix-icon="Search"
            @keyup.enter="handleQuery"
            @clear="handleQuery"
          />
          <el-input
            v-model="queryParams.operName"
            placeholder="搜索操作人员姓名/账号..."
            clearable
            class="pill-input"
            :prefix-icon="Search"
            @keyup.enter="handleQuery"
            @clear="handleQuery"
          />
          <el-select
            v-model="queryParams.businessType"
            placeholder="业务类型"
            clearable
            class="pill-select"
            @change="handleQuery"
          >
            <el-option label="全部类型" :value="null" />
            <el-option label="新增 (INSERT)" :value="1" />
            <el-option label="修改 (UPDATE)" :value="2" />
            <el-option label="删除 (DELETE)" :value="3" />
            <el-option label="授权/变更 (GRANT)" :value="7" />
            <el-option label="导出 (EXPORT)" :value="5" />
            <el-option label="导入 (IMPORT)" :value="6" />
            <el-option label="清空 (CLEAN)" :value="8" />
            <el-option label="其它 (OTHER)" :value="0" />
          </el-select>
          <el-select
            v-model="queryParams.status"
            placeholder="执行状态"
            clearable
            class="pill-select"
            @change="handleQuery"
          >
            <el-option label="全部状态" :value="null" />
            <el-option label="正常成功 (0)" :value="0" />
            <el-option label="异常拦截 (1)" :value="1" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="dateRangeShortcuts"
            class="pill-date-picker"
            @change="handleDateRangeChange"
          />
        </div>

        <div class="filter-actions">
          <button type="button" class="pill-btn pill-btn--primary" @click="handleQuery">
            <el-icon class="mr-1"><Search /></el-icon>
            <span>检索</span>
          </button>
          <button type="button" class="pill-btn pill-btn--default" @click="resetQuery">
            <el-icon class="mr-1"><Refresh /></el-icon>
            <span>重置</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. 操作日志主表格卡片 -->
    <div v-loading="loading" class="log-table-card">
      <div class="table-toolbar-bar">
        <div class="toolbar-left">
          <span class="table-title">操作日志列表</span>
          <span class="table-badge-count">共 {{ total }} 条记录</span>
        </div>
        <div class="toolbar-right">
          <button
            v-if="selectedRowIds.length > 0"
            type="button"
            class="table-action-pill table-action-pill--danger"
            @click="handleBatchDelete"
          >
            <el-icon class="mr-1"><Delete /></el-icon>
            <span>批量删除 ({{ selectedRowIds.length }})</span>
          </button>
          <button
            type="button"
            class="table-action-pill table-action-pill--primary"
            @click="exportCsv"
          >
            <el-icon class="mr-1"><Download /></el-icon>
            <span>导出日志</span>
          </button>
          <button
            type="button"
            class="table-action-pill table-action-pill--danger"
            @click="handleClean"
          >
            <el-icon class="mr-1"><Delete /></el-icon>
            <span>清空所有日志</span>
          </button>
        </div>
      </div>

      <el-table
        :data="tableData"
        stripe
        class="custom-oper-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="模块名称" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="module-title-tag">{{ row.title || '系统操作' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作行为摘要" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="summary-cell">
              <span class="summary-text">{{ getActionSummary(row) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="业务类型" width="110" align="center">
          <template #default="{ row }">
            <span :class="['pill-tag', `pill-tag--biz-${row.businessType ?? 0}`]">
              {{ businessTypeLabel(row.businessType) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="requestMethod" label="方式" width="80" align="center">
          <template #default="{ row }">
            <span class="method-badge" :class="getMethodBadgeClass(row.requestMethod)">
              {{ row.requestMethod || 'POST' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作人员" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="oper-user-cell">
              <span class="user-name">{{ row.operName || ('用户#' + (row.operUserId || '0')) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="operIp" label="客户端 IP" width="130" align="center">
          <template #default="{ row }">
            <span class="ip-code">{{ row.operIp || '127.0.0.1' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="执行状态" width="115" align="center">
          <template #default="{ row }">
            <span :class="['pill-tag', row.status === 0 ? 'pill-tag--success' : 'pill-tag--danger']">
              <span class="dot"></span>
              {{ row.status === 0 ? '正常执行' : '异常拦截' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="耗时" width="95" align="center">
          <template #default="{ row }">
            <span :class="['cost-time-badge', row.costTime > 1000 ? 'text-amber' : 'text-slate-600']">
              {{ row.costTime }}ms
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="operTime" label="操作时间" width="170" align="center" />

        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-pill-group">
              <button
                type="button"
                class="table-action-pill table-action-pill--primary"
                @click="openDetail(row)"
              >
                详情
              </button>
              <button
                type="button"
                class="table-action-pill table-action-pill--danger"
                @click="handleDelete(row)"
              >
                删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 底部分页栏（严格左对齐项目规范） -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="custom-pagination"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 4. 日志详细信息长圆对话框（高度对齐 E:\wu-admin，升级视觉体验） -->
    <el-dialog
      v-model="detailVisible"
      title="操作日志详细审计档案"
      width="780px"
      destroy-on-close
      class="custom-oper-detail-dialog"
    >
      <div v-if="detail" class="detail-dialog-body">
        <!-- 顶部操作状态 Alert 警报条 -->
        <div :class="['detail-status-banner', detail.status === 0 ? 'banner--success' : 'banner--danger']">
          <el-icon class="banner-icon">
            <CircleCheck v-if="detail.status === 0" />
            <CircleClose v-else />
          </el-icon>
          <div class="banner-content">
            <div class="banner-title">
              {{ parsedAction || `${businessTypeLabel(detail.businessType)}${detail.title || ''}` }}
            </div>
            <div class="banner-sub">
              操作人：{{ detail.operName }} | 客户端 IP：{{ detail.operIp }} | 耗时：{{ detail.costTime }}ms | 时间：{{ detail.operTime }}
            </div>
          </div>
        </div>

        <!-- 字段变更明细（若有） -->
        <div v-if="parsedDiffItems.length > 0" class="diff-section">
          <div class="section-label">
            <el-icon class="mr-1"><InfoFilled /></el-icon>
            <span>属性值变更明细（字段变动跟踪）</span>
          </div>
          <div class="diff-tags-cloud">
            <span v-for="(item, idx) in parsedDiffItems" :key="idx" class="diff-pill-tag">
              {{ item }}
            </span>
          </div>
        </div>

        <!-- 异常堆栈信息（若有） -->
        <div v-if="detail.errorMsg" class="error-section">
          <div class="section-label text-danger">
            <el-icon class="mr-1"><Warning /></el-icon>
            <span>异常拦截错误信息</span>
          </div>
          <div class="error-box">
            {{ detail.errorMsg }}
          </div>
        </div>

        <!-- 基础元数据网格 -->
        <div class="meta-grid">
          <div class="meta-item">
            <span class="meta-k">模块名称</span>
            <span class="meta-v">{{ detail.title }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-k">业务类型</span>
            <span class="meta-v">{{ businessTypeLabel(detail.businessType) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-k">请求方式</span>
            <span class="meta-v">{{ detail.requestMethod }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-k">响应耗时</span>
            <span class="meta-v text-amber font-semibold">{{ detail.costTime }} ms</span>
          </div>
          <div class="meta-item full-span">
            <span class="meta-k">请求地址</span>
            <span class="meta-v code-text">{{ detail.operUrl }}</span>
          </div>
          <div class="meta-item full-span">
            <span class="meta-k">执行方法</span>
            <span class="meta-v code-text">{{ detail.method }}</span>
          </div>
        </div>

        <!-- 请求入参与出参代码预览卡片 -->
        <div class="payload-cards-grid">
          <!-- 请求入参 -->
          <div class="payload-box">
            <div class="payload-header">
              <span class="box-title">请求参数 (Request Payload)</span>
              <button
                v-if="detail.operParam"
                type="button"
                class="copy-pill-btn"
                @click="copyText(formatJson(detail.operParam))"
              >
                <el-icon class="mr-1"><DocumentCopy /></el-icon>
                <span>复制入参</span>
              </button>
            </div>
            <pre class="json-code-block">{{ formatJson(detail.operParam) || '无请求参数' }}</pre>
          </div>

          <!-- 返回结果 -->
          <div class="payload-box">
            <div class="payload-header">
              <span class="box-title">返回响应 (Response Result)</span>
              <button
                v-if="detail.jsonResult"
                type="button"
                class="copy-pill-btn"
                @click="copyText(formatJson(detail.jsonResult))"
              >
                <el-icon class="mr-1"><DocumentCopy /></el-icon>
                <span>复制出参</span>
              </button>
            </div>
            <pre class="json-code-block">{{ formatJson(detail.jsonResult) || '无返回出参' }}</pre>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <button type="button" class="pill-btn pill-btn--primary" @click="detailVisible = false">
            关闭详情
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import {
  Search,
  Refresh,
  Delete,
  Download,
  DocumentCopy,
  Clock,
  DataLine,
  CircleCheck,
  CircleClose,
  Timer,
  InfoFilled,
  Warning
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  pageOperLog,
  getOperLogStats,
  deleteOperLog,
  batchDeleteOperLog,
  cleanOperLog
} from '@/api/system/oper-log';
import type { OperLogVO, OperLogPageQuery, OperLogStatsVO } from '@/types/system/oper-log';

const loading = ref(false);
const total = ref(0);
const tableData = ref<OperLogVO[]>([]);
const selectedRowIds = ref<number[]>([]);
const detailVisible = ref(false);
const detail = ref<Partial<OperLogVO>>({});
function formatLocalDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function buildRecentDateRange(days: number): [string, string] {
  const end = new Date();
  end.setHours(0, 0, 0, 0);
  const start = new Date(end);
  start.setDate(start.getDate() - (days - 1));
  return [formatLocalDate(start), formatLocalDate(end)];
}

const dateRangeShortcuts = [
  {
    text: '今天',
    value: () => {
      const today = formatLocalDate(new Date());
      return [today, today] as [string, string];
    }
  },
  {
    text: '近7天',
    value: () => buildRecentDateRange(7)
  },
  {
    text: '近30天',
    value: () => buildRecentDateRange(30)
  }
];

const dateRange = ref<[string, string] | null>(buildRecentDateRange(7));

const stats = reactive<OperLogStatsVO>({
  totalCount: 0,
  todayCount: 0,
  successRate: 100,
  errorCount: 0,
  avgCostTime: 0
});

const queryParams = reactive<OperLogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  title: '',
  operName: '',
  businessType: null,
  status: null,
  startTime: undefined,
  endTime: undefined
});

function syncDateRangeToQuery(range: [string, string] | null) {
  if (range && range.length === 2) {
    queryParams.startTime = range[0] + ' 00:00:00';
    queryParams.endTime = range[1] + ' 23:59:59';
  } else {
    queryParams.startTime = undefined;
    queryParams.endTime = undefined;
  }
}

syncDateRangeToQuery(dateRange.value);

const businessTypeMap: Record<number, string> = {
  0: '其它',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '查询',
  5: '导出',
  6: '导入',
  7: '授权/变更',
  8: '清空'
};

function businessTypeLabel(type: number | undefined): string {
  if (type == null) return '其它';
  return businessTypeMap[type] ?? '其它';
}

function getMethodBadgeClass(method: string | undefined): string {
  const m = (method || '').toUpperCase();
  if (m === 'GET') return 'method--get';
  if (m === 'POST') return 'method--post';
  if (m === 'PUT') return 'method--put';
  if (m === 'DELETE') return 'method--delete';
  return 'method--other';
}

/**
 * 智能解析并提炼操作摘要（对标 E:\wu-admin getActionSummary）
 */
function getActionSummary(row: OperLogVO): string {
  const cleanTitle = (t: string | undefined) => (t ? (t.endsWith('管理') ? t.slice(0, -2) : t) : '');
  const t = cleanTitle(row.title);

  if (!row.operParam) {
    if (t) {
      const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType);
      return `${typeLabel}${t}`;
    }
    return '常规业务处理';
  }

  try {
    const obj = JSON.parse(row.operParam);
    let name = '';
    if (obj && typeof obj === 'object') {
      if (obj.action) {
        return String(obj.action);
      }
      const params = obj.params || obj;
      if (Array.isArray(params)) {
        name = 'ID: ' + params.slice(0, 3).join(', ') + (params.length > 3 ? ' 等' : '');
      } else if (params && typeof params === 'object') {
        name =
          params.username ||
          params.realName ||
          params.nickname ||
          params.name ||
          params.title ||
          params.courseName ||
          params.configName ||
          params.roleName ||
          params.id ||
          '';
        if (typeof name === 'object') {
          name = '';
        }
      } else if (params != null && typeof params !== 'function') {
        name = String(params);
      }
    }
    const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType);
    if (t) {
      return `${typeLabel}${t}${name ? `「${name}」` : ''}`;
    }
  } catch {}

  if (t) {
    const typeLabel = row.businessType === 0 ? '' : businessTypeLabel(row.businessType);
    return `${typeLabel}${t}`;
  }
  return '常规业务处理';
}

const parsedAction = computed(() => {
  if (!detail.value) return '';
  return getActionSummary(detail.value as OperLogVO);
});

const parsedDiffItems = computed<string[]>(() => {
  if (!detail.value?.operParam) return [];
  try {
    const obj = JSON.parse(detail.value.operParam);
    if (obj && typeof obj === 'object' && Array.isArray(obj.diffItems)) {
      return obj.diffItems.map(String);
    }
  } catch {}
  return [];
});

function formatJson(str: string | undefined): string {
  if (!str) return '';
  try {
    const obj = JSON.parse(str);
    if (obj && typeof obj === 'object' && 'params' in obj) {
      return JSON.stringify(obj.params, null, 2);
    }
    return JSON.stringify(obj, null, 2);
  } catch {
    return str;
  }
}

function copyText(text: string) {
  if (!text) return;
  navigator.clipboard.writeText(text).then(
    () => ElMessage.success('已复制到剪贴板'),
    () => ElMessage.error('复制失败')
  );
}

function handleDateRangeChange(range: [string, string] | null) {
  syncDateRangeToQuery(range);
}

async function loadStats() {
  try {
    const res = await getOperLogStats();
    if (res.data) {
      stats.totalCount = res.data.totalCount || 0;
      stats.todayCount = res.data.todayCount || 0;
      stats.successRate = res.data.successRate ?? 100;
      stats.errorCount = res.data.errorCount || 0;
      stats.avgCostTime = res.data.avgCostTime || 0;
    }
  } catch {}
}

async function loadData() {
  loading.value = true;
  try {
    const res = await pageOperLog({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      title: queryParams.title || undefined,
      operName: queryParams.operName || undefined,
      businessType: queryParams.businessType,
      status: queryParams.status,
      startTime: queryParams.startTime,
      endTime: queryParams.endTime
    });
    tableData.value = res.data?.list || [];
    total.value = Number(res.data?.total) || 0;
  } catch (err: any) {
    ElMessage.error(err?.message || '加载操作日志失败');
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNo = 1;
  loadData();
  loadStats();
}

function resetQuery() {
  queryParams.title = '';
  queryParams.operName = '';
  queryParams.businessType = null;
  queryParams.status = null;
  dateRange.value = buildRecentDateRange(7);
  syncDateRangeToQuery(dateRange.value);
  handleQuery();
}

function handleSelectionChange(rows: OperLogVO[]) {
  selectedRowIds.value = rows.map((r) => r.id);
}

function openDetail(row: OperLogVO) {
  detail.value = { ...row };
  detailVisible.value = true;
}

async function handleDelete(row: OperLogVO) {
  try {
    await ElMessageBox.confirm(`确定要删除 ID 为 #${row.id} 的操作日志记录吗？`, '删除日志确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteOperLog(row.id);
    ElMessage.success('操作日志已成功删除');
    loadData();
    loadStats();
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.response?.data?.message || err?.message || '删除日志失败，请检查操作权限');
    }
  }
}

async function handleBatchDelete() {
  if (selectedRowIds.value.length === 0) return;
  try {
    await ElMessageBox.confirm(
      `确定要批量删除选中的 ${selectedRowIds.value.length} 条操作日志吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await batchDeleteOperLog(selectedRowIds.value);
    ElMessage.success(`已成功批量删除 ${selectedRowIds.value.length} 条日志`);
    selectedRowIds.value = [];
    loadData();
    loadStats();
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.response?.data?.message || err?.message || '批量删除日志失败，请检查操作权限');
    }
  }
}

async function handleClean() {
  try {
    await ElMessageBox.confirm(
      '确定要清空平台当前租户下的全部业务操作日志吗？此操作不可逆！',
      '清空操作日志警告',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await cleanOperLog();
    ElMessage.success('当前租户操作日志已全部清空');
    loadData();
    loadStats();
  } catch (err: any) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.response?.data?.message || err?.message || '清空日志失败，请检查操作权限');
    }
  }
}

function exportCsv() {
  if (tableData.value.length === 0) {
    ElMessage.warning('当前无数据可导出');
    return;
  }
  const headers = ['ID', '模块', '业务类型', '操作摘要', '方式', '操作人员', '客户端IP', '状态', '耗时(ms)', '操作时间'];
  const rows = tableData.value.map((r) => [
    r.id,
    `"${(r.title || '').replace(/"/g, '""')}"`,
    `"${businessTypeLabel(r.businessType)}"`,
    `"${getActionSummary(r).replace(/"/g, '""')}"`,
    r.requestMethod || 'POST',
    `"${(r.operName || '').replace(/"/g, '""')}"`,
    r.operIp || '',
    r.status === 0 ? '成功' : '失败',
    r.costTime || 0,
    r.operTime || ''
  ]);
  const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `EduMind_OperLog_${new Date().toISOString().slice(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ElMessage.success('操作日志已成功导出');
}

onMounted(() => {
  loadStats();
  loadData();
});
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.oper-log-container {
  padding: 0 0 $page-bottom-spacing 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 1. 顶部 Hero 指标卡片（长圆跑道拟态风格） */
.metrics-hero-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .metric-capsule-card {
    background: #ffffff;
    border-radius: $border-radius-xl;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    border: 1px solid rgba(226, 232, 240, 0.8);
    box-shadow: $shadow-sm;
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-hover;
      border-color: rgba(22, 119, 255, 0.2);
    }

    .capsule-icon {
      width: 52px;
      height: 52px;
      border-radius: 9999px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;

      &.icon-blue {
        background: #eff6ff;
        color: #2563eb;
      }
      &.icon-purple {
        background: #faf5ff;
        color: #9333ea;
      }
      &.icon-emerald {
        background: #ecfdf5;
        color: #059669;
      }
      &.icon-amber {
        background: #fffbeb;
        color: #d97706;
      }
    }

    .capsule-content {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .capsule-label {
        font-size: 13px;
        color: #64748b;
        font-weight: 500;
      }

      .capsule-val {
        font-size: 24px;
        font-weight: 700;
        color: #0f172a;
        line-height: 1.2;

        .unit {
          font-size: 13px;
          font-weight: 500;
          color: #94a3b8;
          margin-left: 2px;
        }

        &.text-purple {
          color: #9333ea;
        }
        &.text-emerald {
          color: #059669;
        }
        &.text-amber {
          color: #d97706;
        }
      }

      .capsule-hint {
        font-size: 12px;
        color: #94a3b8;
        margin-top: 2px;

        &.text-blue {
          color: #2563eb;
        }
        &.text-emerald {
          color: #059669;
        }
        &.text-danger {
          color: #dc2626;
          font-weight: 600;
        }
      }
    }
  }
}

/* 2. 筛选过滤卡片（长圆跑道控件） */
.filter-capsule-card {
  background: #ffffff;
  border-radius: $border-radius-xl;
  padding: 16px 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: $shadow-sm;

  .filter-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;

    .filter-inputs {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      flex: 1;

      .pill-input {
        width: 200px;
        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
          &:hover {
            box-shadow: 0 0 0 1px #cbd5e1 inset;
          }
          &.is-focus {
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.25) inset;
          }
        }
      }

      .pill-select {
        width: 140px;
        :deep(.el-select__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
        }
      }

      .pill-date-picker {
        width: 250px;
        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
        }
      }
    }

    .filter-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

/* 跑道药丸胶囊按钮通用样式 */
.pill-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &--primary {
    background: #1677ff;
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
    &:hover {
      background: #4096ff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
    }
  }

  &--default {
    background: #f1f5f9;
    color: #475569;
    border-color: #e2e8f0;
    &:hover {
      background: #e2e8f0;
      color: #1e293b;
    }
  }
}

/* 3. 主表格卡片 */
.log-table-card {
  background: #ffffff;
  border-radius: $border-radius-xl;
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: $shadow-sm;

  .table-toolbar-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .table-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }

      .table-badge-count {
        font-size: 12px;
        color: #64748b;
        background: #f1f5f9;
        padding: 2px 10px;
        border-radius: 9999px;
        font-weight: 500;
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .module-title-tag {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
  }

  .summary-cell {
    .summary-text {
      font-size: 13px;
      color: #334155;
      font-weight: 500;
    }
  }

  .oper-user-cell {
    .user-name {
      font-size: 13px;
      font-weight: 600;
      color: #2563eb;
    }
  }

  .ip-code {
    font-family: 'JetBrains Mono', monospace, Consolas;
    font-size: 12px;
    color: #64748b;
    background: #f8fafc;
    padding: 2px 6px;
    border-radius: 6px;
  }

  .cost-time-badge {
    font-family: 'JetBrains Mono', monospace;
    font-size: 12px;
    font-weight: 600;
  }

  /* 跑道标签 pill-tag */
  .pill-tag {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 24px;
    padding: 0 10px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    flex-shrink: 0;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &--success {
      background: #ecfdf5;
      color: #059669;
      .dot {
        background: #10b981;
      }
    }

    &--danger {
      background: #fef2f2;
      color: #dc2626;
      .dot {
        background: #ef4444;
      }
    }

    &--biz-1 {
      background: #ecfdf5;
      color: #059669;
    } // 新增
    &--biz-2 {
      background: #eff6ff;
      color: #2563eb;
    } // 修改
    &--biz-3 {
      background: #fef2f2;
      color: #dc2626;
    } // 删除
    &--biz-7 {
      background: #faf5ff;
      color: #7c3aed;
    } // 授权
    &--biz-5,
    &--biz-6 {
      background: #fff7ed;
      color: #ea580c;
    } // 导入导出
    &--biz-8 {
      background: #fdf2f8;
      color: #db2777;
    } // 清空
    &--biz-0 {
      background: #f1f5f9;
      color: #64748b;
    }
  }

  .method-badge {
    display: inline-block;
    font-size: 12px;
    font-weight: 700;
    font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
    padding: 0 !important;

    &.method--get {
      color: #16a34a;
    }
    &.method--post {
      color: #2563eb;
    }
    &.method--put {
      color: #d97706;
    }
    &.method--delete {
      color: #dc2626;
    }
    &.method--other {
      color: #64748b;
    }
  }

  .action-pill-group {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  .pagination-bar {
    display: flex;
    justify-content: flex-start !important;
    align-items: center;
    margin-top: 18px;
    padding-top: 12px;
    border-top: 1px solid #ebf1f7;

    :deep(.el-pagination) {
      justify-content: flex-start !important;
      align-items: center;
      display: flex;
      width: 100%;
    }
  }
}

/* 药丸操作按钮（对齐 UserList.vue 的风格） */
.table-action-pill {
  height: 28px;
  padding: 0 12px;
  border-radius: 9999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;

  &--primary {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #2563eb;
    &:hover {
      background: #2563eb;
      border-color: #2563eb;
      color: #ffffff;
      transform: translateY(-1px);
    }
  }

  &--danger {
    background: #fef2f2;
    border-color: #fecaca;
    color: #dc2626;
    &:hover {
      background: #dc2626;
      border-color: #dc2626;
      color: #ffffff;
      transform: translateY(-1px);
    }
  }
}

/* 4. 详情弹窗自定义样式 */
:deep(.custom-oper-detail-dialog) {
  border-radius: 16px;
  overflow: hidden;

  .el-dialog__header {
    border-bottom: 1px solid #f1f5f9;
    padding: 16px 20px;
    margin-right: 0;
    .el-dialog__title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  .el-dialog__body {
    padding: 20px;
  }

  .el-dialog__footer {
    border-top: 1px solid #f1f5f9;
    padding: 12px 20px;
  }
}

.detail-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .detail-status-banner {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    border-radius: 12px;

    &.banner--success {
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      color: #065f46;
      .banner-icon {
        color: #059669;
        font-size: 26px;
      }
    }

    &.banner--danger {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
      .banner-icon {
        color: #dc2626;
        font-size: 26px;
      }
    }

    .banner-title {
      font-size: 15px;
      font-weight: 700;
      line-height: 1.3;
    }

    .banner-sub {
      font-size: 12px;
      opacity: 0.85;
      margin-top: 2px;
    }
  }

  .diff-section {
    background: #fffbeb;
    border: 1px solid #fde68a;
    border-radius: 12px;
    padding: 12px 16px;

    .section-label {
      font-size: 13px;
      font-weight: 700;
      color: #92400e;
      display: flex;
      align-items: center;
      margin-bottom: 8px;
    }

    .diff-tags-cloud {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .diff-pill-tag {
        background: #ffffff;
        border: 1px solid #fcd34d;
        color: #b45309;
        font-size: 12px;
        font-weight: 600;
        padding: 3px 10px;
        border-radius: 9999px;
      }
    }
  }

  .error-section {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 12px;
    padding: 12px 16px;

    .section-label {
      font-size: 13px;
      font-weight: 700;
      display: flex;
      align-items: center;
      margin-bottom: 6px;
    }

    .error-box {
      font-size: 12px;
      color: #dc2626;
      font-family: monospace;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }

  .meta-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    background: #f8fafc;
    border-radius: 12px;
    padding: 14px 18px;
    border: 1px solid #e2e8f0;

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 2px;

      &.full-span {
        grid-column: span 2;
      }

      .meta-k {
        font-size: 12px;
        color: #64748b;
        font-weight: 500;
      }

      .meta-v {
        font-size: 13px;
        color: #0f172a;
        font-weight: 600;

        &.code-text {
          font-family: monospace;
          color: #2563eb;
          font-weight: 500;
        }
      }
    }
  }

  .payload-cards-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .payload-box {
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      overflow: hidden;

      .payload-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: #f8fafc;
        padding: 8px 14px;
        border-bottom: 1px solid #e2e8f0;

        .box-title {
          font-size: 12px;
          font-weight: 700;
          color: #475569;
        }

        .copy-pill-btn {
          height: 24px;
          padding: 0 10px;
          border-radius: 9999px;
          font-size: 11px;
          font-weight: 600;
          background: #ffffff;
          border: 1px solid #cbd5e1;
          color: #475569;
          display: inline-flex;
          align-items: center;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            border-color: #2563eb;
            color: #2563eb;
          }
        }
      }

      .json-code-block {
        margin: 0;
        padding: 12px 14px;
        background: #ffffff;
        font-family: 'JetBrains Mono', Consolas, monospace;
        font-size: 12px;
        line-height: 1.5;
        max-height: 200px;
        overflow-y: auto;
        white-space: pre-wrap;
        word-break: break-all;
        color: #334155;
      }
    }
  }
}
</style>
