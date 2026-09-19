<template>
  <div class="question-list-page-container">
    <!-- 1. 顶部操作头区 (Hero Header Dock) -->
    <div class="question-header-dock">
      <div class="header-main">
        <div class="title-with-icon">
          <div class="icon-orb" aria-hidden="true">
            <el-icon class="header-icon"><Reading /></el-icon>
          </div>
          <div class="title-meta-col">
            <div class="title-badges-row">
              <h1 class="main-title">智能题库管理中心</h1>
              <span class="capsule-count-tag">已收录 {{ total }} 道精选试题</span>
            </div>
            <p class="sub-desc">
              覆盖单选、多选、判断与主观推导大题，支持基于课程知识大纲的试题检索、原位精修与 AI 智能批量入库。
            </p>
          </div>
        </div>
      </div>

      <div class="header-right-actions">
        <div class="action-toolbar">
          <button
            type="button"
            class="capsule-btn capsule-btn--ai"
            @click="router.push('/ai/question/generate')"
          >
            <el-icon class="sparkle-icon"><MagicStick /></el-icon>
            <span>AI 智能出题</span>
            <span class="pill-bubble">秒级出题</span>
          </button>

          <button
            type="button"
            class="capsule-btn capsule-btn--primary"
            @click="router.push('/question/create')"
          >
            <el-icon><Plus /></el-icon>
            <span>录入新题</span>
          </button>

          <button
            type="button"
            class="capsule-btn capsule-btn--secondary"
            @click="router.push('/ai/exam/generate')"
          >
            <el-icon><Tickets /></el-icon>
            <span>一键智能组卷</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 2. 长圆药丸筛选工具栏 (Pill Filters Toolbar) -->
    <div class="filter-capsule-card">
      <!-- 课程筛选行 -->
      <div class="filter-row">
        <span class="filter-label">所属课程：</span>
        <el-select
          v-model="selectedCourseId"
          placeholder="全部课程"
          clearable
          filterable
          class="filter-select filter-select--course"
          @change="resetPageAndLoad"
        >
          <el-option
            v-for="c in courseOptions"
            :key="String(c.value)"
            :label="c.label"
            :value="c.value"
          />
        </el-select>
      </div>

      <!-- 题型筛选行 -->
      <div class="filter-row">
        <span class="filter-label">试题题型：</span>
        <div class="pill-tags-track">
          <span
            v-for="t in typeOptions"
            :key="t.value"
            class="filter-pill-tag"
            :class="{ active: selectedType === t.value }"
            @click="handleTypeFilter(t.value)"
          >
            {{ t.label }}
          </span>
        </div>
      </div>

      <!-- 难度筛选行与长圆搜索框 -->
      <div class="filter-row filter-row--bottom">
        <div class="filter-left-col">
          <span class="filter-label">难度等级：</span>
          <div class="pill-tags-track">
            <span
              v-for="d in difficultyOptions"
              :key="d.value"
              class="filter-pill-tag"
              :class="{ active: selectedDifficulty === d.value }"
              @click="handleDifficultyFilter(d.value)"
            >
              {{ d.label }}
            </span>
          </div>
        </div>

        <div class="filter-right-search">
          <div v-if="questions.length > 0" class="list-expand-actions">
            <button
              type="button"
              class="list-expand-toggle-btn"
              @click="toggleAllQuestionBody"
            >
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
              v-model="keyword"
              type="text"
              class="capsule-search-input"
              placeholder="搜索题干内容、考查知识点或解析..."
              @keyup.enter="handleSearch"
            />
            <button
              v-if="keyword"
              type="button"
              class="clear-btn"
              @click="clearKeyword"
            >
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 试题列表卡片（含分页，与操作日志等模块一致） -->
    <div class="questions-list-card">
      <div class="questions-list-toolbar">
        <div class="toolbar-left">
          <span class="list-title">试题列表</span>
          <span class="list-count-badge">共 {{ total }} 道试题</span>
          <span v-if="total > 0" class="list-range-hint">
            当前第 {{ listRangeStart }}-{{ listRangeEnd }} 道
          </span>
        </div>
      </div>

      <div v-loading="loading" class="questions-list-body">
    <div v-if="questions.length > 0" class="questions-stream-list">
      <div
        v-for="(q, index) in questions"
        :key="q.id"
        class="question-wrapper-item"
      >
        <div class="question-index-marker">
          <span>第 {{ (pageNum - 1) * pageSize + index + 1 }} 题</span>
          <span v-if="q.courseName" class="marker-course-tag">{{ q.courseName }}</span>
        </div>

        <QuestionCard
          :question="q"
          allow-ai-tutor
          :default-body-expanded="false"
          :default-analysis-expanded="false"
          :bulk-expand-body="listExpandAllBody"
          :bulk-expand-analysis="listExpandAllAnalysis"
          :expand-sync-key="expandSyncKey"
          @delete="handleDeleteQuestion"
          @update="handleUpdateQuestion"
          @tutor="openQuestionAiTutor"
        />
      </div>
    </div>

    <!-- 4. 空状态处理 -->
    <div v-else class="empty-questions-panel">
      <div class="empty-illustration">
        <div class="icon-halo">
          <el-icon><Search /></el-icon>
        </div>
      </div>
      <h3 class="empty-title">未找到匹配的题库试题</h3>
      <p class="empty-text">可以尝试切换上方课程、题型或难度条件，也可以直接使用 AI 一键生成。</p>
      <div class="empty-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--ai"
          @click="router.push('/ai/question/generate')"
        >
          <el-icon><MagicStick /></el-icon>
          <span>立即前往 AI 智能出题</span>
          <span class="pill-bubble">秒级出题</span>
        </button>

        <button
          v-if="hasActiveFilter"
          type="button"
          class="capsule-btn capsule-btn--secondary"
          @click="resetAllFilters"
        >
          <el-icon><RefreshRight /></el-icon>
          <span>重置筛选条件</span>
        </button>
      </div>
    </div>

    <AppPagination
      v-if="total > 0"
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="() => loadQuestions({ scrollToList: true })"
    />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Reading,
  MagicStick,
  Plus,
  Tickets,
  Search,
  Close,
  RefreshRight,
  Expand,
  Fold
} from '@element-plus/icons-vue';
import QuestionCard from '@/components/question/QuestionCard.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { useQuestion } from '@/composables/question/useQuestion';
import { useQuestionAiTutor } from '@/composables/question/useQuestionAiTutor';
import { difficultyToLevel } from '@/utils/question/difficulty-level';
import type { Question } from '@/types/question/question';

