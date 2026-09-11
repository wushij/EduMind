<template>
  <div class="learning-home-container">
    <!-- 1. 顶部 Hero Banner（对齐原型 §十五 学习中心） -->
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

    <!-- 2. 主体工作区分栏：左侧薄弱考点强化与今日任务，右侧精选推荐 -->
    <div class="learning-main-split">
      <!-- 左列：薄弱考点 AI 诊断 + 今日学习任务 -->
      <div class="learning-left-col">
        <!-- A. 薄弱知识点诊断预警 (AI 图谱驱动) -->
        <div class="section-card">
          <div class="section-card-header">
            <div class="header-left">
              <span class="header-icon">⚠️</span>
              <h3 class="header-title">AI 考点诊断预警与薄弱项攻坚</h3>
              <span class="capsule-alert-pill">急需强化 3 项</span>
            </div>
            <button
              type="button"
              class="capsule-action-link"
              @click="handleGenerateWeakQuestions"
            >
              <span>✨ 一键 AI 生成巩固练习</span>
            </button>
          </div>

          <div class="weakness-points-list">
            <div
              v-for="point in weakPoints"
              :key="point.id"
              class="weakness-point-item"
            >
              <div class="point-main">
                <div class="point-title-row">
                  <span class="point-name">{{ point.name }}</span>
                  <span class="point-course-tag">{{ point.course }}</span>
                  <span class="point-mastery-tag" :class="`point-mastery-tag--${point.level}`">
                    掌握度 {{ point.mastery }}%
                  </span>
                </div>
                <p class="point-diagnose-reason">{{ point.reason }}</p>
                <div class="capsule-progress-track">
                  <div
                    class="capsule-progress-fill"
                    :class="`fill-${point.level}`"
                    :style="{ width: `${point.mastery}%` }"
                  ></div>
                </div>
              </div>

              <div class="point-action">
                <button
                  type="button"
                  class="capsule-weak-btn"
                  @click="handleStudyPoint(point)"
                >
                  <span>立即强化</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- B. 今日推荐学习任务清单 -->
        <div class="section-card">
          <div class="section-card-header">
            <div class="header-left">
              <span class="header-icon">📋</span>
              <h3 class="header-title">今日自适应学习计划</h3>
              <span class="capsule-sub-badge">完成率 1/3</span>
            </div>
          </div>

          <div class="today-tasks-list">
            <div
              v-for="task in todayTasks"
              :key="task.id"
              class="task-row-card"
              :class="{ completed: task.completed }"
            >
              <div class="task-checkbox-col" @click="task.completed = !task.completed">
                <span class="checkbox-circle" :class="{ checked: task.completed }">
                  {{ task.completed ? '✓' : '' }}
                </span>
              </div>

              <div class="task-content-col">
                <div class="task-top">
                  <span class="task-title" :class="{ 'line-through': task.completed }">
                    {{ task.title }}
                  </span>
                  <span class="task-time-pill">⏱️ {{ task.estimatedMinutes }} 分钟</span>
                </div>
                <div class="task-meta">
                  <span class="task-tag">{{ task.course }}</span>
                  <span class="task-type-tag">{{ task.type }}</span>
                </div>
              </div>

              <div class="task-action-col">
                <button
                  type="button"
                  class="capsule-task-btn"
                  :class="{ 'capsule-task-btn--done': task.completed }"
                  @click="handleExecuteTask(task)"
                >
                  <span>{{ task.completed ? '已完成' : '去完成 →' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右列：AI 专属推荐练习与资料精选 -->
      <div class="learning-right-col">
        <div class="section-card">
          <div class="section-card-header">
            <div class="header-left">
              <span class="header-icon">🎯</span>
              <h3 class="header-title">今日 AI 匹配度最高推荐</h3>
            </div>
            <button
              type="button"
              class="capsule-action-link"
              @click="router.push('/learning/recommendations')"
            >
              更多推荐 →
            </button>
          </div>

          <div class="recommended-cards-stack">
            <RecommendationCard
              v-for="item in topRecommendations"
              :key="item.id"
              :item="item"
              @start="handleStartRecommendation"
              @discuss="handleDiscussAI"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import RecommendationCard from '@/components/learning/RecommendationCard.vue';
import { useRecommendations } from '@/composables/learning/useRecommendations';
import type { RecommendationItem } from '@/types/learning/recommendation';
import learningBannerImg from '@/assets/images/学习中心banner.png';

const router = useRouter();
const { items, fetchRecommendations } = useRecommendations();

onMounted(() => {
  fetchRecommendations();
});

// 薄弱考点数据
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

// 今日任务
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

function handleStudyPoint(point: any) {
  router.push({
    path: '/ai/question/generate',
    query: {
      subject: point.course,
      knowledgePoint: point.name
    }
  });
}

function handleExecuteTask(task: any) {
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

  // 1. Hero 动作坞
  .learning-hero-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 20px;
      border-radius: 9999px; // 长圆跑道按钮
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;

      &--primary {
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.3);

        &:hover {
          background: #4096FF;
          transform: translateY(-2px);
        }
      }

      &--ai {
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        box-shadow: 0 3px 12px rgba(114, 46, 209, 0.3);

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 18px rgba(114, 46, 209, 0.4);
        }
      }

      &--secondary {
        background: #FFFFFF;
        color: #334155;
        border: 1px solid #CBD5E1;

        &:hover {
          background: #F8FAFC;
          color: #1677FF;
          border-color: #93C5FD;
        }
      }
    }
  }

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

  // 2. 主体分栏
  .learning-main-split {
    display: grid;
    grid-template-columns: 58% 42%;
    gap: 20px;

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
            font-size: 18px;
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
          background: transparent;
          border: none;
          color: #1677FF;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: color 0.2s;

          &:hover {
            color: #0958D9;
            text-decoration: underline;
          }
        }
      }

      // 薄弱考点列表
      .weakness-points-list {
        display: flex;
        flex-direction: column;
        gap: 14px;

        .weakness-point-item {
          display: flex;
          align-items: center;
          gap: 16px;
          padding: 14px 16px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 14px;
          transition: all 0.2s ease;

          &:hover {
            background: #FFFFFF;
            border-color: #93C5FD;
            box-shadow: 0 4px 14px rgba(22, 119, 255, 0.08);
          }

          .point-main {
            flex: 1;
            min-width: 0;

            .point-title-row {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 6px;
              flex-wrap: wrap;

              .point-name {
                font-size: 13.5px;
                font-weight: 700;
                color: #0F172A;
              }

              .point-course-tag {
                padding: 1px 8px;
                border-radius: 9999px;
                background: #F1F5F9;
                color: #475569;
                font-size: 11px;
              }

              .point-mastery-tag {
                padding: 1px 8px;
                border-radius: 9999px;
                font-size: 11px;
                font-weight: 600;

                &--danger {
                  background: #FEF2F2;
                  color: #DC2626;
                }

                &--warning {
                  background: #FFFBEB;
                  color: #D97706;
                }
              }
            }

            .point-diagnose-reason {
              margin: 0 0 8px 0;
              font-size: 12px;
              color: #64748B;
              line-height: 1.5;
            }

            .capsule-progress-track {
              width: 100%;
              height: 5px;
              background: #E2E8F0;
              border-radius: 9999px;
              overflow: hidden;

              .capsule-progress-fill {
                height: 100%;
                border-radius: 9999px;

                &.fill-danger {
                  background: linear-gradient(90deg, #EF4444 0%, #F87171 100%);
                }

                &.fill-warning {
                  background: linear-gradient(90deg, #F59E0B 0%, #FBBF24 100%);
                }
              }
            }
          }

          .point-action {
            flex-shrink: 0;

            .capsule-weak-btn {
              height: 32px;
              padding: 0 16px;
              border-radius: 9999px; // 纯正长圆
              background: #EFF6FF;
              border: 1px solid #BFDBFE;
              color: #1677FF;
              font-size: 12px;
              font-weight: 600;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                background: #1677FF;
                color: #FFFFFF;
                border-color: #1677FF;
              }
            }
          }
        }
      }

      // 今日任务列表
      .today-tasks-list {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .task-row-card {
          display: flex;
          align-items: center;
          gap: 14px;
          padding: 12px 16px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 12px;
          transition: all 0.2s ease;

          &.completed {
            opacity: 0.7;
            background: #F1F5F9;
          }

          .task-checkbox-col {
            cursor: pointer;

            .checkbox-circle {
              width: 20px;
              height: 20px;
              border-radius: 50%;
              border: 2px solid #CBD5E1;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 12px;
              font-weight: 700;
              color: #FFFFFF;
              transition: all 0.2s;

              &.checked {
                background: #10B981;
                border-color: #10B981;
              }
            }
          }

          .task-content-col {
            flex: 1;
            min-width: 0;

            .task-top {
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 4px;

              .task-title {
                font-size: 13px;
                font-weight: 600;
                color: #1E293B;

                &.line-through {
                  text-decoration: line-through;
                  color: #94A3B8;
                }
              }

              .task-time-pill {
                font-size: 11px;
                color: #64748B;
              }
            }

            .task-meta {
              display: flex;
              gap: 8px;
              font-size: 11px;

              .task-tag {
                color: #64748B;
              }

              .task-type-tag {
                color: #1677FF;
              }
            }
          }

          .task-action-col {
            flex-shrink: 0;

            .capsule-task-btn {
              height: 30px;
              padding: 0 14px;
              border-radius: 9999px; // 长圆按钮
              font-size: 11.5px;
              font-weight: 600;
              cursor: pointer;
              background: #1677FF;
              color: #FFFFFF;
              border: none;
              transition: all 0.2s;

              &:hover {
                background: #4096FF;
              }

              &--done {
                background: #E2E8F0;
                color: #64748B;
                cursor: default;
              }
            }
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
}

@media (max-width: 1024px) {
  .learning-kpi-row {
    grid-template-columns: 1fr !important;
  }

  .learning-main-split {
    grid-template-columns: 1fr !important;
  }
}
</style>
