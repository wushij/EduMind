<template>
  <div class="recommendation-card" :class="{ 'is-completed': item.completed }">
    <!-- 顶部状态栏：AI 匹配度胶囊与分类标签 -->
    <div class="card-header-row">
      <div class="header-left-badges">
        <span class="match-score-pill">
          <el-icon class="sparkle-icon"><Lightning /></el-icon>
          <span>{{ item.matchScore }}% AI 智能匹配</span>
        </span>
        <span class="category-pill" :class="`category-pill--${categoryVariant}`">
          {{ item.category }}
        </span>
      </div>

      <span class="type-badge-pill">
        <el-icon class="type-icon"><component :is="item.type === 'exercise' ? EditPen : Folder" /></el-icon>
        <span>{{ item.typeLabel }}</span>
      </span>
    </div>

    <!-- 标题与描述 -->
    <h3 class="card-title">{{ item.title }}</h3>
    <p class="card-description">{{ item.description }}</p>

    <!-- 课程与考点信息条 (药丸组合) -->
    <div class="card-course-meta">
      <span class="pill-info-tag pill-info-tag--course">
        <el-icon class="tag-icon"><Reading /></el-icon>
        <span>{{ item.courseName }}</span>
      </span>
      <span class="pill-info-tag pill-info-tag--kp">
        <el-icon class="tag-icon"><Opportunity /></el-icon>
        <span>{{ item.knowledgePoint }}</span>
      </span>
    </div>

    <!-- 难度与预计耗时 -->
    <div class="card-params-row">
      <div class="param-item">
        <span class="param-label">难度等级：</span>
        <span class="difficulty-capsule" :class="`difficulty-capsule--${item.difficulty.toLowerCase()}`">
          {{ item.difficultyLabel }}
        </span>
      </div>

      <div class="param-item">
        <span class="param-label">预估耗时：</span>
        <span class="time-capsule">
          <el-icon class="param-icon"><Timer /></el-icon>
          <span>{{ item.estimatedMinutes }} 分钟</span>
        </span>
      </div>

      <div v-if="item.exerciseMeta" class="param-item">
        <span class="param-label">全班均正答率：</span>
        <span class="stat-text">{{ item.exerciseMeta.averageAccuracy }}</span>
      </div>
      <div v-else-if="item.resourceMeta" class="param-item">
        <span class="param-label">格式大小：</span>
        <span class="stat-text">{{ item.resourceMeta.format }} · {{ item.resourceMeta.fileSize }}</span>
      </div>
    </div>

    <!-- 标签胶囊列表 -->
    <div class="card-tags-row">
      <span v-for="tag in item.tags" :key="tag" class="pill-tag-mini">
        # {{ tag }}
      </span>
    </div>

    <!-- 底部胶囊操作栏 (1:1 胶囊规范) -->
    <div class="card-actions-bar">
      <button
        type="button"
        class="capsule-card-btn capsule-card-btn--secondary"
        @click="$emit('discuss', item)"
      >
        <el-icon class="btn-icon"><ChatDotRound /></el-icon>
        <span>助教答疑</span>
      </button>

      <button
        type="button"
        class="capsule-card-btn capsule-card-btn--primary"
        @click="$emit('start', item)"
      >
        <span>{{ item.type === 'exercise' ? '立即开始巩固练习' : '在线研读微课资料' }}</span>
        <el-icon class="btn-arrow-icon"><Right /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Lightning,
  EditPen,
  Folder,
  Reading,
  Opportunity,
  Timer,
  ChatDotRound,
  Right
} from '@element-plus/icons-vue';
import type { RecommendationItem } from '@/types/learning/recommendation';

const props = defineProps<{
  item: RecommendationItem;
}>();

defineEmits<{
  (e: 'start', item: RecommendationItem): void;
  (e: 'discuss', item: RecommendationItem): void;
}>();

const categoryVariant = computed(() => {
  switch (props.item.category) {
    case '薄弱巩固':
      return 'weak';
    case '核心必刷':
      return 'core';
    case '精选课件':
      return 'courseware';
    case '拓展进阶':
      return 'advance';
    default:
      return 'default';
  }
});
</script>

