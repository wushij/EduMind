<template>
  <div class="preferences-container" v-loading="preferenceStore.loading">
    <!-- 顶部配置看板 Dock -->
    <div class="pref-hero-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <div class="header-icon-circle">
            <el-icon class="header-icon"><Setting /></el-icon>
          </div>
          <div class="title-text-group">
            <div class="title-row">
              <h1 class="main-title">个人偏好与教学工作台设置</h1>
              <span class="capsule-tag cloud-tag">
                <el-icon><Check /></el-icon>
                <span>云端全量同步</span>
              </span>
              <span class="capsule-tag live-tag">
                <el-icon><Lightning /></el-icon>
                <span>即时响应生效</span>
              </span>
            </div>
            <p class="sub-desc">
              深度自定义 AI 助教应答范式、LaTeX/代码智能渲染引擎、多模态视觉风格及全站消息通知通道。
            </p>
          </div>
        </div>
      </div>

      <div class="header-right">
        <el-button
          size="default"
          class="pill-btn-action outline-btn"
          :icon="RefreshLeft"
          @click="handleReset"
          :disabled="preferenceStore.saving"
        >
          恢复默认配置
        </el-button>
        <el-button
          type="primary"
          size="default"
          class="pill-btn-action primary-save-btn"
          :icon="Check"
          :loading="preferenceStore.saving"
          @click="handleSave"
        >
          保存偏好配置
        </el-button>
      </div>
    </div>

    <!-- 偏好设置分类卡片堆叠 -->
    <div class="pref-cards-stack">
      <!-- 分区 1: 视觉主题与显示模式 -->
      <div class="pref-card">
        <div class="card-header-bar">
          <div class="section-icon-badge visual-badge">
            <el-icon><Brush /></el-icon>
          </div>
          <div class="section-title-wrap">
            <h3 class="card-section-title">视觉主题与显示模式</h3>
            <span class="section-subtitle">个性化定制控制台视觉风格与视窗排版密度</span>
          </div>
          <el-tag size="small" effect="plain" type="primary" class="section-pill-tag">外观交互</el-tag>
        </div>

        <div class="options-grid">
          <!-- 界面外观主题 -->
          <div class="pref-row">
            <div class="row-info">
              <div class="row-title-line">
                <span class="row-title">界面视觉外观</span>
                <el-tag size="small" type="success" effect="light" class="live-status-tag">点击即时生效</el-tag>
              </div>
              <span class="row-desc">选择契合个人审美的明亮浅色、深邃极夜或跟随操作系统昼夜节律自动调度。</span>
            </div>
            <el-radio-group
              v-model="preferenceStore.preferences.theme"
              size="default"
              class="pill-radio-group"
              @change="handleThemeChange"
            >
              <el-radio-button label="LIGHT">
                <span class="opt-btn-inner">
                  <el-icon><Sunny /></el-icon>
                  <span>明亮浅色</span>
                </span>
              </el-radio-button>
              <el-radio-button label="DARK">
                <span class="opt-btn-inner">
                  <el-icon><Moon /></el-icon>
                  <span>深邃极夜</span>
                </span>
              </el-radio-button>
              <el-radio-button label="AUTO">
                <span class="opt-btn-inner">
                  <el-icon><Monitor /></el-icon>
                  <span>跟随系统</span>
                </span>
              </el-radio-button>
            </el-radio-group>
          </div>

          <!-- 紧凑教学模式 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">紧凑教学工作台布局</span>
              <span class="row-desc">适度精简列表行距与数据卡片留白，在同一屏幕内呈现更多教学指标与批阅内容。</span>
            </div>
            <el-switch v-model="preferenceStore.preferences.compactMode" class="custom-switch" />
          </div>

          <!-- 系统界面语言 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">系统操作界面语言</span>
              <span class="row-desc">选择平台全站各功能模块的指引与术语显示语言。</span>
            </div>
            <el-select
              v-model="preferenceStore.preferences.language"
              size="default"
              class="pill-select"
              style="width: 190px"
            >
              <el-option label="简体中文 (zh-CN)" value="zh-CN" />
              <el-option label="English (en-US)" value="en-US" />
            </el-select>
          </div>
        </div>
      </div>

      <!-- 分区 2: AI 智能教学大模型与响应偏好 -->
      <div class="pref-card">
        <div class="card-header-bar">
          <div class="section-icon-badge ai-badge">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="section-title-wrap">
            <h3 class="card-section-title">AI 智能教学引擎与应答偏好</h3>
            <span class="section-subtitle">自定义基座大模型调度、思维链推理展现与智能问答教师语气</span>
          </div>
          <el-tag size="small" effect="plain" type="success" class="section-pill-tag">AI 教学大脑</el-tag>
        </div>

        <div class="options-grid">
          <!-- 默认优先推理模型 (动态后端网关互通) -->
          <div class="pref-row">
            <div class="row-info">
              <div class="row-title-line">
                <span class="row-title">默认优先推理基座模型</span>
                <el-button
                  link
                  type="primary"
                  size="small"
                  :icon="Refresh"
                  :loading="modelsLoading"
                  @click="fetchDynamicModels"
                  class="refresh-models-btn"
                >
                  刷新模型池
                </el-button>
              </div>
              <span class="row-desc">
                在发起教学备课答疑、智能出题组卷或学情分析时，系统优先调度唤醒的基座大语言模型。
              </span>
            </div>
            <el-select
              v-model="preferenceStore.preferences.defaultModel"
              size="default"
              class="pill-select model-select"
              placeholder="请选择基座大模型"
              style="width: 290px"
            >
              <el-option
                v-for="model in availableModels"
                :key="model.modelKey"
                :label="model.name"
                :value="model.modelKey"
              >
                <div class="model-option-item">
                  <div class="model-meta-main">
                    <span class="model-name">{{ model.name }}</span>
                    <el-tag size="small" :type="resolveProviderTagType(model.provider)" effect="light" class="provider-tag">
                      {{ model.provider }}
                    </el-tag>
                  </div>
                  <div class="model-meta-sub">
                    <span class="context-tag" v-if="model.contextLength">
                      {{ Math.round(model.contextLength / 1024) }}K 上下文
                    </span>
                    <span class="recommend-badge" v-if="model.isDefault">官方推荐</span>
                  </div>
                </div>
              </el-option>
            </el-select>
          </div>

          <!-- RAG 知识库检索增强 -->
          <div class="pref-row">
            <div class="row-info">
              <div class="row-title-line">
                <span class="row-title">默认启用 RAG 校本知识库精准检索增强</span>
                <el-tag size="small" type="info" effect="plain" class="feature-tag">严谨教学溯源</el-tag>
              </div>
              <span class="row-desc">
                发起教学问答时，自动对齐校本课程教材、知识图谱及题库知识切片，输出具备学术引用标识的高置信度回答。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.enableRag" class="custom-switch" />
          </div>

          <!-- 思维链 Deep Thinking 展现模式 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">深度思考过程 (Deep Thinking) 默认呈现策略</span>
              <span class="row-desc">
                面对复杂理科解题推理、高难度公式推导或命题生成时，AI 推理思考链的默认折叠展现形式。
              </span>
            </div>
            <el-radio-group
              v-model="preferenceStore.preferences.thinkingDisplayMode"
              size="default"
              class="pill-radio-group"
            >
              <el-radio-button label="EXPANDED">
                <span class="opt-btn-inner">
                  <el-icon><Expand /></el-icon>
                  <span>默认展开</span>
                </span>
              </el-radio-button>
              <el-radio-button label="COLLAPSED">
                <span class="opt-btn-inner">
                  <el-icon><Fold /></el-icon>
                  <span>默认折叠</span>
                </span>
              </el-radio-button>
              <el-radio-button label="HIDDEN">
                <span class="opt-btn-inner">
                  <el-icon><Hide /></el-icon>
                  <span>仅看最终解答</span>
                </span>
              </el-radio-button>
            </el-radio-group>
          </div>

          <!-- AI 教师角色语气风格 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">AI 助教教学启发风格偏好</span>
              <span class="row-desc">
                设置助教解答学生疑难问题时的教学语气与引导深度，匹配不同阶段的教学辅导需求。
              </span>
            </div>
            <el-select
              v-model="preferenceStore.preferences.aiTone"
              size="default"
              class="pill-select"
              style="width: 290px"
            >
              <el-option label="温和启发型 (循循善诱，多抛出引导问题启发思考)" value="HEURISTIC" />
              <el-option label="严谨学术型 (推理严密，提供标准公理、定理与论证)" value="RIGOROUS" />
              <el-option label="备考提分型 (直击考点考法，归纳题型套路与易错点)" value="EXAM_ORIENTED" />
            </el-select>
          </div>
        </div>
      </div>

      <!-- 分区 3: 教学符号与智能渲染引擎 -->
      <div class="pref-card">
        <div class="card-header-bar">
          <div class="section-icon-badge math-badge">
            <el-icon><Operation /></el-icon>
          </div>
          <div class="section-title-wrap">
            <h3 class="card-section-title">教学符号与智能渲染引擎</h3>
            <span class="section-subtitle">配置数学公式 KaTeX 排版渲染、代码高亮与动态知识图谱编译</span>
          </div>
          <el-tag size="small" effect="plain" type="warning" class="section-pill-tag">渲染核心</el-tag>
        </div>

        <div class="options-grid">
          <!-- 自动解析渲染 LaTeX 数学公式 -->
          <div class="pref-row">
            <div class="row-info">
              <div class="row-title-line">
                <span class="row-title">自动解析并渲染 LaTeX 数学公式</span>
                <span class="math-demo-preview">如：$f(x)=\lim_{t \to 0}\frac{\sin t}{t}$</span>
              </div>
              <span class="row-desc">
                在试题正文、教学解析与 AI 答疑中，自动将 LaTeX 表达式编译为矢量级平滑公式。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.katexEnabled" class="custom-switch" />
          </div>

          <!-- 代码块语法高亮与行号 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">代码块语法着色高亮与快捷复制</span>
              <span class="row-desc">
                自动检测 Python、C++、Java 等编程语言，渲染语法高亮色彩并提供一键复制代码块。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.codeHighlightEnabled" class="custom-switch" />
          </div>

          <!-- Mermaid 拓扑图表解析 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">课程拓扑知识图谱与 Mermaid 图表解析</span>
              <span class="row-desc">
                自动将知识库输出的流程图、章节拓扑结构代码编译为动态交互式 SVG 图表。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.mermaidEnabled" class="custom-switch" />
          </div>
        </div>
      </div>

      <!-- 分区 4: 教学活动提醒与消息通知触达 -->
      <div class="pref-card">
        <div class="card-header-bar">
          <div class="section-icon-badge notify-badge">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="section-title-wrap">
            <h3 class="card-section-title">教学协同提醒与消息触达通道</h3>
            <span class="section-subtitle">定制师生答卷提交、批量智能预批改与知识库向量化状态推送</span>
          </div>
          <el-tag size="small" effect="plain" type="info" class="section-pill-tag">即时通信</el-tag>
        </div>

        <div class="options-grid">
          <!-- 学生作业提交提醒 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">学生作业与考核提交即时提醒</span>
              <span class="row-desc">
                当授课班级学生完成并提交在线作业或随堂考核时，在右上角站内信推送即时动态。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.notifySubmission" class="custom-switch" />
          </div>

          <!-- AI 评阅完成通知 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">全班 AI 批量智能预批改完成通知</span>
              <span class="row-desc">
                全班答卷由 AI 大模型批量打分、错题归因与评语生成完毕后，推送班级学情大盘就绪通知。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.notifyGrading" class="custom-switch" />
          </div>

          <!-- 知识库向量化就绪 -->
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">校本教材切片向量化 (Embedding) 就绪通知</span>
              <span class="row-desc">
                教研组上传大型教材课件文档后，文本向量化切片与知识索引构建完成时发送就绪通知。
              </span>
            </div>
            <el-switch v-model="preferenceStore.preferences.notifyVector" class="custom-switch" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { usePreferenceStore } from '@/stores/user/preference';
