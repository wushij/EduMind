<template>
  <div class="recommendations-page-container">
    <!-- 1. 顶部轻量 Banner（学习中心子页） -->
    <LearningSubpageHero
      title="学习推荐"
      subtitle="根据薄弱考点与学习进度，为你推荐巩固练习与拓展资源"
    >
      <template #actions>
        <button
          type="button"
          class="capsule-switch-btn"
          @click="router.push('/learning')"
        >
          <el-icon class="btn-icon"><Back /></el-icon>
          <span>返回我的学习</span>
        </button>
      </template>
      <template #toolbar>
        <el-select
          v-model="selectedCourseId"
          placeholder="选择课程"
          style="width: 220px"
          @change="onCourseChange"
        >
          <el-option v-for="c in courseListOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </template>
    </LearningSubpageHero>

    <!-- 2. 长圆跑道分类 Tabs (Pill Tabs) -->
    <div class="recommend-tabs-bar">
      <div class="pill-tabs-track">
        <button
          v-for="tab in categoryTabs"
          :key="tab.value"
          type="button"
          class="pill-tab-item"
          :class="{ active: selectedCategory === tab.value }"
          @click="selectedCategory = tab.value"
        >
          <el-icon v-if="tab.icon" class="tab-icon"><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
          <span class="tab-count-pill">{{ getCategoryCount(tab.value) }}</span>
        </button>
      </div>

      <!-- 排序模式长圆切换 -->
      <input
        v-model="searchKeyword"
        type="text"
        class="recommend-inline-search"
        placeholder="搜索推荐题目、微课或考点..."
      />

      <div class="sort-capsules-group">
        <span class="sort-label">排序：</span>
        <button
          type="button"
          class="sort-pill-btn"
          :class="{ active: sortBy === 'score' }"
          @click="sortBy = 'score'"
        >
          <span>AI 匹配度优先</span>
        </button>
        <button
          type="button"
          class="sort-pill-btn"
          :class="{ active: sortBy === 'time' }"
          @click="sortBy = 'time'"
        >
          <span>耗时最短</span>
        </button>
      </div>
    </div>

    <!-- 3. 二级胶囊筛选工具栏 (Course / Difficulty) -->
    <div class="filter-capsule-bar">
      <div class="filter-group">
        <span class="filter-title">所属课程：</span>
        <div class="pill-options-row">
          <span
            v-for="c in courseFilterOptions"
            :key="c"
            class="filter-pill-opt"
            :class="{ active: selectedCourse === c }"
            @click="selectedCourse = c"
          >
            {{ c }}
          </span>
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-title">难度分级：</span>
        <div class="pill-options-row">
          <span
            v-for="d in difficultyOptions"
            :key="d.value"
            class="filter-pill-opt"
            :class="{ active: selectedDifficulty === d.value }"
            @click="selectedDifficulty = d.value"
          >
            {{ d.label }}
          </span>
        </div>
      </div>
    </div>

    <!-- 4. 推荐卡片响应式网格 -->
    <div v-if="filteredList.length > 0" class="recommendations-grid">
      <RecommendationCard
        v-for="item in filteredList"
        :key="item.id"
        :item="item"
        @start="handleStart"
        @discuss="handleDiscuss"
      />
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-recommend-panel">
      <div class="empty-icon">
        <el-icon :size="46" color="#94A3B8"><Search /></el-icon>
      </div>
      <h3 class="empty-title">未找到符合条件的个性化推荐</h3>
      <p class="empty-hint">建议调整上方课程、分类或难度筛选条件，或直接向课程 AI 助教提问</p>
      <button
        type="button"
        class="capsule-reset-btn"
        @click="resetFilters"
      >
        <el-icon class="btn-icon"><RefreshRight /></el-icon>
        <span>重置所有筛选条件</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Back,
  List,
  WarningFilled,
  Aim,
  VideoPlay,
  Top,
  Search,
  RefreshRight
} from '@element-plus/icons-vue';
import LearningSubpageHero from '@/components/learning/LearningSubpageHero.vue';
import RecommendationCard from '@/components/learning/RecommendationCard.vue';
import { useRecommendations } from '@/composables/learning/useRecommendations';
import { useLearningHome } from '@/composables/learning/useLearningHome';
import { resolveCourseRoute } from '@/utils/learning/course-route';
import type { RecommendationItem } from '@/types/learning/recommendation';

