<template>
  <div class="knowledge-base-card">
    <!-- 顶部状态指示条 -->
    <div class="card-header-bar">
      <span class="category-pill" :class="`category-pill--${item.category.toLowerCase()}`">
        {{ item.categoryLabel }}
      </span>

      <span class="vector-status-pill" :class="`vector-status-pill--${item.vectorStatus.toLowerCase()}`">
        <span class="status-dot"></span>
        <span>{{ item.vectorStatusLabel }}</span>
      </span>
    </div>

    <!-- 知识库标题与图标 -->
    <div class="kb-title-block">
      <div class="kb-icon-box" :style="{ background: item.colorGradient || defaultGradient }">
        <span>🧠</span>
      </div>
      <div class="kb-title-info">
        <h3 class="kb-name" :title="item.name">{{ item.name }}</h3>
        <span v-if="item.courseName" class="course-link-pill">
          <span>📖 {{ item.courseName }}</span>
        </span>
      </div>
    </div>

    <!-- 简介描述 -->
    <p class="kb-desc" :title="item.description">{{ item.description }}</p>

    <!-- 向量切片进度条 (解析中特有) -->
    <div v-if="item.vectorStatus === 'PARSING'" class="parsing-progress-row">
      <div class="progress-title-line">
        <span>语义切片与向量索引构建中</span>
        <span class="progress-percent">{{ item.vectorProgress }}%</span>
      </div>
      <div class="capsule-progress-track">
        <div class="capsule-progress-fill" :style="{ width: `${item.vectorProgress}%` }"></div>
      </div>
    </div>

    <!-- 统计指标药丸组合 -->
    <div class="kb-stats-row">
      <div class="stat-pill-chip">
        <span class="stat-icon">📄</span>
        <span class="stat-text"><strong>{{ item.documentCount }}</strong> 篇文档</span>
      </div>
      <div class="stat-pill-chip">
        <span class="stat-icon">🧩</span>
        <span class="stat-text"><strong>{{ item.chunkCount }}</strong> 个切片</span>
      </div>
      <div class="stat-pill-chip">
        <span class="stat-icon">⚡</span>
        <span class="stat-text">{{ item.embeddingModel.split(' ')[0] }}</span>
      </div>
    </div>

    <!-- 底部操作按钮坞 (1:1 胶囊标准) -->
    <div class="card-actions-row">
      <button
        type="button"
        class="capsule-btn capsule-btn--upload"
        @click="$emit('upload', item)"
      >
        <span>📤 上传文档</span>
      </button>

      <button
        type="button"
        class="capsule-btn capsule-btn--enter"
        @click="$emit('open', item)"
      >
        <span>管理与检索 →</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';

defineProps<{
  item: KnowledgeBase;
}>();

defineEmits<{
  (e: 'open', item: KnowledgeBase): void;
  (e: 'upload', item: KnowledgeBase): void;
  (e: 'delete', item: KnowledgeBase): void;
}>();

const defaultGradient = 'linear-gradient(135deg, #1677FF 0%, #722ED1 100%)';
</script>

<style scoped lang="scss">
.knowledge-base-card {
  background: #FFFFFF;
  border-radius: 18px;
  border: 1px solid #E2E8F0;
  padding: 22px;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  display: flex;
  flex-direction: column;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-3px);
    border-color: #93C5FD;
    box-shadow: 0 10px 24px rgba(22, 119, 255, 0.1);
  }

  // 1. 顶部指示条
  .card-header-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 16px;

    .category-pill {
      display: inline-flex;
      align-items: center;
      height: 24px;
      padding: 0 10px;
      border-radius: 9999px; // 长圆
      font-size: 11.5px;
      font-weight: 600;

      &--major {
        background: #EFF6FF;
        color: #1677FF;
        border: 1px solid #BFDBFE;
      }

      &--common {
        background: #F5F3FF;
        color: #7C3AED;
        border: 1px solid #DDD6FE;
      }

      &--exam {
        background: #ECFDF5;
        color: #059669;
        border: 1px solid #A7F3D0;
      }

      &--courseware {
        background: #FFFBEB;
        color: #D97706;
        border: 1px solid #FDE68A;
      }
    }

    .vector-status-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 24px;
      padding: 0 10px;
      border-radius: 9999px; // 长圆
      font-size: 11.5px;
      font-weight: 600;

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      &--synced {
        background: #ECFDF5;
        color: #059669;
        border: 1px solid #A7F3D0;
        .status-dot {
          background: #10B981;
          box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
        }
      }

      &--parsing {
        background: #FFFBEB;
        color: #D97706;
        border: 1px solid #FDE68A;
        .status-dot {
          background: #F59E0B;
          box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.25);
          animation: pulse 1.5s infinite;
        }
      }

      &--pending {
        background: #F1F5F9;
        color: #64748B;
        border: 1px solid #CBD5E1;
        .status-dot {
          background: #94A3B8;
        }
      }
    }
  }

  // 2. 标题区
  .kb-title-block {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 12px;

    .kb-icon-box {
      width: 44px;
      height: 44px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: #FFFFFF;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .kb-title-info {
      flex: 1;
      min-width: 0;

      .kb-name {
        margin: 0 0 4px 0;
        font-size: 15.5px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.4;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .course-link-pill {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11.5px;
        color: #64748B;
        background: #F8FAFC;
        padding: 1px 8px;
        border-radius: 9999px;
      }
    }
  }

  // 3. 描述
  .kb-desc {
    margin: 0 0 14px 0;
    font-size: 12.5px;
    color: #64748B;
    line-height: 1.6;
    height: 40px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  // 4. 解析进度条
  .parsing-progress-row {
    margin-bottom: 14px;

    .progress-title-line {
      display: flex;
      justify-content: space-between;
      font-size: 11px;
      color: #D97706;
      font-weight: 500;
      margin-bottom: 4px;
    }

    .capsule-progress-track {
      width: 100%;
      height: 5px;
      background: #FEF3C7;
      border-radius: 9999px;
      overflow: hidden;

      .capsule-progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #F59E0B 0%, #D97706 100%);
        border-radius: 9999px;
        transition: width 0.3s;
      }
    }
  }

  // 5. 统计参数药丸
  .kb-stats-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 18px;

    .stat-pill-chip {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      height: 24px;
      padding: 0 10px;
      border-radius: 9999px; // 长圆药丸
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      font-size: 11.5px;
      color: #475569;

      strong {
        color: #1E293B;
      }
    }
  }

  // 6. 底部操作栏
  .card-actions-row {
    margin-top: auto;
    display: flex;
    align-items: center;
    gap: 10px;
    padding-top: 14px;
    border-top: 1px solid #F1F5F9;

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 36px;
      border-radius: 9999px; // 纯正长圆跑道
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;

      &--upload {
        padding: 0 16px;
        background: #F1F5F9;
        border: 1px solid #CBD5E1;
        color: #334155;

        &:hover {
          background: #EFF6FF;
          color: #1677FF;
          border-color: #93C5FD;
        }
      }

      &--enter {
        flex: 1;
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

        &:hover {
          background: #4096FF;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.35);
          transform: translateY(-1px);
        }

        &:active {
          background: #0958D9;
        }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.3); }
}
</style>
