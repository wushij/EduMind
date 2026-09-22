<template>
  <div class="learning-path-graph-panel">
    <div class="panel-header">
      <div class="header-left">
        <span class="title-decor-pill"></span>
        <h3 class="panel-title">知识图谱路径视图</h3>
      </div>
      <div class="legend-row">
        <span class="legend-item legend-weak">
          <i class="legend-dot"></i>薄弱 {{ statusCount.WEAK }}
        </span>
        <span class="legend-item legend-learning">
          <i class="legend-dot"></i>学习中 {{ statusCount.LEARNING }}
        </span>
        <span class="legend-item legend-mastered">
          <i class="legend-dot"></i>已掌握 {{ statusCount.MASTERED }}
        </span>
      </div>
    </div>

    <div v-if="!hasGraph" class="graph-empty">
      <el-empty
        description="当前课程未绑定知识库，暂无法展示图谱视图；仍可按右侧周计划学习"
        :image-size="100"
      />
    </div>

    <template v-else>
      <div class="graph-body">
        <KnowledgeGraphG6
          :graph-data="graphVo"
          :height="graphHeight"
          :bordered="false"
          show-toolbar
          @node-click="onNodeClick"
        />
      </div>

      <div class="graph-footer">
        <template v-if="selectedNode">
          <span class="sel-name">{{ selectedNode.label }}</span>
          <span v-if="selectedStatusLabel" class="sel-status" :class="selectedStatusClass">
            {{ selectedStatusLabel }}
          </span>
          <span v-if="selectedNode.masteryPercent != null" class="sel-mastery">
            掌握度 {{ Math.round(selectedNode.masteryPercent) }}%
          </span>
          <button v-if="matchedWeekNo" type="button" class="sel-jump" @click="focusWeek(matchedWeekNo)">
            查看第 {{ matchedWeekNo }} 周计划
          </button>
        </template>
        <span v-else class="footer-hint">
          点击图中的知识点可定位到对应周计划；节点颜色与你的掌握度实时同步（数据来自作业与测评批改）
        </span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import KnowledgeGraphG6 from '@/components/knowledge/KnowledgeGraphG6.vue';
import type {
  LearningPathGraphNode,
  LearningPathGraphSlice,
  LearningPathWeek
} from '@/types/learning/learning-path';
import type { KnowledgeGraphNode, KnowledgeGraphVO } from '@/types/knowledge/graph';
import { weekAnchorId } from '@/utils/learning/learning-path';

const props = withDefaults(
  defineProps<{
    graphSlice: LearningPathGraphSlice | null | undefined;
    /** 周计划：用于把图谱节点定位到对应周卡片 */
    weeks?: LearningPathWeek[];
  }>(),
  {
    weeks: () => []
  }
);

const emit = defineEmits<{
  (e: 'focus-week', weekNo: number): void;
}>();

const hasGraph = computed(() => (props.graphSlice?.nodes?.length ?? 0) > 0);

const STATUS_LABELS: Record<string, string> = {
  WEAK: '薄弱',
  LEARNING: '学习中',
  MASTERED: '已掌握'
};

/** 节点数量随规模自适应画布高度，避免节点少时四周留白、节点多时过于拥挤 */
const graphHeight = computed(() => {
  const count = props.graphSlice?.nodes?.length ?? 0;
  return Math.min(600, Math.max(440, 220 + count * 26));
});

/** 真实状态统计：让图例带上数字，直观体现图谱与掌握度是同一份数据 */
const statusCount = computed(() => {
  const counter = { WEAK: 0, LEARNING: 0, MASTERED: 0 };
  (props.graphSlice?.nodes ?? []).forEach((n) => {
    if (n.status && n.status in counter) {
      counter[n.status as keyof typeof counter] += 1;
    }
  });
  return counter;
});

/**
 * 图谱数据映射。
 * 关键修复：此前只透传 id/label/type/refId，把后端的 status、masteryPercent、highlightNodeIds 全部丢弃，
 * 导致节点永远只有「类型色」、图例「薄弱/学习中/已掌握」形同虚设。这里完整透传。
 */
