import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getMemoryNamespace,
  updateMemoryConsent,
  createMemoryItem,
  forgetMemoryItem,
  forgetAllMemories,
  feedbackMemoryItem,
  retrieveMemories
} from '@/api/ai/memory';
import type { MemoryItemVO } from '@/types/ai/memory';

export function getMemoryTypeLabel(type?: string) {
  switch (type) {
    case 'PREFERENCE':
      return '学习偏好';
    case 'PROFILE':
      return '认知画像';
    case 'EPISODIC':
      return '情境片段';
    case 'FEEDBACK':
      return '交互反馈';
    default:
      return '记忆';
  }
}

export function getMemoryTypeTagType(type?: string) {
  switch (type) {
    case 'PREFERENCE':
      return 'success';
    case 'PROFILE':
      return 'danger';
    case 'EPISODIC':
      return 'warning';
    default:
      return 'info';
  }
}

export function filterMemoryItems(
  items: MemoryItemVO[],
  keyword: string,
  typeFilter?: string
): MemoryItemVO[] {
  return items.filter((item) => {
    const text = `${item.summary || ''} ${item.memoryKey || ''} ${item.memoryValue || ''}`.toLowerCase();
    const matchKw = !keyword || text.includes(keyword.toLowerCase());
    const matchType = !typeFilter || item.memoryType === typeFilter;
    return matchKw && matchType;
  });
}

export function useAgentMemory() {
  const loading = ref(false);
  const consentGranted = ref(false);
  const retentionDays = ref(180);
  const searchKeyword = ref('');
  const typeFilter = ref<string | undefined>(undefined);
  const memoryItems = ref<MemoryItemVO[]>([]);

  const preferenceCount = computed(
    () => memoryItems.value.filter((i) => i.memoryType === 'PREFERENCE').length
  );
  const episodicCount = computed(
    () => memoryItems.value.filter((i) => i.memoryType === 'EPISODIC').length
  );

  const filteredMemories = computed(() =>
    filterMemoryItems(memoryItems.value, searchKeyword.value, typeFilter.value)
  );

  const createDialogVisible = ref(false);
  const createForm = ref({
    memoryKey: '',
    memoryType: 'PREFERENCE',
    sensitivityLevel: 'NORMAL',
    memoryValue: ''
  });

  const recallDrawerVisible = ref(false);
  const queryPrompt = ref('请结合我平时的做题习惯与知识盲区进行诊断');
  const recalledItems = ref<MemoryItemVO[]>([]);

  async function loadMemories() {
    try {
      loading.value = true;
      const res = await getMemoryNamespace();
      if (res?.data) {
        consentGranted.value = !!res.data.consentGranted;
        retentionDays.value = res.data.retentionDays || 180;
        memoryItems.value = res.data.items || [];
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '加载长期记忆空间失败';
      ElMessage.error(message);
      memoryItems.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function handleConsentChange() {
    try {
      await updateMemoryConsent({
        consentGranted: consentGranted.value,
        retentionDays: retentionDays.value
      });
      if (!consentGranted.value) {
        memoryItems.value = [];
        ElMessage.warning('已撤销记忆知情同意，系统已级联擦除历史记忆条目');
      } else {
        ElMessage.success('知情同意偏好已成功更新');
        await loadMemories();
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '更新授权失败';
      ElMessage.error(message);
    }
  }

  function handleForgetAll() {
    ElMessageBox.confirm(
      '确认清空并物理擦除所有 Agent 长期记忆吗？此操作将使 Agent 回归初始零知识状态。',
      '行使被遗忘权警告',
      {
        confirmButtonText: '立即清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      try {
        await forgetAllMemories();
        memoryItems.value = [];
        ElMessage.success('已清空并物理擦除全部记忆资产');
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '一键清空记忆失败';
        ElMessage.error(message);
      }
    });
  }

  function openCreateDialog() {
    createForm.value = {
      memoryKey: '',
      memoryType: 'PREFERENCE',
      sensitivityLevel: 'NORMAL',
      memoryValue: ''
    };
    createDialogVisible.value = true;
  }

  async function submitCreateMemory() {
    const summaryContent = createForm.value.memoryValue.trim();
    if (!summaryContent) {
      ElMessage.warning('请填写记忆描述内容');
      return;
    }
    try {
      await createMemoryItem({
        summary: summaryContent,
        memoryType: createForm.value.memoryType,
        sensitivityLevel: createForm.value.sensitivityLevel
      });
      ElMessage.success('长期记忆已成功沉淀入库');
      createDialogVisible.value = false;
      await loadMemories();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '注入长期记忆失败';
      ElMessage.error(message);
    }
  }

  function forgetSingle(item: MemoryItemVO) {
    const displayTitle = item.summary ? `${item.summary.substring(0, 16)}...` : `#${item.id}`;
    ElMessageBox.confirm(`确认让 Agent 遗忘条目【${displayTitle}】吗？`, '确认遗忘', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    }).then(async () => {
      try {
        await forgetMemoryItem(item.id);
        memoryItems.value = memoryItems.value.filter((i) => i.id !== item.id);
        ElMessage.success('条目已安全物理擦除');
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '遗忘操作失败';
        ElMessage.error(message);
      }
    });
  }

  async function giveFeedback(item: MemoryItemVO, score: number) {
    try {
      await feedbackMemoryItem(item.id, {
        feedbackAction: score > 3 ? 'MODIFY' : 'FORGET',
        relevanceScore: score,
        reason: score > 3 ? '用户标记记忆准确' : '用户标记记忆偏差，行使遗忘'
      });
      if (score <= 2) {
        memoryItems.value = memoryItems.value.filter((i) => i.id !== item.id);
        ElMessage.success('已记录负向反馈并物理遗忘该条目');
      } else {
        ElMessage.success('感谢正向反馈，Agent 将持续强化此项记忆');
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '反馈提交失败';
      ElMessage.error(message);
    }
  }

  function openRecallTester() {
    recalledItems.value = [];
    recallDrawerVisible.value = true;
  }

  async function doRetrieve() {
    if (!queryPrompt.value.trim()) {
      ElMessage.warning('请输入检索 Prompt');
      return;
    }
    try {
      const res = await retrieveMemories(queryPrompt.value.trim());
      recalledItems.value = res?.data || [];
      if (recalledItems.value.length === 0) {
        ElMessage.info('未召回相关记忆条目');
      } else {
        ElMessage.success(`已完成 Top-${recalledItems.value.length} 记忆语义召回`);
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '检索召回失败';
      ElMessage.error(message);
      recalledItems.value = [];
    }
  }

  onMounted(loadMemories);

  return {
    loading,
    consentGranted,
    retentionDays,
    searchKeyword,
    typeFilter,
    memoryItems,
    preferenceCount,
    episodicCount,
    filteredMemories,
    createDialogVisible,
    createForm,
    recallDrawerVisible,
    queryPrompt,
    recalledItems,
    loadMemories,
    handleConsentChange,
    handleForgetAll,
    openCreateDialog,
    submitCreateMemory,
    forgetSingle,
    giveFeedback,
    openRecallTester,
    doRetrieve,
    getTypeLabel: getMemoryTypeLabel,
    getTypeTagType: getMemoryTypeTagType
  };
}
