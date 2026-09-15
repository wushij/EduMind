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

    <!-- 底部分页 -->
    <AppPagination
      v-model:page-num="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />

    <!-- 加入课程长圆弹窗 -->
    <el-dialog
      v-model="showJoinDialog"
      title="加入课程修读空间"
      width="560px"
      append-to-body
      destroy-on-close
      class="capsule-course-dialog"
    >
      <div class="join-dialog-body">
        <!-- 药丸 Tab 切换 -->
        <div class="dialog-pill-tabs">
          <button
            type="button"
            class="dialog-pill-tab"
            :class="{ active: joinTab === 'code' }"
            @click="joinTab = 'code'"
          >
            <span>课程代号 / 邀请码选课</span>
          </button>
          <button
            type="button"
            class="dialog-pill-tab"
            :class="{ active: joinTab === 'browse' }"
            @click="joinTab = 'browse'"
          >
            <span>全校公开课程速选</span>
          </button>
        </div>

        <!-- 模式一：输入课程代号 -->
        <div v-if="joinTab === 'code'" class="tab-pane-code">
          <p class="dialog-tip">
            请输入教师公布的 6 位课程代号或邀请码（如 <strong>CS201</strong>、<strong>CS101</strong>、<strong>MATH101</strong>）
          </p>
          <div class="dialog-capsule-input">
            <input
              v-model="courseCodeInput"
              type="text"
              class="native-code-input"
              placeholder="例如：CS201"
              maxlength="20"
              @keyup.enter="handleJoinByCode"
            />
            <button
              type="button"
              class="capsule-submit-btn"
              :disabled="!courseCodeInput.trim() || joining"
              @click="handleJoinByCode"
            >
              <span>{{ joining ? '加入中...' : '立即选课加入' }}</span>
            </button>
          </div>
          <div class="quick-code-hints">
            <span class="hint-label">热门课程快速填入：</span>
            <span class="quick-code-tag" @click="courseCodeInput = 'CS201'">CS201 数据结构</span>
            <span class="quick-code-tag" @click="courseCodeInput = 'CS101'">CS101 Java程序设计</span>
            <span class="quick-code-tag" @click="courseCodeInput = 'MATH101'">MATH101 高等数学</span>
          </div>
        </div>

        <!-- 模式二：公开课程速选（需后端公开列表 API，暂仅支持代号加入） -->
        <div v-else class="tab-pane-browse">
          <div class="browse-empty-state">
            <p class="dialog-tip">暂无全校公开课程列表，请切换到「课程代号 / 邀请码选课」输入教师公布的代号。</p>
            <p class="dialog-tip muted">演示库示例代号：CS201、CS101、MATH101（以数据库 seed 为准）</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { CircleClose } from '@element-plus/icons-vue';
import CourseCard from '@/components/course/CourseCard.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { useCourse } from '@/composables/course/useCourse';

const router = useRouter();
const { courses, loading, total, fetchCourses, enrollCourseByCode } = useCourse();

const searchKeyword = ref('');
const currentStatusTab = ref('ALL');
const selectedSemester = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(10);

const showJoinDialog = ref(false);
const joinTab = ref<'code' | 'browse'>('code');
const courseCodeInput = ref('');
const joining = ref(false);

const statusTabs = computed(() => [
  { label: '全部课程', value: 'ALL', count: total.value },
  { label: '进行中', value: 'ACTIVE', count: courses.value.filter(c => c.status === 'ACTIVE' || c.status === 1).length },
  { label: '已结课', value: 'ARCHIVED', count: courses.value.filter(c => c.status === 'ARCHIVED' || c.status === 2).length }
]);

async function loadData() {
  try {
    await fetchCourses({
      keyword: searchKeyword.value,
      status: currentStatusTab.value,
      page: currentPage.value,
      pageSize: pageSize.value
    });
  } catch {
    // axios 拦截器已弹出错误提示
  }
}

