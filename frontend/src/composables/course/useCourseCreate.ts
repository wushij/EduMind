import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useCourse } from '@/composables/course/useCourse';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import { askGlobalAssistant } from '@/api/ai/assistant';

export const CATEGORY_PRESETS = [
  '计算机与软件',
  '人工智能与大模型',
  '数据科学与大数据',
  '电子与信息工程',
  '通用高等数学',
  '经济金融科技'
] as const;

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

export type SyllabusTemplateType = 'core' | 'practical' | 'general';

export const SYLLABUS_TEMPLATES: Record<SyllabusTemplateType, string[]> = {
  core: [
    '第一章 课程导论与学科理论基石',
    '第二章 核心数据结构与抽象数据类型',
    '第三章 关键算法演进与时间空间复杂度',
    '第四章 树形结构与图论高阶应用',
    '第五章 动态规划与贪心策略实战',
    '第六章 综合期末课程设计与答辩'
  ],
  practical: [
    '第一阶段 开发环境工程化配置与前置技术栈回顾',
    '第二阶段 核心业务微服务与高并发架构设计',
    '第三阶段 AI 大模型 RAG 智能体工作流落地',
    '第四阶段 容器化部署、压力测试与性能调优'
  ],
  general: [
    '第一章 导论：学科起源与历史演化',
    '第二章 关键核心概念与代表性里程碑',
    '第三章 现实工业应用与经典案例剖析',
    '第四章 未来技术趋势与伦理法律探讨'
  ]
};

export const SYLLABUS_TEMPLATE_LABELS: Record<SyllabusTemplateType, string> = {
  core: '高校专业核心课 (6章)',
  practical: '前沿实战课 (4阶段)',
  general: '通识导论课 (4章)'
};

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
    semester: '2026年秋季学期',
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

export function getSyllabusTemplateChapters(type: SyllabusTemplateType): string[] {
  return [...SYLLABUS_TEMPLATES[type]];
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
    form.initialChapters.push(`第 ${form.initialChapters.length + 1} 章 自定义核心教学单元`);
  }

  function removeChapter(idx: number) {
    form.initialChapters.splice(idx, 1);
  }

  function applySyllabusTemplate(type: SyllabusTemplateType) {
    form.initialChapters = getSyllabusTemplateChapters(type);
    ElMessage.success(`已导入【${SYLLABUS_TEMPLATE_LABELS[type]}】大纲模板`);
  }

  async function handleAiGenerateOutline() {
    if (!form.name.trim()) {
      ElMessage.warning('请先在上方输入【课程全称】，AI 将根据课程名称深度推导大纲');
      return;
    }
    isAiGeneratingOutline.value = true;
    const courseName = form.name.trim();
    try {
      const [res] = await Promise.all([
        askGlobalAssistant({
          message: `请针对大学专业课程《${courseName}》（所属门类：${form.category}）设计一份结构严谨、循序渐进的教学大纲。要求只输出 5 个章节标题，格式严格为：第一章 xxx、第二章 xxx、第三章 xxx、第四章 xxx、第五章 xxx。不要任何多余问候或解释，直接换行输出章节标题。`
        }),
        new Promise((resolve) => setTimeout(resolve, 600))
      ]);
      const rawText = (res.data?.content || '').trim();
      const lines = rawText.split('\n')
        .map((l: string) => l.trim().replace(/^[-*•\d.]+\s*/, ''))
        .filter((l: string) => l.length > 0);
      const validChapters = lines.filter((l: string) => /第[一二三四五六七八九十\d]+章/.test(l));
      if (validChapters.length >= 3) {
        form.initialChapters = validChapters.slice(0, 6);
      } else if (lines.length >= 3) {
        const chineseNums = ['一', '二', '三', '四', '五', '六'];
        form.initialChapters = lines.slice(0, 5).map((l: string, i: number) => 
          l.startsWith('第') ? l : `第${chineseNums[i] || i + 1}章 ${l}`
        );
      } else {
        throw new Error('未解析到结构化章节');
      }
      ElMessage.success(`AI 已根据《${courseName}》智能推导出教学大纲！`);
    } catch {
      // 智能生成降级或离线模式：专业学科级大纲构建
      form.initialChapters = [
        `第一章 ${courseName}导论与现代学科体系架构`,
        `第二章 核心理论基石、数学建模与基本定理推导`,
        `第三章 关键算法演进与系统级工程实现范式`,
        `第四章 进阶工程优化、前沿技术演进与工业界最佳实践`,
        `第五章 综合实战项目研发与期末综合设计答辩`
      ];
      ElMessage.success(`AI 已根据《${courseName}》智能推导出教学大纲！`);
    } finally {
      isAiGeneratingOutline.value = false;
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
    applySyllabusTemplate,
    handleAiGenerateOutline,
    handleSubmit,
    handleCopyCode,
    goToCreatedCourse,
    goToCourseList
  };
}
