<template>
  <div>
    <!-- 列表过滤条 -->
    <div class="tab-filter-bar">
      <div class="left-filters">
        <el-input
          :model-value="studentSearch"
          placeholder="搜索学生姓名、学号..."
          clearable
          :prefix-icon="Search"
          style="width: 280px"
          @update:model-value="$emit('update:studentSearch', $event)"
        />
        <el-select
          :model-value="statusFilter"
          placeholder="批改状态"
          clearable
          style="width: 140px"
          @update:model-value="$emit('update:statusFilter', $event)"
        >
          <el-option label="全部状态" value="" />
          <el-option label="待批改" value="SUBMITTED" />
          <el-option label="AI 已评 · 待确认" value="GRADED" />
          <el-option label="批改完成" value="REVIEWED" />
        </el-select>
      </div>

      <div class="right-stats">
        <span class="text-sm text-slate-500">共检索到 {{ filteredSubmissions.length }} 份答卷</span>
      </div>
    </div>

    <!-- 答卷数据表格 -->
    <el-table
      :data="filteredSubmissions"
      stripe
      class="submissions-table"
      empty-text="暂无学生提交的答卷数据"
    >
      <el-table-column label="学号" prop="studentNo" width="130">
        <template #default="{ row }">
          <span class="font-mono text-slate-600">{{ row.studentNo }}</span>
        </template>
      </el-table-column>

      <el-table-column label="学生姓名" prop="studentName" width="130">
        <template #default="{ row }">
          <div class="student-cell">
            <span class="avatar-dot"></span>
            <span class="font-medium text-slate-800">{{ row.studentName }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="提交时间" prop="submitTime" width="180">
        <template #default="{ row }">
          <span class="text-slate-600 text-xs">{{ row.submitTime }}</span>
          <el-tag v-if="row.isLate" type="danger" size="small" class="ml-1">迟交</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="AI智能预评" width="140">
        <template #default="{ row }">
          <div v-if="row.aiGraded" class="ai-grade-tag">
            <span class="text-xs text-purple-700 bg-purple-50 px-2 py-0.5 rounded font-semibold border border-purple-200 inline-flex items-center gap-1">
              <el-icon><Cpu /></el-icon>
              <span>预评 {{ row.aiScore }}分</span>
            </span>
          </div>
          <span v-else class="text-xs text-slate-400">未触发AI</span>
        </template>
      </el-table-column>

      <el-table-column label="最终实得分" width="130">
        <template #default="{ row }">
          <span v-if="row.finalScore !== null" class="font-bold text-base text-blue-600">
            {{ row.finalScore }} 分
          </span>
          <span v-else class="text-slate-400 text-xs italic">待终审打分</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="getSubmissionStatusType(row.status)" size="small">
            {{ getSubmissionStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            size="small"
            @click="$emit('grade', row.id)"
          >
            {{ row.status === 'GRADED' ? '查看答卷详情' : '进入评阅打分' }}
          </el-button>
          <el-button
            type="success"
            link
            size="small"
            @click="$emit('ai-grade', row)"
          >
            AI 重新评估
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无符合条件的学生提交记录" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { Search, Cpu } from '@element-plus/icons-vue';

defineProps<{
  studentSearch: string;
  statusFilter: string;
  filteredSubmissions: any[];
  getSubmissionStatusLabel: (status: string) => string;
  getSubmissionStatusType: (status: string) => string;
}>();

defineEmits<{
  'update:studentSearch': [value: string];
  'update:statusFilter': [value: string];
  grade: [submissionId: number];
  'ai-grade': [row: any];
}>();
</script>

<style scoped lang="scss">
.tab-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .left-filters {
    display: flex;
    gap: 12px;
  }
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .avatar-dot {
    width: 8px;
    height: 8px;
    background: #3b82f6;
    border-radius: 50%;
  }
}
</style>
