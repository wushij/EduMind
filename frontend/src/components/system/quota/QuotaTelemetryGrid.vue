<template>
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
</template>

<script setup lang="ts">
import { Coin, PieChart, TrendCharts, Cpu } from '@element-plus/icons-vue';
import type { TenantQuotasState } from '@/composables/system/useTenantQuota';

defineProps<{
  quotas: TenantQuotasState;
  tokenPercentage: number;
  isTokenWarning: boolean;
  storagePercentage: number;
}>();
</script>
