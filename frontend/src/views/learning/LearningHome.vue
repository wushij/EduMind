<template>
  <div class="learning-home-container">
    <PageHeroBanner
      title="我的学习"
      subtitle="基于学习轨迹与知识掌握情况，为你推荐今日任务与巩固练习"
      :background-image="learningBannerImg"
      background-variant="learning"
      :show-illustration="false"
    >
      <template #extra>
        <div class="learning-hero-stats">
          <div class="hero-stat-pill">
            <span class="stat-label">课程进度</span>
            <strong class="stat-value">68%</strong>
          </div>
          <div class="hero-stat-pill">
            <span class="stat-label">学习时长</span>
            <strong class="stat-value">14.5 小时</strong>
          </div>
          <div class="hero-stat-pill">
            <span class="stat-label">完成任务</span>
            <strong class="stat-value">12 / 18</strong>
          </div>
          <div class="hero-stat-pill">
            <span class="stat-label">知识点掌握度</span>
            <strong class="stat-value">88.5%</strong>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="learning-main-split">
      <LearningHomeLeftColumn
        :weak-points="weakPoints"
        :today-tasks="todayTasks"
        @generate-weak="handleGenerateWeakQuestions"
        @study-point="handleStudyPoint"
        @execute-task="handleExecuteTask"
      />
      <LearningHomeRecommendationsPanel
        :top-recommendations="topRecommendations"
        @start-recommendation="handleStartRecommendation"
        @discuss-ai="handleDiscussAI"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import LearningHomeLeftColumn from '@/components/learning/LearningHomeLeftColumn.vue';
import LearningHomeRecommendationsPanel from '@/components/learning/LearningHomeRecommendationsPanel.vue';
import { useRecommendations } from '@/composables/learning/useRecommendations';
import type { RecommendationItem } from '@/types/learning/recommendation';
import learningBannerImg from '@/assets/images/学习中心banner.png';

const router = useRouter();
const { items, fetchRecommendations } = useRecommendations();

onMounted(() => {
  fetchRecommendations();
});

const weakPoints = ref([
  {
    id: 'wp_1',
    name: '洛必达法则未定式适用前提与条件',
    course: '高等数学（上）',
    mastery: 42,
    level: 'danger',
    reason: '阶段自测中常与变上限积分求导结合出错，存在“0/0 未定式先验验证不足”问题。'
  },
  {
    id: 'wp_2',
    name: '泰勒中值定理高阶皮亚诺余项展开',
    course: '高等数学（上）',
    mastery: 56,
    level: 'warning',
    reason: '对于分母为 x^3 时的分子阶数保留判断不熟练，常导致阶数过早截断。'
  },
  {
    id: 'wp_3',
    name: '二叉树非递归遍历显式栈设计',
    course: '数据结构与算法',
    mastery: 64,
    level: 'warning',
    reason: '后序遍历双栈法或单栈标记法的回溯条件逻辑易混淆。'
  }
]);

const todayTasks = ref([
  {
    id: 'task_1',
    title: '完成《洛必达法则未定式专项攻坚》3 道针对性练习',
    course: '高等数学（上）',
    type: '专项攻坚',
    estimatedMinutes: 15,
    completed: true
  },
  {
    id: 'task_2',
    title: '研读微课《微分中值定理的几何本质与辅助函数构造》',
    course: '高等数学（上）',
    type: '核心微课',
    estimatedMinutes: 12,
    completed: false
  },
  {
    id: 'task_3',
    title: '完成数据结构第 4 章二叉树自适应测验',
    course: '数据结构与算法',
    type: '单元测验',
    estimatedMinutes: 20,
    completed: false
  }
]);

const topRecommendations = computed(() => items.value.slice(0, 2));

function handleGenerateWeakQuestions() {
  router.push({
    path: '/ai/question/generate',
    query: {
      subject: '高等数学（上）',
      knowledgePoint: '洛必达法则与未定式极限'
    }
  });
}

function handleStudyPoint(point: { course: string; name: string }) {
  router.push({
    path: '/ai/question/generate',
    query: {
      subject: point.course,
      knowledgePoint: point.name
    }
  });
}

function handleExecuteTask(task: { type: string }) {
  if (task.type === '核心微课') {
    router.push('/learning/recommendations');
  } else {
    router.push('/ai/question/generate');
  }
}

function handleStartRecommendation(item: RecommendationItem) {
  if (item.type === 'exercise') {
    router.push('/ai/question/generate');
  } else {
    router.push('/course/101/resources');
  }
}

function handleDiscussAI(item: RecommendationItem) {
  router.push('/course/101/ai');
}
</script>

<style scoped lang="scss">
.learning-home-container {
  display: flex;
  flex-direction: column;
  gap: 22px;
  width: 100%;

  .learning-hero-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;

    .hero-stat-pill {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 12px 16px;
      border-radius: 14px;
      background: rgba(255, 255, 255, 0.82);
      border: 1px solid rgba(226, 232, 240, 0.8);

      .stat-label {
        font-size: 12.5px;
        color: #64748B;
      }

      .stat-value {
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.2;
      }
    }
  }

  @media (max-width: 1024px) {
    .learning-hero-stats {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  .learning-main-split {
    display: grid;
    grid-template-columns: 58% 42%;
    gap: 20px;
  }
}

@media (max-width: 1024px) {
  .learning-main-split {
    grid-template-columns: 1fr !important;
  }
}
</style>
