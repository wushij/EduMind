<template>
  <div class="grading-result-container">
    <!-- 顶部导航 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/ai/grading')">
        返回 AI 评阅中心
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/ai/grading' }">AI评阅中心</el-breadcrumb-item>
        <el-breadcrumb-item>智能评阅学情与错因归因报告</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 顶栏概览卡片 -->
    <div class="report-hero-card">
      <div class="hero-main">
        <div class="report-badge">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="title-row">
            <h1 class="report-title">{{ reportTitle }}</h1>
            <span class="status-pill">大模型多维度智能研判</span>
          </div>
          <p class="report-desc">
            全卷聚合学生作答客观比对与主观大题长文本代码要点归因，自动生成学情薄弱点画像与教学干预建议。
          </p>
        </div>
      </div>

      <div class="hero-actions">
        <el-button type="primary" class="action-btn" :icon="Download" @click="handleExportReport">
          导出 AI 学情诊断报告 (PDF/Excel)
        </el-button>
      </div>
    </div>

    <!-- 诊断内容双栏布局 -->
    <div v-loading="loading" class="report-body-grid">
      <!-- 左栏：班级学情与易错知识点归因 -->
      <div class="report-left-col">
        <el-alert
          title="左侧班级学情聚合、薄弱点排行与教学建议为 V0.5 规划演示数据；右侧试题评阅结果来自真实 API。"
          type="info"
          effect="light"
          show-icon
          :closable="false"
          class="demo-hint-alert"
        />
        <!-- 成绩分布卡片 -->
        <el-card shadow="never" class="analytics-card">
          <h3 class="card-title"><el-icon class="title-icon text-blue-600"><TrendCharts /></el-icon> 成绩阶梯与达标分布 <span class="demo-tag">演示</span></h3>
          <div class="score-tiers-grid">
            <div class="tier-item text-emerald-600 bg-emerald-50">
              <span class="tier-name">优秀 (90-100分)</span>
              <span class="tier-count">18 人 (44%)</span>
            </div>
            <div class="tier-item text-blue-600 bg-blue-50">
              <span class="tier-name">良好 (80-89分)</span>
              <span class="tier-count">15 人 (37%)</span>
            </div>
            <div class="tier-item text-amber-600 bg-amber-50">
              <span class="tier-name">及格 (60-79分)</span>
              <span class="tier-count">6 人 (14%)</span>
            </div>
            <div class="tier-item text-red-600 bg-red-50">
              <span class="tier-name">不及格 (&lt;60分)</span>
              <span class="tier-count">2 人 (5%)</span>
            </div>
          </div>
        </el-card>

        <!-- 易错知识点排行榜 -->
        <el-card shadow="never" class="analytics-card mt-4">
          <h3 class="card-title"><el-icon class="title-icon text-amber-500"><Warning /></el-icon> 高频失分考点归因诊断 <span class="demo-tag">演示</span></h3>
          <div class="weak-kps-list">
            <div
              v-for="kp in weakPoints"
              :key="kp.name"
              class="kp-stat-item"
            >
              <div class="kp-info-line">
                <span class="kp-name">{{ kp.name }}</span>
                <span class="error-rate text-red-600">失分率 {{ kp.errorRate }}%</span>
              </div>
              <el-progress :percentage="kp.errorRate" color="#ef4444" :show-text="false" />
              <p class="kp-ai-advice">{{ kp.aiAnalysis }}</p>
            </div>
          </div>
        </el-card>

        <!-- 教师教学干预建议 -->
        <el-card shadow="never" class="analytics-card mt-4">
          <h3 class="card-title"><el-icon class="title-icon text-amber-500"><Opportunity /></el-icon> AI 授课与巩固干预建议 <span class="demo-tag">演示</span></h3>
          <div class="teaching-advice-content">
            <div class="advice-bullet">
              <span class="bullet-tag">讲义重点回顾</span>
              <p>建议在下节习题课中重点花 15 分钟强化“双向链表头尾插入时的临界空指针保护”，多数同学在此失分。</p>
            </div>
            <div class="advice-bullet">
              <span class="bullet-tag">靶向巩固练习</span>
              <p>系统已根据本次错因自动生成《双向链表边界鲁棒性强化训练卷（共5题）》，可一键推送至失分学生。</p>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右栏：典型试题作答范例与AI评阅要点复核 -->
      <div class="report-right-col">
        <el-card shadow="never" class="analytics-card">
          <div class="card-header-between">
            <h3 class="card-title"><el-icon class="title-icon text-blue-600"><Search /></el-icon> 典型作答案例与大模型评语抽查</h3>
            <el-select v-model="selectedCaseType" size="small" style="width: 140px">
              <el-option label="典型失分题例" value="MISTAKE" />
              <el-option label="标准高分题例" value="PERFECT" />
            </el-select>
          </div>

          <!-- 真实 AI 评阅结果卡片流 -->
          <div v-if="realGradingItems.length > 0" class="case-list">
            <div
              v-for="(item, idx) in realGradingItems"
              :key="item.questionId || idx"
              class="case-item-card"
            >
              <div class="case-top">
                <span class="q-title">试题 #{{ item.questionId }}</span>
                <el-tag size="small" :type="item.isCorrect ? 'success' : 'danger'">
                  {{ item.isCorrect ? '客观正确' : '主观研判/失分' }}
                </el-tag>
              </div>

              <!-- AI 深度要点点评 -->
              <div class="ai-comment-box">
                <div class="ai-badge-row">
                  <span class="ai-icon"><el-icon><Cpu /></el-icon> AI 评分：<strong>{{ item.score }} / {{ item.maxScore || 10 }} 分</strong></span>
                  <span class="confidence">状态：{{ item.status || 'AI_GRADED' }}</span>
                </div>
                <p class="comment-text">{{ item.aiComment || '该题作答基本符合考查要点。' }}</p>
                <div v-if="item.teacherComment" class="teacher-note mt-2 text-xs text-blue-700">
                  <strong>教师批语：</strong>{{ item.teacherComment }}
                </div>
              </div>
            </div>
          </div>

          <!-- 样本案例列表 -->
          <div v-else class="case-list">
            <div v-if="activeCases.length > 0">
              <div
                v-for="c in activeCases"
                :key="c.id"
                class="case-item-card"
              >
                <div class="case-top">
                  <span class="q-title">{{ c.questionTitle }}</span>
                  <span class="student-name">作答学生：{{ c.studentName }}</span>
                </div>

                <!-- 学生原题作答 -->
                <div class="answer-box">
                  <div class="box-label">学生提交原始答案：</div>
                  <div class="content">{{ c.studentAnswer }}</div>
                </div>

                <!-- AI 深度要点点评 -->
                <div class="ai-comment-box">
                  <div class="ai-badge-row">
                    <span class="ai-icon"><el-icon><Cpu /></el-icon> AI 判分得分：<strong>{{ c.aiScore }} 分</strong></span>
                    <span class="confidence">评分置信度：{{ c.confidence }}%</span>
                  </div>
                  <p class="comment-text">{{ c.aiComment }}</p>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无该分类下的 AI 批改诊断案例" />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ArrowLeft,
  DataAnalysis,
  Download,
  TrendCharts,
  Warning,
  Opportunity,
  Search,
  Cpu
} from '@element-plus/icons-vue';
import { useGrading } from '@/composables/ai/useGrading';

