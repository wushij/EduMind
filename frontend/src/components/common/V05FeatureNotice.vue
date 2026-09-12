<template>
  <div class="v05-feature-notice">
    <el-alert
      :title="alertTitle"
      type="warning"
      effect="light"
      show-icon
      :closable="false"
      class="notice-alert"
    />

    <div class="notice-body-card">
      <h2 class="notice-title">{{ title }}</h2>
      <p class="notice-description">{{ description }}</p>

      <div v-if="alternatives.length > 0" class="alternatives-section">
        <h3 class="alt-heading">可先体验以下替代能力</h3>
        <div class="alt-actions">
          <button
            v-for="alt in alternatives"
            :key="alt.route"
            type="button"
            class="alt-btn"
            @click="router.push(alt.route)"
          >
            {{ alt.label }}
          </button>
        </div>
      </div>

      <button type="button" class="back-btn" @click="router.push('/ai/marketplace')">
        返回 AI 工具广场
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { V05Alternative } from '@/types/ai/v05-notice';

const props = defineProps<{
  title: string;
  description: string;
  alternatives?: V05Alternative[];
}>();

const router = useRouter();

const alertTitle = computed(
  () => '【V0.5 规划能力】本工具完整交互与工作台将于 V0.5 正式上线。'
);

const alternatives = computed(() => props.alternatives ?? []);
</script>

<style scoped lang="scss">
.v05-feature-notice {
  max-width: 720px;
  margin: 0 auto;

  .notice-alert {
    margin-bottom: 20px;
  }

  .notice-body-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    padding: 28px 32px;

    .notice-title {
      margin: 0 0 12px;
      font-size: 22px;
      font-weight: 700;
      color: #0f172a;
    }

    .notice-description {
      margin: 0 0 24px;
      font-size: 14px;
      line-height: 1.75;
      color: #475569;
    }

    .alternatives-section {
      margin-bottom: 24px;

      .alt-heading {
        margin: 0 0 12px;
        font-size: 14px;
        font-weight: 600;
        color: #1e293b;
      }

      .alt-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .alt-btn {
          height: 38px;
          padding: 0 18px;
          border-radius: 9999px;
          border: 1px solid #bfdbfe;
          background: #eff6ff;
          color: #1677ff;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            background: #1677ff;
            color: #ffffff;
          }
        }
      }
    }

    .back-btn {
      height: 40px;
      padding: 0 20px;
      border-radius: 9999px;
      border: 1px solid #e2e8f0;
      background: #f8fafc;
      color: #475569;
      font-size: 13px;
      cursor: pointer;

      &:hover {
        color: #1677ff;
        border-color: #bfdbfe;
      }
    }
  }
}
</style>
