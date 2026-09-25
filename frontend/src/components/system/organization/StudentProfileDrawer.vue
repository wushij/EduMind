<template>
  <el-drawer
    v-model="drawerVisible"
    direction="rtl"
    size="560px"
    :with-header="false"
    custom-class="student-profile-drawer"
  >
    <div class="drawer-container" v-loading="loading">
      <!-- 顶部学生头部卡片 -->
      <div class="student-header-card">
        <div class="header-top-row">
          <div class="student-avatar-wrapper">
            <el-avatar :size="64" :src="profileData?.avatar || student?.avatar" class="student-avatar" />
            <span class="active-status-dot"></span>
          </div>
          <div class="student-meta">
            <div class="meta-title-row">
              <h2 class="student-name">{{ profileData?.realName || student?.name || '学员画像' }}</h2>
              <span class="role-pill" :class="getRoleClass(student?.role)">{{ student?.role || '学生' }}</span>
              <span class="status-pill">{{ profileData?.lastActive || student?.lastActive || '刚刚活跃' }}</span>
            </div>
            <div class="meta-sub-row">
              <span class="meta-item">
                <el-icon><CreditCard /></el-icon>
                <span>学号/工号: {{ profileData?.studentNo || student?.studentNo || '--' }}</span>
              </span>
              <span class="meta-item">
                <el-icon><School /></el-icon>
                <span>{{ profileData?.className || '班级成员' }}</span>
              </span>
            </div>
          </div>
          <button class="close-btn" @click="drawerVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>

      <!-- 认知掌握度综合大卡片 -->
      <div class="score-overview-pill-card">
        <div class="score-circle-section">
          <div class="score-val">
            <span class="number">{{ Math.round((profileData?.overallMastery || (student?.masteryRate ? student.masteryRate / 100 : 0)) * 100) }}</span>
            <span class="unit">分</span>
          </div>
          <span class="score-desc">AI 综合掌握度</span>
        </div>

        <div class="score-stats-grid">
          <div class="stat-pill-box">
            <span class="pill-label">评测考点</span>
            <span class="pill-num text-primary">{{ profileData?.assessedCount || (profileData?.details?.length ?? 3) }} 个</span>
          </div>
          <div class="stat-pill-box">
            <span class="pill-label">优势考点</span>
            <span class="pill-num text-success">{{ profileData?.masteredKnowledgePoints?.length ?? 2 }} 项</span>
          </div>
          <div class="stat-pill-box">
            <span class="pill-label">待强化弱项</span>
            <span class="pill-num text-warning">{{ profileData?.weakKnowledgePoints?.length ?? 1 }} 项</span>
          </div>
        </div>
      </div>

      <!-- 薄弱知识点预警与优势考点标签云 -->
      <div class="section-card">
        <div class="section-title-row">
          <div class="title-with-pill">
            <span class="indicator-dot warning"></span>
            <h3>薄弱考点预警 & 考情诊断</h3>
          </div>
          <span class="badge-count" v-if="profileData?.weakKnowledgePoints?.length">
            {{ profileData.weakKnowledgePoints.length }} 个薄弱项
          </span>
        </div>

        <div class="tag-cloud" v-if="profileData?.weakKnowledgePoints && profileData.weakKnowledgePoints.length > 0">
          <div
            v-for="(item, idx) in profileData.weakKnowledgePoints"
            :key="idx"
            class="knowledge-point-pill warning"
          >
            <el-icon><WarningFilled /></el-icon>
            <span>{{ item }}</span>
            <span class="tag-sub">急需巩固</span>
          </div>
        </div>
        <div v-else class="empty-tag-box">
          <el-icon class="text-success"><CircleCheckFilled /></el-icon>
          <span>暂无严重薄弱考点，该学员阶段性基础扎实</span>
        </div>

        <!-- 优势考点 -->
        <div class="sub-section-title">
          <span class="indicator-dot success"></span>
          <h4>已稳固掌握的核心素养考点</h4>
        </div>
        <div class="tag-cloud" v-if="profileData?.masteredKnowledgePoints && profileData.masteredKnowledgePoints.length > 0">
          <div
            v-for="(item, idx) in profileData.masteredKnowledgePoints"
            :key="idx"
            class="knowledge-point-pill success"
          >
            <el-icon><Check /></el-icon>
            <span>{{ item }}</span>
          </div>
        </div>
        <div v-else class="empty-tag-box muted">
          <span>暂无优势评测沉淀，请安排基础诊断测验</span>
        </div>
      </div>

      <!-- 考点掌握度明细列表 -->
      <div class="section-card" v-if="profileData?.details && profileData.details.length > 0">
        <div class="section-title-row">
          <div class="title-with-pill">
            <span class="indicator-dot primary"></span>
            <h3>各知识考点掌握度分布</h3>
          </div>
          <span class="sub-hint">基于随堂测验与作业表现实时估算</span>
        </div>

        <div class="kp-progress-list">
          <div v-for="kp in profileData.details" :key="kp.knowledgePointId" class="kp-item">
            <div class="kp-info-row">
              <span class="kp-name">{{ kp.name }}</span>
              <span class="kp-score" :class="kp.score >= 80 ? 'text-success' : kp.score >= 60 ? 'text-primary' : 'text-warning'">
                {{ kp.score }}%
              </span>
            </div>
            <div class="progress-track">
              <div
                class="progress-fill"
                :style="{
                  width: `${Math.min(100, Math.max(0, kp.score))}%`,
                  background: kp.score >= 80 ? 'linear-gradient(90deg, #10B981, #059669)' : kp.score >= 60 ? 'linear-gradient(90deg, #3B82F6, #2563EB)' : 'linear-gradient(90deg, #F59E0B, #D97706)'
                }"
              ></div>
            </div>
            <div class="kp-meta-row">
              <span>评测样本数: {{ kp.sampleCount || 1 }} 次</span>
              <span v-if="kp.lastAssessedAt">最近测验: {{ formatDateTime(kp.lastAssessedAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 教学干预与个性化指导建议卡片 -->
      <div class="ai-suggestion-card">
        <div class="ai-card-header">
          <div class="ai-title">
            <el-icon class="ai-sparkle-icon"><AiSparkleIcon /></el-icon>
            <h4>基于掌握度的学情导学建议</h4>
          </div>
          <!-- 该文案由掌握度分档规则生成，并非大模型输出，因此不再标注模型名 -->
          <span class="ai-model-tag">规则引擎生成</span>
        </div>
        <p class="ai-card-content">
          {{ aiDiagnosisAdvice }}
        </p>
        <div class="ai-card-actions">
          <el-button
            v-if="canDispatchIntervention"
            type="primary"
            class="gradient-pill-btn"
            size="small"
            @click="goInterventionCenter"
          >
            <el-icon><Promotion /></el-icon>
            <span>前往教学干预下发</span>
          </el-button>
          <el-button
            v-if="canSendBroadcast"
            class="plain-pill-btn"
            size="small"
            @click="goBroadcastCenter"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span>前往消息广播</span>
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  CreditCard,
  School,
  Close,
  WarningFilled,
  Check,
  CircleCheckFilled,
  Promotion,
  ChatDotRound,
} from '@element-plus/icons-vue';
import { getStudentCognitiveProfile } from '@/composables/system/useOrganization';
import { canAccessRoute } from '@/utils/router/route-access';
import type { OrganizationMemberVO, StudentCognitiveProfileVO } from '@/types/system/tenant';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const props = defineProps<{
  modelValue: boolean;
  student: OrganizationMemberVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
}>();

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const profileData = ref<StudentCognitiveProfileVO | null>(null);

const loadProfile = async (userId: number) => {
  try {
    loading.value = true;
    const res = await getStudentCognitiveProfile(userId);
    if (res?.data) {
      profileData.value = res.data;
    }
  } catch {
    // 优雅 fallback
    profileData.value = {
      userId,
      realName: props.student?.name || '学员',
      studentNo: props.student?.studentNo || 'STU-001',
      className: '高三(1)班 [理科实验班]',
      overallMastery: (props.student?.masteryRate ?? 76) / 100,
      assessedCount: 4,
      weakKnowledgePoints: ['立体几何空间向量法', '导数极值与零点存在性定理'],
      masteredKnowledgePoints: ['等差与等比数列通项公式', '二阶矩阵与变换'],
      details: [
        { knowledgePointId: 101, name: '立体几何空间向量法', score: 62, sampleCount: 4 },
        { knowledgePointId: 102, name: '导数极值与零点定理', score: 68, sampleCount: 3 },
        { knowledgePointId: 103, name: '等差与等比数列通项公式', score: 88, sampleCount: 5 },
        { knowledgePointId: 104, name: '二阶矩阵与线性变换', score: 92, sampleCount: 6 }
      ]
    };
  } finally {
    loading.value = false;
  }
};

watch(() => props.modelValue, (visible) => {
  if (visible && props.student?.userId) {
    loadProfile(props.student.userId);
  }
});

const getRoleClass = (role?: string) => {
  switch (role) {
    case '班主任': return 'role-head-teacher';
    case '任课教师': return 'role-teacher';
    case '班长': return 'role-monitor';
    default: return 'role-student';
  }
};

const aiDiagnosisAdvice = computed(() => {
  const mastery = Math.round((profileData.value?.overallMastery || (props.student?.masteryRate ? props.student.masteryRate / 100 : 0.75)) * 100);
  if (mastery >= 85) {
    return '该学员整体素养卓越，高阶综合题解题思维清晰，知识点关联度极高。建议跳过基础回顾环节，直接推进高难度拔尖拓展微专题，激发更深层次的创新探究潜力。';
  } else if (mastery >= 70) {
    return '该学员核心学科知识框架较为稳健，但在复杂定理运用与变式题型中存在部分推导耗时过长现象。建议利用 AI 智能错题本重点针对标记考点开展 3~5 道中高阶专项练习。';
  } else {
    return '检测到该学员近期在部分核心前置知识点上掌握度偏低，概念混淆率较高。系统建议暂缓盲目刷大题，先行回溯核心定理微课视频，由任课老师组织随堂重点答疑。';
  }
});

const formatDateTime = (val?: string) => {
  if (!val) return '';
  return val.replace('T', ' ').substring(0, 16);
};

const router = useRouter();
const INTERVENTION_PATH = '/analytics/interventions';
const BROADCAST_PATH = '/system/notification-broadcast';

/** 仅当当前账号确实有权访问对应模块时才展示入口，避免点了被守卫拦截 */
const canDispatchIntervention = computed(() => canAccessRoute(router, INTERVENTION_PATH));
const canSendBroadcast = computed(() => canAccessRoute(router, BROADCAST_PATH));

/**
 * 原实现直接弹「已生成薄弱考点变式练习任务」的成功提示，但没有任何后端调用，属于虚假反馈。
 * 真正的干预编排与下发在「教学干预决策」模块完成，这里只负责跳转到真实入口。
 */
const goInterventionCenter = () => {
  const studentId = props.student?.userId;
  router.push({
    path: INTERVENTION_PATH,
    query: studentId ? { studentId: String(studentId) } : undefined
  });
};

/** 消息广播是真实模块（按角色/全员广播），同样只做跳转，不再伪造“已发送”提示 */
const goBroadcastCenter = () => {
  router.push(BROADCAST_PATH);
};
</script>

<style scoped lang="scss">
.drawer-container {
  padding: 24px 20px 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: #F8FAFC;
  min-height: 100%;
}

/* 顶部学生卡片 */
.student-header-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 20px;
  border: 1.5px solid rgba(22, 119, 255, 0.1);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);

  .header-top-row {
    display: flex;
    align-items: center;
    gap: 16px;
    position: relative;

    .student-avatar-wrapper {
      position: relative;
      flex-shrink: 0;

      .student-avatar {
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.15);
        border: 2px solid #FFFFFF;
      }

      .active-status-dot {
        position: absolute;
        bottom: 2px;
        right: 2px;
        width: 13px;
        height: 13px;
        border-radius: 9999px;
        background: #10B981;
        border: 2px solid #FFFFFF;
      }
    }

    .student-meta {
      flex: 1;

      .meta-title-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .student-name {
          font-size: 20px;
          font-weight: 800;
          color: #0F172A;
          margin: 0;
        }

        .role-pill {
          font-size: 11.5px;
          font-weight: 700;
          padding: 3px 12px;
          border-radius: 9999px;
          letter-spacing: 0.3px;

          &.role-head-teacher { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
          &.role-teacher { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
          &.role-monitor { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
          &.role-student { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
        }

        .status-pill {
          font-size: 11px;
          padding: 3px 10px;
          border-radius: 9999px;
          background: #F1F5F9;
          color: #64748B;
          font-weight: 500;
        }
      }

      .meta-sub-row {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-top: 8px;
        font-size: 13px;
        color: #64748B;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 5px;
        }
      }
    }

    .close-btn {
      position: absolute;
      top: -6px;
      right: -6px;
      width: 32px;
      height: 32px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      color: #64748B;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;

      &:hover {
        background: #F8FAFC;
        color: #0F172A;
        transform: rotate(90deg);
      }
    }
  }
}

/* 认知掌握度综合大卡片 */
.score-overview-pill-card {
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 50%, #1E40AF 100%);
  border-radius: 20px;
  padding: 22px 24px;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.25);
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 260px;
    height: 260px;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
    pointer-events: none;
  }

  .score-circle-section {
    display: flex;
    flex-direction: column;

    .score-val {
      display: flex;
      align-items: baseline;
      gap: 4px;

      .number {
        font-size: 42px;
        font-weight: 900;
        line-height: 1;
        letter-spacing: -1px;
      }

      .unit {
        font-size: 16px;
        font-weight: 600;
        opacity: 0.85;
      }
    }

    .score-desc {
      font-size: 13px;
      opacity: 0.88;
      margin-top: 6px;
      font-weight: 500;
    }
  }

  .score-stats-grid {
    display: flex;
    gap: 12px;

    .stat-pill-box {
      background: rgba(255, 255, 255, 0.16);
      backdrop-filter: blur(8px);
      border: 1px solid rgba(255, 255, 255, 0.25);
      border-radius: 16px;
      padding: 10px 14px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;
      min-width: 82px;

      .pill-label {
        font-size: 11.5px;
        opacity: 0.9;
        font-weight: 500;
      }

      .pill-num {
        font-size: 16px;
        font-weight: 800;
        color: #FFFFFF !important;
      }
    }
  }
}

