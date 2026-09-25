<template>
  <div class="agent-center-container" v-loading="loading">
    <div class="header-card">
      <div class="header-left">
        <div class="agent-badge">
          <el-icon><Cpu /></el-icon>
          <span>EduMind Autonomous Agent</span>
        </div>
        <h1>AI 教学智能体协同中心</h1>
        <p>基于意图识别、多步规划与 Tool Calling 工具编排的高校级自主教学智能体系统</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="large" :icon="Plus" @click="goWorkflow()">
          新建运行
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据"
      class="mock-alert"
    />

    <div class="status-grid">
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon blue"><el-icon><Compass /></el-icon></div>
        <div class="metric-info">
          <span class="label">已装载智能体</span>
          <span class="val">{{ agents.length }} 个</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon green"><el-icon><Tools /></el-icon></div>
        <div class="metric-info">
          <span class="label">可用工具 (Tool Calling)</span>
          <span class="val">{{ totalTools }} 项</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon purple"><el-icon><Histogram /></el-icon></div>
        <div class="metric-info">
          <span class="label">平均成功率</span>
          <span class="val">{{ avgSuccessRate }}%</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon orange"><el-icon><Timer /></el-icon></div>
        <div class="metric-info">
          <span class="label">累计运行次数</span>
          <span class="val">{{ totalRuns.toLocaleString() }}</span>
        </div>
      </el-card>
    </div>

    <div class="agent-grid">
      <el-card v-for="agent in agents" :key="agent.code" shadow="hover" class="agent-card">
        <div class="agent-card-header">
          <div class="agent-avatar" :style="{ background: getAgentStyle(agent.code).bgColor }">
            <el-icon :size="24" :color="getAgentStyle(agent.code).iconColor">
              <component :is="getAgentStyle(agent.code).icon" />
            </el-icon>
          </div>
          <div class="agent-meta">
            <h3>{{ agent.name }}</h3>
            <el-tag size="small" :type="agent.status === 'ACTIVE' ? 'success' : 'info'">
              {{ agent.status }}
            </el-tag>
          </div>
        </div>
        <p class="agent-desc">模型：{{ agent.modelKey }} · 工具 {{ agent.toolCount }} 项</p>
        <div class="card-footer">
          <span class="steps-info">
            成功率 {{ agent.successRate?.toFixed(1) ?? 0 }}% · 运行 {{ agent.totalRuns }} 次
          </span>
          <el-button type="primary" link @click="goWorkflow(agent.code)">进入工作台 →</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Cpu,
  Plus,
  Compass,
  Tools,
  Histogram,
  Timer,
  Reading,
  Collection,
  Checked
} from '@element-plus/icons-vue';
import { useAgentRun } from '@/composables/ai/useAgentRun';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const router = useRouter();
const { loading, usedMockFallback, agents, fetchAgents } = useAgentRun();

const totalTools = computed(() =>
  agents.value.reduce((sum, item) => sum + (item.toolCount || 0), 0)
);

const totalRuns = computed(() =>
  agents.value.reduce((sum, item) => sum + (item.totalRuns || 0), 0)
);

const avgSuccessRate = computed(() => {
  if (!agents.value.length) return '0.0';
  const avg =
    agents.value.reduce((sum, item) => sum + (item.successRate || 0), 0) / agents.value.length;
  return avg.toFixed(1);
});

const agentStyleMap: Record<string, { icon: any; iconColor: string; bgColor: string }> = {
  'course-tutor': { icon: Reading, iconColor: '#1677FF', bgColor: '#EAF3FF' },
  'exam-builder': { icon: Collection, iconColor: '#52C41A', bgColor: '#F6FFED' },
  'grading-assistant': { icon: Checked, iconColor: '#722ED1', bgColor: '#F9F0FF' },
  'lesson-planner': { icon: AiSparkleIcon, iconColor: '#FA8C16', bgColor: '#FFF7E6' }
};

function getAgentStyle(code: string) {
  return (
    agentStyleMap[code] ?? {
      icon: Compass,
      iconColor: '#1677FF',
      bgColor: '#EAF3FF'
    }
  );
}

function goWorkflow(agentCode?: string) {
  router.push({
    path: '/ai/agent/workflow',
    query: agentCode ? { agentCode } : undefined
  });
}

onMounted(fetchAgents);
</script>

<style scoped lang="scss">
.agent-center-container {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .header-card {
    background: linear-gradient(135deg, #0A1B39 0%, #162C5B 100%);
    border-radius: 16px;
    padding: 28px 32px;
    color: #fff;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .agent-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 20px;
      background: rgba(22, 119, 255, 0.3);
      border: 1px solid rgba(22, 119, 255, 0.5);
      font-size: 13px;
      font-weight: 500;
      color: #85B7FF;
      margin-bottom: 12px;
    }

    h1 {
      font-size: 26px;
      font-weight: 700;
      margin: 0 0 8px 0;
    }

    p {
      margin: 0;
      color: #94A3B8;
      font-size: 14px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .status-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    .metric-card {
      border-radius: 12px;
      :deep(.el-card__body) {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 20px;
      }

      .metric-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;

        &.blue { background: #EAF3FF; color: #1677FF; }
        &.green { background: #F6FFED; color: #52C41A; }
        &.purple { background: #F9F0FF; color: #722ED1; }
        &.orange { background: #FFF7E6; color: #FA8C16; }
      }

      .metric-info {
        display: flex;
        flex-direction: column;
        .label { font-size: 13px; color: #64748B; margin-bottom: 4px; }
        .val { font-size: 20px; font-weight: 700; color: #0F172A; }
      }
    }
  }

  .agent-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;

    .agent-card {
      border-radius: 14px;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        border-color: #1677FF;
      }

      .agent-card-header {
        display: flex;
        align-items: center;
        gap: 14px;
        margin-bottom: 12px;

        .agent-avatar {
          width: 44px;
          height: 44px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .agent-meta {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: space-between;

          h3 {
            margin: 0;
            font-size: 16px;
            font-weight: 600;
            color: #1E293B;
          }
        }
      }

      .agent-desc {
        color: #64748B;
        font-size: 14px;
        line-height: 1.6;
        min-height: 44px;
        margin-bottom: 16px;
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #F1F5F9;
        padding-top: 12px;

        .steps-info {
          font-size: 13px;
          color: #64748B;
        }
      }
    }
  }
}

@media (max-width: 960px) {
  .status-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .agent-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
