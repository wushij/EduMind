<template>
  <div class="step-content-box composition-workspace">
    <!-- 左侧大题面板 -->
    <div class="sections-column">
      <div class="section-actions-bar">
        <div class="bar-title">
          <span class="badge">结构列表</span>
          <span>试卷大题大纲（共 {{ sections.length }} 个大题）</span>
        </div>
        <el-dropdown @command="handleAddSection">
          <el-button type="primary" plain size="small">
            <el-icon class="mr-1"><Plus /></el-icon> 增加大题类型
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="SINGLE_CHOICE">单项选择题</el-dropdown-item>
              <el-dropdown-item command="MULTIPLE_CHOICE">多项选择题</el-dropdown-item>
              <el-dropdown-item command="TRUE_FALSE">判断题</el-dropdown-item>
              <el-dropdown-item command="FILL_BLANK">填空题</el-dropdown-item>
              <el-dropdown-item command="SHORT_ANSWER">简答与问答题</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="sections-stack">
        <div
          v-for="(sec, sIdx) in sections"
          :key="sec.id"
          class="section-block-card"
        >
          <!-- 大题标题与设置行 -->
          <div class="section-card-header">
            <div class="sec-title-area">
              <span class="sec-order-tag">第{{ sIdx + 1 }}大题</span>
              <el-input
                v-model="sec.title"
                placeholder="大题名称"
                class="sec-title-input"
                size="small"
              />
              <el-tag size="small" :type="getTypeTagType(sec.type)">
                {{ getTypeLabel(sec.type) }}
              </el-tag>
            </div>

            <div class="sec-meta-area">
              <span class="meta-item">
                每题默认
                <el-input-number
                  v-model="sec.defaultScore"
                  :min="1"
                  :max="100"
                  size="small"
                  class="score-input"
                  @change="updateSectionDefaultScore(sec)"
                />
                分
              </span>
              <el-button
                type="primary"
                link
                size="small"
                :icon="Plus"
                @click="openQuestionPicker(sec)"
              >
                选题入卷 ({{ sec.questions.length }})
              </el-button>
              <el-popconfirm
                title="确认删除该大题及其包含的所有试题吗？"
                @confirm="removeSection(sIdx)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">删除大题</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>

          <!-- 该大题下的试题清单 -->
          <div class="section-questions-list">
            <div
              v-for="(q, qIdx) in sec.questions"
              :key="q.id"
              class="paper-question-row"
            >
              <span class="q-seq">{{ qIdx + 1 }}.</span>
              <div class="q-content">
                <p class="q-stem">{{ q.stem }}</p>
                <div class="q-inline-tags">
                  <span class="q-diff-badge" :class="q.difficulty?.toLowerCase()">
                    {{ getDifficultyLabel(q.difficulty) }}
                  </span>
                  <span v-for="kp in q.knowledgePointNames" :key="kp" class="q-kp-badge">
                    {{ kp }}
                  </span>
                </div>
              </div>

              <div class="q-score-adjust">
                <el-input-number
                  v-model="q.score"
                  :min="1"
                  :max="50"
                  size="small"
                  class="item-score-input"
                  @change="calculateScores"
                />
                <span class="unit">分</span>
                <el-button
                  type="danger"
                  link
                  size="small"
                  :icon="Delete"
                  @click="removeQuestionFromSection(sec, qIdx)"
                />
              </div>
            </div>

            <div v-if="sec.questions.length === 0" class="sec-empty-hint">
              暂未选择试题，请点击右上角「选题入卷」从课程题库中挑选试题。
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧试卷概览看板 (Sticky) -->
    <div class="summary-column">
      <el-card shadow="never" class="summary-sticky-card">
        <div class="summary-card-title">
          <span>试卷实时指标</span>
          <el-tag type="success" size="small">动态核算</el-tag>
        </div>

        <div class="score-display-box">
          <span class="score-number">{{ currentTotalScore }}</span>
          <span class="score-unit">/ {{ examForm.totalScore }} 分</span>
        </div>

        <div class="progress-bar-wrap">
          <el-progress
            :percentage="Math.min(100, Math.round((currentTotalScore / examForm.totalScore) * 100))"
            :color="currentTotalScore === examForm.totalScore ? '#10b981' : '#3b82f6'"
          />
          <span class="progress-tip">
            <template v-if="currentTotalScore === examForm.totalScore">
              <el-icon class="tip-success-icon"><CircleCheckFilled /></el-icon>
              <span>已达到预期满分</span>
            </template>
            <template v-else>
              还差 {{ examForm.totalScore - currentTotalScore }} 分达到试卷满分
            </template>
          </span>
        </div>

        <div class="summary-stats-list">
          <div class="stat-row">
            <span class="label">已选大题数</span>
            <span class="val">{{ sections.length }} 个</span>
          </div>
          <div class="stat-row">
            <span class="label">已录入总题数</span>
            <span class="val">{{ currentTotalQuestions }} 道</span>
          </div>
          <div class="stat-row">
            <span class="label">预估考试时长</span>
            <span class="val">{{ examForm.durationMinutes }} 分钟</span>
          </div>
          <div class="stat-row">
            <span class="label">及格合格线</span>
            <span class="val">{{ examForm.passScore }} 分</span>
          </div>
        </div>

        <!-- 题型分布占比 -->
        <div class="distribution-section">
          <span class="dist-title">题型分值分布</span>
          <div class="dist-bars">
            <div
              v-for="sec in sections"
              :key="sec.id"
              class="dist-item"
            >
              <div class="dist-label">
                <span>{{ sec.title }} ({{ sec.questions.length }}题)</span>
                <span>{{ getSectionScore(sec) }} 分</span>
              </div>
              <div class="dist-mini-bar">
                <div
                  class="fill"
                  :style="{ width: currentTotalScore ? `${(getSectionScore(sec) / currentTotalScore) * 100}%` : '0%' }"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="summary-actions">
          <el-button class="w-full mb-2" @click="currentStep = 0">
            上一步：修改基本信息
          </el-button>
          <el-button type="primary" class="w-full" size="large" @click="$emit('next')">
            <span>下一步：审阅与发布</span>
            <el-icon class="ml-1"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 弹窗：从题库中挑选试题入卷 -->
    <el-dialog
      v-model="pickerVisible"
      :title="`为【${activeSection?.title || '大题'}】挑选入卷试题`"
      width="780px"
      destroy-on-close
    >
      <div class="picker-modal-inner">
        <div class="picker-filter-row">
          <el-input
            v-model="pickerKeyword"
            placeholder="搜索试题关键词、知识点..."
            clearable
            :prefix-icon="Search"
            class="flex-1"
          />
          <el-select v-model="pickerDifficulty" placeholder="难度" clearable style="width: 120px">
            <el-option label="全部难度" value="" />
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </div>

        <div class="picker-list-area">
          <div
            v-for="q in filteredCandidateQuestions"
            :key="q.id"
            class="picker-question-item"
            :class="{ selected: selectedPickerIds.includes(q.id) }"
            @click="togglePickerItem(q.id)"
          >
            <el-checkbox
              :model-value="selectedPickerIds.includes(q.id)"
              @click.stop
              @change="togglePickerItem(q.id)"
            />
            <div class="picker-item-main">
              <div class="badge-line">
                <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="getDifficultyTagType(q.difficulty)">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
                <span class="picker-score">建议 {{ q.score || 5 }} 分</span>
              </div>
              <p class="picker-stem">{{ q.stem }}</p>
            </div>
          </div>

          <div v-if="filteredCandidateQuestions.length === 0" class="picker-empty">
            没有符合条件的该题型备选试题。
          </div>
        </div>
      </div>

      <template #footer>
        <div class="picker-footer">
          <span>已选中 <strong>{{ selectedPickerIds.length }}</strong> 道试题</span>
          <div>
            <el-button @click="pickerVisible = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="selectedPickerIds.length === 0"
              @click="confirmAddPickedQuestions"
            >
              添加选中的题目到本大题
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  ArrowRight,
  Plus,
  Delete,
  Search,
  CircleCheckFilled
} from '@element-plus/icons-vue';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';
import type { ExamSection } from '@/composables/question/useExam';

