<template>
  <div v-loading="loading" class="prompt-grid-container">
    <div v-if="filteredPrompts.length > 0" class="prompt-grid">
      <div
        v-for="item in filteredPrompts"
        :key="item.id"
        class="prompt-card"
        :class="[`theme-${item.category}`]"
      >
        <!-- 顶部彩色重点装饰条 -->
        <div class="card-accent-bar"></div>

        <!-- 卡片顶栏：分类、运行状态与版本 -->
        <div class="card-top-header">
          <div class="category-box">
            <span class="category-tag" :class="item.category">
              {{ getCategoryLabel(item.category) }}
            </span>
          </div>

          <div class="status-and-version">
            <span
              class="status-pill"
              :class="item.status === 'PUBLISHED' ? 'status-online' : 'status-draft'"
            >
              <span class="status-dot"></span>
              {{ item.status === 'PUBLISHED' ? '运行中' : '草稿' }}
            </span>
            <span class="version-pill">{{ item.version }}</span>
          </div>
        </div>

        <!-- 模板名称与简介 -->
        <div class="card-body">
          <h3 class="prompt-name" :title="item.name">{{ item.name }}</h3>
          <p class="prompt-desc" :title="item.description">
            {{ item.description || '暂未填写模板描述，包含标准系统人设与用户问答指令模板。' }}
          </p>

          <!-- 核心工程特性标签 (全矢量 Icon，摒弃粗糙 Emoji) -->
          <div class="feature-chips">
            <span v-if="item.category === 'rag'" class="feat-chip rag-feat">
              <el-icon class="feat-ic"><Search /></el-icon>
              知识库检索
            </span>
            <span v-if="item.category === 'rag'" class="feat-chip rag-feat">
              <el-icon class="feat-ic"><Link /></el-icon>
              引用溯源 S1/S2
            </span>
            <span v-if="item.category === 'rag'" class="feat-chip shield-feat">
              <el-icon class="feat-ic"><Lock /></el-icon>
              防注入防护
            </span>
            <span v-if="item.category === 'question'" class="feat-chip exam-feat">
              <el-icon class="feat-ic"><Document /></el-icon>
              JSON 结构化
            </span>
            <span v-if="item.category === 'grading'" class="feat-chip grade-feat">
              <el-icon class="feat-ic"><Finished /></el-icon>
              采分点核算
            </span>
            <span v-if="item.category === 'teaching'" class="feat-chip teach-feat">
              <el-icon class="feat-ic"><Tickets /></el-icon>
              高校规范教案
            </span>
          </div>
        </div>

        <!-- 卡片底栏：模型与长圆胶囊操作按钮 (纯粹单行优雅排版，不换行) -->
        <div class="card-footer">
          <div class="model-params">
            <span
              class="model-badge"
              :class="{ 'unset-model': isBoundModelUnset(item.boundModel) }"
              :title="getDisplayModelTooltip(item.boundModel)"
            >
              <el-icon><Cpu /></el-icon>
              <span class="model-name-text">{{ getDisplayModelName(item.boundModel) }}</span>
            </span>
          </div>

          <div class="card-actions">
            <el-button
              size="small"
              class="action-btn preview-btn"
              @click.stop="$emit('open-drawer', item, 'preview')"
            >
              预览
            </el-button>
            <el-button
              size="small"
              class="action-btn test-btn"
              @click.stop="$emit('open-drawer', item, 'test')"
            >
              热测试
            </el-button>
            <el-button
              size="small"
              type="primary"
              class="action-btn edit-btn"
              @click.stop="$emit('edit', item.id)"
            >
              编辑
            </el-button>
            <button
              v-if="item.status !== 'PUBLISHED'"
              type="button"
              class="table-action-pill table-action-pill--danger"
              @click.stop="$emit('delete', item)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态提示 -->
    <div v-else class="empty-state-card">
      <div class="empty-icon-wrap">
        <el-icon :size="48"><CollectionTag /></el-icon>
      </div>
      <h3>未检索到匹配的 Prompt 模板</h3>
      <p>当前分类下暂无符合检索条件的提示词工程资产，您可以重置筛选或新建模板。</p>
      <div class="empty-actions">
        <el-button @click="$emit('reset-filters')">重置筛选条件</el-button>
        <el-button type="primary" :icon="Plus" @click="$emit('create')">
          新建 Prompt 模板
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Search,
  CollectionTag,
  Cpu,
  Link,
  Lock,
  Document,
  Finished,
  Tickets
} from '@element-plus/icons-vue';
import type { PromptTemplate } from '@/types/system/prompt';

