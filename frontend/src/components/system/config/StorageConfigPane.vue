<template>
  <div class="storage-config-pane">
    <!-- 存储引擎选择网格 (4大引擎快速切换) -->
    <div class="storage-engine-grid">
      <div
        v-for="eng in engineList"
        :key="eng.type"
        class="engine-card"
        :class="{
          selected: selectedEngine === eng.type,
          active: storageForm.activeType === eng.type
        }"
        @click="selectedEngine = eng.type"
      >
        <div class="engine-top">
          <div class="engine-icon-wrap" :class="eng.type">
            <el-icon><component :is="eng.icon" /></el-icon>
          </div>
          <div class="engine-status-tags">
            <el-tag v-if="storageForm.activeType === eng.type" type="success" size="small" effect="dark">
              当前生效
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">
              待激活
            </el-tag>
          </div>
        </div>
        <div class="engine-title">{{ eng.name }}</div>
        <div class="engine-desc">{{ eng.desc }}</div>
      </div>
    </div>

    <!-- 选中存储引擎的参数配置表单 -->
    <el-card shadow="never" class="engine-param-card">
      <template #header>
        <div class="card-header-row">
          <div class="header-left">
            <el-icon class="header-icon"><Setting /></el-icon>
            <span class="card-title">{{ getEngineTitle(selectedEngine) }} 参数配置</span>
          </div>
          <div class="header-actions">
            <el-button
              type="primary"
              plain
              size="small"
              :icon="Connection"
              :loading="testingStorage"
              :disabled="savingStorage"
              @click="handleTestStorage"
            >
              测试当前引擎连通性
            </el-button>
            <el-button
              type="primary"
              size="small"
              :icon="Check"
              :loading="savingStorage"
              :disabled="testingStorage"
              @click="handleSaveStorageConfig"
            >
              保存并热重载存储引擎
            </el-button>
          </div>
        </div>
      </template>

      <!-- 1. 本地存储表单 -->
      <el-form
        v-if="selectedEngine === 'local'"
        label-width="140px"
        class="config-form"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="存储根目录">
          <el-input
            v-model="storageForm.localPath"
            placeholder="例如：backend/data 或 E:/EduMind/backend/data"
          />
          <div class="form-tip">平台文件统一存放在该根目录下，系统根据业务分类创建对应子目录</div>
        </el-form-item>
        <el-form-item label="公网访问域名">
          <el-input
            v-model="storageForm.localDomain"
            placeholder="例如：http://localhost:8080/api/system/files（留空使用默认服务地址）"
          />
          <div class="form-tip">留空时系统默认通过当前后端网关提供静态文件访问与下载</div>
        </el-form-item>
      </el-form>

      <!-- 2. MinIO 存储表单 -->
      <el-form
        v-if="selectedEngine === 'minio'"
        label-width="140px"
        class="config-form"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="服务端点 (Endpoint)">
          <el-input v-model="storageForm.minioEndpoint" placeholder="例如：http://127.0.0.1:9000" />
        </el-form-item>
        <el-form-item label="存储桶 (Bucket)">
          <el-input v-model="storageForm.minioBucket" placeholder="例如：edumind" />
        </el-form-item>
        <el-form-item label="AccessKey">
          <el-input v-model="storageForm.minioAccessKey" placeholder="MinIO 访问密钥" />
        </el-form-item>
        <el-form-item label="SecretKey">
          <el-input
            v-model="storageForm.minioSecretKey"
            type="password"
            autocomplete="new-password"
            :placeholder="storageForm.minioSecretKeyConfigured ? '已配置有效密钥（填入新值修改，保留不变请留空）' : '请输入 SecretKey'"
          />
        </el-form-item>
        <el-form-item label="外网下载域名">
          <el-input v-model="storageForm.minioDomain" placeholder="例如：https://oss.edumind.example.com（选填）" />
        </el-form-item>
      </el-form>

      <!-- 3. 腾讯云 COS 表单 -->
      <el-form
        v-if="selectedEngine === 'cos'"
        label-width="140px"
        class="config-form"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="地域 (Region)">
          <el-input v-model="storageForm.cosRegion" placeholder="例如：ap-guangzhou 或 ap-beijing" />
        </el-form-item>
        <el-form-item label="存储桶 (Bucket)">
          <el-input v-model="storageForm.cosBucket" placeholder="例如：edumind-1250000000" />
        </el-form-item>
        <el-form-item label="SecretId">
          <el-input v-model="storageForm.cosSecretId" placeholder="AKIDxxxxxxxxxxxxxxxxxxxxxxxx" />
        </el-form-item>
        <el-form-item label="SecretKey">
          <el-input
            v-model="storageForm.cosSecretKey"
            type="password"
            autocomplete="new-password"
            :placeholder="storageForm.cosSecretKeyConfigured ? '已配置有效密钥（填入新值修改，保留不变请留空）' : '请输入 SecretKey'"
          />
        </el-form-item>
        <el-form-item label="CDN加速域名">
          <el-input v-model="storageForm.cosDomain" placeholder="例如：https://cos.edumind.example.com（选填）" />
        </el-form-item>
      </el-form>

      <!-- 4. 阿里云 OSS 表单 -->
      <el-form
        v-if="selectedEngine === 'oss'"
        label-width="140px"
        class="config-form"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="地域节点 (Endpoint)">
          <el-input v-model="storageForm.ossEndpoint" placeholder="例如：oss-cn-hangzhou.aliyuncs.com" />
        </el-form-item>
        <el-form-item label="存储空间 (Bucket)">
          <el-input v-model="storageForm.ossBucket" placeholder="例如：edumind-bucket" />
        </el-form-item>
        <el-form-item label="AccessKey ID">
          <el-input v-model="storageForm.ossAccessKeyId" placeholder="LTAIxxxxxxxxxxxxxxxxxxxxxxxx" />
        </el-form-item>
        <el-form-item label="AccessKey Secret">
          <el-input
            v-model="storageForm.ossAccessKeySecret"
            type="password"
            autocomplete="new-password"
            :placeholder="storageForm.ossAccessKeySecretConfigured ? '已配置有效密钥（填入新值修改，保留不变请留空）' : '请输入 AccessKeySecret'"
          />
        </el-form-item>
        <el-form-item label="自定义域名">
          <el-input v-model="storageForm.ossDomain" placeholder="例如：https://oss.edumind.example.com（选填）" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 全局文件上传规格控制 (对齐 wu-admin FileConfigTab) -->
    <el-card shadow="never" class="policy-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">文件上传规格与格式限制</span>
        </div>
      </template>
      <el-form label-width="140px" class="config-form" autocomplete="off" @submit.prevent>
        <el-form-item label="单文件上限">
          <el-input-number
            v-model="storagePolicy.maxSizeMb"
            :min="1"
            :max="1024"
            controls-position="right"
            style="width: 160px"
          />
          <span class="unit">MB（全系统统一文件上传单包体积上限）</span>
        </el-form-item>
        <el-form-item label="允许扩展名">
          <el-input
            v-model="storagePolicy.allowedExtensions"
            type="textarea"
            :rows="3"
            placeholder="逗号分隔，如 jpg,png,pdf,docx,mp4"
          />
          <div class="form-tip">全系统文件上传统一格式白名单；危险可执行扩展名将被底层直接拦截过滤</div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  FolderOpened,
  Coin,
  Cloudy,
  Platform,
  Setting,
  Connection,
  Check
} from '@element-plus/icons-vue';
import {
  fetchStorageConfig,
  saveStorageConfig,
  testStorageConnection,
  fetchFilePolicyConfig,
  saveFilePolicyConfig
} from '@/composables/system/useSystemConfig';
import type { StorageConfigVO, StorageConfigDTO, StorageType } from '@/types/system';

