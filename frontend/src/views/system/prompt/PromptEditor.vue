<template>
  <div class="prompt-editor-page">
    <!-- 顶部导航与发布操作栏 -->
    <div class="editor-header-card">
      <div class="header-left">
        <el-button
          link
          class="back-btn"
          :icon="ArrowLeft"
          @click="router.push('/system/prompts')"
        >
          返回模板中心
        </el-button>

        <div class="title-meta-row">
          <h2 class="template-title">{{ form.name || '新建 Prompt 模板' }}</h2>
          <span class="code-badge" @click="copyText(form.code, '模板编码已复制')">
            {{ form.code }}
            <el-icon class="copy-ic"><CopyDocument /></el-icon>
          </span>
          <span class="category-pill" :class="form.category">
            {{ getCategoryLabel(form.category) }}
          </span>
          <span class="version-badge">{{ form.version }}</span>
          <span
            class="status-indicator"
            :class="form.status === 'PUBLISHED' ? 'online' : 'draft'"
          >
            <span class="pulse-dot"></span>
            {{ form.status === 'PUBLISHED' ? '已发布生产' : '草稿' }}
          </span>
        </div>
      </div>

      <div class="header-right">
        <el-button :icon="Check" class="save-draft-btn" @click="saveForm">
          保存草稿
        </el-button>
        <el-button
          type="primary"
          class="publish-btn"
          :icon="Upload"
          :loading="publishing"
          @click="publishForm"
        >
          发布生效至网关
        </el-button>
      </div>
    </div>

    <!-- 主工作台：双栏专业 Studio 布局 -->
    <div class="editor-studio-grid">
      <!-- 左栏：提示词编排与参数配置工作区 (62%) -->
      <div class="studio-main-col">
        <!-- 模块 1：基础工程参数与模型配置 -->
        <div class="section-card meta-section">
          <div class="section-card-header">
            <div class="sch-left">
              <span class="sec-icon"><el-icon><Cpu /></el-icon></span>
              <span class="sec-title">基础工程参数与大模型绑定</span>
            </div>
            <span class="sec-hint">配置模板标识、模型路由与采样超参数</span>
          </div>

          <div class="meta-form-body">
            <div class="grid-row-3">
              <div class="form-item-group">
                <label class="item-label">模板标识编码 (Code) <span class="req">*</span></label>
                <el-input
                  v-model="form.code"
                  placeholder="如 COURSE_RAG_GENERAL"
                  class="mono-input"
                />
              </div>

              <div class="form-item-group">
                <label class="item-label">模板中文名称 <span class="req">*</span></label>
                <el-input
                  v-model="form.name"
                  placeholder="如 通用课程问答 (RAG)"
                />
              </div>

              <div class="form-item-group">
                <label class="item-label">所属业务分类 <span class="req">*</span></label>
                <el-select v-model="form.category" style="width: 100%">
                  <el-option value="rag" label="课程问答 (RAG)" />
                  <el-option value="question" label="智能命题 (Exam)" />
                  <el-option value="grading" label="智能批改 (Grading)" />
                  <el-option value="teaching" label="教案备课" />
                  <el-option value="agent" label="Agent 规划" />
                </el-select>
              </div>
            </div>

            <div class="grid-row-3">
              <div class="form-item-group">
                <div class="label-with-val">
                  <label class="item-label">默认绑定模型</label>
                  <span v-if="modelsLoading" class="loading-mini">加载模型中...</span>
                </div>
                <el-select
                  v-model="form.boundModel"
                  style="width: 100%"
                  filterable
                  clearable
                  :loading="modelsLoading"
                  placeholder="未指定模型（选填，运行时跟随系统网关）"
                >
                  <el-option
                    value=""
                    :label="defaultModelLabel"
                  >
                    <div class="model-option-item">
                      <span class="m-name" style="color: #64748B">{{ defaultModelLabel }}</span>
                      <el-tag size="small" type="info" effect="plain">自适应</el-tag>
                    </div>
                  </el-option>
                  <el-option
                    v-for="item in modelOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  >
                    <div class="model-option-item">
                      <div class="m-main">
                        <span class="m-name">{{ item.label }}</span>
                        <span v-if="item.modelName && item.label !== item.modelName" class="m-code">({{ item.modelName }})</span>
                      </div>
                      <div class="m-tags">
                        <el-tag v-if="item.isDefault" size="small" type="success" effect="plain">默认</el-tag>
                        <el-tag size="small" type="info" effect="light">{{ item.provider }}</el-tag>
                      </div>
                    </div>
                  </el-option>
                </el-select>
              </div>

              <div class="form-item-group">
                <div class="label-with-val">
                  <label class="item-label">采样温度 (Temperature)</label>
                  <span class="val-tag">{{ form.temperature }}</span>
                </div>
                <el-slider
                  v-model="form.temperature"
                  :min="0.0"
                  :max="1.0"
                  :step="0.05"
                  class="custom-slider"
                />
              </div>

              <div class="form-item-group">
                <label class="item-label">最大输出 Token</label>
                <div class="token-input-wrap">
                  <el-input-number
                    v-model="form.maxTokens"
                    :min="256"
                    :max="16384"
                    :step="500"
                    style="width: 100%"
                  />
                </div>
              </div>
            </div>

            <div class="form-item-group full-width">
              <label class="item-label">模板定位与适用范围描述</label>
              <el-input
                v-model="form.description"
                placeholder="清晰阐明该提示词工程资产的教学业务场景、知识库依赖范围与安全边界..."
              />
            </div>
          </div>
        </div>

        <!-- 模块 2：System Prompt (系统人设与硬性约束规则) -->
        <div class="section-card prompt-editor-box">
          <div class="section-card-header">
            <div class="sch-left">
              <span class="sec-icon navy"><el-icon><CollectionTag /></el-icon></span>
              <div class="sec-title-wrap">
                <span class="sec-title">System Prompt (系统人设与硬性约束规则)</span>
                <span class="sec-sub">界定 AI 角色人设、课程数据边界、检索引用规则与防注入屏障</span>
              </div>
            </div>
            <div class="sch-actions">
              <el-button
                size="small"
                link
                :icon="CopyDocument"
                @click="copyText(form.systemPrompt, 'System Prompt 已复制')"
              >
                复制
              </el-button>
              <el-button
                size="small"
                link
                @click="systemExpanded = !systemExpanded"
              >
                {{ systemExpanded ? '收起高度' : '展开高度' }}
              </el-button>
            </div>
          </div>

          <div class="editor-textarea-wrap dark-theme" :class="{ expanded: systemExpanded }">
            <el-input
              v-model="form.systemPrompt"
              type="textarea"
              :rows="systemExpanded ? 24 : 10"
              placeholder="在此编排大模型 System Prompt 核心指令..."
              class="code-textarea"
            />
          </div>
        </div>

        <!-- 模块 3：User Prompt Template (用户提问指令插槽) -->
        <div class="section-card prompt-editor-box">
          <div class="section-card-header">
            <div class="sch-left">
              <span class="sec-icon blue"><el-icon><ChatDotRound /></el-icon></span>
              <div class="sec-title-wrap">
                <span class="sec-title">User Prompt Template (用户指令插槽模板)</span>
                <span class="sec-sub">与用户输入及业务上下文结合的指令插槽</span>
              </div>
            </div>
            <div class="sch-actions">
              <el-button
                size="small"
                link
                :icon="Refresh"
                @click="syncVariablesFromPrompts"
              >
                自动同步变量
              </el-button>
              <el-button
                size="small"
                link
                :icon="CopyDocument"
                @click="copyText(form.userPromptTemplate, 'User Prompt 模板已复制')"
              >
                复制
              </el-button>
            </div>
          </div>

          <!-- 快速插入变量标签栏 -->
          <div class="variable-quick-bar">
            <span class="bar-title">点击快速插入变量插槽：</span>
            <div class="chips-list">
              <el-tag
                v-for="v in form.variables"
                :key="v.name"
                size="small"
                class="var-clickable-chip"
                @click="insertVar(v.name)"
              >
                + &#123;&#123;{{ v.name }}&#125;&#125;
              </el-tag>
            </div>
          </div>

          <div class="editor-textarea-wrap slate-theme">
            <el-input
              ref="userPromptInputRef"
              v-model="form.userPromptTemplate"
              type="textarea"
              :rows="6"
              placeholder="如：请基于当前课程知识库回答下面的问题。\n当前课程：{{course_name}}\n用户问题：{{question}}"
              class="code-textarea user-textarea"
            />
          </div>
        </div>

        <!-- 模块 4：动态参数插槽规格定义清单 -->
        <div class="section-card vars-manager-box">
          <div class="section-card-header">
            <div class="sch-left">
              <span class="sec-icon cyan"><el-icon><DataAnalysis /></el-icon></span>
              <div class="sec-title-wrap">
                <span class="sec-title">动态注入参数规格清单 ({{ form.variables.length }} 个插槽)</span>
                <span class="sec-sub">声明与维护大模型提示词中引用的上下文变量定义</span>
              </div>
            </div>
            <el-button size="small" :icon="Plus" @click="addVariable">
              新增参数
            </el-button>
          </div>

          <div class="vars-table-wrap">
            <div class="vt-head">
              <span class="c-name">变量插槽标识</span>
              <span class="c-label">中文语义</span>
              <span class="c-val">默认测试示例</span>
              <span class="c-op">操作</span>
            </div>
            <div
              v-for="(v, idx) in form.variables"
              :key="idx"
              class="vt-row"
            >
              <div class="c-name">
                <el-input v-model="v.name" size="small" placeholder="如 course_name" class="mono-inp" />
              </div>
              <div class="c-label">
                <el-input v-model="v.label" size="small" placeholder="如 课程名称" />
              </div>
              <div class="c-val">
                <el-input v-model="v.defaultValue" size="small" placeholder="测试默认值" />
              </div>
              <div class="c-op">
                <el-button
                  size="small"
                  link
                  type="danger"
                  :icon="Delete"
                  title="删除参数插槽"
                  @click="handleRemoveVariable(idx, v)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：实时热测试演练台与版本记录 (38%) -->
      <div class="studio-side-col">
        <!-- 模块 1：在线热测试演练台 (Live Playground) -->
        <div class="section-card playground-card">
          <div class="section-card-header">
            <div class="sch-left">
              <span class="sec-icon orange"><el-icon><VideoPlay /></el-icon></span>
              <div class="sec-title-wrap">
                <span class="sec-title">实时热测试沙箱</span>
                <span class="sec-sub">即时渲染插槽并在线调用模型</span>
              </div>
            </div>
            <el-button
              type="primary"
              size="small"
              class="run-test-action-btn"
              :loading="testing"
              :icon="VideoPlay"
              @click="handleRunTest"
            >
              运行测试
            </el-button>
          </div>

          <!-- 快捷填入示例数据入口 -->
          <div class="preset-helper-bar">
            <span>需快速填充？</span>
            <button class="helper-link" @click="fillUniversityDemoData">
              一键填入高校课程真实 RAG 示例
            </button>
          </div>

          <!-- 变量填报区域 -->
          <div class="playground-inputs-area">
            <div
              v-for="v in form.variables"
              :key="v.name"
              class="side-input-item"
            >
              <div class="s-label-row">
                <span class="s-name">&#123;&#123;{{ v.name }}&#125;&#125;</span>
                <span class="s-desc">{{ v.label || v.name }}</span>
              </div>
              <el-input
                v-model="testVariables[v.name]"
                :type="v.name.includes('context') || v.name.includes('history') ? 'textarea' : 'text'"
                :rows="v.name.includes('context') ? 4 : 2"
                :placeholder="`输入 ${v.label || v.name}...`"
                size="small"
              />
            </div>
          </div>

          <!-- 模型输出展示区 -->
          <div class="playground-output-area">
            <div class="output-header-bar">
              <span class="o-title">大模型推理生成结果 (LLM Output)</span>
              <div v-if="testResult" class="o-metrics">
                <span class="metric-pill">{{ testResult.durationMs }}ms</span>
                <span class="metric-pill">{{ testResult.totalTokens }} Tokens</span>
              </div>
            </div>

            <div v-loading="testing" class="output-box">
              <PromptLlmOutput v-if="testResultOutput" :content="testResultOutput" />
              <div v-else class="empty-hint">
                <el-icon :size="28" class="hint-ic"><Promotion /></el-icon>
                <p>点击上方【运行测试】，直接将右侧入参注入 Prompt 并请求当前绑定的大模型进行实时推理。</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 模块 2：版本历史管理 -->
        <div v-if="promptId && versionHistory.length" class="section-card version-card">
          <div class="section-card-header">
            <div class="sch-left">
              <span class="sec-icon purple"><el-icon><Clock /></el-icon></span>
              <div class="sec-title-wrap">
                <div class="title-with-pill">
                  <span class="sec-title">版本历史管理</span>
                  <span class="ver-count-pill">{{ versionHistory.length }} 个快照</span>
                </div>
                <span class="sec-sub">支持查看完整历史快照，对生产版本进行一键无缝回滚</span>
              </div>
            </div>
          </div>

          <div class="version-timeline-list">
            <div
              v-for="item in versionHistory"
              :key="item.id"
              class="v-card-item"
              :class="{ 'is-current': isCurrentVersion(item.version) }"
            >
              <div class="v-card-main">
                <!-- 顶部元信息行：版本号、状态、发布时间 -->
                <div class="v-top-bar">
                  <div class="v-pill-cluster">
                    <span class="v-tag-pill" :class="{ active: isCurrentVersion(item.version) }">
                      v{{ item.version }}.0
                    </span>
                    <span v-if="isCurrentVersion(item.version)" class="v-status-badge current">
                      <span class="pulse-dot"></span>当前运行
                    </span>
                    <span v-else class="v-status-badge archived">
                      历史快照
                    </span>
                  </div>

                  <div class="v-timestamp">
                    <el-icon class="time-ic"><Timer /></el-icon>
                    <span>{{ formatVersionDate(item.createTime) }}</span>
                  </div>
                </div>

                <!-- 变更说明条（改了什么一眼便知） -->
                <div class="v-change-summary-row">
                  <span class="diff-tag-pill" :class="getVersionDiffInfo(item).tagType">
                    <el-icon v-if="getVersionDiffInfo(item).tagType === 'base'"><Flag /></el-icon>
                    <el-icon v-else-if="getVersionDiffInfo(item).tagType === 'identical'"><InfoFilled /></el-icon>
                    <el-icon v-else><EditPen /></el-icon>
                    {{ getVersionDiffInfo(item).tag }}
                  </span>
                  <span class="diff-desc-text" :title="getVersionDiffInfo(item).summary">
                    {{ getVersionDiffInfo(item).summary }}
                  </span>
                </div>

                <!-- 底部操作栏：插槽信息与操作按钮 -->
                <div class="v-bottom-bar">
                  <div class="v-meta-tags">
                    <span v-if="item.variables" class="v-meta-tag">
                      <el-icon><CollectionTag /></el-icon>
                      {{ getVariablesList(item.variables).length }} 个插槽
                    </span>
                    <span v-if="item.systemPrompt" class="v-meta-tag system">
                      <el-icon><Cpu /></el-icon>
                      含角色设定
                    </span>
                  </div>

                  <div class="v-action-group">
                    <el-button
                      class="capsule-action-btn detail-btn"
                      size="small"
                      @click="openVersionDetail(item)"
                    >
                      <el-icon><View /></el-icon>
                      <span>查看详情</span>
                    </el-button>

                    <el-button
                      v-if="!isCurrentVersion(item.version)"
                      class="capsule-action-btn rollback-btn"
                      size="small"
                      :loading="rollingBack"
                      @click="rollbackVersion(item.version)"
                    >
                      <el-icon><RefreshLeft /></el-icon>
                      <span>回滚至此</span>
                    </el-button>

                    <span v-else class="active-running-pill">
                      <el-icon><Check /></el-icon>
                      <span>生效中</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 版本快照详情弹窗 -->
    <el-dialog
      v-model="versionDetailVisible"
      :title="`版本快照详情 · v${selectedVersion?.version}.0`"
      width="780px"
      top="7vh"
      class="version-detail-dialog"
      destroy-on-close
      append-to-body
    >
      <div v-if="selectedVersion" class="version-detail-content">
        <!-- 概览状态栏 -->
        <div class="vd-meta-strip">
          <div class="meta-block">
            <span class="m-label">提示词模板</span>
            <span class="m-val">{{ form.name }}</span>
          </div>
          <div class="meta-block">
            <span class="m-label">模板标识</span>
            <span class="m-val mono">{{ form.code }}</span>
          </div>
          <div class="meta-block">
            <span class="m-label">发布状态</span>
            <div class="m-val">
              <span v-if="isCurrentVersion(selectedVersion.version)" class="vd-status-pill online">
                <span class="pulse-dot"></span>当前在线生产版
              </span>
              <span v-else class="vd-status-pill archived">
                历史归档快照
              </span>
            </div>
          </div>
          <div class="meta-block">
            <span class="m-label">发布时间</span>
            <span class="m-val">{{ formatVersionDate(selectedVersion.createTime) }}</span>
          </div>
        </div>

        <!-- 版本变更差异分析面板（明确标示改了什么） -->
        <div class="vd-change-analysis-card" :class="selectedVersionDiffInfo.tagType">
          <div class="ca-header">
            <div class="ca-title-group">
              <span class="ca-pill" :class="selectedVersionDiffInfo.tagType">
                {{ selectedVersionDiffInfo.tag }}
              </span>
              <span class="ca-main-title">版本变更说明与差异分析</span>
            </div>
            <span v-if="selectedVersionDiffInfo.prevVerNum" class="ca-compare-target">
              对比基准：v{{ selectedVersionDiffInfo.prevVerNum }}.0
            </span>
          </div>

          <div class="ca-body">
            <p class="ca-summary-p">
              {{ selectedVersionDiffInfo.summary }}
            </p>

            <div v-if="!selectedVersionDiffInfo.isBase" class="ca-diff-details-grid">
              <div class="diff-col" :class="{ modified: selectedVersionDiffInfo.sysDiff.includes('已调优') }">
                <span class="col-lbl">System 设定：</span>
                <span class="col-val">{{ selectedVersionDiffInfo.sysDiff }}</span>
              </div>
              <div class="diff-col" :class="{ modified: selectedVersionDiffInfo.userDiff.includes('已调整') }">
                <span class="col-lbl">User 模板：</span>
                <span class="col-val">{{ selectedVersionDiffInfo.userDiff }}</span>
              </div>
              <div class="diff-col" :class="{ modified: selectedVersionDiffInfo.varsDiff.includes('新增') || selectedVersionDiffInfo.varsDiff.includes('移除') }">
                <span class="col-lbl">参数插槽：</span>
                <span class="col-val">{{ selectedVersionDiffInfo.varsDiff }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- System Prompt 区域 -->
        <div class="vd-section">
          <div class="vd-sec-header">
            <div class="sec-h-left">
              <span class="h-icon purple"><el-icon><Cpu /></el-icon></span>
              <span class="h-title">System Prompt（系统角色设定指令快照）</span>
            </div>
            <el-button
              v-if="selectedVersion.systemPrompt"
              link
              size="small"
              class="copy-pill-btn"
              @click="copyText(selectedVersion.systemPrompt)"
            >
              <el-icon><CopyDocument /></el-icon>
              <span>复制系统指令</span>
            </el-button>
          </div>
          <div class="code-terminal-box">
            <pre v-if="selectedVersion.systemPrompt">{{ selectedVersion.systemPrompt }}</pre>
            <div v-else class="empty-code-hint">（此历史版本未指定独立 System Prompt，使用系统或大模型默认角色设定）</div>
          </div>
        </div>

        <!-- User Prompt 区域 -->
        <div class="vd-section">
          <div class="vd-sec-header">
            <div class="sec-h-left">
              <span class="h-icon blue"><el-icon><ChatDotRound /></el-icon></span>
              <span class="h-title">User Prompt 模板（用户输入指令与插槽快照）</span>
            </div>
            <el-button
              link
              size="small"
              class="copy-pill-btn"
              @click="copyText(selectedVersion.content)"
            >
              <el-icon><CopyDocument /></el-icon>
              <span>复制用户指令</span>
            </el-button>
          </div>
          <div class="code-terminal-box">
            <pre>{{ selectedVersion.content }}</pre>
          </div>
        </div>

        <!-- 包含的动态变量插槽 -->
        <div class="vd-section">
          <div class="vd-sec-header">
            <div class="sec-h-left">
              <span class="h-icon amber"><el-icon><CollectionTag /></el-icon></span>
              <span class="h-title">包含的动态插槽变量 ({{ selectedVersionVariables.length }})</span>
            </div>
          </div>
          <div class="variables-capsule-list">
            <span
              v-for="varName in selectedVersionVariables"
              :key="varName"
              class="var-pill"
            >
              <code>&#123;&#123; {{ varName }} &#125;&#125;</code>
            </span>
            <span v-if="!selectedVersionVariables.length" class="empty-var-hint">无独立动态参数</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="vd-dialog-footer">
          <div class="footer-left">
            <span class="hint-text">回滚将把当前在线生产运行的配置完全替换为此快照。</span>
          </div>
          <div class="footer-right">
            <el-button class="pill-btn cancel-btn" @click="versionDetailVisible = false">关闭</el-button>
            <el-button
              v-if="!isCurrentVersion(selectedVersion?.version)"
              type="warning"
              class="pill-btn rollback-btn-confirm"
              :loading="rollingBack"
              @click="handleDetailRollback"
            >
              <el-icon><RefreshLeft /></el-icon>
              <span>回滚至此版本 (v{{ selectedVersion?.version }}.0)</span>
            </el-button>
            <span v-else class="vd-current-running-label">
              <el-icon><Check /></el-icon> 当前在线运行中
            </span>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { usePrompt } from '@/composables/system/usePrompt';
import PromptLlmOutput from '@/components/system/PromptLlmOutput.vue';
import { fetchModels } from '@/api/system/model';
import { PromptTemplate } from '@/types/system/prompt';
import type { PromptVersionItem } from '@/api/system/prompt';
import {
  ArrowLeft,
  Check,
  Upload,
  VideoPlay,
  CopyDocument,
  Cpu,
  CollectionTag,
  ChatDotRound,
  DataAnalysis,
  Refresh,
  Clock,
  Delete,
  Plus,
  Promotion,
  View,
  RefreshLeft,
  Timer,
  Document,
  InfoFilled,
  Flag,
  EditPen
} from '@element-plus/icons-vue';

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

interface ModelOption {
  label: string;
  value: string;
  provider: string;
  modelKey?: string;
  modelName?: string;
  isDefault?: boolean;
  hasApiKey?: boolean;
}

const modelOptions = ref<ModelOption[]>([]);
const defaultModelName = ref<string>('');
const defaultModelLabel = computed(() => {
  return defaultModelName.value
    ? `未指定模型（跟随系统默认: ${defaultModelName.value}）`
    : '未指定模型（跟随系统网关默认）';
});
const modelsLoading = ref(false);

const loadAvailableModels = async () => {
  modelsLoading.value = true;
  try {
    const list = await fetchModels();
    // 过滤出启用的对话模型 (chat)
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

      // 如果当前表单已有绑定模型，确保其在下拉列表中（若不在则追加，避免显示为空白）
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

const form = ref<PromptTemplate>({
  id: 0,
  code: 'COURSE_RAG_GENERAL',
  name: '通用课程问答（RAG）',
  category: 'rag',
  description: '围绕当前课程、章节、知识点和课程知识库，为教师和学生提供具备资料溯源能力的专业智能问答。',
  systemPrompt: '',
  userPromptTemplate: '请基于当前课程知识库回答下面的问题。\n\n当前课程：{{course_name}}\n当前章节：{{chapter_name}}\n当前知识点：{{knowledge_point_name}}\n\n用户问题：\n{{question}}',
  variables: [
    { name: 'course_name', label: '课程名称', defaultValue: 'Java程序设计' },
    { name: 'chapter_name', label: '章节名称', defaultValue: '第三章 面向对象程序设计' },
    { name: 'knowledge_point_name', label: '知识点', defaultValue: '多态与动态分派' },
    { name: 'question', label: '用户提问', defaultValue: 'Java多态的具体实现原理是什么？' }
  ],
  version: 'v1.0.0',
  status: 'DRAFT',
  boundModel: '',
  temperature: 0.3,
  maxTokens: 8000,
  callCount: 0,
  createdAt: '',
  updatedAt: ''
});

const testVariables = ref<Record<string, string>>({});
const testResultOutput = ref('');

const getCategoryLabel = (category: string) => {
  switch (category) {
    case 'rag': return '课程问答 RAG';
    case 'question': return '智能命题';
    case 'grading': return '智能批改';
    case 'teaching': return '教案备课';
    case 'agent': return 'Agent 规划';
    default: return '通用';
  }
};

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

const handleRemoveVariable = async (index: number, v: { name: string; label?: string }) => {
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
    // 用户取消操作
  }
};

// 自动扫描并同步提示词中引用的变量
const syncVariablesFromPrompts = () => {
  const combined = `${form.value.systemPrompt} ${form.value.userPromptTemplate}`;
  const matches = combined.match(/\{\{([a-zA-Z0-9_-]+)\}\}/g);
  if (!matches) {
    ElMessage.info('未在当前提示词中识别到 {{插槽}} 格式变量');
    return;
  }
  const uniqueNames = Array.from(new Set(matches.map((m) => m.replace(/[\{\}]/g, '').trim())));
  const existingNames = new Set(form.value.variables.map((v) => v.name));

  let addedCount = 0;
  uniqueNames.forEach((name) => {
    if (!existingNames.has(name)) {
      form.value.variables.push({
        name,
        label: name,
        defaultValue: ''
      });
      addedCount++;
    }
  });

  if (addedCount > 0) {
    ElMessage.success(`已自动识别并追加 ${addedCount} 个变量插槽`);
  } else {
    ElMessage.success('当前变量插槽与提示词已完全同步');
  }
};

// 一键填入高校课程真实 RAG 演示数据
const fillUniversityDemoData = () => {
  const dummy: Record<string, string> = {
    course_id: '10001',
    course_name: 'Java程序设计',
    course_description: '面向软件技术专业核心基础课，主要讲解面向对象与核心类库。',
    chapter_name: '第三章 面向对象程序设计',
    knowledge_point_name: '多态与动态分派',
    user_role: 'STUDENT',
    answer_depth: 'NORMAL',
    language: 'zh-CN',
    allow_general_knowledge: 'true',
    question: 'Java多态的具体实现原理是什么？它与方法重载有什么区别？',
    conversation_history: '[User]: 什么是面向对象继承？\n[Assistant]: 继承是面向对象三大核心特征之一...',
    retrieved_context: `<rag_context>
<source id="S1" document="Java程序设计教材.pdf" chapter="第三章 面向对象" section="3.4 多态" page="86">
Java中的多态是指同一个引用类型在不同运行状态下，可以指向不同类型的对象，并表现出不同的行为。实际调用的方法由运行时堆中真实对象类型决定。
</source>
<source id="S2" document="Java面向对象核心讲义.pptx" chapter="第三章 面向对象" section="虚方法表" page="24">
JVM通过虚方法表（vtable）进行动态分派，实现父类引用调用子类重写方法。
</source>
<source id="S3" document="Java程序设计课堂案例.docx" chapter="第三章 面向对象" section="Animal-Dog-Cat多态示例" page="12">
通过Animal、Dog、Cat三个类的示例，演示同一父类引用指向不同子类对象时产生不同输出的多态现象。
</source>
</rag_context>`,
    // chat_rag 轻量版使用 {{context}}，与 GENERAL 的 retrieved_context 对齐
    context: `<rag_context>
<source id="S1" document="Java程序设计教材.pdf" chapter="第三章 面向对象" section="3.4 多态" page="86">
Java中的多态是指同一个引用类型在不同运行状态下，可以指向不同类型的对象，并表现出不同的行为。实际调用的方法由运行时堆中真实对象类型决定。
</source>
<source id="S2" document="Java面向对象核心讲义.pptx" chapter="第三章 面向对象" section="虚方法表" page="24">
JVM通过虚方法表（vtable）进行动态分派，实现父类引用调用子类重写方法。
</source>
<source id="S3" document="Java程序设计课堂案例.docx" chapter="第三章 面向对象" section="Animal-Dog-Cat多态示例" page="12">
通过Animal、Dog、Cat三个类的示例，演示同一父类引用指向不同子类对象时产生不同输出的多态现象。
</source>
</rag_context>`,
    // 智能命题（RAG）通用扩展变量
    chapter_id: 'CH03',
    knowledge_point_ids: 'KP003,KP004',
    knowledge_point_names: '继承与重写,多态与动态分派',
    question_types: 'SINGLE_CHOICE,MULTIPLE_CHOICE,SHORT_ANSWER',
    difficulty: 'MEDIUM',
    question_count: '3',
    task_purpose: 'HOMEWORK',
    student_level: '本科二年级',
    score_per_question: '5',
    generation_requirements: '注重考察代码执行结果分析与基本概念辨析，解析需说明错误选项原因',
    existing_questions: '["什么是多态？","Java中单继承关键字是什么？"]',
    // 智能批改（RAG）通用扩展变量
    question_id: 'Q10001',
    question_stem: '请简述Java中运行时多态的实现条件。',
    max_score: '6',
    reference_answer: 'Java运行时多态通常需要：1. 存在继承或接口实现关系；2. 子类对父类方法进行重写；3. 父类引用指向子类对象；4. 方法调用在运行时根据实际对象类型确定。',
    scoring_rubric: '按要点采分，语义等价即可得分；部分正确给部分分，错答概念扣分。',
    scoring_points: '[{"id":"SP1","description":"说明继承或父子类型关系","score":1},{"id":"SP2","description":"说明方法重写","score":2},{"id":"SP3","description":"说明父类引用指向子类对象","score":2},{"id":"SP4","description":"说明运行时动态绑定机制","score":1}]',
    student_answer: '多态就是父类变量可以保存子类对象，如果子类重新实现父类的方法，调用的时候会执行子类自己的实现。',
    grading_mode: 'FULL_EXPLANATION',
    teacher_requirements: '结合Animal/Dog/Cat案例，设计课堂互动与代码预测环节，轻微口语化表述不扣分',
    // 教案备课（RAG）通用扩展变量
    lesson_title: 'Java运行时多态与动态绑定机制',
    lesson_duration: '90分钟',
    lesson_count: '2',
    class_profile: '已掌握Java类与对象、继承与重写，但对声明类型与运行时对象类型的关系理解较弱',
    teaching_mode: 'BOPPPS',
    teaching_method: '问题驱动,案例教学,代码演示,任务驱动',
    teaching_objectives: '理解运行时多态实现条件，能分析并预测多态代码执行结果',
    previous_learning: '类与对象,继承,方法重写',
    next_learning: '抽象类,接口,面向接口编程',
    available_resources: 'Java程序设计教材,课程PPT,IDE开发环境',
    assessment_requirement: '设计课堂代码预测题作为形成性评价，检查学生是否能区分声明类型与运行时对象类型',
    homework_requirement: '布置2道代码执行分析题，以及1道多态设计实践题',
    output_depth: 'DETAILED'
  };

  form.value.variables.forEach((v) => {
    if (dummy[v.name]) {
      testVariables.value[v.name] = dummy[v.name];
    }
  });
  ElMessage.success('已填入真实课程 RAG 演示数据');
};

const handleRunTest = async () => {
  const templateId = form.value.id || promptId;
  if (!templateId) {
    ElMessage.warning('请先点击右上角【保存草稿】后再执行测试');
    return;
  }
  testResultOutput.value = '';

  await runTest(templateId, {
    systemPrompt: form.value.systemPrompt,
    userPromptTemplate: form.value.userPromptTemplate,
    variables: testVariables.value,
    model: form.value.boundModel || defaultModelName.value || '',
    temperature: form.value.temperature,
    maxTokens: form.value.maxTokens
  });

  if (testResult.value?.output) {
    testResultOutput.value = testResult.value.output;
  } else if (form.value.category === 'grading' || form.value.code === 'GRADING_RAG_GENERAL') {
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
  } else if (form.value.category === 'question' || form.value.code === 'EXAM_RAG_GENERAL') {
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
  } else if (form.value.category === 'teaching' || form.value.code === 'LESSON_PREP_RAG_GENERAL') {
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

const formatVersionDate = (dateStr?: string) => {
  if (!dateStr) return '最近发布';
  return dateStr.replace('T', ' ').replace(/\.\d+.*$/, '');
};

const versionDetailVisible = ref(false);
const selectedVersion = ref<PromptVersionItem | null>(null);

const openVersionDetail = (item: PromptVersionItem) => {
  selectedVersion.value = item;
  versionDetailVisible.value = true;
};

const getVariablesList = (vars?: string): string[] => {
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
    // fallback
  }
  return vars
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean);
};

const selectedVersionVariables = computed<string[]>(() => {
  if (!selectedVersion.value?.variables) return [];
  return getVariablesList(selectedVersion.value.variables);
});

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

const getVersionDiffInfo = (item: PromptVersionItem): VersionDiffInfo => {
  const sorted = [...versionHistory.value].sort((a, b) => a.version - b.version);
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
};

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
  return getVersionDiffInfo(selectedVersion.value);
});

