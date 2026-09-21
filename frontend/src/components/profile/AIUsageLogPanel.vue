<template>
  <div class="log-panel-card">
    <div class="panel-header-line">
      <div class="panel-header-left">
        <div class="panel-icon-badge">
          <el-icon><Clock /></el-icon>
        </div>
        <div>
          <h3 class="panel-title">近期个人 AI 交互记录</h3>
          <span class="panel-sub">{{ currentPeriodSubText }}</span>
        </div>
      </div>

      <div class="panel-header-right">
        <div class="log-filter-pills">
          <button
            v-for="opt in filterOptions"
            :key="opt.days"
            type="button"
            class="filter-pill-btn"
            :class="{ 'is-active': selectedDays === opt.days }"
            @click="handleFilterChange(opt.days)"
          >
            {{ opt.label }}
          </button>
        </div>

        <span v-if="logTotal > 0" class="record-count-pill">
          {{ currentPeriodCountText }}
        </span>
      </div>
    </div>

    <div v-loading="logLoading" class="log-table-wrap">
      <el-empty
        v-if="!loading && !logLoading && usage.recentLogs.length === 0"
        class="empty-state"
        description="暂无 AI 调用记录，使用智能助教或出题功能后将在此展示"
      />

      <template v-else>
        <el-table
          :data="usage.recentLogs"
          style="width: 100%"
          :header-cell-style="tableHeaderStyle"
          :row-class-name="() => 'log-table-row'"
        >
          <el-table-column prop="sceneLabel" label="使用功能 / 场景" min-width="200">
            <template #default="{ row }">
              <span class="tool-title">{{ formatSceneDisplay(row.sceneLabel, row.scene) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="model" label="基座模型" width="220">
            <template #default="{ row }">
              <span class="model-pill">{{ formatModelDisplay(row.model) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalTokens" label="消耗 Token" width="150" align="center">
            <template #default="{ row }">
              <span class="tokens-pill">{{ formatNumber(row.totalTokens) }} Tokens</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="交互时间" width="190">
            <template #default="{ row }">
              <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <AppPagination
          v-model:page-num="pageNum"
          v-model:page-size="pageSize"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          @change="emit('page-change')"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Clock } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });
const selectedDays = defineModel<number>('selectedDays', { default: 7 });

const props = defineProps<{
  loading: boolean;
  logLoading: boolean;
  usage: PersonalAiUsageVO;
  logTotal: number;
  tableHeaderStyle: Record<string, string | number>;
  formatNumber: (value: number) => string;
  formatDateTime: (value: string) => string;
}>();

const emit = defineEmits<{
  'page-change': [];
  'days-change': [days: number];
}>();

const filterOptions = [
  { label: '全部', days: 0 },
  { label: '今日 (1日)', days: 1 },
  { label: '近 7 天 (一周)', days: 7 },
  { label: '近 30 天 (一个月)', days: 30 }
];

const currentPeriodSubText = computed(() => {
  if (selectedDays.value === 0) return '全部历史调用明细';
  if (selectedDays.value === 1) return '今日调用明细';
  if (selectedDays.value === 30) return '近 30 天调用明细';
  return '近 7 天调用明细';
});

const currentPeriodCountText = computed(() => {
  if (selectedDays.value === 0) return `全部累计共 ${props.logTotal} 条`;
  if (selectedDays.value === 1) return `今日共 ${props.logTotal} 条`;
  if (selectedDays.value === 30) return `近 30 天共 ${props.logTotal} 条`;
  return `近 7 天共 ${props.logTotal} 条`;
});

const SCENE_DISPLAY_MAP: Record<string, string> = {
  LEARNING: 'AI 自适应学习辅导',
  learning: 'AI 自适应学习辅导',
  MEMORY_EXTRACT: '学情长期记忆沉淀',
  memory_extract: '学情长期记忆沉淀',
  CHAT: '课程 AI 智能对话',
  chat: 'AI 智能对话',
  CHAT_RAG: '课程 AI 智能助教 (RAG 问答)',
  chat_rag: '课程 AI 智能助教 (RAG 问答)',
  CHAT_TITLE: '会话标题生成',
  chat_title: '会话标题生成',
  GLOBAL_ASSISTANT: '全局 AI 教学助手',
  global_assistant: '全局 AI 教学助手',
  QUESTION_GENERATE: 'AI 自适应练习生成',
  question_generate: 'AI 自适应练习生成',
  QUESTION: 'AI 自适应试题生成',
  question: 'AI 自适应试题生成',
  SUBJECTIVE_GRADING: '主观题智能评阅与批改',
  subjective_grading: '主观题智能评阅与批改',
  GRADING: '主观题智能评阅与批改',
  grading: '主观题智能评阅与批改',
  SUMMARY: '章节摘要提炼',
  summary: '章节摘要提炼',
  LESSON_PLAN: '智能教案生成',
  lesson_plan: '智能教案生成',
  PREP: '智能备课教案',
  prep: '智能备课教案',
  PAPER_COMPOSE: '智能组卷与试题分析',
  paper_compose: '智能组卷与试题分析',
  EXAM: '智能组卷与试题分析',
  exam: '智能组卷与试题分析',
  TEACHING_ADVICE: 'AI 学情诊断与教学建议',
  teaching_advice: 'AI 学情诊断与教学建议',
  COURSE_OBJECTIVE: '课程教学目标 AI 推荐',
  course_objective: '课程教学目标 AI 推荐',
  COURSE_DESCRIPTION: '课程简介 AI 生成',
  course_description: '课程简介 AI 生成',
  COURSE_KNOWLEDGE_POINT: '课程知识点 AI 推荐',
  course_knowledge_point: '课程知识点 AI 推荐',
  RAG: 'RAG 知识检索问答',
  rag: 'RAG 知识检索问答',
  KB_RETRIEVAL: '知识库检索增强',
  kb_retrieval: '知识库检索增强',
  EVALUATION: '学情综合诊断评估',
  evaluation: '学情综合诊断评估',
  AGENT: 'AI Agent 任务规划',
  agent: 'AI Agent 任务规划',
  GRAPH_SUGGEST: '知识图谱关系推荐',
  graph_suggest: '知识图谱关系推荐',
  STREAM: '流式 AI 对话',
  stream: '流式 AI 对话',
  OCR: '智能 OCR 文本识别',
  ocr: '智能 OCR 文本识别',
  EMBEDDING: '知识向量化嵌入',
  embedding: '知识向量化嵌入',
  RERANK: '语义重排检索优化',
  rerank: '语义重排检索优化'
};

function formatSceneDisplay(sceneLabel?: string, scene?: string): string {
  if (sceneLabel && SCENE_DISPLAY_MAP[sceneLabel]) {
    return SCENE_DISPLAY_MAP[sceneLabel];
  }
  if (scene && SCENE_DISPLAY_MAP[scene]) {
    return SCENE_DISPLAY_MAP[scene];
  }
  if (sceneLabel && !/^[A-Z0-9_-]+$/.test(sceneLabel.trim())) {
    return sceneLabel;
  }
  const key = (scene || sceneLabel || '').toUpperCase();
  if (SCENE_DISPLAY_MAP[key]) {
    return SCENE_DISPLAY_MAP[key];
  }
  return sceneLabel || scene || 'AI 综合服务';
}

function formatModelDisplay(model?: string): string {
  if (!model || model.trim().toLowerCase() === 'unknown') {
    return '系统默认模型';
  }
  return model;
}

function handleFilterChange(days: number) {
  selectedDays.value = days;
  emit('days-change', days);
}
</script>

<style scoped lang="scss">
.log-panel-card {
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid #E2E8F0;
  padding: 24px 28px 8px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);

  .panel-header-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;
    gap: 12px;
    flex-wrap: wrap;

    .panel-header-left {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .panel-icon-badge {
      width: 44px;
      height: 44px;
      border-radius: 16px;
      background: linear-gradient(135deg, #EFF6FF 0%, #F5F3FF 100%);
      border: 1px solid #E2E8F0;
      color: #1677FF;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .panel-title {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
    }

    .panel-sub {
      display: block;
      font-size: 12px;
      color: #94A3B8;
      margin-top: 2px;
    }

    .panel-header-right {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .log-filter-pills {
      display: inline-flex;
      align-items: center;
      background: #F1F5F9;
      padding: 3px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
      gap: 2px;

      .filter-pill-btn {
        border: none;
        background: transparent;
        padding: 5px 12px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        color: #64748B;
        cursor: pointer;
        transition: all 0.2s ease;
        outline: none;

        &:hover:not(.is-active) {
          color: #1E293B;
        }

        &.is-active {
          background: #FFFFFF;
          color: #2563EB;
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
        }
      }
    }

    .record-count-pill {
      padding: 6px 16px;
      border-radius: 9999px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      font-size: 12px;
      font-weight: 600;
      color: #64748B;
    }
  }

  .log-table-wrap {
    :deep(.empty-state) {
      padding: 48px 0;

      .el-empty__description {
        color: #94A3B8;
        font-size: 13px;
      }
    }

    :deep(.el-table) {
      --el-table-border-color: transparent;
      --el-table-row-hover-bg-color: #F8FAFC;
      background: transparent;

      &::before { display: none; }

      .el-table__header-wrapper th.el-table__cell {
        border-bottom: 1px solid #EEF2F7;
      }

      .log-table-row td.el-table__cell {
        padding: 14px 0;
        border-bottom: 1px solid #F8FAFC;
      }

      .log-table-row:last-child td.el-table__cell {
        border-bottom: none;
      }
    }
  }

  .tool-title {
    font-size: 13.5px;
    font-weight: 600;
    color: #1E293B;
  }

  .model-pill {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 500;
    color: #475569;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
  }

  .tokens-pill {
    display: inline-block;
    font-family: ui-monospace, 'Cascadia Code', monospace;
    font-size: 12px;
    font-weight: 600;
    color: #2563EB;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    padding: 4px 12px;
    border-radius: 9999px;
  }

  .time-text {
    font-size: 12.5px;
    color: #64748B;
    font-variant-numeric: tabular-nums;
  }
}
</style>
