import { ref, reactive, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { Question, QuestionType, Difficulty } from '@/types/question/question';
import type { KnowledgePointItem } from '@/components/ai/generation/question-generate-types';
import { MOCK_COURSES } from '@/mock/courses';
import { generateQuestions } from '@/api/ai/generation';
import { loadGenerationCourseOptions } from '@/services/ai/generation-service';
import { batchSaveQuestions } from '@/api/question/question';
import { getQuestionBanks, createQuestionBank, addQuestionsToBank } from '@/api/question/question-bank';
import { getChapters } from '@/api/course/chapter';
import { getCourseKnowledgePoints } from '@/api/course/knowledge-point';
import { normalizeQuestionList, normalizeQuestion } from '@/utils/question/normalize-question';
import { isGarbageQuestionStem } from '@/utils/question/is-garbage-question-stem';

export interface BatchSaveOptions {
  bankMode?: 'NEW_BANK' | 'EXISTING_BANK' | 'RAW_POOL';
  bankName?: string;
  bankDescription?: string;
  existingBankId?: number | string;
}

const STORAGE_KEY_FORM = 'edumind_question_form_state';
const STORAGE_KEY_QUESTIONS = 'edumind_generated_questions';

function readStoredFormState() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY_FORM);
    if (raw) return JSON.parse(raw);
  } catch {}
  return null;
}

function readStoredQuestions(): Question[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY_QUESTIONS);
    if (raw) return JSON.parse(raw);
  } catch {}
  return [];
}

const initialForm = readStoredFormState();

const generatedQuestions = ref<Question[]>(readStoredQuestions());
const globalCourses = ref<any[]>([]);
const globalCourseChapters = ref<any[]>([]);
const globalCourseKnowledgePoints = ref<KnowledgePointItem[]>([]);

const formState = reactive({
  courseId: initialForm?.courseId || 0,
  chapterIds: (initialForm?.chapterIds || []) as number[],
  knowledgePointIds: (initialForm?.knowledgePointIds || []) as number[],
  knowledgePointNames: (initialForm?.knowledgePointNames || []) as string[],
  questionTypes: (initialForm?.questionTypes || ['SINGLE_CHOICE', 'MULTIPLE_CHOICE']) as QuestionType[],
  difficulty: (initialForm?.difficulty || 'MEDIUM') as Difficulty,
  count: initialForm?.count || 5,
  scorePerQuestion: initialForm?.scorePerQuestion || 5,
  promptDirective: initialForm?.promptDirective || '注重真实工程案例与应用场景；干扰项需针对学生常见思维误区深度剖析',
  questionScene: initialForm?.questionScene || '随堂巩固测验',
  customInstruction: initialForm?.customInstruction || ''
});

watch(
  () => formState,
  (val) => {
    try {
      sessionStorage.setItem(STORAGE_KEY_FORM, JSON.stringify(val));
    } catch {}
  },
  { deep: true }
);

watch(
  () => generatedQuestions.value,
  (val) => {
    try {
      sessionStorage.setItem(STORAGE_KEY_QUESTIONS, JSON.stringify(val));
    } catch {}
  },
  { deep: true }
);

