<template>
  <div class="main-practice-container">
    <div v-if="!isPracticing && !isFinished" class="setup-layout">
      <el-card shadow="never" class="setup-card">
        <template #header>
          <div class="setup-header">
            <div class="setup-title">
              <el-icon class="title-icon"><Operation /></el-icon>
              <span>靶向练习生成配置</span>
            </div>
            <el-tag type="primary" effect="plain">当前模式：{{ currentModeName }}</el-tag>
          </div>
        </template>

        <el-form label-position="top" class="setup-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="目标课程">
                <el-select
                  :model-value="teacherCourseId"
                  placeholder="选择课程"
                  class="w-full"
                  @update:model-value="onTeacherCourseIdChange"
                >
                  <el-option
                    v-for="c in courseOptions"
                    :key="c.id"
                    :label="c.name"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="核心考点范围">
                <el-select
                  :model-value="selectedKpId"
                  placeholder="全范围自适应 / 指定考点"
                  class="w-full"
                  @update:model-value="onSelectedKpIdChange"
                >
                  <el-option label="全课程薄弱考点自适应聚合 (AI 推荐)" :value="0" />
                  <el-option label="第一章：极限存在准则与无穷小量" :value="101" />
                  <el-option label="第二章：导数定义与切线方程" :value="102" />
                  <el-option label="第三章：微分中值定理与洛必达法则" :value="103" />
                  <el-option label="第四章：不定积分换元与分部积分" :value="104" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="布鲁姆认知分层偏好">
            <el-radio-group
              :model-value="cognitiveLevel"
              class="cognitive-radios"
              @update:model-value="onCognitiveLevelChange"
            >
              <el-radio-button label="ALL">均衡演练 (全认知梯度)</el-radio-button>
              <el-radio-button label="UNDERSTAND">基础概念识记与理解</el-radio-button>
              <el-radio-button label="APPLY">综合计算与典型应用</el-radio-button>
              <el-radio-button label="EVALUATE">高阶探究与逆向推理</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="练习题量">
                <el-radio-group
                  :model-value="questionCount"
                  @update:model-value="onQuestionCountChange"
                >
                  <el-radio :label="3">3 题 (快速微测 · 5分钟)</el-radio>
                  <el-radio :label="5">5 题 (标准专项 · 12分钟)</el-radio>
                  <el-radio :label="10">10 题 (深度攻坚 · 25分钟)</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="即时反馈模式">
                <el-switch
                  :model-value="instantFeedback"
                  active-text="每题答完后立即展示 AI 认知解析与评分"
                  inactive-text="全部提交后统一生成分析报告"
                  @update:model-value="onInstantFeedbackChange"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="submit-action-row">
            <el-button
              type="primary"
              size="large"
              class="generate-btn"
              :loading="generating"
              @click="onStartPractice"
            >
              <el-icon><Compass /></el-icon>
              <span>AI 智能生成自适应练习集</span>
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>

    <div v-else-if="isPracticing" class="practice-active-layout">
      <div class="practice-header-hud">
        <div class="hud-left">
          <el-tag effect="dark" type="primary" class="q-progress-badge">
            第 {{ currentIndex + 1 }} / {{ questions.length }} 题
          </el-tag>
          <span class="q-type-label">{{ currentQuestion.typeText }}</span>
          <el-tag size="small" :type="getDifficultyTag(currentQuestion.difficulty)" effect="plain">
            {{ currentQuestion.difficulty }}
          </el-tag>
        </div>

        <div class="hud-center">
          <div class="nav-dots">
            <span
              v-for="(q, idx) in questions"
              :key="idx"
              class="nav-dot"
              :class="{
                active: idx === currentIndex,
                answered: userAnswers[idx] !== undefined,
                correct: answersState[idx] === 'CORRECT',
                wrong: answersState[idx] === 'WRONG'
              }"
              @click="onJumpToQuestion(idx)"
            >
              {{ idx + 1 }}
            </span>
          </div>
        </div>

        <div class="hud-right">
          <el-button type="danger" text @click="onConfirmExit">退出练习</el-button>
        </div>
      </div>

      <el-card shadow="never" class="quiz-card">
        <div class="stem-box">
          <span class="stem-no">Q{{ currentIndex + 1 }}.</span>
          <div class="stem-text">
            <MathText :text="currentQuestion.stem" />
          </div>
        </div>

        <div v-if="currentQuestion.options && currentQuestion.options.length" class="options-group">
          <div
            v-for="opt in currentQuestion.options"
            :key="opt.key"
            class="option-item"
            :class="{
              selected: userAnswers[currentIndex] === opt.key,
              'is-right': submittedCurrent && opt.key === currentQuestion.answer,
              'is-error': submittedCurrent && userAnswers[currentIndex] === opt.key && opt.key !== currentQuestion.answer
            }"
            @click="!submittedCurrent && onSelectOption(opt.key)"
          >
            <div class="option-key-circle">{{ opt.key }}</div>
            <div class="option-content">
              <MathText :text="opt.val" />
            </div>
          </div>
        </div>

        <div v-else class="text-answer-group">
          <el-input
            :model-value="userAnswers[currentIndex]"
            type="textarea"
            :rows="4"
            placeholder="请输入你的解答步骤与最终推导结果..."
            :disabled="submittedCurrent"
            @update:model-value="handleTextAnswer"
          />
        </div>

        <transition name="el-zoom-in-top">
          <div v-if="submittedCurrent" class="feedback-panel">
            <div class="feedback-header">
              <div class="status-indicator">
                <el-icon v-if="answersState[currentIndex] === 'CORRECT'" class="icon-right"><Check /></el-icon>
                <el-icon v-else class="icon-wrong"><Close /></el-icon>
                <span class="status-text">
                  {{ answersState[currentIndex] === 'CORRECT' ? '回答正确！思维逻辑严谨' : '回答有误，请注意概念辨析' }}
                </span>
              </div>
              <div class="kp-badge">
                <span>考点关联：{{ currentQuestion.kpTitle }}</span>
              </div>
            </div>

            <div class="analysis-body">
              <div class="reference-ans">
                <strong>参考标准答案：</strong>
                <MathText :text="currentQuestion.answer" />
              </div>
              <div class="ai-comment">
                <strong>AI 深度解析与变式溯源：</strong>
                <p>{{ currentQuestion.analysis }}</p>
              </div>
            </div>
          </div>
        </transition>

        <div class="quiz-controls-bar">
          <div class="left-action">
            <el-button
              v-if="currentIndex > 0"
              plain
              @click="onJumpToQuestion(currentIndex - 1)"
            >
              上一题
            </el-button>
          </div>

          <div class="right-action">
            <el-button
              v-if="!submittedCurrent && instantFeedback"
              type="primary"
              plain
              :disabled="userAnswers[currentIndex] === undefined"
              @click="onSubmitCurrentQuestion"
            >
              确认作答并查看 AI 解析
            </el-button>

            <el-button
              v-if="currentIndex < questions.length - 1"
              type="primary"
              @click="onJumpToQuestion(currentIndex + 1)"
            >
              下一题
            </el-button>

            <el-button
              v-else
              type="success"
              @click="onFinishPractice"
            >
              完成本次练习并生成报告
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else-if="isFinished" class="result-layout">
      <el-card shadow="never" class="result-card">
        <div class="result-header">
          <div class="score-circle">
            <span class="score-value">{{ finalScore }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="result-meta">
            <h3 class="result-title">本次自适应专项练习报告</h3>
            <p class="result-subtitle">
              共完成 {{ questions.length }} 道练习题，正确率 {{ accuracyRate }}%，用时约 {{ Math.round(usedSeconds / 60) || 1 }} 分钟。
            </p>
            <div class="stat-pills">
              <el-tag type="success">正确 {{ correctCount }} 题</el-tag>
              <el-tag type="danger">错误 {{ questions.length - correctCount }} 题</el-tag>
              <el-tag type="warning">认知掌握度提升 +{{ Math.round(correctCount * 3.5) }}%</el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="result-advice">
          <h4>
            <el-icon><ChatLineSquare /></el-icon>
            AI 学习导师建议
          </h4>
          <p>
            你在【{{ questions[0]?.kpTitle || '高等数学核心考点' }}】上的理解逐渐深入。对于复合函数求导链式法则掌握良好，但在极限等价代换与微分中值条件上偶有失误，已自动将错题收录进智能错题本，建议 48 小时后进行变式二轮复测。
          </p>
        </div>

        <div class="result-actions">
          <el-button size="large" @click="onResetToSetup">再练一组</el-button>
          <el-button size="large" type="primary" @click="onGoToWrongQuestions">
            查看错题本与深度归因
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Operation,
  Compass,
  Check,
  Close,
  ChatLineSquare
} from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import type { PracticeQuestion } from '@/composables/learning/useAIPractice';

