<template>
  <div class="course-chapters-page">
    <!-- 顶部大纲工具栏 -->
    <div class="chapters-top-toolbar">
      <div class="syllabus-summary">
        <h3 class="syllabus-title">教学大纲与章节进度</h3>
        <div class="summary-pill-group">
          <span class="pill-badge pill-badge--primary">共 {{ chapters.length }} 大章节</span>
          <span class="pill-badge pill-badge--neutral">已学完 {{ completedLessonCount }} / {{ totalLessonCount }} 课时</span>
        </div>
      </div>

      <div class="toolbar-actions">
        <!-- 长圆搜索过滤 -->
        <div class="capsule-search-mini">
          <el-icon class="search-mini-icon"><Search /></el-icon>
          <input
            v-model="searchChapterText"
            type="text"
            class="search-mini-input"
            placeholder="搜索小节名称或知识点..."
          />
        </div>

        <button type="button" class="capsule-tool-btn" @click="toggleAllCollapse">
          <span>{{ isAllExpanded ? '全部折叠' : '全部展开' }}</span>
        </button>

        <template v-if="courseEditable">
          <button
            type="button"
            class="capsule-tool-btn capsule-tool-btn--primary"
            @click="openAddChapterDrawer"
          >
            <el-icon><Plus /></el-icon>
            <span>新增章节</span>
          </button>

          <button
            type="button"
            class="capsule-tool-btn capsule-tool-btn--ai"
            @click="handleChapterAiQuiz"
          >
            <el-icon><MagicStick /></el-icon>
            <span>针对本大纲 AI 出题</span>
          </button>
        </template>
      </div>
    </div>

    <!-- 章节树形列表手风琴 -->
    <div class="chapters-accordion-list">
      <div
        v-for="(chapter, cIndex) in filteredChapters"
        :key="chapter.id"
        class="chapter-accordion-card"
        :class="{ 'is-open': openChapters.includes(chapter.id) }"
      >
        <!-- 章节头部栏 -->
        <div class="chapter-card-header" @click="toggleChapter(chapter.id)">
          <div class="header-left-info">
            <div class="chapter-num-badge">
              {{ String(cIndex + 1).padStart(2, '0') }}
            </div>
            <div class="chapter-title-box">
              <h4 class="chapter-title">{{ chapter.title }}</h4>
              <p v-if="chapter.description" class="chapter-desc">
                {{ chapter.description }}
              </p>
            </div>
          </div>

          <div class="header-right-meta">
            <span class="pill-badge-lesson-count">
              {{ chapter.sections?.length || 0 }} 个微课节
            </span>

            <template v-if="courseEditable">
              <button
                type="button"
                class="capsule-ch-action-btn"
                title="手工录入微课节"
                @click.stop="openAddSectionModal(chapter)"
              >
                <el-icon><Plus /></el-icon>
                <span>加课节</span>
              </button>

              <button
                type="button"
                class="capsule-ch-action-btn capsule-ch-action-btn--ai"
                title="AI 智能生成微课大纲"
                @click.stop="openAiGenerateModal(chapter)"
              >
                <span>AI规划</span>
              </button>

              <el-dropdown trigger="click" @command="(cmd: string) => handleChapterCommand(cmd, chapter)">
                <button type="button" class="capsule-ch-action-btn ch-more-btn" @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑章节信息</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: #EF4444;">删除本大纲章节</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>

            <div class="expand-arrow-circle">
              <svg
                viewBox="0 0 24 24"
                class="arrow-svg"
                :class="{ rotated: openChapters.includes(chapter.id) }"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <!-- 展开后的小节列表 -->
        <div v-show="openChapters.includes(chapter.id)" class="sections-drawer">
          <!-- 存在微课节列表 -->
          <div v-if="chapter.sections && chapter.sections.length > 0" class="sections-list-inner">
            <div
              v-for="sec in chapter.sections"
              :key="sec.id"
              class="section-item-row"
            >
              <!-- 状态与图标 -->
              <div class="sec-left">
                <div
                  class="sec-status-icon"
                  :class="{ completed: sec.completed }"
                  :title="sec.completed ? '已完成学习' : '未完成'"
                >
                  <el-icon v-if="sec.completed"><Check /></el-icon>
                  <span v-else>•</span>
                </div>
                <div class="sec-meta-col">
                  <span class="sec-title">{{ sec.title }}</span>
                  <div class="sec-sub-tags">
                    <span v-if="sec.duration" class="pill-mini-tag">
                      <el-icon><Clock /></el-icon> {{ sec.duration }}
                    </span>
                    <span v-if="sec.knowledgePointCount" class="pill-mini-tag">
                      <el-icon><Connection /></el-icon> {{ sec.knowledgePointCount }} 个考查知识点
                    </span>
                    <span v-if="sec.type === 'quiz'" class="pill-mini-tag pill-mini-tag--quiz">
                      <el-icon><EditPen /></el-icon> 智能自测
                    </span>
                  </div>
                </div>
              </div>

              <!-- 右侧快捷按钮 -->
              <div class="sec-actions">
                <button
                  type="button"
                  class="capsule-sec-btn capsule-sec-btn--ai"
                  @click.stop="handleAiExplain(sec.title)"
                >
                  <span>AI 辅导</span>
                </button>
                <button
                  type="button"
                  class="capsule-sec-btn capsule-sec-btn--study"
                  :class="{ completed: sec.completed }"
                  @click.stop="handleStartStudy(sec)"
                >
                  <span>{{ sec.completed ? '重新复习' : '开始学习' }}</span>
                </button>
                <button
                  v-if="courseEditable"
                  type="button"
                  class="capsule-sec-btn capsule-sec-btn--del"
                  title="删除微课节"
                  @click.stop="confirmDeleteSection(chapter, sec)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>

            <!-- 底部微课节快捷追加栏 -->
            <div v-if="courseEditable" class="sections-bottom-bar">
              <button
                type="button"
                class="capsule-mini-add-btn"
                @click="openAddSectionModal(chapter)"
              >
                <el-icon><Plus /></el-icon>
                <span>继续录入微课节</span>
              </button>
              <button
                type="button"
                class="capsule-mini-add-btn capsule-mini-add-btn--ai"
                @click="openAiGenerateModal(chapter)"
              >
                <el-icon><MagicStick /></el-icon>
                <span>AI 扩充微课大纲</span>
              </button>
            </div>
          </div>

          <!-- 0 个微课节时的空状态 -->
          <div v-else class="empty-chapter-sections">
            <div class="empty-sparkle-icon">
              <el-icon><Opportunity /></el-icon>
            </div>
            <div class="empty-text-meta">
              <h4>本章暂无微课节与课时安排</h4>
              <p>您可以手动录入课时微课节，或直接调用 AI 助教一键生成体系化微课时。</p>
            </div>
            <div v-if="courseEditable" class="empty-action-group">
              <button
                type="button"
                class="capsule-empty-btn capsule-empty-btn--primary"
                @click.stop="openAddSectionModal(chapter)"
              >
                <el-icon><Plus /></el-icon>
                <span>手工录入微课节</span>
              </button>
              <button
                type="button"
                class="capsule-empty-btn capsule-empty-btn--ai"
                @click.stop="openAiGenerateModal(chapter)"
              >
                <span>AI 智能生成微课大纲</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑大纲章节抽屉 -->
    <el-drawer
      v-model="showAddChapterDrawer"
      :title="editingChapter ? '编辑教学大纲章节' : '录入新教学大纲章节'"
      size="480px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="章节主标题" required>
          <el-input v-model="newChapterTitle" placeholder="例如：第四章 树与二叉树算法实现" />
        </el-form-item>
        <el-form-item label="章节概要说明">
          <el-input
            v-model="newChapterDesc"
            type="textarea"
            :rows="3"
            placeholder="简述本章节的核心教学目标与知识架构..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button type="button" class="capsule-dialog-btn" @click="closeAddChapterDrawer">取消</button>
        <button type="button" class="capsule-dialog-btn capsule-dialog-btn--primary" @click="handleSaveNewChapter">
          {{ editingChapter ? '保存修改' : '确认录入' }}
        </button>
      </template>
    </el-drawer>

    <!-- 微课节录入/编辑弹窗 -->
    <ChapterSectionDialog
      v-model="showSectionDialog"
      :chapter-id="activeChapter?.id || 0"
      :chapter-title="activeChapter?.title || ''"
      :initial-data="editingSection"
      @submit="handleSectionSubmit"
    />

    <!-- AI 微课节大纲智能生成弹窗 -->
    <ChapterAiGenerateModal
      v-model="showAiGenerateModal"
      :chapter="activeChapter"
      @apply="handleAiApplySections"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Search,
  MagicStick,
  Check,
  Clock,
  Connection,
  EditPen,
  Plus,
  MoreFilled,
  Delete,
  Opportunity
} from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';
import { useCourse } from '@/composables/course/useCourse';
import { useCourseEditable } from '@/composables/course/useCourseEditable';
import ChapterSectionDialog from '@/components/course/ChapterSectionDialog.vue';
import ChapterAiGenerateModal from '@/components/course/ChapterAiGenerateModal.vue';

