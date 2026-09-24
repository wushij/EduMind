import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { usePrompt } from '@/composables/system/usePrompt';
import { resolveModels } from '@/composables/system/useAIModel';
import {
  applyPromptDemoVariables,
  resolvePromptDemoFill
} from '@/composables/system/usePromptDemoData';
import { PromptTemplate } from '@/types/system/prompt';
import type { PromptVersionItem } from '@/types/system/prompt';

export interface ModelOption {
  label: string;
  value: string;
  provider: string;
  modelKey?: string;
  modelName?: string;
  isDefault?: boolean;
  hasApiKey?: boolean;
}

export interface VersionDiffInfo {
  tag: string;
  tagType: 'base' | 'identical' | 'updated';
  summary: string;
  sysDiff: string;
  userDiff: string;
  varsDiff: string;
  isIdentical: boolean;
  isBase: boolean;
  prevVerNum?: number;
}

export function createDefaultForm(): PromptTemplate {
  return {
    id: 0,
    code: 'COURSE_RAG_GENERAL',
    name: '通用课程问答（RAG）',
    category: 'rag',
    description: '围绕当前课程、章节、知识点和课程知识库，为教师和学生提供具备资料溯源能力的专业智能问答。',
    systemPrompt: '',
    userPromptTemplate:
      '请基于当前课程知识库回答下面的问题。\n\n当前课程：{{course_name}}\n当前章节：{{chapter_name}}\n当前知识点：{{knowledge_point_name}}\n\n用户问题：\n{{question}}',
    variables: [
      { name: 'course_name', label: '课程名称', defaultValue: 'Java面向对象程序设计' },
      { name: 'chapter_name', label: '章节名称', defaultValue: '2.1 封装、继承与多态机制' },
      { name: 'knowledge_point_name', label: '知识点', defaultValue: '面向对象三大特征与多态运行时绑定' },
      { name: 'question', label: '用户提问', defaultValue: '多态的实现需要哪些必要条件？运行时又是如何确定实际调用的方法？' }
    ],
    version: 'v1.0.0',
    status: 'DRAFT',
    boundModel: '',
    temperature: 0.3,
    maxTokens: 8000,
    callCount: 0,
    createdAt: '',
    updatedAt: ''
  };
}

export function createEmptyForm(): PromptTemplate {
  return {
    id: 0,
    code: '',
    name: '',
    category: 'rag',
    description: '',
    systemPrompt: '',
    userPromptTemplate: '',
    variables: [],
    version: 'v1.0.0',
    status: 'DRAFT',
    boundModel: '',
    temperature: 0.3,
    maxTokens: 8000,
    callCount: 0,
    createdAt: '',
    updatedAt: ''
  };
}

export function getCategoryLabel(category: string): string {
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
      return 'Agent 规划';
    default:
      return '通用';
  }
}

export function getVariablesList(vars?: string): string[] {
  if (!vars) return [];
  try {
    const trimmed = vars.trim();
    if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed
          .map((item) => (typeof item === 'string' ? item : item.name || ''))
          .filter(Boolean);
      }
    }
  } catch {
    // fallback to comma-separated
  }
  return vars
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean);
}

export function isCurrentVersion(formVersion: string | number | undefined, ver?: number): boolean {
  if (ver === undefined || ver === null) return false;
  if (formVersion === undefined || formVersion === null) return false;
  if (typeof formVersion === 'number') return formVersion === ver;
  const match = String(formVersion).match(/v?(\d+)/i);
  return match ? parseInt(match[1], 10) === ver : false;
}