const props = defineProps<{
  teacherCourseId: number;
  courseOptions: Array<{ id: number; name: string }>;
  selectedKpId: number;
  cognitiveLevel: string;
  questionCount: number;
  instantFeedback: boolean;
  generating: boolean;
  isPracticing: boolean;
  isFinished: boolean;
  currentIndex: number;
  questions: PracticeQuestion[];
  userAnswers: Record<number, string>;
  answersState: Record<number, 'CORRECT' | 'WRONG' | 'PENDING'>;
  usedSeconds: number;
  currentModeName: string;
  currentQuestion: PracticeQuestion;
  submittedCurrent: boolean;
  correctCount: number;
  finalScore: number;
  accuracyRate: number;
  getDifficultyTag: (difficulty: string) => string;
  onTeacherCourseIdChange: (id: number) => void;
  onSelectedKpIdChange: (id: number) => void;
  onCognitiveLevelChange: (level: string) => void;
  onQuestionCountChange: (count: number) => void;
  onInstantFeedbackChange: (value: boolean) => void;
  onStartPractice: () => void;
  onSelectOption: (key: string) => void;
  onTextAnswerChange: (index: number, value: string) => void;
  onSubmitCurrentQuestion: () => void;
  onJumpToQuestion: (index: number) => void;
  onFinishPractice: () => void;
  onConfirmExit: () => void;
  onResetToSetup: () => void;
  onGoToWrongQuestions: () => void;
}>();