const selectedEngine = ref<StorageType>('local');
const savingStorage = ref(false);
const testingStorage = ref(false);

const engineList = [
  { type: 'local' as StorageType, name: '本地磁盘存储', desc: '服务端本地目录高速读写，开发与单机部署首选', icon: FolderOpened },
  { type: 'minio' as StorageType, name: 'MinIO 对象存储', desc: '私有化部署开源对象存储，兼容 S3 协议规范', icon: Coin },
  { type: 'cos' as StorageType, name: '腾讯云 COS', desc: '腾讯云高可用对象存储，自带全球加速与 CDN', icon: Cloudy },
  { type: 'oss' as StorageType, name: '阿里云 OSS', desc: '阿里云高可靠对象存储，企业级海量资源推荐', icon: Platform },
];

const storagePolicy = reactive({
  maxSizeMb: 50,
  allowedExtensions:
    'jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov',
});

const storageForm = reactive<StorageConfigVO>({
  activeType: 'local',
  localPath: 'backend/data',
  localDomain: '',
  minioEndpoint: 'http://127.0.0.1:9000',
  minioBucket: 'edumind',
  minioAccessKey: '',
  minioSecretKey: '',
  minioSecretKeyConfigured: false,
  minioDomain: '',
  cosRegion: 'ap-guangzhou',
  cosBucket: '',
  cosSecretId: '',
  cosSecretKey: '',
  cosSecretKeyConfigured: false,
  cosDomain: '',
  ossEndpoint: 'oss-cn-hangzhou.aliyuncs.com',
  ossBucket: '',
  ossAccessKeyId: '',
  ossAccessKeySecret: '',
  ossAccessKeySecretConfigured: false,
  ossDomain: ''
});

