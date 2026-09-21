<template>
  <div class="student-portrait-view">
    <!-- 学生基本信息卡片（带学员切换器） -->
    <div class="student-hero-card">
      <div class="hero-left-profile">
        <el-avatar :size="64" :src="portrait?.studentInfo.avatar" class="large-student-avatar">
          {{ portrait?.studentInfo.realName?.slice(0, 1) || '学' }}
        </el-avatar>

        <div class="profile-info-group">
          <div class="name-status-row">
            <h2 class="student-real-name">{{ portrait?.studentInfo.realName || '学员画像' }}</h2>
            <span class="student-id-pill">{{ portrait?.studentInfo.studentNo || `STU-${portrait?.studentInfo.studentId}` }}</span>
            <span class="role-status-pill">
              <span class="dot"></span>
              <span>{{ portrait?.studentInfo.role || '在册学员' }}</span>
            </span>
          </div>

          <div class="meta-inline-row">
            <span class="meta-item">班级：<strong>{{ portrait?.studentInfo.className || '未分配行政班' }}</strong></span>
            <span class="divider">/</span>
            <span class="meta-item">用户名：<strong>{{ portrait?.studentInfo.username }}</strong></span>
            <span class="divider">/</span>
            <span class="meta-item">最近活跃：<strong>{{ portrait?.studentInfo.lastActiveTime || '今日' }}</strong></span>
          </div>
        </div>
      </div>

      <div v-if="isTeacherVariant" class="hero-right-controls">
        <div class="student-switch-box">
          <span class="switch-label">切换诊断学员：</span>
          <el-select
            :model-value="portrait?.studentInfo.studentId"
            placeholder="切换选课学生"
            class="student-switch-select"
            @change="handleStudentSelect"
          >
            <el-option
              v-for="s in studentOptions"
              :key="s.studentId"
              :label="`${s.realName} (${s.studentNo || s.username})`"
              :value="s.studentId"
            />
          </el-select>
        </div>

        <button
          type="button"
          class="capsule-btn capsule-btn--default back-overall-btn"
          @click="emit('back-overall')"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          <span>返回班级整体分析</span>
        </button>
      </div>

      <div v-else class="hero-right-controls student-action-row">
        <button type="button" class="capsule-btn capsule-btn--default" @click="emit('go-wrong-book')">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
          <span>错题本</span>
        </button>
        <button type="button" class="capsule-btn capsule-btn--primary" @click="emit('go-practice')">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
          <span>AI 练习</span>
        </button>
      </div>
    </div>

    <!-- 个人 4 项核心 KPI 指标卡片 -->
    <div class="portrait-kpi-grid">
      <div class="portrait-kpi-card">
        <span class="kpi-tag">在线学时</span>
        <div class="kpi-val-row">
          <strong class="kpi-val text-primary">{{ portrait?.summary.totalStudyMinutes ?? 0 }}</strong>
          <span class="kpi-unit">分钟</span>
        </div>
        <div class="kpi-sub-tip">
          <span>班级均值：{{ portrait?.summary.classAvgStudyMinutes?.toFixed(0) ?? 80 }} 分钟</span>
          <span v-if="portrait?.summary.totalStudyMinutesAllTime != null">
            · 累计 {{ portrait.summary.totalStudyMinutesAllTime }} 分钟
          </span>
        </div>
      </div>

      <div class="portrait-kpi-card">
        <span class="kpi-tag">作业平均得分</span>
        <div class="kpi-val-row">
          <strong class="kpi-val text-success">{{ portrait?.summary.avgScore?.toFixed(1) ?? '-' }}</strong>
          <span class="kpi-unit">分</span>
        </div>
        <div class="kpi-sub-tip">
          <span>班级均值：{{ portrait?.summary.classAvgScore?.toFixed(1) ?? 82.0 }} 分</span>
        </div>
      </div>

      <div class="portrait-kpi-card">
        <span class="kpi-tag">知识图谱达成度</span>
        <div class="kpi-val-row">
          <strong class="kpi-val text-indigo">{{ portrait?.summary.overallMastery?.toFixed(1) ?? '-' }}%</strong>
        </div>
        <div class="kpi-sub-tip">
          <span>达成评级：{{ getMasteryGrade(portrait?.summary.overallMastery) }}</span>
        </div>
      </div>

      <div class="portrait-kpi-card">
        <span class="kpi-tag">AI 助教答疑互动</span>
        <div class="kpi-val-row">
          <strong class="kpi-val text-amber">{{ portrait?.summary.aiUsageCount ?? 0 }}</strong>
          <span class="kpi-unit">次提问</span>
        </div>
        <div class="kpi-sub-tip">
          <span>当前累计错题：{{ portrait?.summary.wrongQuestionCount ?? 0 }} 题</span>
        </div>
      </div>
    </div>

    <div v-if="!isTeacherVariant && personalTrends" class="content-panel trend-panel">
      <div class="panel-header">
        <div class="header-title-group">
          <span class="title-decor-pill title-decor-pill--green"></span>
          <h3 class="panel-title">个人学习趋势</h3>
        </div>
        <span class="panel-tag">区间内学习分钟与作业均分</span>
      </div>
      <LearningChart mode="personal" :personal-trends="personalTrends" height="320px" />
    </div>

    <!-- 知识体系雷达与薄弱点突破 -->
    <div class="diagnostic-grid-row">
      <!-- 个人知识点雷达图 -->
      <div class="content-panel radar-panel">
        <div class="panel-header">
          <div class="header-title-group">
            <span class="title-decor-pill"></span>
            <h3 class="panel-title">个人知识结构雷达对比</h3>
          </div>
          <span class="panel-tag">个人得分 vs 班级基准</span>
        </div>
        <div class="radar-wrap">
          <KnowledgeRadar :data="radarAdapterData" height="340px" />
        </div>
      </div>

      <!-- 薄弱考点预警与突破卡片 -->
      <div class="content-panel weak-panel">
        <div class="panel-header">
          <div class="header-title-group">
            <span class="title-decor-pill title-decor-pill--danger"></span>
            <h3 class="panel-title">薄弱考点预警与突破建议</h3>
            <span v-if="portrait?.weakPoints?.length" class="weak-count-badge">
              {{ portrait.weakPoints.length }} 个薄弱项
            </span>
          </div>
          <span class="panel-tag">考点 < 70% 自动预警</span>
        </div>

        <div class="weak-list-body">
          <div v-if="portrait?.weakPoints?.length" class="weak-cards-wrap">
            <div v-for="wp in portrait.weakPoints" :key="wp.knowledgePointId" class="weak-point-card">
              <div class="card-top-line">
                <span class="point-title">{{ wp.title }}</span>
                <span class="score-pill-danger">掌握度：{{ wp.mastery }}%</span>
              </div>
              <p class="suggestion-text">
                <svg viewBox="0 0 24 24" class="suggest-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
                </svg>
                <span>{{ wp.suggestion || '建议进行针对性变式巩固' }}</span>
              </p>
            </div>
          </div>

          <div v-else class="weak-empty-box">
            <svg viewBox="0 0 24 24" class="success-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <h4>太棒了！暂无薄弱预警考点</h4>
            <p>该生所有知识点掌握度均在 70% 以上，可进行拔高挑战练习。</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 个人错题归因与易错题清单 -->
    <div class="content-panel wrong-panel">
      <div class="panel-header">
        <div class="header-title-group">
          <span class="title-decor-pill title-decor-pill--amber"></span>
          <h3 class="panel-title">个人易错题目归因诊断明细</h3>
        </div>
        <span class="panel-tag">最近错题与 AI 归因</span>
      </div>

      <div class="wrong-table-wrap">
        <table v-if="portrait?.wrongQuestions?.length" class="wrong-table">
          <thead>
            <tr>
              <th style="width: 38%;">题目题干摘要</th>
              <th>关联考点</th>
              <th>归因类型</th>
              <th>AI 错因诊断</th>
              <th>重做频次</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="w in portrait.wrongQuestions" :key="w.recordId">
              <td>
                <div class="stem-text">{{ w.questionStem || `题目 #${w.questionId}` }}</div>
              </td>
              <td>
                <span class="kp-pill">{{ w.knowledgePointTitle }}</span>
              </td>
              <td>
                <span class="error-type-tag">{{ formatErrorTypes(w.errorTypes) }}</span>
              </td>
              <td>
                <p class="diagnosis-text">{{ w.diagnosis || '概念细节理解存在轻微偏差' }}</p>
              </td>
              <td>
                <span class="wrong-count-tag">{{ w.wrongCount }} 次</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty-data-row">
          <p>该学员在本课程中暂无错题记录，保持得很好！</p>
        </div>
      </div>
    </div>

    <!-- 自适应推荐学习周计划 -->
    <div v-if="portrait?.adaptiveWeeks?.length" class="content-panel adaptive-panel">
      <div class="panel-header">
        <div class="header-title-group">
          <span class="title-decor-pill title-decor-pill--green"></span>
          <h3 class="panel-title">自适应推荐学习与提分周计划</h3>
        </div>
        <span class="panel-tag">基于掌握度智能编排</span>
      </div>

      <div class="adaptive-weeks-grid">
        <div v-for="wk in portrait.adaptiveWeeks" :key="wk.weekNo" class="week-card">
          <div class="week-header">
            <span class="week-badge">第 {{ wk.weekNo }} 周计划</span>
            <span class="week-theme">{{ wk.theme }}</span>
          </div>

          <div class="week-tasks-list">
            <div v-for="(t, idx) in wk.tasks" :key="idx" class="task-item">
              <span class="task-dot" :class="{ 'task-dot--done': t.status === 'COMPLETED' }"></span>
              <span class="task-title" :class="{ 'task-title--done': t.status === 'COMPLETED' }">{{ t.title }}</span>
              <span class="task-status-tag" :class="t.status === 'COMPLETED' ? 'tag--done' : 'tag--pending'">
                {{ t.status === 'COMPLETED' ? '已完成' : '待完成' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI 综合学情导师评语卡片 -->
    <div class="content-panel ai-feedback-panel">
      <div class="ai-feedback-header">
        <div class="ai-header-left">
          <div class="ai-avatar-circle">
            <svg viewBox="0 0 24 24" class="ai-svg" fill="currentColor">
              <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
            </svg>
          </div>
          <div class="ai-header-title">
            <h4>AI 智教学情导师综合评价与导学建议</h4>
            <p>基于大模型深度融合该生的在线打卡、错题归因与考点雷达多维数据生成</p>
          </div>
        </div>

        <div class="ai-header-actions">
          <button
            v-if="adviceLoading"
            type="button"
            class="ai-action-capsule-btn ai-action-capsule-btn--warning"
            title="停止本次 AI 推演"
            @click.stop="emit('stop-advice')"
          >
            <svg viewBox="0 0 24 24" class="btn-svg is-spin" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="6" y="6" width="12" height="12" rx="2"></rect>
            </svg>
            <span>停止推演</span>
          </button>

          <template v-else>
            <button
              type="button"
              class="ai-action-capsule-btn ai-action-capsule-btn--primary"
              @click="emit('generate-advice')"
            >
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
              </svg>
              <span>{{ portrait?.aiDiagnosis ? '重新推演' : '生成精准诊断建议' }}</span>
            </button>

            <button
              type="button"
              class="ai-action-capsule-btn ai-action-capsule-btn--outline"
              @click="emit('open-diagnosis-drawer')"
            >
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
              <span>深度干预处方</span>
            </button>

            <button
              v-if="portrait?.aiDiagnosis"
              type="button"
              class="ai-action-capsule-btn ai-action-capsule-btn--danger"
              title="删除当前建议"
              @click="handleClearAdvice"
            >
              <span>删除建议</span>
            </button>
          </template>
        </div>
      </div>

      <div
        class="ai-feedback-content"
        v-loading="adviceLoading"
        element-loading-text="AI 深度学情诊断推演中..."
      >
        <p>{{ portrait?.aiDiagnosis || '暂无该学员的 AI 诊断建议，点击上方「生成精准诊断建议」获取基于 DeepSeek 大模型的深度推演分析。' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElMessageBox } from 'element-plus';
import KnowledgeRadar from '@/components/analytics/KnowledgeRadar.vue';
import LearningChart from '@/components/analytics/LearningChart.vue';
import type { StudentPortraitVO, StudentLearningItemVO } from '@/types/analytics/learning';
import type { KnowledgeMasteryVO } from '@/types/analytics/mastery';
import type { LearningReportTrendsVO } from '@/types/learning/report';

const props = withDefaults(
  defineProps<{
    portrait: StudentPortraitVO | null;
    studentOptions?: StudentLearningItemVO[];
    adviceLoading?: boolean;
    variant?: 'teacher' | 'student';
    personalTrends?: LearningReportTrendsVO | null;
    reportCode?: string;
  }>(),
  {
    studentOptions: () => [],
    adviceLoading: false,
    variant: 'teacher',
    personalTrends: null,
    reportCode: ''
  }
);

const isTeacherVariant = computed(() => props.variant === 'teacher');

const emit = defineEmits<{
  (e: 'switch-student', studentId: number): void;
  (e: 'back-overall'): void;
  (e: 'generate-advice'): void;
  (e: 'clear-advice'): void;
  (e: 'open-diagnosis-drawer'): void;
  (e: 'stop-advice'): void;
  (e: 'go-practice'): void;
  (e: 'go-wrong-book'): void;
}>();

function handleClearAdvice() {
  ElMessageBox.confirm('确定清空该学员当前生成的 AI 诊断建议吗？', '清空建议确认', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('clear-advice');
  }).catch(() => {});
}

function handleStudentSelect(val: any) {
  if (val != null) {
    emit('switch-student', Number(val));
  }
}

const radarAdapterData = computed<KnowledgeMasteryVO>(() => {
  if (!props.portrait?.radar) {
    return {
      dimensions: [],
      personal: [],
      classAvg: [],
      weakPoints: []
    };
  }
  return {
    dimensions: props.portrait.radar.dimensions,
    personal: props.portrait.radar.personalScores,
    classAvg: props.portrait.radar.classAvgScores,
    weakPoints: []
  };
});

function getMasteryGrade(score?: number) {
  if (score == null) return '-';
  if (score >= 85) return '卓越 (A+)';
  if (score >= 75) return '优良 (B+)';
  if (score >= 60) return '达标 (C)';
  return '需加强 (D)';
}

function formatErrorTypes(types?: string) {
  if (!types) return '概念混淆';
  return types.replace(/CONCEPT/g, '概念混淆')
              .replace(/LOGIC/g, '逻辑推理')
              .replace(/CALCULATION/g, '计算失误')
              .replace(/,/g, ' / ');
}
</script>

<style scoped lang="scss">
.student-portrait-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.student-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.trend-panel {
  margin-top: 0;
}

// 学生头部卡片
.student-hero-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 20px 24px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;

  .hero-left-profile {
    display: flex;
    align-items: center;
    gap: 16px;

    .large-student-avatar {
      background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
      color: #FFFFFF;
      font-size: 24px;
      font-weight: 800;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
    }

    .profile-info-group {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .name-status-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .student-real-name {
          margin: 0;
          font-size: 20px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.3px;
        }

        .student-id-pill {
          font-size: 12px;
          font-family: monospace;
          font-weight: 700;
          padding: 2px 8px;
          border-radius: 9999px;
          background: #F1F5F9;
          color: #475569;
        }

        .role-status-pill {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 2px 10px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          background: #E6F7ED;
          color: #16A34A;

          .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #16A34A;
          }
        }
      }

      .meta-inline-row {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: #64748B;

        .divider {
          color: #CBD5E1;
        }

        strong {
          color: #1E293B;
        }
      }
    }
  }

  .hero-right-controls {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .student-switch-box {
      display: flex;
      align-items: center;
      gap: 8px;

      .switch-label {
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
        white-space: nowrap;
      }

      .student-switch-select {
        width: 190px;

        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          background: #F8FAFC;
        }
      }
    }

    .back-overall-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0 16px;
      height: 36px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      background: #FFFFFF;
      color: #334155;
      border: 1px solid rgba(203, 213, 225, 0.8);
      transition: all 0.2s ease;

      .btn-icon-svg {
        width: 14px;
        height: 14px;
      }

      &:hover {
        background: #F8FAFC;
        color: #0F172A;
        border-color: #94A3B8;
        transform: translateY(-1px);
      }
    }
  }
}

