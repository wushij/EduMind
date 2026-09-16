import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getMemoryNamespace,
  getMemoryOverview,
  updateMemoryConsent,
  createMemoryItem,
  updateMemoryItem,
  forgetMemoryItem,
  forgetAllMemories,
  feedbackMemoryItem,
  retrieveMemories,
  seedSampleMemories,
  extractMemories,
  decryptMemoryItem,
  cleanupDuplicateMemories,
  batchConfirmMemories
} from '@/api/ai/memory';
import type {
  MemoryItemVO,
  MemoryOverviewVO,
  MemorySpaceItemVO,
  MemoryItemUpdateRequest
} from '@/types/ai/memory';

export function getMemoryTypeLabel(type?: string) {
  switch (type) {
    case 'PREFERENCE':
      return '学习风格与偏好';
    case 'PROFILE':
      return '学术能力与画像';
    case 'EPISODIC':
      return '高频攻坚情境';
    case 'FEEDBACK':
      return '人机调优反馈';
    default:
      return '长效记忆';
  }
}

export function getMemoryTypeTagType(type?: string) {
  switch (type) {
    case 'PREFERENCE':
      return 'success';
    case 'PROFILE':
      return 'primary';
    case 'EPISODIC':
      return 'warning';
    case 'FEEDBACK':
      return 'info';
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
    const text = `${item.summary || ''} ${item.memoryKey || ''} ${item.memoryValue || ''} ${item.sourceRef || ''}`.toLowerCase();
    const matchKw = !keyword || text.includes(keyword.toLowerCase());
    const matchType = !typeFilter || item.memoryType === typeFilter;
    return matchKw && matchType;
  });
}

