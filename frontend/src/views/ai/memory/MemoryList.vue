<template>
  <div class="agent-memory-page" v-loading="loading && !memoryItems.length">
    <!-- 顶部大视觉 Hero Banner 与长圆微光统计看板 -->
    <PageHeroBanner
      title="Agent 长期记忆与隐私治理 · 个性化认知沉淀"
      subtitle="赋予教学智能体跨会话的持续进化记忆，用户享有 100% 透明可控的记忆查看、单条微调遗忘与知情同意权"
      background-variant="ai"
    >
      <template #extra>
        <MemoryHeroStats
          :total-count="memoryItems.length"
          :preference-count="preferenceCount"
          :profile-count="profileCount"
          :episodic-count="episodicCount"
          :encrypted-count="encryptedCount"
          :consent-granted="consentGranted"
        />
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 1. 认知命名空间长圆分段控制器 (全局与课程多空间互联互通) -->
      <div class="scope-navigation-section">
        <MemoryScopeTabs
          :spaces="spaces"
          :selected-course-id="selectedCourseId"
          @switch-course="handleSwitchCourse"
        />
      </div>

      <!-- 2. 隐私授权与合规保护盾牌拟态看板 -->
      <MemoryGovernanceCard
        v-model:consent-granted="consentGranted"
        v-model:retention-days="retentionDays"
        @change="handleConsentChange"
        @forget-all="handleForgetAll"
      />

      <!-- 3. 长圆操作栏与维度胶囊过滤器 -->
      <div class="memory-action-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="searchKeyword"
            placeholder="检索记忆关键词、学术考点或偏好特征..."
            :prefix-icon="Search"
            clearable
            class="capsule-search-input"
          />

          <div class="category-pill-filters">
            <button
              type="button"
              class="filter-pill-btn"
              :class="{ active: typeFilter === undefined }"
              @click="typeFilter = undefined"
            >
              全部 ({{ memoryItems.length }})
            </button>
            <button
              type="button"
              class="filter-pill-btn success"
              :class="{ active: typeFilter === 'PREFERENCE' }"
              @click="typeFilter = 'PREFERENCE'"
            >
              学习偏好 ({{ preferenceCount }})
            </button>
            <button
              type="button"
              class="filter-pill-btn violet"
              :class="{ active: typeFilter === 'PROFILE' }"
              @click="typeFilter = 'PROFILE'"
            >
              学术画像 ({{ profileCount }})
            </button>
            <button
              type="button"
              class="filter-pill-btn warning"
              :class="{ active: typeFilter === 'EPISODIC' }"
              @click="typeFilter = 'EPISODIC'"
            >
              攻坚情境 ({{ episodicCount }})
            </button>
            <button
              type="button"
              class="filter-pill-btn cyan"
              :class="{ active: typeFilter === 'FEEDBACK' }"
              @click="typeFilter = 'FEEDBACK'"
            >
              调优反馈 ({{ feedbackCount }})
            </button>
          </div>
        </div>

        <div class="toolbar-right">
          <el-button
            v-if="hasDuplicates"
            type="warning"
            plain
            class="pill-action-btn deduplicate-btn"
            :loading="cleaningDuplicates"
            @click="handleCleanupDuplicates"
          >
            <el-icon><Brush /></el-icon>
            <span>一键语义去重</span>
          </el-button>

          <el-button
            type="primary"
            class="pill-action-btn gradient-create-btn"
            :loading="extracting"
            @click="handleExtractMemories"
          >
            <el-icon><MagicStick /></el-icon>
            <span>AI 学情智能萃取</span>
          </el-button>

          <el-button
            type="primary"
            plain
            class="pill-action-btn"
            @click="openRecallTester"
          >
            <el-icon><Search /></el-icon>
            <span>语义召回沙盒</span>
          </el-button>

          <el-button
            type="primary"
            class="pill-action-btn gradient-create-btn"
            @click="openCreateDialog"
          >
            <el-icon><Plus /></el-icon>
            <span>注入先验记忆</span>
          </el-button>
        </div>
      </div>

      <!-- 4. 记忆卡片流矩阵 -->
      <div v-if="filteredMemories.length > 0" class="memory-cards-grid">
        <MemoryItemCard
          v-for="item in filteredMemories"
          :key="item.id"
          :item="item"
          @feedback="giveFeedback"
          @edit="openEditDialog"
          @forget="forgetSingle"
          @decrypt="handleDecryptItem"
        />
      </div>

      <!-- 5. 优雅拟态空状态 (带真实行动指引) -->
      <div v-else class="memory-empty-capsule">
        <div class="empty-icon-circle">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <h4 class="empty-title">
          {{ !consentGranted ? '当前空间已开启零知识隐私保护' : '当前空间暂无匹配的长效记忆条目' }}
        </h4>
        <p class="empty-sub">
          {{ !consentGranted
            ? '请点击上方看板的“授权”开关赋予 Agent 跨会话认知演化能力；授权后系统将严格在本地为您服务，保障数据安全。'
            : '长期记忆将随日常师生问答、阶段小测与错题诊断自动沉淀。您也可以立即启动 AI 智能推演萃取，或手动注入先验规则：'
          }}
        </p>

        <div class="empty-actions-row">
          <el-button
            v-if="!consentGranted"
            type="success"
            class="empty-pill-btn"
            @click="consentGranted = true; handleConsentChange()"
          >
            <el-icon><Check /></el-icon>
            <span>立即开启记忆沉淀授权</span>
          </el-button>

          <template v-else>
            <el-button
              type="primary"
              class="empty-pill-btn gradient-btn"
              :loading="extracting"
              @click="handleExtractMemories"
            >
              <el-icon><MagicStick /></el-icon>
              <span>从近期学情萃取认知特征 (AI 研判)</span>
            </el-button>
            <el-button
              class="empty-pill-btn"
              @click="openCreateDialog"
            >
              <el-icon><Plus /></el-icon>
              <span>手动添加先验记忆</span>
            </el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 弹窗与测试沙盒抽屉 -->
    <MemorySandboxDrawer
      v-model:visible="recallDrawerVisible"
      v-model:query-prompt="queryPrompt"
      :recalled-items="recalledItems"
      @retrieve="doRetrieve"
    />

    <MemoryExtractDialog
      v-model:visible="extractDialogVisible"
      :candidates="extractedCandidates"
      :loading="extracting"
      :confirming="confirmingCandidates"
      @cancel="handleCancelExtract"
      @confirm="handleConfirmExtract"
    />

    <MemoryCreateDialog
      v-model:visible="createDialogVisible"
      :form="createForm"
      @submit="submitCreateMemory"
    />

    <MemoryEditDialog
      v-model:visible="editDialogVisible"
      :form="editForm"
      @submit="submitEditMemory"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Search,
  MagicStick,
  Plus,
  Brush,
  FolderOpened,
  Check
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import MemoryHeroStats from '@/components/ai/memory/MemoryHeroStats.vue';
import MemoryScopeTabs from '@/components/ai/memory/MemoryScopeTabs.vue';
import MemoryGovernanceCard from '@/components/ai/memory/MemoryGovernanceCard.vue';
import MemoryItemCard from '@/components/ai/memory/MemoryItemCard.vue';
import MemorySandboxDrawer from '@/components/ai/memory/MemorySandboxDrawer.vue';
import MemoryExtractDialog from '@/components/ai/memory/MemoryExtractDialog.vue';
import MemoryCreateDialog from '@/components/ai/memory/MemoryCreateDialog.vue';
import MemoryEditDialog from '@/components/ai/memory/MemoryEditDialog.vue';
import { useAgentMemory } from '@/composables/ai/useAgentMemory';

