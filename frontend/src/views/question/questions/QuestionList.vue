<template>
  <div class="question-list-page-container">
    <!-- 1. 顶部操作头区 (Pill Header Dock) -->
    <div class="question-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <el-icon class="header-icon text-blue-600"><Reading /></el-icon>
          <h1 class="main-title">智能题库管理中心</h1>
          <span class="capsule-count-tag">已收录 {{ total }} 道精选试题</span>
        </div>
        <p class="sub-desc">
          覆盖单选、多选、判断与主观推导大题，支持基于课程知识大纲的试题检索、原位精修与 AI 智能批量入库。
        </p>
      </div>

      <div class="header-right-actions">
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

    <!-- 2. 长圆药丸筛选工具栏 (Pill Filters Toolbar) -->
    <div class="filter-capsule-card">
      <!-- 课程筛选行 -->
      <div class="filter-row">
        <span class="filter-label">所属课程：</span>
        <div class="pill-tags-track">
          <span
            v-for="c in courseOptions"
            :key="String(c.value)"
            class="filter-pill-tag"
            :class="{ active: selectedCourseId === c.value }"
            @click="handleCourseFilter(c.value)"
          >
            {{ c.label }}
          </span>
        </div>
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

    <!-- 3. 试题卡片流列表 -->
    <div v-loading="loading">
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
          @delete="handleDeleteQuestion"
          @update="handleUpdateQuestion"
        />
      </div>
    </div>

    <!-- 4. 空状态处理 -->
    <div v-else class="empty-questions-panel">
      <div class="empty-emoji">
        <el-icon><Search /></el-icon>
      </div>
      <h3 class="empty-title">未找到匹配的题库试题</h3>
      <p class="empty-text">可以尝试切换上方课程、题型或难度条件，也可以直接使用 AI 一键生成。</p>
      <div class="empty-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--ai"
          @click="router.push('/ai/question/generate')"
        >
          <el-icon class="mr-1"><MagicStick /></el-icon>
          <span>立即前往 AI 智能出题</span>
        </button>
      </div>
    </div>

    <AppPagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadQuestions"
    />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Reading,
  MagicStick,
  Plus,
  Tickets,
  Search,
  Close
} from '@element-plus/icons-vue';
import QuestionCard from '@/components/question/QuestionCard.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { useQuestion } from '@/composables/question/useQuestion';
import { difficultyToLevel } from '@/utils/question/difficulty-level';
import type { Question } from '@/types/question/question';

const router = useRouter();
const { questions, loading, total, fetchQuestions, removeQuestion, saveQuestion, loadCourseOptions } = useQuestion();

const pageNum = ref(1);
const pageSize = ref(10);
const selectedCourseId = ref<number | null>(null);
const selectedType = ref<string>('ALL');
const selectedDifficulty = ref<string>('ALL');
const keyword = ref<string>('');

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
  await loadCourseFilterOptions();
  await loadQuestions();
});

async function loadCourseFilterOptions() {
  courseOptions.value = await loadCourseOptions();
}

async function loadQuestions() {
  await fetchQuestions({
    page: pageNum.value,
    pageSize: pageSize.value,
    courseId: selectedCourseId.value || undefined,
    type: selectedType.value !== 'ALL' ? selectedType.value : undefined,
    difficulty: difficultyToLevel(selectedDifficulty.value),
    keyword: keyword.value.trim() || undefined
  });
}

function resetPageAndLoad() {
  pageNum.value = 1;
  loadQuestions();
}

function handleCourseFilter(value: number | null) {
  selectedCourseId.value = value;
  resetPageAndLoad();
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

async function handleDeleteQuestion(id: number) {
  try {
    await removeQuestion(id);
    ElMessage.success('试题已从题库中移除');
    await loadQuestions();
  } catch {
    ElMessage.error('删除试题失败，请稍后重试');
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
    background: #FFFFFF;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;

    .header-left {
      max-width: 680px;

      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 8px;
        flex-wrap: wrap;

        .header-icon {
          font-size: 22px;
        }

        .main-title {
          margin: 0;
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
          letter-spacing: -0.3px;
        }

        .capsule-count-tag {
          padding: 2px 12px;
          border-radius: 9999px; // 长圆
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          color: #1677FF;
          font-size: 11.5px;
          font-weight: 600;
        }
      }

      .sub-desc {
        margin: 0;
        font-size: 13.5px;
        color: #64748B;
        line-height: 1.6;
      }
    }

    .header-right-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .capsule-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 42px;
        padding: 0 20px;
        border-radius: 9999px; // 纯正长圆
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        transition: all 0.22s ease;

        &--ai {
          background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
          color: #FFFFFF;
          box-shadow: 0 4px 14px rgba(114, 46, 209, 0.28);

          .pill-bubble {
            font-size: 11px;
            padding: 1px 8px;
            border-radius: 9999px;
            background: rgba(255, 255, 255, 0.22);
          }

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(114, 46, 209, 0.38);
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
          }
        }
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

        .filter-left-col {
          display: flex;
          align-items: center;
          gap: 14px;
        }

        .filter-right-search {
          .capsule-search-box {
            display: flex;
            align-items: center;
            width: 320px;
            height: 38px;
            padding: 0 14px;
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

  // 4. 空状态
  .empty-questions-panel {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px dashed #CBD5E1;
    padding: 60px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .empty-emoji {
      font-size: 42px;
      margin-bottom: 12px;
    }

    .empty-title {
      font-size: 16.5px;
      font-weight: 700;
      color: #1E293B;
      margin: 0 0 8px 0;
    }

    .empty-text {
      font-size: 13.5px;
      color: #94A3B8;
      margin: 0 0 20px 0;
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
