<template>
  <div v-loading="loading" class="assignment-hero-card">
    <!-- 顶部导航与面包屑（整合进 Hero 卡片，严格对标图 2） -->
    <div class="header-nav-bar">
      <button type="button" class="back-btn" @click="$emit('back')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回作业列表</span>
      </button>
      <el-divider direction="vertical" class="nav-divider" />
      <el-breadcrumb separator="/" class="header-breadcrumb">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ assignmentInfo?.title || '作业测验详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 主体元数据与核心操作栏 -->
    <div class="header-main-section">
      <div class="header-info-col">
        <div class="assignment-avatar-orb">
          <el-icon class="assignment-icon"><DocumentChecked /></el-icon>
          <span class="orb-glow-ring" />
        </div>

        <div class="assignment-meta-content">
          <div class="assignment-title-line">
            <h1 class="assignment-title">{{ assignmentInfo?.title || '作业测验详情' }}</h1>
            <span class="course-badge">
              <el-icon><Collection /></el-icon>
              {{ assignmentInfo?.courseName || '数据结构与算法' }}
            </span>
            <span
              class="status-capsule-badge"
              :class="assignmentInfo?.status === 'GRADED' ? 'status-capsule-badge--success' : 'status-capsule-badge--warning'"
            >
              <span class="dot"></span>
              <span>{{ assignmentInfo?.status === 'GRADED' ? '已全员批改' : '评阅收集中' }}</span>
            </span>
          </div>

          <p class="assignment-description">
            {{ assignmentInfo?.description || '本课程阶段课后作业评估，覆盖核心章节重点考点。支持 AI 智能多维度自动化预阅卷与个性化评语生成。' }}
          </p>

          <div class="assignment-time-meta">
            <span class="time-item">
              <el-icon><Clock /></el-icon>
              截止时间：{{ assignmentInfo?.deadline || '2026-09-25 23:59:59' }}
            </span>
            <span class="meta-dot">·</span>
            <span class="status-item">
              <span class="status-indicator-dot" />
              智能批改服务正常就绪
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧操作栏（对标图 2 的分层现代操作按钮组） -->
      <div class="header-actions-col">
        <div class="actions-row actions-row--primary">
          <button
            type="button"
            class="action-pill action-pill--ai"
            :disabled="batchAILoading"
            @click="$emit('batch-ai-grade')"
          >
            <el-icon><Service /></el-icon>
            <span>{{ batchAILoading ? 'AI 正在批改中...' : '一键全班 AI 智能预批改' }}</span>
            <span class="ai-spark-chip">自动推演</span>
          </button>
        </div>

        <div class="actions-row actions-row--secondary">
          <button
            type="button"
            class="action-pill action-pill--ghost"
            @click="$emit('remind-unsubmitted')"
          >
            <el-icon><Bell /></el-icon>
            <span>一键催交未交学生</span>
          </button>

          <button
            v-permission="'assignment:delete'"
            type="button"
            class="action-pill action-pill--danger"
            @click="$emit('delete')"
          >
            <el-icon><Delete /></el-icon>
            <span>删除作业</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部：4 维学情测验微看板（1:1 模仿图 2 的 4 宫格卡片） -->
    <div class="stats-micro-bar">
      <!-- 指标卡 1：班级修读总人数 -->
      <div class="stat-card stat-card--blue">
        <div class="stat-card__icon-box">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">班级修读总人数</span>
          <div class="stat-card__value-row">
            <strong class="stat-card__num">{{ totalStudentsCount }}</strong>
            <span class="stat-card__unit">位学生</span>
          </div>
          <div class="stat-card__sub-hint">
            <span>选课在册修读学员</span>
          </div>
        </div>
      </div>

      <!-- 指标卡 2：已提交学生数与提交率 -->
      <div class="stat-card stat-card--emerald">
        <div class="stat-card__icon-box">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">已提交学生数</span>
          <div class="stat-card__value-row">
            <strong class="stat-card__num">{{ submittedCount }}</strong>
            <span class="stat-card__unit">人 ({{ submissionRate }}%)</span>
          </div>
          <div class="stat-card__sub-hint">
            <span>按时完成作答提交</span>
          </div>
        </div>
      </div>

      <!-- 指标卡 3：待终审答卷 -->
      <div class="stat-card stat-card--amber">
        <div class="stat-card__icon-box">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">待教师终审答卷</span>
          <div class="stat-card__value-row">
            <strong class="stat-card__num text-amber-600">{{ pendingReviewCount }}</strong>
            <span class="stat-card__unit">份</span>
          </div>
          <div class="stat-card__sub-hint">
            <span>需人工核验或AI复核</span>
          </div>
        </div>
      </div>

      <!-- 指标卡 4：当前平均得分与基准 -->
      <div class="stat-card stat-card--indigo">
        <div class="stat-card__icon-box">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-card__content">
          <span class="stat-card__label">当前平均得分</span>
          <div class="stat-card__value-row">
            <strong class="stat-card__num text-indigo-600">{{ averageScore }}</strong>
            <span class="stat-card__unit">分</span>
          </div>
          <div class="stat-card__sub-hint">
            <span>卷面满分 {{ assignmentInfo?.totalScore || 100 }} · 合格 {{ assignmentInfo?.passScore || 60 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ArrowLeft,
  Collection,
  Clock,
  Service,
  Bell,
  Delete,
  User,
  DocumentChecked,
  DataAnalysis
} from '@element-plus/icons-vue';

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
  (e: 'batch-ai-grade'): void;
  (e: 'remind-unsubmitted'): void;
  (e: 'delete'): void;
  (e: 'back'): void;
}>();
</script>