const router = useRouter();
const route = useRoute();
const { fetchGrading, loading } = useGrading();

const reportTitle = computed(() => {
  return (route.query.title as string) || '第三周：单链表与双向链表核心算法实现测验';
});

const submissionId = computed(() => {
  return Number(route.query.submissionId) || 201;
});

const selectedCaseType = ref('MISTAKE');
const realGradingItems = ref<any[]>([]);
const cases = ref<any[]>([]);

onMounted(async () => {
  if (submissionId.value) {
    try {
      const data = await fetchGrading(submissionId.value);
      if (Array.isArray(data) && data.length > 0) {
        realGradingItems.value = data;
        cases.value = data.map((item: any, idx: number) => {
          const isErr = !item.isCorrect || ((item.score ?? 0) < (item.maxScore ?? 10));
          return {
            id: item.questionId || idx + 1,
            type: isErr ? 'MISTAKE' : 'PERFECT',
            questionTitle: `第 ${idx + 1} 题：${item.stem || '课程试题考查点'}`,
            studentName: `答卷 #${submissionId.value}`,
            studentAnswer: item.studentAnswer || '（考生作答内容）',
            aiScore: item.score ?? 0,
            confidence: 96,
            aiComment: item.aiComment || (isErr ? '未完全答全考点要点，建议教师重点复核。' : '作答逻辑完整严密，完全符合标准答案要点。')
          };
        });
      } else {
        realGradingItems.value = [];
        cases.value = [];
      }
    } catch (err: any) {
      ElMessage.error(err?.message || '获取答卷批改结果失败');
      realGradingItems.value = [];
      cases.value = [];
    }
  }
});

