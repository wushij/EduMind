<template>
  <div class="assignment-detail-container">
    <!-- 顶部面包屑与导航条 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="handleBack">
        返回作业列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ assignmentInfo?.title || '作业详情与答卷批改' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <AssignmentDetailHeader
      :loading="loading"
      :batch-a-i-loading="batchAILoading"
      :assignment-info="assignmentInfo"
      :total-students-count="totalStudentsCount"
      :submitted-count="submittedCount"
      :submission-rate="submissionRate"
      :pending-review-count="pendingReviewCount"
      :average-score="averageScore"
      @batch-ai-grade="handleBatchAIGrade"
      @remind-unsubmitted="handleRemindUnsubmitted"
    />

    <!-- 主体双标签页：学生答卷管理 vs 作业试题清单 -->
    <div class="main-tabs-card">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- 标签页 1：学生答卷列表与批改 -->
        <el-tab-pane label="学生答卷与批改列表" name="submissions">
          <AssignmentSubmissionTable
            v-model:student-search="studentSearch"
            v-model:status-filter="statusFilter"
            :filtered-submissions="filteredSubmissions"
            :get-submission-status-label="getSubmissionStatusLabel"
            :get-submission-status-type="getSubmissionStatusType"
            @grade="goToGradingWorkspace"
            @ai-grade="triggerSingleAIGrade"
          />
        </el-tab-pane>

        <!-- 标签页 2：作业试卷试题与参考答案 -->
        <el-tab-pane label="作业卷面试题及标准解答" name="questions">
          <div v-if="questionsList.length > 0" class="questions-tab-content">
            <div
              v-for="(q, idx) in questionsList"
              :key="q.id"
              class="question-review-card"
            >
              <div class="q-header">
                <span class="q-idx">第 {{ idx + 1 }} 题</span>
                <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <span class="q-score">{{ q.score || 5 }} 分</span>
              </div>
              <p class="q-stem">{{ q.stem }}</p>

              <div v-if="q.options && q.options.length" class="q-options">
                <div
                  v-for="opt in q.options"
                  :key="opt.key"
                  class="q-opt"
                  :class="{ correct: opt.isCorrect }"
                >
                  <span class="opt-k">{{ opt.key }}.</span>
                  <span>{{ opt.content }}</span>
                </div>
              </div>

              <div class="q-analysis-box">
                <div class="ans-line">
                  <strong>标准参考答案：</strong>
                  <span class="text-emerald-600 font-bold">{{ q.correctAnswer || '无客观标准答案' }}</span>
                </div>
                <div class="ans-line">
                  <strong>解析与评分细则：</strong>
                  <span>{{ q.analysis || '暂无解析' }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="该作业关联的试卷中暂无试题数据" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';
import { useAssignmentDetail } from '@/composables/question/useAssignment';
import AssignmentDetailHeader from '@/components/question/assignment/AssignmentDetailHeader.vue';
import AssignmentSubmissionTable from '@/components/question/assignment/AssignmentSubmissionTable.vue';

const {
  loading,
  batchAILoading,
  activeTab,
  assignmentInfo,
  questionsList,
  studentSearch,
  statusFilter,
  totalStudentsCount,
  filteredSubmissions,
  submittedCount,
  submissionRate,
  pendingReviewCount,
  averageScore,
  goToGradingWorkspace,
  triggerSingleAIGrade,
  handleBatchAIGrade,
  handleRemindUnsubmitted,
  handleBack,
  getSubmissionStatusLabel,
  getSubmissionStatusType,
  getTypeLabel,
  getTypeTagType
} = useAssignmentDetail();
</script>

<style scoped lang="scss">
.assignment-detail-container {
  padding: 24px;
  background: #f8fafc;
  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .main-tabs-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 20px 24px;

    /* 试题与标准解答标签页 */
    .questions-tab-content {
      display: flex;
      flex-direction: column;
      gap: 18px;

      .question-review-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 18px 20px;

        .q-header {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;

          .q-idx {
            font-weight: 700;
            color: #2563eb;
          }

          .q-score {
            font-size: 12px;
            color: #64748b;
          }
        }

        .q-stem {
          font-size: 15px;
          color: #1e293b;
          line-height: 1.6;
          margin: 0 0 12px;
        }

        .q-options {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 8px;
          margin-bottom: 12px;

          .q-opt {
            background: #ffffff;
            border: 1px solid #edf2f7;
            padding: 8px 12px;
            border-radius: 6px;
            font-size: 13px;
            display: flex;
            gap: 6px;

            .opt-k {
              font-weight: 600;
              color: #64748b;
            }

            &.correct {
              border-color: #86efac;
              background: #f0fdf4;
              color: #166534;
            }
          }
        }

        .q-analysis-box {
          background: #ffffff;
          border-left: 3px solid #10b981;
          padding: 10px 14px;
          border-radius: 6px;
          font-size: 13px;
          color: #334155;

          .ans-line {
            margin-bottom: 4px;
            &:last-child {
              margin-bottom: 0;
            }
          }
        }
      }
    }
  }
}
</style>
