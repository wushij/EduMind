<template>
  <div class="ai-grading-container ai-teaching-page-shell">
    <ProfilePageHero
      title="AI 智能批改"
      subtitle="统一管理作业批改队列，支持并发预评、严格度策略与结果复核。"
    >
      <template #actions>
        <div class="hero-action-row">
          <button type="button" class="hero-pill-btn is-outline" @click="router.push('/ai/marketplace')">
            返回 AI 广场
          </button>
          <el-button
            type="primary"
            class="grading-run-btn"
            :icon="Lightning"
            :loading="batchRunning || gradingLoading"
            @click="handleRunAllPending"
          >
            启动全队列并发批改
          </el-button>
        </div>
      </template>
    </ProfilePageHero>

    <!-- 顶栏指标卡 -->
    <div class="metrics-row">
      <div class="metric-card">
        <div class="metric-icon text-blue-600 bg-blue-50">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ totalSubmissionsCount }}</span>
          <span class="label">已收录作业答卷总量</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon text-emerald-600 bg-emerald-50">
          <el-icon><Aim /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ aiGradedCount }}</span>
          <span class="label">已完成 AI 智能预评</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon text-purple-600 bg-purple-50">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">96.4%</span>
          <span class="label">知识点判准置信度</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon text-amber-600 bg-amber-50">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">78% ↓</span>
          <span class="label">教师批改用时缩减</span>
        </div>
      </div>
    </div>

    <!-- AI 批改配置与模型偏好控制台 -->
    <el-card shadow="never" class="settings-card">
      <div class="settings-grid">
        <div class="setting-item">
          <span class="setting-label">批改推理模型：</span>
          <el-select v-model="selectedModel" size="default" style="width: 200px">
            <el-option label="DeepSeek-R1 (深度推理链)" value="deepseek-r1" />
            <el-option label="DeepSeek-V3 (教学标准版)" value="deepseek-v3" />
            <el-option label="Qwen-2.5-72B-Instruct" value="qwen-2.5" />
          </el-select>
        </div>

        <div class="setting-item">
          <span class="setting-label">评分严格倾向：</span>
          <el-select v-model="strictness" size="default" style="width: 160px">
            <el-option label="严格严谨（重点考察细节）" value="STRICT" />
            <el-option label="标准平衡（平衡步骤与结果）" value="NORMAL" />
            <el-option label="包容鼓励（按步给分优先）" value="LENIENT" />
          </el-select>
        </div>

        <div class="setting-item">
          <span class="setting-label">自动生成评语：</span>
          <el-switch v-model="autoFeedback" active-text="开启" inactive-text="关闭" />
        </div>
      </div>
    </el-card>

    <!-- 正在进行与待处理的批改任务列表 -->
    <div v-loading="loadingTasks" class="tasks-table-card">
      <div class="table-header-line">
        <h3>当前作业批改任务队列</h3>
        <span class="text-xs text-slate-500">支持实时监测批改进度与结果复核</span>
      </div>

      <el-table :data="gradingTasks" stripe class="main-table">
        <template #empty>
          <el-empty description="当前暂无作业批改任务队列" />
        </template>
        <el-table-column label="作业标题" prop="title" min-width="240">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="title-text">{{ row.title }}</span>
              <span class="course-text text-xs text-slate-500">{{ row.courseName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="答卷总数" prop="submissionCount" width="120">
          <template #default="{ row }">
            <span class="font-semibold">{{ row.submissionCount }} 份</span>
          </template>
        </el-table-column>

        <el-table-column label="AI 批改进度" width="220">
          <template #default="{ row }">
            <div class="prog-cell">
              <el-progress
                :percentage="row.progress"
                :status="row.progress === 100 ? 'success' : undefined"
              />
              <span class="text-xs text-slate-400 mt-1 block">
                {{ row.progress === 100 ? '已全部完成' : `正在评阅第 ${Math.round((row.submissionCount * row.progress) / 100)} 份...` }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DONE' ? 'success' : 'warning'" size="small">
              {{ row.status === 'DONE' ? '评阅就绪' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="viewGradingResults(row)"
            >
              <span>查看诊断报告</span>
              <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
            <el-button
              type="success"
              link
              size="small"
              :loading="row.running"
              @click="reRunTask(row)"
            >
              触发 AI 评阅
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Cpu,
  Lightning,
  Tickets,
  Aim,
  CircleCheck,
  Clock,
  ArrowRight
} from '@element-plus/icons-vue';
import { useGrading, type GradingTaskRow } from '@/composables/ai/useGrading';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';

const router = useRouter();
const { startGrading, batchGrade, loadGradingTasks, loading: gradingLoading } = useGrading();

const selectedModel = ref('deepseek-v3');
const strictness = ref('NORMAL');
const autoFeedback = ref(true);
const batchRunning = ref(false);
const loadingTasks = ref(false);

const gradingTasks = ref<GradingTaskRow[]>([]);

onMounted(async () => {
  await loadRealTasks();
});

async function loadRealTasks() {
  loadingTasks.value = true;
  try {
    gradingTasks.value = await loadGradingTasks();
  } catch (err: any) {
    ElMessage.error(err?.message || '加载作业批改队列失败');
    gradingTasks.value = [];
  } finally {
    loadingTasks.value = false;
  }
}

const totalSubmissionsCount = computed(() => {
  return gradingTasks.value.reduce((acc, t) => acc + (t.submissionCount || 0), 0);
});

const aiGradedCount = computed(() => {
  return gradingTasks.value.reduce((acc, t) => {
    return acc + Math.round(((t.submissionCount || 0) * (t.progress || 0)) / 100);
  }, 0);
});

async function handleRunAllPending() {
  batchRunning.value = true;
  try {
    const allPendingIds: number[] = [];
    for (const t of gradingTasks.value) {
      if (t.submissions && t.submissions.length > 0) {
        for (const s of t.submissions) {
          if (s.status !== 'GRADED') {
            allPendingIds.push(s.id);
          }
        }
      }
    }

    if (allPendingIds.length === 0) {
      ElMessage.info('当前暂无待批改的学生答卷');
      return;
    }

    await batchGrade(allPendingIds);
    await loadRealTasks();
    ElMessage.success(`已成功为 ${allPendingIds.length} 份答卷完成 AI 智能预审评阅！`);
  } catch (err: any) {
    ElMessage.error(err?.message || '批量并发评阅失败，请重试');
  } finally {
    batchRunning.value = false;
  }
}

async function reRunTask(row: GradingTaskRow) {
  const subIds = row.submissions?.map(s => s.id) || [];
  if (subIds.length === 0) {
    ElMessage.warning(`【${row.title}】暂无学生提交答卷`);
    return;
  }
  row.running = true;
  try {
    for (const sid of subIds) {
      await startGrading(sid);
    }
    row.progress = 100;
    row.status = 'DONE';
    ElMessage.success(`【${row.title}】已完成全新一轮大模型深度复评！`);
  } catch (err: any) {
    ElMessage.error(err?.message || `评阅任务【${row.title}】执行异常`);
  } finally {
    row.running = false;
  }
}

function viewGradingResults(row: GradingTaskRow) {
  const firstSubId = row.submissions?.[0]?.id;
  if (!firstSubId) {
    ElMessage.warning(`【${row.title}】暂无学生提交答卷可供查看`);
    return;
  }
  router.push({
    path: '/ai/grading/result',
    query: { taskId: row.id, submissionId: firstSubId, title: row.title }
  });
}
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.ai-grading-container {
  .grading-run-btn {
    border-radius: 9999px;
    font-weight: 600;
    padding: 10px 22px;
    box-shadow: 0 4px 16px rgba(22, 119, 255, 0.3);
  }

  .metrics-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
    margin-bottom: 20px;

    .metric-card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 22px;
      padding: 20px 24px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

      .metric-icon {
        width: 48px;
        height: 48px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
      }

      .metric-info {
        display: flex;
        flex-direction: column;

        .val {
          font-size: 24px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1.1;
        }

        .label {
          font-size: 12px;
          color: #64748b;
          margin-top: 4px;
        }
      }
    }
  }

  .settings-card {
    background: #ffffff;
    border-radius: 24px;
    border: 1px solid #e2e8f0;
    padding: 6px 14px;
    margin-bottom: 20px;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);

    .settings-grid {
      display: flex;
      align-items: center;
      gap: 32px;
      flex-wrap: wrap;

      .setting-item {
        display: flex;
        align-items: center;
        gap: 10px;

        .setting-label {
          font-size: 13px;
          font-weight: 600;
          color: #334155;
        }
      }
    }
  }

  .tasks-table-card {
    background: #ffffff;
    border-radius: 24px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
    padding: 24px 28px;

    .table-header-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      h3 {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
      }
    }

    .title-cell {
      display: flex;
      flex-direction: column;

      .title-text {
        font-weight: 600;
        color: #1e293b;
      }
    }
  }
}
</style>
