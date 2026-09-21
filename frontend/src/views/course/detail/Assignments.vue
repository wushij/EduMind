<template>
  <div class="course-assignments-panel" v-loading="loading">
    <!-- 1. 顶部高质感卡片式工具栏（对齐大纲与资源模块规范） -->
    <div class="assignments-top-toolbar">
      <div class="toolbar-left">
        <div class="title-badge-avatar">
          <el-icon><Notebook /></el-icon>
        </div>
        <div class="head-texts">
          <div class="title-row">
            <h3 class="panel-title">课程作业与课堂测验</h3>
            <div class="summary-pill-group">
              <span class="pill-badge pill-badge--primary">
                共 {{ assignments.length }} 项测验任务
              </span>
              <span v-if="!isTeacherView && assignments.length" class="pill-badge pill-badge--warning">
                待完成 {{ pendingCount }} 项
              </span>
              <span v-if="!isTeacherView && completedCount > 0" class="pill-badge pill-badge--success">
                已提交 {{ completedCount }} 项
              </span>
              <span class="pill-badge pill-badge--ai">
                <el-icon class="mr-0.5"><Cpu /></el-icon>
                AI 智能快评
              </span>
            </div>
          </div>
          <p class="panel-desc">
            查看本课程已发布的随堂测验与作业任务，支持即时在线作答、自动评阅与 AI 深度错因辅导。
          </p>
        </div>
      </div>

      <!-- 右侧操作栏：长圆胶囊跑道按钮 -->
      <div class="toolbar-actions">
        <button
          type="button"
          class="capsule-tool-btn"
          title="重新拉取作业列表"
          @click="loadAssignments"
        >
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </button>

        <template v-if="isTeacherView">
          <button
            type="button"
            class="capsule-tool-btn capsule-tool-btn--ai"
            @click="goAiExam"
          >
            <el-icon><MagicStick /></el-icon>
            <span>AI 智能出题</span>
          </button>
          <button
            type="button"
            class="capsule-tool-btn capsule-tool-btn--primary"
            @click="goManage"
          >
            <el-icon><Plus /></el-icon>
            <span>发布与管理作业</span>
          </button>
        </template>

        <template v-else>
          <button
            type="button"
            class="capsule-tool-btn capsule-tool-btn--ai"
            @click="goAIPractice"
          >
            <el-icon><MagicStick /></el-icon>
            <span>AI 考点自适应刷题</span>
          </button>
        </template>
      </div>
    </div>

    <!-- 2. 作业列表卡片流 -->
    <div v-if="assignments.length" class="assignment-cards-grid">
      <div
        v-for="a in assignments"
        :key="a.id"
        class="course-assignment-card"
        :class="{ 'is-completed': isDone(a.mySubmissionStatus) }"
      >
        <!-- 左侧色彩状态立条 -->
        <div
          class="card-color-stripe"
          :class="isDone(a.mySubmissionStatus) ? 'stripe--success' : 'stripe--primary'"
        ></div>

        <div class="card-left-badge" :class="{ 'badge--done': isDone(a.mySubmissionStatus) }">
          <el-icon v-if="isDone(a.mySubmissionStatus)"><DocumentChecked /></el-icon>
          <el-icon v-else><EditPen /></el-icon>
        </div>

        <div class="card-content-body">
          <div class="card-title-row">
            <h4 class="assignment-title" :title="a.title">{{ a.title }}</h4>
            <el-tag
              size="small"
              :type="statusTag(a.mySubmissionStatus)"
              effect="light"
              class="status-pill"
            >
              {{ statusLabel(a.mySubmissionStatus) }}
            </el-tag>
          </div>

          <div class="assignment-meta-row">
            <span class="meta-item">
              <el-icon><Timer /></el-icon>
              截止时间：{{ formatDeadline(a.deadline) }}
            </span>
            <span class="meta-item">
              <el-icon><Trophy /></el-icon>
              满分 {{ a.totalScore ?? 100 }} 分
            </span>
            <span v-if="a.settings?.aiGradingEnabled !== false" class="meta-item chip-ai">
              <el-icon><Cpu /></el-icon>
              AI 智能快评支持
            </span>
          </div>
        </div>

        <div class="assignment-actions">
          <el-button
            v-if="!isTeacherView"
            :type="isDone(a.mySubmissionStatus) ? 'info' : 'primary'"
            :plain="isDone(a.mySubmissionStatus)"
            size="default"
            class="btn-action"
            @click="goTake(a.id, a.mySubmissionStatus)"
          >
            <el-icon class="mr-1">
              <Document v-if="isDone(a.mySubmissionStatus)" />
              <Edit v-else />
            </el-icon>
            {{ actionLabel(a.mySubmissionStatus) }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 3. 全新高颜值空状态引导卡片（告别死板纸箱与大白框） -->
    <div v-else class="empty-state-card">
      <div class="empty-glow-orbit">
        <div class="empty-icon-capsule">
          <el-icon><DocumentChecked /></el-icon>
        </div>
      </div>

      <h4 class="empty-title">本课程暂无已发布的作业或测验</h4>
      <p class="empty-subtitle">
        <template v-if="isTeacherView">
          教师端尚未为当前课程创建作业任务。您可以使用 AI 智能组卷快速出题，或直接进入作业管理中心进行发布。
        </template>
        <template v-else>
          授课教师尚未布置新的随堂测验。您可以先前往学习章节课时，或使用课程专属 AI 助教进行考点自适应强化练习！
        </template>
      </p>

      <div class="empty-actions-row">
        <template v-if="isTeacherView">
          <button type="button" class="cta-pill-btn cta-pill-btn--ai" @click="goAiExam">
            <el-icon><MagicStick /></el-icon>
            <span>使用 AI 智能快速出题</span>
          </button>
          <button type="button" class="cta-pill-btn cta-pill-btn--primary" @click="goManage">
            <el-icon><Plus /></el-icon>
            <span>进入作业管理中心</span>
          </button>
        </template>
        <template v-else>
          <button type="button" class="cta-pill-btn cta-pill-btn--outline" @click="goChapters">
            <el-icon><Reading /></el-icon>
            <span>前往章节大纲学习</span>
          </button>
          <button type="button" class="cta-pill-btn cta-pill-btn--ai" @click="goAIPractice">
            <el-icon><MagicStick /></el-icon>
            <span>开启 AI 考点自适应测验</span>
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  Notebook,
  EditPen,
  Timer,
  Trophy,
  Cpu,
  Edit,
  Document,
  DocumentChecked,
  Refresh,
  MagicStick,
  Plus,
  Reading
} from '@element-plus/icons-vue';
import { getMyAssignments } from '@/api/question/assignment';
import type { StudentAssignment } from '@/types/question/assignment';
import {
  SUBMISSION_STATUS,
  SUBMISSION_STATUS_LABEL,
  SUBMISSION_STATUS_TAG
} from '@/constants/question/assignment';
import { useAuthStore } from '@/stores/auth/auth';
import { RoleEnum } from '@/constants/auth';

