<template>
  <div class="kp-tab-container">
    <!-- 工具筛选条 -->
    <div class="kp-toolbar-card">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识点名称、编码..."
          clearable
          :prefix-icon="Search"
          class="filter-search-input"
        />
        <el-select
          v-model="selectedChapterId"
          placeholder="全部章节"
          clearable
          class="filter-chapter-select"
          @change="loadKnowledgePoints"
        >
          <el-option label="全部章节目录" :value="undefined" />
          <el-option
            v-for="chap in chapters"
            :key="chap.id"
            :label="chap.title"
            :value="chap.id"
          />
        </el-select>
        <el-select v-model="selectedLevel" placeholder="认知维度" clearable class="filter-level-select">
          <el-option label="全部维度" value="" />
          <el-option label="识记 (Remember)" value="REMEMBER" />
          <el-option label="理解 (Understand)" value="UNDERSTAND" />
          <el-option label="应用 (Apply)" value="APPLY" />
          <el-option label="综合探究 (Analyze)" value="ANALYZE" />
        </el-select>
      </div>

      <div class="toolbar-right">
        <el-button class="capsule-btn capsule-btn--secondary" @click="openGraphDrawer(null)">
          <el-icon><Connection /></el-icon>
          <span>查看课程全景拓扑</span>
        </el-button>
        <el-button v-if="editable" class="capsule-btn capsule-btn--ai" @click="openAiSuggestModal">
          <el-icon><MagicStick /></el-icon>
          <span>AI 提炼考点</span>
        </el-button>
        <el-button v-if="editable" class="capsule-btn capsule-btn--primary" @click="showCreateDrawer = true">
          <el-icon><Plus /></el-icon>
          <span>新增知识点</span>
        </el-button>
      </div>
    </div>

    <!-- 知识点卡片与网格列表 -->
    <div v-loading="loading" class="kp-grid-wrapper">
      <div v-if="filteredPoints.length > 0" class="kp-cards-grid">
        <div
          v-for="(kp, index) in filteredPoints"
          :key="kp.id || index"
          class="kp-card-item"
        >
          <div class="card-head">
            <div class="badge-row">
              <span class="kp-code font-mono">{{ kp.code || `KP-${100 + (kp.id || index)}` }}</span>
              <el-tag size="small" :type="getLevelTagType(kp.cognitiveDimension)">
                {{ getLevelLabel(kp.cognitiveDimension) }}
              </el-tag>
            </div>
            <div class="star-rating">
              <span v-for="s in (kp.importance || 3)" :key="s" class="star">
                <el-icon><StarFilled /></el-icon>
              </span>
            </div>
          </div>

          <h3 class="kp-name">{{ kp.title || kp.name }}</h3>
          <p class="kp-desc">{{ kp.description || '本知识点为课程大纲的核心重点内容，涉及算法设计、概念理解与综合实战考查。' }}</p>

          <div v-if="kp.prerequisites && kp.prerequisites.length" class="prereq-row">
            <span class="prereq-label">前置要求：</span>
            <div class="prereq-tags">
              <span v-for="pre in kp.prerequisites" :key="pre" class="pre-tag">{{ pre }}</span>
            </div>
          </div>

          <!-- 所属章节独立展示行 -->
          <div class="card-chapter-row">
            <el-icon class="chapter-icon"><Reading /></el-icon>
            <span class="chapter-label">所属章节：</span>
            <span class="chapter-title" :title="getChapterTitle(kp.chapterId)">
              {{ getChapterTitle(kp.chapterId) }}
            </span>
          </div>

          <!-- 底部专属操作按钮栏：独立新排整洁展示 -->
          <div class="card-actions-bar">
            <div class="actions-left">
              <el-button
                size="small"
                class="card-action-btn card-action-btn--graph"
                @click="openGraphDrawer(kp)"
              >
                <el-icon><Connection /></el-icon>
                <span>关联图谱</span>
              </el-button>
              <el-button
                size="small"
                class="card-action-btn card-action-btn--ai"
                @click="handleAskAi(kp)"
              >
                <el-icon><Cpu /></el-icon>
                <span>AI解析</span>
              </el-button>
            </div>
            <div v-if="editable" class="actions-right">
              <el-button
                size="small"
                class="card-action-btn card-action-btn--danger"
                @click="confirmDeleteKp(kp)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state-panel">
        <div class="empty-icon">
          <el-icon><Opportunity /></el-icon>
        </div>
        <h3>当前章节暂无录入的知识点</h3>
        <p>您可以点击上方“新增知识点”，丰富课程的知识体系拓扑结构。</p>
      </div>
    </div>

    <!-- 知识拓扑全景抽屉 -->
    <el-drawer
      v-model="showGraphDrawer"
      title="课程核心知识拓扑全景图谱"
      size="680px"
      destroy-on-close
    >
      <div class="graph-drawer-content">
        <div class="graph-info-header">
          <span class="badge-pill">拓扑节点总数：{{ knowledgePoints.length + chapters.length + 1 }}</span>
          <span class="badge-pill">大纲章节：{{ chapters.length }}</span>
          <span class="badge-pill">核心考点：{{ knowledgePoints.length }}</span>
        </div>

        <!-- 交互式拓扑图模拟树 -->
        <div class="topology-tree-container">
          <!-- 根节点：课程核心 -->
          <div class="topo-root-node">
            <div class="topo-node-card is-root">
              <el-icon><Connection /></el-icon>
              <span>课程知识根基空间</span>
            </div>
          </div>

          <!-- 章节分支 -->
          <div class="topo-branches">
            <div
              v-for="chap in chapters"
              :key="chap.id"
              class="topo-branch-column"
            >
              <div class="topo-chapter-node">
                <span class="chapter-tag">{{ chap.title }}</span>
              </div>
              <div class="topo-kp-leaves">
                <div
                  v-for="point in getPointsForChapter(chap.id)"
                  :key="point.id"
                  class="topo-kp-leaf"
                  :class="{ active: selectedGraphKp?.id === point.id }"
                  @click="selectedGraphKp = point"
                >
                  <span class="leaf-dot"></span>
                  <span class="leaf-title">{{ point.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 当前选中知识点详情面板 -->
        <div v-if="selectedGraphKp" class="active-node-panel">
          <div class="active-header">
            <h4>{{ selectedGraphKp.title }}</h4>
            <el-tag size="small" :type="getLevelTagType(selectedGraphKp.cognitiveDimension)">
              {{ getLevelLabel(selectedGraphKp.cognitiveDimension) }}
            </el-tag>
          </div>
          <p class="active-desc">
            {{ selectedGraphKp.description || '该知识点为所在章节的核心考查要点，直接关联课程期末诊断与作业训练。' }}
          </p>
          <div class="active-actions">
            <button
              type="button"
              class="capsule-btn-action capsule-btn-action--ai"
              @click="handleAskAi(selectedGraphKp)"
            >
              向 AI 助教提问此考点
            </button>
            <button
              type="button"
              class="capsule-btn-action capsule-btn-action--quiz"
              @click="handleGenerateQuizForKp(selectedGraphKp)"
            >
              针对该考点出题练习
            </button>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 录入知识点抽屉 -->
    <el-drawer v-model="showCreateDrawer" title="录入课程核心知识点" size="540px" destroy-on-close class="kp-drawer">
      <template #header>
        <div class="drawer-header-flex">
          <div class="header-icon-circle">
            <el-icon><Connection /></el-icon>
          </div>
          <div>
            <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: #0F172A;">录入课程核心考查知识点</h3>
            <p style="margin: 2px 0 0 0; font-size: 12px; color: #64748B;">构建知识图谱拓扑节点，支持 AI 助教知识溯源与智能出题</p>
          </div>
        </div>
      </template>

      <!-- AI 智能推荐横幅 -->
      <div class="kp-ai-helper-banner" @click="openAiSuggestModal">
        <div class="ai-spark-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <div class="ai-spark-meta">
          <strong>使用 AI 辅助智能提炼核心考点</strong>
          <p>基于当前章节大纲，AI 智能提炼核心概念、认知目标维度与考查要点</p>
        </div>
        <button type="button" class="spark-call-btn">AI提取</button>
      </div>

      <el-form label-position="top" class="kp-create-form">
        <el-form-item label="知识点名称" required>
          <el-input
            v-model="newKp.title"
            placeholder="例如：双向链表插入与删除节点算法"
            maxlength="60"
            show-word-limit
          />
        </el-form-item>

        <div class="form-grid-2">
          <el-form-item label="所属课程章节" required>
            <el-select v-model="newKp.chapterId" placeholder="选择所属章节" class="w-full">
              <el-option
                v-for="(chap, idx) in chapters"
                :key="chap.id"
                :label="`第 ${idx + 1} 章：${chap.title}`"
                :value="chap.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="认知目标维度">
            <el-select v-model="newKp.cognitiveDimension" class="w-full">
              <el-option label="识记概念 (Remember)" value="REMEMBER" />
              <el-option label="理解领会 (Understand)" value="UNDERSTAND" />
              <el-option label="实践应用 (Apply)" value="APPLY" />
              <el-option label="分析综合 (Analyze)" value="ANALYZE" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid-2">
          <el-form-item label="核心考查重要度">
            <el-rate v-model="newKp.importance" :max="5" />
          </el-form-item>

          <el-form-item label="考查易错陷阱与重点">
            <el-input
              v-model="newKp.examFocus"
              placeholder="例如：边界指针判空、断链死循环"
              maxlength="40"
            />
          </el-form-item>
        </div>

        <el-form-item label="核心考点与概念阐述">
          <el-input
            v-model="newKp.description"
            type="textarea"
            :rows="4"
            placeholder="详细说明该知识点需掌握的概念、推导要求与代码实现细节..."
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer-actions">
          <el-button class="capsule-btn capsule-btn--secondary" @click="showCreateDrawer = false">
            取消
          </el-button>
          <el-button
            type="primary"
            class="capsule-btn capsule-btn--primary"
            :loading="creating"
            :disabled="creating || !newKp.title.trim()"
            @click="handleSaveNewKp"
          >
            <span>{{ creating ? '保存入库中...' : '确认添加知识点' }}</span>
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- AI 知识点智能提炼与推荐弹窗 -->
    <el-dialog
      v-model="showAiSuggestModal"
      title="AI 智能考点提炼与推荐"
      width="640px"
      append-to-body
      destroy-on-close
      class="ai-suggest-dialog"
    >
      <div class="ai-dialog-intro">
        <div class="spark-badge">
          <el-icon><MagicStick /></el-icon>
          <span>AI 课程知识图谱引擎</span>
        </div>
        <p>
          AI 已结合本课程大纲与高等教育教学大纲规范，为你智能提炼出以下核心考点。你可以选择单条一键回填到表单，或直接一键批量录入到知识图谱：
        </p>
      </div>

      <div v-if="aiExtracting" class="ai-loading-box">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 正在研读章节大纲并提炼考点体系...</span>
      </div>

      <div v-else class="ai-points-list">
        <div
          v-for="(p, pIdx) in aiSuggestedPoints"
          :key="pIdx"
          class="ai-point-card"
        >
          <div class="point-top-row">
            <span class="point-num">考点 {{ pIdx + 1 }}</span>
            <strong class="point-title">{{ p.title }}</strong>
            <el-tag size="small" :type="getLevelTagType(p.cognitiveDimension)">
              {{ getLevelLabel(p.cognitiveDimension) }}
            </el-tag>
          </div>

          <p class="point-desc">{{ p.description }}</p>

          <div class="point-meta-row">
            <span v-if="p.examFocus" class="focus-tag">
              考查重点：{{ p.examFocus }}
            </span>
            <div class="stars">
              <span v-for="s in p.importance" :key="s">★</span>
            </div>
          </div>

          <div class="card-apply-action">
            <el-button
              size="small"
              class="point-apply-btn"
              @click="applyAiSuggestedPoint(p)"
            >
              <el-icon><EditPen /></el-icon>
              <span>采纳并编辑</span>
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="ai-modal-footer">
          <el-button
            class="capsule-btn capsule-btn--secondary"
            :loading="aiExtracting"
            :disabled="aiExtracting"
            @click="generateAiSuggestedPoints"
          >
            <el-icon><Refresh /></el-icon>
            <span>换一批考点</span>
          </el-button>
          <div class="right-group">
            <el-button class="capsule-btn capsule-btn--secondary" @click="showAiSuggestModal = false">
              关闭
            </el-button>
            <el-button
              type="primary"
              class="capsule-btn capsule-btn--ai"
              :disabled="aiExtracting || aiSuggestedPoints.length === 0"
              @click="batchImportAiPoints(aiSuggestedPoints)"
            >
              <span>一键批量导入所有考点 ({{ aiSuggestedPoints.length }})</span>
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  Search,
  Plus,
  StarFilled,
  Opportunity,
  Connection,
  MagicStick,
  Loading,
  Refresh,
  Reading,
  Cpu,
  Delete,
  EditPen
} from '@element-plus/icons-vue';
import { useKnowledgePoint } from '@/composables/course/useKnowledgePoint';

const props = withDefaults(
  defineProps<{
    editable?: boolean;
  }>(),
  { editable: false }
);

const {
  loading,
  creating,
  showCreateDrawer,
  showGraphDrawer,
  selectedGraphKp,
  searchKeyword,
  selectedChapterId,
  selectedLevel,
  chapters,
  knowledgePoints,
  newKp,
  filteredPoints,
  showAiSuggestModal,
  aiExtracting,
  aiSuggestedPoints,
  openAiSuggestModal,
  generateAiSuggestedPoints,
  applyAiSuggestedPoint,
  batchImportAiPoints,
  loadKnowledgePoints,
  getChapterTitle,
  getPointsForChapter,
  getLevelLabel,
  getLevelTagType,
  handleSaveNewKp,
  openGraphDrawer,
  handleAskAi,
  handleGenerateQuizForKp,
  handleDeleteKp,
  confirmDeleteKp,
} = useKnowledgePoint();
</script>

<style scoped lang="scss">
.kp-tab-container {
  padding: 8px 0;

  .kp-toolbar-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 20px;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      flex: 1;
      min-width: 320px;

      .filter-search-input {
        width: 240px;
        flex-shrink: 0;
      }

      .filter-chapter-select {
        width: 200px;
        flex-shrink: 0;
      }

      .filter-level-select {
        width: 140px;
        flex-shrink: 0;
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;

      .capsule-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 18px;
        border-radius: 9999px;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

        &--secondary {
          background: #ffffff !important;
          color: #334155 !important;
          border: 1.5px solid #cbd5e1 !important;

          &:hover {
            background: #f8fafc !important;
            color: #2563eb !important;
            border-color: #93c5fd !important;
            transform: translateY(-1px);
          }
        }

        &--ai {
          background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%) !important;
          color: #ffffff !important;
          border: none !important;
          box-shadow: 0 4px 14px rgba(124, 58, 237, 0.25);

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(124, 58, 237, 0.35);
          }
        }

        &--primary {
          background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
          color: #ffffff !important;
          border: none !important;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.22);

          &:hover {
            background: #1d4ed8 !important;
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(37, 99, 235, 0.32);
          }
        }
      }
    }
  }

  .kp-grid-wrapper {
    .kp-cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 18px;

      .kp-card-item {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 14px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
        }

        .card-head {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 10px;

          .badge-row {
            display: flex;
            align-items: center;
            gap: 8px;

            .kp-code {
              font-size: 11px;
              color: #64748b;
              background: #f1f5f9;
              padding: 2px 6px;
              border-radius: 4px;
            }
          }

          .star-rating {
            color: #f59e0b;
            font-size: 14px;
          }
        }

        .kp-name {
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
          margin: 0 0 8px;
          line-height: 1.4;
        }

        .kp-desc {
          font-size: 13px;
          color: #64748b;
          line-height: 1.6;
          margin: 0 0 14px;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .prereq-row {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 12px;
          font-size: 12px;

          .prereq-label {
            color: #94a3b8;
          }

          .prereq-tags {
            display: flex;
            gap: 4px;

            .pre-tag {
              background: #eff6ff;
              color: #2563eb;
              padding: 1px 6px;
              border-radius: 4px;
            }
          }
        }

        .card-chapter-row {
          display: flex;
          align-items: center;
          gap: 6px;
          padding-top: 12px;
          margin-top: auto;
          border-top: 1px solid #f1f5f9;
          min-width: 0;

          .chapter-icon {
            font-size: 14px;
            color: #64748b;
            flex-shrink: 0;
          }

          .chapter-label {
            font-size: 12px;
            color: #94a3b8;
            flex-shrink: 0;
          }

          .chapter-title {
            font-size: 12px;
            color: #475569;
            font-weight: 500;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            min-width: 0;
          }
        }

        .card-actions-bar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 8px;
          margin-top: 10px;
          padding-top: 10px;
          border-top: 1px dashed #f1f5f9;

          .actions-left {
            display: flex;
            align-items: center;
            gap: 8px;
          }

          .actions-right {
            display: flex;
            align-items: center;
            flex-shrink: 0;
          }

          .card-action-btn {
            height: 28px;
            padding: 0 10px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 500;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            border: 1px solid transparent;
            transition: all 0.2s ease;

            &--graph {
              background: #eff6ff;
              border-color: #bfdbfe;
              color: #1d4ed8;

              &:hover {
                background: #dbeafe;
                border-color: #93c5fd;
                color: #1e40af;
              }
            }

            &--ai {
              background: #faf5ff;
              border-color: #e9d5ff;
              color: #7c3aed;

              &:hover {
                background: #f3e8ff;
                border-color: #d8b4fe;
                color: #6d28d9;
              }
            }

            &--danger {
              background: #fff1f2;
              border-color: #fecdd3;
              color: #e11d48;

              &:hover {
                background: #ffe4e6;
                border-color: #fda4af;
                color: #be123c;
              }
            }
          }
        }
      }
    }

    .empty-state-panel {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 60px 24px;
      text-align: center;

      .empty-icon {
        font-size: 44px;
        margin-bottom: 12px;
      }

      h3 {
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
        margin-bottom: 6px;
      }

      p {
        font-size: 14px;
        color: #64748b;
      }
    }
  }
}

