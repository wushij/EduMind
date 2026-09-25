<template>
  <div v-loading="loading" class="exam-list-content">
    <div v-if="exams.length > 0" class="exams-cards-grid">
      <div
        v-for="exam in exams"
        :key="exam.id"
        class="exam-paper-card"
      >
        <div class="card-header-line">
          <div class="header-tags">
            <span class="course-pill">
              <el-icon class="mr-1 text-blue-600"><Reading /></el-icon>
              {{ exam.courseName }}
            </span>
            <span class="semester-pill">{{ exam.semester }}</span>
          </div>
          <span class="status-pill status-pill--ready">
            <span class="dot-green"></span>
            <span>已审核就绪</span>
          </span>
        </div>

        <h3 class="exam-title">{{ exam.title }}</h3>

        <div class="exam-specs-row">
          <div class="spec-pill-item">
            <span class="spec-label">卷面满分：</span>
            <strong class="spec-val highlight">{{ exam.totalScore }} 分</strong>
          </div>
          <div class="spec-pill-item">
            <span class="spec-label">考试时长：</span>
            <strong class="spec-val">{{ exam.durationMinutes }} 分钟</strong>
          </div>
          <div class="spec-pill-item">
            <span class="spec-label">及格分数线：</span>
            <strong class="spec-val">{{ exam.passScore }} 分</strong>
          </div>
          <div class="spec-pill-item">
            <span class="spec-label">收录大题：</span>
            <strong class="spec-val">{{ (exam.rules || []).length }} 大类</strong>
          </div>
        </div>

        <div class="rules-distribution-box">
          <div class="rules-title">试卷题型结构分布：</div>
          <div class="rules-tags-track">
            <span
              v-for="r in exam.rules || []"
              :key="r.type"
              class="rule-chip"
            >
              {{ r.label }}：{{ r.count }} 题 (每题 {{ r.scoreEach }} 分)
            </span>
          </div>
        </div>

        <div class="card-footer-actions">
          <button
            type="button"
            class="capsule-action-btn capsule-action-btn--secondary"
            @click="emit('export-pdf', exam)"
          >
            <el-icon><Download /></el-icon>
            <span>导出 PDF 卷面</span>
          </button>

          <button
            type="button"
            class="capsule-action-btn capsule-action-btn--preview"
            @click="emit('preview', exam)"
          >
            <el-icon><View /></el-icon>
            <span>卷面全景预览</span>
          </button>

          <button
            type="button"
            class="capsule-action-btn capsule-action-btn--primary"
            @click="emit('publish', exam)"
          >
            <el-icon><Promotion /></el-icon>
            <span>发布为在线测验</span>
          </button>

          <button
            type="button"
            class="table-action-pill table-action-pill--danger"
            @click="emit('delete', exam)"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-exams-panel">
      <div class="empty-emoji">
        <el-icon><Tickets /></el-icon>
      </div>
      <h3 class="empty-title">未找到匹配的试卷</h3>
      <p class="empty-text">当前筛选条件下暂无试卷，你可以点击下方按钮通过 AI 秒级生成一套完整试卷。</p>
      <button
        type="button"
        class="capsule-btn capsule-btn--ai"
        @click="routerInstance.push('/ai/exam/generate')"
      >
        <el-icon class="mr-1"><AiSparkleIcon /></el-icon>
        <span>立即使用 AI 智能组卷</span>
      </button>
    </div>

    <AppPagination
      class="exam-list-pagination"
      :page-num="pageNum"
      :page-size="pageSize"
      :total="total"
      @update:page-num="emit('update:pageNum', $event)"
      @update:page-size="emit('update:pageSize', $event)"
      @change="emit('page-change')"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  Reading,
  Download,
  View,
  Promotion,
  Tickets,
} from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { ExamPaper } from '@/types/question/exam';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

defineProps<{
  exams: ExamPaper[];
  loading: boolean;
  total: number;
  pageNum: number;
  pageSize: number;
}>();

const routerInstance = useRouter();

