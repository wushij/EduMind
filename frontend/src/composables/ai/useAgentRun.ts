import { onUnmounted, ref } from 'vue';
import { getAgentRun, listAgents, startAgentRun } from '@/api/ai/agent';
import { API_BASE_URL } from '@/config';
import { appendSecurityQuery } from '@/core/http/request-signature';
import { USE_MOCK } from '@/config/mock';
import { MOCK_AGENT_RUN, MOCK_AGENTS } from '@/mock/agent';
import { storage } from '@/core/storage/local';
import { TOKEN_KEY } from '@/constants/auth';
import type { AgentRunCreateRequest, AgentRunVO, AgentVO } from '@/types/ai/agent';

const POLL_INTERVAL_MS = 1500;
const MAX_POLL_ATTEMPTS = 40;

export function useAgentRun() {
  const loading = ref(false);
  const polling = ref(false);
  const usedMockFallback = ref(false);
  const agents = ref<AgentVO[]>([]);
  const currentRun = ref<AgentRunVO | null>(null);

  let pollTimer: ReturnType<typeof setInterval> | null = null;
  let eventSource: EventSource | null = null;
  let pollAttempts = 0;

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    polling.value = false;
    pollAttempts = 0;
  }

  async function fetchAgents() {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await listAgents();
      agents.value = res.data ?? [];
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        agents.value = MOCK_AGENTS;
      } else {
        agents.value = [];
      }
    } finally {
      loading.value = false;
    }
  }

  async function pollRun(runId: string) {
    try {
      const res = await getAgentRun(runId);
      currentRun.value = res.data;
      if (res.data?.status === 'SUCCEEDED' || res.data?.status === 'FAILED') {
        stopPolling();
      }
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        currentRun.value = { ...MOCK_AGENT_RUN, runId };
        stopPolling();
      } else {
        stopPolling();
      }
    }
  }

  function startPolling(runId: string) {
    stopPolling();
    polling.value = true;
    pollRun(runId);
    pollTimer = setInterval(() => {
      pollAttempts += 1;
      if (pollAttempts >= MAX_POLL_ATTEMPTS) {
        stopPolling();
        return;
      }
      pollRun(runId);
    }, POLL_INTERVAL_MS);
  }

  function startSse(runId: string) {
    stopPolling();
    polling.value = true;
    const token = storage.get(TOKEN_KEY);
    let url = `${API_BASE_URL}/ai/agent/runs/${runId}/stream`;
    if (token) {
      url += `${url.includes('?') ? '&' : '?'}satoken=${encodeURIComponent(token)}`;
    }
    url = appendSecurityQuery(url, 'GET');
    eventSource = new EventSource(url);
    eventSource.addEventListener('step', () => pollRun(runId));
    eventSource.addEventListener('done', () => {
      pollRun(runId);
      stopPolling();
    });
    eventSource.addEventListener('error', () => {
      eventSource?.close();
      eventSource = null;
      startPolling(runId);
    });
    pollRun(runId);
  }

  async function runAgent(request: AgentRunCreateRequest) {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await startAgentRun(request);
      const runId = res.data?.runId;
      if (runId) {
        startSse(runId);
      }
      return runId;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        const runId = MOCK_AGENT_RUN.runId;
        currentRun.value = MOCK_AGENT_RUN;
        return runId;
      }
      return undefined;
    } finally {
      loading.value = false;
    }
  }

  onUnmounted(stopPolling);

  return {
    loading,
    polling,
    usedMockFallback,
    agents,
    currentRun,
    fetchAgents,
    runAgent,
    stopPolling
  };
}
