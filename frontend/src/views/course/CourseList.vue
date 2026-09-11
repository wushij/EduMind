<template>
  <div class="course-list-page">
    <!-- 顶部工作台标题与快速操作栏 -->
    <div class="page-top-bar">
      <div class="title-group">
        <div class="main-title-row">
          <h1 class="page-title">课程中心</h1>
          <span class="pill-badge-count">共 {{ total }} 门课程</span>
        </div>
        <p class="page-sub-title">
          管理与修读学科空间，全流程支持 AI 智能出题、知识库沉淀与流式助教问答
        </p>
      </div>

      <div class="action-group">
        <button type="button" class="capsule-btn capsule-btn--default" @click="handleJoinCourse">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <line x1="20" y1="8" x2="20" y2="14"></line>
            <line x1="23" y1="11" x2="17" y2="11"></line>
          </svg>
          <span>加入课程</span>
        </button>

        <button type="button" class="capsule-btn capsule-btn--primary" @click="router.push('/course/create')">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>创建新课程</span>
        </button>
      </div>
    </div>

    <!-- 长圆胶囊筛选与检索工具栏 -->
    <div class="filter-toolbar-panel">
      <!-- 左侧：药丸标签组 (Pill Tabs) -->
      <div class="pill-tabs-nav">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          class="pill-tab-item"
          :class="{ active: currentStatusTab === tab.value }"
          @click="handleStatusTabChange(tab.value)"
        >
          <span>{{ tab.label }}</span>
          <span class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <!-- 右侧：长圆跑道搜索框与学期筛选 -->
      <div class="search-filter-row">
        <!-- 学期筛选下拉 (长圆) -->
        <el-select
          v-model="selectedSemester"
          placeholder="全部学期"
          class="semester-select"
          @change="handleFilterChange"
        >
          <el-option label="全部学期" value="ALL" />
          <el-option label="2026秋季学期" value="2026秋季学期" />
          <el-option label="2026春季学期" value="2026春季学期" />
        </el-select>

        <!-- 纯正长圆搜索条 (1:1 继承登录页输入框设计基因) -->
        <div class="capsule-search-container">
          <span class="search-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </span>
          <input
            v-model="searchKeyword"
            type="text"
            class="capsule-input-native"
            placeholder="搜索课程名称、代号或教师..."
            @input="handleFilterChange"
          />
          <el-icon v-if="searchKeyword" class="clear-btn" @click="clearKeyword"><CircleClose /></el-icon>
        </div>
      </div>
    </div>

    <!-- 课程卡片网格列表 (加载中 / 内容 / 空状态) -->
    <div v-loading="loading" class="course-grid-container">
      <div v-if="courses.length > 0" class="course-grid">
        <CourseCard
          v-for="course in courses"
          :key="course.id"
          :course="course"
        />
      </div>

      <!-- 优雅空状态 -->
      <div v-else class="empty-state-wrapper">
        <div class="empty-icon-box">
          <svg viewBox="0 0 24 24" class="empty-svg" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            <line x1="9" y1="9" x2="15" y2="9"></line>
            <line x1="9" y1="13" x2="13" y2="13"></line>
          </svg>
        </div>
        <h4 class="empty-title">暂无符合条件的课程</h4>
        <p class="empty-desc">换个关键词搜索试试，或点击下方重置筛选</p>
        <button type="button" class="capsule-btn capsule-btn--default" @click="resetFilters">
          重置全部筛选
        </button>
      </div>
    </div>

    <!-- 底部长圆分页 -->
    <div v-if="courses.length > 0" class="pagination-footer">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        class="custom-pagination"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { CircleClose } from '@element-plus/icons-vue';
import CourseCard from '@/components/course/CourseCard.vue';
import { useCourse } from '@/composables/course/useCourse';

const router = useRouter();
const { courses, loading, total, fetchCourses } = useCourse();

const searchKeyword = ref('');
const currentStatusTab = ref('ALL');
const selectedSemester = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(12);

const statusTabs = computed(() => [
  { label: '全部课程', value: 'ALL', count: total.value },
  { label: '进行中', value: 'ACTIVE', count: courses.value.filter(c => c.status === 'ACTIVE' || c.status === 1).length },
  { label: '已结课', value: 'ARCHIVED', count: courses.value.filter(c => c.status === 'ARCHIVED' || c.status === 2).length }
]);

function loadData() {
  fetchCourses({
    keyword: searchKeyword.value,
    status: currentStatusTab.value,
    semester: selectedSemester.value
  });
}

function handleStatusTabChange(tabVal: string) {
  currentStatusTab.value = tabVal;
  loadData();
}

function handleFilterChange() {
  loadData();
}

function clearKeyword() {
  searchKeyword.value = '';
  loadData();
}

function resetFilters() {
  searchKeyword.value = '';
  currentStatusTab.value = 'ALL';
  selectedSemester.value = 'ALL';
  loadData();
}

function handlePageChange(page: number) {
  currentPage.value = page;
  loadData();
}

