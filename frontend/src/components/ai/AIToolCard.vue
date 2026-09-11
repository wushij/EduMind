<template>
  <div class="ai-tool-card" @click="emit('open-detail', tool)">
    <!-- 卡片头部：图标、名称与分类徽章 -->
    <div class="tool-card-header">
      <!-- 矢量彩色图标盒 (零 Emoji) -->
      <ColorIcon
        :name="toolIconName"
        :theme="toolIconTheme"
        :custom-bg="tool.iconBg"
        size="lg"
        rounded="xl"
      />

      <div class="tool-name-col">
        <div class="title-badge-row">
          <h3 class="tool-title">{{ tool.name }}</h3>
          <span v-if="tool.isRecommended" class="pill-badge pill-badge--hot">推荐</span>
        </div>
        <span class="pill-badge pill-badge--cat" :class="categoryClass">
          {{ tool.categoryLabel }}
        </span>
      </div>

      <!-- 收藏按钮 (矢量星标，零 Emoji) -->
      <button
        type="button"
        class="favorite-btn"
        :class="{ active: tool.isFavorite }"
        :title="tool.isFavorite ? '取消收藏' : '加入常用'"
        @click.stop="toggleFavorite"
      >
        <el-icon class="star-icon">
          <StarFilled v-if="tool.isFavorite" />
          <Star v-else />
        </el-icon>
      </button>
    </div>

    <!-- 描述 -->
    <p class="tool-desc">{{ tool.description }}</p>

    <!-- 标签 -->
    <div class="tool-tags-row">
      <span v-for="tag in tool.tags" :key="tag" class="pill-tag">
        # {{ tag }}
      </span>
    </div>

    <!-- 卡片底部：使用统计与立即使用按钮 (对齐原型图 1 中图) -->
    <div class="tool-card-footer">
      <div class="stats-meta">
        <span class="rating">
          <el-icon class="rate-star"><StarFilled /></el-icon>
          {{ tool.rating }}
        </span>
        <span class="dot">·</span>
        <span class="usage">{{ tool.usageCount }} 次调用</span>
      </div>

      <button
        type="button"
        class="capsule-use-btn"
        @click.stop="handleUseTool"
      >
        <span>立即使用</span>
        <el-icon class="arrow-icon"><Right /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Star, StarFilled, Right } from '@element-plus/icons-vue';
import ColorIcon, { type IconTheme } from '@/components/common/ColorIcon.vue';
import type { AITool } from '@/types/ai/tool';

const props = defineProps<{
  tool: AITool;
}>();

const emit = defineEmits<{
  (e: 'open-detail', tool: AITool): void;
}>();

const router = useRouter();

const categoryClass = computed(() => {
  if (props.tool.category === 'TEACHER') return 'cat-teacher';
  if (props.tool.category === 'STUDENT') return 'cat-student';
  return 'cat-general';
});

const toolIconName = computed(() => {
  if (props.tool.category === 'TEACHER') return 'EditPen';
  if (props.tool.category === 'STUDENT') return 'Reading';
  return 'MagicStick';
});

const toolIconTheme = computed<IconTheme>(() => {
  if (props.tool.category === 'TEACHER') return 'blue';
  if (props.tool.category === 'STUDENT') return 'emerald';
  return 'purple';
});

function toggleFavorite() {
  props.tool.isFavorite = !props.tool.isFavorite;
}

function handleUseTool() {
  router.push(props.tool.route);
}
</script>

<style scoped lang="scss">
.ai-tool-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 22px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 28px rgba(22, 119, 255, 0.12);
    border-color: #BFDBFE;

    .capsule-use-btn {
      background: #1677FF;
      color: #FFFFFF;
      border-color: #1677FF;

      .arrow-icon {
        transform: translateX(3px);
      }
    }
  }

  .tool-card-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 14px;

    .tool-name-col {
      flex: 1;
      min-width: 0;

      .title-badge-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .tool-title {
          font-size: 15px;
          font-weight: 700;
          color: #1E293B;
          margin: 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .pill-badge--hot {
          background: #FEF2F2;
          color: #EF4444;
          border: 1px solid #FCA5A5;
          padding: 1px 7px;
          border-radius: 9999px;
          font-size: 10px;
          font-weight: 600;
          flex-shrink: 0;
        }
      }

      .pill-badge--cat {
        display: inline-block;
        font-size: 11px;
        padding: 1px 8px;
        border-radius: 9999px;
        font-weight: 500;

        &.cat-teacher {
          background: #EFF6FF;
          color: #1677FF;
        }

        &.cat-student {
          background: #F0FDF4;
          color: #10B981;
        }

        &.cat-general {
          background: #F8FAFC;
          color: #64748B;
        }
      }
    }

    .favorite-btn {
      background: transparent;
      border: none;
      outline: none;
      cursor: pointer;
      padding: 6px;
      border-radius: 50%;
      color: #94A3B8;
      transition: all 0.2s ease;

      .star-icon {
        font-size: 18px;
      }

      &:hover {
        background: #FEF2F2;
        color: #F59E0B;
      }

      &.active {
        color: #F59E0B;
      }
    }
  }

  .tool-desc {
    font-size: 13px;
    color: #64748B;
    line-height: 1.55;
    margin: 0 0 14px 0;
    min-height: 40px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .tool-tags-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 18px;

    .pill-tag {
      font-size: 11px;
      color: #64748B;
      background: #F1F5F9;
      padding: 2px 8px;
      border-radius: 6px;
    }
  }

  .tool-card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 14px;
    border-top: 1px solid #F1F5F9;

    .stats-meta {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: #64748B;

      .rating {
        display: flex;
        align-items: center;
        gap: 3px;
        font-weight: 600;
        color: #F59E0B;

        .rate-star {
          font-size: 13px;
        }
      }

      .dot {
        color: #CBD5E1;
      }

      .usage {
        color: #64748B;
      }
    }

    .capsule-use-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 6px 14px;
      border-radius: 9999px;
      background: #EFF6FF;
      color: #1677FF;
      border: 1px solid #BFDBFE;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.22s ease;

      .arrow-icon {
        font-size: 12px;
        transition: transform 0.2s ease;
      }
    }
  }
}
</style>