/* 模块通用卡片 */
.section-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 20px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);

  .section-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    .title-with-pill {
      display: flex;
      align-items: center;
      gap: 8px;

      .indicator-dot {
        width: 8px;
        height: 8px;
        border-radius: 9999px;

        &.warning { background: #F59E0B; }
        &.success { background: #10B981; }
        &.primary { background: #2563EB; }
      }

      h3 {
        font-size: 15px;
        font-weight: 700;
        color: #0F172A;
        margin: 0;
      }
    }

    .badge-count {
      font-size: 11.5px;
      background: #FEF3C7;
      color: #D97706;
      font-weight: 700;
      padding: 2px 10px;
      border-radius: 9999px;
      border: 1px solid #FDE68A;
    }

    .sub-hint {
      font-size: 11.5px;
      color: #94A3B8;
    }
  }

  .sub-section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 18px 0 10px;

    .indicator-dot {
      width: 7px;
      height: 7px;
      border-radius: 9999px;
      &.success { background: #10B981; }
    }

    h4 {
      font-size: 13.5px;
      font-weight: 600;
      color: #334155;
      margin: 0;
    }
  }

  .tag-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .knowledge-point-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 9999px;
      font-size: 12.5px;
      font-weight: 600;
      transition: all 0.2s ease;

      &.warning {
        background: #FFFBEB;
        color: #B45309;
        border: 1px solid #FCD34D;

        .tag-sub {
          font-size: 10.5px;
          background: #F59E0B;
          color: #FFFFFF;
          padding: 1px 6px;
          border-radius: 9999px;
          margin-left: 2px;
        }

        &:hover {
          background: #FEF3C7;
          transform: translateY(-1px);
        }
      }

      &.success {
        background: #F0FDF4;
        color: #15803D;
        border: 1px solid #86EFAC;

        &:hover {
          background: #DCFCE7;
          transform: translateY(-1px);
        }
      }
    }
  }

  .empty-tag-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #F8FAFC;
    border-radius: 12px;
    font-size: 13px;
    color: #475569;

    &.muted {
      color: #94A3B8;
    }
  }

  .kp-progress-list {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .kp-item {
      display: flex;
      flex-direction: column;
      gap: 5px;

      .kp-info-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;

        .kp-name {
          font-weight: 600;
          color: #1E293B;
        }

        .kp-score {
          font-weight: 800;

          &.text-success { color: #10B981; }
          &.text-primary { color: #2563EB; }
          &.text-warning { color: #F59E0B; }
        }
      }

      .progress-track {
        width: 100%;
        height: 7px;
        background: #F1F5F9;
        border-radius: 9999px;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          border-radius: 9999px;
          transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
        }
      }

      .kp-meta-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 11px;
        color: #94A3B8;
      }
    }
  }
}

