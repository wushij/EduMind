<template>
  <ProfilePageHero
    eyebrow="AI 教学赋能 · 智能组卷与试卷编排"
    title="AI 智能组卷与试卷编排"
    subtitle="基于课程知识考点与教学难度要求，支持课程题库优选抽选与大模型原创命题缺口补全，快速编排标准化试卷。"
  >
    <template #actions>
      <div class="hero-action-row">
        <el-button
          round
          class="hero-pill-btn is-outline"
          @click="router.push('/question/exams')"
        >
          <el-icon><Document /></el-icon>
          <span>试卷库归档</span>
        </el-button>
        <el-button
          round
          class="hero-pill-btn is-primary"
          @click="$emit('back')"
        >
          <el-icon><Back /></el-icon>
          <span>返回 AI 广场</span>
        </el-button>
      </div>

      <div class="hero-quota-badge-row">
        <div class="quota-pill is-success">
          <span class="quota-dot" />
          <span>智能组卷引擎就绪</span>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="banner-footer-stats-row">
        <div class="stat-capsule">
          <span class="stat-dot" />
          <span class="stat-label">组卷模式：</span>
          <strong class="stat-value">课程题库优选抽选 + 大模型原创补齐</strong>
        </div>
        <div class="stat-capsule">
          <span class="stat-dot is-blue" />
          <span class="stat-label">题目微调：</span>
          <strong class="stat-value">支持在线预览、实时换题与题干编辑</strong>
        </div>
      </div>
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Document, Back } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';

const router = useRouter();

defineEmits<{
  (e: 'back'): void;
}>();
</script>

<style scoped lang="scss">
.hero-action-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hero-quota-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  margin-top: 8px;
}

.quota-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 9999px; // 长圆胶囊边框
  font-size: 12.5px;
  font-weight: 600;
  border: 1px solid transparent;

  .quota-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  &.is-success {
    background: #ecfdf5;
    color: #059669;
    border-color: #a7f3d0;

    .quota-dot {
      background: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
    }
  }
}

.quota-percent-pill {
  padding: 6px 16px;
  border-radius: 9999px; // 长圆胶囊边框
  background: #ffffff;
  border: 1px solid #e2e8f0;
  font-size: 12.5px;
  color: #64748b;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);

  strong {
    color: #2563eb;
    font-weight: 700;
  }
}

.banner-footer-stats-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 14px;
  border-top: 1px dashed #e2e8f0;

  .stat-capsule {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    border-radius: 9999px; // 长圆胶囊边框
    background: rgba(255, 255, 255, 0.85);
    border: 1px solid #e2e8f0;
    font-size: 12.5px;
    color: #475569;

    .stat-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #10b981;

      &.is-blue { background: #3b82f6; }
      &.is-purple { background: #8b5cf6; }
    }

    .stat-value {
      color: #0f172a;
      font-weight: 600;
    }
  }
}
</style>