defineProps<{
  sections: ExamSection[];
  examForm: {
    totalScore: number;
    durationMinutes: number;
    passScore: number;
  };
  currentTotalScore: number;
  currentTotalQuestions: number;
  activeSection: ExamSection | null;
  selectedPickerIds: (number | string)[];
  filteredCandidateQuestions: QuestionItem[];
  handleAddSection: (type: QuestionType) => void;
  updateSectionDefaultScore: (sec: ExamSection) => void;
  calculateScores: () => void;
  removeSection: (idx: number) => void;
  removeQuestionFromSection: (sec: ExamSection, qIdx: number) => void;
  openQuestionPicker: (sec: ExamSection) => void;
  togglePickerItem: (id: number | string) => void;
  confirmAddPickedQuestions: () => void;
  getSectionScore: (sec: ExamSection) => number;
  getTypeLabel: (type: QuestionType | string) => string;
  getTypeTagType: (type: QuestionType | string) => string;
  getDifficultyLabel: (diff: Difficulty | string) => string;
  getDifficultyTagType: (diff: Difficulty | string) => string;
}>();

defineEmits<{
  next: [];
}>();

const currentStep = defineModel<number>('currentStep', { required: true });
const pickerVisible = defineModel<boolean>('pickerVisible', { required: true });
const pickerKeyword = defineModel<string>('pickerKeyword', { required: true });
const pickerDifficulty = defineModel<string>('pickerDifficulty', { required: true });
</script>

