<template>
  <div class="kb-create-container">
    <!-- 顶部导航 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/knowledge')">
        返回知识库中心
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库中心</el-breadcrumb-item>
        <el-breadcrumb-item>新建专业知识库</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="form-wrapper">
      <el-card shadow="never" class="main-card">
        <template #header>
          <div class="card-header-title">
            <el-icon class="header-icon"><Collection /></el-icon>
            <div>
              <h3>创建课程/学科专业知识库</h3>
              <p>配置知识库的基本归属、语义分块策略与向量化嵌入模型，用于支持平台智能检索与 RAG 问答。</p>
            </div>
          </div>
        </template>

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
                    :label="c.title"
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
            class="kb-uploader"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将讲义、教材 PDF、Word 或 Markdown 文档拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .pdf, .docx, .md, .txt 格式，单文件不超过 50MB。创建后系统将自动进行内容解析与向量化。
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
import { ArrowLeft, UploadFilled, Collection, Promotion } from '@element-plus/icons-vue';
import { useKnowledgeBaseCreate } from '@/composables/knowledge/useKnowledgeBase';

const { router, formRef, submitting, courses, formData, rules, handleSubmit } = useKnowledgeBaseCreate();
</script>

<style scoped lang="scss">
.kb-create-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;

  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 9999px;
    padding: 10px 24px;

    .back-link {
      font-size: 13.5px;
      font-weight: 500;
      color: #1677FF;
      padding: 0;
    }
  }

  .form-wrapper {
    width: 100%;

    .main-card {
      background: #ffffff;
      border-radius: 18px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
      padding: 8px 12px;

      .card-header-title {
        display: flex;
        align-items: center;
        gap: 16px;

        .header-icon {
          font-size: 32px;
          color: #2563EB;
          flex-shrink: 0;
        }

        h3 {
          margin: 0;
          font-size: 18px;
          font-weight: 700;
          color: #0f172a;
        }

        p {
          margin: 4px 0 0;
          font-size: 13px;
          color: #64748b;
        }
      }

      .kb-form {
        margin-top: 20px;

        .form-section-title {
          font-size: 15px;
          font-weight: 700;
          color: #1e293b;
          border-left: 3px solid #2563eb;
          padding-left: 10px;
          margin: 28px 0 16px;

          &:first-child {
            margin-top: 8px;
          }
        }

        .kb-uploader {
          margin-bottom: 24px;
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
