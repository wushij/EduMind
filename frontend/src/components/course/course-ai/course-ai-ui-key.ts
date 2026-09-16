import type { InjectionKey } from 'vue';
import type { useCourseAIWorkspace } from '@/composables/course/useCourseAIWorkspace';

export type CourseAiUiContext = ReturnType<typeof useCourseAIWorkspace>;

export const courseAiUiKey: InjectionKey<CourseAiUiContext> = Symbol('courseAiUi');
