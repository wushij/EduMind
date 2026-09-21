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
      // 锚定新题目时开启新会话，避免沿用上一题的历史消息与追问
      { autoSend: false, startNewSession: true }
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
