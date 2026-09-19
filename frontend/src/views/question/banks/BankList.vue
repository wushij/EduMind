<template>
  <div class="bank-list-page-container">
    <!-- 1. 顶部操作头区 (Hero Header Dock) -->
    <div class="bank-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <div class="icon-orb">
            <el-icon class="header-icon"><FolderOpened /></el-icon>
          </div>
          <div class="title-meta-col">
            <div class="title-badges-row">
              <h1 class="main-title">课程与通用题库中心</h1>
              <span class="capsule-count-tag">共 {{ total }} 个精选题库</span>
              <span class="capsule-count-tag capsule-count-tag--green">已收录 {{ totalQuestionsAcrossBanks }} 道试题</span>
              <span class="capsule-count-tag capsule-count-tag--purple">涵盖 {{ courses.length }} 门核心课</span>
            </div>
            <p class="sub-desc">
              归纳整理各课程专项试题集、历年统考与期中期末题库，全链路支持 AI 智能出题扩充、快捷组卷与导出排版。
            </p>
          </div>
        </div>
      </div>

      <div class="header-right-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--primary"
          @click="showCreateDialog = true"
        >
          <el-icon><Plus /></el-icon>
          <span>新建题库</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--ai"
          @click="router.push('/ai/question/generate')"
        >
          <el-icon><MagicStick /></el-icon>
          <span>AI 智能出题</span>
          <span class="pill-bubble">秒级出题</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--secondary"
          @click="router.push('/question/list')"
        >
          <el-icon><Reading /></el-icon>
          <span>管理试题大池</span>
        </button>
      </div>
    </div>

    <!-- 2. 筛选与实时搜索工具栏 (Filter & Search Toolbar) -->
    <div class="filter-capsule-card">
      <div class="filter-row filter-row--course-search">
        <span class="filter-label">所属课程：</span>
        <el-select
          v-model="selectedCourseId"
          placeholder="全部课程"
          clearable
          filterable
          class="filter-select filter-select--course"
        >
          <el-option
            v-for="c in courseFilterOptions"
            :key="String(c.value)"
            :label="c.label"
            :value="c.value"
          />
        </el-select>

        <div class="filter-search-col">
          <div class="search-dock-box">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="快速搜索题库名称、描述范围或关联课程..."
            />
            <button
              v-if="searchKeyword"
              type="button"
              class="clear-btn"
              @click="searchKeyword = ''"
            >
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <div class="filter-stat-hint">
          <span>展示 <strong>{{ filteredBanks.length }}</strong> / {{ total }} 个精选题库</span>
        </div>
      </div>
    </div>

    <!-- 3. 现代化题库卡片网格 (Responsive Card Grid) -->
    <div v-loading="loading" class="banks-grid-wrapper">
      <div v-if="filteredBanks.length > 0" class="banks-responsive-grid">
        <div
          v-for="b in filteredBanks"
          :key="b.id"
          class="bank-card-item"
          @click="router.push(`/question/banks/${b.id}`)"
        >
          <!-- 顶部装饰色条 -->
          <div class="card-accent-bar" />

          <!-- 顶部标签区 -->
          <div class="card-top-header">
            <div class="course-chip">
              <el-icon class="mr-1"><Collection /></el-icon>
              <span>{{ b.courseName || getCourseName(b.courseId) }}</span>
            </div>
            <span class="count-tag">
              <el-icon class="mr-1"><DocumentCopy /></el-icon>
              {{ b.questionCount || 0 }} 题
            </span>
          </div>

          <!-- 题库主体信息 -->
          <h3 class="bank-name" :title="b.name">{{ b.name }}</h3>
          <p class="bank-desc" :title="b.description">
            {{ b.description || '归集本课程核心专项试题与阶段测验题目，支持组卷抽调与导出排版。' }}
          </p>

          <!-- 元数据指标 -->
          <div class="bank-meta-row">
            <span class="meta-item">
              <span class="meta-label">卷面估分：</span>
              <strong class="score-text">约 {{ (b.questionCount || 0) * 5 }} 分</strong>
            </span>
            <span class="meta-item time-item">
              <el-icon class="time-icon"><Clock /></el-icon>
              <span>{{ b.updateTime ? b.updateTime.slice(0, 10) : '近期更新' }}</span>
            </span>
          </div>

          <!-- 卡片操作底栏 -->
          <div class="card-footer" @click.stop>
            <div class="footer-left-actions">
              <button
                type="button"
                class="card-pill-btn card-pill-btn--primary"
                @click="router.push(`/question/banks/${b.id}`)"
              >
                <span>进入维护</span>
              </button>
              <button
                type="button"
                class="card-pill-btn card-pill-btn--secondary"
                @click="handleFastCompose(b)"
              >
                <el-icon><Tickets /></el-icon>
                <span>组卷</span>
              </button>
            </div>

            <div class="footer-right-actions">
              <el-tooltip content="导出本题库试卷 (Markdown)" placement="top">
                <button
                  type="button"
                  class="card-icon-btn"
                  @click="exportSingleBankMarkdown(b)"
                >
                  <el-icon><Download /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="删除此题库" placement="top">
                <button
                  type="button"
                  class="card-icon-btn card-icon-btn--danger"
                  @click="handleDeleteBank(b)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state-panel">
        <div class="empty-halo-icon">
          <el-icon class="empty-icon"><FolderOpened /></el-icon>
        </div>
        <h3>暂无匹配的题库</h3>
        <p>未找到符合当前课程或关键词的题库，您可以点击下方按钮立即新建或使用 AI 智能出题。</p>
        <div class="empty-actions-row">
          <button type="button" class="capsule-btn capsule-btn--primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            <span>新建首个题库</span>
          </button>
          <button type="button" class="capsule-btn capsule-btn--ai" @click="router.push('/ai/question/generate')">
            <el-icon><MagicStick /></el-icon>
            <span>前往 AI 智能出题</span>
          </button>
        </div>
      </div>

      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadBanks"
      />
    </div>

    <!-- 4. 新建题库现代化对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建试题库"
      width="560px"
      destroy-on-close
      class="custom-bank-dialog"
    >
      <div class="dialog-banner-tip">
        <el-icon class="tip-icon"><Collection /></el-icon>
        <span>题库是试题的归纳容器。创建后可收录课程习题、按章节抽调，并支持一键组卷与导出。</span>
      </div>

      <el-form ref="dialogFormRef" :model="newBankForm" :rules="dialogRules" label-position="top">
        <el-form-item label="题库名称" prop="name">
          <el-input
            v-model="newBankForm.name"
            placeholder="例如：2026秋季数据结构期末高频冲刺库"
            maxlength="50"
            show-word-limit
          />
          <!-- 快捷名称预设标签 -->
          <div class="preset-chips-row">
            <span class="preset-label">快捷填充：</span>
            <span
              v-for="tag in presetNameTags"
              :key="tag"
              class="preset-chip"
              @click="applyPresetName(tag)"
            >
              + {{ tag }}
            </span>
          </div>
        </el-form-item>

        <el-form-item label="关联课程" prop="courseId">
          <el-select
            v-model="newBankForm.courseId"
            placeholder="请选择对应课程（支持输入关键字搜索）"
            filterable
            clearable
            class="w-full"
          >
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="c.title || c.name"
              :value="c.id"
            >
              <div class="course-option-item">
                <span class="c-title">{{ c.title || c.name }}</span>
                <span v-if="c.teacherName" class="c-teacher">主讲: {{ c.teacherName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="题库说明与收录范围">
          <el-input
            v-model="newBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="简要说明本题库的收录范围、重点章节与考点考查目标..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateBank">
          确认创建题库
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  FolderOpened,
  Plus,
  MagicStick,
  Reading,
  Search,
  Close,
  Tickets,
  Download,
  Delete,
  Clock,
  Collection,
  DocumentCopy
} from '@element-plus/icons-vue';
import { useBankList } from '@/composables/question/useBank';
import AppPagination from '@/components/common/AppPagination.vue';
import { computed } from 'vue';

const {
  router,
  loading,
  creating,
  showCreateDialog,
  dialogFormRef,
  banks,
  filteredBanks,
  courses,
  selectedCourseId,
  searchKeyword,
  pageNum,
  pageSize,
  total,
  totalQuestionsAcrossBanks,
  newBankForm,
  dialogRules,
  getCourseName,
  loadBanks,
  handleCreateBank,
  handleDeleteBank,
  exportSingleBankMarkdown
} = useBankList();

const courseFilterOptions = computed(() => [
  { label: '全部课程', value: null as number | null },
  ...courses.value.map((c) => ({
    label: String(c.title || c.name || `课程 ${c.id}`),
    value: c.id as number
  }))
]);

const presetNameTags = [
  '期末真题冲刺库',
  '阶段随堂练习集',
  '核心考点专项库',
  '高频易错题精选集'
];

function applyPresetName(tag: string) {
  const currentCourse = courses.value.find(c => c.id === newBankForm.courseId);
  const coursePrefix = currentCourse ? `${currentCourse.title || currentCourse.name} - ` : '';
  newBankForm.name = `${coursePrefix}${tag}`;
}

function handleFastCompose(bank: any) {
  router.push({
    path: '/question/exams/create',
    query: { bankId: bank.id, courseId: bank.courseId }
  });
}
</script>

<style scoped lang="scss">
.bank-list-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  box-sizing: border-box;

  .mr-1 {
    margin-right: 4px;
  }

  /* 1. 顶部操作头区 */
  .bank-header-dock {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 24px 28px;
    box-shadow: 0 4px 20px rgba(30, 80, 160, 0.04);
    gap: 20px;
    flex-wrap: wrap;

    .header-left {
      flex: 1;

      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 16px;

        .icon-orb {
          width: 48px;
          height: 48px;
          border-radius: 14px;
          background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          border: 1px solid #bfdbfe;

          .header-icon {
            font-size: 26px;
            color: #2563eb;
          }
        }

        .title-meta-col {
          display: flex;
          flex-direction: column;
          gap: 6px;

          .title-badges-row {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;

            .main-title {
              font-size: 22px;
              font-weight: 800;
              color: #0f172a;
              margin: 0;
              letter-spacing: -0.01em;
            }

            .capsule-count-tag {
              font-size: 12px;
              color: #2563eb;
              background: #eff6ff;
              border: 1px solid #dbeafe;
              padding: 2px 10px;
              border-radius: 9999px;
              font-weight: 600;

              &--green {
                color: #059669;
                background: #ecfdf5;
                border-color: #a7f3d0;
              }

              &--purple {
                color: #7c3aed;
                background: #f5f3ff;
                border-color: #ddd6fe;
              }
            }
          }

          .sub-desc {
            margin: 0;
            font-size: 13.5px;
            color: #64748b;
            line-height: 1.5;
          }
        }
      }
    }

    .header-right-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .capsule-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 9px 18px;
        border-radius: 9999px;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        border: none;

        &--primary {
          background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
          color: #ffffff;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
          }
        }

        &--ai {
          background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
          color: #ffffff;
          box-shadow: 0 4px 14px rgba(124, 58, 237, 0.28);
          position: relative;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(124, 58, 237, 0.35);
          }

          .pill-bubble {
            background: rgba(255, 255, 255, 0.24);
            font-size: 11px;
            padding: 1px 6px;
            border-radius: 9999px;
            margin-left: 2px;
          }
        }

        &--secondary {
          background: #ffffff;
          color: #334155;
          border: 1px solid #cbd5e1;

          &:hover {
            background: #f8fafc;
            border-color: #94a3b8;
            color: #0f172a;
          }
        }
      }
    }
  }

  /* 2. 筛选与搜索卡片 */
  .filter-capsule-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 18px 24px;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);
    display: flex;
    flex-direction: column;
    gap: 16px;

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      &--course-search {
        justify-content: flex-start;
      }

      .filter-label {
        font-size: 13.5px;
        font-weight: 600;
        color: #475569;
        white-space: nowrap;
        min-width: 70px;
      }

      .filter-select--course {
        width: min(280px, 100%);
        min-width: 200px;
        flex-shrink: 0;
      }

      .filter-search-col {
        flex: 1;
        min-width: 220px;
        max-width: 480px;

        .search-dock-box {
          display: flex;
          align-items: center;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 9999px;
          padding: 6px 14px;
          transition: all 0.2s;

          &:focus-within {
            background: #ffffff;
            border-color: #3b82f6;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
          }

          .search-icon {
            color: #94a3b8;
            font-size: 16px;
            margin-right: 8px;
            flex-shrink: 0;
          }

          .search-input {
            border: none;
            background: transparent;
            outline: none;
            font-size: 13px;
            color: #0f172a;
            width: 100%;
            min-width: 0;

            &::placeholder {
              color: #94a3b8;
            }
          }

          .clear-btn {
            background: none;
            border: none;
            color: #94a3b8;
            cursor: pointer;
            padding: 0;
            display: flex;
            align-items: center;
            flex-shrink: 0;

            &:hover {
              color: #475569;
            }
          }
        }
      }

      .filter-stat-hint {
        font-size: 13px;
        color: #64748b;
        margin-left: auto;
        white-space: nowrap;

        strong {
          color: #2563eb;
        }
      }

      .pill-tags-track {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .filter-pill-tag {
          padding: 5px 14px;
          border-radius: 9999px;
          font-size: 13px;
          color: #64748b;
          background: #f1f5f9;
          cursor: pointer;
          transition: all 0.15s ease;
          border: 1px solid transparent;

          .tag-count {
            font-size: 11.5px;
            opacity: 0.8;
            margin-left: 2px;
          }

          &:hover {
            background: #e2e8f0;
            color: #0f172a;
          }

          &.active {
            background: #2563eb;
            color: #ffffff;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
          }
        }
      }
    }
  }

  /* 3. 题库卡片响应式网格 */
  .banks-grid-wrapper {
    width: 100%;

    .banks-responsive-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
      gap: 20px;
    }

    .bank-card-item {
      position: relative;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 20px;
      padding: 22px 24px;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
      cursor: pointer;
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      display: flex;
      flex-direction: column;
      overflow: hidden;

      &:hover {
        transform: translateY(-3px);
        border-color: #93c5fd;
        box-shadow: 0 10px 28px rgba(37, 99, 235, 0.1);

        .bank-name {
          color: #2563eb;
        }

        .card-accent-bar {
          background: linear-gradient(90deg, #2563eb 0%, #7c3aed 100%);
        }
      }

      .card-accent-bar {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: #e2e8f0;
        transition: background 0.3s;
      }

      .card-top-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .course-chip {
          display: inline-flex;
          align-items: center;
          font-size: 12px;
          font-weight: 600;
          color: #0284c7;
          background: #f0f9ff;
          border: 1px solid #bae6fd;
          padding: 2px 10px;
          border-radius: 9999px;
          max-width: 220px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .count-tag {
          display: inline-flex;
          align-items: center;
          font-size: 12px;
          color: #059669;
          background: #ecfdf5;
          padding: 2px 10px;
          border-radius: 9999px;
          font-weight: 700;
          border: 1px solid #a7f3d0;
        }
      }

      .bank-name {
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 8px 0;
        line-height: 1.4;
        transition: color 0.2s;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .bank-desc {
        font-size: 13px;
        color: #64748b;
        margin: 0 0 16px 0;
        line-height: 1.6;
        min-height: 42px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .bank-meta-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;
        font-size: 12px;
        color: #64748b;

        .meta-label {
          color: #94a3b8;
        }

        .score-text {
          color: #f59e0b;
          font-weight: 600;
        }

        .time-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          color: #94a3b8;
        }
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #f1f5f9;
        padding-top: 14px;
        margin-top: auto;

        .footer-left-actions {
          display: flex;
          align-items: center;
          gap: 8px;

          .card-pill-btn {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 5px 14px;
            border-radius: 9999px;
            font-size: 12.5px;
            font-weight: 600;
            cursor: pointer;
            border: none;
            transition: all 0.15s;

            &--primary {
              background: #eff6ff;
              color: #2563eb;
              border: 1px solid #bfdbfe;

              &:hover {
                background: #2563eb;
                color: #ffffff;
              }
            }

            &--secondary {
              background: #f8fafc;
              color: #475569;
              border: 1px solid #e2e8f0;

              &:hover {
                background: #f1f5f9;
                color: #0f172a;
              }
            }
          }
        }

        .footer-right-actions {
          display: flex;
          align-items: center;
          gap: 6px;

          .card-icon-btn {
            width: 32px;
            height: 32px;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
            background: #ffffff;
            color: #64748b;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.15s;

            &:hover {
              background: #f1f5f9;
              color: #2563eb;
              border-color: #bfdbfe;
            }

            &--danger:hover {
              background: #fef2f2;
              color: #ef4444;
              border-color: #fecaca;
            }
          }
        }
      }
    }

    .empty-state-panel {
      text-align: center;
      padding: 70px 20px;
      background: #ffffff;
      border-radius: 20px;
      border: 1px dashed #cbd5e1;

      .empty-halo-icon {
        width: 64px;
        height: 64px;
        border-radius: 20px;
        background: #f1f5f9;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;

        .empty-icon {
          font-size: 32px;
          color: #94a3b8;
        }
      }

      h3 {
        font-size: 18px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 8px 0;
      }

      p {
        font-size: 14px;
        color: #64748b;
        margin: 0 0 24px 0;
        max-width: 480px;
        margin-left: auto;
        margin-right: auto;
      }

      .empty-actions-row {
        display: flex;
        justify-content: center;
        gap: 12px;
        flex-wrap: wrap;

        .capsule-btn {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 9px 20px;
          border-radius: 9999px;
          font-size: 13.5px;
          font-weight: 600;
          cursor: pointer;
          border: none;

          &--primary {
            background: #2563eb;
            color: #ffffff;
          }

          &--ai {
            background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
            color: #ffffff;
          }
        }
      }
    }
  }

  /* 4. 弹窗定制 */
  .dialog-banner-tip {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    background: #eff6ff;
    border: 1px solid #dbeafe;
    border-radius: 12px;
    padding: 12px 16px;
    margin-bottom: 18px;
    font-size: 13px;
    color: #1e40af;
    line-height: 1.5;

    .tip-icon {
      font-size: 16px;
      margin-top: 2px;
      color: #2563eb;
    }
  }

  .preset-chips-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 8px;
    flex-wrap: wrap;

    .preset-label {
      font-size: 12px;
      color: #94a3b8;
    }

    .preset-chip {
      font-size: 12px;
      color: #2563eb;
      background: #eff6ff;
      border: 1px dashed #bfdbfe;
      padding: 2px 8px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.15s;

      &:hover {
        background: #dbeafe;
      }
    }
  }

  .course-option-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;

    .c-title {
      font-weight: 600;
      color: #0f172a;
    }

    .c-teacher {
      font-size: 12px;
      color: #94a3b8;
    }
  }

  .w-full {
    width: 100%;
  }
}
</style>