const props = defineProps<{
  course?: Course | null;
}>();

const route = useRoute();
const router = useRouter();
const courseEditable = useCourseEditable(() => props.course);
const courseId = computed(() => {
  if (route.params.id) return String(route.params.id);
  if (props.course?.id) return String(props.course.id);
  return '';
});

const {
  chapters,
  fetchChapters,
  createChapter,
  updateChapter,
  removeChapter,
  createSection,
  removeSection
} = useCourse();

const searchChapterText = ref('');
const openChapters = ref<number[]>([]);

const showAddChapterDrawer = ref(false);
const editingChapter = ref<any>(null);
const newChapterTitle = ref('');
const newChapterDesc = ref('');

// 微课节弹窗
const showSectionDialog = ref(false);
const activeChapter = ref<any>(null);
const editingSection = ref<any>(null);

// AI 生成微课节弹窗
const showAiGenerateModal = ref(false);

function openAddChapterDrawer() {
  editingChapter.value = null;
  newChapterTitle.value = '';
  newChapterDesc.value = '';
  showAddChapterDrawer.value = true;
}

function closeAddChapterDrawer() {
  showAddChapterDrawer.value = false;
  editingChapter.value = null;
}

function openAddSectionModal(chapter: any) {
  activeChapter.value = chapter;
  editingSection.value = null;
  showSectionDialog.value = true;
}