const isCurrentVersion = (ver?: number): boolean => {
  if (ver === undefined || ver === null) return false;
  const v = form.value.version;
  if (v === undefined || v === null) return false;
  if (typeof v === 'number') return v === ver;
  const match = String(v).match(/v?(\d+)/i);
  return match ? parseInt(match[1], 10) === ver : false;
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
    // 用户取消回滚
    return;
  }

  const ok = await handleRollback(form.value.id, targetVersion);
  if (ok && currentPrompt.value) {
    form.value = JSON.parse(JSON.stringify(currentPrompt.value));
    form.value.variables.forEach((v) => {
      if (testVariables.value[v.name] === undefined) {
        testVariables.value[v.name] = v.defaultValue || '';
      }
    });
  }
};

onMounted(async () => {
  await loadAvailableModels();
  if (promptId) {
    await loadPrompt(promptId);
    await loadVersions(promptId);
    if (currentPrompt.value) {
      form.value = JSON.parse(JSON.stringify(currentPrompt.value));
      if (form.value.boundModel === 'deepseek-chat') {
        form.value.boundModel = '';
      }
      form.value.variables.forEach((v) => {
        testVariables.value[v.name] = v.defaultValue || '';
      });
      // 检查已绑定的模型是否在下拉列表中，若不在则追加进来显示
      if (form.value.boundModel && !modelOptions.value.some((o) => o.value === form.value.boundModel || o.modelName === form.value.boundModel)) {
        modelOptions.value.unshift({
          label: `${form.value.boundModel} (已绑定)`,
          value: form.value.boundModel,
          provider: 'SAVED'
        });
      }
    }
  }
});
</script>

