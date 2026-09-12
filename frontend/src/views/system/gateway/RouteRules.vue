<template>
  <div class="route-rules-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>网关路由规则</h2>
        <p>按业务场景配置主模型与降级模型</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadRoutes">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoutes">保存配置</el-button>
      </div>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据"
      class="mock-alert"
    />

    <el-card shadow="never" class="table-card">
      <el-table :data="routes" stripe>
        <el-table-column prop="scene" label="业务场景" width="160" />
        <el-table-column label="主模型">
          <template #default="{ row }">
            <el-input v-model="row.primaryModelKey" placeholder="primaryModelKey" />
          </template>
        </el-table-column>
        <el-table-column label="降级模型">
          <template #default="{ row }">
            <el-input v-model="row.fallbackModelKey" placeholder="fallbackModelKey" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { listGatewayRoutes, updateGatewayRoutes } from '@/api/system/gateway';
import { USE_MOCK } from '@/config/mock';
import { MOCK_GATEWAY_ROUTES } from '@/mock/gateway';
import type { GatewayRouteVO } from '@/types/system/gateway';
import { ElMessage } from 'element-plus';

const loading = ref(false);
const saving = ref(false);
const usedMockFallback = ref(false);
const routes = ref<GatewayRouteVO[]>([]);

async function loadRoutes() {
  loading.value = true;
  usedMockFallback.value = false;
  try {
    const res = await listGatewayRoutes();
    routes.value = res.data ?? [];
  } catch {
    if (USE_MOCK) {
      usedMockFallback.value = true;
      routes.value = MOCK_GATEWAY_ROUTES.map((item) => ({ ...item }));
    } else {
      routes.value = [];
      ElMessage.error('加载路由规则失败');
    }
  } finally {
    loading.value = false;
  }
}

async function saveRoutes() {
  saving.value = true;
  try {
    await updateGatewayRoutes(routes.value);
    ElMessage.success('路由规则已保存');
    usedMockFallback.value = false;
  } catch {
    if (USE_MOCK) {
      ElMessage.success('Mock 模式：配置已本地更新');
    } else {
      ElMessage.error('保存路由规则失败');
    }
  } finally {
    saving.value = false;
  }
}

onMounted(loadRoutes);
</script>

<style scoped lang="scss">
.route-rules-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;

    h2 {
      margin: 0 0 6px;
      font-size: 22px;
      font-weight: 700;
      color: #0F172A;
    }

    p {
      margin: 0;
      color: #64748B;
      font-size: 14px;
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .table-card {
    border-radius: 14px;
  }
}
</style>