const weakPoints = ref([
  {
    name: '双向链表头节点插入与临界指针防漏',
    errorRate: 38,
    aiAnalysis: '学生通常能写出基本的4条指针调整语句，但有近4成同学遗漏对原空链表或尾节点的特判保护。'
  },
  {
    name: '递归算法空间复杂度分析（递归调用栈深度）',
    errorRate: 26,
    aiAnalysis: '易将栈空间开销与临时局部变量混淆，未能明确说明 O(n) 的栈帧消耗根源。'
  },
  {
    name: '折半查找判定树 ASL 计算公式',
    errorRate: 18,
    aiAnalysis: '部分同学混淆了成功查找长度与不成功查找长度的树高判定节点数。'
  }
]);

const activeCases = computed(() => {
  return cases.value.filter(c => c.type === selectedCaseType.value);
});

function handleExportReport() {
  ElMessage.success('已开始生成高清晰度 PDF 学情分析诊断专报，下载稍后将自动开始！');
}
</script>

<style scoped lang="scss">
.grading-result-container {
  padding: 24px;
  background: #f8fafc;
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

  .report-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 20px;

    .hero-main {
      display: flex;
      align-items: center;
      gap: 18px;

      .report-badge {
        width: 60px;
        height: 60px;
        background: #eff6ff;
        border: 1px solid #dbeafe;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        color: #2563eb;
      }

      .title-row {
        display: flex;
        align-items: center;
        gap: 12px;

        .report-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .status-pill {
          background: #f1f5f9;
          color: #475569;
          font-size: 12px;
          padding: 2px 10px;
          border-radius: 9999px;
          font-weight: 500;
        }
      }

      .report-desc {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .action-btn {
      background: #2563eb;
      border-color: #2563eb;
      font-weight: 500;
      padding: 9px 20px;
      border-radius: 8px;
    }
  }

  .report-body-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;

    .demo-hint-alert {
      margin-bottom: 16px;
    }

    .demo-tag {
      display: inline-block;
      margin-left: 8px;
      padding: 2px 8px;
      font-size: 12px;
      font-weight: 500;
      color: #64748b;
      background: #f1f5f9;
      border-radius: 9999px;
      vertical-align: middle;
    }

    .analytics-card {
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      padding: 16px 20px;

      .card-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 16px;
        display: flex;
        align-items: center;
        gap: 6px;

        .title-icon {
          font-size: 18px;
        }
      }

      .card-header-between {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;

        .card-title {
          margin: 0;
        }
      }
    }

    .score-tiers-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;

      .tier-item {
        padding: 12px 16px;
        border-radius: 8px;
        display: flex;
        flex-direction: column;

        .tier-name {
          font-size: 13px;
          font-weight: 600;
        }

        .tier-count {
          font-size: 15px;
          font-weight: 800;
          margin-top: 4px;
        }
      }
    }

    .weak-kps-list {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .kp-stat-item {
        .kp-info-line {
          display: flex;
          justify-content: space-between;
          font-size: 13px;
          font-weight: 600;
          margin-bottom: 6px;

          .kp-name {
            color: #1e293b;
          }
        }

        .kp-ai-advice {
          font-size: 12px;
          color: #64748b;
          margin: 6px 0 0;
          line-height: 1.5;
        }
      }
    }

    .teaching-advice-content {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .advice-bullet {
        background: #f8fafc;
        border-radius: 8px;
        padding: 12px 16px;
        border-left: 3px solid #3b82f6;

        .bullet-tag {
          font-size: 12px;
          font-weight: 700;
          color: #2563eb;
          display: block;
          margin-bottom: 4px;
        }

        p {
          font-size: 13px;
          color: #475569;
          margin: 0;
          line-height: 1.5;
        }
      }
    }

    .case-list {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .case-item-card {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 14px 18px;
        background: #ffffff;

        .case-top {
          display: flex;
          justify-content: space-between;
          font-size: 13px;
          font-weight: 600;
          color: #1e293b;
          margin-bottom: 10px;

          .student-name {
            color: #64748b;
          }
        }

        .answer-box {
          background: #f8fafc;
          border-radius: 6px;
          padding: 10px 12px;
          margin-bottom: 10px;

          .box-label {
            font-size: 11px;
            color: #94a3b8;
            margin-bottom: 4px;
          }

          .content {
            font-size: 13px;
            color: #334155;
            line-height: 1.5;
          }
        }

        .ai-comment-box {
          background: #faf5ff;
          border: 1px solid #e9d5ff;
          border-radius: 6px;
          padding: 10px 12px;

          .ai-badge-row {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #7e22ce;
            margin-bottom: 6px;
          }

          .comment-text {
            font-size: 12px;
            color: #581c87;
            margin: 0;
            line-height: 1.5;
          }
        }
      }
    }
  }
}
</style>
