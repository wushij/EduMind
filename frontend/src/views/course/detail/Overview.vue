<template>
  <div class="course-overview-page">
    <div class="overview-grid">
      <!-- 左侧主体：课程介绍、目标与学习大纲引导 -->
      <div class="overview-main-col">
        <!-- 课程简介卡片 -->
        <div class="content-card">
          <div class="card-header-row">
            <el-icon class="card-icon card-icon--blue"><Reading /></el-icon>
            <h3 class="card-title">课程简介与修读要求</h3>
          </div>
          <p class="course-full-desc">
            {{ course?.description || '本课程紧密结合高等院校教学大纲，重点培养学生扎实的学科理论体系与工程实训能力。结合 EduMind AI 教学引擎，提供知识图谱精准画像、智能章节诊断与 7x24 小时随堂 AI 答疑。' }}
          </p>

          <div class="key-tags-row">
            <span class="pill-badge pill-badge--tag">重点必修课</span>
            <span class="pill-badge pill-badge--tag">配套 AI 智能题库</span>
            <span class="pill-badge pill-badge--tag">支持 RAG 课件问答</span>
            <span class="pill-badge pill-badge--tag">期末闭卷统一机考</span>
          </div>
        </div>

        <!-- 教学目标与能力矩阵 -->
        <div class="content-card">
          <div class="card-header-row">
            <el-icon class="card-icon card-icon--emerald"><Aim /></el-icon>
            <h3 class="card-title">教学目标与能力达成度</h3>
          </div>
          <div class="objectives-list">
            <div class="objective-item">
              <div class="obj-index">01</div>
              <div class="obj-text">
                <strong>理论基础掌握</strong>
                <p>系统理解核心概念、定理推导与数学物理背景，建立扎实的数理思维。</p>
              </div>
            </div>
            <div class="objective-item">
              <div class="obj-index">02</div>
              <div class="obj-text">
                <strong>问题抽象与求解</strong>
                <p>具备运用所学算法或数理工具，对复杂实际工程问题进行建模与分步求解。</p>
              </div>
            </div>
            <div class="objective-item">
              <div class="obj-index">03</div>
              <div class="obj-text">
                <strong>AI 赋能自主学习</strong>
                <p>善用课程 AI 助教进行代码审查、概念溯源与薄弱知识点自查巩固。</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 课程最新公告 -->
        <div class="content-card">
          <div class="card-header-row">
            <el-icon class="card-icon card-icon--amber"><Bell /></el-icon>
            <h3 class="card-title">课程通知与近期安排</h3>
          </div>
          <div class="notice-list">
            <div class="notice-item">
              <div class="notice-date">
                <span class="day">12</span>
                <span class="month">9月</span>
              </div>
              <div class="notice-body">
                <div class="notice-title">第 3 周随堂测验与 AI 出题自测已开放</div>
                <div class="notice-desc">请同学们于本周日前完成章节微测验，AI 批改系统将即时反馈知识点掌握情况。</div>
              </div>
            </div>
            <div class="notice-item">
              <div class="notice-date">
                <span class="day">08</span>
                <span class="month">9月</span>
              </div>
              <div class="notice-body">
                <div class="notice-title">课件资源已更新至第二章，知识图谱已完成挂载</div>
                <div class="notice-desc">可直接在【课程 AI 助教】中提问课件中涉及的概念和定理。</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧辅助：课程数据统计、主讲教师信息、快捷操作 -->
      <div class="overview-side-col">
        <!-- 核心统计指标卡片 -->
        <div class="content-card stats-summary-card">
          <h4 class="card-sub-header">课程空间数据概览</h4>
          <div class="stat-items-grid">
            <div class="stat-pill-box">
              <span class="stat-val">{{ course?.chapterCount || 8 }}</span>
              <span class="stat-label">教学大纲章节</span>
            </div>
            <div class="stat-pill-box">
              <span class="stat-val">{{ course?.knowledgePointCount || 46 }}</span>
              <span class="stat-label">核心知识点</span>
            </div>
            <div class="stat-pill-box">
              <span class="stat-val">{{ course?.studentCount || 128 }}</span>
              <span class="stat-label">在读选课学生</span>
            </div>
            <div class="stat-pill-box">
              <span class="stat-val">{{ course?.aiUsageCount || 340 }}</span>
              <span class="stat-label">AI 答疑次数</span>
            </div>
          </div>

          <div class="quick-cta-actions">
            <button
              type="button"
              class="capsule-block-btn capsule-block-btn--primary"
              @click="router.push(`/course/${course?.id || 101}/chapters`)"
            >
              <span>开始章节学习</span>
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button
              type="button"
              class="capsule-block-btn capsule-block-btn--ai"
              @click="router.push(`/course/${course?.id || 101}/ai`)"
            >
              <span>向课程 AI 助教提问</span>
              <el-icon><Service /></el-icon>
            </button>
          </div>
        </div>

        <!-- 主讲教师卡片 -->
        <div class="content-card teacher-card">
          <div class="teacher-header">
            <div class="teacher-large-avatar">
              {{ (course?.teacherName || '李').slice(0, 1) }}
            </div>
            <div class="teacher-title-meta">
              <h4 class="teacher-name">{{ course?.teacherName || '李华 教授' }}</h4>
              <span class="teacher-dept">计算机与人工智能学院</span>
            </div>
          </div>
          <p class="teacher-bio">
            从事高校教学与科研二十余年，主要研究领域为算法分析、机器学习理论与智能教育技术。曾主持多项国家级精品在线开放课程。
          </p>
          <div class="office-hour">
            <span class="oh-label">答疑时间：</span>
            <span>每周二、四 14:00 - 16:30</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Reading, Aim, Bell, ArrowRight, Service } from '@element-plus/icons-vue';
