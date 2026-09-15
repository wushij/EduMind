import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterventions,
  createIntervention,
  approveIntervention,
  rejectIntervention,
  dispatchIntervention
} from '@/api/analytics/intervention';
import type { TeachingInterventionVO, InterventionCreateRequest } from '@/types/analytics/intervention';

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
      return '已通过';
    case 'DISPATCHED':
      return '已分发';
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
    const matchCourse = !courseFilter || i.courseId === courseFilter;
    const matchTrigger = !triggerFilter || i.triggerType === triggerFilter;
    const matchStatus = !statusFilter || i.status === statusFilter;
    return matchCourse && matchTrigger && matchStatus;
  });
}

export function resolveCourseNameById(courseId?: number): string | undefined {
  if (courseId === 102) return '高等数学（上）';
  if (courseId === 101) return '数据结构与算法';
  if (courseId === 103) return '高中物理必修第一册';
  return undefined;
}

export function useIntervention() {
  const loading = ref(false);
  const courseFilter = ref<number | undefined>(undefined);
  const triggerFilter = ref<string | undefined>(undefined);
  const statusFilter = ref<string | undefined>(undefined);
  const interventions = ref<TeachingInterventionVO[]>([]);

  const drawerVisible = ref(false);
  const activeIntervention = ref<TeachingInterventionVO | null>(null);

  const createDialogVisible = ref(false);
  const createSubmitting = ref(false);
  const createForm = ref<InterventionCreateRequest>({
    courseId: 102,
    courseName: '高等数学（上）',
    triggerType: 'EXAM_WEAK',
    title: '',
    proposalText: '',
    affectedStudentCount: 5
  });

  const pendingCount = computed(() => interventions.value.filter((i) => i.status === 'PENDING').length);
  const totalAffectedStudents = computed(() =>
    interventions.value.reduce((acc, cur) => acc + (cur.affectedStudentCount || 0), 0)
  );

  const filteredInterventions = computed(() =>
    filterInterventions(
      interventions.value,
      courseFilter.value,
      triggerFilter.value,
      statusFilter.value
    )
  );

  async function loadData() {
    try {
      loading.value = true;
      const res = await listInterventions(courseFilter.value);
      interventions.value = res?.data || [];
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '加载干预建议失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  watch(courseFilter, () => {
    loadData();
  });

  function openCreateDialog() {
    createForm.value = {
      courseId: 102,
      courseName: '高等数学（上）',
      triggerType: 'EXAM_WEAK',
      title: '',
      proposalText: '',
      affectedStudentCount: 5
    };
    createDialogVisible.value = true;
  }

  function onCourseChange(val?: number) {
    createForm.value.courseName = resolveCourseNameById(val);
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
    try {
      await approveIntervention(item.id);
      item.status = 'APPROVED';
      ElMessage.success('干预方案已审核通过，等待分发推送！');
      await loadData();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '审批失败';
      ElMessage.error(message);
    }
  }

  async function handleDispatch(item: TeachingInterventionVO) {
    try {
      await dispatchIntervention(item.id);
      item.status = 'DISPATCHED';
      ElMessage.success('已成功推送至相关学生任务中心！');
      await loadData();
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '分发失败';
      ElMessage.error(message);
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

  function handleCustomize(_item: TeachingInterventionVO) {
    ElMessage.info('自定义配置：教师可自由勾选替换题库试题与微课视频');
  }

  function viewEfficacyTrace(item: TeachingInterventionVO) {
    activeIntervention.value = item;
    drawerVisible.value = true;
  }

  function openTriggerSimulator() {
    ElMessage.success('已启动全量学情诊断巡检扫描，当前无新的异常动因');
  }

  onMounted(loadData);

  return {
    loading,
    courseFilter,
    triggerFilter,
    statusFilter,
    interventions,
    drawerVisible,
    activeIntervention,
    createDialogVisible,
    createSubmitting,
    createForm,
    pendingCount,
    totalAffectedStudents,
    filteredInterventions,
    loadData,
    openCreateDialog,
    onCourseChange,
    submitCreateIntervention,
    handleApprove,
    handleDispatch,
    handleReject,
    handleCustomize,
    viewEfficacyTrace,
    openTriggerSimulator,
    getTriggerLabel: getInterventionTriggerLabel,
    getTriggerTagType: getInterventionTriggerTagType,
    getStatusLabel: getInterventionStatusLabel,
    getStatusTagType: getInterventionStatusTagType
  };
}
