<template>
  <div class="submissions-table-container">
    <!-- 列表过滤条（对标图 2 胶囊筛选风格） -->
    <div class="tab-filter-bar">
      <div class="left-filters">
        <el-input
          :model-value="studentSearch"
          placeholder="搜索学生姓名、学号..."
          clearable
          :prefix-icon="Search"
          class="filter-search-input"
          @update:model-value="$emit('update:studentSearch', $event)"
        />
        <el-select
          :model-value="statusFilter"
          placeholder="全部批改状态"
          clearable
          class="filter-status-select"
          @update:model-value="$emit('update:statusFilter', $event)"
        >
          <el-option label="全部状态" value="" />
          <el-option label="待批改" value="SUBMITTED" />
          <el-option label="AI 已评 · 待确认" value="GRADED" />
          <el-option label="批改完成" value="REVIEWED" />
        </el-select>
      </div>

      <div class="right-stats">
        <span class="count-tag">
          共检索到 <strong>{{ filteredSubmissions.length }}</strong> 份答卷
        </span>
      </div>
    </div>

    <!-- 答卷数据表格（支持标准10条/页分页） -->
    <el-table
      :data="paginatedSubmissions"
      class="submissions-table"
      empty-text="暂无学生提交的答卷数据"
    >
      <el-table-column label="学号" prop="studentNo" width="140">
        <template #default="{ row }">
          <span class="student-no-text">{{ row.studentNo }}</span>
        </template>
      </el-table-column>

      <el-table-column label="学生姓名" prop="studentName" width="150">
        <template #default="{ row }">
          <div class="student-cell">
            <!-- 有头像则显示头像；无头像 / 加载失败时 el-avatar 自动回退到插槽里的姓名首字 -->
            <el-avatar :size="26" :src="row.studentAvatar" class="student-avatar">
              {{ (row.studentName || '学').slice(0, 1) }}
            </el-avatar>
            <span class="student-name">{{ row.studentName }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="提交时间" prop="submitTime" min-width="190">
        <template #default="{ row }">
          <div class="submit-time-cell">
            <span class="time-text">{{ row.submitTime || '—' }}</span>
            <span v-if="row.isLate" class="late-chip">迟交</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="AI 智能预评" width="150">
        <template #default="{ row }">
          <div v-if="row.aiGraded || row.status === 'GRADED' || row.status === 'REVIEWED'" class="ai-grade-tag">
            <span class="ai-score-capsule">
              <el-icon><Cpu /></el-icon>
              <span>{{ row.aiScore != null ? `${row.aiScore} 分` : (row.totalScore != null ? `${row.totalScore} 分` : '已预评') }}</span>
            </span>
          </div>
          <span v-else class="not-triggered-chip">未触发 AI</span>
        </template>
      </el-table-column>

      <el-table-column label="最终实得分" width="130">
        <template #default="{ row }">
          <span v-if="row.finalScore !== null && row.finalScore !== undefined" class="final-score-text">
            <strong>{{ row.finalScore }}</strong> 分
          </span>
          <span v-else-if="row.totalScore !== null && row.totalScore !== undefined && row.status === 'REVIEWED'" class="final-score-text">
            <strong>{{ row.totalScore }}</strong> 分
          </span>
          <span v-else class="pending-score-text">待教师终评</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <span
            class="status-capsule-badge"
            :class="`status-capsule-badge--${(getSubmissionStatusType(row.status) || 'info')}`"
          >
            <span class="dot"></span>
            <span>{{ getSubmissionStatusLabel(row.status) }}</span>
          </span>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="260" fixed="right">
        <template #default="{ row }">
          <div class="table-actions-cell">
            <button
              type="button"
              class="action-pill-btn action-pill-btn--primary"
              @click="$emit('grade', row.id)"
            >
              {{ row.status === 'REVIEWED' ? '查看答卷详情' : '进入评阅打分' }}
            </button>
            <button
              type="button"
              class="action-pill-btn action-pill-btn--ai"
              @click="$emit('ai-grade', row)"
            >
              <el-icon><AiSparkleIcon /></el-icon>
              <span>AI 重新评估</span>
            </button>
            <button
              type="button"
              class="action-pill-btn action-pill-btn--danger"
              @click="$emit('delete-submission', row.id, row.studentName)"
            >
              <el-icon><Delete /></el-icon>
              <span>删除答卷</span>
            </button>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无符合条件的学生提交记录" />
      </template>
    </el-table>

    <!-- 标准分页栏（默认10条/页，支持 10/20/50 切换） -->
    <div v-if="filteredSubmissions.length > 0" class="table-pagination-footer">
      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="filteredSubmissions.length"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { Search, Cpu, Delete } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const props = defineProps<{
  studentSearch: string;
  statusFilter: string;
  filteredSubmissions: any[];
  getSubmissionStatusLabel: (status: string) => string;
  getSubmissionStatusType: (status: string) => string;
}>();

const emit = defineEmits<{
  'update:studentSearch': [value: string];
  'update:statusFilter': [value: string];
  grade: [submissionId: number];
  'ai-grade': [row: any];
  'delete-submission': [submissionId: number, studentName?: string];
}>();

const pageNum = ref(1);
const pageSize = ref(10);

const paginatedSubmissions = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value;
  return props.filteredSubmissions.slice(start, start + pageSize.value);
});

