<template>
  <div class="agent-center-container">
    <div class="header-card">
      <div class="header-left">
        <div class="agent-badge">
          <el-icon><Cpu /></el-icon>
          <span>EduMind Autonomous Agent</span>
        </div>
        <h1>AI 教学智能体协同中心</h1>
        <p>基于意图识别（Intent）、多步规划（Planning）与 Tool Calling 工具编排的高校级自主教学智能体系统</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="large" :icon="Plus">新建智能体</el-button>
        <el-button size="large" :icon="Setting">编排配置</el-button>
      </div>
    </div>

    <div class="status-grid">
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon blue"><el-icon><Compass /></el-icon></div>
        <div class="metric-info">
          <span class="label">已装载智能体</span>
          <span class="val">8 个</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon green"><el-icon><Tools /></el-icon></div>
        <div class="metric-info">
          <span class="label">可用工具 (Tool Calling)</span>
          <span class="val">24 项</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon purple"><el-icon><Histogram /></el-icon></div>
        <div class="metric-info">
          <span class="label">多步规划完成率</span>
          <span class="val">98.4%</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-icon orange"><el-icon><Timer /></el-icon></div>
        <div class="metric-info">
          <span class="label">平均响应耗时</span>
          <span class="val">1.2s</span>
        </div>
      </el-card>
    </div>

    <div class="agent-grid">
      <el-card v-for="agent in agents" :key="agent.id" shadow="hover" class="agent-card">
        <div class="agent-card-header">
          <div class="agent-avatar" :style="{ background: agent.bgColor }">
            <el-icon :size="24" :color="agent.iconColor"><component :is="agent.icon" /></el-icon>
          </div>
          <div class="agent-meta">
            <h3>{{ agent.name }}</h3>
            <el-tag size="small" :type="agent.status === 'ACTIVE' ? 'success' : 'info'">{{ agent.status }}</el-tag>
          </div>
        </div>
        <p class="agent-desc">{{ agent.description }}</p>
        <div class="tool-tags">
          <span class="tool-label">调用工具：</span>
          <el-tag v-for="t in agent.tools" :key="t" size="small" effect="plain">{{ t }}</el-tag>
        </div>
        <div class="card-footer">
          <span class="steps-info">规划步数：{{ agent.steps }} 步</span>
          <el-button type="primary" link>进入工作台 →</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Cpu, Plus, Setting, Compass, Tools, Histogram, Timer, MagicStick, Reading, Collection, Checked } from '@element-plus/icons-vue';

interface AgentItem {
  id: number;
  name: string;
  description: string;
  status: 'ACTIVE' | 'IDLE';
  icon: any;
  iconColor: string;
  bgColor: string;
  tools: string[];
  steps: number;
}

const agents = ref<AgentItem[]>([
  {
    id: 1,
    name: '课程智能助教 Agent',
    description: '自主根据学生提问定位课件章节、知识点依赖关系，生成阶梯式答疑引导与变式练习。',
    status: 'ACTIVE',
    icon: Reading,
    iconColor: '#1677FF',
    bgColor: '#EAF3FF',
    tools: ['知识库检索', '章节定位', '公式渲染', '练习推送'],
    steps: 4
  },
  {
    id: 2,
    name: '多维组卷评估 Agent',
    description: '基于难度正态分布与知识点覆盖矩阵，多轮自动抽题、题型配比校验与自动重试均衡。',
    status: 'ACTIVE',
    icon: Collection,
    iconColor: '#52C41A',
    bgColor: '#F6FFED',
    tools: ['题库检索', '难度评估', '去重校验', '排版生成'],
    steps: 5
  },
  {
    id: 3,
    name: '主观题智能精判 Agent',
    description: '结合评分细则细化采分点，分析逻辑缺陷与解题思路，自动输出针对性教师复核建议。',
    status: 'ACTIVE',
    icon: Checked,
    iconColor: '#722ED1',
    bgColor: '#F9F0FF',
    tools: ['细则对齐', '语义相似度', '错因归纳', '复核提示'],
    steps: 3
  },
  {
    id: 4,
    name: '教学大纲与教案生成 Agent',
    description: '解析专业培养方案，自动规划周课时教案、教学目标、思政元素融合与配套作业。',
    status: 'ACTIVE',
    icon: MagicStick,
    iconColor: '#FA8C16',
    bgColor: '#FFF7E6',
    tools: ['大纲解析', '思政库检索', '课时编排', '文档导出'],
    steps: 6
  }
]);
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
        margin-bottom: 14px;
      }

      .tool-tags {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 16px;

        .tool-label {
          font-size: 12px;
          color: #94A3B8;
        }
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
</style>
