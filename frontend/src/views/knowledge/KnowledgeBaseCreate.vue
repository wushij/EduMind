<template>
  <div class="kb-create-container">
    <!-- 顶部现代化 Hero 头部卡片（严格对标图 2 题库详情设计语言） -->
    <div class="kb-hero-card">
      <!-- 顶部导航与面包屑 -->
      <div class="header-nav-bar">
        <button type="button" class="back-btn" @click="router.push('/knowledge')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回知识库中心</span>
        </button>
        <el-divider direction="vertical" class="nav-divider" />
        <el-breadcrumb separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库中心</el-breadcrumb-item>
          <el-breadcrumb-item>创建知识库</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 知识库主体信息与快捷操作 -->
      <div class="header-main-section">
        <!-- 左侧：知识库形象、标题与元数据 -->
        <div class="header-info-col">
          <el-icon class="header-icon"><Collection /></el-icon>

          <div class="kb-meta-content">
            <div class="kb-title-line">
              <h1 class="kb-title">{{ formData.name || '创建课程/学科专业知识库' }}</h1>
              <span class="course-badge">
                <el-icon><FolderOpened /></el-icon>
                {{ selectedCourseName }}
              </span>
            </div>

            <p class="kb-description">
              {{ formData.description || '配置知识库的基本归属、语义分块策略与向量化嵌入模型，用于支持平台智能检索与 RAG 问答。' }}
            </p>

            <div class="kb-time-meta">
              <span class="time-item">
                <el-icon><Clock /></el-icon>
                配置就绪：支持多格式课件教材切片
              </span>
              <span class="meta-dot">·</span>
              <span class="status-item">
                <span class="status-indicator-dot" />
                向量化检索流水线准备就绪
              </span>
            </div>
          </div>
        </div>

        <!-- 右侧：分层现代操作按钮组 -->
        <div class="header-actions-col">
          <div class="actions-row actions-row--primary">
            <button
              type="button"
              class="action-pill action-pill--brand"
              :disabled="submitting"
              @click="handleSubmit"
            >
              <el-icon><Promotion /></el-icon>
              <span>{{ submitting ? '创建并初始化中...' : '确认创建并初始化' }}</span>
              <span class="ai-spark-chip">RAG 索引</span>
            </button>
          </div>

          <div class="actions-row actions-row--secondary">
            <button
              type="button"
              class="action-pill action-pill--ghost"
              @click="router.push('/knowledge')"
            >
              <el-icon><Close /></el-icon>
              <span>放弃并返回</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 底部 4 维微看板（1:1 模仿图 2 的指标卡设计） -->
      <div class="stats-micro-bar">
        <!-- 指标卡 1：目标切片大小 -->
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon-box">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">目标分块大小</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num">{{ formData.chunkSize }}</strong>
              <span class="stat-card__unit">Tokens</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>推荐 500-800，兼顾上下文与检索精度</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 2：嵌入向量模型 -->
        <div class="stat-card stat-card--emerald">
          <div class="stat-card__icon-box">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">嵌入向量模型</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num text-emerald-600">{{ embeddingModelBadge }}</strong>
            </div>
            <div class="stat-card__sub-hint">
              <span>高维语义向量基座 · 智能问答检索</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 3：切片拆分策略 -->
        <div class="stat-card stat-card--amber">
          <div class="stat-card__icon-box">
            <el-icon><Files /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">切片拆分策略</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num text-amber-600">{{ chunkStrategyBadge }}</strong>
            </div>
            <div class="stat-card__sub-hint">
              <span>相邻重叠 {{ formData.chunkOverlap }} Tokens 防止截断</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 4：知识库交付状态 -->
        <div class="stat-card stat-card--indigo">
          <div class="stat-card__icon-box">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">知识库交付状态</span>
            <div class="stat-card__value-row">
              <span class="ready-badge">就绪待建</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>创建后自动入库并启动向量切片解析</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 表单卡片区域 -->
    <div class="form-wrapper">
      <el-card shadow="never" class="main-card">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          class="kb-form"
        >
          <!-- 区域 1：基本信息 -->
          <div class="form-section-title">1. 知识库基础信息</div>
          <el-row :gutter="24">
            <el-col :span="16">
              <el-form-item label="知识库名称" prop="name">
                <el-input
                  v-model="formData.name"
                  placeholder="例如：数据结构与算法 408 核心知识库"
                  size="large"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="关联所属课程" prop="courseId">
                <el-select
                  v-model="formData.courseId"
                  placeholder="请选择课程"
                  size="large"
                  class="w-full"
                >
                  <el-option
                    v-for="c in courses"
                    :key="c.id"
                    :label="c.name || c.title || `课程 #${c.id}`"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="知识库说明与收录范围">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="简要说明本知识库收录的教材章节、讲义课件、题库解析或行业参考标准..."
            />
          </el-form-item>

          <!-- 区域 2：切片分块与 Embedding 向量化策略 -->
          <div class="form-section-title">2. 文档切片分块与语义向量化策略</div>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="Embedding 嵌入向量模型">
                <el-select v-model="formData.embeddingModel" size="default" class="w-full">
                  <el-option label="bge-large-zh-v1.5 (智源中文最优 · 1024维)" value="bge-large-zh-v1.5" />
                  <el-option label="text-embedding-3-large (OpenAI 高维语义 · 1536维)" value="text-embedding-3-large" />
                  <el-option label="m3e-base (通用中文基座 · 768维)" value="m3e-base" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="切片拆分策略">
                <el-select v-model="formData.chunkStrategy" size="default" class="w-full">
                  <el-option label="段落自适应切片 (按 Markdown 标题与空行分块)" value="PARAGRAPH" />
                  <el-option label="固定 Token 长度滑动窗口切片" value="FIXED_WINDOW" />
                  <el-option label="语义深度分块 (基于相似度断句)" value="SEMANTIC" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="单个分块目标大小 (Chunk Size)">
                <el-input-number
                  v-model="formData.chunkSize"
                  :min="100"
                  :max="2000"
                  :step="50"
                  class="w-full"
                />
                <span class="text-xs text-slate-500 mt-1 block">推荐 500-800 Tokens，兼顾上下文完整度与检索精准度</span>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="相邻分块重叠长度 (Chunk Overlap)">
                <el-input-number
                  v-model="formData.chunkOverlap"
                  :min="0"
                  :max="300"
                  :step="10"
                  class="w-full"
                />
                <span class="text-xs text-slate-500 mt-1 block">推荐 50-100 Tokens，防止跨切片关键信息被截断</span>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 区域 3：快捷上传初始课件材料 -->
          <div class="form-section-title">3. 初始教学文档与讲义上传（可选）</div>
          <el-upload
            drag
            action="#"
            multiple
            :auto-upload="false"
            :on-change="handleInitialUploadChange"
            :on-remove="handleInitialUploadRemove"
            accept=".pdf,.doc,.docx,.md,.markdown,.txt"
            class="kb-uploader"
          >
            <div class="upload-icon-wrap">
              <svg
                class="upload-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 19V5" />
                <path d="m5 12 7-7 7 7" />
              </svg>
            </div>
            <div class="el-upload__text">
              将讲义、教材 PDF、Word 或 Markdown 文档拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .pdf、.docx、.md、.txt 等格式，可多选上传，单文件不超过 50MB。创建后将自动入库并开始解析与向量化。
              </div>
            </template>
          </el-upload>

          <!-- 底部提交操作栏 -->
          <div class="form-actions-dock">
            <el-button size="large" @click="router.push('/knowledge')">取消</el-button>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              class="submit-btn"
              @click="handleSubmit"
            >
              <el-icon class="btn-icon"><Promotion /></el-icon>
              <span>确认创建并初始化知识库</span>
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  ArrowLeft,
  Collection,
  FolderOpened,
  Clock,
  Promotion,
  Close,
  Document,
  Cpu,
  Files,
  CircleCheck
} from '@element-plus/icons-vue';
import { useKnowledgeBaseCreate } from '@/composables/knowledge/useKnowledgeBase';

