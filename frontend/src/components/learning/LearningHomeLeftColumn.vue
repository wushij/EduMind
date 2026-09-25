<template>
<div class="learning-left-col">
        <!-- A. 薄弱知识点诊断预警 (AI 图谱驱动) -->
        <div class="section-card">
          <div class="section-card-header">
            <div class="header-left">
              <el-icon class="header-icon icon-warning"><WarningFilled /></el-icon>
              <h3 class="header-title">AI 考点诊断预警与薄弱项攻坚</h3>
              <span class="capsule-alert-pill">急需强化 {{ weakPoints.length }} 项</span>
            </div>
            <button
              type="button"
              class="capsule-action-link"
              @click="emit('generate-weak')"
            >
              <el-icon class="action-icon"><AiSparkleIcon /></el-icon>
              <span>一键 AI 生成巩固练习</span>
            </button>
          </div>

          <div v-if="loading" class="section-loading-hint">加载中...</div>
          <el-empty v-else-if="!weakPoints.length" description="暂无薄弱考点，继续保持" />

          <div v-else class="weakness-points-list">
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
                  @click="emit('study-point', point)"
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
              <el-icon class="header-icon icon-tasks"><DocumentChecked /></el-icon>
              <h3 class="header-title">今日自适应学习计划</h3>
              <span class="capsule-sub-badge">完成率 {{ completionRate }}</span>
            </div>
          </div>

          <el-empty v-if="!loading && !todayTasks.length" description="今日暂无待办，可去任务中心查看全部" />

          <div v-else class="today-tasks-list">
            <div
              v-for="task in todayTasks"
              :key="task.id"
              class="task-row-card"
              :class="{ completed: task.completed }"
            >
              <div class="task-checkbox-col">
                <span class="checkbox-circle" :class="{ checked: task.completed }">
                  <el-icon v-if="task.completed" :size="12"><Check /></el-icon>
                </span>
              </div>

              <div class="task-content-col">
                <div class="task-top">
                  <span class="task-title" :class="{ 'line-through': task.completed }">
                    {{ task.title }}
                  </span>
                  <span class="task-time-pill">
                    <el-icon class="time-icon"><Timer /></el-icon>
                    <span>{{ task.estimatedMinutes }} 分钟</span>
                  </span>
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
                  @click="emit('execute-task', task)"
                >
                  <span>{{ task.completed ? '已完成' : '去完成' }}</span>
                  <el-icon v-if="!task.completed" class="btn-arrow-icon"><Right /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
</template>

<script setup lang="ts">
import {
  WarningFilled,
  DocumentChecked,
  Timer,
  Check,
  Right
} from '@element-plus/icons-vue';

import type { LearningHomeTaskUI, LearningHomeWeakPointUI } from '@/types/learning/home';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

defineProps<{
  loading?: boolean;
  weakPoints: LearningHomeWeakPointUI[];
  todayTasks: LearningHomeTaskUI[];
  completionRate: string;
}>();

const emit = defineEmits<{
  'generate-weak': [];
  'study-point': [point: LearningHomeWeakPointUI];
  'execute-task': [task: LearningHomeTaskUI];
}>();
</script>

<style scoped lang="scss">
.learning-left-col {
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
                display: inline-flex;
                align-items: center;
                gap: 4px;
                font-size: 11px;
                color: #64748B;

                .time-icon {
                  font-size: 12px;
                  color: #94A3B8;
                }
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
              display: inline-flex;
              align-items: center;
              gap: 4px;
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

              .btn-arrow-icon {
                font-size: 12px;
              }

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


  }
}
</style>
