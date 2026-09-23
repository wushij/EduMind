<template>
  <div class="dashboard-todo-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">待办任务中心</h2>
        <span class="page-subtitle">集中处理教学协同、作业批阅、学情待办与重要教务提醒</span>
      </div>
      <el-button type="primary" :icon="Refresh" @click="fetchSummary" :loading="loading">
        刷新待办
      </el-button>
    </div>

    <!-- 顶部状态摘要卡片 -->
    <div class="todo-summary-cards">
      <div class="summary-card">
        <div class="card-icon card-icon--blue">
          <el-icon><List /></el-icon>
        </div>
        <div class="card-info">
          <span class="card-num">{{ todoItems.length }}</span>
          <span class="card-label">待处理事项</span>
        </div>
      </div>

      <div class="summary-card">
        <div class="card-icon card-icon--red">
          <el-icon><WarningFilled /></el-icon>
        </div>
        <div class="card-info">
          <span class="card-num">{{ urgentCount }}</span>
          <span class="card-label">加急与关键待办</span>
        </div>
      </div>

      <div class="summary-card">
        <div class="card-icon card-icon--green">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="card-info">
          <span class="card-num">{{ summary?.pendingGradingCount ?? 0 }}</span>
          <span class="card-label">待批改作业试卷</span>
        </div>
      </div>
    </div>

    <!-- 待办任务清单卡片 -->
    <el-card shadow="never" class="todo-list-card">
      <template #header>
        <div class="card-header-flex">
          <div class="tab-filters">
            <el-radio-group v-model="filterType" size="small">
              <el-radio-button label="ALL">全部待办 ({{ todoItems.length }})</el-radio-button>
              <el-radio-button label="URGENT">仅看加急 ({{ urgentCount }})</el-radio-button>
            </el-radio-group>
          </div>
          <span class="header-tip">点击任务项可快速进入对应业务处理界面</span>
        </div>
      </template>

      <div v-loading="loading" class="todo-items-wrapper">
        <template v-if="filteredItems.length > 0">
          <div
            v-for="item in filteredItems"
            :key="item.id"
            class="todo-item-row"
            :class="{ 'todo-item-row--urgent': item.urgent }"
            @click="handleTodoClick(item)"
          >
            <div class="item-left-col">
              <span class="status-indicator" :style="{ backgroundColor: item.dotColor }"></span>
              <div class="item-meta">
                <div class="item-title-row">
                  <span class="item-title">{{ item.title }}</span>
                  <el-tag v-if="item.urgent" size="small" type="danger" effect="plain" class="urgent-tag">
                    加急优先
                  </el-tag>
                </div>
                <span class="item-hint">所属业务链路：{{ item.route }}</span>
              </div>
            </div>

            <div class="item-right-col">
              <span class="item-count-badge" :class="{ 'item-count-badge--urgent': item.urgent }">
                {{ item.count }}
              </span>
              <el-button type="primary" size="small" plain class="action-btn">
                立即处理 &gt;
              </el-button>
            </div>
          </div>
        </template>

        <div v-else class="empty-state">
          <el-icon class="empty-icon"><CircleCheck /></el-icon>
          <p class="empty-title">当前暂无待办事项</p>
          <p class="empty-desc">各项教学活动与作业已按期推进，可前往课程中心或 AI 工具广场探索更多能力。</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Refresh, List, WarningFilled, CircleCheck } from '@element-plus/icons-vue';
import { useDashboard, type TodoItem } from '@/composables/dashboard/useDashboard';

const {
  summary,
  loading,
  todoItems,
  fetchSummary,
  handleTodoClick
} = useDashboard();

const filterType = ref<'ALL' | 'URGENT'>('ALL');

const urgentCount = computed(() => {
  const list = todoItems.value || [];
  return list.filter((i: TodoItem) => Boolean(i.urgent)).length;
});

const filteredItems = computed<TodoItem[]>(() => {
  const list = todoItems.value || [];
  if (filterType.value === 'URGENT') {
    return list.filter((i: TodoItem) => Boolean(i.urgent));
  }
  return list;
});
</script>

<style scoped lang="scss">
.dashboard-todo-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-left {
      .page-title {
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 4px 0;
      }

      .page-subtitle {
        font-size: 13px;
        color: #64748B;
      }
    }
  }

  .todo-summary-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }

    .summary-card {
      background: #FFFFFF;
      border-radius: 14px;
      padding: 18px 20px;
      border: 1px solid #E2E8F0;
      display: flex;
      align-items: center;
      gap: 16px;

      .card-icon {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;

        &--blue { background: #EFF6FF; color: #2563EB; }
        &--red { background: #FEF2F2; color: #EF4444; }
        &--green { background: #ECFDF5; color: #10B981; }
      }

      .card-info {
        display: flex;
        flex-direction: column;

        .card-num {
          font-size: 24px;
          font-weight: 800;
          color: #0F172A;
          line-height: 1.2;
        }

        .card-label {
          font-size: 12px;
          color: #64748B;
        }
      }
    }
  }

  .todo-list-card {
    border-radius: 16px;
    border: 1px solid #E2E8F0;

    .card-header-flex {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .header-tip {
        font-size: 12px;
        color: #94A3B8;
      }
    }

    .todo-items-wrapper {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .todo-item-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px 18px;
        background: #F8FAFC;
        border-radius: 12px;
        border: 1px solid #EDF2F7;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #EFF6FF;
          border-color: #BFDBFE;
          transform: translateY(-1px);
        }

        &--urgent {
          background: #FEF2F2;
          border-color: #FEE2E2;

          &:hover {
            background: #FEE2E2;
          }
        }

        .item-left-col {
          display: flex;
          align-items: center;
          gap: 14px;

          .status-indicator {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            flex-shrink: 0;
          }

          .item-meta {
            display: flex;
            flex-direction: column;
            gap: 4px;

            .item-title-row {
              display: flex;
              align-items: center;
              gap: 8px;

              .item-title {
                font-size: 14px;
                font-weight: 700;
                color: #1E293B;
              }
            }

            .item-hint {
              font-size: 11px;
              color: #94A3B8;
            }
          }
        }

        .item-right-col {
          display: flex;
          align-items: center;
          gap: 14px;

          .item-count-badge {
            font-size: 13px;
            font-weight: 700;
            color: #475569;

            &--urgent {
              color: #DC2626;
            }
          }

          .action-btn {
            border-radius: 8px;
          }
        }
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 48px 20px;
        text-align: center;

        .empty-icon {
          font-size: 48px;
          color: #10B981;
          margin-bottom: 12px;
        }

        .empty-title {
          font-size: 16px;
          font-weight: 700;
          color: #1E293B;
          margin: 0 0 6px 0;
        }

        .empty-desc {
          font-size: 13px;
          color: #64748B;
          max-width: 480px;
          margin: 0;
        }
      }
    }
  }
}
</style>
