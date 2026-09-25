<template>
  <div class="kp-card-item" @click="$emit('open-detail')">
    <div class="card-head">
      <div class="badge-row">
        <span v-if="kp.code" class="kp-code font-mono">{{ kp.code }}</span>
        <span v-else class="kp-code kp-code--muted">未编码</span>
        <el-tag size="small" :type="levelTagType">
          {{ levelLabel }}
        </el-tag>
      </div>
      <div v-if="kp.importance" class="star-rating">
        <span v-for="s in kp.importance" :key="s" class="star">
          <el-icon><StarFilled /></el-icon>
        </span>
      </div>
    </div>

    <h3 class="kp-name">{{ kp.title || kp.name }}</h3>
    <p v-if="kp.description" class="kp-desc">{{ kp.description }}</p>
    <p v-else class="kp-desc kp-desc--empty">暂无考点说明，点击卡片查看或补充</p>

    <p v-if="kp.examFocus" class="kp-focus">
      <el-icon><Warning /></el-icon>
      <span>{{ kp.examFocus }}</span>
    </p>

    <div v-if="kp.prerequisites?.length" class="prereq-row">
      <span class="prereq-label">前置：</span>
      <div class="prereq-tags">
        <span v-for="pre in kp.prerequisites" :key="pre.id" class="pre-tag">{{ pre.title }}</span>
      </div>
    </div>

    <div class="card-chapter-row">
      <el-icon class="chapter-icon"><Reading /></el-icon>
      <span class="chapter-label">所属章节：</span>
      <span class="chapter-title" :title="chapterTitle">{{ chapterTitle }}</span>
    </div>

    <div class="card-actions-bar" @click.stop>
      <div class="actions-left">
        <el-button size="small" class="card-action-btn card-action-btn--graph" @click="$emit('open-graph')">
          <el-icon><Connection /></el-icon>
          <span>关联图谱</span>
        </el-button>
        <el-button size="small" class="card-action-btn card-action-btn--ai" @click="$emit('ask-ai')">
          <el-icon><AiSparkleIcon /></el-icon>
          <span>AI解析</span>
        </el-button>
      </div>
      <div v-if="editable" class="actions-right">
        <el-button size="small" class="card-action-btn card-action-btn--edit" @click="$emit('edit')">
          <el-icon><EditPen /></el-icon>
          <span>编辑</span>
        </el-button>
        <el-button size="small" class="card-action-btn card-action-btn--danger" @click="$emit('delete')">
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { StarFilled, Connection, Delete, Reading, EditPen, Warning } from '@element-plus/icons-vue';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const props = defineProps<{
  kp: KnowledgePoint;
  chapterTitle: string;
  levelLabel: string;
  levelTagType: string;
  editable?: boolean;
}>();

defineEmits<{
  'open-detail': [];
  'open-graph': [];
  'ask-ai': [];
  edit: [];
  delete: [];
}>();

const levelLabel = computed(() => props.levelLabel);
const levelTagType = computed(() => props.levelTagType as any);
</script>

<style scoped lang="scss">
.kp-card-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 16px 14px;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #93c5fd;
    box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
  }

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .badge-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .kp-code {
        font-size: 11px;
        color: #64748b;
        background: #f1f5f9;
        padding: 2px 6px;
        border-radius: 4px;

        &--muted {
          color: #94a3b8;
          font-style: italic;
        }
      }
    }

    .star-rating {
      color: #f59e0b;
      font-size: 14px;
    }
  }

  .kp-name {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 8px;
    line-height: 1.4;
  }

  .kp-desc {
    font-size: 13px;
    color: #64748b;
    line-height: 1.6;
    margin: 0 0 10px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;

    &--empty {
      color: #94a3b8;
      font-style: italic;
      -webkit-line-clamp: 2;
    }
  }

  .kp-focus {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    font-size: 12px;
    color: #b45309;
    background: #fffbeb;
    border: 1px solid #fde68a;
    border-radius: 8px;
    padding: 6px 10px;
    margin: 0 0 10px;

    .el-icon {
      margin-top: 2px;
      flex-shrink: 0;
    }
  }

  .prereq-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 10px;
    font-size: 12px;

    .prereq-label {
      color: #94a3b8;
      flex-shrink: 0;
    }

    .prereq-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      .pre-tag {
        background: #eff6ff;
        color: #2563eb;
        padding: 1px 6px;
        border-radius: 4px;
      }
    }
  }

  .card-chapter-row {
    display: flex;
    align-items: center;
    gap: 6px;
    padding-top: 12px;
    margin-top: auto;
    border-top: 1px solid #f1f5f9;
    min-width: 0;

    .chapter-icon {
      font-size: 14px;
      color: #64748b;
    }

    .chapter-label {
      font-size: 12px;
      color: #94a3b8;
    }

    .chapter-title {
      font-size: 12px;
      color: #475569;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      min-width: 0;
    }
  }

  .card-actions-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 4px;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed #f1f5f9;
    flex-wrap: nowrap;
    width: 100%;
    box-sizing: border-box;

    .actions-left,
    .actions-right {
      display: flex;
      align-items: center;
      gap: 5px;
      flex-wrap: nowrap;
      flex-shrink: 0;
    }

    .card-action-btn {
      height: 26px;
      padding: 0 7px;
      border-radius: 6px;
      font-size: 11.5px;
      font-weight: 500;
      white-space: nowrap;
      margin: 0 !important;
      display: inline-flex;
      align-items: center;
      gap: 2px;
      line-height: 1;

      :deep(.el-icon) {
        font-size: 12.5px;
        margin-right: 1px;
      }

      &--graph {
        background: #eff6ff;
        border-color: #bfdbfe;
        color: #1d4ed8;

        &:hover {
          background: #dbeafe;
          border-color: #93c5fd;
        }
      }

      &--ai {
        background: #faf5ff;
        border-color: #e9d5ff;
        color: #7c3aed;

        &:hover {
          background: #f3e8ff;
          border-color: #d8b4fe;
        }
      }

      &--edit {
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #475569;

        &:hover {
          background: #f1f5f9;
          border-color: #cbd5e1;
          color: #1e293b;
        }
      }

      &--danger {
        background: #fff1f2;
        border-color: #fecdd3;
        color: #e11d48;

        &:hover {
          background: #ffe4e6;
          border-color: #fda4af;
        }
      }
    }
  }
}
</style>
