import { computed, inject, type ComputedRef, type InjectionKey, type Ref } from 'vue';
import type { Course } from '@/types/course/course';
import { useAuthStore } from '@/stores/auth/auth';

export const courseDetailInjectionKey: InjectionKey<Ref<Course | null>> = Symbol('courseDetail');

type CourseSource = Course | null | undefined | Ref<Course | null | undefined> | (() => Course | null | undefined);

function resolveCourse(source?: CourseSource): Course | null | undefined {
  if (typeof source === 'function') {
    return source();
  }
  if (source && typeof source === 'object' && 'value' in source) {
    return source.value;
  }
  return source;
}

function isCourseOwner(course: Course | null | undefined): boolean {
  if (!course?.teacherId) {
    return false;
  }
  const authStore = useAuthStore();
  const uid = authStore.currentUser?.id;
  if (uid == null) {
    return false;
  }
  return Number(course.teacherId) === Number(uid);
}

export function useCourseEditable(course?: CourseSource): ComputedRef<boolean> {
  const injected = inject(courseDetailInjectionKey, null);
  return computed(() => {
    const fromProp = resolveCourse(course);
    const resolved = fromProp ?? injected?.value ?? null;

    if (resolved?.editable === true) {
      return true;
    }
    if (isCourseOwner(resolved)) {
      return true;
    }
    if (resolved?.editable === false) {
      return false;
    }
    return false;
  });
}