export function useAgentMemory() {
  const loading = ref(false);
  const overviewLoading = ref(false);
  const seeding = ref(false);
  const extracting = ref(false);

  const selectedCourseId = ref<number | undefined>(undefined);
  const overview = ref<MemoryOverviewVO | null>(null);
  const spaces = computed<MemorySpaceItemVO[]>(() => overview.value?.spaces || []);

  const consentGranted = ref(false);
  const retentionDays = ref(180);
  const searchKeyword = ref('');
  const typeFilter = ref<string | undefined>(undefined);
  const memoryItems = ref<MemoryItemVO[]>([]);

  const preferenceCount = computed(
    () => memoryItems.value.filter((i) => i.memoryType === 'PREFERENCE').length
  );
  const profileCount = computed(
    () => memoryItems.value.filter((i) => i.memoryType === 'PROFILE').length
  );
  const episodicCount = computed(
    () => memoryItems.value.filter((i) => i.memoryType === 'EPISODIC').length
  );
  const feedbackCount = computed(
    () => memoryItems.value.filter((i) => i.memoryType === 'FEEDBACK').length
  );
  const encryptedCount = computed(
    () => memoryItems.value.filter((i) => Boolean(i.encrypted)).length
  );

  const filteredMemories = computed(() =>
    filterMemoryItems(memoryItems.value, searchKeyword.value, typeFilter.value)
  );

  // 弹窗与抽屉控制
  const createDialogVisible = ref(false);
  const createForm = ref({
    memoryType: 'PREFERENCE',
    sensitivityLevel: 'NORMAL',
    memoryValue: '',
    fullContent: ''
  });

  const editDialogVisible = ref(false);
  const editItemTarget = ref<MemoryItemVO | null>(null);
  const editForm = ref({
    summary: '',
    memoryType: 'PREFERENCE',
    sensitivityLevel: 'NORMAL',
    fullContent: ''
  });

  const recallDrawerVisible = ref(false);
  const queryPrompt = ref('请结合我近期的复习进度与易错点进行针对性解题指导');
  const recalledItems = ref<MemoryItemVO[]>([]);

  const extractDialogVisible = ref(false);
  const extractedCandidates = ref<MemoryItemVO[]>([]);

  async function loadOverview() {
    try {
      overviewLoading.value = true;
      const res = await getMemoryOverview();
      if (res?.data) {
        overview.value = res.data;
      }
    } catch (e: unknown) {
      console.warn('加载长期记忆概览失败:', e);
    } finally {
      overviewLoading.value = false;
    }
  }

  async function loadMemories() {
    try {
      loading.value = true;
      const res = await getMemoryNamespace(selectedCourseId.value);
      if (res?.data) {
        consentGranted.value = Boolean(res.data.consentGranted);
        retentionDays.value = res.data.retentionDays || 180;
        memoryItems.value = res.data.items || [];
      } else {
        memoryItems.value = [];
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '加载长期记忆空间失败';
      ElMessage.error(message);
      memoryItems.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function handleSwitchCourse(courseId?: number) {
    selectedCourseId.value = courseId;
    searchKeyword.value = '';
    typeFilter.value = undefined;
    await loadMemories();
  }

  async function handleConsentChange() {
    try {
      await updateMemoryConsent({
        courseId: selectedCourseId.value,
        consentGranted: consentGranted.value,
        retentionDays: retentionDays.value
      });
      if (!consentGranted.value) {
        memoryItems.value = [];
        ElMessage.warning('已撤销记忆知情同意，系统已按 PIPL 合规要求物理擦除本空间条目');
      } else {
        ElMessage.success('知情同意偏好已成功更新，记忆沉淀引擎已激活');
        await loadMemories();
      }
      await loadOverview();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '更新授权失败';
      ElMessage.error(message);
    }
  }

  function handleForgetAll() {
    ElMessageBox.confirm(
      '确认清空并物理擦除当前空间的全部 Agent 长期记忆吗？此操作将使 Agent 回归零知识初始状态。',
      '行使被遗忘权警告',
      {
        confirmButtonText: '确认物理清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      try {
        await forgetAllMemories(selectedCourseId.value);
        memoryItems.value = [];
        ElMessage.success('已清空并物理擦除当前空间全部记忆资产');
        await loadOverview();
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '一键清空记忆失败';
        ElMessage.error(message);
      }
    });
  }

  async function handleSeedSample() {
    try {
      seeding.value = true;
      const res = await seedSampleMemories(selectedCourseId.value);
      const count = res?.data || 0;
      ElMessage.success(`成功载入 ${count} 条高质量教学与认知特征范本`);
      consentGranted.value = true;
      await loadMemories();
      await loadOverview();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '初始化记忆范本失败';
      ElMessage.error(message);
    } finally {
      seeding.value = false;
    }
  }

  const cleaningDuplicates = ref(false);
  const hasDuplicates = computed(() => {
    const seen = new Set<string>();
    for (const it of memoryItems.value) {
      const key = (it.summary || '').replace(/【.*?】/g, '').replace(/\s+/g, '').trim();
      if (key.length > 5 && seen.has(key)) return true;
      if (key.length > 5) seen.add(key);
    }
    return false;
  });

  let extractAbortController: AbortController | null = null;

  async function handleExtractMemories() {
    if (!consentGranted.value) {
      ElMessage.warning('请先开启知情授权后再执行 AI 学情智能萃取');
      return;
    }
    // 立即弹开深度思考对话框，呈现思考状态
    extractedCandidates.value = [];
    extractDialogVisible.value = true;
    extracting.value = true;
    if (extractAbortController) {
      extractAbortController.abort();
    }
    extractAbortController = new AbortController();

    try {
      const res = await extractMemories(selectedCourseId.value, extractAbortController.signal);
      extractedCandidates.value = res?.data || [];
      const count = extractedCandidates.value.length;
      if (count > 0) {
        ElMessage.success(`AI 深度推演已生成 ${count} 项候选认知特征，请审阅后确认采纳`);
      } else {
        ElMessage.info('AI 分析完成，当前学情暂无新增认知特征');
      }
      // 注意：此处纯草稿研判展示，绝不提前调用 loadMemories()，防止背景列表偷跑！
    } catch (e: unknown) {
      if (e instanceof Error && (e.name === 'CanceledError' || e.name === 'AbortError' || (e as { code?: string }).code === 'ERR_CANCELED')) {
        return;
      }
      extractDialogVisible.value = false;
      const message = e instanceof Error ? e.message : '智能萃取记忆失败';
      ElMessage.error(message);
    } finally {
      extracting.value = false;
      extractAbortController = null;
    }
  }

  const confirmingCandidates = ref(false);

  async function handleConfirmExtract(selectedItems: MemoryItemVO[]) {
    if (!selectedItems || selectedItems.length === 0) {
      ElMessage.warning('请至少勾选一项需要沉淀入库的认知特征');
      return;
    }

    try {
      confirmingCandidates.value = true;
      const dtos = selectedItems.map((item) => ({
        courseId: selectedCourseId.value,
        summary: item.summary,
        memoryType: item.memoryType,
        sensitivityLevel: item.sensitivityLevel,
        fullContent: item.fullContent
      }));

      const res = await batchConfirmMemories(selectedCourseId.value, dtos);
      const savedCount = res?.data || selectedItems.length;
      ElMessage.success(`成功确认采纳并沉淀 ${savedCount} 项长效认知特征！`);
      extractDialogVisible.value = false;

      // 用户主动确认入库后，才刷新列表与统计看板！
      await loadMemories();
      await loadOverview();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '确认采纳记忆失败';
      ElMessage.error(message);
    } finally {
      confirmingCandidates.value = false;
    }
  }

  function handleCancelExtract() {
    if (extractAbortController) {
      extractAbortController.abort();
      extractAbortController = null;
    }
    extracting.value = false;
    extractDialogVisible.value = false;
    ElMessage.info('已安全中止本次 AI 萃取任务');
  }

  async function handleCleanupDuplicates() {
    try {
      cleaningDuplicates.value = true;
      const res = await cleanupDuplicateMemories(selectedCourseId.value);
      const removed = res?.data || 0;
      if (removed > 0) {
        ElMessage.success(`智能去重完成，已安全清理 ${removed} 条重复冗余记忆`);
      } else {
        ElMessage.info('当前空间暂无重复记忆条目');
      }
      await loadMemories();
      await loadOverview();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '清理重复记忆失败';
      ElMessage.error(message);
    } finally {
      cleaningDuplicates.value = false;
    }
  }

  async function handleDecryptItem(item: MemoryItemVO) {
    if (item.fullContent) {
      // 已经解密过，可直接展示
      return item.fullContent;
    }
    try {
      const res = await decryptMemoryItem(item.id);
      if (res?.data?.decryptedContent) {
        item.fullContent = res.data.decryptedContent;
        ElMessage.success('国密 SM4 敏感明文已由 KMS 硬件信道安全解密');
        return res.data.decryptedContent;
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '国密解密失败';
      ElMessage.error(message);
    }
    return null;
  }

  function openCreateDialog() {
    createForm.value = {
      memoryType: 'PREFERENCE',
      sensitivityLevel: 'NORMAL',
      memoryValue: '',
      fullContent: ''
    };
    createDialogVisible.value = true;
  }

  async function submitCreateMemory() {
    const summaryContent = createForm.value.memoryValue.trim();
    if (!summaryContent) {
      ElMessage.warning('请填写记忆事实描述');
      return;
    }
    try {
      await createMemoryItem({
        courseId: selectedCourseId.value,
        summary: summaryContent,
        fullContent: createForm.value.fullContent.trim() || undefined,
        memoryType: createForm.value.memoryType,
        sensitivityLevel: createForm.value.sensitivityLevel
      });
      ElMessage.success('长期记忆特征已成功沉淀入库');
      createDialogVisible.value = false;
      await loadMemories();
      await loadOverview();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '注入长期记忆失败';
      ElMessage.error(message);
    }
  }

  function openEditDialog(item: MemoryItemVO) {
    editItemTarget.value = item;
    editForm.value = {
      summary: item.summary,
      memoryType: item.memoryType,
      sensitivityLevel: item.sensitivityLevel || 'NORMAL',
      fullContent: item.fullContent || ''
    };
    editDialogVisible.value = true;
  }

  async function submitEditMemory() {
    if (!editItemTarget.value) return;
    const summaryContent = editForm.value.summary.trim();
    if (!summaryContent) {
      ElMessage.warning('记忆描述不可为空');
      return;
    }
    try {
      const req: MemoryItemUpdateRequest = {
        summary: summaryContent,
        memoryType: editForm.value.memoryType,
        sensitivityLevel: editForm.value.sensitivityLevel,
        fullContent: editForm.value.fullContent.trim() || undefined
      };
      await updateMemoryItem(editItemTarget.value.id, req);
      ElMessage.success('记忆条目已更新');
      editDialogVisible.value = false;
      await loadMemories();
      await loadOverview();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '更新记忆失败';
      ElMessage.error(message);
    }
  }

  function forgetSingle(item: MemoryItemVO) {
    const displayTitle = item.summary ? `${item.summary.substring(0, 16)}...` : `#${item.id}`;
    ElMessageBox.confirm(`确认让 Agent 物理遗忘条目【${displayTitle}】吗？`, '确认遗忘', {
      confirmButtonText: '确认物理擦除',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await forgetMemoryItem(item.id);
        memoryItems.value = memoryItems.value.filter((i) => i.id !== item.id);
        ElMessage.success('条目已从知识库与向量空间安全物理擦除');
        await loadOverview();
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
        reason: score > 3 ? '用户标记记忆准确有效' : '用户标记记忆偏差，行使遗忘'
      });
      if (score <= 2) {
        memoryItems.value = memoryItems.value.filter((i) => i.id !== item.id);
        ElMessage.success('已记录负向反馈并物理遗忘该条目');
      } else {
        ElMessage.success('感谢正向反馈，Agent 将在后续提示词编排中提升此项权重');
      }
      await loadOverview();
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
      const res = await retrieveMemories(queryPrompt.value.trim(), selectedCourseId.value);
      recalledItems.value = res?.data || [];
      if (recalledItems.value.length === 0) {
        ElMessage.info('未召回相关记忆条目（可能因相关度阈值过滤或暂未开启知情授权）');
      } else {
        ElMessage.success(`已完成 Top-${recalledItems.value.length} 记忆语义召回匹配`);
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '检索召回失败';
      ElMessage.error(message);
      recalledItems.value = [];
    }
  }

  onMounted(async () => {
    await Promise.all([loadOverview(), loadMemories()]);
  });

  return {
    loading,
    overviewLoading,
    seeding,
    extracting,
    selectedCourseId,
    overview,
    spaces,
    consentGranted,
    retentionDays,
    searchKeyword,
    typeFilter,
    memoryItems,
    preferenceCount,
    profileCount,
    episodicCount,
    feedbackCount,
    encryptedCount,
    filteredMemories,
    createDialogVisible,
    createForm,
    editDialogVisible,
    editForm,
    recallDrawerVisible,
    queryPrompt,
    recalledItems,
    extractDialogVisible,
    extractedCandidates,
    loadOverview,
    loadMemories,
    handleSwitchCourse,
    handleConsentChange,
    handleForgetAll,
    handleSeedSample,
    handleExtractMemories,
    handleConfirmExtract,
    confirmingCandidates,
    handleCancelExtract,
    handleCleanupDuplicates,
    cleaningDuplicates,
    hasDuplicates,
    handleDecryptItem,
    openCreateDialog,
    submitCreateMemory,
    openEditDialog,
    submitEditMemory,
    forgetSingle,
    giveFeedback,
    openRecallTester,
    doRetrieve,
    getTypeLabel: getMemoryTypeLabel,
    getTypeTagType: getMemoryTypeTagType
  };
}