export function useQuestionGenerate() {
  const router = useRouter();
  const route = useRoute();
  const currentStep = ref(1);
  const generating = ref(false);
  const regeneratingIndex = ref<number | null>(null);
  const courses = globalCourses;
  const courseChapters = globalCourseChapters;
  const courseKnowledgePoints = globalCourseKnowledgePoints;
  const loadingChapters = ref(false);
  const loadingKps = ref(false);

  function nextStep() {
    if (currentStep.value === 1 && !formState.courseId) {
      ElMessage.warning('请先点击选择一门关联教学课程');
      return;
    }
    if (currentStep.value < 5) currentStep.value++;
  }

  function prevStep() {
    if (currentStep.value > 1) currentStep.value--;
  }

  function goToStep(idx: number) {
    if (idx > 1 && !formState.courseId) {
      ElMessage.warning('请先点击选择一门关联教学课程');
      return;
    }
    if (idx < currentStep.value) {
      currentStep.value = idx;
    }
  }

  const displayCourses = computed(() => {
    return courses.value.length ? courses.value : MOCK_COURSES;
  });

  const selectedCourse = computed(() => {
    return displayCourses.value.find((c) => c.id === formState.courseId);
  });

  const selectedCourseName = computed(() => {
    return selectedCourse.value?.title || selectedCourse.value?.name || '未指定课程';
  });

  // 动态加载选中课程的真实章节列表
  async function loadCourseChapters(courseId: number) {
    loadingChapters.value = true;
    try {
      const res = await getChapters(courseId);
      if (res.data && Array.isArray(res.data) && res.data.length > 0) {
        courseChapters.value = res.data.map(ch => ({
          id: ch.id,
          title: ch.title,
          description: ch.description || '章节核心重点知识梳理与概念实操'
        }));
      } else {
        courseChapters.value = getFallbackChaptersForCourse(selectedCourseName.value);
      }
    } catch {
      courseChapters.value = getFallbackChaptersForCourse(selectedCourseName.value);
    } finally {
      loadingChapters.value = false;
      // 默认全选前两章
      if (courseChapters.value.length > 0 && formState.chapterIds.length === 0) {
        formState.chapterIds = courseChapters.value.slice(0, 3).map(c => c.id);
      }
    }
  }

  // 动态加载选中课程的真实知识点
  async function loadCourseKps(courseId: number) {
    loadingKps.value = true;
    try {
      const res = await getCourseKnowledgePoints(courseId);
      if (res.data && Array.isArray(res.data) && res.data.length > 0) {
        courseKnowledgePoints.value = res.data.map(kp => ({
          id: kp.id,
          title: kp.title || '核心考点',
          chapterId: kp.chapterId,
          cognitiveDimension: kp.cognitiveDimension || undefined,
          importance: kp.importance ?? 3,
          examFocus: kp.examFocus || undefined
        }));
      } else {
        courseKnowledgePoints.value = getFallbackKpsForCourse(selectedCourseName.value);
      }
    } catch {
      courseKnowledgePoints.value = getFallbackKpsForCourse(selectedCourseName.value);
    } finally {
      loadingKps.value = false;
      // 默认勾选考点（优先大纲重点与核心考点）
      if (courseKnowledgePoints.value.length > 0 && formState.knowledgePointNames.length === 0) {
        const highPriority = courseKnowledgePoints.value.filter(k => (k.importance ?? 3) >= 4);
        const defaults = highPriority.length > 0
          ? highPriority.slice(0, 4)
          : courseKnowledgePoints.value.slice(0, 4);
        formState.knowledgePointNames = defaults.map(k => k.title);
        formState.knowledgePointIds = defaults.map(k => k.id);
      }
    }
  }

  watch(
    () => formState.courseId,
    (newId) => {
      if (newId) {
        formState.chapterIds = [];
        formState.knowledgePointIds = [];
        formState.knowledgePointNames = [];
        loadCourseChapters(newId);
        loadCourseKps(newId);
      }
    },
    { immediate: false }
  );

  function toggleChapterSelect(id: number) {
    const idx = formState.chapterIds.indexOf(id);
    if (idx > -1) {
      formState.chapterIds.splice(idx, 1);
    } else {
      formState.chapterIds.push(id);
    }
  }

  function selectAllChapters() {
    formState.chapterIds = courseChapters.value.map(c => c.id);
  }

  function clearChapters() {
    formState.chapterIds = [];
  }

  function toggleKpSelect(kp: KnowledgePointItem | string) {
    const title = typeof kp === 'string' ? kp : kp.title;
    const id = typeof kp === 'object' ? kp.id : undefined;

    const idx = formState.knowledgePointNames.indexOf(title);
    if (idx > -1) {
      formState.knowledgePointNames.splice(idx, 1);
      if (id) {
        const idIdx = formState.knowledgePointIds.indexOf(id);
        if (idIdx > -1) formState.knowledgePointIds.splice(idIdx, 1);
      }
    } else {
      formState.knowledgePointNames.push(title);
      if (id && !formState.knowledgePointIds.includes(id)) {
        formState.knowledgePointIds.push(id);
      }
    }
  }

  function toggleTypeSelect(type: QuestionType) {
    const idx = formState.questionTypes.indexOf(type);
    if (idx > -1) {
      if (formState.questionTypes.length > 1) {
        formState.questionTypes.splice(idx, 1);
      } else {
        ElMessage.warning('至少需要保留一种目标出题题型');
      }
    } else {
      formState.questionTypes.push(type);
    }
  }

  function difficultyLabel(difficulty: Difficulty) {
    const map: Record<Difficulty, string> = {
      EASY: '简单（基础识记与概念理解）',
      MEDIUM: '中等（综合推导与工程应用）',
      HARD: '困难（边界辨析与复杂建模）'
    };
    return map[difficulty] || '中等难度';
  }

  async function loadCourseOptions() {
    const list = await loadGenerationCourseOptions();
    courses.value = list;

    // 1. 优先读取路由参数 ?courseId=xxx
    const queryCourseId = Number(route?.query?.courseId);
    if (queryCourseId && list.some((c) => c.id === queryCourseId)) {
      formState.courseId = queryCourseId;
    } else if (formState.courseId && list.some((c) => c.id === formState.courseId)) {
      // 保持当前有效选择
    } else {
      // 未指定时初始置空，让教师自主选择卡片
      formState.courseId = 0;
    }

    // 加载选中课程的章节与考点
    if (formState.courseId) {
      await Promise.all([
        loadCourseChapters(formState.courseId),
        loadCourseKps(formState.courseId)
      ]);
    }
    return courses.value;
  }

  let isAborted = false;

  function abortGeneration() {
    isAborted = true;
    generating.value = false;
    ElMessage.info('已中止本次 AI 命题推演');
  }

  async function generate() {
    isAborted = false;
    generating.value = true;
    try {
      const fullDirective = [formState.promptDirective, formState.customInstruction]
        .filter(Boolean)
        .join('；');

      const res = await generateQuestions({
        courseId: formState.courseId,
        chapterIds: formState.chapterIds,
        knowledgePointIds: formState.knowledgePointIds,
        knowledgePointNames: formState.knowledgePointNames,
        questionTypes: formState.questionTypes,
        difficulty: formState.difficulty,
        count: formState.count,
        scorePerQuestion: formState.scorePerQuestion,
        promptDirective: fullDirective,
        questionScene: formState.questionScene
      });

      if (isAborted) {
        return;
      }

      const list = normalizeQuestionList(res.data || []).filter((q) => !isGarbageQuestionStem(q.stem));
      if (list.length > 0) {
        generatedQuestions.value = list.map((q) => ({
          ...q,
          courseId: formState.courseId
        }));
        ElMessage.success(`AI 智能命题完成，成功生成 ${list.length} 道结构化试题`);
        router.push('/ai/question/preview');
      } else {
        throw new Error('未返回有效题目');
      }
    } catch {
      if (!isAborted) {
        ElMessage.error('AI 命题生成服务异常，请稍后重试');
      }
    } finally {
      generating.value = false;
    }
  }

  // 单题一键 AI 换一题 / 重新生成变式
  async function regenerateSingleQuestion(index: number) {
    const target = generatedQuestions.value[index];
    if (!target) return;
    regeneratingIndex.value = index;

    try {
      const kpName = target.knowledgePointNames?.[0] || formState.knowledgePointNames[0] || '核心概念';
      const fullDirective = [
        formState.promptDirective,
        `请为考点「${kpName}」重新生成一道高质量同级变式题，更换应用情境与背景数据`
      ].filter(Boolean).join('；');

      const res = await generateQuestions({
        courseId: formState.courseId,
        questionTypes: [target.type],
        difficulty: target.difficulty,
        count: 1,
        scorePerQuestion: target.score,
        knowledgePointNames: [kpName],
        promptDirective: fullDirective,
        questionScene: formState.questionScene
      });

      if (res.data && res.data.length > 0) {
        const fresh = normalizeQuestion(res.data[0]);
        if (isGarbageQuestionStem(fresh.stem)) {
          ElMessage.warning('AI 返回内容无效（疑似提示词），请重试');
          return;
        }
        generatedQuestions.value[index] = {
          ...fresh,
          courseId: formState.courseId
        };
        ElMessage.success(`第 ${index + 1} 题已由 AI 成功重新命制`);
      } else {
        ElMessage.warning('未能生成新题目，已保留原题');
      }
    } catch {
      ElMessage.error('重新生成该题失败');
    } finally {
      regeneratingIndex.value = null;
    }
  }

  function deleteQuestion(id: number | string) {
    generatedQuestions.value = generatedQuestions.value.filter(q => String(q.id) !== String(id));
    ElMessage.info('已剔除该题目');
  }

  function updateQuestion(index: number, updated: Question) {
    if (index >= 0 && index < generatedQuestions.value.length) {
      generatedQuestions.value[index] = { ...updated };
    }
  }

  const existingCourseBanks = ref<any[]>([]);

  async function loadExistingBanksForCourse(courseId?: number | string) {
    const cId = courseId || formState.courseId || (generatedQuestions.value[0] as any)?.courseId;
    if (!cId) return;
    try {
      const res = await getQuestionBanks({ courseId: cId, pageSize: 50 });
      existingCourseBanks.value = res.data?.list || [];
    } catch {
      existingCourseBanks.value = [];
    }
  }

  // 格式转换并真实批量入库
  async function batchSave(options?: BatchSaveOptions) {
    if (generatedQuestions.value.length === 0) {
      ElMessage.warning('当前暂无待入库试题');
      return;
    }

    const targetCourseId = formState.courseId || (generatedQuestions.value[0] as any)?.courseId || 0;
    if (!targetCourseId) {
      ElMessage.warning('未能识别目标课程，请先指定关联课程');
      return;
    }

    try {
      // 严格转换为后端 QuestionBatchCreateDTO 要求的数据格式
      const formatted = generatedQuestions.value
        .filter((q) => !isGarbageQuestionStem(q.stem))
        .map(q => {
        let diffNum = 3;
        if (q.difficulty === 'EASY') diffNum = 2;
        else if (q.difficulty === 'HARD') diffNum = 4;

        let optionsStr = '[]';
        if (Array.isArray(q.options)) {
          optionsStr = JSON.stringify(q.options);
        } else if (typeof q.options === 'string') {
          optionsStr = q.options;
        }

        return {
          courseId: targetCourseId,
          knowledgePointId: q.knowledgePointId || undefined,
          stem: q.stem || '',
          type: q.type,
          options: optionsStr,
          answer: q.correctAnswer || (q as any).answer || 'A',
          analysis: q.analysis || '',
          difficulty: diffNum,
          score: q.score || formState.scorePerQuestion || 5
        };
      });

      const res = await batchSaveQuestions(targetCourseId, formatted as any);
      const savedCount = res.data?.savedCount ?? generatedQuestions.value.length;
      const questionIds = res.data?.questionIds || [];

      // 模式 1：新建专属题库并关联题目
      if (options?.bankMode === 'NEW_BANK' && options.bankName?.trim()) {
        const createRes = await createQuestionBank({
          name: options.bankName.trim(),
          courseId: targetCourseId,
          description: options.bankDescription?.trim() || `由 AI 智能命题推演生成，收录 ${savedCount} 道精选试题。`
        });
        const newBankId = createRes.data;
        if (newBankId && questionIds.length > 0) {
          await addQuestionsToBank(newBankId, questionIds);
        }
        ElMessage.success(`已创建专属题库「${options.bankName.trim()}」并成功收录 ${savedCount} 道试题！`);
        sessionStorage.removeItem(STORAGE_KEY_QUESTIONS);
        generatedQuestions.value = [];
        router.push(`/question/banks/${newBankId}`);
        return;
      }

      // 模式 2：追加至已有题库
      if (options?.bankMode === 'EXISTING_BANK' && options.existingBankId) {
        if (questionIds.length > 0) {
          await addQuestionsToBank(options.existingBankId, questionIds);
        }
        ElMessage.success(`已成功追加 ${savedCount} 道试题至目标题库！`);
        sessionStorage.removeItem(STORAGE_KEY_QUESTIONS);
        generatedQuestions.value = [];
        router.push(`/question/banks/${options.existingBankId}`);
        return;
      }

      // 模式 3：仅存入公共散题池
      ElMessage.success(`成功入库 ${savedCount} 道试题至课程试题池`);
      sessionStorage.removeItem(STORAGE_KEY_QUESTIONS);
      generatedQuestions.value = [];
      router.push(`/question/list?courseId=${targetCourseId}`);
    } catch {
      ElMessage.error('入库保存失败，请检查网络或参数格式');
    }
  }

  // 导出 Markdown 格式试卷
  function exportMarkdown() {
    if (generatedQuestions.value.length === 0) return;
    const lines: string[] = [];
    lines.push(`# ${selectedCourseName.value} - AI 智能命题试卷`);
    lines.push(`> 卷面题量：${generatedQuestions.value.length} 题 | 卷面总分：${generatedQuestions.value.reduce((a, b) => a + (b.score || 0), 0)} 分 | 生成时间：${new Date().toLocaleString()}`);
    lines.push('');

    generatedQuestions.value.forEach((q, idx) => {
      lines.push(`### 第 ${idx + 1} 题【${q.typeLabel || q.type}】(${q.score} 分)`);
      lines.push(q.stem);
      lines.push('');
      if (Array.isArray(q.options) && q.options.length > 0) {
        q.options.forEach(opt => {
          lines.push(`- **${opt.key}.** ${opt.content}`);
        });
        lines.push('');
      }
      lines.push(`**【正确答案】** ${q.correctAnswer}`);
      lines.push(`**【考点点拨】** ${q.analysis}`);
      lines.push('');
    });

    const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const fileName = `${selectedCourseName.value}_AI命题试卷_${Date.now()}.md`;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  }

  return {
    currentStep,
    generating,
    regeneratingIndex,
    courses,
    displayCourses,
    selectedCourse,
    selectedCourseName,
    courseChapters,
    courseKnowledgePoints,
    loadingChapters,
    loadingKps,
    formState,
    generatedQuestions,
    existingCourseBanks,
    nextStep,
    prevStep,
    goToStep,
    toggleChapterSelect,
    selectAllChapters,
    clearChapters,
    toggleKpSelect,
    toggleTypeSelect,
    difficultyLabel,
    loadCourseOptions,
    loadExistingBanksForCourse,
    generate,
    abortGeneration,
    regenerateSingleQuestion,
    deleteQuestion,
    updateQuestion,
    batchSave,
    exportMarkdown
  };
}

