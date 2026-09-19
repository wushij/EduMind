<template>
  <el-dialog
    :model-value="visible"
    title="AI 智能考点提炼与推荐"
    width="640px"
    append-to-body
    destroy-on-close
    class="ai-suggest-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="!loading" class="ai-dialog-intro">
      <div class="spark-badge">
        <el-icon><MagicStick /></el-icon>
        <span>AI 课程知识图谱引擎</span>
      </div>
      <p>结合当前课程章节与已有考点，生成可入库的结构化考点（含说明与考查重点）。</p>
    </div>

    <AiCognitiveThinkingPanel
      v-if="loading"
      :active="loading"
      v-bind="AI_COGNITIVE_THINKING_PRESETS.knowledgePointSuggest"
      show-footer-actions
      abort-label="中止提炼"
      @abort="$emit('abort')"
    />

    <div v-else class="ai-points-list">
      <div v-for="(p, pIdx) in points" :key="pIdx" class="ai-point-card">
        <div class="point-top-row">
          <span class="point-num">考点 {{ pIdx + 1 }}</span>
          <strong class="point-title">{{ p.title }}</strong>
          <el-tag size="small">{{ p.cognitiveDimension || 'APPLY' }}</el-tag>
        </div>
        <p class="point-desc">{{ p.description || '（暂无说明）' }}</p>
        <div v-if="p.examFocus" class="focus-tag">考查：{{ p.examFocus }}</div>
        <div class="card-apply-action">
          <el-button size="small" class="point-apply-btn" @click="$emit('apply', p)">
            <el-icon><EditPen /></el-icon>
            采纳并编辑
          </el-button>
        </div>
      </div>
      <el-empty v-if="!points.length" description="暂无推荐考点，请换一批或稍后重试" />
    </div>

    <template v-if="!loading" #footer>
      <div class="ai-modal-footer">
        <el-button :loading="loading" :disabled="loading" @click="$emit('refresh')">
          <el-icon><Refresh /></el-icon>
          换一批考点
        </el-button>
        <div class="right-group">
          <el-button @click="$emit('update:visible', false)">关闭</el-button>
          <el-button
            type="primary"
            :disabled="loading || !points.length"
            @click="$emit('batch-import', points)"
          >
            一键批量导入 ({{ points.length }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { MagicStick, Refresh, EditPen } from '@element-plus/icons-vue';
import type { CourseKnowledgePointSuggestItem } from '@/types/course/knowledge-point';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';

defineProps<{
  visible: boolean;
  loading: boolean;
  points: CourseKnowledgePointSuggestItem[];
}>();

defineEmits<{
  'update:visible': [boolean];
  apply: [CourseKnowledgePointSuggestItem];
  'batch-import': [CourseKnowledgePointSuggestItem[]];
  refresh: [];
  abort: [];
}>();
</script>

<style scoped lang="scss">
.ai-dialog-intro {
  margin-bottom: 14px;

  .spark-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    border-radius: 9999px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    color: #2563eb;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 6px;
  }

  p {
    font-size: 13px;
    color: #475569;
    margin: 0;
  }
}

.ai-points-list {
  max-height: 380px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-point-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;

  .point-top-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;

    .point-num {
      font-size: 11px;
      font-weight: 700;
      color: #2563eb;
      background: #dbeafe;
      padding: 2px 6px;
      border-radius: 6px;
    }

    .point-title {
      flex: 1;
      font-size: 14px;
    }
  }

  .point-desc {
    font-size: 12.5px;
    color: #64748b;
    margin: 0 0 6px;
  }

  .focus-tag {
    font-size: 11px;
    color: #b45309;
    margin-bottom: 8px;
  }

  .card-apply-action {
    display: flex;
    justify-content: flex-end;
  }
}

.ai-modal-footer {
  display: flex;
  justify-content: space-between;
  width: 100%;

  .right-group {
    display: flex;
    gap: 10px;
  }
}
</style>
