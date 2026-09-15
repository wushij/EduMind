<template>
  <div class="prompt-list-page">
    <!-- 头部工程资产横幅与核心指标看板 (仿 AI 消耗明细高品质现代浅色渐变 UI) -->
    <div class="prompt-hero-card">
      <div class="hero-bg-glow hero-bg-glow--blue" />
      <div class="hero-bg-glow hero-bg-glow--purple" />

      <div class="hero-header-row">
        <div class="hero-title-area">
          <div class="hero-eyebrow">
            <span class="eyebrow-chip">EduMind AI Engineering</span>
            <span class="eyebrow-divider">/</span>
            <span class="eyebrow-sub">提示词工程资产中枢</span>
          </div>
          <h1 class="hero-title">Prompt 提示词模板中心</h1>
          <p class="hero-desc">
            统一纳管课程 RAG 问答、试题生成向导、主客观智能批阅及教学备课的高质量提示词资产，构建具备上下文注入、多轮会话、引用溯源与防注入屏障的生产级教学中枢。
          </p>
        </div>
        <div class="hero-action-area">
          <el-button
            type="primary"
            class="create-btn"
            :icon="Plus"
            @click="router.push('/system/prompts/editor')"
          >
            新建 Prompt 模板
          </el-button>
          <el-button
            round
            class="btn-refresh"
            :icon="Refresh"
            :loading="loading"
            @click="handleReload"
          >
            刷新资产
          </el-button>
        </div>
      </div>

      <!-- 4 大核心指标数据面板 -->
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-icon-box blue">
            <el-icon><CollectionTag /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">纳管工程模板</div>
            <div class="metric-value">{{ promptList.length }} <span class="unit">个</span></div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon-box green">
            <span class="online-pulse-dot"></span>
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">生产环境运行中</div>
            <div class="metric-value text-green">{{ publishedCount }} <span class="unit">项</span></div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon-box cyan">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">课程 RAG 赋能引擎</div>
            <div class="metric-value text-cyan">{{ ragCount }} <span class="unit">个</span></div>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon-box purple">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-label">动态参数插槽覆盖</div>
            <div class="metric-value text-purple">{{ totalVariablesCount }} <span class="unit">槽位</span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 过滤器与检索工具栏 (单行紧凑排版，坚决不换行) -->
    <div class="filter-bar-card">
      <div class="category-tabs">
        <button
          v-for="cat in categoryOptions"
          :key="cat.key"
          class="cat-tab-btn"
          :class="{ active: selectedCategory === cat.key }"
          @click="changeCategory(cat.key)"
        >
          <span class="tab-label">{{ cat.label }}</span>
          <span class="tab-badge" :class="cat.key">{{ getCategoryCount(cat.key) }}</span>
        </button>
      </div>

      <div class="filter-right-tools">
        <el-select
          v-model="selectedStatus"
          placeholder="全部状态"
          style="width: 110px"
          clearable
          @change="filterPrompts"
        >
          <el-option label="全部状态" value="" />
          <el-option label="运行中 (Online)" value="PUBLISHED" />
          <el-option label="草稿 (Draft)" value="DRAFT" />
        </el-select>

        <el-input
          v-model="searchKeyword"
          placeholder="按名称、编码或变量检索..."
          style="width: 220px"
          clearable
          :prefix-icon="Search"
        />
      </div>
    </div>

    <!-- 模板卡片网格 -->
    <div v-loading="loading" class="prompt-grid-container">
      <div v-if="filteredPrompts.length > 0" class="prompt-grid">
        <div
          v-for="item in filteredPrompts"
          :key="item.id"
          class="prompt-card"
          :class="[`theme-${item.category}`]"
        >
          <!-- 顶部彩色重点装饰条 -->
          <div class="card-accent-bar"></div>

          <!-- 卡片顶栏：编码、版本与运行状态 -->
          <div class="card-top-header">
            <div class="code-and-category">
              <span class="prompt-code" @click.stop="copyText(item.code, '模板编码已复制')">
                {{ item.code }}
                <el-icon class="copy-icon"><CopyDocument /></el-icon>
              </span>
              <span class="category-tag" :class="item.category">
                {{ getCategoryLabel(item.category) }}
              </span>
            </div>

            <div class="status-and-version">
              <span
                class="status-pill"
                :class="item.status === 'PUBLISHED' ? 'status-online' : 'status-draft'"
              >
                <span class="status-dot"></span>
                {{ item.status === 'PUBLISHED' ? '运行中' : '草稿' }}
              </span>
              <span class="version-pill">{{ item.version }}</span>
            </div>
          </div>

          <!-- 模板名称与简介 -->
          <div class="card-body">
            <h3 class="prompt-name" :title="item.name">{{ item.name }}</h3>
            <p class="prompt-desc" :title="item.description">
              {{ item.description || '暂未填写模板描述，包含标准系统人设与用户问答指令模板。' }}
            </p>

            <!-- 核心工程特性标签 (全矢量 Icon，摒弃粗糙 Emoji) -->
            <div class="feature-chips">
              <span v-if="item.category === 'rag'" class="feat-chip rag-feat">
                <el-icon class="feat-ic"><Search /></el-icon>
                知识库检索
              </span>
              <span v-if="item.category === 'rag'" class="feat-chip rag-feat">
                <el-icon class="feat-ic"><Link /></el-icon>
                引用溯源 S1/S2
              </span>
              <span v-if="item.category === 'rag'" class="feat-chip shield-feat">
                <el-icon class="feat-ic"><Lock /></el-icon>
                防注入防护
              </span>
              <span v-if="item.category === 'question'" class="feat-chip exam-feat">
                <el-icon class="feat-ic"><Document /></el-icon>
                JSON 结构化
              </span>
              <span v-if="item.category === 'grading'" class="feat-chip grade-feat">
                <el-icon class="feat-ic"><Finished /></el-icon>
                采分点核算
              </span>
              <span v-if="item.category === 'teaching'" class="feat-chip teach-feat">
                <el-icon class="feat-ic"><Tickets /></el-icon>
                高校规范教案
              </span>
            </div>
          </div>

          <!-- 卡片底栏：模型与长圆胶囊操作按钮 (纯粹单行优雅排版，不换行) -->
          <div class="card-footer">
            <div class="model-params">
              <span
                class="model-badge"
                :class="{ 'unset-model': isBoundModelUnset(item.boundModel) }"
                :title="getDisplayModelTooltip(item.boundModel)"
              >
                <el-icon><Cpu /></el-icon>
                <span class="model-name-text">{{ getDisplayModelName(item.boundModel) }}</span>
              </span>
            </div>

            <div class="card-actions">
              <el-button
                size="small"
                class="action-btn preview-btn"
                @click.stop="openDrawer(item, 'preview')"
              >
                预览
              </el-button>
              <el-button
                size="small"
                class="action-btn test-btn"
                @click.stop="openDrawer(item, 'test')"
              >
                热测试
              </el-button>
              <el-button
                size="small"
                type="primary"
                class="action-btn edit-btn"
                @click.stop="router.push(`/system/prompts/editor/${item.id}`)"
              >
                编辑
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态提示 -->
      <div v-else class="empty-state-card">
        <div class="empty-icon-wrap">
          <el-icon :size="48"><CollectionTag /></el-icon>
        </div>
        <h3>未检索到匹配的 Prompt 模板</h3>
        <p>当前分类下暂无符合检索条件的提示词工程资产，您可以重置筛选或新建模板。</p>
        <div class="empty-actions">
          <el-button @click="resetFilters">重置筛选条件</el-button>
          <el-button type="primary" :icon="Plus" @click="router.push('/system/prompts/editor')">
            新建 Prompt 模板
          </el-button>
        </div>
      </div>
    </div>

    <!-- 侧边栏：快速预览与热测试工作台抽屉 (Live Playground) -->
    <el-drawer
      v-model="drawerVisible"
      :size="'58%'"
      destroy-on-close
      class="prompt-quick-drawer"
    >
      <template #header>
        <div v-if="activeItem" class="drawer-header-content">
          <div class="dh-top">
            <span class="dh-code">{{ activeItem.code }}</span>
            <span class="dh-version">{{ activeItem.version }}</span>
            <span class="dh-cat" :class="activeItem.category">
              {{ getCategoryLabel(activeItem.category) }}
            </span>
          </div>
          <h2 class="dh-title">{{ activeItem.name }}</h2>
          <p class="dh-desc">{{ activeItem.description }}</p>
        </div>
      </template>

      <div v-if="activeItem" class="drawer-body-wrap">
        <el-tabs v-model="drawerActiveTab" class="drawer-tabs">
          <!-- 选项卡 1：提示词工程规范 -->
          <el-tab-pane label="提示词工程规范" name="preview">
            <div class="spec-tab-content">
              <!-- 模型绑定与调参参数卡片 -->
              <div class="param-summary-card">
                <div class="p-item">
                  <span class="k">绑定模型</span>
                  <span class="v model-val">{{ activeItem.boundModel || '未指定模型（跟随系统网关）' }}</span>
                </div>
                <div class="p-item">
                  <span class="k">采样温度 (Temperature)</span>
                  <span class="v">{{ activeItem.temperature ?? 0.3 }}</span>
                </div>
                <div class="p-item">
                  <span class="k">最大 Token 限制</span>
                  <span class="v">{{ activeItem.maxTokens ?? 8000 }} Tokens</span>
                </div>
                <div class="p-item">
                  <span class="k">所属场景分类</span>
                  <span class="v">{{ getCategoryLabel(activeItem.category) }}</span>
                </div>
              </div>

              <!-- System Prompt 区域 -->
              <div class="prompt-block">
                <div class="block-header">
                  <span class="b-title">System Prompt (系统人设与硬性约束规则)</span>
                  <el-button
                    size="small"
                    link
                    :icon="CopyDocument"
                    @click="copyText(activeItem.systemPrompt, 'System Prompt 已复制')"
                  >
                    复制内容
                  </el-button>
                </div>
                <div class="code-view-box system-box">
                  <pre>{{ activeItem.systemPrompt || '（未单独设置 System Prompt，使用系统默认人设）' }}</pre>
                </div>
              </div>

              <!-- User Prompt Template 区域 -->
              <div class="prompt-block">
                <div class="block-header">
                  <span class="b-title">User Prompt Template (用户指令插槽模板)</span>
                  <el-button
                    size="small"
                    link
                    :icon="CopyDocument"
                    @click="copyText(activeItem.userPromptTemplate, 'User Prompt 模板已复制')"
                  >
                    复制内容
                  </el-button>
                </div>
                <div class="code-view-box user-box">
                  <pre>{{ activeItem.userPromptTemplate }}</pre>
                </div>
              </div>

              <!-- 变量插槽明细清单 -->
              <div class="prompt-block">
                <div class="block-header">
                  <span class="b-title">动态注入参数插槽清单 ({{ activeItem.variables.length }} 个)</span>
                </div>
                <div class="vars-table-card">
                  <div class="var-table-header">
                    <span class="col-name">变量标识</span>
                    <span class="col-label">中文语义</span>
                    <span class="col-sample">默认示例</span>
                  </div>
                  <div
                    v-for="v in activeItem.variables"
                    :key="v.name"
                    class="var-table-row"
                  >
                    <span class="col-name">&#123;&#123;{{ v.name }}&#125;&#125;</span>
                    <span class="col-label">{{ v.label || v.name }}</span>
                    <span class="col-sample">{{ v.defaultValue || '—' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 选项卡 2：实时热测试演练台 -->
          <el-tab-pane label="在线热测试演练 (Live Playground)" name="test">
            <div class="playground-tab-content">
              <!-- 参数填报表单 -->
              <div class="test-inputs-panel">
                <div class="panel-head">
                  <span class="panel-title">1. 动态入参模拟填充</span>
                  <el-button size="small" link @click="populateDummyVariables">
                    一键填入高校课程示例数据
                  </el-button>
                </div>

                <div class="vars-inputs-grid">
                  <div
                    v-for="v in activeItem.variables"
                    :key="v.name"
                    class="input-group"
                  >
                    <label class="group-label">
                      &#123;&#123;{{ v.name }}&#125;&#125;
                      <span class="sub-label">({{ v.label || v.name }})</span>
                    </label>
                    <el-input
                      v-model="testVariables[v.name]"
                      :type="v.name.includes('context') || v.name.includes('history') ? 'textarea' : 'text'"
                      :rows="v.name.includes('context') ? 4 : 2"
                      :placeholder="`请输入 ${v.label || v.name}...`"
                    />
                  </div>
                </div>

                <div class="run-test-bar">
                  <el-button
                    type="primary"
                    :icon="VideoPlay"
                    :loading="testing"
                    class="run-btn"
                    @click="executePlaygroundTest"
                  >
                    {{ testing ? '模型思考推理中...' : '立即运行 Prompt 热测试' }}
                  </el-button>
                </div>
              </div>

              <!-- 渲染后 Prompt 与推理结果 -->
              <div v-if="testResultOutput || testing" class="test-output-panel">
                <div class="panel-head">
                  <span class="panel-title">2. 模型输出与溯源结果</span>
                  <span v-if="testDuration > 0" class="meta-tag">
                    耗时: {{ testDuration }}ms
                  </span>
                </div>

                <div v-loading="testing" class="output-content-card">
                  <PromptLlmOutput v-if="testResultOutput" :content="testResultOutput" />
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <div class="drawer-footer-actions">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button
            type="primary"
            @click="router.push(`/system/prompts/editor/${activeItem?.id}`)"
          >
            前往完整编辑器配置 →
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePrompt } from '@/composables/system/usePrompt';
import PromptLlmOutput from '@/components/system/PromptLlmOutput.vue';
import { PromptTemplate } from '@/types/system/prompt';
import {
  Plus,
  Search,
  Refresh,
  RefreshRight,
  CollectionTag,
  Cpu,
  ChatDotRound,
  DataAnalysis,
  CopyDocument,
  VideoPlay,
  Link,
  Lock,
  Document,
  Finished,
  Tickets
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { fetchModels } from '@/api/system/model';
import { AIModelConfigItem } from '@/types/system/model';

const router = useRouter();
const selectedCategory = ref('ALL');
const selectedStatus = ref('');
const searchKeyword = ref('');

const { loading, testing, promptList, fetchPrompts, runTest, testResult } = usePrompt();

// 平台真实模型配置状态
const availableModels = ref<AIModelConfigItem[]>([]);
const defaultModelName = ref<string>('');

const loadAvailableModels = async () => {
  try {
    const list = await fetchModels();
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

// 格式化卡片上展示的模型名称 (若未指定，展示跟随网关默认模型名称，如 Flash)
const getDisplayModelName = (boundModel?: string | null) => {
  if (!boundModel || boundModel.trim() === '' || boundModel === 'deepseek-chat') {
    return defaultModelName.value ? `默认: ${defaultModelName.value}` : '系统默认网关';
  }
  const matched = availableModels.value.find(
    (m) => m.name === boundModel || m.modelName === boundModel || (m as any).modelKey === boundModel
  );
  if (matched) {
    return matched.name || matched.modelName;
  }
  return boundModel;
};

// 鼠标悬停详细信息
const getDisplayModelTooltip = (boundModel?: string | null) => {
  if (!boundModel || boundModel.trim() === '' || boundModel === 'deepseek-chat') {
    return defaultModelName.value
      ? `未绑定独立模型，运行时自适应调度全平台默认模型：${defaultModelName.value}`
      : '未绑定独立模型，运行时跟随系统默认网关模型';
  }
  const matched = availableModels.value.find(
    (m) => m.name === boundModel || m.modelName === boundModel || (m as any).modelKey === boundModel
  );
  if (matched) {
    return `当前绑定专用模型：${matched.name} (${matched.modelName}) · ${matched.provider.toUpperCase()}`;
  }
  return `当前绑定模型：${boundModel}`;
};

// 是否处于未绑定/跟随系统默认状态
const isBoundModelUnset = (boundModel?: string | null) => {
  return !boundModel || boundModel.trim() === '' || boundModel === 'deepseek-chat';
};

// 分类项定义 (精炼标签文案，保证单行完整呈现)
const categoryOptions = [
  { key: 'ALL', label: '全部模板' },
  { key: 'rag', label: '课程问答 RAG' },
  { key: 'question', label: '智能命题' },
  { key: 'grading', label: '智能批改' },
  { key: 'teaching', label: '教案备课' }
];

// 计算核心统计数据
const publishedCount = computed(() => {
  return promptList.value.filter((p) => p.status === 'PUBLISHED').length;
});

const ragCount = computed(() => {
  return promptList.value.filter((p) => p.category === 'rag').length;
});

const totalVariablesCount = computed(() => {
  return promptList.value.reduce((acc, p) => acc + (p.variables?.length || 0), 0);
});

// 计算各分类计数
const getCategoryCount = (catKey: string) => {
  if (catKey === 'ALL') return promptList.value.length;
  return promptList.value.filter((p) => p.category === catKey).length;
};

// 翻译分类
const getCategoryLabel = (category: string) => {
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
};

// 客户端组合检索与过滤
const filteredPrompts = computed(() => {
  let list = promptList.value;

  // 分类过滤（如果不是 ALL）
  if (selectedCategory.value && selectedCategory.value !== 'ALL') {
    list = list.filter((p) => p.category === selectedCategory.value);
  }

  // 状态过滤
  if (selectedStatus.value) {
    list = list.filter((p) => p.status === selectedStatus.value);
  }

  // 关键字过滤（名称、编码、描述、变量名）
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim();
    list = list.filter((p) => {
      const matchName = p.name.toLowerCase().includes(kw);
      const matchCode = p.code.toLowerCase().includes(kw);
      const matchDesc = p.description?.toLowerCase().includes(kw);
      const matchVar = p.variables?.some((v) => v.name.toLowerCase().includes(kw));
      return matchName || matchCode || matchDesc || matchVar;
    });
  }

  return list;
});

// 切换分类（纯客户端响应式过滤，不破坏顶部全量资产统计徽章）
const changeCategory = (catKey: string) => {
  selectedCategory.value = catKey;
};

// 状态过滤触发
const filterPrompts = () => {
  // filteredPrompts 是计算属性，自动生效
};

// 重置筛选
const resetFilters = () => {
  selectedCategory.value = 'ALL';
  selectedStatus.value = '';
  searchKeyword.value = '';
  fetchPrompts();
};

// 手动刷新（拉取最新全量提示词资产）
const handleReload = () => {
  fetchPrompts();
  ElMessage.success('提示词资产已更新');
};

// 快捷复制
const copyText = (text: string, msg = '已复制到剪贴板') => {
  if (!text) return;
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(msg);
  }).catch(() => {
    ElMessage.info('复制异常，请手动选中复制');
  });
};

// 快速抽屉控制
const drawerVisible = ref(false);
const drawerActiveTab = ref<'preview' | 'test'>('preview');
const activeItem = ref<PromptTemplate | null>(null);
const testVariables = ref<Record<string, string>>({});
const testResultOutput = ref('');
const testDuration = ref(0);

const openDrawer = (item: PromptTemplate, tab: 'preview' | 'test') => {
  activeItem.value = item;
  drawerActiveTab.value = tab;
  testVariables.value = {};
  testResultOutput.value = '';
  testDuration.value = 0;

  // 预填充默认变量
  item.variables.forEach((v) => {
    testVariables.value[v.name] = v.defaultValue || '';
  });

  drawerVisible.value = true;
};

// 一键填入高校课程示例数据
const populateDummyVariables = () => {
  if (!activeItem.value) return;
  const isRag = activeItem.value.category === 'rag';
  const dummy: Record<string, string> = {
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
    generation_requirements: 'knowledgePointId 必须使用 KP003/KP004 等系统 ID，不得写中文名；chapterName 与输入完全一致；单选题干扰项需包含编译期/运行期或重载/重写混淆',
    existing_questions: '["什么是多态？","Java中单继承关键字是什么？"]',
    question_stem: '简述TCP与UDP的核心区别',
    standard_answer: 'TCP面向连接、可靠交付；UDP无连接、最大努力交付。',
    max_score: '10',
    student_answer: 'TCP连接前要三次握手，可靠传输；UDP不建连接，速度快。',
    teaching_hours: '2',
    target_students: '计算机专业大二学生',
    // 教案备课（RAG）通用扩展变量
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

  activeItem.value.variables.forEach((v) => {
    if (dummy[v.name]) {
      testVariables.value[v.name] = dummy[v.name];
    }
  });
  ElMessage.success('已自动载入课程真实测试变量');
};

// 执行热测试
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
  } else if (activeItem.value.category === 'grading' || activeItem.value.code === 'GRADING_RAG_GENERAL') {
    testResultOutput.value = `{
  "status": "SUCCESS",
  "questionId": "${testVariables.value.question_id || 'Q10001'}",
  "questionType": "${testVariables.value.question_type || 'SHORT_ANSWER'}",
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
  } else if (activeItem.value.category === 'question' || activeItem.value.code === 'EXAM_RAG_GENERAL') {
    testResultOutput.value = `{
  "status": "SUCCESS",
  "courseId": "${testVariables.value.course_id || '10001'}",
  "courseName": "${testVariables.value.course_name || 'Java程序设计'}",
  "chapterId": "${testVariables.value.chapter_id || 'CH03'}",
  "chapterName": "${testVariables.value.chapter_name || '第三章 面向对象程序设计'}",
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
  } else if (activeItem.value.category === 'teaching' || activeItem.value.code === 'LESSON_PREP_RAG_GENERAL') {
    testResultOutput.value = `{
  "status": "SUCCESS",
  "groundingStatus": "FULL",
  "requiresTeacherReview": false,
  "reviewReason": null,
  "lessonPlan": {
    "basicInfo": {
      "courseId": "${testVariables.value.course_id || '10001'}",
      "courseName": "${testVariables.value.course_name || 'Java程序设计'}",
      "chapterId": "${testVariables.value.chapter_id || 'CH03'}",
      "chapterName": "${testVariables.value.chapter_name || '第三章 面向对象程序设计'}",
      "lessonTitle": "${testVariables.value.lesson_title || 'Java运行时多态与动态绑定机制'}",
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
  } else {
    // 降级回显模拟课程 RAG 解答输出
    testResultOutput.value = `### 多态的核心原理与实现机制

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
};

onMounted(async () => {
  await Promise.all([fetchPrompts(), loadAvailableModels()]);
});
</script>

<style scoped lang="scss">
.prompt-list-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 100%;

  // 顶部英雄卡片 (高品质现代浅色渐变与光晕，对标个人 AI 消耗明细设计规范)
  .prompt-hero-card {
    position: relative;
    overflow: hidden;
    background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFF 55%, #F5F3FF 100%);
    border: 1px solid #E2E8F0;
    border-radius: 20px;
    padding: 24px 28px 20px;
    box-shadow: 0 8px 32px rgba(22, 119, 255, 0.05);

    .hero-bg-glow {
      position: absolute;
      border-radius: 50%;
      pointer-events: none;
      filter: blur(60px);
      opacity: 0.35;

      &--blue {
        width: 240px;
        height: 240px;
        background: #BFDBFE;
        top: -90px;
        right: 140px;
      }

      &--purple {
        width: 180px;
        height: 180px;
        background: #DDD6FE;
        bottom: -70px;
        left: 50px;
      }
    }

    .hero-header-row {
      position: relative;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
      z-index: 1;

      .hero-title-area {
        max-width: 820px;

        .hero-eyebrow {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.06em;
          text-transform: uppercase;
          color: #1677FF;
          background: rgba(22, 119, 255, 0.08);
          border: 1px solid rgba(22, 119, 255, 0.15);
          padding: 3px 12px;
          border-radius: 9999px;
          margin-bottom: 8px;

          .eyebrow-chip {
            color: #1677FF;
            font-weight: 700;
          }
          .eyebrow-divider {
            color: #93C5FD;
          }
          .eyebrow-sub {
            color: #2563EB;
          }
        }

        .hero-title {
          margin: 0 0 6px 0;
          font-size: 23px;
          font-weight: 800;
          letter-spacing: -0.02em;
          color: #0F172A;
        }

        .hero-desc {
          margin: 0;
          font-size: 13px;
          line-height: 1.55;
          color: #64748B;
        }
      }

      .hero-action-area {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-shrink: 0;

        .create-btn {
          background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
          border: none;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.28);
          font-weight: 600;
          padding: 8px 18px;
          border-radius: 999px !important;
          color: #FFFFFF;
          transition: background-color 0.15s ease, box-shadow 0.15s ease;

          &:hover {
            box-shadow: 0 6px 16px rgba(37, 99, 235, 0.38);
          }

          &:active {
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
          }
        }

        .refresh-btn {
          background: #FFFFFF;
          border: 1px solid #BFDBFE;
          color: #2563EB;
          border-radius: 999px !important;
          padding: 8px 16px;
          font-weight: 600;
          box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);

          &:hover {
            background: #EFF6FF;
            border-color: #93C5FD;
            color: #1D4ED8;
          }

          &:active {
            background: #DBEAFE;
          }
        }
      }
    }

    // 指标看板
    .metrics-grid {
      position: relative;
      z-index: 1;
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 14px;
      margin-top: 18px;
      padding-top: 16px;
      border-top: 1px solid rgba(226, 232, 240, 0.8);

      @media (max-width: 900px) {
        grid-template-columns: repeat(2, 1fr);
      }

      .metric-card {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 14px;
        padding: 12px 16px;
        display: flex;
        align-items: center;
        gap: 12px;
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
        transition: border-color 0.2s ease, box-shadow 0.2s ease;

        &:hover {
          border-color: #93C5FD;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);
        }

        .metric-icon-box {
          width: 38px;
          height: 38px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
          position: relative;
          flex-shrink: 0;

          &.blue {
            background: #EFF6FF;
            color: #2563EB;
            border: 1px solid #DBEAFE;
          }
          &.green {
            background: #ECFDF5;
            color: #059669;
            border: 1px solid #A7F3D0;
          }
          &.cyan {
            background: #F0F9FF;
            color: #0284C7;
            border: 1px solid #BAE6FD;
          }
          &.purple {
            background: #FAF5FF;
            color: #9333EA;
            border: 1px solid #E9D5FF;
          }

          .online-pulse-dot {
            position: absolute;
            top: 2px;
            right: 2px;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #10B981;
            box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.35);
            animation: pulseGlow 2s infinite;
          }
        }

        .metric-content {
          display: flex;
          flex-direction: column;

          .metric-label {
            font-size: 12px;
            color: #64748B;
            font-weight: 500;
          }

          .metric-value {
            font-size: 19px;
            font-weight: 800;
            color: #0F172A;
            margin-top: 1px;
            letter-spacing: -0.01em;

            .unit {
              font-size: 11px;
              color: #94A3B8;
              font-weight: normal;
              margin-left: 2px;
            }

            &.text-green {
              color: #059669;
            }
            &.text-cyan {
              color: #0284C7;
            }
            &.text-purple {
              color: #7C3AED;
            }
          }
        }
      }
    }
  }

  // 过滤器与工具条 (单行绝对禁止换行)
  .filter-bar-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    padding: 8px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    flex-wrap: nowrap !important; // 坚决不换行！
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);

    .category-tabs {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-wrap: nowrap !important; // 坚决不换行！
      flex-shrink: 1;
      min-width: 0;
      overflow-x: auto;
      scrollbar-width: none;
      &::-webkit-scrollbar {
        display: none;
      }

      .cat-tab-btn {
        background: transparent;
        border: none;
        outline: none;
        cursor: pointer;
        padding: 5px 11px;
        border-radius: 8px;
        font-size: 13px;
        font-weight: 500;
        color: #475569;
        display: flex;
        align-items: center;
        gap: 6px;
        white-space: nowrap !important; // 坚决不换行！
        flex-shrink: 0;
        transition: all 0.15s ease;

        &:hover {
          background: #F1F5F9;
          color: #1E293B;
        }

        &.active {
          background: #EFF6FF;
          color: #2563EB;
          font-weight: 600;

          .tab-badge {
            background: #2563EB;
            color: #FFFFFF;
          }
        }

        .tab-badge {
          font-size: 11px;
          padding: 1px 6px;
          border-radius: 10px;
          background: #E2E8F0;
          color: #64748B;
          font-family: ui-monospace, monospace;
          transition: all 0.2s;
        }
      }
    }

    .filter-right-tools {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
      flex-wrap: nowrap !important; // 坚决不换行！
    }
  }

  // 模板卡片网格
  .prompt-grid-container {
    min-height: 420px;

    .prompt-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 18px;

      @media (max-width: 1300px) {
        grid-template-columns: repeat(2, 1fr);
      }
      @media (max-width: 800px) {
        grid-template-columns: 1fr;
      }
    }

    .prompt-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      display: flex;
      flex-direction: column;
      position: relative;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 8px 24px -4px rgba(37, 99, 235, 0.12), 0 4px 8px -2px rgba(37, 99, 235, 0.04);
      }

      // 顶部根据业务领域的彩色强调条
      .card-accent-bar {
        height: 3px;
        width: 100%;
        background: linear-gradient(90deg, #3B82F6, #60A5FA);
      }
      &.theme-rag .card-accent-bar {
        background: linear-gradient(90deg, #0284C7, #06B6D4);
      }
      &.theme-question .card-accent-bar {
        background: linear-gradient(90deg, #F59E0B, #FBBF24);
      }
      &.theme-grading .card-accent-bar {
        background: linear-gradient(90deg, #8B5CF6, #A855F7);
      }
      &.theme-teaching .card-accent-bar {
        background: linear-gradient(90deg, #10B981, #34D399);
      }

      // 卡片头部
      .card-top-header {
        padding: 14px 18px 8px 18px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .code-and-category {
          display: flex;
          align-items: center;
          gap: 8px;

          .prompt-code {
            font-family: ui-monospace, SFMono-Regular, monospace;
            font-size: 11px;
            font-weight: 700;
            color: #1D4ED8;
            background: #EFF6FF;
            padding: 2px 8px;
            border-radius: 999px;
            display: flex;
            align-items: center;
            gap: 4px;
            cursor: pointer;
            transition: all 0.15s;

            &:hover {
              background: #DBEAFE;
              color: #1E40AF;
            }

            .copy-icon {
              font-size: 11px;
            }
          }

          .category-tag {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 999px;

            &.rag {
              background: #E0F2FE;
              color: #0369A1;
            }
            &.question {
              background: #FEF3C7;
              color: #B45309;
            }
            &.grading {
              background: #F3E8FF;
              color: #7E22CE;
            }
            &.teaching {
              background: #DCFCE7;
              color: #15803D;
            }
          }
        }

        .status-and-version {
          display: flex;
          align-items: center;
          gap: 6px;

          .status-pill {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 999px;
            display: flex;
            align-items: center;
            gap: 4px;

            &.status-online {
              background: #ECFDF5;
              color: #059669;
              .status-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #10B981;
              }
            }
            &.status-draft {
              background: #FFFBEB;
              color: #D97706;
              .status-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #F59E0B;
              }
            }
          }

          .version-pill {
            font-family: ui-monospace, monospace;
            font-size: 11px;
            color: #64748B;
            background: #F1F5F9;
            padding: 2px 8px;
            border-radius: 999px;
          }
        }
      }

      // 卡片主体
      .card-body {
        padding: 6px 18px 14px 18px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        flex: 1;

        .prompt-name {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
          transition: color 0.15s;
        }

        .prompt-desc {
          margin: 0;
          font-size: 12.5px;
          color: #64748B;
          line-height: 1.55;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
          min-height: 38px;
        }

        // 工程特性徽标
        .feature-chips {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;

          .feat-chip {
            font-size: 11px;
            padding: 3px 10px;
            border-radius: 999px !important;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            line-height: 1.4;

            .feat-ic {
              font-size: 12px;
            }

            &.rag-feat {
              background: #F0F9FF;
              color: #0284C7;
              border: 1px solid #BAE6FD;
            }
            &.shield-feat {
              background: #FEF2F2;
              color: #DC2626;
              border: 1px solid #FECACA;
            }
            &.exam-feat {
              background: #FFFBEB;
              color: #D97706;
              border: 1px solid #FDE68A;
            }
            &.grade-feat {
              background: #FAF5FF;
              color: #9333EA;
              border: 1px solid #E9D5FF;
            }
            &.teach-feat {
              background: #F0FDF4;
              color: #16A34A;
              border: 1px solid #BBF7D0;
            }
          }
        }
      }

      // 卡片底栏 (防挤压单行优雅排版，点击绝对静止无抖动)
      .card-footer {
        padding: 10px 16px;
        background: #F8FAFC;
        border-top: 1px solid #F1F5F9;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;

        .model-params {
          display: flex;
          align-items: center;
          gap: 6px;
          min-width: 0;
          flex-shrink: 1;

          .model-badge {
            font-size: 11.5px;
            color: #334155;
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            padding: 2px 8px;
            border-radius: 999px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            white-space: nowrap;
            max-width: 140px;
            box-sizing: border-box;

            .model-name-text {
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .el-icon {
              font-size: 13px;
              color: #64748B;
              flex-shrink: 0;
            }

            &.unset-model {
              color: #0369A1;
              background: #F0F9FF;
              border-color: #BAE6FD;

              .el-icon {
                color: #0284C7;
              }
            }
          }
        }

        .card-actions {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-shrink: 0;

          .action-btn {
            font-size: 12px;
            font-weight: 500;
            padding: 0 11px !important;
            border-radius: 999px !important;
            height: 28px !important;
            line-height: 26px !important;
            box-sizing: border-box !important;
            white-space: nowrap !important;
            user-select: none;
            cursor: pointer;
            outline: none !important;
            transform: none !important; // 彻底禁止 transform 位移导致的抖动
            transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;

            &:active,
            &:focus,
            &:focus-visible {
              outline: none !important;
              transform: none !important;
            }

            &.preview-btn {
              background: #FFFFFF !important;
              border: 1px solid #D1D5DB !important;
              color: #475569 !important;

              &:hover {
                background: #F3F4F6 !important;
                border-color: #9CA3AF !important;
                color: #111827 !important;
              }

              &:active {
                background: #E5E7EB !important;
                border-color: #6B7280 !important;
              }
            }

            &.test-btn {
              background: #EFF6FF !important;
              border: 1px solid #BFDBFE !important;
              color: #2563EB !important;
              font-weight: 600 !important;

              &:hover {
                background: #DBEAFE !important;
                border-color: #93C5FD !important;
                color: #1D4ED8 !important;
              }

              &:active {
                background: #BFDBFE !important;
                border-color: #60A5FA !important;
              }
            }

            &.edit-btn {
              background: #2563EB !important;
              border: 1px solid #2563EB !important;
              color: #FFFFFF !important;
              font-weight: 600 !important;
              box-shadow: 0 1px 3px rgba(37, 99, 235, 0.2) !important;

              &:hover {
                background: #1D4ED8 !important;
                border-color: #1D4ED8 !important;
                box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3) !important;
                transform: none !important;
              }

              &:active {
                background: #1E40AF !important;
                border-color: #1E40AF !important;
                transform: none !important;
              }
            }
          }
        }
      }
    }

    // 空状态卡片
    .empty-state-card {
      background: #FFFFFF;
      border: 1px dashed #CBD5E1;
      border-radius: 16px;
      padding: 60px 20px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .empty-icon-wrap {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: #F1F5F9;
        color: #94A3B8;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;
      }

      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 700;
        color: #1E293B;
      }

      p {
        margin: 0 0 20px 0;
        font-size: 13.5px;
        color: #64748B;
        max-width: 420px;
      }

      .empty-actions {
        display: flex;
        gap: 12px;
      }
    }
  }

  // 抽屉样式
  .prompt-quick-drawer {
    .drawer-header-content {
      .dh-top {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;

        .dh-code {
          font-family: ui-monospace, monospace;
          font-size: 12px;
          font-weight: 700;
          color: #2563EB;
          background: #EFF6FF;
          padding: 2px 10px;
          border-radius: 999px;
        }
        .dh-version {
          font-size: 12px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 8px;
          border-radius: 999px;
        }
        .dh-cat {
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 999px;
          font-weight: 600;

          &.rag {
            background: #E0F2FE;
            color: #0369A1;
          }
          &.question {
            background: #FEF3C7;
            color: #B45309;
          }
          &.grading {
            background: #F3E8FF;
            color: #7E22CE;
          }
          &.teaching {
            background: #DCFCE7;
            color: #15803D;
          }
        }
      }

      .dh-title {
        margin: 0 0 6px 0;
        font-size: 20px;
        font-weight: 800;
        color: #0F172A;
      }

      .dh-desc {
        margin: 0;
        font-size: 13px;
        color: #64748B;
        line-height: 1.5;
      }
    }

    .drawer-body-wrap {
      .drawer-tabs {
        :deep(.el-tabs__item) {
          font-size: 14px;
          font-weight: 600;
        }
      }

      .spec-tab-content {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .param-summary-card {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 10px;
          padding: 12px 18px;
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 12px;

          .p-item {
            display: flex;
            flex-direction: column;
            gap: 3px;

            .k {
              font-size: 11.5px;
              color: #64748B;
            }
            .v {
              font-size: 13.5px;
              font-weight: 600;
              color: #0F172A;

              &.model-val {
                font-family: ui-monospace, monospace;
                color: #2563EB;
              }
            }
          }
        }

        .prompt-block {
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          border-radius: 10px;
          padding: 14px 16px;

          .block-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .b-title {
              font-size: 13.5px;
              font-weight: 700;
              color: #1E293B;
            }
          }

          .code-view-box {
            background: #0F172A;
            border-radius: 8px;
            padding: 12px 14px;
            max-height: 280px;
            overflow-y: auto;

            pre {
              margin: 0;
              font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
              font-size: 12px;
              line-height: 1.6;
              color: #E2E8F0;
              white-space: pre-wrap;
              word-break: break-word;
            }

            &.user-box {
              background: #1E293B;
              pre {
                color: #93C5FD;
              }
            }
          }

          .vars-table-card {
            border: 1px solid #F1F5F9;
            border-radius: 8px;
            overflow: hidden;

            .var-table-header {
              display: grid;
              grid-template-columns: 180px 140px 1fr;
              padding: 8px 12px;
              background: #F8FAFC;
              font-size: 12px;
              font-weight: 600;
              color: #64748B;
            }

            .var-table-row {
              display: grid;
              grid-template-columns: 180px 140px 1fr;
              padding: 8px 12px;
              border-top: 1px solid #F1F5F9;
              font-size: 12.5px;
              align-items: center;

              .col-name {
                font-family: ui-monospace, monospace;
                font-weight: 600;
                color: #2563EB;
              }
              .col-label {
                color: #475569;
              }
              .col-sample {
                color: #64748B;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }
            }
          }
        }
      }

      .playground-tab-content {
        display: flex;
        flex-direction: column;
        gap: 18px;

        .test-inputs-panel {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 12px;
          padding: 16px;

          .panel-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 14px;

            .panel-title {
              font-size: 14px;
              font-weight: 700;
              color: #0F172A;
            }
          }

          .vars-inputs-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 12px;

            @media (max-width: 800px) {
              grid-template-columns: 1fr;
            }

            .input-group {
              display: flex;
              flex-direction: column;
              gap: 4px;

              .group-label {
                font-family: ui-monospace, monospace;
                font-size: 12px;
                font-weight: 600;
                color: #1E293B;

                .sub-label {
                  font-family: sans-serif;
                  font-weight: normal;
                  color: #64748B;
                  margin-left: 4px;
                }
              }
            }
          }

          .run-test-bar {
            margin-top: 16px;
            display: flex;
            justify-content: flex-end;

            .run-btn {
              padding: 10px 22px;
              font-weight: 600;
            }
          }
        }

        .test-output-panel {
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          border-radius: 12px;
          padding: 16px;

          .panel-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 12px;

            .panel-title {
              font-size: 14px;
              font-weight: 700;
              color: #0F172A;
            }

            .meta-tag {
              font-size: 11.5px;
              color: #64748B;
              background: #F1F5F9;
              padding: 2px 8px;
              border-radius: 4px;
            }
          }

          .output-content-card {
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            padding: 16px;
            min-height: 180px;
          }
        }
      }
    }

    .drawer-footer-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
  }
}

// 悬浮列表样式
.tooltip-vars-list {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .tip-v {
    font-family: ui-monospace, monospace;
    font-size: 11px;
    color: #E2E8F0;
  }
}

// 呼吸发光动画
@keyframes pulseGlow {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}
</style>