const router = useRouter();
const { items, loading, fetchRecommendations } = useRecommendations();
const { courseOptions, primaryCourseId, refresh } = useLearningHome();

const selectedCourseId = ref<number | null>(null);
const courseListOptions = computed(() => courseOptions.value);

onMounted(async () => {
  await refresh();
  selectedCourseId.value = primaryCourseId.value;
  if (selectedCourseId.value != null) {
    await fetchRecommendations(selectedCourseId.value);
  }
});

function onCourseChange(courseId: number) {
  void fetchRecommendations(courseId);
}

const selectedCategory = ref<string>('ALL');
const selectedCourse = ref<string>('全部课程');
const selectedDifficulty = ref<string>('ALL');
const sortBy = ref<'score' | 'time'>('score');
const searchKeyword = ref<string>('');

const categoryTabs = [
  { label: '全部精选推荐', value: 'ALL', icon: List },
  { label: '薄弱考点巩固', value: '薄弱巩固', icon: WarningFilled },
  { label: '核心必刷题', value: '核心必刷', icon: Aim },
  { label: '名师精选微课', value: '精选课件', icon: VideoPlay },
  { label: '拔高进阶挑战', value: '拓展进阶', icon: Top }
];

const courseFilterOptions = computed(() => ['全部课程', ...courseListOptions.value.map((c) => c.name)]);

const difficultyOptions = [
  { label: '全部难度', value: 'ALL' },
  { label: '基础巩固', value: 'EASY' },
  { label: '中等难度', value: 'MEDIUM' },
  { label: '较难拓展', value: 'HARD' }
];

function getCategoryCount(cat: string) {
  if (cat === 'ALL') return items.value.length;
  return items.value.filter((item) => item.category === cat).length;
}

const filteredList = computed(() => {
  let list = [...items.value];

  // 1. 分类过滤
  if (selectedCategory.value !== 'ALL') {
    list = list.filter((item) => item.category === selectedCategory.value);
  }

  // 2. 课程过滤
  if (selectedCourse.value !== '全部课程') {
    list = list.filter((item) => item.courseName === selectedCourse.value);
  }

  // 3. 难度过滤
  if (selectedDifficulty.value !== 'ALL') {
    list = list.filter((item) => item.difficulty === selectedDifficulty.value);
  }

  // 4. 关键词搜索
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase();
    list = list.filter(
      (item) =>
        item.title.toLowerCase().includes(kw) ||
        item.knowledgePoint.toLowerCase().includes(kw) ||
        item.description.toLowerCase().includes(kw)
    );
  }

  // 5. 排序
  if (sortBy.value === 'score') {
    list.sort((a, b) => b.matchScore - a.matchScore);
  } else if (sortBy.value === 'time') {
    list.sort((a, b) => a.estimatedMinutes - b.estimatedMinutes);
  }

  return list;
});

function resetFilters() {
  selectedCategory.value = 'ALL';
  selectedCourse.value = '全部课程';
  selectedDifficulty.value = 'ALL';
  searchKeyword.value = '';
}

function handleStart(item: RecommendationItem) {
  const courseId = item.courseId ?? selectedCourseId.value;
  if (item.type === 'exercise') {
    router.push({
      path: '/learning/practice',
      query: courseId != null ? { courseId: String(courseId) } : undefined
    });
  } else if (courseId != null) {
    router.push(resolveCourseRoute(courseId, 'resources'));
  }
}

function handleDiscuss(item: RecommendationItem) {
  const courseId = item.courseId ?? selectedCourseId.value;
  if (courseId != null) {
    router.push(resolveCourseRoute(courseId, 'ai'));
  }
}
</script>

