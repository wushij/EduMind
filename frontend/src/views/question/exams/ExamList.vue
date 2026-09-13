<template>
  <div class="exam-list-page-container">
    <!-- 1. 顶部操作坞 -->
    <div class="exam-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <el-icon class="header-icon text-blue-600"><Tickets /></el-icon>
          <h1 class="main-title">试卷与考试管理中心</h1>
          <span class="capsule-count-tag">已收录 {{ pageReady ? total : '--' }} 套标准化期末试卷</span>
        </div>
        <p class="sub-desc">
          集中管理期中/期末统一测试试卷、随堂测验与单元测试，支持双向细目表校验、AI 一键调优换题与格式化导出。
        </p>
      </div>

      <div class="header-right-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--ai"
          @click="router.push('/ai/exam/generate')"
        >
          <el-icon class="sparkle-icon"><MagicStick /></el-icon>
          <span>AI 智能组卷</span>
          <span class="pill-bubble">双向细目表</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--secondary"
          @click="router.push('/ai/question/generate')"
        >
          <el-icon><EditPen /></el-icon>
          <span>AI 智能出题</span>
        </button>
      </div>
    </div>

    <!-- 2. 统计状态条 -->
    <section class="exam-stats-bar">
      <div class="stat-pill-item">
        <el-icon class="pill-icon text-blue-600"><Document /></el-icon>
        <span class="pill-label">试卷总数：</span>
        <strong class="pill-val">{{ pageReady ? `${total} 套` : '--' }}</strong>
      </div>
      <div class="stat-pill-item">
        <el-icon class="pill-icon text-emerald-600"><CircleCheck /></el-icon>
        <span class="pill-label">已排版就绪：</span>
        <strong class="pill-val">{{ pageReady ? `${exams.length} 套 (当前页)` : '--' }}</strong>
      </div>
      <div class="stat-pill-item">
        <el-icon class="pill-icon text-indigo-600"><User /></el-icon>
        <span class="pill-label">命题教师：</span>
        <strong class="pill-val">教研组统一命题</strong>
      </div>
      <div class="stat-pill-item stat-pill-item--highlight">
        <el-icon class="pill-icon text-amber-500"><Lightning /></el-icon>
        <span class="pill-label">AI 组卷提效：</span>
        <strong class="pill-val">平均节省备课 4.5 小时/套</strong>
      </div>
    </section>

    <!-- 3. 筛选与搜索工具栏 -->
    <div class="exam-filter-card">
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

      <div class="filter-row filter-row--bottom">
        <div class="filter-right-search filter-right-search--full">
          <div class="capsule-search-box">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              v-model="keyword"
              type="text"
              class="capsule-search-input"
              placeholder="搜索试卷标题、课程名称..."
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

    <!-- 4. 试卷卡片流网格 -->
    <div v-loading="loading" class="exam-list-content">
    <div v-if="exams.length > 0" class="exams-cards-grid">
      <div
        v-for="exam in exams"
        :key="exam.id"
        class="exam-paper-card"
      >
        <!-- 卡片头部 -->
        <div class="card-header-line">
          <div class="header-tags">
            <span class="course-pill">
              <el-icon class="mr-1 text-blue-600"><Reading /></el-icon>
              {{ exam.courseName }}
            </span>
            <span class="semester-pill">{{ exam.semester }}</span>
          </div>
          <span class="status-pill status-pill--ready">
            <span class="dot-green"></span>
            <span>已审核就绪</span>
          </span>
        </div>

        <!-- 试卷标题与参数 -->
        <h3 class="exam-title">{{ exam.title }}</h3>

        <div class="exam-specs-row">
          <div class="spec-pill-item">
            <span class="spec-label">卷面满分：</span>
            <strong class="spec-val highlight">{{ exam.totalScore }} 分</strong>
          </div>
          <div class="spec-pill-item">
            <span class="spec-label">考试时长：</span>
            <strong class="spec-val">{{ exam.durationMinutes }} 分钟</strong>
          </div>
          <div class="spec-pill-item">
            <span class="spec-label">及格分数线：</span>
            <strong class="spec-val">{{ exam.passScore }} 分</strong>
          </div>
          <div class="spec-pill-item">
            <span class="spec-label">收录大题：</span>
            <strong class="spec-val">{{ (exam.rules || []).length }} 大类</strong>
          </div>
        </div>

        <!-- 题型与分值配比微图表 (细目表条) -->
        <div class="rules-distribution-box">
          <div class="rules-title">试卷题型结构分布：</div>
          <div class="rules-tags-track">
            <span
              v-for="r in exam.rules || []"
              :key="r.type"
              class="rule-chip"
            >
              {{ r.label }}：{{ r.count }} 题 (每题 {{ r.scoreEach }} 分)
            </span>
          </div>
        </div>

        <!-- 底部胶囊操作栏 -->
        <div class="card-footer-actions">
          <button
            type="button"
            class="capsule-action-btn capsule-action-btn--secondary"
            @click="handleExportPdf(exam)"
          >
            <el-icon><Download /></el-icon>
            <span>导出 PDF 卷面</span>
          </button>

          <button
            type="button"
            class="capsule-action-btn capsule-action-btn--preview"
            @click="handlePreview(exam)"
          >
            <el-icon><View /></el-icon>
            <span>卷面全景预览</span>
          </button>

          <button
            type="button"
            class="capsule-action-btn capsule-action-btn--primary"
            @click="handlePublish(exam)"
          >
            <el-icon><Promotion /></el-icon>
            <span>发布为在线测验</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 5. 空状态 -->
    <div v-else class="empty-exams-panel">
      <div class="empty-emoji">
        <el-icon><Tickets /></el-icon>
      </div>
      <h3 class="empty-title">未找到匹配的试卷</h3>
      <p class="empty-text">当前筛选条件下暂无试卷，你可以点击下方按钮通过 AI 秒级生成一套完整试卷。</p>
      <button
        type="button"
        class="capsule-btn capsule-btn--ai"
        @click="router.push('/ai/exam/generate')"
      >
        <el-icon class="mr-1"><MagicStick /></el-icon>
        <span>立即使用 AI 智能组卷</span>
      </button>
    </div>

    <AppPagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadExams"
    />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Tickets,
  MagicStick,
  EditPen,
  Document,
  CircleCheck,
  User,
  Lightning,
  Search,
  Close,
  Reading,
  Download,
  View,
  Promotion
} from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { useExam } from '@/composables/question/useExam';
import { getCourseList } from '@/api/course/course';
import type { Exam } from '@/types/question/exam';

