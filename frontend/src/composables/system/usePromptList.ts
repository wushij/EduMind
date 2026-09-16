import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePrompt } from '@/composables/system/usePrompt';
import { PromptTemplate } from '@/types/system/prompt';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deletePromptTemplate } from '@/api/system/prompt';
import { resolveModels } from '@/composables/system/useAIModel';
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
  "questionId": "${testVariables.question_id || 'Q10001'}",
  "questionType": "${testVariables.question_type || 'SHORT_ANSWER'}",
  "maxScore": 6,
  "totalScore": 5,
  "scoreRate": 0.8333,
  "gradingPoints": [
    {
      "scoringPointId": "SP1",
      "description": "说明继承或父子类型关系",
      "maxScore": 1,
      "awardedScore": 1,
      "status": "FULL",
      "evidence": "父类变量可以保存子类对象",
      "reason": "学生正确表达了父类引用可以指向子类对象的含义。"
    },
    {
      "scoringPointId": "SP2",
      "description": "说明方法重写",
      "maxScore": 2,
      "awardedScore": 2,
      "status": "FULL",
      "evidence": "如果子类重新实现父类的方法",
      "reason": "学生正确说明了子类对父类方法进行重写。"
    },
    {
      "scoringPointId": "SP3",
      "description": "说明运行时动态调用机制",
      "maxScore": 2,
      "awardedScore": 2,
      "status": "FULL",
      "evidence": "调用的时候会执行子类自己的实现",
      "reason": "能够体现运行时根据实际对象执行重写方法的核心含义。"
    },
    {
      "scoringPointId": "SP4",
      "description": "完整描述多态实现机制",
      "maxScore": 1,
      "awardedScore": 0,
      "status": "NONE",
      "evidence": null,
      "reason": "答案没有进一步明确说明动态绑定这一机制。"
    }
  ],
  "knowledgeDiagnosis": [
    {
      "knowledgePointId": "KP003",
      "knowledgePointName": "多态",
      "mastery": "PARTIAL",
      "reason": "已经掌握父类引用、方法重写及运行时调用的核心关系，但概念表述不够完整。"
    }
  ],
  "overallFeedback": "你已经正确理解了多态中父类引用指向子类对象，以及方法重写后的运行时调用机制。建议进一步补充“运行时动态绑定”这一概念，使答案更加完整。",
  "sourceIds": ["S1", "S2"],
  "confidence": 0.94,
  "requiresManualReview": false,
  "manualReviewReason": null
}`;
  }

  if (activeItem.category === 'question' || activeItem.code === 'EXAM_RAG_GENERAL') {
    return `{
  "status": "SUCCESS",
  "courseId": "${testVariables.course_id || '10001'}",
  "courseName": "${testVariables.course_name || 'Java程序设计'}",
  "chapterId": "${testVariables.chapter_id || 'CH03'}",
  "chapterName": "${testVariables.chapter_name || '第三章 面向对象程序设计'}",
  "questions": [
    {
      "tempId": "Q1",
      "type": "SINGLE_CHOICE",
      "difficulty": "MEDIUM",
      "stem": "关于Java运行时多态，下列说法正确的是？",
      "options": [
        { "key": "A", "content": "父类引用只能指向父类对象" },
        { "key": "B", "content": "父类引用可以指向子类对象，并根据实际对象调用重写的方法" },
        { "key": "C", "content": "多态只发生在方法重载中" },
        { "key": "D", "content": "多态要求父类和子类方法名称必须不同" }
      ],
      "answer": ["B"],
      "acceptableAnswers": [],
      "explanation": "Java运行时多态允许父类引用指向子类对象，当调用被重写的方法时，实际执行的方法由运行时对象类型决定。[S1]",
      "scoringPoints": [],
      "knowledgePoints": [{ "id": "KP003", "name": "多态" }],
      "sourceIds": ["S1"],
      "suggestedScore": 2
    },
    {
      "tempId": "Q2",
      "type": "MULTIPLE_CHOICE",
      "difficulty": "MEDIUM",
      "stem": "下列关于Java方法重写的条件与约束，正确的有？",
      "options": [
        { "key": "A", "content": "子类方法与父类方法的方法名和形参列表必须相同" },
        { "key": "B", "content": "子类方法的访问修饰权限不能低于父类对应方法" },
        { "key": "C", "content": "子类方法抛出的受检异常范围不能宽于父类对应方法" },
        { "key": "D", "content": "私有方法（private）也可以在子类中被重写" }
      ],
      "answer": ["A", "B", "C"],
      "acceptableAnswers": [],
      "explanation": "重写要求遵循'两同两小一大'原则：方法名和参数列表相同，返回值与抛出异常类型不大于父类，访问权限不小于父类。private方法对子类不可见，无法重写。[S2]",
      "scoringPoints": [],
      "knowledgePoints": [{ "id": "KP002", "name": "方法重写" }],
      "sourceIds": ["S2"],
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
      "courseId": "${testVariables.course_id || '10001'}",
      "courseName": "${testVariables.course_name || 'Java程序设计'}",
      "chapterId": "${testVariables.chapter_id || 'CH03'}",
      "chapterName": "${testVariables.chapter_name || '第三章 面向对象程序设计'}",
      "lessonTitle": "${testVariables.lesson_title || 'Java运行时多态与动态绑定机制'}",
      "lessonCount": 2,
      "totalDurationMinutes": 90,
      "studentLevel": "软件技术专业大二学生"
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
    "resources": ["Java程序设计教材", "课程PPT", "IDE开发环境", "课堂示例代码"],
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
[S1] 《Java程序设计教材》· 第三章 面向对象 · 第86页
[S2] 《Java面向对象核心讲义》· 虚方法表 · 第24页`;
}

const DUMMY_VARIABLES: Record<string, string> = {
  course_name: 'Java程序设计',
  course_id: '10001',
  course_description: '面向软件技术专业核心基础课，主要讲解面向对象与核心类库。',
  chapter_name: '第三章 面向对象程序设计',
  knowledge_point_name: '多态与动态分派',
  user_role: 'STUDENT',
  answer_depth: 'NORMAL',
  language: 'zh-CN',
  allow_general_knowledge: 'true',
  question: 'Java中多态的具体实现原理是什么？',
  conversation_history: '[User]: 什么是继承？\n[Assistant]: 继承是面向对象三大特性之一...',
  retrieved_context: `<rag_context>
<source id="S1" document="Java程序设计教材.pdf" chapter="第三章 面向对象" section="3.4 多态" page="86">
Java中的多态是指同一个引用类型在不同运行状态下，可以指向不同类型的对象，并表现出不同的行为。实际调用的方法实现由运行时堆中真实对象类型决定。
</source>
<source id="S2" document="Java面向对象核心讲义.pptx" chapter="第三章 面向对象" section="虚方法表" page="24">
JVM通过虚方法表（vtable）进行动态分派，实现父类引用调用子类重写方法。
</source>
<source id="S3" document="Java程序设计课堂案例.docx" chapter="第三章 面向对象" section="Animal-Dog-Cat多态示例" page="12">
通过Animal、Dog、Cat三个类的示例，演示同一父类引用指向不同子类对象时产生不同输出的多态现象。
</source>
</rag_context>`,
  context: `<rag_context>
<source id="S1" document="Java程序设计教材.pdf" chapter="第三章 面向对象" section="3.4 多态" page="86">
Java中的多态是指同一个引用类型在不同运行状态下，可以指向不同类型的对象，并表现出不同的行为。实际调用的方法实现由运行时堆中真实对象类型决定。
</source>
<source id="S2" document="Java面向对象核心讲义.pptx" chapter="第三章 面向对象" section="虚方法表" page="24">
JVM通过虚方法表（vtable）进行动态分派，实现父类引用调用子类重写方法。
</source>
<source id="S3" document="Java程序设计课堂案例.docx" chapter="第三章 面向对象" section="Animal-Dog-Cat多态示例" page="12">
通过Animal、Dog、Cat三个类的示例，演示同一父类引用指向不同子类对象时产生不同输出的多态现象。
</source>
</rag_context>`,
  count: '3',
  difficulty: 'MEDIUM',
  question_type: '单选题',
  chapter_id: 'CH03',
  knowledge_point_ids: 'KP003,KP004',
  knowledge_point_names: '继承与重写,多态与动态分派',
  question_types: 'SINGLE_CHOICE,MULTIPLE_CHOICE,SHORT_ANSWER',
  question_count: '3',
  task_purpose: 'HOMEWORK',
  student_level: '本科二年级',
  score_per_question: '5',
  generation_requirements:
    'knowledgePointId 必须使用 KP003/KP004 等系统 ID，不得写中文名；chapterName 与输入完全一致；单选题干扰项需包含编译期/运行期或重载/重写混淆',
  existing_questions: '["什么是多态？","Java中单继承关键字是什么？"]',
  question_stem: '简述TCP与UDP的核心区别',
  standard_answer: 'TCP面向连接、可靠交付；UDP无连接、最大努力交付。',
  max_score: '10',
  student_answer: 'TCP连接前要三次握手，可靠传输；UDP不建连接，速度快。',
  teaching_hours: '2',
  target_students: '计算机专业大二学生',
  lesson_title: 'Java运行时多态与动态绑定机制',
  lesson_duration: '90分钟',
  lesson_count: '2',
  class_profile: '已掌握Java类与对象、继承与重写，但对声明类型与运行时对象类型的关系理解较弱',
  teaching_mode: 'BOPPPS',
  teaching_method: '问题驱动,案例教学,代码演示,任务驱动',
  teaching_objectives: '理解运行时多态实现条件，能分析并预测多态代码执行结果',
  teacher_requirements: '结合Animal/Dog/Cat案例，设计课堂互动与代码预测环节，轻微口语化表述不扣分',
  previous_learning: '类与对象,继承,方法重写',
  next_learning: '抽象类,接口,面向接口编程',
  available_resources: 'Java程序设计教材,课程PPT,IDE开发环境',
  assessment_requirement: '设计课堂代码预测题作为形成性评价，检查学生是否能区分声明类型与运行时对象类型',
  homework_requirement: '布置2道代码执行分析题，以及1道多态设计实践题',
  output_depth: 'DETAILED'
};

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

  const populateDummyVariables = () => {
    if (!activeItem.value) return;

    activeItem.value.variables.forEach((v) => {
      if (DUMMY_VARIABLES[v.name]) {
        testVariables.value[v.name] = DUMMY_VARIABLES[v.name];
      }
    });
    ElMessage.success('已自动载入课程真实测试变量');
  };

  const executePlaygroundTest = async () => {
    if (!activeItem.value) return;
    const startTime = Date.now();
    testResultOutput.value = '';

    await runTest(activeItem.value.id, {
      systemPrompt: activeItem.value.systemPrompt,
      userPromptTemplate: activeItem.value.userPromptTemplate,
      variables: testVariables.value,
      model: activeItem.value.boundModel || 'deepseek-chat',
      temperature: activeItem.value.temperature ?? 0.3,
      maxTokens: activeItem.value.maxTokens
    });

    testDuration.value = Date.now() - startTime;
    if (testResult.value?.output) {
      testResultOutput.value = testResult.value.output;
    } else if (activeItem.value) {
      testResultOutput.value = buildFallbackTestOutput(activeItem.value, testVariables.value);
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