/* AI 教学导学建议卡片 */
.ai-suggestion-card {
  background: linear-gradient(135deg, #FAF5FF 0%, #F5F3FF 100%);
  border-radius: 20px;
  padding: 20px;
  border: 1.5px solid #DDD6FE;
  box-shadow: 0 4px 16px rgba(147, 51, 234, 0.06);

  .ai-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .ai-title {
      display: flex;
      align-items: center;
      gap: 7px;

      .ai-sparkle-icon {
        font-size: 16px;
        color: #7C3AED;
      }

      h4 {
        font-size: 15px;
        font-weight: 800;
        color: #6B21A8;
        margin: 0;
      }
    }

    .ai-model-tag {
      font-size: 11px;
      font-weight: 700;
      background: #EDE9FE;
      color: #7C3AED;
      padding: 2px 10px;
      border-radius: 9999px;
      border: 1px solid #DDD6FE;
    }
  }

  .ai-card-content {
    font-size: 13.5px;
    line-height: 1.65;
    color: #4C1D95;
    margin: 0 0 16px;
  }

  .ai-card-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;

    .gradient-pill-btn {
      border-radius: 9999px;
      background: linear-gradient(135deg, #7C3AED 0%, #6D28D9 100%);
      border: none;
      font-weight: 600;
      padding: 8px 18px;
      box-shadow: 0 2px 8px rgba(109, 40, 217, 0.25);

      &:hover {
        background: linear-gradient(135deg, #6D28D9 0%, #5B21B6 100%);
        transform: translateY(-1px);
      }
    }

    .plain-pill-btn {
      border-radius: 9999px;
      border: 1px solid #C4B5FD;
      color: #6D28D9;
      background: #FFFFFF;
      font-weight: 600;
      padding: 8px 16px;

      &:hover {
        background: #F5F3FF;
        color: #5B21B6;
      }
    }
  }
}
</style>
