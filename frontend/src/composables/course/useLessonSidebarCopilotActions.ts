import { ElMessage } from 'element-plus';
import { unref, type MaybeRef } from 'vue';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import type { TeachingCopilotContext } from '@/types/ai/teaching-copilot-context';
import type { LessonMetaForm } from '@/components/course/lesson/studio/LessonStudioSidebar.vue';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import { createDefaultObjectiveCallout } from '@/composables/course/useLessonDocumentModel';
import type { LessonInsertIntent } from '@/utils/ai/lesson-copilot-intent';

function isDefaultObjectivePlaceholder(text: string): boolean {
  const t = text.trim();
  if (!t) return false;
  return t === createDefaultObjectiveCallout().body.trim();
}

function stripMarkdownForExcerpt(markdown: string): string {
  return markdown
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`[^`]+`/g, ' ')
    .replace(/!\[[^\]]*]\([^)]+\)/g, ' ')
    .replace(/\[[^\]]+]\([^)]+\)/g, ' ')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[*_~>#-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export function useLessonSidebarCopilotActions(options: {
  courseId: number;
  lessonChapterId: number;
  meta: LessonMetaForm;
  mainMarkdown: MaybeRef<string>;
  objectiveBody: MaybeRef<string>;
  knowledgePoints: MaybeRef<KnowledgePoint[]>;
  contentStatus?: MaybeRef<string | undefined>;
  wordCount?: MaybeRef<number>;
  onPatchDescription: (value: string) => void;
}) {
  const teachingStore = useTeachingCopilotStore();

  function bodyExcerpt(max = 2000): string {
    return (unref(options.mainMarkdown) || '').trim().slice(0, max);
  }

  function buildStudioContext(): TeachingCopilotContext {
    const excerpt = bodyExcerpt(1500);
    return {
      contextModule: 'lesson_studio',
      courseId: options.courseId,
      lessonChapterId: options.lessonChapterId,
      title: options.meta.title,
      description: options.meta.description,
      lessonType: options.meta.lessonType,
      wordCount: unref(options.wordCount),
      contentStatus: unref(options.contentStatus),
      draftExcerpt: excerpt,
      objectiveExcerpt: (unref(options.objectiveBody) || '').slice(0, 600)
    };
  }

  function requireTitle(focusHint = '请先填写课节标题'): boolean {
    const title = options.meta.title?.trim();
    if (title) return true;
    ElMessage.warning(focusHint);
    return false;
  }

  function openCopilotPrompt(prompt: string, intent: LessonInsertIntent) {
    teachingStore.openAssistantWithContext(
      {
        ...buildStudioContext(),
        lessonInsertIntent: intent
      },
      prompt,
      { autoSend: true }
    );
  }

  function aiExtractDescription() {
    const title = options.meta.title?.trim();
    if (!title) {
      requireTitle('请先填写课节标题，导读将主要依据课节名称生成');
      return;
    }
    const excerpt = bodyExcerpt();
    const lessonType = options.meta.lessonType || 'LECTURE';
    const typeLabel =
      lessonType === 'PRACTICE' ? '实战演练' : lessonType === 'QUIZ' ? '智能自测' : '讲义精讲';
    const existingDesc = options.meta.description?.trim();
    const objective = unref(options.objectiveBody)?.trim();

    let prompt = `请根据课节标题《${title}》撰写一段 80~120 字的课节导读（用于课程大纲卡片与学习页展示）。
要求：突出本课核心主题与学习价值，语气专业简洁，直接输出纯导读段落，不要加「导读：」等前缀，不要客套话。
课节类型：${typeLabel}。`;

    if (existingDesc) {
      prompt += `\n当前已有导读（可在此基础上优化）：${existingDesc}`;
    }
    if (objective) {
      prompt += `\n学习目标参考：${objective.slice(0, 200)}`;
    }
    if (excerpt) {
      prompt += `\n\n若需与正文呼应，可参考以下正文摘录（非必须）：\n${excerpt.slice(0, 800)}`;
    }

    openCopilotPrompt(prompt, 'description');
  }

  function aiRecommendKnowledgePoints() {
    const title = options.meta.title?.trim();
    const excerpt = bodyExcerpt();
    if (!title && !excerpt) {
      ElMessage.warning('请先填写课节标题或正文，AI 才能推荐考查知识点');
      return;
    }
    const kpNames = (unref(options.knowledgePoints) || [])
      .map(kp => kp.title || kp.name)
      .filter(Boolean)
      .join('、');
    openCopilotPrompt(
      `请根据课节《${title || '暂无标题'}》与以下正文，从本课程已有考点【${kpNames || '课程知识图谱中的考点'}】中挑选 3~5 个最匹配的考查知识点。请在文末单独一行以【推荐考点】：考点1, 考点2 格式列出，并在前文简要说明推荐理由：\n\n${excerpt || '（当前主要依据标题进行归纳）'}`,
      'knowledge'
    );
  }

  function aiExtractObjective() {
    const title = options.meta.title?.trim();
    if (!title) {
      requireTitle('请先填写课节标题，学习目标将主要依据课节名称生成');
      return;
    }
    const excerpt = bodyExcerpt();
    const lessonType = options.meta.lessonType || 'LECTURE';
    const typeLabel =
      lessonType === 'PRACTICE' ? '实战演练' : lessonType === 'QUIZ' ? '智能自测' : '讲义精讲';
    const description = options.meta.description?.trim();
    const existingObjective = unref(options.objectiveBody)?.trim();

    let prompt = `请根据课节标题《${title}》撰写 3~5 条可测量的学习目标（适合填入课节「学习目标」卡，使用 Markdown 无序列表）。
