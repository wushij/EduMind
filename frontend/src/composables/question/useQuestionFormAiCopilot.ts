import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { generateQuestions } from '@/api/ai/generation';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import { buildQuestionFormAiPromptDirective } from '@/utils/question/question-form-ai-prompt';
import { normalizeQuestion, hasFilledQuestionOptions } from '@/utils/question/normalize-question';
import { isGarbageQuestionStem } from '@/utils/question/is-garbage-question-stem';
import type { Question } from '@/types/question/question';

function resolveCourseLabel(form: Partial<Question>, courseId: number): string {
  return (
    form.courseName?.trim() ||
    (form as { title?: string }).title?.trim() ||
    `课程 #${courseId}`
  );
}

function assertCourseAndStem(form: Partial<Question>): number | null {
  const courseId = Number(form.courseId);
  if (!Number.isFinite(courseId) || courseId <= 0) {
    ElMessage.warning('请先选择所属课程');
    return null;
  }
  if (!form.stem || form.stem.trim().length < 3) {
    ElMessage.warning('请先输入清晰的试题题干');
    return null;
  }
  return courseId;
}

async function callQuestionAssist(
  mode: Parameters<typeof buildQuestionFormAiPromptDirective>[0],
  form: Partial<Question>
) {
  const courseId = assertCourseAndStem(form);
  if (courseId == null) return null;

  const res = await generateQuestions({
    courseId,
    questionTypes: [form.type || 'SINGLE_CHOICE'],
    count: 1,
    difficulty: form.difficulty || 'MEDIUM',
    scorePerQuestion: form.score || 5,
    promptDirective: buildQuestionFormAiPromptDirective(mode, form, resolveCourseLabel(form, courseId)),
    knowledgePointNames: form.knowledgePointNames
  });

  const generated = normalizeQuestion(res.data?.[0] || {});
  if (!generated.stem || isGarbageQuestionStem(generated.stem)) {
    throw new Error('AI 未返回有效试题');
  }
  return generated;
}

export function useQuestionFormAiCopilot() {
  const aiOptionsLoading = ref(false);
  const aiAnalysisLoading = ref(false);
  const aiPolishLoading = ref(false);
  const aiSuggestLoading = ref(false);
  const aiFullAutoLoading = ref(false);

  const teachingCopilotStore = useTeachingCopilotStore();

  async function generateAiOptions(form: Partial<Question>) {
    if (form.type !== 'SINGLE_CHOICE' && form.type !== 'MULTIPLE_CHOICE') {
      ElMessage.info('当前题型非选择题，无需生成选项列表');
      return;
    }

    aiOptionsLoading.value = true;
    try {
      const generated = await callQuestionAssist('options', form);
      if (!generated?.options?.length || !hasFilledQuestionOptions(generated.options)) {
        throw new Error('AI 未返回可写入的选项内容');
      }
      form.options = generated.options;
      form.correctAnswer =
        generated.correctAnswer || generated.options.find((o) => o.isCorrect)?.key || 'A';
      ElMessage.success('AI 已成功基于题干生成选项并标记正确项');
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, 'AI 选项生成失败，请稍后重试'));
    } finally {
      aiOptionsLoading.value = false;
    }
  }

  async function generateAiAnalysis(form: Partial<Question>) {
    aiAnalysisLoading.value = true;
    try {
      const generated = await callQuestionAssist('analysis', form);
      if (!generated?.analysis || generated.analysis.length < 10) {
        throw new Error('AI 未返回有效解析');
      }
      form.analysis = generated.analysis;
      ElMessage.success('AI 深度解析已自动生成并填充');
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, 'AI 解析生成失败，请稍后重试'));
    } finally {
      aiAnalysisLoading.value = false;
    }
  }

  async function polishStem(form: Partial<Question>) {
    aiPolishLoading.value = true;
    try {
      const generated = await callQuestionAssist('polish', form);
      if (!generated?.stem) {
        throw new Error('AI 未返回润色题干');
      }
      form.stem = generated.stem;
      ElMessage.success('题干已完成 AI 学术化规范润色');
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, 'AI 题干润色失败，请稍后重试'));
    } finally {
      aiPolishLoading.value = false;
    }
  }

  async function suggestKpsAndDifficulty(form: Partial<Question>) {
    aiSuggestLoading.value = true;
    try {
      const generated = await callQuestionAssist('fullAuto', form);
      if (!generated) return;
      if (generated.knowledgePointNames?.length) {
        if (!form.knowledgePointNames) form.knowledgePointNames = [];
        generated.knowledgePointNames.forEach((kp) => {
          if (!form.knowledgePointNames!.includes(kp)) {
            form.knowledgePointNames!.push(kp);
          }
        });
      }
      if (generated.difficulty) form.difficulty = generated.difficulty;
      if (generated.score) form.score = generated.score;
      ElMessage.success('AI 已推荐考点并更新难度/分值建议');
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, 'AI 考点推荐失败，请稍后重试'));
    } finally {
      aiSuggestLoading.value = false;
    }
  }

  async function fullAutoAssist(form: Partial<Question>) {
    aiFullAutoLoading.value = true;
    try {
      const generated = await callQuestionAssist('fullAuto', form);
      if (!generated) return;
      if (generated.stem) form.stem = generated.stem;
      if (generated.options?.length && generated.options.length >= 2) {
        form.options = generated.options;
        form.correctAnswer =
          generated.correctAnswer || generated.options.find((o) => o.isCorrect)?.key || 'A';
      }
      if (generated.analysis) form.analysis = generated.analysis;
      if (generated.difficulty) form.difficulty = generated.difficulty;
      if (generated.knowledgePointNames?.length) {
        form.knowledgePointNames = [...generated.knowledgePointNames];
      }
      ElMessage.success('AI 已完成整道题目的全套生成与教研装配');
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, 'AI 全套生成失败，请稍后重试'));
    } finally {
      aiFullAutoLoading.value = false;
    }
  }

  /** 全局侧边栏 AI 助教（真实 /ai/assistant/ask 链路） */
  function openInteractiveAiTutor(form: Partial<Question>) {
    if (!form.stem || !form.stem.trim()) {
      ElMessage.warning('请先在表单中填写试题题干后再咨询 AI 命题助教');
      return;
    }

    const courseId = Number(form.courseId);
    if (!Number.isFinite(courseId) || courseId <= 0) {
      ElMessage.warning('请先选择所属课程');
      return;
    }

    const title = form.stem.slice(0, 30) + (form.stem.length > 30 ? '...' : '');
    const excerpt = `当前题目状态：\n题型：${form.type}\n课程：${resolveCourseLabel(form, courseId)}\n题干：${form.stem}\n选项：${(form.options || []).map((o) => `${o.key}.${o.content}`).join('; ') || '无'}\n参考答案：${form.correctAnswer || '未定'}`;

    teachingCopilotStore.openAssistantWithContext(
      {
        contextModule: 'question_bank',
        courseId,
        title: `录题辅导 · ${title}`,
        draftTitle: title,
        draftExcerpt: excerpt
      },
      `请针对我当前正在录入的试题进行命题质量评估与改进建议（须结合题干学科与课程，不要套用无关模板题）：\n${excerpt}`,
      { autoSend: true }
    );
  }

  return {
    aiOptionsLoading,
    aiAnalysisLoading,
    aiPolishLoading,
    aiSuggestLoading,
    aiFullAutoLoading,
    generateAiOptions,
    generateAiAnalysis,
    polishStem,
    suggestKpsAndDifficulty,
    fullAutoAssist,
    openInteractiveAiTutor
  };
}
