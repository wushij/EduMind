<template>
  <section class="bank-stats-grid">
    <!-- 指标卡 1：收录试题总量与构成 -->
    <div class="stat-card stat-card--blue">
      <div class="stat-card__icon-box">
        <el-icon><Document /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">收录试题总量</span>
        <div class="stat-card__value-row">
          <strong class="stat-card__num">{{ questionCount }}</strong>
          <span class="stat-card__unit">道题目</span>
        </div>
        <div class="stat-card__sub-hint">
          <span v-if="typeSummary" class="type-summary-text">{{ typeSummary }}</span>
          <span v-else>覆盖本课程高频考点</span>
        </div>
      </div>
    </div>

    <!-- 指标卡 2：卷面参考总分与测验标准 -->
    <div class="stat-card stat-card--emerald">
      <div class="stat-card__icon-box">
        <el-icon><Tickets /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">卷面参考总分</span>
        <div class="stat-card__value-row">
          <strong class="stat-card__num">{{ totalScore }}</strong>
          <span class="stat-card__unit">分</span>
        </div>
        <div class="stat-card__sub-hint">
          <span>及格基准线约 <strong>{{ Math.round(totalScore * 0.6) }}</strong> 分</span>
        </div>
      </div>
    </div>

    <!-- 指标卡 3：试题难度结构分析 -->
    <div class="stat-card stat-card--amber">
      <div class="stat-card__icon-box">
        <el-icon><DataAnalysis /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">综合难度系数</span>
        <div class="stat-card__value-row">
          <strong class="stat-card__num text-amber-600">{{ difficultyLevelText }}</strong>
        </div>
        <div class="stat-card__sub-hint">
          <span>{{ difficultyBreakdown }}</span>
        </div>
      </div>
    </div>

    <!-- 指标卡 4：课程体系与交付就绪 -->
    <div class="stat-card stat-card--indigo">
      <div class="stat-card__icon-box">
        <el-icon><CircleCheck /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">题库交付状态</span>
        <div class="stat-card__value-row">
          <span class="ready-badge">就绪可用</span>
        </div>
        <div class="stat-card__sub-hint">
          <span>支持一键抽题、快捷组卷与导出</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Document, Tickets, DataAnalysis, CircleCheck } from '@element-plus/icons-vue';
import type { QuestionItem } from '@/types/question/question';

const props = withDefaults(
  defineProps<{
    questionCount: number;
    totalScore: number;
    questions?: QuestionItem[];
    courseName?: string;
  }>(),
  {
    questions: () => [],
    courseName: ''
  }
);

const typeSummary = computed(() => {
  if (!props.questions || props.questions.length === 0) return '';
  const single = props.questions.filter(q => q.type === 'SINGLE_CHOICE').length;
  const multi = props.questions.filter(q => q.type === 'MULTIPLE_CHOICE').length;
  const tf = props.questions.filter(q => q.type === 'TRUE_FALSE' || (q.type as any) === 'JUDGMENT').length;
  const parts: string[] = [];
  if (single > 0) parts.push(`单选 ${single}`);
  if (multi > 0) parts.push(`多选 ${multi}`);
  if (tf > 0) parts.push(`判断 ${tf}`);
  const other = props.questions.length - (single + multi + tf);
  if (other > 0) parts.push(`主观 ${other}`);
  return parts.join(' · ');
});

const difficultyLevelText = computed(() => {
  if (!props.questions || props.questions.length === 0) return '标准适中';
  const easy = props.questions.filter(q => q.difficulty === 'EASY').length;
  const hard = props.questions.filter(q => q.difficulty === 'HARD').length;
  if (hard > easy) return '高阶进阶';
  if (easy > hard) return '基础巩固';
  return '标准适中';
});

const difficultyBreakdown = computed(() => {
  if (!props.questions || props.questions.length === 0) return '题型比例均衡';
  const easy = props.questions.filter(q => q.difficulty === 'EASY').length;
  const med = props.questions.filter(q => q.difficulty === 'MEDIUM' || !q.difficulty).length;
  const hard = props.questions.filter(q => q.difficulty === 'HARD').length;
  return `基础 ${easy} · 适中 ${med} · 难点 ${hard}`;
});
</script>

<style scoped lang="scss">
.bank-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  width: 100%;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 16px;
    border-radius: 14px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
    transition: all 0.2s ease;

    &:hover {
      border-color: #cbd5e1;
      transform: translateY(-1px);
      box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
    }

    &__icon-box {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .el-icon {
        font-size: 20px;
      }
    }

    &__content {
      display: flex;
      flex-direction: column;
      min-width: 0;
      flex: 1;
    }

    &__label {
      font-size: 12px;
      color: #64748b;
      font-weight: 500;
      line-height: 1.3;
    }

    &__value-row {
      display: flex;
      align-items: baseline;
      gap: 6px;
      margin-top: 2px;
    }

    &__num {
      font-size: 20px;
      font-weight: 800;
      color: #0f172a;
      line-height: 1.2;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }

    &__unit {
      font-size: 12px;
      color: #64748b;
      font-weight: 600;
    }

    &__sub-hint {
      font-size: 11.5px;
      color: #94a3b8;
      margin-top: 2px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      strong {
        color: #475569;
      }
    }

    .ready-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 8px;
      border-radius: 9999px;
      background: #ecfdf5;
      color: #059669;
      font-size: 12px;
      font-weight: 700;
      border: 1px solid #a7f3d0;
    }

    /* 各卡片主题强调色 */
    &--blue {
      .stat-card__icon-box {
        background: #eff6ff;
        color: #2563eb;
        border: 1px solid #bfdbfe;
      }
    }

    &--emerald {
      .stat-card__icon-box {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
      }
    }

    &--amber {
      .stat-card__icon-box {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid #fde68a;
      }
    }

    &--indigo {
      .stat-card__icon-box {
        background: #f5f3ff;
        color: #7c3aed;
        border: 1px solid #ddd6fe;
      }
    }
  }
}
</style>
