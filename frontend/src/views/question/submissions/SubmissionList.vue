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
          <el-icon><MagicStick /></el-icon>
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
                <span class="avatar-dot"></span>
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

          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <button
                type="button"
                class="table-action-link"
                @click="router.push(`/question/submissions/${row.id}`)"
              >
                {{ row.status === 'REVIEWED' ? '查看详情' : '进入评阅' }}
              </button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="reloadList"
            @size-change="reloadList"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Search, Document, MagicStick, Close } from '@element-plus/icons-vue';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';
import SubmissionListStatsBar from '@/components/question/submission/SubmissionListStatsBar.vue';
import { useSubmissionList } from '@/composables/question/useSubmission';
import {
  SUBMISSION_STATUS,
  SUBMISSION_STATUS_LABEL,
  SUBMISSION_STATUS_TAG
} from '@/constants/question/assignment';

const router = useRouter();
const {
  loading,
  batchLoading,
  courses,
  allSubmissions,
  total,
  stats,
  loadCourses,
  fetchAllSubmissions,
  batchGradePending
} = useSubmissionList();

const searchKeyword = ref('');
const selectedCourseId = ref<number | null>(null);
const selectedStatus = ref('');
const pageNum = ref(1);
const pageSize = ref(20);

onMounted(async () => {
  await loadCourses();
  await reloadList();
});

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

async function handleBatchAIGrading() {
  await batchGradePending(selectedCourseId.value);
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

  .pagination-row {
    display: flex;
    justify-content: flex-end;
    padding: 16px 8px 8px;
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