const router = useRouter();
const route = useRoute();
const { questions, loading, total, fetchQuestions, removeQuestion, saveQuestion, loadCourseOptions } = useQuestion();
const { openQuestionAiTutor } = useQuestionAiTutor();

const pageNum = ref(1);
const pageSize = ref(10);
const selectedCourseId = ref<number | null>(null);
const selectedType = ref<string>('ALL');
const selectedDifficulty = ref<string>('ALL');
const keyword = ref<string>('');

const listExpandAllBody = ref(false);
const listExpandAllAnalysis = ref(false);
const expandSyncKey = ref(0);

const listExpandAllFull = computed(
  () => listExpandAllBody.value && listExpandAllAnalysis.value
);

const listRangeStart = computed(() => {
  if (total.value <= 0) return 0;
  return (pageNum.value - 1) * pageSize.value + 1;
});

const listRangeEnd = computed(() => {
  if (total.value <= 0) return 0;
  return Math.min(pageNum.value * pageSize.value, total.value);
});

const courseOptions = ref<Array<{ label: string; value: number | null }>>([
  { label: '全部课程', value: null }
]);

const typeOptions = [
  { label: '全部题型', value: 'ALL' },
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '简答题', value: 'SHORT_ANSWER' }
];

const difficultyOptions = [
  { label: '全部难度', value: 'ALL' },
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
];

onMounted(async () => {
  document.querySelector('.app-content')?.scrollTo({ top: 0 });
  await loadCourseFilterOptions();
  const qCourseId = route.query.courseId ? Number(route.query.courseId) : null;
  if (qCourseId) {
    selectedCourseId.value = qCourseId;
  }
  await loadQuestions();
});

async function loadCourseFilterOptions() {
  courseOptions.value = await loadCourseOptions();
}

