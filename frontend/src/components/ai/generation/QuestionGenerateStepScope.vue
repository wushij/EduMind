<template>
  <div class="step-content-pane">
    <div class="scope-header-row">
      <div>
        <h3 class="pane-title">第 2 步：划定考察章节与核心考点范围</h3>
        <p class="pane-desc">多选需要考察的章节范围，AI 将对重点知识点及其考查要点（examFocus）进行题目精准覆盖：</p>
      </div>

      <div class="chapter-actions">
        <button type="button" class="mini-pill-btn" @click="$emit('select-all-chapters')">
          全选章节
        </button>
        <button type="button" class="mini-pill-btn" @click="$emit('clear-chapters')">
          清空
        </button>
      </div>
    </div>

    <!-- 章节选择列表 -->
    <div v-loading="loadingChapters" class="chapters-selection-list">
      <label
        v-for="ch in currentCourseChapters"
        :key="ch.id"
        class="chapter-check-row"
        :class="{ checked: formState.chapterIds.includes(ch.id) }"
      >
        <input
          type="checkbox"
          :value="ch.id"
          :checked="formState.chapterIds.includes(ch.id)"
          class="hidden-checkbox"
          @change="$emit('toggle-chapter', ch.id)"
        />
        <div class="check-box-circle">
          <el-icon v-if="formState.chapterIds.includes(ch.id)"><Check /></el-icon>
        </div>
        <div class="chapter-label-col">
          <div class="ch-top">
            <strong class="ch-title">{{ ch.title }}</strong>
            <span v-if="formState.chapterIds.includes(ch.id)" class="ch-active-tag">已圈定</span>
          </div>
          <span class="ch-desc">{{ ch.description }}</span>
        </div>
      </label>
    </div>

    <!-- 考点倾向与认知维度选择区 -->
    <div class="kp-chips-section">
      <div class="kp-title-row">
        <span class="kp-tips-title">
          <el-icon class="mr-1 text-amber-500"><Opportunity /></el-icon>
          命题知识点范围与考查要点（标有【重点/核心】为大纲高频考点，已选中 {{ formState.knowledgePointNames.length }} 个）：
        </span>
        <div class="kp-search-inline">
          <el-input
            v-model="kpFilterText"
            placeholder="过滤知识点..."
            size="small"
            clearable
            class="kp-filter-input"
            :prefix-icon="Search"
          />
        </div>
      </div>

      <div v-loading="loadingKps" class="kp-pills-row">
        <div
          v-for="kp in filteredKnowledgePoints"
          :key="kp.id || kp.title"
          class="pill-selectable-kp"
          :class="{ selected: formState.knowledgePointNames.includes(kp.title) }"
          :title="kp.examFocus ? ('考查要点：' + kp.examFocus) : (kp.importance && kp.importance >= 4 ? '教学大纲重点考点' : undefined)"
          @click="$emit('toggle-kp', kp)"
        >
          <span class="kp-title-text">{{ kp.title }}</span>
          <span v-if="kp.cognitiveDimension" class="cog-badge">
            {{ formatCognitive(kp.cognitiveDimension) }}
          </span>
          <span
            v-if="kp.importance && kp.importance >= 4"
            class="focus-badge"
            :class="{ 'core-badge': kp.importance === 5 }"
          >
            {{ kp.importance === 5 ? '核心' : '重点' }}
          </span>
          <el-icon v-if="formState.knowledgePointNames.includes(kp.title)" class="check-ic">
            <Check />
          </el-icon>
        </div>

        <div v-if="filteredKnowledgePoints.length === 0" class="no-kps-tip">
          <span>暂无匹配的考点标签</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Check, Opportunity, Search } from '@element-plus/icons-vue';
import type { QuestionGenerateFormState, KnowledgePointItem } from './question-generate-types';

const props = defineProps<{
  formState: QuestionGenerateFormState;
  currentCourseChapters: any[];
  availableKnowledgePoints: KnowledgePointItem[];
  loadingChapters?: boolean;
  loadingKps?: boolean;
}>();

defineEmits<{
  'toggle-chapter': [id: number];
  'select-all-chapters': [];
  'clear-chapters': [];
  'toggle-kp': [kp: KnowledgePointItem];
}>();

const kpFilterText = ref('');

