<template>
  <article class="tool-admin-card" :class="{ 'is-offline': tool.status !== 1 }">
    <div class="card-body">
      <!-- 顶部标签与状态呼吸徽标行 -->
      <div class="card-top-bar">
        <div class="top-tags-left">
          <span class="badge badge--cat" :class="`cat-${tool.category?.toLowerCase()}`">
            {{ categoryLabel }}
          </span>
          <span v-if="tool.isHot" class="badge badge--hot">
            <el-icon class="badge-icon"><Aim /></el-icon>
            <span>热门</span>
          </span>
          <span v-if="tool.isRecommended" class="badge badge--rec">
            <el-icon class="badge-icon"><Star /></el-icon>
            <span>推荐</span>
          </span>
        </div>
        <div class="status-glow-pill" :class="tool.status === 1 ? 'online' : 'offline'">
          <span class="status-dot"></span>
          <span>{{ tool.status === 1 ? '在线运行' : '已下架' }}</span>
        </div>
      </div>

      <!-- 卡片主体：高定彩色图标 + 标题 + 描述 -->
      <div class="card-main-row">
        <div class="icon-wrap">
          <ColorIcon
            :name="preview.iconName || 'MagicStick'"
            :theme="iconTheme"
            :custom-bg="preview.iconBg"
            size="lg"
            rounded="xl"
          />
        </div>
        <div class="title-col">
          <h3 class="tool-name" :title="tool.name">{{ tool.name }}</h3>
          <p class="tool-desc" :title="tool.description">{{ tool.description || '暂无详细功能描述' }}</p>
        </div>
      </div>

      <!-- 核心元数据网格：路由、大模型、调用统计、排序 -->
      <div class="meta-grid">
        <div class="meta-item">
          <span class="meta-label">前端路由</span>
          <span class="meta-value route-val" :title="tool.route">{{ tool.route || '-' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">绑定模型</span>
          <span class="meta-value model-val" :title="tool.modelId">{{ tool.modelId || '未绑定' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">累计调用</span>
          <span class="meta-value call-val">{{ tool.useCount ?? 0 }} <span class="unit">次</span></span>
        </div>
        <div class="meta-item">
          <span class="meta-label">排序权重</span>
          <span class="meta-value sort-val">{{ tool.sortOrder ?? 0 }}</span>
        </div>
      </div>

      <!-- 标签展示行 -->
      <div v-if="tagList.length" class="tag-row">
        <span v-for="tag in tagList" :key="tag" class="tag-chip"># {{ tag }}</span>
      </div>
    </div>

    <!-- 底部高定操作按钮组 -->
    <div class="card-actions">
      <div class="actions-left">
        <el-button round size="small" class="pill-act-btn edit-btn" @click="emit('edit', tool)">
          <el-icon><EditPen /></el-icon>
          <span>编辑</span>
        </el-button>
        <el-button
          v-if="tool.status === 1"
          round
          size="small"
          class="pill-act-btn offline-btn"
          @click="emit('offline', tool)"
        >
          下架
        </el-button>
        <el-button
          v-else
          round
          size="small"
          class="pill-act-btn publish-btn"
          @click="emit('publish', tool)"
        >
          上架
        </el-button>
      </div>

      <div class="actions-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <button class="more-circle-btn" title="更多操作">
            <el-icon><MoreFilled /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="pill-dropdown-menu">
              <el-dropdown-item command="preview">
                <el-icon><Shop /></el-icon> 广场预览
              </el-dropdown-item>
              <el-dropdown-item command="toggleRec">
                <el-icon><Star /></el-icon>
                {{ tool.isRecommended ? '取消推荐' : '设为推荐' }}
              </el-dropdown-item>
              <el-dropdown-item command="toggleHot">
                <el-icon><Aim /></el-icon>
                {{ tool.isHot ? '取消热门' : '设为热门' }}
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided style="color: #ef4444;">
                <el-icon><Delete /></el-icon> 删除工具
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  EditPen, MoreFilled, Shop, Star, Aim, Delete
} from '@element-plus/icons-vue';
import ColorIcon from '@/components/common/ColorIcon.vue';
import type { AIToolAdminVO } from '@/types/system/tool';
import type { IconTheme } from '@/types/common/icon';
import { mapTool } from '@/utils/ai/map-tool';

const props = defineProps<{
  tool: AIToolAdminVO;
}>();

const emit = defineEmits<{
  (e: 'edit', tool: AIToolAdminVO): void;
  (e: 'publish', tool: AIToolAdminVO): void;
  (e: 'offline', tool: AIToolAdminVO): void;
  (e: 'delete', tool: AIToolAdminVO): void;
  (e: 'preview', tool: AIToolAdminVO): void;
  (e: 'toggle-rec', tool: AIToolAdminVO): void;
  (e: 'toggle-hot', tool: AIToolAdminVO): void;
}>();

const preview = computed(() => mapTool(props.tool));

const ICON_THEMES: IconTheme[] = ['blue', 'cyan', 'emerald', 'amber', 'rose', 'purple', 'indigo', 'teal'];

const iconTheme = computed((): IconTheme => {
  const theme = preview.value.iconTheme;
  return ICON_THEMES.includes(theme as IconTheme) ? (theme as IconTheme) : 'blue';
});

const categoryLabel = computed(() => {
  const map: Record<string, string> = {
    TEACHER: '教师工具',
    STUDENT: '学生工具',
    GENERAL: '通用工具'
  };
  return map[props.tool.category] || props.tool.category;
});

const tagList = computed(() => {
  if (!props.tool.tags) return [];
  return props.tool.tags.split(/[,，;；\s]+/).map(t => t.trim()).filter(Boolean);
});

function handleCommand(command: string) {
  switch (command) {
    case 'preview':
      emit('preview', props.tool);
      break;
    case 'toggleRec':
      emit('toggle-rec', props.tool);
      break;
    case 'toggleHot':
      emit('toggle-hot', props.tool);
      break;
    case 'delete':
      emit('delete', props.tool);
      break;
  }
}
</script>

<style scoped lang="scss">
.tool-admin-card {
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    box-shadow: 0 12px 32px rgba(22, 119, 255, 0.1);
    border-color: rgba(147, 197, 253, 0.75);
    transform: translateY(-3px);
  }

  &.is-offline {
    background: #fafbfc;
    opacity: 0.9;
    border-style: dashed;
  }
}

.card-body {
  padding: 18px 20px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.top-tags-left {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.status-glow-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.02em;

  &.online {
    background: #ecfdf5;
    color: #059669;
    border: 1px solid rgba(16, 185, 129, 0.25);

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #10b981;
      box-shadow: 0 0 6px #10b981;
      animation: pulse-dot 2s infinite ease-in-out;
    }
  }

  &.offline {
    background: #f1f5f9;
    color: #64748b;
    border: 1px solid #e2e8f0;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #94a3b8;
    }
  }
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.35; transform: scale(0.8); }
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;

  .badge-icon {
    font-size: 12px;
  }

  &--hot {
    background: #fffbeb;
    color: #d97706;
    border: 1px solid #fef3c7;
  }

  &--rec {
    background: #f5f3ff;
    color: #7c3aed;
    border: 1px solid #ede9fe;
  }

  &--cat {
    background: #eff6ff;
    color: #2563eb;
    border: 1px solid #dbeafe;

    &.cat-student {
      background: #ecfdf5;
      color: #059669;
      border-color: #d1fae5;
    }

    &.cat-general {
      background: #f8fafc;
      color: #475569;
      border-color: #e2e8f0;
    }
  }
}

