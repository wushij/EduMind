<template>
  <div class="key-version-page">
    <!-- 顶部高端 Hero Banner -->
    <PageHeroBanner
      title="国密 KMS 密钥管理 · 数据安全与合规中枢"
      subtitle="基于多租户沙箱隔离的国密数据加密密钥（Data Key）与模型凭证全生命周期管理，全面落实 SM4-GCM 认证加密与 Fail-Closed 隐私保护机制"
      background-variant="system"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">国密三级</span>
            <span class="stat-label">商密合规标准</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">SM4-GCM</span>
            <span class="stat-label">128-bit AEAD 认证算法</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ distinctAliases.length || 2 }} 组</span>
            <span class="stat-label">托管密钥核心资产</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">Fail-Closed</span>
            <span class="stat-label">防篡改隐私熔断</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 顶部租户沙箱隔离状态条与全局控制动作 -->
      <div class="kms-top-bar">
        <div class="context-info">
          <div class="shield-badge">
            <el-icon><Lock /></el-icon>
          </div>
          <div class="context-text">
            <span class="tenant-title">当前租户隔离空间：{{ tenantStore.activeTenantName }} ({{ tenantStore.activeCampusName }})</span>
            <span class="tenant-sub">密钥材料严控在内存安全派生，禁止落盘与日志明文输出 · 历史密文按版本无缝向下兼容</span>
          </div>
        </div>
        <div class="action-buttons">
          <el-button type="primary" class="gradient-btn" @click="openCryptoTestModal(activeDataKey || activeModelKey)">
            <el-icon><Connection /></el-icon>
            <span>实机加解密校验</span>
          </el-button>
          <el-button type="primary" plain class="action-pill-btn" @click="openRotateModal()">
            <el-icon><RefreshRight /></el-icon>
            <span>轮换密钥版本</span>
          </el-button>
          <el-button
            round
            :icon="Refresh"
            class="btn-refresh"
            :loading="refreshing"
            @click="handleRefreshAll"
          >
            刷新大盘
          </el-button>
        </div>
      </div>

      <!-- 核心生效主密钥全景卡片矩阵 (互联互通两大核心业务) -->
      <div class="active-keys-grid">
        <!-- 1. 教学业务数据主密钥 (edumind-data-key) -->
        <div class="active-key-card" :class="{ 'is-active': activeDataKey }">
          <div class="card-glow-bg"></div>
          <div class="card-header">
            <div class="header-left">
              <div class="key-icon-box data-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <div class="key-title-meta">
                <div class="title-line">
                  <h4>edumind-data-key</h4>
                  <el-tag size="small" type="primary" effect="plain">教学数据主密钥</el-tag>
                  <span class="active-pulse-badge">
                    <span class="pulse-dot"></span>
                    <span>生效中 v{{ activeDataKey?.keyVersion || 1 }}</span>
                  </span>
                </div>
                <span class="key-desc">保护 AI 长期记忆沙箱、学生错题本、学情认知画像与对话切片高敏数据</span>
              </div>
            </div>
            <div class="header-actions">
              <el-button size="small" type="primary" link @click="openCryptoTestModal(activeDataKey)">
                <el-icon><Connection /></el-icon>
                <span>加解密自检</span>
              </el-button>
              <el-button size="small" type="primary" link @click="openRotateModal('edumind-data-key')">
                <el-icon><RefreshRight /></el-icon>
                <span>轮换版本</span>
              </el-button>
            </div>
          </div>

          <div class="card-body">
            <div class="spec-grid">
              <div class="spec-item">
                <span class="k">加密算法标准</span>
                <span class="v font-semibold text-amber">国密 SM4-GCM</span>
              </div>
              <div class="spec-item">
                <span class="k">当前生效版本</span>
                <span class="v font-mono text-primary font-bold">v{{ activeDataKey?.keyVersion || 1 }}</span>
              </div>
              <div class="spec-item">
                <span class="k">密钥安全指纹</span>
                <div class="v fingerprint-box" @click="copyText(activeDataKey?.keyFingerprint || 'SM4-GCM#7F8A-3C2B')">
                  <span class="font-mono text-xs">{{ activeDataKey?.keyFingerprint || 'SM4-GCM#7F8A-3C2B' }}</span>
                  <el-icon class="copy-icon"><CopyDocument /></el-icon>
                </div>
              </div>
              <div class="spec-item">
                <span class="k">激活生效时间</span>
                <span class="v font-mono text-secondary">{{ activeDataKey?.activatedTime || activeDataKey?.createTime || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 2. AI 模型凭证密钥 (edumind-model-key) -->
        <div class="active-key-card" :class="{ 'is-active': activeModelKey }">
          <div class="card-glow-bg purple"></div>
          <div class="card-header">
            <div class="header-left">
              <div class="key-icon-box model-icon">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="key-title-meta">
                <div class="title-line">
                  <h4>edumind-model-key</h4>
                  <el-tag size="small" type="purple" effect="plain">AI模型凭证密钥</el-tag>
                  <span class="active-pulse-badge purple">
                    <span class="pulse-dot purple"></span>
                    <span>生效中 v{{ activeModelKey?.keyVersion || 1 }}</span>
                  </span>
                </div>
                <span class="key-desc">保护 AI 模型中枢 DeepSeek / OpenAI / Qwen 等大模型 API-Key 根凭证密文存储</span>
              </div>
            </div>
            <div class="header-actions">
              <el-button size="small" type="primary" link @click="openCryptoTestModal(activeModelKey)">
                <el-icon><Connection /></el-icon>
                <span>加解密自检</span>
              </el-button>
              <el-button size="small" type="primary" link @click="openRotateModal('edumind-model-key')">
                <el-icon><RefreshRight /></el-icon>
                <span>轮换版本</span>
              </el-button>
            </div>
          </div>

          <div class="card-body">
            <div class="spec-grid">
              <div class="spec-item">
                <span class="k">加密算法标准</span>
                <span class="v font-semibold text-amber">国密 SM4-GCM</span>
              </div>
              <div class="spec-item">
                <span class="k">当前生效版本</span>
                <span class="v font-mono text-purple font-bold">v{{ activeModelKey?.keyVersion || 1 }}</span>
              </div>
              <div class="spec-item">
                <span class="k">密钥安全指纹</span>
                <div class="v fingerprint-box" @click="copyText(activeModelKey?.keyFingerprint || 'SM4-GCM#8B12-4E90')">
                  <span class="font-mono text-xs">{{ activeModelKey?.keyFingerprint || 'SM4-GCM#8B12-4E90' }}</span>
                  <el-icon class="copy-icon"><CopyDocument /></el-icon>
                </div>
              </div>
              <div class="spec-item">
                <span class="k">激活生效时间</span>
                <span class="v font-mono text-secondary">{{ activeModelKey?.activatedTime || activeModelKey?.createTime || '-' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 密钥全生命周期与合规台账卡片 -->
      <div class="key-management-table-card">
        <!-- 筛选控制条：搜索框紧凑，重置紧贴查询右侧 -->
        <div class="table-header-box">
          <div class="filter-controls-row">
            <el-select
              v-model="aliasFilter"
              placeholder="密钥资产别名"
              clearable
              size="small"
              class="filter-select"
              @change="handleFilterChange"
            >
              <el-option label="全部密钥资产别名" value="" />
              <el-option label="edumind-data-key (教学沙箱数据主密钥)" value="edumind-data-key" />
              <el-option label="edumind-model-key (AI模型中枢凭证密钥)" value="edumind-model-key" />
            </el-select>

            <el-select
              v-model="statusFilter"
              placeholder="版本生命周期状态"
              clearable
              size="small"
              class="filter-select"
              @change="handleFilterChange"
            >
              <el-option label="全部状态" value="" />
              <el-option label="生效中 (ACTIVE)" value="ACTIVE" />
              <el-option label="已弃用归档 (DEPRECATED)" value="DEPRECATED" />
            </el-select>

            <el-input
              v-model="searchKeyword"
              placeholder="搜索密钥别名 / 指纹 / 算法"
              clearable
              size="small"
              class="filter-search-input"
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />

            <!-- 查询与重置按钮并排锁定在一组 -->
            <div class="filter-actions-group">
              <el-button
                type="primary"
                size="small"
                class="action-pill-btn primary"
                :disabled="loading"
                @click="handleSearch"
              >
                <el-icon><Search /></el-icon>
                <span>查询</span>
              </el-button>
              <el-button
                round
                size="small"
                class="btn-refresh"
                :disabled="loading"
                @click="handleReset"
              >
                <el-icon class="mr-1" :class="{ 'is-loading': loading }"><Refresh /></el-icon>
                <span>重置</span>
              </el-button>
            </div>
          </div>

          <!-- 右侧轻量统计提示 -->
          <div class="toolbar-meta">
            <span class="meta-item">共托管 <strong>{{ keyList.length }}</strong> 个密钥版本流水记录</span>
          </div>
        </div>

        <!-- 密钥版本台账数据表格 -->
        <div
          class="table-wrap"
          v-loading="loading"
          element-loading-text="正在检索国密密钥资产流水..."
        >
          <el-table
            :data="paginatedKeyList"
            row-key="id"
            stripe
            style="width: 100%;"
            :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600', fontSize: '13px', height: '46px' }"
          >
            <el-table-column prop="id" label="ID" width="70" align="center" header-align="center" />

            <!-- 密钥别名与应用场景 -->
            <el-table-column label="密钥资产别名 / 保护业务场景" min-width="260">
              <template #default="{ row }">
                <div class="key-alias-cell">
                  <div class="alias-top">
                    <span class="font-mono font-bold alias-title">{{ row.keyAlias }}</span>
                    <el-tag
                      size="small"
                      :type="row.keyAlias?.includes('model') ? 'purple' : 'primary'"
                      effect="plain"
                      class="alias-pill"
                    >
                      {{ row.keyAlias?.includes('model') ? 'AI模型中枢' : '数据沙箱' }}
                    </el-tag>
                  </div>
                  <span class="usage-scope-desc">{{ row.usageScope || '学生错题 / 认知画像 / 对话切片 / 敏感学情' }}</span>
                </div>
              </template>
            </el-table-column>

            <!-- 版本号 -->
            <el-table-column label="版本号" width="90" align="center" header-align="center">
              <template #default="{ row }">
                <div class="version-tag-box">
                  <span class="ver-pill font-mono" :class="row.status === 'ACTIVE' ? 'active' : 'deprecated'">
                    v{{ row.keyVersion }}
                  </span>
                </div>
              </template>
            </el-table-column>

            <!-- 加密算法 -->
            <el-table-column label="国密算法标准" width="140" align="center" header-align="center">
              <template #default="{ row }">
                <el-tag type="warning" size="small" effect="light" class="algorithm-tag">
                  <el-icon class="mr-1"><Lock /></el-icon>
                  <span>{{ row.algorithm }}</span>
                </el-tag>
              </template>
            </el-table-column>

            <!-- 安全指纹 (Fingerprint) -->
            <el-table-column label="派生安全指纹 (Fingerprint)" min-width="210" align="center" header-align="center">
              <template #default="{ row }">
                <div class="table-fingerprint font-mono" @click="copyText(row.keyFingerprint)">
                  <span>{{ row.keyFingerprint }}</span>
                  <el-icon class="copy-icon"><CopyDocument /></el-icon>
                </div>
              </template>
            </el-table-column>

            <!-- 状态 -->
            <el-table-column label="生命周期状态" width="180" align="center" header-align="center">
              <template #default="{ row }">
                <span class="status-pill" :class="row.status === 'ACTIVE' ? 'success' : 'muted'">
                  <span class="status-dot"></span>
                  {{ row.status === 'ACTIVE' ? '生效中 (ACTIVE)' : '已弃用 (DEPRECATED)' }}
                </span>
              </template>
            </el-table-column>

            <!-- 激活生效时间 -->
            <el-table-column label="激活生效时间" width="170" align="center" header-align="center">
              <template #default="{ row }">
                <span class="font-mono text-secondary text-xs">{{ row.activatedTime || row.createTime || '-' }}</span>
              </template>
            </el-table-column>

            <!-- 操作 -->
            <el-table-column label="安全管控" width="230" align="center" header-align="center" fixed="right">
              <template #default="{ row }">
                <div class="table-actions-flex">
                  <button type="button" class="table-action-pill" @click="openCryptoTestModal(row)">
                    <el-icon><Connection /></el-icon>
                    <span>自检</span>
                  </button>
                  <button type="button" class="table-action-pill secondary" @click="openDetailDrawer(row)">
                    <el-icon><Document /></el-icon>
                    <span>详情</span>
                  </button>
                  <button
                    v-if="row.status === 'ACTIVE'"
                    type="button"
                    class="table-action-pill warning"
                    @click="openRotateModal(row.keyAlias)"
                  >
                    <el-icon><RefreshRight /></el-icon>
                    <span>轮换</span>
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 分页器（对标用户管理标准分页交互） -->
        <div class="pagination-footer">
          <AppPagination
            v-model:page-num="pageNum"
            v-model:page-size="pageSize"
            :total="filteredKeyList.length"
            :page-sizes="[10, 20, 50]"
          />
        </div>
      </div>
    </div>

    <!-- 弹窗 1：在线实机国密 SM4-GCM 加解密自检验证 -->
    <el-dialog
      v-model="cryptoTestModalVisible"
      title="国密 SM4-GCM 实机在线加解密实时自检验证"
      width="640px"
      destroy-on-close
      class="crypto-test-dialog"
    >
      <div class="crypto-test-form">
        <!-- 密钥环境概览 -->
        <div class="test-target-capsule">
          <div class="capsule-left">
            <span class="label">测试目标别名：</span>
            <span class="val font-mono font-bold">{{ testForm.keyAlias }}</span>
          </div>
          <div class="capsule-right">
            <span class="label">使用密钥版本：</span>
            <span class="val font-mono text-primary font-bold">v{{ testForm.keyVersion || '最新活跃' }}</span>
            <span class="label ml-2">算法：</span>
            <el-tag size="small" type="warning" effect="light">SM4-GCM</el-tag>
          </div>
        </div>

        <!-- 快速测试样本填充条 -->
        <div class="quick-samples-bar">
          <span class="qs-label">快速加载真实业务测试用例：</span>
          <el-button size="small" plain @click="loadSampleText('student_memory')">
            学生高敏学情记忆明文
          </el-button>
          <el-button size="small" plain @click="loadSampleText('model_key')">
            DeepSeek-V3 API-Key
          </el-button>
        </div>

        <el-form label-position="top">
          <el-form-item label="操作类型">
            <el-radio-group v-model="testForm.operation" size="small" class="operation-radio-group">
              <el-radio-button value="ENCRYPT">
                <el-icon class="mr-1"><Lock /></el-icon>
                <span>执行 SM4-GCM 认证加密</span>
              </el-radio-button>
              <el-radio-button value="DECRYPT">
                <el-icon class="mr-1"><Key /></el-icon>
                <span>执行 SM4-GCM 认证解密</span>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item :label="testForm.operation === 'ENCRYPT' ? '待加密明文 (Plaintext)' : '待解密国密密文 (Base64 Ciphertext)'">
            <el-input
              v-model="testForm.text"
              type="textarea"
              :rows="4"
              :placeholder="testForm.operation === 'ENCRYPT' ? '输入任意待加密敏感明文字符串...' : '输入标准 SM4-GCM 认证加密产生的 Base64 密文...'"
              class="code-textarea font-mono"
            />
          </el-form-item>
        </el-form>

        <!-- 测试结果回显区 -->
        <div v-if="testResult" class="test-result-box" :class="testResult.success ? 'success' : 'failed'">
          <div class="result-header">
            <div class="result-title">
              <el-icon :class="testResult.success ? 'text-success' : 'text-danger'">
                <CircleCheckFilled v-if="testResult.success" />
                <WarningFilled v-else />
              </el-icon>
              <span class="font-bold">{{ testResult.success ? '国密算法执行成功' : '国密防护阻断 (Fail-Closed)' }}</span>
              <span class="duration-badge font-mono">{{ testResult.durationMs }} ms</span>
            </div>
            <el-button
              v-if="testResult.resultText"
              size="small"
              link
              type="primary"
              @click="copyText(testResult.resultText)"
            >
              <el-icon><CopyDocument /></el-icon>
              <span>复制结果</span>
            </el-button>
          </div>
          <p class="result-message">{{ testResult.message }}</p>
          <div v-if="testResult.resultText" class="result-text-preview">
            <pre class="font-mono"><code>{{ testResult.resultText }}</code></pre>
          </div>
          <!-- 若加密成功，提供一键反向填入解密验证快捷按钮 -->
          <div v-if="testResult.success && testForm.operation === 'ENCRYPT'" class="mt-2 text-right">
            <el-button size="small" type="success" plain @click="flipToDecrypt">
              <el-icon><Key /></el-icon>
              <span>一键填入该密文进行反向解密验证</span>
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="cryptoTestModalVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testingCrypto" class="gradient-btn" @click="executeCryptoTest">
          <el-icon><Connection /></el-icon>
          <span>立即执行 {{ testForm.operation === 'ENCRYPT' ? '国密加密' : '国密解密' }}</span>
        </el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2：密钥版本轮换确认弹窗 -->
    <el-dialog
      v-model="rotateDialogVisible"
      title="确认执行国密 KMS 密钥版本递增轮换"
      width="520px"
      destroy-on-close
    >
      <div class="rotate-dialog-body">
        <div class="warning-hero-card">
          <el-icon class="warning-icon"><WarningFilled /></el-icon>
          <div class="warning-text">
            <p class="title">密钥轮换将自动淘汰老版本并派生全新版本</p>
            <p class="desc">
              系统将把当前活跃版本标记为 <code>DEPRECATED</code>，并在租户沙箱中递增生成全新 <code>ACTIVE</code> 密钥。
              历史业务数据仍可按历史版本号平滑解密，新写入数据将即刻采用全新密钥加密。
            </p>
          </div>
        </div>

        <el-form label-position="top" class="mt-3">
          <el-form-item label="待轮换的目标密钥资产别名">
            <el-select v-model="selectedRotateAlias" style="width: 100%;">
              <el-option label="edumind-data-key (教学沙箱数据主密钥)" value="edumind-data-key" />
              <el-option label="edumind-model-key (AI模型中枢凭证密钥)" value="edumind-model-key" />
            </el-select>
          </el-form-item>
          <div class="version-preview-row">
            <span class="vp-label">版本演进预估：</span>
            <span class="font-mono text-muted">v{{ getActiveVerByAlias(selectedRotateAlias) }}</span>
            <el-icon class="vp-arrow"><ArrowRight v-if="false" /></el-icon>
            <span class="vp-symbol">➔</span>
            <span class="font-mono text-success font-bold">v{{ getActiveVerByAlias(selectedRotateAlias) + 1 }} (全新 ACTIVE)</span>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="rotateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rotating" class="gradient-btn" @click="executeRotate">
          确认立即轮换
        </el-button>
      </template>
    </el-dialog>

    <!-- 抽屉：密钥元数据详情与安全指引 -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="国密 KMS 密钥版本元数据与安全合规规范"
      size="560px"
      destroy-on-close
      class="kms-detail-drawer"
    >
      <div v-if="selectedKeyDetail" class="drawer-content">
        <!-- 1. 顶层 Hero 概览卡片 -->
        <div class="drawer-hero-card">
          <div class="hero-top">
            <div class="hero-title-group">
              <div class="key-avatar-box" :class="selectedKeyDetail.keyAlias?.includes('model') ? 'model' : 'data'">
                <el-icon v-if="selectedKeyDetail.keyAlias?.includes('model')"><Cpu /></el-icon>
                <el-icon v-else><Lock /></el-icon>
              </div>
              <div class="title-meta">
                <div class="alias-line">
                  <span class="val font-mono font-bold">{{ selectedKeyDetail.keyAlias }}</span>
                  <span class="alias-pill-badge" :class="selectedKeyDetail.keyAlias?.includes('model') ? 'purple' : 'blue'">
                    {{ selectedKeyDetail.keyAlias?.includes('model') ? 'AI模型中枢凭证' : '教学沙箱数据' }}
                  </span>
                </div>
                <span class="tenant-tag">所属租户空间 · 华东师范大学附属实验学校 (#{{ selectedKeyDetail.tenantId }})</span>
              </div>
            </div>

            <!-- 生效状态呼吸指示灯徽标 -->
            <div class="pulse-status-badge" :class="selectedKeyDetail.status === 'ACTIVE' ? 'active' : 'deprecated'">
              <span class="dot"></span>
              <span>{{ selectedKeyDetail.status === 'ACTIVE' ? '生效中 ACTIVE' : '已归档 DEPRECATED' }}</span>
            </div>
          </div>

          <!-- 3 个关键指标概览卡片 -->
          <div class="hero-grid">
            <div class="hero-item">
              <span class="hi-label">当前版本号</span>
              <span class="hi-val text-primary font-mono font-bold">v{{ selectedKeyDetail.keyVersion }}</span>
            </div>
            <div class="hero-item">
              <span class="hi-label">国密加密算法</span>
              <span class="hi-val text-amber font-mono font-bold">{{ selectedKeyDetail.algorithm }}</span>
            </div>
            <div class="hero-item">
              <span class="hi-label">合规认证标准</span>
              <span class="hi-val text-success font-bold">商密三级 (GM/T)</span>
            </div>
          </div>
        </div>

        <!-- 2. 国密密码学规格参数结构化卡片 -->
        <div class="detail-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-bubble blue"><el-icon><Lock /></el-icon></div>
              <div class="header-text">
                <span class="title">国密密码学规格参数</span>
                <span class="sub">GM/T 0002-2012 / GB/T 32907-2016 规范体系</span>
              </div>
            </div>
          </div>

          <div class="spec-table">
            <div class="spec-row">
              <div class="spec-label">国家商密规范</div>
              <div class="spec-value">
                <span class="spec-pill blue">GB/T 32907-2016</span>
                <span class="spec-note">SM4 分组密码算法行业标准</span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">工作运行模式</div>
              <div class="spec-value">
                <span class="spec-pill cyan">SM4-GCM</span>
                <span class="spec-note">Galois/Counter Mode · 具备认证加密与消息完整性校验</span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">工作密钥长度</div>
              <div class="spec-value">
                <span class="badge-mono font-bold font-mono">128 bits</span>
                <span class="spec-note">(16 bytes 动态安全派生，严禁明文落盘)</span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">初始向量 (IV)</div>
              <div class="spec-value">
                <span class="badge-mono font-bold font-mono">96 bits</span>
                <span class="spec-note">12 bytes 密码学伪随机向量 (与密文封装传输)</span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">认证标签 (Tag)</div>
              <div class="spec-value">
                <span class="badge-mono font-bold font-mono">128 bits</span>
                <span class="spec-note">16 bytes 消息认证码 (防篡改与重放攻击)</span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">安全派生指纹</div>
              <div class="spec-value">
                <div class="fingerprint-copy-box font-mono" @click="copyText(selectedKeyDetail.keyFingerprint)">
                  <span>{{ selectedKeyDetail.keyFingerprint }}</span>
                  <el-icon class="copy-icon"><CopyDocument /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 3. 全链路业务保护范围卡片 -->
        <div class="detail-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-bubble purple"><el-icon><Connection /></el-icon></div>
              <div class="header-text">
                <span class="title">全链路业务保护范围</span>
                <span class="sub">多租户沙箱逻辑与硬件密钥隔离保护边界</span>
              </div>
            </div>
          </div>

          <div class="spec-table">
            <div class="spec-row">
              <div class="spec-label">业务关联保护对象</div>
              <div class="spec-value wrap-pills">
                <span
                  v-for="item in (selectedKeyDetail.usageScope || '学生错题 / 认知画像 / 对话切片 / 敏感学情').split('/')"
                  :key="item"
                  class="scope-pill"
                >
                  {{ item.trim() }}
                </span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">激活生效时间</div>
              <div class="spec-value">
                <span class="font-mono text-secondary">{{ selectedKeyDetail.activatedTime || selectedKeyDetail.createTime || '-' }}</span>
              </div>
            </div>
            <div class="spec-row">
              <div class="spec-label">隐私保护机制</div>
              <div class="spec-value">
                <span class="fail-closed-badge">
                  <el-icon><Lock /></el-icon>
                  <span>Fail-Closed 隐私保护 (认证失败坚决熔断阻断)</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. 原始元数据 JSON 代码卡片 -->
        <div class="detail-card code-card">
          <div class="code-terminal-header">
            <div class="terminal-left">
              <div class="terminal-dots">
                <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span>
              </div>
              <span class="terminal-filename font-mono">key-metadata-v{{ selectedKeyDetail.keyVersion }}.json</span>
            </div>
            <button type="button" class="copy-code-btn" @click="copyText(JSON.stringify(selectedKeyDetail, null, 2))">
              <el-icon><CopyDocument /></el-icon>
              <span>复制 JSON</span>
            </button>
          </div>
          <pre class="terminal-code-block font-mono"><code>{{ JSON.stringify(selectedKeyDetail, null, 2) }}</code></pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Lock,
  Key,
  Refresh,
  RefreshRight,
  Search,
  WarningFilled,
  CopyDocument,
  CircleCheckFilled,
  Connection,
  Cpu,
  Document
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import {
  listSecurityKeys,
  rotateSecurityKey,
  testSecurityKeyCrypto
} from '@/api/system/security-key';
import type {
  SecurityKeyVersionVO,
  SecurityKeyCryptoTestResponse
} from '@/types/system/security-key';
import { useTenantStore } from '@/stores/system/tenant';

const tenantStore = useTenantStore();
const loading = ref(false);
const refreshing = ref(false);
const rotating = ref(false);
const testingCrypto = ref(false);

// 筛选字段
const aliasFilter = ref('');
const statusFilter = ref('');
const searchKeyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);

// 数据列表
const keyList = ref<SecurityKeyVersionVO[]>([]);

// 活跃主密钥计算
const activeDataKey = computed(() => {
  return keyList.value.find(k => k.keyAlias === 'edumind-data-key' && k.status === 'ACTIVE') || null;
});

const activeModelKey = computed(() => {
  return keyList.value.find(k => k.keyAlias === 'edumind-model-key' && k.status === 'ACTIVE') || null;
});

const distinctAliases = computed(() => {
  return Array.from(new Set(keyList.value.map(k => k.keyAlias))).filter(Boolean);
});

function getActiveVerByAlias(alias?: string): number {
  if (!alias) return 1;
  const match = keyList.value.find(k => k.keyAlias === alias && k.status === 'ACTIVE');
  return match?.keyVersion || 1;
}

// 筛选后的列表
const filteredKeyList = computed(() => {
  return keyList.value.filter(item => {
    if (aliasFilter.value && item.keyAlias !== aliasFilter.value) {
      return false;
    }
    if (statusFilter.value && item.status !== statusFilter.value) {
      return false;
    }
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const matchAlias = item.keyAlias?.toLowerCase().includes(kw);
      const matchVer = `v${item.keyVersion}`.toLowerCase().includes(kw);
      const matchFp = item.keyFingerprint?.toLowerCase().includes(kw);
      const matchAlgo = item.algorithm?.toLowerCase().includes(kw);
      if (!matchAlias && !matchVer && !matchFp && !matchAlgo) {
        return false;
      }
    }
    return true;
  });
});

// 分页列表
const paginatedKeyList = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value;
  return filteredKeyList.value.slice(start, start + pageSize.value);
});

// 加载全量密钥
async function fetchList(silent = false) {
  if (!silent) {
    loading.value = true;
  }
  try {
    const [res] = await Promise.all([
      listSecurityKeys(aliasFilter.value || undefined),
      !silent ? new Promise((resolve) => setTimeout(resolve, 220)) : Promise.resolve()
    ]);
    if (res?.data && Array.isArray(res.data)) {
      keyList.value = res.data;
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取国密密钥资产列表失败');
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

async function handleRefreshAll() {
  refreshing.value = true;
  try {
    await fetchList(true);
    ElMessage.success('国密 KMS 密钥与安全状态已同步更新');
  } finally {
    refreshing.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchList();
}

function handleFilterChange() {
  pageNum.value = 1;
  fetchList();
}

function handleReset() {
  aliasFilter.value = '';
  statusFilter.value = '';
  searchKeyword.value = '';
  pageNum.value = 1;
  fetchList();
}

// 弹窗 1：在线实机加解密校验
const cryptoTestModalVisible = ref(false);
const testForm = ref<{
  keyAlias: string;
  keyVersion?: number;
  operation: 'ENCRYPT' | 'DECRYPT';
  text: string;
}>({
  keyAlias: 'edumind-data-key',
  operation: 'ENCRYPT',
  text: '学生张三（学号 S2026001）高考数学模拟考立体几何弱项诊断画像与个性化辅导策略建议'
});
const testResult = ref<SecurityKeyCryptoTestResponse | null>(null);

function openCryptoTestModal(targetKey?: SecurityKeyVersionVO | null) {
  testForm.value = {
    keyAlias: targetKey?.keyAlias || 'edumind-data-key',
    keyVersion: targetKey?.keyVersion,
    operation: 'ENCRYPT',
    text: targetKey?.keyAlias?.includes('model')
      ? 'sk-deepseek-v3-ai-prod-master-key-credential-918237198237'
      : '学生张三（学号 S2026001）高考数学模拟考立体几何弱项诊断画像与个性化辅导策略建议'
  };
  testResult.value = null;
  cryptoTestModalVisible.value = true;
}

function loadSampleText(type: 'student_memory' | 'model_key') {
  if (type === 'student_memory') {
    testForm.value.keyAlias = 'edumind-data-key';
    testForm.value.operation = 'ENCRYPT';
    testForm.value.text = '学生张三（学号 S2026001）高考数学模拟考立体几何弱项诊断画像与个性化辅导策略建议';
  } else {
    testForm.value.keyAlias = 'edumind-model-key';
    testForm.value.operation = 'ENCRYPT';
    testForm.value.text = 'sk-deepseek-v3-ai-prod-master-key-credential-918237198237';
  }
}

async function executeCryptoTest() {
  if (!testForm.value.text.trim()) {
    ElMessage.warning('请输入测试内容');
    return;
  }
  testingCrypto.value = true;
  testResult.value = null;
  try {
    const res = await testSecurityKeyCrypto({
      keyAlias: testForm.value.keyAlias,
      keyVersion: testForm.value.keyVersion,
      operation: testForm.value.operation,
      text: testForm.value.text.trim()
    });
    testResult.value = res.data;
    if (res.data.success) {
      ElMessage.success(testForm.value.operation === 'ENCRYPT' ? '国密 SM4-GCM 认证加密执行成功' : '国密 SM4-GCM 认证解密还原成功');
    } else {
      ElMessage.warning('执行受阻：Fail-Closed 保护已生效');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '执行自检测试失败');
  } finally {
    testingCrypto.value = false;
  }
}

function flipToDecrypt() {
  if (!testResult.value?.resultText) return;
  testForm.value.operation = 'DECRYPT';
  testForm.value.text = testResult.value.resultText;
  testResult.value = null;
}

// 弹窗 2：密钥版本轮换
const rotateDialogVisible = ref(false);
const selectedRotateAlias = ref('edumind-data-key');

function openRotateModal(alias?: string) {
  selectedRotateAlias.value = alias || 'edumind-data-key';
  rotateDialogVisible.value = true;
}

async function executeRotate() {
  rotating.value = true;
  try {
    const res = await rotateSecurityKey({
      keyAlias: selectedRotateAlias.value
    });
    ElMessage.success(`密钥【${selectedRotateAlias.value}】轮换成功，全新激活版本: v${res.data?.keyVersion || '新'}`);
    rotateDialogVisible.value = false;
    await fetchList();
  } catch (err: any) {
    ElMessage.error(err?.message || '执行密钥轮换失败');
  } finally {
    rotating.value = false;
  }
}

// 抽屉：详情展示
const detailDrawerVisible = ref(false);
const selectedKeyDetail = ref<SecurityKeyVersionVO | null>(null);

function openDetailDrawer(row: SecurityKeyVersionVO) {
  selectedKeyDetail.value = row;
  detailDrawerVisible.value = true;
}

async function copyText(text?: string) {
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success('已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败，请手动选择复制');
  }
}

onMounted(() => {
  fetchList();
});
</script>

<style scoped lang="scss">
.key-version-page {
  position: relative;
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
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #1677FF;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
      }

      .stat-num {
        font-size: 16px;
        font-weight: 700;
        font-family: Consolas, Monaco, monospace;
      }

      .stat-label {
        font-size: 12px;
        color: #64748B;
        font-weight: 500;
      }

      .text-primary { color: #1677FF; }
      .text-success { color: #10B981; }
      .text-warning { color: #F59E0B; }
      .text-info { color: #8B5CF6; }
    }
  }

  .main-content-layout {
    max-width: 1440px;
    margin: -16px auto 0 auto;
    padding: 0 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  // 顶部租户状态条
  .kms-top-bar {
    background: #FFFFFF;
    border-radius: 12px;
    padding: 14px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border: 1px solid #E2E8F0;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);

    .context-info {
      display: flex;
      align-items: center;
      gap: 14px;

      .shield-badge {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        background: rgba(22, 119, 255, 0.08);
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
      }

      .context-text {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .tenant-title {
          font-size: 14px;
          font-weight: 700;
          color: #1E293B;
        }

        .tenant-sub {
          font-size: 12px;
          color: #64748B;
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 10px;
    }
  }

  // 活跃主密钥双卡片矩阵
  .active-keys-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(460px, 1fr));
    gap: 16px;

    .active-key-card {
      position: relative;
      background: #FFFFFF;
      border-radius: 14px;
      padding: 18px 22px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
      overflow: hidden;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        box-shadow: 0 6px 20px rgba(22, 119, 255, 0.08);
        border-color: rgba(22, 119, 255, 0.35);
      }

      .card-glow-bg {
        position: absolute;
        top: -40px;
        right: -40px;
        width: 140px;
        height: 140px;
        background: radial-gradient(circle, rgba(22, 119, 255, 0.12) 0%, rgba(255, 255, 255, 0) 70%);
        border-radius: 50%;
        pointer-events: none;

        &.purple {
          background: radial-gradient(circle, rgba(139, 92, 246, 0.12) 0%, rgba(255, 255, 255, 0) 70%);
        }
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding-bottom: 14px;
        border-bottom: 1px dashed #EDF2F7;

        .header-left {
          display: flex;
          align-items: flex-start;
          gap: 12px;

          .key-icon-box {
            width: 42px;
            height: 42px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            flex-shrink: 0;

            &.data-icon {
              background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
              color: #2563EB;
            }

            &.model-icon {
              background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
              color: #7C3AED;
            }
          }

          .key-title-meta {
            .title-line {
              display: flex;
              align-items: center;
              gap: 8px;
              flex-wrap: wrap;

              h4 {
                margin: 0;
                font-size: 15px;
                font-weight: 700;
                font-family: Consolas, Monaco, monospace;
                color: #0F172A;
              }
            }

            .key-desc {
              display: block;
              margin-top: 4px;
              font-size: 12px;
              color: #64748B;
              line-height: 1.4;
            }
          }
        }

        .header-actions {
          display: flex;
          gap: 4px;
          flex-shrink: 0;
        }
      }

      .card-body {
        padding-top: 14px;

        .spec-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 10px 18px;

          .spec-item {
            display: flex;
            flex-direction: column;
            gap: 3px;

            .k {
              font-size: 11px;
              color: #94A3B8;
              font-weight: 500;
            }

            .algorithm-pill-clean {
              display: inline-flex;
              align-items: center;
              font-weight: 600;
              font-size: 13px;
              color: #D97706;
              background: transparent;
              padding: 0;
              border-radius: 0;
              border: none !important;
              box-shadow: none !important;
              width: fit-content;
            }

            .v {
              font-size: 13px;
              color: #1E293B;

              &.fingerprint-box {
                display: inline-flex;
                align-items: center;
                gap: 6px;
                background: transparent;
                padding: 0;
                border-radius: 0;
                border: none !important;
                box-shadow: none !important;
                cursor: pointer;
                transition: color 0.2s ease;

                &:hover {
                  color: #1677FF;

                  span,
                  .copy-icon {
                    color: #1677FF;
                  }
                }

                .copy-icon {
                  font-size: 12px;
                  color: #94A3B8;
                  transition: color 0.2s ease;
                }
              }
            }
          }
        }
      }
    }
  }

  // 呼吸指示灯
  .active-pulse-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: rgba(16, 185, 129, 0.1);
    color: #10B981;
    padding: 2px 8px;
    border-radius: 9999px;
    font-size: 11px;
    font-weight: 600;

    .pulse-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #10B981;
      box-shadow: 0 0 0 rgba(16, 185, 129, 0.4);
      animation: pulse-green 2s infinite;
    }

    &.purple {
      background: rgba(139, 92, 246, 0.1);
      color: #8B5CF6;

      .pulse-dot.purple {
        background: #8B5CF6;
        animation: pulse-purple 2s infinite;
      }
    }
  }

  @keyframes pulse-green {
    0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.6); }
    70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
    100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
  }

  @keyframes pulse-purple {
    0% { box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.6); }
    70% { box-shadow: 0 0 0 6px rgba(139, 92, 246, 0); }
    100% { box-shadow: 0 0 0 0 rgba(139, 92, 246, 0); }
  }

  // 密钥管理主表格卡片
  .key-management-table-card {
    background: #FFFFFF;
    border-radius: 14px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
    padding: 18px 20px;

    .table-header-box {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;
      flex-wrap: wrap;
      gap: 12px;

      .filter-controls-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .filter-select {
          width: 220px;

          :deep(.el-select__wrapper),
          :deep(.el-input__wrapper) {
            border-radius: 9999px !important;
            padding-left: 14px;
            padding-right: 14px;
            box-shadow: 0 0 0 1px #E2E8F0 inset !important;
            transition: all 0.2s ease;

            &:hover {
              box-shadow: 0 0 0 1px #93C5FD inset !important;
            }

            &.is-focused {
              box-shadow: 0 0 0 1.5px #2563EB inset !important;
            }
          }
        }

        .filter-search-input {
          width: 230px;

          :deep(.el-input__wrapper) {
            border-radius: 9999px !important;
            padding-left: 14px;
            padding-right: 14px;
            box-shadow: 0 0 0 1px #E2E8F0 inset !important;
            transition: all 0.2s ease;

            &:hover {
              box-shadow: 0 0 0 1px #93C5FD inset !important;
            }

            &.is-focus {
              box-shadow: 0 0 0 1.5px #2563EB inset !important;
            }
          }
        }

        .filter-actions-group {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;

          .action-pill-btn {
            border-radius: 9999px !important;
            padding: 0 16px;
            height: 32px;
            font-size: 13px;
            font-weight: 500;
            display: inline-flex;
            align-items: center;
            gap: 5px;
            transition: all 0.2s ease;

            &.primary {
              background: #2563EB;
              border-color: #2563EB;
              color: #FFFFFF;
              box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);

              &:hover {
                background: #1D4ED8;
                border-color: #1D4ED8;
                box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
              }
            }
          }
        }
      }

      .toolbar-meta {
        font-size: 13px;
        color: #64748B;

        strong {
          color: #1E293B;
        }
      }
    }

    .table-wrap {
      border: 1px solid #F1F5F9;
      border-radius: 12px;
      overflow: hidden;

      .key-alias-cell {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .alias-top {
          display: flex;
          align-items: center;
          gap: 8px;

          .alias-title {
            color: #0F172A;
            font-size: 13px;
          }

          .alias-pill {
            border-radius: 9999px;
          }
        }

        .usage-scope-desc {
          font-size: 11px;
          color: #64748B;
        }
      }

      .version-tag-box {
        .ver-pill {
          display: inline-block;
          padding: 2px 10px;
          border-radius: 9999px;
          font-weight: 700;
          font-size: 12px;

          &.active {
            background: #ECFDF5;
            color: #059669;
            border: 1px solid #A7F3D0;
          }

          &.deprecated {
            background: #F1F5F9;
            color: #64748B;
            border: 1px solid #E2E8F0;
          }
        }
      }

      .algorithm-tag {
        font-weight: 600;
        border-radius: 9999px;
        padding: 0 10px;
        border: none !important;
        background: #FEF3C7 !important;
        color: #D97706 !important;
        box-shadow: none !important;
      }

      .table-fingerprint {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: #F1F5F9;
        padding: 3px 10px;
        border-radius: 9999px;
        border: none !important;
        box-shadow: none !important;
        color: #334155;
        font-size: 11px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #E2E8F0;
          color: #2563EB;
        }

        .copy-icon {
          font-size: 11px;
        }
      }

      .status-pill {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        font-size: 12px;
        font-weight: 500;
        padding: 3px 12px;
        border-radius: 9999px;
        white-space: nowrap;

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

        &.muted {
          background: #F1F5F9;
          color: #64748B;
          .status-dot { background: #94A3B8; }
        }
      }

      .table-actions-flex {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        width: 100%;
        white-space: nowrap;
      }

      .table-action-pill {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        height: 28px;
        padding: 0 12px;
        border-radius: 9999px !important;
        font-size: 12px;
        font-weight: 500;
        white-space: nowrap;
        flex-shrink: 0;
        background: #EFF6FF;
        color: #2563EB;
        border: 1px solid #DBEAFE;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #2563EB;
          color: #FFFFFF;
          border-color: #2563EB;
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
        }

        &.secondary {
          background: #F8FAFC;
          color: #475569;
          border-color: #E2E8F0;

          &:hover {
            background: #475569;
            color: #FFFFFF;
            border-color: #475569;
          }
        }

        &.warning {
          background: #FFFBEB;
          color: #D97706;
          border-color: #FDE68A;

          &:hover {
            background: #D97706;
            color: #FFFFFF;
            border-color: #D97706;
          }
        }
      }
    }

    .pagination-footer {
      display: flex;
      justify-content: flex-end;
      padding-top: 14px;
    }
  }

  // 加解密测试弹窗
  .crypto-test-form {
    .test-target-capsule {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 10px 14px;
      margin-bottom: 14px;
      font-size: 13px;

      .label {
        color: #64748B;
      }
    }

    .quick-samples-bar {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 14px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #E2E8F0;
      font-size: 12px;

      .qs-label {
        color: #64748B;
      }
    }

    .code-textarea {
      font-size: 13px;
      line-height: 1.5;
    }

    .test-result-box {
      margin-top: 16px;
      padding: 14px;
      border-radius: 8px;
      border: 1px solid #E2E8F0;

      &.success {
        background: #F0FDF4;
        border-color: #BBF7D0;
      }

      &.failed {
        background: #FEF2F2;
        border-color: #FECACA;
      }

      .result-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .result-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;

          .duration-badge {
            background: rgba(0, 0, 0, 0.05);
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 11px;
            color: #64748B;
          }
        }
      }

      .result-message {
        margin: 6px 0 10px 0;
        font-size: 12px;
        color: #475569;
      }

      .result-text-preview {
        background: #FFFFFF;
        border: 1px solid rgba(0, 0, 0, 0.08);
        border-radius: 6px;
        padding: 10px;
        max-height: 140px;
        overflow-y: auto;

        pre {
          margin: 0;
          font-size: 12px;
          white-space: pre-wrap;
          word-break: break-all;
        }
      }
    }
  }

  // 轮换弹窗
  .rotate-dialog-body {
    .warning-hero-card {
      display: flex;
      gap: 14px;
      background: #FFFBEB;
      border: 1px solid #FDE68A;
      border-radius: 10px;
      padding: 14px;

      .warning-icon {
        font-size: 28px;
        color: #F59E0B;
        flex-shrink: 0;
        margin-top: 2px;
      }

      .warning-text {
        .title {
          font-weight: 700;
          font-size: 14px;
          color: #92400E;
          margin: 0 0 4px 0;
        }

        .desc {
          font-size: 12px;
          color: #B45309;
          margin: 0;
          line-height: 1.5;

          code {
            background: rgba(0, 0, 0, 0.06);
            padding: 1px 5px;
            border-radius: 4px;
            font-weight: 700;
          }
        }
      }
    }

    .version-preview-row {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 10px 14px;
      margin-top: 10px;
      font-size: 13px;

      .vp-label {
        color: #64748B;
      }

      .vp-symbol {
        color: #94A3B8;
        font-size: 14px;
      }
    }
  }

  // 抽屉详情
  .kms-detail-drawer {
    :deep(.el-drawer__header) {
      margin-bottom: 0;
      padding: 16px 20px;
      border-bottom: 1px solid #F1F5F9;
      font-weight: 700;
      color: #0F172A;
      font-size: 15px;
    }

    :deep(.el-drawer__body) {
      padding: 18px 20px;
      background: #F8FAFC;
    }

    .drawer-content {
      display: flex;
      flex-direction: column;
      gap: 16px;

      // 顶部 Hero 卡片
      .drawer-hero-card {
        background: linear-gradient(135deg, #FFFFFF 0%, #F0F7FF 100%);
        border: 1px solid #DBEAFE;
        border-radius: 14px;
        padding: 16px 18px;
        box-shadow: 0 2px 10px rgba(37, 99, 235, 0.04);

        .hero-top {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 14px;
          gap: 12px;

          .hero-title-group {
            display: flex;
            align-items: center;
            gap: 12px;

            .key-avatar-box {
              width: 40px;
              height: 40px;
              border-radius: 10px;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 20px;
              flex-shrink: 0;

              &.data {
                background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
                color: #2563EB;
              }

              &.model {
                background: linear-gradient(135deg, #FAF5FF 0%, #EDE9FE 100%);
                color: #7C3AED;
              }
            }

            .title-meta {
              display: flex;
              flex-direction: column;
              gap: 3px;

              .alias-line {
                display: flex;
                align-items: center;
                gap: 8px;

                .val {
                  font-size: 15px;
                  color: #0F172A;
                }

                .alias-pill-badge {
                  font-size: 11px;
                  font-weight: 600;
                  padding: 1px 8px;
                  border-radius: 9999px;

                  &.blue {
                    background: #DBEAFE;
                    color: #1D4ED8;
                  }

                  &.purple {
                    background: #EDE9FE;
                    color: #6D28D9;
                  }
                }
              }

              .tenant-tag {
                font-size: 11.5px;
                color: #64748B;
              }
            }
          }

          .pulse-status-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: 11.5px;
            font-weight: 600;
            padding: 3px 10px;
            border-radius: 9999px;
            white-space: nowrap;
            flex-shrink: 0;

            .dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
            }

            &.active {
              background: #ECFDF5;
              color: #059669;
              .dot {
                background: #10B981;
                box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
              }
            }

            &.deprecated {
              background: #F1F5F9;
              color: #64748B;
              .dot {
                background: #94A3B8;
              }
            }
          }
        }

        .hero-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 10px;

          .hero-item {
            background: rgba(255, 255, 255, 0.85);
            border: 1px solid rgba(219, 234, 254, 0.8);
            border-radius: 8px;
            padding: 8px 12px;
            display: flex;
            flex-direction: column;
            gap: 3px;

            .hi-label {
              font-size: 11px;
              color: #64748B;
            }

            .hi-val {
              font-size: 14px;
            }
          }
        }
      }

      // 分组卡片
      .detail-card {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 12px;
        padding: 14px 16px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);

        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
          padding-bottom: 10px;
          border-bottom: 1px solid #F1F5F9;

          .header-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .icon-bubble {
              width: 30px;
              height: 30px;
              border-radius: 8px;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 15px;

              &.blue { background: #EFF6FF; color: #2563EB; }
              &.purple { background: #FAF5FF; color: #7C3AED; }
            }

            .header-text {
              display: flex;
              flex-direction: column;
              gap: 1px;

              .title {
                font-size: 13.5px;
                font-weight: 700;
                color: #0F172A;
              }

              .sub {
                font-size: 11px;
                color: #94A3B8;
              }
            }
          }
        }

        .spec-table {
          display: flex;
          flex-direction: column;
          gap: 10px;

          .spec-row {
            display: flex;
            align-items: flex-start;
            font-size: 12.5px;
            line-height: 1.5;

            .spec-label {
              width: 125px;
              flex-shrink: 0;
              color: #64748B;
              font-weight: 500;
              padding-top: 1px;
            }

            .spec-value {
              flex: 1;
              color: #1E293B;
              display: flex;
              align-items: center;
              flex-wrap: wrap;
              gap: 8px;

              .spec-pill {
                font-size: 11.5px;
                font-weight: 700;
                padding: 1px 8px;
                border-radius: 4px;

                &.blue {
                  background: #EFF6FF;
                  color: #1D4ED8;
                }

                &.cyan {
                  background: #ECFEFF;
                  color: #0E7490;
                }
              }

              .badge-mono {
                font-size: 12.5px;
                color: #0F172A;
              }

              .spec-note {
                font-size: 11.5px;
                color: #64748B;
              }

              .fingerprint-copy-box {
                display: inline-flex;
                align-items: center;
                gap: 6px;
                background: #F1F5F9;
                padding: 3px 10px;
                border-radius: 9999px;
                font-size: 11.5px;
                color: #2563EB;
                cursor: pointer;
                transition: all 0.2s;

                &:hover {
                  background: #E2E8F0;
                }

                .copy-icon {
                  font-size: 11px;
                  color: #64748B;
                }
              }

              &.wrap-pills {
                gap: 6px;

                .scope-pill {
                  background: #F8FAFC;
                  border: 1px solid #E2E8F0;
                  color: #334155;
                  font-size: 11.5px;
                  padding: 2px 8px;
                  border-radius: 9999px;
                }
              }

              .fail-closed-badge {
                display: inline-flex;
                align-items: center;
                gap: 6px;
                background: #FEF2F2;
                color: #DC2626;
                padding: 3px 10px;
                border-radius: 9999px;
                font-size: 11.5px;
                font-weight: 600;
              }
            }
          }
        }

        &.code-card {
          padding: 0;
          overflow: hidden;
          background: #0B132B;
          border-color: #1E293B;

          .code-terminal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #111B38;
            padding: 8px 14px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);

            .terminal-left {
              display: flex;
              align-items: center;
              gap: 10px;

              .terminal-dots {
                display: flex;
                align-items: center;
                gap: 5px;

                .dot {
                  width: 9px;
                  height: 9px;
                  border-radius: 50%;

                  &.red { background: #FF5F56; }
                  &.yellow { background: #FFBD2E; }
                  &.green { background: #27C93F; }
                }
              }

              .terminal-filename {
                font-size: 11px;
                color: #94A3B8;
              }
            }

            .copy-code-btn {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              background: rgba(255, 255, 255, 0.08);
              border: 1px solid rgba(255, 255, 255, 0.12);
              padding: 2px 8px;
              border-radius: 9999px;
              font-size: 11px;
              color: #CBD5E1;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                background: rgba(255, 255, 255, 0.16);
                color: #FFFFFF;
              }
            }
          }

          .terminal-code-block {
            background: transparent;
            color: #7DD3FC;
            padding: 12px 16px;
            font-size: 11.5px;
            line-height: 1.6;
            max-height: 240px;
            overflow: auto;
            margin: 0;
          }
        }
      }
    }
  }

  // 通用按钮
  .gradient-btn {
    background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
    border: none;
    font-weight: 600;
    border-radius: 9999px !important;
    padding: 0 18px;
    height: 32px;
    box-shadow: 0 2px 6px rgba(22, 119, 255, 0.25);
    display: inline-flex;
    align-items: center;
    gap: 6px;

    &:hover {
      background: linear-gradient(135deg, #0958D9 0%, #2563EB 100%);
      box-shadow: 0 4px 10px rgba(22, 119, 255, 0.35);
    }
  }

  .action-pill-btn {
    border-radius: 9999px !important;
    font-weight: 500;
    padding: 0 16px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    box-sizing: border-box;

    &.refresh-btn {
      min-width: 104px;
    }
  }

  .font-mono { font-family: Consolas, Monaco, monospace; }
  .text-xs { font-size: 11px; }
  .text-primary { color: #1677FF; }
  .text-success { color: #10B981; }
  .text-warning { color: #F59E0B; }
  .text-danger { color: #EF4444; }
  .text-secondary { color: #64748B; }
  .text-muted { color: #94A3B8; }
  .text-amber { color: #D97706; }
  .text-purple { color: #8B5CF6; }
}
</style>
