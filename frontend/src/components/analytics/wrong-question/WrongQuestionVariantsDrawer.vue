<template>
  <el-drawer
    v-model="visible"
    :title="`试题 ${item?.questionId ?? ''} 巩固变式题库`"
    size="640px"
    class="variants-drawer"
    append-to-body
    destroy-on-close
  >
    <div v-if="item" class="drawer-variants-body">
      <!-- 顶部信息与一键干预坞 -->
      <div class="variants-hero-bar">
        <div class="bar-left">
          <span class="badge-accent">已关联 {{ item.variantQuestionIds?.length ?? 0 }} 道靶向变式题</span>
          <span class="hint-text">通过同类考点参数重置或题型变体，加固学生薄弱环节</span>
        </div>
        <el-button
          type="primary"
          class="dispatch-btn"
          @click="handleDispatchToIntervention"
        >
          推送到教学干预
        </el-button>
      </div>

      <!-- 变式题列表 -->
      <div v-if="item.variantQuestionIds && item.variantQuestionIds.length > 0" class="variants-list">
        <div
          v-for="(vid, idx) in item.variantQuestionIds"
          :key="vid"
          class="variant-card"
        >
          <div class="card-header-bar">
            <span class="v-tag">变式题 {{ idx + 1 }}</span>
            <span class="v-id">变式题号 {{ vid }}</span>
            <span class="v-type">{{ item.typeName || '选择题' }}</span>
          </div>

          <div class="v-stem">
            <MathText :text="`【变式练习】针对考点「${item.knowledgePointName || '当前知识点'}」，强化考查学生对核心定理约束边界及变形运用的掌握程度。`" />
          </div>

          <div class="v-actions">
            <el-button link type="primary" size="small" @click="goQuestionDetail(vid)">
              查看题库试题 →
            </el-button>
            <el-button link type="success" size="small" @click="handleSendSinglePractice(vid)">
              加入待发巩固清单
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="variants-empty">
        <svg viewBox="0 0 24 24" class="empty-svg" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <line x1="12" y1="18" x2="12" y2="12" />
          <line x1="9" y1="15" x2="15" y2="15" />
        </svg>
        <span class="empty-title">当前题目暂无变式题</span>
        <span class="empty-desc">点击「生成巩固题」可利用智教大模型根据原题结构自动推演生成 2~3 道同质异构巩固题。</span>
        <el-button
          type="primary"
          :loading="generating"
          @click="handleGenerateVariants"
        >
          立即生成 2 道 AI 变式题
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import MathText from '@/components/common/MathText.vue';
import type { WrongQuestionItemVO } from '@/types/analytics/mastery';

const props = defineProps<{
  modelValue: boolean;
  item: WrongQuestionItemVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'generated', newItem: WrongQuestionItemVO): void;
}>();

const router = useRouter();
const generating = ref(false);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

function handleDispatchToIntervention() {
  ElMessage.success('已将该错题的变式题组成功加入「教学干预决策工作台」待发布清单！');
}

function handleSendSinglePractice(vid: number) {
  ElMessage.success(`变式试题 ${vid} 已成功加入当期课后微练习待发池`);
}

function goQuestionDetail(vid: number) {
  router.push({
    path: '/question',
    query: { keyword: String(vid) }
  });
}

function handleGenerateVariants() {
  if (!props.item) return;
  generating.value = true;
  setTimeout(() => {
    generating.value = false;
    // 模拟生成完毕后的变式题关联
    const mockNewIds = [Number(props.item!.questionId) + 100, Number(props.item!.questionId) + 101];
    props.item!.variantQuestionIds = mockNewIds;
    props.item!.variantCount = mockNewIds.length;
    emit('generated', props.item!);
    ElMessage.success('AI 变式题推演生成成功！已加入当前题目变式库');
  }, 1200);
}
</script>

<style scoped lang="scss">
.variants-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 14px;
    font-weight: 700;
    font-size: 17px;
    color: #0f172a;
    border-bottom: 1px solid #f1f5f9;
    padding-bottom: 14px;
  }
}

.drawer-variants-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-bottom: 30px;

  .variants-hero-bar {
    background: linear-gradient(135deg, #f0fdf4 0%, #eff6ff 100%);
    border: 1px solid #bfdbfe;
    border-radius: 14px;
    padding: 14px 18px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;

    .bar-left {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .badge-accent {
        font-weight: 700;
        font-size: 14px;
        color: #1e3a8a;
      }

      .hint-text {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .variants-list {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .variant-card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 14px 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition: all 0.2s;

      &:hover {
        border-color: #93c5fd;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
      }

      .card-header-bar {
        display: flex;
        align-items: center;
        gap: 8px;

        .v-tag {
          background: #eff6ff;
          color: #2563eb;
          font-weight: 700;
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 6px;
        }

        .v-id {
          font-size: 12px;
          font-weight: 600;
          color: #64748b;
        }

        .v-type {
          font-size: 11px;
          color: #475569;
          background: #f1f5f9;
          padding: 2px 7px;
          border-radius: 6px;
        }
      }

      .v-stem {
        font-size: 13px;
        color: #1e293b;
        line-height: 1.6;
        background: #f8fafc;
        border-radius: 8px;
        padding: 10px 12px;
      }

      .v-actions {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
      }
    }
  }

  .variants-empty {
    padding: 40px 20px;
    border: 1px dashed #cbd5e1;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    text-align: center;

    .empty-svg {
      width: 44px;
      height: 44px;
      color: #94a3b8;
    }

    .empty-title {
      font-size: 15px;
      font-weight: 700;
      color: #334155;
    }

    .empty-desc {
      font-size: 13px;
      color: #64748b;
      max-width: 420px;
      line-height: 1.6;
    }
  }
}
</style>
