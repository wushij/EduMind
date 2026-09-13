<template>
  <div class="learning-tasks-page" v-loading="loading">
    <PageHeroBanner
      title="学习任务中心 · 智能作业与微测打卡"
      subtitle="动态聚合各课程任课教师发布的必修作业、随堂自测与 AI 个性化推送的专项微攻坚任务"
      background-variant="learning"
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
            <span class="hud-title">今日任务达成进度</span>
            <span class="hud-desc">已完成 {{ completedCount }} / {{ tasks.length }} 项学习任务</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="tasks-content-wrapper">
      <!-- 状态过滤与动作栏 -->
      <div class="tasks-toolbar">
        <el-radio-group v-model="filterStatus" @change="filterTasks">
          <el-radio-button label="ALL">全部 ({{ tasks.length }})</el-radio-button>
          <el-radio-button label="PENDING">待完成 ({{ pendingCount }})</el-radio-button>
          <el-radio-button label="COMPLETED">已完成 ({{ completedCount }})</el-radio-button>
        </el-radio-group>

        <div class="toolbar-right">
          <el-select v-model="courseFilter" placeholder="课程过滤" style="width: 200px" @change="filterTasks">
            <el-option label="全部课程" value="" />
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </div>
      </div>

      <!-- 任务列表 -->
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
              <div class="task-type-badge" :class="getTypeBadgeClass(t.type)">
                <el-icon><component :is="getTypeIcon(t.type)" /></el-icon>
              </div>
              <div class="task-details">
                <div class="title-row">
                  <h4 class="task-title">{{ t.title }}</h4>
                  <el-tag size="small" :type="t.status === 'COMPLETED' ? 'success' : 'warning'">
                    {{ t.status === 'COMPLETED' ? '已达成' : '待完成' }}
                  </el-tag>
                  <el-tag v-if="t.isAiRecommended" size="small" type="primary" effect="plain">
                    AI 靶向推送
                  </el-tag>
                </div>
                <div class="meta-row">
                  <span class="course-name">
                    <el-icon><Reading /></el-icon>
                    {{ t.courseName }}
                  </span>
                  <span class="due-time" :class="{ 'is-urgent': isUrgent(t.dueDate) }">
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
              <el-button
                v-if="t.status === 'PENDING'"
                type="primary"
                @click="handleEnterTask(t)"
              >
                立即开始
              </el-button>
              <el-button
                v-else
                type="info"
                plain
                @click="handleReviewTask(t)"
              >
                查看批改与解析
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-box">
        <el-empty description="当前没有待处理的学习任务，尽情享受轻松时光吧！" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Reading,
  Timer,
  Clock,
  Edit,
  VideoPlay,
  Aim,
  Document
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

interface TaskItem {
  id: number;
  title: string;
  type: 'ASSIGNMENT' | 'LESSON' | 'WRONG_RETEST' | 'PRACTICE';
  courseName: string;
  courseId: number;
  dueDate: string;
  estimatedMinutes: number;
  status: 'PENDING' | 'COMPLETED';
  isAiRecommended?: boolean;
  targetUrl?: string;
}

const router = useRouter();
const { courseOptions } = useTeacherCourses(102);

const loading = ref(false);
const filterStatus = ref('ALL');
const courseFilter = ref<number | ''>('');

const tasks = ref<TaskItem[]>([
  {
    id: 1,
    title: '期中巩固作业：微分中值定理与柯西不等式应用大题',
    type: 'ASSIGNMENT',
    courseName: '高等数学（上）',
    courseId: 103,
    dueDate: '2026-09-15 23:59',
    estimatedMinutes: 30,
    status: 'PENDING',
    targetUrl: '/question/assignments'
  },
  {
    id: 2,
    title: 'AI 变式考点攻坚：洛必达法则条件与极限展开 (3 题)',
    type: 'WRONG_RETEST',
    courseName: '高等数学（上）',
    courseId: 103,
    dueDate: '2026-09-14 20:00',
    estimatedMinutes: 10,
    status: 'PENDING',
    isAiRecommended: true,
    targetUrl: '/learning/ai-practice'
  },
  {
    id: 3,
    title: '精讲微课预习：二叉树递归遍历与非递归实现',
    type: 'LESSON',
    courseName: '数据结构与算法',
    courseId: 101,
    dueDate: '2026-09-16 18:00',
    estimatedMinutes: 15,
    status: 'PENDING',
    targetUrl: '/courses/101'
  },
  {
    id: 4,
    title: '单元测试：Java 异常处理机制与反射基础考查',
    type: 'PRACTICE',
    courseName: 'Java面向对象程序设计',
    courseId: 102,
    dueDate: '2026-09-12 22:00',
    estimatedMinutes: 20,
    status: 'COMPLETED',
    targetUrl: '/learning/ai-practice'
  }
]);

