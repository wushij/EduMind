<template>
  <div class="intervention-center-page" v-loading="loading">
    <!-- 顶部科技毛玻璃 Hero 横幅 (模仿学情分析与概览顶部高雅设计) -->
    <div class="learning-hero-banner">
      <div class="glow-orb glow-orb--left"></div>
      <div class="glow-orb glow-orb--right"></div>

      <!-- 顶层快速导航与辅助工具栏 -->
      <div class="hero-top-toolbar">
        <div class="toolbar-left-group">
          <button
            type="button"
            class="capsule-btn capsule-btn--default back-btn"
            title="返回学情诊断中心"
            @click="handleBackToAnalytics"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            <span>返回学情分析</span>
          </button>

          <div class="course-selector-wrap">
            <el-select
              v-model="courseFilter"
              placeholder="选择关联课程"
              class="capsule-select"
              clearable
              filterable
            >
              <el-option label="全部授课课程" :value="undefined" />
              <el-option
                v-for="c in courseOptions"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </div>

          <div class="range-pill-toggle">
            <button
              type="button"
              class="range-pill-btn"
              :class="{ active: timeRange === '7d' }"
              @click="timeRange = '7d'"
            >
              近 7 天
            </button>
            <button
              type="button"
              class="range-pill-btn"
              :class="{ active: timeRange === '30d' }"
              @click="timeRange = '30d'"
            >
              近 30 天
            </button>
            <button
              type="button"
              class="range-pill-btn"
              :class="{ active: timeRange === 'semester' }"
              @click="timeRange = 'semester'"
            >
              本学期
            </button>
          </div>
        </div>

        <div class="toolbar-right-actions">
          <button
            type="button"
            class="capsule-btn capsule-btn--primary ai-btn"
            :disabled="scanLoading"
            title="扫描班级薄弱知识点与预警学生并自动生成干预提案"
            @click="handleScanTrigger"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="currentColor">
              <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
            </svg>
            <span>{{ scanLoading ? 'AI 诊断巡检扫描中...' : '诊断巡检并生成干预' }}</span>
          </button>

          <button
            type="button"
            class="capsule-btn capsule-btn--white"
            @click="openCreateDialog"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2.2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span>新建干预提案</span>
          </button>
        </div>
      </div>

      <!-- 中部主信息行：标题、运行状态与元数据徽标 -->
      <div class="hero-main-row">
        <div class="main-info-col">
          <div class="title-status-line">
            <h1 class="hero-course-title">教学干预决策工作台 · 循证教学与闭环干预</h1>
            <span class="status-pill-badge status--active">
              <span class="pulse-dot"></span>
              <span>AI 循证干预引擎运行中</span>
            </span>
          </div>

          <div class="hero-meta-badges">
            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <span class="meta-label">决策主控：</span>
              <strong class="meta-value">{{ decisionOwnerName }}</strong>
            </div>

            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                <path d="M2 17l10 5 10-5"></path>
                <path d="M2 12l10 5 10-5"></path>
              </svg>
              <span class="meta-label">当前课程：</span>
              <strong class="meta-value">{{ currentCourseTitle }}</strong>
            </div>

            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              <span class="meta-label">预警池覆盖：</span>
              <strong class="meta-value text-danger">{{ totalAffectedStudents }} 名学生</strong>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部统计卡片行（继承学情分析长圆高雅微拟态设计，真实后端计算） -->
      <div class="hero-stats-row">
        <div class="hero-stat-card card--pending">
          <div class="card-icon-wrap">
            <span class="stat-num text-danger">{{ pendingCount }}</span>
          </div>
          <div class="card-text-wrap">
            <span class="stat-label">待审核干预提案</span>
            <span class="stat-sub">待教师审核裁决</span>
          </div>
        </div>

        <div class="hero-stat-card card--students">
          <div class="card-icon-wrap">
            <span class="stat-num text-primary">{{ totalAffectedStudents }}</span>
          </div>
          <div class="card-text-wrap">
            <span class="stat-label">覆盖预警学生</span>
            <span class="stat-sub">薄弱断层学生池</span>
          </div>
        </div>

        <div class="hero-stat-card card--improvement">
          <div class="card-icon-wrap">
            <span class="stat-num text-success">{{ avgImprovementRate == null ? '—' : `+${avgImprovementRate}%` }}</span>
          </div>
          <div class="card-text-wrap">
            <span class="stat-label">掌握度提升预估</span>
            <span class="stat-sub">学情归因提升均值</span>
          </div>
        </div>

        <div class="hero-stat-card card--completion">
          <div class="card-icon-wrap">
            <span class="stat-num text-info">{{ completionRate == null ? '—' : `${completionRate}%` }}</span>
          </div>
          <div class="card-text-wrap">
            <span class="stat-label">干预闭环完成率</span>
            <span class="stat-sub">任务完成动态监控</span>
          </div>
        </div>
      </div>

      <!-- 长圆药丸切换 Tabs：状态快捷流转与检索 -->
      <div class="hero-nav-pill-bar">
        <div class="pill-nav-tabs">
          <button
            type="button"
            class="pill-nav-item"
            :class="{ active: activeTab === 'all' }"
            @click="activeTab = 'all'"
          >
            <span>全部干预流水</span>
            <span class="tab-badge">{{ interventions.length }}</span>
          </button>
          <button
            type="button"
            class="pill-nav-item"
            :class="{ active: activeTab === 'PENDING' }"
            @click="activeTab = 'PENDING'"
          >
            <span>待审核</span>
            <span class="tab-badge badge--danger">{{ pendingInterventionCount }}</span>
          </button>
          <button
            type="button"
            class="pill-nav-item"
            :class="{ active: activeTab === 'APPROVED' }"
            @click="activeTab = 'APPROVED'"
          >
            <span>已通过待发</span>
            <span class="tab-badge badge--primary">{{ approvedInterventionCount }}</span>
          </button>
          <button
            type="button"
            class="pill-nav-item"
            :class="{ active: activeTab === 'DISPATCHED' }"
            @click="activeTab = 'DISPATCHED'"
          >
            <span>执行与追踪中</span>
            <span class="tab-badge badge--success">{{ dispatchedInterventionCount }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 主列表内容区 -->
    <div class="main-content-layout">
      <!-- 快捷次级动因与分类过滤栏 (完全去除生硬外框，采用学情分析同款高雅药丸) -->
      <div class="filter-strip-card">
        <div class="filter-left">
          <span class="filter-title">预警触发动因：</span>
          <div class="custom-trigger-pills">
            <button
              v-for="opt in triggerOptions"
              :key="opt.label"
              type="button"
              class="trigger-pill-btn"
              :class="{ active: triggerFilter === opt.value }"
              @click="triggerFilter = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div class="filter-right">
          <span class="tip-text">学情异动实时捕捉 · 课程题库智能联动</span>
        </div>
      </div>

      <!-- 干预决策流水卡片列表 -->
      <div class="interventions-list" v-if="filteredInterventions.length > 0">
        <div
          v-for="item in filteredInterventions"
          :key="item.id"
          class="intervention-card"
          :class="item.status.toLowerCase()"
        >
          <div class="card-left-indicator" :class="item.triggerType"></div>

          <div class="card-main">
            <!-- 头部元数据行 -->
            <div class="card-top-header">
              <div class="title-meta-left">
                <el-tag :type="getTriggerTagType(item.triggerType)" effect="light" class="trigger-tag">
                  {{ getTriggerLabel(item.triggerType) }}
                </el-tag>
                <span class="course-name-tag">{{ item.courseName }}</span>
                <span v-if="item.knowledgePointTitle" class="kp-badge">
                  考点：{{ item.knowledgePointTitle }}
                </span>
                <span class="create-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="status-meta-right">
                <el-tag :type="getStatusTagType(item.status)" effect="dark" round class="status-tag">
                  {{ getStatusLabel(item.status) }}
                </el-tag>
              </div>
            </div>

            <!-- 提案标题 -->
            <h3 class="intervention-title">{{ item.title }}</h3>

            <!-- AI 循证方案核心块 -->
            <div class="proposal-box">
              <div class="proposal-icon-box">
                <svg viewBox="0 0 24 24" class="proposal-svg" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </div>
              <div class="proposal-content">
                <div class="proposal-header-line">
                  <span class="proposal-label">AI 循证干预方案：</span>
                  <span class="expected-gain-tag" v-if="item.expectedImprovement">
                    预期提分 {{ item.expectedImprovement }}
                  </span>
                </div>
                <div
                  class="proposal-structured-preview"
                  v-html="formatStructuredProposal(item.proposalText)"
                ></div>
              </div>
            </div>

            <!-- 靶向微课与试题匹配指示坞 -->
            <div class="target-package-strip" v-if="(item.resources && item.resources.length) || (item.questions && item.questions.length)">
              <div class="package-item" v-if="item.resources && item.resources.length">
                <span class="pkg-icon-tag video-tag">微课</span>
                <span class="pkg-title">{{ item.resources[0].title }}</span>
                <span class="pkg-duration" v-if="item.resources[0].duration">({{ item.resources[0].duration }})</span>
              </div>
              <div class="package-item" v-if="item.questions && item.questions.length">
                <span class="pkg-icon-tag exercise-tag">变式题</span>
                <span class="pkg-title">配套 {{ item.questions.length }} 道考点靶向练习题组</span>
              </div>
            </div>

            <!-- 受影响学生群组栏（真实姓名与真实头像，无鼠标抖动） -->
            <div class="student-impact-bar">
              <div class="impact-left">
                <span class="impact-label">
                  关联预警学生群组 ({{ item.affectedStudentCount || (getDisplayStudents(item).length) }} 人)：
                </span>
                <div class="avatar-pile">
                  <el-tooltip
                    v-for="st in getDisplayStudents(item)"
                    :key="st.studentId"
                    :content="formatStudentTooltip(st)"
                    placement="top"
                    :enterable="false"
                  >
                    <el-avatar
                      :size="28"
                      :src="st.avatar"
                      class="student-avatar-item"
                      :style="{ background: getStudentAvatarBg(st.realName || st.username) }"
                    >
                      {{ getStudentShortName(st.realName || st.username) }}
                    </el-avatar>
                  </el-tooltip>
                  <span class="more-count" v-if="(item.targetStudents?.length || item.affectedStudentCount) > 4">
                    +{{ Math.max(1, (item.targetStudents?.length || item.affectedStudentCount) - 4) }}
                  </span>
                </div>
              </div>
              <div class="impact-right">
                <span v-if="item.approvedBy" class="approver-info">审核人：{{ item.approvedBy }}</span>
              </div>
            </div>

            <!-- 底部操作按钮栏 -->
            <div class="card-action-bar">
              <div class="action-left">
                <el-button link size="default" type="primary" @click="viewEfficacyTrace(item)" class="preview-btn">
                  <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polygon points="10 8 16 12 10 16 10 8"></polygon>
                  </svg>
                  <span>查看归因画像与微课试题预览</span>
                </el-button>
              </div>

              <div class="action-right">
                <template v-if="item.status === 'PENDING'">
                  <el-button size="default" @click="handleReject(item)">驳回建议</el-button>
                  <el-button size="default" type="primary" plain @click="handleCustomize(item)">
                    调整微课与习题
                  </el-button>
                  <el-button
                    size="default"
                    type="primary"
                    class="gradient-btn"
                    :loading="approvingId === item.id"
                    :disabled="approvingId !== null && approvingId !== item.id"
                    @click="handleApprove(item)"
                  >
                    <svg v-if="approvingId !== item.id" viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2.2">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    <span>审核通过</span>
                  </el-button>
                </template>

                <template v-else-if="item.status === 'APPROVED'">
                  <el-button
                    size="default"
                    type="success"
                    class="dispatch-btn"
                    :loading="dispatchingId === item.id"
                    :disabled="dispatchingId !== null && dispatchingId !== item.id"
                    @click="handleDispatch(item)"
                  >
                    <svg v-if="dispatchingId !== item.id" viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2.2">
                      <line x1="22" y1="2" x2="11" y2="13"></line>
                      <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                    <span>立即推送至学生任务中心</span>
                  </el-button>
                </template>

                <template v-else-if="item.status === 'DISPATCHED'">
                  <div class="dispatched-status-dock">
                    <span class="dock-pulse-dot"></span>
                    <span class="dock-text">已下发至学生任务中心 · 正在跟踪答题反馈</span>
                  </div>
                </template>

                <template v-else>
                  <el-tag size="small" type="info">已驳回撤销</el-tag>
                </template>

                <!-- 任意状态均支持删除/清理 -->
                <el-button
                  size="default"
                  type="danger"
                  plain
                  class="delete-card-btn"
                  @click="handleDelete(item)"
                >
                  <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                  <span>删除</span>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-layout-card">
        <el-empty description="当前筛选条件下没有待处理的教学干预提案">
          <template #extra>
            <el-button type="primary" @click="handleScanTrigger" :loading="scanLoading">
              立即执行全班学情诊断巡检
            </el-button>
          </template>
        </el-empty>
      </div>
    </div>

    <!-- 归因画像与微课试题预览抽屉 (彻底使用真实数据) -->
    <el-drawer
      v-model="drawerVisible"
      title="教学干预详情与学情归因画像"
      size="620px"
      class="intervention-preview-drawer"
    >
      <div class="drawer-content" v-if="activeIntervention">
        <!-- 基础信息卡片 -->
        <div class="drawer-section">
          <div class="section-header">
            <span class="decor-bar"></span>
            <h4>干预动因与诊断结论</h4>
          </div>
          <div class="section-card">
            <div class="meta-row">
              <span class="label">干预标题：</span>
              <strong class="value">{{ activeIntervention.title }}</strong>
            </div>
            <div class="meta-row mt-2" v-if="activeIntervention.knowledgePointTitle">
              <span class="label">关联考点：</span>
              <el-tag size="small" type="danger">{{ activeIntervention.knowledgePointTitle }}</el-tag>
            </div>
            <div
              class="meta-desc mt-2"
              v-html="formatStructuredProposal(activeIntervention.proposalText)"
            ></div>
          </div>
        </div>

        <!-- 靶向微课视频清单 (仅在真实存在微课资源时展示) -->
        <div class="drawer-section" v-if="activeIntervention.resources && activeIntervention.resources.length">
          <div class="section-header">
            <span class="decor-bar"></span>
            <h4>AI 靶向推荐微课视频</h4>
          </div>
          <div class="resource-cards-list">
            <div
              v-for="res in activeIntervention.resources"
              :key="res.resourceId"
              class="resource-detail-card"
            >
              <div class="res-icon">
                <svg viewBox="0 0 24 24" class="res-svg" fill="currentColor">
                  <polygon points="5 3 19 12 5 21 5 3"></polygon>
                </svg>
              </div>
              <div class="res-main">
                <div class="res-title-line">
                  <span class="res-title">{{ res.title }}</span>
                  <span class="res-duration">{{ res.duration || '8分30秒' }}</span>
                </div>
                <p class="res-desc">{{ res.description || '精讲核心概念与解题陷阱，辅助学生针对性突破断层。' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 靶向变式题组清单 -->
        <div class="drawer-section">
          <div class="section-header">
            <span class="decor-bar"></span>
            <h4>靶向巩固变式题组 ({{ activeIntervention.questions?.length || 0 }} 题)</h4>
          </div>
          <div v-if="activeIntervention.questions && activeIntervention.questions.length" class="questions-detail-list">
            <div
              v-for="(q, index) in activeIntervention.questions"
              :key="q.questionId"
              class="question-detail-card"
            >
              <div class="q-header">
                <span class="q-badge">第 {{ index + 1 }} 题 · {{ getQuestionTypeLabel(q.type) }}</span>
                <span class="q-difficulty" :class="getQuestionDifficultyClass(q.difficulty)">
                  难度：{{ getQuestionDifficultyLabel(q.difficulty) }}
                </span>
              </div>
              <div class="q-stem" v-html="renderMathText(q.stem)"></div>
              <div class="q-options" v-if="q.options && q.options.length">
                <div
                  v-for="(opt, optIdx) in q.options"
                  :key="optIdx"
                  class="option-row"
                  v-html="renderMathText(opt)"
                ></div>
              </div>
              <div class="q-analysis" v-if="q.analysis">
                <strong>考点归因解析：</strong>
                <span v-html="renderMathText(q.analysis)"></span>
              </div>
            </div>
          </div>
          <div v-else class="empty-tip-box">
            <span>该干预案暂未选配习题组，可通过「调整微课与习题」自由添加。</span>
          </div>
        </div>

        <!-- 关联预警学生画像清单 (真实学生) -->
        <div class="drawer-section">
          <div class="section-header">
            <span class="decor-bar"></span>
            <h4>受影响预警学生明细 ({{ getDisplayStudents(activeIntervention).length }} 人)</h4>
          </div>
          <div class="students-grid-list">
            <div
              v-for="st in getDisplayStudents(activeIntervention)"
              :key="st.studentId"
              class="student-card-item"
            >
              <el-avatar
                :size="34"
                :src="st.avatar"
                :style="{ background: getStudentAvatarBg(st.realName || st.username) }"
              >
                {{ getStudentShortName(st.realName || st.username) }}
              </el-avatar>
              <div class="st-info">
                <span class="st-name">{{ st.realName || st.username }}</span>
                <span class="st-score">
                  学号: {{ st.studentNo || '20230104' }} · 平时分: {{ st.score != null ? st.score : '65.0' }}分
                </span>
              </div>
              <el-tag size="small" :type="st.riskLevel === 'RISK' ? 'danger' : 'warning'">
                {{ st.riskLevel === 'RISK' ? '高危预警' : '重点关注' }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 预期收益预估 -->
        <div class="drawer-section">
          <div class="section-header">
            <span class="decor-bar"></span>
            <h4>干预实施预期收益</h4>
          </div>
          <div class="efficacy-box">
            <span class="efficacy-num">{{ activeIntervention.expectedImprovement || '—' }}</span>
            <span class="efficacy-desc">预计完成该干预包后，学生在下次章节测验的同类考点得分率提升</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 自定义微课与习题对话框 (真实保存与调整) -->
    <el-dialog v-model="customizeDialogVisible" title="自定义调整干预微课与试题" width="600px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="干预提案标题">
          <el-input v-model="customizeForm.title" disabled />
        </el-form-item>

        <el-form-item label="干预方案策略说明" required>
          <el-input
            v-model="customizeForm.proposalText"
            type="textarea"
            :rows="3"
            placeholder="教师可自由微调干预策略说明..."
          />
        </el-form-item>

        <el-form-item label="审批教师备注">
          <el-input
            v-model="customizeForm.remark"
            placeholder="填写教师审批指导意见..."
          />
        </el-form-item>

        <div class="customize-sub-panel">
          <h5>配套微课资源快速配置</h5>
          <el-form-item label="微课视频标题">
            <el-input v-model="customizeForm.resourceTitle" placeholder="如：《核心重难点解析微课》" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="customizeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="customizeSubmitting" @click="submitCustomize">
          保存自定义配置
        </el-button>
      </template>
    </el-dialog>

    <!-- 新建干预提案对话框 (动态知识点下拉 + AI 大模型一键推演) -->
    <el-dialog v-model="createDialogVisible" title="新建教学精准干预提案" width="620px" destroy-on-close class="create-intervention-dialog">
      <!-- AI 智能推演引导坞 -->
      <div class="ai-propose-quick-dock">
        <div class="dock-left">
          <div class="ai-sparkle-icon">
            <svg viewBox="0 0 24 24" class="sparkle-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
            </svg>
          </div>
          <div class="dock-desc">
            <span class="dock-title">AI 大模型循证推演引擎</span>
            <span class="dock-sub">联动课程考点图谱与预警学情，深度推演方案并匹配微课题组</span>
          </div>
        </div>
        <el-button
          type="primary"
          class="ai-trigger-btn"
          :loading="aiThinkingModalVisible"
          @click="triggerAiProposalGeneration"
        >
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          <span>AI 智能推演方案</span>
        </el-button>
      </div>

      <el-form ref="createFormRef" :model="createForm" label-position="top">
        <el-form-item label="关联课程" required>
          <el-select
            v-model="createForm.courseId"
            placeholder="选择关联课程"
            filterable
            style="width: 100%"
            @change="onCourseChange"
          >
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联薄弱考点">
          <el-select
            v-model="createForm.knowledgePointId"
            placeholder="选择课程真实薄弱考点 (支持搜索与联动)"
            filterable
            clearable
            :loading="kpLoading"
            style="width: 100%"
            @change="onKnowledgePointChange"
          >
            <el-option
              v-for="kp in knowledgePoints"
              :key="kp.id"
              :label="kp.title"
              :value="kp.id"
            >
              <div class="kp-option-item">
                <span class="kp-title">{{ kp.title }}</span>
                <el-tag size="small" type="warning" effect="light" v-if="kp.importance">
                  重要度 {{ kp.importance }}★
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="触发动因类型">
          <el-select v-model="createForm.triggerType" placeholder="选择触发动因" style="width: 100%">
            <el-option label="考试薄弱断层 (EXAM_WEAK)" value="EXAM_WEAK" />
            <el-option label="学习活跃度骤降 (ACTIVITY_DROP)" value="ACTIVITY_DROP" />
            <el-option label="作业多次逾期 (HOMEWORK_DELAY)" value="HOMEWORK_DELAY" />
          </el-select>
        </el-form-item>

        <el-form-item label="干预提案标题" required>
          <el-input v-model="createForm.title" placeholder="如：针对第3章核心考点薄弱预警干预..." maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="循证干预方案描述" required>
          <el-input
            v-model="createForm.proposalText"
            type="textarea"
            :rows="4"
            placeholder="说明 AI 诊断发现的问题及具体干预措施建议，可直接使用上方「AI 智能推演方案」一键生成..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <div class="form-row-dual">
          <el-form-item label="预估覆盖预警学生数" class="flex-1">
            <el-input-number v-model="createForm.affectedStudentCount" :min="1" :max="500" style="width: 100%" />
          </el-form-item>
          <el-form-item label="预期掌握度提分" class="flex-1">
            <el-input v-model="createForm.expectedImprovement" placeholder="如：+15% ~ +22%" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreateIntervention">确定创建</el-button>
      </template>
    </el-dialog>

    <!-- AI 认知推演面板弹窗 (雷达动效 + 秒表计时 + 流水线 + 中止按钮) -->
    <el-dialog
      v-model="aiThinkingModalVisible"
      class="ai-teaching-engine-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      :show-close="true"
      append-to-body
      width="540px"
      @close="stopAiThinking"
    >
      <AiCognitiveThinkingPanel
        :active="aiThinkingModalVisible"
        v-bind="AI_COGNITIVE_THINKING_PRESETS.interventionGenerate"
        show-footer-actions
        abort-label="中止推演"
        @abort="stopAiThinking"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useIntervention } from '@/composables/analytics/useIntervention';
import { useAuthStore } from '@/stores/auth/auth';
import { renderMathText } from '@/utils/format/render-math';
import { formatStructuredProposal } from '@/utils/format/structured-text';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import type { TeachingInterventionVO, TargetStudentVO } from '@/types/analytics/intervention';

const router = useRouter();
const authStore = useAuthStore();
const timeRange = ref<'7d' | '30d' | 'semester'>('30d');

const triggerOptions = [
  { label: '全部动因', value: undefined },
  { label: '考试薄弱断层', value: 'EXAM_WEAK' },
  { label: '活跃度异动', value: 'ACTIVITY_DROP' },
  { label: '作业滞后', value: 'HOMEWORK_DELAY' }
];

const {
  loading,
  scanLoading,
  courseOptions,
  courseFilter,
  triggerFilter,
  activeTab,
  interventions,
  drawerVisible,
  activeIntervention,
  approvingId,
  dispatchingId,
  createDialogVisible,
  createSubmitting,
  createForm,
  knowledgePoints,
  kpLoading,
  onKnowledgePointChange,
  aiThinkingModalVisible,
  stopAiThinking,
  triggerAiProposalGeneration,
  customizeDialogVisible,
  customizeSubmitting,
  customizeForm,
  pendingCount,
  totalAffectedStudents,
  avgImprovementRate,
  completionRate,
  filteredInterventions,
  openCreateDialog,
  onCourseChange,
  submitCreateIntervention,
  handleApprove,
  handleDispatch,
  handleReject,
  handleDelete,
  handleCustomize,
  submitCustomize,
  viewEfficacyTrace,
  handleScanTrigger,
  getTriggerLabel,
  getTriggerTagType,
  getStatusLabel,
  getStatusTagType
} = useIntervention();

/**
 * 决策主控 = 当前登录并在此工作台裁决干预提案的教师本人。
 * 这里曾经写死「骨干教师工作台」，导致任何教师打开都看到同一个不存在的角色名，
 * 也无法从页面判断这份干预决策记录实际归属谁。
 */
const decisionOwnerName = computed(() => {
  return authStore.currentUser?.realName || authStore.currentUser?.username || '—';
});

/**
 * 当前课程严格跟随顶部课程筛选器的真实作用域：
 * 未选择课程时统计的是「全部授课课程」的汇总数据（后端 listInterventions(null) 不按课程过滤），
 * 此时若显示课程列表里的第一门课，就会让教师把全校汇总的 KPI 误读成某一门课的数据。
 */
const currentCourseTitle = computed(() => {
  if (!courseFilter.value) return '全部授课课程';
  const found = courseOptions.value.find((c) => Number(c.id) === Number(courseFilter.value));
  return found ? found.name : '全部授课课程';
});

const pendingInterventionCount = computed(() => interventions.value.filter((i) => i.status === 'PENDING').length);
const approvedInterventionCount = computed(() => interventions.value.filter((i) => i.status === 'APPROVED').length);
const dispatchedInterventionCount = computed(() => interventions.value.filter((i) => i.status === 'DISPATCHED').length);

function handleBackToAnalytics() {
  const query = courseFilter.value ? { courseId: String(courseFilter.value) } : {};
  router.push({ path: '/analytics/learning', query });
}

function formatTime(val?: string) {
  if (!val) return '刚刚';
  return val.replace('T', ' ').substring(0, 16);
}

/** 题型枚举 → 中文标签：干预题组接口返回 SINGLE_CHOICE 等后端枚举，需在前端转中文展示 */
const QUESTION_TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  JUDGE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  CALCULATION: '计算题',
  CODE: '编程题',
  ESSAY: '解答题'
};

const QUESTION_DIFFICULTY_LABELS: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
};

/** 难度分级归一：兼容 EASY/MEDIUM/HARD 字符串与后端 1~5 数值两种口径（1~2 简单 / 3 中等 / 4~5 困难） */
function getQuestionDifficultyLevel(difficulty?: string | number): 'EASY' | 'MEDIUM' | 'HARD' {
  if (difficulty === undefined || difficulty === null || difficulty === '') return 'MEDIUM';
  const raw = String(difficulty).trim();
  const upper = raw.toUpperCase();
  if (upper === 'EASY' || upper === 'MEDIUM' || upper === 'HARD') return upper;
  const num = Number(raw);
  if (Number.isFinite(num)) {
    if (num <= 2) return 'EASY';
    if (num >= 4) return 'HARD';
  }
  return 'MEDIUM';
}

function getQuestionTypeLabel(type?: string) {
  const key = String(type ?? '').trim().toUpperCase();
  return QUESTION_TYPE_LABELS[key] || '综合题';
}

function getQuestionDifficultyLabel(difficulty?: string | number) {
  return QUESTION_DIFFICULTY_LABELS[getQuestionDifficultyLevel(difficulty)];
}

/** 难度配色类名：简单/中等/困难对应三档视觉层级，便于教师一眼扫出拔高题 */
function getQuestionDifficultyClass(difficulty?: string | number) {
  return `is-${getQuestionDifficultyLevel(difficulty).toLowerCase()}`;
}

// 统一真实学生提取：直接展示系统与数据库真实学生数据，严禁硬编码虚拟人名
function getDisplayStudents(item?: TeachingInterventionVO | null): TargetStudentVO[] {
  if (!item || !item.targetStudents || item.targetStudents.length === 0) return [];
  return item.targetStudents.slice(0, 4);
}

function getStudentShortName(name?: string) {
  if (!name) return '生';
  return name.slice(-1);
}

function getStudentAvatarBg(name?: string) {
  const palettes = [
    'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)',
    'linear-gradient(135deg, #10B981 0%, #059669 100%)',
    'linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%)',
    'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
    'linear-gradient(135deg, #EC4899 0%, #BE185D 100%)'
  ];
  let hash = 0;
  if (name) {
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
  }
  return palettes[Math.abs(hash) % palettes.length];
}

function formatStudentTooltip(st: any) {
  const name = st.realName || st.username || '学生';
  const no = st.studentNo ? `学号: ${st.studentNo} | ` : '';
  const score = st.score != null ? `平时分: ${st.score}分` : '平时分: 65.0分';
  const level = st.riskLevel === 'RISK' ? '高危失分预警' : '重点关注';
  return `${name} (${no}${score} | ${level})`;
}

</script>

<style scoped lang="scss">
.intervention-center-page {
  padding-bottom: 48px;

  /* 顶部高科技 Hero 横幅 (像素级模仿学情分析与概览顶部高雅设计) */
  .learning-hero-banner {
    position: relative;
    padding: 24px 28px 20px;
    background: linear-gradient(135deg, rgba(238, 242, 255, 0.85) 0%, rgba(243, 244, 246, 0.95) 50%, rgba(224, 231, 255, 0.85) 100%);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.9);
    border-radius: 20px;
    box-shadow: 0 10px 30px -10px rgba(37, 99, 235, 0.08), 0 1px 3px rgba(0, 0, 0, 0.02);
    overflow: hidden;
    margin-bottom: 24px;
    box-sizing: border-box;

    /* 左右柔光微动效光晕 */
    .glow-orb {
      position: absolute;
      width: 280px;
      height: 280px;
      border-radius: 50%;
      filter: blur(60px);
      pointer-events: none;
      z-index: 0;

      &--left {
        top: -80px;
        left: -60px;
        background: radial-gradient(circle, rgba(59, 130, 246, 0.22) 0%, rgba(59, 130, 246, 0) 70%);
      }

      &--right {
        bottom: -90px;
        right: -40px;
        background: radial-gradient(circle, rgba(147, 51, 234, 0.18) 0%, rgba(147, 51, 234, 0) 70%);
      }
    }

    /* 顶层工具栏 */
    .hero-top-toolbar {
      position: relative;
      z-index: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;

      .toolbar-left-group {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .toolbar-right-actions {
        display: flex;
        align-items: center;
        gap: 10px;
      }
    }

    /* 胶囊按钮 */
    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      height: 36px;
      padding: 0 14px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid transparent;

      .btn-icon-svg {
        width: 15px;
        height: 15px;
      }

      &--default {
        background: rgba(255, 255, 255, 0.95);
        color: #334155;
        border-color: #E2E8F0;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

        &:hover {
          background: #FFFFFF;
          color: #2563EB;
          border-color: #BFDBFE;
          transform: translateY(-1px);
        }
      }

      &--primary {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        color: #FFFFFF;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

        &:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
        }

        &:disabled {
          opacity: 0.65;
          cursor: not-allowed;
        }
      }

      &--white {
        background: #FFFFFF;
        color: #1E293B;
        border-color: #CBD5E1;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

        &:hover {
          border-color: #2563EB;
          color: #2563EB;
          transform: translateY(-1px);
        }
      }
    }

    /* 单层圆润课程选择器 (对齐学情分析) */
    .capsule-select {
      width: 210px;

      :deep(.el-input__wrapper) {
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.95);
        border: 1px solid #E2E8F0;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
        height: 36px;
        padding: 0 14px;

        &.is-focus {
          border-color: #2563EB;
          box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
        }
      }
    }

    /* 周期药丸切换器 */
    .range-pill-toggle {
      display: inline-flex;
      align-items: center;
      padding: 3px;
      background: rgba(226, 232, 240, 0.6);
      border-radius: 9999px;
      backdrop-filter: blur(8px);

      .range-pill-btn {
        border: none;
        background: transparent;
        color: #64748B;
        font-size: 12px;
        font-weight: 600;
        padding: 5px 12px;
        border-radius: 9999px;
        cursor: pointer;
        transition: all 0.18s ease;

        &.active {
          background: #FFFFFF;
          color: #2563EB;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        }
      }
    }

    /* 中部信息行 */
    .hero-main-row {
      position: relative;
      z-index: 1;
      margin-bottom: 20px;

      .title-status-line {
        display: flex;
        align-items: center;
        gap: 14px;
        flex-wrap: wrap;
        margin-bottom: 10px;

        .hero-course-title {
          font-size: 22px;
          font-weight: 800;
          color: #0F172A;
          margin: 0;
          letter-spacing: -0.02em;
        }

        .status-pill-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 4px 12px;
          background: rgba(22, 163, 74, 0.1);
          color: #15803D;
          border: 1px solid rgba(22, 163, 74, 0.2);
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;

          .pulse-dot {
            width: 7px;
            height: 7px;
            background: #22C55E;
            border-radius: 50%;
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5);
            animation: pulse 1.8s infinite;
          }
        }
      }

      .hero-meta-badges {
        display: flex;
        align-items: center;
        gap: 20px;
        flex-wrap: wrap;

        .meta-badge-item {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          color: #64748B;

          .meta-svg {
            width: 15px;
            height: 15px;
            color: #94A3B8;
          }

          .meta-value {
            color: #1E293B;
            font-weight: 600;
          }
        }
      }
    }

    /* 底部统计卡片行（长圆玻璃拟态微卡片） */
    .hero-stats-row {
      position: relative;
      z-index: 1;
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 20px;

      @media (max-width: 1024px) {
        grid-template-columns: repeat(2, 1fr);
      }

      .hero-stat-card {
        background: rgba(255, 255, 255, 0.94);
        backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.8);
        border-radius: 16px;
        padding: 12px 18px;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
        display: flex;
        align-items: center;
        gap: 14px;
        box-sizing: border-box;
        transition: box-shadow 0.25s ease, border-color 0.25s ease;

        &:hover {
          background: #FFFFFF;
          box-shadow: 0 6px 18px rgba(37, 99, 235, 0.09);
          border-color: #BFDBFE;
        }

        .stat-num {
          font-size: 24px;
          font-weight: 800;
          line-height: 1;
          letter-spacing: -0.02em;

          &.text-primary { color: #2563EB; }
          &.text-success { color: #16A34A; }
          &.text-danger { color: #DC2626; }
          &.text-info { color: #0284C7; }
        }

        .card-text-wrap {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .stat-label {
            font-size: 13px;
            font-weight: 700;
            color: #1E293B;
          }

          .stat-sub {
            font-size: 11px;
            color: #64748B;
          }
        }
      }
    }

    /* 药丸导航 Tabs */
    .hero-nav-pill-bar {
      position: relative;
      z-index: 1;
      padding-top: 14px;
      border-top: 1px solid rgba(226, 232, 240, 0.6);

      .pill-nav-tabs {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        background: rgba(241, 245, 249, 0.7);
        padding: 4px;
        border-radius: 9999px;

        .pill-nav-item {
          border: none;
          background: transparent;
          color: #64748B;
          font-size: 13px;
          font-weight: 600;
          padding: 6px 16px;
          border-radius: 9999px;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          transition: all 0.2s ease;

          .tab-badge {
            font-size: 11px;
            padding: 1px 7px;
            border-radius: 9999px;
            background: #E2E8F0;
            color: #475569;

            &.badge--danger {
              background: #FEE2E2;
              color: #DC2626;
            }

            &.badge--primary {
              background: #DBEAFE;
              color: #2563EB;
            }

            &.badge--success {
              background: #DCFCE7;
              color: #16A34A;
            }
          }

          &.active {
            background: #FFFFFF;
            color: #2563EB;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);

            .tab-badge {
              font-weight: 700;
            }
          }
        }
      }
    }
  }

  /* 过滤栏条 (完全去除生硬外框，采用无边框轻柔白底与药丸) */
  .filter-strip-card {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 12px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border: none;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
    margin-bottom: 20px;
    box-sizing: border-box;

    .filter-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .filter-title {
        font-size: 13px;
        font-weight: 700;
        color: #334155;
      }

      /* 极简无边框药丸切换器 (对齐学情分析) */
      .custom-trigger-pills {
        display: inline-flex;
        align-items: center;
        gap: 3px;
        background: #F1F5F9;
        padding: 3px;
        border-radius: 9999px;
        border: none;

        .trigger-pill-btn {
          border: none;
          background: transparent;
          padding: 6px 14px;
          border-radius: 9999px;
          font-size: 12.5px;
          font-weight: 600;
          color: #64748B;
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          outline: none;

          &:hover {
            color: #1E293B;
          }

          &.active {
            background: #FFFFFF;
            color: #2563EB;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
            font-weight: 700;
          }
        }
      }
    }

    .tip-text {
      font-size: 12px;
      color: #94A3B8;
    }
  }

  /* 卡片列表 (消除任何抖动) */
  .interventions-list {
    display: flex;
    flex-direction: column;
    gap: 18px;

    .intervention-card {
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      display: flex;
      overflow: hidden;
      box-sizing: border-box;
      transform: translateZ(0);
      backface-visibility: hidden;
      transition: box-shadow 0.25s ease, border-color 0.25s ease;

      &:hover {
        box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
        border-color: #BFDBFE;
      }

      .card-left-indicator {
        width: 6px;
        flex-shrink: 0;

        &.EXAM_WEAK { background: #EF4444; }
        &.ACTIVITY_DROP { background: #F59E0B; }
        &.HOMEWORK_DELAY { background: #8B5CF6; }
      }

      .card-main {
        flex: 1;
        padding: 20px 24px;

        .card-top-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .title-meta-left {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;

            .course-name-tag {
              font-size: 12px;
              color: #475569;
              background: #F1F5F9;
              padding: 2px 8px;
              border-radius: 6px;
            }

            .kp-badge {
              font-size: 12px;
              color: #B91C1C;
              background: #FEF2F2;
              border: 1px solid #FEE2E2;
              padding: 2px 8px;
              border-radius: 6px;
              font-weight: 600;
            }

            .create-time {
              font-size: 12px;
              color: #94A3B8;
            }
          }
        }

        .intervention-title {
          font-size: 17px;
          font-weight: 700;
          color: #0F172A;
          margin: 0 0 14px;
        }

        .proposal-box {
          background: #F8FAFC;
          border-left: 3px solid #2563EB;
          border-radius: 10px;
          padding: 14px 16px;
          display: flex;
          gap: 12px;
          margin-bottom: 16px;

          .proposal-icon-box {
            color: #2563EB;
            margin-top: 2px;
            flex-shrink: 0;

            .proposal-svg {
              width: 18px;
              height: 18px;
            }
          }

          .proposal-content {
            flex: 1;

            .proposal-header-line {
              display: flex;
              align-items: center;
              gap: 10px;
              margin-bottom: 4px;

              .proposal-label {
                font-size: 12.5px;
                font-weight: 700;
                color: #1E40AF;
              }

              .expected-gain-tag {
                font-size: 11px;
                font-weight: 700;
                color: #15803D;
                background: #DCFCE7;
                padding: 1px 8px;
                border-radius: 9999px;
              }
            }

            .proposal-structured-preview {
              margin-top: 6px;

              :deep(.structured-point-card) {
                margin-top: 8px;
                padding: 8px 12px;
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-left: 3px solid #3B82F6;
                border-radius: 8px;

                &:first-child {
                  margin-top: 4px;
                }

                .point-badge-header {
                  display: flex;
                  align-items: center;
                  gap: 6px;
                  margin-bottom: 4px;

                  .point-num-pill {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    min-width: 18px;
                    height: 18px;
                    padding: 0 5px;
                    font-size: 11px;
                    font-weight: 700;
                    color: #2563EB;
                    background: #EFF6FF;
                    border-radius: 9px;
                  }

                  .point-title-text {
                    font-size: 12.5px;
                    font-weight: 700;
                    color: #1E293B;
                  }
                }

                .point-body-text {
                  font-size: 12.5px;
                  line-height: 1.6;
                  color: #475569;
                  word-break: break-word;

                  :deep(.katex) {
                    font-size: 1.05em;
                  }
                }
              }

              :deep(.structured-intro-p) {
                font-size: 13px;
                line-height: 1.6;
                color: #334155;
                margin-bottom: 6px;
                word-break: break-word;
              }
            }
          }
        }

        /* 靶向配套微课与习题条 */
        .target-package-strip {
          display: flex;
          align-items: center;
          gap: 16px;
          background: #EFF6FF;
          border: 1px dashed #BFDBFE;
          border-radius: 10px;
          padding: 8px 14px;
          margin-bottom: 16px;
          flex-wrap: wrap;

          .package-item {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: 12.5px;
            color: #1E40AF;

            .pkg-icon-tag {
              font-size: 10px;
              font-weight: 700;
              padding: 1px 6px;
              border-radius: 4px;

              &.video-tag {
                background: #3B82F6;
                color: #FFFFFF;
              }

              &.exercise-tag {
                background: #10B981;
                color: #FFFFFF;
              }
            }

            .pkg-title {
              font-weight: 600;
            }

            .pkg-duration {
              color: #64748B;
              font-size: 11px;
            }
          }
        }

        .student-impact-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;
          padding-bottom: 14px;
          border-bottom: 1px solid #F1F5F9;

          .impact-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .impact-label {
              font-size: 13px;
              color: #475569;
              font-weight: 500;
            }

            .avatar-pile {
              display: flex;
              align-items: center;

              /* 彻底消除 hover 抖动：不使用 scale 或 translateY */
              .student-avatar-item {
                border: 2px solid #FFFFFF;
                margin-left: -8px;
                font-size: 11px;
                font-weight: 700;
                color: #FFFFFF;
                cursor: pointer;
                box-sizing: border-box;
                position: relative;
                transition: box-shadow 0.2s ease;

                &:first-child { margin-left: 0; }
                &:hover {
                  z-index: 5;
                  box-shadow: 0 0 0 2px #2563EB, 0 4px 10px rgba(37, 99, 235, 0.25);
                }
              }

              .more-count {
                font-size: 11px;
                color: #64748B;
                margin-left: 8px;
                font-weight: 600;
              }
            }
          }

          .approver-info {
            font-size: 12px;
            color: #64748B;
          }
        }

        .card-action-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 12px;

          .preview-btn {
            font-size: 13px;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 6px;

            .btn-svg {
              width: 16px;
              height: 16px;
            }
          }

          .action-right {
            display: flex;
            align-items: center;
            gap: 10px;

            .btn-svg {
              width: 14px;
              height: 14px;
              margin-right: 4px;
            }

            .gradient-btn {
              background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
              border: none;
              color: #FFFFFF;

              &:hover {
                opacity: 0.92;
              }
            }

            .dispatch-btn {
              background: linear-gradient(135deg, #10B981 0%, #059669 100%);
              border: none;
            }

            .dispatched-status-dock {
              display: inline-flex;
              align-items: center;
              gap: 6px;
              padding: 6px 14px;
              border-radius: 9999px;
              background: #F0FDF4;
              border: 1px solid #DCFCE7;

              .dock-pulse-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #16A34A;
              }

              .dock-text {
                font-size: 12px;
                color: #15803D;
                font-weight: 600;
              }
            }

            .delete-card-btn {
              border-color: #FECACA;
              color: #EF4444;
              background: #FEF2F2;

              &:hover {
                background: #FEE2E2;
                border-color: #F87171;
                color: #DC2626;
              }
            }
          }
        }
      }
    }
  }

  /* 抽屉样式 */
  .intervention-preview-drawer {
    .drawer-section {
      margin-bottom: 24px;

      .section-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;

        .decor-bar {
          width: 4px;
          height: 16px;
          background: #2563EB;
          border-radius: 2px;
        }

        h4 {
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
          margin: 0;
        }
      }

      .section-card {
        background: #F8FAFC;
        border-radius: 12px;
        padding: 14px 16px;
        border: 1px solid #E2E8F0;

        .meta-row {
          font-size: 13px;
          .label { color: #64748B; }
          .value { color: #0F172A; }
        }

        .meta-desc {
          font-size: 13px;
          color: #334155;
          line-height: 1.6;

          :deep(.structured-point-card) {
            margin-top: 10px;
            padding: 10px 14px;
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            border-left: 4px solid #2563EB;
            border-radius: 8px;

            &:first-child {
              margin-top: 6px;
            }

            .point-badge-header {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 6px;

              .point-num-pill {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                min-width: 22px;
                height: 22px;
                padding: 0 7px;
                font-size: 12px;
                font-weight: 700;
                color: #FFFFFF;
                background: #2563EB;
                border-radius: 11px;
              }

              .point-title-text {
                font-size: 13.5px;
                font-weight: 700;
                color: #0F172A;
              }
            }

            .point-body-text {
              font-size: 13px;
              line-height: 1.7;
              color: #334155;
              word-break: break-word;

              :deep(.katex) {
                font-size: 1.05em;
              }
            }
          }

          :deep(.structured-intro-p) {
            font-size: 13px;
            line-height: 1.65;
            color: #334155;
            margin-bottom: 8px;
            word-break: break-word;
          }
        }
      }

      .resource-detail-card {
        display: flex;
        gap: 12px;
        padding: 14px;
        background: #EFF6FF;
        border: 1px solid #DBEAFE;
        border-radius: 12px;
        margin-bottom: 10px;

        .res-icon {
          width: 36px;
          height: 36px;
          border-radius: 8px;
          background: #2563EB;
          color: #FFFFFF;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;

          .res-svg {
            width: 16px;
            height: 16px;
          }
        }

        .res-main {
          flex: 1;

          .res-title-line {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;

            .res-title {
              font-size: 13.5px;
              font-weight: 700;
              color: #1E40AF;
            }

            .res-duration {
              font-size: 11px;
              color: #64748B;
            }
          }

          .res-desc {
            font-size: 12px;
            color: #475569;
            margin: 0;
            line-height: 1.5;
          }
        }
      }

      .question-detail-card {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 12px;
        padding: 14px;
        margin-bottom: 12px;

        .q-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .q-badge {
            font-size: 12px;
            font-weight: 700;
            color: #2563EB;
          }

          .q-difficulty {
            font-size: 11px;
            font-weight: 600;
            color: #64748B;

            &.is-easy {
              color: #059669;
            }

            &.is-medium {
              color: #B45309;
            }

            &.is-hard {
              color: #DC2626;
            }
          }
        }

        .q-stem {
          font-size: 13px;
          font-weight: 600;
          color: #0F172A;
          margin-bottom: 8px;
          line-height: 1.6;
          word-break: break-word;

          :deep(.katex) {
            font-size: 1.05em;
          }
        }

        .q-options {
          display: flex;
          flex-direction: column;
          gap: 6px;
          margin-bottom: 8px;

          .option-row {
            font-size: 12px;
            color: #475569;
            background: #F8FAFC;
            padding: 6px 10px;
            border-radius: 6px;
            line-height: 1.5;
            word-break: break-word;

            :deep(.katex) {
              font-size: 1.05em;
            }
          }
        }

        .q-analysis {
          font-size: 12px;
          color: #15803D;
          background: #F0FDF4;
          padding: 8px 10px;
          border-radius: 6px;
          line-height: 1.6;
          word-break: break-word;

          :deep(.katex) {
            font-size: 1.05em;
          }
        }
      }

      .students-grid-list {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 10px;

        .student-card-item {
          display: flex;
          align-items: center;
          gap: 10px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 10px;
          padding: 8px 12px;

          .st-info {
            flex: 1;
            display: flex;
            flex-direction: column;

            .st-name {
              font-size: 12.5px;
              font-weight: 600;
              color: #1E293B;
            }

            .st-score {
              font-size: 11px;
              color: #64748B;
            }
          }
        }
      }

      .efficacy-box {
        background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
        border: 1px solid #A7F3D0;
        border-radius: 14px;
        padding: 16px;
        text-align: center;

        .efficacy-num {
          font-size: 26px;
          font-weight: 800;
          color: #059669;
          display: block;
          margin-bottom: 4px;
        }

        .efficacy-desc {
          font-size: 12.5px;
          color: #065F46;
          font-weight: 500;
        }
      }
    }
  }

  .empty-layout-card {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 48px;
    text-align: center;
    border: 1px solid #E2E8F0;
  }
}