.capsule-btn {
  height: 36px !important;
  line-height: 36px !important;
  border-radius: 9999px !important;
  padding: 0 16px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  box-sizing: border-box !important;
  vertical-align: middle !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;

  .el-icon {
    font-size: 15px;
    margin: 0;
  }

  &--secondary {
    background: #f8fafc !important;
    border: 1px solid #e2e8f0 !important;
    color: #475569 !important;

    &:hover {
      background: #f1f5f9 !important;
      border-color: #cbd5e1 !important;
      color: #1677ff !important;
    }
  }

  &--ai {
    background: #eff6ff !important;
    border: 1px solid #bfdbfe !important;
    color: #2563eb !important;

    &:hover {
      background: #dbeafe !important;
      border-color: #93c5fd !important;
      color: #1d4ed8 !important;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15) !important;
    }
  }

  &--primary {
    background: #1677ff !important;
    border: 1px solid #1677ff !important;
    color: #ffffff !important;

    &:hover {
      background: #4096ff !important;
      border-color: #4096ff !important;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3) !important;
    }
  }
}

// 知识拓扑抽屉样式
.graph-drawer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .graph-info-header {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    .badge-pill {
      padding: 4px 12px;
      border-radius: 9999px;
      background: #F1F5F9;
      color: #475569;
      font-size: 12px;
      font-weight: 500;
    }
  }

  .topology-tree-container {
    background: #0F172A;
    border-radius: 18px;
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    min-height: 360px;
    box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.4);

    .topo-root-node {
      .topo-node-card.is-root {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 8px 20px;
        border-radius: 9999px;
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        font-size: 13.5px;
        font-weight: 600;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.4);
      }
    }

    .topo-branches {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 16px;
      width: 100%;

      .topo-branch-column {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 14px;
        padding: 12px 14px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        min-width: 180px;
        max-width: 260px;
        flex: 1;

        .topo-chapter-node {
          .chapter-tag {
            font-size: 12px;
            font-weight: 600;
            color: #93C5FD;
            display: block;
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            padding-bottom: 6px;
          }
        }

        .topo-kp-leaves {
          display: flex;
          flex-direction: column;
          gap: 6px;

          .topo-kp-leaf {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 10px;
            border-radius: 8px;
            background: rgba(255, 255, 255, 0.04);
            cursor: pointer;
            transition: all 0.2s ease;

            .leaf-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #60A5FA;
              flex-shrink: 0;
            }

            .leaf-title {
              font-size: 12px;
              color: #E2E8F0;
              line-height: 1.3;
            }

            &:hover {
              background: rgba(22, 119, 255, 0.25);
            }

            &.active {
              background: #1677FF;
              box-shadow: 0 2px 10px rgba(22, 119, 255, 0.5);

              .leaf-dot {
                background: #FFFFFF;
              }

              .leaf-title {
                color: #FFFFFF;
                font-weight: 600;
              }
            }
          }
        }
      }
    }
  }

  .active-node-panel {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .active-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      h4 {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
        color: #0F172A;
      }
    }

    .active-desc {
      margin: 0;
      font-size: 13px;
      color: #64748B;
      line-height: 1.6;
    }

    .active-actions {
      display: flex;
      gap: 10px;
      margin-top: 6px;

      .capsule-btn-action {
        height: 34px;
        padding: 0 16px;
        border-radius: 9999px;
        font-size: 12.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
        border: none;

        &--ai {
          background: #1677FF;
          color: #FFFFFF;
          &:hover { background: #4096FF; }
        }

        &--quiz {
          background: #EEF2FF;
          color: #4F46E5;
          &:hover { background: #E0E7FF; }
        }
      }
    }
  }
}

