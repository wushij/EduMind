<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    class="tool-form"
  >
    <!-- 1. 基础信息 -->
    <div class="form-section">
      <div class="section-title">
        <span class="section-badge">1</span>
        <span class="section-name">基本信息</span>
        <span class="section-tip">定义工具在广场的唯一标识、标题与文本介绍</span>
      </div>

      <div class="form-row-2">
        <el-form-item v-if="!isEdit" label="工具唯一 ID" prop="id">
          <el-input
            v-model="form.id"
            placeholder="如 tool_custom_demo（小写字母、数字、下划线）"
            maxlength="64"
            show-word-limit
            class="custom-input"
          />
          <p class="field-hint">创建后不可更改，用作系统与广场的唯一路由标识</p>
        </el-form-item>

        <el-form-item v-else label="工具唯一 ID">
          <el-input v-model="form.id" disabled class="custom-input disabled-input" />
          <p class="field-hint">工具 ID 已锁定，不可修改</p>
        </el-form-item>

        <el-form-item label="工具名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="在工具广场与导航展示的名称，如：AI 智能出题"
            maxlength="128"
            class="custom-input"
          />
        </el-form-item>
      </div>

      <el-form-item label="短描述（广场卡片简介）" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="简述该 AI 工具的核心作用，将展示在卡片首页（建议 20-50 字）"
          maxlength="512"
          show-word-limit
          class="custom-textarea"
        />
      </el-form-item>

      <el-form-item label="详细介绍（详情抽屉完整介绍）">
        <el-input
          v-model="form.detailedIntro"
          type="textarea"
          :rows="4"
          placeholder="点击卡片后在抽屉或预告页展示的完整功能特性、使用技巧与典型教学场景"
          maxlength="1024"
          show-word-limit
          class="custom-textarea"
        />
      </el-form-item>
    </div>

    <!-- 2. 适用分类 -->
    <div class="form-section">
      <div class="section-title">
        <span class="section-badge">2</span>
        <span class="section-name">适用分类与角色权限</span>
        <span class="section-tip">决定工具在工具广场与权限体系中的展示分组</span>
      </div>

      <el-form-item prop="category">
        <div class="category-cards-grid">
          <div
            v-for="cat in categoryCardOptions"
            :key="cat.value"
            class="category-select-card"
            :class="{ 'is-active': form.category === cat.value }"
            @click="form.category = cat.value"
          >
            <div class="card-radio-ring">
              <div class="inner-dot" />
            </div>
            <div class="card-icon-wrap" :class="cat.themeClass">
              <el-icon :size="20"><component :is="cat.icon" /></el-icon>
            </div>
            <div class="card-text">
              <div class="card-title-row">
                <span class="card-title">{{ cat.label }}</span>
                <span class="card-code">{{ cat.value }}</span>
              </div>
              <p class="card-desc">{{ cat.desc }}</p>
            </div>
          </div>
        </div>
      </el-form-item>
    </div>

    <!-- 3. 运行与交互 -->
    <div class="form-section">
      <div class="section-title">
        <span class="section-badge">3</span>
        <span class="section-name">运行模式与智能模型</span>
        <span class="section-tip">配置点击工具时的跳转路由与绑定的后台大语言模型</span>
      </div>

      <div class="form-row-2">
        <el-form-item label="执行模式" prop="executionMode">
          <el-select v-model="form.executionMode" class="custom-select" style="width: 100%">
            <el-option
              v-for="opt in toolExecutionModeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="卡片图标">
          <el-select
            v-model="form.icon"
            filterable
            placeholder="选择 Element Plus 图标"
            class="custom-select"
            style="width: 100%"
          >
            <el-option
              v-for="iconName in toolIconOptions"
              :key="iconName"
              :label="iconName"
              :value="iconName"
            >
              <div class="icon-option-item">
                <el-icon :size="16"><component :is="iconComponents[iconName]" /></el-icon>
                <span>{{ iconName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </div>

      <div class="form-row-2">
        <el-form-item label="前端跳转路由" prop="route">
          <el-select
            v-model="form.route"
            filterable
            allow-create
            default-first-option
            placeholder="选择建议路由或直接输入系统路由"
            class="custom-select"
            style="width: 100%"
          >
            <el-option
              v-for="route in routeOptions"
              :key="route"
              :label="route"
              :value="route"
            />
          </el-select>
          <p v-if="form.executionMode === 'V05_NOTICE'" class="field-hint">
            V05 预告模式可留空，将自动引导至 /ai/marketplace/v05/{id}
          </p>
        </el-form-item>

        <el-form-item label="绑定的默认 AI 模型">
          <el-select
            v-model="form.modelId"
            clearable
            filterable
            placeholder="可选，绑定默认 Chat 模型"
            class="custom-select"
            style="width: 100%"
          >
            <el-option
              v-for="model in chatModels"
              :key="model.modelKey || model.name"
              :label="`${model.name} (${model.modelName})`"
              :value="model.modelKey || model.name"
            />
          </el-select>
          <p class="field-hint">调用该工具时优先接入的推理模型</p>
        </el-form-item>
      </div>
    </div>

    <!-- 4. 运营展示与发布 -->
    <div class="form-section">
      <div class="section-title">
        <span class="section-badge">4</span>
        <span class="section-name">运营展示与发布状态</span>
        <span class="section-tip">管理搜索关键字、热门与推荐标记、卡片排序及上下架状态</span>
      </div>

      <el-form-item label="搜索标签（以半角逗号分隔）">
        <el-input
          v-model="form.tags"
          placeholder="如：智能出题, 教师工具, 热门, 自动批改"
          maxlength="256"
          class="custom-input"
        />
      </el-form-item>

      <div class="form-row-3 ops-row">
        <div class="toggle-card">
          <div class="toggle-info">
            <span class="toggle-title">推荐置顶</span>
            <span class="toggle-desc">在广场“编辑精选”与推荐专区突出展示</span>
          </div>
          <el-switch v-model="form.isRecommended" />
        </div>

        <div class="toggle-card">
          <div class="toggle-info">
            <span class="toggle-title">热门徽章</span>
            <span class="toggle-desc">卡片标题旁点亮专属 🔥 HOT 热门徽章</span>
          </div>
          <el-switch v-model="form.isHot" />
        </div>

        <div class="sort-card">
          <div class="sort-info">
            <span class="sort-title">排序权重</span>
            <span class="sort-desc">数值越大排序越靠前 (0-9999)</span>
          </div>
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" class="custom-number" />
        </div>
      </div>

      <el-form-item v-if="showStatus" label="发布状态" class="status-form-item">
        <div class="status-select-grid">
          <div
            class="status-choice-card"
            :class="{ 'is-selected': form.status === 1 }"
            @click="form.status = 1"
          >
            <div class="status-dot online" />
            <div class="status-text">
              <strong>上架发布</strong>
              <span>在工具广场、课程中心和用户端立即展示可用</span>
            </div>
          </div>
          <div
            class="status-choice-card"
            :class="{ 'is-selected': form.status === 0 }"
            @click="form.status = 0"
          >
            <div class="status-dot offline" />
            <div class="status-text">
              <strong>下架维护</strong>
              <span>仅在系统后台可见，普通学生与教师不可直接查看</span>
            </div>
          </div>
        </div>
      </el-form-item>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import {
  EditPen,
  Document,
  CircleCheck,
  Notebook,
  DataAnalysis,
  ChatDotRound,
  Warning,
  Reading,
  Calendar,
  Monitor,
  Connection,
  Promotion,
  MagicStick,
  Tickets,
  Cpu,
  User,
  School,
  Compass
} from '@element-plus/icons-vue';
import { fetchEnabledChatModels } from '@/composables/system/useAIModel';
import type { AIModelConfigItem } from '@/types/system/model';
import {
  TOOL_EXECUTION_MODE_OPTIONS,
  TOOL_ICON_OPTIONS,
  TOOL_ROUTE_SUGGESTIONS,
  type AIToolSaveRequest
} from '@/types/system/tool';

const form = defineModel<Partial<AIToolSaveRequest>>({ required: true });

const props = withDefaults(defineProps<{
  isEdit?: boolean;
  showStatus?: boolean;
}>(), {
  isEdit: false,
  showStatus: true
});

const formRef = ref<FormInstance>();
const chatModels = ref<AIModelConfigItem[]>([]);
const toolIconOptions = [...TOOL_ICON_OPTIONS];
const toolExecutionModeOptions = [...TOOL_EXECUTION_MODE_OPTIONS];

const iconComponents: Record<string, any> = {
  EditPen,
  Document,
  CircleCheck,
  Notebook,
  DataAnalysis,
  ChatDotRound,
  Warning,
  Reading,
  Calendar,
  Monitor,
  Connection,
  Promotion,
  MagicStick,
  Tickets,
  Cpu
};

const categoryCardOptions = [
  {
    value: 'TEACHER',
    label: '教师工具',
    desc: '面向任课教师的智能教研、试卷生成、自动批改与学情分析中枢',
    icon: School,
    themeClass: 'theme-cyan'
  },
  {
    value: 'STUDENT',
    label: '学生工具',
    desc: '面向学习者的自适应刷题、个性化答疑、智能错题本与知识进阶',
    icon: User,
    themeClass: 'theme-orange'
  },
  {
    value: 'GENERAL',
    label: '通用工具',
    desc: '师生通用的智能问答、课程助教、知识图谱与全局通用能力',
    icon: Compass,
    themeClass: 'theme-blue'
  }
];

const routeOptions = computed((): string[] => {
  const routes = new Set<string>(TOOL_ROUTE_SUGGESTIONS as readonly string[]);
  if (form.value?.id) {
    routes.add(`/ai/marketplace/v05/${form.value.id}`);
  }
  if (form.value?.route) {
    routes.add(form.value.route);
  }
  return Array.from(routes);
});

const rules: FormRules = {
  id: [
    { required: true, message: '请输入工具 ID', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '仅支持小写字母、数字与下划线，以字母开头', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择适用分类', trigger: 'change' }],
  route: [{
    validator: (_rule, value, callback) => {
      if (form.value?.executionMode === 'V05_NOTICE') {
        callback();
        return;
      }
      if (!value) {
        callback(new Error('直接路由 (ROUTE) 模式下必须填写路由路径'));
        return;
      }
      callback();
    },
    trigger: 'blur'
  }]
};

async function loadModels() {
  try {
    chatModels.value = await fetchEnabledChatModels();
  } catch {
    chatModels.value = [];
  }
}

async function validate(): Promise<boolean> {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
}

onMounted(loadModels);

defineExpose({ validate });
</script>

<style scoped lang="scss">
.tool-form {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f1f5f9;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;

  .section-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 999px;
    background: linear-gradient(135deg, #2563eb, #3b82f6);
    color: #ffffff;
    font-size: 11px;
    font-weight: 700;
  }

  .section-name {
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
    letter-spacing: -0.2px;
  }

  .section-tip {
    font-size: 12px;
    color: #94a3b8;
  }
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* 适用分类 3 卡片选择器 */
.category-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  width: 100%;
}

.category-select-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: #93c5fd;
    background: #f8fafc;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.06);
  }

  &.is-active {
    border-color: #2563eb;
    background: #f0f7ff;
    box-shadow: 0 0 0 1px #2563eb, 0 6px 16px rgba(37, 99, 235, 0.12);

    .card-radio-ring {
      border-color: #2563eb;
      .inner-dot {
        background: #2563eb;
        transform: scale(1);
      }
    }
  }

  .card-radio-ring {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border: 2px solid #cbd5e1;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    .inner-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: transparent;
      transform: scale(0);
      transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
  }

  .card-icon-wrap {
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;

    &.theme-cyan {
      background: #ecfeff;
      color: #0891b2;
    }
    &.theme-orange {
      background: #fff7ed;
      color: #ea580c;
    }
    &.theme-blue {
      background: #eff6ff;
      color: #2563eb;
    }
  }

  .card-text {
    flex: 1;
    min-width: 0;
    padding-right: 18px;

    .card-title-row {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 4px;

      .card-title {
        font-size: 14px;
        font-weight: 700;
        color: #0f172a;
      }

      .card-code {
        font-size: 10px;
        color: #94a3b8;
        background: #f1f5f9;
        padding: 1px 6px;
        border-radius: 4px;
        font-family: monospace;
      }
    }

    .card-desc {
      margin: 0;
      font-size: 12px;
      line-height: 1.45;
      color: #64748b;
    }
  }
}

/* 下拉选项图标展示 */
.icon-option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

/* 运营卡片行 */
.ops-row {
  align-items: stretch;
}

.toggle-card,
.sort-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  gap: 12px;

  .toggle-info,
  .sort-info {
    display: flex;
    flex-direction: column;
    gap: 3px;

    .toggle-title,
    .sort-title {
      font-size: 13px;
      font-weight: 600;
      color: #1e293b;
    }

    .toggle-desc,
    .sort-desc {
      font-size: 11.5px;
      color: #94a3b8;
    }
  }
}

/* 发布状态选择 */
.status-select-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  width: 100%;
}

.status-choice-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #93c5fd;
    background: #f8fafc;
  }

  &.is-selected {
    border-color: #2563eb;
    background: #f0f7ff;
    box-shadow: 0 0 0 1px #2563eb;
  }

  .status-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    flex-shrink: 0;

    &.online {
      background: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
    }
    &.offline {
      background: #94a3b8;
      box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.2);
    }
  }

  .status-text {
    display: flex;
    flex-direction: column;
    gap: 2px;

    strong {
      font-size: 13px;
      font-weight: 600;
      color: #0f172a;
    }

    span {
      font-size: 11.5px;
      color: #64748b;
    }
  }
}

/* 统一输入框圆角与高定质感 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 0 0 1px #93c5fd inset;
  }

  &.is-focus,
  &:focus {
    box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 3px rgba(37, 99, 235, 0.12) !important;
  }
}

:deep(.disabled-input .el-input__wrapper) {
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  color: #94a3b8;
}

@media (max-width: 900px) {
  .category-cards-grid,
  .form-row-2,
  .form-row-3,
  .status-select-grid {
    grid-template-columns: 1fr;
  }
}
</style>
