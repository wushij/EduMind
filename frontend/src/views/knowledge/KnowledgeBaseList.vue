<template>
  <div v-loading="loading" class="knowledge-base-page-container profile-page-shell">
    <!-- 顶部科技毛玻璃 Hero 横幅 (完全替代原有静态图片，与 AI 消耗明细对齐) -->
    <KnowledgeBaseHeroCard
      :loading="loading"
      :rag-stats="ragStats"
      :rag-stats-loading="ragStatsLoading"
      :knowledge-base-count="knowledgeBases.length"
      :total-docs="totalDocs"
      :total-chunks="totalChunks"
      :last-updated-text="lastUpdatedText"
      :format-number="formatNumber"
      @create="handleOpenCreate"
      @open-retrieval="handleOpenRetrieval()"
      @open-rag-dashboard="router.push('/knowledge/rag-dashboard')"
      @refresh="handleRefresh(true)"
    />

    <!-- 4 维核心数据指标卡片矩阵 (1:1 像素级对齐 AI 消耗明细 MetricsGrid) -->
    <KnowledgeBaseMetricsGrid
      :knowledge-base-count="knowledgeBases.length"
      :total-docs="totalDocs"
      :total-chunks="totalChunks"
      :active-courses-count="activeCoursesCount"
      :categories-count="categoryTabs.length"
      :format-number="formatNumber"
    />

    <!-- 综合筛选与视图控制坞 -->
    <div class="kb-tabs-filter-bar">
      <!-- 分类胶囊切换 -->
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

      <!-- 右侧控制：状态过滤 + 关键词搜索 + 视图模式切换 -->
      <div class="filter-right-actions">
        <el-select
          v-model="selectedStatus"
          placeholder="状态筛选"
          class="status-filter-select"
          style="width: 150px"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>

        <div class="search-input-wrap">
          <input
            v-model="searchKeyword"
            type="text"
            class="kb-inline-search"
            placeholder="搜索知识库名称、文档或课程..."
          />
          <el-icon class="search-icon"><Search /></el-icon>
        </div>

        <!-- 视图切换模式：卡片网格 vs 结构化表格 -->
        <div class="view-mode-toggle">
          <button
            type="button"
            class="mode-toggle-btn"
            :class="{ 'is-active': viewMode === 'card' }"
            title="卡片网格视图"
            @click="viewMode = 'card'"
          >
            <el-icon><Grid /></el-icon>
          </button>
          <button
            type="button"
            class="mode-toggle-btn"
            :class="{ 'is-active': viewMode === 'table' }"
            title="结构化明细列表视图"
            @click="viewMode = 'table'"
          >
            <el-icon><List /></el-icon>
          </button>
        </div>

        <span class="total-hint">共展示 {{ filteredList.length }} 个知识库</span>
      </div>
    </div>

    <!-- 视图 1：卡片网格视图 -->
    <div v-if="viewMode === 'card' && filteredList.length > 0" class="knowledge-cards-grid">
      <KnowledgeBaseCard
        v-for="item in filteredList"
        :key="item.id"
        :item="item"
        @open="handleOpenDetail"
        @upload="handleUploadDoc"
        @edit="handleEditKb"
        @reindex="handleTriggerIndex"
        @retrieval="handleOpenRetrieval"
        @delete="handleDeleteKb"
      />
    </div>

    <!-- 视图 2：结构化明细表格视图 (与 AI 消耗明细日志表格同源风格) -->
    <div v-else-if="viewMode === 'table' && filteredList.length > 0" class="kb-table-card">
      <el-table
        :data="filteredList"
        style="width: 100%"
        :header-cell-style="tableHeaderStyle"
        :row-class-name="() => 'kb-table-row'"
      >
        <el-table-column label="知识库名称与简介" min-width="260">
          <template #default="{ row }">
            <div class="kb-table-name-cell">
              <div class="name-icon-box">
                <el-icon><FolderOpened /></el-icon>
              </div>
              <div class="name-info-col">
                <div class="name-line">
                  <span class="kb-row-name" :title="row.name" @click="handleOpenDetail(row)">
                    {{ row.name }}
                  </span>
                </div>
                <span class="kb-row-desc" :title="row.description">
                  {{ row.description || '暂无描述信息' }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="所属类别" width="120" align="center">
          <template #default="{ row }">
            <span class="table-category-pill" :class="`table-category-pill--${row.category.toLowerCase()}`">
              {{ row.categoryLabel }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="关联课程" min-width="170">
          <template #default="{ row }">
            <span v-if="row.courseName" class="table-course-pill">
              <el-icon class="course-icon"><Reading /></el-icon>
              <span>{{ row.courseName }}</span>
            </span>
            <span v-else class="text-muted">公共通用库</span>
          </template>
        </el-table-column>

        <el-table-column label="入库文档" width="120" align="center">
          <template #default="{ row }">
            <span class="metric-tabular-pill text-teal">
              <strong>{{ formatNumber(row.documentCount) }}</strong> 篇
            </span>
          </template>
        </el-table-column>

        <el-table-column label="向量切片" width="130" align="center">
          <template #default="{ row }">
            <span class="metric-tabular-pill text-purple">
              <strong>{{ formatNumber(row.chunkCount) }}</strong> 个
            </span>
          </template>
        </el-table-column>

        <el-table-column label="向量化状态" width="140" align="center">
          <template #default="{ row }">
            <span class="table-status-pill" :class="`status--${row.vectorStatus.toLowerCase()}`">
              <span class="status-dot"></span>
              <span>{{ row.vectorStatusLabel }}</span>
            </span>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.updatedAt ? row.updatedAt.replace('T', ' ').slice(0, 16) : '刚刚' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="快捷操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions-cell">
              <el-button
                link
                type="primary"
                size="small"
                @click="handleOpenDetail(row)"
              >
                详情
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="handleUploadDoc(row)"
              >
                上传
              </el-button>
              <el-button
                link
                type="info"
                size="small"
                @click="handleOpenRetrieval(row)"
              >
                测试
              </el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleTableCommand(cmd, row)">
                <el-button link type="default" size="small">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑基本信息</el-dropdown-item>
                    <el-dropdown-item command="reindex">全量重建索引</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: #ef4444">
                      删除知识库
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 空状态提示 -->
    <div v-else class="empty-kb-panel">
      <el-icon class="empty-icon"><Search /></el-icon>
      <h3 class="empty-title">未找到匹配的知识库</h3>
      <p class="empty-hint">建议调整上方搜索关键字或状态筛选条件，也可以点击上方按钮快速创建。</p>
      <button
        type="button"
        class="capsule-reset-btn"
        @click="resetFilters"
      >
        <span>重置筛选条件</span>
      </button>
    </div>

    <!-- 创建 / 编辑知识库对话框 (已打通真实课程) -->
    <KnowledgeBaseCreateDialog
      v-model:visible="showCreateDialog"
      :is-edit="isEditMode"
      :create-form="createForm"
      :create-rules="createRules"
      :category-options="categoryTabs.filter((t) => t.value !== 'ALL')"
      :courses="courses"
      @confirm="handleConfirmSave"
    />

    <!-- 课件文档快速上传弹窗 -->
    <KnowledgeBaseUploadDialog
      v-model:visible="showUploadDialog"
      :knowledge-bases="knowledgeBases"
      :initial-kb-id="uploadTargetKbId"
      @success="handleRefresh(false)"
    />

    <!-- 即时语义检索测试抽屉 -->
    <KnowledgeBaseQuickRetrievalDrawer
      v-model:visible="showRetrievalDrawer"
      :knowledge-bases="knowledgeBases"
      :initial-kb-id="retrievalInitialKbId"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Search, Grid, List, FolderOpened, Reading } from '@element-plus/icons-vue';

const router = useRouter();
import KnowledgeBaseHeroCard from '@/components/knowledge/KnowledgeBaseHeroCard.vue';
import KnowledgeBaseMetricsGrid from '@/components/knowledge/KnowledgeBaseMetricsGrid.vue';
import KnowledgeBaseCard from '@/components/knowledge/KnowledgeBaseCard.vue';
import KnowledgeBaseCreateDialog from '@/components/knowledge/KnowledgeBaseCreateDialog.vue';
import KnowledgeBaseUploadDialog from '@/components/knowledge/KnowledgeBaseUploadDialog.vue';
import KnowledgeBaseQuickRetrievalDrawer from '@/components/knowledge/KnowledgeBaseQuickRetrievalDrawer.vue';
import { useKnowledgeBaseList } from '@/composables/knowledge/useKnowledgeBaseList';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';

const {
  loading,
  knowledgeBases,
  courses,
  selectedCategory,
  selectedStatus,
  statusOptions,
  searchKeyword,
  viewMode,
  lastUpdatedText,
  showCreateDialog,
  isEditMode,
  showUploadDialog,
  uploadTargetKbId,
  showRetrievalDrawer,
  retrievalInitialKbId,
  categoryTabs,
  totalDocs,
  totalChunks,
  ragStats,
  ragStatsLoading,
  activeCoursesCount,
  filteredList,
  createForm,
  createRules,
  formatNumber,
  getCategoryCount,
  resetFilters,
  handleRefresh,
  handleOpenCreate,
  handleEditKb,
  handleConfirmSave,
  handleOpenDetail,
  handleUploadDoc,
  handleOpenRetrieval,
  handleTriggerIndex,
  handleDeleteKb
} = useKnowledgeBaseList();

const tableHeaderStyle = {
  background: '#F8FAFC',
  color: '#475569',
  fontWeight: 600,
  fontSize: '12.5px',
  borderBottom: '1px solid #E2E8F0',
  padding: '12px 0'
};

function handleTableCommand(cmd: string, row: KnowledgeBase) {
  if (cmd === 'edit') handleEditKb(row);
  else if (cmd === 'reindex') handleTriggerIndex(row);
  else if (cmd === 'delete') handleDeleteKb(row);
}
</script>

<style scoped lang="scss">
@use '@/styles/profile-page-shell.scss';

.knowledge-base-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .kb-tabs-filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #ffffff;
    border-radius: 20px;
    padding: 10px 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
    gap: 12px;
    flex-wrap: wrap;

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
        border-radius: 9999px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        color: #64748b;
        font-size: 13px;
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
          background: #e2e8f0;
          color: #475569;
          font-size: 11px;
          font-weight: 600;
        }

        &:hover {
          color: #1677ff;
          background: #eff6ff;
          border-color: #bfdbfe;
        }

        &.active {
          background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
          border-color: #1677ff;
          color: #ffffff;
          font-weight: 600;
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

          .tab-count-pill {
            background: rgba(255, 255, 255, 0.25);
            color: #ffffff;
          }
        }
      }
    }

    .filter-right-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .status-filter-select {
        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          background: #f8fafc;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
        }
      }

      .search-input-wrap {
        position: relative;
        display: flex;
        align-items: center;

        .kb-inline-search {
          width: 240px;
          height: 36px;
          padding: 0 34px 0 16px;
          border-radius: 9999px;
          border: 1px solid #e2e8f0;
          background: #f8fafc;
          font-size: 13px;
          color: #1e293b;
          outline: none;
          transition: all 0.2s;

          &:focus {
            border-color: #1677ff;
            background: #ffffff;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
            width: 270px;
          }
        }

        .search-icon {
          position: absolute;
          right: 12px;
          color: #94a3b8;
          font-size: 14px;
          pointer-events: none;
        }
      }

      .view-mode-toggle {
        display: inline-flex;
        align-items: center;
        background: #f1f5f9;
        padding: 3px;
        border-radius: 9999px;
        border: 1px solid #e2e8f0;
        gap: 2px;

        .mode-toggle-btn {
          width: 32px;
          height: 30px;
          border-radius: 9999px;
          border: none;
          background: transparent;
          color: #64748b;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          font-size: 15px;
          transition: all 0.2s;

          &:hover:not(.is-active) {
            color: #1e293b;
          }

          &.is-active {
            background: #ffffff;
            color: #2563eb;
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
          }
        }
      }

      .total-hint {
        font-size: 12.5px;
        color: #94a3b8;
        white-space: nowrap;
      }
    }
  }

  // 1. 卡片视图
  .knowledge-cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  // 2. 表格明细视图
  .kb-table-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    padding: 16px 20px;
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
    overflow: hidden;

    :deep(.el-table) {
      --el-table-border-color: transparent;
      --el-table-row-hover-bg-color: #f8faff;
      background: transparent;

      &::before {
        display: none;
      }

      .kb-table-row td.el-table__cell {
        padding: 14px 0;
        border-bottom: 1px solid #f1f5f9;
      }

      .kb-table-row:last-child td.el-table__cell {
        border-bottom: none;
      }
    }

    .kb-table-name-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .name-icon-box {
        width: 38px;
        height: 38px;
        border-radius: 12px;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        color: #2563eb;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        flex-shrink: 0;
        border: 1px solid #bfdbfe;
      }

      .name-info-col {
        display: flex;
        flex-direction: column;
        gap: 3px;
        min-width: 0;

        .kb-row-name {
          font-size: 13.5px;
          font-weight: 700;
          color: #0f172a;
          cursor: pointer;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;

          &:hover {
            color: #1677ff;
          }
        }

        .kb-row-desc {
          font-size: 12px;
          color: #94a3b8;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }

    .table-category-pill {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 9999px;
      font-size: 11.5px;
      font-weight: 600;

      &--major {
        background: #eff6ff;
        color: #1677ff;
        border: 1px solid #bfdbfe;
      }

      &--common {
        background: #f5f3ff;
        color: #7c3aed;
        border: 1px solid #ddd6fe;
      }

      &--exam {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
      }

      &--courseware {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid #fde68a;
      }
    }

    .table-course-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 3px 10px;
      border-radius: 9999px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      font-size: 12px;
      color: #475569;

      .course-icon {
        font-size: 13px;
        color: #64748b;
      }
    }

    .text-muted {
      color: #94a3b8;
      font-size: 12px;
    }

    .metric-tabular-pill {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 9999px;
      font-size: 12px;
      font-variant-numeric: tabular-nums;

      &.text-teal {
        background: #f0fdfa;
        color: #0d9488;
        border: 1px solid #ccfbf1;
      }

      &.text-purple {
        background: #faf5ff;
        color: #9333ea;
        border: 1px solid #f3e8ff;
      }
    }

    .table-status-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      border-radius: 9999px;
      font-size: 11.5px;
      font-weight: 600;

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      &.status--synced {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
        .status-dot {
          background: #10b981;
          box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
        }
      }

      &.status--parsing {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid #fde68a;
        .status-dot {
          background: #f59e0b;
        }
      }

      &.status--pending {
        background: #f1f5f9;
        color: #64748b;
        border: 1px solid #cbd5e1;
        .status-dot {
          background: #94a3b8;
        }
      }
    }

    .time-text {
      font-size: 12px;
      color: #64748b;
      font-variant-numeric: tabular-nums;
    }

    .table-actions-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
  }

  // 3. 空状态
  .empty-kb-panel {
    background: #ffffff;
    border-radius: 20px;
    border: 1px dashed #cbd5e1;
    padding: 64px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .empty-icon {
      font-size: 48px;
      color: #94a3b8;
      margin-bottom: 12px;
    }

    .empty-title {
      font-size: 16px;
      font-weight: 700;
      color: #1e293b;
      margin: 0 0 8px 0;
    }

    .empty-hint {
      font-size: 13px;
      color: #94a3b8;
      margin: 0 0 20px 0;
      max-width: 420px;
      line-height: 1.5;
    }

    .capsule-reset-btn {
      height: 38px;
      padding: 0 24px;
      border-radius: 9999px;
      background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
      color: #ffffff;
      font-size: 13px;
      font-weight: 600;
      border: none;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

      &:hover {
        background: linear-gradient(135deg, #4096ff 0%, #1d4ed8 100%);
      }
    }
  }
}

@media (max-width: 1280px) {
  .knowledge-cards-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 768px) {
  .knowledge-cards-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