const props = defineProps<{ course?: { id?: number } }>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const assignments = ref<StudentAssignment[]>([]);

const courseId = computed(() => Number(props.course?.id || route.params.id));

const isTeacherView = computed(() => {
  const roles = authStore.currentUser?.roles || [];
  return roles.includes(RoleEnum.TEACHER) || roles.includes(RoleEnum.ADMIN);
});

const completedCount = computed(() => {
  return assignments.value.filter((a) => isDone(a.mySubmissionStatus)).length;
});

const pendingCount = computed(() => {
  return Math.max(0, assignments.value.length - completedCount.value);
});

function isDone(st?: string): boolean {
  return (
    st === SUBMISSION_STATUS.GRADED ||
    st === SUBMISSION_STATUS.REVIEWED ||
    st === SUBMISSION_STATUS.SUBMITTED
  );
}

function formatDeadline(val?: string) {
  if (!val) return '无时间限制';
  return String(val).replace('T', ' ').slice(0, 16);
}

function statusLabel(st?: string) {
  if (!st || st === SUBMISSION_STATUS.NOT_STARTED) return '待完成';
  return SUBMISSION_STATUS_LABEL[st] || st;
}

function statusTag(st?: string) {
  return (
    (SUBMISSION_STATUS_TAG[st || ''] as 'info' | 'success' | 'warning' | 'primary') || 'warning'
  );
}