const filteredKnowledgePoints = computed(() => {
  if (!kpFilterText.value.trim()) return props.availableKnowledgePoints;
  const q = kpFilterText.value.toLowerCase().trim();
  return props.availableKnowledgePoints.filter(k =>
    k.title.toLowerCase().includes(q) || (k.examFocus && k.examFocus.toLowerCase().includes(q))
  );
});

function formatCognitive(dim: string): string {
  const map: Record<string, string> = {
    REMEMBER: '识记',
    UNDERSTAND: '理解',
    APPLY: '应用',
    ANALYZE: '分析',
    EVALUATE: '综合',
    CREATE: '设计'
  };
  return map[dim] || '考点';
}
</script>

<style scoped lang="scss">
.step-content-pane {
  .scope-header-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;

    .pane-title {
      margin: 0 0 6px 0;
      font-size: 18px;
      font-weight: 800;
      color: #0F172A;
    }

    .pane-desc {
      margin: 0;
      font-size: 13.5px;
      color: #64748B;
    }

    .chapter-actions {
      display: flex;
      gap: 8px;
      flex-shrink: 0;

      .mini-pill-btn {
        height: 30px;
        padding: 0 12px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #CBD5E1;
        color: #475569;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: #1677FF;
          color: #1677FF;
          background: #EFF6FF;
        }
      }
    }
  }
}

.chapters-selection-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;

  .chapter-check-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 18px;
    border-radius: 14px;
    border: 1.5px solid #E2E8F0;
    background: #FFFFFF;
    cursor: pointer;
    transition: all 0.2s;

    .hidden-checkbox {
      display: none;
    }

    .check-box-circle {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 2px solid #CBD5E1;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #FFFFFF;
      margin-top: 2px;
      flex-shrink: 0;
      transition: all 0.2s;
    }

    .chapter-label-col {
      flex: 1;

      .ch-top {
        display: flex;
        align-items: center;
        gap: 8px;

        .ch-title {
          font-size: 14px;
          color: #0F172A;
        }

        .ch-active-tag {
          font-size: 11px;
          color: #1677FF;
          background: #EFF6FF;
          padding: 1px 7px;
          border-radius: 9999px;
          font-weight: 600;
        }
      }

      .ch-desc {
        font-size: 12.5px;
        color: #64748B;
        margin-top: 3px;
        display: block;
      }
    }

    &.checked {
      border-color: #1677FF;
      background: #F0F7FF;

      .check-box-circle {
        background: #1677FF;
        border-color: #1677FF;
      }
    }

    &:hover:not(.checked) {
      border-color: #93C5FD;
      background: #FAFCFF;
    }
  }
}

.kp-chips-section {
  padding-top: 18px;
  border-top: 1px solid #F1F5F9;

  .kp-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 12px;
    flex-wrap: wrap;

    .kp-tips-title {
      font-size: 13.5px;
      font-weight: 700;
      color: #1E293B;
      display: inline-flex;
      align-items: center;
    }

    .kp-search-inline {
      width: 200px;
      :deep(.el-input__wrapper) {
        border-radius: 9999px;
      }
    }
  }

  .kp-pills-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .pill-selectable-kp {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      color: #475569;
      font-size: 12.5px;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .kp-title-text {
        font-weight: 500;
      }

      .cog-badge {
        font-size: 10.5px;
        background: #F1F5F9;
        color: #475569;
        padding: 1px 6px;
        border-radius: 9999px;
      }

      .focus-badge {
        font-size: 10.5px;
        background: #FEF3C7;
        color: #D97706;
        padding: 1px 5px;
        border-radius: 4px;
        font-weight: 600;

        &.core-badge {
          background: #FEE2E2;
          color: #DC2626;
        }
      }

      .check-ic {
        font-size: 12px;
      }

      &:hover {
        border-color: #93C5FD;
        color: #1677FF;
        transform: translateY(-1px);
      }

      &.selected {
        border-color: #1677FF;
        background: #EFF6FF;
        color: #1677FF;
        font-weight: 600;

        .cog-badge {
          background: #DBEAFE;
          color: #1D4ED8;
        }
      }
    }

    .no-kps-tip {
      font-size: 12.5px;
      color: #94A3B8;
      padding: 10px 0;
    }
  }
}
</style>