function handleJoinCourse() {
  ElMessage.info('正在开启全校课程选课通道与课程邀请码输入弹窗...');
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.course-list-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. 顶部工作台标题栏
  .page-top-bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    padding: 6px 0;

    .title-group {
      .main-title-row {
        display: flex;
        align-items: center;
        gap: 12px;

        .page-title {
          margin: 0;
          font-size: 24px;
          font-weight: 700;
          color: #0F172A;
          letter-spacing: -0.2px;
        }

        .pill-badge-count {
          display: inline-block;
          height: 22px;
          line-height: 22px;
          padding: 0 10px;
          border-radius: 9999px; // 长圆胶囊
          background: #EAF3FF;
          color: #1677FF;
          font-size: 12px;
          font-weight: 600;
        }
      }

      .page-sub-title {
        margin: 6px 0 0 0;
        font-size: 13.5px;
        color: #64748B;
        line-height: 1.5;
      }
    }

    .action-group {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  // 长圆按钮统一样式
  .capsule-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 40px;
    padding: 0 20px;
    border-radius: 9999px; // 纯正长圆
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
    white-space: nowrap;

    .btn-icon-svg {
      width: 16px;
      height: 16px;
    }

    &--primary {
      background: #1677FF;
      color: #FFFFFF;
      border: none;
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

      &:hover {
        background: #4096FF;
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);
      }

      &:active {
        background: #0958D9;
        transform: translateY(0);
      }
    }

    &--default {
      background: #FFFFFF;
      color: #334155;
      border: 1px solid #E2E8F0;

      &:hover {
        border-color: #CBD5E1;
        color: #1677FF;
        background: #F8FAFC;
        transform: translateY(-1px);
      }
    }
  }

  // 2. 长圆筛选与检索面板
  .filter-toolbar-panel {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 14px 20px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    // 药丸标签组
    .pill-tabs-nav {
      display: flex;
      align-items: center;
      gap: 6px;
      background: #F1F5F9;
      padding: 4px;
      border-radius: 9999px; // 长圆外框

      .pill-tab-item {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 32px;
        padding: 0 14px;
        border-radius: 9999px; // 药丸内按钮
        border: none;
        background: transparent;
        color: #64748B;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;

        .tab-count {
          font-size: 11px;
          padding: 1px 6px;
          border-radius: 9999px;
          background: rgba(148, 163, 184, 0.2);
          color: #475569;
        }

        &:hover {
          color: #1E293B;
        }

        &.active {
          background: #FFFFFF;
          color: #1677FF;
          font-weight: 600;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

          .tab-count {
            background: #EAF3FF;
            color: #1677FF;
          }
        }
      }
    }

    .search-filter-row {
      display: flex;
      align-items: center;
      gap: 12px;

      .semester-select {
        width: 150px;
      }

      // 纯正长圆跑道输入框 (对齐 Login.vue)
      .capsule-search-container {
        display: flex;
        align-items: center;
        width: 320px;
        height: 40px;
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 9999px; // 纯正长圆胶囊
        padding: 0 16px;
        box-sizing: border-box;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        &:hover {
          border-color: #CBD5E1;
        }

        &:focus-within {
          border-color: #1677FF;
          box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.16);
        }

        .search-icon {
          display: flex;
          align-items: center;
          color: #94A3B8;
          margin-right: 8px;

          .svg-icon {
            width: 16px;
            height: 16px;
          }
        }

        .capsule-input-native {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 13.5px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
            font-size: 13px;
          }
        }

        .clear-btn {
          cursor: pointer;
          color: #94A3B8;
          font-size: 12px;
          padding: 2px 4px;
          &:hover { color: #64748B; }
        }
      }
    }
  }

  // 3. 课程卡片网格
  .course-grid-container {
    min-height: 420px;

    .course-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 22px;
    }

    .empty-state-wrapper {
      padding: 60px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #EBF1F7;

      .empty-icon-box {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: #F1F5F9;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #94A3B8;
        margin-bottom: 16px;

        .empty-svg {
          width: 32px;
          height: 32px;
        }
      }

      .empty-title {
        margin: 0 0 8px 0;
        font-size: 16px;
        font-weight: 600;
        color: #1E293B;
      }

      .empty-desc {
        margin: 0 0 20px 0;
        font-size: 13px;
        color: #94A3B8;
      }
    }
  }

  // 4. 底部长圆分页
  .pagination-footer {
    display: flex;
    justify-content: center;
    padding: 12px 0 24px 0;
  }
}

// 响应式
@media (max-width: 860px) {
  .course-list-page {
    .page-top-bar {
      flex-direction: column;
      align-items: stretch;
    }

    .filter-toolbar-panel {
      flex-direction: column;
      align-items: stretch;

      .search-filter-row {
        flex-direction: column;
        align-items: stretch;

        .semester-select,
        .capsule-search-container {
          width: 100%;
        }
      }
    }
  }
}
</style>
