<template>
<div class="paper-display-area">
        <!-- 模式切换与工具条 -->
        <div class="mode-switch-dock">
          <div class="left-toggles">
            <span class="dock-label">卷面视图模式：</span>
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="PAPER">
                <el-icon class="mr-1"><Document /></el-icon>
                <span>标准考生纸质试卷</span>
              </el-radio-button>
              <el-radio-button label="ANSWER_KEY">
                <el-icon class="mr-1"><View /></el-icon>
                <span>含参考答案及评分细则</span>
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="right-quick">
            <span class="q-count-hint">本试卷共 {{ totalQuestionsCount }} 道题目</span>
            <el-button link type="primary" size="small" @click="emit('print')">
              <el-icon class="mr-1"><Printer /></el-icon> 打印试卷
            </el-button>
          </div>
        </div>

        <!-- 试卷纸张 -->
        <div id="printable-exam-paper" class="exam-paper-sheet">
          <div class="sheet-head">
            <div class="school-header">EduMind 智教云 · 全数字化教学与智能评阅中心</div>
            <h2 class="exam-title-text">{{ examData?.title }}</h2>
            <div class="exam-subtitle-meta">
              <span>课程代码/名称：{{ examData?.courseName }}</span>
              <span>学期：{{ examData?.semester }}</span>
              <span>考试时限：{{ examData?.durationMinutes }}分钟</span>
              <span>满分：{{ examData?.totalScore }}分</span>
            </div>

            <div class="exam-seal-info-bar">
              <span>班级：____________________</span>
              <span>学号：____________________</span>
              <span>姓名：____________________</span>
              <span>考场座位号：________</span>
            </div>

            <div class="exam-instructions">
              <strong>考生答题规范与须知：</strong>
              <span>本试卷共 {{ groupedSections.length }} 个大题，请考生仔细核对试卷完整性，在规定答题纸或作答区域内规范作答。</span>
            </div>
          </div>

          <div class="sheet-body">
            <ExamPaperBody
              :grouped-sections="groupedSections"
              :view-mode="viewMode"
              skin="detail"
              :show-point-badge="true"
            />
          </div>
        </div>
      </div>
</template>

<script setup lang="ts">
import { Document, View, Printer } from '@element-plus/icons-vue';
import ExamPaperBody from '@/components/question/paper/ExamPaperBody.vue';
import type { ExamPaper } from '@/types/question/exam';
import type { GroupedSection } from '@/composables/question/useExam';

const viewMode = defineModel<'PAPER' | 'ANSWER_KEY'>('viewMode', { required: true });

defineProps<{
  examData: ExamPaper | null;
  groupedSections: GroupedSection[];
  totalQuestionsCount: number;
  getChineseNumber: (n: number) => string;
}>();

const emit = defineEmits<{
  print: [];
}>();
</script>

<style scoped lang="scss">
.paper-display-area {
  flex: 1;

  .mode-switch-dock {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 12px 20px;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .left-toggles {
      display: flex;
      align-items: center;
      gap: 12px;

      .dock-label {
        font-size: 13px;
        font-weight: 600;
        color: #334155;
      }
    }

    .right-quick {
      display: flex;
      align-items: center;
      gap: 16px;

      .q-count-hint {
        font-size: 13px;
        color: #64748b;
      }
    }
  }

  .exam-paper-sheet {
    background: #ffffff;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    padding: 48px 56px;
    font-family: 'Times New Roman', SimSun, serif;

    .sheet-head {
      text-align: center;
      border-bottom: 2px solid #1e293b;
      padding-bottom: 20px;
      margin-bottom: 24px;

      .school-header {
        font-size: 14px;
        letter-spacing: 2px;
        font-weight: 600;
        color: #4b5563;
      }

      .exam-title-text {
        font-size: 24px;
        font-weight: 800;
        color: #111827;
        margin: 10px 0;
      }

      .exam-subtitle-meta {
        display: flex;
        justify-content: center;
        gap: 20px;
        font-size: 13px;
        color: #4b5563;
        margin-bottom: 16px;
      }

      .exam-seal-info-bar {
        display: flex;
        justify-content: space-around;
        background: #f9fafb;
        border: 1px dashed #9ca3af;
        padding: 8px;
        border-radius: 4px;
        font-size: 13px;
        color: #374151;
        margin-bottom: 12px;
      }

      .exam-instructions {
        text-align: left;
        font-size: 13px;
        color: #6b7280;
        background: #f8fafc;
        padding: 8px 12px;
        border-radius: 4px;
      }
    }

    .sheet-body {
      .sheet-section-block {
        margin-bottom: 28px;

        .section-title-line {
          font-size: 17px;
          font-weight: 700;
          color: #111827;
          border-bottom: 1px solid #e5e7eb;
          padding-bottom: 6px;
          margin-bottom: 16px;

          .sec-score-info {
            font-size: 13px;
            font-weight: normal;
            color: #4b5563;
          }
        }

        .section-questions {
          display: flex;
          flex-direction: column;
          gap: 18px;

          .paper-question-card {
            .stem-line {
              font-size: 15px;
              line-height: 1.6;
              color: #1f2937;

              .q-index {
                font-weight: 700;
                margin-right: 4px;
              }

              .q-score-tag {
                color: #6b7280;
                font-size: 13px;
              }
            }

            .options-grid {
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: 8px 24px;
              margin-top: 10px;
              padding-left: 18px;

              .option-item {
                font-size: 14px;
                color: #374151;
                display: flex;
                align-items: center;
                gap: 6px;

                .opt-key {
                  font-weight: 600;
                }

                &.is-correct-answer {
                  color: #15803d;
                  font-weight: 600;

                  .correct-badge {
                    font-size: 11px;
                    background: #dcfce7;
                    color: #166534;
                    padding: 1px 6px;
                    border-radius: 4px;
                  }
                }
              }
            }

            .answer-key-box {
              background: #f8fafc;
              border-radius: 6px;
              padding: 10px 14px;
              margin-top: 10px;
              border-left: 3px solid #10b981;
              font-size: 13px;

              .ans-row {
                margin-bottom: 4px;
                line-height: 1.5;

                &:last-child {
                  margin-bottom: 0;
                }

                .ans-title {
                  font-weight: 600;
                  color: #334155;
                }

                .kps-tags {
                  display: inline-flex;
                  gap: 6px;

                  .kp-pill {
                    background: #e2e8f0;
                    color: #475569;
                    font-size: 11px;
                    padding: 1px 6px;
                    border-radius: 4px;
                  }
                }
              }
            }

            .student-blank-area {
              margin-top: 10px;
              padding-left: 18px;

              .answer-guide {
                font-size: 12px;
                color: #9ca3af;
                margin-bottom: 6px;
              }

              .ruled-lines {
                display: flex;
                flex-direction: column;
                gap: 16px;

                .line {
                  border-bottom: 1px dashed #e5e7eb;
                  height: 1px;
                }
              }
            }
          }
        }
      }
    }
  }
}


</style>
