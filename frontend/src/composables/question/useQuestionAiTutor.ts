import { onUnmounted } from 'vue';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import {
  buildQuestionCopilotExcerpt,
  buildQuestionCopilotTitle
} from '@/utils/question/build-question-copilot-context';
import type { Question } from '@/types/question/question';

export function useQuestionAiTutor(options?: { clearOnUnmount?: boolean }) {
  const teachingCopilotStore = useTeachingCopilotStore();
  const clearOnUnmount = options?.clearOnUnmount ?? false;

  function openQuestionAiTutor(question: Question) {
    const courseId = question.courseId != null ? Number(question.courseId) : undefined;
    const questionId = question.id != null ? Number(question.id) : undefined;
    const title = buildQuestionCopilotTitle(question);
    const excerpt = buildQuestionCopilotExcerpt(question);

    teachingCopilotStore.openAssistantWithContext(
      {
        contextModule: 'question_bank',
        courseId: courseId && !Number.isNaN(courseId) ? courseId : undefined,
        questionId: questionId && !Number.isNaN(questionId) ? questionId : undefined,
        title: `题目辅导 · ${title}`,
        draftTitle: title,
        draftExcerpt: excerpt
      },
      '',
      { autoSend: false }
    );
  }

  if (clearOnUnmount) {
    onUnmounted(() => {
      if (teachingCopilotStore.activeContext?.contextModule === 'question_bank') {
        teachingCopilotStore.clearContext();
      }
    });
  }

  return { openQuestionAiTutor };
}