export function getVersionDiffInfo(
  item: PromptVersionItem,
  versionHistory: PromptVersionItem[]
): VersionDiffInfo {
  const sorted = [...versionHistory].sort((a, b) => a.version - b.version);
  const idx = sorted.findIndex((v) => v.version === item.version);

  if (idx <= 0) {
    const varCount = getVariablesList(item.variables).length;
    return {
      tag: '初始基线',
      tagType: 'base',
      summary: `初始生产基线版本 · 包含 ${varCount} 个插槽`,
      sysDiff: '初始基线角色指令',
      userDiff: '初始基线用户模板',
      varsDiff: `共 ${varCount} 个动态参数插槽`,
      isIdentical: false,
      isBase: true
    };
  }

  const prev = sorted[idx - 1];
  const curSys = (item.systemPrompt || '').trim();
  const prevSys = (prev.systemPrompt || '').trim();
  const curUser = (item.content || '').trim();
  const prevUser = (prev.content || '').trim();
  const curVars = getVariablesList(item.variables);
  const prevVars = getVariablesList(prev.variables);

  const sysChanged = curSys !== prevSys;
  const userChanged = curUser !== prevUser;
  const varsChanged = curVars.join(',') !== prevVars.join(',');

  if (!sysChanged && !userChanged && !varsChanged) {
    return {
      tag: '配置无变更',
      tagType: 'identical',
      summary: `较 v${prev.version}.0 配置完全一致（重复发布）`,
      sysDiff: `与 v${prev.version}.0 完全一致`,
      userDiff: `与 v${prev.version}.0 完全一致`,
      varsDiff: `与 v${prev.version}.0 完全一致`,
      isIdentical: true,
      isBase: false,
      prevVerNum: prev.version
    };
  }

  const changes: string[] = [];
  let sysDiff = `与 v${prev.version}.0 一致`;
  let userDiff = `与 v${prev.version}.0 一致`;
  let varsDiff = `与 v${prev.version}.0 一致`;

  if (sysChanged) {
    const diff = curSys.length - prevSys.length;
    const diffStr = diff > 0 ? `+${diff} 字符` : `${diff} 字符`;
    sysDiff = `已调优 (${diffStr})`;
    changes.push(`System 指令调优 (${diffStr})`);
  }

  if (userChanged) {
    const diff = curUser.length - prevUser.length;
    const diffStr = diff > 0 ? `+${diff} 字符` : `${diff} 字符`;
    userDiff = `已调整 (${diffStr})`;
    changes.push(`User 模板调整 (${diffStr})`);
  }

  if (varsChanged) {
    const added = curVars.filter((v) => !prevVars.includes(v));
    const removed = prevVars.filter((v) => !curVars.includes(v));
    const parts: string[] = [];
    if (added.length) parts.push(`新增 {{${added.join('}}, {{')}}}`);
    if (removed.length) parts.push(`移除 {{${removed.join('}}, {{')}}}`);
    varsDiff = parts.join(' · ') || '参数插槽已调整';
    changes.push(varsDiff);
  }

  return {
    tag: '提示词调优',
    tagType: 'updated',
    summary: `较 v${prev.version}.0：${changes.join(' · ')}`,
    sysDiff,
    userDiff,
    varsDiff,
    isIdentical: false,
    isBase: false,
    prevVerNum: prev.version
  };
}

export function formatVersionDate(dateStr?: string): string {
  if (!dateStr) return '最近发布';
  return dateStr.replace('T', ' ').replace(/\.\d+.*$/, '');
}

export function extractVariableNamesFromPrompts(systemPrompt: string, userPromptTemplate: string): string[] {
  const combined = `${systemPrompt} ${userPromptTemplate}`;
  const matches = combined.match(/\{\{([a-zA-Z0-9_-]+)\}\}/g);
  if (!matches) return [];
  return Array.from(new Set(matches.map((m) => m.replace(/[\{\}]/g, '').trim())));
}


