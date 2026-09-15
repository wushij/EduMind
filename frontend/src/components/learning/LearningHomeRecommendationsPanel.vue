<template>
<div class="learning-right-col">
        <div class="section-card">
          <div class="section-card-header">
            <div class="header-left">
              <el-icon class="header-icon icon-target"><Aim /></el-icon>
              <h3 class="header-title">今日 AI 匹配度最高推荐</h3>
            </div>
            <button
              type="button"
              class="capsule-action-link"
              @click="router.push('/learning/recommendations')"
            >
              <span>更多推荐</span>
              <el-icon class="action-icon"><Right /></el-icon>
            </button>
          </div>

          <div class="recommended-cards-stack">
            <RecommendationCard
              v-for="item in topRecommendations"
              :key="item.id"
              :item="item"
              @start="emit('start-recommendation', $event)"
              @discuss="emit('discuss-ai', $event)"
            />
          </div>
        </div>
      </div>
</template>

<script setup lang="ts">
import { Aim, Right } from '@element-plus/icons-vue';
import RecommendationCard from '@/components/learning/RecommendationCard.vue';
import type { RecommendationItem } from '@/types/learning/recommendation';
import { useRouter } from 'vue-router';

defineProps<{
  topRecommendations: RecommendationItem[];
}>();

const emit = defineEmits<{
  'start-recommendation': [item: RecommendationItem];
  'discuss-ai': [item: RecommendationItem];
}>();

const router = useRouter();
</script>

<style scoped lang="scss">
.learning-right-col {
  .section-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 22px 24px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    margin-bottom: 20px;
      .section-card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 18px;

        .header-left {
          display: flex;
          align-items: center;
          gap: 8px;

          .header-icon {
            font-size: 19px;
            display: inline-flex;
            align-items: center;
            justify-content: center;

            &.icon-warning {
              color: #EF4444;
            }

            &.icon-tasks {
              color: #2563EB;
            }

            &.icon-target {
              color: #EC4899;
            }
          }

          .header-title {
            margin: 0;
            font-size: 16px;
            font-weight: 700;
            color: #0F172A;
          }

          .capsule-alert-pill {
            padding: 2px 10px;
            border-radius: 9999px;
            background: #FEF2F2;
            border: 1px solid #FECACA;
            color: #DC2626;
            font-size: 11px;
            font-weight: 600;
          }

          .capsule-sub-badge {
            padding: 2px 10px;
            border-radius: 9999px;
            background: #EFF6FF;
            color: #1677FF;
            font-size: 11px;
            font-weight: 600;
          }
        }

        .capsule-action-link {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background: transparent;
          border: none;
          color: #1677FF;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: color 0.2s;

          .action-icon {
            font-size: 14px;
          }

          &:hover {
            color: #0958D9;
            text-decoration: underline;
          }
        }
      }


      // 推荐卡片堆叠
      .recommended-cards-stack {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }
  }
}
</style>
