<template>
  <div class="bank-list-page-container">
    <!-- 顶部操作头区 -->
    <div class="bank-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <el-icon class="header-icon"><FolderOpened /></el-icon>
          <h1 class="main-title">课程与通用题库中心</h1>
          <span class="capsule-count-tag">共 {{ total }} 个精选题库</span>
        </div>
        <p class="sub-desc">
          归纳整理各课程专项试题集、历年统考与期中期末题库，支持快捷组卷与试题穿梭调度。
        </p>
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
      </div>
    </div>

    <!-- 课程筛选行 -->
    <div class="filter-capsule-card">
      <div class="filter-row">
        <span class="filter-label">所属课程：</span>
        <div class="pill-tags-track">
          <span
            class="filter-pill-tag"
            :class="{ active: selectedCourseId === null }"
            @click="handleCourseFilter(null)"
          >
            全部课程
          </span>
          <span
            v-for="c in courses"
            :key="c.id"
            class="filter-pill-tag"
            :class="{ active: selectedCourseId === c.id }"
            @click="handleCourseFilter(c.id)"
          >
            {{ c.title }}
          </span>
        </div>
      </div>
    </div>

    <!-- 题库卡片网格 -->
    <div v-loading="loading" class="banks-grid-wrapper">
      <div v-if="banks.length > 0" class="banks-stack">
        <div
          v-for="b in banks"
          :key="b.id"
          class="bank-card-item"
          @click="router.push(`/question/banks/${b.id}`)"
        >
          <div class="card-top-header">
            <div class="course-chip">{{ b.courseName || getCourseName(b.courseId) }}</div>
            <span class="count-tag">{{ b.questionCount || 0 }} 题</span>
          </div>

          <h3 class="bank-name">{{ b.name }}</h3>
          <p class="bank-desc">{{ b.description || '暂无详细描述信息' }}</p>

          <div class="card-footer">
            <span class="date-text">更新于 {{ b.updateTime ? b.updateTime.slice(0, 10) : '近期' }}</span>
            <el-button type="primary" link size="small">
              <span>进入题库维护</span>
              <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state-panel">
        <el-icon class="empty-icon"><FolderOpened /></el-icon>
        <h3>暂无匹配的题库</h3>
        <p>您可以点击右上角“新建题库”为您的课程创建首个专属试题库。</p>
        <el-button type="primary" @click="showCreateDialog = true">立即新建题库</el-button>
      </div>

      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadBanks"
      />
    </div>

    <!-- 新建题库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建试题库"
      width="540px"
      destroy-on-close
    >
      <el-form ref="dialogFormRef" :model="newBankForm" :rules="dialogRules" label-position="top">
        <el-form-item label="题库名称" prop="name">
          <el-input v-model="newBankForm.name" placeholder="例如：2026秋季数据结构期末高频冲刺库" />
        </el-form-item>

        <el-form-item label="关联课程" prop="courseId">
          <el-select v-model="newBankForm.courseId" placeholder="请选择对应课程" class="w-full">
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="c.title"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题库说明">
          <el-input
            v-model="newBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="简要说明本题库的收录范围与考察重点..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateBank">
          确认创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { FolderOpened, Plus, ArrowRight } from '@element-plus/icons-vue';
import { useBankList } from '@/composables/question/useBank';
import AppPagination from '@/components/common/AppPagination.vue';

const {
  router,
  loading,
  creating,
  showCreateDialog,
  dialogFormRef,
  banks,
  courses,
  selectedCourseId,
  pageNum,
  pageSize,
  total,
  newBankForm,
  dialogRules,
  handleCourseFilter,
  getCourseName,
  loadBanks,
  handleCreateBank
} = useBankList();
</script>

<style scoped lang="scss">
.bank-list-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .bank-header-dock {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 24px 28px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 24px;
          color: #2563eb;
          display: inline-flex;
          align-items: center;
        }

        .main-title {
          font-size: 22px;
          font-weight: 700;
          color: #0f172a;
          margin: 0;
        }

        .capsule-count-tag {
          font-size: 12px;
          color: #2563eb;
          background: #eff6ff;
          padding: 3px 10px;
          border-radius: 9999px;
          font-weight: 600;
        }
      }

      .sub-desc {
        margin: 8px 0 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .capsule-btn--primary {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 10px 22px;
      border-radius: 9999px;
      background: #2563eb;
      color: #ffffff;
      border: none;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
      transition: all 0.2s;

      &:hover {
        background: #1d4ed8;
        transform: translateY(-1px);
      }
    }
  }

  .filter-capsule-card {
    width: 100%;
    box-sizing: border-box;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 18px 24px;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;

      .filter-label {
        font-size: 13px;
        font-weight: 600;
        color: #475569;
        white-space: nowrap;
      }

      .pill-tags-track {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .filter-pill-tag {
          padding: 4px 14px;
          border-radius: 9999px;
          font-size: 13px;
          color: #64748b;
          background: #f1f5f9;
          cursor: pointer;
          transition: all 0.15s;

          &:hover {
            background: #e2e8f0;
            color: #0f172a;
          }

          &.active {
            background: #2563eb;
            color: #ffffff;
            font-weight: 600;
          }
        }
      }
    }
  }

  .banks-grid-wrapper {
    width: 100%;
  }

  .banks-stack {
    display: flex;
    flex-direction: column;
    gap: 20px;
    width: 100%;
  }

  .bank-card-item {
    width: 100%;
    box-sizing: border-box;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 24px 28px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    cursor: pointer;
    transition: all 0.22s ease;
    display: flex;
    flex-direction: column;

    &:hover {
      border-color: #93c5fd;
      box-shadow: 0 8px 24px rgba(22, 119, 255, 0.08);
    }

    .card-top-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .course-chip {
        font-size: 12px;
        font-weight: 600;
        color: #1677ff;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        padding: 2px 12px;
        border-radius: 9999px;
      }

      .count-tag {
        font-size: 12px;
        color: #059669;
        background: #ecfdf5;
        padding: 2px 10px;
        border-radius: 9999px;
        font-weight: 600;
        border: 1px solid #a7f3d0;
      }
    }

    .bank-name {
      font-size: 17px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 8px 0;
      line-height: 1.4;
    }

    .bank-desc {
      font-size: 13px;
      color: #64748b;
      margin: 0 0 18px 0;
      line-height: 1.6;
      flex: 1;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-top: 1px dashed #f1f5f9;
      padding-top: 14px;

      .date-text {
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }

  .empty-state-panel {
    text-align: center;
    padding: 60px 20px;
    background: #ffffff;
    border-radius: 16px;
    border: 1px dashed #cbd5e1;

    .empty-icon {
      font-size: 44px;
      display: block;
      margin-bottom: 12px;
    }

    h3 {
      font-size: 18px;
      color: #0f172a;
      margin-bottom: 6px;
    }

    p {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 20px;
    }
  }

  .w-full {
    width: 100%;
  }
}
</style>
