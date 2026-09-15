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
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增大纲章节抽屉 -->
    <el-drawer
      v-model="showAddChapterDrawer"
      title="录入新教学大纲章节"
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
        <el-form-item label="微课节数预设">
          <el-input-number v-model="newChapterSectionCount" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button type="button" class="capsule-dialog-btn" @click="closeAddChapterDrawer">取消</button>
        <button type="button" class="capsule-dialog-btn capsule-dialog-btn--primary" @click="handleSaveNewChapter">确认录入</button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, MagicStick, Check, Clock, Connection, EditPen, Plus } from '@element-plus/icons-vue';
import { useCourse } from '@/composables/course/useCourse';

const route = useRoute();
const router = useRouter();
const courseId = computed(() => route.params.id || '101');

const { chapters, fetchChapters } = useCourse();

const searchChapterText = ref('');
const openChapters = ref<number[]>([]);

const showAddChapterDrawer = ref(false);
const newChapterTitle = ref('');
const newChapterDesc = ref('');
const newChapterSectionCount = ref(3);

function openAddChapterDrawer() {
  showAddChapterDrawer.value = true;
}

function closeAddChapterDrawer() {
  showAddChapterDrawer.value = false;
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

function handleSaveNewChapter() {
  if (!newChapterTitle.value.trim()) {
    ElMessage.warning('章节标题不能为空');
    return;
  }
  const newId = Date.now();
  const subSections = Array.from({ length: newChapterSectionCount.value }, (_, i) => ({
    id: newId * 10 + i + 1,
    title: `${newChapterTitle.value.trim()} - 核心知识讲义 ${i + 1}`,
    completed: false,
    duration: '45分钟',
    knowledgePointCount: 2,
    type: i === newChapterSectionCount.value - 1 ? 'quiz' : 'lecture'
  }));

  chapters.value.push({
    id: newId,
    courseId: Number(courseId.value),
    title: newChapterTitle.value.trim(),
    sort: chapters.value.length + 1,
    description: newChapterDesc.value.trim() || '本章涵盖学科核心理论基础与典型案例解析。',
    sections: subSections
  });

  openChapters.value.push(newId);
  ElMessage.success('新章节已成功加入教学大纲！');
  closeAddChapterDrawer();
  newChapterTitle.value = '';
  newChapterDesc.value = '';
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
            }
          }
        }
      }
    }
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