const emit = defineEmits<{
  'update:pageNum': [value: number];
  'update:pageSize': [value: number];
  'page-change': [];
  preview: [exam: ExamPaper];
  'export-pdf': [exam: ExamPaper];
  publish: [exam: ExamPaper];
  delete: [exam: ExamPaper];
}>();
</script>

<style scoped lang="scss">
.exam-list-content {
  :deep(.pagination-bar) {
    margin-top: 12px;
    padding-top: 12px;
    padding-bottom: 0;
  }

  .exams-cards-grid {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .exam-paper-card {
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #E2E8F0;
      padding: 24px 28px;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
      display: flex;
      flex-direction: column;
      gap: 16px;
      transition: all 0.22s ease;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 8px 24px rgba(22, 119, 255, 0.08);
      }

      .card-header-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;

        .header-tags {
          display: flex;
          align-items: center;
          gap: 8px;

          .course-pill {
            padding: 2px 12px;
            border-radius: 9999px;
            background: #EFF6FF;
            color: #1677FF;
            font-size: 12px;
            font-weight: 600;
          }

          .semester-pill {
            padding: 2px 10px;
            border-radius: 9999px;
            background: #F1F5F9;
            color: #64748B;
            font-size: 11.5px;
          }
        }

        .status-pill {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 2px 12px;
          border-radius: 9999px;
          font-size: 11.5px;
          font-weight: 600;

          &--ready {
            background: #ECFDF5;
            color: #059669;

            .dot-green {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10B981;
            }
          }
        }
      }

      .exam-title {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.4;
      }

      .exam-specs-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .spec-pill-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 14px;
          border-radius: 9999px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          font-size: 12px;
          color: #64748B;

          .spec-val {
            color: #1E293B;

            &.highlight {
              color: #1677FF;
              font-weight: 700;
            }
          }
        }
      }

      .rules-distribution-box {
        background: #F8FAFC;
        border-radius: 12px;
        padding: 12px 16px;
        border: 1px dashed #CBD5E1;

        .rules-title {
          font-size: 12px;
          color: #64748B;
          margin-bottom: 8px;
          font-weight: 600;
        }

        .rules-tags-track {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .rule-chip {
            padding: 3px 10px;
            border-radius: 9999px;
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            font-size: 11.5px;
            color: #334155;
          }
        }
      }

      .card-footer-actions {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
        padding-top: 8px;
        border-top: 1px solid #F1F5F9;

        .capsule-action-btn {
          height: 36px;
          padding: 0 18px;
          border-radius: 9999px;
          font-size: 12.5px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s;
          // <button> 默认 inline-block，图标与文字按基线排版会显得歪且没有间距，
          // 统一改成 flex 居中，间距由 gap 控制
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 6px;

          &--secondary {
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            color: #334155;

            &:hover {
              background: #F8FAFC;
              color: #1677FF;
              border-color: #93C5FD;
            }
          }

          &--preview {
            background: #EFF6FF;
            border: 1px solid #BFDBFE;
            color: #1677FF;

            &:hover {
              background: #1677FF;
              color: #FFFFFF;
            }
          }

          &--primary {
            background: #1677FF;
            border: none;
            color: #FFFFFF;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

            &:hover {
              background: #4096FF;
            }
          }
        }
      }
    }
  }

  .empty-exams-panel {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px dashed #CBD5E1;
    padding: 60px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .empty-emoji {
      font-size: 42px;
      margin-bottom: 12px;
    }

    .empty-title {
      font-size: 16.5px;
      font-weight: 700;
      color: #1E293B;
      margin: 0 0 8px 0;
    }

    .empty-text {
      font-size: 13.5px;
      color: #94A3B8;
      margin: 0 0 20px 0;
    }

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 42px;
      padding: 0 20px;
      border-radius: 9999px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.22s ease;

      &--ai {
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        box-shadow: 0 4px 14px rgba(114, 46, 209, 0.28);

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(114, 46, 209, 0.38);
        }
      }
    }
  }
}
</style>
