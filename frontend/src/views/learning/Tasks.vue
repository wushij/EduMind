<template>
  <div class="learning-tasks-page" v-loading="loading">
    <LearningSubpageHero
      title="学习任务中心 · 课程作业"
      subtitle="查看各课程已发布的在线作业，在截止前完成作答并提交。"
    >
      <template #extra>
        <div class="tasks-progress-hud">
          <div class="hud-circle">
            <el-progress
              type="circle"
              :percentage="completionPercentage"
              :width="70"
              :stroke-width="7"
              color="#1677FF"
            />
          </div>
          <div class="hud-text">
            <span class="hud-title">作业完成进度</span>
            <span class="hud-desc">已完成 {{ completedCount }} / {{ tasks.length }} 项</span>
          </div>
        </div>
      </template>
    </LearningSubpageHero>

    <div class="tasks-content-wrapper">
      <div class="tasks-toolbar">
        <el-radio-group v-model="filterStatus">
          <el-radio-button label="ALL">全部 ({{ tasks.length }})</el-radio-button>
          <el-radio-button label="PENDING">待完成 ({{ pendingCount }})</el-radio-button>
          <el-radio-button label="COMPLETED">已完成 ({{ completedCount }})</el-radio-button>
        </el-radio-group>

        <div class="toolbar-right">
          <el-select
            v-model="courseFilter"
            placeholder="课程过滤"
            style="width: 200px"
            clearable
            @change="onCourseFilterChange"
          >
            <el-option label="全部课程" :value="undefined" />
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name || c.title" :value="c.id" />
          </el-select>
        </div>
      </div>

      <div v-if="filteredTasks.length > 0" class="tasks-list">
        <el-card
          v-for="t in filteredTasks"
          :key="t.id"
          class="task-item-card"
          :class="{ 'is-done': t.status === 'COMPLETED' }"
          shadow="hover"
        >
          <div class="task-card-inner">
            <div class="task-left">
              <div class="task-type-badge badge-assignment">
                <el-icon><Edit /></el-icon>
              </div>
              <div class="task-details">
                <div class="title-row">
                  <h4 class="task-title">{{ t.title }}</h4>
                  <el-tag size="small" :type="t.status === 'COMPLETED' ? 'success' : 'warning'">
                    {{ t.status === 'COMPLETED' ? '已达成' : '待完成' }}
                  </el-tag>
                </div>
                <div class="meta-row">
                  <span class="course-name">
                    <el-icon><Reading /></el-icon>
                    {{ t.courseName }}
                  </span>
                  <span class="due-time">
                    <el-icon><Timer /></el-icon>
                    截止时间：{{ t.dueDate }}
                  </span>
                  <span class="estimated-time">
                    <el-icon><Clock /></el-icon>
                    预计用时 {{ t.estimatedMinutes }} 分钟
                  </span>
                </div>
              </div>
            </div>

            <div class="task-actions">
              <el-button v-if="t.status === 'PENDING'" type="primary" @click="handleEnterTask(t)">
                立即开始
              </el-button>
              <el-button v-else type="info" plain @click="handleReviewTask(t)">
                查看成绩
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <div v-else class="empty-box">
        <el-empty description="暂无已发布的课程作业，请加入课程后查看。" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Reading, Timer, Clock, Edit } from '@element-plus/icons-vue';
import LearningSubpageHero from '@/components/learning/LearningSubpageHero.vue';
import { useLearningTasks, type LearningTaskItem } from '@/composables/learning/useLearningTasks';

const router = useRouter();
const {
  loading,
  tasks,
  courseOptions,
  pendingCount,
  completedCount,
  completionPercentage,
  loadCourseOptions,
  fetchTasks
} = useLearningTasks();

const filterStatus = ref('ALL');
const courseFilter = ref<number | undefined>(undefined);

const filteredTasks = computed(() => {
  return tasks.value.filter((t) => {
    if (filterStatus.value !== 'ALL' && t.status !== filterStatus.value) {
      return false;
    }
    return true;
  });
});

function handleEnterTask(t: LearningTaskItem) {
  router.push(`/learning/assignments/${t.assignmentId}/take`);
}

function handleReviewTask(t: LearningTaskItem) {
  router.push(`/learning/assignments/${t.assignmentId}/result`);
}

async function onCourseFilterChange() {
  await fetchTasks(courseFilter.value);
}

onMounted(async () => {
  await loadCourseOptions();
  await fetchTasks();
});
</script>

<style scoped lang="scss">
.learning-tasks-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;

  .tasks-progress-hud {
    display: flex;
    align-items: center;
    gap: 16px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
    border-radius: 12px;
    padding: 10px 18px;
    margin-top: 14px;
    border: 1px solid rgba(22, 119, 255, 0.15);
    box-shadow: 0 2px 10px rgba(30, 80, 150, 0.06);

    .hud-text {
      display: flex;
      flex-direction: column;

      .hud-title {
        font-size: 14px;
        font-weight: 600;
        color: #0f172a;
      }

      .hud-desc {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .tasks-content-wrapper {
    padding: 0 4px;
  }

  .tasks-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;
  }

  .tasks-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .task-item-card {
    border-radius: 14px;
    border: 1px solid #e2e8f0;

    &.is-done {
      opacity: 0.92;
    }
  }

  .task-card-inner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
  }

  .task-left {
    display: flex;
    gap: 14px;
    flex: 1;
    min-width: 240px;
  }

  .task-type-badge {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.badge-assignment {
      background: #eff6ff;
      color: #1677ff;
    }
  }

  .title-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .task-title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #0f172a;
  }

  .meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    margin-top: 8px;
    font-size: 13px;
    color: #64748b;

    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .empty-box {
    padding: 48px 0;
  }
}
</style>
