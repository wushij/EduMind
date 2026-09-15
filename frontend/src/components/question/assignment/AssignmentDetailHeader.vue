<template>
  <div v-loading="loading" class="assignment-hero-card">
    <div class="hero-top-row">
      <div class="title-and-tags">
        <el-tag
          :type="assignmentInfo?.status === 'GRADED' ? 'success' : 'warning'"
          size="small"
          effect="dark"
          round
        >
          {{ assignmentInfo?.status === 'GRADED' ? '已全员批改' : '评阅收集中' }}
        </el-tag>
        <h1 class="assignment-title">{{ assignmentInfo?.title || '作业测验详情' }}</h1>
      </div>

      <div class="hero-actions">
        <el-button
          type="success"
          class="action-btn"
          :loading="batchAILoading"
          @click="$emit('batch-ai-grade')"
        >
          <el-icon><Service /></el-icon>
          <span>一键全班 AI 智能预批改</span>
        </el-button>
        <el-button class="action-btn" @click="$emit('remind-unsubmitted')">
          <el-icon><Bell /></el-icon>
          <span>一键催交未交学生</span>
        </el-button>
      </div>
    </div>

    <div class="meta-pills-row">
      <span class="meta-pill">课程：<strong>{{ assignmentInfo?.courseName || '数据结构与算法' }}</strong></span>
      <span class="meta-pill">截止时间：<strong>{{ assignmentInfo?.deadline || '2026-09-25 23:59:59' }}</strong></span>
      <span class="meta-pill">卷面总分：<strong class="text-blue-600">{{ assignmentInfo?.totalScore || 100 }} 分</strong></span>
      <span class="meta-pill">及格标准：<strong class="text-emerald-600">{{ assignmentInfo?.passScore || 60 }} 分</strong></span>
    </div>

    <!-- 四宫格学情指标 -->
    <div class="kpi-grid">
      <div class="kpi-box">
        <span class="kpi-val">{{ totalStudentsCount }}</span>
        <span class="kpi-label">班级修读总人数</span>
      </div>
      <div class="kpi-box">
        <span class="kpi-val text-blue-600">{{ submittedCount }}</span>
        <span class="kpi-label">已提交学生数 ({{ submissionRate }}%)</span>
      </div>
      <div class="kpi-box">
        <span class="kpi-val text-amber-600">{{ pendingReviewCount }}</span>
        <span class="kpi-label">待教师终审答卷</span>
      </div>
      <div class="kpi-box">
        <span class="kpi-val text-emerald-600">{{ averageScore }} 分</span>
        <span class="kpi-label">当前平均得分</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Service, Bell } from '@element-plus/icons-vue';

defineProps<{
  loading: boolean;
  batchAILoading: boolean;
  assignmentInfo: any;
  totalStudentsCount: number;
  submittedCount: number;
  submissionRate: number;
  pendingReviewCount: number;
  averageScore: string;
}>();

defineEmits<{
  'batch-ai-grade': [];
  'remind-unsubmitted': [];
}>();
</script>

<style scoped lang="scss">
.assignment-hero-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
  padding: 24px 32px;
  margin-bottom: 24px;

  .hero-top-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 14px;

    .title-and-tags {
      display: flex;
      align-items: center;
      gap: 12px;

      .assignment-title {
        font-size: 22px;
        font-weight: 800;
        color: #0f172a;
        margin: 0;
      }
    }

    .hero-actions {
      display: flex;
      gap: 12px;
    }
  }

  .meta-pills-row {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 20px;

    .meta-pill {
      background: #f1f5f9;
      padding: 4px 12px;
      border-radius: 6px;
      font-size: 13px;
      color: #475569;
    }
  }

  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
    padding-top: 20px;
    border-top: 1px solid #f1f5f9;

    .kpi-box {
      display: flex;
      flex-direction: column;

      .kpi-val {
        font-size: 26px;
        font-weight: 800;
        color: #0f172a;
        line-height: 1;
      }

      .kpi-label {
        font-size: 12px;
        color: #94a3b8;
        margin-top: 6px;
      }
    }
  }
}
</style>
