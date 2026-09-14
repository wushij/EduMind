<template>
  <div class="tenant-quota-page" v-loading="loading">
    <PageHeroBanner
      title="租户资源配额与用量大盘 · 智能云容量管控"
      subtitle="实时监控各校区与院系的 AI 算力 Token 消耗、向量检索库存储、QPS 并发吞吐与部门配额预算切分"
      background-variant="system"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">{{ tokenPercentage }}%</span>
            <span class="stat-label">总算力 Token 水位</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ (quotas.storageUsed / 1024).toFixed(2) }} GB</span>
            <span class="stat-label">向量检索库容量</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ quotas.qpsPeak }} QPS</span>
            <span class="stat-label">今日并发峰值</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num" :class="tokenPercentage >= quotas.tokenWarningThreshold ? (tokenPercentage >= 100 ? 'text-danger' : 'text-warning') : 'text-info'">
              {{ tokenPercentage >= 100 ? '超额阻断' : (tokenPercentage >= quotas.tokenWarningThreshold ? '水位预警' : '配额正常') }}
            </span>
            <span class="stat-label">算力配额健康状态</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 顶部控制条与租户上下文 -->
      <div class="quota-top-bar">
        <div class="context-info">
          <el-icon><School /></el-icon>
          <span class="tenant-title">当前租户大盘：{{ tenantStore.activeTenantName }} ({{ tenantStore.activeCampusName }})</span>
          <el-tag size="small" type="success" effect="plain">企业教育旗舰版</el-tag>
        </div>
        <div class="action-buttons">
          <el-button plain :loading="refreshing" @click="handleRefreshAll">
            <el-icon><Refresh /></el-icon>
            <span>刷新算力大盘</span>
          </el-button>
          <el-button type="primary" class="gradient-btn" @click="openConfigModal">
            <el-icon><Setting /></el-icon>
            <span>租户全局配额策略</span>
          </el-button>
        </div>
      </div>

      <!-- 四大核心配额卡片矩阵 -->
      <div class="telemetry-grid">
        <!-- 1. Token 配额 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box token"><el-icon><Coin /></el-icon></div>
              <div class="title-meta">
                <h4>AI Token 算力池总量</h4>
                <span class="sub">周期：按自然月度清零重置</span>
              </div>
            </div>
            <el-tag :type="tokenPercentage >= quotas.tokenWarningThreshold ? (tokenPercentage >= 100 ? 'danger' : 'warning') : 'primary'" size="small">
              {{ tokenPercentage }}% 已消耗
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ (quotas.tokenUsed / 10000).toFixed(1) }}万</span>
              <span class="total-val">/ {{ (quotas.tokenLimit / 10000).toFixed(0) }}万 Tokens</span>
            </div>
            <el-progress
              :percentage="Math.min(100, tokenPercentage)"
              :color="isTokenWarning ? (tokenPercentage >= 100 ? '#EF4444' : '#F59E0B') : '#2563EB'"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>预警水位线：{{ quotas.tokenWarningThreshold }}%</span>
              <span class="est-text" :class="{ 'text-danger': tokenPercentage >= 100 }">
                {{ tokenPercentage >= 100 ? '租户 Token 配额已耗尽，请联系管理员扩容' : (isTokenWarning ? '建议尽快扩容' : '余量充足') }}
              </span>
            </div>
          </div>
        </div>

        <!-- 2. 向量库与知识存储 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box storage"><el-icon><PieChart /></el-icon></div>
              <div class="title-meta">
                <h4>Milvus 向量知识库存储</h4>
                <span class="sub">包含切片索引与元数据存储</span>
              </div>
            </div>
            <el-tag type="success" size="small">
              {{ storagePercentage }}% 容量
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ quotas.storageUsed }} MB</span>
              <span class="total-val">/ {{ quotas.storageLimit }} MB</span>
            </div>
            <el-progress
              :percentage="storagePercentage"
              color="#10B981"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>预估建立切片：{{ (quotas.storageUsed * 332).toLocaleString() }} 条</span>
              <span class="est-text">存储充足</span>
            </div>
          </div>
        </div>

        <!-- 3. 并发 QPS 峰值 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box qps"><el-icon><TrendCharts /></el-icon></div>
              <div class="title-meta">
                <h4>接口吞吐与 QPS 峰值</h4>
                <span class="sub">单校区速率限制与突发缓冲</span>
              </div>
            </div>
            <el-tag type="warning" size="small">
              最大限制 {{ quotas.qpsLimit }} QPS
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ quotas.qpsPeak }} QPS</span>
              <span class="total-val">/ 限流值 {{ quotas.qpsLimit }} QPS</span>
            </div>
            <el-progress
              :percentage="Math.round((quotas.qpsPeak / quotas.qpsLimit) * 100)"
              color="#F59E0B"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>今日拒绝拦截：0 次</span>
              <span class="est-text text-success">吞吐平稳</span>
            </div>
          </div>
        </div>

        <!-- 4. Agent 并发会话席位 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box concurrency"><el-icon><Cpu /></el-icon></div>
              <div class="title-meta">
                <h4>Agent 并发会话席位</h4>
                <span class="sub">支持多工作流同时执行</span>
              </div>
            </div>
            <el-tag type="info" size="small">
              {{ quotas.concurrencyUsed }} / {{ quotas.concurrencyLimit }} 席位
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ quotas.concurrencyUsed }} 席</span>
              <span class="total-val">/ 额定 {{ quotas.concurrencyLimit }} 席位</span>
            </div>
            <el-progress
              :percentage="Math.round((quotas.concurrencyUsed / quotas.concurrencyLimit) * 100)"
              color="#8B5CF6"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>空闲可用席位：{{ quotas.concurrencyLimit - quotas.concurrencyUsed }}</span>
              <span class="est-text">弹性扩容支持</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 核心主体区：算力与配额多维智能管控中心 -->
      <div class="compute-quota-management-card">
        <!-- 顶部 Tab 导航：严格围绕“算力与配额”进行划分 -->
        <div class="quota-tabs-header">
          <div class="tabs-nav-group">
            <button
              type="button"
              class="tab-nav-btn"
              :class="{ active: activeTab === 'allocation' }"
              @click="activeTab = 'allocation'"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span>校区与院系配额切分大盘</span>
              <span class="tab-badge">{{ deptQuotaList.length }}</span>
            </button>
            <button
              type="button"
              class="tab-nav-btn"
              :class="{ active: activeTab === 'ledger' }"
              @click="activeTab = 'ledger'"
            >
              <el-icon><Files /></el-icon>
              <span>算力消耗与配额抵扣流水</span>
              <span class="tab-badge">{{ total }}</span>
            </button>
          </div>

          <div class="quota-meta-tag">
            <span class="dot-online"></span>
            <span>算力池实时计费状态：正常入账中</span>
          </div>
        </div>

        <!-- 视角 1: 校区与院系算力配额切分与用量大盘 -->
        <div v-show="activeTab === 'allocation'" class="tab-pane-content">
          <!-- 部门配额统计概览胶囊 -->
          <div class="summary-ribbon">
            <div class="ribbon-item">
              <span class="ribbon-label">总已切分配额</span>
              <span class="ribbon-val text-primary">{{ (totalAllocatedTokens / 10000).toFixed(0) }} <span class="unit">万 Tokens</span></span>
            </div>
            <div class="ribbon-divider"></div>
            <div class="ribbon-item">
              <span class="ribbon-label">部门累计已消耗</span>
              <span class="ribbon-val text-amber">{{ (totalAllocatedUsed / 10000).toFixed(1) }} <span class="unit">万 Tokens</span></span>
            </div>
            <div class="ribbon-divider"></div>
            <div class="ribbon-item">
              <span class="ribbon-label">租户未切分机动池</span>
              <span class="ribbon-val text-emerald">{{ Math.max(0, (quotas.tokenLimit - totalAllocatedTokens) / 10000).toFixed(0) }} <span class="unit">万 Tokens</span></span>
            </div>
            <div class="ribbon-divider"></div>
            <div class="ribbon-item">
              <span class="ribbon-label">预警/超额部门数</span>
              <span class="ribbon-val" :class="warningDeptCount > 0 ? 'text-danger' : 'text-success'">
                {{ warningDeptCount }} <span class="unit">个</span>
              </span>
            </div>
          </div>

          <!-- 部门配额筛选工具栏 -->
          <div class="dept-filter-bar">
            <el-input
              v-model="deptSearchKeyword"
              placeholder="搜索学院/教研组/部门名称"
              clearable
              size="small"
              class="filter-search-input"
              :prefix-icon="Search"
            />
            <el-select
              v-model="deptStatusFilter"
              placeholder="配额健康度"
              clearable
              size="small"
              class="filter-select"
            >
              <el-option label="全部状态" value="" />
              <el-option label="配额正常 (<80%)" value="NORMAL" />
              <el-option label="水位预警 (≥80%)" value="WARNING" />
              <el-option label="超额阻断 (≥100%)" value="EXCEEDED" />
            </el-select>
          </div>

          <!-- 部门配额分配表格 -->
          <div class="table-wrap">
            <el-table
              :data="paginatedDeptQuotaList"
              row-key="orgId"
              stripe
              style="width: 100%;"
              :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '46px' }"
            >
              <el-table-column label="院系/组织机构" min-width="190">
                <template #default="{ row }">
                  <div class="dept-name-cell">
                    <span class="dept-title">{{ row.name }}</span>
                    <div class="sub-dept-meta">
                      <span class="org-type-badge">{{ row.orgTypeLabel }}</span>
                      <span class="campus-sub">{{ row.campusName }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- Token 算力配额与消耗水位 -->
              <el-table-column label="AI Token 算力配额及消耗水位" min-width="220">
                <template #default="{ row }">
                  <div class="quota-progress-cell">
                    <div class="metric-line">
                      <span class="used-metric font-mono">{{ (row.tokenUsed / 10000).toFixed(1) }}万</span>
                      <span class="limit-metric font-mono">/ {{ (row.tokenLimit / 10000).toFixed(0) }}万</span>
                      <span class="pct-badge" :class="row.usagePercent >= 85 ? (row.usagePercent >= 100 ? 'danger' : 'warning') : 'normal'">
                        {{ row.usagePercent }}%
                      </span>
                    </div>
                    <el-progress
                      :percentage="Math.min(100, row.usagePercent)"
                      :color="row.usagePercent >= 85 ? (row.usagePercent >= 100 ? '#EF4444' : '#F59E0B') : '#2563EB'"
                      :stroke-width="7"
                      :show-text="false"
                      style="margin: 6px 0 3px 0;"
                    />
                    <div class="remain-line">
                      <span>剩余：{{ Math.max(0, ((row.tokenLimit - row.tokenUsed) / 10000)).toFixed(1) }}万 Tokens</span>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- 向量知识库配额 -->
              <el-table-column label="向量库存储配额" min-width="140">
                <template #default="{ row }">
                  <div class="sub-quota-item">
                    <span class="sub-val font-mono">{{ row.storageUsed }} MB / {{ row.storageLimit }} MB</span>
                    <el-progress
                      :percentage="Math.min(100, Math.round((row.storageUsed / row.storageLimit) * 100))"
                      color="#10B981"
                      :stroke-width="5"
                      :show-text="false"
                    />
                  </div>
                </template>
              </el-table-column>

              <!-- 并发会话席位 -->
              <el-table-column label="Agent 并发席位" min-width="130">
                <template #default="{ row }">
                  <div class="seats-item font-mono">
                    <span class="active-seats">{{ row.seatsUsed }}</span>
                    <span class="total-seats">/ {{ row.seatsLimit }} 席</span>
                  </div>
                </template>
              </el-table-column>

              <!-- 配额健康状态 -->
              <el-table-column label="配额状态" width="110">
                <template #default="{ row }">
                  <span class="status-pill" :class="row.usagePercent >= 85 ? (row.usagePercent >= 100 ? 'danger' : 'warning') : 'success'">
                    <span class="status-dot"></span>
                    {{ row.usagePercent >= 100 ? '超额阻断' : (row.usagePercent >= 85 ? '水位预警' : '配额正常') }}
                  </span>
                </template>
              </el-table-column>

              <!-- 操作 -->
              <el-table-column label="算力管控" width="120" fixed="right">
                <template #default="{ row }">
                  <button type="button" class="table-action-pill" @click="openAdjustDeptQuota(row)">
                    <el-icon><EditPen /></el-icon>
                    <span>调整配额</span>
                  </button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 分页器（对标用户管理规范） -->
          <div class="pagination-footer">
            <AppPagination
              v-model:page-num="deptPageNum"
              v-model:page-size="deptPageSize"
              :total="filteredDeptQuotaList.length"
              :page-sizes="[10, 20, 50]"
            />
          </div>
        </div>

        <!-- 视角 2: 租户算力消耗与配额抵扣流水账单 -->
        <div v-show="activeTab === 'ledger'" class="tab-pane-content">
          <!-- 筛选控制条：搜索框变小，重置紧贴查询右侧 -->
          <div class="table-header-box">
            <div class="filter-controls-row">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                size="small"
                class="filter-date-picker"
                @change="handleFilterChange"
              />

              <el-select
                v-model="sceneFilter"
                placeholder="算力消费场景"
                clearable
                size="small"
                class="filter-select"
                @change="handleFilterChange"
              >
                <el-option label="全部场景" value="" />
                <el-option label="智能对话 (CHAT)" value="CHAT" />
                <el-option label="知识增强问答 (CHAT_RAG)" value="CHAT_RAG" />
                <el-option label="智能备课 (PREP)" value="PREP" />
                <el-option label="知识库问答 (RAG)" value="RAG" />
                <el-option label="作业批改 (GRADING)" value="GRADING" />
                <el-option label="AI 出题 (QUESTION)" value="question_generate" />
                <el-option label="智能体协作 (AGENT)" value="AGENT" />
                <el-option label="全局智能助手" value="GLOBAL_ASSISTANT" />
              </el-select>

              <el-select
                v-model="modelFilter"
                placeholder="模型计费规格"
                clearable
                size="small"
                class="filter-select"
                @change="handleFilterChange"
              >
                <el-option label="全部模型规格" value="" />
                <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
              </el-select>

              <el-input
                v-model="searchKeyword"
                placeholder="搜索师生/账号/Trace ID"
                clearable
                size="small"
                class="filter-search-input"
                :prefix-icon="Search"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              />

              <!-- 查询与重置按钮并排锁定在一组 -->
              <div class="filter-actions-group">
                <el-button type="primary" size="small" class="action-pill-btn primary" @click="handleSearch">
                  <el-icon><Search /></el-icon>
                  <span>查询</span>
                </el-button>
                <el-button size="small" class="action-pill-btn" @click="handleReset">
                  <el-icon><RefreshRight /></el-icon>
                  <span>重置</span>
                </el-button>
              </div>
            </div>
          </div>

          <!-- 算力抵扣统计胶囊条 -->
          <div class="summary-ribbon">
            <div class="ribbon-item">
              <span class="ribbon-label">本页抵扣 Token 合计</span>
              <span class="ribbon-val text-primary">{{ pageTotalTokens.toLocaleString() }} <span class="unit">toks</span></span>
            </div>
            <div class="ribbon-divider"></div>
            <div class="ribbon-item">
              <span class="ribbon-label">预估算力支出折算</span>
              <span class="ribbon-val text-amber">¥ {{ pageEstimatedCost.toFixed(4) }}</span>
            </div>
            <div class="ribbon-divider"></div>
            <div class="ribbon-item">
              <span class="ribbon-label">租户剩余总算力池</span>
              <span class="ribbon-val text-emerald">{{ (Math.max(0, quotas.tokenLimit - quotas.tokenUsed) / 10000).toFixed(1) }} <span class="unit">万 toks (余 {{ (100 - tokenPercentage).toFixed(1) }}%)</span></span>
            </div>
            <div class="ribbon-divider"></div>
            <div class="ribbon-item">
              <span class="ribbon-label">扣减执行状态</span>
              <span class="ribbon-val text-success">100% 成功扣减</span>
            </div>
          </div>

          <!-- 算力扣减账单流水表格 -->
          <div class="table-wrap">
            <el-table
              v-loading="tableLoading"
              :data="auditLogs"
              stripe
              style="width: 100%;"
              :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '46px' }"
            >
              <!-- 扣减单号 / 时间 -->
              <el-table-column label="扣减单号 / 时间" width="180">
                <template #default="{ row }">
                  <div class="ledger-bill-cell">
                    <span class="bill-no font-mono">{{ row.traceId || `QTA-${row.id}` }}</span>
                    <span class="time-sub font-mono">{{ row.createdAt || '刚刚' }}</span>
                  </div>
                </template>
              </el-table-column>

              <!-- 消费主体 (师生/部门) -->
              <el-table-column label="算力消费主体 (师生)" min-width="170">
                <template #default="{ row }">
                  <div class="caller-profile-cell">
                    <el-avatar :size="30" :src="getUserAvatarUrl(row)" class="user-avatar">
                      <el-icon><User /></el-icon>
                    </el-avatar>
                    <div class="user-meta">
                      <span class="real-name">{{ row.realName || row.username }}</span>
                      <div class="sub-meta">
                        <span class="user-tag">@{{ row.username }}</span>
                        <span class="role-pill" :class="(row.userRole || 'USER').toLowerCase()">
                          {{ getRoleLabel(row.userRole) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- 算力资源类型 -->
              <el-table-column label="算力资源类型" width="130">
                <template #default>
                  <span class="resource-pill">
                    <el-icon><Coin /></el-icon>
                    <span>Token 算力</span>
                  </span>
                </template>
              </el-table-column>

              <!-- 消费场景与规格 -->
              <el-table-column label="消费场景 / 模型规格" min-width="170">
                <template #default="{ row }">
                  <div class="scenario-spec-cell">
                    <span class="scene-pill" :class="getSceneStyleClass(row.scene || row.toolName)">
                      {{ getSceneLabel(row.scene || row.toolName) }}
                    </span>
                    <span class="model-spec font-mono">{{ row.model }}</span>
                  </div>
                </template>
              </el-table-column>

              <!-- 扣减算力额度 -->
              <el-table-column label="本次扣减算力" min-width="150">
                <template #default="{ row }">
                  <div class="token-metric-cell">
                    <span class="deduct-amount font-mono">-{{ row.totalTokens.toLocaleString() }}</span>
                    <div class="token-split font-mono">
                      入: {{ row.promptTokens }} · 出: {{ row.completionTokens }}
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- 算力折算成本 -->
              <el-table-column label="算力折算" width="110">
                <template #default="{ row }">
                  <span class="cost-tag font-mono">¥ {{ row.estimatedCost.toFixed(4) }}</span>
                </template>
              </el-table-column>

              <!-- 扣减状态 -->
              <el-table-column label="扣减状态" width="100">
                <template #default="{ row }">
                  <span class="status-pill" :class="row.status === 'SUCCESS' ? 'success' : 'danger'">
                    <span class="status-dot"></span>
                    {{ row.status === 'SUCCESS' ? '扣减成功' : '超额阻断' }}
                  </span>
                </template>
              </el-table-column>

              <!-- 操作 -->
              <el-table-column label="配额凭单" width="90" fixed="right">
                <template #default="{ row }">
                  <button type="button" class="table-action-pill" @click="viewDetail(row)">
                    <el-icon><View /></el-icon>
                    <span>凭单</span>
                  </button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 分页器（严格对标用户管理规范） -->
          <div class="pagination-footer">
            <AppPagination
              v-model:page-num="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              @change="loadAuditLogs"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗 1：租户全局配额策略配置 -->
    <el-dialog v-model="configDialogVisible" title="配置租户资源全局预警阈值与额度" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="Token 预警阈值水位线 (%)">
          <el-slider v-model="editForm.warningThreshold" :min="50" :max="95" show-input />
          <span class="form-tip">当租户总用量达到该百分比时，系统自动发送平台通知/邮件预警</span>
        </el-form-item>
        <el-form-item label="月度 Token 额度上限 (Tokens)">
          <el-input-number v-model="editForm.tokenLimit" :step="5000000" :min="1000000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="单校区最高 QPS 限流速率">
          <el-input-number v-model="editForm.qpsLimit" :step="10" :min="10" :max="200" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingConfig" @click="saveConfig">保存配额策略</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2：调整部门/院系算力配额 -->
    <el-dialog v-model="adjustDeptModalVisible" :title="`调整算力配额 - ${currentEditingDept?.name || ''}`" width="480px" destroy-on-close>
      <el-form v-if="currentEditingDept" label-position="top">
        <el-form-item label="院系/组织名称">
          <el-input :model-value="currentEditingDept.name" disabled />
        </el-form-item>
        <el-form-item label="AI Token 算力分配额度 (Tokens)">
          <el-input-number v-model="currentEditingDept.tokenLimit" :step="1000000" :min="500000" :max="50000000" style="width: 100%;" />
          <span class="form-tip">当前已消耗：{{ (currentEditingDept.tokenUsed / 10000).toFixed(1) }} 万 Tokens</span>
        </el-form-item>
        <el-form-item label="向量知识库存储配额 (MB)">
          <el-input-number v-model="currentEditingDept.storageLimit" :step="20" :min="10" :max="500" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="Agent 最大并发席位">
          <el-input-number v-model="currentEditingDept.seatsLimit" :step="50" :min="10" :max="1000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="预警水位线阈值 (%)">
          <el-input-number v-model="currentEditingDept.warningThreshold" :step="5" :min="50" :max="95" style="width: 100%;" />
          <span class="form-tip">当该部门已消耗配额达到此水位时触发系统预警</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDeptModalVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingDeptQuota" @click="saveDeptQuotaAdjustment">保存院系配额</el-button>
      </template>
    </el-dialog>

    <!-- 抽屉：算力抵扣凭单详情 -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="算力抵扣与配额消费凭单"
      size="540px"
      destroy-on-close
      class="audit-detail-drawer"
    >
      <div v-if="selectedLog" class="drawer-content">
        <!-- 头部 Trace 概览卡片 -->
        <div class="drawer-hero-card">
          <div class="hero-top">
            <div class="trace-title">
              <span class="label">算力流水单号</span>
              <span class="val font-mono">{{ selectedLog.traceId }}</span>
            </div>
            <button class="copy-pill-btn" @click="copyText(selectedLog.traceId)">
              <el-icon><CopyDocument /></el-icon>
              <span>复制单号</span>
            </button>
          </div>
          <div class="hero-grid">
            <div class="hero-item">
              <span class="hi-label">本次扣除算力</span>
              <span class="hi-val text-primary font-mono">-{{ selectedLog.totalTokens.toLocaleString() }} toks</span>
            </div>
            <div class="hero-item">
              <span class="hi-label">算力折算成本</span>
              <span class="hi-val text-amber font-mono">¥{{ selectedLog.estimatedCost.toFixed(4) }}</span>
            </div>
            <div class="hero-item">
              <span class="hi-label">扣减执行响应</span>
              <span class="hi-val text-emerald font-mono">{{ selectedLog.durationMs }}ms</span>
            </div>
          </div>
        </div>

        <!-- 结构化项：消费主体 -->
        <div class="detail-section">
          <h4 class="sec-heading">
            <el-icon class="sec-icon icon-blue"><User /></el-icon>
            <span>算力消费主体与归属</span>
          </h4>
          <div class="info-list">
            <div class="info-row">
              <span class="k">消费师生</span>
              <div class="v user-flex-item">
                <el-avatar :size="22" :src="getUserAvatarUrl(selectedLog)" class="mr-1" />
                <span class="font-bold">{{ selectedLog.realName || selectedLog.username }} (@{{ selectedLog.username }})</span>
              </div>
            </div>
            <div class="info-row">
              <span class="k">用户 ID / 身份</span>
              <span class="v">ID: {{ selectedLog.userId }} · <span class="role-pill" :class="(selectedLog.userRole || 'USER').toLowerCase()">{{ getRoleLabel(selectedLog.userRole) }}</span></span>
            </div>
            <div v-if="selectedLog.courseId" class="info-row">
              <span class="k">关联课程 ID</span>
              <span class="v font-mono">#{{ selectedLog.courseId }}</span>
            </div>
            <div v-if="selectedLog.conversationId" class="info-row">
              <span class="k">会话 ID</span>
              <span class="v font-mono">{{ selectedLog.conversationId }}</span>
            </div>
          </div>
        </div>

        <!-- 结构化项：算力与模型规格 -->
        <div class="detail-section">
          <h4 class="sec-heading">
            <el-icon class="sec-icon icon-purple"><Cpu /></el-icon>
            <span>模型计费规格与 Token 明细</span>
          </h4>
          <div class="info-list">
            <div class="info-row">
              <span class="k">计费模型</span>
              <span class="v font-mono font-bold">{{ selectedLog.model }}</span>
            </div>
            <div class="info-row">
              <span class="k">消费业务场景</span>
              <span class="v">
                <span class="scene-pill" :class="getSceneStyleClass(selectedLog.scene || selectedLog.toolName)">
                  {{ getSceneLabel(selectedLog.scene || selectedLog.toolName) }}
                </span>
              </span>
            </div>
            <div class="info-row">
              <span class="k">Prompt Token (输入额度)</span>
              <span class="v font-mono">{{ selectedLog.promptTokens }} toks</span>
            </div>
            <div class="info-row">
              <span class="k">Completion Token (输出额度)</span>
              <span class="v font-mono">{{ selectedLog.completionTokens }} toks</span>
            </div>
          </div>
        </div>

        <!-- 原始算力凭据 JSON -->
        <div class="detail-section">
          <div class="json-header">
            <h4 class="sec-heading mb-0">
              <el-icon class="sec-icon icon-amber"><Document /></el-icon>
              <span>算力凭单完整元数据 (JSON)</span>
            </h4>
            <button type="button" class="copy-json-btn" @click="copyText(JSON.stringify(selectedLog, null, 2))">
              <el-icon><CopyDocument /></el-icon>
              <span>复制凭据</span>
            </button>
          </div>
          <pre class="json-code-block"><code>{{ JSON.stringify(selectedLog, null, 2) }}</code></pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  School,
  Refresh,
  Setting,
  Coin,
  PieChart,
  TrendCharts,
  Cpu,
  Search,
  RefreshRight,
  User,
  View,
  CopyDocument,
  Document,
  DataAnalysis,
  Files,
  EditPen
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { listTenantQuotas, updateTenantQuota, listOrgQuotas, updateOrgQuota } from '@/api/system/tenant';
import type { OrgQuotaVO } from '@/types/system/tenant';
import { getAuditLogs, getAvailableAiModels } from '@/api/system/audit';
import type { AIAuditLog } from '@/types/system/audit';
import { useTenantStore } from '@/stores/system/tenant';
import { normalizeAvatarUrl } from '@/utils/format/file';

const tenantStore = useTenantStore();
const loading = ref(false);
const refreshing = ref(false);
const savingConfig = ref(false);
const configDialogVisible = ref(false);

// Tab 切换：'allocation' (院系配额切分) | 'ledger' (算力扣减账单流水)
const activeTab = ref<'allocation' | 'ledger'>('allocation');

// 租户 4 大核心配额
const quotas = ref({
  tokenUsed: 4820000,
  tokenLimit: 10000000,
  tokenUsagePercent: 48,
  tokenWarningThreshold: 85,
  tokenIsWarning: false,
  storageUsed: 1820,
  storageLimit: 5120,
  qpsPeak: 42,
  qpsLimit: 80,
  concurrencyUsed: 16,
  concurrencyLimit: 50
});

// 编辑全局配额表单
const editForm = ref({
  warningThreshold: 85,
  tokenLimit: 10000000,
  qpsLimit: 80
});

// 水位计算
const tokenPercentage = computed(() => {
  if (typeof quotas.value.tokenUsagePercent === 'number') {
    return quotas.value.tokenUsagePercent;
  }
  return quotas.value.tokenLimit > 0 ? Math.round((quotas.value.tokenUsed / quotas.value.tokenLimit) * 100) : 0;
});

const isTokenWarning = computed(() => {
  if (typeof quotas.value.tokenIsWarning === 'boolean') {
    return quotas.value.tokenIsWarning;
  }
  return tokenPercentage.value >= quotas.value.tokenWarningThreshold;
});

const storagePercentage = computed(() => {
  return quotas.value.storageLimit > 0 ? Math.round((quotas.value.storageUsed / quotas.value.storageLimit) * 100) : 0;
});

// ===== 院系与组织机构配额切分大盘数据模型 =====
const deptQuotaList = ref<OrgQuotaVO[]>([]);
const deptSearchKeyword = ref('');
const deptStatusFilter = ref('');
const deptPageNum = ref(1);
const deptPageSize = ref(10);
const savingDeptQuota = ref(false);

const totalAllocatedTokens = computed(() => {
  return deptQuotaList.value.reduce((sum, item) => sum + item.tokenLimit, 0);
});

const totalAllocatedUsed = computed(() => {
  return deptQuotaList.value.reduce((sum, item) => sum + item.tokenUsed, 0);
});

const warningDeptCount = computed(() => {
  return deptQuotaList.value.filter(d => d.usagePercent >= (d.warningThreshold || 85)).length;
});

const filteredDeptQuotaList = computed(() => {
  return deptQuotaList.value.filter(item => {
    if (deptSearchKeyword.value.trim() && !item.name.includes(deptSearchKeyword.value.trim())) {
      return false;
    }
    const threshold = item.warningThreshold || 80;
    if (deptStatusFilter.value === 'NORMAL' && item.usagePercent >= threshold) return false;
    if (deptStatusFilter.value === 'WARNING' && (item.usagePercent < threshold || item.usagePercent >= 100)) return false;
    if (deptStatusFilter.value === 'EXCEEDED' && item.usagePercent < 100) return false;
    return true;
  });
});

const paginatedDeptQuotaList = computed(() => {
  const start = (deptPageNum.value - 1) * deptPageSize.value;
  return filteredDeptQuotaList.value.slice(start, start + deptPageSize.value);
});

// 调整部门配额弹窗
const adjustDeptModalVisible = ref(false);
const currentEditingDept = ref<OrgQuotaVO | null>(null);

function openAdjustDeptQuota(row: OrgQuotaVO) {
  currentEditingDept.value = { ...row };
  adjustDeptModalVisible.value = true;
}

async function saveDeptQuotaAdjustment() {
  if (!currentEditingDept.value) return;
  savingDeptQuota.value = true;
  try {
    await updateOrgQuota({
      orgId: currentEditingDept.value.orgId,
      tokenLimit: currentEditingDept.value.tokenLimit,
      storageLimit: currentEditingDept.value.storageLimit,
      seatsLimit: currentEditingDept.value.seatsLimit,
      warningThreshold: currentEditingDept.value.warningThreshold || 85
    });
    ElMessage.success(`已成功更新【${currentEditingDept.value.name}】的算力配额指标`);
    adjustDeptModalVisible.value = false;
    await fetchOrgQuotas();
  } catch (err: any) {
    ElMessage.error(err?.message || '保存院系配额失败');
  } finally {
    savingDeptQuota.value = false;
  }
}

// ===== 真实 AI 算力扣减账单流水状态 =====
const auditLogs = ref<AIAuditLog[]>([]);
const tableLoading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 筛选参数
const dateRange = ref<[string, string] | null>(null);
const sceneFilter = ref('');
const modelFilter = ref('');
const searchKeyword = ref('');
const modelOptions = ref<string[]>(['DeepSeek-V3-Chat', 'DeepSeek-R1-Reasoning', 'deepseek-v4-flash']);

// 详情抽屉
const detailDrawerVisible = ref(false);
const selectedLog = ref<AIAuditLog | null>(null);

const pageTotalTokens = computed(() => {
  return auditLogs.value.reduce((sum, item) => sum + (item.totalTokens || 0), 0);
});

const pageEstimatedCost = computed(() => {
  return auditLogs.value.reduce((sum, item) => sum + (item.estimatedCost || 0), 0);
});

// 头像辅助
function getUserAvatarUrl(row?: AIAuditLog | null): string {
  if (!row) return '';
  if (row.avatar) {
    const normalized = normalizeAvatarUrl(row.avatar);
    if (normalized) return normalized;
  }
  const seed = row.username || `User_${row.userId}`;
  return `https://api.dicebear.com/7.x/bottts/svg?seed=${seed}&backgroundColor=e0e7ff`;
}

function getRoleLabel(role?: string) {
  if (!role) return '用户';
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[role.toUpperCase()] || role;
}

function getSceneLabel(scene?: string) {
  if (!scene) return '智能对话';
  const s = scene.toUpperCase();
  const map: Record<string, string> = {
    CHAT: '智能对话',
    CHAT_RAG: '知识增强对话',
    PREP: '智能备课',
    RAG: '知识问答',
    GRADING: '作业智能批改',
    QUESTION_GENERATE: 'AI 试卷/题库生成',
    AGENT: '智能体多步执行',
    GLOBAL_ASSISTANT: '全局智能助手'
  };
  return map[s] || scene;
}

function getSceneStyleClass(scene?: string) {
  const s = (scene || '').toUpperCase();
  if (s === 'CHAT' || s === 'CHAT_RAG') return 'badge-chat';
  if (s === 'PREP') return 'badge-prep';
  if (s === 'RAG') return 'badge-rag';
  if (s === 'GRADING') return 'badge-grading';
  if (s === 'QUESTION_GENERATE') return 'badge-question';
  if (s === 'AGENT') return 'badge-agent';
  return 'badge-default';
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success('已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败，请手动选取复制');
  }
}

// 加载可用大模型列表
async function loadAvailableModels() {
  try {
    const models = await getAvailableAiModels();
    if (models && models.length > 0) {
      modelOptions.value = Array.from(new Set([...modelOptions.value, ...models]));
    }
  } catch {
    // Keep fallback list
  }
}

// 加载租户配额核心数据
async function fetchQuotas() {
  try {
    const res = await listTenantQuotas();
    if (res?.data && res.data.length > 0) {
      const tokenItem = res.data.find(q => q.quotaType === 'TOKEN');
      if (tokenItem) {
        quotas.value.tokenUsed = tokenItem.usedValue;
        quotas.value.tokenLimit = tokenItem.limitValue;
        quotas.value.tokenUsagePercent = tokenItem.usagePercent ?? (tokenItem.limitValue > 0 ? Math.round((tokenItem.usedValue / tokenItem.limitValue) * 100) : 0);
        quotas.value.tokenWarningThreshold = tokenItem.warningThreshold;
        quotas.value.tokenIsWarning = tokenItem.isWarning ?? (quotas.value.tokenUsagePercent >= tokenItem.warningThreshold);
      }
      const storageItem = res.data.find(q => q.quotaType === 'STORAGE');
      if (storageItem) {
        quotas.value.storageUsed = storageItem.usedValue;
        quotas.value.storageLimit = storageItem.limitValue;
      }
      const qpsItem = res.data.find(q => q.quotaType === 'QPS');
      if (qpsItem) {
        quotas.value.qpsPeak = qpsItem.usedValue;
        quotas.value.qpsLimit = qpsItem.limitValue;
      }
      const seatsItem = res.data.find(q => q.quotaType === 'SEATS');
      if (seatsItem) {
        quotas.value.concurrencyUsed = seatsItem.usedValue;
        quotas.value.concurrencyLimit = seatsItem.limitValue;
      }
    }
  } catch {
    // Keep baseline default
  }
}

// 加载真实组织机构与持久化院系算力配额
async function fetchOrgQuotas() {
  try {
    const res = await listOrgQuotas();
    if (res?.data && Array.isArray(res.data)) {
      deptQuotaList.value = res.data;
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取组织配额失败');
  }
}

// 加载算力扣减账单流水
async function loadAuditLogs() {
  tableLoading.value = true;
  try {
    let startDate: string | undefined;
    let endDate: string | undefined;
    if (dateRange.value && dateRange.value.length === 2) {
      startDate = dateRange.value[0];
      endDate = dateRange.value[1];
    }

    const res = await getAuditLogs({
      page: pageNum.value,
      pageSize: pageSize.value,
      scene: sceneFilter.value || undefined,
      model: modelFilter.value || undefined,
      keyword: searchKeyword.value.trim() || undefined,
      startDate,
      endDate
    });

    auditLogs.value = res.list;
    total.value = res.total;

    const foundModels = res.list.map(l => l.model).filter(Boolean);
    if (foundModels.length > 0) {
      modelOptions.value = Array.from(new Set([...modelOptions.value, ...foundModels]));
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载算力流水账单失败');
  } finally {
    tableLoading.value = false;
  }
}

async function handleRefreshAll() {
  refreshing.value = true;
  try {
    await Promise.all([fetchQuotas(), fetchOrgQuotas(), loadAuditLogs()]);
    ElMessage.success('租户用量大盘与算力账单已同步');
  } finally {
    refreshing.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadAuditLogs();
}

function handleFilterChange() {
  pageNum.value = 1;
  loadAuditLogs();
}

function handleReset() {
  dateRange.value = null;
  sceneFilter.value = '';
  modelFilter.value = '';
  searchKeyword.value = '';
  pageNum.value = 1;
  loadAuditLogs();
}

function viewDetail(row: AIAuditLog) {
  selectedLog.value = row;
  detailDrawerVisible.value = true;
}

const openConfigModal = () => {
  editForm.value = {
    warningThreshold: quotas.value.tokenWarningThreshold,
    tokenLimit: quotas.value.tokenLimit,
    qpsLimit: quotas.value.qpsLimit
  };
  configDialogVisible.value = true;
};

const saveConfig = async () => {
  try {
    savingConfig.value = true;
    await updateTenantQuota({
      quotaType: 'TOKEN',
      limitValue: editForm.value.tokenLimit,
      warningThreshold: editForm.value.warningThreshold
    });
    quotas.value.tokenLimit = editForm.value.tokenLimit;
    quotas.value.tokenWarningThreshold = editForm.value.warningThreshold;
    quotas.value.qpsLimit = editForm.value.qpsLimit;
    ElMessage.success('全局配额策略已成功应用');
    configDialogVisible.value = false;
  } catch (e: any) {
    ElMessage.error(e.message || '保存全局配额失败');
  } finally {
    savingConfig.value = false;
  }
};

onMounted(async () => {
  loading.value = true;
  try {
    await Promise.all([fetchQuotas(), fetchOrgQuotas(), loadAuditLogs(), loadAvailableModels()]);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped lang="scss">
.tenant-quota-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 14px;
    margin-top: 4px;
    flex-wrap: wrap;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(8px);
      padding: 6px 20px;
      border-radius: 9999px;
      border: 1.5px solid rgba(22, 119, 255, 0.12);
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #1677FF;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
      }

      .stat-num {
        font-size: 18px;
        font-weight: 800;
        line-height: 1;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-danger { color: #EF4444; }
        &.text-info { color: #0284C7; }
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
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .quota-top-bar {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    border: 1px solid #E2E8F0;

    .context-info {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 15px;
      font-weight: 600;
      color: #1E293B;

      .el-icon { color: #2563EB; font-size: 20px; }
    }

    .action-buttons {
      display: flex;
      gap: 12px;

      .gradient-btn {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        border: none;
        border-radius: 10px;
      }
    }
  }

  .telemetry-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 20px;
    width: 100%;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }
    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }

    .telemetry-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 20px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      display: flex;
      flex-direction: column;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 14px;

        .header-left {
          display: flex;
          gap: 12px;

          .icon-box {
            width: 42px;
            height: 42px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;

            &.token { background: #EFF6FF; color: #2563EB; }
            &.storage { background: #ECFDF5; color: #10B981; }
            &.qps { background: #FFFBEB; color: #F59E0B; }
            &.concurrency { background: #F5F3FF; color: #8B5CF6; }
          }

          .title-meta {
            h4 {
              font-size: 14px;
              font-weight: 600;
              color: #0F172A;
              margin: 0;
            }
            .sub {
              font-size: 11px;
              color: #94A3B8;
              margin-top: 2px;
              display: block;
            }
          }
        }
      }

      .card-body {
        .usage-stats {
          display: flex;
          align-items: baseline;
          gap: 6px;

          .used-val {
            font-size: 20px;
            font-weight: 700;
            color: #1E293B;
          }

          .total-val {
            font-size: 12px;
            color: #64748B;
          }
        }

        .card-bottom-info {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #64748B;

          .est-text {
            font-weight: 500;
            &.text-success { color: #10B981; }
          }
        }
      }
    }
  }

  /* 算力与配额核心管控大卡片 */
  .compute-quota-management-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 24px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

    .quota-tabs-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1.5px solid #F1F5F9;
      padding-bottom: 16px;
      margin-bottom: 20px;
      flex-wrap: wrap;
      gap: 12px;

      .tabs-nav-group {
        display: flex;
        align-items: center;
        gap: 10px;

        .tab-nav-btn {
          border: 1px solid #E2E8F0;
          background: #F8FAFC;
          color: #475569;
          font-size: 13.5px;
          font-weight: 600;
          padding: 8px 18px;
          border-radius: 10px;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 8px;
          transition: all 0.2s ease;

          .tab-badge {
            background: #E2E8F0;
            color: #475569;
            font-size: 11px;
            font-weight: 700;
            padding: 1px 7px;
            border-radius: 9999px;
          }

          &:hover {
            background: #EFF6FF;
            color: #2563EB;
            border-color: #BFDBFE;
          }

          &.active {
            background: #2563EB;
            color: #FFFFFF;
            border-color: #2563EB;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);

            .tab-badge {
              background: rgba(255, 255, 255, 0.25);
              color: #FFFFFF;
            }
          }
        }
      }

      .quota-meta-tag {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #64748B;

        .dot-online {
          width: 7px;
          height: 7px;
          border-radius: 50%;
          background: #10B981;
          box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
        }
      }
    }

    .dept-filter-bar {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;

      .filter-search-input {
        width: 220px;
      }
      .filter-select {
        width: 160px;
      }
    }

    .dept-name-cell {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .dept-title {
        font-size: 13.5px;
        font-weight: 600;
        color: #0F172A;
      }

      .sub-dept-meta {
        display: flex;
        align-items: center;
        gap: 6px;

        .org-type-badge {
          font-size: 10.5px;
          font-weight: 600;
          color: #6366F1;
          background: #EEF2FF;
          padding: 1px 6px;
          border-radius: 4px;
        }

        .campus-sub {
          font-size: 11px;
          color: #94A3B8;
        }
      }
    }

    .quota-progress-cell {
      display: flex;
      flex-direction: column;

      .metric-line {
        display: flex;
        align-items: baseline;
        gap: 4px;

        .used-metric {
          font-size: 13px;
          font-weight: 700;
          color: #2563EB;
        }

        .limit-metric {
          font-size: 11.5px;
          color: #64748B;
        }

        .pct-badge {
          margin-left: auto;
          font-size: 11px;
          font-weight: 700;
          padding: 1px 6px;
          border-radius: 9999px;

          &.normal { background: #EFF6FF; color: #2563EB; }
          &.warning { background: #FFFBEB; color: #D97706; }
          &.danger { background: #FEF2F2; color: #DC2626; }
        }
      }

      .remain-line {
        font-size: 11px;
        color: #94A3B8;
      }
    }

    .sub-quota-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .sub-val {
        font-size: 12px;
        color: #334155;
      }
    }

    .seats-item {
      display: flex;
      align-items: baseline;
      gap: 3px;

      .active-seats {
        font-size: 13px;
        font-weight: 700;
        color: #8B5CF6;
      }

      .total-seats {
        font-size: 11px;
        color: #64748B;
      }
    }

    /* 算力流水账单部分 */
    .table-header-box {
      margin-bottom: 16px;

      .filter-controls-row {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .filter-date-picker {
          width: 210px;
          flex-shrink: 0;
        }

        .filter-select {
          width: 130px;
          flex-shrink: 0;
        }

        .filter-search-input {
          width: 175px;
          flex-shrink: 0;
        }

        .filter-actions-group {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;
        }

        .action-pill-btn {
          border-radius: 8px;
          height: 32px;
          padding: 0 12px;
          font-size: 12.5px;
          font-weight: 600;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          white-space: nowrap;

          &.primary {
            background: #2563EB;
            border-color: #2563EB;
            color: #FFFFFF;

            &:hover {
              background: #1D4ED8;
            }
          }
        }
      }
    }

    .ledger-bill-cell {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .bill-no {
        font-size: 12px;
        font-weight: 600;
        color: #1E293B;
      }

      .time-sub {
        font-size: 11px;
        color: #94A3B8;
      }
    }

    .caller-profile-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .user-avatar {
        background: #EFF6FF;
        border: 1px solid #DBEAFE;
        flex-shrink: 0;
      }

      .user-meta {
        display: flex;
        flex-direction: column;
        gap: 1px;

        .real-name {
          font-size: 12.5px;
          font-weight: 600;
          color: #0F172A;
          line-height: 1.2;
        }

        .sub-meta {
          display: flex;
          align-items: center;
          gap: 4px;

          .user-tag {
            font-size: 11px;
            color: #94A3B8;
            font-family: monospace;
          }
        }
      }
    }

    .role-pill {
      font-size: 10px;
      font-weight: 600;
      padding: 1px 5px;
      border-radius: 9999px;
      line-height: 1.2;

      &.admin { background: #FEE2E2; color: #DC2626; }
      &.teacher { background: #EFF6FF; color: #2563EB; }
      &.student { background: #ECFDF5; color: #059669; }
      &.user { background: #F1F5F9; color: #475569; }
    }

    .resource-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 11.5px;
      font-weight: 600;
      color: #2563EB;
      background: #EFF6FF;
      padding: 3px 8px;
      border-radius: 6px;
      border: 1px solid #BFDBFE;

      .el-icon { font-size: 13px; }
    }

    .scenario-spec-cell {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .scene-pill {
        display: inline-block;
        font-size: 11px;
        font-weight: 600;
        padding: 2px 7px;
        border-radius: 9999px;
        width: fit-content;

        &.badge-chat { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
        &.badge-prep { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
        &.badge-rag { background: #F0FDFA; color: #0D9488; border: 1px solid #99F6E4; }
        &.badge-grading { background: #EEF2FF; color: #4F46E5; border: 1px solid #C7D2FE; }
        &.badge-question { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
        &.badge-agent { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
        &.badge-default { background: #F8FAFC; color: #475569; border: 1px solid #E2E8F0; }
      }

      .model-spec {
        font-size: 11px;
        color: #64748B;
      }
    }

    .token-metric-cell {
      display: flex;
      flex-direction: column;
      gap: 1px;

      .deduct-amount {
        font-size: 13.5px;
        font-weight: 700;
        color: #2563EB;
      }

      .token-split {
        font-size: 10.5px;
        color: #94A3B8;
      }
    }

    .cost-tag {
      font-size: 12px;
      font-weight: 600;
      color: #D97706;
    }

    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 11.5px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: 9999px;

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      &.success {
        background: #ECFDF5;
        color: #059669;
        .status-dot { background: #10B981; }
      }

      &.warning {
        background: #FFFBEB;
        color: #D97706;
        .status-dot { background: #F59E0B; }
      }

      &.danger {
        background: #FEF2F2;
        color: #DC2626;
        .status-dot { background: #EF4444; }
      }
    }

    .table-action-pill {
      padding: 4px 12px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 500;
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      color: #2563EB;
      cursor: pointer;
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      &:hover {
        background: #EFF6FF;
        border-color: #BFDBFE;
        color: #1D4ED8;
      }
    }

    /* 轻量统计胶囊条 */
    .summary-ribbon {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 10px;
      padding: 10px 18px;
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 16px;
      flex-wrap: wrap;

      .ribbon-item {
        display: flex;
        align-items: baseline;
        gap: 8px;

        .ribbon-label {
          font-size: 12px;
          color: #64748B;
        }

        .ribbon-val {
          font-size: 14px;
          font-weight: 700;

          &.text-primary { color: #2563EB; }
          &.text-amber { color: #D97706; }
          &.text-emerald { color: #059669; }
          &.text-success { color: #16A34A; }
          &.text-danger { color: #EF4444; }

          .unit {
            font-size: 11px;
            font-weight: 500;
            color: #94A3B8;
            margin-left: 2px;
          }
        }
      }

      .ribbon-divider {
        width: 1px;
        height: 16px;
        background: #CBD5E1;
      }
    }

    .table-wrap {
      width: 100%;
      overflow-x: auto;
    }

    .pagination-footer {
      margin-top: 18px;
      display: flex;
      justify-content: flex-start;
    }
  }

  .form-tip {
    font-size: 11px;
    color: #94A3B8;
    margin-top: 4px;
    display: block;
  }
}

/* 链路详情抽屉样式 */
:deep(.audit-detail-drawer) {
  .el-drawer__header {
    margin-bottom: 16px;
    padding: 18px 24px;
    border-bottom: 1px solid #F1F5F9;
    font-weight: 700;
    font-size: 16px;
    color: #0F172A;
  }

  .el-drawer__body {
    padding: 20px 24px;
    background: #F8FAFC;
  }

  .drawer-content {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .drawer-hero-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;

      .hero-top {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .trace-title {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .label {
            font-size: 11px;
            color: #94A3B8;
          }

          .val {
            font-size: 13.5px;
            font-weight: 600;
            color: #1E293B;
          }
        }

        .copy-pill-btn {
          background: #F1F5F9;
          border: 1px solid #E2E8F0;
          border-radius: 6px;
          padding: 4px 10px;
          font-size: 11px;
          color: #475569;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 4px;

          &:hover {
            background: #EFF6FF;
            color: #2563EB;
            border-color: #BFDBFE;
          }
        }
      }

      .hero-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 10px;
        background: #F8FAFC;
        padding: 10px;
        border-radius: 8px;

        .hero-item {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .hi-label { font-size: 11px; color: #64748B; }
          .hi-val { font-size: 14px; font-weight: 700; }
        }
      }
    }

    .detail-section {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 16px;

      .sec-heading {
        font-size: 13.5px;
        font-weight: 600;
        color: #0F172A;
        margin: 0 0 12px 0;
        display: flex;
        align-items: center;
        gap: 6px;

        .sec-icon {
          font-size: 15px;
          &.icon-blue { color: #2563EB; }
          &.icon-purple { color: #9333EA; }
          &.icon-amber { color: #D97706; }
        }
      }

      .info-list {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .info-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 12.5px;

          .k { color: #64748B; }
          .v {
            color: #1E293B;
            display: flex;
            align-items: center;
          }

          .user-flex-item {
            gap: 6px;
          }
        }
      }

      .json-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .copy-json-btn {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 6px;
          padding: 3px 8px;
          font-size: 11px;
          color: #475569;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 4px;

          &:hover {
            color: #2563EB;
            border-color: #BFDBFE;
          }
        }
      }

      .json-code-block {
        background: #0F172A;
        color: #E2E8F0;
        padding: 12px;
        border-radius: 8px;
        font-size: 11px;
        font-family: monospace;
        max-height: 220px;
        overflow-y: auto;
        margin: 0;
      }
    }
  }
}
</style>
