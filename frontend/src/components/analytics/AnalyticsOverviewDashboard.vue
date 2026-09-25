<template>
  <div class="analytics-overview-container" v-loading="loading">
    <!-- 1. 顶部科技毛玻璃 Hero 横幅 (像素级对齐个人 AI 消耗明细流光设计) -->
    <div class="analytics-page-hero">
      <div class="hero-bg-glow hero-bg-glow--blue" />
      <div class="hero-bg-glow hero-bg-glow--purple" />

      <div class="hero-content">
        <div class="hero-left">
          <h2 class="hero-title">教学与课程分析</h2>
          <p class="hero-subtitle">
            全链路整合课程学情、考点掌握度、作业测验与 AI 助教交互数据，辅助精准教学干预
          </p>
        </div>

        <div class="hero-right">
          <!-- 纯单层课程选择器（单一圆润边框，无外部嵌套，字体标准饱满） -->
          <el-select
            v-model="courseId"
            placeholder="选择授课课程"
            class="hero-course-select-pure"
            @change="loadOverviewData"
          >
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>

          <!-- 时间周期切换药丸 -->
          <div class="period-pills-wrap">
            <button
              v-for="p in ['近7天', '近30天', '近学期']"
              :key="p"
              type="button"
              class="period-pill-btn"
              :class="{ 'is-active': selectedTrendRange === p }"
              @click="handleQuickRangeChange(p)"
            >
              {{ p }}
            </button>
          </div>

          <!-- 自定义日期范围 -->
          <div class="date-picker-wrap">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="hero-range-picker"
              @change="handleDateRangePickerChange"
            />
          </div>

          <!-- 操作按钮坞 -->
          <div class="hero-action-buttons">
            <el-tooltip content="刷新最新学情统计" placement="top">
              <el-button
                circle
                class="btn-icon-action"
                :icon="Refresh"
                :loading="loading"
                @click="loadOverviewData"
              />
            </el-tooltip>
            <el-button
              class="btn-hero-action"
              :icon="Download"
              @click="handleExportReport"
            >
              导出周报
            </el-button>
            <el-button
              type="primary"
              class="btn-hero-action btn-hero-action--primary"
              :icon="AiSparkleIcon"
              @click="handleOpenAiAdvice"
            >
              AI 智能诊断
            </el-button>
          </div>
        </div>
      </div>

      <!-- 底部状态指示坞 (类 AI 消耗明细进度与健康度胶囊) -->
      <div class="hero-footer-dock">
        <div class="status-indicators">
          <div class="status-pill status-pill--primary">
            <span class="status-dot status-dot--active" />
            <span>选课学生: <strong>{{ studentCount }}</strong> 人</span>
          </div>
          <div class="status-pill status-pill--info">
            <span class="status-dot status-dot--info" />
            <span>人均学习时长: <strong>{{ avgStudyMinutes }}</strong> 分钟</span>
          </div>
          <div class="status-pill status-pill--time">
            <el-icon class="pill-icon"><Clock /></el-icon>
            <span>数据更新: {{ lastUpdatedTime }}</span>
          </div>
        </div>

        <div class="hero-metric-progress">
          <span class="progress-title">知识大纲推进完成度</span>
          <div class="progress-track">
            <div
              class="progress-fill"
              :style="{ width: syllabusCoverageText !== '--' ? syllabusCoverageText : '0%' }"
            />
          </div>
          <span class="progress-value">{{ syllabusCoverageText }}</span>
        </div>
      </div>
    </div>

    <!-- 2. 二级导航 Tabs (教学概览、课程分析、学生分析、知识点掌握、AI使用分析) -->
    <div class="analytics-tabs-bar">
      <div
        v-for="tab in analyticsTabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="handleTabClick(tab)"
      >
        <span class="tab-label">{{ tab.label }}</span>
        <span v-if="tab.value === 'course'" class="tab-badge">深度分析</span>
      </div>
    </div>

    <!-- 3. 4 大核心 KPI 指标卡片 (微光质感 + 环比趋势胶囊) -->
    <div class="kpi-cards-grid">
      <!-- KPI 1: 班级平均分 -->
      <div class="kpi-card kpi-card--blue">
        <div class="kpi-top">
          <div class="kpi-label-row">
            <span class="kpi-label">班级平均分</span>
            <el-tooltip content="依据当前周期内学生作业、测验与随堂互动的综合加权均分" placement="top">
              <el-icon class="kpi-tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="kpi-icon-badge kpi-icon-badge--blue">
            <el-icon><Trophy /></el-icon>
          </div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value-row">
            <span class="kpi-value">{{ avgScore }}</span>
            <span class="kpi-unit">分</span>
          </div>
          <div class="kpi-trend-pill trend-up">
            <el-icon><CaretTop /></el-icon>
            <span>基于已批改答卷的真实均分</span>
          </div>
        </div>
      </div>

      <!-- KPI 2: 知识点掌握率 -->
      <div class="kpi-card kpi-card--cyan">
        <div class="kpi-top">
          <div class="kpi-label-row">
            <span class="kpi-label">知识点掌握率</span>
            <el-tooltip content="班级全体学生在已学考点上的平均掌握度（基于做题正确率与认知深度综合评估）" placement="top">
              <el-icon class="kpi-tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="kpi-icon-badge kpi-icon-badge--cyan">
            <el-icon><Aim /></el-icon>
          </div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value-row">
            <span class="kpi-value">{{ masteryRate }}</span>
          </div>
          <div class="kpi-trend-pill trend-up">
            <el-icon><CaretTop /></el-icon>
            <span>课程知识点真实平均掌握度</span>
          </div>
        </div>
      </div>

      <!-- KPI 3: 学生参与度 -->
      <div class="kpi-card kpi-card--green">
        <div class="kpi-top">
          <div class="kpi-label-row">
            <span class="kpi-label">学生参与度</span>
            <el-tooltip content="周期内至少参与一次课件学习、作业提交或 AI 答疑的学生比例" placement="top">
              <el-icon class="kpi-tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="kpi-icon-badge kpi-icon-badge--green">
            <el-icon><UserFilled /></el-icon>
          </div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value-row">
            <span class="kpi-value">{{ participationRate }}</span>
          </div>
          <div class="kpi-trend-pill trend-steady">
            <el-icon><Check /></el-icon>
            <span>周期内活跃学生占比</span>
          </div>
        </div>
      </div>

      <!-- KPI 4: AI 调用次数 -->
      <div class="kpi-card kpi-card--purple">
        <div class="kpi-top">
          <div class="kpi-label-row">
            <span class="kpi-label">AI 助学调用次数</span>
            <el-tooltip content="学生在当前课程知识库中通过 AI 助教、解题推演与自适应测验发起的大模型调用总量" placement="top">
              <el-icon class="kpi-tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="kpi-icon-badge kpi-icon-badge--purple">
            <el-icon><Cpu /></el-icon>
          </div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value-row">
            <span class="kpi-value">{{ aiCallCount }}</span>
            <span class="kpi-unit">次</span>
          </div>
          <div class="kpi-trend-pill trend-purple">
            <el-icon><Lightning /></el-icon>
            <span>主导场景：{{ aiToolLegend[0]?.name || '暂无调用' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 视图 A：当处于「教学概览」时，展示 2x2 专业图表矩阵 -->
    <div v-show="currentTab === 'overview'" class="charts-matrix-grid">
      <!-- 图表 1：成绩趋势 (双折线：班级均分 vs 全校/年级均分 + 时间范围切换) -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3 class="chart-title">成绩演进与年级对照</h3>
            <span class="chart-sub-label">班级平均分与全校基准平滑对比曲线（纵轴按当期真实样本自适应，非 0-100 满分刻度）</span>
          </div>
          <div class="chart-actions">
            <div class="time-filter-pills">
              <button
                v-for="t in ['近7天', '近30天', '近学期']"
                :key="t"
                type="button"
                class="pill-btn"
                :class="{ active: selectedTrendRange === t }"
                @click="handleQuickRangeChange(t)"
              >
                {{ t }}
              </button>
            </div>
          </div>
        </div>
        <div ref="scoreTrendChartRef" class="chart-body" />
      </div>

      <!-- 图表 2：知识点掌握情况 (横向柱状图，实时联动课程真实考点) -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3 class="chart-title">课程核心考点掌握情况</h3>
            <span class="chart-sub-label">按知识点掌握度动态排序及健康分级</span>
          </div>
          <div class="chart-actions">
            <el-button
              link
              type="primary"
              size="small"
              @click="router.push('/analytics/mastery')"
            >
              查看全谱雷达
            </el-button>
          </div>
        </div>
        <div ref="knowledgeMasteryChartRef" class="chart-body" />
      </div>

      <!-- 图表 3：日学习活跃人次 (已升级为平滑科技发光折线图，杜绝生硬柱子与小数人次) -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3 class="chart-title">日学习活跃人次</h3>
            <span class="chart-sub-label">学生每日线上学习、作业提交与答疑互动热度演进曲线</span>
          </div>
          <div class="chart-actions">
            <el-tag size="small" type="primary" effect="plain" round class="chart-mode-tag">
              平滑热度折线
            </el-tag>
          </div>
        </div>
        <div ref="gradeDistributionChartRef" class="chart-body" />
      </div>

      <!-- 图表 4：AI 助教使用与提供商分布 (环形饼图 + 场景真实频次 + 模型底座胶囊) -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3 class="chart-title">AI 助教使用与提供商分布</h3>
            <span class="chart-sub-label">大模型底座与课程助教各场景真实调用占比</span>
          </div>
          <div class="chart-actions">
            <el-button
              link
              type="primary"
              size="small"
              @click="router.push('/analytics/ai-usage')"
            >
              AI 消耗明细
            </el-button>
          </div>
        </div>
        <div class="donut-chart-container">
          <div ref="aiToolUsageChartRef" class="donut-chart-canvas" />
          <div class="donut-legend-col">
            <div
              v-for="item in aiToolLegend"
              :key="item.name"
              class="donut-legend-card"
              @mouseenter="handleHighlightLegend(item.name)"
              @mouseleave="handleDownplayLegend(item.name)"
            >
              <div class="legend-card-header">
                <span class="legend-circle" :style="{ backgroundColor: item.color }" />
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-calls-tag">{{ item.calls }} 次</span>
                <span class="legend-ratio">{{ item.ratio }}%</span>
              </div>
              <div class="legend-track">
                <div class="legend-bar" :style="{ width: `${item.ratio}%`, backgroundColor: item.color }" />
              </div>
            </div>

            <!-- 大模型底座提供商分布胶囊条（与标题"AI助教使用与提供商分布"名副其实） -->
            <div v-if="aiProviderList.length > 0" class="donut-provider-bar">
              <span class="provider-bar-label">模型底座:</span>
              <div class="provider-badges-wrap">
                <span
                  v-for="p in aiProviderList"
                  :key="p.provider"
                  class="provider-pill-badge"
                >
                  {{ p.provider }} <strong class="badge-ratio">{{ p.ratio }}%</strong>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. 视图 B：当处于「课程分析」时，展示深度课程分析全景面板 (彻底解决“功能不全、假数据”问题) -->
    <div v-show="currentTab === 'course'" class="course-deep-analytics-panel">
      <!-- 顶部课程健康度看板 (5 维雷达图 + 综合健康分评级) -->
      <div class="course-health-section">
        <div class="health-summary-card">
          <div class="health-badge-row">
            <span class="health-tag">课程综合质效指数</span>
            <span class="health-level-pill" :class="healthLevelClass">
              {{ healthLevelText }}
            </span>
          </div>
          <div class="health-score-display">
            <span class="score-num">{{ courseHealth ? courseHealth.overallScore : '--' }}</span>
            <span class="score-total">/ 100 分</span>
          </div>
          <p class="health-desc">
            由课程真实数据加权得出：大纲考点覆盖度（有学习行为的章节占比）、作业提交达标率、师生互动热度（活跃学生占比）、测验及格率与 AI 助学渗透率。
          </p>
          <div class="health-kpi-sublist">
            <div class="sub-kpi-item">
              <span class="sub-kpi-name">考点大纲覆盖</span>
              <span class="sub-kpi-val">{{ courseHealth ? courseHealth.syllabusCoverage : '--' }}%</span>
            </div>
            <div class="sub-kpi-item">
              <span class="sub-kpi-name">作业达标率</span>
              <span class="sub-kpi-val">{{ courseHealth ? courseHealth.assignmentCompletion : '--' }}%</span>
            </div>
            <div class="sub-kpi-item">
              <span class="sub-kpi-name">测验及格率</span>
              <span class="sub-kpi-val">{{ courseHealth ? courseHealth.passRate : '--' }}%</span>
            </div>
          </div>
        </div>

        <div class="health-radar-card">
          <div class="card-header-simple">
            <h4>课程教学 5 维能力雷达</h4>
            <span class="sub-tip">大纲覆盖 · 作业 · 互动 · 及格 · AI 辅学</span>
          </div>
          <div ref="courseHealthRadarRef" class="radar-chart-body" />
        </div>
      </div>

      <!-- 章节推进通关漏斗表 (真实读取课程章节) -->
      <div class="chapter-funnel-card">
        <div class="card-header-row">
          <div class="header-left">
            <h3 class="panel-card-title">课程章节推进与学习覆盖</h3>
            <span class="panel-card-subtitle">基于学生真实学习行为，统计各章节的学习覆盖率、参与人数与人均学时</span>
          </div>
          <div class="header-right">
            <el-tag type="info" round effect="plain">共 {{ chapterProgressList.length }} 个核心章节</el-tag>
          </div>
        </div>

        <div class="chapter-table-wrapper">
          <el-table
            :data="chapterProgressList"
            style="width: 100%"
            stripe
            class="custom-chapter-table"
          >
            <el-table-column prop="sort" label="章节序号" width="90" align="center">
              <template #default="{ row }">
                <span class="chapter-sort-pill">第 {{ row.sort }} 章</span>
              </template>
            </el-table-column>
            <el-table-column prop="chapterTitle" label="章节大纲标题" min-width="220">
              <template #default="{ row }">
                <div class="chapter-title-cell">
                  <el-icon class="ch-icon"><FolderOpened /></el-icon>
                  <span class="ch-text">{{ row.chapterTitle }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="completionRate" label="学习覆盖率" width="200">
              <template #default="{ row }">
                <div class="progress-cell-wrap">
                  <el-progress
                    :percentage="row.completionRate"
                    :color="row.completionRate >= 80 ? '#1677FF' : '#F59E0B'"
                    :stroke-width="8"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="avgScore" label="章节掌握度" width="130" align="center">
              <template #default="{ row }">
                <span
                  v-if="row.avgScore !== null && row.avgScore !== undefined"
                  class="score-badge"
                  :class="row.avgScore >= 80 ? 'score-high' : 'score-mid'"
                >
                  {{ row.avgScore }}%
                </span>
                <span v-else class="score-badge score-mid">暂无数据</span>
              </template>
            </el-table-column>
            <el-table-column prop="studentCount" label="学习人数" width="110" align="center">
              <template #default="{ row }">
                <span class="student-count-pill">{{ row.studentCount }} 人</span>
              </template>
            </el-table-column>
            <el-table-column prop="avgStudyMinutes" label="人均学时" width="130" align="center">
              <template #default="{ row }">
                <span>{{ row.avgStudyMinutes }} 分钟</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 课程薄弱考点与教学风险预警排行榜 (支持一键靶向干预) -->
      <div class="weak-points-warning-card">
        <div class="card-header-row">
          <div class="header-left">
            <h3 class="panel-card-title">重点薄弱考点与教学预警清单</h3>
            <span class="panel-card-subtitle">基于班级做题错因归纳识别出的低掌握度考点，建议安排课堂定向讲评</span>
          </div>
          <div class="header-right">
            <el-button
              type="primary"
              size="small"
              round
              :icon="Promotion"
              :loading="interventionProposalLoading"
              @click="handleGenerateInterventionProposal"
            >
              生成靶向干预预案
            </el-button>
          </div>
        </div>

        <div class="weak-points-grid">
          <div
            v-for="(item, idx) in courseWeakPoints"
            :key="item.knowledgePointId"
            class="weak-point-item-card"
          >
            <div class="item-header">
              <span class="item-rank">#{{ idx + 1 }}</span>
              <span
                class="item-urgency-tag"
                :class="item.urgency === 'HIGH' ? 'urgency-high' : 'urgency-medium'"
              >
                {{ item.urgency === 'HIGH' ? '重点攻坚' : '需巩固' }}
              </span>
            </div>
            <h4 class="item-title">{{ item.title }}</h4>
            <div class="item-stat-row">
              <div class="stat-col">
                <span class="stat-label">全班平均掌握度</span>
                <span class="stat-num" :class="item.mastery < 65 ? 'stat-danger' : 'stat-warning'">
                  {{ item.mastery }}%
                </span>
              </div>
              <div class="stat-col">
                <span class="stat-label">错题累计频次</span>
                <span class="stat-num">{{ item.wrongCount }} 次</span>
              </div>
              <div class="stat-col">
                <span class="stat-label">受影响学生</span>
                <span class="stat-num">{{ item.affectedStudents }} 人</span>
              </div>
            </div>
            <div class="item-footer-action">
              <el-button
                link
                type="primary"
                size="small"
                @click="router.push('/analytics/wrong-questions')"
              >
                查看错因明细 →
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. AI 智教认知推演引擎弹窗 (与系统级其它 AI 引擎完全对齐：640px 弹窗 + 罗盘脉冲雷达 + 0.1s 秒表实时递增 + 流水线推进 + 随时中止) -->
    <el-dialog
      v-model="aiThinkingModalVisible"
      title="AI 智能教学质量评估与干预推演引擎"
      width="640px"
      class="ai-teaching-engine-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      :show-close="true"
      append-to-body
      @close="handleStopAiAdvice"
    >
      <AiCognitiveThinkingPanel
        :active="aiThinkingModalVisible"
        v-bind="AI_COGNITIVE_THINKING_PRESETS.analyticsTeachingAdvice"
        show-footer-actions
        abort-label="中止推演"
        @abort="handleStopAiAdvice"
      />
    </el-dialog>

    <!-- 7. AI 智能教学诊断决策抽屉 (推演完毕呈现完整评估、建议与薄弱干预清单) -->
    <el-drawer
      v-model="aiAdviceDrawerVisible"
      title="AI 教学质量评估与智能干预建议"
      size="580px"
      append-to-body
      class="ai-advice-drawer"
    >
      <div class="drawer-advice-content">
        <!-- 阶段 1：推演生成完毕后的诊断结果视图 -->
        <template v-if="teachingAdvice">
          <div class="advice-hero-banner">
            <div class="advice-hero-icon">
              <el-icon><AiSparkleIcon /></el-icon>
            </div>
            <div class="advice-hero-text">
              <div class="hero-title-row">
                <h4>智教大模型综合学情诊断</h4>
                <span v-if="aiExecutionDurationText" class="duration-badge">
                  耗时 {{ aiExecutionDurationText }} · 智能推演
                </span>
              </div>
              <p>已整合当前课程全量考题正误率、章节学习留存与群体薄弱考点画像</p>
            </div>
          </div>

          <div class="advice-body-wrap">
            <div class="advice-block">
              <h5 class="advice-block-title">
                <el-icon><Compass /></el-icon>
                <span>诊断评估摘要</span>
              </h5>
              <div class="advice-summary-box">
                {{ teachingAdvice.summary }}
              </div>
            </div>

            <div class="advice-block">
              <h5 class="advice-block-title">
                <el-icon><Finished /></el-icon>
                <span>推荐干预策略与行动清单</span>
              </h5>
              <div class="advice-actions-list">
                <div
                  v-for="(act, idx) in teachingAdvice.actions"
                  :key="idx"
                  class="action-item-row"
                >
                  <span class="action-num">{{ idx + 1 }}</span>
                  <span class="action-text">{{ act }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="advice-empty-box">
          <el-empty description="暂无诊断报告，点击立即开启 AI 推演" />
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer-row">
          <el-button @click="aiAdviceDrawerVisible = false">关闭</el-button>
          <el-button
            v-if="adviceLoading"
            type="danger"
            plain
            @click="handleStopAiAdvice"
          >
            中止推演
          </el-button>
          <el-button
            v-else
            type="primary"
            :loading="adviceLoading"
            @click="handleOpenAiAdvice"
          >
            {{ teachingAdvice ? '重新评估推演' : '开始智能推演' }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  Refresh,
  Download,
  Clock,
  InfoFilled,
  Trophy,
  Aim,
  UserFilled,
  Cpu,
  CaretTop,
  Check,
  Lightning,
  FolderOpened,
  Promotion,
  Compass,
  Finished
} from '@element-plus/icons-vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { useAnalyticsOverview } from '@/composables/analytics/useAnalyticsOverview';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const router = useRouter();

const {
  loading,
  courseOptions,
  courseId,
  avgScore,
  masteryRate,
  participationRate,
  aiCallCount,
  studentCount,
  avgStudyMinutes,
  lastUpdatedTime,
  syllabusCoverageText,
  dateRange,
  selectedTrendRange,
  analyticsTabs,
  currentTab,
  aiToolLegend,
  aiProviderList,
  chapterProgressList,
  courseHealth,
  courseWeakPoints,
  aiThinkingModalVisible,
  aiAdviceDrawerVisible,
  teachingAdvice,
  adviceLoading,
  aiElapsedTimeText,
  aiCurrentStep,
  aiThinkingSteps,
  aiExecutionDurationText,
  interventionProposalLoading,
  scoreTrendChartRef,
  knowledgeMasteryChartRef,
  gradeDistributionChartRef,
  aiToolUsageChartRef,
  courseHealthRadarRef,
  handleTabClick,
  handleQuickRangeChange,
  handleDateRangePickerChange,
  handleExportReport,
  handleGenerateInterventionProposal,
  handleOpenAiAdvice,
  handleStopAiAdvice,
  handleHighlightLegend,
  handleDownplayLegend,
  loadOverviewData
} = useAnalyticsOverview();

/** 质效等级配色：由后端真实 healthLevel 推导，避免任何情况都渲染成"良好" */
const healthLevelClass = computed(() => {
  const level = courseHealth.value?.healthLevel;
  if (level === 'EXCELLENT') return 'level-excellent';
  if (level === 'WARNING') return 'level-warning';
  return 'level-good';
});

const healthLevelText = computed(() => {
  const level = courseHealth.value?.healthLevel;
  if (level === 'EXCELLENT') return '极佳 · 稳健推进';
  if (level === 'WARNING') return '偏弱 · 需重点干预';
  return '良好 · 正常推进';
});

// 说明：此处原有 handleDispatchGroupPractice() 只弹一句"已生成 5 题靶向变式题组"的提示，
// 既不调用任何接口也不落库，属于假动作，已替换为 useAnalyticsOverview 中的真实实现
// handleGenerateInterventionProposal()（调用干预中心 AI 推演接口 + 跳转审核）。
</script>

<style scoped lang="scss">
.analytics-overview-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

// 1. 顶部科技毛玻璃 Hero 横幅 (像素级仿 AI 消耗明细 ProfilePageHero 设计)
.analytics-page-hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 55%, #f5f3ff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 26px 30px 22px;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);

  .hero-bg-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(60px);
    opacity: 0.35;

    &--blue {
      width: 220px;
      height: 220px;
      background: #bfdbfe;
      top: -80px;
      right: 120px;
    }

    &--purple {
      width: 160px;
      height: 160px;
      background: #ddd6fe;
      bottom: -60px;
      left: 40px;
    }
  }

  .hero-content {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 24px;
    flex-wrap: wrap;

    @media (max-width: 1280px) {
      flex-direction: column;
    }
  }

  .hero-left {
    min-width: 320px;
    flex: 1;

    .hero-title {
      margin: 0;
      font-size: 24px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: -0.02em;
      line-height: 1.25;
    }

    .hero-subtitle {
      margin: 8px 0 0;
      font-size: 13.5px;
      color: #64748b;
      line-height: 1.6;
      max-width: 600px;
    }
  }

  .hero-right {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: nowrap;
    white-space: nowrap;

    // 纯单层课程选择器：单一圆润边框，无外部嵌套，字体标准饱满
    .hero-course-select-pure {
      width: 220px;

      :deep(.el-input__wrapper) {
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.95);
        border: 1px solid #cbd5e1;
        box-shadow: none !important;
        height: 40px;
        padding: 0 16px;
        font-size: 14px;
        transition: all 0.2s ease;

        &.is-focus, &:hover {
          border-color: #1677ff;
        }

        .el-input__inner {
          font-size: 14px;
          color: #1e293b;
          font-weight: 500;
        }
      }
    }

    .period-pills-wrap {
      display: flex;
      background: rgba(241, 245, 249, 0.85);
      padding: 4px;
      height: 40px;
      box-sizing: border-box;
      border-radius: 9999px;
      border: 1px solid #cbd5e1;

      .period-pill-btn {
        padding: 6px 16px;
        font-size: 13.5px;
        font-weight: 600;
        color: #64748b;
        background: transparent;
        border: none;
        border-radius: 9999px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          color: #1677ff;
        }

        &.is-active {
          color: #1677ff;
          background: #ffffff;
          box-shadow: 0 2px 6px rgba(22, 119, 255, 0.14);
        }
      }
    }

    .date-picker-wrap {
      :deep(.el-range-editor.el-input__wrapper) {
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.95);
        border: 1px solid #cbd5e1;
        box-shadow: none !important;
        height: 40px;
        width: 260px;
        padding: 0 14px;
        font-size: 13.5px;

        &.is-active, &:hover {
          border-color: #1677ff;
        }

        .el-range-input {
          font-size: 13px;
          color: #334155;
        }

        .el-range-separator {
          font-size: 13px;
          color: #94a3b8;
          padding: 0 4px;
        }

        .el-range__icon {
          font-size: 15px;
          margin-right: 4px;
        }
      }
    }

    .hero-action-buttons {
      display: flex;
      align-items: center;
      gap: 10px;

      .btn-icon-action {
        width: 40px;
        height: 40px;
        font-size: 16px;
        border-color: #cbd5e1;
        background: #ffffff;
        color: #475569;

        &:hover {
          color: #1677ff;
          border-color: #1677ff;
        }
      }

      .btn-hero-action {
        border-radius: 9999px;
        height: 40px;
        padding: 0 18px;
        font-weight: 600;
        font-size: 14px;
        border-color: #cbd5e1;

        &--primary {
          background: #1677ff;
          border-color: #1677ff;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);

          &:hover {
            background: #4096ff;
            border-color: #4096ff;
          }
        }
      }
    }
  }

  // 底部状态指示坞
  .hero-footer-dock {
    position: relative;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid rgba(226, 232, 240, 0.85);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;

    .status-indicators {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        padding: 4px 12px;
        border-radius: 9999px;
        font-weight: 500;

        &--primary {
          background: rgba(22, 119, 255, 0.08);
          color: #1677ff;

          .status-dot--active {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #1677ff;
            box-shadow: 0 0 8px #1677ff;
          }
        }

        &--info {
          background: #f1f5f9;
          color: #475569;

          .status-dot--info {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #0ea5e9;
          }
        }

        &--time {
          background: transparent;
          color: #94a3b8;

          .pill-icon {
            font-size: 13px;
          }
        }
      }
    }

    .hero-metric-progress {
      display: flex;
      align-items: center;
      gap: 10px;

      .progress-title {
        font-size: 12px;
        font-weight: 600;
        color: #64748b;
      }

      .progress-track {
        width: 140px;
        height: 7px;
        background: #e2e8f0;
        border-radius: 9999px;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #38bdf8, #1677ff);
          border-radius: 9999px;
          transition: width 0.4s ease;
        }
      }

      .progress-value {
        font-size: 12.5px;
        font-weight: 700;
        color: #1677ff;
      }
    }
  }
}