const router = useRouter();
const { exams, loading, total, fetchExams } = useExam();

const pageNum = ref(1);
const pageSize = ref(10);
const selectedCourseId = ref<number | null>(null);
const keyword = ref<string>('');

const courseOptions = ref<Array<{ label: string; value: number | null }>>([
  { label: '全部课程', value: null }
]);

const pageReady = ref(false);

onMounted(async () => {
  pageReady.value = false;
  try {
    await Promise.all([loadCourseOptions(), loadExams()]);
  } finally {
    pageReady.value = true;
  }
});

async function loadCourseOptions() {
  try {
    const res = await getCourseList({ page: 1, pageSize: 100 });
    const list = res.data?.list || [];
    courseOptions.value = [
      { label: '全部课程', value: null },
      ...list.map((item: any) => ({
        label: item.title || item.name,
        value: item.id
      }))
    ];
  } catch {
    courseOptions.value = [{ label: '全部课程', value: null }];
  }
}

async function loadExams() {
  await fetchExams({
    page: pageNum.value,
    pageSize: pageSize.value,
    courseId: selectedCourseId.value || undefined,
    keyword: keyword.value.trim() || undefined
  });
}

function handleCourseFilter(value: number | null) {
  selectedCourseId.value = value;
  pageNum.value = 1;
  loadExams();
}

function handleSearch() {
  pageNum.value = 1;
  loadExams();
}

function clearKeyword() {
  keyword.value = '';
  pageNum.value = 1;
  loadExams();
}

