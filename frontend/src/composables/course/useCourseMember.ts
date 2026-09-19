import { ref } from 'vue';
import { getCourseMembers, addCourseMember, removeCourseMember } from '@/api/course/member';
import { getCourseResources, createCourseResource, uploadCourseResource, deleteCourseResource } from '@/api/course/resource';
import type { CourseMemberItem } from '@/types/course/member';
import type { CourseResourceItem } from '@/types/course/resource';

export function useCourseMember(courseId: number) {
  const members = ref<CourseMemberItem[]>([]);
  const resources = ref<CourseResourceItem[]>([]);
  const loading = ref(false);
  const resourceLoading = ref(false);

  async function fetchMembers() {
    loading.value = true;
    try {
      const res = await getCourseMembers(courseId);
      members.value = (res.data || []).map((m) => ({
        ...m,
        progress: m.progress != null ? Math.round(Number(m.progress)) : (m.memberRole === 'TEACHER' ? 100 : 0),
        joinTime: m.joinTime || '',
        status: m.status || 'ACTIVE'
      }));
    } catch (err) {
      members.value = [];
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function addMember(userId: number, role: string) {
    await addCourseMember(courseId, userId, role);
    await fetchMembers();
  }

  async function removeMember(userId: number) {
    await removeCourseMember(courseId, userId);
    members.value = members.value.filter(m => m.userId !== userId);
  }

  async function fetchResources() {
    resourceLoading.value = true;
    try {
      const res = await getCourseResources(courseId);
      resources.value = res.data || [];
    } catch (err) {
      resources.value = [];
      throw err;
    } finally {
      resourceLoading.value = false;
    }
  }

  async function createResource(data: {
    title: string;
    resourceType?: string;
    chapterId?: number;
    resourceId?: number;
    documentId?: number;
  }) {
    await createCourseResource(courseId, data);
    await fetchResources();
  }

  async function uploadResource(
    file: File,
    data: { title: string; resourceType?: string; chapterId?: number }
  ) {
    await uploadCourseResource(courseId, file, data);
    await fetchResources();
  }

  async function deleteResource(resourceId: number) {
    await deleteCourseResource(courseId, resourceId);
    resources.value = resources.value.filter(r => r.id !== resourceId);
  }

  return {
    members,
    resources,
    loading,
    resourceLoading,
    fetchMembers,
    addMember,
    removeMember,
    fetchResources,
    createResource,
    uploadResource,
    deleteResource
  };
}