function getEngineTitle(type: StorageType) {
  switch (type) {
    case 'local':
      return '本地磁盘存储 (Local)';
    case 'minio':
      return 'MinIO 私有化对象存储';
    case 'cos':
      return '腾讯云 COS 对象存储';
    case 'oss':
      return '阿里云 OSS 对象存储';
    default:
      return type;
  }
}

async function loadStorageConfig() {
  try {
    const res = await fetchStorageConfig();
    if (res?.data) {
      Object.assign(storageForm, res.data);
      if (!storageForm.localPath || !storageForm.localPath.trim()) {
        storageForm.localPath = 'backend/data';
      }
      storageForm.minioSecretKey = '';
      storageForm.cosSecretKey = '';
      storageForm.ossAccessKeySecret = '';
      selectedEngine.value = res.data.activeType || 'local';
    }

    try {
      const pRes = await fetchFilePolicyConfig();
      if (pRes?.data?.configValue) {
        const p = JSON.parse(pRes.data.configValue);
        if (p.maxSizeMb) storagePolicy.maxSizeMb = p.maxSizeMb;
        if (p.allowedExtensions) storagePolicy.allowedExtensions = p.allowedExtensions;
      }
    } catch {}
  } catch (err: any) {
    ElMessage.error(err?.message || '获取存储配置失败');
  }
}

async function handleTestStorage() {
  testingStorage.value = true;
  try {
    const payload: StorageConfigDTO = {
      activeType: selectedEngine.value,
      localPath: storageForm.localPath?.trim(),
      localDomain: storageForm.localDomain?.trim(),
      minioEndpoint: storageForm.minioEndpoint?.trim(),
      minioBucket: storageForm.minioBucket?.trim(),
      minioAccessKey: storageForm.minioAccessKey?.trim(),
      minioSecretKey: storageForm.minioSecretKey?.trim() || undefined,
      minioDomain: storageForm.minioDomain?.trim(),
      cosRegion: storageForm.cosRegion?.trim(),
      cosBucket: storageForm.cosBucket?.trim(),
      cosSecretId: storageForm.cosSecretId?.trim(),
      cosSecretKey: storageForm.cosSecretKey?.trim() || undefined,
      cosDomain: storageForm.cosDomain?.trim(),
      ossEndpoint: storageForm.ossEndpoint?.trim(),
      ossBucket: storageForm.ossBucket?.trim(),
      ossAccessKeyId: storageForm.ossAccessKeyId?.trim(),
      ossAccessKeySecret: storageForm.ossAccessKeySecret?.trim() || undefined,
      ossDomain: storageForm.ossDomain?.trim()
    };
    await testStorageConnection(payload);
    ElMessage.success(`【${getEngineTitle(selectedEngine.value)}】连通性探测成功，握手正常！`);
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '存储引擎连通性测试失败');
  } finally {
    testingStorage.value = false;
  }
}