function getFallbackChaptersForCourse(courseName: string): any[] {
  const s = courseName.toLowerCase();
  if (s.includes('java') || s.includes('jvm') || s.includes('面向对象') || s.includes('oop')) {
    return [
      { id: 501, title: '第 1 章：Java 核心语法、OOP 与类加载机制', description: '从 .java 到 .class、类加载过程与双亲委派模型' },
      { id: 502, title: '第 2 章：JVM 内存模型、JIT 编译与垃圾回收', description: '运行时数据区、分代 GC、解释执行与 JIT 混合模式' },
      { id: 503, title: '第 3 章：Java 集合框架与并发多线程编程', description: 'HashMap 扩容、JMM 可见性、AQS 与锁优化机制' },
      { id: 504, title: '第 4 章：I/O 流、网络通信与 Spring 框架核心', description: 'NIO 架构、反射与动态代理、IoC 与 AOP 容器实践' }
    ];
  }
  if (s.includes('ai') || s.includes('大模型') || s.includes('机器学习')) {
    return [
      { id: 201, title: '第 1 章：Transformer 与自注意力机制架构剖析', description: '缩放点积、多头注意力与自回归解码' },
      { id: 202, title: '第 2 章：大语言模型位置编码与长文本外推', description: 'RoPE 旋转位置编码、ALiBi 与插值拓展' },
      { id: 203, title: '第 3 章：参数高效微调（PEFT）与 LoRA 适配', description: '低秩矩阵分解、量化微调 QLoRA 实践' },
      { id: 204, title: '第 4 章：检索增强生成（RAG）全链路架构', description: '分块切片、向量检索、混合重排与上下文组装' }
    ];
  }
  if (s.includes('操作系统') || s.includes('os') || s.includes('linux')) {
    return [
      { id: 301, title: '第 1 章：进程与线程并发控制机制', description: '进程状态流转、用户态与内核态、上下文切换' },
      { id: 302, title: '第 2 章：临界区互斥、信号量与死锁预防', description: 'PV 操作、哲学家就餐、死锁四大必要条件' },
      { id: 303, title: '第 3 章：虚拟内存分页与页面置换算法', description: '快表 TLB、多级页表、缺页异常、FIFO 与 LRU' },
      { id: 304, title: '第 4 章：文件系统与 I/O 设备中断处理', description: 'inode 索引节点、VFS 抽象、磁盘调度算法' }
    ];
  }
  if (s.includes('数学') || s.includes('微积分')) {
    return [
      { id: 401, title: '第 1 章：函数极限与无穷小量代换', description: '极限保号性、夹逼定理、海涅定理与泰勒展开' },
      { id: 402, title: '第 2 章：导数几何意义与微分中值定理', description: '罗尔定理、拉格朗日中值定理、柯西中值定理' },
      { id: 403, title: '第 3 章：一元函数不定积分与定积分计算', description: '换元积分法、分部积分法、变上限积分求导' },
      { id: 404, title: '第 4 章：常微分方程与多元函数偏导数', description: '一阶线性微分方程、齐次方程、极值充分条件' }
    ];
  }
  return [
    { id: 101, title: '第 1 章：线性表与链式存储结构设计', description: '单链表逆置、双向循环链表、顺序表扩容机制' },
    { id: 102, title: '第 2 章：树与二叉树拓扑遍历与平衡调整', description: '二叉树递归非递归遍历、哈夫曼树、AVL 树与红黑树' },
    { id: 103, title: '第 3 章：图结构遍历与最短路径算法', description: '邻接表、BFS/DFS、Dijkstra 与 Floyd 算法' },
    { id: 104, title: '第 4 章：内部排序与散列表冲突探测', description: '快速排序、堆排序、除留余数法与二次探测' }
  ];
}