import { getChatModels } from '@/api/ai/chat';
import type { ModelProviderConfig } from '@/types/system/model';
import {
  Setting,
  Check,
  RefreshLeft,
  Refresh,
  Brush,
  Sunny,
  Moon,
  Monitor,
  Cpu,
  Operation,
  Bell,
  Lightning,
  Expand,
  Fold,
  Hide
} from '@element-plus/icons-vue';

const preferenceStore = usePreferenceStore();

const modelsLoading = ref(false);
const dynamicModels = ref<ModelProviderConfig[]>([]);

// 真实模型列表与兜底保障
const availableModels = computed(() => {
  if (dynamicModels.value.length > 0) {
    return dynamicModels.value;
  }
  // 本地高可用基准备选模型
  return [
    {
      id: 1,
      modelKey: 'deepseek-v3',
      name: 'DeepSeek-V3',
      provider: 'DeepSeek',
      contextLength: 65536,
      enabled: true,
      isDefault: true
    },
    {
      id: 2,
      modelKey: 'deepseek-r1',
      name: 'DeepSeek-R1 (深度思维链)',
      provider: 'DeepSeek',
      contextLength: 65536,
      enabled: true,
      isDefault: false
    },
    {
      id: 3,
      modelKey: 'qwen-2.5',
      name: '通义千问 Qwen-2.5-72B',
      provider: 'Qwen',
      contextLength: 131072,
      enabled: true,
      isDefault: false
    },
    {
      id: 4,
      modelKey: 'glm-4-plus',
      name: '智谱 GLM-4-Plus',
      provider: 'Zhipu',
      contextLength: 131072,
      enabled: true,
      isDefault: false
    }
  ] as ModelProviderConfig[];
});

