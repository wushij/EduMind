<template>
  <div class="course-ai-page-wrapper">
    <!-- 1. 顶部专属视觉大 Banner (对齐 docs/课程ai助手的banner.png 与原型图) -->
    <div class="course-ai-hero-banner">
      <div class="banner-overlay-content">
        <div class="hero-left-section">
          <div class="hero-header-row">
            <!-- 渐变立体图标盒 -->
            <div class="hero-icon-squircle">
              <img class="hero-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
            </div>
            <div class="hero-titles">
              <h2 class="hero-course-title">
                {{ course?.title || 'Java程序设计' }} · AI 助手
              </h2>
              <p class="hero-course-desc">
                基于课程知识库，为你提供专业、准确、个性化的学习支持
              </p>
            </div>
          </div>

          <!-- 5 个核心功能长圆跑道胶囊按钮 (原型同款) -->
          <div class="hero-capsules-row">
            <button
              v-for="pill in quickActionPills"
              :key="pill.title"
              type="button"
              class="hero-action-pill"
              @click="handlePillClick(pill)"
            >
              <el-icon class="pill-icon" :style="{ color: pill.color }">
                <component :is="pill.icon" />
              </el-icon>
              <span class="pill-text">{{ pill.title }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 原型同款三栏式教学工作台 (左: 课程章节 | 中: AI 问答主工作台 | 右: 辅助学习面板) -->
    <div class="course-ai-grid-workbench">
      <!-- 左栏：课程章节 (240px) -->
      <aside class="workbench-col workbench-col--chapters">
        <div class="col-card chapters-card">
          <div class="col-card-header">
            <span class="header-title">课程章节</span>
            <span class="count-badge">{{ chaptersData.length }}章</span>
          </div>

          <!-- 章节搜索框 -->
          <div class="chapters-search-box">
            <el-input
              v-model="chapterKeyword"
              placeholder="搜索章节内容..."
              size="small"
              clearable
              class="chapter-search-input"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 章节可折叠树列表 -->
          <div class="chapters-scroll-area">
            <div
              v-for="chapter in filteredChapters"
              :key="chapter.id"
              class="chapter-group-item"
            >
              <div
                class="chapter-header-row"
                @click="chapter.expanded = !chapter.expanded"
              >
                <el-icon class="arrow-icon">
                  <ArrowDown v-if="chapter.expanded" />
                  <ArrowRight v-else />
                </el-icon>
                <span class="chapter-label" :title="chapter.title">{{ chapter.title }}</span>
              </div>

              <!-- 二级节列表 -->
              <div v-show="chapter.expanded" class="sections-sub-list">
                <div
                  v-for="sec in chapter.sections"
                  :key="sec.id"
                  class="section-leaf-row"
                  :class="{ active: activeSectionId === sec.id }"
                  @click="selectSection(sec)"
                >
                  <span class="section-dot"></span>
                  <span class="section-title" :title="sec.title">{{ sec.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中栏：AI 问答主工作台 (Flex 1) -->
      <main class="workbench-col workbench-col--chat">
        <div class="col-card chat-workbench-card">
          <!-- 工作台顶栏：模式切换、模型选择、历史记录 -->
          <div class="chat-top-toolbar">
            <div class="mode-capsule-tabs">
              <button
                v-for="tab in modeTabs"
                :key="tab.key"
                type="button"
                class="mode-pill-tab"
                :class="{ active: currentModeTab === tab.key }"
                @click="switchModeTab(tab.key)"
              >
                {{ tab.label }}
              </button>
            </div>

            <div class="toolbar-right-actions">
              <!-- 大模型选择器下拉框 -->
              <el-dropdown trigger="click" @command="handleModelSelect">
                <button type="button" class="toolbar-pill-btn model-selector-btn">
                  <span class="model-name">{{ currentModel }}</span>
                  <el-icon class="arrow"><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu class="model-dropdown-menu">
                    <el-dropdown-item
                      v-for="m in modelOptions"
                      :key="m.key"
                      :command="m.key"
                      :class="{ 'is-selected': currentModelKey === m.key }"
                    >
                      <div class="model-item-row">
                        <span class="item-name">{{ m.name }}</span>
                        <span class="item-badge">{{ m.desc }}</span>
                      </div>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- 历史记录抽屉触发按钮 -->
              <button
                type="button"
                class="toolbar-pill-btn history-trigger-btn"
                @click="historyDrawerVisible = true"
              >
                <el-icon><Clock /></el-icon>
                <span>历史记录</span>
              </button>
            </div>
          </div>

          <!-- 消息流展示滚动区 -->
          <div ref="messagesScrollRef" class="messages-flow-scroll">
            <!-- 章节锚定上下文小浮条 -->
            <div v-if="activeSectionTitle" class="context-anchor-chip">
              <el-icon class="pin-icon"><Connection /></el-icon>
              <span>当前知识锚定章节：<strong>{{ activeSectionTitle }}</strong></span>
              <button type="button" class="clear-anchor-btn" @click="activeSectionId = null; activeChapterId = undefined; activeSectionTitle = ''">
                <el-icon><Close /></el-icon>
              </button>
            </div>

            <!-- 消息流列表 -->
            <ChatMessage
              v-for="msg in allDisplayMessages"
              :key="msg.id"
              :message="msg"
            />
          </div>

          <!-- 底部多功能自适应输入框组件 -->
          <ChatInput
            ref="chatInputRef"
            class="chat-input-dock"
            :streaming="streaming"
            @send="handleSend"
            @stop="stopStream"
          />
        </div>
      </main>

      <!-- 右栏：辅助学习面板 (280px) -->
      <aside class="workbench-col workbench-col--assist">
        <!-- 卡片 1：课程相关资源 -->
        <div class="col-card assist-card resources-assist-card">
          <div class="col-card-header">
            <span class="header-title">课程相关资源</span>
            <button type="button" class="more-link" @click="navigateToResources">
              <span>更多</span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <!-- 资源分类筛选小胶囊 -->
          <div class="resource-filter-pills">
            <button
              v-for="cat in resourceCategories"
              :key="cat"
              type="button"
              class="filter-pill"
              :class="{ active: activeResourceCat === cat }"
              @click="activeResourceCat = cat"
            >
              {{ cat }}
            </button>
          </div>

          <!-- 资源文件列表 -->
          <div class="resource-items-list">
            <div
              v-for="res in filteredResources"
              :key="res.title"
              class="resource-file-row"
              @click="downloadResource(res)"
            >
              <div class="file-badge-box" :class="`file-badge-box--${res.type}`">
                <span class="badge-text">{{ res.ext }}</span>
              </div>
              <span class="file-name" :title="res.title">{{ res.title }}</span>
              <button type="button" class="file-download-btn" title="下载资料">
                <el-icon><Download /></el-icon>
              </button>
            </div>
          </div>
        </div>

        <!-- 卡片 2：推荐问题 (精选最多 5 个高频点击直问) -->
        <div class="col-card assist-card prompts-assist-card">
          <div class="col-card-header">
            <span class="header-title">推荐问题</span>
          </div>

          <div class="recommended-prompts-list">
            <div
              v-for="(question, qIdx) in recommendedQuestions.slice(0, 5)"
              :key="qIdx"
              class="prompt-item-row"
              @click="handleSendRecommended(question)"
            >
              <el-icon class="prompt-arrow-icon"><ArrowRight /></el-icon>
              <span class="prompt-text" :title="question">{{ question }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 历史记录侧边抽屉 -->
    <el-drawer
      v-model="historyDrawerVisible"
      title="问答历史会话"
      size="320px"
      direction="rtl"
      :append-to-body="true"
    >
      <ChatSessionList
        :sessions="sessions"
        :current-id="currentSessionId"
        @select="handleSelectSession"
        @create="handleCreateNewSession"
        @delete="deleteSession"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, markRaw } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Search,
  ArrowDown,
  ArrowRight,
  Clock,
  Connection,
  Download,
  EditPen,
  Reading,
  VideoCamera,
  FolderOpened,
  Document,
  ChatDotRound,
  Monitor,
  Opportunity,
  Close
} from '@element-plus/icons-vue';
import type { CourseVO } from '@/types/course/course';
import type { Chapter } from '@/types/course/chapter';
import { getChapters } from '@/api/course/chapter';
import { getModelConfigs } from '@/api/system/model';
import { useAIStream } from '@/composables/ai/useAIStream';
import ChatMessage from '@/components/ai/ChatMessage.vue';
import ChatInput from '@/components/ai/ChatInput.vue';
import ChatSessionList from '@/components/ai/ChatSessionList.vue';

const props = defineProps<{
  course?: CourseVO | null;
}>();

const router = useRouter();

// ======================= 1. 顶部 Banner 与 5 大胶囊 (组件库矢量图标) =======================
const quickActionPills = [
  { icon: markRaw(ChatDotRound), title: '智能答疑', prompt: '请针对当前课程知识体系进行智能答疑与难点梳理', color: '#1677FF' },
  { icon: markRaw(Monitor), title: '代码解析', prompt: '请解析一段典型的核心代码示例并分析关键逻辑', color: '#0284C7' },
  { icon: markRaw(Reading), title: '学习指导', prompt: '请为我提供当前学习阶段的高效复习与规划指导', color: '#10B981' },
  { icon: markRaw(EditPen), title: '习题讲解', prompt: '请结合典型例题为我详细讲解解题思路与避坑指南', color: '#F59E0B' },
  { icon: markRaw(Opportunity), title: '知识拓展', prompt: '请提供本门课程的进阶知识拓展与工程实践场景', color: '#EAB308' }
];

function handlePillClick(pill: (typeof quickActionPills)[0]) {
  handleSend(pill.prompt);
}

// ======================= 2. 左栏：14 章课程章节树 =======================
const chapterKeyword = ref('');
const activeSectionId = ref<number | null>(null);
const activeSectionTitle = ref('');
const activeChapterId = ref<number | undefined>(undefined);
const chaptersLoading = ref(false);

interface ChapterNode {
  id: number;
  title: string;
  expanded: boolean;
  sections: { id: number; title: string }[];
}

const chaptersData = ref<ChapterNode[]>([]);

function mapChapterTree(nodes: Chapter[], expandedFirst = true): ChapterNode[] {
  return nodes.map((node, index) => {
    const childSections = (node.children || []).map((child) => ({
      id: child.id,
      title: child.title
    }));
    const sections =
      childSections.length > 0
        ? childSections
        : [{ id: node.id, title: node.title }];
    return {
      id: node.id,
      title: node.title,
      expanded: expandedFirst && index === 0,
      sections
    };
  });
}

async function loadChapters() {
  const courseId = props.course?.id ? Number(props.course.id) : 0;
  if (!courseId) return;
  chaptersLoading.value = true;
  try {
    const res = await getChapters(courseId);
    const list = Array.isArray(res?.data) ? res.data : [];
    chaptersData.value = mapChapterTree(list as Chapter[]);
    if (chaptersData.value.length > 0) {
      const first = chaptersData.value[0];
      const firstSec = first.sections[0];
      if (firstSec) {
        activeSectionId.value = firstSec.id;
        activeChapterId.value = firstSec.id;
        activeSectionTitle.value = firstSec.title;
      }
    }
  } catch {
    chaptersData.value = [];
    ElMessage.error('加载课程章节失败');
  } finally {
    chaptersLoading.value = false;
  }
}

const filteredChapters = computed(() => {
  const kw = chapterKeyword.value.trim().toLowerCase();
  if (!kw) return chaptersData.value;
  return chaptersData.value
    .map(chap => {
      const matchChap = chap.title.toLowerCase().includes(kw);
      const filteredSecs = chap.sections.filter(s => s.title.toLowerCase().includes(kw));
      if (matchChap || filteredSecs.length > 0) {
        return {
          ...chap,
          expanded: true,
          sections: filteredSecs.length > 0 ? filteredSecs : chap.sections
        };
      }
      return null;
    })
    .filter(Boolean) as ChapterNode[];
});

function selectSection(sec: { id: number; title: string }) {
  activeSectionId.value = sec.id;
  activeChapterId.value = sec.id;
  activeSectionTitle.value = sec.title;
}

// ======================= 3. 中栏：AI 问答工作台与消息流 =======================
const modeTabs = [
  { key: 'ai', label: 'AI 助手' },
  { key: 'task', label: '学习任务' },
  { key: 'graph', label: '知识图谱' },
  { key: 'resource', label: '推荐资源' }
];
const currentModeTab = ref('ai');

function switchModeTab(key: string) {
  currentModeTab.value = key;
  if (key === 'graph') {
    router.push(`/course/${props.course?.id || '101'}/knowledge-points`);
  } else if (key === 'resource') {
    router.push(`/course/${props.course?.id || '101'}/resources`);
  } else if (key === 'task') {
    router.push(`/course/${props.course?.id || '101'}/overview`);
  }
}

const modelOptions = ref<Array<{ name: string; key: string; desc: string }>>([]);
const currentModel = ref('默认模型');
const currentModelKey = ref<string | undefined>(undefined);

async function loadModels() {
  try {
    const configs = await getModelConfigs();
    const chatModels = configs.filter((m) => m.enabled && !m.supportsEmbedding);
    modelOptions.value = chatModels.map((m) => ({
      name: m.name,
      key: m.modelKey,
      desc: m.provider || 'LLM'
    }));
    const defaultModel = chatModels.find((m) => m.isDefault) || chatModels[0];
    if (defaultModel) {
      currentModel.value = defaultModel.name;
      currentModelKey.value = defaultModel.modelKey;
    }
  } catch {
    modelOptions.value = [];
  }
}

function handleModelSelect(modelKey: string) {
  const found = modelOptions.value.find((m) => m.key === modelKey);
  if (found) {
    currentModel.value = found.name;
    currentModelKey.value = found.key;
    ElMessage.success(`已切换推理引擎为：${found.name}`);
  }
}

const historyDrawerVisible = ref(false);
const messagesScrollRef = ref<HTMLDivElement | null>(null);
const chatInputRef = ref<InstanceType<typeof ChatInput> | null>(null);

// useAIStream 状态与方法
const {
  sessions,
  currentSessionId,
  messages,
  streaming,
  loadSessions,
  switchSession,
  sendMessage,
  stopStream,
  createNewSession,
  deleteSession
} = useAIStream();

const welcomeMessage = computed(() => ({
  id: 'welcome',
  role: 'assistant' as const,
  createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  content:
    '你好！我是本课程的专属 AI 助教。已为你加载当前课程知识库，关于章节知识、典型例题或代码实现，请随时向我提问！'
}));

const allDisplayMessages = computed(() => {
  if (messages.value.length === 0) {
    return [welcomeMessage.value];
  }
  return messages.value;
});

function scrollToBottom() {
  nextTick(() => {
    if (messagesScrollRef.value) {
      messagesScrollRef.value.scrollTop = messagesScrollRef.value.scrollHeight;
    }
  });
}

function handleSelectSession(id: string) {
  switchSession(id);
  historyDrawerVisible.value = false;
}

function handleCreateNewSession() {
  createNewSession(props.course?.id ? Number(props.course.id) : 101);
  historyDrawerVisible.value = false;
  ElMessage.success('已开启新问答会话');
}

function handleSend(promptText: string) {
  const courseId = props.course?.id ? Number(props.course.id) : 101;
  let finalPrompt = promptText;
  if (activeSectionTitle.value) {
    finalPrompt = `[当前章节: ${activeSectionTitle.value}] ${finalPrompt}`;
  }
  sendMessage(finalPrompt, courseId, {
    chapterId: activeChapterId.value,
    modelKey: currentModelKey.value
  });
  scrollToBottom();
}

// ======================= 4. 右栏：资源、推荐问题与 8 宫格学习工具 =======================
const resourceCategories = ['课件', '视频', '文档', '习题', '项目'];
const activeResourceCat = ref('课件');

interface ResourceItem {
  title: string;
  type: string;
  ext: string;
  cat: string;
}

const allResources: ResourceItem[] = [
  { title: 'Java5.0课件.pdf', type: 'pdf', ext: 'PDF', cat: '课件' },
  { title: '第5章课堂笔记.zip', type: 'zip', ext: 'ZIP', cat: '课件' },
  { title: '课程PPT.pptx', type: 'ppt', ext: 'PPT', cat: '课件' },
  { title: 'Java示例代码.zip', type: 'code', ext: 'ZIP', cat: '课件' },
  { title: '1.1 语言概述精讲.mp4', type: 'video', ext: 'MP4', cat: '视频' },
  { title: '6.1 面向对象深度精析.mp4', type: 'video', ext: 'MP4', cat: '视频' },
  { title: 'Java并发编程参考手册.pdf', type: 'pdf', ext: 'PDF', cat: '文档' },
  { title: 'JDK 17 新特性速查指南.pdf', type: 'pdf', ext: 'PDF', cat: '文档' },
  { title: '基础语法自测习题集.pdf', type: 'pdf', ext: 'PDF', cat: '习题' },
  { title: '面向对象典型真题及解析.docx', type: 'doc', ext: 'DOC', cat: '习题' },
  { title: '在线图书商城项目源码.zip', type: 'code', ext: 'ZIP', cat: '项目' }
];

const filteredResources = computed(() => {
  return allResources.filter(r => r.cat === activeResourceCat.value);
});

function downloadResource(res: ResourceItem) {
  ElMessage.success(`开始下载课程资料：${res.title}`);
}

function navigateToResources() {
  router.push(`/course/${props.course?.id || '101'}/resources`);
}

// 精选 5 大推荐高频提问
const recommendedQuestions = [
  '什么是Java虚拟机（JVM）？',
  'JDK、JRE、JVM 三者有什么区别？',
  '如何理解面向对象的三大特性？',
  'Java中的异常处理机制是怎样的？',
  'ArrayList 和 LinkedList 有什么区别？'
];

function handleSendRecommended(question: string) {
  handleSend(question);
}



function exportChatMarkdown() {
  const content = allDisplayMessages.value
    .map(m => `### ${m.role === 'assistant' ? '[EduMind AI 助教]' : '[学员]'} (${m.createdAt})\n\n${m.content}\n`)
    .join('\n---\n\n');
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${props.course?.title || 'Java程序设计'}_AI问答记录.md`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('已导出当前问答记录为 Markdown 文件');
}

// 自动滚动跟随
watch(
  () => [allDisplayMessages.value.length, allDisplayMessages.value[allDisplayMessages.value.length - 1]?.content],
  () => {
    scrollToBottom();
  },
  { deep: true }
);

onMounted(() => {
  loadChapters();
  loadModels();
  loadSessions(props.course?.id ? Number(props.course.id) : undefined);
  scrollToBottom();
  nextTick(() => {
    const appContent = document.querySelector('.app-content');
    if (appContent) {
      appContent.scrollLeft = 0;
    }
  });
});
</script>

<style scoped lang="scss">
.course-ai-page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;

  // ================= 1. 顶部专属视觉大 Banner =================
  .course-ai-hero-banner {
    position: relative;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    flex-shrink: 0;
    min-height: 108px;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    overflow: hidden;
    box-sizing: border-box;
    background:
      linear-gradient(90deg, rgba(240, 247, 255, 0.96) 0%, rgba(243, 244, 255, 0.9) 45%, rgba(255, 255, 255, 0.2) 75%),
      url('@/assets/images/课程ai助手的banner.png') no-repeat right center;
    background-size: cover;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);

    .banner-overlay-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 20px;
      width: 100%;
      height: 100%;
      box-sizing: border-box;

      .hero-left-section {
        display: flex;
        flex-direction: column;
        gap: 12px;
        max-width: 58%;
        min-width: 0;

        .hero-header-row {
          display: flex;
          align-items: center;
          gap: 14px;

          .hero-icon-squircle {
            width: 48px;
            height: 48px;
            border-radius: 14px;
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 14px rgba(22, 119, 255, 0.18);
            flex-shrink: 0;

            .hero-brand-logo {
              width: 100%;
              height: 100%;
              object-fit: contain;
              display: block;
            }
          }

          .hero-titles {
            display: flex;
            flex-direction: column;
            gap: 4px;

            .hero-course-title {
              margin: 0;
              font-size: 20px;
              font-weight: 700;
              color: #0F172A;
              letter-spacing: -0.3px;
            }

            .hero-course-desc {
              margin: 0;
              font-size: 13px;
              color: #475569;
              font-weight: 400;
            }
          }
        }

        // 5 个功能胶囊 (稍微往下移动一点，与上方描述文字拉开层次)
        .hero-capsules-row {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
          margin-top: 4px;

          .hero-action-pill {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            height: 30px;
            padding: 0 12px;
            border-radius: 9999px; // 长圆跑道胶囊
            background: rgba(255, 255, 255, 0.88);
            border: 1px solid #CBD5E1;
            backdrop-filter: blur(8px);
            color: #334155;
            font-size: 12px;
            font-weight: 500;
            cursor: pointer;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
            transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
            white-space: nowrap;

            .pill-icon {
              font-size: 14px;
              display: inline-flex;
              align-items: center;
              justify-content: center;
              flex-shrink: 0;
            }

            &:hover {
              color: #1677FF;
              border-color: #93C5FD;
              background: #FFFFFF;
              transform: translateY(-1px);
              box-shadow: 0 4px 12px rgba(22, 119, 255, 0.16);
            }
          }
        }
      }
    }
  }

  // ================= 2. 原型同款三栏布局 =================
  .course-ai-grid-workbench {
    display: grid;
    grid-template-columns: 210px minmax(0, 1fr) 250px;
    gap: 12px;
    align-items: stretch;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    height: 640px;
    min-height: 580px;
    box-sizing: border-box;

    .workbench-col {
      min-width: 0;
      max-width: 100%;
      min-height: 0;
      height: 100%;
      box-sizing: border-box;
    }

    // 通用卡片容器
    .col-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      box-shadow: 0 2px 12px rgba(30, 80, 150, 0.04);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      min-width: 0;
      box-sizing: border-box;

      .col-card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px 16px 10px;

        .header-title {
          font-size: 14px;
          font-weight: 700;
          color: #0F172A;
        }

        .count-badge {
          font-size: 11px;
          color: #94A3B8;
        }

        .more-link {
          display: inline-flex;
          align-items: center;
          gap: 2px;
          background: transparent;
          border: none;
          padding: 0;
          font-size: 12px;
          color: #64748B;
          cursor: pointer;

          &:hover {
            color: #1677FF;
          }
        }
      }
    }

    // ================= 左栏：课程章节 =================
    .workbench-col--chapters {
      min-height: 0;

      .chapters-card {
        height: 100%;
        .chapters-search-box {
          padding: 0 14px 10px;

          :deep(.el-input__wrapper) {
            border-radius: 8px;
            box-shadow: 0 0 0 1px #E2E8F0 inset;
            background: #F8FAFC;
          }

          .search-icon {
            color: #94A3B8;
          }
        }

        .chapters-scroll-area {
          flex: 1;
          min-height: 0;
          overflow-y: auto;
          padding: 4px 10px 14px;

          .chapter-group-item {
            margin-bottom: 4px;

            .chapter-header-row {
              display: flex;
              align-items: center;
              gap: 6px;
              padding: 7px 8px;
              border-radius: 6px;
              cursor: pointer;
              font-size: 13px;
              font-weight: 600;
              color: #1E293B;
              transition: background 0.15s;

              .arrow-icon {
                font-size: 11px;
                color: #94A3B8;
              }

              .chapter-label {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              }

              &:hover {
                background: #F1F5F9;
              }
            }

            .sections-sub-list {
              padding-left: 18px;
              display: flex;
              flex-direction: column;
              gap: 2px;
              margin-top: 2px;

              .section-leaf-row {
                display: flex;
                align-items: center;
                gap: 6px;
                padding: 6px 10px;
                border-radius: 6px;
                font-size: 12.5px;
                color: #475569;
                cursor: pointer;
                transition: all 0.18s;

                .section-dot {
                  width: 4px;
                  height: 4px;
                  border-radius: 50%;
                  background: #CBD5E1;
                }

                .section-title {
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                }

                &:hover {
                  color: #1677FF;
                  background: #F8FAFC;

                  .section-dot {
                    background: #1677FF;
                  }
                }

                &.active {
                  color: #1677FF;
                  background: #EFF6FF;
                  font-weight: 600;

                  .section-dot {
                    background: #1677FF;
                    transform: scale(1.3);
                  }
                }
              }
            }
          }
        }
      }
    }

    // ================= 中栏：AI 问答主工作台 =================
    .workbench-col--chat {
      display: flex;
      flex-direction: column;
      min-height: 0;

      .chat-workbench-card {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
        height: 100%;

        // 顶栏工具条
        .chat-top-toolbar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex-shrink: 0;
          padding: 8px 12px;
          border-bottom: 1px solid #F1F5F9;
          background: #FFFFFF;
          flex-wrap: wrap;
          gap: 8px;
          min-width: 0;

          .mode-capsule-tabs {
            display: flex;
            align-items: center;
            gap: 6px;
            flex-wrap: wrap;

            .mode-pill-tab {
              height: 28px;
              padding: 0 12px;
              border-radius: 9999px; // 长圆跑道 Tab
              border: 1px solid transparent;
              background: #F8FAFC;
              color: #475569;
              font-size: 12px;
              font-weight: 500;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                color: #1677FF;
                background: #F1F5F9;
              }

              &.active {
                background: #1677FF;
                color: #FFFFFF;
                font-weight: 600;
                box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
              }
            }
          }

          .toolbar-right-actions {
            display: flex;
            align-items: center;
            gap: 8px;

            .toolbar-pill-btn {
              display: inline-flex;
              align-items: center;
              gap: 6px;
              height: 28px;
              padding: 0 10px;
              border-radius: 9999px;
              border: 1px solid #E2E8F0;
              background: #FFFFFF;
              color: #334155;
              font-size: 11.5px;
              font-weight: 500;
              cursor: pointer;
              transition: all 0.2s;

              .arrow {
                font-size: 10px;
                color: #94A3B8;
              }

              &:hover {
                border-color: #1677FF;
                color: #1677FF;
              }
            }
          }
        }

        // 消息区：占满卡片剩余高度，内容多时在内部滚动
        .messages-flow-scroll {
          flex: 1;
          min-height: 0;
          overflow-x: hidden;
          overflow-y: auto;
          -webkit-overflow-scrolling: touch;
          padding: 16px 20px;
          display: flex;
          flex-direction: column;
          gap: 14px;
          background: #FAFCFE;

          .context-anchor-chip {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            align-self: flex-start;
            padding: 4px 12px;
            border-radius: 6px;
            background: #EFF6FF;
            border: 1px dashed #93C5FD;
            font-size: 12px;
            color: #1E40AF;
            margin-bottom: 16px;

            .pin-icon {
              font-size: 13px;
            }

            .clear-anchor-btn {
              background: transparent;
              border: none;
              color: #93C5FD;
              cursor: pointer;
              font-size: 12px;

              &:hover {
                color: #1E40AF;
              }
            }
          }
        }

        :deep(.chat-input-dock) {
          flex-shrink: 0;
        }
      }
    }

    // ================= 右栏：辅助学习面板 (3 张卡片) =================
    .workbench-col--assist {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-height: 0;
      overflow-y: auto;

      .assist-card {
        background: #FFFFFF;
        border-radius: 12px;
      }

      // 卡片 1：课程相关资源
      .resources-assist-card {
        flex: 1.1;

        .resource-filter-pills {
          display: flex;
          align-items: center;
          gap: 4px;
          padding: 0 12px 10px;
          overflow-x: auto;

          .filter-pill {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 24px;
            padding: 0 6px;
            border-radius: 9999px;
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            color: #64748B;
            font-size: 11px;
            font-weight: 500;
            cursor: pointer;
            white-space: nowrap;
            transition: all 0.18s;

            &:hover {
              color: #1677FF;
              border-color: #93C5FD;
              background: #EFF6FF;
            }

            &.active {
              background: #1677FF;
              border-color: #1677FF;
              color: #FFFFFF;
              font-weight: 600;
              box-shadow: 0 2px 6px rgba(22, 119, 255, 0.25);
            }
          }
        }

        .resource-items-list {
          flex: 1;
          overflow-y: auto;
          padding: 0 14px 10px;
          display: flex;
          flex-direction: column;
          gap: 6px;

          .resource-file-row {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 8px;
            border-radius: 6px;
            background: #FAFAFA;
            border: 1px solid #F1F5F9;
            cursor: pointer;
            transition: all 0.18s;

            .file-badge-box {
              width: 30px;
              height: 24px;
              border-radius: 4px;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 9.5px;
              font-weight: 700;
              flex-shrink: 0;

              &--pdf { background: #FEE2E2; color: #DC2626; }
              &--zip { background: #F3E8FF; color: #7C3AED; }
              &--ppt { background: #FFEDD5; color: #EA580C; }
              &--code { background: #E0F2FE; color: #0284C7; }
              &--video { background: #DCFCE7; color: #16A34A; }
              &--doc { background: #EFF6FF; color: #2563EB; }
            }

            .file-name {
              flex: 1;
              font-size: 12px;
              color: #334155;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .file-download-btn {
              background: transparent;
              border: none;
              color: #94A3B8;
              font-size: 13px;
              cursor: pointer;
              padding: 2px;

              &:hover {
                color: #1677FF;
              }
            }

            &:hover {
              background: #F1F5F9;
              border-color: #E2E8F0;

              .file-name {
                color: #1677FF;
              }
            }
          }
        }
      }

      // 卡片 2：推荐问题 (8 大高频问答)
      .prompts-assist-card {
        flex: 1;
        min-height: 0;
        display: flex;
        flex-direction: column;

        .recommended-prompts-list {
          flex: 1;
          overflow-y: auto;
          padding: 0 14px 10px;
          display: flex;
          flex-direction: column;
          gap: 4px;

          .prompt-item-row {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 6px 8px;
            border-radius: 6px;
            font-size: 12px;
            color: #334155;
            cursor: pointer;
            transition: all 0.18s;

            .prompt-arrow-icon {
              color: #94A3B8;
              font-size: 12px;
              flex-shrink: 0;
              transition: transform 0.2s ease, color 0.2s ease;
            }

            .prompt-text {
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            &:hover {
              color: #1677FF;
              background: #EFF6FF;

              .prompt-arrow-icon {
                color: #1677FF;
                transform: translateX(2px);
              }
            }
          }
        }
      }
    }
  }
}

// 下拉菜单项
.model-dropdown-menu {
  .model-item-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    width: 220px;

    .item-name {
      font-weight: 600;
      color: #1E293B;
    }

    .item-badge {
      font-size: 11px;
      color: #94A3B8;
    }
  }

  .is-selected {
    color: #1677FF;
    background: #EFF6FF;
  }
}
</style>
