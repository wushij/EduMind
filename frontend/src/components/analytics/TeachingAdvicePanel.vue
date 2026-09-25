<template>
  <div class="split-right-col">
    <div class="report-panel-card">
      <div class="panel-header-line">
        <div class="header-left">
          <el-icon class="panel-icon panel-icon--amber"><PieChart /></el-icon>
          <h3 class="panel-title">AI 学情错因聚类分析</h3>
        </div>
      </div>

      <div class="error-diagnosis-list">
        <div
          v-for="err in errorCategories"
          :key="err.type"
          class="error-cat-box"
        >
          <div class="error-top-line">
            <span class="err-name">{{ getErrorName(err) }}</span>
            <span class="err-percent">{{ err.percent }}% 占比</span>
          </div>
          <div class="capsule-progress-track">
            <div class="capsule-progress-fill" :style="{ width: `${err.percent}%`, background: err.color }"></div>
          </div>
          <p class="err-desc">{{ getErrorDesc(err) }}</p>
        </div>
      </div>
    </div>

    <div class="report-panel-card report-panel-card--ai-suggestion">
      <div class="panel-header-line">
        <div class="header-left">
          <el-icon class="panel-icon panel-icon--purple"><Opportunity /></el-icon>
          <h3 class="panel-title">AI 教学策略改进建议</h3>
        </div>
      </div>

      <div class="suggestion-content-box">
        <div class="suggestion-bubble">
          <div class="bot-avatar">
            <el-icon><Service /></el-icon>
          </div>
          <div class="bubble-text">
            <strong>课堂教学与改进建议：</strong>
            <p v-if="reportData && reportData.weakPoints && reportData.weakPoints.length > 0">
              根据近期做题轨迹与错因诊断分析，学生在<strong>《{{ topWeakPointNames }}》</strong>等考点存在较集中错因。建议在下阶段教学中安排 <strong>10~15 分钟典型数形辨析与变式巩固</strong>。
            </p>
            <p v-else>
              当前课程学生知识点掌握情况总体平稳，无显著集中性错因。建议持续关注平时作业订正率，并通过课程 AI 助教保持常态化答疑支持。
            </p>
          </div>
        </div>

        <div class="action-buttons-stack">
          <button
            type="button"
            class="capsule-card-action-btn"
            @click="goLessonStudio"
          >
            <el-icon class="btn-inner-icon"><DocumentAdd /></el-icon>
            <span>进入课节用 AI 备课生成针对性教案</span>
          </button>

          <button
            type="button"
            class="capsule-card-action-btn capsule-card-action-btn--secondary"
            @click="router.push(`/course/${courseId}/ai`)"
          >
            <el-icon class="btn-inner-icon"><ChatDotRound /></el-icon>
            <span>与课程 AI 助教研讨教学方案</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PieChart, Opportunity, Service, DocumentAdd, ChatDotRound } from '@element-plus/icons-vue';
import type { Router } from 'vue-router';
import type { TeachingReportVO } from '@/types/analytics/report';
import { openLessonStudio } from '@/services/course/lesson-studio-entry';

const props = defineProps<{
  router: Router;
  courseId: number;
  reportData: TeachingReportVO | null;
  topWeakPointNames: string;
  errorCategories: Array<{ type: string; name: string; percent: number; color: string; desc: string }>;
}>();

const errorDescMap: Record<string, string> = {
  CONCEPT: '对基础概念定义判定标准或充分必要条件认知模糊',
  CALC: '步骤繁琐导致的运算失误、符号漏算或恒等变形错误',
  LOGIC: '解题步骤推理跳跃、前后因果倒置或推导链断层',
  READING: '未准确提炼题设核心限定条件或忽略了隐含边界'
};

function getErrorDesc(err: { type: string; name: string; desc: string }): string {
  if (errorDescMap[err.type]) return errorDescMap[err.type];
  if (err.desc && err.desc !== err.name && err.desc !== err.type) return err.desc;
  return '典型薄弱项成因，需结合变式训练加深理解';
}

function getErrorName(err: { type: string; name: string }): string {
  if (err.type === 'READING') return '审题理解偏差';
  return err.name;
}

function goLessonStudio() {
  void openLessonStudio(props.router, props.courseId);
}
</script>

<style scoped lang="scss">
.split-right-col {
  .report-panel-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 22px 24px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    margin-bottom: 20px;

    .panel-header-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .panel-icon {
          font-size: 18px;
          display: inline-flex;
          align-items: center;
          justify-content: center;

          &--amber { color: #D97706; }
          &--purple { color: #722ED1; }
        }

        .panel-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }
      }
    }

    .error-diagnosis-list {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .error-cat-box {
        .error-top-line {
          display: flex;
          justify-content: space-between;
          font-size: 13px;
          margin-bottom: 6px;

          .err-name {
            font-weight: 600;
            color: #1E293B;
          }

          .err-percent {
            font-weight: 700;
            color: #1677FF;
          }
        }

        .capsule-progress-track {
          width: 100%;
          height: 6px;
          background: #E2E8F0;
          border-radius: 9999px;
          overflow: hidden;
          margin-bottom: 6px;

          .capsule-progress-fill {
            height: 100%;
            border-radius: 9999px;
          }
        }

        .err-desc {
          margin: 0;
          font-size: 11.5px;
          color: #94A3B8;
          line-height: 1.5;
        }
      }
    }

    &--ai-suggestion {
      background: linear-gradient(135deg, #FAF5FF 0%, #FFFFFF 100%);
      border-color: #E9D5FF;

      .suggestion-content-box {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .suggestion-bubble {
          display: flex;
          gap: 12px;

          .bot-avatar {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            background: linear-gradient(135deg, #722ED1 0%, #9333EA 100%);
            color: #FFFFFF;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            flex-shrink: 0;
            box-shadow: 0 4px 12px rgba(114, 46, 209, 0.25);
          }

          .bubble-text {
            font-size: 12.5px;
            color: #334155;
            line-height: 1.6;

            strong {
              color: #722ED1;
            }

            p {
              margin: 4px 0 0 0;
            }
          }
        }

        .action-buttons-stack {
          display: flex;
          flex-direction: column;
          gap: 10px;

          .capsule-card-action-btn {
            width: 100%;
            height: 38px;
            border-radius: 9999px;
            background: #722ED1;
            color: #FFFFFF;
            border: none;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 2px 8px rgba(114, 46, 209, 0.25);
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;

            .btn-inner-icon {
              font-size: 15px;
            }

            &:hover {
              background: #531DAB;
            }

            &--secondary {
              background: #FFFFFF;
              border: 1px solid #CBD5E1;
              color: #334155;
              box-shadow: none;

              &:hover {
                background: #F8FAFC;
                color: #1677FF;
              }
            }
          }
        }
      }
    }
  }
}
</style>