.capsule-ai-trigger-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 9999px;
  background: linear-gradient(135deg, #EEF2FF 0%, #FAF5FF 100%);
  border: 1px solid #C7D2FE;
  color: #4F46E5;
  font-size: 12.5px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #E0E7FF;
    color: #4338CA;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(79, 70, 229, 0.15);
  }
}

.kp-drawer {
  .drawer-header-flex {
    display: flex;
    align-items: center;
    gap: 12px;

    .header-icon-circle {
      width: 38px;
      height: 38px;
      border-radius: 12px;
      background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
      color: #FFFFFF;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
    }
  }

  .kp-ai-helper-banner {
    display: flex;
    align-items: center;
    gap: 12px;
    background: linear-gradient(135deg, #F0FDF4 0%, #EFF6FF 100%);
    border: 1.5px dashed #86EFAC;
    border-radius: 14px;
    padding: 12px 14px;
    cursor: pointer;
    margin-bottom: 18px;
    transition: all 0.2s;

    &:hover {
      border-color: #3B82F6;
      background: #EFF6FF;
      transform: translateY(-1px);
    }

    .ai-spark-icon {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: #DCFCE7;
      color: #15803D;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;
    }

    .ai-spark-meta {
      flex: 1;

      strong {
        display: block;
        font-size: 13px;
        color: #166534;
        margin-bottom: 2px;
      }

      p {
        font-size: 11.5px;
        color: #64748B;
        margin: 0;
      }
    }

    .spark-call-btn {
      height: 28px;
      padding: 0 12px;
      border-radius: 9999px;
      background: #10B981;
      border: none;
      color: #FFFFFF;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
    }
  }

  .form-grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .drawer-footer-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;

    .capsule-btn {
      height: 38px;
      padding: 0 20px;
      border-radius: 9999px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;

      &--secondary {
        background: #ffffff !important;
        border: 1.5px solid #cbd5e1 !important;
        color: #475569 !important;

        &:hover {
          background: #f8fafc !important;
          color: #2563eb !important;
          border-color: #93c5fd !important;
        }
      }

      &--primary {
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
        border: none !important;
        color: #ffffff !important;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
        }
      }
    }
  }
}