function handleTextAnswer(value: string) {
  props.onTextAnswerChange(props.currentIndex, value);
}
</script>

<style scoped lang="scss">
.main-practice-container {
  .setup-card {
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;

    .setup-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .setup-title {
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
        display: flex;
        align-items: center;
        gap: 8px;

        .title-icon {
          color: #1677FF;
        }
      }
    }

    .setup-form {
      padding: 10px 0;

      .cognitive-radios {
        width: 100%;
      }

      .submit-action-row {
        display: flex;
        justify-content: center;
        margin-top: 24px;

        .generate-btn {
          padding: 14px 36px;
          font-size: 16px;
          font-weight: 700;
          border-radius: 9999px;
          background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
          border: none;
          box-shadow: 0 6px 20px rgba(22, 119, 255, 0.25);
        }
      }
    }
  }

  .practice-active-layout {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .practice-header-hud {
      background: #FFFFFF;
      border-radius: 14px;
      padding: 12px 20px;
      border: 1px solid #E2E8F0;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .hud-left {
        display: flex;
        align-items: center;
        gap: 10px;

        .q-progress-badge {
          font-weight: 700;
          border-radius: 9999px;
        }

        .q-type-label {
          font-weight: 600;
          font-size: 13px;
          color: #475569;
        }
      }

      .hud-center {
        .nav-dots {
          display: flex;
          gap: 8px;

          .nav-dot {
            width: 28px;
            height: 28px;
            border-radius: 50%;
            border: 1px solid #CBD5E1;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: 700;
            color: #64748B;
            cursor: pointer;
            transition: all 0.2s;

            &.active {
              border-color: #1677FF;
              color: #1677FF;
              transform: scale(1.15);
            }

            &.answered {
              background: #F1F5F9;
            }

            &.correct {
              background: #52C41A;
              border-color: #52C41A;
              color: #FFFFFF;
            }

            &.wrong {
              background: #F5222D;
              border-color: #F5222D;
              color: #FFFFFF;
            }
          }
        }
      }
    }

    .quiz-card {
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 10px;

      .stem-box {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        font-size: 16px;
        font-weight: 600;
        line-height: 1.7;
        color: #0F172A;
        margin-bottom: 24px;
        padding: 14px 18px;
        background: #F8FAFC;
        border-radius: 12px;
        border-left: 4px solid #1677FF;

        .stem-no {
          color: #1677FF;
        }
      }

      .options-group {
        display: flex;
        flex-direction: column;
        gap: 12px;
        margin-bottom: 24px;

        .option-item {
          display: flex;
          align-items: center;
          gap: 14px;
          padding: 12px 18px;
          border-radius: 10px;
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          cursor: pointer;
          transition: all 0.2s;

          .option-key-circle {
            width: 28px;
            height: 28px;
            border-radius: 50%;
            background: #F1F5F9;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            color: #475569;
            font-size: 13px;
          }

          &:hover {
            border-color: #93C5FD;
            background: #F8FAFC;
          }

          &.selected {
            border-color: #1677FF;
            background: #EFF6FF;

            .option-key-circle {
              background: #1677FF;
              color: #FFFFFF;
            }
          }

          &.is-right {
            border-color: #52C41A;
            background: #F6FFED;

            .option-key-circle {
              background: #52C41A;
              color: #FFFFFF;
            }
          }

          &.is-error {
            border-color: #F5222D;
            background: #FFF1F0;

            .option-key-circle {
              background: #F5222D;
              color: #FFFFFF;
            }
          }
        }
      }

      .feedback-panel {
        margin-top: 20px;
        border-radius: 12px;
        border: 1px solid #D6E4FF;
        background: #F4F8FF;
        padding: 16px 20px;

        .feedback-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .status-indicator {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 15px;
            font-weight: 700;

            .icon-right {
              color: #52C41A;
              font-size: 20px;
            }

            .icon-wrong {
              color: #F5222D;
              font-size: 20px;
            }
          }

          .kp-badge {
            font-size: 12px;
            color: #64748B;
          }
        }

        .analysis-body {
          font-size: 14px;
          line-height: 1.7;
          color: #334155;

          .reference-ans {
            margin-bottom: 8px;
          }
        }
      }

      .quiz-controls-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 24px;
        padding-top: 16px;
        border-top: 1px solid #F1F5F9;
      }
    }
  }

  .result-layout {
    .result-card {
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 24px;

      .result-header {
        display: flex;
        align-items: center;
        gap: 28px;

        .score-circle {
          width: 100px;
          height: 100px;
          border-radius: 50%;
          background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #FFFFFF;
          box-shadow: 0 8px 24px rgba(22, 119, 255, 0.25);

          .score-value {
            font-size: 38px;
            font-weight: 800;
          }

          .score-unit {
            font-size: 14px;
            margin-top: 12px;
          }
        }

        .result-meta {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .result-title {
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            color: #0F172A;
          }

          .result-subtitle {
            margin: 0;
            font-size: 14px;
            color: #64748B;
          }

          .stat-pills {
            display: flex;
            gap: 8px;
            margin-top: 4px;
          }
        }
      }

      .result-advice {
        background: #F8FAFC;
        border-radius: 12px;
        padding: 16px 20px;
        margin: 20px 0;

        h4 {
          margin: 0 0 8px;
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        p {
          margin: 0;
          font-size: 14px;
          line-height: 1.7;
          color: #475569;
        }
      }

      .result-actions {
        display: flex;
        justify-content: center;
        gap: 16px;
        margin-top: 24px;
      }
    }
  }
}
</style>
