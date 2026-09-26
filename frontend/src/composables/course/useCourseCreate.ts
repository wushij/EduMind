import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { COURSE_CATEGORY_PRESETS } from '@/constants/course';
import { getCurrentSemester } from '@/constants/semester';
import { useCourse } from '@/composables/course/useCourse';
import { useAiThinkingTimer } from '@/composables/ai/useAiThinkingTimer';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import { askGlobalAssistant } from '@/api/ai/assistant';

/**
 * 健壮解析大模型生成的大纲文本
 * 兼顾：
 * 1. 连续无换行输出：如 "第一章 Python语言基础第二章 数据结构..."
 * 2. 标准换行输出：每行一章
 * 3. 序号带前缀输出：如 "1. 第一章 ..." 或 "1. Python语言基础"
 */
export function parseAiOutlineChapters(rawText: string): string[] {
  if (!rawText || !rawText.trim()) return [];
  const text = rawText.trim();

  // 1. 优先使用正则提取所有以 "第X章" 开头的片段（完美兼容连写无换行）
  const chapterRegex = /(第[一二三四五六七八九十\d]+章[\s:：、]*[^第\n]+)/g;
  const regexMatches = [...text.matchAll(chapterRegex)]
    .map(m => m[1].trim())
    .filter(c => c.length > 2);

  if (regexMatches.length >= 3) {
    return regexMatches.slice(0, 8).map(formatChapterTitle);
  }

  // 2. 如果未匹配到足够多的 "第X章"，按换行拆分
  const lines = text
    .split('\n')
    .map(line => line.trim().replace(/^[-*•\d.]+\s*/, ''))
    .filter(line => line.length > 0 && !line.startsWith('#') && !line.includes('教学大纲') && !line.includes('课程大纲'));

  if (lines.length >= 3) {
    const chineseNums = ['一', '二', '三', '四', '五', '六', '七', '八'];
    return lines.slice(0, 8).map((line, idx) => {
      if (/^第[一二三四五六七八九十\d]+章/.test(line)) {
        return formatChapterTitle(line);
      }
      return `第${chineseNums[idx] || idx + 1}章 ${line}`;
    });
  }

  return [];
}

function formatChapterTitle(raw: string): string {
  return raw
    .replace(/^第[一二三四五六七八九十\d]+章[\s:：、]*/, '')
    .replace(/^(chapter\s*\d+)[\s:：、]*/i, '')
    .replace(/^[-*•\d.]+\s*/, '')
    .trim();
}

export const CATEGORY_PRESETS = COURSE_CATEGORY_PRESETS;

export const COURSE_CODE_LETTERS = ['CS', 'AI', 'SE', 'DATA', 'EE', 'MATH'] as const;

export const PRESET_COVERS = [
  { id: 1, name: '计算科技蓝', gradientClass: 'grad-blue' },
  { id: 2, name: '数学几何紫', gradientClass: 'grad-purple' },
  { id: 3, name: '智能仿生青', gradientClass: 'grad-cyan' },
  { id: 4, name: '工程系统靛', gradientClass: 'grad-indigo' },
  { id: 5, name: '琥珀明光橙', gradientClass: 'grad-amber' }
] as const;

export const AI_PERSONAS = [
  {
    id: 'socrates',
    name: '苏格拉底启发型',
    desc: '循循善诱，善于反问引导学生自主领悟核心原理与本质',
    icon: 'Opportunity'
  },
  {
    id: 'academic',
    name: '严谨学术推导型',
    desc: '注重数学严密性、定理证明、标准学术规范与前沿学术论文',
    icon: 'Reading'
  },
  {
    id: 'engineer',
    name: '工程实战导师型',
    desc: '工业界编码思维，重在落地实战、Bug 排查与高可用架构',
    icon: 'Tools'
  }
] as const;

export interface CourseCreateFormState {
  name: string;
  code: string;
  semester: string;
  category: string;
  credits: number;
  plannedHours: number;
  description: string;
  coverUrl: string;
  initialChapters: string[];
  knowledgeBaseId: number | undefined;
  aiPersona: string;
  welcomeMessage: string;
}

export function createDefaultFormState(): CourseCreateFormState {
  return {
    name: '',
    code: 'AI2026-CS01',
    semester: getCurrentSemester(),
    category: '人工智能与大模型',
    credits: 3.0,
    plannedHours: 48,
    description: '',
    coverUrl: '',
    initialChapters: [],
    knowledgeBaseId: undefined,
    aiPersona: 'socrates',
    welcomeMessage:
      '同学你好！我是本课程的专属 AI 助教。在接下来的学期中，我将陪伴你完成所有章节大纲的学习、重难点考点答疑以及随堂编程实战练习！'
  };
}

export function buildRandomCourseCode(randomValue = Math.random()): string {
  const randLetter = COURSE_CODE_LETTERS[Math.floor(randomValue * COURSE_CODE_LETTERS.length)];
  const randNum = Math.floor(1000 + randomValue * 9000);
  return `${randLetter}2026-${randNum}`;
}