import { Course } from '@/types/course/course';

defineProps<{
  course?: Course | null;
}>();

const router = useRouter();
</script>

<style scoped lang="scss">
.course-overview-page {
  .overview-grid {
    display: grid;
    grid-template-columns: 1fr 340px;
    gap: 20px;
    align-items: start;
  }

  // 通用卡片容器 (16px 圆角 + 柔和科技阴影)
  .content-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    margin-bottom: 20px;

    .card-header-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;

      .card-icon {
        font-size: 20px;
        display: inline-flex;
        align-items: center;
        justify-content: center;

        &--blue { color: #1677FF; }
        &--emerald { color: #10B981; }
        &--amber { color: #F59E0B; }
      }

      .card-title {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #0F172A;
      }
    }

    .course-full-desc {
      font-size: 14.5px;
      line-height: 1.7;
      color: #334155;
      margin: 0 0 16px 0;
    }

    .key-tags-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .pill-badge--tag {
        display: inline-block;
        padding: 4px 12px;
        border-radius: 9999px; // 长圆跑道胶囊
        background: #F1F5F9;
        color: #475569;
        font-size: 12.5px;
        font-weight: 500;
      }
    }
  }

  // 教学目标
  .objectives-list {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .objective-item {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 14px 18px;
      border-radius: 14px;
      background: #F8FAFC;
      border: 1px solid #F1F5F9;

      .obj-index {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #1677FF;
        color: #FFFFFF;
        font-size: 13px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .obj-text {
        strong {
          font-size: 14px;
          color: #0F172A;
        }

        p {
          margin: 4px 0 0 0;
          font-size: 13px;
          color: #64748B;
          line-height: 1.5;
        }
      }
    }
  }

  // 最新公告
  .notice-list {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .notice-item {
      display: flex;
      align-items: center;
      gap: 18px;
      padding: 12px 0;
      border-bottom: 1px solid #F1F5F9;

      &:last-child {
        border-bottom: none;
      }

      .notice-date {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .day {
          font-size: 16px;
          font-weight: 700;
          line-height: 1;
        }

        .month {
          font-size: 10.5px;
          margin-top: 2px;
        }
      }

      .notice-body {
        .notice-title {
          font-size: 14px;
          font-weight: 600;
          color: #1E293B;
        }

        .notice-desc {
          font-size: 12.5px;
          color: #64748B;
          margin-top: 4px;
        }
      }
    }
  }

  // 右侧辅助卡片
  .stats-summary-card {
    .card-sub-header {
      margin: 0 0 16px 0;
      font-size: 15px;
      font-weight: 700;
      color: #0F172A;
    }

    .stat-items-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-bottom: 20px;

      .stat-pill-box {
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 14px;
        padding: 12px;
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;

        .stat-val {
          font-size: 20px;
          font-weight: 700;
          color: #1677FF;
        }

        .stat-label {
          font-size: 11.5px;
          color: #64748B;
          margin-top: 2px;
        }
      }
    }

    .quick-cta-actions {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .capsule-block-btn {
        width: 100%;
        height: 42px;
        border-radius: 9999px; // 长圆药丸
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 20px;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        transition: all 0.22s ease;

        &--primary {
          background: #1677FF;
          color: #FFFFFF;
          box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);

          &:hover {
            background: #4096FF;
            transform: translateY(-1px);
          }
        }

        &--ai {
          background: linear-gradient(135deg, #EEF2FF 0%, #FAF5FF 100%);
          border: 1px solid #C7D2FE;
          color: #4F46E5;

          &:hover {
            background: #E0E7FF;
            transform: translateY(-1px);
          }
        }
      }
    }
  }

  // 教师卡片
  .teacher-card {
    .teacher-header {
      display: flex;
      align-items: center;
      gap: 14px;
      margin-bottom: 14px;

      .teacher-large-avatar {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background: #1677FF;
        color: #FFFFFF;
        font-size: 20px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .teacher-title-meta {
        .teacher-name {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .teacher-dept {
          font-size: 12px;
          color: #64748B;
        }
      }
    }

    .teacher-bio {
      font-size: 13px;
      color: #475569;
      line-height: 1.6;
      margin: 0 0 14px 0;
    }

    .office-hour {
      font-size: 12px;
      color: #64748B;
      padding-top: 10px;
      border-top: 1px solid #F1F5F9;

      .oh-label {
        font-weight: 600;
        color: #334155;
      }
    }
  }
}

// 响应式
@media (max-width: 960px) {
  .course-overview-page .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