const resolveProviderTagType = (provider: string) => {
  if (!provider) return 'info';
  const p = provider.toLowerCase();
  if (p.includes('deepseek')) return 'primary';
  if (p.includes('qwen') || p.includes('aliyun')) return 'success';
  if (p.includes('zhipu')) return 'warning';
  return 'info';
};

const fetchDynamicModels = async () => {
  try {
    modelsLoading.value = true;
    const list = await getChatModels();
    if (list && list.length > 0) {
      dynamicModels.value = list.filter((m) => m.enabled !== false);
      ElMessage.success(`已从智能网关同步 ${dynamicModels.value.length} 个可用模型`);
    }
  } catch (e: any) {
    console.warn('获取动态模型列表失败:', e);
  } finally {
    modelsLoading.value = false;
  }
};

const handleThemeChange = (val: any) => {
  preferenceStore.applyTheme(val);
};

const handleReset = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复系统默认的个人偏好设置吗？所有显示模式、默认模型与渲染开关将还原为出厂状态。',
      '恢复默认偏好确认',
      {
        confirmButtonText: '恢复默认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await preferenceStore.resetToDefaults();
  } catch {
    // 用户取消
  }
};

const handleSave = async () => {
  await preferenceStore.savePreferences();
};

onMounted(async () => {
  await preferenceStore.loadPreferences();
  await fetchDynamicModels();
});
</script>

