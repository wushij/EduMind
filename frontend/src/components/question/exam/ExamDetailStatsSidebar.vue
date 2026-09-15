<template>
<div class="paper-sidebar-area">
        <el-card shadow="never" class="sidebar-card">
          <h3 class="side-card-title">
            <el-icon class="title-icon"><TrendCharts /></el-icon>
            <span>试卷难度与考点画像</span>
          </h3>

          <div class="side-stat-row">
            <span class="label">试卷大题类别</span>
            <span class="val">{{ groupedSections.length }} 类</span>
          </div>
          <div class="side-stat-row">
            <span class="label">入卷试题总量</span>
            <span class="val font-bold text-blue-600">{{ totalQuestionsCount }} 道</span>
          </div>
          <div class="side-stat-row">
            <span class="label">及格要求比例</span>
            <span class="val font-semibold text-emerald-600">
              {{ Math.round(((examData?.passScore || 60) / (examData?.totalScore || 100)) * 100) }}%
            </span>
          </div>

          <el-divider class="my-3" />

          <!-- 难度配比 -->
          <div class="difficulty-breakdown">
            <span class="sub-label">难度梯度构成</span>
            <div class="diff-bars-stack">
              <div class="diff-bar-item">
                <div class="diff-header">
                  <span class="diff-name text-emerald-600">简单题</span>
                  <span class="diff-count">{{ difficultyCounts.EASY }} 题 ({{ difficultyPercentages.EASY }}%)</span>
                </div>
                <el-progress :percentage="difficultyPercentages.EASY" color="#10b981" :show-text="false" />
              </div>

              <div class="diff-bar-item">
                <div class="diff-header">
                  <span class="diff-name text-amber-600">中等题</span>
                  <span class="diff-count">{{ difficultyCounts.MEDIUM }} 题 ({{ difficultyPercentages.MEDIUM }}%)</span>
                </div>
                <el-progress :percentage="difficultyPercentages.MEDIUM" color="#f59e0b" :show-text="false" />
              </div>

              <div class="diff-bar-item">
                <div class="diff-header">
                  <span class="diff-name text-red-600">困难题</span>
                  <span class="diff-count">{{ difficultyCounts.HARD }} 题 ({{ difficultyPercentages.HARD }}%)</span>
                </div>
                <el-progress :percentage="difficultyPercentages.HARD" color="#ef4444" :show-text="false" />
              </div>
            </div>
          </div>

          <el-divider class="my-3" />

          <!-- 覆盖核心知识点清单 -->
          <div class="kps-breakdown">
            <span class="sub-label">覆盖核心知识点 ({{ coveredKnowledgePoints.length }} 个)</span>
            <div class="kps-chips-flow">
              <span v-for="kp in coveredKnowledgePoints" :key="kp" class="kp-badge-chip">
                {{ kp }}
              </span>
            </div>
          </div>
        </el-card>
      </div>
</template>

<script setup lang="ts">
import { TrendCharts } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';
import type { GroupedSection } from '@/composables/question/useExam';

defineProps<{
  examData: ExamPaper | null;
  groupedSections: GroupedSection[];
  totalQuestionsCount: number;
  difficultyCounts: { EASY: number; MEDIUM: number; HARD: number };
  difficultyPercentages: { EASY: number; MEDIUM: number; HARD: number };
  coveredKnowledgePoints: string[];
}>();
</script>

<style scoped lang="scss">
.paper-sidebar-area {
  width: 320px;
  flex-shrink: 0;

  .sidebar-card {
    position: sticky;
    top: 80px;
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 16px;

    .side-card-title {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 16px;
      display: flex;
      align-items: center;
      gap: 6px;

      .title-icon {
        font-size: 17px;
        color: #2563eb;
      }
    }

    .side-stat-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      margin-bottom: 8px;

      .label {
        color: #64748b;
      }

      .val {
        color: #1e293b;
      }
    }

    .difficulty-breakdown {
      .sub-label {
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        display: block;
        margin-bottom: 10px;
      }

      .diff-bars-stack {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .diff-bar-item {
          .diff-header {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            margin-bottom: 4px;
          }
        }
      }
    }

    .kps-breakdown {
      .sub-label {
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        display: block;
        margin-bottom: 10px;
      }

      .kps-chips-flow {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .kp-badge-chip {
          background: #eff6ff;
          color: #2563eb;
          font-size: 12px;
          padding: 3px 8px;
          border-radius: 6px;
          font-weight: 500;
        }
      }
    }
  }
}
</style>
