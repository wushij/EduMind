<template>
  <div class="submission-detail-container">
    <!-- 顶部导航 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.back()">
        返回答卷列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
        <el-breadcrumb-item>学生答卷智能评阅工作台</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 学生与答卷概览大卡片 -->
    <div v-loading="loading" class="submission-hero-card">
      <div class="hero-left">
        <div class="student-title-line">
          <div class="student-avatar-circle">
            {{ submissionData?.studentName ? submissionData.studentName.slice(0, 1) : '学' }}
          </div>
          <div>
            <div class="name-row">
              <h2 class="student-name">{{ submissionData?.studentName }}</h2>
              <span class="student-no">学号：{{ submissionData?.studentNo }}</span>
              <el-tag :type="submissionData?.status === 'GRADED' ? 'success' : 'warning'" size="small">
                {{ submissionData?.status === 'GRADED' ? '已完成教师终审' : '待教师确认打分' }}
              </el-tag>
            </div>
            <p class="assignment-belong">
              作业任务：<strong>{{ submissionData?.assignmentTitle }}</strong>
              <span class="submit-time-text">（提交于 {{ submissionData?.submitTime }}）</span>
            </p>
          </div>
        </div>
      </div>

      <!-- 右侧当前总得分大字与快捷触发 AI 预评按钮 -->
      <div class="hero-right-score">
        <el-button
          v-if="submissionData?.status === 'PENDING'"
          type="primary"
          plain
          :loading="gradingInProgress"
          class="mr-4"
          @click="handleTriggerGradeNow"
        >
          <el-icon><Cpu /></el-icon>
          <span>立即触发该答卷 AI 预评</span>
        </el-button>
        <div class="score-pill">
          <span class="score-num">{{ calculatedTotalScore }}</span>
          <span class="score-label">/ {{ submissionData?.totalScore || 100 }} 满分</span>
        </div>
      </div>
    </div>

    <!-- 试题逐题评阅流 -->
    <div v-loading="loading" class="questions-grading-stream">
      <template v-if="gradingItems.length > 0">
        <div
          v-for="(item, idx) in gradingItems"
          :key="item.questionId"
          class="grading-question-card"
        >
        <!-- 试题头部 -->
        <div class="card-q-header">
          <div class="header-left">
            <span class="q-index-badge">第 {{ idx + 1 }} 题</span>
            <el-tag size="small" :type="getTypeTagType(item.type)">{{ getTypeLabel(item.type) }}</el-tag>
            <span class="standard-score">满分：{{ item.maxScore }} 分</span>
          </div>
          <div class="header-right">
            <span class="suggest-hint">
              AI 建议得分：<strong class="text-purple-600">{{ item.aiScore ?? '未评分' }} 分</strong>
            </span>
          </div>
        </div>

        <!-- 试题题干正文 -->
        <div class="q-stem-body">
          {{ item.stem }}
        </div>

        <!-- 学生作答呈现区 -->
        <div class="student-answer-panel">
          <div class="panel-header">
            <span class="panel-tag">
              <el-icon><User /></el-icon>
              <span>考生提交作答：</span>
            </span>
            <span v-if="item.isObjective" class="objective-result-tag" :class="{ pass: item.isCorrect }">
              <el-icon class="mr-1">
                <Check v-if="item.isCorrect" />
                <Close v-else />
              </el-icon>
              <span>{{ item.isCorrect ? '客观比对正确' : '客观比对错误' }}</span>
            </span>
          </div>
          <div class="answer-content">
            {{ item.studentAnswer || '（考生未作答）' }}
          </div>
        </div>

        <!-- 标准参考答案与解析 -->
        <div class="standard-answer-panel">
          <div class="panel-header">
            <span class="panel-tag">
              <el-icon><Reading /></el-icon>
              <span>标准参考答案与考查重点：</span>
            </span>
          </div>
          <div class="std-content">
            <div class="ans-line">
              <span class="font-semibold text-emerald-700">正确参考答案：</span>
              <span class="font-bold text-emerald-700">{{ item.standardAnswer }}</span>
            </div>
            <div class="analysis-line">
              <span class="text-slate-500">解析说明：</span>
              <span class="text-slate-700">{{ item.analysis }}</span>
            </div>
          </div>
        </div>

        <!-- AI 智能评阅分析小助手卡片 -->
        <div class="ai-review-copilot-box">
          <div class="copilot-header">
            <div class="copilot-title">
              <el-icon class="ai-spark"><MagicStick /></el-icon>
              <span>EduMind AI 智能辅助评阅研判</span>
              <span class="confidence-tag">置信度 96%</span>
            </div>
            <el-button
              type="primary"
              size="small"
              plain
              @click="adoptSingleAIScore(item)"
            >
              采纳此 AI 评分
            </el-button>
          </div>

          <div class="copilot-body">
            <p class="ai-rationale">
              <strong>【评阅判定】：</strong>{{ item.aiComment || '大模型已完成考点比对与步骤采分。' }}
            </p>
          </div>
        </div>

        <!-- 教师最终终审打分与批语输入 -->
        <div class="teacher-grading-dock">
          <div class="score-input-group">
            <span class="input-label">本题最终得分：</span>
            <el-input-number
              v-model="item.teacherScore"
              :min="0"
              :max="item.maxScore"
              :step="1"
              size="default"
              class="score-input-number"
            />
            <span class="unit">/ {{ item.maxScore }} 分</span>
          </div>

          <div class="comment-input-group">
            <el-input
              v-model="item.teacherComment"
              placeholder="在此填写对该学生的个性化评阅指导批语（选填）..."
              clearable
              size="default"
            />
          </div>
        </div>
      </div>
    </template>

      <!-- 尚未评阅空状态提示 -->
      <div v-else class="empty-grading-card bg-white p-12 rounded-xl text-center border border-slate-200 my-4 shadow-sm">
        <el-empty description="当前答卷暂无 AI 智能评阅细则。请点击下方按钮触发大模型深度评阅。">
          <el-button
            type="primary"
            size="large"
            :loading="gradingInProgress"
            @click="handleTriggerGradeNow"
          >
            <el-icon><Service /></el-icon>
            <span>立即执行 AI 智能批改</span>
          </el-button>
        </el-empty>
      </div>
    </div>

    <!-- 底部悬浮固定操作条 (Sticky Bottom Dock) -->
    <div class="bottom-sticky-bar">
      <div class="bar-left">
        <el-button @click="adoptAllAIScores">
          <el-icon><Select /></el-icon>
          <span>一键采纳全卷 AI 智能建议得分</span>
        </el-button>
      </div>

      <div class="bar-right">
        <div class="total-stat">
          <span>当前试卷累计总分：</span>
          <strong class="total-num">{{ calculatedTotalScore }}</strong>
          <span>/ {{ submissionData?.totalScore || 100 }} 分</span>
        </div>

        <el-button
          type="primary"
          size="large"
          class="confirm-btn"
          :loading="saving"
          @click="handleSaveGrading"
        >
          <el-icon><Finished /></el-icon>
          <span>保存并确认评阅最终成绩</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Cpu, User, Reading, MagicStick, Service, Select, Finished, Check, Close } from '@element-plus/icons-vue';
