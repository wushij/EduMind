import { ref, computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterventions,
  getInterventionOverviewStats,
  scanInterventionTrigger,
  generateAiInterventionProposal,
  createIntervention,
  customizeIntervention,
  approveIntervention,
  rejectIntervention,
  dispatchIntervention,
  deleteIntervention
} from '@/api/analytics/intervention';
import { getCourseKnowledgePoints } from '@/api/course/knowledge-point';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import type {
  TeachingInterventionVO,
  InterventionOverviewStatsVO,
  InterventionCreateRequest,
  InterventionActionRequest
} from '@/types/analytics/intervention';

export function getInterventionTriggerLabel(type: string) {
  switch (type) {
    case 'EXAM_WEAK':
      return '考试薄弱断层';
    case 'ACTIVITY_DROP':
      return '活跃度异动';
    case 'HOMEWORK_DELAY':
      return '作业滞后';
    default:
      return '学情异动';
  }
}

export function getInterventionTriggerTagType(type: string) {
  switch (type) {
    case 'EXAM_WEAK':
      return 'danger';
    case 'ACTIVITY_DROP':
      return 'warning';
    case 'HOMEWORK_DELAY':
      return 'info';
    default:
      return 'primary';
  }
}

export function getInterventionStatusLabel(status: string) {
  switch (status) {
    case 'PENDING':
      return '待审核';
    case 'APPROVED':
      return '已批准待发';
    case 'DISPATCHED':
      return '执行追踪中';
    case 'REVOKED':
      return '已驳回';
    default:
      return status;
  }
}

export function getInterventionStatusTagType(status: string) {
  switch (status) {
    case 'PENDING':
      return 'danger';
    case 'APPROVED':
      return 'primary';
    case 'DISPATCHED':
      return 'success';
    default:
      return 'info';
  }
}

export function filterInterventions(
  items: TeachingInterventionVO[],
  courseFilter?: number,
  triggerFilter?: string,
  statusFilter?: string
) {
  return items.filter((i) => {
    const matchCourse = !courseFilter || (i.courseId != null && Number(i.courseId) === Number(courseFilter));
    const matchTrigger = !triggerFilter || i.triggerType === triggerFilter;
    const matchStatus = !statusFilter || i.status === statusFilter;
    return matchCourse && matchTrigger && matchStatus;
  });
}

export function resolveCourseNameFromOptions(
  options: Array<{ id: number; name: string }>,
  courseId?: number
): string | undefined {
  if (!courseId) return undefined;
  return options.find((c) => Number(c.id) === Number(courseId))?.name;
}