// 2. 二级导航 Tabs
.analytics-tabs-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 2px;

  .tab-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    font-size: 14px;
    font-weight: 600;
    color: #64748b;
    cursor: pointer;
    border-radius: 10px 10px 0 0;
    transition: all 0.2s ease;
    position: relative;

    &:hover {
      color: #1677ff;
      background: rgba(241, 245, 249, 0.6);
    }

    &.active {
      color: #1677ff;
      font-weight: 700;

      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        right: 0;
        height: 3px;
        background: #1677ff;
        border-radius: 2px;
      }
    }

    .tab-badge {
      font-size: 10.5px;
      padding: 1px 7px;
      border-radius: 9999px;
      background: rgba(22, 119, 255, 0.1);
      color: #1677ff;
      font-weight: 700;
    }
  }
}

// 3. 4 大核心 KPI 指标卡片 (微光质感 + 渐变边框)
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .kpi-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 20px 22px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    transition: all 0.25s ease;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 10px 24px rgba(22, 119, 255, 0.08);
      border-color: #cbd5e1;
    }

    &--blue:hover { border-color: #93c5fd; }
    &--cyan:hover { border-color: #a5f3fc; }
    &--green:hover { border-color: #a7f3d0; }
    &--purple:hover { border-color: #ddd6fe; }

    .kpi-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .kpi-label-row {
        display: inline-flex;
        align-items: center;
        gap: 6px;

        .kpi-label {
          font-size: 13.5px;
          font-weight: 600;
          color: #475569;
        }

        .kpi-tip-icon {
          font-size: 13px;
          color: #94a3b8;
          cursor: pointer;

          &:hover {
            color: #1677ff;
          }
        }
      }

      .kpi-icon-badge {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;

        &--blue { background: rgba(22, 119, 255, 0.1); color: #1677ff; }
        &--cyan { background: rgba(6, 182, 212, 0.1); color: #0891b2; }
        &--green { background: rgba(16, 185, 129, 0.1); color: #059669; }
        &--purple { background: rgba(139, 92, 246, 0.1); color: #7c3aed; }
      }
    }

    .kpi-body {
      .kpi-value-row {
        display: flex;
        align-items: baseline;
        gap: 4px;
        margin-bottom: 8px;

        .kpi-value {
          font-size: 32px;
          font-weight: 800;
          color: #0f172a;
          letter-spacing: -0.02em;
          line-height: 1;
        }

        .kpi-unit {
          font-size: 14px;
          font-weight: 600;
          color: #64748b;
        }
      }

      .kpi-trend-pill {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11.5px;
        font-weight: 600;
        padding: 3px 8px;
        border-radius: 6px;

        &.trend-up {
          background: rgba(16, 185, 129, 0.1);
          color: #059669;
        }

        &.trend-steady {
          background: #f1f5f9;
          color: #475569;
        }

        &.trend-purple {
          background: rgba(139, 92, 246, 0.1);
          color: #7c3aed;
        }
      }
    }
  }
}

// 4. 2x2 专业图表矩阵
.charts-matrix-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }

  .chart-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 22px 24px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
      gap: 12px;
      flex-wrap: wrap;

      .chart-title-group {
        .chart-title {
          margin: 0;
          font-size: 16.5px;
          font-weight: 700;
          color: #0f172a;
        }

        .chart-sub-label {
          display: block;
          margin-top: 3px;
          font-size: 12px;
          color: #64748b;
        }
      }

      .time-filter-pills {
        display: flex;
        background: #f1f5f9;
        padding: 2px;
        border-radius: 9999px;

        .pill-btn {
          padding: 3px 10px;
          font-size: 11.5px;
          font-weight: 600;
          color: #64748b;
          border: none;
          background: transparent;
          border-radius: 9999px;
          cursor: pointer;

          &.active {
            background: #1677ff;
            color: #ffffff;
          }
        }
      }
    }

    .chart-body {
      width: 100%;
      height: 290px;
    }

    // 环形甜甜圈图容器
    .donut-chart-container {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 290px;
      gap: 16px;

      .donut-chart-canvas {
        flex: 1;
        height: 100%;
      }

      .donut-legend-col {
        width: 220px;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .donut-legend-card {
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 10px;
          padding: 8px 12px;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            background: #ffffff;
            border-color: #cbd5e1;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
            transform: translateX(-2px);
          }

          .legend-card-header {
            display: flex;
            align-items: center;
            gap: 8px;

            .legend-circle {
              width: 8px;
              height: 8px;
              border-radius: 50%;
            }

            .legend-name {
              flex: 1;
              font-size: 12.5px;
              font-weight: 600;
              color: #334155;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .legend-calls-tag {
              font-size: 11px;
              color: #64748b;
              background: #f1f5f9;
              padding: 1px 6px;
              border-radius: 4px;
              font-weight: 500;
            }

            .legend-ratio {
              font-size: 12.5px;
              font-weight: 700;
              color: #0f172a;
              min-width: 38px;
              text-align: right;
            }
          }

          .legend-track {
            height: 4px;
            border-radius: 999px;
            background: #e2e8f0;
            margin-top: 6px;
            overflow: hidden;

            .legend-bar {
              height: 100%;
              border-radius: 999px;
              transition: width 0.4s ease;
            }
          }
        }

        // 模型底座提供商胶囊栏
        .donut-provider-bar {
          margin-top: 4px;
          padding: 8px 10px;
          background: rgba(248, 250, 252, 0.9);
          border: 1px dashed #e2e8f0;
          border-radius: 8px;
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .provider-bar-label {
            font-size: 11px;
            font-weight: 600;
            color: #64748b;
          }

          .provider-badges-wrap {
            display: flex;
            align-items: center;
            gap: 6px;
            flex-wrap: wrap;

            .provider-pill-badge {
              font-size: 11px;
              color: #1e293b;
              background: #ffffff;
              border: 1px solid #cbd5e1;
              padding: 2px 7px;
              border-radius: 9999px;
              display: inline-flex;
              align-items: center;
              gap: 3px;

              .badge-ratio {
                color: #2563eb;
                font-weight: 700;
              }
            }
          }
        }
      }
    }
  }
}

