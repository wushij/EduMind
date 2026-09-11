import { ref } from 'vue';
import { getCourseMembers, addCourseMember } from '@/api/course/member';
import { getCourseResources } from '@/api/course/resource';

export function useCourseMember(courseId: number) {
  const members = ref<any[]>([]);
  const resources = ref<any[]>([]);
  const loading = ref(false);

  async function fetchMembers() {
    loading.value = true;
    try {
      const res = await getCourseMembers(courseId);
      members.value = res.data || [];
    } catch {
      members.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function addMember(userId: number, role: string) {
    await addCourseMember(courseId, userId, role);
    await fetchMembers();
  }

  async function fetchResources() {
    try {
      const res = await getCourseResources(courseId);
      resources.value = res.data || [];
    } catch {
      resources.value = [];
    }
  }

  return { members, resources, loading, fetchMembers, addMember, fetchResources };
}
