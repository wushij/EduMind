<template>
  <div class="course-create-form">
    <!-- 模块 1：课程基准档案 -->
    <div class="form-block-card">
      <div class="block-card-header">
        <div class="block-icon-circle blue-glow">
          <el-icon><Document /></el-icon>
        </div>
        <div class="block-title-box">
          <h3 class="block-title">1. 课程基准档案</h3>
          <p class="block-desc">设定课程基本标识、学期安排与学科归属分类</p>
        </div>
      </div>

      <div class="block-card-body">
        <el-form-item label="课程全称" prop="name" required>
          <div class="capsule-input-wrap">
            <el-input
              v-model="form.name"
              placeholder="例如：深度学习与大语言模型系统工程"
              size="large"
              clearable
            />
          </div>
        </el-form-item>

        <div class="form-row-two-cols">
          <el-form-item label="课程代码 / 选课邀请码" prop="code" required>
            <div class="code-input-group">
              <el-input v-model="form.code" placeholder="例如：AI2026-CS01" size="large" />
              <button
                type="button"
                class="capsule-mini-btn"
                title="随机生成唯一课程邀请码"
                @click="$emit('generate-code')"
              >
                <el-icon class="mini-btn-icon"><Refresh /></el-icon>
                <span>随机生成</span>
              </button>
            </div>
            <span class="field-hint-text">学生可在课程中心输入此代码一键自选加入班级</span>
          </el-form-item>

          <el-form-item label="开课学期" prop="semester" required>
            <el-select
              v-model="form.semester"
              placeholder="请选择开课学期"
              size="large"
              class="w-100 capsule-select"
            >
              <el-option label="2026年秋季学期" value="2026年秋季学期" />
              <el-option label="2027年春季学期" value="2027年春季学期" />
              <el-option label="2026年暑期实训专周" value="2026年暑期实训专周" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="学科专业分类">
          <div class="pill-category-group">
            <div
              v-for="cat in categoryPresets"
              :key="cat"
              class="pill-category-tag"
              :class="{ active: form.category === cat }"
              @click="form.category = cat"
            >
              {{ cat }}
            </div>
          </div>
        </el-form-item>

        <div class="form-row-two-cols">
          <el-form-item label="建议学分">
            <el-select
              v-model="form.credits"
              size="large"
              class="w-100 capsule-select"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入学分"
              @change="(val: any) => form.credits = Number(val) || 3.0"
            >
              <el-option label="1.0 学分 (通识微课)" :value="1.0" />
              <el-option label="1.5 学分 (实训实践)" :value="1.5" />
              <el-option label="2.0 学分 (基础选修)" :value="2.0" />
              <el-option label="2.5 学分 (拓展选修)" :value="2.5" />
              <el-option label="3.0 学分 (专业核心)" :value="3.0" />
              <el-option label="3.5 学分 (实践综合)" :value="3.5" />
              <el-option label="4.0 学分 (综合必修)" :value="4.0" />
              <el-option label="4.5 学分 (进阶核心)" :value="4.5" />
              <el-option label="5.0 学分 (重难点必修 / 基础大课)" :value="5.0" />
              <el-option label="6.0 学分 (卓越实训 / 综合大课)" :value="6.0" />
            </el-select>
          </el-form-item>

          <el-form-item label="规划总学时">
            <el-select
              v-model="form.plannedHours"
              size="large"
              class="w-100 capsule-select"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入学时"
              @change="(val: any) => form.plannedHours = Number(val) || 48"
            >
              <el-option label="16 学时 (短学期/微课)" :value="16" />
              <el-option label="24 学时 (集中强化课)" :value="24" />
              <el-option label="32 学时 (理论讲授为主)" :value="32" />
              <el-option label="48 学时 (理论+上机实验)" :value="48" />
              <el-option label="64 学时 (重点深度课程)" :value="64" />
              <el-option label="80 学时 (重难点深度大课)" :value="80" />
              <el-option label="96 学时 (学年/双学期大课)" :value="96" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="课程简介与教学目标" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="简要介绍课程背景、核心教学目标及重点涵盖的技术模块，AI 助教将以此为依据向选课学生提供背景引导..."
            class="refined-textarea"
          />
        </el-form-item>
      </div>
    </div>

    <slot name="chapter-editor" />

    <!-- 模块 3：专业知识库与 RAG 互联 -->
    <div class="form-block-card">
      <div class="block-card-header">
        <div class="block-icon-circle cyan-glow">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="block-title-box">
          <h3 class="block-title">3. 专业知识库与 RAG 向量互联</h3>
          <p class="block-desc">打通知识资产，使课程专属 AI 助教即刻具备权威文档引用与高精度向量检索能力</p>
        </div>
      </div>

      <div class="block-card-body">
        <div class="kb-connection-options">
          <div
            class="kb-option-card"
            :class="{ active: kbMode === 'existing' }"
            @click="kbMode = 'existing'"
          >
            <div class="option-radio-dot"></div>
            <div class="option-info">
              <span class="option-title">
                <el-icon class="option-inline-icon"><Link /></el-icon>
                <span>关联现有专业学科知识库</span>
              </span>
              <span class="option-desc">复用机构已建立的高质量切片索引与教学讲义图谱</span>
            </div>
          </div>

          <div class="kb-option-card" :class="{ active: kbMode === 'new' }" @click="kbMode = 'new'">
            <div class="option-radio-dot"></div>
            <div class="option-info">
              <span class="option-title">
                <el-icon class="option-inline-icon"><Lightning /></el-icon>
                <span>自动为本课程创建专属 RAG 知识库</span>
              </span>
              <span class="option-desc">后续在“课程资料库”上传的讲义/课件将自动入库切片，建立独立向量索引</span>
            </div>
          </div>
        </div>

        <div v-if="kbMode === 'existing'" class="existing-kb-picker-wrap">
          <label class="picker-label">选择目标知识库：</label>
          <el-select
            v-model="form.knowledgeBaseId"
            placeholder="请选择要关联的知识库"
            size="large"
            class="w-100 capsule-select"
          >
            <el-option
              v-for="kb in availableKnowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            >
              <div class="kb-option-item">
                <span class="kb-name">{{ kb.name }}</span>
                <span class="kb-badge">{{ kb.documentCount || 0 }} 份切片资料</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="rag-switch-box">
          <div class="rag-switch-info">
            <span class="rag-switch-title">开启教学资源自动向量化 (RAG Auto-Indexing)</span>
            <span class="rag-switch-desc">资料库上传的讲义自动触发 OCR 解析与 Embedding，供 AI 助教实时查阅</span>
          </div>
          <el-switch v-model="enableRagAutoIndex" active-color="#1677FF" />
        </div>
      </div>
    </div>

    <!-- 模块 4：课程专属 AI 助教挂载 -->
    <div class="form-block-card">
      <div class="block-card-header">
        <div class="block-icon-circle indigo-glow">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="block-title-box">
          <h3 class="block-title">4. 课程专属 AI 助教挂载</h3>
          <p class="block-desc">定制 AI 助教教学风格与人格，为全班学生提供 7×24 小时个性化伴学答疑</p>
        </div>
      </div>

      <div class="block-card-body">
        <div class="ai-persona-cards-grid">
          <div
            v-for="persona in aiPersonas"
            :key="persona.id"
            class="persona-card"
            :class="[persona.id, { active: form.aiPersona === persona.id }]"
            @click="$emit('select-persona', persona)"
          >
            <div class="persona-icon-box">
              <el-icon v-if="persona.id === 'socrates'"><Opportunity /></el-icon>
              <el-icon v-else-if="persona.id === 'academic'"><Reading /></el-icon>
              <el-icon v-else><Tools /></el-icon>
            </div>
            <div class="persona-text">
              <span class="persona-name">{{ persona.name }}</span>
              <span class="persona-style">{{ persona.desc }}</span>
            </div>
            <div v-if="form.aiPersona === persona.id" class="check-dot">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-form-item label="AI 助教开课欢迎问候语">
          <el-input v-model="form.welcomeMessage" type="textarea" :rows="2" class="refined-textarea" />
        </el-form-item>
      </div>
    </div>

    <!-- 模块 5：视觉主题与封面选择 -->
    <div class="form-block-card">
      <div class="block-card-header">
        <div class="block-icon-circle orange-glow">
          <el-icon><Picture /></el-icon>
        </div>
        <div class="block-title-box">
          <h3 class="block-title">5. 课程视觉主题与封面定制</h3>
          <p class="block-desc">支持预设高保真多重渐变艺术主题或自定义图片 URL</p>
        </div>
      </div>

      <div class="block-card-body">
        <div class="cover-selection-grid">
          <div
            v-for="preset in presetCovers"
            :key="preset.id"
            class="preset-cover-card"
            :class="[preset.gradientClass, { active: selectedCoverId === preset.id }]"
            @click="$emit('select-cover', preset)"
          >
            <div class="cover-pattern-overlay"></div>
            <span class="preset-name">{{ preset.name }}</span>
            <div v-if="selectedCoverId === preset.id" class="check-badge">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>

        <div class="custom-url-row">
          <span class="custom-url-label">或输入自定义封面网络图片 URL：</span>
          <el-input
            v-model="form.coverUrl"
            placeholder="https://images.unsplash.com/photo-..."
            size="default"
            clearable
            @input="$emit('clear-cover-selection')"
          />
        </div>
      </div>
    </div>

    <div class="form-actions-row">
      <button type="button" class="action-btn action-btn--cancel" @click="$emit('cancel')">
        取消并返回
      </button>
      <button
        type="button"
        class="action-btn action-btn--submit"
        :disabled="submitting"
        @click="$emit('submit')"
      >
        <el-icon v-if="!submitting"><Select /></el-icon>
        <span v-if="!submitting">立即创建并初始化全链路空间</span>
        <span v-else>正在全链路初始化空间与大纲...</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Document,
  Connection,
  Cpu,
  Picture,
  Check,
  Select,
  Refresh,
  Link,
  Lightning,
  Opportunity,
  Reading,
  Tools
} from '@element-plus/icons-vue';
import type {
  AI_PERSONAS,
  CATEGORY_PRESETS,
  CourseCreateFormState,
  PRESET_COVERS
} from '@/composables/course/useCourseCreate';