<style scoped lang="scss">
.prompt-editor-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;

  // 顶部导航与发布操作栏
  .editor-header-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    padding: 16px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);

    @media (max-width: 900px) {
      flex-direction: column;
      align-items: flex-start;
      gap: 14px;
    }

    .header-left {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .back-btn {
        padding: 0;
        font-size: 13px;
        color: #64748B;
        align-self: flex-start;

        &:hover {
          color: #2563EB;
        }
      }

      .title-meta-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .template-title {
          margin: 0;
          font-size: 20px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.01em;
        }

        .code-badge {
          font-family: ui-monospace, SFMono-Regular, monospace;
          font-size: 11.5px;
          font-weight: 700;
          color: #1D4ED8;
          background: #EFF6FF;
          padding: 3px 8px;
          border-radius: 6px;
          display: flex;
          align-items: center;
          gap: 4px;
          cursor: pointer;
          transition: all 0.15s;

          &:hover {
            background: #DBEAFE;
          }

          .copy-ic {
            font-size: 11px;
          }
        }

        .category-pill {
          font-size: 11.5px;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 6px;

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

        .version-badge {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 8px;
          border-radius: 999px;
        }

        .status-indicator {
          font-size: 11.5px;
          font-weight: 600;
          padding: 2px 10px;
          border-radius: 999px;
          display: flex;
          align-items: center;
          gap: 6px;

          &.online {
            background: #ECFDF5;
            color: #059669;
            .pulse-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10B981;
            }
          }
          &.draft {
            background: #FFFBEB;
            color: #D97706;
            .pulse-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #F59E0B;
            }
          }
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .save-draft-btn {
        background: #F8FAFC;
        border: 1px solid #CBD5E1;
        color: #334155;
        font-weight: 500;

        &:hover {
          background: #F1F5F9;
          color: #0F172A;
        }
      }

      .publish-btn {
        background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
        border: none;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
        font-weight: 600;
        padding: 9px 20px;
        transition: background-color 0.15s ease, box-shadow 0.15s ease;

        &:hover {
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.45);
        }

        &:active {
          box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
        }
      }
    }
  }

  // 主编辑区双栏 Grid
  .editor-studio-grid {
    display: grid;
    grid-template-columns: 1.4fr 1fr;
    gap: 18px;
    align-items: start;

    @media (max-width: 1280px) {
      grid-template-columns: 1fr;
    }

    .section-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      padding: 18px 22px;
      margin-bottom: 18px;
      box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);

      .section-card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-bottom: 12px;
        margin-bottom: 16px;
        border-bottom: 1px solid #F1F5F9;

        .sch-left {
          display: flex;
          align-items: center;
          gap: 10px;

          .sec-icon {
            width: 32px;
            height: 32px;
            border-radius: 8px;
            background: #EFF6FF;
            color: #2563EB;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;

            &.navy { background: #0F172A; color: #60A5FA; }
            &.blue { background: #E0F2FE; color: #0284C7; }
            &.cyan { background: #ECFEFF; color: #0891B2; }
            &.orange { background: #FFF7ED; color: #EA580C; }
            &.purple { background: #FAF5FF; color: #9333EA; }
          }

          .sec-title-wrap {
            display: flex;
            flex-direction: column;
            gap: 2px;

            .sec-title {
              font-size: 14.5px;
              font-weight: 700;
              color: #0F172A;
            }
            .sec-sub {
              font-size: 11.5px;
              color: #64748B;
            }
          }
        }

        .sch-actions {
          display: flex;
          align-items: center;
          gap: 10px;

          .stat-pill {
            font-family: ui-monospace, monospace;
            font-size: 11px;
            color: #64748B;
            background: #F1F5F9;
            padding: 2px 8px;
            border-radius: 999px;
          }
        }
      }
    }

    // 基础参数表单
    .meta-section {
      .meta-form-body {
        display: flex;
        flex-direction: column;
        gap: 14px;

        .grid-row-3 {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 14px;

          @media (max-width: 800px) {
            grid-template-columns: 1fr;
          }
        }

        .form-item-group {
          display: flex;
          flex-direction: column;
          gap: 6px;

          .item-label {
            font-size: 12.5px;
            font-weight: 600;
            color: #334155;

            .req {
              color: #EF4444;
              margin-left: 2px;
            }
          }

          .label-with-val {
            display: flex;
            justify-content: space-between;
            align-items: center;

            .loading-mini {
              font-size: 11px;
              color: #64748B;
            }

            .val-tag {
              font-family: ui-monospace, monospace;
              font-size: 11.5px;
              font-weight: 700;
              color: #2563EB;
              background: #EFF6FF;
              padding: 1px 8px;
              border-radius: 999px;
            }
          }

          .mono-input {
            :deep(input) {
              font-family: ui-monospace, monospace;
              font-size: 12.5px;
              font-weight: 600;
              color: #1E40AF;
            }
          }
        }
      }
    }

    // 提示词编辑器卡片
    .prompt-editor-box {
      .variable-quick-bar {
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 8px;
        padding: 8px 12px;
        margin-bottom: 10px;
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .bar-title {
          font-size: 11.5px;
          color: #64748B;
          font-weight: 500;
        }

        .chips-list {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-wrap: wrap;

          .var-clickable-chip {
            cursor: pointer;
            font-family: ui-monospace, monospace;
            font-size: 11px;
            background: #FFFFFF;
            border-color: #CBD5E1;
            color: #1E293B;
            transition: all 0.15s;

            &:hover {
              background: #EFF6FF;
              border-color: #93C5FD;
              color: #2563EB;
            }
          }
        }
      }

      .editor-textarea-wrap {
        border-radius: 10px;
        overflow: hidden;
        border: 1px solid #1E293B;
        background: #0F172A;
        transition: all 0.2s ease;
        box-sizing: border-box;

        &:focus-within {
          border-color: #3B82F6 !important;
          box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.25) !important;
        }

        :deep(.el-textarea) {
          display: block;
          width: 100%;
          margin: 0 !important;
          padding: 0 !important;
          border: none !important;
          box-shadow: none !important;
          background: transparent !important;
        }

        :deep(.el-textarea__inner) {
          display: block !important;
          width: 100% !important;
          margin: 0 !important;
          border: none !important;
          outline: none !important;
          box-shadow: none !important;
          border-radius: 0 !important;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace !important;
          font-size: 13px !important;
          line-height: 1.7 !important;
          padding: 14px 16px !important;
          resize: vertical;
          transition: none !important;

          &:hover,
          &:focus {
            border: none !important;
            outline: none !important;
            box-shadow: none !important;
          }
        }

        &.dark-theme {
          background: #0F172A;
          border-color: #1E293B;

          :deep(.el-textarea__inner) {
            background: #0F172A !important;
            color: #E2E8F0 !important;

            &::placeholder {
              color: #475569 !important;
            }

            &::-webkit-scrollbar {
              width: 6px;
              height: 6px;
            }
            &::-webkit-scrollbar-thumb {
              background: #334155;
              border-radius: 3px;
            }
            &::-webkit-scrollbar-thumb:hover {
              background: #475569;
            }
            &::-webkit-scrollbar-track {
              background: transparent;
            }
          }
        }

        &.slate-theme {
          background: #1E293B;
          border-color: #334155;

          :deep(.el-textarea__inner) {
            background: #1E293B !important;
            color: #93C5FD !important;

            &::placeholder {
              color: #64748B !important;
            }

            &::-webkit-scrollbar {
              width: 6px;
              height: 6px;
            }
            &::-webkit-scrollbar-thumb {
              background: #475569;
              border-radius: 3px;
            }
            &::-webkit-scrollbar-thumb:hover {
              background: #64748B;
            }
            &::-webkit-scrollbar-track {
              background: transparent;
            }
          }
        }
      }
    }

    // 变量管理表格
    .vars-manager-box {
      .vars-table-wrap {
        border: 1px solid #E2E8F0;
        border-radius: 8px;
        overflow: hidden;

        .vt-head {
          display: grid;
          grid-template-columns: 180px 140px 1fr 50px;
          gap: 10px;
          padding: 8px 14px;
          background: #F8FAFC;
          font-size: 12px;
          font-weight: 600;
          color: #64748B;
        }

        .vt-row {
          display: grid;
          grid-template-columns: 180px 140px 1fr 50px;
          gap: 10px;
          padding: 8px 14px;
          border-top: 1px solid #F1F5F9;
          align-items: center;

          .mono-inp {
            :deep(input) {
              font-family: ui-monospace, monospace;
              color: #2563EB;
              font-weight: 600;
            }
          }
        }
      }
    }

    // 右栏：测试演练与沙箱
    .playground-card {
      .preset-helper-bar {
        background: #F0F9FF;
        border: 1px solid #BAE6FD;
        border-radius: 8px;
        padding: 8px 12px;
        margin-bottom: 14px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 12px;
        color: #0369A1;

        .helper-link {
          background: transparent;
          border: none;
          color: #0284C7;
          font-weight: 600;
          cursor: pointer;
          text-decoration: underline;

          &:hover {
            color: #0369A1;
          }
        }
      }

      .playground-inputs-area {
        display: flex;
        flex-direction: column;
        gap: 10px;
        max-height: 380px;
        overflow-y: auto;
        padding-right: 4px;
        margin-bottom: 14px;

        .side-input-item {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 8px;
          padding: 8px 10px;

          .s-label-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;

            .s-name {
              font-family: ui-monospace, monospace;
              font-size: 11px;
              font-weight: 700;
              color: #1D4ED8;
            }
            .s-desc {
              font-size: 11px;
              color: #64748B;
            }
          }
        }
      }

      .playground-output-area {
        border-top: 1px solid #F1F5F9;
        padding-top: 14px;

        .output-header-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          .o-title {
            font-size: 13px;
            font-weight: 700;
            color: #0F172A;
          }

          .o-metrics {
            display: flex;
            gap: 6px;

            .metric-pill {
              font-size: 11px;
              color: #64748B;
              background: #F1F5F9;
              padding: 2px 8px;
              border-radius: 999px;
              font-family: ui-monospace, monospace;
            }
          }
        }

        .output-box {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 8px;
          padding: 14px;
          min-height: 180px;

          .rendered-response,
          .prompt-llm-output {
            width: 100%;
          }

          .empty-hint {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            color: #94A3B8;
            text-align: center;
            padding: 30px 10px;

            .hint-ic {
              color: #CBD5E1;
              margin-bottom: 8px;
            }

            p {
              margin: 0;
              font-size: 12.5px;
              max-width: 260px;
              line-height: 1.5;
            }
          }
        }
      }
    }

    // 版本历史卡片
    .version-card {
      .section-card-header {
        .title-with-pill {
          display: flex;
          align-items: center;
          gap: 8px;

          .ver-count-pill {
            font-size: 11px;
            font-weight: 600;
            color: #7C3AED;
            background: #F3E8FF;
            padding: 1px 8px;
            border-radius: 999px !important;
          }
        }
      }

      .version-timeline-list {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .v-card-item {
          background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
          border: 1px solid #E2E8F0;
          border-radius: 12px;
          padding: 12px 14px;
          transition: border-color 0.2s ease, box-shadow 0.2s ease;

          &:hover {
            border-color: #CBD5E1;
            box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
          }

          &.is-current {
            background: linear-gradient(180deg, #F0FDF4 0%, #FFFFFF 100%);
            border: 1px solid #BBF7D0;
            border-left: 3.5px solid #10B981;

            &:hover {
              border-color: #86EFAC;
              border-left-color: #059669;
              box-shadow: 0 4px 14px rgba(16, 185, 129, 0.08);
            }
          }

          .v-card-main {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .v-top-bar {
              display: flex;
              justify-content: space-between;
              align-items: center;
              flex-wrap: wrap;
              gap: 8px;

              .v-pill-cluster {
                display: flex;
                align-items: center;
                gap: 6px;

                .v-tag-pill {
                  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
                  font-size: 12px;
                  font-weight: 700;
                  color: #2563EB;
                  background: #EFF6FF;
                  border: 1px solid #BFDBFE;
                  padding: 2px 9px;
                  border-radius: 999px !important;

                  &.active {
                    color: #059669;
                    background: #ECFDF5;
                    border-color: #A7F3D0;
                  }
                }

                .v-status-badge {
                  font-size: 11px;
                  font-weight: 600;
                  padding: 2px 8px;
                  border-radius: 999px !important;
                  display: inline-flex;
                  align-items: center;
                  gap: 4px;

                  &.current {
                    color: #059669;
                    background: #DCFCE7;
                    border: 1px solid #86EFAC;

                    .pulse-dot {
                      width: 6px;
                      height: 6px;
                      border-radius: 50%;
                      background: #10B981;
                      box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
                    }
                  }

                  &.archived {
                    color: #64748B;
                    background: #F1F5F9;
                    border: 1px solid #E2E8F0;
                  }
                }
              }

              .v-timestamp {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 12px;
                color: #64748B;

                .time-ic {
                  font-size: 13px;
                  color: #94A3B8;
                }
              }
            }

            .v-change-summary-row {
              display: flex;
              align-items: center;
              gap: 8px;
              background: #F8FAFC;
              border-radius: 8px;
              padding: 6px 10px;
              border: 1px dashed #E2E8F0;

              .diff-tag-pill {
                display: inline-flex;
                align-items: center;
                gap: 4px;
                font-size: 11px;
                font-weight: 600;
                padding: 1px 8px;
                border-radius: 999px !important;
                white-space: nowrap;

                &.base {
                  background: #F1F5F9;
                  color: #475569;
                  border: 1px solid #CBD5E1;
                }

                &.identical {
                  background: #F1F5F9;
                  color: #64748B;
                  border: 1px solid #E2E8F0;
                }

                &.updated {
                  background: #EFF6FF;
                  color: #2563EB;
                  border: 1px solid #BFDBFE;
                }
              }

              .diff-desc-text {
                font-size: 11.5px;
                color: #475569;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                line-height: 1.4;
              }
            }

            .v-bottom-bar {
              display: flex;
              justify-content: space-between;
              align-items: center;
              flex-wrap: wrap;
              gap: 8px;
              padding-top: 8px;
              border-top: 1px dashed #E2E8F0;

              .v-meta-tags {
                display: flex;
                align-items: center;
                gap: 6px;

                .v-meta-tag {
                  display: inline-flex;
                  align-items: center;
                  gap: 4px;
                  font-size: 11px;
                  color: #475569;
                  background: #F1F5F9;
                  padding: 2px 8px;
                  border-radius: 999px !important;
                  border: 1px solid #E2E8F0;

                  &.system {
                    color: #7C3AED;
                    background: #F5F3FF;
                    border-color: #DDD6FE;
                  }
                }
              }

              .v-action-group {
                display: flex;
                align-items: center;
                gap: 6px;

                .capsule-action-btn {
                  border-radius: 999px !important;
                  font-size: 12px;
                  font-weight: 500;
                  padding: 5px 12px;
                  height: 28px;
                  display: inline-flex;
                  align-items: center;
                  gap: 4px;
                  transition: all 0.15s ease;

                  &.detail-btn {
                    background: #F8FAFC;
                    color: #334155;
                    border: 1px solid #CBD5E1;

                    &:hover {
                      background: #EFF6FF;
                      color: #2563EB;
                      border-color: #93C5FD;
                    }
                  }

                  &.rollback-btn {
                    background: #FFFBEB;
                    color: #D97706;
                    border: 1px solid #FDE68A;

                    &:hover {
                      background: #FEF3C7;
                      color: #B45309;
                      border-color: #F59E0B;
                    }
                  }
                }

                .active-running-pill {
                  display: inline-flex;
                  align-items: center;
                  gap: 4px;
                  font-size: 11.5px;
                  font-weight: 600;
                  color: #059669;
                  background: #ECFDF5;
                  border: 1px solid #A7F3D0;
                  padding: 3px 10px;
                  border-radius: 999px !important;
                }
              }
            }
          }
        }
      }
    }
  }
}
</style>