// 辅助函数：根据课程生成学科专属考点兜底
function getFallbackKpsForCourse(courseName: string): KnowledgePointItem[] {
  const s = courseName.toLowerCase();
  if (s.includes('java') || s.includes('jvm') || s.includes('面向对象') || s.includes('oop')) {
    return [
      { id: 5001, title: 'Java类加载过程与双亲委派模型', cognitiveDimension: 'ANALYZE', importance: 5, examFocus: 'Bootstrap/Ext/App 加载层次与打破委派' },
      { id: 5002, title: 'JVM运行时数据区与分代垃圾回收GC', cognitiveDimension: 'APPLY', importance: 5, examFocus: '堆空间新生代老年代划分与垃圾标记算法' },
      { id: 5003, title: 'JIT即时编译与解释执行混合模式', cognitiveDimension: 'UNDERSTAND', importance: 4, examFocus: '热点代码探测与逃逸分析优化' },
      { id: 5004, title: 'HashMap底层原理与扩容机制', cognitiveDimension: 'APPLY', importance: 5, examFocus: '链表转红黑树阈值与扰动函数' },
      { id: 5005, title: 'Volatile关键字与JMM内存可见性', cognitiveDimension: 'ANALYZE', importance: 4, examFocus: '指令重排序禁止与内存屏障' },
      { id: 5006, title: 'ReentrantLock与AQS同步器原理', cognitiveDimension: 'APPLY', importance: 4, examFocus: 'CLH队列锁状态维护与条件变量' }
    ];
  }
  if (s.includes('ai') || s.includes('大模型') || s.includes('机器学习')) {
    return [
      { id: 1001, title: 'Transformer自注意力机制', cognitiveDimension: 'APPLY', importance: 5, examFocus: '缩放点积公式与多头并行' },
      { id: 1002, title: 'RoPE旋转位置编码', cognitiveDimension: 'ANALYZE', importance: 4, examFocus: '相对位置外推与复数旋转' },
      { id: 1003, title: 'LoRA参数高效微调', cognitiveDimension: 'APPLY', importance: 5, examFocus: '低秩矩阵更新与显存优化' },
      { id: 1004, title: 'RAG向量检索与重排', cognitiveDimension: 'APPLY', importance: 4, examFocus: 'Top-K 召回与幻觉抑制' },
      { id: 1005, title: '大模型Token化与BPE词表原理', cognitiveDimension: 'UNDERSTAND', importance: 3 },
      { id: 1006, title: 'RLHF与DPO模型偏好对齐', cognitiveDimension: 'UNDERSTAND', importance: 3 },
      { id: 1007, title: 'KV Cache自回归显存优化', cognitiveDimension: 'ANALYZE', importance: 4, examFocus: '自回归推理缓存与PagedAttention' }
    ];
  }
  if (s.includes('操作系统') || s.includes('os') || s.includes('linux')) {
    return [
      { id: 2001, title: '进程互斥与信号量PV操作', cognitiveDimension: 'APPLY', importance: 5, examFocus: '临界区互斥与生产者消费者' },
      { id: 2002, title: '死锁产生条件与死锁避免', cognitiveDimension: 'ANALYZE', importance: 5, examFocus: '银行家算法与循环等待' },
      { id: 2003, title: '进程与线程概念及状态流转', cognitiveDimension: 'UNDERSTAND', importance: 3 },
      { id: 2004, title: '虚拟内存分页与缺页异常', cognitiveDimension: 'UNDERSTAND', importance: 4, examFocus: '页表结构与内核态陷阱' },
      { id: 2005, title: 'LRU页面置换算法', cognitiveDimension: 'APPLY', importance: 4, examFocus: '栈式置换特性与Belady异常' },
      { id: 2006, title: 'CPU时间片轮转调度与优先权', cognitiveDimension: 'APPLY', importance: 3 }
    ];
  }
  if (s.includes('数学') || s.includes('微积分')) {
    return [
      { id: 3001, title: '泰勒公式与无穷小代换', cognitiveDimension: 'APPLY', importance: 5, examFocus: '高阶无穷小展开与极限抵消' },
      { id: 3002, title: '拉格朗日中值定理应用', cognitiveDimension: 'ANALYZE', importance: 4, examFocus: '不等式证明与函数单调性' },
      { id: 3003, title: '导数定义与连续性关系', cognitiveDimension: 'UNDERSTAND', importance: 3 },
      { id: 3004, title: '定积分换元法与分部积分', cognitiveDimension: 'APPLY', importance: 5, examFocus: '积分上下限同步替换' },
      { id: 3005, title: '多元函数偏导数极值与驻点', cognitiveDimension: 'APPLY', importance: 3 }
    ];
  }
  return [
    { id: 4001, title: '平衡二叉树平衡调整(AVL)', cognitiveDimension: 'APPLY', importance: 5, examFocus: 'LL/RR/LR/RL 旋转机制' },
    { id: 4002, title: '线性表顺序存储与链式存储对比', cognitiveDimension: 'UNDERSTAND', importance: 3 },
    { id: 4003, title: '哈希冲突与二次探测法', cognitiveDimension: 'UNDERSTAND', importance: 4, examFocus: '一次聚集与二次聚集辨析' },
    { id: 4004, title: '快速排序枢轴划分策略', cognitiveDimension: 'APPLY', importance: 5, examFocus: '最坏时间复杂度与稳定性' },
    { id: 4005, title: 'Dijkstra单源最短路径', cognitiveDimension: 'APPLY', importance: 4, examFocus: '贪心策略与负权边限制' },
    { id: 4006, title: '循环队列队满队空判定', cognitiveDimension: 'APPLY', importance: 3 }
  ];
}
