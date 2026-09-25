<template>
  <div class="question-module-page submission-list-page">
    <ModulePageHeroHeader
      :icon="Document"
      title="学生作业答卷与批改总览"
      :badge="`当前 ${total} 份答卷`"
      description="汇聚各课程在线提交的作业答卷，支持教师终审、AI 辅助预批改与评阅溯源。"
    >
      <template #stats>
        <SubmissionListStatsBar
          :total="stats.total ?? total"
          :pending-count="stats.submittedCount ?? 0"
          :ai-graded-count="stats.gradedCount ?? 0"
          :graded-count="stats.reviewedCount ?? 0"
        />
      </template>
      <template #actions>
        <button
          type="button"
          class="module-capsule-btn module-capsule-btn--ai"
          :disabled="batchLoading"
          @click="handleBatchAIGrading"
        >
          <el-icon><AiSparkleIcon /></el-icon>
          <span>{{ batchLoading ? '队列批改中…' : '启动全队列 AI 批改' }}</span>
        </button>
      </template>
    </ModulePageHeroHeader>

    <div class="filter-capsule-card">
      <div class="submission-filter-row">
        <div class="capsule-search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            type="text"
            class="capsule-search-input"
            placeholder="搜索学生姓名、学号、作业名称..."
            @keyup.enter="reloadList"
          />
          <button v-if="searchKeyword" type="button" class="clear-btn" @click="clearSearch">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <el-select v-model="selectedCourseId" placeholder="所属课程" clearable class="filter-select" @change="reloadList">
          <el-option label="全部课程" :value="null" />
          <el-option
            v-for="c in courses"
            :key="c.id"
            :label="c.title || c.name"
            :value="c.id"
          />
        </el-select>
        <el-select v-model="selectedStatus" placeholder="批改状态" clearable class="filter-select" @change="reloadList">
          <el-option label="全部状态" value="" />
          <el-option label="待批改" :value="SUBMISSION_STATUS.SUBMITTED" />
          <el-option label="AI 已评 · 待确认" :value="SUBMISSION_STATUS.GRADED" />
          <el-option label="批改完成" :value="SUBMISSION_STATUS.REVIEWED" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="questions-list-card submissions-table-wrap">
      <div class="submissions-table-toolbar">
        <span class="list-title">答卷列表</span>
        <span class="list-count-badge">共 {{ total }} 份</span>
      </div>

      <div class="submissions-table-body">
        <el-table :data="allSubmissions" stripe class="main-table">
          <el-table-column label="学号" prop="studentNo" width="130">
            <template #default="{ row }">
              <span class="font-mono text-slate-600 font-semibold">{{ row.studentNo || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="学生姓名" prop="studentName" width="130">
            <template #default="{ row }">
              <div class="student-name-cell">
                <!-- 有头像显示头像；无头像 / 加载失败时自动回退到姓名首字 -->
                <el-avatar :size="26" :src="row.studentAvatar" class="student-avatar">
                  {{ (row.studentName || '学').slice(0, 1) }}
                </el-avatar>
                <span class="font-medium text-slate-800">{{ row.studentName || '—' }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="所属课程" prop="courseName" width="180">
            <template #default="{ row }">
              <span class="pill-badge pill-badge--type">{{ row.courseName || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="对应作业任务" prop="assignmentTitle" min-width="220">
            <template #default="{ row }">
              <span
                class="font-semibold text-slate-700 hover:text-blue-600 cursor-pointer"
                @click="router.push(`/question/submissions/${row.id}`)"
              >
                {{ row.assignmentTitle || '—' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="提交时间" prop="submitTime" width="170">
            <template #default="{ row }">
              <span class="text-xs text-slate-500">{{ formatTime(row.submitTime) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="AI 预评" width="120">
            <template #default="{ row }">
              <span
                v-if="row.totalScore !== null && row.totalScore !== undefined && row.status !== 'SUBMITTED'"
                class="pill-badge pill-badge--ai"
              >
                {{ row.totalScore }} 分
              </span>
              <span v-else class="text-xs text-slate-400">未调用</span>
            </template>
          </el-table-column>

          <el-table-column label="最终得分" width="110">
            <template #default="{ row }">
              <span v-if="row.status === 'REVIEWED' && row.totalScore != null" class="font-bold text-blue-600">
                {{ row.totalScore }} 分
              </span>
              <span v-else class="text-xs text-slate-400">待终评</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small" round effect="light">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <div class="action-btn-group">
                <button
                  type="button"
                  class="table-action-link"
                  @click="router.push(`/question/submissions/${row.id}`)"
                >
                  {{ row.status === 'REVIEWED' ? '详情' : '评阅' }}
                </button>
                <button
                  type="button"
                  class="table-action-link table-action-link--ai"
                  title="重新调用 AI 阅卷推演模型重新判分与撰写评语"
                  @click="handleSingleAIGrade(row)"
                >
                  <el-icon><AiSparkleIcon /></el-icon>
                  <span>{{ row.status === 'SUBMITTED' ? 'AI批改' : 'AI复评' }}</span>
                </button>
                <button
                  type="button"
                  class="table-action-link table-action-link--danger"
                  title="彻底删除此份答卷与评分记录"
                  @click="handleDeleteSubmission(row)"
                >
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 居左标准分页底栏（远离右下角悬浮球） -->
        <div v-if="total > 0" class="pagination-footer-bar">
          <AppPagination
            v-model:page-num="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            @change="reloadList"
          />
        </div>
      </div>
    </div>

    <!-- AI 智能阅卷认知推演弹窗（雷达环脉冲、秒级实时计时、流水线推进与中止控制） -->
    <AssignmentGradingEngineDialog
      :visible="aiThinkingVisible"
      :title="aiThinkingTitle"
      @abort="handleAbortSubmissionGrade"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { Search, Document, Close, Delete } from '@element-plus/icons-vue';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';
import SubmissionListStatsBar from '@/components/question/submission/SubmissionListStatsBar.vue';
import AssignmentGradingEngineDialog from '@/components/question/assignment/AssignmentGradingEngineDialog.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { useSubmissionList } from '@/composables/question/useSubmission';
import {
  SUBMISSION_STATUS,
  SUBMISSION_STATUS_LABEL,
  SUBMISSION_STATUS_TAG
} from '@/constants/question/assignment';
import type { SubmissionItem } from '@/types/question/submission';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const router = useRouter();
const {
  loading,
  batchLoading,
  courses,
  allSubmissions,
  total,
  stats,
  aiThinkingVisible,
  aiThinkingTitle,
  handleAbortSubmissionGrade,
  loadCourses,
  fetchAllSubmissions,
  batchGradePending,
  triggerSingleRegrade,
  removeSubmissionRecord
} = useSubmissionList();

const searchKeyword = ref('');
const selectedCourseId = ref<number | null>(null);
const selectedStatus = ref('');
const pageNum = ref(1);
const pageSize = ref(10);

onMounted(async () => {
  await Promise.all([loadCourses(), reloadList()]);
});

async function handleDeleteSubmission(row: SubmissionItem) {
  const success = await removeSubmissionRecord(row.id, row.studentName);
  if (success) {
    await reloadList();
  }
}

async function reloadList() {
  await fetchAllSubmissions({
    courseId: selectedCourseId.value,
    status: selectedStatus.value,
    keyword: searchKeyword.value,
    page: pageNum.value,
    pageSize: pageSize.value
  });
}

function clearSearch() {
  searchKeyword.value = '';
  pageNum.value = 1;
  reloadList();
}

function formatTime(val: unknown) {
  if (!val) return '—';
  return String(val).replace('T', ' ').slice(0, 16);
}

function getStatusLabel(status: string) {
  return SUBMISSION_STATUS_LABEL[status] || status || '—';
}

function getStatusTagType(status: string) {
  return SUBMISSION_STATUS_TAG[status] || 'info';
}

async function handleSingleAIGrade(row: SubmissionItem) {
  await triggerSingleRegrade(row);
  await reloadList();
}

async function handleBatchAIGrading() {
  const pendingCount = stats.value?.submittedCount ?? 0;
  if (pendingCount === 0 && total.value > 0) {
    try {
      await ElMessageBox.confirm(
        '当前队列中暂无新提交的待评答卷（已有答卷均已完成初次 AI 预评）。\n\n是否对全队列已有答卷【重新执行一轮 AI 智能复评】？',
        '全量 AI 复评确认',
        {
          confirmButtonText: '启动全队列复评',
          cancelButtonText: '取消',
          type: 'info'
        }
      );
      await batchGradePending(selectedCourseId.value, true);
    } catch {
      return;
    }
  } else {
    await batchGradePending(selectedCourseId.value, false);
  }
  await reloadList();
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
@use '@/styles/question/question-list-panel.scss';
@use '@/styles/question/submission-panel.scss';

.submission-list-page {
  .list-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
    margin-right: 8px;
  }

  .list-count-badge {
    font-size: 12px;
    font-weight: 600;
    color: #1677ff;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    padding: 2px 10px;
    border-radius: 9999px;
  }

  .pagination-footer-bar {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    padding: 10px 14px 6px;
    border-top: 1px solid #f1f5f9;

    :deep(.pagination-bar) {
      margin-top: 0;
      padding: 0;
      border-top: none;
      justify-content: flex-start !important;
    }
  }

  .action-btn-group {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    white-space: nowrap;

    :deep(.table-action-link) {
      white-space: nowrap;
      height: 28px;
      line-height: 28px;
      padding: 0 10px;
      font-size: 12px;
    }
  }

  .table-action-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 28px;
    padding: 0 10px;
    border-radius: 9999px;
    border: 1px solid #bfdbfe;
    background: #eff6ff;
    color: #1677ff;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.15s ease;

    &:hover {
      background: #dbeafe;
      border-color: #93c5fd;
    }
  }

  .table-action-link--ai {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    height: 28px;
    padding: 0 10px;
    border-radius: 9999px;
    border: 1px solid #ddd6fe;
    background: #f5f3ff;
    color: #7c3aed;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background: #ede9fe;
      border-color: #c4b5fd;
      color: #6d28d9;
    }
  }

  .table-action-link--danger {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    height: 28px;
    padding: 0 10px;
    border-radius: 9999px;
    border: 1px solid #fecaca;
    background: #fef2f2;
    color: #ef4444;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background: #fee2e2;
      border-color: #fca5a5;
      color: #dc2626;
    }
  }

  .pill-badge {
    padding: 3px 11px;
    border-radius: 9999px;
    font-size: 11.5px;
    font-weight: 600;

    &--type {
      background: #f1f5f9;
      color: #475569;
    }

    &--ai {
      background: #f5f3ff;
      color: #6d28d9;
    }
  }
}
</style>