async function handleSaveStorageConfig() {
  savingStorage.value = true;
  try {
    const payload: StorageConfigDTO = {
      activeType: selectedEngine.value,
      localPath: storageForm.localPath?.trim(),
      localDomain: storageForm.localDomain?.trim(),
      minioEndpoint: storageForm.minioEndpoint?.trim(),
      minioBucket: storageForm.minioBucket?.trim(),
      minioAccessKey: storageForm.minioAccessKey?.trim(),
      minioSecretKey: storageForm.minioSecretKey?.trim() || undefined,
      minioDomain: storageForm.minioDomain?.trim(),
      cosRegion: storageForm.cosRegion?.trim(),
      cosBucket: storageForm.cosBucket?.trim(),
      cosSecretId: storageForm.cosSecretId?.trim(),
      cosSecretKey: storageForm.cosSecretKey?.trim() || undefined,
      cosDomain: storageForm.cosDomain?.trim(),
      ossEndpoint: storageForm.ossEndpoint?.trim(),
      ossBucket: storageForm.ossBucket?.trim(),
      ossAccessKeyId: storageForm.ossAccessKeyId?.trim(),
      ossAccessKeySecret: storageForm.ossAccessKeySecret?.trim() || undefined,
      ossDomain: storageForm.ossDomain?.trim()
    };
    await saveStorageConfig(payload);
    try {
      await saveFilePolicyConfig(JSON.stringify(storagePolicy));
    } catch {}
    storageForm.activeType = selectedEngine.value;
    ElMessage.success(`存储配置已保存，系统已热切换为【${getEngineTitle(selectedEngine.value)}】！`);
    await loadStorageConfig();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '保存存储配置失败');
  } finally {
    savingStorage.value = false;
  }
}

defineExpose({
  loadStorageConfig
});

onMounted(() => {
  loadStorageConfig();
});
</script>

<style scoped>
.storage-config-pane {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 6px;
}

.storage-engine-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.engine-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &:hover {
    border-color: #93c5fd;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.06);
  }

  &.selected {
    border-color: #1677ff;
    background: #f8faff;
  }

  &.active {
    border-color: #10b981;
    &.selected {
      border-color: #1677ff;
    }
  }

  .engine-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .engine-icon-wrap {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;

    &.local { background: #eff6ff; color: #1677ff; }
    &.minio { background: #fef3c7; color: #d97706; }
    &.cos { background: #ecfdf5; color: #059669; }
    &.oss { background: #f3e8ff; color: #7c3aed; }
  }

  .engine-title {
    font-size: 14px;
    font-weight: 600;
    color: #1e293b;
  }

  .engine-desc {
    font-size: 12px;
    color: #64748b;
    line-height: 1.4;
  }
}

.engine-param-card,
.policy-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-icon {
      font-size: 16px;
      color: #1677ff;
    }

    .card-title {
      font-size: 14px;
      font-weight: 600;
      color: #1e293b;
    }
  }

  .card-title {
    font-size: 14px;
    font-weight: 600;
    color: #1e293b;
  }

  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.config-form {
  max-width: 680px;
  padding: 8px 0;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.unit {
  margin-left: 10px;
  font-size: 13px;
  color: #909399;
}

@media (max-width: 1024px) {
  .storage-engine-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