<style scoped lang="scss">
.preferences-container {
  padding: 24px 28px 48px;
  background: var(--el-bg-color-page, #f8fafc);
  min-height: calc(100vh - 64px);
  transition: background-color 0.3s ease;

  .pref-hero-dock {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 20px;
    margin-bottom: 24px;
    background: var(--el-bg-color, #ffffff);
    border: 1px solid var(--el-border-color-lighter, #e2e8f0);
    border-radius: 20px;
    padding: 22px 28px;
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
    transition: all 0.3s ease;

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 16px;

        .header-icon-circle {
          width: 48px;
          height: 48px;
          border-radius: 14px;
          background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
          flex-shrink: 0;

          .header-icon {
            font-size: 24px;
            color: #ffffff;
          }
        }

        .title-text-group {
          display: flex;
          flex-direction: column;
          gap: 6px;

          .title-row {
            display: flex;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;

            .main-title {
              font-size: 20px;
              font-weight: 800;
              color: var(--el-text-color-primary, #0f172a);
              margin: 0;
              line-height: 1.2;
            }

            .capsule-tag {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              font-size: 11.5px;
              border-radius: 9999px;
              padding: 3px 10px;
              font-weight: 600;

              &.cloud-tag {
                background: #eff6ff;
                color: #2563eb;
                border: 1px solid #bfdbfe;
              }

              &.live-tag {
                background: #ecfdf5;
                color: #059669;
                border: 1px solid #a7f3d0;
              }
            }
          }

          .sub-desc {
            margin: 0;
            font-size: 13px;
            color: var(--el-text-color-secondary, #64748b);
            line-height: 1.5;
          }
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .pill-btn-action {
        border-radius: 9999px;
        height: 38px;
        padding: 0 22px;
        font-weight: 600;
        font-size: 13px;
        transition: all 0.25s ease;

        &.primary-save-btn {
          background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
          border: none;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
          }
        }

        &.outline-btn {
          border-color: var(--el-border-color, #cbd5e1);
          color: var(--el-text-color-regular, #475569);
          background: var(--el-bg-color, #ffffff);

          &:hover {
            border-color: #94a3b8;
            color: var(--el-text-color-primary, #0f172a);
            background: var(--el-fill-color-light, #f8fafc);
          }
        }
      }
    }
  }

  .pref-cards-stack {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .pref-card {
      background: var(--el-bg-color, #ffffff);
      border-radius: 20px;
      border: 1px solid var(--el-border-color-lighter, #e2e8f0);
      padding: 22px 26px;
      box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
      transition: all 0.3s ease;

      &:hover {
        border-color: rgba(37, 99, 235, 0.25);
        box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
      }

      .card-header-bar {
        display: flex;
        align-items: center;
        gap: 14px;
        margin-bottom: 20px;
        padding-bottom: 14px;
        border-bottom: 1px solid var(--el-border-color-extra-light, #f1f5f9);

        .section-icon-badge {
          width: 38px;
          height: 38px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
          flex-shrink: 0;

          &.visual-badge {
            background: #ede9fe;
            color: #7c3aed;
          }

          &.ai-badge {
            background: #dbeafe;
            color: #2563eb;
          }

          &.math-badge {
            background: #ccfbf1;
            color: #0d9488;
          }

          &.notify-badge {
            background: #fef3c7;
            color: #d97706;
          }
        }

        .section-title-wrap {
          display: flex;
          flex-direction: column;
          gap: 2px;
          flex: 1;

          .card-section-title {
            font-size: 16px;
            font-weight: 700;
            color: var(--el-text-color-primary, #0f172a);
            margin: 0;
          }

          .section-subtitle {
            font-size: 12px;
            color: var(--el-text-color-secondary, #64748b);
          }
        }

        .section-pill-tag {
          border-radius: 9999px;
          font-weight: 600;
          font-size: 11.5px;
          padding: 2px 10px;
        }
      }

      .options-grid {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .pref-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 14px 18px;
          background: var(--el-fill-color-light, #f8fafc);
          border-radius: 14px;
          border: 1px solid var(--el-border-color-extra-light, #edf2f7);
          gap: 20px;
          transition: all 0.2s ease;

          &:hover {
            background: var(--el-fill-color, #f1f5f9);
            border-color: var(--el-border-color-lighter, #e2e8f0);
          }

          .row-info {
            display: flex;
            flex-direction: column;
            gap: 4px;
            flex: 1;
            min-width: 0;

            .row-title-line {
              display: flex;
              align-items: center;
              gap: 8px;
              flex-wrap: wrap;

              .row-title {
                font-size: 14px;
                font-weight: 700;
                color: var(--el-text-color-primary, #1e293b);
              }

              .live-status-tag {
                border-radius: 9999px;
                font-size: 10.5px;
                height: 18px;
                padding: 0 6px;
              }

              .feature-tag {
                border-radius: 9999px;
                font-size: 11px;
                height: 20px;
                padding: 0 8px;
              }

              .math-demo-preview {
                font-size: 12px;
                font-family: 'Times New Roman', serif;
                font-style: italic;
                color: #2563eb;
                background: #eff6ff;
                padding: 1px 8px;
                border-radius: 6px;
              }

              .refresh-models-btn {
                font-size: 12px;
                padding: 0;
                margin-left: 4px;
              }
            }

            .row-desc {
              font-size: 12px;
              color: var(--el-text-color-secondary, #64748b);
              line-height: 1.5;
            }
          }
        }
      }
    }
  }
}

/* 药丸单选与输入控件风格定制 */
:deep(.pill-radio-group) {
  .el-radio-button__inner {
    border-radius: 9999px !important;
    padding: 7px 16px;
    font-size: 12.5px;
    font-weight: 600;
    margin: 0 3px;
    border: 1px solid var(--el-border-color-lighter, #e2e8f0);
    background: var(--el-bg-color, #ffffff);
    color: var(--el-text-color-regular, #475569);
    transition: all 0.2s ease;
  }

  .el-radio-button__original-radio:checked + .el-radio-button__inner {
    background: #2563eb !important;
    color: #ffffff !important;
    border-color: #2563eb !important;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3) !important;
  }

  .opt-btn-inner {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}

:deep(.pill-select) {
  .el-input__wrapper {
    border-radius: 9999px !important;
    padding: 4px 14px;
    box-shadow: 0 0 0 1px var(--el-border-color-lighter, #e2e8f0) inset;
    background: var(--el-bg-color, #ffffff);
  }
}

.model-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;

  .model-meta-main {
    display: flex;
    align-items: center;
    gap: 6px;

    .model-name {
      font-weight: 600;
      color: var(--el-text-color-primary, #1e293b);
    }

    .provider-tag {
      border-radius: 6px;
      font-size: 10.5px;
      padding: 0 5px;
      height: 18px;
    }
  }

  .model-meta-sub {
    display: flex;
    align-items: center;
    gap: 4px;

    .context-tag {
      font-size: 11px;
      color: #94a3b8;
      font-family: monospace;
    }

    .recommend-badge {
      font-size: 10.5px;
      background: #fef2f2;
      color: #ef4444;
      padding: 1px 5px;
      border-radius: 4px;
      font-weight: 600;
    }
  }
}

:deep(.custom-switch) {
  --el-switch-on-color: #2563eb;
}
</style>