<style scoped lang="scss">
.recommendation-card {
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

  // 1. 顶部状态栏
  .card-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 14px;

    .header-left-badges {
      display: flex;
      align-items: center;
      gap: 8px;

      .match-score-pill {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 24px;
        padding: 0 10px;
        border-radius: 9999px; // 长圆药丸
        background: #ECFDF5;
        border: 1px solid #A7F3D0;
        color: #059669;
        font-size: 11.5px;
        font-weight: 700;

        .sparkle-icon {
          font-size: 12px;
        }
      }

      .category-pill {
        display: inline-flex;
        align-items: center;
        height: 24px;
        padding: 0 10px;
        border-radius: 9999px; // 长圆药丸
        font-size: 11.5px;
        font-weight: 600;

        &--weak {
          background: #FEF2F2;
          color: #DC2626;
          border: 1px solid #FECACA;
        }

        &--core {
          background: #EFF6FF;
          color: #1677FF;
          border: 1px solid #BFDBFE;
        }

        &--courseware {
          background: #FAF5FF;
          color: #7E22CE;
          border: 1px solid #E9D5FF;
        }

        &--advance {
          background: #FFFBEB;
          color: #D97706;
          border: 1px solid #FDE68A;
        }
      }
    }

    .type-badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #64748B;
      font-weight: 500;
    }
  }

  // 2. 标题与说明
  .card-title {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 700;
    color: #0F172A;
    line-height: 1.4;
  }

  .card-description {
    margin: 0 0 14px 0;
    font-size: 13px;
    color: #64748B;
    line-height: 1.6;
    height: 42px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  // 3. 课程与考点
  .card-course-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 14px;

    .pill-info-tag {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      height: 24px;
      padding: 0 10px;
      border-radius: 9999px; // 药丸
      font-size: 11.5px;
      font-weight: 500;

      .tag-icon {
        font-size: 13px;
      }

      &--course {
        background: #F1F5F9;
        color: #334155;
      }

      &--kp {
        background: #F0FDF4;
        color: #15803D;
        border: 1px solid #BBF7D0;
      }
    }
  }

  // 4. 难度与参数
  .card-params-row {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 12px;
    color: #64748B;
    margin-bottom: 14px;
    flex-wrap: wrap;

    .param-item {
      display: inline-flex;
      align-items: center;
      gap: 4px;

      .param-label {
        color: #94A3B8;
      }

      .difficulty-capsule {
        padding: 1px 8px;
        border-radius: 9999px;
        font-weight: 600;
        font-size: 11px;

        &--easy {
          background: #ECFDF5;
          color: #059669;
        }

        &--medium {
          background: #EFF6FF;
          color: #1677FF;
        }

        &--hard {
          background: #FEF2F2;
          color: #DC2626;
        }
      }

      .time-capsule {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        color: #334155;
        font-weight: 500;

        .param-icon {
          font-size: 13px;
          color: #94A3B8;
        }
      }

      .stat-text {
        color: #1E293B;
        font-weight: 600;
      }
    }
  }

  // 5. 标签条
  .card-tags-row {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    margin-bottom: 18px;

    .pill-tag-mini {
      font-size: 11px;
      color: #64748B;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      padding: 2px 8px;
      border-radius: 9999px;
    }
  }

  // 6. 底部胶囊操作栏
  .card-actions-bar {
    margin-top: auto;
    display: flex;
    align-items: center;
    gap: 10px;
    padding-top: 14px;
    border-top: 1px solid #F1F5F9;

    .capsule-card-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px; // 纯正长圆跑道
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;

      .btn-icon {
        font-size: 14px;
      }

      .btn-arrow-icon {
        font-size: 13px;
      }

      &--secondary {
        background: #F1F5F9;
        color: #475569;
        border: 1px solid #CBD5E1;

        &:hover {
          background: #EFF6FF;
          color: #1677FF;
          border-color: #93C5FD;
        }
      }

      &--primary {
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
</style>