export function usePromptEditor() {
  const route = useRoute();
  const router = useRouter();
  const promptId = route.params.id ? Number(route.params.id) : null;
  const systemExpanded = ref(false);

  const {
    testing,
    publishing,
    rollingBack,
    currentPrompt,
    versionHistory,
    testResult,
    loadPrompt,
    loadVersions,
    handleSave,
    handlePublish,
    handleRollback,
    runTest
  } = usePrompt();

  const modelOptions = ref<ModelOption[]>([]);
  const defaultModelName = ref('');
  const modelsLoading = ref(false);
  const defaultModelLabel = computed(() => {
    return defaultModelName.value
      ? `未指定模型（跟随系统默认: ${defaultModelName.value}）`
      : '未指定模型（跟随系统网关默认）';
  });

  const form = ref<PromptTemplate>(createDefaultForm());
  const testVariables = ref<Record<string, string>>({});
  const testResultOutput = ref('');
  /** 演示数据需要实时拉取课程 / 章节 / 知识点 / 知识库 / 题库，按钮需给出进行中反馈 */
  const demoFilling = ref(false);
  const versionDetailVisible = ref(false);
  const selectedVersion = ref<PromptVersionItem | null>(null);

  const selectedVersionVariables = computed<string[]>(() => {
    if (!selectedVersion.value?.variables) return [];
    return getVariablesList(selectedVersion.value.variables);
  });

  const selectedVersionDiffInfo = computed<VersionDiffInfo>(() => {
    if (!selectedVersion.value) {
      return {
        tag: '版本快照',
        tagType: 'base',
        summary: '',
        sysDiff: '',
        userDiff: '',
        varsDiff: '',
        isIdentical: false,
        isBase: true
      };
    }
    return getVersionDiffInfo(selectedVersion.value, versionHistory.value);
  });

  const checkIsCurrentVersion = (ver?: number) => isCurrentVersion(form.value.version, ver);

  const copyText = async (text: string, msg = '已复制到剪贴板') => {
    if (!text) {
      ElMessage.warning('内容为空，无需复制');
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      ElMessage.success(msg);
    } catch {
      ElMessage.error('复制失败，请手动选取复制');
    }
  };

  const ensureBoundModelInOptions = () => {
    if (!form.value.boundModel) return;
    const exists = modelOptions.value.some(
      (o) => o.value === form.value.boundModel || o.modelName === form.value.boundModel
    );
    if (!exists) {
      modelOptions.value.unshift({
        label: `${form.value.boundModel} (已绑定)`,
        value: form.value.boundModel,
        provider: 'SAVED'
      });
    }
  };

  const loadAvailableModels = async () => {
    modelsLoading.value = true;
    try {
      const list = await resolveModels();
      const chatModels = (list || []).filter(
        (m) => (m.configType === 'chat' || !m.configType) && m.status !== 'disabled'
      );
      if (chatModels.length > 0) {
        const def = chatModels.find((m) => m.isDefault) || chatModels[0];
        if (def) {
          defaultModelName.value = def.modelKey || def.name || def.modelName;
        }

        modelOptions.value = chatModels.map((m) => ({
          label: m.name || m.modelName,
          value: m.modelKey || m.name || m.modelName,
          modelKey: m.modelKey || m.name,
          modelName: m.modelName,
          provider: (m.provider || 'AI').toUpperCase(),
          isDefault: Boolean(m.isDefault),
          hasApiKey: Boolean(m.hasApiKey)
        }));

        if (form.value.boundModel) {
          const exists = modelOptions.value.some(
            (o) => o.value === form.value.boundModel || o.modelName === form.value.boundModel
          );
          if (!exists) {
            modelOptions.value.unshift({
              label: `${form.value.boundModel} (已绑定)`,
              value: form.value.boundModel,
              provider: 'CONFIG'
            });
          }
        }
      }
    } catch (err) {
      console.warn('加载系统 AI 模型配置失败', err);
      if (form.value.boundModel) {
        modelOptions.value = [
          { label: form.value.boundModel, value: form.value.boundModel, provider: 'CURRENT' }
        ];
      }
    } finally {
      modelsLoading.value = false;
    }
  };

  const insertVar = (name: string) => {
    form.value.userPromptTemplate += ` {{${name}}}`;
    ElMessage.success(`已插入插槽 {{${name}}}`);
  };

  const addVariable = () => {
    form.value.variables.push({
      name: `var_${form.value.variables.length + 1}`,
      label: '自定义参数',
      defaultValue: ''
    });
  };

  const handleRemoveVariable = async (
    index: number,
    v: { name: string; label?: string }
  ) => {
    const displayTitle = v.label ? `「${v.label} (${v.name})」` : `「${v.name}」`;
    try {
      await ElMessageBox.confirm(
        `确定删除参数插槽 ${displayTitle} 吗？删除后该变量将无法在热测试与上下文推理中自动注入。`,
        '删除参数插槽确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger',
          lockScroll: false
        }
      );
      form.value.variables.splice(index, 1);
      delete testVariables.value[v.name];
      ElMessage.success('已删除参数插槽');
    } catch {
      // user cancelled
    }
  };

  const syncVariablesFromPrompts = () => {
    const uniqueNames = extractVariableNamesFromPrompts(
      form.value.systemPrompt,
      form.value.userPromptTemplate
    );
    if (!uniqueNames.length) {
      ElMessage.info('未在当前提示词中识别到 {{插槽}} 格式变量');
      return;
    }

    const existingNames = new Set(form.value.variables.map((v) => v.name));
    let addedCount = 0;
    uniqueNames.forEach((name) => {
      if (!existingNames.has(name)) {
        form.value.variables.push({ name, label: name, defaultValue: '' });
        addedCount++;
      }
    });

    if (addedCount > 0) {
      ElMessage.success(`已自动识别并追加 ${addedCount} 个变量插槽`);
    } else {
      ElMessage.success('当前变量插槽与提示词已完全同步');
    }
  };

  /**
   * 一键填入高校课程真实 RAG 示例。
   * 数据来自当前部署的真实课程 / 章节 / 知识点 / 知识库切片 / 题库题目，
   * 接口不可用时由 `resolvePromptDemoFill` 静默回落到仓库种子快照。
   */
  const fillUniversityDemoData = async () => {
    const slotNames = form.value.variables.map((v) => v.name);
    if (!slotNames.length) {
      ElMessage.warning('当前模板未声明任何参数插槽，无法注入演示数据');
      return;
    }
    if (demoFilling.value) return;
    demoFilling.value = true;
    try {
      const fill = await resolvePromptDemoFill();
      const filled = applyPromptDemoVariables(testVariables.value, fill.variables, slotNames);
      if (!filled) {
        ElMessage.warning('当前模板插槽与演示数据不匹配，请先执行【同步插槽】再试');
        return;
      }
      const suffix = fill.source === 'remote' ? '' : '（后端数据不可用，已回落到种子快照）';
      ElMessage.success(`已填入 ${filled} 个真实课程示例：${fill.label}${suffix}`);
    } finally {
      demoFilling.value = false;
    }
  };

  const syncTestVariablesFromForm = () => {
    form.value.variables.forEach((v) => {
      if (testVariables.value[v.name] === undefined) {
        testVariables.value[v.name] = v.defaultValue || '';
      }
    });
  };

  const handleRunTest = async () => {
    const templateId = form.value.id || promptId;
    if (!templateId) {
      ElMessage.warning('请先点击右上角【保存草稿】后再执行测试');
      return;
    }
    testResultOutput.value = '';

    const ok = await runTest(templateId, {
      systemPrompt: form.value.systemPrompt,
      userPromptTemplate: form.value.userPromptTemplate,
      variables: testVariables.value,
      model: form.value.boundModel || defaultModelName.value || '',
      temperature: form.value.temperature,
      maxTokens: form.value.maxTokens
    });

    if (!ok) {
      // 失败已由全局拦截器提示，不再伪造兜底输出，避免被误读为测试通过
      return;
    }
    if (testResult.value?.output) {
      testResultOutput.value = testResult.value.output;
    }
  };

  const saveForm = async () => {
    const newId = await handleSave(form.value);
    if (newId && !form.value.id) {
      form.value.id = newId;
      router.replace(`/system/prompts/editor/${newId}`);
    }
  };

  const publishForm = async () => {
    if (form.value.id && versionHistory.value.length > 0) {
      const sorted = [...versionHistory.value].sort((a, b) => b.version - a.version);
      const latestVersion = sorted[0];
      if (latestVersion) {
        const curSys = (form.value.systemPrompt || '').trim();
        const latestSys = (latestVersion.systemPrompt || '').trim();
        const curUser = (form.value.userPromptTemplate || '').trim();
        const latestUser = (latestVersion.content || '').trim();
        const formVars = (form.value.variables || []).map((v) => v.name).sort().join(',');
        const verVars = getVariablesList(latestVersion.variables).sort().join(',');

        if (curSys === latestSys && curUser === latestUser && formVars === verVars) {
          try {
            await ElMessageBox.confirm(
              `当前编辑器的 System Prompt、User 指令模板及参数插槽与当前最新版本 (v${latestVersion.version}.0) 完全一致，未检测到任何修改。\n\n重复发布将递增生成一个内容完全相同的新快照版本 (v${latestVersion.version + 1}.0)。确定仍要发布吗？`,
              '提示词无变更提示',
              {
                confirmButtonText: '确定发布新版本',
                cancelButtonText: '取消',
                type: 'info',
                lockScroll: false
              }
            );
          } catch {
            return;
          }
        }
      }
    }

    await saveForm();
    if (form.value.id) {
      await handlePublish(form.value.id);
      await loadVersions(form.value.id);
    }
  };

  const openVersionDetail = (item: PromptVersionItem) => {
    selectedVersion.value = item;
    versionDetailVisible.value = true;
  };

  const handleDetailRollback = async () => {
    if (!selectedVersion.value) return;
    const ver = selectedVersion.value.version;
    versionDetailVisible.value = false;
    await rollbackVersion(ver);
  };

  const rollbackVersion = async (targetVersion: number) => {
    if (!form.value.id) return;
    const currentVerStr = form.value.version || '当前运行版本';
    try {
      await ElMessageBox.confirm(
        `确定将提示词模板「${form.value.name}」从 ${currentVerStr} 回滚至历史版本 v${targetVersion}.0 吗？\n\n回滚后，当前在线运行的 System Prompt、User 指令插槽及参数规格将被历史版本完全覆盖并立即生效。`,
        '版本回滚二次确认',
        {
          confirmButtonText: '确定回滚',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--warning',
          lockScroll: false
        }
      );
    } catch {
      return;
    }

    const ok = await handleRollback(form.value.id, targetVersion);
    if (ok && currentPrompt.value) {
      form.value = JSON.parse(JSON.stringify(currentPrompt.value));
      syncTestVariablesFromForm();
    }
  };

  const initEditor = async () => {
    await loadAvailableModels();
    if (!promptId) return;

    await loadPrompt(promptId);
    await loadVersions(promptId);
    if (currentPrompt.value) {
      form.value = JSON.parse(JSON.stringify(currentPrompt.value));
      if (form.value.boundModel === 'deepseek-chat') {
        form.value.boundModel = '';
      }
      syncTestVariablesFromForm();
      ensureBoundModelInOptions();
    } else {
      form.value = createEmptyForm();
      testVariables.value = {};
    }
  };

  onMounted(() => {
    initEditor();
  });

  return {
    promptId,
    systemExpanded,
    testing,
    publishing,
    rollingBack,
    versionHistory,
    testResult,
    modelOptions,
    defaultModelName,
    defaultModelLabel,
    modelsLoading,
    form,
    testVariables,
    testResultOutput,
    demoFilling,
    versionDetailVisible,
    selectedVersion,
    selectedVersionVariables,
    selectedVersionDiffInfo,
    getCategoryLabel,
    copyText,
    insertVar,
    addVariable,
    handleRemoveVariable,
    syncVariablesFromPrompts,
    fillUniversityDemoData,
    handleRunTest,
    saveForm,
    publishForm,
    formatVersionDate,
    openVersionDetail,
    getVersionDiffInfo: (item: PromptVersionItem) =>
      getVersionDiffInfo(item, versionHistory.value),
    isCurrentVersion: checkIsCurrentVersion,
    handleDetailRollback,
    rollbackVersion,
    initEditor
  };
}
