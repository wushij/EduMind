<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="480px"
    class="pill-styled-dialog"
    destroy-on-close
  >
    <el-form :model="form" label-position="top" class="org-node-form">
      <div class="parent-info-pill" v-if="parentNode && mode === 'create'">
        <span class="info-label">上级归属节点:</span>
        <span class="info-val">{{ parentNode.name }}</span>
        <span class="type-badge">{{ parentNode.orgType }}</span>
      </div>

      <el-form-item label="教学实体/组织名称" required>
        <el-input
          v-model="form.name"
          placeholder="如：高三年级组 / 计算机科学与技术(1)班"
          class="pill-input"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="实体层级类型" v-if="mode === 'create'">
        <el-select v-model="form.orgType" class="pill-select" style="width: 100%;">
          <el-option
            v-for="opt in availableOrgTypes"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="显示与排序权重 (较小值排在前面)">
        <el-input-number
          v-model="form.sortOrder"
          :min="1"
          :max="999"
          class="pill-number-input"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer-row">
        <el-button class="pill-cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          class="pill-confirm-btn"
          :loading="submitting"
          @click="submitForm"
        >
          <span>确认保存</span>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { createOrgNode, updateOrgNode } from '@/composables/system/useOrganization';
import type { OrganizationNodeVO } from '@/types/system/tenant';

const props = defineProps<{
  modelValue: boolean;
  mode: 'create' | 'edit';
  parentNode: OrganizationNodeVO | null;
  editNode: OrganizationNodeVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'success'): void;
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const submitting = ref(false);

const form = ref({
  name: '',
  orgType: 'CLASS',
  sortOrder: 1
});

const dialogTitle = computed(() => {
  if (props.mode === 'edit') {
    return `重命名 / 编辑【${props.editNode?.name || ''}】`;
  }
  if (!props.parentNode) {
    return '新增校区顶层实体单元';
  }
  return `在【${props.parentNode.name}】下编排下级实体`;
});

const availableOrgTypes = computed(() => {
  if (!props.parentNode) {
    return [{ label: '校区单元 (CAMPUS)', value: 'CAMPUS' }];
  }
  const pType = props.parentNode.orgType;
  if (pType === 'CAMPUS') {
    return [
      { label: '学院/年级部 (FACULTY)', value: 'FACULTY' },
      { label: '学院/系所 (COLLEGE)', value: 'COLLEGE' },
      { label: '教研室/学科组 (DEPT)', value: 'DEPT' }
    ];
  }
  return [
    { label: '行政教学班级 (CLASS)', value: 'CLASS' },
    { label: '教研室/备课组 (DEPT)', value: 'DEPT' }
  ];
});

watch(() => props.modelValue, (visible) => {
  if (visible) {
    if (props.mode === 'edit' && props.editNode) {
      form.value = {
        name: props.editNode.name,
        orgType: props.editNode.orgType,
        sortOrder: props.editNode.sortOrder || 1
      };
    } else {
      const defaultType = (!props.parentNode) ? 'CAMPUS' : (props.parentNode.orgType === 'CAMPUS' ? 'FACULTY' : 'CLASS');
      form.value = {
        name: '',
        orgType: defaultType,
        sortOrder: (props.parentNode?.children?.length || 0) + 1
      };
    }
  }
});

const submitForm = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入教学实体/组织名称');
    return;
  }

  try {
    submitting.value = true;
    if (props.mode === 'create') {
      await createOrgNode({
        name: form.value.name.trim(),
        orgType: form.value.orgType,
        parentId: props.parentNode ? props.parentNode.id : 0,
        sortOrder: form.value.sortOrder
      });
      ElMessage.success('下级教学实体创建成功');
    } else if (props.editNode) {
      await updateOrgNode(props.editNode.id, {
        name: form.value.name.trim(),
        sortOrder: form.value.sortOrder
      });
      ElMessage.success('实体已重命名');
    }
    dialogVisible.value = false;
    emit('success');
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped lang="scss">
.pill-styled-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 20px 48px rgba(15, 23, 42, 0.12);
  }

  :deep(.el-dialog__header) {
    margin-right: 0;
    padding: 20px 24px 14px;
    border-bottom: 1px solid #F1F5F9;

    .el-dialog__title {
      font-size: 16.5px;
      font-weight: 800;
      color: #0F172A;
    }
  }

  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }

  :deep(.el-dialog__footer) {
    padding: 14px 24px 20px;
    border-top: 1px solid #F1F5F9;
  }
}

.org-node-form {
  .parent-info-pill {
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 9999px;
    padding: 6px 14px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 12.5px;
    margin-bottom: 18px;

    .info-label {
      color: #64748B;
    }

    .info-val {
      font-weight: 700;
      color: #2563EB;
    }

    .type-badge {
      background: #2563EB;
      color: #FFFFFF;
      font-size: 10.5px;
      font-weight: 700;
      padding: 1px 7px;
      border-radius: 9999px;
    }
  }

  .pill-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      padding: 4px 12px;
    }
  }

  .pill-select {
    :deep(.el-select__wrapper) {
      border-radius: 12px;
    }
  }

  .pill-number-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
    }
  }
}

.dialog-footer-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .pill-cancel-btn {
    border-radius: 9999px;
    padding: 8px 20px;
  }

  .pill-confirm-btn {
    border-radius: 9999px;
    padding: 8px 24px;
    background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
    font-weight: 600;

    &:hover {
      background: linear-gradient(135deg, #1D4ED8 0%, #1E40AF 100%);
      transform: translateY(-1px);
    }
  }
}
</style>
