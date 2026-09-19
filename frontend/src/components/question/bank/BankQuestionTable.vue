<template>
  <div class="bank-question-panel">
    <div class="filter-capsule-card">
      <div class="filter-row">
        <span class="filter-label">试题题型：</span>
        <div class="pill-tags-track">
          <span
            v-for="t in typeOptions"
            :key="t.value || 'all-type'"
            class="filter-pill-tag"
            :class="{ active: filterType === t.value }"
            @click="onFilterTypeChange(t.value)"
          >
            {{ t.label }}
          </span>
        </div>
      </div>

      <div class="filter-row filter-row--bottom">
        <div class="filter-left-col">
          <span class="filter-label">难度等级：</span>
          <div class="pill-tags-track">
            <span
              v-for="d in difficultyOptions"
              :key="d.value || 'all-diff'"
              class="filter-pill-tag"
              :class="{ active: filterDifficulty === d.value }"
              @click="onFilterDifficultyChange(d.value)"
            >
              {{ d.label }}
            </span>
          </div>
        </div>

        <div class="filter-right-search">
          <div v-if="filteredQuestions.length > 0" class="list-expand-actions">
            <button type="button" class="list-expand-toggle-btn" @click="toggleAllQuestionBody">
              <el-icon><component :is="listExpandAllBody ? Fold : Expand" /></el-icon>
              <span>{{ listExpandAllBody ? '收起题目' : '展开题目' }}</span>
            </button>
            <button
              type="button"
              class="list-expand-toggle-btn list-expand-toggle-btn--full"
              @click="toggleAllQuestionDetails"
            >
              <el-icon><component :is="listExpandAllFull ? Fold : Expand" /></el-icon>
              <span>{{ listExpandAllFull ? '收起题目和解析' : '展开题目和解析' }}</span>
            </button>
          </div>
          <div class="capsule-search-box">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              :value="searchKeyword"
              type="text"
              class="capsule-search-input"
              placeholder="搜索题干、知识点或解析..."
              @input="onSearchInput"
            />
            <button
              v-if="searchKeyword"
              type="button"
              class="clear-btn"
              @click="onSearchKeywordChange('')"
            >
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="questions-list-card">
      <div class="questions-list-toolbar">
        <div class="toolbar-left">
          <span class="list-title">题库内试题</span>
          <span class="list-count-badge">共 {{ filteredQuestions.length }} 道</span>
        </div>
        <div v-if="selectedRowKeys.length > 0" class="toolbar-right">
          <button type="button" class="batch-remove-btn" @click="onBatchRemove">
            批量移出题库 ({{ selectedRowKeys.length }})
          </button>
        </div>
      </div>

      <div class="questions-list-body">
        <div v-if="filteredQuestions.length > 0" class="questions-stream-list">
          <div
            v-for="(item, index) in filteredQuestions"
            :key="item.id"
            class="question-wrapper-item"
          >
            <div class="question-index-marker">
              <div class="marker-left">
                <el-checkbox
                  :model-value="selectedRowKeys.includes(item.id)"
                  @change="onToggleSelectRow(item.id)"
                />
                <span>第 {{ index + 1 }} 题</span>
                <span v-if="item.chapterName" class="marker-course-tag">{{ item.chapterName }}</span>
              </div>
            </div>

            <QuestionCard
              :question="item"
              :index="index"
              :allow-edit="false"
              allow-ai-tutor
              delete-scope="bank"
              delete-action-label="移出题库"
              :default-body-expanded="false"
              :default-analysis-expanded="false"
              :bulk-expand-body="listExpandAllBody"
              :bulk-expand-analysis="listExpandAllAnalysis"
              :expand-sync-key="expandSyncKey"
              @delete="onRemoveQuestion"
              @tutor="openQuestionAiTutor"
            />
          </div>
        </div>

        <div v-else class="empty-questions-panel">
          <div class="empty-icon">
            <el-icon><Files /></el-icon>
          </div>
          <h3 class="empty-title">当前筛选下暂无题目</h3>
          <p class="empty-text">可调整题型或难度条件，或从公共试题池挑选题目加入本题库。</p>
          <button type="button" class="module-capsule-btn module-capsule-btn--primary" @click="onOpenAddDrawer">
            <el-icon><Plus /></el-icon>
            <span>挑选题目入库</span>
          </button>
        </div>
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
                <el-tag size="small" :type="getDifficultyTagType(q.difficulty)">
                  {{ getDifficultyLabel(q.difficulty) }}
                </el-tag>
                <span class="cand-score">{{ q.score || 5 }}分</span>
                <span class="cand-course">{{ q.courseName }}</span>
              </div>
              <MathText :text="q.stem" tag="div" custom-class="candidate-stem" />
            </div>
          </div>

          <div v-if="candidateQuestions.length === 0" class="drawer-empty-hint">
            没有更多可添加的试题（题库已包含所有相关试题，或没有匹配结果）
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer-actions">
          <span class="selected-summary">
            已选中 <strong>{{ selectedCandidateIds.length }}</strong> 道试题
          </span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, toRefs } from 'vue';
