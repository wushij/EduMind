<template>
  <div class="storage-config-pane">
    <!-- 存储引擎选择卡片 -->
    <div class="storage-engine-selector-grid">
      <!-- 1. 本地磁盘存储 -->
      <div
        class="engine-card"
        :class="{
          selected: selectedEngine === 'local',
          active: storageForm.activeType === 'local'
        }"
        @click="selectedEngine = 'local'"
      >
        <div class="engine-card-top">
          <div class="engine-icon-box local">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="engine-badges">
            <span v-if="storageForm.activeType === 'local'" class="engine-active-pill">
              <span class="active-dot"></span>当前生效引擎
            </span>
            <span class="engine-tag-pill">本地高性能</span>
          </div>
        </div>
        <div class="engine-card-body">
          <h3 class="engine-title">本地磁盘存储 (Local)</h3>
          <p class="engine-desc">
            文件保存于本地后端服务器目录（如 backend/data），支持分类目录自动分流与本地快速读写。
          </p>
        </div>
        <div class="engine-card-footer">
          <span class="engine-status-text">开发与单机部署首选</span>
          <span class="radio-circle" :class="{ checked: selectedEngine === 'local' }"></span>
        </div>
      </div>

      <!-- 2. MinIO 对象存储 -->
      <div
        class="engine-card"
        :class="{
          selected: selectedEngine === 'minio',
          active: storageForm.activeType === 'minio'
        }"
        @click="selectedEngine = 'minio'"
      >
        <div class="engine-card-top">
          <div class="engine-icon-box minio">
            <el-icon><Coin /></el-icon>
          </div>
          <div class="engine-badges">
            <span v-if="storageForm.activeType === 'minio'" class="engine-active-pill">
              <span class="active-dot"></span>当前生效引擎
            </span>
            <span class="engine-tag-pill">S3标准协议</span>
          </div>
        </div>
        <div class="engine-card-body">
          <h3 class="engine-title">MinIO 私有化对象存储</h3>
          <p class="engine-desc">
            私有化集群部署的高性能开源对象存储，具备高并发高可用特性，完美兼容标准 S3 协议规范。
          </p>
        </div>
        <div class="engine-card-footer">
          <span class="engine-status-text">私有云与高校专网推荐</span>
          <span class="radio-circle" :class="{ checked: selectedEngine === 'minio' }"></span>
        </div>
      </div>

      <!-- 3. 腾讯云 COS -->
      <div
        class="engine-card"
        :class="{
          selected: selectedEngine === 'cos',
          active: storageForm.activeType === 'cos'
        }"
        @click="selectedEngine = 'cos'"
      >
        <div class="engine-card-top">
          <div class="engine-icon-box cos">
            <el-icon><Cloudy /></el-icon>
          </div>
          <div class="engine-badges">
            <span v-if="storageForm.activeType === 'cos'" class="engine-active-pill">
              <span class="active-dot"></span>当前生效引擎
            </span>
            <span class="engine-tag-pill">腾讯云全功能</span>
          </div>
        </div>
        <div class="engine-card-body">
          <h3 class="engine-title">腾讯云 COS 对象存储</h3>
          <p class="engine-desc">
            腾讯云弹性伸缩的对象存储服务，自带全球加速 CDN 与内容分发网络，具备超高可靠性。
          </p>
        </div>
        <div class="engine-card-footer">
          <span class="engine-status-text">公网大规模高可用</span>
          <span class="radio-circle" :class="{ checked: selectedEngine === 'cos' }"></span>
        </div>
      </div>

      <!-- 4. 阿里云 OSS -->
      <div
        class="engine-card"
        :class="{
          selected: selectedEngine === 'oss',
          active: storageForm.activeType === 'oss'
        }"
        @click="selectedEngine = 'oss'"
      >
        <div class="engine-card-top">
          <div class="engine-icon-box oss">
            <el-icon><Platform /></el-icon>
          </div>
          <div class="engine-badges">
            <span v-if="storageForm.activeType === 'oss'" class="engine-active-pill">
              <span class="active-dot"></span>当前生效引擎
            </span>
            <span class="engine-tag-pill">阿里云主流</span>
          </div>
        </div>
        <div class="engine-card-body">
          <h3 class="engine-title">阿里云 OSS 对象存储</h3>
          <p class="engine-desc">
            海量、安全、低成本、高可靠的阿里云对象存储，提供 99.9999999999% 的数据持久性与自动容灾。
          </p>
        </div>
        <div class="engine-card-footer">
          <span class="engine-status-text">企业级海量资源推荐</span>
          <span class="radio-circle" :class="{ checked: selectedEngine === 'oss' }"></span>
        </div>
      </div>
    </div>

    <!-- 参数配置表单与架构说明区域 -->
    <div class="config-grid">
      <!-- 左侧：参数填写 -->
      <div class="config-card">
        <div class="card-header-bar">
          <div class="header-title-group">
            <el-icon class="card-icon"><Operation /></el-icon>
            <h2 class="card-title">{{ getEngineTitle(selectedEngine) }} 参数配置</h2>
          </div>
          <div
            class="status-indicator"
            :class="{ enabled: storageForm.activeType === selectedEngine }"
          >
            <span class="status-dot"></span>
            <span>{{ storageForm.activeType === selectedEngine ? '当前工作引擎' : '待激活/测试引擎' }}</span>
          </div>
        </div>

        <!-- 表单：本地磁盘 -->
        <el-form v-if="selectedEngine === 'local'" label-position="top" class="config-form" autocomplete="off">
          <el-form-item label="文件存储根目录绝对/相对路径">
            <input
              v-model="storageForm.localPath"
              type="text"
              name="edumind_local_storage_path"
              autocomplete="off"
              class="capsule-input"
              placeholder="例如：backend/data 或 E:/EduMind/backend/data"
            />
            <div class="input-hint">
              平台文件将统一归档至该目录下，自动根据资源属性创建子目录（如 avatars/admin/、courses/ 等）。
            </div>
          </el-form-item>

          <el-form-item label="文件静态访问公共域名/前缀 (选填)">
            <input
              v-model="storageForm.localDomain"
              type="text"
              name="edumind_local_storage_domain"
              autocomplete="off"
              class="capsule-input"
              placeholder="例如：http://localhost:8080/api/system/files 或留空使用默认服务地址"
            />
            <div class="input-hint">留空时系统默认通过当前后端网关提供静态文件访问与下载通道。</div>
          </el-form-item>
        </el-form>

        <!-- 表单：MinIO -->
        <el-form v-if="selectedEngine === 'minio'" label-position="top" class="config-form" autocomplete="off">
          <div class="form-cols-two">
            <el-form-item label="MinIO 服务端点 (Endpoint)">
              <input
                v-model="storageForm.minioEndpoint"
                type="text"
                name="minio_service_endpoint"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：http://127.0.0.1:9000"
              />
            </el-form-item>
            <el-form-item label="存储桶名称 (Bucket Name)">
              <input
                v-model="storageForm.minioBucket"
                type="text"
                name="minio_bucket_name"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：edumind"
              />
            </el-form-item>
          </div>

          <div class="form-cols-two">
            <el-form-item label="访问密钥 (AccessKey)">
              <input
                v-model="storageForm.minioAccessKey"
                type="text"
                name="minio_cloud_access_key"
                autocomplete="off"
                class="capsule-input"
                placeholder="请输入 MinIO AccessKey"
              />
            </el-form-item>
            <el-form-item label="安全密钥 (SecretKey)">
              <div class="pwd-input-wrap">
                <!-- 采用 type='text' + secret-masked，100% 杜绝浏览器自动填充账号密码 -->
                <input
                  v-model="storageForm.minioSecretKey"
                  type="text"
                  name="minio_cloud_secret_key"
                  autocomplete="off"
                  class="capsule-input"
                  :class="{ 'secret-masked': !showMinioSecret }"
                  :placeholder="
                    storageForm.minioSecretKeyConfigured
                      ? '已配置有效密钥（填入新值修改，保留不变请留空）'
                      : '请输入 MinIO SecretKey'
                  "
                />
                <button
                  type="button"
                  class="pwd-toggle-btn"
                  @click="showMinioSecret = !showMinioSecret"
                >
                  <el-icon><View v-if="!showMinioSecret" /><Hide v-else /></el-icon>
                </button>
              </div>
            </el-form-item>
          </div>

          <el-form-item label="外网下载 / 自定义域名 (Domain, 选填)">
            <input
              v-model="storageForm.minioDomain"
              type="text"
              name="minio_custom_domain"
              autocomplete="off"
              class="capsule-input"
              placeholder="例如：https://oss.edumind.example.com (留空则默认使用 Endpoint 直连)"
            />
          </el-form-item>
        </el-form>

        <!-- 表单：腾讯云 COS -->
        <el-form v-if="selectedEngine === 'cos'" label-position="top" class="config-form" autocomplete="off">
          <div class="form-cols-two">
            <el-form-item label="所属地域代码 (Region)">
              <input
                v-model="storageForm.cosRegion"
                type="text"
                name="tencent_cos_region"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：ap-guangzhou 或 ap-beijing"
              />
            </el-form-item>
            <el-form-item label="存储桶名称 (Bucket Name)">
              <input
                v-model="storageForm.cosBucket"
                type="text"
                name="tencent_cos_bucket"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：edumind-1250000000"
              />
            </el-form-item>
          </div>

          <div class="form-cols-two">
            <el-form-item label="腾讯云 SecretId">
              <input
                v-model="storageForm.cosSecretId"
                type="text"
                name="tencent_cos_secret_id"
                autocomplete="off"
                class="capsule-input"
                placeholder="AKIDxxxxxxxxxxxxxxxxxxxxxxxx"
              />
            </el-form-item>
            <el-form-item label="腾讯云 SecretKey">
              <div class="pwd-input-wrap">
                <!-- 采用 type='text' + secret-masked，100% 杜绝浏览器自动填充账号密码 -->
                <input
                  v-model="storageForm.cosSecretKey"
                  type="text"
                  name="tencent_cos_secret_key"
                  autocomplete="off"
                  class="capsule-input"
                  :class="{ 'secret-masked': !showCosSecret }"
                  :placeholder="
                    storageForm.cosSecretKeyConfigured
                      ? '已配置有效密钥（填入新值修改，保留不变请留空）'
                      : '请输入 COS SecretKey'
                  "
                />
                <button
                  type="button"
                  class="pwd-toggle-btn"
                  @click="showCosSecret = !showCosSecret"
                >
                  <el-icon><View v-if="!showCosSecret" /><Hide v-else /></el-icon>
                </button>
              </div>
            </el-form-item>
          </div>

          <el-form-item label="CDN 加速或自定义访问域名 (Domain, 选填)">
            <input
              v-model="storageForm.cosDomain"
              type="text"
              name="tencent_cos_custom_domain"
              autocomplete="off"
              class="capsule-input"
              placeholder="例如：https://cos.edumind.example.com"
            />
          </el-form-item>
        </el-form>

        <!-- 表单：阿里云 OSS -->
        <el-form v-if="selectedEngine === 'oss'" label-position="top" class="config-form" autocomplete="off">
          <div class="form-cols-two">
            <el-form-item label="地域节点端点 (Endpoint)">
              <input
                v-model="storageForm.ossEndpoint"
                type="text"
                name="aliyun_oss_endpoint"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：oss-cn-hangzhou.aliyuncs.com"
              />
            </el-form-item>
            <el-form-item label="存储空间名称 (Bucket Name)">
              <input
                v-model="storageForm.ossBucket"
                type="text"
                name="aliyun_oss_bucket"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：edumind-bucket"
              />
            </el-form-item>
          </div>

          <div class="form-cols-two">
            <el-form-item label="AccessKey ID">
              <input
                v-model="storageForm.ossAccessKeyId"
                type="text"
                name="aliyun_oss_access_key_id"
                autocomplete="off"
                class="capsule-input"
                placeholder="LTAIxxxxxxxxxxxxxxxxxxxxxxxx"
              />
            </el-form-item>
            <el-form-item label="AccessKey Secret">
              <div class="pwd-input-wrap">
                <!-- 采用 type='text' + secret-masked，100% 杜绝浏览器自动填充账号密码 -->
                <input
                  v-model="storageForm.ossAccessKeySecret"
                  type="text"
                  name="aliyun_oss_access_key_secret"
                  autocomplete="off"
                  class="capsule-input"
                  :class="{ 'secret-masked': !showOssSecret }"
                  :placeholder="
                    storageForm.ossAccessKeySecretConfigured
                      ? '已配置有效密钥（填入新值修改，保留不变请留空）'
                      : '请输入 OSS AccessKeySecret'
                  "
                />
                <button
                  type="button"
                  class="pwd-toggle-btn"
                  @click="showOssSecret = !showOssSecret"
                >
                  <el-icon><View v-if="!showOssSecret" /><Hide v-else /></el-icon>
                </button>
              </div>
            </el-form-item>
          </div>

          <el-form-item label="自定义加速域名 (Domain, 选填)">
            <input
              v-model="storageForm.ossDomain"
              type="text"
              name="aliyun_oss_custom_domain"
              autocomplete="off"
              class="capsule-input"
              placeholder="例如：https://oss.edumind.example.com"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 右侧：分类目录结构说明 -->
      <div class="config-card">
        <div class="card-header-bar">
          <div class="header-title-group">
            <el-icon class="card-icon"><FolderOpened /></el-icon>
            <h2 class="card-title">平台文件资源统一分类与归档规范</h2>
          </div>
          <span class="badge-pill-soft">自动分流与防覆盖保护</span>
        </div>

        <div class="storage-rules-list">
          <div class="storage-rule-row">
            <div class="rule-icon-box blue">
              <el-icon><User /></el-icon>
            </div>
            <div class="rule-detail">
              <div class="rule-title-line">
                <strong class="title">用户与教师头像资产 (Avatars)</strong>
                <span class="path-tag">avatars/{username}/</span>
              </div>
              <p class="desc">
                用户上传的个人头像按账号隔离归档，上传新头像时自动覆盖旧文件并生成唯一时间戳指纹，杜绝冗余空间堆积。
              </p>
            </div>
          </div>

          <div class="storage-rule-row">
            <div class="rule-icon-box amber">
              <el-icon><VideoCamera /></el-icon>
            </div>
            <div class="rule-detail">
              <div class="rule-title-line">
                <strong class="title">课程大文件与多媒体课件 (Courses)</strong>
                <span class="path-tag">courses/{courseId}/</span>
              </div>
              <p class="desc">
                支持超大视频教学切片、PPT、PDF 课件的秒级上传与流式播放，保障教学资源的高速交付与高并发点播。
              </p>
            </div>
          </div>

          <div class="storage-rule-row">
            <div class="rule-icon-box emerald">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="rule-detail">
              <div class="rule-title-line">
                <strong class="title">RAG 知识库与原始文本资产 (Knowledge)</strong>
                <span class="path-tag">knowledge/documents/{kbId}/</span>
              </div>
              <p class="desc">
                支持 Markdown、Word、PDF 原始文档的存证与版本管理，无缝联动向量数据库切片索引。
              </p>
            </div>
          </div>

          <div class="storage-rule-row">
            <div class="rule-icon-box purple">
              <el-icon><Lightning /></el-icon>
            </div>
            <div class="rule-detail">
              <div class="rule-title-line">
                <strong class="title">内存门面与平滑热重载 (RoutingFacade)</strong>
                <span class="path-tag">零重启服务 / 动态驱动绑定</span>
              </div>
              <p class="desc">
                基于动态路由存储门面模式（RoutingFileStorageService），修改配置保存后，底层驱动实例将在内存中毫秒级热切换。
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="config-actions-bar">
      <div class="actions-left-tip">
        <el-icon class="tip-icon"><InfoFilled /></el-icon>
        <span>
          当前正在配置：<strong>{{ getEngineTitle(selectedEngine) }}</strong>。建议在保存前先执行连通性握手测试，以验证网络通道与鉴权凭证。
        </span>
      </div>
      <div class="actions-right-buttons">
        <button
          type="button"
          class="btn-test-send"
          :disabled="testingStorage || savingStorage"
          @click="handleTestStorage"
        >
          <el-icon class="btn-icon"><Connection /></el-icon>
          <span>{{ testingStorage ? '正在探测连通性...' : '测试选中引擎连通性' }}</span>
        </button>
        <button
          type="button"
          class="btn-save-main"
          :disabled="testingStorage || savingStorage"
          @click="handleSaveStorageConfig"
        >
          <el-icon class="btn-icon"><Check /></el-icon>
          <span>{{ savingStorage ? '正在热切换...' : '保存并切换为当前引擎' }}</span>
        </button>
      </div>
    </div>
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
  Operation,
  View,
  Hide,
  User,
  VideoCamera,
  Cpu,
  Lightning,
  InfoFilled,
  Connection,
  Check
} from '@element-plus/icons-vue';
import {
  getStorageConfig,
  updateStorageConfig,
  testStorageConfig
} from '@/api/system/config';
import type { StorageConfigVO, StorageConfigDTO, StorageType } from '@/types/system';