defineProps<{
  loading: boolean;
  filteredPrompts: PromptTemplate[];
  getCategoryLabel: (category: string) => string;
  getDisplayModelName: (boundModel?: string | null) => string;
  getDisplayModelTooltip: (boundModel?: string | null) => string;
  isBoundModelUnset: (boundModel?: string | null) => boolean;
}>();

defineEmits<{
  'copy-text': [text: string, msg: string];
  'open-drawer': [item: PromptTemplate, tab: 'preview' | 'test'];
  edit: [id: number];
  delete: [item: PromptTemplate];
  'reset-filters': [];
  create: [];
}>();
</script>

<style scoped lang="scss">
.prompt-grid-container {
  min-height: 420px;

  .prompt-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 18px;

    @media (max-width: 1300px) {
      grid-template-columns: repeat(2, 1fr);
    }
    @media (max-width: 800px) {
      grid-template-columns: 1fr;
    }
  }

  .prompt-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      border-color: #93C5FD;
      box-shadow: 0 8px 24px -4px rgba(37, 99, 235, 0.12), 0 4px 8px -2px rgba(37, 99, 235, 0.04);
    }

    .card-accent-bar {
      height: 3px;
      width: 100%;
      background: linear-gradient(90deg, #3B82F6, #60A5FA);
    }
    &.theme-rag .card-accent-bar {
      background: linear-gradient(90deg, #0284C7, #06B6D4);
    }
    &.theme-question .card-accent-bar {
      background: linear-gradient(90deg, #F59E0B, #FBBF24);
    }
    &.theme-grading .card-accent-bar {
      background: linear-gradient(90deg, #8B5CF6, #A855F7);
    }
    &.theme-teaching .card-accent-bar {
      background: linear-gradient(90deg, #10B981, #34D399);
    }

    .card-top-header {
      padding: 14px 18px 8px 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .category-box {
        display: flex;
        align-items: center;

        .category-tag {
          font-size: 11px;
          font-weight: 600;
          padding: 3px 10px;
          border-radius: 999px;
          white-space: nowrap;
          letter-spacing: 0.2px;

          &.rag {
            background: #E0F2FE;
            color: #0369A1;
          }
          &.question {
            background: #FEF3C7;
            color: #B45309;
          }
          &.grading {
            background: #F3E8FF;
            color: #7E22CE;
          }
          &.teaching {
            background: #DCFCE7;
            color: #15803D;
          }
        }
      }

      .status-and-version {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0;
        white-space: nowrap;

        .status-pill {
          font-size: 11px;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 999px;
          display: flex;
          align-items: center;
          gap: 5px;
          white-space: nowrap;
          flex-shrink: 0;

          &.status-online {
            background: #ECFDF5;
            color: #059669;
            .status-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10B981;
            }
          }
          &.status-draft {
            background: #FFFBEB;
            color: #D97706;
            .status-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #F59E0B;
            }
          }
        }

        .version-pill {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 8px;
          border-radius: 999px;
        }
      }
    }

    .card-body {
      padding: 6px 18px 14px 18px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      flex: 1;

      .prompt-name {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
        transition: color 0.15s;
      }

      .prompt-desc {
        margin: 0;
        font-size: 12.5px;
        color: #64748B;
        line-height: 1.55;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        min-height: 38px;
      }

      .feature-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .feat-chip {
          font-size: 11px;
          padding: 3px 10px;
          border-radius: 999px !important;
          font-weight: 600;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          line-height: 1.4;

          .feat-ic {
            font-size: 12px;
          }

          &.rag-feat {
            background: #F0F9FF;
            color: #0284C7;
            border: 1px solid #BAE6FD;
          }
          &.shield-feat {
            background: #FEF2F2;
            color: #DC2626;
            border: 1px solid #FECACA;
          }
          &.exam-feat {
            background: #FFFBEB;
            color: #D97706;
            border: 1px solid #FDE68A;
          }
          &.grade-feat {
            background: #FAF5FF;
            color: #9333EA;
            border: 1px solid #E9D5FF;
          }
          &.teach-feat {
            background: #F0FDF4;
            color: #16A34A;
            border: 1px solid #BBF7D0;
          }
        }
      }
    }

    .card-footer {
      padding: 10px 16px;
      background: #F8FAFC;
      border-top: 1px solid #F1F5F9;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;

      .model-params {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        flex-shrink: 1;

        .model-badge {
          font-size: 11.5px;
          color: #334155;
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          padding: 2px 8px;
          border-radius: 999px;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          white-space: nowrap;
          max-width: 140px;
          box-sizing: border-box;

          .model-name-text {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .el-icon {
            font-size: 13px;
            color: #64748B;
            flex-shrink: 0;
          }

          &.unset-model {
            color: #0369A1;
            background: #F0F9FF;
            border-color: #BAE6FD;

            .el-icon {
              color: #0284C7;
            }
          }
        }
      }

      .card-actions {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;

        .action-btn {
          font-size: 12px;
          font-weight: 500;
          padding: 0 11px !important;
          border-radius: 999px !important;
          height: 28px !important;
          line-height: 26px !important;
          box-sizing: border-box !important;
          white-space: nowrap !important;
          user-select: none;
          cursor: pointer;
          outline: none !important;
          transform: none !important;
          transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;

          &:active,
          &:focus,
          &:focus-visible {
            outline: none !important;
            transform: none !important;
          }

          &.preview-btn {
            background: #FFFFFF !important;
            border: 1px solid #D1D5DB !important;
            color: #475569 !important;

            &:hover {
              background: #F3F4F6 !important;
              border-color: #9CA3AF !important;
              color: #111827 !important;
            }

            &:active {
              background: #E5E7EB !important;
              border-color: #6B7280 !important;
            }
          }

          &.test-btn {
            background: #EFF6FF !important;
            border: 1px solid #BFDBFE !important;
            color: #2563EB !important;
            font-weight: 600 !important;

            &:hover {
              background: #DBEAFE !important;
              border-color: #93C5FD !important;
              color: #1D4ED8 !important;
            }

            &:active {
              background: #BFDBFE !important;
              border-color: #60A5FA !important;
            }
          }

          &.edit-btn {
            background: #2563EB !important;
            border: 1px solid #2563EB !important;
            color: #FFFFFF !important;
            font-weight: 600 !important;
            box-shadow: 0 1px 3px rgba(37, 99, 235, 0.2) !important;

            &:hover {
              background: #1D4ED8 !important;
              border-color: #1D4ED8 !important;
              box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3) !important;
              transform: none !important;
            }

            &:active {
              background: #1E40AF !important;
              border-color: #1E40AF !important;
              transform: none !important;
            }
          }
        }
      }
    }
  }

  .empty-state-card {
    background: #FFFFFF;
    border: 1px dashed #CBD5E1;
    border-radius: 16px;
    padding: 60px 20px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .empty-icon-wrap {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: #F1F5F9;
      color: #94A3B8;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16px;
    }

    h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      font-weight: 700;
      color: #1E293B;
    }

    p {
      margin: 0 0 20px 0;
      font-size: 13.5px;
      color: #64748B;
      max-width: 420px;
    }

    .empty-actions {
      display: flex;
      gap: 12px;
    }
  }
}
</style>
