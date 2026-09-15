<template>
  <div class="agent-memory-page" v-loading="loading">
    <PageHeroBanner
      title="Agent 长期记忆与隐私治理 · 个性化认知沉淀"
      subtitle="赋予教学智能体跨会话的持续进化记忆，用户享有 100% 透明可控的记忆查看、单条遗忘与知情同意权"
      background-variant="ai"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">{{ memoryItems.length }}</span>
            <span class="stat-label">已沉淀认知记忆</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ preferenceCount }}</span>
            <span class="stat-label">学习风格与偏好</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ episodicCount }}</span>
            <span class="stat-label">历史攻坚情境</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">已受保护</span>
            <span class="stat-label">零知识隐私加密</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 隐私授权与合规保护看板 -->
      <div class="privacy-consent-card">
        <div class="consent-left">
          <div class="shield-icon-box">
            <el-icon><Lock /></el-icon>
          </div>
          <div class="consent-text">
            <div class="consent-title-row">
              <h3>Agent 记忆与用户隐私保护知情同意</h3>
              <el-tag :type="consentGranted ? 'success' : 'danger'" effect="dark" round>
                {{ consentGranted ? '已开启记忆沉淀' : '已暂停记忆沉淀' }}
              </el-tag>
            </div>
            <p>EduMind 遵循个人信息保护法与可解释 AI 标准，所有记忆仅限用于个性化教学辅导，绝不用于模型二次训练或跨机构共享。</p>
          </div>
        </div>

        <div class="consent-right">
          <div class="retention-selector">
            <span class="label">记忆留存周期：</span>
            <el-select v-model="retentionDays" size="small" style="width: 120px;" @change="handleConsentChange">
              <el-option label="30 天" :value="30" />
              <el-option label="90 天" :value="90" />
              <el-option label="180 天" :value="180" />
              <el-option label="长期有效" :value="365" />
            </el-select>
          </div>
          <el-switch
            v-model="consentGranted"
            active-text="授权"
            inactive-text="关闭"
            @change="handleConsentChange"
          />
          <el-button type="danger" plain size="small" @click="handleForgetAll">
            一键被遗忘 (清空)
          </el-button>
        </div>
      </div>

      <!-- 检索与测试工作台 -->
      <div class="memory-search-card">
        <div class="search-left">
          <el-input
            v-model="searchKeyword"
            placeholder="检索记忆关键词或学习特征..."
            prefix-icon="Search"
            clearable
            style="width: 280px;"
          />
          <el-select v-model="typeFilter" placeholder="记忆类型" clearable style="width: 140px;">
            <el-option label="全部类型" :value="undefined" />
            <el-option label="学习偏好 (PREFERENCE)" value="PREFERENCE" />
            <el-option label="认知画像 (PROFILE)" value="PROFILE" />
            <el-option label="情境片段 (EPISODIC)" value="EPISODIC" />
            <el-option label="交互反馈 (FEEDBACK)" value="FEEDBACK" />
          </el-select>
        </div>

        <div class="search-right">
          <el-button type="primary" plain @click="openRecallTester">
            <el-icon><MagicStick /></el-icon>
            <span>Agent 语义召回测试</span>
          </el-button>
          <el-button type="primary" class="gradient-btn" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>注入教学先验记忆</span>
          </el-button>
        </div>
      </div>

      <!-- 记忆条目卡片流 -->
      <div class="memory-grid" v-if="filteredMemories.length > 0">
        <div v-for="item in filteredMemories" :key="item.id" class="memory-card">
          <div class="card-header">
            <div class="header-type">
              <el-tag :type="getTypeTagType(item.memoryType)" effect="light">
                {{ getTypeLabel(item.memoryType) }}
              </el-tag>
              <el-tag v-if="item.encrypted" type="warning" size="small" effect="plain" round>
                国密SM4加密
              </el-tag>
              <span class="memory-key font-mono">{{ item.memoryKey || '#' + item.id }}</span>
            </div>
            <div class="header-score">
              <span class="score-label">置信度:</span>
              <span class="score-val">{{ ((item.confidenceScore || 0.95) * 100).toFixed(0) }}%</span>
            </div>
          </div>

          <div class="card-body">
            <p class="memory-content">{{ item.summary || item.memoryValue }}</p>
            <div class="memory-meta">
              <span>敏感级别：{{ item.sensitivityLevel || 'NORMAL' }}</span>
              <span>记录时间：{{ item.createTime || '刚刚' }}</span>
            </div>
          </div>

          <div class="card-footer">
            <div class="feedback-actions">
              <el-button link size="small" type="primary" @click="giveFeedback(item, 5)">
                <el-icon><Check /></el-icon> 准确
              </el-button>
              <el-button link size="small" type="info" @click="giveFeedback(item, 1)">
                <el-icon><Close /></el-icon> 不准
              </el-button>
            </div>
            <el-button link size="small" type="danger" @click="forgetSingle(item)">
              <el-icon><Delete /></el-icon> 遗忘
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无符合条件的记忆条目" />
    </div>

    <!-- 注入先验记忆对话框 -->
    <el-dialog v-model="createDialogVisible" title="注入教学先验特征记忆" width="520px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="记忆类别" required>
          <el-select v-model="createForm.memoryType" style="width: 100%;">
            <el-option label="学习风格与偏好 (PREFERENCE)" value="PREFERENCE" />
            <el-option label="能力画像与薄弱点 (PROFILE)" value="PROFILE" />
            <el-option label="教学交互情境 (EPISODIC)" value="EPISODIC" />
            <el-option label="交互纠错反馈 (FEEDBACK)" value="FEEDBACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="敏感级别 (安全合规)">
          <el-select v-model="createForm.sensitivityLevel" style="width: 100%;">
            <el-option label="普通 (NORMAL)" value="NORMAL" />
            <el-option label="学术特征 (ACADEMIC)" value="ACADEMIC" />
            <el-option label="高敏感 (HIGH_RISK - 启用国密 SM4 加密)" value="HIGH_RISK" />
          </el-select>
        </el-form-item>
        <el-form-item label="记忆摘要/事实描述" required>
          <el-input
            v-model="createForm.memoryValue"
            type="textarea"
            :rows="3"
            placeholder="如：该学生对复合函数求导的链式法则极度敏感，但极易遗忘定义域约束，建议提问时多加前置检查。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateMemory">沉淀入库</el-button>
      </template>
    </el-dialog>

    <!-- 语义召回测试抽屉 -->
    <el-drawer v-model="recallDrawerVisible" title="Agent 语义记忆召回沙盒测试" size="520px">
      <div class="recall-sandbox">
        <p class="sandbox-desc">输入当前对话场景 Prompt，模拟 Agent 在生成回答前实时检索长期记忆库的 Top-K 相关上下文片段：</p>
        <el-input
          v-model="queryPrompt"
          type="textarea"
          :rows="3"
          placeholder="例如：我准备做 3 道导数综合大题，帮我按照我平时的做题节奏安排题目。"
        />
        <el-button type="primary" class="gradient-btn mt-3" style="width: 100%; margin-top: 14px;" @click="doRetrieve">
          <el-icon><Search /></el-icon>
          <span>执行向量语义召回匹配</span>
        </el-button>

        <div class="recalled-results" v-if="recalledItems.length > 0">
          <el-divider content-position="left">召回命中的记忆片段 (Top-{{ recalledItems.length }})</el-divider>
          <div v-for="(rec, idx) in recalledItems" :key="rec.id" class="recalled-item">
            <div class="recalled-top">
              <span class="rank">#{{ idx + 1 }} 相似度得分：{{ ((rec.confidenceScore || 0.95) * 100).toFixed(0) }}%</span>
              <el-tag size="small" :type="getTypeTagType(rec.memoryType)">{{ getTypeLabel(rec.memoryType) }}</el-tag>
            </div>
            <p class="rec-val">{{ rec.summary || rec.memoryValue }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Lock, Search, MagicStick, Plus, Check, Close, Delete } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useAgentMemory } from '@/composables/ai/useAgentMemory';

