<template>
  <div class="summary-history">
    <div class="panel-head">
      <div class="head-left">
        <el-icon class="head-icon"><Clock /></el-icon>
        <h3>历史总结</h3>
        <span class="count-chip">{{ records.length }}</span>
      </div>
      <el-button text size="small" @click="emit('refresh')">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="keyword"
        size="small"
        clearable
        placeholder="搜索标题 / 来源"
        class="search-input"
        @keyup.enter="emit('refresh')"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select
        v-model="historyCourseId"
        size="small"
        clearable
        placeholder="全部课程"
        class="course-filter"
        @change="emit('refresh')"
      >
        <el-option :label="'全部课程'" :value="0" />
        <el-option
          v-for="course in courseOptions"
          :key="course.id"
          :label="course.name"
          :value="course.id"
        />
      </el-select>
    </div>

    <div v-loading="loading" class="history-list">
      <el-empty
        v-if="!loading && records.length === 0"
        description="暂无历史总结"
        :image-size="70"
      />

      <button
        v-for="record in records"
        :key="record.id"
        type="button"
        class="history-item"
        :class="{ 'is-active': activeId === record.id }"
        @click="emit('open', record)"
      >
        <div class="item-main">
          <div class="item-title-row">
            <el-icon class="item-icon"><DocumentCopy /></el-icon>
            <span class="item-title">{{ record.title }}</span>
          </div>
          <div class="item-meta">
            <span class="mode-chip">{{ record.modeLabel }}</span>
            <span v-if="record.documentName" class="meta-text">{{ record.documentName }}</span>
            <span v-if="record.wordCount" class="meta-text">{{ record.wordCount }} 字</span>
          </div>
          <div class="item-foot">
            <span class="meta-text">{{ formatTime(record.createTime) }}</span>
            <span v-if="record.courseName" class="meta-text">{{ record.courseName }}</span>
          </div>
        </div>

        <div class="item-actions" @click.stop>
          <el-tooltip content="重命名" placement="top">
            <el-button text size="small" @click="emit('rename', record)">
              <el-icon><EditPen /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button text size="small" @click="emit('remove', record)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Clock, Refresh, Search, DocumentCopy, EditPen, Delete } from '@element-plus/icons-vue';
import type { SummaryRecord } from '@/types/ai/summary';
import type { SummaryCourseOption } from '@/composables/ai/useSummaryStudio';

const keyword = defineModel<string>('keyword', { required: true });
const historyCourseId = defineModel<number>('historyCourseId', { required: true });

defineProps<{
  records: SummaryRecord[];
  loading: boolean;
  courseOptions: SummaryCourseOption[];
  activeId: number | null;
}>();

const emit = defineEmits<{
  open: [record: SummaryRecord];
  rename: [record: SummaryRecord];
  remove: [record: SummaryRecord];
  refresh: [];
}>();

function formatTime(value?: string | null): string {
  if (!value) return '';
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped lang="scss">
.summary-history {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 22px 24px 14px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  display: flex;
  flex-direction: column;

  .panel-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .head-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .head-icon {
        font-size: 17px;
        color: #2563eb;
      }

      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
        color: #0f172a;
      }

      .count-chip {
        font-size: 11px;
        color: #64748b;
        background: #f1f5f9;
        border-radius: 9999px;
        padding: 1px 9px;
      }
    }
  }

  .filter-bar {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;

    .search-input { flex: 1; }
    .course-filter { width: 128px; flex-shrink: 0; }
  }

  .history-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 520px;
    overflow: auto;
    min-height: 120px;

    .history-item {
      position: relative;
      display: flex;
      align-items: stretch;
      justify-content: space-between;
      gap: 8px;
      padding: 12px 14px;
      border-radius: 14px;
      background: #f8fafc;
      border: 1px solid #eef2f7;
      cursor: pointer;
      text-align: left;
      transition: all 0.18s ease;

      &:hover {
        border-color: #bfdbfe;
        background: #ffffff;
      }

      &.is-active {
        border-color: #60a5fa;
        background: linear-gradient(135deg, #f5f9ff 0%, #ffffff 100%);
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
      }

      .item-main {
        display: flex;
        flex-direction: column;
        gap: 5px;
        min-width: 0;
        flex: 1;

        .item-title-row {
          display: flex;
          align-items: center;
          gap: 6px;
          min-width: 0;

          .item-icon {
            font-size: 14px;
            color: #2563eb;
            flex-shrink: 0;
          }

          .item-title {
            font-size: 13px;
            font-weight: 600;
            color: #0f172a;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .item-meta,
        .item-foot {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }

        .mode-chip {
          font-size: 10.5px;
          color: #1d4ed8;
          background: #eff6ff;
          border: 1px solid #bfdbfe;
          border-radius: 9999px;
          padding: 1px 8px;
        }

        .meta-text {
          font-size: 11px;
          color: #94a3b8;
        }
      }

      .item-actions {
        display: flex;
        align-items: center;
        gap: 2px;
        flex-shrink: 0;
      }
    }
  }
}
</style>
