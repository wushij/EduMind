import { computed, inject, type ComputedRef, type InjectionKey, type Ref } from 'vue';
import type { Course } from '@/types/course/course';

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

export function useCourseEditable(course?: CourseSource): ComputedRef<boolean> {
  const injected = inject(courseDetailInjectionKey, null);
  return computed(() => {
    const fromProp = resolveCourse(course);
    if (fromProp?.editable !== undefined) {
      return Boolean(fromProp.editable);
    }
    if (injected?.value?.editable !== undefined) {
      return Boolean(injected.value.editable);
    }
    return false;
  });
}
