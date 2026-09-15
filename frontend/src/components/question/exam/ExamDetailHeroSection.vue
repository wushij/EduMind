<template>
<div class="exam-detail-hero-wrap">
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/question/exams')">
        返回试卷列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/exams' }">试卷管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ examData?.title || '试卷详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 试卷顶部信息卡片 -->
    <div v-loading="loading" class="exam-hero-card">
      <div class="hero-left">
        <div class="status-and-title">
          <el-tag :type="getStatusTagType(examData?.status)" effect="dark" size="small" round>
            {{ getStatusLabel(examData?.status) }}
          </el-tag>
          <h1 class="exam-main-title">{{ examData?.title || '考核试卷详情' }}</h1>
        </div>

        <div class="meta-pills-row">
          <span class="meta-pill">
            <span class="label">所属课程：</span>
            <strong>{{ examData?.courseName || '未指定课程' }}</strong>
          </span>
          <span class="meta-pill">
            <span class="label">考核学期：</span>
            <strong>{{ examData?.semester || '2025-2026-2' }}</strong>
          </span>
          <span class="meta-pill">
            <span class="label">考试时限：</span>
            <strong>{{ examData?.durationMinutes || 120 }} 分钟</strong>
          </span>
          <span class="meta-pill">
            <span class="label">试卷总分：</span>
            <strong class="text-blue-600">{{ examData?.totalScore || 100 }} 分</strong>
          </span>
          <span class="meta-pill">
            <span class="label">及格合格线：</span>
            <strong class="text-emerald-600">{{ examData?.passScore || 60 }} 分</strong>
          </span>
        </div>
      </div>

      <!-- 右侧操作区 -->
      <div class="hero-right-actions">
        <el-button type="primary" class="action-btn" @click="emit('publish')">
          <el-icon class="mr-1"><Promotion /></el-icon> 一键发布为在线作业/测验
        </el-button>
        <el-button class="action-btn" @click="emit('export')">
          <el-icon class="mr-1"><Download /></el-icon> 导出试卷 (JSON / 打印)
        </el-button>
      </div>
    </div>
</div>
</template>

<script setup lang="ts">
import { ArrowLeft, Promotion, Download } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';
import type { Router } from 'vue-router';

defineProps<{
  router: Router;
  loading: boolean;
  examData: ExamPaper | null;
  getStatusLabel: (status?: string) => string;
  getStatusTagType: (status?: string) => string;
}>();

const emit = defineEmits<{
  publish: [];
  export: [];
}>();
</script>

<style scoped lang="scss">
.exam-detail-hero-wrap {
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

.exam-hero-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
  padding: 24px 32px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 24px;

  .hero-left {
    .status-and-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .exam-main-title {
        font-size: 22px;
        font-weight: 800;
        color: #0f172a;
        margin: 0;
      }
    }

    .meta-pills-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .meta-pill {
        background: #f1f5f9;
        border-radius: 8px;
        padding: 4px 12px;
        font-size: 13px;
        color: #475569;

        .label {
          color: #64748b;
        }
      }
    }
  }

  .hero-right-actions {
    display: flex;
    gap: 12px;

    .action-btn {
      border-radius: 8px;
      font-weight: 500;
    }
  }
}


}
</style>
