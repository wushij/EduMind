import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePrompt } from '@/composables/system/usePrompt';
import { PromptTemplate } from '@/types/system/prompt';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deletePromptTemplate } from '@/api/system/prompt';
import { resolveModels } from '@/composables/system/useAIModel';
import {
  applyPromptDemoVariables,
  resolvePromptDemoFill
} from '@/composables/system/usePromptDemoData';
import { AIModelConfigItem } from '@/types/system/model';

export const categoryOptions = [
  { key: 'ALL', label: '全部模板' },
  { key: 'rag', label: '课程问答 RAG' },
  { key: 'question', label: '智能命题' },
  { key: 'grading', label: '智能批改' },
  { key: 'teaching', label: '教案备课' }
];

export function getCategoryLabel(category: string) {
  switch (category) {
    case 'rag':
      return '课程问答 RAG';
    case 'question':
      return '智能命题';
    case 'grading':
      return '智能批改';
    case 'teaching':
      return '教案备课';
    case 'agent':
      return 'Agent 编排';
    default:
      return '通用';
  }
}

export function getCategoryCount(promptList: PromptTemplate[], catKey: string) {
  if (catKey === 'ALL') return promptList.length;
  return promptList.filter((p) => p.category === catKey).length;
}

export function filterPromptList(
  promptList: PromptTemplate[],
  selectedCategory: string,
  selectedStatus: string,
  searchKeyword: string
) {
  let list = promptList;

  if (selectedCategory && selectedCategory !== 'ALL') {
    list = list.filter((p) => p.category === selectedCategory);
  }

  if (selectedStatus) {
    list = list.filter((p) => p.status === selectedStatus);
  }

  if (searchKeyword.trim()) {
    const kw = searchKeyword.toLowerCase().trim();
    list = list.filter((p) => {
      const matchName = p.name.toLowerCase().includes(kw);
      const matchCode = p.code.toLowerCase().includes(kw);
      const matchDesc = p.description?.toLowerCase().includes(kw);
      const matchVar = p.variables?.some((v) => v.name.toLowerCase().includes(kw));
      return matchName || matchCode || matchDesc || matchVar;
    });
  }

  return list;
}

export function isBoundModelUnset(boundModel?: string | null) {
  return !boundModel || boundModel.trim() === '' || boundModel === 'deepseek-chat';
}

export function getDisplayModelName(
  boundModel?: string | null,
  availableModels: AIModelConfigItem[] = [],
  defaultModelName = ''
) {
  if (!boundModel || boundModel.trim() === '' || boundModel === 'deepseek-chat') {
    return defaultModelName ? `默认: ${defaultModelName}` : '系统默认网关';
  }
  const matched = availableModels.find(
    (m) => m.name === boundModel || m.modelName === boundModel || (m as any).modelKey === boundModel
  );
  if (matched) {
    return matched.name || matched.modelName;
  }
  return boundModel;
}

export function getDisplayModelTooltip(
  boundModel?: string | null,
  availableModels: AIModelConfigItem[] = [],
  defaultModelName = ''
) {
  if (!boundModel || boundModel.trim() === '' || boundModel === 'deepseek-chat') {
    return defaultModelName
      ? `未绑定独立模型，运行时自适应调度全平台默认模型：${defaultModelName}`
      : '未绑定独立模型，运行时跟随系统默认网关模型';
  }
  const matched = availableModels.find(
    (m) => m.name === boundModel || m.modelName === boundModel || (m as any).modelKey === boundModel
  );
  if (matched) {
    return `当前绑定专用模型：${matched.name} (${matched.modelName}) · ${matched.provider.toUpperCase()}`;
  }
  return `当前绑定模型：${boundModel}`;
}

export function computePublishedCount(promptList: PromptTemplate[]) {
  return promptList.filter((p) => p.status === 'PUBLISHED').length;
}

export function computeRagCount(promptList: PromptTemplate[]) {
  return promptList.filter((p) => p.category === 'rag').length;
}

export function computeTotalVariablesCount(promptList: PromptTemplate[]) {
  return promptList.reduce((acc, p) => acc + (p.variables?.length || 0), 0);
}

