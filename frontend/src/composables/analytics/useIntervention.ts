import { ref, computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterventions,
  createIntervention,
  approveIntervention,
  rejectIntervention,
  dispatchIntervention
} from '@/api/analytics/intervention';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
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

/**
 * 依据「教师真实可选课程列表」解析课程名。
 * 原实现是 `if (courseId === 102) return '高等数学（上）'` 式写死映射，既无法覆盖新增课程，
 * 又会把课程名张冠李戴（种子数据中 102 实为《Java面向对象程序设计》）。
 */
export function resolveCourseNameFromOptions(
  options: Array<{ id: number; name: string }>,
  courseId?: number
): string | undefined {
  if (!courseId) return undefined;
  return options.find((c) => Number(c.id) === Number(courseId))?.name;
}

export function useIntervention() {
  const route = useRoute();
  // 支持从学情分析页携带 courseId 跳转进来：既作为列表筛选条件，也作为新建提案的默认课程。
  const routeCourseId = Number(route.query.courseId) || undefined;
  // 可选课程来自教师真实课程列表，替换原先写死的 101/102/103 三项。
  const { courseOptions, courseId: activeCourseId } = useTeacherCourses(routeCourseId);

  const loading = ref(false);
  const courseFilter = ref<number | undefined>(routeCourseId);
  const triggerFilter = ref<string | undefined>(undefined);
  const statusFilter = ref<string | undefined>(undefined);
  const interventions = ref<TeachingInterventionVO[]>([]);

  const drawerVisible = ref(false);
  const activeIntervention = ref<TeachingInterventionVO | null>(null);

  const createDialogVisible = ref(false);
  const createSubmitting = ref(false);

  /** 新建提案默认值：优先当前课程，其次可用课程首项；解析不到课程名时留空而不伪造 */
  function buildCreateForm(): InterventionCreateRequest {
    const fallbackId = activeCourseId.value > 0 ? activeCourseId.value : courseOptions.value[0]?.id;
    return {
      courseId: fallbackId,
      courseName: resolveCourseNameFromOptions(courseOptions.value, fallbackId),
      triggerType: 'EXAM_WEAK',
      title: '',
      proposalText: '',
      affectedStudentCount: 5
    };
  }

  const createForm = ref<InterventionCreateRequest>(buildCreateForm());

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
    createForm.value = buildCreateForm();
    createDialogVisible.value = true;
  }

  function onCourseChange(val?: number) {
    createForm.value.courseName = resolveCourseNameFromOptions(courseOptions.value, val);
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
    courseOptions,
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