// 筛选或搜索变化时，重置分页到第一页
watch(
  () => [props.studentSearch, props.statusFilter],
  () => {
    pageNum.value = 1;
  }
);
</script>

<style scoped lang="scss">
.submissions-table-container {
  width: 100%;

  .tab-filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;
    gap: 16px;
    flex-wrap: wrap;

    .left-filters {
      display: flex;
      align-items: center;
      gap: 12px;

      .filter-search-input {
        width: 280px;

        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding: 3px 14px;
        }
      }

      .filter-status-select {
        width: 150px;

        :deep(.el-select__wrapper) {
          border-radius: 9999px;
          padding: 3px 14px;
        }
      }
    }

    .right-stats {
      .count-tag {
        font-size: 13px;
        color: #64748b;

        strong {
          color: #2563eb;
          font-weight: 700;
        }
      }
    }
  }

  .submissions-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__header-wrapper) th {
      background: #f8fafc;
      color: #475569;
      font-weight: 700;
      font-size: 12.5px;
      padding: 10px 0;
    }

    :deep(.el-table__row) td {
      padding: 12px 0;
    }

    .student-no-text {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      color: #475569;
      font-weight: 600;
      font-size: 13px;
    }

    .student-cell {
      display: flex;
      align-items: center;
      gap: 9px;

      .student-avatar {
        flex-shrink: 0;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border: 1px solid #bfdbfe;
        color: #2563eb;
        font-size: 12px;
        font-weight: 700;
      }

      .student-name {
        font-size: 13.5px;
        font-weight: 600;
        color: #1e293b;
      }
    }

    .submit-time-cell {
      display: flex;
      align-items: center;
      gap: 6px;

      .time-text {
        font-size: 12.5px;
        color: #64748b;
      }

      .late-chip {
        padding: 1px 6px;
        border-radius: 4px;
        background: #fef2f2;
        border: 1px solid #fecaca;
        color: #dc2626;
        font-size: 11px;
        font-weight: 600;
      }
    }

    .ai-score-capsule {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 10px;
      border-radius: 9999px;
      background: #f5f3ff;
      border: 1px solid #ddd6fe;
      color: #7c3aed;
      font-size: 12px;
      font-weight: 700;

      .el-icon {
        font-size: 13px;
      }
    }

    .not-triggered-chip {
      font-size: 12px;
      color: #94a3b8;
    }

    .final-score-text {
      font-size: 13px;
      color: #059669;

      strong {
        font-size: 16px;
        font-weight: 800;
      }
    }

    .pending-score-text {
      font-size: 12px;
      color: #94a3b8;
      font-style: italic;
    }

    .status-capsule-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 2px 9px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;

      .dot {
        width: 5px;
        height: 5px;
        border-radius: 50%;
      }

      &--success {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
        .dot {
          background: #10b981;
        }
      }

      &--warning {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid #fde68a;
        .dot {
          background: #f59e0b;
        }
      }

      &--info {
        background: #f8fafc;
        color: #64748b;
        border: 1px solid #e2e8f0;
        .dot {
          background: #94a3b8;
        }
      }

      &--primary {
        background: #eff6ff;
        color: #2563eb;
        border: 1px solid #bfdbfe;
        .dot {
          background: #3b82f6;
        }
      }
    }

    .table-actions-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .action-pill-btn {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 4px 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        outline: none;
        transition: all 0.15s ease;

        &--primary {
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;

          &:hover {
            background: #2563eb;
            color: #ffffff;
          }
        }

        &--ai {
          background: #f5f3ff;
          color: #7c3aed;
          border: 1px solid #ddd6fe;

          &:hover {
            background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
            color: #ffffff;
            border-color: transparent;
          }
        }

        &--danger {
          background: #fef2f2;
          color: #dc2626;
          border: 1px solid #fecaca;

          &:hover {
            background: #dc2626;
            color: #ffffff;
            border-color: #dc2626;
          }
        }
      }
    }
  }

  .table-pagination-footer {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    padding-top: 10px;
    margin-top: 10px;
    border-top: 1px solid #f1f5f9;
    width: 100%;

    :deep(.pagination-bar) {
      margin-top: 0;
      padding: 4px 0;
      border-top: none;
      width: 100%;
      justify-content: flex-start !important;
    }
  }
}
</style>