async function loadQuestions(options?: { scrollToList?: boolean }) {
  await fetchQuestions({
    page: pageNum.value,
    pageSize: pageSize.value,
    courseId: selectedCourseId.value || undefined,
    type: selectedType.value !== 'ALL' ? selectedType.value : undefined,
    difficulty: difficultyToLevel(selectedDifficulty.value),
    keyword: keyword.value.trim() || undefined
  });
  if (options?.scrollToList) {
    await nextTick();
    document.querySelector('.questions-list-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function resetPageAndLoad() {
  pageNum.value = 1;
  loadQuestions();
}

function handleTypeFilter(value: string) {
  selectedType.value = value;
  resetPageAndLoad();
}

function handleDifficultyFilter(value: string) {
  selectedDifficulty.value = value;
  resetPageAndLoad();
}

function handleSearch() {
  resetPageAndLoad();
}

function clearKeyword() {
  keyword.value = '';
  resetPageAndLoad();
}

function toggleAllQuestionBody() {
  listExpandAllBody.value = !listExpandAllBody.value;
  expandSyncKey.value += 1;
}

function toggleAllQuestionDetails() {
  const next = !listExpandAllFull.value;
  listExpandAllBody.value = next;
  listExpandAllAnalysis.value = next;
  expandSyncKey.value += 1;
}

const hasActiveFilter = computed(() => {
  return (
    selectedCourseId.value !== null ||
    selectedType.value !== 'ALL' ||
    selectedDifficulty.value !== 'ALL' ||
    Boolean(keyword.value.trim())
  );
});

function resetAllFilters() {
  selectedCourseId.value = null;
  selectedType.value = 'ALL';
  selectedDifficulty.value = 'ALL';
  keyword.value = '';
  resetPageAndLoad();
}

async function handleDeleteQuestion(id: number | string) {
  try {
    await removeQuestion(id);
    ElMessage.success('试题已删除');
    await loadQuestions();
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '删除试题失败，请稍后重试';
    ElMessage.error(msg || '删除试题失败，请稍后重试');
  }
}

async function handleUpdateQuestion(updated: Question) {
  try {
    await saveQuestion(updated, updated.id);
    ElMessage.success('试题内容更新已保存');
  } catch {
    ElMessage.error('保存试题失败，请稍后重试');
  }
}
</script>

<style scoped lang="scss">
.question-list-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. 顶部操作坞
  .question-header-dock {
    position: relative;
    overflow: hidden;
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 55%, #f0f7ff 100%);
    border-radius: 20px;
    padding: 22px 28px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 22px rgba(30, 80, 160, 0.06);
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 20px 28px;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, #2563eb 0%, #6366f1 45%, #7c3aed 100%);
      opacity: 0.85;
    }

    @media (max-width: 960px) {
      grid-template-columns: 1fr;
      align-items: stretch;

      .header-right-actions {
        justify-content: flex-start;
      }
    }

    .header-main {
      min-width: 0;
    }

    .title-with-icon {
      display: flex;
      align-items: flex-start;
      gap: 16px;

      .icon-orb {
        flex-shrink: 0;
        width: 52px;
        height: 52px;
        border-radius: 16px;
        background: linear-gradient(145deg, #eff6ff 0%, #dbeafe 100%);
        border: 1px solid #bfdbfe;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.12);

        .header-icon {
          font-size: 26px;
          color: #2563eb;
        }
      }

      .title-meta-col {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding-top: 2px;

        .title-badges-row {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;

          .main-title {
            margin: 0;
            font-size: 22px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: -0.02em;
            line-height: 1.25;
          }

          .capsule-count-tag {
            padding: 3px 12px;
            border-radius: 9999px;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #2563eb;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;
          }
        }

        .sub-desc {
          margin: 0;
          font-size: 13.5px;
          color: #64748b;
          line-height: 1.65;
          max-width: 640px;
        }
      }
    }

    .header-right-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;

      .action-toolbar {
        display: inline-flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
        padding: 8px 10px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.72);
        border: 1px solid rgba(226, 232, 240, 0.9);
        box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
        backdrop-filter: blur(6px);
      }
    }
  }

  // 通用长圆胶囊按钮规范
  .capsule-btn {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    height: 40px;
    padding: 0 18px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    outline: none;
    user-select: none;
    white-space: nowrap;
    transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

    &--ai {
      background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(114, 46, 209, 0.28);

      .pill-bubble {
        font-size: 11px;
        padding: 1px 8px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.22);
        font-weight: 500;
      }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(114, 46, 209, 0.38);
      }

      &:active {
        transform: translateY(0);
      }
    }

    &--primary {
      background: #1677FF;
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);

      &:hover {
        background: #0958D9;
        transform: translateY(-2px);
        box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
      }

      &:active {
        transform: translateY(0);
      }
    }

    &--secondary {
      background: #FFFFFF;
      color: #334155;
      border: 1.5px solid #CBD5E1;

      &:hover {
        background: #F8FAFC;
        color: #1677FF;
        border-color: #93C5FD;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.08);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }

  // 2. 筛选工具栏
  .filter-capsule-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 18px 24px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);
    display: flex;
    flex-direction: column;
    gap: 14px;

    .filter-row {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;

      .filter-label {
        font-size: 13px;
        font-weight: 600;
        color: #64748B;
        min-width: 70px;
      }

      .filter-select--course {
        width: min(360px, 100%);
        min-width: 220px;
        flex-shrink: 0;
      }

      .pill-tags-track {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;

        .filter-pill-tag {
          padding: 4px 14px;
          border-radius: 9999px; // 长圆药丸
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          color: #475569;
          font-size: 12px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            color: #1677FF;
            border-color: #BFDBFE;
          }

          &.active {
            background: #1677FF;
            color: #FFFFFF;
            border-color: #1677FF;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
          }
        }
      }

      &--bottom {
        justify-content: space-between;
        margin-top: 4px;
        padding-top: 14px;
        border-top: 1px solid #F1F5F9;
        display: grid;
        grid-template-columns: minmax(0, 1fr) auto;
        align-items: center;
        column-gap: 16px;

        .filter-left-col {
          display: flex;
          align-items: center;
          gap: 14px;
          min-width: 0;
          flex-wrap: nowrap;
        }

        .filter-right-search {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: nowrap;
          justify-content: flex-end;
          flex-shrink: 0;

          .list-expand-actions {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: nowrap;
            flex-shrink: 0;
          }

          .list-expand-toggle-btn {
            height: 38px;
            padding: 0 16px;
            border-radius: 9999px;
            border: 1.5px solid #E2E8F0;
            background: #F8FAFC;
            color: #475569;
            font-size: 12.5px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s;
            white-space: nowrap;

            &:hover {
              border-color: #1677FF;
              color: #1677FF;
              background: #EFF6FF;
            }

            &--full {
              background: #F0FDF4;
              border-color: #BBF7D0;
              color: #15803D;

              &:hover {
                border-color: #22C55E;
                color: #166534;
                background: #DCFCE7;
              }
            }
          }

          .capsule-search-box {
            display: flex;
            align-items: center;
            width: 260px;
            max-width: 36vw;
            min-width: 200px;
            height: 38px;
            padding: 0 14px;
            flex-shrink: 1;
            border-radius: 9999px; // 长圆输入框
            border: 1.5px solid #E2E8F0;
            background: #FFFFFF;
            transition: all 0.2s;

            &:focus-within {
              border-color: #1677FF;
              box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.14);
            }

            .search-icon {
              font-size: 13px;
              color: #94A3B8;
              margin-right: 8px;
            }

            .capsule-search-input {
              flex: 1;
              border: none;
              outline: none;
              font-size: 13px;
              color: #1E293B;

              &::placeholder {
                color: #94A3B8;
              }
            }

            .clear-btn {
              background: transparent;
              border: none;
              color: #94A3B8;
              cursor: pointer;
              font-size: 12px;
            }
          }
        }
      }
    }
  }

  .questions-list-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);
    overflow: hidden;

    .questions-list-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 22px;
      border-bottom: 1px solid #EBF1F7;
      background: #FAFCFF;

      .toolbar-left {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
      }

      .list-title {
        font-size: 14px;
        font-weight: 700;
        color: #0F172A;
      }

      .list-count-badge {
        font-size: 12px;
        font-weight: 600;
        color: #1677FF;
        background: #EFF6FF;
        border: 1px solid #BFDBFE;
        padding: 2px 10px;
        border-radius: 9999px;
      }

      .list-range-hint {
        font-size: 12px;
        color: #64748B;
      }
    }

    .questions-list-body {
      padding: 18px 22px 20px;
      min-height: 120px;

      :deep(.pagination-bar) {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        margin-top: 16px;
        padding-top: 14px;
        border-top: 1px solid #EBF1F7;

        .el-pagination {
          justify-content: flex-start;
          width: 100%;
        }
      }
    }
  }

  // 3. 试题列表
  .questions-stream-list {
    display: flex;
    flex-direction: column;
    gap: 18px;

    .question-wrapper-item {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .question-index-marker {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        font-weight: 700;
        color: #64748B;
        padding-left: 6px;

        .marker-course-tag {
          font-weight: 500;
          color: #94A3B8;
        }
      }
    }
  }

  // 4. 空状态卡片（现代精致微质感卡片）
  .empty-questions-panel {
    background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
    border-radius: 20px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.02);
    padding: 64px 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    margin-top: 16px;
    transition: all 0.25s ease;

    &:hover {
      border-color: #CBD5E1;
      box-shadow: 0 8px 28px -4px rgba(15, 23, 42, 0.07);
    }

    .empty-illustration {
      margin-bottom: 16px;

      .icon-halo {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: linear-gradient(135deg, rgba(22, 119, 255, 0.09) 0%, rgba(114, 46, 209, 0.09) 100%);
        border: 1px solid rgba(22, 119, 255, 0.16);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #1677FF;
        font-size: 26px;
        box-shadow: 0 8px 24px -4px rgba(22, 119, 255, 0.14);
        transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      }
    }

    &:hover .icon-halo {
      transform: scale(1.08) rotate(4deg);
    }

    .empty-title {
      font-size: 17px;
      font-weight: 700;
      color: #0F172A;
      margin: 0 0 8px 0;
      letter-spacing: -0.01em;
    }

    .empty-text {
      font-size: 13.5px;
      color: #64748B;
      margin: 0 0 24px 0;
      max-width: 460px;
      line-height: 1.6;
    }

    .empty-actions {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      flex-wrap: wrap;
    }
  }
}

@media (max-width: 1024px) {
  .filter-row--bottom {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 14px;

    .filter-right-search {
      width: 100%;
      .capsule-search-box {
        width: 100% !important;
      }
    }
  }
}
</style>
