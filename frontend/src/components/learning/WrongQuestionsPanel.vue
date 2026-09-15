<template>
  <div class="wrong-questions-page" v-loading="loading">
    <!-- 顶部 Hero 统计横幅 -->
    <PageHeroBanner
      title="智能错题本 · 认知归因与变式攻坚"
      subtitle="基于 AI 知识图谱与认知诊断模型，精准定位失分根本原因，一键生成同构变式题定向攻坚"
      background-variant="learning"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-danger">{{ totalWrongQuestions }}</span>
            <span class="stat-label">待攻坚错题</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ weakPointCount }}</span>
            <span class="stat-label">薄弱知识考点</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ masteredCount }}</span>
            <span class="stat-label">已攻克题量</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-primary">82%</span>
            <span class="stat-label">变式题攻克率</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 筛选与控制工具栏 -->
      <div class="filter-card">
        <div class="filter-left">
          <el-select
            v-model="teacherCourseId"
            placeholder="选择关联课程"
            class="course-select"
            @change="handleCourseChange"
          >
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>

          <el-radio-group v-model="selectedErrorType" @change="fetchList">
            <el-radio-button label="">全部错误类型</el-radio-button>
            <el-radio-button label="概念模糊">概念模糊</el-radio-button>
            <el-radio-button label="计算失误">计算失误</el-radio-button>
            <el-radio-button label="逻辑漏洞">逻辑漏洞</el-radio-button>
            <el-radio-button label="审题不清">审题不清</el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-right">
          <el-button type="primary" plain class="batch-practice-btn" @click="handleLaunchBatchPractice">
            <el-icon><Lightning /></el-icon>
            <span>一键发起错题变式攻坚练习</span>
          </el-button>
        </div>
      </div>

      <!-- 错题列表区域 -->
      <div v-if="wrongList.length > 0" class="questions-grid">
        <el-card
          v-for="item in wrongList"
          :key="item.id"
          class="question-item-card"
          shadow="hover"
        >
          <div class="card-top-bar">
            <div class="badge-group">
              <el-tag size="small" type="danger" effect="dark" class="error-count-tag">
                累计错误 {{ item.wrongCount }} 次
              </el-tag>
              <el-tag v-if="item.type" size="small" type="info" effect="plain">
                {{ formatQuestionType(item.type) }}
              </el-tag>
              <el-tag v-if="item.difficulty" size="small" :type="getDifficultyType(item.difficulty)" effect="plain">
                {{ item.difficulty }}
              </el-tag>
            </div>
            <div class="action-top">
              <span class="item-time">录入时间：{{ item.createTime || '近期作业' }}</span>
            </div>
          </div>

          <!-- 题干展示区 -->
          <div class="stem-container">
            <MathText :text="item.stem || '暂无题目内容'" />
          </div>

          <!-- 选项展示（若是单选题/多选题） -->
          <div v-if="parsedOptions(item.options).length > 0" class="options-container">
            <div
              v-for="opt in parsedOptions(item.options)"
              :key="opt.key"
              class="option-row"
              :class="{
                'is-answer': opt.key === item.answer,
                'is-wrong': opt.key === item.studentAnswer
              }"
            >
              <span class="option-key">{{ opt.key }}.</span>
              <span class="option-val">{{ opt.val }}</span>
              <span v-if="opt.key === item.answer" class="option-label answer-label">正确答案</span>
              <span v-else-if="opt.key === item.studentAnswer" class="option-label wrong-label">历史作答</span>
            </div>
          </div>

          <!-- 作答对比（非选择题或答案对照） -->
          <div v-if="!parsedOptions(item.options).length" class="answers-compare-row">
            <div class="ans-box wrong-box">
              <span class="ans-title">历史错误提交：</span>
              <span class="ans-text">{{ item.studentAnswer || '作答不完整或步骤中断' }}</span>
            </div>
            <div class="ans-box right-box">
              <span class="ans-title">标准参考答案：</span>
              <span class="ans-text">{{ item.answer || '详见完整解析' }}</span>
            </div>
          </div>

          <!-- AI 认知归因与诊断摘要条 -->
          <div class="ai-diagnosis-banner">
            <div class="ai-header">
              <div class="ai-tag">
                <el-icon><Cpu /></el-icon>
                <span>AI 认知归因诊断</span>
              </div>
              <div class="error-types-tags">
                <el-tag
                  v-for="err in item.errorTypes || ['概念混淆']"
                  :key="err"
                  size="small"
                  type="warning"
                  effect="plain"
                >
                  {{ err }}
                </el-tag>
              </div>
            </div>
            <p class="diagnosis-text">
              {{ item.diagnosis || '系统已捕获该知识点失分模式，点击下方按钮获取完整诊断与前驱依赖解析。' }}
            </p>
          </div>

          <!-- 卡片底部动作栏 -->
          <div class="card-footer-bar">
            <div class="footer-left">
              <el-button
                size="small"
                type="primary"
                text
                @click="openDiagnosisDrawer(item)"
              >
                <el-icon><Document /></el-icon>
                <span>查看深度归因与前驱知识</span>
              </el-button>
            </div>
            <div class="footer-right">
              <el-button
                size="small"
                type="success"
                plain
                @click="handleMarkMastered(item)"
              >
                <el-icon><Check /></el-icon>
                <span>标为已攻克</span>
              </el-button>
              <el-button
                size="small"
                type="primary"
                @click="handleStartVariantPractice(item)"
              >
                <el-icon><Aim /></el-icon>
                <span>练习同类变式题</span>
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-container">
        <el-empty
          description="太棒了！当前所选维度暂无错题记录，保持精熟状态！"
          :image-size="160"
        >
          <el-button type="primary" @click="handleGoToPractice">去自适应题库巩固</el-button>
        </el-empty>
      </div>

      <!-- 分页栏 -->
      <div v-if="totalWrongQuestions > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="totalWrongQuestions"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </div>

    <!-- AI 深度认知诊断与图谱依赖抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="错题深度认知归因与前驱依赖分析"
      size="560px"
      destroy-on-close
    >
      <div v-if="activeItem" class="drawer-content">
        <div class="drawer-section">
          <h4 class="section-title">
            <el-icon><Compass /></el-icon>
            错因根源诊断
          </h4>
          <div class="diagnosis-callout">
            <p class="summary-p">
              {{ activeItem.diagnosis || '正在生成 AI 认知根因诊断...' }}
            </p>
            <div class="tags-row">
              <el-tag
                v-for="err in activeItem.errorTypes || ['概念模糊']"
                :key="err"
                type="danger"
                effect="plain"
              >
                失分主因：{{ err }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="drawer-section">
          <h4 class="section-title">
            <el-icon><Connection /></el-icon>
            知识图谱前驱依赖溯源
          </h4>
          <div class="prereq-tree">
            <div class="tree-node parent-node">
              <span class="node-badge">前驱基础考点</span>
              <span class="node-title">函数的极限存在准则与无穷小阶数</span>
              <el-tag size="small" type="success">掌握度 88%</el-tag>
            </div>
            <div class="tree-link-line" />
            <div class="tree-node current-node">
              <span class="node-badge node-badge-danger">当前错题考点</span>
              <span class="node-title">导数定义式极限计算与洛必达法则条件</span>
              <el-tag size="small" type="danger">掌握度 42% (薄弱聚集)</el-tag>
            </div>
          </div>
        </div>

        <div class="drawer-section">
          <h4 class="section-title">
            <el-icon><Tickets /></el-icon>
            推荐变式攻坚题集
          </h4>
          <div class="variant-list">
            <div
              v-for="(varId, idx) in (activeItem.variantQuestionIds || [201, 202, 203])"
              :key="varId"
              class="variant-item-box"
            >
              <div class="variant-left">
                <span class="variant-idx">变式 #{{ idx + 1 }}</span>
                <span class="variant-desc">针对该考点的逆向变式训练题 (Q-{{ varId }})</span>
              </div>
              <el-button size="small" type="primary" plain @click="handlePracticeSingleVariant(varId)">
                立即自测
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleLaunchPracticeFromDrawer">
            开始这组变式题练习
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import {
  Lightning,
  Cpu,
  Document,
  Check,
  Aim,
  Compass,
  Connection,
  Tickets
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import MathText from '@/components/common/MathText.vue';
import { useWrongQuestionsPage } from '@/composables/learning/useWrongQuestionsPage';

const {
  courseOptions,
  teacherCourseId,
  loading,
  page,
  pageSize,
  totalWrongQuestions,
  weakPointCount,
  masteredCount,
  selectedErrorType,
  wrongList,
  drawerVisible,
  activeItem,
  fetchList,
  formatQuestionType,
  getDifficultyType,
  parsedOptions,
  handleCourseChange,
  openDiagnosisDrawer,
  handleMarkMastered,
  handleStartVariantPractice,
  handleLaunchBatchPractice,
  handlePracticeSingleVariant,
  handleLaunchPracticeFromDrawer,
  handleGoToPractice
} = useWrongQuestionsPage(102);
</script>

<style scoped lang="scss">
.wrong-questions-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;

  .hero-stats-row {
    display: flex;
    gap: 14px;
    margin-top: 4px;
    flex-wrap: wrap;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(8px);
      padding: 6px 20px;
      border-radius: 9999px; // 长圆胶囊形态
      border: 1.5px solid rgba(22, 119, 255, 0.14);
      display: flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.05);
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #1677FF;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.12);
      }

      .stat-num {
        font-size: 19px;
        font-weight: 800;
        line-height: 1;

        &.text-danger { color: #F5222D; }
        &.text-warning { color: #FA8C16; }
        &.text-success { color: #52C41A; }
        &.text-primary { color: #1677FF; }
      }

      .stat-label {
        font-size: 12.5px;
        font-weight: 500;
        color: #475569;
        margin-top: 0;
        white-space: nowrap;
      }
    }
  }

  .main-content-layout {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .filter-card {
      background: #FFFFFF;
      border-radius: 14px;
      padding: 16px 20px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 2px 10px rgba(30, 80, 150, 0.04);
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      .filter-left {
        display: flex;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;

        .course-select {
          width: 220px;
        }
      }

      .batch-practice-btn {
        border-radius: 9999px;
        font-weight: 600;
      }
    }

    .questions-grid {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .question-item-card {
        border-radius: 16px;
        border: 1px solid #E2E8F0;
        transition: all 0.25s ease;

        &:hover {
          border-color: #BFDBFE;
          box-shadow: 0 8px 24px rgba(22, 119, 255, 0.08);
          transform: translateY(-2px);
        }

        .card-top-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 14px;

          .badge-group {
            display: flex;
            align-items: center;
            gap: 8px;

            .error-count-tag {
              font-weight: 600;
              border-radius: 9999px;
            }
          }

          .item-time {
            font-size: 12px;
            color: #94A3B8;
          }
        }

        .stem-container {
          font-size: 15px;
          line-height: 1.7;
          color: #1E293B;
          font-weight: 500;
          margin-bottom: 14px;
          padding: 12px 16px;
          background: #F8FAFC;
          border-radius: 10px;
          border-left: 4px solid #1677FF;
        }

        .options-container {
          display: flex;
          flex-direction: column;
          gap: 8px;
          margin-bottom: 14px;

          .option-row {
            display: flex;
            align-items: center;
            padding: 8px 14px;
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            font-size: 14px;
            color: #334155;
            transition: all 0.2s;

            .option-key {
              font-weight: 700;
              margin-right: 8px;
              color: #1677FF;
            }

            .option-val {
              flex: 1;
            }

            .option-label {
              font-size: 12px;
              padding: 2px 8px;
              border-radius: 9999px;
              margin-left: 12px;
              font-weight: 600;

              &.answer-label {
                background: #EAF8EE;
                color: #52C41A;
                border: 1px solid #B7EB8F;
              }

              &.wrong-label {
                background: #FFF1F0;
                color: #F5222D;
                border: 1px solid #FFA39E;
              }
            }

            &.is-answer {
              background: #F6FFED;
              border-color: #B7EB8F;
            }

            &.is-wrong {
              background: #FFF1F0;
              border-color: #FFA39E;
            }
          }
        }

        .answers-compare-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
          margin-bottom: 14px;

          @media (max-width: 768px) {
            grid-template-columns: 1fr;
          }

          .ans-box {
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13px;

            .ans-title {
              font-weight: 600;
              display: block;
              margin-bottom: 4px;
            }

            &.wrong-box {
              background: #FFF2F0;
              border: 1px solid #FFCCC7;
              color: #CF1322;
            }

            &.right-box {
              background: #F6FFED;
              border: 1px solid #D9F7BE;
              color: #389E0D;
            }
          }
        }

        .ai-diagnosis-banner {
          background: linear-gradient(135deg, #F0F5FF 0%, #F5F0FF 100%);
          border: 1px solid #D6E4FF;
          border-radius: 12px;
          padding: 12px 16px;
          margin-bottom: 14px;

          .ai-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 6px;

            .ai-tag {
              display: flex;
              align-items: center;
              gap: 6px;
              color: #1677FF;
              font-size: 13px;
              font-weight: 700;
            }

            .error-types-tags {
              display: flex;
              gap: 6px;
            }
          }

          .diagnosis-text {
            margin: 0;
            font-size: 13px;
            line-height: 1.6;
            color: #475569;
          }
        }

        .card-footer-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding-top: 10px;
          border-top: 1px solid #F1F5F9;

          .footer-right {
            display: flex;
            gap: 10px;
          }
        }
      }
    }

    .empty-container {
      background: #FFFFFF;
      border-radius: 16px;
      padding: 40px;
      text-align: center;
      border: 1px solid #E2E8F0;
    }

    .pagination-wrapper {
      display: flex;
      justify-content: flex-end;
      margin-top: 12px;
    }
  }

  .drawer-content {
    display: flex;
    flex-direction: column;
    gap: 24px;

    .drawer-section {
      .section-title {
        font-size: 15px;
        font-weight: 700;
        color: #0F172A;
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 12px 0;
      }

      .diagnosis-callout {
        background: #F8FAFC;
        border-radius: 10px;
        padding: 14px;
        border-left: 4px solid #F5222D;

        .summary-p {
          margin: 0 0 10px 0;
          font-size: 14px;
          line-height: 1.6;
          color: #334155;
        }

        .tags-row {
          display: flex;
          gap: 8px;
        }
      }

      .prereq-tree {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        padding-left: 10px;

        .tree-node {
          padding: 10px 14px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          gap: 10px;
          border: 1px solid #E2E8F0;
          width: 100%;

          .node-badge {
            font-size: 11px;
            padding: 2px 6px;
            border-radius: 4px;
            background: #E2E8F0;
            color: #475569;
            font-weight: 600;

            &.node-badge-danger {
              background: #FEE2E2;
              color: #DC2626;
            }
          }

          .node-title {
            font-size: 13px;
            font-weight: 600;
            color: #1E293B;
            flex: 1;
          }

          &.parent-node {
            background: #F0FDF4;
            border-color: #BBF7D0;
          }

          &.current-node {
            background: #FEF2F2;
            border-color: #FECACA;
          }
        }

        .tree-link-line {
          width: 2px;
          height: 20px;
          background: #CBD5E1;
          margin-left: 30px;
        }
      }

      .variant-list {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .variant-item-box {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 12px 14px;
          border-radius: 8px;
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          transition: all 0.2s;

          &:hover {
            border-color: #1677FF;
            background: #F0F7FF;
          }

          .variant-left {
            display: flex;
            flex-direction: column;
            gap: 2px;

            .variant-idx {
              font-size: 12px;
              color: #1677FF;
              font-weight: 700;
            }

            .variant-desc {
              font-size: 13px;
              color: #334155;
            }
          }
        }
      }
    }
  }

  .drawer-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>