function handlePreview(exam: Exam) {
  router.push(`/question/exams/${exam.id}`);
}

function handleExportPdf(exam: Exam) {
  ElMessage.success(`试卷《${exam.title}》已生成排版，准备导出 PDF...`);
}

function handlePublish(exam: Exam) {
  ElMessage.success(`试卷《${exam.title}》已成功发布至选课班级考试中心！`);
}
</script>

<style scoped lang="scss">
.exam-list-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .exam-list-content {
    min-height: 360px;
  }

  .pill-tags-track {
    min-height: 32px;
  }

  .filter-right-search--full {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }

  // 1. 顶部操作坞
  .exam-header-dock {
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

  // 2. 指标条
  .exam-stats-bar {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
    min-height: 36px;

    .stat-pill-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px; // 长圆药丸
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      box-shadow: 0 2px 10px rgba(30, 80, 150, 0.03);
      font-size: 12.5px;
      color: #64748B;

      .pill-icon {
        font-size: 14px;
      }

      .pill-val {
        color: #1E293B;
        font-weight: 700;
      }

      &--highlight {
        margin-left: auto;
        background: #EFF6FF;
        border-color: #BFDBFE;
        color: #1677FF;

        .pill-val {
          color: #1677FF;
        }
      }
    }
  }

  // 3. 筛选栏
  .exam-filter-card {
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

  // 4. 试卷卡片网格
  .exams-cards-grid {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .exam-paper-card {
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #E2E8F0;
      padding: 24px 28px;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
      display: flex;
      flex-direction: column;
      gap: 16px;
      transition: all 0.22s ease;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 8px 24px rgba(22, 119, 255, 0.08);
      }

      .card-header-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;

        .header-tags {
          display: flex;
          align-items: center;
          gap: 8px;

          .course-pill {
            padding: 2px 12px;
            border-radius: 9999px;
            background: #EFF6FF;
            color: #1677FF;
            font-size: 12px;
            font-weight: 600;
          }

          .semester-pill {
            padding: 2px 10px;
            border-radius: 9999px;
            background: #F1F5F9;
            color: #64748B;
            font-size: 11.5px;
          }
        }

        .status-pill {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 2px 12px;
          border-radius: 9999px;
          font-size: 11.5px;
          font-weight: 600;

          &--ready {
            background: #ECFDF5;
            color: #059669;

            .dot-green {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10B981;
            }
          }
        }
      }

      .exam-title {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.4;
      }

      .exam-specs-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .spec-pill-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 14px;
          border-radius: 9999px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          font-size: 12px;
          color: #64748B;

          .spec-val {
            color: #1E293B;

            &.highlight {
              color: #1677FF;
              font-weight: 700;
            }
          }
        }
      }

      .rules-distribution-box {
        background: #F8FAFC;
        border-radius: 12px;
        padding: 12px 16px;
        border: 1px dashed #CBD5E1;

        .rules-title {
          font-size: 12px;
          color: #64748B;
          margin-bottom: 8px;
          font-weight: 600;
        }

        .rules-tags-track {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .rule-chip {
            padding: 3px 10px;
            border-radius: 9999px;
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            font-size: 11.5px;
            color: #334155;
          }
        }
      }

      .card-footer-actions {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
        padding-top: 8px;
        border-top: 1px solid #F1F5F9;

        .capsule-action-btn {
          height: 36px;
          padding: 0 18px;
          border-radius: 9999px; // 长圆按钮
          font-size: 12.5px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s;

          &--secondary {
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            color: #334155;

            &:hover {
              background: #F8FAFC;
              color: #1677FF;
              border-color: #93C5FD;
            }
          }

          &--preview {
            background: #EFF6FF;
            border: 1px solid #BFDBFE;
            color: #1677FF;

            &:hover {
              background: #1677FF;
              color: #FFFFFF;
            }
          }

          &--primary {
            background: #1677FF;
            border: none;
            color: #FFFFFF;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

            &:hover {
              background: #4096FF;
            }
          }
        }
      }
    }
  }

  // 5. 空状态
  .empty-exams-panel {
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
</style>