// 4 项个人 KPI 卡片
.portrait-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  .portrait-kpi-card {
    background: #FFFFFF;
    border-radius: 20px;
    padding: 18px 20px;
    border: 1px solid rgba(226, 232, 240, 0.9);
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
    display: flex;
    flex-direction: column;
    gap: 6px;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
    }

    .kpi-tag {
      font-size: 13px;
      color: #64748B;
      font-weight: 600;
    }

    .kpi-val-row {
      display: flex;
      align-items: baseline;
      gap: 4px;

      .kpi-val {
        font-size: 24px;
        font-weight: 800;
        letter-spacing: -0.4px;

        &.text-primary { color: #1677FF; }
        &.text-success { color: #16A34A; }
        &.text-indigo { color: #6366F1; }
        &.text-amber { color: #D97706; }
      }

      .kpi-unit {
        font-size: 12px;
        color: #94A3B8;
        font-weight: 600;
      }
    }

    .kpi-sub-tip {
      font-size: 11px;
      color: #94A3B8;
    }
  }
}

// 雷达图与薄弱点行
.diagnostic-grid-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;

  .radar-wrap {
    padding: 10px;
  }

  .weak-list-body {
    padding: 16px 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .weak-cards-wrap {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .weak-point-card {
        padding: 14px 16px;
        border-radius: 14px;
        background: #FEF2F2;
        border: 1px solid #FEE2E2;
        display: flex;
        flex-direction: column;
        gap: 6px;

        .card-top-line {
          display: flex;
          align-items: center;
          justify-content: space-between;

          .point-title {
            font-size: 14px;
            font-weight: 700;
            color: #991B1B;
          }

          .score-pill-danger {
            font-size: 12px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 9999px;
            background: #FEE2E2;
            color: #DC2626;
          }
        }

        .suggestion-text {
          margin: 0;
          font-size: 12px;
          color: #B91C1C;
          display: flex;
          align-items: center;
          gap: 6px;

          .suggest-svg {
            width: 13px;
            height: 13px;
            flex-shrink: 0;
          }
        }
      }
    }

    .weak-empty-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
      text-align: center;
      gap: 8px;

      .success-icon-svg {
        width: 36px;
        height: 36px;
        color: #10B981;
      }

      h4 {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
        color: #0F172A;
      }

      p {
        margin: 0;
        font-size: 12px;
        color: #64748B;
      }
    }
  }
}

