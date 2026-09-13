<template>
  <div class="agent-workflow-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>Agent 工作流</h2>
        <p>{{ agent?.name || '智能体执行编排' }}</p>
      </div>
      <el-button @click="router.push('/ai/agent')">返回中心</el-button>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据"
      class="mock-alert"
    />

    <el-card shadow="never" class="run-form-card">
      <el-form label-position="top">
        <el-form-item label="智能体">
          <el-select v-model="agentCode" placeholder="选择智能体" style="width: 100%">
            <el-option
              v-for="item in agents"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="执行目标">
          <el-input
            v-model="goal"
            type="textarea"
            :rows="3"
            placeholder="描述希望 Agent 完成的任务，例如：为班级薄弱知识点生成巩固练习"
          />
        </el-form-item>
        <el-form-item label="关联课程 ID">
          <el-input-number v-model="courseId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-button type="primary" :loading="loading || polling" @click="handleRun">
          启动 Agent
        </el-button>
      </el-form>
    </el-card>

    <el-card v-if="currentRun" shadow="never" class="steps-card">
      <template #header>
        <div class="steps-header">
          <span>执行步骤 ({{ planProgress }}%)</span>
          <div class="steps-header-tags">
            <el-tag size="small" type="info">工具调用 {{ toolSummary.total }}</el-tag>
            <el-tag :type="statusTagType">{{ currentRun.status }}</el-tag>
          </div>
        </div>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="step in currentRun.steps"
          :key="step.index"
          :type="step.status === 'DONE' ? 'success' : 'primary'"
        >
          <div class="step-title">{{ step.title }}</div>
          <div v-if="step.tool" class="step-meta">工具：{{ step.tool }}</div>
          <div v-if="step.outputPreview" class="step-output">{{ step.outputPreview }}</div>
        </el-timeline-item>
      </el-timeline>

      <div v-if="currentRun.result" class="result-box">
        <h4>执行结果</h4>
        <p v-if="currentRun.result.answer" class="result-answer">{{ currentRun.result.answer }}</p>
        <CitationList
          v-if="resultCitations.length > 0"
          :citations="resultCitations"
        />
        <pre v-else-if="!currentRun.result.answer">{{ JSON.stringify(currentRun.result, null, 2) }}</pre>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAgentExecution } from '@/features/agent/execution';
import { getPlanProgress } from '@/features/agent/planning';
import { summarizeToolCalling } from '@/features/agent/tool-calling';
import CitationList from '@/components/knowledge/CitationList.vue';
import type { AgentCitationVO } from '@/types/ai/agent';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();

const agentCode = ref((route.query.agentCode as string) || '');
const goal = ref('');
const courseId = ref(1);

const { loading, polling, usedMockFallback, agents, currentRun, fetchAgents, executeGoal } =
  useAgentExecution();

const planProgress = computed(() => getPlanProgress(currentRun.value));
const toolSummary = computed(() => summarizeToolCalling(currentRun.value?.steps ?? []));

const agent = computed(() => agents.value.find((item) => item.code === agentCode.value));

const statusTagType = computed(() => {
  const status = currentRun.value?.status;
  if (status === 'SUCCEEDED' || status === 'COMPLETED') return 'success';
  if (status === 'FAILED') return 'danger';
  return 'warning';
});

const resultCitations = computed(() => {
  const citations = currentRun.value?.result?.citations;
  if (!Array.isArray(citations)) return [];
  return citations.map((item: AgentCitationVO) => ({
    documentName: item.documentName,
    pageNo: item.pageNo,
    chunkId: item.chunkId,
    score: item.score,
    excerpt: item.excerpt,
    chunkIndex: item.chunkIndex
  }));
});

async function handleRun() {
  if (!agentCode.value || !goal.value.trim()) {
    ElMessage.warning('请选择智能体并填写执行目标');
    return;
  }
  await executeGoal({
    agentCode: agentCode.value,
    goal: goal.value.trim(),
    courseId: courseId.value
  });
}

watch(
  () => route.query.agentCode,
  (code) => {
    if (code) agentCode.value = String(code);
  }
);

onMounted(fetchAgents);
</script>

<style scoped lang="scss">
.agent-workflow-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;

    h2 {
      margin: 0 0 6px;
      font-size: 22px;
      font-weight: 700;
      color: #0F172A;
    }

    p {
      margin: 0;
      color: #64748B;
      font-size: 14px;
    }
  }

  .run-form-card,
  .steps-card {
    border-radius: 14px;
  }

  .steps-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 700;

    .steps-header-tags {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .step-title {
    font-weight: 600;
    color: #1E293B;
    margin-bottom: 4px;
  }

  .step-meta,
  .step-output {
    font-size: 13px;
    color: #64748B;
    line-height: 1.6;
  }

  .result-box {
    margin-top: 16px;
    padding: 14px;
    background: #F8FAFC;
    border-radius: 10px;

    h4 {
      margin: 0 0 8px;
      color: #0F172A;
    }

    .result-answer {
      margin: 0 0 12px;
      line-height: 1.7;
      color: #334155;
      white-space: pre-wrap;
    }

    pre {
      margin: 0;
      font-size: 12px;
      color: #334155;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }
}
</style>