要求：动词可观测（如「理解」「辨析」「能够…」），与标题主题一致，直接输出列表正文，不要加「学习目标：」等前缀，不要客套话。
课节类型：${typeLabel}。`;

    if (description) {
      prompt += `\n课节导读参考：${description}`;
    }
    if (existingObjective && !isDefaultObjectivePlaceholder(existingObjective)) {
      prompt += `\n当前已有目标（可优化）：${existingObjective.slice(0, 300)}`;
    }
    if (excerpt) {
      prompt += `\n\n若需与正文呼应，可参考以下摘录（非必须）：\n${excerpt.slice(0, 800)}`;
    }
    prompt += '\n\n请勿输出【推荐考点】或知识点关联建议，只输出学习目标无序列表。';

    openCopilotPrompt(prompt, 'objective');
  }

  function aiGenerateLessonBody() {
    const title = options.meta.title?.trim();
    if (!title) {
      requireTitle('请先填写课节标题，再一键生成课节正文');
      return;
    }
    const excerpt = bodyExcerpt(2000);
    const lessonType = options.meta.lessonType || 'LECTURE';
    const typeLabel =
      lessonType === 'PRACTICE' ? '实战演练' : lessonType === 'QUIZ' ? '智能自测' : '讲义精讲';
    const description = options.meta.description?.trim();
    const objective = unref(options.objectiveBody)?.trim();

    let prompt = `请为课节《${title}》撰写完整教学正文（Markdown），可直接插入课节编辑器。
要求：
1. 使用 ## / ### 组织小节，含概念讲解、示例与课堂小结；
2. 语气专业、适合「${typeLabel}」课型；
3. 输出完整讲解正文，不要只列学习目标清单，不要单独输出【推荐考点】；
4. 所有程序示例必须用 Markdown 围栏代码块（\`\`\`java 独占一行开头、\`\`\` 独占一行结尾），不要用反引号包裹整段代码；
5. 正文里文件名、命令可直接写 Hello.java、javac，不要加 \` 反引号；
6. 不要输出 JSON、不要客套开场白。`;

    if (description) {
      prompt += `\n\n课节导读：${description}`;
    }
    if (objective && !isDefaultObjectivePlaceholder(objective)) {
      prompt += `\n\n已有学习目标（可呼应或优化）：\n${objective.slice(0, 400)}`;
    }
    if (excerpt) {
      prompt += `\n\n请在以下已有正文基础上扩写或重构（保留有价值段落）：\n${excerpt}`;
    } else {
      prompt += '\n\n当前正文为空，请从零生成初稿。';
    }

    openCopilotPrompt(prompt, 'editor');
  }

  function aiPolishTitle() {
    const title = options.meta.title?.trim();
    const excerpt = bodyExcerpt(1200);
    if (!title && !excerpt) {
      ElMessage.warning('请先填写标题或正文后再优化标题');
      return;
    }
    openCopilotPrompt(
      `请为以下课节拟定或优化一个简洁、准确的课节标题（不超过 40 字），只输出标题本身：\n\n当前标题：${title || '（未命名）'}\n\n正文摘录：\n${excerpt || '（暂无正文）'}`,
      'title'
    );
  }

  function rollbackDescription() {
    window.dispatchEvent(new CustomEvent('rollback-lesson-description'));
  }

  function rollbackObjective() {
    window.dispatchEvent(new CustomEvent('rollback-lesson-objective'));
  }

  function rollbackKnowledge() {
    window.dispatchEvent(new CustomEvent('rollback-lesson-knowledge-suggestions'));
  }

  return {
    aiExtractDescription,
    aiRecommendKnowledgePoints,
    aiExtractObjective,
    aiGenerateLessonBody,
    aiPolishTitle,
    rollbackDescription,
    rollbackObjective,
    rollbackKnowledge
  };
}
