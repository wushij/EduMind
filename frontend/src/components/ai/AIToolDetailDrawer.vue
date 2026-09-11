<template>
  <el-drawer
    v-model="visible"
    :title="tool?.name || 'AI 工具详情'"
    size="460px"
    direction="rtl"
    class="ai-tool-drawer"
  >
    <div v-if="tool" class="drawer-content">
      <!-- 头部信息 -->
      <div class="drawer-header-box">
        <div class="drawer-icon-box" :style="{ background: tool.iconBg }">
          <span class="drawer-emoji">{{ tool.iconEmoji }}</span>
        </div>
        <div class="drawer-header-meta">
          <h3 class="drawer-title">{{ tool.name }}</h3>
          <div class="drawer-tags-row">
            <span class="pill-badge pill-badge--cat">{{ tool.categoryLabel }}</span>
            <span v-if="tool.isRecommended" class="pill-badge pill-badge--hot">平台精选</span>
            <span class="stats-text">⭐ {{ tool.rating }} 评分</span>
          </div>
        </div>
      </div>

      <!-- 功能详述 -->
      <div class="drawer-section">
        <h4 class="section-title">功能场景与特点</h4>
        <p class="section-p">{{ tool.detailedIntro || tool.description }}</p>
      </div>

      <!-- 适用学科范围 -->
      <div class="drawer-section">
        <h4 class="section-title">适用场景与标签</h4>
        <div class="drawer-tags-grid">
          <span v-for="tag in tool.tags" :key="tag" class="pill-tag-large">
            # {{ tag }}
          </span>
        </div>
      </div>

      <!-- 预期产出模式 -->
      <div class="drawer-section">
        <h4 class="section-title">AI 驱动模型与交互模式</h4>
        <div class="feature-item">
          <span class="feature-bullet">⚡</span>
          <div class="feature-info">
            <strong>DeepSeek-R1 / V3 教学大模型加速</strong>
            <p>针对理工与文史高教题目深度微调，具备思维链推理与公式逆向解析能力。</p>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-bullet">🔒</span>
          <div class="feature-info">
            <strong>高校私域知识库隔离</strong>
            <p>出题与批改严格限定在课程课件与指定知识点大纲内，保障学术合规。</p>
          </div>
        </div>
      </div>

      <!-- 底部启动按钮 (长圆跑道) -->
      <div class="drawer-footer">
        <button type="button" class="capsule-launch-btn" @click="handleLaunch">
          <span>立即启动使用该工具</span>
          <span>→</span>
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { AITool } from '@/types/ai/tool';

const props = defineProps<{
  modelValue: boolean;
  tool?: AITool | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
}>();

const router = useRouter();

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
});

function handleLaunch() {
  if (props.tool) {
    visible.value = false;
    router.push(props.tool.route);
  }
}
</script>

<style scoped lang="scss">
.ai-tool-drawer {
  .drawer-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
    padding: 10px 4px 40px;

    .drawer-header-box {
      display: flex;
      align-items: center;
      gap: 16px;
      padding-bottom: 20px;
      border-bottom: 1px solid #F1F5F9;

      .drawer-icon-box {
        width: 56px;
        height: 56px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);

        .drawer-emoji {
          font-size: 26px;
        }
      }

      .drawer-header-meta {
        .drawer-title {
          margin: 0 0 6px 0;
          font-size: 18px;
          font-weight: 700;
          color: #0F172A;
        }

        .drawer-tags-row {
          display: flex;
          align-items: center;
          gap: 8px;

          .pill-badge {
            padding: 2px 10px;
            border-radius: 9999px; // 长圆药丸
            font-size: 11.5px;
            font-weight: 600;

            &--cat {
              background: #EFF6FF;
              color: #1677FF;
            }

            &--hot {
              background: #FEE2E2;
              color: #EF4444;
            }
          }

          .stats-text {
            font-size: 12px;
            color: #94A3B8;
          }
        }
      }
    }

    .drawer-section {
      .section-title {
        margin: 0 0 10px 0;
        font-size: 14.5px;
        font-weight: 600;
        color: #1E293B;
      }

      .section-p {
        margin: 0;
        font-size: 13.5px;
        line-height: 1.7;
        color: #475569;
      }

      .drawer-tags-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .pill-tag-large {
          padding: 4px 12px;
          border-radius: 9999px; // 长圆
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          font-size: 12.5px;
          color: #475569;
        }
      }

      .feature-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px 14px;
        background: #F8FAFC;
        border-radius: 12px;
        margin-top: 10px;
        border: 1px solid #F1F5F9;

        .feature-bullet {
          font-size: 16px;
        }

        .feature-info {
          strong {
            font-size: 13px;
            color: #1E293B;
          }
          p {
            margin: 2px 0 0 0;
            font-size: 12px;
            color: #64748B;
            line-height: 1.4;
          }
        }
      }
    }

    .drawer-footer {
      margin-top: 20px;

      .capsule-launch-btn {
        width: 100%;
        height: 44px;
        border-radius: 9999px; // 纯正长圆胶囊
        background: #1677FF;
        color: #FFFFFF;
        border: none;
        font-size: 14.5px;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);
        transition: all 0.22s ease;

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
        }
      }
    }
  }
}
</style>