// 5. 视图 B：课程深度分析全景面板
.course-deep-analytics-panel {
  display: flex;
  flex-direction: column;
  gap: 22px;

  .course-health-section {
    display: grid;
    grid-template-columns: 42% 58%;
    gap: 18px;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    .health-summary-card {
      background: linear-gradient(145deg, #ffffff 0%, #f0f7ff 100%);
      border: 1px solid #bfdbfe;
      border-radius: 20px;
      padding: 24px 26px;
      box-shadow: 0 4px 18px rgba(22, 119, 255, 0.06);
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .health-badge-row {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .health-tag {
          font-size: 13px;
          font-weight: 700;
          color: #1e40af;
        }

        .health-level-pill {
          font-size: 11.5px;
          font-weight: 700;
          padding: 3px 10px;
          border-radius: 9999px;

          &.level-excellent {
            background: #dcfce7;
            color: #15803d;
          }

          &.level-good {
            background: #e0f2fe;
            color: #0369a1;
          }

          &.level-warning {
            background: #fef3c7;
            color: #b45309;
          }
        }
      }

      .health-score-display {
        margin: 16px 0 8px;
        display: flex;
        align-items: baseline;
        gap: 6px;

        .score-num {
          font-size: 48px;
          font-weight: 900;
          color: #0f172a;
          letter-spacing: -0.03em;
          line-height: 1;
        }

        .score-total {
          font-size: 16px;
          color: #64748b;
          font-weight: 600;
        }
      }

      .health-desc {
        font-size: 13px;
        color: #475569;
        line-height: 1.6;
        margin: 0 0 16px;
      }

      .health-kpi-sublist {
        display: flex;
        justify-content: space-between;
        padding-top: 14px;
        border-top: 1px solid rgba(226, 232, 240, 0.8);

        .sub-kpi-item {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .sub-kpi-name {
            font-size: 11.5px;
            color: #64748b;
          }

          .sub-kpi-val {
            font-size: 16px;
            font-weight: 800;
            color: #1677ff;
          }
        }
      }
    }

    .health-radar-card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 20px;
      padding: 20px 24px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);

      .card-header-simple {
        margin-bottom: 8px;

        h4 {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
        }

        .sub-tip {
          font-size: 12px;
          color: #64748b;
        }
      }

      .radar-chart-body {
        width: 100%;
        height: 250px;
      }
    }
  }

  // 章节通关进度漏斗
  .chapter-funnel-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 22px 26px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);

    .card-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;

      .panel-card-title {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
      }

      .panel-card-subtitle {
        display: block;
        margin-top: 3px;
        font-size: 12.5px;
        color: #64748b;
      }
    }

    .chapter-table-wrapper {
      .custom-chapter-table {
        border-radius: 12px;
        overflow: hidden;

        .chapter-sort-pill {
          font-size: 12px;
          font-weight: 600;
          color: #475569;
          background: #f1f5f9;
          padding: 2px 8px;
          border-radius: 6px;
        }

        .chapter-title-cell {
          display: inline-flex;
          align-items: center;
          gap: 8px;

          .ch-icon {
            color: #1677ff;
            font-size: 16px;
          }

          .ch-text {
            font-weight: 600;
            color: #1e293b;
          }
        }

        .progress-cell-wrap {
          padding-right: 12px;
        }

        .score-badge {
          font-weight: 700;
          font-size: 13px;

          &.score-high { color: #1677ff; }
          &.score-mid { color: #f59e0b; }
        }

        .student-count-pill {
          color: #475569;
          font-weight: 500;
        }
      }
    }
  }

  // 重点薄弱考点预警
  .weak-points-warning-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 22px 26px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);

    .card-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;

      .panel-card-title {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
      }

      .panel-card-subtitle {
        display: block;
        margin-top: 3px;
        font-size: 12.5px;
        color: #64748b;
      }
    }

    .weak-points-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;

      @media (max-width: 1024px) {
        grid-template-columns: 1fr;
      }

      .weak-point-item-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 14px;
        padding: 16px 18px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        gap: 12px;

        .item-header {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .item-rank {
            font-size: 13px;
            font-weight: 800;
            color: #94a3b8;
          }

          .item-urgency-tag {
            font-size: 11px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 4px;

            &.urgency-high {
              background: #fee2e2;
              color: #dc2626;
            }

            &.urgency-medium {
              background: #fef3c7;
              color: #d97706;
            }
          }
        }

        .item-title {
          margin: 0;
          font-size: 14.5px;
          font-weight: 700;
          color: #0f172a;
          line-height: 1.4;
        }

        .item-stat-row {
          display: flex;
          justify-content: space-between;
          padding: 8px 0;
          border-top: 1px dashed #e2e8f0;
          border-bottom: 1px dashed #e2e8f0;

          .stat-col {
            display: flex;
            flex-direction: column;
            gap: 2px;

            .stat-label {
              font-size: 11px;
              color: #64748b;
            }

            .stat-num {
              font-size: 14px;
              font-weight: 700;
              color: #1e293b;

              &.stat-danger { color: #dc2626; }
              &.stat-warning { color: #d97706; }
            }
          }
        }

        .item-footer-action {
          display: flex;
          justify-content: flex-end;
        }
      }
    }
  }
}