<style scoped lang="scss">
.recommendations-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. 顶部操作栏
  .header-status-capsules {
    display: flex;
    align-items: center;
    gap: 10px;

    .pill-stat-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 34px;
      padding: 0 16px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.9);
      border: 1px solid rgba(226, 232, 240, 0.8);
      color: #059669;
      font-size: 12.5px;
      font-weight: 600;

      .chip-sparkle {
        font-size: 13px;
      }
    }

    .capsule-switch-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 34px;
      padding: 0 18px;
      border-radius: 9999px;
      background: #FFFFFF;
      border: 1px solid #CBD5E1;
      color: #334155;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;

      .btn-icon {
        font-size: 14px;
      }

      &:hover {
        background: #EFF6FF;
        color: #1677FF;
        border-color: #93C5FD;
      }
    }
  }

  // 2. 长圆药丸 Tabs 栏
  .recommend-tabs-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #FFFFFF;
    border-radius: 18px;
    padding: 8px 14px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);
    gap: 12px;
    flex-wrap: wrap;

    .recommend-inline-search {
      width: 240px;
      height: 36px;
      padding: 0 14px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
      background: #F8FAFC;
      font-size: 13px;
      color: #334155;
      outline: none;

      &:focus {
        border-color: #1677FF;
        background: #FFFFFF;
      }
    }

    .pill-tabs-track {
      display: flex;
      align-items: center;
      gap: 8px;
      overflow-x: auto;

      .pill-tab-item {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 16px;
        border-radius: 9999px; // 纯正长圆药丸
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        color: #64748B;
        font-size: 13.5px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        white-space: nowrap;

        .tab-icon {
          font-size: 14px;
        }

        .tab-count-pill {
          padding: 1px 8px;
          border-radius: 9999px;
          background: #E2E8F0;
          color: #475569;
          font-size: 11px;
          font-weight: 600;
        }

        &:hover {
          color: #1677FF;
          border-color: #BFDBFE;
          background: #EFF6FF;
        }

        &.active {
          background: #1677FF;
          border-color: #1677FF;
          color: #FFFFFF;
          font-weight: 600;
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

          .tab-count-pill {
            background: rgba(255, 255, 255, 0.25);
            color: #FFFFFF;
          }
        }
      }
    }

    .sort-capsules-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .sort-label {
        font-size: 12.5px;
        color: #94A3B8;
      }

      .sort-pill-btn {
        height: 30px;
        padding: 0 12px;
        border-radius: 9999px; // 长圆
        background: #F1F5F9;
        border: 1px solid #E2E8F0;
        color: #64748B;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          color: #1677FF;
        }

        &.active {
          background: #EFF6FF;
          border-color: #93C5FD;
          color: #1677FF;
          font-weight: 600;
        }
      }
    }
  }

  // 3. 二级胶囊筛选条
  .filter-capsule-bar {
    display: flex;
    align-items: center;
    gap: 28px;
    background: #FFFFFF;
    border-radius: 14px;
    padding: 12px 20px;
    border: 1px solid #E2E8F0;
    flex-wrap: wrap;

    .filter-group {
      display: flex;
      align-items: center;
      gap: 10px;

      .filter-title {
        font-size: 12.5px;
        color: #64748B;
        font-weight: 500;
      }

      .pill-options-row {
        display: flex;
        align-items: center;
        gap: 6px;

        .filter-pill-opt {
          display: inline-flex;
          align-items: center;
          height: 28px;
          padding: 0 12px;
          border-radius: 9999px; // 纯正长圆
          font-size: 12px;
          color: #475569;
          cursor: pointer;
          transition: all 0.18s;

          &:hover {
            color: #1677FF;
            background: #F1F5F9;
          }

          &.active {
            background: #EFF6FF;
            color: #1677FF;
            font-weight: 600;
            border: 1px solid #BFDBFE;
          }
        }
      }
    }
  }

  // 4. 卡片网格
  .recommendations-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  // 5. 空状态
  .empty-recommend-panel {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px dashed #CBD5E1;
    padding: 60px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .empty-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
    }

    .empty-title {
      font-size: 16px;
      font-weight: 700;
      color: #1E293B;
      margin: 0 0 8px 0;
    }

    .empty-hint {
      font-size: 13px;
      color: #94A3B8;
      margin: 0 0 18px 0;
    }

    .capsule-reset-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      height: 38px;
      padding: 0 24px;
      border-radius: 9999px; // 长圆按钮
      background: #1677FF;
      color: #FFFFFF;
      font-size: 13.5px;
      font-weight: 600;
      border: none;
      cursor: pointer;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);
      transition: all 0.2s;

      .btn-icon {
        font-size: 15px;
      }

      &:hover {
        background: #4096FF;
      }
    }
  }
}

@media (max-width: 1024px) {
  .recommendations-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