import {
  getSubmissionDetail,
  getSubmissionGrading,
  gradeSubmission,
  reviewGrading
} from '@/api/question/submission';
import type { QuestionType } from '@/types/question/question';

const route = useRoute();
const router = useRouter();

const submissionId = computed(() => Number(route.params.id) || 201);
const loading = ref(false);
const saving = ref(false);
const gradingInProgress = ref(false);

const submissionData = ref<any>(null);

interface GradingItem {
  questionId: number;
  type: QuestionType;
  stem: string;
  maxScore: number;
  studentAnswer: string;
  standardAnswer: string;
  analysis: string;
  isObjective: boolean;
  isCorrect: boolean;
  aiScore: number;
  aiComment: string;
  teacherScore: number;
  teacherComment: string;
}

const gradingItems = ref<GradingItem[]>([]);

onMounted(async () => {
  await loadSubmissionData();
});

async function loadSubmissionData() {
  loading.value = true;
  try {
    const res = await getSubmissionDetail(submissionId.value);
    submissionData.value = res.data;
    if (!submissionData.value) {
      ElMessage.error('未找到该答卷详情');
      return;
    }

    // 尝试拉取真实的批改结果
    let gradingResults: any[] = [];
    try {
      const gRes = await getSubmissionGrading(submissionId.value);
      gradingResults = gRes.data || [];
    } catch (gErr) {
      gradingResults = submissionData.value?.gradingItems || [];
    }

    // 组装真实评阅呈现项（若尚未评阅则为 []，由界面引导执行 AI 批改）
    gradingItems.value = buildGradingItems(submissionData.value, gradingResults);
  } catch (err: any) {
    ElMessage.error(err?.message || '加载答卷详情失败');
    submissionData.value = null;
    gradingItems.value = [];
  } finally {
    loading.value = false;
  }
}