const filteredTasks = computed(() => {
  return tasks.value.filter((t) => {
    if (filterStatus.value !== 'ALL' && t.status !== filterStatus.value) {
      return false;
    }
    if (courseFilter.value && t.courseId !== courseFilter.value) {
      return false;
    }
    return true;
  });
});

const completedCount = computed(() => {
  return tasks.value.filter((t) => t.status === 'COMPLETED').length;
});

const pendingCount = computed(() => {
  return tasks.value.filter((t) => t.status === 'PENDING').length;
});

const completionPercentage = computed(() => {
  if (!tasks.value.length) return 0;
  return Math.round((completedCount.value / tasks.value.length) * 100);
});

const getTypeBadgeClass = (type: string) => {
  if (type === 'ASSIGNMENT') return 'badge-assignment';
  if (type === 'LESSON') return 'badge-lesson';
  if (type === 'WRONG_RETEST') return 'badge-wrong';
  return 'badge-practice';
};

const getTypeIcon = (type: string) => {
  if (type === 'ASSIGNMENT') return Edit;
  if (type === 'LESSON') return VideoPlay;
  if (type === 'WRONG_RETEST') return Aim;
  return Document;
};

const isUrgent = (dueDate: string) => {
  return dueDate.includes('09-14');
};

const filterTasks = () => {
  // computed reactive
};

const handleEnterTask = (t: TaskItem) => {
  if (t.targetUrl) {
    router.push(t.targetUrl);
  } else {
    ElMessage.info(`正在进入【${t.title}】`);
  }
};

const handleReviewTask = (t: TaskItem) => {
  ElMessage.info(`查看任务【${t.title}】的成绩与详细解析`);
};

onMounted(() => {
  loading.value = true;
  setTimeout(() => {
    loading.value = false;
  }, 200);
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
        font-weight: 700;
        color: #1E293B;
      }

      .hud-desc {
        font-size: 12px;
        color: #64748B;
        margin-top: 2px;
      }
    }
  }

  .tasks-content-wrapper {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .tasks-toolbar {
      background: #FFFFFF;
      border-radius: 14px;
      padding: 14px 20px;
      border: 1px solid #E2E8F0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .tasks-list {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .task-item-card {
        border-radius: 14px;
        border: 1px solid #E2E8F0;
        transition: all 0.25s ease;

        &:hover {
          border-color: #BFDBFE;
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(22, 119, 255, 0.08);
        }

        &.is-done {
          opacity: 0.75;
          background: #F8FAFC;
        }

        .task-card-inner {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 16px;
          flex-wrap: wrap;

          .task-left {
            display: flex;
            align-items: center;
            gap: 16px;
            flex: 1;

            .task-type-badge {
              width: 44px;
              height: 44px;
              border-radius: 12px;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 20px;

              &.badge-assignment {
                background: #EFF6FF;
                color: #1677FF;
              }
              &.badge-wrong {
                background: #FEF2F2;
                color: #F5222D;
              }
              &.badge-lesson {
                background: #F0FDF4;
                color: #52C41A;
              }
              &.badge-practice {
                background: #FAF5FF;
                color: #722ED1;
              }
            }

            .task-details {
              display: flex;
              flex-direction: column;
              gap: 6px;

              .title-row {
                display: flex;
                align-items: center;
                gap: 10px;
                flex-wrap: wrap;

                .task-title {
                  margin: 0;
                  font-size: 15px;
                  font-weight: 700;
                  color: #0F172A;
                }
              }

              .meta-row {
                display: flex;
                align-items: center;
                gap: 18px;
                font-size: 12px;
                color: #64748B;
                flex-wrap: wrap;

                span {
                  display: flex;
                  align-items: center;
                  gap: 4px;
                }

                .is-urgent {
                  color: #F5222D;
                  font-weight: 600;
                }
              }
            }
          }
        }
      }
    }

    .empty-box {
      background: #FFFFFF;
      border-radius: 14px;
      padding: 40px;
      border: 1px solid #E2E8F0;
    }
  }
}
</style>