export function buildFallbackTestOutput(
  activeItem: PromptTemplate,
  testVariables: Record<string, string>
): string {
  if (activeItem.category === 'grading' || activeItem.code === 'GRADING_RAG_GENERAL') {
    return `{
  "status": "SUCCESS",
  "questionId": "${testVariables.question_id || '1007'}",
  "questionType": "${testVariables.question_type || 'SINGLE_CHOICE'}",
  "maxScore": ${Number(testVariables.max_score) || 5},
  "totalScore": 0,
  "scoreRate": 0,
  "gradingPoints": [
    {
      "scoringPointId": "SP1",
      "description": "指出 ArrayList 底层为动态数组，随机访问时间复杂度 O(1)",
      "maxScore": 2,
      "awardedScore": 0,
      "status": "NONE",
      "evidence": null,
      "reason": "学生作答为 C，未命中该采分点。"
    },
    {
      "scoringPointId": "SP2",
      "description": "指出 LinkedList 按下标访问需要遍历，时间复杂度 O(n)",
      "maxScore": 2,
      "awardedScore": 0,
      "status": "NONE",
      "evidence": null,
      "reason": "作答未涉及链表下标访问的遍历代价。"
    },
    {
      "scoringPointId": "SP3",
      "description": "指出 LinkedList 每个节点额外持有前后指针，内存开销更大",
      "maxScore": 1,
      "awardedScore": 0,
      "status": "NONE",
      "evidence": null,
      "reason": "作答未涉及链表节点的额外指针开销。"
    }
  ],
  "knowledgeDiagnosis": [
    {
      "knowledgePointId": "16",
      "knowledgePointName": "ArrayList 与 LinkedList 源码剖析",
      "mastery": "WEAK",
      "reason": "把“动态数组扩容需要复制元素”误迁移成“ArrayList 插入永远不需要复制数组”，说明尚未把扩容机制与下标随机访问两套机制区分开。"
    }
  ],
  "overallFeedback": "本题正确选项为 A。ArrayList 的 O(1) 随机访问来自连续内存 + 下标寻址，LinkedList 的 O(n) 查找与节点额外指针开销才是与 B、D 的分界；C 描述的是扩容代价，属于另一条考点，不能用来否定 A。",
  "sourceIds": ["S1"],
  "confidence": 0.93,
  "requiresManualReview": false,
  "manualReviewReason": null
}`;
  }

  if (activeItem.category === 'question' || activeItem.code === 'EXAM_RAG_GENERAL') {
    return `{
  "status": "SUCCESS",
  "courseId": "${testVariables.course_id || '102'}",
  "courseName": "${testVariables.course_name || 'Java面向对象程序设计'}",
  "chapterId": "${testVariables.chapter_id || '10'}",
  "chapterName": "${testVariables.chapter_name || '2.1 封装、继承与多态机制'}",
  "questions": [
    {
      "tempId": "Q1",
      "type": "SINGLE_CHOICE",
      "difficulty": "MEDIUM",
      "stem": "关于 Java 运行时多态，下列说法正确的是？",
      "options": [
        { "key": "A", "content": "父类引用只能指向父类对象" },
        { "key": "B", "content": "父类引用可以指向子类对象，并根据实际对象调用重写的方法" },
        { "key": "C", "content": "多态只发生在方法重载中" },
        { "key": "D", "content": "多态要求父类和子类方法名称必须不同" }
      ],
      "answer": ["B"],
      "acceptableAnswers": [],
      "explanation": "Java 运行时多态允许父类引用指向子类对象，当调用被重写的方法时，实际执行的方法由运行时对象类型决定。[S1]",
      "scoringPoints": [],
      "knowledgePoints": [{ "id": "15", "name": "面向对象三大特征与多态运行时绑定" }],
      "sourceIds": ["S1"],
      "suggestedScore": 2
    },
    {
      "tempId": "Q2",
      "type": "MULTIPLE_CHOICE",
      "difficulty": "MEDIUM",
      "stem": "关于 ArrayList 与 LinkedList 的结构与复杂度，下列说法正确的有？",
      "options": [
        { "key": "A", "content": "ArrayList 底层是 Object[] 数组，按下标访问为 O(1)" },
        { "key": "B", "content": "LinkedList 底层是双向链表，按下标访问需要从头遍历" },
        { "key": "C", "content": "ArrayList 扩容时会创建新数组并复制原有元素" },
        { "key": "D", "content": "LinkedList 的每个节点额外持有前后指针，内存开销比 ArrayList 更大" }
      ],
      "answer": ["A", "B", "C", "D"],
      "acceptableAnswers": [],
      "explanation": "四项描述均与源码结构一致：ArrayList 使用连续数组并按下标寻址，扩容时复制元素；LinkedList 使用双向链表，访问需遍历且节点额外持有 prev/next 指针。[S1]",
      "scoringPoints": [],
      "knowledgePoints": [{ "id": "16", "name": "ArrayList 与 LinkedList 源码剖析" }],
      "sourceIds": ["S1"],
      "suggestedScore": 3
    }
  ]
}`;
  }

  if (activeItem.category === 'teaching' || activeItem.code === 'LESSON_PREP_RAG_GENERAL') {
    return `{
  "status": "SUCCESS",
  "groundingStatus": "FULL",
  "requiresTeacherReview": false,
  "reviewReason": null,
  "lessonPlan": {
    "basicInfo": {
      "courseId": "${testVariables.course_id || '102'}",
      "courseName": "${testVariables.course_name || 'Java面向对象程序设计'}",
      "chapterId": "${testVariables.chapter_id || '10'}",
      "chapterName": "${testVariables.chapter_name || '2.1 封装、继承与多态机制'}",
      "lessonTitle": "${testVariables.lesson_title || '面向对象三大特征与多态运行时绑定'}",
      "lessonCount": 2,
      "totalDurationMinutes": 90,
      "studentLevel": "计算机专业大二学生"
    },
    "learningAnalysis": {
      "priorKnowledge": ["类与对象", "继承", "方法重写"],
      "learningCharacteristics": "学生已经能够阅读基础Java代码，但对声明类型和实际对象类型之间的关系理解较弱。",
      "possibleDifficulties": ["混淆方法重载与方法重写", "混淆编译时类型与运行时类型"]
    },
    "objectives": [
      {
        "type": "KNOWLEDGE",
        "content": "能够解释Java运行时多态的基本概念及实现条件。",
        "sourceIds": ["S1", "S2"]
      },
      {
        "type": "ABILITY",
        "content": "能够分析简单多态程序并判断实际调用的方法。",
        "sourceIds": ["S1"]
      },
      {
        "type": "LITERACY",
        "content": "形成基于抽象进行程序设计的初步意识。",
        "sourceIds": []
      }
    ],
    "keyPoints": [
      {
        "content": "父类引用指向子类对象",
        "reason": "是理解Java运行时多态的基础。",
        "sourceIds": ["S2"]
      },
      {
        "content": "运行时动态绑定",
        "reason": "决定实际执行的方法。",
        "sourceIds": ["S2"]
      }
    ],
    "difficultPoints": [
      {
        "content": "声明类型与运行时对象类型之间的关系",
        "breakthroughStrategy": "通过代码预测、运行验证和结果对比逐步建立理解。"
      }
    ],
    "teachingMethods": ["问题驱动", "案例教学", "代码演示", "任务驱动"],
    "resources": ["Java面向对象编程实战教程", "课程PPT", "IDE开发环境", "课堂示例代码"],
    "stages": [
      {
        "stage": "LESSON_INTRODUCTION",
        "name": "情境导入",
        "durationMinutes": 8,
        "teacherActivity": "展示Animal、Dog、Cat三个类，提出'同一个Animal变量为什么能够产生不同输出'的问题。",
        "studentActivity": "观察代码并预测程序运行结果。",
        "teachingContent": "回顾继承和方法重写，引出多态问题。",
        "teachingPurpose": "利用已有知识制造认知冲突，引出新知识。",
        "assessment": "通过学生对代码执行结果的预测判断前置知识掌握情况。",
        "knowledgePoints": ["方法重写", "多态"],
        "sourceIds": ["S2", "S3"]
      },
      {
        "stage": "KNOWLEDGE_EXPLORATION",
        "name": "多态机制探究",
        "durationMinutes": 22,
        "teacherActivity": "逐步分析父类引用、实际对象和方法重写之间的关系，并运行代码验证。",
        "studentActivity": "记录预测结果、运行代码并比较实际结果。",
        "teachingContent": "父类引用、子类对象、方法重写、运行时动态绑定。",
        "teachingPurpose": "建立运行时多态的完整知识模型。",
        "assessment": "随机修改对象实例，让学生判断实际调用的方法。",
        "knowledgePoints": ["多态", "动态绑定"],
        "sourceIds": ["S2"]
      },
      {
        "stage": "PRACTICE",
        "name": "课堂实践",
        "durationMinutes": 25,
        "teacherActivity": "提供两组多态代码任务，引导学生完成分析和修改。",
        "studentActivity": "独立完成代码预测、运行和解释。",
        "teachingContent": "多态代码分析与应用。",
        "teachingPurpose": "从理解提升到应用。",
        "assessment": "检查代码执行结果及学生对原因的解释。",
        "knowledgePoints": ["多态"],
        "sourceIds": ["S2", "S3"]
      }
    ],
    "formativeAssessment": [
      {
        "objective": "判断学生能否区分声明类型和实际对象类型。",
        "method": "代码预测题",
        "successCriteria": "能够正确指出实际执行的重写方法并解释原因。"
      }
    ],
    "summary": {
      "content": [
        "多态建立在继承或接口实现关系基础上",
        "子类重写父类方法",
        "父类引用可以指向子类对象",
        "运行时根据实际对象确定调用方法"
      ]
    },
    "homework": [
      { "type": "PRACTICE", "content": "完成2道多态代码分析题。" },
      { "type": "PROGRAMMING", "content": "使用Animal、Dog、Cat设计一个简单多态示例。" }
    ],
    "boardDesign": "多态 → 继承/实现 → 方法重写 → 父类引用指向子类对象 → 运行时动态绑定",
    "reflectionSuggestions": [
      "重点观察学生是否仍然混淆方法重载与方法重写。",
      "根据课堂代码预测题正确率决定下一课时是否增加动态绑定复习。"
    ],
    "sourceIds": ["S1", "S2", "S3"]
  }
}`;
  }

  return `### 多态的核心原理与实现机制

Java 中的多态可以简单理解为：**同一个父类引用，在运行时可以指向不同的子类对象，并表现出不同的行为。** [S1]

在底层的 JVM 实现中：
1. **编译时静态检查**：编译器仅依据引用的声明类型检查调用的方法是否存在；
2. **运行时动态分派**：JVM 在执行虚方法调用时，通过对象头中的类元数据指针定位具体的类，并在该类的虚方法表（vtable）中检索对应的方法入口地址并跳转执行。[S1][S2]

例如：
\`\`\`java
Animal animal = new Dog();
animal.speak(); // 运行时实际调用 Dog 类中重写的 speak() 方法
\`\`\`

### 教学提示
多态与“方法重载”不同，重载发生在编译阶段（静态多分派），而多态属于运行阶段（动态单分派）。

### 参考资料
[S1] 《Java面向对象编程实战教程》· 2.1 封装、继承与多态机制 · 多态三大必要条件
[S2] 《Java面向对象程序设计知识库》· 面向对象三大特性剖析 · 方法重写`;
}