const {
  loading,
  consentGranted,
  retentionDays,
  searchKeyword,
  typeFilter,
  memoryItems,
  preferenceCount,
  episodicCount,
  filteredMemories,
  createDialogVisible,
  createForm,
  recallDrawerVisible,
  queryPrompt,
  recalledItems,
  handleConsentChange,
  handleForgetAll,
  openCreateDialog,
  submitCreateMemory,
  forgetSingle,
  giveFeedback,
  openRecallTester,
  doRetrieve,
  getTypeLabel,
  getTypeTagType
} = useAgentMemory();
</script>

<style scoped lang="scss">
.agent-memory-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 16px;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(8px);
      padding: 10px 18px;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
      display: flex;
      flex-direction: column;
      align-items: center;

      .stat-num {
        font-size: 20px;
        font-weight: 700;
        line-height: 1.2;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #0284C7; }
      }

      .stat-label {
        font-size: 11px;
        color: #64748B;
        margin-top: 4px;
      }
    }
  }

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .privacy-consent-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 18px 22px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    margin-bottom: 22px;

    .consent-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .shield-icon-box {
        width: 46px;
        height: 46px;
        border-radius: 12px;
        background: #ECFDF5;
        color: #10B981;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
      }

      .consent-text {
        .consent-title-row {
          display: flex;
          align-items: center;
          gap: 10px;

          h3 {
            font-size: 16px;
            font-weight: 600;
            color: #0F172A;
            margin: 0;
          }
        }

        p {
          font-size: 12px;
          color: #64748B;
          margin: 4px 0 0;
        }
      }
    }

    .consent-right {
      display: flex;
      align-items: center;
      gap: 16px;

      .retention-selector {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: #475569;
      }
    }
  }

  .memory-search-card {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    margin-bottom: 22px;

    .search-left {
      display: flex;
      gap: 14px;
      align-items: center;
    }

    .search-right {
      display: flex;
      gap: 12px;

      .gradient-btn {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        border: none;
        border-radius: 10px;
      }
    }
  }

  .memory-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
    gap: 20px;

    .memory-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 20px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      display: flex;
      flex-direction: column;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(37, 99, 235, 0.07);
        border-color: #BFDBFE;
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .header-type {
          display: flex;
          align-items: center;
          gap: 8px;

          .memory-key {
            font-size: 13px;
            font-weight: 600;
            color: #1E293B;
          }
        }

        .header-score {
          font-size: 12px;
          color: #64748B;
          .score-val {
            font-weight: 700;
            color: #2563EB;
            margin-left: 4px;
          }
        }
      }

      .card-body {
        flex: 1;

        .memory-content {
          font-size: 14px;
          line-height: 1.6;
          color: #334155;
          margin: 0 0 12px;
        }

        .memory-meta {
          display: flex;
          justify-content: space-between;
          font-size: 11px;
          color: #94A3B8;
        }
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #F1F5F9;
        padding-top: 12px;
        margin-top: 8px;
      }
    }
  }

  .recall-sandbox {
    .sandbox-desc {
      font-size: 13px;
      color: #475569;
      line-height: 1.5;
      margin-bottom: 14px;
    }

    .recalled-results {
      margin-top: 20px;

      .recalled-item {
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 12px;
        padding: 12px 14px;
        margin-bottom: 12px;

        .recalled-top {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 6px;

          .rank {
            font-size: 12px;
            font-weight: 600;
            color: #2563EB;
          }
        }

        .rec-val {
          font-size: 13px;
          color: #334155;
          margin: 0;
          line-height: 1.5;
        }
      }
    }
  }
}
</style>
