<template>
  <el-drawer
    :model-value="visible"
    title="错题深度认知归因与前驱依赖分析"
    size="560px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading">
      <div v-if="item" class="drawer-content">
        <div class="drawer-section">
          <h4 class="section-title">
            <el-icon><Compass /></el-icon>
            错因根源诊断
          </h4>
          <div class="diagnosis-callout">
            <p class="summary-p">{{ item.diagnosis || '正在生成 AI 认知根因诊断...' }}</p>
            <div v-if="errorTags.length" class="tags-row">
              <el-tag v-for="err in errorTags" :key="err" type="danger" effect="plain">
                失分主因：{{ err }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="drawer-section">
          <h4 class="section-title">
            <el-icon><Connection /></el-icon>
            知识图谱前驱依赖溯源
          </h4>
          <el-empty
            v-if="!prerequisiteNodes.length"
            description="当前考点暂无配置前驱依赖，可在课程知识图谱中维护 prerequisite 关系"
            :image-size="80"
          />
          <div v-else class="prereq-tree">
            <template v-for="(node, idx) in prerequisiteNodes" :key="node.knowledgePointId">
              <div
                class="tree-node"
                :class="node.current ? 'current-node' : 'parent-node'"
              >
                <span class="node-badge" :class="{ 'node-badge-danger': node.current }">
                  {{ node.current ? '当前错题考点' : '前驱基础考点' }}
                </span>
                <span class="node-title">{{ node.name }}</span>
                <el-tag size="small" :type="node.current ? 'danger' : 'success'">
                  掌握度 {{ node.masteryPercent }}%
                </el-tag>
              </div>
              <div v-if="idx < prerequisiteNodes.length - 1" class="tree-link-line" />
            </template>
          </div>
        </div>

        <div class="drawer-section">
          <h4 class="section-title">
            <el-icon><Tickets /></el-icon>
            推荐变式攻坚题集
          </h4>
          <el-empty
            v-if="!variantQuestions.length"
            description="暂无变式题，可点击诊断生成 AI 变式巩固题"
            :image-size="80"
          />
          <div v-else class="variant-list">
            <div v-for="(variant, idx) in variantQuestions" :key="variant.questionId" class="variant-item-box">
              <div class="variant-left">
                <span class="variant-idx">变式 #{{ idx + 1 }}</span>
                <span class="variant-desc">{{ variant.stemPreview }}</span>
              </div>
              <el-button size="small" type="primary" plain @click="emit('practice-variant', variant.questionId)">
                立即自测
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('update:visible', false)">关闭</el-button>
        <el-button type="primary" :disabled="!item" @click="emit('start-practice')">
          开始这组变式题练习
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Compass, Connection, Tickets } from '@element-plus/icons-vue';
import type {
  WrongBookKnowledgeGraphNode,
  WrongBookVariantSummary,
  WrongQuestionRecordItem
} from '@/types/learning/wrong-question';

const props = defineProps<{
  visible: boolean;
  loading: boolean;
  item: WrongQuestionRecordItem | null;
  prerequisiteNodes: WrongBookKnowledgeGraphNode[];
  variantQuestions: WrongBookVariantSummary[];
  displayErrorTags: (item: WrongQuestionRecordItem) => string[];
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'practice-variant': [questionId: number];
  'start-practice': [];
}>();

const errorTags = computed(() => (props.item ? props.displayErrorTags(props.item) : []));
</script>

<style scoped lang="scss">
.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.drawer-section {
  .section-title {
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 12px;
  }

  .diagnosis-callout {
    background: #f8fafc;
    border-radius: 10px;
    padding: 14px;
    border-left: 4px solid #f5222d;

    .summary-p {
      margin: 0 0 10px;
      font-size: 14px;
      line-height: 1.6;
      color: #334155;
    }

    .tags-row {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  }

  .prereq-tree {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding-left: 10px;
    width: 100%;

    .tree-node {
      padding: 10px 14px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      gap: 10px;
      border: 1px solid #e2e8f0;
      width: 100%;
      flex-wrap: wrap;

      .node-badge {
        font-size: 11px;
        padding: 2px 6px;
        border-radius: 4px;
        background: #e2e8f0;
        color: #475569;
        font-weight: 600;

        &.node-badge-danger {
          background: #fee2e2;
          color: #dc2626;
        }
      }

      .node-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        flex: 1;
        min-width: 120px;
      }

      &.parent-node {
        background: #f0fdf4;
        border-color: #bbf7d0;
      }

      &.current-node {
        background: #fef2f2;
        border-color: #fecaca;
      }
    }

    .tree-link-line {
      width: 2px;
      height: 20px;
      background: #cbd5e1;
      margin-left: 30px;
    }
  }

  .variant-list {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .variant-item-box {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 14px;
      border-radius: 8px;
      border: 1px solid #e2e8f0;
      background: #fff;
      gap: 12px;

      &:hover {
        border-color: #1677ff;
        background: #f0f7ff;
      }

      .variant-left {
        display: flex;
        flex-direction: column;
        gap: 2px;
        flex: 1;
        min-width: 0;

        .variant-idx {
          font-size: 12px;
          color: #1677ff;
          font-weight: 700;
        }

        .variant-desc {
          font-size: 13px;
          color: #334155;
          line-height: 1.5;
        }
      }
    }
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