export function usePromptList() {
  const router = useRouter();
  const selectedCategory = ref('ALL');
  const selectedStatus = ref('');
  const searchKeyword = ref('');

  const { loading, testing, promptList, fetchPrompts, runTest, testResult } = usePrompt();

  const availableModels = ref<AIModelConfigItem[]>([]);
  const defaultModelName = ref<string>('');

  const drawerVisible = ref(false);
  const drawerActiveTab = ref<'preview' | 'test'>('preview');
  const activeItem = ref<PromptTemplate | null>(null);
  const testVariables = ref<Record<string, string>>({});
  const testResultOutput = ref('');
  const testDuration = ref(0);
  /** 一键填入需要实时拉取真实课程数据，用于按钮态与重复点击保护 */
  const demoFilling = ref(false);

  const loadAvailableModels = async () => {
    try {
      const list = await resolveModels();
      availableModels.value = (list || []).filter(
        (m) => (m.configType === 'chat' || !m.configType) && m.status !== 'disabled'
      );
      const def = availableModels.value.find((m) => m.isDefault) || availableModels.value[0];
      if (def) {
        defaultModelName.value = def.name || def.modelName;
      }
    } catch (err) {
      console.warn('获取 AI 模型配置列表失败', err);
    }
  };

  const resolveDisplayModelName = (boundModel?: string | null) =>
    getDisplayModelName(boundModel, availableModels.value, defaultModelName.value);

  const resolveDisplayModelTooltip = (boundModel?: string | null) =>
    getDisplayModelTooltip(boundModel, availableModels.value, defaultModelName.value);

  const publishedCount = computed(() => computePublishedCount(promptList.value));
  const ragCount = computed(() => computeRagCount(promptList.value));
  const totalVariablesCount = computed(() => computeTotalVariablesCount(promptList.value));

  const filteredPrompts = computed(() =>
    filterPromptList(promptList.value, selectedCategory.value, selectedStatus.value, searchKeyword.value)
  );

  const changeCategory = (catKey: string) => {
    selectedCategory.value = catKey;
  };

  const filterPrompts = () => {
    // filteredPrompts 是计算属性，自动生效
  };

  const resetFilters = () => {
    selectedCategory.value = 'ALL';
    selectedStatus.value = '';
    searchKeyword.value = '';
    fetchPrompts();
  };

  const handleReload = () => {
    fetchPrompts();
    ElMessage.success('提示词资产已更新');
  };

  const copyText = (text: string, msg = '已复制到剪贴板') => {
    if (!text) return;
    navigator.clipboard
      .writeText(text)
      .then(() => {
        ElMessage.success(msg);
      })
      .catch(() => {
        ElMessage.info('复制异常，请手动选中复制');
      });
  };

  const openDrawer = (item: PromptTemplate, tab: 'preview' | 'test') => {
    activeItem.value = item;
    drawerActiveTab.value = tab;
    testVariables.value = {};
    testResultOutput.value = '';
    testDuration.value = 0;

    item.variables.forEach((v) => {
      testVariables.value[v.name] = v.defaultValue || '';
    });

    drawerVisible.value = true;
  };

  /** 一键填入真实课程示例：优先读取当前部署的真实课程 / 章节 / 知识点 / 知识库切片 */
  const populateDummyVariables = async () => {
    const item = activeItem.value;
    if (!item || demoFilling.value) return;

    const slotNames = (item.variables || []).map((v) => v.name);
    if (!slotNames.length) {
      ElMessage.warning('当前模板未声明任何参数插槽，无法注入示例数据');
      return;
    }

    demoFilling.value = true;
    try {
      const fill = await resolvePromptDemoFill();
      const filled = applyPromptDemoVariables(testVariables.value, fill.variables, slotNames);
      if (!filled) {
        ElMessage.warning('当前模板插槽与示例数据不匹配，请检查插槽命名');
        return;
      }
      const suffix = fill.source === 'remote' ? '' : '（后端数据不可用，已回落到种子快照）';
      ElMessage.success(`已载入 ${filled} 个真实课程示例：${fill.label}${suffix}`);
    } finally {
      demoFilling.value = false;
    }
  };

  const executePlaygroundTest = async () => {
    if (!activeItem.value) return;
    const startTime = Date.now();
    testResultOutput.value = '';

    const ok = await runTest(activeItem.value.id, {
      systemPrompt: activeItem.value.systemPrompt,
      userPromptTemplate: activeItem.value.userPromptTemplate,
      variables: testVariables.value,
      // 空 boundModel 表示「跟随系统默认模型」，须交给后端自行路由：
      // 传 'deepseek-chat' 这类占位串会被后端当成显式 model_key 去查模型配置，查不到就落到
      // 全局默认配置，全局无 API Key 时直接抛错（表现为 500 系统繁忙），
      // 且与界面上显示的「默认: xxx」完全不符。
      model: activeItem.value.boundModel || defaultModelName.value || '',
      temperature: activeItem.value.temperature ?? 0.3,
      maxTokens: activeItem.value.maxTokens
    });

    testDuration.value = Date.now() - startTime;
    if (!ok) {
      // 失败已由全局拦截器提示，此处不再伪造兜底输出：
      // 否则界面会渲染一段看似正常的样例文本，让人误以为测试通过。
      return;
    }
    if (testResult.value?.output) {
      testResultOutput.value = testResult.value.output;
    }
  };

  const goToEditor = () => {
    router.push('/system/prompts/editor');
  };

  const goToEditorById = (id?: number) => {
    router.push(`/system/prompts/editor/${id}`);
  };

  const handleDeletePrompt = async (item: PromptTemplate) => {
    if (item.status === 'PUBLISHED') {
      ElMessage.warning('已发布的模板不可删除');
      return;
    }
    try {
      await ElMessageBox.confirm(
        `确定删除 Prompt 模板「${item.name}」吗？删除后不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deletePromptTemplate(item.id);
      ElMessage.success('模板已删除');
      await fetchPrompts();
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '删除模板失败');
    }
  };

  const updateTestVariable = (name: string, value: string) => {
    testVariables.value[name] = value;
  };

  onMounted(async () => {
    await Promise.all([fetchPrompts(), loadAvailableModels()]);
  });

  return {
    router,
    loading,
    testing,
    promptList,
    selectedCategory,
    selectedStatus,
    searchKeyword,
    availableModels,
    defaultModelName,
    publishedCount,
    ragCount,
    totalVariablesCount,
    filteredPrompts,
    drawerVisible,
    drawerActiveTab,
    activeItem,
    testVariables,
    testResultOutput,
    testDuration,
    demoFilling,
    categoryOptions,
    getCategoryCount: (catKey: string) => getCategoryCount(promptList.value, catKey),
    getCategoryLabel,
    changeCategory,
    filterPrompts,
    resetFilters,
    handleReload,
    copyText,
    openDrawer,
    populateDummyVariables,
    executePlaygroundTest,
    getDisplayModelName: resolveDisplayModelName,
    getDisplayModelTooltip: resolveDisplayModelTooltip,
    isBoundModelUnset,
    goToEditor,
    goToEditorById,
    updateTestVariable,
    handleDeletePrompt
  };
}
