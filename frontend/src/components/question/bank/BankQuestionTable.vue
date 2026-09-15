<template>
  <div class="bank-content-card">
    <div class="toolbar-wrapper">
      <div class="toolbar-left">
        <el-input
          :model-value="searchKeyword"
          placeholder="在题库内搜索题干关键词、知识点..."
          clearable
          class="search-input"
          :prefix-icon="Search"
          @update:model-value="onSearchKeywordChange"
        />
        <el-select
          :model-value="filterType"
          placeholder="题型筛选"
          clearable
          class="filter-select"
          @update:model-value="onFilterTypeChange"
        >
          <el-option label="全部题型" value="" />
          <el-option label="单选题" value="SINGLE_CHOICE" />
          <el-option label="多选题" value="MULTIPLE_CHOICE" />
          <el-option label="判断题" value="TRUE_FALSE" />
          <el-option label="填空题" value="FILL_BLANK" />
          <el-option label="简答题" value="SHORT_ANSWER" />
        </el-select>
        <el-select
          :model-value="filterDifficulty"
          placeholder="难度筛选"
          clearable
          class="filter-select"
          @update:model-value="onFilterDifficultyChange"
        >
          <el-option label="全部难度" value="" />
          <el-option label="简单" value="EASY" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="困难" value="HARD" />
        </el-select>
      </div>

      <div class="toolbar-right">
        <el-button
          v-if="selectedRowKeys.length > 0"
          type="danger"
          plain
          size="small"
          @click="onBatchRemove"
        >
          批量移出题库 ({{ selectedRowKeys.length }})
        </el-button>
        <span class="total-hint-text">共 {{ filteredQuestions.length }} 道符合条件的题目</span>
      </div>
    </div>

    <!-- 题库题目列表 -->
    <div class="questions-list-section">
      <div v-if="filteredQuestions.length > 0" class="question-items-stack">
        <div
          v-for="(item, index) in filteredQuestions"
          :key="item.id"
          class="question-bank-item-card"
        >
          <div class="item-checkbox-col">
            <el-checkbox
              :model-value="selectedRowKeys.includes(item.id)"
              @change="onToggleSelectRow(item.id)"
            />
            <span class="index-num">#{{ index + 1 }}</span>
          </div>

          <div class="item-body">
            <div class="item-badges-row">
              <el-tag :type="getTypeTagType(item.type)" effect="light" round size="small">
                {{ getTypeLabel(item.type) }}
              </el-tag>
              <el-tag :type="getDifficultyTagType(item.difficulty)" effect="plain" round size="small">
                {{ getDifficultyLabel(item.difficulty) }}
              </el-tag>
              <span class="score-badge">{{ item.score || 5 }} 分</span>
              <span v-if="item.chapterName" class="chapter-badge">{{ item.chapterName }}</span>
            </div>

            <MathText :text="item.stem" tag="div" custom-class="stem-content" />

            <div v-if="item.options && item.options.length > 0" class="options-container">
              <div
                v-for="opt in item.options"
                :key="opt.key"
                class="option-pill"
                :class="{ 'option-pill--correct': opt.isCorrect }"
              >
                <span class="opt-key">{{ opt.key }}.</span>
                <MathText :text="opt.content" tag="span" custom-class="opt-text" />
                <span v-if="opt.isCorrect" class="opt-check-icon"><el-icon><Check /></el-icon> 正确项</span>
              </div>
            </div>

            <div v-if="expandedAnalyses.includes(item.id)" class="analysis-box">
              <div class="analysis-line">
                <span class="label">参考答案：</span>
                <span class="val font-semibold text-emerald-600">{{ item.correctAnswer || '无' }}</span>
              </div>
              <div class="analysis-line">
                <span class="label">解析说明：</span>
                <MathText :text="item.analysis || '暂无详细文字解析'" tag="span" custom-class="val" />
              </div>
              <div v-if="item.knowledgePointNames && item.knowledgePointNames.length > 0" class="analysis-line">
                <span class="label">知识点：</span>
                <div class="kp-tags">
                  <span v-for="kp in item.knowledgePointNames" :key="kp" class="kp-tag">{{ kp }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="item-actions">
            <el-button
              link
              type="primary"
              size="small"
              @click="onToggleExpandAnalysis(item.id)"
            >
              {{ expandedAnalyses.includes(item.id) ? '收起答案解析' : '查看答案解析' }}
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              @click="onViewDetailDialog(item)"
            >
              完整题卡
            </el-button>
            <el-popconfirm
              title="确定要将该试题从当前题库移出吗？（原题库库源题不会被删除）"
              confirm-button-text="确定移出"
              cancel-button-text="取消"
              @confirm="onRemoveQuestion(item.id)"
            >
              <template #reference>
                <el-button link type="danger" size="small">移出题库</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>

      <div v-else class="empty-questions-card">
        <div class="empty-icon">
          <el-icon><Files /></el-icon>
        </div>
        <h3 class="empty-title">当前题库暂无题目数据</h3>
        <p class="empty-sub">
          您可以从平台的公共试题库中挑选试题批量加入，或者点击上方“挑选题目入库”。
        </p>
        <el-button type="primary" class="mt-4" @click="onOpenAddDrawer">
          挑选题目加入此题库
        </el-button>
      </div>
    </div>

    <el-drawer
      :model-value="drawerVisible"
      title="挑选试题批量入库"
      size="720px"
      destroy-on-close
      @update:model-value="onDrawerVisibleChange"
    >
      <div class="drawer-content-box">
        <div class="drawer-filter-bar">
          <el-input
            :model-value="drawerSearch"
            placeholder="搜索公共试题题干、知识点..."
            clearable
            class="flex-1"
            :prefix-icon="Search"
            @update:model-value="onDrawerSearchChange"
          />
          <el-select
            :model-value="drawerType"
            placeholder="题型"
            clearable
            style="width: 140px"
            @update:model-value="onDrawerTypeChange"
          >
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL_BLANK" />
            <el-option label="简答题" value="SHORT_ANSWER" />
          </el-select>
        </div>

        <div class="candidate-questions-list">
          <div
            v-for="q in candidateQuestions"
            :key="q.id"
            class="candidate-item"
            :class="{ selected: selectedCandidateIds.includes(q.id) }"
            @click="onToggleCandidateSelect(q.id)"
          >
            <el-checkbox
              :model-value="selectedCandidateIds.includes(q.id)"
              @click.stop
              @change="onToggleCandidateSelect(q.id)"
            />
            <div class="candidate-main">
              <div class="badges-line">
                <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="getDifficultyTagType(q.difficulty)">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
                <span class="cand-score">{{ q.score || 5 }}分</span>
                <span class="cand-course">{{ q.courseName }}</span>
              </div>
              <p class="candidate-stem">{{ q.stem }}</p>
            </div>
          </div>

          <div v-if="candidateQuestions.length === 0" class="drawer-empty-hint">
            没有更多可添加的试题（题库已包含所有相关试题，或没有匹配结果）
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer-actions">
          <span class="selected-summary">已选中 <strong>{{ selectedCandidateIds.length }}</strong> 道试题</span>
          <div class="btns">
            <el-button @click="onDrawerVisibleChange(false)">取消</el-button>
            <el-button
              type="primary"
              :disabled="selectedCandidateIds.length === 0"
              :loading="addingLoading"
              @click="onConfirmAddQuestions"
            >
              确认批量入库 ({{ selectedCandidateIds.length }})
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      :model-value="detailModalVisible"
      title="试题完整题卡详情"
      width="640px"
      destroy-on-close
      @update:model-value="onDetailModalVisibleChange"
    >
      <div v-if="activeQuestion" class="detail-modal-body">
        <div class="modal-tags-row">
          <el-tag :type="getTypeTagType(activeQuestion.type)">{{ getTypeLabel(activeQuestion.type) }}</el-tag>
          <el-tag :type="getDifficultyTagType(activeQuestion.difficulty)">{{ getDifficultyLabel(activeQuestion.difficulty) }}</el-tag>
          <span class="text-sm text-slate-500">分值：{{ activeQuestion.score }} 分</span>
          <span class="text-sm text-slate-500">所属课程：{{ activeQuestion.courseName }}</span>
        </div>

        <div class="detail-stem-box">
          <h4 class="box-subtitle">试题题干：</h4>
          <p class="stem-text">{{ activeQuestion.stem }}</p>
        </div>

        <div v-if="activeQuestion.options && activeQuestion.options.length" class="detail-opts-box">
          <h4 class="box-subtitle">备选答案项：</h4>
          <div class="options-vert-list">
            <div
              v-for="opt in activeQuestion.options"
              :key="opt.key"
              class="opt-item"
              :class="{ 'opt-item--correct': opt.isCorrect }"
            >
              <span class="opt-tag">{{ opt.key }}</span>
              <span class="opt-text">{{ opt.content }}</span>
              <span v-if="opt.isCorrect" class="opt-badge">正确答案</span>
            </div>
          </div>
        </div>

        <div class="detail-answer-box">
          <h4 class="box-subtitle">参考答案及评阅要点：</h4>
          <div class="ans-content">{{ activeQuestion.correctAnswer || '无指定客观答案' }}</div>
        </div>

        <div class="detail-analysis-box">
          <h4 class="box-subtitle">试题深度解析：</h4>
          <div class="analysis-content">{{ activeQuestion.analysis || '暂无解析说明' }}</div>
        </div>

        <div v-if="activeQuestion.knowledgePointNames && activeQuestion.knowledgePointNames.length" class="detail-kp-box">
          <h4 class="box-subtitle">关联知识点：</h4>
          <div class="kp-chips">
            <span v-for="kp in activeQuestion.knowledgePointNames" :key="kp" class="kp-chip">
              # {{ kp }}
            </span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Search, Files, Check } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';