// 6. AI 智能教学诊断抽屉
.drawer-advice-content {
  padding: 10px 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  .advice-hero-banner {
    display: flex;
    align-items: center;
    gap: 14px;
    background: linear-gradient(135deg, #eff6ff 0%, #f5f3ff 100%);
    border: 1px solid #bfdbfe;
    border-radius: 16px;
    padding: 18px 20px;

    .advice-hero-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: #1677ff;
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      flex-shrink: 0;
    }

    .advice-hero-text {
      h4 {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }

      p {
        margin: 4px 0 0;
        font-size: 12.5px;
        color: #64748b;
        line-height: 1.4;
      }
    }
  }

  .advice-block {
    .advice-block-title {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      margin: 0 0 10px;
      font-size: 14.5px;
      font-weight: 700;
      color: #1e293b;

      .el-icon {
        color: #1677ff;
      }
    }

    .advice-summary-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 14px 16px;
      font-size: 13.5px;
      color: #334155;
      line-height: 1.7;
    }

    .advice-actions-list {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .action-item-row {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 12px 14px;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);

        .action-num {
          width: 22px;
          height: 22px;
          border-radius: 50%;
          background: rgba(22, 119, 255, 0.1);
          color: #1677ff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 700;
          flex-shrink: 0;
        }

        .action-text {
          font-size: 13px;
          color: #334155;
          line-height: 1.5;
        }
      }
    }
  }
}

.drawer-footer-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.ai-teaching-engine-dialog) {
  .el-dialog {
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 24px 64px rgba(15, 23, 42, 0.18);
  }

  .el-dialog__header {
    padding: 20px 24px 14px;
    margin-right: 0;
    border-bottom: 1px solid #f1f5f9;

    .el-dialog__title {
      font-size: 16.5px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }
}
</style>