function openAiGenerateModal(chapter: any) {
  activeChapter.value = chapter;
  showAiGenerateModal.value = true;
}

async function handleChapterCommand(cmd: string, chapter: any) {
  if (cmd === 'edit') {
    editingChapter.value = chapter;
    newChapterTitle.value = chapter.title;
    newChapterDesc.value = chapter.description || '';
    showAddChapterDrawer.value = true;
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定要删除大纲章节「${chapter.title}」吗？删除后该章节下的所有微课节将同步被级联移除，且无法恢复。`,
        '删除确认',
        {
          type: 'warning',
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          confirmButtonClass: 'el-button--danger',
          lockScroll: false
        }
      );
      await handleDeleteChapter(chapter.id);
    } catch {
      // 用户取消删除
    }
  }
}

async function confirmDeleteSection(chapter: any, sec: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除微课节「${sec.title}」吗？删除后将无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        lockScroll: false
      }
    );
    await handleDeleteSection(chapter, sec.id);
  } catch {
    // 用户取消删除
  }
}

async function handleSectionSubmit(data: any) {
  if (!activeChapter.value?.id) return;
  const cId = Number(courseId.value);
  try {
    await createSection(cId, activeChapter.value.id, {
      title: data.title,
      sortOrder: (activeChapter.value.sections?.length || 0) + 1
    });
    ElMessage.success('微课节已成功录入大纲！');
    if (!openChapters.value.includes(activeChapter.value.id)) {
      openChapters.value.push(activeChapter.value.id);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '录入微课节失败');
  }
}

async function handleAiApplySections(sections: any[]) {
  if (!activeChapter.value?.id) return;
  const cId = Number(courseId.value);
  try {
    for (let i = 0; i < sections.length; i++) {
      await createSection(cId, activeChapter.value.id, {
        title: sections[i].title,
        sortOrder: (activeChapter.value.sections?.length || 0) + i + 1
      });
    }
    ElMessage.success(`AI 已为本章导入 ${sections.length} 个微课节！`);
    if (!openChapters.value.includes(activeChapter.value.id)) {
      openChapters.value.push(activeChapter.value.id);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '批量导入微课节失败');
  }
}

async function handleDeleteSection(chapter: any, sectionId: number) {
  const cId = Number(courseId.value);
  try {
    await removeSection(cId, sectionId);
    ElMessage.success('微课节已移除');
  } catch (err: any) {
    ElMessage.error(err?.message || '删除微课节失败');
  }
}

async function handleDeleteChapter(chapterId: number) {
  const cId = Number(courseId.value);
  try {
    await removeChapter(cId, chapterId);
    ElMessage.success('章节已成功删除');
  } catch (err: any) {
    ElMessage.error(err?.message || '删除章节失败');
  }
}

const filteredChapters = computed(() => {
  if (!searchChapterText.value.trim()) return chapters.value;
  const kw = searchChapterText.value.trim().toLowerCase();
  return chapters.value.filter(ch => {
    const inChapter = ch.title.toLowerCase().includes(kw) || (ch.description && ch.description.toLowerCase().includes(kw));
    const inSection = (ch.sections || []).some(s => s.title.toLowerCase().includes(kw));
    return inChapter || inSection;
  });
});

const totalLessonCount = computed(() => {
  return chapters.value.reduce((acc, cur) => acc + (cur.sections?.length || 0), 0);
});

const completedLessonCount = computed(() => {
  let count = 0;
  chapters.value.forEach(ch => {
    (ch.sections || []).forEach(s => {
      if (s.completed) count++;
    });
  });
  return count;
});

const isAllExpanded = computed(() => {
  return openChapters.value.length === chapters.value.length && chapters.value.length > 0;
});

function toggleChapter(id: number) {
  const idx = openChapters.value.indexOf(id);
  if (idx > -1) {
    openChapters.value.splice(idx, 1);
  } else {
    openChapters.value.push(id);
  }
}

function toggleAllCollapse() {
  if (isAllExpanded.value) {
    openChapters.value = [];
  } else {
    openChapters.value = chapters.value.map(c => c.id);
  }
}

function handleChapterAiQuiz() {
  router.push(`/ai/question/generate?courseId=${courseId.value}`);
}

function handleAiExplain(secTitle: string) {
  router.push({
    path: `/course/${courseId.value}/ai`,
    query: { prompt: `请结合本课程大纲，深度解析微课时核心内容：${secTitle}` }
  });
}

function handleStartStudy(sec: any) {
  if (sec.type === 'quiz') {
    router.push(`/ai/question/generate?courseId=${courseId.value}`);
  } else {
    router.push({
      path: `/course/${courseId.value}/ai`,
      query: { prompt: `请针对课时【${sec.title}】的重点概念、定理公式与代码实现进行苏格拉底式精细辅导。` }
    });
  }
}

async function handleSaveNewChapter() {
  if (!newChapterTitle.value.trim()) {
    ElMessage.warning('章节标题不能为空');
    return;
  }
  const cId = Number(courseId.value);
  const title = newChapterTitle.value.trim();

  try {
    if (editingChapter.value?.id) {
      await updateChapter(cId, editingChapter.value.id, {
        title
      });
      ElMessage.success('章节信息已修改！');
    } else {
      const sort = chapters.value.length + 1;
      const res = await createChapter(cId, {
        title,
        parentId: 0,
        sortOrder: sort
      });
      ElMessage.success('新章节已成功持久化存入教学大纲！');
      if (res) {
        openChapters.value.push(Number(res));
      }
    }
    closeAddChapterDrawer();
    newChapterTitle.value = '';
    newChapterDesc.value = '';
    await fetchChapters(cId);
  } catch (err: any) {
    ElMessage.error(err?.message || '保存章节失败');
  }
}

onMounted(async () => {
  await fetchChapters(courseId.value as string);
  // 默认展开前两章
  if (chapters.value.length > 0) {
    openChapters.value = chapters.value.slice(0, 2).map(c => c.id);
  }
});
</script>

<style scoped lang="scss">
.course-chapters-page {
  display: flex;
  flex-direction: column;
  gap: 18px;

  // 1. 顶部大纲工具栏
  .chapters-top-toolbar {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 18px 24px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    .syllabus-summary {
      .syllabus-title {
        margin: 0 0 6px 0;
        font-size: 17px;
        font-weight: 700;
        color: #0F172A;
      }

      .summary-pill-group {
        display: flex;
        align-items: center;
        gap: 8px;

        .pill-badge {
          display: inline-block;
          padding: 2px 10px;
          border-radius: 9999px; // 长圆跑道
          font-size: 11.5px;
          font-weight: 600;

          &--primary {
            background: #EAF3FF;
            color: #1677FF;
          }

          &--neutral {
            background: #F1F5F9;
            color: #475569;
          }
        }
      }
    }

    .toolbar-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .capsule-search-mini {
        display: flex;
        align-items: center;
        width: 210px;
        height: 36px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 9999px;
        padding: 0 14px;
        transition: all 0.2s;

        &:focus-within {
          border-color: #1677FF;
          background: #FFFFFF;
          box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
        }

        .search-mini-icon {
          font-size: 12px;
          margin-right: 6px;
        }

        .search-mini-input {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 12.5px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
            font-size: 12px;
          }
        }
      }

      .capsule-tool-btn {
        height: 36px;
        padding: 0 16px;
        border-radius: 9999px; // 长圆跑道胶囊
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        color: #475569;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: #CBD5E1;
          color: #1677FF;
          background: #F8FAFC;
        }

        &--ai {
          background: linear-gradient(135deg, #EEF2FF 0%, #FAF5FF 100%);
          border: 1px solid #C7D2FE;
          color: #4F46E5;
          font-weight: 600;

          &:hover {
            background: #E0E7FF;
            color: #4338CA;
            transform: translateY(-1px);
          }
        }
      }
    }
  }

  // 2. 章节卡片手风琴
  .chapters-accordion-list {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .chapter-accordion-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #EBF1F7;
      overflow: hidden;
      box-shadow: 0 3px 12px rgba(30, 80, 150, 0.03);
      transition: all 0.22s ease;

      &.is-open {
        border-color: #DBEAFE;
        box-shadow: 0 6px 20px rgba(22, 119, 255, 0.07);
      }

      .chapter-card-header {
        padding: 18px 24px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        cursor: pointer;
        user-select: none;
        background: #FFFFFF;
        transition: background 0.2s;

        &:hover {
          background: #F8FAFC;
        }

        .header-left-info {
          display: flex;
          align-items: center;
          gap: 16px;

          .chapter-num-badge {
            width: 36px;
            height: 36px;
            border-radius: 12px;
            background: #EFF6FF;
            color: #1677FF;
            font-size: 14px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
          }

          .chapter-title-box {
            .chapter-title {
              margin: 0;
              font-size: 15.5px;
              font-weight: 700;
              color: #0F172A;
            }

            .chapter-desc {
              margin: 4px 0 0 0;
              font-size: 12.5px;
              color: #64748B;
              line-height: 1.4;
            }
          }
        }

        .header-right-meta {
          display: flex;
          align-items: center;
          gap: 14px;

          .pill-badge-lesson-count {
            padding: 2px 10px;
            border-radius: 9999px;
            background: #F1F5F9;
            color: #64748B;
            font-size: 12px;
            font-weight: 500;
          }

          .expand-arrow-circle {
            width: 28px;
            height: 28px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #F1F5F9;
            color: #64748B;
            transition: all 0.2s;

            .arrow-svg {
              width: 14px;
              height: 14px;
              transition: transform 0.22s ease;

              &.rotated {
                transform: rotate(180deg);
              }
            }
          }
        }
      }

      // 抽屉内小节列表
      .sections-drawer {
        padding: 0 24px 14px;
        background: #FAFCFE;
        border-top: 1px solid #F1F5F9;

        .section-item-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 12px 16px;
          margin-top: 10px;
          background: #FFFFFF;
          border-radius: 12px;
          border: 1px solid #EDF2F7;
          transition: all 0.2s;

          &:hover {
            border-color: #CBD5E1;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
          }

          .sec-left {
            display: flex;
            align-items: center;
            gap: 14px;
            min-width: 0;

            .sec-status-icon {
              width: 22px;
              height: 22px;
              border-radius: 50%;
              border: 1.5px solid #CBD5E1;
              color: #94A3B8;
              font-size: 11px;
              font-weight: 700;
              display: flex;
              align-items: center;
              justify-content: center;
              flex-shrink: 0;

              &.completed {
                background: #10B981;
                border-color: #10B981;
                color: #FFFFFF;
              }
            }

            .sec-meta-col {
              display: flex;
              flex-direction: column;
              gap: 4px;

              .sec-title {
                font-size: 13.5px;
                font-weight: 600;
                color: #1E293B;
              }

              .sec-sub-tags {
                display: flex;
                align-items: center;
                gap: 8px;

                .pill-mini-tag {
                  font-size: 11px;
                  color: #64748B;
                  background: #F1F5F9;
                  padding: 1px 8px;
                  border-radius: 9999px;

                  &--quiz {
                    background: #FEF3C7;
                    color: #D97706;
                  }
                }
              }
            }
          }

          .sec-actions {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-shrink: 0;

            .capsule-sec-btn {
              height: 30px;
              padding: 0 14px;
              border-radius: 9999px; // 长圆小按钮
              font-size: 12px;
              font-weight: 500;
              cursor: pointer;
              transition: all 0.2s ease;
              border: none;

              &--ai {
                background: #EEF2FF;
                color: #4F46E5;

                &:hover {
                  background: #E0E7FF;
                }
              }

              &--study {
                background: #1677FF;
                color: #FFFFFF;

                &.completed {
                  background: #F1F5F9;
                  color: #475569;
                }

                &:hover {
                  opacity: 0.9;
                  transform: translateY(-1px);
                }
              }

              &--del {
                background: transparent;
                color: #94A3B8;
                padding: 0 8px;

                &:hover {
                  color: #EF4444;
                  background: #FEE2E2;
                }
              }
            }
          }
        }

        // 底部快捷追加栏
        .sections-bottom-bar {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-top: 14px;
          padding-top: 10px;
          border-top: 1px dashed #E2E8F0;

          .capsule-mini-add-btn {
            height: 30px;
            padding: 0 14px;
            border-radius: 9999px;
            background: #FFFFFF;
            border: 1px dashed #CBD5E1;
            color: #475569;
            font-size: 12px;
            font-weight: 500;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            transition: all 0.2s;

            &:hover {
              border-color: #3B82F6;
              color: #2563EB;
              background: #EFF6FF;
            }

            &--ai {
              background: linear-gradient(135deg, #F0FDF4 0%, #EFF6FF 100%);
              border-color: #86EFAC;
              color: #15803D;

              &:hover {
                background: #DCFCE7;
                color: #166534;
                border-color: #4ADE80;
              }
            }
          }
        }

        // 0 个微课节空状态
        .empty-chapter-sections {
          padding: 28px 20px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;
          background: #FFFFFF;
          border-radius: 12px;
          border: 1px dashed #E2E8F0;
          margin-top: 12px;

          .empty-sparkle-icon {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: #EFF6FF;
            color: #3B82F6;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
          }

          .empty-text-meta {
            h4 {
              font-size: 14.5px;
              font-weight: 700;
              color: #0F172A;
              margin: 0 0 4px 0;
            }
            p {
              font-size: 12.5px;
              color: #64748B;
              margin: 0;
            }
          }

          .empty-action-group {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-top: 4px;

            .capsule-empty-btn {
              height: 34px;
              padding: 0 16px;
              border-radius: 9999px;
              font-size: 12.5px;
              font-weight: 600;
              cursor: pointer;
              display: inline-flex;
              align-items: center;
              gap: 6px;
              transition: all 0.2s;

              &--primary {
                background: #EFF6FF;
                border: 1px solid #BFDBFE;
                color: #2563EB;

                &:hover {
                  background: #DBEAFE;
                  color: #1D4ED8;
                }
              }

              &--ai {
                background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
                color: #FFFFFF;
                border: none;
                box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

                &:hover {
                  transform: translateY(-1px);
                  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
                }
              }
            }
          }
        }
      }
    }
  }
}

.capsule-ch-action-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 9999px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;

  &:hover {
    background: #EFF6FF;
    color: #2563EB;
    border-color: #BFDBFE;
  }

  &--ai {
    background: #EEF2FF;
    border-color: #C7D2FE;
    color: #4F46E5;
    font-weight: 600;

    &:hover {
      background: #E0E7FF;
      color: #4338CA;
    }
  }

  &.ch-more-btn {
    padding: 0 8px;
  }
}

.capsule-tool-btn--primary {
  background: #1677FF !important;
  color: #FFFFFF !important;
  border-color: #1677FF !important;

  &:hover {
    background: #4096FF !important;
    color: #FFFFFF !important;
  }
}

.capsule-dialog-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: 9999px;
  border: 1px solid #E2E8F0;
  background: #FFFFFF;
  color: #475569;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #F8FAFC;
    border-color: #CBD5E1;
  }

  &--primary {
    background: #1677FF;
    color: #FFFFFF;
    border-color: #1677FF;

    &:hover {
      background: #4096FF;
      border-color: #4096FF;
    }
  }
}
</style>