// AI 智能提炼弹窗样式
.ai-dialog-intro {
  margin-bottom: 14px;

  .spark-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    border-radius: 9999px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    color: #2563EB;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 6px;
  }

  p {
    font-size: 13px;
    color: #475569;
    line-height: 1.5;
    margin: 0;
  }
}

.ai-loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 0;
  color: #2563EB;
  font-size: 14px;
}

.ai-points-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 380px;
  overflow-y: auto;

  .ai-point-card {
    background: #F8FAFC;
    border: 1.5px solid #E2E8F0;
    border-radius: 12px;
    padding: 12px 14px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    transition: all 0.2s;

    &:hover {
      background: #FFFFFF;
      border-color: #93C5FD;
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
    }

    .point-top-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .point-num {
        font-size: 11px;
        font-weight: 700;
        color: #2563EB;
        background: #DBEAFE;
        padding: 2px 6px;
        border-radius: 6px;
      }

      .point-title {
        font-size: 14px;
        color: #0F172A;
        flex: 1;
      }
    }

    .point-desc {
      font-size: 12.5px;
      color: #64748B;
      margin: 0;
      line-height: 1.4;
    }

    .point-meta-row {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .focus-tag {
        font-size: 11.5px;
        color: #D97706;
        background: #FEF3C7;
        padding: 1px 8px;
        border-radius: 6px;
      }

      .stars {
        color: #F59E0B;
        font-size: 12px;
        letter-spacing: 2px;
      }
    }

    .card-apply-action {
      display: flex;
      justify-content: flex-end;
      margin-top: 4px;

      .point-apply-btn {
        height: 28px;
        padding: 0 12px;
        border-radius: 9999px;
        background: #eff6ff !important;
        border: 1px solid #bfdbfe !important;
        color: #2563eb !important;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #2563eb !important;
          color: #ffffff !important;
        }
      }
    }
  }
}

.ai-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .right-group {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .capsule-btn {
    height: 38px;
    padding: 0 18px;
    border-radius: 9999px;
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;

    &--secondary {
      background: #ffffff !important;
      border: 1.5px solid #cbd5e1 !important;
      color: #475569 !important;

      &:hover {
        background: #f8fafc !important;
        color: #2563eb !important;
        border-color: #93c5fd !important;
      }
    }

    &--ai {
      background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%) !important;
      border: none !important;
      color: #ffffff !important;
      box-shadow: 0 4px 14px rgba(124, 58, 237, 0.25);

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(124, 58, 237, 0.35);
      }
    }
  }
}
</style>