defineProps<{
  form: CourseCreateFormState;
  categoryPresets: typeof CATEGORY_PRESETS;
  availableKnowledgeBases: Array<{ id: number; name: string; documentCount?: number }>;
  aiPersonas: typeof AI_PERSONAS;
  presetCovers: typeof PRESET_COVERS;
  selectedCoverId: number;
  submitting: boolean;
}>();

const kbMode = defineModel<'new' | 'existing'>('kbMode', { required: true });
const enableRagAutoIndex = defineModel<boolean>('enableRagAutoIndex', { required: true });

defineEmits<{
  'generate-code': [];
  'select-persona': [persona: (typeof AI_PERSONAS)[number]];
  'select-cover': [preset: (typeof PRESET_COVERS)[number]];
  'clear-cover-selection': [];
  cancel: [];
  submit: [];
}>();
</script>

<style scoped lang="scss">
.course-create-form {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
  min-width: 0;

  .form-block-card {
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    padding: 24px 28px;
    margin-bottom: 20px;
    transition: box-shadow 0.22s;

    &:hover {
      box-shadow: 0 6px 24px rgba(30, 80, 150, 0.07);
    }

    .block-card-header {
      display: flex;
      align-items: center;
      gap: 14px;
      margin-bottom: 22px;
      padding-bottom: 16px;
      border-bottom: 1px solid #F1F5F9;

      .block-icon-circle {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;

        &.blue-glow { background: #EFF6FF; color: #1677FF; }
        &.cyan-glow { background: #ECFDF5; color: #0D9488; }
        &.indigo-glow { background: #EEF2FF; color: #4F46E5; }
        &.orange-glow { background: #FFFBEB; color: #D97706; }
      }

      .block-title-box {
        .block-title {
          margin: 0 0 3px 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .block-desc {
          margin: 0;
          font-size: 12.5px;
          color: #64748B;
        }
      }
    }

    .block-card-body {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }
  }

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: #1E293B;
    font-size: 13.5px;
    margin-bottom: 7px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    border-radius: 9999px !important;
    box-shadow: 0 0 0 1px #E2E8F0 inset !important;
    padding: 0 16px;
    height: 42px;
    transition: all 0.2s;

    &:hover {
      box-shadow: 0 0 0 1px #CBD5E1 inset !important;
    }

    &.is-focus {
      box-shadow: 0 0 0 2px #1677FF inset !important;
    }
  }

  .form-row-two-cols {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }

  .w-100 {
    width: 100%;
  }

  .code-input-group {
    display: flex;
    align-items: center;
    gap: 10px;

    .capsule-mini-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 40px;
      padding: 0 16px;
      border-radius: 9999px;
      border: 1px solid #DBEAFE;
      background: #EFF6FF;
      color: #1677FF;
      font-size: 12.5px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;

      .mini-btn-icon {
        font-size: 14px;
      }

      &:hover {
        background: #DBEAFE;
        transform: translateY(-1px);
      }
    }
  }

  .field-hint-text {
    font-size: 12px;
    color: #94A3B8;
    margin-top: 4px;
    display: block;
  }

  .pill-category-group {
    display: flex;
    flex-wrap: wrap;
    gap: 9px;

    .pill-category-tag {
      padding: 6px 15px;
      border-radius: 9999px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      color: #475569;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #F1F5F9;
        color: #1677FF;
        border-color: #BFDBFE;
      }

      &.active {
        background: #1677FF;
        border-color: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);
      }
    }
  }

  .refined-textarea {
    :deep(.el-textarea__inner) {
      border-radius: 16px !important;
      border: 1px solid #E2E8F0;
      padding: 12px 16px;
      font-size: 13.5px;
      color: #1E293B;
      transition: all 0.2s;

      &:hover {
        border-color: #CBD5E1;
      }

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }
    }
  }

  .kb-connection-options {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }

    .kb-option-card {
      padding: 16px;
      border-radius: 16px;
      border: 1.5px solid #E2E8F0;
      background: #FAFCFE;
      cursor: pointer;
      display: flex;
      align-items: flex-start;
      gap: 12px;
      transition: all 0.2s ease;

      .option-radio-dot {
        width: 18px;
        height: 18px;
        border-radius: 50%;
        border: 2px solid #CBD5E1;
        margin-top: 2px;
        flex-shrink: 0;
        transition: all 0.2s;
      }

      .option-info {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .option-title {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          font-size: 13.5px;
          font-weight: 700;
          color: #1E293B;

          .option-inline-icon {
            font-size: 15px;
            color: #1677FF;
          }
        }

        .option-desc {
          font-size: 12px;
          color: #64748B;
          line-height: 1.4;
        }
      }

      &:hover {
        border-color: #93C5FD;
        background: #F0F7FF;
      }

      &.active {
        border-color: #1677FF;
        background: #EFF6FF;

        .option-radio-dot {
          border-color: #1677FF;
          background: #1677FF;
          box-shadow: inset 0 0 0 3px #FFFFFF;
        }

        .option-title {
          color: #1677FF;
        }
      }
    }
  }

  .existing-kb-picker-wrap {
    margin-top: 10px;

    .picker-label {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      margin-bottom: 6px;
      display: block;
    }

    .kb-option-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;

      .kb-name {
        font-weight: 500;
      }

      .kb-badge {
        font-size: 11px;
        color: #64748B;
        background: #F1F5F9;
        padding: 1px 8px;
        border-radius: 9999px;
      }
    }
  }

  .rag-switch-box {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 18px;
    background: #F8FAFC;
    border-radius: 14px;
    border: 1px solid #EDF2F7;
    margin-top: 10px;

    .rag-switch-info {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .rag-switch-title {
        font-size: 13.5px;
        font-weight: 600;
        color: #1E293B;
      }

      .rag-switch-desc {
        font-size: 12px;
        color: #64748B;
      }
    }
  }

  .ai-persona-cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-bottom: 14px;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }

    .persona-card {
      position: relative;
      padding: 16px 14px;
      border-radius: 16px;
      border: 1.5px solid #E2E8F0;
      background: #FFFFFF;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition: all 0.2s;

      .persona-icon-box {
        width: 38px;
        height: 38px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 19px;
        background: #F1F5F9;
        color: #475569;
        transition: all 0.2s;
      }

      &.socrates .persona-icon-box {
        background: #FEF3C7;
        color: #D97706;
      }

      &.academic .persona-icon-box {
        background: #EEF2FF;
        color: #4F46E5;
      }

      &.engineer .persona-icon-box {
        background: #ECFDF5;
        color: #059669;
      }

      .persona-text {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .persona-name {
          font-size: 13.5px;
          font-weight: 700;
          color: #0F172A;
        }

        .persona-style {
          font-size: 11.5px;
          color: #64748B;
          line-height: 1.4;
        }
      }

      .check-dot {
        position: absolute;
        top: 10px;
        right: 10px;
        width: 18px;
        height: 18px;
        border-radius: 50%;
        background: #1677FF;
        color: #FFFFFF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
      }

      &:hover {
        border-color: #93C5FD;
        transform: translateY(-2px);
      }

      &.active {
        border-color: #1677FF;
        background: #F0F7FF;

        .persona-name {
          color: #1677FF;
        }
      }
    }
  }

  .cover-selection-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 12px;
    margin-bottom: 12px;

    @media (max-width: 900px) {
      grid-template-columns: repeat(3, 1fr);
    }

    .preset-cover-card {
      position: relative;
      height: 76px;
      border-radius: 14px;
      cursor: pointer;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #FFFFFF;
      font-size: 12.5px;
      font-weight: 700;
      transition: all 0.22s;

      .cover-pattern-overlay {
        position: absolute;
        inset: 0;
        background: radial-gradient(circle at 100% 0%, rgba(255, 255, 255, 0.25) 0%, transparent 60%);
      }

      &.grad-blue { background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 50%, #60A5FA 100%); }
      &.grad-purple { background: linear-gradient(135deg, #4C1D95 0%, #7C3AED 50%, #A78BFA 100%); }
      &.grad-cyan { background: linear-gradient(135deg, #064E3B 0%, #0D9488 50%, #2DD4BF 100%); }
      &.grad-indigo { background: linear-gradient(135deg, #1E1B4B 0%, #3730A3 50%, #6366F1 100%); }
      &.grad-amber { background: linear-gradient(135deg, #78350F 0%, #D97706 50%, #FBBF24 100%); }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.16);
      }

      &.active {
        box-shadow: 0 0 0 2px #FFFFFF, 0 0 0 4px #1677FF;
      }

      .check-badge {
        position: absolute;
        top: 6px;
        right: 6px;
        width: 18px;
        height: 18px;
        background: #FFFFFF;
        color: #1677FF;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
      }
    }
  }

  .custom-url-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .custom-url-label {
      font-size: 12.5px;
      color: #64748B;
      white-space: nowrap;
    }
  }

  .form-actions-row {
    margin-top: 10px;
    padding: 20px 28px;
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 14px;

    .action-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      height: 44px;
      padding: 0 28px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.22s ease;

      &--cancel {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        color: #64748B;

        &:hover {
          background: #F8FAFC;
          color: #1E293B;
          border-color: #CBD5E1;
        }
      }

      &--submit {
        background: #1677FF;
        border: none;
        color: #FFFFFF;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);

        &:hover:not(:disabled) {
          background: #4096FF;
          transform: translateY(-1px);
          box-shadow: 0 8px 22px rgba(22, 119, 255, 0.4);
        }

        &:disabled {
          opacity: 0.65;
          cursor: not-allowed;
        }
      }
    }
  }
}
</style>