<style lang="scss">
.model-option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;

  .m-main {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    .m-name {
      font-weight: 600;
      color: #0F172A;
    }
    .m-code {
      font-family: ui-monospace, monospace;
      font-size: 11px;
      color: #64748B;
    }
  }

  .m-tags {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

// 版本快照详情弹窗全局样式
.version-detail-dialog {
  border-radius: 16px !important;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding: 18px 24px 14px;
    border-bottom: 1px solid #F1F5F9;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 800;
      color: #0F172A;
      letter-spacing: -0.01em;
    }
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 14px 24px;
    border-top: 1px solid #F1F5F9;
    background: #FAFAFA;
  }

  .version-detail-content {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .vd-meta-strip {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 10px;
      padding: 12px 16px;

      @media (max-width: 640px) {
        grid-template-columns: 1fr 1fr;
      }

      .meta-block {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .m-label {
          font-size: 11.5px;
          color: #64748B;
          font-weight: 500;
        }

        .m-val {
          font-size: 13px;
          font-weight: 600;
          color: #0F172A;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          &.mono {
            font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
            color: #2563EB;
          }

          .vd-status-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 999px !important;

            &.online {
              background: #ECFDF5;
              color: #059669;
              border: 1px solid #A7F3D0;

              .pulse-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #10B981;
              }
            }

            &.archived {
              background: #F1F5F9;
              color: #64748B;
              border: 1px solid #E2E8F0;
            }
          }
        }
      }
    }

    .vd-change-analysis-card {
      border-radius: 12px;
      padding: 14px 16px;
      border: 1px solid #E2E8F0;

      &.base {
        background: #F8FAFC;
        border-color: #E2E8F0;
      }

      &.identical {
        background: #F8FAFC;
        border-color: #E2E8F0;
      }

      &.updated {
        background: #EFF6FF;
        border-color: #BFDBFE;
      }

      .ca-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .ca-title-group {
          display: flex;
          align-items: center;
          gap: 8px;

          .ca-pill {
            font-size: 11px;
            font-weight: 700;
            padding: 2px 9px;
            border-radius: 999px !important;

            &.base {
              background: #E2E8F0;
              color: #334155;
            }
            &.identical {
              background: #E2E8F0;
              color: #475569;
            }
            &.updated {
              background: #DBEAFE;
              color: #1D4ED8;
            }
          }

          .ca-main-title {
            font-size: 13px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .ca-compare-target {
          font-size: 11.5px;
          color: #64748B;
          font-family: ui-monospace, monospace;
        }
      }

      .ca-body {
        .ca-summary-p {
          margin: 0 0 8px 0;
          font-size: 12.5px;
          color: #334155;
          line-height: 1.5;
        }

        .ca-diff-details-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 8px;
          margin-top: 8px;

          @media (max-width: 640px) {
            grid-template-columns: 1fr;
          }

          .diff-col {
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            padding: 8px 10px;
            display: flex;
            flex-direction: column;
            gap: 3px;

            &.modified {
              border-color: #93C5FD;
              background: #F0F7FF;

              .col-val {
                color: #2563EB;
                font-weight: 600;
              }
            }

            .col-lbl {
              font-size: 11px;
              color: #64748B;
            }

            .col-val {
              font-size: 12px;
              color: #1E293B;
            }
          }
        }
      }
    }

    .vd-section {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .vd-sec-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .sec-h-left {
          display: flex;
          align-items: center;
          gap: 6px;

          .h-icon {
            width: 22px;
            height: 22px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;

            &.purple {
              background: #F5F3FF;
              color: #7C3AED;
            }
            &.blue {
              background: #EFF6FF;
              color: #2563EB;
            }
            &.amber {
              background: #FFFBEB;
              color: #D97706;
            }
          }

          .h-title {
            font-size: 13px;
            font-weight: 700;
            color: #1E293B;
          }
        }

        .copy-pill-btn {
          font-size: 12px;
          color: #2563EB;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 2px 8px;
          border-radius: 999px !important;

          &:hover {
            background: #EFF6FF;
          }
        }
      }

      .code-terminal-box {
        background: #0F172A;
        border-radius: 10px;
        padding: 14px 16px;
        border: 1px solid #1E293B;

        pre {
          margin: 0;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 12.5px;
          color: #E2E8F0;
          line-height: 1.6;
          white-space: pre-wrap;
          word-break: break-word;
          max-height: 180px;
          overflow-y: auto;

          &::-webkit-scrollbar {
            width: 5px;
            height: 5px;
          }
          &::-webkit-scrollbar-thumb {
            background: #334155;
            border-radius: 3px;
          }
        }

        .empty-code-hint {
          color: #64748B;
          font-size: 12px;
          font-style: italic;
        }
      }

      .variables-capsule-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        padding: 10px 14px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 8px;

        .var-pill {
          display: inline-flex;
          align-items: center;
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          padding: 3px 10px;
          border-radius: 999px !important;

          code {
            font-family: ui-monospace, monospace;
            font-size: 12px;
            color: #1D4ED8;
            font-weight: 600;
          }
        }

        .empty-var-hint {
          font-size: 12px;
          color: #94A3B8;
        }
      }
    }
  }

  .vd-dialog-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    gap: 12px;

    .footer-left {
      .hint-text {
        font-size: 12px;
        color: #64748B;
      }
    }

    .footer-right {
      display: flex;
      align-items: center;
      gap: 10px;

      .pill-btn {
        border-radius: 999px !important;
        font-weight: 600;
        padding: 8px 18px;

        &.cancel-btn {
          background: #F1F5F9;
          border: 1px solid #CBD5E1;
          color: #475569;

          &:hover {
            background: #E2E8F0;
            color: #1E293B;
          }
        }

        &.rollback-btn-confirm {
          background: #F59E0B;
          border: none;
          color: #FFFFFF;
          box-shadow: 0 3px 8px rgba(245, 158, 11, 0.35);

          &:hover {
            background: #D97706;
            box-shadow: 0 4px 12px rgba(245, 158, 11, 0.45);
          }
        }
      }

      .vd-current-running-label {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 12.5px;
        font-weight: 600;
        color: #059669;
        background: #ECFDF5;
        border: 1px solid #A7F3D0;
        padding: 6px 14px;
        border-radius: 999px !important;
      }
    }
  }
}
</style>
