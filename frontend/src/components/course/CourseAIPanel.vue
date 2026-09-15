<template>
  <div class="course-ai-page-wrapper">
    <!-- 1. 顶部专属视觉大 Banner -->
    <div class="course-ai-hero-banner">
      <div class="banner-overlay-content">
        <div class="hero-left-section">
          <div class="hero-header-row">
            <!-- 渐变立体图标盒 -->
            <div class="hero-icon-squircle">
              <img class="hero-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
            </div>
            <div class="hero-titles">
              <div class="hero-title-row">
                <h2 class="hero-course-title">
                  {{ displayCourseTitle }} · AI 助手
                </h2>
                <!-- 切换当前学校租户名下其他课程 -->
                <el-dropdown
                  v-if="tenantCourseOptions.length > 1"
                  trigger="click"
                  @command="handleSwitchCourse"
                >
                  <button type="button" class="course-switch-pill" title="切换当前所选课程">
                    <span>切换课程</span>
                    <el-icon class="arrow"><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu class="course-dropdown-menu">
                      <el-dropdown-item
                        v-for="c in tenantCourseOptions"
                        :key="c.id"
                        :command="c.id"
                        :class="{ 'is-selected': Number(c.id) === currentCourseIdNum }"
                      >
                        {{ c.title || c.name }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <p class="hero-course-desc">
                基于学科知识库与教学大纲，为你提供专业、准确、个性化的学习支持
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

              <!-- 开启新对话 -->
              <button
                type="button"
                class="toolbar-pill-btn new-chat-btn"
                title="开启新问答会话"
                @click="handleCreateNewSession"
              >
                <el-icon><Plus /></el-icon>
                <span>新对话</span>
              </button>

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
          <div
            ref="messagesScrollRef"
            class="messages-flow-scroll"
            data-chat-scroll="true"
            @scroll="handleViewportScroll"
          >
            <!-- 章节锚定上下文小浮条 -->
            <div v-if="activeSectionTitle" class="context-anchor-chip">
              <el-icon class="pin-icon"><Connection /></el-icon>
              <span>当前知识锚定章节：<strong>{{ activeSectionTitle }}</strong></span>
              <button type="button" class="clear-anchor-btn" @click="activeSectionId = null; activeChapterId = undefined; activeSectionTitle = ''">
                <el-icon><Close /></el-icon>
              </button>
            </div>

            <!-- 历史已结算消息列表 -->
            <ChatMessage
              v-for="(msg, idx) in allDisplayMessages"
              :key="msg.id || idx"
              :message="msg"
              :is-last="idx === allDisplayMessages.length - 1 && !streaming"
              :follow-up-prompts="followUpPrompts"
              @send-prompt="handleSendPrompt"
              @regenerate="handleRegenerate(idx)"
              @delete="confirmDeleteMessage(idx)"
              @reasoning-collapse="pauseAutoScrollFollow"
            />

            <!-- 正在流式生成的进行时消息卡片 (对标侧边栏真流式) -->
            <div v-if="streaming" class="streaming-active-row">
              <div class="streaming-avatar-box">
                <img class="assistant-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
              </div>
              <div class="streaming-body-box">
                <div class="msg-meta-header">
                  <span class="sender-name">EduMind 课程 AI 助教</span>
                  <span class="streaming-badge-tag">正在生成研读解析...</span>
                </div>

                <!-- 深度思考状态卡片 (动态脉冲，正文输出时自动折叠) -->
                <AIThinking
                  v-if="showThinkingPanel && (!streamingAnswerBody || streamingThinkingDisplay)"
                  :content="streamingThinkingDisplay"
                  :folded="isReasoningFolded"
                  :active="!streamingAnswerBody && (isReasoningActive || !streamingThinkingDisplay)"
                  :has-answer-body="!!streamingAnswerBody"
                  :phase-message="streamPhaseMessage || '正在深度研读本门课程知识大纲与切片...'"
                  @update:folded="isReasoningFolded = $event"
                  @user-collapse="pauseAutoScrollFollow"
                />

                <!-- 正文流式渲染 (useStreamingMarkdown 增量输出) -->
                <div v-if="streamingAnswerBody" class="msg-bubble is-streaming">
                  <div class="markdown-body chat-md-content" v-html="streamingRenderedHtml" />
                  <span class="stream-cursor">▋</span>
                </div>
              </div>
            </div>

            <!-- 不可见物理锚点，用于 requestAnimationFrame 顺畅跟随贴底向上滚动 -->
            <div ref="streamAnchorRef" class="stream-bottom-anchor" />
          </div>

          <transition name="fade">
            <button
              v-if="showScrollToBottom"
              class="scroll-bottom-btn"
              type="button"
              aria-label="回到底部"
              title="查看最新回复"
              @click="scrollToBottomSmooth"
            >
              <svg class="scroll-bottom-svg" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="12" r="10" class="scroll-bottom-svg__bg" />
                <path d="M12 7.5v6.5" class="scroll-bottom-svg__shaft" />
                <path d="M8.8 11.8 12 15 15.2 11.8" class="scroll-bottom-svg__head" />
              </svg>
            </button>
          </transition>

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
          <div class="resource-items-list" v-loading="resourcesLoading">
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
            <div v-if="filteredResources.length === 0 && !resourcesLoading" class="resource-empty-box">
              <el-icon><Document /></el-icon>
              <span>当前分类暂无教学资料</span>
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
        @delete="handleDeleteSession"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { toRef } from 'vue';
import {
  Search,
  ArrowDown,
  ArrowRight,
  Clock,
  Connection,
  Download,
  Plus,
  Document,
  Close
} from '@element-plus/icons-vue';
import type { CourseVO } from '@/types/course/course';
import { useCourseAIWorkspace } from '@/composables/course/useCourseAIWorkspace';
import ChatMessage from '@/components/ai/ChatMessage.vue';
import ChatInput from '@/components/ai/ChatInput.vue';
import ChatSessionList from '@/components/ai/ChatSessionList.vue';
import AIThinking from '@/components/ai/AIChat/AIThinking.vue';

const props = defineProps<{
  course?: CourseVO | null;
}>();

const emit = defineEmits<{
  (e: 'switch-course', courseId: number): void;
}>();

const {
  currentCourseIdNum,
  displayCourseTitle,
  tenantCourseOptions,
  handleSwitchCourse,
  quickActionPills,
  handlePillClick,
  chapterKeyword,
  activeSectionId,
  activeSectionTitle,
  activeChapterId,
  chaptersData,
  filteredChapters,
  selectSection,
  modeTabs,
  currentModeTab,
  switchModeTab,
  modelOptions,
  currentModel,
  currentModelKey,
  handleModelSelect,
  historyDrawerVisible,
  chatInputRef,
  sessions,
  currentSessionId,
  streaming,
  messagesScrollRef,
  streamAnchorRef,
  streamingRenderedHtml,
  streamingAnswerBody,
  streamingThinkingDisplay,
  isReasoningFolded,
  isReasoningActive,
  streamPhaseMessage,
  followUpPrompts,
  showThinkingPanel,
  allDisplayMessages,
  handleDeleteSession,
  handleSelectSession,
  handleCreateNewSession,
  handleSend,
  handleSendPrompt,
  handleRegenerate,
  confirmDeleteMessage,
  stopStream,
  scrollToBottomSmooth,
  showScrollToBottom,
  pauseAutoScrollFollow,
  handleViewportScroll,
  resourceCategories,
  activeResourceCat,
  resourcesLoading,
  filteredResources,
  downloadResource,
  navigateToResources,
  recommendedQuestions,
  handleSendRecommended
} = useCourseAIWorkspace({
  course: toRef(props, 'course'),
  onSwitchCourse: (courseId) => emit('switch-course', courseId)
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
    background: linear-gradient(135deg, #eaf3ff 0%, #eef2ff 45%, #e0e7ff 100%);
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);

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
        max-width: 100%;
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

            .hero-title-row {
              display: flex;
              align-items: center;
              gap: 12px;
              flex-wrap: wrap;

              .course-switch-pill {
                display: inline-flex;
                align-items: center;
                gap: 4px;
                height: 24px;
                padding: 0 10px;
                border-radius: 9999px;
                background: #EFF6FF;
                border: 1px solid #BFDBFE;
                color: #1677FF;
                font-size: 11.5px;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s;

                &:hover {
                  background: #DBEAFE;
                  border-color: #93C5FD;
                }
              }
            }

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
    height: clamp(640px, calc(100vh - 160px), 960px);
    min-height: 640px;
    box-sizing: border-box;
    overflow-x: hidden;

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
        position: relative;
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

              &.new-chat-btn {
                border-color: rgba(22, 119, 255, 0.35);
                color: #1677FF;
                background: rgba(22, 119, 255, 0.06);

                &:hover {
                  background: rgba(22, 119, 255, 0.12);
                }
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
          overflow-anchor: none;
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

          /* 正在流式生成的进行时卡片 (与结算完成后的 ChatMessage 保持 100% 结构一致) */
          .streaming-active-row {
            display: flex;
            gap: 12px;
            width: 100%;
            max-width: 100%;
            margin-bottom: 22px;
            box-sizing: border-box;

            .streaming-avatar-box {
              width: 40px;
              height: 40px;
              border-radius: 50%;
              overflow: hidden;
              display: flex;
              align-items: center;
              justify-content: center;
              flex-shrink: 0;
              box-shadow: 0 4px 14px rgba(37, 99, 235, 0.18);
              position: sticky;
              top: 0;

              .assistant-brand-logo {
                width: 100%;
                height: 100%;
                object-fit: cover;
                display: block;
              }
            }

            .streaming-body-box {
              max-width: 86%;
              min-width: 0;
              display: flex;
              flex-direction: column;
              gap: 8px;

              .msg-meta-header {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 5px;

                .sender-name {
                  font-size: 12.5px;
                  font-weight: 600;
                  color: #1E293B;
                }

                .streaming-badge-tag {
                  font-size: 11px;
                  padding: 1px 8px;
                  border-radius: 999px;
                  background: rgba(22, 119, 255, 0.1);
                  color: #1677ff;
                  font-weight: 500;
                  animation: pulse 1.5s infinite ease-in-out;
                }
              }

              .msg-bubble.is-streaming {
                position: relative;
                max-width: 100%;
                min-width: 0;
                box-sizing: border-box;
                padding: 16px 20px 24px;
                font-size: 13.5px;
                line-height: 1.7;
                word-break: break-word;
                background: #F8FAFC;
                border: 1px solid #E2E8F0;
                border-radius: 4px 18px 18px 18px;
                box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
                color: #1E293B;
                overflow: hidden;

                .stream-cursor {
                  display: inline-block;
                  margin-left: 2px;
                  color: #1677FF;
                  font-size: 14px;
                  animation: blink 0.9s infinite;
                }
              }
            }
          }

          .stream-bottom-anchor {
            height: 1px;
            width: 100%;
            pointer-events: none;
            visibility: hidden;
            overflow-anchor: none;
          }
        }

        :deep(.chat-input-dock) {
          flex-shrink: 0;
        }

        .scroll-bottom-btn {
          position: absolute;
          bottom: 88px;
          right: 20px;
          z-index: 20;
          width: 34px;
          height: 34px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: rgba(255, 255, 255, 0.96);
          border: 1px solid rgba(22, 119, 255, 0.35);
          box-shadow: 0 4px 14px rgba(15, 23, 42, 0.12);
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover {
            transform: translateY(-2px);
            background: #1677ff;
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);

            .scroll-bottom-svg__bg {
              fill: #1677ff;
            }

            .scroll-bottom-svg__shaft,
            .scroll-bottom-svg__head {
              stroke: #ffffff;
            }
          }

          .scroll-bottom-svg {
            width: 20px;
            height: 20px;

            &__bg {
              fill: #ffffff;
            }

            &__shaft,
            &__head {
              stroke: #1677ff;
              stroke-width: 2.2;
              fill: none;
            }
          }
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

          .resource-empty-box {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 8px;
            padding: 32px 10px;
            color: #94A3B8;
            font-size: 12px;

            .el-icon {
              font-size: 24px;
              color: #CBD5E1;
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