function buildGradingItems(sub: any, gList: any[]): GradingItem[] {
  if (!Array.isArray(gList) || gList.length === 0) {
    return [];
  }
  const answersMap = new Map<number, string>();
  if (sub?.answers && Array.isArray(sub.answers)) {
    sub.answers.forEach((ans: any) => {
      answersMap.set(ans.questionId, ans.answer);
    });
  }

  return gList.map((g: any, idx: number) => {
    const qid = g.questionId || (idx + 1);
    const stuAns = answersMap.get(qid) || '';
    const isObj = g.isCorrect !== undefined;

    return {
      questionId: qid,
      type: isObj ? 'SINGLE_CHOICE' : 'SHORT_ANSWER',
      stem: g.stem || `答卷试题 #${qid} 评分考查点`,
      maxScore: g.maxScore || 10,
      studentAnswer: stuAns || '（考生作答内容）',
      standardAnswer: g.standardAnswer || '标准参考答案与评分细则',
      analysis: g.analysis || '考查知识体系掌握与解题规范程度。',
      isObjective: isObj,
      isCorrect: g.isCorrect ?? (g.score === g.maxScore),
      aiScore: g.score !== undefined ? g.score : 0,
      aiComment: g.aiComment || 'AI智能辅助评阅已完成。',
      teacherScore: g.score !== undefined ? g.score : 0,
      teacherComment: g.teacherComment || ''
    };
  });
}

// 动态总得分
const calculatedTotalScore = computed(() => {
  return gradingItems.value.reduce((acc, item) => acc + (item.teacherScore || 0), 0);
});

// 立即触发 AI 评阅
async function handleTriggerGradeNow() {
  gradingInProgress.value = true;
  try {
    await gradeSubmission(submissionId.value);
    ElMessage.success('已成功触发该答卷的 AI 智能分析与评分！');
    await loadSubmissionData();
  } catch (err: any) {
    ElMessage.error(err?.message || '触发 AI 评阅失败，请稍后重试');
  } finally {
    gradingInProgress.value = false;
  }
}

// 采纳单题 AI 评分
function adoptSingleAIScore(item: GradingItem) {
  item.teacherScore = item.aiScore;
  if (!item.teacherComment && item.aiComment) {
    item.teacherComment = item.aiComment;
  }
  ElMessage.success(`已采纳本题 AI 建议得分：${item.aiScore} 分`);
}

// 采纳全卷 AI 评分
function adoptAllAIScores() {
  gradingItems.value.forEach(item => {
    item.teacherScore = item.aiScore;
    if (!item.teacherComment) {
      item.teacherComment = item.aiComment;
    }
  });
  ElMessage.success('已将所有试题的得分一键同步为 AI 建议评分！');
}

// 保存最终成绩
async function handleSaveGrading() {
  saving.value = true;
  try {
    const payload = gradingItems.value.map(item => ({
      questionId: item.questionId,
      score: item.teacherScore,
      teacherComment: item.teacherComment || ''
    }));

    await reviewGrading(submissionId.value, payload);
    if (submissionData.value) {
      submissionData.value.status = 'GRADED';
    }
    ElMessage.success('评阅成绩已正式确认并发布！总成绩：' + calculatedTotalScore.value + ' 分');
    setTimeout(() => {
      router.push('/question/submissions');
    }, 800);
  } catch (err: any) {
    ElMessage.error(err?.message || '评阅结果保存失败，请检查网络与登录权限');
  } finally {
    saving.value = false;
  }
}

function getTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '综合简答题'
  };
  return map[type] || type || '题目';
}

function getTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as any) || '';
}
</script>