defineProps<{
  searchKeyword: string;
  filterType: string;
  filterDifficulty: string;
  selectedRowKeys: number[];
  expandedAnalyses: number[];
  drawerVisible: boolean;
  drawerSearch: string;
  drawerType: string;
  selectedCandidateIds: number[];
  addingLoading: boolean;
  detailModalVisible: boolean;
  activeQuestion: QuestionItem | null;
  filteredQuestions: QuestionItem[];
  candidateQuestions: QuestionItem[];
  onSearchKeywordChange: (value: string) => void;
  onFilterTypeChange: (value: string) => void;
  onFilterDifficultyChange: (value: string) => void;
  onToggleSelectRow: (id: number) => void;
  onToggleExpandAnalysis: (id: number) => void;
  onViewDetailDialog: (item: QuestionItem) => void;
  onRemoveQuestion: (id: number) => void;
  onBatchRemove: () => void;
  onOpenAddDrawer: () => void;
  onDrawerVisibleChange: (visible: boolean) => void;
  onDrawerSearchChange: (value: string) => void;
  onDrawerTypeChange: (value: string) => void;
  onToggleCandidateSelect: (id: number) => void;
  onConfirmAddQuestions: () => void;
  onDetailModalVisibleChange: (visible: boolean) => void;
  getTypeLabel: (type: QuestionType | string) => string;
  getTypeTagType: (type: QuestionType | string) => string;
  getDifficultyLabel: (diff: Difficulty | string) => string;
  getDifficultyTagType: (diff: Difficulty | string) => string;
}>();
</script>

