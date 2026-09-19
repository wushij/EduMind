import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useCourseMember } from '@/composables/course/useCourseMember';
import type { CourseMemberItem } from '@/types/course/member';
import { normalizeAvatarUrl } from '@/utils/format/file';

export function useCourseMembersPage(courseId: number) {
  const router = useRouter();

  const {
    members,
    loading,
    fetchMembers,
    addMember,
    removeMember
  } = useCourseMember(courseId);

  const searchKeyword = ref('');
  const currentRoleTab = ref('ALL');

  const showAddDialog = ref(false);
  const newUserId = ref<number | null>(null);
  const newMemberRole = ref('STUDENT');
  const adding = ref(false);
  const brokenAvatars = ref<Record<number, boolean>>({});

  function resolveMemberAvatar(member: CourseMemberItem): string | undefined {
    return normalizeAvatarUrl(member.avatar);
  }

  function markAvatarBroken(userId: number) {
    brokenAvatars.value[userId] = true;
  }

  const studentMembers = computed(() => members.value.filter(m => m.memberRole === 'STUDENT'));
  const staffMembers = computed(() => members.value.filter(m => m.memberRole === 'TEACHER' || m.memberRole === 'ASSISTANT'));

  const averageProgress = computed(() => {
    const withProgress = studentMembers.value.filter(m => m.progress != null && !Number.isNaN(m.progress));
    if (withProgress.length === 0) return 0;
    const sum = withProgress.reduce((acc, cur) => acc + (cur.progress as number), 0);
    return Math.round(sum / withProgress.length);
  });

  const roleTabs = computed(() => [
    { label: '全部成员', value: 'ALL', count: members.value.length },
    { label: '选课学生', value: 'STUDENT', count: studentMembers.value.length },
    { label: '教学团队', value: 'STAFF', count: staffMembers.value.length }
  ]);

  const filteredMembers = computed(() => {
    return members.value.filter(m => {
      if (currentRoleTab.value === 'STUDENT' && m.memberRole !== 'STUDENT') return false;
      if (currentRoleTab.value === 'STAFF' && m.memberRole !== 'TEACHER' && m.memberRole !== 'ASSISTANT') return false;

      if (searchKeyword.value.trim()) {
        const kw = searchKeyword.value.trim().toLowerCase();
        const inName = (m.realName || '').toLowerCase().includes(kw);
        const inUsername = (m.username || '').toLowerCase().includes(kw);
        const inId = String(m.userId).includes(kw);
        if (!inName && !inUsername && !inId) return false;
      }
      return true;
    });
  });

  function getRoleLabel(role: string) {
    const map: Record<string, string> = {
      TEACHER: '主讲教师',
      ASSISTANT: '课程助教',
      STUDENT: '选课学生'
    };
    return map[role] || '在读学生';
  }

  function getRoleClass(role: string) {
    if (role === 'TEACHER') return 'role-badge--teacher';
    if (role === 'ASSISTANT') return 'role-badge--ta';
    return 'role-badge--student';
  }

  function getAvatarClass(role: string) {
    if (role === 'TEACHER') return 'avatar--teacher';
    if (role === 'ASSISTANT') return 'avatar--ta';
    return 'avatar--student';
  }

  function handleViewPortrait(member: CourseMemberItem) {
    if (member.memberRole === 'TEACHER') {
      router.push({
        path: '/analytics/learning',
        query: { courseId, tab: 'overall' }
      });
    } else {
      router.push({
        path: '/analytics/learning',
        query: { studentId: member.userId, courseId, tab: 'personal' }
      });
    }
  }

  async function handleRemove(userId: number) {
    const member = members.value.find((m) => m.userId === userId);
    const displayName = member?.realName || member?.username || `用户${userId}`;
    try {
      await ElMessageBox.confirm(
        `确定将「${displayName}」移出此课程空间吗？移出后该成员将无法继续访问课程学习内容。`,
        '移出成员确认',
        {
          type: 'warning',
          confirmButtonText: '确定移出',
          cancelButtonText: '取消',
          confirmButtonClass: 'el-button--danger',
          lockScroll: false
        }
      );
    } catch {
      return;
    }
    try {
      await removeMember(userId);
      ElMessage.success('成员已成功移出课程班级');
    } catch (err: any) {
      ElMessage.error(err?.message || '移出成员失败');
    }
  }

  async function handleAddSubmit() {
    if (!newUserId.value) {
      ElMessage.warning('请搜索并选择要录入的用户');
      return;
    }
    adding.value = true;
    try {
      await addMember(newUserId.value, newMemberRole.value);
      ElMessage.success('成功录入选课成员！');
      showAddDialog.value = false;
      newUserId.value = null;
    } catch (err: any) {
      ElMessage.error(err?.message || '录入成员失败');
    } finally {
      adding.value = false;
    }
  }

  onMounted(() => {
    fetchMembers();
  });

  return {
    members,
    loading,
    searchKeyword,
    currentRoleTab,
    showAddDialog,
    newUserId,
    newMemberRole,
    adding,
    brokenAvatars,
    studentMembers,
    staffMembers,
    averageProgress,
    roleTabs,
    filteredMembers,
    resolveMemberAvatar,
    markAvatarBroken,
    getRoleLabel,
    getRoleClass,
    getAvatarClass,
    handleViewPortrait,
    handleRemove,
    handleAddSubmit
  };
}