<style scoped lang="scss">
.assignment-hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 24px rgba(30, 80, 160, 0.05);
  background: #ffffff;
  background-image:
    linear-gradient(90deg, #2563eb 0%, #6366f1 50%, #7c3aed 100%),
    linear-gradient(145deg, #ffffff 0%, #f8fafc 55%, #f1f7ff 100%);
  background-size: 100% 3px, 100% calc(100% - 3px);
  background-position: 0 0, 0 3px;
  background-repeat: no-repeat;
  padding: 16px 28px 20px;
  margin-bottom: 20px;

  .header-nav-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f1f5f9;
    margin-bottom: 18px;

    .back-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0;
      border: none;
      background: transparent;
      font-size: 13px;
      font-weight: 600;
      color: #2563eb;
      cursor: pointer;
      white-space: nowrap;
      transition: color 0.15s ease;

      .el-icon {
        font-size: 14px;
      }

      &:hover {
        color: #1d4ed8;
      }
    }

    .nav-divider {
      margin: 0 4px;
      height: 14px;
      border-color: #e2e8f0;
    }

    .header-breadcrumb {
      flex: 1;

      :deep(.el-breadcrumb__inner) {
        font-size: 12.5px;
        color: #94a3b8;
      }

      :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
        color: #475569;
        font-weight: 600;
      }
    }
  }

  .header-main-section {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 28px;
    flex-wrap: wrap;

    @media (max-width: 1080px) {
      flex-direction: column;
    }
  }

  .header-info-col {
    display: flex;
    align-items: flex-start;
    gap: 18px;
    flex: 1;
    min-width: 320px;

    .assignment-avatar-orb {
      position: relative;
      flex-shrink: 0;
      width: 58px;
      height: 58px;
      border-radius: 16px;
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      border: 1.5px solid #bfdbfe;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 8px 20px rgba(37, 99, 235, 0.14);

      .assignment-icon {
        font-size: 28px;
        color: #2563eb;
      }

      .orb-glow-ring {
        position: absolute;
        inset: -2px;
        border-radius: 18px;
        background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), transparent 70%);
        pointer-events: none;
      }
    }

    .assignment-meta-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 0;

      .assignment-title-line {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .assignment-title {
          margin: 0;
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          letter-spacing: -0.02em;
          line-height: 1.25;
        }

        .course-badge {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 3px 12px;
          border-radius: 9999px;
          background: #eff6ff;
          border: 1px solid #bfdbfe;
          color: #2563eb;
          font-size: 12px;
          font-weight: 600;
          white-space: nowrap;

          .el-icon {
            font-size: 13px;
          }
        }

        .status-capsule-badge {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 3px 10px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 700;

          .dot {
            width: 6px;
            height: 6px;
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
        }
      }

      .assignment-description {
        margin: 0;
        font-size: 13.5px;
        color: #64748b;
        line-height: 1.6;
        max-width: 720px;
      }

      .assignment-time-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12.5px;
        color: #94a3b8;

        .time-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;

          .el-icon {
            font-size: 13px;
          }
        }

        .meta-dot {
          color: #cbd5e1;
        }

        .status-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          color: #059669;
          font-weight: 500;

          .status-indicator-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
          }
        }
      }
    }
  }

  .header-actions-col {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
    flex-shrink: 0;

    @media (max-width: 1080px) {
      align-items: flex-start;
      width: 100%;
    }

    .actions-row {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      &--secondary {
        justify-content: flex-end;
      }
    }

    .action-pill {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      height: 38px;
      padding: 0 16px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      outline: none;
      user-select: none;
      white-space: nowrap;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .el-icon {
        font-size: 14px;
      }

      &--ai {
        background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(114, 46, 209, 0.26);

        &:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(114, 46, 209, 0.38);
        }

        &:disabled {
          opacity: 0.65;
          cursor: not-allowed;
        }

        .ai-spark-chip {
          padding: 1px 7px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.24);
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.02em;
        }
      }

      &--ghost {
        background: #ffffff;
        color: #475569;
        border: 1px solid #cbd5e1;

        &:hover {
          background: #f8fafc;
          border-color: #94a3b8;
          color: #1e293b;
          transform: translateY(-1px);
        }
      }

      &--danger {
        background: #ffffff;
        color: #ef4444;
        border: 1px solid #fca5a5;

        &:hover {
          background: #fef2f2;
          border-color: #f87171;
          transform: translateY(-1px);
        }
      }
    }
  }

  /* 4 维微看板 */
  .stats-micro-bar {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    width: 100%;
    margin-top: 18px;
    padding-top: 18px;
    border-top: 1px solid rgba(226, 232, 240, 0.8);

    @media (max-width: 1100px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }

    .stat-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px 16px;
      border-radius: 14px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
      transition: all 0.2s ease;

      &:hover {
        border-color: #cbd5e1;
        transform: translateY(-1px);
        box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
      }

      &__icon-box {
        width: 42px;
        height: 42px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .el-icon {
          font-size: 20px;
        }
      }

      &__content {
        display: flex;
        flex-direction: column;
        min-width: 0;
        flex: 1;
      }

      &__label {
        font-size: 12px;
        color: #64748b;
        font-weight: 500;
        line-height: 1.3;
      }

      &__value-row {
        display: flex;
        align-items: baseline;
        gap: 6px;
        margin-top: 2px;
      }

      &__num {
        font-size: 20px;
        font-weight: 800;
        color: #0f172a;
        line-height: 1.2;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      &__unit {
        font-size: 12px;
        color: #64748b;
        font-weight: 600;
      }

      &__sub-hint {
        font-size: 11.5px;
        color: #94a3b8;
        margin-top: 2px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;

        strong {
          color: #475569;
        }
      }

      &--blue {
        .stat-card__icon-box {
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
        }
      }

      &--emerald {
        .stat-card__icon-box {
          background: #ecfdf5;
          color: #059669;
          border: 1px solid #a7f3d0;
        }
      }

      &--amber {
        .stat-card__icon-box {
          background: #fffbeb;
          color: #d97706;
          border: 1px solid #fde68a;
        }
      }

      &--indigo {
        .stat-card__icon-box {
          background: #f5f3ff;
          color: #7c3aed;
          border: 1px solid #ddd6fe;
        }
      }
    }
  }
}
</style>