// 统一内容面板容器
.content-panel {
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  overflow: hidden;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 22px;
    border-bottom: 1px solid #F1F5F9;

    .header-title-group {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-decor-pill {
        width: 4px;
        height: 16px;
        border-radius: 9999px;
        background: #1677FF;

        &--danger { background: #EF4444; }
        &--amber { background: #F59E0B; }
        &--green { background: #10B981; }
      }

      .panel-title {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
      }

      .weak-count-badge {
        font-size: 12px;
        padding: 2px 10px;
        border-radius: 9999px;
        background: #FEF2F2;
        color: #DC2626;
        font-weight: 700;
      }
    }

    .panel-tag {
      font-size: 12px;
      padding: 3px 10px;
      border-radius: 9999px;
      background: #F1F5F9;
      color: #64748B;
      font-weight: 600;
    }
  }
}

// 错题表格
.wrong-panel {
  .wrong-table-wrap {
    width: 100%;
    overflow-x: auto;

    .wrong-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;

      th {
        padding: 12px 22px;
        font-size: 12px;
        font-weight: 600;
        color: #64748B;
        background: #F8FAFC;
        border-bottom: 1px solid #E2E8F0;
      }

      td {
        padding: 14px 22px;
        border-bottom: 1px solid #F1F5F9;
        vertical-align: middle;
        font-size: 13px;
        color: #334155;
      }

      .stem-text {
        font-weight: 600;
        color: #0F172A;
        line-height: 1.4;
      }

      .kp-pill {
        display: inline-block;
        padding: 3px 10px;
        border-radius: 9999px;
        background: #EFF6FF;
        color: #1E40AF;
        font-size: 12px;
        font-weight: 600;
      }

      .error-type-tag {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 9999px;
        background: #FEF2F2;
        color: #DC2626;
        font-size: 11px;
        font-weight: 700;
      }

      .diagnosis-text {
        margin: 0;
        font-size: 12px;
        color: #64748B;
        line-height: 1.4;
      }

      .wrong-count-tag {
        font-size: 12px;
        font-weight: 700;
        color: #DC2626;
      }
    }

    .empty-data-row {
      padding: 30px;
      text-align: center;
      color: #94A3B8;
      font-size: 13px;
    }
  }
}

// 自适应周计划卡片
.adaptive-panel {
  .adaptive-weeks-grid {
    padding: 18px 22px;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    .week-card {
      background: #F8FAFC;
      border-radius: 16px;
      padding: 16px;
      border: 1px solid #E2E8F0;
      display: flex;
      flex-direction: column;
      gap: 12px;

      .week-header {
        display: flex;
        align-items: center;
        gap: 10px;

        .week-badge {
          font-size: 12px;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #10B981;
          color: #FFFFFF;
          font-weight: 700;
        }

        .week-theme {
          font-size: 14px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      .week-tasks-list {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .task-item {
          display: flex;
          align-items: center;
          gap: 8px;
          background: #FFFFFF;
          padding: 8px 12px;
          border-radius: 10px;
          font-size: 13px;

          .task-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #CBD5E1;

            &--done {
              background: #10B981;
            }
          }

          .task-title {
            flex: 1;
            color: #334155;

            &--done {
              color: #94A3B8;
              text-decoration: line-through;
            }
          }

          .task-status-tag {
            font-size: 11px;
            padding: 1px 8px;
            border-radius: 9999px;
            font-weight: 600;

            &.tag--done {
              background: #DCFCE7;
              color: #15803D;
            }

            &.tag--pending {
              background: #F1F5F9;
              color: #64748B;
            }
          }
        }
      }
    }
  }
}

// AI 综合导师反馈面板
.ai-feedback-panel {
  padding: 20px 24px;
  background: linear-gradient(135deg, #F0FDF4 0%, #EEF2FF 100%);
  border: 1px solid #C7D2FE;

  .ai-feedback-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    margin-bottom: 14px;
    flex-wrap: wrap;

    .ai-header-left {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .ai-header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .ai-action-capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 16px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      border: 1px solid transparent;

      .btn-svg {
        width: 14px;
        height: 14px;
      }

      &--primary {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        color: #FFFFFF;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);

        &:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
        }

        &:disabled {
          opacity: 0.65;
          cursor: not-allowed;
        }
      }

      &--outline {
        background: #FFFFFF;
        color: #4F46E5;
        border-color: #C7D2FE;

        &:hover {
          background: #EEF2FF;
          border-color: #818CF8;
          transform: translateY(-1px);
        }
      }

      &--warning {
        background: #FFFBEB;
        color: #D97706;
        border-color: #FCD34D;

        &:hover {
          background: #FEF3C7;
          color: #B45309;
          border-color: #F59E0B;
          transform: translateY(-1px);
        }
      }

      &--danger {
        background: #FFF1F0;
        color: #FF4D4F;
        border-color: #FFA39E;

        &:hover {
          background: #FFCCC7;
          color: #CF1322;
          border-color: #F5222D;
          transform: translateY(-1px);
        }
      }
    }

    .ai-avatar-circle {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, #10B981 0%, #3B82F6 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #FFFFFF;
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);

      .ai-svg {
        width: 22px;
        height: 22px;
      }
    }

    .ai-header-title {
      h4 {
        margin: 0 0 2px;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
      }

      p {
        margin: 0;
        font-size: 12px;
        color: #64748B;
      }
    }
  }

  .ai-feedback-content {
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(8px);
    border-radius: 14px;
    padding: 16px 20px;
    border: 1px solid rgba(255, 255, 255, 0.9);
    position: relative;
    min-height: 80px;

    p {
      margin: 0;
      font-size: 14px;
      color: #1E293B;
      line-height: 1.65;
    }
  }
}

@media (max-width: 960px) {
  .portrait-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .diagnostic-grid-row {
    grid-template-columns: 1fr;
  }

  .adaptive-panel .adaptive-weeks-grid {
    grid-template-columns: 1fr;
  }
}
</style>
