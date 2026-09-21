<template>
  <div class="route-rules-page" v-loading="loading">
    <div class="page-top-nav">
      <el-button link class="back-btn" @click="router.push('/system/gateway')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回 AI 网关监控大盘</span>
      </el-button>
    </div>

    <div class="page-header">
      <div class="header-info">
        <div class="header-badges">
          <span class="badge primary">AI 网关核心调度</span>
          <span class="badge success">高可用容灾保障</span>
        </div>
        <h2>网关路由与模型调度规则</h2>
        <p>为平台核心教学业务场景配置主选执行模型与故障自动降级模型，当主模型发生限流、异常或超时时无缝熔断切换</p>
      </div>
      <div class="header-actions">
        <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="loadData">
          刷新
        </el-button>
        <el-button type="primary" class="gradient-btn" :loading="saving" @click="saveRoutes">
          <el-icon><Check /></el-icon>
          <span>保存路由策略</span>
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 示例数据，真实环境将直接同步服务端配置"
      class="mock-alert"
    />

    <el-card shadow="never" class="table-card">
      <div class="table-tip-row">
        <el-icon><InfoFilled /></el-icon>
        <span>建议：主模型选择能力强、推理质量高的大模型（如 deepseek-v4-flash），降级模型选择本地备用或高吞吐低延迟模型（如 mock 或轻量模型）。</span>
      </div>

      <!-- 明确解析顺序，避免"配了却不生效"的误判 -->
      <div class="table-tip-row tip-row--hierarchy">
        <el-icon><InfoFilled /></el-icon>
        <span>
          模型解析顺序：<strong>① 会话内用户选择</strong>（受平台白名单约束）
          → <strong>② 本页场景策略</strong>
          → <strong>③ 平台默认模型</strong>
          <template v-if="defaultModelLabel">（当前：{{ defaultModelLabel }}，仅在本场景未配置或目标不可用时兜底）</template>
          。批改、评测、出题等教学任务不接受用户选择，保证评测口径一致。
        </span>
      </div>

      <el-table :data="routes" stripe style="width: 100%;">
        <el-table-column label="教学业务场景" width="240">
          <template #default="{ row }">
            <div class="scene-cell">
              <span class="scene-title">{{ getSceneName(row.scene) }}</span>
              <span class="scene-code font-mono">{{ row.scene }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="首选主模型 (Primary Model)" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.primaryModelKey"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入模型标识"
              style="width: 100%;"
            >
              <el-option
                v-for="opt in modelOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="自动降级模型 (Fallback Model)" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.fallbackModelKey"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入降级模型"
              style="width: 100%;"
            >
              <el-option
                v-for="opt in modelOptions"
                :key="`${opt.value}-fallback`"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="调度说明" width="220">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">
              {{ row.primaryModelKey === row.fallbackModelKey ? '未启用降级隔离' : '故障自动熔断降级' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ArrowLeft, Refresh, Check, InfoFilled } from '@element-plus/icons-vue';
import { useGatewayRoutes } from '@/composables/system/useGateway';

const {
  router,
  loading,
  saving,
  usedMockFallback,
  routes,
  modelOptions,
  getSceneName,
  loadData,
  saveRoutes
} = useGatewayRoutes();

/** 平台默认模型（is_default）：场景未配置时的兜底，展示出来避免"哪个准"的困惑 */
const defaultModelLabel = computed(
  () => modelOptions.value.find((option) => option.isDefault)?.label ?? ''
);
</script>

<style scoped lang="scss">
.route-rules-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 40px;

  .page-top-nav {
    .back-btn {
      font-size: 13.5px;
      font-weight: 600;
      color: #2563EB;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0;

      &:hover {
        color: #1D4ED8;
      }
    }
  }

  .page-header {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 20px 24px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .header-info {
      .header-badges {
        display: flex;
        gap: 8px;
        margin-bottom: 8px;

        .badge {
          font-size: 11px;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 6px;

          &.primary {
            background: #EFF6FF;
            color: #2563EB;
          }

          &.success {
            background: #F0FDF4;
            color: #16A34A;
          }
        }
      }

      h2 {
        margin: 0 0 6px;
        font-size: 20px;
        font-weight: 800;
        color: #0F172A;
      }

      p {
        margin: 0;
        color: #64748B;
        font-size: 13px;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;

      .gradient-btn {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        border: none;
        border-radius: 8px;
      }
    }
  }

  .table-card {
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 10px 16px 20px;

    .table-tip-row {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;
      border-radius: 8px;
      padding: 10px 14px;
      margin-bottom: 16px;
      font-size: 12.5px;
      color: #475569;

      .el-icon {
        color: #2563EB;
        font-size: 16px;
      }

      /* 解析顺序说明：与"建议"区分开，突出这是生效规则 */
      &.tip-row--hierarchy {
        background: #EFF6FF;
        border-color: #BFDBFE;
        color: #1D4ED8;
        align-items: flex-start;
        line-height: 1.7;

        strong {
          color: #1E40AF;
          font-weight: 700;
        }
      }
    }

    .scene-cell {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .scene-title {
        font-weight: 700;
        color: #1E293B;
        font-size: 13.5px;
      }

      .scene-code {
        font-size: 11px;
        color: #94A3B8;
      }
    }

  }
}
</style>
