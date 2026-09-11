import { ref } from 'vue';
import { getDashboardSummary } from '@/api/dashboard/dashboard';
import { DashboardStatistics } from '@/types/analytics/statistics';
import { USE_MOCK } from '@/config/mock';
import { MOCK_DASHBOARD_DATA } from '@/mock/dashboard';

export function useDashboard() {
  const summary = ref<DashboardStatistics | null>(null);
  const loading = ref(false);
  const usedMockFallback = ref(false);

  async function fetchSummary() {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await getDashboardSummary();
      summary.value = res.data;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        summary.value = {
          courseCount: 3,
          questionCount: 128,
          examCount: 5,
          aiConversationCount: 12,
          assignmentCount: 8,
          pendingGradingCount: 3,
          pendingAssignmentCount: 2,
          recentCourses: MOCK_DASHBOARD_DATA.kpiStats.ADMIN.length
            ? [
                { id: 101, name: '高等数学（上）' },
                { id: 102, name: '数据结构与算法深度解析' }
              ]
            : []
        };
      } else {
        summary.value = null;
      }
    } finally {
      loading.value = false;
    }
  }

  return { summary, loading, usedMockFallback, fetchSummary };
}
