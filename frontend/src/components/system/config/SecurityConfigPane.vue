<template>
  <div class="security-config-pane">
    <div class="config-grid">
      <!-- 左侧：网络传输与重放攻击防护 -->
      <div class="config-card">
        <div class="card-header-bar">
          <div class="header-title-group">
            <el-icon class="card-icon"><Lock /></el-icon>
            <h2 class="card-title">全链路接口防重放与国密传输安全</h2>
          </div>
          <span class="badge-pill-soft">金融级防御矩阵</span>
        </div>

        <el-form label-position="top" class="config-form" autocomplete="off">
          <!-- 1. 时间戳防重放校验 -->
          <div class="form-row-toggle">
            <div class="toggle-info">
              <span class="toggle-title">开启请求时间戳时效校验 (X-Timestamp)</span>
              <span class="toggle-desc">
                严格检验客户端请求时间戳，防范网络嗅探后报文在数分钟后被恶意重放调用
              </span>
            </div>
            <el-switch v-model="securityForm.timestampCheckEnabled" active-color="#1677FF" />
          </div>

          <el-form-item v-if="securityForm.timestampCheckEnabled" label="时间戳最大允许偏差窗口 (秒，默认推荐 300 秒)">
            <input
              v-model.number="securityForm.timestampToleranceSeconds"
              type="number"
              min="30"
              max="3600"
              class="capsule-input"
              placeholder="300"
            />
            <div class="input-hint">严格检验客户端请求时间偏差，推荐 300 秒 (5分钟)，兼顾网络延时与防重放严密性。</div>
          </el-form-item>

          <!-- 2. Nonce 随机数防重放 -->
          <div class="form-row-toggle">
            <div class="toggle-info">
              <span class="toggle-title">开启 Nonce 随机标识防重放锁 (X-Nonce)</span>
              <span class="toggle-desc">
                结合 Redis 缓存窗口记录单次请求随机凭据，同一 Nonce 在有效窗口内仅能执行一次
              </span>
            </div>
            <el-switch v-model="securityForm.nonceCheckEnabled" active-color="#1677FF" />
          </div>

          <!-- 3. 国密 SM3 报文签名 -->
          <div class="form-row-toggle">
            <div class="toggle-info">
              <span class="toggle-title">开启国密 SM3 请求摘要与签名防篡改 (X-Signature)</span>
              <span class="toggle-desc">
                前后端针对 URL、时间戳、Nonce 及 Body 自动计算国密 SM3 签名，拒绝篡改数据
              </span>
            </div>
            <el-switch v-model="securityForm.sm3SignEnabled" active-color="#1677FF" />
          </div>

          <!-- 4. 国密 SM4 数据加密 -->
          <div class="form-row-toggle mini">
            <div class="toggle-info">
              <span class="toggle-title">开启国密 SM4 端到端敏感传输加密 (CBC 模式)</span>
              <span class="toggle-desc">
                登录密码、个人隐私数据在浏览器端以国密 SM4 算法加密后再行投递，传输链路全密文
              </span>
            </div>
            <el-switch v-model="securityForm.sm4EncryptEnabled" active-color="#1677FF" />
          </div>
        </el-form>
      </div>

      <!-- 右侧：人机验证码与防自动化爆破 -->
      <div class="config-card">
        <div class="card-header-bar">
          <div class="header-title-group">
            <el-icon class="card-icon"><CircleCheckFilled /></el-icon>
            <h2 class="card-title">人机验证码触发与防自动化脚本策略</h2>
          </div>
          <span class="badge-pill-soft">动态自适应策略</span>
        </div>

        <el-form label-position="top" class="config-form" autocomplete="off">
          <!-- 5. 登录验证码策略 -->
          <el-form-item label="登录入口人机图形验证码触发策略 (Captcha Policy)">
            <el-select
              v-model="securityForm.loginCaptchaStrategy"
              placeholder="请选择验证码策略"
              style="width: 100%"
            >
              <el-option
                label="密码错误 5 次后动态触发（推荐：兼顾顺畅体验与爆破防护）"
                value="fail_trigger"
              />
              <el-option
                label="始终强制开启图形验证码（适合高安全等级或公开外网环境）"
                value="always"
              />
              <el-option
                label="彻底停用登录验证码（仅限内网调试，不推荐在生产使用）"
                value="off"
              />
            </el-select>
            <div class="input-hint">
              动态触发模式下，系统在 Redis 中记录 IP / 账号失败频次，平素无需输入，遭遇撞库攻击时自动激活。
            </div>
          </el-form-item>

          <!-- 6. 注册验证码 -->
          <div class="form-row-toggle">
            <div class="toggle-info">
              <span class="toggle-title">开启新用户自主注册图形验证码</span>
              <span class="toggle-desc">
                防止利用自动化无头浏览器与脚本批量注册垃圾账号，保障数据库用户体系纯净
              </span>
            </div>
            <el-switch v-model="securityForm.registerCaptchaEnabled" active-color="#1677FF" />
          </div>

          <!-- 7. 防 F12 调试 -->
          <div class="form-row-toggle mini">
            <div class="toggle-info">
              <span class="toggle-title">开启生产环境防 F12 调试与控制台代码防逆向</span>
              <span class="toggle-desc">
                在生产环境中激活反调试探针与 DevTools 监测，严防恶意篡改前端业务状态与逆向逻辑
              </span>
            </div>
            <el-switch v-model="securityForm.antiDebugEnabled" active-color="#1677FF" />
          </div>

          <!-- 会话机制说明卡片 -->
          <div class="security-info-pill-box">
            <div class="pill-icon">
              <el-icon><Key /></el-icon>
            </div>
            <div class="pill-content">
              <strong>会话机制：Sa-Token 24小时滑动续期保护</strong>
              <span>
                用户单次登录令牌有效时间为 86,400 秒，支持无感知活跃续签与踢人下线，密码修改后多端即刻失效。
              </span>
            </div>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 底部保存工具栏 -->
    <div class="config-actions-bar">
      <div class="actions-left-tip">
        <el-icon class="tip-icon"><InfoFilled /></el-icon>
        <span>
          全链路安全策略保存后，将通过 Redis 动态控制总线毫秒级广播，后端拦截器即刻生效，无须重启服务。
        </span>
      </div>
      <div class="actions-right-buttons">
        <button
          type="button"
          class="btn-save-main"
          :disabled="savingSecurity"
          @click="handleSaveSecurityConfig"
        >
          <el-icon class="btn-icon"><Check /></el-icon>
          <span>{{ savingSecurity ? '正在应用安全策略...' : '保存并热应用安全策略' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Lock,
  CircleCheckFilled,
  Key,
  InfoFilled,
  Check
} from '@element-plus/icons-vue';
import { getSecurityConfig, updateSecurityConfig } from '@/api/system/config';
import type { SecurityConfigVO } from '@/types/system';

const savingSecurity = ref(false);

const securityForm = reactive<SecurityConfigVO>({
  timestampCheckEnabled: false,
  timestampToleranceSeconds: 300,
  nonceCheckEnabled: false,
  sm3SignEnabled: false,
  sm4EncryptEnabled: false,
  loginCaptchaStrategy: 'fail_trigger',
  registerCaptchaEnabled: true,
  antiDebugEnabled: false
});

async function loadSecurityConfig() {
  try {
    const res = await getSecurityConfig();
    if (res?.data) {
      Object.assign(securityForm, res.data);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取安全配置失败');
  }
}

async function handleSaveSecurityConfig() {
  savingSecurity.value = true;
  try {
    await updateSecurityConfig({
      timestampCheckEnabled: securityForm.timestampCheckEnabled,
      timestampToleranceSeconds: securityForm.timestampToleranceSeconds,
      nonceCheckEnabled: securityForm.nonceCheckEnabled,
      sm3SignEnabled: securityForm.sm3SignEnabled,
      sm4EncryptEnabled: securityForm.sm4EncryptEnabled,
      loginCaptchaStrategy: securityForm.loginCaptchaStrategy,
      registerCaptchaEnabled: securityForm.registerCaptchaEnabled,
      antiDebugEnabled: securityForm.antiDebugEnabled
    });
    ElMessage.success('全链路安全防护策略已成功保存，后端拦截器与安全网关毫秒级热生效！');
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '保存安全配置失败');
  } finally {
    savingSecurity.value = false;
  }
}

defineExpose({
  loadSecurityConfig
});

onMounted(() => {
  loadSecurityConfig();
});
</script>

<style scoped lang="scss">
.security-config-pane {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .config-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
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

      .badge-pill-soft {
        padding: 3px 10px;
        border-radius: 9999px;
        background: #EFF6FF;
        color: #1D4ED8;
        font-size: 11.5px;
        font-weight: 600;
      }
    }

    .form-row-toggle {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 16px;
      background: #F8FAFC;
      border-radius: 12px;
      margin-bottom: 14px;
      border: 1px solid #E2E8F0;
      gap: 16px;

      &.mini {
        padding: 12px 16px;
      }

      .toggle-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;

        .toggle-title {
          font-size: 13.5px;
          font-weight: 600;
          color: #1E293B;
        }

        .toggle-desc {
          font-size: 12px;
          color: #64748B;
          line-height: 1.4;
        }
      }
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

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }
    }


    .input-hint {
      margin-top: 6px;
      font-size: 11.5px;
      color: #94A3B8;
      line-height: 1.4;
    }

    .security-info-pill-box {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 16px;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;
      border-radius: 14px;
      margin-top: 14px;

      .pill-icon {
        font-size: 18px;
        color: #1677FF;
        margin-top: 2px;
      }

      .pill-content {
        display: flex;
        flex-direction: column;
        gap: 2px;

        strong {
          font-size: 13px;
          color: #1E293B;
        }

        span {
          font-size: 11.5px;
          color: #64748B;
          line-height: 1.45;
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

@media (max-width: 1024px) {
  .config-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
