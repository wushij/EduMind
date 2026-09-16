<template>
  <div>
    <!-- 顶部面包屑与导航条 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/question/banks')">
        返回题库列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/banks' }">题库中心</el-breadcrumb-item>
        <el-breadcrumb-item>{{ bankInfo?.name || '题库详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 题库信息看板 -->
    <div class="bank-hero-card" v-loading="loading">
      <div class="hero-main-content">
        <div class="bank-avatar-badge">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <div class="hero-info">
          <div class="title-row">
            <h1 class="bank-title">{{ bankInfo?.name || '试题库详情' }}</h1>
            <span class="course-pill-tag">{{ bankInfo?.courseName || '未指定课程' }}</span>
            <span class="update-time-tag">更新于 {{ bankInfo?.updateTime || '近期' }}</span>
          </div>
          <p class="bank-description">
            {{ bankInfo?.description || '本题库聚合了课程教学过程中的核心试题、历年真题与经典测验题目，可用于按需检索、日常练习与组卷出题。' }}
          </p>
        </div>
      </div>

      <!-- 右侧指标与操作区 -->
      <div class="hero-actions-dock">
        <div class="metrics-grid">
          <div class="metric-item">
            <span class="metric-val">{{ questionCount }}</span>
            <span class="metric-label">收录题目数</span>
          </div>
          <div class="metric-item">
            <span class="metric-val">{{ totalScore }}</span>
            <span class="metric-label">卷面参考总分</span>
          </div>
        </div>

        <div class="action-buttons-group">
          <el-button type="primary" class="capsule-btn-main" @click="onOpenAddDrawer">
            <el-icon class="mr-1"><Plus /></el-icon> 挑选题目入库
          </el-button>
          <el-button class="capsule-btn-sub" @click="onFastComposeExam">
            <el-icon class="mr-1"><DocumentCopy /></el-icon> 基于此题库组卷
          </el-button>
          <button type="button" class="table-action-pill table-action-pill--danger" @click="onDeleteBank">
            删除题库
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Plus, DocumentCopy, FolderOpened } from '@element-plus/icons-vue';
import type { Router } from 'vue-router';

defineProps<{
  router: Router;
  loading: boolean;
  bankInfo: Record<string, unknown> | null;
  questionCount: number;
  totalScore: number;
  onOpenAddDrawer: () => void;
  onFastComposeExam: () => void;
  onDeleteBank: () => void;
}>();
</script>

<style scoped lang="scss">
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

.bank-hero-card {
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

  .hero-main-content {
    display: flex;
    align-items: center;
    gap: 20px;
    max-width: 680px;

    .bank-avatar-badge {
      width: 64px;
      height: 64px;
      background: #eff6ff;
      border: 1px solid #dbeafe;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: #2563eb;
      flex-shrink: 0;
    }

    .hero-info {
      .title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .bank-title {
          font-size: 22px;
          font-weight: 700;
          color: #0f172a;
          margin: 0;
        }

        .course-pill-tag {
          background: #f1f5f9;
          color: #475569;
          font-size: 12px;
          padding: 3px 10px;
          border-radius: 9999px;
          font-weight: 500;
        }

        .update-time-tag {
          font-size: 12px;
          color: #94a3b8;
        }
      }

      .bank-description {
        margin: 8px 0 0;
        font-size: 14px;
        line-height: 1.6;
        color: #64748b;
      }
    }
  }

  .hero-actions-dock {
    display: flex;
    align-items: center;
    gap: 32px;

    .metrics-grid {
      display: flex;
      gap: 24px;

      .metric-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .metric-val {
          font-size: 28px;
          font-weight: 800;
          color: #1e293b;
          line-height: 1;
        }

        .metric-label {
          font-size: 12px;
          color: #94a3b8;
          margin-top: 6px;
        }
      }
    }

    .action-buttons-group {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .capsule-btn-main {
        background: #2563eb;
        border-color: #2563eb;
        border-radius: 8px;
        font-weight: 500;
        padding: 8px 18px;
      }

      .capsule-btn-sub {
        border-radius: 8px;
        font-weight: 500;
        padding: 8px 18px;
      }
    }
  }
}
</style>