<style scoped lang="scss">
.composition-workspace {
  display: flex;
  gap: 24px;
  align-items: flex-start;

  .sections-column {
    flex: 1;

    .section-actions-bar {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 14px 20px;
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;

      .bar-title {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 15px;
        font-weight: 600;
        color: #1e293b;

        .badge {
          background: #eff6ff;
          color: #2563eb;
          font-size: 12px;
          padding: 2px 8px;
          border-radius: 4px;
        }
      }
    }

    .sections-stack {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .section-block-card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 14px;
        padding: 18px 20px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

        .section-card-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-bottom: 14px;
          border-bottom: 1px solid #f1f5f9;
          margin-bottom: 14px;
          flex-wrap: wrap;
          gap: 12px;

          .sec-title-area {
            display: flex;
            align-items: center;
            gap: 10px;

            .sec-order-tag {
              font-weight: 700;
              color: #2563eb;
              font-size: 14px;
            }

            .sec-title-input {
              width: 160px;
            }
          }

          .sec-meta-area {
            display: flex;
            align-items: center;
            gap: 14px;

            .meta-item {
              font-size: 13px;
              color: #64748b;
              display: inline-flex;
              align-items: center;
              gap: 6px;

              .score-input {
                width: 90px;
              }
            }
          }
        }

        .section-questions-list {
          display: flex;
          flex-direction: column;
          gap: 10px;

          .paper-question-row {
            background: #f8fafc;
            border: 1px solid #edf2f7;
            border-radius: 8px;
            padding: 10px 14px;
            display: flex;
            align-items: flex-start;
            gap: 12px;

            .q-seq {
              font-weight: 700;
              color: #64748b;
              font-size: 14px;
              margin-top: 2px;
            }

            .q-content {
              flex: 1;

              .q-stem {
                margin: 0 0 6px;
                font-size: 14px;
                line-height: 1.5;
                color: #1e293b;
              }

              .q-inline-tags {
                display: flex;
                gap: 6px;

                .q-diff-badge {
                  font-size: 11px;
                  padding: 1px 6px;
                  border-radius: 4px;

                  &.easy { background: #dcfce7; color: #166534; }
                  &.medium { background: #fef9c3; color: #854d0e; }
                  &.hard { background: #fee2e2; color: #991b1b; }
                }

                .q-kp-badge {
                  background: #e2e8f0;
                  color: #475569;
                  font-size: 11px;
                  padding: 1px 6px;
                  border-radius: 4px;
                }
              }
            }

            .q-score-adjust {
              display: flex;
              align-items: center;
              gap: 6px;

              .item-score-input {
                width: 90px;
              }

              .unit {
                font-size: 12px;
                color: #64748b;
              }
            }
          }

          .sec-empty-hint {
            padding: 24px;
            text-align: center;
            color: #94a3b8;
            font-size: 13px;
            background: #fafafa;
            border-radius: 8px;
            border: 1px dashed #e2e8f0;
          }
        }
      }
    }
  }

  .summary-column {
    width: 320px;
    flex-shrink: 0;

    .summary-sticky-card {
      position: sticky;
      top: 80px;
      background: #ffffff;
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
      padding: 16px;

      .summary-card-title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-weight: 700;
        font-size: 15px;
        color: #0f172a;
        margin-bottom: 16px;
      }

      .score-display-box {
        text-align: center;
        margin-bottom: 12px;

        .score-number {
          font-size: 40px;
          font-weight: 800;
          color: #2563eb;
          line-height: 1;
        }

        .score-unit {
          font-size: 16px;
          color: #64748b;
          margin-left: 4px;
        }
      }

      .progress-bar-wrap {
        margin-bottom: 20px;

        .progress-tip {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 4px;
          margin-top: 6px;
          font-size: 12px;
          color: #64748b;
          text-align: center;

          .tip-success-icon {
            font-size: 14px;
            color: #10b981;
          }
        }
      }

      .summary-stats-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding-bottom: 16px;
        border-bottom: 1px solid #f1f5f9;
        margin-bottom: 16px;

        .stat-row {
          display: flex;
          justify-content: space-between;
          font-size: 13px;

          .label {
            color: #64748b;
          }

          .val {
            font-weight: 600;
            color: #1e293b;
          }
        }
      }

      .distribution-section {
        margin-bottom: 20px;

        .dist-title {
          font-size: 13px;
          font-weight: 600;
          color: #334155;
          display: block;
          margin-bottom: 10px;
        }

        .dist-bars {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .dist-item {
            .dist-label {
              display: flex;
              justify-content: space-between;
              font-size: 12px;
              color: #64748b;
              margin-bottom: 3px;
            }

            .dist-mini-bar {
              height: 6px;
              background: #f1f5f9;
              border-radius: 9999px;
              overflow: hidden;

              .fill {
                height: 100%;
                background: #3b82f6;
                border-radius: 9999px;
              }
            }
          }
        }
      }

      .summary-actions {
        display: flex;
        flex-direction: column;
      }
    }
  }
}

.picker-modal-inner {
  .picker-filter-row {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .picker-list-area {
    max-height: 460px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .picker-question-item {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 12px 14px;
      display: flex;
      gap: 12px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #f8fafc;
        border-color: #cbd5e1;
      }

      &.selected {
        border-color: #3b82f6;
        background: #eff6ff;
      }

      .picker-item-main {
        flex: 1;

        .badge-line {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 6px;

          .picker-score {
            font-size: 12px;
            color: #2563eb;
            font-weight: 600;
          }
        }

        .picker-stem {
          font-size: 14px;
          color: #1e293b;
          line-height: 1.5;
          margin: 0;
        }
      }
    }

    .picker-empty {
      padding: 40px;
      text-align: center;
      color: #94a3b8;
      font-size: 14px;
    }
  }
}

.picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  strong {
    color: #2563eb;
    font-size: 16px;
  }
}
</style>
