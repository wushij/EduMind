<template>
  <div class="step-content-box">
    <div class="review-paper-wrapper">
      <!-- 仿真实考试纸质卷面抬头 -->
      <div class="paper-sheet">
        <div class="paper-header-section">
          <div class="school-mark">EduMind 智教云 · 教学与考核标准化测试卷</div>
          <h2 class="paper-main-title">{{ examForm.title }}</h2>
          <div class="paper-sub-meta">
            <span>课程：{{ getCourseName(examForm.courseId) }}</span>
            <span>学期：{{ examForm.semester }}</span>
            <span>考试时限：{{ examForm.durationMinutes }} 分钟</span>
            <span>试卷总分：{{ currentTotalScore }} 分</span>
          </div>

          <!-- 考生装订信息栏 -->
          <div class="student-meta-seal-bar">
            <div class="seal-item">班级：__________________</div>
            <div class="seal-item">学号：__________________</div>
            <div class="seal-item">姓名：__________________</div>
            <div class="seal-item">考场座位：__________</div>
          </div>

          <div class="paper-notice-box">
            <strong>考生须知：</strong>{{ examForm.description || '请认真审题，在规定时间内独立作答，字迹清晰规范。' }}
          </div>
        </div>

        <!-- 各大题渲染 -->
        <div class="paper-sections-body">
          <div
            v-for="(sec, sIdx) in sections"
            :key="sec.id"
            class="paper-section-block"
          >
            <h3 class="paper-sec-heading">
              {{ getChineseNumber(sIdx + 1) }}、{{ sec.title }}
              <span class="sec-score-sum">（本大题共 {{ sec.questions.length }} 小题，共 {{ getSectionScore(sec) }} 分）</span>
            </h3>

            <div class="paper-questions-flow">
              <div
                v-for="(q, qIdx) in sec.questions"
                :key="q.id"
                class="paper-question-unit"
              >
                <div class="unit-stem">
                  <span class="unit-index">{{ qIdx + 1 }}.</span>
                  <span class="unit-text">{{ q.stem }}</span>
                  <span class="unit-score">（{{ q.score }}分）</span>
                </div>

                <!-- 选项展示 -->
                <div v-if="q.options && q.options.length > 0" class="unit-options-grid">
                  <div
                    v-for="opt in q.options"
                    :key="opt.key"
                    class="unit-option-item"
                  >
                    <span class="key">{{ opt.key }}.</span>
                    <span class="content">{{ opt.content }}</span>
                  </div>
                </div>

                <!-- 主观/问答题留白 -->
                <div
                  v-if="q.type === 'SHORT_ANSWER' || q.type === 'FILL_BLANK'"
                  class="paper-answer-blank"
                >
                  <div class="blank-lines">
                    <div class="line"></div>
                    <div class="line"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 审阅底部发布操作条 -->
      <div class="review-bottom-dock">
        <div class="dock-left">
          <el-radio-group v-model="publishStatus">
            <el-radio-button label="DRAFT">暂存为草稿 (后续可继续修改)</el-radio-button>
            <el-radio-button label="PUBLISHED">正式发布入库 (立即可用于测验)</el-radio-button>
          </el-radio-group>
        </div>
        <div class="dock-right">
          <el-button size="large" @click="currentStep = 1">返回选题编排</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="saving"
            class="submit-publish-btn"
            @click="$emit('save')"
          >
            <el-icon class="btn-icon">
              <Promotion v-if="publishStatus === 'PUBLISHED'" />
              <DocumentCopy v-else />
            </el-icon>
            <span>{{ publishStatus === 'PUBLISHED' ? '确认并正式发布试卷' : '保存试卷草稿' }}</span>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Promotion, DocumentCopy } from '@element-plus/icons-vue';
import type { ExamSection } from '@/composables/question/useExam';

defineProps<{
  examForm: {
    title: string;
    courseId?: number;
    semester: string;
    durationMinutes: number;
    description: string;
  };
  sections: ExamSection[];
  currentTotalScore: number;
  saving: boolean;
  getCourseName: (courseId?: number) => string;
  getSectionScore: (sec: ExamSection) => number;
  getChineseNumber: (num: number) => string;
}>();

defineEmits<{
  save: [];
}>();

const currentStep = defineModel<number>('currentStep', { required: true });
const publishStatus = defineModel<'DRAFT' | 'PUBLISHED'>('publishStatus', { required: true });
</script>

<style scoped lang="scss">
.review-paper-wrapper {
  width: 100%;
  max-width: none;
  margin: 0;

  .paper-sheet {
    background: #ffffff;
    border: 1px solid #d1d5db;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
    padding: 48px 64px;
    border-radius: 8px;
    font-family: 'Times New Roman', SimSun, serif;

    .paper-header-section {
      text-align: center;
      border-bottom: 2px solid #1e293b;
      padding-bottom: 24px;
      margin-bottom: 24px;

      .school-mark {
        font-size: 14px;
        font-weight: 600;
        color: #4b5563;
        letter-spacing: 2px;
        margin-bottom: 6px;
      }

      .paper-main-title {
        font-size: 24px;
        font-weight: 800;
        color: #111827;
        margin: 10px 0;
      }

      .paper-sub-meta {
        display: flex;
        justify-content: center;
        gap: 24px;
        font-size: 14px;
        color: #4b5563;
        margin: 8px 0 16px;
      }

      .student-meta-seal-bar {
        display: flex;
        justify-content: space-around;
        background: #f9fafb;
        border: 1px dashed #9ca3af;
        padding: 10px;
        border-radius: 6px;
        font-size: 13px;
        color: #374151;
        margin-bottom: 14px;
      }

      .paper-notice-box {
        font-size: 13px;
        color: #6b7280;
        text-align: left;
        background: #f8fafc;
        padding: 8px 12px;
        border-radius: 6px;
      }
    }

    .paper-sections-body {
      .paper-section-block {
        margin-bottom: 28px;

        .paper-sec-heading {
          font-size: 17px;
          font-weight: 700;
          color: #111827;
          border-bottom: 1px solid #e5e7eb;
          padding-bottom: 6px;
          margin-bottom: 14px;

          .sec-score-sum {
            font-size: 14px;
            font-weight: normal;
            color: #4b5563;
          }
        }

        .paper-questions-flow {
          display: flex;
          flex-direction: column;
          gap: 16px;

          .paper-question-unit {
            .unit-stem {
              font-size: 15px;
              line-height: 1.6;
              color: #1f2937;

              .unit-index {
                font-weight: 700;
                margin-right: 4px;
              }

              .unit-score {
                color: #6b7280;
                font-size: 13px;
              }
            }

            .unit-options-grid {
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: 8px 24px;
              margin-top: 8px;
              padding-left: 18px;

              .unit-option-item {
                font-size: 14px;
                color: #374151;

                .key {
                  font-weight: 600;
                  margin-right: 4px;
                }
              }
            }

            .paper-answer-blank {
              margin-top: 12px;
              padding-left: 18px;

              .blank-lines {
                display: flex;
                flex-direction: column;
                gap: 18px;

                .line {
                  border-bottom: 1px dashed #d1d5db;
                  height: 1px;
                }
              }
            }
          }
        }
      }
    }
  }

  .review-bottom-dock {
    margin-top: 24px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 16px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);

    .dock-right {
      display: flex;
      gap: 12px;
    }
  }
}
</style>
