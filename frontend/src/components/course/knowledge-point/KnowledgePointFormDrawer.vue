<template>
  <el-drawer
    :model-value="visible"
    :title="title"
    size="540px"
    destroy-on-close
    class="kp-drawer"
    @update:model-value="$emit('update:visible', $event)"
  >
    <template #header>
      <div class="drawer-header-flex">
        <div class="header-icon-circle">
          <el-icon><Connection /></el-icon>
        </div>
        <div>
          <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: #0f172a">{{ title }}</h3>
          <p style="margin: 2px 0 0; font-size: 12px; color: #64748b">构建知识图谱节点，支持 AI 提炼与前置关联</p>
        </div>
      </div>
    </template>

    <div class="kp-ai-helper-banner" @click="$emit('open-ai')">
      <div class="ai-spark-icon">
        <el-icon><MagicStick /></el-icon>
      </div>
      <div class="ai-spark-meta">
        <strong>使用 AI 辅助智能提炼核心考点</strong>
        <p>基于当前章节大纲与课程上下文生成可入库考点</p>
      </div>
      <button type="button" class="spark-call-btn">AI提取</button>
    </div>

    <el-form label-position="top" class="kp-create-form">
      <el-form-item label="知识点名称" required>
        <el-input v-model="form.title" placeholder="例如：双向链表插入与删除节点算法" maxlength="60" show-word-limit />
      </el-form-item>

      <div class="form-grid-2">
        <el-form-item label="所属课程章节" required>
          <el-select v-model="form.chapterId" placeholder="选择所属章节" class="w-full">
            <el-option
              v-for="(chap, idx) in chapters"
              :key="chap.id"
              :label="`第 ${idx + 1} 章：${chap.title}`"
              :value="chap.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="认知目标维度">
          <el-select v-model="form.cognitiveDimension" class="w-full">
            <el-option label="识记概念 (Remember)" value="REMEMBER" />
            <el-option label="理解领会 (Understand)" value="UNDERSTAND" />
            <el-option label="实践应用 (Apply)" value="APPLY" />
            <el-option label="分析综合 (Analyze)" value="ANALYZE" />
          </el-select>
        </el-form-item>
      </div>

      <div class="form-grid-2">
        <el-form-item label="核心考查重要度">
          <el-rate v-model="form.importance" :max="5" />
        </el-form-item>
        <el-form-item label="知识点编码（可选）">
          <el-input v-model="form.code" placeholder="留空则由系统自动生成" maxlength="32" />
        </el-form-item>
      </div>

      <el-form-item label="考查易错陷阱与重点">
        <el-input v-model="form.examFocus" placeholder="例如：边界指针判空、断链死循环" maxlength="80" show-word-limit />
      </el-form-item>

      <el-form-item label="前置知识点（图谱 prerequisite）">
        <el-select
          v-model="form.prerequisiteIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择本课程中需先掌握的知识点"
          class="w-full"
        >
          <el-option v-for="opt in prerequisiteOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="核心考点与概念阐述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="详细说明该知识点需掌握的概念、推导要求与代码实现细节..."
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer-actions">
        <el-button class="capsule-btn capsule-btn--secondary" @click="$emit('update:visible', false)">取消</el-button>
        <el-button
          type="primary"
          class="capsule-btn capsule-btn--primary"
          :loading="saving"
          :disabled="saving || !form.title.trim()"
          @click="$emit('save')"
        >
          {{ saving ? '保存中...' : '确认保存' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { Connection, MagicStick } from '@element-plus/icons-vue';

defineProps<{
  visible: boolean;
  title: string;
  form: {
    title: string;
    chapterId: number;
    code: string;
    cognitiveDimension: string;
    importance: number;
    description: string;
    examFocus: string;
    prerequisiteIds: number[];
  };
  chapters: Array<{ id: number; title: string }>;
  prerequisiteOptions: Array<{ value: number; label: string }>;
  saving: boolean;
}>();

defineEmits<{
  'update:visible': [boolean];
  save: [];
  'open-ai': [];
}>();
</script>

<style scoped lang="scss">
.drawer-header-flex {
  display: flex;
  align-items: center;
  gap: 12px;

  .header-icon-circle {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
  }
}

.kp-ai-helper-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #f0fdf4 0%, #eff6ff 100%);
  border: 1.5px dashed #86efac;
  border-radius: 14px;
  padding: 12px 14px;
  cursor: pointer;
  margin-bottom: 18px;

  .ai-spark-icon {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: #dcfce7;
    color: #15803d;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
  }

  .ai-spark-meta {
    flex: 1;

    strong {
      display: block;
      font-size: 13px;
      color: #166534;
    }

    p {
      font-size: 11.5px;
      color: #64748b;
      margin: 2px 0 0;
    }
  }

  .spark-call-btn {
    height: 28px;
    padding: 0 12px;
    border-radius: 9999px;
    background: #10b981;
    border: none;
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
  }
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.w-full {
  width: 100%;
}

.drawer-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.capsule-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: 9999px;
  font-weight: 600;
}
</style>
