<template>
  <div class="profile-side-column">
    <!-- A. AI 助教偏好配置 -->
    <div class="profile-panel-card">
      <div class="panel-header-line">
        <el-icon class="panel-icon"><Cpu /></el-icon>
        <h3 class="panel-title">AI 大模型推理与答疑偏好</h3>
      </div>

      <div class="preference-section">
        <div class="pref-item">
          <span class="pref-label">默认教学大模型引擎：</span>
          <div class="pill-radio-group">
            <span
              v-for="model in modelOptions"
              :key="model.value"
              class="pill-radio-opt"
              :class="{ active: aiPref.model === model.value }"
              @click="aiPref.model = model.value"
            >
              {{ model.label }}
            </span>
          </div>
        </div>

        <div class="pref-item">
          <span class="pref-label">生成推导创造性（Temperature 温度）：</span>
          <div class="pill-radio-group">
            <span
              class="pill-radio-opt"
              :class="{ active: aiPref.temp === 0.2 }"
              @click="aiPref.temp = 0.2"
            >
              严谨学术 (0.2)
            </span>
            <span
              class="pill-radio-opt"
              :class="{ active: aiPref.temp === 0.5 }"
              @click="aiPref.temp = 0.5"
            >
              平衡教学 (0.5)
            </span>
            <span
              class="pill-radio-opt"
              :class="{ active: aiPref.temp === 0.8 }"
              @click="aiPref.temp = 0.8"
            >
              启发发散 (0.8)
            </span>
          </div>
        </div>

        <div class="pref-item">
          <span class="pref-label">RAG 知识库召回切片数量 (TopK)：</span>
          <div class="pill-radio-group">
            <span
              v-for="k in [3, 5, 8]"
              :key="k"
              class="pill-radio-opt"
              :class="{ active: aiPref.topK === k }"
              @click="aiPref.topK = k"
            >
              Top {{ k }} 片段
            </span>
          </div>
        </div>

        <button
          type="button"
          class="capsule-ai-pref-btn"
          @click="handleSaveAiPref"
        >
          <span>保存 AI 推理偏好</span>
        </button>
      </div>
    </div>

    <!-- B. 个人 Token 额度消耗 -->
    <div class="profile-panel-card">
      <div class="panel-header-line">
        <el-icon class="panel-icon"><Lightning /></el-icon>
        <h3 class="panel-title">本学期个人 AI 算力额度</h3>
      </div>

      <div class="quota-meter-box">
        <div class="quota-top">
          <span class="quota-label">Token 消耗情况</span>
          <span class="quota-val"><strong>1,842,000</strong> / 5,000,000</span>
        </div>
        <div class="capsule-progress-track">
          <div class="capsule-progress-fill" style="width: 36.8%"></div>
        </div>
        <div class="quota-sub">
          <span>剩余 3,158,000 Tokens (已用 36.8%)</span>
          <span class="quota-status">高校校园网无限制支持</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Cpu, Lightning } from '@element-plus/icons-vue';
import { PROFILE_MODEL_OPTIONS } from '@/composables/profile/useProfile';

defineProps<{
  modelOptions: typeof PROFILE_MODEL_OPTIONS;
  aiPref: {
    model: string;
    temp: number;
    topK: number;
  };
  handleSaveAiPref: () => void;
}>();
</script>

<style scoped lang="scss">
.profile-side-column {
  .profile-panel-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 24px 28px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    margin-bottom: 20px;

    .panel-header-line {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 20px;
      padding-bottom: 14px;
      border-bottom: 1px solid #F1F5F9;

      .panel-icon {
        font-size: 18px;
        color: #1677FF;
        display: inline-flex;
        align-items: center;
      }

      .panel-title {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
      }
    }

    // AI 偏好
    .preference-section {
      display: flex;
      flex-direction: column;
      gap: 18px;

      .pref-item {
        .pref-label {
          display: block;
          font-size: 13px;
          color: #64748B;
          font-weight: 500;
          margin-bottom: 8px;
        }

        .pill-radio-group {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .pill-radio-opt {
            padding: 6px 16px;
            border-radius: 9999px; // 纯正长圆单选
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            color: #475569;
            font-size: 12.5px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
              color: #1677FF;
              border-color: #93C5FD;
            }

            &.active {
              background: #EFF6FF;
              border-color: #1677FF;
              color: #1677FF;
              font-weight: 600;
            }
          }
        }
      }

      .capsule-ai-pref-btn {
        margin-top: 6px;
        height: 38px;
        border-radius: 9999px;
        background: #722ED1;
        color: #FFFFFF;
        border: none;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 2px 8px rgba(114, 46, 209, 0.25);
        transition: all 0.2s;

        &:hover {
          background: #531DAB;
        }
      }
    }

    // 配额条
    .quota-meter-box {
      .quota-top {
        display: flex;
        justify-content: space-between;
        font-size: 13px;
        margin-bottom: 8px;

        .quota-label {
          color: #64748B;
        }

        .quota-val {
          color: #1E293B;
        }
      }

      .capsule-progress-track {
        width: 100%;
        height: 8px;
        background: #E2E8F0;
        border-radius: 9999px;
        overflow: hidden;
        margin-bottom: 8px;

        .capsule-progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #1677FF 0%, #38BDF8 100%);
          border-radius: 9999px;
        }
      }

      .quota-sub {
        display: flex;
        justify-content: space-between;
        font-size: 11.5px;
        color: #94A3B8;

        .quota-status {
          color: #059669;
          font-weight: 600;
        }
      }
    }
  }
}
</style>
