<template>
  <PageHeroBanner
    title="智能错题本 · 认知归因与变式攻坚"
    subtitle="基于 AI 知识图谱与认知诊断模型，精准定位失分根本原因，一键生成同构变式题定向攻坚"
    background-variant="learning"
    size="large"
  >
    <template #toolbar>
      <el-select
        v-if="courseOptions.length"
        :model-value="selectedCourseId"
        placeholder="选择关联课程"
        style="width: 220px"
        @change="emit('course-change', $event)"
      >
        <el-option
          v-for="c in courseOptions"
          :key="c.id"
          :label="c.name || (c as any).title || ('课程 #' + c.id)"
          :value="c.id"
        />
      </el-select>
      <button type="button" class="capsule-btn capsule-btn--default" @click="router.push('/learning/practice')">
        <el-icon class="btn-icon"><MagicStick /></el-icon>
        <span>AI 练习</span>
      </button>
      <button type="button" class="capsule-btn capsule-btn--primary" @click="emit('batch-practice')">
        <el-icon class="btn-icon"><Lightning /></el-icon>
        <span>错题变式攻坚</span>
      </button>
    </template>

    <template #extra>
      <div class="hero-stats-row">
        <div class="hero-stat-card">
          <span class="stat-num text-danger">{{ overview.pendingCount }}</span>
          <span class="stat-label">待攻坚错题</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-warning">{{ overview.weakKnowledgePointCount }}</span>
          <span class="stat-label">薄弱知识考点</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-success">{{ overview.masteredCount }}</span>
          <span class="stat-label">已攻克题量</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-primary">{{ overview.variantConquerRatePercent }}%</span>
          <span class="stat-label">变式攻克率</span>
        </div>
      </div>
    </template>
  </PageHeroBanner>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Lightning, MagicStick } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import type { WrongBookOverviewVO } from '@/types/learning/wrong-question';

defineProps<{
  courseOptions: Array<{ id: number; name: string }>;
  selectedCourseId: number;
  overview: WrongBookOverviewVO;
}>();

const emit = defineEmits<{
  'course-change': [courseId: number];
  'batch-practice': [];
}>();

const router = useRouter();
</script>

<style scoped lang="scss">
@use './wrong-book-shared.scss' as *;
</style>