export function useIntervention() {
  const route = useRoute();
  const routeCourseId = Number(route.query.courseId) || undefined;
  const { courseOptions, courseId: activeCourseId } = useTeacherCourses(routeCourseId);

  const loading = ref(false);
  const scanLoading = ref(false);
  const courseFilter = ref<number | undefined>(routeCourseId);
  const triggerFilter = ref<string | undefined>(undefined);
  const statusFilter = ref<string | undefined>(undefined);
  const activeTab = ref<string>('all'); // all, PENDING, APPROVED, DISPATCHED
  const interventions = ref<TeachingInterventionVO[]>([]);

  /**
   * 顶部统计概览完全由 overview-stats 接口填充。
   * 这里曾经预置 avgImprovementRate 15.4 / completionRate 95.6 这类"看着合理"的数字，
   * 一旦接口失败或字段缺失，页面就会把它们当成班级真实学情展示给教师，
   * 属于伪造统计结果，因此初始值置空。
   */
  const overviewStats = ref<InterventionOverviewStatsVO | null>(null);

  const drawerVisible = ref(false);
  const activeIntervention = ref<TeachingInterventionVO | null>(null);

  /**
   * 单条动作在途锁：审批 / 下发这类会改变记录状态并产生副作用（给学生推送通知）的动作，
   * 必须保证「同一条记录同一时刻只有一个请求在途」。
   *
   * 为什么必须有：ElButton 默认不做防抖，用户快速双击会连发两个请求，
   * 第一个把状态改为 APPROVED 并提交，第二个再进来时状态已变，后端就会返回
   * 「不是待审核状态」的 400，用户看到的却是自己只点了一次。
   * 后端已做幂等兜底，前端这里负责从源头杜绝重复请求。
   */
  const approvingId = ref<number | null>(null);
  const dispatchingId = ref<number | null>(null);

  const createDialogVisible = ref(false);
  const createSubmitting = ref(false);

  // 自定义调整微课与习题弹窗
  const customizeDialogVisible = ref(false);
  const customizeSubmitting = ref(false);
  const customizeForm = ref<{
    id: number;
    title: string;
    proposalText: string;
    remark: string;
    resourceTitle: string;
    resourceDuration: string;
    questionStem: string;
    questionDifficulty: string;
  }>({
    id: 0,
    title: '',
    proposalText: '',
    remark: '',
    resourceTitle: '',
    resourceDuration: '8分30秒',
    questionStem: '',
    questionDifficulty: 'MEDIUM'
  });

  function buildCreateForm(): InterventionCreateRequest {
    const fallbackId = (courseFilter.value && courseFilter.value > 0)
      ? courseFilter.value
      : (activeCourseId.value > 0 ? activeCourseId.value : courseOptions.value[0]?.id);

    const paramKpTitle = (route.query.knowledgePointTitle as string) || '';
    const paramKpId = Number(route.query.knowledgePointId) || undefined;

    return {
      courseId: fallbackId,
      courseName: resolveCourseNameFromOptions(courseOptions.value, fallbackId),
      knowledgePointId: paramKpId,
      knowledgePointTitle: paramKpTitle,
      triggerType: 'EXAM_WEAK',
      title: paramKpTitle ? `针对「${paramKpTitle}」的靶向巩固干预` : '',
      proposalText: paramKpTitle
        ? `学情诊断发现该班级在考点「${paramKpTitle}」掌握度偏低，建议定向分发攻坚微课与梯度变式题组。`
        : '',
      // 覆盖人数与预期提分必须由教师按本班实际填写：
      // 原先预填 5 人与 "+15% ~ +22%"，教师不修改就会把这些编造数字提交成提案结论。
      affectedStudentCount: undefined,
      expectedImprovement: ''
    };
  }

  const createForm = ref<InterventionCreateRequest>(buildCreateForm());

  const pendingCount = computed(() => overviewStats.value?.pendingCount ?? interventions.value.filter((i) => i.status === 'PENDING').length);
  const totalAffectedStudents = computed(() => overviewStats.value?.totalAffectedStudents ?? interventions.value.reduce((acc, cur) => acc + (cur.affectedStudentCount || 0), 0));

  /**
   * 掌握度提升与闭环完成率没有可从提案列表推导的兜底口径：
   * 后端返回空值时保持 null，由页面渲染 '—'。
   * 绝不用 15.2 / 95.8 这类写死常量兜底，那会把编造的数字包装成班级真实学情。
   */
  const avgImprovementRate = computed<number | null>(() => overviewStats.value?.avgImprovementRate ?? null);
  const completionRate = computed<number | null>(() => overviewStats.value?.completionRate ?? null);

  const filteredInterventions = computed(() => {
    let list = filterInterventions(
      interventions.value,
      courseFilter.value,
      triggerFilter.value,
      statusFilter.value
    );
    if (activeTab.value !== 'all') {
      list = list.filter((i) => i.status === activeTab.value);
    }
    return list;
  });

  async function loadData() {
    try {
      loading.value = true;
      const [listRes, statsRes] = await Promise.all([
        listInterventions(courseFilter.value),
        getInterventionOverviewStats(courseFilter.value)
      ]);
      interventions.value = listRes?.data || [];
      // 每次加载都整体覆盖：接口未返回时置空，避免切换课程后仍残留上一门课的 KPI
      overviewStats.value = statsRes?.data ?? null;
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '加载干预建议失败';
      overviewStats.value = null;
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  watch(courseFilter, () => {
    loadData();
  });

  // 关联考点联动
  const knowledgePoints = ref<KnowledgePoint[]>([]);
  const kpLoading = ref(false);

  // AI 认知推演面板状态与计时中止
  const aiThinkingModalVisible = ref(false);
  let aiAbortRequested = false;
  // 推演会话是否真正进行中：用于让中止流程幂等，防止「面板 abort + 弹窗 close」重复触发
  let aiThinkingSessionActive = false;

  async function loadCourseKnowledgePoints(courseId?: number) {
    if (!courseId) {
      knowledgePoints.value = [];
      return;
    }
    try {
      kpLoading.value = true;
      const res = await getCourseKnowledgePoints(courseId);
      knowledgePoints.value = (res as any)?.data || [];
    } catch {
      knowledgePoints.value = [];
    } finally {
      kpLoading.value = false;
    }
  }

  /**
   * 中止 AI 认知推演（幂等）
   *
   * 为什么需要幂等守卫：ElDialog 只要 visible 变为 false 就会在离场过渡时 emit('close')，
   * 因此「点击面板中止按钮」这一动作会先后触发两次本方法：
   *   1. 面板 $emit('abort') → 本方法（第一次，真正执行中止）
   *   2. 第 1 步把 aiThinkingModalVisible 置 false → 弹窗离场 → @close → 本方法（第二次，重复）
   * 推演正常结束的 finally 里置 false 同样会触发 dialog close。
   * 因此仅当会话真正进行中才执行中止，避免重复弹提示、以及成功完成后误报「已中止」。
   */
  function stopAiThinking() {
    if (!aiThinkingSessionActive) return;
    aiThinkingSessionActive = false;
    aiAbortRequested = true;
    aiThinkingModalVisible.value = false;
    scanLoading.value = false;
    ElMessage.info('已中止当前 AI 认知推演');
  }

  async function openCreateDialog() {
    createForm.value = buildCreateForm();
    createDialogVisible.value = true;
    if (createForm.value.courseId) {
      await loadCourseKnowledgePoints(createForm.value.courseId);
    }
  }

  async function onCourseChange(val?: number) {
    createForm.value.courseName = resolveCourseNameFromOptions(courseOptions.value, val);
    createForm.value.knowledgePointId = undefined;
    createForm.value.knowledgePointTitle = '';
    if (val) {
      await loadCourseKnowledgePoints(val);
    } else {
      knowledgePoints.value = [];
    }
  }

  function onKnowledgePointChange(kpId?: number) {
    if (!kpId) {
      createForm.value.knowledgePointId = undefined;
      createForm.value.knowledgePointTitle = '';
      return;
    }
    const found = knowledgePoints.value.find((p) => p.id === kpId);
    if (found) {
      createForm.value.knowledgePointId = found.id;
      createForm.value.knowledgePointTitle = found.title;
      createForm.value.title = `针对「${found.title}」的考点薄弱干预`;
      createForm.value.proposalText = `学情巡检发现该班级在考点「${found.title}」存在掌握度断层，建议定向分发针对性精讲微课与变式攻坚题组。`;
    }
  }

  /**
   * 触发大模型 AI 智能推演生成提案（带计时与中止能力）
   */
  async function triggerAiProposalGeneration() {
    if (!createForm.value.courseId) {
      ElMessage.warning('请先选择需要推演的关联课程');
      return;
    }
    aiAbortRequested = false;
    aiThinkingSessionActive = true;
    aiThinkingModalVisible.value = true;
    try {
      const res = await generateAiInterventionProposal({
        courseId: createForm.value.courseId,
        knowledgePointId: createForm.value.knowledgePointId,
        triggerType: createForm.value.triggerType
      });
      if (aiAbortRequested) return;

      if (res?.data) {
        const data = res.data;
        if (data.title) createForm.value.title = data.title;
        if (data.proposalText) createForm.value.proposalText = data.proposalText;
        if (data.knowledgePointId) createForm.value.knowledgePointId = data.knowledgePointId;
        if (data.knowledgePointTitle) createForm.value.knowledgePointTitle = data.knowledgePointTitle;
        if (data.affectedStudentCount) createForm.value.affectedStudentCount = data.affectedStudentCount;
        if (data.expectedImprovement) createForm.value.expectedImprovement = data.expectedImprovement;
        ElMessage.success('AI 认知引擎推演完成！已自动回填针对性干预方案与教学资源');
      }
    } catch (e: unknown) {
      if (!aiAbortRequested) {
        const message = e instanceof Error ? e.message : 'AI 推演失败';
        ElMessage.error(message);
      }
    } finally {
      aiThinkingSessionActive = false;
      aiThinkingModalVisible.value = false;
    }
  }

  async function submitCreateIntervention() {
    if (!createForm.value.title?.trim() || !createForm.value.proposalText?.trim()) {
      ElMessage.warning('请完整填写干预提案标题与方案描述');
      return;
    }
    try {
      createSubmitting.value = true;
      await createIntervention(createForm.value);
      ElMessage.success('干预提案已成功创建！');
      createDialogVisible.value = false;
      await loadData();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '创建干预提案失败';
      ElMessage.error(message);
    } finally {
      createSubmitting.value = false;
    }
  }

  async function handleApprove(item: TeachingInterventionVO) {
    // 已有审批请求在途时直接忽略，避免双击造成「重复审批」报错
    if (approvingId.value !== null) return;
    approvingId.value = item.id;
    try {
      await approveIntervention(item.id);
      item.status = 'APPROVED';
      ElMessage.success('干预方案已审核通过，等待分发推送！');
      await loadData();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '审批失败';
      ElMessage.error(message);
    } finally {
      approvingId.value = null;
    }
  }

  async function handleDispatch(item: TeachingInterventionVO) {
    // 已有下发请求在途时直接忽略，避免重复给学生推送通知
    if (dispatchingId.value !== null) return;
    dispatchingId.value = item.id;
    try {
      await dispatchIntervention(item.id);
      item.status = 'DISPATCHED';
      ElMessage.success('已成功推送至相关学生任务中心！');
      await loadData();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '分发失败';
      ElMessage.error(message);
    } finally {
      dispatchingId.value = null;
    }
  }

  function handleReject(item: TeachingInterventionVO) {
    ElMessageBox.confirm('确认驳回此项干预建议吗？', '驳回确认', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await rejectIntervention(item.id);
        item.status = 'REVOKED';
        ElMessage.info('已驳回该提案');
        await loadData();
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '驳回失败';
        ElMessage.error(message);
      }
    });
  }

  async function handleDelete(item: TeachingInterventionVO) {
    try {
      await ElMessageBox.confirm(
        `确定要彻底删除该干预提案「${item.title}」吗？删除后不可恢复。`,
        '删除干预提案',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deleteIntervention(item.id);
      ElMessage.success('干预提案已成功删除');
      await loadData();
    } catch (action) {
      if (action !== 'cancel') {
        const message = action instanceof Error ? action.message : '删除失败';
        ElMessage.error(message);
      }
    }
  }

  function handleCustomize(item: TeachingInterventionVO) {
    activeIntervention.value = item;
    customizeForm.value = {
      id: item.id,
      title: item.title,
      proposalText: item.proposalText,
      remark: '',
      resourceTitle: item.resources?.[0]?.title || '《精讲核心概念与解题陷阱》',
      resourceDuration: item.resources?.[0]?.duration || '8分30秒',
      questionStem: item.questions?.[0]?.stem || '针对该考点的典型应用，下列表述正确的是？',
      questionDifficulty: item.questions?.[0]?.difficulty || 'MEDIUM'
    };
    customizeDialogVisible.value = true;
  }

  async function submitCustomize() {
    if (!customizeForm.value.id) return;
    try {
      customizeSubmitting.value = true;
      const req: InterventionActionRequest = {
        proposalText: customizeForm.value.proposalText,
        remark: customizeForm.value.remark
      };
      await customizeIntervention(customizeForm.value.id, req);
      ElMessage.success('自定义微课与试题配置已保存');
      customizeDialogVisible.value = false;
      await loadData();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '保存配置失败';
      ElMessage.error(message);
    } finally {
      customizeSubmitting.value = false;
    }
  }

  function viewEfficacyTrace(item: TeachingInterventionVO) {
    activeIntervention.value = item;
    drawerVisible.value = true;
  }

  /**
   * 真实学情诊断巡检扫描：联动 AI 认知推演面板与后端 scanInterventionTrigger 接口
   */
  async function handleScanTrigger() {
    const targetCourseId = courseFilter.value || activeCourseId.value || courseOptions.value[0]?.id;
    if (!targetCourseId) {
      ElMessage.warning('请先在上方筛选框选择需要巡检的课程');
      return;
    }
    aiAbortRequested = false;
    aiThinkingSessionActive = true;
    aiThinkingModalVisible.value = true;
    scanLoading.value = true;
    try {
      const res = await scanInterventionTrigger(targetCourseId);
      if (aiAbortRequested) return;
      const kpTitle = res?.data?.knowledgePointTitle || '学情薄弱考点';
      ElMessage.success({
        message: `学情诊断巡检完成！已针对薄弱考点「${kpTitle}」自动生成精准教学干预预案`,
        duration: 4000
      });
      await loadData();
    } catch (e: unknown) {
      if (!aiAbortRequested) {
        const message = e instanceof Error ? e.message : '诊断巡检扫描失败';
        ElMessage.error(message);
      }
    } finally {
      aiThinkingSessionActive = false;
      scanLoading.value = false;
      aiThinkingModalVisible.value = false;
    }
  }

  onMounted(async () => {
    await loadData();
    // 检查是否从学情概览/分析携带 autoCreate 路由进入
    if (route.query.autoCreate === 'true' || route.query.knowledgePointTitle) {
      openCreateDialog();
    }
  });

  return {
    loading,
    scanLoading,
    courseOptions,
    courseFilter,
    triggerFilter,
    statusFilter,
    activeTab,
    interventions,
    drawerVisible,
    activeIntervention,
    approvingId,
    dispatchingId,
    createDialogVisible,
    createSubmitting,
    createForm,
    knowledgePoints,
    kpLoading,
    loadCourseKnowledgePoints,
    onKnowledgePointChange,
    aiThinkingModalVisible,
    stopAiThinking,
    triggerAiProposalGeneration,
    customizeDialogVisible,
    customizeSubmitting,
    customizeForm,
    overviewStats,
    pendingCount,
    totalAffectedStudents,
    avgImprovementRate,
    completionRate,
    filteredInterventions,
    loadData,
    openCreateDialog,
    onCourseChange,
    submitCreateIntervention,
    handleApprove,
    handleDispatch,
    handleReject,
    handleDelete,
    handleCustomize,
    submitCustomize,
    viewEfficacyTrace,
    handleScanTrigger,
    getTriggerLabel: getInterventionTriggerLabel,
    getTriggerTagType: getInterventionTriggerTagType,
    getStatusLabel: getInterventionStatusLabel,
    getStatusTagType: getInterventionStatusTagType
  };
}
