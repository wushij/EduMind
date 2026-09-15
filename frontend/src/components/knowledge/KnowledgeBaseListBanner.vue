<template>
  <div class="knowledge-banner-stage">
    <div class="banner-ratio-box">
      <img
        class="banner-image"
        :src="kbBannerImg"
        alt="知识库管理"
        draggable="false"
      />
      <div class="banner-float-actions">
        <button
          type="button"
          class="capsule-nav-btn capsule-nav-btn--primary"
          @click="emit('create')"
        >
          <el-icon><Plus /></el-icon>
          <span>新建知识库</span>
        </button>
      </div>
      <div class="banner-stats-dock">
        <div class="stat-item-pill">
          <el-icon class="pill-icon pill-icon--blue"><Collection /></el-icon>
          <span class="pill-label">知识库总数：</span>
          <strong class="pill-val">{{ knowledgeBaseCount }} 个</strong>
        </div>

        <div class="stat-item-pill">
          <el-icon class="pill-icon pill-icon--emerald"><Document /></el-icon>
          <span class="pill-label">入库文档规模：</span>
          <strong class="pill-val">{{ totalDocs }} 篇</strong>
        </div>

        <div class="stat-item-pill">
          <el-icon class="pill-icon pill-icon--amber"><Coin /></el-icon>
          <span class="pill-label">向量切片总量：</span>
          <strong class="pill-val">{{ totalChunks }} 个</strong>
        </div>

        <div class="stat-item-pill stat-item-pill--green">
          <span class="status-pulse-dot"></span>
          <span class="pill-label">Milvus / PgVector 索引状态：</span>
          <strong class="pill-val">正常运行 (99.4% 命中率)</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Collection, Document, Coin, Plus } from '@element-plus/icons-vue';

defineProps<{
  kbBannerImg: string;
  knowledgeBaseCount: number;
  totalDocs: number;
  totalChunks: number;
}>();

const emit = defineEmits<{
  create: [];
}>();
</script>

<style scoped lang="scss">
.knowledge-banner-stage {
  width: 100%;
  margin-bottom: 2px;

  .banner-ratio-box {
    position: relative;
    width: 100%;
    aspect-ratio: 2508 / 627;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 6px 24px rgba(22, 119, 255, 0.08);
    border: 1px solid #E2E8F0;

    .banner-image {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      user-select: none;
    }

    .banner-float-actions {
      position: absolute;
      top: 20px;
      right: 24px;
      z-index: 2;

      .capsule-nav-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 20px;
        border-radius: 9999px;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 4px 16px rgba(22, 119, 255, 0.35);
        transition: all 0.2s ease;

        &:hover {
          background: #0958d9;
          transform: translateY(-1px);
        }
      }
    }

    .banner-stats-dock {
      position: absolute;
      left: 4.4%;
      top: 84%;
      z-index: 2;
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: nowrap;
      max-width: 92%;

      .stat-item-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 34px;
        padding: 0 14px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.92);
        backdrop-filter: blur(8px);
        -webkit-backdrop-filter: blur(8px);
        border: 1px solid rgba(255, 255, 255, 0.85);
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.08);
        font-size: 12px;
        color: #475569;
        white-space: nowrap;
        flex-shrink: 0;
        transition: all 0.2s ease;

        &:hover {
          background: #FFFFFF;
          box-shadow: 0 6px 18px rgba(22, 119, 255, 0.14);
          transform: translateY(-1px);
        }

        .pill-icon {
          font-size: 14px;
          display: inline-flex;
          align-items: center;
          justify-content: center;

          &--blue { color: #2563EB; }
          &--emerald { color: #059669; }
          &--amber { color: #D97706; }
        }

        .pill-val {
          color: #0F172A;
          font-weight: 700;
        }

        &--green {
          background: rgba(240, 253, 244, 0.95);
          border-color: rgba(167, 243, 208, 0.9);
          color: #059669;
          box-shadow: 0 4px 14px rgba(16, 185, 129, 0.1);

          .pill-val {
            color: #059669;
          }

          .status-pulse-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #10B981;
            box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
          }
        }
      }
    }

    @media (max-width: 1400px) {
      .banner-stats-dock {
        top: 83%;
        gap: 8px;

        .stat-item-pill {
          height: 30px;
          padding: 0 10px;
          font-size: 11.5px;

          .pill-icon {
            font-size: 13px;
          }
        }
      }
    }

    @media (max-width: 1100px) {
      .banner-stats-dock {
        top: 81.5%;
        gap: 6px;

        .stat-item-pill {
          height: 28px;
          padding: 0 8px;
          font-size: 11px;
        }
      }
    }
  }
}
</style>