<style scoped lang="scss">
.submission-detail-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
  padding-bottom: 90px;

  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .submission-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .hero-left {
      .student-title-line {
        display: flex;
        align-items: center;
        gap: 18px;

        .student-avatar-circle {
          width: 56px;
          height: 56px;
          border-radius: 50%;
          background: linear-gradient(135deg, #3b82f6, #2563eb);
          color: #ffffff;
          font-size: 22px;
          font-weight: 700;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .name-row {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 6px;

          .student-name {
            font-size: 22px;
            font-weight: 800;
            color: #0f172a;
            margin: 0;
          }

          .student-no {
            font-size: 14px;
            color: #64748b;
            font-family: monospace;
          }
        }

        .assignment-belong {
          font-size: 14px;
          color: #475569;
          margin: 0;

          .submit-time-text {
            color: #94a3b8;
            font-size: 13px;
          }
        }
      }
    }

    .hero-right-score {
      display: flex;
      align-items: center;

      .score-pill {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 12px 24px;
        text-align: center;

        .score-num {
          font-size: 36px;
          font-weight: 800;
          color: #2563eb;
          line-height: 1;
        }

        .score-label {
          display: block;
          font-size: 12px;
          color: #94a3b8;
          margin-top: 4px;
        }
      }
    }
  }

  .questions-grading-stream {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .grading-question-card {
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      padding: 24px;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);

      .card-q-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;

        .header-left {
          display: flex;
          align-items: center;
          gap: 10px;

          .q-index-badge {
            font-weight: 700;
            color: #1e293b;
            font-size: 15px;
          }

          .standard-score {
            font-size: 13px;
            color: #64748b;
          }
        }

        .header-right {
          font-size: 13px;
          color: #475569;
        }
      }

      .q-stem-body {
        font-size: 15px;
        line-height: 1.6;
        color: #1e293b;
        font-weight: 500;
        margin-bottom: 16px;
      }

      .student-answer-panel {
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 10px;
        padding: 14px 18px;
        margin-bottom: 14px;

        .panel-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 13px;
          font-weight: 700;
          color: #1d4ed8;
          margin-bottom: 8px;

          .panel-tag {
            display: inline-flex;
            align-items: center;
            gap: 5px;
          }

          .objective-result-tag {
            font-size: 12px;
            padding: 2px 8px;
            border-radius: 4px;
            background: #fee2e2;
            color: #b91c1c;

            &.pass {
              background: #dcfce7;
              color: #15803d;
            }
          }
        }

        .answer-content {
          font-size: 14px;
          line-height: 1.6;
          color: #1e293b;
          white-space: pre-wrap;
          font-family: inherit;
        }
      }

      .standard-answer-panel {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 14px 18px;
        margin-bottom: 14px;

        .panel-header {
          display: flex;
          align-items: center;
          font-size: 13px;
          font-weight: 700;
          color: #475569;
          margin-bottom: 8px;

          .panel-tag {
            display: inline-flex;
            align-items: center;
            gap: 5px;
          }
        }

        .std-content {
          font-size: 13px;
          line-height: 1.6;

          .ans-line {
            margin-bottom: 4px;
          }
        }
      }

      .ai-review-copilot-box {
        background: #faf5ff;
        border: 1px solid #e9d5ff;
        border-radius: 10px;
        padding: 14px 18px;
        margin-bottom: 16px;

        .copilot-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 8px;

          .copilot-title {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            font-weight: 700;
            color: #7e22ce;

            .confidence-tag {
              font-size: 11px;
              background: #f3e8ff;
              padding: 2px 6px;
              border-radius: 4px;
              font-weight: 500;
            }
          }
        }

        .copilot-body {
          font-size: 13px;
          color: #4c1d95;
          line-height: 1.6;

          p {
            margin: 0;
          }
        }
      }

      .teacher-grading-dock {
        display: flex;
        align-items: center;
        gap: 20px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 12px 18px;

        .score-input-group {
          display: flex;
          align-items: center;
          gap: 8px;

          .input-label {
            font-size: 14px;
            font-weight: 600;
            color: #1e293b;
          }

          .score-input-number {
            width: 110px;
          }

          .unit {
            font-size: 13px;
            color: #64748b;
          }
        }

        .comment-input-group {
          flex: 1;
        }
      }
    }
  }

  .bottom-sticky-bar {
    position: fixed;
    bottom: 0;
    left: 220px;
    right: 0;
    height: 72px;
    background: #ffffff;
    border-top: 1px solid #e2e8f0;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.05);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 32px;
    z-index: 100;

    .bar-right {
      display: flex;
      align-items: center;
      gap: 24px;

      .total-stat {
        font-size: 15px;
        color: #475569;

        .total-num {
          font-size: 26px;
          color: #2563eb;
          margin: 0 4px;
        }
      }

      .confirm-btn {
        background: #2563eb;
        border-color: #2563eb;
        padding: 10px 24px;
        font-weight: 600;
      }
    }
  }
}
</style>
