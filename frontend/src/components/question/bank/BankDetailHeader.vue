<template>
  <div v-loading="loading" class="bank-detail-header-card">
    <!-- 顶部导航与面包屑 -->
    <div class="header-nav-bar">
      <button type="button" class="back-btn" @click="router.push('/question/banks')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回题库列表</span>
      </button>
      <el-divider direction="vertical" class="nav-divider" />
      <el-breadcrumb separator="/" class="header-breadcrumb">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/banks' }">题库中心</el-breadcrumb-item>
        <el-breadcrumb-item>{{ bankInfo?.name || '题库详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 题库主体信息（整行铺开，避免右侧留白） -->
    <div class="header-main-section">
      <!-- 题库形象、标题与元数据 -->
      <div class="header-info-col">
        <div class="bank-avatar-orb">
          <el-icon class="bank-icon"><FolderOpened /></el-icon>
          <span class="orb-glow-ring" />
        </div>

        <div class="bank-meta-content">
          <div class="bank-title-line">
            <h1 class="bank-title" :title="bankInfo?.name">{{ bankInfo?.name || '试题库详情' }}</h1>
            <span class="course-badge">
              <el-icon><Collection /></el-icon>
              {{ bankInfo?.courseName || '未关联课程' }}
            </span>
          </div>

          <!-- 描述与更新时间 / 维护状态压缩为同一行，减少纵向占用 -->
          <div class="bank-summary-line">
            <p class="bank-description">
              {{
                bankInfo?.description ||
                '聚合课程核心知识点专项试题、历年真题与经典测验，支持全链路智能检索、AI 变式题扩充与快速组卷交付。'
              }}
            </p>

            <div class="bank-time-meta">
              <span class="time-item">
                <el-icon><Clock /></el-icon>
                最近更新于 {{ formatUpdateTime(bankInfo?.updateTime) }}
              </span>
              <span class="meta-dot">·</span>
              <span class="status-item">
                <span class="status-indicator-dot" />
                题库维护就绪
              </span>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 中部：4 维教学资产微看板 -->
    <BankDetailStatsBar
      :question-count="questionCount"
      :total-score="totalScore"
      :questions="questions"
      :course-name="bankInfo?.courseName"
    />

    <!-- 底部操作条：辅助工具靠左，核心主操作靠右收尾 -->
    <div class="header-actions-bar">
      <div class="actions-group actions-group--tools">
        <button
          type="button"
          class="action-pill action-pill--ghost"
          @click="onOpenAddDrawer"
        >
          <el-icon><Plus /></el-icon>
          <span>挑选题目入库</span>
        </button>

        <button
          type="button"
          class="action-pill action-pill--ghost"
          @click="onExportMarkdown"
        >
          <el-icon><Download /></el-icon>
          <span>导出试题集</span>
        </button>

        <el-popconfirm
          title="确定要删除此题库吗？题库内试题在公共试题池仍会保留。"
          confirm-button-text="确认删除"
          cancel-button-text="取消"
          confirm-button-type="danger"
          @confirm="onDeleteBank"
        >
          <template #reference>
            <button
              type="button"
              class="action-pill action-pill--danger"
            >
              <el-icon><Delete /></el-icon>
              <span>删除题库</span>
            </button>
          </template>
        </el-popconfirm>
      </div>

      <div class="actions-group actions-group--primary">
        <button
          type="button"
          class="action-pill action-pill--brand"
          @click="onCreateNewQuestion"
        >
          <el-icon><EditPen /></el-icon>
          <span>录入新题</span>
        </button>

        <button
          type="button"
          class="action-pill action-pill--compose"
          @click="onFastComposeExam"
        >
          <el-icon><DocumentCopy /></el-icon>
          <span>基于此题库组卷</span>
        </button>

        <button
          type="button"
          class="action-pill action-pill--ai"
          @click="onAiExpand"
        >
          <el-icon><AiSparkleIcon /></el-icon>
          <span>AI 智能扩题</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ArrowLeft,
  Plus,
  EditPen,
  DocumentCopy,
  FolderOpened,
  Download,
  Clock,
  Delete,
  Collection
} from '@element-plus/icons-vue';
import type { Router } from 'vue-router';
import BankDetailStatsBar from '@/components/question/bank/BankDetailStatsBar.vue';
import type { QuestionItem } from '@/types/question/question';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

defineProps<{
  router: Router;
  loading: boolean;
  bankInfo: Record<string, any> | null;
  questionCount: number;
  totalScore: number;
  questions?: QuestionItem[];
  onOpenAddDrawer: () => void;
  onCreateNewQuestion: () => void;
  onFastComposeExam: () => void;
  onExportMarkdown: () => void;
  onAiExpand: () => void;
  onDeleteBank: () => void;
}>();

function formatUpdateTime(val: unknown): string {
  if (!val) return '近期';
  const str = String(val);
  return str.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped lang="scss">
.bank-detail-header-card {
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

  /* 主体信息整行铺开，不再与右侧按钮挤同一行 */
  .header-main-section {
    display: block;
    width: 100%;
  }

  .header-info-col {
    display: flex;
    align-items: flex-start;
    gap: 18px;
    flex: 1;
    min-width: 320px;

    .bank-avatar-orb {
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

      .bank-icon {
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

    .bank-meta-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 0;

      .bank-title-line {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .bank-title {
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
      }

      /* 描述 + 更新时间 / 维护状态同一行排列，空间不足时自动换行 */
      .bank-summary-line {
        display: flex;
        align-items: center;
        gap: 14px;
        flex-wrap: wrap;
        min-width: 0;
      }

      .bank-description {
        margin: 0;
        font-size: 13.5px;
        color: #64748b;
        line-height: 1.6;
        flex: 0 1 auto;
        min-width: 0;
      }

      .bank-time-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0;
        font-size: 12.5px;
        color: #94a3b8;

        .time-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          white-space: nowrap;

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

  /* 底部操作条：整行铺满，工具组在左、主操作组在右 */
  .header-actions-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    width: 100%;
    margin-top: 18px;
    padding-top: 16px;
    border-top: 1px solid rgba(226, 232, 240, 0.8);

    @media (max-width: 1080px) {
      justify-content: flex-start;
    }

    .actions-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      min-width: 0;
    }

    .action-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      height: 32px;
      padding: 0 12px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      outline: none;
      user-select: none;
      white-space: nowrap;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .el-icon {
        font-size: 13px;
      }

      /* 品牌主蓝：录入新题 */
      &--brand {
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.22);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
          background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
        }
      }

      /* 组卷操作：白底描边蓝字 */
      &--compose {
        background: #ffffff;
        color: #1e40af;
        border: 1px solid #93c5fd;
        box-shadow: 0 1px 4px rgba(37, 99, 235, 0.06);

        &:hover {
          background: #eff6ff;
          border-color: #3b82f6;
          color: #1d4ed8;
          transform: translateY(-1px);
          box-shadow: 0 3px 10px rgba(37, 99, 235, 0.14);
        }
      }

      /* AI 专属紫蓝渐变 */
      &--ai {
        background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%);
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(124, 58, 237, 0.22);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(124, 58, 237, 0.32);
        }
      }

      /* 次要幽灵按钮 */
      &--ghost {
        background: #ffffff;
        color: #475569;
        border: 1px solid #dbe3ec;

        &:hover {
          background: #f8fafc;
          border-color: #94a3b8;
          color: #1e293b;
          transform: translateY(-1px);
        }
      }

      /* 危险删除按钮 */
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
}
</style>