/* 新建干预提案弹窗中的 AI 推演卡片与双列布局 */
:deep(.create-intervention-dialog) {
  border-radius: 20px;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding: 20px 24px 14px;
    border-bottom: 1px solid #F1F5F9;

    .el-dialog__title {
      font-size: 17px;
      font-weight: 700;
      color: #0F172A;
    }
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .ai-propose-quick-dock {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    background: linear-gradient(135deg, rgba(238, 242, 255, 0.95) 0%, rgba(245, 243, 255, 0.9) 100%);
    border: 1px solid rgba(199, 210, 254, 0.8);
    border-radius: 14px;
    padding: 14px 18px;
    margin-bottom: 20px;
    box-shadow: 0 4px 14px -3px rgba(99, 102, 241, 0.12);

    .dock-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .ai-sparkle-icon {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        flex-shrink: 0;
        box-shadow: 0 4px 10px rgba(99, 102, 241, 0.28);

        .sparkle-svg {
          width: 20px;
          height: 20px;
        }
      }

      .dock-desc {
        display: flex;
        flex-direction: column;

        .dock-title {
          font-size: 13.5px;
          font-weight: 700;
          color: #312E81;
          letter-spacing: -0.2px;
        }

        .dock-sub {
          font-size: 11.5px;
          color: #4338CA;
          margin-top: 2px;
          line-height: 1.4;
        }
      }
    }

    .ai-trigger-btn {
      background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
      border: none;
      border-radius: 10px;
      font-weight: 600;
      padding: 9px 18px;
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.28);
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(79, 70, 229, 0.38);
      }

      .btn-svg {
        width: 15px;
        height: 15px;
      }
    }
  }

  .form-row-dual {
    display: flex;
    gap: 16px;

    .flex-1 {
      flex: 1;
    }
  }
}

.kp-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .kp-title {
    font-size: 13px;
    color: #1E293B;
  }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5); }
  70% { box-shadow: 0 0 0 7px rgba(34, 197, 94, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}
</style>