<style scoped lang="scss">
.bank-content-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
  padding: 24px;

  .toolbar-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    padding-bottom: 20px;
    border-bottom: 1px solid #f1f5f9;
    margin-bottom: 20px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .search-input {
        width: 320px;
      }

      .filter-select {
        width: 140px;
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .total-hint-text {
        font-size: 13px;
        color: #94a3b8;
      }
    }
  }

  .questions-list-section {
    .question-items-stack {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .question-bank-item-card {
        border: 1px solid #f1f5f9;
        background: #ffffff;
        border-radius: 12px;
        padding: 18px 20px;
        display: flex;
        gap: 16px;
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
        }

        .item-checkbox-col {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;

          .index-num {
            font-size: 12px;
            color: #94a3b8;
            font-family: monospace;
          }
        }

        .item-body {
          flex: 1;

          .item-badges-row {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 10px;

            .score-badge {
              font-size: 12px;
              color: #2563eb;
              background: #eff6ff;
              padding: 2px 8px;
              border-radius: 4px;
              font-weight: 600;
            }

            .chapter-badge {
              font-size: 12px;
              color: #64748b;
              background: #f1f5f9;
              padding: 2px 8px;
              border-radius: 4px;
            }
          }

          .stem-content {
            font-size: 15px;
            line-height: 1.6;
            color: #1e293b;
            font-weight: 500;
            margin-bottom: 12px;
          }

          .options-container {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 8px;
            margin-bottom: 12px;

            .option-pill {
              background: #f8fafc;
              border: 1px solid #e2e8f0;
              border-radius: 8px;
              padding: 8px 12px;
              font-size: 13px;
              color: #334155;
              display: flex;
              align-items: center;
              gap: 8px;

              .opt-key {
                font-weight: 700;
                color: #64748b;
              }

              .opt-text {
                flex: 1;
              }

              &--correct {
                background: #f0fdf4;
                border-color: #bbf7d0;
                color: #15803d;

                .opt-key {
                  color: #16a34a;
                }

                .opt-check-icon {
                  font-size: 11px;
                  font-weight: 600;
                  color: #16a34a;
                }
              }
            }
          }

          .analysis-box {
            background: #f8fafc;
            border-radius: 8px;
            padding: 12px 16px;
            margin-top: 10px;
            border-left: 3px solid #3b82f6;

            .analysis-line {
              font-size: 13px;
              line-height: 1.6;
              color: #475569;
              margin-bottom: 4px;

              &:last-child {
                margin-bottom: 0;
              }

              .label {
                font-weight: 600;
                color: #334155;
              }

              .kp-tags {
                display: inline-flex;
                gap: 6px;

                .kp-tag {
                  background: #e2e8f0;
                  color: #475569;
                  font-size: 11px;
                  padding: 1px 6px;
                  border-radius: 4px;
                }
              }
            }
          }
        }

        .item-actions {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          justify-content: flex-start;
          gap: 6px;
          width: 100px;
          flex-shrink: 0;
        }
      }
    }

    .empty-questions-card {
      padding: 48px;
      text-align: center;

      .empty-icon {
        font-size: 48px;
        color: #94a3b8;
        display: inline-flex;
        margin-bottom: 12px;
      }

      .empty-title {
        font-size: 18px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 6px;
      }

      .empty-sub {
        font-size: 14px;
        color: #94a3b8;
      }
    }
  }
}

