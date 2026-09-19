<template>
  <el-drawer
    :model-value="visible"
    title="知识点详情"
    size="480px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <template v-if="kp">
      <div class="detail-header">
        <div class="detail-badges">
          <span v-if="kp.code" class="code-pill">{{ kp.code }}</span>
          <el-tag size="small" :type="levelTagType">{{ levelLabel }}</el-tag>
        </div>
        <h2 class="detail-title">{{ kp.title || kp.name }}</h2>
        <p class="detail-chapter">
          <el-icon><Reading /></el-icon>
          {{ chapterTitle }}
        </p>
      </div>

      <section class="detail-block">
        <h4>考点说明</h4>
        <p v-if="kp.description" class="detail-text">{{ kp.description }}</p>
        <p v-else class="detail-empty">暂无说明</p>
      </section>

      <section v-if="kp.examFocus" class="detail-block">
        <h4>考查重点 / 易错点</h4>
        <p class="detail-text detail-text--warn">{{ kp.examFocus }}</p>
      </section>

      <section v-if="kp.prerequisites?.length" class="detail-block">
        <h4>前置知识点</h4>
        <div class="chip-row">
          <span v-for="pre in kp.prerequisites" :key="pre.id" class="chip">{{ pre.title }}</span>
        </div>
      </section>

      <div v-if="editable" class="detail-actions">
        <el-button type="primary" class="capsule-btn" @click="$emit('edit')">
          <el-icon><EditPen /></el-icon>
          编辑考点
        </el-button>
        <el-button class="capsule-btn" @click="$emit('ask-ai')">
          <el-icon><MagicStick /></el-icon>
          AI 解析
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { Reading, EditPen, MagicStick } from '@element-plus/icons-vue';
import type { KnowledgePoint } from '@/types/course/knowledge-point';

defineProps<{
  visible: boolean;
  kp: KnowledgePoint | null;
  chapterTitle: string;
  levelLabel: string;
  levelTagType: string;
  editable?: boolean;
}>();

defineEmits<{
  'update:visible': [boolean];
  edit: [];
  'ask-ai': [];
}>();
</script>

<style scoped lang="scss">
.detail-header {
  margin-bottom: 20px;

  .detail-badges {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
  }

  .code-pill {
    font-size: 11px;
    background: #f1f5f9;
    padding: 2px 8px;
    border-radius: 4px;
    color: #64748b;
  }

  .detail-title {
    margin: 0 0 8px;
    font-size: 20px;
    font-weight: 700;
    color: #0f172a;
  }

  .detail-chapter {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #64748b;
    margin: 0;
  }
}

.detail-block {
  margin-bottom: 18px;

  h4 {
    margin: 0 0 8px;
    font-size: 13px;
    font-weight: 600;
    color: #334155;
  }

  .detail-text {
    margin: 0;
    font-size: 14px;
    line-height: 1.65;
    color: #475569;

    &--warn {
      color: #b45309;
      background: #fffbeb;
      padding: 10px 12px;
      border-radius: 8px;
      border: 1px solid #fde68a;
    }
  }

  .detail-empty {
    margin: 0;
    font-size: 13px;
    color: #94a3b8;
    font-style: italic;
  }

  .chip-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .chip {
      font-size: 12px;
      padding: 4px 10px;
      border-radius: 9999px;
      background: #eff6ff;
      color: #1d4ed8;
    }
  }
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.capsule-btn {
  border-radius: 9999px;
}
</style>