const {
  loading,
  extracting,
  cleaningDuplicates,
  hasDuplicates,
  selectedCourseId,
  spaces,
  consentGranted,
  retentionDays,
  searchKeyword,
  typeFilter,
  memoryItems,
  preferenceCount,
  profileCount,
  episodicCount,
  feedbackCount,
  encryptedCount,
  filteredMemories,
  createDialogVisible,
  createForm,
  editDialogVisible,
  editForm,
  recallDrawerVisible,
  queryPrompt,
  recalledItems,
  extractDialogVisible,
  extractedCandidates,
  handleSwitchCourse,
  handleConsentChange,
  handleForgetAll,
  handleExtractMemories,
  handleConfirmExtract,
  confirmingCandidates,
  handleCancelExtract,
  handleCleanupDuplicates,
  handleDecryptItem,
  openCreateDialog,
  submitCreateMemory,
  openEditDialog,
  submitEditMemory,
  forgetSingle,
  giveFeedback,
  openRecallTester,
  doRetrieve
} = useAgentMemory();
</script>

<style scoped lang="scss">
.agent-memory-page {
  padding-bottom: 50px;

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .scope-navigation-section {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #FFFFFF;
    border-radius: 20px;
    padding: 12px 20px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
  }

  .memory-action-toolbar {
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #E2E8F0;
    padding: 14px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
    flex-wrap: wrap;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;

      .capsule-search-input {
        width: 290px;
        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
        }
      }

      .category-pill-filters {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 9999px;
        padding: 3px;

        .filter-pill-btn {
          border: none;
          background: transparent;
          font-size: 12px;
          font-weight: 500;
          color: #64748B;
          padding: 5px 12px;
          border-radius: 9999px;
          cursor: pointer;
          transition: all 0.2s ease;
          white-space: nowrap;

          &:hover {
            color: #1E293B;
            background: rgba(255, 255, 255, 0.7);
          }

          &.active {
            background: #FFFFFF;
            color: #2563EB;
            font-weight: 600;
            box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
          }

          &.success.active {
            color: #059669;
          }
          &.violet.active {
            color: #7C3AED;
          }
          &.warning.active {
            color: #D97706;
          }
          &.cyan.active {
            color: #0891B2;
          }
        }
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .pill-action-btn {
        border-radius: 9999px;
        padding: 8px 16px;
        font-size: 13px;
        font-weight: 500;
        transition: all 0.2s ease;

        &.gradient-create-btn {
          background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
          border: none;
          color: #FFFFFF;
          font-weight: 600;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
          }
        }
      }
    }
  }

  .memory-cards-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
    gap: 20px;

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }
  }

  .memory-empty-capsule {
    background: #FFFFFF;
    border-radius: 24px;
    border: 1px dashed #CBD5E1;
    padding: 48px 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.02);

    .empty-icon-circle {
      width: 68px;
      height: 68px;
      border-radius: 50%;
      background: #EFF6FF;
      color: #2563EB;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32px;
      margin-bottom: 16px;
    }

    .empty-title {
      font-size: 17px;
      font-weight: 700;
      color: #0F172A;
      margin: 0 0 8px;
    }

    .empty-sub {
      font-size: 13px;
      color: #64748B;
      max-width: 580px;
      line-height: 1.6;
      margin: 0 0 24px;
    }

    .empty-actions-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      justify-content: center;

      .empty-pill-btn {
        border-radius: 9999px;
        padding: 9px 20px;
        font-size: 13px;
        font-weight: 600;

        &.gradient-btn {
          background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
          border: none;
          color: #FFFFFF;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
        }
      }
    }
  }
}
</style>
