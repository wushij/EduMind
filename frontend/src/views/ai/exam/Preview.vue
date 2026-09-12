<template>
  <div class="exam-preview-page">
    <!-- 顶部操作栏 -->
    <div class="preview-action-bar">
      <div class="left-meta">
        <h1 class="page-title">试卷排版与预览</h1>
        <span class="pill-badge pill-badge--primary">{{ currentExam.courseName }}</span>
        <span class="pill-badge pill-badge--score">总分：{{ currentExam.totalScore }} 分</span>
        <span class="pill-badge pill-badge--time">时长：{{ currentExam.durationMinutes }} 分钟</span>
      </div>

      <div class="right-buttons">
        <button
          type="button"
          class="capsule-btn capsule-btn--default"
          @click="router.push('/ai/exam/generate')"
        >
          <span>← 返回修改组卷规则</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--primary"
          @click="handleSaveExam"
        >
          <span>保存试卷并归档入库</span>
          <el-icon><Check /></el-icon>
        </button>
      </div>
    </div>

    <!-- 高仿真标准化试卷纸质卡片 -->
    <div class="paper-sheet-card">
      <!-- 卷头信息区 -->
      <div class="paper-header-box">
        <div class="school-univ-title">EduMind 智教云 · 高等院校期末统一水平评估测试</div>
        <h2 class="exam-paper-title">{{ currentExam.title }}</h2>

        <div class="paper-rules-meta">
          <span>适用学期：{{ currentExam.semester }}</span>
          <span class="dot">·</span>
          <span>卷面满分：{{ currentExam.totalScore }} 分</span>
          <span class="dot">·</span>
          <span>考试时间：{{ currentExam.durationMinutes }} 分钟</span>
          <span class="dot">·</span>
          <span>考核形式：闭卷机考</span>
        </div>

        <!-- 考生信息填涂栏模拟 -->
        <div class="student-meta-filling-bar">
          <span class="fill-item">姓名：____________</span>
          <span class="fill-item">学号：____________</span>
          <span class="fill-item">班级：____________</span>
          <span class="fill-item score-fill">得分：______ / 100</span>
        </div>
      </div>

      <!-- 试卷大题内容区 -->
      <div class="exam-sections-body">
        <!-- 一、单选题大题 -->
        <div class="exam-part-section">
          <div class="part-header-row">
            <h3 class="part-title">
              一、单项选择题（本大题共 10 题，每题 3 分，共 30 分。在每小题列出的备选项中只有一项是最符合题目要求的）
            </h3>
          </div>

          <div class="part-questions-list">
            <div
              v-for="(q, index) in choiceQuestions"
              :key="q.id"
              class="exam-q-item"
            >
              <div class="q-header-line">
                <span class="q-index">{{ index + 1 }}.</span>
                <span class="q-stem">{{ q.stem }}</span>
                <span class="q-score">({{ q.score }}分)</span>

                <button
                  type="button"
                  class="swap-q-btn"
                  title="让 AI 从题库调取同考点替选题"
                  @click="swapQuestion(q.id)"
                >
                  <el-icon><Refresh /></el-icon>
                  <span>换一题</span>
                </button>
              </div>

              <div v-if="q.options" class="q-options-grid">
                <div
                  v-for="opt in q.options"
                  :key="opt.key"
                  class="q-opt-item"
                  :class="{ 'show-correct': opt.isCorrect }"
                >
                  <span class="opt-label">{{ opt.key }}.</span>
                  <span class="opt-text">{{ opt.content }}</span>
                </div>
              </div>

              <div class="q-analysis-mini">
                <span class="kp-pill">考点：{{ q.knowledgePointNames.join('、') }}</span>
                <span class="ans-pill">参考答案：{{ q.correctAnswer }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 二、简答与分析大题 -->
        <div class="exam-part-section">
          <div class="part-header-row">
            <h3 class="part-title">
              二、解答与算法推导分析题（本大题共 2 题，共 70 分。请写出必要的推导步骤、核心代码或分析依据）
            </h3>
          </div>

          <div class="part-questions-list">
            <div
              v-for="(q, index) in essayQuestions"
              :key="q.id"
              class="exam-q-item"
            >
              <div class="q-header-line">
                <span class="q-index">{{ index + 1 + choiceQuestions.length }}.</span>
                <span class="q-stem">{{ q.stem }}</span>
                <span class="q-score">({{ q.score }}分)</span>

                <button
                  type="button"
                  class="swap-q-btn"
                  @click="swapQuestion(q.id)"
                >
                  <el-icon><Refresh /></el-icon>
                  <span>换一题</span>
                </button>
              </div>

              <div class="essay-answer-box">
                <div class="ans-title">【评分标准与要点】</div>
                <div class="ans-body">{{ q.correctAnswer }}</div>
                <div class="ans-analysis">{{ q.analysis }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Refresh, Check } from '@element-plus/icons-vue';
import { useExamGenerate } from '@/composables/ai/useExamGenerate';

const router = useRouter();
const { currentExam, swapQuestion, saveExam } = useExamGenerate();

const choiceQuestions = computed(() => {
  return currentExam.value.questions.filter(
    (q) => q.type === 'SINGLE_CHOICE' || q.type === 'MULTIPLE_CHOICE'
  );
});

const essayQuestions = computed(() => {
  return currentExam.value.questions.filter((q) => q.type === 'SHORT_ANSWER');
});

async function handleSaveExam() {
  await saveExam();
}
</script>

<style scoped lang="scss">
.exam-preview-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;

  .preview-action-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
    background: #FFFFFF;
    border-radius: 18px;
    padding: 18px 24px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

    .left-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .page-title {
        margin: 0 8px 0 0;
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
      }

      .pill-badge {
        padding: 3px 12px;
        border-radius: 9999px; // 长圆跑道胶囊
        font-size: 12px;
        font-weight: 600;

        &--primary { background: #EAF3FF; color: #1677FF; }
        &--score { background: #FEF3C7; color: #D97706; }
        &--time { background: #F1F5F9; color: #475569; }
      }
    }

    .right-buttons {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .capsule-btn {
    height: 40px;
    padding: 0 20px;
    border-radius: 9999px; // 纯正长圆
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.22s ease;
    white-space: nowrap;

    &--default {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      color: #475569;

      &:hover {
        border-color: #CBD5E1;
        color: #1677FF;
      }
    }

    &--primary {
      background: #1677FF;
      border: none;
      color: #FFFFFF;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.28);

      &:hover {
        background: #4096FF;
        transform: translateY(-1px);
      }
    }
  }

  // 纸质试卷卡片
  .paper-sheet-card {
    background: #FFFFFF;
    border-radius: 20px;
    padding: 48px 56px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 8px 30px rgba(15, 23, 42, 0.08);

    .paper-header-box {
      text-align: center;
      padding-bottom: 24px;
      border-bottom: 2px solid #0F172A;
      margin-bottom: 32px;

      .school-univ-title {
        font-size: 14px;
        color: #64748B;
        font-weight: 600;
        letter-spacing: 1px;
        margin-bottom: 8px;
      }

      .exam-paper-title {
        margin: 0 0 12px 0;
        font-size: 24px;
        font-weight: 800;
        color: #0F172A;
      }

      .paper-rules-meta {
        font-size: 13px;
        color: #64748B;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        margin-bottom: 20px;
      }

      .student-meta-filling-bar {
        display: flex;
        align-items: center;
        justify-content: space-around;
        padding: 10px 20px;
        border: 1px dashed #CBD5E1;
        border-radius: 10px;
        background: #F8FAFC;
        font-size: 13.5px;
        color: #334155;

        .score-fill {
          font-weight: 700;
          color: #DC2626;
        }
      }
    }

    .exam-sections-body {
      display: flex;
      flex-direction: column;
      gap: 36px;

      .exam-part-section {
        .part-header-row {
          margin-bottom: 18px;
          .part-title {
            margin: 0;
            font-size: 15.5px;
            font-weight: 700;
            color: #0F172A;
            line-height: 1.5;
          }
        }

        .part-questions-list {
          display: flex;
          flex-direction: column;
          gap: 20px;

          .exam-q-item {
            padding: 16px 20px;
            border-radius: 14px;
            background: #F8FAFC;
            border: 1px solid #EDF2F7;
            transition: all 0.2s;

            &:hover {
              border-color: #CBD5E1;
              background: #FFFFFF;
            }

            .q-header-line {
              display: flex;
              align-items: baseline;
              gap: 8px;
              margin-bottom: 12px;

              .q-index {
                font-weight: 800;
                color: #1677FF;
                font-size: 15px;
              }

              .q-stem {
                font-size: 14.5px;
                font-weight: 600;
                color: #1E293B;
                flex: 1;
                line-height: 1.5;
              }

              .q-score {
                font-size: 13px;
                color: #94A3B8;
                white-space: nowrap;
              }

              .swap-q-btn {
                display: inline-flex;
                align-items: center;
                gap: 4px;
                height: 26px;
                padding: 0 10px;
                border-radius: 9999px;
                background: #FFFFFF;
                border: 1px solid #CBD5E1;
                font-size: 11.5px;
                color: #475569;
                cursor: pointer;
                transition: all 0.2s;

                &:hover {
                  color: #1677FF;
                  border-color: #93C5FD;
                }
              }
            }

            .q-options-grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 8px;
              margin-bottom: 12px;

              .q-opt-item {
                display: flex;
                align-items: center;
                gap: 8px;
                padding: 8px 14px;
                border-radius: 9999px; // 长圆选项
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                font-size: 13px;
                color: #334155;

                &.show-correct {
                  background: #F0FDF4;
                  border-color: #86EFAC;
                  color: #166534;
                  font-weight: 600;
                }
              }
            }

            .q-analysis-mini {
              display: flex;
              align-items: center;
              gap: 12px;
              padding-top: 8px;
              border-top: 1px dashed #E2E8F0;

              .kp-pill,
              .ans-pill {
                font-size: 11.5px;
                color: #64748B;
              }
            }

            .essay-answer-box {
              background: #FFFFFF;
              border-radius: 12px;
              padding: 14px 18px;
              border: 1px solid #E2E8F0;

              .ans-title {
                font-size: 12px;
                font-weight: 700;
                color: #1677FF;
                margin-bottom: 4px;
              }

              .ans-body {
                font-size: 13px;
                color: #1E293B;
                font-weight: 500;
                margin-bottom: 6px;
              }

              .ans-analysis {
                font-size: 12.5px;
                color: #64748B;
                line-height: 1.5;
              }
            }
          }
        }
      }
    }
  }
}
</style>