.drawer-content-box {
  display: flex;
  flex-direction: column;
  height: 100%;

  .drawer-filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .candidate-questions-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .candidate-item {
      border: 1px solid #e2e8f0;
      border-radius: 10px;
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

      .candidate-main {
        flex: 1;

        .badges-line {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 6px;

          .cand-score {
            font-size: 12px;
            color: #2563eb;
            font-weight: 600;
          }

          .cand-course {
            font-size: 12px;
            color: #94a3b8;
          }
        }

        .candidate-stem {
          font-size: 14px;
          color: #1e293b;
          line-height: 1.5;
          margin: 0;
        }
      }
    }

    .drawer-empty-hint {
      padding: 40px;
      text-align: center;
      color: #94a3b8;
      font-size: 14px;
    }
  }
}

.drawer-footer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .selected-summary {
    font-size: 14px;
    color: #475569;

    strong {
      color: #2563eb;
      font-size: 16px;
    }
  }

  .btns {
    display: flex;
    gap: 8px;
  }
}

.detail-modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .modal-tags-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f1f5f9;
  }

  .box-subtitle {
    font-size: 14px;
    font-weight: 600;
    color: #334155;
    margin: 0 0 6px;
  }

  .stem-text {
    font-size: 15px;
    line-height: 1.6;
    color: #1e293b;
    background: #f8fafc;
    padding: 12px 14px;
    border-radius: 8px;
    margin: 0;
  }

  .options-vert-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .opt-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 12px;
      border-radius: 6px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;

      .opt-tag {
        font-weight: 700;
        color: #64748b;
      }

      .opt-text {
        flex: 1;
        font-size: 13px;
      }

      &--correct {
        background: #f0fdf4;
        border-color: #86efac;

        .opt-tag {
          color: #16a34a;
        }

        .opt-badge {
          font-size: 11px;
          color: #16a34a;
          font-weight: 600;
        }
      }
    }
  }

  .ans-content,
  .analysis-content {
    font-size: 14px;
    color: #334155;
    line-height: 1.6;
    background: #f8fafc;
    padding: 10px 14px;
    border-radius: 8px;
  }

  .kp-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .kp-chip {
      background: #eff6ff;
      color: #2563eb;
      font-size: 12px;
      padding: 4px 10px;
      border-radius: 6px;
      font-weight: 500;
    }
  }
}
</style>