function actionLabel(st?: string) {
  if (isDone(st)) {
    return '查看评阅报告';
  }
  return '立即作答';
}

function goTake(id: number, st?: string) {
  if (isDone(st)) {
    router.push(`/learning/assignments/${id}/result`);
  } else {
    router.push(`/learning/assignments/${id}/take`);
  }
}

function goManage() {
  router.push({ path: '/question/assignments', query: { courseId: String(courseId.value) } });
}

function goAiExam() {
  router.push({ path: '/ai/question/generate', query: { courseId: String(courseId.value) } });
}

function goAIPractice() {
  router.push({ path: '/learning/practice', query: { courseId: String(courseId.value) } });
}

function goChapters() {
  if (courseId.value) {
    router.push(`/course/${courseId.value}/chapters`);
  }
}

async function loadAssignments() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const res = await getMyAssignments({ courseId: courseId.value });
    assignments.value = (res.data || []).filter(
      (a) => a.status === 'PUBLISHED' || a.status === 'CLOSED'
    );
  } finally {
    loading.value = false;
  }
}

onMounted(loadAssignments);
watch(courseId, loadAssignments);
</script>

<style scoped lang="scss">
.course-assignments-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 4px 0 24px;
}

// 1. 顶部高质感卡片式工具栏
.assignments-top-toolbar {
  background: #ffffff;
  border-radius: 18px;
  padding: 18px 24px;
  border: 1px solid #ebf1f7;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 1;
    min-width: 280px;

    .title-badge-avatar {
      width: 46px;
      height: 46px;
      border-radius: 14px;
      background: linear-gradient(135deg, #eaf3ff 0%, #dbeafe 100%);
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      flex-shrink: 0;
      border: 1px solid rgba(37, 99, 235, 0.12);
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
    }

    .head-texts {
      .title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-bottom: 5px;

        .panel-title {
          margin: 0;
          font-size: 17px;
          font-weight: 700;
          color: #0f172a;
          letter-spacing: -0.2px;
        }

        .summary-pill-group {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-wrap: wrap;

          .pill-badge {
            display: inline-flex;
            align-items: center;
            padding: 2px 9px;
            border-radius: 9999px;
            font-size: 11.5px;
            font-weight: 600;

            &--primary {
              background: #eaf3ff;
              color: #1677ff;
            }

            &--warning {
              background: #fff7ed;
              color: #ea580c;
            }

            &--success {
              background: #ecfdf5;
              color: #059669;
            }

            &--ai {
              background: #f5f3ff;
              color: #7c3aed;
              border: 1px solid rgba(124, 58, 237, 0.12);
            }
          }
        }
      }

      .panel-desc {
        margin: 0;
        font-size: 13px;
        color: #64748b;
        line-height: 1.45;
      }
    }
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    .capsule-tool-btn {
      height: 36px;
      padding: 0 14px;
      border-radius: 9999px;
      font-size: 12.5px;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      border: 1px solid #e2e8f0;
      background: #ffffff;
      color: #475569;
      transition: all 0.2s ease;

      &:hover {
        background: #f8fafc;
        color: #1e293b;
        border-color: #cbd5e1;
        transform: translateY(-1px);
      }

      &--primary {
        background: #1677ff;
        border-color: #1677ff;
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

        &:hover {
          background: #0958d9;
          border-color: #0958d9;
          color: #ffffff;
          box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
        }
      }

      &--ai {
        background: linear-gradient(135deg, #8b5cf6, #6d28d9);
        border-color: transparent;
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(109, 40, 217, 0.22);

        &:hover {
          background: linear-gradient(135deg, #7c3aed, #5b21b6);
          color: #ffffff;
          box-shadow: 0 4px 12px rgba(109, 40, 217, 0.32);
        }
      }
    }
  }
}

// 2. 作业卡片流
.assignment-cards-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.course-assignment-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 22px;
  border-radius: 16px;
  border: 1px solid #ebf1f7;
  background: #ffffff;
  box-shadow: 0 3px 12px rgba(15, 23, 42, 0.03);
  transition: all 0.25s ease;
  overflow: hidden;

  .card-color-stripe {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;

    &.stripe--primary {
      background: linear-gradient(180deg, #3b82f6, #1d4ed8);
    }
    &.stripe--success {
      background: linear-gradient(180deg, #10b981, #059669);
    }
  }

  &:hover {
    border-color: #bfdbfe;
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(37, 99, 235, 0.07);
  }

  &.is-completed {
    background: #fbfcfe;
  }

  .card-left-badge {
    width: 46px;
    height: 46px;
    border-radius: 14px;
    background: #eff6ff;
    color: #2563eb;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    flex-shrink: 0;
    border: 1px solid rgba(37, 99, 235, 0.1);

    &.badge--done {
      background: #ecfdf5;
      color: #059669;
      border-color: rgba(5, 150, 105, 0.1);
    }
  }

  .card-content-body {
    flex: 1;
    min-width: 0;

    .card-title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
      flex-wrap: wrap;

      .assignment-title {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #0f172a;
      }

      .status-pill {
        font-weight: 600;
        border-radius: 6px;
      }
    }

    .assignment-meta-row {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 13px;
      color: #64748b;
      flex-wrap: wrap;

      .meta-item {
        display: inline-flex;
        align-items: center;
        gap: 5px;
      }

      .chip-ai {
        color: #7c3aed;
        background: #f5f3ff;
        padding: 2px 8px;
        border-radius: 6px;
        font-size: 12px;
        font-weight: 600;
      }
    }
  }

  .assignment-actions {
    flex-shrink: 0;

    .btn-action {
      border-radius: 10px;
      font-weight: 600;
      padding: 0 18px;
      height: 36px;
    }
  }
}

// 3. 全新高颜值空状态
.empty-state-card {
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid #ebf1f7;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.03);
  padding: 56px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;

  .empty-glow-orbit {
    position: relative;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(37, 99, 235, 0.08) 0%, rgba(37, 99, 235, 0) 70%);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;

    .empty-icon-capsule {
      width: 58px;
      height: 58px;
      border-radius: 18px;
      background: linear-gradient(135deg, #f0f7ff 0%, #e0f2fe 100%);
      color: #0284c7;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      box-shadow: 0 6px 16px rgba(2, 132, 199, 0.12);
      border: 1px solid rgba(2, 132, 199, 0.15);
    }
  }

  .empty-title {
    margin: 0 0 8px 0;
    font-size: 16.5px;
    font-weight: 700;
    color: #1e293b;
  }

  .empty-subtitle {
    margin: 0 0 24px 0;
    font-size: 13.5px;
    color: #64748b;
    max-width: 520px;
    line-height: 1.6;
  }

  .empty-actions-row {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    justify-content: center;

    .cta-pill-btn {
      height: 38px;
      padding: 0 20px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 7px;
      cursor: pointer;
      transition: all 0.2s ease;
      border: none;

      &--outline {
        background: #ffffff;
        border: 1px solid #cbd5e1;
        color: #334155;

        &:hover {
          background: #f8fafc;
          border-color: #94a3b8;
          transform: translateY(-1px);
        }
      }

      &--primary {
        background: #1677ff;
        color: #ffffff;
        box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

        &:hover {
          background: #0958d9;
          transform: translateY(-1px);
          box-shadow: 0 5px 14px rgba(22, 119, 255, 0.35);
        }
      }

      &--ai {
        background: linear-gradient(135deg, #8b5cf6, #6d28d9);
        color: #ffffff;
        box-shadow: 0 3px 10px rgba(109, 40, 217, 0.25);

        &:hover {
          background: linear-gradient(135deg, #7c3aed, #5b21b6);
          transform: translateY(-1px);
          box-shadow: 0 5px 14px rgba(109, 40, 217, 0.35);
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .assignments-top-toolbar {
    flex-direction: column;
    align-items: flex-start;

    .toolbar-actions {
      width: 100%;
      justify-content: flex-start;
    }
  }

  .course-assignment-card {
    flex-direction: column;
    align-items: flex-start;

    .assignment-actions {
      width: 100%;

      .btn-action {
        width: 100%;
      }
    }
  }
}
</style>


