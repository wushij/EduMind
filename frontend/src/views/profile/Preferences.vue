<template>
  <div class="preferences-container">
    <div class="pref-hero-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <span class="header-icon">⚙️</span>
          <h1 class="main-title">个人偏好与教学系统设置</h1>
          <span class="capsule-tag">个性化定制 · 本地即时生效</span>
        </div>
        <p class="sub-desc">
          自定义视觉主题风格、AI 助教响应偏好、智能渲染引擎及系统通知触达通道。
        </p>
      </div>

      <div class="header-right">
        <el-button type="primary" size="large" class="save-btn" @click="savePreferences">
          💾 保存偏好配置
        </el-button>
      </div>
    </div>

    <div class="pref-cards-stack">
      <!-- 视觉与界面偏好 -->
      <el-card shadow="never" class="pref-card">
        <h3 class="card-section-title">🎨 视觉主题与显示模式</h3>
        <div class="options-grid">
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">界面视觉外观</span>
              <span class="row-desc">选择契合个人审美的明亮浅色、深邃暗黑或跟随系统调度。</span>
            </div>
            <el-radio-group v-model="prefs.theme" size="default">
              <el-radio-button label="LIGHT">☀️ 明亮浅色</el-radio-button>
              <el-radio-button label="DARK">🌙 深邃极夜</el-radio-button>
              <el-radio-button label="AUTO">💻 跟随系统</el-radio-button>
            </el-radio-group>
          </div>

          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">系统语言界面</span>
              <span class="row-desc">选择平台的显示语言。</span>
            </div>
            <el-select v-model="prefs.language" size="default" style="width: 160px">
              <el-option label="简体中文 (zh-CN)" value="zh-CN" />
              <el-option label="English (en-US)" value="en-US" />
            </el-select>
          </div>
        </div>
      </el-card>

      <!-- AI 交互体验偏好 -->
      <el-card shadow="never" class="pref-card">
        <h3 class="card-section-title">🤖 AI 智能助教与渲染引擎偏好</h3>
        <div class="options-grid">
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">默认优先推理模型</span>
              <span class="row-desc">在对话与答疑时优先调用的基座大语言模型。</span>
            </div>
            <el-select v-model="prefs.defaultModel" size="default" style="width: 220px">
              <el-option label="DeepSeek-V3 (教学标准推荐)" value="deepseek-v3" />
              <el-option label="DeepSeek-R1 (深度思维链推理)" value="deepseek-r1" />
              <el-option label="Qwen-2.5-72B-Instruct" value="qwen-2.5" />
            </el-select>
          </div>

          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">自动解析并渲染 LaTeX 数学公式</span>
              <span class="row-desc">在试题与答疑正文中自动将 $x \to 0$ 渲染为优雅印刷体公式。</span>
            </div>
            <el-switch v-model="prefs.katexEnabled" />
          </div>

          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">代码块语法高亮与行号</span>
              <span class="row-desc">自动检测代码语言，支持一键复制代码与高亮。</span>
            </div>
            <el-switch v-model="prefs.codeHighlightEnabled" />
          </div>
        </div>
      </el-card>

      <!-- 教学消息与通知提醒 -->
      <el-card shadow="never" class="pref-card">
        <h3 class="card-section-title">🔔 教学活动提醒与消息通知</h3>
        <div class="options-grid">
          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">学生作业提交提醒</span>
              <span class="row-desc">当班级学生提交新的考核答卷时，在右上角站内信通知。</span>
            </div>
            <el-switch v-model="prefs.notifyOnSubmission" />
          </div>

          <div class="pref-row">
            <div class="row-info">
              <span class="row-title">AI 评阅完成通知</span>
              <span class="row-desc">全班批量 AI 智能预批改完成时推送即时通知。</span>
            </div>
            <el-switch v-model="prefs.notifyOnGradingDone" />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';

const STORAGE_KEY = 'edumind_user_preferences';

const prefs = reactive({
  theme: 'LIGHT',
  language: 'zh-CN',
  defaultModel: 'deepseek-v3',
  katexEnabled: true,
  codeHighlightEnabled: true,
  notifyOnSubmission: true,
  notifyOnGradingDone: true
});

onMounted(() => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      Object.assign(prefs, JSON.parse(saved));
    }
  } catch (err) {
    console.warn('读取用户偏好失败:', err);
  }
});

function savePreferences() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
    ElMessage.success('🎉 偏好设置已保存并同步至当前浏览器环境！');
  } catch (err) {
    ElMessage.error('保存偏好设置失败，请检查浏览器存储权限');
  }
}
</script>

<style scoped lang="scss">
.preferences-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);

  .pref-hero-dock {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 24px;

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 28px;
        }

        .main-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .capsule-tag {
          font-size: 12px;
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
          border-radius: 9999px;
          padding: 2px 10px;
          font-weight: 500;
        }
      }

      .sub-desc {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .save-btn {
      background: #2563eb;
      border-color: #2563eb;
      font-weight: 600;
      padding: 10px 24px;
      border-radius: 8px;
    }
  }

  .pref-cards-stack {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .pref-card {
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      padding: 18px 24px;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);

      .card-section-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 16px;
      }

      .options-grid {
        display: flex;
        flex-direction: column;
        gap: 14px;

        .pref-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 12px 16px;
          background: #f8fafc;
          border-radius: 10px;
          border: 1px solid #edf2f7;

          .row-info {
            display: flex;
            flex-direction: column;

            .row-title {
              font-size: 14px;
              font-weight: 600;
              color: #1e293b;
            }

            .row-desc {
              font-size: 12px;
              color: #64748b;
              margin-top: 2px;
            }
          }
        }
      }
    }
  }
}
</style>