const selectedEngine = ref<StorageType>('local');
const savingStorage = ref(false);
const testingStorage = ref(false);

const showMinioSecret = ref(false);
const showCosSecret = ref(false);
const showOssSecret = ref(false);

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
    const res = await getStorageConfig();
    if (res?.data) {
      Object.assign(storageForm, res.data);
      if (!storageForm.localPath || !storageForm.localPath.trim()) {
        storageForm.localPath = 'backend/data';
      }
      // 避免浏览器残留污染
      storageForm.minioSecretKey = '';
      storageForm.cosSecretKey = '';
      storageForm.ossAccessKeySecret = '';
      selectedEngine.value = res.data.activeType || 'local';
    }
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
    await testStorageConfig(payload);
    ElMessage.success(`【${getEngineTitle(selectedEngine.value)}】连通性探测成功，握手与读写鉴权完全正常！`);
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '存储引擎连通性测试失败，请检查配置参数');
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
    await updateStorageConfig(payload);
    storageForm.activeType = selectedEngine.value;
    ElMessage.success(`存储配置已成功保存，系统底层已热切换为【${getEngineTitle(selectedEngine.value)}】！`);

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

<style scoped lang="scss">
.storage-config-pane {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .storage-engine-selector-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    .engine-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1.5px solid #E2E8F0;
      padding: 18px 20px;
      cursor: pointer;
      transition: all 0.25s ease;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      position: relative;
      overflow: hidden;

      &:hover {
        transform: translateY(-2px);
        border-color: #93C5FD;
        box-shadow: 0 6px 20px rgba(22, 119, 255, 0.08);
      }

      &.selected {
        border-color: #1677FF;
        background: #F8FAFF;
        box-shadow: 0 4px 18px rgba(22, 119, 255, 0.12);
      }

      &.active {
        border-color: #10B981;
        &.selected {
          border-color: #1677FF;
        }
      }

      .engine-card-top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        margin-bottom: 12px;

        .engine-icon-box {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 22px;

          &.local {
            background: #EFF6FF;
            color: #1D4ED8;
            border: 1px solid #DBEAFE;
          }
          &.minio {
            background: #FFF1F2;
            color: #E11D48;
            border: 1px solid #FFE4E6;
          }
          &.cos {
            background: #F0FDF4;
            color: #16A34A;
            border: 1px solid #DCFCE7;
          }
          &.oss {
            background: #FFF7ED;
            color: #EA580C;
            border: 1px solid #FFEDD5;
          }
        }

        .engine-badges {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          gap: 6px;

          .engine-active-pill {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 9px;
            border-radius: 9999px;
            background: #ECFDF5;
            color: #059669;
            font-size: 11px;
            font-weight: 600;
            border: 1px solid #A7F3D0;

            .active-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10B981;
            }
          }

          .engine-tag-pill {
            padding: 2px 8px;
            border-radius: 6px;
            background: #F1F5F9;
            color: #64748B;
            font-size: 11px;
          }
        }
      }

      .engine-card-body {
        margin-bottom: 14px;

        .engine-title {
          margin: 0 0 6px;
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
        }

        .engine-desc {
          margin: 0;
          font-size: 12px;
          color: #64748B;
          line-height: 1.5;
        }
      }

      .engine-card-footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-top: 10px;
        border-top: 1px solid #F1F5F9;

        .engine-status-text {
          font-size: 11.5px;
          color: #94A3B8;
        }

        .radio-circle {
          width: 16px;
          height: 16px;
          border-radius: 50%;
          border: 1.5px solid #CBD5E1;
          position: relative;
          transition: all 0.2s;

          &.checked {
            border-color: #1677FF;
            background: #1677FF;

            &::after {
              content: '';
              position: absolute;
              width: 6px;
              height: 6px;
              background: #FFFFFF;
              border-radius: 50%;
              top: 50%;
              left: 50%;
              transform: translate(-50%, -50%);
            }
          }
        }
      }
    }
  }

  .config-grid {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 20px;
  }

  .config-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 24px 28px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

    .card-header-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 22px;
      padding-bottom: 14px;
      border-bottom: 1px solid #F1F5F9;

      .header-title-group {
        display: flex;
        align-items: center;
        gap: 10px;

        .card-icon {
          font-size: 18px;
          color: #1677FF;
        }

        .card-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      .status-indicator {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 3px 12px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        background: #F1F5F9;
        color: #94A3B8;

        .status-dot {
          width: 7px;
          height: 7px;
          border-radius: 50%;
          background: #94A3B8;
        }

        &.enabled {
          background: #ECFDF5;
          color: #059669;

          .status-dot {
            background: #10B981;
          }
        }
      }

      .badge-pill-soft {
        padding: 3px 10px;
        border-radius: 9999px;
        background: #EFF6FF;
        color: #1D4ED8;
        font-size: 11.5px;
        font-weight: 600;
      }
    }

    .form-cols-two {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .capsule-input {
      width: 100%;
      height: 42px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      padding: 0 18px;
      font-size: 13.5px;
      color: #1E293B;
      outline: none;
      box-sizing: border-box;
      transition: all 0.2s;

      // 彻底消除 Chrome/Edge 浏览器自动填充带来的黄色/浅蓝色背景
      &:-webkit-autofill,
      &:-webkit-autofill:hover,
      &:-webkit-autofill:focus,
      &:-webkit-autofill:active {
        -webkit-box-shadow: 0 0 0 1000px #FFFFFF inset !important;
        -webkit-text-fill-color: #1E293B !important;
        transition: background-color 50000s ease-in-out 0s;
      }

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }
    }

    .pwd-input-wrap {
      position: relative;
      width: 100%;

      .capsule-input {
        padding-right: 44px;

        // 利用 text-security 伪装圆点，避免浏览器识别为密码框自动填充
        &.secret-masked {
          -webkit-text-security: disc !important;
          text-security: disc !important;
          letter-spacing: 2px;
        }
      }

      .pwd-toggle-btn {
        position: absolute;
        right: 14px;
        top: 50%;
        transform: translateY(-50%);
        border: none;
        background: transparent;
        color: #94A3B8;
        cursor: pointer;
        display: flex;
        align-items: center;
        font-size: 16px;

        &:hover {
          color: #1677FF;
        }
      }
    }

    .input-hint {
      margin-top: 6px;
      font-size: 11.5px;
      color: #94A3B8;
      line-height: 1.4;
    }

    .storage-rules-list {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .storage-rule-row {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 14px 16px;
        background: #F8FAFC;
        border-radius: 14px;
        border: 1px solid #E2E8F0;

        .rule-icon-box {
          width: 36px;
          height: 36px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 18px;
          flex-shrink: 0;

          &.blue {
            background: #EFF6FF;
            color: #1677FF;
            border: 1px solid #BFDBFE;
          }
          &.amber {
            background: #FFFBEB;
            color: #D97706;
            border: 1px solid #FDE68A;
          }
          &.emerald {
            background: #ECFDF5;
            color: #059669;
            border: 1px solid #A7F3D0;
          }
          &.purple {
            background: #FAF5FF;
            color: #9333EA;
            border: 1px solid #E9D5FF;
          }
        }

        .rule-detail {
          display: flex;
          flex-direction: column;
          gap: 4px;
          flex: 1;

          .rule-title-line {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            flex-wrap: wrap;

            .title {
              font-size: 13.5px;
              color: #1E293B;
              font-weight: 600;
            }

            .path-tag {
              padding: 1px 8px;
              border-radius: 6px;
              background: #F1F5F9;
              color: #475569;
              font-family: monospace;
              font-size: 11px;
              border: 1px solid #E2E8F0;
            }
          }

          .desc {
            margin: 0;
            font-size: 12px;
            color: #64748B;
            line-height: 1.45;
          }
        }
      }
    }
  }

  .config-actions-bar {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 16px 24px;
    border: 1px solid #E2E8F0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
    gap: 16px;
    flex-wrap: wrap;

    .actions-left-tip {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #64748B;
      font-size: 13px;

      .tip-icon {
        font-size: 16px;
        color: #1677FF;
        flex-shrink: 0;
      }
    }

    .actions-right-buttons {
      display: flex;
      align-items: center;
      gap: 12px;

      .btn-test-send {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 9px 20px;
        border-radius: 9999px;
        background: #F1F5F9;
        border: 1px solid #CBD5E1;
        color: #334155;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;

        &:hover:not(:disabled) {
          background: #E2E8F0;
          color: #0F172A;
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }

      .btn-save-main {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 9px 24px;
        border-radius: 9999px;
        background: #1677FF;
        border: none;
        color: #FFFFFF;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);

        &:hover:not(:disabled) {
          background: #0958D9;
          transform: translateY(-1px);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }
    }
  }
}

@media (max-width: 1280px) {
  .storage-engine-selector-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 1024px) {
  .config-grid {
    grid-template-columns: 1fr !important;
  }
  .storage-engine-selector-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