function handleStatusTabChange(tabVal: string) {
  currentStatusTab.value = tabVal;
  currentPage.value = 1;
  loadData();
}

function handleFilterChange() {
  currentPage.value = 1;
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
  currentPage.value = 1;
  loadData();
}

function handleJoinCourse() {
  courseCodeInput.value = '';
  showJoinDialog.value = true;
}

async function handleJoinByCode() {
  if (!courseCodeInput.value.trim()) {
    ElMessage.warning('请输入课程代号或邀请码');
    return;
  }
  joining.value = true;
  try {
    const courseId = await enrollCourseByCode(courseCodeInput.value.trim());
    ElMessage.success('选课成功！已成功加入该课程空间！');
    showJoinDialog.value = false;
    courseCodeInput.value = '';
    loadData();
    if (courseId) {
      router.push(`/course/${courseId}/overview`);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加入课程失败，请检查课程代号是否存在');
  } finally {
    joining.value = false;
  }
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

// 加入课程长圆弹窗样式
.join-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .dialog-pill-tabs {
    display: flex;
    background: #F1F5F9;
    padding: 4px;
    border-radius: 9999px;
    gap: 4px;

    .dialog-pill-tab {
      flex: 1;
      height: 36px;
      border-radius: 9999px;
      border: none;
      background: transparent;
      font-size: 13.5px;
      font-weight: 500;
      color: #64748B;
      cursor: pointer;
      transition: all 0.2s ease;

      &.active {
        background: #FFFFFF;
        color: #1677FF;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      }
    }
  }

  .dialog-tip {
    margin: 0;
    font-size: 13px;
    color: #64748B;
    line-height: 1.6;

    strong {
      color: #1677FF;
    }
  }

  .dialog-capsule-input {
    display: flex;
    align-items: center;
    gap: 8px;
    background: #FFFFFF;
    border: 1.5px solid #E2E8F0;
    border-radius: 9999px;
    padding: 4px 6px 4px 18px;
    transition: all 0.2s ease;

    &:focus-within {
      border-color: #1677FF;
      box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
    }

    .native-code-input {
      flex: 1;
      border: none;
      outline: none;
      font-size: 15px;
      font-family: inherit;
      color: #0F172A;
      letter-spacing: 0.5px;

      &::placeholder {
        color: #94A3B8;
        font-size: 13.5px;
      }
    }

    .capsule-submit-btn {
      height: 38px;
      padding: 0 20px;
      border-radius: 9999px;
      border: none;
      background: #1677FF;
      color: #FFFFFF;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;

      &:hover:not(:disabled) {
        background: #4096FF;
        transform: translateY(-1px);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }

  .quick-code-hints {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;

    .hint-label {
      color: #94A3B8;
    }

    .quick-code-tag {
      padding: 3px 10px;
      border-radius: 9999px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      color: #475569;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover {
        background: #EAF3FF;
        color: #1677FF;
        border-color: #BFDBFE;
      }
    }
  }

  .quick-course-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-height: 280px;
    overflow-y: auto;
    padding-right: 4px;

    .quick-course-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      border-radius: 14px;
      background: #F8FAFC;
      border: 1px solid #F1F5F9;
      transition: all 0.2s ease;

      &:hover {
        border-color: #BFDBFE;
        background: #F0F7FF;
      }

      .course-mini-info {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .mini-code {
          font-size: 11px;
          color: #1677FF;
          font-weight: 600;
        }

        .mini-title {
          margin: 0;
          font-size: 14px;
          font-weight: 600;
          color: #0F172A;
        }

        .mini-teacher {
          font-size: 12px;
          color: #64748B;
        }
      }

      .capsule-btn-mini {
        height: 30px;
        padding: 0 16px;
        border-radius: 9999px;
        border: none;
        background: #1677FF;
        color: #FFFFFF;
        font-size: 12.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
        }
      }
    }
  }
}
</style>