.card-main-row {
  display: flex;
  gap: 14px;
  margin-bottom: 12px;

  .icon-wrap {
    flex-shrink: 0;
  }
}

.title-col {
  flex: 1;
  min-width: 0;
}

.tool-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.01em;
}

.tool-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin: 12px 0 10px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 6px 10px;
  transition: all 0.2s;

  &:hover {
    background: #f1f5f9;
    border-color: #e2e8f0;
  }
}

.meta-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

.meta-value {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &.route-val {
    font-family: 'Fira Code', monospace;
    font-size: 11.5px;
    color: #2563eb;
  }

  &.call-val {
    color: #7c3aed;
  }

  .unit {
    font-size: 11px;
    font-weight: 400;
    color: #94a3b8;
  }
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}

.tag-chip {
  font-size: 11px;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 1px 7px;
  font-weight: 500;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;

  .actions-left {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .pill-act-btn {
    border-radius: 9999px !important;
    font-size: 12px !important;
    font-weight: 500 !important;
    padding: 0 12px !important;
    height: 28px !important;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: all 0.2s ease;

    &.edit-btn {
      background: #f8fafc !important;
      border: 1px solid #e2e8f0 !important;
      color: #334155 !important;

      &:hover {
        background: #eff6ff !important;
        color: #2563eb !important;
        border-color: #bfdbfe !important;
      }
    }

    &.offline-btn {
      background: #fffbeb !important;
      border: 1px solid #fde68a !important;
      color: #d97706 !important;

      &:hover {
        background: #fef3c7 !important;
        border-color: #fcd34d !important;
      }
    }

    &.publish-btn {
      background: #ecfdf5 !important;
      border: 1px solid #a7f3d0 !important;
      color: #059669 !important;

      &:hover {
        background: #d1fae5 !important;
        border-color: #6ee7b7 !important;
      }
    }
  }

  .more-circle-btn {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    color: #64748b;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    &:hover {
      background: #eff6ff;
      color: #2563eb;
      border-color: #bfdbfe;
      transform: scale(1.05);
    }
  }
}
</style>