const graphVo = computed<KnowledgeGraphVO>(() => {
  const highlightIds = new Set(props.graphSlice?.highlightNodeIds ?? []);
  return {
    nodes: (props.graphSlice?.nodes ?? []).map((n) => ({
      id: n.id,
      label: n.label,
      type: n.type,
      refId: n.refId,
      masteryPercent: n.masteryPercent,
      status: n.status,
      highlight: highlightIds.has(n.id)
    })),
    edges: (props.graphSlice?.edges ?? []).map((e) => ({
      source: e.source,
      target: e.target,
      relation: e.relation
    }))
  };
});

const selectedNode = ref<LearningPathGraphNode | null>(null);

const selectedStatusLabel = computed(() =>
  selectedNode.value?.status ? STATUS_LABELS[selectedNode.value.status] || '' : ''
);

const selectedStatusClass = computed(() => {
  const status = selectedNode.value?.status;
  if (status === 'WEAK') return 'sel-status--weak';
  if (status === 'LEARNING') return 'sel-status--learning';
  if (status === 'MASTERED') return 'sel-status--mastered';
  return '';
});

/** 选中的知识点对应的周计划序号（按 knowledgePointId 匹配） */
const matchedWeekNo = computed(() => {
  const refId = selectedNode.value?.refId;
  if (refId == null) return null;
  const week = props.weeks.find((w) => w.knowledgePointId === refId);
  return week ? week.weekNo : null;
});

function focusWeek(weekNo: number) {
  const el = document.getElementById(weekAnchorId(weekNo));
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  emit('focus-week', weekNo);
}

function onNodeClick(node: KnowledgeGraphNode | null) {
  // 只对知识点节点做定位；章节/切片节点没有对应周计划
  selectedNode.value = (node as LearningPathGraphNode | null) ?? null;
  if (matchedWeekNo.value) {
    focusWeek(matchedWeekNo.value);
  }
}
</script>

<style scoped lang="scss">
.learning-path-graph-panel {
  background: #fff;
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    border-bottom: 1px solid #f1f5f9;
    flex-wrap: wrap;
    gap: 10px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .title-decor-pill {
    width: 4px;
    height: 16px;
    border-radius: 9999px;
    background: #1677ff;
  }

  .panel-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }

  .legend-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 11.5px;
    padding: 3px 10px;
    border-radius: 9999px;
    font-weight: 600;

    .legend-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: currentColor;
      flex-shrink: 0;
    }

    &.legend-weak {
      background: #fff1f2;
      color: #e11d48;
      border: 1px solid rgba(225, 29, 72, 0.15);
    }
    &.legend-learning {
      background: #fffbeb;
      color: #d97706;
      border: 1px solid rgba(217, 119, 6, 0.15);
    }
    &.legend-mastered {
      background: #ecfdf5;
      color: #059669;
      border: 1px solid rgba(5, 150, 105, 0.15);
    }
  }

  .graph-body {
    // 画布已改为无边框嵌入，这里只留一点呼吸感，避免与面板边框贴死
    padding: 4px 6px 0;
  }

  .graph-footer {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    padding: 12px 20px 16px;
    border-top: 1px solid #f1f5f9;
    margin-top: 12px;
  }

  .footer-hint {
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.6;
  }

  .sel-name {
    font-size: 13px;
    font-weight: 700;
    color: #0f172a;
  }

  .sel-status {
    font-size: 11.5px;
    padding: 2px 10px;
    border-radius: 9999px;
    font-weight: 600;

    &--weak {
      background: #fef2f2;
      color: #dc2626;
    }
    &--learning {
      background: #fffbeb;
      color: #d97706;
    }
    &--mastered {
      background: #ecfdf5;
      color: #059669;
    }
  }

  .sel-mastery {
    font-size: 12px;
    color: #475569;
    font-weight: 600;
  }

  .sel-jump {
    margin-left: auto;
    border: none;
    cursor: pointer;
    padding: 6px 14px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;

    &:hover {
      opacity: 0.92;
    }
  }

  .graph-empty {
    padding: 40px 24px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
