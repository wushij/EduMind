<template>
  <div class="key-version-container">
    <!-- 头部信息卡片 -->
    <el-card shadow="never" class="header-card">
      <div class="header-content">
        <div class="header-left">
          <div class="title-row">
            <el-icon class="title-icon"><Lock /></el-icon>
            <h2 class="page-title">国密 KMS 密钥管理</h2>
            <el-tag type="success" effect="dark" round class="crypto-tag">国密 SM4-GCM</el-tag>
          </div>
          <p class="subtitle">
            基于多租户沙箱隔离的国密数据加密密钥（Data Key）全生命周期管理，支持无缝密钥版本递增轮换与历史密文向下兼容。
          </p>
        </div>
        <div class="header-right">
          <el-button
            type="primary"
            :icon="RefreshRight"
            :loading="rotating"
            @click="handleRotateConfirm"
          >
            轮换当前密钥
          </el-button>
        </div>
      </div>

      <!-- 当前活跃密钥概览 Banner -->
      <div v-if="activeKey" class="active-banner">
        <div class="banner-item">
          <span class="label">当前生效版本：</span>
          <el-tag type="primary" effect="plain" class="ver-tag">v{{ activeKey.keyVersion }}</el-tag>
        </div>
        <div class="banner-item">
          <span class="label">算法类型：</span>
          <span class="value">{{ activeKey.algorithm }}</span>
        </div>
        <div class="banner-item">
          <span class="label">密钥别名：</span>
          <span class="value font-mono">{{ activeKey.keyAlias }}</span>
        </div>
        <div class="banner-item">
          <span class="label">激活生效时间：</span>
          <span class="value">{{ activeKey.activatedTime || activeKey.createTime || '-' }}</span>
        </div>
      </div>
    </el-card>

    <!-- 安全指引 Alert -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="kms-alert"
      title="国密安全合规与 Fail-Closed 策略说明"
      description="系统密钥材料严格在内存中按安全算法派生，禁止落盘与日志明文输出。密钥轮换后，旧版本密文将依据历史版本号无缝解密，若版本不匹配或密文被篡改，系统将触发 Fail-Closed 隐私保护机制拒绝暴露敏感明文。"
    />

    <!-- 列表数据卡片 -->
    <el-card shadow="never" class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="searchAlias"
            placeholder="按密钥别名筛选（默认: edumind-data-key）"
            clearable
            style="width: 320px"
            @keyup.enter="fetchList"
            @clear="fetchList"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="default" :icon="Search" @click="fetchList">查询</el-button>
        </div>
        <div class="toolbar-right">
          <el-button :icon="Refresh" circle @click="fetchList" />
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="keyList"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="keyAlias" label="密钥别名" min-width="180">
          <template #default="{ row }">
            <span class="font-mono">{{ row.keyAlias }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="keyVersion" label="版本号" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'ACTIVE' ? 'success' : 'info'"
              effect="light"
            >
              v{{ row.keyVersion }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="algorithm" label="加密算法" width="160" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.algorithm }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'ACTIVE' ? 'success' : 'info'"
              effect="dark"
            >
              {{ row.status === 'ACTIVE' ? '生效中 (ACTIVE)' : '已弃用 (DEPRECATED)' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activatedTime" label="激活时间" min-width="170">
          <template #default="{ row }">
            {{ row.activatedTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 轮换确认对话框 -->
    <el-dialog
      v-model="rotateDialogVisible"
      title="确认轮换当前国密数据密钥"
      width="480px"
      destroy-on-close
    >
      <div class="rotate-dialog-body">
        <el-icon class="warning-icon"><WarningFilled /></el-icon>
        <div class="warning-text">
          <p><strong>确定要轮换当前的 KMS 密钥吗？</strong></p>
          <p class="desc">
            执行后，当前活跃密钥将被标记为 <code>DEPRECATED</code>，系统将自动生成 
            <code>v{{ (activeKey ? activeKey.keyVersion : 1) + 1 }}</code> 作为全新活跃密钥。
            历史老数据仍可基于原版本正常读取，写入的新高敏数据将直接采用全新密钥加密。
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="rotateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rotating" @click="executeRotate">
          确认轮换
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Lock, Refresh, RefreshRight, Search, WarningFilled } from '@element-plus/icons-vue';
import { listSecurityKeys, getActiveSecurityKey, rotateSecurityKey } from '@/api/system/security-key';
import type { SecurityKeyVersionVO } from '@/types/system/security-key';

const loading = ref(false);
const rotating = ref(false);
const rotateDialogVisible = ref(false);
const searchAlias = ref('edumind-data-key');

const keyList = ref<SecurityKeyVersionVO[]>([]);
const activeKey = ref<SecurityKeyVersionVO | null>(null);

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await listSecurityKeys(searchAlias.value);
    keyList.value = res.data || [];
    // 同步提取当前 ACTIVE 密钥
    activeKey.value = keyList.value.find(k => k.status === 'ACTIVE') || null;
    if (!activeKey.value && keyList.value.length > 0) {
      activeKey.value = keyList.value[0];
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取国密密钥列表失败');
  } finally {
    loading.value = false;
  }
};

const handleRotateConfirm = () => {
  rotateDialogVisible.value = true;
};

const executeRotate = async () => {
  rotating.value = true;
  try {
    const res = await rotateSecurityKey({
      keyAlias: searchAlias.value || 'edumind-data-key'
    });
    ElMessage.success(`密钥轮换成功，新版本号: v${res.data?.keyVersion || '新'}`);
    rotateDialogVisible.value = false;
    await fetchList();
  } catch (err: any) {
    ElMessage.error(err.message || '密钥轮换失败');
  } finally {
    rotating.value = false;
  }
};

onMounted(() => {
  fetchList();
});
</script>

<style scoped lang="scss">
.key-version-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .header-card {
    border-radius: 8px;

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;

      .title-row {
        display: flex;
        align-items: center;
        gap: 10px;

        .title-icon {
          font-size: 22px;
          color: var(--el-color-primary);
        }

        .page-title {
          font-size: 18px;
          font-weight: 600;
          margin: 0;
          color: var(--el-text-color-primary);
        }
      }

      .subtitle {
        margin: 8px 0 0 0;
        font-size: 13px;
        color: var(--el-text-color-secondary);
        line-height: 1.5;
      }
    }

    .active-banner {
      display: flex;
      flex-wrap: wrap;
      gap: 24px;
      padding: 12px 16px;
      background: var(--el-fill-color-light);
      border-radius: 6px;
      border-left: 4px solid var(--el-color-primary);

      .banner-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;

        .label {
          color: var(--el-text-color-secondary);
        }

        .value {
          font-weight: 500;
          color: var(--el-text-color-primary);
        }

        .ver-tag {
          font-weight: 600;
        }
      }
    }
  }

  .kms-alert {
    border-radius: 6px;
  }

  .table-card {
    border-radius: 8px;

    .table-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .toolbar-left {
        display: flex;
        gap: 10px;
      }
    }
  }

  .font-mono {
    font-family: Consolas, Monaco, monospace;
  }

  .rotate-dialog-body {
    display: flex;
    gap: 16px;
    align-items: flex-start;

    .warning-icon {
      font-size: 32px;
      color: var(--el-color-warning);
      flex-shrink: 0;
    }

    .warning-text {
      font-size: 14px;
      line-height: 1.6;

      p {
        margin: 0 0 8px 0;
      }

      .desc {
        color: var(--el-text-color-secondary);
        font-size: 13px;
      }

      code {
        background: var(--el-fill-color-dark);
        padding: 2px 6px;
        border-radius: 4px;
        color: var(--el-color-primary);
        font-family: Consolas, Monaco, monospace;
      }
    }
  }
}
</style>
