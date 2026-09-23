<template>
  <div class="dashboard-recent-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">动态与通知中心</h2>
        <span class="page-subtitle">实时追踪教学动态、AI 助教答疑反馈、系统版本与教务公告</span>
      </div>
      <div class="header-actions">
        <el-button :icon="Check" @click="handleReadAll">
          全部标为已读
        </el-button>
        <el-button type="primary" :icon="Refresh" @click="fetchList" :loading="loading">
          刷新
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="recent-list-card">
      <template #header>
        <div class="card-header-row">
          <el-radio-group v-model="activeCategory" size="small" @change="changeCategory(activeCategory)">
            <el-radio-button label="all">全部 ({{ total }})</el-radio-button>
            <el-radio-button label="system">系统公告</el-radio-button>
            <el-radio-button label="teaching">教学教务</el-radio-button>
            <el-radio-button label="ai">AI 动态</el-radio-button>
          </el-radio-group>
          <span class="header-count-tip">共 {{ total }} 条动态记录</span>
        </div>
      </template>

      <div v-loading="loading" class="notifications-list">
        <template v-if="notifications.length > 0">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-card-item"
            :class="{ 'notification-card-item--unread': !item.read }"
            @click="handleItemClick(item)"
          >
            <div class="notif-left">
              <div class="badge-icon-box" :class="item.typeClass">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="notif-content-box">
                <div class="notif-title-row">
                  <span class="notif-title">{{ item.title }}</span>
                  <el-tag
                    v-if="!item.read"
                    size="small"
                    type="danger"
                    effect="dark"
                    class="unread-tag"
                  >
                    NEW
                  </el-tag>
                  <el-tag size="small" type="info" effect="plain" class="cat-tag">
                    {{ item.typeLabel }}
                  </el-tag>
                </div>
                <p class="notif-desc">{{ item.content || '点击查看详情及相关处理链路' }}</p>
                <div class="notif-footer">
                  <span class="notif-time">{{ item.time || '刚刚' }}</span>
                </div>
              </div>
            </div>

            <div class="notif-right">
              <el-button text size="small" class="detail-btn">
                查看详情 &gt;
              </el-button>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="pageSize"
              layout="prev, pager, next"
              :total="total"
              @current-change="onPageChange"
            />
          </div>
        </template>

        <div v-else class="empty-state">
          <el-icon class="empty-icon"><Bell /></el-icon>
          <p class="empty-title">暂无相关动态与通知</p>
          <p class="empty-desc">{{ emptyText }}</p>
        </div>
      </div>
    </el-card>

    <!-- 动态详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentDetail?.title || '动态详情'"
      width="540px"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-dialog-body">
        <div class="detail-meta-row">
          <el-tag size="small" type="primary">{{ currentDetail.typeLabel }}</el-tag>
          <span class="detail-time">{{ currentDetail.time }}</span>
        </div>
        <div class="detail-content-text">
          {{ currentDetail.content || '无具体正文内容' }}
        </div>
      </div>
      <template #footer>
        <el-button @click="closeDetail">关闭</el-button>
        <el-button
          v-if="currentDetail?.navigatePath"
          type="primary"
          @click="handleNavigate"
        >
          前往相关模块
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Bell, Refresh, Check } from '@element-plus/icons-vue';
import { useNotificationList } from '@/composables/notification/useNotificationList';

const router = useRouter();

const {
  loading,
  activeCategory,
  notifications,
  page,
  pageSize,
  total,
  emptyText,
  detailVisible,
  currentDetail,
  fetchList,
  changeCategory,
  onPageChange,
  handleItemClick,
  closeDetail,
  handleReadAll
} = useNotificationList({ pageSize: 10, enablePagination: true });

function handleNavigate() {
  const path = currentDetail.value?.navigatePath;
  closeDetail();
  if (path) {
    router.push(path);
  }
}
</script>

<style scoped lang="scss">
.dashboard-recent-page {
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

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .recent-list-card {
    border-radius: 16px;
    border: 1px solid #E2E8F0;

    .card-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .header-count-tip {
        font-size: 12px;
        color: #94A3B8;
      }
    }

    .notifications-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .notification-card-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px 18px;
        border-radius: 12px;
        background: #F8FAFC;
        border: 1px solid #EDF2F7;
        cursor: pointer;
        transition: all 0.25s ease;

        &:hover {
          background: #EFF6FF;
          border-color: #BFDBFE;
          transform: translateY(-1px);
        }

        &--unread {
          background: #FFFFFF;
          border-color: #CBD5E1;
          box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);

          .notif-title {
            color: #0F172A;
            font-weight: 700;
          }
        }

        .notif-left {
          display: flex;
          align-items: flex-start;
          gap: 14px;
          flex: 1;
          min-width: 0;

          .badge-icon-box {
            width: 38px;
            height: 38px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            flex-shrink: 0;
            background: #EFF6FF;
            color: #2563EB;
          }

          .notif-content-box {
            display: flex;
            flex-direction: column;
            gap: 4px;
            flex: 1;
            min-width: 0;

            .notif-title-row {
              display: flex;
              align-items: center;
              gap: 8px;

              .notif-title {
                font-size: 14px;
                font-weight: 600;
                color: #334155;
              }

              .unread-tag {
                font-size: 10px;
                padding: 0 4px;
                height: 18px;
                line-height: 16px;
              }

              .cat-tag {
                font-size: 11px;
                padding: 0 6px;
                height: 20px;
                line-height: 18px;
              }
            }

            .notif-desc {
              font-size: 13px;
              color: #64748B;
              margin: 0;
              line-height: 1.5;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
              overflow: hidden;
            }

            .notif-footer {
              display: flex;
              align-items: center;
              gap: 12px;
              margin-top: 4px;

              .notif-time {
                font-size: 11px;
                color: #94A3B8;
              }
            }
          }
        }

        .notif-right {
          flex-shrink: 0;
          margin-left: 16px;

          .detail-btn {
            color: #2563EB;
          }
        }
      }

      .pagination-wrapper {
        display: flex;
        justify-content: flex-end;
        padding-top: 12px;
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 56px 20px;
        text-align: center;

        .empty-icon {
          font-size: 48px;
          color: #94A3B8;
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

  .detail-dialog-body {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .detail-meta-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-bottom: 10px;
      border-bottom: 1px solid #F1F5F9;

      .detail-time {
        font-size: 12px;
        color: #94A3B8;
      }
    }

    .detail-content-text {
      font-size: 14px;
      line-height: 1.7;
      color: #334155;
      white-space: pre-wrap;
    }
  }
}
</style>