export function getPersonaWelcomeMessage(
  personaId: (typeof AI_PERSONAS)[number]['id'],
  courseName: string
): string {
  const name = courseName || '本课程';
  if (personaId === 'socrates') {
    return `同学你好！我是《${name}》苏格拉底启发式 AI 助教。学习不是被动接受，让我们通过层层深入的提问，共同探索知识的底层逻辑！`;
  }
  if (personaId === 'academic') {
    return `同学你好！我是《${name}》学术助教。我将为你提供严谨的概念辨析、定理推导与学术论文溯源。`;
  }
  return `嗨！我是《${name}》实战导师。代码是运行出来的，遇到任何运行报错或架构疑惑，随时把代码发给我！`;
}

export function useCourseCreate() {
  const router = useRouter();
  const { createCourse } = useCourse();

  const formRef = ref<FormInstance>();
  const submitting = ref(false);
  const isAiGeneratingOutline = ref(false);
  const selectedCoverId = ref(1);
  const kbMode = ref<'new' | 'existing'>('new');
  const enableRagAutoIndex = ref(true);
  const successModalVisible = ref(false);
  const createdCourse = ref<{ id: number; name: string } | null>(null);
  const createdCourseChaptersCount = ref(0);

  const form = reactive(createDefaultFormState());

  const rules = reactive<FormRules>({
    name: [{ required: true, message: '请输入课程全称', trigger: 'blur' }],
    code: [{ required: true, message: '请输入课程代码 / 邀请码', trigger: 'blur' }],
    semester: [{ required: true, message: '请选择开课学期', trigger: 'change' }]
  });

  const availableKnowledgeBases = ref<Array<{ id: number; name: string; documentCount?: number }>>([
    { id: 1, name: '数据结构与算法专业知识库', documentCount: 2 },
    { id: 2, name: 'Java面向对象程序设计知识库', documentCount: 1 },
    { id: 3, name: '人工智能与深度学习系统理论库', documentCount: 4 }
  ]);

  const currentCoverClass = computed(() => {
    const found = PRESET_COVERS.find(p => p.id === selectedCoverId.value);
    return found ? found.gradientClass : 'grad-blue';
  });

  const kbLabel = computed(() => {
    if (kbMode.value === 'new') {
      return '专属 RAG 知识空间 (自动新建)';
    }
    const found = availableKnowledgeBases.value.find(k => k.id === form.knowledgeBaseId);
    return found ? found.name : '已关联专业知识库';
  });

  const currentPersonaName = computed(() => {
    const found = AI_PERSONAS.find(p => p.id === form.aiPersona);
    return found ? found.name : '苏格拉底启发型';
  });

  function selectPresetCover(preset: (typeof PRESET_COVERS)[number]) {
    selectedCoverId.value = preset.id;
    form.coverUrl = '';
  }

  function generateRandomCourseCode() {
    form.code = buildRandomCourseCode();
    ElMessage.success(`已生成新课程邀请码：${form.code}`);
  }

  function selectPersona(persona: (typeof AI_PERSONAS)[number]) {
    form.aiPersona = persona.id;
    form.welcomeMessage = getPersonaWelcomeMessage(persona.id, form.name);
  }

  function addChapter() {
    form.initialChapters.push(`核心教学单元 ${form.initialChapters.length + 1}`);
  }

  async function removeChapter(idx: number) {
    const raw = form.initialChapters[idx]?.trim();
    const label = raw || `第 ${idx + 1} 章`;
    const preview = label.length > 36 ? `${label.slice(0, 36)}…` : label;
    try {
      await ElMessageBox.confirm(
        `确定移除大纲章节「${preview}」吗？移除后仍可手动添加或使用 AI 重新生成。`,
        '移除章节确认',
        {
          confirmButtonText: '移除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      form.initialChapters.splice(idx, 1);
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
    }
  }

  const { elapsedTimeText: aiThinkingElapsedTime } = useAiThinkingTimer(isAiGeneratingOutline);
  let aiOutlineAbortController: AbortController | null = null;

  function cancelAiGenerateOutline() {
    if (aiOutlineAbortController) {
      aiOutlineAbortController.abort();
      aiOutlineAbortController = null;
    }
    isAiGeneratingOutline.value = false;
    ElMessage.info('已中止大纲推演');
  }

  async function handleAiGenerateOutline() {
    if (!form.name.trim()) {
      ElMessage.warning('请先在上方输入【课程全称】，AI 将根据课程名称深度推导大纲');
      return;
    }
    isAiGeneratingOutline.value = true;
    aiOutlineAbortController = new AbortController();
    const courseName = form.name.trim();

    try {
      const res = await askGlobalAssistant(
        {
          message: `请针对大学专业课程《${courseName}》（所属门类：${form.category}）制定 5 个核心章节的大纲目录。要求严格只输出 5 行，每一章占独立一行，格式严格为：\n第一章 xxx\n第二章 xxx\n第三章 xxx\n第四章 xxx\n第五章 xxx\n不要任何多余问候、前后缀说明或总结，直接换行输出章节标题。`
        },
        { signal: aiOutlineAbortController.signal }
      );

      const rawText = (res.data?.content || '').trim();
      const parsedChapters = parseAiOutlineChapters(rawText);

      if (parsedChapters.length >= 3) {
        form.initialChapters = parsedChapters;
        ElMessage.success(`AI 已根据《${courseName}》智能推导出 ${parsedChapters.length} 个教学章节！`);
      } else {
        throw new Error(rawText ? '大模型输出格式未匹配到有效章节目录' : '大模型未返回有效内容');
      }
    } catch (err: any) {
      if (err?.name === 'CanceledError' || err?.code === 'ERR_CANCELED') {
        return;
      }
      console.warn('[CourseCreate] AI 大纲生成异常或解析回落:', err);
      // 智能生成降级或离线模式：专业学科级大纲构建（去除重复的第X章）
      form.initialChapters = [
        `${courseName}导论与现代学科体系架构`,
        `核心理论基石、数学建模与基本定理推导`,
        `关键算法演进与系统级工程实现范式`,
        `进阶工程优化、前沿技术演进与工业界最佳实践`,
        `综合实战项目研发与期末综合设计答辩`
      ];
      ElMessage.warning(`大模型在线推演受阻（${err?.message || '接口超时或格式不符'}），已载入专业学科推荐大纲`);
    } finally {
      isAiGeneratingOutline.value = false;
      aiOutlineAbortController = null;
    }
  }

  async function handleSubmit() {
    if (!formRef.value) return;
    await formRef.value.validate(async (valid) => {
      if (!valid) return;

      submitting.value = true;
      try {
        const payload = {
          name: form.name.trim(),
          code: form.code.trim(),
          semester: form.semester,
          category: form.category,
          credits: form.credits,
          plannedHours: form.plannedHours,
          description: form.description.trim(),
          coverUrl: form.coverUrl.trim(),
          initialChapters: form.initialChapters.filter(c => c && c.trim()),
          knowledgeBaseId: kbMode.value === 'existing' ? form.knowledgeBaseId : undefined,
          aiPersona: form.aiPersona,
          welcomeMessage: form.welcomeMessage
        };

        const result = await createCourse(payload);
        if (!result) {
          ElMessage.error('创建失败，请重试');
          return;
        }

        // 后端 CourseServiceImpl.createCourse 已原子事务插入初始章节，无需前端重复循环插入

        createdCourse.value = {
          id: result.id,
          name: result.title || form.name
        };
        createdCourseChaptersCount.value = payload.initialChapters.length;
        successModalVisible.value = true;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : '创建失败，请检查网络';
        ElMessage.error(msg);
      } finally {
        submitting.value = false;
      }
    });
  }

  function handleCopyCode() {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(form.code);
      ElMessage.success('课程邀请码已复制到剪贴板！');
    } else {
      ElMessage.info(`邀请码：${form.code}`);
    }
  }

  function goToCreatedCourse(tab: 'overview' | 'chapters') {
    if (!createdCourse.value) return;
    router.push(`/course/${createdCourse.value.id}/${tab}`);
  }

  function goToCourseList() {
    router.push('/course');
  }

  onMounted(async () => {
    try {
      const res = await getKnowledgeBases();
      if (res.data && res.data.length > 0) {
        availableKnowledgeBases.value = res.data.map(kb => ({
          id: kb.id,
          name: kb.name,
          documentCount: (kb as { documentCount?: number }).documentCount ?? 2
        }));
        if (availableKnowledgeBases.value.length > 0) {
          form.knowledgeBaseId = availableKnowledgeBases.value[0].id;
        }
      }
    } catch {
      // 保持优雅的默认知识库列表
    }
  });

  return {
    router,
    formRef,
    form,
    rules,
    submitting,
    isAiGeneratingOutline,
    selectedCoverId,
    kbMode,
    enableRagAutoIndex,
    successModalVisible,
    createdCourse,
    createdCourseChaptersCount,
    categoryPresets: CATEGORY_PRESETS,
    presetCovers: PRESET_COVERS,
    aiPersonas: AI_PERSONAS,
    availableKnowledgeBases,
    currentCoverClass,
    kbLabel,
    currentPersonaName,
    selectPresetCover,
    generateRandomCourseCode,
    selectPersona,
    addChapter,
    removeChapter,
    handleAiGenerateOutline,
    cancelAiGenerateOutline,
    aiThinkingElapsedTime,
    handleSubmit,
    handleCopyCode,
    goToCreatedCourse,
    goToCourseList
  };
}