const {
  router,
  formRef,
  submitting,
  courses,
  formData,
  rules,
  handleInitialUploadChange,
  handleInitialUploadRemove,
  handleSubmit
} = useKnowledgeBaseCreate();

const selectedCourseName = computed(() => {
  if (!formData.courseId) return '未关联课程（通用知识库）';
  const found = courses.value.find((c: any) => String(c.id) === String(formData.courseId));
  return found ? (found.title || found.name || '已选课程') : '已选课程';
});

const embeddingModelBadge = computed(() => {
  const model = formData.embeddingModel || '';
  if (model.includes('bge-large')) return 'BGE-Large-zh';
  if (model.includes('text-embedding-3')) return 'OpenAI-Embedding-3';
  if (model.includes('m3e')) return 'M3E-Base';
  return model || '默认模型';
});

const chunkStrategyBadge = computed(() => {
  switch (formData.chunkStrategy) {
    case 'PARAGRAPH':
      return '段落自适应分块';
    case 'FIXED_WINDOW':
      return '固定窗口分块';
    case 'SEMANTIC':
      return '语义深度断句';
    default:
      return '自适应分块';
  }
});
</script>

<style scoped lang="scss">
.kb-create-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;

  /* 顶部现代化 Hero 头部卡片（1:1 对标图 2 设计规范） */
  .kb-hero-card {
    position: relative;
    overflow: hidden;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 24px rgba(30, 80, 160, 0.05);
    background: #ffffff;
    padding: 16px 28px 20px;

    .header-nav-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-bottom: 14px;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 18px;

      .back-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 0;
        border: none;
        background: transparent;
        font-size: 13px;
        font-weight: 600;
        color: #2563eb;
        cursor: pointer;
        white-space: nowrap;
        transition: color 0.15s ease;

        .el-icon {
          font-size: 14px;
        }

        &:hover {
          color: #1d4ed8;
        }
      }

      .nav-divider {
        margin: 0 4px;
        height: 14px;
        border-color: #e2e8f0;
      }

      .header-breadcrumb {
        flex: 1;

        :deep(.el-breadcrumb__inner) {
          font-size: 12.5px;
          color: #94a3b8;
        }

        :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
          color: #475569;
          font-weight: 600;
        }
      }
    }

    .header-main-section {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 28px;
      flex-wrap: wrap;

      @media (max-width: 1080px) {
        flex-direction: column;
      }
    }

    .header-info-col {
      display: flex;
      align-items: flex-start;
      gap: 18px;
      flex: 1;
      min-width: 320px;

      .header-icon {
        font-size: 34px;
        color: #2563eb;
        flex-shrink: 0;
        margin-top: 1px;
      }

      .kb-meta-content {
        display: flex;
        flex-direction: column;
        gap: 8px;
        min-width: 0;

        .kb-title-line {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;

          .kb-title {
            margin: 0;
            font-size: 22px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: -0.02em;
            line-height: 1.25;
          }

          .course-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 12px;
            border-radius: 9999px;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #2563eb;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;

            .el-icon {
              font-size: 13px;
            }
          }
        }

        .kb-description {
          margin: 0;
          font-size: 13.5px;
          color: #64748b;
          line-height: 1.6;
          max-width: 720px;
        }

        .kb-time-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12.5px;
          color: #94a3b8;

          .time-item {
            display: inline-flex;
            align-items: center;
            gap: 5px;

            .el-icon {
              font-size: 13px;
            }
          }

          .meta-dot {
            color: #cbd5e1;
          }

          .status-item {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            color: #059669;
            font-weight: 500;

            .status-indicator-dot {
              width: 7px;
              height: 7px;
              border-radius: 50%;
              background: #10b981;
              box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
            }
          }
        }
      }
    }

    .header-actions-col {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 10px;
      flex-shrink: 0;

      @media (max-width: 1080px) {
        align-items: flex-start;
        width: 100%;
      }

      .actions-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        &--secondary {
          justify-content: flex-end;
        }
      }

      .action-pill {
        display: inline-flex;
        align-items: center;
        gap: 7px;
        height: 38px;
        padding: 0 16px;
        border-radius: 9999px;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        outline: none;
        user-select: none;
        white-space: nowrap;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        .el-icon {
          font-size: 14px;
        }

        &--brand {
          background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
          color: #ffffff;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);

          &:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
            background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
          }

          &:disabled {
            opacity: 0.65;
            cursor: not-allowed;
          }

          .ai-spark-chip {
            padding: 1px 7px;
            border-radius: 9999px;
            background: rgba(255, 255, 255, 0.24);
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.02em;
          }
        }

        &--ghost {
          background: #ffffff;
          color: #64748b;
          border: 1.5px solid #e2e8f0;

          &:hover {
            border-color: #cbd5e1;
            color: #1e293b;
            background: #f8fafc;
          }
        }
      }
    }

    /* 底部 4 维微看板（1:1 图 2 风格） */
    .stats-micro-bar {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 14px;
      width: 100%;
      margin-top: 18px;
      padding-top: 18px;
      border-top: 1px solid rgba(226, 232, 240, 0.8);

      @media (max-width: 1100px) {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }

      .stat-card {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 12px 16px;
        border-radius: 14px;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
        }

        &__icon-box {
          width: 42px;
          height: 42px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          font-size: 20px;
        }

        &__content {
          display: flex;
          flex-direction: column;
          gap: 2px;
          min-width: 0;
        }

        &__label {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
        }

        &__value-row {
          display: flex;
          align-items: baseline;
          gap: 6px;
          line-height: 1.2;
        }

        &__num {
          font-size: 19px;
          font-weight: 800;
          color: #0f172a;
          letter-spacing: -0.02em;
        }

        &__unit {
          font-size: 12px;
          color: #94a3b8;
          font-weight: 600;
        }

        &__sub-hint {
          font-size: 11px;
          color: #94a3b8;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 200px;
        }

        .ready-badge {
          display: inline-block;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #ecfdf5;
          border: 1px solid #a7f3d0;
          color: #059669;
          font-size: 12px;
          font-weight: 700;
        }

        /* 颜色变体 */
        &--blue {
          .stat-card__icon-box {
            background: #eff6ff;
            color: #2563eb;
            border: 1px solid #dbeafe;
          }
        }

        &--emerald {
          .stat-card__icon-box {
            background: #ecfdf5;
            color: #059669;
            border: 1px solid #d1fae5;
          }
        }

        &--amber {
          .stat-card__icon-box {
            background: #fffbeb;
            color: #d97706;
            border: 1px solid #fef3c7;
          }
        }

        &--indigo {
          .stat-card__icon-box {
            background: #f5f3ff;
            color: #7c3aed;
            border: 1px solid #ede9fe;
          }
        }
      }
    }
  }

  .form-wrapper {
    width: 100%;

    .main-card {
      background: #ffffff;
      border-radius: 18px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
      padding: 6px 12px 12px;

      .kb-form {
        .form-section-title {
          font-size: 15px;
          font-weight: 700;
          color: #1e293b;
          border-left: 3px solid #2563eb;
          padding-left: 10px;
          margin: 24px 0 16px;

          &:first-child {
            margin-top: 8px;
          }
        }

        .kb-uploader {
          margin-bottom: 24px;

          :deep(.el-upload-dragger) {
            padding: 28px 20px;
            border-radius: 12px;
            border-color: #cbd5e1;
            background: #f8fafc;
            transition: all 0.2s ease;

            &:hover {
              border-color: #2563eb;
              background: #eff6ff;

              .upload-icon-wrap {
                background: #dbeafe;
                color: #1d4ed8;
                transform: translateY(-2px);
              }
            }
          }

          .upload-icon-wrap {
            width: 48px;
            height: 48px;
            margin: 0 auto 12px;
            border-radius: 50%;
            background: #e2e8f0;
            color: #475569;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s ease;

            .upload-svg {
              width: 24px;
              height: 24px;
              display: block;
            }
          }
        }

        .form-actions-dock {
          display: flex;
          justify-content: flex-end;
          gap: 16px;
          margin-top: 32px;
          padding-top: 20px;
          border-top: 1px solid #f1f5f9;

          .submit-btn {
            background: #2563eb;
            border-color: #2563eb;
            font-weight: 600;
            padding: 10px 28px;
          }
        }
      }
    }
  }
}
</style>