import { Search, Files, Close, Expand, Fold, Plus } from '@element-plus/icons-vue';
import QuestionCard from '@/components/question/QuestionCard.vue';
import MathText from '@/components/common/MathText.vue';
import { useQuestionAiTutor } from '@/composables/question/useQuestionAiTutor';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';

const props = defineProps<{
  searchKeyword: string;
  filterType: string;
  filterDifficulty: string;
  selectedRowKeys: (number | string)[];
  drawerVisible: boolean;
  drawerSearch: string;
  drawerType: string;
  selectedCandidateIds: (number | string)[];
  addingLoading: boolean;
  filteredQuestions: QuestionItem[];
  candidateQuestions: QuestionItem[];
  onSearchKeywordChange: (value: string) => void;
  onFilterTypeChange: (value: string) => void;
  onFilterDifficultyChange: (value: string) => void;
  onToggleSelectRow: (id: number | string) => void;
  onRemoveQuestion: (id: number | string) => void;
  onBatchRemove: () => void;
  onOpenAddDrawer: () => void;
  onDrawerVisibleChange: (visible: boolean) => void;
  onDrawerSearchChange: (value: string) => void;
  onDrawerTypeChange: (value: string) => void;
  onToggleCandidateSelect: (id: number | string) => void;
  onConfirmAddQuestions: () => void;
  getTypeLabel: (type: QuestionType | string) => string;
  getTypeTagType: (type: QuestionType | string) => string;
  getDifficultyLabel: (diff: Difficulty | string) => string;
  getDifficultyTagType: (diff: Difficulty | string) => string;
}>();

const {
  searchKeyword,
  filterType,
  filterDifficulty,
  selectedRowKeys,
  drawerVisible,
  drawerSearch,
  drawerType,
  selectedCandidateIds,
  addingLoading,
  filteredQuestions,
  candidateQuestions
} = toRefs(props);

const {
  onSearchKeywordChange,
  onFilterTypeChange,
  onFilterDifficultyChange,
  onToggleSelectRow,
  onRemoveQuestion,
  onBatchRemove,
  onOpenAddDrawer,
  onDrawerVisibleChange,
  onDrawerSearchChange,
  onDrawerTypeChange,
  onToggleCandidateSelect,
  onConfirmAddQuestions,
  getTypeLabel,
  getTypeTagType,
  getDifficultyLabel,
  getDifficultyTagType
} = props;

const { openQuestionAiTutor } = useQuestionAiTutor();

const typeOptions = [
  { label: '全部题型', value: '' },
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '判断题', value: 'TRUE_FALSE' },
  { label: '填空题', value: 'FILL_BLANK' },
  { label: '简答题', value: 'SHORT_ANSWER' }
];

const difficultyOptions = [
  { label: '全部难度', value: '' },
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
];

const listExpandAllBody = ref(false);
const listExpandAllAnalysis = ref(false);
const expandSyncKey = ref(0);

const listExpandAllFull = computed(
  () => listExpandAllBody.value && listExpandAllAnalysis.value
);

function bumpExpandSync() {
  expandSyncKey.value += 1;
}

function toggleAllQuestionBody() {
  listExpandAllBody.value = !listExpandAllBody.value;
  bumpExpandSync();
}

function toggleAllQuestionDetails() {
  const next = !listExpandAllFull.value;
  listExpandAllBody.value = next;
  listExpandAllAnalysis.value = next;
  bumpExpandSync();
}

function onSearchInput(event: Event) {
  const target = event.target as HTMLInputElement;
  props.onSearchKeywordChange(target.value);
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
@use '@/styles/question/question-list-panel.scss';

:deep(.question-card) {
  margin-bottom: 0;
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
          line-height: 1.55;
          margin: 0;

          :deep(.katex) {
            font-size: 1.02em;
          }
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
</style>
